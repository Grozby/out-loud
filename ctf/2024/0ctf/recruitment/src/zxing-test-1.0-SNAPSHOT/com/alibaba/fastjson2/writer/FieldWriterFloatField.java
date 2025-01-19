package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

class FieldWriterFloatField<T> extends FieldWriter<T> {
   protected FieldWriterFloatField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, null, label, Float.class, Float.class, field, null);
   }

   @Override
   public Object getFieldValue(Object object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else {
         try {
            Object value;
            if (this.fieldOffset != -1L && !this.fieldClass.isPrimitive()) {
               value = JDKUtils.UNSAFE.getObject(object, this.fieldOffset);
            } else {
               value = this.field.get(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Float value = (Float)this.getFieldValue(object);
      if (value == null) {
         long features = jsonWriter.getFeatures(this.features);
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L && (features & JSONWriter.Feature.NotWriteDefaultValue.mask) == 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNumberNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeFieldName(jsonWriter);
         float floatValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeFloat(floatValue, this.decimalFormat);
         } else {
            jsonWriter.writeFloat(floatValue);
         }

         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Float value = (Float)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         float floatValue = value;
         if (this.decimalFormat != null) {
            jsonWriter.writeFloat(floatValue, this.decimalFormat);
         } else {
            jsonWriter.writeFloat(floatValue);
         }
      }
   }
}
