package com.github.jaiimageio.impl.common;

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

public class ImageUtil {
   public static final ColorModel createColorModel(SampleModel sampleModel) {
      if (sampleModel == null) {
         throw new IllegalArgumentException("sampleModel == null!");
      } else {
         int dataType = sampleModel.getDataType();
         switch (dataType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
               ColorModel colorModel = null;
               int[] sampleSize = sampleModel.getSampleSize();
               if (sampleModel instanceof ComponentSampleModel) {
                  int numBands = sampleModel.getNumBands();
                  ColorSpace colorSpace = null;
                  if (numBands <= 2) {
                     colorSpace = ColorSpace.getInstance(1003);
                  } else if (numBands <= 4) {
                     colorSpace = ColorSpace.getInstance(1000);
                  } else {
                     colorSpace = new BogusColorSpace(numBands);
                  }

                  boolean hasAlpha = numBands == 2 || numBands == 4;
                  boolean isAlphaPremultiplied = false;
                  int transparency = hasAlpha ? 3 : 1;
                  colorModel = new ComponentColorModel(colorSpace, sampleSize, hasAlpha, isAlphaPremultiplied, transparency, dataType);
               } else {
                  if (sampleModel.getNumBands() <= 4 && sampleModel instanceof SinglePixelPackedSampleModel) {
                     SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel)sampleModel;
                     int[] bitMasks = sppsm.getBitMasks();
                     int rmask = 0;
                     int gmask = 0;
                     int bmask = 0;
                     int amask = 0;
                     int numBands = bitMasks.length;
                     if (numBands <= 2) {
                        rmask = gmask = bmask = bitMasks[0];
                        if (numBands == 2) {
                           amask = bitMasks[1];
                        }
                     } else {
                        rmask = bitMasks[0];
                        gmask = bitMasks[1];
                        bmask = bitMasks[2];
                        if (numBands == 4) {
                           amask = bitMasks[3];
                        }
                     }

                     int bits = 0;

                     for (int i = 0; i < sampleSize.length; i++) {
                        bits += sampleSize[i];
                     }

                     return new DirectColorModel(bits, rmask, gmask, bmask, amask);
                  }

                  if (sampleModel instanceof MultiPixelPackedSampleModel) {
                     int bitsPerSample = sampleSize[0];
                     int numEntries = 1 << bitsPerSample;
                     byte[] map = new byte[numEntries];

                     for (int i = 0; i < numEntries; i++) {
                        map[i] = (byte)(i * 255 / (numEntries - 1));
                     }

                     colorModel = new IndexColorModel(bitsPerSample, numEntries, map, map, map);
                  }
               }

               return colorModel;
            default:
               return null;
         }
      }
   }

   public static byte[] getPackedBinaryData(Raster raster, Rectangle rect) {
      SampleModel sm = raster.getSampleModel();
      if (!isBinary(sm)) {
         throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
      } else {
         int rectX = rect.x;
         int rectY = rect.y;
         int rectWidth = rect.width;
         int rectHeight = rect.height;
         DataBuffer dataBuffer = raster.getDataBuffer();
         int dx = rectX - raster.getSampleModelTranslateX();
         int dy = rectY - raster.getSampleModelTranslateY();
         MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
         int lineStride = mpp.getScanlineStride();
         int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
         int bitOffset = mpp.getBitOffset(dx);
         int numBytesPerRow = (rectWidth + 7) / 8;
         if (dataBuffer instanceof DataBufferByte
            && eltOffset == 0
            && bitOffset == 0
            && numBytesPerRow == lineStride
            && ((DataBufferByte)dataBuffer).getData().length == numBytesPerRow * rectHeight) {
            return ((DataBufferByte)dataBuffer).getData();
         } else {
            byte[] binaryDataArray = new byte[numBytesPerRow * rectHeight];
            int b = 0;
            if (bitOffset == 0) {
               if (dataBuffer instanceof DataBufferByte) {
                  byte[] data = ((DataBufferByte)dataBuffer).getData();
                  int stride = numBytesPerRow;
                  int offset = 0;

                  for (int y = 0; y < rectHeight; y++) {
                     System.arraycopy(data, eltOffset, binaryDataArray, offset, stride);
                     offset += stride;
                     eltOffset += lineStride;
                  }
               } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
                  if (dataBuffer instanceof DataBufferInt) {
                     int[] data = ((DataBufferInt)dataBuffer).getData();

                     for (int y = 0; y < rectHeight; y++) {
                        int xRemaining = rectWidth;

                        int i;
                        for (i = eltOffset; xRemaining > 24; xRemaining -= 32) {
                           int datum = data[i++];
                           binaryDataArray[b++] = (byte)(datum >>> 24 & 0xFF);
                           binaryDataArray[b++] = (byte)(datum >>> 16 & 0xFF);
                           binaryDataArray[b++] = (byte)(datum >>> 8 & 0xFF);
                           binaryDataArray[b++] = (byte)(datum & 0xFF);
                        }

                        for (int shift = 24; xRemaining > 0; xRemaining -= 8) {
                           binaryDataArray[b++] = (byte)(data[i] >>> shift & 0xFF);
                           shift -= 8;
                        }

                        eltOffset += lineStride;
                     }
                  }
               } else {
                  short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();

                  for (int y = 0; y < rectHeight; y++) {
                     int xRemaining = rectWidth;

                     int i;
                     for (i = eltOffset; xRemaining > 8; xRemaining -= 16) {
                        short datum = data[i++];
                        binaryDataArray[b++] = (byte)(datum >>> 8 & 0xFF);
                        binaryDataArray[b++] = (byte)(datum & 255);
                     }

                     if (xRemaining > 0) {
                        binaryDataArray[b++] = (byte)(data[i] >>> 8 & 0xFF);
                     }

                     eltOffset += lineStride;
                  }
               }
            } else if (dataBuffer instanceof DataBufferByte) {
               byte[] data = ((DataBufferByte)dataBuffer).getData();
               if ((bitOffset & 7) == 0) {
                  int stride = numBytesPerRow;
                  int offset = 0;

                  for (int y = 0; y < rectHeight; y++) {
                     System.arraycopy(data, eltOffset, binaryDataArray, offset, stride);
                     offset += stride;
                     eltOffset += lineStride;
                  }
               } else {
                  int leftShift = bitOffset & 7;
                  int rightShift = 8 - leftShift;

                  for (int y = 0; y < rectHeight; y++) {
                     int ix = eltOffset;

                     for (int xRemaining = rectWidth; xRemaining > 0; xRemaining -= 8) {
                        if (xRemaining > rightShift) {
                           binaryDataArray[b++] = (byte)((data[ix++] & 255) << leftShift | (data[ix] & 255) >>> rightShift);
                        } else {
                           binaryDataArray[b++] = (byte)((data[ix] & 255) << leftShift);
                        }
                     }

                     eltOffset += lineStride;
                  }
               }
            } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
               if (dataBuffer instanceof DataBufferInt) {
                  int[] data = ((DataBufferInt)dataBuffer).getData();

                  for (int y = 0; y < rectHeight; y++) {
                     int bOffset = bitOffset;

                     for (int x = 0; x < rectWidth; bOffset += 8) {
                        int ix = eltOffset + bOffset / 32;
                        int mod = bOffset % 32;
                        int left = data[ix];
                        if (mod <= 24) {
                           binaryDataArray[b++] = (byte)(left >>> 24 - mod);
                        } else {
                           int delta = mod - 24;
                           int right = data[ix + 1];
                           binaryDataArray[b++] = (byte)(left << delta | right >>> 32 - delta);
                        }

                        x += 8;
                     }

                     eltOffset += lineStride;
                  }
               }
            } else {
               short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();

               for (int y = 0; y < rectHeight; y++) {
                  int bOffset = bitOffset;

                  for (int x = 0; x < rectWidth; bOffset += 8) {
                     int ix = eltOffset + bOffset / 16;
                     int mod = bOffset % 16;
                     int left = data[ix] & '\uffff';
                     if (mod <= 8) {
                        binaryDataArray[b++] = (byte)(left >>> 8 - mod);
                     } else {
                        int delta = mod - 8;
                        int right = data[ix + 1] & '\uffff';
                        binaryDataArray[b++] = (byte)(left << delta | right >>> 16 - delta);
                     }

                     x += 8;
                  }

                  eltOffset += lineStride;
               }
            }

            return binaryDataArray;
         }
      }
   }

   public static byte[] getUnpackedBinaryData(Raster raster, Rectangle rect) {
      SampleModel sm = raster.getSampleModel();
      if (!isBinary(sm)) {
         throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
      } else {
         int rectX = rect.x;
         int rectY = rect.y;
         int rectWidth = rect.width;
         int rectHeight = rect.height;
         DataBuffer dataBuffer = raster.getDataBuffer();
         int dx = rectX - raster.getSampleModelTranslateX();
         int dy = rectY - raster.getSampleModelTranslateY();
         MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
         int lineStride = mpp.getScanlineStride();
         int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
         int bitOffset = mpp.getBitOffset(dx);
         byte[] bdata = new byte[rectWidth * rectHeight];
         int maxY = rectY + rectHeight;
         int maxX = rectX + rectWidth;
         int k = 0;
         if (dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();

            for (int y = rectY; y < maxY; y++) {
               int bOffset = eltOffset * 8 + bitOffset;

               for (int x = rectX; x < maxX; x++) {
                  byte b = data[bOffset / 8];
                  bdata[k++] = (byte)(b >>> (7 - bOffset & 7) & 1);
                  bOffset++;
               }

               eltOffset += lineStride;
            }
         } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
            if (dataBuffer instanceof DataBufferInt) {
               int[] data = ((DataBufferInt)dataBuffer).getData();

               for (int y = rectY; y < maxY; y++) {
                  int bOffset = eltOffset * 32 + bitOffset;

                  for (int x = rectX; x < maxX; x++) {
                     int i = data[bOffset / 32];
                     bdata[k++] = (byte)(i >>> 31 - bOffset % 32 & 1);
                     bOffset++;
                  }

                  eltOffset += lineStride;
               }
            }
         } else {
            short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();

            for (int y = rectY; y < maxY; y++) {
               int bOffset = eltOffset * 16 + bitOffset;

               for (int x = rectX; x < maxX; x++) {
                  short s = data[bOffset / 16];
                  bdata[k++] = (byte)(s >>> 15 - bOffset % 16 & 1);
                  bOffset++;
               }

               eltOffset += lineStride;
            }
         }

         return bdata;
      }
   }

   public static void setPackedBinaryData(byte[] binaryDataArray, WritableRaster raster, Rectangle rect) {
      SampleModel sm = raster.getSampleModel();
      if (!isBinary(sm)) {
         throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
      } else {
         int rectX = rect.x;
         int rectY = rect.y;
         int rectWidth = rect.width;
         int rectHeight = rect.height;
         DataBuffer dataBuffer = raster.getDataBuffer();
         int dx = rectX - raster.getSampleModelTranslateX();
         int dy = rectY - raster.getSampleModelTranslateY();
         MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
         int lineStride = mpp.getScanlineStride();
         int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
         int bitOffset = mpp.getBitOffset(dx);
         int b = 0;
         if (bitOffset == 0) {
            if (dataBuffer instanceof DataBufferByte) {
               byte[] data = ((DataBufferByte)dataBuffer).getData();
               if (data == binaryDataArray) {
                  return;
               }

               int stride = (rectWidth + 7) / 8;
               int offset = 0;

               for (int y = 0; y < rectHeight; y++) {
                  System.arraycopy(binaryDataArray, offset, data, eltOffset, stride);
                  offset += stride;
                  eltOffset += lineStride;
               }
            } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
               if (dataBuffer instanceof DataBufferInt) {
                  int[] data = ((DataBufferInt)dataBuffer).getData();

                  for (int y = 0; y < rectHeight; y++) {
                     int xRemaining = rectWidth;

                     int i;
                     for (i = eltOffset; xRemaining > 24; xRemaining -= 32) {
                        data[i++] = (binaryDataArray[b++] & 255) << 24
                           | (binaryDataArray[b++] & 255) << 16
                           | (binaryDataArray[b++] & 255) << 8
                           | binaryDataArray[b++] & 255;
                     }

                     for (int shift = 24; xRemaining > 0; xRemaining -= 8) {
                        data[i] |= (binaryDataArray[b++] & 255) << shift;
                        shift -= 8;
                     }

                     eltOffset += lineStride;
                  }
               }
            } else {
               short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();

               for (int y = 0; y < rectHeight; y++) {
                  int xRemaining = rectWidth;

                  int i;
                  for (i = eltOffset; xRemaining > 8; xRemaining -= 16) {
                     data[i++] = (short)((binaryDataArray[b++] & 255) << 8 | binaryDataArray[b++] & 255);
                  }

                  if (xRemaining > 0) {
                     data[i++] = (short)((binaryDataArray[b++] & 255) << 8);
                  }

                  eltOffset += lineStride;
               }
            }
         } else {
            int stride = (rectWidth + 7) / 8;
            int offset = 0;
            if (dataBuffer instanceof DataBufferByte) {
               byte[] data = ((DataBufferByte)dataBuffer).getData();
               if ((bitOffset & 7) == 0) {
                  for (int y = 0; y < rectHeight; y++) {
                     System.arraycopy(binaryDataArray, offset, data, eltOffset, stride);
                     offset += stride;
                     eltOffset += lineStride;
                  }
               } else {
                  int rightShift = bitOffset & 7;
                  int leftShift = 8 - rightShift;
                  int leftShift8 = 8 + leftShift;
                  int mask = (byte)(255 << leftShift);
                  int mask1 = (byte)(~mask);

                  for (int y = 0; y < rectHeight; y++) {
                     int ix = eltOffset;

                     for (int xRemaining = rectWidth; xRemaining > 0; xRemaining -= 8) {
                        byte datum = binaryDataArray[b++];
                        if (xRemaining > leftShift8) {
                           data[ix] = (byte)(data[ix] & mask | (datum & 255) >>> rightShift);
                           ix++;
                           data[ix] = (byte)((datum & 255) << leftShift);
                        } else if (xRemaining > leftShift) {
                           data[ix] = (byte)(data[ix] & mask | (datum & 255) >>> rightShift);
                           data[ix] = (byte)(data[++ix] & mask1 | (datum & 255) << leftShift);
                        } else {
                           int remainMask = (1 << leftShift - xRemaining) - 1;
                           data[ix] = (byte)(data[ix] & (mask | remainMask) | (datum & 255) >>> rightShift & ~remainMask);
                        }
                     }

                     eltOffset += lineStride;
                  }
               }
            } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
               if (dataBuffer instanceof DataBufferInt) {
                  int[] data = ((DataBufferInt)dataBuffer).getData();
                  int rightShift = bitOffset & 7;
                  int leftShift = 8 - rightShift;
                  int leftShift32 = 32 + leftShift;
                  int mask = -1 << leftShift;
                  int mask1 = ~mask;

                  for (int y = 0; y < rectHeight; y++) {
                     int bOffset = bitOffset;
                     int xRemainingx = rectWidth;

                     for (int x = 0; x < rectWidth; xRemainingx -= 8) {
                        int ix = eltOffset + (bOffset >> 5);
                        int mod = bOffset & 31;
                        int datum = binaryDataArray[b++] & 255;
                        if (mod <= 24) {
                           int shift = 24 - mod;
                           if (xRemainingx < 8) {
                              datum &= 255 << 8 - xRemainingx;
                           }

                           data[ix] = data[ix] & ~(255 << shift) | datum << shift;
                        } else if (xRemainingx > leftShift32) {
                           data[ix] = data[ix] & mask | datum >>> rightShift;
                           ix++;
                           data[ix] = datum << leftShift;
                        } else if (xRemainingx > leftShift) {
                           data[ix] = data[ix] & mask | datum >>> rightShift;
                           data[ix] = data[++ix] & mask1 | datum << leftShift;
                        } else {
                           int remainMask = (1 << leftShift - xRemainingx) - 1;
                           data[ix] = data[ix] & (mask | remainMask) | datum >>> rightShift & ~remainMask;
                        }

                        x += 8;
                        bOffset += 8;
                     }

                     eltOffset += lineStride;
                  }
               }
            } else {
               short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();
               int rightShift = bitOffset & 7;
               int leftShift = 8 - rightShift;
               int leftShift16 = 16 + leftShift;
               int mask = (short)(~(255 << leftShift));
               int mask1 = (short)(65535 << leftShift);
               int mask2 = (short)(~mask1);

               for (int y = 0; y < rectHeight; y++) {
                  int bOffset = bitOffset;
                  int xRemainingx = rectWidth;

                  for (int x = 0; x < rectWidth; xRemainingx -= 8) {
                     int ix = eltOffset + (bOffset >> 4);
                     int mod = bOffset & 15;
                     int datum = binaryDataArray[b++] & 255;
                     if (mod <= 8) {
                        if (xRemainingx < 8) {
                           datum &= 255 << 8 - xRemainingx;
                        }

                        data[ix] = (short)(data[ix] & mask | datum << leftShift);
                     } else if (xRemainingx > leftShift16) {
                        data[ix] = (short)(data[ix] & mask1 | datum >>> rightShift & 65535);
                        ix++;
                        data[ix] = (short)(datum << leftShift & 65535);
                     } else if (xRemainingx > leftShift) {
                        data[ix] = (short)(data[ix] & mask1 | datum >>> rightShift & 65535);
                        data[ix] = (short)(data[++ix] & mask2 | datum << leftShift & 65535);
                     } else {
                        int remainMask = (1 << leftShift - xRemainingx) - 1;
                        data[ix] = (short)(data[ix] & (mask1 | remainMask) | datum >>> rightShift & 65535 & ~remainMask);
                     }

                     x += 8;
                     bOffset += 8;
                  }

                  eltOffset += lineStride;
               }
            }
         }
      }
   }

   public static void setUnpackedBinaryData(byte[] bdata, WritableRaster raster, Rectangle rect) {
      SampleModel sm = raster.getSampleModel();
      if (!isBinary(sm)) {
         throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
      } else {
         int rectX = rect.x;
         int rectY = rect.y;
         int rectWidth = rect.width;
         int rectHeight = rect.height;
         DataBuffer dataBuffer = raster.getDataBuffer();
         int dx = rectX - raster.getSampleModelTranslateX();
         int dy = rectY - raster.getSampleModelTranslateY();
         MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
         int lineStride = mpp.getScanlineStride();
         int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
         int bitOffset = mpp.getBitOffset(dx);
         int k = 0;
         if (dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();

            for (int y = 0; y < rectHeight; y++) {
               int bOffset = eltOffset * 8 + bitOffset;

               for (int x = 0; x < rectWidth; x++) {
                  if (bdata[k++] != 0) {
                     data[bOffset / 8] = (byte)(data[bOffset / 8] | (byte)(1 << (7 - bOffset & 7)));
                  }

                  bOffset++;
               }

               eltOffset += lineStride;
            }
         } else if (!(dataBuffer instanceof DataBufferShort) && !(dataBuffer instanceof DataBufferUShort)) {
            if (dataBuffer instanceof DataBufferInt) {
               int[] data = ((DataBufferInt)dataBuffer).getData();

               for (int y = 0; y < rectHeight; y++) {
                  int bOffset = eltOffset * 32 + bitOffset;

                  for (int x = 0; x < rectWidth; x++) {
                     if (bdata[k++] != 0) {
                        data[bOffset / 32] = data[bOffset / 32] | 1 << 31 - bOffset % 32;
                     }

                     bOffset++;
                  }

                  eltOffset += lineStride;
               }
            }
         } else {
            short[] data = dataBuffer instanceof DataBufferShort ? ((DataBufferShort)dataBuffer).getData() : ((DataBufferUShort)dataBuffer).getData();

            for (int y = 0; y < rectHeight; y++) {
               int bOffset = eltOffset * 16 + bitOffset;

               for (int x = 0; x < rectWidth; x++) {
                  if (bdata[k++] != 0) {
                     data[bOffset / 16] = (short)(data[bOffset / 16] | (short)(1 << 15 - bOffset % 16));
                  }

                  bOffset++;
               }

               eltOffset += lineStride;
            }
         }
      }
   }

   public static boolean isBinary(SampleModel sm) {
      return sm instanceof MultiPixelPackedSampleModel && ((MultiPixelPackedSampleModel)sm).getPixelBitStride() == 1 && sm.getNumBands() == 1;
   }

   public static ColorModel createColorModel(ColorSpace colorSpace, SampleModel sampleModel) {
      ColorModel colorModel = null;
      if (sampleModel == null) {
         throw new IllegalArgumentException(I18N.getString("ImageUtil1"));
      } else {
         int numBands = sampleModel.getNumBands();
         if (numBands >= 1 && numBands <= 4) {
            int dataType = sampleModel.getDataType();
            if (sampleModel instanceof ComponentSampleModel) {
               if (dataType < 0 || dataType > 5) {
                  return null;
               }

               if (colorSpace == null) {
                  colorSpace = numBands <= 2 ? ColorSpace.getInstance(1003) : ColorSpace.getInstance(1000);
               }

               boolean useAlpha = numBands == 2 || numBands == 4;
               int transparency = useAlpha ? 3 : 1;
               boolean premultiplied = false;
               int dataTypeSize = DataBuffer.getDataTypeSize(dataType);
               int[] bits = new int[numBands];

               for (int i = 0; i < numBands; i++) {
                  bits[i] = dataTypeSize;
               }

               colorModel = new ComponentColorModel(colorSpace, bits, useAlpha, premultiplied, transparency, dataType);
            } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
               SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel)sampleModel;
               int[] bitMasks = sppsm.getBitMasks();
               int rmask = 0;
               int gmask = 0;
               int bmask = 0;
               int amask = 0;
               numBands = bitMasks.length;
               if (numBands <= 2) {
                  rmask = gmask = bmask = bitMasks[0];
                  if (numBands == 2) {
                     amask = bitMasks[1];
                  }
               } else {
                  rmask = bitMasks[0];
                  gmask = bitMasks[1];
                  bmask = bitMasks[2];
                  if (numBands == 4) {
                     amask = bitMasks[3];
                  }
               }

               int[] sampleSize = sppsm.getSampleSize();
               int bits = 0;

               for (int i = 0; i < sampleSize.length; i++) {
                  bits += sampleSize[i];
               }

               if (colorSpace == null) {
                  colorSpace = ColorSpace.getInstance(1000);
               }

               colorModel = new DirectColorModel(colorSpace, bits, rmask, gmask, bmask, amask, false, sampleModel.getDataType());
            } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
               int bits = ((MultiPixelPackedSampleModel)sampleModel).getPixelBitStride();
               int size = 1 << bits;
               byte[] comp = new byte[size];

               for (int i = 0; i < size; i++) {
                  comp[i] = (byte)(255 * i / (size - 1));
               }

               colorModel = new IndexColorModel(bits, size, comp, comp, comp);
            }

            return colorModel;
         } else {
            return null;
         }
      }
   }

   public static int getElementSize(SampleModel sm) {
      int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());
      if (sm instanceof MultiPixelPackedSampleModel) {
         MultiPixelPackedSampleModel mppsm = (MultiPixelPackedSampleModel)sm;
         return mppsm.getSampleSize(0) * mppsm.getNumBands();
      } else if (sm instanceof ComponentSampleModel) {
         return sm.getNumBands() * elementSize;
      } else {
         return sm instanceof SinglePixelPackedSampleModel ? elementSize : elementSize * sm.getNumBands();
      }
   }

   public static long getTileSize(SampleModel sm) {
      int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());
      if (sm instanceof MultiPixelPackedSampleModel) {
         MultiPixelPackedSampleModel mppsm = (MultiPixelPackedSampleModel)sm;
         return (long)((mppsm.getScanlineStride() * mppsm.getHeight() + (mppsm.getDataBitOffset() + elementSize - 1) / elementSize) * ((elementSize + 7) / 8));
      } else if (!(sm instanceof ComponentSampleModel)) {
         if (sm instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel)sm;
            long size = (long)(sppsm.getScanlineStride() * (sppsm.getHeight() - 1) + sppsm.getWidth());
            return size * (long)((elementSize + 7) / 8);
         } else {
            return 0L;
         }
      } else {
         ComponentSampleModel csm = (ComponentSampleModel)sm;
         int[] bandOffsets = csm.getBandOffsets();
         int maxBandOff = bandOffsets[0];

         for (int i = 1; i < bandOffsets.length; i++) {
            maxBandOff = Math.max(maxBandOff, bandOffsets[i]);
         }

         long size = 0L;
         int pixelStride = csm.getPixelStride();
         int scanlineStride = csm.getScanlineStride();
         if (maxBandOff >= 0) {
            size += (long)(maxBandOff + 1);
         }

         if (pixelStride > 0) {
            size += (long)(pixelStride * (sm.getWidth() - 1));
         }

         if (scanlineStride > 0) {
            size += (long)(scanlineStride * (sm.getHeight() - 1));
         }

         int[] bankIndices = csm.getBankIndices();
         maxBandOff = bankIndices[0];

         for (int i = 1; i < bankIndices.length; i++) {
            maxBandOff = Math.max(maxBandOff, bankIndices[i]);
         }

         return size * (long)(maxBandOff + 1) * (long)((elementSize + 7) / 8);
      }
   }

   public static long getBandSize(SampleModel sm) {
      int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());
      if (sm instanceof ComponentSampleModel) {
         ComponentSampleModel csm = (ComponentSampleModel)sm;
         int pixelStride = csm.getPixelStride();
         int scanlineStride = csm.getScanlineStride();
         long size = (long)Math.min(pixelStride, scanlineStride);
         if (pixelStride > 0) {
            size += (long)(pixelStride * (sm.getWidth() - 1));
         }

         if (scanlineStride > 0) {
            size += (long)(scanlineStride * (sm.getHeight() - 1));
         }

         return size * (long)((elementSize + 7) / 8);
      } else {
         return getTileSize(sm);
      }
   }

   public static boolean isGrayscaleMapping(IndexColorModel icm) {
      if (icm == null) {
         throw new IllegalArgumentException("icm == null!");
      } else {
         int mapSize = icm.getMapSize();
         byte[] r = new byte[mapSize];
         byte[] g = new byte[mapSize];
         byte[] b = new byte[mapSize];
         icm.getReds(r);
         icm.getGreens(g);
         icm.getBlues(b);
         boolean isGrayToColor = true;

         for (int i = 0; i < mapSize; i++) {
            byte temp = (byte)(i * 255 / (mapSize - 1));
            if (r[i] != temp || g[i] != temp || b[i] != temp) {
               isGrayToColor = false;
               break;
            }
         }

         if (!isGrayToColor) {
            isGrayToColor = true;
            int ix = 0;

            for (int j = mapSize - 1; ix < mapSize; j--) {
               byte temp = (byte)(j * 255 / (mapSize - 1));
               if (r[ix] != temp || g[ix] != temp || b[ix] != temp) {
                  isGrayToColor = false;
                  break;
               }

               ix++;
            }
         }

         return isGrayToColor;
      }
   }

   public static boolean isIndicesForGrayscale(byte[] r, byte[] g, byte[] b) {
      if (r.length == g.length && r.length == b.length) {
         int size = r.length;
         if (size != 256) {
            return false;
         } else {
            for (int i = 0; i < size; i++) {
               byte temp = (byte)i;
               if (r[i] != temp || g[i] != temp || b[i] != temp) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static String convertObjectToString(Object obj) {
      if (obj == null) {
         return "";
      } else {
         String s = "";
         if (obj instanceof byte[]) {
            byte[] bArray = (byte[])obj;

            for (int i = 0; i < bArray.length; i++) {
               s = s + bArray[i] + " ";
            }

            return s;
         } else if (obj instanceof int[]) {
            int[] iArray = (int[])obj;

            for (int i = 0; i < iArray.length; i++) {
               s = s + iArray[i] + " ";
            }

            return s;
         } else if (!(obj instanceof short[])) {
            return obj.toString();
         } else {
            short[] sArray = (short[])obj;

            for (int i = 0; i < sArray.length; i++) {
               s = s + sArray[i] + " ";
            }

            return s;
         }
      }
   }

   public static final void canEncodeImage(ImageWriter writer, ImageTypeSpecifier type) throws IIOException {
      ImageWriterSpi spi = writer.getOriginatingProvider();
      if (type != null && spi != null && !spi.canEncodeImage(type)) {
         throw new IIOException(I18N.getString("ImageUtil2") + " " + writer.getClass().getName());
      }
   }

   public static final void canEncodeImage(ImageWriter writer, ColorModel colorModel, SampleModel sampleModel) throws IIOException {
      ImageTypeSpecifier type = null;
      if (colorModel != null && sampleModel != null) {
         type = new ImageTypeSpecifier(colorModel, sampleModel);
      }

      canEncodeImage(writer, type);
   }

   public static final boolean imageIsContiguous(RenderedImage image) {
      SampleModel sm;
      if (image instanceof BufferedImage) {
         WritableRaster ras = ((BufferedImage)image).getRaster();
         sm = ras.getSampleModel();
      } else {
         sm = image.getSampleModel();
      }

      if (sm instanceof ComponentSampleModel) {
         ComponentSampleModel csm = (ComponentSampleModel)sm;
         if (csm.getPixelStride() != csm.getNumBands()) {
            return false;
         } else {
            int[] bandOffsets = csm.getBandOffsets();

            for (int i = 0; i < bandOffsets.length; i++) {
               if (bandOffsets[i] != i) {
                  return false;
               }
            }

            int[] bankIndices = csm.getBankIndices();

            for (int ix = 0; ix < bandOffsets.length; ix++) {
               if (bankIndices[ix] != 0) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return isBinary(sm);
      }
   }

   public static final ImageTypeSpecifier getDestinationType(ImageReadParam param, Iterator imageTypes) throws IIOException {
      if (imageTypes != null && imageTypes.hasNext()) {
         ImageTypeSpecifier imageType = null;
         if (param != null) {
            imageType = param.getDestinationType();
         }

         if (imageType == null) {
            Object o = imageTypes.next();
            if (!(o instanceof ImageTypeSpecifier)) {
               throw new IllegalArgumentException("Non-ImageTypeSpecifier retrieved from imageTypes!");
            }

            imageType = (ImageTypeSpecifier)o;
         } else {
            boolean foundIt = false;

            while (imageTypes.hasNext()) {
               ImageTypeSpecifier type = (ImageTypeSpecifier)imageTypes.next();
               if (type.equals(imageType)) {
                  foundIt = true;
                  break;
               }
            }

            if (!foundIt) {
               throw new IIOException("Destination type from ImageReadParam does not match!");
            }
         }

         return imageType;
      } else {
         throw new IllegalArgumentException("imageTypes null or empty!");
      }
   }

   public static boolean isNonStandardICCColorSpace(ColorSpace cs) {
      boolean retval = false;

      try {
         retval = cs instanceof ICC_ColorSpace
            && !cs.isCS_sRGB()
            && !cs.equals(ColorSpace.getInstance(1004))
            && !cs.equals(ColorSpace.getInstance(1003))
            && !cs.equals(ColorSpace.getInstance(1001))
            && !cs.equals(ColorSpace.getInstance(1002));
      } catch (IllegalArgumentException var3) {
      }

      return retval;
   }

   public static List getJDKImageReaderWriterSPI(ServiceRegistry registry, String formatName, boolean isReader) {
      IIORegistry iioRegistry = (IIORegistry)registry;
      Class spiClass;
      String descPart;
      if (isReader) {
         spiClass = ImageReaderSpi.class;
         descPart = " image reader";
      } else {
         spiClass = ImageWriterSpi.class;
         descPart = " image writer";
      }

      Iterator iter = ServiceLoader.load(spiClass).iterator();
      String desc = "standard " + formatName + descPart;
      String jiioPath = "com.github.jaiimageio.impl";
      Locale locale = Locale.getDefault();
      ArrayList list = new ArrayList();

      while (iter.hasNext()) {
         ImageReaderWriterSpi provider = (ImageReaderWriterSpi)iter.next();
         if (provider.getVendorName().startsWith("Sun Microsystems")
            && desc.equalsIgnoreCase(provider.getDescription(locale))
            && !provider.getPluginClassName().startsWith(jiioPath)) {
            String[] formatNames = provider.getFormatNames();

            for (int i = 0; i < formatNames.length; i++) {
               if (formatNames[i].equalsIgnoreCase(formatName)) {
                  list.add(provider);
                  break;
               }
            }
         }
      }

      return list;
   }

   public static void processOnRegistration(
      ServiceRegistry registry, Class category, String formatName, ImageReaderWriterSpi spi, int deregisterJvmVersion, int priorityJvmVersion
   ) {
      String jvmVendor = System.getProperty("java.vendor");
      String jvmSpecificationVersion = System.getProperty("java.specification.version");
      int jvmVersion = getJvmVersion(jvmSpecificationVersion);
      if (jvmVendor.equals("Sun Microsystems Inc.")) {
         List list;
         if (spi instanceof ImageReaderSpi) {
            list = getJDKImageReaderWriterSPI(registry, formatName, true);
         } else {
            list = getJDKImageReaderWriterSPI(registry, formatName, false);
         }

         if (jvmVersion >= deregisterJvmVersion && list.size() != 0) {
            registry.deregisterServiceProvider(spi, category);
         } else {
            for (int i = 0; i < list.size(); i++) {
               if (jvmVersion >= priorityJvmVersion) {
                  registry.setOrdering(category, list.get(i), spi);
               } else {
                  registry.setOrdering(category, spi, (ImageReaderWriterSpi)list.get(i));
               }
            }
         }
      }
   }

   static int getJvmVersion(String jvmSpecificationVersion) {
      if (jvmSpecificationVersion.startsWith("1.")) {
         jvmSpecificationVersion = jvmSpecificationVersion.substring(2);
         return Integer.parseInt(jvmSpecificationVersion);
      } else {
         return Integer.parseInt(jvmSpecificationVersion);
      }
   }

   public static int readMultiByteInteger(ImageInputStream iis) throws IOException {
      int value = iis.readByte();
      int result = value & 127;

      while ((value & 128) == 128) {
         result <<= 7;
         value = iis.readByte();
         result |= value & 127;
      }

      return result;
   }
}
