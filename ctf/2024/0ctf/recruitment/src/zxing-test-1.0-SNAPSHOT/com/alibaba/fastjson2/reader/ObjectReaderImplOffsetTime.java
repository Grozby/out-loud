package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectReaderImplOffsetTime extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplOffsetTime INSTANCE = new ObjectReaderImplOffsetTime(null, null);

   public static ObjectReaderImplOffsetTime of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplOffsetTime(format, locale);
   }

   public ObjectReaderImplOffsetTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return OffsetTime.class;
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
         } else if (this.format == null) {
            return jsonReader.readOffsetTime();
         } else {
            String str = jsonReader.readString();
            ZoneId zoneId = context.getZoneId();
            if (!this.formatMillis && !this.formatUnixTime) {
               DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
               if (!this.formatHasHour) {
                  LocalDateTime ldt = LocalDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN);
                  return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(ldt)).toOffsetTime();
               } else if (!this.formatHasDay) {
                  ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter), zoneId);
                  return zdt.toOffsetDateTime().toOffsetTime();
               } else {
                  LocalDateTime ldt = LocalDateTime.parse(str, formatter);
                  OffsetDateTime odt = OffsetDateTime.of(ldt, zoneId.getRules().getOffset(ldt));
                  return odt.toOffsetTime();
               }
            } else {
               long millis = Long.parseLong(str);
               if (this.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               LocalDateTime ldt = LocalDateTime.ofInstant(instant, zoneId);
               return OffsetDateTime.of(ldt, zoneId.getRules().getOffset(instant)).toOffsetTime();
            }
         }
      } else {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime || context.isFormatUnixTime()) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         ZoneId zoneId = context.getZoneId();
         LocalDateTime ldt = LocalDateTime.ofInstant(instant, zoneId);
         return OffsetTime.of(ldt.toLocalTime(), zoneId.getRules().getOffset(instant));
      }
   }
}
