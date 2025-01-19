package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

final class FieldWriterListMethod<T> extends FieldWriterList<T> {
   FieldWriterListMethod(
      String fieldName, Type itemType, int ordinal, long features, String format, String label, Field field, Method method, Type fieldType, Class fieldClass
   ) {
      super(fieldName, itemType, ordinal, features, format, label, fieldType, fieldClass, field, method);
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
   public boolean write(JSONWriter jsonWriter, T object) {
      List value;
      try {
         value = (List)this.getFieldValue(object);
      } catch (JSONException var7) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var7;
      }

      long features = this.features | jsonWriter.getFeatures();
      if (value == null) {
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask))
            != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
            return true;
         } else {
            return false;
         }
      } else if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) != 0L && value.isEmpty()) {
         return false;
      } else {
         String refPath = jsonWriter.setPath(this, value);
         if (refPath != null) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeReference(refPath);
            jsonWriter.popPath(value);
            return true;
         } else {
            if (this.itemType == String.class) {
               this.writeListStr(jsonWriter, true, value);
            } else {
               this.writeList(jsonWriter, value);
            }

            jsonWriter.popPath(value);
            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      List value = (List)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         this.writeListValue(jsonWriter, value);
      }
   }
}
