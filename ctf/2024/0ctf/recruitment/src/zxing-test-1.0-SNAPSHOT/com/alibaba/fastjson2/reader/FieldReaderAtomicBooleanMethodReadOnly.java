package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

final class FieldReaderAtomicBooleanMethodReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicBooleanMethodReadOnly(String fieldName, Class fieldClass, int ordinal, JSONSchema schema, Method method) {
      super(fieldName, fieldClass, fieldClass, ordinal, 0L, null, null, null, schema, method, null);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            AtomicBoolean atomic = (AtomicBoolean)this.method.invoke(object);
            if (value instanceof AtomicBoolean) {
               value = ((AtomicBoolean)value).get();
            }

            atomic.set((Boolean)value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Boolean value = jsonReader.readBool();
      this.accept(object, value);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readBool();
   }
}
