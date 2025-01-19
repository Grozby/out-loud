package com.google.zxing.common;

import java.util.Arrays;

public final class BitMatrix implements Cloneable {
   private int width;
   private int height;
   private int rowSize;
   private int[] bits;

   public BitMatrix(int dimension) {
      this(dimension, dimension);
   }

   public BitMatrix(int width, int height) {
      if (width >= 1 && height >= 1) {
         this.width = width;
         this.height = height;
         this.rowSize = (width + 31) / 32;
         this.bits = new int[this.rowSize * height];
      } else {
         throw new IllegalArgumentException("Both dimensions must be greater than 0");
      }
   }

   private BitMatrix(int width, int height, int rowSize, int[] bits) {
      this.width = width;
      this.height = height;
      this.rowSize = rowSize;
      this.bits = bits;
   }

   public static BitMatrix parse(boolean[][] image) {
      int height = image.length;
      int width = image[0].length;
      BitMatrix bits = new BitMatrix(width, height);

      for (int i = 0; i < height; i++) {
         boolean[] imageI = image[i];

         for (int j = 0; j < width; j++) {
            if (imageI[j]) {
               bits.set(j, i);
            }
         }
      }

      return bits;
   }

   public static BitMatrix parse(String stringRepresentation, String setString, String unsetString) {
      if (stringRepresentation == null) {
         throw new IllegalArgumentException();
      } else {
         boolean[] bits = new boolean[stringRepresentation.length()];
         int bitsPos = 0;
         int rowStartPos = 0;
         int rowLength = -1;
         int nRows = 0;
         int pos = 0;

         while (pos < stringRepresentation.length()) {
            if (stringRepresentation.charAt(pos) != '\n' && stringRepresentation.charAt(pos) != '\r') {
               if (stringRepresentation.startsWith(setString, pos)) {
                  pos += setString.length();
                  bits[bitsPos] = true;
                  bitsPos++;
               } else {
                  if (!stringRepresentation.startsWith(unsetString, pos)) {
                     throw new IllegalArgumentException("illegal character encountered: " + stringRepresentation.substring(pos));
                  }

                  pos += unsetString.length();
                  bits[bitsPos] = false;
                  bitsPos++;
               }
            } else {
               if (bitsPos > rowStartPos) {
                  if (rowLength == -1) {
                     rowLength = bitsPos - rowStartPos;
                  } else if (bitsPos - rowStartPos != rowLength) {
                     throw new IllegalArgumentException("row lengths do not match");
                  }

                  rowStartPos = bitsPos;
                  nRows++;
               }

               pos++;
            }
         }

         if (bitsPos > rowStartPos) {
            if (rowLength == -1) {
               rowLength = bitsPos - rowStartPos;
            } else if (bitsPos - rowStartPos != rowLength) {
               throw new IllegalArgumentException("row lengths do not match");
            }

            nRows++;
         }

         BitMatrix matrix = new BitMatrix(rowLength, nRows);

         for (int i = 0; i < bitsPos; i++) {
            if (bits[i]) {
               matrix.set(i % rowLength, i / rowLength);
            }
         }

         return matrix;
      }
   }

   public boolean get(int x, int y) {
      int offset = y * this.rowSize + x / 32;
      return (this.bits[offset] >>> (x & 31) & 1) != 0;
   }

   public void set(int x, int y) {
      int offset = y * this.rowSize + x / 32;
      this.bits[offset] = this.bits[offset] | 1 << (x & 31);
   }

   public void unset(int x, int y) {
      int offset = y * this.rowSize + x / 32;
      this.bits[offset] = this.bits[offset] & ~(1 << (x & 31));
   }

   public void flip(int x, int y) {
      int offset = y * this.rowSize + x / 32;
      this.bits[offset] = this.bits[offset] ^ 1 << (x & 31);
   }

   public void flip() {
      int max = this.bits.length;

      for (int i = 0; i < max; i++) {
         this.bits[i] = ~this.bits[i];
      }
   }

   public void xor(BitMatrix mask) {
      if (this.width == mask.width && this.height == mask.height && this.rowSize == mask.rowSize) {
         BitArray rowArray = new BitArray(this.width);

         for (int y = 0; y < this.height; y++) {
            int offset = y * this.rowSize;
            int[] row = mask.getRow(y, rowArray).getBitArray();

            for (int x = 0; x < this.rowSize; x++) {
               this.bits[offset + x] = this.bits[offset + x] ^ row[x];
            }
         }
      } else {
         throw new IllegalArgumentException("input matrix dimensions do not match");
      }
   }

   public void clear() {
      int max = this.bits.length;

      for (int i = 0; i < max; i++) {
         this.bits[i] = 0;
      }
   }

   public void setRegion(int left, int top, int width, int height) {
      if (top < 0 || left < 0) {
         throw new IllegalArgumentException("Left and top must be nonnegative");
      } else if (height >= 1 && width >= 1) {
         int right = left + width;
         int bottom = top + height;
         if (bottom <= this.height && right <= this.width) {
            for (int y = top; y < bottom; y++) {
               int offset = y * this.rowSize;

               for (int x = left; x < right; x++) {
                  this.bits[offset + x / 32] = this.bits[offset + x / 32] | 1 << (x & 31);
               }
            }
         } else {
            throw new IllegalArgumentException("The region must fit inside the matrix");
         }
      } else {
         throw new IllegalArgumentException("Height and width must be at least 1");
      }
   }

   public BitArray getRow(int y, BitArray row) {
      if (row != null && row.getSize() >= this.width) {
         row.clear();
      } else {
         row = new BitArray(this.width);
      }

      int offset = y * this.rowSize;

      for (int x = 0; x < this.rowSize; x++) {
         row.setBulk(x * 32, this.bits[offset + x]);
      }

      return row;
   }

