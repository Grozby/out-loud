package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderInt32Field<T> extends FieldReaderObjectField<T> {
   FieldReaderInt32Field(String fieldName, Class fieldType, int ordinal, long features, String format, Integer defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Integer fieldValue = jsonReader.readInt32();
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
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32();
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, (int)value);
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, (int)value);
   }

   @Override
   public void accept(T object, Object value) {
      Integer integer = TypeUtils.toInteger(value);
      if (this.schema != null) {
         this.schema.assertValidate(integer);
      }

      if (value != null || (this.features & JSONReader.Feature.IgnoreSetNullValue.mask) == 0L) {
         try {
            this.field.set(object, integer);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }
      }
   }
}
