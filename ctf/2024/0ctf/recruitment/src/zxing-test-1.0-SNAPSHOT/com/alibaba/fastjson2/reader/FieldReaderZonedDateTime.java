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

public class FieldReaderZonedDateTime<T> extends FieldReaderDateTimeCodec<T> {
   final BiConsumer<T, ZonedDateTime> function;

   FieldReaderZonedDateTime(
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
         ObjectReaderImplZonedDateTime.of(format, locale)
      );
      this.function = function;
   }

   @Override
   public final void readFieldValue(JSONReader jsonReader, T object) {
      ZonedDateTime date = (ZonedDateTime)this.dateReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
      this.accept(object, date);
   }

   @Override
   public final void readFieldValueJSONB(JSONReader jsonReader, T object) {
      ZonedDateTime date = jsonReader.readZonedDateTime();
      this.accept(object, date);
   }

   @Override
   protected void accept(T object, Date value) {
      Instant instant = value.toInstant();
      ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
      this.accept(object, zdt);
   }

   @Override
   protected void accept(T object, Instant instant) {
      ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
      this.accept(object, zdt);
   }

   @Override
   protected void accept(T object, LocalDateTime ldt) {
      ZonedDateTime zdt = ZonedDateTime.of(ldt, DateUtils.DEFAULT_ZONE_ID);
      this.accept(object, zdt);
   }

   @Override
   protected Object apply(Date value) {
      Instant instant = value.toInstant();
      return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
   }

   @Override
   protected Object apply(Instant value) {
      return ZonedDateTime.ofInstant(value, DateUtils.DEFAULT_ZONE_ID);
   }

   @Override
   protected Object apply(ZonedDateTime zdt) {
      return zdt;
   }

   @Override
   protected Object apply(long millis) {
      Instant instant = Instant.ofEpochMilli(millis);
      return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
   }

   @Override
   protected Object apply(LocalDateTime ldt) {
      return ldt.atZone(DateUtils.DEFAULT_ZONE_ID);
   }

   @Override
   protected void acceptNull(T object) {
      this.accept(object, (ZonedDateTime)null);
   }

   @Override
   public void accept(T object, long milli) {
      Instant instant = Instant.ofEpochMilli(milli);
      ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
      this.accept(object, zdt);
   }

   @Override
   protected void accept(T object, ZonedDateTime zdt) {
      if (this.schema != null) {
         this.schema.assertValidate(zdt);
      }

      if (zdt != null || (this.features & JSONReader.Feature.IgnoreSetNullValue.mask) == 0L) {
         if (object == null) {
            throw new JSONException("set " + this.fieldName + " error, object is null");
         } else if (this.function != null) {
            this.function.accept(object, zdt);
         } else if (this.method != null) {
            try {
               this.method.invoke(object, zdt);
            } catch (Exception var4) {
               throw new JSONException("set " + this.fieldName + " error", var4);
            }
         } else if (this.fieldOffset != -1L) {
            JDKUtils.UNSAFE.putObject(object, this.fieldOffset, zdt);
         } else {
            try {
               this.field.set(object, zdt);
            } catch (Exception var5) {
               throw new JSONException("set " + this.fieldName + " error", var5);
            }
         }
      }
   }
}
