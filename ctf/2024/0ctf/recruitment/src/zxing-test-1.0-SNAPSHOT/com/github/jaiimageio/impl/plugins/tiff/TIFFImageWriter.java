package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.impl.common.ImageUtil;
import com.github.jaiimageio.impl.common.SingleTileRenderedImage;
import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.EXIFTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFColorConverter;
import com.github.jaiimageio.plugins.tiff.TIFFCompressor;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFImageWriteParam;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.github.jaiimageio.plugins.tiff.TIFFTagSet;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

public class TIFFImageWriter extends ImageWriter {
   private static final boolean DEBUG = false;
   static final String EXIF_JPEG_COMPRESSION_TYPE = "EXIF JPEG";
   public static final int DEFAULT_BYTES_PER_STRIP = 8192;
   public static final String[] TIFFCompressionTypes = new String[]{
      "CCITT RLE", "CCITT T.4", "CCITT T.6", "LZW", "JPEG", "ZLib", "PackBits", "Deflate", "EXIF JPEG"
   };
   public static final String[] compressionTypes = new String[]{
      "CCITT RLE", "CCITT T.4", "CCITT T.6", "LZW", "Old JPEG", "JPEG", "ZLib", "PackBits", "Deflate", "EXIF JPEG"
   };
   public static final boolean[] isCompressionLossless = new boolean[]{true, true, true, true, false, false, true, true, true, false};
   public static final int[] compressionNumbers = new int[]{2, 3, 4, 5, 6, 7, 8, 32773, 32946, 6};
   ImageOutputStream stream;
   long headerPosition;
   RenderedImage image;
   ImageTypeSpecifier imageType;
   ByteOrder byteOrder;
   ImageWriteParam param;
   TIFFCompressor compressor;
   TIFFColorConverter colorConverter;
   TIFFStreamMetadata streamMetadata;
   TIFFImageMetadata imageMetadata;
   int sourceXOffset;
   int sourceYOffset;
   int sourceWidth;
   int sourceHeight;
   int[] sourceBands;
   int periodX;
   int periodY;
   int bitDepth;
   int numBands;
   int tileWidth;
   int tileLength;
   int tilesAcross;
   int tilesDown;
   int[] sampleSize = null;
   int scalingBitDepth = -1;
   boolean isRescaling = false;
   boolean isBilevel;
   boolean isImageSimple;
   boolean isInverted;
   boolean isTiled;
   int nativePhotometricInterpretation;
   int photometricInterpretation;
   char[] bitsPerSample;
   int sampleFormat = 4;
   byte[][] scale = (byte[][])null;
   byte[] scale0 = null;
   byte[][] scaleh = (byte[][])null;
   byte[][] scalel = (byte[][])null;
   int compression;
   int predictor;
   int totalPixels;
   int pixelsDone;
   long nextIFDPointerPos;
   long nextSpace = 0L;
   boolean isWritingSequence = false;
   private boolean isInsertingEmpty = false;
   private boolean isWritingEmpty = false;
   private Object replacePixelsLock = new Object();
   private int replacePixelsIndex = -1;
   private TIFFImageMetadata replacePixelsMetadata = null;
   private long[] replacePixelsTileOffsets = null;
   private long[] replacePixelsByteCounts = null;
   private long replacePixelsOffsetsPosition = 0L;
   private long replacePixelsByteCountsPosition = 0L;
   private Rectangle replacePixelsRegion = null;
   private boolean inReplacePixelsNest = false;
   private TIFFImageReader reader = null;

   public static int XToTileX(int x, int tileGridXOffset, int tileWidth) {
      x -= tileGridXOffset;
      if (x < 0) {
         x += 1 - tileWidth;
      }

      return x / tileWidth;
   }

   public static int YToTileY(int y, int tileGridYOffset, int tileHeight) {
      y -= tileGridYOffset;
      if (y < 0) {
         y += 1 - tileHeight;
      }

      return y / tileHeight;
   }

   public TIFFImageWriter(ImageWriterSpi originatingProvider) {
      super(originatingProvider);
   }

   @Override
   public ImageWriteParam getDefaultWriteParam() {
      return new TIFFImageWriteParam(this.getLocale());
   }

   @Override
   public void setOutput(Object output) {
      super.setOutput(output);
      if (output != null) {
         if (!(output instanceof ImageOutputStream)) {
            throw new IllegalArgumentException("output not an ImageOutputStream!");
         }

         this.stream = (ImageOutputStream)output;

         try {
            this.headerPosition = this.stream.getStreamPosition();

            try {
               byte[] b = new byte[4];
               this.stream.readFully(b);
               if ((b[0] != 73 || b[1] != 73 || b[2] != 42 || b[3] != 0) && (b[0] != 77 || b[1] != 77 || b[2] != 0 || b[3] != 42)) {
                  this.nextSpace = this.headerPosition;
               } else {
                  this.nextSpace = this.stream.length();
               }
            } catch (IOException var3) {
               this.nextSpace = this.headerPosition;
            }

            this.stream.seek(this.headerPosition);
         } catch (IOException var4) {
            this.nextSpace = this.headerPosition = 0L;
         }
      } else {
         this.stream = null;
      }
   }

