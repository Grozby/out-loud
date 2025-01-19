package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.function.Function;

final class FieldWriterLocalDateFunc<T> extends FieldWriterObjectFinal<T> {
   final Function function;

   FieldWriterLocalDateFunc(
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
      LocalDate localDate = (LocalDate)this.function.apply(object);
      if (localDate == null) {
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
            this.objectWriter = this.getObjectWriter(jsonWriter, LocalDate.class);
         }

         if (this.objectWriter != ObjectWriterImplLocalDate.INSTANCE) {
            this.objectWriter.write(jsonWriter, localDate, this.fieldName, this.fieldClass, this.features);
         } else {
            jsonWriter.writeLocalDate(localDate);
         }

         return true;
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
