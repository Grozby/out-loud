package com.google.zxing.qrcode.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitMatrix;

final class BitMatrixParser {
   private final BitMatrix bitMatrix;
   private Version parsedVersion;
   private FormatInformation parsedFormatInfo;
   private boolean mirror;

   BitMatrixParser(BitMatrix bitMatrix) throws FormatException {
      int dimension = bitMatrix.getHeight();
      if (dimension >= 21 && (dimension & 3) == 1) {
         this.bitMatrix = bitMatrix;
      } else {
         throw FormatException.getFormatInstance();
      }
   }

   FormatInformation readFormatInformation() throws FormatException {
      if (this.parsedFormatInfo != null) {
         return this.parsedFormatInfo;
      } else {
         int formatInfoBits1 = 0;

         for (int i = 0; i < 6; i++) {
            formatInfoBits1 = this.copyBit(i, 8, formatInfoBits1);
         }

         formatInfoBits1 = this.copyBit(7, 8, formatInfoBits1);
         formatInfoBits1 = this.copyBit(8, 8, formatInfoBits1);
         formatInfoBits1 = this.copyBit(8, 7, formatInfoBits1);

         for (int j = 5; j >= 0; j--) {
            formatInfoBits1 = this.copyBit(8, j, formatInfoBits1);
         }

         int dimension = this.bitMatrix.getHeight();
         int formatInfoBits2 = 0;
         int jMin = dimension - 7;

         for (int j = dimension - 1; j >= jMin; j--) {
            formatInfoBits2 = this.copyBit(8, j, formatInfoBits2);
         }

         for (int i = dimension - 8; i < dimension; i++) {
            formatInfoBits2 = this.copyBit(i, 8, formatInfoBits2);
         }

         this.parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits1, formatInfoBits2);
         if (this.parsedFormatInfo != null) {
            return this.parsedFormatInfo;
         } else {
            throw FormatException.getFormatInstance();
         }
      }
   }

   Version readVersion() throws FormatException {
      if (this.parsedVersion != null) {
         return this.parsedVersion;
      } else {
         int dimension = this.bitMatrix.getHeight();
         int provisionalVersion = (dimension - 17) / 4;
         if (provisionalVersion <= 6) {
            return Version.getVersionForNumber(provisionalVersion);
         } else {
            int versionBits = 0;
            int ijMin = dimension - 11;

            for (int j = 5; j >= 0; j--) {
               for (int i = dimension - 9; i >= ijMin; i--) {
                  versionBits = this.copyBit(i, j, versionBits);
               }
            }

            Version theParsedVersion = Version.decodeVersionInformation(versionBits);
            if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
               this.parsedVersion = theParsedVersion;
               return theParsedVersion;
            } else {
               versionBits = 0;

               for (int i = 5; i >= 0; i--) {
                  for (int j = dimension - 9; j >= ijMin; j--) {
                     versionBits = this.copyBit(i, j, versionBits);
                  }
               }

               theParsedVersion = Version.decodeVersionInformation(versionBits);
               if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
                  this.parsedVersion = theParsedVersion;
                  return theParsedVersion;
               } else {
                  throw FormatException.getFormatInstance();
               }
            }
         }
      }
   }

   private int copyBit(int i, int j, int versionBits) {
      boolean bit = this.mirror ? this.bitMatrix.get(j, i) : this.bitMatrix.get(i, j);
      return bit ? versionBits << 1 | 1 : versionBits << 1;
   }

   byte[] readCodewords() throws FormatException {
      FormatInformation formatInfo = this.readFormatInformation();
      Version version = this.readVersion();
      DataMask dataMask = DataMask.values()[formatInfo.getDataMask()];
      int dimension = this.bitMatrix.getHeight();
      dataMask.unmaskBitMatrix(this.bitMatrix, dimension);
      BitMatrix functionPattern = version.buildFunctionPattern();
      boolean readingUp = true;
      byte[] result = new byte[version.getTotalCodewords()];
      int resultOffset = 0;
      int currentByte = 0;
      int bitsRead = 0;

      for (int j = dimension - 1; j > 0; j -= 2) {
         if (j == 6) {
            j--;
         }

         for (int count = 0; count < dimension; count++) {
            int i = readingUp ? dimension - 1 - count : count;

            for (int col = 0; col < 2; col++) {
               if (!functionPattern.get(j - col, i)) {
                  bitsRead++;
                  currentByte <<= 1;
                  if (this.bitMatrix.get(j - col, i)) {
                     currentByte |= 1;
                  }

                  if (bitsRead == 8) {
                     result[resultOffset++] = (byte)currentByte;
                     bitsRead = 0;
                     currentByte = 0;
                  }
               }
            }
         }

         readingUp ^= true;
      }

      if (resultOffset != version.getTotalCodewords()) {
         throw FormatException.getFormatInstance();
      } else {
         return result;
      }
   }

   void remask() {
      if (this.parsedFormatInfo != null) {
         DataMask dataMask = DataMask.values()[this.parsedFormatInfo.getDataMask()];
         int dimension = this.bitMatrix.getHeight();
         dataMask.unmaskBitMatrix(this.bitMatrix, dimension);
      }
   }

   void setMirror(boolean mirror) {
      this.parsedVersion = null;
      this.parsedFormatInfo = null;
      this.mirror = mirror;
   }

   void mirror() {
      for (int x = 0; x < this.bitMatrix.getWidth(); x++) {
         for (int y = x + 1; y < this.bitMatrix.getHeight(); y++) {
            if (this.bitMatrix.get(x, y) != this.bitMatrix.get(y, x)) {
               this.bitMatrix.flip(y, x);
               this.bitMatrix.flip(x, y);
            }
         }
      }
   }
}
