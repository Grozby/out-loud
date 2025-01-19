package com.google.zxing.aztec;

import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;

public final class AztecDetectorResult extends DetectorResult {
   private final boolean compact;
   private final int nbDatablocks;
   private final int nbLayers;
   private final int errorsCorrected;

   public AztecDetectorResult(BitMatrix bits, ResultPoint[] points, boolean compact, int nbDatablocks, int nbLayers) {
      this(bits, points, compact, nbDatablocks, nbLayers, 0);
   }

   public AztecDetectorResult(BitMatrix bits, ResultPoint[] points, boolean compact, int nbDatablocks, int nbLayers, int errorsCorrected) {
      super(bits, points);
      this.compact = compact;
      this.nbDatablocks = nbDatablocks;
      this.nbLayers = nbLayers;
      this.errorsCorrected = errorsCorrected;
   }

   public int getNbLayers() {
      return this.nbLayers;
   }

   public int getNbDatablocks() {
      return this.nbDatablocks;
   }

   public boolean isCompact() {
      return this.compact;
   }

   public int getErrorsCorrected() {
      return this.errorsCorrected;
   }
}
