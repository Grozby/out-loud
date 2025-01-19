package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterInt8ValField<T> extends FieldWriterInt8<T> {
   FieldWriterInt8ValField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, label, byte.class, field, null);
   }

   @Override
   public Object getFieldValue(T object) {
      return this.getFieldValueByte(object);
   }

   public byte getFieldValueByte(T object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            byte value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getByte(object, this.fieldOffset);
            } else {
               value = this.field.getByte(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      byte value = this.getFieldValueByte(object);
      this.writeInt8(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      byte value = this.getFieldValueByte(object);
      jsonWriter.writeInt32(value);
   }
}
