package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.function.Function;

final class FieldWriterCalendarFunc<T> extends FieldWriterDate<T> {
   final Function<T, Calendar> function;

   FieldWriterCalendarFunc(
      String fieldName, int ordinal, long features, String dateTimeFormat, String label, Field field, Method method, Function<T, Calendar> function
   ) {
      super(fieldName, ordinal, features, dateTimeFormat, label, Calendar.class, Calendar.class, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      Calendar value = this.function.apply(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         long millis = value.getTimeInMillis();
         this.writeDate(jsonWriter, false, millis);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T o) {
      Calendar value = this.function.apply(o);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         this.writeDate(jsonWriter, value.getTimeInMillis());
         return true;
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
