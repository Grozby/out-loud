package com.google.zxing.multi.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.multi.qrcode.detector.MultiDetector;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.QRCodeDecoderMetaData;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class QRCodeMultiReader extends QRCodeReader implements MultipleBarcodeReader {
   private static final Result[] EMPTY_RESULT_ARRAY = new Result[0];
   private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

   @Override
   public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
      return this.decodeMultiple(image, null);
   }

   @Override
   public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException {
      List<Result> results = new ArrayList<>();
      DetectorResult[] detectorResults = new MultiDetector(image.getBlackMatrix()).detectMulti(hints);

      for (DetectorResult detectorResult : detectorResults) {
         try {
            DecoderResult decoderResult = this.getDecoder().decode(detectorResult.getBits(), hints);
            ResultPoint[] points = detectorResult.getPoints();
            if (decoderResult.getOther() instanceof QRCodeDecoderMetaData) {
               ((QRCodeDecoderMetaData)decoderResult.getOther()).applyMirroredCorrection(points);
            }

            Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
            List<byte[]> byteSegments = decoderResult.getByteSegments();
            if (byteSegments != null) {
               result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
            }

            String ecLevel = decoderResult.getECLevel();
            if (ecLevel != null) {
               result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
            }

            if (decoderResult.hasStructuredAppend()) {
               result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE, decoderResult.getStructuredAppendSequenceNumber());
               result.putMetadata(ResultMetadataType.STRUCTURED_APPEND_PARITY, decoderResult.getStructuredAppendParity());
            }

            results.add(result);
         } catch (ReaderException var14) {
         }
      }

      if (results.isEmpty()) {
         return EMPTY_RESULT_ARRAY;
      } else {
         results = processStructuredAppend(results);
         return results.toArray(EMPTY_RESULT_ARRAY);
      }
   }

   static List<Result> processStructuredAppend(List<Result> results) {
      List<Result> newResults = new ArrayList<>();
      List<Result> saResults = new ArrayList<>();

      for (Result result : results) {
         if (result.getResultMetadata().containsKey(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE)) {
            saResults.add(result);
         } else {
            newResults.add(result);
         }
      }

      if (saResults.isEmpty()) {
         return results;
      } else {
         Collections.sort(saResults, new QRCodeMultiReader.SAComparator());
         StringBuilder newText = new StringBuilder();
         ByteArrayOutputStream newRawBytes = new ByteArrayOutputStream();
         ByteArrayOutputStream newByteSegment = new ByteArrayOutputStream();

         for (Result saResult : saResults) {
            newText.append(saResult.getText());
            byte[] saBytes = saResult.getRawBytes();
            newRawBytes.write(saBytes, 0, saBytes.length);
            Iterable<byte[]> byteSegments = (Iterable<byte[]>)saResult.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
            if (byteSegments != null) {
               for (byte[] segment : byteSegments) {
                  newByteSegment.write(segment, 0, segment.length);
               }
            }
         }

         Result newResult = new Result(newText.toString(), newRawBytes.toByteArray(), NO_POINTS, BarcodeFormat.QR_CODE);
         if (newByteSegment.size() > 0) {
            newResult.putMetadata(ResultMetadataType.BYTE_SEGMENTS, Collections.singletonList(newByteSegment.toByteArray()));
         }

         newResults.add(newResult);
         return newResults;
      }
   }

   private static final class SAComparator implements Comparator<Result>, Serializable {
      private SAComparator() {
      }

      public int compare(Result a, Result b) {
         int aNumber = (Integer)a.getResultMetadata().get(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE);
         int bNumber = (Integer)b.getResultMetadata().get(ResultMetadataType.STRUCTURED_APPEND_SEQUENCE);
         return Integer.compare(aNumber, bNumber);
      }
   }
}
