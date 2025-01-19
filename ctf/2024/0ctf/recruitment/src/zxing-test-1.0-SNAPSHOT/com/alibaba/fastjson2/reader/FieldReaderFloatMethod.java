package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.util.Locale;

final class FieldReaderFloatMethod<T> extends FieldReaderObject<T> {
   FieldReaderFloatMethod(String fieldName, int ordinal, long features, String format, Locale locale, Float defaultValue, JSONSchema schema, Method setter) {
      super(fieldName, Float.class, Float.class, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Float fieldValue = jsonReader.readFloat();
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
      Float floatValue = TypeUtils.toFloat(value);
      if (this.schema != null) {
         this.schema.assertValidate(floatValue);
      }

      try {
         this.method.invoke(object, floatValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
