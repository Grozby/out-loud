package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderDoubleFunc<T> extends FieldReader<T> {
   final BiConsumer<T, Double> function;

   public FieldReaderDoubleFunc(
      String fieldName,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Double defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, Double> function
   ) {
      super(fieldName, fieldClass, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, Object value) {
      Double doubleValue = TypeUtils.toDouble(value);
      if (this.schema != null) {
         this.schema.assertValidate(doubleValue);
      }

      this.function.accept(object, doubleValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Double fieldValue;
      try {
         fieldValue = jsonReader.readDouble();
      } catch (Exception var5) {
         if ((jsonReader.features(this.features) & JSONReader.Feature.NullOnError.mask) == 0L) {
            throw var5;
         }

         fieldValue = null;
      }

      if (fieldValue != null || this.defaultValue == null) {
         if (this.schema != null) {
            this.schema.assertValidate(fieldValue);
         }

         this.function.accept(object, fieldValue);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readDouble();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
