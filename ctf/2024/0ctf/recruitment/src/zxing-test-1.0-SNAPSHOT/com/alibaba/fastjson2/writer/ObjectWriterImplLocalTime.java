package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectWriterImplLocalTime extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplLocalTime INSTANCE = new ObjectWriterImplLocalTime(null, null);

   public ObjectWriterImplLocalTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      jsonWriter.writeLocalTime((LocalTime)object);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         LocalTime time = (LocalTime)object;
         if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
            if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter == null) {
                  formatter = ctx.getDateFormatter();
               }

               if (formatter == null) {
                  int hour = time.getHour();
                  int minute = time.getMinute();
                  int second = time.getSecond();
                  int nano = time.getNano();
                  if (nano == 0) {
                     jsonWriter.writeTimeHHMMSS8(hour, minute, second);
                  } else {
                     jsonWriter.writeLocalTime(time);
                  }
               } else {
                  String str;
                  if (!this.formatHasDay && !ctx.isDateFormatHasDay()) {
                     str = formatter.format(time);
                  } else {
                     str = formatter.format(LocalDateTime.of(LocalDate.of(1970, 1, 1), time));
                  }

                  jsonWriter.writeString(str);
               }
            } else {
               LocalDateTime dateTime = LocalDateTime.of(LocalDate.of(1970, 1, 1), time);
               Instant instant = dateTime.atZone(ctx.getZoneId()).toInstant();
               int seconds = (int)(instant.toEpochMilli() / 1000L);
               jsonWriter.writeInt32(seconds);
            }
         } else {
            LocalDateTime dateTime = LocalDateTime.of(LocalDate.of(1970, 1, 1), time);
            Instant instant = dateTime.atZone(ctx.getZoneId()).toInstant();
            long millis = instant.toEpochMilli();
            jsonWriter.writeInt64(millis);
         }
      }
   }
}
