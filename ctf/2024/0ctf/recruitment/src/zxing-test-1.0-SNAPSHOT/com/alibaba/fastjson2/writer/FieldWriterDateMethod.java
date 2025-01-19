package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

final class FieldWriterDateMethod<T> extends FieldWriterDate<T> {
   FieldWriterDateMethod(String fieldName, int ordinal, long features, String format, String label, Class fieldClass, Field field, Method method) {
      super(fieldName, ordinal, features, format, label, fieldClass, fieldClass, field, method);
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
   public void writeValue(JSONWriter jsonWriter, T object) {
      Date value = (Date)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         this.writeDate(jsonWriter, false, value.getTime());
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Date value = (Date)this.getFieldValue(object);
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
         this.writeDate(jsonWriter, value.getTime());
         return true;
      }
   }
}
