package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

final class FieldReaderAtomicReferenceMethodReadOnly<T> extends FieldReaderAtomicReference<T> {
   FieldReaderAtomicReferenceMethodReadOnly(String fieldName, Type fieldType, Class fieldClass, int ordinal, JSONSchema schema, Method method) {
      super(fieldName, fieldType, fieldClass, ordinal, 0L, null, schema, method, null);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            AtomicReference atomic = (AtomicReference)this.method.invoke(object);
            atomic.set(value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }
}
