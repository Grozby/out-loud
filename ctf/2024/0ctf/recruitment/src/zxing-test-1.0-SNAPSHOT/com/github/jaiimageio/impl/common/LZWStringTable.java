package com.github.jaiimageio.impl.common;

import java.io.PrintStream;

public class LZWStringTable {
   private static final int RES_CODES = 2;
   private static final short HASH_FREE = -1;
   private static final short NEXT_FIRST = -1;
   private static final int MAXBITS = 12;
   private static final int MAXSTR = 4096;
   private static final short HASHSIZE = 9973;
   private static final short HASHSTEP = 2039;
   byte[] strChr_ = new byte[4096];
   short[] strNxt_ = new short[4096];
   short[] strHsh_;
   short numStrings_;
   int[] strLen_ = new int[4096];

   public LZWStringTable() {
      this.strHsh_ = new short[9973];
   }

   public int AddCharString(short index, byte b) {
      if (this.numStrings_ >= 4096) {
         return 65535;
      } else {
         int hshidx = Hash(index, b);

         while (this.strHsh_[hshidx] != -1) {
            hshidx = (hshidx + 2039) % 9973;
         }

         this.strHsh_[hshidx] = this.numStrings_;
         this.strChr_[this.numStrings_] = b;
         if (index == -1) {
            this.strNxt_[this.numStrings_] = -1;
            this.strLen_[this.numStrings_] = 1;
         } else {
            this.strNxt_[this.numStrings_] = index;
            this.strLen_[this.numStrings_] = this.strLen_[index] + 1;
         }

         return this.numStrings_++;
      }
   }

   public short FindCharString(short index, byte b) {
      if (index == -1) {
         return (short)(b & 255);
      } else {
         int nxtidx;
         for (int hshidx = Hash(index, b); (nxtidx = this.strHsh_[hshidx]) != -1; hshidx = (hshidx + 2039) % 9973) {
            if (this.strNxt_[nxtidx] == index && this.strChr_[nxtidx] == b) {
               return (short)nxtidx;
            }
         }

         return -1;
      }
   }

   public void ClearTable(int codesize) {
      this.numStrings_ = 0;

      for (int q = 0; q < 9973; q++) {
         this.strHsh_[q] = -1;
      }

      int w = (1 << codesize) + 2;

      for (int q = 0; q < w; q++) {
         this.AddCharString((short)-1, (byte)q);
      }
   }

   public static int Hash(short index, byte lastbyte) {
      return (((short)(lastbyte << 8) ^ index) & 65535) % 9973;
   }

   public int expandCode(byte[] buf, int offset, short code, int skipHead) {
      if (offset == -2 && skipHead == 1) {
         skipHead = 0;
      }

      if (code != -1 && skipHead != this.strLen_[code]) {
         int codeLen = this.strLen_[code] - skipHead;
         int bufSpace = buf.length - offset;
         int expandLen;
         if (bufSpace > codeLen) {
            expandLen = codeLen;
         } else {
            expandLen = bufSpace;
         }

         int skipTail = codeLen - expandLen;

         for (int idx = offset + expandLen; idx > offset && code != -1; code = this.strNxt_[code]) {
            if (--skipTail < 0) {
               idx--;
               buf[idx] = this.strChr_[code];
            }
         }

         return codeLen > expandLen ? -expandLen : expandLen;
      } else {
         return 0;
      }
   }

   public void dump(PrintStream out) {
      for (int i = 258; i < this.numStrings_; i++) {
         out.println(
            " strNxt_["
               + i
               + "] = "
               + this.strNxt_[i]
               + " strChr_ "
               + Integer.toHexString(this.strChr_[i] & 255)
               + " strLen_ "
               + Integer.toHexString(this.strLen_[i])
         );
      }
   }
}
