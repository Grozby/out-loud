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
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiConsumer;

public final class FieldReaderLocalDateTime<T> extends FieldReaderDateTimeCodec<T> {
   final BiConsumer<T, ZonedDateTime> function;

   FieldReaderLocalDateTime(
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
      BiConsumer<T, ZonedDateTime> function
   ) {
      super(
         fieldName,
         fieldType,
         fieldClass,
         ordinal,
         features,
         format,
         locale,
         defaultValue,
         schema,
         method,
         field,
         format != null ? new ObjectReaderImplLocalDateTime(format, locale) : ObjectReaderImplLocalDateTime.INSTANCE
      );
      this.function = function;
   }

   @Override
   public boolean supportAcceptType(Class valueClass) {
      return this.fieldClass == Instant.class || this.fieldClass == Long.class;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, Object object) {
      LocalDateTime date = null;
      if (jsonReader.jsonb) {
         date = (LocalDateTime)this.dateReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
      } else {
         date = (LocalDateTime)this.dateReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
      }

      this.accept(object, date);
   }

   @Override
   public void accept(Object object, long value) {
      Instant instant = Instant.ofEpochMilli(value);
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      LocalDateTime ldt = zdt.toLocalDateTime();
      this.accept(object, ldt);
   }

   @Override
   protected void accept(Object object, Date value) {
      Instant instant = value.toInstant();
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      LocalDateTime ldt = zdt.toLocalDateTime();
      this.accept(object, ldt);
   }

   @Override
   protected void acceptNull(Object object) {
      this.accept(object, (LocalDateTime)null);
   }

   @Override
   protected void accept(Object object, Instant instant) {
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      LocalDateTime ldt = zdt.toLocalDateTime();
      this.accept(object, ldt);
   }

   @Override
   protected void accept(Object object, ZonedDateTime zdt) {
      LocalDateTime ldt = zdt.toLocalDateTime();
      this.accept(object, ldt);
   }

   @Override
   protected Object apply(Date value) {
      Instant instant = value.toInstant();
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      return zdt.toLocalDateTime();
   }

   @Override
   protected Object apply(Instant instant) {
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      return zdt.toLocalDateTime();
   }

   @Override
   protected Object apply(ZonedDateTime zdt) {
      return zdt.toLocalDateTime();
   }

   @Override
   protected Object apply(LocalDateTime ldt) {
      return ldt;
   }

   @Override
   protected Object apply(long millis) {
      Instant instant = Instant.ofEpochMilli(millis);
      ZonedDateTime zdt = instant.atZone(DateUtils.DEFAULT_ZONE_ID);
      return zdt.toLocalDateTime();
   }

   @Override
   public void accept(Object object, LocalDateTime value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (object == null) {
         throw new JSONException("set " + this.fieldName + " error, object is null");
      } else if (value != null || (this.features & JSONReader.Feature.IgnoreSetNullValue.mask) == 0L) {
         if (this.fieldOffset != -1L) {
            JDKUtils.UNSAFE.putObject(object, this.fieldOffset, value);
         } else {
            try {
               this.field.set(object, value);
            } catch (Exception var4) {
               throw new JSONException("set " + this.fieldName + " error", var4);
            }
         }
      }
   }
}
