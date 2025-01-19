package com.github.jaiimageio.impl.plugins.raw;

import com.github.jaiimageio.impl.common.ImageUtil;
import com.github.jaiimageio.impl.common.SimpleRenderedImage;
import com.github.jaiimageio.stream.RawImageInputStream;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;

public class RawRenderedImage extends SimpleRenderedImage {
   private SampleModel originalSampleModel;
   private Raster currentTile;
   private Point currentTileGrid;
   private RawImageInputStream iis = null;
   private RawImageReader reader;
   private ImageReadParam param = null;
   private int imageIndex;
   private Rectangle destinationRegion;
   private Rectangle originalRegion;
   private Point sourceOrigin;
   private Dimension originalDimension;
   private int maxXTile;
   private int maxYTile;
   private int scaleX;
   private int scaleY;
   private int xOffset;
   private int yOffset;
   private int[] destinationBands = null;
   private int[] sourceBands = null;
   private int nComp;
   private boolean noTransform = true;
   private WritableRaster rasForATile;
   private BufferedImage destImage;
   private long position;
   private long tileDataSize;
   private int originalNumXTiles;

   public RawRenderedImage(RawImageInputStream iis, RawImageReader reader, ImageReadParam param, int imageIndex) throws IOException {
      this.iis = iis;
      this.reader = reader;
      this.param = param;
      this.imageIndex = imageIndex;
      this.position = iis.getImageOffset(imageIndex);
      this.originalDimension = iis.getImageDimension(imageIndex);
      ImageTypeSpecifier type = iis.getImageType();
      this.sampleModel = this.originalSampleModel = type.getSampleModel();
      this.colorModel = type.getColorModel();
      this.sourceBands = param == null ? null : param.getSourceBands();
      if (this.sourceBands == null) {
         this.nComp = this.originalSampleModel.getNumBands();
         this.sourceBands = new int[this.nComp];
         int i = 0;

         while (i < this.nComp) {
            this.sourceBands[i] = i++;
         }
      } else {
         this.sampleModel = this.originalSampleModel.createSubsetSampleModel(this.sourceBands);
         this.colorModel = ImageUtil.createColorModel(null, this.sampleModel);
      }

      this.nComp = this.sourceBands.length;
      this.destinationBands = param == null ? null : param.getDestinationBands();
      if (this.destinationBands == null) {
         this.destinationBands = new int[this.nComp];
         int i = 0;

         while (i < this.nComp) {
            this.destinationBands[i] = i++;
         }
      }

      Dimension dim = iis.getImageDimension(imageIndex);
      this.width = dim.width;
      this.height = dim.height;
      Rectangle sourceRegion = new Rectangle(0, 0, this.width, this.height);
      this.originalRegion = (Rectangle)sourceRegion.clone();
      this.destinationRegion = (Rectangle)sourceRegion.clone();
      if (param != null) {
         RawImageReader.computeRegionsWrapper(param, this.width, this.height, param.getDestination(), sourceRegion, this.destinationRegion);
         this.scaleX = param.getSourceXSubsampling();
         this.scaleY = param.getSourceYSubsampling();
         this.xOffset = param.getSubsamplingXOffset();
         this.yOffset = param.getSubsamplingYOffset();
      }

      this.sourceOrigin = new Point(sourceRegion.x, sourceRegion.y);
      if (!this.destinationRegion.equals(sourceRegion)) {
         this.noTransform = false;
      }

      this.tileDataSize = ImageUtil.getTileSize(this.originalSampleModel);
      this.tileWidth = this.originalSampleModel.getWidth();
      this.tileHeight = this.originalSampleModel.getHeight();
      this.tileGridXOffset = this.destinationRegion.x;
      this.tileGridYOffset = this.destinationRegion.y;
      this.originalNumXTiles = this.getNumXTiles();
      this.width = this.destinationRegion.width;
      this.height = this.destinationRegion.height;
      this.minX = this.destinationRegion.x;
      this.minY = this.destinationRegion.y;
      this.sampleModel = this.sampleModel.createCompatibleSampleModel(this.tileWidth, this.tileHeight);
      this.maxXTile = this.originalDimension.width / this.tileWidth;
      this.maxYTile = this.originalDimension.height / this.tileHeight;
   }

