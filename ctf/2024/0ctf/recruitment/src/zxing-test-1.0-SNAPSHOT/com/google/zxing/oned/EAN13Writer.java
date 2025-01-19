package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.FormatException;
import java.util.Collection;
import java.util.Collections;

public final class EAN13Writer extends UPCEANWriter {
   private static final int CODE_WIDTH = 95;

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.EAN_13);
   }

   @Override
   public boolean[] encode(String contents) {
      int length = contents.length();
      switch (length) {
         case 12:
            int check;
            try {
               check = UPCEANReader.getStandardUPCEANChecksum(contents);
            } catch (FormatException var10) {
               throw new IllegalArgumentException(var10);
            }

            contents = contents + check;
            break;
         case 13:
            try {
               if (!UPCEANReader.checkStandardUPCEANChecksum(contents)) {
                  throw new IllegalArgumentException("Contents do not pass checksum");
               }
               break;
            } catch (FormatException var9) {
               throw new IllegalArgumentException("Illegal contents");
            }
         default:
            throw new IllegalArgumentException("Requested contents should be 12 or 13 digits long, but got " + length);
      }

      checkNumeric(contents);
      int firstDigit = Character.digit(contents.charAt(0), 10);
      int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
      boolean[] result = new boolean[95];
      int pos = 0;
      pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);

      for (int i = 1; i <= 6; i++) {
         int digit = Character.digit(contents.charAt(i), 10);
         if ((parities >> 6 - i & 1) == 1) {
            digit += 10;
         }

         pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], false);
      }

      pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, false);

      for (int i = 7; i <= 12; i++) {
         int digit = Character.digit(contents.charAt(i), 10);
         pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], true);
      }

      appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);
      return result;
   }
}
