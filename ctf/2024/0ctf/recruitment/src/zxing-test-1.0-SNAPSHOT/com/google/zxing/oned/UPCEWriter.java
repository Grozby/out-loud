package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.FormatException;
import java.util.Collection;
import java.util.Collections;

public final class UPCEWriter extends UPCEANWriter {
   private static final int CODE_WIDTH = 51;

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.UPC_E);
   }

   @Override
   public boolean[] encode(String contents) {
      int length = contents.length();
      switch (length) {
         case 7:
            int check;
            try {
               check = UPCEANReader.getStandardUPCEANChecksum(UPCEReader.convertUPCEtoUPCA(contents));
            } catch (FormatException var11) {
               throw new IllegalArgumentException(var11);
            }

            contents = contents + check;
            break;
         case 8:
            try {
               if (!UPCEANReader.checkStandardUPCEANChecksum(UPCEReader.convertUPCEtoUPCA(contents))) {
                  throw new IllegalArgumentException("Contents do not pass checksum");
               }
               break;
            } catch (FormatException var10) {
               throw new IllegalArgumentException("Illegal contents");
            }
         default:
            throw new IllegalArgumentException("Requested contents should be 7 or 8 digits long, but got " + length);
      }

      checkNumeric(contents);
      int firstDigit = Character.digit(contents.charAt(0), 10);
      if (firstDigit != 0 && firstDigit != 1) {
         throw new IllegalArgumentException("Number system must be 0 or 1");
      } else {
         int checkDigit = Character.digit(contents.charAt(7), 10);
         int parities = UPCEReader.NUMSYS_AND_CHECK_DIGIT_PATTERNS[firstDigit][checkDigit];
         boolean[] result = new boolean[51];
         int pos = appendPattern(result, 0, UPCEANReader.START_END_PATTERN, true);

         for (int i = 1; i <= 6; i++) {
            int digit = Character.digit(contents.charAt(i), 10);
            if ((parities >> 6 - i & 1) == 1) {
               digit += 10;
            }

            pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], false);
         }

         appendPattern(result, pos, UPCEANReader.END_PATTERN, false);
         return result;
      }
   }
}
