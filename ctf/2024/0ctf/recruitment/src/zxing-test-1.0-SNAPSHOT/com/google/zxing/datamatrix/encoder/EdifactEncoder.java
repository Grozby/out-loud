package com.google.zxing.datamatrix.encoder;

final class EdifactEncoder implements Encoder {
   @Override
   public int getEncodingMode() {
      return 4;
   }

   @Override
   public void encode(EncoderContext context) {
      StringBuilder buffer = new StringBuilder();

      while (context.hasMoreCharacters()) {
         char c = context.getCurrentChar();
         encodeChar(c, buffer);
         context.pos++;
         int count = buffer.length();
         if (count >= 4) {
            context.writeCodewords(encodeToCodewords(buffer));
            buffer.delete(0, 4);
            int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, this.getEncodingMode());
            if (newMode != this.getEncodingMode()) {
               context.signalEncoderChange(0);
               break;
            }
         }
      }

      buffer.append('\u001f');
      handleEOD(context, buffer);
   }

   private static void handleEOD(EncoderContext context, CharSequence buffer) {
      try {
         int count = buffer.length();
         if (count != 0) {
            if (count == 1) {
               context.updateSymbolInfo();
               int available = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
               int remaining = context.getRemainingCharacters();
               if (remaining > available) {
                  context.updateSymbolInfo(context.getCodewordCount() + 1);
                  available = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
               }

               if (remaining <= available && available <= 2) {
                  return;
               }
            }

            if (count > 4) {
               throw new IllegalStateException("Count must not exceed 4");
            } else {
               int restChars = count - 1;
               String encoded = encodeToCodewords(buffer);
               boolean endOfSymbolReached = !context.hasMoreCharacters();
               boolean restInAscii = endOfSymbolReached && restChars <= 2;
               if (restChars <= 2) {
                  context.updateSymbolInfo(context.getCodewordCount() + restChars);
                  int availablex = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
                  if (availablex >= 3) {
                     restInAscii = false;
                     context.updateSymbolInfo(context.getCodewordCount() + encoded.length());
                  }
               }

               if (restInAscii) {
                  context.resetSymbolInfo();
                  context.pos -= restChars;
               } else {
                  context.writeCodewords(encoded);
               }
            }
         }
      } finally {
         context.signalEncoderChange(0);
      }
   }

   private static void encodeChar(char c, StringBuilder sb) {
      if (c >= ' ' && c <= '?') {
         sb.append(c);
      } else if (c >= '@' && c <= '^') {
         sb.append((char)(c - '@'));
      } else {
         HighLevelEncoder.illegalCharacter(c);
      }
   }

   private static String encodeToCodewords(CharSequence sb) {
      int len = sb.length();
      if (len == 0) {
         throw new IllegalStateException("StringBuilder must not be empty");
      } else {
         char c1 = sb.charAt(0);
         char c2 = len >= 2 ? sb.charAt(1) : 0;
         char c3 = len >= 3 ? sb.charAt(2) : 0;
         char c4 = len >= 4 ? sb.charAt(3) : 0;
         int v = (c1 << 18) + (c2 << '\f') + (c3 << 6) + c4;
         char cw1 = (char)(v >> 16 & 0xFF);
         char cw2 = (char)(v >> 8 & 0xFF);
         char cw3 = (char)(v & 0xFF);
         StringBuilder res = new StringBuilder(3);
         res.append(cw1);
         if (len >= 2) {
            res.append(cw2);
         }

         if (len >= 3) {
            res.append(cw3);
         }

         return res.toString();
      }
   }
}
