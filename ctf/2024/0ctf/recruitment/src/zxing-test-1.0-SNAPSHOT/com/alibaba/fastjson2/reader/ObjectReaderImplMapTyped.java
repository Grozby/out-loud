package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.ReferenceKey;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;

class ObjectReaderImplMapTyped implements ObjectReader {
   final Class mapType;
   final Class instanceType;
   final Type keyType;
   final Type valueType;
   final Class valueClass;
   final long features;
   final Function builder;
   final boolean multiValue;
   final Constructor defaultConstructor;
   ObjectReader valueObjectReader;
   ObjectReader keyObjectReader;

   public ObjectReaderImplMapTyped(Class mapType, Class instanceType, Type keyType, Type valueType, long features, Function builder) {
      if (keyType == Object.class) {
         keyType = null;
      }

      this.mapType = mapType;
      this.instanceType = instanceType;
      this.keyType = keyType;
      this.valueType = valueType;
      this.valueClass = TypeUtils.getClass(valueType);
      this.features = features;
      this.builder = builder;
      this.multiValue = instanceType != null && "org.springframework.util.LinkedMultiValueMap".equals(instanceType.getName());
      Constructor defaultConstructor = null;
      Constructor[] constructors = this.instanceType.getDeclaredConstructors();

      for (Constructor constructor : constructors) {
         if (constructor.getParameterCount() == 0 && !Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
            defaultConstructor = constructor;
            break;
         }
      }

      this.defaultConstructor = defaultConstructor;
   }

   @Override
   public Class getObjectClass() {
      return this.mapType;
   }

