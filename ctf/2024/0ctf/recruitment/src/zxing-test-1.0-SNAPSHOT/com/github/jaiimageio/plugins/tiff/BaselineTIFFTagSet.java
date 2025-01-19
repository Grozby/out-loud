package com.github.jaiimageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

public class BaselineTIFFTagSet extends TIFFTagSet {
   private static BaselineTIFFTagSet theInstance = null;
   public static final int TAG_NEW_SUBFILE_TYPE = 254;
   public static final int NEW_SUBFILE_TYPE_REDUCED_RESOLUTION = 1;
   public static final int NEW_SUBFILE_TYPE_SINGLE_PAGE = 2;
   public static final int NEW_SUBFILE_TYPE_TRANSPARENCY = 4;
   public static final int TAG_SUBFILE_TYPE = 255;
   public static final int SUBFILE_TYPE_FULL_RESOLUTION = 1;
   public static final int SUBFILE_TYPE_REDUCED_RESOLUTION = 2;
   public static final int SUBFILE_TYPE_SINGLE_PAGE = 3;
   public static final int TAG_IMAGE_WIDTH = 256;
   public static final int TAG_IMAGE_LENGTH = 257;
   public static final int TAG_BITS_PER_SAMPLE = 258;
   public static final int TAG_COMPRESSION = 259;
   public static final int COMPRESSION_NONE = 1;
   public static final int COMPRESSION_CCITT_RLE = 2;
   public static final int COMPRESSION_CCITT_T_4 = 3;
   public static final int COMPRESSION_CCITT_T_6 = 4;
   public static final int COMPRESSION_LZW = 5;
   public static final int COMPRESSION_OLD_JPEG = 6;
   public static final int COMPRESSION_JPEG = 7;
   public static final int COMPRESSION_ZLIB = 8;
   public static final int COMPRESSION_PACKBITS = 32773;
   public static final int COMPRESSION_DEFLATE = 32946;
   public static final int TAG_PHOTOMETRIC_INTERPRETATION = 262;
   public static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;
   public static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;
   public static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;
   public static final int PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR = 3;
   public static final int PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK = 4;
   public static final int PHOTOMETRIC_INTERPRETATION_CMYK = 5;
   public static final int PHOTOMETRIC_INTERPRETATION_Y_CB_CR = 6;
   public static final int PHOTOMETRIC_INTERPRETATION_CIELAB = 8;
   public static final int PHOTOMETRIC_INTERPRETATION_ICCLAB = 9;
   public static final int TAG_THRESHHOLDING = 263;
   public static final int THRESHHOLDING_NONE = 1;
   public static final int THRESHHOLDING_ORDERED_DITHER = 2;
   public static final int THRESHHOLDING_RANDOMIZED_DITHER = 3;
   public static final int TAG_CELL_WIDTH = 264;
   public static final int TAG_CELL_LENGTH = 265;
   public static final int TAG_FILL_ORDER = 266;
   public static final int FILL_ORDER_LEFT_TO_RIGHT = 1;
   public static final int FILL_ORDER_RIGHT_TO_LEFT = 2;
   public static final int TAG_DOCUMENT_NAME = 269;
   public static final int TAG_IMAGE_DESCRIPTION = 270;
   public static final int TAG_MAKE = 271;
   public static final int TAG_MODEL = 272;
   public static final int TAG_STRIP_OFFSETS = 273;
   public static final int TAG_ORIENTATION = 274;
   public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_LEFT = 1;
   public static final int ORIENTATION_ROW_0_TOP_COLUMN_0_RIGHT = 2;
   public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_RIGHT = 3;
   public static final int ORIENTATION_ROW_0_BOTTOM_COLUMN_0_LEFT = 4;
   public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_TOP = 5;
   public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_TOP = 6;
   public static final int ORIENTATION_ROW_0_RIGHT_COLUMN_0_BOTTOM = 7;
   public static final int ORIENTATION_ROW_0_LEFT_COLUMN_0_BOTTOM = 8;
   public static final int TAG_SAMPLES_PER_PIXEL = 277;
   public static final int TAG_ROWS_PER_STRIP = 278;
   public static final int TAG_STRIP_BYTE_COUNTS = 279;
   public static final int TAG_MIN_SAMPLE_VALUE = 280;
   public static final int TAG_MAX_SAMPLE_VALUE = 281;
   public static final int TAG_X_RESOLUTION = 282;
   public static final int TAG_Y_RESOLUTION = 283;
   public static final int TAG_PLANAR_CONFIGURATION = 284;
   public static final int PLANAR_CONFIGURATION_CHUNKY = 1;
   public static final int PLANAR_CONFIGURATION_PLANAR = 2;
   public static final int TAG_PAGE_NAME = 285;
   public static final int TAG_X_POSITION = 286;
   public static final int TAG_Y_POSITION = 287;
   public static final int TAG_FREE_OFFSETS = 288;
   public static final int TAG_FREE_BYTE_COUNTS = 289;
   public static final int TAG_GRAY_RESPONSE_UNIT = 290;
   public static final int GRAY_RESPONSE_UNIT_TENTHS = 1;
   public static final int GRAY_RESPONSE_UNIT_HUNDREDTHS = 2;
   public static final int GRAY_RESPONSE_UNIT_THOUSANDTHS = 3;
   public static final int GRAY_RESPONSE_UNIT_TEN_THOUSANDTHS = 4;
   public static final int GRAY_RESPONSE_UNIT_HUNDRED_THOUSANDTHS = 5;
   public static final int TAG_GRAY_RESPONSE_CURVE = 291;
   public static final int TAG_T4_OPTIONS = 292;
   public static final int T4_OPTIONS_2D_CODING = 1;
   public static final int T4_OPTIONS_UNCOMPRESSED = 2;
   public static final int T4_OPTIONS_EOL_BYTE_ALIGNED = 4;
   public static final int TAG_T6_OPTIONS = 293;
   public static final int T6_OPTIONS_UNCOMPRESSED = 2;
   public static final int TAG_RESOLUTION_UNIT = 296;
   public static final int RESOLUTION_UNIT_NONE = 1;
   public static final int RESOLUTION_UNIT_INCH = 2;
   public static final int RESOLUTION_UNIT_CENTIMETER = 3;
   public static final int TAG_PAGE_NUMBER = 297;
   public static final int TAG_TRANSFER_FUNCTION = 301;
   public static final int TAG_SOFTWARE = 305;
   public static final int TAG_DATE_TIME = 306;
   public static final int TAG_ARTIST = 315;
   public static final int TAG_HOST_COMPUTER = 316;
   public static final int TAG_PREDICTOR = 317;
   public static final int PREDICTOR_NONE = 1;
   public static final int PREDICTOR_HORIZONTAL_DIFFERENCING = 2;
   public static final int TAG_WHITE_POINT = 318;
   public static final int TAG_PRIMARY_CHROMATICITES = 319;
   public static final int TAG_COLOR_MAP = 320;
   public static final int TAG_HALFTONE_HINTS = 321;
   public static final int TAG_TILE_WIDTH = 322;
   public static final int TAG_TILE_LENGTH = 323;
   public static final int TAG_TILE_OFFSETS = 324;
   public static final int TAG_TILE_BYTE_COUNTS = 325;
   public static final int TAG_INK_SET = 332;
   public static final int INK_SET_CMYK = 1;
   public static final int INK_SET_NOT_CMYK = 2;
   public static final int TAG_INK_NAMES = 333;
   public static final int TAG_NUMBER_OF_INKS = 334;
   public static final int TAG_DOT_RANGE = 336;
   public static final int TAG_TARGET_PRINTER = 337;
   public static final int TAG_EXTRA_SAMPLES = 338;
   public static final int EXTRA_SAMPLES_UNSPECIFIED = 0;
   public static final int EXTRA_SAMPLES_ASSOCIATED_ALPHA = 1;
   public static final int EXTRA_SAMPLES_UNASSOCIATED_ALPHA = 2;
   public static final int TAG_SAMPLE_FORMAT = 339;
   public static final int SAMPLE_FORMAT_UNSIGNED_INTEGER = 1;
   public static final int SAMPLE_FORMAT_SIGNED_INTEGER = 2;
   public static final int SAMPLE_FORMAT_FLOATING_POINT = 3;
   public static final int SAMPLE_FORMAT_UNDEFINED = 4;
   public static final int TAG_S_MIN_SAMPLE_VALUE = 340;
   public static final int TAG_S_MAX_SAMPLE_VALUE = 341;
   public static final int TAG_TRANSFER_RANGE = 342;
   public static final int TAG_JPEG_TABLES = 347;
   public static final int TAG_JPEG_PROC = 512;
   public static final int JPEG_PROC_BASELINE = 1;
   public static final int JPEG_PROC_LOSSLESS = 14;
   public static final int TAG_JPEG_INTERCHANGE_FORMAT = 513;
   public static final int TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = 514;
   public static final int TAG_JPEG_RESTART_INTERVAL = 515;
   public static final int TAG_JPEG_LOSSLESS_PREDICTORS = 517;
   public static final int TAG_JPEG_POINT_TRANSFORMS = 518;
   public static final int TAG_JPEG_Q_TABLES = 519;
   public static final int TAG_JPEG_DC_TABLES = 520;
   public static final int TAG_JPEG_AC_TABLES = 521;
   public static final int TAG_Y_CB_CR_COEFFICIENTS = 529;
   public static final int TAG_Y_CB_CR_SUBSAMPLING = 530;
   public static final int TAG_Y_CB_CR_POSITIONING = 531;
   public static final int Y_CB_CR_POSITIONING_CENTERED = 1;
   public static final int Y_CB_CR_POSITIONING_COSITED = 2;
   public static final int TAG_REFERENCE_BLACK_WHITE = 532;
   public static final int TAG_COPYRIGHT = 33432;
   public static final int TAG_ICC_PROFILE = 34675;
   private static List tags;

