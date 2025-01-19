package com.github.jaiimageio.impl.plugins.pnm;

import com.github.jaiimageio.impl.common.PackageUtil;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

public class PNMImageReaderSpi extends ImageReaderSpi {
   private static String[] writerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.pnm.PNMImageWriterSpi"};
   private static String[] formatNames = new String[]{"pnm", "PNM"};
   private static String[] entensions = new String[]{"pbm", "pgm", "ppm"};
   private static String[] mimeType = new String[]{"image/x-portable-anymap", "image/x-portable-bitmap", "image/x-portable-graymap", "image/x-portable-pixmap"};
   private boolean registered = false;

   public PNMImageReaderSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         entensions,
         mimeType,
         "com.github.jaiimageio.impl.plugins.pnm.PNMImageReader",
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
      return PackageUtil.getSpecificationTitle() + " PNM Image Reader";
   }

   @Override
   public boolean canDecodeInput(Object source) throws IOException {
      if (!(source instanceof ImageInputStream)) {
         return false;
      } else {
         ImageInputStream stream = (ImageInputStream)source;
         byte[] b = new byte[2];
         stream.mark();
         stream.readFully(b);
         stream.reset();
         return b[0] == 80 && b[1] >= 49 && b[1] <= 54;
      }
   }

   @Override
   public ImageReader createReaderInstance(Object extension) throws IIOException {
      return new PNMImageReader(this);
   }
}
