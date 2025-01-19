package com.github.jaiimageio.impl.plugins.pcx;

import com.github.jaiimageio.impl.common.PackageUtil;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class PCXImageWriterSpi extends ImageWriterSpi {
   private static String[] readerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.pcx.PCXImageReaderSpi"};
   private static String[] formatNames = new String[]{"pcx", "PCX"};
   private static String[] extensions = new String[]{"pcx"};
   private static String[] mimeTypes = new String[]{"image/pcx", "image/x-pcx", "image/x-windows-pcx", "image/x-pc-paintbrush"};
   private boolean registered = false;

   public PCXImageWriterSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         extensions,
         mimeTypes,
         "com.github.jaiimageio.impl.plugins.pcx.PCXImageWriter",
         STANDARD_OUTPUT_TYPE,
         readerSpiNames,
         false,
         null,
         null,
         null,
         null,
         true,
         null,
         null,
         null,
         null
      );
   }

   @Override
   public String getDescription(Locale locale) {
      return PackageUtil.getSpecificationTitle() + " PCX Image Writer";
   }

   @Override
   public void onRegistration(ServiceRegistry registry, Class category) {
      if (!this.registered) {
         this.registered = true;
      }
   }

   @Override
   public boolean canEncodeImage(ImageTypeSpecifier type) {
      int dataType = type.getSampleModel().getDataType();
      if (dataType >= 0 && dataType <= 3) {
         SampleModel sm = type.getSampleModel();
         int numBands = sm.getNumBands();
         if (numBands != 1 && numBands != 3) {
            return false;
         } else {
            return numBands == 1 && dataType != 0 ? false : dataType <= 0 || sm instanceof SinglePixelPackedSampleModel;
         }
      } else {
         return false;
      }
   }

   @Override
   public ImageWriter createWriterInstance(Object extension) throws IIOException {
      return new PCXImageWriter(this);
   }
}
