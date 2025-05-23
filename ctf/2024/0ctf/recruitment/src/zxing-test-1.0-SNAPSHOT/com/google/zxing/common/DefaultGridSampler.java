package com.google.zxing.common;

import com.google.zxing.NotFoundException;

public final class DefaultGridSampler extends GridSampler {
   @Override
   public BitMatrix sampleGrid(
      BitMatrix image,
      int dimensionX,
      int dimensionY,
      float p1ToX,
      float p1ToY,
      float p2ToX,
      float p2ToY,
      float p3ToX,
      float p3ToY,
      float p4ToX,
      float p4ToY,
      float p1FromX,
      float p1FromY,
      float p2FromX,
      float p2FromY,
      float p3FromX,
      float p3FromY,
      float p4FromX,
      float p4FromY
   ) throws NotFoundException {
      PerspectiveTransform transform = PerspectiveTransform.quadrilateralToQuadrilateral(
         p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY, p1FromX, p1FromY, p2FromX, p2FromY, p3FromX, p3FromY, p4FromX, p4FromY
      );
      return this.sampleGrid(image, dimensionX, dimensionY, transform);
   }

   @Override
   public BitMatrix sampleGrid(BitMatrix image, int dimensionX, int dimensionY, PerspectiveTransform transform) throws NotFoundException {
      if (dimensionX > 0 && dimensionY > 0) {
         BitMatrix bits = new BitMatrix(dimensionX, dimensionY);
         float[] points = new float[2 * dimensionX];

         for (int y = 0; y < dimensionY; y++) {
            int max = points.length;
            float iValue = (float)y + 0.5F;

            for (int x = 0; x < max; x += 2) {
               points[x] = (float)(x / 2) + 0.5F;
               points[x + 1] = iValue;
            }

            transform.transformPoints(points);
            checkAndNudgePoints(image, points);

            try {
               for (int x = 0; x < max; x += 2) {
                  if (image.get((int)points[x], (int)points[x + 1])) {
                     bits.set(x / 2, y);
                  }
               }
            } catch (ArrayIndexOutOfBoundsException var11) {
               throw NotFoundException.getNotFoundInstance();
            }
         }

         return bits;
      } else {
         throw NotFoundException.getNotFoundInstance();
      }
   }
}
