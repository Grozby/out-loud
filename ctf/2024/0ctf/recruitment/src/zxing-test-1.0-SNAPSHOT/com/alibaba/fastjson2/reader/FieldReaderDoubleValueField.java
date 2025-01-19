package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderDoubleValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderDoubleValueField(
      String fieldName, Class fieldType, int ordinal, long features, String format, Double defaultValue, JSONSchema schema, Field field
   ) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      double fieldValue = jsonReader.readDoubleValue();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.field.setDouble(object, fieldValue);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readDoubleValue();
   }

   @Override
   public void accept(T object, Object value) {
      double doubleValue = TypeUtils.toDoubleValue(value);
      if (this.schema != null) {
         this.schema.assertValidate(doubleValue);
      }

      try {
         this.field.set(object, doubleValue);
      } catch (Exception var6) {
         throw new JSONException("set " + this.fieldName + " error", var6);
      }
   }
}
