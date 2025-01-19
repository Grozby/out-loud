package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

final class FieldReaderAtomicLongArrayReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicLongArrayReadOnly(String fieldName, Class fieldType, int ordinal, JSONSchema jsonSchema, Method method) {
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
            AtomicLongArray atomic = (AtomicLongArray)this.method.invoke(object);
            if (value instanceof AtomicLongArray) {
               AtomicLongArray array = (AtomicLongArray)value;

               for (int i = 0; i < array.length(); i++) {
                  atomic.set(i, array.get(i));
               }
            } else {
               List values = (List)value;

               for (int i = 0; i < values.size(); i++) {
                  int itemValue = TypeUtils.toIntValue(values.get(i));
                  atomic.set(i, (long)itemValue);
               }
            }
         } catch (Exception var7) {
            throw new JSONException("set " + this.fieldName + " error", var7);
         }
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (!jsonReader.readIfNull()) {
         AtomicLongArray atomic;
         try {
            atomic = (AtomicLongArray)this.method.invoke(object);
         } catch (Exception var7) {
            throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var7);
         }

         if (jsonReader.nextIfArrayStart()) {
            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               long value = jsonReader.readInt64Value();
               if (atomic != null && i < atomic.length()) {
                  atomic.set(i, value);
               }
            }
         }
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.nextIfNull() ? null : jsonReader.readArray(Long.class);
   }
}
