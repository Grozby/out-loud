package com.github.jaiimageio.impl.plugins.pnm;

import com.github.jaiimageio.impl.common.PackageUtil;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class PNMImageWriterSpi extends ImageWriterSpi {
   private static String[] readerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.pnm.PNMImageReaderSpi"};
   private static String[] formatNames = new String[]{"pnm", "PNM"};
   private static String[] entensions = new String[]{"pbm", "pgm", "ppm"};
   private static String[] mimeType = new String[]{"image/x-portable-anymap", "image/x-portable-bitmap", "image/x-portable-graymap", "image/x-portable-pixmap"};
   private boolean registered = false;

   public PNMImageWriterSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         entensions,
         mimeType,
         "com.github.jaiimageio.impl.plugins.pnm.PNMImageWriter",
         STANDARD_OUTPUT_TYPE,
         readerSpiNames,
         true,
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
      return PackageUtil.getSpecificationTitle() + " PNM Image Writer";
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
         int numBands = type.getSampleModel().getNumBands();
         return numBands == 1 || numBands == 3;
      } else {
         return false;
      }
   }

   @Override
   public ImageWriter createWriterInstance(Object extension) throws IIOException {
      return new PNMImageWriter(this);
   }
}
