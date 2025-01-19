package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class FieldReaderAnySetter<T> extends FieldReaderObject<T> {
   FieldReaderAnySetter(Type fieldType, Class fieldClass, int ordinal, long features, String format, JSONSchema schema, Method method) {
      super("$$any$$", fieldType, fieldClass, ordinal, features, format, null, null, schema, method, null, null);
   }

   @Override
   public ObjectReader getItemObjectReader(JSONReader jsonReader) {
      return this.itemReader != null ? this.itemReader : (this.itemReader = jsonReader.getObjectReader(this.fieldType));
   }

   @Override
   public void accept(T object, Object value) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void processExtra(JSONReader jsonReader, Object object) {
      String name = jsonReader.getFieldName();
      ObjectReader itemObjectReader = this.getItemObjectReader(jsonReader);
      Object value = itemObjectReader.readObject(jsonReader, this.fieldType, this.fieldName, 0L);

      try {
         this.method.invoke(object, name, value);
      } catch (Exception var7) {
         throw new JSONException(jsonReader.info("any set error"), var7);
      }
   }

   @Override
   public void acceptExtra(Object object, String name, Object value) {
      try {
         this.method.invoke(object, name, value);
      } catch (Exception var5) {
         throw new JSONException("any set error");
      }
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      throw new UnsupportedOperationException();
   }
}
