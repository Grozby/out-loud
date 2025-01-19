package com.google.zxing.datamatrix.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.detector.WhiteRectangleDetector;

public final class Detector {
   private final BitMatrix image;
   private final WhiteRectangleDetector rectangleDetector;

   public Detector(BitMatrix image) throws NotFoundException {
      this.image = image;
      this.rectangleDetector = new WhiteRectangleDetector(image);
   }

   public DetectorResult detect() throws NotFoundException {
      ResultPoint[] cornerPoints = this.rectangleDetector.detect();
      ResultPoint[] points = this.detectSolid1(cornerPoints);
      points = this.detectSolid2(points);
      points[3] = this.correctTopRight(points);
      if (points[3] == null) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         points = this.shiftToModuleCenter(points);
         ResultPoint topLeft = points[0];
         ResultPoint bottomLeft = points[1];
         ResultPoint bottomRight = points[2];
         ResultPoint topRight = points[3];
         int dimensionTop = this.transitionsBetween(topLeft, topRight) + 1;
         int dimensionRight = this.transitionsBetween(bottomRight, topRight) + 1;
         if ((dimensionTop & 1) == 1) {
            dimensionTop++;
         }

         if ((dimensionRight & 1) == 1) {
            dimensionRight++;
         }

         if (4 * dimensionTop < 6 * dimensionRight && 4 * dimensionRight < 6 * dimensionTop) {
            dimensionTop = dimensionRight = Math.max(dimensionTop, dimensionRight);
         }

         BitMatrix bits = sampleGrid(this.image, topLeft, bottomLeft, bottomRight, topRight, dimensionTop, dimensionRight);
         return new DetectorResult(bits, new ResultPoint[]{topLeft, bottomLeft, bottomRight, topRight});
      }
   }

   private static ResultPoint shiftPoint(ResultPoint point, ResultPoint to, int div) {
      float x = (to.getX() - point.getX()) / (float)(div + 1);
      float y = (to.getY() - point.getY()) / (float)(div + 1);
      return new ResultPoint(point.getX() + x, point.getY() + y);
   }

   private static ResultPoint moveAway(ResultPoint point, float fromX, float fromY) {
      float x = point.getX();
      float y = point.getY();
      if (x < fromX) {
         x--;
      } else {
         x++;
      }

      if (y < fromY) {
         y--;
      } else {
         y++;
      }

      return new ResultPoint(x, y);
   }

   private ResultPoint[] detectSolid1(ResultPoint[] cornerPoints) {
      ResultPoint pointA = cornerPoints[0];
      ResultPoint pointB = cornerPoints[1];
      ResultPoint pointC = cornerPoints[3];
      ResultPoint pointD = cornerPoints[2];
      int trAB = this.transitionsBetween(pointA, pointB);
      int trBC = this.transitionsBetween(pointB, pointC);
      int trCD = this.transitionsBetween(pointC, pointD);
      int trDA = this.transitionsBetween(pointD, pointA);
      int min = trAB;
      ResultPoint[] points = new ResultPoint[]{pointD, pointA, pointB, pointC};
      if (trAB > trBC) {
         min = trBC;
         points[0] = pointA;
         points[1] = pointB;
         points[2] = pointC;
         points[3] = pointD;
      }

      if (min > trCD) {
         min = trCD;
         points[0] = pointB;
         points[1] = pointC;
         points[2] = pointD;
         points[3] = pointA;
      }

      if (min > trDA) {
         points[0] = pointC;
         points[1] = pointD;
         points[2] = pointA;
         points[3] = pointB;
      }

      return points;
   }

   private ResultPoint[] detectSolid2(ResultPoint[] points) {
      ResultPoint pointA = points[0];
      ResultPoint pointB = points[1];
      ResultPoint pointC = points[2];
      ResultPoint pointD = points[3];
      int tr = this.transitionsBetween(pointA, pointD);
      ResultPoint pointBs = shiftPoint(pointB, pointC, (tr + 1) * 4);
      ResultPoint pointCs = shiftPoint(pointC, pointB, (tr + 1) * 4);
      int trBA = this.transitionsBetween(pointBs, pointA);
      int trCD = this.transitionsBetween(pointCs, pointD);
      if (trBA < trCD) {
         points[0] = pointA;
         points[1] = pointB;
         points[2] = pointC;
         points[3] = pointD;
      } else {
         points[0] = pointB;
         points[1] = pointC;
         points[2] = pointD;
         points[3] = pointA;
      }

      return points;
   }

   private ResultPoint correctTopRight(ResultPoint[] points) {
      ResultPoint pointA = points[0];
      ResultPoint pointB = points[1];
      ResultPoint pointC = points[2];
      ResultPoint pointD = points[3];
      int trTop = this.transitionsBetween(pointA, pointD);
      int trRight = this.transitionsBetween(pointB, pointD);
      ResultPoint pointAs = shiftPoint(pointA, pointB, (trRight + 1) * 4);
      ResultPoint pointCs = shiftPoint(pointC, pointB, (trTop + 1) * 4);
      trTop = this.transitionsBetween(pointAs, pointD);
      trRight = this.transitionsBetween(pointCs, pointD);
      ResultPoint candidate1 = new ResultPoint(
         pointD.getX() + (pointC.getX() - pointB.getX()) / (float)(trTop + 1), pointD.getY() + (pointC.getY() - pointB.getY()) / (float)(trTop + 1)
      );
      ResultPoint candidate2 = new ResultPoint(
         pointD.getX() + (pointA.getX() - pointB.getX()) / (float)(trRight + 1), pointD.getY() + (pointA.getY() - pointB.getY()) / (float)(trRight + 1)
      );
      if (!this.isValid(candidate1)) {
         return this.isValid(candidate2) ? candidate2 : null;
      } else if (!this.isValid(candidate2)) {
         return candidate1;
      } else {
         int sumc1 = this.transitionsBetween(pointAs, candidate1) + this.transitionsBetween(pointCs, candidate1);
         int sumc2 = this.transitionsBetween(pointAs, candidate2) + this.transitionsBetween(pointCs, candidate2);
         return sumc1 > sumc2 ? candidate1 : candidate2;
      }
   }

   private ResultPoint[] shiftToModuleCenter(ResultPoint[] points) {
      ResultPoint pointA = points[0];
      ResultPoint pointB = points[1];
      ResultPoint pointC = points[2];
      ResultPoint pointD = points[3];
      int dimH = this.transitionsBetween(pointA, pointD) + 1;
      int dimV = this.transitionsBetween(pointC, pointD) + 1;
      ResultPoint pointAs = shiftPoint(pointA, pointB, dimV * 4);
      ResultPoint pointCs = shiftPoint(pointC, pointB, dimH * 4);
      dimH = this.transitionsBetween(pointAs, pointD) + 1;
      dimV = this.transitionsBetween(pointCs, pointD) + 1;
      if ((dimH & 1) == 1) {
         dimH++;
      }

      if ((dimV & 1) == 1) {
         dimV++;
      }

      float centerX = (pointA.getX() + pointB.getX() + pointC.getX() + pointD.getX()) / 4.0F;
      float centerY = (pointA.getY() + pointB.getY() + pointC.getY() + pointD.getY()) / 4.0F;
      pointA = moveAway(pointA, centerX, centerY);
      pointB = moveAway(pointB, centerX, centerY);
      pointC = moveAway(pointC, centerX, centerY);
      pointD = moveAway(pointD, centerX, centerY);
      pointAs = shiftPoint(pointA, pointB, dimV * 4);
      pointAs = shiftPoint(pointAs, pointD, dimH * 4);
      ResultPoint pointBs = shiftPoint(pointB, pointA, dimV * 4);
      pointBs = shiftPoint(pointBs, pointC, dimH * 4);
      pointCs = shiftPoint(pointC, pointD, dimV * 4);
      pointCs = shiftPoint(pointCs, pointB, dimH * 4);
      ResultPoint pointDs = shiftPoint(pointD, pointC, dimV * 4);
      pointDs = shiftPoint(pointDs, pointA, dimH * 4);
      return new ResultPoint[]{pointAs, pointBs, pointCs, pointDs};
   }

   private boolean isValid(ResultPoint p) {
      return p.getX() >= 0.0F && p.getX() <= (float)(this.image.getWidth() - 1) && p.getY() > 0.0F && p.getY() <= (float)(this.image.getHeight() - 1);
   }

   private static BitMatrix sampleGrid(
      BitMatrix image, ResultPoint topLeft, ResultPoint bottomLeft, ResultPoint bottomRight, ResultPoint topRight, int dimensionX, int dimensionY
   ) throws NotFoundException {
      GridSampler sampler = GridSampler.getInstance();
      return sampler.sampleGrid(
         image,
         dimensionX,
         dimensionY,
         0.5F,
         0.5F,
         (float)dimensionX - 0.5F,
         0.5F,
         (float)dimensionX - 0.5F,
         (float)dimensionY - 0.5F,
         0.5F,
         (float)dimensionY - 0.5F,
         topLeft.getX(),
         topLeft.getY(),
         topRight.getX(),
         topRight.getY(),
         bottomRight.getX(),
         bottomRight.getY(),
         bottomLeft.getX(),
         bottomLeft.getY()
      );
   }

   private int transitionsBetween(ResultPoint from, ResultPoint to) {
      int fromX = (int)from.getX();
      int fromY = (int)from.getY();
      int toX = (int)to.getX();
      int toY = Math.min(this.image.getHeight() - 1, (int)to.getY());
      boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
      if (steep) {
         int temp = fromX;
         fromX = fromY;
         fromY = temp;
         temp = toX;
         toX = toY;
         toY = temp;
      }

      int dx = Math.abs(toX - fromX);
      int dy = Math.abs(toY - fromY);
      int error = -dx / 2;
      int ystep = fromY < toY ? 1 : -1;
      int xstep = fromX < toX ? 1 : -1;
      int transitions = 0;
      boolean inBlack = this.image.get(steep ? fromY : fromX, steep ? fromX : fromY);
      int x = fromX;

      for (int y = fromY; x != toX; x += xstep) {
         boolean isBlack = this.image.get(steep ? y : x, steep ? x : y);
         if (isBlack != inBlack) {
            transitions++;
            inBlack = isBlack;
         }

         error += dy;
         if (error > 0) {
            if (y == toY) {
               break;
            }

            y += ystep;
            error -= dx;
         }
      }

      return transitions;
   }
}
