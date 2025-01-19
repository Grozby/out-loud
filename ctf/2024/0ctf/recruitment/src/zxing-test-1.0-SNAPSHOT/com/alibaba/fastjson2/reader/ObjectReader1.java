package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReader1<T> extends ObjectReaderAdapter<T> {
   protected final FieldReader fieldReader0;
   final long hashCode0;
   final long hashCode0LCase;
   protected ObjectReader objectReader0;

   public ObjectReader1(Class objectClass, long features, JSONSchema schema, Supplier<T> creator, Function buildFunction, FieldReader fieldReader) {
      this(objectClass, null, null, features, schema, creator, buildFunction, fieldReader);
   }

   public ObjectReader1(
      Class objectClass, String typeKey, String typeName, long features, Supplier<T> creator, Function buildFunction, FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   public ObjectReader1(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      JSONSchema schema,
      Supplier<T> creator,
      Function buildFunction,
      FieldReader... fieldReaders
   ) {
      super(objectClass, typeKey, typeName, features, schema, creator, buildFunction, fieldReaders);
      this.fieldReader0 = fieldReaders[0];
      this.hashCode0 = this.fieldReader0.fieldNameHash;
      this.hashCode0LCase = this.fieldReader0.fieldNameHashLCase;
      this.hasDefaultValue = this.fieldReader0.defaultValue != null;
   }

   @Override
   public T readObject(JSONReader jsonReader) {
      return this.readObject(jsonReader, null, null, this.features);
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
         T object = this.creator.get();
         int entryCnt = jsonReader.startArray();
         if (entryCnt > 0) {
            this.fieldReader0.readFieldValue(jsonReader, object);

            for (int i = 1; i < entryCnt; i++) {
               jsonReader.skipValue();
            }
         }

         return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
      }
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      ObjectReader autoTypeReader = this.checkAutoType(jsonReader, features);
      if (autoTypeReader != null) {
         return (T)autoTypeReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.isArray()) {
         T object = this.creator.get();
         int entryCnt = jsonReader.startArray();
         if (entryCnt > 0) {
            this.fieldReader0.readFieldValue(jsonReader, object);

            for (int i = 1; i < entryCnt; i++) {
               jsonReader.skipValue();
            }
         }

         return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
      } else {
         if (!jsonReader.nextIfMatch((byte)-90)) {
            if (!jsonReader.isTypeRedirect()) {
               throw new JSONException(jsonReader.info("expect object, but " + JSONB.typeName(jsonReader.getType())));
            }

            jsonReader.setTypeRedirect(false);
         }

         T object;
         if (this.creator != null) {
            object = this.creator.get();
         } else if (((features | jsonReader.getContext().getFeatures()) & JSONReader.Feature.FieldBased.mask) != 0L) {
            try {
               object = (T)JDKUtils.UNSAFE.allocateInstance(this.objectClass);
            } catch (InstantiationException var16) {
               throw new JSONException(jsonReader.info("create instance error"), var16);
            }
         } else {
            object = null;
         }

         if (object != null && this.hasDefaultValue) {
            this.initDefaultValue(object);
         }

         for (int i = 0; !jsonReader.nextIfMatch((byte)-91); i++) {
            long hashCode = jsonReader.readFieldNameHashCode();
            if (hashCode == this.getTypeKeyHash() && i == 0) {
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

               if (autoTypeObjectReader != this) {
                  return (T)autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
               }
            } else if (hashCode != 0L) {
               if (hashCode == this.hashCode0) {
                  this.fieldReader0.readFieldValueJSONB(jsonReader, object);
               } else if (jsonReader.isSupportSmartMatch(features | this.features) && jsonReader.getNameHashCodeLCase() == this.hashCode0LCase) {
                  this.fieldReader0.readFieldValue(jsonReader, object);
               } else {
                  this.processExtra(jsonReader, object);
               }
            }
         }

         if (this.buildFunction != null) {
            object = (T)this.buildFunction.apply(object);
         }

         if (this.schema != null) {
            this.schema.assertValidate(object);
         }

         return object;
      }
   }

   @Override
   protected void initDefaultValue(T object) {
      this.fieldReader0.acceptDefaultValue(object);
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         long featuresAll = jsonReader.features(this.features | features);
         if (jsonReader.isArray()) {
            if ((featuresAll & JSONReader.Feature.SupportArrayToBean.mask) != 0L) {
               jsonReader.next();
               T object = this.creator.get();
               this.fieldReader0.readFieldValue(jsonReader, object);
               if (!jsonReader.nextIfArrayEnd()) {
                  throw new JSONException(jsonReader.info("array to bean end error, " + jsonReader.current()));
               } else {
                  jsonReader.nextIfComma();
                  return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
               }
            } else {
               return this.processObjectInputSingleItemArray(jsonReader, fieldType, fieldName, featuresAll);
            }
         } else {
            jsonReader.nextIfObjectStart();
            T object = this.creator != null ? this.creator.get() : null;
            if (this.hasDefaultValue) {
               this.initDefaultValue(object);
            }

            if (object != null && (featuresAll & JSONReader.Feature.InitStringFieldAsEmpty.mask) != 0L) {
               this.initStringFieldAsEmpty(object);
            }

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               long hashCode = jsonReader.readFieldNameHashCode();
               if (i == 0 && hashCode == HASH_TYPE) {
                  long typeHash = jsonReader.readTypeHashCode();
                  JSONReader.Context context = jsonReader.getContext();
                  ObjectReader autoTypeObjectReader = context.getObjectReaderAutoType(typeHash);
                  if (autoTypeObjectReader == null) {
                     String typeName = jsonReader.getString();
                     autoTypeObjectReader = context.getObjectReaderAutoType(typeName, this.objectClass);
                     if (autoTypeObjectReader == null) {
                        continue;
                     }
                  }

                  if (autoTypeObjectReader != this) {
                     object = (T)autoTypeObjectReader.readObject(jsonReader, fieldType, fieldName, features);
                     break;
                  }
               } else if (hashCode == this.hashCode0) {
                  this.fieldReader0.readFieldValue(jsonReader, object);
               } else if (jsonReader.isSupportSmartMatch(features | this.features) && jsonReader.getNameHashCodeLCase() == this.hashCode0LCase) {
                  this.fieldReader0.readFieldValue(jsonReader, object);
               } else {
                  this.processExtra(jsonReader, object);
               }
            }

            jsonReader.nextIfComma();
            if (this.buildFunction != null) {
               object = (T)this.buildFunction.apply(object);
            }

            if (this.schema != null) {
               this.schema.assertValidate(object);
            }

            return object;
         }
      }
   }

   @Override
   public FieldReader getFieldReader(long hashCode) {
      return hashCode == this.hashCode0 ? this.fieldReader0 : null;
   }

   @Override
   public FieldReader getFieldReaderLCase(long hashCode) {
      return hashCode == this.hashCode0LCase ? this.fieldReader0 : null;
   }

   @Override
   public boolean setFieldValue(Object object, String fieldName, long fieldNameHashCode, int value) {
      if (this.hashCode0 != fieldNameHashCode) {
         return false;
      } else {
         this.fieldReader0.accept(object, value);
         return true;
      }
   }
}
