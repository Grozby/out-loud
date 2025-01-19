package com.google.zxing.aztec.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Decoder {
   private static final String[] UPPER_TABLE = new String[]{
      "CTRL_PS",
      " ",
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "H",
      "I",
      "J",
      "K",
      "L",
      "M",
      "N",
      "O",
      "P",
      "Q",
      "R",
      "S",
      "T",
      "U",
      "V",
      "W",
      "X",
      "Y",
      "Z",
      "CTRL_LL",
      "CTRL_ML",
      "CTRL_DL",
      "CTRL_BS"
   };
   private static final String[] LOWER_TABLE = new String[]{
      "CTRL_PS",
      " ",
      "a",
      "b",
      "c",
      "d",
      "e",
      "f",
      "g",
      "h",
      "i",
      "j",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "q",
      "r",
      "s",
      "t",
      "u",
      "v",
      "w",
      "x",
      "y",
      "z",
      "CTRL_US",
      "CTRL_ML",
      "CTRL_DL",
      "CTRL_BS"
   };
   private static final String[] MIXED_TABLE = new String[]{
      "CTRL_PS",
      " ",
      "\u0001",
      "\u0002",
      "\u0003",
      "\u0004",
      "\u0005",
      "\u0006",
      "\u0007",
      "\b",
      "\t",
      "\n",
      "\u000b",
      "\f",
      "\r",
      "\u001b",
      "\u001c",
      "\u001d",
      "\u001e",
      "\u001f",
      "@",
      "\\",
      "^",
      "_",
      "`",
      "|",
      "~",
      "\u007f",
      "CTRL_LL",
      "CTRL_UL",
      "CTRL_PL",
      "CTRL_BS"
   };
   private static final String[] PUNCT_TABLE = new String[]{
      "FLG(n)",
      "\r",
      "\r\n",
      ". ",
      ", ",
      ": ",
      "!",
      "\"",
      "#",
      "$",
      "%",
      "&",
      "'",
      "(",
      ")",
      "*",
      "+",
      ",",
      "-",
      ".",
      "/",
      ":",
      ";",
      "<",
      "=",
      ">",
      "?",
      "[",
      "]",
      "{",
      "}",
      "CTRL_UL"
   };
   private static final String[] DIGIT_TABLE = new String[]{"CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"};
   private static final Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1;
   private AztecDetectorResult ddata;

   public DecoderResult decode(AztecDetectorResult detectorResult) throws FormatException {
      this.ddata = detectorResult;
      BitMatrix matrix = detectorResult.getBits();
      boolean[] rawbits = this.extractBits(matrix);
      Decoder.CorrectedBitsResult correctedBits = this.correctBits(rawbits);
      byte[] rawBytes = convertBoolArrayToByteArray(correctedBits.correctBits);
      String result = getEncodedData(correctedBits.correctBits);
      DecoderResult decoderResult = new DecoderResult(rawBytes, result, null, String.format("%d%%", correctedBits.ecLevel));
      decoderResult.setNumBits(correctedBits.correctBits.length);
      decoderResult.setErrorsCorrected(correctedBits.errorsCorrected);
      return decoderResult;
   }

   public static String highLevelDecode(boolean[] correctedBits) throws FormatException {
      return getEncodedData(correctedBits);
   }

   private static String getEncodedData(boolean[] correctedBits) throws FormatException {
      int endIndex = correctedBits.length;
      Decoder.Table latchTable = Decoder.Table.UPPER;
      Decoder.Table shiftTable = Decoder.Table.UPPER;
      StringBuilder result = new StringBuilder((correctedBits.length - 5) / 4);
      ByteArrayOutputStream decodedBytes = new ByteArrayOutputStream();
      Charset encoding = DEFAULT_ENCODING;
      int index = 0;

      while (index < endIndex) {
         if (shiftTable == Decoder.Table.BINARY) {
            if (endIndex - index < 5) {
               break;
            }

            int length = readCode(correctedBits, index, 5);
            index += 5;
            if (length == 0) {
               if (endIndex - index < 11) {
                  break;
               }

               length = readCode(correctedBits, index, 11) + 31;
               index += 11;
            }

            int charCount = 0;

            while (true) {
               if (charCount < length) {
                  if (endIndex - index >= 8) {
                     int code = readCode(correctedBits, index, 8);
                     decodedBytes.write((byte)code);
                     index += 8;
                     charCount++;
                     continue;
                  }

                  index = endIndex;
               }

               shiftTable = latchTable;
               break;
            }
         } else {
            int size = shiftTable == Decoder.Table.DIGIT ? 4 : 5;
            if (endIndex - index < size) {
               break;
            }

            int code = readCode(correctedBits, index, size);
            index += size;
            String str = getCharacter(shiftTable, code);
            if (!"FLG(n)".equals(str)) {
               if (str.startsWith("CTRL_")) {
                  latchTable = shiftTable;
                  shiftTable = getTable(str.charAt(5));
                  if (str.charAt(6) == 'L') {
                     latchTable = shiftTable;
                  }
               } else {
                  byte[] b = str.getBytes(StandardCharsets.US_ASCII);
                  decodedBytes.write(b, 0, b.length);
                  shiftTable = latchTable;
               }
            } else {
               if (endIndex - index >= 3) {
                  int n = readCode(correctedBits, index, 3);
                  index += 3;

                  try {
                     result.append(decodedBytes.toString(encoding.name()));
                  } catch (UnsupportedEncodingException var15) {
                     throw new IllegalStateException(var15);
                  }

                  decodedBytes.reset();
                  switch (n) {
                     case 0:
                        result.append('\u001d');
                        break;
                     case 7:
                        throw FormatException.getFormatInstance();
                     default:
                        int eci = 0;
                        if (endIndex - index >= 4 * n) {
                           while (n-- > 0) {
                              int nextDigit = readCode(correctedBits, index, 4);
                              index += 4;
                              if (nextDigit < 2 || nextDigit > 11) {
                                 throw FormatException.getFormatInstance();
                              }

                              eci = eci * 10 + (nextDigit - 2);
                           }

                           CharacterSetECI charsetECI = CharacterSetECI.getCharacterSetECIByValue(eci);
                           if (charsetECI == null) {
                              throw FormatException.getFormatInstance();
                           }

                           encoding = charsetECI.getCharset();
                        }
                  }

                  shiftTable = latchTable;
                  continue;
               }
               break;
            }
         }
      }

      try {
         result.append(decodedBytes.toString(encoding.name()));
      } catch (UnsupportedEncodingException var14) {
         throw new IllegalStateException(var14);
      }

      return result.toString();
   }

   private static Decoder.Table getTable(char t) {
      switch (t) {
         case 'B':
            return Decoder.Table.BINARY;
         case 'C':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'N':
         case 'O':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         default:
            return Decoder.Table.UPPER;
         case 'D':
            return Decoder.Table.DIGIT;
         case 'L':
            return Decoder.Table.LOWER;
         case 'M':
            return Decoder.Table.MIXED;
         case 'P':
            return Decoder.Table.PUNCT;
      }
   }

   private static String getCharacter(Decoder.Table table, int code) {
      switch (table) {
         case UPPER:
            return UPPER_TABLE[code];
         case LOWER:
            return LOWER_TABLE[code];
         case MIXED:
            return MIXED_TABLE[code];
         case PUNCT:
            return PUNCT_TABLE[code];
         case DIGIT:
            return DIGIT_TABLE[code];
         default:
            throw new IllegalStateException("Bad table");
      }
   }

   private Decoder.CorrectedBitsResult correctBits(boolean[] rawbits) throws FormatException {
      GenericGF gf;
      int codewordSize;
      if (this.ddata.getNbLayers() <= 2) {
         codewordSize = 6;
         gf = GenericGF.AZTEC_DATA_6;
      } else if (this.ddata.getNbLayers() <= 8) {
         codewordSize = 8;
         gf = GenericGF.AZTEC_DATA_8;
      } else if (this.ddata.getNbLayers() <= 22) {
         codewordSize = 10;
         gf = GenericGF.AZTEC_DATA_10;
      } else {
         codewordSize = 12;
         gf = GenericGF.AZTEC_DATA_12;
      }

      int numDataCodewords = this.ddata.getNbDatablocks();
      int numCodewords = rawbits.length / codewordSize;
      if (numCodewords < numDataCodewords) {
         throw FormatException.getFormatInstance();
      } else {
         int offset = rawbits.length % codewordSize;
         int[] dataWords = new int[numCodewords];

         for (int i = 0; i < numCodewords; offset += codewordSize) {
            dataWords[i] = readCode(rawbits, offset, codewordSize);
            i++;
         }

         int errorsCorrected = 0;

         try {
            ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(gf);
            errorsCorrected = rsDecoder.decodeWithECCount(dataWords, numCodewords - numDataCodewords);
         } catch (ReedSolomonException var16) {
            throw FormatException.getFormatInstance(var16);
         }

         int mask = (1 << codewordSize) - 1;
         int stuffedBits = 0;

         for (int i = 0; i < numDataCodewords; i++) {
            int dataWord = dataWords[i];
            if (dataWord == 0 || dataWord == mask) {
               throw FormatException.getFormatInstance();
            }

            if (dataWord == 1 || dataWord == mask - 1) {
               stuffedBits++;
            }
         }

         boolean[] correctedBits = new boolean[numDataCodewords * codewordSize - stuffedBits];
         int index = 0;

         for (int i = 0; i < numDataCodewords; i++) {
            int dataWordx = dataWords[i];
            if (dataWordx != 1 && dataWordx != mask - 1) {
               for (int bit = codewordSize - 1; bit >= 0; bit--) {
                  correctedBits[index++] = (dataWordx & 1 << bit) != 0;
               }
            } else {
               Arrays.fill(correctedBits, index, index + codewordSize - 1, dataWordx > 1);
               index += codewordSize - 1;
            }
         }

         int ecLevel = 100 * (numCodewords - numDataCodewords) / numCodewords;
         return new Decoder.CorrectedBitsResult(correctedBits, errorsCorrected, ecLevel);
      }
   }

   private boolean[] extractBits(BitMatrix matrix) {
      boolean compact = this.ddata.isCompact();
      int layers = this.ddata.getNbLayers();
      int baseMatrixSize = (compact ? 11 : 14) + layers * 4;
      int[] alignmentMap = new int[baseMatrixSize];
      boolean[] rawbits = new boolean[totalBitsInLayer(layers, compact)];
      if (compact) {
         int i = 0;

         while (i < alignmentMap.length) {
            alignmentMap[i] = i++;
         }
      } else {
         int matrixSize = baseMatrixSize + 1 + 2 * ((baseMatrixSize / 2 - 1) / 15);
         int origCenter = baseMatrixSize / 2;
         int center = matrixSize / 2;

         for (int i = 0; i < origCenter; i++) {
            int newOffset = i + i / 15;
            alignmentMap[origCenter - i - 1] = center - newOffset - 1;
            alignmentMap[origCenter + i] = center + newOffset + 1;
         }
      }

      int i = 0;

      for (int rowOffset = 0; i < layers; i++) {
         int rowSize = (layers - i) * 4 + (compact ? 9 : 12);
         int low = i * 2;
         int high = baseMatrixSize - 1 - low;

         for (int j = 0; j < rowSize; j++) {
            int columnOffset = j * 2;

            for (int k = 0; k < 2; k++) {
               rawbits[rowOffset + columnOffset + k] = matrix.get(alignmentMap[low + k], alignmentMap[low + j]);
               rawbits[rowOffset + 2 * rowSize + columnOffset + k] = matrix.get(alignmentMap[low + j], alignmentMap[high - k]);
               rawbits[rowOffset + 4 * rowSize + columnOffset + k] = matrix.get(alignmentMap[high - k], alignmentMap[high - j]);
               rawbits[rowOffset + 6 * rowSize + columnOffset + k] = matrix.get(alignmentMap[high - j], alignmentMap[low + k]);
            }
         }

         rowOffset += rowSize * 8;
      }

      return rawbits;
   }

   private static int readCode(boolean[] rawbits, int startIndex, int length) {
      int res = 0;

      for (int i = startIndex; i < startIndex + length; i++) {
         res <<= 1;
         if (rawbits[i]) {
            res |= 1;
         }
      }

      return res;
   }

   private static byte readByte(boolean[] rawbits, int startIndex) {
      int n = rawbits.length - startIndex;
      return n >= 8 ? (byte)readCode(rawbits, startIndex, 8) : (byte)(readCode(rawbits, startIndex, n) << 8 - n);
   }

   static byte[] convertBoolArrayToByteArray(boolean[] boolArr) {
      byte[] byteArr = new byte[(boolArr.length + 7) / 8];

      for (int i = 0; i < byteArr.length; i++) {
         byteArr[i] = readByte(boolArr, 8 * i);
      }

      return byteArr;
   }

   private static int totalBitsInLayer(int layers, boolean compact) {
      return ((compact ? 88 : 112) + 16 * layers) * layers;
   }

   static final class CorrectedBitsResult {
      private final boolean[] correctBits;
      private final int errorsCorrected;
      private final int ecLevel;

      CorrectedBitsResult(boolean[] correctBits, int errorsCorrected, int ecLevel) {
         this.correctBits = correctBits;
         this.errorsCorrected = errorsCorrected;
         this.ecLevel = ecLevel;
      }
   }

   private static enum Table {
      UPPER,
      LOWER,
      MIXED,
      DIGIT,
      PUNCT,
      BINARY;
   }
}
