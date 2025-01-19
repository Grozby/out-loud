package com.google.zxing.multi;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import java.util.Map;

public final class ByQuadrantReader implements Reader {
   private final Reader delegate;

   public ByQuadrantReader(Reader delegate) {
      this.delegate = delegate;
   }

   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
      return this.decode(image, null);
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
      int width = image.getWidth();
      int height = image.getHeight();
      int halfWidth = width / 2;
      int halfHeight = height / 2;

      try {
         return this.delegate.decode(image.crop(0, 0, halfWidth, halfHeight), hints);
      } catch (NotFoundException var14) {
         try {
            Result result = this.delegate.decode(image.crop(halfWidth, 0, halfWidth, halfHeight), hints);
            makeAbsolute(result.getResultPoints(), halfWidth, 0);
            return result;
         } catch (NotFoundException var13) {
            try {
               Result resultx = this.delegate.decode(image.crop(0, halfHeight, halfWidth, halfHeight), hints);
               makeAbsolute(resultx.getResultPoints(), 0, halfHeight);
               return resultx;
            } catch (NotFoundException var12) {
               try {
                  Result resultxx = this.delegate.decode(image.crop(halfWidth, halfHeight, halfWidth, halfHeight), hints);
                  makeAbsolute(resultxx.getResultPoints(), halfWidth, halfHeight);
                  return resultxx;
               } catch (NotFoundException var11) {
                  int quarterWidth = halfWidth / 2;
                  int quarterHeight = halfHeight / 2;
                  BinaryBitmap center = image.crop(quarterWidth, quarterHeight, halfWidth, halfHeight);
                  Result resultxxx = this.delegate.decode(center, hints);
                  makeAbsolute(resultxxx.getResultPoints(), quarterWidth, quarterHeight);
                  return resultxxx;
               }
            }
         }
      }
   }

   @Override
   public void reset() {
      this.delegate.reset();
   }

   private static void makeAbsolute(ResultPoint[] points, int leftOffset, int topOffset) {
      if (points != null) {
         for (int i = 0; i < points.length; i++) {
            ResultPoint relative = points[i];
            if (relative != null) {
               points[i] = new ResultPoint(relative.getX() + (float)leftOffset, relative.getY() + (float)topOffset);
            }
         }
      }
   }
}
