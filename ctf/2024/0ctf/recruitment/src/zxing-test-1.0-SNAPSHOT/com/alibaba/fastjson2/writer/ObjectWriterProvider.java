package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.modules.ObjectCodecProvider;
import com.alibaba.fastjson2.modules.ObjectWriterAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.GuavaSupport;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ObjectWriterProvider implements ObjectCodecProvider {
   static final int TYPE_INT32_MASK = 2;
   static final int TYPE_INT64_MASK = 4;
   static final int TYPE_DECIMAL_MASK = 8;
   static final int TYPE_DATE_MASK = 16;
   static final int TYPE_ENUM_MASK = 32;
   static final int NAME_COMPATIBLE_WITH_FILED = 64;
   final ConcurrentMap<Type, ObjectWriter> cache = new ConcurrentHashMap<>();
   final ConcurrentMap<Type, ObjectWriter> cacheFieldBased = new ConcurrentHashMap<>();
   final ConcurrentMap<Class, Class> mixInCache = new ConcurrentHashMap<>();
   final ObjectWriterCreator creator;
   final List<ObjectWriterModule> modules = new ArrayList<>();
   PropertyNamingStrategy namingStrategy;
   boolean disableReferenceDetect = JSONFactory.isDisableReferenceDetect();
   boolean disableArrayMapping = JSONFactory.isDisableArrayMapping();
   boolean disableJSONB = JSONFactory.isDisableJSONB();
   boolean disableAutoType = JSONFactory.isDisableAutoType();
   volatile long userDefineMask;
   boolean alphabetic = JSONFactory.isDefaultWriterAlphabetic();
   static final int ENUM = 16384;
   static final int[] PRIMITIVE_HASH_CODES;
   static final int[] NOT_REFERENCES_TYPE_HASH_CODES;

   public ObjectWriterProvider() {
      this((PropertyNamingStrategy)null);
   }

   public ObjectWriterProvider(PropertyNamingStrategy namingStrategy) {
      this.init();
      ObjectWriterCreator creator = null;
      String var3 = JSONFactory.CREATOR;
      switch (var3) {
         case "reflect":
         case "lambda":
            creator = ObjectWriterCreator.INSTANCE;
            break;
         case "asm":
         default:
            try {
               if (!JDKUtils.ANDROID && !JDKUtils.GRAAL) {
                  creator = ObjectWriterCreatorASM.INSTANCE;
               }
            } catch (Throwable var6) {
            }

            if (creator == null) {
               creator = ObjectWriterCreator.INSTANCE;
            }
      }

      this.creator = creator;
      this.namingStrategy = namingStrategy;
   }

   public ObjectWriterProvider(ObjectWriterCreator creator) {
      this.init();
      this.creator = creator;
   }

   public PropertyNamingStrategy getNamingStrategy() {
      return this.namingStrategy;
   }

   /** @deprecated */
   public void setCompatibleWithFieldName(boolean stat) {
      if (stat) {
         this.userDefineMask |= 64L;
      } else {
         this.userDefineMask &= -65L;
      }
   }

   public void setNamingStrategy(PropertyNamingStrategy namingStrategy) {
      this.namingStrategy = namingStrategy;
   }

   public void mixIn(Class target, Class mixinSource) {
      if (mixinSource == null) {
         this.mixInCache.remove(target);
      } else {
         this.mixInCache.put(target, mixinSource);
      }

      this.cache.remove(target);
   }

   public void cleanupMixIn() {
      this.mixInCache.clear();
   }

   public ObjectWriterCreator getCreator() {
      ObjectWriterCreator contextCreator = JSONFactory.getContextWriterCreator();
      return contextCreator != null ? contextCreator : this.creator;
   }

   public ObjectWriter register(Type type, ObjectWriter objectWriter) {
      return this.register(type, objectWriter, false);
   }

   public ObjectWriter register(Type type, ObjectWriter objectWriter, boolean fieldBased) {
      if (type == Integer.class) {
         if (objectWriter != null && objectWriter != ObjectWriterImplInt32.INSTANCE) {
            this.userDefineMask |= 2L;
         } else {
            this.userDefineMask &= -3L;
         }
      } else if (type != Long.class && type != long.class) {
         if (type == BigDecimal.class) {
            if (objectWriter != null && objectWriter != ObjectWriterImplBigDecimal.INSTANCE) {
               this.userDefineMask |= 8L;
            } else {
               this.userDefineMask &= -9L;
            }
         } else if (type == Date.class) {
            if (objectWriter != null && objectWriter != ObjectWriterImplDate.INSTANCE) {
               this.userDefineMask |= 16L;
            } else {
               this.userDefineMask &= -17L;
            }
         } else if (type == Enum.class) {
            if (objectWriter == null) {
               this.userDefineMask &= -33L;
            } else {
               this.userDefineMask |= 32L;
            }
         }
      } else if (objectWriter != null && objectWriter != ObjectWriterImplInt64.INSTANCE) {
         this.userDefineMask |= 4L;
      } else {
         this.userDefineMask &= -5L;
      }

      ConcurrentMap<Type, ObjectWriter> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return objectWriter == null ? cache.remove(type) : cache.put(type, objectWriter);
   }

   public ObjectWriter registerIfAbsent(Type type, ObjectWriter objectWriter) {
      return this.registerIfAbsent(type, objectWriter, false);
   }

   public ObjectWriter registerIfAbsent(Type type, ObjectWriter objectWriter, boolean fieldBased) {
      ConcurrentMap<Type, ObjectWriter> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.putIfAbsent(type, objectWriter);
   }

   public ObjectWriter unregister(Type type) {
      return this.unregister(type, false);
   }

   public ObjectWriter unregister(Type type, boolean fieldBased) {
      ConcurrentMap<Type, ObjectWriter> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.remove(type);
   }

   public boolean unregister(Type type, ObjectWriter objectWriter) {
      return this.unregister(type, objectWriter, false);
   }

   public boolean unregister(Type type, ObjectWriter objectWriter, boolean fieldBased) {
      ConcurrentMap<Type, ObjectWriter> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.remove(type, objectWriter);
   }

   public boolean register(ObjectWriterModule module) {
      for (int i = this.modules.size() - 1; i >= 0; i--) {
         if (this.modules.get(i) == module) {
            return false;
         }
      }

      module.init(this);
      this.modules.add(0, module);
      return true;
   }

   public boolean unregister(ObjectWriterModule module) {
      return this.modules.remove(module);
   }

   @Override
   public Class getMixIn(Class target) {
      return this.mixInCache.get(target);
   }

   public void init() {
      this.modules.add(new ObjectWriterBaseModule(this));
   }

   public List<ObjectWriterModule> getModules() {
      return this.modules;
   }

   public void getFieldInfo(BeanInfo beanInfo, FieldInfo fieldInfo, Class objectClass, Field field) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectWriterModule module = this.modules.get(i);
         ObjectWriterAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getFieldInfo(beanInfo, fieldInfo, objectClass, field);
         }
      }
   }

   public void getFieldInfo(BeanInfo beanInfo, FieldInfo fieldInfo, Class objectClass, Method method) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectWriterModule module = this.modules.get(i);
         ObjectWriterAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getFieldInfo(beanInfo, fieldInfo, objectClass, method);
         }
      }
   }

   public void getBeanInfo(BeanInfo beanInfo, Class objectClass) {
      if (this.namingStrategy != null && this.namingStrategy != PropertyNamingStrategy.NeverUseThisValueExceptDefaultValue) {
         beanInfo.namingStrategy = this.namingStrategy.name();
      }

      for (int i = 0; i < this.modules.size(); i++) {
         ObjectWriterModule module = this.modules.get(i);
         ObjectWriterAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getBeanInfo(beanInfo, objectClass);
         }
      }
   }

   public ObjectWriter getObjectWriter(Type objectType, String format, Locale locale) {
      if (objectType == Double.class) {
         return new ObjectWriterImplDouble(new DecimalFormat(format));
      } else if (objectType == Float.class) {
         return new ObjectWriterImplFloat(new DecimalFormat(format));
      } else if (objectType == BigDecimal.class) {
         return new ObjectWriterImplBigDecimal(new DecimalFormat(format), null);
      } else if (objectType == LocalDate.class) {
         return ObjectWriterImplLocalDate.of(format, null);
      } else if (objectType == LocalDateTime.class) {
         return new ObjectWriterImplLocalDateTime(format, null);
      } else if (objectType == LocalTime.class) {
         return new ObjectWriterImplLocalTime(format, null);
      } else if (objectType == Date.class) {
         return new ObjectWriterImplDate(format, null);
      } else if (objectType == OffsetDateTime.class) {
         return ObjectWriterImplOffsetDateTime.of(format, null);
      } else {
         return (ObjectWriter)(objectType == ZonedDateTime.class ? new ObjectWriterImplZonedDateTime(format, null) : this.getObjectWriter(objectType));
      }
   }

   public ObjectWriter getObjectWriter(Class objectClass) {
      return this.getObjectWriter(objectClass, objectClass, false);
   }

   public ObjectWriter getObjectWriter(Type objectType, Class objectClass) {
      return this.getObjectWriter(objectType, objectClass, false);
   }

   public ObjectWriter getObjectWriter(Type objectType) {
      Class objectClass = TypeUtils.getClass(objectType);
      return this.getObjectWriter(objectType, objectClass, false);
   }

   public ObjectWriter getObjectWriterFromCache(Type objectType, Class objectClass, boolean fieldBased) {
      return fieldBased ? this.cacheFieldBased.get(objectType) : this.cache.get(objectType);
   }

   public ObjectWriter getObjectWriter(Type objectType, Class objectClass, String format, boolean fieldBased) {
      ObjectWriter objectWriter = this.getObjectWriter(objectType, objectClass, fieldBased);
      return (ObjectWriter)(format != null && objectType == LocalDateTime.class && objectWriter == ObjectWriterImplLocalDateTime.INSTANCE
         ? ObjectWriterImplLocalDateTime.of(format, null)
         : objectWriter);
   }

   public ObjectWriter getObjectWriter(Type objectType, Class objectClass, boolean fieldBased) {
      ObjectWriter objectWriter = fieldBased ? this.cacheFieldBased.get(objectType) : this.cache.get(objectType);
      return objectWriter != null ? objectWriter : this.getObjectWriterInternal(objectType, objectClass, fieldBased);
   }

   private ObjectWriter getObjectWriterInternal(Type objectType, Class objectClass, boolean fieldBased) {
      Class superclass = objectClass.getSuperclass();
      if (!objectClass.isEnum() && superclass != null && superclass.isEnum()) {
         return this.getObjectWriter(superclass, superclass, fieldBased);
      } else {
         String className = objectClass.getName();
         if (fieldBased) {
            if (superclass != null && superclass != Object.class && "com.google.protobuf.GeneratedMessageV3".equals(superclass.getName())) {
               fieldBased = false;
            } else {
               switch (className) {
                  case "springfox.documentation.spring.web.json.Json":
                  case "cn.hutool.json.JSONArray":
                  case "cn.hutool.json.JSONObject":
                  case "cn.hutool.core.map.CaseInsensitiveMap":
                  case "cn.hutool.core.map.CaseInsensitiveLinkedMap":
                     fieldBased = false;
               }
            }
         } else {
            byte var11 = -1;
            switch (className.hashCode()) {
               case -466733643:
                  if (className.equals("org.springframework.core.ResolvableType")) {
                     var11 = 0;
                  }
               default:
                  switch (var11) {
                     case 0:
                        fieldBased = true;
                  }
            }
         }

         ObjectWriter objectWriter = fieldBased ? this.cacheFieldBased.get(objectType) : this.cache.get(objectType);
         if (objectWriter != null) {
            return objectWriter;
         } else {
            if (TypeUtils.isProxy(objectClass)) {
               if (objectClass == objectType) {
                  objectType = superclass;
               }

               objectClass = superclass;
               if (fieldBased) {
                  fieldBased = false;
                  objectWriter = this.cacheFieldBased.get(objectType);
                  if (objectWriter != null) {
                     return objectWriter;
                  }
               }
            }

            boolean useModules = true;
            if (fieldBased && Iterable.class.isAssignableFrom(objectClass) && !Collection.class.isAssignableFrom(objectClass)) {
               useModules = false;
            }

            if (useModules) {
               for (int i = 0; i < this.modules.size(); i++) {
                  ObjectWriterModule module = this.modules.get(i);
                  objectWriter = module.getObjectWriter(objectType, objectClass);
                  if (objectWriter != null) {
                     ObjectWriter previous = fieldBased
                        ? this.cacheFieldBased.putIfAbsent(objectType, objectWriter)
                        : this.cache.putIfAbsent(objectType, objectWriter);
                     if (previous != null) {
                        objectWriter = previous;
                     }

                     return objectWriter;
                  }
               }
            }

            switch (className) {
               case "com.google.common.collect.HashMultimap":
               case "com.google.common.collect.LinkedListMultimap":
               case "com.google.common.collect.LinkedHashMultimap":
               case "com.google.common.collect.ArrayListMultimap":
               case "com.google.common.collect.TreeMultimap":
                  objectWriter = GuavaSupport.createAsMapWriter(objectClass);
                  break;
               case "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList":
                  objectWriter = ObjectWriterImplList.INSTANCE;
                  break;
               case "com.alibaba.fastjson.JSONObject":
                  objectWriter = ObjectWriterImplMap.of(objectClass);
                  break;
               case "android.net.Uri$OpaqueUri":
               case "android.net.Uri$HierarchicalUri":
               case "android.net.Uri$StringUri":
                  objectWriter = ObjectWriterImplToString.INSTANCE;
                  break;
               case "com.clickhouse.data.value.UnsignedLong":
                  objectWriter = new ObjectWriterImplToString(true);
            }

            if (objectWriter == null && !fieldBased && Map.class.isAssignableFrom(objectClass) && BeanUtils.isExtendedMap(objectClass)) {
               return ObjectWriterImplMap.of(objectClass);
            } else {
               if (objectWriter == null) {
                  ObjectWriterCreator creator = this.getCreator();
                  objectWriter = creator.createObjectWriter(objectClass, fieldBased ? JSONWriter.Feature.FieldBased.mask : 0L, this);
                  ObjectWriter previous = fieldBased
                     ? this.cacheFieldBased.putIfAbsent(objectType, objectWriter)
                     : this.cache.putIfAbsent(objectType, objectWriter);
                  if (previous != null) {
                     objectWriter = previous;
                  }
               }

               return objectWriter;
            }
         }
      }
   }

   public static boolean isPrimitiveOrEnum(Class<?> clazz) {
      return Arrays.binarySearch(PRIMITIVE_HASH_CODES, System.identityHashCode(clazz)) >= 0
         || (clazz.getModifiers() & 16384) != 0 && clazz.getSuperclass() == Enum.class;
   }

   public static boolean isNotReferenceDetect(Class<?> clazz) {
      return Arrays.binarySearch(NOT_REFERENCES_TYPE_HASH_CODES, System.identityHashCode(clazz)) >= 0
         || (clazz.getModifiers() & 16384) != 0 && clazz.getSuperclass() == Enum.class;
   }

   public void clear() {
      this.mixInCache.clear();
      this.cache.clear();
      this.cacheFieldBased.clear();
   }

   public void cleanup(Class objectClass) {
      this.mixInCache.remove(objectClass);
      this.cache.remove(objectClass);
      this.cacheFieldBased.remove(objectClass);
      BeanUtils.cleanupCache(objectClass);
   }

   static boolean match(Type objectType, ObjectWriter objectWriter, ClassLoader classLoader, IdentityHashMap<ObjectWriter, Object> checkedMap) {
      Class<?> objectClass = TypeUtils.getClass(objectType);
      if (objectClass != null && objectClass.getClassLoader() == classLoader) {
         return true;
      } else if (checkedMap.containsKey(objectWriter)) {
         return false;
      } else if (objectWriter instanceof ObjectWriterImplMap) {
         ObjectWriterImplMap mapTyped = (ObjectWriterImplMap)objectWriter;
         Class valueClass = TypeUtils.getClass(mapTyped.valueType);
         if (valueClass != null && valueClass.getClassLoader() == classLoader) {
            return true;
         } else {
            Class keyClass = TypeUtils.getClass(mapTyped.keyType);
            return keyClass != null && keyClass.getClassLoader() == classLoader;
         }
      } else if (objectWriter instanceof ObjectWriterImplCollection) {
         Class itemClass = TypeUtils.getClass(((ObjectWriterImplCollection)objectWriter).itemType);
         return itemClass != null && itemClass.getClassLoader() == classLoader;
      } else if (objectWriter instanceof ObjectWriterImplOptional) {
         Class itemClass = TypeUtils.getClass(((ObjectWriterImplOptional)objectWriter).valueType);
         return itemClass != null && itemClass.getClassLoader() == classLoader;
      } else {
         if (objectWriter instanceof ObjectWriterAdapter) {
            checkedMap.put(objectWriter, null);
            List<FieldWriter> fieldWriters = ((ObjectWriterAdapter)objectWriter).fieldWriters;

            for (int i = 0; i < fieldWriters.size(); i++) {
               FieldWriter fieldWriter = fieldWriters.get(i);
               if (fieldWriter instanceof FieldWriterObject) {
                  ObjectWriter initObjectWriter = ((FieldWriterObject)fieldWriter).initObjectWriter;
                  if (match(null, initObjectWriter, classLoader, checkedMap)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   public void cleanup(ClassLoader classLoader) {
      this.mixInCache.entrySet().removeIf(entry -> entry.getKey().getClassLoader() == classLoader);
      IdentityHashMap<ObjectWriter, Object> checkedMap = new IdentityHashMap<>();
      this.cache.entrySet().removeIf(entry -> match(entry.getKey(), entry.getValue(), classLoader, checkedMap));
      this.cacheFieldBased.entrySet().removeIf(entry -> match(entry.getKey(), entry.getValue(), classLoader, checkedMap));
      BeanUtils.cleanupCache(classLoader);
   }

   public boolean isDisableReferenceDetect() {
      return this.disableReferenceDetect;
   }

   public boolean isDisableAutoType() {
      return this.disableAutoType;
   }

   public boolean isDisableJSONB() {
      return this.disableJSONB;
   }

   public boolean isDisableArrayMapping() {
      return this.disableArrayMapping;
   }

   public void setDisableReferenceDetect(boolean disableReferenceDetect) {
      this.disableReferenceDetect = disableReferenceDetect;
   }

   public void setDisableArrayMapping(boolean disableArrayMapping) {
      this.disableArrayMapping = disableArrayMapping;
   }

   public void setDisableJSONB(boolean disableJSONB) {
      this.disableJSONB = disableJSONB;
   }

   public void setDisableAutoType(boolean disableAutoType) {
      this.disableAutoType = disableAutoType;
   }

   public boolean isAlphabetic() {
      return this.alphabetic;
   }

   protected BeanInfo createBeanInfo() {
      return new BeanInfo(this);
   }

   static {
      Class<?>[] classes = new Class[]{
         boolean.class,
         Boolean.class,
         Character.class,
         char.class,
         Byte.class,
         byte.class,
         Short.class,
         short.class,
         Integer.class,
         int.class,
         Long.class,
         long.class,
         Float.class,
         float.class,
         Double.class,
         double.class,
         BigInteger.class,
         BigDecimal.class,
         String.class,
         Currency.class,
         Date.class,
         Calendar.class,
         UUID.class,
         Locale.class,
         LocalTime.class,
         LocalDate.class,
         LocalDateTime.class,
         Instant.class,
         ZoneId.class,
         ZonedDateTime.class,
         OffsetDateTime.class,
         OffsetTime.class,
         AtomicInteger.class,
         AtomicLong.class,
         String.class,
         StackTraceElement.class,
         Collections.emptyList().getClass(),
         Collections.emptyMap().getClass(),
         Collections.emptySet().getClass()
      };
      int[] codes = new int[classes.length];

      for (int i = 0; i < classes.length; i++) {
         codes[i] = System.identityHashCode(classes[i]);
      }

      Arrays.sort(codes);
      PRIMITIVE_HASH_CODES = codes;
      int[] codes2 = Arrays.copyOf(codes, codes.length + 3);
      codes2[codes2.length - 1] = System.identityHashCode(Class.class);
      codes2[codes2.length - 2] = System.identityHashCode(int[].class);
      codes2[codes2.length - 3] = System.identityHashCode(long[].class);
      Arrays.sort(codes2);
      NOT_REFERENCES_TYPE_HASH_CODES = codes2;
   }
}
