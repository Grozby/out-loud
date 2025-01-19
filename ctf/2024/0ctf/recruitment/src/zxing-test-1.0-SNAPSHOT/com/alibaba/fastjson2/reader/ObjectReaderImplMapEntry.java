package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

class ObjectReaderImplMapEntry extends ObjectReaderPrimitive {
   final Type keyType;
   final Type valueType;
   volatile ObjectReader keyReader;
   volatile ObjectReader valueReader;

   public ObjectReaderImplMapEntry(Type keyType, Type valueType) {
      super(Entry.class);
      this.keyType = keyType;
      this.valueType = valueType;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int entryCnt = jsonReader.startArray();
      if (entryCnt != 2) {
         throw new JSONException(jsonReader.info("entryCnt must be 2, but " + entryCnt));
      } else {
         Object key;
         if (this.keyType == null) {
            key = jsonReader.readAny();
         } else {
            if (this.keyReader == null) {
               this.keyReader = jsonReader.getObjectReader(this.keyType);
            }

            key = this.keyReader.readObject(jsonReader, fieldType, fieldName, features);
         }

         Object value;
         if (this.valueType == null) {
            value = jsonReader.readAny();
         } else {
            if (this.valueReader == null) {
               this.valueReader = jsonReader.getObjectReader(this.valueType);
            }

            value = this.valueReader.readObject(jsonReader, fieldType, fieldName, features);
         }

         return new SimpleEntry<>(key, value);
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      jsonReader.nextIfObjectStart();
      Object key = jsonReader.readAny();
      jsonReader.nextIfMatch(':');
      Object value;
      if (this.valueType == null) {
         value = jsonReader.readAny();
      } else {
         if (this.valueReader == null) {
            this.valueReader = jsonReader.getObjectReader(this.valueType);
         }

         value = this.valueReader.readObject(jsonReader, fieldType, fieldName, features);
      }

      jsonReader.nextIfObjectEnd();
      jsonReader.nextIfComma();
      return new SimpleEntry<>(key, value);
   }
}
