package com.google.zxing.datamatrix.encoder;

final class ASCIIEncoder implements Encoder {
   @Override
   public int getEncodingMode() {
      return 0;
   }

   @Override
   public void encode(EncoderContext context) {
      int n = HighLevelEncoder.determineConsecutiveDigitCount(context.getMessage(), context.pos);
      if (n >= 2) {
         context.writeCodeword(encodeASCIIDigits(context.getMessage().charAt(context.pos), context.getMessage().charAt(context.pos + 1)));
         context.pos += 2;
      } else {
         char c = context.getCurrentChar();
         int newMode = HighLevelEncoder.lookAheadTest(context.getMessage(), context.pos, this.getEncodingMode());
         if (newMode != this.getEncodingMode()) {
            switch (newMode) {
               case 1:
                  context.writeCodeword('æ');
                  context.signalEncoderChange(1);
                  return;
               case 2:
                  context.writeCodeword('ï');
                  context.signalEncoderChange(2);
                  break;
               case 3:
                  context.writeCodeword('î');
                  context.signalEncoderChange(3);
                  break;
               case 4:
                  context.writeCodeword('ð');
                  context.signalEncoderChange(4);
                  break;
               case 5:
                  context.writeCodeword('ç');
                  context.signalEncoderChange(5);
                  return;
               default:
                  throw new IllegalStateException("Illegal mode: " + newMode);
            }
         } else if (HighLevelEncoder.isExtendedASCII(c)) {
            context.writeCodeword('ë');
            context.writeCodeword((char)(c - 128 + 1));
            context.pos++;
         } else {
            context.writeCodeword((char)(c + 1));
            context.pos++;
         }
      }
   }

   private static char encodeASCIIDigits(char digit1, char digit2) {
      if (HighLevelEncoder.isDigit(digit1) && HighLevelEncoder.isDigit(digit2)) {
         int num = (digit1 - '0') * 10 + (digit2 - '0');
         return (char)(num + 130);
      } else {
         throw new IllegalArgumentException("not digits: " + digit1 + digit2);
      }
   }
}
