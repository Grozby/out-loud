package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONPObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONCompiler;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.modules.ObjectWriterAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.support.money.MoneySupport;
import com.alibaba.fastjson2.util.ApacheLang3Support;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.JdbcSupport;
import com.alibaba.fastjson2.util.JodaSupport;
import com.alibaba.fastjson2.util.KotlinUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.File;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

public class ObjectWriterBaseModule implements ObjectWriterModule {
   static ObjectWriterAdapter STACK_TRACE_ELEMENT_WRITER;
   final ObjectWriterProvider provider;
   final ObjectWriterBaseModule.WriterAnnotationProcessor annotationProcessor;

   public ObjectWriterBaseModule(ObjectWriterProvider provider) {
      this.provider = provider;
      this.annotationProcessor = new ObjectWriterBaseModule.WriterAnnotationProcessor();
   }

   @Override
   public ObjectWriterProvider getProvider() {
      return this.provider;
   }

   @Override
   public ObjectWriterAnnotationProcessor getAnnotationProcessor() {
      return this.annotationProcessor;
   }

   ObjectWriter getExternalObjectWriter(String className, Class objectClass) {
      switch (className) {
         case "java.sql.Time":
            return JdbcSupport.createTimeWriter(null);
         case "java.sql.Timestamp":
            return JdbcSupport.createTimestampWriter(objectClass, null);
         case "org.joda.time.chrono.GregorianChronology":
            return JodaSupport.createGregorianChronologyWriter(objectClass);
         case "org.joda.time.chrono.ISOChronology":
            return JodaSupport.createISOChronologyWriter(objectClass);
         case "org.joda.time.LocalDate":
            return JodaSupport.createLocalDateWriter(objectClass, null);
         case "org.joda.time.LocalDateTime":
            return JodaSupport.createLocalDateTimeWriter(objectClass, null);
         case "org.joda.time.DateTime":
            return new ObjectWriterImplZonedDateTime(null, null, new JodaSupport.DateTime2ZDT());
         default:
            return JdbcSupport.isClob(objectClass) ? JdbcSupport.createClobWriter(objectClass) : null;
      }
   }

