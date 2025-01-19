package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

abstract class FieldWriterInt32<T> extends FieldWriter<T> {
   final boolean toString;

   protected FieldWriterInt32(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      this.toString = (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L || "string".equals(format);
   }

   @Override
   public final void writeInt32(JSONWriter jsonWriter, int value) {
      if (this.toString) {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeString(Integer.toString(value));
      } else {
         this.writeFieldName(jsonWriter);
         if (this.format != null) {
            jsonWriter.writeInt32(value, this.format);
         } else {
            jsonWriter.writeInt32(value);
         }
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      Integer value;
      try {
         value = (Integer)this.getFieldValue(object);
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
         this.writeInt32(jsonWriter, value);
         return true;
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Integer value = (Integer)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNumberNull();
      } else {
         jsonWriter.writeInt32(value);
      }
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      return (ObjectWriter)(valueClass == this.fieldClass ? ObjectWriterImplInt32.INSTANCE : jsonWriter.getObjectWriter(valueClass));
   }
}
