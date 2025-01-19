package com.google.zxing.multi;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GenericMultipleBarcodeReader implements MultipleBarcodeReader {
   private static final int MIN_DIMENSION_TO_RECUR = 100;
   private static final int MAX_DEPTH = 4;
   static final Result[] EMPTY_RESULT_ARRAY = new Result[0];
   private final Reader delegate;

   public GenericMultipleBarcodeReader(Reader delegate) {
      this.delegate = delegate;
   }

   @Override
   public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
      return this.decodeMultiple(image, null);
   }

   @Override
   public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException {
      List<Result> results = new ArrayList<>();
      this.doDecodeMultiple(image, hints, results, 0, 0, 0);
      if (results.isEmpty()) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         return results.toArray(EMPTY_RESULT_ARRAY);
      }
   }

   private void doDecodeMultiple(BinaryBitmap image, Map<DecodeHintType, ?> hints, List<Result> results, int xOffset, int yOffset, int currentDepth) {
      if (currentDepth <= 4) {
         Result result;
         try {
            result = this.delegate.decode(image, hints);
         } catch (ReaderException var22) {
            return;
         }

         boolean alreadyFound = false;

         for (Result existingResult : results) {
            if (existingResult.getText().equals(result.getText())) {
               alreadyFound = true;
               break;
            }
         }

         if (!alreadyFound) {
            results.add(translateResultPoints(result, xOffset, yOffset));
         }

         ResultPoint[] resultPoints = result.getResultPoints();
         if (resultPoints != null && resultPoints.length != 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float minX = (float)width;
            float minY = (float)height;
            float maxX = 0.0F;
            float maxY = 0.0F;

            for (ResultPoint point : resultPoints) {
               if (point != null) {
                  float x = point.getX();
                  float y = point.getY();
                  if (x < minX) {
                     minX = x;
                  }

                  if (y < minY) {
                     minY = y;
                  }

                  if (x > maxX) {
                     maxX = x;
                  }

                  if (y > maxY) {
                     maxY = y;
                  }
               }
            }

            if (minX > 100.0F) {
               this.doDecodeMultiple(image.crop(0, 0, (int)minX, height), hints, results, xOffset, yOffset, currentDepth + 1);
            }

            if (minY > 100.0F) {
               this.doDecodeMultiple(image.crop(0, 0, width, (int)minY), hints, results, xOffset, yOffset, currentDepth + 1);
            }

            if (maxX < (float)(width - 100)) {
               this.doDecodeMultiple(image.crop((int)maxX, 0, width - (int)maxX, height), hints, results, xOffset + (int)maxX, yOffset, currentDepth + 1);
            }

            if (maxY < (float)(height - 100)) {
               this.doDecodeMultiple(image.crop(0, (int)maxY, width, height - (int)maxY), hints, results, xOffset, yOffset + (int)maxY, currentDepth + 1);
            }
         }
      }
   }

   private static Result translateResultPoints(Result result, int xOffset, int yOffset) {
      ResultPoint[] oldResultPoints = result.getResultPoints();
      if (oldResultPoints == null) {
         return result;
      } else {
         ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.length];

         for (int i = 0; i < oldResultPoints.length; i++) {
            ResultPoint oldPoint = oldResultPoints[i];
            if (oldPoint != null) {
               newResultPoints[i] = new ResultPoint(oldPoint.getX() + (float)xOffset, oldPoint.getY() + (float)yOffset);
            }
         }

         Result newResult = new Result(
            result.getText(), result.getRawBytes(), result.getNumBits(), newResultPoints, result.getBarcodeFormat(), result.getTimestamp()
         );
         newResult.putAllMetadata(result.getResultMetadata());
         return newResult;
      }
   }
}
