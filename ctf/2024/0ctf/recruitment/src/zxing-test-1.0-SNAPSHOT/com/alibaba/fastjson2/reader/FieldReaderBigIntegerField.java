package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.math.BigInteger;

final class FieldReaderBigIntegerField<T> extends FieldReaderObjectField<T> {
   FieldReaderBigIntegerField(
      String fieldName, Class fieldType, int ordinal, long features, String format, BigInteger defaultValue, JSONSchema schema, Field field
   ) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      BigInteger fieldValue = jsonReader.readBigInteger();
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
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      try {
         this.field.set(object, BigInteger.valueOf((long)value));
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
         this.field.set(object, BigInteger.valueOf(value));
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      BigInteger bigInteger = TypeUtils.toBigInteger(value);
      if (this.schema != null) {
         this.schema.assertValidate(bigInteger);
      }

      try {
         this.field.set(object, bigInteger);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
