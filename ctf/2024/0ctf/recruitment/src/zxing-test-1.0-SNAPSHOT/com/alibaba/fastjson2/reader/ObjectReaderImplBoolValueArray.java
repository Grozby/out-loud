package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Arrays;

class ObjectReaderImplBoolValueArray extends ObjectReaderPrimitive {
   static final ObjectReaderImplBoolValueArray INSTANCE = new ObjectReaderImplBoolValueArray();
   static final long TYPE_HASH = Fnv.hashCode64("[Z");

   ObjectReaderImplBoolValueArray() {
      super(boolean[].class);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         boolean[] values = new boolean[16];

         int size;
         for (size = 0; !jsonReader.nextIfArrayEnd(); values[size++] = jsonReader.readBoolValue()) {
            int minCapacity = size + 1;
            if (minCapacity - values.length > 0) {
               int oldCapacity = values.length;
               int newCapacity = oldCapacity + (oldCapacity >> 1);
               if (newCapacity - minCapacity < 0) {
                  newCapacity = minCapacity;
               }

               values = Arrays.copyOf(values, newCapacity);
            }
         }

         jsonReader.nextIfComma();
         return Arrays.copyOf(values, size);
      } else if (jsonReader.isString()) {
         String str = jsonReader.readString();
         if (str.isEmpty()) {
            return null;
         } else {
            throw new JSONException(jsonReader.info("not support input " + str));
         }
      } else {
         throw new JSONException(jsonReader.info("TODO"));
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfMatch((byte)-110)) {
         long typeHashCode = jsonReader.readTypeHashCode();
         if (typeHashCode != TYPE_HASH) {
            throw new JSONException("not support autoType : " + jsonReader.getString());
         }
      }

      int entryCnt = jsonReader.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         boolean[] array = new boolean[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = jsonReader.readBoolValue();
         }

         return array;
      }
   }
}
