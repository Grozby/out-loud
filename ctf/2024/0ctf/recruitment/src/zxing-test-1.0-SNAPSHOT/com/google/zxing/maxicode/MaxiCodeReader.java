package com.google.zxing.maxicode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.maxicode.decoder.Decoder;
import java.util.Map;

public final class MaxiCodeReader implements Reader {
   private static final ResultPoint[] NO_POINTS = new ResultPoint[0];
   private static final int MATRIX_WIDTH = 30;
   private static final int MATRIX_HEIGHT = 33;
   private final Decoder decoder = new Decoder();

   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
      return this.decode(image, null);
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
      BitMatrix bits = extractPureBits(image.getBlackMatrix());
      DecoderResult decoderResult = this.decoder.decode(bits, hints);
      Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), NO_POINTS, BarcodeFormat.MAXICODE);
      result.putMetadata(ResultMetadataType.ERRORS_CORRECTED, decoderResult.getErrorsCorrected());
      String ecLevel = decoderResult.getECLevel();
      if (ecLevel != null) {
         result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
      }

      return result;
   }

   @Override
   public void reset() {
   }

   private static BitMatrix extractPureBits(BitMatrix image) throws NotFoundException {
      int[] enclosingRectangle = image.getEnclosingRectangle();
      if (enclosingRectangle == null) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         int left = enclosingRectangle[0];
         int top = enclosingRectangle[1];
         int width = enclosingRectangle[2];
         int height = enclosingRectangle[3];
         BitMatrix bits = new BitMatrix(30, 33);

         for (int y = 0; y < 33; y++) {
            int iy = top + Math.min((y * height + height / 2) / 33, height - 1);

            for (int x = 0; x < 30; x++) {
               int ix = left + Math.min((x * width + width / 2 + (y & 1) * width / 2) / 30, width - 1);
               if (image.get(ix, iy)) {
                  bits.set(x, y);
               }
            }
         }

         return bits;
      }
   }
}
