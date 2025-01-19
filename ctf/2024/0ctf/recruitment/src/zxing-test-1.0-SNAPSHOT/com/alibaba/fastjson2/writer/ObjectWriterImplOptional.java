package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Optional;

final class ObjectWriterImplOptional extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplOptional INSTANCE = new ObjectWriterImplOptional(null, null);
   Type valueType;
   long features;
   final String format;
   final Locale locale;

   public static ObjectWriterImplOptional of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectWriterImplOptional(format, locale);
   }

   public ObjectWriterImplOptional(String format, Locale locale) {
      this.format = format;
      this.locale = locale;
   }

   public ObjectWriterImplOptional(Type valueType, String format, Locale locale) {
      this.valueType = valueType;
      this.format = format;
      this.locale = locale;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         Optional optional = (Optional)object;
         if (!optional.isPresent()) {
            jsonWriter.writeNull();
         } else {
            Object value = optional.get();
            ObjectWriter objectWriter = jsonWriter.getObjectWriter(value.getClass());
            objectWriter.writeJSONB(jsonWriter, value, fieldName, null, features);
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         Optional optional = (Optional)object;
         if (!optional.isPresent()) {
            jsonWriter.writeNull();
         } else {
            Object value = optional.get();
            Class<?> valueClass = value.getClass();
            ObjectWriter valueWriter = null;
            if (this.format != null) {
               valueWriter = FieldWriter.getObjectWriter(null, null, this.format, this.locale, valueClass);
            }

            if (valueWriter == null) {
               valueWriter = jsonWriter.getObjectWriter(valueClass);
            }

            valueWriter.write(jsonWriter, value, fieldName, this.valueType, this.features);
         }
      }
   }
}
