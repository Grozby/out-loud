package com.google.zxing.pdf417.decoder;

import com.google.zxing.ResultPoint;

final class DetectionResultRowIndicatorColumn extends DetectionResultColumn {
   private final boolean isLeft;

   DetectionResultRowIndicatorColumn(BoundingBox boundingBox, boolean isLeft) {
      super(boundingBox);
      this.isLeft = isLeft;
   }

   private void setRowNumbers() {
      for (Codeword codeword : this.getCodewords()) {
         if (codeword != null) {
            codeword.setRowNumberAsRowIndicatorColumn();
         }
      }
   }

   void adjustCompleteIndicatorColumnRowNumbers(BarcodeMetadata barcodeMetadata) {
      Codeword[] codewords = this.getCodewords();
      this.setRowNumbers();
      this.removeIncorrectCodewords(codewords, barcodeMetadata);
      BoundingBox boundingBox = this.getBoundingBox();
      ResultPoint top = this.isLeft ? boundingBox.getTopLeft() : boundingBox.getTopRight();
      ResultPoint bottom = this.isLeft ? boundingBox.getBottomLeft() : boundingBox.getBottomRight();
      int firstRow = this.imageRowToCodewordIndex((int)top.getY());
      int lastRow = this.imageRowToCodewordIndex((int)bottom.getY());
      int barcodeRow = -1;
      int maxRowHeight = 1;
      int currentRowHeight = 0;

      for (int codewordsRow = firstRow; codewordsRow < lastRow; codewordsRow++) {
         if (codewords[codewordsRow] != null) {
            Codeword codeword = codewords[codewordsRow];
            int rowDifference = codeword.getRowNumber() - barcodeRow;
            if (rowDifference == 0) {
               currentRowHeight++;
            } else if (rowDifference == 1) {
               maxRowHeight = Math.max(maxRowHeight, currentRowHeight);
               currentRowHeight = 1;
               barcodeRow = codeword.getRowNumber();
            } else if (rowDifference >= 0 && codeword.getRowNumber() < barcodeMetadata.getRowCount() && rowDifference <= codewordsRow) {
               int checkedRows;
               if (maxRowHeight > 2) {
                  checkedRows = (maxRowHeight - 2) * rowDifference;
               } else {
                  checkedRows = rowDifference;
               }

               boolean closePreviousCodewordFound = checkedRows >= codewordsRow;

               for (int i = 1; i <= checkedRows && !closePreviousCodewordFound; i++) {
                  closePreviousCodewordFound = codewords[codewordsRow - i] != null;
               }

               if (closePreviousCodewordFound) {
                  codewords[codewordsRow] = null;
               } else {
                  barcodeRow = codeword.getRowNumber();
                  currentRowHeight = 1;
               }
            } else {
               codewords[codewordsRow] = null;
            }
         }
      }
   }

   int[] getRowHeights() {
      BarcodeMetadata barcodeMetadata = this.getBarcodeMetadata();
      if (barcodeMetadata == null) {
         return null;
      } else {
         this.adjustIncompleteIndicatorColumnRowNumbers(barcodeMetadata);
         int[] result = new int[barcodeMetadata.getRowCount()];

         for (Codeword codeword : this.getCodewords()) {
            if (codeword != null) {
               int rowNumber = codeword.getRowNumber();
               if (rowNumber < result.length) {
                  result[rowNumber]++;
               }
            }
         }

         return result;
      }
   }

   private void adjustIncompleteIndicatorColumnRowNumbers(BarcodeMetadata barcodeMetadata) {
      BoundingBox boundingBox = this.getBoundingBox();
      ResultPoint top = this.isLeft ? boundingBox.getTopLeft() : boundingBox.getTopRight();
      ResultPoint bottom = this.isLeft ? boundingBox.getBottomLeft() : boundingBox.getBottomRight();
      int firstRow = this.imageRowToCodewordIndex((int)top.getY());
      int lastRow = this.imageRowToCodewordIndex((int)bottom.getY());
      Codeword[] codewords = this.getCodewords();
      int barcodeRow = -1;
      int maxRowHeight = 1;
      int currentRowHeight = 0;

      for (int codewordsRow = firstRow; codewordsRow < lastRow; codewordsRow++) {
         if (codewords[codewordsRow] != null) {
            Codeword codeword = codewords[codewordsRow];
            codeword.setRowNumberAsRowIndicatorColumn();
            int rowDifference = codeword.getRowNumber() - barcodeRow;
            if (rowDifference == 0) {
               currentRowHeight++;
            } else if (rowDifference == 1) {
               maxRowHeight = Math.max(maxRowHeight, currentRowHeight);
               currentRowHeight = 1;
               barcodeRow = codeword.getRowNumber();
            } else if (codeword.getRowNumber() >= barcodeMetadata.getRowCount()) {
               codewords[codewordsRow] = null;
            } else {
               barcodeRow = codeword.getRowNumber();
               currentRowHeight = 1;
            }
         }
      }
   }

