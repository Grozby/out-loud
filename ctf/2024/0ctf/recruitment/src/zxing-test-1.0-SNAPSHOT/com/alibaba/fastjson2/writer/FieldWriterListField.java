package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

final class FieldWriterListField<T> extends FieldWriterList<T> {
   FieldWriterListField(String fieldName, Type itemType, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field) {
      super(fieldName, itemType, ordinal, features, format, label, fieldType, fieldClass, field, null);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      List value = (List)this.getFieldValue(object);
      JSONWriter.Context context = jsonWriter.context;
      if (value == null) {
         long features = this.features | context.getFeatures();
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask))
            != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
            return true;
         } else {
            return false;
         }
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
         boolean refDetect = jsonWriter.isRefDetect();
         if (refDetect) {
            String refPath = jsonWriter.setPath(this.fieldName, value);
            if (refPath != null) {
               jsonWriter.writeReference(refPath);
               jsonWriter.popPath(value);
               return;
            }
         }

         this.writeListValue(jsonWriter, value);
         if (refDetect) {
            jsonWriter.popPath(value);
         }
      }
   }
}
