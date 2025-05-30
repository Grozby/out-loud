package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.function.ObjByteConsumer;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;

final class FieldReaderInt8ValueFunc<T> extends FieldReader<T> {
   final ObjByteConsumer<T> function;

   public FieldReaderInt8ValueFunc(String fieldName, int ordinal, JSONSchema schema, Method method, ObjByteConsumer<T> function) {
      super(fieldName, byte.class, byte.class, ordinal, 0L, null, null, null, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, byte value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public void accept(T object, Object value) {
      byte byteValue = TypeUtils.toByteValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)byteValue);
      }

      this.function.accept(object, byteValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      byte value = (byte)jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (byte)jsonReader.readInt32Value();
   }
}
