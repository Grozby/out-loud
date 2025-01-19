package com.google.zxing.pdf417.detector;

import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import java.util.List;

public final class PDF417DetectorResult {
   private final BitMatrix bits;
   private final List<ResultPoint[]> points;
   private final int rotation;

   public PDF417DetectorResult(BitMatrix bits, List<ResultPoint[]> points, int rotation) {
      this.bits = bits;
      this.points = points;
      this.rotation = rotation;
   }

   public PDF417DetectorResult(BitMatrix bits, List<ResultPoint[]> points) {
      this(bits, points, 0);
   }

   public BitMatrix getBits() {
      return this.bits;
   }

   public List<ResultPoint[]> getPoints() {
      return this.points;
   }

   public int getRotation() {
      return this.rotation;
   }
}
