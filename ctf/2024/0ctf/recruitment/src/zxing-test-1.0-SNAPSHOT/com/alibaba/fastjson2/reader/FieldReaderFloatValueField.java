package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderFloatValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderFloatValueField(String fieldName, Class fieldType, int ordinal, long features, String format, Float defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      float fieldFloat = jsonReader.readFloatValue();
      if (this.schema != null) {
         this.schema.assertValidate((double)fieldFloat);
      }

      try {
         this.field.setFloat(object, fieldFloat);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readFloatValue();
   }

   @Override
   public void accept(T object, Object value) {
      float floatValue = TypeUtils.toFloatValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((double)floatValue);
      }

      try {
         this.field.setFloat(object, floatValue);
      } catch (Exception var5) {
         throw new JSONException("set " + this.fieldName + " error", var5);
      }
   }
}
