package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.ECIStringBuilder;
import com.google.zxing.pdf417.PDF417ResultMetadata;
import java.math.BigInteger;
import java.util.Arrays;

final class DecodedBitStreamParser {
   private static final int TEXT_COMPACTION_MODE_LATCH = 900;
   private static final int BYTE_COMPACTION_MODE_LATCH = 901;
   private static final int NUMERIC_COMPACTION_MODE_LATCH = 902;
   private static final int BYTE_COMPACTION_MODE_LATCH_6 = 924;
   private static final int ECI_USER_DEFINED = 925;
   private static final int ECI_GENERAL_PURPOSE = 926;
   private static final int ECI_CHARSET = 927;
   private static final int BEGIN_MACRO_PDF417_CONTROL_BLOCK = 928;
   private static final int BEGIN_MACRO_PDF417_OPTIONAL_FIELD = 923;
   private static final int MACRO_PDF417_TERMINATOR = 922;
   private static final int MODE_SHIFT_TO_BYTE_COMPACTION_MODE = 913;
   private static final int MAX_NUMERIC_CODEWORDS = 15;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_FILE_NAME = 0;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_SEGMENT_COUNT = 1;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_TIME_STAMP = 2;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_SENDER = 3;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_ADDRESSEE = 4;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_FILE_SIZE = 5;
   private static final int MACRO_PDF417_OPTIONAL_FIELD_CHECKSUM = 6;
   private static final int PL = 25;
   private static final int LL = 27;
   private static final int AS = 27;
   private static final int ML = 28;
   private static final int AL = 28;
   private static final int PS = 29;
   private static final int PAL = 29;
   private static final char[] PUNCT_CHARS = ";<>@[\\]_`~!\r\t,:\n-.$/\"|*()?{}'".toCharArray();
   private static final char[] MIXED_CHARS = "0123456789&\r\t,:#-.$/+%*=^".toCharArray();
   private static final BigInteger[] EXP900 = new BigInteger[16];
   private static final int NUMBER_OF_SEQUENCE_CODEWORDS = 2;

   private DecodedBitStreamParser() {
   }

   static DecoderResult decode(int[] codewords, String ecLevel) throws FormatException {
      ECIStringBuilder result = new ECIStringBuilder(codewords.length * 2);
      int codeIndex = textCompaction(codewords, 1, result);
      PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

      while (codeIndex < codewords[0]) {
         int code = codewords[codeIndex++];
         switch (code) {
            case 900:
               codeIndex = textCompaction(codewords, codeIndex, result);
               break;
            case 901:
            case 924:
               codeIndex = byteCompaction(code, codewords, codeIndex, result);
               break;
            case 902:
               codeIndex = numericCompaction(codewords, codeIndex, result);
               break;
            case 903:
            case 904:
            case 905:
            case 906:
            case 907:
            case 908:
            case 909:
            case 910:
            case 911:
            case 912:
            case 914:
            case 915:
            case 916:
            case 917:
            case 918:
            case 919:
            case 920:
            case 921:
            default:
               codeIndex = textCompaction(codewords, --codeIndex, result);
               break;
            case 913:
               result.append((char)codewords[codeIndex++]);
               break;
            case 922:
            case 923:
               throw FormatException.getFormatInstance();
            case 925:
               codeIndex++;
               break;
            case 926:
               codeIndex += 2;
               break;
            case 927:
               result.appendECI(codewords[codeIndex++]);
               break;
            case 928:
               codeIndex = decodeMacroBlock(codewords, codeIndex, resultMetadata);
         }
      }

      if (result.isEmpty() && resultMetadata.getFileId() == null) {
         throw FormatException.getFormatInstance();
      } else {
         DecoderResult decoderResult = new DecoderResult(null, result.toString(), null, ecLevel);
         decoderResult.setOther(resultMetadata);
         return decoderResult;
      }
   }

