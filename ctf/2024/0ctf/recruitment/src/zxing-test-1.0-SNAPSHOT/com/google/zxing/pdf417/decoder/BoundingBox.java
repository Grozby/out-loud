package com.google.zxing.pdf417.decoder;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

final class BoundingBox {
   private final BitMatrix image;
   private final ResultPoint topLeft;
   private final ResultPoint bottomLeft;
   private final ResultPoint topRight;
   private final ResultPoint bottomRight;
   private final int minX;
   private final int maxX;
   private final int minY;
   private final int maxY;

   BoundingBox(BitMatrix image, ResultPoint topLeft, ResultPoint bottomLeft, ResultPoint topRight, ResultPoint bottomRight) throws NotFoundException {
      boolean leftUnspecified = topLeft == null || bottomLeft == null;
      boolean rightUnspecified = topRight == null || bottomRight == null;
      if (leftUnspecified && rightUnspecified) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         if (leftUnspecified) {
            topLeft = new ResultPoint(0.0F, topRight.getY());
            bottomLeft = new ResultPoint(0.0F, bottomRight.getY());
         } else if (rightUnspecified) {
            topRight = new ResultPoint((float)(image.getWidth() - 1), topLeft.getY());
            bottomRight = new ResultPoint((float)(image.getWidth() - 1), bottomLeft.getY());
         }

         this.image = image;
         this.topLeft = topLeft;
         this.bottomLeft = bottomLeft;
         this.topRight = topRight;
         this.bottomRight = bottomRight;
         this.minX = (int)Math.min(topLeft.getX(), bottomLeft.getX());
         this.maxX = (int)Math.max(topRight.getX(), bottomRight.getX());
         this.minY = (int)Math.min(topLeft.getY(), topRight.getY());
         this.maxY = (int)Math.max(bottomLeft.getY(), bottomRight.getY());
      }
   }

   BoundingBox(BoundingBox boundingBox) {
      this.image = boundingBox.image;
      this.topLeft = boundingBox.topLeft;
      this.bottomLeft = boundingBox.bottomLeft;
      this.topRight = boundingBox.topRight;
      this.bottomRight = boundingBox.bottomRight;
      this.minX = boundingBox.minX;
      this.maxX = boundingBox.maxX;
      this.minY = boundingBox.minY;
      this.maxY = boundingBox.maxY;
   }

   static BoundingBox merge(BoundingBox leftBox, BoundingBox rightBox) throws NotFoundException {
      if (leftBox == null) {
         return rightBox;
      } else {
         return rightBox == null ? leftBox : new BoundingBox(leftBox.image, leftBox.topLeft, leftBox.bottomLeft, rightBox.topRight, rightBox.bottomRight);
      }
   }

   BoundingBox addMissingRows(int missingStartRows, int missingEndRows, boolean isLeft) throws NotFoundException {
      ResultPoint newTopLeft = this.topLeft;
      ResultPoint newBottomLeft = this.bottomLeft;
      ResultPoint newTopRight = this.topRight;
      ResultPoint newBottomRight = this.bottomRight;
      if (missingStartRows > 0) {
         ResultPoint top = isLeft ? this.topLeft : this.topRight;
         int newMinY = (int)top.getY() - missingStartRows;
         if (newMinY < 0) {
            newMinY = 0;
         }

         ResultPoint newTop = new ResultPoint(top.getX(), (float)newMinY);
         if (isLeft) {
            newTopLeft = newTop;
         } else {
            newTopRight = newTop;
         }
      }

      if (missingEndRows > 0) {
         ResultPoint bottom = isLeft ? this.bottomLeft : this.bottomRight;
         int newMaxY = (int)bottom.getY() + missingEndRows;
         if (newMaxY >= this.image.getHeight()) {
            newMaxY = this.image.getHeight() - 1;
         }

         ResultPoint newBottom = new ResultPoint(bottom.getX(), (float)newMaxY);
         if (isLeft) {
            newBottomLeft = newBottom;
         } else {
            newBottomRight = newBottom;
         }
      }

      return new BoundingBox(this.image, newTopLeft, newBottomLeft, newTopRight, newBottomRight);
   }

   int getMinX() {
      return this.minX;
   }

   int getMaxX() {
      return this.maxX;
   }

   int getMinY() {
      return this.minY;
   }

   int getMaxY() {
      return this.maxY;
   }

   ResultPoint getTopLeft() {
      return this.topLeft;
   }

   ResultPoint getTopRight() {
      return this.topRight;
   }

   ResultPoint getBottomLeft() {
      return this.bottomLeft;
   }

   ResultPoint getBottomRight() {
      return this.bottomRight;
   }
}
