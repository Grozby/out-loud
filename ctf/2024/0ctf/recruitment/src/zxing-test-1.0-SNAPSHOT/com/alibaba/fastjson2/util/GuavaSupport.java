package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class GuavaSupport {
   static Class CLASS_IMMUTABLE_MAP;
   static Class CLASS_IMMUTABLE_LIST;
   static Class CLASS_IMMUTABLE_SET;
   static Class CLASS_ARRAYLIST_MULTI_MAP;
   static Supplier FUNC_IMMUTABLE_MAP_OF_0;
   static BiFunction FUNC_IMMUTABLE_MAP_OF_1;
   static Function FUNC_IMMUTABLE_MAP_COPY_OF;
   static Supplier FUNC_IMMUTABLE_LIST_OF_0;
   static Function FUNC_IMMUTABLE_LIST_OF_1;
   static Function FUNC_IMMUTABLE_LIST_COPY_OF;
   static Supplier FUNC_IMMUTABLE_SET_OF_0;
   static Function FUNC_IMMUTABLE_SET_OF_1;
   static Function FUNC_IMMUTABLE_SET_COPY_OF;
   static Supplier FUNC_ARRAYLIST_MULTI_MAP_CREATE;
   static Method METHOD_ARRAYLIST_MULTI_MAP_PUT_ALL;
   static volatile boolean METHOD_ARRAYLIST_MULTI_MAP_ERROR;
   static BiFunction FUNC_SINGLETON_IMMUTABLE_BIMAP;

   public static Function immutableListConverter() {
      return new GuavaSupport.ImmutableListConvertFunction();
   }

   public static Function immutableSetConverter() {
      return new GuavaSupport.ImmutableSetConvertFunction();
   }

   public static Function immutableMapConverter() {
      return new GuavaSupport.ImmutableSingletonMapConvertFunction();
   }

   public static Function singletonBiMapConverter() {
      return new GuavaSupport.SingletonImmutableBiMapConvertFunction();
   }

   public static ObjectWriter createAsMapWriter(Class objectClass) {
      return new GuavaSupport.AsMapWriter(objectClass);
   }

   public static Function createConvertFunction(Class objectClass) {
      String instanceTypeName = objectClass.getName();
      if ("com.google.common.collect.ArrayListMultimap".equals(instanceTypeName)) {
         if (CLASS_ARRAYLIST_MULTI_MAP == null) {
            CLASS_ARRAYLIST_MULTI_MAP = objectClass;
         }

         if (!METHOD_ARRAYLIST_MULTI_MAP_ERROR && FUNC_ARRAYLIST_MULTI_MAP_CREATE == null) {
            try {
               Method method = CLASS_ARRAYLIST_MULTI_MAP.getMethod("create");
               FUNC_ARRAYLIST_MULTI_MAP_CREATE = LambdaMiscCodec.createSupplier(method);
            } catch (Throwable var4) {
               METHOD_ARRAYLIST_MULTI_MAP_ERROR = true;
            }
         }

         if (!METHOD_ARRAYLIST_MULTI_MAP_ERROR && METHOD_ARRAYLIST_MULTI_MAP_PUT_ALL == null) {
            try {
               METHOD_ARRAYLIST_MULTI_MAP_PUT_ALL = CLASS_ARRAYLIST_MULTI_MAP.getMethod("putAll", Object.class, Iterable.class);
            } catch (Throwable var3) {
               METHOD_ARRAYLIST_MULTI_MAP_ERROR = true;
            }
         }

         if (FUNC_ARRAYLIST_MULTI_MAP_CREATE != null && METHOD_ARRAYLIST_MULTI_MAP_PUT_ALL != null) {
            return new GuavaSupport.ArrayListMultimapConvertFunction(FUNC_ARRAYLIST_MULTI_MAP_CREATE, METHOD_ARRAYLIST_MULTI_MAP_PUT_ALL);
         }
      }

      throw new JSONException("create map error : " + objectClass);
   }

   static class ArrayListMultimapConvertFunction implements Function {
      final Supplier method;
      final Method putAllMethod;

      public ArrayListMultimapConvertFunction(Supplier method, Method putAllMethod) {
         this.method = method;
         this.putAllMethod = putAllMethod;
      }

      @Override
      public Object apply(Object o) {
         Map map = (Map)o;
         Object multiMap = this.method.get();

         for (Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            Iterable item = (Iterable)entry.getValue();

            try {
               this.putAllMethod.invoke(multiMap, key, item);
            } catch (Throwable var9) {
               throw new JSONException("putAll ArrayListMultimap error", var9);
            }
         }

         return multiMap;
      }
   }

   static class AsMapWriter implements ObjectWriter {
      final Class objectClass;
      final String typeName;
      final long typeNameHash;
      final Function asMap;
      protected byte[] typeNameJSONB;

      public AsMapWriter(Class objectClass) {
         this.objectClass = objectClass;
         this.typeName = TypeUtils.getTypeName(objectClass);
         this.typeNameHash = Fnv.hashCode64(this.typeName);

         try {
            Method method = objectClass.getMethod("asMap");
            this.asMap = LambdaMiscCodec.createFunction(method);
         } catch (NoSuchMethodException var3) {
            throw new JSONException("create Guava AsMapWriter error", var3);
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Map map = (Map)this.asMap.apply(object);
         jsonWriter.write(map);
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            if (this.typeNameJSONB == null) {
               this.typeNameJSONB = JSONB.toBytes(this.typeName);
            }

            jsonWriter.writeTypeName(this.typeNameJSONB, this.typeNameHash);
         }

         Map map = (Map)this.asMap.apply(object);
         jsonWriter.write(map);
      }
   }

   static class ImmutableListConvertFunction implements Function {
      @Override
      public Object apply(Object object) {
         if (GuavaSupport.CLASS_IMMUTABLE_LIST == null) {
            GuavaSupport.CLASS_IMMUTABLE_LIST = TypeUtils.loadClass("com.google.common.collect.ImmutableList");
         }

         if (GuavaSupport.CLASS_IMMUTABLE_LIST == null) {
            throw new JSONException("class not found : com.google.common.collect.ImmutableList");
         } else {
            List list = (List)object;
            if (list.isEmpty()) {
               if (GuavaSupport.FUNC_IMMUTABLE_LIST_OF_0 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_LIST.getMethod("of");
                     GuavaSupport.FUNC_IMMUTABLE_LIST_OF_0 = LambdaMiscCodec.createSupplier(method);
                  } catch (NoSuchMethodException var4) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableList.of", var4);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_LIST_OF_0.get();
            } else if (list.size() == 1) {
               if (GuavaSupport.FUNC_IMMUTABLE_LIST_OF_1 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_LIST.getMethod("of", Object.class);
                     GuavaSupport.FUNC_IMMUTABLE_LIST_OF_1 = LambdaMiscCodec.createFunction(method);
                  } catch (NoSuchMethodException var5) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableList.of", var5);
                  }
               }

               Object first = list.get(0);
               return GuavaSupport.FUNC_IMMUTABLE_LIST_OF_1.apply(first);
            } else {
               if (GuavaSupport.FUNC_IMMUTABLE_LIST_COPY_OF == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_LIST.getMethod("copyOf", Collection.class);
                     GuavaSupport.FUNC_IMMUTABLE_LIST_COPY_OF = LambdaMiscCodec.createFunction(method);
                  } catch (NoSuchMethodException var6) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableList.copyOf", var6);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_LIST_COPY_OF.apply(list);
            }
         }
      }
   }

   static class ImmutableSetConvertFunction implements Function {
      @Override
      public Object apply(Object object) {
         if (GuavaSupport.CLASS_IMMUTABLE_SET == null) {
            GuavaSupport.CLASS_IMMUTABLE_SET = TypeUtils.loadClass("com.google.common.collect.ImmutableSet");
         }

         if (GuavaSupport.CLASS_IMMUTABLE_SET == null) {
            throw new JSONException("class not found : com.google.common.collect.ImmutableSet");
         } else {
            List list = (List)object;
            if (list.isEmpty()) {
               if (GuavaSupport.FUNC_IMMUTABLE_SET_OF_0 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_SET.getMethod("of");
                     GuavaSupport.FUNC_IMMUTABLE_SET_OF_0 = LambdaMiscCodec.createSupplier(method);
                  } catch (NoSuchMethodException var4) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableSet.of", var4);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_SET_OF_0.get();
            } else if (list.size() == 1) {
               if (GuavaSupport.FUNC_IMMUTABLE_SET_OF_1 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_SET.getMethod("of", Object.class);
                     GuavaSupport.FUNC_IMMUTABLE_SET_OF_1 = LambdaMiscCodec.createFunction(method);
                  } catch (NoSuchMethodException var5) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableSet.of", var5);
                  }
               }

               Object first = list.get(0);
               return GuavaSupport.FUNC_IMMUTABLE_SET_OF_1.apply(first);
            } else {
               if (GuavaSupport.FUNC_IMMUTABLE_SET_COPY_OF == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_SET.getMethod("copyOf", Collection.class);
                     GuavaSupport.FUNC_IMMUTABLE_SET_COPY_OF = LambdaMiscCodec.createFunction(method);
                  } catch (NoSuchMethodException var6) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableSet.copyOf", var6);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_SET_COPY_OF.apply(list);
            }
         }
      }
   }

   static class ImmutableSingletonMapConvertFunction implements Function {
      @Override
      public Object apply(Object object) {
         if (GuavaSupport.CLASS_IMMUTABLE_MAP == null) {
            GuavaSupport.CLASS_IMMUTABLE_MAP = TypeUtils.loadClass("com.google.common.collect.ImmutableMap");
         }

         if (GuavaSupport.CLASS_IMMUTABLE_MAP == null) {
            throw new JSONException("class not found : com.google.common.collect.ImmutableMap");
         } else {
            Map map = (Map)object;
            if (map.size() == 0) {
               if (GuavaSupport.FUNC_IMMUTABLE_MAP_OF_0 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_MAP.getMethod("of");
                     GuavaSupport.FUNC_IMMUTABLE_MAP_OF_0 = LambdaMiscCodec.createSupplier(method);
                  } catch (NoSuchMethodException var4) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableMap.of", var4);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_MAP_OF_0.get();
            } else if (map.size() == 1) {
               if (GuavaSupport.FUNC_IMMUTABLE_MAP_OF_1 == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_MAP.getMethod("of", Object.class, Object.class);
                     method.setAccessible(true);
                     GuavaSupport.FUNC_IMMUTABLE_MAP_OF_1 = LambdaMiscCodec.createBiFunction(method);
                  } catch (NoSuchMethodException var5) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableBiMap.of", var5);
                  }
               }

               Entry entry = (Entry)map.entrySet().iterator().next();
               return GuavaSupport.FUNC_IMMUTABLE_MAP_OF_1.apply(entry.getKey(), entry.getValue());
            } else {
               if (GuavaSupport.FUNC_IMMUTABLE_MAP_COPY_OF == null) {
                  try {
                     Method method = GuavaSupport.CLASS_IMMUTABLE_MAP.getMethod("copyOf", Map.class);
                     GuavaSupport.FUNC_IMMUTABLE_MAP_COPY_OF = LambdaMiscCodec.createFunction(method);
                  } catch (NoSuchMethodException var6) {
                     throw new JSONException("method not found : com.google.common.collect.ImmutableBiMap.copyOf", var6);
                  }
               }

               return GuavaSupport.FUNC_IMMUTABLE_MAP_COPY_OF.apply(map);
            }
         }
      }
   }

   static class SingletonImmutableBiMapConvertFunction implements Function {
      @Override
      public Object apply(Object object) {
         if (GuavaSupport.FUNC_SINGLETON_IMMUTABLE_BIMAP == null) {
            try {
               Constructor constructor = TypeUtils.loadClass("com.google.common.collect.SingletonImmutableBiMap")
                  .getDeclaredConstructor(Object.class, Object.class);
               GuavaSupport.FUNC_SINGLETON_IMMUTABLE_BIMAP = LambdaMiscCodec.createBiFunction(constructor);
            } catch (SecurityException | NoSuchMethodException var4) {
               throw new JSONException("method not found : com.google.common.collect.SingletonImmutableBiMap(Object, Object)", var4);
            }
         }

         Map map = (Map)object;
         Entry entry = (Entry)map.entrySet().iterator().next();
         return GuavaSupport.FUNC_SINGLETON_IMMUTABLE_BIMAP.apply(entry.getKey(), entry.getValue());
      }
   }
}
