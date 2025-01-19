package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;

final class FieldReaderInt32Method<T> extends FieldReaderObject<T> {
   FieldReaderInt32Method(String fieldName, int ordinal, long features, String format, Locale locale, Integer defaultValue, JSONSchema schema, Method setter) {
      super(fieldName, Integer.class, Integer.class, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Integer fieldValue = jsonReader.readInt32();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      Integer fieldValue = jsonReader.readInt32();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      Integer integer = TypeUtils.toInteger(value);
      if (this.schema != null) {
         this.schema.assertValidate(integer);
      }

      try {
         this.method.invoke(object, integer);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32();
   }
}
