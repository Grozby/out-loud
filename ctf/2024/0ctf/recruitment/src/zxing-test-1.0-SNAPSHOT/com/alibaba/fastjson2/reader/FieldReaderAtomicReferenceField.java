package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

final class FieldReaderAtomicReferenceField<T> extends FieldReaderAtomicReference<T> {
   final boolean readOnly;

   FieldReaderAtomicReferenceField(String fieldName, Type fieldType, Class fieldClass, int ordinal, String format, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldClass, ordinal, 0L, format, schema, null, field);
      this.readOnly = Modifier.isFinal(field.getModifiers());
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            if (this.readOnly) {
               AtomicReference atomic = (AtomicReference)this.field.get(object);
               atomic.set(value);
            } else {
               this.field.set(object, new AtomicReference<>(value));
            }
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }
}