   private static void initTags() {
      tags = new ArrayList(76);
      tags.add(new BaselineTIFFTagSet.Artist());
      tags.add(new BaselineTIFFTagSet.BitsPerSample());
      tags.add(new BaselineTIFFTagSet.CellLength());
      tags.add(new BaselineTIFFTagSet.CellWidth());
      tags.add(new BaselineTIFFTagSet.ColorMap());
      tags.add(new BaselineTIFFTagSet.Compression());
      tags.add(new BaselineTIFFTagSet.Copyright());
      tags.add(new BaselineTIFFTagSet.DateTime());
      tags.add(new BaselineTIFFTagSet.DocumentName());
      tags.add(new BaselineTIFFTagSet.DotRange());
      tags.add(new BaselineTIFFTagSet.ExtraSamples());
      tags.add(new BaselineTIFFTagSet.FillOrder());
      tags.add(new BaselineTIFFTagSet.FreeByteCounts());
      tags.add(new BaselineTIFFTagSet.FreeOffsets());
      tags.add(new BaselineTIFFTagSet.GrayResponseCurve());
      tags.add(new BaselineTIFFTagSet.GrayResponseUnit());
      tags.add(new BaselineTIFFTagSet.HalftoneHints());
      tags.add(new BaselineTIFFTagSet.HostComputer());
      tags.add(new BaselineTIFFTagSet.ImageDescription());
      tags.add(new BaselineTIFFTagSet.ICCProfile());
      tags.add(new BaselineTIFFTagSet.ImageLength());
      tags.add(new BaselineTIFFTagSet.ImageWidth());
      tags.add(new BaselineTIFFTagSet.InkNames());
      tags.add(new BaselineTIFFTagSet.InkSet());
      tags.add(new BaselineTIFFTagSet.JPEGACTables());
      tags.add(new BaselineTIFFTagSet.JPEGDCTables());
      tags.add(new BaselineTIFFTagSet.JPEGInterchangeFormat());
      tags.add(new BaselineTIFFTagSet.JPEGInterchangeFormatLength());
      tags.add(new BaselineTIFFTagSet.JPEGLosslessPredictors());
      tags.add(new BaselineTIFFTagSet.JPEGPointTransforms());
      tags.add(new BaselineTIFFTagSet.JPEGProc());
      tags.add(new BaselineTIFFTagSet.JPEGQTables());
      tags.add(new BaselineTIFFTagSet.JPEGRestartInterval());
      tags.add(new BaselineTIFFTagSet.JPEGTables());
      tags.add(new BaselineTIFFTagSet.Make());
      tags.add(new BaselineTIFFTagSet.MaxSampleValue());
      tags.add(new BaselineTIFFTagSet.MinSampleValue());
      tags.add(new BaselineTIFFTagSet.Model());
      tags.add(new BaselineTIFFTagSet.NewSubfileType());
      tags.add(new BaselineTIFFTagSet.NumberOfInks());
      tags.add(new BaselineTIFFTagSet.Orientation());
      tags.add(new BaselineTIFFTagSet.PageName());
      tags.add(new BaselineTIFFTagSet.PageNumber());
      tags.add(new BaselineTIFFTagSet.PhotometricInterpretation());
      tags.add(new BaselineTIFFTagSet.PlanarConfiguration());
      tags.add(new BaselineTIFFTagSet.Predictor());
      tags.add(new BaselineTIFFTagSet.PrimaryChromaticities());
      tags.add(new BaselineTIFFTagSet.ReferenceBlackWhite());
      tags.add(new BaselineTIFFTagSet.ResolutionUnit());
      tags.add(new BaselineTIFFTagSet.RowsPerStrip());
      tags.add(new BaselineTIFFTagSet.SampleFormat());
      tags.add(new BaselineTIFFTagSet.SamplesPerPixel());
      tags.add(new BaselineTIFFTagSet.SMaxSampleValue());
      tags.add(new BaselineTIFFTagSet.SMinSampleValue());
      tags.add(new BaselineTIFFTagSet.Software());
      tags.add(new BaselineTIFFTagSet.StripByteCounts());
      tags.add(new BaselineTIFFTagSet.StripOffsets());
      tags.add(new BaselineTIFFTagSet.SubfileType());
      tags.add(new BaselineTIFFTagSet.T4Options());
      tags.add(new BaselineTIFFTagSet.T6Options());
      tags.add(new BaselineTIFFTagSet.TargetPrinter());
      tags.add(new BaselineTIFFTagSet.Threshholding());
      tags.add(new BaselineTIFFTagSet.TileByteCounts());
      tags.add(new BaselineTIFFTagSet.TileOffsets());
      tags.add(new BaselineTIFFTagSet.TileLength());
      tags.add(new BaselineTIFFTagSet.TileWidth());
      tags.add(new BaselineTIFFTagSet.TransferFunction());
      tags.add(new BaselineTIFFTagSet.TransferRange());
      tags.add(new BaselineTIFFTagSet.WhitePoint());
      tags.add(new BaselineTIFFTagSet.XPosition());
      tags.add(new BaselineTIFFTagSet.XResolution());
      tags.add(new BaselineTIFFTagSet.YCbCrCoefficients());
      tags.add(new BaselineTIFFTagSet.YCbCrPositioning());
      tags.add(new BaselineTIFFTagSet.YCbCrSubSampling());
      tags.add(new BaselineTIFFTagSet.YPosition());
      tags.add(new BaselineTIFFTagSet.YResolution());
   }

