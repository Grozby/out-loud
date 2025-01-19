package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

class ObjectReaderImplGenericArray implements ObjectReader {
   final Type arrayType;
   final Class arrayClass;
   final Type itemType;
   final Class<?> componentClass;
   ObjectReader itemObjectReader;
   final String arrayClassName;
   final long arrayClassNameHash;

   public ObjectReaderImplGenericArray(GenericArrayType genericType) {
      this.arrayType = genericType;
      this.arrayClass = TypeUtils.getClass(this.arrayType);
      this.itemType = genericType.getGenericComponentType();
      this.componentClass = TypeUtils.getMapping(this.itemType);
      this.arrayClassName = "[" + TypeUtils.getTypeName(this.componentClass);
      this.arrayClassNameHash = Fnv.hashCode64(this.arrayClassName);
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfMatch((byte)-110)) {
         long typeHash = jsonReader.readTypeHashCode();
         if (typeHash != this.arrayClassNameHash) {
            String typeName = jsonReader.getString();
            throw new JSONException("not support input typeName " + typeName);
         }
      }

      int entryCnt = jsonReader.startArray();
      if (entryCnt > 0 && this.itemObjectReader == null) {
         this.itemObjectReader = jsonReader.getContext().getObjectReader(this.itemType);
      }

      Object array = Array.newInstance(this.componentClass, entryCnt);

      for (int i = 0; i < entryCnt; i++) {
         Object item = this.itemObjectReader.readJSONBObject(jsonReader, this.itemType, null, 0L);
         Array.set(array, i, item);
      }

      return array;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (this.itemObjectReader == null) {
         this.itemObjectReader = jsonReader.getContext().getObjectReader(this.itemType);
      }

      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else {
         char ch = jsonReader.current();
         if (ch == '"') {
            if (fieldType instanceof GenericArrayType && ((GenericArrayType)fieldType).getGenericComponentType() == byte.class) {
               byte[] bytes;
               if ((jsonReader.features(features) & JSONReader.Feature.Base64StringAsByteArray.mask) != 0L) {
                  String str = jsonReader.readString();
                  bytes = Base64.getDecoder().decode(str);
               } else {
                  bytes = jsonReader.readBinary();
               }

               return bytes;
            } else {
               String str = jsonReader.readString();
               if (str.isEmpty()) {
                  return null;
               } else {
                  throw new JSONException(jsonReader.info());
               }
            }
         } else {
            List<Object> list = new ArrayList<>();
            if (ch != '[') {
               throw new JSONException(jsonReader.info());
            } else {
               jsonReader.next();

               while (!jsonReader.nextIfArrayEnd()) {
                  Object item;
                  if (this.itemObjectReader != null) {
                     item = this.itemObjectReader.readObject(jsonReader, this.itemType, null, 0L);
                  } else {
                     if (this.itemType != String.class) {
                        throw new JSONException(jsonReader.info("TODO : " + this.itemType));
                     }

                     item = jsonReader.readString();
                  }

                  list.add(item);
                  jsonReader.nextIfComma();
               }

               jsonReader.nextIfComma();
               Object array = Array.newInstance(this.componentClass, list.size());

               for (int i = 0; i < list.size(); i++) {
                  Array.set(array, i, list.get(i));
               }

               return array;
            }
         }
      }
   }
}
