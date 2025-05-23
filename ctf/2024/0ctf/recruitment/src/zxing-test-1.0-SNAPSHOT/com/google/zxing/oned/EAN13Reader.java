package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

public final class EAN13Reader extends UPCEANReader {
   static final int[] FIRST_DIGIT_ENCODINGS = new int[]{0, 11, 13, 14, 19, 25, 28, 21, 22, 26};
   private final int[] decodeMiddleCounters = new int[4];

   @Override
   protected int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString) throws NotFoundException {
      int[] counters = this.decodeMiddleCounters;
      counters[0] = 0;
      counters[1] = 0;
      counters[2] = 0;
      counters[3] = 0;
      int end = row.getSize();
      int rowOffset = startRange[1];
      int lgPatternFound = 0;

      for (int x = 0; x < 6 && rowOffset < end; x++) {
         int bestMatch = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
         resultString.append((char)(48 + bestMatch % 10));

         for (int counter : counters) {
            rowOffset += counter;
         }

         if (bestMatch >= 10) {
            lgPatternFound |= 1 << 5 - x;
         }
      }

      determineFirstDigit(resultString, lgPatternFound);
      int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
      rowOffset = middleRange[1];

      for (int x = 0; x < 6 && rowOffset < end; x++) {
         int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
         resultString.append((char)(48 + bestMatch));

         for (int counter : counters) {
            rowOffset += counter;
         }
      }

      return rowOffset;
   }

   @Override
   BarcodeFormat getBarcodeFormat() {
      return BarcodeFormat.EAN_13;
   }

   private static void determineFirstDigit(StringBuilder resultString, int lgPatternFound) throws NotFoundException {
      for (int d = 0; d < 10; d++) {
         if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
            resultString.insert(0, (char)(48 + d));
            return;
         }
      }

      throw NotFoundException.getNotFoundInstance();
   }
}
