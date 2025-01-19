package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import java.util.Collection;
import java.util.Collections;

public class Code93Writer extends OneDimensionalCodeWriter {
   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.CODE_93);
   }

   @Override
   public boolean[] encode(String contents) {
      contents = convertToExtended(contents);
      int length = contents.length();
      if (length > 80) {
         throw new IllegalArgumentException("Requested contents should be less than 80 digits long after converting to extended encoding, but got " + length);
      } else {
         int codeWidth = (contents.length() + 2 + 2) * 9 + 1;
         boolean[] result = new boolean[codeWidth];
         int pos = appendPattern(result, 0, Code93Reader.ASTERISK_ENCODING);

         for (int i = 0; i < length; i++) {
            int indexInString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*".indexOf(contents.charAt(i));
            pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[indexInString]);
         }

         int check1 = computeChecksumIndex(contents, 20);
         pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[check1]);
         contents = contents + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*".charAt(check1);
         int check2 = computeChecksumIndex(contents, 15);
         pos += appendPattern(result, pos, Code93Reader.CHARACTER_ENCODINGS[check2]);
         pos += appendPattern(result, pos, Code93Reader.ASTERISK_ENCODING);
         result[pos] = true;
         return result;
      }
   }

   @Deprecated
   protected static int appendPattern(boolean[] target, int pos, int[] pattern, boolean startColor) {
      for (int bit : pattern) {
         target[pos++] = bit != 0;
      }

      return 9;
   }

   private static int appendPattern(boolean[] target, int pos, int a) {
      for (int i = 0; i < 9; i++) {
         int temp = a & 1 << 8 - i;
         target[pos + i] = temp != 0;
      }

      return 9;
   }

   private static int computeChecksumIndex(String contents, int maxWeight) {
      int weight = 1;
      int total = 0;

      for (int i = contents.length() - 1; i >= 0; i--) {
         int indexInString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*".indexOf(contents.charAt(i));
         total += indexInString * weight;
         if (++weight > maxWeight) {
            weight = 1;
         }
      }

      return total % 47;
   }

   static String convertToExtended(String contents) {
      int length = contents.length();
      StringBuilder extendedContent = new StringBuilder(length * 2);

      for (int i = 0; i < length; i++) {
         char character = contents.charAt(i);
         if (character == 0) {
            extendedContent.append("bU");
         } else if (character <= 26) {
            extendedContent.append('a');
            extendedContent.append((char)('A' + character - 1));
         } else if (character <= 31) {
            extendedContent.append('b');
            extendedContent.append((char)('A' + character - 27));
         } else if (character == ' ' || character == '$' || character == '%' || character == '+') {
            extendedContent.append(character);
         } else if (character <= ',') {
            extendedContent.append('c');
            extendedContent.append((char)('A' + character - 33));
         } else if (character <= '9') {
            extendedContent.append(character);
         } else if (character == ':') {
            extendedContent.append("cZ");
         } else if (character <= '?') {
            extendedContent.append('b');
            extendedContent.append((char)('F' + character - 59));
         } else if (character == '@') {
            extendedContent.append("bV");
         } else if (character <= 'Z') {
            extendedContent.append(character);
         } else if (character <= '_') {
            extendedContent.append('b');
            extendedContent.append((char)('K' + character - 91));
         } else if (character == '`') {
            extendedContent.append("bW");
         } else if (character <= 'z') {
            extendedContent.append('d');
            extendedContent.append((char)('A' + character - 97));
         } else {
            if (character > 127) {
               throw new IllegalArgumentException("Requested content contains a non-encodable character: '" + character + "'");
            }

            extendedContent.append('b');
            extendedContent.append((char)('P' + character - 123));
         }
      }

      return extendedContent.toString();
   }
}
