package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.function.ObjFloatConsumer;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;

final class FieldReaderFloatValueFunc<T> extends FieldReader<T> {
   final ObjFloatConsumer<T> function;

   public FieldReaderFloatValueFunc(String fieldName, int ordinal, Float defaultValue, JSONSchema schema, Method method, ObjFloatConsumer<T> function) {
      super(fieldName, float.class, float.class, ordinal, 0L, null, null, defaultValue, schema, method, null);
      this.function = function;
   }

   @Override
   public void accept(T object, float value) {
      if (this.schema != null) {
         this.schema.assertValidate((double)value);
      }

      this.function.accept(object, value);
   }

   @Override
   public void accept(T object, Object value) {
      float floatValue = TypeUtils.toFloatValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((double)floatValue);
      }

      this.function.accept(object, floatValue);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      float fieldValue = jsonReader.readFloatValue();
      if (this.schema != null) {
         this.schema.assertValidate((double)fieldValue);
      }

      this.function.accept(object, fieldValue);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readFloatValue();
   }
}
