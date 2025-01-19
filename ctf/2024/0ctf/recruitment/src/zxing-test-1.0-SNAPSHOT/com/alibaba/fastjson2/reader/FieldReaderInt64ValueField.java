package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

class FieldReaderInt64ValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderInt64ValueField(String fieldName, Class fieldType, int ordinal, long features, String format, Long defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      long fieldLong = jsonReader.readInt64Value();
      if (this.schema != null) {
         this.schema.assertValidate(fieldLong);
      }

      JDKUtils.UNSAFE.putLong(object, this.fieldOffset, fieldLong);
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      this.readFieldValue(jsonReader, object);
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, (long)value);
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, (long)value);
   }

   @Override
   public void accept(T object, Object value) {
      long longValue = TypeUtils.toLongValue(value);
      if (this.schema != null) {
         this.schema.assertValidate(longValue);
      }

      JDKUtils.UNSAFE.putLong(object, this.fieldOffset, longValue);
   }
}
