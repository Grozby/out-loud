package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderInt8Func<T, V> extends FieldReader<T> {
   final BiConsumer<T, V> function;

   FieldReaderInt8Func(
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
      Byte byteValue = TypeUtils.toByte(value);
      if (this.schema != null) {
         this.schema.assertValidate(byteValue);
      }

      this.function.accept(object, (V)byteValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Byte fieldValue;
      try {
         Integer value = jsonReader.readInt32();
         fieldValue = value == null ? null : value.byteValue();
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
      return jsonReader.readInt32();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
