package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderBigIntegerFunc<T, V> extends FieldReader<T> {
   final BiConsumer<T, V> function;

   public FieldReaderBigIntegerFunc(
      String fieldName,
      Class<V> fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, V> function
   ) {
      super(fieldName, fieldClass, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, Object value) {
      BigInteger bigInteger = TypeUtils.toBigInteger(value);
      if (this.schema != null) {
         this.schema.assertValidate(bigInteger);
      }

      try {
         this.function.accept(object, (V)bigInteger);
      } catch (Exception var5) {
         throw new JSONException("set " + super.toString() + " error", var5);
      }
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      try {
         this.function.accept(object, (V)BigInteger.valueOf((long)value));
      } catch (Exception var4) {
         throw new JSONException("set " + super.toString() + " error", var4);
      }
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      try {
         this.function.accept(object, (V)BigInteger.valueOf(value));
      } catch (Exception var5) {
         throw new JSONException("set " + super.toString() + " error", var5);
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      BigInteger fieldValue;
      try {
         fieldValue = jsonReader.readBigInteger();
      } catch (Exception var5) {
         if ((jsonReader.features(this.features) & JSONReader.Feature.NullOnError.mask) == 0L) {
            throw var5;
         }

         fieldValue = null;
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      this.function.accept(object, (V)fieldValue);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readBigInteger();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