   private BaselineTIFFTagSet() {
      super(tags);
   }

   public static synchronized BaselineTIFFTagSet getInstance() {
      if (theInstance == null) {
         initTags();
         theInstance = new BaselineTIFFTagSet();
         tags = null;
      }

      return theInstance;
   }

   static class Artist extends TIFFTag {
      public Artist() {
         super("Artist", 315, 4);
      }
   }

   static class BitsPerSample extends TIFFTag {
      public BitsPerSample() {
         super("BitsPerSample", 258, 8);
      }
   }

   static class CellLength extends TIFFTag {
      public CellLength() {
         super("CellLength", 265, 8);
      }
   }

   static class CellWidth extends TIFFTag {
      public CellWidth() {
         super("CellWidth", 264, 8);
      }
   }

   static class ColorMap extends TIFFTag {
      public ColorMap() {
         super("ColorMap", 320, 8);
      }
   }

   static class Compression extends TIFFTag {
      public Compression() {
         super("Compression", 259, 8);
         this.addValueName(1, "Uncompressed");
         this.addValueName(2, "CCITT RLE");
         this.addValueName(3, "CCITT T.4");
         this.addValueName(4, "CCITT T.6");
         this.addValueName(5, "LZW");
         this.addValueName(6, "Old JPEG");
         this.addValueName(7, "JPEG");
         this.addValueName(8, "ZLib");
         this.addValueName(32773, "PackBits");
         this.addValueName(32946, "Deflate");
      }
   }

