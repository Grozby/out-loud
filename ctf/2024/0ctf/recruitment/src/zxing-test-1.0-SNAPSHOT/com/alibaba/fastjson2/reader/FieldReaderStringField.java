package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Field;

class FieldReaderStringField<T> extends FieldReaderObjectField<T> {
   final boolean trim;
   final boolean upper;
   final boolean emptyToNull;

   FieldReaderStringField(String fieldName, Class fieldType, int ordinal, long features, String format, String defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
      this.trim = "trim".equals(format) || (features & JSONReader.Feature.TrimString.mask) != 0L;
      this.upper = "upper".equals(format);
      this.emptyToNull = (features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L;
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

         if (this.emptyToNull && fieldValue.isEmpty()) {
            fieldValue = null;
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      JDKUtils.UNSAFE.putObject(object, this.fieldOffset, fieldValue);
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      String fieldValue = jsonReader.readString();
      if (fieldValue != null) {
         if (this.trim) {
            fieldValue = fieldValue.trim();
         }

         if (this.upper) {
            fieldValue = fieldValue.toUpperCase();
         }

         if (this.emptyToNull && fieldValue.isEmpty()) {
            fieldValue = null;
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      this.accept(object, fieldValue);
   }

   public String readFieldValue(JSONReader jsonReader) {
      String fieldValue = jsonReader.readString();
      if (this.trim && fieldValue != null) {
         fieldValue = fieldValue.trim();
      }

      return fieldValue;
   }

   @Override
   public boolean supportAcceptType(Class valueClass) {
      return true;
   }

   @Override
   public void accept(T object, Object value) {
      String fieldValue;
      if (value != null && !(value instanceof String)) {
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

         if (this.emptyToNull && fieldValue.isEmpty()) {
            fieldValue = null;
         }
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      JDKUtils.UNSAFE.putObject(object, this.fieldOffset, fieldValue);
   }
}
