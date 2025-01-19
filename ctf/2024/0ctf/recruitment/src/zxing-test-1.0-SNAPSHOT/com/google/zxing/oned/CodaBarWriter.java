package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import java.util.Collection;
import java.util.Collections;

public final class CodaBarWriter extends OneDimensionalCodeWriter {
   private static final char[] START_END_CHARS = new char[]{'A', 'B', 'C', 'D'};
   private static final char[] ALT_START_END_CHARS = new char[]{'T', 'N', '*', 'E'};
   private static final char[] CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED = new char[]{'/', ':', '+', '.'};
   private static final char DEFAULT_GUARD = START_END_CHARS[0];

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.CODABAR);
   }

   @Override
   public boolean[] encode(String contents) {
      if (contents.length() < 2) {
         contents = DEFAULT_GUARD + contents + DEFAULT_GUARD;
      } else {
         char firstChar = Character.toUpperCase(contents.charAt(0));
         char lastChar = Character.toUpperCase(contents.charAt(contents.length() - 1));
         boolean startsNormal = CodaBarReader.arrayContains(START_END_CHARS, firstChar);
         boolean endsNormal = CodaBarReader.arrayContains(START_END_CHARS, lastChar);
         boolean startsAlt = CodaBarReader.arrayContains(ALT_START_END_CHARS, firstChar);
         boolean endsAlt = CodaBarReader.arrayContains(ALT_START_END_CHARS, lastChar);
         if (startsNormal) {
            if (!endsNormal) {
               throw new IllegalArgumentException("Invalid start/end guards: " + contents);
            }
         } else if (startsAlt) {
            if (!endsAlt) {
               throw new IllegalArgumentException("Invalid start/end guards: " + contents);
            }
         } else {
            if (endsNormal || endsAlt) {
               throw new IllegalArgumentException("Invalid start/end guards: " + contents);
            }

            contents = DEFAULT_GUARD + contents + DEFAULT_GUARD;
         }
      }

      int resultLength = 20;

      for (int i = 1; i < contents.length() - 1; i++) {
         if (!Character.isDigit(contents.charAt(i)) && contents.charAt(i) != '-' && contents.charAt(i) != '$') {
            if (!CodaBarReader.arrayContains(CHARS_WHICH_ARE_TEN_LENGTH_EACH_AFTER_DECODED, contents.charAt(i))) {
               throw new IllegalArgumentException("Cannot encode : '" + contents.charAt(i) + '\'');
            }

            resultLength += 10;
         } else {
            resultLength += 9;
         }
      }

      resultLength += contents.length() - 1;
      boolean[] result = new boolean[resultLength];
      int position = 0;

      for (int index = 0; index < contents.length(); index++) {
         char c = Character.toUpperCase(contents.charAt(index));
         if (index == 0 || index == contents.length() - 1) {
            switch (c) {
               case '*':
                  c = 'C';
                  break;
               case 'E':
                  c = 'D';
                  break;
               case 'N':
                  c = 'B';
                  break;
               case 'T':
                  c = 'A';
            }
         }

         int code = 0;

         for (int ix = 0; ix < CodaBarReader.ALPHABET.length; ix++) {
            if (c == CodaBarReader.ALPHABET[ix]) {
               code = CodaBarReader.CHARACTER_ENCODINGS[ix];
               break;
            }
         }

         boolean color = true;
         int counter = 0;
         int bit = 0;

         while (bit < 7) {
            result[position] = color;
            position++;
            if ((code >> 6 - bit & 1) != 0 && counter != 1) {
               counter++;
            } else {
               color = !color;
               bit++;
               counter = 0;
            }
         }

         if (index < contents.length() - 1) {
            result[position] = false;
            position++;
         }
      }

      return result;
   }
}