   static class Copyright extends TIFFTag {
      public Copyright() {
         super("Copyright", 33432, 4);
      }
   }

   static class DateTime extends TIFFTag {
      public DateTime() {
         super("DateTime", 306, 4);
      }
   }

   static class DocumentName extends TIFFTag {
      public DocumentName() {
         super("DocumentName", 269, 4);
      }
   }

   static class DotRange extends TIFFTag {
      public DotRange() {
         super("DotRange", 336, 10);
      }
   }

   static class ExtraSamples extends TIFFTag {
      public ExtraSamples() {
         super("ExtraSamples", 338, 8);
         this.addValueName(0, "Unspecified");
         this.addValueName(1, "Associated Alpha");
         this.addValueName(2, "Unassociated Alpha");
      }
   }

   static class FillOrder extends TIFFTag {
      public FillOrder() {
         super("FillOrder", 266, 8);
         this.addValueName(1, "LeftToRight");
         this.addValueName(2, "RightToLeft");
      }
   }

   static class FreeByteCounts extends TIFFTag {
      public FreeByteCounts() {
         super("FreeByteCounts", 289, 16);
      }
   }

   static class FreeOffsets extends TIFFTag {
      public FreeOffsets() {
         super("FreeOffsets", 288, 16);
      }
   }

   static class GrayResponseCurve extends TIFFTag {
      public GrayResponseCurve() {
         super("GrayResponseCurve", 291, 8);
      }
   }

