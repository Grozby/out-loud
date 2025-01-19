package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplEnum;
import com.alibaba.fastjson2.reader.ObjectReaderImplInstant;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterPrimitiveImpl;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class TypeUtils {
   public static final Class CLASS_JSON_OBJECT_1x = loadClass("com.alibaba.fastjson.JSONObject");
   public static final Field FIELD_JSON_OBJECT_1x_map;
   public static final Class CLASS_JSON_ARRAY_1x;
   public static final Class CLASS_SINGLE_SET = Collections.singleton(1).getClass();
   public static final Class CLASS_SINGLE_LIST = Collections.singletonList(1).getClass();
   public static final Class CLASS_UNMODIFIABLE_COLLECTION = Collections.unmodifiableCollection(new ArrayList()).getClass();
   public static final Class CLASS_UNMODIFIABLE_LIST = Collections.unmodifiableList(new ArrayList()).getClass();
   public static final Class CLASS_UNMODIFIABLE_SET = Collections.unmodifiableSet(new HashSet()).getClass();
   public static final Class CLASS_UNMODIFIABLE_SORTED_SET = Collections.unmodifiableSortedSet(new TreeSet()).getClass();
   public static final Class CLASS_UNMODIFIABLE_NAVIGABLE_SET = Collections.unmodifiableNavigableSet(new TreeSet()).getClass();
   public static final ParameterizedType PARAM_TYPE_LIST_STR = new ParameterizedTypeImpl(List.class, String.class);
   public static final MethodType METHOD_TYPE_SUPPLIER = MethodType.methodType(Supplier.class);
   public static final MethodType METHOD_TYPE_FUNCTION = MethodType.methodType(Function.class);
   public static final MethodType METHOD_TYPE_TO_INT_FUNCTION = MethodType.methodType(ToIntFunction.class);
   public static final MethodType METHOD_TYPE_TO_LONG_FUNCTION = MethodType.methodType(ToLongFunction.class);
   public static final MethodType METHOD_TYPE_OBJECT_INT_CONSUMER = MethodType.methodType(ObjIntConsumer.class);
   public static final MethodType METHOD_TYPE_INT_FUNCTION = MethodType.methodType(IntFunction.class);
   public static final MethodType METHOD_TYPE_LONG_FUNCTION = MethodType.methodType(LongFunction.class);
   public static final MethodType METHOD_TYPE_BI_FUNCTION = MethodType.methodType(BiFunction.class);
   public static final MethodType METHOD_TYPE_BI_CONSUMER = MethodType.methodType(BiConsumer.class);
   public static final MethodType METHOD_TYPE_VOO = MethodType.methodType(void.class, Object.class, Object.class);
   public static final MethodType METHOD_TYPE_OBJECT = MethodType.methodType(Object.class);
   public static final MethodType METHOD_TYPE_OBJECT_OBJECT = MethodType.methodType(Object.class, Object.class);
   public static final MethodType METHOD_TYPE_INT_OBJECT = MethodType.methodType(int.class, Object.class);
   public static final MethodType METHOD_TYPE_LONG_OBJECT = MethodType.methodType(long.class, Object.class);
   public static final MethodType METHOD_TYPE_VOID_OBJECT_INT = MethodType.methodType(void.class, Object.class, int.class);
   public static final MethodType METHOD_TYPE_OBJECT_LONG = MethodType.methodType(Object.class, long.class);
   public static final MethodType METHOD_TYPE_VOID_LONG = MethodType.methodType(void.class, long.class);
   public static final MethodType METHOD_TYPE_OBJECT_OBJECT_OBJECT = MethodType.methodType(Object.class, Object.class, Object.class);
   public static final MethodType METHOD_TYPE_VOID = MethodType.methodType(void.class);
   public static final MethodType METHOD_TYPE_VOID_INT = MethodType.methodType(void.class, int.class);
   public static final MethodType METHOD_TYPE_VOID_STRING = MethodType.methodType(void.class, String.class);
   public static final MethodType METHOD_TYPE_OBJECT_INT = MethodType.methodType(Object.class, int.class);
   public static final BigInteger BIGINT_INT32_MIN = BigInteger.valueOf(-2147483648L);
   public static final BigInteger BIGINT_INT32_MAX = BigInteger.valueOf(2147483647L);
   public static final BigInteger BIGINT_INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE);
   public static final BigInteger BIGINT_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);
   static final long LONG_JAVASCRIPT_LOW = -9007199254740991L;
   static final long LONG_JAVASCRIPT_HIGH = 9007199254740991L;
   static final BigDecimal DECIMAL_JAVASCRIPT_LOW = BigDecimal.valueOf(-9007199254740991L);
   static final BigDecimal DECIMAL_JAVASCRIPT_HIGH = BigDecimal.valueOf(9007199254740991L);
   static final BigInteger BIGINT_JAVASCRIPT_LOW = BigInteger.valueOf(-9007199254740991L);
   static final BigInteger BIGINT_JAVASCRIPT_HIGH = BigInteger.valueOf(9007199254740991L);
   public static final double[] SMALL_10_POW = new double[]{
      1.0,
      10.0,
      100.0,
      1000.0,
      10000.0,
      100000.0,
      1000000.0,
      1.0E7,
      1.0E8,
      1.0E9,
      1.0E10,
      1.0E11,
      1.0E12,
      1.0E13,
      1.0E14,
      1.0E15,
      1.0E16,
      1.0E17,
      1.0E18,
      1.0E19,
      1.0E20,
      1.0E21,
      1.0E22
   };
   static final float[] SINGLE_SMALL_10_POW = new float[]{1.0F, 10.0F, 100.0F, 1000.0F, 10000.0F, 100000.0F, 1000000.0F, 1.0E7F, 1.0E8F, 1.0E9F, 1.0E10F};
   static final double[] BIG_10_POW = new double[]{1.0E16, 1.0E32, 1.0E64, 1.0E128, 1.0E256};
   static final double[] TINY_10_POW = new double[]{1.0E-16, 1.0E-32, 1.0E-64, 1.0E-128, 1.0E-256};
   static volatile boolean METHOD_NEW_PROXY_INSTANCE_ERROR;
   static volatile MethodHandle METHOD_NEW_PROXY_INSTANCE;
   static final TypeUtils.Cache CACHE = new TypeUtils.Cache();
   static final AtomicReferenceFieldUpdater<TypeUtils.Cache, char[]> CHARS_UPDATER = AtomicReferenceFieldUpdater.newUpdater(
      TypeUtils.Cache.class, char[].class, "chars"
   );
   static final Map<Class, String> NAME_MAPPINGS = new IdentityHashMap<>();
   static final Map<String, Class> TYPE_MAPPINGS = new ConcurrentHashMap<>();
   private static final BigInteger[] BIG_TEN_POWERS_TABLE;

   public static <T> T newProxyInstance(Class<T> objectClass, JSONObject object) {
      MethodHandle newProxyInstance = METHOD_NEW_PROXY_INSTANCE;

      try {
         if (newProxyInstance == null) {
            Class<?> proxyClass = Class.forName("java.lang.reflect.Proxy");
            Lookup lookup = JDKUtils.trustedLookup(proxyClass);
            newProxyInstance = lookup.findStatic(
               proxyClass, "newProxyInstance", MethodType.methodType(Object.class, ClassLoader.class, Class[].class, InvocationHandler.class)
            );
            METHOD_NEW_PROXY_INSTANCE = newProxyInstance;
         }
      } catch (Throwable var6) {
         METHOD_NEW_PROXY_INSTANCE_ERROR = true;
      }

      try {
         return (T)(Object)newProxyInstance.invokeExact(
            (ClassLoader)objectClass.getClassLoader(), (Class[])(new Class[]{objectClass}), (InvocationHandler)object
         );
      } catch (Throwable var5) {
         throw new JSONException("create proxy error : " + objectClass, var5);
      }
   }

   static char[] toAsciiCharArray(byte[] bytes) {
      char[] charArray = new char[bytes.length];

      for (int i = 0; i < bytes.length; i++) {
         charArray[i] = (char)bytes[i];
      }

      return charArray;
   }

   public static String toString(char ch) {
      return ch < TypeUtils.X2.chars.length ? TypeUtils.X2.chars[ch] : Character.toString(ch);
   }

   public static String toString(byte ch) {
      return ch >= 0 && ch < TypeUtils.X2.chars.length ? TypeUtils.X2.chars[ch] : new String(new byte[]{ch}, StandardCharsets.ISO_8859_1);
   }

   public static String toString(char c0, char c1) {
      if (c0 >= ' ' && c0 <= '~' && c1 >= ' ' && c1 <= '~') {
         int value = (c0 - ' ') * 95 + (c1 - ' ');
         return TypeUtils.X2.chars2[value];
      } else {
         return new String(new char[]{c0, c1});
      }
   }

   public static String toString(byte c0, byte c1) {
      if (c0 >= 32 && c0 <= 126 && c1 >= 32 && c1 <= 126) {
         int value = (c0 - 32) * 95 + (c1 - 32);
         return TypeUtils.X2.chars2[value];
      } else {
         return new String(new byte[]{c0, c1}, StandardCharsets.ISO_8859_1);
      }
   }

   public static Type intern(Type type) {
      if (type instanceof ParameterizedType) {
         ParameterizedType paramType = (ParameterizedType)type;
         Type rawType = paramType.getRawType();
         Type[] actualTypeArguments = paramType.getActualTypeArguments();
         if (rawType == List.class && actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class) {
            return PARAM_TYPE_LIST_STR;
         }
      }

      return type;
   }

   public static double parseDouble(byte[] in, int off, int len) throws NumberFormatException {
      boolean isNegative = false;
      boolean signSeen = false;
      int end = off + len;

      try {
         if (len == 0) {
            throw new NumberFormatException("empty String");
         }

         int i = off;
         char[] digits;
         int nDigits;
         boolean decSeen;
         int decPt;
         int nLeadZero;
         int nTrailZero;
         switch (in[off]) {
            case 45:
               isNegative = true;
            case 43:
               i = off + 1;
               signSeen = true;
            default:
               digits = new char[len];
               nDigits = 0;
               decSeen = false;
               decPt = 0;
               nLeadZero = 0;
               nTrailZero = 0;
         }

         for (; i < end; i++) {
            byte c = in[i];
            if (c == 48) {
               nLeadZero++;
            } else {
               if (c != 46) {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         for (; i < end; i++) {
            byte c = in[i];
            if (c >= 49 && c <= 57) {
               digits[nDigits++] = (char)c;
               nTrailZero = 0;
            } else if (c == 48) {
               digits[nDigits++] = (char)c;
               nTrailZero++;
            } else {
               if (c != 46) {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         nDigits -= nTrailZero;
         boolean isZero = nDigits == 0;
         if (!isZero || nLeadZero != 0) {
            int decExp;
            if (decSeen) {
               decExp = decPt - nLeadZero;
            } else {
               decExp = nDigits + nTrailZero;
            }

            byte c;
            if (i < end && ((c = in[i]) == 101 || c == 69)) {
               int expSign = 1;
               int expVal = 0;
               int reallyBig = 214748364;
               boolean expOverflow = false;
               i++;
               int expAt;
               switch (in[i]) {
                  case 45:
                     expSign = -1;
                  case 43:
                     i++;
                  default:
                     expAt = i;
               }

               while (i < end) {
                  if (expVal >= reallyBig) {
                     expOverflow = true;
                  }

                  c = in[i++];
                  if (c < 48 || c > 57) {
                     i--;
                     break;
                  }

                  expVal = expVal * 10 + (c - 48);
               }

               int BIG_DECIMAL_EXPONENT = 324;
               int expLimit = 324 + nDigits + nTrailZero;
               if (!expOverflow && expVal <= expLimit) {
                  decExp += expSign * expVal;
               } else {
                  decExp = expSign * expLimit;
               }

               if (i == expAt) {
                  throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
               }
            }

            if (i >= end || i == end - 1) {
               if (isZero) {
                  return 0.0;
               }

               return doubleValue(isNegative, decExp, digits, nDigits);
            }
         }
      } catch (StringIndexOutOfBoundsException var23) {
      }

      throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
   }

   public static double parseDouble(char[] in, int off, int len) throws NumberFormatException {
      boolean isNegative = false;
      boolean signSeen = false;
      int end = off + len;

      try {
         if (len == 0) {
            throw new NumberFormatException("empty String");
         }

         int i = off;
         char[] digits;
         int nDigits;
         boolean decSeen;
         int decPt;
         int nLeadZero;
         int nTrailZero;
         switch (in[off]) {
            case '-':
               isNegative = true;
            case '+':
               i = off + 1;
               signSeen = true;
            default:
               digits = new char[len];
               nDigits = 0;
               decSeen = false;
               decPt = 0;
               nLeadZero = 0;
               nTrailZero = 0;
         }

         for (; i < end; i++) {
            char c = in[i];
            if (c == '0') {
               nLeadZero++;
            } else {
               if (c != '.') {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         for (; i < end; i++) {
            char c = in[i];
            if (c >= '1' && c <= '9') {
               digits[nDigits++] = c;
               nTrailZero = 0;
            } else if (c == '0') {
               digits[nDigits++] = c;
               nTrailZero++;
            } else {
               if (c != '.') {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         nDigits -= nTrailZero;
         boolean isZero = nDigits == 0;
         if (!isZero || nLeadZero != 0) {
            int decExp;
            if (decSeen) {
               decExp = decPt - nLeadZero;
            } else {
               decExp = nDigits + nTrailZero;
            }

            char c;
            if (i < end && ((c = in[i]) == 'e' || c == 'E')) {
               int expSign = 1;
               int expVal = 0;
               int reallyBig = 214748364;
               boolean expOverflow = false;
               i++;
               int expAt;
               switch (in[i]) {
                  case '-':
                     expSign = -1;
                  case '+':
                     i++;
                  default:
                     expAt = i;
               }

               while (i < end) {
                  if (expVal >= reallyBig) {
                     expOverflow = true;
                  }

                  c = in[i++];
                  if (c < '0' || c > '9') {
                     i--;
                     break;
                  }

                  expVal = expVal * 10 + (c - '0');
               }

               int BIG_DECIMAL_EXPONENT = 324;
               int expLimit = 324 + nDigits + nTrailZero;
               if (!expOverflow && expVal <= expLimit) {
                  decExp += expSign * expVal;
               } else {
                  decExp = expSign * expLimit;
               }

               if (i == expAt) {
                  throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
               }
            }

            if (i >= end || i == end - 1) {
               if (isZero) {
                  return 0.0;
               }

               return doubleValue(isNegative, decExp, digits, nDigits);
            }
         }
      } catch (StringIndexOutOfBoundsException var23) {
      }

      throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
   }

   public static float parseFloat(byte[] in, int off, int len) throws NumberFormatException {
      boolean isNegative = false;
      boolean signSeen = false;
      int end = off + len;

      try {
         if (len == 0) {
            throw new NumberFormatException("empty String");
         }

         int i = off;
         char[] digits;
         int nDigits;
         boolean decSeen;
         int decPt;
         int nLeadZero;
         int nTrailZero;
         switch (in[off]) {
            case 45:
               isNegative = true;
            case 43:
               i = off + 1;
               signSeen = true;
            default:
               digits = new char[len];
               nDigits = 0;
               decSeen = false;
               decPt = 0;
               nLeadZero = 0;
               nTrailZero = 0;
         }

         for (; i < end; i++) {
            byte c = in[i];
            if (c == 48) {
               nLeadZero++;
            } else {
               if (c != 46) {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         for (; i < end; i++) {
            byte c = in[i];
            if (c >= 49 && c <= 57) {
               digits[nDigits++] = (char)c;
               nTrailZero = 0;
            } else if (c == 48) {
               digits[nDigits++] = (char)c;
               nTrailZero++;
            } else {
               if (c != 46) {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         nDigits -= nTrailZero;
         boolean isZero = nDigits == 0;
         if (!isZero || nLeadZero != 0) {
            int decExp;
            if (decSeen) {
               decExp = decPt - nLeadZero;
            } else {
               decExp = nDigits + nTrailZero;
            }

            byte c;
            if (i < end && ((c = in[i]) == 101 || c == 69)) {
               int expSign = 1;
               int expVal = 0;
               int reallyBig = 214748364;
               boolean expOverflow = false;
               i++;
               int expAt;
               switch (in[i]) {
                  case 45:
                     expSign = -1;
                  case 43:
                     i++;
                  default:
                     expAt = i;
               }

               while (i < end) {
                  if (expVal >= reallyBig) {
                     expOverflow = true;
                  }

                  c = in[i++];
                  if (c < 48 || c > 57) {
                     i--;
                     break;
                  }

                  expVal = expVal * 10 + (c - 48);
               }

               int BIG_DECIMAL_EXPONENT = 324;
               int expLimit = 324 + nDigits + nTrailZero;
               if (!expOverflow && expVal <= expLimit) {
                  decExp += expSign * expVal;
               } else {
                  decExp = expSign * expLimit;
               }

               if (i == expAt) {
                  throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
               }
            }

            if (i >= end || i == end - 1) {
               if (isZero) {
                  return 0.0F;
               }

               return floatValue(isNegative, decExp, digits, nDigits);
            }
         }
      } catch (StringIndexOutOfBoundsException var23) {
      }

      throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
   }

   public static float parseFloat(char[] in, int off, int len) throws NumberFormatException {
      boolean isNegative = false;
      boolean signSeen = false;
      int end = off + len;

      try {
         if (len == 0) {
            throw new NumberFormatException("empty String");
         }

         int i = off;
         char[] digits;
         int nDigits;
         boolean decSeen;
         int decPt;
         int nLeadZero;
         int nTrailZero;
         switch (in[off]) {
            case '-':
               isNegative = true;
            case '+':
               i = off + 1;
               signSeen = true;
            default:
               digits = new char[len];
               nDigits = 0;
               decSeen = false;
               decPt = 0;
               nLeadZero = 0;
               nTrailZero = 0;
         }

         for (; i < end; i++) {
            char c = in[i];
            if (c == '0') {
               nLeadZero++;
            } else {
               if (c != '.') {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         for (; i < end; i++) {
            char c = in[i];
            if (c >= '1' && c <= '9') {
               digits[nDigits++] = c;
               nTrailZero = 0;
            } else if (c == '0') {
               digits[nDigits++] = c;
               nTrailZero++;
            } else {
               if (c != '.') {
                  break;
               }

               if (decSeen) {
                  throw new NumberFormatException("multiple points");
               }

               decPt = i - off;
               if (signSeen) {
                  decPt--;
               }

               decSeen = true;
            }
         }

         nDigits -= nTrailZero;
         boolean isZero = nDigits == 0;
         if (!isZero || nLeadZero != 0) {
            int decExp;
            if (decSeen) {
               decExp = decPt - nLeadZero;
            } else {
               decExp = nDigits + nTrailZero;
            }

            char c;
            if (i < end && ((c = in[i]) == 'e' || c == 'E')) {
               int expSign = 1;
               int expVal = 0;
               int reallyBig = 214748364;
               boolean expOverflow = false;
               i++;
               int expAt;
               switch (in[i]) {
                  case '-':
                     expSign = -1;
                  case '+':
                     i++;
                  default:
                     expAt = i;
               }

               while (i < end) {
                  if (expVal >= reallyBig) {
                     expOverflow = true;
                  }

                  c = in[i++];
                  if (c < '0' || c > '9') {
                     i--;
                     break;
                  }

                  expVal = expVal * 10 + (c - '0');
               }

               int BIG_DECIMAL_EXPONENT = 324;
               int expLimit = 324 + nDigits + nTrailZero;
               if (!expOverflow && expVal <= expLimit) {
                  decExp += expSign * expVal;
               } else {
                  decExp = expSign * expLimit;
               }

               if (i == expAt) {
                  throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
               }
            }

            if (i >= end || i == end - 1) {
               if (isZero) {
                  return 0.0F;
               }

               return floatValue(isNegative, decExp, digits, nDigits);
            }
         }
      } catch (StringIndexOutOfBoundsException var23) {
      }

      throw new NumberFormatException("For input string: \"" + new String(in, off, len) + "\"");
   }

   public static double doubleValue(boolean isNegative, int decExp, char[] digits, int nDigits) {
      int MAX_DECIMAL_EXPONENT = 308;
      int MIN_DECIMAL_EXPONENT = -324;
      int MAX_NDIGITS = 1100;
      int INT_DECIMAL_DIGITS = 9;
      int MAX_DECIMAL_DIGITS = 15;
      int DOUBLE_EXP_BIAS = 1023;
      int EXP_SHIFT = 52;
      long FRACT_HOB = 4503599627370496L;
      int MAX_SMALL_TEN = SMALL_10_POW.length - 1;
      int SINGLE_MAX_SMALL_TEN = SINGLE_SMALL_10_POW.length - 1;
      int kDigits = Math.min(nDigits, 16);
      int iValue = digits[0] - '0';
      int iDigits = Math.min(kDigits, 9);

      for (int i = 1; i < iDigits; i++) {
         iValue = iValue * 10 + digits[i] - 48;
      }

      long lValue = (long)iValue;

      for (int i = iDigits; i < kDigits; i++) {
         lValue = lValue * 10L + (long)(digits[i] - '0');
      }

      double dValue = (double)lValue;
      int exp = decExp - kDigits;
      if (nDigits <= 15) {
         if (exp == 0 || dValue == 0.0) {
            return isNegative ? -dValue : dValue;
         }

         if (exp >= 0) {
            if (exp <= MAX_SMALL_TEN) {
               double rValue = dValue * SMALL_10_POW[exp];
               return isNegative ? -rValue : rValue;
            }

            int slop = 15 - kDigits;
            if (exp <= MAX_SMALL_TEN + slop) {
               dValue *= SMALL_10_POW[slop];
               double rValue = dValue * SMALL_10_POW[exp - slop];
               return isNegative ? -rValue : rValue;
            }
         } else if (exp >= -MAX_SMALL_TEN) {
            double rValue = dValue / SMALL_10_POW[-exp];
            return isNegative ? -rValue : rValue;
         }
      }

      if (exp > 0) {
         if (decExp > 309) {
            return isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
         }

         if ((exp & 15) != 0) {
            dValue *= SMALL_10_POW[exp & 15];
         }

         if ((exp = exp >> 4) != 0) {
            int j;
            for (j = 0; exp > 1; exp >>= 1) {
               if ((exp & 1) != 0) {
                  dValue *= BIG_10_POW[j];
               }

               j++;
            }

            double t = dValue * BIG_10_POW[j];
            if (Double.isInfinite(t)) {
               t = dValue / 2.0;
               t *= BIG_10_POW[j];
               if (Double.isInfinite(t)) {
                  return isNegative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
               }

               t = Double.MAX_VALUE;
            }

            dValue = t;
         }
      } else if (exp < 0) {
         exp = -exp;
         if (decExp < -325) {
            return isNegative ? -0.0 : 0.0;
         }

         if ((exp & 15) != 0) {
            dValue /= SMALL_10_POW[exp & 15];
         }

         if ((exp = exp >> 4) != 0) {
            int jx;
            for (jx = 0; exp > 1; exp >>= 1) {
               if ((exp & 1) != 0) {
                  dValue *= TINY_10_POW[jx];
               }

               jx++;
            }

            double t = dValue * TINY_10_POW[jx];
            if (t == 0.0) {
               t = dValue * 2.0;
               t *= TINY_10_POW[jx];
               if (t == 0.0) {
                  return isNegative ? -0.0 : 0.0;
               }

               t = Double.MIN_VALUE;
            }

            dValue = t;
         }
      }

      if (nDigits > 1100) {
         nDigits = 1101;
         digits[1100] = '1';
      }

      FDBigInteger bigD0 = new FDBigInteger(lValue, digits, kDigits, nDigits);
      exp = decExp - nDigits;
      long ieeeBits = Double.doubleToRawLongBits(dValue);
      int B5 = Math.max(0, -exp);
      int D5 = Math.max(0, exp);
      bigD0 = bigD0.multByPow52(D5, 0);
      bigD0.makeImmutable();
      FDBigInteger bigD = null;
      int prevD2 = 0;

      do {
         int binexp = (int)(ieeeBits >>> 52);
         long DOUBLE_SIGNIF_BIT_MASK = 4503599627370495L;
         long bigBbits = ieeeBits & 4503599627370495L;
         if (binexp > 0) {
            bigBbits |= 4503599627370496L;
         } else {
            assert bigBbits != 0L : bigBbits;

            int leadingZeros = Long.numberOfLeadingZeros(bigBbits);
            int shift = leadingZeros - 11;
            bigBbits <<= shift;
            binexp = 1 - shift;
         }

         binexp -= 1023;
         int lowOrderZeros = Long.numberOfTrailingZeros(bigBbits);
         bigBbits >>>= lowOrderZeros;
         int bigIntExp = binexp - 52 + lowOrderZeros;
         int bigIntNBits = 53 - lowOrderZeros;
         int B2 = B5;
         int D2 = D5;
         if (bigIntExp >= 0) {
            B2 = B5 + bigIntExp;
         } else {
            D2 = D5 - bigIntExp;
         }

         int hulpbias;
         if (binexp <= -1023) {
            hulpbias = binexp + lowOrderZeros + 1023;
         } else {
            hulpbias = 1 + lowOrderZeros;
         }

         int var75 = B2 + hulpbias;
         D2 += hulpbias;
         int common2 = Math.min(var75, Math.min(D2, B2));
         int var76 = var75 - common2;
         D2 -= common2;
         int var79 = B2 - common2;
         FDBigInteger bigB = FDBigInteger.valueOfMulPow52(bigBbits, B5, var76);
         if (bigD == null || prevD2 != D2) {
            bigD = bigD0.leftShift(D2);
            prevD2 = D2;
         }

         FDBigInteger diff;
         int cmpResult;
         boolean overvalue;
         if ((cmpResult = bigB.cmp(bigD)) > 0) {
            overvalue = true;
            diff = bigB.leftInplaceSub(bigD);
            if (bigIntNBits == 1 && bigIntExp > -1022) {
               if (--var79 < 0) {
                  var79 = 0;
                  diff = diff.leftShift(1);
               }
            }
         } else {
            if (cmpResult >= 0) {
               break;
            }

            overvalue = false;
            diff = bigD.rightInplaceSub(bigB);
         }

         cmpResult = diff.cmpPow52(B5, var79);
         if (cmpResult < 0) {
            break;
         }

         if (cmpResult == 0) {
            if ((ieeeBits & 1L) != 0L) {
               ieeeBits += overvalue ? -1L : 1L;
            }
            break;
         }

         ieeeBits += overvalue ? -1L : 1L;
         long DOUBLE_EXP_BIT_MASK = 9218868437227405312L;
      } while (ieeeBits != 0L && ieeeBits != 9218868437227405312L);

      if (isNegative) {
         long DOUBLE_SIGN_BIT_MASK = Long.MIN_VALUE;
         ieeeBits |= Long.MIN_VALUE;
      }

      return Double.longBitsToDouble(ieeeBits);
   }

   public static float floatValue(boolean isNegative, int decExponent, char[] digits, int nDigits) {
      int SINGLE_MAX_NDIGITS = 200;
      int SINGLE_MAX_DECIMAL_DIGITS = 7;
      int MAX_DECIMAL_DIGITS = 15;
      int FLOAT_EXP_BIAS = 127;
      int SINGLE_EXP_SHIFT = 23;
      int SINGLE_MAX_SMALL_TEN = SINGLE_SMALL_10_POW.length - 1;
      int kDigits = Math.min(nDigits, 8);
      int iValue = digits[0] - '0';

      for (int i = 1; i < kDigits; i++) {
         iValue = iValue * 10 + digits[i] - 48;
      }

      float fValue = (float)iValue;
      int exp = decExponent - kDigits;
      if (nDigits <= 7) {
         if (exp == 0 || fValue == 0.0F) {
            return isNegative ? -fValue : fValue;
         }

         if (exp >= 0) {
            if (exp <= SINGLE_MAX_SMALL_TEN) {
               fValue *= SINGLE_SMALL_10_POW[exp];
               return isNegative ? -fValue : fValue;
            }

            int slop = 7 - kDigits;
            if (exp <= SINGLE_MAX_SMALL_TEN + slop) {
               fValue *= SINGLE_SMALL_10_POW[slop];
               fValue *= SINGLE_SMALL_10_POW[exp - slop];
               return isNegative ? -fValue : fValue;
            }
         } else if (exp >= -SINGLE_MAX_SMALL_TEN) {
            fValue /= SINGLE_SMALL_10_POW[-exp];
            return isNegative ? -fValue : fValue;
         }
      } else if (decExponent >= nDigits && nDigits + decExponent <= 15) {
         long lValue = (long)iValue;

         for (int i = kDigits; i < nDigits; i++) {
            lValue = lValue * 10L + (long)(digits[i] - '0');
         }

         double dValue = (double)lValue;
         exp = decExponent - nDigits;
         dValue *= SMALL_10_POW[exp];
         fValue = (float)dValue;
         return isNegative ? -fValue : fValue;
      }

      double dValue = (double)fValue;
      if (exp > 0) {
         int SINGLE_MAX_DECIMAL_EXPONENT = 38;
         if (decExponent > 39) {
            return isNegative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
         }

         if ((exp & 15) != 0) {
            dValue *= SMALL_10_POW[exp & 15];
         }

         if ((exp = exp >> 4) != 0) {
            for (int j = 0; exp > 0; exp >>= 1) {
               if ((exp & 1) != 0) {
                  dValue *= BIG_10_POW[j];
               }

               j++;
            }
         }
      } else if (exp < 0) {
         exp = -exp;
         int SINGLE_MIN_DECIMAL_EXPONENT = -45;
         if (decExponent < -46) {
            return isNegative ? -0.0F : 0.0F;
         }

         if ((exp & 15) != 0) {
            dValue /= SMALL_10_POW[exp & 15];
         }

         if ((exp = exp >> 4) != 0) {
            for (int j = 0; exp > 0; exp >>= 1) {
               if ((exp & 1) != 0) {
                  dValue *= TINY_10_POW[j];
               }

               j++;
            }
         }
      }

      fValue = Math.max(Float.MIN_VALUE, Math.min(Float.MAX_VALUE, (float)dValue));
      if (nDigits > 200) {
         nDigits = 201;
         digits[200] = '1';
      }

      FDBigInteger bigD0 = new FDBigInteger((long)iValue, digits, kDigits, nDigits);
      exp = decExponent - nDigits;
      int ieeeBits = Float.floatToRawIntBits(fValue);
      int B5 = Math.max(0, -exp);
      int D5 = Math.max(0, exp);
      bigD0 = bigD0.multByPow52(D5, 0);
      bigD0.makeImmutable();
      FDBigInteger bigD = null;
      int prevD2 = 0;

      do {
         int binexp = ieeeBits >>> 23;
         int FLOAT_SIGNIF_BIT_MASK = 8388607;
         int bigBbits = ieeeBits & 8388607;
         if (binexp > 0) {
            int SINGLE_FRACT_HOB = 8388608;
            bigBbits |= 8388608;
         } else {
            assert bigBbits != 0 : bigBbits;

            int leadingZeros = Integer.numberOfLeadingZeros(bigBbits);
            int shift = leadingZeros - 8;
            bigBbits <<= shift;
            binexp = 1 - shift;
         }

         binexp -= 127;
         int lowOrderZeros = Integer.numberOfTrailingZeros(bigBbits);
         bigBbits >>>= lowOrderZeros;
         int bigIntExp = binexp - 23 + lowOrderZeros;
         int bigIntNBits = 24 - lowOrderZeros;
         int B2 = B5;
         int D2 = D5;
         if (bigIntExp >= 0) {
            B2 = B5 + bigIntExp;
         } else {
            D2 = D5 - bigIntExp;
         }

         int hulpbias;
         if (binexp <= -127) {
            hulpbias = binexp + lowOrderZeros + 127;
         } else {
            hulpbias = 1 + lowOrderZeros;
         }

         int var67 = B2 + hulpbias;
         D2 += hulpbias;
         int common2 = Math.min(var67, Math.min(D2, B2));
         int var68 = var67 - common2;
         D2 -= common2;
         int var71 = B2 - common2;
         FDBigInteger bigB = FDBigInteger.valueOfMulPow52((long)bigBbits, B5, var68);
         if (bigD == null || prevD2 != D2) {
            bigD = bigD0.leftShift(D2);
            prevD2 = D2;
         }

         FDBigInteger diff;
         int cmpResult;
         boolean overvalue;
         if ((cmpResult = bigB.cmp(bigD)) > 0) {
            overvalue = true;
            diff = bigB.leftInplaceSub(bigD);
            if (bigIntNBits == 1 && bigIntExp > -126) {
               if (--var71 < 0) {
                  var71 = 0;
                  diff = diff.leftShift(1);
               }
            }
         } else {
            if (cmpResult >= 0) {
               break;
            }

            overvalue = false;
            diff = bigD.rightInplaceSub(bigB);
         }

         cmpResult = diff.cmpPow52(B5, var71);
         if (cmpResult < 0) {
            break;
         }

         if (cmpResult == 0) {
            if ((ieeeBits & 1) != 0) {
               ieeeBits += overvalue ? -1 : 1;
            }
            break;
         }

         ieeeBits += overvalue ? -1 : 1;
         int FLOAT_EXP_BIT_MASK = 2139095040;
      } while (ieeeBits != 0 && ieeeBits != 2139095040);

      if (isNegative) {
         int FLOAT_SIGN_BIT_MASK = Integer.MIN_VALUE;
         ieeeBits |= Integer.MIN_VALUE;
      }

      return Float.intBitsToFloat(ieeeBits);
   }

   public static Class<?> getMapping(Type type) {
      if (type == null) {
         return null;
      } else if (type.getClass() == Class.class) {
         return (Class<?>)type;
      } else if (type instanceof ParameterizedType) {
         return getMapping(((ParameterizedType)type).getRawType());
      } else if (type instanceof TypeVariable) {
         Type boundType = ((TypeVariable)type).getBounds()[0];
         return boundType instanceof Class ? (Class)boundType : getMapping(boundType);
      } else {
         if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType)type).getUpperBounds();
            if (upperBounds.length == 1) {
               return getMapping(upperBounds[0]);
            }
         }

         if (type instanceof GenericArrayType) {
            Type genericComponentType = ((GenericArrayType)type).getGenericComponentType();
            Class<?> componentClass = getClass(genericComponentType);
            return getArrayClass(componentClass);
         } else {
            return Object.class;
         }
      }
   }

   public static Date toDate(Object obj) {
      if (obj == null) {
         return null;
      } else if (obj instanceof Date) {
         return (Date)obj;
      } else if (obj instanceof Instant) {
         Instant instant = (Instant)obj;
         return new Date(instant.toEpochMilli());
      } else if (obj instanceof ZonedDateTime) {
         ZonedDateTime zdt = (ZonedDateTime)obj;
         return new Date(zdt.toInstant().toEpochMilli());
      } else if (obj instanceof LocalDate) {
         LocalDate localDate = (LocalDate)obj;
         ZonedDateTime zdt = localDate.atStartOfDay(ZoneId.systemDefault());
         return new Date(zdt.toInstant().toEpochMilli());
      } else if (obj instanceof LocalDateTime) {
         LocalDateTime ldt = (LocalDateTime)obj;
         ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
         return new Date(zdt.toInstant().toEpochMilli());
      } else if (obj instanceof String) {
         return DateUtils.parseDate((String)obj);
      } else if (!(obj instanceof Long) && !(obj instanceof Integer)) {
         if (obj instanceof Map) {
            Object date = ((Map)obj).get("$date");
            if (date instanceof String) {
               return DateUtils.parseDate((String)date);
            }
         }

         throw new JSONException("can not cast to Date from " + obj.getClass());
      } else {
         return new Date(((Number)obj).longValue());
      }
   }

   public static Instant toInstant(Object obj) {
      if (obj == null) {
         return null;
      } else if (obj instanceof Instant) {
         return (Instant)obj;
      } else if (obj instanceof Date) {
         return ((Date)obj).toInstant();
      } else if (obj instanceof ZonedDateTime) {
         ZonedDateTime zdt = (ZonedDateTime)obj;
         return zdt.toInstant();
      } else if (obj instanceof String) {
         String str = (String)obj;
         if (!str.isEmpty() && !"null".equals(str)) {
            JSONReader jsonReader;
            if (str.charAt(0) != '"') {
               jsonReader = JSONReader.of('"' + str + '"');
            } else {
               jsonReader = JSONReader.of(str);
            }

            return jsonReader.read(Instant.class);
         } else {
            return null;
         }
      } else if (obj instanceof Map) {
         return (Instant)ObjectReaderImplInstant.INSTANCE.createInstance((Map)obj, 0L);
      } else {
         throw new JSONException("can not cast to Date from " + obj.getClass());
      }
   }

   public static Object[] cast(Object obj, Type[] types) {
      if (obj == null) {
         return null;
      } else {
         Object[] array = new Object[types.length];
         if (obj instanceof Collection) {
            int i = 0;

            for (Object item : (Collection)obj) {
               int index = i++;
               array[index] = cast(item, types[index]);
            }
         } else {
            Class<?> objectClass = obj.getClass();
            if (!objectClass.isArray()) {
               throw new JSONException("can not cast to types " + JSON.toJSONString(types) + " from " + objectClass);
            }

            int length = Array.getLength(obj);

            for (int i = 0; i < array.length && i < length; i++) {
               Object item = Array.get(obj, i);
               array[i] = cast(item, types[i]);
            }
         }

         return array;
      }
   }

   public static String[] toStringArray(Object object) {
      if (object != null && !(object instanceof String[])) {
         if (object instanceof Collection) {
            Collection collection = (Collection)object;
            String[] array = new String[collection.size()];
            int i = 0;

            for (Object item : (Collection)object) {
               int index = i++;
               array[index] = item != null && !(item instanceof String) ? item.toString() : (String)item;
            }

            return array;
         } else {
            Class<?> objectClass = object.getClass();
            if (!objectClass.isArray()) {
               return cast(object, String[].class);
            } else {
               int length = Array.getLength(object);
               String[] array = new String[length];

               for (int i = 0; i < array.length; i++) {
                  Object item = Array.get(object, i);
                  array[i] = item != null && !(item instanceof String) ? item.toString() : (String)item;
               }

               return array;
            }
         }
      } else {
         return (String[])object;
      }
   }

   public static <T> T cast(Object obj, Type type) {
      return cast(obj, type, JSONFactory.getDefaultObjectReaderProvider());
   }

   public static <T> T cast(Object obj, Type type, ObjectReaderProvider provider) {
      if (type instanceof Class) {
         return cast(obj, (Class<T>)type, provider);
      } else if (obj instanceof Collection) {
         return (T)provider.getObjectReader(type).createInstance((Collection)obj);
      } else {
         return (T)(obj instanceof Map ? provider.getObjectReader(type).createInstance((Map)obj, 0L) : JSON.parseObject(JSON.toJSONString(obj), type));
      }
   }

   public static <T> T cast(Object obj, Class<T> targetClass) {
      return cast(obj, targetClass, JSONFactory.getDefaultObjectReaderProvider());
   }

   public static <T> T cast(Object obj, Class<T> targetClass, ObjectReaderProvider provider) {
      if (obj == null) {
         return null;
      } else if (targetClass.isInstance(obj)) {
         return (T)obj;
      } else if (targetClass == Date.class) {
         return (T)toDate(obj);
      } else if (targetClass == Instant.class) {
         return (T)toInstant(obj);
      } else if (targetClass == String.class) {
         return (T)(obj instanceof Character ? obj.toString() : JSON.toJSONString(obj));
      } else if (targetClass == AtomicInteger.class) {
         return (T)(new AtomicInteger(toIntValue(obj)));
      } else if (targetClass == AtomicLong.class) {
         return (T)(new AtomicLong(toLongValue(obj)));
      } else if (targetClass == AtomicBoolean.class) {
         return (T)(new AtomicBoolean((Boolean)obj));
      } else if (obj instanceof Map) {
         ObjectReader objectReader = provider.getObjectReader(targetClass);
         return (T)objectReader.createInstance((Map)obj, 0L);
      } else {
         Function typeConvert = provider.getTypeConvert(obj.getClass(), targetClass);
         if (typeConvert != null) {
            return (T)typeConvert.apply(obj);
         } else {
            if (targetClass.isEnum()) {
               ObjectReader objectReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(targetClass);
               if (!(objectReader instanceof ObjectReaderImplEnum)) {
                  JSONReader jsonReader = JSONReader.of(JSON.toJSONString(obj));
                  return (T)objectReader.readObject(jsonReader, targetClass, null, 0L);
               }

               if (obj instanceof Integer) {
                  int intValue = (Integer)obj;
                  return (T)((ObjectReaderImplEnum)objectReader).of(intValue);
               }
            }

            if (obj instanceof String) {
               String json = (String)obj;
               if (!json.isEmpty() && !"null".equals(json)) {
                  char first = json.trim().charAt(0);
                  JSONReader jsonReader;
                  if (first != '"' && first != '{' && first != '[') {
                     jsonReader = JSONReader.of(JSON.toJSONString(json));
                  } else {
                     jsonReader = JSONReader.of(json);
                  }

                  ObjectReader objectReaderx = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(targetClass);
                  return (T)objectReaderx.readObject(jsonReader, targetClass, null, 0L);
               } else {
                  return null;
               }
            } else if (obj instanceof Collection) {
               return (T)provider.getObjectReader(targetClass).createInstance((Collection)obj);
            } else {
               String className = targetClass.getName();
               if (obj instanceof Integer || obj instanceof Long) {
                  long millis = ((Number)obj).longValue();
                  switch (className) {
                     case "java.sql.Date":
                        return (T)JdbcSupport.createDate(millis);
                     case "java.sql.Timestamp":
                        return (T)JdbcSupport.createTimestamp(millis);
                     case "java.sql.Time":
                        return (T)JdbcSupport.createTime(millis);
                  }
               }

               String objClassName = obj.getClass().getName();
               if (objClassName.equals("org.bson.types.Decimal128") && targetClass == Double.class) {
                  ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(obj.getClass());
                  if (objectWriter instanceof ObjectWriterPrimitiveImpl) {
                     Function function = ((ObjectWriterPrimitiveImpl)objectWriter).getFunction();
                     if (function != null) {
                        Object apply = function.apply(obj);
                        Function DecimalTypeConvert = provider.getTypeConvert(apply.getClass(), targetClass);
                        if (DecimalTypeConvert != null) {
                           return (T)DecimalTypeConvert.apply(obj);
                        }
                     }
                  }
               }

               ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(obj.getClass());
               if (objectWriter instanceof ObjectWriterPrimitiveImpl) {
                  Function function = ((ObjectWriterPrimitiveImpl)objectWriter).getFunction();
                  if (function != null) {
                     Object apply = function.apply(obj);
                     if (targetClass.isInstance(apply)) {
                        return (T)apply;
                     }
                  }
               }

               throw new JSONException("can not cast to " + className + ", from " + obj.getClass());
            }
         }
      }
   }

   public static String getTypeName(Class type) {
      String mapTypeName = NAME_MAPPINGS.get(type);
      if (mapTypeName != null) {
         return mapTypeName;
      } else {
         if (Proxy.isProxyClass(type)) {
            Class[] interfaces = type.getInterfaces();
            if (interfaces.length > 0) {
               type = interfaces[0];
            }
         }

         String typeName = type.getTypeName();
         switch (typeName) {
            case "com.alibaba.fastjson.JSONObject":
               NAME_MAPPINGS.putIfAbsent(type, "JO1");
               return NAME_MAPPINGS.get(type);
            case "com.alibaba.fastjson.JSONArray":
               NAME_MAPPINGS.putIfAbsent(type, "JA1");
               return NAME_MAPPINGS.get(type);
            default:
               int index = typeName.indexOf(36);
               if (index != -1 && isInteger(typeName.substring(index + 1))) {
                  Class superclass = type.getSuperclass();
                  if (Map.class.isAssignableFrom(superclass)) {
                     return getTypeName(superclass);
                  }
               }

               return typeName;
         }
      }
   }

   public static Class getMapping(String typeName) {
      return TYPE_MAPPINGS.get(typeName);
   }

   public static BigDecimal toBigDecimal(Object value) {
      if (value == null || value instanceof BigDecimal) {
         return (BigDecimal)value;
      } else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
         return BigDecimal.valueOf(((Number)value).longValue());
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? new BigDecimal(str) : null;
      } else {
         return cast(value, BigDecimal.class);
      }
   }

   public static BigDecimal toBigDecimal(long i) {
      return BigDecimal.valueOf(i);
   }

   public static BigDecimal toBigDecimal(float f) {
      byte[] bytes = new byte[15];
      int size = DoubleToDecimal.toString(f, bytes, 0, true);
      return parseBigDecimal(bytes, 0, size);
   }

   public static BigDecimal toBigDecimal(double d) {
      byte[] bytes = new byte[24];
      int size = DoubleToDecimal.toString(d, bytes, 0, true);
      return parseBigDecimal(bytes, 0, size);
   }

   public static BigDecimal toBigDecimal(String str) {
      if (str != null && !str.isEmpty() && !"null".equals(str)) {
         if (JDKUtils.STRING_CODER != null) {
            int code = JDKUtils.STRING_CODER.applyAsInt(str);
            if (code == JDKUtils.LATIN1 && JDKUtils.STRING_VALUE != null) {
               byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
               return parseBigDecimal(bytes, 0, bytes.length);
            }
         }

         char[] chars = JDKUtils.getCharArray(str);
         return parseBigDecimal(chars, 0, chars.length);
      } else {
         return null;
      }
   }

   public static BigDecimal toBigDecimal(char[] chars) {
      return chars == null ? null : parseBigDecimal(chars, 0, chars.length);
   }

   public static BigDecimal toBigDecimal(byte[] strBytes) {
      return strBytes == null ? null : parseBigDecimal(strBytes, 0, strBytes.length);
   }

   public static boolean isInt32(BigInteger value) {
      if (JDKUtils.FIELD_BIGINTEGER_MAG_OFFSET != -1L) {
         int[] mag = (int[])JDKUtils.UNSAFE.getObject(value, JDKUtils.FIELD_BIGINTEGER_MAG_OFFSET);
         return mag.length == 0 || mag.length == 1 && (mag[0] >= 0 || mag[0] == Integer.MIN_VALUE && value.signum() == -1);
      } else {
         return value.compareTo(BIGINT_INT32_MIN) >= 0 && value.compareTo(BIGINT_INT32_MAX) <= 0;
      }
   }

   public static boolean isInt64(BigInteger value) {
      if (JDKUtils.FIELD_BIGINTEGER_MAG_OFFSET != -1L) {
         int[] mag = (int[])JDKUtils.UNSAFE.getObject(value, JDKUtils.FIELD_BIGINTEGER_MAG_OFFSET);
         if (mag.length <= 1) {
            return true;
         }

         if (mag.length == 2) {
            int mag0 = mag[0];
            return mag[0] >= 0 || mag0 == Integer.MIN_VALUE && mag[1] == 0 && value.signum() == -1;
         }
      }

      return value.compareTo(BIGINT_INT64_MIN) >= 0 && value.compareTo(BIGINT_INT64_MAX) <= 0;
   }

   public static boolean isInteger(BigDecimal decimal) {
      int scale = decimal.scale();
      if (scale == 0) {
         return true;
      } else {
         int precision = decimal.precision();
         if (precision < 20 && JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET != -1L) {
            long intCompact = JDKUtils.UNSAFE.getLong(decimal, JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET);
            switch (scale) {
               case 1:
                  return intCompact % 10L == 0L;
               case 2:
                  return intCompact % 100L == 0L;
               case 3:
                  return intCompact % 1000L == 0L;
               case 4:
                  return intCompact % 10000L == 0L;
               case 5:
                  return intCompact % 100000L == 0L;
               case 6:
                  return intCompact % 1000000L == 0L;
               case 7:
                  return intCompact % 10000000L == 0L;
               case 8:
                  return intCompact % 100000000L == 0L;
               case 9:
                  return intCompact % 1000000000L == 0L;
            }
         }

         return decimal.stripTrailingZeros().scale() == 0;
      }
   }

   public static BigInteger toBigInteger(Object value) {
      if (value == null || value instanceof BigInteger) {
         return (BigInteger)value;
      } else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
         return BigInteger.valueOf(((Number)value).longValue());
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? new BigInteger(str) : null;
      } else {
         throw new JSONException("can not cast to bigint");
      }
   }

   public static Long toLong(Object value) {
      if (value != null && !(value instanceof Long)) {
         if (value instanceof String) {
            String str = (String)value;
            if (str.isEmpty() || "null".equals(str)) {
               return null;
            }
         }

         return toLongValue(value);
      } else {
         return (Long)value;
      }
   }

   public static long toLongValue(Object value) {
      if (value == null) {
         return 0L;
      } else if (value instanceof Long) {
         return (Long)value;
      } else if (value instanceof Number) {
         return ((Number)value).longValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equals(str)) {
            try {
               int lastCommaIndex = str.lastIndexOf(44);
               if (lastCommaIndex == str.length() - 4 && str.indexOf(46) == -1) {
                  return NumberFormat.getNumberInstance().parse(str).longValue();
               }
            } catch (ParseException var3) {
            }

            if (IOUtils.isNumber(str)) {
               return Long.parseLong(str);
            } else {
               throw new JSONException("parseLong error " + str);
            }
         } else {
            return 0L;
         }
      } else {
         throw new JSONException("can not cast to long from " + value.getClass());
      }
   }

   public static Boolean parseBoolean(byte[] bytes, int off, int len) {
      switch (len) {
         case 0:
            return null;
         case 1:
            byte b0 = bytes[off];
            if (b0 == 49 || b0 == 89) {
               return Boolean.TRUE;
            }

            if (b0 == 48 || b0 == 78) {
               return Boolean.FALSE;
            }
         case 2:
         case 3:
         default:
            break;
         case 4:
            if (bytes[off] == 116 && bytes[off + 1] == 114 && bytes[off + 2] == 117 && bytes[off + 3] == 101) {
               return Boolean.TRUE;
            }
            break;
         case 5:
            if (bytes[off] == 102 && bytes[off + 1] == 97 && bytes[off + 2] == 108 && bytes[off + 3] == 115 && bytes[off + 4] == 101) {
               return Boolean.FALSE;
            }
      }

      String str = new String(bytes, off, len);
      return Boolean.parseBoolean(str);
   }

   public static Boolean parseBoolean(char[] bytes, int off, int len) {
      switch (len) {
         case 0:
            return null;
         case 1:
            char b0 = bytes[off];
            if (b0 == '1' || b0 == 'Y') {
               return Boolean.TRUE;
            }

            if (b0 == '0' || b0 == 'N') {
               return Boolean.FALSE;
            }
         case 2:
         case 3:
         default:
            break;
         case 4:
            if (bytes[off] == 't' && bytes[off + 1] == 'r' && bytes[off + 2] == 'u' && bytes[off + 3] == 'e') {
               return Boolean.TRUE;
            }
            break;
         case 5:
            if (bytes[off] == 'f' && bytes[off + 1] == 'a' && bytes[off + 2] == 'l' && bytes[off + 3] == 's' && bytes[off + 4] == 'e') {
               return Boolean.FALSE;
            }
      }

      String str = new String(bytes, off, len);
      return Boolean.parseBoolean(str);
   }

   public static int parseInt(byte[] bytes, int off, int len) {
      switch (len) {
         case 1:
            byte b0xxxxxxx = bytes[off];
            if (b0xxxxxxx >= 48 && b0xxxxxxx <= 57) {
               return b0xxxxxxx - 48;
            }
            break;
         case 2:
            byte b0xxxxxx = bytes[off];
            byte b1xxxxxx = bytes[off + 1];
            if (b0xxxxxx >= 48 && b0xxxxxx <= 57 && b1xxxxxx >= 48 && b1xxxxxx <= 57) {
               return (b0xxxxxx - 48) * 10 + (b1xxxxxx - 48);
            }
            break;
         case 3:
            byte b0xxxxx = bytes[off];
            byte b1xxxxx = bytes[off + 1];
            byte b2xxxxx = bytes[off + 2];
            if (b0xxxxx >= 48 && b0xxxxx <= 57 && b1xxxxx >= 48 && b1xxxxx <= 57 && b2xxxxx >= 48 && b2xxxxx <= 57) {
               return (b0xxxxx - 48) * 100 + (b1xxxxx - 48) * 10 + (b2xxxxx - 48);
            }
            break;
         case 4:
            byte b0xxxx = bytes[off];
            byte b1xxxx = bytes[off + 1];
            byte b2xxxx = bytes[off + 2];
            byte b3xxxx = bytes[off + 3];
            if (b0xxxx >= 48 && b0xxxx <= 57 && b1xxxx >= 48 && b1xxxx <= 57 && b2xxxx >= 48 && b2xxxx <= 57 && b3xxxx >= 48 && b3xxxx <= 57) {
               return (b0xxxx - 48) * 1000 + (b1xxxx - 48) * 100 + (b2xxxx - 48) * 10 + (b3xxxx - 48);
            }
            break;
         case 5:
            byte b0xxx = bytes[off];
            byte b1xxx = bytes[off + 1];
            byte b2xxx = bytes[off + 2];
            byte b3xxx = bytes[off + 3];
            byte b4xxx = bytes[off + 4];
            if (b0xxx >= 48
               && b0xxx <= 57
               && b1xxx >= 48
               && b1xxx <= 57
               && b2xxx >= 48
               && b2xxx <= 57
               && b3xxx >= 48
               && b3xxx <= 57
               && b4xxx >= 48
               && b4xxx <= 57) {
               return (b0xxx - 48) * 10000 + (b1xxx - 48) * 1000 + (b2xxx - 48) * 100 + (b3xxx - 48) * 10 + (b4xxx - 48);
            }
            break;
         case 6:
            byte b0xx = bytes[off];
            byte b1xx = bytes[off + 1];
            byte b2xx = bytes[off + 2];
            byte b3xx = bytes[off + 3];
            byte b4xx = bytes[off + 4];
            byte b5xx = bytes[off + 5];
            if (b0xx >= 48
               && b0xx <= 57
               && b1xx >= 48
               && b1xx <= 57
               && b2xx >= 48
               && b2xx <= 57
               && b3xx >= 48
               && b3xx <= 57
               && b4xx >= 48
               && b4xx <= 57
               && b5xx >= 48
               && b5xx <= 57) {
               return (b0xx - 48) * 100000 + (b1xx - 48) * 10000 + (b2xx - 48) * 1000 + (b3xx - 48) * 100 + (b4xx - 48) * 10 + (b5xx - 48);
            }
            break;
         case 7:
            byte b0x = bytes[off];
            byte b1x = bytes[off + 1];
            byte b2x = bytes[off + 2];
            byte b3x = bytes[off + 3];
            byte b4x = bytes[off + 4];
            byte b5x = bytes[off + 5];
            byte b6x = bytes[off + 6];
            if (b0x >= 48
               && b0x <= 57
               && b1x >= 48
               && b1x <= 57
               && b2x >= 48
               && b2x <= 57
               && b3x >= 48
               && b3x <= 57
               && b4x >= 48
               && b4x <= 57
               && b5x >= 48
               && b5x <= 57
               && b6x >= 48
               && b6x <= 57) {
               return (b0x - 48) * 1000000 + (b1x - 48) * 100000 + (b2x - 48) * 10000 + (b3x - 48) * 1000 + (b4x - 48) * 100 + (b5x - 48) * 10 + (b6x - 48);
            }
            break;
         case 8:
            byte b0 = bytes[off];
            byte b1 = bytes[off + 1];
            byte b2 = bytes[off + 2];
            byte b3 = bytes[off + 3];
            byte b4 = bytes[off + 4];
            byte b5 = bytes[off + 5];
            byte b6 = bytes[off + 6];
            byte b7 = bytes[off + 7];
            if (b0 >= 48
               && b0 <= 57
               && b1 >= 48
               && b1 <= 57
               && b2 >= 48
               && b2 <= 57
               && b3 >= 48
               && b3 <= 57
               && b4 >= 48
               && b4 <= 57
               && b5 >= 48
               && b5 <= 57
               && b6 >= 48
               && b6 <= 57
               && b7 >= 48
               && b7 <= 57) {
               return (b0 - 48) * 10000000
                  + (b1 - 48) * 1000000
                  + (b2 - 48) * 100000
                  + (b3 - 48) * 10000
                  + (b4 - 48) * 1000
                  + (b5 - 48) * 100
                  + (b6 - 48) * 10
                  + (b7 - 48);
            }
      }

      String str = new String(bytes, off, len);
      return Integer.parseInt(str);
   }

   public static int parseInt(char[] bytes, int off, int len) {
      switch (len) {
         case 1:
            char b0xxxxxxx = bytes[off];
            if (b0xxxxxxx >= '0' && b0xxxxxxx <= '9') {
               return b0xxxxxxx - 48;
            }
            break;
         case 2:
            char b0xxxxxx = bytes[off];
            char b1xxxxxx = bytes[off + 1];
            if (b0xxxxxx >= '0' && b0xxxxxx <= '9' && b1xxxxxx >= '0' && b1xxxxxx <= '9') {
               return (b0xxxxxx - 48) * 10 + (b1xxxxxx - 48);
            }
            break;
         case 3:
            char b0xxxxx = bytes[off];
            char b1xxxxx = bytes[off + 1];
            char b2xxxxx = bytes[off + 2];
            if (b0xxxxx >= '0' && b0xxxxx <= '9' && b1xxxxx >= '0' && b1xxxxx <= '9' && b2xxxxx >= '0' && b2xxxxx <= '9') {
               return (b0xxxxx - 48) * 100 + (b1xxxxx - 48) * 10 + (b2xxxxx - 48);
            }
            break;
         case 4:
            char b0xxxx = bytes[off];
            char b1xxxx = bytes[off + 1];
            char b2xxxx = bytes[off + 2];
            char b3xxxx = bytes[off + 3];
            if (b0xxxx >= '0' && b0xxxx <= '9' && b1xxxx >= '0' && b1xxxx <= '9' && b2xxxx >= '0' && b2xxxx <= '9' && b3xxxx >= '0' && b3xxxx <= '9') {
               return (b0xxxx - 48) * 1000 + (b1xxxx - 48) * 100 + (b2xxxx - 48) * 10 + (b3xxxx - 48);
            }
            break;
         case 5:
            char b0xxx = bytes[off];
            char b1xxx = bytes[off + 1];
            char b2xxx = bytes[off + 2];
            char b3xxx = bytes[off + 3];
            char b4xxx = bytes[off + 4];
            if (b0xxx >= '0'
               && b0xxx <= '9'
               && b1xxx >= '0'
               && b1xxx <= '9'
               && b2xxx >= '0'
               && b2xxx <= '9'
               && b3xxx >= '0'
               && b3xxx <= '9'
               && b4xxx >= '0'
               && b4xxx <= '9') {
               return (b0xxx - 48) * 10000 + (b1xxx - 48) * 1000 + (b2xxx - 48) * 100 + (b3xxx - 48) * 10 + (b4xxx - 48);
            }
            break;
         case 6:
            char b0xx = bytes[off];
            char b1xx = bytes[off + 1];
            char b2xx = bytes[off + 2];
            char b3xx = bytes[off + 3];
            char b4xx = bytes[off + 4];
            char b5xx = bytes[off + 5];
            if (b0xx >= '0'
               && b0xx <= '9'
               && b1xx >= '0'
               && b1xx <= '9'
               && b2xx >= '0'
               && b2xx <= '9'
               && b3xx >= '0'
               && b3xx <= '9'
               && b4xx >= '0'
               && b4xx <= '9'
               && b5xx >= '0'
               && b5xx <= '9') {
               return (b0xx - 48) * 100000 + (b1xx - 48) * 10000 + (b2xx - 48) * 1000 + (b3xx - 48) * 100 + (b4xx - 48) * 10 + (b5xx - 48);
            }
            break;
         case 7:
            char b0x = bytes[off];
            char b1x = bytes[off + 1];
            char b2x = bytes[off + 2];
            char b3x = bytes[off + 3];
            char b4x = bytes[off + 4];
            char b5x = bytes[off + 5];
            char b6x = bytes[off + 6];
            if (b0x >= '0'
               && b0x <= '9'
               && b1x >= '0'
               && b1x <= '9'
               && b2x >= '0'
               && b2x <= '9'
               && b3x >= '0'
               && b3x <= '9'
               && b4x >= '0'
               && b4x <= '9'
               && b5x >= '0'
               && b5x <= '9'
               && b6x >= '0'
               && b6x <= '9') {
               return (b0x - 48) * 1000000 + (b1x - 48) * 100000 + (b2x - 48) * 10000 + (b3x - 48) * 1000 + (b4x - 48) * 100 + (b5x - 48) * 10 + (b6x - 48);
            }
            break;
         case 8:
            char b0 = bytes[off];
            char b1 = bytes[off + 1];
            char b2 = bytes[off + 2];
            char b3 = bytes[off + 3];
            char b4 = bytes[off + 4];
            char b5 = bytes[off + 5];
            char b6 = bytes[off + 6];
            char b7 = bytes[off + 7];
            if (b0 >= '0'
               && b0 <= '9'
               && b1 >= '0'
               && b1 <= '9'
               && b2 >= '0'
               && b2 <= '9'
               && b3 >= '0'
               && b3 <= '9'
               && b4 >= '0'
               && b4 <= '9'
               && b5 >= '0'
               && b5 <= '9'
               && b6 >= '0'
               && b6 <= '9'
               && b7 >= '0'
               && b7 <= '9') {
               return (b0 - 48) * 10000000
                  + (b1 - 48) * 1000000
                  + (b2 - 48) * 100000
                  + (b3 - 48) * 10000
                  + (b4 - 48) * 1000
                  + (b5 - 48) * 100
                  + (b6 - 48) * 10
                  + (b7 - 48);
            }
      }

      String str = new String(bytes, off, len);
      return Integer.parseInt(str);
   }

   public static long parseLong(byte[] bytes, int off, int len) {
      switch (len) {
         case 1:
            byte b0xxxxxxx = bytes[off];
            if (b0xxxxxxx >= 48 && b0xxxxxxx <= 57) {
               return (long)(b0xxxxxxx - 48);
            }
            break;
         case 2:
            byte b0xxxxxx = bytes[off];
            byte b1xxxxxx = bytes[off + 1];
            if (b0xxxxxx >= 48 && b0xxxxxx <= 57 && b1xxxxxx >= 48 && b1xxxxxx <= 57) {
               return (long)(b0xxxxxx - 48) * 10L + (long)(b1xxxxxx - 48);
            }
            break;
         case 3:
            byte b0xxxxx = bytes[off];
            byte b1xxxxx = bytes[off + 1];
            byte b2xxxxx = bytes[off + 2];
            if (b0xxxxx >= 48 && b0xxxxx <= 57 && b1xxxxx >= 48 && b1xxxxx <= 57 && b2xxxxx >= 48 && b2xxxxx <= 57) {
               return (long)(b0xxxxx - 48) * 100L + (long)((b1xxxxx - 48) * 10) + (long)(b2xxxxx - 48);
            }
            break;
         case 4:
            byte b0xxxx = bytes[off];
            byte b1xxxx = bytes[off + 1];
            byte b2xxxx = bytes[off + 2];
            byte b3xxxx = bytes[off + 3];
            if (b0xxxx >= 48 && b0xxxx <= 57 && b1xxxx >= 48 && b1xxxx <= 57 && b2xxxx >= 48 && b2xxxx <= 57 && b3xxxx >= 48 && b3xxxx <= 57) {
               return (long)(b0xxxx - 48) * 1000L + (long)((b1xxxx - 48) * 100) + (long)((b2xxxx - 48) * 10) + (long)(b3xxxx - 48);
            }
            break;
         case 5:
            byte b0xxx = bytes[off];
            byte b1xxx = bytes[off + 1];
            byte b2xxx = bytes[off + 2];
            byte b3xxx = bytes[off + 3];
            byte b4xxx = bytes[off + 4];
            if (b0xxx >= 48
               && b0xxx <= 57
               && b1xxx >= 48
               && b1xxx <= 57
               && b2xxx >= 48
               && b2xxx <= 57
               && b3xxx >= 48
               && b3xxx <= 57
               && b4xxx >= 48
               && b4xxx <= 57) {
               return (long)(b0xxx - 48) * 10000L + (long)((b1xxx - 48) * 1000) + (long)((b2xxx - 48) * 100) + (long)((b3xxx - 48) * 10) + (long)(b4xxx - 48);
            }
            break;
         case 6:
            byte b0xx = bytes[off];
            byte b1xx = bytes[off + 1];
            byte b2xx = bytes[off + 2];
            byte b3xx = bytes[off + 3];
            byte b4xx = bytes[off + 4];
            byte b5xx = bytes[off + 5];
            if (b0xx >= 48
               && b0xx <= 57
               && b1xx >= 48
               && b1xx <= 57
               && b2xx >= 48
               && b2xx <= 57
               && b3xx >= 48
               && b3xx <= 57
               && b4xx >= 48
               && b4xx <= 57
               && b5xx >= 48
               && b5xx <= 57) {
               return (long)(b0xx - 48) * 100000L
                  + (long)((b1xx - 48) * 10000)
                  + (long)((b2xx - 48) * 1000)
                  + (long)((b3xx - 48) * 100)
                  + (long)((b4xx - 48) * 10)
                  + (long)(b5xx - 48);
            }
            break;
         case 7:
            byte b0x = bytes[off];
            byte b1x = bytes[off + 1];
            byte b2x = bytes[off + 2];
            byte b3x = bytes[off + 3];
            byte b4x = bytes[off + 4];
            byte b5x = bytes[off + 5];
            byte b6x = bytes[off + 6];
            if (b0x >= 48
               && b0x <= 57
               && b1x >= 48
               && b1x <= 57
               && b2x >= 48
               && b2x <= 57
               && b3x >= 48
               && b3x <= 57
               && b4x >= 48
               && b4x <= 57
               && b5x >= 48
               && b5x <= 57
               && b6x >= 48
               && b6x <= 57) {
               return (long)(b0x - 48) * 1000000L
                  + (long)((b1x - 48) * 100000)
                  + (long)((b2x - 48) * 10000)
                  + (long)((b3x - 48) * 1000)
                  + (long)((b4x - 48) * 100)
                  + (long)((b5x - 48) * 10)
                  + (long)(b6x - 48);
            }
            break;
         case 8:
            byte b0 = bytes[off];
            byte b1 = bytes[off + 1];
            byte b2 = bytes[off + 2];
            byte b3 = bytes[off + 3];
            byte b4 = bytes[off + 4];
            byte b5 = bytes[off + 5];
            byte b6 = bytes[off + 6];
            byte b7 = bytes[off + 7];
            if (b0 >= 48
               && b0 <= 57
               && b1 >= 48
               && b1 <= 57
               && b2 >= 48
               && b2 <= 57
               && b3 >= 48
               && b3 <= 57
               && b4 >= 48
               && b4 <= 57
               && b5 >= 48
               && b5 <= 57
               && b6 >= 48
               && b6 <= 57
               && b7 >= 48
               && b7 <= 57) {
               return (long)(b0 - 48) * 10000000L
                  + (long)((b1 - 48) * 1000000)
                  + (long)((b2 - 48) * 100000)
                  + (long)((b3 - 48) * 10000)
                  + (long)((b4 - 48) * 1000)
                  + (long)((b5 - 48) * 100)
                  + (long)((b6 - 48) * 10)
                  + (long)(b7 - 48);
            }
      }

      String str = new String(bytes, off, len);
      return Long.parseLong(str);
   }

   public static long parseLong(char[] bytes, int off, int len) {
      switch (len) {
         case 1:
            char b0xxxxxxx = bytes[off];
            if (b0xxxxxxx >= '0' && b0xxxxxxx <= '9') {
               return (long)(b0xxxxxxx - '0');
            }
            break;
         case 2:
            char b0xxxxxx = bytes[off];
            char b1xxxxxx = bytes[off + 1];
            if (b0xxxxxx >= '0' && b0xxxxxx <= '9' && b1xxxxxx >= '0' && b1xxxxxx <= '9') {
               return (long)(b0xxxxxx - '0') * 10L + (long)(b1xxxxxx - '0');
            }
            break;
         case 3:
            char b0xxxxx = bytes[off];
            char b1xxxxx = bytes[off + 1];
            char b2xxxxx = bytes[off + 2];
            if (b0xxxxx >= '0' && b0xxxxx <= '9' && b1xxxxx >= '0' && b1xxxxx <= '9' && b2xxxxx >= '0' && b2xxxxx <= '9') {
               return (long)(b0xxxxx - '0') * 100L + (long)((b1xxxxx - '0') * 10) + (long)(b2xxxxx - '0');
            }
            break;
         case 4:
            char b0xxxx = bytes[off];
            char b1xxxx = bytes[off + 1];
            char b2xxxx = bytes[off + 2];
            char b3xxxx = bytes[off + 3];
            if (b0xxxx >= '0' && b0xxxx <= '9' && b1xxxx >= '0' && b1xxxx <= '9' && b2xxxx >= '0' && b2xxxx <= '9' && b3xxxx >= '0' && b3xxxx <= '9') {
               return (long)(b0xxxx - '0') * 1000L + (long)((b1xxxx - '0') * 100) + (long)((b2xxxx - '0') * 10) + (long)(b3xxxx - '0');
            }
            break;
         case 5:
            char b0xxx = bytes[off];
            char b1xxx = bytes[off + 1];
            char b2xxx = bytes[off + 2];
            char b3xxx = bytes[off + 3];
            char b4xxx = bytes[off + 4];
            if (b0xxx >= '0'
               && b0xxx <= '9'
               && b1xxx >= '0'
               && b1xxx <= '9'
               && b2xxx >= '0'
               && b2xxx <= '9'
               && b3xxx >= '0'
               && b3xxx <= '9'
               && b4xxx >= '0'
               && b4xxx <= '9') {
               return (long)(b0xxx - '0') * 10000L
                  + (long)((b1xxx - '0') * 1000)
                  + (long)((b2xxx - '0') * 100)
                  + (long)((b3xxx - '0') * 10)
                  + (long)(b4xxx - '0');
            }
            break;
         case 6:
            char b0xx = bytes[off];
            char b1xx = bytes[off + 1];
            char b2xx = bytes[off + 2];
            char b3xx = bytes[off + 3];
            char b4xx = bytes[off + 4];
            char b5xx = bytes[off + 5];
            if (b0xx >= '0'
               && b0xx <= '9'
               && b1xx >= '0'
               && b1xx <= '9'
               && b2xx >= '0'
               && b2xx <= '9'
               && b3xx >= '0'
               && b3xx <= '9'
               && b4xx >= '0'
               && b4xx <= '9'
               && b5xx >= '0'
               && b5xx <= '9') {
               return (long)(b0xx - '0') * 100000L
                  + (long)((b1xx - '0') * 10000)
                  + (long)((b2xx - '0') * 1000)
                  + (long)((b3xx - '0') * 100)
                  + (long)((b4xx - '0') * 10)
                  + (long)(b5xx - '0');
            }
            break;
         case 7:
            char b0x = bytes[off];
            char b1x = bytes[off + 1];
            char b2x = bytes[off + 2];
            char b3x = bytes[off + 3];
            char b4x = bytes[off + 4];
            char b5x = bytes[off + 5];
            char b6x = bytes[off + 6];
            if (b0x >= '0'
               && b0x <= '9'
               && b1x >= '0'
               && b1x <= '9'
               && b2x >= '0'
               && b2x <= '9'
               && b3x >= '0'
               && b3x <= '9'
               && b4x >= '0'
               && b4x <= '9'
               && b5x >= '0'
               && b5x <= '9'
               && b6x >= '0'
               && b6x <= '9') {
               return (long)(b0x - '0') * 1000000L
                  + (long)((b1x - '0') * 100000)
                  + (long)((b2x - '0') * 10000)
                  + (long)((b3x - '0') * 1000)
                  + (long)((b4x - '0') * 100)
                  + (long)((b5x - '0') * 10)
                  + (long)(b6x - '0');
            }
            break;
         case 8:
            char b0 = bytes[off];
            char b1 = bytes[off + 1];
            char b2 = bytes[off + 2];
            char b3 = bytes[off + 3];
            char b4 = bytes[off + 4];
            char b5 = bytes[off + 5];
            char b6 = bytes[off + 6];
            char b7 = bytes[off + 7];
            if (b0 >= '0'
               && b0 <= '9'
               && b1 >= '0'
               && b1 <= '9'
               && b2 >= '0'
               && b2 <= '9'
               && b3 >= '0'
               && b3 <= '9'
               && b4 >= '0'
               && b4 <= '9'
               && b5 >= '0'
               && b5 <= '9'
               && b6 >= '0'
               && b6 <= '9'
               && b7 >= '0'
               && b7 <= '9') {
               return (long)(b0 - '0') * 10000000L
                  + (long)((b1 - '0') * 1000000)
                  + (long)((b2 - '0') * 100000)
                  + (long)((b3 - '0') * 10000)
                  + (long)((b4 - '0') * 1000)
                  + (long)((b5 - '0') * 100)
                  + (long)((b6 - '0') * 10)
                  + (long)(b7 - '0');
            }
      }

      String str = new String(bytes, off, len);
      return Long.parseLong(str);
   }

   public static BigDecimal parseBigDecimal(char[] bytes, int off, int len) {
      if (bytes != null && len != 0) {
         boolean negative = false;
         int j = off;
         if (bytes[off] == '-') {
            negative = true;
            j = off + 1;
         }

         if (len <= 20 || negative && len == 21) {
            int end = off + len;
            int dot = 0;
            int dotIndex = -1;

            long unscaleValue;
            for (unscaleValue = 0L; j < end; j++) {
               char b = bytes[j];
               if (b == '.') {
                  if (++dot > 1) {
                     break;
                  }

                  dotIndex = j;
               } else {
                  if (b < '0' || b > '9') {
                     unscaleValue = -1L;
                     break;
                  }

                  long r = unscaleValue * 10L;
                  if ((unscaleValue | 10L) >>> 31 != 0L && r / 10L != unscaleValue) {
                     unscaleValue = -1L;
                     break;
                  }

                  unscaleValue = r + (long)(b - '0');
               }
            }

            int scale = 0;
            if (unscaleValue >= 0L && dot <= 1) {
               if (negative) {
                  unscaleValue = -unscaleValue;
               }

               if (dotIndex != -1) {
                  scale = len - (dotIndex - off) - 1;
               }

               return BigDecimal.valueOf(unscaleValue, scale);
            }
         }

         return new BigDecimal(bytes, off, len);
      } else {
         return null;
      }
   }

   public static BigDecimal parseBigDecimal(byte[] bytes, int off, int len) {
      if (bytes != null && len != 0) {
         boolean negative = false;
         int j = off;
         if (bytes[off] == 45) {
            negative = true;
            j = off + 1;
         }

         if (len <= 20 || negative && len == 21) {
            int end = off + len;
            int dot = 0;
            int dotIndex = -1;

            long unscaleValue;
            for (unscaleValue = 0L; j < end; j++) {
               byte b = bytes[j];
               if (b == 46) {
                  if (++dot > 1) {
                     break;
                  }

                  dotIndex = j;
               } else {
                  if (b < 48 || b > 57) {
                     unscaleValue = -1L;
                     break;
                  }

                  long r = unscaleValue * 10L;
                  if ((unscaleValue | 10L) >>> 31 != 0L && r / 10L != unscaleValue) {
                     unscaleValue = -1L;
                     break;
                  }

                  unscaleValue = r + (long)(b - 48);
               }
            }

            int scale = 0;
            if (unscaleValue >= 0L && dot <= 1) {
               if (negative) {
                  unscaleValue = -unscaleValue;
               }

               if (dotIndex != -1) {
                  scale = len - (dotIndex - off) - 1;
               }

               return BigDecimal.valueOf(unscaleValue, scale);
            }
         }

         char[] chars;
         if (off == 0 && len == bytes.length) {
            chars = TypeUtils.X1.TO_CHARS.apply(bytes);
         } else {
            chars = new char[len];

            for (int i = 0; i < len; i++) {
               chars[i] = (char)bytes[off + i];
            }
         }

         return new BigDecimal(chars, 0, chars.length);
      } else {
         return null;
      }
   }

   public static Integer toInteger(Object value) {
      if (value == null || value instanceof Integer) {
         return (Integer)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Integer.parseInt(str) : null;
      } else if (value instanceof Map && ((Map)value).isEmpty()) {
         return null;
      } else if (value instanceof Boolean) {
         return (Boolean)value ? 1 : 0;
      } else {
         throw new JSONException("can not cast to integer");
      }
   }

   public static Byte toByte(Object value) {
      if (value == null || value instanceof Byte) {
         return (Byte)value;
      } else if (value instanceof Number) {
         return ((Number)value).byteValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Byte.parseByte(str) : null;
      } else {
         throw new JSONException("can not cast to byte");
      }
   }

   public static byte toByteValue(Object value) {
      if (value == null) {
         return 0;
      } else if (value instanceof Byte) {
         return (Byte)value;
      } else if (value instanceof Number) {
         return ((Number)value).byteValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Byte.parseByte(str) : 0;
      } else {
         throw new JSONException("can not cast to byte");
      }
   }

   public static Short toShort(Object value) {
      if (value == null || value instanceof Short) {
         return (Short)value;
      } else if (value instanceof Number) {
         return ((Number)value).shortValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Short.parseShort(str) : null;
      } else {
         throw new JSONException("can not cast to byte");
      }
   }

   public static short toShortValue(Object value) {
      if (value == null) {
         return 0;
      } else if (value instanceof Short) {
         return (Short)value;
      } else if (value instanceof Number) {
         return (short)((byte)((Number)value).shortValue());
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Short.parseShort(str) : 0;
      } else {
         throw new JSONException("can not cast to byte");
      }
   }

   public static int toIntValue(Object value) {
      if (value == null) {
         return 0;
      } else if (value instanceof Integer) {
         return (Integer)value;
      } else if (value instanceof Number) {
         return ((Number)value).intValue();
      } else if (value instanceof String) {
         String str = (String)value;
         if (!str.isEmpty() && !"null".equals(str)) {
            try {
               int lastCommaIndex = str.lastIndexOf(44);
               if (lastCommaIndex == str.length() - 4 && str.indexOf(46) == -1) {
                  return NumberFormat.getNumberInstance().parse(str).intValue();
               }
            } catch (ParseException var3) {
            }

            if (IOUtils.isNumber(str)) {
               return Integer.parseInt(str);
            } else {
               throw new JSONException("parseInt error, " + str);
            }
         } else {
            return 0;
         }
      } else {
         throw new JSONException("can not cast to int");
      }
   }

   public static boolean toBooleanValue(Object value) {
      if (value == null) {
         return false;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Boolean.parseBoolean(str) : false;
      } else {
         if (value instanceof Number) {
            int intValue = ((Number)value).intValue();
            if (intValue == 1) {
               return true;
            }

            if (intValue == 0) {
               return false;
            }
         }

         throw new JSONException("can not cast to boolean");
      }
   }

   public static Boolean toBoolean(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Boolean.parseBoolean(str) : null;
      } else {
         if (value instanceof Number) {
            int intValue = ((Number)value).intValue();
            if (intValue == 1) {
               return true;
            }

            if (intValue == 0) {
               return false;
            }
         }

         throw new JSONException("can not cast to boolean");
      }
   }

   public static float toFloatValue(Object value) {
      if (value == null) {
         return 0.0F;
      } else if (value instanceof Float) {
         return (Float)value;
      } else if (value instanceof Number) {
         return ((Number)value).floatValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Float.parseFloat(str) : 0.0F;
      } else {
         throw new JSONException("can not cast to decimal");
      }
   }

   public static Float toFloat(Object value) {
      if (value == null || value instanceof Float) {
         return (Float)value;
      } else if (value instanceof Number) {
         return ((Number)value).floatValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Float.parseFloat(str) : null;
      } else {
         throw new JSONException("can not cast to decimal");
      }
   }

   public static double toDoubleValue(Object value) {
      if (value == null) {
         return 0.0;
      } else if (value instanceof Double) {
         return (Double)value;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Double.parseDouble(str) : 0.0;
      } else {
         throw new JSONException("can not cast to decimal");
      }
   }

   public static Double toDouble(Object value) {
      if (value == null || value instanceof Double) {
         return (Double)value;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue();
      } else if (value instanceof String) {
         String str = (String)value;
         return !str.isEmpty() && !"null".equals(str) ? Double.parseDouble(str) : null;
      } else {
         throw new JSONException("can not cast to decimal");
      }
   }

   public static int compare(Object a, Object b) {
      Class typeA = a.getClass();
      Class typeB = b.getClass();
      if (typeA == typeB) {
         return ((Comparable)a).compareTo(b);
      } else {
         if (typeA == BigDecimal.class) {
            if (typeB == Integer.class || typeB == Long.class) {
               b = BigDecimal.valueOf(((Number)b).longValue());
            } else if (typeB == Float.class || typeB == Double.class) {
               b = BigDecimal.valueOf(((Number)b).doubleValue());
            } else if (typeB == BigInteger.class) {
               b = new BigDecimal((BigInteger)b);
            }
         } else if (typeA == BigInteger.class) {
            if (typeB == Integer.class || typeB == Long.class) {
               b = BigInteger.valueOf(((Number)b).longValue());
            } else if (typeB == Float.class || typeB == Double.class) {
               b = BigDecimal.valueOf(((Number)b).doubleValue());
               a = new BigDecimal((BigInteger)a);
            } else if (typeB == BigDecimal.class) {
               a = new BigDecimal((BigInteger)a);
            }
         } else if (typeA == Long.class) {
            if (typeB == Integer.class) {
               return Long.compare((Long)a, (long)((Integer)b).intValue());
            }

            if (typeB == BigDecimal.class) {
               a = BigDecimal.valueOf((Long)a);
            } else {
               if (typeB == Float.class || typeB == Double.class) {
                  return Double.compare((double)((Long)a).longValue(), ((Number)b).doubleValue());
               }

               if (typeB == BigInteger.class) {
                  a = BigInteger.valueOf((Long)a);
               } else if (typeB == String.class) {
                  a = BigDecimal.valueOf((Long)a);
                  b = new BigDecimal((String)b);
               }
            }
         } else if (typeA == Integer.class) {
            if (typeB == Long.class) {
               return Long.compare((long)((Integer)a).intValue(), (Long)b);
            }

            if (typeB == BigDecimal.class) {
               a = BigDecimal.valueOf((long)((Integer)a).intValue());
            } else if (typeB == BigInteger.class) {
               a = BigInteger.valueOf((long)((Integer)a).intValue());
            } else {
               if (typeB == Float.class || typeB == Double.class) {
                  return Double.compare((double)((Integer)a).intValue(), ((Number)b).doubleValue());
               }

               if (typeB == String.class) {
                  a = BigDecimal.valueOf((long)((Integer)a).intValue());
                  b = new BigDecimal((String)b);
               }
            }
         } else if (typeA == Double.class) {
            if (typeB == Integer.class || typeB == Long.class || typeB == Float.class) {
               return Double.compare((Double)a, ((Number)b).doubleValue());
            }

            if (typeB == BigDecimal.class) {
               a = BigDecimal.valueOf((Double)a);
            } else if (typeB == String.class) {
               a = BigDecimal.valueOf((Double)a);
               b = new BigDecimal((String)b);
            } else if (typeB == BigInteger.class) {
               a = BigDecimal.valueOf((Double)a);
               b = new BigDecimal((BigInteger)b);
            }
         } else if (typeA == Float.class) {
            if (typeB == Integer.class || typeB == Long.class || typeB == Double.class) {
               return Double.compare((double)((Float)a).floatValue(), ((Number)b).doubleValue());
            }

            if (typeB == BigDecimal.class) {
               a = BigDecimal.valueOf((double)((Float)a).floatValue());
            } else if (typeB == String.class) {
               a = BigDecimal.valueOf((double)((Float)a).floatValue());
               b = new BigDecimal((String)b);
            } else if (typeB == BigInteger.class) {
               a = BigDecimal.valueOf((double)((Float)a).floatValue());
               b = new BigDecimal((BigInteger)b);
            }
         } else if (typeA == String.class) {
            String strA = (String)a;
            if (typeB != Integer.class && typeB != Long.class) {
               if (typeB == Float.class || typeB == Double.class) {
                  return Double.compare(Double.parseDouble(strA), ((Number)b).doubleValue());
               }

               if (typeB == BigInteger.class) {
                  a = new BigInteger(strA);
               } else if (typeB == BigDecimal.class) {
                  a = new BigDecimal(strA);
               }
            } else {
               try {
                  long aVal = Long.parseLong(strA);
                  return Long.compare(aVal, ((Number)b).longValue());
               } catch (NumberFormatException var7) {
                  a = new BigDecimal(strA);
                  b = BigDecimal.valueOf(((Number)b).longValue());
               }
            }
         }

         return ((Comparable)a).compareTo(b);
      }
   }

   public static Object getDefaultValue(Type paramType) {
      if (paramType == int.class) {
         return 0;
      } else if (paramType == long.class) {
         return 0L;
      } else if (paramType == float.class) {
         return 0.0F;
      } else if (paramType == double.class) {
         return 0.0;
      } else if (paramType == boolean.class) {
         return Boolean.FALSE;
      } else if (paramType == short.class) {
         return (short)0;
      } else if (paramType == byte.class) {
         return (byte)0;
      } else if (paramType == char.class) {
         return '\u0000';
      } else if (paramType == Optional.class) {
         return Optional.empty();
      } else if (paramType == OptionalInt.class) {
         return OptionalInt.empty();
      } else if (paramType == OptionalLong.class) {
         return OptionalLong.empty();
      } else {
         return paramType == OptionalDouble.class ? OptionalDouble.empty() : null;
      }
   }

   public static Class loadClass(String className) {
      if (className.length() >= 192) {
         return null;
      } else {
         switch (className) {
            case "O":
            case "Object":
            case "java.lang.Object":
               return Object.class;
            case "java.util.Collections$EmptyMap":
               return Collections.emptyMap().getClass();
            case "java.util.Collections$EmptyList":
               return Collections.emptyList().getClass();
            case "java.util.Collections$EmptySet":
               return Collections.emptySet().getClass();
            case "java.util.Optional":
               return Optional.class;
            case "java.util.OptionalInt":
               return OptionalInt.class;
            case "java.util.OptionalLong":
               return OptionalLong.class;
            case "List":
            case "java.util.List":
               return List.class;
            case "A":
            case "ArrayList":
            case "java.util.ArrayList":
               return ArrayList.class;
            case "LA":
            case "LinkedList":
            case "java.util.LinkedList":
               return LinkedList.class;
            case "Map":
            case "java.util.Map":
               return Map.class;
            case "M":
            case "HashMap":
            case "java.util.HashMap":
               return HashMap.class;
            case "LM":
            case "LinkedHashMap":
            case "java.util.LinkedHashMap":
               return LinkedHashMap.class;
            case "ConcurrentHashMap":
               return ConcurrentHashMap.class;
            case "ConcurrentLinkedQueue":
               return ConcurrentLinkedQueue.class;
            case "ConcurrentLinkedDeque":
               return ConcurrentLinkedDeque.class;
            case "JSONObject":
               return JSONObject.class;
            case "JO1":
               className = "com.alibaba.fastjson.JSONObject";
            default:
               Class mapping = TYPE_MAPPINGS.get(className);
               if (mapping != null) {
                  return mapping;
               } else if (className.startsWith("java.util.ImmutableCollections$")) {
                  try {
                     return Class.forName(className);
                  } catch (ClassNotFoundException var5) {
                     return CLASS_UNMODIFIABLE_LIST;
                  }
               } else {
                  if (className.charAt(0) == 'L' && className.charAt(className.length() - 1) == ';') {
                     className = className.substring(1, className.length() - 1);
                  }

                  if (className.charAt(0) != '[' && !className.endsWith("[]")) {
                     ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                     if (contextClassLoader != null) {
                        try {
                           return contextClassLoader.loadClass(className);
                        } catch (ClassNotFoundException var7) {
                        }
                     }

                     try {
                        return JSON.class.getClassLoader().loadClass(className);
                     } catch (ClassNotFoundException var6) {
                        try {
                           return Class.forName(className);
                        } catch (ClassNotFoundException var4) {
                           return null;
                        }
                     }
                  } else {
                     String itemClassName = className.charAt(0) == '[' ? className.substring(1) : className.substring(0, className.length() - 2);
                     Class itemClass = loadClass(itemClassName);
                     if (itemClass == null) {
                        throw new JSONException("load class error " + className);
                     }

                     return Array.newInstance(itemClass, 0).getClass();
                  }
               }
            case "Set":
            case "java.util.Set":
               return Set.class;
            case "HashSet":
            case "java.util.HashSet":
               return HashSet.class;
            case "LinkedHashSet":
            case "java.util.LinkedHashSet":
               return LinkedHashSet.class;
            case "TreeSet":
            case "java.util.TreeSet":
               return TreeSet.class;
            case "java.lang.Class":
               return Class.class;
            case "java.lang.Integer":
               return Integer.class;
            case "java.lang.Long":
               return Long.class;
            case "String":
            case "java.lang.String":
               return String.class;
            case "[String":
               return String[].class;
            case "I":
            case "int":
               return int.class;
            case "S":
            case "short":
               return short.class;
            case "J":
            case "long":
               return long.class;
            case "Z":
            case "boolean":
               return boolean.class;
            case "B":
            case "byte":
               return byte.class;
            case "F":
            case "float":
               return float.class;
            case "D":
            case "double":
               return double.class;
            case "C":
            case "char":
               return char.class;
            case "[B":
            case "byte[]":
               return byte[].class;
            case "[S":
            case "short[]":
               return short[].class;
            case "[I":
            case "int[]":
               return int[].class;
            case "[J":
            case "long[]":
               return long[].class;
            case "[F":
            case "float[]":
               return float[].class;
            case "[D":
            case "double[]":
               return double[].class;
            case "[C":
            case "char[]":
               return char[].class;
            case "[Z":
            case "boolean[]":
               return boolean[].class;
            case "[O":
               return Object[].class;
            case "UUID":
               return UUID.class;
            case "Date":
               return Date.class;
            case "Calendar":
               return Calendar.class;
            case "java.io.IOException":
               return IOException.class;
            case "java.util.Collections$UnmodifiableRandomAccessList":
               return CLASS_UNMODIFIABLE_LIST;
            case "java.util.Arrays$ArrayList":
               return Arrays.asList(1).getClass();
            case "java.util.Collections$SingletonList":
               return CLASS_SINGLE_LIST;
            case "java.util.Collections$SingletonSet":
               return CLASS_SINGLE_SET;
         }
      }
   }

   public static Class<?> getArrayClass(Class componentClass) {
      if (componentClass == int.class) {
         return int[].class;
      } else if (componentClass == byte.class) {
         return byte[].class;
      } else if (componentClass == short.class) {
         return short[].class;
      } else if (componentClass == long.class) {
         return long[].class;
      } else if (componentClass == String.class) {
         return String[].class;
      } else {
         return componentClass == Object.class ? Object[].class : Array.newInstance(componentClass, 1).getClass();
      }
   }

   public static Class nonePrimitive(Class type) {
      if (type.isPrimitive()) {
         String name = type.getName();
         switch (name) {
            case "byte":
               return Byte.class;
            case "short":
               return Short.class;
            case "int":
               return Integer.class;
            case "long":
               return Long.class;
            case "float":
               return Float.class;
            case "double":
               return Double.class;
            case "char":
               return Character.class;
            case "boolean":
               return Boolean.class;
         }
      }

      return type;
   }

   public static Class<?> getClass(Type type) {
      if (type == null) {
         return null;
      } else if (type.getClass() == Class.class) {
         return (Class<?>)type;
      } else if (type instanceof ParameterizedType) {
         return getClass(((ParameterizedType)type).getRawType());
      } else if (type instanceof TypeVariable) {
         Type boundType = ((TypeVariable)type).getBounds()[0];
         return boundType instanceof Class ? (Class)boundType : getClass(boundType);
      } else {
         if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType)type).getUpperBounds();
            if (upperBounds.length == 1) {
               return getClass(upperBounds[0]);
            }
         }

         if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType)type;
            Type componentType = genericArrayType.getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            return getArrayClass(componentClass);
         } else {
            return Object.class;
         }
      }
   }

   public static boolean isProxy(Class<?> clazz) {
      for (Class<?> item : clazz.getInterfaces()) {
         String interfaceName = item.getName();
         switch (interfaceName) {
            case "org.springframework.cglib.proxy.Factory":
            case "javassist.util.proxy.ProxyObject":
            case "org.apache.ibatis.javassist.util.proxy.ProxyObject":
            case "org.hibernate.proxy.HibernateProxy":
            case "org.springframework.context.annotation.ConfigurationClassEnhancer$EnhancedConfiguration":
            case "org.mockito.cglib.proxy.Factory":
            case "net.sf.cglib.proxy.Factory":
               return true;
         }
      }

      return false;
   }

   public static Map getInnerMap(Map object) {
      if (CLASS_JSON_OBJECT_1x != null && CLASS_JSON_OBJECT_1x.isInstance(object) && FIELD_JSON_OBJECT_1x_map != null) {
         try {
            object = (Map)FIELD_JSON_OBJECT_1x_map.get(object);
         } catch (IllegalAccessException var2) {
         }

         return object;
      } else {
         return object;
      }
   }

   public static boolean isFunction(Class type) {
      if (type.isInterface()) {
         String typeName = type.getName();
         return typeName.startsWith("java.util.function.") ? true : type.isAnnotationPresent(FunctionalInterface.class);
      } else {
         return false;
      }
   }

   public static boolean isInteger(String str) {
      if (str != null && !str.isEmpty()) {
         char ch = str.charAt(0);
         boolean sign = ch == '-' || ch == '+';
         if (sign) {
            if (str.length() == 1) {
               return false;
            }
         } else if (ch < '0' || ch > '9') {
            return false;
         }

         for (int i = 1; i < str.length(); i++) {
            ch = str.charAt(i);
            if (ch < '0' || ch > '9') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isInteger(byte[] str, int off, int len) {
      if (str != null && len != 0) {
         char ch = (char)str[off];
         boolean sign = ch == '-' || ch == '+';
         if (sign) {
            if (len == 1) {
               return false;
            }
         } else if (ch < '0' || ch > '9') {
            return false;
         }

         int end = off + len;

         for (int i = off + 1; i < end; i++) {
            ch = (char)str[i];
            if (ch < '0' || ch > '9') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isInteger(char[] str, int off, int len) {
      if (str != null && len != 0) {
         char ch = str[off];
         boolean sign = ch == '-' || ch == '+';
         if (sign) {
            if (len == 1) {
               return false;
            }
         } else if (ch < '0' || ch > '9') {
            return false;
         }

         int end = off + len;

         for (int i = off + 1; i < end; i++) {
            ch = str[i];
            if (ch < '0' || ch > '9') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isNumber(String str) {
      if (str != null && !str.isEmpty()) {
         char ch = str.charAt(0);
         boolean sign = ch == '-' || ch == '+';
         int offset;
         if (sign) {
            if (str.length() == 1) {
               return false;
            }

            ch = str.charAt(1);
            offset = 1;
         } else if (ch == '.') {
            if (str.length() == 1) {
               return false;
            }

            offset = 1;
         } else {
            offset = 0;
         }

         int end = str.length();
         boolean dot = ch == '.';
         boolean space = false;
         boolean num = false;
         if (!dot && ch >= '0' && ch <= '9') {
            num = true;

            do {
               if (offset >= end) {
                  return true;
               }

               ch = str.charAt(offset++);
            } while (!space && ch >= '0' && ch <= '9');
         }

         boolean small = false;
         if (ch == '.') {
            small = true;
            if (offset >= end) {
               return true;
            }

            ch = str.charAt(offset++);
            if (ch >= '0' && ch <= '9') {
               do {
                  if (offset >= end) {
                     return true;
                  }

                  ch = str.charAt(offset++);
               } while (!space && ch >= '0' && ch <= '9');
            }
         }

         if (!num && !small) {
            return false;
         } else {
            if (ch == 'e' || ch == 'E') {
               if (offset == end) {
                  return true;
               }

               ch = str.charAt(offset++);
               boolean eSign = false;
               if (ch == '+' || ch == '-') {
                  eSign = true;
                  if (offset >= end) {
                     return false;
                  }

                  ch = str.charAt(offset++);
               }

               if (ch >= '0' && ch <= '9') {
                  do {
                     if (offset >= end) {
                        return true;
                     }

                     ch = str.charAt(offset++);
                  } while (ch >= '0' && ch <= '9');
               } else if (eSign) {
                  return false;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isNumber(byte[] str, int off, int len) {
      if (str != null && len != 0) {
         char ch = (char)str[off];
         boolean sign = ch == '-' || ch == '+';
         int offset;
         if (sign) {
            if (len == 1) {
               return false;
            }

            ch = (char)str[off + 1];
            offset = off + 1;
         } else if (ch == '.') {
            if (len == 1) {
               return false;
            }

            offset = off + 1;
         } else {
            offset = off;
         }

         int end = off + len;
         boolean dot = ch == '.';
         boolean num = false;
         if (!dot && ch >= '0' && ch <= '9') {
            num = true;

            do {
               if (offset >= end) {
                  return true;
               }

               ch = (char)str[offset++];
            } while (ch >= '0' && ch <= '9');
         }

         boolean small = false;
         if (ch == '.') {
            small = true;
            if (offset >= end) {
               return true;
            }

            ch = (char)str[offset++];
            if (ch >= '0' && ch <= '9') {
               do {
                  if (offset >= end) {
                     return true;
                  }

                  ch = (char)str[offset++];
               } while (ch >= '0' && ch <= '9');
            }
         }

         if (!num && !small) {
            return false;
         } else {
            if (ch == 'e' || ch == 'E') {
               if (offset == end) {
                  return true;
               }

               ch = (char)str[offset++];
               boolean eSign = false;
               if (ch == '+' || ch == '-') {
                  eSign = true;
                  if (offset >= end) {
                     return false;
                  }

                  ch = (char)str[offset++];
               }

               if (ch >= '0' && ch <= '9') {
                  do {
                     if (offset >= end) {
                        return true;
                     }

                     ch = (char)str[offset++];
                  } while (ch >= '0' && ch <= '9');
               } else if (eSign) {
                  return false;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isNumber(char[] str, int off, int len) {
      if (str != null && len != 0) {
         char ch = str[off];
         boolean sign = ch == '-' || ch == '+';
         int offset;
         if (sign) {
            if (len == 1) {
               return false;
            }

            ch = str[off + 1];
            offset = off + 1;
         } else if (ch == '.') {
            if (len == 1) {
               return false;
            }

            offset = off + 1;
         } else {
            offset = off;
         }

         int end = off + len;
         boolean dot = ch == '.';
         boolean space = false;
         boolean num = false;
         if (!dot && ch >= '0' && ch <= '9') {
            num = true;

            do {
               if (offset >= end) {
                  return true;
               }

               ch = str[offset++];
            } while (!space && ch >= '0' && ch <= '9');
         }

         boolean small = false;
         if (ch == '.') {
            small = true;
            if (offset >= end) {
               return true;
            }

            ch = str[offset++];
            if (ch >= '0' && ch <= '9') {
               do {
                  if (offset >= end) {
                     return true;
                  }

                  ch = str[offset++];
               } while (!space && ch >= '0' && ch <= '9');
            }
         }

         if (!num && !small) {
            return false;
         } else {
            if (ch == 'e' || ch == 'E') {
               if (offset == end) {
                  return true;
               }

               ch = str[offset++];
               boolean eSign = false;
               if (ch == '+' || ch == '-') {
                  eSign = true;
                  if (offset >= end) {
                     return false;
                  }

                  ch = str[offset++];
               }

               if (ch >= '0' && ch <= '9') {
                  do {
                     if (offset >= end) {
                        return true;
                     }

                     ch = str[offset++];
                  } while (ch >= '0' && ch <= '9');
               } else if (eSign) {
                  return false;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isUUID(String str) {
      if (str == null) {
         return false;
      } else if (str.length() == 32) {
         for (int i = 0; i < 32; i++) {
            char ch = str.charAt(i);
            boolean valid = ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
            if (!valid) {
               return false;
            }
         }

         return true;
      } else if (str.length() != 36) {
         return false;
      } else {
         for (int ix = 0; ix < 36; ix++) {
            char ch = str.charAt(ix);
            if (ix != 8 && ix != 13 && ix != 18 && ix != 23) {
               boolean valid = ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
               if (!valid) {
                  return false;
               }
            } else if (ch != '-') {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean validateIPv4(String str) {
      return validateIPv4(str, 0);
   }

   static boolean validateIPv4(String str, int off) {
      if (str == null) {
         return false;
      } else {
         int strlen = str.length();
         int len = strlen - off;
         if (len >= 7 && len <= 25) {
            len = off;
            int dotCount = 0;

            for (int i = off; i < strlen; i++) {
               char ch = str.charAt(i);
               if (ch == '.' || i == strlen - 1) {
                  int end = ch == '.' ? i : i + 1;
                  int n = end - len;
                  switch (n) {
                     case 1:
                        char c0 = str.charAt(end - 1);
                        if (c0 < '0' || c0 > '9') {
                           return false;
                        }
                        break;
                     case 2:
                        char c0x = str.charAt(end - 2);
                        char c1 = str.charAt(end - 1);
                        if (c0x < '0' || c0x > '9') {
                           return false;
                        }

                        if (c1 < '0' || c1 > '9') {
                           return false;
                        }
                        break;
                     case 3:
                        char c0xx = str.charAt(end - 3);
                        char c1x = str.charAt(end - 2);
                        char c2 = str.charAt(end - 1);
                        if (c0xx < '0' || c0xx > '2') {
                           return false;
                        }

                        if (c1x < '0' || c1x > '9') {
                           return false;
                        }

                        if (c2 < '0' || c2 > '9') {
                           return false;
                        }

                        int value = (c0xx - '0') * 100 + (c1x - '0') * 10 + (c2 - '0');
                        if (value > 255) {
                           return false;
                        }
                        break;
                     default:
                        return false;
                  }

                  if (ch == '.') {
                     dotCount++;
                     len = i + 1;
                  }
               }
            }

            return dotCount == 3;
         } else {
            return false;
         }
      }
   }

   public static boolean validateIPv6(String str) {
      if (str == null) {
         return false;
      } else {
         int len = str.length();
         if (len >= 2 && len <= 39) {
            int start = 0;
            int colonCount = 0;

            for (int i = 0; i < len; i++) {
               char ch = str.charAt(i);
               if (ch == '.') {
                  boolean ipV4 = validateIPv4(str, start);
                  if (!ipV4) {
                     return false;
                  }
                  break;
               }

               if (ch == ':' || i == len - 1) {
                  int end = ch == ':' ? i : i + 1;
                  int n = end - start;
                  switch (n) {
                     case 0:
                        break;
                     case 1:
                        char c0xxx = str.charAt(end - 1);
                        if ((c0xxx < '0' || c0xxx > '9') && (c0xxx < 'A' || c0xxx > 'F') && (c0xxx < 'a' || c0xxx > 'f')) {
                           return false;
                        }
                        break;
                     case 2:
                        char c0xx = str.charAt(end - 2);
                        char c1xx = str.charAt(end - 1);
                        if ((c0xx < '0' || c0xx > '9') && (c0xx < 'A' || c0xx > 'F') && (c0xx < 'a' || c0xx > 'f')) {
                           return false;
                        }

                        if ((c1xx < '0' || c1xx > '9') && (c1xx < 'A' || c1xx > 'F') && (c1xx < 'a' || c1xx > 'f')) {
                           return false;
                        }
                        break;
                     case 3:
                        char c0x = str.charAt(end - 3);
                        char c1x = str.charAt(end - 2);
                        char c2x = str.charAt(end - 1);
                        if ((c0x < '0' || c0x > '9') && (c0x < 'A' || c0x > 'F') && (c0x < 'a' || c0x > 'f')) {
                           return false;
                        }

                        if ((c1x < '0' || c1x > '9') && (c1x < 'A' || c1x > 'F') && (c1x < 'a' || c1x > 'f')) {
                           return false;
                        }

                        if ((c2x < '0' || c2x > '9') && (c2x < 'A' || c2x > 'F') && (c2x < 'a' || c2x > 'f')) {
                           return false;
                        }
                        break;
                     case 4:
                        char c0 = str.charAt(end - 4);
                        char c1 = str.charAt(end - 3);
                        char c2 = str.charAt(end - 2);
                        char c3 = str.charAt(end - 1);
                        if ((c0 < '0' || c0 > '9') && (c0 < 'A' || c0 > 'F') && (c0 < 'a' || c0 > 'f')) {
                           return false;
                        }

                        if ((c1 < '0' || c1 > '9') && (c1 < 'A' || c1 > 'F') && (c1 < 'a' || c1 > 'f')) {
                           return false;
                        }

                        if ((c2 < '0' || c2 > '9') && (c2 < 'A' || c2 > 'F') && (c2 < 'a' || c2 > 'f')) {
                           return false;
                        }

                        if ((c3 < '0' || c3 > '9') && (c3 < 'A' || c3 > 'F') && (c3 < 'a' || c3 > 'f')) {
                           return false;
                        }
                        break;
                     default:
                        return false;
                  }

                  if (ch == ':') {
                     colonCount++;
                     start = i + 1;
                  }
               }
            }

            return colonCount > 0 && colonCount < 8;
         } else {
            return false;
         }
      }
   }

   public static double doubleValue(int signNum, long intCompact, int scale) {
      int P_D = 53;
      int Q_MIN_D = -1074;
      int Q_MAX_D = 971;
      double L = 3.321928094887362;
      int bitLength = 64 - Long.numberOfLeadingZeros(intCompact);
      long qb = (long)bitLength - (long)Math.ceil((double)scale * 3.321928094887362);
      if (qb < -1076L) {
         return (double)signNum * 0.0;
      } else if (qb > 1025L) {
         return (double)signNum * (Double.POSITIVE_INFINITY);
      } else if (scale < 0) {
         BigInteger pow10 = BIG_TEN_POWERS_TABLE[-scale];
         BigInteger w = BigInteger.valueOf(intCompact);
         return (double)signNum * w.multiply(pow10).doubleValue();
      } else if (scale == 0) {
         return (double)signNum * (double)intCompact;
      } else {
         BigInteger w = BigInteger.valueOf(intCompact);
         int ql = (int)qb - 56;
         BigInteger pow10 = BIG_TEN_POWERS_TABLE[scale];
         BigInteger m;
         BigInteger n;
         if (ql <= 0) {
            m = w.shiftLeft(-ql);
            n = pow10;
         } else {
            m = w;
            n = pow10.shiftLeft(ql);
         }

         BigInteger[] qr = m.divideAndRemainder(n);
         long i = qr[0].longValue();
         int sb = qr[1].signum();
         int dq = 9 - Long.numberOfLeadingZeros(i);
         int eq = -1076 - ql;
         if (dq >= eq) {
            return (double)signNum * Math.scalb((double)(i | (long)sb), ql);
         } else {
            long mask = (1L << eq) - 1L;
            long j = i >> eq | (long)Long.signum(i & mask) | (long)sb;
            return (double)signNum * Math.scalb((double)j, -1076);
         }
      }
   }

   public static float floatValue(int signNum, long intCompact, int scale) {
      int P_F = 24;
      int Q_MIN_F = -149;
      int Q_MAX_F = 104;
      double L = 3.321928094887362;
      int bitLength = 64 - Long.numberOfLeadingZeros(intCompact);
      long qb = (long)bitLength - (long)Math.ceil((double)scale * 3.321928094887362);
      if (qb < -151L) {
         return (float)signNum * 0.0F;
      } else if (qb > 129L) {
         return (float)signNum * (Float.POSITIVE_INFINITY);
      } else if (scale < 0) {
         BigInteger w = BigInteger.valueOf(intCompact);
         return (float)signNum * w.multiply(BIG_TEN_POWERS_TABLE[-scale]).floatValue();
      } else {
         BigInteger w = BigInteger.valueOf(intCompact);
         int ql = (int)qb - 27;
         BigInteger pow10 = BIG_TEN_POWERS_TABLE[scale];
         BigInteger m;
         BigInteger n;
         if (ql <= 0) {
            m = w.shiftLeft(-ql);
            n = pow10;
         } else {
            m = w;
            n = pow10.shiftLeft(ql);
         }

         BigInteger[] qr = m.divideAndRemainder(n);
         int i = qr[0].intValue();
         int sb = qr[1].signum();
         int dq = 6 - Integer.numberOfLeadingZeros(i);
         int eq = -151 - ql;
         if (dq >= eq) {
            return (float)signNum * Math.scalb((float)(i | sb), ql);
         } else {
            int mask = (1 << eq) - 1;
            int j = i >> eq | Integer.signum(i & mask) | sb;
            return (float)signNum * Math.scalb((float)j, -151);
         }
      }
   }

   public static boolean isJavaScriptSupport(long i) {
      return i >= -9007199254740991L && i <= 9007199254740991L;
   }

   public static boolean isJavaScriptSupport(BigDecimal i) {
      return i.precision() >= 16 && isJavaScriptSupport(i.unscaledValue());
   }

   public static boolean isJavaScriptSupport(BigInteger i) {
      return i.compareTo(BIGINT_JAVASCRIPT_LOW) >= 0 && i.compareTo(BIGINT_JAVASCRIPT_HIGH) <= 0;
   }

   public static Type getMapValueType(Type fieldType) {
      if (fieldType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)fieldType;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 2) {
            return actualTypeArguments[1];
         }
      }

      return Object.class;
   }

   static {
      Field field = null;
      if (CLASS_JSON_OBJECT_1x != null) {
         try {
            field = CLASS_JSON_OBJECT_1x.getDeclaredField("map");
            field.setAccessible(true);
         } catch (Throwable var7) {
         }
      }

      FIELD_JSON_OBJECT_1x_map = field;
      CLASS_JSON_ARRAY_1x = loadClass("com.alibaba.fastjson.JSONArray");
      NAME_MAPPINGS.put(byte.class, "B");
      NAME_MAPPINGS.put(short.class, "S");
      NAME_MAPPINGS.put(int.class, "I");
      NAME_MAPPINGS.put(long.class, "J");
      NAME_MAPPINGS.put(float.class, "F");
      NAME_MAPPINGS.put(double.class, "D");
      NAME_MAPPINGS.put(char.class, "C");
      NAME_MAPPINGS.put(boolean.class, "Z");
      NAME_MAPPINGS.put(Object[].class, "[O");
      NAME_MAPPINGS.put(Object[][].class, "[[O");
      NAME_MAPPINGS.put(byte[].class, "[B");
      NAME_MAPPINGS.put(byte[][].class, "[[B");
      NAME_MAPPINGS.put(short[].class, "[S");
      NAME_MAPPINGS.put(short[][].class, "[[S");
      NAME_MAPPINGS.put(int[].class, "[I");
      NAME_MAPPINGS.put(int[][].class, "[[I");
      NAME_MAPPINGS.put(long[].class, "[J");
      NAME_MAPPINGS.put(long[][].class, "[[J");
      NAME_MAPPINGS.put(float[].class, "[F");
      NAME_MAPPINGS.put(float[][].class, "[[F");
      NAME_MAPPINGS.put(double[].class, "[D");
      NAME_MAPPINGS.put(double[][].class, "[[D");
      NAME_MAPPINGS.put(char[].class, "[C");
      NAME_MAPPINGS.put(char[][].class, "[[C");
      NAME_MAPPINGS.put(boolean[].class, "[Z");
      NAME_MAPPINGS.put(boolean[][].class, "[[Z");
      NAME_MAPPINGS.put(Byte[].class, "[Byte");
      NAME_MAPPINGS.put(Byte[][].class, "[[Byte");
      NAME_MAPPINGS.put(Short[].class, "[Short");
      NAME_MAPPINGS.put(Short[][].class, "[[Short");
      NAME_MAPPINGS.put(Integer[].class, "[Integer");
      NAME_MAPPINGS.put(Integer[][].class, "[[Integer");
      NAME_MAPPINGS.put(Long[].class, "[Long");
      NAME_MAPPINGS.put(Long[][].class, "[[Long");
      NAME_MAPPINGS.put(Float[].class, "[Float");
      NAME_MAPPINGS.put(Float[][].class, "[[Float");
      NAME_MAPPINGS.put(Double[].class, "[Double");
      NAME_MAPPINGS.put(Double[][].class, "[[Double");
      NAME_MAPPINGS.put(Character[].class, "[Character");
      NAME_MAPPINGS.put(Character[][].class, "[[Character");
      NAME_MAPPINGS.put(Boolean[].class, "[Boolean");
      NAME_MAPPINGS.put(Boolean[][].class, "[[Boolean");
      NAME_MAPPINGS.put(String[].class, "[String");
      NAME_MAPPINGS.put(String[][].class, "[[String");
      NAME_MAPPINGS.put(BigDecimal[].class, "[BigDecimal");
      NAME_MAPPINGS.put(BigDecimal[][].class, "[[BigDecimal");
      NAME_MAPPINGS.put(BigInteger[].class, "[BigInteger");
      NAME_MAPPINGS.put(BigInteger[][].class, "[[BigInteger");
      NAME_MAPPINGS.put(UUID[].class, "[UUID");
      NAME_MAPPINGS.put(UUID[][].class, "[[UUID");
      NAME_MAPPINGS.put(Object.class, "Object");
      NAME_MAPPINGS.put(HashMap.class, "M");
      TYPE_MAPPINGS.put("HashMap", HashMap.class);
      TYPE_MAPPINGS.put("java.util.HashMap", HashMap.class);
      NAME_MAPPINGS.put(LinkedHashMap.class, "LM");
      TYPE_MAPPINGS.put("LinkedHashMap", LinkedHashMap.class);
      TYPE_MAPPINGS.put("java.util.LinkedHashMap", LinkedHashMap.class);
      NAME_MAPPINGS.put(TreeMap.class, "TM");
      TYPE_MAPPINGS.put("TreeMap", TreeMap.class);
      NAME_MAPPINGS.put(ArrayList.class, "A");
      TYPE_MAPPINGS.put("ArrayList", ArrayList.class);
      TYPE_MAPPINGS.put("java.util.ArrayList", ArrayList.class);
      NAME_MAPPINGS.put(LinkedList.class, "LA");
      TYPE_MAPPINGS.put("LA", LinkedList.class);
      TYPE_MAPPINGS.put("LinkedList", LinkedList.class);
      TYPE_MAPPINGS.put("java.util.LinkedList", LinkedList.class);
      TYPE_MAPPINGS.put("java.util.concurrent.ConcurrentLinkedQueue", ConcurrentLinkedQueue.class);
      TYPE_MAPPINGS.put("java.util.concurrent.ConcurrentLinkedDeque", ConcurrentLinkedDeque.class);
      NAME_MAPPINGS.put(HashSet.class, "HashSet");
      NAME_MAPPINGS.put(TreeSet.class, "TreeSet");
      NAME_MAPPINGS.put(LinkedHashSet.class, "LinkedHashSet");
      NAME_MAPPINGS.put(ConcurrentHashMap.class, "ConcurrentHashMap");
      NAME_MAPPINGS.put(ConcurrentLinkedQueue.class, "ConcurrentLinkedQueue");
      NAME_MAPPINGS.put(ConcurrentLinkedDeque.class, "ConcurrentLinkedDeque");
      NAME_MAPPINGS.put(JSONObject.class, "JSONObject");
      NAME_MAPPINGS.put(JSONArray.class, "JSONArray");
      NAME_MAPPINGS.put(Currency.class, "Currency");
      NAME_MAPPINGS.put(TimeUnit.class, "TimeUnit");
      Class<?>[] classes = new Class[]{
         Object.class,
         Cloneable.class,
         AutoCloseable.class,
         Exception.class,
         RuntimeException.class,
         IllegalAccessError.class,
         IllegalAccessException.class,
         IllegalArgumentException.class,
         IllegalMonitorStateException.class,
         IllegalStateException.class,
         IllegalThreadStateException.class,
         IndexOutOfBoundsException.class,
         InstantiationError.class,
         InstantiationException.class,
         InternalError.class,
         InterruptedException.class,
         LinkageError.class,
         NegativeArraySizeException.class,
         NoClassDefFoundError.class,
         NoSuchFieldError.class,
         NoSuchFieldException.class,
         NoSuchMethodError.class,
         NoSuchMethodException.class,
         NullPointerException.class,
         NumberFormatException.class,
         OutOfMemoryError.class,
         SecurityException.class,
         StackOverflowError.class,
         StringIndexOutOfBoundsException.class,
         TypeNotPresentException.class,
         VerifyError.class,
         StackTraceElement.class,
         Hashtable.class,
         TreeMap.class,
         IdentityHashMap.class,
         WeakHashMap.class,
         HashSet.class,
         LinkedHashSet.class,
         TreeSet.class,
         LinkedList.class,
         TimeUnit.class,
         ConcurrentHashMap.class,
         AtomicInteger.class,
         AtomicLong.class,
         Collections.EMPTY_MAP.getClass(),
         Boolean.class,
         Character.class,
         Byte.class,
         Short.class,
         Integer.class,
         Long.class,
         Float.class,
         Double.class,
         Number.class,
         String.class,
         BigDecimal.class,
         BigInteger.class,
         BitSet.class,
         Calendar.class,
         Date.class,
         Locale.class,
         UUID.class,
         Currency.class,
         SimpleDateFormat.class,
         JSONObject.class,
         JSONArray.class,
         ConcurrentSkipListMap.class,
         ConcurrentSkipListSet.class
      };

      for (Class clazz : classes) {
         TYPE_MAPPINGS.put(clazz.getSimpleName(), clazz);
         TYPE_MAPPINGS.put(clazz.getName(), clazz);
         NAME_MAPPINGS.put(clazz, clazz.getSimpleName());
      }

      TYPE_MAPPINGS.put("JO10", JSONObject1O.class);
      TYPE_MAPPINGS.put("[O", Object[].class);
      TYPE_MAPPINGS.put("[Ljava.lang.Object;", Object[].class);
      TYPE_MAPPINGS.put("[java.lang.Object", Object[].class);
      TYPE_MAPPINGS.put("[Object", Object[].class);
      TYPE_MAPPINGS.put("StackTraceElement", StackTraceElement.class);
      TYPE_MAPPINGS.put("[StackTraceElement", StackTraceElement[].class);
      String[] items = new String[]{"java.util.Collections$UnmodifiableMap", "java.util.Collections$UnmodifiableCollection"};

      for (String className : items) {
         Class<?> clazz = loadClass(className);
         TYPE_MAPPINGS.put(clazz.getName(), clazz);
      }

      if (CLASS_JSON_OBJECT_1x != null) {
         TYPE_MAPPINGS.putIfAbsent("JO1", CLASS_JSON_OBJECT_1x);
         TYPE_MAPPINGS.putIfAbsent(CLASS_JSON_OBJECT_1x.getName(), CLASS_JSON_OBJECT_1x);
      }

      if (CLASS_JSON_ARRAY_1x != null) {
         TYPE_MAPPINGS.putIfAbsent("JA1", CLASS_JSON_ARRAY_1x);
         TYPE_MAPPINGS.putIfAbsent(CLASS_JSON_ARRAY_1x.getName(), CLASS_JSON_ARRAY_1x);
      }

      NAME_MAPPINGS.put(new HashMap().keySet().getClass(), "Set");
      NAME_MAPPINGS.put(new LinkedHashMap().keySet().getClass(), "Set");
      NAME_MAPPINGS.put(new TreeMap().keySet().getClass(), "Set");
      NAME_MAPPINGS.put(new ConcurrentHashMap().keySet().getClass(), "Set");
      NAME_MAPPINGS.put(new ConcurrentSkipListMap().keySet().getClass(), "Set");
      TYPE_MAPPINGS.put("Set", HashSet.class);
      NAME_MAPPINGS.put(new HashMap().values().getClass(), "List");
      NAME_MAPPINGS.put(new LinkedHashMap().values().getClass(), "List");
      NAME_MAPPINGS.put(new TreeMap().values().getClass(), "List");
      NAME_MAPPINGS.put(new ConcurrentHashMap().values().getClass(), "List");
      NAME_MAPPINGS.put(new ConcurrentSkipListMap().values().getClass(), "List");
      TYPE_MAPPINGS.put("List", ArrayList.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$Map1", HashMap.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$MapN", LinkedHashMap.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$Set12", LinkedHashSet.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$SetN", LinkedHashSet.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$List12", ArrayList.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$ListN", ArrayList.class);
      TYPE_MAPPINGS.put("java.util.ImmutableCollections$SubList", ArrayList.class);

      for (Entry<Class, String> entry : NAME_MAPPINGS.entrySet()) {
         TYPE_MAPPINGS.putIfAbsent(entry.getValue(), entry.getKey());
      }

      BigInteger[] bigInts = new BigInteger[128];
      bigInts[0] = BigInteger.ONE;
      bigInts[1] = BigInteger.TEN;
      long longValue = 10L;

      for (int i = 2; i < 19; i++) {
         longValue *= 10L;
         bigInts[i] = BigInteger.valueOf(longValue);
      }

      BigInteger bigInt = bigInts[18];

      for (int i = 19; i < 128; i++) {
         bigInt = bigInt.multiply(BigInteger.TEN);
         bigInts[i] = bigInt;
      }

      BIG_TEN_POWERS_TABLE = bigInts;
   }

   static class Cache {
      volatile char[] chars;
   }

   static class X1 {
      static final Function<byte[], char[]> TO_CHARS;

      static {
         Function<byte[], char[]> toChars = null;
         if (JDKUtils.JVM_VERSION > 9) {
            try {
               Class<?> latin1Class = Class.forName("java.lang.StringLatin1");
               Lookup lookup = JDKUtils.trustedLookup(latin1Class);
               MethodHandle handle = lookup.findStatic(latin1Class, "toChars", MethodType.methodType(char[].class, byte[].class));
               CallSite callSite = LambdaMetafactory.metafactory(
                  lookup,
                  "apply",
                  MethodType.methodType(Function.class),
                  MethodType.methodType(Object.class, Object.class),
                  handle,
                  MethodType.methodType(char[].class, byte[].class)
               );
               toChars = (Function)callSite.getTarget().invokeExact();
            } catch (Throwable var5) {
            }
         }

         if (toChars == null) {
            toChars = TypeUtils::toAsciiCharArray;
         }

         TO_CHARS = toChars;
      }
   }

   static class X2 {
      static final String[] chars;
      static final String[] chars2;
      static final char START = ' ';
      static final char END = '~';
      static final int SIZE2 = 95;

      static {
         String[] array0 = new String[128];

         for (char i = 0; i < array0.length; i++) {
            array0[i] = Character.toString(i);
         }

         chars = array0;
         String[] array1 = new String[9025];
         char[] c2 = new char[2];

         for (char i = ' '; i <= '~'; i++) {
            for (char j = ' '; j <= '~'; j++) {
               int value = (i - ' ') * 95 + (j - ' ');
               c2[0] = i;
               c2[1] = j;
               array1[value] = new String(c2);
            }
         }

         chars2 = array1;
      }
   }
}
