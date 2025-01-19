package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public class ObjectReaderNoneDefaultConstructor<T> extends ObjectReaderAdapter<T> {
   final String[] paramNames;
   final FieldReader[] setterFieldReaders;
   private final Function<Map<Long, Object>, T> creator;
   final Map<Long, FieldReader> paramFieldReaderMap;

   public ObjectReaderNoneDefaultConstructor(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      Function<Map<Long, Object>, T> creator,
      List<Constructor> alternateConstructors,
      String[] paramNames,
      FieldReader[] paramFieldReaders,
      FieldReader[] setterFieldReaders,
      Class[] seeAlso,
      String[] seeAlsoNames
   ) {
      super(objectClass, typeKey, typeName, features, null, null, null, seeAlso, seeAlsoNames, null, concat(paramFieldReaders, setterFieldReaders));
      this.paramNames = paramNames;
      this.creator = creator;
      this.setterFieldReaders = setterFieldReaders;
      this.paramFieldReaderMap = new HashMap<>();

      for (FieldReader paramFieldReader : paramFieldReaders) {
         this.paramFieldReaderMap.put(paramFieldReader.fieldNameHash, paramFieldReader);
      }
   }

   static FieldReader[] concat(FieldReader[] a, FieldReader[] b) {
      if (b == null) {
         return a;
      } else {
         int alen = a.length;
         a = Arrays.copyOf(a, alen + b.length);
         System.arraycopy(b, 0, a, alen, b.length);
         return a;
      }
   }

   @Override
   public T createInstanceNoneDefaultConstructor(Map<Long, Object> values) {
      return this.creator.apply(values);
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      byte type = jsonReader.getType();
      if (type == -81) {
         jsonReader.next();
         return null;
      } else {
         if (type == -110) {
            ObjectReader objectReader = jsonReader.checkAutoType(this.objectClass, this.typeNameHash, this.features | features);
            if (objectReader != null && objectReader != this) {
               return (T)objectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
            }
         }

         LinkedHashMap<Long, Object> valueMap = null;
         Map<Long, String> references = null;
         if (jsonReader.isArray()) {
            if (!jsonReader.isSupportBeanArray()) {
               throw new JSONException(jsonReader.info("expect object, but " + JSONB.typeName(jsonReader.getType())));
            }

            int entryCnt = jsonReader.startArray();

            for (int i = 0; i < entryCnt; i++) {
               FieldReader fieldReader = this.fieldReaders[i];
               Object fieldValue = fieldReader.readFieldValue(jsonReader);
               if (valueMap == null) {
                  valueMap = new LinkedHashMap<>();
               }

               valueMap.put(Long.valueOf(fieldReader.fieldNameHash), fieldValue);
            }
         } else {
            jsonReader.nextIfObjectStart();

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               long hashCode = jsonReader.readFieldNameHashCode();
               if (hashCode != 0L) {
                  if (hashCode == HASH_TYPE && i == 0) {
                     long typeHash = jsonReader.readTypeHashCode();
                     JSONReader.Context context = jsonReader.getContext();
                     ObjectReader autoTypeObjectReader = context.getObjectReaderAutoType(typeHash);
                     if (autoTypeObjectReader == null) {
                        String typeName = jsonReader.getString();
                        autoTypeObjectReader = context.getObjectReaderAutoType(typeName, this.objectClass);
                        if (autoTypeObjectReader == null) {
                           throw new JSONException(jsonReader.info("autoType not support : " + typeName));
                        }
                     }

                     Object object = autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
                     jsonReader.nextIfComma();
                     return (T)object;
                  }

                  FieldReader fieldReader = this.getFieldReader(hashCode);
                  if (fieldReader == null) {
                     this.processExtra(jsonReader, null);
                  } else if (jsonReader.isReference()) {
                     jsonReader.next();
                     String reference = jsonReader.readString();
                     if (references == null) {
                        references = new HashMap<>();
                     }

                     references.put(hashCode, reference);
                  } else {
                     Object fieldValue = fieldReader.readFieldValue(jsonReader);
                     if (valueMap == null) {
                        valueMap = new LinkedHashMap<>();
                     }

                     valueMap.put(Long.valueOf(fieldReader.fieldNameHash), fieldValue);
                  }
               }
            }
         }

         Map<Long, Object> args = (Map<Long, Object>)(valueMap == null ? Collections.emptyMap() : valueMap);
         T object = this.createInstanceNoneDefaultConstructor(args);
         if (this.setterFieldReaders != null) {
            for (int ix = 0; ix < this.setterFieldReaders.length; ix++) {
               FieldReader fieldReader = this.setterFieldReaders[ix];
               Object fieldValue = args.get(fieldReader.fieldNameHash);
               fieldReader.accept(object, fieldValue);
            }
         }

         if (references != null) {
            for (Entry<Long, String> entry : references.entrySet()) {
               Long hashCode = entry.getKey();
               String reference = entry.getValue();
               FieldReader fieldReader = this.getFieldReader(hashCode);
               if ("..".equals(reference)) {
                  fieldReader.accept(object, object);
               } else {
                  fieldReader.addResolveTask(jsonReader, object, reference);
               }
            }
         }

         return object;
      }
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.isSupportBeanArray(features | this.features) && jsonReader.nextIfArrayStart()) {
         LinkedHashMap<Long, Object> valueMap = null;

         for (int i = 0; i < this.fieldReaders.length; i++) {
            FieldReader fieldReader = this.fieldReaders[i];
            Object fieldValue = fieldReader.readFieldValue(jsonReader);
            if (valueMap == null) {
               valueMap = new LinkedHashMap<>();
            }

            long hash = fieldReader.fieldNameHash;
            valueMap.put(Long.valueOf(hash), fieldValue);
         }

         if (!jsonReader.nextIfArrayEnd()) {
            throw new JSONException(jsonReader.info("array not end, " + jsonReader.current()));
         } else {
            jsonReader.nextIfComma();
            return this.createInstanceNoneDefaultConstructor((Map<Long, Object>)(valueMap == null ? Collections.emptyMap() : valueMap));
         }
      } else {
         boolean objectStart = jsonReader.nextIfObjectStart();
         if (!objectStart) {
            if (jsonReader.isTypeRedirect()) {
               jsonReader.setTypeRedirect(false);
            } else if (jsonReader.nextIfNullOrEmptyString()) {
               return null;
            }
         }

         IdentityHashMap<FieldReader, String> refMap = null;
         JSONReader.Context context = jsonReader.getContext();
         long featuresAll = this.features | features | context.getFeatures();
         LinkedHashMap<Long, Object> valueMap = null;

         for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
            long hashCode = jsonReader.readFieldNameHashCode();
            if (hashCode != 0L) {
               if (hashCode == this.typeKeyHashCode && i == 0) {
                  long typeHash = jsonReader.readTypeHashCode();
                  if (typeHash != this.typeNameHash) {
                     boolean supportAutoType = (featuresAll & JSONReader.Feature.SupportAutoType.mask) != 0L;
                     ObjectReader autoTypeObjectReader;
                     if (supportAutoType) {
                        autoTypeObjectReader = jsonReader.getObjectReaderAutoType(typeHash, this.objectClass, this.features);
                     } else {
                        String typeName = jsonReader.getString();
                        autoTypeObjectReader = context.getObjectReaderAutoType(typeName, this.objectClass);
                     }

                     if (autoTypeObjectReader == null) {
                        String typeName = jsonReader.getString();
                        autoTypeObjectReader = context.getObjectReaderAutoType(typeName, this.objectClass, this.features);
                     }

                     if (autoTypeObjectReader != null) {
                        Object object = autoTypeObjectReader.readObject(jsonReader, fieldType, fieldName, 0L);
                        jsonReader.nextIfComma();
                        return (T)object;
                     }
                  }
               } else if (!jsonReader.nextIfNull()) {
                  FieldReader fieldReader = this.getFieldReader(hashCode);
                  FieldReader paramReader = this.paramFieldReaderMap.get(hashCode);
                  if (paramReader != null && fieldReader != null && paramReader.fieldClass != null && !paramReader.fieldClass.equals(fieldReader.fieldClass)) {
                     fieldReader = paramReader;
                  }

                  if (fieldReader == null && (featuresAll & JSONReader.Feature.SupportSmartMatch.mask) != 0L) {
                     long hashCodeLCase = jsonReader.getNameHashCodeLCase();
                     fieldReader = this.getFieldReaderLCase(hashCodeLCase);
                     if (fieldReader != null && valueMap != null && valueMap.containsKey(Long.valueOf(fieldReader.fieldNameHash))) {
                        fieldReader = null;
                     }
                  }

                  if (fieldReader == null) {
                     this.processExtra(jsonReader, null);
                  } else if (jsonReader.isReference()) {
                     String ref = jsonReader.readReference();
                     if (refMap == null) {
                        refMap = new IdentityHashMap<>();
                     }

                     refMap.put(fieldReader, ref);
                  } else {
                     Object fieldValue = fieldReader.readFieldValue(jsonReader);
                     if (valueMap == null) {
                        valueMap = new LinkedHashMap<>();
                     }

                     long hash;
                     if (fieldReader instanceof FieldReaderObjectParam) {
                        hash = ((FieldReaderObjectParam)fieldReader).paramNameHash;
                     } else {
                        hash = fieldReader.fieldNameHash;
                     }

                     valueMap.put(Long.valueOf(hash), fieldValue);
                  }
               }
            }
         }

         if (this.hasDefaultValue) {
            if (valueMap == null) {
               valueMap = new LinkedHashMap<>();
            }

            for (FieldReader fieldReaderx : this.fieldReaders) {
               if (fieldReaderx.defaultValue != null) {
                  Object fieldValuex = valueMap.get(fieldReaderx.fieldNameHash);
                  if (fieldValuex == null) {
                     valueMap.put(Long.valueOf(fieldReaderx.fieldNameHash), fieldReaderx.defaultValue);
                  }
               }
            }
         }

         Map<Long, Object> argsMap = (Map<Long, Object>)(valueMap == null ? Collections.emptyMap() : valueMap);
         T object = this.creator.apply(argsMap);
         if (this.setterFieldReaders != null && valueMap != null) {
            for (int ix = 0; ix < this.setterFieldReaders.length; ix++) {
               FieldReader fieldReaderxx = this.setterFieldReaders[ix];
               FieldReader paramReaderx = this.paramFieldReaderMap.get(fieldReaderxx.fieldNameHash);
               if (paramReaderx == null || paramReaderx.fieldClass.equals(fieldReaderxx.fieldClass)) {
                  Object fieldValuex = valueMap.get(fieldReaderxx.fieldNameHash);
                  if (fieldValuex != null
                     && (
                        paramReaderx == null
                           || paramReaderx.fieldName != null && fieldReaderxx.fieldName != null && paramReaderx.fieldName.equals(fieldReaderxx.fieldName)
                     )) {
                     fieldReaderxx.accept(object, fieldValuex);
                  }
               }
            }
         }

         if (refMap != null) {
            for (Entry<FieldReader, String> entry : refMap.entrySet()) {
               FieldReader fieldReaderxx = entry.getKey();
               String reference = entry.getValue();
               fieldReaderxx.addResolveTask(jsonReader, object, reference);
            }
         }

         jsonReader.nextIfComma();
         return object;
      }
   }

   public T readFromCSV(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      LinkedHashMap<Long, Object> valueMap = new LinkedHashMap<>();

      for (int i = 0; i < this.fieldReaders.length; i++) {
         FieldReader fieldReader = this.fieldReaders[i];
         Object fieldValue = fieldReader.readFieldValue(jsonReader);
         valueMap.put(Long.valueOf(fieldReader.fieldNameHash), fieldValue);
      }

      jsonReader.nextIfMatch('\n');
      return this.createInstanceNoneDefaultConstructor(valueMap);
   }

   @Override
   public T createInstance(Collection collection, long features) {
      int index = 0;
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      LinkedHashMap<Long, Object> valueMap = new LinkedHashMap<>();

      for (Object fieldValue : collection) {
         if (index >= this.fieldReaders.length) {
            break;
         }

         FieldReader fieldReader = this.fieldReaders[index];
         if (fieldValue != null) {
            Class<?> valueClass = fieldValue.getClass();
            Class fieldClass = fieldReader.fieldClass;
            if (valueClass != fieldClass) {
               Function typeConvert = provider.getTypeConvert(valueClass, fieldClass);
               if (typeConvert != null) {
                  fieldValue = typeConvert.apply(fieldValue);
               }
            }
         }

         long hash;
         if (fieldReader instanceof FieldReaderObjectParam) {
            hash = ((FieldReaderObjectParam)fieldReader).paramNameHash;
         } else {
            hash = fieldReader.fieldNameHash;
         }

         valueMap.put(Long.valueOf(hash), fieldValue);
         index++;
      }

      return this.createInstanceNoneDefaultConstructor(valueMap);
   }

   @Override
   public T createInstance(Map map, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      Object typeKey = map.get(this.getTypeKey());
      if (typeKey instanceof String) {
         String typeName = (String)typeKey;
         long typeHash = Fnv.hashCode64(typeName);
         ObjectReader<T> reader = null;
         if ((features & JSONReader.Feature.SupportAutoType.mask) != 0L) {
            reader = this.autoType(provider, typeHash);
         }

         if (reader == null) {
            reader = provider.getObjectReader(typeName, this.getObjectClass(), features | this.getFeatures());
         }

         if (reader != this && reader != null) {
            return reader.createInstance(map, features);
         }
      }

      LinkedHashMap<Long, Object> valueMap = null;

      for (Entry entry : map.entrySet()) {
         String fieldName = entry.getKey().toString();
         Object fieldValue = entry.getValue();
         FieldReader fieldReader = this.getFieldReader(fieldName);
         if (fieldReader != null) {
            if (fieldValue != null) {
               Class<?> valueClass = fieldValue.getClass();
               Class fieldClass = fieldReader.fieldClass;
               if (valueClass != fieldClass) {
                  Function typeConvert = provider.getTypeConvert(valueClass, fieldClass);
                  if (typeConvert != null) {
                     fieldValue = typeConvert.apply(fieldValue);
                  }
               }
            }

            if (valueMap == null) {
               valueMap = new LinkedHashMap<>();
            }

            long hash;
            if (fieldReader instanceof FieldReaderObjectParam) {
               hash = ((FieldReaderObjectParam)fieldReader).paramNameHash;
            } else {
               hash = fieldReader.fieldNameHash;
            }

            valueMap.put(Long.valueOf(hash), fieldValue);
         }
      }

      T object = this.createInstanceNoneDefaultConstructor((Map<Long, Object>)(valueMap == null ? Collections.emptyMap() : valueMap));
      if (this.setterFieldReaders != null) {
         for (int i = 0; i < this.setterFieldReaders.length; i++) {
            FieldReader fieldReader = this.setterFieldReaders[i];
            Object fieldValue = map.get(fieldReader.fieldName);
            if (fieldValue != null) {
               Class<?> valueClass = fieldValue.getClass();
               Class fieldClass = fieldReader.fieldClass;
               Type fieldType = fieldReader.fieldType;
               if (!(fieldType instanceof Class)) {
                  fieldValue = TypeUtils.cast(fieldValue, fieldType, provider);
               } else if (valueClass != fieldClass) {
                  Function typeConvert = provider.getTypeConvert(valueClass, fieldClass);
                  if (typeConvert != null) {
                     fieldValue = typeConvert.apply(fieldValue);
                  } else if (fieldValue instanceof Map) {
                     ObjectReader objectReader = fieldReader.getObjectReader(JSONFactory.createReadContext(provider));
                     fieldValue = objectReader.createInstance((Map)fieldValue, features | fieldReader.features);
                  }
               }

               fieldReader.accept(object, fieldValue);
            }
         }
      }

      return object;
   }
}
