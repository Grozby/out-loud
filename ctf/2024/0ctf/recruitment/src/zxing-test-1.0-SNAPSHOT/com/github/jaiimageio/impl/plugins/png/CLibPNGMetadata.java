package com.github.jaiimageio.impl.plugins.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.Node;

public class CLibPNGMetadata extends IIOMetadata implements Cloneable {
   public static final String nativeMetadataFormatName = "javax_imageio_png_1.0";
   protected static final String nativeMetadataFormatClassName = "com.github.jaiimageio.impl.plugins.png.CLibPNGMetadataFormat";
   public static final String[] IHDR_colorTypeNames = new String[]{"Grayscale", null, "RGB", "Palette", "GrayAlpha", null, "RGBAlpha"};
   public static final int[] IHDR_numChannels = new int[]{1, 0, 3, 3, 2, 0, 4};
   public static final String[] IHDR_bitDepths = new String[]{"1", "2", "4", "8", "16"};
   public static final String[] IHDR_compressionMethodNames = new String[]{"deflate"};
   public static final String[] IHDR_filterMethodNames = new String[]{"adaptive"};
   public static final String[] IHDR_interlaceMethodNames = new String[]{"none", "adam7"};
   public static final String[] iCCP_compressionMethodNames = new String[]{"deflate"};
   public static final String[] zTXt_compressionMethodNames = new String[]{"deflate"};
   public static final int PHYS_UNIT_UNKNOWN = 0;
   public static final int PHYS_UNIT_METER = 1;
   public static final String[] unitSpecifierNames = new String[]{"unknown", "meter"};
   public static final String[] renderingIntentNames = new String[]{"Perceptual", "Relative colorimetric", "Saturation", "Absolute colorimetric"};
   public static final String[] colorSpaceTypeNames = new String[]{"GRAY", null, "RGB", "RGB", "GRAY", null, "RGB"};
   static final int IHDR_TYPE = chunkType("IHDR");
   static final int PLTE_TYPE = chunkType("PLTE");
   static final int IDAT_TYPE = chunkType("IDAT");
   static final int IEND_TYPE = chunkType("IEND");
   static final int bKGD_TYPE = chunkType("bKGD");
   static final int cHRM_TYPE = chunkType("cHRM");
   static final int gAMA_TYPE = chunkType("gAMA");
   static final int hIST_TYPE = chunkType("hIST");
   static final int iCCP_TYPE = chunkType("iCCP");
   static final int iTXt_TYPE = chunkType("iTXt");
   static final int pHYs_TYPE = chunkType("pHYs");
   static final int sBIT_TYPE = chunkType("sBIT");
   static final int sPLT_TYPE = chunkType("sPLT");
   static final int sRGB_TYPE = chunkType("sRGB");
   static final int tEXt_TYPE = chunkType("tEXt");
   static final int tIME_TYPE = chunkType("tIME");
   static final int tRNS_TYPE = chunkType("tRNS");
   static final int zTXt_TYPE = chunkType("zTXt");
   static final int PNG_COLOR_GRAY = 0;
   static final int PNG_COLOR_RGB = 2;
   static final int PNG_COLOR_PALETTE = 3;
   static final int PNG_COLOR_GRAY_ALPHA = 4;
   static final int PNG_COLOR_RGB_ALPHA = 6;
   public boolean IHDR_present;
   public int IHDR_width;
   public int IHDR_height;
   public int IHDR_bitDepth;
   public int IHDR_colorType;
   public int IHDR_compressionMethod;
   public int IHDR_filterMethod;
   public int IHDR_interlaceMethod;
   public boolean PLTE_present;
   public byte[] PLTE_red;
   public byte[] PLTE_green;
   public byte[] PLTE_blue;
   public boolean bKGD_present;
   public int bKGD_colorType;
   public int bKGD_index;
   public int bKGD_gray;
   public int bKGD_red;
   public int bKGD_green;
   public int bKGD_blue;
   public boolean cHRM_present;
   public int cHRM_whitePointX;
   public int cHRM_whitePointY;
   public int cHRM_redX;
   public int cHRM_redY;
   public int cHRM_greenX;
   public int cHRM_greenY;
   public int cHRM_blueX;
   public int cHRM_blueY;
   public boolean gAMA_present;
   public int gAMA_gamma;
   public boolean hIST_present;
   public char[] hIST_histogram;
   public boolean iCCP_present;
   public String iCCP_profileName;
   public int iCCP_compressionMethod;
   public byte[] iCCP_compressedProfile;
   public ArrayList iTXt_keyword = new ArrayList();
   public ArrayList iTXt_compressionFlag = new ArrayList();
   public ArrayList iTXt_compressionMethod = new ArrayList();
   public ArrayList iTXt_languageTag = new ArrayList();
   public ArrayList iTXt_translatedKeyword = new ArrayList();
   public ArrayList iTXt_text = new ArrayList();
   public boolean pHYs_present;
   public int pHYs_pixelsPerUnitXAxis;
   public int pHYs_pixelsPerUnitYAxis;
   public int pHYs_unitSpecifier;
   public boolean sBIT_present;
   public int sBIT_colorType;
   public int sBIT_grayBits;
   public int sBIT_redBits;
   public int sBIT_greenBits;
   public int sBIT_blueBits;
   public int sBIT_alphaBits;
   public boolean sPLT_present;
   public String sPLT_paletteName;
   public int sPLT_sampleDepth;
   public int[] sPLT_red;
   public int[] sPLT_green;
   public int[] sPLT_blue;
   public int[] sPLT_alpha;
   public int[] sPLT_frequency;
   public boolean sRGB_present;
   public int sRGB_renderingIntent;
   public ArrayList tEXt_keyword = new ArrayList();
   public ArrayList tEXt_text = new ArrayList();
   public boolean tIME_present;
   public int tIME_year;
   public int tIME_month;
   public int tIME_day;
   public int tIME_hour;
   public int tIME_minute;
   public int tIME_second;
   public boolean tRNS_present;
   public int tRNS_colorType;
   public byte[] tRNS_alpha;
   public int tRNS_gray;
   public int tRNS_red;
   public int tRNS_green;
   public int tRNS_blue;
   public ArrayList zTXt_keyword = new ArrayList();
   public ArrayList zTXt_compressionMethod = new ArrayList();
   public ArrayList zTXt_text = new ArrayList();
   public ArrayList unknownChunkType = new ArrayList();
   public ArrayList unknownChunkData = new ArrayList();
   private boolean gotHeader = false;
   private boolean gotMetadata = false;

   static String toPrintableLatin1(String s) {
      if (s == null) {
         return null;
      } else {
         byte[] data = null;

         try {
            data = s.getBytes("ISO-8859-1");
         } catch (UnsupportedEncodingException var6) {
            data = s.getBytes();
         }

         int len = 0;
         int prev = 0;

         for (int i = 0; i < data.length; i++) {
            int d = data[i] & 255;
            if (prev != 32 || d != 32) {
               if (d > 32 && d <= 126 || d >= 161 && d <= 255 || d == 32 && len != 0) {
                  data[len++] = (byte)d;
               }

               prev = d;
            }
         }

         if (len == 0) {
            return "";
         } else {
            if (data[len - 1] == 32) {
               len--;
            }

            return new String(data, 0, len);
         }
      }
   }

   public CLibPNGMetadata() {
      super(true, "javax_imageio_png_1.0", "com.github.jaiimageio.impl.plugins.png.CLibPNGMetadataFormat", null, null);
   }

   public CLibPNGMetadata(IIOMetadata metadata) throws IIOInvalidTreeException {
      this();
      if (metadata != null) {
         List formats = Arrays.asList(metadata.getMetadataFormatNames());
         if (formats.contains("javax_imageio_png_1.0")) {
            String format = "javax_imageio_png_1.0";
            this.setFromTree(format, metadata.getAsTree(format));
         } else if (metadata.isStandardMetadataFormatSupported()) {
            String format = "javax_imageio_1.0";
            this.setFromTree(format, metadata.getAsTree(format));
         }
      }
   }