   @Override
   public ObjectWriter getObjectWriter(Type objectType, Class objectClass) {
      if (objectType == String.class) {
         return ObjectWriterImplString.INSTANCE;
      } else {
         if (objectClass == null) {
            if (objectType instanceof Class) {
               objectClass = (Class)objectType;
            } else {
               objectClass = TypeUtils.getMapping(objectType);
            }
         }

         String className = objectClass.getName();
         ObjectWriter externalObjectWriter = this.getExternalObjectWriter(className, objectClass);
         if (externalObjectWriter != null) {
            return externalObjectWriter;
         } else {
            switch (className) {
               case "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList":
               case "com.google.common.collect.AbstractMapBasedMultimap$WrappedSet":
                  return null;
               case "org.javamoney.moneta.internal.JDKCurrencyAdapter":
                  return ObjectWriterImplToString.INSTANCE;
               case "com.fasterxml.jackson.databind.node.ObjectNode":
                  return ObjectWriterImplToString.DIRECT;
               case "org.javamoney.moneta.Money":
                  return MoneySupport.createMonetaryAmountWriter();
               case "org.javamoney.moneta.spi.DefaultNumberValue":
                  return MoneySupport.createNumberValueWriter();
               case "net.sf.json.JSONNull":
               case "java.net.Inet4Address":
               case "java.net.Inet6Address":
               case "java.net.InetSocketAddress":
               case "java.text.SimpleDateFormat":
               case "java.util.regex.Pattern":
               case "com.fasterxml.jackson.databind.node.ArrayNode":
                  return ObjectWriterMisc.INSTANCE;
               case "org.apache.commons.lang3.tuple.Pair":
               case "org.apache.commons.lang3.tuple.MutablePair":
               case "org.apache.commons.lang3.tuple.ImmutablePair":
                  return new ApacheLang3Support.PairWriter(objectClass);
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
               case "com.carrotsearch.hppc.BitSet":
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
               case "gnu.trove.stack.array.TByteArrayStack":
               case "org.bson.types.Decimal128":
                  return LambdaMiscCodec.getObjectWriter(objectType, objectClass);
               case "java.nio.HeapByteBuffer":
               case "java.nio.DirectByteBuffer":
                  return new ObjectWriterImplInt8ValueArray(o -> ((ByteBuffer)o).array());
               case "java.awt.Color":
                  try {
                     List<FieldWriter> fieldWriters = Arrays.asList(
                        ObjectWriters.fieldWriter("r", objectClass.getMethod("getRed")),
                        ObjectWriters.fieldWriter("g", objectClass.getMethod("getGreen")),
                        ObjectWriters.fieldWriter("b", objectClass.getMethod("getBlue")),
                        ObjectWriters.fieldWriter("alpha", objectClass.getMethod("getAlpha"))
                     );
                     return new ObjectWriter4(objectClass, null, null, 0L, fieldWriters);
                  } catch (NoSuchMethodException var9) {
                  }
               default:
                  if (objectType instanceof ParameterizedType) {
                     ParameterizedType parameterizedType = (ParameterizedType)objectType;
                     Type rawType = parameterizedType.getRawType();
                     Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                     if (rawType == List.class || rawType == ArrayList.class) {
                        if (actualTypeArguments.length == 1 && actualTypeArguments[0] == String.class) {
                           return ObjectWriterImplListStr.INSTANCE;
                        }

                        objectType = rawType;
                     }

                     if (Map.class.isAssignableFrom(objectClass)) {
                        return ObjectWriterImplMap.of(objectType, objectClass);
                     }

                     if (objectClass == Optional.class && actualTypeArguments.length == 1) {
                        return new ObjectWriterImplOptional(actualTypeArguments[0], null, null);
                     }
                  }

                  if (objectType == LinkedList.class) {
                     return ObjectWriterImplList.INSTANCE;
                  } else if (objectType == ArrayList.class || objectType == List.class || List.class.isAssignableFrom(objectClass)) {
                     return ObjectWriterImplList.INSTANCE;
                  } else if (Collection.class.isAssignableFrom(objectClass)) {
                     return ObjectWriterImplCollection.INSTANCE;
                  } else if (BeanUtils.isExtendedMap(objectClass)) {
                     return null;
                  } else if (Map.class.isAssignableFrom(objectClass)) {
                     return ObjectWriterImplMap.of(objectClass);
                  } else if (Entry.class.isAssignableFrom(objectClass)) {
                     return ObjectWriterImplMapEntry.INSTANCE;
                  } else if (Path.class.isAssignableFrom(objectClass)) {
                     return ObjectWriterImplToString.INSTANCE;
                  } else if (objectType == Integer.class) {
                     return ObjectWriterImplInt32.INSTANCE;
                  } else if (objectType == AtomicInteger.class) {
                     return ObjectWriterImplAtomicInteger.INSTANCE;
                  } else if (objectType == Byte.class) {
                     return ObjectWriterImplInt8.INSTANCE;
                  } else if (objectType == Short.class) {
                     return ObjectWriterImplInt16.INSTANCE;
                  } else if (objectType == Long.class) {
                     return ObjectWriterImplInt64.INSTANCE;
                  } else if (objectType == AtomicLong.class) {
                     return ObjectWriterImplAtomicLong.INSTANCE;
                  } else if (objectType == AtomicReference.class) {
                     return ObjectWriterImplAtomicReference.INSTANCE;
                  } else if (objectType == Float.class) {
                     return ObjectWriterImplFloat.INSTANCE;
                  } else if (objectType == Double.class) {
                     return ObjectWriterImplDouble.INSTANCE;
                  } else if (objectType == BigInteger.class) {
                     return ObjectWriterBigInteger.INSTANCE;
                  } else if (objectType == BigDecimal.class) {
                     return ObjectWriterImplBigDecimal.INSTANCE;
                  } else if (objectType == BitSet.class) {
                     return ObjectWriterImplBitSet.INSTANCE;
                  } else if (objectType == OptionalInt.class) {
                     return ObjectWriterImplOptionalInt.INSTANCE;
                  } else if (objectType == OptionalLong.class) {
                     return ObjectWriterImplOptionalLong.INSTANCE;
                  } else if (objectType == OptionalDouble.class) {
                     return ObjectWriterImplOptionalDouble.INSTANCE;
                  } else if (objectType == Optional.class) {
                     return ObjectWriterImplOptional.INSTANCE;
                  } else if (objectType == Boolean.class) {
                     return ObjectWriterImplBoolean.INSTANCE;
                  } else if (objectType == AtomicBoolean.class) {
                     return ObjectWriterImplAtomicBoolean.INSTANCE;
                  } else if (objectType == AtomicIntegerArray.class) {
                     return ObjectWriterImplAtomicIntegerArray.INSTANCE;
                  } else if (objectType == AtomicLongArray.class) {
                     return ObjectWriterImplAtomicLongArray.INSTANCE;
                  } else if (objectType == Character.class) {
                     return ObjectWriterImplCharacter.INSTANCE;
                  } else {
                     if (objectType instanceof Class) {
                        Class clazz = (Class)objectType;
                        if (TimeUnit.class.isAssignableFrom(clazz)) {
                           return new ObjectWriterImplEnum(null, TimeUnit.class, null, null, 0L);
                        }

                        if (Enum.class.isAssignableFrom(clazz)) {
                           ObjectWriter enumWriter = this.createEnumWriter(clazz);
                           if (enumWriter != null) {
                              return enumWriter;
                           }
                        }

                        if (JSONPath.class.isAssignableFrom(clazz)) {
                           return ObjectWriterImplToString.INSTANCE;
                        }

                        if (clazz == boolean[].class) {
                           return ObjectWriterImplBoolValueArray.INSTANCE;
                        }

                        if (clazz == char[].class) {
                           return ObjectWriterImplCharValueArray.INSTANCE;
                        }

                        if (clazz == StringBuffer.class || clazz == StringBuilder.class) {
                           return ObjectWriterImplToString.INSTANCE;
                        }

                        if (clazz == byte[].class) {
                           return ObjectWriterImplInt8ValueArray.INSTANCE;
                        }

                        if (clazz == short[].class) {
                           return ObjectWriterImplInt16ValueArray.INSTANCE;
                        }

                        if (clazz == int[].class) {
                           return ObjectWriterImplInt32ValueArray.INSTANCE;
                        }

                        if (clazz == long[].class) {
                           return ObjectWriterImplInt64ValueArray.INSTANCE;
                        }

                        if (clazz == float[].class) {
                           return ObjectWriterImplFloatValueArray.INSTANCE;
                        }

                        if (clazz == double[].class) {
                           return ObjectWriterImplDoubleValueArray.INSTANCE;
                        }

                        if (clazz == Byte[].class) {
                           return ObjectWriterImplInt8Array.INSTANCE;
                        }

                        if (clazz == Integer[].class) {
                           return ObjectWriterImplInt32Array.INSTANCE;
                        }

                        if (clazz == Long[].class) {
                           return ObjectWriterImplInt64Array.INSTANCE;
                        }

                        if (String[].class == clazz) {
                           return ObjectWriterImplStringArray.INSTANCE;
                        }

                        if (BigDecimal[].class == clazz) {
                           return ObjectWriterImpDecimalArray.INSTANCE;
                        }

                        if (Object[].class.isAssignableFrom(clazz)) {
                           if (clazz == Object[].class) {
                              return ObjectWriterArray.INSTANCE;
                           }

                           Class componentType = clazz.getComponentType();
                           if (Modifier.isFinal(componentType.getModifiers())) {
                              return new ObjectWriterArrayFinal(componentType, null);
                           }

                           return new ObjectWriterArray(componentType);
                        }

                        if (clazz == UUID.class) {
                           return ObjectWriterImplUUID.INSTANCE;
                        }

                        if (clazz == Locale.class) {
                           return ObjectWriterImplLocale.INSTANCE;
                        }

                        if (clazz == Currency.class) {
                           return ObjectWriterImplCurrency.INSTANCE;
                        }

                        if (TimeZone.class.isAssignableFrom(clazz)) {
                           return ObjectWriterImplTimeZone.INSTANCE;
                        }

                        if (JSONPObject.class.isAssignableFrom(clazz)) {
                           return new ObjectWriterImplJSONP();
                        }

                        if (clazz == URI.class
                           || clazz == URL.class
                           || clazz == File.class
                           || ZoneId.class.isAssignableFrom(clazz)
                           || Charset.class.isAssignableFrom(clazz)) {
                           return ObjectWriterImplToString.INSTANCE;
                        }

                        externalObjectWriter = this.getExternalObjectWriter(clazz.getName(), clazz);
                        if (externalObjectWriter != null) {
                           return externalObjectWriter;
                        }

                        BeanInfo beanInfo = this.provider.createBeanInfo();
                        Class mixIn = this.provider.getMixIn(clazz);
                        if (mixIn != null) {
                           this.annotationProcessor.getBeanInfo(beanInfo, mixIn);
                        }

                        if (Date.class.isAssignableFrom(clazz)) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplDate.INSTANCE;
                           }

                           return new ObjectWriterImplDate(beanInfo.format, beanInfo.locale);
                        }

                        if (Calendar.class.isAssignableFrom(clazz)) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplCalendar.INSTANCE;
                           }

                           return new ObjectWriterImplCalendar(beanInfo.format, beanInfo.locale);
                        }

                        if (ZonedDateTime.class == clazz) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplZonedDateTime.INSTANCE;
                           }

                           return new ObjectWriterImplZonedDateTime(beanInfo.format, beanInfo.locale);
                        }

