package com.github.jaiimageio.impl.plugins.raw;

import com.github.jaiimageio.stream.RawImageInputStream;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

public class RawImageReader extends ImageReader {
   private RawImageInputStream iis = null;

   public static void computeRegionsWrapper(ImageReadParam param, int srcWidth, int srcHeight, BufferedImage image, Rectangle srcRegion, Rectangle destRegion) {
      computeRegions(param, srcWidth, srcHeight, image, srcRegion, destRegion);
   }

   public RawImageReader(ImageReaderSpi originator) {
      super(originator);
   }

   @Override
   public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
      super.setInput(input, seekForwardOnly, ignoreMetadata);
      this.iis = (RawImageInputStream)input;
   }

   @Override
   public int getNumImages(boolean allowSearch) throws IOException {
      return this.iis.getNumImages();
   }

   @Override
   public int getWidth(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      return this.iis.getImageDimension(imageIndex).width;
   }

   @Override
   public int getHeight(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      return this.iis.getImageDimension(imageIndex).height;
   }

   @Override
   public int getTileWidth(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      return this.iis.getImageType().getSampleModel().getWidth();
   }

   @Override
   public int getTileHeight(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      return this.iis.getImageType().getSampleModel().getHeight();
   }

   private void checkIndex(int imageIndex) throws IOException {
      if (imageIndex < 0 || imageIndex >= this.getNumImages(true)) {
         throw new IndexOutOfBoundsException(I18N.getString("RawImageReader0"));
      }
   }

   @Override
   public Iterator getImageTypes(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      ArrayList list = new ArrayList(1);
      list.add(this.iis.getImageType());
      return list.iterator();
   }

   @Override
   public ImageReadParam getDefaultReadParam() {
      return new ImageReadParam();
   }

   @Override
   public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
      return null;
   }

   @Override
   public IIOMetadata getStreamMetadata() throws IOException {
      return null;
   }

   @Override
   public boolean isRandomAccessEasy(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      return true;
   }

   @Override
   public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
      if (param == null) {
         param = this.getDefaultReadParam();
      }

      this.checkIndex(imageIndex);
      this.clearAbortRequest();
      this.processImageStarted(imageIndex);
      BufferedImage bi = param.getDestination();
      RawRenderedImage image = new RawRenderedImage(this.iis, this, param, imageIndex);
      Point offset = param.getDestinationOffset();
      WritableRaster raster;
      if (bi == null) {
         ColorModel colorModel = image.getColorModel();
         SampleModel sampleModel = image.getSampleModel();
         ImageTypeSpecifier type = param.getDestinationType();
         if (type != null) {
            colorModel = type.getColorModel();
         }

         raster = Raster.createWritableRaster(
            sampleModel.createCompatibleSampleModel(image.getMinX() + image.getWidth(), image.getMinY() + image.getHeight()), new Point(0, 0)
         );
         bi = new BufferedImage(colorModel, raster, colorModel != null ? colorModel.isAlphaPremultiplied() : false, new Hashtable());
      } else {
         raster = bi.getWritableTile(0, 0);
      }

      image.setDestImage(bi);
      image.readAsRaster(raster);
      image.clearDestImage();
      if (this.abortRequested()) {
         this.processReadAborted();
      } else {
         this.processImageComplete();
      }

      return bi;
   }

   @Override
   public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param) throws IOException {
      if (param == null) {
         param = this.getDefaultReadParam();
      }

      this.checkIndex(imageIndex);
      this.clearAbortRequest();
      this.processImageStarted(0);
      RenderedImage image = new RawRenderedImage(this.iis, this, param, imageIndex);
      if (this.abortRequested()) {
         this.processReadAborted();
      } else {
         this.processImageComplete();
      }

      return image;
   }

   @Override
   public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
      BufferedImage bi = this.read(imageIndex, param);
      return bi.getData();
   }

   @Override
   public boolean canReadRaster() {
      return true;
   }

   @Override
   public void reset() {
      super.reset();
      this.iis = null;
   }

   public void processImageUpdateWrapper(BufferedImage theImage, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
      this.processImageUpdate(theImage, minX, minY, width, height, periodX, periodY, bands);
   }

   public void processImageProgressWrapper(float percentageDone) {
      this.processImageProgress(percentageDone);
   }

   public boolean getAbortRequest() {
      return this.abortRequested();
   }
}
