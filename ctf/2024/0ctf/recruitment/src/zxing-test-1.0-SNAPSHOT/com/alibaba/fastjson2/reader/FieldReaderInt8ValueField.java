package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt8ValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderInt8ValueField(String fieldName, Class fieldType, int ordinal, long features, String format, Byte defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      try {
         this.field.setByte(object, (byte)fieldInt);
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
         this.field.setByte(object, (byte)value);
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
         this.field.setByte(object, (byte)((int)value));
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public void accept(T object, Object value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      try {
         this.field.setByte(object, TypeUtils.toByteValue(value));
      } catch (Exception var4) {
         throw new JSONException("set " + this.fieldName + " error", var4);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (byte)jsonReader.readInt32Value();
   }
}
