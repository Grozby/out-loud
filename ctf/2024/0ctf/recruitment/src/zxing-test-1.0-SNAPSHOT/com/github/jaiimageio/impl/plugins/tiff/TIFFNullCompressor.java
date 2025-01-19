package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFCompressor;
import java.io.IOException;

public class TIFFNullCompressor extends TIFFCompressor {
   public TIFFNullCompressor() {
      super("", 1, true);
   }

   @Override
   public int encode(byte[] b, int off, int width, int height, int[] bitsPerSample, int scanlineStride) throws IOException {
      int bitsPerPixel = 0;

      for (int i = 0; i < bitsPerSample.length; i++) {
         bitsPerPixel += bitsPerSample[i];
      }

      int bytesPerRow = (bitsPerPixel * width + 7) / 8;
      int numBytes = height * bytesPerRow;
      if (bytesPerRow == scanlineStride) {
         this.stream.write(b, off, numBytes);
      } else {
         for (int row = 0; row < height; row++) {
            this.stream.write(b, off, bytesPerRow);
            off += scanlineStride;
         }
      }

      return numBytes;
   }
}