   public void initialize(ImageTypeSpecifier imageType, int numBands, ImageWriteParam param, int interlaceMethod) {
      ColorModel colorModel = imageType.getColorModel();
      SampleModel sampleModel = imageType.getSampleModel();
      this.IHDR_width = sampleModel.getWidth();
      this.IHDR_height = sampleModel.getHeight();
      int[] sampleSize = sampleModel.getSampleSize();
      int bitDepth = sampleSize[0];

      for (int i = 1; i < sampleSize.length; i++) {
         if (sampleSize[i] > bitDepth) {
            bitDepth = sampleSize[i];
         }
      }

      if (sampleSize.length > 1 && bitDepth < 8) {
         bitDepth = 8;
      }

      if (bitDepth > 2 && bitDepth < 4) {
         bitDepth = 4;
      } else if (bitDepth > 4 && bitDepth < 8) {
         bitDepth = 8;
      } else if (bitDepth > 8 && bitDepth < 16) {
         bitDepth = 16;
      } else if (bitDepth > 16) {
         throw new RuntimeException("bitDepth > 16!");
      }

      this.IHDR_bitDepth = bitDepth;
      if (colorModel instanceof IndexColorModel) {
         IndexColorModel icm = (IndexColorModel)colorModel;
         int size = icm.getMapSize();
         byte[] reds = new byte[size];
         icm.getReds(reds);
         byte[] greens = new byte[size];
         icm.getGreens(greens);
         byte[] blues = new byte[size];
         icm.getBlues(blues);
         boolean isGray = false;
         if (!this.IHDR_present || this.IHDR_colorType != 3) {
            isGray = true;
            int scale = 255 / ((1 << this.IHDR_bitDepth) - 1);

            for (int ix = 0; ix < size; ix++) {
               byte red = reds[ix];
               if (red != (byte)(ix * scale) || red != greens[ix] || red != blues[ix]) {
                  isGray = false;
                  break;
               }
            }
         }

         boolean hasAlpha = colorModel.hasAlpha();
         byte[] alpha = null;
         if (hasAlpha) {
            alpha = new byte[size];
            icm.getAlphas(alpha);
         }

         if (isGray && hasAlpha) {
            this.IHDR_colorType = 4;
         } else if (isGray) {
            this.IHDR_colorType = 0;
         } else {
            this.IHDR_colorType = 3;
            this.PLTE_present = true;
            this.PLTE_red = (byte[])reds.clone();
            this.PLTE_green = (byte[])greens.clone();
            this.PLTE_blue = (byte[])blues.clone();
            if (hasAlpha) {
               this.tRNS_present = true;
               this.tRNS_colorType = 3;
               this.tRNS_alpha = (byte[])alpha.clone();
            }
         }
      } else if (numBands == 1) {
         this.IHDR_colorType = 0;
      } else if (numBands == 2) {
         this.IHDR_colorType = 4;
      } else if (numBands == 3) {
         this.IHDR_colorType = 2;
      } else {
         if (numBands != 4) {
            throw new RuntimeException("Number of bands not 1-4!");
         }

         this.IHDR_colorType = 6;
      }

      this.IHDR_compressionMethod = this.IHDR_filterMethod = 0;
      if (param != null && param.getProgressiveMode() == 0) {
         this.IHDR_interlaceMethod = 0;
      } else if (param != null && param.getProgressiveMode() == 1) {
         this.IHDR_interlaceMethod = 1;
      } else {
         this.IHDR_interlaceMethod = interlaceMethod;
      }

      this.IHDR_present = true;
   }

   @Override
   public boolean isReadOnly() {
      return false;
   }

   private ArrayList cloneBytesArrayList(ArrayList in) {
      if (in == null) {
         return null;
      } else {
         ArrayList list = new ArrayList(in.size());

         for (Object o : in) {
            if (o == null) {
               list.add(null);
            } else {
               list.add(((byte[])o).clone());
            }
         }

         return list;
      }
   }

   @Override
   public Object clone() {
      CLibPNGMetadata metadata;
      try {
         metadata = (CLibPNGMetadata)super.clone();
      } catch (CloneNotSupportedException var3) {
         return null;
      }

      metadata.unknownChunkData = this.cloneBytesArrayList(this.unknownChunkData);
      return metadata;
   }

   @Override
   public Node getAsTree(String formatName) {
      if (formatName.equals("javax_imageio_png_1.0")) {
         return this.getNativeTree();
      } else if (formatName.equals("javax_imageio_1.0")) {
         return this.getStandardTree();
      } else {
         throw new IllegalArgumentException("Not a recognized format!");
      }
   }

