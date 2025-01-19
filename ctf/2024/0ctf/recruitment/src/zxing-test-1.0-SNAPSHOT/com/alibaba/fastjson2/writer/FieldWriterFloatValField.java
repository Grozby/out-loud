package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterFloatValField<T> extends FieldWriter<T> {
   FieldWriterFloatValField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, null, label, float.class, float.class, field, null);
   }

   @Override
   public Object getFieldValue(T object) {
      return this.getFieldValueFloat(object);
   }

   public float getFieldValueFloat(T object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            float value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getFloat(object, this.fieldOffset);
            } else {
               value = this.field.getFloat(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      float value = this.getFieldValueFloat(object);
      this.writeFloat(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      float value = this.getFieldValueFloat(object);
      if (this.decimalFormat != null) {
         jsonWriter.writeFloat(value, this.decimalFormat);
      } else {
         jsonWriter.writeFloat(value);
      }
   }
}
