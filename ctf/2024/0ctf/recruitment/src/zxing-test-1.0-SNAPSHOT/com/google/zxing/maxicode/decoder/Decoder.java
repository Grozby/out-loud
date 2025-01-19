package com.google.zxing.maxicode.decoder;

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
   private static final int ALL = 0;
   private static final int EVEN = 1;
   private static final int ODD = 2;
   private final ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(GenericGF.MAXICODE_FIELD_64);

   public DecoderResult decode(BitMatrix bits) throws ChecksumException, FormatException {
      return this.decode(bits, null);
   }

   public DecoderResult decode(BitMatrix bits, Map<DecodeHintType, ?> hints) throws FormatException, ChecksumException {
      BitMatrixParser parser = new BitMatrixParser(bits);
      byte[] codewords = parser.readCodewords();
      int errorsCorrected = this.correctErrors(codewords, 0, 10, 10, 0);
      int mode = codewords[0] & 15;
      byte[] datawords;
      switch (mode) {
         case 2:
         case 3:
         case 4:
            errorsCorrected += this.correctErrors(codewords, 20, 84, 40, 1);
            errorsCorrected += this.correctErrors(codewords, 20, 84, 40, 2);
            datawords = new byte[94];
            break;
         case 5:
            errorsCorrected += this.correctErrors(codewords, 20, 68, 56, 1);
            errorsCorrected += this.correctErrors(codewords, 20, 68, 56, 2);
            datawords = new byte[78];
            break;
         default:
            throw FormatException.getFormatInstance();
      }

      System.arraycopy(codewords, 0, datawords, 0, 10);
      System.arraycopy(codewords, 20, datawords, 10, datawords.length - 10);
      DecoderResult result = DecodedBitStreamParser.decode(datawords, mode);
      result.setErrorsCorrected(errorsCorrected);
      return result;
   }

   private int correctErrors(byte[] codewordBytes, int start, int dataCodewords, int ecCodewords, int mode) throws ChecksumException {
      int codewords = dataCodewords + ecCodewords;
      int divisor = mode == 0 ? 1 : 2;
      int[] codewordsInts = new int[codewords / divisor];

      for (int i = 0; i < codewords; i++) {
         if (mode == 0 || i % 2 == mode - 1) {
            codewordsInts[i / divisor] = codewordBytes[i + start] & 255;
         }
      }

      int errorsCorrected = 0;

      try {
         errorsCorrected = this.rsDecoder.decodeWithECCount(codewordsInts, ecCodewords / divisor);
      } catch (ReedSolomonException var11) {
         throw ChecksumException.getChecksumInstance();
      }

      for (int ix = 0; ix < dataCodewords; ix++) {
         if (mode == 0 || ix % 2 == mode - 1) {
            codewordBytes[ix + start] = (byte)codewordsInts[ix / divisor];
         }
      }

      return errorsCorrected;
   }
}
