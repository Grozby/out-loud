package com.google.zxing.aztec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.aztec.decoder.Decoder;
import com.google.zxing.aztec.detector.Detector;
import com.google.zxing.common.DecoderResult;
import java.util.List;
import java.util.Map;

public final class AztecReader implements Reader {
   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
      return this.decode(image, null);
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
      NotFoundException notFoundException = null;
      FormatException formatException = null;
      Detector detector = new Detector(image.getBlackMatrix());
      ResultPoint[] points = null;
      DecoderResult decoderResult = null;
      int errorsCorrected = 0;

      try {
         AztecDetectorResult detectorResult = detector.detect(false);
         points = detectorResult.getPoints();
         errorsCorrected = detectorResult.getErrorsCorrected();
         decoderResult = new Decoder().decode(detectorResult);
      } catch (NotFoundException var14) {
         notFoundException = var14;
      } catch (FormatException var15) {
         formatException = var15;
      }

      if (decoderResult == null) {
         try {
            AztecDetectorResult detectorResult = detector.detect(true);
            points = detectorResult.getPoints();
            errorsCorrected = detectorResult.getErrorsCorrected();
            decoderResult = new Decoder().decode(detectorResult);
         } catch (FormatException | NotFoundException var16) {
            if (notFoundException != null) {
               throw notFoundException;
            }

            if (formatException != null) {
               throw formatException;
            }

            throw var16;
         }
      }

      if (hints != null) {
         ResultPointCallback rpcb = (ResultPointCallback)hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
         if (rpcb != null) {
            for (ResultPoint point : points) {
               rpcb.foundPossibleResultPoint(point);
            }
         }
      }

      Result result = new Result(
         decoderResult.getText(), decoderResult.getRawBytes(), decoderResult.getNumBits(), points, BarcodeFormat.AZTEC, System.currentTimeMillis()
      );
      List<byte[]> byteSegments = decoderResult.getByteSegments();
      if (byteSegments != null) {
         result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
      }

      String ecLevel = decoderResult.getECLevel();
      if (ecLevel != null) {
         result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
      }

      errorsCorrected += decoderResult.getErrorsCorrected();
      result.putMetadata(ResultMetadataType.ERRORS_CORRECTED, errorsCorrected);
      result.putMetadata(ResultMetadataType.SYMBOLOGY_IDENTIFIER, "]z" + decoderResult.getSymbologyModifier());
      return result;
   }

   @Override
   public void reset() {
   }
}
