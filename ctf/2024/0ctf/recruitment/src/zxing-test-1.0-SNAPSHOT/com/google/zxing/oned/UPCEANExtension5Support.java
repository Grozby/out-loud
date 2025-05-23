package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.EnumMap;
import java.util.Map;

final class UPCEANExtension5Support {
   private static final int[] CHECK_DIGIT_ENCODINGS = new int[]{24, 20, 18, 17, 12, 6, 3, 10, 9, 5};
   private final int[] decodeMiddleCounters = new int[4];
   private final StringBuilder decodeRowStringBuffer = new StringBuilder();

   Result decodeRow(int rowNumber, BitArray row, int[] extensionStartRange) throws NotFoundException {
      StringBuilder result = this.decodeRowStringBuffer;
      result.setLength(0);
      int end = this.decodeMiddle(row, extensionStartRange, result);
      String resultString = result.toString();
      Map<ResultMetadataType, Object> extensionData = parseExtensionString(resultString);
      Result extensionResult = new Result(
         resultString,
         null,
         new ResultPoint[]{
            new ResultPoint((float)(extensionStartRange[0] + extensionStartRange[1]) / 2.0F, (float)rowNumber), new ResultPoint((float)end, (float)rowNumber)
         },
         BarcodeFormat.UPC_EAN_EXTENSION
      );
      if (extensionData != null) {
         extensionResult.putAllMetadata(extensionData);
      }

      return extensionResult;
   }

   private int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString) throws NotFoundException {
      int[] counters = this.decodeMiddleCounters;
      counters[0] = 0;
      counters[1] = 0;
      counters[2] = 0;
      counters[3] = 0;
      int end = row.getSize();
      int rowOffset = startRange[1];
      int lgPatternFound = 0;

      for (int x = 0; x < 5 && rowOffset < end; x++) {
         int bestMatch = UPCEANReader.decodeDigit(row, counters, rowOffset, UPCEANReader.L_AND_G_PATTERNS);
         resultString.append((char)(48 + bestMatch % 10));

         for (int counter : counters) {
            rowOffset += counter;
         }

         if (bestMatch >= 10) {
            lgPatternFound |= 1 << 4 - x;
         }

         if (x != 4) {
            rowOffset = row.getNextSet(rowOffset);
            rowOffset = row.getNextUnset(rowOffset);
         }
      }

      if (resultString.length() != 5) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         int checkDigit = determineCheckDigit(lgPatternFound);
         if (extensionChecksum(resultString.toString()) != checkDigit) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            return rowOffset;
         }
      }
   }

   private static int extensionChecksum(CharSequence s) {
      int length = s.length();
      int sum = 0;

      for (int i = length - 2; i >= 0; i -= 2) {
         sum += s.charAt(i) - '0';
      }

      sum *= 3;

      for (int i = length - 1; i >= 0; i -= 2) {
         sum += s.charAt(i) - '0';
      }

      sum *= 3;
      return sum % 10;
   }

   private static int determineCheckDigit(int lgPatternFound) throws NotFoundException {
      for (int d = 0; d < 10; d++) {
         if (lgPatternFound == CHECK_DIGIT_ENCODINGS[d]) {
            return d;
         }
      }

      throw NotFoundException.getNotFoundInstance();
   }

   private static Map<ResultMetadataType, Object> parseExtensionString(String raw) {
      if (raw.length() != 5) {
         return null;
      } else {
         Object value = parseExtension5String(raw);
         if (value == null) {
            return null;
         } else {
            Map<ResultMetadataType, Object> result = new EnumMap<>(ResultMetadataType.class);
            result.put(ResultMetadataType.SUGGESTED_PRICE, value);
            return result;
         }
      }
   }

   private static String parseExtension5String(String raw) {
      String currency;
      label33:
      switch (raw.charAt(0)) {
         case '0':
            currency = "£";
            break;
         case '5':
            currency = "$";
            break;
         case '9':
            switch (raw) {
               case "90000":
                  return null;
               case "99991":
                  return "0.00";
               case "99990":
                  return "Used";
               default:
                  currency = "";
                  break label33;
            }
         default:
            currency = "";
      }

      int rawAmount = Integer.parseInt(raw.substring(1));
      String unitsString = String.valueOf(rawAmount / 100);
      int hundredths = rawAmount % 100;
      String hundredthsString = hundredths < 10 ? "0" + hundredths : String.valueOf(hundredths);
      return currency + unitsString + '.' + hundredthsString;
   }
}
