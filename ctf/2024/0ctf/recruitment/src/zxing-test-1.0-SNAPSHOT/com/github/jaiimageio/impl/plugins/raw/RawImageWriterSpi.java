package com.github.jaiimageio.impl.plugins.raw;

import com.github.jaiimageio.impl.common.PackageUtil;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class RawImageWriterSpi extends ImageWriterSpi {
   private static String[] readerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.raw.RawImageReaderSpi"};
   private static String[] formatNames = new String[]{"raw", "RAW"};
   private static String[] entensions = new String[]{"raw"};
   private static String[] mimeType = new String[]{"image/x-raw"};
   private boolean registered = false;

   public RawImageWriterSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         entensions,
         mimeType,
         "com.github.jaiimageio.impl.plugins.raw.RawImageWriter",
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
      return PackageUtil.getSpecificationTitle() + " Raw Image Writer";
   }

   @Override
   public void onRegistration(ServiceRegistry registry, Class category) {
      if (!this.registered) {
         this.registered = true;
      }
   }

   @Override
   public boolean canEncodeImage(ImageTypeSpecifier type) {
      return true;
   }

   @Override
   public ImageWriter createWriterInstance(Object extension) throws IIOException {
      return new RawImageWriter(this);
   }
}
