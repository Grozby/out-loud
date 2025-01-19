package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.IOUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

abstract class FieldReaderDateTimeCodec<T> extends FieldReader<T> {
   final ObjectReader dateReader;
   final boolean formatUnixTime;
   final boolean formatMillis;

   public FieldReaderDateTimeCodec(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      Field field,
      ObjectReader dateReader
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, field);
      this.dateReader = dateReader;
      boolean formatUnixTime = false;
      boolean formatMillis = false;
      boolean hasDay = false;
      boolean hasHour = false;
      if (format != null) {
         switch (format) {
            case "unixtime":
               formatUnixTime = true;
               break;
            case "millis":
               formatMillis = true;
         }
      }

      this.formatUnixTime = formatUnixTime;
      this.formatMillis = formatMillis;
   }

   @Override
   public final Object readFieldValue(JSONReader jsonReader) {
      return this.dateReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
   }

   @Override
   public final ObjectReader getObjectReader(JSONReader jsonReader) {
      return this.dateReader;
   }

   @Override
   public final ObjectReader getObjectReader(JSONReader.Context context) {
      return this.dateReader;
   }

   protected abstract void accept(T var1, Date var2);

   protected abstract void acceptNull(T var1);

   protected abstract void accept(T var1, Instant var2);

   protected abstract void accept(T var1, LocalDateTime var2);

   protected abstract void accept(T var1, ZonedDateTime var2);

   protected abstract Object apply(Date var1);

   protected abstract Object apply(Instant var1);

   protected abstract Object apply(ZonedDateTime var1);

   protected abstract Object apply(LocalDateTime var1);

   protected abstract Object apply(long var1);

   @Override
   public void accept(T object, Object value) {
      if (value == null) {
         this.acceptNull(object);
      } else {
         if (value instanceof String) {
            String str = (String)value;
            if (str.isEmpty() || "null".equals(str)) {
               this.acceptNull(object);
               return;
            }

            if ((this.format == null || this.formatUnixTime || this.formatMillis) && IOUtils.isNumber(str)) {
               long millis = Long.parseLong(str);
               if (this.formatUnixTime) {
                  millis *= 1000L;
               }

               this.accept(object, millis);
               return;
            }

            value = DateUtils.parseDate(str, this.format, DateUtils.DEFAULT_ZONE_ID);
         }

         if (value instanceof Date) {
            this.accept(object, (Date)value);
         } else if (value instanceof Instant) {
            this.accept(object, (Instant)value);
         } else if (value instanceof Long) {
            this.accept(object, ((Long)value).longValue());
         } else if (value instanceof LocalDateTime) {
            this.accept(object, (LocalDateTime)value);
         } else {
            if (!(value instanceof ZonedDateTime)) {
               throw new JSONException("not support value " + value.getClass());
            }

            this.accept(object, (ZonedDateTime)value);
         }
      }
   }

   @Override
   public boolean supportAcceptType(Class valueClass) {
      return valueClass == Date.class || valueClass == String.class;
   }
}
