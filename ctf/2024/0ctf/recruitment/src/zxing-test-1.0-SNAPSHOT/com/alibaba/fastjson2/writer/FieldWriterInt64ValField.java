package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterInt64ValField<T> extends FieldWriterInt64<T> {
   FieldWriterInt64ValField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, label, long.class, field, null);
   }

   @Override
   public Object getFieldValue(T object) {
      return this.getFieldLong(object);
   }

   public long getFieldLong(T object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            long value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getLong(object, this.fieldOffset);
            } else {
               value = this.field.getLong(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var4) {
            throw new JSONException("field.get error, " + this.fieldName, var4);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T o) {
      long value = this.getFieldLong(o);
      if (value == 0L && jsonWriter.isEnabled(JSONWriter.Feature.NotWriteDefaultValue)) {
         return false;
      } else {
         this.writeInt64(jsonWriter, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      long value = this.getFieldLong(object);
      jsonWriter.writeInt64(value);
   }
}
