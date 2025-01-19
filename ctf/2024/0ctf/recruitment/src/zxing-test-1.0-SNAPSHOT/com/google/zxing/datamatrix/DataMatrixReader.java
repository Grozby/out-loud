package com.google.zxing.datamatrix;

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
import com.google.zxing.common.DetectorResult;
import com.google.zxing.datamatrix.decoder.Decoder;
import com.google.zxing.datamatrix.detector.Detector;
import java.util.List;
import java.util.Map;

public final class DataMatrixReader implements Reader {
   private static final ResultPoint[] NO_POINTS = new ResultPoint[0];
   private final Decoder decoder = new Decoder();

   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
      return this.decode(image, null);
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
      DecoderResult decoderResult;
      ResultPoint[] points;
      if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
         BitMatrix bits = extractPureBits(image.getBlackMatrix());
         decoderResult = this.decoder.decode(bits);
         points = NO_POINTS;
      } else {
         DetectorResult detectorResult = new Detector(image.getBlackMatrix()).detect();
         decoderResult = this.decoder.decode(detectorResult.getBits());
         points = detectorResult.getPoints();
      }

      Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.DATA_MATRIX);
      List<byte[]> byteSegments = decoderResult.getByteSegments();
      if (byteSegments != null) {
         result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
      }

      String ecLevel = decoderResult.getECLevel();
      if (ecLevel != null) {
         result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
      }

      result.putMetadata(ResultMetadataType.ERRORS_CORRECTED, decoderResult.getErrorsCorrected());
      result.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]d" + decoderResult.getSymbologyModifier());
      return result;
   }

   @Override
   public void reset() {
   }

   private static BitMatrix extractPureBits(BitMatrix image) throws NotFoundException {
      int[] leftTopBlack = image.getTopLeftOnBit();
      int[] rightBottomBlack = image.getBottomRightOnBit();
      if (leftTopBlack != null && rightBottomBlack != null) {
         int moduleSize = moduleSize(leftTopBlack, image);
         int top = leftTopBlack[1];
         int bottom = rightBottomBlack[1];
         int left = leftTopBlack[0];
         int right = rightBottomBlack[0];
         int matrixWidth = (right - left + 1) / moduleSize;
         int matrixHeight = (bottom - top + 1) / moduleSize;
         if (matrixWidth > 0 && matrixHeight > 0) {
            int nudge = moduleSize / 2;
            top += nudge;
            left += nudge;
            BitMatrix bits = new BitMatrix(matrixWidth, matrixHeight);

            for (int y = 0; y < matrixHeight; y++) {
               int iOffset = top + y * moduleSize;

               for (int x = 0; x < matrixWidth; x++) {
                  if (image.get(left + x * moduleSize, iOffset)) {
                     bits.set(x, y);
                  }
               }
            }

            return bits;
         } else {
            throw NotFoundException.getNotFoundInstance();
         }
      } else {
         throw NotFoundException.getNotFoundInstance();
      }
   }

   private static int moduleSize(int[] leftTopBlack, BitMatrix image) throws NotFoundException {
      int width = image.getWidth();
      int x = leftTopBlack[0];
      int y = leftTopBlack[1];

      while (x < width && image.get(x, y)) {
         x++;
      }

      if (x == width) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         int moduleSize = x - leftTopBlack[0];
         if (moduleSize == 0) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            return moduleSize;
         }
      }
   }
}
