package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

abstract class FieldWriterInt64<T> extends FieldWriter<T> {
   final boolean browserCompatible;

   FieldWriterInt64(String name, int ordinal, long features, String format, String label, Class fieldClass, Field field, Method method) {
      super(name, ordinal, features, format, null, label, fieldClass, fieldClass, field, method);
      this.browserCompatible = (features & JSONWriter.Feature.BrowserCompatible.mask) != 0L;
   }

   @Override
   public final void writeInt64(JSONWriter jsonWriter, long value) {
      long features = jsonWriter.getFeatures() | this.features;
      boolean writeAsString = (features & (JSONWriter.Feature.WriteNonStringValueAsString.mask | JSONWriter.Feature.WriteLongAsString.mask)) != 0L;
      this.writeFieldName(jsonWriter);
      if (!writeAsString) {
         writeAsString = this.browserCompatible && !TypeUtils.isJavaScriptSupport(value) && !jsonWriter.jsonb;
      }

      if (writeAsString) {
         jsonWriter.writeString(Long.toString(value));
      } else {
         jsonWriter.writeInt64(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Long value;
      try {
         value = (Long)this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask))
            == 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNumberNull();
            return true;
         }
      } else {
         this.writeInt64(jsonWriter, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Long value = (Long)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         jsonWriter.writeInt64(value);
      }
   }
}
