package com.google.zxing.pdf417.detector;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Detector {
   private static final int[] INDEXES_START_PATTERN = new int[]{0, 4, 1, 5};
   private static final int[] INDEXES_STOP_PATTERN = new int[]{6, 2, 7, 3};
   private static final float MAX_AVG_VARIANCE = 0.42F;
   private static final float MAX_INDIVIDUAL_VARIANCE = 0.8F;
   private static final float MAX_STOP_PATTERN_HEIGHT_VARIANCE = 0.5F;
   private static final int[] START_PATTERN = new int[]{8, 1, 1, 1, 1, 1, 1, 3};
   private static final int[] STOP_PATTERN = new int[]{7, 1, 1, 3, 1, 1, 1, 2, 1};
   private static final int MAX_PIXEL_DRIFT = 3;
   private static final int MAX_PATTERN_DRIFT = 5;
   private static final int SKIPPED_ROW_COUNT_MAX = 25;
   private static final int ROW_STEP = 5;
   private static final int BARCODE_MIN_HEIGHT = 10;
   private static final int[] ROTATIONS = new int[]{0, 180, 270, 90};

   private Detector() {
   }

   public static PDF417DetectorResult detect(BinaryBitmap image, Map<DecodeHintType, ?> hints, boolean multiple) throws NotFoundException {
      BitMatrix originalMatrix = image.getBlackMatrix();

      for (int rotation : ROTATIONS) {
         BitMatrix bitMatrix = applyRotation(originalMatrix, rotation);
         List<ResultPoint[]> barcodeCoordinates = detect(multiple, bitMatrix);
         if (!barcodeCoordinates.isEmpty()) {
            return new PDF417DetectorResult(bitMatrix, barcodeCoordinates, rotation);
         }
      }

      return new PDF417DetectorResult(originalMatrix, new ArrayList<>(), 0);
   }

   private static BitMatrix applyRotation(BitMatrix matrix, int rotation) {
      if (rotation % 360 == 0) {
         return matrix;
      } else {
         BitMatrix newMatrix = matrix.clone();
         newMatrix.rotate(rotation);
         return newMatrix;
      }
   }

   private static List<ResultPoint[]> detect(boolean multiple, BitMatrix bitMatrix) {
      List<ResultPoint[]> barcodeCoordinates = new ArrayList<>();
      int row = 0;
      int column = 0;
      boolean foundBarcodeInRow = false;

      while (row < bitMatrix.getHeight()) {
         ResultPoint[] vertices = findVertices(bitMatrix, row, column);
         if (vertices[0] == null && vertices[3] == null) {
            if (!foundBarcodeInRow) {
               break;
            }

            foundBarcodeInRow = false;
            column = 0;

            for (ResultPoint[] barcodeCoordinate : barcodeCoordinates) {
               if (barcodeCoordinate[1] != null) {
                  row = (int)Math.max((float)row, barcodeCoordinate[1].getY());
               }

               if (barcodeCoordinate[3] != null) {
                  row = Math.max(row, (int)barcodeCoordinate[3].getY());
               }
            }

            row += 5;
         } else {
            foundBarcodeInRow = true;
            barcodeCoordinates.add(vertices);
            if (!multiple) {
               break;
            }

            if (vertices[2] != null) {
               column = (int)vertices[2].getX();
               row = (int)vertices[2].getY();
            } else {
               column = (int)vertices[4].getX();
               row = (int)vertices[4].getY();
            }
         }
      }

      return barcodeCoordinates;
   }

   private static ResultPoint[] findVertices(BitMatrix matrix, int startRow, int startColumn) {
      int height = matrix.getHeight();
      int width = matrix.getWidth();
      ResultPoint[] result = new ResultPoint[8];
      int minHeight = 10;
      copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, minHeight, START_PATTERN), INDEXES_START_PATTERN);
      if (result[4] != null) {
         startColumn = (int)result[4].getX();
         startRow = (int)result[4].getY();
         if (result[5] != null) {
            int endRow = (int)result[5].getY();
            int startPatternHeight = endRow - startRow;
            minHeight = (int)Math.max((float)startPatternHeight * 0.5F, 10.0F);
         }
      }

      copyToResult(result, findRowsWithPattern(matrix, height, width, startRow, startColumn, minHeight, STOP_PATTERN), INDEXES_STOP_PATTERN);
      return result;
   }

   private static void copyToResult(ResultPoint[] result, ResultPoint[] tmpResult, int[] destinationIndexes) {
      for (int i = 0; i < destinationIndexes.length; i++) {
         result[destinationIndexes[i]] = tmpResult[i];
      }
   }

   private static ResultPoint[] findRowsWithPattern(BitMatrix matrix, int height, int width, int startRow, int startColumn, int minHeight, int[] pattern) {
      ResultPoint[] result = new ResultPoint[4];
      boolean found = false;
      int[] counters = new int[pattern.length];

      while (startRow < height) {
         int[] loc = findGuardPattern(matrix, startColumn, startRow, width, pattern, counters);
         if (loc != null) {
            while (startRow > 0) {
               int[] previousRowLoc = findGuardPattern(matrix, startColumn, --startRow, width, pattern, counters);
               if (previousRowLoc == null) {
                  startRow++;
                  break;
               }

               loc = previousRowLoc;
            }

            result[0] = new ResultPoint((float)loc[0], (float)startRow);
            result[1] = new ResultPoint((float)loc[1], (float)startRow);
            found = true;
            break;
         }

         startRow += 5;
      }

      int stopRow = startRow + 1;
      if (found) {
         int skippedRowCount = 0;

         int[] previousRowLoc;
         for (previousRowLoc = new int[]{(int)result[0].getX(), (int)result[1].getX()}; stopRow < height; stopRow++) {
            int[] loc = findGuardPattern(matrix, previousRowLoc[0], stopRow, width, pattern, counters);
            if (loc != null && Math.abs(previousRowLoc[0] - loc[0]) < 5 && Math.abs(previousRowLoc[1] - loc[1]) < 5) {
               previousRowLoc = loc;
               skippedRowCount = 0;
            } else {
               if (skippedRowCount > 25) {
                  break;
               }

               skippedRowCount++;
            }
         }

         stopRow -= skippedRowCount + 1;
         result[2] = new ResultPoint((float)previousRowLoc[0], (float)stopRow);
         result[3] = new ResultPoint((float)previousRowLoc[1], (float)stopRow);
      }

      if (stopRow - startRow < minHeight) {
         Arrays.fill(result, null);
      }

      return result;
   }

   private static int[] findGuardPattern(BitMatrix matrix, int column, int row, int width, int[] pattern, int[] counters) {
      Arrays.fill(counters, 0, counters.length, 0);
      int patternStart = column;
      int pixelDrift = 0;

      while (matrix.get(patternStart, row) && patternStart > 0 && pixelDrift++ < 3) {
         patternStart--;
      }

      int x = patternStart;
      int counterPosition = 0;
      int patternLength = pattern.length;

      for (boolean isWhite = false; x < width; x++) {
         boolean pixel = matrix.get(x, row);
         if (pixel != isWhite) {
            counters[counterPosition]++;
         } else {
            if (counterPosition == patternLength - 1) {
               if (patternMatchVariance(counters, pattern) < 0.42F) {
                  return new int[]{patternStart, x};
               }

               patternStart += counters[0] + counters[1];
               System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
               counters[counterPosition - 1] = 0;
               counters[counterPosition] = 0;
               counterPosition--;
            } else {
               counterPosition++;
            }

            counters[counterPosition] = 1;
            isWhite = !isWhite;
         }
      }

      return counterPosition == patternLength - 1 && patternMatchVariance(counters, pattern) < 0.42F ? new int[]{patternStart, x - 1} : null;
   }

   private static float patternMatchVariance(int[] counters, int[] pattern) {
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
         float maxIndividualVariance = 0.8F * unitBarWidth;
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
}