   static class GrayResponseUnit extends TIFFTag {
      public GrayResponseUnit() {
         super("GrayResponseUnit", 290, 8);
         this.addValueName(1, "Tenths");
         this.addValueName(2, "Hundredths");
         this.addValueName(3, "Thousandths");
         this.addValueName(4, "Ten-Thousandths");
         this.addValueName(5, "Hundred-Thousandths");
      }
   }

   static class HalftoneHints extends TIFFTag {
      public HalftoneHints() {
         super("HalftoneHints", 321, 8);
      }
   }

   static class HostComputer extends TIFFTag {
      public HostComputer() {
         super("HostComputer", 316, 4);
      }
   }

   static class ICCProfile extends TIFFTag {
      public ICCProfile() {
         super("ICC Profile", 34675, 128);
      }
   }

   static class ImageDescription extends TIFFTag {
      public ImageDescription() {
         super("ImageDescription", 270, 4);
      }
   }

   static class ImageLength extends TIFFTag {
      public ImageLength() {
         super("ImageLength", 257, 24);
      }
   }

   static class ImageWidth extends TIFFTag {
      public ImageWidth() {
         super("ImageWidth", 256, 24);
      }
   }

   static class InkNames extends TIFFTag {
      public InkNames() {
         super("InkNames", 333, 4);
      }
   }

   static class InkSet extends TIFFTag {
      public InkSet() {
         super("InkSet", 332, 8);
         this.addValueName(1, "CMYK");
         this.addValueName(2, "Not CMYK");
      }
   }

   static class JPEGACTables extends TIFFTag {
      public JPEGACTables() {
         super("JPEGACTables", 521, 16);
      }
   }

   static class JPEGDCTables extends TIFFTag {
      public JPEGDCTables() {
         super("JPEGDCTables", 520, 16);
      }
   }

   static class JPEGInterchangeFormat extends TIFFTag {
      public JPEGInterchangeFormat() {
         super("JPEGInterchangeFormat", 513, 16);
      }
   }

   static class JPEGInterchangeFormatLength extends TIFFTag {
      public JPEGInterchangeFormatLength() {
         super("JPEGInterchangeFormatLength", 514, 16);
      }
   }

   static class JPEGLosslessPredictors extends TIFFTag {
      public JPEGLosslessPredictors() {
         super("JPEGLosslessPredictors", 517, 8);
         this.addValueName(1, "A");
         this.addValueName(2, "B");
         this.addValueName(3, "C");
         this.addValueName(4, "A+B-C");
         this.addValueName(5, "A+((B-C)/2)");
         this.addValueName(6, "B+((A-C)/2)");
         this.addValueName(7, "(A+B)/2");
      }
   }

   static class JPEGPointTransforms extends TIFFTag {
      public JPEGPointTransforms() {
         super("JPEGPointTransforms", 518, 8);
      }
   }

   static class JPEGProc extends TIFFTag {
      public JPEGProc() {
         super("JPEGProc", 512, 8);
         this.addValueName(1, "Baseline sequential process");
         this.addValueName(14, "Lossless process with Huffman coding");
      }
   }

   static class JPEGQTables extends TIFFTag {
      public JPEGQTables() {
         super("JPEGQTables", 519, 16);
      }
   }

