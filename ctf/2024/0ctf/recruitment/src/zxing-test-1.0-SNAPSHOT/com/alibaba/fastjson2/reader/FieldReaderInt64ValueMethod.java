package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

final class FieldReaderInt64ValueMethod<T> extends FieldReaderObject<T> {
   FieldReaderInt64ValueMethod(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Long defaultValue,
      JSONSchema schema,
      Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      long fieldLong = jsonReader.readInt64Value();
      if (this.schema != null) {
         this.schema.assertValidate(fieldLong);
      }

      try {
         this.method.invoke(object, fieldLong);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      long fieldLong = jsonReader.readInt64Value();
      if (this.schema != null) {
         this.schema.assertValidate(fieldLong);
      }

      try {
         this.method.invoke(object, fieldLong);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
      }
   }

   @Override
   public void accept(T object, Object value) {
      long longValue = TypeUtils.toLongValue(value);
      if (this.schema != null) {
         this.schema.assertValidate(longValue);
      }

      try {
         this.method.invoke(object, longValue);
      } catch (Exception var6) {
         throw new JSONException("set " + this.fieldName + " error", var6);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt64Value();
   }
}
