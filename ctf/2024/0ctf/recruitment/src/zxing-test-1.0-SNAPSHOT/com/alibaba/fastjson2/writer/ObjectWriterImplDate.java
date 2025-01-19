package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.util.DateUtils;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

final class ObjectWriterImplDate extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplDate INSTANCE = new ObjectWriterImplDate(null, null);
   static final char[] PREFIX_CHARS = "new Date(".toCharArray();
   static final byte[] PREFIX_BYTES = "new Date(".getBytes(StandardCharsets.UTF_8);
   static final char[] PREFIX_CHARS_SQL = "{\"@type\":\"java.sql.Date\",\"val\":".toCharArray();
   static final byte[] PREFIX_BYTES_SQL = "{\"@type\":\"java.sql.Date\",\"val\":".getBytes(StandardCharsets.UTF_8);

   public ObjectWriterImplDate(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         jsonWriter.writeMillis(((Date)object).getTime());
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         Date date = (Date)object;
         long millis = date.getTime();
         if (jsonWriter.isWriteTypeInfo(object, fieldType)) {
            char end = ')';
            if (jsonWriter.utf16) {
               char[] prefix;
               if ("java.sql.Date".equals(date.getClass().getName())) {
                  prefix = PREFIX_CHARS_SQL;
                  end = '}';
               } else {
                  prefix = PREFIX_CHARS;
               }

               jsonWriter.writeRaw(prefix, 0, prefix.length);
            } else {
               byte[] prefix;
               if ("java.sql.Date".equals(date.getClass().getName())) {
                  prefix = PREFIX_BYTES_SQL;
                  end = '}';
               } else {
                  prefix = PREFIX_BYTES;
               }

               jsonWriter.writeRaw(prefix);
            }

            jsonWriter.writeInt64(millis);
            jsonWriter.writeRaw(end);
         } else if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
            if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
               ZoneId zoneId = ctx.getZoneId();
               int offsetSeconds;
               if (zoneId == DateUtils.SHANGHAI_ZONE_ID || zoneId.getRules() == DateUtils.SHANGHAI_ZONE_RULES) {
                  offsetSeconds = DateUtils.getShanghaiZoneOffsetTotalSeconds(Math.floorDiv(millis, 1000L));
               } else if (zoneId != ZoneOffset.UTC && !"UTC".equals(zoneId.getId())) {
                  ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
                  offsetSeconds = zdt.getOffset().getTotalSeconds();
               } else {
                  offsetSeconds = 0;
               }

               boolean formatISO8601 = this.formatISO8601 || ctx.isDateFormatISO8601();
               String dateFormat;
               if (formatISO8601) {
                  dateFormat = null;
               } else {
                  dateFormat = this.format;
                  if (dateFormat == null) {
                     dateFormat = ctx.getDateFormat();
                  }
               }

               if (dateFormat == null) {
                  int SECONDS_PER_DAY = 86400;
                  long epochSecond = Math.floorDiv(millis, 1000L);
                  int offsetTotalSeconds;
                  if (zoneId != DateUtils.SHANGHAI_ZONE_ID && zoneId.getRules() != DateUtils.SHANGHAI_ZONE_RULES) {
                     Instant instant = Instant.ofEpochMilli(millis);
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
                     int mos = (int)Math.floorMod(millis, 1000L);
                     if (mos != 0 || formatISO8601) {
                        jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hours, minutes, second, mos, offsetSeconds, formatISO8601);
                     } else if (hours == 0 && minutes == 0 && second == 0 && "java.sql.Date".equals(date.getClass().getName())) {
                        jsonWriter.writeDateYYYMMDD10(year, month, dayOfMonth);
                     } else {
                        jsonWriter.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
                     }

                     return;
                  }
               }

               DateTimeFormatter formatter;
               if (this.format != null) {
                  formatter = this.getDateFormatter();
               } else {
                  formatter = ctx.getDateFormatter();
               }

               ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
               String str = formatter.format(zdt);
               jsonWriter.writeString(str);
            } else {
               jsonWriter.writeInt64(millis / 1000L);
            }
         } else {
            jsonWriter.writeInt64(millis);
         }
      }
   }
}