   BarcodeMetadata getBarcodeMetadata() {
      Codeword[] codewords = this.getCodewords();
      BarcodeValue barcodeColumnCount = new BarcodeValue();
      BarcodeValue barcodeRowCountUpperPart = new BarcodeValue();
      BarcodeValue barcodeRowCountLowerPart = new BarcodeValue();
      BarcodeValue barcodeECLevel = new BarcodeValue();

      for (Codeword codeword : codewords) {
         if (codeword != null) {
            codeword.setRowNumberAsRowIndicatorColumn();
            int rowIndicatorValue = codeword.getValue() % 30;
            int codewordRowNumber = codeword.getRowNumber();
            if (!this.isLeft) {
               codewordRowNumber += 2;
            }

            switch (codewordRowNumber % 3) {
               case 0:
                  barcodeRowCountUpperPart.setValue(rowIndicatorValue * 3 + 1);
                  break;
               case 1:
                  barcodeECLevel.setValue(rowIndicatorValue / 3);
                  barcodeRowCountLowerPart.setValue(rowIndicatorValue % 3);
                  break;
               case 2:
                  barcodeColumnCount.setValue(rowIndicatorValue + 1);
            }
         }
      }

      if (barcodeColumnCount.getValue().length != 0
         && barcodeRowCountUpperPart.getValue().length != 0
         && barcodeRowCountLowerPart.getValue().length != 0
         && barcodeECLevel.getValue().length != 0
         && barcodeColumnCount.getValue()[0] >= 1
         && barcodeRowCountUpperPart.getValue()[0] + barcodeRowCountLowerPart.getValue()[0] >= 3
         && barcodeRowCountUpperPart.getValue()[0] + barcodeRowCountLowerPart.getValue()[0] <= 90) {
         BarcodeMetadata barcodeMetadata = new BarcodeMetadata(
            barcodeColumnCount.getValue()[0], barcodeRowCountUpperPart.getValue()[0], barcodeRowCountLowerPart.getValue()[0], barcodeECLevel.getValue()[0]
         );
         this.removeIncorrectCodewords(codewords, barcodeMetadata);
         return barcodeMetadata;
      } else {
         return null;
      }
   }

   private void removeIncorrectCodewords(Codeword[] codewords, BarcodeMetadata barcodeMetadata) {
      for (int codewordRow = 0; codewordRow < codewords.length; codewordRow++) {
         Codeword codeword = codewords[codewordRow];
         if (codewords[codewordRow] != null) {
            int rowIndicatorValue = codeword.getValue() % 30;
            int codewordRowNumber = codeword.getRowNumber();
            if (codewordRowNumber > barcodeMetadata.getRowCount()) {
               codewords[codewordRow] = null;
            } else {
               if (!this.isLeft) {
                  codewordRowNumber += 2;
               }

               switch (codewordRowNumber % 3) {
                  case 0:
                     if (rowIndicatorValue * 3 + 1 != barcodeMetadata.getRowCountUpperPart()) {
                        codewords[codewordRow] = null;
                     }
                     break;
                  case 1:
                     if (rowIndicatorValue / 3 != barcodeMetadata.getErrorCorrectionLevel() || rowIndicatorValue % 3 != barcodeMetadata.getRowCountLowerPart()) {
                        codewords[codewordRow] = null;
                     }
                     break;
                  case 2:
                     if (rowIndicatorValue + 1 != barcodeMetadata.getColumnCount()) {
                        codewords[codewordRow] = null;
                     }
               }
            }
         }
      }
   }

   boolean isLeft() {
      return this.isLeft;
   }

   @Override
   public String toString() {
      return "IsLeft: " + this.isLeft + '\n' + super.toString();
   }
}
