package com.google.zxing.datamatrix.encoder;

class C40Encoder implements Encoder {
   @Override
   public int getEncodingMode() {
      return 1;
   }

   void encodeMaximal(EncoderContext context) {
      StringBuilder buffer = new StringBuilder();
      int lastCharSize = 0;
      int backtrackStartPosition = context.pos;
      int backtrackBufferLength = 0;

      while (context.hasMoreCharacters()) {
         char c = context.getCurrentChar();
         context.pos++;
         lastCharSize = this.encodeChar(c, buffer);
         if (buffer.length() % 3 == 0) {
            backtrackStartPosition = context.pos;
            backtrackBufferLength = buffer.length();
         }
      }

      if (backtrackBufferLength != buffer.length()) {
         int unwritten = buffer.length() / 3 * 2;
         int curCodewordCount = context.getCodewordCount() + unwritten + 1;
         context.updateSymbolInfo(curCodewordCount);
         int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;
         int rest = buffer.length() % 3;
         if (rest == 2 && available != 2 || rest == 1 && (lastCharSize > 3 || available != 1)) {
            buffer.setLength(backtrackBufferLength);
            context.pos = backtrackStartPosition;
         }
      }

      if (buffer.length() > 0) {
         context.writeCodeword('æ');
      }

      this.handleEOD(context, buffer);
   }

   @Override
   public void encode(EncoderContext context) {
      StringBuilder buffer = new StringBuilder();

      while (context.hasMoreCharacters()) {
         char c = context.getCurrentChar();
         context.pos++;
         int lastCharSize = this.encodeChar(c, buffer);
         int unwritten = buffer.length() / 3 * 2;
         int curCodewordCount = context.getCodewordCount() + unwritten;
         context.updateSymbolInfo(curCodewordCount);
         int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;
         if (!context.hasMoreCharacters()) {
            StringBuilder removed = new StringBuilder();
            if (buffer.length() % 3 == 2 && available != 2) {
               lastCharSize = this.backtrackOneCharacter(context, buffer, removed, lastCharSize);
            }

            while (buffer.length() % 3 == 1 && (lastCharSize > 3 || available != 1)) {
               lastCharSize = this.backtrackOneCharacter(context, buffer, removed, lastCharSize);
            }
            break;
         }

         int count = buffer.length();
         if (count % 3 == 0) {
            int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, this.getEncodingMode());
            if (newMode != this.getEncodingMode()) {
               context.signalEncoderChange(0);
               break;
            }
         }
      }

      this.handleEOD(context, buffer);
   }

   private int backtrackOneCharacter(EncoderContext context, StringBuilder buffer, StringBuilder removed, int lastCharSize) {
      int count = buffer.length();
      buffer.delete(count - lastCharSize, count);
      context.pos--;
      char c = context.getCurrentChar();
      lastCharSize = this.encodeChar(c, removed);
      context.resetSymbolInfo();
      return lastCharSize;
   }

   static void writeNextTriplet(EncoderContext context, StringBuilder buffer) {
      context.writeCodewords(encodeToCodewords(buffer));
      buffer.delete(0, 3);
   }

   void handleEOD(EncoderContext context, StringBuilder buffer) {
      int unwritten = buffer.length() / 3 * 2;
      int rest = buffer.length() % 3;
      int curCodewordCount = context.getCodewordCount() + unwritten;
      context.updateSymbolInfo(curCodewordCount);
      int available = context.getSymbolInfo().getDataCapacity() - curCodewordCount;
      if (rest == 2) {
         buffer.append('\u0000');

         while (buffer.length() >= 3) {
            writeNextTriplet(context, buffer);
         }

         if (context.hasMoreCharacters()) {
            context.writeCodeword('þ');
         }
      } else if (available == 1 && rest == 1) {
         while (buffer.length() >= 3) {
            writeNextTriplet(context, buffer);
         }

         if (context.hasMoreCharacters()) {
            context.writeCodeword('þ');
         }

         context.pos--;
      } else {
         if (rest != 0) {
            throw new IllegalStateException("Unexpected case. Please report!");
         }

         while (buffer.length() >= 3) {
            writeNextTriplet(context, buffer);
         }

         if (available > 0 || context.hasMoreCharacters()) {
            context.writeCodeword('þ');
         }
      }

      context.signalEncoderChange(0);
   }

   int encodeChar(char c, StringBuilder sb) {
      if (c == ' ') {
         sb.append('\u0003');
         return 1;
      } else if (c >= '0' && c <= '9') {
         sb.append((char)(c - '0' + 4));
         return 1;
      } else if (c >= 'A' && c <= 'Z') {
         sb.append((char)(c - 'A' + 14));
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
      } else if (c <= '_') {
         sb.append('\u0001');
         sb.append((char)(c - '[' + 22));
         return 2;
      } else if (c <= 127) {
         sb.append('\u0002');
         sb.append((char)(c - '`'));
         return 2;
      } else {
         sb.append("\u0001\u001e");
         int len = 2;
         return len + this.encodeChar((char)(c - 128), sb);
      }
   }

   private static String encodeToCodewords(CharSequence sb) {
      int v = 1600 * sb.charAt(0) + '(' * sb.charAt(1) + sb.charAt(2) + 1;
      char cw1 = (char)(v / 256);
      char cw2 = (char)(v % 256);
      return new String(new char[]{cw1, cw2});
   }
}
