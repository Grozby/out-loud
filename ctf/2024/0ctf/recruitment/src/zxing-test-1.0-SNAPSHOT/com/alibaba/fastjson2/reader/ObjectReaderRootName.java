package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ObjectReaderRootName<T> extends ObjectReaderAdapter<T> {
   protected final String rootName;
   protected final long rootNameHashCode;

   public ObjectReaderRootName(
      Class objectClass,
      String typeKey,
      String typeName,
      String rootName,
      long features,
      JSONSchema schema,
      Supplier creator,
      Function buildFunction,
      Class[] seeAlso,
      String[] seeAlsoNames,
      Class seeAlsoDefault,
      FieldReader[] fieldReaders
   ) {
      super(objectClass, typeKey, typeName, features, schema, creator, buildFunction, seeAlso, seeAlsoNames, seeAlsoDefault, fieldReaders);
      this.rootName = rootName;
      this.rootNameHashCode = rootName == null ? 0L : Fnv.hashCode64(rootName);
   }

   @Override
   public T createInstance(Map map, long features) {
      Map object = (Map)map.get(this.rootName);
      return object == null ? null : super.createInstance(object, features);
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         boolean objectStart = jsonReader.nextIfObjectStart();
         if (!objectStart) {
            throw new JSONException(jsonReader.info("read rootName error " + this.typeName));
         } else {
            T object = null;

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               if (this.rootNameHashCode == jsonReader.readFieldNameHashCode()) {
                  object = super.readJSONBObject(jsonReader, fieldType, fieldName, features);
               } else {
                  jsonReader.skipValue();
               }
            }

            return object;
         }
      }
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         boolean objectStart = jsonReader.nextIfObjectStart();
         if (!objectStart) {
            throw new JSONException(jsonReader.info("read rootName error " + this.typeName));
         } else {
            T object = null;

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               if (this.rootNameHashCode == jsonReader.readFieldNameHashCode()) {
                  object = super.readObject(jsonReader, fieldType, fieldName, features);
               } else {
                  jsonReader.skipValue();
               }
            }

            return object;
         }
      }
   }
}
