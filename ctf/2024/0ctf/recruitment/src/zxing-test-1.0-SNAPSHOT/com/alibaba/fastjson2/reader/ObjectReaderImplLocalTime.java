package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class ObjectReaderImplLocalTime extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplLocalTime INSTANCE = new ObjectReaderImplLocalTime(null, null);

   public ObjectReaderImplLocalTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return LocalTime.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readLocalTime();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      JSONReader.Context context = jsonReader.getContext();
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.isInt()) {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         ZoneId zoneId = context.getZoneId();
         return LocalDateTime.ofInstant(instant, zoneId).toLocalTime();
      } else if (this.format == null || jsonReader.isNumber()) {
         return jsonReader.readLocalTime();
      } else if (!this.yyyyMMddhhmmss19 && !this.formatISO8601) {
         String str = jsonReader.readString();
         if (str.isEmpty()) {
            return null;
         } else if (!this.formatMillis && !this.formatUnixTime) {
            DateTimeFormatter formatter = this.getDateFormatter(context.getLocale());
            return this.formatHasDay ? LocalDateTime.parse(str, formatter).toLocalTime() : LocalTime.parse(str, formatter);
         } else {
            long millis = Long.parseLong(str);
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            Instant instant = Instant.ofEpochMilli(millis);
            return LocalDateTime.ofInstant(instant, context.getZoneId()).toLocalTime();
         }
      } else {
         return jsonReader.readLocalDateTime().toLocalTime();
      }
   }
}
