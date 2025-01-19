package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFDecompressor;
import java.io.IOException;

public class TIFFLSBDecompressor extends TIFFDecompressor {
   private static byte[] flipTable = TIFFFaxDecompressor.flipTable;

   @Override
   public void decodeRaw(byte[] b, int dstOffset, int bitsPerPixel, int scanlineStride) throws IOException {
      this.stream.seek(this.offset);
      int bytesPerRow = (this.srcWidth * bitsPerPixel + 7) / 8;
      if (bytesPerRow == scanlineStride) {
         int numBytes = bytesPerRow * this.srcHeight;
         this.stream.readFully(b, dstOffset, numBytes);
         int xMax = dstOffset + numBytes;

         for (int x = dstOffset; x < xMax; x++) {
            b[x] = flipTable[b[x] & 255];
         }
      } else {
         for (int y = 0; y < this.srcHeight; y++) {
            this.stream.readFully(b, dstOffset, bytesPerRow);
            int xMax = dstOffset + bytesPerRow;

            for (int x = dstOffset; x < xMax; x++) {
               b[x] = flipTable[b[x] & 255];
            }

            dstOffset += scanlineStride;
         }
      }
   }
}
