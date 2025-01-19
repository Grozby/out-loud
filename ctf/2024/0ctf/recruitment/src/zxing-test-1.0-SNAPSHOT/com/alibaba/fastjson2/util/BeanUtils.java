package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.modules.ObjectCodecProvider;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class BeanUtils {
   static final Type[] EMPTY_TYPE_ARRAY = new Type[0];
   static final ConcurrentMap<Class, Field[]> fieldCache = new ConcurrentHashMap<>();
   static final ConcurrentMap<Class, Map<String, Field>> fieldMapCache = new ConcurrentHashMap<>();
   static final ConcurrentMap<Class, Field[]> declaredFieldCache = new ConcurrentHashMap<>();
   static final ConcurrentMap<Class, Method[]> methodCache = new ConcurrentHashMap<>();
   static final ConcurrentMap<Class, Constructor[]> constructorCache = new ConcurrentHashMap<>();
   private static volatile Class RECORD_CLASS;
   private static volatile Method RECORD_GET_RECORD_COMPONENTS;
   private static volatile Method RECORD_COMPONENT_GET_NAME;
   public static final String SUPER = "$super$";
   static final long[] IGNORE_CLASS_HASH_CODES = new long[]{
      -9214723784238596577L,
      -9030616758866828325L,
      -8335274122997354104L,
      -6963030519018899258L,
      -4863137578837233966L,
      -3653547262287832698L,
      -2819277587813726773L,
      -2669552864532011468L,
      -2458634727370886912L,
      -2291619803571459675L,
      -1811306045128064037L,
      -864440709753525476L,
      -779604756358333743L,
      8731803887940231L,
      1616814008855344660L,
      2164749833121980361L,
      2688642392827789427L,
      3724195282986200606L,
      3742915795806478647L,
      3977020351318456359L,
      4882459834864833642L,
      6033839080488254886L,
      7981148566008458638L,
      8344106065386396833L
   };

   public static String[] getRecordFieldNames(Class<?> recordType) {
      if (JDKUtils.JVM_VERSION < 14 && JDKUtils.ANDROID_SDK_INT < 33) {
         return new String[0];
      } else {
         try {
            if (RECORD_GET_RECORD_COMPONENTS == null) {
               RECORD_GET_RECORD_COMPONENTS = Class.class.getMethod("getRecordComponents");
            }

            if (RECORD_COMPONENT_GET_NAME == null) {
               Class<?> c = Class.forName("java.lang.reflect.RecordComponent");
               RECORD_COMPONENT_GET_NAME = c.getMethod("getName");
            }

            Object[] components = (Object[])RECORD_GET_RECORD_COMPONENTS.invoke(recordType);
            String[] names = new String[components.length];

            for (int i = 0; i < components.length; i++) {
               names[i] = (String)RECORD_COMPONENT_GET_NAME.invoke(components[i]);
            }

            return names;
         } catch (Exception var4) {
            throw new RuntimeException(
               String.format("Failed to access Methods needed to support `java.lang.Record`: (%s) %s", var4.getClass().getName(), var4.getMessage()), var4
            );
         }
      }
   }

   public static void fields(Class objectClass, Consumer<Field> fieldReaders) {
      if (TypeUtils.isProxy(objectClass)) {
         Class superclass = objectClass.getSuperclass();
         fields(superclass, fieldReaders);
      } else {
         Field[] fields = fieldCache.get(objectClass);
         if (fields == null) {
            fields = objectClass.getFields();
            fieldCache.putIfAbsent(objectClass, fields);
         }

         boolean enumClass = Enum.class.isAssignableFrom(objectClass);

         for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || enumClass) {
               fieldReaders.accept(field);
            }
         }
      }
   }

   public static Method getMethod(Class objectClass, String methodName) {
      Method[] methods = methodCache.get(objectClass);
      if (methods == null) {
         methods = getMethods(objectClass);
         methodCache.putIfAbsent(objectClass, methods);
      }

      for (Method method : methods) {
         if (method.getName().equals(methodName)) {
            return method;
         }
      }

      return null;
   }

   public static Method fluentSetter(Class objectClass, String methodName, Class paramType) {
      Method[] methods = methodCache.get(objectClass);
      if (methods == null) {
         methods = getMethods(objectClass);
         methodCache.putIfAbsent(objectClass, methods);
      }

      for (Method method : methods) {
         if (method.getName().equals(methodName)
            && method.getReturnType() == objectClass
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == paramType) {
            return method;
         }
      }

      return null;
   }

   public static Method getMethod(Class objectClass, Method signature) {
      if (objectClass != null && objectClass != Object.class && objectClass != Serializable.class) {
         Method[] methods = methodCache.get(objectClass);
         if (methods == null) {
            methods = getMethods(objectClass);
            methodCache.putIfAbsent(objectClass, methods);
         }

         for (Method method : methods) {
            if (method.getName().equals(signature.getName()) && method.getParameterCount() == signature.getParameterCount()) {
               Class<?>[] parameterTypes0 = method.getParameterTypes();
               Class<?>[] parameterTypes1 = signature.getParameterTypes();
               boolean paramMatch = true;

               for (int i = 0; i < parameterTypes0.length; i++) {
                  if (!parameterTypes0[i].equals(parameterTypes1[i])) {
                     paramMatch = false;
                     break;
                  }
               }

               if (paramMatch) {
                  return method;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static Field getDeclaredField(Class objectClass, String fieldName) {
      Map<String, Field> fieldMap = fieldMapCache.get(objectClass);
      if (fieldMap == null) {
         Map<String, Field> map = new HashMap<>();
         declaredFields(objectClass, field -> map.put(field.getName(), field));
         fieldMapCache.putIfAbsent(objectClass, map);
         fieldMap = fieldMapCache.get(objectClass);
      }

      return fieldMap.get(fieldName);
   }

   public static Method getSetter(Class objectClass, String methodName) {
      Method[] methods = new Method[1];
      setters(objectClass, e -> {
         if (methodName.equals(e.getName())) {
            methods[0] = e;
         }
      });
      return methods[0];
   }

   public static void declaredFields(Class objectClass, Consumer<Field> fieldConsumer) {
      if (objectClass != null && fieldConsumer != null) {
         if (!ignore(objectClass) && !objectClass.getName().contains("$$Lambda") && !JdbcSupport.isStruct(objectClass)) {
            if (TypeUtils.isProxy(objectClass)) {
               Class superclass = objectClass.getSuperclass();
               declaredFields(superclass, fieldConsumer);
            } else {
               Class superClass = objectClass.getSuperclass();
               boolean protobufMessageV3 = false;
               if (superClass != null && superClass != Object.class) {
                  protobufMessageV3 = "com.google.protobuf.GeneratedMessageV3".equals(superClass.getName());
                  if (!protobufMessageV3) {
                     declaredFields(superClass, fieldConsumer);
                  }
               }

               Field[] fields = declaredFieldCache.get(objectClass);
               if (fields == null) {
                  Field[] declaredFields;
                  try {
                     declaredFields = objectClass.getDeclaredFields();
                     declaredFieldCache.put(objectClass, declaredFields);
                  } catch (Throwable var13) {
                     declaredFields = new Field[0];
                  }

                  boolean allMatch = true;

                  for (Field field : declaredFields) {
                     int modifiers = field.getModifiers();
                     if (Modifier.isStatic(modifiers)) {
                        allMatch = false;
                        break;
                     }
                  }

                  if (allMatch) {
                     fields = declaredFields;
                  } else {
                     boolean isEnum = Enum.class.isAssignableFrom(objectClass);
                     List<Field> list = new ArrayList<>(declaredFields.length);

                     for (Field fieldx : declaredFields) {
                        if (isEnum || !Modifier.isStatic(fieldx.getModifiers())) {
                           list.add(fieldx);
                        }
                     }

                     fields = list.toArray(new Field[list.size()]);
                  }

                  fieldCache.putIfAbsent(objectClass, fields);
               }

               for (Field fieldxx : fields) {
                  int modifiers = fieldxx.getModifiers();
                  Class<?> fieldClass = fieldxx.getType();
                  if ((modifiers & 8) == 0 && !ignore(fieldClass)) {
                     if (protobufMessageV3) {
                        String fieldName = fieldxx.getName();
                        if ("cardsmap_".equals(fieldName) && "com.google.protobuf.MapField".equals(fieldClass.getName())) {
                           return;
                        }
                     }

                     Class<?> declaringClass = fieldxx.getDeclaringClass();
                     if (declaringClass != AbstractMap.class
                        && declaringClass != HashMap.class
                        && declaringClass != LinkedHashMap.class
                        && declaringClass != TreeMap.class
                        && declaringClass != ConcurrentHashMap.class) {
                        fieldConsumer.accept(fieldxx);
                     }
                  }
               }
            }
         }
      }
   }

   public static void staticMethod(Class objectClass, Consumer<Method> methodConsumer) {
      Method[] methods = methodCache.get(objectClass);
      if (methods == null) {
         methods = getMethods(objectClass);
         methodCache.putIfAbsent(objectClass, methods);
      }

      for (Method method : methods) {
         int modifiers = method.getModifiers();
         if (Modifier.isStatic(modifiers)) {
            methodConsumer.accept(method);
         }
      }
   }

   public static Method buildMethod(Class objectClass, String methodName) {
      Method[] methods = methodCache.get(objectClass);
      if (methods == null) {
         methods = getMethods(objectClass);
         methodCache.putIfAbsent(objectClass, methods);
      }

      for (Method method : methods) {
         int modifiers = method.getModifiers();
         if (!Modifier.isStatic(modifiers) && method.getParameterCount() == 0 && method.getName().equals(methodName)) {
            return method;
         }
      }

      return null;
   }

   public static void constructor(Class objectClass, Consumer<Constructor> constructorConsumer) {
      Constructor[] constructors = constructorCache.get(objectClass);
      if (constructors == null) {
         constructors = objectClass.getDeclaredConstructors();
         constructorCache.putIfAbsent(objectClass, constructors);
      }

      boolean record = isRecord(objectClass);

      for (Constructor constructor : constructors) {
         if (!record || constructor.getParameterCount() != 0) {
            constructorConsumer.accept(constructor);
         }
      }
   }

   public static Constructor[] getConstructor(Class objectClass) {
      Constructor[] constructors = constructorCache.get(objectClass);
      if (constructors == null) {
         constructors = objectClass.getDeclaredConstructors();
         constructorCache.putIfAbsent(objectClass, constructors);
      }

      return constructors;
   }

   public static boolean hasPublicDefaultConstructor(Class objectClass) {
      Constructor constructor = getDefaultConstructor(objectClass, false);
      return constructor != null && Modifier.isPublic(constructor.getModifiers());
   }

   public static Constructor getDefaultConstructor(Class objectClass, boolean includeNoneStaticMember) {
      if ((objectClass != StackTraceElement.class || JDKUtils.JVM_VERSION < 9) && !isRecord(objectClass)) {
         Constructor[] constructors = constructorCache.get(objectClass);
         if (constructors == null) {
            constructors = objectClass.getDeclaredConstructors();
            constructorCache.putIfAbsent(objectClass, constructors);
         }

         for (Constructor constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
               return constructor;
            }
         }

         if (!includeNoneStaticMember) {
            return null;
         } else {
            Class declaringClass = objectClass.getDeclaringClass();
            if (declaringClass != null) {
               for (Constructor constructorx : constructors) {
                  if (constructorx.getParameterCount() == 1) {
                     Class firstParamType = constructorx.getParameterTypes()[0];
                     if (declaringClass.equals(firstParamType)) {
                        return constructorx;
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static void setters(Class objectClass, Consumer<Method> methodConsumer) {
      setters(objectClass, null, null, methodConsumer);
   }

   public static void setters(Class objectClass, Class mixin, Consumer<Method> methodConsumer) {
      setters(objectClass, null, mixin, methodConsumer);
   }

   public static void setters(Class objectClass, BeanInfo beanInfo, Class mixin, Consumer<Method> methodConsumer) {
      if (!ignore(objectClass)) {
         Method[] methods = methodCache.get(objectClass);
         if (methods == null) {
            methods = getMethods(objectClass);
            methodCache.putIfAbsent(objectClass, methods);
         }

         for (Method method : methods) {
            int mods = method.getModifiers();
            if (!Modifier.isStatic(mods) && method.getDeclaringClass() != Object.class) {
               String methodName = method.getName();
               boolean methodSkip = false;
               switch (methodName) {
                  case "equals":
                  case "hashCode":
                  case "toString":
                     methodSkip = true;
                     break;
                  case "copy":
                     if (beanInfo != null && beanInfo.kotlin) {
                        methodSkip = true;
                     }
               }

               if (!methodSkip) {
                  int paramCount = method.getParameterCount();
                  Class<?> returnType = method.getReturnType();
                  if (paramCount == 0) {
                     if (methodName.length() <= 3 || !methodName.startsWith("get")) {
                        continue;
                     }

                     if (returnType == AtomicInteger.class
                        || returnType == AtomicLong.class
                        || returnType == AtomicBoolean.class
                        || returnType == AtomicIntegerArray.class
                        || returnType == AtomicLongArray.class
                        || returnType == AtomicReference.class
                        || Collection.class.isAssignableFrom(returnType)
                        || Map.class.isAssignableFrom(returnType)) {
                        methodConsumer.accept(method);
                        continue;
                     }
                  }

                  if (paramCount == 2 && method.getReturnType() == void.class && method.getParameterTypes()[0] == String.class) {
                     Annotation[] annotations = getAnnotations(method);
                     AtomicBoolean unwrapped = new AtomicBoolean(false);

                     for (Annotation annotation : annotations) {
                        Class<? extends Annotation> annotationType = annotation.annotationType();
                        JSONField jsonField = findAnnotation(annotation, JSONField.class);
                        if (jsonField != null) {
                           if (jsonField.unwrapped()) {
                              unwrapped.set(true);
                              break;
                           }
                        } else {
                           String var39 = annotationType.getName();
                           switch (var39) {
                              case "com.fasterxml.jackson.annotation.JsonAnySetter":
                                 if (JSONFactory.isUseJacksonAnnotation()) {
                                    unwrapped.set(true);
                                 }
                                 break;
                              case "com.alibaba.fastjson.annotation.JSONField":
                                 annotationMethods(annotation.getClass(), m -> {
                                    String name = m.getName();

                                    try {
                                       if ("unwrapped".equals(name)) {
                                          Object result = m.invoke(annotation);
                                          if ((Boolean)result) {
                                             unwrapped.set(true);
                                          }
                                       }
                                    } catch (Throwable var5) {
                                    }
                                 });
                           }
                        }
                     }

                     if (unwrapped.get()) {
                        methodConsumer.accept(method);
                     }
                  } else if (paramCount == 1) {
                     int methodNameLength = methodName.length();
                     boolean nameMatch = methodNameLength > 3 && (methodName.startsWith("set") || returnType == objectClass);
                     if (!nameMatch && mixin != null) {
                        Method mixinMethod = getMethod(mixin, method);
                        if (mixinMethod != null) {
                           Annotation[] annotations = getAnnotations(mixinMethod);

                           for (Annotation annotationx : annotations) {
                              if (annotationx.annotationType() == JSONField.class) {
                                 JSONField jsonField = (JSONField)annotationx;
                                 if (!jsonField.unwrapped()) {
                                    nameMatch = true;
                                 }
                                 break;
                              }
                           }
                        }
                     }

                     if (!nameMatch) {
                        Annotation[] annotations = getAnnotations(method);

                        for (Annotation annotationxx : annotations) {
                           if (annotationxx.annotationType() == JSONField.class) {
                              JSONField jsonField = (JSONField)annotationxx;
                              if (!jsonField.unwrapped()) {
                                 nameMatch = true;
                              }
                              break;
                           }
                        }
                     }

                     if (nameMatch) {
                        methodConsumer.accept(method);
                     }
                  }
               }
            }
         }
      }
   }

   public static void setters(Class objectClass, boolean checkPrefix, Consumer<Method> methodConsumer) {
      if (!ignore(objectClass)) {
         Method[] methods = methodCache.get(objectClass);
         if (methods == null) {
            methods = getMethods(objectClass);
            methodCache.putIfAbsent(objectClass, methods);
         }

         for (Method method : methods) {
            int paramType = method.getParameterCount();
            if (paramType == 0) {
               String methodName = method.getName();
               if (checkPrefix && (methodName.length() <= 3 || !methodName.startsWith("get"))) {
                  continue;
               }

               Class<?> returnType = method.getReturnType();
               if (returnType == AtomicInteger.class
                  || returnType == AtomicLong.class
                  || returnType == AtomicBoolean.class
                  || returnType == AtomicIntegerArray.class
                  || returnType == AtomicLongArray.class
                  || Collection.class.isAssignableFrom(returnType)) {
                  methodConsumer.accept(method);
                  continue;
               }
            }

            if (paramType == 1) {
               int mods = method.getModifiers();
               if (!Modifier.isStatic(mods)) {
                  String methodNamex = method.getName();
                  int methodNameLength = methodNamex.length();
                  if (!checkPrefix || methodNameLength > 3 && methodNamex.startsWith("set")) {
                     methodConsumer.accept(method);
                  }
               }
            }
         }
      }
   }

   public static void annotationMethods(Class objectClass, Consumer<Method> methodConsumer) {
      Method[] methods = methodCache.get(objectClass);
      if (methods == null) {
         methods = getMethods(objectClass);
         methodCache.putIfAbsent(objectClass, methods);
      }

      for (Method method : methods) {
         if (method.getParameterCount() == 0) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass != Object.class) {
               String var8 = method.getName();
               switch (var8) {
                  case "toString":
                  case "hashCode":
                  case "annotationType":
                     break;
                  default:
                     methodConsumer.accept(method);
               }
            }
         }
      }
   }

   public static boolean isWriteEnumAsJavaBean(Class clazz) {
      Annotation[] annotations = getAnnotations(clazz);

      for (Annotation annotation : annotations) {
         JSONType jsonType = findAnnotation(annotation, JSONType.class);
         if (jsonType != null) {
            return jsonType.writeEnumAsJavaBean();
         }

         Class<? extends Annotation> annotationType = annotation.annotationType();
         String name = annotationType.getName();
         BeanInfo beanInfo = new BeanInfo(JSONFactory.getDefaultObjectWriterProvider());
         switch (name) {
            case "com.alibaba.fastjson.annotation.JSONType":
               annotationMethods(annotationType, method -> processJSONType1x(beanInfo, annotation, method));
               break;
            case "com.fasterxml.jackson.annotation.JsonFormat":
               boolean useJacksonAnnotation = JSONFactory.isUseJacksonAnnotation();
               if (useJacksonAnnotation) {
                  processJacksonJsonFormat(beanInfo, annotation);
               }
         }

         if (beanInfo.writeEnumAsJavaBean) {
            return true;
         }
      }

      return false;
   }

   public static String[] getEnumAnnotationNames(Class enumClass) {
      Enum[] enumConstants = (Enum[])enumClass.getEnumConstants();
      String[] annotationNames = new String[enumConstants.length];
      fields(
         enumClass,
         field -> {
            String fieldName = field.getName();

            for (int i = 0; i < enumConstants.length; i++) {
               Enum e = enumConstants[i];
               int enumIndex = i;
               String enumName = e.name();
               if (fieldName.equals(enumName)) {
                  for (Annotation annotation : field.getAnnotations()) {
                     Class annotationType = annotation.annotationType();
                     String annotationTypeName = annotationType.getName();
                     if ("com.alibaba.fastjson2.annotation.JSONField".equals(annotationTypeName)
                        || "com.alibaba.fastjson.annotation.JSONField".equals(annotationTypeName)) {
                        annotationMethods(annotationType, m -> {
                           String name = m.getName();

                           try {
                              Object result = m.invoke(annotation);
                              if ("name".equals(name)) {
                                 String annotationNamex = (String)result;
                                 if (annotationNamex.length() != 0 && !annotationNamex.equals(enumName)) {
                                    annotationNames[enumIndex] = annotationNamex;
                                 }
                              }
                           } catch (Exception var8) {
                           }
                        });
                     }
                  }
                  break;
               }
            }
         }
      );
      int nulls = 0;

      for (String annotationName : annotationNames) {
         if (annotationName == null) {
            nulls++;
         }
      }

      return nulls == annotationNames.length ? null : annotationNames;
   }

   public static Member getEnumValueField(Class enumClass, ObjectCodecProvider mixinProvider) {
      if (enumClass == null) {
         return null;
      } else {
         Class[] interfaces = enumClass.getInterfaces();
         Method[] methods = methodCache.get(enumClass);
         if (methods == null) {
            methods = enumClass.getMethods();
            methodCache.putIfAbsent(enumClass, methods);
         }

         Member valueMember = null;

         for (Method method : methods) {
            if (method.getReturnType() != Void.class && method.getParameterCount() == 0) {
               Class<?> declaringClass = method.getDeclaringClass();
               if (declaringClass != Enum.class && declaringClass != Object.class) {
                  String methodName = method.getName();
                  if (!"values".equals(methodName)) {
                     if (isJSONField(method)) {
                        return method;
                     }

                     if (methodName.startsWith("get")) {
                        String fieldName = getterName(methodName, null);
                        Field field = getDeclaredField(enumClass, fieldName);
                        if (field != null && isJSONField(field)) {
                           if (valueMember == null) {
                              valueMember = method;
                           } else {
                              if (!valueMember.getName().equals(method.getName())) {
                                 return null;
                              }

                              if (valueMember instanceof Method) {
                                 Method valueMethod = (Method)valueMember;
                                 if (valueMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                                    valueMember = method;
                                 }
                              }
                           }
                           continue;
                        }
                     }

                     AtomicReference<Member> memberRef = new AtomicReference<>();

                     for (Class enumInterface : interfaces) {
                        getters(enumInterface, ex -> {
                           if (ex.getName().equals(methodName) && isJSONField(ex)) {
                              memberRef.set(method);
                           }
                        });
                        Class mixIn;
                        if (mixinProvider != null) {
                           mixIn = mixinProvider.getMixIn(enumInterface);
                        } else {
                           mixIn = JSONFactory.getDefaultObjectWriterProvider().getMixIn(enumInterface);
                        }

                        if (mixIn != null) {
                           getters(mixIn, ex -> {
                              if (ex.getName().equals(methodName) && isJSONField(ex)) {
                                 memberRef.set(method);
                              }
                           });
                        }
                     }

                     Member refMember = memberRef.get();
                     if (refMember != null) {
                        if (valueMember == null) {
                           valueMember = refMember;
                        } else if (!valueMember.getName().equals(refMember.getName())) {
                           return null;
                        }
                     }
                  }
               }
            }
         }

         if (valueMember != null) {
            return valueMember;
         } else {
            Field[] fields = fieldCache.get(enumClass);
            if (fields == null) {
               fields = enumClass.getFields();
               fieldCache.putIfAbsent(enumClass, fields);
            }

            Member member = null;
            Enum[] enumConstants = (Enum[])enumClass.getEnumConstants();

            for (Field field : fields) {
               boolean found = false;
               if (enumConstants != null) {
                  String fieldName = field.getName();

                  for (Enum e : enumConstants) {
                     if (fieldName.equals(e.name())) {
                        found = true;
                        break;
                     }
                  }
               }

               if (isJSONField(field) && !found) {
                  member = field;
                  break;
               }
            }

            return member;
         }
      }
   }

   public static void getters(Class objectClass, Consumer<Method> methodConsumer) {
      getters(objectClass, null, methodConsumer);
   }

   public static void getters(Class objectClass, Class mixinSource, Consumer<Method> methodConsumer) {
      getters(objectClass, mixinSource, false, methodConsumer);
   }

   public static void getters(Class objectClass, Class mixinSource, boolean kotlin, Consumer<Method> methodConsumer) {
      if (objectClass != null) {
         if (Proxy.isProxyClass(objectClass)) {
            Class[] interfaces = objectClass.getInterfaces();
            if (interfaces.length == 1) {
               getters(interfaces[0], methodConsumer);
               return;
            }
         }

         if (!ignore(objectClass)) {
            Class superClass = objectClass.getSuperclass();
            if (TypeUtils.isProxy(objectClass)) {
               getters(superClass, methodConsumer);
            } else {
               boolean record = isRecord(objectClass);
               boolean jdbcStruct = JdbcSupport.isStruct(objectClass);
               String[] recordFieldNames = null;
               if (record) {
                  recordFieldNames = getRecordFieldNames(objectClass);
               }

               Method[] methods = methodCache.get(objectClass);
               if (methods == null) {
                  methods = getMethods(objectClass);
                  methodCache.putIfAbsent(objectClass, methods);
               }

               boolean protobufMessageV3 = superClass != null && "com.google.protobuf.GeneratedMessageV3".equals(superClass.getName());

               for (Method method : methods) {
                  int paramType = method.getParameterCount();
                  if (paramType == 0) {
                     int mods = method.getModifiers();
                     if (!Modifier.isStatic(mods)) {
                        Class<?> returnClass = method.getReturnType();
                        if (returnClass != Void.class && returnClass != void.class && !ignore(returnClass)) {
                           Class<?> declaringClass = method.getDeclaringClass();
                           if (declaringClass != Enum.class && declaringClass != Object.class) {
                              String methodName = method.getName();
                              if (!jdbcStruct || "getSQLTypeName".equals(methodName) || "getAttributes".equals(methodName)) {
                                 boolean methodSkip = false;
                                 switch (methodName) {
                                    case "isInitialized":
                                    case "getInitializationErrorString":
                                    case "getSerializedSize":
                                       if (protobufMessageV3) {
                                          methodSkip = true;
                                       }
                                       break;
                                    case "equals":
                                    case "hashCode":
                                    case "toString":
                                       methodSkip = true;
                                 }

                                 if (!methodSkip
                                    && (
                                       !protobufMessageV3
                                          || !methodName.endsWith("Type") && !methodName.endsWith("Bytes")
                                          || !"com.google.protobuf.ByteString".equals(returnClass.getName())
                                    )) {
                                    if (methodName.startsWith("isSet") && returnClass == boolean.class) {
                                       boolean setterFound = false;
                                       boolean unsetFound = false;
                                       boolean getterFound = false;
                                       String setterName = getterName(methodName, null);
                                       String getterName = "g" + setterName.substring(1);
                                       String unsetName = "un" + setterName;

                                       for (Method m : methods) {
                                          if (m.getName().equals(setterName) && m.getParameterCount() == 1 && m.getReturnType() == void.class) {
                                             setterFound = true;
                                          } else if (m.getName().equals(getterName) && m.getParameterCount() == 0) {
                                             getterFound = true;
                                          } else if (m.getName().equals(unsetName) && m.getParameterCount() == 0 && m.getReturnType() == void.class) {
                                             unsetFound = true;
                                          }
                                       }

                                       if (setterFound && unsetFound && getterFound && findAnnotation(method, JSONField.class) == null) {
                                          continue;
                                       }
                                    }

                                    if (record) {
                                       boolean match = false;

                                       for (String recordFieldName : recordFieldNames) {
                                          if (methodName.equals(recordFieldName)) {
                                             match = true;
                                             break;
                                          }
                                       }

                                       if (match) {
                                          methodConsumer.accept(method);
                                          continue;
                                       }
                                    }

                                    int methodNameLength = methodName.length();
                                    boolean nameMatch = methodNameLength > 3 && methodName.startsWith("get");
                                    if (nameMatch) {
                                       char firstChar = methodName.charAt(3);
                                       if (firstChar >= 'a' && firstChar <= 'z' && methodNameLength == 4) {
                                          nameMatch = false;
                                       }
                                    } else if (returnClass == boolean.class || returnClass == Boolean.class || kotlin) {
                                       nameMatch = methodNameLength > 2 && methodName.startsWith("is");
                                       if (nameMatch) {
                                          char firstChar = methodName.charAt(2);
                                          if (firstChar >= 'a' && firstChar <= 'z' && methodNameLength == 3) {
                                             nameMatch = false;
                                          }
                                       }
                                    }

                                    if (!nameMatch && isJSONField(method)) {
                                       nameMatch = true;
                                    }

                                    if (!nameMatch && mixinSource != null) {
                                       Method mixinMethod = getMethod(mixinSource, method);
                                       if (mixinMethod != null && isJSONField(mixinMethod)) {
                                          nameMatch = true;
                                       }
                                    }

                                    if (!nameMatch
                                       && objectClass != returnClass
                                       && !methodName.startsWith("build")
                                       && fluentSetter(objectClass, methodName, returnClass) != null) {
                                       nameMatch = true;
                                    }

                                    if (nameMatch) {
                                       if (protobufMessageV3) {
                                          if (method.getDeclaringClass() == superClass) {
                                             continue;
                                          }

                                          Class<?> returnType = method.getReturnType();
                                          boolean ignore = false;
                                          switch (methodName) {
                                             case "getUnknownFields":
                                             case "getSerializedSize":
                                             case "getParserForType":
                                             case "getMessageBytes":
                                             case "getDefaultInstanceForType":
                                                ignore = returnType.getName().startsWith("com.google.protobuf.") || returnType == objectClass;
                                          }

                                          if (ignore) {
                                             continue;
                                          }
                                       }

                                       methodConsumer.accept(method);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static Method[] getMethods(Class objectClass) {
      Method[] methods;
      try {
         methods = objectClass.getMethods();
      } catch (NoClassDefFoundError var3) {
         methods = new Method[0];
      }

      return methods;
   }

   private static boolean isJSONField(AnnotatedElement element) {
      Annotation[] annotations = element.getAnnotations();
      Annotation[] var2 = annotations;
      int var3 = annotations.length;
      int var4 = 0;

      while (var4 < var3) {
         Annotation annotation = var2[var4];
         String annotationTypeName = annotation.annotationType().getName();
         switch (annotationTypeName) {
            case "com.alibaba.fastjson.annotation.JSONField":
            case "com.alibaba.fastjson2.annotation.JSONField":
               return true;
            case "com.fasterxml.jackson.annotation.JsonValue":
            case "com.fasterxml.jackson.annotation.JsonRawValue":
            case "com.fasterxml.jackson.annotation.JsonProperty":
            case "com.fasterxml.jackson.annotation.JsonUnwrapped":
               if (JSONFactory.isUseJacksonAnnotation()) {
                  return true;
               }
            default:
               var4++;
         }
      }

      return false;
   }

   static boolean ignore(Class objectClass) {
      return objectClass == null ? true : Arrays.binarySearch(IGNORE_CLASS_HASH_CODES, Fnv.hashCode64(objectClass.getName())) >= 0;
   }

   public static boolean isRecord(Class objectClass) {
      Class superclass = objectClass.getSuperclass();
      if (superclass == null) {
         return false;
      } else if (RECORD_CLASS == null) {
         String superclassName = superclass.getName();
         if ("java.lang.Record".equals(superclassName)) {
            RECORD_CLASS = superclass;
            return true;
         } else {
            return false;
         }
      } else {
         return superclass == RECORD_CLASS;
      }
   }

   public static String setterName(String methodName, String namingStrategy) {
      if (namingStrategy == null) {
         namingStrategy = "CamelCase";
      }

      int methodNameLength = methodName.length();
      if (methodNameLength <= 3) {
         return methodName;
      } else {
         int prefixLength = methodName.startsWith("set") ? 3 : 0;
         switch (namingStrategy) {
            case "NeverUseThisValueExceptDefaultValue":
            case "CamelCase":
               char[] chars = new char[methodNameLength - prefixLength];
               methodName.getChars(prefixLength, methodNameLength, chars, 0);
               char c0 = chars[0];
               boolean c1UCase = chars.length > 1 && chars[1] >= 'A' && chars[1] <= 'Z';
               if (c0 >= 'A' && c0 <= 'Z' && !c1UCase) {
                  chars[0] = (char)(c0 + ' ');
               }

               return new String(chars);
            case "PascalCase":
               return pascal(methodName, methodNameLength, prefixLength);
            case "SnakeCase":
               return snakeCase(methodName, prefixLength);
            case "UpperCaseWithUnderScores":
               return underScores(methodName, prefixLength, true);
            case "UpperCase":
               char[] chars = new char[methodNameLength - prefixLength];
               methodName.getChars(prefixLength, methodNameLength, chars, 0);
               char c0 = chars[0];

               for (int i = 0; i < chars.length; i++) {
                  char ch = chars[i];
                  if (ch >= 'a' && c0 <= 'z') {
                     chars[i] = (char)(ch - ' ');
                  }
               }

               return new String(chars);
            case "CamelCase1x":
               char[] chars = new char[methodNameLength - prefixLength];
               methodName.getChars(prefixLength, methodNameLength, chars, 0);
               char c0 = chars[0];
               if (c0 >= 'A' && c0 <= 'Z') {
                  chars[0] = (char)(c0 + ' ');
               }

               return new String(chars);
            case "UpperCamelCaseWithSpaces":
               return upperCamelWith(methodName, prefixLength, ' ');
            case "UpperCamelCaseWithUnderScores":
               return upperCamelWith(methodName, prefixLength, '_');
            case "UpperCamelCaseWithDashes":
               return upperCamelWith(methodName, prefixLength, '-');
            case "UpperCamelCaseWithDots":
               return upperCamelWith(methodName, prefixLength, '.');
            case "KebabCase":
               StringBuilder buf = new StringBuilder();
               int firstIndex = prefixLength;

               for (int i = prefixLength; i < methodName.length(); i++) {
                  char ch = methodName.charAt(i);
                  if (ch >= 'A' && ch <= 'Z') {
                     ch = (char)(ch + ' ');
                     if (i > firstIndex) {
                        buf.append('-');
                     }
                  }

                  buf.append(ch);
               }

               return buf.toString();
            case "UpperCaseWithDashes":
               return dashes(methodName, prefixLength, true);
            case "UpperCaseWithDots":
               return dots(methodName, prefixLength, true);
            case "LowerCase":
               return methodName.substring(prefixLength).toLowerCase();
            case "LowerCaseWithUnderScores":
               return underScores(methodName, prefixLength, false);
            case "LowerCaseWithDashes":
               return dashes(methodName, prefixLength, false);
            case "LowerCaseWithDots":
               return dots(methodName, prefixLength, false);
            default:
               throw new JSONException("TODO : " + namingStrategy);
         }
      }
   }

   public static String setterName(String methodName, int prefixLength) {
      int methodNameLength = methodName.length();
      char[] chars = new char[methodNameLength - prefixLength];
      methodName.getChars(prefixLength, methodNameLength, chars, 0);
      char c0 = chars[0];
      boolean c1UCase = chars.length > 1 && chars[1] >= 'A' && chars[1] <= 'Z';
      if (c0 >= 'A' && c0 <= 'Z' && !c1UCase) {
         chars[0] = (char)(c0 + ' ');
      }

      return new String(chars);
   }

   public static String getterName(Method method, String namingStrategy) {
      return getterName(method, false, namingStrategy);
   }

   public static String getterName(Method method, boolean kotlin, String namingStrategy) {
      String methodName = method.getName();
      if (methodName.startsWith("is")) {
         Class<?> returnType = method.getReturnType();
         if (returnType != Boolean.class && returnType != boolean.class || kotlin) {
            return methodName;
         }
      }

      String fieldName = getterName(methodName, namingStrategy);
      if (fieldName.length() > 2 && fieldName.charAt(0) >= 'A' && fieldName.charAt(0) <= 'Z' && fieldName.charAt(1) >= 'A' && fieldName.charAt(1) <= 'Z') {
         char[] chars = fieldName.toCharArray();
         chars[0] = (char)(chars[0] + ' ');
         String fieldName1 = new String(chars);
         Field field = getDeclaredField(method.getDeclaringClass(), fieldName1);
         if (field != null && Modifier.isPublic(field.getModifiers())) {
            fieldName = field.getName();
         }
      }

      return fieldName;
   }

   public static Field getField(Class objectClass, Method method) {
      String methodName = method.getName();
      int len = methodName.length();
      Class<?> returnType = method.getReturnType();
      boolean is = false;
      boolean get = false;
      boolean set = false;
      if (len > 2) {
         char c0 = methodName.charAt(0);
         char c1 = methodName.charAt(1);
         char c2 = methodName.charAt(2);
         if (c0 == 'i' && c1 == 's') {
            is = returnType == Boolean.class || returnType == boolean.class;
         } else if (c0 == 'g' && c1 == 'e' && c2 == 't') {
            get = len > 3;
         } else if (c0 == 's' && c1 == 'e' && c2 == 't') {
            set = len > 3 && method.getParameterCount() == 1;
         }
      }

      Field[] fields = new Field[2];
      if (is || get || set) {
         Class type = !is && !get ? method.getParameterTypes()[0] : returnType;
         int prefix = is ? 2 : 3;
         char[] chars = new char[len - prefix];
         methodName.getChars(prefix, len, chars, 0);
         char c0 = chars[0];
         declaredFields(objectClass, fieldx -> {
            if (fieldx.getDeclaringClass() == method.getDeclaringClass()) {
               String fieldName = fieldx.getName();
               int fieldNameLength = fieldName.length();
               if (fieldNameLength == len - prefix && (fieldx.getType() == type || type.isAssignableFrom(fieldx.getType()))) {
                  if (c0 >= 'A' && c0 <= 'Z' && c0 + ' ' == fieldName.charAt(0) && fieldName.regionMatches(1, methodName, prefix + 1, fieldNameLength - 1)) {
                     fields[0] = fieldx;
                  } else if (fieldName.regionMatches(0, methodName, prefix, fieldNameLength)) {
                     fields[1] = fieldx;
                  }
               } else if (boolean.class == fieldx.getType() && methodName.equals(fieldName)) {
                  fields[0] = fieldx;
               }
            }
         });
      }

      Field field = fields[0] != null ? fields[0] : fields[1];
      if (Throwable.class.isAssignableFrom(objectClass)) {
         if (returnType != String.class || (field != null || !"getMessage".equals(methodName)) && (field != null || !"getLocalizedMessage".equals(methodName))) {
            if (returnType == Throwable[].class && "getSuppressed".equals(methodName)) {
               field = getDeclaredField(objectClass, "suppressedExceptions");
            }
         } else {
            field = getDeclaredField(objectClass, "detailMessage");
         }
      }

      return field;
   }

   public static String getterName(String methodName, String namingStrategy) {
      if (namingStrategy == null) {
         namingStrategy = "CamelCase";
      }

      int methodNameLength = methodName.length();
      boolean is = methodName.startsWith("is");
      boolean get = methodName.startsWith("get");
      int prefixLength;
      if (is) {
         prefixLength = 2;
      } else if (get) {
         prefixLength = 3;
      } else {
         prefixLength = 0;
      }

      if (methodNameLength == prefixLength) {
         return methodName;
      } else {
         switch (namingStrategy) {
            case "NeverUseThisValueExceptDefaultValue":
            case "CamelCase":
               char[] chars = new char[methodNameLength - prefixLength];
               methodName.getChars(prefixLength, methodNameLength, chars, 0);
               char c0 = chars[0];
               boolean c1UCase = chars.length > 1 && chars[1] >= 'A' && chars[1] <= 'Z';
               if (c0 >= 'A' && c0 <= 'Z' && !c1UCase) {
                  chars[0] = (char)(c0 + ' ');
               }

               return new String(chars);
            case "CamelCase1x":
               char[] chars = new char[methodNameLength - prefixLength];
               methodName.getChars(prefixLength, methodNameLength, chars, 0);
               char c0 = chars[0];
               if (c0 >= 'A' && c0 <= 'Z') {
                  chars[0] = (char)(c0 + ' ');
               }

               return new String(chars);
            case "PascalCase":
               return pascal(methodName, methodNameLength, prefixLength);
            case "SnakeCase":
               return snakeCase(methodName, prefixLength);
            case "UpperCaseWithUnderScores":
               return underScores(methodName, prefixLength, true);
            case "UpperCamelCaseWithSpaces":
               return upperCamelWith(methodName, prefixLength, ' ');
            case "UpperCase":
               return methodName.substring(prefixLength).toUpperCase();
            case "UpperCaseWithDashes":
               return dashes(methodName, prefixLength, true);
            case "UpperCaseWithDots":
               return dots(methodName, prefixLength, true);
            case "KebabCase":
               StringBuilder buf = new StringBuilder();
               int firstIndex;
               if (is) {
                  firstIndex = 2;
               } else if (get) {
                  firstIndex = 3;
               } else {
                  firstIndex = 0;
               }

               for (int i = firstIndex; i < methodName.length(); i++) {
                  char ch = methodName.charAt(i);
                  if (ch >= 'A' && ch <= 'Z') {
                     ch = (char)(ch + ' ');
                     if (i > firstIndex) {
                        buf.append('-');
                     }
                  }

                  buf.append(ch);
               }

               return buf.toString();
            case "UpperCamelCaseWithUnderScores":
               return upperCamelWith(methodName, prefixLength, '_');
            case "UpperCamelCaseWithDashes":
               return upperCamelWith(methodName, prefixLength, '-');
            case "UpperCamelCaseWithDots":
               return upperCamelWith(methodName, prefixLength, '.');
            case "LowerCase":
               return methodName.substring(prefixLength).toLowerCase();
            case "LowerCaseWithUnderScores":
               return underScores(methodName, prefixLength, false);
            case "LowerCaseWithDashes":
               return dashes(methodName, prefixLength, false);
            case "LowerCaseWithDots":
               return dots(methodName, prefixLength, false);
            default:
               throw new JSONException("TODO : " + namingStrategy);
         }
      }
   }

   private static String pascal(String methodName, int methodNameLength, int prefixLength) {
      char[] chars = new char[methodNameLength - prefixLength];
      methodName.getChars(prefixLength, methodNameLength, chars, 0);
      char c0 = chars[0];
      if (c0 >= 'a' && c0 <= 'z' && chars.length > 1) {
         chars[0] = (char)(c0 - ' ');
      } else if (c0 == '_' && chars.length > 2) {
         char c1 = chars[1];
         if (c1 >= 'a' && c1 <= 'z' && chars[2] >= 'a' && chars[2] <= 'z') {
            chars[1] = (char)(c1 - ' ');
         }
      }

      return new String(chars);
   }

   public static String fieldName(String methodName, String namingStrategy) {
      if (namingStrategy == null) {
         namingStrategy = "CamelCase";
      }

      if (methodName != null && !methodName.isEmpty()) {
         switch (namingStrategy) {
            case "NoChange":
            case "NeverUseThisValueExceptDefaultValue":
            case "CamelCase":
               char c0x = methodName.charAt(0);
               char c1x = methodName.length() > 1 ? methodName.charAt(1) : 0;
               if (c0x < 'A' || c0x > 'Z' || methodName.length() <= 1 || c1x >= 'A' && c1x <= 'Z') {
                  return methodName;
               }

               char[] chars = methodName.toCharArray();
               chars[0] = (char)(c0x + ' ');
               return new String(chars);
            case "CamelCase1x":
               char c0x = methodName.charAt(0);
               if (c0x >= 'A' && c0x <= 'Z' && methodName.length() > 1) {
                  char[] chars = methodName.toCharArray();
                  chars[0] = (char)(c0x + ' ');
                  return new String(chars);
               }

               return methodName;
            case "PascalCase":
               char c0 = methodName.charAt(0);
               char c1;
               if (c0 >= 'a' && c0 <= 'z' && methodName.length() > 1 && (c1 = methodName.charAt(1)) >= 'a' && c1 <= 'z') {
                  char[] chars = methodName.toCharArray();
                  chars[0] = (char)(c0 - ' ');
                  return new String(chars);
               } else {
                  if (c0 == '_' && methodName.length() > 1 && (c1 = methodName.charAt(1)) >= 'a' && c1 <= 'z') {
                     char[] chars = methodName.toCharArray();
                     chars[1] = (char)(c1 - ' ');
                     return new String(chars);
                  }

                  return methodName;
               }
            case "SnakeCase":
               return snakeCase(methodName, 0);
            case "UpperCaseWithUnderScores":
               return underScores(methodName, 0, true);
            case "LowerCaseWithUnderScores":
               return underScores(methodName, 0, false);
            case "UpperCaseWithDashes":
               return dashes(methodName, 0, true);
            case "LowerCaseWithDashes":
               return dashes(methodName, 0, false);
            case "UpperCaseWithDots":
               return dots(methodName, 0, true);
            case "LowerCaseWithDots":
               return dots(methodName, 0, false);
            case "UpperCase":
               return methodName.toUpperCase();
            case "LowerCase":
               return methodName.toLowerCase();
            case "UpperCamelCaseWithSpaces":
               return upperCamelWith(methodName, 0, ' ');
            case "UpperCamelCaseWithUnderScores":
               return upperCamelWith(methodName, 0, '_');
            case "UpperCamelCaseWithDashes":
               return upperCamelWith(methodName, 0, '-');
            case "UpperCamelCaseWithDots":
               return upperCamelWith(methodName, 0, '.');
            case "KebabCase":
               StringBuilder buf = new StringBuilder();

               for (int i = 0; i < methodName.length(); i++) {
                  char ch = methodName.charAt(i);
                  if (ch >= 'A' && ch <= 'Z') {
                     ch = (char)(ch + ' ');
                     if (i > 0) {
                        buf.append('-');
                     }
                  }

                  buf.append(ch);
               }

               return buf.toString();
            default:
               throw new JSONException("TODO : " + namingStrategy);
         }
      } else {
         return methodName;
      }
   }

   static String snakeCase(String methodName, int prefixLength) {
      int methodNameLength = methodName.length();
      char[] buf = TypeUtils.CHARS_UPDATER.getAndSet(TypeUtils.CACHE, null);
      if (buf == null) {
         buf = new char[128];
      }

      String var10;
      try {
         int off = 0;

         for (int i = prefixLength; i < methodNameLength; i++) {
            char ch = methodName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
               ch = (char)(ch + ' ');
               if (i > prefixLength) {
                  buf[off++] = '_';
               }
            }

            buf[off++] = ch;
         }

         var10 = new String(buf, 0, off);
      } finally {
         TypeUtils.CHARS_UPDATER.set(TypeUtils.CACHE, buf);
      }

      return var10;
   }

   static String upperCamelWith(String methodName, int prefixLength, char separator) {
      int methodNameLength = methodName.length();
      char[] buf = TypeUtils.CHARS_UPDATER.getAndSet(TypeUtils.CACHE, null);
      if (buf == null) {
         buf = new char[128];
      }

      String var12;
      try {
         int off = 0;

         for (int i = prefixLength; i < methodNameLength; i++) {
            char ch = methodName.charAt(i);
            if (i == prefixLength) {
               char c1;
               if (ch >= 'a' && ch <= 'z' && i + 1 < methodNameLength && (c1 = methodName.charAt(i + 1)) >= 'a' && c1 <= 'z') {
                  ch = (char)(ch - ' ');
               } else if (ch == '_' && i + 1 < methodNameLength && (c1 = methodName.charAt(i + 1)) >= 'a' && c1 <= 'z') {
                  buf[off++] = ch;
                  ch = (char)(c1 - ' ');
                  i++;
               }
            } else {
               char c1;
               if (ch >= 'A' && ch <= 'Z' && i + 1 < methodNameLength && ((c1 = methodName.charAt(i + 1)) < 'A' || c1 > 'Z')) {
                  if (i > prefixLength) {
                     buf[off++] = separator;
                  }
               } else if (ch >= 'A'
                  && ch <= 'Z'
                  && i > prefixLength
                  && i + 1 < methodNameLength
                  && (c1 = methodName.charAt(i + 1)) >= 'A'
                  && c1 <= 'Z'
                  && (c1 = methodName.charAt(i - 1)) >= 'a'
                  && c1 <= 'z') {
                  buf[off++] = separator;
               }
            }

            buf[off++] = ch;
         }

         var12 = new String(buf, 0, off);
      } finally {
         TypeUtils.CHARS_UPDATER.set(TypeUtils.CACHE, buf);
      }

      return var12;
   }

   static String underScores(String methodName, int prefixLength, boolean upper) {
      int methodNameLength = methodName.length();
      char[] buf = TypeUtils.CHARS_UPDATER.getAndSet(TypeUtils.CACHE, null);
      if (buf == null) {
         buf = new char[128];
      }

      String var11;
      try {
         int off = 0;

         for (int i = prefixLength; i < methodNameLength; i++) {
            char ch = methodName.charAt(i);
            if (upper) {
               if (ch >= 'A' && ch <= 'Z') {
                  if (i > prefixLength) {
                     buf[off++] = '_';
                  }
               } else if (ch >= 'a' && ch <= 'z') {
                  ch = (char)(ch - ' ');
               }
            } else if (ch >= 'A' && ch <= 'Z') {
               if (i > prefixLength) {
                  buf[off++] = '_';
               }

               ch = (char)(ch + ' ');
            }

            buf[off++] = ch;
         }

         var11 = new String(buf, 0, off);
      } finally {
         TypeUtils.CHARS_UPDATER.set(TypeUtils.CACHE, buf);
      }

      return var11;
   }

   static String dashes(String methodName, int prefixLength, boolean upper) {
      int methodNameLength = methodName.length();
      char[] buf = TypeUtils.CHARS_UPDATER.getAndSet(TypeUtils.CACHE, null);
      if (buf == null) {
         buf = new char[128];
      }

      String var11;
      try {
         int off = 0;

         for (int i = prefixLength; i < methodNameLength; i++) {
            char ch = methodName.charAt(i);
            if (upper) {
               if (ch >= 'A' && ch <= 'Z') {
                  if (i > prefixLength) {
                     buf[off++] = '-';
                  }
               } else if (ch >= 'a' && ch <= 'z') {
                  ch = (char)(ch - ' ');
               }
            } else if (ch >= 'A' && ch <= 'Z') {
               if (i > prefixLength) {
                  buf[off++] = '-';
               }

               ch = (char)(ch + ' ');
            }

            buf[off++] = ch;
         }

         var11 = new String(buf, 0, off);
      } finally {
         TypeUtils.CHARS_UPDATER.set(TypeUtils.CACHE, buf);
      }

      return var11;
   }

   static String dots(String methodName, int prefixLength, boolean upper) {
      int methodNameLength = methodName.length();
      char[] buf = TypeUtils.CHARS_UPDATER.getAndSet(TypeUtils.CACHE, null);
      if (buf == null) {
         buf = new char[128];
      }

      String var11;
      try {
         int off = 0;

         for (int i = prefixLength; i < methodNameLength; i++) {
            char ch = methodName.charAt(i);
            if (upper) {
               if (ch >= 'A' && ch <= 'Z') {
                  if (i > prefixLength) {
                     buf[off++] = '.';
                  }
               } else if (ch >= 'a' && ch <= 'z') {
                  ch = (char)(ch - ' ');
               }
            } else if (ch >= 'A' && ch <= 'Z') {
               if (i > prefixLength) {
                  buf[off++] = '.';
               }

               ch = (char)(ch + ' ');
            }

            buf[off++] = ch;
         }

         var11 = new String(buf, 0, off);
      } finally {
         TypeUtils.CHARS_UPDATER.set(TypeUtils.CACHE, buf);
      }

      return var11;
   }

   public static Type getFieldType(TypeReference typeReference, Class<?> raw, Member field, Type fieldType) {
      Class<?> declaringClass = field == null ? null : field.getDeclaringClass();

      while (raw != Object.class) {
         Type type = typeReference == null ? null : typeReference.getType();
         if (declaringClass == raw) {
            return resolve(type, declaringClass, fieldType);
         }

         Type superType = raw.getGenericSuperclass();
         if (superType == null) {
            break;
         }

         typeReference = TypeReference.get(resolve(type, raw, superType));
         raw = typeReference.getRawType();
      }

      return null;
   }

   public static Type getParamType(TypeReference type, Class<?> raw, Class declaringClass, Parameter field, Type fieldType) {
      while (raw != Object.class) {
         if (declaringClass == raw) {
            return resolve(type.getType(), declaringClass, fieldType);
         }

         type = TypeReference.get(resolve(type.getType(), raw, raw.getGenericSuperclass()));
         raw = type.getRawType();
      }

      return null;
   }

   public static ParameterizedType newParameterizedTypeWithOwner(Type ownerType, Type rawType, Type... typeArguments) {
      return new BeanUtils.ParameterizedTypeImpl(ownerType, rawType, typeArguments);
   }

   public static GenericArrayType arrayOf(Type componentType) {
      return new BeanUtils.GenericArrayTypeImpl(componentType);
   }

   public static WildcardType subtypeOf(Type bound) {
      return new BeanUtils.WildcardTypeImpl(bound instanceof WildcardType ? ((WildcardType)bound).getUpperBounds() : new Type[]{bound}, EMPTY_TYPE_ARRAY);
   }

   public static WildcardType supertypeOf(Type bound) {
      return new BeanUtils.WildcardTypeImpl(
         new Type[]{Object.class}, bound instanceof WildcardType ? ((WildcardType)bound).getLowerBounds() : new Type[]{bound}
      );
   }

   public static Type canonicalize(Type type) {
      if (type instanceof Class) {
         Class<?> c = (Class<?>)type;
         return (Type)(c.isArray() ? new BeanUtils.GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c);
      } else if (type instanceof ParameterizedType) {
         ParameterizedType p = (ParameterizedType)type;
         return new BeanUtils.ParameterizedTypeImpl(p.getOwnerType(), p.getRawType(), p.getActualTypeArguments());
      } else if (type instanceof GenericArrayType) {
         GenericArrayType g = (GenericArrayType)type;
         return new BeanUtils.GenericArrayTypeImpl(g.getGenericComponentType());
      } else if (type instanceof WildcardType) {
         WildcardType w = (WildcardType)type;
         return new BeanUtils.WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());
      } else {
         return type;
      }
   }

   public static Class<?> getRawType(Type type) {
      if (type instanceof Class) {
         return (Class<?>)type;
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type rawType = parameterizedType.getRawType();
         checkArgument(rawType instanceof Class);
         return (Class<?>)rawType;
      } else if (type instanceof GenericArrayType) {
         Type componentType = ((GenericArrayType)type).getGenericComponentType();
         return Array.newInstance(getRawType(componentType), 0).getClass();
      } else if (type instanceof TypeVariable) {
         return Object.class;
      } else if (type instanceof WildcardType) {
         return getRawType(((WildcardType)type).getUpperBounds()[0]);
      } else {
         String className = type == null ? "null" : type.getClass().getName();
         throw new IllegalArgumentException("Expected a Class, ParameterizedType, or GenericArrayType, but <" + type + "> is of type " + className);
      }
   }

   static boolean equal(Object a, Object b) {
      return Objects.equals(a, b);
   }

   public static boolean equals(Type a, Type b) {
      if (a == b) {
         return true;
      } else if (a instanceof Class) {
         return a.equals(b);
      } else if (a instanceof ParameterizedType) {
         if (!(b instanceof ParameterizedType)) {
            return false;
         } else {
            ParameterizedType pa = (ParameterizedType)a;
            ParameterizedType pb = (ParameterizedType)b;
            return equal(pa.getOwnerType(), pb.getOwnerType())
               && pa.getRawType().equals(pb.getRawType())
               && Arrays.equals((Object[])pa.getActualTypeArguments(), (Object[])pb.getActualTypeArguments());
         }
      } else if (a instanceof GenericArrayType) {
         if (!(b instanceof GenericArrayType)) {
            return false;
         } else {
            GenericArrayType ga = (GenericArrayType)a;
            GenericArrayType gb = (GenericArrayType)b;
            return equals(ga.getGenericComponentType(), gb.getGenericComponentType());
         }
      } else if (a instanceof WildcardType) {
         if (!(b instanceof WildcardType)) {
            return false;
         } else {
            WildcardType wa = (WildcardType)a;
            WildcardType wb = (WildcardType)b;
            return Arrays.equals((Object[])wa.getUpperBounds(), (Object[])wb.getUpperBounds())
               && Arrays.equals((Object[])wa.getLowerBounds(), (Object[])wb.getLowerBounds());
         }
      } else if (a instanceof TypeVariable) {
         if (!(b instanceof TypeVariable)) {
            return false;
         } else {
            TypeVariable<?> va = (TypeVariable<?>)a;
            TypeVariable<?> vb = (TypeVariable<?>)b;
            return va.getGenericDeclaration() == vb.getGenericDeclaration() && va.getName().equals(vb.getName());
         }
      } else {
         return false;
      }
   }

   static int hashCodeOrZero(Object o) {
      return o != null ? o.hashCode() : 0;
   }

   public static String typeToString(Type type) {
      return type instanceof Class ? ((Class)type).getName() : type.toString();
   }

   static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
      if (toResolve == rawType) {
         return context;
      } else {
         if (toResolve.isInterface()) {
            Class<?>[] interfaces = rawType.getInterfaces();
            int i = 0;

            for (int length = interfaces.length; i < length; i++) {
               if (interfaces[i] == toResolve) {
                  return rawType.getGenericInterfaces()[i];
               }

               if (toResolve.isAssignableFrom(interfaces[i])) {
                  return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
               }
            }
         }

         if (rawType != null && !rawType.isInterface()) {
            while (rawType != Object.class) {
               Class<?> rawSupertype = rawType.getSuperclass();
               if (rawSupertype == toResolve) {
                  return rawType.getGenericSuperclass();
               }

               if (toResolve.isAssignableFrom(rawSupertype)) {
                  return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
               }

               rawType = rawSupertype;
            }
         }

         return toResolve;
      }
   }

   public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
      return resolve(context, contextRawType, toResolve, new HashMap<>());
   }

   private static Type resolve(Type context, Class<?> contextRawType, Type toResolve, Map<TypeVariable<?>, Type> visitedTypeVariables) {
      TypeVariable<?> resolving = null;

      while (true) {
         if (toResolve instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>)toResolve;
            Type previouslyResolved = visitedTypeVariables.get(typeVariable);
            if (previouslyResolved != null) {
               return previouslyResolved == void.class ? toResolve : previouslyResolved;
            }

            visitedTypeVariables.put(typeVariable, void.class);
            if (resolving == null) {
               resolving = typeVariable;
            }

            toResolve = resolveTypeVariable(context, contextRawType, typeVariable);
            if (toResolve != typeVariable) {
               continue;
            }
         } else if (toResolve instanceof Class && ((Class)toResolve).isArray()) {
            Class<?> original = (Class<?>)toResolve;
            Type componentType = original.getComponentType();
            Type newComponentType = resolve(context, contextRawType, componentType, visitedTypeVariables);
            toResolve = (Type)(equal(componentType, newComponentType) ? original : arrayOf(newComponentType));
         } else if (toResolve instanceof GenericArrayType) {
            GenericArrayType original = (GenericArrayType)toResolve;
            Type componentType = original.getGenericComponentType();
            Type newComponentType = resolve(context, contextRawType, componentType, visitedTypeVariables);
            toResolve = equal(componentType, newComponentType) ? original : arrayOf(newComponentType);
         } else if (toResolve instanceof ParameterizedType) {
            ParameterizedType original = (ParameterizedType)toResolve;
            Type ownerType = original.getOwnerType();
            Type newOwnerType = resolve(context, contextRawType, ownerType, visitedTypeVariables);
            boolean changed = !equal(newOwnerType, ownerType);
            Type[] args = original.getActualTypeArguments();
            int i = 0;

            for (int length = args.length; i < length; i++) {
               Type arg = args[i];
               if (arg != String.class) {
                  Type resolvedTypeArgument = resolve(context, contextRawType, arg, visitedTypeVariables);
                  if (!equal(resolvedTypeArgument, arg)) {
                     if (!changed) {
                        args = (Type[])args.clone();
                        changed = true;
                     }

                     args[i] = resolvedTypeArgument;
                  }
               }
            }

            toResolve = changed ? newParameterizedTypeWithOwner(newOwnerType, original.getRawType(), args) : original;
         } else if (toResolve instanceof WildcardType) {
            WildcardType original = (WildcardType)toResolve;
            Type[] originalLowerBound = original.getLowerBounds();
            Type[] originalUpperBound = original.getUpperBounds();
            if (originalLowerBound.length == 1) {
               Type lowerBound = resolve(context, contextRawType, originalLowerBound[0], visitedTypeVariables);
               if (lowerBound != originalLowerBound[0]) {
                  toResolve = supertypeOf(lowerBound);
               }
            } else if (originalUpperBound.length == 1) {
               Type upperBound = resolve(context, contextRawType, originalUpperBound[0], visitedTypeVariables);
               if (upperBound != originalUpperBound[0]) {
                  toResolve = subtypeOf(upperBound);
               }
            }
         }

         if (resolving != null) {
            visitedTypeVariables.put(resolving, toResolve);
         }

         return toResolve;
      }
   }

   static Type resolveTypeVariable(Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
      Class<?> declaredByRaw = declaringClassOf(unknown);
      if (declaredByRaw == null) {
         return unknown;
      } else {
         Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
         if (declaredBy instanceof ParameterizedType) {
            int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
            return ((ParameterizedType)declaredBy).getActualTypeArguments()[index];
         } else {
            return unknown;
         }
      }
   }

   private static int indexOf(Object[] array, Object toFind) {
      int i = 0;

      for (int length = array.length; i < length; i++) {
         if (toFind.equals(array[i])) {
            return i;
         }
      }

      throw new NoSuchElementException();
   }

   private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
      GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
      return genericDeclaration instanceof Class ? (Class)genericDeclaration : null;
   }

   static void checkNotPrimitive(Type type) {
      checkArgument(!(type instanceof Class) || !((Class)type).isPrimitive());
   }

   public static <A extends Annotation> A findAnnotation(AnnotatedElement element, Class<A> annotationType) {
      if (annotationType == null) {
         throw new NullPointerException("annotationType must not be null");
      } else {
         boolean inherited = annotationType.isAnnotationPresent(Inherited.class);
         return findAnnotation(element, annotationType, inherited, new HashSet<>());
      }
   }

   public static <A extends Annotation> A findAnnotation(Annotation annotation, Class<A> annotationType) {
      if (annotation == null) {
         throw new NullPointerException("annotation must not be null");
      } else if (annotationType == null) {
         throw new NullPointerException("annotationType must not be null");
      } else {
         Class<? extends Annotation> annotationTypeClass = annotation.annotationType();
         if (annotationTypeClass == annotationType) {
            return (A)annotation;
         } else {
            boolean inherited = annotationType.isAnnotationPresent(Inherited.class);
            return findAnnotation(annotationTypeClass, annotationType, inherited, new HashSet<>());
         }
      }
   }

   private static <A extends Annotation> A findAnnotation(AnnotatedElement element, Class<A> annotationType, boolean inherited, Set<Annotation> visited) {
      if (element != null && annotationType != null) {
         A annotation = element.getDeclaredAnnotation(annotationType);
         if (annotation != null) {
            return annotation;
         } else {
            Annotation[] declaredAnnotations = element.getDeclaredAnnotations();
            A directMetaAnnotation = findMetaAnnotation(annotationType, declaredAnnotations, inherited, visited);
            if (directMetaAnnotation != null) {
               return directMetaAnnotation;
            } else {
               if (element instanceof Class) {
                  Class<?> clazz = (Class<?>)element;

                  for (Class<?> ifc : clazz.getInterfaces()) {
                     if (ifc != Annotation.class) {
                        A annotationOnInterface = findAnnotation(ifc, annotationType, inherited, visited);
                        if (annotationOnInterface != null) {
                           return annotationOnInterface;
                        }
                     }
                  }

                  if (inherited) {
                     Class<?> superclass = clazz.getSuperclass();
                     if (superclass != null && superclass != Object.class) {
                        A annotationOnSuperclass = findAnnotation(superclass, annotationType, true, visited);
                        if (annotationOnSuperclass != null) {
                           return annotationOnSuperclass;
                        }
                     }
                  }
               }

               return findMetaAnnotation(annotationType, getAnnotations(element), inherited, visited);
            }
         }
      } else {
         return null;
      }
   }

   private static <A extends Annotation> A findMetaAnnotation(Class<A> annotationType, Annotation[] candidates, boolean inherited, Set<Annotation> visited) {
      for (Annotation candidateAnnotation : candidates) {
         Class<? extends Annotation> candidateAnnotationType = candidateAnnotation.annotationType();
         String name = candidateAnnotationType.getName();
         boolean isInJavaLangAnnotationPackage = name.startsWith("java.lang.annotation") || name.startsWith("kotlin.");
         if (!isInJavaLangAnnotationPackage && visited.add(candidateAnnotation)) {
            A metaAnnotation = findAnnotation(candidateAnnotationType, annotationType, inherited, visited);
            if (metaAnnotation != null) {
               return metaAnnotation;
            }
         }
      }

      return null;
   }

   public static Annotation[] getAnnotations(AnnotatedElement element) {
      try {
         return element.getDeclaredAnnotations();
      } catch (Throwable var2) {
         return new Annotation[0];
      }
   }

   static void checkArgument(boolean condition) {
      if (!condition) {
         throw new IllegalArgumentException();
      }
   }

   public static void processJacksonJsonIgnore(FieldInfo fieldInfo, Annotation annotation) {
      fieldInfo.ignore = true;
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("value".equals(name)) {
               fieldInfo.ignore = (Boolean)result;
            }
         } catch (Throwable var5) {
         }
      });
   }

   public static boolean isNoneStaticMemberClass(Class objectClass, Class memberClass) {
      if (memberClass != null && !memberClass.isPrimitive() && memberClass != String.class && memberClass != List.class) {
         Class enclosingClass = memberClass.getEnclosingClass();
         if (enclosingClass == null) {
            return false;
         } else if (objectClass != null && !objectClass.equals(enclosingClass)) {
            return false;
         } else {
            Constructor[] constructors = constructorCache.get(memberClass);
            if (constructors == null) {
               constructors = memberClass.getDeclaredConstructors();
               constructorCache.putIfAbsent(memberClass, constructors);
            }

            if (constructors.length == 0) {
               return false;
            } else {
               Constructor firstConstructor = constructors[0];
               if (firstConstructor.getParameterCount() == 0) {
                  return false;
               } else {
                  Class[] parameterTypes = firstConstructor.getParameterTypes();
                  return enclosingClass.equals(parameterTypes[0]);
               }
            }
         }
      } else {
         return false;
      }
   }

   public static void setNoneStaticMemberClassParent(Object object, Object parent) {
      Class objectClass = object.getClass();
      Field[] fields = declaredFieldCache.get(objectClass);
      if (fields == null) {
         Field[] declaredFields = objectClass.getDeclaredFields();
         boolean allMatch = true;

         for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
               allMatch = false;
               break;
            }
         }

         if (allMatch) {
            fields = declaredFields;
         } else {
            List<Field> list = new ArrayList<>(declaredFields.length);

            for (Field fieldx : declaredFields) {
               int modifiers = fieldx.getModifiers();
               if (!Modifier.isStatic(modifiers)) {
                  list.add(fieldx);
               }
            }

            fields = list.toArray(new Field[list.size()]);
         }

         fieldCache.putIfAbsent(objectClass, fields);
      }

      Field this0 = null;

      for (Field fieldxx : fields) {
         if ("this$0".equals(fieldxx.getName())) {
            this0 = fieldxx;
         }
      }

      if (this0 != null) {
         this0.setAccessible(true);

         try {
            this0.set(object, parent);
         } catch (IllegalAccessException var12) {
            throw new JSONException("setNoneStaticMemberClassParent error, class " + objectClass);
         }
      }
   }

   public static void cleanupCache(Class objectClass) {
      if (objectClass != null) {
         fieldCache.remove(objectClass);
         fieldMapCache.remove(objectClass);
         declaredFieldCache.remove(objectClass);
         methodCache.remove(objectClass);
         constructorCache.remove(objectClass);
      }
   }

   public static void cleanupCache(ClassLoader classLoader) {
      Iterator<Entry<Class, Field[]>> it = fieldCache.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Class, Field[]> entry = it.next();
         Class entryKey = entry.getKey();
         if (entryKey.getClassLoader() == classLoader) {
            it.remove();
         }
      }

      it = fieldMapCache.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Class, Map<String, Field>> entry = it.next();
         Class entryKey = entry.getKey();
         if (entryKey.getClassLoader() == classLoader) {
            it.remove();
         }
      }

      it = declaredFieldCache.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Class, Field[]> entry = it.next();
         Class entryKey = entry.getKey();
         if (entryKey.getClassLoader() == classLoader) {
            it.remove();
         }
      }

      it = methodCache.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Class, Method[]> entry = it.next();
         Class entryKey = entry.getKey();
         if (entryKey.getClassLoader() == classLoader) {
            it.remove();
         }
      }

      it = constructorCache.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Class, Constructor[]> entry = it.next();
         Class entryKey = entry.getKey();
         if (entryKey.getClassLoader() == classLoader) {
            it.remove();
         }
      }
   }

   public static void processJSONType1x(BeanInfo beanInfo, Annotation jsonType1x, Method method) {
      try {
         Object result = method.invoke(jsonType1x);
         String var4 = method.getName();
         switch (var4) {
            case "seeAlso":
               Class<?>[] classes = (Class<?>[])result;
               if (classes.length != 0) {
                  beanInfo.seeAlso = classes;
               }
               break;
            case "typeName":
               String typeName = (String)result;
               if (!typeName.isEmpty()) {
                  beanInfo.typeName = typeName;
               }
               break;
            case "typeKey":
               String typeKey = (String)result;
               if (!typeKey.isEmpty()) {
                  beanInfo.typeKey = typeKey;
               }
               break;
            case "rootName":
               String rootName = (String)result;
               if (!rootName.isEmpty()) {
                  beanInfo.rootName = rootName;
               }
               break;
            case "alphabetic":
               Boolean alphabetic = (Boolean)result;
               if (!alphabetic) {
                  beanInfo.alphabetic = false;
               }
               break;
            case "serializeFeatures":
            case "serialzeFeatures":
               Enum[] serializeFeatures = (Enum[])result;

               for (Enum feature : serializeFeatures) {
                  String var11 = feature.name();
                  switch (var11) {
                     case "WriteMapNullValue":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNulls.mask;
                        break;
                     case "WriteNullListAsEmpty":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNullListAsEmpty.mask;
                        break;
                     case "WriteNullStringAsEmpty":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNullStringAsEmpty.mask;
                        break;
                     case "WriteNullNumberAsZero":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNullNumberAsZero.mask;
                        break;
                     case "WriteNullBooleanAsFalse":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNullBooleanAsFalse.mask;
                        break;
                     case "BrowserCompatible":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.BrowserCompatible.mask;
                        break;
                     case "WriteClassName":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteClassName.mask;
                        break;
                     case "WriteNonStringValueAsString":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNonStringValueAsString.mask;
                        break;
                     case "WriteEnumUsingToString":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteEnumUsingToString.mask;
                        break;
                     case "NotWriteRootClassName":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.NotWriteRootClassName.mask;
                        break;
                     case "IgnoreErrorGetter":
                        beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.IgnoreErrorGetter.mask;
                  }
               }
               break;
            case "serializeEnumAsJavaBean":
               boolean serializeEnumAsJavaBean = (Boolean)result;
               if (serializeEnumAsJavaBean) {
                  beanInfo.writeEnumAsJavaBean = true;
               }
               break;
            case "naming":
               Enum naming = (Enum)result;
               beanInfo.namingStrategy = naming.name();
               break;
            case "ignores":
               String[] fieldsxx = (String[])result;
               if (fieldsxx.length != 0) {
                  if (beanInfo.ignores == null) {
                     beanInfo.ignores = fieldsxx;
                  } else {
                     LinkedHashSet<String> ignoresSet = new LinkedHashSet<>();
                     ignoresSet.addAll(Arrays.asList(beanInfo.ignores));
                     ignoresSet.addAll(Arrays.asList(fieldsxx));
                     beanInfo.ignores = ignoresSet.toArray(new String[ignoresSet.size()]);
                  }
               }
               break;
            case "includes":
               String[] fieldsx = (String[])result;
               if (fieldsx.length != 0) {
                  beanInfo.includes = fieldsx;
               }
               break;
            case "orders":
               String[] fields = (String[])result;
               if (fields.length != 0) {
                  beanInfo.orders = fields;
               }
               break;
            case "serializer":
               Class serializerClass = (Class)result;
               if (ObjectWriter.class.isAssignableFrom(serializerClass)) {
                  beanInfo.writeEnumAsJavaBean = true;
                  beanInfo.serializer = serializerClass;
               }
               break;
            case "deserializer":
               Class deserializerClass = (Class)result;
               if (ObjectReader.class.isAssignableFrom(deserializerClass)) {
                  beanInfo.deserializer = deserializerClass;
               }
         }
      } catch (Throwable var13) {
      }
   }

   public static void processJacksonJsonFormat(FieldInfo fieldInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            switch (name) {
               case "pattern":
                  String pattern = (String)result;
                  if (pattern.length() != 0) {
                     fieldInfo.format = pattern;
                  }
                  break;
               case "shape":
                  String shape = ((Enum)result).name();
                  if ("STRING".equals(shape)) {
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNonStringValueAsString.mask;
                  } else if ("NUMBER".equals(shape)) {
                     fieldInfo.format = "millis";
                  }
                  break;
               case "locale":
                  String locale = (String)result;
                  if (!locale.isEmpty() && !"##default".equals(locale)) {
                     fieldInfo.locale = Locale.forLanguageTag(locale);
                  }
            }
         } catch (Throwable var8) {
         }
      });
   }

   public static void processJacksonJsonFormat(BeanInfo beanInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("pattern".equals(name)) {
               String pattern = (String)result;
               if (!pattern.isEmpty()) {
                  beanInfo.format = pattern;
               }
            } else if ("shape".equals(name)) {
               String shape = ((Enum)result).name();
               if ("NUMBER".equals(shape)) {
                  beanInfo.format = "millis";
               } else if ("OBJECT".equals(shape)) {
                  beanInfo.writeEnumAsJavaBean = true;
               }
            } else if ("locale".equals(name)) {
               String locale = (String)result;
               if (!locale.isEmpty() && !"##default".equals(locale)) {
                  beanInfo.locale = Locale.forLanguageTag(locale);
               }
            }
         } catch (Throwable var6) {
         }
      });
   }

   public static void processJacksonJsonInclude(BeanInfo beanInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("value".equals(name)) {
               String include = ((Enum)result).name();
               switch (include) {
                  case "ALWAYS":
                     beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.WriteNulls.mask;
                     break;
                  case "NON_DEFAULT":
                     beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.NotWriteDefaultValue.mask;
                     break;
                  case "NON_EMPTY":
                     beanInfo.writerFeatures = beanInfo.writerFeatures | JSONWriter.Feature.NotWriteEmptyArray.mask;
               }
            }
         } catch (Throwable var8) {
         }
      });
   }

   public static void processJacksonJsonInclude(FieldInfo fieldInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("value".equals(name)) {
               String include = ((Enum)result).name();
               switch (include) {
                  case "ALWAYS":
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.WriteNulls.mask;
                     break;
                  case "NON_DEFAULT":
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.NotWriteDefaultValue.mask;
                     break;
                  case "NON_EMPTY":
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.NotWriteEmptyArray.mask;
                     fieldInfo.features = fieldInfo.features | JSONWriter.Feature.IgnoreEmpty.mask;
               }
            }
         } catch (Throwable var8) {
         }
      });
   }

   public static void processJacksonJsonUnwrapped(FieldInfo fieldInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("enabled".equals(name)) {
               boolean value = (Boolean)result;
               if (value) {
                  fieldInfo.features = 562949953421312L;
               }
            }
         } catch (Throwable var6) {
         }
      });
   }

   public static void processJacksonJsonTypeName(BeanInfo beanInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            if ("value".equals(name)) {
               String value = (String)result;
               if (!value.isEmpty()) {
                  beanInfo.typeName = value;
               }
            }
         } catch (Throwable var6) {
         }
      });
   }

   public static void processJacksonJsonSubTypesType(BeanInfo beanInfo, int index, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            switch (name) {
               case "value": {
                  Class value = (Class)result;
                  beanInfo.seeAlso[index] = value;
                  break;
               }
               case "name": {
                  String value = (String)result;
                  beanInfo.seeAlsoNames[index] = value;
               }
            }
         } catch (Throwable var9) {
         }
      });
   }

   public static void processGsonSerializedName(FieldInfo fieldInfo, Annotation annotation) {
      Class<? extends Annotation> annotationClass = (Class<? extends Annotation>)annotation.getClass();
      annotationMethods(annotationClass, m -> {
         String name = m.getName();

         try {
            Object result = m.invoke(annotation);
            switch (name) {
               case "value":
                  String value = (String)result;
                  if (!value.isEmpty()) {
                     fieldInfo.fieldName = value;
                  }
                  break;
               case "alternate":
                  String[] alternate = (String[])result;
                  if (alternate.length != 0) {
                     fieldInfo.alternateNames = alternate;
                  }
            }
         } catch (Throwable var9) {
         }
      });
   }

   public static boolean isExtendedMap(Class objectClass) {
      if (objectClass != HashMap.class && objectClass != LinkedHashMap.class && objectClass != TreeMap.class && !"".equals(objectClass.getSimpleName())) {
         Class superclass = objectClass.getSuperclass();
         if (superclass != HashMap.class && superclass != LinkedHashMap.class && superclass != TreeMap.class) {
            return false;
         } else {
            Constructor defaultConstructor = getDefaultConstructor(objectClass, false);
            if (defaultConstructor != null) {
               return false;
            } else {
               List<Field> fields = new ArrayList<>();
               declaredFields(
                  objectClass,
                  field -> {
                     int modifiers = field.getModifiers();
                     if (!Modifier.isStatic(modifiers)
                        && !Modifier.isTransient(modifiers)
                        && !field.getDeclaringClass().isAssignableFrom(superclass)
                        && !"this$0".equals(field.getName())) {
                        fields.add(field);
                     }
                  }
               );
               return !fields.isEmpty();
            }
         }
      } else {
         return false;
      }
   }

   public static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
      private final Type componentType;
      private static final long serialVersionUID = 0L;

      public GenericArrayTypeImpl(Type componentType) {
         this.componentType = BeanUtils.canonicalize(componentType);
      }

      @Override
      public Type getGenericComponentType() {
         return this.componentType;
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof GenericArrayType && BeanUtils.equals(this, (GenericArrayType)o);
      }

      @Override
      public int hashCode() {
         return this.componentType.hashCode();
      }

      @Override
      public String toString() {
         return BeanUtils.typeToString(this.componentType) + "[]";
      }
   }

   static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
      private final Type ownerType;
      private final Type rawType;
      private final Type[] typeArguments;
      private static final long serialVersionUID = 0L;

      public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
         if (rawType instanceof Class) {
            Class<?> rawTypeAsClass = (Class<?>)rawType;
            boolean isStaticOrTopLevelClass = Modifier.isStatic(rawTypeAsClass.getModifiers()) || rawTypeAsClass.getEnclosingClass() == null;
            BeanUtils.checkArgument(ownerType != null || isStaticOrTopLevelClass);
         }

         this.ownerType = ownerType == null ? null : BeanUtils.canonicalize(ownerType);
         this.rawType = BeanUtils.canonicalize(rawType);
         this.typeArguments = (Type[])typeArguments.clone();
         int t = 0;

         for (int length = this.typeArguments.length; t < length; t++) {
            BeanUtils.checkNotPrimitive(this.typeArguments[t]);
            this.typeArguments[t] = BeanUtils.canonicalize(this.typeArguments[t]);
         }
      }

      @Override
      public Type[] getActualTypeArguments() {
         return (Type[])this.typeArguments.clone();
      }

      @Override
      public Type getRawType() {
         return this.rawType;
      }

      @Override
      public Type getOwnerType() {
         return this.ownerType;
      }

      @Override
      public boolean equals(Object other) {
         return other instanceof ParameterizedType && BeanUtils.equals(this, (ParameterizedType)other);
      }

      @Override
      public int hashCode() {
         return Arrays.hashCode((Object[])this.typeArguments) ^ this.rawType.hashCode() ^ BeanUtils.hashCodeOrZero(this.ownerType);
      }

      @Override
      public String toString() {
         int length = this.typeArguments.length;
         if (length == 0) {
            return BeanUtils.typeToString(this.rawType);
         } else {
            StringBuilder stringBuilder = new StringBuilder(30 * (length + 1));
            stringBuilder.append(BeanUtils.typeToString(this.rawType)).append("<").append(BeanUtils.typeToString(this.typeArguments[0]));

            for (int i = 1; i < length; i++) {
               stringBuilder.append(", ").append(BeanUtils.typeToString(this.typeArguments[i]));
            }

            return stringBuilder.append(">").toString();
         }
      }
   }

   static final class WildcardTypeImpl implements WildcardType, Serializable {
      private final Type upperBound;
      private final Type lowerBound;
      private static final long serialVersionUID = 0L;

      public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
         BeanUtils.checkArgument(lowerBounds.length <= 1);
         BeanUtils.checkArgument(upperBounds.length == 1);
         if (lowerBounds.length == 1) {
            BeanUtils.checkNotPrimitive(lowerBounds[0]);
            BeanUtils.checkArgument(upperBounds[0] == Object.class);
            this.lowerBound = BeanUtils.canonicalize(lowerBounds[0]);
            this.upperBound = Object.class;
         } else {
            BeanUtils.checkNotPrimitive(upperBounds[0]);
            this.lowerBound = null;
            this.upperBound = BeanUtils.canonicalize(upperBounds[0]);
         }
      }

      @Override
      public Type[] getUpperBounds() {
         return new Type[]{this.upperBound};
      }

      @Override
      public Type[] getLowerBounds() {
         return this.lowerBound != null ? new Type[]{this.lowerBound} : BeanUtils.EMPTY_TYPE_ARRAY;
      }

      @Override
      public boolean equals(Object other) {
         return other instanceof WildcardType && BeanUtils.equals(this, (WildcardType)other);
      }

      @Override
      public int hashCode() {
         return (this.lowerBound != null ? 31 + this.lowerBound.hashCode() : 1) ^ 31 + this.upperBound.hashCode();
      }

      @Override
      public String toString() {
         if (this.lowerBound != null) {
            return "? super " + BeanUtils.typeToString(this.lowerBound);
         } else {
            return this.upperBound == Object.class ? "?" : "? extends " + BeanUtils.typeToString(this.upperBound);
         }
      }
   }
}
