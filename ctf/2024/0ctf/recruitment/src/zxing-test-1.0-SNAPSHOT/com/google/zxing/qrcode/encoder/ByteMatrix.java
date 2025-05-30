package com.google.zxing.qrcode.encoder;

import java.util.Arrays;

public final class ByteMatrix {
   private final byte[][] bytes;
   private final int width;
   private final int height;

   public ByteMatrix(int width, int height) {
      this.bytes = new byte[height][width];
      this.width = width;
      this.height = height;
   }

   public int getHeight() {
      return this.height;
   }

   public int getWidth() {
      return this.width;
   }

   public byte get(int x, int y) {
      return this.bytes[y][x];
   }

   public byte[][] getArray() {
      return this.bytes;
   }

   public void set(int x, int y, byte value) {
      this.bytes[y][x] = value;
   }

   public void set(int x, int y, int value) {
      this.bytes[y][x] = (byte)value;
   }

   public void set(int x, int y, boolean value) {
      this.bytes[y][x] = (byte)(value ? 1 : 0);
   }

   public void clear(byte value) {
      for (byte[] aByte : this.bytes) {
         Arrays.fill(aByte, value);
      }
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder(2 * this.width * this.height + 2);

      for (int y = 0; y < this.height; y++) {
         byte[] bytesY = this.bytes[y];

         for (int x = 0; x < this.width; x++) {
            switch (bytesY[x]) {
               case 0:
                  result.append(" 0");
                  break;
               case 1:
                  result.append(" 1");
                  break;
               default:
                  result.append("  ");
            }
         }

         result.append('\n');
      }

      return result.toString();
   }
}
