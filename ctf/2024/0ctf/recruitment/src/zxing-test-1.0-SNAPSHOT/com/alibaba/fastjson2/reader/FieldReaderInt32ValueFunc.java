package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.function.ObjIntConsumer;

final class FieldReaderInt32ValueFunc<T> extends FieldReader<T> {
   final ObjIntConsumer<T> function;

   public FieldReaderInt32ValueFunc(String fieldName, int ordinal, Integer defaultValue, JSONSchema schema, Method method, ObjIntConsumer<T> function) {
      super(fieldName, int.class, int.class, ordinal, 0L, null, null, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      this.function.accept(object, (int)value);
   }

   @Override
   public void accept(T object, Object value) {
      int intValue = TypeUtils.toIntValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)intValue);
      }

      this.function.accept(object, intValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int value = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32Value();
   }
}
