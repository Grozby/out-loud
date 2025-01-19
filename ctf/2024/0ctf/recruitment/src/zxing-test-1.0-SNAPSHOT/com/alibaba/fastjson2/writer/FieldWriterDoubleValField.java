package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterDoubleValField<T> extends FieldWriter<T> {
   FieldWriterDoubleValField(String name, int ordinal, String format, String label, Field field) {
      super(name, ordinal, 0L, format, null, label, double.class, double.class, field, null);
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.getFieldValueDouble(object);
   }

   public double getFieldValueDouble(Object object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            double value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getDouble(object, this.fieldOffset);
            } else {
               value = this.field.getDouble(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var4) {
            throw new JSONException("field.get error, " + this.fieldName, var4);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      double value = this.getFieldValueDouble(object);
      this.writeDouble(jsonWriter, value);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      double value = this.getFieldValueDouble(object);
      if (this.decimalFormat != null) {
         jsonWriter.writeDouble(value, this.decimalFormat);
      } else {
         jsonWriter.writeDouble(value);
      }
   }
}