   static class JPEGRestartInterval extends TIFFTag {
      public JPEGRestartInterval() {
         super("JPEGRestartInterval", 515, 8);
      }
   }

   static class JPEGTables extends TIFFTag {
      public JPEGTables() {
         super("JPEGTables", 347, 128);
      }
   }

   static class Make extends TIFFTag {
      public Make() {
         super("Make", 271, 4);
      }
   }

   static class MaxSampleValue extends TIFFTag {
      public MaxSampleValue() {
         super("MaxSampleValue", 281, 8);
      }
   }

   static class MinSampleValue extends TIFFTag {
      public MinSampleValue() {
         super("MinSampleValue", 280, 8);
      }
   }

   static class Model extends TIFFTag {
      public Model() {
         super("Model", 272, 4);
      }
   }

   static class NewSubfileType extends TIFFTag {
      public NewSubfileType() {
         super("NewSubfileType", 254, 16);
         this.addValueName(0, "Default");
         this.addValueName(1, "ReducedResolution");
         this.addValueName(2, "SinglePage");
         this.addValueName(3, "SinglePage+ReducedResolution");
         this.addValueName(4, "Transparency");
         this.addValueName(5, "Transparency+ReducedResolution");
         this.addValueName(6, "Transparency+SinglePage");
         this.addValueName(7, "Transparency+SinglePage+ReducedResolution");
      }
   }

   static class NumberOfInks extends TIFFTag {
      public NumberOfInks() {
         super("NumberOfInks", 334, 8);
      }
   }

   static class Orientation extends TIFFTag {
      public Orientation() {
         super("Orientation", 274, 8);
         this.addValueName(1, "Row 0=Top, Column 0=Left");
         this.addValueName(2, "Row 0=Top, Column 0=Right");
         this.addValueName(3, "Row 0=Bottom, Column 0=Right");
         this.addValueName(4, "Row 0=Bottom, Column 0=Left");
         this.addValueName(5, "Row 0=Left, Column 0=Top");
         this.addValueName(6, "Row 0=Right, Column 0=Top");
         this.addValueName(7, "Row 0=Right, Column 0=Bottom");
      }
   }

   static class PageName extends TIFFTag {
      public PageName() {
         super("PageName", 285, 4);
      }
   }

   static class PageNumber extends TIFFTag {
      public PageNumber() {
         super("PageNumber", 297, 8);
      }
   }

   static class PhotometricInterpretation extends TIFFTag {
      public PhotometricInterpretation() {
         super("PhotometricInterpretation", 262, 8);
         this.addValueName(0, "WhiteIsZero");
         this.addValueName(1, "BlackIsZero");
         this.addValueName(2, "RGB");
         this.addValueName(3, "Palette Color");
         this.addValueName(4, "Transparency Mask");
         this.addValueName(5, "CMYK");
         this.addValueName(6, "YCbCr");
         this.addValueName(8, "CIELAB");
         this.addValueName(9, "ICCLAB");
      }
   }

   static class PlanarConfiguration extends TIFFTag {
      public PlanarConfiguration() {
         super("PlanarConfiguration", 284, 8);
         this.addValueName(1, "Chunky");
         this.addValueName(2, "Planar");
      }
   }

   static class Predictor extends TIFFTag {
      public Predictor() {
         super("Predictor", 317, 8);
         this.addValueName(1, "None");
         this.addValueName(2, "Horizontal Differencing");
      }
   }

   static class PrimaryChromaticities extends TIFFTag {
      public PrimaryChromaticities() {
         super("PrimaryChromaticities", 319, 32);
      }
   }

   static class ReferenceBlackWhite extends TIFFTag {
      public ReferenceBlackWhite() {
         super("ReferenceBlackWhite", 532, 32);
      }
   }

   static class ResolutionUnit extends TIFFTag {
      public ResolutionUnit() {
         super("ResolutionUnit", 296, 8);
         this.addValueName(1, "None");
         this.addValueName(2, "Inch");
         this.addValueName(3, "Centimeter");
      }
   }

   static class RowsPerStrip extends TIFFTag {
      public RowsPerStrip() {
         super("RowsPerStrip", 278, 24);
      }
   }

   static class SMaxSampleValue extends TIFFTag {
      public SMaxSampleValue() {
         super("SMaxSampleValue", 341, 8058);
      }
   }

   static class SMinSampleValue extends TIFFTag {
      public SMinSampleValue() {
         super("SMinSampleValue", 340, 8058);
      }
   }