                        if (OffsetDateTime.class == clazz) {
                           return ObjectWriterImplOffsetDateTime.of(beanInfo.format, beanInfo.locale);
                        }

                        if (LocalDateTime.class == clazz) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplLocalDateTime.INSTANCE;
                           }

                           return new ObjectWriterImplLocalDateTime(beanInfo.format, beanInfo.locale);
                        }

                        if (LocalDate.class == clazz) {
                           return ObjectWriterImplLocalDate.of(beanInfo.format, beanInfo.locale);
                        }

                        if (LocalTime.class == clazz) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplLocalTime.INSTANCE;
                           }

                           return new ObjectWriterImplLocalTime(beanInfo.format, beanInfo.locale);
                        }

                        if (OffsetTime.class == clazz) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplOffsetTime.INSTANCE;
                           }

                           return new ObjectWriterImplOffsetTime(beanInfo.format, beanInfo.locale);
                        }

                        if (Instant.class == clazz) {
                           if (beanInfo.format == null && beanInfo.locale == null) {
                              return ObjectWriterImplInstant.INSTANCE;
                           }

                           return new ObjectWriterImplInstant(beanInfo.format, beanInfo.locale);
                        }

                        if (Duration.class == clazz || Period.class == clazz) {
                           return ObjectWriterImplToString.INSTANCE;
                        }

                        if (StackTraceElement.class == clazz) {
                           if (STACK_TRACE_ELEMENT_WRITER == null) {
                              ObjectWriterCreator creator = this.provider.getCreator();
                              STACK_TRACE_ELEMENT_WRITER = new ObjectWriterAdapter<>(
                                 StackTraceElement.class,
                                 null,
                                 null,
                                 0L,
                                 Arrays.asList(
                                    creator.createFieldWriter(
                                       "fileName",
                                       String.class,
                                       BeanUtils.getDeclaredField(StackTraceElement.class, "fileName"),
                                       BeanUtils.getMethod(StackTraceElement.class, "getFileName"),
                                       StackTraceElement::getFileName
                                    ),
                                    creator.createFieldWriter(
                                       "lineNumber",
                                       BeanUtils.getDeclaredField(StackTraceElement.class, "lineNumber"),
                                       BeanUtils.getMethod(StackTraceElement.class, "getLineNumber"),
                                       StackTraceElement::getLineNumber
                                    ),
                                    creator.createFieldWriter(
                                       "className",
                                       String.class,
                                       BeanUtils.getDeclaredField(StackTraceElement.class, "declaringClass"),
                                       BeanUtils.getMethod(StackTraceElement.class, "getClassName"),
                                       StackTraceElement::getClassName
                                    ),
                                    creator.createFieldWriter(
                                       "methodName",
                                       String.class,
                                       BeanUtils.getDeclaredField(StackTraceElement.class, "methodName"),
                                       BeanUtils.getMethod(StackTraceElement.class, "getMethodName"),
                                       StackTraceElement::getMethodName
                                    )
                                 )
                              );
                           }

                           return STACK_TRACE_ELEMENT_WRITER;
                        }

                        if (Class.class == clazz) {
                           return ObjectWriterImplClass.INSTANCE;
                        }

                        if (Method.class == clazz) {
                           return new ObjectWriterAdapter<>(
                              Method.class,
                              null,
                              null,
                              0L,
                              Arrays.asList(
                                 ObjectWriters.fieldWriter("declaringClass", Class.class, Method::getDeclaringClass),
                                 ObjectWriters.fieldWriter("name", String.class, Method::getName),
                                 ObjectWriters.fieldWriter("parameterTypes", Class[].class, Method::getParameterTypes)
                              )
                           );
                        }

                        if (Field.class == clazz) {
                           return new ObjectWriterAdapter<>(
                              Method.class,
                              null,
                              null,
                              0L,
                              Arrays.asList(
                                 ObjectWriters.fieldWriter("declaringClass", Class.class, Field::getDeclaringClass),
                                 ObjectWriters.fieldWriter("name", String.class, Field::getName)
                              )
                           );
                        }

                        if (ParameterizedType.class.isAssignableFrom(clazz)) {
                           return ObjectWriters.objectWriter(
                              ParameterizedType.class,
                              ObjectWriters.fieldWriter("actualTypeArguments", Type[].class, ParameterizedType::getActualTypeArguments),
                              ObjectWriters.fieldWriter("ownerType", Type.class, ParameterizedType::getOwnerType),
                              ObjectWriters.fieldWriter("rawType", Type.class, ParameterizedType::getRawType)
                           );
                        }
                     }

                     return null;
                  }
            }
         }
      }
   }

   private ObjectWriter createEnumWriter(Class enumClass) {
      if (!enumClass.isEnum()) {
         Class superclass = enumClass.getSuperclass();
         if (superclass.isEnum()) {
            enumClass = superclass;
         }
      }

      Member valueField = BeanUtils.getEnumValueField(enumClass, this.provider);
      if (valueField == null) {
         Class mixInSource = this.provider.mixInCache.get(enumClass);
         Member mixedValueField = BeanUtils.getEnumValueField(mixInSource, this.provider);
         if (mixedValueField instanceof Field) {
            try {
               valueField = enumClass.getField(mixedValueField.getName());
            } catch (NoSuchFieldException var7) {
            }
         } else if (mixedValueField instanceof Method) {
            try {
               valueField = enumClass.getMethod(mixedValueField.getName());
            } catch (NoSuchMethodException var6) {
            }
         }
      }

      BeanInfo beanInfo = this.provider.createBeanInfo();
      Class[] interfaces = enumClass.getInterfaces();

      for (int i = 0; i < interfaces.length; i++) {
         this.annotationProcessor.getBeanInfo(beanInfo, interfaces[i]);
      }

      this.annotationProcessor.getBeanInfo(beanInfo, enumClass);
      if (beanInfo.writeEnumAsJavaBean) {
         return null;
      } else {
         String[] annotationNames = BeanUtils.getEnumAnnotationNames(enumClass);
         return new ObjectWriterImplEnum(null, enumClass, valueField, annotationNames, 0L);
      }
   }

   static class VoidObjectWriter implements ObjectWriter {
      public static final ObjectWriterBaseModule.VoidObjectWriter INSTANCE = new ObjectWriterBaseModule.VoidObjectWriter();

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      }
   }

   public class WriterAnnotationProcessor implements ObjectWriterAnnotationProcessor {
      @Override
      public void getBeanInfo(BeanInfo beanInfo, Class objectClass) {
         if (objectClass != null) {
            Class superclass = objectClass.getSuperclass();
            if (superclass != Object.class && superclass != null && superclass != Enum.class) {
               this.getBeanInfo(beanInfo, superclass);
            }

            Class[] interfaces = objectClass.getInterfaces();

            for (Class item : interfaces) {
               if (item != Serializable.class) {
                  this.getBeanInfo(beanInfo, item);
               }
            }

            if (beanInfo.seeAlso != null && beanInfo.seeAlsoNames != null) {
               for (int i = 0; i < beanInfo.seeAlso.length; i++) {
                  Class seeAlso = beanInfo.seeAlso[i];
                  if (seeAlso == objectClass && i < beanInfo.seeAlsoNames.length) {
                     String seeAlsoName = beanInfo.seeAlsoNames[i];
                     if (seeAlsoName != null && seeAlsoName.length() != 0) {
                        beanInfo.typeName = seeAlsoName;
                        break;
                     }
                  }
               }
            }
         }

         Annotation jsonType1x = null;
         JSONType jsonType = null;
         Annotation[] annotations = BeanUtils.getAnnotations(objectClass);

         for (int ix = 0; ix < annotations.length; ix++) {
            Annotation annotation = annotations[ix];
            Class annotationType = annotation.annotationType();
            if (jsonType == null) {
               jsonType = BeanUtils.findAnnotation(annotation, JSONType.class);
            }

            if (jsonType != annotation) {
               if (annotationType == JSONCompiler.class) {
                  JSONCompiler compiler = (JSONCompiler)annotation;
                  if (compiler.value() == JSONCompiler.CompilerOption.LAMBDA) {
                     beanInfo.writerFeatures |= 18014398509481984L;
                  }
               }

               boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
               String includes = annotationType.getName();
               switch (includes) {
                  case "com.alibaba.fastjson.annotation.JSONType":
                     jsonType1x = annotation;
                     break;
                  case "com.fasterxml.jackson.annotation.JsonIgnoreProperties":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonIgnoreProperties(beanInfo, annotation);
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonPropertyOrder":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonPropertyOrder(beanInfo, annotation);
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
                  case "com.fasterxml.jackson.annotation.JsonTypeInfo":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonTypeInfo(beanInfo, annotation);
                     }
                     break;
                  case "com.fasterxml.jackson.databind.annotation.JsonSerialize":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonSerialize(beanInfo, annotation);
                        if (beanInfo.serializer != null && Enum.class.isAssignableFrom(objectClass)) {
                           beanInfo.writeEnumAsJavaBean = true;
                        }
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonTypeName":
                     if (useJacksonAnnotation) {
                        BeanUtils.processJacksonJsonTypeName(beanInfo, annotation);
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonSubTypes":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonSubTypes(beanInfo, annotation);
                     }
                     break;
                  case "kotlin.Metadata":
                     beanInfo.kotlin = true;
                     KotlinUtils.getConstructor(objectClass, beanInfo);
               }
            }
         }

         if (jsonType == null) {
            Class mixInSource = ObjectWriterBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null) {
               beanInfo.mixIn = true;
               Annotation[] mixInAnnotations = BeanUtils.getAnnotations(mixInSource);

               for (int ix = 0; ix < mixInAnnotations.length; ix++) {
                  Annotation annotationx = mixInAnnotations[ix];
                  Class<? extends Annotation> annotationTypex = annotationx.annotationType();
                  jsonType = BeanUtils.findAnnotation(annotationx, JSONType.class);
                  if (jsonType != annotationx) {
                     String annotationTypeName = annotationTypex.getName();
                     if ("com.alibaba.fastjson.annotation.JSONType".equals(annotationTypeName)) {
                        jsonType1x = annotationx;
                     }
                  }
               }
            }
         }

         if (jsonType != null) {
            Class<?>[] classes = jsonType.seeAlso();
            if (classes.length != 0) {
               beanInfo.seeAlso = classes;
            }

            String typeKey = jsonType.typeKey();
            if (!typeKey.isEmpty()) {
               beanInfo.typeKey = typeKey;
            }

            String typeName = jsonType.typeName();
            if (!typeName.isEmpty()) {
               beanInfo.typeName = typeName;
            }

            for (JSONWriter.Feature feature : jsonType.serializeFeatures()) {
               beanInfo.writerFeatures = beanInfo.writerFeatures | feature.mask;
            }

            beanInfo.namingStrategy = jsonType.naming().name();
            String[] ignores = jsonType.ignores();
            if (ignores.length > 0) {
               beanInfo.ignores = ignores;
            }

            String[] includes = jsonType.includes();
            if (includes.length > 0) {
               beanInfo.includes = includes;
            }

            String[] orders = jsonType.orders();
            if (orders.length > 0) {
               beanInfo.orders = orders;
            }

            Class<?> serializer = jsonType.serializer();
            if (ObjectWriter.class.isAssignableFrom(serializer)) {
               beanInfo.serializer = serializer;
               beanInfo.writeEnumAsJavaBean = true;
            }

            Class<? extends Filter>[] serializeFilters = jsonType.serializeFilters();
            if (serializeFilters.length != 0) {
               beanInfo.serializeFilters = serializeFilters;
            }

            String format = jsonType.format();
            if (!format.isEmpty()) {
               beanInfo.format = format;
            }

            String locale = jsonType.locale();
            if (!locale.isEmpty()) {
               String[] parts = locale.split("_");
               if (parts.length == 2) {
                  beanInfo.locale = new Locale(parts[0], parts[1]);
               }
            }

            if (!jsonType.alphabetic()) {
               beanInfo.alphabetic = false;
            }

            if (jsonType.writeEnumAsJavaBean()) {
               beanInfo.writeEnumAsJavaBean = true;
            }

            String rootName = jsonType.rootName();
            if (!rootName.isEmpty()) {
               beanInfo.rootName = rootName;
            }
         } else if (jsonType1x != null) {
            Annotation annotationx = jsonType1x;
            BeanUtils.annotationMethods(jsonType1x.annotationType(), method -> BeanUtils.processJSONType1x(beanInfo, annotation, method));
         }

         if (beanInfo.seeAlso != null && beanInfo.seeAlso.length != 0 && (beanInfo.typeName == null || beanInfo.typeName.length() == 0)) {
            for (Class seeAlsoClass : beanInfo.seeAlso) {
               if (seeAlsoClass == objectClass) {
                  beanInfo.typeName = objectClass.getSimpleName();
                  break;
               }
            }
         }
      }

      @Override
      public void getFieldInfo(BeanInfo beanInfo, FieldInfo fieldInfo, Class objectClass, Field field) {
         if (objectClass != null) {
            Class mixInSource = ObjectWriterBaseModule.this.provider.mixInCache.get(objectClass);
            if (mixInSource != null && mixInSource != objectClass) {
               Field mixInField = null;

               try {
                  mixInField = mixInSource.getDeclaredField(field.getName());
               } catch (Exception var19) {
               }

               if (mixInField != null) {
                  this.getFieldInfo(beanInfo, fieldInfo, mixInSource, mixInField);
               }
            }
         }

         Class fieldClassMixInSource = ObjectWriterBaseModule.this.provider.mixInCache.get(field.getType());
         if (fieldClassMixInSource != null) {
            fieldInfo.fieldClassMixIn = true;
         }

         int modifiers = field.getModifiers();
         boolean isTransient = Modifier.isTransient(modifiers);
         if (isTransient) {
            fieldInfo.ignore = true;
         }

         JSONField jsonField = null;
         Annotation[] annotations = BeanUtils.getAnnotations(field);

         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (jsonField == null) {
               jsonField = BeanUtils.findAnnotation(annotation, JSONField.class);
               if (jsonField == annotation) {
                  continue;
               }
            }

            String annotationTypeName = annotationType.getName();
            boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
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
               case "com.fasterxml.jackson.annotation.JsonValue":
                  if (useJacksonAnnotation) {
                     fieldInfo.features |= 281474976710656L;
                  }
                  break;
               case "com.fasterxml.jackson.annotation.JsonRawValue":
                  if (useJacksonAnnotation) {
                     fieldInfo.features |= 1125899906842624L;
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
               case "com.fasterxml.jackson.annotation.JsonInclude":
                  if (useJacksonAnnotation) {
                     BeanUtils.processJacksonJsonInclude(beanInfo, annotation);
                  }
                  break;
               case "com.fasterxml.jackson.databind.annotation.JsonSerialize":
                  if (useJacksonAnnotation) {
                     this.processJacksonJsonSerialize(fieldInfo, annotation);
                  }
                  break;
               case "com.google.gson.annotations.SerializedName":
                  if (JSONFactory.isUseGsonAnnotation()) {
                     BeanUtils.processGsonSerializedName(fieldInfo, annotation);
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

         if (jsonField != null) {
            this.loadFieldInfo(fieldInfo, jsonField);
            Class writeUsing = jsonField.writeUsing();
            if (ObjectWriter.class.isAssignableFrom(writeUsing)) {
               fieldInfo.writeUsing = writeUsing;
            }

            Class serializeUsing = jsonField.serializeUsing();
            if (ObjectWriter.class.isAssignableFrom(serializeUsing)) {
               fieldInfo.writeUsing = serializeUsing;
            }

            if (jsonField.jsonDirect()) {
               fieldInfo.features |= 1125899906842624L;
            }

            if ((fieldInfo.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L
               && !String.class.equals(field.getType())
               && fieldInfo.writeUsing == null) {
               fieldInfo.writeUsing = ObjectWriterImplToString.class;
            }
         }
      }

      private void processJacksonJsonSubTypes(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("value".equals(name)) {
                  Annotation[] value = (Annotation[])result;
                  if (value.length != 0) {
                     beanInfo.seeAlso = new Class[value.length];
                     beanInfo.seeAlsoNames = new String[value.length];

                     for (int i = 0; i < value.length; i++) {
                        Annotation item = value[i];
                        BeanUtils.processJacksonJsonSubTypesType(beanInfo, i, item);
                     }
                  }
               }
            } catch (Throwable var8) {
            }
         });
      }

      private void processJacksonJsonSerialize(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               switch (name) {
                  case "using":
                     Class using = this.processUsing((Class)result);
                     if (using != null) {
                        beanInfo.serializer = using;
                     }
                     break;
                  case "keyUsing":
                     Class keyUsing = this.processUsing((Class)result);
                     if (keyUsing != null) {
                        beanInfo.serializer = keyUsing;
                     }
               }
            } catch (Throwable var9) {
            }
         });
      }

      private Class processUsing(Class result) {
         String usingName = result.getName();
         String noneClassName1 = "com.fasterxml.jackson.databind.JsonSerializer$None";
         if (!noneClassName1.equals(usingName) && ObjectWriter.class.isAssignableFrom(result)) {
            return result;
         } else {
            return "com.fasterxml.jackson.databind.ser.std.ToStringSerializer".equals(usingName) ? ObjectWriterImplToString.class : null;
         }
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
                     beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteClassName.mask;
                  }
               }
            } catch (Throwable var6) {
            }
         });
      }

      private void processJacksonJsonPropertyOrder(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         AtomicBoolean alphabetic = new AtomicBoolean(false);
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("value".equals(name)) {
                  String[] value = (String[])result;
                  if (value.length != 0) {
                     beanInfo.orders = value;
                  }
               } else if ("alphabetic".equals(name)) {
                  alphabetic.set((Boolean)result);
               }
            } catch (Throwable var7) {
            }
         });
         if (beanInfo.orders == null || beanInfo.orders.length == 0) {
            beanInfo.alphabetic = alphabetic.get();
         }
      }

      private void processJacksonJsonSerialize(FieldInfo fieldInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               switch (name) {
                  case "using":
                     Class using = this.processUsing((Class)result);
                     if (using != null) {
                        fieldInfo.writeUsing = using;
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
                        fieldInfo.valueUsing = valueUsing;
                     }
               }
            } catch (Throwable var11) {
            }
         });
      }

      private void processJacksonJsonProperty(FieldInfo fieldInfo, Annotation annotation) {
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
                     fieldInfo.ignore = "WRITE_ONLY".equals(access);
                     break;
                  case "index":
                     int index = (Integer)result;
                     if (index != -1) {
                        fieldInfo.ordinal = index;
                     }
               }
            } catch (Throwable var9) {
            }
         });
      }

      private void processJacksonJsonIgnoreProperties(BeanInfo beanInfo, Annotation annotation) {
         Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
         BeanUtils.annotationMethods(annotationClass, m -> {
            String name = m.getName();

            try {
               Object result = m.invoke(annotation);
               if ("value".equals(name)) {
                  String[] value = (String[])result;
                  if (value.length != 0) {
                     beanInfo.ignores = value;
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
                     String valuexx = (String)result;
                     if (!valuexx.isEmpty()) {
                        fieldInfo.fieldName = valuexx;
                     }
                     break;
                  case "format":
                     this.loadJsonFieldFormat(fieldInfo, (String)result);
                     break;
                  case "label":
                     String valuex = (String)result;
                     if (!valuex.isEmpty()) {
                        fieldInfo.label = valuex;
                     }
                     break;
                  case "defaultValue":
                     String value = (String)result;
                     if (!value.isEmpty()) {
                        fieldInfo.defaultValue = value;
                     }
                     break;
                  case "ordinal":
                     int ordinal = (Integer)result;
                     if (ordinal != 0) {
                        fieldInfo.ordinal = ordinal;
                     }
                     break;
                  case "serialize":
                     boolean serialize = (Boolean)result;
                     if (!serialize) {
                        fieldInfo.ignore = true;
                     }
                     break;
                  case "unwrapped":
                     if ((Boolean)result) {
                        fieldInfo.features |= 562949953421312L;
                     }
                     break;
                  case "serialzeFeatures":
                     Enum[] features = (Enum[])result;
                     this.applyFeatures(fieldInfo, features);
                     break;
                  case "serializeUsing":
                     Class writeUsing = (Class)result;
                     if (ObjectWriter.class.isAssignableFrom(writeUsing)) {
                        fieldInfo.writeUsing = writeUsing;
                     }
                     break;
                  case "jsonDirect":
                     Boolean jsonDirect = (Boolean)result;
                     if (jsonDirect) {
                        fieldInfo.features |= 1125899906842624L;
                     }
               }
            } catch (Throwable var9) {
            }
         });
      }

      private void applyFeatures(FieldInfo fieldInfo, Enum[] features) {
         for (Enum feature : features) {
            String var7 = feature.name();
            switch (var7) {
               case "UseISO8601DateFormat":
                  fieldInfo.format = "iso8601";
                  break;
               case "WriteMapNullValue":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNulls.mask;
                  break;
               case "WriteNullListAsEmpty":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNullListAsEmpty.mask;
                  break;
               case "WriteNullStringAsEmpty":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNullStringAsEmpty.mask;
                  break;
               case "WriteNullNumberAsZero":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNullNumberAsZero.mask;
                  break;
               case "WriteNullBooleanAsFalse":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNullBooleanAsFalse.mask;
                  break;
               case "BrowserCompatible":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.BrowserCompatible.mask;
                  break;
               case "WriteClassName":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteClassName.mask;
                  break;
               case "WriteNonStringValueAsString":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNonStringValueAsString.mask;
                  break;
               case "WriteEnumUsingToString":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteEnumUsingToString.mask;
                  break;
               case "NotWriteRootClassName":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.NotWriteRootClassName.mask;
                  break;
               case "IgnoreErrorGetter":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.IgnoreErrorGetter.mask;
                  break;
               case "WriteBigDecimalAsPlain":
                  fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteBigDecimalAsPlain.mask;
            }
         }
      }

      @Override
      public void getFieldInfo(BeanInfo beanInfo, FieldInfo fieldInfo, Class objectClass, Method method) {
         Class mixInSource = ObjectWriterBaseModule.this.provider.mixInCache.get(objectClass);
         String methodName = method.getName();
         if ("getTargetSql".equals(methodName) && objectClass != null && objectClass.getName().startsWith("com.baomidou.mybatisplus.")) {
            fieldInfo.features = fieldInfo.features | JSONWriter.Feature.IgnoreErrorGetter.mask;
         }

         if (mixInSource != null && mixInSource != objectClass) {
            Method mixInMethod = null;

            try {
               mixInMethod = mixInSource.getDeclaredMethod(methodName, method.getParameterTypes());
            } catch (Exception var17) {
            }

            if (mixInMethod != null) {
               this.getFieldInfo(beanInfo, fieldInfo, mixInSource, mixInMethod);
            }
         }

         Class fieldClassMixInSource = ObjectWriterBaseModule.this.provider.mixInCache.get(method.getReturnType());
         if (fieldClassMixInSource != null) {
            fieldInfo.fieldClassMixIn = true;
         }

         if (JDKUtils.CLASS_TRANSIENT != null && method.getAnnotation(JDKUtils.CLASS_TRANSIENT) != null) {
            fieldInfo.ignore = true;
         }

         if (objectClass != null) {
            Class superclass = objectClass.getSuperclass();
            Method supperMethod = BeanUtils.getMethod(superclass, method);
            boolean ignore = fieldInfo.ignore;
            if (supperMethod != null) {
               this.getFieldInfo(beanInfo, fieldInfo, superclass, supperMethod);
               Field field = BeanUtils.getField(objectClass, method);
               int supperMethodModifiers = supperMethod.getModifiers();
               if (null != field && ignore != fieldInfo.ignore && !Modifier.isAbstract(supperMethodModifiers) && !supperMethod.equals(method)) {
                  fieldInfo.ignore = ignore;
               }
            }

            Class[] interfaces = objectClass.getInterfaces();

            for (Class anInterface : interfaces) {
               Method interfaceMethod = BeanUtils.getMethod(anInterface, method);
               if (interfaceMethod != null) {
                  this.getFieldInfo(beanInfo, fieldInfo, superclass, interfaceMethod);
               }
            }
         }

         Annotation[] annotations = BeanUtils.getAnnotations(method);
         this.processAnnotations(fieldInfo, annotations);
         if (!objectClass.getName().startsWith("java.lang") && !BeanUtils.isRecord(objectClass)) {
            Field methodField = BeanUtils.getField(objectClass, method);
            if (methodField != null) {
               fieldInfo.features |= 4503599627370496L;
               this.getFieldInfo(beanInfo, fieldInfo, objectClass, methodField);
            }
         }

         if (beanInfo.kotlin && beanInfo.creatorConstructor != null && beanInfo.createParameterNames != null) {
            String fieldName = BeanUtils.getterName(method, beanInfo.kotlin, null);

            for (int i = 0; i < beanInfo.createParameterNames.length; i++) {
               if (fieldName.equals(beanInfo.createParameterNames[i])) {
                  Annotation[][] creatorConsParamAnnotations = beanInfo.creatorConstructor.getParameterAnnotations();
                  if (i < creatorConsParamAnnotations.length) {
                     Annotation[] parameterAnnotations = creatorConsParamAnnotations[i];
                     this.processAnnotations(fieldInfo, parameterAnnotations);
                     break;
                  }
               }
            }
         }
      }

      private void processAnnotations(FieldInfo fieldInfo, Annotation[] annotations) {
         for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            JSONField jsonField = BeanUtils.findAnnotation(annotation, JSONField.class);
            if (Objects.nonNull(jsonField)) {
               this.loadFieldInfo(fieldInfo, jsonField);
            } else {
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
                  case "java.beans.Transient":
                     fieldInfo.ignore = true;
                     fieldInfo.isTransient = true;
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
                  case "com.fasterxml.jackson.annotation.JsonValue":
                     if (useJacksonAnnotation) {
                        fieldInfo.features |= 281474976710656L;
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonRawValue":
                     if (useJacksonAnnotation) {
                        fieldInfo.features |= 1125899906842624L;
                     }
                     break;
                  case "com.fasterxml.jackson.databind.annotation.JsonSerialize":
                     if (useJacksonAnnotation) {
                        this.processJacksonJsonSerialize(fieldInfo, annotation);
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonInclude":
                     if (useJacksonAnnotation) {
                        BeanUtils.processJacksonJsonInclude(fieldInfo, annotation);
                     }
                     break;
                  case "com.fasterxml.jackson.annotation.JsonUnwrapped":
                     if (useJacksonAnnotation) {
                        BeanUtils.processJacksonJsonUnwrapped(fieldInfo, annotation);
                     }
               }
            }
         }
      }

      private void loadFieldInfo(FieldInfo fieldInfo, JSONField jsonField) {
         String jsonFieldName = jsonField.name();
         if (!jsonFieldName.isEmpty()) {
            fieldInfo.fieldName = jsonFieldName;
         }

         String defaultValue = jsonField.defaultValue();
         if (!defaultValue.isEmpty()) {
            fieldInfo.defaultValue = defaultValue;
         }

         this.loadJsonFieldFormat(fieldInfo, jsonField.format());
         String label = jsonField.label();
         if (!label.isEmpty()) {
            fieldInfo.label = label;
         }

         String locale = jsonField.locale();
         if (!locale.isEmpty()) {
            String[] parts = locale.split("_");
            if (parts.length == 2) {
               fieldInfo.locale = new Locale(parts[0], parts[1]);
            }
         }

         boolean ignore = !jsonField.serialize();
         if (!fieldInfo.ignore) {
            fieldInfo.ignore = ignore;
         }

         if (jsonField.unwrapped()) {
            fieldInfo.features |= 562949953421312L;
         }

         for (JSONWriter.Feature feature : jsonField.serializeFeatures()) {
            fieldInfo.features = fieldInfo.features | feature.mask;
            if (fieldInfo.ignore && !ignore && feature == JSONWriter.Feature.FieldBased) {
               fieldInfo.ignore = false;
            }
         }

         int ordinal = jsonField.ordinal();
         if (ordinal != 0) {
            fieldInfo.ordinal = ordinal;
         }

         if (jsonField.value()) {
            fieldInfo.features |= 281474976710656L;
         }

         if (jsonField.jsonDirect()) {
            fieldInfo.features |= 1125899906842624L;
         }

         Class serializeUsing = jsonField.serializeUsing();
         if (ObjectWriter.class.isAssignableFrom(serializeUsing)) {
            fieldInfo.writeUsing = serializeUsing;
         }
      }

      private void loadJsonFieldFormat(FieldInfo fieldInfo, String jsonFieldFormat) {
         if (!jsonFieldFormat.isEmpty()) {
            jsonFieldFormat = jsonFieldFormat.trim();
            if (jsonFieldFormat.indexOf(84) != -1 && !jsonFieldFormat.contains("'T'")) {
               jsonFieldFormat = jsonFieldFormat.replaceAll("T", "'T'");
            }

            if (!jsonFieldFormat.isEmpty()) {
               fieldInfo.format = jsonFieldFormat;
            }
         }
      }
   }
}