   private Node getNativeTree() {
      IIOMetadataNode node = null;
      IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
      if (this.IHDR_present) {
         IIOMetadataNode IHDR_node = new IIOMetadataNode("IHDR");
         IHDR_node.setAttribute("width", Integer.toString(this.IHDR_width));
         IHDR_node.setAttribute("height", Integer.toString(this.IHDR_height));
         IHDR_node.setAttribute("bitDepth", Integer.toString(this.IHDR_bitDepth));
         IHDR_node.setAttribute("colorType", IHDR_colorTypeNames[this.IHDR_colorType]);
         IHDR_node.setAttribute("compressionMethod", IHDR_compressionMethodNames[this.IHDR_compressionMethod]);
         IHDR_node.setAttribute("filterMethod", IHDR_filterMethodNames[this.IHDR_filterMethod]);
         IHDR_node.setAttribute("interlaceMethod", IHDR_interlaceMethodNames[this.IHDR_interlaceMethod]);
         root.appendChild(IHDR_node);
      }

      if (this.PLTE_present) {
         IIOMetadataNode PLTE_node = new IIOMetadataNode("PLTE");
         int numEntries = this.PLTE_red.length;

         for (int i = 0; i < numEntries; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("PLTEEntry");
            entry.setAttribute("index", Integer.toString(i));
            entry.setAttribute("red", Integer.toString(this.PLTE_red[i] & 255));
            entry.setAttribute("green", Integer.toString(this.PLTE_green[i] & 255));
            entry.setAttribute("blue", Integer.toString(this.PLTE_blue[i] & 255));
            PLTE_node.appendChild(entry);
         }

         root.appendChild(PLTE_node);
      }

      if (this.bKGD_present) {
         IIOMetadataNode bKGD_node = new IIOMetadataNode("bKGD");
         if (this.bKGD_colorType == 3) {
            node = new IIOMetadataNode("bKGD_Palette");
            node.setAttribute("index", Integer.toString(this.bKGD_index));
         } else if (this.bKGD_colorType == 0) {
            node = new IIOMetadataNode("bKGD_Grayscale");
            node.setAttribute("gray", Integer.toString(this.bKGD_gray));
         } else if (this.bKGD_colorType == 2) {
            node = new IIOMetadataNode("bKGD_RGB");
            node.setAttribute("red", Integer.toString(this.bKGD_red));
            node.setAttribute("green", Integer.toString(this.bKGD_green));
            node.setAttribute("blue", Integer.toString(this.bKGD_blue));
         }

         bKGD_node.appendChild(node);
         root.appendChild(bKGD_node);
      }

      if (this.cHRM_present) {
         IIOMetadataNode cHRM_node = new IIOMetadataNode("cHRM");
         cHRM_node.setAttribute("whitePointX", Integer.toString(this.cHRM_whitePointX));
         cHRM_node.setAttribute("whitePointY", Integer.toString(this.cHRM_whitePointY));
         cHRM_node.setAttribute("redX", Integer.toString(this.cHRM_redX));
         cHRM_node.setAttribute("redY", Integer.toString(this.cHRM_redY));
         cHRM_node.setAttribute("greenX", Integer.toString(this.cHRM_greenX));
         cHRM_node.setAttribute("greenY", Integer.toString(this.cHRM_greenY));
         cHRM_node.setAttribute("blueX", Integer.toString(this.cHRM_blueX));
         cHRM_node.setAttribute("blueY", Integer.toString(this.cHRM_blueY));
         root.appendChild(cHRM_node);
      }

      if (this.gAMA_present) {
         IIOMetadataNode gAMA_node = new IIOMetadataNode("gAMA");
         gAMA_node.setAttribute("value", Integer.toString(this.gAMA_gamma));
         root.appendChild(gAMA_node);
      }

      if (this.hIST_present) {
         IIOMetadataNode hIST_node = new IIOMetadataNode("hIST");

         for (int i = 0; i < this.hIST_histogram.length; i++) {
            IIOMetadataNode hist = new IIOMetadataNode("hISTEntry");
            hist.setAttribute("index", Integer.toString(i));
            hist.setAttribute("value", Integer.toString(this.hIST_histogram[i]));
            hIST_node.appendChild(hist);
         }

         root.appendChild(hIST_node);
      }

      if (this.iCCP_present) {
         IIOMetadataNode iCCP_node = new IIOMetadataNode("iCCP");
         iCCP_node.setAttribute("profileName", this.iCCP_profileName);
         iCCP_node.setAttribute("compressionMethod", iCCP_compressionMethodNames[this.iCCP_compressionMethod]);
         Object profile = this.iCCP_compressedProfile;
         if (profile != null) {
            profile = ((byte[])profile).clone();
         }

         iCCP_node.setUserObject(profile);
         root.appendChild(iCCP_node);
      }

      if (this.iTXt_keyword.size() > 0) {
         IIOMetadataNode iTXt_parent = new IIOMetadataNode("iTXt");

         for (int i = 0; i < this.iTXt_keyword.size(); i++) {
            IIOMetadataNode iTXt_node = new IIOMetadataNode("iTXtEntry");
            iTXt_node.setAttribute("keyword", (String)this.iTXt_keyword.get(i));
            Integer val = (Integer)this.iTXt_compressionFlag.get(i);
            iTXt_node.setAttribute("compressionFlag", val.toString());
            val = (Integer)this.iTXt_compressionMethod.get(i);
            iTXt_node.setAttribute("compressionMethod", val.toString());
            iTXt_node.setAttribute("languageTag", (String)this.iTXt_languageTag.get(i));
            iTXt_node.setAttribute("translatedKeyword", (String)this.iTXt_translatedKeyword.get(i));
            iTXt_node.setAttribute("text", (String)this.iTXt_text.get(i));
            iTXt_parent.appendChild(iTXt_node);
         }

         root.appendChild(iTXt_parent);
      }

      if (this.pHYs_present) {
         IIOMetadataNode pHYs_node = new IIOMetadataNode("pHYs");
         pHYs_node.setAttribute("pixelsPerUnitXAxis", Integer.toString(this.pHYs_pixelsPerUnitXAxis));
         pHYs_node.setAttribute("pixelsPerUnitYAxis", Integer.toString(this.pHYs_pixelsPerUnitYAxis));
         pHYs_node.setAttribute("unitSpecifier", unitSpecifierNames[this.pHYs_unitSpecifier]);
         root.appendChild(pHYs_node);
      }

      if (this.sBIT_present) {
         IIOMetadataNode sBIT_node = new IIOMetadataNode("sBIT");
         if (this.sBIT_colorType == 0) {
            node = new IIOMetadataNode("sBIT_Grayscale");
            node.setAttribute("gray", Integer.toString(this.sBIT_grayBits));
         } else if (this.sBIT_colorType == 4) {
            node = new IIOMetadataNode("sBIT_GrayAlpha");
            node.setAttribute("gray", Integer.toString(this.sBIT_grayBits));
            node.setAttribute("alpha", Integer.toString(this.sBIT_alphaBits));
         } else if (this.sBIT_colorType == 2) {
            node = new IIOMetadataNode("sBIT_RGB");
            node.setAttribute("red", Integer.toString(this.sBIT_redBits));
            node.setAttribute("green", Integer.toString(this.sBIT_greenBits));
            node.setAttribute("blue", Integer.toString(this.sBIT_blueBits));
         } else if (this.sBIT_colorType == 6) {
            node = new IIOMetadataNode("sBIT_RGBAlpha");
            node.setAttribute("red", Integer.toString(this.sBIT_redBits));
            node.setAttribute("green", Integer.toString(this.sBIT_greenBits));
            node.setAttribute("blue", Integer.toString(this.sBIT_blueBits));
            node.setAttribute("alpha", Integer.toString(this.sBIT_alphaBits));
         } else if (this.sBIT_colorType == 3) {
            node = new IIOMetadataNode("sBIT_Palette");
            node.setAttribute("red", Integer.toString(this.sBIT_redBits));
            node.setAttribute("green", Integer.toString(this.sBIT_greenBits));
            node.setAttribute("blue", Integer.toString(this.sBIT_blueBits));
         }

         sBIT_node.appendChild(node);
         root.appendChild(sBIT_node);
      }

      if (this.sPLT_present) {
         IIOMetadataNode sPLT_node = new IIOMetadataNode("sPLT");
         sPLT_node.setAttribute("name", this.sPLT_paletteName);
         sPLT_node.setAttribute("sampleDepth", Integer.toString(this.sPLT_sampleDepth));
         int numEntries = this.sPLT_red.length;

         for (int i = 0; i < numEntries; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("sPLTEntry");
            entry.setAttribute("index", Integer.toString(i));
            entry.setAttribute("red", Integer.toString(this.sPLT_red[i]));
            entry.setAttribute("green", Integer.toString(this.sPLT_green[i]));
            entry.setAttribute("blue", Integer.toString(this.sPLT_blue[i]));
            entry.setAttribute("alpha", Integer.toString(this.sPLT_alpha[i]));
            entry.setAttribute("frequency", Integer.toString(this.sPLT_frequency[i]));
            sPLT_node.appendChild(entry);
         }

         root.appendChild(sPLT_node);
      }

      if (this.sRGB_present) {
         IIOMetadataNode sRGB_node = new IIOMetadataNode("sRGB");
         sRGB_node.setAttribute("renderingIntent", renderingIntentNames[this.sRGB_renderingIntent]);
         root.appendChild(sRGB_node);
      }

      if (this.tEXt_keyword.size() > 0) {
         IIOMetadataNode tEXt_parent = new IIOMetadataNode("tEXt");

         for (int i = 0; i < this.tEXt_keyword.size(); i++) {
            IIOMetadataNode tEXt_node = new IIOMetadataNode("tEXtEntry");
            tEXt_node.setAttribute("keyword", (String)this.tEXt_keyword.get(i));
            tEXt_node.setAttribute("value", (String)this.tEXt_text.get(i));
            tEXt_parent.appendChild(tEXt_node);
         }

         root.appendChild(tEXt_parent);
      }

      if (this.tIME_present) {
         IIOMetadataNode tIME_node = new IIOMetadataNode("tIME");
         tIME_node.setAttribute("year", Integer.toString(this.tIME_year));
         tIME_node.setAttribute("month", Integer.toString(this.tIME_month));
         tIME_node.setAttribute("day", Integer.toString(this.tIME_day));
         tIME_node.setAttribute("hour", Integer.toString(this.tIME_hour));
         tIME_node.setAttribute("minute", Integer.toString(this.tIME_minute));
         tIME_node.setAttribute("second", Integer.toString(this.tIME_second));
         root.appendChild(tIME_node);
      }

      if (this.tRNS_present) {
         IIOMetadataNode tRNS_node = new IIOMetadataNode("tRNS");
         if (this.tRNS_colorType == 3) {
            node = new IIOMetadataNode("tRNS_Palette");

            for (int i = 0; i < this.tRNS_alpha.length; i++) {
               IIOMetadataNode entry = new IIOMetadataNode("tRNS_PaletteEntry");
               entry.setAttribute("index", Integer.toString(i));
               entry.setAttribute("alpha", Integer.toString(this.tRNS_alpha[i] & 255));
               node.appendChild(entry);
            }
         } else if (this.tRNS_colorType == 0) {
            node = new IIOMetadataNode("tRNS_Grayscale");
            node.setAttribute("gray", Integer.toString(this.tRNS_gray));
         } else if (this.tRNS_colorType == 2) {
            node = new IIOMetadataNode("tRNS_RGB");
            node.setAttribute("red", Integer.toString(this.tRNS_red));
            node.setAttribute("green", Integer.toString(this.tRNS_green));
            node.setAttribute("blue", Integer.toString(this.tRNS_blue));
         }

         tRNS_node.appendChild(node);
         root.appendChild(tRNS_node);
      }

      if (this.zTXt_keyword.size() > 0) {
         IIOMetadataNode zTXt_parent = new IIOMetadataNode("zTXt");

         for (int i = 0; i < this.zTXt_keyword.size(); i++) {
            IIOMetadataNode zTXt_node = new IIOMetadataNode("zTXtEntry");
            zTXt_node.setAttribute("keyword", (String)this.zTXt_keyword.get(i));
            int cm = (Integer)this.zTXt_compressionMethod.get(i);
            zTXt_node.setAttribute("compressionMethod", zTXt_compressionMethodNames[cm]);
            zTXt_node.setAttribute("text", (String)this.zTXt_text.get(i));
            zTXt_parent.appendChild(zTXt_node);
         }

         root.appendChild(zTXt_parent);
      }

      if (this.unknownChunkType.size() > 0) {
         IIOMetadataNode unknown_parent = new IIOMetadataNode("UnknownChunks");

         for (int i = 0; i < this.unknownChunkType.size(); i++) {
            IIOMetadataNode unknown_node = new IIOMetadataNode("UnknownChunk");
            unknown_node.setAttribute("type", (String)this.unknownChunkType.get(i));
            unknown_node.setUserObject((byte[])this.unknownChunkData.get(i));
            unknown_parent.appendChild(unknown_node);
         }

         root.appendChild(unknown_parent);
      }

      return root;
   }

