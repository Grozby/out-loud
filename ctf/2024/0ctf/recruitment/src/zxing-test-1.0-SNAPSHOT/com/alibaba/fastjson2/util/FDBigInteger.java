package com.alibaba.fastjson2.util;

import java.util.Arrays;

public final class FDBigInteger {
   private static final int[] SMALL_5_POW = new int[]{1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125};
   private static final FDBigInteger[] POW_5_CACHE = new FDBigInteger[340];
   private int[] data;
   private int offset;
   private int nWords;
   private boolean immutable;

   private FDBigInteger(int[] data, int offset) {
      this.data = data;
      this.offset = offset;
      this.nWords = data.length;
      this.trimLeadingZeros();
   }

   public FDBigInteger(long lValue, char[] digits, int kDigits, int nDigits) {
      int n = Math.max((nDigits + 8) / 9, 2);
      this.data = new int[n];
      this.data[0] = (int)lValue;
      this.data[1] = (int)(lValue >>> 32);
      this.offset = 0;
      this.nWords = 2;
      int i = kDigits;
      int limit = nDigits - 5;

      while (i < limit) {
         int ilim = i + 5;
         int v = digits[i++] - '0';

         while (i < ilim) {
            v = 10 * v + digits[i++] - 48;
         }

         this.multAddMe(100000, v);
      }

      int factor = 1;

      int v;
      for (v = 0; i < nDigits; factor *= 10) {
         v = 10 * v + digits[i++] - 48;
      }

      if (factor != 1) {
         this.multAddMe(factor, v);
      }

      this.trimLeadingZeros();
   }

   public void makeImmutable() {
      this.immutable = true;
   }

   private void multAddMe(int iv, int addend) {
      long v = (long)iv & 4294967295L;
      long p = v * ((long)this.data[0] & 4294967295L) + ((long)addend & 4294967295L);
      this.data[0] = (int)p;
      p >>>= 32;

      for (int i = 1; i < this.nWords; i++) {
         p += v * ((long)this.data[i] & 4294967295L);
         this.data[i] = (int)p;
         p >>>= 32;
      }

      if (p != 0L) {
         this.data[this.nWords++] = (int)p;
      }
   }

   private void trimLeadingZeros() {
      int i = this.nWords;
      if (i > 0) {
         if (this.data[--i] == 0) {
            while (i > 0 && this.data[i - 1] == 0) {
               i--;
            }

            this.nWords = i;
            if (i == 0) {
               this.offset = 0;
            }
         }
      }
   }

   private int size() {
      return this.nWords + this.offset;
   }

   public FDBigInteger multByPow52(int p5, int p2) {
      if (this.nWords == 0) {
         return this;
      } else {
         FDBigInteger res = this;
         if (p5 != 0) {
            int extraSize = p2 != 0 ? 1 : 0;
            if (p5 < SMALL_5_POW.length) {
               int[] r = new int[this.nWords + 1 + extraSize];
               mult(this.data, this.nWords, SMALL_5_POW[p5], r);
               res = new FDBigInteger(r, this.offset);
            } else {
               FDBigInteger pow5 = big5pow(p5);
               int[] r = new int[this.nWords + pow5.size() + extraSize];
               mult(this.data, this.nWords, pow5.data, pow5.nWords, r);
               res = new FDBigInteger(r, this.offset + pow5.offset);
            }
         }

         return res.leftShift(p2);
      }
   }

   private static FDBigInteger big5pow(int p) {
      assert p >= 0 : p;

      return p < 340 ? POW_5_CACHE[p] : big5powRec(p);
   }

   private FDBigInteger mult(int i) {
      if (this.nWords == 0) {
         return this;
      } else {
         int[] r = new int[this.nWords + 1];
         mult(this.data, this.nWords, i, r);
         return new FDBigInteger(r, this.offset);
      }
   }

   private FDBigInteger mult(FDBigInteger other) {
      if (this.nWords == 0) {
         return this;
      } else if (this.size() == 1) {
         return other.mult(this.data[0]);
      } else if (other.nWords == 0) {
         return other;
      } else if (other.size() == 1) {
         return this.mult(other.data[0]);
      } else {
         int[] r = new int[this.nWords + other.nWords];
         mult(this.data, this.nWords, other.data, other.nWords, r);
         return new FDBigInteger(r, this.offset + other.offset);
      }
   }

