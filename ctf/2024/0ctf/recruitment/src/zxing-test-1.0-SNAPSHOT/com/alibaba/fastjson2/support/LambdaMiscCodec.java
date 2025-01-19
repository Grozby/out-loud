package com.alibaba.fastjson2.support;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaders;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriters;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class LambdaMiscCodec {
   static volatile boolean hppcError;
   static volatile Throwable errorLast;

   public static ObjectWriter getObjectWriter(Type objectType, Class objectClass) {
      if (hppcError) {
         return null;
      } else {
         String className = objectClass.getName();
         switch (className) {
            case "gnu.trove.set.hash.TByteHashSet":
            case "gnu.trove.stack.array.TByteArrayStack":
            case "gnu.trove.list.array.TByteArrayList":
            case "com.carrotsearch.hppc.ByteArrayList":
               try {
                  return ObjectWriters.ofToByteArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var17) {
                  throw new JSONException("illegal state", var17);
               }
            case "gnu.trove.set.hash.TShortHashSet":
            case "gnu.trove.list.array.TShortArrayList":
            case "com.carrotsearch.hppc.ShortArrayList":
               try {
                  return ObjectWriters.ofToShortArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var16) {
                  throw new JSONException("illegal state", var16);
               }
            case "gnu.trove.list.array.TIntArrayList":
            case "gnu.trove.set.hash.TIntHashSet":
            case "com.carrotsearch.hppc.IntArrayList":
            case "com.carrotsearch.hppc.IntHashSet":
               try {
                  return ObjectWriters.ofToIntArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var15) {
                  throw new JSONException("illegal state", var15);
               }
            case "gnu.trove.list.array.TLongArrayList":
            case "gnu.trove.set.hash.TLongHashSet":
            case "com.carrotsearch.hppc.LongArrayList":
            case "com.carrotsearch.hppc.LongHashSet":
               try {
                  return ObjectWriters.ofToLongArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var14) {
                  throw new JSONException("illegal state", var14);
               }
            case "gnu.trove.list.array.TCharArrayList":
            case "com.carrotsearch.hppc.CharArrayList":
            case "com.carrotsearch.hppc.CharHashSet":
               try {
                  return ObjectWriters.ofToCharArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var13) {
                  throw new JSONException("illegal state", var13);
               }
            case "gnu.trove.list.array.TFloatArrayList":
            case "com.carrotsearch.hppc.FloatArrayList":
               try {
                  return ObjectWriters.ofToFloatArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var12) {
                  throw new JSONException("illegal state", var12);
               }
            case "gnu.trove.list.array.TDoubleArrayList":
            case "com.carrotsearch.hppc.DoubleArrayList":
               try {
                  return ObjectWriters.ofToDoubleArray(createFunction(objectClass.getMethod("toArray")));
               } catch (SecurityException | NoSuchMethodException var11) {
                  throw new JSONException("illegal state", var11);
               }
            case "com.carrotsearch.hppc.BitSet":
               Lookup lookup = JDKUtils.trustedLookup(objectClass);

               try {
                  ToLongFunction functionSize = createToLongFunction(objectClass.getMethod("size"));
                  MethodHandle getHandler = lookup.findVirtual(objectClass, "get", MethodType.methodType(boolean.class, int.class));
                  CallSite getCallSite = LambdaMetafactory.metafactory(
                     lookup,
                     "apply",
                     MethodType.methodType(BiFunction.class),
                     MethodType.methodType(Object.class, Object.class, Object.class),
                     getHandler,
                     MethodType.methodType(Boolean.class, objectClass, Integer.class)
                  );
                  BiFunction<Object, Integer, Boolean> functionGet = (BiFunction)getCallSite.getTarget().invokeExact();
                  return ObjectWriters.ofToBooleanArray(functionSize, functionGet);
               } catch (Throwable var18) {
                  hppcError = true;
               }
            default:
               return null;
            case "org.bson.types.Decimal128":
               try {
                  return ObjectWriters.ofToBigDecimal(createFunction(objectClass.getMethod("bigDecimalValue")));
               } catch (SecurityException | NoSuchMethodException var10) {
                  throw new JSONException("illegal state", var10);
               }
         }
      }
   }

   public static ObjectReader getObjectReader(Class objectClass) {
      if (hppcError) {
         return null;
      } else {
         String className = objectClass.getName();
         switch (className) {
            case "com.carrotsearch.hppc.ByteArrayList":
               try {
                  return ObjectReaders.fromByteArray(createFunction(objectClass.getMethod("from", byte[].class)));
               } catch (SecurityException | NoSuchMethodException var19) {
                  throw new JSONException("illegal state", var19);
               }
            case "com.carrotsearch.hppc.ShortArrayList":
               try {
                  return ObjectReaders.fromShortArray(createFunction(objectClass.getMethod("from", short[].class)));
               } catch (SecurityException | NoSuchMethodException var18) {
                  throw new JSONException("illegal state", var18);
               }
            case "com.carrotsearch.hppc.IntArrayList":
            case "com.carrotsearch.hppc.IntHashSet":
               try {
                  return ObjectReaders.fromIntArray(createFunction(objectClass.getMethod("from", int[].class)));
               } catch (SecurityException | NoSuchMethodException var17) {
                  throw new JSONException("illegal state", var17);
               }
            case "com.carrotsearch.hppc.LongArrayList":
            case "com.carrotsearch.hppc.LongHashSet":
               try {
                  return ObjectReaders.fromLongArray(createFunction(objectClass.getMethod("from", long[].class)));
               } catch (SecurityException | NoSuchMethodException var16) {
                  throw new JSONException("illegal state", var16);
               }
            case "com.carrotsearch.hppc.CharArrayList":
            case "com.carrotsearch.hppc.CharHashSet":
               try {
                  return ObjectReaders.fromCharArray(createFunction(objectClass.getMethod("from", char[].class)));
               } catch (SecurityException | NoSuchMethodException var15) {
                  throw new JSONException("illegal state", var15);
               }
            case "com.carrotsearch.hppc.FloatArrayList":
               try {
                  return ObjectReaders.fromFloatArray(createFunction(objectClass.getMethod("from", float[].class)));
               } catch (SecurityException | NoSuchMethodException var14) {
                  throw new JSONException("illegal state", var14);
               }
            case "com.carrotsearch.hppc.DoubleArrayList":
               try {
                  return ObjectReaders.fromDoubleArray(createFunction(objectClass.getMethod("from", double[].class)));
               } catch (SecurityException | NoSuchMethodException var13) {
                  throw new JSONException("illegal state", var13);
               }
            case "gnu.trove.set.hash.TByteHashSet":
            case "gnu.trove.stack.array.TByteArrayStack":
            case "gnu.trove.list.array.TByteArrayList":
               try {
                  return ObjectReaders.fromByteArray(createFunction(objectClass.getConstructor(byte[].class)));
               } catch (SecurityException | NoSuchMethodException var12) {
                  throw new JSONException("illegal state", var12);
               }
            case "gnu.trove.list.array.TCharArrayList":
               try {
                  return ObjectReaders.fromCharArray(createFunction(objectClass.getConstructor(char[].class)));
               } catch (SecurityException | NoSuchMethodException var11) {
                  throw new JSONException("illegal state", var11);
               }
            case "gnu.trove.set.hash.TShortHashSet":
            case "gnu.trove.list.array.TShortArrayList":
               try {
                  return ObjectReaders.fromShortArray(createFunction(objectClass.getConstructor(short[].class)));
               } catch (SecurityException | NoSuchMethodException var10) {
                  throw new JSONException("illegal state", var10);
               }
            case "gnu.trove.set.hash.TIntHashSet":
            case "gnu.trove.list.array.TIntArrayList":
               try {
                  return ObjectReaders.fromIntArray(createFunction(objectClass.getConstructor(int[].class)));
               } catch (SecurityException | NoSuchMethodException var9) {
                  throw new JSONException("illegal state", var9);
               }
            case "gnu.trove.set.hash.TLongHashSet":
            case "gnu.trove.list.array.TLongArrayList":
               try {
                  return ObjectReaders.fromLongArray(createFunction(objectClass.getConstructor(long[].class)));
               } catch (SecurityException | NoSuchMethodException var8) {
                  throw new JSONException("illegal state", var8);
               }
            case "gnu.trove.list.array.TFloatArrayList":
               try {
                  return ObjectReaders.fromFloatArray(createFunction(objectClass.getConstructor(float[].class)));
               } catch (SecurityException | NoSuchMethodException var7) {
                  throw new JSONException("illegal state", var7);
               }
            case "gnu.trove.list.array.TDoubleArrayList":
               try {
                  return ObjectReaders.fromDoubleArray(createFunction(objectClass.getConstructor(double[].class)));
               } catch (SecurityException | NoSuchMethodException var6) {
                  throw new JSONException("illegal state", var6);
               }
            case "org.bson.types.Decimal128":
               try {
                  return ObjectReaders.fromBigDecimal(createFunction(objectClass.getConstructor(BigDecimal.class)));
               } catch (SecurityException | NoSuchMethodException var5) {
                  throw new JSONException("illegal state", var5);
               }
            default:
               return null;
         }
      }
   }

   public static LongFunction createLongFunction(Constructor constructor) {
      try {
         Class objectClass = constructor.getDeclaringClass();
         Lookup lookup = JDKUtils.trustedLookup(objectClass);
         MethodHandle methodHandle = lookup.findConstructor(objectClass, TypeUtils.METHOD_TYPE_VOID_LONG);
         MethodType invokedType = MethodType.methodType(objectClass, long.class);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_LONG_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_LONG, methodHandle, invokedType
         );
         return (LongFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var6) {
         errorLast = var6;
         return new LambdaMiscCodec.ReflectLongFunction(constructor);
      }
   }

   public static ToIntFunction createToIntFunction(Method method) {
      Class<?> objectClass = method.getDeclaringClass();

      try {
         Lookup lookup = JDKUtils.trustedLookup(objectClass);
         MethodType methodType = MethodType.methodType(int.class);
         MethodHandle methodHandle = lookup.findVirtual(objectClass, method.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "applyAsInt",
            TypeUtils.METHOD_TYPE_TO_INT_FUNCTION,
            TypeUtils.METHOD_TYPE_INT_OBJECT,
            methodHandle,
            MethodType.methodType(int.class, objectClass)
         );
         return (ToIntFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var6) {
         errorLast = var6;
         return new LambdaMiscCodec.ReflectToIntFunction(method);
      }
   }

   public static ToLongFunction createToLongFunction(Method method) {
      Class<?> objectClass = method.getDeclaringClass();

      try {
         Lookup lookup = JDKUtils.trustedLookup(objectClass);
         MethodType methodType = MethodType.methodType(long.class);
         MethodHandle methodHandle = lookup.findVirtual(objectClass, method.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "applyAsLong",
            TypeUtils.METHOD_TYPE_TO_LONG_FUNCTION,
            TypeUtils.METHOD_TYPE_LONG_OBJECT,
            methodHandle,
            MethodType.methodType(long.class, objectClass)
         );
         return (ToLongFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var6) {
         errorLast = var6;
         return new LambdaMiscCodec.ReflectToLongFunction(method);
      }
   }

   public static Function createFunction(Constructor constructor) {
      try {
         Class<?> declaringClass = constructor.getDeclaringClass();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         Class<?>[] parameterTypes = constructor.getParameterTypes();
         Class<?> param0 = parameterTypes[0];
         MethodHandle methodHandle = lookup.findConstructor(declaringClass, MethodType.methodType(void.class, param0));
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, methodHandle, MethodType.methodType(declaringClass, param0)
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var7) {
         errorLast = var7;
         return new LambdaMiscCodec.ConstructorFunction(constructor);
      }
   }

   public static Supplier createSupplier(Constructor constructor) {
      try {
         Class<?> declaringClass = constructor.getDeclaringClass();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         MethodHandle methodHandle = lookup.findConstructor(declaringClass, MethodType.methodType(void.class));
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "get", TypeUtils.METHOD_TYPE_SUPPLIER, TypeUtils.METHOD_TYPE_OBJECT, methodHandle, MethodType.methodType(declaringClass)
         );
         return (Supplier)callSite.getTarget().invokeExact();
      } catch (Throwable var5) {
         errorLast = var5;
         return new LambdaMiscCodec.ConstructorSupplier(constructor);
      }
   }

   public static Supplier createSupplier(Method method) {
      try {
         Class<?> declaringClass = method.getDeclaringClass();
         Class objectClass = method.getReturnType();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         MethodHandle methodHandle = lookup.findStatic(declaringClass, method.getName(), MethodType.methodType(objectClass));
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "get", TypeUtils.METHOD_TYPE_SUPPLIER, TypeUtils.METHOD_TYPE_OBJECT, methodHandle, MethodType.methodType(objectClass)
         );
         return (Supplier)callSite.getTarget().invokeExact();
      } catch (Throwable var6) {
         errorLast = var6;
         return new LambdaMiscCodec.ReflectSupplier(method);
      }
   }

   public static BiFunction createBiFunction(Method method) {
      try {
         Class<?> declaringClass = method.getDeclaringClass();
         Class objectClass = method.getReturnType();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         Class<?>[] parameterTypes = method.getParameterTypes();
         Class<?> param0 = parameterTypes[0];
         MethodType methodType;
         MethodHandle methodHandle;
         if (Modifier.isStatic(method.getModifiers())) {
            Class<?> param1 = parameterTypes[1];
            methodHandle = lookup.findStatic(declaringClass, method.getName(), MethodType.methodType(objectClass, param0, param1));
            methodType = MethodType.methodType(objectClass, param0, param1);
         } else {
            methodHandle = lookup.findVirtual(declaringClass, method.getName(), MethodType.methodType(objectClass, param0));
            methodType = MethodType.methodType(objectClass, declaringClass, param0);
         }

         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_BI_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT_OBJECT, methodHandle, methodType
         );
         return (BiFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var9) {
         errorLast = var9;
         return new LambdaMiscCodec.ReflectBiFunction(method);
      }
   }

   public static BiFunction createBiFunction(Constructor constructor) {
      try {
         Class<?> declaringClass = constructor.getDeclaringClass();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         Class<?>[] parameterTypes = constructor.getParameterTypes();
         Class<?> param0 = parameterTypes[0];
         Class<?> param1 = parameterTypes[1];
         MethodHandle methodHandle = lookup.findConstructor(declaringClass, MethodType.methodType(void.class, param0, param1));
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "apply",
            TypeUtils.METHOD_TYPE_BI_FUNCTION,
            TypeUtils.METHOD_TYPE_OBJECT_OBJECT_OBJECT,
            methodHandle,
            MethodType.methodType(declaringClass, param0, param1)
         );
         return (BiFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var8) {
         errorLast = var8;
         return new LambdaMiscCodec.ConstructorBiFunction(constructor);
      }
   }

   public static Function createFunction(Method method) {
      Class<?> declaringClass = method.getDeclaringClass();
      int modifiers = method.getModifiers();
      Class<?>[] parameterTypes = method.getParameterTypes();
      boolean isStatic = Modifier.isStatic(modifiers);
      Class objectClass = method.getReturnType();
      Class paramClass;
      if (parameterTypes.length == 1 && isStatic) {
         paramClass = parameterTypes[0];
      } else {
         if (parameterTypes.length != 0 || isStatic) {
            throw new JSONException("not support parameters " + method);
         }

         paramClass = declaringClass;
      }

      try {
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         MethodHandle methodHandle;
         if (isStatic) {
            methodHandle = lookup.findStatic(declaringClass, method.getName(), MethodType.methodType(objectClass, paramClass));
         } else {
            methodHandle = lookup.findVirtual(declaringClass, method.getName(), MethodType.methodType(objectClass));
         }

         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, methodHandle, MethodType.methodType(objectClass, paramClass)
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var10) {
         errorLast = var10;
         return (Function)(!Modifier.isStatic(method.getModifiers()) ? new LambdaMiscCodec.GetterFunction(method) : new LambdaMiscCodec.FactoryFunction(method));
      }
   }

   public static ObjIntConsumer createObjIntConsumer(Method method) {
      Class<?> declaringClass = method.getDeclaringClass();

      try {
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);
         MethodType methodType = MethodType.methodType(void.class, int.class);
         MethodHandle methodHandle = lookup.findVirtual(declaringClass, method.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "accept",
            TypeUtils.METHOD_TYPE_OBJECT_INT_CONSUMER,
            TypeUtils.METHOD_TYPE_VOID_OBJECT_INT,
            methodHandle,
            MethodType.methodType(void.class, declaringClass, int.class)
         );
         return (ObjIntConsumer)callSite.getTarget().invokeExact();
      } catch (Throwable var6) {
         errorLast = var6;
         return new LambdaMiscCodec.ReflectObjIntConsumer(method);
      }
   }

   static final class ConstructorBiFunction implements BiFunction {
      final Constructor constructor;

      ConstructorBiFunction(Constructor constructor) {
         this.constructor = constructor;
      }

      @Override
      public Object apply(Object arg0, Object arg1) {
         try {
            return this.constructor.newInstance(arg0, arg1);
         } catch (InvocationTargetException | InstantiationException | IllegalAccessException var4) {
            throw new JSONException("invoke error", var4);
         }
      }
   }

   static final class ConstructorFunction implements Function {
      final Constructor constructor;

      ConstructorFunction(Constructor constructor) {
         this.constructor = constructor;
      }

      @Override
      public Object apply(Object arg0) {
         try {
            return this.constructor.newInstance(arg0);
         } catch (InvocationTargetException | InstantiationException | IllegalAccessException var3) {
            throw new JSONException("invoke error", var3);
         }
      }
   }

   static final class ConstructorSupplier implements Supplier {
      final Constructor constructor;

      ConstructorSupplier(Constructor constructor) {
         this.constructor = constructor;
      }

      @Override
      public Object get() {
         try {
            return this.constructor.newInstance();
         } catch (InvocationTargetException | InstantiationException | IllegalAccessException var2) {
            throw new JSONException("invoke error", var2);
         }
      }
   }

   static final class FactoryFunction implements Function {
      final Method method;

      FactoryFunction(Method method) {
         this.method = method;
      }

      @Override
      public Object apply(Object arg) {
         try {
            return this.method.invoke(null, arg);
         } catch (Exception var3) {
            throw new JSONException("createInstance error", var3);
         }
      }
   }

   static final class GetterFunction implements Function {
      final Method method;

      GetterFunction(Method method) {
         this.method = method;
      }

      @Override
      public Object apply(Object arg) {
         try {
            return this.method.invoke(arg);
         } catch (Exception var3) {
            throw new JSONException("createInstance error", var3);
         }
      }
   }

   static final class ReflectBiFunction implements BiFunction {
      final Method method;

      ReflectBiFunction(Method method) {
         this.method = method;
      }

      @Override
      public Object apply(Object arg0, Object arg1) {
         try {
            return Modifier.isStatic(this.method.getModifiers()) ? this.method.invoke(null, arg0, arg1) : this.method.invoke(arg0, arg1);
         } catch (InvocationTargetException | IllegalAccessException var4) {
            throw new JSONException("invoke error", var4);
         }
      }
   }

   static final class ReflectLongFunction implements LongFunction {
      final Constructor constructor;

      public ReflectLongFunction(Constructor constructor) {
         this.constructor = constructor;
      }

      @Override
      public Object apply(long value) {
         try {
            return this.constructor.newInstance(value);
         } catch (Exception var4) {
            throw new JSONException("createInstance error", var4);
         }
      }
   }

   static final class ReflectObjIntConsumer implements ObjIntConsumer {
      final Method method;

      public ReflectObjIntConsumer(Method method) {
         this.method = method;
      }

      @Override
      public void accept(Object object, int value) {
         try {
            this.method.invoke(object, value);
         } catch (InvocationTargetException | IllegalAccessException var4) {
            throw new JSONException("invoke error", var4);
         }
      }
   }

   static final class ReflectSupplier implements Supplier {
      final Method method;

      ReflectSupplier(Method method) {
         this.method = method;
      }

      @Override
      public Object get() {
         try {
            return this.method.invoke(null);
         } catch (InvocationTargetException | IllegalAccessException var2) {
            throw new JSONException("invoke error", var2);
         }
      }
   }

   static final class ReflectToIntFunction implements ToIntFunction {
      final Method method;

      public ReflectToIntFunction(Method method) {
         this.method = method;
      }

      @Override
      public int applyAsInt(Object object) {
         try {
            return (Integer)this.method.invoke(object);
         } catch (Exception var3) {
            throw new JSONException("applyAsInt error", var3);
         }
      }
   }

   static final class ReflectToLongFunction implements ToLongFunction {
      final Method method;

      public ReflectToLongFunction(Method method) {
         this.method = method;
      }

      @Override
      public long applyAsLong(Object object) {
         try {
            return (Long)this.method.invoke(object);
         } catch (Exception var3) {
            throw new JSONException("applyAsLong error", var3);
         }
      }
   }
}
