package com.google.zxing.oned.rss.expanded;

import com.google.zxing.oned.rss.DataCharacter;
import com.google.zxing.oned.rss.FinderPattern;
import java.util.Objects;

final class ExpandedPair {
   private final DataCharacter leftChar;
   private final DataCharacter rightChar;
   private final FinderPattern finderPattern;

   ExpandedPair(DataCharacter leftChar, DataCharacter rightChar, FinderPattern finderPattern) {
      this.leftChar = leftChar;
      this.rightChar = rightChar;
      this.finderPattern = finderPattern;
   }

   DataCharacter getLeftChar() {
      return this.leftChar;
   }

   DataCharacter getRightChar() {
      return this.rightChar;
   }

   FinderPattern getFinderPattern() {
      return this.finderPattern;
   }

   boolean mustBeLast() {
      return this.rightChar == null;
   }

   @Override
   public String toString() {
      return "[ " + this.leftChar + " , " + this.rightChar + " : " + (this.finderPattern == null ? "null" : this.finderPattern.getValue()) + " ]";
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof ExpandedPair)) {
         return false;
      } else {
         ExpandedPair that = (ExpandedPair)o;
         return Objects.equals(this.leftChar, that.leftChar)
            && Objects.equals(this.rightChar, that.rightChar)
            && Objects.equals(this.finderPattern, that.finderPattern);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.leftChar) ^ Objects.hashCode(this.rightChar) ^ Objects.hashCode(this.finderPattern);
   }
}
