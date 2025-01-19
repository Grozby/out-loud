package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONException;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import sun.misc.Unsafe;

public class JDKUtils {
   public static final Unsafe UNSAFE;
   public static final long ARRAY_BYTE_BASE_OFFSET;
   public static final long ARRAY_CHAR_BASE_OFFSET;
   public static final int JVM_VERSION;
   public static final Byte LATIN1 = (byte)0;
   public static final Byte UTF16 = (byte)1;
   public static final Field FIELD_STRING_VALUE;
   public static final long FIELD_STRING_VALUE_OFFSET;
   public static volatile boolean FIELD_STRING_VALUE_ERROR;
   public static final long FIELD_DECIMAL_INT_COMPACT_OFFSET;
   public static final long FIELD_BIGINTEGER_MAG_OFFSET;
   public static final Field FIELD_STRING_CODER;
   public static final long FIELD_STRING_CODER_OFFSET;
   public static volatile boolean FIELD_STRING_CODER_ERROR;
   static final Class<?> CLASS_SQL_DATASOURCE;
   static final Class<?> CLASS_SQL_ROW_SET;
   public static final boolean HAS_SQL;
   public static final boolean ANDROID;
   public static final boolean GRAAL;
   public static final boolean OPENJ9;
   public static final int ANDROID_SDK_INT;
   public static final Class CLASS_TRANSIENT;
   public static final boolean BIG_ENDIAN;
   public static final boolean VECTOR_SUPPORT;
   public static final int VECTOR_BIT_LENGTH;
   public static final BiFunction<char[], Boolean, String> STRING_CREATOR_JDK8;
   public static final BiFunction<byte[], Byte, String> STRING_CREATOR_JDK11;
   public static final ToIntFunction<String> STRING_CODER;
   public static final Function<String, byte[]> STRING_VALUE;
   public static final MethodHandle METHOD_HANDLE_HAS_NEGATIVE;
   public static final Predicate<byte[]> PREDICATE_IS_ASCII;
   static final Lookup IMPL_LOOKUP;
   static volatile MethodHandle CONSTRUCTOR_LOOKUP;
   static volatile boolean CONSTRUCTOR_LOOKUP_ERROR;
   static volatile Throwable initErrorLast;
   static volatile Throwable reflectErrorLast;
   static final AtomicInteger reflectErrorCount = new AtomicInteger();

   public static boolean isSQLDataSourceOrRowSet(Class<?> type) {
      return CLASS_SQL_DATASOURCE != null && CLASS_SQL_DATASOURCE.isAssignableFrom(type)
         || CLASS_SQL_ROW_SET != null && CLASS_SQL_ROW_SET.isAssignableFrom(type);
   }

   public static void setReflectErrorLast(Throwable error) {
      reflectErrorCount.incrementAndGet();
      reflectErrorLast = error;
   }

   public static char[] getCharArray(String str) {
      if (!FIELD_STRING_VALUE_ERROR) {
         try {
            return (char[])UNSAFE.getObject(str, FIELD_STRING_VALUE_OFFSET);
         } catch (Exception var2) {
            FIELD_STRING_VALUE_ERROR = true;
         }
      }

      return str.toCharArray();
   }

   public static Lookup trustedLookup(Class objectClass) {
      if (!CONSTRUCTOR_LOOKUP_ERROR) {
         try {
            int TRUSTED = -1;
            MethodHandle constructor = CONSTRUCTOR_LOOKUP;
            if (JVM_VERSION < 15) {
               if (constructor == null) {
                  constructor = IMPL_LOOKUP.findConstructor(Lookup.class, MethodType.methodType(void.class, Class.class, int.class));
                  CONSTRUCTOR_LOOKUP = constructor;
               }

               int FULL_ACCESS_MASK = 31;
               return (Lookup)constructor.invoke((Class)objectClass, (int)(OPENJ9 ? FULL_ACCESS_MASK : TRUSTED));
            }

            if (constructor == null) {
               constructor = IMPL_LOOKUP.findConstructor(Lookup.class, MethodType.methodType(void.class, Class.class, Class.class, int.class));
               CONSTRUCTOR_LOOKUP = constructor;
            }

            return (Lookup)constructor.invoke((Class)objectClass, (Void)null, (int)TRUSTED);
         } catch (Throwable var4) {
            CONSTRUCTOR_LOOKUP_ERROR = true;
         }
      }

      return IMPL_LOOKUP.in(objectClass);
   }

