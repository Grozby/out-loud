package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

abstract class FieldWriterInt8<T> extends FieldWriter<T> {
   FieldWriterInt8(String name, int ordinal, long features, String format, String label, Class fieldClass, Field field, Method method) {
      super(name, ordinal, features, format, null, label, fieldClass, fieldClass, field, method);
   }

   protected final void writeInt8(JSONWriter jsonWriter, byte value) {
      boolean writeNonStringValueAsString = (this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if (writeNonStringValueAsString) {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeString(Byte.toString(value));
      } else {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeInt8(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Byte value;
      try {
         value = (Byte)this.getFieldValue(object);
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
         this.writeInt8(jsonWriter, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Byte value = (Byte)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         jsonWriter.writeInt32(value);
      }
   }
}
