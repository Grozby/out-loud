package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

final class FieldReaderDoubleValueMethod<T> extends FieldReaderObject<T> {
   FieldReaderDoubleValueMethod(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Double defaultValue,
      JSONSchema schema,
      Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      double fieldValue = jsonReader.readDoubleValue();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      double fieldValue = jsonReader.readDoubleValue();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
      }
   }

   @Override
   public void accept(T object, Object value) {
      double doubleValue = TypeUtils.toDoubleValue(value);
      if (this.schema != null) {
         this.schema.assertValidate(doubleValue);
      }

      try {
         this.method.invoke(object, doubleValue);
      } catch (Exception var6) {
         throw new JSONException("set " + this.fieldName + " error", var6);
      }
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      try {
         this.method.invoke(object, (double)value);
      } catch (Exception var4) {
         throw new JSONException("set " + this.fieldName + " error", var4);
      }
   }
}
