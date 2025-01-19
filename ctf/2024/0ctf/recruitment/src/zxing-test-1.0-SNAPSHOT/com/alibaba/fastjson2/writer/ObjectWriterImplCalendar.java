package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

final class ObjectWriterImplCalendar extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplCalendar INSTANCE = new ObjectWriterImplCalendar(null, null);

   public ObjectWriterImplCalendar(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         long millis = ((Calendar)object).getTimeInMillis();
         jsonWriter.writeMillis(millis);
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         Calendar date = (Calendar)object;
         long millis = date.getTimeInMillis();
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (this.format == null && ctx.isDateFormatMillis()) {
               jsonWriter.writeInt64(millis);
            } else {
               ZoneId zoneId = ctx.getZoneId();
               Instant instant = Instant.ofEpochMilli(millis);
               ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
               int offsetSeconds = zdt.getOffset().getTotalSeconds();
               int year = zdt.getYear();
               if (year >= 0 && year <= 9999) {
                  if (this.format == null && ctx.isDateFormatISO8601()) {
                     int month = zdt.getMonthValue();
                     int dayOfMonth = zdt.getDayOfMonth();
                     int hour = zdt.getHour();
                     int minute = zdt.getMinute();
                     int second = zdt.getSecond();
                     int nano = zdt.getNano() / 1000000;
                     jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, nano, offsetSeconds, true);
                     return;
                  }

                  String dateFormat = this.format == null ? ctx.getDateFormat() : this.format;
                  if (dateFormat == null) {
                     int month = zdt.getMonthValue();
                     int dayOfMonth = zdt.getDayOfMonth();
                     int hour = zdt.getHour();
                     int minute = zdt.getMinute();
                     int second = zdt.getSecond();
                     int nano = zdt.getNano();
                     if (nano == 0) {
                        jsonWriter.writeDateTime19(year, month, dayOfMonth, hour, minute, second);
                     } else {
                        jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, nano / 1000000, offsetSeconds, false);
                     }

                     return;
                  }
               }

               DateTimeFormatter dateFormatter;
               if (this.format != null) {
                  dateFormatter = this.getDateFormatter();
               } else {
                  dateFormatter = ctx.getDateFormatter();
               }

               if (dateFormatter == null) {
                  jsonWriter.writeZonedDateTime(zdt);
               } else {
                  String str = dateFormatter.format(zdt);
                  jsonWriter.writeString(str);
               }
            }
         } else {
            jsonWriter.writeInt64(millis / 1000L);
         }
      }
   }
}