   static {
      Unsafe unsafe;
      long offset;
      long charOffset;
      try {
         Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
         theUnsafeField.setAccessible(true);
         unsafe = (Unsafe)theUnsafeField.get(null);
         offset = (long)unsafe.arrayBaseOffset(byte[].class);
         charOffset = (long)unsafe.arrayBaseOffset(char[].class);
      } catch (Throwable var46) {
         throw new JSONException("init unsafe error", var46);
      }

      UNSAFE = unsafe;
      ARRAY_BYTE_BASE_OFFSET = offset;
      ARRAY_CHAR_BASE_OFFSET = charOffset;
      if (offset == -1L) {
         throw new JSONException("init JDKUtils error", initErrorLast);
      } else {
         int jvmVersion = -1;
         int android_sdk_int = -1;
         boolean openj9 = false;
         boolean android = false;
         boolean graal = false;

         try {
            String jmvName = System.getProperty("java.vm.name");
            openj9 = jmvName.contains("OpenJ9");
            android = "Dalvik".equals(jmvName);
            graal = System.getProperty("org.graalvm.nativeimage.imagecode") != null;
            if (openj9 || android || graal) {
               FIELD_STRING_VALUE_ERROR = true;
            }

            String javaSpecVer = System.getProperty("java.specification.version");
            if (javaSpecVer.startsWith("1.")) {
               javaSpecVer = javaSpecVer.substring(2);
            }

            if (javaSpecVer.indexOf(46) == -1) {
               jvmVersion = Integer.parseInt(javaSpecVer);
            }

            if (android) {
               android_sdk_int = Class.forName("android.os.Build$VERSION").getField("SDK_INT").getInt(null);
            }
         } catch (Throwable var48) {
            initErrorLast = var48;
         }

         OPENJ9 = openj9;
         ANDROID = android;
         GRAAL = graal;
         ANDROID_SDK_INT = android_sdk_int;
         boolean hasJavaSql = true;
         Class dataSourceClass = null;
         Class rowSetClass = null;

         try {
            dataSourceClass = Class.forName("javax.sql.DataSource");
            rowSetClass = Class.forName("javax.sql.RowSet");
         } catch (Throwable var45) {
            hasJavaSql = false;
         }

         CLASS_SQL_DATASOURCE = dataSourceClass;
         CLASS_SQL_ROW_SET = rowSetClass;
         HAS_SQL = hasJavaSql;
         Class transientClass = null;
         if (!android) {
            try {
               transientClass = Class.forName("java.beans.Transient");
            } catch (Throwable var44) {
            }
         }

         CLASS_TRANSIENT = transientClass;
         JVM_VERSION = jvmVersion;
         if (JVM_VERSION == 8) {
            Field field = null;
            long fieldOffset = -1L;
            if (!ANDROID) {
               try {
                  field = String.class.getDeclaredField("value");
                  field.setAccessible(true);
                  fieldOffset = UNSAFE.objectFieldOffset(field);
               } catch (Exception var43) {
                  FIELD_STRING_VALUE_ERROR = true;
               }
            }

            FIELD_STRING_VALUE = field;
            FIELD_STRING_VALUE_OFFSET = fieldOffset;
            FIELD_STRING_CODER = null;
            FIELD_STRING_CODER_OFFSET = -1L;
            FIELD_STRING_CODER_ERROR = true;
         } else {
            Field fieldValue = null;
            long fieldValueOffset = -1L;
            if (!ANDROID) {
               try {
                  fieldValue = String.class.getDeclaredField("value");
                  fieldValueOffset = UNSAFE.objectFieldOffset(fieldValue);
               } catch (Exception var42) {
                  FIELD_STRING_VALUE_ERROR = true;
               }
            }

            FIELD_STRING_VALUE_OFFSET = fieldValueOffset;
            FIELD_STRING_VALUE = fieldValue;
            Field fieldCode = null;
            long fieldCodeOffset = -1L;
            if (!ANDROID) {
               try {
                  fieldCode = String.class.getDeclaredField("coder");
                  fieldCodeOffset = UNSAFE.objectFieldOffset(fieldCode);
               } catch (Exception var41) {
                  FIELD_STRING_CODER_ERROR = true;
               }
            }

            FIELD_STRING_CODER_OFFSET = fieldCodeOffset;
            FIELD_STRING_CODER = fieldCode;
         }

         long fieldOffset = -1L;

         for (Field field : BigDecimal.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (fieldName.equals("intCompact") || fieldName.equals("smallValue")) {
               fieldOffset = UNSAFE.objectFieldOffset(field);
               break;
            }
         }

         FIELD_DECIMAL_INT_COMPACT_OFFSET = fieldOffset;
         fieldOffset = -1L;

         try {
            Field fieldx = BigInteger.class.getDeclaredField("mag");
            fieldOffset = UNSAFE.objectFieldOffset(fieldx);
         } catch (Throwable var40) {
         }

         FIELD_BIGINTEGER_MAG_OFFSET = fieldOffset;
         BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
         BiFunction<char[], Boolean, String> stringCreatorJDK8 = null;
         BiFunction<byte[], Byte, String> stringCreatorJDK11 = null;
         ToIntFunction<String> stringCoder = null;
         Function<String, byte[]> stringValue = null;
         Lookup trustedLookup = null;
         if (!ANDROID) {
            try {
               Class lookupClass = Lookup.class;
               Field implLookup = lookupClass.getDeclaredField("IMPL_LOOKUP");
               long fieldOffsetx = UNSAFE.staticFieldOffset(implLookup);
               trustedLookup = (Lookup)UNSAFE.getObject(lookupClass, fieldOffsetx);
            } catch (Throwable var39) {
            }

            if (trustedLookup == null) {
               trustedLookup = MethodHandles.lookup();
            }
         }

         IMPL_LOOKUP = trustedLookup;
         int vector_bit_length = -1;
         boolean vector_support = false;

         try {
            if (JVM_VERSION >= 11) {
               Class<?> factorClass = Class.forName("java.lang.management.ManagementFactory");
               Class<?> runtimeMXBeanClass = Class.forName("java.lang.management.RuntimeMXBean");
               Method getRuntimeMXBean = factorClass.getMethod("getRuntimeMXBean");
               Object runtimeMXBean = getRuntimeMXBean.invoke(null);
               Method getInputArguments = runtimeMXBeanClass.getMethod("getInputArguments");
               List<String> inputArguments = (List<String>)getInputArguments.invoke(runtimeMXBean);
               vector_support = inputArguments.contains("--add-modules=jdk.incubator.vector");
               if (vector_support) {
                  Class<?> byteVectorClass = Class.forName("jdk.incubator.vector.ByteVector");
                  Class<?> vectorSpeciesClass = Class.forName("jdk.incubator.vector.VectorSpecies");
                  Field speciesMax = byteVectorClass.getField("SPECIES_MAX");
                  Object species = speciesMax.get(null);
                  Method lengthMethod = vectorSpeciesClass.getMethod("length");
                  int length = (Integer)lengthMethod.invoke(species);
                  vector_bit_length = length * 8;
               }
            }
         } catch (Throwable var38) {
            initErrorLast = var38;
         }

         VECTOR_SUPPORT = vector_support;
         VECTOR_BIT_LENGTH = vector_bit_length;
         Predicate<byte[]> isAscii = null;
         MethodHandle handle = null;
         Class<?> classStringCoding = null;
         if (JVM_VERSION >= 17) {
            try {
               classStringCoding = String.class;
               handle = trustedLookup.findStatic(String.class, "isASCII", MethodType.methodType(boolean.class, byte[].class));
            } catch (Throwable var37) {
               initErrorLast = var37;
            }
         }

         if (handle == null && JVM_VERSION >= 11) {
            try {
               classStringCoding = Class.forName("java.lang.StringCoding");
               handle = trustedLookup.findStatic(classStringCoding, "isASCII", MethodType.methodType(boolean.class, byte[].class));
            } catch (Throwable var36) {
               initErrorLast = var36;
            }
         }

         if (handle != null) {
            try {
               Lookup lookup = trustedLookup(classStringCoding);
               CallSite callSite = LambdaMetafactory.metafactory(
                  lookup,
                  "test",
                  MethodType.methodType(Predicate.class),
                  MethodType.methodType(boolean.class, Object.class),
                  handle,
                  MethodType.methodType(boolean.class, byte[].class)
               );
               isAscii = (Predicate)callSite.getTarget().invokeExact();
            } catch (Throwable var35) {
               initErrorLast = var35;
            }
         }

         PREDICATE_IS_ASCII = isAscii;
         MethodHandle handlex = null;
         if (JVM_VERSION >= 11) {
            try {
               Class<?> classStringCodingx = Class.forName("java.lang.StringCoding");
               handlex = trustedLookup.findStatic(classStringCodingx, "hasNegatives", MethodType.methodType(boolean.class, byte[].class, int.class, int.class));
            } catch (Throwable var34) {
               initErrorLast = var34;
            }
         }

         METHOD_HANDLE_HAS_NEGATIVE = handlex;
         Boolean compact_strings = null;

         try {
            if (JVM_VERSION == 8) {
               Lookup lookup = trustedLookup(String.class);
               MethodHandle handlexx = lookup.findConstructor(String.class, MethodType.methodType(void.class, char[].class, boolean.class));
               CallSite callSite = LambdaMetafactory.metafactory(
                  lookup,
                  "apply",
                  MethodType.methodType(BiFunction.class),
                  MethodType.methodType(Object.class, Object.class, Object.class),
                  handlexx,
                  MethodType.methodType(String.class, char[].class, boolean.class)
               );
               stringCreatorJDK8 = (BiFunction)callSite.getTarget().invokeExact();
            }

            boolean lookupLambda = false;
            if (JVM_VERSION > 8 && !android) {
               try {
                  Field compact_strings_field = String.class.getDeclaredField("COMPACT_STRINGS");
                  long fieldOffsetx = UNSAFE.staticFieldOffset(compact_strings_field);
                  compact_strings = UNSAFE.getBoolean(String.class, fieldOffsetx);
               } catch (Throwable var33) {
                  initErrorLast = var33;
               }

               lookupLambda = compact_strings != null && compact_strings;
            }

            if (lookupLambda) {
               Lookup lookup = trustedLookup.in(String.class);
               MethodHandle handlexx = lookup.findConstructor(String.class, MethodType.methodType(void.class, byte[].class, byte.class));
               CallSite callSite = LambdaMetafactory.metafactory(
                  lookup,
                  "apply",
                  MethodType.methodType(BiFunction.class),
                  MethodType.methodType(Object.class, Object.class, Object.class),
                  handlexx,
                  MethodType.methodType(String.class, byte[].class, Byte.class)
               );
               stringCreatorJDK11 = (BiFunction)callSite.getTarget().invokeExact();
               MethodHandle coder = lookup.findSpecial(String.class, "coder", MethodType.methodType(byte.class), String.class);
               CallSite applyAsInt = LambdaMetafactory.metafactory(
                  lookup,
                  "applyAsInt",
                  MethodType.methodType(ToIntFunction.class),
                  MethodType.methodType(int.class, Object.class),
                  coder,
                  MethodType.methodType(byte.class, String.class)
               );
               stringCoder = (ToIntFunction)applyAsInt.getTarget().invokeExact();
               MethodHandle value = lookup.findSpecial(String.class, "value", MethodType.methodType(byte[].class), String.class);
               CallSite apply = LambdaMetafactory.metafactory(
                  lookup,
                  "apply",
                  MethodType.methodType(Function.class),
                  MethodType.methodType(Object.class, Object.class),
                  value,
                  MethodType.methodType(byte[].class, String.class)
               );
               stringValue = (Function)apply.getTarget().invokeExact();
            }
         } catch (Throwable var47) {
            initErrorLast = var47;
         }

         if (stringCoder == null) {
            stringCoder = str -> 1;
         }

         STRING_CREATOR_JDK8 = stringCreatorJDK8;
         STRING_CREATOR_JDK11 = stringCreatorJDK11;
         STRING_CODER = stringCoder;
         STRING_VALUE = stringValue;
      }
   }
}
