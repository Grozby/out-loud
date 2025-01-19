package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterInt16ValField<T> extends FieldWriterInt16<T> {
   FieldWriterInt16ValField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, label, short.class, field, null);
   }

   @Override
   public Object getFieldValue(T object) {
      return this.getFieldValueShort(object);
   }

   public short getFieldValueShort(T object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            short value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getShort(object, this.fieldOffset);
            } else {
               value = this.field.getShort(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      short value = this.getFieldValueShort(object);
      this.writeInt16(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      short value = this.getFieldValueShort(object);
      jsonWriter.writeInt32(value);
   }
}
