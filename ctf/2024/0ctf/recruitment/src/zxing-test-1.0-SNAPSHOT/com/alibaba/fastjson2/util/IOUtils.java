package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONException;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalTime;

public class IOUtils {
   public static final int NULL_32 = JDKUtils.BIG_ENDIAN ? 1853189228 : 1819047278;
   public static final long NULL_64 = JDKUtils.BIG_ENDIAN ? 30962749956423788L : 30399761348886638L;
   public static final int TRUE = JDKUtils.BIG_ENDIAN ? 1953658213 : 1702195828;
   public static final long TRUE_64 = JDKUtils.BIG_ENDIAN ? 32651586932375653L : 28429475166421108L;
   public static final int ALSE = JDKUtils.BIG_ENDIAN ? 1634497381 : 1702063201;
   public static final long ALSE_64 = JDKUtils.BIG_ENDIAN ? 27303536604938341L : 28429466576093281L;
   public static final long DOT_X0 = JDKUtils.BIG_ENDIAN ? 11776L : 46L;
   static final int[] sizeTable = new int[]{9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};
   public static final int[] DIGITS_K_32 = new int[1000];
   public static final long[] DIGITS_K_64 = new long[1000];
   private static final byte[] MIN_INT_BYTES = "-2147483648".getBytes();
   private static final char[] MIN_INT_CHARS = "-2147483648".toCharArray();
   private static final byte[] MIN_LONG = "-9223372036854775808".getBytes();
   public static final short[] PACKED_DIGITS;
   public static final int[] PACKED_DIGITS_UTF16;
   static final long[] POWER_TEN = new long[]{
      10L,
      100L,
      1000L,
      10000L,
      100000L,
      1000000L,
      10000000L,
      100000000L,
      1000000000L,
      10000000000L,
      100000000000L,
      1000000000000L,
      10000000000000L,
      100000000000000L,
      1000000000000000L,
      10000000000000000L,
      100000000000000000L,
      1000000000000000000L
   };

   public static int stringSize(int x) {
      int i = 0;

      while (x > sizeTable[i]) {
         i++;
      }

      return i + 1;
   }

   public static int stringSize(long x) {
      long p = 10L;

      for (int i = 1; i < 19; i++) {
         if (x < p) {
            return i;
         }

         p = 10L * p;
      }

      return 19;
   }

