package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public final class ObjectReaderImplInt64Array extends ObjectReaderPrimitive {
   static final ObjectReaderImplInt64Array INSTANCE = new ObjectReaderImplInt64Array();
   public static final long HASH_TYPE = Fnv.hashCode64("[Long");

   ObjectReaderImplInt64Array() {
      super(Long[].class);
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      Long[] array = new Long[collection.size()];
      int i = 0;

      for (Object item : collection) {
         Long value;
         if (item == null) {
            value = null;
         } else if (item instanceof Number) {
            value = ((Number)item).longValue();
         } else {
            Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(item.getClass(), Long.class);
            if (typeConvert == null) {
               throw new JSONException("can not cast to Integer " + item.getClass());
            }

            value = (Long)typeConvert.apply(item);
         }

         array[i++] = value;
      }

      return array;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         Long[] values = new Long[16];

         int size;
         for (size = 0; !jsonReader.nextIfArrayEnd(); values[size++] = jsonReader.readInt64()) {
            if (jsonReader.isEnd()) {
               throw new JSONException(jsonReader.info("input end"));
            }

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
      } else if (jsonReader.isNumber()) {
         return new Long[]{jsonReader.readInt64()};
      } else {
         throw new JSONException(jsonReader.info("TODO"));
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfMatch((byte)-110)) {
         long typeHash = jsonReader.readTypeHashCode();
         if (typeHash != HASH_TYPE
            && typeHash != ObjectReaderImplInt64ValueArray.HASH_TYPE
            && typeHash != ObjectReaderImplInt32Array.HASH_TYPE
            && typeHash != ObjectReaderImplInt32ValueArray.HASH_TYPE) {
            throw new JSONException(jsonReader.info("not support type " + jsonReader.getString()));
         }
      }

      int entryCnt = jsonReader.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         Long[] array = new Long[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = jsonReader.readInt64();
         }

         return array;
      }
   }
}
