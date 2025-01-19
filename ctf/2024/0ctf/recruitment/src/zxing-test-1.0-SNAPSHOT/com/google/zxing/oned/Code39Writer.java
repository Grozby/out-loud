package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import java.util.Collection;
import java.util.Collections;

public final class Code39Writer extends OneDimensionalCodeWriter {
   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.CODE_39);
   }

   @Override
   public boolean[] encode(String contents) {
      int length = contents.length();
      if (length > 80) {
         throw new IllegalArgumentException("Requested contents should be less than 80 digits long, but got " + length);
      } else {
         for (int i = 0; i < length; i++) {
            int indexInString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".indexOf(contents.charAt(i));
            if (indexInString < 0) {
               contents = tryToConvertToExtendedMode(contents);
               length = contents.length();
               if (length > 80) {
                  throw new IllegalArgumentException("Requested contents should be less than 80 digits long, but got " + length + " (extended full ASCII mode)");
               }
               break;
            }
         }

         int[] widths = new int[9];
         int codeWidth = 25 + 13 * length;
         boolean[] result = new boolean[codeWidth];
         toIntArray(148, widths);
         int pos = appendPattern(result, 0, widths, true);
         int[] narrowWhite = new int[]{1};
         pos += appendPattern(result, pos, narrowWhite, false);

         for (int ix = 0; ix < length; ix++) {
            int indexInString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".indexOf(contents.charAt(ix));
            toIntArray(Code39Reader.CHARACTER_ENCODINGS[indexInString], widths);
            pos += appendPattern(result, pos, widths, true);
            pos += appendPattern(result, pos, narrowWhite, false);
         }

         toIntArray(148, widths);
         appendPattern(result, pos, widths, true);
         return result;
      }
   }

   private static void toIntArray(int a, int[] toReturn) {
      for (int i = 0; i < 9; i++) {
         int temp = a & 1 << 8 - i;
         toReturn[i] = temp == 0 ? 1 : 2;
      }
   }

   private static String tryToConvertToExtendedMode(String contents) {
      int length = contents.length();
      StringBuilder extendedContent = new StringBuilder();

      for (int i = 0; i < length; i++) {
         char character = contents.charAt(i);
         switch (character) {
            case '\u0000':
               extendedContent.append("%U");
               break;
            case ' ':
            case '-':
            case '.':
               extendedContent.append(character);
               break;
            case '@':
               extendedContent.append("%V");
               break;
            case '`':
               extendedContent.append("%W");
               break;
            default:
               if (character <= 26) {
                  extendedContent.append('$');
                  extendedContent.append((char)(65 + (character - 1)));
               } else if (character < ' ') {
                  extendedContent.append('%');
                  extendedContent.append((char)(65 + (character - 27)));
               } else if (character <= ',' || character == '/' || character == ':') {
                  extendedContent.append('/');
                  extendedContent.append((char)(65 + (character - '!')));
               } else if (character <= '9') {
                  extendedContent.append((char)(48 + (character - '0')));
               } else if (character <= '?') {
                  extendedContent.append('%');
                  extendedContent.append((char)(70 + (character - ';')));
               } else if (character <= 'Z') {
                  extendedContent.append((char)(65 + (character - 'A')));
               } else if (character <= '_') {
                  extendedContent.append('%');
                  extendedContent.append((char)(75 + (character - '[')));
               } else if (character <= 'z') {
                  extendedContent.append('+');
                  extendedContent.append((char)(65 + (character - 'a')));
               } else {
                  if (character > 127) {
                     throw new IllegalArgumentException("Requested content contains a non-encodable character: '" + contents.charAt(i) + "'");
                  }

                  extendedContent.append('%');
                  extendedContent.append((char)(80 + (character - '{')));
               }
         }
      }

      return extendedContent.toString();
   }
}