   @Override
   public Object createInstance(Map input, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      Map object;
      if (this.instanceType != Map.class && this.instanceType != HashMap.class) {
         object = (Map)this.createInstance(features);
      } else {
         object = new HashMap();
      }

      for (Entry entry : input.entrySet()) {
         Object key = entry.getKey();
         Object fieldValue = entry.getValue();
         Object fieldName;
         if (this.keyType != null && this.keyType != String.class) {
            fieldName = TypeUtils.cast(key, this.keyType);
         } else {
            fieldName = key.toString();
         }

         Object value = fieldValue;
         if (fieldValue != null) {
            Class<?> valueClass = fieldValue.getClass();
            if (this.valueType != Object.class) {
               if (valueClass != JSONObject.class && valueClass != TypeUtils.CLASS_JSON_OBJECT_1x) {
                  if ((valueClass == JSONArray.class || valueClass == TypeUtils.CLASS_JSON_ARRAY_1x) && this.valueClass == List.class) {
                     if (this.valueObjectReader == null) {
                        this.valueObjectReader = provider.getObjectReader(this.valueType);
                     }

                     value = this.valueObjectReader.createInstance((List)fieldValue, features);
                  } else {
                     Function typeConvert;
                     if ((typeConvert = provider.getTypeConvert(valueClass, this.valueType)) != null) {
                        value = typeConvert.apply(fieldValue);
                     } else if (fieldValue instanceof Map) {
                        if (this.valueObjectReader == null) {
                           this.valueObjectReader = provider.getObjectReader(this.valueType);
                        }

                        value = this.valueObjectReader.createInstance((Map)fieldValue, features);
                     } else if (fieldValue instanceof Collection && !this.multiValue) {
                        if (this.valueObjectReader == null) {
                           this.valueObjectReader = provider.getObjectReader(this.valueType);
                        }

                        value = this.valueObjectReader.createInstance((Collection)fieldValue, features);
                     } else if (!valueClass.isInstance(fieldValue)) {
                        throw new JSONException("can not convert from " + valueClass + " to " + this.valueType);
                     }
                  }
               } else {
                  if (this.valueObjectReader == null) {
                     this.valueObjectReader = provider.getObjectReader(this.valueType);
                  }

                  value = this.valueObjectReader.createInstance((Map)fieldValue, features);
               }
            }
         }

         object.put(fieldName, value);
      }

      return this.builder != null ? this.builder.apply(object) : object;
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType != null && !this.instanceType.isInterface()) {
         try {
            return this.defaultConstructor != null ? this.defaultConstructor.newInstance() : this.instanceType.newInstance();
         } catch (Exception var4) {
            throw new JSONException("create map error", var4);
         }
      } else {
         return new HashMap();
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      ObjectReader objectReader = null;
      Function builder = this.builder;
      if (jsonReader.getType() == -110) {
         objectReader = jsonReader.checkAutoType(this.mapType, 0L, this.features | features);
         if (objectReader != null && objectReader != this) {
            builder = objectReader.getBuildFunction();
            if (!(objectReader instanceof ObjectReaderImplMap) && !(objectReader instanceof ObjectReaderImplMapTyped)) {
               return objectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
            }
         }
      }

      byte firstType = jsonReader.getType();
      if (firstType == -81) {
         jsonReader.next();
         return null;
      } else {
         if (firstType == -90) {
            jsonReader.next();
         }

         JSONReader.Context context = jsonReader.getContext();
         long contextFeatures = features | context.getFeatures();
         Map object;
         if (objectReader != null) {
            object = (Map)objectReader.createInstance(contextFeatures);
         } else {
            object = (Map)(this.instanceType == HashMap.class ? new HashMap() : (Map)this.createInstance(features));
         }

         int i = 0;

         while (true) {
            byte type = jsonReader.getType();
            if (type == -91) {
               jsonReader.next();
               if (builder != null) {
                  if (builder == ObjectReaderImplMap.ENUM_MAP_BUILDER && object.isEmpty()) {
                     return new EnumMap((Class)this.keyType);
                  }

                  return builder.apply(object);
               }

               return object;
            }

            Object name;
            if (this.keyType == String.class || jsonReader.isString()) {
               name = jsonReader.readFieldName();
            } else if (jsonReader.isReference()) {
               String reference = jsonReader.readReference();
               name = new ReferenceKey(i);
               jsonReader.addResolveTask(object, name, JSONPath.of(reference));
            } else {
               if (this.keyObjectReader == null && this.keyType != null) {
                  this.keyObjectReader = jsonReader.getObjectReader(this.keyType);
               }

               if (this.keyObjectReader == null) {
                  name = jsonReader.readAny();
               } else {
                  name = this.keyObjectReader.readJSONBObject(jsonReader, null, null, features);
               }
            }

            if (jsonReader.isReference()) {
               String reference = jsonReader.readReference();
               if ("..".equals(reference)) {
                  object.put(name, object);
               } else {
                  jsonReader.addResolveTask(object, name, JSONPath.of(reference));
                  if (!(object instanceof ConcurrentMap)) {
                     object.put(name, null);
                  }
               }
            } else if (jsonReader.nextIfNull()) {
               object.put(name, null);
            } else {
               Object value;
               if (this.valueType == Object.class) {
                  value = jsonReader.readAny();
               } else {
                  ObjectReader autoTypeValueReader = jsonReader.checkAutoType(this.valueClass, 0L, features);
                  if (autoTypeValueReader != null && autoTypeValueReader != this) {
                     value = autoTypeValueReader.readJSONBObject(jsonReader, this.valueType, name, features);
                  } else {
                     if (this.valueObjectReader == null) {
                        this.valueObjectReader = jsonReader.getObjectReader(this.valueType);
                     }

                     value = this.valueObjectReader.readJSONBObject(jsonReader, this.valueType, name, features);
                  }
               }

               if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                  object.put(name, value);
               }
            }

            i++;
         }
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int index = 0;
      if (!jsonReader.nextIfObjectStart()) {
         if (!jsonReader.isTypeRedirect()) {
            if (jsonReader.nextIfNullOrEmptyString()) {
               return null;
            }

            throw new JSONException(jsonReader.info("expect '{', but '" + jsonReader.current() + "'"));
         }

         index = 1;
         jsonReader.setTypeRedirect(false);
      }

      JSONReader.Context context = jsonReader.getContext();
      long contextFeatures = context.getFeatures() | features;
      Map object;
      if (this.instanceType == HashMap.class) {
         Supplier<Map> objectSupplier = context.getObjectSupplier();
         if (this.mapType == Map.class && objectSupplier != null) {
            if (this.keyType != String.class && objectSupplier.getClass().getName().equals("com.alibaba.fastjson.JSONObject$Creator")) {
               object = new HashMap();
            } else {
               object = objectSupplier.get();
               object = TypeUtils.getInnerMap(object);
            }
         } else {
            object = new HashMap();
         }
      } else if (this.instanceType == EnumMap.class && this.keyType instanceof Class) {
         object = new EnumMap((Class)this.keyType);
      } else {
         object = (Map)this.createInstance(contextFeatures);
      }

      for (; !jsonReader.nextIfObjectEnd() && !jsonReader.isEnd(); index++) {
         Object name;
         if (jsonReader.nextIfNull()) {
            if (!jsonReader.nextIfMatch(':')) {
               throw new JSONException(jsonReader.info("illegal json"));
            }

            name = null;
         } else if (this.keyType == String.class) {
            name = jsonReader.readFieldName();
            if (index == 0 && (contextFeatures & JSONReader.Feature.SupportAutoType.mask) != 0L && name.equals(this.getTypeKey())) {
               long typeHashCode = jsonReader.readTypeHashCode();
               ObjectReader objectReaderAutoType = jsonReader.getObjectReaderAutoType(typeHashCode, this.mapType, features);
               if (objectReaderAutoType != null
                  && objectReaderAutoType instanceof ObjectReaderImplMap
                  && !object.getClass().equals(((ObjectReaderImplMap)objectReaderAutoType).instanceType)) {
                  object = (Map)objectReaderAutoType.createInstance(features);
               }
               continue;
            }

            if (name == null) {
               name = jsonReader.readString();
               if (!jsonReader.nextIfMatch(':')) {
                  throw new JSONException(jsonReader.info("illegal json"));
               }
            }
         } else if (index == 0
            && (jsonReader.isEnabled(JSONReader.Feature.SupportAutoType) || jsonReader.getContext().getContextAutoTypeBeforeHandler() != null)
            && jsonReader.current() == '"'
            && (!(this.keyType instanceof Class) || !Enum.class.isAssignableFrom((Class<?>)this.keyType))) {
            name = jsonReader.readFieldName();
            if (name.equals(this.getTypeKey())) {
               long typeHashCode = jsonReader.readTypeHashCode();
               ObjectReader objectReaderAutoType = jsonReader.getObjectReaderAutoType(typeHashCode, this.mapType, features);
               if (objectReaderAutoType != null
                  && objectReaderAutoType instanceof ObjectReaderImplMap
                  && !object.getClass().equals(((ObjectReaderImplMap)objectReaderAutoType).instanceType)) {
                  object = (Map)objectReaderAutoType.createInstance(features);
               }
               continue;
            }

            name = TypeUtils.cast(name, this.keyType);
         } else {
            if (this.keyObjectReader != null) {
               name = this.keyObjectReader.readObject(jsonReader, null, null, 0L);
            } else {
               name = jsonReader.read(this.keyType);
            }

            if (name == null && Enum.class.isAssignableFrom((Class<?>)this.keyType)) {
               name = jsonReader.getString();
               jsonReader.nextIfMatch(':');
            }

            if (index == 0 && (contextFeatures & JSONReader.Feature.SupportAutoType.mask) != 0L && name.equals(this.getTypeKey())) {
               long typeHashCode = jsonReader.readTypeHashCode();
               ObjectReader objectReaderAutoType = jsonReader.getObjectReaderAutoType(typeHashCode, this.mapType, features);
               if (objectReaderAutoType != null
                  && objectReaderAutoType instanceof ObjectReaderImplMap
                  && !object.getClass().equals(((ObjectReaderImplMap)objectReaderAutoType).instanceType)) {
                  object = (Map)objectReaderAutoType.createInstance(features);
               }
               continue;
            }

            jsonReader.nextIfMatch(':');
         }

         if (this.valueObjectReader == null) {
            this.valueObjectReader = jsonReader.getObjectReader(this.valueType);
         }

         Object value;
         if (jsonReader.isReference()) {
            String reference = jsonReader.readReference();
            if (!"..".equals(reference)) {
               jsonReader.addResolveTask(object, name, JSONPath.of(reference));
               continue;
            }

            value = object;
         } else {
            if (this.multiValue && jsonReader.nextIfArrayStart()) {
               List list = new JSONArray();

               while (!jsonReader.nextIfArrayEnd()) {
                  value = this.valueObjectReader.readObject(jsonReader, this.valueType, fieldName, 0L);
                  list.add(value);
               }

               object.put(name, list);
               continue;
            }

            value = this.valueObjectReader.readObject(jsonReader, this.valueType, fieldName, 0L);
         }

         if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
            Object origin = object.put(name, value);
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

      jsonReader.nextIfComma();
      return this.builder != null ? this.builder.apply(object) : object;
   }
}
