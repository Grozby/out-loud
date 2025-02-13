package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

abstract class FieldWriterInt16<T> extends FieldWriter<T> {
   FieldWriterInt16(String name, int ordinal, long features, String format, String label, Class fieldClass, Field field, Method method) {
      super(name, ordinal, features, format, null, label, fieldClass, fieldClass, field, method);
   }

   protected final void writeInt16(JSONWriter jsonWriter, short value) {
      boolean writeNonStringValueAsString = (jsonWriter.getFeatures(this.features) & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if (writeNonStringValueAsString) {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeString(Short.toString(value));
      } else {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeInt16(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Short value;
      try {
         value = (Short)this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNumberNull();
            return true;
         }
      } else {
         this.writeInt16(jsonWriter, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Short value = (Short)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         jsonWriter.writeInt32(value);
      }
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      return ObjectWriterImplInt16.INSTANCE;
   }
}
