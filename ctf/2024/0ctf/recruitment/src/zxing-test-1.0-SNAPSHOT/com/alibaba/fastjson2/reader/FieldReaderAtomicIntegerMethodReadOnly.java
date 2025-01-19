package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

class FieldReaderAtomicIntegerMethodReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicIntegerMethodReadOnly(String fieldName, Class fieldType, int ordinal, JSONSchema jsonSchema, Method method) {
      super(fieldName, fieldType, fieldType, ordinal, 0L, null, null, null, jsonSchema, method, null);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            AtomicInteger atomic = (AtomicInteger)this.method.invoke(object);
            int intValue = ((Number)value).intValue();
            atomic.set(intValue);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Integer value = jsonReader.readInt32();
      this.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      int intValue = jsonReader.readInt32Value();
      return jsonReader.wasNull() ? null : new AtomicInteger(intValue);
   }
}
