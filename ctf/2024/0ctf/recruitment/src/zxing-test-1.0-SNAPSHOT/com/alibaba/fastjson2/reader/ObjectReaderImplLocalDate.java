package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class ObjectReaderImplLocalDate extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplLocalDate INSTANCE = new ObjectReaderImplLocalDate(null, null);

   public ObjectReaderImplLocalDate(String format, Locale locale) {
      super(format, locale);
   }

   public static ObjectReaderImplLocalDate of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplLocalDate(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return LocalDate.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readLocalDate();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         JSONReader.Context context = jsonReader.getContext();
         if (jsonReader.readIfNull()) {
            return null;
         } else if (this.format != null && !this.yyyyMMddhhmmss19 && !this.formatISO8601 && !jsonReader.isNumber()) {
            String str = jsonReader.readString();
            if (str.isEmpty() || "null".equals(str)) {
               return null;
            } else if (!this.formatMillis && !this.formatUnixTime) {
               DateTimeFormatter formatter = this.getDateFormatter(context.getLocale());
               if (!this.formatHasHour) {
                  return LocalDate.parse(str, formatter);
               } else {
                  return !this.formatHasDay ? LocalDate.of(1970, 1, 1) : LocalDateTime.parse(str, formatter).toLocalDate();
               }
            } else {
               long millis = Long.parseLong(str);
               if (this.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               return LocalDateTime.ofInstant(instant, context.getZoneId()).toLocalDate();
            }
         } else {
            return jsonReader.readLocalDate();
         }
      }
   }
}