   @Override
   public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
      return new TIFFStreamMetadata();
   }

   @Override
   public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
      List tagSets = new ArrayList(1);
      tagSets.add(BaselineTIFFTagSet.getInstance());
      TIFFImageMetadata imageMetadata = new TIFFImageMetadata(tagSets);
      if (imageType != null) {
         TIFFImageMetadata im = (TIFFImageMetadata)this.convertImageMetadata(imageMetadata, imageType, param);
         if (im != null) {
            imageMetadata = im;
         }
      }

      return imageMetadata;
   }

   @Override
   public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
      if (inData == null) {
         throw new IllegalArgumentException("inData == null!");
      } else {
         TIFFStreamMetadata outData = null;
         if (inData instanceof TIFFStreamMetadata) {
            outData = new TIFFStreamMetadata();
            outData.byteOrder = ((TIFFStreamMetadata)inData).byteOrder;
            return outData;
         } else {
            if (Arrays.asList(inData.getMetadataFormatNames()).contains("com_sun_media_imageio_plugins_tiff_stream_1.0")) {
               outData = new TIFFStreamMetadata();
               String format = "com_sun_media_imageio_plugins_tiff_stream_1.0";

               try {
                  outData.mergeTree(format, inData.getAsTree(format));
               } catch (IIOInvalidTreeException var6) {
               }
            }

            return outData;
         }
      }
   }

   @Override
   public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
      if (inData == null) {
         throw new IllegalArgumentException("inData == null!");
      } else if (imageType == null) {
         throw new IllegalArgumentException("imageType == null!");
      } else {
         TIFFImageMetadata outData = null;
         if (inData instanceof TIFFImageMetadata) {
            TIFFIFD inIFD = ((TIFFImageMetadata)inData).getRootIFD();
            outData = new TIFFImageMetadata(inIFD.getShallowClone());
         } else if (Arrays.asList(inData.getMetadataFormatNames()).contains("com_sun_media_imageio_plugins_tiff_image_1.0")) {
            try {
               outData = this.convertNativeImageMetadata(inData);
            } catch (IIOInvalidTreeException var15) {
            }
         } else if (inData.isStandardMetadataFormatSupported()) {
            try {
               outData = this.convertStandardImageMetadata(inData);
            } catch (IIOInvalidTreeException var14) {
            }
         }

         if (outData != null) {
            TIFFImageWriter bogusWriter = new TIFFImageWriter(this.originatingProvider);
            bogusWriter.imageMetadata = outData;
            bogusWriter.param = param;
            SampleModel sm = imageType.getSampleModel();

            Object var8;
            try {
               bogusWriter.setupMetadata(imageType.getColorModel(), sm, sm.getWidth(), sm.getHeight());
               return bogusWriter.imageMetadata;
            } catch (IIOException var16) {
               var8 = null;
            } finally {
               bogusWriter.dispose();
            }

            return (IIOMetadata)var8;
         } else {
            return outData;
         }
      }
   }

   private TIFFImageMetadata convertStandardImageMetadata(IIOMetadata inData) throws IIOInvalidTreeException {
      if (inData == null) {
         throw new IllegalArgumentException("inData == null!");
      } else if (!inData.isStandardMetadataFormatSupported()) {
         throw new IllegalArgumentException("inData does not support standard metadata format!");
      } else {
         TIFFImageMetadata outData = null;
         String formatName = "javax_imageio_1.0";
         Node tree = inData.getAsTree(formatName);
         if (tree != null) {
            List tagSets = new ArrayList(1);
            tagSets.add(BaselineTIFFTagSet.getInstance());
            outData = new TIFFImageMetadata(tagSets);
            outData.setFromTree(formatName, tree);
         }

         return outData;
      }
   }

   private TIFFImageMetadata convertNativeImageMetadata(IIOMetadata inData) throws IIOInvalidTreeException {
      if (inData == null) {
         throw new IllegalArgumentException("inData == null!");
      } else if (!Arrays.asList(inData.getMetadataFormatNames()).contains("com_sun_media_imageio_plugins_tiff_image_1.0")) {
         throw new IllegalArgumentException("inData does not support native metadata format!");
      } else {
         TIFFImageMetadata outData = null;
         String formatName = "com_sun_media_imageio_plugins_tiff_image_1.0";
         Node tree = inData.getAsTree(formatName);
         if (tree != null) {
            List tagSets = new ArrayList(1);
            tagSets.add(BaselineTIFFTagSet.getInstance());
            outData = new TIFFImageMetadata(tagSets);
            outData.setFromTree(formatName, tree);
         }

         return outData;
      }
   }

   void setupMetadata(ColorModel cm, SampleModel sm, int destWidth, int destHeight) throws IIOException {
      TIFFIFD rootIFD = this.imageMetadata.getRootIFD();
      BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
      TIFFField f = rootIFD.getTIFFField(284);
      if (f != null && f.getAsInt(0) != 1) {
         TIFFField planarConfigurationField = new TIFFField(base.getTag(284), 1);
         rootIFD.addTIFFField(planarConfigurationField);
      }

      char[] extraSamples = null;
      this.photometricInterpretation = -1;
      boolean forcePhotometricInterpretation = false;
      f = rootIFD.getTIFFField(262);
      if (f != null) {
         this.photometricInterpretation = f.getAsInt(0);
         if (this.photometricInterpretation == 3 && !(cm instanceof IndexColorModel)) {
            this.photometricInterpretation = -1;
         } else {
            forcePhotometricInterpretation = true;
         }
      }

      int[] sampleSize = sm.getSampleSize();
      int numBands = sm.getNumBands();
      int numExtraSamples = 0;
      if (numBands > 1 && cm != null && cm.hasAlpha()) {
         numBands--;
         numExtraSamples = 1;
         extraSamples = new char[1];
         if (cm.isAlphaPremultiplied()) {
            extraSamples[0] = 1;
         } else {
            extraSamples[0] = 2;
         }
      }

      if (numBands == 3) {
         this.nativePhotometricInterpretation = 2;
         if (this.photometricInterpretation == -1) {
            this.photometricInterpretation = 2;
         }
      } else if (sm.getNumBands() == 1 && cm instanceof IndexColorModel) {
         IndexColorModel icm = (IndexColorModel)cm;
         int r0 = icm.getRed(0);
         int r1 = icm.getRed(1);
         if (icm.getMapSize() == 2
            && r0 == icm.getGreen(0)
            && r0 == icm.getBlue(0)
            && r1 == icm.getGreen(1)
            && r1 == icm.getBlue(1)
            && (r0 == 0 || r0 == 255)
            && (r1 == 0 || r1 == 255)
            && r0 != r1) {
            if (r0 == 0) {
               this.nativePhotometricInterpretation = 1;
            } else {
               this.nativePhotometricInterpretation = 0;
            }

            if (this.photometricInterpretation != 1 && this.photometricInterpretation != 0) {
               this.photometricInterpretation = r0 == 0 ? 1 : 0;
            }
         } else {
            this.nativePhotometricInterpretation = this.photometricInterpretation = 3;
         }
      } else {
         if (cm != null) {
            switch (cm.getColorSpace().getType()) {
               case 1:
                  this.nativePhotometricInterpretation = 8;
                  break;
               case 3:
                  this.nativePhotometricInterpretation = 6;
                  break;
               case 9:
                  this.nativePhotometricInterpretation = 5;
                  break;
               default:
                  this.nativePhotometricInterpretation = 1;
            }
         } else {
            this.nativePhotometricInterpretation = 1;
         }

         if (this.photometricInterpretation == -1) {
            this.photometricInterpretation = this.nativePhotometricInterpretation;
         }
      }

      this.compressor = null;
      this.colorConverter = null;
      if (this.param instanceof TIFFImageWriteParam) {
         TIFFImageWriteParam tparam = (TIFFImageWriteParam)this.param;
         if (tparam.getCompressionMode() == 2) {
            this.compressor = tparam.getTIFFCompressor();
            String compressionType = this.param.getCompressionType();
            if (this.compressor != null && !this.compressor.getCompressionType().equals(compressionType)) {
               this.compressor = null;
            }
         } else {
            this.compressor = null;
         }

         this.colorConverter = tparam.getColorConverter();
         if (this.colorConverter != null) {
            this.photometricInterpretation = tparam.getPhotometricInterpretation();
         }
      }

      int compressionMode = this.param instanceof TIFFImageWriteParam ? this.param.getCompressionMode() : 1;
      switch (compressionMode) {
         case 2:
            String compressionType = this.param.getCompressionType();
            if (compressionType == null) {
               this.compression = 1;
            } else {
               int len = compressionTypes.length;

               for (int i = 0; i < len; i++) {
                  if (compressionType.equals(compressionTypes[i])) {
                     this.compression = compressionNumbers[i];
                  }
               }
            }

            if (this.compressor != null && this.compressor.getCompressionTagValue() != this.compression) {
               this.compressor = null;
            }
            break;
         case 3:
            TIFFField compField = rootIFD.getTIFFField(259);
            if (compField != null) {
               this.compression = compField.getAsInt(0);
               break;
            }
         case 0:
         case 1:
         default:
            this.compression = 1;
      }

      TIFFField predictorField = rootIFD.getTIFFField(317);
      if (predictorField != null) {
         this.predictor = predictorField.getAsInt(0);
         if (sampleSize[0] != 8 || this.predictor != 1 && this.predictor != 2) {
            this.predictor = 1;
            TIFFField newPredictorField = new TIFFField(base.getTag(317), this.predictor);
            rootIFD.addTIFFField(newPredictorField);
         }
      }

      TIFFField compressionField = new TIFFField(base.getTag(259), this.compression);
      rootIFD.addTIFFField(compressionField);
      boolean isEXIF = false;
      if (numBands == 3 && sampleSize[0] == 8 && sampleSize[1] == 8 && sampleSize[2] == 8) {
         if (rootIFD.getTIFFField(34665) != null) {
            if (this.compression != 1 || this.photometricInterpretation != 2 && this.photometricInterpretation != 6) {
               if (this.compression == 6) {
                  isEXIF = true;
               }
            } else {
               isEXIF = true;
            }
         } else if (compressionMode == 2 && "EXIF JPEG".equals(this.param.getCompressionType())) {
            isEXIF = true;
         }
      }

      boolean isJPEGInterchange = isEXIF && this.compression == 6;
      if (this.compressor == null) {
         if (this.compression == 2) {
            if (this.compressor == null) {
               this.compressor = new TIFFRLECompressor();
            }

            if (!forcePhotometricInterpretation) {
               this.photometricInterpretation = 0;
            }
         } else if (this.compression == 3) {
            if (this.compressor == null) {
               this.compressor = new TIFFT4Compressor();
            }

            if (!forcePhotometricInterpretation) {
               this.photometricInterpretation = 0;
            }
         } else if (this.compression == 4) {
            if (this.compressor == null) {
               this.compressor = new TIFFT6Compressor();
            }

            if (!forcePhotometricInterpretation) {
               this.photometricInterpretation = 0;
            }
         } else if (this.compression == 5) {
            this.compressor = new TIFFLZWCompressor(this.predictor);
         } else if (this.compression == 6) {
            if (!isEXIF) {
               throw new IIOException("Old JPEG compression not supported!");
            }

            this.compressor = new TIFFEXIFJPEGCompressor(this.param);
         } else if (this.compression != 7) {
            if (this.compression == 8) {
               this.compressor = new TIFFZLibCompressor(this.param, this.predictor);
            } else if (this.compression == 32773) {
               this.compressor = new TIFFPackBitsCompressor();
            } else if (this.compression == 32946) {
               this.compressor = new TIFFDeflateCompressor(this.param, this.predictor);
            } else {
               f = rootIFD.getTIFFField(266);
               boolean inverseFill = f != null && f.getAsInt(0) == 2;
               if (inverseFill) {
                  this.compressor = new TIFFLSBCompressor();
               } else {
                  this.compressor = new TIFFNullCompressor();
               }
            }
         } else {
            if (numBands == 3 && sampleSize[0] == 8 && sampleSize[1] == 8 && sampleSize[2] == 8) {
               this.photometricInterpretation = 6;
            } else {
               if (numBands != 1 || sampleSize[0] != 8) {
                  throw new IIOException("JPEG compression supported for 1- and 3-band byte images only!");
               }

               this.photometricInterpretation = 1;
            }

            this.compressor = new TIFFJPEGCompressor(this.param);
         }
      }

      if (this.colorConverter == null && cm != null && cm.getColorSpace().getType() == 5) {
         if (this.photometricInterpretation == 6 && this.compression != 7) {
            this.colorConverter = new TIFFYCbCrColorConverter(this.imageMetadata);
         } else if (this.photometricInterpretation == 8) {
            this.colorConverter = new TIFFCIELabColorConverter();
         }
      }

      if (this.photometricInterpretation == 6 && this.compression != 7) {
         rootIFD.removeTIFFField(530);
         rootIFD.removeTIFFField(531);
         rootIFD.addTIFFField(new TIFFField(base.getTag(530), 3, 2, new char[]{'\u0001', '\u0001'}));
         rootIFD.addTIFFField(new TIFFField(base.getTag(531), 3, 1, new char[]{'\u0002'}));
      }

      TIFFField photometricInterpretationField = new TIFFField(base.getTag(262), this.photometricInterpretation);
      rootIFD.addTIFFField(photometricInterpretationField);
      this.bitsPerSample = new char[numBands + numExtraSamples];
      this.bitDepth = 0;

      for (int ix = 0; ix < numBands; ix++) {
         this.bitDepth = Math.max(this.bitDepth, sampleSize[ix]);
      }

      if (this.bitDepth == 3) {
         this.bitDepth = 4;
      } else if (this.bitDepth > 4 && this.bitDepth < 8) {
         this.bitDepth = 8;
      } else if (this.bitDepth > 8 && this.bitDepth < 16) {
         this.bitDepth = 16;
      } else if (this.bitDepth > 16) {
         this.bitDepth = 32;
      }

      for (int ix = 0; ix < this.bitsPerSample.length; ix++) {
         this.bitsPerSample[ix] = (char)this.bitDepth;
      }

      if (this.bitsPerSample.length == 1 && this.bitsPerSample[0] == 1) {
         TIFFField bitsPerSampleField = rootIFD.getTIFFField(258);
         if (bitsPerSampleField != null) {
            int[] bps = bitsPerSampleField.getAsInts();
            if (bps == null || bps.length != 1 || bps[0] != 1) {
               rootIFD.removeTIFFField(258);
            }
         }
      } else {
         TIFFField bitsPerSampleField = new TIFFField(base.getTag(258), 3, this.bitsPerSample.length, this.bitsPerSample);
         rootIFD.addTIFFField(bitsPerSampleField);
      }

      f = rootIFD.getTIFFField(339);
      if (f == null && (this.bitDepth == 16 || this.bitDepth == 32)) {
         int dataType = sm.getDataType();
         char sampleFormatValue;
         if (this.bitDepth == 16 && dataType == 1) {
            sampleFormatValue = 1;
         } else if (this.bitDepth == 32 && dataType == 4) {
            sampleFormatValue = 3;
         } else {
            sampleFormatValue = 2;
         }

         this.sampleFormat = sampleFormatValue;
         char[] sampleFormatArray = new char[this.bitsPerSample.length];
         Arrays.fill(sampleFormatArray, sampleFormatValue);
         TIFFTag sampleFormatTag = base.getTag(339);
         TIFFField sampleFormatField = new TIFFField(sampleFormatTag, 3, sampleFormatArray.length, sampleFormatArray);
         rootIFD.addTIFFField(sampleFormatField);
      } else if (f != null) {
         this.sampleFormat = f.getAsInt(0);
      } else {
         this.sampleFormat = 4;
      }

      if (extraSamples != null) {
         TIFFField extraSamplesField = new TIFFField(base.getTag(338), 3, extraSamples.length, extraSamples);
         rootIFD.addTIFFField(extraSamplesField);
      } else {
         rootIFD.removeTIFFField(338);
      }

      TIFFField samplesPerPixelField = new TIFFField(base.getTag(277), this.bitsPerSample.length);
      rootIFD.addTIFFField(samplesPerPixelField);
      if (this.photometricInterpretation == 3 && cm instanceof IndexColorModel) {
         char[] colorMap = new char[3 * (1 << this.bitsPerSample[0])];
         IndexColorModel icm = (IndexColorModel)cm;
         int mapSize = 1 << this.bitsPerSample[0];
         int indexBound = Math.min(mapSize, icm.getMapSize());

         for (int ix = 0; ix < indexBound; ix++) {
            colorMap[ix] = (char)(icm.getRed(ix) * 65535 / 255);
            colorMap[mapSize + ix] = (char)(icm.getGreen(ix) * 65535 / 255);
            colorMap[2 * mapSize + ix] = (char)(icm.getBlue(ix) * 65535 / 255);
         }

         TIFFField colorMapField = new TIFFField(base.getTag(320), 3, colorMap.length, colorMap);
         rootIFD.addTIFFField(colorMapField);
      } else {
         rootIFD.removeTIFFField(320);
      }

      if (cm != null && rootIFD.getTIFFField(34675) == null && ImageUtil.isNonStandardICCColorSpace(cm.getColorSpace())) {
         ICC_ColorSpace iccColorSpace = (ICC_ColorSpace)cm.getColorSpace();
         byte[] iccProfileData = iccColorSpace.getProfile().getData();
         TIFFField iccProfileField = new TIFFField(base.getTag(34675), 7, iccProfileData.length, iccProfileData);
         rootIFD.addTIFFField(iccProfileField);
      }

      TIFFField XResolutionField = rootIFD.getTIFFField(282);
      TIFFField YResolutionField = rootIFD.getTIFFField(283);
      if (XResolutionField == null && YResolutionField == null) {
         long[][] resRational = new long[1][2];
         resRational[0] = new long[2];
         TIFFField ResolutionUnitField = rootIFD.getTIFFField(296);
         if (ResolutionUnitField == null && rootIFD.getTIFFField(286) == null && rootIFD.getTIFFField(287) == null) {
            resRational[0][0] = 1L;
            resRational[0][1] = 1L;
            ResolutionUnitField = new TIFFField(rootIFD.getTag(296), 1);
            rootIFD.addTIFFField(ResolutionUnitField);
         } else {
            int resolutionUnit = ResolutionUnitField != null ? ResolutionUnitField.getAsInt(0) : 2;
            int maxDimension = Math.max(destWidth, destHeight);
            switch (resolutionUnit) {
               case 2:
                  resRational[0][0] = (long)maxDimension;
                  resRational[0][1] = 4L;
                  break;
               case 3:
                  resRational[0][0] = 100L * (long)maxDimension;
                  resRational[0][1] = 1016L;
                  break;
               default:
                  resRational[0][0] = 1L;
                  resRational[0][1] = 1L;
            }
         }

         XResolutionField = new TIFFField(rootIFD.getTag(282), 5, 1, resRational);
         rootIFD.addTIFFField(XResolutionField);
         YResolutionField = new TIFFField(rootIFD.getTag(283), 5, 1, resRational);
         rootIFD.addTIFFField(YResolutionField);
      } else if (XResolutionField == null && YResolutionField != null) {
         long[] yResolution = (long[])YResolutionField.getAsRational(0).clone();
         XResolutionField = new TIFFField(rootIFD.getTag(282), 5, 1, yResolution);
         rootIFD.addTIFFField(XResolutionField);
      } else if (XResolutionField != null && YResolutionField == null) {
         long[] xResolution = (long[])XResolutionField.getAsRational(0).clone();
         YResolutionField = new TIFFField(rootIFD.getTag(283), 5, 1, xResolution);
         rootIFD.addTIFFField(YResolutionField);
      }

      TIFFField imageWidthField = new TIFFField(base.getTag(256), destWidth);
      rootIFD.addTIFFField(imageWidthField);
      TIFFField imageLengthField = new TIFFField(base.getTag(257), destHeight);
      rootIFD.addTIFFField(imageLengthField);
      TIFFField rowsPerStripField = rootIFD.getTIFFField(278);
      int rowsPerStrip;
      if (rowsPerStripField != null) {
         rowsPerStrip = rowsPerStripField.getAsInt(0);
         if (rowsPerStrip < 0) {
            rowsPerStrip = destHeight;
         }
      } else {
         int bitsPerPixel = this.bitDepth * (numBands + numExtraSamples);
         int bytesPerRow = (bitsPerPixel * destWidth + 7) / 8;
         rowsPerStrip = Math.max(Math.max(8192 / bytesPerRow, 1), 8);
      }

      rowsPerStrip = Math.min(rowsPerStrip, destHeight);
      boolean useTiling = false;
      int tilingMode = this.param instanceof TIFFImageWriteParam ? this.param.getTilingMode() : 1;
      if (tilingMode == 0 || tilingMode == 1) {
         this.tileWidth = destWidth;
         this.tileLength = rowsPerStrip;
         useTiling = false;
      } else if (tilingMode == 2) {
         this.tileWidth = this.param.getTileWidth();
         this.tileLength = this.param.getTileHeight();
         useTiling = true;
      } else {
         if (tilingMode != 3) {
            throw new IIOException("Illegal value of tilingMode!");
         }

         f = rootIFD.getTIFFField(322);
         if (f == null) {
            this.tileWidth = destWidth;
            useTiling = false;
         } else {
            this.tileWidth = f.getAsInt(0);
            useTiling = true;
         }

         f = rootIFD.getTIFFField(323);
         if (f == null) {
            this.tileLength = rowsPerStrip;
         } else {
            this.tileLength = f.getAsInt(0);
            useTiling = true;
         }
      }

      if (this.compression == 7) {
         int subX;
         int subY;
         if (numBands == 1) {
            subY = 1;
            subX = 1;
         } else {
            subY = 2;
            subX = 2;
         }

         if (useTiling) {
            int MCUMultipleX = 8 * subX;
            int MCUMultipleY = 8 * subY;
            this.tileWidth = Math.max(MCUMultipleX * ((this.tileWidth + MCUMultipleX / 2) / MCUMultipleX), MCUMultipleX);
            this.tileLength = Math.max(MCUMultipleY * ((this.tileLength + MCUMultipleY / 2) / MCUMultipleY), MCUMultipleY);
         } else if (rowsPerStrip < destHeight) {
            int MCUMultiple = 8 * Math.max(subX, subY);
            rowsPerStrip = this.tileLength = Math.max(MCUMultiple * ((this.tileLength + MCUMultiple / 2) / MCUMultiple), MCUMultiple);
         }
      } else if (isJPEGInterchange) {
         this.tileWidth = destWidth;
         this.tileLength = destHeight;
      } else if (useTiling) {
         int tileWidthRemainder = this.tileWidth % 16;
         if (tileWidthRemainder != 0) {
            this.tileWidth = Math.max(16 * ((this.tileWidth + 8) / 16), 16);
         }

         int tileLengthRemainder = this.tileLength % 16;
         if (tileLengthRemainder != 0) {
            this.tileLength = Math.max(16 * ((this.tileLength + 8) / 16), 16);
         }
      }

      this.tilesAcross = (destWidth + this.tileWidth - 1) / this.tileWidth;
      this.tilesDown = (destHeight + this.tileLength - 1) / this.tileLength;
      if (!useTiling) {
         this.isTiled = false;
         rootIFD.removeTIFFField(322);
         rootIFD.removeTIFFField(323);
         rootIFD.removeTIFFField(324);
         rootIFD.removeTIFFField(325);
         rowsPerStripField = new TIFFField(base.getTag(278), rowsPerStrip);
         rootIFD.addTIFFField(rowsPerStripField);
         TIFFField stripOffsetsField = new TIFFField(base.getTag(273), 4, this.tilesDown);
         rootIFD.addTIFFField(stripOffsetsField);
         TIFFField stripByteCountsField = new TIFFField(base.getTag(279), 4, this.tilesDown);
         rootIFD.addTIFFField(stripByteCountsField);
      } else {
         this.isTiled = true;
         rootIFD.removeTIFFField(278);
         rootIFD.removeTIFFField(273);
         rootIFD.removeTIFFField(279);
         TIFFField tileWidthField = new TIFFField(base.getTag(322), this.tileWidth);
         rootIFD.addTIFFField(tileWidthField);
         TIFFField tileLengthField = new TIFFField(base.getTag(323), this.tileLength);
         rootIFD.addTIFFField(tileLengthField);
         TIFFField tileOffsetsField = new TIFFField(base.getTag(324), 4, this.tilesDown * this.tilesAcross);
         rootIFD.addTIFFField(tileOffsetsField);
         TIFFField tileByteCountsField = new TIFFField(base.getTag(325), 4, this.tilesDown * this.tilesAcross);
         rootIFD.addTIFFField(tileByteCountsField);
      }

      if (isEXIF) {
         boolean isPrimaryIFD = this.isEncodingEmpty();
         if (this.compression == 6) {
            rootIFD.removeTIFFField(256);
            rootIFD.removeTIFFField(257);
            rootIFD.removeTIFFField(258);
            if (isPrimaryIFD) {
               rootIFD.removeTIFFField(259);
            }

            rootIFD.removeTIFFField(262);
            rootIFD.removeTIFFField(273);
            rootIFD.removeTIFFField(277);
            rootIFD.removeTIFFField(278);
            rootIFD.removeTIFFField(279);
            rootIFD.removeTIFFField(284);
            if (rootIFD.getTIFFField(296) == null) {
               f = new TIFFField(base.getTag(296), 2);
               rootIFD.addTIFFField(f);
            }

            if (isPrimaryIFD) {
               rootIFD.removeTIFFField(513);
               rootIFD.removeTIFFField(514);
               rootIFD.removeTIFFField(530);
               if (rootIFD.getTIFFField(531) == null) {
                  f = new TIFFField(base.getTag(531), 3, 1, new char[]{'\u0001'});
                  rootIFD.addTIFFField(f);
               }
            } else {
               f = new TIFFField(base.getTag(513), 4, 1);
               rootIFD.addTIFFField(f);
               f = new TIFFField(base.getTag(514), 4, 1);
               rootIFD.addTIFFField(f);
               rootIFD.removeTIFFField(530);
            }
         } else {
            if (rootIFD.getTIFFField(296) == null) {
               f = new TIFFField(base.getTag(296), 2);
               rootIFD.addTIFFField(f);
            }

            rootIFD.removeTIFFField(513);
            rootIFD.removeTIFFField(514);
            if (this.photometricInterpretation == 2) {
               rootIFD.removeTIFFField(529);
               rootIFD.removeTIFFField(530);
               rootIFD.removeTIFFField(531);
            }
         }

         TIFFTagSet exifTags = EXIFTIFFTagSet.getInstance();
         TIFFIFD exifIFD = null;
         f = rootIFD.getTIFFField(34665);
         if (f != null) {
            exifIFD = (TIFFIFD)f.getData();
         } else if (isPrimaryIFD) {
            List exifTagSets = new ArrayList(1);
            exifTagSets.add(exifTags);
            exifIFD = new TIFFIFD(exifTagSets);
            TIFFTagSet tagSet = EXIFParentTIFFTagSet.getInstance();
            TIFFTag exifIFDTag = tagSet.getTag(34665);
            rootIFD.addTIFFField(new TIFFField(exifIFDTag, 4, 1, exifIFD));
         }

         if (exifIFD != null) {
            if (exifIFD.getTIFFField(36864) == null) {
               f = new TIFFField(exifTags.getTag(36864), 7, 4, EXIFTIFFTagSet.EXIF_VERSION_2_2);
               exifIFD.addTIFFField(f);
            }

            if (this.compression == 6) {
               if (exifIFD.getTIFFField(37121) == null) {
                  f = new TIFFField(exifTags.getTag(37121), 7, 4, new byte[]{1, 2, 3, 0});
                  exifIFD.addTIFFField(f);
               }
            } else {
               exifIFD.removeTIFFField(37121);
               exifIFD.removeTIFFField(37122);
            }

            if (exifIFD.getTIFFField(40960) == null) {
               f = new TIFFField(exifTags.getTag(40960), 7, 4, new byte[]{48, 49, 48, 48});
               exifIFD.addTIFFField(f);
            }

            if (exifIFD.getTIFFField(40961) == null) {
               f = new TIFFField(exifTags.getTag(40961), 3, 1, new char[]{'\u0001'});
               exifIFD.addTIFFField(f);
            }

            if (this.compression == 6) {
               if (exifIFD.getTIFFField(40962) == null) {
                  f = new TIFFField(exifTags.getTag(40962), destWidth);
                  exifIFD.addTIFFField(f);
               }

               if (exifIFD.getTIFFField(40963) == null) {
                  f = new TIFFField(exifTags.getTag(40963), destHeight);
                  exifIFD.addTIFFField(f);
               }
            } else {
               exifIFD.removeTIFFField(40965);
            }
         }
      }
   }

   private int writeTile(Rectangle tileRect, TIFFCompressor compressor) throws IOException {
      Rectangle imageBounds = new Rectangle(this.image.getMinX(), this.image.getMinY(), this.image.getWidth(), this.image.getHeight());
      Rectangle activeRect;
      boolean isPadded;
      if (!this.isTiled) {
         activeRect = tileRect.intersection(imageBounds);
         tileRect = activeRect;
         isPadded = false;
      } else if (imageBounds.contains(tileRect)) {
         activeRect = tileRect;
         isPadded = false;
      } else {
         activeRect = imageBounds.intersection(tileRect);
         isPadded = true;
      }

      if (activeRect.isEmpty()) {
         return 0;
      } else {
         int minX = tileRect.x;
         int minY = tileRect.y;
         int width = tileRect.width;
         int height = tileRect.height;
         if (this.isImageSimple) {
            SampleModel sm = this.image.getSampleModel();
            Raster raster = this.image.getData(activeRect);
            if (isPadded) {
               WritableRaster wr = raster.createCompatibleWritableRaster(minX, minY, width, height);
               wr.setRect(raster);
               raster = wr;
            }

            if (this.isBilevel) {
               byte[] buf = ImageUtil.getPackedBinaryData(raster, tileRect);
               if (this.isInverted) {
                  DataBuffer dbb = raster.getDataBuffer();
                  if (dbb instanceof DataBufferByte && buf == ((DataBufferByte)dbb).getData()) {
                     byte[] bbuf = new byte[buf.length];
                     int len = buf.length;

                     for (int i = 0; i < len; i++) {
                        bbuf[i] = (byte)(buf[i] ^ 255);
                     }

                     buf = bbuf;
                  } else {
                     int len = buf.length;

                     for (int i = 0; i < len; i++) {
                        buf[i] = (byte)(buf[i] ^ 255);
                     }
                  }
               }

               return compressor.encode(buf, 0, width, height, this.sampleSize, (tileRect.width + 7) / 8);
            }

            if (this.bitDepth == 8 && sm.getDataType() == 0) {
               ComponentSampleModel csm = (ComponentSampleModel)raster.getSampleModel();
               byte[] buf = ((DataBufferByte)raster.getDataBuffer()).getData();
               int off = csm.getOffset(minX - raster.getSampleModelTranslateX(), minY - raster.getSampleModelTranslateY());
               return compressor.encode(buf, off, width, height, this.sampleSize, csm.getScanlineStride());
            }
         }

         int xSkip = this.periodX;
         int yOffset = minY;
         int ySkip = this.periodY;
         int hpixels = (width + xSkip - 1) / xSkip;
         int vpixels = (height + ySkip - 1) / ySkip;
         if (hpixels != 0 && vpixels != 0) {
            int var40 = minX * this.numBands;
            xSkip *= this.numBands;
            int samplesPerByte = 8 / this.bitDepth;
            int numSamples = width * this.numBands;
            int bytesPerRow = hpixels * this.numBands;
            if (this.bitDepth < 8) {
               bytesPerRow = (bytesPerRow + samplesPerByte - 1) / samplesPerByte;
            } else if (this.bitDepth == 16) {
               bytesPerRow *= 2;
            } else if (this.bitDepth == 32) {
               bytesPerRow *= 4;
            }

            int[] samples = null;
            float[] fsamples = null;
            if (this.sampleFormat == 3) {
               fsamples = new float[numSamples];
            } else {
               samples = new int[numSamples];
            }

            byte[] currTile = new byte[bytesPerRow * vpixels];
            if (!this.isInverted && !this.isRescaling && this.sourceBands == null && this.periodX == 1 && this.periodY == 1 && this.colorConverter == null) {
               SampleModel smx = this.image.getSampleModel();
               if (smx instanceof ComponentSampleModel && this.bitDepth == 8 && smx.getDataType() == 0) {
                  Raster rasterx = this.image.getData(activeRect);
                  if (isPadded) {
                     WritableRaster wr = rasterx.createCompatibleWritableRaster(minX, minY, width, height);
                     wr.setRect(rasterx);
                     rasterx = wr;
                  }

                  ComponentSampleModel csm = (ComponentSampleModel)rasterx.getSampleModel();
                  int[] bankIndices = csm.getBankIndices();
                  byte[][] bankData = ((DataBufferByte)rasterx.getDataBuffer()).getBankData();
                  int lineStride = csm.getScanlineStride();
                  int pixelStride = csm.getPixelStride();

                  for (int k = 0; k < this.numBands; k++) {
                     byte[] bandData = bankData[bankIndices[k]];
                     int lineOffset = csm.getOffset(
                        rasterx.getMinX() - rasterx.getSampleModelTranslateX(), rasterx.getMinY() - rasterx.getSampleModelTranslateY(), k
                     );
                     int idx = k;

                     for (int j = 0; j < vpixels; j++) {
                        int offset = lineOffset;

                        for (int i = 0; i < hpixels; i++) {
                           currTile[idx] = bandData[offset];
                           idx += this.numBands;
                           offset += pixelStride;
                        }

                        lineOffset += lineStride;
                     }
                  }

                  return compressor.encode(currTile, 0, width, height, this.sampleSize, width * this.numBands);
               }
            }

            int tcount = 0;
            int activeMinX = activeRect.x;
            int activeMinY = activeRect.y;
            int activeMaxY = activeMinY + activeRect.height - 1;
            int activeWidth = activeRect.width;
            SampleModel rowSampleModel = null;
            if (isPadded) {
               rowSampleModel = this.image.getSampleModel().createCompatibleSampleModel(width, 1);
            }

            for (int row = minY; row < yOffset + height; row += ySkip) {
               Raster ras = null;
               if (isPadded) {
                  WritableRaster wr = Raster.createWritableRaster(rowSampleModel, new Point(minX, row));
                  if (row >= activeMinY && row <= activeMaxY) {
                     Rectangle rect = new Rectangle(activeMinX, row, activeWidth, 1);
                     ras = this.image.getData(rect);
                     wr.setRect(ras);
                  }

                  ras = wr;
               } else {
                  Rectangle rect = new Rectangle(minX, row, width, 1);
                  ras = this.image.getData(rect);
               }

               if (this.sourceBands != null) {
                  ras = ras.createChild(minX, row, width, 1, minX, row, this.sourceBands);
               }

               if (this.sampleFormat == 3) {
                  ras.getPixels(minX, row, width, 1, fsamples);
               } else {
                  ras.getPixels(minX, row, width, 1, samples);
                  if (this.nativePhotometricInterpretation == 1 && this.photometricInterpretation == 0
                     || this.nativePhotometricInterpretation == 0 && this.photometricInterpretation == 1) {
                     int bitMask = (1 << this.bitDepth) - 1;

                     for (int s = 0; s < numSamples; s++) {
                        samples[s] ^= bitMask;
                     }
                  }
               }

               if (this.colorConverter != null) {
                  int idx = 0;
                  float[] result = new float[3];
                  if (this.sampleFormat == 3) {
                     for (int i = 0; i < width; i++) {
                        float r = fsamples[idx];
                        float g = fsamples[idx + 1];
                        float b = fsamples[idx + 2];
                        this.colorConverter.fromRGB(r, g, b, result);
                        fsamples[idx] = result[0];
                        fsamples[idx + 1] = result[1];
                        fsamples[idx + 2] = result[2];
                        idx += 3;
                     }
                  } else {
                     for (int i = 0; i < width; i++) {
                        float r = (float)samples[idx];
                        float g = (float)samples[idx + 1];
                        float b = (float)samples[idx + 2];
                        this.colorConverter.fromRGB(r, g, b, result);
                        samples[idx] = (int)result[0];
                        samples[idx + 1] = (int)result[1];
                        samples[idx + 2] = (int)result[2];
                        idx += 3;
                     }
                  }
               }

               int tmp = 0;
               int pos = 0;
               switch (this.bitDepth) {
                  case 1:
                  case 2:
                  case 4:
                     if (this.isRescaling) {
                        for (int s = 0; s < numSamples; s += xSkip) {
                           byte val = this.scale0[samples[s]];
                           tmp = tmp << this.bitDepth | val;
                           if (++pos == samplesPerByte) {
                              currTile[tcount++] = (byte)tmp;
                              tmp = 0;
                              pos = 0;
                           }
                        }
                     } else {
                        for (int sx = 0; sx < numSamples; sx += xSkip) {
                           byte val = (byte)samples[sx];
                           tmp = tmp << this.bitDepth | val;
                           if (++pos == samplesPerByte) {
                              currTile[tcount++] = (byte)tmp;
                              tmp = 0;
                              pos = 0;
                           }
                        }
                     }

                     if (pos != 0) {
                        tmp <<= (8 / this.bitDepth - pos) * this.bitDepth;
                        currTile[tcount++] = (byte)tmp;
                     }
                     break;
                  case 8:
                     if (this.numBands == 1) {
                        if (this.isRescaling) {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              currTile[tcount++] = this.scale0[samples[sxx]];
                           }
                        } else {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              currTile[tcount++] = (byte)samples[sxx];
                           }
                        }
                     } else if (this.isRescaling) {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              currTile[tcount++] = this.scale[b][samples[sxx + b]];
                           }
                        }
                     } else {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              currTile[tcount++] = (byte)samples[sxx + b];
                           }
                        }
                     }
                     break;
                  case 16:
                     if (this.isRescaling) {
                        if (this.stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 int sample = samples[sxx + b];
                                 currTile[tcount++] = this.scaleh[b][sample];
                                 currTile[tcount++] = this.scalel[b][sample];
                              }
                           }
                        } else {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 int sample = samples[sxx + b];
                                 currTile[tcount++] = this.scalel[b][sample];
                                 currTile[tcount++] = this.scaleh[b][sample];
                              }
                           }
                        }
                     } else if (this.stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              int sample = samples[sxx + b];
                              currTile[tcount++] = (byte)(sample >>> 8 & 0xFF);
                              currTile[tcount++] = (byte)(sample & 0xFF);
                           }
                        }
                     } else {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              int sample = samples[sxx + b];
                              currTile[tcount++] = (byte)(sample & 0xFF);
                              currTile[tcount++] = (byte)(sample >>> 8 & 0xFF);
                           }
                        }
                     }
                     break;
                  case 32:
                     if (this.sampleFormat == 3) {
                        if (this.stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 float fsample = fsamples[sxx + b];
                                 int isample = Float.floatToIntBits(fsample);
                                 currTile[tcount++] = (byte)((isample & 0xFF000000) >> 24);
                                 currTile[tcount++] = (byte)((isample & 0xFF0000) >> 16);
                                 currTile[tcount++] = (byte)((isample & 0xFF00) >> 8);
                                 currTile[tcount++] = (byte)(isample & 0xFF);
                              }
                           }
                        } else {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 float fsample = fsamples[sxx + b];
                                 int isample = Float.floatToIntBits(fsample);
                                 currTile[tcount++] = (byte)(isample & 0xFF);
                                 currTile[tcount++] = (byte)((isample & 0xFF00) >> 8);
                                 currTile[tcount++] = (byte)((isample & 0xFF0000) >> 16);
                                 currTile[tcount++] = (byte)((isample & 0xFF000000) >> 24);
                              }
                           }
                        }
                     } else if (this.isRescaling) {
                        long[] maxIn = new long[this.numBands];
                        long[] halfIn = new long[this.numBands];
                        long maxOut = (1L << (int)((long)this.bitDepth)) - 1L;

                        for (int b = 0; b < this.numBands; b++) {
                           maxIn[b] = (1L << (int)((long)this.sampleSize[b])) - 1L;
                           halfIn[b] = maxIn[b] / 2L;
                        }

                        if (this.stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 long sampleOut = ((long)samples[sxx + b] * maxOut + halfIn[b]) / maxIn[b];
                                 currTile[tcount++] = (byte)((int)((sampleOut & -16777216L) >> 24));
                                 currTile[tcount++] = (byte)((int)((sampleOut & 16711680L) >> 16));
                                 currTile[tcount++] = (byte)((int)((sampleOut & 65280L) >> 8));
                                 currTile[tcount++] = (byte)((int)(sampleOut & 255L));
                              }
                           }
                        } else {
                           for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                              for (int b = 0; b < this.numBands; b++) {
                                 long sampleOut = ((long)samples[sxx + b] * maxOut + halfIn[b]) / maxIn[b];
                                 currTile[tcount++] = (byte)((int)(sampleOut & 255L));
                                 currTile[tcount++] = (byte)((int)((sampleOut & 65280L) >> 8));
                                 currTile[tcount++] = (byte)((int)((sampleOut & 16711680L) >> 16));
                                 currTile[tcount++] = (byte)((int)((sampleOut & -16777216L) >> 24));
                              }
                           }
                        }
                     } else if (this.stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              int isample = samples[sxx + b];
                              currTile[tcount++] = (byte)((isample & 0xFF000000) >> 24);
                              currTile[tcount++] = (byte)((isample & 0xFF0000) >> 16);
                              currTile[tcount++] = (byte)((isample & 0xFF00) >> 8);
                              currTile[tcount++] = (byte)(isample & 0xFF);
                           }
                        }
                     } else {
                        for (int sxx = 0; sxx < numSamples; sxx += xSkip) {
                           for (int b = 0; b < this.numBands; b++) {
                              int isample = samples[sxx + b];
                              currTile[tcount++] = (byte)(isample & 0xFF);
                              currTile[tcount++] = (byte)((isample & 0xFF00) >> 8);
                              currTile[tcount++] = (byte)((isample & 0xFF0000) >> 16);
                              currTile[tcount++] = (byte)((isample & 0xFF000000) >> 24);
                           }
                        }
                     }
               }
            }

            int[] bitsPerSample = new int[this.numBands];

            for (int i = 0; i < bitsPerSample.length; i++) {
               bitsPerSample[i] = this.bitDepth;
            }

            return compressor.encode(currTile, 0, hpixels, vpixels, bitsPerSample, bytesPerRow);
         } else {
            return 0;
         }
      }
   }

   private boolean equals(int[] s0, int[] s1) {
      if (s0 != null && s1 != null) {
         if (s0.length != s1.length) {
            return false;
         } else {
            for (int i = 0; i < s0.length; i++) {
               if (s0[i] != s1[i]) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void initializeScaleTables(int[] sampleSize) {
      if (this.bitDepth != this.scalingBitDepth || !this.equals(sampleSize, this.sampleSize)) {
         this.isRescaling = false;
         this.scalingBitDepth = -1;
         this.scale = this.scalel = this.scaleh = (byte[][])null;
         this.scale0 = null;
         this.sampleSize = sampleSize;
         if (this.bitDepth <= 16) {
            for (int b = 0; b < this.numBands; b++) {
               if (sampleSize[b] != this.bitDepth) {
                  this.isRescaling = true;
                  break;
               }
            }
         }

         if (this.isRescaling) {
            this.scalingBitDepth = this.bitDepth;
            int maxOutSample = (1 << this.bitDepth) - 1;
            if (this.bitDepth <= 8) {
               this.scale = new byte[this.numBands][];

               for (int bx = 0; bx < this.numBands; bx++) {
                  int maxInSample = (1 << sampleSize[bx]) - 1;
                  int halfMaxInSample = maxInSample / 2;
                  this.scale[bx] = new byte[maxInSample + 1];

                  for (int s = 0; s <= maxInSample; s++) {
                     this.scale[bx][s] = (byte)((s * maxOutSample + halfMaxInSample) / maxInSample);
                  }
               }

               this.scale0 = this.scale[0];
               this.scaleh = this.scalel = (byte[][])null;
            } else if (this.bitDepth <= 16) {
               this.scaleh = new byte[this.numBands][];
               this.scalel = new byte[this.numBands][];

               for (int bx = 0; bx < this.numBands; bx++) {
                  int maxInSample = (1 << sampleSize[bx]) - 1;
                  int halfMaxInSample = maxInSample / 2;
                  this.scaleh[bx] = new byte[maxInSample + 1];
                  this.scalel[bx] = new byte[maxInSample + 1];

                  for (int s = 0; s <= maxInSample; s++) {
                     int val = (s * maxOutSample + halfMaxInSample) / maxInSample;
                     this.scaleh[bx][s] = (byte)(val >> 8);
                     this.scalel[bx][s] = (byte)(val & 0xFF);
                  }
               }

               this.scale = (byte[][])null;
               this.scale0 = null;
            }
         }
      }
   }

   @Override
   public void write(IIOMetadata sm, IIOImage iioimage, ImageWriteParam p) throws IOException {
      this.write(sm, iioimage, p, true, true);
   }

   private void writeHeader() throws IOException {
      if (this.streamMetadata != null) {
         this.byteOrder = this.streamMetadata.byteOrder;
      } else {
         this.byteOrder = ByteOrder.BIG_ENDIAN;
      }

      this.stream.setByteOrder(this.byteOrder);
      if (this.byteOrder == ByteOrder.BIG_ENDIAN) {
         this.stream.writeShort(19789);
      } else {
         this.stream.writeShort(18761);
      }

      this.stream.writeShort(42);
      this.stream.writeInt(0);
      this.nextSpace = this.stream.getStreamPosition();
      this.headerPosition = this.nextSpace - 8L;
   }

   private void write(IIOMetadata sm, IIOImage iioimage, ImageWriteParam p, boolean writeHeader, boolean writeData) throws IOException {
      if (this.stream == null) {
         throw new IllegalStateException("output == null!");
      } else if (iioimage == null) {
         throw new IllegalArgumentException("image == null!");
      } else if (iioimage.hasRaster() && !this.canWriteRasters()) {
         throw new UnsupportedOperationException("TIFF ImageWriter cannot write Rasters!");
      } else {
         this.image = iioimage.getRenderedImage();
         SampleModel sampleModel = this.image.getSampleModel();
         this.sourceXOffset = this.image.getMinX();
         this.sourceYOffset = this.image.getMinY();
         this.sourceWidth = this.image.getWidth();
         this.sourceHeight = this.image.getHeight();
         Rectangle imageBounds = new Rectangle(this.sourceXOffset, this.sourceYOffset, this.sourceWidth, this.sourceHeight);
         ColorModel colorModel = null;
         if (p == null) {
            this.param = this.getDefaultWriteParam();
            this.sourceBands = null;
            this.periodX = 1;
            this.periodY = 1;
            this.numBands = sampleModel.getNumBands();
            colorModel = this.image.getColorModel();
         } else {
            this.param = p;
            Rectangle sourceRegion = this.param.getSourceRegion();
            if (sourceRegion != null) {
               sourceRegion = sourceRegion.intersection(imageBounds);
               this.sourceXOffset = sourceRegion.x;
               this.sourceYOffset = sourceRegion.y;
               this.sourceWidth = sourceRegion.width;
               this.sourceHeight = sourceRegion.height;
            }

            int gridX = this.param.getSubsamplingXOffset();
            int gridY = this.param.getSubsamplingYOffset();
            this.sourceXOffset += gridX;
            this.sourceYOffset += gridY;
            this.sourceWidth -= gridX;
            this.sourceHeight -= gridY;
            this.periodX = this.param.getSourceXSubsampling();
            this.periodY = this.param.getSourceYSubsampling();
            int[] sBands = this.param.getSourceBands();
            if (sBands != null) {
               this.sourceBands = sBands;
               this.numBands = this.sourceBands.length;
            } else {
               this.numBands = sampleModel.getNumBands();
            }

            ImageTypeSpecifier destType = p.getDestinationType();
            if (destType != null) {
               ColorModel cm = destType.getColorModel();
               if (cm.getNumComponents() == this.numBands) {
                  colorModel = cm;
               }
            }

            if (colorModel == null) {
               colorModel = this.image.getColorModel();
            }
         }

         this.imageType = new ImageTypeSpecifier(colorModel, sampleModel);
         ImageUtil.canEncodeImage(this, this.imageType);
         int destWidth = (this.sourceWidth + this.periodX - 1) / this.periodX;
         int destHeight = (this.sourceHeight + this.periodY - 1) / this.periodY;
         if (destWidth > 0 && destHeight > 0) {
            this.clearAbortRequest();
            this.processImageStarted(0);
            if (writeHeader) {
               this.streamMetadata = null;
               if (sm != null) {
                  this.streamMetadata = (TIFFStreamMetadata)this.convertStreamMetadata(sm, this.param);
               }

               if (this.streamMetadata == null) {
                  this.streamMetadata = (TIFFStreamMetadata)this.getDefaultStreamMetadata(this.param);
               }

               this.writeHeader();
               this.stream.seek(this.headerPosition + 4L);
               this.nextSpace = this.nextSpace + 3L & -4L;
               this.stream.writeInt((int)this.nextSpace);
            }

            this.imageMetadata = null;
            IIOMetadata im = iioimage.getMetadata();
            if (im != null) {
               if (im instanceof TIFFImageMetadata) {
                  this.imageMetadata = ((TIFFImageMetadata)im).getShallowClone();
               } else if (Arrays.asList(im.getMetadataFormatNames()).contains("com_sun_media_imageio_plugins_tiff_image_1.0")) {
                  this.imageMetadata = this.convertNativeImageMetadata(im);
               } else if (im.isStandardMetadataFormatSupported()) {
                  try {
                     this.imageMetadata = this.convertStandardImageMetadata(im);
                  } catch (IIOInvalidTreeException var26) {
                  }
               }
            }

            if (this.imageMetadata == null) {
               this.imageMetadata = (TIFFImageMetadata)this.getDefaultImageMetadata(this.imageType, this.param);
            }

            this.setupMetadata(colorModel, sampleModel, destWidth, destHeight);
            this.compressor.setWriter(this);
            this.compressor.setMetadata(this.imageMetadata);
            this.compressor.setStream(this.stream);
            int[] sampleSize = sampleModel.getSampleSize();
            this.initializeScaleTables(sampleModel.getSampleSize());
            this.isBilevel = ImageUtil.isBinary(this.image.getSampleModel());
            this.isInverted = this.nativePhotometricInterpretation == 1 && this.photometricInterpretation == 0
               || this.nativePhotometricInterpretation == 0 && this.photometricInterpretation == 1;
            this.isImageSimple = (this.isBilevel || !this.isInverted && ImageUtil.imageIsContiguous(this.image))
               && !this.isRescaling
               && this.sourceBands == null
               && this.periodX == 1
               && this.periodY == 1
               && this.colorConverter == null;
            TIFFIFD rootIFD = this.imageMetadata.getRootIFD();
            rootIFD.writeToStream(this.stream);
            this.nextIFDPointerPos = this.stream.getStreamPosition();
            this.stream.writeInt(0);
            long lastIFDPosition = rootIFD.getLastPosition();
            this.stream.seek(lastIFDPosition);
            if (lastIFDPosition > this.nextSpace) {
               this.nextSpace = lastIFDPosition;
            }

            if (writeData) {
               long stripOrTileByteCountsPosition = rootIFD.getStripOrTileByteCountsPosition();
               long stripOrTileOffsetsPosition = rootIFD.getStripOrTileOffsetsPosition();
               this.totalPixels = this.tileWidth * this.tileLength * this.tilesDown * this.tilesAcross;
               this.pixelsDone = 0;

               for (int tj = 0; tj < this.tilesDown; tj++) {
                  for (int ti = 0; ti < this.tilesAcross; ti++) {
                     long pos = this.stream.getStreamPosition();
                     Rectangle tileRect = new Rectangle(
                        this.sourceXOffset + ti * this.tileWidth * this.periodX,
                        this.sourceYOffset + tj * this.tileLength * this.periodY,
                        this.tileWidth * this.periodX,
                        this.tileLength * this.periodY
                     );

                     try {
                        int byteCount = this.writeTile(tileRect, this.compressor);
                        if (pos + (long)byteCount > this.nextSpace) {
                           this.nextSpace = pos + (long)byteCount;
                        }

                        this.pixelsDone = this.pixelsDone + tileRect.width * tileRect.height;
                        this.processImageProgress(100.0F * (float)this.pixelsDone / (float)this.totalPixels);
                        this.stream.mark();
                        this.stream.seek(stripOrTileOffsetsPosition);
                        this.stream.writeInt((int)pos);
                        stripOrTileOffsetsPosition += 4L;
                        this.stream.seek(stripOrTileByteCountsPosition);
                        this.stream.writeInt(byteCount);
                        stripOrTileByteCountsPosition += 4L;
                        this.stream.reset();
                     } catch (IOException var27) {
                        throw new IIOException("I/O error writing TIFF file!", var27);
                     }

                     if (this.abortRequested()) {
                        this.processWriteAborted();
                        return;
                     }
                  }
               }

               this.processImageComplete();
            }
         } else {
            throw new IllegalArgumentException("Empty source region!");
         }
      }
   }

   @Override
   public boolean canWriteSequence() {
      return true;
   }

   @Override
   public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else {
         if (streamMetadata != null) {
            streamMetadata = this.convertStreamMetadata(streamMetadata, null);
         }

         if (streamMetadata == null) {
            streamMetadata = this.getDefaultStreamMetadata(null);
         }

         this.streamMetadata = (TIFFStreamMetadata)streamMetadata;
         this.writeHeader();
         this.isWritingSequence = true;
      }
   }

   @Override
   public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
      if (!this.isWritingSequence) {
         throw new IllegalStateException("prepareWriteSequence() has not been called!");
      } else {
         this.writeInsert(-1, image, param);
      }
   }

   @Override
   public void endWriteSequence() throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else if (!this.isWritingSequence) {
         throw new IllegalStateException("prepareWriteSequence() has not been called!");
      } else {
         this.isWritingSequence = false;
      }
   }

   @Override
   public boolean canInsertImage(int imageIndex) throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else {
         this.stream.mark();
         long[] ifdpos = new long[1];
         long[] ifd = new long[1];
         this.locateIFD(imageIndex, ifdpos, ifd);
         this.stream.reset();
         return true;
      }
   }

   private void locateIFD(int imageIndex, long[] ifdpos, long[] ifd) throws IOException {
      if (imageIndex < -1) {
         throw new IndexOutOfBoundsException("imageIndex < -1!");
      } else {
         long startPos = this.stream.getStreamPosition();
         this.stream.seek(this.headerPosition);
         int byteOrder = this.stream.readUnsignedShort();
         if (byteOrder == 19789) {
            this.stream.setByteOrder(ByteOrder.BIG_ENDIAN);
         } else {
            if (byteOrder != 18761) {
               this.stream.seek(startPos);
               throw new IIOException("Illegal byte order");
            }

            this.stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
         }

         if (this.stream.readUnsignedShort() != 42) {
            this.stream.seek(startPos);
            throw new IIOException("Illegal magic number");
         } else {
            ifdpos[0] = this.stream.getStreamPosition();
            ifd[0] = this.stream.readUnsignedInt();
            if (ifd[0] == 0L) {
               if (imageIndex > 0) {
                  this.stream.seek(startPos);
                  throw new IndexOutOfBoundsException("imageIndex is greater than the largest available index!");
               }
            } else {
               this.stream.seek(ifd[0]);

               for (int i = 0; imageIndex == -1 || i < imageIndex; i++) {
                  int numFields;
                  try {
                     numFields = this.stream.readShort();
                  } catch (EOFException var10) {
                     this.stream.seek(startPos);
                     ifd[0] = 0L;
                     return;
                  }

                  this.stream.skipBytes(12 * numFields);
                  ifdpos[0] = this.stream.getStreamPosition();
                  ifd[0] = this.stream.readUnsignedInt();
                  if (ifd[0] == 0L) {
                     if (imageIndex != -1 && i < imageIndex - 1) {
                        this.stream.seek(startPos);
                        throw new IndexOutOfBoundsException("imageIndex is greater than the largest available index!");
                     }
                     break;
                  }

                  this.stream.seek(ifd[0]);
               }
            }
         }
      }
   }

   @Override
   public void writeInsert(int imageIndex, IIOImage image, ImageWriteParam param) throws IOException {
      this.insert(imageIndex, image, param, true);
   }

   private void insert(int imageIndex, IIOImage image, ImageWriteParam param, boolean writeData) throws IOException {
      if (this.stream == null) {
         throw new IllegalStateException("Output not set!");
      } else if (image == null) {
         throw new IllegalArgumentException("image == null!");
      } else {
         long[] ifdpos = new long[1];
         long[] ifd = new long[1];
         this.locateIFD(imageIndex, ifdpos, ifd);
         this.stream.seek(ifdpos[0]);
         if (ifdpos[0] + 4L > this.nextSpace) {
            this.nextSpace = ifdpos[0] + 4L;
         }

         this.nextSpace = this.nextSpace + 3L & -4L;
         this.stream.writeInt((int)this.nextSpace);
         this.stream.seek(this.nextSpace);
         this.write(null, image, param, false, writeData);
         this.stream.seek(this.nextIFDPointerPos);
         this.stream.writeInt((int)ifd[0]);
      }
   }

   private boolean isEncodingEmpty() {
      return this.isInsertingEmpty || this.isWritingEmpty;
   }

   @Override
   public boolean canInsertEmpty(int imageIndex) throws IOException {
      return this.canInsertImage(imageIndex);
   }

   @Override
   public boolean canWriteEmpty() throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else {
         return true;
      }
   }

   private void checkParamsEmpty(ImageTypeSpecifier imageType, int width, int height, List thumbnails) {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else if (imageType == null) {
         throw new IllegalArgumentException("imageType == null!");
      } else if (width >= 1 && height >= 1) {
         if (thumbnails != null) {
            int numThumbs = thumbnails.size();

            for (int i = 0; i < numThumbs; i++) {
               Object thumb = thumbnails.get(i);
               if (thumb == null || !(thumb instanceof BufferedImage)) {
                  throw new IllegalArgumentException("thumbnails contains null references or objects other than BufferedImages!");
               }
            }
         }

         if (this.isInsertingEmpty) {
            throw new IllegalStateException("Previous call to prepareInsertEmpty() without corresponding call to endInsertEmpty()!");
         } else if (this.isWritingEmpty) {
            throw new IllegalStateException("Previous call to prepareWriteEmpty() without corresponding call to endWriteEmpty()!");
         }
      } else {
         throw new IllegalArgumentException("width < 1 || height < 1!");
      }
   }

   @Override
   public void prepareInsertEmpty(
      int imageIndex, ImageTypeSpecifier imageType, int width, int height, IIOMetadata imageMetadata, List thumbnails, ImageWriteParam param
   ) throws IOException {
      this.checkParamsEmpty(imageType, width, height, thumbnails);
      this.isInsertingEmpty = true;
      SampleModel emptySM = imageType.getSampleModel();
      RenderedImage emptyImage = new EmptyImage(0, 0, width, height, 0, 0, emptySM.getWidth(), emptySM.getHeight(), emptySM, imageType.getColorModel());
      this.insert(imageIndex, new IIOImage(emptyImage, null, imageMetadata), param, false);
   }

   @Override
   public void prepareWriteEmpty(
      IIOMetadata streamMetadata, ImageTypeSpecifier imageType, int width, int height, IIOMetadata imageMetadata, List thumbnails, ImageWriteParam param
   ) throws IOException {
      this.checkParamsEmpty(imageType, width, height, thumbnails);
      this.isWritingEmpty = true;
      SampleModel emptySM = imageType.getSampleModel();
      RenderedImage emptyImage = new EmptyImage(0, 0, width, height, 0, 0, emptySM.getWidth(), emptySM.getHeight(), emptySM, imageType.getColorModel());
      this.write(streamMetadata, new IIOImage(emptyImage, null, imageMetadata), param, true, false);
   }

   @Override
   public void endInsertEmpty() throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else if (!this.isInsertingEmpty) {
         throw new IllegalStateException("No previous call to prepareInsertEmpty()!");
      } else if (this.isWritingEmpty) {
         throw new IllegalStateException("Previous call to prepareWriteEmpty() without corresponding call to endWriteEmpty()!");
      } else if (this.inReplacePixelsNest) {
         throw new IllegalStateException("In nested call to prepareReplacePixels!");
      } else {
         this.isInsertingEmpty = false;
      }
   }

   @Override
   public void endWriteEmpty() throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else if (!this.isWritingEmpty) {
         throw new IllegalStateException("No previous call to prepareWriteEmpty()!");
      } else if (this.isInsertingEmpty) {
         throw new IllegalStateException("Previous call to prepareInsertEmpty() without corresponding call to endInsertEmpty()!");
      } else if (this.inReplacePixelsNest) {
         throw new IllegalStateException("In nested call to prepareReplacePixels!");
      } else {
         this.isWritingEmpty = false;
      }
   }

   private TIFFIFD readIFD(int imageIndex) throws IOException {
      if (this.stream == null) {
         throw new IllegalStateException("Output not set!");
      } else if (imageIndex < 0) {
         throw new IndexOutOfBoundsException("imageIndex < 0!");
      } else {
         this.stream.mark();
         long[] ifdpos = new long[1];
         long[] ifd = new long[1];
         this.locateIFD(imageIndex, ifdpos, ifd);
         if (ifd[0] == 0L) {
            this.stream.reset();
            throw new IndexOutOfBoundsException("imageIndex out of bounds!");
         } else {
            List tagSets = new ArrayList(1);
            tagSets.add(BaselineTIFFTagSet.getInstance());
            TIFFIFD rootIFD = new TIFFIFD(tagSets);
            rootIFD.initialize(this.stream, true);
            this.stream.reset();
            return rootIFD;
         }
      }
   }

   @Override
   public boolean canReplacePixels(int imageIndex) throws IOException {
      if (this.getOutput() == null) {
         throw new IllegalStateException("getOutput() == null!");
      } else {
         TIFFIFD rootIFD = this.readIFD(imageIndex);
         TIFFField f = rootIFD.getTIFFField(259);
         int compression = f.getAsInt(0);
         return compression == 1;
      }
   }

   @Override
   public void prepareReplacePixels(int imageIndex, Rectangle region) throws IOException {
      synchronized (this.replacePixelsLock) {
         if (this.stream == null) {
            throw new IllegalStateException("Output not set!");
         } else if (region == null) {
            throw new IllegalArgumentException("region == null!");
         } else if (region.getWidth() < 1.0) {
            throw new IllegalArgumentException("region.getWidth() < 1!");
         } else if (region.getHeight() < 1.0) {
            throw new IllegalArgumentException("region.getHeight() < 1!");
         } else if (this.inReplacePixelsNest) {
            throw new IllegalStateException("In nested call to prepareReplacePixels!");
         } else {
            TIFFIFD replacePixelsIFD = this.readIFD(imageIndex);
            TIFFField f = replacePixelsIFD.getTIFFField(259);
            int compression = f.getAsInt(0);
            if (compression != 1) {
               throw new UnsupportedOperationException("canReplacePixels(imageIndex) == false!");
            } else {
               f = replacePixelsIFD.getTIFFField(256);
               if (f == null) {
                  throw new IIOException("Cannot read ImageWidth field.");
               } else {
                  int w = f.getAsInt(0);
                  f = replacePixelsIFD.getTIFFField(257);
                  if (f == null) {
                     throw new IIOException("Cannot read ImageHeight field.");
                  } else {
                     int h = f.getAsInt(0);
                     Rectangle bounds = new Rectangle(0, 0, w, h);
                     region = region.intersection(bounds);
                     if (region.isEmpty()) {
                        throw new IIOException("Region does not intersect image bounds");
                     } else {
                        this.replacePixelsRegion = region;
                        f = replacePixelsIFD.getTIFFField(324);
                        if (f == null) {
                           f = replacePixelsIFD.getTIFFField(273);
                        }

                        this.replacePixelsTileOffsets = f.getAsLongs();
                        f = replacePixelsIFD.getTIFFField(325);
                        if (f == null) {
                           f = replacePixelsIFD.getTIFFField(279);
                        }

                        this.replacePixelsByteCounts = f.getAsLongs();
                        this.replacePixelsOffsetsPosition = replacePixelsIFD.getStripOrTileOffsetsPosition();
                        this.replacePixelsByteCountsPosition = replacePixelsIFD.getStripOrTileByteCountsPosition();
                        this.replacePixelsMetadata = new TIFFImageMetadata(replacePixelsIFD);
                        this.replacePixelsIndex = imageIndex;
                        this.inReplacePixelsNest = true;
                     }
                  }
               }
            }
         }
      }
   }

   private Raster subsample(
      Raster raster, int[] sourceBands, int subOriginX, int subOriginY, int subPeriodX, int subPeriodY, int dstOffsetX, int dstOffsetY, Rectangle target
   ) {
      int x = raster.getMinX();
      int y = raster.getMinY();
      int w = raster.getWidth();
      int h = raster.getHeight();
      int b = raster.getSampleModel().getNumBands();
      int t = raster.getSampleModel().getDataType();
      int outMinX = XToTileX(x, subOriginX, subPeriodX) + dstOffsetX;
      int outMinY = YToTileY(y, subOriginY, subPeriodY) + dstOffsetY;
      int outMaxX = XToTileX(x + w - 1, subOriginX, subPeriodX) + dstOffsetX;
      int outMaxY = YToTileY(y + h - 1, subOriginY, subPeriodY) + dstOffsetY;
      int outWidth = outMaxX - outMinX + 1;
      int outHeight = outMaxY - outMinY + 1;
      if (outWidth > 0 && outHeight > 0) {
         int inMinX = (outMinX - dstOffsetX) * subPeriodX + subOriginX;
         int inMaxX = (outMaxX - dstOffsetX) * subPeriodX + subOriginX;
         int inWidth = inMaxX - inMinX + 1;
         int inMinY = (outMinY - dstOffsetY) * subPeriodY + subOriginY;
         int inMaxY = (outMaxY - dstOffsetY) * subPeriodY + subOriginY;
         int inHeight = inMaxY - inMinY + 1;
         WritableRaster wr = raster.createCompatibleWritableRaster(outMinX, outMinY, outWidth, outHeight);
         int jMax = inMinY + inHeight;
         if (t != 4 && t != 5) {
            int[] samples = new int[inWidth];
            int[] subsamples = new int[outWidth];

            for (int k = 0; k < b; k++) {
               int outY = outMinY;

               for (int j = inMinY; j < jMax; j += subPeriodY) {
                  raster.getSamples(inMinX, j, inWidth, 1, k, samples);
                  int s = 0;

                  for (int i = 0; i < inWidth; i += subPeriodX) {
                     subsamples[s++] = samples[i];
                  }

                  wr.setSamples(outMinX, outY++, outWidth, 1, k, subsamples);
               }
            }
         } else {
            float[] fsamples = new float[inWidth];
            float[] fsubsamples = new float[outWidth];

            for (int k = 0; k < b; k++) {
               int outY = outMinY;

               for (int j = inMinY; j < jMax; j += subPeriodY) {
                  raster.getSamples(inMinX, j, inWidth, 1, k, fsamples);
                  int s = 0;

                  for (int i = 0; i < inWidth; i += subPeriodX) {
                     fsubsamples[s++] = fsamples[i];
                  }

                  wr.setSamples(outMinX, outY++, outWidth, 1, k, fsubsamples);
               }
            }
         }

         return wr.createChild(outMinX, outMinY, target.width, target.height, target.x, target.y, sourceBands);
      } else {
         return null;
      }
   }

   @Override
   public void replacePixels(RenderedImage image, ImageWriteParam param) throws IOException {
      synchronized (this.replacePixelsLock) {
         if (this.stream == null) {
            throw new IllegalStateException("stream == null!");
         } else if (image == null) {
            throw new IllegalArgumentException("image == null!");
         } else if (!this.inReplacePixelsNest) {
            throw new IllegalStateException("No previous call to prepareReplacePixels!");
         } else {
            int stepX = 1;
            int stepY = 1;
            int gridX = 0;
            int gridY = 0;
            if (param == null) {
               param = this.getDefaultWriteParam();
            } else {
               ImageWriteParam paramCopy = this.getDefaultWriteParam();
               paramCopy.setCompressionMode(0);
               paramCopy.setTilingMode(3);
               paramCopy.setDestinationOffset(param.getDestinationOffset());
               paramCopy.setSourceBands(param.getSourceBands());
               paramCopy.setSourceRegion(param.getSourceRegion());
               stepX = param.getSourceXSubsampling();
               stepY = param.getSourceYSubsampling();
               gridX = param.getSubsamplingXOffset();
               gridY = param.getSubsamplingYOffset();
               param = paramCopy;
            }

            TIFFField f = this.replacePixelsMetadata.getTIFFField(258);
            if (f == null) {
               throw new IIOException("Cannot read destination BitsPerSample");
            } else {
               int[] dstBitsPerSample = f.getAsInts();
               int[] srcBitsPerSample = image.getSampleModel().getSampleSize();
               int[] sourceBands = param.getSourceBands();
               if (sourceBands != null) {
                  if (sourceBands.length != dstBitsPerSample.length) {
                     throw new IIOException("Source and destination have different SamplesPerPixel");
                  }

                  for (int i = 0; i < sourceBands.length; i++) {
                     if (dstBitsPerSample[i] != srcBitsPerSample[sourceBands[i]]) {
                        throw new IIOException("Source and destination have different BitsPerSample");
                     }
                  }
               } else {
                  int srcNumBands = image.getSampleModel().getNumBands();
                  if (srcNumBands != dstBitsPerSample.length) {
                     throw new IIOException("Source and destination have different SamplesPerPixel");
                  }

                  for (int ix = 0; ix < srcNumBands; ix++) {
                     if (dstBitsPerSample[ix] != srcBitsPerSample[ix]) {
                        throw new IIOException("Source and destination have different BitsPerSample");
                     }
                  }
               }

               Rectangle srcImageBounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
               Rectangle srcRect = param.getSourceRegion();
               if (srcRect == null) {
                  srcRect = srcImageBounds;
               }

               int subPeriodX = stepX;
               int subPeriodY = stepY;
               int subOriginX = gridX + srcRect.x;
               int subOriginY = gridY + srcRect.y;
               if (!srcRect.equals(srcImageBounds)) {
                  srcRect = srcRect.intersection(srcImageBounds);
                  if (srcRect.isEmpty()) {
                     throw new IllegalArgumentException("Source region does not intersect source image!");
                  }
               }

               Point dstOffset = param.getDestinationOffset();
               int dMinX = XToTileX(srcRect.x, subOriginX, stepX) + dstOffset.x;
               int dMinY = YToTileY(srcRect.y, subOriginY, stepY) + dstOffset.y;
               int dMaxX = XToTileX(srcRect.x + srcRect.width, subOriginX, stepX) + dstOffset.x;
               int dMaxY = YToTileY(srcRect.y + srcRect.height, subOriginY, stepY) + dstOffset.y;
               Rectangle dstRect = new Rectangle(dstOffset.x, dstOffset.y, dMaxX - dMinX, dMaxY - dMinY);
               dstRect = dstRect.intersection(this.replacePixelsRegion);
               if (dstRect.isEmpty()) {
                  throw new IllegalArgumentException("Forward mapped source region does not intersect destination region!");
               } else {
                  int activeSrcMinX = (dstRect.x - dstOffset.x) * stepX + subOriginX;
                  int sxmax = (dstRect.x + dstRect.width - 1 - dstOffset.x) * stepX + subOriginX;
                  int activeSrcWidth = sxmax - activeSrcMinX + 1;
                  int activeSrcMinY = (dstRect.y - dstOffset.y) * stepY + subOriginY;
                  int symax = (dstRect.y + dstRect.height - 1 - dstOffset.y) * stepY + subOriginY;
                  int activeSrcHeight = symax - activeSrcMinY + 1;
                  Rectangle activeSrcRect = new Rectangle(activeSrcMinX, activeSrcMinY, activeSrcWidth, activeSrcHeight);
                  if (activeSrcRect.intersection(srcImageBounds).isEmpty()) {
                     throw new IllegalArgumentException("Backward mapped destination region does not intersect source image!");
                  } else {
                     if (this.reader == null) {
                        this.reader = new TIFFImageReader(new TIFFImageReaderSpi());
                     } else {
                        this.reader.reset();
                     }

                     this.stream.mark();

                     try {
                        this.stream.seek(this.headerPosition);
                        this.reader.setInput(this.stream);
                        this.imageMetadata = this.replacePixelsMetadata;
                        this.param = param;
                        SampleModel sm = image.getSampleModel();
                        ColorModel cm = image.getColorModel();
                        this.numBands = sm.getNumBands();
                        this.imageType = new ImageTypeSpecifier(image);
                        this.periodX = param.getSourceXSubsampling();
                        this.periodY = param.getSourceYSubsampling();
                        this.sourceBands = null;
                        int[] sBands = param.getSourceBands();
                        if (sBands != null) {
                           this.sourceBands = sBands;
                           this.numBands = sourceBands.length;
                        }

                        this.setupMetadata(cm, sm, this.reader.getWidth(this.replacePixelsIndex), this.reader.getHeight(this.replacePixelsIndex));
                        int[] scaleSampleSize = sm.getSampleSize();
                        this.initializeScaleTables(scaleSampleSize);
                        this.isBilevel = ImageUtil.isBinary(image.getSampleModel());
                        this.isInverted = this.nativePhotometricInterpretation == 1 && this.photometricInterpretation == 0
                           || this.nativePhotometricInterpretation == 0 && this.photometricInterpretation == 1;
                        this.isImageSimple = (this.isBilevel || !this.isInverted && ImageUtil.imageIsContiguous(image))
                           && !this.isRescaling
                           && sourceBands == null
                           && this.periodX == 1
                           && this.periodY == 1
                           && this.colorConverter == null;
                        int minTileX = XToTileX(dstRect.x, 0, this.tileWidth);
                        int minTileY = YToTileY(dstRect.y, 0, this.tileLength);
                        int maxTileX = XToTileX(dstRect.x + dstRect.width - 1, 0, this.tileWidth);
                        int maxTileY = YToTileY(dstRect.y + dstRect.height - 1, 0, this.tileLength);
                        TIFFCompressor encoder = new TIFFNullCompressor();
                        encoder.setWriter(this);
                        encoder.setStream(this.stream);
                        encoder.setMetadata(this.imageMetadata);
                        Rectangle tileRect = new Rectangle();

                        for (int ty = minTileY; ty <= maxTileY; ty++) {
                           for (int tx = minTileX; tx <= maxTileX; tx++) {
                              int tileIndex = ty * this.tilesAcross + tx;
                              boolean isEmpty = this.replacePixelsByteCounts[tileIndex] == 0L;
                              WritableRaster raster;
                              if (isEmpty) {
                                 SampleModel tileSM = sm.createCompatibleSampleModel(this.tileWidth, this.tileLength);
                                 raster = Raster.createWritableRaster(tileSM, null);
                              } else {
                                 BufferedImage tileImage = this.reader.readTile(this.replacePixelsIndex, tx, ty);
                                 raster = tileImage.getRaster();
                              }

                              tileRect.setLocation(tx * this.tileWidth, ty * this.tileLength);
                              tileRect.setSize(raster.getWidth(), raster.getHeight());
                              raster = raster.createWritableTranslatedChild(tileRect.x, tileRect.y);
                              Rectangle replacementRect = tileRect.intersection(dstRect);
                              int srcMinX = (replacementRect.x - dstOffset.x) * subPeriodX + subOriginX;
                              int srcXmax = (replacementRect.x + replacementRect.width - 1 - dstOffset.x) * subPeriodX + subOriginX;
                              int srcWidth = srcXmax - srcMinX + 1;
                              int srcMinY = (replacementRect.y - dstOffset.y) * subPeriodY + subOriginY;
                              int srcYMax = (replacementRect.y + replacementRect.height - 1 - dstOffset.y) * subPeriodY + subOriginY;
                              int srcHeight = srcYMax - srcMinY + 1;
                              Rectangle srcTileRect = new Rectangle(srcMinX, srcMinY, srcWidth, srcHeight);
                              Raster replacementData = image.getData(srcTileRect);
                              if (subPeriodX == 1 && subPeriodY == 1 && subOriginX == 0 && subOriginY == 0) {
                                 replacementData = replacementData.createChild(
                                    srcTileRect.x, srcTileRect.y, srcTileRect.width, srcTileRect.height, replacementRect.x, replacementRect.y, sourceBands
                                 );
                              } else {
                                 replacementData = this.subsample(
                                    replacementData, sourceBands, subOriginX, subOriginY, subPeriodX, subPeriodY, dstOffset.x, dstOffset.y, replacementRect
                                 );
                                 if (replacementData == null) {
                                    continue;
                                 }
                              }

                              raster.setRect(replacementData);
                              if (isEmpty) {
                                 this.stream.seek(this.nextSpace);
                              } else {
                                 this.stream.seek(this.replacePixelsTileOffsets[tileIndex]);
                              }

                              this.image = new SingleTileRenderedImage(raster, cm);
                              int numBytes = this.writeTile(tileRect, encoder);
                              if (isEmpty) {
                                 this.stream.mark();
                                 this.stream.seek(this.replacePixelsOffsetsPosition + (long)(4 * tileIndex));
                                 this.stream.writeInt((int)this.nextSpace);
                                 this.stream.seek(this.replacePixelsByteCountsPosition + (long)(4 * tileIndex));
                                 this.stream.writeInt(numBytes);
                                 this.stream.reset();
                                 this.nextSpace += (long)numBytes;
                              }
                           }
                        }
                     } catch (IOException var61) {
                        throw var61;
                     } finally {
                        this.stream.reset();
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void replacePixels(Raster raster, ImageWriteParam param) throws IOException {
      if (raster == null) {
         throw new IllegalArgumentException("raster == null!");
      } else {
         this.replacePixels(new SingleTileRenderedImage(raster, this.image.getColorModel()), param);
      }
   }

   @Override
   public void endReplacePixels() throws IOException {
      synchronized (this.replacePixelsLock) {
         if (!this.inReplacePixelsNest) {
            throw new IllegalStateException("No previous call to prepareReplacePixels()!");
         } else {
            this.replacePixelsIndex = -1;
            this.replacePixelsMetadata = null;
            this.replacePixelsTileOffsets = null;
            this.replacePixelsByteCounts = null;
            this.replacePixelsOffsetsPosition = 0L;
            this.replacePixelsByteCountsPosition = 0L;
            this.replacePixelsRegion = null;
            this.inReplacePixelsNest = false;
         }
      }
   }

   @Override
   public void reset() {
      super.reset();
      this.stream = null;
      this.image = null;
      this.imageType = null;
      this.byteOrder = null;
      this.param = null;
      if (this.compressor != null) {
         this.compressor.dispose();
      }

      this.compressor = null;
      this.colorConverter = null;
      this.streamMetadata = null;
      this.imageMetadata = null;
      this.isWritingSequence = false;
      this.isWritingEmpty = false;
      this.isInsertingEmpty = false;
      this.replacePixelsIndex = -1;
      this.replacePixelsMetadata = null;
      this.replacePixelsTileOffsets = null;
      this.replacePixelsByteCounts = null;
      this.replacePixelsOffsetsPosition = 0L;
      this.replacePixelsByteCountsPosition = 0L;
      this.replacePixelsRegion = null;
      this.inReplacePixelsNest = false;
   }

   @Override
   public void dispose() {
      this.reset();
      super.dispose();
   }
}
