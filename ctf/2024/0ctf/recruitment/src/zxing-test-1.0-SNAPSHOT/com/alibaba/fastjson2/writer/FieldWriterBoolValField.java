package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

final class FieldWriterBoolValField extends FieldWriterBoolVal {
   FieldWriterBoolValField(String fieldName, int ordinal, long features, String format, String label, Field field, Class fieldClass) {
      super(fieldName, ordinal, features, format, label, fieldClass, fieldClass, field, null);
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.getFieldValueBoolean(object);
   }

   public boolean getFieldValueBoolean(Object object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            boolean value;
            if (this.fieldOffset != -1L) {
               value = JDKUtils.UNSAFE.getBoolean(object, this.fieldOffset);
            } else {
               value = this.field.getBoolean(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }
}
