package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;

final class FieldReaderCharValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderCharValueField(String fieldName, int ordinal, long features, String format, Character defaultValue, JSONSchema schema, Field field) {
      super(fieldName, char.class, char.class, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      char ch = jsonReader.readCharValue();
      if (ch != 0 || !jsonReader.wasNull()) {
         this.accept(object, ch);
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      String str = jsonReader.readString();
      return str != null && !str.isEmpty() ? str.charAt(0) : '\u0000';
   }

   @Override
   public void accept(T object, Object value) {
      char charValue;
      if (value instanceof String) {
         charValue = ((String)value).charAt(0);
      } else {
         if (!(value instanceof Character)) {
            throw new JSONException("cast to char error");
         }

         charValue = (Character)value;
      }

      this.accept(object, charValue);
   }
}
