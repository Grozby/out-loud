package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

final class FieldReaderAtomicBooleanFieldReadOnly<T> extends FieldReader<T> {
   FieldReaderAtomicBooleanFieldReadOnly(
      String fieldName, Class fieldClass, int ordinal, String format, AtomicBoolean defaultValue, JSONSchema schema, Field field
   ) {
      super(fieldName, fieldClass, fieldClass, ordinal, 0L, format, null, defaultValue, schema, null, field);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         try {
            AtomicBoolean atomic = (AtomicBoolean)this.field.get(object);
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
