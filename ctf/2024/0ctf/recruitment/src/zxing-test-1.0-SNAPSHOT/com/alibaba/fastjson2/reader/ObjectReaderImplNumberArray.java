package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

class ObjectReaderImplNumberArray extends ObjectReaderPrimitive {
   static final ObjectReaderImplNumberArray INSTANCE = new ObjectReaderImplNumberArray();

   public ObjectReaderImplNumberArray() {
      super(Number[].class);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         Number[] values = new Number[16];

         int size;
         for (size = 0; !jsonReader.nextIfArrayEnd(); values[size++] = jsonReader.readNumber()) {
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
      } else {
         throw new JSONException(jsonReader.info("TODO"));
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int entryCnt = jsonReader.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         Number[] array = new Number[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = jsonReader.readNumber();
         }

         return array;
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      Number[] array = new Number[collection.size()];
      int i = 0;

      for (Object item : collection) {
         Number value;
         if (item != null && !(item instanceof Number)) {
            Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(item.getClass(), Number.class);
            if (typeConvert == null) {
               throw new JSONException("can not cast to Number " + item.getClass());
            }

            value = (Number)typeConvert.apply(item);
         } else {
            value = (Number)item;
         }

         array[i++] = value;
      }

      return array;
   }
}
