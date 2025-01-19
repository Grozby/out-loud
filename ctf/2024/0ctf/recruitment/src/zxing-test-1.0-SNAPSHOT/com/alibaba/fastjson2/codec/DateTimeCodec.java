package com.alibaba.fastjson2.codec;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract class DateTimeCodec {
   public final String format;
   public final boolean formatUnixTime;
   public final boolean formatMillis;
   public final boolean formatISO8601;
   protected final boolean formatHasDay;
   protected final boolean formatHasHour;
   public final boolean useSimpleFormatter;
   public final Locale locale;
   protected final boolean yyyyMMddhhmmss19;
   protected final boolean yyyyMMddhhmm16;
   protected final boolean yyyyMMddhhmmss14;
   protected final boolean yyyyMMdd10;
   protected final boolean yyyyMMdd8;
   protected final boolean useSimpleDateFormat;
   DateTimeFormatter dateFormatter;

   public DateTimeCodec(String format) {
      this(format, null);
   }

   public DateTimeCodec(String format, Locale locale) {
      if (format != null) {
         format = format.replaceAll("aa", "a");
      }

      this.format = format;
      this.locale = locale;
      this.yyyyMMddhhmmss14 = "yyyyMMddHHmmss".equals(format);
      this.yyyyMMddhhmmss19 = "yyyy-MM-dd HH:mm:ss".equals(format);
      this.yyyyMMddhhmm16 = "yyyy-MM-dd HH:mm".equals(format);
      this.yyyyMMdd10 = "yyyy-MM-dd".equals(format);
      this.yyyyMMdd8 = "yyyyMMdd".equals(format);
      this.useSimpleDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX".equals(format);
      boolean formatUnixTime = false;
      boolean formatISO8601 = false;
      boolean formatMillis = false;
      boolean hasDay = false;
      boolean hasHour = false;
      if (format != null) {
         switch (format) {
            case "unixtime":
               formatUnixTime = true;
               break;
            case "iso8601":
               formatISO8601 = true;
               break;
            case "millis":
               formatMillis = true;
               break;
            default:
               hasDay = format.indexOf(100) != -1;
               hasHour = format.indexOf(72) != -1 || format.indexOf(104) != -1 || format.indexOf(75) != -1 || format.indexOf(107) != -1;
         }
      }

      this.formatUnixTime = formatUnixTime;
      this.formatMillis = formatMillis;
      this.formatISO8601 = formatISO8601;
      this.formatHasDay = hasDay;
      this.formatHasHour = hasHour;
      this.useSimpleFormatter = "yyyyMMddHHmmssSSSZ".equals(format);
   }

   public DateTimeFormatter getDateFormatter() {
      if (this.dateFormatter == null && this.format != null && !this.formatMillis && !this.formatISO8601 && !this.formatUnixTime) {
         if (this.locale == null) {
            this.dateFormatter = DateTimeFormatter.ofPattern(this.format);
         } else {
            this.dateFormatter = DateTimeFormatter.ofPattern(this.format, this.locale);
         }
      }

      return this.dateFormatter;
   }

   public DateTimeFormatter getDateFormatter(Locale locale) {
      if (this.format != null && !this.formatMillis && !this.formatISO8601 && !this.formatUnixTime) {
         if (this.dateFormatter == null
            || (this.locale != null || locale != null && locale != Locale.getDefault()) && (this.locale == null || !this.locale.equals(locale))) {
            if (locale != null) {
               return this.dateFormatter = DateTimeFormatter.ofPattern(this.format, locale);
            } else {
               return this.locale == null
                  ? (this.dateFormatter = DateTimeFormatter.ofPattern(this.format))
                  : (this.dateFormatter = DateTimeFormatter.ofPattern(this.format, this.locale));
            }
         } else {
            return this.dateFormatter;
         }
      } else {
         return null;
      }
   }
}
