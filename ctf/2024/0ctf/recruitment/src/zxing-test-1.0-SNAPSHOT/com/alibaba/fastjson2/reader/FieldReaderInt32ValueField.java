package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

class FieldReaderInt32ValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderInt32ValueField(String fieldName, Class fieldType, int ordinal, String format, Integer defaultValue, JSONSchema schema, Field field) {
      super(fieldName, fieldType, fieldType, ordinal, 0L, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      if (this.schema != null) {
         this.schema.assertValidate((long)fieldInt);
      }

      JDKUtils.UNSAFE.putInt(object, this.fieldOffset, fieldInt);
   }

   @Override
   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      int fieldInt = jsonReader.readInt32Value();
      this.accept(object, fieldInt);
   }

   @Override
   public void accept(T object, float value) {
      this.accept(object, (int)value);
   }

   @Override
   public void accept(T object, double value) {
      this.accept(object, (int)value);
   }

   @Override
   public void accept(T object, Object value) {
      int intValue = TypeUtils.toIntValue(value);
      if (this.schema != null) {
         this.schema.assertValidate((long)intValue);
      }

      JDKUtils.UNSAFE.putInt(object, this.fieldOffset, intValue);
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      int intValue = (int)value;
      JDKUtils.UNSAFE.putInt(object, this.fieldOffset, intValue);
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readInt32Value();
   }
}
