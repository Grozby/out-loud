package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

class ObjectReaderImplZonedDateTime extends DateTimeCodec implements ObjectReader {
   static final ObjectReaderImplZonedDateTime INSTANCE = new ObjectReaderImplZonedDateTime(null, null);
   private Function builder;

   public static ObjectReaderImplZonedDateTime of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplZonedDateTime(format, locale);
   }

   public ObjectReaderImplZonedDateTime(Function builder) {
      super(null, null);
      this.builder = builder;
   }

   public ObjectReaderImplZonedDateTime(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return ZonedDateTime.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      ZonedDateTime zdt = jsonReader.readZonedDateTime();
      return this.builder != null && zdt != null ? this.builder.apply(zdt) : zdt;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      JSONReader.Context context = jsonReader.getContext();
      ZonedDateTime zdt;
      if (jsonReader.isInt()) {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         zdt = ZonedDateTime.ofInstant(instant, context.getZoneId());
      } else if (jsonReader.readIfNull()) {
         zdt = null;
      } else if (this.format != null && !this.yyyyMMddhhmmss19 && !this.formatISO8601) {
         String str = jsonReader.readString();
         if (!this.formatMillis && !this.formatUnixTime) {
            DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
            if (!this.formatHasHour) {
               zdt = ZonedDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN, context.getZoneId());
            } else if (!this.formatHasDay) {
               zdt = ZonedDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter), context.getZoneId());
            } else {
               LocalDateTime localDateTime = LocalDateTime.parse(str, formatter);
               zdt = ZonedDateTime.of(localDateTime, context.getZoneId());
            }
         } else {
            long millis = Long.parseLong(str);
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            Instant instant = Instant.ofEpochMilli(millis);
            zdt = ZonedDateTime.ofInstant(instant, context.getZoneId());
         }
      } else {
         zdt = jsonReader.readZonedDateTime();
      }

      return this.builder != null && zdt != null ? this.builder.apply(zdt) : zdt;
   }
}
