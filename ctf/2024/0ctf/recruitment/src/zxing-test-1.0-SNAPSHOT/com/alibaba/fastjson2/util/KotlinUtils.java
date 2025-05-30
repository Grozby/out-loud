package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.codec.BeanInfo;
import java.lang.reflect.Constructor;
import java.util.List;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;

public class KotlinUtils {
   public static final int STATE;

   private KotlinUtils() {
      throw new IllegalStateException();
   }

   public static void getConstructor(Class<?> clazz, BeanInfo beanInfo) {
      int creatorParams = 0;
      Constructor<?> creatorConstructor = null;
      String[] paramNames = beanInfo.createParameterNames;
      Constructor<?>[] constructors = BeanUtils.getConstructor(clazz);

      for (Constructor<?> constructor : constructors) {
         int paramCount = constructor.getParameterCount();
         if (paramNames == null || paramCount == paramNames.length) {
            if (paramCount > 2) {
               Class<?>[] parameterTypes = constructor.getParameterTypes();
               if (parameterTypes[paramCount - 2] == int.class && parameterTypes[paramCount - 1] == DefaultConstructorMarker.class) {
                  beanInfo.markerConstructor = constructor;
                  continue;
               }
            }

            if (creatorConstructor == null || creatorParams < paramCount) {
               creatorParams = paramCount;
               creatorConstructor = constructor;
            }
         }
      }

      if (creatorParams != 0 && STATE == 2) {
         try {
            List<KParameter> params = null;
            KClass<?> kClass = Reflection.getOrCreateKotlinClass(clazz);

            for (KFunction<?> function : kClass.getConstructors()) {
               List<KParameter> parameters = function.getParameters();
               if (params == null || creatorParams == parameters.size()) {
                  params = parameters;
               }
            }

            if (params != null) {
               String[] names = new String[params.size()];
               int i = 0;

               for (int m = names.length; i < m; i++) {
                  names[i] = params.get(i).getName();
               }

               beanInfo.createParameterNames = names;
            }
         } catch (Throwable var12) {
         }
      }

      beanInfo.creatorConstructor = creatorConstructor;
   }

   static {
      int state = 0;

      try {
         Class.forName("kotlin.Metadata");
         state++;
         Class.forName("kotlin.reflect.jvm.ReflectJvmMapping");
         state++;
      } catch (Throwable var2) {
      }

      STATE = state;
   }
}
