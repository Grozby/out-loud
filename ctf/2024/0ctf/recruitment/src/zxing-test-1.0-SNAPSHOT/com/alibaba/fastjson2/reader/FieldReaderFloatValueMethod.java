package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

final class FieldReaderFloatValueMethod<T> extends FieldReaderObject<T> {
   FieldReaderFloatValueMethod(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Float defaultValue,
      JSONSchema schema,
      Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      float fieldValue = jsonReader.readFloatValue();
      if (this.schema != null) {
         this.schema.assertValidate((double)fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      float fieldValue = jsonReader.readFloatValue();
      if (this.schema != null) {
         this.schema.assertValidate((double)fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      float floatValue = TypeUtils.toFloatValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((double)floatValue);
      }

      try {
         this.method.invoke(object, floatValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      try {
         this.method.invoke(object, (float)value);
      } catch (Exception var4) {
         throw new JSONException("set " + this.fieldName + " error", var4);
      }
   }
}
