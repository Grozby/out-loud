package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterMillisField<T> extends FieldWriterDate<T> {
   FieldWriterMillisField(String fieldName, int ordinal, long features, String dateTimeFormat, String label, Field field) {
      super(fieldName, ordinal, features, dateTimeFormat, label, long.class, long.class, field, null);
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
   public boolean write(JSONWriter jsonWriter, T object) {
      long millis = this.getFieldLong(object);
      this.writeDate(jsonWriter, millis);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      long millis = this.getFieldLong(object);
      this.writeDate(jsonWriter, false, millis);
   }
}
