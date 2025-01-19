package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplDate;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRules;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
   public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
   public static final String SHANGHAI_ZONE_ID_NAME = "Asia/Shanghai";
   public static final ZoneId SHANGHAI_ZONE_ID = "Asia/Shanghai".equals(DEFAULT_ZONE_ID.getId()) ? DEFAULT_ZONE_ID : ZoneId.of("Asia/Shanghai");
   public static final ZoneRules SHANGHAI_ZONE_RULES = SHANGHAI_ZONE_ID.getRules();
   public static final String OFFSET_8_ZONE_ID_NAME = "+08:00";
   public static final ZoneId OFFSET_8_ZONE_ID = ZoneId.of("+08:00");
   public static final LocalDate LOCAL_DATE_19700101 = LocalDate.of(1970, 1, 1);
   static DateTimeFormatter DATE_TIME_FORMATTER_34;
   static DateTimeFormatter DATE_TIME_FORMATTER_COOKIE;
   static DateTimeFormatter DATE_TIME_FORMATTER_COOKIE_LOCAL;
   static DateTimeFormatter DATE_TIME_FORMATTER_RFC_2822;
   static final int LOCAL_EPOCH_DAY;

   public static Date parseDateYMDHMS19(String str) {
      if (str != null && !str.isEmpty()) {
         long millis = parseMillisYMDHMS19(str, DEFAULT_ZONE_ID);
         return new Date(millis);
      } else {
         return null;
      }
   }

   public static Date parseDate(String str, String format) {
      return parseDate(str, format, DEFAULT_ZONE_ID);
   }

   public static Date parseDate(String str, String format, ZoneId zoneId) {
      if (str == null || str.isEmpty() || "null".equals(str)) {
         return null;
      } else if (format != null && !format.isEmpty()) {
         switch (format) {
            case "yyyy-MM-dd'T'HH:mm:ss": {
               long millis = parseMillis19(str, zoneId, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH_T);
               return new Date(millis);
            }
            case "yyyy-MM-dd HH:mm:ss": {
               long millis = parseMillisYMDHMS19(str, zoneId);
               return new Date(millis);
            }
            case "yyyy/MM/dd HH:mm:ss": {
               long millis = parseMillis19(str, zoneId, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_SLASH);
               return new Date(millis);
            }
            case "dd.MM.yyyy HH:mm:ss": {
               long millis = parseMillis19(str, zoneId, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DOT);
               return new Date(millis);
            }
            case "yyyy-MM-dd": {
               long millis = parseMillis10(str, zoneId, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH);
               return new Date(millis);
            }
            case "yyyy/MM/dd": {
               long millis = parseMillis10(str, zoneId, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH);
               return new Date(millis);
            }
            case "yyyyMMdd": {
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               LocalDate ldt = LocalDate.parse(str, formatter);
               long millis = millis(zoneId, ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), 0, 0, 0, 0);
               return new Date(millis);
            }
            case "yyyyMMddHHmmssSSSZ": {
               long millis = parseMillis(str, DEFAULT_ZONE_ID);
               return new Date(millis);
            }
            case "iso8601":
               return parseDate(str);
            default: {
               if (zoneId == null) {
                  zoneId = DEFAULT_ZONE_ID;
               }

               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               LocalDateTime ldt = LocalDateTime.parse(str, formatter);
               long millis = millis(ldt, zoneId);
               return new Date(millis);
            }
         }
      } else {
         long millis = parseMillis(str, zoneId);
         return millis == 0L ? null : new Date(millis);
      }
   }

   public static Date parseDate(String str) {
      long millis = parseMillis(str, DEFAULT_ZONE_ID);
      return millis == 0L ? null : new Date(millis);
   }

   public static Date parseDate(String str, ZoneId zoneId) {
      long millis = parseMillis(str, zoneId);
      return millis == 0L ? null : new Date(millis);
   }

   public static long parseMillis(String str) {
      return parseMillis(str, DEFAULT_ZONE_ID);
   }

   public static long parseMillis(String str, ZoneId zoneId) {
      if (str == null) {
         return 0L;
      } else if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
         byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
         return parseMillis(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1, zoneId);
      } else {
         char[] chars = JDKUtils.getCharArray(str);
         return parseMillis(chars, 0, chars.length, zoneId);
      }
   }

   public static LocalDateTime parseLocalDateTime(String str) {
      return str == null ? null : parseLocalDateTime(str, 0, str.length());
   }

   public static LocalDateTime parseLocalDateTime(String str, int off, int len) {
      if (str != null && len != 0) {
         LocalDateTime ldt;
         if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            ldt = parseLocalDateTime(bytes, off, len);
         } else if (JDKUtils.JVM_VERSION == 8 && !JDKUtils.FIELD_STRING_VALUE_ERROR) {
            char[] chars = JDKUtils.getCharArray(str);
            ldt = parseLocalDateTime(chars, off, len);
         } else {
            char[] chars = new char[len];
            str.getChars(off, off + len, chars, 0);
            ldt = parseLocalDateTime(chars, off, len);
         }

         if (ldt == null) {
            switch (str) {
               case "":
               case "null":
               case "00000000":
               case "000000000000":
               case "0000年00月00日":
               case "0000-0-00":
               case "0000-00-0":
               case "0000-00-00":
                  return null;
               default:
                  throw new DateTimeParseException(str, str, off);
            }
         } else {
            return ldt;
         }
      } else {
         return null;
      }
   }

   public static LocalDateTime parseLocalDateTime(char[] str, int off, int len) {
      if (str != null && len != 0) {
         switch (len) {
            case 4:
               if (str[off] == 'n' && str[off + 1] == 'u' && str[off + 2] == 'l' && str[off + 3] == 'l') {
                  return null;
               }

               String input = new String(str, off, len);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            case 5:
            case 6:
            case 7:
            case 13:
            case 15:
            default:
               return parseLocalDateTimeX(str, off, len);
            case 8:
               if (str[2] == ':' && str[5] == ':') {
                  LocalTime localTime = parseLocalTime8(str, off);
                  return LocalDateTime.of(LOCAL_DATE_19700101, localTime);
               } else {
                  LocalDate localDate = parseLocalDate8(str, off);
                  if (localDate == null) {
                     return null;
                  }

                  return LocalDateTime.of(localDate, LocalTime.MIN);
               }
            case 9:
               LocalDate localDate = parseLocalDate9(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 10:
               LocalDate localDate = parseLocalDate10(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 11:
               LocalDate localDate = parseLocalDate11(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 12:
               return parseLocalDateTime12(str, off);
            case 14:
               return parseLocalDateTime14(str, off);
            case 16:
               return parseLocalDateTime16(str, off);
            case 17:
               return parseLocalDateTime17(str, off);
            case 18:
               return parseLocalDateTime18(str, off);
            case 19:
               return parseLocalDateTime19(str, off);
            case 20:
               return parseLocalDateTime20(str, off);
         }
      } else {
         return null;
      }
   }

   public static LocalTime parseLocalTime5(byte[] bytes, int off) {
      if (off + 5 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         int second = 0;
         byte h0;
         byte h1;
         byte i0;
         byte i1;
         if (c2 == 58) {
            h0 = c0;
            h1 = c1;
            i0 = c3;
            i1 = c4;
         } else {
            if (c1 != 58 || c3 != 58) {
               return null;
            }

            h0 = 48;
            h1 = c0;
            i0 = 48;
            i1 = c2;
            second = c4 - 48;
         }

         if (h0 >= 48 && h0 <= 57 && h1 >= 48 && h1 <= 57) {
            int hour = (h0 - 48) * 10 + (h1 - 48);
            if (i0 >= 48 && i0 <= 57 && i1 >= 48 && i1 <= 57) {
               int minute = (i0 - 48) * 10 + (i1 - 48);
               return LocalTime.of(hour, minute, second);
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime5(char[] chars, int off) {
      if (off + 5 > chars.length) {
         return null;
      } else {
         char c0 = chars[off];
         char c1 = chars[off + 1];
         char c2 = chars[off + 2];
         char c3 = chars[off + 3];
         char c4 = chars[off + 4];
         int second = 0;
         char h0;
         char h1;
         char i0;
         char i1;
         if (c2 == ':') {
            h0 = c0;
            h1 = c1;
            i0 = c3;
            i1 = c4;
         } else {
            if (c1 != ':' || c3 != ':') {
               return null;
            }

            h0 = '0';
            h1 = c0;
            i0 = '0';
            i1 = c2;
            second = c4 - '0';
         }

         if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
            int hour = (h0 - '0') * 10 + (h1 - '0');
            if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
               int minute = (i0 - '0') * 10 + (i1 - '0');
               return LocalTime.of(hour, minute, second);
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime6(byte[] bytes, int off) {
      if (off + 5 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte h0;
         byte h1;
         byte i0;
         byte i1;
         byte s0;
         byte s1;
         if (c2 == 58 && c4 == 58) {
            h0 = c0;
            h1 = c1;
            i0 = 48;
            i1 = c3;
            s0 = 48;
            s1 = c5;
         } else if (c1 == 58 && c4 == 58) {
            h0 = 48;
            h1 = c0;
            i0 = c2;
            i1 = c3;
            s0 = 48;
            s1 = c5;
         } else {
            if (c1 != 58 || c3 != 58) {
               return null;
            }

            h0 = 48;
            h1 = c0;
            i0 = 48;
            i1 = c2;
            s0 = c4;
            s1 = c5;
         }

         if (h0 >= 48 && h0 <= 57 && h1 >= 48 && h1 <= 57) {
            int hour = (h0 - 48) * 10 + (h1 - 48);
            if (i0 >= 48 && i0 <= 57 && i1 >= 48 && i1 <= 57) {
               int minute = (i0 - 48) * 10 + (i1 - 48);
               if (s0 >= 48 && s0 <= 57 && s1 >= 48 && s1 <= 57) {
                  int second = (s0 - 48) * 10 + (s1 - 48);
                  return LocalTime.of(hour, minute, second);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime6(char[] chars, int off) {
      if (off + 5 > chars.length) {
         return null;
      } else {
         char c0 = chars[off];
         char c1 = chars[off + 1];
         char c2 = chars[off + 2];
         char c3 = chars[off + 3];
         char c4 = chars[off + 4];
         char c5 = chars[off + 5];
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c2 == ':' && c4 == ':') {
            h0 = c0;
            h1 = c1;
            i0 = '0';
            i1 = c3;
            s0 = '0';
            s1 = c5;
         } else if (c1 == ':' && c4 == ':') {
            h0 = '0';
            h1 = c0;
            i0 = c2;
            i1 = c3;
            s0 = '0';
            s1 = c5;
         } else {
            if (c1 != ':' || c3 != ':') {
               return null;
            }

            h0 = '0';
            h1 = c0;
            i0 = '0';
            i1 = c2;
            s0 = c4;
            s1 = c5;
         }

         if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
            int hour = (h0 - '0') * 10 + (h1 - '0');
            if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
               int minute = (i0 - '0') * 10 + (i1 - '0');
               if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                  int second = (s0 - '0') * 10 + (s1 - '0');
                  return LocalTime.of(hour, minute, second);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime7(byte[] bytes, int off) {
      if (off + 5 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte c6 = bytes[off + 6];
         byte h0;
         byte h1;
         byte i0;
         byte i1;
         byte s0;
         byte s1;
         if (c1 == 58 && c4 == 58) {
            h0 = 48;
            h1 = c0;
            i0 = c2;
            i1 = c3;
            s0 = c5;
            s1 = c6;
         } else if (c2 == 58 && c4 == 58) {
            h0 = c0;
            h1 = c1;
            i0 = 48;
            i1 = c3;
            s0 = c5;
            s1 = c6;
         } else {
            if (c2 != 58 || c5 != 58) {
               return null;
            }

            h0 = c0;
            h1 = c1;
            i0 = c3;
            i1 = c4;
            s0 = 48;
            s1 = c6;
         }

         if (h0 >= 48 && h0 <= 57 && h1 >= 48 && h1 <= 57) {
            int hour = (h0 - 48) * 10 + (h1 - 48);
            if (i0 >= 48 && i0 <= 57 && i1 >= 48 && i1 <= 57) {
               int minute = (i0 - 48) * 10 + (i1 - 48);
               if (s0 >= 48 && s0 <= 57 && s1 >= 48 && s1 <= 57) {
                  int second = (s0 - 48) * 10 + (s1 - 48);
                  return LocalTime.of(hour, minute, second);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime7(char[] chars, int off) {
      if (off + 5 > chars.length) {
         return null;
      } else {
         char c0 = chars[off];
         char c1 = chars[off + 1];
         char c2 = chars[off + 2];
         char c3 = chars[off + 3];
         char c4 = chars[off + 4];
         char c5 = chars[off + 5];
         char c6 = chars[off + 6];
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c1 == ':' && c4 == ':') {
            h0 = '0';
            h1 = c0;
            i0 = c2;
            i1 = c3;
            s0 = c5;
            s1 = c6;
         } else if (c2 == ':' && c4 == ':') {
            h0 = c0;
            h1 = c1;
            i0 = '0';
            i1 = c3;
            s0 = c5;
            s1 = c6;
         } else {
            if (c2 != ':' || c5 != ':') {
               return null;
            }

            h0 = c0;
            h1 = c1;
            i0 = c3;
            i1 = c4;
            s0 = '0';
            s1 = c6;
         }

         if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
            int hour = (h0 - '0') * 10 + (h1 - '0');
            if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
               int minute = (i0 - '0') * 10 + (i1 - '0');
               if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                  int second = (s0 - '0') * 10 + (s1 - '0');
                  return LocalTime.of(hour, minute, second);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime8(byte[] bytes, int off) {
      if (off + 8 > bytes.length) {
         return null;
      } else {
         char c0 = (char)bytes[off];
         char c1 = (char)bytes[off + 1];
         char c2 = (char)bytes[off + 2];
         char c3 = (char)bytes[off + 3];
         char c4 = (char)bytes[off + 4];
         char c5 = (char)bytes[off + 5];
         char c6 = (char)bytes[off + 6];
         char c7 = (char)bytes[off + 7];
         return parseLocalTime(c0, c1, c2, c3, c4, c5, c6, c7);
      }
   }

   public static LocalTime parseLocalTime8(char[] bytes, int off) {
      if (off + 8 > bytes.length) {
         return null;
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         return parseLocalTime(c0, c1, c2, c3, c4, c5, c6, c7);
      }
   }

   public static LocalTime parseLocalTime(char c0, char c1, char c2, char c3, char c4, char c5, char c6, char c7) {
      if (c2 != ':' || c5 != ':') {
         return null;
      } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9') {
         int hour = (c0 - '0') * 10 + (c1 - '0');
         if (c3 >= '0' && c3 <= '9' && c4 >= '0' && c4 <= '9') {
            int minute = (c3 - '0') * 10 + (c4 - '0');
            if (c6 >= '0' && c6 <= '9' && c7 >= '0' && c7 <= '9') {
               int second = (c6 - '0') * 10 + (c7 - '0');
               return LocalTime.of(hour, minute, second);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static LocalTime parseLocalTime10(byte[] bytes, int off) {
      if (off + 10 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte c6 = bytes[off + 6];
         byte c7 = bytes[off + 7];
         byte c8 = bytes[off + 8];
         byte c9 = bytes[off + 9];
         if (c2 != 58 || c5 != 58 || c8 != 46) {
            return null;
         } else if (c0 >= 48 && c0 <= 57 && c1 >= 48 && c1 <= 57) {
            int hour = (c0 - 48) * 10 + (c1 - 48);
            if (c3 >= 48 && c3 <= 57 && c4 >= 48 && c4 <= 57) {
               int minute = (c3 - 48) * 10 + (c4 - 48);
               if (c6 >= 48 && c6 <= 57 && c7 >= 48 && c7 <= 57) {
                  int second = (c6 - 48) * 10 + (c7 - 48);
                  if (c9 >= 48 && c9 <= 57) {
                     int millis = (c9 - 48) * 100;
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime10(char[] bytes, int off) {
      if (off + 10 > bytes.length) {
         return null;
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         char c8 = bytes[off + 8];
         char c9 = bytes[off + 9];
         if (c2 != ':' || c5 != ':' || c8 != '.') {
            return null;
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9') {
            int hour = (c0 - '0') * 10 + (c1 - '0');
            if (c3 >= '0' && c3 <= '9' && c4 >= '0' && c4 <= '9') {
               int minute = (c3 - '0') * 10 + (c4 - '0');
               if (c6 >= '0' && c6 <= '9' && c7 >= '0' && c7 <= '9') {
                  int second = (c6 - '0') * 10 + (c7 - '0');
                  if (c9 >= '0' && c9 <= '9') {
                     int millis = (c9 - '0') * 100;
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime11(byte[] bytes, int off) {
      if (off + 11 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte c6 = bytes[off + 6];
         byte c7 = bytes[off + 7];
         byte c8 = bytes[off + 8];
         byte c9 = bytes[off + 9];
         byte c10 = bytes[off + 10];
         if (c2 != 58 || c5 != 58 || c8 != 46) {
            return null;
         } else if (c0 >= 48 && c0 <= 57 && c1 >= 48 && c1 <= 57) {
            int hour = (c0 - 48) * 10 + (c1 - 48);
            if (c3 >= 48 && c3 <= 57 && c4 >= 48 && c4 <= 57) {
               int minute = (c3 - 48) * 10 + (c4 - 48);
               if (c6 >= 48 && c6 <= 57 && c7 >= 48 && c7 <= 57) {
                  int second = (c6 - 48) * 10 + (c7 - 48);
                  if (c9 >= 48 && c9 <= 57 && c10 >= 48 && c10 <= 57) {
                     int millis = (c9 - 48) * 100 + (c10 - 48) * 10;
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime11(char[] bytes, int off) {
      if (off + 11 > bytes.length) {
         return null;
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         char c8 = bytes[off + 8];
         char c9 = bytes[off + 9];
         char c10 = bytes[off + 10];
         if (c2 != ':' || c5 != ':' || c8 != '.') {
            return null;
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9') {
            int hour = (c0 - '0') * 10 + (c1 - '0');
            if (c3 >= '0' && c3 <= '9' && c4 >= '0' && c4 <= '9') {
               int minute = (c3 - '0') * 10 + (c4 - '0');
               if (c6 >= '0' && c6 <= '9' && c7 >= '0' && c7 <= '9') {
                  int second = (c6 - '0') * 10 + (c7 - '0');
                  if (c9 >= '0' && c9 <= '9' && c10 >= '0' && c10 <= '9') {
                     int millis = (c9 - '0') * 100 + (c10 - '0') * 10;
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime12(byte[] bytes, int off) {
      if (off + 12 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte c6 = bytes[off + 6];
         byte c7 = bytes[off + 7];
         byte c8 = bytes[off + 8];
         byte c9 = bytes[off + 9];
         byte c10 = bytes[off + 10];
         byte c11 = bytes[off + 11];
         if (c2 != 58 || c5 != 58 || c8 != 46) {
            return null;
         } else if (c0 >= 48 && c0 <= 57 && c1 >= 48 && c1 <= 57) {
            int hour = (c0 - 48) * 10 + (c1 - 48);
            if (c3 >= 48 && c3 <= 57 && c4 >= 48 && c4 <= 57) {
               int minute = (c3 - 48) * 10 + (c4 - 48);
               if (c6 >= 48 && c6 <= 57 && c7 >= 48 && c7 <= 57) {
                  int second = (c6 - 48) * 10 + (c7 - 48);
                  if (c9 >= 48 && c9 <= 57 && c10 >= 48 && c10 <= 57 && c11 >= 48 && c11 <= 57) {
                     int millis = (c9 - 48) * 100 + (c10 - 48) * 10 + (c11 - 48);
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime12(char[] bytes, int off) {
      if (off + 12 > bytes.length) {
         return null;
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         char c8 = bytes[off + 8];
         char c9 = bytes[off + 9];
         char c10 = bytes[off + 10];
         char c11 = bytes[off + 11];
         if (c2 != ':' || c5 != ':' || c8 != '.') {
            return null;
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9') {
            int hour = (c0 - '0') * 10 + (c1 - '0');
            if (c3 >= '0' && c3 <= '9' && c4 >= '0' && c4 <= '9') {
               int minute = (c3 - '0') * 10 + (c4 - '0');
               if (c6 >= '0' && c6 <= '9' && c7 >= '0' && c7 <= '9') {
                  int second = (c6 - '0') * 10 + (c7 - '0');
                  if (c9 >= '0' && c9 <= '9' && c10 >= '0' && c10 <= '9' && c11 >= '0' && c11 <= '9') {
                     int millis = (c9 - '0') * 100 + (c10 - '0') * 10 + (c11 - '0');
                     millis *= 1000000;
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime18(byte[] bytes, int off) {
      if (off + 18 > bytes.length) {
         return null;
      } else {
         byte c0 = bytes[off];
         byte c1 = bytes[off + 1];
         byte c2 = bytes[off + 2];
         byte c3 = bytes[off + 3];
         byte c4 = bytes[off + 4];
         byte c5 = bytes[off + 5];
         byte c6 = bytes[off + 6];
         byte c7 = bytes[off + 7];
         byte c8 = bytes[off + 8];
         byte c9 = bytes[off + 9];
         byte c10 = bytes[off + 10];
         byte c11 = bytes[off + 11];
         byte c12 = bytes[off + 12];
         byte c13 = bytes[off + 13];
         byte c14 = bytes[off + 14];
         byte c15 = bytes[off + 15];
         byte c16 = bytes[off + 16];
         byte c17 = bytes[off + 17];
         if (c2 != 58 || c5 != 58 || c8 != 46) {
            return null;
         } else if (c0 >= 48 && c0 <= 57 && c1 >= 48 && c1 <= 57) {
            int hour = (c0 - 48) * 10 + (c1 - 48);
            if (c3 >= 48 && c3 <= 57 && c4 >= 48 && c4 <= 57) {
               int minute = (c3 - 48) * 10 + (c4 - 48);
               if (c6 >= 48 && c6 <= 57 && c7 >= 48 && c7 <= 57) {
                  int second = (c6 - 48) * 10 + (c7 - 48);
                  if (c9 >= 48
                     && c9 <= 57
                     && c10 >= 48
                     && c10 <= 57
                     && c11 >= 48
                     && c11 <= 57
                     && c12 >= 48
                     && c12 <= 57
                     && c13 >= 48
                     && c13 <= 57
                     && c14 >= 48
                     && c14 <= 57
                     && c15 >= 48
                     && c15 <= 57
                     && c16 >= 48
                     && c16 <= 57
                     && c17 >= 48
                     && c17 <= 57) {
                     int millis = (c9 - 48) * 100000000
                        + (c10 - 48) * 10000000
                        + (c11 - 48) * 1000000
                        + (c12 - 48) * 100000
                        + (c13 - 48) * 10000
                        + (c14 - 48) * 1000
                        + (c15 - 48) * 100
                        + (c16 - 48) * 10
                        + (c17 - 48);
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalTime parseLocalTime18(char[] bytes, int off) {
      if (off + 18 > bytes.length) {
         return null;
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         char c8 = bytes[off + 8];
         char c9 = bytes[off + 9];
         char c10 = bytes[off + 10];
         char c11 = bytes[off + 11];
         char c12 = bytes[off + 12];
         char c13 = bytes[off + 13];
         char c14 = bytes[off + 14];
         char c15 = bytes[off + 15];
         char c16 = bytes[off + 16];
         char c17 = bytes[off + 17];
         if (c2 != ':' || c5 != ':' || c8 != '.') {
            return null;
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9') {
            int hour = (c0 - '0') * 10 + (c1 - '0');
            if (c3 >= '0' && c3 <= '9' && c4 >= '0' && c4 <= '9') {
               int minute = (c3 - '0') * 10 + (c4 - '0');
               if (c6 >= '0' && c6 <= '9' && c7 >= '0' && c7 <= '9') {
                  int second = (c6 - '0') * 10 + (c7 - '0');
                  if (c9 >= '0'
                     && c9 <= '9'
                     && c10 >= '0'
                     && c10 <= '9'
                     && c11 >= '0'
                     && c11 <= '9'
                     && c12 >= '0'
                     && c12 <= '9'
                     && c13 >= '0'
                     && c13 <= '9'
                     && c14 >= '0'
                     && c14 <= '9'
                     && c15 >= '0'
                     && c15 <= '9'
                     && c16 >= '0'
                     && c16 <= '9'
                     && c17 >= '0'
                     && c17 <= '9') {
                     int millis = (c9 - '0') * 100000000
                        + (c10 - '0') * 10000000
                        + (c11 - '0') * 1000000
                        + (c12 - '0') * 100000
                        + (c13 - '0') * 10000
                        + (c14 - '0') * 1000
                        + (c15 - '0') * 100
                        + (c16 - '0') * 10
                        + (c17 - '0');
                     return LocalTime.of(hour, minute, second, millis);
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime(byte[] str, int off, int len) {
      if (str != null && len != 0) {
         switch (len) {
            case 4:
               if (str[off] == 110 && str[off + 1] == 117 && str[off + 2] == 108 && str[off + 3] == 108) {
                  return null;
               }

               String input = new String(str, off, len);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            case 5:
            case 6:
            case 7:
            case 13:
            case 15:
            default:
               return parseLocalDateTimeX(str, off, len);
            case 8:
               LocalDate localDate = parseLocalDate8(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 9:
               LocalDate localDate = parseLocalDate9(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 10:
               LocalDate localDate = parseLocalDate10(str, off);
               if (localDate == null) {
                  return null;
               }

               return LocalDateTime.of(localDate, LocalTime.MIN);
            case 11:
               return LocalDateTime.of(parseLocalDate11(str, off), LocalTime.MIN);
            case 12:
               return parseLocalDateTime12(str, off);
            case 14:
               return parseLocalDateTime14(str, off);
            case 16:
               return parseLocalDateTime16(str, off);
            case 17:
               return parseLocalDateTime17(str, off);
            case 18:
               return parseLocalDateTime18(str, off);
            case 19:
               return parseLocalDateTime19(str, off);
            case 20:
               return parseLocalDateTime20(str, off);
         }
      } else {
         return null;
      }
   }

   public static LocalDate parseLocalDate(String str) {
      if (str == null) {
         return null;
      } else {
         LocalDate localDate;
         if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            localDate = parseLocalDate(bytes, 0, bytes.length);
         } else {
            char[] chars = JDKUtils.getCharArray(str);
            localDate = parseLocalDate(chars, 0, chars.length);
         }

         if (localDate == null) {
            switch (str) {
               case "":
               case "null":
               case "00000000":
               case "0000年00月00日":
               case "0000-0-00":
               case "0000-00-00":
                  return null;
               default:
                  throw new DateTimeParseException(str, str, 0);
            }
         } else {
            return localDate;
         }
      }
   }

   public static LocalDate parseLocalDate(byte[] str, int off, int len) {
      if (str != null && len != 0) {
         if (off + len > str.length) {
            String input = new String(str, off, len);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         } else {
            switch (len) {
               case 8:
                  return parseLocalDate8(str, off);
               case 9:
                  return parseLocalDate9(str, off);
               case 10:
                  return parseLocalDate10(str, off);
               case 11:
                  return parseLocalDate11(str, off);
               default:
                  if (len == 4 && str[off] == 110 && str[off + 1] == 117 && str[off + 2] == 108 && str[off + 3] == 108) {
                     return null;
                  } else {
                     String input = new String(str, off, len);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
            }
         }
      } else {
         return null;
      }
   }

   public static LocalDate parseLocalDate(char[] str, int off, int len) {
      if (str != null && len != 0) {
         if (off + len > str.length) {
            String input = new String(str, off, len);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         } else {
            switch (len) {
               case 8:
                  return parseLocalDate8(str, off);
               case 9:
                  return parseLocalDate9(str, off);
               case 10:
                  return parseLocalDate10(str, off);
               case 11:
                  return parseLocalDate11(str, off);
               default:
                  if (len == 4 && str[off] == 'n' && str[off + 1] == 'u' && str[off + 2] == 'l' && str[off + 3] == 'l') {
                     return null;
                  } else {
                     String input = new String(str, off, len);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
            }
         }
      } else {
         return null;
      }
   }

   public static long parseMillis(byte[] bytes, int off, int len) {
      return parseMillis(bytes, off, len, StandardCharsets.UTF_8, DEFAULT_ZONE_ID);
   }

   public static long parseMillis(byte[] bytes, int off, int len, Charset charset) {
      return parseMillis(bytes, off, len, charset, DEFAULT_ZONE_ID);
   }

   public static long parseMillis(byte[] chars, int off, int len, Charset charset, ZoneId zoneId) {
      if (chars != null && len != 0) {
         if (len == 4 && chars[off] == 110 && chars[off + 1] == 117 && chars[off + 2] == 108 && chars[off + 3] == 108) {
            return 0L;
         } else {
            char c0 = (char)chars[off];
            long millis;
            if (c0 == '"' && chars[len - 1] == 34) {
               JSONReader jsonReader = JSONReader.of(chars, off, len, charset);

               try {
                  Date date = (Date)ObjectReaderImplDate.INSTANCE.readObject(jsonReader, null, null, 0L);
                  millis = date.getTime();
               } catch (Throwable var18) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }
            } else if (len == 19) {
               millis = parseMillis19(chars, off, zoneId);
            } else {
               char c10;
               if (len <= 19 && (len != 16 || (c10 = (char)chars[off + 10]) != '+' && c10 != '-')) {
                  if ((c0 == '-' || c0 >= '0' && c0 <= '9') && IOUtils.isNumber(chars, off, len)) {
                     millis = TypeUtils.parseLong(chars, off, len);
                     if (len == 8 && millis >= 19700101L && millis <= 21000101L) {
                        int year = (int)millis / 10000;
                        int month = (int)millis % 10000 / 100;
                        int dom = (int)millis % 100;
                        if (month >= 1 && month <= 12) {
                           int max = 31;
                           switch (month) {
                              case 2:
                                 boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                                 max = leapYear ? 29 : 28;
                              case 3:
                              case 5:
                              case 7:
                              case 8:
                              case 10:
                              default:
                                 break;
                              case 4:
                              case 6:
                              case 9:
                              case 11:
                                 max = 30;
                           }

                           if (dom <= max) {
                              LocalDateTime ldt = LocalDateTime.of(year, month, dom, 0, 0, 0);
                              ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt, zoneId, null);
                              long seconds = zdt.toEpochSecond();
                              millis = seconds * 1000L;
                           }
                        }
                     }
                  } else {
                     char last = (char)chars[len - 1];
                     if (last == 'Z') {
                        zoneId = ZoneOffset.UTC;
                     }

                     LocalDateTime ldt = parseLocalDateTime(chars, off, len);
                     if (ldt == null
                        && chars[off] == 48
                        && chars[off + 1] == 48
                        && chars[off + 2] == 48
                        && chars[off + 3] == 48
                        && chars[off + 4] == 45
                        && chars[off + 5] == 48
                        && chars[off + 6] == 48
                        && chars[off + 7] == 45
                        && chars[off + 8] == 48
                        && chars[off + 9] == 48) {
                        ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
                     }

                     ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt, zoneId, null);
                     long seconds = zdt.toEpochSecond();
                     int nanos = ldt.getNano();
                     if (seconds < 0L && nanos > 0) {
                        millis = (seconds + 1L) * 1000L + (long)(nanos / 1000000) - 1000L;
                     } else {
                        millis = seconds * 1000L + (long)(nanos / 1000000);
                     }
                  }
               } else {
                  ZonedDateTime zdt = parseZonedDateTime(chars, off, len, zoneId);
                  if (zdt == null) {
                     String input = new String(chars, off, len - off);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }

                  millis = zdt.toInstant().toEpochMilli();
               }
            }

            return millis;
         }
      } else {
         return 0L;
      }
   }

   public static long parseMillis(char[] bytes, int off, int len) {
      return parseMillis(bytes, off, len, DEFAULT_ZONE_ID);
   }

   public static long parseMillis(char[] chars, int off, int len, ZoneId zoneId) {
      if (chars != null && len != 0) {
         if (len == 4 && chars[off] == 'n' && chars[off + 1] == 'u' && chars[off + 2] == 'l' && chars[off + 3] == 'l') {
            return 0L;
         } else {
            char c0 = chars[off];
            long millis;
            if (c0 == '"' && chars[len - 1] == '"') {
               JSONReader jsonReader = JSONReader.of(chars, off, len);

               try {
                  Date date = (Date)ObjectReaderImplDate.INSTANCE.readObject(jsonReader, null, null, 0L);
                  millis = date.getTime();
               } catch (Throwable var17) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }
            } else if (len == 19) {
               millis = parseMillis19(chars, off, zoneId);
            } else {
               char c10;
               if (len <= 19 && (len != 16 || (c10 = chars[off + 10]) != '+' && c10 != '-')) {
                  if ((c0 == '-' || c0 >= '0' && c0 <= '9') && IOUtils.isNumber(chars, off, len)) {
                     millis = TypeUtils.parseLong(chars, off, len);
                     if (len == 8 && millis >= 19700101L && millis <= 21000101L) {
                        int year = (int)millis / 10000;
                        int month = (int)millis % 10000 / 100;
                        int dom = (int)millis % 100;
                        if (month >= 1 && month <= 12) {
                           int max = 31;
                           switch (month) {
                              case 2:
                                 boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                                 max = leapYear ? 29 : 28;
                              case 3:
                              case 5:
                              case 7:
                              case 8:
                              case 10:
                              default:
                                 break;
                              case 4:
                              case 6:
                              case 9:
                              case 11:
                                 max = 30;
                           }

                           if (dom <= max) {
                              LocalDateTime ldt = LocalDateTime.of(year, month, dom, 0, 0, 0);
                              ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt, zoneId, null);
                              long seconds = zdt.toEpochSecond();
                              millis = seconds * 1000L;
                           }
                        }
                     }
                  } else {
                     char last = chars[len - 1];
                     if (last == 'Z') {
                        len--;
                        zoneId = ZoneOffset.UTC;
                     }

                     LocalDateTime ldt = parseLocalDateTime(chars, off, len);
                     if (ldt == null
                        && chars[off] == '0'
                        && chars[off + 1] == '0'
                        && chars[off + 2] == '0'
                        && chars[off + 3] == '0'
                        && chars[off + 4] == '-'
                        && chars[off + 5] == '0'
                        && chars[off + 6] == '0'
                        && chars[off + 7] == '-'
                        && chars[off + 8] == '0'
                        && chars[off + 9] == '0') {
                        ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
                     }

                     if (ldt == null) {
                        String input = new String(chars, off, len - off);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }

                     ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt, zoneId, null);
                     long seconds = zdt.toEpochSecond();
                     int nanos = ldt.getNano();
                     if (seconds < 0L && nanos > 0) {
                        millis = (seconds + 1L) * 1000L + (long)(nanos / 1000000) - 1000L;
                     } else {
                        millis = seconds * 1000L + (long)(nanos / 1000000);
                     }
                  }
               } else {
                  ZonedDateTime zdt = parseZonedDateTime(chars, off, len, zoneId);
                  if (zdt == null) {
                     String input = new String(chars, off, len - off);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }

                  millis = zdt.toInstant().toEpochMilli();
               }
            }

            return millis;
         }
      } else {
         return 0L;
      }
   }

   public static LocalDate parseLocalDate8(byte[] str, int off) {
      if (off + 8 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c6 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = '0';
            d1 = c7;
         } else if (c1 == '/' && c3 == '/') {
            m0 = '0';
            m1 = c0;
            d0 = '0';
            d1 = c2;
            y0 = c4;
            y1 = c5;
            y2 = c6;
            y3 = c7;
         } else if (c1 == '-' && c5 == '-') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = '2';
            y1 = '0';
            y2 = c6;
            y3 = c7;
         } else {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c4;
            m1 = c5;
            d0 = c6;
            d1 = c7;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate8(char[] str, int off) {
      if (off + 8 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c6 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = '0';
            d1 = c7;
         } else if (c1 == '/' && c3 == '/') {
            m0 = '0';
            m1 = c0;
            d0 = '0';
            d1 = c2;
            y0 = c4;
            y1 = c5;
            y2 = c6;
            y3 = c7;
         } else if (c1 == '-' && c5 == '-') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = '2';
            y1 = '0';
            y2 = c6;
            y3 = c7;
         } else {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c4;
            m1 = c5;
            d0 = c6;
            d1 = c7;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate9(byte[] str, int off) {
      if (off + 9 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c7 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else if (c4 == '-' && c6 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c4 == '/' && c7 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else if (c4 == '/' && c6 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c1 == '.' && c4 == '.') {
            d0 = '0';
            d1 = c0;
            m0 = c2;
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c2 == '.' && c4 == '.') {
            d0 = c0;
            d1 = c1;
            m0 = '0';
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c1 == '-' && c4 == '-') {
            d0 = '0';
            d1 = c0;
            m0 = c2;
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c2 == '-' && c4 == '-') {
            d0 = c0;
            d1 = c1;
            m0 = '0';
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c2 == '-' && c6 == '-') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = '2';
            y1 = '0';
            y2 = c7;
            y3 = c8;
         } else if (c1 == '/' && c4 == '/') {
            m0 = '0';
            m1 = c0;
            d0 = c2;
            d1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else {
            if (c2 != '/' || c4 != '/') {
               return null;
            }

            m0 = c0;
            m1 = c1;
            d0 = '0';
            d1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate9(char[] str, int off) {
      if (off + 9 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c7 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else if (c4 == '-' && c6 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c4 == '/' && c7 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else if (c4 == '/' && c6 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c1 == '.' && c4 == '.') {
            d0 = '0';
            d1 = c0;
            m0 = c2;
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c2 == '.' && c4 == '.') {
            d0 = c0;
            d1 = c1;
            m0 = '0';
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c1 == '-' && c4 == '-') {
            d0 = '0';
            d1 = c0;
            m0 = c2;
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c2 == '-' && c4 == '-') {
            d0 = c0;
            d1 = c1;
            m0 = '0';
            m1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else if (c4 == 24180 && c6 == 26376 && c8 == 26085) {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = '0';
            d1 = c7;
         } else if (c4 == '년' && c6 == '월' && c8 == '일') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = '0';
            d1 = c7;
         } else if (c2 == '-' && c6 == '-') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = '2';
            y1 = '0';
            y2 = c7;
            y3 = c8;
         } else if (c1 == '/' && c4 == '/') {
            m0 = '0';
            m1 = c0;
            d0 = c2;
            d1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         } else {
            if (c2 != '/' || c4 != '/') {
               return null;
            }

            m0 = c0;
            m1 = c1;
            d0 = '0';
            d1 = c3;
            y0 = c5;
            y1 = c6;
            y2 = c7;
            y3 = c8;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate10(byte[] str, int off) {
      if (off + 10 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c7 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c4 == '/' && c7 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c2 == '.' && c5 == '.') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else if (c2 == '-' && c5 == '-') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else if (c2 == '/' && c5 == '/') {
            m0 = c0;
            m1 = c1;
            d0 = c3;
            d1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else {
            if (c1 != ' ' || c5 != ' ') {
               return null;
            }

            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = '0';
            d1 = c0;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate10(char[] str, int off) {
      if (off + 10 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c7 == '-') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c4 == '/' && c7 == '/') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c2 == '.' && c5 == '.') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else if (c2 == '-' && c5 == '-') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else if (c2 == '/' && c5 == '/') {
            m0 = c0;
            m1 = c1;
            d0 = c3;
            d1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
         } else if (c4 == 24180 && c6 == 26376 && c9 == 26085) {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c4 == '년' && c6 == '월' && c9 == '일') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
         } else if (c4 == 24180 && c7 == 26376 && c9 == 26085) {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else if (c4 == '년' && c7 == '월' && c9 == '일') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
         } else {
            if (c1 != ' ' || c5 != ' ') {
               return null;
            }

            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = '0';
            d1 = c0;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate11(char[] str, int off) {
      if (off + 11 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == 24180 && c7 == 26376 && c10 == 26085) {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c4 == '-' && c7 == '-' && c10 == 'Z') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else if (c4 == '년' && c7 == '월' && c10 == '일') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else {
            if (c2 != ' ' || c6 != ' ') {
               return null;
            }

            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = c0;
            d1 = c1;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDate parseLocalDate11(byte[] str, int off) {
      if (off + 11 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         if (c4 == '-' && c7 == '-' && c10 == 'Z') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
         } else {
            if (c2 != ' ' || c6 != ' ') {
               return null;
            }

            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = c0;
            d1 = c1;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  return year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime12(char[] str, int off) {
      if (off + 12 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char y0 = str[off];
         char y1 = str[off + 1];
         char y2 = str[off + 2];
         char y3 = str[off + 3];
         char m0 = str[off + 4];
         char m1 = str[off + 5];
         char d0 = str[off + 6];
         char d1 = str[off + 7];
         char h0 = str[off + 8];
         char h1 = str[off + 9];
         char i0 = str[off + 10];
         char i1 = str[off + 11];
         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        return year == 0 && month == 0 && dom == 0 && hour == 0 && minute == 0 ? null : LocalDateTime.of(year, month, dom, hour, minute, 0);
                     } else {
                        String input = new String(str, off, off + 12);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }
                  } else {
                     String input = new String(str, off, off + 12);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
               } else {
                  String input = new String(str, off, off + 12);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, off + 12);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, off + 12);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static LocalDateTime parseLocalDateTime12(byte[] str, int off) {
      if (off + 12 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char y0 = (char)str[off];
         char y1 = (char)str[off + 1];
         char y2 = (char)str[off + 2];
         char y3 = (char)str[off + 3];
         char m0 = (char)str[off + 4];
         char m1 = (char)str[off + 5];
         char d0 = (char)str[off + 6];
         char d1 = (char)str[off + 7];
         char h0 = (char)str[off + 8];
         char h1 = (char)str[off + 9];
         char i0 = (char)str[off + 10];
         char i1 = (char)str[off + 11];
         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        return year == 0 && month == 0 && dom == 0 && hour == 0 && minute == 0 ? null : LocalDateTime.of(year, month, dom, hour, minute, 0);
                     } else {
                        String input = new String(str, off, off + 12);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }
                  } else {
                     String input = new String(str, off, off + 12);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
               } else {
                  String input = new String(str, off, off + 12);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, off + 12);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, off + 12);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static LocalDateTime parseLocalDateTime14(char[] str, int off) {
      if (off + 14 > str.length) {
         return null;
      } else {
         char y0 = str[off];
         char y1 = str[off + 1];
         char y2 = str[off + 2];
         char y3 = str[off + 3];
         char m0 = str[off + 4];
         char m1 = str[off + 5];
         char d0 = str[off + 6];
         char d1 = str[off + 7];
         char h0 = str[off + 8];
         char h1 = str[off + 9];
         char i0 = str[off + 10];
         char i1 = str[off + 11];
         char s0 = str[off + 12];
         char s1 = str[off + 13];
         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime14(byte[] str, int off) {
      if (off + 14 > str.length) {
         return null;
      } else {
         char y0 = (char)str[off];
         char y1 = (char)str[off + 1];
         char y2 = (char)str[off + 2];
         char y3 = (char)str[off + 3];
         char m0 = (char)str[off + 4];
         char m1 = (char)str[off + 5];
         char d0 = (char)str[off + 6];
         char d1 = (char)str[off + 7];
         char h0 = (char)str[off + 8];
         char h1 = (char)str[off + 9];
         char i0 = (char)str[off + 10];
         char i1 = (char)str[off + 11];
         char s0 = (char)str[off + 12];
         char s1 = (char)str[off + 13];
         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime16(char[] str, int off) {
      if (off + 16 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char s0 = '0';
         char s1 = '0';
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         if (c4 == '-' && c7 == '-' && (c10 == 'T' || c10 == ' ') && c13 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
         } else if (c8 == 'T' && c15 == 'Z') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c4;
            m1 = c5;
            d0 = c6;
            d1 = c7;
            h0 = c9;
            h1 = c10;
            i0 = c11;
            i1 = c12;
            s0 = c13;
            s1 = c14;
         } else if (c4 == '-' && c7 == '-' && (c10 == 'T' || c10 == ' ') && c12 == ':' && c14 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = '0';
            h1 = c11;
            i0 = '0';
            i1 = c13;
            s1 = c15;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':') {
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = '0';
            d1 = c0;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
         } else {
            if (c1 != ' ' || c5 != ' ' || c10 != ' ' || c12 != ':' || c14 != ':') {
               return null;
            }

            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = '0';
            h1 = c11;
            i0 = '0';
            i1 = c13;
            s1 = c15;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime16(byte[] str, int off) {
      if (off + 16 > str.length) {
         return null;
      } else {
         byte c0 = str[off];
         byte c1 = str[off + 1];
         byte c2 = str[off + 2];
         byte c3 = str[off + 3];
         byte c4 = str[off + 4];
         byte c5 = str[off + 5];
         byte c6 = str[off + 6];
         byte c7 = str[off + 7];
         byte c8 = str[off + 8];
         byte c9 = str[off + 9];
         byte c10 = str[off + 10];
         byte c11 = str[off + 11];
         byte c12 = str[off + 12];
         byte c13 = str[off + 13];
         byte c14 = str[off + 14];
         byte c15 = str[off + 15];
         char s0 = '0';
         char s1 = '0';
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         if (c4 == 45 && c7 == 45 && (c10 == 84 || c10 == 32) && c13 == 58) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c5;
            m1 = (char)c6;
            d0 = (char)c8;
            d1 = (char)c9;
            h0 = (char)c11;
            h1 = (char)c12;
            i0 = (char)c14;
            i1 = (char)c15;
         } else if (c8 == 84 && c15 == 90) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c4;
            m1 = (char)c5;
            d0 = (char)c6;
            d1 = (char)c7;
            h0 = (char)c9;
            h1 = (char)c10;
            i0 = (char)c11;
            i1 = (char)c12;
            s0 = (char)c13;
            s1 = (char)c14;
         } else if (c4 == -27 && c5 == -71 && c6 == -76 && c8 == -26 && c9 == -100 && c10 == -120 && c13 == -26 && c14 == -105 && c15 == -91) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = '0';
            m1 = (char)c7;
            d0 = (char)c11;
            d1 = (char)c12;
            h0 = '0';
            h1 = '0';
            i0 = '0';
            i1 = '0';
         } else if (c4 == -27 && c5 == -71 && c6 == -76 && c9 == -26 && c10 == -100 && c11 == -120 && c13 == -26 && c14 == -105 && c15 == -91) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c7;
            m1 = (char)c8;
            d0 = '0';
            d1 = (char)c12;
            h0 = '0';
            h1 = '0';
            i0 = '0';
            i1 = '0';
         } else if (c4 == 45 && c7 == 45 && (c10 == 84 || c10 == 32) && c12 == 58 && c14 == 58) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c5;
            m1 = (char)c6;
            d0 = (char)c8;
            d1 = (char)c9;
            h0 = '0';
            h1 = (char)c11;
            i0 = '0';
            i1 = (char)c13;
            s1 = (char)c15;
         } else if (c1 == 32 && c5 == 32 && c10 == 32 && c13 == 58) {
            y0 = (char)c6;
            y1 = (char)c7;
            y2 = (char)c8;
            y3 = (char)c9;
            int month = month((char)c2, (char)c3, (char)c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = '0';
            d1 = (char)c0;
            h0 = (char)c11;
            h1 = (char)c12;
            i0 = (char)c14;
            i1 = (char)c15;
         } else {
            if (c1 != 32 || c5 != 32 || c10 != 32 || c12 != 58 || c14 != 58) {
               return null;
            }

            d0 = '0';
            d1 = (char)c0;
            int month = month((char)c2, (char)c3, (char)c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = (char)c6;
            y1 = (char)c7;
            y2 = (char)c8;
            y3 = (char)c9;
            h0 = '0';
            h1 = (char)c11;
            i0 = '0';
            i1 = (char)c13;
            s1 = (char)c15;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime17(char[] str, int off) {
      if (off + 17 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         int nanoOfSecond = 0;
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c7 == '-' && (c10 == 'T' || c10 == ' ') && c13 == ':' && c16 == 'Z') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = '0';
         } else if (c4 == '-' && c6 == '-' && (c8 == ' ' || c8 == 'T') && c11 == ':' && c14 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = '0';
            d1 = c7;
            h0 = c9;
            h1 = c10;
            i0 = c12;
            i1 = c13;
            s0 = c15;
            s1 = c16;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':') {
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = c0;
            d1 = c1;
            h0 = c12;
            h1 = c13;
            i0 = c15;
            i1 = c16;
            s0 = '0';
            s1 = '0';
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c12 == ':' && c14 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = '0';
            h1 = c11;
            i0 = '0';
            i1 = c13;
            s0 = c15;
            s1 = c16;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c12 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = '0';
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = '0';
            s1 = c16;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = '0';
            s1 = c16;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c15 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               return null;
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = '0';
            s1 = c16;
         } else {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c4;
            m1 = c5;
            d0 = c6;
            d1 = c7;
            h0 = c8;
            h1 = c9;
            i0 = c10;
            i1 = c11;
            s0 = c12;
            s1 = c13;
            if (c14 < '0' || c14 > '9' || c15 < '0' || c15 > '9' || c16 < '0' || c16 > '9') {
               return null;
            }

            nanoOfSecond = ((c14 - '0') * 100 + (c15 - '0') * 10 + (c16 - '0')) * 1000000;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second, nanoOfSecond);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime17(byte[] str, int off) {
      if (off + 17 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         byte c0 = str[off];
         byte c1 = str[off + 1];
         byte c2 = str[off + 2];
         byte c3 = str[off + 3];
         byte c4 = str[off + 4];
         byte c5 = str[off + 5];
         byte c6 = str[off + 6];
         byte c7 = str[off + 7];
         byte c8 = str[off + 8];
         byte c9 = str[off + 9];
         byte c10 = str[off + 10];
         byte c11 = str[off + 11];
         byte c12 = str[off + 12];
         byte c13 = str[off + 13];
         byte c14 = str[off + 14];
         byte c15 = str[off + 15];
         byte c16 = str[off + 16];
         int nanoOfSecond = 0;
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == 45 && c7 == 45 && (c10 == 84 || c10 == 32) && c13 == 58 && c16 == 90) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c5;
            m1 = (char)c6;
            d0 = (char)c8;
            d1 = (char)c9;
            h0 = (char)c11;
            h1 = (char)c12;
            i0 = (char)c14;
            i1 = (char)c15;
            s0 = '0';
            s1 = '0';
         } else if (c4 == 45 && c6 == 45 && (c8 == 32 || c8 == 84) && c11 == 58 && c14 == 58) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = '0';
            m1 = (char)c5;
            d0 = '0';
            d1 = (char)c7;
            h0 = (char)c9;
            h1 = (char)c10;
            i0 = (char)c12;
            i1 = (char)c13;
            s0 = (char)c15;
            s1 = (char)c16;
         } else if (c4 == -27 && c5 == -71 && c6 == -76 && c9 == -26 && c10 == -100 && c11 == -120 && c14 == -26 && c15 == -105 && c16 == -91) {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c7;
            m1 = (char)c8;
            d0 = (char)c12;
            d1 = (char)c13;
            h0 = '0';
            h1 = '0';
            i0 = '0';
            i1 = '0';
            s0 = '0';
            s1 = '0';
         } else if (c2 == 32 && c6 == 32 && c11 == 32 && c14 == 58) {
            y0 = (char)c7;
            y1 = (char)c8;
            y2 = (char)c9;
            y3 = (char)c10;
            int month = month((char)c3, (char)c4, (char)c5);
            if (month <= 0) {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            d0 = (char)c0;
            d1 = (char)c1;
            h0 = (char)c12;
            h1 = (char)c13;
            i0 = (char)c15;
            i1 = (char)c16;
            s0 = '0';
            s1 = '0';
         } else if (c1 == 32 && c5 == 32 && c10 == 32 && c12 == 58 && c14 == 58) {
            d0 = '0';
            d1 = (char)c0;
            int month = month((char)c2, (char)c3, (char)c4);
            if (month <= 0) {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = (char)c6;
            y1 = (char)c7;
            y2 = (char)c8;
            y3 = (char)c9;
            h0 = '0';
            h1 = (char)c11;
            i0 = '0';
            i1 = (char)c13;
            s0 = (char)c15;
            s1 = (char)c16;
         } else if (c1 == 32 && c5 == 32 && c10 == 32 && c12 == 58 && c15 == 58) {
            d0 = '0';
            d1 = (char)c0;
            int month = month((char)c2, (char)c3, (char)c4);
            if (month <= 0) {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = (char)c6;
            y1 = (char)c7;
            y2 = (char)c8;
            y3 = (char)c9;
            h0 = '0';
            h1 = (char)c11;
            i0 = (char)c13;
            i1 = (char)c14;
            s0 = '0';
            s1 = (char)c16;
         } else if (c1 == 32 && c5 == 32 && c10 == 32 && c13 == 58 && c15 == 58) {
            d0 = '0';
            d1 = (char)c0;
            int month = month((char)c2, (char)c3, (char)c4);
            if (month <= 0) {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = (char)c6;
            y1 = (char)c7;
            y2 = (char)c8;
            y3 = (char)c9;
            h0 = (char)c11;
            h1 = (char)c12;
            i0 = '0';
            i1 = (char)c14;
            s0 = '0';
            s1 = (char)c16;
         } else if (c2 == 32 && c6 == 32 && c11 == 32 && c13 == 58 && c15 == 58) {
            d0 = (char)c0;
            d1 = (char)c1;
            int month = month((char)c3, (char)c4, (char)c5);
            if (month <= 0) {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = (char)c7;
            y1 = (char)c8;
            y2 = (char)c9;
            y3 = (char)c10;
            h0 = '0';
            h1 = (char)c12;
            i0 = '0';
            i1 = (char)c14;
            s0 = '0';
            s1 = (char)c16;
         } else {
            y0 = (char)c0;
            y1 = (char)c1;
            y2 = (char)c2;
            y3 = (char)c3;
            m0 = (char)c4;
            m1 = (char)c5;
            d0 = (char)c6;
            d1 = (char)c7;
            h0 = (char)c8;
            h1 = (char)c9;
            i0 = (char)c10;
            i1 = (char)c11;
            s0 = (char)c12;
            s1 = (char)c13;
            if (c14 < 48 || c14 > 57 || c15 < 48 || c15 > 57 || c16 < 48 || c16 > 57) {
               return null;
            }

            nanoOfSecond = ((c14 - 48) * 100 + (c15 - 48) * 10 + (c16 - 48)) * 1000000;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second, nanoOfSecond);
                        } else {
                           String input = new String(str, off, 17);
                           throw new DateTimeParseException("illegal input " + input, input, 0);
                        }
                     } else {
                        String input = new String(str, off, 17);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }
                  } else {
                     String input = new String(str, off, 17);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
               } else {
                  String input = new String(str, off, 17);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, 17);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, 17);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static LocalDateTime parseLocalDateTime18(char[] str, int off) {
      if (off + 18 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c6 == '-' && (c9 == ' ' || c9 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
            h0 = c10;
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c9 == ' ' || c9 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
            h0 = c10;
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = '0';
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c12 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = '0';
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = '0';
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else {
            if (c2 != ' ' || c6 != ' ' || c11 != ' ' || c13 != ':' || c15 != ':') {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           String input = new String(str, off, 18);
                           throw new DateTimeParseException("illegal input " + input, input, 0);
                        }
                     } else {
                        String input = new String(str, off, 18);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }
                  } else {
                     String input = new String(str, off, 18);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
               } else {
                  String input = new String(str, off, 18);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, 18);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static LocalDateTime parseLocalDateTime18(byte[] str, int off) {
      if (off + 18 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c6 == '-' && (c9 == ' ' || c9 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = '0';
            m1 = c5;
            d0 = c7;
            d1 = c8;
            h0 = c10;
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c9 == ' ' || c9 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = '0';
            d1 = c8;
            h0 = c10;
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c12 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = '0';
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c15 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c12 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = '0';
            h1 = c11;
            i0 = c13;
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c15 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = '0';
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = '0';
            s1 = c17;
         } else {
            if (c2 != ' ' || c6 != ' ' || c11 != ' ' || c13 != ':' || c15 != ':') {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = '0';
            i1 = c14;
            s0 = c16;
            s1 = c17;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     int hour = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second);
                        } else {
                           String input = new String(str, off, 18);
                           throw new DateTimeParseException("illegal input " + input, input, 0);
                        }
                     } else {
                        String input = new String(str, off, 18);
                        throw new DateTimeParseException("illegal input " + input, input, 0);
                     }
                  } else {
                     String input = new String(str, off, 18);
                     throw new DateTimeParseException("illegal input " + input, input, 0);
                  }
               } else {
                  String input = new String(str, off, 18);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, 18);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, 18);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static LocalDateTime parseLocalDateTime19(char[] str, int off) {
      if (off + 19 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c4 == '/' && c7 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == '/' && c5 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else {
            if (c1 != ' ' || c5 != ' ' || c10 != ' ' || c13 != ':' || c16 != ':') {
               return null;
            }

            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            int month = month(c2, c3, c4);
            if (month > 0) {
               m0 = (char)(48 + month / 10);
               m1 = (char)(48 + month % 10);
            } else {
               m0 = '0';
               m1 = '0';
            }

            d0 = '0';
            d1 = c0;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         }

         return localDateTime(y0, y1, y2, y3, m0, m1, d0, d1, h0, h1, i0, i1, s0, s1);
      }
   }

   public static LocalDateTime parseLocalDateTime19(String str, int off) {
      if (off + 19 > str.length()) {
         return null;
      } else {
         LocalDateTime ldt;
         if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            ldt = parseLocalDateTime19(bytes, off);
         } else if (JDKUtils.JVM_VERSION == 8 && !JDKUtils.FIELD_STRING_VALUE_ERROR) {
            char[] chars = JDKUtils.getCharArray(str);
            ldt = parseLocalDateTime19(chars, off);
         } else {
            char[] chars = new char[19];
            str.getChars(off, off + 19, chars, 0);
            ldt = parseLocalDateTime19(chars, off);
         }

         return ldt;
      }
   }

   public static LocalDateTime parseLocalDateTime19(byte[] str, int off) {
      if (off + 19 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c4 == '/' && c7 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == '/' && c5 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else {
            if (c1 != ' ' || c5 != ' ' || c10 != ' ' || c13 != ':' || c16 != ':') {
               return null;
            }

            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            int month = month(c2, c3, c4);
            if (month > 0) {
               m0 = (char)(48 + month / 10);
               m1 = (char)(48 + month % 10);
            } else {
               m0 = '0';
               m1 = '0';
            }

            d0 = '0';
            d1 = c0;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         }

         return localDateTime(y0, y1, y2, y3, m0, m1, d0, d1, h0, h1, i0, i1, s0, s1);
      }
   }

   public static LocalDateTime parseLocalDateTime20(char[] str, int off) {
      if (off + 19 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = str[off + 19];
         if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':') {
            int month = month(c3, c4, c5);
            char m0;
            char m1;
            if (month > 0) {
               m0 = (char)(48 + month / 10);
               m1 = (char)(48 + month % 10);
            } else {
               m0 = '0';
               m1 = '0';
            }

            return localDateTime(c7, c8, c9, c10, m0, m1, c0, c1, c12, c13, c15, c16, c18, c19);
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime20(byte[] str, int off) {
      if (off + 19 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = (char)str[off + 19];
         if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':') {
            int month = month(c3, c4, c5);
            char m0;
            char m1;
            if (month > 0) {
               m0 = (char)(48 + month / 10);
               m1 = (char)(48 + month % 10);
            } else {
               m0 = '0';
               m1 = '0';
            }

            return localDateTime(c7, c8, c9, c10, m0, m1, c0, c1, c12, c13, c15, c16, c18, c19);
         } else {
            return null;
         }
      }
   }

   public static LocalDateTime parseLocalDateTime26(byte[] str, int off) {
      if (off + 26 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = (char)str[off + 19];
         char c20 = (char)str[off + 20];
         char c21 = (char)str[off + 21];
         char c22 = (char)str[off + 22];
         char c23 = (char)str[off + 23];
         char c24 = (char)str[off + 24];
         char c25 = (char)str[off + 25];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, '0', '0', '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime26(char[] str, int off) {
      if (off + 26 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = str[off + 19];
         char c20 = str[off + 20];
         char c21 = str[off + 21];
         char c22 = str[off + 22];
         char c23 = str[off + 23];
         char c24 = str[off + 24];
         char c25 = str[off + 25];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, '0', '0', '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime27(byte[] str, int off) {
      if (off + 27 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = (char)str[off + 19];
         char c20 = (char)str[off + 20];
         char c21 = (char)str[off + 21];
         char c22 = (char)str[off + 22];
         char c23 = (char)str[off + 23];
         char c24 = (char)str[off + 24];
         char c25 = (char)str[off + 25];
         char c26 = (char)str[off + 26];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, '0', '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime27(char[] str, int off) {
      if (off + 27 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = str[off + 19];
         char c20 = str[off + 20];
         char c21 = str[off + 21];
         char c22 = str[off + 22];
         char c23 = str[off + 23];
         char c24 = str[off + 24];
         char c25 = str[off + 25];
         char c26 = str[off + 26];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, '0', '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime28(char[] str, int off) {
      if (off + 28 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = str[off + 19];
         char c20 = str[off + 20];
         char c21 = str[off + 21];
         char c22 = str[off + 22];
         char c23 = str[off + 23];
         char c24 = str[off + 24];
         char c25 = str[off + 25];
         char c26 = str[off + 26];
         char c27 = str[off + 27];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime28(byte[] str, int off) {
      if (off + 28 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = (char)str[off + 19];
         char c20 = (char)str[off + 20];
         char c21 = (char)str[off + 21];
         char c22 = (char)str[off + 22];
         char c23 = (char)str[off + 23];
         char c24 = (char)str[off + 24];
         char c25 = (char)str[off + 25];
         char c26 = (char)str[off + 26];
         char c27 = (char)str[off + 27];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, '0')
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime29(byte[] str, int off) {
      if (off + 29 > str.length) {
         return null;
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = (char)str[off + 19];
         char c20 = (char)str[off + 20];
         char c21 = (char)str[off + 21];
         char c22 = (char)str[off + 22];
         char c23 = (char)str[off + 23];
         char c24 = (char)str[off + 24];
         char c25 = (char)str[off + 25];
         char c26 = (char)str[off + 26];
         char c27 = (char)str[off + 27];
         char c28 = (char)str[off + 28];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, c28)
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTime29(char[] str, int off) {
      if (off + 29 > str.length) {
         return null;
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = str[off + 19];
         char c20 = str[off + 20];
         char c21 = str[off + 21];
         char c22 = str[off + 22];
         char c23 = str[off + 23];
         char c24 = str[off + 24];
         char c25 = str[off + 25];
         char c26 = str[off + 26];
         char c27 = str[off + 27];
         char c28 = str[off + 28];
         return c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.'
            ? localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, c28)
            : null;
      }
   }

   public static LocalDateTime parseLocalDateTimeX(char[] str, int offset, int len) {
      if (str == null || len == 0) {
         return null;
      } else if (len >= 21 && len <= 29) {
         char c0 = str[offset];
         char c1 = str[offset + 1];
         char c2 = str[offset + 2];
         char c3 = str[offset + 3];
         char c4 = str[offset + 4];
         char c5 = str[offset + 5];
         char c6 = str[offset + 6];
         char c7 = str[offset + 7];
         char c8 = str[offset + 8];
         char c9 = str[offset + 9];
         char c10 = str[offset + 10];
         char c11 = str[offset + 11];
         char c12 = str[offset + 12];
         char c13 = str[offset + 13];
         char c14 = str[offset + 14];
         char c15 = str[offset + 15];
         char c16 = str[offset + 16];
         char c17 = str[offset + 17];
         char c18 = str[offset + 18];
         char c19 = str[offset + 19];
         char c21 = '0';
         char c22 = '0';
         char c23 = '0';
         char c24 = '0';
         char c25 = '0';
         char c26 = '0';
         char c27 = '0';
         char c28 = '0';
         char c20;
         switch (len) {
            case 21:
               c20 = str[offset + 20];
               break;
            case 22:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               break;
            case 23:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               break;
            case 24:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               break;
            case 25:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               c24 = str[offset + 24];
               break;
            case 26:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               c24 = str[offset + 24];
               c25 = str[offset + 25];
               break;
            case 27:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               c24 = str[offset + 24];
               c25 = str[offset + 25];
               c26 = str[offset + 26];
               break;
            case 28:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               c24 = str[offset + 24];
               c25 = str[offset + 25];
               c26 = str[offset + 26];
               c27 = str[offset + 27];
               break;
            default:
               c20 = str[offset + 20];
               c21 = str[offset + 21];
               c22 = str[offset + 22];
               c23 = str[offset + 23];
               c24 = str[offset + 24];
               c25 = str[offset + 25];
               c26 = str[offset + 26];
               c27 = str[offset + 27];
               c28 = str[offset + 28];
         }

         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.') {
            return localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, c28);
         } else if (str[offset + len - 15] == '-'
            && str[offset + len - 12] == '-'
            && (str[offset + len - 9] == ' ' || str[offset + len - 9] == 'T')
            && str[offset + len - 6] == ':'
            && str[offset + len - 3] == ':') {
            int year = TypeUtils.parseInt(str, offset, len - 15);
            int month = TypeUtils.parseInt(str, offset + len - 14, 2);
            int dayOfMonth = TypeUtils.parseInt(str, offset + len - 11, 2);
            int hour = TypeUtils.parseInt(str, offset + len - 8, 2);
            int minute = TypeUtils.parseInt(str, offset + len - 5, 2);
            int second = TypeUtils.parseInt(str, offset + len - 2, 2);
            return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static LocalDateTime parseLocalDateTimeX(byte[] str, int offset, int len) {
      if (str == null || len == 0) {
         return null;
      } else if (len >= 21 && len <= 29) {
         char c0 = (char)str[offset];
         char c1 = (char)str[offset + 1];
         char c2 = (char)str[offset + 2];
         char c3 = (char)str[offset + 3];
         char c4 = (char)str[offset + 4];
         char c5 = (char)str[offset + 5];
         char c6 = (char)str[offset + 6];
         char c7 = (char)str[offset + 7];
         char c8 = (char)str[offset + 8];
         char c9 = (char)str[offset + 9];
         char c10 = (char)str[offset + 10];
         char c11 = (char)str[offset + 11];
         char c12 = (char)str[offset + 12];
         char c13 = (char)str[offset + 13];
         char c14 = (char)str[offset + 14];
         char c15 = (char)str[offset + 15];
         char c16 = (char)str[offset + 16];
         char c17 = (char)str[offset + 17];
         char c18 = (char)str[offset + 18];
         char c19 = (char)str[offset + 19];
         char c21 = '0';
         char c22 = '0';
         char c23 = '0';
         char c24 = '0';
         char c25 = '0';
         char c26 = '0';
         char c27 = '0';
         char c28 = '0';
         char c20;
         switch (len) {
            case 21:
               c20 = (char)str[offset + 20];
               break;
            case 22:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               break;
            case 23:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               break;
            case 24:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               break;
            case 25:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               c24 = (char)str[offset + 24];
               break;
            case 26:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               c24 = (char)str[offset + 24];
               c25 = (char)str[offset + 25];
               break;
            case 27:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               c24 = (char)str[offset + 24];
               c25 = (char)str[offset + 25];
               c26 = (char)str[offset + 26];
               break;
            case 28:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               c24 = (char)str[offset + 24];
               c25 = (char)str[offset + 25];
               c26 = (char)str[offset + 26];
               c27 = (char)str[offset + 27];
               break;
            default:
               c20 = (char)str[offset + 20];
               c21 = (char)str[offset + 21];
               c22 = (char)str[offset + 22];
               c23 = (char)str[offset + 23];
               c24 = (char)str[offset + 24];
               c25 = (char)str[offset + 25];
               c26 = (char)str[offset + 26];
               c27 = (char)str[offset + 27];
               c28 = (char)str[offset + 28];
         }

         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':' && c19 == '.') {
            return localDateTime(c0, c1, c2, c3, c5, c6, c8, c9, c11, c12, c14, c15, c17, c18, c20, c21, c22, c23, c24, c25, c26, c27, c28);
         } else if (str[offset + len - 15] == 45
            && str[offset + len - 12] == 45
            && (str[offset + len - 9] == 32 || str[offset + len - 9] == 84)
            && str[offset + len - 6] == 58
            && str[offset + len - 3] == 58) {
            int year = TypeUtils.parseInt(str, offset, len - 15);
            int month = TypeUtils.parseInt(str, offset + len - 14, 2);
            int dayOfMonth = TypeUtils.parseInt(str, offset + len - 11, 2);
            int hour = TypeUtils.parseInt(str, offset + len - 8, 2);
            int minute = TypeUtils.parseInt(str, offset + len - 5, 2);
            int second = TypeUtils.parseInt(str, offset + len - 2, 2);
            return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static ZonedDateTime parseZonedDateTime16(char[] str, int off, ZoneId defaultZonedId) {
      if (off + 16 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c13 = str[off + 13];
         if (c4 != '-' || c7 != '-' || c10 != '+' && c10 != '-' || c13 != ':') {
            String input = new String(str, off, 16);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9' && c3 >= '0' && c3 <= '9') {
            int year = (c0 - '0') * 1000 + (c1 - '0') * 100 + (c2 - '0') * 10 + (c3 - '0');
            if (c5 >= '0' && c5 <= '9' && c6 >= '0' && c6 <= '9') {
               int month = (c5 - '0') * 10 + (c6 - '0');
               if (c8 >= '0' && c8 <= '9' && c9 >= '0' && c9 <= '9') {
                  int dom = (c8 - '0') * 10 + (c9 - '0');
                  String zoneIdStr = new String(str, off + 10, 6);
                  ZoneId var33 = getZoneId(zoneIdStr, defaultZonedId);
                  LocalDateTime ldt = LocalDateTime.of(LocalDate.of(year, month, dom), LocalTime.MIN);
                  return ZonedDateTime.of(ldt, var33);
               } else {
                  String input = new String(str, off, 16);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, 16);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, 16);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   static ZonedDateTime parseZonedDateTime16(byte[] str, int off, ZoneId defaultZonedId) {
      if (off + 16 > str.length) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c13 = (char)str[off + 13];
         if (c4 != '-' || c7 != '-' || c10 != '+' && c10 != '-' || c13 != ':') {
            String input = new String(str, off, 16);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9' && c3 >= '0' && c3 <= '9') {
            int year = (c0 - '0') * 1000 + (c1 - '0') * 100 + (c2 - '0') * 10 + (c3 - '0');
            if (c5 >= '0' && c5 <= '9' && c6 >= '0' && c6 <= '9') {
               int month = (c5 - '0') * 10 + (c6 - '0');
               if (c8 >= '0' && c8 <= '9' && c9 >= '0' && c9 <= '9') {
                  int dom = (c8 - '0') * 10 + (c9 - '0');
                  String zoneIdStr = new String(str, off + 10, 6);
                  ZoneId var33 = getZoneId(zoneIdStr, defaultZonedId);
                  LocalDateTime ldt = LocalDateTime.of(LocalDate.of(year, month, dom), LocalTime.MIN);
                  return ZonedDateTime.of(ldt, var33);
               } else {
                  String input = new String(str, off, 16);
                  throw new DateTimeParseException("illegal input " + input, input, 0);
               }
            } else {
               String input = new String(str, off, 16);
               throw new DateTimeParseException("illegal input " + input, input, 0);
            }
         } else {
            String input = new String(str, off, 16);
            throw new DateTimeParseException("illegal input " + input, input, 0);
         }
      }
   }

   public static ZonedDateTime parseZonedDateTime(String str) {
      return parseZonedDateTime(str, DEFAULT_ZONE_ID);
   }

   public static ZonedDateTime parseZonedDateTime(String str, ZoneId defaultZoneId) {
      if (str == null) {
         return null;
      } else {
         int len = str.length();
         if (len == 0) {
            return null;
         } else {
            ZonedDateTime zdt;
            if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
               byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
               zdt = parseZonedDateTime(bytes, 0, bytes.length, defaultZoneId);
            } else {
               char[] chars = JDKUtils.getCharArray(str);
               zdt = parseZonedDateTime(chars, 0, chars.length, defaultZoneId);
            }

            if (zdt == null) {
               switch (str) {
                  case "null":
                  case "0":
                  case "0000-00-00":
                     return null;
                  default:
                     throw new DateTimeParseException(str, str, 0);
               }
            } else {
               return zdt;
            }
         }
      }
   }

   public static ZonedDateTime parseZonedDateTime(byte[] str, int off, int len) {
      return parseZonedDateTime(str, off, len, DEFAULT_ZONE_ID);
   }

   public static ZonedDateTime parseZonedDateTime(char[] str, int off, int len) {
      return parseZonedDateTime(str, off, len, DEFAULT_ZONE_ID);
   }

   public static ZonedDateTime parseZonedDateTime(byte[] str, int off, int len, ZoneId defaultZoneId) {
      if (str == null) {
         return null;
      } else if (len == 0) {
         return null;
      } else if (len == 16) {
         return parseZonedDateTime16(str, off, defaultZoneId);
      } else if (len < 19) {
         return null;
      } else {
         String zoneIdStr = null;
         char c0 = (char)str[off];
         char c1 = (char)str[off + 1];
         char c2 = (char)str[off + 2];
         char c3 = (char)str[off + 3];
         char c4 = (char)str[off + 4];
         char c5 = (char)str[off + 5];
         char c6 = (char)str[off + 6];
         char c7 = (char)str[off + 7];
         char c8 = (char)str[off + 8];
         char c9 = (char)str[off + 9];
         char c10 = (char)str[off + 10];
         char c11 = (char)str[off + 11];
         char c12 = (char)str[off + 12];
         char c13 = (char)str[off + 13];
         char c14 = (char)str[off + 14];
         char c15 = (char)str[off + 15];
         char c16 = (char)str[off + 16];
         char c17 = (char)str[off + 17];
         char c18 = (char)str[off + 18];
         char c19 = len == 19 ? 32 : (char)str[off + 19];
         char c21 = '0';
         char c22 = '0';
         char c23 = '0';
         char c24 = '0';
         char c25 = '0';
         char c26 = '0';
         char c27 = '0';
         char c28 = '0';
         char c29 = 0;
         char c20;
         switch (len) {
            case 19:
            case 20:
               c20 = 0;
               break;
            case 21:
               c20 = (char)str[off + 20];
               break;
            case 22:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               break;
            case 23:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               break;
            case 24:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               break;
            case 25:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               break;
            case 26:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               c25 = (char)str[off + 25];
               break;
            case 27:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               c25 = (char)str[off + 25];
               c26 = (char)str[off + 26];
               break;
            case 28:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               c25 = (char)str[off + 25];
               c26 = (char)str[off + 26];
               c27 = (char)str[off + 27];
               break;
            case 29:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               c25 = (char)str[off + 25];
               c26 = (char)str[off + 26];
               c27 = (char)str[off + 27];
               c28 = (char)str[off + 28];
               break;
            default:
               c20 = (char)str[off + 20];
               c21 = (char)str[off + 21];
               c22 = (char)str[off + 22];
               c23 = (char)str[off + 23];
               c24 = (char)str[off + 24];
               c25 = (char)str[off + 25];
               c26 = (char)str[off + 26];
               c27 = (char)str[off + 27];
               c28 = (char)str[off + 28];
               c29 = (char)str[off + 29];
         }

         boolean isTimeZone = false;
         boolean pm = false;
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         char S0;
         char S1;
         char S2;
         char S3;
         char S4;
         char S5;
         char S6;
         char S7;
         char S8;
         int zoneIdBegin;
         if (c4 != '-'
            || c7 != '-'
            || c10 != ' ' && c10 != 'T'
            || c13 != ':'
            || c16 != ':'
            || c19 != '[' && c19 != 'Z' && c19 != '+' && c19 != '-' && c19 != ' ') {
            if (c4 == '-' && c7 == '-' && c10 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':' && len == 20) {
               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c12;
               h1 = c13;
               i0 = c15;
               i1 = c16;
               s0 = c18;
               s1 = c19;
               S0 = '0';
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 20;
            } else if (len == 20 && c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':') {
               y0 = c7;
               y1 = c8;
               y2 = c9;
               y3 = c10;
               int month = month(c3, c4, c5);
               if (month > 0) {
                  m0 = (char)(48 + month / 10);
                  m1 = (char)(48 + month % 10);
               } else {
                  m0 = '0';
                  m1 = '0';
               }

               d0 = c0;
               d1 = c1;
               h0 = c12;
               h1 = c13;
               i0 = c15;
               i1 = c16;
               s0 = c18;
               s1 = c19;
               S0 = '0';
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 20;
            } else if (c4 != '-'
               || c7 != '-'
               || c10 != ' ' && c10 != 'T'
               || c13 != ':'
               || c16 != ':'
               || c19 != '.'
               || len != 21 && c21 != '[' && c21 != '+' && c21 != '-' && c21 != 'Z') {
               if (c4 != '-'
                  || c7 != '-'
                  || c10 != ' ' && c10 != 'T'
                  || c13 != ':'
                  || c16 != ':'
                  || c19 != '.'
                  || len != 22 && c22 != '[' && c22 != '+' && c22 != '-' && c22 != 'Z') {
                  if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == 'Z' && c17 == '[' && c21 == ']' && len == 22) {
                     y0 = c0;
                     y1 = c1;
                     y2 = c2;
                     y3 = c3;
                     m0 = c5;
                     m1 = c6;
                     d0 = c8;
                     d1 = c9;
                     h0 = c11;
                     h1 = c12;
                     i0 = c14;
                     i1 = c15;
                     s0 = '0';
                     s1 = '0';
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     isTimeZone = true;
                     zoneIdBegin = 17;
                  } else if (len == 22
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ' '
                     && c13 == ':'
                     && c16 == ':'
                     && c19 == ' '
                     && (c20 == 'A' || c20 == 'P')
                     && c21 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = '0';
                     h1 = c12;
                     pm = c20 == 'P';
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 22;
                  } else if (len == 22
                     && c2 == '/'
                     && c5 == '/'
                     && c10 == ' '
                     && c13 == ':'
                     && c16 == ':'
                     && c19 == ' '
                     && (c20 == 'A' || c20 == 'P')
                     && c21 == 'M') {
                     m0 = c0;
                     m1 = c1;
                     d0 = c3;
                     d1 = c4;
                     y0 = c6;
                     y1 = c7;
                     y2 = c8;
                     y3 = c9;
                     h0 = c11;
                     h1 = c12;
                     pm = c20 == 'P';
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 22;
                  } else if (len == 23
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = c12;
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 23
                     && c3 == ' '
                     && c6 == ','
                     && c7 == ' '
                     && c12 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c8;
                     y1 = c9;
                     y2 = c10;
                     y3 = c11;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = c4;
                     d1 = c5;
                     h0 = '0';
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 23
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ','
                     && c12 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = '0';
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 24
                     && c3 == ' '
                     && c6 == ','
                     && c7 == ' '
                     && c12 == ' '
                     && c15 == ':'
                     && c18 == ':'
                     && c21 == ' '
                     && (c22 == 'A' || c22 == 'P')
                     && c23 == 'M') {
                     y0 = c8;
                     y1 = c9;
                     y2 = c10;
                     y3 = c11;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = c4;
                     d1 = c5;
                     h0 = c13;
                     h1 = c14;
                     pm = c22 == 'P';
                     i0 = c16;
                     i1 = c17;
                     s0 = c19;
                     s1 = c20;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 24;
                  } else if (len == 24
                     && c3 == ' '
                     && c6 == ','
                     && c7 == ' '
                     && c12 == ','
                     && c13 == ' '
                     && c15 == ':'
                     && c18 == ':'
                     && c21 == ' '
                     && (c22 == 'A' || c22 == 'P')
                     && c23 == 'M') {
                     y0 = c8;
                     y1 = c9;
                     y2 = c10;
                     y3 = c11;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = c4;
                     d1 = c5;
                     h0 = '0';
                     h1 = c14;
                     pm = c22 == 'P';
                     i0 = c16;
                     i1 = c17;
                     s0 = c19;
                     s1 = c20;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 24;
                  } else if (len == 24
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ','
                     && c12 == ' '
                     && c15 == ':'
                     && c18 == ':'
                     && c21 == ' '
                     && (c22 == 'A' || c22 == 'P')
                     && c23 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = c13;
                     h1 = c14;
                     pm = c22 == 'P';
                     i0 = c16;
                     i1 = c17;
                     s0 = c19;
                     s1 = c20;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 24;
                  } else if (c4 != '-'
                     || c7 != '-'
                     || c10 != ' ' && c10 != 'T'
                     || c13 != ':'
                     || c16 != ':'
                     || c19 != '.'
                     || len != 23 && c23 != '[' && c23 != '|' && c23 != '+' && c23 != '-' && c23 != 'Z') {
                     if (c4 != '-'
                        || c7 != '-'
                        || c10 != ' ' && c10 != 'T'
                        || c13 != ':'
                        || c16 != ':'
                        || c19 != '.'
                        || len != 24 && c24 != '[' && c24 != '|' && c24 != '+' && c24 != '-' && c24 != 'Z') {
                        if (c4 != '-'
                           || c7 != '-'
                           || c10 != ' ' && c10 != 'T'
                           || c13 != ':'
                           || c16 != ':'
                           || c19 != '.'
                           || len != 25 && c25 != '[' && c25 != '|' && c25 != '+' && c25 != '-' && c25 != 'Z') {
                           if (len == 25
                              && c3 == ' '
                              && c6 == ','
                              && c7 == ' '
                              && c12 == ','
                              && c13 == ' '
                              && c16 == ':'
                              && c19 == ':'
                              && c22 == ' '
                              && (c23 == 'A' || c23 == 'P')
                              && c24 == 'M') {
                              y0 = c8;
                              y1 = c9;
                              y2 = c10;
                              y3 = c11;
                              int month = month(c0, c1, c2);
                              if (month > 0) {
                                 m0 = (char)(48 + month / 10);
                                 m1 = (char)(48 + month % 10);
                              } else {
                                 m0 = '0';
                                 m1 = '0';
                              }

                              d0 = c4;
                              d1 = c5;
                              h0 = c14;
                              h1 = c15;
                              pm = c23 == 'P';
                              i0 = c17;
                              i1 = c18;
                              s0 = c20;
                              s1 = c21;
                              S0 = '0';
                              S1 = '0';
                              S2 = '0';
                              S3 = '0';
                              S4 = '0';
                              S5 = '0';
                              S6 = '0';
                              S7 = '0';
                              S8 = '0';
                              zoneIdBegin = 25;
                           } else if (c4 != '-'
                              || c7 != '-'
                              || c10 != ' ' && c10 != 'T'
                              || c13 != ':'
                              || c16 != ':'
                              || c19 != '.'
                              || len != 26 && c26 != '[' && c26 != '|' && c26 != '+' && c26 != '-' && c26 != 'Z') {
                              if (c4 != '-'
                                 || c7 != '-'
                                 || c10 != ' ' && c10 != 'T'
                                 || c13 != ':'
                                 || c16 != ':'
                                 || c19 != '.'
                                 || len != 27 && c27 != '[' && c27 != '|' && c27 != '+' && c27 != '-' && c27 != 'Z') {
                                 if (c4 != '-'
                                    || c7 != '-'
                                    || c10 != ' ' && c10 != 'T'
                                    || c13 != ':'
                                    || c16 != ':'
                                    || c19 != '.'
                                    || len != 28 && c28 != '[' && c28 != '|' && c28 != '+' && c28 != '-' && c28 != 'Z') {
                                    if (len == 28 && c3 == ' ' && c7 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':' && c19 == ' ' && c23 == ' ') {
                                       int month = month(c4, c5, c6);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = c8;
                                       d1 = c9;
                                       h0 = c11;
                                       h1 = c12;
                                       i0 = c14;
                                       i1 = c15;
                                       s0 = c17;
                                       s1 = c18;
                                       y0 = c24;
                                       y1 = c25;
                                       y2 = c26;
                                       y3 = c27;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 19;
                                       zoneIdStr = new String(str, off + 20, 3);
                                    } else if (len == 28
                                       && c3 == ','
                                       && c4 == ' '
                                       && c6 == ' '
                                       && c10 == ' '
                                       && c15 == ' '
                                       && c18 == ':'
                                       && c21 == ':'
                                       && c24 == ' ') {
                                       y0 = c11;
                                       y1 = c12;
                                       y2 = c13;
                                       y3 = c14;
                                       int month = month(c7, c8, c9);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = '0';
                                       d1 = c5;
                                       h0 = c16;
                                       h1 = c17;
                                       i0 = c19;
                                       i1 = c20;
                                       s0 = c22;
                                       s1 = c23;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 24;
                                       isTimeZone = true;
                                    } else if (len == 29
                                       && c3 == ','
                                       && c4 == ' '
                                       && c7 == ' '
                                       && c11 == ' '
                                       && c16 == ' '
                                       && c19 == ':'
                                       && c22 == ':'
                                       && c25 == ' ') {
                                       y0 = c12;
                                       y1 = c13;
                                       y2 = c14;
                                       y3 = c15;
                                       int month = month(c8, c9, c10);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = c5;
                                       d1 = c6;
                                       h0 = c17;
                                       h1 = c18;
                                       i0 = c20;
                                       i1 = c21;
                                       s0 = c23;
                                       s1 = c24;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 25;
                                       isTimeZone = true;
                                    } else if (c4 != '-'
                                       || c7 != '-'
                                       || c10 != ' ' && c10 != 'T'
                                       || c13 != ':'
                                       || c16 != ':'
                                       || c19 != '.'
                                       || len != 29 && c29 != '[' && c29 != '|' && c29 != '+' && c29 != '-' && c29 != 'Z') {
                                       if (len != 22 || c17 != '+' && c17 != '-') {
                                          if ((
                                                len != 32
                                                   || c6 != ','
                                                   || c7 != ' '
                                                   || c10 != '-'
                                                   || c14 != '-'
                                                   || c19 != ' '
                                                   || c22 != ':'
                                                   || c25 != ':'
                                                   || str[off + 28] != 32
                                             )
                                             && (
                                                len != 33
                                                   || c7 != ','
                                                   || c8 != ' '
                                                   || c11 != '-'
                                                   || c15 != '-'
                                                   || c20 != ' '
                                                   || c23 != ':'
                                                   || c26 != ':'
                                                   || str[off + 29] != 32
                                             )
                                             && (
                                                len != 34
                                                   || c8 != ','
                                                   || c9 != ' '
                                                   || c12 != '-'
                                                   || c16 != '-'
                                                   || c21 != ' '
                                                   || c24 != ':'
                                                   || c27 != ':'
                                                   || str[off + 30] != 32
                                             )
                                             && (
                                                len != 35
                                                   || c9 != ','
                                                   || c10 != ' '
                                                   || c13 != '-'
                                                   || c17 != '-'
                                                   || c22 != ' '
                                                   || c25 != ':'
                                                   || c28 != ':'
                                                   || str[off + 31] != 32
                                             )) {
                                             if (len == 34) {
                                                DateTimeFormatter formatter = DATE_TIME_FORMATTER_34;
                                                if (formatter == null) {
                                                   formatter = DATE_TIME_FORMATTER_34 = DateTimeFormatter.ofPattern(
                                                      "EEE MMM dd HH:mm:ss O yyyy", Locale.ENGLISH
                                                   );
                                                }

                                                return ZonedDateTime.parse(new String(str, off, len), formatter);
                                             }

                                             if (len == 31 && str[off + 3] == 44) {
                                                DateTimeFormatter formatter = DATE_TIME_FORMATTER_RFC_2822;
                                                if (formatter == null) {
                                                   formatter = DATE_TIME_FORMATTER_RFC_2822 = DateTimeFormatter.ofPattern(
                                                      "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH
                                                   );
                                                }

                                                return ZonedDateTime.parse(new String(str, off, len), formatter);
                                             }

                                             return null;
                                          }

                                          return parseZonedDateTimeCookie(new String(str, off, len));
                                       }

                                       y0 = c0;
                                       y1 = c1;
                                       y2 = c2;
                                       y3 = c3;
                                       m0 = c4;
                                       m1 = c5;
                                       d0 = c6;
                                       d1 = c7;
                                       h0 = c8;
                                       h1 = c9;
                                       i0 = c10;
                                       i1 = c11;
                                       s0 = c12;
                                       s1 = c13;
                                       S0 = c14;
                                       S1 = c15;
                                       S2 = c16;
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 17;
                                    } else {
                                       y0 = c0;
                                       y1 = c1;
                                       y2 = c2;
                                       y3 = c3;
                                       m0 = c5;
                                       m1 = c6;
                                       d0 = c8;
                                       d1 = c9;
                                       h0 = c11;
                                       h1 = c12;
                                       i0 = c14;
                                       i1 = c15;
                                       s0 = c17;
                                       s1 = c18;
                                       S0 = c20;
                                       S1 = c21;
                                       S2 = c22;
                                       S3 = c23;
                                       S4 = c24;
                                       S5 = c25;
                                       S6 = c26;
                                       S7 = c27;
                                       S8 = c28;
                                       zoneIdBegin = 29;
                                       isTimeZone = c29 == '|';
                                    }
                                 } else {
                                    y0 = c0;
                                    y1 = c1;
                                    y2 = c2;
                                    y3 = c3;
                                    m0 = c5;
                                    m1 = c6;
                                    d0 = c8;
                                    d1 = c9;
                                    h0 = c11;
                                    h1 = c12;
                                    i0 = c14;
                                    i1 = c15;
                                    s0 = c17;
                                    s1 = c18;
                                    S0 = c20;
                                    S1 = c21;
                                    S2 = c22;
                                    S3 = c23;
                                    S4 = c24;
                                    S5 = c25;
                                    S6 = c26;
                                    S7 = c27;
                                    S8 = '0';
                                    zoneIdBegin = 28;
                                    isTimeZone = c28 == '|';
                                 }
                              } else {
                                 y0 = c0;
                                 y1 = c1;
                                 y2 = c2;
                                 y3 = c3;
                                 m0 = c5;
                                 m1 = c6;
                                 d0 = c8;
                                 d1 = c9;
                                 h0 = c11;
                                 h1 = c12;
                                 i0 = c14;
                                 i1 = c15;
                                 s0 = c17;
                                 s1 = c18;
                                 S0 = c20;
                                 S1 = c21;
                                 S2 = c22;
                                 if (c23 == ' ') {
                                    S3 = '0';
                                    S4 = '0';
                                    S5 = '0';
                                    S6 = '0';
                                    S7 = '0';
                                    S8 = '0';
                                    zoneIdBegin = 23;
                                 } else {
                                    S3 = c23;
                                    S4 = c24;
                                    S5 = c25;
                                    S6 = c26;
                                    S7 = '0';
                                    S8 = '0';
                                    zoneIdBegin = 27;
                                    isTimeZone = c27 == '|';
                                 }
                              }
                           } else {
                              y0 = c0;
                              y1 = c1;
                              y2 = c2;
                              y3 = c3;
                              m0 = c5;
                              m1 = c6;
                              d0 = c8;
                              d1 = c9;
                              h0 = c11;
                              h1 = c12;
                              i0 = c14;
                              i1 = c15;
                              s0 = c17;
                              s1 = c18;
                              S0 = c20;
                              S1 = c21;
                              S2 = c22;
                              S3 = c23;
                              S4 = c24;
                              S5 = c25;
                              S6 = '0';
                              S7 = '0';
                              S8 = '0';
                              zoneIdBegin = 26;
                              isTimeZone = c26 == '|';
                           }
                        } else {
                           y0 = c0;
                           y1 = c1;
                           y2 = c2;
                           y3 = c3;
                           m0 = c5;
                           m1 = c6;
                           d0 = c8;
                           d1 = c9;
                           h0 = c11;
                           h1 = c12;
                           i0 = c14;
                           i1 = c15;
                           s0 = c17;
                           s1 = c18;
                           S0 = c20;
                           S1 = c21;
                           S2 = c22;
                           S3 = c23;
                           S4 = c24;
                           S5 = '0';
                           S6 = '0';
                           S7 = '0';
                           S8 = '0';
                           zoneIdBegin = 25;
                           isTimeZone = c25 == '|';
                        }
                     } else {
                        y0 = c0;
                        y1 = c1;
                        y2 = c2;
                        y3 = c3;
                        m0 = c5;
                        m1 = c6;
                        d0 = c8;
                        d1 = c9;
                        h0 = c11;
                        h1 = c12;
                        i0 = c14;
                        i1 = c15;
                        s0 = c17;
                        s1 = c18;
                        S0 = c20;
                        S1 = c21;
                        S2 = c22;
                        S3 = c23;
                        S4 = '0';
                        S5 = '0';
                        S6 = '0';
                        S7 = '0';
                        S8 = '0';
                        zoneIdBegin = 24;
                        isTimeZone = c24 == '|';
                     }
                  } else {
                     y0 = c0;
                     y1 = c1;
                     y2 = c2;
                     y3 = c3;
                     m0 = c5;
                     m1 = c6;
                     d0 = c8;
                     d1 = c9;
                     h0 = c11;
                     h1 = c12;
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = c20;
                     S1 = c21;
                     S2 = c22;
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                     isTimeZone = c23 == '|';
                  }
               } else {
                  y0 = c0;
                  y1 = c1;
                  y2 = c2;
                  y3 = c3;
                  m0 = c5;
                  m1 = c6;
                  d0 = c8;
                  d1 = c9;
                  h0 = c11;
                  h1 = c12;
                  i0 = c14;
                  i1 = c15;
                  s0 = c17;
                  s1 = c18;
                  S0 = c20;
                  S1 = c21;
                  S2 = '0';
                  S3 = '0';
                  S4 = '0';
                  S5 = '0';
                  S6 = '0';
                  S7 = '0';
                  S8 = '0';
                  zoneIdBegin = 22;
                  isTimeZone = c22 == '|';
               }
            } else {
               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               S0 = c20;
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 21;
               isTimeZone = c21 == '|';
            }
         } else {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
            S0 = '0';
            S1 = '0';
            S2 = '0';
            S3 = '0';
            S4 = '0';
            S5 = '0';
            S6 = '0';
            S7 = '0';
            S8 = '0';
            zoneIdBegin = 19;
         }

         if (pm && h0 == '1' && h1 == '2') {
            pm = false;
         }

         if (pm) {
            int hourValue = hourAfterNoon(h0, h1);
            h0 = (char)(hourValue >> 16);
            h1 = (char)((short)hourValue);
         }

         LocalDateTime ldt = localDateTime(y0, y1, y2, y3, m0, m1, d0, d1, h0, h1, i0, i1, s0, s1, S0, S1, S2, S3, S4, S5, S6, S7, S8);
         if (ldt == null) {
            return null;
         } else {
            ZoneId zoneId;
            if (isTimeZone) {
               String tzStr = new String(str, zoneIdBegin, len - zoneIdBegin);
               switch (tzStr) {
                  case "UTC":
                  case "[UTC]":
                     zoneId = ZoneOffset.UTC;
                     break;
                  default:
                     TimeZone timeZone = TimeZone.getTimeZone(tzStr);
                     zoneId = timeZone.toZoneId();
               }
            } else if (zoneIdBegin == len) {
               zoneId = defaultZoneId;
            } else {
               char first = (char)str[off + zoneIdBegin];
               if (first == 'Z') {
                  zoneId = ZoneOffset.UTC;
               } else {
                  if (zoneIdStr == null) {
                     if (first == '+' || first == '-') {
                        zoneIdStr = new String(str, off + zoneIdBegin, len - zoneIdBegin);
                     } else if (first == ' ') {
                        zoneIdStr = new String(str, off + zoneIdBegin + 1, len - zoneIdBegin - 1);
                     } else if (zoneIdBegin < len) {
                        zoneIdStr = new String(str, off + zoneIdBegin + 1, len - zoneIdBegin - 2);
                     }
                  }

                  zoneId = getZoneId(zoneIdStr, defaultZoneId);
               }
            }

            if (zoneId == null) {
               zoneId = defaultZoneId;
            }

            if (zoneId == null) {
               zoneId = DEFAULT_ZONE_ID;
            }

            return ZonedDateTime.ofLocal(ldt, zoneId, null);
         }
      }
   }

   public static ZonedDateTime parseZonedDateTime(char[] str, int off, int len, ZoneId defaultZoneId) {
      if (str == null) {
         return null;
      } else if (len == 0) {
         return null;
      } else if (len == 16) {
         return parseZonedDateTime16(str, off, defaultZoneId);
      } else if (len < 19) {
         String input = new String(str, off, str.length - off);
         throw new DateTimeParseException("illegal input " + input, input, 0);
      } else {
         String zoneIdStr = null;
         char c0 = str[off];
         char c1 = str[off + 1];
         char c2 = str[off + 2];
         char c3 = str[off + 3];
         char c4 = str[off + 4];
         char c5 = str[off + 5];
         char c6 = str[off + 6];
         char c7 = str[off + 7];
         char c8 = str[off + 8];
         char c9 = str[off + 9];
         char c10 = str[off + 10];
         char c11 = str[off + 11];
         char c12 = str[off + 12];
         char c13 = str[off + 13];
         char c14 = str[off + 14];
         char c15 = str[off + 15];
         char c16 = str[off + 16];
         char c17 = str[off + 17];
         char c18 = str[off + 18];
         char c19 = len == 19 ? 32 : str[off + 19];
         char c21 = '0';
         char c22 = '0';
         char c23 = '0';
         char c24 = '0';
         char c25 = '0';
         char c26 = '0';
         char c27 = '0';
         char c28 = '0';
         char c29 = 0;
         char c20;
         switch (len) {
            case 19:
            case 20:
               c20 = 0;
               break;
            case 21:
               c20 = str[off + 20];
               break;
            case 22:
               c20 = str[off + 20];
               c21 = str[off + 21];
               break;
            case 23:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               break;
            case 24:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               break;
            case 25:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               break;
            case 26:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               c25 = str[off + 25];
               break;
            case 27:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               c25 = str[off + 25];
               c26 = str[off + 26];
               break;
            case 28:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               c25 = str[off + 25];
               c26 = str[off + 26];
               c27 = str[off + 27];
               break;
            case 29:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               c25 = str[off + 25];
               c26 = str[off + 26];
               c27 = str[off + 27];
               c28 = str[off + 28];
               break;
            default:
               c20 = str[off + 20];
               c21 = str[off + 21];
               c22 = str[off + 22];
               c23 = str[off + 23];
               c24 = str[off + 24];
               c25 = str[off + 25];
               c26 = str[off + 26];
               c27 = str[off + 27];
               c28 = str[off + 28];
               c29 = str[off + 29];
         }

         boolean isTimeZone = false;
         boolean pm = false;
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         char S0;
         char S1;
         char S2;
         char S3;
         char S4;
         char S5;
         char S6;
         char S7;
         char S8;
         int zoneIdBegin;
         if (c4 != '-'
            || c7 != '-'
            || c10 != ' ' && c10 != 'T'
            || c13 != ':'
            || c16 != ':'
            || c19 != '[' && c19 != 'Z' && c19 != '+' && c19 != '-' && c19 != ' ') {
            if (c4 == '-' && c7 == '-' && c10 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':' && len == 20) {
               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c12;
               h1 = c13;
               i0 = c15;
               i1 = c16;
               s0 = c18;
               s1 = c19;
               S0 = '0';
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 20;
            } else if (len == 20 && c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c17 == ':') {
               y0 = c7;
               y1 = c8;
               y2 = c9;
               y3 = c10;
               int month = month(c3, c4, c5);
               if (month > 0) {
                  m0 = (char)(48 + month / 10);
                  m1 = (char)(48 + month % 10);
               } else {
                  m0 = '0';
                  m1 = '0';
               }

               d0 = c0;
               d1 = c1;
               h0 = c12;
               h1 = c13;
               i0 = c15;
               i1 = c16;
               s0 = c18;
               s1 = c19;
               S0 = '0';
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 20;
            } else if (c4 != '-'
               || c7 != '-'
               || c10 != ' ' && c10 != 'T'
               || c13 != ':'
               || c16 != ':'
               || c19 != '.'
               || len != 21 && c21 != '[' && c21 != '+' && c21 != '-' && c21 != 'Z') {
               if (c4 != '-'
                  || c7 != '-'
                  || c10 != ' ' && c10 != 'T'
                  || c13 != ':'
                  || c16 != ':'
                  || c19 != '.'
                  || len != 22 && c22 != '[' && c22 != '+' && c22 != '-' && c22 != 'Z') {
                  if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == 'Z' && c17 == '[' && c21 == ']' && len == 22) {
                     y0 = c0;
                     y1 = c1;
                     y2 = c2;
                     y3 = c3;
                     m0 = c5;
                     m1 = c6;
                     d0 = c8;
                     d1 = c9;
                     h0 = c11;
                     h1 = c12;
                     i0 = c14;
                     i1 = c15;
                     s0 = '0';
                     s1 = '0';
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     isTimeZone = true;
                     zoneIdBegin = 17;
                  } else if (len == 22
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ' '
                     && c13 == ':'
                     && c16 == ':'
                     && c19 == ' '
                     && (c20 == 'A' || c20 == 'P')
                     && c21 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = '0';
                     h1 = c12;
                     pm = c20 == 'P';
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 22;
                  } else if (len == 22
                     && c2 == '/'
                     && c5 == '/'
                     && c10 == ' '
                     && c13 == ':'
                     && c16 == ':'
                     && c19 == ' '
                     && (c20 == 'A' || c20 == 'P')
                     && c21 == 'M') {
                     m0 = c0;
                     m1 = c1;
                     d0 = c3;
                     d1 = c4;
                     y0 = c6;
                     y1 = c7;
                     y2 = c8;
                     y3 = c9;
                     h0 = c11;
                     h1 = c12;
                     pm = c20 == 'P';
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 22;
                  } else if (len == 23
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = c12;
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 23
                     && c3 == ' '
                     && c6 == ','
                     && c7 == ' '
                     && c12 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c8;
                     y1 = c9;
                     y2 = c10;
                     y3 = c11;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = c4;
                     d1 = c5;
                     h0 = '0';
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 23
                     && c3 == ' '
                     && c5 == ','
                     && c6 == ' '
                     && c11 == ','
                     && c12 == ' '
                     && c14 == ':'
                     && c17 == ':'
                     && c20 == ' '
                     && (c21 == 'A' || c21 == 'P')
                     && c22 == 'M') {
                     y0 = c7;
                     y1 = c8;
                     y2 = c9;
                     y3 = c10;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = '0';
                     d1 = c4;
                     h0 = '0';
                     h1 = c13;
                     pm = c21 == 'P';
                     i0 = c15;
                     i1 = c16;
                     s0 = c18;
                     s1 = c19;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                  } else if (len == 24
                     && c3 == ' '
                     && c6 == ','
                     && c7 == ' '
                     && c12 == ' '
                     && c15 == ':'
                     && c18 == ':'
                     && c21 == ' '
                     && (c22 == 'A' || c22 == 'P')
                     && c23 == 'M') {
                     y0 = c8;
                     y1 = c9;
                     y2 = c10;
                     y3 = c11;
                     int month = month(c0, c1, c2);
                     if (month > 0) {
                        m0 = (char)(48 + month / 10);
                        m1 = (char)(48 + month % 10);
                     } else {
                        m0 = '0';
                        m1 = '0';
                     }

                     d0 = c4;
                     d1 = c5;
                     h0 = c13;
                     h1 = c14;
                     pm = c22 == 'P';
                     i0 = c16;
                     i1 = c17;
                     s0 = c19;
                     s1 = c20;
                     S0 = '0';
                     S1 = '0';
                     S2 = '0';
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 24;
                  } else if (c4 != '-'
                     || c7 != '-'
                     || c10 != ' ' && c10 != 'T'
                     || c13 != ':'
                     || c16 != ':'
                     || c19 != '.'
                     || len != 23 && c23 != '[' && c23 != '|' && c23 != '+' && c23 != '-' && c23 != 'Z') {
                     if (c4 != '-'
                        || c7 != '-'
                        || c10 != ' ' && c10 != 'T'
                        || c13 != ':'
                        || c16 != ':'
                        || c19 != '.'
                        || len != 24 && c24 != '[' && c24 != '|' && c24 != '+' && c24 != '-' && c24 != 'Z') {
                        if (len == 24
                           && c3 == ' '
                           && c6 == ','
                           && c7 == ' '
                           && c12 == ','
                           && c13 == ' '
                           && c15 == ':'
                           && c18 == ':'
                           && c21 == ' '
                           && (c22 == 'A' || c22 == 'P')
                           && c23 == 'M') {
                           y0 = c8;
                           y1 = c9;
                           y2 = c10;
                           y3 = c11;
                           int month = month(c0, c1, c2);
                           if (month > 0) {
                              m0 = (char)(48 + month / 10);
                              m1 = (char)(48 + month % 10);
                           } else {
                              m0 = '0';
                              m1 = '0';
                           }

                           d0 = c4;
                           d1 = c5;
                           h0 = '0';
                           h1 = c14;
                           pm = c22 == 'P';
                           i0 = c16;
                           i1 = c17;
                           s0 = c19;
                           s1 = c20;
                           S0 = '0';
                           S1 = '0';
                           S2 = '0';
                           S3 = '0';
                           S4 = '0';
                           S5 = '0';
                           S6 = '0';
                           S7 = '0';
                           S8 = '0';
                           zoneIdBegin = 24;
                        } else if (len == 24
                           && c3 == ' '
                           && c5 == ','
                           && c6 == ' '
                           && c11 == ','
                           && c12 == ' '
                           && c15 == ':'
                           && c18 == ':'
                           && c21 == ' '
                           && (c22 == 'A' || c22 == 'P')
                           && c23 == 'M') {
                           y0 = c7;
                           y1 = c8;
                           y2 = c9;
                           y3 = c10;
                           int month = month(c0, c1, c2);
                           if (month > 0) {
                              m0 = (char)(48 + month / 10);
                              m1 = (char)(48 + month % 10);
                           } else {
                              m0 = '0';
                              m1 = '0';
                           }

                           d0 = '0';
                           d1 = c4;
                           h0 = c13;
                           h1 = c14;
                           pm = c22 == 'P';
                           i0 = c16;
                           i1 = c17;
                           s0 = c19;
                           s1 = c20;
                           S0 = '0';
                           S1 = '0';
                           S2 = '0';
                           S3 = '0';
                           S4 = '0';
                           S5 = '0';
                           S6 = '0';
                           S7 = '0';
                           S8 = '0';
                           zoneIdBegin = 24;
                        } else if (c4 != '-'
                           || c7 != '-'
                           || c10 != ' ' && c10 != 'T'
                           || c13 != ':'
                           || c16 != ':'
                           || c19 != '.'
                           || len != 25 && c25 != '[' && c25 != '|' && c25 != '+' && c25 != '-' && c25 != 'Z') {
                           if (len == 25
                              && c3 == ' '
                              && c6 == ','
                              && c7 == ' '
                              && c12 == ','
                              && c13 == ' '
                              && c16 == ':'
                              && c19 == ':'
                              && c22 == ' '
                              && (c23 == 'A' || c23 == 'P')
                              && c24 == 'M') {
                              y0 = c8;
                              y1 = c9;
                              y2 = c10;
                              y3 = c11;
                              int month = month(c0, c1, c2);
                              if (month > 0) {
                                 m0 = (char)(48 + month / 10);
                                 m1 = (char)(48 + month % 10);
                              } else {
                                 m0 = '0';
                                 m1 = '0';
                              }

                              d0 = c4;
                              d1 = c5;
                              h0 = c14;
                              h1 = c15;
                              pm = c23 == 'P';
                              i0 = c17;
                              i1 = c18;
                              s0 = c20;
                              s1 = c21;
                              S0 = '0';
                              S1 = '0';
                              S2 = '0';
                              S3 = '0';
                              S4 = '0';
                              S5 = '0';
                              S6 = '0';
                              S7 = '0';
                              S8 = '0';
                              zoneIdBegin = 25;
                           } else if (c4 != '-'
                              || c7 != '-'
                              || c10 != ' ' && c10 != 'T'
                              || c13 != ':'
                              || c16 != ':'
                              || c19 != '.'
                              || len != 26 && c26 != '[' && c26 != '|' && c26 != '+' && c26 != '-' && c26 != 'Z') {
                              if (c4 != '-'
                                 || c7 != '-'
                                 || c10 != ' ' && c10 != 'T'
                                 || c13 != ':'
                                 || c16 != ':'
                                 || c19 != '.'
                                 || len != 27 && c27 != '[' && c27 != '|' && c27 != '+' && c27 != '-' && c27 != 'Z') {
                                 if (c4 != '-'
                                    || c7 != '-'
                                    || c10 != ' ' && c10 != 'T'
                                    || c13 != ':'
                                    || c16 != ':'
                                    || c19 != '.'
                                    || len != 28 && c28 != '[' && c28 != '|' && c28 != '+' && c28 != '-' && c28 != 'Z') {
                                    if (len == 28 && c3 == ' ' && c7 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':' && c19 == ' ' && c23 == ' ') {
                                       int month = month(c4, c5, c6);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = c8;
                                       d1 = c9;
                                       h0 = c11;
                                       h1 = c12;
                                       i0 = c14;
                                       i1 = c15;
                                       s0 = c17;
                                       s1 = c18;
                                       y0 = c24;
                                       y1 = c25;
                                       y2 = c26;
                                       y3 = c27;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 19;
                                       zoneIdStr = new String(str, off + 20, 3);
                                    } else if (len == 28
                                       && c3 == ','
                                       && c4 == ' '
                                       && c6 == ' '
                                       && c10 == ' '
                                       && c15 == ' '
                                       && c18 == ':'
                                       && c21 == ':'
                                       && c24 == ' ') {
                                       y0 = c11;
                                       y1 = c12;
                                       y2 = c13;
                                       y3 = c14;
                                       int month = month(c7, c8, c9);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = '0';
                                       d1 = c5;
                                       h0 = c16;
                                       h1 = c17;
                                       i0 = c19;
                                       i1 = c20;
                                       s0 = c22;
                                       s1 = c23;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 24;
                                       isTimeZone = true;
                                    } else if (len == 29
                                       && c3 == ','
                                       && c4 == ' '
                                       && c7 == ' '
                                       && c11 == ' '
                                       && c16 == ' '
                                       && c19 == ':'
                                       && c22 == ':'
                                       && c25 == ' ') {
                                       y0 = c12;
                                       y1 = c13;
                                       y2 = c14;
                                       y3 = c15;
                                       int month = month(c8, c9, c10);
                                       if (month > 0) {
                                          m0 = (char)(48 + month / 10);
                                          m1 = (char)(48 + month % 10);
                                       } else {
                                          m0 = '0';
                                          m1 = '0';
                                       }

                                       d0 = c5;
                                       d1 = c6;
                                       h0 = c17;
                                       h1 = c18;
                                       i0 = c20;
                                       i1 = c21;
                                       s0 = c23;
                                       s1 = c24;
                                       S0 = '0';
                                       S1 = '0';
                                       S2 = '0';
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 25;
                                       isTimeZone = true;
                                    } else if (c4 != '-'
                                       || c7 != '-'
                                       || c10 != ' ' && c10 != 'T'
                                       || c13 != ':'
                                       || c16 != ':'
                                       || c19 != '.'
                                       || len != 29 && c29 != '[' && c29 != '|' && c29 != '+' && c29 != '-' && c29 != 'Z') {
                                       if (len != 22 || c17 != '+' && c17 != '-') {
                                          if ((
                                                len != 32
                                                   || c6 != ','
                                                   || c7 != ' '
                                                   || c10 != '-'
                                                   || c14 != '-'
                                                   || c19 != ' '
                                                   || c22 != ':'
                                                   || c25 != ':'
                                                   || str[off + 28] != ' '
                                             )
                                             && (
                                                len != 33
                                                   || c7 != ','
                                                   || c8 != ' '
                                                   || c11 != '-'
                                                   || c15 != '-'
                                                   || c20 != ' '
                                                   || c23 != ':'
                                                   || c26 != ':'
                                                   || str[off + 29] != ' '
                                             )
                                             && (
                                                len != 34
                                                   || c8 != ','
                                                   || c9 != ' '
                                                   || c12 != '-'
                                                   || c16 != '-'
                                                   || c21 != ' '
                                                   || c24 != ':'
                                                   || c27 != ':'
                                                   || str[off + 30] != ' '
                                             )
                                             && (
                                                len != 35
                                                   || c9 != ','
                                                   || c10 != ' '
                                                   || c13 != '-'
                                                   || c17 != '-'
                                                   || c22 != ' '
                                                   || c25 != ':'
                                                   || c28 != ':'
                                                   || str[off + 31] != ' '
                                             )) {
                                             if (len == 34) {
                                                DateTimeFormatter formatter = DATE_TIME_FORMATTER_34;
                                                if (formatter == null) {
                                                   formatter = DATE_TIME_FORMATTER_34 = DateTimeFormatter.ofPattern(
                                                      "EEE MMM dd HH:mm:ss O yyyy", Locale.ENGLISH
                                                   );
                                                }

                                                return ZonedDateTime.parse(new String(str, off, len), formatter);
                                             }

                                             if (len == 31 && str[off + 3] == ',') {
                                                DateTimeFormatter formatter = DATE_TIME_FORMATTER_RFC_2822;
                                                if (formatter == null) {
                                                   formatter = DATE_TIME_FORMATTER_RFC_2822 = DateTimeFormatter.ofPattern(
                                                      "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH
                                                   );
                                                }

                                                return ZonedDateTime.parse(new String(str, off, len), formatter);
                                             }

                                             return null;
                                          }

                                          return parseZonedDateTimeCookie(new String(str, off, len));
                                       }

                                       y0 = c0;
                                       y1 = c1;
                                       y2 = c2;
                                       y3 = c3;
                                       m0 = c4;
                                       m1 = c5;
                                       d0 = c6;
                                       d1 = c7;
                                       h0 = c8;
                                       h1 = c9;
                                       i0 = c10;
                                       i1 = c11;
                                       s0 = c12;
                                       s1 = c13;
                                       S0 = c14;
                                       S1 = c15;
                                       S2 = c16;
                                       S3 = '0';
                                       S4 = '0';
                                       S5 = '0';
                                       S6 = '0';
                                       S7 = '0';
                                       S8 = '0';
                                       zoneIdBegin = 17;
                                    } else {
                                       y0 = c0;
                                       y1 = c1;
                                       y2 = c2;
                                       y3 = c3;
                                       m0 = c5;
                                       m1 = c6;
                                       d0 = c8;
                                       d1 = c9;
                                       h0 = c11;
                                       h1 = c12;
                                       i0 = c14;
                                       i1 = c15;
                                       s0 = c17;
                                       s1 = c18;
                                       S0 = c20;
                                       S1 = c21;
                                       S2 = c22;
                                       S3 = c23;
                                       S4 = c24;
                                       S5 = c25;
                                       S6 = c26;
                                       S7 = c27;
                                       S8 = c28;
                                       zoneIdBegin = 29;
                                       isTimeZone = c29 == '|';
                                    }
                                 } else {
                                    y0 = c0;
                                    y1 = c1;
                                    y2 = c2;
                                    y3 = c3;
                                    m0 = c5;
                                    m1 = c6;
                                    d0 = c8;
                                    d1 = c9;
                                    h0 = c11;
                                    h1 = c12;
                                    i0 = c14;
                                    i1 = c15;
                                    s0 = c17;
                                    s1 = c18;
                                    S0 = c20;
                                    S1 = c21;
                                    S2 = c22;
                                    S3 = c23;
                                    S4 = c24;
                                    S5 = c25;
                                    S6 = c26;
                                    S7 = c27;
                                    S8 = '0';
                                    zoneIdBegin = 28;
                                    isTimeZone = c28 == '|';
                                 }
                              } else {
                                 y0 = c0;
                                 y1 = c1;
                                 y2 = c2;
                                 y3 = c3;
                                 m0 = c5;
                                 m1 = c6;
                                 d0 = c8;
                                 d1 = c9;
                                 h0 = c11;
                                 h1 = c12;
                                 i0 = c14;
                                 i1 = c15;
                                 s0 = c17;
                                 s1 = c18;
                                 S0 = c20;
                                 S1 = c21;
                                 S2 = c22;
                                 if (c23 == ' ') {
                                    S3 = '0';
                                    S4 = '0';
                                    S5 = '0';
                                    S6 = '0';
                                    S7 = '0';
                                    S8 = '0';
                                    zoneIdBegin = 23;
                                 } else {
                                    S3 = c23;
                                    S4 = c24;
                                    S5 = c25;
                                    S6 = c26;
                                    S7 = '0';
                                    S8 = '0';
                                    zoneIdBegin = 27;
                                    isTimeZone = c27 == '|';
                                 }
                              }
                           } else {
                              y0 = c0;
                              y1 = c1;
                              y2 = c2;
                              y3 = c3;
                              m0 = c5;
                              m1 = c6;
                              d0 = c8;
                              d1 = c9;
                              h0 = c11;
                              h1 = c12;
                              i0 = c14;
                              i1 = c15;
                              s0 = c17;
                              s1 = c18;
                              S0 = c20;
                              S1 = c21;
                              S2 = c22;
                              S3 = c23;
                              S4 = c24;
                              S5 = c25;
                              S6 = '0';
                              S7 = '0';
                              S8 = '0';
                              zoneIdBegin = 26;
                              isTimeZone = c26 == '|';
                           }
                        } else {
                           y0 = c0;
                           y1 = c1;
                           y2 = c2;
                           y3 = c3;
                           m0 = c5;
                           m1 = c6;
                           d0 = c8;
                           d1 = c9;
                           h0 = c11;
                           h1 = c12;
                           i0 = c14;
                           i1 = c15;
                           s0 = c17;
                           s1 = c18;
                           S0 = c20;
                           S1 = c21;
                           S2 = c22;
                           S3 = c23;
                           S4 = c24;
                           S5 = '0';
                           S6 = '0';
                           S7 = '0';
                           S8 = '0';
                           zoneIdBegin = 25;
                           isTimeZone = c25 == '|';
                        }
                     } else {
                        y0 = c0;
                        y1 = c1;
                        y2 = c2;
                        y3 = c3;
                        m0 = c5;
                        m1 = c6;
                        d0 = c8;
                        d1 = c9;
                        h0 = c11;
                        h1 = c12;
                        i0 = c14;
                        i1 = c15;
                        s0 = c17;
                        s1 = c18;
                        S0 = c20;
                        S1 = c21;
                        S2 = c22;
                        S3 = c23;
                        S4 = '0';
                        S5 = '0';
                        S6 = '0';
                        S7 = '0';
                        S8 = '0';
                        zoneIdBegin = 24;
                        isTimeZone = c24 == '|';
                     }
                  } else {
                     y0 = c0;
                     y1 = c1;
                     y2 = c2;
                     y3 = c3;
                     m0 = c5;
                     m1 = c6;
                     d0 = c8;
                     d1 = c9;
                     h0 = c11;
                     h1 = c12;
                     i0 = c14;
                     i1 = c15;
                     s0 = c17;
                     s1 = c18;
                     S0 = c20;
                     S1 = c21;
                     S2 = c22;
                     S3 = '0';
                     S4 = '0';
                     S5 = '0';
                     S6 = '0';
                     S7 = '0';
                     S8 = '0';
                     zoneIdBegin = 23;
                     isTimeZone = c23 == '|';
                  }
               } else {
                  y0 = c0;
                  y1 = c1;
                  y2 = c2;
                  y3 = c3;
                  m0 = c5;
                  m1 = c6;
                  d0 = c8;
                  d1 = c9;
                  h0 = c11;
                  h1 = c12;
                  i0 = c14;
                  i1 = c15;
                  s0 = c17;
                  s1 = c18;
                  S0 = c20;
                  S1 = c21;
                  S2 = '0';
                  S3 = '0';
                  S4 = '0';
                  S5 = '0';
                  S6 = '0';
                  S7 = '0';
                  S8 = '0';
                  zoneIdBegin = 22;
                  isTimeZone = c22 == '|';
               }
            } else {
               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               S0 = c20;
               S1 = '0';
               S2 = '0';
               S3 = '0';
               S4 = '0';
               S5 = '0';
               S6 = '0';
               S7 = '0';
               S8 = '0';
               zoneIdBegin = 21;
               isTimeZone = c21 == '|';
            }
         } else {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
            S0 = '0';
            S1 = '0';
            S2 = '0';
            S3 = '0';
            S4 = '0';
            S5 = '0';
            S6 = '0';
            S7 = '0';
            S8 = '0';
            zoneIdBegin = 19;
         }

         if (pm && h0 == '1' && h1 == '2') {
            pm = false;
         }

         if (pm) {
            int hourValue = hourAfterNoon(h0, h1);
            h0 = (char)(hourValue >> 16);
            h1 = (char)((short)hourValue);
         }

         LocalDateTime ldt = localDateTime(y0, y1, y2, y3, m0, m1, d0, d1, h0, h1, i0, i1, s0, s1, S0, S1, S2, S3, S4, S5, S6, S7, S8);
         if (ldt == null) {
            return null;
         } else {
            ZoneId zoneId;
            if (isTimeZone) {
               String tzStr = new String(str, zoneIdBegin, len - zoneIdBegin);
               switch (tzStr) {
                  case "UTC":
                  case "[UTC]":
                     zoneId = ZoneOffset.UTC;
                     break;
                  default:
                     TimeZone timeZone = TimeZone.getTimeZone(tzStr);
                     zoneId = timeZone.toZoneId();
               }
            } else if (zoneIdBegin == len) {
               zoneId = defaultZoneId;
            } else {
               char first = str[off + zoneIdBegin];
               if (first == 'Z') {
                  zoneId = ZoneOffset.UTC;
               } else {
                  if (zoneIdStr == null) {
                     if (first == '+' || first == '-') {
                        zoneIdStr = new String(str, off + zoneIdBegin, len - zoneIdBegin);
                     } else if (first == ' ') {
                        zoneIdStr = new String(str, off + zoneIdBegin + 1, len - zoneIdBegin - 1);
                     } else if (zoneIdBegin < len) {
                        zoneIdStr = new String(str, off + zoneIdBegin + 1, len - zoneIdBegin - 2);
                     }
                  }

                  zoneId = getZoneId(zoneIdStr, defaultZoneId);
               }
            }

            if (zoneId == null) {
               zoneId = defaultZoneId;
            }

            if (zoneId == null) {
               zoneId = DEFAULT_ZONE_ID;
            }

            return ZonedDateTime.ofLocal(ldt, zoneId, null);
         }
      }
   }

   static ZonedDateTime parseZonedDateTimeCookie(String str) {
      if (str.endsWith(" CST")) {
         DateTimeFormatter formatter = DATE_TIME_FORMATTER_COOKIE_LOCAL;
         if (formatter == null) {
            formatter = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
            DATE_TIME_FORMATTER_COOKIE_LOCAL = formatter;
         }

         String strLocalDateTime = str.substring(0, str.length() - 4);
         LocalDateTime ldt = LocalDateTime.parse(strLocalDateTime, formatter);
         return ZonedDateTime.of(ldt, SHANGHAI_ZONE_ID);
      } else {
         DateTimeFormatter formatter = DATE_TIME_FORMATTER_COOKIE;
         if (formatter == null) {
            formatter = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.ENGLISH);
            DATE_TIME_FORMATTER_COOKIE = formatter;
         }

         return ZonedDateTime.parse(str, formatter);
      }
   }

   public static ZoneId getZoneId(String zoneIdStr, ZoneId defaultZoneId) {
      if (zoneIdStr == null) {
         return defaultZoneId != null ? defaultZoneId : DEFAULT_ZONE_ID;
      } else {
         ZoneId zoneId;
         switch (zoneIdStr) {
            case "000":
               zoneId = ZoneOffset.UTC;
               break;
            case "+08:00":
               zoneId = OFFSET_8_ZONE_ID;
               break;
            case "CST":
               zoneId = SHANGHAI_ZONE_ID;
               break;
            default:
               char c0;
               if (zoneIdStr.length() > 0 && ((c0 = zoneIdStr.charAt(0)) == '+' || c0 == '-') && zoneIdStr.charAt(zoneIdStr.length() - 1) != ']') {
                  zoneId = ZoneOffset.of(zoneIdStr);
               } else {
                  int p0;
                  int p1;
                  if ((p0 = zoneIdStr.indexOf(91)) > 0 && (p1 = zoneIdStr.indexOf(93, p0)) > 0) {
                     String str = zoneIdStr.substring(p0 + 1, p1);
                     zoneId = ZoneId.of(str);
                  } else {
                     zoneId = ZoneId.of(zoneIdStr);
                  }
               }
         }

         return zoneId;
      }
   }

   public static long parseMillisYMDHMS19(String str, ZoneId zoneId) {
      if (str == null) {
         return 0L;
      } else {
         char c0;
         char c1;
         char c2;
         char c3;
         char c4;
         char c5;
         char c6;
         char c7;
         char c8;
         char c9;
         char c10;
         char c11;
         char c12;
         char c13;
         char c14;
         char c15;
         char c16;
         char c17;
         char c18;
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            if (chars.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = chars[0];
            c1 = chars[1];
            c2 = chars[2];
            c3 = chars[3];
            c4 = chars[4];
            c5 = chars[5];
            c6 = chars[6];
            c7 = chars[7];
            c8 = chars[8];
            c9 = chars[9];
            c10 = chars[10];
            c11 = chars[11];
            c12 = chars[12];
            c13 = chars[13];
            c14 = chars[14];
            c15 = chars[15];
            c16 = chars[16];
            c17 = chars[17];
            c18 = chars[18];
         } else if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0 && JDKUtils.STRING_VALUE != null) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            if (bytes.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = (char)bytes[0];
            c1 = (char)bytes[1];
            c2 = (char)bytes[2];
            c3 = (char)bytes[3];
            c4 = (char)bytes[4];
            c5 = (char)bytes[5];
            c6 = (char)bytes[6];
            c7 = (char)bytes[7];
            c8 = (char)bytes[8];
            c9 = (char)bytes[9];
            c10 = (char)bytes[10];
            c11 = (char)bytes[11];
            c12 = (char)bytes[12];
            c13 = (char)bytes[13];
            c14 = (char)bytes[14];
            c15 = (char)bytes[15];
            c16 = (char)bytes[16];
            c17 = (char)bytes[17];
            c18 = (char)bytes[18];
         } else {
            if (str.length() != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = str.charAt(0);
            c1 = str.charAt(1);
            c2 = str.charAt(2);
            c3 = str.charAt(3);
            c4 = str.charAt(4);
            c5 = str.charAt(5);
            c6 = str.charAt(6);
            c7 = str.charAt(7);
            c8 = str.charAt(8);
            c9 = str.charAt(9);
            c10 = str.charAt(10);
            c11 = str.charAt(11);
            c12 = str.charAt(12);
            c13 = str.charAt(13);
            c14 = str.charAt(14);
            c15 = str.charAt(15);
            c16 = str.charAt(16);
            c17 = str.charAt(17);
            c18 = str.charAt(18);
         }

         if (c4 != '-' || c7 != '-' || c10 != ' ' || c13 != ':' || c16 != ':') {
            throw new DateTimeParseException("illegal input", str, 0);
         } else if (c0 >= '0' && c0 <= '9' && c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9' && c3 >= '0' && c3 <= '9') {
            int year = (c0 - '0') * 1000 + (c1 - '0') * 100 + (c2 - '0') * 10 + (c3 - '0');
            if (c5 >= '0' && c5 <= '9' && c6 >= '0' && c6 <= '9') {
               int month = (c5 - '0') * 10 + (c6 - '0');
               if ((month != 0 || year == 0) && month <= 12) {
                  if (c8 >= '0' && c8 <= '9' && c9 >= '0' && c9 <= '9') {
                     int dom = (c8 - '0') * 10 + (c9 - '0');
                     int max = 31;
                     switch (month) {
                        case 2:
                           boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                           max = leapYear ? 29 : 28;
                        case 3:
                        case 5:
                        case 7:
                        case 8:
                        case 10:
                        default:
                           break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                           max = 30;
                     }

                     if ((dom != 0 || year == 0) && dom <= max) {
                        if (c11 >= '0' && c11 <= '9' && c12 >= '0' && c12 <= '9') {
                           max = (c11 - '0') * 10 + (c12 - '0');
                           if (c14 >= '0' && c14 <= '9' && c15 >= '0' && c15 <= '9') {
                              int minute = (c14 - '0') * 10 + (c15 - '0');
                              if (c17 >= '0' && c17 <= '9' && c18 >= '0' && c18 <= '9') {
                                 int second = (c17 - '0') * 10 + (c18 - '0');
                                 if (year == 0 && month == 0 && dom == 0) {
                                    year = 1970;
                                    month = 1;
                                    dom = 1;
                                 }

                                 int DAYS_PER_CYCLE = 146097;
                                 long DAYS_0000_TO_1970 = 719528L;
                                 long total = (long)(
                                    365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1)
                                 );
                                 if (month > 2) {
                                    total--;
                                    boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                                    if (!leapYear) {
                                       total--;
                                    }
                                 }

                                 long epochDay = total - 719528L;
                                 long utcSeconds = epochDay * 86400L + (long)(max * 3600) + (long)(minute * 60) + (long)second;
                                 if (zoneId == null) {
                                    zoneId = DEFAULT_ZONE_ID;
                                 }

                                 boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                                 long SECONDS_1991_09_15_02 = 684900000L;
                                 if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                                    DAYS_PER_CYCLE = 28800;
                                 } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                                    LocalDate localDate = LocalDate.of(year, month, dom);
                                    LocalTime localTime = LocalTime.of(max, minute, second, 0);
                                    LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                                    ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                                    DAYS_PER_CYCLE = offset.getTotalSeconds();
                                 } else {
                                    DAYS_PER_CYCLE = 0;
                                 }

                                 return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                              } else {
                                 throw new DateTimeParseException("illegal input", str, 0);
                              }
                           } else {
                              throw new DateTimeParseException("illegal input", str, 0);
                           }
                        } else {
                           throw new DateTimeParseException("illegal input", str, 0);
                        }
                     } else {
                        throw new DateTimeParseException("illegal input", str, 0);
                     }
                  } else {
                     throw new DateTimeParseException("illegal input", str, 0);
                  }
               } else {
                  throw new DateTimeParseException("illegal input", str, 0);
               }
            } else {
               throw new DateTimeParseException("illegal input", str, 0);
            }
         } else {
            throw new DateTimeParseException("illegal input", str, 0);
         }
      }
   }

   static long parseMillis19(String str, ZoneId zoneId, DateUtils.DateTimeFormatPattern pattern) {
      if (str == null || "null".equals(str)) {
         return 0L;
      } else if (pattern.length != 19) {
         throw new UnsupportedOperationException();
      } else {
         char c0;
         char c1;
         char c2;
         char c3;
         char c4;
         char c5;
         char c6;
         char c7;
         char c8;
         char c9;
         char c10;
         char c11;
         char c12;
         char c13;
         char c14;
         char c15;
         char c16;
         char c17;
         char c18;
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            if (chars.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = chars[0];
            c1 = chars[1];
            c2 = chars[2];
            c3 = chars[3];
            c4 = chars[4];
            c5 = chars[5];
            c6 = chars[6];
            c7 = chars[7];
            c8 = chars[8];
            c9 = chars[9];
            c10 = chars[10];
            c11 = chars[11];
            c12 = chars[12];
            c13 = chars[13];
            c14 = chars[14];
            c15 = chars[15];
            c16 = chars[16];
            c17 = chars[17];
            c18 = chars[18];
         } else if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            if (bytes.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = (char)bytes[0];
            c1 = (char)bytes[1];
            c2 = (char)bytes[2];
            c3 = (char)bytes[3];
            c4 = (char)bytes[4];
            c5 = (char)bytes[5];
            c6 = (char)bytes[6];
            c7 = (char)bytes[7];
            c8 = (char)bytes[8];
            c9 = (char)bytes[9];
            c10 = (char)bytes[10];
            c11 = (char)bytes[11];
            c12 = (char)bytes[12];
            c13 = (char)bytes[13];
            c14 = (char)bytes[14];
            c15 = (char)bytes[15];
            c16 = (char)bytes[16];
            c17 = (char)bytes[17];
            c18 = (char)bytes[18];
         } else {
            if (str.length() != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = str.charAt(0);
            c1 = str.charAt(1);
            c2 = str.charAt(2);
            c3 = str.charAt(3);
            c4 = str.charAt(4);
            c5 = str.charAt(5);
            c6 = str.charAt(6);
            c7 = str.charAt(7);
            c8 = str.charAt(8);
            c9 = str.charAt(9);
            c10 = str.charAt(10);
            c11 = str.charAt(11);
            c12 = str.charAt(12);
            c13 = str.charAt(13);
            c14 = str.charAt(14);
            c15 = str.charAt(15);
            c16 = str.charAt(16);
            c17 = str.charAt(17);
            c18 = str.charAt(18);
         }

         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         char y0;
         switch (pattern) {
            case DATE_TIME_FORMAT_19_DASH:
               if (c4 != '-' || c7 != '-' || c10 != ' ' || c13 != ':' || c16 != ':') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               break;
            case DATE_TIME_FORMAT_19_DASH_T:
               if (c4 != '-' || c7 != '-' || c10 != 'T' || c13 != ':' || c16 != ':') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               break;
            case DATE_TIME_FORMAT_19_SLASH:
               if (c4 != '/' || c7 != '/' || c10 != ' ' || c13 != ':' || c16 != ':') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               break;
            case DATE_TIME_FORMAT_19_DOT:
               if (c2 != '.' || c5 != '.' || c10 != ' ' || c13 != ':' || c16 != ':') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               d0 = c0;
               d1 = c1;
               m0 = c3;
               m1 = c4;
               y0 = c6;
               y1 = c7;
               y2 = c8;
               y3 = c9;
               h0 = c11;
               h1 = c12;
               i0 = c14;
               i1 = c15;
               s0 = c17;
               s1 = c18;
               break;
            default:
               throw new DateTimeParseException("illegal input", str, 0);
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (month == 0 && year != 0 || month > 12) {
                  throw new DateTimeParseException("illegal input", str, 0);
               } else if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  int max = 31;
                  switch (month) {
                     case 2:
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        max = leapYear ? 29 : 28;
                     case 3:
                     case 5:
                     case 7:
                     case 8:
                     case 10:
                     default:
                        break;
                     case 4:
                     case 6:
                     case 9:
                     case 11:
                        max = 30;
                  }

                  if (dom == 0 && year != 0 || dom > max) {
                     throw new DateTimeParseException("illegal input", str, 0);
                  } else if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     max = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           if (year == 0 && month == 0 && dom == 0) {
                              year = 1970;
                              month = 1;
                              dom = 1;
                           }

                           int DAYS_PER_CYCLE = 146097;
                           long DAYS_0000_TO_1970 = 719528L;
                           long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
                           if (month > 2) {
                              total--;
                              boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                              if (!leapYear) {
                                 total--;
                              }
                           }

                           long epochDay = total - 719528L;
                           long utcSeconds = epochDay * 86400L + (long)(max * 3600) + (long)(minute * 60) + (long)second;
                           if (zoneId == null) {
                              zoneId = DEFAULT_ZONE_ID;
                           }

                           boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                           long SECONDS_1991_09_15_02 = 684900000L;
                           if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                              DAYS_PER_CYCLE = 28800;
                           } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                              LocalDate localDate = LocalDate.of(year, month, dom);
                              LocalTime localTime = LocalTime.of(max, minute, second, 0);
                              LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                              ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                              DAYS_PER_CYCLE = offset.getTotalSeconds();
                           } else {
                              DAYS_PER_CYCLE = 0;
                           }

                           return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                        } else {
                           throw new DateTimeParseException("illegal input", str, 0);
                        }
                     } else {
                        throw new DateTimeParseException("illegal input", str, 0);
                     }
                  } else {
                     throw new DateTimeParseException("illegal input", str, 0);
                  }
               } else {
                  throw new DateTimeParseException("illegal input", str, 0);
               }
            } else {
               throw new DateTimeParseException("illegal input", str, 0);
            }
         } else {
            throw new DateTimeParseException("illegal input", str, 0);
         }
      }
   }

   static long parseMillis10(String str, ZoneId zoneId, DateUtils.DateTimeFormatPattern pattern) {
      if (str == null || "null".equals(str)) {
         return 0L;
      } else if (pattern.length != 10) {
         throw new UnsupportedOperationException();
      } else {
         char c0;
         char c1;
         char c2;
         char c3;
         char c4;
         char c5;
         char c6;
         char c7;
         char c8;
         char c9;
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            if (chars.length != 10) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = chars[0];
            c1 = chars[1];
            c2 = chars[2];
            c3 = chars[3];
            c4 = chars[4];
            c5 = chars[5];
            c6 = chars[6];
            c7 = chars[7];
            c8 = chars[8];
            c9 = chars[9];
         } else if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            if (bytes.length != 10) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = (char)bytes[0];
            c1 = (char)bytes[1];
            c2 = (char)bytes[2];
            c3 = (char)bytes[3];
            c4 = (char)bytes[4];
            c5 = (char)bytes[5];
            c6 = (char)bytes[6];
            c7 = (char)bytes[7];
            c8 = (char)bytes[8];
            c9 = (char)bytes[9];
         } else {
            if (str.length() != 10) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = str.charAt(0);
            c1 = str.charAt(1);
            c2 = str.charAt(2);
            c3 = str.charAt(3);
            c4 = str.charAt(4);
            c5 = str.charAt(5);
            c6 = str.charAt(6);
            c7 = str.charAt(7);
            c8 = str.charAt(8);
            c9 = str.charAt(9);
         }

         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char y0;
         switch (pattern) {
            case DATE_FORMAT_10_DASH:
               if (c4 != '-' || c7 != '-') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               break;
            case DATE_FORMAT_10_SLASH:
               if (c4 != '/' || c7 != '/') {
                  throw new DateTimeParseException("illegal input", str, 0);
               }

               y0 = c0;
               y1 = c1;
               y2 = c2;
               y3 = c3;
               m0 = c5;
               m1 = c6;
               d0 = c8;
               d1 = c9;
               break;
            default:
               throw new DateTimeParseException("illegal input", str, 0);
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (month == 0 && year != 0 || month > 12) {
                  throw new DateTimeParseException("illegal input", str, 0);
               } else if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  int max = 31;
                  switch (month) {
                     case 2:
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        max = leapYear ? 29 : 28;
                     case 3:
                     case 5:
                     case 7:
                     case 8:
                     case 10:
                     default:
                        break;
                     case 4:
                     case 6:
                     case 9:
                     case 11:
                        max = 30;
                  }

                  if ((dom != 0 || year == 0) && dom <= max) {
                     if (year == 0 && month == 0 && dom == 0) {
                        year = 1970;
                        month = 1;
                        dom = 1;
                     }

                     int DAYS_PER_CYCLE = 146097;
                     long DAYS_0000_TO_1970 = 719528L;
                     long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
                     if (month > 2) {
                        total--;
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        if (!leapYear) {
                           total--;
                        }
                     }

                     long epochDay = total - 719528L;
                     long utcSeconds = epochDay * 86400L;
                     boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                     long SECONDS_1991_09_15_02 = 684900000L;
                     if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                        DAYS_PER_CYCLE = 28800;
                     } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                        LocalDate localDate = LocalDate.of(year, month, dom);
                        LocalDateTime ldt = LocalDateTime.of(localDate, LocalTime.MIN);
                        ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                        DAYS_PER_CYCLE = offset.getTotalSeconds();
                     } else {
                        DAYS_PER_CYCLE = 0;
                     }

                     return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                  } else {
                     throw new DateTimeParseException("illegal input", str, 0);
                  }
               } else {
                  throw new DateTimeParseException("illegal input", str, 0);
               }
            } else {
               throw new DateTimeParseException("illegal input", str, 0);
            }
         } else {
            throw new DateTimeParseException("illegal input", str, 0);
         }
      }
   }

   public static long parseMillis19(String str, ZoneId zoneId) {
      if (str == null) {
         throw new NullPointerException();
      } else {
         char c0;
         char c1;
         char c2;
         char c3;
         char c4;
         char c5;
         char c6;
         char c7;
         char c8;
         char c9;
         char c10;
         char c11;
         char c12;
         char c13;
         char c14;
         char c15;
         char c16;
         char c17;
         char c18;
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            if (chars.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = chars[0];
            c1 = chars[1];
            c2 = chars[2];
            c3 = chars[3];
            c4 = chars[4];
            c5 = chars[5];
            c6 = chars[6];
            c7 = chars[7];
            c8 = chars[8];
            c9 = chars[9];
            c10 = chars[10];
            c11 = chars[11];
            c12 = chars[12];
            c13 = chars[13];
            c14 = chars[14];
            c15 = chars[15];
            c16 = chars[16];
            c17 = chars[17];
            c18 = chars[18];
         } else if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(str) == 0) {
            byte[] chars = JDKUtils.STRING_VALUE.apply(str);
            if (chars.length != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = (char)chars[0];
            c1 = (char)chars[1];
            c2 = (char)chars[2];
            c3 = (char)chars[3];
            c4 = (char)chars[4];
            c5 = (char)chars[5];
            c6 = (char)chars[6];
            c7 = (char)chars[7];
            c8 = (char)chars[8];
            c9 = (char)chars[9];
            c10 = (char)chars[10];
            c11 = (char)chars[11];
            c12 = (char)chars[12];
            c13 = (char)chars[13];
            c14 = (char)chars[14];
            c15 = (char)chars[15];
            c16 = (char)chars[16];
            c17 = (char)chars[17];
            c18 = (char)chars[18];
         } else {
            if (str.length() != 19) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            c0 = str.charAt(0);
            c1 = str.charAt(1);
            c2 = str.charAt(2);
            c3 = str.charAt(3);
            c4 = str.charAt(4);
            c5 = str.charAt(5);
            c6 = str.charAt(6);
            c7 = str.charAt(7);
            c8 = str.charAt(8);
            c9 = str.charAt(9);
            c10 = str.charAt(10);
            c11 = str.charAt(11);
            c12 = str.charAt(12);
            c13 = str.charAt(13);
            c14 = str.charAt(14);
            c15 = str.charAt(15);
            c16 = str.charAt(16);
            c17 = str.charAt(17);
            c18 = str.charAt(18);
         }

         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         char y0;
         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c4 == '/' && c7 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == '/' && c5 == '/' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == '.' && c5 == '.' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = '0';
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else {
            if (c2 != ' ' || c6 != ' ' || c11 != ' ' || c14 != ':' || c17 != ':') {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = c15;
            i1 = c16;
            s0 = '0';
            s1 = c18;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (month == 0 && year != 0 || month > 12) {
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               } else if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  int max = 31;
                  switch (month) {
                     case 2:
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        max = leapYear ? 29 : 28;
                     case 3:
                     case 5:
                     case 7:
                     case 8:
                     case 10:
                     default:
                        break;
                     case 4:
                     case 6:
                     case 9:
                     case 11:
                        max = 30;
                  }

                  if (dom == 0 && year != 0 || dom > max) {
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  } else if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     max = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           if (year == 0 && month == 0 && dom == 0) {
                              year = 1970;
                              month = 1;
                              dom = 1;
                           }

                           int DAYS_PER_CYCLE = 146097;
                           long DAYS_0000_TO_1970 = 719528L;
                           long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
                           if (month > 2) {
                              total--;
                              boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                              if (!leapYear) {
                                 total--;
                              }
                           }

                           long epochDay = total - 719528L;
                           long utcSeconds = epochDay * 86400L + (long)(max * 3600) + (long)(minute * 60) + (long)second;
                           if (zoneId == null) {
                              zoneId = DEFAULT_ZONE_ID;
                           }

                           boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                           long SECONDS_1991_09_15_02 = 684900000L;
                           if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                              DAYS_PER_CYCLE = 28800;
                           } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                              LocalDate localDate = LocalDate.of(year, month, dom);
                              LocalTime localTime = LocalTime.of(max, minute, second, 0);
                              LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                              ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                              DAYS_PER_CYCLE = offset.getTotalSeconds();
                           } else {
                              DAYS_PER_CYCLE = 0;
                           }

                           return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                        } else {
                           throw new DateTimeParseException("illegal input " + str, str, 0);
                        }
                     } else {
                        throw new DateTimeParseException("illegal input " + str, str, 0);
                     }
                  } else {
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  }
               } else {
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               }
            } else {
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }
         } else {
            throw new DateTimeParseException("illegal input " + str, str, 0);
         }
      }
   }

   public static long parseMillis19(byte[] bytes, int off, ZoneId zoneId) {
      if (bytes == null) {
         throw new NullPointerException();
      } else {
         char c0 = (char)bytes[off];
         char c1 = (char)bytes[off + 1];
         char c2 = (char)bytes[off + 2];
         char c3 = (char)bytes[off + 3];
         char c4 = (char)bytes[off + 4];
         char c5 = (char)bytes[off + 5];
         char c6 = (char)bytes[off + 6];
         char c7 = (char)bytes[off + 7];
         char c8 = (char)bytes[off + 8];
         char c9 = (char)bytes[off + 9];
         char c10 = (char)bytes[off + 10];
         char c11 = (char)bytes[off + 11];
         char c12 = (char)bytes[off + 12];
         char c13 = (char)bytes[off + 13];
         char c14 = (char)bytes[off + 14];
         char c15 = (char)bytes[off + 15];
         char c16 = (char)bytes[off + 16];
         char c17 = (char)bytes[off + 17];
         char c18 = (char)bytes[off + 18];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c4 == '/' && c7 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if ((c2 == '/' && c5 == '/' || c2 == '-' && c5 == '-' || c2 == '.' && c5 == '.') && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = '0';
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else {
            if (c2 != ' ' || c6 != ' ' || c11 != ' ' || c14 != ':' || c17 != ':') {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = c15;
            i1 = c16;
            s0 = '0';
            s1 = c18;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (month == 0 && year != 0 || month > 12) {
                  String str = new String(bytes, off, 19);
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               } else if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  int max = 31;
                  switch (month) {
                     case 2:
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        max = leapYear ? 29 : 28;
                     case 3:
                     case 5:
                     case 7:
                     case 8:
                     case 10:
                     default:
                        break;
                     case 4:
                     case 6:
                     case 9:
                     case 11:
                        max = 30;
                  }

                  if (dom == 0 && year != 0 || dom > max) {
                     String str = new String(bytes, off, 19);
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  } else if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     max = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           if (year == 0 && month == 0 && dom == 0) {
                              year = 1970;
                              month = 1;
                              dom = 1;
                           }

                           int DAYS_PER_CYCLE = 146097;
                           long DAYS_0000_TO_1970 = 719528L;
                           long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
                           if (month > 2) {
                              total--;
                              boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                              if (!leapYear) {
                                 total--;
                              }
                           }

                           long epochDay = total - 719528L;
                           long utcSeconds = epochDay * 86400L + (long)(max * 3600) + (long)(minute * 60) + (long)second;
                           if (zoneId == null) {
                              zoneId = DEFAULT_ZONE_ID;
                           }

                           boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                           long SECONDS_1991_09_15_02 = 684900000L;
                           if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                              DAYS_PER_CYCLE = 28800;
                           } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                              LocalDate localDate = LocalDate.of(year, month, dom);
                              LocalTime localTime = LocalTime.of(max, minute, second, 0);
                              LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                              ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                              DAYS_PER_CYCLE = offset.getTotalSeconds();
                           } else {
                              DAYS_PER_CYCLE = 0;
                           }

                           return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                        } else {
                           String str = new String(bytes, off, 19);
                           throw new DateTimeParseException("illegal input " + str, str, 0);
                        }
                     } else {
                        String str = new String(bytes, off, 19);
                        throw new DateTimeParseException("illegal input " + str, str, 0);
                     }
                  } else {
                     String str = new String(bytes, off, 19);
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  }
               } else {
                  String str = new String(bytes, off, 19);
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               }
            } else {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }
         } else {
            String str = new String(bytes, off, 19);
            throw new DateTimeParseException("illegal input " + str, str, 0);
         }
      }
   }

   public static long parseMillis19(char[] bytes, int off, ZoneId zoneId) {
      if (bytes == null) {
         throw new NullPointerException();
      } else {
         char c0 = bytes[off];
         char c1 = bytes[off + 1];
         char c2 = bytes[off + 2];
         char c3 = bytes[off + 3];
         char c4 = bytes[off + 4];
         char c5 = bytes[off + 5];
         char c6 = bytes[off + 6];
         char c7 = bytes[off + 7];
         char c8 = bytes[off + 8];
         char c9 = bytes[off + 9];
         char c10 = bytes[off + 10];
         char c11 = bytes[off + 11];
         char c12 = bytes[off + 12];
         char c13 = bytes[off + 13];
         char c14 = bytes[off + 14];
         char c15 = bytes[off + 15];
         char c16 = bytes[off + 16];
         char c17 = bytes[off + 17];
         char c18 = bytes[off + 18];
         char y0;
         char y1;
         char y2;
         char y3;
         char m0;
         char m1;
         char d0;
         char d1;
         char h0;
         char h1;
         char i0;
         char i1;
         char s0;
         char s1;
         if (c4 == '-' && c7 == '-' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c4 == '/' && c7 == '/' && (c10 == ' ' || c10 == 'T') && c13 == ':' && c16 == ':') {
            y0 = c0;
            y1 = c1;
            y2 = c2;
            y3 = c3;
            m0 = c5;
            m1 = c6;
            d0 = c8;
            d1 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if ((c2 == '/' && c5 == '/' || c2 == '-' && c5 == '-' || c2 == '.' && c5 == '.') && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            m0 = c3;
            m1 = c4;
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c1 == ' ' && c5 == ' ' && c10 == ' ' && c13 == ':' && c16 == ':') {
            d0 = '0';
            d1 = c0;
            int month = month(c2, c3, c4);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c6;
            y1 = c7;
            y2 = c8;
            y3 = c9;
            h0 = c11;
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c13 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = '0';
            h1 = c12;
            i0 = c14;
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else if (c2 == ' ' && c6 == ' ' && c11 == ' ' && c14 == ':' && c16 == ':') {
            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = '0';
            i1 = c15;
            s0 = c17;
            s1 = c18;
         } else {
            if (c2 != ' ' || c6 != ' ' || c11 != ' ' || c14 != ':' || c17 != ':') {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            d0 = c0;
            d1 = c1;
            int month = month(c3, c4, c5);
            if (month <= 0) {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }

            m0 = (char)(48 + month / 10);
            m1 = (char)(48 + month % 10);
            y0 = c7;
            y1 = c8;
            y2 = c9;
            y3 = c10;
            h0 = c12;
            h1 = c13;
            i0 = c15;
            i1 = c16;
            s0 = '0';
            s1 = c18;
         }

         if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
               int month = (m0 - '0') * 10 + (m1 - '0');
               if (month == 0 && year != 0 || month > 12) {
                  String str = new String(bytes, off, 19);
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               } else if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                  int dom = (d0 - '0') * 10 + (d1 - '0');
                  int max = 31;
                  switch (month) {
                     case 2:
                        boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                        max = leapYear ? 29 : 28;
                     case 3:
                     case 5:
                     case 7:
                     case 8:
                     case 10:
                     default:
                        break;
                     case 4:
                     case 6:
                     case 9:
                     case 11:
                        max = 30;
                  }

                  if (dom == 0 && year != 0 || dom > max) {
                     String str = new String(bytes, off, 19);
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  } else if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                     max = (h0 - '0') * 10 + (h1 - '0');
                     if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                        int minute = (i0 - '0') * 10 + (i1 - '0');
                        if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                           int second = (s0 - '0') * 10 + (s1 - '0');
                           if (year == 0 && month == 0 && dom == 0) {
                              year = 1970;
                              month = 1;
                              dom = 1;
                           }

                           int DAYS_PER_CYCLE = 146097;
                           long DAYS_0000_TO_1970 = 719528L;
                           long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
                           if (month > 2) {
                              total--;
                              boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
                              if (!leapYear) {
                                 total--;
                              }
                           }

                           long epochDay = total - 719528L;
                           long utcSeconds = epochDay * 86400L + (long)(max * 3600) + (long)(minute * 60) + (long)second;
                           if (zoneId == null) {
                              zoneId = DEFAULT_ZONE_ID;
                           }

                           boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
                           long SECONDS_1991_09_15_02 = 684900000L;
                           if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
                              DAYS_PER_CYCLE = 28800;
                           } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                              LocalDate localDate = LocalDate.of(year, month, dom);
                              LocalTime localTime = LocalTime.of(max, minute, second, 0);
                              LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                              ZoneOffset offset = zoneId.getRules().getOffset(ldt);
                              DAYS_PER_CYCLE = offset.getTotalSeconds();
                           } else {
                              DAYS_PER_CYCLE = 0;
                           }

                           return (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
                        } else {
                           String str = new String(bytes, off, 19);
                           throw new DateTimeParseException("illegal input " + str, str, 0);
                        }
                     } else {
                        String str = new String(bytes, off, 19);
                        throw new DateTimeParseException("illegal input " + str, str, 0);
                     }
                  } else {
                     String str = new String(bytes, off, 19);
                     throw new DateTimeParseException("illegal input " + str, str, 0);
                  }
               } else {
                  String str = new String(bytes, off, 19);
                  throw new DateTimeParseException("illegal input " + str, str, 0);
               }
            } else {
               String str = new String(bytes, off, 19);
               throw new DateTimeParseException("illegal input " + str, str, 0);
            }
         } else {
            String str = new String(bytes, off, 19);
            throw new DateTimeParseException("illegal input " + str, str, 0);
         }
      }
   }

   public static LocalDateTime localDateTime(
      char y0, char y1, char y2, char y3, char m0, char m1, char d0, char d1, char h0, char h1, char i0, char i1, char s0, char s1
   ) {
      if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
         int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
         if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
            int month = (m0 - '0') * 10 + (m1 - '0');
            if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
               int dom = (d0 - '0') * 10 + (d1 - '0');
               if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                  int hour = (h0 - '0') * 10 + (h1 - '0');
                  if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                     int minute = (i0 - '0') * 10 + (i1 - '0');
                     if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                        int second = (s0 - '0') * 10 + (s1 - '0');
                        if (year == 0 && month == 0 && dom == 0 && hour == 0 && minute == 0 && second == 0) {
                           return null;
                        } else {
                           return hour <= 24 && minute <= 60 && second <= 60 ? LocalDateTime.of(year, month, dom, hour, minute, second, 0) : null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static LocalDateTime localDateTime(
      char y0,
      char y1,
      char y2,
      char y3,
      char m0,
      char m1,
      char d0,
      char d1,
      char h0,
      char h1,
      char i0,
      char i1,
      char s0,
      char s1,
      char S0,
      char S1,
      char S2,
      char S3,
      char S4,
      char S5,
      char S6,
      char S7,
      char S8
   ) {
      if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
         int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
         if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
            int month = (m0 - '0') * 10 + (m1 - '0');
            if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
               int dom = (d0 - '0') * 10 + (d1 - '0');
               if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
                  int hour = (h0 - '0') * 10 + (h1 - '0');
                  if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                     int minute = (i0 - '0') * 10 + (i1 - '0');
                     if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                        int second = (s0 - '0') * 10 + (s1 - '0');
                        if (S0 >= '0'
                           && S0 <= '9'
                           && S1 >= '0'
                           && S1 <= '9'
                           && S2 >= '0'
                           && S2 <= '9'
                           && S3 >= '0'
                           && S3 <= '9'
                           && S4 >= '0'
                           && S4 <= '9'
                           && S5 >= '0'
                           && S5 <= '9'
                           && S6 >= '0'
                           && S6 <= '9'
                           && S7 >= '0'
                           && S7 <= '9'
                           && S8 >= '0'
                           && S8 <= '9') {
                           int nanos = (S0 - '0') * 100000000
                              + (S1 - '0') * 10000000
                              + (S2 - '0') * 1000000
                              + (S3 - '0') * 100000
                              + (S4 - '0') * 10000
                              + (S5 - '0') * 1000
                              + (S6 - '0') * 100
                              + (S7 - '0') * 10
                              + (S8 - '0');
                           return LocalDateTime.of(year, month, dom, hour, minute, second, nanos);
                        } else {
                           return null;
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static long millis(LocalDateTime ldt) {
      return millis(null, ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());
   }

   public static long millis(LocalDateTime ldt, ZoneId zoneId) {
      return millis(zoneId, ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano());
   }

   public static long millis(ZoneId zoneId, int year, int month, int dom, int hour, int minute, int second, int nanoOfSecond) {
      if (zoneId == null) {
         zoneId = DEFAULT_ZONE_ID;
      }

      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
      if (month > 2) {
         total--;
         boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
         if (!leapYear) {
            total--;
         }
      }

      long epochDay = total - 719528L;
      long utcSeconds = epochDay * 86400L + (long)(hour * 3600) + (long)(minute * 60) + (long)second;
      boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
      long SECONDS_1991_09_15_02 = 684900000L;
      if (shanghai && utcSeconds >= SECONDS_1991_09_15_02) {
         DAYS_PER_CYCLE = 28800;
      } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
         LocalDate localDate = LocalDate.of(year, month, dom);
         LocalTime localTime = LocalTime.of(hour, minute, second, nanoOfSecond);
         LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
         ZoneOffset offset = zoneId.getRules().getOffset(ldt);
         DAYS_PER_CYCLE = offset.getTotalSeconds();
      } else {
         DAYS_PER_CYCLE = 0;
      }

      long millis = (utcSeconds - (long)DAYS_PER_CYCLE) * 1000L;
      if (nanoOfSecond != 0) {
         millis += (long)(nanoOfSecond / 1000000);
      }

      return millis;
   }

   public static long utcSeconds(int year, int month, int dom, int hour, int minute, int second) {
      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long total = (long)(365 * year + (year + 3) / 4 - (year + 99) / 100 + (year + 399) / 400 + (367 * month - 362) / 12 + (dom - 1));
      if (month > 2) {
         total--;
         boolean leapYear = (year & 3) == 0 && (year % 100 != 0 || year % 400 == 0);
         if (!leapYear) {
            total--;
         }
      }

      long epochDay = total - 719528L;
      return epochDay * 86400L + (long)(hour * 3600) + (long)(minute * 60) + (long)second;
   }

   public static String formatYMDHMS19(Date date) {
      return formatYMDHMS19(date, DEFAULT_ZONE_ID);
   }

   public static String formatYMDHMS19(Date date, ZoneId zoneId) {
      if (date == null) {
         return null;
      } else {
         long timeMillis = date.getTime();
         if (zoneId == null) {
            zoneId = DEFAULT_ZONE_ID;
         }

         int SECONDS_PER_DAY = 86400;
         long epochSecond = Math.floorDiv(timeMillis, 1000L);
         long SECONDS_1991_09_15_02 = 684900000L;
         boolean shanghai = zoneId == SHANGHAI_ZONE_ID || zoneId.getRules() == SHANGHAI_ZONE_RULES;
         int offsetTotalSeconds;
         if (shanghai && epochSecond > 684900000L) {
            offsetTotalSeconds = 28800;
         } else {
            Instant instant = Instant.ofEpochMilli(timeMillis);
            offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
         }

         long localSecond = epochSecond + (long)offsetTotalSeconds;
         long localEpochDay = Math.floorDiv(localSecond, 86400L);
         int secsOfDay = (int)Math.floorMod(localSecond, 86400L);
         int DAYS_PER_CYCLE = 146097;
         long DAYS_0000_TO_1970 = 719528L;
         long zeroDay = localEpochDay + 719528L;
         zeroDay -= 60L;
         long adjust = 0L;
         if (zeroDay < 0L) {
            long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
            adjust = adjustCycles * 400L;
            zeroDay += -adjustCycles * 146097L;
         }

         long yearEst = (400L * zeroDay + 591L) / 146097L;
         long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
         if (doyEst < 0L) {
            yearEst--;
            doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
         }

         yearEst += adjust;
         int marchDoy0 = (int)doyEst;
         int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
         int month = (marchMonth0 + 2) % 12 + 1;
         int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
         yearEst += (long)(marchMonth0 / 10);
         if (yearEst >= -999999999L && yearEst <= 999999999L) {
            int year = (int)yearEst;
            int MINUTES_PER_HOUR = 60;
            int SECONDS_PER_MINUTE = 60;
            int SECONDS_PER_HOUR = 3600;
            long secondOfDay = (long)secsOfDay;
            if (secondOfDay >= 0L && secondOfDay <= 86399L) {
               int hours = (int)(secondOfDay / 3600L);
               secondOfDay -= (long)(hours * 3600);
               int minutes = (int)(secondOfDay / 60L);
               secondOfDay -= (long)(minutes * 60);
               int second = (int)secondOfDay;
               if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                  byte[] bytes = new byte[19];
                  IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
                  bytes[10] = 32;
                  IOUtils.writeLocalTime(bytes, 11, hours, minutes, second);
                  return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
               } else {
                  char[] chars = new char[19];
                  IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
                  chars[10] = ' ';
                  IOUtils.writeLocalTime(chars, 11, hours, minutes, second);
                  return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
               }
            } else {
               throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
            }
         } else {
            throw new DateTimeException("Invalid year " + yearEst);
         }
      }
   }

   public static String formatYMD8(Date date) {
      return date == null ? null : formatYMD8(date.getTime(), DEFAULT_ZONE_ID);
   }

   public static String formatYMD8(long timeMillis, ZoneId zoneId) {
      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(timeMillis, 1000L);
      if (zoneId == null) {
         zoneId = DEFAULT_ZONE_ID;
      }

      int offsetTotalSeconds;
      if (zoneId != SHANGHAI_ZONE_ID && zoneId.getRules() != SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(timeMillis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      long localEpochDay = Math.floorDiv(localSecond, 86400L);
      int off = (int)(localEpochDay - (long)LOCAL_EPOCH_DAY + 128L);
      String[] cache = DateUtils.CacheDate8.CACHE;
      if (off >= 0 && off < cache.length) {
         String str = cache[off];
         if (str != null) {
            return str;
         }
      }

      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long zeroDay = localEpochDay + 719528L;
      zeroDay -= 60L;
      long adjust = 0L;
      if (zeroDay < 0L) {
         long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
         adjust = adjustCycles * 400L;
         zeroDay += -adjustCycles * 146097L;
      }

      long yearEst = (400L * zeroDay + 591L) / 146097L;
      long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      if (doyEst < 0L) {
         yearEst--;
         doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      }

      yearEst += adjust;
      int marchDoy0 = (int)doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += (long)(marchMonth0 / 10);
      if (yearEst >= -999999999L && yearEst <= 999999999L) {
         int year = (int)yearEst;
         DAYS_PER_CYCLE = year / 100;
         int y23 = year - DAYS_PER_CYCLE * 100;
         String str;
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] bytes = new byte[8];
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[DAYS_PER_CYCLE]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 2L, IOUtils.PACKED_DIGITS[y23]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS[month]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS[dayOfMonth]);
            str = JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
         } else {
            char[] chars = new char[8];
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[DAYS_PER_CYCLE]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS_UTF16[y23]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS_UTF16[month]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 12L, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            } else {
               str = new String(chars);
            }
         }

         if (off >= 0 && off < cache.length) {
            cache[off] = str;
         }

         return str;
      } else {
         throw new DateTimeException("Invalid year " + yearEst);
      }
   }

   public static String formatYMD10(int year, int month, int dayOfMonth) {
      if (JDKUtils.STRING_CREATOR_JDK11 != null) {
         byte[] bytes = new byte[10];
         IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
         return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
      } else {
         char[] chars = new char[10];
         IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
         return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
      }
   }

   public static String formatYMD10(LocalDate date) {
      return date == null ? null : formatYMD10(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
   }

   public static String formatYMD10(Date date) {
      return date == null ? null : formatYMD10(date.getTime(), DEFAULT_ZONE_ID);
   }

   public static String formatYMD8(LocalDate date) {
      if (date == null) {
         return null;
      } else {
         int year = date.getYear();
         int month = date.getMonthValue();
         int dayOfMonth = date.getDayOfMonth();
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         String str;
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] bytes = new byte[8];
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[y01]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 2L, IOUtils.PACKED_DIGITS[y23]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS[month]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS[dayOfMonth]);
            str = JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
         } else {
            char[] chars = new char[8];
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[y01]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS_UTF16[y23]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS_UTF16[month]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 12L, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            } else {
               str = new String(chars);
            }
         }

         return str;
      }
   }

   public static String formatYMD10(long timeMillis, ZoneId zoneId) {
      if (zoneId == null) {
         zoneId = DEFAULT_ZONE_ID;
      }

      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(timeMillis, 1000L);
      int offsetTotalSeconds;
      if (zoneId != SHANGHAI_ZONE_ID && zoneId.getRules() != SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(timeMillis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      long localEpochDay = Math.floorDiv(localSecond, 86400L);
      int off = (int)(localEpochDay - (long)LOCAL_EPOCH_DAY + 128L);
      String[] cache = DateUtils.CacheDate10.CACHE;
      if (off >= 0 && off < cache.length) {
         String str = cache[off];
         if (str != null) {
            return str;
         }
      }

      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long zeroDay = localEpochDay + 719528L;
      zeroDay -= 60L;
      long adjust = 0L;
      if (zeroDay < 0L) {
         long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
         adjust = adjustCycles * 400L;
         zeroDay += -adjustCycles * 146097L;
      }

      long yearEst = (400L * zeroDay + 591L) / 146097L;
      long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      if (doyEst < 0L) {
         yearEst--;
         doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      }

      yearEst += adjust;
      int marchDoy0 = (int)doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += (long)(marchMonth0 / 10);
      if (yearEst >= -999999999L && yearEst <= 999999999L) {
         int year = (int)yearEst;
         String str;
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] bytes = new byte[10];
            IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
            str = JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
         } else {
            char[] chars = new char[10];
            IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            } else {
               str = new String(chars);
            }
         }

         if (off >= 0 && off < cache.length) {
            cache[off] = str;
         }

         return str;
      } else {
         throw new DateTimeException("Invalid year " + yearEst);
      }
   }

   public static String format(Date date, String format) {
      if (date == null) {
         return null;
      } else if (format == null) {
         return format(date);
      } else {
         switch (format) {
            case "yyyy-MM-dd HH:mm:ss":
               return format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
            case "yyyy-MM-ddTHH:mm:ss":
            case "yyyy-MM-dd'T'HH:mm:ss":
               return format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH_T);
            case "dd.MM.yyyy HH:mm:ss":
               return format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DOT);
            case "yyyyMMdd":
               return formatYMD8(date.getTime(), DEFAULT_ZONE_ID);
            case "yyyy-MM-dd":
               return formatYMD10(date.getTime(), DEFAULT_ZONE_ID);
            case "yyyy/MM/dd":
               return format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH);
            case "dd.MM.yyyy":
               return format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT);
            default:
               long epochMilli = date.getTime();
               Instant instant = Instant.ofEpochMilli(epochMilli);
               ZonedDateTime zdt = instant.atZone(DEFAULT_ZONE_ID);
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               return formatter.format(zdt);
         }
      }
   }

   public static String formatYMDHMS19(ZonedDateTime zdt) {
      if (zdt == null) {
         return null;
      } else {
         int year = zdt.getYear();
         int month = zdt.getMonthValue();
         int dayOfMonth = zdt.getDayOfMonth();
         int hour = zdt.getHour();
         int minute = zdt.getMinute();
         int second = zdt.getSecond();
         return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
      }
   }

   public static String format(ZonedDateTime zdt, String format) {
      if (zdt == null) {
         return null;
      } else {
         int year = zdt.getYear();
         int month = zdt.getMonthValue();
         int dayOfMonth = zdt.getDayOfMonth();
         switch (format) {
            case "yyyy-MM-dd HH:mm:ss": {
               int hour = zdt.getHour();
               int minute = zdt.getMinute();
               int second = zdt.getSecond();
               return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
            }
            case "yyyy-MM-ddTHH:mm:ss":
            case "yyyy-MM-dd'T'HH:mm:ss": {
               int hour = zdt.getHour();
               int minute = zdt.getMinute();
               int second = zdt.getSecond();
               return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH_T);
            }
            case "yyyy-MM-dd":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH);
            case "yyyy/MM/dd":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH);
            case "dd.MM.yyyy":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT);
            default:
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               return formatter.format(zdt);
         }
      }
   }

   public static String formatYMDHMS19(LocalDateTime ldt) {
      if (ldt == null) {
         return null;
      } else {
         int year = ldt.getYear();
         int month = ldt.getMonthValue();
         int dayOfMonth = ldt.getDayOfMonth();
         int hour = ldt.getHour();
         int minute = ldt.getMinute();
         int second = ldt.getSecond();
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] bytes = new byte[19];
            IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
            bytes[10] = 32;
            IOUtils.writeLocalTime(bytes, 11, hour, minute, second);
            return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
         } else {
            char[] chars = new char[19];
            IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
            chars[10] = ' ';
            IOUtils.writeLocalTime(chars, 11, hour, minute, second);
            return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
         }
      }
   }

   public static String format(LocalDateTime ldt, String format) {
      if (ldt == null) {
         return null;
      } else {
         int year = ldt.getYear();
         int month = ldt.getMonthValue();
         int dayOfMonth = ldt.getDayOfMonth();
         switch (format) {
            case "yyyy-MM-dd HH:mm:ss": {
               int hour = ldt.getHour();
               int minute = ldt.getMinute();
               int second = ldt.getSecond();
               return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
            }
            case "yyyy-MM-ddTHH:mm:ss":
            case "yyyy-MM-dd'T'HH:mm:ss": {
               int hour = ldt.getHour();
               int minute = ldt.getMinute();
               int second = ldt.getSecond();
               return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH_T);
            }
            case "yyyy-MM-dd":
               return formatYMD10(year, month, dayOfMonth);
            case "yyyy/MM/dd":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH);
            case "dd.MM.yyyy":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT);
            default:
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               return formatter.format(ldt);
         }
      }
   }

   public static String formatYMDHMS19(LocalDate localDate) {
      if (localDate == null) {
         return null;
      } else {
         int year = localDate.getYear();
         int month = localDate.getMonthValue();
         int dayOfMonth = localDate.getDayOfMonth();
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] bytes = new byte[19];
            IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
            bytes[10] = 32;
            IOUtils.writeLocalTime(bytes, 11, 0, 0, 0);
            return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
         } else {
            char[] chars = new char[19];
            IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
            chars[10] = ' ';
            IOUtils.writeLocalTime(chars, 11, 0, 0, 0);
            return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
         }
      }
   }

   public static String format(LocalDate localDate, String format) {
      if (localDate == null) {
         return null;
      } else {
         int year = localDate.getYear();
         int month = localDate.getMonthValue();
         int dayOfMonth = localDate.getDayOfMonth();
         switch (format) {
            case "yyyy-MM-dd HH:mm:ss":
               return format(year, month, dayOfMonth, 0, 0, 0, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
            case "yyyy-MM-ddTHH:mm:ss":
            case "yyyy-MM-dd'T'HH:mm:ss":
               return format(year, month, dayOfMonth, 0, 0, 0, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH_T);
            case "yyyy-MM-dd":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH);
            case "yyyy/MM/dd":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH);
            case "dd.MM.yyyy":
               return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT);
            default:
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
               return formatter.format(localDate);
         }
      }
   }

   public static String format(int year, int month, int dayOfMonth) {
      return format(year, month, dayOfMonth, DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH);
   }

   public static String format(int year, int month, int dayOfMonth, DateUtils.DateTimeFormatPattern pattern) {
      int y01 = year / 100;
      int y23 = year - y01 * 100;
      if (JDKUtils.STRING_CREATOR_JDK11 != null) {
         byte[] bytes = new byte[10];
         if (pattern == DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT) {
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[dayOfMonth]);
            bytes[2] = 46;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L, IOUtils.PACKED_DIGITS[month]);
            bytes[5] = 46;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS[y01]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS[y23]);
         } else {
            byte separator = (byte)(pattern == DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH ? 45 : 47);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[y01]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 2L, IOUtils.PACKED_DIGITS[y23]);
            bytes[4] = separator;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 5L, IOUtils.PACKED_DIGITS[month]);
            bytes[7] = separator;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS[dayOfMonth]);
         }

         return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
      } else {
         char[] chars = new char[10];
         if (pattern == DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT) {
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
            chars[2] = '.';
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS_UTF16[month]);
            chars[5] = '.';
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 12L, IOUtils.PACKED_DIGITS_UTF16[y01]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 16L, IOUtils.PACKED_DIGITS_UTF16[y23]);
         } else {
            char separator = (char)(pattern == DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH ? 45 : 47);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[y01]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS_UTF16[y23]);
            chars[4] = separator;
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 10L, IOUtils.PACKED_DIGITS_UTF16[month]);
            chars[7] = separator;
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 16L, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
         }

         return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
      }
   }

   public static String format(long timeMillis) {
      return format(timeMillis, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
   }

   public static String format(Date date) {
      return date == null ? null : format(date.getTime(), DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
   }

   public static String format(long timeMillis, DateUtils.DateTimeFormatPattern pattern) {
      ZoneId zoneId = DEFAULT_ZONE_ID;
      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(timeMillis, 1000L);
      int offsetTotalSeconds;
      if (zoneId != SHANGHAI_ZONE_ID && zoneId.getRules() != SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(timeMillis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      long localEpochDay = Math.floorDiv(localSecond, 86400L);
      int secsOfDay = (int)Math.floorMod(localSecond, 86400L);
      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long zeroDay = localEpochDay + 719528L;
      zeroDay -= 60L;
      long adjust = 0L;
      if (zeroDay < 0L) {
         long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
         adjust = adjustCycles * 400L;
         zeroDay += -adjustCycles * 146097L;
      }

      long yearEst = (400L * zeroDay + 591L) / 146097L;
      long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      if (doyEst < 0L) {
         yearEst--;
         doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      }

      yearEst += adjust;
      int marchDoy0 = (int)doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += (long)(marchMonth0 / 10);
      if (yearEst >= -999999999L && yearEst <= 999999999L) {
         int year = (int)yearEst;
         if (pattern != DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DASH
            && pattern != DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_SLASH
            && pattern != DateUtils.DateTimeFormatPattern.DATE_FORMAT_10_DOT) {
            int MINUTES_PER_HOUR = 60;
            int SECONDS_PER_MINUTE = 60;
            int SECONDS_PER_HOUR = 3600;
            long secondOfDay = (long)secsOfDay;
            if (secondOfDay >= 0L && secondOfDay <= 86399L) {
               int hours = (int)(secondOfDay / 3600L);
               secondOfDay -= (long)(hours * 3600);
               int minutes = (int)(secondOfDay / 60L);
               secondOfDay -= (long)(minutes * 60);
               int second = (int)secondOfDay;
               return format(year, month, dayOfMonth, hours, minutes, second, pattern);
            } else {
               throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
            }
         } else {
            return format(year, month, dayOfMonth, pattern);
         }
      } else {
         throw new DateTimeException("Invalid year " + yearEst);
      }
   }

   public static String format(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      return format(year, month, dayOfMonth, hour, minute, second, DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH);
   }

   static String format(int year, int month, int dayOfMonth, int hour, int minute, int second, DateUtils.DateTimeFormatPattern pattern) {
      int y01 = year / 100;
      int y23 = year - y01 * 100;
      if (JDKUtils.STRING_CREATOR_JDK11 != null) {
         byte[] bytes = new byte[19];
         if (pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DOT) {
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[dayOfMonth]);
            bytes[2] = 46;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L, IOUtils.PACKED_DIGITS[month]);
            bytes[5] = 46;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS[y01]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS[y23]);
            bytes[10] = 32;
         } else {
            char separator = (char)(pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH ? 32 : 84);
            byte dateSeparator = (byte)(pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_SLASH ? 47 : 45);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET, IOUtils.PACKED_DIGITS[y01]);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 2L, IOUtils.PACKED_DIGITS[y23]);
            bytes[4] = dateSeparator;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 5L, IOUtils.PACKED_DIGITS[month]);
            bytes[7] = dateSeparator;
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L, IOUtils.PACKED_DIGITS[dayOfMonth]);
            bytes[10] = (byte)separator;
         }

         IOUtils.writeLocalTime(bytes, 11, hour, minute, second);
         return JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
      } else {
         char[] chars = new char[19];
         if (pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DOT) {
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
            chars[2] = '.';
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 6L, IOUtils.PACKED_DIGITS_UTF16[month]);
            chars[5] = '.';
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 12L, IOUtils.PACKED_DIGITS_UTF16[y01]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 16L, IOUtils.PACKED_DIGITS_UTF16[y23]);
            chars[10] = ' ';
         } else {
            char separator = (char)(pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_DASH ? 32 : 84);
            char dateSeparator = (char)(pattern == DateUtils.DateTimeFormatPattern.DATE_TIME_FORMAT_19_SLASH ? 47 : 45);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET, IOUtils.PACKED_DIGITS_UTF16[y01]);
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 4L, IOUtils.PACKED_DIGITS_UTF16[y23]);
            chars[4] = dateSeparator;
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 10L, IOUtils.PACKED_DIGITS_UTF16[month]);
            chars[7] = dateSeparator;
            JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + 16L, IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
            chars[10] = separator;
         }

         IOUtils.writeLocalTime(chars, 11, hour, minute, second);
         return JDKUtils.STRING_CREATOR_JDK8 != null ? JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE) : new String(chars);
      }
   }

   public static String toString(Date date) {
      return toString(date.getTime(), false, DEFAULT_ZONE_ID);
   }

   public static String toString(long timeMillis, boolean timeZone, ZoneId zoneId) {
      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(timeMillis, 1000L);
      int offsetTotalSeconds;
      if (zoneId != SHANGHAI_ZONE_ID && zoneId.getRules() != SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(timeMillis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      long localEpochDay = Math.floorDiv(localSecond, 86400L);
      int secsOfDay = (int)Math.floorMod(localSecond, 86400L);
      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long zeroDay = localEpochDay + 719528L;
      zeroDay -= 60L;
      long adjust = 0L;
      if (zeroDay < 0L) {
         long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
         adjust = adjustCycles * 400L;
         zeroDay += -adjustCycles * 146097L;
      }

      long yearEst = (400L * zeroDay + 591L) / 146097L;
      long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      if (doyEst < 0L) {
         yearEst--;
         doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      }

      yearEst += adjust;
      int marchDoy0 = (int)doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += (long)(marchMonth0 / 10);
      if (yearEst >= -999999999L && yearEst <= 999999999L) {
         int year = (int)yearEst;
         int MINUTES_PER_HOUR = 60;
         int SECONDS_PER_MINUTE = 60;
         int SECONDS_PER_HOUR = 3600;
         long secondOfDay = (long)secsOfDay;
         if (secondOfDay >= 0L && secondOfDay <= 86399L) {
            int hours = (int)(secondOfDay / 3600L);
            secondOfDay -= (long)(hours * 3600);
            int minutes = (int)(secondOfDay / 60L);
            secondOfDay -= (long)(minutes * 60);
            int second = (int)secondOfDay;
            int millis = (int)Math.floorMod(timeMillis, 1000L);
            if (millis == 0) {
               SECONDS_PER_MINUTE = 0;
            } else if (millis < 10) {
               SECONDS_PER_MINUTE = 4;
            } else if (millis % 100 == 0) {
               SECONDS_PER_MINUTE = 2;
            } else if (millis % 10 == 0) {
               SECONDS_PER_MINUTE = 3;
            } else {
               SECONDS_PER_MINUTE = 4;
            }

            int zonelen;
            if (timeZone) {
               zonelen = offsetTotalSeconds == 0 ? 1 : 6;
            } else {
               zonelen = 0;
            }

            int len = 19 + SECONDS_PER_MINUTE + zonelen;
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               char[] chars = new char[len];
               IOUtils.writeLocalDate(chars, 0, year, month, dayOfMonth);
               chars[10] = ' ';
               IOUtils.writeLocalTime(chars, 11, hours, minutes, second);
               if (SECONDS_PER_MINUTE > 0) {
                  chars[19] = '.';

                  for (int i = 20; i < len; i++) {
                     chars[i] = '0';
                  }

                  if (millis < 10) {
                     IOUtils.getChars(millis, 19 + SECONDS_PER_MINUTE, chars);
                  } else if (millis % 100 == 0) {
                     IOUtils.getChars(millis / 100, 19 + SECONDS_PER_MINUTE, chars);
                  } else if (millis % 10 == 0) {
                     IOUtils.getChars(millis / 10, 19 + SECONDS_PER_MINUTE, chars);
                  } else {
                     IOUtils.getChars(millis, 19 + SECONDS_PER_MINUTE, chars);
                  }
               }

               if (timeZone) {
                  hours = offsetTotalSeconds / 3600;
                  if (offsetTotalSeconds == 0) {
                     chars[19 + SECONDS_PER_MINUTE] = 'Z';
                  } else {
                     int offsetAbs = Math.abs(hours);
                     if (hours >= 0) {
                        chars[19 + SECONDS_PER_MINUTE] = '+';
                     } else {
                        chars[19 + SECONDS_PER_MINUTE] = '-';
                     }

                     chars[19 + SECONDS_PER_MINUTE + 1] = '0';
                     IOUtils.getChars(offsetAbs, 19 + SECONDS_PER_MINUTE + 3, chars);
                     chars[19 + SECONDS_PER_MINUTE + 3] = ':';
                     chars[19 + SECONDS_PER_MINUTE + 4] = '0';
                     int offsetMinutes = (offsetTotalSeconds - hours * 3600) / 60;
                     if (offsetMinutes < 0) {
                        offsetMinutes = -offsetMinutes;
                     }

                     IOUtils.getChars(offsetMinutes, 19 + SECONDS_PER_MINUTE + zonelen, chars);
                  }
               }

               return JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            } else {
               byte[] bytes = new byte[len];
               IOUtils.writeLocalDate(bytes, 0, year, month, dayOfMonth);
               bytes[10] = 32;
               IOUtils.writeLocalTime(bytes, 11, hours, minutes, second);
               if (SECONDS_PER_MINUTE > 0) {
                  bytes[19] = 46;

                  for (int i = 20; i < len; i++) {
                     bytes[i] = 48;
                  }

                  if (millis < 10) {
                     IOUtils.getChars(millis, 19 + SECONDS_PER_MINUTE, bytes);
                  } else if (millis % 100 == 0) {
                     IOUtils.getChars(millis / 100, 19 + SECONDS_PER_MINUTE, bytes);
                  } else if (millis % 10 == 0) {
                     IOUtils.getChars(millis / 10, 19 + SECONDS_PER_MINUTE, bytes);
                  } else {
                     IOUtils.getChars(millis, 19 + SECONDS_PER_MINUTE, bytes);
                  }
               }

               if (timeZone) {
                  hours = offsetTotalSeconds / 3600;
                  if (offsetTotalSeconds == 0) {
                     bytes[19 + SECONDS_PER_MINUTE] = 90;
                  } else {
                     int offsetAbsx = Math.abs(hours);
                     if (hours >= 0) {
                        bytes[19 + SECONDS_PER_MINUTE] = 43;
                     } else {
                        bytes[19 + SECONDS_PER_MINUTE] = 45;
                     }

                     bytes[19 + SECONDS_PER_MINUTE + 1] = 48;
                     IOUtils.getChars(offsetAbsx, 19 + SECONDS_PER_MINUTE + 3, bytes);
                     bytes[19 + SECONDS_PER_MINUTE + 3] = 58;
                     bytes[19 + SECONDS_PER_MINUTE + 4] = 48;
                     int offsetMinutes = (offsetTotalSeconds - hours * 3600) / 60;
                     if (offsetMinutes < 0) {
                        offsetMinutes = -offsetMinutes;
                     }

                     IOUtils.getChars(offsetMinutes, 19 + SECONDS_PER_MINUTE + zonelen, bytes);
                  }
               }

               return JDKUtils.STRING_CREATOR_JDK11 != null
                  ? JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1)
                  : new String(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
            }
         } else {
            throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
         }
      } else {
         throw new DateTimeException("Invalid year " + yearEst);
      }
   }

   public static int month(char c0, char c1, char c2) {
      switch (c0) {
         case 'A':
            if (c1 == 'p' && c2 == 'r') {
               return 4;
            }

            if (c1 == 'u' && c2 == 'g') {
               return 8;
            }
         case 'B':
         case 'C':
         case 'E':
         case 'G':
         case 'H':
         case 'I':
         case 'K':
         case 'L':
         case 'P':
         case 'Q':
         case 'R':
         default:
            break;
         case 'D':
            if (c1 == 'e' && c2 == 'c') {
               return 12;
            }
            break;
         case 'F':
            if (c1 == 'e' && c2 == 'b') {
               return 2;
            }
            break;
         case 'J':
            if (c1 == 'a' && c2 == 'n') {
               return 1;
            }

            if (c1 == 'u') {
               if (c2 == 'n') {
                  return 6;
               }

               if (c2 == 'l') {
                  return 7;
               }
            }
            break;
         case 'M':
            if (c1 == 'a') {
               if (c2 == 'r') {
                  return 3;
               }

               if (c2 == 'y') {
                  return 5;
               }
            }
            break;
         case 'N':
            if (c1 == 'o' && c2 == 'v') {
               return 11;
            }
            break;
         case 'O':
            if (c1 == 'c' && c2 == 't') {
               return 10;
            }
            break;
         case 'S':
            if (c1 == 'e' && c2 == 'p') {
               return 9;
            }
      }

      return 0;
   }

   public static int hourAfterNoon(char h0, char h1) {
      if (h0 == '0') {
         switch (h1) {
            case '0':
               h0 = '1';
               h1 = '2';
               break;
            case '1':
               h0 = '1';
               h1 = '3';
               break;
            case '2':
               h0 = '1';
               h1 = '4';
               break;
            case '3':
               h0 = '1';
               h1 = '5';
               break;
            case '4':
               h0 = '1';
               h1 = '6';
               break;
            case '5':
               h0 = '1';
               h1 = '7';
               break;
            case '6':
               h0 = '1';
               h1 = '8';
               break;
            case '7':
               h0 = '1';
               h1 = '9';
               break;
            case '8':
               h0 = '2';
               h1 = '0';
               break;
            case '9':
               h0 = '2';
               h1 = '1';
         }
      } else if (h0 == '1') {
         switch (h1) {
            case '0':
               h0 = '2';
               h1 = '2';
               break;
            case '1':
               h0 = '2';
               h1 = '3';
               break;
            case '2':
               h0 = '2';
               h1 = '4';
         }
      }

      return h0 << 16 | h1;
   }

   public static int getShanghaiZoneOffsetTotalSeconds(long seconds) {
      long SECONDS_1991_09_15_02 = 684900000L;
      long SECONDS_1991_04_14_03 = 671598000L;
      long SECONDS_1990_09_16_02 = 653450400L;
      long SECONDS_1990_04_15_03 = 640148400L;
      long SECONDS_1989_09_17_02 = 622000800L;
      long SECONDS_1989_04_16_03 = 608698800L;
      long SECONDS_1988_09_11_02 = 589946400L;
      long SECONDS_1988_04_17_03 = 577249200L;
      long SECONDS_1987_09_13_02 = 558496800L;
      long SECONDS_1987_04_12_03 = 545194800L;
      long SECONDS_1986_09_14_02 = 527047200L;
      long SECONDS_1986_05_04_03 = 515559600L;
      long SECONDS_1949_05_28_00 = -649987200L;
      long SECONDS_1949_05_01_01 = -652316400L;
      long SECONDS_1948_10_01_00 = -670636800L;
      long SECONDS_1948_05_01_01 = -683852400L;
      long SECONDS_1947_11_01_00 = -699580800L;
      long SECONDS_1947_04_15_01 = -716857200L;
      long SECONDS_1946_10_01_00 = -733795200L;
      long SECONDS_1946_05_15_01 = -745801200L;
      long SECONDS_1945_09_02_00 = -767836800L;
      long SECONDS_1942_01_31_01 = -881017200L;
      long SECONDS_1941_11_02_00 = -888796800L;
      long SECONDS_1941_03_15_01 = -908838000L;
      long SECONDS_1940_10_13_00 = -922060800L;
      long SECONDS_1940_06_01_01 = -933634800L;
      long SECONDS_1919_10_01_00 = -1585872000L;
      long SECONDS_1919_04_13_01 = -1600642800L;
      long SECONDS_1901_01_01_00 = -2177452800L;
      int OFFSET_0900_TOTAL_SECONDS = 32400;
      int OFFSET_0800_TOTAL_SECONDS = 28800;
      int OFFSET_0543_TOTAL_SECONDS = 29143;
      int zoneOffsetTotalSeconds;
      if (seconds >= SECONDS_1991_09_15_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1991_04_14_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1990_09_16_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1990_04_15_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1989_09_17_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1989_04_16_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1988_09_11_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1988_04_17_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1987_09_13_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1987_04_12_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1986_09_14_02) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1986_05_04_03) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1949_05_28_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1949_05_01_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1948_10_01_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1948_05_01_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1947_11_01_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1947_04_15_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1946_10_01_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1946_05_15_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1945_09_02_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1942_01_31_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1941_11_02_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1941_03_15_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1940_10_13_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1940_06_01_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1919_10_01_00) {
         zoneOffsetTotalSeconds = 28800;
      } else if (seconds >= SECONDS_1919_04_13_01) {
         zoneOffsetTotalSeconds = 32400;
      } else if (seconds >= SECONDS_1901_01_01_00) {
         zoneOffsetTotalSeconds = 28800;
      } else {
         zoneOffsetTotalSeconds = 29143;
      }

      return zoneOffsetTotalSeconds;
   }

   public static boolean isLocalDate(String str) {
      if (str != null && !str.isEmpty()) {
         if (str.length() == 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
            char y0 = str.charAt(0);
            char y1 = str.charAt(1);
            char y2 = str.charAt(2);
            char y3 = str.charAt(3);
            char m0 = str.charAt(5);
            char m1 = str.charAt(6);
            char d0 = str.charAt(8);
            char d1 = str.charAt(9);
            int yyyy = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            int mm = (m0 - '0') * 10 + (m1 - '0');
            int dd = (d0 - '0') * 10 + (d1 - '0');
            if (mm > 12) {
               return false;
            } else if (dd <= 28) {
               return true;
            } else {
               int dom = 31;
               switch (mm) {
                  case 2:
                     boolean isLeapYear = (yyyy & 15) == 0 ? (yyyy & 3) == 0 : (yyyy & 3) == 0 && yyyy % 100 != 0;
                     dom = isLeapYear ? 29 : 28;
                  case 3:
                  case 5:
                  case 7:
                  case 8:
                  case 10:
                  default:
                     break;
                  case 4:
                  case 6:
                  case 9:
                  case 11:
                     dom = 30;
               }

               return dd <= dom;
            }
         } else if (str.length() >= 9 && str.length() <= 40) {
            try {
               return parseLocalDate(str) != null;
            } catch (JSONException | DateTimeException var14) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isDate(String str) {
      if (str != null && !str.isEmpty()) {
         char c10;
         if (str.length() == 19
            && str.charAt(4) == '-'
            && str.charAt(7) == '-'
            && ((c10 = str.charAt(10)) == ' ' || c10 == 'T')
            && str.charAt(13) == ':'
            && str.charAt(16) == ':') {
            char y0 = str.charAt(0);
            char y1 = str.charAt(1);
            char y2 = str.charAt(2);
            char y3 = str.charAt(3);
            char m0 = str.charAt(5);
            char m1 = str.charAt(6);
            char d0 = str.charAt(8);
            char d1 = str.charAt(9);
            char h0 = str.charAt(11);
            char h1 = str.charAt(12);
            char i0 = str.charAt(14);
            char i1 = str.charAt(15);
            char s0 = str.charAt(17);
            char s1 = str.charAt(18);
            if (y0 >= '0'
               && y0 <= '9'
               && y1 >= '0'
               && y1 <= '9'
               && y2 >= '0'
               && y2 <= '9'
               && y3 >= '0'
               && y3 <= '9'
               && m0 >= '0'
               && m0 <= '9'
               && m1 >= '0'
               && m1 <= '9'
               && d0 >= '0'
               && d0 <= '9'
               && d1 >= '0'
               && d1 <= '9'
               && h0 >= '0'
               && h0 <= '9'
               && h1 >= '0'
               && h1 <= '9'
               && i0 >= '0'
               && i0 <= '9'
               && i1 >= '0'
               && i1 <= '9'
               && s0 >= '0'
               && s0 <= '9'
               && s1 >= '0'
               && s1 <= '9') {
               int yyyy = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
               int mm = (m0 - '0') * 10 + (m1 - '0');
               int dd = (d0 - '0') * 10 + (d1 - '0');
               int hh = (h0 - '0') * 10 + (h1 - '0');
               int ii = (i0 - '0') * 10 + (i1 - '0');
               int ss = (s0 - '0') * 10 + (s1 - '0');
               if (mm > 12) {
                  return false;
               } else {
                  if (dd > 28) {
                     int dom = 31;
                     switch (mm) {
                        case 2:
                           boolean isLeapYear = (yyyy & 15) == 0 ? (yyyy & 3) == 0 : (yyyy & 3) == 0 && yyyy % 100 != 0;
                           dom = isLeapYear ? 29 : 28;
                        case 3:
                        case 5:
                        case 7:
                        case 8:
                        case 10:
                        default:
                           break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                           dom = 30;
                     }

                     if (dd > dom) {
                        return false;
                     }
                  }

                  if (hh > 24) {
                     return false;
                  } else {
                     return ii > 60 ? false : ss <= 61;
                  }
               }
            } else {
               return false;
            }
         } else {
            try {
               return parseMillis(str, DEFAULT_ZONE_ID) != 0L;
            } catch (JSONException | DateTimeException var24) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isLocalTime(String str) {
      if (str != null && !str.isEmpty()) {
         if (str.length() == 8 && str.charAt(2) == ':' && str.charAt(5) == ':') {
            char h0 = str.charAt(0);
            char h1 = str.charAt(1);
            char m0 = str.charAt(3);
            char m1 = str.charAt(4);
            char s0 = str.charAt(6);
            char s1 = str.charAt(7);
            if (h0 >= '0'
               && h0 <= '2'
               && h1 >= '0'
               && h1 <= '9'
               && m0 >= '0'
               && m0 <= '6'
               && m1 >= '0'
               && m1 <= '9'
               && s0 >= '0'
               && s0 <= '6'
               && s1 >= '0'
               && s1 <= '9') {
               int hh = (h0 - '0') * 10 + (h1 - '0');
               if (hh > 24) {
                  return false;
               } else {
                  int mm = (m0 - '0') * 10 + (m1 - '0');
                  if (mm > 60) {
                     return false;
                  } else {
                     int ss = (s0 - '0') * 10 + (s1 - '0');
                     return ss <= 61;
                  }
               }
            } else {
               return false;
            }
         } else {
            try {
               LocalTime.parse(str);
               return true;
            } catch (DateTimeParseException var10) {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static int readNanos(char[] chars, int len, int offset) {
      switch (len) {
         case 1:
            return 100000000 * (chars[offset] - 48);
         case 2:
            return 100000000 * (chars[offset] - 48) + 10000000 * (chars[offset + 1] - 48);
         case 3:
            return 100000000 * (chars[offset] - 48) + 10000000 * (chars[offset + 1] - 48) + 1000000 * (chars[offset + 2] - 48);
         case 4:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48);
         case 5:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48)
               + 10000 * (chars[offset + 4] - 48);
         case 6:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48)
               + 10000 * (chars[offset + 4] - 48)
               + 1000 * (chars[offset + 5] - 48);
         case 7:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48)
               + 10000 * (chars[offset + 4] - 48)
               + 1000 * (chars[offset + 5] - 48)
               + 100 * (chars[offset + 6] - 48);
         case 8:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48)
               + 10000 * (chars[offset + 4] - 48)
               + 1000 * (chars[offset + 5] - 48)
               + 100 * (chars[offset + 6] - 48)
               + 10 * (chars[offset + 7] - 48);
         default:
            return 100000000 * (chars[offset] - 48)
               + 10000000 * (chars[offset + 1] - 48)
               + 1000000 * (chars[offset + 2] - 48)
               + 100000 * (chars[offset + 3] - 48)
               + 10000 * (chars[offset + 4] - 48)
               + 1000 * (chars[offset + 5] - 48)
               + 100 * (chars[offset + 6] - 48)
               + 10 * (chars[offset + 7] - 48)
               + chars[offset + 8]
               - 48;
      }
   }

   public static int readNanos(byte[] bytes, int len, int offset) {
      switch (len) {
         case 1:
            return 100000000 * (bytes[offset] - 48);
         case 2:
            return 100000000 * (bytes[offset] - 48) + 10000000 * (bytes[offset + 1] - 48);
         case 3:
            return 100000000 * (bytes[offset] - 48) + 10000000 * (bytes[offset + 1] - 48) + 1000000 * (bytes[offset + 2] - 48);
         case 4:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48);
         case 5:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48)
               + 10000 * (bytes[offset + 4] - 48);
         case 6:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48)
               + 10000 * (bytes[offset + 4] - 48)
               + 1000 * (bytes[offset + 5] - 48);
         case 7:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48)
               + 10000 * (bytes[offset + 4] - 48)
               + 1000 * (bytes[offset + 5] - 48)
               + 100 * (bytes[offset + 6] - 48);
         case 8:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48)
               + 10000 * (bytes[offset + 4] - 48)
               + 1000 * (bytes[offset + 5] - 48)
               + 100 * (bytes[offset + 6] - 48)
               + 10 * (bytes[offset + 7] - 48);
         default:
            return 100000000 * (bytes[offset] - 48)
               + 10000000 * (bytes[offset + 1] - 48)
               + 1000000 * (bytes[offset + 2] - 48)
               + 100000 * (bytes[offset + 3] - 48)
               + 10000 * (bytes[offset + 4] - 48)
               + 1000 * (bytes[offset + 5] - 48)
               + 100 * (bytes[offset + 6] - 48)
               + 10 * (bytes[offset + 7] - 48)
               + bytes[offset + 8]
               - 48;
      }
   }

   static {
      long timeMillis = System.currentTimeMillis();
      ZoneId zoneId = DEFAULT_ZONE_ID;
      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(timeMillis, 1000L);
      int offsetTotalSeconds;
      if (zoneId != SHANGHAI_ZONE_ID && zoneId.getRules() != SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(timeMillis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      LOCAL_EPOCH_DAY = (int)Math.floorDiv(localSecond, 86400L);
   }

   static class CacheDate10 {
      static final String[] CACHE = new String[1024];
   }

   static class CacheDate8 {
      static final String[] CACHE = new String[1024];
   }

   public static enum DateTimeFormatPattern {
      DATE_FORMAT_10_DASH("yyyy-MM-dd", 10),
      DATE_FORMAT_10_SLASH("yyyy/MM/dd", 10),
      DATE_FORMAT_10_DOT("dd.MM.yyyy", 10),
      DATE_TIME_FORMAT_19_DASH("yyyy-MM-dd HH:mm:ss", 19),
      DATE_TIME_FORMAT_19_DASH_T("yyyy-MM-dd'T'HH:mm:ss", 19),
      DATE_TIME_FORMAT_19_SLASH("yyyy/MM/dd HH:mm:ss", 19),
      DATE_TIME_FORMAT_19_DOT("dd.MM.yyyy HH:mm:ss", 19);

      public final String pattern;
      public final int length;

      private DateTimeFormatPattern(String pattern, int length) {
         this.pattern = pattern;
         this.length = length;
      }
   }
}
