package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class FieldReaderCollectionMethodReadOnly<T> extends FieldReaderObject<T> {
   FieldReaderCollectionMethodReadOnly(
      String fieldName, Type fieldType, Class fieldClass, int ordinal, long features, String format, JSONSchema schema, Method setter, Field field
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, null, null, schema, setter, field, null);
      Type itemType = null;
      if (fieldType instanceof ParameterizedType) {
         Type[] actualTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
         if (actualTypeArguments.length > 0) {
            itemType = actualTypeArguments[0];
         }
      }

      this.itemType = itemType;
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         Collection collection;
         try {
            collection = (Collection)this.method.invoke(object);
         } catch (Exception var8) {
            throw new JSONException("set " + this.fieldName + " error", var8);
         }

         if (collection != Collections.EMPTY_LIST && collection != Collections.EMPTY_SET && collection != null && !collection.equals(value)) {
            String name = collection.getClass().getName();
            if (!"java.util.Collections$UnmodifiableRandomAccessList".equals(name)
               && !"java.util.Arrays$ArrayList".equals(name)
               && !"java.util.Collections$SingletonList".equals(name)
               && !name.startsWith("java.util.ImmutableCollections$")
               && !name.startsWith("java.util.Collections$Unmodifiable")) {
               if (value != collection) {
                  for (Object item : (Collection)value) {
                     if (item == null) {
                        collection.add(null);
                     } else {
                        if (item instanceof Map && this.itemType instanceof Class && !((Class)this.itemType).isAssignableFrom(item.getClass())) {
                           if (this.itemReader == null) {
                              this.itemReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(this.itemType);
                           }

                           item = this.itemReader.createInstance((Map)item, 0L);
                        }

                        collection.add(item);
                     }
                  }

                  if (this.schema != null) {
                     this.schema.assertValidate(collection);
                  }
               }
            }
         } else {
            if (this.schema != null) {
               this.schema.assertValidate(collection);
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

      Object value = jsonReader.jsonb
         ? this.initReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, 0L)
         : this.initReader.readObject(jsonReader, this.fieldType, this.fieldName, 0L);
      this.accept(object, value);
   }
}
