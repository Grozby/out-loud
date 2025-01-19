package com.google.zxing.qrcode.decoder;

import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;
import java.util.Map;

public final class Decoder {
   private final ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);

   public DecoderResult decode(boolean[][] image) throws ChecksumException, FormatException {
      return this.decode(image, null);
   }

   public DecoderResult decode(boolean[][] image, Map<DecodeHintType, ?> hints) throws ChecksumException, FormatException {
      return this.decode(BitMatrix.parse(image), hints);
   }

   public DecoderResult decode(BitMatrix bits) throws ChecksumException, FormatException {
      return this.decode(bits, null);
   }

   public DecoderResult decode(BitMatrix bits, Map<DecodeHintType, ?> hints) throws FormatException, ChecksumException {
      BitMatrixParser parser = new BitMatrixParser(bits);
      FormatException fe = null;
      ChecksumException ce = null;

      try {
         return this.decode(parser, hints);
      } catch (FormatException var7) {
         fe = var7;
      } catch (ChecksumException var8) {
         ce = var8;
      }

      try {
         parser.remask();
         parser.setMirror(true);
         parser.readVersion();
         parser.readFormatInformation();
         parser.mirror();
         DecoderResult result = this.decode(parser, hints);
         result.setOther(new QRCodeDecoderMetaData(true));
         return result;
      } catch (ChecksumException | FormatException var9) {
         if (fe != null) {
            throw fe;
         } else {
            throw ce;
         }
      }
   }

   private DecoderResult decode(BitMatrixParser parser, Map<DecodeHintType, ?> hints) throws FormatException, ChecksumException {
      Version version = parser.readVersion();
      ErrorCorrectionLevel ecLevel = parser.readFormatInformation().getErrorCorrectionLevel();
      byte[] codewords = parser.readCodewords();
      DataBlock[] dataBlocks = DataBlock.getDataBlocks(codewords, version, ecLevel);
      int totalBytes = 0;

      for (DataBlock dataBlock : dataBlocks) {
         totalBytes += dataBlock.getNumDataCodewords();
      }

      byte[] resultBytes = new byte[totalBytes];
      int resultOffset = 0;
      int errorsCorrected = 0;

      for (DataBlock dataBlock : dataBlocks) {
         byte[] codewordBytes = dataBlock.getCodewords();
         int numDataCodewords = dataBlock.getNumDataCodewords();
         errorsCorrected += this.correctErrors(codewordBytes, numDataCodewords);

         for (int i = 0; i < numDataCodewords; i++) {
            resultBytes[resultOffset++] = codewordBytes[i];
         }
      }

      DecoderResult result = DecodedBitStreamParser.decode(resultBytes, version, ecLevel, hints);
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
