package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;

final class FieldReaderDoubleMethod<T> extends FieldReaderObject<T> {
   FieldReaderDoubleMethod(String fieldName, int ordinal, long features, String format, Double defaultValue, JSONSchema schema, Method setter) {
      super(fieldName, Double.class, Double.class, ordinal, features, format, null, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Double fieldValue = jsonReader.readDouble();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      if (fieldValue != null || this.defaultValue == null) {
         try {
            this.method.invoke(object, fieldValue);
         } catch (Exception var5) {
            throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
         }
      }
   }

   @Override
   public void accept(T object, Object value) {
      Double doubleValue = TypeUtils.toDouble(value);
      if (this.schema != null) {
         this.schema.assertValidate(doubleValue);
      }

      try {
         this.method.invoke(object, doubleValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