   private static void mult(int[] src, int srcLen, int value, int[] dst) {
      long val = (long)value & 4294967295L;
      long carry = 0L;

      for (int i = 0; i < srcLen; i++) {
         long product = ((long)src[i] & 4294967295L) * val + carry;
         dst[i] = (int)product;
         carry = product >>> 32;
      }

      dst[srcLen] = (int)carry;
   }

   private static void mult(int[] s1, int s1Len, int[] s2, int s2Len, int[] dst) {
      for (int i = 0; i < s1Len; i++) {
         long v = (long)s1[i] & 4294967295L;
         long p = 0L;

         for (int j = 0; j < s2Len; j++) {
            p += ((long)dst[i + j] & 4294967295L) + v * ((long)s2[j] & 4294967295L);
            dst[i + j] = (int)p;
            p >>>= 32;
         }

         dst[i + s2Len] = (int)p;
      }
   }

   private static void mult(int[] src, int srcLen, int v0, int v1, int[] dst) {
      long v = (long)v0 & 4294967295L;
      long carry = 0L;

      for (int j = 0; j < srcLen; j++) {
         long product = v * ((long)src[j] & 4294967295L) + carry;
         dst[j] = (int)product;
         carry = product >>> 32;
      }

      dst[srcLen] = (int)carry;
      v = (long)v1 & 4294967295L;
      carry = 0L;

      for (int j = 0; j < srcLen; j++) {
         long product = ((long)dst[j + 1] & 4294967295L) + v * ((long)src[j] & 4294967295L) + carry;
         dst[j + 1] = (int)product;
         carry = product >>> 32;
      }

      dst[srcLen + 1] = (int)carry;
   }

   private static void leftShift(int[] src, int idx, int[] result, int bitcount, int anticount, int prev) {
      while (idx > 0) {
         int v = prev << bitcount;
         prev = src[idx - 1];
         v |= prev >>> anticount;
         result[idx] = v;
         idx--;
      }

      int v = prev << bitcount;
      result[0] = v;
   }

   public FDBigInteger leftShift(int shift) {
      int[] data = this.data;
      int nWords = this.nWords;
      int offset = this.offset;
      if (shift != 0 && nWords != 0) {
         int wordcount = shift >> 5;
         int bitcount = shift & 31;
         if (this.immutable) {
            if (bitcount == 0) {
               return new FDBigInteger(Arrays.copyOf(data, nWords), offset + wordcount);
            } else {
               int anticount = 32 - bitcount;
               int idx = nWords - 1;
               int prev = data[idx];
               int hi = prev >>> anticount;
               int[] result;
               if (hi != 0) {
                  result = new int[nWords + 1];
                  result[nWords] = hi;
               } else {
                  result = new int[nWords];
               }

               leftShift(data, idx, result, bitcount, anticount, prev);
               return new FDBigInteger(result, offset + wordcount);
            }
         } else {
            if (bitcount != 0) {
               int anticount = 32 - bitcount;
               if (data[0] << bitcount == 0) {
                  int idx = 0;

                  int prev;
                  for (prev = data[idx]; idx < nWords - 1; idx++) {
                     int v = prev >>> anticount;
                     prev = data[idx + 1];
                     v |= prev << bitcount;
                     data[idx] = v;
                  }

                  int v = prev >>> anticount;
                  data[idx] = v;
                  if (v == 0) {
                     nWords--;
                  }

                  offset++;
               } else {
                  int idx = nWords - 1;
                  int prevx = data[idx];
                  int hi = prevx >>> anticount;
                  int[] result = data;
                  if (hi != 0) {
                     if (nWords == data.length) {
                        this.data = result = new int[nWords + 1];
                     }

                     result[nWords++] = hi;
                  }

                  leftShift(data, idx, result, bitcount, anticount, prevx);
               }
            }

            this.nWords = nWords;
            this.offset = offset + wordcount;
            return this;
         }
      } else {
         return this;
      }
   }

