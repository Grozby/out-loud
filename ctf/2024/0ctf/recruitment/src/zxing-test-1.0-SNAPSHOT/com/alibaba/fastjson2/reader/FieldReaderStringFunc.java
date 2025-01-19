package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderStringFunc<T, V> extends FieldReader<T> {
   final BiConsumer<T, V> function;
   final String format;
   final boolean trim;
   final boolean upper;

   FieldReaderStringFunc(
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
      this.format = format;
      this.trim = "trim".equals(format) || (features & JSONReader.Feature.TrimString.mask) != 0L;
      this.upper = "upper".equals(format);
   }

   @Override
   public void accept(T object, int value) {
      this.accept(object, Integer.toString(value));
   }

   @Override
   public void accept(T object, long value) {
      this.accept(object, Long.toString(value));
   }

   @Override
   public void accept(T object, Object value) {
      String fieldValue;
      if (!(value instanceof String) && value != null) {
         fieldValue = value.toString();
      } else {
         fieldValue = (String)value;
      }

      if (fieldValue != null) {
         if (this.trim) {
            fieldValue = fieldValue.trim();
         }

         if (this.upper) {
            fieldValue = fieldValue.toUpperCase();
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.function.accept(object, (V)fieldValue);
      } catch (Exception var5) {
         throw new JSONException("set " + super.toString() + " error", var5);
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      String fieldValue = jsonReader.readString();
      if (fieldValue != null) {
         if (this.trim) {
            fieldValue = fieldValue.trim();
         }

         if (this.upper) {
            fieldValue = fieldValue.toUpperCase();
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      this.function.accept(object, (V)fieldValue);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readString();
   }

   @Override
   public boolean supportAcceptType(Class valueClass) {
      return true;
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
