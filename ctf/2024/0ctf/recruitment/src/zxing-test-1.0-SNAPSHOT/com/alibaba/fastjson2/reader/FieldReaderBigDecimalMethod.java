package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Locale;

final class FieldReaderBigDecimalMethod<T> extends FieldReaderObject<T> {
   FieldReaderBigDecimalMethod(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      BigDecimal defaultValue,
      JSONSchema schema,
      Method method
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, null, null);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      BigDecimal fieldValue = jsonReader.readBigDecimal();
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
      BigDecimal decimalValue = TypeUtils.toBigDecimal(value);
      if (this.schema != null) {
         this.schema.assertValidate(decimalValue);
      }

      try {
         this.method.invoke(object, decimalValue);
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
         this.method.invoke(object, BigDecimal.valueOf((long)value));
      } catch (Exception var4) {
         throw new JSONException("set " + this.fieldName + " error", var4);
      }
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      try {
         this.method.invoke(object, BigDecimal.valueOf(value));
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
