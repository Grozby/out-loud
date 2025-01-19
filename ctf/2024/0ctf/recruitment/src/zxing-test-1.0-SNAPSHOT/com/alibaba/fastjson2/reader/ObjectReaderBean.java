package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.filter.ExtraProcessor;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ObjectReaderBean<T> implements ObjectReader<T> {
   protected final Class objectClass;
   protected final Supplier<T> creator;
   protected final Function buildFunction;
   protected final long features;
   protected final String typeName;
   protected final long typeNameHash;
   protected FieldReader extraFieldReader;
   protected boolean hasDefaultValue;
   protected final boolean serializable;
   protected final JSONSchema schema;
   protected JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler;

   protected ObjectReaderBean(Class objectClass, Supplier<T> creator, String typeName, long features, JSONSchema schema, Function buildFunction) {
      if (typeName == null && objectClass != null) {
         typeName = TypeUtils.getTypeName(objectClass);
      }

      this.objectClass = objectClass;
      this.creator = creator;
      this.buildFunction = buildFunction;
      this.features = features;
      this.typeName = typeName;
      this.typeNameHash = typeName != null ? Fnv.hashCode64(typeName) : 0L;
      this.schema = schema;
      this.serializable = objectClass != null && Serializable.class.isAssignableFrom(objectClass);
   }

   @Override
   public Class<T> getObjectClass() {
      return this.objectClass;
   }

   protected T processObjectInputSingleItemArray(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String message = "expect {, but [, class " + this.typeName;
      if (fieldName != null) {
         message = message + ", parent fieldName " + fieldName;
      }

      String info = jsonReader.info(message);
      long featuresAll = jsonReader.features(features);
      if ((featuresAll & JSONReader.Feature.SupportSmartMatch.mask) != 0L) {
         Type itemType = (Type)(fieldType == null ? this.objectClass : fieldType);
         List list = jsonReader.readArray(itemType);
         if (list != null) {
            if (list.size() == 0) {
               return null;
            }

            if (list.size() == 1) {
               return (T)list.get(0);
            }
         }
      }

      throw new JSONException(info);
   }

   protected void processExtra(JSONReader jsonReader, Object object) {
      this.processExtra(jsonReader, object, 0L);
   }

   protected void processExtra(JSONReader jsonReader, Object object, long features) {
      if ((jsonReader.features(this.features | features) & JSONReader.Feature.SupportSmartMatch.mask) != 0L) {
         String fieldName = jsonReader.getFieldName();
         if (fieldName.startsWith("is")) {
            String fieldName1 = fieldName.substring(2);
            long hashCode64LCase = Fnv.hashCode64LCase(fieldName1);
            FieldReader fieldReader = this.getFieldReaderLCase(hashCode64LCase);
            if (fieldReader != null) {
               Class fieldClass = fieldReader.fieldClass;
               if (fieldClass == Boolean.class || fieldClass == boolean.class) {
                  fieldReader.readFieldValue(jsonReader, object);
                  return;
               }
            }
         }
      }

      if (this.extraFieldReader != null && object != null) {
         this.extraFieldReader.processExtra(jsonReader, object);
      } else {
         ExtraProcessor extraProcessor = jsonReader.getContext().getExtraProcessor();
         if (extraProcessor != null) {
            String fieldName = jsonReader.getFieldName();
            Type type = extraProcessor.getType(fieldName);
            Object extraValue = jsonReader.read(type);
            extraProcessor.processExtra(object, fieldName, extraValue);
         } else if ((jsonReader.features(features) & JSONReader.Feature.ErrorOnUnknownProperties.mask) != 0L) {
            throw new JSONException("Unknown Property " + jsonReader.getFieldName());
         } else {
            jsonReader.skipValue();
         }
      }
   }

   @Override
   public void acceptExtra(Object object, String fieldName, Object fieldValue, long features) {
      if (this.extraFieldReader != null && object != null) {
         this.extraFieldReader.acceptExtra(object, fieldName, fieldValue);
      } else {
         if (fieldName.startsWith("is")) {
            String fieldName1 = fieldName.substring(2);
            long hashCode64LCase = Fnv.hashCode64LCase(fieldName1);
            FieldReader fieldReader = this.getFieldReaderLCase(hashCode64LCase);
            if (fieldReader != null) {
               Class fieldClass = fieldReader.fieldClass;
               if (fieldClass == Boolean.class || fieldClass == boolean.class) {
                  fieldReader.accept(object, fieldValue);
                  return;
               }
            }
         }

         if ((features & JSONReader.Feature.ErrorOnUnknownProperties.mask) != 0L) {
            throw new JSONException("Unknown Property " + fieldName);
         }
      }
   }

   @Deprecated
   public final ObjectReader checkAutoType(JSONReader jsonReader, Class expectClass, long features) {
      return this.checkAutoType(jsonReader, features);
   }

   public final ObjectReader checkAutoType(JSONReader jsonReader, long features) {
      return !jsonReader.nextIfMatchTypedAny() ? null : this.checkAutoType0(jsonReader, features);
   }

   protected final ObjectReader checkAutoType0(JSONReader jsonReader, long features) {
      Class expectClass = this.objectClass;
      long typeHash = jsonReader.readTypeHashCode();
      JSONReader.Context context = jsonReader.getContext();
      long features3 = jsonReader.features(features | this.features);
      JSONReader.AutoTypeBeforeHandler autoTypeFilter = context.getContextAutoTypeBeforeHandler();
      ObjectReader autoTypeObjectReader;
      if (autoTypeFilter != null) {
         Class<?> filterClass = autoTypeFilter.apply(typeHash, expectClass, features);
         if (filterClass == null) {
            String typeName = jsonReader.getString();
            filterClass = autoTypeFilter.apply(typeName, expectClass, features);
            if (filterClass != null && !expectClass.isAssignableFrom(filterClass)) {
               if ((jsonReader.features(features) & JSONReader.Feature.IgnoreAutoTypeNotMatch.mask) == 0L) {
                  throw this.notMatchError();
               }

               filterClass = expectClass;
            }
         }

         autoTypeObjectReader = context.getObjectReader(filterClass);
      } else {
         autoTypeObjectReader = jsonReader.getObjectReaderAutoType(typeHash, expectClass, features);
         if (autoTypeObjectReader == null) {
            throw this.auotypeError(jsonReader);
         }

         Class autoTypeObjectReaderClass = autoTypeObjectReader.getObjectClass();
         if (expectClass != null && autoTypeObjectReaderClass != null && !expectClass.isAssignableFrom(autoTypeObjectReaderClass)) {
            if ((features3 & JSONReader.Feature.IgnoreAutoTypeNotMatch.mask) == 0L) {
               throw this.notMatchError();
            }

            autoTypeObjectReader = context.getObjectReader(expectClass);
         } else if (typeHash == this.typeNameHash || (features3 & JSONReader.Feature.SupportAutoType.mask) == 0L) {
            autoTypeObjectReader = null;
         }
      }

      if (autoTypeObjectReader == this || autoTypeObjectReader != null && autoTypeObjectReader.getObjectClass() == this.objectClass) {
         autoTypeObjectReader = null;
      }

      return autoTypeObjectReader;
   }

   private JSONException notMatchError() {
      return new JSONException("type not match. " + this.typeName + " -> " + this.objectClass.getName());
   }

   private JSONException auotypeError(JSONReader jsonReader) {
      return new JSONException(jsonReader.info("autoType not support"));
   }

   protected void initDefaultValue(T object) {
   }

   public void readObject(JSONReader jsonReader, Object object, long features) {
      if (jsonReader.nextIfNull()) {
         jsonReader.nextIfComma();
      } else {
         boolean objectStart = jsonReader.nextIfObjectStart();
         if (!objectStart) {
            throw new JSONException(jsonReader.info());
         } else {
            while (!jsonReader.nextIfObjectEnd()) {
               long hash = jsonReader.readFieldNameHashCode();
               FieldReader fieldReader = this.getFieldReader(hash);
               if (fieldReader == null && jsonReader.isSupportSmartMatch(features | this.getFeatures())) {
                  long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                  fieldReader = this.getFieldReaderLCase(nameHashCodeLCase);
               }

               if (fieldReader == null) {
                  this.processExtra(jsonReader, object);
               } else {
                  fieldReader.readFieldValue(jsonReader, object);
               }
            }

            jsonReader.nextIfComma();
            if (this.schema != null) {
               this.schema.assertValidate(object);
            }
         }
      }
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.nextIfNullOrEmptyString()) {
         jsonReader.nextIfComma();
         return null;
      } else {
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

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               JSONReader.Context context = jsonReader.getContext();
               long hash = jsonReader.readFieldNameHashCode();
               JSONReader.AutoTypeBeforeHandler autoTypeFilter = this.autoTypeBeforeHandler;
               if (autoTypeFilter == null) {
                  autoTypeFilter = context.getContextAutoTypeBeforeHandler();
               }

               long features3;
               if (i != 0
                  || hash != this.getTypeKeyHash()
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
                  if (autoTypeFilter != null) {
                     Class<?> filterClass = autoTypeFilter.apply(typeHash, this.objectClass, features3);
                     if (filterClass == null) {
                        filterClass = autoTypeFilter.apply(jsonReader.getString(), this.objectClass, features3);
                        if (filterClass != null) {
                           reader = context.getObjectReader(filterClass);
                        }
                     }
                  }

                  if (reader == null) {
                     reader = this.autoType(context, typeHash);
                  }

                  String typeName = null;
                  if (reader == null) {
                     typeName = jsonReader.getString();
                     reader = context.getObjectReaderAutoType(typeName, this.objectClass, features3);
                     if (reader == null) {
                        throw new JSONException(jsonReader.info("No suitable ObjectReader found for " + typeName));
                     }
                  }

                  if (reader != this) {
                     FieldReader fieldReaderx = reader.getFieldReader(hash);
                     if (fieldReaderx != null && typeName == null) {
                        typeName = jsonReader.getString();
                     }

                     object = (T)reader.readObject(jsonReader, null, null, features | this.getFeatures());
                     if (fieldReaderx != null) {
                        fieldReaderx.accept(object, typeName);
                     }

                     return object;
                  }
               }
            }

            if (object == null) {
               object = this.createInstance(jsonReader.getContext().getFeatures() | features);
               if (object != null && (featuresAll & JSONReader.Feature.InitStringFieldAsEmpty.mask) != 0L) {
                  this.initStringFieldAsEmpty(object);
               }
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

   protected void initStringFieldAsEmpty(Object object) {
   }

   public JSONReader.AutoTypeBeforeHandler getAutoTypeBeforeHandler() {
      return this.autoTypeBeforeHandler;
   }

   public void setAutoTypeBeforeHandler(JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler) {
      this.autoTypeBeforeHandler = autoTypeBeforeHandler;
   }

   protected boolean readFieldValueWithLCase(JSONReader jsonReader, Object object, long hashCode64, long features2) {
      if (jsonReader.isSupportSmartMatch(features2)) {
         long hashCode64L = jsonReader.getNameHashCodeLCase();
         if (hashCode64L != hashCode64) {
            FieldReader fieldReader = this.getFieldReaderLCase(hashCode64L);
            if (fieldReader != null) {
               fieldReader.readFieldValue(jsonReader, object);
               return true;
            }
         }
      }

      return false;
   }
}
