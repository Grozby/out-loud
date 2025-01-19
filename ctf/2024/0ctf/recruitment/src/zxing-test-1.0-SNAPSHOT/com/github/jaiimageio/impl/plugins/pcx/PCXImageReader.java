package com.github.jaiimageio.impl.plugins.pcx;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

public class PCXImageReader extends ImageReader implements PCXConstants {
   private ImageInputStream iis;
   private int width;
   private int height;
   private boolean gotHeader = false;
   private byte manufacturer;
   private byte encoding;
   private short xmax;
   private short ymax;
   private byte[] smallPalette = new byte[48];
   private byte[] largePalette = new byte[768];
   private byte colorPlanes;
   private short bytesPerLine;
   private short paletteType;
   private PCXMetadata metadata;
   private SampleModel sampleModel;
   private SampleModel originalSampleModel;
   private ColorModel colorModel;
   private ColorModel originalColorModel;
   private Rectangle destinationRegion;
   private Rectangle sourceRegion;
   private BufferedImage bi;
   private boolean noTransform = true;
   private boolean seleBand = false;
   private int scaleX;
   private int scaleY;
   private int[] sourceBands;
   private int[] destBands;

   public PCXImageReader(PCXImageReaderSpi imageReaderSpi) {
      super(imageReaderSpi);
   }

   @Override
   public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
      super.setInput(input, seekForwardOnly, ignoreMetadata);
      this.iis = (ImageInputStream)input;
      if (this.iis != null) {
         this.iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
      }

