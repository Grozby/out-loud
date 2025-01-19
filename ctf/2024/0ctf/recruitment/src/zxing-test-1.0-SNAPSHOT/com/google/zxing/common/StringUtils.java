package com.google.zxing.common;

import com.google.zxing.DecodeHintType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

public final class StringUtils {
   private static final Charset PLATFORM_DEFAULT_ENCODING = Charset.defaultCharset();
   public static final Charset SHIFT_JIS_CHARSET;
   public static final Charset GB2312_CHARSET;
   private static final Charset EUC_JP;
   private static final boolean ASSUME_SHIFT_JIS;
   public static final String SHIFT_JIS = "SJIS";
   public static final String GB2312 = "GB2312";

   private StringUtils() {
   }

   public static String guessEncoding(byte[] bytes, Map<DecodeHintType, ?> hints) {
      Charset c = guessCharset(bytes, hints);
      if (c.equals(SHIFT_JIS_CHARSET)) {
         return "SJIS";
      } else if (c.equals(StandardCharsets.UTF_8)) {
         return "UTF8";
      } else {
         return c.equals(StandardCharsets.ISO_8859_1) ? "ISO8859_1" : c.name();
      }
   }

   public static Charset guessCharset(byte[] bytes, Map<DecodeHintType, ?> hints) {
      if (hints != null && hints.containsKey(DecodeHintType.CHARACTER_SET)) {
         return Charset.forName(hints.get(DecodeHintType.CHARACTER_SET).toString());
      } else if (bytes.length <= 2 || (bytes[0] != -2 || bytes[1] != -1) && (bytes[0] != -1 || bytes[1] != -2)) {
         int length = bytes.length;
         boolean canBeISO88591 = true;
         boolean canBeShiftJIS = SHIFT_JIS_CHARSET != null;
         boolean canBeUTF8 = true;
         int utf8BytesLeft = 0;
         int utf2BytesChars = 0;
         int utf3BytesChars = 0;
         int utf4BytesChars = 0;
         int sjisBytesLeft = 0;
         int sjisKatakanaChars = 0;
         int sjisCurKatakanaWordLength = 0;
         int sjisCurDoubleBytesWordLength = 0;
         int sjisMaxKatakanaWordLength = 0;
         int sjisMaxDoubleBytesWordLength = 0;
         int isoHighOther = 0;
         boolean utf8bom = bytes.length > 3 && bytes[0] == -17 && bytes[1] == -69 && bytes[2] == -65;

         for (int i = 0; i < length && (canBeISO88591 || canBeShiftJIS || canBeUTF8); i++) {
            int value = bytes[i] & 255;
            if (canBeUTF8) {
               if (utf8BytesLeft > 0) {
                  if ((value & 128) == 0) {
                     canBeUTF8 = false;
                  } else {
                     utf8BytesLeft--;
                  }
               } else if ((value & 128) != 0) {
                  if ((value & 64) == 0) {
                     canBeUTF8 = false;
                  } else {
                     utf8BytesLeft++;
                     if ((value & 32) == 0) {
                        utf2BytesChars++;
                     } else {
                        utf8BytesLeft++;
                        if ((value & 16) == 0) {
                           utf3BytesChars++;
                        } else {
                           utf8BytesLeft++;
                           if ((value & 8) == 0) {
                              utf4BytesChars++;
                           } else {
                              canBeUTF8 = false;
                           }
                        }
                     }
                  }
               }
            }

            if (canBeISO88591) {
               if (value > 127 && value < 160) {
                  canBeISO88591 = false;
               } else if (value > 159 && (value < 192 || value == 215 || value == 247)) {
                  isoHighOther++;
               }
            }

            if (canBeShiftJIS) {
               if (sjisBytesLeft > 0) {
                  if (value >= 64 && value != 127 && value <= 252) {
                     sjisBytesLeft--;
                  } else {
                     canBeShiftJIS = false;
                  }
               } else if (value == 128 || value == 160 || value > 239) {
                  canBeShiftJIS = false;
               } else if (value > 160 && value < 224) {
                  sjisKatakanaChars++;
                  sjisCurDoubleBytesWordLength = 0;
                  if (++sjisCurKatakanaWordLength > sjisMaxKatakanaWordLength) {
                     sjisMaxKatakanaWordLength = sjisCurKatakanaWordLength;
                  }
               } else if (value > 127) {
                  sjisBytesLeft++;
                  sjisCurKatakanaWordLength = 0;
                  if (++sjisCurDoubleBytesWordLength > sjisMaxDoubleBytesWordLength) {
                     sjisMaxDoubleBytesWordLength = sjisCurDoubleBytesWordLength;
                  }
               } else {
                  sjisCurKatakanaWordLength = 0;
                  sjisCurDoubleBytesWordLength = 0;
               }
            }
         }

         if (canBeUTF8 && utf8BytesLeft > 0) {
            canBeUTF8 = false;
         }

         if (canBeShiftJIS && sjisBytesLeft > 0) {
            canBeShiftJIS = false;
         }

         if (!canBeUTF8 || !utf8bom && utf2BytesChars + utf3BytesChars + utf4BytesChars <= 0) {
            if (!canBeShiftJIS || !ASSUME_SHIFT_JIS && sjisMaxKatakanaWordLength < 3 && sjisMaxDoubleBytesWordLength < 3) {
               if (canBeISO88591 && canBeShiftJIS) {
                  return (sjisMaxKatakanaWordLength != 2 || sjisKatakanaChars != 2) && isoHighOther * 10 < length
                     ? StandardCharsets.ISO_8859_1
                     : SHIFT_JIS_CHARSET;
               } else if (canBeISO88591) {
                  return StandardCharsets.ISO_8859_1;
               } else if (canBeShiftJIS) {
                  return SHIFT_JIS_CHARSET;
               } else {
                  return canBeUTF8 ? StandardCharsets.UTF_8 : PLATFORM_DEFAULT_ENCODING;
               }
            } else {
               return SHIFT_JIS_CHARSET;
            }
         } else {
            return StandardCharsets.UTF_8;
         }
      } else {
         return StandardCharsets.UTF_16;
      }
   }

   static {
      Charset sjisCharset;
      try {
         sjisCharset = Charset.forName("SJIS");
      } catch (UnsupportedCharsetException var4) {
         sjisCharset = null;
      }

      SHIFT_JIS_CHARSET = sjisCharset;

      try {
         sjisCharset = Charset.forName("GB2312");
      } catch (UnsupportedCharsetException var3) {
         sjisCharset = null;
      }

      GB2312_CHARSET = sjisCharset;

      try {
         sjisCharset = Charset.forName("EUC_JP");
      } catch (UnsupportedCharsetException var2) {
         sjisCharset = null;
      }

      EUC_JP = sjisCharset;
      ASSUME_SHIFT_JIS = SHIFT_JIS_CHARSET != null && SHIFT_JIS_CHARSET.equals(PLATFORM_DEFAULT_ENCODING)
         || EUC_JP != null && EUC_JP.equals(PLATFORM_DEFAULT_ENCODING);
   }
}
