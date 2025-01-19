package com.google.zxing.datamatrix.encoder;

final class X12Encoder extends C40Encoder {
   @Override
   public int getEncodingMode() {
      return 3;
   }

   @Override
   public void encode(EncoderContext context) {
      StringBuilder buffer = new StringBuilder();

      while (context.hasMoreCharacters()) {
         char c = context.getCurrentChar();
         context.pos++;
         this.encodeChar(c, buffer);
         int count = buffer.length();
         if (count % 3 == 0) {
            writeNextTriplet(context, buffer);
            int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, this.getEncodingMode());
            if (newMode != this.getEncodingMode()) {
               context.signalEncoderChange(0);
               break;
            }
         }
      }

      this.handleEOD(context, buffer);
   }

   @Override
   int encodeChar(char c, StringBuilder sb) {
      switch (c) {
         case '\r':
            sb.append('\u0000');
            break;
         case ' ':
            sb.append('\u0003');
            break;
         case '*':
            sb.append('\u0001');
            break;
         case '>':
            sb.append('\u0002');
            break;
         default:
            if (c >= '0' && c <= '9') {
               sb.append((char)(c - '0' + 4));
            } else if (c >= 'A' && c <= 'Z') {
               sb.append((char)(c - 'A' + 14));
            } else {
               HighLevelEncoder.illegalCharacter(c);
            }
      }

      return 1;
   }

   @Override
   void handleEOD(EncoderContext context, StringBuilder buffer) {
      context.updateSymbolInfo();
      int available = context.getSymbolInfo().getDataCapacity() - context.getCodewordCount();
      int count = buffer.length();
      context.pos -= count;
      if (context.getRemainingCharacters() > 1 || available > 1 || context.getRemainingCharacters() != available) {
         context.writeCodeword('þ');
      }

      if (context.getNewEncoding() < 0) {
         context.signalEncoderChange(0);
      }
   }
}