      this.gotHeader = false;
   }

   @Override
   public int getHeight(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      this.readHeader();
      return this.height;
   }

   @Override
   public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      this.readHeader();
      return this.metadata;
   }

   @Override
   public Iterator getImageTypes(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      this.readHeader();
      return Collections.singletonList(new ImageTypeSpecifier(this.originalColorModel, this.originalSampleModel)).iterator();
   }

   @Override
   public int getNumImages(boolean allowSearch) throws IOException {
      if (this.iis == null) {
         throw new IllegalStateException("input is null");
      } else if (this.seekForwardOnly && allowSearch) {
         throw new IllegalStateException("cannot search with forward only input");
      } else {
         return 1;
      }
   }

   @Override
   public IIOMetadata getStreamMetadata() throws IOException {
      return null;
   }

   @Override
   public int getWidth(int imageIndex) throws IOException {
      this.checkIndex(imageIndex);
      this.readHeader();
      return this.width;
   }

   @Override
   public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
      this.checkIndex(imageIndex);
      this.readHeader();
      if (this.iis == null) {
         throw new IllegalStateException("input is null");
      } else {
         this.clearAbortRequest();
         this.processImageStarted(imageIndex);
         if (param == null) {
            param = this.getDefaultReadParam();
         }

         this.sourceRegion = new Rectangle(0, 0, 0, 0);
         this.destinationRegion = new Rectangle(0, 0, 0, 0);
         computeRegions(param, this.width, this.height, param.getDestination(), this.sourceRegion, this.destinationRegion);
         this.scaleX = param.getSourceXSubsampling();
         this.scaleY = param.getSourceYSubsampling();
         this.sourceBands = param.getSourceBands();
         this.destBands = param.getDestinationBands();
         this.seleBand = this.sourceBands != null && this.destBands != null;
         this.noTransform = this.destinationRegion.equals(new Rectangle(0, 0, this.width, this.height)) || this.seleBand;
         if (!this.seleBand) {
            this.sourceBands = new int[this.colorPlanes];
            this.destBands = new int[this.colorPlanes];

            for (int i = 0; i < this.colorPlanes; i++) {
               this.destBands[i] = this.sourceBands[i] = i;
            }
         }

         this.bi = param.getDestination();
         WritableRaster raster = null;
         if (this.bi == null) {
            if (this.sampleModel != null && this.colorModel != null) {
               this.sampleModel = this.sampleModel
                  .createCompatibleSampleModel(
                     this.destinationRegion.width + this.destinationRegion.x, this.destinationRegion.height + this.destinationRegion.y
                  );
               if (this.seleBand) {
                  this.sampleModel = this.sampleModel.createSubsetSampleModel(this.sourceBands);
               }

               raster = Raster.createWritableRaster(this.sampleModel, new Point(0, 0));
               this.bi = new BufferedImage(this.colorModel, raster, false, null);
            }
         } else {
            raster = this.bi.getWritableTile(0, 0);
            this.sampleModel = this.bi.getSampleModel();
            this.colorModel = this.bi.getColorModel();
            this.noTransform = this.noTransform & this.destinationRegion.equals(raster.getBounds());
         }

         byte[] bdata = null;
         if (this.sampleModel.getDataType() == 0) {
            bdata = ((DataBufferByte)raster.getDataBuffer()).getData();
         }

         this.readImage(bdata);
         if (this.abortRequested()) {
            this.processReadAborted();
         } else {
            this.processImageComplete();
         }

         return this.bi;
      }
   }

   private void readImage(byte[] data) throws IOException {
      byte[] scanline = new byte[this.bytesPerLine * this.colorPlanes];
      if (this.noTransform) {
         try {
            int offset = 0;
            int nbytes = (this.width * this.metadata.bitsPerPixel + 8 - this.metadata.bitsPerPixel) / 8;

            for (int line = 0; line < this.height; line++) {
               this.readScanLine(scanline);

               for (int band = 0; band < this.colorPlanes; band++) {
                  System.arraycopy(scanline, this.bytesPerLine * band, data, offset, nbytes);
                  offset += nbytes;
               }

               this.processImageProgress(100.0F * (float)line / (float)this.height);
            }
         } catch (EOFException var7) {
         }
      } else if (this.metadata.bitsPerPixel == 1) {
         this.read1Bit(data);
      } else if (this.metadata.bitsPerPixel == 4) {
         this.read4Bit(data);
      } else {
         this.read8Bit(data);
      }
   }

   private void read1Bit(byte[] data) throws IOException {
      byte[] scanline = new byte[this.bytesPerLine];

      try {
         for (int line = 0; line < this.sourceRegion.y; line++) {
            this.readScanLine(scanline);
         }

         int lineStride = ((MultiPixelPackedSampleModel)this.sampleModel).getScanlineStride();
         int[] srcOff = new int[this.destinationRegion.width];
         int[] destOff = new int[this.destinationRegion.width];
         int[] srcPos = new int[this.destinationRegion.width];
         int[] destPos = new int[this.destinationRegion.width];
         int i = this.destinationRegion.x;
         int x = this.sourceRegion.x;

         for (int j = 0; i < this.destinationRegion.x + this.destinationRegion.width; x += this.scaleX) {
            srcPos[j] = x >> 3;
            srcOff[j] = 7 - (x & 7);
            destPos[j] = i >> 3;
            destOff[j] = 7 - (i & 7);
            i++;
            j++;
         }

         i = this.destinationRegion.y * lineStride;

         for (int line = 0; line < this.sourceRegion.height; line++) {
            this.readScanLine(scanline);
            if (line % this.scaleY == 0) {
               for (int ix = 0; ix < this.destinationRegion.width; ix++) {
                  int v = scanline[srcPos[ix]] >> srcOff[ix] & 1;
                  data[i + destPos[ix]] = (byte)(data[i + destPos[ix]] | v << destOff[ix]);
               }

               i += lineStride;
            }

            this.processImageProgress(100.0F * (float)line / (float)this.sourceRegion.height);
         }
      } catch (EOFException var12) {
      }
   }

   private void read4Bit(byte[] data) throws IOException {
      byte[] scanline = new byte[this.bytesPerLine];

      try {
         int lineStride = ((MultiPixelPackedSampleModel)this.sampleModel).getScanlineStride();
         int[] srcOff = new int[this.destinationRegion.width];
         int[] destOff = new int[this.destinationRegion.width];
         int[] srcPos = new int[this.destinationRegion.width];
         int[] destPos = new int[this.destinationRegion.width];
         int i = this.destinationRegion.x;
         int x = this.sourceRegion.x;

         for (int j = 0; i < this.destinationRegion.x + this.destinationRegion.width; x += this.scaleX) {
            srcPos[j] = x >> 1;
            srcOff[j] = 1 - (x & 1) << 2;
            destPos[j] = i >> 1;
            destOff[j] = 1 - (i & 1) << 2;
            i++;
            j++;
         }

         i = this.destinationRegion.y * lineStride;

         for (int line = 0; line < this.sourceRegion.height; line++) {
            this.readScanLine(scanline);
            if (this.abortRequested()) {
               break;
            }

            if (line % this.scaleY == 0) {
               for (int ix = 0; ix < this.destinationRegion.width; ix++) {
                  int v = scanline[srcPos[ix]] >> srcOff[ix] & 15;
                  data[i + destPos[ix]] = (byte)(data[i + destPos[ix]] | v << destOff[ix]);
               }

               i += lineStride;
            }

            this.processImageProgress(100.0F * (float)line / (float)this.sourceRegion.height);
         }
      } catch (EOFException var12) {
      }
   }

   private void read8Bit(byte[] data) throws IOException {
      byte[] scanline = new byte[this.colorPlanes * this.bytesPerLine];

      try {
         for (int line = 0; line < this.sourceRegion.y; line++) {
            this.readScanLine(scanline);
         }

         int dstOffset = this.destinationRegion.y * (this.destinationRegion.x + this.destinationRegion.width) * this.colorPlanes;

         for (int line = 0; line < this.sourceRegion.height; line++) {
            this.readScanLine(scanline);
            if (line % this.scaleY == 0) {
               int srcOffset = this.sourceRegion.x;

               for (int band = 0; band < this.colorPlanes; band++) {
                  dstOffset += this.destinationRegion.x;

                  for (int x = 0; x < this.destinationRegion.width; x += this.scaleX) {
                     data[dstOffset++] = scanline[srcOffset + x];
                  }

                  srcOffset += this.bytesPerLine;
               }
            }

            this.processImageProgress(100.0F * (float)line / (float)this.sourceRegion.height);
         }
      } catch (EOFException var8) {
      }
   }

   private void readScanLine(byte[] buffer) throws IOException {
      int max = this.bytesPerLine * this.colorPlanes;
      int j = 0;

      while (j < max) {
         int val = this.iis.readUnsignedByte();
         if ((val & 192) == 192) {
            int count = val & -193;
            val = this.iis.readUnsignedByte();

            for (int k = 0; k < count && j < max; k++) {
               buffer[j++] = (byte)(val & 0xFF);
            }
         } else {
            buffer[j++] = (byte)(val & 0xFF);
         }
      }
   }

   private void checkIndex(int imageIndex) {
      if (imageIndex != 0) {
         throw new IndexOutOfBoundsException("only one image exists in the stream");
      }
   }

   private void readHeader() throws IOException {
      if (this.gotHeader) {
         this.iis.seek(128L);
      } else {
         this.metadata = new PCXMetadata();
         this.manufacturer = this.iis.readByte();
         if (this.manufacturer != 10) {
            throw new IllegalStateException("image is not a PCX file");
         } else {
            this.metadata.version = (short)this.iis.readByte();
            this.encoding = this.iis.readByte();
            if (this.encoding != 1) {
               throw new IllegalStateException("image is not a PCX file, invalid encoding " + this.encoding);
            } else {
               this.metadata.bitsPerPixel = this.iis.readByte();
               this.metadata.xmin = this.iis.readShort();
               this.metadata.ymin = this.iis.readShort();
               this.xmax = this.iis.readShort();
               this.ymax = this.iis.readShort();
               this.metadata.hdpi = this.iis.readShort();
               this.metadata.vdpi = this.iis.readShort();
               this.iis.readFully(this.smallPalette);
               this.iis.readByte();
               this.colorPlanes = this.iis.readByte();
               this.bytesPerLine = this.iis.readShort();
               this.paletteType = this.iis.readShort();
               this.metadata.hsize = this.iis.readShort();
               this.metadata.vsize = this.iis.readShort();
               this.iis.skipBytes(54);
               this.width = this.xmax - this.metadata.xmin + 1;
               this.height = this.ymax - this.metadata.ymin + 1;
               if (this.colorPlanes == 1) {
                  if (this.paletteType == 2) {
                     ColorSpace cs = ColorSpace.getInstance(1003);
                     int[] nBits = new int[]{8};
                     this.colorModel = new ComponentColorModel(cs, nBits, false, false, 1, 0);
                     this.sampleModel = new ComponentSampleModel(0, this.width, this.height, 1, this.width, new int[]{0});
                  } else if (this.metadata.bitsPerPixel == 8) {
                     this.iis.mark();
                     if (this.iis.length() != -1L) {
                        this.iis.seek(this.iis.length() - 768L - 1L);
                     } else {
                        while (this.iis.read() != -1) {
                        }

                        this.iis.seek(this.iis.getStreamPosition() - 768L - 1L);
                     }

                     int palletteMagic = this.iis.read();
                     if (palletteMagic != 12) {
                        this.processWarningOccurred("Expected palette magic number 12; instead read " + palletteMagic + " from this image.");
                     }

                     this.iis.readFully(this.largePalette);
                     this.iis.reset();
                     this.colorModel = new IndexColorModel(this.metadata.bitsPerPixel, 256, this.largePalette, 0, false);
                     this.sampleModel = this.colorModel.createCompatibleSampleModel(this.width, this.height);
                  } else {
                     int msize = this.metadata.bitsPerPixel == 1 ? 2 : 16;
                     this.colorModel = new IndexColorModel(this.metadata.bitsPerPixel, msize, this.smallPalette, 0, false);
                     this.sampleModel = this.colorModel.createCompatibleSampleModel(this.width, this.height);
                  }
               } else {
                  ColorSpace cs = ColorSpace.getInstance(1000);
                  int[] nBits = new int[]{8, 8, 8};
                  this.colorModel = new ComponentColorModel(cs, nBits, false, false, 1, 0);
                  this.sampleModel = new ComponentSampleModel(
                     0, this.width, this.height, 1, this.width * this.colorPlanes, new int[]{0, this.width, this.width * 2}
                  );
               }

               this.originalSampleModel = this.sampleModel;
               this.originalColorModel = this.colorModel;
               this.gotHeader = true;
            }
         }
      }
   }
}
