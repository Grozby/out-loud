package com.google.zxing.datamatrix.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.ECIStringBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DecodedBitStreamParser {
   private static final char[] C40_BASIC_SET_CHARS = new char[]{
      '*',
      '*',
      '*',
      ' ',
      '0',
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
      '7',
      '8',
      '9',
      'A',
      'B',
      'C',
      'D',
      'E',
      'F',
      'G',
      'H',
      'I',
      'J',
      'K',
      'L',
      'M',
      'N',
      'O',
      'P',
      'Q',
      'R',
      'S',
      'T',
      'U',
      'V',
      'W',
      'X',
      'Y',
      'Z'
   };
   private static final char[] C40_SHIFT2_SET_CHARS = new char[]{
      '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'
   };
   private static final char[] TEXT_BASIC_SET_CHARS = new char[]{
      '*',
      '*',
      '*',
      ' ',
      '0',
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
      '7',
      '8',
      '9',
      'a',
      'b',
      'c',
      'd',
      'e',
      'f',
      'g',
      'h',
      'i',
      'j',
      'k',
      'l',
      'm',
      'n',
      'o',
      'p',
      'q',
      'r',
      's',
      't',
      'u',
      'v',
      'w',
      'x',
      'y',
      'z'
   };
   private static final char[] TEXT_SHIFT2_SET_CHARS = C40_SHIFT2_SET_CHARS;
   private static final char[] TEXT_SHIFT3_SET_CHARS = new char[]{
      '`',
      'A',
      'B',
      'C',
      'D',
      'E',
      'F',
      'G',
      'H',
      'I',
      'J',
      'K',
      'L',
      'M',
      'N',
      'O',
      'P',
      'Q',
      'R',
      'S',
      'T',
      'U',
      'V',
      'W',
      'X',
      'Y',
      'Z',
      '{',
      '|',
      '}',
      '~',
      '\u007f'
   };

   private DecodedBitStreamParser() {
   }

   static DecoderResult decode(byte[] bytes) throws FormatException {
      BitSource bits = new BitSource(bytes);
      ECIStringBuilder result = new ECIStringBuilder(100);
      StringBuilder resultTrailer = new StringBuilder(0);
      List<byte[]> byteSegments = new ArrayList<>(1);
      DecodedBitStreamParser.Mode mode = DecodedBitStreamParser.Mode.ASCII_ENCODE;
      Set<Integer> fnc1Positions = new HashSet<>();
      boolean isECIencoded = false;

      do {
         if (mode == DecodedBitStreamParser.Mode.ASCII_ENCODE) {
            mode = decodeAsciiSegment(bits, result, resultTrailer, fnc1Positions);
         } else {
            switch (mode) {
               case C40_ENCODE:
                  decodeC40Segment(bits, result, fnc1Positions);
                  break;
               case TEXT_ENCODE:
                  decodeTextSegment(bits, result, fnc1Positions);
                  break;
               case ANSIX12_ENCODE:
                  decodeAnsiX12Segment(bits, result);
                  break;
               case EDIFACT_ENCODE:
                  decodeEdifactSegment(bits, result);
                  break;
               case BASE256_ENCODE:
                  decodeBase256Segment(bits, result, byteSegments);
                  break;
               case ECI_ENCODE:
                  decodeECISegment(bits, result);
                  isECIencoded = true;
                  break;
               default:
                  throw FormatException.getFormatInstance();
            }

            mode = DecodedBitStreamParser.Mode.ASCII_ENCODE;
         }
      } while (mode != DecodedBitStreamParser.Mode.PAD_ENCODE && bits.available() > 0);

      if (resultTrailer.length() > 0) {
         result.appendCharacters(resultTrailer);
      }

      int symbologyModifier;
      if (isECIencoded) {
         if (fnc1Positions.contains(0) || fnc1Positions.contains(4)) {
            symbologyModifier = 5;
         } else if (!fnc1Positions.contains(1) && !fnc1Positions.contains(5)) {
            symbologyModifier = 4;
         } else {
            symbologyModifier = 6;
         }
      } else if (fnc1Positions.contains(0) || fnc1Positions.contains(4)) {
         symbologyModifier = 2;
      } else if (!fnc1Positions.contains(1) && !fnc1Positions.contains(5)) {
         symbologyModifier = 1;
      } else {
         symbologyModifier = 3;
      }

      return new DecoderResult(bytes, result.toString(), byteSegments.isEmpty() ? null : byteSegments, null, symbologyModifier);
   }

   private static DecodedBitStreamParser.Mode decodeAsciiSegment(
      BitSource bits, ECIStringBuilder result, StringBuilder resultTrailer, Set<Integer> fnc1positions
   ) throws FormatException {
      boolean upperShift = false;

      do {
         int oneByte = bits.readBits(8);
         if (oneByte == 0) {
            throw FormatException.getFormatInstance();
         }

         if (oneByte <= 128) {
            if (upperShift) {
               oneByte += 128;
            }

            result.append((char)(oneByte - 1));
            return DecodedBitStreamParser.Mode.ASCII_ENCODE;
         }

         if (oneByte == 129) {
            return DecodedBitStreamParser.Mode.PAD_ENCODE;
         }

         if (oneByte <= 229) {
            int value = oneByte - 130;
            if (value < 10) {
               result.append('0');
            }

            result.append(value);
         } else {
            switch (oneByte) {
               case 230:
                  return DecodedBitStreamParser.Mode.C40_ENCODE;
               case 231:
                  return DecodedBitStreamParser.Mode.BASE256_ENCODE;
               case 232:
                  fnc1positions.add(result.length());
                  result.append('\u001d');
               case 233:
               case 234:
                  break;
               case 235:
                  upperShift = true;
                  break;
               case 236:
                  result.append("[)>\u001e05\u001d");
                  resultTrailer.insert(0, "\u001e\u0004");
                  break;
               case 237:
                  result.append("[)>\u001e06\u001d");
                  resultTrailer.insert(0, "\u001e\u0004");
                  break;
               case 238:
                  return DecodedBitStreamParser.Mode.ANSIX12_ENCODE;
               case 239:
                  return DecodedBitStreamParser.Mode.TEXT_ENCODE;
               case 240:
                  return DecodedBitStreamParser.Mode.EDIFACT_ENCODE;
               case 241:
                  return DecodedBitStreamParser.Mode.ECI_ENCODE;
               default:
                  if (oneByte != 254 || bits.available() != 0) {
                     throw FormatException.getFormatInstance();
                  }
            }
         }
      } while (bits.available() > 0);

      return DecodedBitStreamParser.Mode.ASCII_ENCODE;
   }

   private static void decodeC40Segment(BitSource bits, ECIStringBuilder result, Set<Integer> fnc1positions) throws FormatException {
      boolean upperShift = false;
      int[] cValues = new int[3];
      int shift = 0;

      while (bits.available() != 8) {
         int firstByte = bits.readBits(8);
         if (firstByte == 254) {
            return;
         }

         parseTwoBytes(firstByte, bits.readBits(8), cValues);

         for (int i = 0; i < 3; i++) {
            int cValue = cValues[i];
            switch (shift) {
               case 0:
                  if (cValue < 3) {
                     shift = cValue + 1;
                  } else {
                     if (cValue >= C40_BASIC_SET_CHARS.length) {
                        throw FormatException.getFormatInstance();
                     }

                     char c40char = C40_BASIC_SET_CHARS[cValue];
                     if (upperShift) {
                        result.append((char)(c40char + 128));
                        upperShift = false;
                     } else {
                        result.append(c40char);
                     }
                  }
                  break;
               case 1:
                  if (upperShift) {
                     result.append((char)(cValue + 128));
                     upperShift = false;
                  } else {
                     result.append((char)cValue);
                  }

                  shift = 0;
                  break;
               case 2:
                  if (cValue < C40_SHIFT2_SET_CHARS.length) {
                     char c40char = C40_SHIFT2_SET_CHARS[cValue];
                     if (upperShift) {
                        result.append((char)(c40char + 128));
                        upperShift = false;
                     } else {
                        result.append(c40char);
                     }
                  } else {
                     switch (cValue) {
                        case 27:
                           fnc1positions.add(result.length());
                           result.append('\u001d');
                           break;
                        case 30:
                           upperShift = true;
                           break;
                        default:
                           throw FormatException.getFormatInstance();
                     }
                  }

                  shift = 0;
                  break;
               case 3:
                  if (upperShift) {
                     result.append((char)(cValue + 224));
                     upperShift = false;
                  } else {
                     result.append((char)(cValue + 96));
                  }

                  shift = 0;
                  break;
               default:
                  throw FormatException.getFormatInstance();
            }
         }

         if (bits.available() <= 0) {
            return;
         }
      }
   }

   private static void decodeTextSegment(BitSource bits, ECIStringBuilder result, Set<Integer> fnc1positions) throws FormatException {
      boolean upperShift = false;
      int[] cValues = new int[3];
      int shift = 0;

      while (bits.available() != 8) {
         int firstByte = bits.readBits(8);
         if (firstByte == 254) {
            return;
         }

         parseTwoBytes(firstByte, bits.readBits(8), cValues);

         for (int i = 0; i < 3; i++) {
            int cValue = cValues[i];
            switch (shift) {
               case 0:
                  if (cValue < 3) {
                     shift = cValue + 1;
                  } else {
                     if (cValue >= TEXT_BASIC_SET_CHARS.length) {
                        throw FormatException.getFormatInstance();
                     }

                     char textChar = TEXT_BASIC_SET_CHARS[cValue];
                     if (upperShift) {
                        result.append((char)(textChar + 128));
                        upperShift = false;
                     } else {
                        result.append(textChar);
                     }
                  }
                  break;
               case 1:
                  if (upperShift) {
                     result.append((char)(cValue + 128));
                     upperShift = false;
                  } else {
                     result.append((char)cValue);
                  }

                  shift = 0;
                  break;
               case 2:
                  if (cValue < TEXT_SHIFT2_SET_CHARS.length) {
                     char textChar = TEXT_SHIFT2_SET_CHARS[cValue];
                     if (upperShift) {
                        result.append((char)(textChar + 128));
                        upperShift = false;
                     } else {
                        result.append(textChar);
                     }
                  } else {
                     switch (cValue) {
                        case 27:
                           fnc1positions.add(result.length());
                           result.append('\u001d');
                           break;
                        case 30:
                           upperShift = true;
                           break;
                        default:
                           throw FormatException.getFormatInstance();
                     }
                  }

                  shift = 0;
                  break;
               case 3:
                  if (cValue >= TEXT_SHIFT3_SET_CHARS.length) {
                     throw FormatException.getFormatInstance();
                  }

                  char textChar = TEXT_SHIFT3_SET_CHARS[cValue];
                  if (upperShift) {
                     result.append((char)(textChar + 128));
                     upperShift = false;
                  } else {
                     result.append(textChar);
                  }

                  shift = 0;
                  break;
               default:
                  throw FormatException.getFormatInstance();
            }
         }

         if (bits.available() <= 0) {
            return;
         }
      }
   }

   private static void decodeAnsiX12Segment(BitSource bits, ECIStringBuilder result) throws FormatException {
      int[] cValues = new int[3];

      while (bits.available() != 8) {
         int firstByte = bits.readBits(8);
         if (firstByte == 254) {
            return;
         }

         parseTwoBytes(firstByte, bits.readBits(8), cValues);

         for (int i = 0; i < 3; i++) {
            int cValue = cValues[i];
            switch (cValue) {
               case 0:
                  result.append('\r');
                  break;
               case 1:
                  result.append('*');
                  break;
               case 2:
                  result.append('>');
                  break;
               case 3:
                  result.append(' ');
                  break;
               default:
                  if (cValue < 14) {
                     result.append((char)(cValue + 44));
                  } else {
                     if (cValue >= 40) {
                        throw FormatException.getFormatInstance();
                     }

                     result.append((char)(cValue + 51));
                  }
            }
         }

         if (bits.available() <= 0) {
            return;
         }
      }
   }

   private static void parseTwoBytes(int firstByte, int secondByte, int[] result) {
      int fullBitValue = (firstByte << 8) + secondByte - 1;
      int temp = fullBitValue / 1600;
      result[0] = temp;
      fullBitValue -= temp * 1600;
      temp = fullBitValue / 40;
      result[1] = temp;
      result[2] = fullBitValue - temp * 40;
   }

   private static void decodeEdifactSegment(BitSource bits, ECIStringBuilder result) {
      while (bits.available() > 16) {
         for (int i = 0; i < 4; i++) {
            int edifactValue = bits.readBits(6);
            if (edifactValue == 31) {
               int bitsLeft = 8 - bits.getBitOffset();
               if (bitsLeft != 8) {
                  bits.readBits(bitsLeft);
               }

               return;
            }

            if ((edifactValue & 32) == 0) {
               edifactValue |= 64;
            }

            result.append((char)edifactValue);
         }

         if (bits.available() <= 0) {
            return;
         }
      }
   }

   private static void decodeBase256Segment(BitSource bits, ECIStringBuilder result, Collection<byte[]> byteSegments) throws FormatException {
      int codewordPosition = 1 + bits.getByteOffset();
      int d1 = unrandomize255State(bits.readBits(8), codewordPosition++);
      int count;
      if (d1 == 0) {
         count = bits.available() / 8;
      } else if (d1 < 250) {
         count = d1;
      } else {
         count = 250 * (d1 - 249) + unrandomize255State(bits.readBits(8), codewordPosition++);
      }

      if (count < 0) {
         throw FormatException.getFormatInstance();
      } else {
         byte[] bytes = new byte[count];

         for (int i = 0; i < count; i++) {
            if (bits.available() < 8) {
               throw FormatException.getFormatInstance();
            }

            bytes[i] = (byte)unrandomize255State(bits.readBits(8), codewordPosition++);
         }

         byteSegments.add(bytes);
         result.append(new String(bytes, StandardCharsets.ISO_8859_1));
      }
   }

   private static void decodeECISegment(BitSource bits, ECIStringBuilder result) throws FormatException {
      if (bits.available() < 8) {
         throw FormatException.getFormatInstance();
      } else {
         int c1 = bits.readBits(8);
         if (c1 <= 127) {
            result.appendECI(c1 - 1);
         }
      }
   }

   private static int unrandomize255State(int randomizedBase256Codeword, int base256CodewordPosition) {
      int pseudoRandomNumber = 149 * base256CodewordPosition % 255 + 1;
      int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
      return tempVariable >= 0 ? tempVariable : tempVariable + 256;
   }

   private static enum Mode {
      PAD_ENCODE,
      ASCII_ENCODE,
      C40_ENCODE,
      TEXT_ENCODE,
      ANSIX12_ENCODE,
      EDIFACT_ENCODE,
      BASE256_ENCODE,
      ECI_ENCODE;
   }
}