   static int decodeMacroBlock(int[] codewords, int codeIndex, PDF417ResultMetadata resultMetadata) throws FormatException {
      if (codeIndex + 2 > codewords[0]) {
         throw FormatException.getFormatInstance();
      } else {
         int[] segmentIndexArray = new int[2];

         for (int i = 0; i < 2; codeIndex++) {
            segmentIndexArray[i] = codewords[codeIndex];
            i++;
         }

         String segmentIndexString = decodeBase900toBase10(segmentIndexArray, 2);
         if (segmentIndexString.isEmpty()) {
            resultMetadata.setSegmentIndex(0);
         } else {
            try {
               resultMetadata.setSegmentIndex(Integer.parseInt(segmentIndexString));
            } catch (NumberFormatException var19) {
               throw FormatException.getFormatInstance();
            }
         }

         StringBuilder fileId;
         for (fileId = new StringBuilder();
            codeIndex < codewords[0] && codeIndex < codewords.length && codewords[codeIndex] != 922 && codewords[codeIndex] != 923;
            codeIndex++
         ) {
            fileId.append(String.format("%03d", codewords[codeIndex]));
         }

         if (fileId.length() == 0) {
            throw FormatException.getFormatInstance();
         } else {
            resultMetadata.setFileId(fileId.toString());
            int optionalFieldsStart = -1;
            if (codewords[codeIndex] == 923) {
               optionalFieldsStart = codeIndex + 1;
            }

            while (codeIndex < codewords[0]) {
               switch (codewords[codeIndex]) {
                  case 922:
                     codeIndex++;
                     resultMetadata.setLastSegment(true);
                     break;
                  case 923:
                     codeIndex++;
                     switch (codewords[codeIndex]) {
                        case 0:
                           ECIStringBuilder fileName = new ECIStringBuilder();
                           codeIndex = textCompaction(codewords, codeIndex + 1, fileName);
                           resultMetadata.setFileName(fileName.toString());
                           continue;
                        case 1:
                           ECIStringBuilder segmentCount = new ECIStringBuilder();
                           codeIndex = numericCompaction(codewords, codeIndex + 1, segmentCount);

                           try {
                              resultMetadata.setSegmentCount(Integer.parseInt(segmentCount.toString()));
                              continue;
                           } catch (NumberFormatException var18) {
                              throw FormatException.getFormatInstance();
                           }
                        case 2:
                           ECIStringBuilder timestamp = new ECIStringBuilder();
                           codeIndex = numericCompaction(codewords, codeIndex + 1, timestamp);

                           try {
                              resultMetadata.setTimestamp(Long.parseLong(timestamp.toString()));
                              continue;
                           } catch (NumberFormatException var17) {
                              throw FormatException.getFormatInstance();
                           }
                        case 3:
                           ECIStringBuilder sender = new ECIStringBuilder();
                           codeIndex = textCompaction(codewords, codeIndex + 1, sender);
                           resultMetadata.setSender(sender.toString());
                           continue;
                        case 4:
                           ECIStringBuilder addressee = new ECIStringBuilder();
                           codeIndex = textCompaction(codewords, codeIndex + 1, addressee);
                           resultMetadata.setAddressee(addressee.toString());
                           continue;
                        case 5:
                           ECIStringBuilder fileSize = new ECIStringBuilder();
                           codeIndex = numericCompaction(codewords, codeIndex + 1, fileSize);

                           try {
                              resultMetadata.setFileSize(Long.parseLong(fileSize.toString()));
                              continue;
                           } catch (NumberFormatException var15) {
                              throw FormatException.getFormatInstance();
                           }
                        case 6:
                           ECIStringBuilder checksum = new ECIStringBuilder();
                           codeIndex = numericCompaction(codewords, codeIndex + 1, checksum);

                           try {
                              resultMetadata.setChecksum(Integer.parseInt(checksum.toString()));
                              continue;
                           } catch (NumberFormatException var16) {
                              throw FormatException.getFormatInstance();
                           }
                        default:
                           throw FormatException.getFormatInstance();
                     }
                  default:
                     throw FormatException.getFormatInstance();
               }
            }

            if (optionalFieldsStart != -1) {
               int optionalFieldsLength = codeIndex - optionalFieldsStart;
               if (resultMetadata.isLastSegment()) {
                  optionalFieldsLength--;
               }

               if (optionalFieldsLength > 0) {
                  resultMetadata.setOptionalData(Arrays.copyOfRange(codewords, optionalFieldsStart, optionalFieldsStart + optionalFieldsLength));
               }
            }

            return codeIndex;
         }
      }
   }

