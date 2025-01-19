package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.Map;

public final class ITFReader extends OneDReader {
   private static final float MAX_AVG_VARIANCE = 0.38F;
   private static final float MAX_INDIVIDUAL_VARIANCE = 0.5F;
   private static final int W = 3;
   private static final int w = 2;
   private static final int N = 1;
   private static final int[] DEFAULT_ALLOWED_LENGTHS = new int[]{6, 8, 10, 12, 14};
   private int narrowLineWidth = -1;
   private static final int[] START_PATTERN = new int[]{1, 1, 1, 1};
   private static final int[][] END_PATTERN_REVERSED = new int[][]{{1, 1, 2}, {1, 1, 3}};
   private static final int[][] PATTERNS = new int[][]{
      {1, 1, 2, 2, 1},
      {2, 1, 1, 1, 2},
      {1, 2, 1, 1, 2},
      {2, 2, 1, 1, 1},
      {1, 1, 2, 1, 2},
      {2, 1, 2, 1, 1},
      {1, 2, 2, 1, 1},
      {1, 1, 1, 2, 2},
      {2, 1, 1, 2, 1},
      {1, 2, 1, 2, 1},
      {1, 1, 3, 3, 1},
      {3, 1, 1, 1, 3},
      {1, 3, 1, 1, 3},
      {3, 3, 1, 1, 1},
      {1, 1, 3, 1, 3},
      {3, 1, 3, 1, 1},
      {1, 3, 3, 1, 1},
      {1, 1, 1, 3, 3},
      {3, 1, 1, 3, 1},
      {1, 3, 1, 3, 1}
   };

   @Override
   public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws FormatException, NotFoundException {
      int[] startRange = this.decodeStart(row);
      int[] endRange = this.decodeEnd(row);
      StringBuilder result = new StringBuilder(20);
      decodeMiddle(row, startRange[1], endRange[0], result);
      String resultString = result.toString();
      int[] allowedLengths = null;
      if (hints != null) {
         allowedLengths = (int[])hints.get(DecodeHintType.ALLOWED_LENGTHS);
      }

      if (allowedLengths == null) {
         allowedLengths = DEFAULT_ALLOWED_LENGTHS;
      }

      int length = resultString.length();
      boolean lengthOK = false;
      int maxAllowedLength = 0;

      for (int allowedLength : allowedLengths) {
         if (length == allowedLength) {
            lengthOK = true;
            break;
         }

         if (allowedLength > maxAllowedLength) {
            maxAllowedLength = allowedLength;
         }
      }

      if (!lengthOK && length > maxAllowedLength) {
         lengthOK = true;
      }

      if (!lengthOK) {
         throw FormatException.getFormatInstance();
      } else {
         Result resultObject = new Result(
            resultString,
            null,
            new ResultPoint[]{new ResultPoint((float)startRange[1], (float)rowNumber), new ResultPoint((float)endRange[0], (float)rowNumber)},
            BarcodeFormat.ITF
         );
         resultObject.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]I0");
         return resultObject;
      }
   }

   private static void decodeMiddle(BitArray row, int payloadStart, int payloadEnd, StringBuilder resultString) throws NotFoundException {
      int[] counterDigitPair = new int[10];
      int[] counterBlack = new int[5];
      int[] counterWhite = new int[5];

      while (payloadStart < payloadEnd) {
         recordPattern(row, payloadStart, counterDigitPair);

         for (int k = 0; k < 5; k++) {
            int twoK = 2 * k;
            counterBlack[k] = counterDigitPair[twoK];
            counterWhite[k] = counterDigitPair[twoK + 1];
         }

         int bestMatch = decodeDigit(counterBlack);
         resultString.append((char)(48 + bestMatch));
         bestMatch = decodeDigit(counterWhite);
         resultString.append((char)(48 + bestMatch));

         for (int counterDigit : counterDigitPair) {
            payloadStart += counterDigit;
         }
      }
   }

   private int[] decodeStart(BitArray row) throws NotFoundException {
      int endStart = skipWhiteSpace(row);
      int[] startPattern = findGuardPattern(row, endStart, START_PATTERN);
      this.narrowLineWidth = (startPattern[1] - startPattern[0]) / 4;
      this.validateQuietZone(row, startPattern[0]);
      return startPattern;
   }

   private void validateQuietZone(BitArray row, int startPattern) throws NotFoundException {
      int quietCount = this.narrowLineWidth * 10;
      quietCount = Math.min(quietCount, startPattern);

      for (int i = startPattern - 1; quietCount > 0 && i >= 0 && !row.get(i); i--) {
         quietCount--;
      }

      if (quietCount != 0) {
         throw NotFoundException.getNotFoundInstance();
      }
   }

   private static int skipWhiteSpace(BitArray row) throws NotFoundException {
      int width = row.getSize();
      int endStart = row.getNextSet(0);
      if (endStart == width) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         return endStart;
      }
   }

   private int[] decodeEnd(BitArray row) throws NotFoundException {
      row.reverse();

      int[] var5;
      try {
         int endStart = skipWhiteSpace(row);

         int[] endPattern;
         try {
            endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED[0]);
         } catch (NotFoundException var9) {
            endPattern = findGuardPattern(row, endStart, END_PATTERN_REVERSED[1]);
         }

         this.validateQuietZone(row, endPattern[0]);
         int temp = endPattern[0];
         endPattern[0] = row.getSize() - endPattern[1];
         endPattern[1] = row.getSize() - temp;
         var5 = endPattern;
      } finally {
         row.reverse();
      }

      return var5;
   }

   private static int[] findGuardPattern(BitArray row, int rowOffset, int[] pattern) throws NotFoundException {
      int patternLength = pattern.length;
      int[] counters = new int[patternLength];
      int width = row.getSize();
      boolean isWhite = false;
      int counterPosition = 0;
      int patternStart = rowOffset;

      for (int x = rowOffset; x < width; x++) {
         if (row.get(x) != isWhite) {
            counters[counterPosition]++;
         } else {
            if (counterPosition == patternLength - 1) {
               if (patternMatchVariance(counters, pattern, 0.5F) < 0.38F) {
                  return new int[]{patternStart, x};
               }

               patternStart += counters[0] + counters[1];
               System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
               counters[counterPosition - 1] = 0;
               counters[counterPosition] = 0;
               counterPosition--;
            } else {
               counterPosition++;
            }

            counters[counterPosition] = 1;
            isWhite = !isWhite;
         }
      }

      throw NotFoundException.getNotFoundInstance();
   }

   private static int decodeDigit(int[] counters) throws NotFoundException {
      float bestVariance = 0.38F;
      int bestMatch = -1;
      int max = PATTERNS.length;

      for (int i = 0; i < max; i++) {
         int[] pattern = PATTERNS[i];
         float variance = patternMatchVariance(counters, pattern, 0.5F);
         if (variance < bestVariance) {
            bestVariance = variance;
            bestMatch = i;
         } else if (variance == bestVariance) {
            bestMatch = -1;
         }
      }

      if (bestMatch >= 0) {
         return bestMatch % 10;
      } else {
         throw NotFoundException.getNotFoundInstance();
      }
   }
}