   static class SampleFormat extends TIFFTag {
      public SampleFormat() {
         super("SampleFormat", 339, 8);
         this.addValueName(1, "Unsigned Integer");
         this.addValueName(2, "Signed Integer");
         this.addValueName(3, "Floating Point");
         this.addValueName(4, "Undefined");
      }
   }

   static class SamplesPerPixel extends TIFFTag {
      public SamplesPerPixel() {
         super("SamplesPerPixel", 277, 8);
      }
   }

   static class Software extends TIFFTag {
      public Software() {
         super("Software", 305, 4);
      }
   }

   static class StripByteCounts extends TIFFTag {
      public StripByteCounts() {
         super("StripByteCounts", 279, 24);
      }
   }

   static class StripOffsets extends TIFFTag {
      public StripOffsets() {
         super("StripOffsets", 273, 24);
      }
   }

   static class SubfileType extends TIFFTag {
      public SubfileType() {
         super("SubfileType", 255, 8);
         this.addValueName(1, "FullResolution");
         this.addValueName(2, "ReducedResolution");
         this.addValueName(3, "SinglePage");
      }
   }

   static class T4Options extends TIFFTag {
      public T4Options() {
         super("T4Options", 292, 16);
         this.addValueName(0, "Default 1DCoding");
         this.addValueName(1, "2DCoding");
         this.addValueName(2, "Uncompressed");
         this.addValueName(3, "2DCoding+Uncompressed");
         this.addValueName(4, "EOLByteAligned");
         this.addValueName(5, "2DCoding+EOLByteAligned");
         this.addValueName(6, "Uncompressed+EOLByteAligned");
         this.addValueName(7, "2DCoding+Uncompressed+EOLByteAligned");
      }
   }

   static class T6Options extends TIFFTag {
      public T6Options() {
         super("T6Options", 293, 16);
         this.addValueName(0, "Default");
         this.addValueName(2, "Uncompressed");
      }
   }

   static class TargetPrinter extends TIFFTag {
      public TargetPrinter() {
         super("TargetPrinter", 337, 4);
      }
   }

   static class Threshholding extends TIFFTag {
      public Threshholding() {
         super("Threshholding", 263, 8);
         this.addValueName(1, "None");
         this.addValueName(2, "OrderedDither");
         this.addValueName(3, "RandomizedDither");
      }
   }

   static class TileByteCounts extends TIFFTag {
      public TileByteCounts() {
         super("TileByteCounts", 325, 24);
      }
   }

   static class TileLength extends TIFFTag {
      public TileLength() {
         super("TileLength", 323, 24);
      }
   }

   static class TileOffsets extends TIFFTag {
      public TileOffsets() {
         super("TileOffsets", 324, 16);
      }
   }

   static class TileWidth extends TIFFTag {
      public TileWidth() {
         super("TileWidth", 322, 24);
      }
   }

   static class TransferFunction extends TIFFTag {
      public TransferFunction() {
         super("TransferFunction", 301, 8);
      }
   }

   static class TransferRange extends TIFFTag {
      public TransferRange() {
         super("TransferRange", 342, 8);
      }
   }

   static class WhitePoint extends TIFFTag {
      public WhitePoint() {
         super("WhitePoint", 318, 32);
      }
   }

   static class XPosition extends TIFFTag {
      public XPosition() {
         super("XPosition", 286, 32);
      }
   }

   static class XResolution extends TIFFTag {
      public XResolution() {
         super("XResolution", 282, 32);
      }
   }

   static class YCbCrCoefficients extends TIFFTag {
      public YCbCrCoefficients() {
         super("YCbCrCoefficients", 529, 32);
      }
   }

   static class YCbCrPositioning extends TIFFTag {
      public YCbCrPositioning() {
         super("YCbCrPositioning", 531, 8);
         this.addValueName(1, "Centered");
         this.addValueName(2, "Cosited");
      }
   }

   static class YCbCrSubSampling extends TIFFTag {
      public YCbCrSubSampling() {
         super("YCbCrSubSampling", 530, 8);
      }
   }

   static class YPosition extends TIFFTag {
      public YPosition() {
         super("YPosition", 287, 32);
      }
   }

   static class YResolution extends TIFFTag {
      public YResolution() {
         super("YResolution", 283, 32);
      }
   }
}
