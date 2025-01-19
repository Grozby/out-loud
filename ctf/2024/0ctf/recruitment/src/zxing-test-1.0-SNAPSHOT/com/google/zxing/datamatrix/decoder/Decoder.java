package com.google.zxing.datamatrix.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

public final class Decoder {
   private final ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);

   public DecoderResult decode(boolean[][] image) throws FormatException, ChecksumException {
      return this.decode(BitMatrix.parse(image));
   }

   public DecoderResult decode(BitMatrix bits) throws FormatException, ChecksumException {
      BitMatrixParser parser = new BitMatrixParser(bits);
      Version version = parser.getVersion();
      byte[] codewords = parser.readCodewords();
      DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version);
      int totalBytes = 0;

      for (DataBlock db : dataBlocks) {
         totalBytes += db.getNumDataCodewords();
      }

      byte[] resultBytes = new byte[totalBytes];
      int errorsCorrected = 0;
      int dataBlocksCount = dataBlocks.length;

      for (int j = 0; j < dataBlocksCount; j++) {
         DataBlock dataBlock = dataBlocks[j];
         byte[] codewordBytes = dataBlock.getCodewords();
         int numDataCodewords = dataBlock.getNumDataCodewords();
         errorsCorrected += this.correctErrors(codewordBytes, numDataCodewords);

         for (int i = 0; i < numDataCodewords; i++) {
            resultBytes[i * dataBlocksCount + j] = codewordBytes[i];
         }
      }

      DecoderResult result = DecodedBitStreamParser.decode(resultBytes);
      result.setErrorsCorrected(errorsCorrected);
      return result;
   }

   private int correctErrors(byte[] codewordBytes, int numDataCodewords) throws ChecksumException {
      int numCodewords = codewordBytes.length;
      int[] codewordsInts = new int[numCodewords];

      for (int i = 0; i < numCodewords; i++) {
         codewordsInts[i] = codewordBytes[i] & 255;
      }

      int errorsCorrected = 0;

      try {
         errorsCorrected = this.rsDecoder.decodeWithECCount(codewordsInts, codewordBytes.length - numDataCodewords);
      } catch (ReedSolomonException var7) {
         throw ChecksumException.getChecksumInstance();
      }

      for (int i = 0; i < numDataCodewords; i++) {
         codewordBytes[i] = (byte)codewordsInts[i];
      }

      return errorsCorrected;
   }
}
