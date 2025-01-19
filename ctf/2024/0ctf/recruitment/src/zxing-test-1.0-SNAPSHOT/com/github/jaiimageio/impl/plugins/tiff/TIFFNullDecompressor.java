package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFDecompressor;
import java.io.IOException;

public class TIFFNullDecompressor extends TIFFDecompressor {
   private static final boolean DEBUG = false;
   private boolean isReadActiveOnly = false;
   private int originalSrcMinX;
   private int originalSrcMinY;
   private int originalSrcWidth;
   private int originalSrcHeight;

   @Override
   public void beginDecoding() {
      int bitsPerPixel = 0;

      for (int i = 0; i < this.bitsPerSample.length; i++) {
         bitsPerPixel += this.bitsPerSample[i];
      }

      if ((
            this.activeSrcMinX != this.srcMinX
               || this.activeSrcMinY != this.srcMinY
               || this.activeSrcWidth != this.srcWidth
               || this.activeSrcHeight != this.srcHeight
         )
         && (this.activeSrcMinX - this.srcMinX) * bitsPerPixel % 8 == 0) {
         this.isReadActiveOnly = true;
         this.originalSrcMinX = this.srcMinX;
         this.originalSrcMinY = this.srcMinY;
         this.originalSrcWidth = this.srcWidth;
         this.originalSrcHeight = this.srcHeight;
         this.srcMinX = this.activeSrcMinX;
         this.srcMinY = this.activeSrcMinY;
         this.srcWidth = this.activeSrcWidth;
         this.srcHeight = this.activeSrcHeight;
      } else {
         this.isReadActiveOnly = false;
      }

      super.beginDecoding();
   }

   @Override
   public void decode() throws IOException {
      super.decode();
      if (this.isReadActiveOnly) {
         this.srcMinX = this.originalSrcMinX;
         this.srcMinY = this.originalSrcMinY;
         this.srcWidth = this.originalSrcWidth;
         this.srcHeight = this.originalSrcHeight;
         this.isReadActiveOnly = false;
      }
   }

   @Override
   public void decodeRaw(byte[] b, int dstOffset, int bitsPerPixel, int scanlineStride) throws IOException {
      if (this.isReadActiveOnly) {
         int activeBytesPerRow = (this.activeSrcWidth * bitsPerPixel + 7) / 8;
         int totalBytesPerRow = (this.originalSrcWidth * bitsPerPixel + 7) / 8;
         int bytesToSkipPerRow = totalBytesPerRow - activeBytesPerRow;
         this.stream
            .seek(
               this.offset
                  + (long)((this.activeSrcMinY - this.originalSrcMinY) * totalBytesPerRow)
                  + (long)((this.activeSrcMinX - this.originalSrcMinX) * bitsPerPixel / 8)
            );
         int lastRow = this.activeSrcHeight - 1;

         for (int y = 0; y < this.activeSrcHeight; y++) {
            this.stream.read(b, dstOffset, activeBytesPerRow);
            dstOffset += scanlineStride;
            if (y != lastRow) {
               this.stream.skipBytes(bytesToSkipPerRow);
            }
         }
      } else {
         this.stream.seek(this.offset);
         int bytesPerRow = (this.srcWidth * bitsPerPixel + 7) / 8;
         if (bytesPerRow == scanlineStride) {
            this.stream.read(b, dstOffset, bytesPerRow * this.srcHeight);
         } else {
            for (int yx = 0; yx < this.srcHeight; yx++) {
               this.stream.read(b, dstOffset, bytesPerRow);
               dstOffset += scanlineStride;
            }
         }
      }
   }
}
