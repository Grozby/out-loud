package com.google.zxing.oned.rss.expanded.decoders;

final class BlockParsedResult {
   private final DecodedInformation decodedInformation;
   private final boolean finished;

   BlockParsedResult() {
      this(null, false);
   }

   BlockParsedResult(DecodedInformation information, boolean finished) {
      this.finished = finished;
      this.decodedInformation = information;
   }

   DecodedInformation getDecodedInformation() {
      return this.decodedInformation;
   }

   boolean isFinished() {
      return this.finished;
   }
}
