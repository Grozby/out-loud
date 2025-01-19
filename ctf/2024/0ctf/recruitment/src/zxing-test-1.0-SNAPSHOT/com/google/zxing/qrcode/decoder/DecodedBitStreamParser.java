package com.google.zxing.qrcode.decoder;

import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.StringUtils;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class DecodedBitStreamParser {
   private static final char[] ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:".toCharArray();
   private static final int GB2312_SUBSET = 1;

   private DecodedBitStreamParser() {
   }

   static DecoderResult decode(byte[] bytes, Version version, ErrorCorrectionLevel ecLevel, Map<DecodeHintType, ?> hints) throws FormatException {
      BitSource bits = new BitSource(bytes);
      StringBuilder result = new StringBuilder(50);
      List<byte[]> byteSegments = new ArrayList<>(1);
      int symbolSequence = -1;
      int parityData = -1;

      int symbologyModifier;
      try {
         CharacterSetECI currentCharacterSetECI = null;
         boolean fc1InEffect = false;
         boolean hasFNC1first = false;
         boolean hasFNC1second = false;

         Mode mode;
         do {
            if (bits.available() < 4) {
               mode = Mode.TERMINATOR;
            } else {
               mode = Mode.forBits(bits.readBits(4));
            }

            switch (mode) {
               case TERMINATOR:
                  break;
               case FNC1_FIRST_POSITION:
                  hasFNC1first = true;
                  fc1InEffect = true;
                  break;
               case FNC1_SECOND_POSITION:
                  hasFNC1second = true;
                  fc1InEffect = true;
                  break;
               case STRUCTURED_APPEND:
                  if (bits.available() < 16) {
                     throw FormatException.getFormatInstance();
                  }

                  symbolSequence = bits.readBits(8);
                  parityData = bits.readBits(8);
                  break;
               case ECI:
                  int value = parseECIValue(bits);
                  currentCharacterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
                  if (currentCharacterSetECI == null) {
                     throw FormatException.getFormatInstance();
                  }
                  break;
               case HANZI:
                  int subset = bits.readBits(4);
                  int countHanzi = bits.readBits(mode.getCharacterCountBits(version));
                  if (subset == 1) {
                     decodeHanziSegment(bits, result, countHanzi);
                  }
                  break;
               default:
                  int count = bits.readBits(mode.getCharacterCountBits(version));
                  switch (mode) {
                     case NUMERIC:
                        decodeNumericSegment(bits, result, count);
                        break;
                     case ALPHANUMERIC:
                        decodeAlphanumericSegment(bits, result, count, fc1InEffect);
                        break;
                     case BYTE:
                        decodeByteSegment(bits, result, count, currentCharacterSetECI, byteSegments, hints);
                        break;
                     case KANJI:
                        decodeKanjiSegment(bits, result, count);
                        break;
                     default:
                        throw FormatException.getFormatInstance();
                  }
            }
         } while (mode != Mode.TERMINATOR);

         if (currentCharacterSetECI != null) {
            if (hasFNC1first) {
               symbologyModifier = 4;
            } else if (hasFNC1second) {
               symbologyModifier = 6;
            } else {
               symbologyModifier = 2;
            }
         } else if (hasFNC1first) {
            symbologyModifier = 3;
         } else if (hasFNC1second) {
            symbologyModifier = 5;
         } else {
            symbologyModifier = 1;
         }
      } catch (IllegalArgumentException var19) {
         throw FormatException.getFormatInstance();
      }

      return new DecoderResult(
         bytes,
         result.toString(),
         byteSegments.isEmpty() ? null : byteSegments,
         ecLevel == null ? null : ecLevel.toString(),
         symbolSequence,
         parityData,
         symbologyModifier
      );
   }

   private static void decodeHanziSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
      if (StringUtils.GB2312_CHARSET == null) {
         throw FormatException.getFormatInstance();
      } else if (count * 13 > bits.available()) {
         throw FormatException.getFormatInstance();
      } else {
         byte[] buffer = new byte[2 * count];

         for (int offset = 0; count > 0; count--) {
            int twoBytes = bits.readBits(13);
            int assembledTwoBytes = twoBytes / 96 << 8 | twoBytes % 96;
            if (assembledTwoBytes < 2560) {
               assembledTwoBytes += 41377;
            } else {
               assembledTwoBytes += 42657;
            }

            buffer[offset] = (byte)(assembledTwoBytes >> 8 & 0xFF);
            buffer[offset + 1] = (byte)(assembledTwoBytes & 0xFF);
            offset += 2;
         }

         result.append(new String(buffer, StringUtils.GB2312_CHARSET));
      }
   }

   private static void decodeKanjiSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
      if (StringUtils.SHIFT_JIS_CHARSET == null) {
         throw FormatException.getFormatInstance();
      } else if (count * 13 > bits.available()) {
         throw FormatException.getFormatInstance();
      } else {
         byte[] buffer = new byte[2 * count];

         for (int offset = 0; count > 0; count--) {
            int twoBytes = bits.readBits(13);
            int assembledTwoBytes = twoBytes / 192 << 8 | twoBytes % 192;
            if (assembledTwoBytes < 7936) {
               assembledTwoBytes += 33088;
            } else {
               assembledTwoBytes += 49472;
            }

            buffer[offset] = (byte)(assembledTwoBytes >> 8);
            buffer[offset + 1] = (byte)assembledTwoBytes;
            offset += 2;
         }

         result.append(new String(buffer, StringUtils.SHIFT_JIS_CHARSET));
      }
   }

   private static void decodeByteSegment(
      BitSource bits, StringBuilder result, int count, CharacterSetECI currentCharacterSetECI, Collection<byte[]> byteSegments, Map<DecodeHintType, ?> hints
   ) throws FormatException {
      if (8 * count > bits.available()) {
         throw FormatException.getFormatInstance();
      } else {
         byte[] readBytes = new byte[count];

         for (int i = 0; i < count; i++) {
            readBytes[i] = (byte)bits.readBits(8);
         }

         Charset encoding;
         if (currentCharacterSetECI == null) {
            encoding = StringUtils.guessCharset(readBytes, hints);
         } else {
            encoding = currentCharacterSetECI.getCharset();
         }

         result.append(new String(readBytes, encoding));
         byteSegments.add(readBytes);
      }
   }

   private static char toAlphaNumericChar(int value) throws FormatException {
      if (value >= ALPHANUMERIC_CHARS.length) {
         throw FormatException.getFormatInstance();
      } else {
         return ALPHANUMERIC_CHARS[value];
      }
   }

   private static void decodeAlphanumericSegment(BitSource bits, StringBuilder result, int count, boolean fc1InEffect) throws FormatException {
      int start = result.length();

      while (count > 1) {
         if (bits.available() < 11) {
            throw FormatException.getFormatInstance();
         }

         int nextTwoCharsBits = bits.readBits(11);
         result.append(toAlphaNumericChar(nextTwoCharsBits / 45));
         result.append(toAlphaNumericChar(nextTwoCharsBits % 45));
         count -= 2;
      }

      if (count == 1) {
         if (bits.available() < 6) {
            throw FormatException.getFormatInstance();
         }

         result.append(toAlphaNumericChar(bits.readBits(6)));
      }

      if (fc1InEffect) {
         for (int i = start; i < result.length(); i++) {
            if (result.charAt(i) == '%') {
               if (i < result.length() - 1 && result.charAt(i + 1) == '%') {
                  result.deleteCharAt(i + 1);
               } else {
                  result.setCharAt(i, '\u001d');
               }
            }
         }
      }
   }

   private static void decodeNumericSegment(BitSource bits, StringBuilder result, int count) throws FormatException {
      while (count >= 3) {
         if (bits.available() < 10) {
            throw FormatException.getFormatInstance();
         }

         int threeDigitsBits = bits.readBits(10);
         if (threeDigitsBits >= 1000) {
            throw FormatException.getFormatInstance();
         }

         result.append(toAlphaNumericChar(threeDigitsBits / 100));
         result.append(toAlphaNumericChar(threeDigitsBits / 10 % 10));
         result.append(toAlphaNumericChar(threeDigitsBits % 10));
         count -= 3;
      }

      if (count == 2) {
         if (bits.available() < 7) {
            throw FormatException.getFormatInstance();
         }

         int twoDigitsBits = bits.readBits(7);
         if (twoDigitsBits >= 100) {
            throw FormatException.getFormatInstance();
         }

         result.append(toAlphaNumericChar(twoDigitsBits / 10));
         result.append(toAlphaNumericChar(twoDigitsBits % 10));
      } else if (count == 1) {
         if (bits.available() < 4) {
            throw FormatException.getFormatInstance();
         }

         int digitBits = bits.readBits(4);
         if (digitBits >= 10) {
            throw FormatException.getFormatInstance();
         }

         result.append(toAlphaNumericChar(digitBits));
      }
   }

   private static int parseECIValue(BitSource bits) throws FormatException {
      int firstByte = bits.readBits(8);
      if ((firstByte & 128) == 0) {
         return firstByte & 127;
      } else if ((firstByte & 192) == 128) {
         int secondByte = bits.readBits(8);
         return (firstByte & 63) << 8 | secondByte;
      } else if ((firstByte & 224) == 192) {
         int secondThirdBytes = bits.readBits(16);
         return (firstByte & 31) << 16 | secondThirdBytes;
      } else {
         throw FormatException.getFormatInstance();
      }
   }
}
