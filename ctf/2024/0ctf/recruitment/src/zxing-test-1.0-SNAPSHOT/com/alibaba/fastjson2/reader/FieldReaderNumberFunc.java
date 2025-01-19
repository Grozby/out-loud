package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderNumberFunc<T, V> extends FieldReader<T> {
   final BiConsumer<T, V> function;

   public FieldReaderNumberFunc(
      String fieldName,
      Class<V> fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Number defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, V> function
   ) {
      super(fieldName, fieldClass, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, Object value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (value instanceof Boolean) {
         value = (Boolean)value ? 1 : 0;
      }

      this.function.accept(object, (V)value);
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, (V)value);
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      this.function.accept(object, (V)value);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Number fieldValue;
      try {
         fieldValue = jsonReader.readNumber();
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
      return jsonReader.readNumber();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