   public void setRow(int y, BitArray row) {
      System.arraycopy(row.getBitArray(), 0, this.bits, y * this.rowSize, this.rowSize);
   }

   public void rotate(int degrees) {
      switch (degrees % 360) {
         case 0:
            return;
         case 90:
            this.rotate90();
            return;
         case 180:
            this.rotate180();
            return;
         case 270:
            this.rotate90();
            this.rotate180();
            return;
         default:
            throw new IllegalArgumentException("degrees must be a multiple of 0, 90, 180, or 270");
      }
   }

   public void rotate180() {
      BitArray topRow = new BitArray(this.width);
      BitArray bottomRow = new BitArray(this.width);
      int maxHeight = (this.height + 1) / 2;

      for (int i = 0; i < maxHeight; i++) {
         topRow = this.getRow(i, topRow);
         int bottomRowIndex = this.height - 1 - i;
         bottomRow = this.getRow(bottomRowIndex, bottomRow);
         topRow.reverse();
         bottomRow.reverse();
         this.setRow(i, bottomRow);
         this.setRow(bottomRowIndex, topRow);
      }
   }

   public void rotate90() {
      int newWidth = this.height;
      int newHeight = this.width;
      int newRowSize = (newWidth + 31) / 32;
      int[] newBits = new int[newRowSize * newHeight];

      for (int y = 0; y < this.height; y++) {
         for (int x = 0; x < this.width; x++) {
            int offset = y * this.rowSize + x / 32;
            if ((this.bits[offset] >>> (x & 31) & 1) != 0) {
               int newOffset = (newHeight - 1 - x) * newRowSize + y / 32;
               newBits[newOffset] |= 1 << (y & 31);
            }
         }
      }

      this.width = newWidth;
      this.height = newHeight;
      this.rowSize = newRowSize;
      this.bits = newBits;
   }

   public int[] getEnclosingRectangle() {
      int left = this.width;
      int top = this.height;
      int right = -1;
      int bottom = -1;

      for (int y = 0; y < this.height; y++) {
         for (int x32 = 0; x32 < this.rowSize; x32++) {
            int theBits = this.bits[y * this.rowSize + x32];
            if (theBits != 0) {
               if (y < top) {
                  top = y;
               }

               if (y > bottom) {
                  bottom = y;
               }

               if (x32 * 32 < left) {
                  int bit = 0;

                  while (theBits << 31 - bit == 0) {
                     bit++;
                  }

                  if (x32 * 32 + bit < left) {
                     left = x32 * 32 + bit;
                  }
               }

               if (x32 * 32 + 31 > right) {
                  int bit = 31;

                  while (theBits >>> bit == 0) {
                     bit--;
                  }

                  if (x32 * 32 + bit > right) {
                     right = x32 * 32 + bit;
                  }
               }
            }
         }
      }

      return right >= left && bottom >= top ? new int[]{left, top, right - left + 1, bottom - top + 1} : null;
   }

   public int[] getTopLeftOnBit() {
      int bitsOffset = 0;

      while (bitsOffset < this.bits.length && this.bits[bitsOffset] == 0) {
         bitsOffset++;
      }

      if (bitsOffset == this.bits.length) {
         return null;
      } else {
         int y = bitsOffset / this.rowSize;
         int x = bitsOffset % this.rowSize * 32;
         int theBits = this.bits[bitsOffset];
         int bit = 0;

         while (theBits << 31 - bit == 0) {
            bit++;
         }

         x += bit;
         return new int[]{x, y};
      }
   }

   public int[] getBottomRightOnBit() {
      int bitsOffset = this.bits.length - 1;

      while (bitsOffset >= 0 && this.bits[bitsOffset] == 0) {
         bitsOffset--;
      }

      if (bitsOffset < 0) {
         return null;
      } else {
         int y = bitsOffset / this.rowSize;
         int x = bitsOffset % this.rowSize * 32;
         int theBits = this.bits[bitsOffset];
         int bit = 31;

         while (theBits >>> bit == 0) {
            bit--;
         }

         x += bit;
         return new int[]{x, y};
      }
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public int getRowSize() {
      return this.rowSize;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof BitMatrix)) {
         return false;
      } else {
         BitMatrix other = (BitMatrix)o;
         return this.width == other.width && this.height == other.height && this.rowSize == other.rowSize && Arrays.equals(this.bits, other.bits);
      }
   }

   @Override
   public int hashCode() {
      int hash = this.width;
      hash = 31 * hash + this.width;
      hash = 31 * hash + this.height;
      hash = 31 * hash + this.rowSize;
      return 31 * hash + Arrays.hashCode(this.bits);
   }

   @Override
   public String toString() {
      return this.toString("X ", "  ");
   }

   public String toString(String setString, String unsetString) {
      return this.buildToString(setString, unsetString, "\n");
   }

   @Deprecated
   public String toString(String setString, String unsetString, String lineSeparator) {
      return this.buildToString(setString, unsetString, lineSeparator);
   }

   private String buildToString(String setString, String unsetString, String lineSeparator) {
      StringBuilder result = new StringBuilder(this.height * (this.width + 1));

      for (int y = 0; y < this.height; y++) {
         for (int x = 0; x < this.width; x++) {
            result.append(this.get(x, y) ? setString : unsetString);
         }

         result.append(lineSeparator);
      }

      return result.toString();
   }

   public BitMatrix clone() {
      return new BitMatrix(this.width, this.height, this.rowSize, (int[])this.bits.clone());
   }
}
