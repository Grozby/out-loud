package com.google.zxing.datamatrix.encoder;

final class Base256Encoder implements Encoder {
   @Override
   public int getEncodingMode() {
      return 5;
   }

   @Override
   public void encode(EncoderContext context) {
      StringBuilder buffer = new StringBuilder();
      buffer.append('\u0000');

      while (context.hasMoreCharacters()) {
         char c = context.getCurrentChar();
         buffer.append(c);
         context.pos++;
         int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, this.getEncodingMode());
         if (newMode != this.getEncodingMode()) {
            context.signalEncoderChange(0);
            break;
         }
      }

      int dataCount = buffer.length() - 1;
      int lengthFieldSize = 1;
      int currentSize = context.getCodewordCount() + dataCount + lengthFieldSize;
      context.updateSymbolInfo(currentSize);
      boolean mustPad = context.getSymbolInfo().getDataCapacity() - currentSize > 0;
      if (context.hasMoreCharacters() || mustPad) {
         if (dataCount <= 249) {
            buffer.setCharAt(0, (char)dataCount);
         } else {
            if (dataCount > 1555) {
               throw new IllegalStateException("Message length not in valid ranges: " + dataCount);
            }

            buffer.setCharAt(0, (char)(dataCount / 250 + 249));
            buffer.insert(1, (char)(dataCount % 250));
         }
      }

      int i = 0;

      for (int c = buffer.length(); i < c; i++) {
         context.writeCodeword(randomize255State(buffer.charAt(i), context.getCodewordCount() + 1));
      }
   }

   private static char randomize255State(char ch, int codewordPosition) {
      int pseudoRandom = 149 * codewordPosition % 255 + 1;
      int tempVariable = ch + pseudoRandom;
      return tempVariable <= 255 ? (char)tempVariable : (char)(tempVariable - 256);
   }
}
