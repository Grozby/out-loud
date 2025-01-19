package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONPObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONBuilder;
import com.alibaba.fastjson2.annotation.JSONCompiler;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.function.impl.StringToAny;
import com.alibaba.fastjson2.function.impl.ToBigDecimal;
import com.alibaba.fastjson2.function.impl.ToBigInteger;
import com.alibaba.fastjson2.function.impl.ToBoolean;
import com.alibaba.fastjson2.function.impl.ToByte;
import com.alibaba.fastjson2.function.impl.ToDouble;
import com.alibaba.fastjson2.function.impl.ToFloat;
import com.alibaba.fastjson2.function.impl.ToInteger;
import com.alibaba.fastjson2.function.impl.ToLong;
import com.alibaba.fastjson2.function.impl.ToNumber;
import com.alibaba.fastjson2.function.impl.ToShort;
import com.alibaba.fastjson2.function.impl.ToString;
import com.alibaba.fastjson2.modules.ObjectReaderAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.support.money.MoneySupport;
import com.alibaba.fastjson2.util.ApacheLang3Support;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.GuavaSupport;
import com.alibaba.fastjson2.util.JdbcSupport;
import com.alibaba.fastjson2.util.JodaSupport;
import com.alibaba.fastjson2.util.KotlinUtils;
import com.alibaba.fastjson2.util.MapMultiValueType;
import com.alibaba.fastjson2.util.MultiType;
import com.alibaba.fastjson2.util.ParameterizedTypeImpl;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ObjectReaderBaseModule implements ObjectReaderModule {
   final ObjectReaderProvider provider;
   final ObjectReaderBaseModule.ReaderAnnotationProcessor annotationProcessor;

   public ObjectReaderBaseModule(ObjectReaderProvider provider) {
      this.provider = provider;
      this.annotationProcessor = new ObjectReaderBaseModule.ReaderAnnotationProcessor();
   }

   @Override
   public ObjectReaderProvider getProvider() {
      return this.provider;
   }

   @Override
   public void init(ObjectReaderProvider provider) {
      provider.registerTypeConvert(Character.class, char.class, o -> o);
      Class[] numberTypes = new Class[]{
         Boolean.class,
         Byte.class,
         Short.class,
         Integer.class,
         Long.class,
         Number.class,
         Float.class,
         Double.class,
         BigInteger.class,
         BigDecimal.class,
         AtomicInteger.class,
         AtomicLong.class
      };
      Function<Object, Boolean> TO_BOOLEAN = new ToBoolean(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Boolean.class, TO_BOOLEAN);
      }

      Function<Object, Boolean> TO_BOOLEAN_VALUE = new ToBoolean(Boolean.FALSE);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, boolean.class, TO_BOOLEAN_VALUE);
      }

      Function<Object, String> TO_STRING = new ToString();

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, String.class, TO_STRING);
      }

      Function<Object, BigDecimal> TO_DECIMAL = new ToBigDecimal();

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, BigDecimal.class, TO_DECIMAL);
      }

      Function<Object, BigInteger> TO_BIGINT = new ToBigInteger();

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, BigInteger.class, TO_BIGINT);
      }

      Function<Object, Byte> TO_BYTE = new ToByte(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Byte.class, TO_BYTE);
      }

      Function<Object, Byte> TO_BYTE_VALUE = new ToByte((byte)0);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, byte.class, TO_BYTE_VALUE);
      }

      Function<Object, Short> TO_SHORT = new ToShort(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Short.class, TO_SHORT);
      }

      Function<Object, Short> TO_SHORT_VALUE = new ToShort((short)0);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, short.class, TO_SHORT_VALUE);
      }

      Function<Object, Integer> TO_INTEGER = new ToInteger(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Integer.class, TO_INTEGER);
      }

      Function<Object, Integer> TO_INT = new ToInteger(0);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, int.class, TO_INT);
      }

      Function<Object, Long> TO_LONG = new ToLong(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Long.class, TO_LONG);
      }

      Function<Object, Long> TO_LONG_VALUE = new ToLong(0L);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, long.class, TO_LONG_VALUE);
      }

      Function<Object, Float> TO_FLOAT = new ToFloat(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Float.class, TO_FLOAT);
      }

      Function<Object, Float> TO_FLOAT_VALUE = new ToFloat(0.0F);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, float.class, TO_FLOAT_VALUE);
      }

      Function<Object, Double> TO_DOUBLE = new ToDouble(null);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Double.class, TO_DOUBLE);
      }

      Function<Object, Double> TO_DOUBLE_VALUE = new ToDouble(0.0);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, double.class, TO_DOUBLE_VALUE);
      }

      Function<Object, Number> TO_NUMBER = new ToNumber(0.0);

      for (Class type : numberTypes) {
         provider.registerTypeConvert(type, Number.class, TO_NUMBER);
      }

      provider.registerTypeConvert(String.class, char.class, new StringToAny(char.class, '0'));
      provider.registerTypeConvert(String.class, boolean.class, new StringToAny(boolean.class, false));
      provider.registerTypeConvert(String.class, float.class, new StringToAny(float.class, 0.0F));
      provider.registerTypeConvert(String.class, double.class, new StringToAny(double.class, 0.0));
      provider.registerTypeConvert(String.class, byte.class, new StringToAny(byte.class, (byte)0));
      provider.registerTypeConvert(String.class, short.class, new StringToAny(short.class, (short)0));
      provider.registerTypeConvert(String.class, int.class, new StringToAny(int.class, 0));
      provider.registerTypeConvert(String.class, long.class, new StringToAny(long.class, 0L));
      provider.registerTypeConvert(String.class, Character.class, new StringToAny(Character.class, null));
      provider.registerTypeConvert(String.class, Boolean.class, new StringToAny(Boolean.class, null));
      provider.registerTypeConvert(String.class, Double.class, new StringToAny(Double.class, null));
      provider.registerTypeConvert(String.class, Float.class, new StringToAny(Float.class, null));
      provider.registerTypeConvert(String.class, Byte.class, new StringToAny(Byte.class, null));
      provider.registerTypeConvert(String.class, Short.class, new StringToAny(Short.class, null));
      provider.registerTypeConvert(String.class, Integer.class, new StringToAny(Integer.class, null));
      provider.registerTypeConvert(String.class, Long.class, new StringToAny(Long.class, null));
      provider.registerTypeConvert(String.class, BigDecimal.class, new StringToAny(BigDecimal.class, null));
      provider.registerTypeConvert(String.class, BigInteger.class, new StringToAny(BigInteger.class, null));
      provider.registerTypeConvert(String.class, Number.class, new StringToAny(BigDecimal.class, null));
      provider.registerTypeConvert(String.class, Collection.class, new StringToAny(Collection.class, null));
      provider.registerTypeConvert(String.class, List.class, new StringToAny(List.class, null));
      provider.registerTypeConvert(String.class, JSONArray.class, new StringToAny(JSONArray.class, null));
      provider.registerTypeConvert(Boolean.class, boolean.class, o -> o);
      Function function = o -> o != null && !"null".equals(o) && !o.equals(0L)
            ? LocalDateTime.ofInstant(Instant.ofEpochMilli((Long)o), ZoneId.systemDefault())
            : null;
      provider.registerTypeConvert(Long.class, LocalDateTime.class, function);
      function = o -> o != null && !"null".equals(o) && !"".equals(o) ? UUID.fromString((String)o) : null;
      provider.registerTypeConvert(String.class, UUID.class, function);
   }

   private void getBeanInfo1xJSONPOJOBuilder(
      BeanInfo beanInfo, Class<?> builderClass, Annotation builderAnnatation, Class<? extends Annotation> builderAnnatationClass
   ) {
      BeanUtils.annotationMethods(builderAnnatationClass, method -> {
         try {
            String methodName = method.getName();
            switch (methodName) {
               case "buildMethodName":
               case "buildMethod":
                  String buildMethodName = (String)method.invoke(builderAnnatation);
                  beanInfo.buildMethod = BeanUtils.buildMethod(builderClass, buildMethodName);
                  break;
               case "withPrefix":
                  String withPrefix = (String)method.invoke(builderAnnatation);
                  if (!withPrefix.isEmpty()) {
                     beanInfo.builderWithPrefix = withPrefix;
                  }
            }
         } catch (Throwable var8) {
         }
      });
   }

   private void getCreator(BeanInfo beanInfo, Class<?> objectClass, Constructor constructor) {
      if (!objectClass.isEnum()) {
         Annotation[] annotations = BeanUtils.getAnnotations(constructor);
         boolean creatorMethod = false;

         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            JSONCreator jsonCreator = BeanUtils.findAnnotation(annotation, JSONCreator.class);
            if (jsonCreator != null) {
               String[] createParameterNames = jsonCreator.parameterNames();
               if (createParameterNames.length != 0) {
                  beanInfo.createParameterNames = createParameterNames;
               }

               creatorMethod = true;
               if (jsonCreator == annotation) {
                  continue;
               }
            }

            String var16 = annotationType.getName();
            switch (var16) {
               case "com.alibaba.fastjson.annotation.JSONCreator":
               case "com.alibaba.fastjson2.annotation.JSONCreator":
                  creatorMethod = true;
                  BeanUtils.annotationMethods(annotationType, m1 -> {
                     try {
                        if ("parameterNames".equals(m1.getName())) {
                           String[] createParameterNamesx = (String[])m1.invoke(annotation);
                           if (createParameterNamesx.length != 0) {
                              beanInfo.createParameterNames = createParameterNamesx;
                           }
                        }
                     } catch (Throwable var4x) {
                     }
                  });
                  break;
               case "com.fasterxml.jackson.annotation.JsonCreator":
                  if (JSONFactory.isUseJacksonAnnotation()) {
                     creatorMethod = true;
                  }
            }
         }

         if (creatorMethod) {
            Constructor<?> targetConstructor = null;

            try {
               targetConstructor = objectClass.getDeclaredConstructor(constructor.getParameterTypes());
            } catch (NoSuchMethodException var14) {
            }

            if (targetConstructor != null) {
               beanInfo.creatorConstructor = targetConstructor;
            }
         }
      }
   }

   private void getCreator(BeanInfo beanInfo, Class<?> objectClass, Method method) {
      if (method.getDeclaringClass() != Enum.class) {
         String methodName = method.getName();
         if (!objectClass.isEnum() || !"values".equals(methodName)) {
            Annotation[] annotations = BeanUtils.getAnnotations(method);
            boolean creatorMethod = false;
            JSONCreator jsonCreator = null;

            for (Annotation annotation : annotations) {
               Class<? extends Annotation> annotationType = annotation.annotationType();
               jsonCreator = BeanUtils.findAnnotation(annotation, JSONCreator.class);
               if (jsonCreator != annotation) {
                  String var13 = annotationType.getName();
                  switch (var13) {
                     case "com.alibaba.fastjson.annotation.JSONCreator":
                        creatorMethod = true;
                        BeanUtils.annotationMethods(annotationType, m1 -> {
                           try {
                              if ("parameterNames".equals(m1.getName())) {
                                 String[] createParameterNames = (String[])m1.invoke(annotation);
                                 if (createParameterNames.length != 0) {
                                    beanInfo.createParameterNames = createParameterNames;
                                 }
                              }
                           } catch (Throwable var4x) {
                           }
                        });
                        break;
                     case "com.fasterxml.jackson.annotation.JsonCreator":
                        if (JSONFactory.isUseJacksonAnnotation()) {
                           creatorMethod = true;
                           BeanUtils.annotationMethods(annotationType, m1 -> {
                              try {
                                 if ("parameterNames".equals(m1.getName())) {
                                    String[] createParameterNames = (String[])m1.invoke(annotation);
                                    if (createParameterNames.length != 0) {
                                       beanInfo.createParameterNames = createParameterNames;
                                    }
                                 }
                              } catch (Throwable var4x) {
                              }
                           });
                        }
                  }
               }
            }

            if (jsonCreator != null) {
               String[] createParameterNames = jsonCreator.parameterNames();
               if (createParameterNames.length != 0) {
                  beanInfo.createParameterNames = createParameterNames;
               }

               creatorMethod = true;
            }

            if (creatorMethod) {
               Method targetMethod = null;

               try {
                  targetMethod = objectClass.getDeclaredMethod(methodName, method.getParameterTypes());
               } catch (NoSuchMethodException var15) {
               }

               if (targetMethod != null) {
                  beanInfo.createMethod = targetMethod;
               }
            }
         }
      }
   }

   public ObjectReaderBaseModule.ReaderAnnotationProcessor getAnnotationProcessor() {
      return this.annotationProcessor;
   }

   @Override
   public void getBeanInfo(BeanInfo beanInfo, Class<?> objectClass) {
      if (this.annotationProcessor != null) {
         this.annotationProcessor.getBeanInfo(beanInfo, objectClass);
      }
   }

   @Override
   public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Field field) {
      if (this.annotationProcessor != null) {
         this.annotationProcessor.getFieldInfo(fieldInfo, objectClass, field);
      }
   }

   @Override
   public ObjectReader getObjectReader(ObjectReaderProvider provider, Type type) {
      if (type == String.class || type == CharSequence.class) {
         return ObjectReaderImplString.INSTANCE;
      } else if (type == char.class || type == Character.class) {
         return ObjectReaderImplCharacter.INSTANCE;
      } else if (type == boolean.class || type == Boolean.class) {
         return ObjectReaderImplBoolean.INSTANCE;
      } else if (type == byte.class || type == Byte.class) {
         return ObjectReaderImplByte.INSTANCE;
      } else if (type == short.class || type == Short.class) {
         return ObjectReaderImplShort.INSTANCE;
      } else if (type == int.class || type == Integer.class) {
         return ObjectReaderImplInteger.INSTANCE;
      } else if (type == long.class || type == Long.class) {
         return ObjectReaderImplInt64.INSTANCE;
      } else if (type == float.class || type == Float.class) {
         return ObjectReaderImplFloat.INSTANCE;
      } else if (type == double.class || type == Double.class) {
         return ObjectReaderImplDouble.INSTANCE;
      } else if (type == BigInteger.class) {
         return ObjectReaderImplBigInteger.INSTANCE;
      } else if (type == BigDecimal.class) {
         return ObjectReaderImplBigDecimal.INSTANCE;
      } else if (type == Number.class) {
         return ObjectReaderImplNumber.INSTANCE;
      } else if (type == BitSet.class) {
         return ObjectReaderImplBitSet.INSTANCE;
      } else if (type == OptionalInt.class) {
         return ObjectReaderImplOptionalInt.INSTANCE;
      } else if (type == OptionalLong.class) {
         return ObjectReaderImplOptionalLong.INSTANCE;
      } else if (type == OptionalDouble.class) {
         return ObjectReaderImplOptionalDouble.INSTANCE;
      } else if (type == Optional.class) {
         return ObjectReaderImplOptional.INSTANCE;
      } else if (type == UUID.class) {
         return ObjectReaderImplUUID.INSTANCE;
      } else if (type == Duration.class) {
         return new ObjectReaderImplFromString<>(Duration.class, Duration::parse);
      } else if (type == Period.class) {
         return new ObjectReaderImplFromString<>(Period.class, Period::parse);
      } else if (type == AtomicBoolean.class) {
         return new ObjectReaderImplFromBoolean<>(AtomicBoolean.class, AtomicBoolean::new);
      } else if (type == URI.class) {
         return new ObjectReaderImplFromString<>(URI.class, URI::create);
      } else if (type == Charset.class) {
         return new ObjectReaderImplFromString<>(Charset.class, Charset::forName);
      } else if (type == File.class) {
         return new ObjectReaderImplFromString<>(File.class, File::new);
      } else if (type == Path.class) {
         return new ObjectReaderImplFromString<>(Path.class, x$0 -> Paths.get(x$0));
      } else if (type == URL.class) {
         return new ObjectReaderImplFromString<>(URL.class, e -> {
            try {
               return new URL(e);
            } catch (MalformedURLException var2x) {
               throw new JSONException("read URL error", var2x);
            }
         });
      } else if (type == Pattern.class) {
         return new ObjectReaderImplFromString<>(Pattern.class, Pattern::compile);
      } else if (type == Class.class) {
         return ObjectReaderImplClass.INSTANCE;
      } else if (type == Method.class) {
         return new ObjectReaderImplMethod();
      } else if (type == Field.class) {
         return new ObjectReaderImplField();
      } else if (type == Type.class) {
         return ObjectReaderImplClass.INSTANCE;
      } else {
         String internalMixin = null;
         String typeName = type.getTypeName();
         switch (typeName) {
            case "com.google.common.collect.AbstractMapBasedMultimap$WrappedSet":
               return null;
            case "org.springframework.util.LinkedMultiValueMap":
               return ObjectReaderImplMap.of(type, (Class)type, 0L);
            case "org.springframework.security.core.authority.RememberMeAuthenticationToken":
               internalMixin = "org.springframework.security.jackson2.AnonymousAuthenticationTokenMixin";
               break;
            case "org.springframework.security.core.authority.AnonymousAuthenticationToken":
               internalMixin = "org.springframework.security.jackson2.RememberMeAuthenticationTokenMixin";
               break;
            case "org.springframework.security.core.authority.SimpleGrantedAuthority":
               internalMixin = "org.springframework.security.jackson2.SimpleGrantedAuthorityMixin";
               break;
            case "org.springframework.security.core.userdetails.User":
               internalMixin = "org.springframework.security.jackson2.UserMixin";
               break;
            case "org.springframework.security.authentication.UsernamePasswordAuthenticationToken":
               internalMixin = "org.springframework.security.jackson2.UsernamePasswordAuthenticationTokenMixin";
               break;
            case "org.springframework.security.authentication.BadCredentialsException":
               internalMixin = "org.springframework.security.jackson2.BadCredentialsExceptionMixin";
               break;
            case "org.springframework.security.web.csrf.DefaultCsrfToken":
               internalMixin = "org.springframework.security.web.jackson2.DefaultCsrfTokenMixin";
               break;
            case "org.springframework.security.web.savedrequest.SavedCookie":
               internalMixin = "org.springframework.security.web.jackson2.SavedCookieMixin";
               break;
            case "org.springframework.security.web.authentication.WebAuthenticationDetails":
               internalMixin = "org.springframework.security.web.jackson2.WebAuthenticationDetailsMixin";
         }

         if (internalMixin != null) {
            Class mixin = provider.mixInCache.get(type);
            if (mixin == null) {
               mixin = TypeUtils.loadClass(internalMixin);
               if (mixin == null && "org.springframework.security.jackson2.SimpleGrantedAuthorityMixin".equals(internalMixin)) {
                  mixin = TypeUtils.loadClass("com.alibaba.fastjson2.internal.mixin.spring.SimpleGrantedAuthorityMixin");
               }

               if (mixin != null) {
                  provider.mixInCache.putIfAbsent((Class)type, mixin);
               }
            }
         }

         if (type == Map.class || type == AbstractMap.class) {
            return ObjectReaderImplMap.of(null, (Class)type, 0L);
         } else if (type == ConcurrentMap.class || type == ConcurrentHashMap.class) {
            return typedMap((Class)type, ConcurrentHashMap.class, null, Object.class);
         } else if (type == ConcurrentNavigableMap.class || type == ConcurrentSkipListMap.class) {
            return typedMap((Class)type, ConcurrentSkipListMap.class, null, Object.class);
         } else if (type == SortedMap.class || type == NavigableMap.class || type == TreeMap.class) {
            return typedMap((Class)type, TreeMap.class, null, Object.class);
         } else if (type == Calendar.class || "javax.xml.datatype.XMLGregorianCalendar".equals(typeName)) {
            return ObjectReaderImplCalendar.INSTANCE;
         } else if (type == Date.class) {
            return ObjectReaderImplDate.INSTANCE;
         } else if (type == LocalDate.class) {
            return ObjectReaderImplLocalDate.INSTANCE;
         } else if (type == LocalTime.class) {
            return ObjectReaderImplLocalTime.INSTANCE;
         } else if (type == LocalDateTime.class) {
            return ObjectReaderImplLocalDateTime.INSTANCE;
         } else if (type == ZonedDateTime.class) {
            return ObjectReaderImplZonedDateTime.INSTANCE;
         } else if (type == OffsetDateTime.class) {
            return ObjectReaderImplOffsetDateTime.INSTANCE;
         } else if (type == OffsetTime.class) {
            return ObjectReaderImplOffsetTime.INSTANCE;
         } else if (type == ZoneOffset.class) {
            return new ObjectReaderImplFromString<>(ZoneOffset.class, ZoneOffset::of);
         } else if (type == Instant.class) {
            return ObjectReaderImplInstant.INSTANCE;
         } else if (type == Locale.class) {
            return ObjectReaderImplLocale.INSTANCE;
         } else if (type == Currency.class) {
            return ObjectReaderImplCurrency.INSTANCE;
         } else if (type == ZoneId.class) {
            return new ObjectReaderImplFromString<>(ZoneId.class, ZoneId::of);
         } else if (type == TimeZone.class) {
            return new ObjectReaderImplFromString<>(TimeZone.class, TimeZone::getTimeZone);
         } else if (type == char[].class) {
            return ObjectReaderImplCharValueArray.INSTANCE;
         } else if (type == float[].class) {
            return ObjectReaderImplFloatValueArray.INSTANCE;
         } else if (type == double[].class) {
            return ObjectReaderImplDoubleValueArray.INSTANCE;
         } else if (type == boolean[].class) {
            return ObjectReaderImplBoolValueArray.INSTANCE;
         } else if (type == byte[].class) {
            return ObjectReaderImplInt8ValueArray.INSTANCE;
         } else if (type == short[].class) {
            return ObjectReaderImplInt16ValueArray.INSTANCE;
         } else if (type == int[].class) {
            return ObjectReaderImplInt32ValueArray.INSTANCE;
         } else if (type == long[].class) {
            return ObjectReaderImplInt64ValueArray.INSTANCE;
         } else if (type == Byte[].class) {
            return ObjectReaderImplInt8Array.INSTANCE;
         } else if (type == Short[].class) {
            return ObjectReaderImplInt16Array.INSTANCE;
         } else if (type == Integer[].class) {
            return ObjectReaderImplInt32Array.INSTANCE;
         } else if (type == Long[].class) {
            return ObjectReaderImplInt64Array.INSTANCE;
         } else if (type == Float[].class) {
            return ObjectReaderImplFloatArray.INSTANCE;
         } else if (type == Double[].class) {
            return ObjectReaderImplDoubleArray.INSTANCE;
         } else if (type == Number[].class) {
            return ObjectReaderImplNumberArray.INSTANCE;
         } else if (type == String[].class) {
            return ObjectReaderImplStringArray.INSTANCE;
         } else if (type == AtomicInteger.class) {
            return new ObjectReaderImplFromInt<>(AtomicInteger.class, AtomicInteger::new);
         } else if (type == AtomicLong.class) {
            return new ObjectReaderImplFromLong<>(AtomicLong.class, AtomicLong::new);
         } else if (type == AtomicIntegerArray.class) {
            return new ObjectReaderImplInt32ValueArray(AtomicIntegerArray.class, AtomicIntegerArray::new);
         } else if (type == AtomicLongArray.class) {
            return new ObjectReaderImplInt64ValueArray(AtomicLongArray.class, AtomicLongArray::new);
         } else if (type == AtomicReference.class) {
            return ObjectReaderImplAtomicReference.INSTANCE;
         } else if (type instanceof MultiType) {
            return new ObjectArrayReaderMultiType((MultiType)type);
         } else if (type instanceof MapMultiValueType) {
            return new ObjectReaderImplMapMultiValueType((MapMultiValueType)type);
         } else if (type == StringBuffer.class || type == StringBuilder.class) {
            try {
               Class objectClass = (Class)type;
               return new ObjectReaderImplValue<>(
                  objectClass, String.class, String.class, 0L, null, null, null, objectClass.getConstructor(String.class), null, null
               );
            } catch (NoSuchMethodException var12) {
               throw new RuntimeException(var12);
            }
         } else if (type == Iterable.class
            || type == Collection.class
            || type == List.class
            || type == AbstractCollection.class
            || type == AbstractList.class
            || type == ArrayList.class) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == Queue.class || type == Deque.class || type == AbstractSequentialList.class || type == LinkedList.class) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == Set.class || type == AbstractSet.class || type == EnumSet.class) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == NavigableSet.class || type == SortedSet.class) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == ConcurrentLinkedDeque.class
            || type == ConcurrentLinkedQueue.class
            || type == ConcurrentSkipListSet.class
            || type == LinkedHashSet.class
            || type == HashSet.class
            || type == TreeSet.class
            || type == CopyOnWriteArrayList.class) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == ObjectReaderImplList.CLASS_EMPTY_SET
            || type == ObjectReaderImplList.CLASS_EMPTY_LIST
            || type == ObjectReaderImplList.CLASS_SINGLETON
            || type == ObjectReaderImplList.CLASS_SINGLETON_LIST
            || type == ObjectReaderImplList.CLASS_ARRAYS_LIST
            || type == ObjectReaderImplList.CLASS_UNMODIFIABLE_COLLECTION
            || type == ObjectReaderImplList.CLASS_UNMODIFIABLE_LIST
            || type == ObjectReaderImplList.CLASS_UNMODIFIABLE_SET
            || type == ObjectReaderImplList.CLASS_UNMODIFIABLE_SORTED_SET
            || type == ObjectReaderImplList.CLASS_UNMODIFIABLE_NAVIGABLE_SET) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == TypeUtils.CLASS_SINGLE_SET) {
            return ObjectReaderImplList.of(type, null, 0L);
         } else if (type == Object.class || type == Cloneable.class || type == Closeable.class || type == Serializable.class || type == Comparable.class) {
            return ObjectReaderImplObject.INSTANCE;
         } else if (type == Entry.class) {
            return new ObjectReaderImplMapEntry(null, null);
         } else {
            if (type instanceof Class) {
               Class objectClass = (Class)type;
               if (BeanUtils.isExtendedMap(objectClass)) {
                  return null;
               }

               if (Map.class.isAssignableFrom(objectClass)) {
                  return ObjectReaderImplMap.of(null, objectClass, 0L);
               }

               if (Collection.class.isAssignableFrom(objectClass)) {
                  return ObjectReaderImplList.of(objectClass, objectClass, 0L);
               }

               if (objectClass.isArray()) {
                  Class componentType = objectClass.getComponentType();
                  if (componentType == Object.class) {
                     return ObjectArrayReader.INSTANCE;
                  }

                  return new ObjectArrayTypedReader(objectClass);
               }

               if (JSONPObject.class.isAssignableFrom(objectClass)) {
                  return new ObjectReaderImplJSONP(objectClass);
               }

               ObjectReaderCreator creator = JSONFactory.getDefaultObjectReaderProvider().getCreator();
               if (objectClass == StackTraceElement.class) {
                  try {
                     Constructor constructor = objectClass.getConstructor(String.class, String.class, String.class, int.class);
                     return creator.createObjectReaderNoneDefaultConstructor(constructor, "className", "methodName", "fileName", "lineNumber");
                  } catch (Throwable var14) {
                  }
               }
            }

            if (type instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)type;
               Type rawType = parameterizedType.getRawType();
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 2) {
                  Type actualTypeParam0 = actualTypeArguments[0];
                  Type actualTypeParam1 = actualTypeArguments[1];
                  if (rawType == Map.class || rawType == AbstractMap.class || rawType == HashMap.class) {
                     return typedMap((Class)rawType, HashMap.class, actualTypeParam0, actualTypeParam1);
                  }

                  if (rawType == ConcurrentMap.class || rawType == ConcurrentHashMap.class) {
                     return typedMap((Class)rawType, ConcurrentHashMap.class, actualTypeParam0, actualTypeParam1);
                  }

                  if (rawType == ConcurrentNavigableMap.class || rawType == ConcurrentSkipListMap.class) {
                     return typedMap((Class)rawType, ConcurrentSkipListMap.class, actualTypeParam0, actualTypeParam1);
                  }

                  if (rawType == LinkedHashMap.class || rawType == TreeMap.class || rawType == Hashtable.class) {
                     return typedMap((Class)rawType, (Class)rawType, actualTypeParam0, actualTypeParam1);
                  }

                  if (rawType == Entry.class) {
                     return new ObjectReaderImplMapEntry(actualTypeParam0, actualTypeParam1);
                  }

                  String var28 = rawType.getTypeName();
                  switch (var28) {
                     case "com.google.common.collect.ImmutableMap":
                     case "com.google.common.collect.RegularImmutableMap":
                        return new ObjectReaderImplMapTyped(
                           (Class)rawType, HashMap.class, actualTypeParam0, actualTypeParam1, 0L, GuavaSupport.immutableMapConverter()
                        );
                     case "com.google.common.collect.SingletonImmutableBiMap":
                        return new ObjectReaderImplMapTyped(
                           (Class)rawType, HashMap.class, actualTypeParam0, actualTypeParam1, 0L, GuavaSupport.singletonBiMapConverter()
                        );
                     case "org.springframework.util.LinkedMultiValueMap":
                        return ObjectReaderImplMap.of(type, (Class)rawType, 0L);
                     case "org.apache.commons.lang3.tuple.Pair":
                     case "org.apache.commons.lang3.tuple.ImmutablePair":
                        return new ApacheLang3Support.PairReader((Class)rawType, actualTypeParam0, actualTypeParam1);
                  }
               } else if (actualTypeArguments.length == 1) {
                  Type itemType = actualTypeArguments[0];
                  Class itemClass = TypeUtils.getMapping(itemType);
                  if (rawType != Iterable.class
                     && rawType != Collection.class
                     && rawType != List.class
                     && rawType != AbstractCollection.class
                     && rawType != AbstractList.class
                     && rawType != ArrayList.class) {
                     if (rawType != Queue.class && rawType != Deque.class && rawType != AbstractSequentialList.class && rawType != LinkedList.class) {
                        if (rawType != Set.class && rawType != AbstractSet.class && rawType != EnumSet.class) {
                           if (rawType != NavigableSet.class && rawType != SortedSet.class) {
                              if (rawType != ConcurrentLinkedDeque.class
                                 && rawType != ConcurrentLinkedQueue.class
                                 && rawType != ConcurrentSkipListSet.class
                                 && rawType != LinkedHashSet.class
                                 && rawType != HashSet.class
                                 && rawType != TreeSet.class
                                 && rawType != CopyOnWriteArrayList.class) {
                                 String var10 = rawType.getTypeName();
                                 switch (var10) {
                                    case "com.google.common.collect.ImmutableList":
                                    case "com.google.common.collect.ImmutableSet":
                                    case "com.google.common.collect.SingletonImmutableSet":
                                       return ObjectReaderImplList.of(type, null, 0L);
                                    default:
                                       if (rawType == Optional.class) {
                                          return ObjectReaderImplOptional.of(type, null, null);
                                       }

                                       if (rawType == AtomicReference.class) {
                                          return new ObjectReaderImplAtomicReference(itemType);
                                       }

                                       if (itemType instanceof WildcardType) {
                                          return this.getObjectReader(provider, rawType);
                                       }

                                       return null;
                                 }
                              }

                              if (itemType == String.class) {
                                 return new ObjectReaderImplListStr((Class)rawType, (Class)rawType);
                              }

                              if (itemClass == Long.class) {
                                 return new ObjectReaderImplListInt64((Class)rawType, (Class)rawType);
                              }

                              return ObjectReaderImplList.of(type, null, 0L);
                           }

                           if (itemType == String.class) {
                              return new ObjectReaderImplListStr((Class)rawType, TreeSet.class);
                           }

                           if (itemClass == Long.class) {
                              return new ObjectReaderImplListInt64((Class)rawType, TreeSet.class);
                           }

                           return ObjectReaderImplList.of(type, null, 0L);
                        }

                        if (itemClass == String.class) {
                           return new ObjectReaderImplListStr((Class)rawType, HashSet.class);
                        }

                        if (itemClass == Long.class) {
                           return new ObjectReaderImplListInt64((Class)rawType, HashSet.class);
                        }

                        return ObjectReaderImplList.of(type, null, 0L);
                     }

                     if (itemClass == String.class) {
                        return new ObjectReaderImplListStr((Class)rawType, LinkedList.class);
                     }

                     if (itemClass == Long.class) {
                        return new ObjectReaderImplListInt64((Class)rawType, LinkedList.class);
                     }

                     return ObjectReaderImplList.of(type, null, 0L);
                  }

                  if (itemClass == String.class) {
                     return new ObjectReaderImplListStr((Class)rawType, ArrayList.class);
                  }

                  if (itemClass == Long.class) {
                     return new ObjectReaderImplListInt64((Class)rawType, ArrayList.class);
                  }

                  return ObjectReaderImplList.of(type, null, 0L);
               }

               return null;
            } else if (type instanceof GenericArrayType) {
               return new ObjectReaderImplGenericArray((GenericArrayType)type);
            } else {
               if (type instanceof WildcardType) {
                  Type[] upperBounds = ((WildcardType)type).getUpperBounds();
                  if (upperBounds.length == 1) {
                     return this.getObjectReader(provider, upperBounds[0]);
                  }
               }

               if (type == ParameterizedType.class) {
                  return ObjectReaders.ofReflect(ParameterizedTypeImpl.class);
               } else {
                  switch (typeName) {
                     case "java.sql.Time":
                        return JdbcSupport.createTimeReader((Class)type, null, null);
                     case "java.sql.Timestamp":
                        return JdbcSupport.createTimestampReader((Class)type, null, null);
                     case "java.sql.Date":
                        return JdbcSupport.createDateReader((Class)type, null, null);
                     case "java.util.RegularEnumSet":
                     case "java.util.JumboEnumSet":
                        return ObjectReaderImplList.of(type, TypeUtils.getClass(type), 0L);
                     case "org.joda.time.Chronology":
                        return JodaSupport.createChronologyReader((Class)type);
                     case "org.joda.time.LocalDate":
                        return JodaSupport.createLocalDateReader((Class)type);
                     case "org.joda.time.LocalDateTime":
                        return JodaSupport.createLocalDateTimeReader((Class)type);
                     case "org.joda.time.Instant":
                        return JodaSupport.createInstantReader((Class)type);
                     case "org.joda.time.DateTime":
                        return new ObjectReaderImplZonedDateTime(new JodaSupport.DateTimeFromZDT());
                     case "javax.money.CurrencyUnit":
                        return MoneySupport.createCurrencyUnitReader();
                     case "javax.money.MonetaryAmount":
                     case "javax.money.Money":
                        return MoneySupport.createMonetaryAmountReader();
                     case "javax.money.NumberValue":
                        return MoneySupport.createNumberValueReader();
                     case "java.net.InetSocketAddress":
                        return new ObjectReaderMisc((Class)type);
                     case "java.net.InetAddress":
                        return ObjectReaderImplValue.of((Class<InetAddress>)type, String.class, address -> {
                           try {
                              return InetAddress.getByName(address);
                           } catch (UnknownHostException var2x) {
                              throw new JSONException("create address error", var2x);
                           }
                        });
                     case "java.text.SimpleDateFormat":
                        return ObjectReaderImplValue.of((Class<SimpleDateFormat>)type, String.class, SimpleDateFormat::new);
                     case "java.lang.Throwable":
                     case "java.lang.Exception":
                     case "java.lang.IllegalStateException":
                     case "java.lang.RuntimeException":
                     case "java.io.IOException":
                     case "java.io.UncheckedIOException":
                        return new ObjectReaderException((Class)type);
                     case "java.nio.HeapByteBuffer":
                     case "java.nio.ByteBuffer":
                        return new ObjectReaderImplInt8ValueArray(ByteBuffer::wrap, null);
                     case "org.apache.commons.lang3.tuple.Pair":
                     case "org.apache.commons.lang3.tuple.ImmutablePair":
                        return new ApacheLang3Support.PairReader((Class)type, Object.class, Object.class);
                     case "com.google.common.collect.ImmutableList":
                     case "com.google.common.collect.ImmutableSet":
                     case "com.google.common.collect.SingletonImmutableSet":
                     case "com.google.common.collect.RegularImmutableSet":
                     case "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList":
                        return ObjectReaderImplList.of(type, null, 0L);
                     case "com.carrotsearch.hppc.ByteArrayList":
                     case "com.carrotsearch.hppc.ShortArrayList":
                     case "com.carrotsearch.hppc.IntArrayList":
                     case "com.carrotsearch.hppc.IntHashSet":
                     case "com.carrotsearch.hppc.LongArrayList":
                     case "com.carrotsearch.hppc.LongHashSet":
                     case "com.carrotsearch.hppc.CharArrayList":
                     case "com.carrotsearch.hppc.CharHashSet":
                     case "com.carrotsearch.hppc.FloatArrayList":
                     case "com.carrotsearch.hppc.DoubleArrayList":
                     case "gnu.trove.list.array.TByteArrayList":
                     case "gnu.trove.list.array.TCharArrayList":
                     case "gnu.trove.list.array.TShortArrayList":
                     case "gnu.trove.list.array.TIntArrayList":
                     case "gnu.trove.list.array.TLongArrayList":
                     case "gnu.trove.list.array.TFloatArrayList":
                     case "gnu.trove.list.array.TDoubleArrayList":
                     case "gnu.trove.set.hash.TByteHashSet":
                     case "gnu.trove.set.hash.TShortHashSet":
                     case "gnu.trove.set.hash.TIntHashSet":
                     case "gnu.trove.set.hash.TLongHashSet":
                     case "org.bson.types.Decimal128":
                        return LambdaMiscCodec.getObjectReader((Class)type);
                     case "java.awt.Color":
                        try {
                           Constructor constructor = ((Class)type).getConstructor(int.class, int.class, int.class, int.class);
                           return ObjectReaderCreator.INSTANCE.createObjectReaderNoneDefaultConstructor(constructor, "r", "g", "b", "alpha");
                        } catch (Throwable var13) {
                        }
                     default:
                        return null;
                  }
               }
            }
         }
      }
   }

   public static ObjectReader typedMap(Class mapType, Class instanceType, Type keyType, Type valueType) {
      return (ObjectReader)((keyType == null || keyType == String.class) && valueType == String.class
         ? new ObjectReaderImplMapString(mapType, instanceType, 0L)
         : new ObjectReaderImplMapTyped(mapType, instanceType, keyType, valueType, 0L, null));
   }

   public class ReaderAnnotationProcessor implements ObjectReaderAnnotationProcessor {
      @Override
      public void getBeanInfo(BeanInfo beanInfo, Class<?> objectClass) {
         Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
         if (mixInSource == null) {
            String typeName = objectClass.getName();
            if ("org.apache.commons.lang3.tuple.Triple".equals(typeName)) {
               mixInSource = ApacheLang3Support.TripleMixIn.class;
               ObjectReaderBaseModule.this.provider.mixIn(objectClass, ApacheLang3Support.TripleMixIn.class);
            }
         }

         if (mixInSource != null && mixInSource != objectClass) {
            beanInfo.mixIn = true;
            this.getBeanInfo(beanInfo, BeanUtils.getAnnotations(mixInSource));
            BeanUtils.staticMethod(mixInSource, method -> ObjectReaderBaseModule.this.getCreator(beanInfo, objectClass, method));
            BeanUtils.constructor(mixInSource, constructor -> ObjectReaderBaseModule.this.getCreator(beanInfo, objectClass, constructor));
         }

         Class seeAlsoClass = null;

         for (Class superClass = objectClass.getSuperclass();
            superClass != null && superClass != Object.class && superClass != Enum.class;
            superClass = superClass.getSuperclass()
         ) {
            BeanInfo superBeanInfo = new BeanInfo(JSONFactory.getDefaultObjectReaderProvider());
            this.getBeanInfo(superBeanInfo, superClass);
            if (superBeanInfo.seeAlso != null) {
               boolean inSeeAlso = false;

               for (Class seeAlsoItem : superBeanInfo.seeAlso) {
                  if (seeAlsoItem == objectClass) {
                     inSeeAlso = true;
                     break;
                  }
               }

               if (!inSeeAlso) {
                  seeAlsoClass = superClass;
               }
            }
         }

         if (seeAlsoClass != null) {
            this.getBeanInfo(beanInfo, seeAlsoClass);
         }

         Annotation[] annotations = BeanUtils.getAnnotations(objectClass);
         this.getBeanInfo(beanInfo, annotations);

         for (Annotation annotation : annotations) {
            boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
            Class<? extends Annotation> annotationType = annotation.annotationType();
            String annotationTypeName = annotationType.getName();
            switch (annotationTypeName) {
               case "com.alibaba.fastjson.annotation.JSONType":
                  this.getBeanInfo1x(beanInfo, annotation);
                  break;
               case "com.fasterxml.jackson.annotation.JsonTypeInfo":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonTypeInfo(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.databind.annotation.JsonDeserialize":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonDeserializer(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonTypeName":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonTypeName(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonFormat":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonFormat(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonInclude":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonInclude(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonSubTypes":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonSubTypes(beanInfo, annotation);
                  }
                  break;
               case "kotlin.Metadata":
                  beanInfo.kotlin = true;
            }
         }

         BeanUtils.staticMethod(objectClass, method -> ObjectReaderBaseModule.this.getCreator(beanInfo, objectClass, method));
         BeanUtils.constructor(objectClass, constructor -> ObjectReaderBaseModule.this.getCreator(beanInfo, objectClass, constructor));
         if (beanInfo.creatorConstructor == null && (beanInfo.readerFeatures & JSONReader.Feature.FieldBased.mask) == 0L && beanInfo.kotlin) {
            KotlinUtils.getConstructor(objectClass, beanInfo);
         }
      }

      private void processJacksonJsonSubTypes(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("value".equals(name)) {
                  Object[] value = (Object[])result;
                  if (value.length != 0) {
                     beanInfo.seeAlso = new Class[value.length];
                     beanInfo.seeAlsoNames = new String[value.length];

                     for (int i = 0; i < value.length; i++) {
                        Annotation subTypeAnn = (Annotation)value[i];
                        BeanUtils.processJacksonJsonSubTypesType(beanInfo, i, subTypeAnn);
                     }
                  }
               }
            } catch (Throwable var8) {
            }
         });
      }

      private void processJacksonJsonDeserializer(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("using".equals(name) || "contentUsing".equals(name)) {
                  Class using = this.processUsing((Class)result);
                  if (using != null) {
                     beanInfo.deserializer = using;
                  }
               } else if ("builder".equals(name)) {
                  this.processBuilder(beanInfo, (Class)result);
               }
            } catch (Throwable var7) {
            }
         });
      }

      private void processJacksonJsonTypeInfo(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("property".equals(name)) {
                  String value = (String)result;
                  if (!value.isEmpty()) {
                     beanInfo.typeKey = value;
                     beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.SupportAutoType.mask;
                  }
               }
            } catch (Throwable var6) {
            }
         });
      }

      private void getBeanInfo(BeanInfo beanInfo, Annotation[] annotations) {
         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            JSONType jsonType = BeanUtils.findAnnotation(annotation, JSONType.class);
            if (jsonType != null) {
               this.getBeanInfo1x(beanInfo, annotation);
               if (jsonType == annotation) {
                  continue;
               }
            }

            if (annotationType == JSONCompiler.class) {
               JSONCompiler compiler = (JSONCompiler)annotation;
               if (compiler.value() == JSONCompiler.CompilerOption.LAMBDA) {
                  beanInfo.readerFeatures |= 18014398509481984L;
               }
            }
         }
      }

      void getBeanInfo1x(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(
            annotationClass,
            m -> {
               String name = m.getName();

               try {
                  Object result = m.invoke(annotation);
                  switch (name) {
                     case "seeAlso":
                        Class<?>[] classes = (Class<?>[])result;
                        if (classes.length != 0) {
                           beanInfo.seeAlso = classes;
                           beanInfo.seeAlsoNames = new String[classes.length];

                           for (int i = 0; i < classes.length; i++) {
                              Class<?> item = classes[i];
                              BeanInfo itemBeanInfo = new BeanInfo(JSONFactory.getDefaultObjectReaderProvider());
                              this.processSeeAlsoAnnotation(itemBeanInfo, item);
                              String typeNamex = itemBeanInfo.typeName;
                              if (typeNamex == null || typeNamex.isEmpty()) {
                                 typeNamex = item.getSimpleName();
                              }

                              beanInfo.seeAlsoNames[i] = typeNamex;
                           }

                           beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.SupportAutoType.mask;
                        }
                        break;
                     case "seeAlsoDefault":
                        Class<?> seeAlsoDefault = (Class<?>)result;
                        if (seeAlsoDefault != Void.class) {
                           beanInfo.seeAlsoDefault = seeAlsoDefault;
                        }
                     case "typeKey":
                        String jsonTypeKey = (String)result;
                        if (!jsonTypeKey.isEmpty()) {
                           beanInfo.typeKey = jsonTypeKey;
                        }
                        break;
                     case "typeName":
                        String typeName = (String)result;
                        if (!typeName.isEmpty()) {
                           beanInfo.typeName = typeName;
                        }
                        break;
                     case "rootName":
                        String rootName = (String)result;
                        if (!rootName.isEmpty()) {
                           beanInfo.rootName = rootName;
                        }
                        break;
                     case "naming":
                        Enum naming = (Enum)result;
                        beanInfo.namingStrategy = naming.name();
                        break;
                     case "ignores":
                        String[] ignores = (String[])result;
                        if (ignores.length > 0) {
                           beanInfo.ignores = ignores;
                        }
                        break;
                     case "orders":
                        String[] fields = (String[])result;
                        if (fields.length != 0) {
                           beanInfo.orders = fields;
                        }
                        break;
                     case "schema":
                        String schema = (String)result;
                        schema = schema.trim();
                        if (!schema.isEmpty()) {
                           beanInfo.schema = schema;
                        }
                        break;
                     case "deserializer":
                        Class<?> deserializer = (Class<?>)result;
                        if (ObjectReader.class.isAssignableFrom(deserializer)) {
                           beanInfo.deserializer = deserializer;
                        }
                        break;
                     case "parseFeatures":
                        Enum[] features = (Enum[])result;

                        for (Enum feature : features) {
                           String var13 = feature.name();
                           switch (var13) {
                              case "SupportAutoType":
                                 beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.SupportAutoType.mask;
                                 break;
                              case "SupportArrayToBean":
                                 beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.SupportArrayToBean.mask;
                                 break;
                              case "InitStringFieldAsEmpty":
                                 beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.InitStringFieldAsEmpty.mask;
                              case "TrimStringFieldValue":
                           }
                        }
                        break;
                     case "deserializeFeatures":
                        JSONReader.Feature[] features = (JSONReader.Feature[])result;

                        for (JSONReader.Feature feature : features) {
                           beanInfo.readerFeatures = beanInfo.readerFeatures | feature.mask;
                        }
                        break;
                     case "builder":
                        this.processBuilder(beanInfo, (Class)result);
                        break;
                     case "deserializeUsing":
                        Class<?> deserializeUsing = (Class<?>)result;
                        if (ObjectReader.class.isAssignableFrom(deserializeUsing)) {
                           beanInfo.deserializer = deserializeUsing;
                        }
                        break;
                     case "autoTypeBeforeHandler":
                     case "autoTypeCheckHandler":
                        Class<?> autoTypeCheckHandler = (Class<?>)result;
                        if (autoTypeCheckHandler != JSONReader.AutoTypeBeforeHandler.class
                           && JSONReader.AutoTypeBeforeHandler.class.isAssignableFrom(autoTypeCheckHandler)) {
                           beanInfo.autoTypeBeforeHandler = (Class<? extends JSONReader.AutoTypeBeforeHandler>)autoTypeCheckHandler;
                        }
                        break;
                     case "disableReferenceDetect":
                        if (Boolean.TRUE.equals(result)) {
                           beanInfo.readerFeatures |= 144115188075855872L;
                        }
                        break;
                     case "disableArrayMapping":
                        if (Boolean.TRUE.equals(result)) {
                           beanInfo.readerFeatures |= 288230376151711744L;
                        }
                        break;
                     case "disableAutoType":
                        if (Boolean.TRUE.equals(result)) {
                           beanInfo.readerFeatures |= 576460752303423488L;
                        }
                        break;
                     case "disableJSONB":
                        if (Boolean.TRUE.equals(result)) {
                           beanInfo.readerFeatures |= 1152921504606846976L;
                        }
                  }
               } catch (Throwable var15) {
               }
            }
         );
      }

      private void processBuilder(BeanInfo beanInfo, Class result) {
         Class<?> builderClass = result;
         if (result != void.class && result != Void.class) {
            beanInfo.builder = result;

            for (Annotation builderAnnotation : BeanUtils.getAnnotations(result)) {
               Class<? extends Annotation> builderAnnotationClass = builderAnnotation.annotationType();
               String builderAnnotationName = builderAnnotationClass.getName();
               if (!"com.alibaba.fastjson.annotation.JSONPOJOBuilder".equals(builderAnnotationName)
                  && !"com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder".equals(builderAnnotationName)) {
                  JSONBuilder jsonBuilder = BeanUtils.findAnnotation(builderClass, JSONBuilder.class);
                  if (jsonBuilder != null) {
                     String buildMethodName = jsonBuilder.buildMethod();
                     beanInfo.buildMethod = BeanUtils.buildMethod(builderClass, buildMethodName);
                     String withPrefix = jsonBuilder.withPrefix();
                     if (!withPrefix.isEmpty()) {
                        beanInfo.builderWithPrefix = withPrefix;
                     }
                  }
               } else {
                  ObjectReaderBaseModule.this.getBeanInfo1xJSONPOJOBuilder(beanInfo, builderClass, builderAnnotation, builderAnnotationClass);
               }
            }

            if (beanInfo.buildMethod == null) {
               beanInfo.buildMethod = BeanUtils.buildMethod(builderClass, "build");
            }

            if (beanInfo.buildMethod == null) {
               beanInfo.buildMethod = BeanUtils.buildMethod(builderClass, "create");
            }
         }
      }

      private void processSeeAlsoAnnotation(BeanInfo beanInfo, Class<?> objectClass) {
         Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
         if (mixInSource == null) {
            String typeName = objectClass.getName();
            if ("org.apache.commons.lang3.tuple.Triple".equals(typeName)) {
               mixInSource = ApacheLang3Support.TripleMixIn.class;
               ObjectReaderBaseModule.this.provider.mixIn(objectClass, ApacheLang3Support.TripleMixIn.class);
            }
         }

         if (mixInSource != null && mixInSource != objectClass) {
            beanInfo.mixIn = true;
            this.processSeeAlsoAnnotation(beanInfo, BeanUtils.getAnnotations(mixInSource));
         }

         this.processSeeAlsoAnnotation(beanInfo, BeanUtils.getAnnotations(objectClass));
      }

      private void processSeeAlsoAnnotation(BeanInfo beanInfo, Annotation[] annotations) {
         for (Annotation annotation : annotations) {
            Class<? extends Annotation> itemAnnotationType = annotation.annotationType();
            BeanUtils.annotationMethods(itemAnnotationType, m -> {
               String name = m.getName();

               try {
                  Object result = m.invoke(annotation);
                  if ("typeName".equals(name)) {
                     String typeName = (String)result;
                     if (!typeName.isEmpty()) {
                        beanInfo.typeName = typeName;
                     }
                  }
               } catch (Throwable var6x) {
               }
            });
         }
      }

      @Override
      public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Constructor constructor, int paramIndex, Parameter parameter) {
         if (objectClass != null) {
            Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null && mixInSource != objectClass) {
               Constructor mixInConstructor = null;

               try {
                  mixInConstructor = mixInSource.getDeclaredConstructor(constructor.getParameterTypes());
               } catch (NoSuchMethodException var11) {
               }

               if (mixInConstructor != null) {
                  Parameter mixInParam = mixInConstructor.getParameters()[paramIndex];
                  this.processAnnotation(fieldInfo, BeanUtils.getAnnotations(mixInParam));
               }
            }
         }

         boolean staticClass = Modifier.isStatic(constructor.getDeclaringClass().getModifiers());
         Annotation[] annotations = null;
         if (staticClass) {
            try {
               annotations = BeanUtils.getAnnotations(parameter);
            } catch (ArrayIndexOutOfBoundsException var10) {
            }
         } else {
            Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
            int paIndex;
            if (parameterAnnotations.length == constructor.getParameterCount()) {
               paIndex = paramIndex;
            } else {
               paIndex = paramIndex - 1;
            }

            if (paIndex >= 0 && paIndex < parameterAnnotations.length) {
               annotations = parameterAnnotations[paIndex];
            }
         }

         if (annotations != null && annotations.length > 0) {
            this.processAnnotation(fieldInfo, annotations);
         }
      }

      @Override
      public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Method method, int paramIndex, Parameter parameter) {
         if (objectClass != null) {
            Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null && mixInSource != objectClass) {
               Method mixInMethod = null;

               try {
                  mixInMethod = mixInSource.getMethod(method.getName(), method.getParameterTypes());
               } catch (NoSuchMethodException var9) {
               }

               if (mixInMethod != null) {
                  Parameter mixInParam = mixInMethod.getParameters()[paramIndex];
                  this.processAnnotation(fieldInfo, BeanUtils.getAnnotations(mixInParam));
               }
            }
         }

         this.processAnnotation(fieldInfo, BeanUtils.getAnnotations(parameter));
      }

      @Override
      public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Field field) {
         if (objectClass != null) {
            Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null && mixInSource != objectClass) {
               Field mixInField = null;

               try {
                  mixInField = mixInSource.getDeclaredField(field.getName());
               } catch (Exception var7) {
               }

               if (mixInField != null) {
                  this.getFieldInfo(fieldInfo, mixInSource, mixInField);
               }
            }
         }

         Annotation[] annotations = BeanUtils.getAnnotations(field);
         this.processAnnotation(fieldInfo, annotations);
      }

      @Override
      public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Method method) {
         String methodName = method.getName();
         if (objectClass != null) {
            Class superclass = objectClass.getSuperclass();
            Method supperMethod = BeanUtils.getMethod(superclass, method);
            if (supperMethod != null) {
               this.getFieldInfo(fieldInfo, superclass, supperMethod);
            }

            Class[] interfaces = objectClass.getInterfaces();

            for (Class i : interfaces) {
               if (i != Serializable.class) {
                  Method interfaceMethod = BeanUtils.getMethod(i, method);
                  if (interfaceMethod != null) {
                     this.getFieldInfo(fieldInfo, superclass, interfaceMethod);
                  }
               }
            }

            Class mixInSource = ObjectReaderBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null && mixInSource != objectClass) {
               Method mixInMethod = null;

               try {
                  mixInMethod = mixInSource.getDeclaredMethod(methodName, method.getParameterTypes());
               } catch (Exception var17) {
               }

               if (mixInMethod != null) {
                  this.getFieldInfo(fieldInfo, mixInSource, mixInMethod);
               }
            }
         }

         String jsonFieldName = null;
         Annotation[] annotations = BeanUtils.getAnnotations(method);

         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            JSONField jsonField = BeanUtils.findAnnotation(annotation, JSONField.class);
            if (jsonField != null) {
               this.getFieldInfo(fieldInfo, jsonField);
               jsonFieldName = jsonField.name();
               if (jsonField == annotation) {
                  continue;
               }
            }

            if (annotationType == JSONCompiler.class) {
               JSONCompiler compiler = (JSONCompiler)annotation;
               if (compiler.value() == JSONCompiler.CompilerOption.LAMBDA) {
                  fieldInfo.features |= 18014398509481984L;
               }
            }

            boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
            String annotationTypeName = annotationType.getName();
            switch (annotationTypeName) {
               case "com.fasterxml.jackson.annotation.JsonIgnore":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonIgnore(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.databind.annotation.JsonDeserialize":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonDeserialize(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonFormat":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonFormat(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonAnySetter":
                  if (useJacksonAnnotation) {
                     fieldInfo.features |= 562949953421312L;
                  }
                  break;
               case "com.alibaba.fastjson.annotation.JSONField":
                  this.processJSONField1x(fieldInfo, annotation);
                  break;
               case "com.fasterxml.jackson.annotation.JsonProperty":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonProperty(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonAlias":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonAlias(fieldInfo, annotation);
                  }
                  break;
               case "com.google.gson.annotations.SerializedName":
                  if (JSONFactory.isUseGsonAnnotation()) {
                     BeanUtils.processGsonSerializedName(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonInclude":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonInclude(fieldInfo, annotation);
                  }
            }
         }

         String fieldName;
         if (methodName.startsWith("set")) {
            fieldName = BeanUtils.setterName(methodName, null);
         } else {
            fieldName = BeanUtils.getterName(methodName, null);
         }

         String fieldName1;
         String fieldName2;
         char c0;
         char c1;
         if (fieldName.length() <= 1
            || (c0 = fieldName.charAt(0)) < 'A'
            || c0 > 'Z'
            || (c1 = fieldName.charAt(1)) < 'A'
            || c1 > 'Z'
            || jsonFieldName != null && !jsonFieldName.isEmpty()) {
            fieldName1 = null;
            fieldName2 = null;
         } else {
            char[] chars = fieldName.toCharArray();
            chars[0] = (char)(chars[0] + ' ');
            fieldName1 = new String(chars);
            chars[1] = (char)(chars[1] + ' ');
            fieldName2 = new String(chars);
         }

         BeanUtils.declaredFields(objectClass, field -> {
            if (field.getName().equals(fieldName)) {
               int modifiers = field.getModifiers();
               if (!Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                  this.getFieldInfo(fieldInfo, objectClass, field);
               }

               fieldInfo.features |= 4503599627370496L;
            } else if (field.getName().equals(fieldName1)) {
               int modifiers = field.getModifiers();
               if (!Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                  this.getFieldInfo(fieldInfo, objectClass, field);
               }

               fieldInfo.features |= 4503599627370496L;
            } else if (field.getName().equals(fieldName2)) {
               int modifiers = field.getModifiers();
               if (!Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                  this.getFieldInfo(fieldInfo, objectClass, field);
               }

               fieldInfo.features |= 4503599627370496L;
            }
         });
         if (fieldName1 != null && fieldInfo.fieldName == null && fieldInfo.alternateNames == null) {
            fieldInfo.alternateNames = new String[]{fieldName1, fieldName2};
         }
      }

      private void processAnnotation(FieldInfo fieldInfo, Annotation[] annotations) {
         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            JSONField jsonField = BeanUtils.findAnnotation(annotation, JSONField.class);
            if (jsonField != null) {
               this.getFieldInfo(fieldInfo, jsonField);
               if (jsonField == annotation) {
                  continue;
               }
            }

            if (annotationType == JSONCompiler.class) {
               JSONCompiler compiler = (JSONCompiler)annotation;
               if (compiler.value() == JSONCompiler.CompilerOption.LAMBDA) {
                  fieldInfo.features |= 18014398509481984L;
               }
            }

            boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
            String annotationTypeName = annotationType.getName();
            switch (annotationTypeName) {
               case "com.fasterxml.jackson.annotation.JsonIgnore":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonIgnore(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonAnyGetter":
                  if (useJacksonAnnotation) {
                     fieldInfo.features |= 562949953421312L;
                  }
                  break;
               case "com.alibaba.fastjson.annotation.JSONField":
                  this.processJSONField1x(fieldInfo, annotation);
                  break;
               case "com.fasterxml.jackson.annotation.JsonProperty":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonProperty(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonFormat":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonFormat(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.databind.annotation.JsonDeserialize":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonDeserialize(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonAlias":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonAlias(fieldInfo, annotation);
                  }
                  break;
               case "com.google.gson.annotations.SerializedName":
                  if (JSONFactory.isUseGsonAnnotation()) {
                     BeanUtils.processGsonSerializedName(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonSetter":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonSetter(fieldInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonManagedReference":
                  if (useJacksonAnnotation) {
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.ReferenceDetection.mask;
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonBackReference":
                  if (useJacksonAnnotation) {
                     fieldInfo.features |= 2305843009213693952L;
                  }
            }
         }
      }

      private void processJacksonJsonDeserialize(FieldInfo fieldInfo, Annotation annotation) {
         if (JSONFactory.isUseJacksonAnnotation()) {
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
            BeanUtils.annotationMethods(annotationClass, m -> {
               String name = m.getName();

               try {
                  Object result = m.invoke(annotation);
                  switch (name) {
                     case "using":
                        Class using = this.processUsing((Class)result);
                        if (using != null) {
                           fieldInfo.readUsing = using;
                        }
                        break;
                     case "keyUsing":
                        Class keyUsing = this.processUsing((Class)result);
                        if (keyUsing != null) {
                           fieldInfo.keyUsing = keyUsing;
                        }
                        break;
                     case "valueUsing":
                        Class valueUsing = this.processUsing((Class)result);
                        if (valueUsing != null) {
                           fieldInfo.keyUsing = valueUsing;
                        }
                  }
               } catch (Throwable var9) {
               }
            });
         }
      }

      private Class processUsing(Class using) {
         String usingName = using.getName();
         String noneClassName0 = "com.fasterxml.jackson.databind.JsonDeserializer$None";
         return !noneClassName0.equals(usingName) && ObjectReader.class.isAssignableFrom(using) ? using : null;
      }

      private void processJacksonJsonProperty(FieldInfo fieldInfo, Annotation annotation) {
         if (JSONFactory.isUseJacksonAnnotation()) {
            Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
            BeanUtils.annotationMethods(annotationClass, m -> {
               String name = m.getName();

               try {
                  Object result = m.invoke(annotation);
                  switch (name) {
                     case "value":
                        String value = (String)result;
                        if (!value.isEmpty() && (fieldInfo.fieldName == null || fieldInfo.fieldName.isEmpty())) {
                           fieldInfo.fieldName = value;
                        }
                        break;
                     case "access":
                        String access = ((Enum)result).name();
                        fieldInfo.ignore = "READ_ONLY".equals(access);
                        break;
                     case "required":
                        boolean required = (Boolean)result;
                        if (required) {
                           fieldInfo.required = true;
                        }
                  }
               } catch (Throwable var8) {
               }
            });
         }
      }

      private void processJacksonJsonSetter(FieldInfo fieldInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               byte var6 = -1;
               switch (name.hashCode()) {
                  case 111972721:
                     if (name.equals("value")) {
                        var6 = 0;
                     }
                  default:
                     switch (var6) {
                        case 0:
                           String value = (String)result;
                           if (!value.isEmpty()) {
                              fieldInfo.fieldName = value;
                           }
                     }
               }
            } catch (Throwable var8) {
            }
         });
      }

      private void processJacksonJsonAlias(FieldInfo fieldInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("value".equals(name)) {
                  String[] values = (String[])result;
                  if (values.length != 0) {
                     fieldInfo.alternateNames = values;
                  }
               }
            } catch (Throwable var6) {
            }
         });
      }

      private void processJSONField1x(FieldInfo fieldInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               switch (name) {
                  case "name":
                     String valuex = (String)result;
                     if (!valuex.isEmpty()) {
                        fieldInfo.fieldName = valuex;
                     }
                     break;
                  case "format":
                     String format = (String)result;
                     if (!format.isEmpty()) {
                        format = format.trim();
                        if (format.indexOf(84) != -1 && !format.contains("'T'")) {
                           format = format.replaceAll("T", "'T'");
                        }

                        fieldInfo.format = format;
                     }
                     break;
                  case "label":
                     String label = (String)result;
                     if (!label.isEmpty()) {
                        fieldInfo.label = label;
                     }
                     break;
                  case "defaultValue":
                     String value = (String)result;
                     if (!value.isEmpty()) {
                        fieldInfo.defaultValue = value;
                     }
                     break;
                  case "alternateNames":
                     String[] alternateNames = (String[])result;
                     if (alternateNames.length != 0) {
                        if (fieldInfo.alternateNames == null) {
                           fieldInfo.alternateNames = alternateNames;
                        } else {
                           Set<String> nameSet = new LinkedHashSet<>();
                           nameSet.addAll(Arrays.asList(alternateNames));
                           nameSet.addAll(Arrays.asList(fieldInfo.alternateNames));
                           fieldInfo.alternateNames = nameSet.toArray(new String[nameSet.size()]);
                        }
                     }
                     break;
                  case "ordinal":
                     int ordinal = (Integer)result;
                     if (ordinal != 0) {
                        fieldInfo.ordinal = ordinal;
                     }
                     break;
                  case "deserialize":
                     boolean serialize = (Boolean)result;
                     if (!serialize) {
                        fieldInfo.ignore = true;
                     }
                     break;
                  case "parseFeatures":
                     Enum[] features = (Enum[])result;

                     for (Enum feature : features) {
                        String var12 = feature.name();
                        switch (var12) {
                           case "SupportAutoType":
                              fieldInfo.features = fieldInfo.features | JSONReader.Feature.SupportAutoType.mask;
                              break;
                           case "SupportArrayToBean":
                              fieldInfo.features = fieldInfo.features | JSONReader.Feature.SupportArrayToBean.mask;
                              break;
                           case "InitStringFieldAsEmpty":
                              fieldInfo.features = fieldInfo.features | JSONReader.Feature.InitStringFieldAsEmpty.mask;
                        }
                     }
                     break;
                  case "deserializeUsing":
                     Class<?> deserializeUsing = (Class<?>)result;
                     if (ObjectReader.class.isAssignableFrom(deserializeUsing)) {
                        fieldInfo.readUsing = deserializeUsing;
                     }
                     break;
                  case "unwrapped":
                     boolean unwrapped = (Boolean)result;
                     if (unwrapped) {
                        fieldInfo.features |= 562949953421312L;
                     }
               }
            } catch (Throwable var14) {
            }
         });
      }

      private void getFieldInfo(FieldInfo fieldInfo, JSONField jsonField) {
         if (jsonField != null) {
            String jsonFieldName = jsonField.name();
            if (!jsonFieldName.isEmpty()) {
               fieldInfo.fieldName = jsonFieldName;
            }

            String jsonFieldFormat = jsonField.format();
            if (!jsonFieldFormat.isEmpty()) {
               jsonFieldFormat = jsonFieldFormat.trim();
               if (jsonFieldFormat.indexOf(84) != -1 && !jsonFieldFormat.contains("'T'")) {
                  jsonFieldFormat = jsonFieldFormat.replaceAll("T", "'T'");
               }

               fieldInfo.format = jsonFieldFormat;
            }

            String label = jsonField.label();
            if (!label.isEmpty()) {
               label = label.trim();
               fieldInfo.label = label;
            }

            String defaultValue = jsonField.defaultValue();
            if (!defaultValue.isEmpty()) {
               fieldInfo.defaultValue = defaultValue;
            }

            String locale = jsonField.locale();
            if (!locale.isEmpty()) {
               String[] parts = locale.split("_");
               if (parts.length == 2) {
                  fieldInfo.locale = new Locale(parts[0], parts[1]);
               }
            }

            String[] alternateNames = jsonField.alternateNames();
            if (alternateNames.length != 0) {
               if (fieldInfo.alternateNames == null) {
                  fieldInfo.alternateNames = alternateNames;
               } else {
                  Set<String> nameSet = new LinkedHashSet<>();
                  nameSet.addAll(Arrays.asList(alternateNames));
                  nameSet.addAll(Arrays.asList(fieldInfo.alternateNames));
                  fieldInfo.alternateNames = nameSet.toArray(new String[nameSet.size()]);
               }
            }

            boolean ignore = !jsonField.deserialize();
            if (!fieldInfo.ignore) {
               fieldInfo.ignore = ignore;
            }

            for (JSONReader.Feature feature : jsonField.deserializeFeatures()) {
               fieldInfo.features = fieldInfo.features | feature.mask;
               if (fieldInfo.ignore && !ignore && feature == JSONReader.Feature.FieldBased) {
                  fieldInfo.ignore = false;
               }
            }

            int ordinal = jsonField.ordinal();
            if (ordinal != 0) {
               fieldInfo.ordinal = ordinal;
            }

            boolean value = jsonField.value();
            if (value) {
               fieldInfo.features |= 281474976710656L;
            }

            if (jsonField.unwrapped()) {
               fieldInfo.features |= 562949953421312L;
            }

            if (jsonField.required()) {
               fieldInfo.required = true;
            }

            String schema = jsonField.schema().trim();
            if (!schema.isEmpty()) {
               fieldInfo.schema = schema;
            }

            Class deserializeUsing = jsonField.deserializeUsing();
            if (ObjectReader.class.isAssignableFrom(deserializeUsing)) {
               fieldInfo.readUsing = deserializeUsing;
            }

            String keyName = jsonField.arrayToMapKey().trim();
            if (!keyName.isEmpty()) {
               fieldInfo.arrayToMapKey = keyName;
            }

            Class<?> arrayToMapDuplicateHandler = jsonField.arrayToMapDuplicateHandler();
            if (arrayToMapDuplicateHandler != Void.class) {
               fieldInfo.arrayToMapDuplicateHandler = arrayToMapDuplicateHandler;
            }
         }
      }
   }
}
