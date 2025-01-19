package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderDate<T> extends FieldReaderDateTimeCodec<T> {
   final BiConsumer<T, Date> function;

   public FieldReaderDate(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Field field,
      Method method,
      BiConsumer<T, Date> function
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, field, ObjectReaderImplDate.of(format, locale));
      this.function = function;
   }

   @Override
   protected void acceptNull(T object) {
      this.accept(object, (Date)null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Date date;
      try {
         date = (Date)this.dateReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
      } catch (Exception var5) {
         if ((this.features & JSONReader.Feature.NullOnError.mask) == 0L) {
            throw var5;
         }

         date = null;
      }

      this.accept(object, date);
   }

   @Override
   protected void accept(T object, Date value) {
      if (this.function != null) {
         this.function.accept(object, value);
      } else if (object == null) {
         throw new JSONException("set " + this.fieldName + " error, object is null");
      } else if (this.method != null) {
         try {
            this.method.invoke(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      } else if (this.fieldOffset != -1L) {
         JDKUtils.UNSAFE.putObject(object, this.fieldOffset, value);
      } else {
         try {
            this.field.set(object, value);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }
      }
   }

   @Override
   protected void accept(T object, Instant instant) {
      Date date = Date.from(instant);
      this.accept(object, date);
   }

   @Override
   public void accept(T object, long value) {
      this.accept(object, new Date(value));
   }

   @Override
   protected void accept(T object, ZonedDateTime zdt) {
      long epochMilli = zdt.toInstant().toEpochMilli();
      Date value = new Date(epochMilli);
      this.accept(object, value);
   }

   @Override
   protected Object apply(LocalDateTime ldt) {
      ZoneOffset offset = DateUtils.DEFAULT_ZONE_ID.getRules().getOffset(ldt);
      Instant instant = ldt.toInstant(offset);
      return Date.from(instant);
   }

   @Override
   protected void accept(T object, LocalDateTime ldt) {
      ZoneOffset offset = DateUtils.DEFAULT_ZONE_ID.getRules().getOffset(ldt);
      Instant instant = ldt.toInstant(offset);
      Date value = Date.from(instant);
      this.accept(object, value);
   }

   @Override
   protected Object apply(Date value) {
      return value;
   }

   @Override
   protected Object apply(Instant instant) {
      return Date.from(instant);
   }

   @Override
   protected Object apply(ZonedDateTime zdt) {
      Instant instant = zdt.toInstant();
      return Date.from(instant);
   }

   @Override
   protected Object apply(long millis) {
      return new Date(millis);
   }
}
