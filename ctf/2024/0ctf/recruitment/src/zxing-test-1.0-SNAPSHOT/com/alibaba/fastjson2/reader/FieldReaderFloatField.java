package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderFloatField<T> extends FieldReaderObjectField<T> {
   FieldReaderFloatField(String fieldName, Class fieldType, int ordinal, long features, String format, Float defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      Float fieldValue = jsonReader.readFloat();
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
      return jsonReader.readFloat();
   }

   @Override
   public void accept(T object, Object value) {
      Float floatValue = TypeUtils.toFloat(value);
      if (this.schema != null) {
         this.schema.assertValidate(floatValue);
      }

      try {
         this.field.set(object, floatValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
