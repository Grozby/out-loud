package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

class FieldReaderMapMethodReadOnly<T> extends FieldReaderMapMethod<T> {
   FieldReaderMapMethodReadOnly(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      JSONSchema schema,
      Method method,
      Field field,
      String arrayToMapKey,
      BiConsumer arrayToMapDuplicateHandler
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, null, null, schema, method, field, null, arrayToMapKey, arrayToMapDuplicateHandler);
   }

   @Override
   public ObjectReader getItemObjectReader(JSONReader jsonReader) {
      if (this.itemReader != null) {
         return this.itemReader;
      } else {
         ObjectReader objectReader = this.getObjectReader(jsonReader);
         if (objectReader instanceof ObjectReaderImplMap) {
            return this.itemReader = ObjectReaderImplString.INSTANCE;
         } else if (objectReader instanceof ObjectReaderImplMapTyped) {
            Type valueType = ((ObjectReaderImplMapTyped)objectReader).valueType;
            return this.itemReader = jsonReader.getObjectReader(valueType);
         } else {
            return ObjectReaderImplObject.INSTANCE;
         }
      }
   }

   @Override
   public void accept(T object, Object value) {
      if (value != null) {
         Map map = this.getReadOnlyMap(object);
         if (map != Collections.EMPTY_MAP && map != null) {
            String name = map.getClass().getName();
            if (!"java.util.Collections$UnmodifiableMap".equals(name)) {
               if (this.schema != null) {
                  this.schema.assertValidate(value);
               }

               map.putAll((Map)value);
            }
         }
      }
   }

   @Override
   public void processExtra(JSONReader jsonReader, Object object) {
      String name = jsonReader.getFieldName();
      ObjectReader itemObjectReader = this.getItemObjectReader(jsonReader);
      Object value = itemObjectReader.readObject(jsonReader, this.getItemType(), this.fieldName, 0L);
      this.getReadOnlyMap(object).put(name, value);
   }

   @Override
   public void acceptExtra(Object object, String name, Object value) {
      this.getReadOnlyMap(object).put(name, value);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (this.arrayToMapKey != null && jsonReader.isArray()) {
         Map map = this.getReadOnlyMap(object);
         List array = jsonReader.readArray(this.valueType);
         arrayToMap(
            map,
            array,
            this.arrayToMapKey,
            this.namingStrategy,
            JSONFactory.getObjectReader(this.valueType, this.features | this.features),
            this.arrayToMapDuplicateHandler
         );
      } else {
         if (this.initReader == null) {
            this.initReader = jsonReader.getContext().getObjectReader(this.fieldType);
         }

         Object value;
         if (jsonReader.jsonb) {
            value = this.initReader.readJSONBObject(jsonReader, this.getItemType(), this.fieldName, this.features);
         } else {
            value = this.initReader.readObject(jsonReader, this.getItemType(), this.fieldName, this.features);
         }

         this.accept(object, value);
      }
   }

   @Override
   protected void acceptAny(T object, Object fieldValue, long features) {
      if (this.arrayToMapKey != null && fieldValue instanceof Collection) {
         Map map = this.getReadOnlyMap(object);
         arrayToMap(
            map,
            (Collection)fieldValue,
            this.arrayToMapKey,
            this.namingStrategy,
            JSONFactory.getObjectReader(this.valueType, this.features | features),
            this.arrayToMapDuplicateHandler
         );
      } else {
         super.acceptAny(object, fieldValue, features);
      }
   }

   private Map getReadOnlyMap(Object object) {
      try {
         return (Map)this.method.invoke(object);
      } catch (Exception var4) {
         throw new JSONException("set " + this.fieldName + " error");
      }
   }
}