   private int getNumChannels() {
      int numChannels = IHDR_numChannels[this.IHDR_colorType];
      if (this.IHDR_colorType == 3 && this.tRNS_present && this.tRNS_colorType == this.IHDR_colorType) {
         numChannels = 4;
      }

      return numChannels;
   }

   @Override
   public IIOMetadataNode getStandardChromaNode() {
      IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("ColorSpaceType");
      node.setAttribute("name", colorSpaceTypeNames[this.IHDR_colorType]);
      chroma_node.appendChild(node);
      node = new IIOMetadataNode("NumChannels");
      node.setAttribute("value", Integer.toString(this.getNumChannels()));
      chroma_node.appendChild(node);
      if (this.gAMA_present) {
         node = new IIOMetadataNode("Gamma");
         node.setAttribute("value", Float.toString((float)this.gAMA_gamma * 1.0E-5F));
         chroma_node.appendChild(node);
      }

      node = new IIOMetadataNode("BlackIsZero");
      node.setAttribute("value", "TRUE");
      chroma_node.appendChild(node);
      if (this.PLTE_present) {
         boolean hasAlpha = this.tRNS_present && this.tRNS_colorType == 3;
         node = new IIOMetadataNode("Palette");

         for (int i = 0; i < this.PLTE_red.length; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("PaletteEntry");
            entry.setAttribute("index", Integer.toString(i));
            entry.setAttribute("red", Integer.toString(this.PLTE_red[i] & 255));
            entry.setAttribute("green", Integer.toString(this.PLTE_green[i] & 255));
            entry.setAttribute("blue", Integer.toString(this.PLTE_blue[i] & 255));
            if (hasAlpha) {
               int alpha = i < this.tRNS_alpha.length ? this.tRNS_alpha[i] & 255 : 255;
               entry.setAttribute("alpha", Integer.toString(alpha));
            }

            node.appendChild(entry);
         }

         chroma_node.appendChild(node);
      }

      if (this.bKGD_present) {
         if (this.bKGD_colorType == 3) {
            node = new IIOMetadataNode("BackgroundIndex");
            node.setAttribute("value", Integer.toString(this.bKGD_index));
         } else {
            node = new IIOMetadataNode("BackgroundColor");
            int r;
            int g;
            int b;
            if (this.bKGD_colorType == 0) {
               b = this.bKGD_gray;
               g = this.bKGD_gray;
               r = this.bKGD_gray;
            } else {
               r = this.bKGD_red;
               g = this.bKGD_green;
               b = this.bKGD_blue;
            }

            node.setAttribute("red", Integer.toString(r));
            node.setAttribute("green", Integer.toString(g));
            node.setAttribute("blue", Integer.toString(b));
         }

         chroma_node.appendChild(node);
      }

      return chroma_node;
   }

   @Override
   public IIOMetadataNode getStandardCompressionNode() {
      IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("CompressionTypeName");
      node.setAttribute("value", "deflate");
      compression_node.appendChild(node);
      node = new IIOMetadataNode("Lossless");
      node.setAttribute("value", "TRUE");
      compression_node.appendChild(node);
      node = new IIOMetadataNode("NumProgressiveScans");
      node.setAttribute("value", this.IHDR_interlaceMethod == 0 ? "1" : "7");
      compression_node.appendChild(node);
      return compression_node;
   }

   private String repeat(String s, int times) {
      if (times == 1) {
         return s;
      } else {
         StringBuffer sb = new StringBuffer((s.length() + 1) * times - 1);
         sb.append(s);

         for (int i = 1; i < times; i++) {
            sb.append(" ");
            sb.append(s);
         }

         return sb.toString();
      }
   }

   @Override
   public IIOMetadataNode getStandardDataNode() {
      IIOMetadataNode data_node = new IIOMetadataNode("Data");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("PlanarConfiguration");
      node.setAttribute("value", "PixelInterleaved");
      data_node.appendChild(node);
      node = new IIOMetadataNode("SampleFormat");
      node.setAttribute("value", this.IHDR_colorType == 3 ? "Index" : "UnsignedIntegral");
      data_node.appendChild(node);
      String bitDepth = Integer.toString(this.IHDR_bitDepth);
      node = new IIOMetadataNode("BitsPerSample");
      node.setAttribute("value", this.repeat(bitDepth, this.getNumChannels()));
      data_node.appendChild(node);
      if (this.sBIT_present) {
         node = new IIOMetadataNode("SignificantBitsPerSample");
         String sbits;
         if (this.sBIT_colorType != 0 && this.sBIT_colorType != 4) {
            sbits = Integer.toString(this.sBIT_redBits) + " " + Integer.toString(this.sBIT_greenBits) + " " + Integer.toString(this.sBIT_blueBits);
         } else {
            sbits = Integer.toString(this.sBIT_grayBits);
         }

         if (this.sBIT_colorType == 4 || this.sBIT_colorType == 6) {
            sbits = sbits + " " + Integer.toString(this.sBIT_alphaBits);
         }

         node.setAttribute("value", sbits);
         data_node.appendChild(node);
      }

      return data_node;
   }

   @Override
   public IIOMetadataNode getStandardDimensionNode() {
      IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("PixelAspectRatio");
      float ratio = this.pHYs_present ? (float)this.pHYs_pixelsPerUnitYAxis / (float)this.pHYs_pixelsPerUnitXAxis : 1.0F;
      node.setAttribute("value", Float.toString(ratio));
      dimension_node.appendChild(node);
      node = new IIOMetadataNode("ImageOrientation");
      node.setAttribute("value", "Normal");
      dimension_node.appendChild(node);
      if (this.pHYs_present && this.pHYs_unitSpecifier == 1) {
         node = new IIOMetadataNode("HorizontalPixelSize");
         node.setAttribute("value", Float.toString(1000.0F / (float)this.pHYs_pixelsPerUnitXAxis));
         dimension_node.appendChild(node);
         node = new IIOMetadataNode("VerticalPixelSize");
         node.setAttribute("value", Float.toString(1000.0F / (float)this.pHYs_pixelsPerUnitYAxis));
         dimension_node.appendChild(node);
      }

      return dimension_node;
   }

   @Override
   public IIOMetadataNode getStandardDocumentNode() {
      if (!this.tIME_present) {
         return null;
      } else {
         IIOMetadataNode document_node = new IIOMetadataNode("Document");
         IIOMetadataNode node = null;
         node = new IIOMetadataNode("ImageModificationTime");
         node.setAttribute("year", Integer.toString(this.tIME_year));
         node.setAttribute("month", Integer.toString(this.tIME_month));
         node.setAttribute("day", Integer.toString(this.tIME_day));
         node.setAttribute("hour", Integer.toString(this.tIME_hour));
         node.setAttribute("minute", Integer.toString(this.tIME_minute));
         node.setAttribute("second", Integer.toString(this.tIME_second));
         document_node.appendChild(node);
         return document_node;
      }
   }

   @Override
   public IIOMetadataNode getStandardTextNode() {
      int numEntries = this.tEXt_keyword.size() + this.iTXt_keyword.size() + this.zTXt_keyword.size();
      if (numEntries == 0) {
         return null;
      } else {
         IIOMetadataNode text_node = new IIOMetadataNode("Text");
         IIOMetadataNode node = null;

         for (int i = 0; i < this.tEXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", (String)this.tEXt_keyword.get(i));
            node.setAttribute("value", (String)this.tEXt_text.get(i));
            node.setAttribute("encoding", "ISO-8859-1");
            node.setAttribute("compression", "none");
            text_node.appendChild(node);
         }

         for (int i = 0; i < this.iTXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", (String)this.iTXt_keyword.get(i));
            node.setAttribute("value", (String)this.iTXt_text.get(i));
            node.setAttribute("language", (String)this.iTXt_languageTag.get(i));
            if ((Integer)this.iTXt_compressionFlag.get(i) == 1) {
               node.setAttribute("compression", "deflate");
            } else {
               node.setAttribute("compression", "none");
            }

            text_node.appendChild(node);
         }

         for (int i = 0; i < this.zTXt_keyword.size(); i++) {
            node = new IIOMetadataNode("TextEntry");
            node.setAttribute("keyword", (String)this.zTXt_keyword.get(i));
            node.setAttribute("value", (String)this.zTXt_text.get(i));
            node.setAttribute("compression", "deflate");
            text_node.appendChild(node);
         }

         return text_node;
      }
   }

