package com.github.jaiimageio.impl.plugins.wbmp;

import com.github.jaiimageio.impl.common.ImageUtil;
import com.github.jaiimageio.impl.common.PackageUtil;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SampleModel;
import java.util.Locale;
import javax.imageio.IIOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class WBMPImageWriterSpi extends ImageWriterSpi {
   private static String[] readerSpiNames = new String[]{"com.github.jaiimageio.impl.plugins.wbmp.WBMPImageReaderSpi"};
   private static String[] formatNames = new String[]{"wbmp", "WBMP"};
   private static String[] entensions = new String[]{"wbmp"};
   private static String[] mimeType = new String[]{"image/vnd.wap.wbmp"};
   private boolean registered = false;

   public WBMPImageWriterSpi() {
      super(
         PackageUtil.getVendor(),
         PackageUtil.getVersion(),
         formatNames,
         entensions,
         mimeType,
         "com.github.jaiimageio.impl.plugins.wbmp.WBMPImageWriter",
         STANDARD_OUTPUT_TYPE,
         readerSpiNames,
         true,
         null,
         null,
         null,
         null,
         true,
         "com_sun_media_imageio_plugins_wbmp_image_1.0",
         "com.github.jaiimageio.impl.plugins.wbmp.WBMPMetadataFormat",
         null,
         null
      );
   }

   @Override
   public String getDescription(Locale locale) {
      return PackageUtil.getSpecificationTitle() + " WBMP Image Writer";
   }

   @Override
   public void onRegistration(ServiceRegistry registry, Class category) {
      if (!this.registered) {
         this.registered = true;
         ImageUtil.processOnRegistration(registry, category, "WBMP", this, 8, 7);
      }
   }

   @Override
   public boolean canEncodeImage(ImageTypeSpecifier type) {
      SampleModel sm = type.getSampleModel();
      return !(sm instanceof MultiPixelPackedSampleModel) ? false : sm.getSampleSize(0) == 1;
   }

   @Override
   public ImageWriter createWriterInstance(Object extension) throws IIOException {
      return new WBMPImageWriter(this);
   }
}
