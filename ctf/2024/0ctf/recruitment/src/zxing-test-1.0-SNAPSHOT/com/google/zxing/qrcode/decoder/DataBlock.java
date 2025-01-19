package com.google.zxing.qrcode.decoder;

final class DataBlock {
   private final int numDataCodewords;
   private final byte[] codewords;

   private DataBlock(int numDataCodewords, byte[] codewords) {
      this.numDataCodewords = numDataCodewords;
      this.codewords = codewords;
   }

   static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version, ErrorCorrectionLevel ecLevel) {
      if (rawCodewords.length != version.getTotalCodewords()) {
         throw new IllegalArgumentException();
      } else {
         Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
         int totalBlocks = 0;
         Version.ECB[] ecBlockArray = ecBlocks.getECBlocks();

         for (Version.ECB ecBlock : ecBlockArray) {
            totalBlocks += ecBlock.getCount();
         }

         DataBlock[] result = new DataBlock[totalBlocks];
         int numResultBlocks = 0;

         for (Version.ECB ecBlock : ecBlockArray) {
            for (int i = 0; i < ecBlock.getCount(); i++) {
               int numDataCodewords = ecBlock.getDataCodewords();
               int numBlockCodewords = ecBlocks.getECCodewordsPerBlock() + numDataCodewords;
               result[numResultBlocks++] = new DataBlock(numDataCodewords, new byte[numBlockCodewords]);
            }
         }

         int shorterBlocksTotalCodewords = result[0].codewords.length;

         int longerBlocksStartAt;
         for (longerBlocksStartAt = result.length - 1; longerBlocksStartAt >= 0; longerBlocksStartAt--) {
            int numCodewords = result[longerBlocksStartAt].codewords.length;
            if (numCodewords == shorterBlocksTotalCodewords) {
               break;
            }
         }

         longerBlocksStartAt++;
         int shorterBlocksNumDataCodewords = shorterBlocksTotalCodewords - ecBlocks.getECCodewordsPerBlock();
         int rawCodewordsOffset = 0;

         for (int i = 0; i < shorterBlocksNumDataCodewords; i++) {
            for (int j = 0; j < numResultBlocks; j++) {
               result[j].codewords[i] = rawCodewords[rawCodewordsOffset++];
            }
         }

         for (int j = longerBlocksStartAt; j < numResultBlocks; j++) {
            result[j].codewords[shorterBlocksNumDataCodewords] = rawCodewords[rawCodewordsOffset++];
         }

         int max = result[0].codewords.length;

         for (int i = shorterBlocksNumDataCodewords; i < max; i++) {
            for (int j = 0; j < numResultBlocks; j++) {
               int iOffset = j < longerBlocksStartAt ? i : i + 1;
               result[j].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
            }
         }

         return result;
      }
   }

   int getNumDataCodewords() {
      return this.numDataCodewords;
   }

   byte[] getCodewords() {
      return this.codewords;
   }
}