   @Override
   public IIOMetadataNode getStandardTransparencyNode() {
      IIOMetadataNode transparency_node = new IIOMetadataNode("Transparency");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("Alpha");
      boolean hasAlpha = this.IHDR_colorType == 6
         || this.IHDR_colorType == 4
         || this.IHDR_colorType == 3 && this.tRNS_present && this.tRNS_colorType == this.IHDR_colorType && this.tRNS_alpha != null;
      node.setAttribute("value", hasAlpha ? "nonpremultiplied" : "none");
      transparency_node.appendChild(node);
      if (this.tRNS_present && (this.tRNS_colorType == 2 || this.tRNS_colorType == 0)) {
         node = new IIOMetadataNode("TransparentColor");
         if (this.tRNS_colorType == 2) {
            node.setAttribute("value", Integer.toString(this.tRNS_red) + " " + Integer.toString(this.tRNS_green) + " " + Integer.toString(this.tRNS_blue));
         } else if (this.tRNS_colorType == 0) {
            node.setAttribute("value", Integer.toString(this.tRNS_gray));
         }

         transparency_node.appendChild(node);
      }

      return transparency_node;
   }

   private void fatal(Node node, String reason) throws IIOInvalidTreeException {
      throw new IIOInvalidTreeException(reason, node);
   }

   private int getIntAttribute(Node node, String name, int defaultValue, boolean required) throws IIOInvalidTreeException {
      String value = this.getAttribute(node, name, null, required);
      return value == null ? defaultValue : Integer.parseInt(value);
   }

   private float getFloatAttribute(Node node, String name, float defaultValue, boolean required) throws IIOInvalidTreeException {
      String value = this.getAttribute(node, name, null, required);
      return value == null ? defaultValue : Float.parseFloat(value);
   }

   private int getIntAttribute(Node node, String name) throws IIOInvalidTreeException {
      return this.getIntAttribute(node, name, -1, true);
   }

   private float getFloatAttribute(Node node, String name) throws IIOInvalidTreeException {
      return this.getFloatAttribute(node, name, -1.0F, true);
   }

   private boolean getBooleanAttribute(Node node, String name, boolean defaultValue, boolean required) throws IIOInvalidTreeException {
      Node attr = node.getAttributes().getNamedItem(name);
      if (attr == null) {
         if (!required) {
            return defaultValue;
         }

         this.fatal(node, "Required attribute " + name + " not present!");
      }

      String value = attr.getNodeValue();
      if (value.equalsIgnoreCase("true")) {
         return true;
      } else if (value.equalsIgnoreCase("false")) {
         return false;
      } else {
         this.fatal(node, "Attribute " + name + " must be 'true' or 'false'!");
         return false;
      }
   }

   private boolean getBooleanAttribute(Node node, String name) throws IIOInvalidTreeException {
      return this.getBooleanAttribute(node, name, false, true);
   }

   private int getEnumeratedAttribute(Node node, String name, String[] legalNames, int defaultValue, boolean required) throws IIOInvalidTreeException {
      Node attr = node.getAttributes().getNamedItem(name);
      if (attr == null) {
         if (!required) {
            return defaultValue;
         }

         this.fatal(node, "Required attribute " + name + " not present!");
      }

      String value = attr.getNodeValue();

      for (int i = 0; i < legalNames.length; i++) {
         if (value.equals(legalNames[i])) {
            return i;
         }
      }

      this.fatal(node, "Illegal value for attribute " + name + "!");
      return -1;
   }

   private int getEnumeratedAttribute(Node node, String name, String[] legalNames) throws IIOInvalidTreeException {
      return this.getEnumeratedAttribute(node, name, legalNames, -1, true);
   }

   private String getAttribute(Node node, String name, String defaultValue, boolean required) throws IIOInvalidTreeException {
      Node attr = node.getAttributes().getNamedItem(name);
      if (attr == null) {
         if (!required) {
            return defaultValue;
         }

         this.fatal(node, "Required attribute " + name + " not present!");
      }

      return attr.getNodeValue();
   }

   private String getAttribute(Node node, String name) throws IIOInvalidTreeException {
      return this.getAttribute(node, name, null, true);
   }

