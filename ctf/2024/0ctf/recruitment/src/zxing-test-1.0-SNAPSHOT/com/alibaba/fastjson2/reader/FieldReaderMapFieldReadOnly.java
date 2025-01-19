package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

class FieldReaderMapFieldReadOnly<T> extends FieldReaderMapField<T> {
   FieldReaderMapFieldReadOnly(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      JSONSchema schema,
      Field field,
      String arrayToMapKey,
      BiConsumer arrayToMapDuplicateHandler
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, null, null, schema, field, arrayToMapKey, arrayToMapDuplicateHandler);
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
         Map map;
         try {
            map = (Map)this.field.get(object);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }

         if (map != Collections.EMPTY_MAP && map != null) {
            String name = map.getClass().getName();
            if (!"java.util.Collections$UnmodifiableMap".equals(name)) {
               map.putAll((Map)value);
            }
         }
      }
   }

   @Override
   public void processExtra(JSONReader jsonReader, Object object) {
      Map map;
      try {
         map = (Map)this.field.get(object);
      } catch (Exception var7) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var7);
      }

      String name = jsonReader.getFieldName();
      ObjectReader itemObjectReader = this.getItemObjectReader(jsonReader);
      Object value = itemObjectReader.readObject(jsonReader, null, name, 0L);
      map.put(name, value);
   }

   @Override
   public void acceptExtra(Object object, String name, Object value) {
      Map map;
      try {
         map = (Map)this.field.get(object);
      } catch (Exception var6) {
         throw new JSONException("set " + this.fieldName + " error");
      }

      map.put(name, value);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (this.arrayToMapKey != null && jsonReader.isArray()) {
         Map map;
         try {
            map = (Map)this.field.get(object);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error");
         }

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
            value = this.initReader.readJSONBObject(jsonReader, this.fieldType, this.fieldName, this.features);
         } else {
            value = this.initReader.readObject(jsonReader, this.fieldType, this.fieldName, this.features);
         }

         this.accept(object, value);
      }
   }

   @Override
   protected void acceptAny(T object, Object fieldValue, long features) {
      if (this.arrayToMapKey != null && fieldValue instanceof Collection) {
         Map map;
         try {
            map = (Map)this.field.get(object);
         } catch (Exception var7) {
            throw new JSONException("set " + this.fieldName + " error");
         }

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
}
