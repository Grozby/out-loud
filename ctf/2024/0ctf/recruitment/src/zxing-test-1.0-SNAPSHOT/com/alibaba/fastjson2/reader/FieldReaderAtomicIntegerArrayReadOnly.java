package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

final class FieldReaderAtomicIntegerArrayReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicIntegerArrayReadOnly(String fieldName, Class fieldType, int ordinal, JSONSchema jsonSchema, Method method) {
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
            AtomicIntegerArray atomic = (AtomicIntegerArray)this.method.invoke(object);
            if (value instanceof AtomicIntegerArray) {
               AtomicIntegerArray array = (AtomicIntegerArray)value;

               for (int i = 0; i < array.length(); i++) {
                  atomic.set(i, array.get(i));
               }
            } else {
               List values = (List)value;

               for (int i = 0; i < values.size(); i++) {
                  int itemValue = TypeUtils.toIntValue(values.get(i));
                  atomic.set(i, itemValue);
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
         AtomicIntegerArray atomic;
         try {
            atomic = (AtomicIntegerArray)this.method.invoke(object);
         } catch (Exception var6) {
            throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
         }

         if (jsonReader.nextIfArrayStart()) {
            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               int value = jsonReader.readInt32Value();
               if (atomic != null && i < atomic.length()) {
                  atomic.set(i, value);
               }
            }
         }
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.nextIfNull() ? null : jsonReader.readArray(Integer.class);
   }
}
