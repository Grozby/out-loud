package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReader2<T> extends ObjectReaderAdapter<T> {
   protected final FieldReader fieldReader0;
   protected final FieldReader fieldReader1;
   protected final long hashCode0;
   protected final long hashCode1;
   protected final long hashCode0LCase;
   protected final long hashCode1LCase;
   protected ObjectReader objectReader0;
   protected ObjectReader objectReader1;

   public ObjectReader2(Class objectClass, long features, JSONSchema schema, Supplier<T> creator, Function buildFunction, FieldReader first, FieldReader second) {
      this(objectClass, null, null, features, schema, creator, buildFunction, first, second);
   }

   public ObjectReader2(
      Class objectClass, String typeKey, String typeName, long features, Supplier<T> creator, Function buildFunction, FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   public ObjectReader2(
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
      this.fieldReader1 = fieldReaders[1];
      this.hashCode0 = this.fieldReader0.fieldNameHash;
      this.hashCode0LCase = this.fieldReader0.fieldNameHashLCase;
      this.hashCode1 = this.fieldReader1.fieldNameHash;
      this.hashCode1LCase = this.fieldReader1.fieldNameHashLCase;
      this.hasDefaultValue = this.fieldReader0.defaultValue != null || this.fieldReader1.defaultValue != null;
   }

   @Override
   protected void initDefaultValue(T object) {
      this.fieldReader0.acceptDefaultValue(object);
      this.fieldReader1.acceptDefaultValue(object);
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
            if (entryCnt > 1) {
               this.fieldReader1.readFieldValue(jsonReader, object);

               for (int i = 2; i < entryCnt; i++) {
                  jsonReader.skipValue();
               }
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

      ObjectReader autoTypeReader = jsonReader.checkAutoType(this.objectClass, this.typeNameHash, this.features | features);
      if (autoTypeReader != null && autoTypeReader.getObjectClass() != this.objectClass) {
         return (T)autoTypeReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.isArray()) {
         T object = this.creator.get();
         if (this.hasDefaultValue) {
            this.initDefaultValue(object);
         }

         int entryCnt = jsonReader.startArray();
         if (entryCnt > 0) {
            this.fieldReader0.readFieldValue(jsonReader, object);
            if (entryCnt > 1) {
               this.fieldReader1.readFieldValue(jsonReader, object);

               for (int i = 2; i < entryCnt; i++) {
                  jsonReader.skipValue();
               }
            }
         }

         return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
      } else if (!jsonReader.nextIfMatch((byte)-90)) {
         throw new JSONException(jsonReader.info("expect object, but " + JSONB.typeName(jsonReader.getType())));
      } else {
         T objectx;
         if (this.creator != null) {
            objectx = this.creator.get();
         } else if (((features | jsonReader.getContext().getFeatures()) & JSONReader.Feature.FieldBased.mask) != 0L) {
            try {
               objectx = (T)JDKUtils.UNSAFE.allocateInstance(this.objectClass);
            } catch (InstantiationException var12) {
               throw new JSONException(jsonReader.info("create instance error"), var12);
            }
         } else {
            objectx = null;
         }

         if (objectx != null && this.hasDefaultValue) {
            this.initDefaultValue(objectx);
         }

         if (objectx != null && jsonReader.isInitStringFieldAsEmpty()) {
            this.initStringFieldAsEmpty(objectx);
         }

         while (!jsonReader.nextIfMatch((byte)-91)) {
            long hashCode = jsonReader.readFieldNameHashCode();
            if (hashCode != 0L) {
               if (hashCode == this.hashCode0) {
                  this.fieldReader0.readFieldValue(jsonReader, objectx);
               } else if (hashCode == this.hashCode1) {
                  this.fieldReader1.readFieldValueJSONB(jsonReader, objectx);
               } else {
                  if (jsonReader.isSupportSmartMatch(features | this.features)) {
                     long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                     if (nameHashCodeLCase == this.hashCode0LCase) {
                        this.fieldReader0.readFieldValueJSONB(jsonReader, objectx);
                        continue;
                     }

                     if (nameHashCodeLCase == this.hashCode1LCase) {
                        this.fieldReader1.readFieldValueJSONB(jsonReader, objectx);
                        continue;
                     }
                  }

                  this.processExtra(jsonReader, objectx);
               }
            }
         }

         if (this.buildFunction != null) {
            objectx = (T)this.buildFunction.apply(objectx);
         }

         if (this.schema != null) {
            this.schema.assertValidate(objectx);
         }

         return objectx;
      }
   }

   @Override
   public T readObject(JSONReader jsonReader) {
      return this.readObject(jsonReader, null, null, this.features);
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (!this.serializable) {
         jsonReader.errorOnNoneSerializable(this.objectClass);
      }

      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.nextIfNull()) {
         jsonReader.nextIfComma();
         return null;
      } else {
         long featuresAll = jsonReader.features(this.features | features);
         if (jsonReader.isArray()) {
            if ((featuresAll & JSONReader.Feature.SupportArrayToBean.mask) != 0L) {
               jsonReader.next();
               T object = this.creator.get();
               if (this.hasDefaultValue) {
                  this.initDefaultValue(object);
               }

               this.fieldReader0.readFieldValue(jsonReader, object);
               this.fieldReader1.readFieldValue(jsonReader, object);
               if (jsonReader.current() != ']') {
                  throw new JSONException(jsonReader.info("array to bean end error"));
               } else {
                  jsonReader.next();
                  return object;
               }
            } else {
               return this.processObjectInputSingleItemArray(jsonReader, fieldType, fieldName, featuresAll);
            }
         } else {
            jsonReader.nextIfObjectStart();
            T objectx = this.creator.get();
            if (this.hasDefaultValue) {
               this.initDefaultValue(objectx);
            }

            if (objectx != null && (featuresAll & JSONReader.Feature.InitStringFieldAsEmpty.mask) != 0L) {
               this.initStringFieldAsEmpty(objectx);
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
                     objectx = (T)autoTypeObjectReader.readObject(jsonReader, fieldType, fieldName, features);
                     break;
                  }
               } else if (hashCode == this.hashCode0) {
                  this.fieldReader0.readFieldValue(jsonReader, objectx);
               } else if (hashCode == this.hashCode1) {
                  this.fieldReader1.readFieldValue(jsonReader, objectx);
               } else {
                  if (jsonReader.isSupportSmartMatch(features | this.features)) {
                     long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                     if (nameHashCodeLCase == this.hashCode0LCase) {
                        this.fieldReader0.readFieldValue(jsonReader, objectx);
                        continue;
                     }

                     if (nameHashCodeLCase == this.hashCode1LCase) {
                        this.fieldReader1.readFieldValue(jsonReader, objectx);
                        continue;
                     }
                  }

                  this.processExtra(jsonReader, objectx);
               }
            }

            jsonReader.nextIfMatch(',');
            if (this.buildFunction != null) {
               try {
                  objectx = (T)this.buildFunction.apply(objectx);
               } catch (IllegalStateException var17) {
                  throw new JSONException(jsonReader.info("build object error"), var17);
               }
            }

            if (this.schema != null) {
               this.schema.assertValidate(objectx);
            }

            return objectx;
         }
      }
   }

   @Override
   public FieldReader getFieldReader(long hashCode) {
      if (hashCode == this.hashCode0) {
         return this.fieldReader0;
      } else {
         return hashCode == this.hashCode1 ? this.fieldReader1 : null;
      }
   }

   @Override
   public FieldReader getFieldReaderLCase(long hashCode) {
      if (hashCode == this.hashCode0LCase) {
         return this.fieldReader0;
      } else {
         return hashCode == this.hashCode1LCase ? this.fieldReader1 : null;
      }
   }
}
