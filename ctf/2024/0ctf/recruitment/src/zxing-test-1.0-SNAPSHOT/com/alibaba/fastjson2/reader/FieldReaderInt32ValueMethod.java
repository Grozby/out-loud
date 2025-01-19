package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

final class FieldReaderInt32ValueMethod<T> extends FieldReaderObject<T> {
   FieldReaderInt32ValueMethod(
      String fieldName, Type fieldType, Class fieldClass, int ordinal, long features, String format, Integer defaultValue, JSONSchema schema, Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, null, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      try {
         this.method.invoke(object, fieldInt);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      try {
         this.method.invoke(object, fieldInt);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      int intValue = TypeUtils.toIntValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)intValue);
      }

      try {
         this.method.invoke(object, intValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      try {
         this.method.invoke(object, (int)value);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32Value();
   }
}
