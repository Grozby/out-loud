package com.github.jaiimageio.impl.plugins.raw;

import com.github.jaiimageio.impl.common.PackageUtil;
import com.github.jaiimageio.stream.RawImageInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

public class RawImageReaderSpi extends ImageReaderSpi {
   private static String[] writerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.raw.RawImageWriterSpi"};
   private static String[] formatNames = new String[]{"raw", "RAW"};
   private static String[] entensions = new String[]{"raw"};
   private static String[] mimeType = new String[]{"image/x-raw"};
   private boolean registered = false;

   public RawImageReaderSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         entensions,
         mimeType,
         "com.github.jaiimageio.impl.plugins.raw.RawImageReader",
         STANDARD_INPUT_TYPE,
         writerSpiNames,
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
   public void onRegistration(ServiceRegistry registry, Class category) {
      if (!this.registered) {
         this.registered = true;
      }
   }

   @Override
   public String getDescription(Locale locale) {
      return PackageUtil.getSpecificationTitle() + " Raw Image Reader";
   }

   @Override
   public boolean canDecodeInput(Object source) throws IOException {
      return source instanceof RawImageInputStream;
   }

   @Override
   public ImageReader createReaderInstance(Object extension) throws IIOException {
      return new RawImageReader(this);
   }
}