   private static FDBigInteger big5powRec(int p) {
      if (p < 340) {
         return POW_5_CACHE[p];
      } else {
         int q = p >> 1;
         int r = p - q;
         FDBigInteger bigq = big5powRec(q);
         return r < SMALL_5_POW.length ? bigq.mult(SMALL_5_POW[r]) : bigq.mult(big5powRec(r));
      }
   }

   public static FDBigInteger valueOfMulPow52(long value, int p5, int p2) {
      assert p5 >= 0 : p5;

      assert p2 >= 0 : p2;

      int v0 = (int)value;
      int v1 = (int)(value >>> 32);
      int wordcount = p2 >> 5;
      int bitcount = p2 & 31;
      if (p5 != 0) {
         if (p5 < SMALL_5_POW.length) {
            long pow5 = (long)SMALL_5_POW[p5] & 4294967295L;
            long carry = ((long)v0 & 4294967295L) * pow5;
            v0 = (int)carry;
            carry >>>= 32;
            carry = ((long)v1 & 4294967295L) * pow5 + carry;
            v1 = (int)carry;
            int v2 = (int)(carry >>> 32);
            return bitcount == 0
               ? new FDBigInteger(new int[]{v0, v1, v2}, wordcount)
               : new FDBigInteger(
                  new int[]{v0 << bitcount, v1 << bitcount | v0 >>> 32 - bitcount, v2 << bitcount | v1 >>> 32 - bitcount, v2 >>> 32 - bitcount}, wordcount
               );
         } else {
            FDBigInteger pow5 = big5pow(p5);
            int[] r;
            if (v1 == 0) {
               r = new int[pow5.nWords + 1 + (p2 != 0 ? 1 : 0)];
               mult(pow5.data, pow5.nWords, v0, r);
            } else {
               r = new int[pow5.nWords + 2 + (p2 != 0 ? 1 : 0)];
               mult(pow5.data, pow5.nWords, v0, v1, r);
            }

            return new FDBigInteger(r, pow5.offset).leftShift(p2);
         }
      } else if (p2 != 0) {
         return bitcount == 0
            ? new FDBigInteger(new int[]{v0, v1}, wordcount)
            : new FDBigInteger(new int[]{v0 << bitcount, v1 << bitcount | v0 >>> 32 - bitcount, v1 >>> 32 - bitcount}, wordcount);
      } else {
         return new FDBigInteger(new int[]{v0, v1}, 0);
      }
   }

   public int cmp(FDBigInteger other) {
      int aSize = this.nWords + this.offset;
      int bSize = other.nWords + other.offset;
      if (aSize > bSize) {
         return 1;
      } else if (aSize < bSize) {
         return -1;
      } else {
         int aLen = this.nWords;
         int bLen = other.nWords;

         while (aLen > 0 && bLen > 0) {
            int a = this.data[--aLen];
            int b = other.data[--bLen];
            if (a != b) {
               return ((long)a & 4294967295L) < ((long)b & 4294967295L) ? -1 : 1;
            }
         }

         if (aLen > 0) {
            return checkZeroTail(this.data, aLen);
         } else {
            return bLen > 0 ? -checkZeroTail(other.data, bLen) : 0;
         }
      }
   }

   private static int checkZeroTail(int[] a, int from) {
      while (from > 0) {
         if (a[--from] != 0) {
            return 1;
         }
      }

      return 0;
   }

