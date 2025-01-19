package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public interface ObjectReader<T> {
   long HASH_TYPE = Fnv.hashCode64("@type");
   String VALUE_NAME = "@value";

   default T createInstance() {
      return this.createInstance(0L);
   }

   default T createInstance(long features) {
      throw new UnsupportedOperationException();
   }

   default T createInstance(Collection collection) {
      return this.createInstance(collection, 0L);
   }

   default T createInstance(Collection collection, JSONReader.Feature... features) {
      return this.createInstance(collection, JSONReader.Feature.of(features));
   }

   default T createInstance(Collection collection, long features) {
      throw new UnsupportedOperationException(this.getClass().getName());
   }

   default void acceptExtra(Object object, String fieldName, Object fieldValue) {
      this.acceptExtra(object, fieldName, fieldValue, this.getFeatures());
   }

   default void acceptExtra(Object object, String fieldName, Object fieldValue, long features) {
   }

   default T createInstance(Map map, JSONReader.Feature... features) {
      long featuresValue = 0L;

      for (int i = 0; i < features.length; i++) {
         featuresValue |= features[i].mask;
      }

      return this.createInstance(map, featuresValue);
   }

   default T createInstance(Map map, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      Object typeKey = map.get(this.getTypeKey());
      if (typeKey instanceof String) {
         String typeName = (String)typeKey;
         long typeHash = Fnv.hashCode64(typeName);
         ObjectReader<T> reader = null;
         if ((features & JSONReader.Feature.SupportAutoType.mask) != 0L || this instanceof ObjectReaderSeeAlso) {
            reader = this.autoType(provider, typeHash);
         }

         if (reader == null) {
            reader = provider.getObjectReader(typeName, this.getObjectClass(), features | this.getFeatures());
         }

         if (reader != this && reader != null) {
            return reader.createInstance(map, features);
         }
      }

      T object = this.createInstance(0L);
      return this.accept(object, map, features);
   }

   default T accept(T object, Map map, long features) {
      for (Entry entry : map.entrySet()) {
         String entryKey = entry.getKey().toString();
         Object fieldValue = entry.getValue();
         FieldReader fieldReader = this.getFieldReader(entryKey);
         if (fieldReader == null) {
            this.acceptExtra(object, entryKey, entry.getValue(), features);
         } else {
            fieldReader.acceptAny(object, fieldValue, features);
         }
      }

      Function buildFunction = this.getBuildFunction();
      return (T)(buildFunction != null ? buildFunction.apply(object) : object);
   }

   default T createInstanceNoneDefaultConstructor(Map<Long, Object> values) {
      throw new UnsupportedOperationException();
   }

   default long getFeatures() {
      return 0L;
   }

   default String getTypeKey() {
      return "@type";
   }

   default long getTypeKeyHash() {
      return HASH_TYPE;
   }

   default Class<T> getObjectClass() {
      return null;
   }

   default FieldReader getFieldReader(long hashCode) {
      return null;
   }

   default FieldReader getFieldReaderLCase(long hashCode) {
      return null;
   }

   default boolean setFieldValue(Object object, String fieldName, long fieldNameHashCode, int value) {
      FieldReader fieldReader = this.getFieldReader(fieldNameHashCode);
      if (fieldReader == null) {
         return false;
      } else {
         fieldReader.accept(object, value);
         return true;
      }
   }

   default boolean setFieldValue(Object object, String fieldName, long fieldNameHashCode, long value) {
      FieldReader fieldReader = this.getFieldReader(fieldNameHashCode);
      if (fieldReader == null) {
         return false;
      } else {
         fieldReader.accept(object, value);
         return true;
      }
   }

   default FieldReader getFieldReader(String fieldName) {
      long fieldNameHash = Fnv.hashCode64(fieldName);
      FieldReader fieldReader = this.getFieldReader(fieldNameHash);
      if (fieldReader == null) {
         long fieldNameHashLCase = Fnv.hashCode64LCase(fieldName);
         if (fieldNameHashLCase != fieldNameHash) {
            fieldReader = this.getFieldReaderLCase(fieldNameHashLCase);
         }
      }

      return fieldReader;
   }

   default boolean setFieldValue(Object object, String fieldName, Object value) {
      FieldReader fieldReader = this.getFieldReader(fieldName);
      if (fieldReader == null) {
         return false;
      } else {
         fieldReader.accept(object, value);
         return true;
      }
   }

   default Function getBuildFunction() {
      return null;
   }

   default ObjectReader autoType(JSONReader.Context context, long typeHash) {
      return context.getObjectReaderAutoType(typeHash);
   }

   default ObjectReader autoType(ObjectReaderProvider provider, long typeHash) {
      return provider.getObjectReader(typeHash);
   }

   default T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.isArray() && jsonReader.isSupportBeanArray()) {
         return this.readArrayMappingJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         T object = null;
         jsonReader.nextIfObjectStart();
         JSONReader.Context context = jsonReader.getContext();
         long features2 = context.getFeatures() | features;

         for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
            long hash = jsonReader.readFieldNameHashCode();
            if (hash == this.getTypeKeyHash() && i == 0) {
               long typeHash = jsonReader.readTypeHashCode();
               ObjectReader reader = this.autoType(context, typeHash);
               if (reader == null) {
                  String typeName = jsonReader.getString();
                  reader = context.getObjectReaderAutoType(typeName, null);
                  if (reader == null) {
                     throw new JSONException(jsonReader.info("No suitable ObjectReader found for " + typeName));
                  }
               }

               if (reader != this) {
                  return (T)reader.readJSONBObject(jsonReader, fieldType, fieldName, features);
               }
            } else if (hash != 0L) {
               FieldReader fieldReader = this.getFieldReader(hash);
               if (fieldReader == null && jsonReader.isSupportSmartMatch(features2 | this.getFeatures())) {
                  long nameHashCodeLCase = jsonReader.getNameHashCodeLCase();
                  if (nameHashCodeLCase != hash) {
                     fieldReader = this.getFieldReaderLCase(nameHashCodeLCase);
                  }
               }

               if (fieldReader == null) {
                  jsonReader.skipValue();
               } else {
                  if (object == null) {
                     object = this.createInstance(features2);
                  }

                  fieldReader.readFieldValue(jsonReader, object);
               }
            }
         }

         return object != null ? object : this.createInstance(features2);
      }
   }

   default T readArrayMappingJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      throw new UnsupportedOperationException();
   }

   default T readArrayMappingObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      throw new UnsupportedOperationException();
   }

   default T readObject(String str, JSONReader.Feature... features) {
      JSONReader jsonReader = JSONReader.of(str, JSONFactory.createReadContext(features));

      Object var4;
      try {
         var4 = this.readObject(jsonReader, null, null, this.getFeatures());
      } catch (Throwable var7) {
         if (jsonReader != null) {
            try {
               jsonReader.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (jsonReader != null) {
         jsonReader.close();
      }

      return (T)var4;
   }

   default T readObject(JSONReader jsonReader) {
      return this.readObject(jsonReader, null, null, this.getFeatures());
   }

   default T readObject(JSONReader jsonReader, long features) {
      return this.readObject(jsonReader, null, null, features);
   }

   T readObject(JSONReader var1, Type var2, Object var3, long var4);
}
