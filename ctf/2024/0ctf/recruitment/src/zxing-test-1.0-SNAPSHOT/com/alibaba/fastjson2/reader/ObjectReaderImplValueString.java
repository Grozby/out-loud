package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Type;
import java.util.function.Function;

public class ObjectReaderImplValueString<T> implements ObjectReader<T> {
   final long features;
   final Function<String, T> function;
   final JSONSchema schema;

   public ObjectReaderImplValueString(Class<T> objectClass, long features, JSONSchema schema, Function<String, T> function) {
      this.features = features;
      this.schema = schema;
      this.function = function;
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readObject(jsonReader, fieldType, fieldName, features);
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         String value = jsonReader.readString();
         if (this.schema != null) {
            this.schema.validate(value);
         }

         try {
            return this.function.apply(value);
         } catch (Exception var9) {
            throw new JSONException(jsonReader.info("create object error"), var9);
         }
      }
   }

   public static <T> ObjectReaderImplValueString<T> of(Class<T> objectClass, Function<String, T> function) {
      return new ObjectReaderImplValueString<>(objectClass, 0L, null, function);
   }

   public static <T> ObjectReaderImplValueString<T> of(Class<T> objectClass, long features, JSONSchema schema, Function<String, T> function) {
      return new ObjectReaderImplValueString<>(objectClass, features, schema, function);
   }
}
