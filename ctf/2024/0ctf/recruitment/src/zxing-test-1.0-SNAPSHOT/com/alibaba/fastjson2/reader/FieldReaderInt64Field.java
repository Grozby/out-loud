package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt64Field<T> extends FieldReaderObjectField<T> {
   FieldReaderInt64Field(String fieldName, Class fieldType, int ordinal, long features, String format, Long defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Long fieldValue = jsonReader.readInt64();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.field.set(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt64();
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, (long)value);
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, (long)value);
   }

   @Override
   public void accept(T object, Object value) {
      Long longValue = TypeUtils.toLong(value);
      if (this.schema != null) {
         this.schema.assertValidate(longValue);
      }

      try {
         this.field.set(object, longValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
