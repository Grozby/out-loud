package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

final class ObjectWriterImplLocalDateTime extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplLocalDateTime INSTANCE = new ObjectWriterImplLocalDateTime(null, null);

   static ObjectWriterImplLocalDateTime of(String format, Locale locale) {
      return new ObjectWriterImplLocalDateTime(format, locale);
   }

   public ObjectWriterImplLocalDateTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (this.format != null) {
         this.write(jsonWriter, object, fieldName, fieldType, features);
      } else {
         jsonWriter.writeLocalDateTime((LocalDateTime)object);
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         LocalDateTime ldt = (LocalDateTime)object;
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
               int year = ldt.getYear();
               if (year >= 0 && year <= 9999) {
                  if (this.formatISO8601 || this.format == null && ctx.isDateFormatISO8601()) {
                     int month = ldt.getMonthValue();
                     int dayOfMonth = ldt.getDayOfMonth();
                     int hour = ldt.getHour();
                     int minute = ldt.getMinute();
                     int second = ldt.getSecond();
                     int nano = ldt.getNano() / 1000000;
                     int offsetSeconds = ctx.getZoneId().getRules().getOffset(ldt).getTotalSeconds();
                     jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, nano, offsetSeconds, true);
                     return;
                  }

                  if (this.yyyyMMddhhmmss19) {
                     jsonWriter.writeDateTime19(year, ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond());
                     return;
                  }

                  if (this.yyyyMMddhhmmss14) {
                     jsonWriter.writeDateTime14(year, ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond());
                     return;
                  }

                  if (this.yyyyMMdd8) {
                     jsonWriter.writeDateYYYMMDD8(year, ldt.getMonthValue(), ldt.getDayOfMonth());
                     return;
                  }

                  if (this.yyyyMMdd10) {
                     jsonWriter.writeDateYYYMMDD10(year, ldt.getMonthValue(), ldt.getDayOfMonth());
                     return;
                  }
               }

               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter == null) {
                  formatter = ctx.getDateFormatter();
               }

               if (formatter == null) {
                  jsonWriter.writeLocalDateTime(ldt);
               } else {
                  String str;
                  if (this.useSimpleDateFormat) {
                     Instant instant = ldt.toInstant(jsonWriter.context.getZoneId().getRules().getOffset(ldt));
                     Date date = new Date(instant.toEpochMilli());
                     str = new SimpleDateFormat(this.format).format(date);
                  } else if (this.locale != null) {
                     ZonedDateTime zdt = ZonedDateTime.of(ldt, jsonWriter.context.getZoneId());
                     str = formatter.format(zdt);
                  } else {
                     str = formatter.format(ldt);
                  }

                  jsonWriter.writeString(str);
               }
            } else {
               long millis = ldt.atZone(ctx.getZoneId()).toInstant().toEpochMilli();
               jsonWriter.writeInt64(millis);
            }
         } else {
            long millis = ldt.atZone(ctx.getZoneId()).toInstant().toEpochMilli();
            jsonWriter.writeInt64(millis / 1000L);
         }
      }
   }
}
