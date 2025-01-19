package com.google.zxing.oned;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitArray;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public abstract class OneDReader implements Reader {
   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
      return this.decode(image, null);
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
      try {
         return this.doDecode(image, hints);
      } catch (NotFoundException var12) {
         boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
         if (tryHarder && image.isRotateSupported()) {
            BinaryBitmap rotatedImage = image.rotateCounterClockwise();
            Result result = this.doDecode(rotatedImage, hints);
            Map<ResultMetadataType, ?> metadata = result.getResultMetadata();
            int orientation = 270;
            if (metadata != null && metadata.containsKey(ResultMetadataType.ORIENTATION)) {
               orientation = (orientation + (Integer)metadata.get(ResultMetadataType.ORIENTATION)) % 360;
            }

            result.putMetadata(ResultMetadataType.ORIENTATION, orientation);
            ResultPoint[] points = result.getResultPoints();
            if (points != null) {
               int height = rotatedImage.getHeight();

               for (int i = 0; i < points.length; i++) {
                  points[i] = new ResultPoint((float)height - points[i].getY() - 1.0F, points[i].getX());
               }
            }

            return result;
         } else {
            throw var12;
         }
      }
   }

   @Override
   public void reset() {
   }

   private Result doDecode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException {
      int width = image.getWidth();
      int height = image.getHeight();
      BitArray row = new BitArray(width);
      boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
      int rowStep = Math.max(1, height >> (tryHarder ? 8 : 5));
      int maxLines;
      if (tryHarder) {
         maxLines = height;
      } else {
         maxLines = 15;
      }

      int middle = height / 2;

      for (int x = 0; x < maxLines; x++) {
         int rowStepsAboveOrBelow = (x + 1) / 2;
         boolean isAbove = (x & 1) == 0;
         int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
         if (rowNumber >= 0 && rowNumber < height) {
            try {
               row = image.getBlackRow(rowNumber, row);
            } catch (NotFoundException var17) {
               continue;
            }

            for (int attempt = 0; attempt < 2; attempt++) {
               if (attempt == 1) {
                  row.reverse();
                  if (hints != null && hints.containsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK)) {
                     Map<DecodeHintType, Object> newHints = new EnumMap<>(DecodeHintType.class);
                     newHints.putAll((Map<? extends DecodeHintType, ? extends Object>)hints);
                     newHints.remove(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
                     hints = newHints;
                  }
               }

               try {
                  Result result = this.decodeRow(rowNumber, row, hints);
                  if (attempt == 1) {
                     result.putMetadata(ResultMetadataType.ORIENTATION, 180);
                     ResultPoint[] points = result.getResultPoints();
                     if (points != null) {
                        points[0] = new ResultPoint((float)width - points[0].getX() - 1.0F, points[0].getY());
                        points[1] = new ResultPoint((float)width - points[1].getX() - 1.0F, points[1].getY());
                     }
                  }

                  return result;
               } catch (ReaderException var18) {
               }
            }
            continue;
         }
         break;
      }

      throw NotFoundException.getNotFoundInstance();
   }

   protected static void recordPattern(BitArray row, int start, int[] counters) throws NotFoundException {
      int numCounters = counters.length;
      Arrays.fill(counters, 0, numCounters, 0);
      int end = row.getSize();
      if (start >= end) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         boolean isWhite = !row.get(start);
         int counterPosition = 0;

         int i;
         for (i = start; i < end; i++) {
            if (row.get(i) != isWhite) {
               counters[counterPosition]++;
            } else {
               if (++counterPosition == numCounters) {
                  break;
               }

               counters[counterPosition] = 1;
               isWhite = !isWhite;
            }
         }

         if (counterPosition != numCounters && (counterPosition != numCounters - 1 || i != end)) {
            throw NotFoundException.getNotFoundInstance();
         }
      }
   }

   protected static void recordPatternInReverse(BitArray row, int start, int[] counters) throws NotFoundException {
      int numTransitionsLeft = counters.length;
      boolean last = row.get(start);

      while (start > 0 && numTransitionsLeft >= 0) {
         if (row.get(--start) != last) {
            numTransitionsLeft--;
            last = !last;
         }
      }

      if (numTransitionsLeft >= 0) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         recordPattern(row, start + 1, counters);
      }
   }

   protected static float patternMatchVariance(int[] counters, int[] pattern, float maxIndividualVariance) {
      int numCounters = counters.length;
      int total = 0;
      int patternLength = 0;

      for (int i = 0; i < numCounters; i++) {
         total += counters[i];
         patternLength += pattern[i];
      }

      if (total < patternLength) {
         return Float.POSITIVE_INFINITY;
      } else {
         float unitBarWidth = (float)total / (float)patternLength;
         maxIndividualVariance *= unitBarWidth;
         float totalVariance = 0.0F;

         for (int x = 0; x < numCounters; x++) {
            int counter = counters[x];
            float scaledPattern = (float)pattern[x] * unitBarWidth;
            float variance = (float)counter > scaledPattern ? (float)counter - scaledPattern : scaledPattern - (float)counter;
            if (variance > maxIndividualVariance) {
               return Float.POSITIVE_INFINITY;
            }

            totalVariance += variance;
         }

         return totalVariance / (float)total;
      }
   }

   public abstract Result decodeRow(int var1, BitArray var2, Map<DecodeHintType, ?> var3) throws NotFoundException, ChecksumException, FormatException;
}
