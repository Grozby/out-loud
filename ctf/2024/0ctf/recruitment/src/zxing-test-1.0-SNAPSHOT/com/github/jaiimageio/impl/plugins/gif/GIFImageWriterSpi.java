package com.github.jaiimageio.impl.plugins.gif;

import com.github.jaiimageio.impl.common.ImageUtil;
import com.github.jaiimageio.impl.common.PackageUtil;
import com.github.jaiimageio.impl.common.PaletteBuilder;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class GIFImageWriterSpi extends ImageWriterSpi {
   private static final String vendorName = "Sun Microsystems, Inc.";
   private static final String version = "1.0";
   private static final String[] names = new String[]{"gif", "GIF"};
   private static final String[] suffixes = new String[]{"gif"};
   private static final String[] MIMETypes = new String[]{"image/gif"};
   private static final String writerClassName = "com.github.jaiimageio.impl.plugins.gif.GIFImageWriter";
   private static final String[] readerSpiNames = new String[]{"com.sun.imageio.plugins.gif.GIFImageReaderSpi"};
   private boolean registered = false;

   public GIFImageWriterSpi() {
      super(
         "Sun Microsystems, Inc.",
         "1.0",
         names,
         suffixes,
         MIMETypes,
         "com.github.jaiimageio.impl.plugins.gif.GIFImageWriter",
         STANDARD_OUTPUT_TYPE,
         readerSpiNames,
         true,
         "javax_imageio_gif_stream_1.0",
         "com.github.jaiimageio.impl.plugins.gif.GIFStreamMetadataFormat",
         null,
         null,
         true,
         "javax_imageio_gif_image_1.0",
         "com.github.jaiimageio.impl.plugins.gif.GIFStreamMetadataFormat",
         null,
         null
      );
   }

   @Override
   public boolean canEncodeImage(ImageTypeSpecifier type) {
      if (type == null) {
         throw new IllegalArgumentException("type == null!");
      } else {
         SampleModel sm = type.getSampleModel();
         ColorModel cm = type.getColorModel();
         boolean canEncode = sm.getNumBands() == 1
            && sm.getSampleSize(0) <= 8
            && sm.getWidth() <= 65535
            && sm.getHeight() <= 65535
            && (cm == null || cm.getComponentSize()[0] <= 8);
         return canEncode ? true : PaletteBuilder.canCreatePalette(type);
      }
   }

   @Override
   public String getDescription(Locale locale) {
      return PackageUtil.getSpecificationTitle() + " GIF Image Writer";
   }

   @Override
   public void onRegistration(ServiceRegistry registry, Class category) {
      if (!this.registered) {
         this.registered = true;
         ImageUtil.processOnRegistration(registry, category, "GIF", this, 9, 8);
      }
   }

   @Override
   public ImageWriter createWriterInstance(Object extension) {
      return new GIFImageWriter(this);
   }
}
