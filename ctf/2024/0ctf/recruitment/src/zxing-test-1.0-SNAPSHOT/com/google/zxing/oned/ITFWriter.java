package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import java.util.Collection;
import java.util.Collections;

public final class ITFWriter extends OneDimensionalCodeWriter {
   private static final int[] START_PATTERN = new int[]{1, 1, 1, 1};
   private static final int[] END_PATTERN = new int[]{3, 1, 1};
   private static final int W = 3;
   private static final int N = 1;
   private static final int[][] PATTERNS = new int[][]{
      {1, 1, 3, 3, 1},
      {3, 1, 1, 1, 3},
      {1, 3, 1, 1, 3},
      {3, 3, 1, 1, 1},
      {1, 1, 3, 1, 3},
      {3, 1, 3, 1, 1},
      {1, 3, 3, 1, 1},
      {1, 1, 1, 3, 3},
      {3, 1, 1, 3, 1},
      {1, 3, 1, 3, 1}
   };

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.ITF);
   }

   @Override
   public boolean[] encode(String contents) {
      int length = contents.length();
      if (length % 2 != 0) {
         throw new IllegalArgumentException("The length of the input should be even");
      } else if (length > 80) {
         throw new IllegalArgumentException("Requested contents should be less than 80 digits long, but got " + length);
      } else {
         checkNumeric(contents);
         boolean[] result = new boolean[9 + 9 * length];
         int pos = appendPattern(result, 0, START_PATTERN, true);

         for (int i = 0; i < length; i += 2) {
            int one = Character.digit(contents.charAt(i), 10);
            int two = Character.digit(contents.charAt(i + 1), 10);
            int[] encoding = new int[10];

            for (int j = 0; j < 5; j++) {
               encoding[2 * j] = PATTERNS[one][j];
               encoding[2 * j + 1] = PATTERNS[two][j];
            }

            pos += appendPattern(result, pos, encoding, true);
         }

         appendPattern(result, pos, END_PATTERN, true);
         return result;
      }
   }
}
