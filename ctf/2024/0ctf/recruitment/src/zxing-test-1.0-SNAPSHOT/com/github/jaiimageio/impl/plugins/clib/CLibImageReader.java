package com.github.jaiimageio.impl.plugins.clib;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

public abstract class CLibImageReader extends ImageReader {
   private int currIndex = -1;
   private long highWaterMark = Long.MIN_VALUE;
   private ArrayList imageStartPosition = new ArrayList();
   private int numImages = -1;
   private int mlibImageIndex = -1;

   private static boolean subBandsMatch(int[] sourceBands, int[] destinationBands) {
      if (sourceBands == null && destinationBands == null) {
         return true;
      } else if (sourceBands != null && destinationBands != null) {
         if (sourceBands.length != destinationBands.length) {
            return false;
         } else {
            for (int i = 0; i < sourceBands.length; i++) {
               if (sourceBands[i] != destinationBands[i]) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static final void subsample(Raster src, int subX, int subY, WritableRaster dst) {
      int sx0 = src.getMinX();
      int sy0 = src.getMinY();
      int sw = src.getWidth();
      int syUB = sy0 + src.getHeight();
      int dx0 = dst.getMinX();
      int dy0 = dst.getMinY();
      int dw = dst.getWidth();
      int b = src.getSampleModel().getNumBands();
      int t = src.getSampleModel().getDataType();
      int numSubSamples = (sw + subX - 1) / subX;
      if (t != 4 && t != 5) {
         int[] samples = new int[sw];
         int[] subsamples = new int[numSubSamples];

         for (int k = 0; k < b; k++) {
            int sy = sy0;

            for (int dy = dy0; sy < syUB; dy++) {
               src.getSamples(sx0, sy, sw, 1, k, samples);
               int i = 0;

               for (int s = 0; i < sw; i += subX) {
                  subsamples[s] = samples[i];
                  s++;
               }

               dst.setSamples(dx0, dy, dw, 1, k, subsamples);
               sy += subY;
            }
         }
      } else {
         float[] fsamples = new float[sw];
         float[] fsubsamples = new float[numSubSamples];

         for (int k = 0; k < b; k++) {
            int sy = sy0;

            for (int dy = dy0; sy < syUB; dy++) {
               src.getSamples(sx0, sy, sw, 1, k, fsamples);
               int i = 0;

               for (int s = 0; i < sw; i += subX) {
                  fsubsamples[s] = fsamples[i];
                  s++;
               }

               dst.setSamples(dx0, dy, dw, 1, k, fsubsamples);
               sy += subY;
            }
         }
      }
   }

   protected CLibImageReader(ImageReaderSpi originatingProvider) {
      super(originatingProvider);
   }

   protected int getImageIndex() {
      return this.mlibImageIndex;
   }

   @Override
   public IIOMetadata getStreamMetadata() throws IOException {
      return null;
   }

   private class SoloIterator implements Iterator {
      Object theObject;

      SoloIterator(Object o) {
         if (o == null) {
            new IllegalArgumentException(I18N.getString("CLibImageReader0"));
         }

         this.theObject = o;
      }

      @Override
      public boolean hasNext() {
         return this.theObject != null;
      }

      @Override
      public Object next() {
         if (this.theObject == null) {
            throw new NoSuchElementException();
         } else {
            Object theNextObject = this.theObject;
            this.theObject = null;
            return theNextObject;
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
