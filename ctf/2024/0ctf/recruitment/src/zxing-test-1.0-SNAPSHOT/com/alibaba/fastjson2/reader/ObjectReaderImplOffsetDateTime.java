package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectReaderImplOffsetDateTime extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplOffsetDateTime INSTANCE = new ObjectReaderImplOffsetDateTime(null, null);

   public static ObjectReaderImplOffsetDateTime of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplOffsetDateTime(format, locale);
   }

   public ObjectReaderImplOffsetDateTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return OffsetDateTime.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readObject(jsonReader, fieldType, fieldName, features);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      JSONReader.Context context = jsonReader.getContext();
      if (!jsonReader.isInt()) {
         if (jsonReader.readIfNull()) {
            return null;
         } else if (this.format != null && !this.yyyyMMddhhmmss19 && !this.formatISO8601) {
            String str = jsonReader.readString();
            ZoneId zoneId = context.getZoneId();
            if (!this.formatMillis && !this.formatUnixTime) {
               DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
               if (!this.formatHasHour) {
                  LocalDateTime ldt = LocalDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN);
                  return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(ldt));
               } else if (!this.formatHasDay) {
                  return ZonedDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter), zoneId).toOffsetDateTime();
               } else {
                  LocalDateTime ldt = LocalDateTime.parse(str, formatter);
                  return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(ldt));
               }
            } else {
               long millis = Long.parseLong(str);
               if (this.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               LocalDateTime ldt = LocalDateTime.ofInstant(instant, zoneId);
               return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(instant));
            }
         } else {
            return jsonReader.readOffsetDateTime();
         }
      } else {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime || context.isFormatUnixTime()) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         ZoneId zoneId = context.getZoneId();
         LocalDateTime ldt = LocalDateTime.ofInstant(instant, zoneId);
         return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(instant));
      }
   }
}
