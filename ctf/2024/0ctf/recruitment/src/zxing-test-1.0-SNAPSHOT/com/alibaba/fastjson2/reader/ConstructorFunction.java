package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

final class ConstructorFunction<T> implements Function<Map<Long, Object>, T> {
   final Constructor constructor;
   final Function function;
   final BiFunction biFunction;
   final Parameter[] parameters;
   final String[] paramNames;
   final boolean marker;
   final long[] hashCodes;
   final List<Constructor> alternateConstructors;
   Map<Set<Long>, Constructor> alternateConstructorMap;
   Map<Set<Long>, String[]> alternateConstructorNames;
   Map<Set<Long>, long[]> alternateConstructorNameHashCodes;
   Map<Set<Long>, Type[]> alternateConstructorArgTypes;

   ConstructorFunction(
      List<Constructor> alternateConstructors,
      Constructor constructor,
      Function function,
      BiFunction biFunction,
      Constructor markerConstructor,
      String... paramNames
   ) {
      this.function = function;
      this.biFunction = biFunction;
      this.marker = markerConstructor != null;
      this.constructor = this.marker ? markerConstructor : constructor;
      this.parameters = constructor.getParameters();
      this.paramNames = paramNames;
      this.hashCodes = new long[this.parameters.length];

      for (int i = 0; i < this.parameters.length; i++) {
         String name;
         if (i < paramNames.length) {
            name = paramNames[i];
         } else {
            name = this.parameters[i].getName();
         }

         if (name == null) {
            name = "arg" + i;
         }

         this.hashCodes[i] = Fnv.hashCode64(name);
      }

      this.alternateConstructors = alternateConstructors;
      if (alternateConstructors != null) {
         int size = alternateConstructors.size();
         this.alternateConstructorMap = new HashMap<>(size, 1.0F);
         this.alternateConstructorNames = new HashMap<>(size, 1.0F);
         this.alternateConstructorArgTypes = new HashMap<>(size, 1.0F);
         this.alternateConstructorNameHashCodes = new HashMap<>(size, 1.0F);

         for (int i = 0; i < size; i++) {
            Constructor alternateConstructor = alternateConstructors.get(i);
            alternateConstructor.setAccessible(true);
            String[] parameterNames = ASMUtils.lookupParameterNames(alternateConstructor);
            Parameter[] parameters = alternateConstructor.getParameters();
            FieldInfo fieldInfo = new FieldInfo();
            ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();

            for (int j = 0; j < parameters.length && j < parameterNames.length; j++) {
               fieldInfo.init();
               Parameter parameter = parameters[j];
               provider.getFieldInfo(fieldInfo, alternateConstructor.getDeclaringClass(), alternateConstructor, j, parameter);
               if (fieldInfo.fieldName != null) {
                  parameterNames[j] = fieldInfo.fieldName;
               }
            }

            long[] parameterNameHashCodes = new long[parameterNames.length];
            Type[] parameterTypes = alternateConstructor.getGenericParameterTypes();
            Set<Long> paramHashCodes = new HashSet<>(parameterNames.length);

            for (int jx = 0; jx < parameterNames.length; jx++) {
               long hashCode64 = Fnv.hashCode64(parameterNames[jx]);
               parameterNameHashCodes[jx] = hashCode64;
               paramHashCodes.add(hashCode64);
            }

            this.alternateConstructorMap.put(paramHashCodes, alternateConstructor);
            this.alternateConstructorNames.put(paramHashCodes, parameterNames);
            this.alternateConstructorNameHashCodes.put(paramHashCodes, parameterNameHashCodes);
            this.alternateConstructorArgTypes.put(paramHashCodes, parameterTypes);
         }
      }
   }

   public T apply(Map<Long, Object> values) {
      boolean containsAll = true;

      for (long hashCode : this.hashCodes) {
         if (!values.containsKey(hashCode)) {
            containsAll = false;
            break;
         }
      }

      if (!containsAll && this.alternateConstructorMap != null) {
         Set<Long> key = values.keySet();
         Constructor constructor = this.alternateConstructorMap.get(key);
         if (constructor != null) {
            long[] hashCodes = this.alternateConstructorNameHashCodes.get(key);
            Type[] paramTypes = this.alternateConstructorArgTypes.get(key);
            Object[] args = new Object[hashCodes.length];

            for (int i = 0; i < hashCodes.length; i++) {
               Object arg = values.get(hashCodes[i]);
               Type paramType = paramTypes[i];
               if (arg == null) {
                  arg = TypeUtils.getDefaultValue(paramType);
               }

               args[i] = arg;
            }

            try {
               return (T)constructor.newInstance(args);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException var11) {
               throw new JSONException("invoke constructor error, " + constructor, var11);
            }
         }
      }

      if (this.function != null && this.parameters.length == 1) {
         Parameter param = this.parameters[0];
         Object arg = values.get(this.hashCodes[0]);
         Class<?> paramType = param.getType();
         if (arg == null) {
            arg = TypeUtils.getDefaultValue(paramType);
         } else if (!paramType.isInstance(arg)) {
            arg = TypeUtils.cast(arg, paramType);
         }

         return (T)this.function.apply(arg);
      } else if (this.biFunction != null && this.parameters.length == 2) {
         Object arg0 = values.get(this.hashCodes[0]);
         Parameter param0 = this.parameters[0];
         Class<?> param0Type = param0.getType();
         if (arg0 == null) {
            arg0 = TypeUtils.getDefaultValue(param0Type);
         } else if (!param0Type.isInstance(arg0)) {
            arg0 = TypeUtils.cast(arg0, param0Type);
         }

         Object arg1 = values.get(this.hashCodes[1]);
         Parameter param1 = this.parameters[1];
         Class<?> param1Type = param1.getType();
         if (arg1 == null) {
            arg1 = TypeUtils.getDefaultValue(param1Type);
         } else if (!param1Type.isInstance(arg1)) {
            arg1 = TypeUtils.cast(arg1, param1Type);
         }

         return (T)this.biFunction.apply(arg0, arg1);
      } else {
         int size = this.parameters.length;
         Object[] args = new Object[this.constructor.getParameterCount()];
         if (this.marker) {
            int i = 0;
            int flag = 0;

            while (i < size) {
               Object arg = values.get(this.hashCodes[i]);
               if (arg != null) {
                  args[i] = arg;
               } else {
                  flag |= 1 << i;
                  Class<?> paramType = this.parameters[i].getType();
                  if (paramType.isPrimitive()) {
                     args[i] = TypeUtils.getDefaultValue(paramType);
                  }
               }

               int n = i + 1;
               if (n % 32 == 0 || n == size) {
                  args[size + i / 32] = flag;
                  flag = 0;
               }

               i = n;
            }
         } else {
            for (int i = 0; i < size; i++) {
               Parameter parameter = this.parameters[i];
               Class<?> paramClass = parameter.getType();
               Type paramType = parameter.getParameterizedType();
               Object argx = values.get(this.hashCodes[i]);
               if (argx == null) {
                  argx = TypeUtils.getDefaultValue(paramClass);
               } else if (!paramClass.isInstance(argx)) {
                  argx = TypeUtils.cast(argx, paramClass);
               } else if (paramType instanceof ParameterizedType) {
                  argx = TypeUtils.cast(argx, paramType);
               }

               args[i] = argx;
            }
         }

         try {
            return (T)this.constructor.newInstance(args);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException var12) {
            throw new JSONException("invoke constructor error, " + this.constructor, var12);
         }
      }
   }
}
