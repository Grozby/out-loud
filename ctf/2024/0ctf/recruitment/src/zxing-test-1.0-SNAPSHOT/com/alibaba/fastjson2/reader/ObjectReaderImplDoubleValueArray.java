package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

class ObjectReaderImplDoubleValueArray extends ObjectReaderPrimitive {
   static final ObjectReaderImplDoubleValueArray INSTANCE = new ObjectReaderImplDoubleValueArray(null);
   static final long TYPE_HASH = Fnv.hashCode64("[D");
   final Function<double[], Object> builder;

   ObjectReaderImplDoubleValueArray(Function<double[], Object> builder) {
      super(double[].class);
      this.builder = builder;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         double[] values = new double[16];

         int size;
         for (size = 0; !jsonReader.nextIfArrayEnd(); values[size++] = jsonReader.readDoubleValue()) {
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
         double[] array = Arrays.copyOf(values, size);
         return this.builder != null ? this.builder.apply(array) : array;
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
         double[] array = new double[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = jsonReader.readDoubleValue();
         }

         return this.builder != null ? this.builder.apply(array) : array;
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      double[] array = new double[collection.size()];
      int i = 0;

      for (Object item : collection) {
         double value;
         if (item == null) {
            value = 0.0;
         } else if (item instanceof Number) {
            value = ((Number)item).doubleValue();
         } else {
            Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(item.getClass(), double.class);
            if (typeConvert == null) {
               throw new JSONException("can not cast to double " + item.getClass());
            }

            value = (Double)typeConvert.apply(item);
         }

         array[i++] = value;
      }

      return this.builder != null ? this.builder.apply(array) : array;
   }
}
