package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;

final class FieldReaderInt32ValueArrayFinalField<T> extends FieldReaderObjectField<T> {
   FieldReaderInt32ValueArrayFinalField(
      String fieldName, Class fieldType, int ordinal, long features, String format, int[] defaultValue, JSONSchema schema, Field field
   ) {
      super(fieldName, fieldType, fieldType, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (!jsonReader.readIfNull()) {
         int[] array;
         try {
            array = (int[])this.field.get(object);
         } catch (Exception var6) {
            throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var6);
         }

         if (jsonReader.nextIfArrayStart()) {
            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               int value = jsonReader.readInt32Value();
               if (array != null && i < array.length) {
                  array[i] = value;
               }
            }
         }
      }
   }
}
