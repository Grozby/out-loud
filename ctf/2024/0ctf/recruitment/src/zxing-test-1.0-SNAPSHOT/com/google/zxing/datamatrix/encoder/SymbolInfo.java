package com.google.zxing.datamatrix.encoder;

import com.google.zxing.Dimension;

public class SymbolInfo {
   static final SymbolInfo[] PROD_SYMBOLS = new SymbolInfo[]{
      new SymbolInfo(false, 3, 5, 8, 8, 1),
      new SymbolInfo(false, 5, 7, 10, 10, 1),
      new SymbolInfo(true, 5, 7, 16, 6, 1),
      new SymbolInfo(false, 8, 10, 12, 12, 1),
      new SymbolInfo(true, 10, 11, 14, 6, 2),
      new SymbolInfo(false, 12, 12, 14, 14, 1),
      new SymbolInfo(true, 16, 14, 24, 10, 1),
      new SymbolInfo(false, 18, 14, 16, 16, 1),
      new SymbolInfo(false, 22, 18, 18, 18, 1),
      new SymbolInfo(true, 22, 18, 16, 10, 2),
      new SymbolInfo(false, 30, 20, 20, 20, 1),
      new SymbolInfo(true, 32, 24, 16, 14, 2),
      new SymbolInfo(false, 36, 24, 22, 22, 1),
      new SymbolInfo(false, 44, 28, 24, 24, 1),
      new SymbolInfo(true, 49, 28, 22, 14, 2),
      new SymbolInfo(false, 62, 36, 14, 14, 4),
      new SymbolInfo(false, 86, 42, 16, 16, 4),
      new SymbolInfo(false, 114, 48, 18, 18, 4),
      new SymbolInfo(false, 144, 56, 20, 20, 4),
      new SymbolInfo(false, 174, 68, 22, 22, 4),
      new SymbolInfo(false, 204, 84, 24, 24, 4, 102, 42),
      new SymbolInfo(false, 280, 112, 14, 14, 16, 140, 56),
      new SymbolInfo(false, 368, 144, 16, 16, 16, 92, 36),
      new SymbolInfo(false, 456, 192, 18, 18, 16, 114, 48),
      new SymbolInfo(false, 576, 224, 20, 20, 16, 144, 56),
      new SymbolInfo(false, 696, 272, 22, 22, 16, 174, 68),
      new SymbolInfo(false, 816, 336, 24, 24, 16, 136, 56),
      new SymbolInfo(false, 1050, 408, 18, 18, 36, 175, 68),
      new SymbolInfo(false, 1304, 496, 20, 20, 36, 163, 62),
      new DataMatrixSymbolInfo144()
   };
   private static SymbolInfo[] symbols = PROD_SYMBOLS;
   private final boolean rectangular;
   private final int dataCapacity;
   private final int errorCodewords;
   public final int matrixWidth;
   public final int matrixHeight;
   private final int dataRegions;
   private final int rsBlockData;
   private final int rsBlockError;

   public static void overrideSymbolSet(SymbolInfo[] override) {
      symbols = override;
   }

   public SymbolInfo(boolean rectangular, int dataCapacity, int errorCodewords, int matrixWidth, int matrixHeight, int dataRegions) {
      this(rectangular, dataCapacity, errorCodewords, matrixWidth, matrixHeight, dataRegions, dataCapacity, errorCodewords);
   }

   SymbolInfo(boolean rectangular, int dataCapacity, int errorCodewords, int matrixWidth, int matrixHeight, int dataRegions, int rsBlockData, int rsBlockError) {
      this.rectangular = rectangular;
      this.dataCapacity = dataCapacity;
      this.errorCodewords = errorCodewords;
      this.matrixWidth = matrixWidth;
      this.matrixHeight = matrixHeight;
      this.dataRegions = dataRegions;
      this.rsBlockData = rsBlockData;
      this.rsBlockError = rsBlockError;
   }

