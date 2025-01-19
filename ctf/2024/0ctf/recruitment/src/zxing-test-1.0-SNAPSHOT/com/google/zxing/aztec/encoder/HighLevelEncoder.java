package com.google.zxing.aztec.encoder;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.CharacterSetECI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public final class HighLevelEncoder {
   static final String[] MODE_NAMES = new String[]{"UPPER", "LOWER", "DIGIT", "MIXED", "PUNCT"};
   static final int MODE_UPPER = 0;
   static final int MODE_LOWER = 1;
   static final int MODE_DIGIT = 2;
   static final int MODE_MIXED = 3;
   static final int MODE_PUNCT = 4;
   static final int[][] LATCH_TABLE = new int[][]{
      {0, 327708, 327710, 327709, 656318},
      {590318, 0, 327710, 327709, 656318},
      {262158, 590300, 0, 590301, 932798},
      {327709, 327708, 656318, 0, 327710},
      {327711, 656380, 656382, 656381, 0}
   };
   private static final int[][] CHAR_MAP = new int[5][256];
   static final int[][] SHIFT_TABLE;
   private final byte[] text;
   private final Charset charset;

   public HighLevelEncoder(byte[] text) {
      this.text = text;
      this.charset = null;
   }

   public HighLevelEncoder(byte[] text, Charset charset) {
      this.text = text;
      this.charset = charset;
   }

   public BitArray encode() {
      State initialState = State.INITIAL_STATE;
      if (this.charset != null) {
         CharacterSetECI eci = CharacterSetECI.getCharacterSetECI(this.charset);
         if (null == eci) {
            throw new IllegalArgumentException("No ECI code for character set " + this.charset);
         }

         initialState = initialState.appendFLGn(eci.getValue());
      }

      Collection<State> states = Collections.singletonList(initialState);

      for (int index = 0; index < this.text.length; index++) {
         int nextChar = index + 1 < this.text.length ? this.text[index + 1] : 0;
         int pairCode;
         switch (this.text[index]) {
            case 13:
               pairCode = nextChar == 10 ? 2 : 0;
               break;
            case 44:
               pairCode = nextChar == 32 ? 4 : 0;
               break;
            case 46:
               pairCode = nextChar == 32 ? 3 : 0;
               break;
            case 58:
               pairCode = nextChar == 32 ? 5 : 0;
               break;
            default:
               pairCode = 0;
         }

         if (pairCode > 0) {
            states = updateStateListForPair(states, index, pairCode);
            index++;
         } else {
            states = this.updateStateListForChar(states, index);
         }
      }

      State minState = Collections.min(states, new Comparator<State>() {
         public int compare(State a, State b) {
            return a.getBitCount() - b.getBitCount();
         }
      });
      return minState.toBitArray(this.text);
   }

   private Collection<State> updateStateListForChar(Iterable<State> states, int index) {
      Collection<State> result = new LinkedList<>();

      for (State state : states) {
         this.updateStateForChar(state, index, result);
      }

      return simplifyStates(result);
   }

   private void updateStateForChar(State state, int index, Collection<State> result) {
      char ch = (char)(this.text[index] & 255);
      boolean charInCurrentTable = CHAR_MAP[state.getMode()][ch] > 0;
      State stateNoBinary = null;

      for (int mode = 0; mode <= 4; mode++) {
         int charInMode = CHAR_MAP[mode][ch];
         if (charInMode > 0) {
            if (stateNoBinary == null) {
               stateNoBinary = state.endBinaryShift(index);
            }

            if (!charInCurrentTable || mode == state.getMode() || mode == 2) {
               State latchState = stateNoBinary.latchAndAppend(mode, charInMode);
               result.add(latchState);
            }

            if (!charInCurrentTable && SHIFT_TABLE[state.getMode()][mode] >= 0) {
               State shiftState = stateNoBinary.shiftAndAppend(mode, charInMode);
               result.add(shiftState);
            }
         }
      }

      if (state.getBinaryShiftByteCount() > 0 || CHAR_MAP[state.getMode()][ch] == 0) {
         State binaryState = state.addBinaryShiftChar(index);
         result.add(binaryState);
      }
   }

   private static Collection<State> updateStateListForPair(Iterable<State> states, int index, int pairCode) {
      Collection<State> result = new LinkedList<>();

      for (State state : states) {
         updateStateForPair(state, index, pairCode, result);
      }

      return simplifyStates(result);
   }

   private static void updateStateForPair(State state, int index, int pairCode, Collection<State> result) {
      State stateNoBinary = state.endBinaryShift(index);
      result.add(stateNoBinary.latchAndAppend(4, pairCode));
      if (state.getMode() != 4) {
         result.add(stateNoBinary.shiftAndAppend(4, pairCode));
      }

      if (pairCode == 3 || pairCode == 4) {
         State digitState = stateNoBinary.latchAndAppend(2, 16 - pairCode).latchAndAppend(2, 1);
         result.add(digitState);
      }

      if (state.getBinaryShiftByteCount() > 0) {
         State binaryState = state.addBinaryShiftChar(index).addBinaryShiftChar(index + 1);
         result.add(binaryState);
      }
   }

   private static Collection<State> simplifyStates(Iterable<State> states) {
      Deque<State> result = new LinkedList<>();

      for (State newState : states) {
         boolean add = true;
         Iterator<State> iterator = result.iterator();

         while (iterator.hasNext()) {
            State oldState = iterator.next();
            if (oldState.isBetterThanOrEqualTo(newState)) {
               add = false;
               break;
            }

            if (newState.isBetterThanOrEqualTo(oldState)) {
               iterator.remove();
            }
         }

         if (add) {
            result.addFirst(newState);
         }
      }

      return result;
   }

   static {
      CHAR_MAP[0][32] = 1;

      for (int c = 65; c <= 90; c++) {
         CHAR_MAP[0][c] = c - 65 + 2;
      }

      CHAR_MAP[1][32] = 1;

      for (int c = 97; c <= 122; c++) {
         CHAR_MAP[1][c] = c - 97 + 2;
      }

      CHAR_MAP[2][32] = 1;

      for (int c = 48; c <= 57; c++) {
         CHAR_MAP[2][c] = c - 48 + 2;
      }

      CHAR_MAP[2][44] = 12;
      CHAR_MAP[2][46] = 13;
      int[] mixedTable = new int[]{0, 32, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 27, 28, 29, 30, 31, 64, 92, 94, 95, 96, 124, 126, 127};
      int i = 0;

      while (i < mixedTable.length) {
         CHAR_MAP[3][mixedTable[i]] = i++;
      }

      int[] punctTable = new int[]{0, 13, 0, 0, 0, 0, 33, 39, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 58, 59, 60, 61, 62, 63, 91, 93, 123, 125};

      for (int ix = 0; ix < punctTable.length; ix++) {
         if (punctTable[ix] > 0) {
            CHAR_MAP[4][punctTable[ix]] = ix;
         }
      }

      SHIFT_TABLE = new int[6][6];

      for (int[] table : SHIFT_TABLE) {
         Arrays.fill(table, -1);
      }

      SHIFT_TABLE[0][4] = 0;
      SHIFT_TABLE[1][4] = 0;
      SHIFT_TABLE[1][0] = 28;
      SHIFT_TABLE[3][4] = 0;
      SHIFT_TABLE[2][4] = 0;
      SHIFT_TABLE[2][0] = 15;
   }
}
