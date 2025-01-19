package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.function.Function;

final class FieldWriterUUIDFunc<T> extends FieldWriterObjectFinal<T> {
   final Function function;

   FieldWriterUUIDFunc(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method, Function function
   ) {
      super(name, ordinal, features, format, label, fieldType, fieldClass, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(Object object) {
      return this.function.apply(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      UUID uuid = (UUID)this.function.apply(object);
      if (uuid == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeFieldName(jsonWriter);
         if (this.objectWriter == null) {
            this.objectWriter = this.getObjectWriter(jsonWriter, UUID.class);
         }

         if (this.objectWriter != ObjectWriterImplUUID.INSTANCE) {
            this.objectWriter.write(jsonWriter, uuid, this.fieldName, this.fieldClass, this.features);
         } else {
            jsonWriter.writeUUID(uuid);
         }

         return true;
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
