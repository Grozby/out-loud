package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFImageReadParam;
import com.github.jaiimageio.plugins.tiff.TIFFTagSet;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;

public class TIFFRenderedImage implements RenderedImage {
   TIFFImageReader reader;
   int imageIndex;
   ImageReadParam tileParam;
   int subsampleX;
   int subsampleY;
   boolean isSubsampling;
   int width;
   int height;
   int tileWidth;
   int tileHeight;
   ImageTypeSpecifier its;

   public TIFFRenderedImage(TIFFImageReader reader, int imageIndex, ImageReadParam readParam, int width, int height) throws IOException {
      this.reader = reader;
      this.imageIndex = imageIndex;
      this.tileParam = this.cloneImageReadParam(readParam, false);
      this.subsampleX = this.tileParam.getSourceXSubsampling();
      this.subsampleY = this.tileParam.getSourceYSubsampling();
      this.isSubsampling = this.subsampleX != 1 || this.subsampleY != 1;
      this.width = width / this.subsampleX;
      this.height = height / this.subsampleY;
      this.tileWidth = reader.getTileWidth(imageIndex) / this.subsampleX;
      this.tileHeight = reader.getTileHeight(imageIndex) / this.subsampleY;
      Iterator iter = reader.getImageTypes(imageIndex);
      this.its = (ImageTypeSpecifier)iter.next();
      this.tileParam.setDestinationType(this.its);
   }

   private ImageReadParam cloneImageReadParam(ImageReadParam param, boolean copyTagSets) {
      TIFFImageReadParam newParam = new TIFFImageReadParam();
      newParam.setSourceSubsampling(param.getSourceXSubsampling(), param.getSourceYSubsampling(), param.getSubsamplingXOffset(), param.getSubsamplingYOffset());
      newParam.setSourceBands(param.getSourceBands());
      newParam.setDestinationBands(param.getDestinationBands());
      newParam.setDestinationOffset(param.getDestinationOffset());
      if (param instanceof TIFFImageReadParam) {
         TIFFImageReadParam tparam = (TIFFImageReadParam)param;
         newParam.setTIFFDecompressor(tparam.getTIFFDecompressor());
         newParam.setColorConverter(tparam.getColorConverter());
         if (copyTagSets) {
            List tagSets = tparam.getAllowedTagSets();
            if (tagSets != null) {
               Iterator tagSetIter = tagSets.iterator();
               if (tagSetIter != null) {
                  while (tagSetIter.hasNext()) {
                     TIFFTagSet tagSet = (TIFFTagSet)tagSetIter.next();
                     newParam.addAllowedTagSet(tagSet);
                  }
               }
            }
         }
      } else {
         newParam.setTIFFDecompressor(null);
         newParam.setColorConverter(null);
      }

      return newParam;
   }

   @Override
   public Vector getSources() {
      return null;
   }

   @Override
   public Object getProperty(String name) {
      return Image.UndefinedProperty;
   }

   @Override
   public String[] getPropertyNames() {
      return null;
   }

   @Override
   public ColorModel getColorModel() {
      return this.its.getColorModel();
   }

   @Override
   public SampleModel getSampleModel() {
      return this.its.getSampleModel();
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public int getMinX() {
      return 0;
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getNumXTiles() {
      return (this.width + this.tileWidth - 1) / this.tileWidth;
   }

   @Override
   public int getNumYTiles() {
      return (this.height + this.tileHeight - 1) / this.tileHeight;
   }

   @Override
   public int getMinTileX() {
      return 0;
   }

   @Override
   public int getMinTileY() {
      return 0;
   }

   @Override
   public int getTileWidth() {
      return this.tileWidth;
   }

   @Override
   public int getTileHeight() {
      return this.tileHeight;
   }

   @Override
   public int getTileGridXOffset() {
      return 0;
   }

   @Override
   public int getTileGridYOffset() {
      return 0;
   }

   @Override
   public Raster getTile(int tileX, int tileY) {
      Rectangle tileRect = new Rectangle(tileX * this.tileWidth, tileY * this.tileHeight, this.tileWidth, this.tileHeight);
      return this.getData(tileRect);
   }

   @Override
   public Raster getData() {
      return this.read(new Rectangle(0, 0, this.getWidth(), this.getHeight()));
   }

   @Override
   public Raster getData(Rectangle rect) {
      return this.read(rect);
   }

   public synchronized WritableRaster read(Rectangle rect) {
      this.tileParam
         .setSourceRegion(
            this.isSubsampling
               ? new Rectangle(this.subsampleX * rect.x, this.subsampleY * rect.y, this.subsampleX * rect.width, this.subsampleY * rect.height)
               : rect
         );

      try {
         BufferedImage bi = this.reader.read(this.imageIndex, this.tileParam);
         WritableRaster ras = bi.getRaster();
         return ras.createWritableChild(0, 0, ras.getWidth(), ras.getHeight(), rect.x, rect.y, null);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   public WritableRaster copyData(WritableRaster raster) {
      if (raster == null) {
         return this.read(new Rectangle(0, 0, this.getWidth(), this.getHeight()));
      } else {
         Raster src = this.read(raster.getBounds());
         raster.setRect(src);
         return raster;
      }
   }
}
