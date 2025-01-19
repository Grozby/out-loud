package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ObjectReaderInterface<T> extends ObjectReaderAdapter<T> {
   public ObjectReaderInterface(
      Class objectClass, String typeKey, String typeName, long features, Supplier creator, Function buildFunction, FieldReader[] fieldReaders
   ) {
      super(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         ObjectReader autoTypeReader = jsonReader.checkAutoType(this.objectClass, this.typeNameHash, this.features | features);
         if (autoTypeReader != null && autoTypeReader.getObjectClass() != this.objectClass) {
            return (T)autoTypeReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
         } else if (jsonReader.isArray()) {
            if (jsonReader.isSupportBeanArray()) {
               return this.readArrayMappingJSONBObject(jsonReader, fieldType, fieldName, features);
            } else {
               throw new JSONException(jsonReader.info("expect object, but " + JSONB.typeName(jsonReader.getType())));
            }
         } else {
            boolean objectStart = jsonReader.nextIfObjectStart();
            JSONObject jsonObject = new JSONObject();

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
                     jsonObject.put(jsonReader.getFieldName(), jsonReader.readAny());
                  } else {
                     Object fieldValue = fieldReader.readFieldValue(jsonReader);
                     jsonObject.put(fieldReader.fieldName, fieldValue);
                  }
               }
            }

            Object object = TypeUtils.newProxyInstance(this.objectClass, jsonObject);
            if (this.schema != null) {
               this.schema.assertValidate(object);
            }

            return (T)object;
         }
      }
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else if (jsonReader.nextIfNull()) {
         jsonReader.nextIfComma();
         return null;
      } else if (jsonReader.isArray() && jsonReader.isSupportBeanArray(this.getFeatures() | features)) {
         return this.readArrayMappingObject(jsonReader, fieldType, fieldName, features);
      } else {
         JSONObject jsonObject = new JSONObject();
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
            JSONReader.AutoTypeBeforeHandler autoTypeFilter = context.getContextAutoTypeBeforeHandler();
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

               if (fieldReader == null) {
                  jsonObject.put(jsonReader.getFieldName(), jsonReader.readAny());
               } else {
                  Object fieldValue = fieldReader.readFieldValue(jsonReader);
                  jsonObject.put(fieldReader.fieldName, fieldValue);
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
                     throw new JSONException(jsonReader.info("No suitable ObjectReader found for" + typeName));
                  }
               }

               if (reader != this) {
                  FieldReader fieldReaderx = reader.getFieldReader(hash);
                  if (fieldReaderx != null && typeName == null) {
                     typeName = jsonReader.getString();
                  }

                  T object = (T)reader.readObject(jsonReader, null, null, features | this.getFeatures());
                  if (fieldReaderx != null) {
                     fieldReaderx.accept(object, typeName);
                  }

                  return object;
               }
            }
         }

         jsonReader.nextIfComma();
         T object = TypeUtils.newProxyInstance(this.objectClass, jsonObject);
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

   @Override
   public T createInstance(long features) {
      JSONObject object = new JSONObject();
      return TypeUtils.newProxyInstance(this.objectClass, object);
   }

   @Override
   public T createInstance(Map map, long features) {
      JSONObject object;
      if (map instanceof JSONObject) {
         object = (JSONObject)map;
      } else {
         object = new JSONObject(map);
      }

      return TypeUtils.newProxyInstance(this.objectClass, object);
   }
}
