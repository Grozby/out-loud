package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;

final class FieldReaderInt8Method<T> extends FieldReaderObject<T> {
   FieldReaderInt8Method(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Byte defaultValue,
      JSONSchema schema,
      Method setter
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, setter, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Integer fieldValue = jsonReader.readInt32();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.method.invoke(object, fieldValue == null ? null : fieldValue.byteValue());
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      Byte byteValue = TypeUtils.toByte(value);
      if (this.schema != null) {
         this.schema.assertValidate(byteValue);
      }

      try {
         this.method.invoke(object, byteValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32();
   }
}
