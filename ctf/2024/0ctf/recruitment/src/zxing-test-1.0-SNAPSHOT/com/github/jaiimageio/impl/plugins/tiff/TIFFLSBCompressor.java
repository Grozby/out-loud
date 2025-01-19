package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFCompressor;
import java.io.IOException;

public class TIFFLSBCompressor extends TIFFCompressor {
   public TIFFLSBCompressor() {
      super("", 1, true);
   }

   @Override
   public int encode(byte[] b, int off, int width, int height, int[] bitsPerSample, int scanlineStride) throws IOException {
      int bitsPerPixel = 0;

      for (int i = 0; i < bitsPerSample.length; i++) {
         bitsPerPixel += bitsPerSample[i];
      }

      int bytesPerRow = (bitsPerPixel * width + 7) / 8;
      byte[] compData = new byte[bytesPerRow];
      byte[] flipTable = TIFFFaxDecompressor.flipTable;

      for (int row = 0; row < height; row++) {
         System.arraycopy(b, off, compData, 0, bytesPerRow);

         for (int j = 0; j < bytesPerRow; j++) {
            compData[j] = flipTable[compData[j] & 255];
         }

         this.stream.write(compData, 0, bytesPerRow);
         off += scanlineStride;
      }

      return height * bytesPerRow;
   }
}
