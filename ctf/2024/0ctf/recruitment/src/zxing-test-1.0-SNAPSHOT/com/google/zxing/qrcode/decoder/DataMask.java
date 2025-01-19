package com.google.zxing.qrcode.decoder;

import com.google.zxing.common.BitMatrix;

enum DataMask {
   DATA_MASK_000 {
      @Override
      boolean isMasked(int i, int j) {
         return (i + j & 1) == 0;
      }
   },
   DATA_MASK_001 {
      @Override
      boolean isMasked(int i, int j) {
         return (i & 1) == 0;
      }
   },
   DATA_MASK_010 {
      @Override
      boolean isMasked(int i, int j) {
         return j % 3 == 0;
      }
   },
   DATA_MASK_011 {
      @Override
      boolean isMasked(int i, int j) {
         return (i + j) % 3 == 0;
      }
   },
   DATA_MASK_100 {
      @Override
      boolean isMasked(int i, int j) {
         return (i / 2 + j / 3 & 1) == 0;
      }
   },
   DATA_MASK_101 {
      @Override
      boolean isMasked(int i, int j) {
         return i * j % 6 == 0;
      }
   },
   DATA_MASK_110 {
      @Override
      boolean isMasked(int i, int j) {
         return i * j % 6 < 3;
      }
   },
   DATA_MASK_111 {
      @Override
      boolean isMasked(int i, int j) {
         return (i + j + i * j % 3 & 1) == 0;
      }
   };

   private DataMask() {
   }

   final void unmaskBitMatrix(BitMatrix bits, int dimension) {
      for (int i = 0; i < dimension; i++) {
         for (int j = 0; j < dimension; j++) {
            if (this.isMasked(i, j)) {
               bits.flip(j, i);
            }
         }
      }
   }

   abstract boolean isMasked(int var1, int var2);
}
