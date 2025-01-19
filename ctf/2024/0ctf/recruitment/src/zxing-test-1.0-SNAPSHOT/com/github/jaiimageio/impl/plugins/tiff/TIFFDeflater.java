package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFCompressor;
import java.io.IOException;
import java.util.zip.Deflater;
import javax.imageio.ImageWriteParam;

public class TIFFDeflater extends TIFFCompressor {
   Deflater deflater;
   int predictor;

   public TIFFDeflater(String compressionType, int compressionTagValue, ImageWriteParam param, int predictorValue) {
      super(compressionType, compressionTagValue, true);
      this.predictor = predictorValue;
      int deflateLevel;
      if (param != null && param.getCompressionMode() == 2) {
         float quality = param.getCompressionQuality();
         deflateLevel = (int)(1.0F + 8.0F * quality);
      } else {
         deflateLevel = -1;
      }

      this.deflater = new Deflater(deflateLevel);
   }

   @Override
   public int encode(byte[] b, int off, int width, int height, int[] bitsPerSample, int scanlineStride) throws IOException {
      int inputSize = height * scanlineStride;
      int blocks = (inputSize + 32767) / 32768;
      byte[] compData = new byte[inputSize + 5 * blocks + 6];
      int numCompressedBytes = 0;
      if (this.predictor == 2) {
         int samplesPerPixel = bitsPerSample.length;
         int bitsPerPixel = 0;

         for (int i = 0; i < samplesPerPixel; i++) {
            bitsPerPixel += bitsPerSample[i];
         }

         int bytesPerRow = (bitsPerPixel * width + 7) / 8;
         byte[] rowBuf = new byte[bytesPerRow];
         int maxRow = height - 1;

         for (int i = 0; i < height; i++) {
            System.arraycopy(b, off, rowBuf, 0, bytesPerRow);

            for (int j = bytesPerRow - 1; j >= samplesPerPixel; j--) {
               rowBuf[j] -= rowBuf[j - samplesPerPixel];
            }

            this.deflater.setInput(rowBuf);
            if (i == maxRow) {
               this.deflater.finish();
            }

            int numBytes = 0;

            while ((numBytes = this.deflater.deflate(compData, numCompressedBytes, compData.length - numCompressedBytes)) != 0) {
               numCompressedBytes += numBytes;
            }

            off += scanlineStride;
         }
      } else {
         this.deflater.setInput(b, off, height * scanlineStride);
         this.deflater.finish();
         numCompressedBytes = this.deflater.deflate(compData);
      }

      this.deflater.reset();
      this.stream.write(compData, 0, numCompressedBytes);
      return numCompressedBytes;
   }

   @Override
   public void dispose() {
      if (this.deflater != null) {
         this.deflater.end();
         this.deflater = null;
      }

      super.dispose();
   }
}