   public FDBigInteger leftInplaceSub(FDBigInteger subtrahend) {
      assert this.size() >= subtrahend.size() : "result should be positive";

      FDBigInteger minuend;
      if (this.immutable) {
         minuend = new FDBigInteger((int[])this.data.clone(), this.offset);
      } else {
         minuend = this;
      }

      int offsetDiff = subtrahend.offset - minuend.offset;
      int[] sData = subtrahend.data;
      int[] mData = minuend.data;
      int subLen = subtrahend.nWords;
      int minLen = minuend.nWords;
      if (offsetDiff < 0) {
         int rLen = minLen - offsetDiff;
         if (rLen < mData.length) {
            System.arraycopy(mData, 0, mData, -offsetDiff, minLen);
            Arrays.fill(mData, 0, -offsetDiff, 0);
         } else {
            int[] r = new int[rLen];
            System.arraycopy(mData, 0, r, -offsetDiff, minLen);
            mData = r;
            minuend.data = r;
         }

         minuend.offset = subtrahend.offset;
         minLen = rLen;
         minuend.nWords = rLen;
         offsetDiff = 0;
      }

      long borrow = 0L;
      int mIndex = offsetDiff;

      for (int sIndex = 0; sIndex < subLen && mIndex < minLen; mIndex++) {
         long diff = ((long)mData[mIndex] & 4294967295L) - ((long)sData[sIndex] & 4294967295L) + borrow;
         mData[mIndex] = (int)diff;
         borrow = diff >> 32;
         sIndex++;
      }

      while (borrow != 0L && mIndex < minLen) {
         long diff = ((long)mData[mIndex] & 4294967295L) + borrow;
         mData[mIndex] = (int)diff;
         borrow = diff >> 32;
         mIndex++;
      }

      assert borrow == 0L : borrow;

      minuend.trimLeadingZeros();
      return minuend;
   }

   public FDBigInteger rightInplaceSub(FDBigInteger subtrahend) {
      assert this.size() >= subtrahend.size() : "result should be positive";

      if (subtrahend.immutable) {
         subtrahend = new FDBigInteger((int[])subtrahend.data.clone(), subtrahend.offset);
      }

      int offsetDiff = this.offset - subtrahend.offset;
      int[] sData = subtrahend.data;
      int[] mData = this.data;
      int subLen = subtrahend.nWords;
      int minLen = this.nWords;
      if (offsetDiff < 0) {
         if (minLen < sData.length) {
            System.arraycopy(sData, 0, sData, -offsetDiff, subLen);
            Arrays.fill(sData, 0, -offsetDiff, 0);
         } else {
            int[] r = new int[minLen];
            System.arraycopy(sData, 0, r, -offsetDiff, subLen);
            sData = r;
            subtrahend.data = r;
         }

         subtrahend.offset = this.offset;
         offsetDiff = 0;
      } else {
         int rLen = minLen + offsetDiff;
         if (rLen >= sData.length) {
            subtrahend.data = sData = Arrays.copyOf(sData, rLen);
         }
      }

      int sIndex = 0;

      long borrow;
      for (borrow = 0L; sIndex < offsetDiff; sIndex++) {
         long diff = -((long)sData[sIndex] & 4294967295L) + borrow;
         sData[sIndex] = (int)diff;
         borrow = diff >> 32;
      }

      for (int mIndex = 0; mIndex < minLen; mIndex++) {
         long diff = ((long)mData[mIndex] & 4294967295L) - ((long)sData[sIndex] & 4294967295L) + borrow;
         sData[sIndex] = (int)diff;
         borrow = diff >> 32;
         sIndex++;
      }

      assert borrow == 0L : borrow;

      subtrahend.nWords = sIndex;
      subtrahend.trimLeadingZeros();
      return subtrahend;
   }

   public int cmpPow52(int p5, int p2) {
      if (p5 == 0) {
         int wordcount = p2 >> 5;
         int bitcount = p2 & 31;
         int size = this.nWords + this.offset;
         if (size > wordcount + 1) {
            return 1;
         } else if (size < wordcount + 1) {
            return -1;
         } else {
            int a = this.data[this.nWords - 1];
            int b = 1 << bitcount;
            if (a != b) {
               return ((long)a & 4294967295L) < ((long)b & 4294967295L) ? -1 : 1;
            } else {
               return checkZeroTail(this.data, this.nWords - 1);
            }
         }
      } else {
         return this.cmp(big5pow(p5).leftShift(p2));
      }
   }

   static {
      int i;
      for (i = 0; i < SMALL_5_POW.length; i++) {
         FDBigInteger pow5 = new FDBigInteger(new int[]{SMALL_5_POW[i]}, 0);
         pow5.makeImmutable();
         POW_5_CACHE[i] = pow5;
      }

      for (FDBigInteger prev = POW_5_CACHE[i - 1]; i < 340; i++) {
         POW_5_CACHE[i] = prev = prev.mult(5);
         prev.makeImmutable();
      }
   }
}
