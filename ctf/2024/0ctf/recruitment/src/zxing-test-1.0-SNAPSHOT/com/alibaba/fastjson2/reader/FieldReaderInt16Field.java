package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt16Field<T> extends FieldReaderObjectField<T> {
   FieldReaderInt16Field(String fieldName, Class fieldType, int ordinal, long features, String format, Short defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int intValue = jsonReader.readInt32Value();
      Short fieldValue;
      if (jsonReader.wasNull()) {
         fieldValue = null;
      } else {
         fieldValue = (short)intValue;
      }

      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.field.set(object, fieldValue);
      } catch (Exception var6) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
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
   public void accept(T object, int value) {
      this.accept(object, Short.valueOf((short)value));
   }

   @Override
   public void accept(T object, long value) {
      this.accept(object, Short.valueOf((short)((int)value)));
   }

   @Override
   public void accept(T object, Object value) {
      Short shortValue = TypeUtils.toShort(value);
      if (this.schema != null) {
         this.schema.assertValidate(shortValue);
      }

      try {
         this.field.set(object, shortValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return (short)jsonReader.readInt32Value();
   }
}
