package com.google.zxing.common;

import com.google.zxing.Binarizer;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;

public class GlobalHistogramBinarizer extends Binarizer {
   private static final int LUMINANCE_BITS = 5;
   private static final int LUMINANCE_SHIFT = 3;
   private static final int LUMINANCE_BUCKETS = 32;
   private static final byte[] EMPTY = new byte[0];
   private byte[] luminances = EMPTY;
   private final int[] buckets = new int[32];

   public GlobalHistogramBinarizer(LuminanceSource source) {
      super(source);
   }

   @Override
   public BitArray getBlackRow(int y, BitArray row) throws NotFoundException {
      LuminanceSource source = this.getLuminanceSource();
      int width = source.getWidth();
      if (row != null && row.getSize() >= width) {
         row.clear();
      } else {
         row = new BitArray(width);
      }

      this.initArrays(width);
      byte[] localLuminances = source.getRow(y, this.luminances);
      int[] localBuckets = this.buckets;

      for (int x = 0; x < width; x++) {
         localBuckets[(localLuminances[x] & 255) >> 3]++;
      }

      int blackPoint = estimateBlackPoint(localBuckets);
      if (width < 3) {
         for (int x = 0; x < width; x++) {
            if ((localLuminances[x] & 255) < blackPoint) {
               row.set(x);
            }
         }
      } else {
         int left = localLuminances[0] & 255;
         int center = localLuminances[1] & 255;

         for (int xx = 1; xx < width - 1; xx++) {
            int right = localLuminances[xx + 1] & 255;
            if ((center * 4 - left - right) / 2 < blackPoint) {
               row.set(xx);
            }

            left = center;
            center = right;
         }
      }

      return row;
   }

   @Override
   public BitMatrix getBlackMatrix() throws NotFoundException {
      LuminanceSource source = this.getLuminanceSource();
      int width = source.getWidth();
      int height = source.getHeight();
      BitMatrix matrix = new BitMatrix(width, height);
      this.initArrays(width);
      int[] localBuckets = this.buckets;

      for (int y = 1; y < 5; y++) {
         int row = height * y / 5;
         byte[] localLuminances = source.getRow(row, this.luminances);
         int right = width * 4 / 5;

         for (int x = width / 5; x < right; x++) {
            int pixel = localLuminances[x] & 255;
            localBuckets[pixel >> 3]++;
         }
      }

      int blackPoint = estimateBlackPoint(localBuckets);
      byte[] localLuminances = source.getMatrix();

      for (int y = 0; y < height; y++) {
         int offset = y * width;

         for (int x = 0; x < width; x++) {
            int pixel = localLuminances[offset + x] & 255;
            if (pixel < blackPoint) {
               matrix.set(x, y);
            }
         }
      }

      return matrix;
   }

   @Override
   public Binarizer createBinarizer(LuminanceSource source) {
      return new GlobalHistogramBinarizer(source);
   }

   private void initArrays(int luminanceSize) {
      if (this.luminances.length < luminanceSize) {
         this.luminances = new byte[luminanceSize];
      }

      for (int x = 0; x < 32; x++) {
         this.buckets[x] = 0;
      }
   }

   private static int estimateBlackPoint(int[] buckets) throws NotFoundException {
      int numBuckets = buckets.length;
      int maxBucketCount = 0;
      int firstPeak = 0;
      int firstPeakSize = 0;

      for (int x = 0; x < numBuckets; x++) {
         if (buckets[x] > firstPeakSize) {
            firstPeak = x;
            firstPeakSize = buckets[x];
         }

         if (buckets[x] > maxBucketCount) {
            maxBucketCount = buckets[x];
         }
      }

      int secondPeak = 0;
      int secondPeakScore = 0;

      for (int x = 0; x < numBuckets; x++) {
         int distanceToBiggest = x - firstPeak;
         int score = buckets[x] * distanceToBiggest * distanceToBiggest;
         if (score > secondPeakScore) {
            secondPeak = x;
            secondPeakScore = score;
         }
      }

      if (firstPeak > secondPeak) {
         int temp = firstPeak;
         firstPeak = secondPeak;
         secondPeak = temp;
      }

      if (secondPeak - firstPeak <= numBuckets / 16) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         int bestValley = secondPeak - 1;
         int bestValleyScore = -1;

         for (int xx = secondPeak - 1; xx > firstPeak; xx--) {
            int fromFirst = xx - firstPeak;
            int score = fromFirst * fromFirst * (secondPeak - xx) * (maxBucketCount - buckets[xx]);
            if (score > bestValleyScore) {
               bestValley = xx;
               bestValleyScore = score;
            }
         }

         return bestValley << 3;
      }
   }
}
