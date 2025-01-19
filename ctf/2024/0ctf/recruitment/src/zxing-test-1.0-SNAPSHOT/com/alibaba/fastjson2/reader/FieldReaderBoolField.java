package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderBoolField<T> extends FieldReaderObjectField<T> {
   FieldReaderBoolField(String fieldName, Class fieldType, int ordinal, long features, String format, Boolean defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Boolean fieldValue = jsonReader.readBool();

      try {
         this.field.set(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, boolean value) {
      this.accept(object, Boolean.valueOf(value));
   }

   @Override
   public void accept(T object, int value) {
      this.accept(object, TypeUtils.toBoolean(value));
   }

   @Override
   public void accept(T object, Object value) {
      Boolean booleanValue = TypeUtils.toBoolean(value);
      if (this.schema != null) {
         this.schema.assertValidate(booleanValue);
      }

      try {
         this.field.set(object, booleanValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readBool();
   }
}
