package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

final class FieldReaderStringMethod<T> extends FieldReaderObject<T> {
   final boolean trim;
   final boolean upper;

   FieldReaderStringMethod(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      String defaultValue,
      JSONSchema schema,
      Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
      this.trim = "trim".equals(format) || (features & JSONReader.Feature.TrimString.mask) != 0L;
      this.upper = "upper".equals(format);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      String fieldValue = jsonReader.readString();
      if (fieldValue != null) {
         if (this.trim) {
            fieldValue = fieldValue.trim();
         }

         if (this.upper) {
            fieldValue = fieldValue.toUpperCase();
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   public String readFieldValue(JSONReader jsonReader) {
      String fieldValue = jsonReader.readString();
      if (this.trim && fieldValue != null) {
         fieldValue = fieldValue.trim();
      }

      return fieldValue;
   }

   @Override
   public void accept(T object, Object value) {
      String fieldValue;
      if (!(value instanceof String) && value != null) {
         fieldValue = value.toString();
      } else {
         fieldValue = (String)value;
      }

      if (fieldValue != null) {
         if (this.trim) {
            fieldValue = fieldValue.trim();
         }

         if (this.upper) {
            fieldValue = fieldValue.toUpperCase();
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public boolean supportAcceptType(Class valueClass) {
      return true;
   }
}
