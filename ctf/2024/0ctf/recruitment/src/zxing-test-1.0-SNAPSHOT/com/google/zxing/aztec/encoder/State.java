package com.google.zxing.aztec.encoder;

import com.google.zxing.common.BitArray;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class State {
   static final State INITIAL_STATE = new State(Token.EMPTY, 0, 0, 0);
   private final int mode;
   private final Token token;
   private final int binaryShiftByteCount;
   private final int bitCount;
   private final int binaryShiftCost;

   private State(Token token, int mode, int binaryBytes, int bitCount) {
      this.token = token;
      this.mode = mode;
      this.binaryShiftByteCount = binaryBytes;
      this.bitCount = bitCount;
      this.binaryShiftCost = calculateBinaryShiftCost(binaryBytes);
   }

   int getMode() {
      return this.mode;
   }

   Token getToken() {
      return this.token;
   }

   int getBinaryShiftByteCount() {
      return this.binaryShiftByteCount;
   }

   int getBitCount() {
      return this.bitCount;
   }

   State appendFLGn(int eci) {
      State result = this.shiftAndAppend(4, 0);
      Token token = result.token;
      int bitsAdded = 3;
      if (eci < 0) {
         token = token.add(0, 3);
      } else {
         if (eci > 999999) {
            throw new IllegalArgumentException("ECI code must be between 0 and 999999");
         }

         byte[] eciDigits = Integer.toString(eci).getBytes(StandardCharsets.ISO_8859_1);
         token = token.add(eciDigits.length, 3);

         for (byte eciDigit : eciDigits) {
            token = token.add(eciDigit - 48 + 2, 4);
         }

         bitsAdded += eciDigits.length * 4;
      }

      return new State(token, this.mode, 0, this.bitCount + bitsAdded);
   }

   State latchAndAppend(int mode, int value) {
      int bitCount = this.bitCount;
      Token token = this.token;
      if (mode != this.mode) {
         int latch = HighLevelEncoder.LATCH_TABLE[this.mode][mode];
         token = token.add(latch & 65535, latch >> 16);
         bitCount += latch >> 16;
      }

      int latchModeBitCount = mode == 2 ? 4 : 5;
      token = token.add(value, latchModeBitCount);
      return new State(token, mode, 0, bitCount + latchModeBitCount);
   }

   State shiftAndAppend(int mode, int value) {
      Token token = this.token;
      int thisModeBitCount = this.mode == 2 ? 4 : 5;
      token = token.add(HighLevelEncoder.SHIFT_TABLE[this.mode][mode], thisModeBitCount);
      token = token.add(value, 5);
      return new State(token, this.mode, 0, this.bitCount + thisModeBitCount + 5);
   }

   State addBinaryShiftChar(int index) {
      Token token = this.token;
      int mode = this.mode;
      int bitCount = this.bitCount;
      if (this.mode == 4 || this.mode == 2) {
         int latch = HighLevelEncoder.LATCH_TABLE[mode][0];
         token = token.add(latch & 65535, latch >> 16);
         bitCount += latch >> 16;
         mode = 0;
      }

      int deltaBitCount = this.binaryShiftByteCount != 0 && this.binaryShiftByteCount != 31 ? (this.binaryShiftByteCount == 62 ? 9 : 8) : 18;
      State result = new State(token, mode, this.binaryShiftByteCount + 1, bitCount + deltaBitCount);
      if (result.binaryShiftByteCount == 2078) {
         result = result.endBinaryShift(index + 1);
      }

      return result;
   }

   State endBinaryShift(int index) {
      if (this.binaryShiftByteCount == 0) {
         return this;
      } else {
         Token token = this.token;
         token = token.addBinaryShift(index - this.binaryShiftByteCount, this.binaryShiftByteCount);
         return new State(token, this.mode, 0, this.bitCount);
      }
   }

   boolean isBetterThanOrEqualTo(State other) {
      int newModeBitCount = this.bitCount + (HighLevelEncoder.LATCH_TABLE[this.mode][other.mode] >> 16);
      if (this.binaryShiftByteCount < other.binaryShiftByteCount) {
         newModeBitCount += other.binaryShiftCost - this.binaryShiftCost;
      } else if (this.binaryShiftByteCount > other.binaryShiftByteCount && other.binaryShiftByteCount > 0) {
         newModeBitCount += 10;
      }

      return newModeBitCount <= other.bitCount;
   }

   BitArray toBitArray(byte[] text) {
      List<Token> symbols = new ArrayList<>();

      for (Token token = this.endBinaryShift(text.length).token; token != null; token = token.getPrevious()) {
         symbols.add(token);
      }

      BitArray bitArray = new BitArray();

      for (int i = symbols.size() - 1; i >= 0; i--) {
         symbols.get(i).appendTo(bitArray, text);
      }

      return bitArray;
   }

   @Override
   public String toString() {
      return String.format("%s bits=%d bytes=%d", HighLevelEncoder.MODE_NAMES[this.mode], this.bitCount, this.binaryShiftByteCount);
   }

   private static int calculateBinaryShiftCost(int binaryShiftByteCount) {
      if (binaryShiftByteCount > 62) {
         return 21;
      } else if (binaryShiftByteCount > 31) {
         return 20;
      } else {
         return binaryShiftByteCount > 0 ? 10 : 0;
      }
   }
}
