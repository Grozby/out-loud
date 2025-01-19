package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

final class FieldReaderAtomicLongReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicLongReadOnly(String fieldName, Class fieldType, int ordinal, JSONSchema schema, Method method) {
      super(fieldName, fieldType, fieldType, ordinal, 0L, null, null, null, schema, method, null);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            AtomicLong atomic = (AtomicLong)this.method.invoke(object);
            long longValue = ((Number)value).longValue();
            atomic.set(longValue);
         } catch (Exception var6) {
            throw new JSONException("set " + this.fieldName + " error", var6);
         }
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Long value = jsonReader.readInt64();
      this.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      long longValue = jsonReader.readInt64Value();
      return jsonReader.wasNull() ? null : new AtomicLong(longValue);
   }
}
