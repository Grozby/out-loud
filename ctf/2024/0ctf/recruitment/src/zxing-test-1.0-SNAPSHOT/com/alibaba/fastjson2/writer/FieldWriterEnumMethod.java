package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterEnumMethod extends FieldWriterEnum {
   FieldWriterEnumMethod(String name, int ordinal, long features, String format, String label, Class fieldType, Field field, Method method) {
      super(name, ordinal, features, format, label, fieldType, fieldType, field, method);
   }

   @Override
   public Object getFieldValue(Object object) {
      try {
         return this.method.invoke(object);
      } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException var3) {
         throw new JSONException("invoke getter method error, " + this.fieldName, var3);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      Enum value = (Enum)this.getFieldValue(object);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeEnum(jsonWriter, value);
         return true;
      }
   }
}
