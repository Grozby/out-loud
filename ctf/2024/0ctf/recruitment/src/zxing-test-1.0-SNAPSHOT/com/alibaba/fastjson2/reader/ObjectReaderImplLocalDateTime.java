package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class ObjectReaderImplLocalDateTime extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplLocalDateTime INSTANCE = new ObjectReaderImplLocalDateTime(null, null);

   public ObjectReaderImplLocalDateTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return LocalDateTime.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readLocalDateTime();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         JSONReader.Context context = jsonReader.getContext();
         if (jsonReader.isInt()) {
            if (!this.yyyyMMddhhmmss19 && !this.formatMillis && !this.formatISO8601 && !this.formatUnixTime) {
               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter != null) {
                  String str = jsonReader.readString();
                  return LocalDateTime.parse(str, formatter);
               }
            }

            long millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            Instant instant = Instant.ofEpochMilli(millis);
            ZoneId zoneId = context.getZoneId();
            return LocalDateTime.ofInstant(instant, zoneId);
         } else if (jsonReader.readIfNull()) {
            return null;
         } else if (this.format != null && !this.yyyyMMdd8 && !this.yyyyMMdd10 && !this.yyyyMMddhhmmss19 && !this.formatISO8601) {
            String str = jsonReader.readString();
            if (str.isEmpty()) {
               return null;
            } else if (!this.formatMillis && !this.formatUnixTime) {
               DateTimeFormatter formatter = this.getDateFormatter(context.getLocale());
               if (!this.formatHasHour) {
                  return LocalDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN);
               } else {
                  return !this.formatHasDay ? LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter)) : LocalDateTime.parse(str, formatter);
               }
            } else {
               long millis = Long.parseLong(str);
               if (this.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               return LocalDateTime.ofInstant(instant, context.getZoneId());
            }
         } else {
            return jsonReader.readLocalDateTime();
         }
      }
   }
}