   @Override
   public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
      if (formatName.equals("javax_imageio_png_1.0")) {
         if (root == null) {
            throw new IllegalArgumentException("root == null!");
         }

         this.mergeNativeTree(root);
      } else {
         if (!formatName.equals("javax_imageio_1.0")) {
            throw new IllegalArgumentException("Not a recognized format!");
         }

         if (root == null) {
            throw new IllegalArgumentException("root == null!");
         }

         this.mergeStandardTree(root);
      }
   }

   private void mergeNativeTree(Node root) throws IIOInvalidTreeException {
      if (!root.getNodeName().equals("javax_imageio_png_1.0")) {
         this.fatal(root, "Root must be javax_imageio_png_1.0");
      }

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
         String name = node.getNodeName();
         if (name.equals("IHDR")) {
            this.IHDR_width = this.getIntAttribute(node, "width");
            this.IHDR_height = this.getIntAttribute(node, "height");
            this.IHDR_bitDepth = this.getEnumeratedAttribute(node, "bitDepth", IHDR_bitDepths);
            this.IHDR_colorType = this.getEnumeratedAttribute(node, "colorType", IHDR_colorTypeNames);
            this.IHDR_compressionMethod = this.getEnumeratedAttribute(node, "compressionMethod", IHDR_compressionMethodNames);
            this.IHDR_filterMethod = this.getEnumeratedAttribute(node, "filterMethod", IHDR_filterMethodNames);
            this.IHDR_interlaceMethod = this.getEnumeratedAttribute(node, "interlaceMethod", IHDR_interlaceMethodNames);
            this.IHDR_present = true;
         } else if (name.equals("PLTE")) {
            byte[] red = new byte[256];
            byte[] green = new byte[256];
            byte[] blue = new byte[256];
            int maxindex = -1;
            Node PLTE_entry = node.getFirstChild();
            if (PLTE_entry == null) {
               this.fatal(node, "Palette has no entries!");
            }

            while (PLTE_entry != null) {
               if (!PLTE_entry.getNodeName().equals("PLTEEntry")) {
                  this.fatal(node, "Only a PLTEEntry may be a child of a PLTE!");
               }

               int index = this.getIntAttribute(PLTE_entry, "index");
               if (index < 0 || index > 255) {
                  this.fatal(node, "Bad value for PLTEEntry attribute index!");
               }

               if (index > maxindex) {
                  maxindex = index;
               }

               red[index] = (byte)this.getIntAttribute(PLTE_entry, "red");
               green[index] = (byte)this.getIntAttribute(PLTE_entry, "green");
               blue[index] = (byte)this.getIntAttribute(PLTE_entry, "blue");
               PLTE_entry = PLTE_entry.getNextSibling();
            }

            int numEntries = maxindex + 1;
            this.PLTE_red = new byte[numEntries];
            this.PLTE_green = new byte[numEntries];
            this.PLTE_blue = new byte[numEntries];
            System.arraycopy(red, 0, this.PLTE_red, 0, numEntries);
            System.arraycopy(green, 0, this.PLTE_green, 0, numEntries);
            System.arraycopy(blue, 0, this.PLTE_blue, 0, numEntries);
            this.PLTE_present = true;
         } else if (name.equals("bKGD")) {
            this.bKGD_present = false;
            Node bKGD_node = node.getFirstChild();
            if (bKGD_node == null) {
               this.fatal(node, "bKGD node has no children!");
            }

            String bKGD_name = bKGD_node.getNodeName();
            if (bKGD_name.equals("bKGD_Palette")) {
               this.bKGD_index = this.getIntAttribute(bKGD_node, "index");
               this.bKGD_colorType = 3;
            } else if (bKGD_name.equals("bKGD_Grayscale")) {
               this.bKGD_gray = this.getIntAttribute(bKGD_node, "gray");
               this.bKGD_colorType = 0;
            } else if (bKGD_name.equals("bKGD_RGB")) {
               this.bKGD_red = this.getIntAttribute(bKGD_node, "red");
               this.bKGD_green = this.getIntAttribute(bKGD_node, "green");
               this.bKGD_blue = this.getIntAttribute(bKGD_node, "blue");
               this.bKGD_colorType = 2;
            } else {
               this.fatal(node, "Bad child of a bKGD node!");
            }

            if (bKGD_node.getNextSibling() != null) {
               this.fatal(node, "bKGD node has more than one child!");
            }

            this.bKGD_present = true;
         } else if (name.equals("cHRM")) {
            this.cHRM_whitePointX = this.getIntAttribute(node, "whitePointX");
            this.cHRM_whitePointY = this.getIntAttribute(node, "whitePointY");
            this.cHRM_redX = this.getIntAttribute(node, "redX");
            this.cHRM_redY = this.getIntAttribute(node, "redY");
            this.cHRM_greenX = this.getIntAttribute(node, "greenX");
            this.cHRM_greenY = this.getIntAttribute(node, "greenY");
            this.cHRM_blueX = this.getIntAttribute(node, "blueX");
            this.cHRM_blueY = this.getIntAttribute(node, "blueY");
            this.cHRM_present = true;
         } else if (name.equals("gAMA")) {
            this.gAMA_gamma = this.getIntAttribute(node, "value");
            this.gAMA_present = true;
         } else if (name.equals("hIST")) {
            char[] hist = new char[256];
            int maxindex = -1;
            Node hIST_entry = node.getFirstChild();
            if (hIST_entry == null) {
               this.fatal(node, "hIST node has no children!");
            }

            while (hIST_entry != null) {
               if (!hIST_entry.getNodeName().equals("hISTEntry")) {
                  this.fatal(node, "Only a hISTEntry may be a child of a hIST!");
               }

               int indexx = this.getIntAttribute(hIST_entry, "index");
               if (indexx < 0 || indexx > 255) {
                  this.fatal(node, "Bad value for histEntry attribute index!");
               }

               if (indexx > maxindex) {
                  maxindex = indexx;
               }

               hist[indexx] = (char)this.getIntAttribute(hIST_entry, "value");
               hIST_entry = hIST_entry.getNextSibling();
            }

            int numEntries = maxindex + 1;
            this.hIST_histogram = new char[numEntries];
            System.arraycopy(hist, 0, this.hIST_histogram, 0, numEntries);
            this.hIST_present = true;
         } else if (name.equals("iCCP")) {
            this.iCCP_profileName = toPrintableLatin1(this.getAttribute(node, "profileName"));
            this.iCCP_compressionMethod = this.getEnumeratedAttribute(node, "compressionMethod", iCCP_compressionMethodNames);
            Object compressedProfile = ((IIOMetadataNode)node).getUserObject();
            if (compressedProfile == null) {
               this.fatal(node, "No ICCP profile present in user object!");
            }

            if (!(compressedProfile instanceof byte[])) {
               this.fatal(node, "User object not a byte array!");
            }

            this.iCCP_compressedProfile = (byte[])((byte[])compressedProfile).clone();
            this.iCCP_present = true;
         } else if (name.equals("iTXt")) {
            for (Node iTXt_node = node.getFirstChild(); iTXt_node != null; iTXt_node = iTXt_node.getNextSibling()) {
               if (!iTXt_node.getNodeName().equals("iTXtEntry")) {
                  this.fatal(node, "Only an iTXtEntry may be a child of an iTXt!");
               }

               String keyword = toPrintableLatin1(this.getAttribute(iTXt_node, "keyword"));
               this.iTXt_keyword.add(keyword);
               boolean compressionFlag = this.getBooleanAttribute(iTXt_node, "compressionFlag");
               this.iTXt_compressionFlag.add(new Boolean(compressionFlag));
               String compressionMethod = this.getAttribute(iTXt_node, "compressionMethod");
               this.iTXt_compressionMethod.add(compressionMethod);
               String languageTag = this.getAttribute(iTXt_node, "languageTag");
               this.iTXt_languageTag.add(languageTag);
               String translatedKeyword = this.getAttribute(iTXt_node, "translatedKeyword");
               this.iTXt_translatedKeyword.add(translatedKeyword);
               String text = this.getAttribute(iTXt_node, "text");
               this.iTXt_text.add(text);
            }
         } else if (name.equals("pHYs")) {
            this.pHYs_pixelsPerUnitXAxis = this.getIntAttribute(node, "pixelsPerUnitXAxis");
            this.pHYs_pixelsPerUnitYAxis = this.getIntAttribute(node, "pixelsPerUnitYAxis");
            this.pHYs_unitSpecifier = this.getEnumeratedAttribute(node, "unitSpecifier", unitSpecifierNames);
            this.pHYs_present = true;
         } else if (name.equals("sBIT")) {
            this.sBIT_present = false;
            Node sBIT_node = node.getFirstChild();
            if (sBIT_node == null) {
               this.fatal(node, "sBIT node has no children!");
            }

            String sBIT_name = sBIT_node.getNodeName();
            if (sBIT_name.equals("sBIT_Grayscale")) {
               this.sBIT_grayBits = this.getIntAttribute(sBIT_node, "gray");
               this.sBIT_colorType = 0;
            } else if (sBIT_name.equals("sBIT_GrayAlpha")) {
               this.sBIT_grayBits = this.getIntAttribute(sBIT_node, "gray");
               this.sBIT_alphaBits = this.getIntAttribute(sBIT_node, "alpha");
               this.sBIT_colorType = 4;
            } else if (sBIT_name.equals("sBIT_RGB")) {
               this.sBIT_redBits = this.getIntAttribute(sBIT_node, "red");
               this.sBIT_greenBits = this.getIntAttribute(sBIT_node, "green");
               this.sBIT_blueBits = this.getIntAttribute(sBIT_node, "blue");
               this.sBIT_colorType = 2;
            } else if (sBIT_name.equals("sBIT_RGBAlpha")) {
               this.sBIT_redBits = this.getIntAttribute(sBIT_node, "red");
               this.sBIT_greenBits = this.getIntAttribute(sBIT_node, "green");
               this.sBIT_blueBits = this.getIntAttribute(sBIT_node, "blue");
               this.sBIT_alphaBits = this.getIntAttribute(sBIT_node, "alpha");
               this.sBIT_colorType = 6;
            } else if (sBIT_name.equals("sBIT_Palette")) {
               this.sBIT_redBits = this.getIntAttribute(sBIT_node, "red");
               this.sBIT_greenBits = this.getIntAttribute(sBIT_node, "green");
               this.sBIT_blueBits = this.getIntAttribute(sBIT_node, "blue");
               this.sBIT_colorType = 3;
            } else {
               this.fatal(node, "Bad child of an sBIT node!");
            }

            if (sBIT_node.getNextSibling() != null) {
               this.fatal(node, "sBIT node has more than one child!");
            }

            this.sBIT_present = true;
         } else if (name.equals("sPLT")) {
            this.sPLT_paletteName = toPrintableLatin1(this.getAttribute(node, "name"));
            this.sPLT_sampleDepth = this.getIntAttribute(node, "sampleDepth");
            int[] red = new int[256];
            int[] green = new int[256];
            int[] blue = new int[256];
            int[] alpha = new int[256];
            int[] frequency = new int[256];
            int maxindex = -1;
            Node sPLT_entry = node.getFirstChild();
            if (sPLT_entry == null) {
               this.fatal(node, "sPLT node has no children!");
            }

            while (sPLT_entry != null) {
               if (!sPLT_entry.getNodeName().equals("sPLTEntry")) {
                  this.fatal(node, "Only an sPLTEntry may be a child of an sPLT!");
               }

               int indexxx = this.getIntAttribute(sPLT_entry, "index");
               if (indexxx < 0 || indexxx > 255) {
                  this.fatal(node, "Bad value for PLTEEntry attribute index!");
               }

               if (indexxx > maxindex) {
                  maxindex = indexxx;
               }

               red[indexxx] = this.getIntAttribute(sPLT_entry, "red");
               green[indexxx] = this.getIntAttribute(sPLT_entry, "green");
               blue[indexxx] = this.getIntAttribute(sPLT_entry, "blue");
               alpha[indexxx] = this.getIntAttribute(sPLT_entry, "alpha");
               frequency[indexxx] = this.getIntAttribute(sPLT_entry, "frequency");
               sPLT_entry = sPLT_entry.getNextSibling();
            }

            int numEntries = maxindex + 1;
            this.sPLT_red = new int[numEntries];
            this.sPLT_green = new int[numEntries];
            this.sPLT_blue = new int[numEntries];
            this.sPLT_alpha = new int[numEntries];
            this.sPLT_frequency = new int[numEntries];
            System.arraycopy(red, 0, this.sPLT_red, 0, numEntries);
            System.arraycopy(green, 0, this.sPLT_green, 0, numEntries);
            System.arraycopy(blue, 0, this.sPLT_blue, 0, numEntries);
            System.arraycopy(alpha, 0, this.sPLT_alpha, 0, numEntries);
            System.arraycopy(frequency, 0, this.sPLT_frequency, 0, numEntries);
            this.sPLT_present = true;
         } else if (name.equals("sRGB")) {
            this.sRGB_renderingIntent = this.getEnumeratedAttribute(node, "renderingIntent", renderingIntentNames);
            this.sRGB_present = true;
         } else if (name.equals("tEXt")) {
            for (Node tEXt_node = node.getFirstChild(); tEXt_node != null; tEXt_node = tEXt_node.getNextSibling()) {
               if (!tEXt_node.getNodeName().equals("tEXtEntry")) {
                  this.fatal(node, "Only an tEXtEntry may be a child of an tEXt!");
               }

               String keyword = toPrintableLatin1(this.getAttribute(tEXt_node, "keyword"));
               this.tEXt_keyword.add(keyword);
               String text = this.getAttribute(tEXt_node, "value");
               this.tEXt_text.add(text);
            }
         } else if (name.equals("tIME")) {
            this.tIME_year = this.getIntAttribute(node, "year");
            this.tIME_month = this.getIntAttribute(node, "month");
            this.tIME_day = this.getIntAttribute(node, "day");
            this.tIME_hour = this.getIntAttribute(node, "hour");
            this.tIME_minute = this.getIntAttribute(node, "minute");
            this.tIME_second = this.getIntAttribute(node, "second");
            this.tIME_present = true;
         } else if (name.equals("tRNS")) {
            this.tRNS_present = false;
            Node tRNS_node = node.getFirstChild();
            if (tRNS_node == null) {
               this.fatal(node, "tRNS node has no children!");
            }

            String tRNS_name = tRNS_node.getNodeName();
            if (tRNS_name.equals("tRNS_Palette")) {
               byte[] alpha = new byte[256];
               int maxindex = -1;
               Node tRNS_paletteEntry = tRNS_node.getFirstChild();
               if (tRNS_paletteEntry == null) {
                  this.fatal(node, "tRNS_Palette node has no children!");
               }

               while (tRNS_paletteEntry != null) {
                  if (!tRNS_paletteEntry.getNodeName().equals("tRNS_PaletteEntry")) {
                     this.fatal(node, "Only a tRNS_PaletteEntry may be a child of a tRNS_Palette!");
                  }

                  int indexxxx = this.getIntAttribute(tRNS_paletteEntry, "index");
                  if (indexxxx < 0 || indexxxx > 255) {
                     this.fatal(node, "Bad value for tRNS_PaletteEntry attribute index!");
                  }

                  if (indexxxx > maxindex) {
                     maxindex = indexxxx;
                  }

                  alpha[indexxxx] = (byte)this.getIntAttribute(tRNS_paletteEntry, "alpha");
                  tRNS_paletteEntry = tRNS_paletteEntry.getNextSibling();
               }

               int numEntries = maxindex + 1;
               this.tRNS_alpha = new byte[numEntries];
               this.tRNS_colorType = 3;
               System.arraycopy(alpha, 0, this.tRNS_alpha, 0, numEntries);
            } else if (tRNS_name.equals("tRNS_Grayscale")) {
               this.tRNS_gray = this.getIntAttribute(tRNS_node, "gray");
               this.tRNS_colorType = 0;
            } else if (tRNS_name.equals("tRNS_RGB")) {
               this.tRNS_red = this.getIntAttribute(tRNS_node, "red");
               this.tRNS_green = this.getIntAttribute(tRNS_node, "green");
               this.tRNS_blue = this.getIntAttribute(tRNS_node, "blue");
               this.tRNS_colorType = 2;
            } else {
               this.fatal(node, "Bad child of a tRNS node!");
            }

            if (tRNS_node.getNextSibling() != null) {
               this.fatal(node, "tRNS node has more than one child!");
            }

            this.tRNS_present = true;
         } else if (name.equals("zTXt")) {
            for (Node zTXt_node = node.getFirstChild(); zTXt_node != null; zTXt_node = zTXt_node.getNextSibling()) {
               if (!zTXt_node.getNodeName().equals("zTXtEntry")) {
                  this.fatal(node, "Only an zTXtEntry may be a child of an zTXt!");
               }

               String keyword = toPrintableLatin1(this.getAttribute(zTXt_node, "keyword"));
               this.zTXt_keyword.add(keyword);
               int compressionMethod = this.getEnumeratedAttribute(zTXt_node, "compressionMethod", zTXt_compressionMethodNames);
               this.zTXt_compressionMethod.add(new Integer(compressionMethod));
               String text = this.getAttribute(zTXt_node, "text");
               this.zTXt_text.add(text);
            }
         } else if (name.equals("UnknownChunks")) {
            for (Node unknown_node = node.getFirstChild(); unknown_node != null; unknown_node = unknown_node.getNextSibling()) {
               if (!unknown_node.getNodeName().equals("UnknownChunk")) {
                  this.fatal(node, "Only an UnknownChunk may be a child of an UnknownChunks!");
               }

               String chunkType = this.getAttribute(unknown_node, "type");
               Object chunkData = ((IIOMetadataNode)unknown_node).getUserObject();
               if (chunkType.length() != 4) {
                  this.fatal(unknown_node, "Chunk type must be 4 characters!");
               }

               if (chunkData == null) {
                  this.fatal(unknown_node, "No chunk data present in user object!");
               }

               if (!(chunkData instanceof byte[])) {
                  this.fatal(unknown_node, "User object not a byte array!");
               }

               this.unknownChunkType.add(chunkType);
               this.unknownChunkData.add(((byte[])chunkData).clone());
            }
         } else {
            this.fatal(node, "Unknown child of root node!");
         }
      }
   }

   private boolean isISOLatin(String s) {
      int len = s.length();

      for (int i = 0; i < len; i++) {
         if (s.charAt(i) > 255) {
            return false;
         }
      }

      return true;
   }

   private void mergeStandardTree(Node root) throws IIOInvalidTreeException {
      if (!root.getNodeName().equals("javax_imageio_1.0")) {
         this.fatal(root, "Root must be javax_imageio_1.0");
      }

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
         String name = node.getNodeName();
         if (name.equals("Chroma")) {
            for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
               String childName = child.getNodeName();
               if (childName.equals("Gamma")) {
                  float gamma = this.getFloatAttribute(child, "value");
                  this.gAMA_present = true;
                  this.gAMA_gamma = (int)((double)(gamma * 100000.0F) + 0.5);
               } else if (childName.equals("Palette")) {
                  byte[] red = new byte[256];
                  byte[] green = new byte[256];
                  byte[] blue = new byte[256];
                  int maxindex = -1;

                  for (Node entry = child.getFirstChild(); entry != null; entry = entry.getNextSibling()) {
                     String entryName = entry.getNodeName();
                     if (entryName.equals("PaletteEntry")) {
                        int index = this.getIntAttribute(entry, "index");
                        if (index >= 0 && index <= 255) {
                           red[index] = (byte)this.getIntAttribute(entry, "red");
                           green[index] = (byte)this.getIntAttribute(entry, "green");
                           blue[index] = (byte)this.getIntAttribute(entry, "blue");
                           if (index > maxindex) {
                              maxindex = index;
                           }
                        }
                     }
                  }

                  int numEntries = maxindex + 1;
                  this.PLTE_red = new byte[numEntries];
                  this.PLTE_green = new byte[numEntries];
                  this.PLTE_blue = new byte[numEntries];
                  System.arraycopy(red, 0, this.PLTE_red, 0, numEntries);
                  System.arraycopy(green, 0, this.PLTE_green, 0, numEntries);
                  System.arraycopy(blue, 0, this.PLTE_blue, 0, numEntries);
                  this.PLTE_present = true;
               } else if (childName.equals("BackgroundIndex")) {
                  this.bKGD_present = true;
                  this.bKGD_colorType = 3;
                  this.bKGD_index = this.getIntAttribute(child, "value");
               } else if (childName.equals("BackgroundColor")) {
                  int red = this.getIntAttribute(child, "red");
                  int green = this.getIntAttribute(child, "green");
                  int blue = this.getIntAttribute(child, "blue");
                  if (red == green && red == blue) {
                     this.bKGD_colorType = 0;
                     this.bKGD_gray = red;
                  } else {
                     this.bKGD_colorType = 2;
                     this.bKGD_red = red;
                     this.bKGD_green = green;
                     this.bKGD_blue = blue;
                  }

                  this.bKGD_present = true;
               }
            }
         } else if (name.equals("Compression")) {
            for (Node childx = node.getFirstChild(); childx != null; childx = childx.getNextSibling()) {
               String childName = childx.getNodeName();
               if (childName.equals("NumProgressiveScans")) {
                  int scans = this.getIntAttribute(childx, "value");
                  this.IHDR_interlaceMethod = scans > 1 ? 1 : 0;
               }
            }
         } else if (name.equals("Data")) {
            for (Node childxx = node.getFirstChild(); childxx != null; childxx = childxx.getNextSibling()) {
               String childName = childxx.getNodeName();
               if (!childName.equals("BitsPerSample")) {
                  if (childName.equals("SignificantBitsPerSample")) {
                     String s = this.getAttribute(childxx, "value");
                     StringTokenizer t = new StringTokenizer(s);
                     int numTokens = t.countTokens();
                     if (numTokens == 1) {
                        this.sBIT_colorType = 0;
                        this.sBIT_grayBits = Integer.parseInt(t.nextToken());
                     } else if (numTokens == 2) {
                        this.sBIT_colorType = 4;
                        this.sBIT_grayBits = Integer.parseInt(t.nextToken());
                        this.sBIT_alphaBits = Integer.parseInt(t.nextToken());
                     } else if (numTokens == 3) {
                        this.sBIT_colorType = 2;
                        this.sBIT_redBits = Integer.parseInt(t.nextToken());
                        this.sBIT_greenBits = Integer.parseInt(t.nextToken());
                        this.sBIT_blueBits = Integer.parseInt(t.nextToken());
                     } else if (numTokens == 4) {
                        this.sBIT_colorType = 6;
                        this.sBIT_redBits = Integer.parseInt(t.nextToken());
                        this.sBIT_greenBits = Integer.parseInt(t.nextToken());
                        this.sBIT_blueBits = Integer.parseInt(t.nextToken());
                        this.sBIT_alphaBits = Integer.parseInt(t.nextToken());
                     }

                     if (numTokens >= 1 && numTokens <= 4) {
                        this.sBIT_present = true;
                     }
                  }
               } else {
                  String sx = this.getAttribute(childxx, "value");
                  StringTokenizer tx = new StringTokenizer(sx);
                  int maxBits = -1;

                  while (tx.hasMoreTokens()) {
                     int bits = Integer.parseInt(tx.nextToken());
                     if (bits > maxBits) {
                        maxBits = bits;
                     }
                  }

                  if (maxBits < 1) {
                     maxBits = 1;
                  } else if (maxBits == 3) {
                     maxBits = 4;
                  } else if (maxBits > 4 && maxBits < 8) {
                     maxBits = 8;
                  } else if (maxBits > 8) {
                     maxBits = 16;
                  }

                  this.IHDR_bitDepth = maxBits;
               }
            }
         } else if (!name.equals("Dimension")) {
            if (name.equals("Document")) {
               for (Node childxxx = node.getFirstChild(); childxxx != null; childxxx = childxxx.getNextSibling()) {
                  String childName = childxxx.getNodeName();
                  if (childName.equals("ImageModificationTime")) {
                     this.tIME_present = true;
                     this.tIME_year = this.getIntAttribute(childxxx, "year");
                     this.tIME_month = this.getIntAttribute(childxxx, "month");
                     this.tIME_day = this.getIntAttribute(childxxx, "day");
                     this.tIME_hour = this.getIntAttribute(childxxx, "hour", 0, false);
                     this.tIME_minute = this.getIntAttribute(childxxx, "minute", 0, false);
                     this.tIME_second = this.getIntAttribute(childxxx, "second", 0, false);
                  }
               }
            } else if (name.equals("Text")) {
               for (Node childxxxx = node.getFirstChild(); childxxxx != null; childxxxx = childxxxx.getNextSibling()) {
                  String childName = childxxxx.getNodeName();
                  if (childName.equals("TextEntry")) {
                     String keyword = this.getAttribute(childxxxx, "keyword", "text", false);
                     String value = this.getAttribute(childxxxx, "value");
                     String encoding = this.getAttribute(childxxxx, "encoding", "unknown", false);
                     String language = this.getAttribute(childxxxx, "language", "unknown", false);
                     String compression = this.getAttribute(childxxxx, "compression", "other", false);
                     if (this.isISOLatin(value)) {
                        if (compression.equals("zip")) {
                           this.zTXt_keyword.add(toPrintableLatin1(keyword));
                           this.zTXt_text.add(value);
                           this.zTXt_compressionMethod.add(new Integer(0));
                        } else {
                           this.tEXt_keyword.add(toPrintableLatin1(keyword));
                           this.tEXt_text.add(value);
                        }
                     } else {
                        int flag = compression.equals("zip") ? 1 : 0;
                        this.iTXt_keyword.add(toPrintableLatin1(keyword));
                        this.iTXt_compressionFlag.add(new Integer(flag));
                        this.iTXt_compressionMethod.add(new Integer(0));
                        this.iTXt_languageTag.add(language);
                        this.iTXt_translatedKeyword.add(keyword);
                        this.iTXt_text.add(value);
                     }
                  }
               }
            }
         } else {
            boolean gotWidth = false;
            boolean gotHeight = false;
            boolean gotAspectRatio = false;
            float width = -1.0F;
            float height = -1.0F;
            float aspectRatio = -1.0F;

            for (Node childxxxxx = node.getFirstChild(); childxxxxx != null; childxxxxx = childxxxxx.getNextSibling()) {
               String childName = childxxxxx.getNodeName();
               if (childName.equals("PixelAspectRatio")) {
                  aspectRatio = this.getFloatAttribute(childxxxxx, "value");
                  gotAspectRatio = true;
               } else if (childName.equals("HorizontalPixelSize")) {
                  width = this.getFloatAttribute(childxxxxx, "value");
                  gotWidth = true;
               } else if (childName.equals("VerticalPixelSize")) {
                  height = this.getFloatAttribute(childxxxxx, "value");
                  gotHeight = true;
               }
            }

            if (gotWidth && gotHeight) {
               this.pHYs_present = true;
               this.pHYs_unitSpecifier = 1;
               this.pHYs_pixelsPerUnitXAxis = (int)(1000.0F / width + 0.5F);
               this.pHYs_pixelsPerUnitYAxis = (int)(1000.0F / height + 0.5F);
            } else if (gotAspectRatio) {
               this.pHYs_present = true;
               this.pHYs_unitSpecifier = 0;

               int denom;
               for (denom = 1; denom < 100; denom++) {
                  int num = (int)(aspectRatio * (float)denom);
                  if ((double)Math.abs((float)(num / denom) - aspectRatio) < 0.001) {
                     break;
                  }
               }

               this.pHYs_pixelsPerUnitXAxis = (int)(aspectRatio * (float)denom);
               this.pHYs_pixelsPerUnitYAxis = denom;
            }
         }
      }
   }

   @Override
   public void reset() {
      this.IHDR_present = false;
      this.PLTE_present = false;
      this.bKGD_present = false;
      this.cHRM_present = false;
      this.gAMA_present = false;
      this.hIST_present = false;
      this.iCCP_present = false;
      this.iTXt_keyword = new ArrayList();
      this.iTXt_compressionFlag = new ArrayList();
      this.iTXt_compressionMethod = new ArrayList();
      this.iTXt_languageTag = new ArrayList();
      this.iTXt_translatedKeyword = new ArrayList();
      this.iTXt_text = new ArrayList();
      this.pHYs_present = false;
      this.sBIT_present = false;
      this.sPLT_present = false;
      this.sRGB_present = false;
      this.tEXt_keyword = new ArrayList();
      this.tEXt_text = new ArrayList();
      this.tIME_present = false;
      this.tRNS_present = false;
      this.zTXt_keyword = new ArrayList();
      this.zTXt_compressionMethod = new ArrayList();
      this.zTXt_text = new ArrayList();
      this.unknownChunkType = new ArrayList();
      this.unknownChunkData = new ArrayList();
   }

   private static int chunkType(String typeString) {
      char c0 = typeString.charAt(0);
      char c1 = typeString.charAt(1);
      char c2 = typeString.charAt(2);
      char c3 = typeString.charAt(3);
      return c0 << 24 | c1 << 16 | c2 << 8 | c3;
   }

   private String readNullTerminatedString(ImageInputStream stream) throws IOException {
      StringBuffer b = new StringBuffer();

      int c;
      while ((c = stream.read()) != 0) {
         b.append((char)c);
      }

      return b.toString();
   }
}
