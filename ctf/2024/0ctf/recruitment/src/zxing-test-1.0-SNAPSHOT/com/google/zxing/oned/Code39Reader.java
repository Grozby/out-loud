package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.Arrays;
import java.util.Map;

public final class Code39Reader extends OneDReader {
   static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%";
   static final int[] CHARACTER_ENCODINGS = new int[]{
      52,
      289,
      97,
      352,
      49,
      304,
      112,
      37,
      292,
      100,
      265,
      73,
      328,
      25,
      280,
      88,
      13,
      268,
      76,
      28,
      259,
      67,
      322,
      19,
      274,
      82,
      7,
      262,
      70,
      22,
      385,
      193,
      448,
      145,
      400,
      208,
      133,
      388,
      196,
      168,
      162,
      138,
      42
   };
   static final int ASTERISK_ENCODING = 148;
   private final boolean usingCheckDigit;
   private final boolean extendedMode;
   private final StringBuilder decodeRowResult;
   private final int[] counters;

   public Code39Reader() {
      this(false);
   }

   public Code39Reader(boolean usingCheckDigit) {
      this(usingCheckDigit, false);
   }

   public Code39Reader(boolean usingCheckDigit, boolean extendedMode) {
      this.usingCheckDigit = usingCheckDigit;
      this.extendedMode = extendedMode;
      this.decodeRowResult = new StringBuilder(20);
      this.counters = new int[9];
   }

   @Override
   public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
      int[] theCounters = this.counters;
      Arrays.fill(theCounters, 0);
      StringBuilder result = this.decodeRowResult;
      result.setLength(0);
      int[] start = findAsteriskPattern(row, theCounters);
      int nextStart = row.getNextSet(start[1]);
      int end = row.getSize();

      char decodedChar;
      int lastStart;
      do {
         recordPattern(row, nextStart, theCounters);
         int pattern = toNarrowWidePattern(theCounters);
         if (pattern < 0) {
            throw NotFoundException.getNotFoundInstance();
         }

         decodedChar = patternToChar(pattern);
         result.append(decodedChar);
         lastStart = nextStart;

         for (int counter : theCounters) {
            nextStart += counter;
         }

         nextStart = row.getNextSet(nextStart);
      } while (decodedChar != '*');

      result.setLength(result.length() - 1);
      int lastPatternSize = 0;

      for (int counter : theCounters) {
         lastPatternSize += counter;
      }

      int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;
      if (nextStart != end && whiteSpaceAfterEnd * 2 < lastPatternSize) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         if (this.usingCheckDigit) {
            int max = result.length() - 1;
            int total = 0;

            for (int i = 0; i < max; i++) {
               total += "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".indexOf(this.decodeRowResult.charAt(i));
            }

            if (result.charAt(max) != "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".charAt(total % 43)) {
               throw ChecksumException.getChecksumInstance();
            }

            result.setLength(max);
         }

         if (result.length() == 0) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            String resultString;
            if (this.extendedMode) {
               resultString = decodeExtended(result);
            } else {
               resultString = result.toString();
            }

            float left = (float)(start[1] + start[0]) / 2.0F;
            float right = (float)lastStart + (float)lastPatternSize / 2.0F;
            Result resultObject = new Result(
               resultString, null, new ResultPoint[]{new ResultPoint(left, (float)rowNumber), new ResultPoint(right, (float)rowNumber)}, BarcodeFormat.CODE_39
            );
            resultObject.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]A0");
            return resultObject;
         }
      }
   }

   private static int[] findAsteriskPattern(BitArray row, int[] counters) throws NotFoundException {
      int width = row.getSize();
      int rowOffset = row.getNextSet(0);
      int counterPosition = 0;
      int patternStart = rowOffset;
      boolean isWhite = false;
      int patternLength = counters.length;

      for (int i = rowOffset; i < width; i++) {
         if (row.get(i) != isWhite) {
            counters[counterPosition]++;
         } else {
            if (counterPosition == patternLength - 1) {
               if (toNarrowWidePattern(counters) == 148 && row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
                  return new int[]{patternStart, i};
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

   private static int toNarrowWidePattern(int[] counters) {
      int numCounters = counters.length;
      int maxNarrowCounter = 0;

      int wideCounters;
      do {
         int minCounter = Integer.MAX_VALUE;

         for (int counter : counters) {
            if (counter < minCounter && counter > maxNarrowCounter) {
               minCounter = counter;
            }
         }

         maxNarrowCounter = minCounter;
         wideCounters = 0;
         int totalWideCountersWidth = 0;
         int pattern = 0;

         for (int i = 0; i < numCounters; i++) {
            int counterx = counters[i];
            if (counterx > maxNarrowCounter) {
               pattern |= 1 << numCounters - 1 - i;
               wideCounters++;
               totalWideCountersWidth += counterx;
            }
         }

         if (wideCounters == 3) {
            for (int ix = 0; ix < numCounters && wideCounters > 0; ix++) {
               int counterx = counters[ix];
               if (counterx > maxNarrowCounter) {
                  wideCounters--;
                  if (counterx * 2 >= totalWideCountersWidth) {
                     return -1;
                  }
               }
            }

            return pattern;
         }
      } while (wideCounters > 3);

      return -1;
   }

   private static char patternToChar(int pattern) throws NotFoundException {
      for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
         if (CHARACTER_ENCODINGS[i] == pattern) {
            return "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".charAt(i);
         }
      }

      if (pattern == 148) {
         return '*';
      } else {
         throw NotFoundException.getNotFoundInstance();
      }
   }

   private static String decodeExtended(CharSequence encoded) throws FormatException {
      int length = encoded.length();
      StringBuilder decoded = new StringBuilder(length);

      for (int i = 0; i < length; i++) {
         char c = encoded.charAt(i);
         if (c != '+' && c != '$' && c != '%' && c != '/') {
            decoded.append(c);
         } else {
            char next = encoded.charAt(i + 1);
            char decodedChar = 0;
            switch (c) {
               case '$':
                  if (next < 'A' || next > 'Z') {
                     throw FormatException.getFormatInstance();
                  }

                  decodedChar = (char)(next - '@');
                  break;
               case '%':
                  if (next >= 'A' && next <= 'E') {
                     decodedChar = (char)(next - '&');
                  } else if (next >= 'F' && next <= 'J') {
                     decodedChar = (char)(next - 11);
                  } else if (next >= 'K' && next <= 'O') {
                     decodedChar = (char)(next + 16);
                  } else if (next >= 'P' && next <= 'T') {
                     decodedChar = (char)(next + '+');
                  } else if (next == 'U') {
                     decodedChar = 0;
                  } else if (next == 'V') {
                     decodedChar = '@';
                  } else if (next == 'W') {
                     decodedChar = '`';
                  } else {
                     if (next != 'X' && next != 'Y' && next != 'Z') {
                        throw FormatException.getFormatInstance();
                     }

                     decodedChar = 127;
                  }
                  break;
               case '+':
                  if (next < 'A' || next > 'Z') {
                     throw FormatException.getFormatInstance();
                  }

                  decodedChar = (char)(next + ' ');
                  break;
               case '/':
                  if (next >= 'A' && next <= 'O') {
                     decodedChar = (char)(next - ' ');
                  } else {
                     if (next != 'Z') {
                        throw FormatException.getFormatInstance();
                     }

                     decodedChar = ':';
                  }
            }

            decoded.append(decodedChar);
            i++;
         }
      }

      return decoded.toString();
   }
}
