package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.DateUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

abstract class FieldWriterDate<T> extends FieldWriter<T> {
   protected DateTimeFormatter formatter;
   final boolean formatMillis;
   final boolean formatISO8601;
   final boolean formatyyyyMMdd8;
   final boolean formatyyyyMMddhhmmss14;
   final boolean formatyyyyMMddhhmmss19;
   final boolean formatUnixTime;
   protected ObjectWriter dateWriter;

   protected FieldWriterDate(
      String fieldName, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(fieldName, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      boolean formatMillis = false;
      boolean formatISO8601 = false;
      boolean formatUnixTime = false;
      boolean formatyyyyMMdd8 = false;
      boolean formatyyyyMMddhhmmss14 = false;
      boolean formatyyyyMMddhhmmss19 = false;
      if (format != null) {
         switch (format) {
            case "millis":
               formatMillis = true;
               break;
            case "iso8601":
               formatISO8601 = true;
               break;
            case "unixtime":
               formatUnixTime = true;
               break;
            case "yyyy-MM-dd HH:mm:ss":
               formatyyyyMMddhhmmss19 = true;
               break;
            case "yyyyMMdd":
               formatyyyyMMdd8 = true;
               break;
            case "yyyyMMddHHmmss":
               formatyyyyMMddhhmmss14 = true;
         }
      }

      this.formatMillis = formatMillis;
      this.formatISO8601 = formatISO8601;
      this.formatUnixTime = formatUnixTime;
      this.formatyyyyMMdd8 = formatyyyyMMdd8;
      this.formatyyyyMMddhhmmss14 = formatyyyyMMddhhmmss14;
      this.formatyyyyMMddhhmmss19 = formatyyyyMMddhhmmss19;
   }

   @Override
   public boolean isDateFormatMillis() {
      return this.formatMillis;
   }

   @Override
   public boolean isDateFormatISO8601() {
      return this.formatISO8601;
   }

   public DateTimeFormatter getFormatter() {
      if (this.formatter == null && this.format != null && !this.formatMillis && !this.formatISO8601 && !this.formatUnixTime) {
         this.formatter = DateTimeFormatter.ofPattern(this.format);
      }

      return this.formatter;
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      if (valueClass == this.fieldClass) {
         ObjectWriterProvider provider = jsonWriter.context.provider;
         if (this.dateWriter == null) {
            if ((provider.userDefineMask & 16L) == 0L) {
               if (this.format == null) {
                  return this.dateWriter = ObjectWriterImplDate.INSTANCE;
               }

               return this.dateWriter = new ObjectWriterImplDate(this.format, null);
            }

            this.dateWriter = provider.getObjectWriter(valueClass, valueClass, false);
         }

         return this.dateWriter;
      } else {
         return jsonWriter.getObjectWriter(valueClass);
      }
   }

   @Override
   public void writeDate(JSONWriter jsonWriter, long timeMillis) {
      if (jsonWriter.jsonb) {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeMillis(timeMillis);
      } else {
         int SECONDS_PER_DAY = 86400;
         JSONWriter.Context ctx = jsonWriter.context;
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
               ZoneId zoneId = ctx.getZoneId();
               String dateFormat = this.format != null ? this.format : ctx.getDateFormat();
               boolean formatyyyyMMddhhmmss19 = this.formatyyyyMMddhhmmss19 || ctx.isFormatyyyyMMddhhmmss19() && this.format == null;
               if (dateFormat == null || this.formatyyyyMMddhhmmss14 || formatyyyyMMddhhmmss19) {
                  long epochSecond = Math.floorDiv(timeMillis, 1000L);
                  int offsetTotalSeconds;
                  if (zoneId != DateUtils.SHANGHAI_ZONE_ID && zoneId.getRules() != DateUtils.SHANGHAI_ZONE_RULES) {
                     Instant instant = Instant.ofEpochMilli(timeMillis);
                     offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
                  } else {
                     offsetTotalSeconds = DateUtils.getShanghaiZoneOffsetTotalSeconds(epochSecond);
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
                  if (yearEst < -999999999L || yearEst > 999999999L) {
                     throw new DateTimeException("Invalid year " + yearEst);
                  }

                  int year = (int)yearEst;
                  int MINUTES_PER_HOUR = 60;
                  int SECONDS_PER_MINUTE = 60;
                  int SECONDS_PER_HOUR = 3600;
                  long secondOfDay = (long)secsOfDay;
                  if (secondOfDay < 0L || secondOfDay > 86399L) {
                     throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
                  }

                  int hours = (int)(secondOfDay / 3600L);
                  secondOfDay -= (long)(hours * 3600);
                  int minutes = (int)(secondOfDay / 60L);
                  secondOfDay -= (long)(minutes * 60);
                  int second = (int)secondOfDay;
                  if (year >= 0 && year <= 9999) {
                     if (this.formatyyyyMMddhhmmss14) {
                        this.writeFieldName(jsonWriter);
                        jsonWriter.writeDateTime14(year, month, dayOfMonth, hours, minutes, second);
                        return;
                     }

                     if (formatyyyyMMddhhmmss19) {
                        this.writeFieldName(jsonWriter);
                        jsonWriter.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
                        return;
                     }

                     int millis = (int)Math.floorMod(timeMillis, 1000L);
                     if (millis != 0) {
                        Instant instant = Instant.ofEpochMilli(timeMillis);
                        int offsetSeconds = ctx.getZoneId().getRules().getOffset(instant).getTotalSeconds();
                        this.writeFieldName(jsonWriter);
                        jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hours, minutes, second, millis, offsetSeconds, false);
                        return;
                     }

                     this.writeFieldName(jsonWriter);
                     jsonWriter.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
                     return;
                  }
               }

               this.writeFieldName(jsonWriter);
               ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), zoneId);
               if (this.formatISO8601 || ctx.isDateFormatISO8601() && this.format == null) {
                  int yearx = zdt.getYear();
                  if (yearx >= 0 && yearx <= 9999) {
                     int monthx = zdt.getMonthValue();
                     int dayOfMonthx = zdt.getDayOfMonth();
                     int hour = zdt.getHour();
                     int minute = zdt.getMinute();
                     int second = zdt.getSecond();
                     int millis = zdt.getNano() / 1000000;
                     int offsetSeconds = zdt.getOffset().getTotalSeconds();
                     jsonWriter.writeDateTimeISO8601(yearx, monthx, dayOfMonthx, hour, minute, second, millis, offsetSeconds, true);
                     return;
                  }
               }

               if (this.formatyyyyMMdd8) {
                  int yearx = zdt.getYear();
                  if (yearx >= 0 && yearx <= 9999) {
                     int monthx = zdt.getMonthValue();
                     int dayOfMonthx = zdt.getDayOfMonth();
                     jsonWriter.writeDateYYYMMDD8(yearx, monthx, dayOfMonthx);
                     return;
                  }
               }

               DateTimeFormatter formatter = this.getFormatter();
               if (formatter == null) {
                  formatter = ctx.getDateFormatter();
               }

               if (formatter != null) {
                  jsonWriter.writeString(formatter.format(zdt));
               } else {
                  jsonWriter.writeZonedDateTime(zdt);
               }
            } else {
               this.writeFieldName(jsonWriter);
               jsonWriter.writeInt64(timeMillis);
            }
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeInt64(timeMillis / 1000L);
         }
      }
   }
}
