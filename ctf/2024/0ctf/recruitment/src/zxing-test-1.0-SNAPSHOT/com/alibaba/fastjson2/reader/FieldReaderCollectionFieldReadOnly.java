package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

final class FieldReaderCollectionFieldReadOnly<T> extends FieldReaderObjectField<T> {
   FieldReaderCollectionFieldReadOnly(
      String fieldName, Type fieldType, Class fieldClass, int ordinal, long features, String format, JSONSchema schema, Field field
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, null, null, schema, field);
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         Collection collection;
         try {
            collection = (Collection)this.field.get(object);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }

         if (collection != Collections.EMPTY_LIST && collection != Collections.EMPTY_SET && collection != null && !collection.equals(value)) {
            String name = collection.getClass().getName();
            if (!"java.util.Collections$UnmodifiableRandomAccessList".equals(name)
               && !"java.util.Arrays$ArrayList".equals(name)
               && !"java.util.Collections$SingletonList".equals(name)
               && !name.startsWith("java.util.ImmutableCollections$")) {
               collection.addAll((Collection)value);
            }
         }
      }
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (this.initReader == null) {
         this.initReader = jsonReader.getContext().getObjectReader(this.fieldType);
      }

      Object value = this.initReader.readObject(jsonReader, this.fieldType, this.fieldName, 0L);
      this.accept(object, value);
   }
}
