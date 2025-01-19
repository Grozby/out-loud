package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt16ValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderInt16ValueField(String fieldName, Class fieldType, int ordinal, long features, String format, Short defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      try {
         this.field.setShort(object, (short)fieldInt);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, Short.valueOf((short)((int)value)));
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, Short.valueOf((short)((int)value)));
   }

   @Override
   public void accept(T object, Object value) {
      short shortValue = TypeUtils.toShortValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)shortValue);
      }

      try {
         this.field.setShort(object, shortValue);
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
         this.field.setShort(object, (short)value);
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
         this.field.setShort(object, (short)((int)value));
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (short)jsonReader.readInt32Value();
   }
}
