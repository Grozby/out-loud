package com.google.zxing.datamatrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Dimension;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.encoder.DefaultPlacement;
import com.google.zxing.datamatrix.encoder.ErrorCorrection;
import com.google.zxing.datamatrix.encoder.HighLevelEncoder;
import com.google.zxing.datamatrix.encoder.MinimalEncoder;
import com.google.zxing.datamatrix.encoder.SymbolInfo;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import java.nio.charset.Charset;
import java.util.Map;

public final class DataMatrixWriter implements Writer {
   @Override
   public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) {
      return this.encode(contents, format, width, height, null);
   }

   @Override
   public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) {
      if (contents.isEmpty()) {
         throw new IllegalArgumentException("Found empty contents");
      } else if (format != BarcodeFormat.DATA_MATRIX) {
         throw new IllegalArgumentException("Can only encode DATA_MATRIX, but got " + format);
      } else if (width >= 0 && height >= 0) {
         SymbolShapeHint shape = SymbolShapeHint.FORCE_NONE;
         Dimension minSize = null;
         Dimension maxSize = null;
         if (hints != null) {
            SymbolShapeHint requestedShape = (SymbolShapeHint)hints.get(EncodeHintType.DATA_MATRIX_SHAPE);
            if (requestedShape != null) {
               shape = requestedShape;
            }

            Dimension requestedMinSize = (Dimension)hints.get(EncodeHintType.MIN_SIZE);
            if (requestedMinSize != null) {
               minSize = requestedMinSize;
            }

            Dimension requestedMaxSize = (Dimension)hints.get(EncodeHintType.MAX_SIZE);
            if (requestedMaxSize != null) {
               maxSize = requestedMaxSize;
            }
         }

         boolean hasCompactionHint = hints != null
            && hints.containsKey(EncodeHintType.DATA_MATRIX_COMPACT)
            && Boolean.parseBoolean(hints.get(EncodeHintType.DATA_MATRIX_COMPACT).toString());
         String encoded;
         if (hasCompactionHint) {
            boolean hasGS1FormatHint = hints.containsKey(EncodeHintType.GS1_FORMAT) && Boolean.parseBoolean(hints.get(EncodeHintType.GS1_FORMAT).toString());
            Charset charset = null;
            boolean hasEncodingHint = hints.containsKey(EncodeHintType.CHARACTER_SET);
            if (hasEncodingHint) {
               charset = Charset.forName(hints.get(EncodeHintType.CHARACTER_SET).toString());
            }

            encoded = MinimalEncoder.encodeHighLevel(contents, charset, hasGS1FormatHint ? 29 : -1, shape);
         } else {
            boolean hasForceC40Hint = hints != null
               && hints.containsKey(EncodeHintType.FORCE_C40)
               && Boolean.parseBoolean(hints.get(EncodeHintType.FORCE_C40).toString());
            encoded = HighLevelEncoder.encodeHighLevel(contents, shape, minSize, maxSize, hasForceC40Hint);
         }

         SymbolInfo symbolInfo = SymbolInfo.lookup(encoded.length(), shape, minSize, maxSize, true);
         String codewords = ErrorCorrection.encodeECC200(encoded, symbolInfo);
         DefaultPlacement placement = new DefaultPlacement(codewords, symbolInfo.getSymbolDataWidth(), symbolInfo.getSymbolDataHeight());
         placement.place();
         return encodeLowLevel(placement, symbolInfo, width, height);
      } else {
         throw new IllegalArgumentException("Requested dimensions can't be negative: " + width + 'x' + height);
      }
   }

   private static BitMatrix encodeLowLevel(DefaultPlacement placement, SymbolInfo symbolInfo, int width, int height) {
      int symbolWidth = symbolInfo.getSymbolDataWidth();
      int symbolHeight = symbolInfo.getSymbolDataHeight();
      ByteMatrix matrix = new ByteMatrix(symbolInfo.getSymbolWidth(), symbolInfo.getSymbolHeight());
      int matrixY = 0;

      for (int y = 0; y < symbolHeight; y++) {
         if (y % symbolInfo.matrixHeight == 0) {
            int matrixX = 0;

            for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
               matrix.set(matrixX, matrixY, x % 2 == 0);
               matrixX++;
            }

            matrixY++;
         }

         int matrixX = 0;

         for (int x = 0; x < symbolWidth; x++) {
            if (x % symbolInfo.matrixWidth == 0) {
               matrix.set(matrixX, matrixY, true);
               matrixX++;
            }

            matrix.set(matrixX, matrixY, placement.getBit(x, y));
            matrixX++;
            if (x % symbolInfo.matrixWidth == symbolInfo.matrixWidth - 1) {
               matrix.set(matrixX, matrixY, y % 2 == 0);
               matrixX++;
            }
         }

         matrixY++;
         if (y % symbolInfo.matrixHeight == symbolInfo.matrixHeight - 1) {
            matrixX = 0;

            for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
               matrix.set(matrixX, matrixY, true);
               matrixX++;
            }

            matrixY++;
         }
      }

      return convertByteMatrixToBitMatrix(matrix, width, height);
   }

   private static BitMatrix convertByteMatrixToBitMatrix(ByteMatrix matrix, int reqWidth, int reqHeight) {
      int matrixWidth = matrix.getWidth();
      int matrixHeight = matrix.getHeight();
      int outputWidth = Math.max(reqWidth, matrixWidth);
      int outputHeight = Math.max(reqHeight, matrixHeight);
      int multiple = Math.min(outputWidth / matrixWidth, outputHeight / matrixHeight);
      int leftPadding = (outputWidth - matrixWidth * multiple) / 2;
      int topPadding = (outputHeight - matrixHeight * multiple) / 2;
      BitMatrix output;
      if (reqHeight >= matrixHeight && reqWidth >= matrixWidth) {
         output = new BitMatrix(reqWidth, reqHeight);
      } else {
         leftPadding = 0;
         topPadding = 0;
         output = new BitMatrix(matrixWidth, matrixHeight);
      }

      output.clear();
      int inputY = 0;

      for (int outputY = topPadding; inputY < matrixHeight; outputY += multiple) {
         int inputX = 0;

         for (int outputX = leftPadding; inputX < matrixWidth; outputX += multiple) {
            if (matrix.get(inputX, inputY) == 1) {
               output.setRegion(outputX, outputY, multiple, multiple);
            }

            inputX++;
         }

         inputY++;
      }

      return output;
   }
}
