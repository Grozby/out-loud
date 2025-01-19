package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

final class ObjectReaderImplCalendar extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplCalendar INSTANCE = new ObjectReaderImplCalendar(null, null);

   public static ObjectReaderImplCalendar of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplCalendar(format, locale);
   }

   public ObjectReaderImplCalendar(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return Calendar.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.isInt()) {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(millis);
         return calendar;
      } else if (jsonReader.readIfNull()) {
         return null;
      } else {
         long millis = jsonReader.readMillisFromString();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(millis);
         return calendar;
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.isString()) {
         if (this.format != null) {
            DateTimeFormatter formatter = this.getDateFormatter();
            if (formatter != null) {
               String str = jsonReader.readString();
               if (str.isEmpty()) {
                  return null;
               }

               LocalDateTime ldt = LocalDateTime.parse(str, formatter);
               ZonedDateTime zdt = ZonedDateTime.of(ldt, jsonReader.getContext().getZoneId());
               long millis = zdt.toInstant().toEpochMilli();
               Calendar calendar = Calendar.getInstance();
               calendar.setTimeInMillis(millis);
               return calendar;
            }
         }

         long millis = jsonReader.readMillisFromString();
         if (millis == 0L && jsonReader.wasNull()) {
            return null;
         } else {
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            return calendar;
         }
      } else if (jsonReader.readIfNull()) {
         return null;
      } else {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime || jsonReader.getContext().isFormatUnixTime()) {
            millis *= 1000L;
         }

         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(millis);
         return calendar;
      }
   }
}
