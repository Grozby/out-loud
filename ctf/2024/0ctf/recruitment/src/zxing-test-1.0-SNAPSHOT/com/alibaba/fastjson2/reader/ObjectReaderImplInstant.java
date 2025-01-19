package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
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
import java.util.Map;

public final class ObjectReaderImplInstant extends DateTimeCodec implements ObjectReader {
   public static final ObjectReaderImplInstant INSTANCE = new ObjectReaderImplInstant(null, null);

   public static ObjectReaderImplInstant of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplInstant(format, locale);
   }

   ObjectReaderImplInstant(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return Instant.class;
   }

   @Override
   public Object createInstance(Map map, long features) {
      Number nano = (Number)map.get("nano");
      Number epochSecond = (Number)map.get("epochSecond");
      if (nano != null && epochSecond != null) {
         return Instant.ofEpochSecond(epochSecond.longValue(), nano.longValue());
      } else if (epochSecond != null) {
         return Instant.ofEpochSecond(epochSecond.longValue());
      } else {
         Number epochMilli = (Number)map.get("epochMilli");
         if (epochMilli != null) {
            return Instant.ofEpochMilli(epochMilli.longValue());
         } else {
            throw new JSONException("can not create instant.");
         }
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readInstant();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      JSONReader.Context context = jsonReader.getContext();
      if (jsonReader.isInt() && context.getDateFormat() == null) {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         return Instant.ofEpochMilli(millis);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else if (this.format != null && !this.yyyyMMddhhmmss19 && !this.formatISO8601 && !jsonReader.isObject()) {
         String str = jsonReader.readString();
         if (str.isEmpty()) {
            return null;
         } else if (!this.formatMillis && !this.formatUnixTime) {
            DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
            if (!this.formatHasHour) {
               return ZonedDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN, context.getZoneId()).toInstant();
            } else if (!this.formatHasDay) {
               return ZonedDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter), context.getZoneId()).toInstant();
            } else {
               LocalDateTime localDateTime = LocalDateTime.parse(str, formatter);
               return ZonedDateTime.of(localDateTime, context.getZoneId()).toInstant();
            }
         } else {
            long millis = Long.parseLong(str);
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            return Instant.ofEpochMilli(millis);
         }
      } else {
         return jsonReader.readInstant();
      }
   }
}
