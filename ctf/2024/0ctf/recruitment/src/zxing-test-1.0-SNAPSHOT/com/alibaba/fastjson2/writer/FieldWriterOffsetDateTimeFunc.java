package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.function.Function;

final class FieldWriterOffsetDateTimeFunc<T> extends FieldWriterObjectFinal<T> {
   final Function function;

   FieldWriterOffsetDateTimeFunc(
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
      OffsetDateTime dateTime = (OffsetDateTime)this.function.apply(object);
      if (dateTime == null) {
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
            this.objectWriter = this.getObjectWriter(jsonWriter, OffsetDateTime.class);
         }

         if (this.objectWriter != ObjectWriterImplOffsetDateTime.INSTANCE) {
            this.objectWriter.write(jsonWriter, dateTime, this.fieldName, this.fieldClass, this.features);
         } else {
            jsonWriter.writeOffsetDateTime(dateTime);
         }

         return true;
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