   @Override
   public synchronized Raster getTile(int tileX, int tileY) {
      if (this.currentTile != null && this.currentTileGrid.x == tileX && this.currentTileGrid.y == tileY) {
         return this.currentTile;
      } else if (tileX < this.getNumXTiles() && tileY < this.getNumYTiles()) {
         try {
            this.iis.seek(this.position + (long)(tileY * this.originalNumXTiles + tileX) * this.tileDataSize);
            int x = this.tileXToX(tileX);
            int y = this.tileYToY(tileY);
            this.currentTile = Raster.createWritableRaster(this.sampleModel, new Point(x, y));
            if (this.noTransform) {
               switch (this.sampleModel.getDataType()) {
                  case 0:
                     byte[][] buf = ((DataBufferByte)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < buf.length; i++) {
                        this.iis.readFully(buf[i], 0, buf[i].length);
                     }
                     break;
                  case 1:
                     short[][] usbuf = ((DataBufferUShort)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < usbuf.length; i++) {
                        this.iis.readFully(usbuf[i], 0, usbuf[i].length);
                     }
                     break;
                  case 2:
                     short[][] sbuf = ((DataBufferShort)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < sbuf.length; i++) {
                        this.iis.readFully(sbuf[i], 0, sbuf[i].length);
                     }
                     break;
                  case 3:
                     int[][] ibuf = ((DataBufferInt)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < ibuf.length; i++) {
                        this.iis.readFully(ibuf[i], 0, ibuf[i].length);
                     }
                     break;
                  case 4:
                     float[][] fbuf = ((DataBufferFloat)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < fbuf.length; i++) {
                        this.iis.readFully(fbuf[i], 0, fbuf[i].length);
                     }
                     break;
                  case 5:
                     double[][] dbuf = ((DataBufferDouble)this.currentTile.getDataBuffer()).getBankData();

                     for (int i = 0; i < dbuf.length; i++) {
                        this.iis.readFully(dbuf[i], 0, dbuf[i].length);
                     }
               }
            } else {
               this.currentTile = this.readSubsampledRaster((WritableRaster)this.currentTile);
            }
         } catch (IOException var12) {
            throw new RuntimeException(var12);
         }

         if (this.currentTileGrid == null) {
            this.currentTileGrid = new Point(tileX, tileY);
         } else {
            this.currentTileGrid.x = tileX;
            this.currentTileGrid.y = tileY;
         }

         return this.currentTile;
      } else {
         throw new IllegalArgumentException(I18N.getString("RawRenderedImage0"));
      }
   }

   public void readAsRaster(WritableRaster raster) throws IOException {
      this.readSubsampledRaster(raster);
   }

