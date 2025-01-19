package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReader5<T> extends ObjectReaderAdapter<T> {
   protected final FieldReader fieldReader0;
   protected final FieldReader fieldReader1;
   protected final FieldReader fieldReader2;
   protected final FieldReader fieldReader3;
   protected final FieldReader fieldReader4;
   final long hashCode0;
   final long hashCode1;
   final long hashCode2;
   final long hashCode3;
   final long hashCode4;
   final long hashCode0LCase;
   final long hashCode1LCase;
   final long hashCode2LCase;
   final long hashCode3LCase;
   final long hashCode4LCase;
   protected ObjectReader objectReader0;
   protected ObjectReader objectReader1;
   protected ObjectReader objectReader2;
   protected ObjectReader objectReader3;
   protected ObjectReader objectReader4;

   ObjectReader5(
      Class objectClass,
      Supplier<T> creator,
      long features,
      JSONSchema schema,
      Function buildFunction,
      FieldReader fieldReader0,
      FieldReader fieldReader1,
      FieldReader fieldReader2,
      FieldReader fieldReader3,
      FieldReader fieldReader4
   ) {
      this(objectClass, null, null, features, schema, creator, buildFunction, fieldReader0, fieldReader1, fieldReader2, fieldReader3, fieldReader4);
   }

   public ObjectReader5(
      Class objectClass, String typeKey, String typeName, long features, Supplier<T> creator, Function buildFunction, FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   public ObjectReader5(
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
      this.fieldReader2 = fieldReaders[2];
      this.fieldReader3 = fieldReaders[3];
      this.fieldReader4 = fieldReaders[4];
      this.hashCode0 = this.fieldReader0.fieldNameHash;
      this.hashCode1 = this.fieldReader1.fieldNameHash;
      this.hashCode2 = this.fieldReader2.fieldNameHash;
      this.hashCode3 = this.fieldReader3.fieldNameHash;
      this.hashCode4 = this.fieldReader4.fieldNameHash;
      this.hashCode0LCase = this.fieldReader0.fieldNameHashLCase;
      this.hashCode1LCase = this.fieldReader1.fieldNameHashLCase;
      this.hashCode2LCase = this.fieldReader2.fieldNameHashLCase;
      this.hashCode3LCase = this.fieldReader3.fieldNameHashLCase;
      this.hashCode4LCase = this.fieldReader4.fieldNameHashLCase;
      this.hasDefaultValue = this.fieldReader0.defaultValue != null
         || this.fieldReader1.defaultValue != null
         || this.fieldReader2.defaultValue != null
         || this.fieldReader3.defaultValue != null
         || this.fieldReader4.defaultValue != null;
   }

   @Override
   protected void initDefaultValue(T object) {
      this.fieldReader0.acceptDefaultValue(object);
      this.fieldReader1.acceptDefaultValue(object);
      this.fieldReader2.acceptDefaultValue(object);
      this.fieldReader3.acceptDefaultValue(object);
      this.fieldReader4.acceptDefaultValue(object);
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
               if (entryCnt > 2) {
                  this.fieldReader2.readFieldValue(jsonReader, object);
                  if (entryCnt > 3) {
                     this.fieldReader3.readFieldValue(jsonReader, object);
                     if (entryCnt > 4) {
                        this.fieldReader4.readFieldValue(jsonReader, object);

                        for (int i = 5; i < entryCnt; i++) {
                           jsonReader.skipValue();
                        }
                     }
                  }
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

      if (!jsonReader.isArray()) {
         ObjectReader autoTypeReader = jsonReader.checkAutoType(this.objectClass, this.typeNameHash, this.features | features);
         if (autoTypeReader != null && autoTypeReader.getObjectClass() != this.objectClass) {
            return (T)autoTypeReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
         } else if (!jsonReader.nextIfMatch((byte)-90)) {
            throw new JSONException("expect object, but " + JSONB.typeName(jsonReader.getType()));
         } else {
            T object;
            if (this.creator != null) {
               object = this.creator.get();
            } else if (((features | jsonReader.getContext().getFeatures()) & JSONReader.Feature.FieldBased.mask) != 0L) {
               try {
                  object = (T)JDKUtils.UNSAFE.allocateInstance(this.objectClass);
               } catch (InstantiationException var12) {
                  throw new JSONException(jsonReader.info("create instance error"), var12);
               }
            } else {
               object = null;
            }

            if (object != null && this.hasDefaultValue) {
               this.initDefaultValue(object);
            }

            while (!jsonReader.nextIfMatch((byte)-91)) {
               long hashCode = jsonReader.readFieldNameHashCode();
               if (hashCode != 0L) {
                  if (hashCode == this.hashCode0) {
                     this.fieldReader0.readFieldValue(jsonReader, object);
                  } else if (hashCode == this.hashCode1) {
                     this.fieldReader1.readFieldValue(jsonReader, object);
                  } else if (hashCode == this.hashCode2) {
                     this.fieldReader2.readFieldValue(jsonReader, object);
                  } else if (hashCode == this.hashCode3) {
                     this.fieldReader3.readFieldValue(jsonReader, object);
                  } else if (hashCode == this.hashCode4) {
                     this.fieldReader4.readFieldValue(jsonReader, object);
                  } else if (!jsonReader.isSupportSmartMatch(features | this.features)) {
                     this.processExtra(jsonReader, object);
                  } else {
                     long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                     if (nameHashCodeLCase == this.hashCode0LCase) {
                        this.fieldReader0.readFieldValue(jsonReader, object);
                     } else if (nameHashCodeLCase == this.hashCode1LCase) {
                        this.fieldReader1.readFieldValue(jsonReader, object);
                     } else if (nameHashCodeLCase == this.hashCode2LCase) {
                        this.fieldReader2.readFieldValue(jsonReader, object);
                     } else if (nameHashCodeLCase == this.hashCode3LCase) {
                        this.fieldReader3.readFieldValue(jsonReader, object);
                     } else if (nameHashCodeLCase == this.hashCode4LCase) {
                        this.fieldReader4.readFieldValue(jsonReader, object);
                     } else {
                        this.processExtra(jsonReader, object);
                     }
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
      } else {
         T objectx = this.creator.get();
         int entryCnt = jsonReader.startArray();
         if (entryCnt > 0) {
            this.fieldReader0.readFieldValue(jsonReader, objectx);
            if (entryCnt > 1) {
               this.fieldReader1.readFieldValue(jsonReader, objectx);
               if (entryCnt > 2) {
                  this.fieldReader2.readFieldValue(jsonReader, objectx);
                  if (entryCnt > 3) {
                     this.fieldReader3.readFieldValue(jsonReader, objectx);
                     if (entryCnt > 4) {
                        this.fieldReader4.readFieldValue(jsonReader, objectx);

                        for (int i = 5; i < entryCnt; i++) {
                           jsonReader.skipValue();
                        }
                     }
                  }
               }
            }
         }

         return (T)(this.buildFunction != null ? this.buildFunction.apply(objectx) : objectx);
      }
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
               jsonReader.nextIfArrayStart();
               T object = this.creator.get();
               if (this.hasDefaultValue) {
                  this.initDefaultValue(object);
               }

               this.fieldReader0.readFieldValue(jsonReader, object);
               this.fieldReader1.readFieldValue(jsonReader, object);
               this.fieldReader2.readFieldValue(jsonReader, object);
               this.fieldReader3.readFieldValue(jsonReader, object);
               this.fieldReader4.readFieldValue(jsonReader, object);
               if (!jsonReader.nextIfArrayEnd()) {
                  throw new JSONException(jsonReader.info("array to bean end error"));
               } else {
                  jsonReader.nextIfComma();
                  return (T)(this.buildFunction != null ? this.buildFunction.apply(object) : object);
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
               } else {
                  if (hashCode == -1L) {
                     break;
                  }

                  if (hashCode == this.hashCode0) {
                     this.fieldReader0.readFieldValue(jsonReader, objectx);
                  } else if (hashCode == this.hashCode1) {
                     this.fieldReader1.readFieldValue(jsonReader, objectx);
                  } else if (hashCode == this.hashCode2) {
                     this.fieldReader2.readFieldValue(jsonReader, objectx);
                  } else if (hashCode == this.hashCode3) {
                     this.fieldReader3.readFieldValue(jsonReader, objectx);
                  } else if (hashCode == this.hashCode4) {
                     this.fieldReader4.readFieldValue(jsonReader, objectx);
                  } else if (!jsonReader.isSupportSmartMatch(features | this.features)) {
                     this.processExtra(jsonReader, objectx);
                  } else {
                     long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                     if (nameHashCodeLCase == this.hashCode0LCase) {
                        this.fieldReader0.readFieldValue(jsonReader, objectx);
                     } else if (nameHashCodeLCase == this.hashCode1LCase) {
                        this.fieldReader1.readFieldValue(jsonReader, objectx);
                     } else if (nameHashCodeLCase == this.hashCode2LCase) {
                        this.fieldReader2.readFieldValue(jsonReader, objectx);
                     } else if (nameHashCodeLCase == this.hashCode3LCase) {
                        this.fieldReader3.readFieldValue(jsonReader, objectx);
                     } else if (nameHashCodeLCase == this.hashCode4LCase) {
                        this.fieldReader4.readFieldValue(jsonReader, objectx);
                     } else {
                        this.processExtra(jsonReader, objectx);
                     }
                  }
               }
            }

            jsonReader.nextIfComma();
            if (this.buildFunction != null) {
               objectx = (T)this.buildFunction.apply(objectx);
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
      } else if (hashCode == this.hashCode1) {
         return this.fieldReader1;
      } else if (hashCode == this.hashCode2) {
         return this.fieldReader2;
      } else if (hashCode == this.hashCode3) {
         return this.fieldReader3;
      } else {
         return hashCode == this.hashCode4 ? this.fieldReader4 : null;
      }
   }

   @Override
   public FieldReader getFieldReaderLCase(long hashCode) {
      if (hashCode == this.hashCode0LCase) {
         return this.fieldReader0;
      } else if (hashCode == this.hashCode1LCase) {
         return this.fieldReader1;
      } else if (hashCode == this.hashCode2LCase) {
         return this.fieldReader2;
      } else if (hashCode == this.hashCode3LCase) {
         return this.fieldReader3;
      } else {
         return hashCode == this.hashCode4LCase ? this.fieldReader4 : null;
      }
   }
}
