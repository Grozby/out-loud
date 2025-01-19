package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt8Field<T> extends FieldReaderObjectField<T> {
   FieldReaderInt8Field(String fieldName, Class fieldType, int ordinal, long features, String format, Byte defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Integer fieldInt = jsonReader.readInt32();
      if (this.schema != null) {
         this.schema.assertValidate(fieldInt);
      }

      try {
         this.field.set(object, fieldInt == null ? null : fieldInt.byteValue());
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, short value) {
      this.accept(object, Byte.valueOf((byte)value));
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, Byte.valueOf((byte)((int)value)));
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, Byte.valueOf((byte)((int)value)));
   }

   @Override
   public void accept(T object, int value) {
      this.accept(object, Byte.valueOf((byte)value));
   }

   @Override
   public void accept(T object, long value) {
      this.accept(object, Byte.valueOf((byte)((int)value)));
   }

   @Override
   public void accept(T object, Object value) {
      Byte byteValue = TypeUtils.toByte(value);
      if (this.schema != null) {
         this.schema.assertValidate(byteValue);
      }

      try {
         this.field.set(object, byteValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (byte)jsonReader.readInt32Value();
   }
}
