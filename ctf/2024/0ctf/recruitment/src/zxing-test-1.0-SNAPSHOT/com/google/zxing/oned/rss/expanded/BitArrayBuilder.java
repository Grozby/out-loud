package com.google.zxing.oned.rss.expanded;

import com.google.zxing.common.BitArray;
import java.util.List;

final class BitArrayBuilder {
   private BitArrayBuilder() {
   }

   static BitArray buildBitArray(List<ExpandedPair> pairs) {
      int charNumber = pairs.size() * 2 - 1;
      if (pairs.get(pairs.size() - 1).getRightChar() == null) {
         charNumber--;
      }

      int size = 12 * charNumber;
      BitArray binary = new BitArray(size);
      int accPos = 0;
      ExpandedPair firstPair = pairs.get(0);
      int firstValue = firstPair.getRightChar().getValue();

      for (int i = 11; i >= 0; i--) {
         if ((firstValue & 1 << i) != 0) {
            binary.set(accPos);
         }

         accPos++;
      }

      for (int i = 1; i < pairs.size(); i++) {
         ExpandedPair currentPair = pairs.get(i);
         int leftValue = currentPair.getLeftChar().getValue();

         for (int j = 11; j >= 0; j--) {
            if ((leftValue & 1 << j) != 0) {
               binary.set(accPos);
            }

            accPos++;
         }

         if (currentPair.getRightChar() != null) {
            int rightValue = currentPair.getRightChar().getValue();

            for (int j = 11; j >= 0; j--) {
               if ((rightValue & 1 << j) != 0) {
                  binary.set(accPos);
               }

               accPos++;
            }
         }
      }

      return binary;
   }
}
