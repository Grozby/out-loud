package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.util.DateUtils;
import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectWriterImplInstant extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplInstant INSTANCE = new ObjectWriterImplInstant(null, null);

   public ObjectWriterImplInstant(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      jsonWriter.writeInstant((Instant)object);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context context = jsonWriter.context;
         String dateFormat = this.format != null ? this.format : context.getDateFormat();
         Instant instant = (Instant)object;
         if (dateFormat == null) {
            jsonWriter.writeInstant(instant);
         } else {
            boolean yyyyMMddhhmmss19 = this.yyyyMMddhhmmss19 || context.isFormatyyyyMMddhhmmss19() && this.format == null;
            if (!this.yyyyMMddhhmmss14 && !yyyyMMddhhmmss19 && !this.yyyyMMdd8 && !this.yyyyMMdd10) {
               ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, context.getZoneId());
               if (!this.formatUnixTime && (this.format != null || !context.isDateFormatUnixTime())) {
                  if (!this.formatMillis && (this.format != null || !context.isDateFormatMillis())) {
                     int year = zdt.getYear();
                     if (year < 0 || year > 9999 || !this.formatISO8601 && (this.format != null || !context.isDateFormatISO8601())) {
                        DateTimeFormatter formatter = this.getDateFormatter();
                        if (formatter == null) {
                           formatter = context.getDateFormatter();
                        }

                        if (formatter == null) {
                           jsonWriter.writeZonedDateTime(zdt);
                        } else {
                           String str = formatter.format(zdt);
                           jsonWriter.writeString(str);
                        }
                     } else {
                        jsonWriter.writeDateTimeISO8601(
                           year,
                           zdt.getMonthValue(),
                           zdt.getDayOfMonth(),
                           zdt.getHour(),
                           zdt.getMinute(),
                           zdt.getSecond(),
                           zdt.getNano() / 1000000,
                           zdt.getOffset().getTotalSeconds(),
                           true
                        );
                     }
                  } else {
                     jsonWriter.writeInt64(zdt.toInstant().toEpochMilli());
                  }
               } else {
                  long millis = zdt.toInstant().toEpochMilli();
                  jsonWriter.writeInt64(millis / 1000L);
               }
            } else {
               int SECONDS_PER_DAY = 86400;
               ZoneId zoneId = context.getZoneId();
               long epochSecond = instant.getEpochSecond();
               int offsetTotalSeconds;
               if (zoneId != DateUtils.SHANGHAI_ZONE_ID && zoneId.getRules() != DateUtils.SHANGHAI_ZONE_RULES) {
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
                     if (yyyyMMddhhmmss19) {
                        jsonWriter.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
                     } else if (this.yyyyMMddhhmmss14) {
                        jsonWriter.writeDateTime14(year, month, dayOfMonth, hours, minutes, second);
                     } else if (this.yyyyMMdd10) {
                        jsonWriter.writeDateYYYMMDD10(year, month, dayOfMonth);
                     } else {
                        jsonWriter.writeDateYYYMMDD8(year, month, dayOfMonth);
                     }
                  } else {
                     throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
                  }
               } else {
                  throw new DateTimeException("Invalid year " + yearEst);
               }
            }
         }
      }
   }
}