   public static void getChars(int i, int index, byte[] buf) {
      int charPos = index;
      boolean negative = i < 0;
      if (!negative) {
         i = -i;
      }

      while (i <= -100) {
         int q = i / 100;
         int r = q * 100 - i;
         i = q;
         charPos -= 2;
         JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)charPos, PACKED_DIGITS[r]);
      }

      if (i < -9) {
         charPos -= 2;
         JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)charPos, PACKED_DIGITS[-i]);
      } else {
         charPos--;
         buf[charPos] = (byte)(48 - i);
      }

      if (negative) {
         buf[charPos - 1] = 45;
      }
   }

   public static void getChars(int i, int index, char[] buf) {
      int charPos = index;
      boolean negative = i < 0;
      if (!negative) {
         i = -i;
      }

      while (i <= -100) {
         int q = i / 100;
         int r = q * 100 - i;
         i = q;
         charPos -= 2;
         JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(charPos << 1), PACKED_DIGITS_UTF16[r]);
      }

      if (i < -9) {
         charPos -= 2;
         JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(charPos << 1), PACKED_DIGITS_UTF16[-i]);
      } else {
         charPos--;
         buf[charPos] = (char)(48 - i);
      }

      if (negative) {
         buf[charPos - 1] = '-';
      }
   }

   public static void getChars(long i, int index, byte[] buf) {
      int charPos = index;
      boolean negative = i < 0L;
      if (!negative) {
         i = -i;
      }

      while (i <= -2147483648L) {
         long q = i / 100L;
         charPos -= 2;
         JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)charPos, PACKED_DIGITS[(int)(q * 100L - i)]);
         i = q;
      }

      int i2 = (int)i;

      while (i2 <= -100) {
         int q2 = i2 / 100;
         charPos -= 2;
         JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)charPos, PACKED_DIGITS[q2 * 100 - i2]);
         i2 = q2;
      }

      if (i2 < -9) {
         charPos -= 2;
         JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)charPos, PACKED_DIGITS[-i2]);
      } else {
         charPos--;
         buf[charPos] = (byte)(48 - i2);
      }

      if (negative) {
         buf[charPos - 1] = 45;
      }
   }

   public static void getChars(long i, int index, char[] buf) {
      int charPos = index;
      boolean negative = i < 0L;
      if (!negative) {
         i = -i;
      }

      while (i <= -2147483648L) {
         long q = i / 100L;
         charPos -= 2;
         JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(charPos << 1), PACKED_DIGITS_UTF16[(int)(q * 100L - i)]);
         i = q;
      }

      int i2 = (int)i;

      while (i2 <= -100) {
         int q2 = i2 / 100;
         charPos -= 2;
         JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(charPos << 1), PACKED_DIGITS_UTF16[q2 * 100 - i2]);
         i2 = q2;
      }

      if (i2 < -9) {
         charPos -= 2;
         JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(charPos << 1), PACKED_DIGITS_UTF16[-i2]);
      } else {
         charPos--;
         buf[charPos] = (char)(48 - i2);
      }

      if (negative) {
         charPos--;
         buf[charPos] = '-';
      }
   }

   public static int writeDecimal(byte[] buf, int off, long unscaledVal, int scale) {
      if (unscaledVal < 0L) {
         buf[off++] = 45;
         unscaledVal = -unscaledVal;
      }

      if (scale != 0) {
         int unscaleValSize = stringSize(unscaledVal);
         int insertionPoint = unscaleValSize - scale;
         if (insertionPoint == 0) {
            buf[off] = 48;
            buf[off + 1] = 46;
            off += 2;
         } else {
            if (insertionPoint >= 0) {
               long power = POWER_TEN[scale - 1];
               long div = unscaledVal / power;
               long rem = unscaledVal - div * power;
               off = writeInt64(buf, off, div);
               buf[off] = 46;
               if (scale == 1) {
                  buf[off + 1] = (byte)((int)(rem + 48L));
                  return off + 2;
               }

               if (scale == 2) {
                  JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, PACKED_DIGITS[(int)rem]);
                  return off + 3;
               }

               int i = 0;

               for (int end = unscaleValSize - stringSize(rem) - insertionPoint; i < end; i++) {
                  off++;
                  buf[off] = 48;
               }

               return writeInt64(buf, off + 1, rem);
            }

            buf[off] = 48;
            buf[off + 1] = 46;
            off += 2;

            for (int i = 0; i < -insertionPoint; i++) {
               buf[off++] = 48;
            }
         }
      }

      return writeInt64(buf, off, unscaledVal);
   }

   public static int writeDecimal(char[] buf, int off, long unscaledVal, int scale) {
      if (unscaledVal < 0L) {
         buf[off++] = '-';
         unscaledVal = -unscaledVal;
      }

      if (scale != 0) {
         int unscaleValSize = stringSize(unscaledVal);
         int insertionPoint = unscaleValSize - scale;
         if (insertionPoint == 0) {
            buf[off] = '0';
            buf[off + 1] = '.';
            off += 2;
         } else {
            if (insertionPoint >= 0) {
               long power = POWER_TEN[scale - 1];
               long div = unscaledVal / power;
               long rem = unscaledVal - div * power;
               off = writeInt64(buf, off, div);
               buf[off] = '.';
               if (scale == 1) {
                  buf[off + 1] = (char)((int)(rem + 48L));
                  return off + 2;
               }

               if (scale == 2) {
                  JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 1 << 1), PACKED_DIGITS_UTF16[(int)rem]);
                  return off + 3;
               }

               int i = 0;

               for (int end = unscaleValSize - stringSize(rem) - insertionPoint; i < end; i++) {
                  off++;
                  buf[off] = '0';
               }

               return writeInt64(buf, off + 1, rem);
            }

            buf[off] = '0';
            buf[off + 1] = '.';
            off += 2;

            for (int i = 0; i < -insertionPoint; i++) {
               buf[off++] = '0';
            }
         }
      }

      return writeInt64(buf, off, unscaledVal);
   }

   public static int encodeUTF8(byte[] src, int offset, int len, byte[] dst, int dp) {
      int sl = offset + len;

      while (offset < sl) {
         char c = JDKUtils.UNSAFE.getChar(src, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
         offset += 2;
         if (c < 128) {
            dst[dp++] = (byte)c;
         } else if (c < 2048) {
            dst[dp] = (byte)(192 | c >> 6);
            dst[dp + 1] = (byte)(128 | c & '?');
            dp += 2;
         } else if (c >= '\ud800' && c <= '\udfff') {
            utf8_char2(src, offset, sl, c, dst, dp);
            offset += 2;
            dp += 4;
         } else {
            dst[dp] = (byte)(224 | c >> '\f');
            dst[dp + 1] = (byte)(128 | c >> 6 & 63);
            dst[dp + 2] = (byte)(128 | c & '?');
            dp += 3;
         }
      }

      return dp;
   }

   public static int encodeUTF8(char[] src, int offset, int len, byte[] dst, int dp) {
      int sl = offset + len;
      int dlASCII = dp + Math.min(len, dst.length);

      while (dp < dlASCII && src[offset] < 128) {
         dst[dp++] = (byte)src[offset++];
      }

      while (offset < sl) {
         char c = src[offset++];
         if (c < 128) {
            dst[dp++] = (byte)c;
         } else if (c < 2048) {
            dst[dp] = (byte)(192 | c >> 6);
            dst[dp + 1] = (byte)(128 | c & '?');
            dp += 2;
         } else if (c >= '\ud800' && c <= '\udfff') {
            utf8_char2(src, offset, sl, c, dst, dp);
            offset++;
            dp += 4;
         } else {
            dst[dp] = (byte)(224 | c >> '\f');
            dst[dp + 1] = (byte)(128 | c >> 6 & 63);
            dst[dp + 2] = (byte)(128 | c & '?');
            dp += 3;
         }
      }

      return dp;
   }

   private static void utf8_char2(byte[] src, int offset, int sl, char c, byte[] dst, int dp) {
      char d;
      if (c <= '\udbff' && sl - offset >= 1 && (d = JDKUtils.UNSAFE.getChar(src, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset)) >= '\udc00' && d <= '\udfff') {
         int uc = (c << '\n') + d + -56613888;
         dst[dp] = (byte)(240 | uc >> 18);
         dst[dp + 1] = (byte)(128 | uc >> 12 & 63);
         dst[dp + 2] = (byte)(128 | uc >> 6 & 63);
         dst[dp + 3] = (byte)(128 | uc & 63);
      } else {
         throw new JSONException("malformed input off : " + offset);
      }
   }

   private static void utf8_char2(char[] src, int offset, int sl, char c, byte[] dst, int dp) {
      char d;
      if (c <= '\udbff' && sl - offset >= 1 && (d = src[offset]) >= '\udc00' && d <= '\udfff') {
         int uc = (c << '\n') + d + -56613888;
         dst[dp] = (byte)(240 | uc >> 18);
         dst[dp + 1] = (byte)(128 | uc >> 12 & 63);
         dst[dp + 2] = (byte)(128 | uc >> 6 & 63);
         dst[dp + 3] = (byte)(128 | uc & 63);
      } else {
         throw new JSONException("malformed input off : " + offset);
      }
   }

   public static boolean isNumber(String str) {
      for (int i = 0; i < str.length(); i++) {
         char ch = str.charAt(i);
         if (ch != '+' && ch != '-') {
            if (ch < '0' || ch > '9') {
               return false;
            }
         } else if (i != 0) {
            return false;
         }
      }

      return true;
   }

   public static boolean isNumber(char[] chars, int off, int len) {
      int i = off;

      for (int end = off + len; i < end; i++) {
         char ch = chars[i];
         if (ch != '+' && ch != '-') {
            if (ch < '0' || ch > '9') {
               return false;
            }
         } else if (i != 0) {
            return false;
         }
      }

      return true;
   }

   public static boolean isNumber(byte[] chars, int off, int len) {
      int i = off;

      for (int end = off + len; i < end; i++) {
         char ch = (char)chars[i];
         if (ch != '+' && ch != '-') {
            if (ch < '0' || ch > '9') {
               return false;
            }
         } else if (i != 0) {
            return false;
         }
      }

      return true;
   }

   public static void close(Closeable x) {
      if (x != null) {
         try {
            x.close();
         } catch (Exception var2) {
         }
      }
   }

   public static int decodeUTF8(byte[] src, int off, int len, byte[] dst) {
      int sl = off + len;
      int dp = 0;

      while (off < sl) {
         int b0 = src[off++];
         if (b0 >= 0) {
            dst[dp] = (byte)b0;
            dst[dp + 1] = 0;
            dp += 2;
         } else if (b0 >> 5 == -2 && (b0 & 30) != 0) {
            if (off < sl) {
               int b1 = src[off++];
               if ((b1 & 192) != 128) {
                  return -1;
               }

               char c = (char)(b0 << 6 ^ b1 ^ 3968);
               dst[dp] = (byte)c;
               dst[dp + 1] = (byte)(c >> '\b');
               dp += 2;
            } else {
               dst[dp] = (byte)b0;
               dst[dp + 1] = 0;
               dp += 2;
               break;
            }
         } else if (b0 >> 4 == -2) {
            if (off + 1 >= sl) {
               return -1;
            }

            int b1 = src[off];
            int b2 = src[off + 1];
            off += 2;
            if (b0 == -32 && (b1 & 224) == 128 || (b1 & 192) != 128 || (b2 & 192) != 128) {
               return -1;
            }

            char c = (char)(b0 << 12 ^ b1 << 6 ^ b2 ^ -123008);
            boolean isSurrogate = c >= '\ud800' && c < '\ue000';
            if (isSurrogate) {
               return -1;
            }

            dst[dp] = (byte)c;
            dst[dp + 1] = (byte)(c >> '\b');
            dp += 2;
         } else {
            if (b0 >> 3 == -2) {
               if (off + 2 >= sl) {
                  return -1;
               }

               int b2x = src[off];
               int b3 = src[off + 1];
               int b4 = src[off + 2];
               off += 3;
               int uc = b0 << 18 ^ b2x << 12 ^ b3 << 6 ^ b4 ^ 3678080;
               if ((b2x & 192) == 128 && (b3 & 192) == 128 && (b4 & 192) == 128 && uc >= 65536 && uc < 1114112) {
                  char c = (char)((uc >>> 10) + 55232);
                  dst[dp] = (byte)c;
                  dst[dp + 1] = (byte)(c >> '\b');
                  dp += 2;
                  c = (char)((uc & 1023) + 56320);
                  dst[dp] = (byte)c;
                  dst[dp + 1] = (byte)(c >> '\b');
                  dp += 2;
                  continue;
               }

               return -1;
            }

            return -1;
         }
      }

      return dp;
   }

   public static int decodeUTF8(byte[] src, int off, int len, char[] dst) {
      int sl = off + len;
      int dp = 0;
      int dlASCII = Math.min(len, dst.length);

      while (dp < dlASCII && src[off] >= 0) {
         dst[dp++] = (char)src[off++];
      }

      while (off < sl) {
         int b1 = src[off++];
         if (b1 >= 0) {
            dst[dp++] = (char)b1;
         } else if (b1 >> 5 != -2 || (b1 & 30) == 0) {
            if (b1 >> 4 == -2) {
               if (off + 1 >= sl) {
                  return -1;
               }

               int b2 = src[off];
               int b3 = src[off + 1];
               off += 2;
               if (b1 == -32 && (b2 & 224) == 128 || (b2 & 192) != 128 || (b3 & 192) != 128) {
                  return -1;
               }

               char c = (char)(b1 << 12 ^ b2 << 6 ^ b3 ^ -123008);
               boolean isSurrogate = c >= '\ud800' && c < '\ue000';
               if (isSurrogate) {
                  return -1;
               }

               dst[dp++] = c;
            } else {
               if (b1 >> 3 != -2) {
                  return -1;
               }

               if (off + 2 >= sl) {
                  return -1;
               }

               int b2x = src[off];
               int b3x = src[off + 1];
               int b4 = src[off + 2];
               off += 3;
               int uc = b1 << 18 ^ b2x << 12 ^ b3x << 6 ^ b4 ^ 3678080;
               if ((b2x & 192) != 128 || (b3x & 192) != 128 || (b4 & 192) != 128 || uc < 65536 || uc >= 1114112) {
                  return -1;
               }

               dst[dp] = (char)((uc >>> 10) + 55232);
               dst[dp + 1] = (char)((uc & 1023) + 56320);
               dp += 2;
            }
         } else {
            if (off >= sl) {
               return -1;
            }

            int b2x = src[off++];
            if ((b2x & 192) != 128) {
               return -1;
            }

            dst[dp++] = (char)(b1 << 6 ^ b2x ^ 3968);
         }
      }

      return dp;
   }

   public static long lines(File file) throws Exception {
      FileInputStream in = new FileInputStream(file);

      long var2;
      try {
         var2 = lines(in);
      } catch (Throwable var5) {
         try {
            in.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      in.close();
      return var2;
   }

   public static long lines(InputStream in) throws Exception {
      long lines = 0L;
      byte[] buf = new byte[8192];

      while (true) {
         int len = in.read(buf, 0, buf.length);
         if (len == -1) {
            return lines;
         }

         for (int i = 0; i < len; i++) {
            byte b = buf[i];
            if (b == 10) {
               lines++;
            }
         }
      }
   }

   public static int writeLocalDate(byte[] bytes, int off, int year, int month, int dayOfMonth) {
      if (year < 0) {
         bytes[off++] = 45;
         year = -year;
      } else if (year > 9999) {
         bytes[off++] = 43;
      }

      if (year < 10000) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, PACKED_DIGITS[y01]);
         JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 2L, PACKED_DIGITS[y23]);
         off += 4;
      } else {
         off = writeInt32(bytes, off, year);
      }

      bytes[off] = 45;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, PACKED_DIGITS[month]);
      bytes[off + 3] = 45;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 4L, PACKED_DIGITS[dayOfMonth]);
      return off + 6;
   }

   public static int writeLocalDate(char[] chars, int off, int year, int month, int dayOfMonth) {
      if (year < 0) {
         chars[off++] = '-';
         year = -year;
      } else if (year > 9999) {
         chars[off++] = '+';
      }

      if (year < 10000) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off << 1), PACKED_DIGITS_UTF16[y01]);
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 2 << 1), PACKED_DIGITS_UTF16[y23]);
         off += 4;
      } else {
         off = writeInt32(chars, off, year);
      }

      chars[off] = '-';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 1 << 1), PACKED_DIGITS_UTF16[month]);
      chars[off + 3] = '-';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 4 << 1), PACKED_DIGITS_UTF16[dayOfMonth]);
      return off + 6;
   }

   public static void writeLocalTime(byte[] bytes, int off, int hour, int minute, int second) {
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, PACKED_DIGITS[hour]);
      bytes[off + 2] = 58;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 3L, PACKED_DIGITS[minute]);
      bytes[off + 5] = 58;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 6L, PACKED_DIGITS[second]);
   }

   public static int writeLocalTime(byte[] bytes, int off, LocalTime time) {
      writeLocalTime(bytes, off, time.getHour(), time.getMinute(), time.getSecond());
      off += 8;
      int nano = time.getNano();
      return nano != 0 ? writeNano(bytes, off, nano) : off;
   }

   public static int writeNano(byte[] bytes, int off, int nano) {
      int div = nano / 1000;
      int div2 = div / 1000;
      int rem1 = nano - div * 1000;
      putInt(bytes, off, DIGITS_K_32[div2] & -256 | 46);
      off += 4;
      int v;
      if (rem1 == 0) {
         int rem2 = div - div2 * 1000;
         if (rem2 == 0) {
            return off;
         }

         v = DIGITS_K_32[rem2];
      } else {
         v = DIGITS_K_32[div - div2 * 1000];
      }

      bytes[off] = (byte)(v >> 8);
      bytes[off + 1] = (byte)(v >> 16);
      off += 2;
      if (rem1 == 0) {
         bytes[off] = (byte)(v >> 24);
         return off + 1;
      } else {
         putInt(bytes, off, DIGITS_K_32[rem1] & -256 | v >> 24);
         return off + 4;
      }
   }

   public static int writeNano(char[] chars, int off, int nano) {
      int div = nano / 1000;
      int div2 = div / 1000;
      int rem1 = nano - div * 1000;
      putLong(chars, off, DIGITS_K_64[div2] & -65536L | DOT_X0);
      off += 4;
      long v;
      if (rem1 == 0) {
         int rem2 = div - div2 * 1000;
         if (rem2 == 0) {
            return off;
         }

         v = DIGITS_K_64[rem2];
      } else {
         v = DIGITS_K_64[div - div2 * 1000];
      }

      chars[off] = (char)((int)(v >> 16));
      chars[off + 1] = (char)((int)(v >> 32));
      off += 2;
      if (rem1 == 0) {
         chars[off] = (char)((int)(v >> 48));
         return off + 1;
      } else {
         putLong(chars, off, DIGITS_K_64[rem1] & -65536L | v >> 48);
         return off + 4;
      }
   }

   public static void writeLocalTime(char[] chars, int off, int hour, int minute, int second) {
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off << 1), PACKED_DIGITS_UTF16[hour]);
      chars[off + 2] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 3 << 1), PACKED_DIGITS_UTF16[minute]);
      chars[off + 5] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 6 << 1), PACKED_DIGITS_UTF16[second]);
   }

   public static int writeLocalTime(char[] chars, int off, LocalTime time) {
      writeLocalTime(chars, off, time.getHour(), time.getMinute(), time.getSecond());
      off += 8;
      int nano = time.getNano();
      return nano != 0 ? writeNano(chars, off, nano) : off;
   }

   public static int writeInt64(byte[] buf, int pos, long value) {
      long i;
      if (value < 0L) {
         if (value == Long.MIN_VALUE) {
            System.arraycopy(MIN_LONG, 0, buf, pos, MIN_LONG.length);
            return pos + MIN_LONG.length;
         }

         i = -value;
         buf[pos++] = 45;
      } else {
         i = value;
      }

      if (i < 1000L) {
         int v = DIGITS_K_32[(int)i];
         int start = v & 0xFF;
         if (start == 0) {
            buf[pos] = (byte)(v >> 8);
            buf[pos + 1] = (byte)(v >> 16);
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (byte)(v >> 16);
         }

         buf[pos++] = (byte)(v >> 24);
         return pos;
      } else {
         long q1 = i / 1000L;
         int r1 = (int)(i - q1 * 1000L);
         int v1 = DIGITS_K_32[r1];
         if (i < 1000000L) {
            int v2 = DIGITS_K_32[(int)q1];
            int start = v2 & 0xFF;
            if (start == 0) {
               buf[pos] = (byte)(v2 >> 8);
               buf[pos + 1] = (byte)(v2 >> 16);
               pos += 2;
            } else if (start == 1) {
               buf[pos++] = (byte)(v2 >> 16);
            }

            putInt(buf, pos, v1 & -256 | v2 >> 24);
            return pos + 4;
         } else {
            long q2 = q1 / 1000L;
            int r2 = (int)(q1 - q2 * 1000L);
            long q3 = q2 / 1000L;
            int v2 = DIGITS_K_32[r2];
            if (q3 == 0L) {
               int v3 = DIGITS_K_32[(int)q2];
               int start = v3 & 0xFF;
               if (start == 0) {
                  buf[pos] = (byte)(v3 >> 8);
                  buf[pos + 1] = (byte)(v3 >> 16);
                  pos += 2;
               } else if (start == 1) {
                  buf[pos++] = (byte)(v3 >> 16);
               }

               buf[pos] = (byte)(v3 >> 24);
               buf[pos + 1] = (byte)(v2 >> 8);
               buf[pos + 2] = (byte)(v2 >> 16);
               putInt(buf, pos + 3, v1 & -256 | v2 >> 24);
               return pos + 7;
            } else {
               int r3 = (int)(q2 - q3 * 1000L);
               int q4 = (int)(q3 / 1000L);
               int v3 = DIGITS_K_32[r3];
               if (q4 == 0) {
                  int v4 = DIGITS_K_32[(int)q3];
                  int start = v4 & 0xFF;
                  if (start == 0) {
                     buf[pos] = (byte)(v4 >> 8);
                     buf[pos + 1] = (byte)(v4 >> 16);
                     pos += 2;
                  } else if (start == 1) {
                     buf[pos++] = (byte)(v4 >> 16);
                  }

                  buf[pos] = (byte)(v4 >> 24);
                  buf[pos + 1] = (byte)(v3 >> 8);
                  putInt(buf, pos + 2, (v2 & 16776960) << 8 | v3 >> 16);
                  putInt(buf, pos + 6, v1 & -256 | v2 >> 24);
                  return pos + 10;
               } else {
                  int r4 = (int)(q3 - (long)(q4 * 1000));
                  int q5 = q4 / 1000;
                  int v4 = DIGITS_K_32[r4];
                  if (q5 == 0) {
                     int v5 = DIGITS_K_32[q4];
                     int start = v5 & 0xFF;
                     if (start == 0) {
                        buf[pos] = (byte)(v5 >> 8);
                        buf[pos + 1] = (byte)(v5 >> 16);
                        pos += 2;
                     } else if (start == 1) {
                        buf[pos++] = (byte)(v5 >> 16);
                     }

                     putInt(buf, pos, v4 & -256 | v5 >> 24);
                     buf[pos + 4] = (byte)(v3 >> 8);
                     putInt(buf, pos + 5, (v2 & 16776960) << 8 | v3 >> 16);
                     putInt(buf, pos + 9, v1 & -256 | v2 >> 24);
                     return pos + 13;
                  } else {
                     int r5 = q4 - q5 * 1000;
                     int q6 = q5 / 1000;
                     int v5 = DIGITS_K_32[r5];
                     if (q6 == 0) {
                        int v = DIGITS_K_32[q5];
                        int start = v & 0xFF;
                        if (start == 0) {
                           buf[pos] = (byte)(v >> 8);
                           buf[pos + 1] = (byte)(v >> 16);
                           pos += 2;
                        } else if (start == 1) {
                           buf[pos++] = (byte)(v >> 16);
                        }

                        buf[pos++] = (byte)(v >> 24);
                     } else {
                        putInt(buf, pos, DIGITS_K_32[q5 - q6 * 1000] & -256 | q6 + 48);
                        pos += 4;
                     }

                     buf[pos] = (byte)(v5 >> 8);
                     putInt(buf, pos + 1, (v4 & 16776960) << 8 | v5 >> 16);
                     putInt(buf, pos + 5, v3 & -256 | v4 >> 24);
                     buf[pos + 9] = (byte)(v2 >> 8);
                     buf[pos + 10] = (byte)(v2 >> 16);
                     putInt(buf, pos + 11, v1 & -256 | v2 >> 24);
                     return pos + 15;
                  }
               }
            }
         }
      }
   }

   public static int writeInt64(char[] buf, int pos, long value) {
      long i;
      if (value < 0L) {
         if (value == Long.MIN_VALUE) {
            for (int x = 0; x < MIN_LONG.length; x++) {
               buf[pos + x] = (char)MIN_LONG[x];
            }

            return pos + MIN_LONG.length;
         }

         i = -value;
         buf[pos++] = '-';
      } else {
         i = value;
      }

      if (i < 1000L) {
         long v = DIGITS_K_64[(int)i];
         int start = (byte)((int)v);
         if (start == 0) {
            putInt(buf, pos, (int)(v >> 16));
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (char)((int)(v >> 32));
         }

         buf[pos++] = (char)((int)(v >> 48));
         return pos;
      } else {
         long q1 = i / 1000L;
         int r1 = (int)(i - q1 * 1000L);
         long v1 = DIGITS_K_64[r1];
         if (i < 1000000L) {
            long v2 = DIGITS_K_64[(int)q1];
            int start = (byte)((int)v2);
            if (start == 0) {
               putInt(buf, pos, (int)(v2 >> 16));
               pos += 2;
            } else if (start == 1) {
               buf[pos++] = (char)((int)(v2 >> 32));
            }

            putLong(buf, pos, v1 & -65536L | v2 >> 48);
            return pos + 4;
         } else {
            long q2 = q1 / 1000L;
            int r2 = (int)(q1 - q2 * 1000L);
            long q3 = q2 / 1000L;
            long v2 = DIGITS_K_64[r2];
            if (q3 == 0L) {
               long v3 = DIGITS_K_64[(int)q2];
               int start = (byte)((int)v3);
               if (start == 0) {
                  putInt(buf, pos, (int)(v3 >> 16));
                  pos += 2;
               } else if (start == 1) {
                  buf[pos++] = (char)((int)(v3 >> 32));
               }

               buf[pos] = (char)((int)(v3 >> 48));
               putInt(buf, pos + 1, (int)(v2 >> 16));
               putLong(buf, pos + 3, v1 & -65536L | v2 >> 48);
               return pos + 7;
            } else {
               int r3 = (int)(q2 - q3 * 1000L);
               int q4 = (int)(q3 / 1000L);
               long v3 = DIGITS_K_64[r3];
               if (q4 == 0) {
                  long v4 = DIGITS_K_64[(int)q3];
                  int start = (byte)((int)v4);
                  if (start == 0) {
                     putInt(buf, pos, (int)(v4 >> 16));
                     pos += 2;
                  } else if (start == 1) {
                     buf[pos++] = (char)((int)(v4 >> 32));
                  }

                  buf[pos] = (char)((int)(v4 >> 48));
                  buf[pos + 1] = (char)((int)(v3 >> 16));
                  putLong(buf, pos + 2, (v2 & 281474976645120L) << 16 | v3 >> 32);
                  putLong(buf, pos + 6, v1 & -65536L | v2 >> 48);
                  return pos + 10;
               } else {
                  int r4 = (int)(q3 - (long)(q4 * 1000));
                  int q5 = q4 / 1000;
                  long v4 = DIGITS_K_64[r4];
                  if (q5 == 0) {
                     long v5 = DIGITS_K_64[q4];
                     int start = (byte)((int)v5);
                     if (start == 0) {
                        putInt(buf, pos, (int)(v5 >> 16));
                        pos += 2;
                     } else if (start == 1) {
                        buf[pos++] = (char)((int)(v5 >> 32));
                     }

                     buf[pos] = (char)((int)(v5 >> 48));
                     putInt(buf, pos + 1, (int)(v4 >> 16));
                     putLong(buf, pos + 3, v3 & -65536L | v4 >> 48);
                     putInt(buf, pos + 7, (int)(v2 >> 16));
                     putLong(buf, pos + 9, v1 & -65536L | v2 >> 48);
                     return pos + 13;
                  } else {
                     int r5 = q4 - q5 * 1000;
                     int q6 = q5 / 1000;
                     long v5 = DIGITS_K_64[r5];
                     if (q6 == 0) {
                        int v = DIGITS_K_32[q5];
                        int start = (byte)v;
                        if (start == 0) {
                           buf[pos] = (char)((byte)(v >> 8));
                           buf[pos + 1] = (char)((byte)(v >> 16));
                           pos += 2;
                        } else if (start == 1) {
                           buf[pos++] = (char)((byte)(v >> 16));
                        }

                        buf[pos++] = (char)(v >> 24);
                     } else {
                        putLong(buf, pos, DIGITS_K_64[q5 - q6 * 1000]);
                        buf[pos] = (char)(q6 + 48);
                        pos += 4;
                     }

                     putInt(buf, pos, (int)(v5 >> 16));
                     putLong(buf, pos + 2, v4 & -65536L | v5 >> 48);
                     buf[pos + 6] = (char)((int)(v3 >> 16));
                     putLong(buf, pos + 7, (v2 & 281474976645120L) << 16 | v3 >> 32);
                     putLong(buf, pos + 11, v1 & -65536L | v2 >> 48);
                     return pos + 15;
                  }
               }
            }
         }
      }
   }

   public static int writeInt8(byte[] buf, int pos, byte value) {
      int i;
      if (value < 0) {
         i = -value;
         buf[pos++] = 45;
      } else {
         i = value;
      }

      int v = DIGITS_K_32[i];
      int start = (byte)v;
      if (start == 0) {
         putShort(buf, pos, (short)(v >> 8));
         pos += 2;
      } else if (start == 1) {
         buf[pos++] = (byte)(v >> 16);
      }

      buf[pos] = (byte)(v >> 24);
      return pos + 1;
   }

   public static int writeInt8(char[] buf, int pos, byte value) {
      int i;
      if (value < 0) {
         i = -value;
         buf[pos++] = '-';
      } else {
         i = value;
      }

      long v = DIGITS_K_64[i];
      int start = (byte)((int)v);
      if (start == 0) {
         putInt(buf, pos, (int)(v >> 16));
         pos += 2;
      } else if (start == 1) {
         buf[pos++] = (char)((int)(v >> 32));
      }

      buf[pos] = (char)((int)(v >> 48));
      return pos + 1;
   }

   public static int writeInt16(byte[] buf, int pos, short value) {
      int i;
      if (value < 0) {
         i = -value;
         buf[pos++] = 45;
      } else {
         i = value;
      }

      if (i < 1000) {
         int v = DIGITS_K_32[i];
         int start = (byte)v;
         if (start == 0) {
            putShort(buf, pos, (short)(v >> 8));
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (byte)(v >> 16);
         }

         buf[pos] = (byte)(v >> 24);
         return pos + 1;
      } else {
         int q1 = i / 1000;
         int v2 = DIGITS_K_32[q1];
         if ((byte)v2 == 1) {
            buf[pos++] = (byte)(v2 >> 16);
         }

         putInt(buf, pos, DIGITS_K_32[i - q1 * 1000] & -256 | v2 >> 24);
         return pos + 4;
      }
   }

   public static int writeInt16(char[] buf, int pos, short value) {
      int i;
      if (value < 0) {
         i = -value;
         buf[pos++] = '-';
      } else {
         i = value;
      }

      if (i < 1000) {
         long v = DIGITS_K_64[i];
         int start = (byte)((int)v);
         if (start == 0) {
            putInt(buf, pos, (int)(v >> 16));
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (char)((int)(v >> 32));
         }

         buf[pos] = (char)((int)(v >> 48));
         return pos + 1;
      } else {
         int q1 = i / 1000;
         long v2 = DIGITS_K_64[q1];
         if ((byte)((int)v2) == 1) {
            buf[pos++] = (char)((int)(v2 >> 32));
         }

         putLong(buf, pos, DIGITS_K_64[i - q1 * 1000] & -65536L | v2 >> 48);
         return pos + 4;
      }
   }

   public static int writeInt32(byte[] buf, int pos, int value) {
      int i;
      if (value < 0) {
         if (value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_INT_BYTES, 0, buf, pos, MIN_INT_BYTES.length);
            return pos + MIN_INT_BYTES.length;
         }

         i = -value;
         buf[pos++] = 45;
      } else {
         i = value;
      }

      if (i < 1000) {
         int v = DIGITS_K_32[i];
         int start = (byte)v;
         if (start == 0) {
            putShort(buf, pos, (short)(v >> 8));
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (byte)(v >> 16);
         }

         buf[pos] = (byte)(v >> 24);
         return pos + 1;
      } else {
         int q1 = i / 1000;
         int r1 = i - q1 * 1000;
         int v1 = DIGITS_K_32[r1];
         if (i < 1000000) {
            int v2 = DIGITS_K_32[q1];
            int start = (byte)v2;
            if (start == 0) {
               putShort(buf, pos, (short)(v2 >> 8));
               pos += 2;
            } else if (start == 1) {
               buf[pos++] = (byte)(v2 >> 16);
            }

            putInt(buf, pos, v1 & -256 | v2 >> 24);
            return pos + 4;
         } else {
            int q2 = q1 / 1000;
            int r2 = q1 - q2 * 1000;
            int q3 = q2 / 1000;
            int v2 = DIGITS_K_32[r2];
            if (q3 == 0) {
               int v = DIGITS_K_32[q2];
               int start = (byte)v;
               if (start == 0) {
                  putShort(buf, pos, (short)(v >> 8));
                  pos += 2;
               } else if (start == 1) {
                  buf[pos++] = (byte)(v >> 16);
               }

               buf[pos++] = (byte)(v >> 24);
            } else {
               putInt(buf, pos, DIGITS_K_32[q2 - q3 * 1000] & -256 | q3 + 48);
               pos += 4;
            }

            putShort(buf, pos, (short)(v2 >> 8));
            putInt(buf, pos + 2, v1 & -256 | v2 >> 24);
            return pos + 6;
         }
      }
   }

   public static int writeInt32(char[] buf, int pos, int value) {
      int i;
      if (value < 0) {
         if (value == Integer.MIN_VALUE) {
            System.arraycopy(MIN_INT_CHARS, 0, buf, pos, MIN_INT_CHARS.length);
            return pos + MIN_INT_CHARS.length;
         }

         i = -value;
         buf[pos++] = '-';
      } else {
         i = value;
      }

      if (i < 1000) {
         long v = DIGITS_K_64[i];
         int start = (byte)((int)v);
         if (start == 0) {
            putInt(buf, pos, (int)(v >> 16));
            pos += 2;
         } else if (start == 1) {
            buf[pos++] = (char)((int)(v >> 32));
         }

         buf[pos] = (char)((int)(v >> 48));
         return pos + 1;
      } else {
         int q1 = i / 1000;
         int r1 = i - q1 * 1000;
         long v1 = DIGITS_K_64[r1];
         if (i < 1000000) {
            long v2 = DIGITS_K_64[q1];
            int start = (byte)((int)v2);
            if (start == 0) {
               putInt(buf, pos, (int)(v2 >> 16));
               pos += 2;
            } else if (start == 1) {
               buf[pos++] = (char)((int)(v2 >> 32));
            }

            putLong(buf, pos, v1 & -65536L | v2 >> 48);
            return pos + 4;
         } else {
            int q2 = q1 / 1000;
            int r2 = q1 - q2 * 1000;
            int q3 = q2 / 1000;
            long v2 = DIGITS_K_64[r2];
            if (q3 == 0) {
               long v = DIGITS_K_64[q2];
               int start = (byte)((int)v);
               if (start == 0) {
                  putInt(buf, pos, (int)(v >> 16));
                  pos += 2;
               } else if (start == 1) {
                  buf[pos++] = (char)((int)(v >> 32));
               }

               buf[pos++] = (char)((int)(v >> 48));
            } else {
               putLong(buf, pos, DIGITS_K_64[q2 - q3 * 1000]);
               buf[pos] = (char)(q3 + 48);
               pos += 4;
            }

            putInt(buf, pos, (int)(v2 >> 16));
            putLong(buf, pos + 2, v1 & -65536L | v2 >> 48);
            return pos + 6;
         }
      }
   }

   public static void putShort(byte[] buf, int pos, short v) {
      JDKUtils.UNSAFE.putShort(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)pos, JDKUtils.BIG_ENDIAN ? Short.reverseBytes(v) : v);
   }

   public static void putInt(byte[] buf, int pos, int v) {
      JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)pos, JDKUtils.BIG_ENDIAN ? Integer.reverseBytes(v) : v);
   }

   public static void putInt(char[] buf, int pos, int v) {
      JDKUtils.UNSAFE.putInt(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(pos << 1), JDKUtils.BIG_ENDIAN ? Integer.reverseBytes(v) : v);
   }

   public static void putLong(char[] buf, int pos, long v) {
      JDKUtils.UNSAFE.putLong(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(pos << 1), JDKUtils.BIG_ENDIAN ? Long.reverseBytes(v) : v);
   }

   static {
      short[] shorts = new short[]{
         12336,
         12592,
         12848,
         13104,
         13360,
         13616,
         13872,
         14128,
         14384,
         14640,
         12337,
         12593,
         12849,
         13105,
         13361,
         13617,
         13873,
         14129,
         14385,
         14641,
         12338,
         12594,
         12850,
         13106,
         13362,
         13618,
         13874,
         14130,
         14386,
         14642,
         12339,
         12595,
         12851,
         13107,
         13363,
         13619,
         13875,
         14131,
         14387,
         14643,
         12340,
         12596,
         12852,
         13108,
         13364,
         13620,
         13876,
         14132,
         14388,
         14644,
         12341,
         12597,
         12853,
         13109,
         13365,
         13621,
         13877,
         14133,
         14389,
         14645,
         12342,
         12598,
         12854,
         13110,
         13366,
         13622,
         13878,
         14134,
         14390,
         14646,
         12343,
         12599,
         12855,
         13111,
         13367,
         13623,
         13879,
         14135,
         14391,
         14647,
         12344,
         12600,
         12856,
         13112,
         13368,
         13624,
         13880,
         14136,
         14392,
         14648,
         12345,
         12601,
         12857,
         13113,
         13369,
         13625,
         13881,
         14137,
         14393,
         14649
      };
      int[] digits = new int[]{
         3145776,
         3211312,
         3276848,
         3342384,
         3407920,
         3473456,
         3538992,
         3604528,
         3670064,
         3735600,
         3145777,
         3211313,
         3276849,
         3342385,
         3407921,
         3473457,
         3538993,
         3604529,
         3670065,
         3735601,
         3145778,
         3211314,
         3276850,
         3342386,
         3407922,
         3473458,
         3538994,
         3604530,
         3670066,
         3735602,
         3145779,
         3211315,
         3276851,
         3342387,
         3407923,
         3473459,
         3538995,
         3604531,
         3670067,
         3735603,
         3145780,
         3211316,
         3276852,
         3342388,
         3407924,
         3473460,
         3538996,
         3604532,
         3670068,
         3735604,
         3145781,
         3211317,
         3276853,
         3342389,
         3407925,
         3473461,
         3538997,
         3604533,
         3670069,
         3735605,
         3145782,
         3211318,
         3276854,
         3342390,
         3407926,
         3473462,
         3538998,
         3604534,
         3670070,
         3735606,
         3145783,
         3211319,
         3276855,
         3342391,
         3407927,
         3473463,
         3538999,
         3604535,
         3670071,
         3735607,
         3145784,
         3211320,
         3276856,
         3342392,
         3407928,
         3473464,
         3539000,
         3604536,
         3670072,
         3735608,
         3145785,
         3211321,
         3276857,
         3342393,
         3407929,
         3473465,
         3539001,
         3604537,
         3670073,
         3735609
      };
      if (JDKUtils.BIG_ENDIAN) {
         for (int i = 0; i < shorts.length; i++) {
            shorts[i] = Short.reverseBytes(shorts[i]);
         }

         for (int i = 0; i < digits.length; i++) {
            digits[i] = Integer.reverseBytes(digits[i] << 8);
         }
      }

      PACKED_DIGITS = shorts;
      PACKED_DIGITS_UTF16 = digits;

      for (int i = 0; i < 1000; i++) {
         int c0 = i < 10 ? 2 : (i < 100 ? 1 : 0);
         int c1 = i / 100 + 48;
         int c2 = i / 10 % 10 + 48;
         int c3 = i % 10 + 48;
         DIGITS_K_32[i] = c0 + (c1 << 8) + (c2 << 16) + (c3 << 24);
         long v = (long)(c1 << 16) + ((long)c2 << 32) + ((long)c3 << 48);
         if (JDKUtils.BIG_ENDIAN) {
            v <<= 8;
         }

         DIGITS_K_64[i] = (long)c0 + v;
      }
   }
}
