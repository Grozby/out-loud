package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.BiConsumer;

final class FieldReaderLocalDate extends FieldReaderObject {
   public FieldReaderLocalDate(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      Field field,
      BiConsumer function
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, field, function);
      this.initReader = ObjectReaderImplLocalDate.of(format, locale);
   }

   @Override
   public ObjectReader getObjectReader(JSONReader jsonReader) {
      return this.initReader;
   }

   @Override
   public ObjectReader getObjectReader(JSONReader.Context context) {
      return this.initReader;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, Object object) {
      LocalDate localDate;
      if (jsonReader.jsonb) {
         localDate = (LocalDate)this.initReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
      } else if (this.format != null) {
         localDate = (LocalDate)this.initReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
      } else {
         localDate = jsonReader.readLocalDate();
      }

      this.accept(object, localDate);
   }
}