   private static int textCompaction(int[] codewords, int codeIndex, ECIStringBuilder result) throws FormatException {
      int[] textCompactionData = new int[(codewords[0] - codeIndex) * 2];
      int[] byteCompactionData = new int[(codewords[0] - codeIndex) * 2];
      int index = 0;
      boolean end = false;
      DecodedBitStreamParser.Mode subMode = DecodedBitStreamParser.Mode.ALPHA;

      while (codeIndex < codewords[0] && !end) {
         int code = codewords[codeIndex++];
         if (code < 900) {
            textCompactionData[index] = code / 30;
            textCompactionData[index + 1] = code % 30;
            index += 2;
         } else {
            switch (code) {
               case 900:
                  textCompactionData[index++] = 900;
                  break;
               case 901:
               case 902:
               case 922:
               case 923:
               case 924:
               case 928:
                  codeIndex--;
                  end = true;
               case 903:
               case 904:
               case 905:
               case 906:
               case 907:
               case 908:
               case 909:
               case 910:
               case 911:
               case 912:
               case 914:
               case 915:
               case 916:
               case 917:
               case 918:
               case 919:
               case 920:
               case 921:
               case 925:
               case 926:
               default:
                  break;
               case 913:
                  textCompactionData[index] = 913;
                  code = codewords[codeIndex++];
                  byteCompactionData[index] = code;
                  index++;
                  break;
               case 927:
                  subMode = decodeTextCompaction(textCompactionData, byteCompactionData, index, result, subMode);
                  result.appendECI(codewords[codeIndex++]);
                  if (codeIndex > codewords[0]) {
                     throw FormatException.getFormatInstance();
                  }

                  textCompactionData = new int[(codewords[0] - codeIndex) * 2];
                  byteCompactionData = new int[(codewords[0] - codeIndex) * 2];
                  index = 0;
            }
         }
      }

      decodeTextCompaction(textCompactionData, byteCompactionData, index, result, subMode);
      return codeIndex;
   }

   private static DecodedBitStreamParser.Mode decodeTextCompaction(
      int[] textCompactionData, int[] byteCompactionData, int length, ECIStringBuilder result, DecodedBitStreamParser.Mode startMode
   ) {
      DecodedBitStreamParser.Mode subMode = startMode;
      DecodedBitStreamParser.Mode priorToShiftMode = startMode;
      DecodedBitStreamParser.Mode latchedMode = startMode;

      for (int i = 0; i < length; i++) {
         char ch;
         int subModeCh = textCompactionData[i];
         ch = 0;
         label79:
         switch (subMode) {
            case ALPHA:
               if (subModeCh < 26) {
                  ch = (char)(65 + subModeCh);
               } else {
                  switch (subModeCh) {
                     case 26:
                        ch = ' ';
                        break label79;
                     case 27:
                        subMode = DecodedBitStreamParser.Mode.LOWER;
                        latchedMode = subMode;
                        break label79;
                     case 28:
                        subMode = DecodedBitStreamParser.Mode.MIXED;
                        latchedMode = subMode;
                        break label79;
                     case 29:
                        priorToShiftMode = subMode;
                        subMode = DecodedBitStreamParser.Mode.PUNCT_SHIFT;
                        break label79;
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                        latchedMode = subMode;
                        break label79;
                     case 913:
                        result.append((char)byteCompactionData[i]);
                  }
               }
               break;
            case LOWER:
               if (subModeCh < 26) {
                  ch = (char)(97 + subModeCh);
               } else {
                  switch (subModeCh) {
                     case 26:
                        ch = ' ';
                        break label79;
                     case 27:
                        priorToShiftMode = subMode;
                        subMode = DecodedBitStreamParser.Mode.ALPHA_SHIFT;
                        break label79;
                     case 28:
                        subMode = DecodedBitStreamParser.Mode.MIXED;
                        latchedMode = subMode;
                        break label79;
                     case 29:
                        priorToShiftMode = subMode;
                        subMode = DecodedBitStreamParser.Mode.PUNCT_SHIFT;
                        break label79;
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                        latchedMode = subMode;
                        break label79;
                     case 913:
                        result.append((char)byteCompactionData[i]);
                  }
               }
               break;
            case MIXED:
               if (subModeCh < 25) {
                  ch = MIXED_CHARS[subModeCh];
               } else {
                  switch (subModeCh) {
                     case 25:
                        subMode = DecodedBitStreamParser.Mode.PUNCT;
                        latchedMode = subMode;
                        break label79;
                     case 26:
                        ch = ' ';
                        break label79;
                     case 27:
                        subMode = DecodedBitStreamParser.Mode.LOWER;
                        latchedMode = subMode;
                        break label79;
                     case 28:
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                        latchedMode = subMode;
                        break label79;
                     case 29:
                        priorToShiftMode = subMode;
                        subMode = DecodedBitStreamParser.Mode.PUNCT_SHIFT;
                        break label79;
                     case 913:
                        result.append((char)byteCompactionData[i]);
                  }
               }
               break;
            case PUNCT:
               if (subModeCh < 29) {
                  ch = PUNCT_CHARS[subModeCh];
               } else {
                  switch (subModeCh) {
                     case 29:
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                        latchedMode = subMode;
                        break label79;
                     case 913:
                        result.append((char)byteCompactionData[i]);
                  }
               }
               break;
            case ALPHA_SHIFT:
               subMode = priorToShiftMode;
               if (subModeCh < 26) {
                  ch = (char)(65 + subModeCh);
               } else {
                  switch (subModeCh) {
                     case 26:
                        ch = ' ';
                        break label79;
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                  }
               }
               break;
            case PUNCT_SHIFT:
               subMode = priorToShiftMode;
               if (subModeCh < 29) {
                  ch = PUNCT_CHARS[subModeCh];
               } else {
                  switch (subModeCh) {
                     case 29:
                     case 900:
                        subMode = DecodedBitStreamParser.Mode.ALPHA;
                        break;
                     case 913:
                        result.append((char)byteCompactionData[i]);
                  }
               }
         }

         if (ch != 0) {
            result.append(ch);
         }
      }

      return latchedMode;
   }

