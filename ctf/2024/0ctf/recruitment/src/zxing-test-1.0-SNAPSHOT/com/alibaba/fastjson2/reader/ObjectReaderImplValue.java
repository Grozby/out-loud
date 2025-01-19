package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

public class ObjectReaderImplValue<I, T> implements ObjectReader<T> {
   final Type valueType;
   final Class<I> valueClass;
   final long features;
   final Constructor<T> constructor;
   final Method factoryMethod;
   final Function<I, T> function;
   final JSONSchema schema;
   final Object emptyVariantArgs;
   ObjectReader valueReader;

   public ObjectReaderImplValue(
      Class<T> objectClass,
      Type valueType,
      Class<I> valueClass,
      long features,
      String format,
      Object defaultValue,
      JSONSchema schema,
      Constructor<T> constructor,
      Method factoryMethod,
      Function<I, T> function
   ) {
      this.valueType = valueType;
      this.valueClass = valueClass;
      this.features = features;
      this.schema = schema;
      this.constructor = constructor;
      this.factoryMethod = factoryMethod;
      this.function = function;
      if (factoryMethod != null && factoryMethod.getParameterCount() == 2) {
         Class<?> varArgType = factoryMethod.getParameterTypes()[1].getComponentType();
         this.emptyVariantArgs = Array.newInstance(varArgType, 0);
      } else {
         this.emptyVariantArgs = null;
      }
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readObject(jsonReader, fieldType, fieldName, features);
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (this.valueReader == null) {
         this.valueReader = jsonReader.getObjectReader(this.valueType);
      }

      I value = (I)this.valueReader.readObject(jsonReader, fieldType, fieldName, features | this.features);
      if (value == null) {
         return null;
      } else {
         if (this.schema != null) {
            this.schema.validate(value);
         }

         T object;
         if (this.function != null) {
            try {
               object = this.function.apply(value);
            } catch (Exception var11) {
               throw new JSONException(jsonReader.info("create object error"), var11);
            }
         } else if (this.constructor != null) {
            try {
               object = this.constructor.newInstance(value);
            } catch (Exception var10) {
               throw new JSONException(jsonReader.info("create object error"), var10);
            }
         } else {
            if (this.factoryMethod == null) {
               throw new JSONException(jsonReader.info("create object error"));
            }

            try {
               if (this.emptyVariantArgs != null) {
                  object = (T)this.factoryMethod.invoke(null, value, this.emptyVariantArgs);
               } else {
                  object = (T)this.factoryMethod.invoke(null, value);
               }
            } catch (Exception var9) {
               throw new JSONException(jsonReader.info("create object error"), var9);
            }
         }

         return object;
      }
   }

   public static <I, T> ObjectReaderImplValue<I, T> of(Class<T> objectClass, Class<I> valueClass, Method method) {
      return new ObjectReaderImplValue<>(objectClass, valueClass, valueClass, 0L, null, null, null, null, method, null);
   }

   public static <I, T> ObjectReaderImplValue<I, T> of(Class<T> objectClass, Class<I> valueClass, Function<I, T> function) {
      return new ObjectReaderImplValue<>(objectClass, valueClass, valueClass, 0L, null, null, null, null, null, function);
   }
}
