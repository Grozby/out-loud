package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterCharValField<T> extends FieldWriter<T> {
   FieldWriterCharValField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, null, label, char.class, char.class, field, null);
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.getFieldValueChar(object);
   }

   public char getFieldValueChar(Object object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            char value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getChar(object, this.fieldOffset);
            } else {
               value = this.field.getChar(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      char value = this.getFieldValueChar(object);
      this.writeFieldName(jsonWriter);
      jsonWriter.writeChar(value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      char value = this.getFieldValueChar(object);
      jsonWriter.writeChar(value);
   }
}
