package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Function;

public final class ObjectReaderImplListInt64 implements ObjectReader {
   final Class listType;
   final Class instanceType;
   final long instanceTypeHash;

   public ObjectReaderImplListInt64(Class listType, Class instanceType) {
      this.listType = listType;
      this.instanceType = instanceType;
      this.instanceTypeHash = Fnv.hashCode64(TypeUtils.getTypeName(instanceType));
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType == ArrayList.class) {
         return new ArrayList();
      } else if (this.instanceType == LinkedList.class) {
         return new LinkedList();
      } else {
         try {
            return this.instanceType.newInstance();
         } catch (IllegalAccessException | InstantiationException var4) {
            throw new JSONException("create list error, type " + this.instanceType);
         }
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      Collection list = (Collection)this.createInstance(features);

      for (Object item : collection) {
         list.add(TypeUtils.toLong(item));
      }

      return list;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         Class listType = this.listType;
         ObjectReader objectReader = jsonReader.checkAutoType(listType, this.instanceTypeHash, features);
         if (objectReader != null) {
            listType = objectReader.getObjectClass();
         }

         Collection list;
         if (listType == ArrayList.class) {
            list = new ArrayList();
         } else if (listType == JSONArray.class) {
            list = new JSONArray();
         } else if (listType != null && listType != this.listType) {
            list = (Collection)objectReader.createInstance(features);
         } else {
            list = (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features);
         }

         int entryCnt = jsonReader.startArray();

         for (int i = 0; i < entryCnt; i++) {
            list.add(jsonReader.readInt64());
         }

         if (objectReader != null) {
            Function buildFunction = objectReader.getBuildFunction();
            if (buildFunction != null) {
               list = (Collection)buildFunction.apply(list);
            }
         }

         return list;
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.isString()) {
         Collection list = (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features);
         String str = jsonReader.readString();
         if (str.indexOf(44) != -1) {
            String[] items = str.split(",");

            for (int i = 0; i < items.length; i++) {
               String item = items[i];
               list.add(Long.parseLong(item));
            }
         } else {
            list.add(Long.parseLong(str));
         }

         jsonReader.nextIfComma();
         return list;
      } else {
         boolean set = jsonReader.nextIfSet();
         if (jsonReader.current() != '[') {
            throw new JSONException(jsonReader.info("format error"));
         } else {
            jsonReader.next();
            Collection list;
            if (set && this.instanceType == Collection.class) {
               list = new LinkedHashSet();
            } else {
               list = (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features);
            }

            while (!jsonReader.isEnd()) {
               if (jsonReader.nextIfArrayEnd()) {
                  jsonReader.nextIfComma();
                  return list;
               }

               list.add(jsonReader.readInt64());
            }

            throw new JSONException(jsonReader.info("illegal input error"));
         }
      }
   }
}
