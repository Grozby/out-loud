package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.MultiType;
import com.alibaba.fastjson2.util.ParameterizedTypeImpl;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class TypeReference<T> {
   protected final Type type;
   protected final Class<? super T> rawType;

   public TypeReference() {
      Type superClass = this.getClass().getGenericSuperclass();
      this.type = ((ParameterizedType)superClass).getActualTypeArguments()[0];
      this.rawType = (Class<? super T>)BeanUtils.getRawType(this.type);
   }

   private TypeReference(Type type) {
      if (type == null) {
         throw new NullPointerException();
      } else {
         this.type = BeanUtils.canonicalize(type);
         this.rawType = (Class<? super T>)BeanUtils.getRawType(type);
      }
   }

   public TypeReference(Type... actualTypeArguments) {
      if (actualTypeArguments != null && actualTypeArguments.length != 0) {
         if (actualTypeArguments.length == 1 && actualTypeArguments[0] == null) {
            actualTypeArguments = new Type[]{Object.class};
         }

         Class<?> thisClass = this.getClass();
         Type superClass = thisClass.getGenericSuperclass();
         ParameterizedType argType = (ParameterizedType)((ParameterizedType)superClass).getActualTypeArguments()[0];
         this.type = canonicalize(thisClass, argType, actualTypeArguments, 0);
         this.rawType = (Class<? super T>)BeanUtils.getRawType(this.type);
      } else {
         throw new NullPointerException();
      }
   }

   public final Type getType() {
      return this.type;
   }

   public final Class<? super T> getRawType() {
      return this.rawType;
   }

   public T parseObject(String text) {
      return JSON.parseObject(text, this.type);
   }

   public T parseObject(byte[] utf8Bytes) {
      return JSON.parseObject(utf8Bytes, this.type);
   }

   public List<T> parseArray(String text, JSONReader.Feature... features) {
      return JSON.parseArray(text, this.type, features);
   }

   public List<T> parseArray(byte[] utf8Bytes, JSONReader.Feature... features) {
      return JSON.parseArray(utf8Bytes, this.type, features);
   }

   public T to(JSONArray array) {
      return array.to(this.type);
   }

   public T to(JSONObject object, JSONReader.Feature... features) {
      return object.to(this.type, features);
   }

   @Deprecated
   public T toJavaObject(JSONArray array) {
      return array.to(this.type);
   }

   @Deprecated
   public T toJavaObject(JSONObject object, JSONReader.Feature... features) {
      return object.to(this.type, features);
   }

   public static TypeReference<?> get(Type type) {
      return new TypeReference<Object>(type) {
      };
   }

   private static Type canonicalize(Class<?> thisClass, ParameterizedType type, Type[] actualTypeArguments, int actualIndex) {
      Type rawType = type.getRawType();
      Type[] argTypes = type.getActualTypeArguments();

      for (int i = 0; i < argTypes.length; i++) {
         if (argTypes[i] instanceof TypeVariable && actualIndex < actualTypeArguments.length) {
            argTypes[i] = actualTypeArguments[actualIndex++];
         }

         if (argTypes[i] instanceof GenericArrayType) {
            Type componentType = argTypes[i];

            int dimension;
            for (dimension = 0; componentType instanceof GenericArrayType; componentType = ((GenericArrayType)componentType).getGenericComponentType()) {
               dimension++;
            }

            if (componentType instanceof Class) {
               Class<?> cls = (Class<?>)componentType;
               label56:
               if (cls.isPrimitive()) {
                  char ch;
                  if (cls == int.class) {
                     ch = 'I';
                  } else if (cls == long.class) {
                     ch = 'J';
                  } else if (cls == float.class) {
                     ch = 'F';
                  } else if (cls == double.class) {
                     ch = 'D';
                  } else if (cls == boolean.class) {
                     ch = 'Z';
                  } else if (cls == char.class) {
                     ch = 'C';
                  } else if (cls == byte.class) {
                     ch = 'B';
                  } else {
                     if (cls != short.class) {
                        break label56;
                     }

                     ch = 'S';
                  }

                  char[] chars = new char[dimension + 1];

                  for (int j = 0; j < dimension; j++) {
                     chars[j] = '[';
                  }

                  chars[dimension] = ch;
                  String typeName = new String(chars);
                  argTypes[i] = TypeUtils.loadClass(typeName);
               }
            }
         }

         if (argTypes[i] instanceof ParameterizedType) {
            argTypes[i] = canonicalize(thisClass, (ParameterizedType)argTypes[i], actualTypeArguments, actualIndex);
         }
      }

      return new ParameterizedTypeImpl(argTypes, thisClass, rawType);
   }

   public static Type of(Type... types) {
      return new MultiType(types);
   }

   public static Type collectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
      return new ParameterizedTypeImpl(collectionClass, elementClass);
   }

   public static Type arrayType(Class<?> elementType) {
      return new BeanUtils.GenericArrayTypeImpl(elementType);
   }

   public static Type mapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
      return new ParameterizedTypeImpl(mapClass, keyClass, valueClass);
   }

   public static Type mapType(Class<?> keyClass, Type valueType) {
      return new ParameterizedTypeImpl(Map.class, keyClass, valueType);
   }

   public static Type parametricType(Class<?> parametrized, Class<?>... parameterClasses) {
      return new ParameterizedTypeImpl(parametrized, parameterClasses);
   }

   public static Type parametricType(Class<?> parametrized, Type... parameterTypes) {
      return new ParameterizedTypeImpl(parametrized, parameterTypes);
   }
}
