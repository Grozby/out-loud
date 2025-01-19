package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.function.ObjBoolConsumer;
import com.alibaba.fastjson2.function.ObjByteConsumer;
import com.alibaba.fastjson2.function.ObjCharConsumer;
import com.alibaba.fastjson2.function.ObjFloatConsumer;
import com.alibaba.fastjson2.function.ObjShortConsumer;
import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.modules.ObjectReaderAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ObjectReaderCreator {
   public static final boolean JIT = !JDKUtils.ANDROID && !JDKUtils.GRAAL;
   public static final ObjectReaderCreator INSTANCE = new ObjectReaderCreator();
   protected final AtomicInteger jitErrorCount = new AtomicInteger();
   protected volatile Throwable jitErrorLast;
   protected static final Map<Class, ObjectReaderCreator.LambdaSetterInfo> methodTypeMapping = new HashMap<>();

   public <T> ObjectReader<T> createObjectReaderNoneDefaultConstructor(Constructor constructor, String... paramNames) {
      Function<Map<Long, Object>, T> function = this.createFunction(constructor, paramNames);
      Class declaringClass = constructor.getDeclaringClass();
      FieldReader[] fieldReaders = this.createFieldReaders(
         JSONFactory.getDefaultObjectReaderProvider(), declaringClass, declaringClass, constructor, constructor.getParameters(), paramNames
      );
      return this.createObjectReaderNoneDefaultConstructor(declaringClass, function, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReaderNoneDefaultConstructor(Class objectClass, Function<Map<Long, Object>, T> creator, FieldReader... fieldReaders) {
      return new ObjectReaderNoneDefaultConstructor<>(objectClass, null, null, 0L, creator, null, null, fieldReaders, null, null, null);
   }

   public <T> ObjectReader<T> createObjectReaderFactoryMethod(Method factoryMethod, String... paramNames) {
      Function<Map<Long, Object>, Object> factoryFunction = this.createFactoryFunction(factoryMethod, paramNames);
      FieldReader[] fieldReaders = this.createFieldReaders(
         JSONFactory.getDefaultObjectReaderProvider(), null, null, factoryMethod, factoryMethod.getParameters(), paramNames
      );
      return new ObjectReaderNoneDefaultConstructor<>(
         null, null, null, 0L, (Function<Map<Long, Object>, T>)factoryFunction, null, paramNames, fieldReaders, null, null, null
      );
   }

   public FieldReader[] createFieldReaders(
      ObjectReaderProvider provider, Class objectClass, Type objectType, Executable owner, Parameter[] parameters, String... paramNames
   ) {
      Class<?> declaringClass = null;
      if (owner != null) {
         declaringClass = owner.getDeclaringClass();
      }

      FieldReader[] fieldReaders = new FieldReader[parameters.length];

      for (int i = 0; i < parameters.length; i++) {
         FieldInfo fieldInfo = new FieldInfo();
         Parameter parameter = parameters[i];
         String paramName;
         if (i < paramNames.length) {
            paramName = paramNames[i];
         } else {
            paramName = parameter.getName();
         }

         if (owner instanceof Constructor) {
            provider.getFieldInfo(fieldInfo, declaringClass, (Constructor)owner, i, parameter);
         }

         if (owner instanceof Constructor) {
            Field field = BeanUtils.getDeclaredField(declaringClass, paramName);
            if (field != null) {
               provider.getFieldInfo(fieldInfo, declaringClass, field);
            }
         }

         String fieldName;
         if (fieldInfo.fieldName != null && !fieldInfo.fieldName.isEmpty()) {
            fieldName = fieldInfo.fieldName;
         } else {
            fieldName = paramName;
         }

         if (fieldName == null) {
            fieldName = "arg" + i;
         }

         if (paramName == null) {
            paramName = "arg" + i;
         }

         ObjectReader initReader = getInitReader(provider, parameter.getParameterizedType(), parameter.getType(), fieldInfo);
         Type paramType = parameter.getParameterizedType();
         fieldReaders[i] = this.createFieldReaderParam(
            null,
            null,
            fieldName,
            i,
            fieldInfo.features,
            fieldInfo.format,
            fieldInfo.locale,
            fieldInfo.defaultValue,
            paramType,
            parameter.getType(),
            paramName,
            declaringClass,
            parameter,
            null,
            initReader
         );
      }

      return fieldReaders;
   }

   public <T> Function<Map<Long, Object>, T> createFactoryFunction(Method factoryMethod, String... paramNames) {
      factoryMethod.setAccessible(true);
      return new FactoryFunction<>(factoryMethod, paramNames);
   }

   public <T> Function<Map<Long, Object>, T> createFunction(Constructor constructor, String... paramNames) {
      constructor.setAccessible(true);
      return new ConstructorFunction<>(null, constructor, null, null, null, paramNames);
   }

   public <T> Function<Map<Long, Object>, T> createFunction(Constructor constructor, Constructor markerConstructor, String... paramNames) {
      if (markerConstructor == null) {
         constructor.setAccessible(true);
      } else {
         markerConstructor.setAccessible(true);
      }

      return new ConstructorFunction<>(null, constructor, null, null, markerConstructor, paramNames);
   }

   public <T> ObjectReader<T> createObjectReader(Class<T> objectClass, FieldReader... fieldReaders) {
      return this.createObjectReader(objectClass, null, 0L, null, this.createSupplier(objectClass), null, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReader(Class<T> objectClass, Supplier<T> defaultCreator, FieldReader... fieldReaders) {
      return this.createObjectReader(objectClass, null, 0L, null, defaultCreator, null, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReaderSeeAlso(Class<T> objectType, Class[] seeAlso, FieldReader... fieldReaders) {
      Supplier<T> instanceSupplier = this.createSupplier(objectType);
      return new ObjectReaderSeeAlso<>(objectType, instanceSupplier, "@type", seeAlso, null, null, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReaderSeeAlso(
      Class<T> objectClass, String typeKey, Class[] seeAlso, String[] seeAlsoNames, FieldReader... fieldReaders
   ) {
      Supplier<T> creator = this.createSupplier(objectClass);
      return new ObjectReaderSeeAlso<>(objectClass, creator, typeKey, seeAlso, seeAlsoNames, null, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReaderSeeAlso(
      Class<T> objectClass, String typeKey, Class[] seeAlso, String[] seeAlsoNames, Class seeAlsoDefault, FieldReader... fieldReaders
   ) {
      Supplier<T> creator = this.createSupplier(objectClass);
      return new ObjectReaderSeeAlso<>(objectClass, creator, typeKey, seeAlso, seeAlsoNames, seeAlsoDefault, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReaderSeeAlso(
      Class<T> objectType, Supplier<T> defaultCreator, String typeKey, Class[] seeAlso, String[] seeAlsoNames, FieldReader... fieldReaders
   ) {
      return new ObjectReaderSeeAlso<>(objectType, defaultCreator, typeKey, seeAlso, seeAlsoNames, null, fieldReaders);
   }

   protected <T> ObjectReader<T> createObjectReaderWithBuilder(Class<T> objectClass, Type objectType, ObjectReaderProvider provider, BeanInfo beanInfo) {
      Function<Object, Object> builderFunction = null;
      if (beanInfo.buildMethod != null) {
         builderFunction = this.createBuildFunction(beanInfo.buildMethod);
      }

      Class builderClass = beanInfo.builder;
      String builderWithPrefix = beanInfo.builderWithPrefix;
      if (builderWithPrefix == null || builderWithPrefix.isEmpty()) {
         builderWithPrefix = "with";
      }

      int builderWithPrefixLenth = builderWithPrefix.length();
      Map<String, List<FieldReader>> fieldReaders = new LinkedHashMap<>();
      String prefix = builderWithPrefix;
      FieldInfo fieldInfo = new FieldInfo();
      BeanUtils.setters(
         builderClass,
         false,
         method -> {
            fieldInfo.init();
            provider.getFieldInfo(fieldInfo, objectClass, method);
            if (!fieldInfo.ignore) {
               String methodName = method.getName();
               String fieldName;
               if (fieldInfo.fieldName != null && !fieldInfo.fieldName.isEmpty()) {
                  fieldName = fieldInfo.fieldName;
               } else {
                  int methodNameLength = methodName.length();
                  boolean prefixNotMach = methodNameLength <= prefix.length() || !methodName.startsWith(prefix);
                  if (prefixNotMach) {
                     if (method.getDeclaringClass() == Object.class
                        || method.getReturnType() != builderClass
                        || method.getAnnotation(JSONField.class) == null && (beanInfo.readerFeatures & JSONReader.Feature.SupportSmartMatch.mask) == 0L) {
                        return;
                     }

                     fieldName = methodName;
                  } else {
                     fieldName = BeanUtils.setterName(methodName, builderWithPrefixLenth);
                  }
               }

               if (method.getParameterCount() == 0) {
                  FieldReader fieldReader = this.createFieldReaderMethod(
                     builderClass,
                     builderClass,
                     fieldName,
                     fieldInfo.ordinal,
                     fieldInfo.features,
                     fieldInfo.format,
                     fieldInfo.locale,
                     fieldInfo.defaultValue,
                     fieldInfo.schema,
                     method.getGenericReturnType(),
                     method.getReturnType(),
                     method,
                     null
                  );
                  this.putIfAbsent(fieldReaders, fieldName, fieldReader, objectClass);
               } else {
                  Type fieldType = method.getGenericParameterTypes()[0];
                  Class fieldClass = method.getParameterTypes()[0];
                  method.setAccessible(true);
                  FieldReader fieldReader = this.createFieldReaderMethod(
                     builderClass,
                     objectType,
                     fieldName,
                     fieldInfo.ordinal,
                     fieldInfo.features,
                     fieldInfo.format,
                     fieldInfo.locale,
                     fieldInfo.defaultValue,
                     fieldInfo.schema,
                     fieldType,
                     fieldClass,
                     method,
                     null
                  );
                  this.putIfAbsent(fieldReaders, fieldName, fieldReader, objectClass);
                  if (fieldInfo.alternateNames != null) {
                     for (String alternateName : fieldInfo.alternateNames) {
                        if (!fieldName.equals(alternateName)) {
                           this.putIfAbsent(
                              fieldReaders,
                              alternateName,
                              this.createFieldReaderMethod(
                                 builderClass,
                                 objectType,
                                 alternateName,
                                 fieldInfo.ordinal,
                                 fieldInfo.features,
                                 fieldInfo.format,
                                 fieldInfo.locale,
                                 fieldInfo.defaultValue,
                                 fieldInfo.schema,
                                 fieldType,
                                 fieldClass,
                                 method,
                                 null
                              ),
                              objectClass
                           );
                        }
                     }
                  }
               }
            }
         }
      );
      Supplier instanceSupplier = this.createSupplier(builderClass);
      return this.createObjectReader(builderClass, 0L, instanceSupplier, builderFunction, this.toFieldReaderArray(fieldReaders));
   }

   protected <T> ObjectReader<T> createObjectReaderWithCreator(Class<T> objectClass, Type objectType, ObjectReaderProvider provider, BeanInfo beanInfo) {
      FieldInfo fieldInfo = new FieldInfo();
      Map<String, List<FieldReader>> fieldReaders = new LinkedHashMap<>();
      Class declaringClass;
      Parameter[] parameters;
      String[] paramNames;
      if (beanInfo.creatorConstructor != null) {
         parameters = beanInfo.creatorConstructor.getParameters();
         declaringClass = beanInfo.creatorConstructor.getDeclaringClass();
         paramNames = ASMUtils.lookupParameterNames(beanInfo.creatorConstructor);
      } else {
         parameters = beanInfo.createMethod.getParameters();
         declaringClass = beanInfo.createMethod.getDeclaringClass();
         paramNames = ASMUtils.lookupParameterNames(beanInfo.createMethod);
      }

      for (int i = 0; i < parameters.length; i++) {
         fieldInfo.init();
         Parameter parameter = parameters[i];
         if (beanInfo.creatorConstructor != null) {
            provider.getFieldInfo(fieldInfo, objectClass, beanInfo.creatorConstructor, i, parameter);
         } else {
            provider.getFieldInfo(fieldInfo, objectClass, beanInfo.createMethod, i, parameter);
         }

         if (parameters.length == 1 && (fieldInfo.features & 281474976710656L) != 0L) {
            break;
         }

         String fieldName = fieldInfo.fieldName;
         if (fieldName == null || fieldName.isEmpty()) {
            if (beanInfo.createParameterNames != null && i < beanInfo.createParameterNames.length) {
               fieldName = beanInfo.createParameterNames[i];
            }

            if (fieldName == null || fieldName.isEmpty()) {
               fieldName = parameter.getName();
            }
         }

         if (fieldName == null || fieldName.isEmpty()) {
            fieldName = paramNames[i];
         } else if (fieldName.startsWith("arg")) {
            if (paramNames != null && paramNames.length > i) {
               fieldName = paramNames[i];
            }
         } else {
            paramNames[i] = fieldName;
         }

         Class<?> paramClass = parameter.getType();
         BeanUtils.getters(objectClass, method -> {
            if (method.getReturnType() == paramClass) {
               FieldInfo methodFieldInfo = new FieldInfo();
               provider.getFieldInfo(methodFieldInfo, objectClass, method);
               String methodFieldName = methodFieldInfo.fieldName;
               if (methodFieldName == null) {
                  methodFieldName = BeanUtils.getterName(method, beanInfo.kotlin, PropertyNamingStrategy.CamelCase.name());
               }

               if (methodFieldInfo.readUsing != null && fieldName.equals(methodFieldName)) {
                  fieldInfo.readUsing = methodFieldInfo.readUsing;
               }
            }
         });
         if (fieldName == null || fieldName.isEmpty()) {
            fieldName = "arg" + i;
         }

         Type paramType = parameter.getParameterizedType();
         ObjectReader initReader = getInitReader(provider, paramType, paramClass, fieldInfo);
         FieldReader fieldReaderParam = this.createFieldReaderParam(
            objectClass,
            objectType,
            fieldName,
            i,
            fieldInfo.features,
            fieldInfo.format,
            paramType,
            paramClass,
            fieldName,
            declaringClass,
            parameter,
            null,
            initReader
         );
         fieldReaders.put(fieldName, this.listOf(fieldReaderParam));
         if (fieldInfo.alternateNames != null) {
            for (String alternateName : fieldInfo.alternateNames) {
               if (!fieldName.equals(alternateName)) {
                  this.putIfAbsent(
                     fieldReaders,
                     alternateName,
                     this.createFieldReaderParam(
                        objectClass,
                        objectType,
                        alternateName,
                        i,
                        fieldInfo.features,
                        fieldInfo.format,
                        paramType,
                        paramClass,
                        fieldName,
                        declaringClass,
                        parameter,
                        null
                     ),
                     objectClass
                  );
               }
            }
         }
      }

      if (parameters.length == 1 && (fieldInfo.features & 281474976710656L) != 0L) {
         Type valueType = beanInfo.creatorConstructor == null
            ? beanInfo.createMethod.getGenericParameterTypes()[0]
            : beanInfo.creatorConstructor.getGenericParameterTypes()[0];
         Class valueClass = beanInfo.creatorConstructor == null
            ? beanInfo.createMethod.getParameterTypes()[0]
            : beanInfo.creatorConstructor.getParameterTypes()[0];
         JSONSchema jsonSchema = null;
         if (fieldInfo.schema != null && !fieldInfo.schema.isEmpty()) {
            JSONObject object = JSON.parseObject(fieldInfo.schema);
            if (!object.isEmpty()) {
               jsonSchema = JSONSchema.of(object, valueClass);
            }
         }

         Object defaultValue = fieldInfo.defaultValue;
         if (defaultValue != null && defaultValue.getClass() != valueClass) {
            Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(defaultValue.getClass(), valueType);
            if (typeConvert == null) {
               throw new JSONException("illegal defaultValue : " + defaultValue + ", class " + valueClass.getName());
            }

            defaultValue = typeConvert.apply(defaultValue);
         }

         boolean jit = JIT || (fieldInfo.features & 18014398509481984L) != 0L || (beanInfo.readerFeatures & 18014398509481984L) != 0L;
         Function function = null;
         if (defaultValue == null && jit) {
            if (valueClass == int.class) {
               IntFunction intFunction = null;
               if (beanInfo.creatorConstructor != null) {
                  intFunction = this.createIntFunction(beanInfo.creatorConstructor);
               } else if (beanInfo.createMethod != null) {
                  intFunction = this.createIntFunction(beanInfo.createMethod);
               }

               if (intFunction != null) {
                  return ObjectReaderImplValueInt.of(objectClass, fieldInfo.features, jsonSchema, intFunction);
               }
            } else if (valueClass == String.class) {
               if (beanInfo.creatorConstructor != null) {
                  function = this.createStringFunction(beanInfo.creatorConstructor);
               } else if (beanInfo.createMethod != null) {
                  function = this.createStringFunction(beanInfo.createMethod);
               }

               if (function != null) {
                  return ObjectReaderImplValueString.of(objectClass, fieldInfo.features, jsonSchema, function);
               }
            }
         }

         if (jit && !valueClass.isPrimitive()) {
            if (beanInfo.creatorConstructor != null) {
               function = this.createValueFunction(beanInfo.creatorConstructor, valueClass);
            } else if (beanInfo.createMethod != null) {
               function = this.createValueFunction(beanInfo.createMethod, valueClass);
            }
         }

         return new ObjectReaderImplValue<>(
            objectClass,
            valueType,
            valueClass,
            fieldInfo.features,
            fieldInfo.format,
            defaultValue,
            jsonSchema,
            beanInfo.creatorConstructor,
            beanInfo.createMethod,
            function
         );
      } else {
         Function<Map<Long, Object>, Object> functionx;
         if (beanInfo.creatorConstructor != null) {
            functionx = this.createFunction(beanInfo.creatorConstructor, beanInfo.markerConstructor, paramNames);
         } else {
            functionx = this.createFactoryFunction(beanInfo.createMethod, paramNames);
         }

         FieldReader[] setterFieldReaders = this.createFieldReaders(objectClass, objectType);
         Arrays.sort((Object[])setterFieldReaders);
         boolean[] flags = null;
         int maskCount = 0;
         if (setterFieldReaders != null) {
            for (int i = 0; i < setterFieldReaders.length; i++) {
               FieldReader setterFieldReader = setterFieldReaders[i];
               if (fieldReaders.containsKey(setterFieldReader.fieldName)) {
                  if (flags == null) {
                     flags = new boolean[setterFieldReaders.length];
                  }

                  flags[i] = true;
                  maskCount++;
               }
            }

            if (maskCount > 0) {
               FieldReader[] array = new FieldReader[setterFieldReaders.length - maskCount];
               int index = 0;

               for (int ix = 0; ix < setterFieldReaders.length; ix++) {
                  if (!flags[ix]) {
                     array[index++] = setterFieldReaders[ix];
                  }
               }

               setterFieldReaders = array;
            }
         }

         return new ObjectReaderNoneDefaultConstructor<>(
            objectClass,
            beanInfo.typeKey,
            beanInfo.typeName,
            beanInfo.readerFeatures,
            (Function<Map<Long, Object>, T>)functionx,
            null,
            paramNames,
            this.toFieldReaderArray(fieldReaders),
            setterFieldReaders,
            beanInfo.seeAlso,
            beanInfo.seeAlsoNames
         );
      }
   }

   public <T> ObjectReader<T> createObjectReader(
      Class<T> objectClass, long features, Supplier<T> defaultCreator, Function buildFunction, FieldReader... fieldReaders
   ) {
      return this.createObjectReader(objectClass, null, features, null, defaultCreator, buildFunction, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReader(
      Class<T> objectClass, String typeKey, long features, JSONSchema schema, Supplier<T> defaultCreator, Function buildFunction, FieldReader... fieldReaders
   ) {
      return this.createObjectReader(objectClass, typeKey, null, features, schema, defaultCreator, buildFunction, fieldReaders);
   }

   public <T> ObjectReader<T> createObjectReader(
      Class<T> objectClass,
      String typeKey,
      String rootName,
      long features,
      JSONSchema schema,
      Supplier<T> defaultCreator,
      Function buildFunction,
      FieldReader... fieldReaders
   ) {
      if (objectClass != null) {
         int modifiers = objectClass.getModifiers();
         if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return new ObjectReaderAdapter<>(objectClass, typeKey, null, features, schema, defaultCreator, buildFunction, fieldReaders);
         }
      }

      if (rootName != null) {
         return new ObjectReaderRootName<>(
            objectClass, typeKey, null, rootName, features, schema, defaultCreator, buildFunction, null, null, null, fieldReaders
         );
      } else {
         switch (fieldReaders.length) {
            case 1:
               return new ObjectReader1<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders[0]);
            case 2:
               return new ObjectReader2<>(objectClass, features, schema, defaultCreator, buildFunction, fieldReaders[0], fieldReaders[1]);
            case 3:
               return new ObjectReader3<>(objectClass, defaultCreator, features, schema, buildFunction, fieldReaders[0], fieldReaders[1], fieldReaders[2]);
            case 4:
               return new ObjectReader4<>(
                  objectClass, features, schema, defaultCreator, buildFunction, fieldReaders[0], fieldReaders[1], fieldReaders[2], fieldReaders[3]
               );
            case 5:
               return new ObjectReader5<>(
                  objectClass,
                  defaultCreator,
                  features,
                  schema,
                  buildFunction,
                  fieldReaders[0],
                  fieldReaders[1],
                  fieldReaders[2],
                  fieldReaders[3],
                  fieldReaders[4]
               );
            case 6:
               return new ObjectReader6<>(
                  objectClass,
                  defaultCreator,
                  features,
                  schema,
                  buildFunction,
                  fieldReaders[0],
                  fieldReaders[1],
                  fieldReaders[2],
                  fieldReaders[3],
                  fieldReaders[4],
                  fieldReaders[5]
               );
            case 7:
               return new ObjectReader7<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            case 8:
               return new ObjectReader8<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            case 9:
               return new ObjectReader9<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            case 10:
               return new ObjectReader10<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            case 11:
               return new ObjectReader11<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            case 12:
               return new ObjectReader12<>(objectClass, null, null, features, schema, defaultCreator, buildFunction, fieldReaders);
            default:
               return new ObjectReaderAdapter<>(objectClass, typeKey, null, features, schema, defaultCreator, buildFunction, fieldReaders);
         }
      }
   }

   public <T> ObjectReader<T> createObjectReader(Type objectType) {
      if (objectType instanceof Class) {
         return this.createObjectReader((Class<T>)objectType);
      } else {
         Class<T> objectClass = (Class<T>)TypeUtils.getMapping(objectType);
         FieldReader[] fieldReaderArray = this.createFieldReaders(objectClass, objectType);
         return this.createObjectReader(objectClass, this.createSupplier(objectClass), fieldReaderArray);
      }
   }

   public <T> ObjectReader<T> createObjectReader(Class<T> objectType) {
      return this.createObjectReader(objectType, objectType, false, JSONFactory.getDefaultObjectReaderProvider());
   }

   public <T> ObjectReader<T> createObjectReader(Class<T> objectType, boolean fieldBased) {
      return this.createObjectReader(objectType, objectType, fieldBased, JSONFactory.getDefaultObjectReaderProvider());
   }

   public <T> ObjectReader<T> createObjectReader(Class<T> objectClass, Type objectType, boolean fieldBased, ObjectReaderProvider provider) {
      BeanInfo beanInfo = new BeanInfo(provider);
      if (fieldBased) {
         beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.FieldBased.mask;
      }

      for (ObjectReaderModule module : provider.modules) {
         ObjectReaderAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getBeanInfo(beanInfo, objectClass);
         }
      }

      if (beanInfo.deserializer != null && ObjectReader.class.isAssignableFrom(beanInfo.deserializer)) {
         try {
            Constructor constructor = beanInfo.deserializer.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ObjectReader<T>)constructor.newInstance();
         } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException var22) {
            throw new JSONException("create deserializer error", var22);
         }
      } else {
         if (fieldBased) {
            beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.FieldBased.mask;
         }

         if (!Enum.class.isAssignableFrom(objectClass) || beanInfo.createMethod != null && beanInfo.createMethod.getParameterCount() != 1) {
            if (Throwable.class.isAssignableFrom(objectClass)) {
               fieldBased = false;
               beanInfo.readerFeatures = beanInfo.readerFeatures | JSONReader.Feature.IgnoreSetNullValue.mask;
            }

            if (objectClass == Class.class) {
               return ObjectReaderImplClass.INSTANCE;
            } else {
               if (fieldBased && objectClass.isInterface()) {
                  fieldBased = false;
               }

               FieldReader[] fieldReaderArray = this.createFieldReaders(objectClass, objectType, beanInfo, fieldBased, provider);
               boolean allReadOnlyOrZero = true;

               for (int i = 0; i < fieldReaderArray.length; i++) {
                  FieldReader fieldReader = fieldReaderArray[i];
                  if (!fieldReader.isReadOnly()) {
                     allReadOnlyOrZero = false;
                     break;
                  }
               }

               if (beanInfo.creatorConstructor != null || beanInfo.createMethod != null) {
                  return this.createObjectReaderWithCreator(objectClass, objectType, provider, beanInfo);
               } else if (beanInfo.builder != null) {
                  return this.createObjectReaderWithBuilder(objectClass, objectType, provider, beanInfo);
               } else {
                  Constructor creatorConstructor = null;
                  List<Constructor> alternateConstructors = new ArrayList<>();
                  BeanUtils.constructor(objectClass, alternateConstructors::add);
                  if (Throwable.class.isAssignableFrom(objectClass)) {
                     return new ObjectReaderException<>(objectClass, alternateConstructors, fieldReaderArray);
                  } else {
                     Constructor defaultConstructor = null;
                     Class<?> declaringClass = objectClass.getDeclaringClass();
                     int index = -1;

                     for (int ix = 0; ix < alternateConstructors.size(); ix++) {
                        Constructor constructor = alternateConstructors.get(ix);
                        if (constructor.getParameterCount() == 0) {
                           defaultConstructor = constructor;
                        }

                        if (declaringClass != null && constructor.getParameterCount() == 1 && declaringClass.equals(constructor.getParameterTypes()[0])) {
                           creatorConstructor = constructor;
                           index = ix;
                           break;
                        }

                        if (creatorConstructor == null) {
                           creatorConstructor = constructor;
                           index = ix;
                        } else if (constructor.getParameterCount() == 0) {
                           creatorConstructor = constructor;
                           index = ix;
                        } else if (creatorConstructor.getParameterCount() < constructor.getParameterCount()) {
                           creatorConstructor = constructor;
                           index = ix;
                        }
                     }

                     if (index != -1) {
                        alternateConstructors.remove(index);
                     }

                     if (creatorConstructor != null && creatorConstructor.getParameterCount() != 0 && beanInfo.seeAlso == null) {
                        boolean record = BeanUtils.isRecord(objectClass);
                        creatorConstructor.setAccessible(true);
                        String[] parameterNames = beanInfo.createParameterNames;
                        if (record && parameterNames == null) {
                           parameterNames = BeanUtils.getRecordFieldNames(objectClass);
                        }

                        if (parameterNames == null || parameterNames.length == 0) {
                           parameterNames = ASMUtils.lookupParameterNames(creatorConstructor);
                           Parameter[] parameters = creatorConstructor.getParameters();
                           FieldInfo fieldInfo = new FieldInfo();

                           for (int ix = 0; ix < parameters.length && ix < parameterNames.length; ix++) {
                              fieldInfo.init();
                              Parameter parameter = parameters[ix];
                              provider.getFieldInfo(fieldInfo, objectClass, creatorConstructor, ix, parameter);
                              if (fieldInfo.fieldName != null) {
                                 parameterNames[ix] = fieldInfo.fieldName;
                              }
                           }
                        }

                        int matchCount = 0;
                        if (defaultConstructor != null) {
                           for (int ixx = 0; ixx < parameterNames.length; ixx++) {
                              String parameterName = parameterNames[ixx];
                              if (parameterName != null) {
                                 for (int j = 0; j < fieldReaderArray.length; j++) {
                                    FieldReader fieldReader = fieldReaderArray[j];
                                    if (fieldReader != null && parameterName.equals(fieldReader.fieldName)) {
                                       matchCount++;
                                       break;
                                    }
                                 }
                              }
                           }
                        }

                        if (!fieldBased && !Throwable.class.isAssignableFrom(objectClass) && defaultConstructor == null && matchCount != parameterNames.length) {
                           if (creatorConstructor.getParameterCount() == 1) {
                              FieldInfo fieldInfo = new FieldInfo();
                              provider.getFieldInfo(fieldInfo, objectClass, creatorConstructor, 0, creatorConstructor.getParameters()[0]);
                              if ((fieldInfo.features & 281474976710656L) != 0L) {
                                 Type valueType = creatorConstructor.getGenericParameterTypes()[0];
                                 Class valueClass = creatorConstructor.getParameterTypes()[0];
                                 JSONSchema jsonSchema = null;
                                 if (fieldInfo.schema != null && !fieldInfo.schema.isEmpty()) {
                                    JSONObject object = JSON.parseObject(fieldInfo.schema);
                                    if (!object.isEmpty()) {
                                       jsonSchema = JSONSchema.of(object, valueClass);
                                    }
                                 }

                                 Object defaultValue = fieldInfo.defaultValue;
                                 if (defaultValue != null && defaultValue.getClass() != valueClass) {
                                    Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(defaultValue.getClass(), valueType);
                                    if (typeConvert == null) {
                                       throw new JSONException("illegal defaultValue : " + defaultValue + ", class " + valueClass.getName());
                                    }

                                    defaultValue = typeConvert.apply(defaultValue);
                                 }

                                 return new ObjectReaderImplValue<>(
                                    objectClass,
                                    valueType,
                                    valueClass,
                                    fieldInfo.features,
                                    fieldInfo.format,
                                    defaultValue,
                                    jsonSchema,
                                    creatorConstructor,
                                    null,
                                    null
                                 );
                              }
                           }

                           if (allReadOnlyOrZero && fieldReaderArray.length != 0 && alternateConstructors.isEmpty()) {
                              for (int ixxx = 0; ixxx < parameterNames.length; ixxx++) {
                                 String paramName = parameterNames[ixxx];

                                 for (FieldReader fieldReader : fieldReaderArray) {
                                    if (fieldReader.field != null && fieldReader.field.getName().equals(paramName) && !fieldReader.fieldName.equals(paramName)) {
                                       parameterNames[ixxx] = fieldReader.fieldName;
                                       break;
                                    }
                                 }
                              }
                           }

                           Function function = null;
                           BiFunction biFunction = null;
                           if (JIT) {
                              if (creatorConstructor.getParameterCount() == 1) {
                                 function = LambdaMiscCodec.createFunction(creatorConstructor);
                              } else if (creatorConstructor.getParameterCount() == 2) {
                                 biFunction = LambdaMiscCodec.createBiFunction(creatorConstructor);
                              }
                           }

                           Function<Map<Long, Object>, T> constructorFunction = new ConstructorFunction<>(
                              alternateConstructors, creatorConstructor, function, biFunction, null, parameterNames
                           );
                           FieldReader[] paramFieldReaders = this.createFieldReaders(
                              provider, objectClass, objectType, creatorConstructor, creatorConstructor.getParameters(), parameterNames
                           );
                           return new ObjectReaderNoneDefaultConstructor<>(
                              objectClass,
                              beanInfo.typeKey,
                              beanInfo.typeName,
                              beanInfo.readerFeatures,
                              constructorFunction,
                              alternateConstructors,
                              parameterNames,
                              paramFieldReaders,
                              fieldReaderArray,
                              null,
                              null
                           );
                        }
                     }

                     if (beanInfo.seeAlso != null && beanInfo.seeAlso.length != 0) {
                        return this.createObjectReaderSeeAlso(
                           objectClass, beanInfo.typeKey, beanInfo.seeAlso, beanInfo.seeAlsoNames, beanInfo.seeAlsoDefault, fieldReaderArray
                        );
                     } else if (objectClass.isInterface()) {
                        return new ObjectReaderInterface<>(objectClass, null, null, 0L, null, null, fieldReaderArray);
                     } else {
                        Supplier<T> creator = this.createSupplier(objectClass);
                        JSONSchema jsonSchemax = JSONSchema.of(JSON.parseObject(beanInfo.schema), objectClass);
                        ObjectReader<T> objectReader = this.createObjectReader(
                           objectClass, beanInfo.typeKey, beanInfo.rootName, beanInfo.readerFeatures, jsonSchemax, creator, null, fieldReaderArray
                        );
                        if (objectReader instanceof ObjectReaderBean) {
                           JSONReader.AutoTypeBeforeHandler beforeHandler = null;
                           if (beanInfo.autoTypeBeforeHandler != null) {
                              try {
                                 Constructor constructorx = beanInfo.autoTypeBeforeHandler.getDeclaredConstructor();
                                 constructorx.setAccessible(true);
                                 beforeHandler = (JSONReader.AutoTypeBeforeHandler)constructorx.newInstance();
                              } catch (Exception var23) {
                              }
                           }

                           if (beforeHandler != null) {
                              ((ObjectReaderBean)objectReader).setAutoTypeBeforeHandler(beforeHandler);
                           }
                        }

                        return objectReader;
                     }
                  }
               }
            }
         } else {
            return this.createEnumReader(objectClass, beanInfo.createMethod, provider);
         }
      }
   }

   public <T> FieldReader[] createFieldReaders(Class<T> objectClass) {
      return this.createFieldReaders(objectClass, objectClass, null, false, JSONFactory.getDefaultObjectReaderProvider());
   }

   public <T> FieldReader[] createFieldReaders(Class<T> objectClass, Type objectType) {
      return this.createFieldReaders(objectClass, objectType, null, false, JSONFactory.getDefaultObjectReaderProvider());
   }

   protected void createFieldReader(
      Class objectClass,
      Type objectType,
      String namingStrategy,
      String[] orders,
      FieldInfo fieldInfo,
      Field field,
      Map<String, List<FieldReader>> fieldReaders,
      ObjectReaderProvider provider
   ) {
      provider.getFieldInfo(fieldInfo, objectClass, field);
      if (fieldInfo.ignore) {
         boolean unwrap = (fieldInfo.features & 562949953421312L) != 0L && Map.class.isAssignableFrom(field.getType());
         if (!unwrap) {
            return;
         }
      }

      String fieldName;
      if (fieldInfo.fieldName != null && !fieldInfo.fieldName.isEmpty()) {
         fieldName = fieldInfo.fieldName;
      } else {
         fieldName = field.getName();
         if (namingStrategy != null) {
            fieldName = BeanUtils.fieldName(fieldName, namingStrategy);
         }
      }

      if (orders != null && orders.length > 0) {
         boolean match = false;

         for (int i = 0; i < orders.length; i++) {
            if (fieldName.equals(orders[i])) {
               fieldInfo.ordinal = i;
               match = true;
               break;
            }
         }

         if (!match && fieldInfo.ordinal == 0) {
            fieldInfo.ordinal = orders.length;
         }
      }

      Type fieldType = field.getGenericType();
      Class<?> fieldClass = field.getType();
      ObjectReader initReader = getInitReader(provider, fieldType, fieldClass, fieldInfo);
      String schema = fieldInfo.schema;
      if (fieldInfo.required && schema == null) {
         schema = "{\"required\":true}";
      }

      FieldReader<Object> fieldReader = this.createFieldReader(
         objectClass,
         objectType,
         fieldName,
         fieldInfo.ordinal,
         fieldInfo.features,
         fieldInfo.format,
         fieldInfo.locale,
         fieldInfo.defaultValue,
         schema,
         fieldType,
         fieldClass,
         field,
         initReader,
         fieldInfo.arrayToMapKey,
         fieldInfo.getInitArrayToMapDuplicateHandler()
      );
      this.putIfAbsent(fieldReaders, fieldName, fieldReader, objectClass);
      if (fieldInfo.alternateNames != null) {
         for (String alternateName : fieldInfo.alternateNames) {
            if (!fieldName.equals(alternateName)) {
               this.putIfAbsent(
                  fieldReaders,
                  alternateName,
                  this.createFieldReader(
                     objectClass,
                     objectType,
                     alternateName,
                     fieldInfo.ordinal,
                     fieldInfo.features,
                     null,
                     fieldInfo.locale,
                     fieldInfo.defaultValue,
                     schema,
                     fieldType,
                     fieldClass,
                     field,
                     null
                  ),
                  objectClass
               );
            }
         }
      }
   }

   protected void createFieldReader(
      Class objectClass,
      Type objectType,
      String namingStrategy,
      String[] orders,
      BeanInfo beanInfo,
      FieldInfo fieldInfo,
      Method method,
      Map<String, List<FieldReader>> fieldReaders,
      ObjectReaderProvider provider
   ) {
      provider.getFieldInfo(fieldInfo, objectClass, method);
      if (!fieldInfo.ignore) {
         String fieldName;
         if (fieldInfo.fieldName != null && !fieldInfo.fieldName.isEmpty()) {
            fieldName = fieldInfo.fieldName;
         } else {
            String methodName = method.getName();
            if (methodName.startsWith("set")) {
               fieldName = BeanUtils.setterName(methodName, namingStrategy);
            } else {
               fieldName = BeanUtils.getterName(method, beanInfo.kotlin, namingStrategy);
            }

            char c0 = 0;
            int len = fieldName.length();
            if (len > 0) {
               c0 = fieldName.charAt(0);
            }

            char c1;
            if (len == 1 && c0 >= 'a' && c0 <= 'z' || len > 2 && c0 >= 'A' && c0 <= 'Z' && (c1 = fieldName.charAt(1)) >= 'A' && c1 <= 'Z') {
               char[] chars = fieldName.toCharArray();
               if (len == 1) {
                  chars[0] = (char)(chars[0] - ' ');
               } else {
                  chars[0] = (char)(chars[0] + ' ');
               }

               String fieldName1 = new String(chars);
               Field field = BeanUtils.getDeclaredField(objectClass, fieldName1);
               if (field != null) {
                  if (Modifier.isPublic(field.getModifiers())) {
                     fieldName = field.getName();
                  } else if (len == 1) {
                     fieldInfo.alternateNames = new String[]{fieldName};
                     fieldName = field.getName();
                  }
               }
            }
         }

         if (orders != null && orders.length > 0) {
            boolean match = false;

            for (int i = 0; i < orders.length; i++) {
               if (fieldName.equals(orders[i])) {
                  fieldInfo.ordinal = i;
                  match = true;
                  break;
               }
            }

            if (!match && fieldInfo.ordinal == 0) {
               fieldInfo.ordinal = orders.length;
            }
         }

         int parameterCount = method.getParameterCount();
         if (parameterCount == 0) {
            Type fieldType = method.getGenericReturnType();
            Class<?> fieldClass = method.getReturnType();
            FieldReader fieldReader = this.createFieldReaderMethod(
               objectClass,
               objectType,
               fieldName,
               fieldInfo.ordinal,
               fieldInfo.features,
               fieldInfo.format,
               fieldInfo.locale,
               fieldInfo.defaultValue,
               fieldInfo.schema,
               fieldType,
               fieldClass,
               method,
               fieldInfo.getInitReader(),
               fieldInfo.arrayToMapKey,
               fieldInfo.getInitArrayToMapDuplicateHandler()
            );
            this.putIfAbsent(fieldReaders, fieldName, fieldReader, objectClass);
         } else if (parameterCount == 2) {
            Class<?> fieldClass = method.getParameterTypes()[1];
            Type fieldType = method.getGenericParameterTypes()[1];
            method.setAccessible(true);
            FieldReaderAnySetter anySetter = new FieldReaderAnySetter(
               fieldType, fieldClass, fieldInfo.ordinal, fieldInfo.features, fieldInfo.format, null, method
            );
            fieldReaders.put(anySetter.fieldName, this.listOf(anySetter));
         } else {
            Type fieldType = method.getGenericParameterTypes()[0];
            Class fieldClass = method.getParameterTypes()[0];
            if (fieldType instanceof Class && Collection.class.isAssignableFrom((Class<?>)fieldType)) {
               Class[] interfaces = objectClass.getInterfaces();

               for (int ix = 0; ix < interfaces.length; ix++) {
                  Method interfaceMethod = BeanUtils.getMethod(interfaces[ix], method);
                  if (interfaceMethod != null) {
                     Type[] genericParameterTypes = interfaceMethod.getGenericParameterTypes();
                     if (genericParameterTypes.length == 1 && genericParameterTypes[0] instanceof ParameterizedType) {
                        fieldType = genericParameterTypes[0];
                     }
                  }
               }
            }

            if (!TypeUtils.isFunction(fieldClass)) {
               ObjectReader initReader = getInitReader(provider, fieldType, fieldClass, fieldInfo);
               FieldReader fieldReader = null;
               boolean jit = (fieldInfo.features & 18014398509481984L) != 0L;
               if (jit) {
                  try {
                     fieldReader = this.createFieldReaderLambda(
                        objectClass,
                        objectType,
                        fieldName,
                        fieldInfo.ordinal,
                        fieldInfo.features,
                        fieldInfo.format,
                        fieldInfo.locale,
                        fieldInfo.defaultValue,
                        fieldInfo.schema,
                        fieldType,
                        fieldClass,
                        method,
                        initReader
                     );
                  } catch (Throwable var21) {
                     this.jitErrorCount.incrementAndGet();
                     this.jitErrorLast = var21;
                  }
               }

               if (fieldReader == null) {
                  fieldReader = this.createFieldReaderMethod(
                     objectClass,
                     objectType,
                     fieldName,
                     fieldInfo.ordinal,
                     fieldInfo.features,
                     fieldInfo.format,
                     fieldInfo.locale,
                     fieldInfo.defaultValue,
                     fieldInfo.schema,
                     fieldType,
                     fieldClass,
                     method,
                     initReader,
                     fieldInfo.arrayToMapKey,
                     fieldInfo.getInitArrayToMapDuplicateHandler()
                  );
               }

               this.putIfAbsent(fieldReaders, fieldName, fieldReader, objectClass);
               if (fieldInfo.alternateNames != null) {
                  for (String alternateName : fieldInfo.alternateNames) {
                     if (!fieldName.equals(alternateName)) {
                        this.putIfAbsent(
                           fieldReaders,
                           alternateName,
                           this.createFieldReaderMethod(
                              objectClass,
                              objectType,
                              alternateName,
                              fieldInfo.ordinal,
                              fieldInfo.features,
                              fieldInfo.format,
                              fieldInfo.locale,
                              fieldInfo.defaultValue,
                              fieldInfo.schema,
                              fieldType,
                              fieldClass,
                              method,
                              initReader
                           ),
                           objectClass
                        );
                     }
                  }
               }
            }
         }
      }
   }

   protected <T> FieldReader[] createFieldReaders(Class<T> objectClass, Type objectType, BeanInfo beanInfo, boolean fieldBased, ObjectReaderProvider provider) {
      if (beanInfo == null) {
         beanInfo = new BeanInfo(provider);

         for (ObjectReaderModule module : provider.modules) {
            ObjectReaderAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
            if (annotationProcessor != null) {
               annotationProcessor.getBeanInfo(beanInfo, objectClass);
            }
         }
      }

      boolean record = BeanUtils.isRecord(objectClass);
      String namingStrategy = beanInfo.namingStrategy;
      Map<String, List<FieldReader>> fieldReaders = new LinkedHashMap<>();
      BeanInfo finalBeanInfo = beanInfo;
      long beanFeatures = beanInfo.readerFeatures;
      String beanFormat = beanInfo.format;
      FieldInfo fieldInfo = new FieldInfo();
      String[] orders = beanInfo.orders;
      if (fieldBased) {
         BeanUtils.declaredFields(objectClass, field -> {
            fieldInfo.init();
            fieldInfo.features = fieldInfo.features | JSONReader.Feature.FieldBased.mask;
            fieldInfo.features |= beanFeatures;
            fieldInfo.format = beanFormat;
            this.createFieldReader(objectClass, objectType, namingStrategy, orders, fieldInfo, field, fieldReaders, provider);
         });
      } else {
         if (!record) {
            BeanUtils.declaredFields(objectClass, field -> {
               fieldInfo.init();
               fieldInfo.ignore = (field.getModifiers() & 1) == 0 && (beanFeatures & JSONReader.Feature.FieldBased.mask) == 0L;
               fieldInfo.features |= beanFeatures;
               fieldInfo.format = beanFormat;
               this.createFieldReader(objectClass, objectType, namingStrategy, orders, fieldInfo, field, fieldReaders, provider);
               if (fieldInfo.required) {
                  String fieldName = fieldInfo.fieldName;
                  if (fieldName == null || fieldName.isEmpty()) {
                     fieldName = field.getName();
                  }

                  finalBeanInfo.required(fieldName);
               }
            });
         }

         Class mixIn = provider.getMixIn(objectClass);
         BeanUtils.setters(objectClass, beanInfo, mixIn, method -> {
            fieldInfo.init();
            fieldInfo.features |= beanFeatures;
            fieldInfo.format = beanFormat;
            this.createFieldReader(objectClass, objectType, namingStrategy, orders, finalBeanInfo, fieldInfo, method, fieldReaders, provider);
         });
         if (objectClass.isInterface()) {
            BeanUtils.getters(objectClass, method -> {
               fieldInfo.init();
               fieldInfo.features |= beanFeatures;
               this.createFieldReader(objectClass, objectType, namingStrategy, orders, finalBeanInfo, fieldInfo, method, fieldReaders, provider);
            });
         }
      }

      Class<? super T> superclass = objectClass.getSuperclass();
      if (BeanUtils.isExtendedMap(objectClass)) {
         Type superType = objectClass.getGenericSuperclass();
         FieldReader fieldReader = ObjectReaders.fieldReader("$super$", superType, superclass, (o, f) -> {
            Map thisMap = (Map)o;
            Map superMap = (Map)f;

            for (Object value : superMap.entrySet()) {
               Entry entry = (Entry)value;
               thisMap.put(entry.getKey(), entry.getValue());
            }
         });
         fieldReaders.put("$super$", this.listOf(fieldReader));
      }

      return this.toFieldReaderArray(fieldReaders);
   }

   public <T> Supplier<T> createSupplier(Class<T> objectClass) {
      if (objectClass.isInterface()) {
         return null;
      } else {
         int modifiers = objectClass.getModifiers();
         if (Modifier.isAbstract(modifiers)) {
            return null;
         } else {
            Constructor<T> constructor;
            try {
               constructor = objectClass.getDeclaredConstructor();
               constructor.setAccessible(true);
            } catch (NoSuchMethodException var5) {
               return null;
            } catch (Throwable var6) {
               throw new JSONException("get constructor error, class " + objectClass.getName(), var6);
            }

            return this.createSupplier(constructor, true);
         }
      }
   }

   public <T> Supplier<T> createSupplier(Constructor constructor, boolean jit) {
      jit &= JIT;
      if (jit) {
         Class declaringClass = constructor.getDeclaringClass();
         Lookup lookup = JDKUtils.trustedLookup(declaringClass);

         try {
            if (constructor.getParameterCount() == 0) {
               MethodHandle handle = lookup.findConstructor(declaringClass, TypeUtils.METHOD_TYPE_VOID);
               CallSite callSite = LambdaMetafactory.metafactory(
                  lookup, "get", TypeUtils.METHOD_TYPE_SUPPLIER, TypeUtils.METHOD_TYPE_OBJECT, handle, TypeUtils.METHOD_TYPE_OBJECT
               );
               return (Supplier)callSite.getTarget().invokeExact();
            }
         } catch (Throwable var7) {
            this.jitErrorCount.incrementAndGet();
            this.jitErrorLast = var7;
         }
      }

      return new ConstructorSupplier(constructor);
   }

   protected <T> IntFunction<T> createIntFunction(Constructor constructor) {
      Class declaringClass = constructor.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodHandle handle = lookup.findConstructor(declaringClass, TypeUtils.METHOD_TYPE_VOID_INT);
         MethodType instantiatedMethodType = MethodType.methodType(declaringClass, int.class);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_INT_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_INT, handle, instantiatedMethodType
         );
         return (IntFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var7) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var7;
         return null;
      }
   }

   protected <T> IntFunction<T> createIntFunction(Method factoryMethod) {
      Class declaringClass = factoryMethod.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodType methodType = MethodType.methodType(factoryMethod.getReturnType(), int.class);
         MethodHandle handle = lookup.findStatic(declaringClass, factoryMethod.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_INT_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_INT, handle, methodType
         );
         return (IntFunction)callSite.getTarget().invokeExact();
      } catch (Throwable var7) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var7;
         return null;
      }
   }

   protected <T> Function<String, T> createStringFunction(Constructor constructor) {
      Class declaringClass = constructor.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodHandle handle = lookup.findConstructor(declaringClass, TypeUtils.METHOD_TYPE_VOID_STRING);
         MethodType instantiatedMethodType = MethodType.methodType(declaringClass, String.class);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, handle, instantiatedMethodType
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var7) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var7;
         return null;
      }
   }

   protected <T> Function<String, T> createStringFunction(Method factoryMethod) {
      Class declaringClass = factoryMethod.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodType methodType = MethodType.methodType(factoryMethod.getReturnType(), String.class);
         MethodHandle handle = lookup.findStatic(declaringClass, factoryMethod.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, handle, methodType
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var7) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var7;
         return null;
      }
   }

   protected <I, T> Function<I, T> createValueFunction(Constructor<T> constructor, Class<I> valueClass) {
      Class declaringClass = constructor.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodType methodType = MethodType.methodType(void.class, valueClass);
         MethodHandle handle = lookup.findConstructor(declaringClass, methodType);
         MethodType instantiatedMethodType = MethodType.methodType(declaringClass, valueClass);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, handle, instantiatedMethodType
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var9) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var9;
         return null;
      }
   }

   protected <I, T> Function<I, T> createValueFunction(Method factoryMethod, Class valueClass) {
      Class declaringClass = factoryMethod.getDeclaringClass();
      Lookup lookup = JDKUtils.trustedLookup(declaringClass);

      try {
         MethodType methodType = MethodType.methodType(factoryMethod.getReturnType(), valueClass);
         MethodHandle handle = lookup.findStatic(declaringClass, factoryMethod.getName(), methodType);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, TypeUtils.METHOD_TYPE_OBJECT_OBJECT, handle, methodType
         );
         return (Function)callSite.getTarget().invokeExact();
      } catch (Throwable var8) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var8;
         return null;
      }
   }

   public <T, R> Function<T, R> createBuildFunction(Method builderMethod) {
      try {
         return this.createBuildFunctionLambda(builderMethod);
      } catch (Throwable var3) {
         this.jitErrorCount.incrementAndGet();
         this.jitErrorLast = var3;
         builderMethod.setAccessible(true);
         return o -> {
            try {
               return (R)builderMethod.invoke(o);
            } catch (Throwable var3x) {
               throw new JSONException("create instance error", var3x);
            }
         };
      }
   }

   <T, R> Function<T, R> createBuildFunctionLambda(Method builderMethod) {
      Lookup lookup = JDKUtils.trustedLookup(builderMethod.getDeclaringClass());

      try {
         MethodHandle target = lookup.findVirtual(
            builderMethod.getDeclaringClass(), builderMethod.getName(), MethodType.methodType(builderMethod.getReturnType())
         );
         MethodType func = target.type();
         CallSite callSite = LambdaMetafactory.metafactory(lookup, "apply", TypeUtils.METHOD_TYPE_FUNCTION, func.erase(), target, func);
         Object object = (Object)callSite.getTarget().invoke();
         return (Function<T, R>)object;
      } catch (Throwable var7) {
         throw new JSONException("create fieldReader error", var7);
      }
   }

   public <T> FieldReader createFieldReader(Class<T> objectType, String fieldName, Type fieldType, Class fieldClass, Method method) {
      return this.createFieldReaderMethod(objectType, objectType, fieldName, 0, 0L, null, null, null, null, fieldType, fieldClass, method, null);
   }

   public <T> FieldReader createFieldReader(Class<T> objectType, String fieldName, String format, Type fieldType, Class fieldClass, Method method) {
      return this.createFieldReaderMethod(objectType, fieldName, format, fieldType, fieldClass, method);
   }

   public <T> FieldReader createFieldReaderMethod(Class<T> objectClass, String fieldName, String format, Type fieldType, Class fieldClass, Method method) {
      return this.createFieldReaderMethod(objectClass, objectClass, fieldName, 0, 0L, format, null, null, null, fieldType, fieldClass, method, null);
   }

   public <T> FieldReader createFieldReaderParam(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Type fieldType,
      Class fieldClass,
      String paramName,
      Class declaringClass,
      Parameter parameter,
      JSONSchema schema
   ) {
      return this.createFieldReaderParam(
         objectClass, objectType, fieldName, ordinal, features, format, fieldType, fieldClass, paramName, declaringClass, parameter, schema, null
      );
   }

   public <T> FieldReader createFieldReaderParam(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Type fieldType,
      Class fieldClass,
      String paramName,
      Class declaringClass,
      Parameter parameter,
      JSONSchema schema,
      ObjectReader initReader
   ) {
      return this.createFieldReaderParam(
         objectClass,
         objectType,
         fieldName,
         ordinal,
         features,
         format,
         null,
         null,
         fieldType,
         fieldClass,
         paramName,
         declaringClass,
         parameter,
         schema,
         initReader
      );
   }

   public <T> FieldReader createFieldReaderParam(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      Type fieldType,
      Class fieldClass,
      String paramName,
      Class declaringClass,
      Parameter parameter,
      JSONSchema schema,
      ObjectReader initReader
   ) {
      if (defaultValue instanceof String && fieldClass.isEnum()) {
         defaultValue = Enum.valueOf(fieldClass, (String)defaultValue);
      }

      if (initReader != null) {
         FieldReaderObjectParam paramReader = new FieldReaderObjectParam(
            fieldName, fieldType, fieldClass, paramName, parameter, ordinal, features, format, locale, defaultValue, schema
         );
         paramReader.initReader = initReader;
         return paramReader;
      } else if (fieldType == byte.class || fieldType == Byte.class) {
         return new FieldReaderInt8Param(fieldName, fieldClass, paramName, parameter, ordinal, features, format, locale, defaultValue, schema);
      } else if (fieldType == short.class || fieldType == Short.class) {
         return new FieldReaderInt16Param(fieldName, fieldClass, paramName, parameter, ordinal, features, format, locale, defaultValue, schema);
      } else if (fieldType == int.class || fieldType == Integer.class) {
         return new FieldReaderInt32Param(fieldName, fieldClass, paramName, parameter, ordinal, features, format, locale, defaultValue, schema);
      } else if (fieldType != long.class && fieldType != Long.class) {
         Type fieldTypeResolved = null;
         Class fieldClassResolved = null;
         if (!(fieldType instanceof Class) && objectType != null) {
            fieldTypeResolved = BeanUtils.getParamType(TypeReference.get(objectType), objectClass, declaringClass, parameter, fieldType);
            if (fieldTypeResolved != null) {
               fieldClassResolved = TypeUtils.getMapping(fieldTypeResolved);
            }
         }

         if (fieldTypeResolved == null) {
            fieldTypeResolved = fieldType;
         }

         if (fieldClassResolved == null) {
            fieldClassResolved = fieldClass;
         }

         return new FieldReaderObjectParam(
            fieldName, fieldTypeResolved, fieldClassResolved, paramName, parameter, ordinal, features, format, locale, defaultValue, schema
         );
      } else {
         return new FieldReaderInt64Param(fieldName, fieldClass, paramName, parameter, ordinal, features, format, locale, defaultValue, schema);
      }
   }

   public <T> FieldReader createFieldReaderMethod(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      String schema,
      Type fieldType,
      Class fieldClass,
      Method method,
      ObjectReader initReader
   ) {
      return this.createFieldReaderMethod(
         objectClass, objectType, fieldName, ordinal, features, format, locale, defaultValue, schema, fieldType, fieldClass, method, initReader, null, null
      );
   }

   public <T> FieldReader createFieldReaderMethod(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      String schema,
      Type fieldType,
      Class fieldClass,
      Method method,
      ObjectReader initReader,
      String keyName,
      BiConsumer arrayToMapDuplicateHandler
   ) {
      if (method != null) {
         method.setAccessible(true);
      }

      if (defaultValue instanceof String && fieldClass.isEnum()) {
         defaultValue = Enum.valueOf(fieldClass, (String)defaultValue);
      }

      if (defaultValue != null && defaultValue.getClass() != fieldClass) {
         Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(defaultValue.getClass(), fieldType);
         if (typeConvert == null) {
            throw new JSONException("illegal defaultValue : " + defaultValue + ", class " + fieldClass.getName());
         }

         defaultValue = typeConvert.apply(defaultValue);
      }

      JSONSchema jsonSchema = null;
      if (schema != null && !schema.isEmpty()) {
         JSONObject object = JSON.parseObject(schema);
         if (!object.isEmpty()) {
            jsonSchema = JSONSchema.of(object, fieldClass);
         }
      }

      if (initReader != null) {
         FieldReaderObject fieldReaderObjectMethod = new FieldReaderObject(
            fieldName, fieldType, fieldClass, ordinal, features | 2251799813685248L, format, locale, defaultValue, jsonSchema, method, null, null
         );
         fieldReaderObjectMethod.initReader = initReader;
         return fieldReaderObjectMethod;
      } else if (fieldType == boolean.class) {
         return new FieldReaderBoolValueMethod(fieldName, ordinal, features, format, (Boolean)defaultValue, jsonSchema, method);
      } else if (fieldType == Boolean.class) {
         return new FieldReaderBoolMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Boolean)defaultValue, jsonSchema, method);
      } else if (fieldType == byte.class) {
         return new FieldReaderInt8ValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Byte)defaultValue, jsonSchema, method);
      } else if (fieldType == short.class) {
         return new FieldReaderInt16ValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Short)defaultValue, jsonSchema, method);
      } else if (fieldType == int.class) {
         return new FieldReaderInt32ValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, (Integer)defaultValue, jsonSchema, method);
      } else if (fieldType == long.class) {
         return new FieldReaderInt64ValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Long)defaultValue, jsonSchema, method);
      } else if (fieldType == float.class) {
         return new FieldReaderFloatValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Float)defaultValue, jsonSchema, method);
      } else if (fieldType == double.class) {
         return new FieldReaderDoubleValueMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Double)defaultValue, jsonSchema, method);
      } else if (fieldType == Byte.class) {
         return new FieldReaderInt8Method(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Byte)defaultValue, jsonSchema, method);
      } else if (fieldType == Short.class) {
         return new FieldReaderInt16Method(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (Short)defaultValue, jsonSchema, method);
      } else if (fieldType == Integer.class) {
         return new FieldReaderInt32Method(fieldName, ordinal, features, format, locale, (Integer)defaultValue, jsonSchema, method);
      } else if (fieldType == Long.class) {
         return new FieldReaderInt64Method(fieldName, ordinal, features, format, locale, (Long)defaultValue, jsonSchema, method);
      } else if (fieldType == Float.class) {
         return new FieldReaderFloatMethod(fieldName, ordinal, features, format, locale, (Float)defaultValue, jsonSchema, method);
      } else if (fieldType == Double.class) {
         return new FieldReaderDoubleMethod(fieldName, ordinal, features, format, (Double)defaultValue, jsonSchema, method);
      } else if (fieldClass == BigDecimal.class) {
         return new FieldReaderBigDecimalMethod(
            fieldName, fieldType, fieldClass, ordinal, features, format, locale, (BigDecimal)defaultValue, jsonSchema, method
         );
      } else if (fieldClass == BigInteger.class) {
         return new FieldReaderBigIntegerMethod(
            fieldName, fieldType, fieldClass, ordinal, features, format, locale, (BigInteger)defaultValue, jsonSchema, method
         );
      } else if (fieldType == String.class) {
         return new FieldReaderStringMethod(fieldName, fieldType, fieldClass, ordinal, features, format, locale, (String)defaultValue, jsonSchema, method);
      } else if (fieldType == LocalDate.class) {
         return new FieldReaderLocalDate(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, method, null, null);
      } else if (fieldType == OffsetDateTime.class) {
         return new FieldReaderOffsetDateTime(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, method, null, null);
      } else if (fieldType == UUID.class) {
         return new FieldReaderUUID(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, method, null, null);
      } else if (fieldType == String[].class) {
         return new FieldReaderStringArray(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, method, null, null);
      } else {
         Type fieldTypeResolved = null;
         Class fieldClassResolved = null;
         if (!(fieldType instanceof Class) || !(objectType instanceof Class)) {
            fieldTypeResolved = BeanUtils.getFieldType(TypeReference.get(objectType), objectClass, method, fieldType);
            fieldClassResolved = TypeUtils.getMapping(fieldTypeResolved);
         }

         if (method.getParameterCount() == 0) {
            if (fieldClass == AtomicInteger.class) {
               return new FieldReaderAtomicIntegerMethodReadOnly(fieldName, fieldClass, ordinal, jsonSchema, method);
            }

            if (fieldClass == AtomicLong.class) {
               return new FieldReaderAtomicLongReadOnly(fieldName, fieldClass, ordinal, jsonSchema, method);
            }

            if (fieldClass == AtomicIntegerArray.class) {
               return new FieldReaderAtomicIntegerArrayReadOnly(fieldName, fieldClass, ordinal, jsonSchema, method);
            }

            if (fieldClass == AtomicLongArray.class) {
               return new FieldReaderAtomicLongArrayReadOnly(fieldName, fieldClass, ordinal, jsonSchema, method);
            }

            if (fieldClass == AtomicBoolean.class) {
               return new FieldReaderAtomicBooleanMethodReadOnly(fieldName, fieldClass, ordinal, jsonSchema, method);
            }

            if (fieldClass == AtomicReference.class) {
               return new FieldReaderAtomicReferenceMethodReadOnly(fieldName, fieldType, fieldClass, ordinal, jsonSchema, method);
            }

            if (Collection.class.isAssignableFrom(fieldClass)) {
               Field field = null;
               String methodName = method.getName();
               if (methodName.startsWith("get")) {
                  String getterName = BeanUtils.getterName(methodName, PropertyNamingStrategy.CamelCase.name());
                  field = BeanUtils.getDeclaredField(method.getDeclaringClass(), getterName);
               }

               return new FieldReaderCollectionMethodReadOnly(
                  fieldName, fieldTypeResolved != null ? fieldTypeResolved : fieldType, fieldClass, ordinal, features, format, jsonSchema, method, field
               );
            }

            if (Map.class.isAssignableFrom(fieldClass)) {
               Field field = null;
               String methodName = method.getName();
               if (methodName.startsWith("get")) {
                  String getterName = BeanUtils.getterName(methodName, PropertyNamingStrategy.CamelCase.name());
                  field = BeanUtils.getDeclaredField(method.getDeclaringClass(), getterName);
               }

               return new FieldReaderMapMethodReadOnly(
                  fieldName, fieldType, fieldClass, ordinal, features, format, jsonSchema, method, field, keyName, arrayToMapDuplicateHandler
               );
            }

            if (!objectClass.isInterface()) {
               return null;
            }
         }

         boolean list = fieldClass == List.class
            || fieldClass == ArrayList.class
            || fieldClass == LinkedList.class
            || "cn.hutool.json.JSONArray".equals(fieldClass.getName());
         if (list) {
            if (fieldTypeResolved instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)fieldTypeResolved;
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  Type itemType = actualTypeArguments[0];
                  Class itemClass = TypeUtils.getMapping(itemType);
                  if (itemClass == String.class) {
                     return new FieldReaderList(
                        fieldName,
                        fieldTypeResolved,
                        fieldClass,
                        String.class,
                        String.class,
                        ordinal,
                        features,
                        format,
                        locale,
                        null,
                        jsonSchema,
                        method,
                        null,
                        null
                     );
                  }

                  return new FieldReaderList(
                     fieldName,
                     fieldTypeResolved,
                     fieldClassResolved,
                     itemType,
                     itemClass,
                     ordinal,
                     features,
                     format,
                     locale,
                     null,
                     jsonSchema,
                     method,
                     null,
                     null
                  );
               }
            }

            return new FieldReaderList(
               fieldName, fieldType, fieldClass, Object.class, Object.class, ordinal, features, format, locale, null, jsonSchema, method, null, null
            );
         } else if (fieldClass == Date.class) {
            return new FieldReaderDate(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, null, method, null);
         } else if (fieldClass == StackTraceElement[].class && method.getDeclaringClass() == Throwable.class) {
            return new FieldReaderStackTrace(
               fieldName,
               fieldTypeResolved != null ? fieldTypeResolved : fieldType,
               fieldClass,
               ordinal,
               features,
               format,
               locale,
               defaultValue,
               jsonSchema,
               method,
               null,
               Throwable::setStackTrace
            );
         } else {
            Field field = null;
            if ((features & 562949953421312L) != 0L) {
               String methodName = method.getName();
               if (methodName.startsWith("set")) {
                  String setterName = BeanUtils.setterName(methodName, PropertyNamingStrategy.CamelCase.name());
                  field = BeanUtils.getDeclaredField(method.getDeclaringClass(), setterName);

                  try {
                     field.setAccessible(true);
                  } catch (Throwable var25) {
                  }
               }
            }

            return (FieldReader)(Map.class.isAssignableFrom(fieldClass)
               ? new FieldReaderMapMethod(
                  fieldName,
                  fieldTypeResolved != null ? fieldTypeResolved : fieldType,
                  fieldClass,
                  ordinal,
                  features,
                  format,
                  locale,
                  defaultValue,
                  jsonSchema,
                  method,
                  field,
                  null,
                  keyName,
                  arrayToMapDuplicateHandler
               )
               : new FieldReaderObject(
                  fieldName,
                  fieldTypeResolved != null ? fieldTypeResolved : fieldType,
                  fieldClass,
                  ordinal,
                  features,
                  format,
                  locale,
                  defaultValue,
                  jsonSchema,
                  method,
                  field,
                  null
               ));
         }
      }
   }

   public <T> FieldReader<T> createFieldReader(String fieldName, Type fieldType, Field field) {
      return this.createFieldReader(fieldName, null, fieldType, field);
   }

   public <T> FieldReader<T> createFieldReader(String fieldName, Field field) {
      return this.createFieldReader(fieldName, null, field.getGenericType(), field);
   }

   public <T> FieldReader createFieldReader(String fieldName, Method method) {
      Class<?> declaringClass = method.getDeclaringClass();
      int parameterCount = method.getParameterCount();
      Class fieldClass;
      Type fieldType;
      if (parameterCount == 0) {
         fieldClass = method.getReturnType();
         fieldType = method.getGenericReturnType();
      } else {
         if (parameterCount != 1) {
            throw new JSONException("illegal setter method " + method);
         }

         fieldClass = method.getParameterTypes()[0];
         fieldType = method.getGenericParameterTypes()[0];
      }

      return this.createFieldReaderMethod(declaringClass, declaringClass, fieldName, 0, 0L, null, null, null, null, fieldType, fieldClass, method, null);
   }

   public <T> FieldReader<T> createFieldReader(String fieldName, String format, Type fieldType, Field field) {
      Class objectClass = field.getDeclaringClass();
      return this.createFieldReader(objectClass, objectClass, fieldName, 0L, format, fieldType, field.getType(), field);
   }

   public <T> FieldReader<T> createFieldReader(
      Class objectClass, Type objectType, String fieldName, long features, String format, Type fieldType, Class fieldClass, Field field
   ) {
      return this.createFieldReader(
         objectClass, objectType, fieldName, 0, features, format, null, null, null, fieldType, field.getType(), field, null, null, null
      );
   }

   public <T> FieldReader<T> createFieldReader(
      Class objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      String schema,
      Type fieldType,
      Class fieldClass,
      Field field,
      ObjectReader initReader
   ) {
      return this.createFieldReader(
         objectClass, objectType, fieldName, 0, features, format, locale, defaultValue, schema, fieldType, field.getType(), field, initReader, null, null
      );
   }

   public <T> FieldReader<T> createFieldReader(
      Class objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      String schema,
      Type fieldType,
      Class fieldClass,
      Field field,
      ObjectReader initReader,
      String keyName,
      BiConsumer arrayToMapDuplicateHandler
   ) {
      if (defaultValue instanceof String && fieldClass.isEnum()) {
         defaultValue = Enum.valueOf(fieldClass, (String)defaultValue);
      }

      if (defaultValue != null && defaultValue.getClass() != fieldClass) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Function typeConvert = provider.getTypeConvert(defaultValue.getClass(), fieldType);
         if (typeConvert == null) {
            throw new JSONException("illegal defaultValue : " + defaultValue + ", class " + fieldClass.getName());
         }

         defaultValue = typeConvert.apply(defaultValue);
      }

      JSONSchema jsonSchema = null;
      if (schema != null && !schema.isEmpty()) {
         JSONObject object = JSON.parseObject(schema);
         if (!object.isEmpty()) {
            jsonSchema = JSONSchema.of(object, fieldClass);
         }
      }

      if (field != null) {
         String objectClassName = objectClass.getName();
         if (!objectClassName.startsWith("java.lang")
            && !objectClassName.startsWith("java.time")
            && !field.getDeclaringClass().getName().startsWith("java.lang")
            && !field.getDeclaringClass().getName().startsWith("java.time")) {
            field.setAccessible(true);
         }
      }

      if (initReader != null) {
         FieldReaderObjectField fieldReader = new FieldReaderObjectField(
            fieldName, fieldType, fieldClass, ordinal, features | 2251799813685248L, format, locale, defaultValue, jsonSchema, field
         );
         fieldReader.initReader = initReader;
         return fieldReader;
      } else if (fieldClass == int.class) {
         return new FieldReaderInt32ValueField<>(fieldName, fieldClass, ordinal, format, (Integer)defaultValue, jsonSchema, field);
      } else if (fieldClass == Integer.class) {
         return new FieldReaderInt32Field<>(fieldName, fieldClass, ordinal, features, format, (Integer)defaultValue, jsonSchema, field);
      } else if (fieldClass == long.class) {
         return new FieldReaderInt64ValueField<>(fieldName, fieldClass, ordinal, features, format, (Long)defaultValue, jsonSchema, field);
      } else if (fieldClass == Long.class) {
         return new FieldReaderInt64Field<>(fieldName, fieldClass, ordinal, features, format, (Long)defaultValue, jsonSchema, field);
      } else if (fieldClass == short.class) {
         return new FieldReaderInt16ValueField<>(fieldName, fieldClass, ordinal, features, format, (Short)defaultValue, jsonSchema, field);
      } else if (fieldClass == Short.class) {
         return new FieldReaderInt16Field<>(fieldName, fieldClass, ordinal, features, format, (Short)defaultValue, jsonSchema, field);
      } else if (fieldClass == boolean.class) {
         return new FieldReaderBoolValueField<>(fieldName, ordinal, features, format, (Boolean)defaultValue, jsonSchema, field);
      } else if (fieldClass == Boolean.class) {
         return new FieldReaderBoolField<>(fieldName, fieldClass, ordinal, features, format, (Boolean)defaultValue, jsonSchema, field);
      } else if (fieldClass == byte.class) {
         return new FieldReaderInt8ValueField<>(fieldName, fieldClass, ordinal, features, format, (Byte)defaultValue, jsonSchema, field);
      } else if (fieldClass == Byte.class) {
         return new FieldReaderInt8Field<>(fieldName, fieldClass, ordinal, features, format, (Byte)defaultValue, jsonSchema, field);
      } else if (fieldClass == float.class) {
         return new FieldReaderFloatValueField<>(fieldName, fieldClass, ordinal, features, format, (Float)defaultValue, jsonSchema, field);
      } else if (fieldClass == Float.class) {
         return new FieldReaderFloatField<>(fieldName, fieldClass, ordinal, features, format, (Float)defaultValue, jsonSchema, field);
      } else if (fieldClass == double.class) {
         return new FieldReaderDoubleValueField<>(fieldName, fieldClass, ordinal, features, format, (Double)defaultValue, jsonSchema, field);
      } else if (fieldClass == Double.class) {
         return new FieldReaderDoubleField<>(fieldName, fieldClass, ordinal, features, format, (Double)defaultValue, jsonSchema, field);
      } else if (fieldClass == char.class) {
         return new FieldReaderCharValueField<>(fieldName, ordinal, features, format, (Character)defaultValue, jsonSchema, field);
      } else if (fieldClass == BigDecimal.class) {
         return new FieldReaderBigDecimalField<>(fieldName, fieldClass, ordinal, features, format, (BigDecimal)defaultValue, jsonSchema, field);
      } else if (fieldClass == BigInteger.class) {
         return new FieldReaderBigIntegerField<>(fieldName, fieldClass, ordinal, features, format, (BigInteger)defaultValue, jsonSchema, field);
      } else if (fieldClass == String.class) {
         return new FieldReaderStringField<>(fieldName, fieldClass, ordinal, features, format, (String)defaultValue, jsonSchema, field);
      } else if (fieldType == String[].class) {
         return new FieldReaderStringArray(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, null, field, null);
      } else if (fieldClass == Date.class) {
         return new FieldReaderDate<>(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field, null, null);
      } else if (fieldClass == AtomicBoolean.class) {
         return new FieldReaderAtomicBooleanFieldReadOnly<>(fieldName, fieldClass, ordinal, format, (AtomicBoolean)defaultValue, jsonSchema, field);
      } else if (fieldClass == AtomicReference.class) {
         return new FieldReaderAtomicReferenceField<>(fieldName, fieldType, fieldClass, ordinal, format, jsonSchema, field);
      } else {
         Type fieldTypeResolved = null;
         Class fieldClassResolved = null;
         if (!(fieldType instanceof Class)) {
            fieldTypeResolved = BeanUtils.getFieldType(TypeReference.get(objectType), objectClass, field, fieldType);
            fieldClassResolved = TypeUtils.getMapping(fieldTypeResolved);
         }

         boolean finalField = Modifier.isFinal(field.getModifiers());
         if (Collection.class.isAssignableFrom(fieldClass)) {
            if (fieldTypeResolved instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)fieldTypeResolved;
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  Type itemType = actualTypeArguments[0];
                  Class itemClass = TypeUtils.getMapping(itemType);
                  if (itemClass == String.class) {
                     if (finalField) {
                        if ((features & JSONReader.Feature.FieldBased.mask) != 0L) {
                           return new FieldReaderListField<>(
                              fieldName,
                              fieldTypeResolved,
                              fieldClassResolved,
                              String.class,
                              String.class,
                              ordinal,
                              features,
                              format,
                              locale,
                              null,
                              jsonSchema,
                              field
                           );
                        }

                        return new FieldReaderCollectionFieldReadOnly<>(
                           fieldName, fieldTypeResolved, fieldClassResolved, ordinal, features, format, jsonSchema, field
                        );
                     }

                     return new FieldReaderListField<>(
                        fieldName,
                        fieldTypeResolved,
                        fieldClassResolved,
                        String.class,
                        String.class,
                        ordinal,
                        features,
                        format,
                        locale,
                        null,
                        jsonSchema,
                        field
                     );
                  }

                  return new FieldReaderListField<>(
                     fieldName,
                     fieldTypeResolved,
                     fieldClassResolved,
                     itemType,
                     itemClass,
                     ordinal,
                     features,
                     format,
                     locale,
                     (Collection)defaultValue,
                     jsonSchema,
                     field
                  );
               }
            }

            Type itemType = null;
            if (fieldType instanceof ParameterizedType) {
               Type[] actualTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
               if (actualTypeArguments.length > 0) {
                  itemType = actualTypeArguments[0];
               }
            }

            if (itemType == null) {
               itemType = Object.class;
            }

            Class itemClass = TypeUtils.getClass(itemType);
            return new FieldReaderListField<>(
               fieldName, fieldType, fieldClass, itemType, itemClass, ordinal, features, format, locale, (Collection)defaultValue, jsonSchema, field
            );
         } else {
            if (Map.class.isAssignableFrom(fieldClass) && fieldTypeResolved instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)fieldTypeResolved;
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 2 && finalField && (features & JSONReader.Feature.FieldBased.mask) == 0L) {
                  return new FieldReaderMapFieldReadOnly<>(
                     fieldName, fieldTypeResolved, fieldClassResolved, ordinal, features, format, jsonSchema, field, keyName, arrayToMapDuplicateHandler
                  );
               }
            }

            if (finalField) {
               if (fieldClass == int[].class) {
                  return new FieldReaderInt32ValueArrayFinalField<>(fieldName, fieldClass, ordinal, features, format, (int[])defaultValue, jsonSchema, field);
               }

               if (fieldClass == long[].class) {
                  return new FieldReaderInt64ValueArrayFinalField<>(fieldName, fieldClass, ordinal, features, format, (long[])defaultValue, jsonSchema, field);
               }
            }

            if (fieldClassResolved != null) {
               if ((features & 562949953421312L) != 0L && Map.class.isAssignableFrom(fieldClassResolved)) {
                  return new FieldReaderMapFieldReadOnly<>(
                     fieldName, fieldTypeResolved, fieldClass, ordinal, features, format, jsonSchema, field, keyName, arrayToMapDuplicateHandler
                  );
               } else if (Map.class.isAssignableFrom(fieldClassResolved)) {
                  return (FieldReader<T>)((features & 562949953421312L) != 0L
                     ? new FieldReaderMapFieldReadOnly<>(
                        fieldName, fieldTypeResolved, fieldClass, ordinal, features, format, jsonSchema, field, keyName, arrayToMapDuplicateHandler
                     )
                     : new FieldReaderMapField<>(
                        fieldName,
                        fieldTypeResolved,
                        fieldClass,
                        ordinal,
                        features,
                        format,
                        locale,
                        defaultValue,
                        jsonSchema,
                        field,
                        keyName,
                        arrayToMapDuplicateHandler
                     ));
               } else {
                  return new FieldReaderObjectField<>(
                     fieldName, fieldTypeResolved, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field
                  );
               }
            } else if (fieldClass == LocalDateTime.class) {
               return new FieldReaderLocalDateTime<>(
                  fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field, null, null
               );
            } else if (fieldClass == ZonedDateTime.class) {
               return new FieldReaderZonedDateTime<>(
                  fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field, null, null
               );
            } else {
               return (FieldReader<T>)(fieldClass == Instant.class
                  ? new FieldReaderInstant<>(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field, null, null)
                  : new FieldReaderObjectField<>(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, field));
            }
         }
      }
   }

   public <T, V> FieldReader createFieldReader(String fieldName, Type fieldType, Class<V> fieldClass, long features, BiConsumer<T, V> function) {
      return this.createFieldReader(null, null, fieldName, fieldType, fieldClass, 0, features, null, null, null, null, null, function, null);
   }

   public <T, V> FieldReader createFieldReader(String fieldName, Type fieldType, Class<V> fieldClass, Method method, BiConsumer<T, V> function) {
      return this.createFieldReader(null, null, fieldName, fieldType, fieldClass, 0, 0L, null, null, null, null, method, function, null);
   }

   public <T, V> FieldReader createFieldReader(
      Class objectClass,
      Type objectType,
      String fieldName,
      Type fieldType,
      Class<V> fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      BiConsumer<T, V> function,
      ObjectReader initReader
   ) {
      if (initReader != null) {
         FieldReaderObject fieldReaderObjectMethod = new FieldReaderObject(
            fieldName, fieldType, fieldClass, ordinal, features | 2251799813685248L, format, locale, defaultValue, schema, method, null, function
         );
         fieldReaderObjectMethod.initReader = initReader;
         return fieldReaderObjectMethod;
      } else if (fieldClass == Integer.class) {
         return new FieldReaderInt32Func<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == Long.class) {
         return new FieldReaderInt64Func<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == String.class) {
         return new FieldReaderStringFunc<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == Boolean.class) {
         return new FieldReaderBoolFunc<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == Short.class) {
         return new FieldReaderInt16Func<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == Byte.class) {
         return new FieldReaderInt8Func<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == BigDecimal.class) {
         return new FieldReaderBigDecimalFunc<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == BigInteger.class) {
         return new FieldReaderBigIntegerFunc<>(fieldName, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, function);
      } else if (fieldClass == Float.class) {
         return new FieldReaderFloatFunc<>(fieldName, fieldClass, ordinal, features, format, locale, (Float)defaultValue, schema, method, function);
      } else if (fieldClass == Double.class) {
         return new FieldReaderDoubleFunc<>(fieldName, fieldClass, ordinal, features, format, locale, (Double)defaultValue, schema, method, function);
      } else if (fieldClass == Number.class) {
         return new FieldReaderNumberFunc<>(fieldName, fieldClass, ordinal, features, format, locale, (Number)defaultValue, schema, method, function);
      } else if (fieldClass == Date.class) {
         return new FieldReaderDate<>(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, null, method, function);
      } else {
         Type fieldTypeResolved = null;
         Class fieldClassResolved = null;
         if (!(fieldType instanceof Class)) {
            TypeReference<?> objectTypeReference;
            if (objectType == null) {
               objectTypeReference = null;
            } else {
               objectTypeReference = TypeReference.get(objectType);
            }

            fieldTypeResolved = BeanUtils.getFieldType(objectTypeReference, objectClass, method, fieldType);
            fieldClassResolved = TypeUtils.getMapping(fieldTypeResolved);
         }

         if (fieldClass != List.class && fieldClass != ArrayList.class) {
            return new FieldReaderObjectFunc<>(
               fieldName,
               fieldTypeResolved == null ? fieldType : fieldTypeResolved,
               fieldClass,
               ordinal,
               features,
               format,
               locale,
               defaultValue,
               schema,
               method,
               function,
               null
            );
         } else {
            Type itemType = Object.class;
            Class itemClass = Object.class;
            if (fieldTypeResolved instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)fieldTypeResolved;
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  itemType = actualTypeArguments[0];
                  itemClass = TypeUtils.getMapping(itemType);
                  if (itemClass == String.class) {
                     return new FieldReaderList(
                        fieldName,
                        fieldTypeResolved,
                        fieldClassResolved,
                        String.class,
                        String.class,
                        ordinal,
                        features,
                        format,
                        locale,
                        defaultValue,
                        schema,
                        method,
                        null,
                        function
                     );
                  }
               }
            }

            boolean nullResolvedType = fieldTypeResolved == null;
            return new FieldReaderList(
               fieldName,
               nullResolvedType ? fieldType : fieldTypeResolved,
               nullResolvedType ? fieldClass : fieldClassResolved,
               itemType,
               itemClass,
               ordinal,
               features,
               format,
               locale,
               defaultValue,
               schema,
               method,
               null,
               function
            );
         }
      }
   }

   protected ObjectReader createEnumReader(Class objectClass, Method createMethod, ObjectReaderProvider provider) {
      FieldInfo fieldInfo = new FieldInfo();
      Enum[] ordinalEnums = (Enum[])objectClass.getEnumConstants();
      Map<Long, Enum> enumMap = new LinkedHashMap<>();

      for (int i = 0; ordinalEnums != null && i < ordinalEnums.length; i++) {
         Enum e = ordinalEnums[i];
         String name = e.name();
         long hash = Fnv.hashCode64(name);
         enumMap.put(hash, e);

         try {
            fieldInfo.init();
            Field field = objectClass.getField(name);
            provider.getFieldInfo(fieldInfo, objectClass, field);
            String jsonFieldName = fieldInfo.fieldName;
            if (jsonFieldName != null && !jsonFieldName.isEmpty() && !jsonFieldName.equals(name)) {
               long jsonFieldNameHash = Fnv.hashCode64(jsonFieldName);
               enumMap.putIfAbsent(jsonFieldNameHash, e);
            }

            if (fieldInfo.alternateNames != null) {
               for (String alternateName : fieldInfo.alternateNames) {
                  if (alternateName != null && !alternateName.isEmpty()) {
                     long alternateNameHash = Fnv.hashCode64(alternateName);
                     enumMap.putIfAbsent(alternateNameHash, e);
                  }
               }
            }
         } catch (Exception var22) {
         }
      }

      for (int i = 0; ordinalEnums != null && i < ordinalEnums.length; i++) {
         Enum e = ordinalEnums[i];
         String name = e.name();
         long hashLCase = Fnv.hashCode64LCase(name);
         enumMap.putIfAbsent(hashLCase, e);
      }

      long[] enumNameHashCodes = new long[enumMap.size()];
      int i = 0;

      for (Long h : enumMap.keySet()) {
         enumNameHashCodes[i++] = h;
      }

      Arrays.sort(enumNameHashCodes);
      Member enumValueField = BeanUtils.getEnumValueField(objectClass, provider);
      if (enumValueField == null && provider.modules.size() > 0) {
         Class fieldClassMixInSource = provider.getMixIn(objectClass);
         if (fieldClassMixInSource != null) {
            Member mixedValueField = BeanUtils.getEnumValueField(fieldClassMixInSource, provider);
            if (mixedValueField instanceof Field) {
               try {
                  enumValueField = objectClass.getField(mixedValueField.getName());
               } catch (NoSuchFieldException var21) {
               }
            } else if (mixedValueField instanceof Method) {
               try {
                  enumValueField = objectClass.getMethod(mixedValueField.getName());
               } catch (NoSuchMethodException var20) {
               }
            }
         }
      }

      Enum[] enums = new Enum[enumNameHashCodes.length];

      for (int ix = 0; ix < enumNameHashCodes.length; ix++) {
         long hash = enumNameHashCodes[ix];
         Enum e = enumMap.get(hash);
         enums[ix] = e;
      }

      return new ObjectReaderImplEnum(objectClass, createMethod, enumValueField, enums, ordinalEnums, enumNameHashCodes);
   }

   static ObjectReader getInitReader(ObjectReaderProvider provider, Type fieldType, Class fieldClass, FieldInfo fieldInfo) {
      ObjectReader initReader = fieldInfo.getInitReader();
      if (initReader == null && (fieldInfo.keyUsing != null || fieldInfo.valueUsing != null) && Map.class.isAssignableFrom(fieldClass)) {
         ObjectReader keyReader = null;
         if (fieldInfo.keyUsing != null) {
            try {
               Constructor<?> constructor = fieldInfo.keyUsing.getDeclaredConstructor();
               constructor.setAccessible(true);
               keyReader = (ObjectReader)constructor.newInstance();
            } catch (Exception var10) {
            }
         }

         ObjectReader valueReader = null;
         if (fieldInfo.valueUsing != null) {
            try {
               Constructor<?> constructor = fieldInfo.valueUsing.getDeclaredConstructor();
               constructor.setAccessible(true);
               valueReader = (ObjectReader)constructor.newInstance();
            } catch (Exception var9) {
            }
         }

         if (keyReader != null || valueReader != null) {
            ObjectReader reader = ObjectReaderImplMap.of(fieldType, fieldClass, fieldInfo.features);
            if (reader instanceof ObjectReaderImplMapTyped) {
               ObjectReaderImplMapTyped mapReader = (ObjectReaderImplMapTyped)reader;
               if (keyReader != null) {
                  mapReader.keyObjectReader = keyReader;
               }

               if (valueReader != null) {
                  mapReader.valueObjectReader = valueReader;
               }

               return mapReader;
            }
         }
      }

      if (initReader == null) {
         if (fieldClass == long.class || fieldClass == Long.class) {
            ObjectReader objectReader = provider.getObjectReader(Long.class);
            if (objectReader != ObjectReaderImplInt64.INSTANCE) {
               initReader = objectReader;
            }
         } else if (fieldClass == BigDecimal.class) {
            ObjectReader objectReader = provider.getObjectReader(BigDecimal.class);
            if (objectReader != ObjectReaderImplBigDecimal.INSTANCE) {
               initReader = objectReader;
            }
         } else if (fieldClass == BigInteger.class) {
            ObjectReader objectReader = provider.getObjectReader(BigInteger.class);
            if (objectReader != ObjectReaderImplBigInteger.INSTANCE) {
               initReader = objectReader;
            }
         } else if (fieldClass == Date.class) {
            ObjectReader objectReader = provider.getObjectReader(Date.class);
            if (objectReader != ObjectReaderImplDate.INSTANCE) {
               initReader = objectReader;
            }
         }
      }

      return initReader;
   }

   protected <T> FieldReader createFieldReaderLambda(
      Class<T> objectClass,
      Type objectType,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      String schema,
      Type fieldType,
      Class fieldClass,
      Method method,
      ObjectReader initReader
   ) {
      if (defaultValue != null && defaultValue.getClass() != fieldClass) {
         Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(defaultValue.getClass(), fieldType);
         if (typeConvert == null) {
            throw new JSONException("illegal defaultValue : " + defaultValue + ", class " + fieldClass.getName());
         }

         defaultValue = typeConvert.apply(defaultValue);
      }

      JSONSchema jsonSchema = null;
      if (schema != null && !schema.isEmpty()) {
         JSONObject object = JSON.parseObject(schema);
         if (!object.isEmpty()) {
            jsonSchema = JSONSchema.of(object, fieldClass);
         }
      }

      if (initReader != null) {
         BiConsumer function = (BiConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return this.createFieldReader(
            objectClass,
            objectType,
            fieldName,
            fieldType,
            fieldClass,
            ordinal,
            features,
            format,
            locale,
            defaultValue,
            jsonSchema,
            method,
            function,
            initReader
         );
      } else if (fieldType == boolean.class) {
         ObjBoolConsumer function = (ObjBoolConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderBoolValFunc(fieldName, ordinal, jsonSchema, method, function);
      } else if (fieldType == byte.class) {
         ObjByteConsumer function = (ObjByteConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderInt8ValueFunc(fieldName, ordinal, jsonSchema, method, function);
      } else if (fieldType == short.class) {
         ObjShortConsumer function = (ObjShortConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderInt16ValueFunc(fieldName, ordinal, features, format, locale, (Short)defaultValue, jsonSchema, method, function);
      } else if (fieldType == int.class) {
         ObjIntConsumer function = (ObjIntConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderInt32ValueFunc(fieldName, ordinal, (Integer)defaultValue, jsonSchema, method, function);
      } else if (fieldType == long.class) {
         ObjLongConsumer function = (ObjLongConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderInt64ValueFunc(fieldName, ordinal, (Long)defaultValue, jsonSchema, method, function);
      } else if (fieldType == char.class) {
         ObjCharConsumer function = (ObjCharConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderCharValueFunc(fieldName, ordinal, format, (Character)defaultValue, jsonSchema, method, function);
      } else if (fieldType == float.class) {
         ObjFloatConsumer function = (ObjFloatConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderFloatValueFunc(fieldName, ordinal, (Float)defaultValue, jsonSchema, method, function);
      } else if (fieldType == double.class) {
         ObjDoubleConsumer function = (ObjDoubleConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return new FieldReaderDoubleValueFunc(fieldName, ordinal, (Double)defaultValue, jsonSchema, method, function);
      } else {
         BiConsumer consumer = (BiConsumer)this.lambdaSetter(objectClass, fieldClass, method);
         return this.createFieldReader(
            objectClass, objectType, fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, jsonSchema, method, consumer, null
         );
      }
   }

   protected Object lambdaSetter(Class objectClass, Class fieldClass, Method method) {
      Lookup lookup = JDKUtils.trustedLookup(objectClass);
      Class<?> returnType = method.getReturnType();
      ObjectReaderCreator.LambdaSetterInfo lambdaInfo = methodTypeMapping.get(fieldClass);
      MethodType methodType = null;
      MethodType samMethodType;
      MethodType invokedType;
      if (lambdaInfo != null) {
         samMethodType = lambdaInfo.sameMethodMethod;
         invokedType = lambdaInfo.invokedType;
         if (returnType == void.class) {
            methodType = lambdaInfo.methodType;
         }
      } else {
         samMethodType = TypeUtils.METHOD_TYPE_VOO;
         invokedType = TypeUtils.METHOD_TYPE_BI_CONSUMER;
      }

      if (methodType == null) {
         methodType = MethodType.methodType(returnType, fieldClass);
      }

      try {
         MethodHandle target = lookup.findVirtual(objectClass, method.getName(), methodType);
         MethodType instantiatedMethodType = MethodType.methodType(void.class, objectClass, fieldClass);
         CallSite callSite = LambdaMetafactory.metafactory(lookup, "accept", invokedType, samMethodType, target, instantiatedMethodType);
         return (Object)callSite.getTarget().invoke();
      } catch (Throwable var13) {
         throw new JSONException("create fieldReader error", var13);
      }
   }

   public Function<Consumer, ByteArrayValueConsumer> createByteArrayValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return null;
   }

   public Function<Consumer, CharArrayValueConsumer> createCharArrayValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return null;
   }

   private List<FieldReader> listOf(FieldReader fieldReader) {
      List<FieldReader> list = new ArrayList<>();
      list.add(fieldReader);
      return list;
   }

   private void putIfAbsent(Map<String, List<FieldReader>> fieldReaders, String fieldName, FieldReader fieldReader, Class objectClass) {
      List<FieldReader> origin = fieldReaders.get(fieldName);
      if (origin == null) {
         fieldReaders.put(fieldName, this.listOf(fieldReader));
      } else {
         if (!fieldReader.isReadOnly()) {
            FieldReader sameReader = null;

            for (int i = 0; i < origin.size(); i++) {
               FieldReader tempReader = origin.get(i);
               if (tempReader.sameTo(fieldReader)) {
                  sameReader = tempReader;
                  break;
               }
            }

            if (sameReader != null) {
               if (sameReader.compareTo(fieldReader) > 0 || !sameReader.belongTo(objectClass)) {
                  origin.set(origin.indexOf(sameReader), fieldReader);
               }
            } else {
               origin.add(fieldReader);
            }
         }
      }
   }

   private FieldReader[] toFieldReaderArray(Map<String, List<FieldReader>> fieldReaders) {
      int size = fieldReaders.values().stream().mapToInt(Collection::size).sum();
      FieldReader[] fieldReaderArray = new FieldReader[size];
      fieldReaders.values().stream().flatMap(Collection::stream).collect(Collectors.toList()).toArray(fieldReaderArray);
      Arrays.sort((Object[])fieldReaderArray);
      return fieldReaderArray;
   }

   static {
      methodTypeMapping.put(boolean.class, new ObjectReaderCreator.LambdaSetterInfo(boolean.class, ObjBoolConsumer.class));
      methodTypeMapping.put(byte.class, new ObjectReaderCreator.LambdaSetterInfo(byte.class, ObjByteConsumer.class));
      methodTypeMapping.put(short.class, new ObjectReaderCreator.LambdaSetterInfo(short.class, ObjShortConsumer.class));
      methodTypeMapping.put(int.class, new ObjectReaderCreator.LambdaSetterInfo(int.class, ObjIntConsumer.class));
      methodTypeMapping.put(long.class, new ObjectReaderCreator.LambdaSetterInfo(long.class, ObjLongConsumer.class));
      methodTypeMapping.put(char.class, new ObjectReaderCreator.LambdaSetterInfo(char.class, ObjCharConsumer.class));
      methodTypeMapping.put(float.class, new ObjectReaderCreator.LambdaSetterInfo(float.class, ObjFloatConsumer.class));
      methodTypeMapping.put(double.class, new ObjectReaderCreator.LambdaSetterInfo(double.class, ObjDoubleConsumer.class));
   }

   static class LambdaSetterInfo {
      final Class fieldClass;
      final MethodType sameMethodMethod;
      final MethodType methodType;
      final MethodType invokedType;

      LambdaSetterInfo(Class fieldClass, Class functionClass) {
         this.fieldClass = fieldClass;
         this.sameMethodMethod = MethodType.methodType(void.class, Object.class, fieldClass);
         this.methodType = MethodType.methodType(void.class, fieldClass);
         this.invokedType = MethodType.methodType(functionClass);
      }
   }
}