   private Raster readSubsampledRaster(WritableRaster raster) throws IOException {
      if (raster == null) {
         raster = Raster.createWritableRaster(
            this.sampleModel
               .createCompatibleSampleModel(this.destinationRegion.x + this.destinationRegion.width, this.destinationRegion.y + this.destinationRegion.height),
            new Point(this.destinationRegion.x, this.destinationRegion.y)
         );
      }

      int numBands = this.sourceBands.length;
      int dataType = this.sampleModel.getDataType();
      int sampleSizeBit = DataBuffer.getDataTypeSize(dataType);
      int sampleSizeByte = (sampleSizeBit + 7) / 8;
      Rectangle destRect = raster.getBounds().intersection(this.destinationRegion);
      int offx = this.destinationRegion.x;
      int offy = this.destinationRegion.y;
      int sourceSX = (destRect.x - offx) * this.scaleX + this.sourceOrigin.x;
      int sourceSY = (destRect.y - offy) * this.scaleY + this.sourceOrigin.y;
      int sourceEX = (destRect.width - 1) * this.scaleX + sourceSX;
      int sourceEY = (destRect.height - 1) * this.scaleY + sourceSY;
      int startXTile = sourceSX / this.tileWidth;
      int startYTile = sourceSY / this.tileHeight;
      int endXTile = sourceEX / this.tileWidth;
      int endYTile = sourceEY / this.tileHeight;
      startXTile = this.clip(startXTile, 0, this.maxXTile);
      startYTile = this.clip(startYTile, 0, this.maxYTile);
      endXTile = this.clip(endXTile, 0, this.maxXTile);
      endYTile = this.clip(endYTile, 0, this.maxYTile);
      int totalXTiles = this.getNumXTiles();
      int totalYTiles = this.getNumYTiles();
      int totalTiles = totalXTiles * totalYTiles;
      byte[] pixbuf = null;
      short[] spixbuf = null;
      int[] ipixbuf = null;
      float[] fpixbuf = null;
      double[] dpixbuf = null;
      boolean singleBank = true;
      int pixelStride = 0;
      int scanlineStride = 0;
      int bandStride = 0;
      int[] bandOffsets = null;
      int[] bankIndices = null;
      if (this.originalSampleModel instanceof ComponentSampleModel) {
         ComponentSampleModel csm = (ComponentSampleModel)this.originalSampleModel;
         bankIndices = csm.getBankIndices();
         int maxBank = 0;

         for (int i = 0; i < bankIndices.length; i++) {
            if (maxBank > bankIndices[i]) {
               maxBank = bankIndices[i];
            }
         }

         if (maxBank > 0) {
            singleBank = false;
         }

         pixelStride = csm.getPixelStride();
         scanlineStride = csm.getScanlineStride();
         bandOffsets = csm.getBandOffsets();

         for (int ix = 0; ix < bandOffsets.length; ix++) {
            if (bandStride < bandOffsets[ix]) {
               bandStride = bandOffsets[ix];
            }
         }
      } else if (this.originalSampleModel instanceof MultiPixelPackedSampleModel) {
         scanlineStride = ((MultiPixelPackedSampleModel)this.originalSampleModel).getScanlineStride();
      } else if (this.originalSampleModel instanceof SinglePixelPackedSampleModel) {
         pixelStride = 1;
         scanlineStride = ((SinglePixelPackedSampleModel)this.originalSampleModel).getScanlineStride();
      }

      byte[] destPixbuf = null;
      short[] destSPixbuf = null;
      int[] destIPixbuf = null;
      float[] destFPixbuf = null;
      double[] destDPixbuf = null;
      int[] destBandOffsets = null;
      int destPixelStride = 0;
      int destScanlineStride = 0;
      int destSX = 0;
      if (raster.getSampleModel() instanceof ComponentSampleModel) {
         ComponentSampleModel csm = (ComponentSampleModel)raster.getSampleModel();
         bankIndices = csm.getBankIndices();
         destBandOffsets = csm.getBandOffsets();
         destPixelStride = csm.getPixelStride();
         destScanlineStride = csm.getScanlineStride();
         destSX = csm.getOffset(raster.getMinX() - raster.getSampleModelTranslateX(), raster.getMinY() - raster.getSampleModelTranslateY())
            - destBandOffsets[0];
         switch (dataType) {
            case 0:
               destPixbuf = ((DataBufferByte)raster.getDataBuffer()).getData();
               break;
            case 1:
               destSPixbuf = ((DataBufferUShort)raster.getDataBuffer()).getData();
               break;
            case 2:
               destSPixbuf = ((DataBufferShort)raster.getDataBuffer()).getData();
               break;
            case 3:
               destIPixbuf = ((DataBufferInt)raster.getDataBuffer()).getData();
               break;
            case 4:
               destFPixbuf = ((DataBufferFloat)raster.getDataBuffer()).getData();
               break;
            case 5:
               destDPixbuf = ((DataBufferDouble)raster.getDataBuffer()).getData();
         }
      } else if (raster.getSampleModel() instanceof SinglePixelPackedSampleModel) {
         numBands = 1;
         bankIndices = new int[]{0};
         destBandOffsets = new int[numBands];

         for (int ixx = 0; ixx < numBands; ixx++) {
            destBandOffsets[ixx] = 0;
         }

         destPixelStride = 1;
         destScanlineStride = ((SinglePixelPackedSampleModel)raster.getSampleModel()).getScanlineStride();
      }

      for (int y = startYTile; y <= endYTile && !this.reader.getAbortRequest(); y++) {
         for (int x = startXTile; x <= endXTile && !this.reader.getAbortRequest(); x++) {
            long tilePosition = this.position + (long)(y * this.originalNumXTiles + x) * this.tileDataSize;
            this.iis.seek(tilePosition);
            float percentage = (float)((x - startXTile + y * totalXTiles) / totalXTiles);
            int startX = x * this.tileWidth;
            int startY = y * this.tileHeight;
            int cTileHeight = this.tileHeight;
            int cTileWidth = this.tileWidth;
            if (startY + cTileHeight >= this.originalDimension.height) {
               cTileHeight = this.originalDimension.height - startY;
            }

            if (startX + cTileWidth >= this.originalDimension.width) {
               cTileWidth = this.originalDimension.width - startX;
            }

            int tx = startX;
            int ty = startY;
            if (sourceSX > startX) {
               cTileWidth += startX - sourceSX;
               tx = sourceSX;
               startX = sourceSX;
            }

            if (sourceSY > startY) {
               cTileHeight += startY - sourceSY;
               ty = sourceSY;
               startY = sourceSY;
            }

            if (sourceEX < startX + cTileWidth - 1) {
               cTileWidth += sourceEX - startX - cTileWidth + 1;
            }

            if (sourceEY < startY + cTileHeight - 1) {
               cTileHeight += sourceEY - startY - cTileHeight + 1;
            }

            int x1 = (startX + this.scaleX - 1 - this.sourceOrigin.x) / this.scaleX;
            int x2 = (startX + this.scaleX - 1 + cTileWidth - this.sourceOrigin.x) / this.scaleX;
            int lineLength = x2 - x1;
            x2 = (x2 - 1) * this.scaleX + this.sourceOrigin.x;
            int y1 = (startY + this.scaleY - 1 - this.sourceOrigin.y) / this.scaleY;
            startX = x1 * this.scaleX + this.sourceOrigin.x;
            startY = y1 * this.scaleY + this.sourceOrigin.y;
            x1 += offx;
            y1 += offy;
            tx -= x * this.tileWidth;
            ty -= y * this.tileHeight;
            if (this.sampleModel instanceof MultiPixelPackedSampleModel) {
               MultiPixelPackedSampleModel mppsm = (MultiPixelPackedSampleModel)this.originalSampleModel;
               this.iis.skipBytes(mppsm.getOffset(tx, ty) * sampleSizeByte);
               int readBytes = (mppsm.getOffset(x2, 0) - mppsm.getOffset(startX, 0) + 1) * sampleSizeByte;
               int skipLength = (scanlineStride * this.scaleY - readBytes) * sampleSizeByte;
               readBytes *= sampleSizeByte;
               if (pixbuf == null || pixbuf.length < readBytes) {
                  pixbuf = new byte[readBytes];
               }

               int bitoff = mppsm.getBitOffset(tx);
               int l = 0;

               for (int m = y1; l < cTileHeight && !this.reader.getAbortRequest(); m++) {
                  this.iis.readFully(pixbuf, 0, readBytes);
                  if (this.scaleX != 1) {
                     int bit = 7;
                     int pos = 0;
                     int mask = 128;
                     int n = 0;

                     for (int n1 = startX & 7; n < lineLength; n1 += this.scaleX) {
                        pixbuf[pos] = (byte)(pixbuf[pos] & ~(1 << bit) | (pixbuf[n1 >> 3] >> 7 - (n1 & 7) & 1) << bit);
                        if (--bit == -1) {
                           bit = 7;
                           pos++;
                        }

                        n++;
                     }
                  } else if (bitoff != 0) {
                     int mask1 = 255 << bitoff & 0xFF;
                     int mask2 = ~mask1 & 0xFF;
                     int shift = 8 - bitoff;

                     int n;
                     for (n = 0; n < readBytes - 1; n++) {
                        pixbuf[n] = (byte)((pixbuf[n] & mask2) << shift | (pixbuf[n + 1] & mask1) >> bitoff);
                     }

                     pixbuf[n] = (byte)((pixbuf[n] & mask2) << shift);
                  }

                  ImageUtil.setPackedBinaryData(pixbuf, raster, new Rectangle(x1, m, lineLength, 1));
                  this.iis.skipBytes(skipLength);
                  if (this.destImage != null) {
                     this.reader.processImageUpdateWrapper(this.destImage, x1, m, cTileWidth, 1, 1, 1, this.destinationBands);
                  }

                  this.reader.processImageProgressWrapper(percentage + ((float)(l - startY) + 1.0F) / (float)cTileHeight / (float)totalTiles);
                  l += this.scaleY;
               }
            } else {
               int readLength;
               int skipLength;
               if (pixelStride < scanlineStride) {
                  readLength = cTileWidth * pixelStride;
                  skipLength = (scanlineStride * this.scaleY - readLength) * sampleSizeByte;
               } else {
                  readLength = cTileHeight * scanlineStride;
                  skipLength = (pixelStride * this.scaleX - readLength) * sampleSizeByte;
               }

               switch (this.sampleModel.getDataType()) {
                  case 0:
                     if (pixbuf == null || pixbuf.length < readLength) {
                        pixbuf = new byte[readLength];
                     }
                     break;
                  case 1:
                  case 2:
                     if (spixbuf == null || spixbuf.length < readLength) {
                        spixbuf = new short[readLength];
                     }
                     break;
                  case 3:
                     if (ipixbuf == null || ipixbuf.length < readLength) {
                        ipixbuf = new int[readLength];
                     }
                     break;
                  case 4:
                     if (fpixbuf == null || fpixbuf.length < readLength) {
                        fpixbuf = new float[readLength];
                     }
                     break;
                  case 5:
                     if (dpixbuf == null || dpixbuf.length < readLength) {
                        dpixbuf = new double[readLength];
                     }
               }

               if (this.sampleModel instanceof PixelInterleavedSampleModel) {
                  this.iis.skipBytes((tx * pixelStride + ty * scanlineStride) * sampleSizeByte);
                  int outerFirst;
                  int outerSecond;
                  int outerStep;
                  int outerBound;
                  int innerStep;
                  int innerStep1;
                  int outerStep1;
                  if (pixelStride < scanlineStride) {
                     outerFirst = 0;
                     outerSecond = y1;
                     outerStep = this.scaleY;
                     outerBound = cTileHeight;
                     innerStep = this.scaleX * pixelStride;
                     innerStep1 = destPixelStride;
                     outerStep1 = destScanlineStride;
                  } else {
                     outerFirst = 0;
                     outerSecond = x1;
                     outerStep = this.scaleX;
                     outerBound = cTileWidth;
                     innerStep = this.scaleY * scanlineStride;
                     innerStep1 = destScanlineStride;
                     outerStep1 = destPixelStride;
                  }

                  int destPos = destSX
                     + (y1 - raster.getSampleModelTranslateY()) * destScanlineStride
                     + (x1 - raster.getSampleModelTranslateX()) * destPixelStride;
                  int l = outerFirst;

                  for (int m = outerSecond; l < outerBound && !this.reader.getAbortRequest(); m++) {
                     switch (dataType) {
                        case 0:
                           if (innerStep == numBands && innerStep1 == numBands) {
                              this.iis.readFully(destPixbuf, destPos, readLength);
                              break;
                           }

                           this.iis.readFully(pixbuf, 0, readLength);
                           break;
                        case 1:
                        case 2:
                           if (innerStep == numBands && innerStep1 == numBands) {
                              this.iis.readFully(destSPixbuf, destPos, readLength);
                              break;
                           }

                           this.iis.readFully(spixbuf, 0, readLength);
                           break;
                        case 3:
                           if (innerStep == numBands && innerStep1 == numBands) {
                              this.iis.readFully(destIPixbuf, destPos, readLength);
                              break;
                           }

                           this.iis.readFully(ipixbuf, 0, readLength);
                           break;
                        case 4:
                           if (innerStep == numBands && innerStep1 == numBands) {
                              this.iis.readFully(destFPixbuf, destPos, readLength);
                              break;
                           }

                           this.iis.readFully(fpixbuf, 0, readLength);
                           break;
                        case 5:
                           if (innerStep == numBands && innerStep1 == numBands) {
                              this.iis.readFully(destDPixbuf, destPos, readLength);
                           } else {
                              this.iis.readFully(dpixbuf, 0, readLength);
                           }
                     }

                     if (innerStep != numBands || innerStep1 != numBands) {
                        for (int b = 0; b < numBands; b++) {
                           int destBandOffset = destBandOffsets[this.destinationBands[b]];
                           destPos += destBandOffset;
                           int sourceBandOffset = bandOffsets[this.sourceBands[b]];
                           switch (dataType) {
                              case 0:
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destPixbuf[n] = pixbuf[m1 + sourceBandOffset];
                                    m1 += innerStep;
                                 }
                                 break;
                              case 1:
                              case 2:
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destSPixbuf[n] = spixbuf[m1 + sourceBandOffset];
                                    m1 += innerStep;
                                 }
                                 break;
                              case 3:
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destIPixbuf[n] = ipixbuf[m1 + sourceBandOffset];
                                    m1 += innerStep;
                                 }
                                 break;
                              case 4:
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destFPixbuf[n] = fpixbuf[m1 + sourceBandOffset];
                                    m1 += innerStep;
                                 }
                                 break;
                              case 5:
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destDPixbuf[n] = dpixbuf[m1 + sourceBandOffset];
                                    m1 += innerStep;
                                 }
                           }

                           destPos -= destBandOffset;
                        }
                     }

                     this.iis.skipBytes(skipLength);
                     destPos += outerStep1;
                     if (this.destImage != null) {
                        if (pixelStride < scanlineStride) {
                           this.reader.processImageUpdateWrapper(this.destImage, x1, m, outerBound, 1, 1, 1, this.destinationBands);
                        } else {
                           this.reader.processImageUpdateWrapper(this.destImage, m, y1, 1, outerBound, 1, 1, this.destinationBands);
                        }
                     }

                     this.reader.processImageProgressWrapper(percentage + ((float)l + 1.0F) / (float)outerBound / (float)totalTiles);
                     l += outerStep;
                  }
               } else if (!(this.sampleModel instanceof BandedSampleModel) && !(this.sampleModel instanceof SinglePixelPackedSampleModel) && bandStride != 0) {
                  if (!(this.sampleModel instanceof ComponentSampleModel)) {
                     throw new IllegalArgumentException(I18N.getString("RawRenderedImage1"));
                  }

                  int bufferSize = (int)this.tileDataSize;
                  switch (this.sampleModel.getDataType()) {
                     case 0:
                        if (pixbuf == null || (long)pixbuf.length < this.tileDataSize) {
                           pixbuf = new byte[(int)this.tileDataSize];
                        }

                        this.iis.readFully(pixbuf, 0, (int)this.tileDataSize);
                        break;
                     case 1:
                     case 2:
                        bufferSize /= 2;
                        if (spixbuf == null || spixbuf.length < bufferSize) {
                           spixbuf = new short[bufferSize];
                        }

                        this.iis.readFully(spixbuf, 0, bufferSize);
                        break;
                     case 3:
                        bufferSize /= 4;
                        if (ipixbuf == null || ipixbuf.length < bufferSize) {
                           ipixbuf = new int[bufferSize];
                        }

                        this.iis.readFully(ipixbuf, 0, bufferSize);
                        break;
                     case 4:
                        bufferSize /= 4;
                        if (fpixbuf == null || fpixbuf.length < bufferSize) {
                           fpixbuf = new float[bufferSize];
                        }

                        this.iis.readFully(fpixbuf, 0, bufferSize);
                        break;
                     case 5:
                        bufferSize /= 8;
                        if (dpixbuf == null || dpixbuf.length < bufferSize) {
                           dpixbuf = new double[bufferSize];
                        }

                        this.iis.readFully(dpixbuf, 0, bufferSize);
                  }

                  for (int b = 0; b < numBands; b++) {
                     int destBandOffset = destBandOffsets[this.destinationBands[b]];
                     int destPos = ((ComponentSampleModel)raster.getSampleModel())
                        .getOffset(x1 - raster.getSampleModelTranslateX(), y1 - raster.getSampleModelTranslateY(), this.destinationBands[b]);
                     int bank = bankIndices[this.destinationBands[b]];
                     switch (dataType) {
                        case 0:
                           destPixbuf = ((DataBufferByte)raster.getDataBuffer()).getData(bank);
                           break;
                        case 1:
                           destSPixbuf = ((DataBufferUShort)raster.getDataBuffer()).getData(bank);
                           break;
                        case 2:
                           destSPixbuf = ((DataBufferShort)raster.getDataBuffer()).getData(bank);
                           break;
                        case 3:
                           destIPixbuf = ((DataBufferInt)raster.getDataBuffer()).getData(bank);
                           break;
                        case 4:
                           destFPixbuf = ((DataBufferFloat)raster.getDataBuffer()).getData(bank);
                           break;
                        case 5:
                           destDPixbuf = ((DataBufferDouble)raster.getDataBuffer()).getData(bank);
                     }

                     int srcPos = ((ComponentSampleModel)this.originalSampleModel).getOffset(tx, ty, this.sourceBands[b]);
                     int skipX = this.scaleX * pixelStride;
                     int l = 0;

                     for (int m = y1; l < cTileHeight && !this.reader.getAbortRequest(); m++) {
                        switch (dataType) {
                           case 0:
                              int n = 0;
                              int m1 = srcPos;

                              for (int m2 = destPos; n < lineLength; m2 += destPixelStride) {
                                 destPixbuf[m2] = pixbuf[m1];
                                 n++;
                                 m1 += skipX;
                              }
                              break;
                           case 1:
                           case 2:
                              int n = 0;
                              int m1 = srcPos;

                              for (int m2 = destPos; n < lineLength; m2 += destPixelStride) {
                                 destSPixbuf[m2] = spixbuf[m1];
                                 n++;
                                 m1 += skipX;
                              }
                              break;
                           case 3:
                              int n = 0;
                              int m1 = srcPos;

                              for (int m2 = destPos; n < lineLength; m2 += destPixelStride) {
                                 destIPixbuf[m2] = ipixbuf[m1];
                                 n++;
                                 m1 += skipX;
                              }
                              break;
                           case 4:
                              int n = 0;
                              int m1 = srcPos;

                              for (int m2 = destPos; n < lineLength; m2 += destPixelStride) {
                                 destFPixbuf[m2] = fpixbuf[m1];
                                 n++;
                                 m1 += skipX;
                              }
                              break;
                           case 5:
                              int n = 0;
                              int m1 = srcPos;

                              for (int m2 = destPos; n < lineLength; m2 += destPixelStride) {
                                 destDPixbuf[m2] = dpixbuf[m1];
                                 n++;
                                 m1 += skipX;
                              }
                        }

                        destPos += destScanlineStride;
                        srcPos += scanlineStride * this.scaleY;
                        if (this.destImage != null) {
                           int[] destBands = new int[]{this.destinationBands[b]};
                           this.reader.processImageUpdateWrapper(this.destImage, x1, m, cTileHeight, 1, 1, 1, destBands);
                        }

                        this.reader.processImageProgressWrapper(percentage + ((float)l + 1.0F) / (float)cTileHeight / (float)numBands / (float)totalTiles);
                        l += this.scaleY;
                     }
                  }
               } else {
                  boolean isBanded = this.sampleModel instanceof BandedSampleModel;
                  int bandSize = (int)ImageUtil.getBandSize(this.originalSampleModel);

                  for (int b = 0; b < numBands; b++) {
                     this.iis.seek(tilePosition + (long)(bandSize * this.sourceBands[b] * sampleSizeByte));
                     int destBandOffset = destBandOffsets[this.destinationBands[b]];
                     this.iis.skipBytes((ty * scanlineStride + tx * pixelStride) * sampleSizeByte);
                     int outerFirst;
                     int outerSecond;
                     int outerStep;
                     int outerBound;
                     int innerStep;
                     int innerStep1;
                     int outerStep1;
                     if (pixelStride < scanlineStride) {
                        outerFirst = 0;
                        outerSecond = y1;
                        outerStep = this.scaleY;
                        outerBound = cTileHeight;
                        innerStep = this.scaleX * pixelStride;
                        innerStep1 = destPixelStride;
                        outerStep1 = destScanlineStride;
                     } else {
                        outerFirst = 0;
                        outerSecond = x1;
                        outerStep = this.scaleX;
                        outerBound = cTileWidth;
                        innerStep = this.scaleY * scanlineStride;
                        innerStep1 = destScanlineStride;
                        outerStep1 = destPixelStride;
                     }

                     int destPos = destSX
                        + (y1 - raster.getSampleModelTranslateY()) * destScanlineStride
                        + (x1 - raster.getSampleModelTranslateX()) * destPixelStride
                        + destBandOffset;
                     int bank = bankIndices[this.destinationBands[b]];
                     switch (dataType) {
                        case 0:
                           destPixbuf = ((DataBufferByte)raster.getDataBuffer()).getData(bank);
                           break;
                        case 1:
                           destSPixbuf = ((DataBufferUShort)raster.getDataBuffer()).getData(bank);
                           break;
                        case 2:
                           destSPixbuf = ((DataBufferShort)raster.getDataBuffer()).getData(bank);
                           break;
                        case 3:
                           destIPixbuf = ((DataBufferInt)raster.getDataBuffer()).getData(bank);
                           break;
                        case 4:
                           destFPixbuf = ((DataBufferFloat)raster.getDataBuffer()).getData(bank);
                           break;
                        case 5:
                           destDPixbuf = ((DataBufferDouble)raster.getDataBuffer()).getData(bank);
                     }

                     int l = outerFirst;

                     for (int m = outerSecond; l < outerBound && !this.reader.getAbortRequest(); m++) {
                        switch (dataType) {
                           case 0:
                              if (innerStep == 1 && innerStep1 == 1) {
                                 this.iis.readFully(destPixbuf, destPos, readLength);
                              } else {
                                 this.iis.readFully(pixbuf, 0, readLength);
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destPixbuf[n] = pixbuf[m1];
                                    m1 += innerStep;
                                 }
                              }
                              break;
                           case 1:
                           case 2:
                              if (innerStep == 1 && innerStep1 == 1) {
                                 this.iis.readFully(destSPixbuf, destPos, readLength);
                              } else {
                                 this.iis.readFully(spixbuf, 0, readLength);
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destSPixbuf[n] = spixbuf[m1];
                                    m1 += innerStep;
                                 }
                              }
                              break;
                           case 3:
                              if (innerStep == 1 && innerStep1 == 1) {
                                 this.iis.readFully(destIPixbuf, destPos, readLength);
                              } else {
                                 this.iis.readFully(ipixbuf, 0, readLength);
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destIPixbuf[n] = ipixbuf[m1];
                                    m1 += innerStep;
                                 }
                              }
                              break;
                           case 4:
                              if (innerStep == 1 && innerStep1 == 1) {
                                 this.iis.readFully(destFPixbuf, destPos, readLength);
                              } else {
                                 this.iis.readFully(fpixbuf, 0, readLength);
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destFPixbuf[n] = fpixbuf[m1];
                                    m1 += innerStep;
                                 }
                              }
                              break;
                           case 5:
                              if (innerStep == 1 && innerStep1 == 1) {
                                 this.iis.readFully(destDPixbuf, destPos, readLength);
                              } else {
                                 this.iis.readFully(dpixbuf, 0, readLength);
                                 int m1 = 0;

                                 for (int n = destPos; m1 < readLength; n += innerStep1) {
                                    destDPixbuf[n] = dpixbuf[m1];
                                    m1 += innerStep;
                                 }
                              }
                        }

                        this.iis.skipBytes(skipLength);
                        destPos += outerStep1;
                        if (this.destImage != null) {
                           int[] destBands = new int[]{this.destinationBands[b]};
                           if (pixelStride < scanlineStride) {
                              this.reader.processImageUpdateWrapper(this.destImage, x1, m, outerBound, 1, 1, 1, destBands);
                           } else {
                              this.reader.processImageUpdateWrapper(this.destImage, m, y1, 1, outerBound, 1, 1, destBands);
                           }
                        }

                        this.reader
                           .processImageProgressWrapper((percentage + ((float)l + 1.0F) / (float)outerBound / (float)numBands / (float)totalTiles) * 100.0F);
                        l += outerStep;
                     }
                  }
               }
            }
         }
      }

      return raster;
   }

   public void setDestImage(BufferedImage image) {
      this.destImage = image;
   }

   public void clearDestImage() {
      this.destImage = null;
   }

   private int getTileNum(int x, int y) {
      int num = (y - this.getMinTileY()) * this.getNumXTiles() + x - this.getMinTileX();
      if (num >= 0 && num < this.getNumXTiles() * this.getNumYTiles()) {
         return num;
      } else {
         throw new IllegalArgumentException(I18N.getString("RawRenderedImage0"));
      }
   }

   private int clip(int value, int min, int max) {
      if (value < min) {
         value = min;
      }

      if (value > max) {
         value = max;
      }

      return value;
   }
}
