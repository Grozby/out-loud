package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class ObjectReaderImplField implements ObjectReader {
   static final long HASH_DECLARING_CLASS = Fnv.hashCode64("declaringClass");
   static final long HASH_NAME = Fnv.hashCode64("name");

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readObject(jsonReader, fieldType, fieldName, features);
   }

   @Override
   public Object readArrayMappingJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int entryCount = jsonReader.startArray();
      if (entryCount != 2) {
         throw new JSONException("not support input " + jsonReader.info());
      } else {
         String declaringClassName = jsonReader.readString();
         String methodName = jsonReader.readString();
         return this.getField(jsonReader.getContext().getFeatures() | features, methodName, declaringClassName);
      }
   }

   @Override
   public Object readArrayMappingObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      boolean arrayStart = jsonReader.nextIfArrayStart();
      if (!arrayStart) {
         throw new JSONException("not support input " + jsonReader.info());
      } else {
         String declaringClassName = jsonReader.readString();
         String methodName = jsonReader.readString();
         boolean arrayEnd = jsonReader.nextIfArrayEnd();
         if (!arrayEnd) {
            throw new JSONException("not support input " + jsonReader.info());
         } else {
            jsonReader.nextIfComma();
            return this.getField(jsonReader.getContext().getFeatures() | features, methodName, declaringClassName);
         }
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      boolean objectStart = jsonReader.nextIfObjectStart();
      if (!objectStart) {
         if (jsonReader.isSupportBeanArray(features)) {
            return jsonReader.jsonb
               ? this.readArrayMappingJSONBObject(jsonReader, fieldType, fieldName, features)
               : this.readArrayMappingObject(jsonReader, fieldType, fieldName, features);
         } else {
            throw new JSONException("not support input " + jsonReader.info());
         }
      } else {
         String methodName = null;
         String declaringClassName = null;

         while (!jsonReader.nextIfObjectEnd()) {
            long nameHashCode = jsonReader.readFieldNameHashCode();
            if (nameHashCode == HASH_DECLARING_CLASS) {
               declaringClassName = jsonReader.readString();
            } else if (nameHashCode == HASH_NAME) {
               methodName = jsonReader.readString();
            } else {
               jsonReader.skipValue();
            }
         }

         if (!jsonReader.jsonb) {
            jsonReader.nextIfComma();
         }

         return this.getField(jsonReader.getContext().getFeatures() | features, methodName, declaringClassName);
      }
   }

   private Field getField(long features, String methodName, String declaringClassName) {
      boolean supportClassForName = (features & JSONReader.Feature.SupportClassForName.mask) != 0L;
      if (supportClassForName) {
         Class declaringClass = TypeUtils.loadClass(declaringClassName);

         try {
            return declaringClass.getDeclaredField(methodName);
         } catch (NoSuchFieldException var8) {
            throw new JSONException("method not found", var8);
         }
      } else {
         throw new JSONException("ClassForName not support");
      }
   }
}
