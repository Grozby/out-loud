package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderFloatFunc<T> extends FieldReader<T> {
   final BiConsumer<T, Float> function;

   public FieldReaderFloatFunc(
      String fieldName,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Float defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, Float> function
   ) {
      super(fieldName, fieldClass, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, Object value) {
      Float floatValue = TypeUtils.toFloat(value);
      if (this.schema != null) {
         this.schema.assertValidate(floatValue);
      }

      this.function.accept(object, floatValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Float fieldValue;
      try {
         fieldValue = jsonReader.readFloat();
      } catch (Exception var5) {
         if ((jsonReader.features(this.features) & JSONReader.Feature.NullOnError.mask) == 0L) {
            throw var5;
         }

         fieldValue = null;
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      this.function.accept(object, fieldValue);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readFloat();
   }

   @Override
   public BiConsumer getFunction() {
      return this.function;
   }
}
