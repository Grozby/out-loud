package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.FormatException;
import java.util.Collection;
import java.util.Collections;

public final class EAN8Writer extends UPCEANWriter {
   private static final int CODE_WIDTH = 67;

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.EAN_8);
   }

   @Override
   public boolean[] encode(String contents) {
      int length = contents.length();
      switch (length) {
         case 7:
            int check;
            try {
               check = UPCEANReader.getStandardUPCEANChecksum(contents);
            } catch (FormatException var8) {
               throw new IllegalArgumentException(var8);
            }

            contents = contents + check;
            break;
         case 8:
            try {
               if (!UPCEANReader.checkStandardUPCEANChecksum(contents)) {
                  throw new IllegalArgumentException("Contents do not pass checksum");
               }
               break;
            } catch (FormatException var7) {
               throw new IllegalArgumentException("Illegal contents");
            }
         default:
            throw new IllegalArgumentException("Requested contents should be 7 or 8 digits long, but got " + length);
      }

      checkNumeric(contents);
      boolean[] result = new boolean[67];
      int pos = 0;
      pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);

      for (int i = 0; i <= 3; i++) {
         int digit = Character.digit(contents.charAt(i), 10);
         pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], false);
      }

      pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, false);

      for (int i = 4; i <= 7; i++) {
         int digit = Character.digit(contents.charAt(i), 10);
         pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], true);
      }

      appendPattern(result, pos, UPCEANReader.START_END_PATTERN, true);
      return result;
   }
}
