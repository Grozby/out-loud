package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

final class ObjectReaderSeeAlso<T> extends ObjectReaderAdapter<T> {
   ObjectReaderSeeAlso(
      Class objectType, Supplier<T> defaultCreator, String typeKey, Class[] seeAlso, String[] seeAlsoNames, Class seeAlsoDefault, FieldReader... fieldReaders
   ) {
      super(objectType, typeKey, null, JSONReader.Feature.SupportAutoType.mask, null, defaultCreator, null, seeAlso, seeAlsoNames, seeAlsoDefault, fieldReaders);
   }

   ObjectReaderSeeAlso addSubType(Class subTypeClass, String subTypeClassName) {
      for (int i = 0; i < this.seeAlso.length; i++) {
         if (this.seeAlso[i] == subTypeClass) {
            return this;
         }
      }

      Class[] seeAlso1 = Arrays.copyOf(this.seeAlso, this.seeAlso.length + 1);
      String[] seeAlsoNames1 = Arrays.copyOf(this.seeAlsoNames, this.seeAlsoNames.length + 1);
      seeAlso1[seeAlso1.length - 1] = subTypeClass;
      if (subTypeClassName == null) {
         JSONType jsonType = subTypeClass.getAnnotation(JSONType.class);
         if (jsonType != null) {
            subTypeClassName = jsonType.typeName();
         }
      }

      if (subTypeClassName != null) {
         seeAlsoNames1[seeAlsoNames1.length - 1] = subTypeClassName;
      }

      return new ObjectReaderSeeAlso<>(this.objectClass, this.creator, this.typeKey, seeAlso1, seeAlsoNames1, this.seeAlsoDefault, this.fieldReaders);
   }

   @Override
   public T createInstance(long features) {
      return this.creator == null ? null : this.creator.get();
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         if (!this.serializable) {
            jsonReader.errorOnNoneSerializable(this.objectClass);
         }

         if (jsonReader.nextIfNull()) {
            jsonReader.nextIfComma();
            return null;
         } else if (jsonReader.isString()) {
            long valueHashCode = jsonReader.readValueHashCode();

            for (int i = 0; i < this.seeAlso.length; i++) {
               Class seeAlsoType = this.seeAlso[i];
               if (Enum.class.isAssignableFrom(seeAlsoType)) {
                  ObjectReader seeAlsoTypeReader = jsonReader.getObjectReader(seeAlsoType);
                  Enum e = null;
                  if (seeAlsoTypeReader instanceof ObjectReaderImplEnum) {
                     e = ((ObjectReaderImplEnum)seeAlsoTypeReader).getEnumByHashCode(valueHashCode);
                  }

                  if (e != null) {
                     return (T)e;
                  }
               }
            }

            String strVal = jsonReader.getString();
            throw new JSONException(jsonReader.info("not support input " + strVal));
         } else {
            JSONReader.SavePoint savePoint = jsonReader.mark();
            long featuresAll = jsonReader.features(this.getFeatures() | features);
            if (jsonReader.isArray()) {
               return (featuresAll & JSONReader.Feature.SupportArrayToBean.mask) != 0L
                  ? this.readArrayMappingObject(jsonReader, fieldType, fieldName, features)
                  : this.processObjectInputSingleItemArray(jsonReader, fieldType, fieldName, featuresAll);
            } else {
               T object = null;
               boolean objectStart = jsonReader.nextIfObjectStart();
               if (!objectStart) {
                  char ch = jsonReader.current();
                  if (ch == 't' || ch == 'f') {
                     jsonReader.readBoolValue();
                     return null;
                  }

                  if (ch != '"' && ch != '\'' && ch != '}') {
                     throw new JSONException(jsonReader.info());
                  }
               }

               for (int ix = 0; !jsonReader.nextIfObjectEnd(); ix++) {
                  JSONReader.Context context = jsonReader.getContext();
                  long hash = jsonReader.readFieldNameHashCode();
                  JSONReader.AutoTypeBeforeHandler autoTypeFilter = context.getContextAutoTypeBeforeHandler();
                  long features3;
                  if (hash != this.getTypeKeyHash()
                     || ((features3 = features | this.getFeatures() | context.getFeatures()) & JSONReader.Feature.SupportAutoType.mask) == 0L
                        && autoTypeFilter == null) {
                     FieldReader fieldReader = this.getFieldReader(hash);
                     if (fieldReader == null && jsonReader.isSupportSmartMatch(features | this.getFeatures())) {
                        long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                        fieldReader = this.getFieldReaderLCase(nameHashCodeLCase);
                     }

                     if (object == null) {
                        object = this.createInstance(jsonReader.getContext().getFeatures() | features);
                     }

                     if (fieldReader == null) {
                        this.processExtra(jsonReader, object);
                     } else {
                        fieldReader.readFieldValue(jsonReader, object);
                     }
                  } else {
                     ObjectReader reader = null;
                     long typeHash = jsonReader.readTypeHashCode();
                     Number typeNumber = null;
                     String typeNumberStr = null;
                     if (typeHash == -1L && jsonReader.isNumber()) {
                        typeNumber = jsonReader.readNumber();
                        typeNumberStr = typeNumber.toString();
                        typeHash = Fnv.hashCode64(typeNumberStr);
                     }

                     if (autoTypeFilter != null) {
                        Class<?> filterClass = autoTypeFilter.apply(typeHash, this.objectClass, features3);
                        if (filterClass == null) {
                           filterClass = autoTypeFilter.apply(jsonReader.getString(), this.objectClass, features3);
                           if (filterClass != null) {
                              reader = context.getObjectReader(filterClass);
                           }
                        }
                     }

                     String typeName = null;
                     if (reader == null) {
                        reader = this.autoType(context, typeHash);
                        if (reader != null && hash != HASH_TYPE) {
                           typeName = jsonReader.getString();
                        }
                     }

                     if (reader == null) {
                        typeName = jsonReader.getString();
                        reader = context.getObjectReaderAutoType(typeName, this.objectClass, features3);
                        if (reader == null && this.seeAlsoDefault != null) {
                           reader = context.getObjectReader(this.seeAlsoDefault);
                        }

                        if (reader == null) {
                           throw new JSONException(jsonReader.info("No suitable ObjectReader found for" + typeName));
                        }
                     }

                     if (reader != this) {
                        FieldReader fieldReaderx = reader.getFieldReader(hash);
                        if (fieldReaderx == null && hash != HASH_TYPE) {
                           fieldReaderx = reader.getFieldReader(this.typeKey);
                        }

                        if (fieldReaderx != null && typeName == null) {
                           if (typeNumberStr != null) {
                              typeName = typeNumberStr;
                           } else {
                              typeName = jsonReader.getString();
                           }
                        }

                        if (ix != 0) {
                           jsonReader.reset(savePoint);
                        }

                        object = (T)reader.readObject(jsonReader, fieldType, fieldName, features | this.getFeatures());
                        if (fieldReaderx != null) {
                           if (typeNumber != null) {
                              fieldReaderx.accept(object, typeNumber);
                           } else {
                              fieldReaderx.accept(object, typeName);
                           }
                        }

                        return object;
                     }
                  }
               }

               if (object == null) {
                  object = this.createInstance(jsonReader.getContext().getFeatures() | features);
               }

               jsonReader.nextIfComma();
               Function buildFunction = this.getBuildFunction();
               if (buildFunction != null) {
                  object = (T)buildFunction.apply(object);
               }

               if (this.schema != null) {
                  this.schema.assertValidate(object);
               }

               return object;
            }
         }
      }
   }
}