   public static SymbolInfo lookup(int dataCodewords) {
      return lookup(dataCodewords, SymbolShapeHint.FORCE_NONE, true);
   }

   public static SymbolInfo lookup(int dataCodewords, SymbolShapeHint shape) {
      return lookup(dataCodewords, shape, true);
   }

   public static SymbolInfo lookup(int dataCodewords, boolean allowRectangular, boolean fail) {
      SymbolShapeHint shape = allowRectangular ? SymbolShapeHint.FORCE_NONE : SymbolShapeHint.FORCE_SQUARE;
      return lookup(dataCodewords, shape, fail);
   }

   private static SymbolInfo lookup(int dataCodewords, SymbolShapeHint shape, boolean fail) {
      return lookup(dataCodewords, shape, null, null, fail);
   }

   public static SymbolInfo lookup(int dataCodewords, SymbolShapeHint shape, Dimension minSize, Dimension maxSize, boolean fail) {
      for (SymbolInfo symbol : symbols) {
         if ((shape != SymbolShapeHint.FORCE_SQUARE || !symbol.rectangular)
            && (shape != SymbolShapeHint.FORCE_RECTANGLE || symbol.rectangular)
            && (minSize == null || symbol.getSymbolWidth() >= minSize.getWidth() && symbol.getSymbolHeight() >= minSize.getHeight())
            && (maxSize == null || symbol.getSymbolWidth() <= maxSize.getWidth() && symbol.getSymbolHeight() <= maxSize.getHeight())
            && dataCodewords <= symbol.dataCapacity) {
            return symbol;
         }
      }

      if (fail) {
         throw new IllegalArgumentException("Can't find a symbol arrangement that matches the message. Data codewords: " + dataCodewords);
      } else {
         return null;
      }
   }

   private int getHorizontalDataRegions() {
      switch (this.dataRegions) {
         case 1:
            return 1;
         case 2:
         case 4:
            return 2;
         case 16:
            return 4;
         case 36:
            return 6;
         default:
            throw new IllegalStateException("Cannot handle this number of data regions");
      }
   }

   private int getVerticalDataRegions() {
      switch (this.dataRegions) {
         case 1:
         case 2:
            return 1;
         case 4:
            return 2;
         case 16:
            return 4;
         case 36:
            return 6;
         default:
            throw new IllegalStateException("Cannot handle this number of data regions");
      }
   }

   public final int getSymbolDataWidth() {
      return this.getHorizontalDataRegions() * this.matrixWidth;
   }

   public final int getSymbolDataHeight() {
      return this.getVerticalDataRegions() * this.matrixHeight;
   }

   public final int getSymbolWidth() {
      return this.getSymbolDataWidth() + this.getHorizontalDataRegions() * 2;
   }

   public final int getSymbolHeight() {
      return this.getSymbolDataHeight() + this.getVerticalDataRegions() * 2;
   }

   public int getCodewordCount() {
      return this.dataCapacity + this.errorCodewords;
   }

   public int getInterleavedBlockCount() {
      return this.dataCapacity / this.rsBlockData;
   }

   public final int getDataCapacity() {
      return this.dataCapacity;
   }

   public final int getErrorCodewords() {
      return this.errorCodewords;
   }

   public int getDataLengthForInterleavedBlock(int index) {
      return this.rsBlockData;
   }

   public final int getErrorLengthForInterleavedBlock(int index) {
      return this.rsBlockError;
   }

   @Override
   public final String toString() {
      return (this.rectangular ? "Rectangular Symbol:" : "Square Symbol:")
         + " data region "
         + this.matrixWidth
         + 'x'
         + this.matrixHeight
         + ", symbol size "
         + this.getSymbolWidth()
         + 'x'
         + this.getSymbolHeight()
         + ", symbol data size "
         + this.getSymbolDataWidth()
         + 'x'
         + this.getSymbolDataHeight()
         + ", codewords "
         + this.dataCapacity
         + '+'
         + this.errorCodewords;
   }
}
