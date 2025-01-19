package com.google.zxing.datamatrix.encoder;

final class TextEncoder extends C40Encoder {
   @Override
   public int getEncodingMode() {
      return 2;
   }

   @Override
   int encodeChar(char c, StringBuilder sb) {
      if (c == ' ') {
         sb.append('\u0003');
         return 1;
      } else if (c >= '0' && c <= '9') {
         sb.append((char)(c - '0' + 4));
         return 1;
      } else if (c >= 'a' && c <= 'z') {
         sb.append((char)(c - 'a' + 14));
         return 1;
      } else if (c < ' ') {
         sb.append('\u0000');
         sb.append(c);
         return 2;
      } else if (c <= '/') {
         sb.append('\u0001');
         sb.append((char)(c - '!'));
         return 2;
      } else if (c <= '@') {
         sb.append('\u0001');
         sb.append((char)(c - ':' + 15));
         return 2;
      } else if (c >= '[' && c <= '_') {
         sb.append('\u0001');
         sb.append((char)(c - '[' + 22));
         return 2;
      } else if (c == '`') {
         sb.append('\u0002');
         sb.append('\u0000');
         return 2;
      } else if (c <= 'Z') {
         sb.append('\u0002');
         sb.append((char)(c - 'A' + 1));
         return 2;
      } else if (c <= 127) {
         sb.append('\u0002');
         sb.append((char)(c - '{' + 27));
         return 2;
      } else {
         sb.append("\u0001\u001e");
         int len = 2;
         return len + this.encodeChar((char)(c - 128), sb);
      }
   }
}
