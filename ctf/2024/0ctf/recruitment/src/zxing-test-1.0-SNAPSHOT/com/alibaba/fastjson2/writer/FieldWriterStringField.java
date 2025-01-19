package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;

final class FieldWriterStringField<T> extends FieldWriter<T> {
   FieldWriterStringField(String fieldName, int ordinal, long features, String format, String label, Field field) {
      super(fieldName, ordinal, features, format, null, label, String.class, String.class, field, null);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      String value = (String)this.getFieldValue(object);
      long features = this.features | jsonWriter.getFeatures();
      if (value == null) {
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask))
               != 0L
            && (features & JSONWriter.Feature.NotWriteDefaultValue.mask) == 0L) {
            this.writeFieldName(jsonWriter);
            if ((features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) != 0L) {
               jsonWriter.writeString("");
            } else {
               jsonWriter.writeNull();
            }

            return true;
         } else {
            return false;
         }
      } else {
         if (this.trim) {
            value = value.trim();
         }

         if (value.isEmpty() && (features & JSONWriter.Feature.IgnoreEmpty.mask) != 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            if (this.symbol && jsonWriter.jsonb) {
               jsonWriter.writeSymbol(value);
            } else if (this.raw) {
               jsonWriter.writeRaw(value);
            } else {
               jsonWriter.writeString(value);
            }

            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      String value = (String)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         if (this.trim) {
            value = value.trim();
         }

         if (this.raw) {
            jsonWriter.writeRaw(value);
         } else {
            jsonWriter.writeString(value);
         }
      }
   }
}
