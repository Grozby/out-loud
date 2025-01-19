package com.google.zxing.datamatrix.decoder;

final class DataBlock {
   private final int numDataCodewords;
   private final byte[] codewords;

   private DataBlock(int numDataCodewords, byte[] codewords) {
      this.numDataCodewords = numDataCodewords;
      this.codewords = codewords;
   }

   static DataBlock[] getDataBlocks(byte[] rawCodewords, Version version) {
      Version.ECBlocks ecBlocks = version.getECBlocks();
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
            int numBlockCodewords = ecBlocks.getECCodewords() + numDataCodewords;
            result[numResultBlocks++] = new DataBlock(numDataCodewords, new byte[numBlockCodewords]);
         }
      }

      int longerBlocksTotalCodewords = result[0].codewords.length;
      int longerBlocksNumDataCodewords = longerBlocksTotalCodewords - ecBlocks.getECCodewords();
      int shorterBlocksNumDataCodewords = longerBlocksNumDataCodewords - 1;
      int rawCodewordsOffset = 0;

      for (int i = 0; i < shorterBlocksNumDataCodewords; i++) {
         for (int j = 0; j < numResultBlocks; j++) {
            result[j].codewords[i] = rawCodewords[rawCodewordsOffset++];
         }
      }

      boolean specialVersion = version.getVersionNumber() == 24;
      int numLongerBlocks = specialVersion ? 8 : numResultBlocks;

      for (int j = 0; j < numLongerBlocks; j++) {
         result[j].codewords[longerBlocksNumDataCodewords - 1] = rawCodewords[rawCodewordsOffset++];
      }

      int max = result[0].codewords.length;

      for (int i = longerBlocksNumDataCodewords; i < max; i++) {
         for (int j = 0; j < numResultBlocks; j++) {
            int jOffset = specialVersion ? (j + 8) % numResultBlocks : j;
            int iOffset = specialVersion && jOffset > 7 ? i - 1 : i;
            result[jOffset].codewords[iOffset] = rawCodewords[rawCodewordsOffset++];
         }
      }

      if (rawCodewordsOffset != rawCodewords.length) {
         throw new IllegalArgumentException();
      } else {
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
