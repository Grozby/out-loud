package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FieldWriterStringMethod<T> extends FieldWriter<T> {
   FieldWriterStringMethod(String fieldName, int ordinal, String format, String label, long features, Field field, Method method) {
      super(fieldName, ordinal, features, format, null, label, String.class, String.class, field, method);
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
      String value = (String)this.getFieldValue(object);
      if (this.trim && value != null) {
         value = value.trim();
      }

      if (this.symbol && jsonWriter.jsonb) {
         jsonWriter.writeSymbol(value);
      } else if (this.raw) {
         jsonWriter.writeRaw(value);
      } else {
         jsonWriter.writeString(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      String value;
      try {
         value = (String)this.getFieldValue(object);
      } catch (JSONException var6) {
         if ((jsonWriter.getFeatures(this.features) | JSONWriter.Feature.IgnoreNonFieldGetter.mask) != 0L) {
            return false;
         }

         throw var6;
      }

      long features = this.features | jsonWriter.getFeatures();
      if (value == null) {
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask))
            == 0L) {
            return false;
         }
      } else if (this.trim) {
         value = value.trim();
      }

      if (value != null && value.isEmpty() && (features & JSONWriter.Feature.IgnoreEmpty.mask) != 0L) {
         return false;
      } else {
         this.writeString(jsonWriter, value);
         return true;
      }
   }
}
