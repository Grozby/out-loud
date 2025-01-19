package com.beust.jcommander;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Parameterized {
   private Field field;
   private Method method;
   private Method getter;
   private WrappedParameter wrappedParameter;
   private ParametersDelegate parametersDelegate;

   public Parameterized(WrappedParameter wp, ParametersDelegate pd, Field field, Method method) {
      this.wrappedParameter = wp;
      this.method = method;
      this.field = field;
      if (this.field != null) {
         if (pd == null) {
            setFieldAccessible(this.field);
         } else {
            setFieldAccessibleWithoutFinalCheck(this.field);
         }
      }

      this.parametersDelegate = pd;
   }

   private static void describeClassTree(Class<?> inputClass, Set<Class<?>> setOfClasses) {
      if (inputClass != null) {
         if (!Object.class.equals(inputClass) && !setOfClasses.contains(inputClass)) {
            setOfClasses.add(inputClass);
            describeClassTree(inputClass.getSuperclass(), setOfClasses);

            for (Class<?> hasInterface : inputClass.getInterfaces()) {
               describeClassTree(hasInterface, setOfClasses);
            }
         }
      }
   }

   private static Set<Class<?>> describeClassTree(Class<?> inputClass) {
      if (inputClass == null) {
         return Collections.emptySet();
      } else {
         Set<Class<?>> classes = Sets.newLinkedHashSet();
         describeClassTree(inputClass, classes);
         return classes;
      }
   }

   public static List<Parameterized> parseArg(Object arg) {
      List<Parameterized> result = Lists.newArrayList();
      Class<?> rootClass = arg.getClass();

      for (Class<?> cls : describeClassTree(rootClass)) {
         for (Field f : cls.getDeclaredFields()) {
            Annotation annotation = f.getAnnotation(Parameter.class);
            Annotation delegateAnnotation = f.getAnnotation(ParametersDelegate.class);
            Annotation dynamicParameter = f.getAnnotation(DynamicParameter.class);
            if (annotation != null) {
               result.add(new Parameterized(new WrappedParameter((Parameter)annotation), null, f, null));
            } else if (dynamicParameter != null) {
               result.add(new Parameterized(new WrappedParameter((DynamicParameter)dynamicParameter), null, f, null));
            } else if (delegateAnnotation != null) {
               result.add(new Parameterized(null, (ParametersDelegate)delegateAnnotation, f, null));
            }
         }

         for (Method m : cls.getDeclaredMethods()) {
            m.setAccessible(true);
            Annotation annotation = m.getAnnotation(Parameter.class);
            Annotation delegateAnnotation = m.getAnnotation(ParametersDelegate.class);
            Annotation dynamicParameter = m.getAnnotation(DynamicParameter.class);
            if (annotation != null) {
               result.add(new Parameterized(new WrappedParameter((Parameter)annotation), null, null, m));
            } else if (dynamicParameter != null) {
               result.add(new Parameterized(new WrappedParameter((DynamicParameter)dynamicParameter), null, null, m));
            } else if (delegateAnnotation != null) {
               result.add(new Parameterized(null, (ParametersDelegate)delegateAnnotation, null, m));
            }
         }
      }

      return result;
   }

   public WrappedParameter getWrappedParameter() {
      return this.wrappedParameter;
   }

   public Class<?> getType() {
      return this.method != null ? this.method.getParameterTypes()[0] : this.field.getType();
   }

   public String getName() {
      return this.method != null ? this.method.getName() : this.field.getName();
   }

   public Object get(Object object) {
      try {
         if (this.method != null) {
            if (this.getter == null) {
               this.setGetter(object);
            }

            return this.getter.invoke(object);
         } else {
            return this.field.get(object);
         }
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | SecurityException var8) {
         throw new ParameterException(var8);
      } catch (NoSuchMethodException var9) {
         String name = this.method.getName();
         String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
         Object result = null;

         try {
            Field field = this.method.getDeclaringClass().getDeclaredField(fieldName);
            if (field != null) {
               setFieldAccessible(field);
               result = field.get(object);
            }
         } catch (IllegalAccessException | NoSuchFieldException var7) {
         }

         return result;
      }
   }

   private void setGetter(Object object) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
      if (Boolean.class.getSimpleName().toLowerCase().equals(this.getType().getName())) {
         try {
            this.getter = object.getClass().getMethod("is" + this.method.getName().substring(3));
            return;
         } catch (NoSuchMethodException var3) {
         }
      }

      this.getter = object.getClass().getMethod("g" + this.method.getName().substring(1));
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.field == null ? 0 : this.field.hashCode());
      return 31 * result + (this.method == null ? 0 : this.method.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         Parameterized other = (Parameterized)obj;
         if (this.field == null) {
            if (other.field != null) {
               return false;
            }
         } else if (!this.field.equals(other.field)) {
            return false;
         }

         if (this.method == null) {
            if (other.method != null) {
               return false;
            }
         } else if (!this.method.equals(other.method)) {
            return false;
         }

         return true;
      }
   }

   public boolean isDynamicParameter(Field field) {
      return this.method != null ? this.method.getAnnotation(DynamicParameter.class) != null : this.field.getAnnotation(DynamicParameter.class) != null;
   }

   private static void setFieldAccessible(Field f) {
      if (Modifier.isFinal(f.getModifiers())) {
         throw new ParameterException(
            "Cannot use final field "
               + f.getDeclaringClass().getName()
               + "#"
               + f.getName()
               + " as a parameter; compile-time constant inlining may hide new values written to it."
         );
      } else {
         f.setAccessible(true);
      }
   }

   private static void setFieldAccessibleWithoutFinalCheck(Field f) {
      f.setAccessible(true);
   }

   private static String errorMessage(Method m, Exception ex) {
      return "Could not invoke " + m + "\n    Reason: " + ex.getMessage();
   }

   public void set(Object object, Object value) {
      try {
         if (this.method != null) {
            this.method.invoke(object, value);
         } else {
            this.field.set(object, value);
         }
      } catch (IllegalArgumentException | IllegalAccessException var4) {
         throw new ParameterException(errorMessage(this.method, var4));
      } catch (InvocationTargetException var5) {
         if (var5.getTargetException() instanceof ParameterException) {
            throw (ParameterException)var5.getTargetException();
         } else {
            throw new ParameterException(errorMessage(this.method, var5));
         }
      }
   }

   public ParametersDelegate getDelegateAnnotation() {
      return this.parametersDelegate;
   }

   public Type getGenericType() {
      return this.method != null ? this.method.getGenericParameterTypes()[0] : this.field.getGenericType();
   }

   public Parameter getParameter() {
      return this.wrappedParameter.getParameter();
   }

   public Type findFieldGenericType() {
      if (this.method != null) {
         return null;
      } else {
         if (this.field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType)this.field.getGenericType();
            Type cls = p.getActualTypeArguments()[0];
            if (cls instanceof Class) {
               return cls;
            }

            if (cls instanceof WildcardType) {
               WildcardType wildcardType = (WildcardType)cls;
               if (wildcardType.getLowerBounds().length > 0) {
                  return wildcardType.getLowerBounds()[0];
               }

               if (wildcardType.getUpperBounds().length > 0) {
                  return wildcardType.getUpperBounds()[0];
               }
            }
         }

         return null;
      }
   }

   public boolean isDynamicParameter() {
      return this.wrappedParameter.getDynamicParameter() != null;
   }
}
