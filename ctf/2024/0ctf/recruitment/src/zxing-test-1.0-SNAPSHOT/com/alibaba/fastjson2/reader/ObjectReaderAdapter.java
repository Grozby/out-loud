package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReaderAdapter<T> extends ObjectReaderBean<T> {
   protected final String typeKey;
   protected final long typeKeyHashCode;
   protected final FieldReader[] fieldReaders;
   final long[] hashCodes;
   final short[] mapping;
   final long[] hashCodesLCase;
   final short[] mappingLCase;
   final Constructor constructor;
   volatile boolean instantiationError;
   final Class[] seeAlso;
   final String[] seeAlsoNames;
   final Class seeAlsoDefault;
   final Map<Long, Class> seeAlsoMapping;

   public ObjectReaderAdapter(Class objectClass, Supplier<T> creator, FieldReader... fieldReaders) {
      this(objectClass, null, null, 0L, null, creator, null, fieldReaders);
   }

   public ObjectReaderAdapter(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      JSONSchema schema,
      Supplier<T> creator,
      Function buildFunction,
      FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, schema, creator, buildFunction, null, null, null, fieldReaders);
   }

   public ObjectReaderAdapter(
      Class objectClass, String typeKey, String typeName, long features, Supplier<T> creator, Function buildFunction, FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   public ObjectReaderAdapter(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      JSONSchema schema,
      Supplier<T> creator,
      Function buildFunction,
      Class[] seeAlso,
      String[] seeAlsoNames,
      FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, schema, creator, buildFunction, seeAlso, seeAlsoNames, null, fieldReaders);
   }

   public ObjectReaderAdapter(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      JSONSchema schema,
      Supplier<T> creator,
      Function buildFunction,
      Class[] seeAlso,
      String[] seeAlsoNames,
      Class seeAlsoDefault,
      FieldReader... fieldReaders
   ) {
      super(objectClass, creator, typeName, features, schema, buildFunction);
      this.constructor = objectClass == null ? null : BeanUtils.getDefaultConstructor(objectClass, true);
      if (this.constructor != null) {
         this.constructor.setAccessible(true);
      }

      if (typeKey != null && !typeKey.isEmpty()) {
         this.typeKey = typeKey;
         this.typeKeyHashCode = Fnv.hashCode64(typeKey);
      } else {
         this.typeKey = "@type";
         this.typeKeyHashCode = HASH_TYPE;
      }

      this.fieldReaders = fieldReaders;
      long[] hashCodes = new long[fieldReaders.length];
      long[] hashCodesLCase = new long[fieldReaders.length];

      for (int i = 0; i < fieldReaders.length; i++) {
         FieldReader fieldReader = fieldReaders[i];
         hashCodes[i] = fieldReader.fieldNameHash;
         hashCodesLCase[i] = fieldReader.fieldNameHashLCase;
         if (fieldReader.isUnwrapped() && (this.extraFieldReader == null || !(this.extraFieldReader instanceof FieldReaderAnySetter))) {
            this.extraFieldReader = fieldReader;
         }

         if (fieldReader.defaultValue != null) {
            this.hasDefaultValue = true;
         }
      }

      this.hashCodes = Arrays.copyOf(hashCodes, hashCodes.length);
      Arrays.sort(this.hashCodes);
      this.mapping = new short[this.hashCodes.length];

      for (int i = 0; i < hashCodes.length; i++) {
         long hashCode = hashCodes[i];
         int index = Arrays.binarySearch(this.hashCodes, hashCode);
         this.mapping[index] = (short)i;
      }

      this.hashCodesLCase = Arrays.copyOf(hashCodesLCase, hashCodesLCase.length);
      Arrays.sort(this.hashCodesLCase);
      this.mappingLCase = new short[this.hashCodesLCase.length];

      for (int i = 0; i < hashCodesLCase.length; i++) {
         long hashCode = hashCodesLCase[i];
         int index = Arrays.binarySearch(this.hashCodesLCase, hashCode);
         this.mappingLCase[index] = (short)i;
      }

      this.seeAlso = seeAlso;
      if (seeAlso != null) {
         this.seeAlsoMapping = new HashMap<>(seeAlso.length, 1.0F);
         this.seeAlsoNames = new String[seeAlso.length];

         for (int i = 0; i < seeAlso.length; i++) {
            Class seeAlsoClass = seeAlso[i];
            String seeAlsoTypeName = null;
            if (seeAlsoNames != null && seeAlsoNames.length >= i + 1) {
               seeAlsoTypeName = seeAlsoNames[i];
            }

            if (seeAlsoTypeName == null || seeAlsoTypeName.isEmpty()) {
               seeAlsoTypeName = seeAlsoClass.getSimpleName();
            }

            long hashCode = Fnv.hashCode64(seeAlsoTypeName);
            this.seeAlsoMapping.put(hashCode, seeAlsoClass);
            this.seeAlsoNames[i] = seeAlsoTypeName;
         }
      } else {
         this.seeAlsoMapping = null;
         this.seeAlsoNames = null;
      }

      this.seeAlsoDefault = seeAlsoDefault;
   }

   @Override
   public final String getTypeKey() {
      return this.typeKey;
   }

   @Override
   public final long getTypeKeyHash() {
      return this.typeKeyHashCode;
   }

   @Override
   public final long getFeatures() {
      return this.features;
   }

   public FieldReader[] getFieldReaders() {
      return Arrays.copyOf(this.fieldReaders, this.fieldReaders.length);
   }

   public void apply(Consumer<FieldReader> fieldReaderConsumer) {
      for (FieldReader fieldReader : this.fieldReaders) {
         fieldReaderConsumer.accept(fieldReader);
      }
   }

   public Object autoType(JSONReader jsonReader, Class expectClass, long features) {
      long typeHash = jsonReader.readTypeHashCode();
      JSONReader.Context context = jsonReader.getContext();
      ObjectReader autoTypeObjectReader = null;
      if (jsonReader.isSupportAutoTypeOrHandler(features)) {
         autoTypeObjectReader = context.getObjectReaderAutoType(typeHash);
      }

      if (autoTypeObjectReader == null) {
         String typeName = jsonReader.getString();
         autoTypeObjectReader = context.getObjectReaderAutoType(typeName, expectClass, this.features | features | context.getFeatures());
         if (autoTypeObjectReader == null) {
            if (expectClass != this.objectClass) {
               throw new JSONException(jsonReader.info("autoType not support : " + typeName));
            }

            autoTypeObjectReader = this;
         }
      }

      return autoTypeObjectReader.readObject(jsonReader, null, null, features);
   }

   @Override
   public final Function getBuildFunction() {
      return this.buildFunction;
   }

   @Override
   public T readArrayMappingObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      jsonReader.nextIfArrayStart();
      Object object = this.creator.get();

      for (int i = 0; i < this.fieldReaders.length; i++) {
         this.fieldReaders[i].readFieldValue(jsonReader, object);
      }

      if (!jsonReader.nextIfArrayEnd()) {
         throw new JSONException(jsonReader.info("array to bean end error"));
      } else {
         jsonReader.nextIfComma();
         return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
      }
   }

   @Override
   public T readArrayMappingJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      ObjectReader autoTypeReader = this.checkAutoType(jsonReader, features);
      if (autoTypeReader != null) {
         return (T)autoTypeReader.readArrayMappingJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         T object = this.createInstance(0L);
         int entryCnt = jsonReader.startArray();
         if (entryCnt == this.fieldReaders.length) {
            for (int i = 0; i < this.fieldReaders.length; i++) {
               FieldReader fieldReader = this.fieldReaders[i];
               fieldReader.readFieldValue(jsonReader, object);
            }
         } else {
            this.readArrayMappingJSONBObject0(jsonReader, object, entryCnt);
         }

         return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
      }
   }

   protected void readArrayMappingJSONBObject0(JSONReader jsonReader, Object object, int entryCnt) {
      for (int i = 0; i < this.fieldReaders.length; i++) {
         if (i < entryCnt) {
            FieldReader fieldReader = this.fieldReaders[i];
            fieldReader.readFieldValue(jsonReader, object);
         }
      }

      for (int ix = this.fieldReaders.length; ix < entryCnt; ix++) {
         jsonReader.skipValue();
      }
   }

   protected Object createInstance0(long features) {
      if ((features & JSONReader.Feature.UseDefaultConstructorAsPossible.mask) != 0L && this.constructor != null && this.constructor.getParameterCount() == 0) {
         T object;
         try {
            object = (T)this.constructor.newInstance();
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException var5) {
            throw new JSONException("create instance error, " + this.objectClass, var5);
         }

         if (this.hasDefaultValue) {
            this.initDefaultValue(object);
         }

         return object;
      } else if (this.creator == null) {
         throw new JSONException("create instance error, " + this.objectClass);
      } else {
         return this.creator.get();
      }
   }

   @Override
   protected void initDefaultValue(T object) {
      for (int i = 0; i < this.fieldReaders.length; i++) {
         FieldReader fieldReader = this.fieldReaders[i];
         Object defaultValue = fieldReader.defaultValue;
         if (defaultValue != null) {
            fieldReader.accept(object, defaultValue);
         }
      }
   }

   @Override
   public T createInstance(Collection collection, long features) {
      T object = this.createInstance(0L);
      int index = 0;

      for (Object fieldValue : collection) {
         if (index >= this.fieldReaders.length) {
            break;
         }

         FieldReader fieldReader = this.fieldReaders[index];
         fieldReader.accept(object, fieldValue);
         index++;
      }

      return object;
   }

   @Override
   public T createInstance(long features) {
      if (this.instantiationError && this.constructor != null) {
         T object;
         try {
            object = (T)this.constructor.newInstance();
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException var6) {
            throw new JSONException("create instance error, " + this.objectClass, var6);
         }

         if (this.hasDefaultValue) {
            this.initDefaultValue(object);
         }

         return object;
      } else {
         try {
            T objectx = (T)this.createInstance0(features);
            if (this.hasDefaultValue) {
               this.initDefaultValue(objectx);
            }

            return objectx;
         } catch (Exception var7) {
            this.instantiationError = true;
            if (this.constructor != null) {
               try {
                  T objectx = (T)this.constructor.newInstance();
                  if (this.hasDefaultValue) {
                     this.initDefaultValue(objectx);
                  }

                  return objectx;
               } catch (IllegalAccessException | InvocationTargetException | InstantiationException var5) {
                  throw new JSONException("create instance error, " + this.objectClass, var5);
               }
            } else {
               throw new JSONException("create instance error, " + this.objectClass, var7);
            }
         }
      }
   }

   @Override
   public FieldReader getFieldReader(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodes, hashCode);
      if (m < 0) {
         return null;
      } else {
         int index = this.mapping[m];
         return this.fieldReaders[index];
      }
   }

   public int getFieldOrdinal(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodes, hashCode);
      return m < 0 ? -1 : this.mapping[m];
   }

   protected final FieldReader getFieldReaderUL(long hashCode, JSONReader jsonReader, long features) {
      FieldReader fieldReader = this.getFieldReader(hashCode);
      if (fieldReader == null && jsonReader.isSupportSmartMatch(this.features | features)) {
         long hashCodeL = jsonReader.getNameHashCodeLCase();
         fieldReader = this.getFieldReaderLCase(hashCodeL == hashCode ? hashCode : hashCodeL);
      }

      return fieldReader;
   }

   protected final void readFieldValue(long hashCode, JSONReader jsonReader, long features, Object object) {
      FieldReader fieldReader = this.getFieldReader(hashCode);
      if (fieldReader == null && jsonReader.isSupportSmartMatch(this.features | features)) {
         long hashCodeL = jsonReader.getNameHashCodeLCase();
         fieldReader = this.getFieldReaderLCase(hashCodeL == hashCode ? hashCode : hashCodeL);
      }

      if (fieldReader != null) {
         if (jsonReader.jsonb) {
            fieldReader.readFieldValueJSONB(jsonReader, object);
         } else {
            fieldReader.readFieldValue(jsonReader, object);
         }
      } else {
         this.processExtra(jsonReader, object);
      }
   }

   @Override
   public FieldReader getFieldReaderLCase(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodesLCase, hashCode);
      if (m < 0) {
         return null;
      } else {
         int index = this.mappingLCase[m];
         return this.fieldReaders[index];
      }
   }

   protected T autoType(JSONReader jsonReader) {
      long typeHash = jsonReader.readTypeHashCode();
      JSONReader.Context context = jsonReader.getContext();
      ObjectReader autoTypeObjectReader = this.autoType(context, typeHash);
      if (autoTypeObjectReader == null) {
         String typeName = jsonReader.getString();
         autoTypeObjectReader = context.getObjectReaderAutoType(typeName, null);
         if (autoTypeObjectReader == null) {
            throw new JSONException(jsonReader.info("autoType not support : " + typeName));
         }
      }

      return (T)autoTypeObjectReader.readJSONBObject(jsonReader, null, null, this.features);
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         ObjectReader autoTypeReader = jsonReader.checkAutoType(this.objectClass, this.typeNameHash, this.features | features);
         if (autoTypeReader != null && autoTypeReader.getObjectClass() != this.objectClass) {
            return (T)autoTypeReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
         } else {
            if (!this.serializable) {
               jsonReader.errorOnNoneSerializable(this.objectClass);
            }

            if (jsonReader.isArray()) {
               if (jsonReader.isSupportBeanArray()) {
                  return this.readArrayMappingJSONBObject(jsonReader, fieldType, fieldName, features);
               } else {
                  throw new JSONException(jsonReader.info("expect object, but " + JSONB.typeName(jsonReader.getType())));
               }
            } else {
               boolean objectStart = jsonReader.nextIfObjectStart();
               T object = null;

               for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
                  long hash = jsonReader.readFieldNameHashCode();
                  if (hash == this.typeKeyHashCode && i == 0) {
                     long typeHash = jsonReader.readValueHashCode();
                     JSONReader.Context context = jsonReader.getContext();
                     ObjectReader autoTypeObjectReader = this.autoType(context, typeHash);
                     if (autoTypeObjectReader == null) {
                        String typeName = jsonReader.getString();
                        autoTypeObjectReader = context.getObjectReaderAutoType(typeName, null);
                        if (autoTypeObjectReader == null) {
                           throw new JSONException(jsonReader.info("autoType not support : " + typeName));
                        }
                     }

                     if (autoTypeObjectReader != this) {
                        jsonReader.setTypeRedirect(true);
                        return (T)autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
                     }
                  } else if (hash != 0L) {
                     FieldReader fieldReader = this.getFieldReader(hash);
                     if (fieldReader == null && jsonReader.isSupportSmartMatch(features | this.features)) {
                        long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                        fieldReader = this.getFieldReaderLCase(nameHashCodeLCase);
                     }

                     if (fieldReader == null) {
                        this.processExtra(jsonReader, object);
                     } else {
                        if (object == null) {
                           object = this.createInstance(jsonReader.getContext().getFeatures() | features);
                        }

                        fieldReader.readFieldValue(jsonReader, object);
                     }
                  }
               }

               if (object == null) {
                  object = this.createInstance(jsonReader.getContext().getFeatures() | features);
               }

               if (this.schema != null) {
                  this.schema.assertValidate(object);
               }

               return object;
            }
         }
      }
   }

   @Override
   public ObjectReader autoType(ObjectReaderProvider provider, long typeHash) {
      if (this.seeAlsoMapping != null && this.seeAlsoMapping.size() > 0) {
         Class seeAlsoClass = this.seeAlsoMapping.get(typeHash);
         return seeAlsoClass == null ? null : provider.getObjectReader(seeAlsoClass);
      } else {
         return provider.getObjectReader(typeHash);
      }
   }

   @Override
   public ObjectReader autoType(JSONReader.Context context, long typeHash) {
      if (this.seeAlsoMapping != null && this.seeAlsoMapping.size() > 0) {
         Class seeAlsoClass = this.seeAlsoMapping.get(typeHash);
         return seeAlsoClass == null ? null : context.getObjectReader(seeAlsoClass);
      } else {
         return context.getObjectReaderAutoType(typeHash);
      }
   }

   @Override
   protected void initStringFieldAsEmpty(Object object) {
      for (int i = 0; i < this.fieldReaders.length; i++) {
         FieldReader fieldReader = this.fieldReaders[i];
         if (fieldReader.fieldClass == String.class) {
            fieldReader.accept(object, "");
         }
      }
   }

   @Override
   public T createInstance(Map map, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      Object typeKey = map.get(this.typeKey);
      long features2 = features | this.features;
      if (typeKey instanceof String) {
         String typeName = (String)typeKey;
         long typeHash = Fnv.hashCode64(typeName);
         ObjectReader<T> reader = null;
         if ((features & JSONReader.Feature.SupportAutoType.mask) != 0L || this instanceof ObjectReaderSeeAlso) {
            reader = this.autoType(provider, typeHash);
         }

         if (reader == null) {
            reader = provider.getObjectReader(typeName, this.getObjectClass(), features2);
         }

         if (reader != this && reader != null) {
            return reader.createInstance(map, features);
         }
      }

      T object = this.createInstance(features);
      if (this.extraFieldReader == null && (features2 & (JSONReader.Feature.SupportSmartMatch.mask | JSONReader.Feature.ErrorOnUnknownProperties.mask)) == 0L) {
         boolean fieldBased = (features2 & JSONReader.Feature.FieldBased.mask) != 0L;

         for (int i = 0; i < this.fieldReaders.length; i++) {
            FieldReader fieldReader = this.fieldReaders[i];
            Object fieldValue = map.get(fieldReader.fieldName);
            if (fieldValue != null) {
               if (fieldValue.getClass() == fieldReader.fieldType) {
                  fieldReader.accept(object, fieldValue);
               } else if (fieldReader instanceof FieldReaderList && fieldValue instanceof JSONArray) {
                  ObjectReader objectReader = fieldReader.getObjectReader(provider);
                  Object fieldValueList = objectReader.createInstance((JSONArray)fieldValue, features);
                  fieldReader.accept(object, fieldValueList);
               } else if (fieldValue instanceof JSONObject && fieldReader.fieldType != JSONObject.class) {
                  JSONObject jsonObject = (JSONObject)fieldValue;
                  Object fieldValueJavaBean = provider.getObjectReader(fieldReader.fieldType, fieldBased).createInstance(jsonObject, features);
                  fieldReader.accept(object, fieldValueJavaBean);
               } else {
                  fieldReader.acceptAny(object, fieldValue, features);
               }
            }
         }
      } else {
         for (Entry entry : map.entrySet()) {
            String entryKey = entry.getKey().toString();
            Object fieldValue = entry.getValue();
            FieldReader fieldReader = this.getFieldReader(entryKey);
            if (fieldReader == null) {
               this.acceptExtra(object, entryKey, entry.getValue(), features);
            } else if (fieldValue != null && fieldValue.getClass() == fieldReader.fieldType) {
               fieldReader.accept(object, fieldValue);
            } else {
               fieldReader.acceptAny(object, fieldValue, features);
            }
         }
      }

      return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
   }
}
