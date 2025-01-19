package com.google.zxing.common.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

@Deprecated
public final class MonochromeRectangleDetector {
   private static final int MAX_MODULES = 32;
   private final BitMatrix image;

   public MonochromeRectangleDetector(BitMatrix image) {
      this.image = image;
   }

   public ResultPoint[] detect() throws NotFoundException {
      int height = this.image.getHeight();
      int width = this.image.getWidth();
      int halfHeight = height / 2;
      int halfWidth = width / 2;
      int deltaY = Math.max(1, height / 256);
      int deltaX = Math.max(1, width / 256);
      int top = 0;
      int left = 0;
      ResultPoint pointA = this.findCornerFromCenter(halfWidth, 0, left, width, halfHeight, -deltaY, top, height, halfWidth / 2);
      top = (int)pointA.getY() - 1;
      ResultPoint pointB = this.findCornerFromCenter(halfWidth, -deltaX, left, width, halfHeight, 0, top, height, halfHeight / 2);
      left = (int)pointB.getX() - 1;
      ResultPoint pointC = this.findCornerFromCenter(halfWidth, deltaX, left, width, halfHeight, 0, top, height, halfHeight / 2);
      int right = (int)pointC.getX() + 1;
      ResultPoint pointD = this.findCornerFromCenter(halfWidth, 0, left, right, halfHeight, deltaY, top, height, halfWidth / 2);
      int bottom = (int)pointD.getY() + 1;
      pointA = this.findCornerFromCenter(halfWidth, 0, left, right, halfHeight, -deltaY, top, bottom, halfWidth / 4);
      return new ResultPoint[]{pointA, pointB, pointC, pointD};
   }

   private ResultPoint findCornerFromCenter(int centerX, int deltaX, int left, int right, int centerY, int deltaY, int top, int bottom, int maxWhiteRun) throws NotFoundException {
      int[] lastRange = null;
      int y = centerY;
      int x = centerX;

      while (y < bottom && y >= top && x < right && x >= left) {
         int[] range;
         if (deltaX == 0) {
            range = this.blackWhiteRange(y, maxWhiteRun, left, right, true);
         } else {
            range = this.blackWhiteRange(x, maxWhiteRun, top, bottom, false);
         }

         if (range == null) {
            if (lastRange == null) {
               throw NotFoundException.getNotFoundInstance();
            }

            if (deltaX == 0) {
               int lastY = y - deltaY;
               if (lastRange[0] < centerX) {
                  if (lastRange[1] > centerX) {
                     return new ResultPoint((float)lastRange[deltaY > 0 ? 0 : 1], (float)lastY);
                  }

                  return new ResultPoint((float)lastRange[0], (float)lastY);
               }

               return new ResultPoint((float)lastRange[1], (float)lastY);
            }

            int lastX = x - deltaX;
            if (lastRange[0] < centerY) {
               if (lastRange[1] > centerY) {
                  return new ResultPoint((float)lastX, (float)lastRange[deltaX < 0 ? 0 : 1]);
               }

               return new ResultPoint((float)lastX, (float)lastRange[0]);
            }

            return new ResultPoint((float)lastX, (float)lastRange[1]);
         }

         lastRange = range;
         y += deltaY;
         x += deltaX;
      }

      throw NotFoundException.getNotFoundInstance();
   }

   private int[] blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim, boolean horizontal) {
      int center = (minDim + maxDim) / 2;
      int start = center;

      while (start >= minDim) {
         if (horizontal ? !this.image.get(start, fixedDimension) : !this.image.get(fixedDimension, start)) {
            int whiteRunStart = start;

            do {
               start--;
            } while (start >= minDim && (horizontal ? !this.image.get(start, fixedDimension) : !this.image.get(fixedDimension, start)));

            int whiteRunSize = whiteRunStart - start;
            if (start < minDim || whiteRunSize > maxWhiteRun) {
               start = whiteRunStart;
               break;
            }
         } else {
            start--;
         }
      }

      start++;
      int end = center;

      while (end < maxDim) {
         if (horizontal ? !this.image.get(end, fixedDimension) : !this.image.get(fixedDimension, end)) {
            int whiteRunStart = end;

            do {
               end++;
            } while (end < maxDim && (horizontal ? !this.image.get(end, fixedDimension) : !this.image.get(fixedDimension, end)));

            int whiteRunSize = end - whiteRunStart;
            if (end >= maxDim || whiteRunSize > maxWhiteRun) {
               end = whiteRunStart;
               break;
            }
         } else {
            end++;
         }
      }

      end--;
      return end > start ? new int[]{start, end} : null;
   }
}
