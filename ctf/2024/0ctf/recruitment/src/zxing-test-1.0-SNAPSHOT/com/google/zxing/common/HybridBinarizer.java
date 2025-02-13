package com.google.zxing.common;

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;

public final class HybridBinarizer extends GlobalHistogramBinarizer {
   private static final int BLOCK_SIZE_POWER = 3;
   private static final int BLOCK_SIZE = 8;
   private static final int BLOCK_SIZE_MASK = 7;
   private static final int MINIMUM_DIMENSION = 40;
   private static final int MIN_DYNAMIC_RANGE = 24;
   private BitMatrix matrix;

   public HybridBinarizer(LuminanceSource source) {
      super(source);
   }

   @Override
   public BitMatrix getBlackMatrix() throws NotFoundException {
      if (this.matrix != null) {
         return this.matrix;
      } else {
         LuminanceSource source = this.getLuminanceSource();
         int width = source.getWidth();
         int height = source.getHeight();
         if (width >= 40 && height >= 40) {
            byte[] luminances = source.getMatrix();
            int subWidth = width >> 3;
            if ((width & 7) != 0) {
               subWidth++;
            }

            int subHeight = height >> 3;
            if ((height & 7) != 0) {
               subHeight++;
            }

            int[][] blackPoints = calculateBlackPoints(luminances, subWidth, subHeight, width, height);
            BitMatrix newMatrix = new BitMatrix(width, height);
            calculateThresholdForBlock(luminances, subWidth, subHeight, width, height, blackPoints, newMatrix);
            this.matrix = newMatrix;
         } else {
            this.matrix = super.getBlackMatrix();
         }

         return this.matrix;
      }
   }

   @Override
   public Binarizer createBinarizer(LuminanceSource source) {
      return new HybridBinarizer(source);
   }

   private static void calculateThresholdForBlock(byte[] luminances, int subWidth, int subHeight, int width, int height, int[][] blackPoints, BitMatrix matrix) {
      int maxYOffset = height - 8;
      int maxXOffset = width - 8;

      for (int y = 0; y < subHeight; y++) {
         int yoffset = y << 3;
         if (yoffset > maxYOffset) {
            yoffset = maxYOffset;
         }

         int top = cap(y, subHeight - 3);

         for (int x = 0; x < subWidth; x++) {
            int xoffset = x << 3;
            if (xoffset > maxXOffset) {
               xoffset = maxXOffset;
            }

            int left = cap(x, subWidth - 3);
            int sum = 0;

            for (int z = -2; z <= 2; z++) {
               int[] blackRow = blackPoints[top + z];
               sum += blackRow[left - 2] + blackRow[left - 1] + blackRow[left] + blackRow[left + 1] + blackRow[left + 2];
            }

            int average = sum / 25;
            thresholdBlock(luminances, xoffset, yoffset, average, width, matrix);
         }
      }
   }

   private static int cap(int value, int max) {
      return value < 2 ? 2 : Math.min(value, max);
   }

   private static void thresholdBlock(byte[] luminances, int xoffset, int yoffset, int threshold, int stride, BitMatrix matrix) {
      int y = 0;

      for (int offset = yoffset * stride + xoffset; y < 8; offset += stride) {
         for (int x = 0; x < 8; x++) {
            if ((luminances[offset + x] & 255) <= threshold) {
               matrix.set(xoffset + x, yoffset + y);
            }
         }

         y++;
      }
   }

   private static int[][] calculateBlackPoints(byte[] luminances, int subWidth, int subHeight, int width, int height) {
      int maxYOffset = height - 8;
      int maxXOffset = width - 8;
      int[][] blackPoints = new int[subHeight][subWidth];

      for (int y = 0; y < subHeight; y++) {
         int yoffset = y << 3;
         if (yoffset > maxYOffset) {
            yoffset = maxYOffset;
         }

         for (int x = 0; x < subWidth; x++) {
            int xoffset = x << 3;
            if (xoffset > maxXOffset) {
               xoffset = maxXOffset;
            }

            int sum = 0;
            int min = 255;
            int max = 0;
            int yy = 0;

            for (int offset = yoffset * width + xoffset; yy < 8; offset += width) {
               for (int xx = 0; xx < 8; xx++) {
                  int pixel = luminances[offset + xx] & 255;
                  sum += pixel;
                  if (pixel < min) {
                     min = pixel;
                  }

                  if (pixel > max) {
                     max = pixel;
                  }
               }

               if (max - min > 24) {
                  yy++;

                  for (offset += width; yy < 8; offset += width) {
                     for (int xx = 0; xx < 8; xx++) {
                        sum += luminances[offset + xx] & 255;
                     }

                     yy++;
                  }
               }

               yy++;
            }

            yy = sum >> 6;
            if (max - min <= 24) {
               yy = min / 2;
               if (y > 0 && x > 0) {
                  int averageNeighborBlackPoint = (blackPoints[y - 1][x] + 2 * blackPoints[y][x - 1] + blackPoints[y - 1][x - 1]) / 4;
                  if (min < averageNeighborBlackPoint) {
                     yy = averageNeighborBlackPoint;
                  }
               }
            }

            blackPoints[y][x] = yy;
         }
      }

      return blackPoints;
   }
}
