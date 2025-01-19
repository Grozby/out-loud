package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.GuavaSupport;
import com.alibaba.fastjson2.util.MapMultiValueType;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReaderImplMapMultiValueType implements ObjectReader {
   final Class mapType;
   final Class instanceType;
   final Function builder;
   final MapMultiValueType multiValueType;

   public ObjectReaderImplMapMultiValueType(MapMultiValueType multiValueType) {
      this.multiValueType = multiValueType;
      this.mapType = multiValueType.getMapType();
      Class instanceType = this.mapType;
      Function builder = null;
      if (this.mapType == Map.class || this.mapType == AbstractMap.class || this.mapType == ObjectReaderImplMap.CLASS_SINGLETON_MAP) {
         instanceType = HashMap.class;
      } else if (this.mapType == ObjectReaderImplMap.CLASS_UNMODIFIABLE_MAP) {
         instanceType = LinkedHashMap.class;
      } else if (this.mapType == SortedMap.class
         || this.mapType == ObjectReaderImplMap.CLASS_UNMODIFIABLE_SORTED_MAP
         || this.mapType == ObjectReaderImplMap.CLASS_UNMODIFIABLE_NAVIGABLE_MAP) {
         instanceType = TreeMap.class;
      } else if (this.mapType == ConcurrentMap.class) {
         instanceType = ConcurrentHashMap.class;
      } else if (this.mapType == ConcurrentNavigableMap.class) {
         instanceType = ConcurrentSkipListMap.class;
      } else {
         String var4 = this.mapType.getTypeName();
         switch (var4) {
            case "com.google.common.collect.ImmutableMap":
            case "com.google.common.collect.RegularImmutableMap":
               instanceType = HashMap.class;
               builder = GuavaSupport.immutableMapConverter();
               break;
            case "com.google.common.collect.SingletonImmutableBiMap":
               instanceType = HashMap.class;
               builder = GuavaSupport.singletonBiMapConverter();
               break;
            case "java.util.Collections$SynchronizedMap":
               instanceType = HashMap.class;
               builder = Collections::synchronizedMap;
               break;
            case "java.util.Collections$SynchronizedNavigableMap":
               instanceType = TreeMap.class;
               builder = Collections::synchronizedNavigableMap;
               break;
            case "java.util.Collections$SynchronizedSortedMap":
               instanceType = TreeMap.class;
               builder = Collections::synchronizedSortedMap;
         }
      }

      this.instanceType = instanceType;
      this.builder = builder;
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType != null && !this.instanceType.isInterface()) {
         try {
            return this.instanceType.newInstance();
         } catch (Exception var4) {
            throw new JSONException("create map error", var4);
         }
      } else {
         return new HashMap();
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!jsonReader.nextIfObjectStart()) {
         if (jsonReader.nextIfNullOrEmptyString()) {
            return null;
         } else {
            throw new JSONException(jsonReader.info("expect '{', but '" + jsonReader.current() + "'"));
         }
      } else {
         JSONReader.Context context = jsonReader.getContext();
         long contextFeatures = context.getFeatures() | features;
         Map innerMap = null;
         Map object;
         if (this.instanceType == HashMap.class) {
            Supplier<Map> objectSupplier = context.getObjectSupplier();
            if (this.mapType == Map.class && objectSupplier != null) {
               object = objectSupplier.get();
               innerMap = TypeUtils.getInnerMap(object);
            } else {
               object = new HashMap();
            }
         } else if (this.instanceType == JSONObject.class) {
            object = new JSONObject();
         } else {
            object = (Map)this.createInstance(contextFeatures);
         }

         Type valueType = null;

         for (int i = 0; !jsonReader.nextIfObjectEnd() && !jsonReader.isEnd(); i++) {
            String name;
            if (jsonReader.nextIfNull()) {
               if (!jsonReader.nextIfMatch(':')) {
                  throw new JSONException(jsonReader.info("illegal json"));
               }

               name = null;
            } else {
               name = jsonReader.readFieldName();
               valueType = this.multiValueType.getType(name);
            }

            Object value;
            if (valueType == null) {
               value = jsonReader.readAny();
            } else {
               ObjectReader valueObjectReader = jsonReader.getObjectReader(valueType);
               value = valueObjectReader.readObject(jsonReader, valueType, fieldName, 0L);
            }

            if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
               Object origin;
               if (innerMap != null) {
                  origin = innerMap.put(name, value);
               } else {
                  origin = object.put(name, value);
               }

               if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                  if (origin instanceof Collection) {
                     ((Collection)origin).add(value);
                     object.put(name, origin);
                  } else {
                     JSONArray array = JSONArray.of(origin, value);
                     object.put(name, array);
                  }
               }
            }
         }

         jsonReader.nextIfMatch(',');
         return this.builder != null ? this.builder.apply(object) : object;
      }
   }
}
