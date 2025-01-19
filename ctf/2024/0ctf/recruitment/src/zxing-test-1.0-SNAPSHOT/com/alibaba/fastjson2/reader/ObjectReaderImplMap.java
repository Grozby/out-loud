package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.GuavaSupport;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.JSONObject1O;
import com.alibaba.fastjson2.util.ReferenceKey;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ObjectReaderImplMap implements ObjectReader {
   static final Function ENUM_MAP_BUILDER = e -> new EnumMap((Map)e);
   static Function UNSAFE_OBJECT_CREATOR;
   static final Class CLASS_SINGLETON_MAP = Collections.singletonMap(1, 1).getClass();
   static final Class CLASS_EMPTY_MAP = Collections.EMPTY_MAP.getClass();
   static final Class CLASS_EMPTY_SORTED_MAP = Collections.emptySortedMap().getClass();
   static final Class CLASS_EMPTY_NAVIGABLE_MAP = Collections.emptyNavigableMap().getClass();
   static final Class CLASS_UNMODIFIABLE_MAP = Collections.unmodifiableMap(Collections.emptyMap()).getClass();
   static final Class CLASS_UNMODIFIABLE_SORTED_MAP = Collections.unmodifiableSortedMap(Collections.emptySortedMap()).getClass();
   static final Class CLASS_UNMODIFIABLE_NAVIGABLE_MAP = Collections.unmodifiableNavigableMap(Collections.emptyNavigableMap()).getClass();
   public static final ObjectReaderImplMap INSTANCE = new ObjectReaderImplMap(null, HashMap.class, HashMap.class, 0L, null);
   public static final ObjectReaderImplMap INSTANCE_OBJECT = new ObjectReaderImplMap(null, JSONObject.class, JSONObject.class, 0L, null);
   final Type fieldType;
   final Class mapType;
   final long mapTypeHash;
   final Class instanceType;
   final long features;
   final Function builder;
   Object mapSingleton;
   volatile boolean instanceError;

   public static ObjectReader of(Type fieldType, Class mapType, long features) {
      Function builder = null;
      Class instanceType = mapType;
      if ("".equals(mapType.getSimpleName())) {
         instanceType = mapType.getSuperclass();
         if (fieldType == null) {
            fieldType = mapType.getGenericSuperclass();
         }
      }

      if (mapType == Map.class || mapType == AbstractMap.class || mapType == CLASS_SINGLETON_MAP) {
         instanceType = HashMap.class;
      } else if (mapType == CLASS_UNMODIFIABLE_MAP) {
         instanceType = LinkedHashMap.class;
      } else if (mapType == SortedMap.class || mapType == CLASS_UNMODIFIABLE_SORTED_MAP || mapType == CLASS_UNMODIFIABLE_NAVIGABLE_MAP) {
         instanceType = TreeMap.class;
      } else if (mapType == ConcurrentMap.class) {
         instanceType = ConcurrentHashMap.class;
      } else if (mapType == ConcurrentNavigableMap.class) {
         instanceType = ConcurrentSkipListMap.class;
      } else {
         String instanceTypeName = mapType.getTypeName();
         switch (instanceTypeName) {
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

      if (fieldType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)fieldType;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 2) {
            Type keyType = actualTypeArguments[0];
            Type valueType = actualTypeArguments[1];
            boolean multiValueMap = "org.springframework.util.LinkedMultiValueMap".equals(instanceType.getName());
            if (keyType == String.class && valueType == String.class && builder == null) {
               return new ObjectReaderImplMapString(mapType, instanceType, features);
            }

            return new ObjectReaderImplMapTyped(mapType, instanceType, keyType, valueType, 0L, builder);
         }
      }

      if (fieldType == null && features == 0L) {
         if (mapType == HashMap.class && instanceType == HashMap.class) {
            return INSTANCE;
         }

         if (mapType == JSONObject.class && instanceType == JSONObject.class) {
            return INSTANCE_OBJECT;
         }
      }

      String instanceTypeName = instanceType.getName();
      switch (instanceTypeName) {
         case "com.alibaba.fastjson.JSONObject":
            builder = createObjectSupplier(instanceType);
            break;
         case "com.google.common.collect.RegularImmutableMap":
            builder = GuavaSupport.immutableMapConverter();
            instanceType = HashMap.class;
            break;
         case "com.google.common.collect.SingletonImmutableBiMap":
            builder = GuavaSupport.singletonBiMapConverter();
            instanceType = HashMap.class;
            break;
         case "com.google.common.collect.ArrayListMultimap":
            builder = GuavaSupport.createConvertFunction(instanceType);
            instanceType = HashMap.class;
            break;
         case "kotlin.collections.EmptyMap":
            Object mapSingleton;
            try {
               Field field = instanceType.getField("INSTANCE");
               if (!field.isAccessible()) {
                  field.setAccessible(true);
               }

               mapSingleton = field.get(null);
            } catch (IllegalAccessException | NoSuchFieldException var15) {
               throw new IllegalStateException("Failed to get singleton of " + instanceType, var15);
            }

            return new ObjectReaderImplMap(instanceType, features, mapSingleton);
         case "java.util.Collections$EmptyMap":
            return new ObjectReaderImplMap(instanceType, features, Collections.EMPTY_MAP);
         default:
            Type genericSuperclass = instanceType.getGenericSuperclass();
            if (mapType != JSONObject.class && genericSuperclass instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)genericSuperclass;
               Type[] arguments = parameterizedType.getActualTypeArguments();
               if (arguments.length == 2) {
                  Type arg0 = arguments[0];
                  Type arg1 = arguments[1];
                  boolean typed = !(arg0 instanceof TypeVariable) && !(arg1 instanceof TypeVariable);
                  if (typed) {
                     return new ObjectReaderImplMapTyped(mapType, instanceType, arg0, arg1, 0L, builder);
                  }
               }
            }

            if (instanceType == JSONObject1O.class) {
               builder = createObjectSupplier(TypeUtils.CLASS_JSON_OBJECT_1x);
               instanceType = LinkedHashMap.class;
            } else if (mapType == CLASS_UNMODIFIABLE_MAP) {
               builder = Collections::unmodifiableMap;
            } else if (mapType == CLASS_UNMODIFIABLE_SORTED_MAP) {
               builder = Collections::unmodifiableSortedMap;
            } else if (mapType == CLASS_UNMODIFIABLE_NAVIGABLE_MAP) {
               builder = Collections::unmodifiableNavigableMap;
            } else if (mapType == CLASS_SINGLETON_MAP) {
               builder = map -> {
                  Entry entry = (Entry)map.entrySet().iterator().next();
                  return Collections.singletonMap(entry.getKey(), entry.getValue());
               };
            } else if (mapType == EnumMap.class) {
               instanceType = LinkedHashMap.class;
               builder = ENUM_MAP_BUILDER;
            }
      }

      return new ObjectReaderImplMap(fieldType, mapType, instanceType, features, builder);
   }

   ObjectReaderImplMap(Class mapClass, long features, Object mapSingleton) {
      this(mapClass, mapClass, mapClass, features, null);
      this.mapSingleton = mapSingleton;
   }

   ObjectReaderImplMap(Type fieldType, Class mapType, Class instanceType, long features, Function builder) {
      this.fieldType = fieldType;
      this.mapType = mapType;
      this.mapTypeHash = Fnv.hashCode64(TypeUtils.getTypeName(mapType));
      this.instanceType = instanceType;
      this.features = features;
      this.builder = builder;
   }

   @Override
   public Class getObjectClass() {
      return this.mapType;
   }

   @Override
   public Function getBuildFunction() {
      return this.builder;
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType == HashMap.class) {
         return new HashMap();
      } else if (this.instanceType == LinkedHashMap.class) {
         return new LinkedHashMap();
      } else if (this.instanceType == JSONObject.class) {
         return new JSONObject();
      } else if (this.mapSingleton != null) {
         return this.mapSingleton;
      } else if (this.instanceType == CLASS_EMPTY_SORTED_MAP) {
         return Collections.emptySortedMap();
      } else if (this.instanceType == CLASS_EMPTY_NAVIGABLE_MAP) {
         return Collections.emptyNavigableMap();
      } else {
         String instanceTypeName = this.instanceType.getName();
         switch (instanceTypeName) {
            case "com.ali.com.google.common.collect.EmptyImmutableBiMap":
               try {
                  return JDKUtils.UNSAFE.allocateInstance(this.instanceType);
               } catch (InstantiationException var7) {
                  throw new JSONException("create map error : " + this.instanceType);
               }
            case "java.util.ImmutableCollections$Map1":
               return new HashMap();
            case "java.util.ImmutableCollections$MapN":
               return new LinkedHashMap();
            default:
               try {
                  return this.instanceType.newInstance();
               } catch (IllegalAccessException | InstantiationException var8) {
                  throw new JSONException("create map error : " + this.instanceType);
               }
         }
      }
   }

   @Override
   public Object createInstance(Map map, long features) {
      if (this.mapType.isInstance(map)) {
         return map;
      } else if (this.mapType == JSONObject.class) {
         return new JSONObject(map);
      } else {
         Map instance = (Map)this.createInstance(features);
         instance.putAll(map);
         return this.builder != null ? this.builder.apply(instance) : instance;
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName1, long features) {
      ObjectReader objectReader = jsonReader.checkAutoType(this.mapType, this.mapTypeHash, this.features | features);
      if (objectReader != null && objectReader != this) {
         return objectReader.readJSONBObject(jsonReader, fieldType, fieldName1, features);
      } else if (jsonReader.nextIfNull()) {
         return null;
      } else {
         boolean emptyObject = false;
         jsonReader.nextIfMatch((byte)-90);
         Supplier<Map> objectSupplier = jsonReader.getContext().getObjectSupplier();
         long contextFeatures = features | jsonReader.features(features);
         Map map = null;
         if (this.mapType == null && objectSupplier != null) {
            map = objectSupplier.get();
         } else if (this.instanceType == HashMap.class) {
            map = new HashMap();
         } else if (this.instanceType == LinkedHashMap.class) {
            map = new LinkedHashMap();
         } else if (this.instanceType == JSONObject.class) {
            map = new JSONObject();
         } else if (this.instanceType == CLASS_EMPTY_MAP) {
            map = Collections.EMPTY_MAP;
         } else {
            JSONException error = null;
            if (!this.instanceError) {
               try {
                  map = (Map)this.instanceType.newInstance();
               } catch (IllegalAccessException | InstantiationException var17) {
                  this.instanceError = true;
                  error = new JSONException(jsonReader.info("create map error " + this.instanceType));
               }
            }

            if (this.instanceError && Map.class.isAssignableFrom(this.instanceType.getSuperclass())) {
               try {
                  map = (Map)this.instanceType.getSuperclass().newInstance();
                  error = null;
               } catch (IllegalAccessException | InstantiationException var18) {
                  if (error == null) {
                     error = new JSONException(jsonReader.info("create map error " + this.instanceType));
                  }
               }
            }

            if (error != null) {
               throw error;
            }
         }

         if (!emptyObject) {
            int i = 0;

            while (true) {
               byte type = jsonReader.getType();
               if (type == -91) {
                  jsonReader.next();
                  break;
               }

               Object fieldName;
               if (type >= 73) {
                  fieldName = jsonReader.readFieldName();
               } else if (jsonReader.nextIfMatch((byte)-109)) {
                  String reference = jsonReader.readString();
                  fieldName = new ReferenceKey(i);
                  jsonReader.addResolveTask(map, fieldName, JSONPath.of(reference));
               } else {
                  fieldName = jsonReader.readAny();
               }

               if (jsonReader.isReference()) {
                  String reference = jsonReader.readReference();
                  if ("..".equals(reference)) {
                     map.put(fieldName, map);
                  } else {
                     jsonReader.addResolveTask(map, fieldName, JSONPath.of(reference));
                     map.put(fieldName, null);
                  }
               } else {
                  type = jsonReader.getType();
                  Object value;
                  if (type >= 73 && type <= 125) {
                     value = jsonReader.readString();
                  } else if (type == -110) {
                     ObjectReader autoTypeObjectReader = jsonReader.checkAutoType(Object.class, 0L, this.features | features);
                     if (autoTypeObjectReader != null) {
                        value = autoTypeObjectReader.readJSONBObject(jsonReader, null, fieldName, features);
                     } else {
                        value = jsonReader.readAny();
                     }
                  } else if (type == -79) {
                     value = Boolean.TRUE;
                     jsonReader.next();
                  } else if (type == -80) {
                     value = Boolean.FALSE;
                     jsonReader.next();
                  } else if (type == -109) {
                     String reference = jsonReader.readReference();
                     if ("..".equals(reference)) {
                        value = map;
                     } else {
                        value = null;
                        jsonReader.addResolveTask(map, fieldName, JSONPath.of(reference));
                     }
                  } else if (type == -90) {
                     value = jsonReader.readObject();
                  } else if (type >= -108 && type <= -92) {
                     value = jsonReader.readArray();
                  } else {
                     value = jsonReader.readAny();
                  }

                  if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                     map.put(fieldName, value);
                  }
               }

               i++;
            }
         }

         return this.builder != null ? this.builder.apply(map) : map;
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.nextIfNull()) {
         return null;
      } else {
         JSONReader.Context context = jsonReader.getContext();
         Supplier<Map> objectSupplier = jsonReader.getContext().getObjectSupplier();
         Map object;
         if (objectSupplier == null
            || this.mapType != null && this.mapType != JSONObject.class && !"com.alibaba.fastjson.JSONObject".equals(this.mapType.getName())) {
            object = (Map)this.createInstance(context.getFeatures() | features);
         } else {
            object = objectSupplier.get();
         }

         if (jsonReader.isString() && !jsonReader.isTypeRedirect()) {
            String str = jsonReader.readString();
            if (!str.isEmpty()) {
               JSONReader strReader = JSONReader.of(str, jsonReader.getContext());

               try {
                  strReader.read(object, features);
               } catch (Throwable var14) {
                  if (strReader != null) {
                     try {
                        strReader.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (strReader != null) {
                  strReader.close();
               }
            }
         } else {
            jsonReader.read(object, features);
         }

         jsonReader.nextIfComma();
         return this.builder != null ? this.builder.apply(object) : object;
      }
   }

   static Function createObjectSupplier(Class objectClass) {
      return UNSAFE_OBJECT_CREATOR != null ? UNSAFE_OBJECT_CREATOR : (UNSAFE_OBJECT_CREATOR = new ObjectReaderImplMap.ObjectCreatorUF(objectClass));
   }

   static class ObjectCreatorUF implements Function {
      final Class objectClass;
      final Field map;
      final long mapOffset;

      ObjectCreatorUF(Class objectClass) {
         this.objectClass = objectClass;

         try {
            this.map = objectClass.getDeclaredField("map");
         } catch (NoSuchFieldException var3) {
            throw new JSONException("field map not found", var3);
         }

         this.mapOffset = JDKUtils.UNSAFE.objectFieldOffset(this.map);
      }

      @Override
      public Object apply(Object map) {
         if (map == null) {
            map = new HashMap();
         }

         try {
            Object object = JDKUtils.UNSAFE.allocateInstance(this.objectClass);
            JDKUtils.UNSAFE.putObject(object, this.mapOffset, map);
            return object;
         } catch (InstantiationException var4) {
            throw new JSONException("create " + this.objectClass.getName() + " error", var4);
         }
      }
   }
}