   private static int byteCompaction(int mode, int[] codewords, int codeIndex, ECIStringBuilder result) throws FormatException {
      boolean end = false;

      while (codeIndex < codewords[0] && !end) {
         while (codeIndex < codewords[0] && codewords[codeIndex] == 927) {
            result.appendECI(codewords[++codeIndex]);
            codeIndex++;
         }

         if (codeIndex < codewords[0] && codewords[codeIndex] < 900) {
            long value = 0L;
            int count = 0;

            do {
               value = 900L * value + (long)codewords[codeIndex++];
               count++;
            } while (count < 5 && codeIndex < codewords[0] && codewords[codeIndex] < 900);

            if (count != 5 || mode != 924 && (codeIndex >= codewords[0] || codewords[codeIndex] >= 900)) {
               codeIndex -= count;

               while (codeIndex < codewords[0] && !end) {
                  int code = codewords[codeIndex++];
                  if (code < 900) {
                     result.append((byte)code);
                  } else if (code == 927) {
                     result.appendECI(codewords[codeIndex++]);
                  } else {
                     codeIndex--;
                     end = true;
                  }
               }
            } else {
               for (int i = 0; i < 6; i++) {
                  result.append((byte)((int)(value >> 8 * (5 - i))));
               }
            }
         } else {
            end = true;
         }
      }

      return codeIndex;
   }

   private static int numericCompaction(int[] codewords, int codeIndex, ECIStringBuilder result) throws FormatException {
      int count = 0;
      boolean end = false;
      int[] numericCodewords = new int[15];

      while (codeIndex < codewords[0] && !end) {
         int code = codewords[codeIndex++];
         if (codeIndex == codewords[0]) {
            end = true;
         }

         if (code < 900) {
            numericCodewords[count] = code;
            count++;
         } else {
            switch (code) {
               case 900:
               case 901:
               case 922:
               case 923:
               case 924:
               case 927:
               case 928:
                  codeIndex--;
                  end = true;
            }
         }

         if ((count % 15 == 0 || code == 902 || end) && count > 0) {
            result.append(decodeBase900toBase10(numericCodewords, count));
            count = 0;
         }
      }

      return codeIndex;
   }

   private static String decodeBase900toBase10(int[] codewords, int count) throws FormatException {
      BigInteger result = BigInteger.ZERO;

      for (int i = 0; i < count; i++) {
         result = result.add(EXP900[count - i - 1].multiply(BigInteger.valueOf((long)codewords[i])));
      }

      String resultString = result.toString();
      if (resultString.charAt(0) != '1') {
         throw FormatException.getFormatInstance();
      } else {
         return resultString.substring(1);
      }
   }

   static {
      EXP900[0] = BigInteger.ONE;
      BigInteger nineHundred = BigInteger.valueOf(900L);
      EXP900[1] = nineHundred;

      for (int i = 2; i < EXP900.length; i++) {
         EXP900[i] = EXP900[i - 1].multiply(nineHundred);
      }
   }

   private static enum Mode {
      ALPHA,
      LOWER,
      MIXED,
      PUNCT,
      ALPHA_SHIFT,
      PUNCT_SHIFT;
   }
}
