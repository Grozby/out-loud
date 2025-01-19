package com.google.zxing.oned.rss.expanded;

import java.util.ArrayList;
import java.util.List;

final class ExpandedRow {
   private final List<ExpandedPair> pairs;
   private final int rowNumber;

   ExpandedRow(List<ExpandedPair> pairs, int rowNumber) {
      this.pairs = new ArrayList<>(pairs);
      this.rowNumber = rowNumber;
   }

   List<ExpandedPair> getPairs() {
      return this.pairs;
   }

   int getRowNumber() {
      return this.rowNumber;
   }

   boolean isEquivalent(List<ExpandedPair> otherPairs) {
      return this.pairs.equals(otherPairs);
   }

   @Override
   public String toString() {
      return "{ " + this.pairs + " }";
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof ExpandedRow)) {
         return false;
      } else {
         ExpandedRow that = (ExpandedRow)o;
         return this.pairs.equals(that.pairs);
      }
   }

   @Override
   public int hashCode() {
      return this.pairs.hashCode();
   }
}
