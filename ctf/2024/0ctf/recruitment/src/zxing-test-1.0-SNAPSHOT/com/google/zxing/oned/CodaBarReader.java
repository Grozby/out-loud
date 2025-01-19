package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.Arrays;
import java.util.Map;

public final class CodaBarReader extends OneDReader {
   private static final float MAX_ACCEPTABLE = 2.0F;
   private static final float PADDING = 1.5F;
   private static final String ALPHABET_STRING = "0123456789-$:/.+ABCD";
   static final char[] ALPHABET = "0123456789-$:/.+ABCD".toCharArray();
   static final int[] CHARACTER_ENCODINGS = new int[]{3, 6, 9, 96, 18, 66, 33, 36, 48, 72, 12, 24, 69, 81, 84, 21, 26, 41, 11, 14};
   private static final int MIN_CHARACTER_LENGTH = 3;
   private static final char[] STARTEND_ENCODING = new char[]{'A', 'B', 'C', 'D'};
   private final StringBuilder decodeRowResult = new StringBuilder(20);
   private int[] counters = new int[80];
   private int counterLength = 0;

   @Override
   public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException {
      Arrays.fill(this.counters, 0);
      this.setCounters(row);
      int startOffset = this.findStartPattern();
      int nextStart = startOffset;
      this.decodeRowResult.setLength(0);

      int charOffset;
      do {
         charOffset = this.toNarrowWidePattern(nextStart);
         if (charOffset == -1) {
            throw NotFoundException.getNotFoundInstance();
         }

         this.decodeRowResult.append((char)charOffset);
         nextStart += 8;
      } while ((this.decodeRowResult.length() <= 1 || !arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) && nextStart < this.counterLength);

      charOffset = this.counters[nextStart - 1];
      int lastPatternSize = 0;

      for (int i = -8; i < -1; i++) {
         lastPatternSize += this.counters[nextStart + i];
      }

      if (nextStart < this.counterLength && charOffset < lastPatternSize / 2) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         this.validatePattern(startOffset);

         for (int i = 0; i < this.decodeRowResult.length(); i++) {
            this.decodeRowResult.setCharAt(i, ALPHABET[this.decodeRowResult.charAt(i)]);
         }

         char startchar = this.decodeRowResult.charAt(0);
         if (!arrayContains(STARTEND_ENCODING, startchar)) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            char endchar = this.decodeRowResult.charAt(this.decodeRowResult.length() - 1);
            if (!arrayContains(STARTEND_ENCODING, endchar)) {
               throw NotFoundException.getNotFoundInstance();
            } else if (this.decodeRowResult.length() <= 3) {
               throw NotFoundException.getNotFoundInstance();
            } else {
               if (hints == null || !hints.containsKey(DecodeHintType.RETURN_CODABAR_START_END)) {
                  this.decodeRowResult.deleteCharAt(this.decodeRowResult.length() - 1);
                  this.decodeRowResult.deleteCharAt(0);
               }

               int runningCount = 0;

               for (int i = 0; i < startOffset; i++) {
                  runningCount += this.counters[i];
               }

               float left = (float)runningCount;

               for (int i = startOffset; i < nextStart - 1; i++) {
                  runningCount += this.counters[i];
               }

               float right = (float)runningCount;
               Result result = new Result(
                  this.decodeRowResult.toString(),
                  null,
                  new ResultPoint[]{new ResultPoint(left, (float)rowNumber), new ResultPoint(right, (float)rowNumber)},
                  BarcodeFormat.CODABAR
               );
               result.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]F0");
               return result;
            }
         }
      }
   }

   private void validatePattern(int start) throws NotFoundException {
      int[] sizes = new int[]{0, 0, 0, 0};
      int[] counts = new int[]{0, 0, 0, 0};
      int end = this.decodeRowResult.length() - 1;
      int pos = start;

      for (int i = 0; i <= end; i++) {
         int pattern = CHARACTER_ENCODINGS[this.decodeRowResult.charAt(i)];

         for (int j = 6; j >= 0; j--) {
            int category = (j & 1) + (pattern & 1) * 2;
            sizes[category] += this.counters[pos + j];
            counts[category]++;
            pattern >>= 1;
         }

         pos += 8;
      }

      float[] maxes = new float[4];
      float[] mins = new float[4];

      for (int i = 0; i < 2; i++) {
         mins[i] = 0.0F;
         mins[i + 2] = ((float)sizes[i] / (float)counts[i] + (float)sizes[i + 2] / (float)counts[i + 2]) / 2.0F;
         maxes[i] = mins[i + 2];
         maxes[i + 2] = ((float)sizes[i + 2] * 2.0F + 1.5F) / (float)counts[i + 2];
      }

      pos = start;

      for (int i = 0; i <= end; i++) {
         int pattern = CHARACTER_ENCODINGS[this.decodeRowResult.charAt(i)];

         for (int j = 6; j >= 0; j--) {
            int category = (j & 1) + (pattern & 1) * 2;
            int size = this.counters[pos + j];
            if ((float)size < mins[category] || (float)size > maxes[category]) {
               throw NotFoundException.getNotFoundInstance();
            }

            pattern >>= 1;
         }

         pos += 8;
      }
   }

   private void setCounters(BitArray row) throws NotFoundException {
      this.counterLength = 0;
      int i = row.getNextUnset(0);
      int end = row.getSize();
      if (i >= end) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         boolean isWhite = true;

         int count;
         for (count = 0; i < end; i++) {
            if (row.get(i) != isWhite) {
               count++;
            } else {
               this.counterAppend(count);
               count = 1;
               isWhite = !isWhite;
            }
         }

         this.counterAppend(count);
      }
   }

   private void counterAppend(int e) {
      this.counters[this.counterLength] = e;
      this.counterLength++;
      if (this.counterLength >= this.counters.length) {
         int[] temp = new int[this.counterLength * 2];
         System.arraycopy(this.counters, 0, temp, 0, this.counterLength);
         this.counters = temp;
      }
   }

   private int findStartPattern() throws NotFoundException {
      for (int i = 1; i < this.counterLength; i += 2) {
         int charOffset = this.toNarrowWidePattern(i);
         if (charOffset != -1 && arrayContains(STARTEND_ENCODING, ALPHABET[charOffset])) {
            int patternSize = 0;

            for (int j = i; j < i + 7; j++) {
               patternSize += this.counters[j];
            }

            if (i == 1 || this.counters[i - 1] >= patternSize / 2) {
               return i;
            }
         }
      }

      throw NotFoundException.getNotFoundInstance();
   }

   static boolean arrayContains(char[] array, char key) {
      if (array != null) {
         for (char c : array) {
            if (c == key) {
               return true;
            }
         }
      }

      return false;
   }

   private int toNarrowWidePattern(int position) {
      int end = position + 7;
      if (end >= this.counterLength) {
         return -1;
      } else {
         int[] theCounters = this.counters;
         int maxBar = 0;
         int minBar = Integer.MAX_VALUE;

         for (int j = position; j < end; j += 2) {
            int currentCounter = theCounters[j];
            if (currentCounter < minBar) {
               minBar = currentCounter;
            }

            if (currentCounter > maxBar) {
               maxBar = currentCounter;
            }
         }

         int thresholdBar = (minBar + maxBar) / 2;
         int maxSpace = 0;
         int minSpace = Integer.MAX_VALUE;

         for (int j = position + 1; j < end; j += 2) {
            int currentCounterx = theCounters[j];
            if (currentCounterx < minSpace) {
               minSpace = currentCounterx;
            }

            if (currentCounterx > maxSpace) {
               maxSpace = currentCounterx;
            }
         }

         int thresholdSpace = (minSpace + maxSpace) / 2;
         int bitmask = 128;
         int pattern = 0;

         for (int i = 0; i < 7; i++) {
            int threshold = (i & 1) == 0 ? thresholdBar : thresholdSpace;
            bitmask >>= 1;
            if (theCounters[position + i] > threshold) {
               pattern |= bitmask;
            }
         }

         for (int ix = 0; ix < CHARACTER_ENCODINGS.length; ix++) {
            if (CHARACTER_ENCODINGS[ix] == pattern) {
               return ix;
            }
         }

         return -1;
      }
   }
}
