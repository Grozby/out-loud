package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.function.ObjDoubleConsumer;

final class FieldReaderDoubleValueFunc<T> extends FieldReader<T> {
   final ObjDoubleConsumer<T> function;

   public FieldReaderDoubleValueFunc(String fieldName, int ordinal, Double defaultValue, JSONSchema schema, Method method, ObjDoubleConsumer<T> function) {
      super(fieldName, double.class, double.class, ordinal, 0L, null, null, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, double value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      this.function.accept(object, value);
   }

   @Override
   public void accept(T object, Object value) {
      double doubleValue = TypeUtils.toDoubleValue(value);
      if (this.schema != null) {
         this.schema.assertValidate(doubleValue);
      }

      this.function.accept(object, doubleValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      double value = jsonReader.readDoubleValue();
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      this.function.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readDoubleValue();
   }
}
