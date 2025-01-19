package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.function.FieldBiConsumer;
import com.alibaba.fastjson2.function.FieldConsumer;
import com.alibaba.fastjson2.modules.ObjectCodecProvider;
import com.alibaba.fastjson2.modules.ObjectReaderAnnotationProcessor;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReaderProvider implements ObjectCodecProvider {
   static final ClassLoader FASTJSON2_CLASS_LOADER = JSON.class.getClassLoader();
   public static final boolean SAFE_MODE;
   static final String[] DENYS;
   static final String[] AUTO_TYPE_ACCEPT_LIST;
   static JSONReader.AutoTypeBeforeHandler DEFAULT_AUTO_TYPE_BEFORE_HANDLER;
   static Consumer<Class> DEFAULT_AUTO_TYPE_HANDLER;
   static boolean DEFAULT_AUTO_TYPE_HANDLER_INIT_ERROR;
   static ObjectReaderProvider.ObjectReaderCachePair readerCache;
   final ConcurrentMap<Type, ObjectReader> cache = new ConcurrentHashMap<>();
   final ConcurrentMap<Type, ObjectReader> cacheFieldBased = new ConcurrentHashMap<>();
   final ConcurrentMap<Integer, ConcurrentHashMap<Long, ObjectReader>> tclHashCaches = new ConcurrentHashMap<>();
   final ConcurrentMap<Long, ObjectReader> hashCache = new ConcurrentHashMap<>();
   final ConcurrentMap<Class, Class> mixInCache = new ConcurrentHashMap<>();
   final ObjectReaderProvider.LRUAutoTypeCache autoTypeList = new ObjectReaderProvider.LRUAutoTypeCache(1024);
   private final ConcurrentMap<Type, Map<Type, Function>> typeConverts = new ConcurrentHashMap<>();
   final ObjectReaderCreator creator;
   final List<ObjectReaderModule> modules = new ArrayList<>();
   boolean disableReferenceDetect = JSONFactory.isDisableReferenceDetect();
   boolean disableArrayMapping = JSONFactory.isDisableArrayMapping();
   boolean disableJSONB = JSONFactory.isDisableJSONB();
   boolean disableAutoType = JSONFactory.isDisableAutoType();
   boolean disableSmartMatch = JSONFactory.isDisableSmartMatch();
   private long[] acceptHashCodes;
   private JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler;
   private Consumer<Class> autoTypeHandler;
   PropertyNamingStrategy namingStrategy;

   public void registerIfAbsent(long hashCode, ObjectReader objectReader) {
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      if (tcl != null && tcl != JSON.class.getClassLoader()) {
         int tclHash = System.identityHashCode(tcl);
         ConcurrentHashMap<Long, ObjectReader> tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         if (tclHashCache == null) {
            this.tclHashCaches.putIfAbsent(tclHash, new ConcurrentHashMap<>());
            tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         }

         tclHashCache.putIfAbsent(hashCode, objectReader);
      }

      this.hashCache.putIfAbsent(hashCode, objectReader);
   }

   public void addAutoTypeAccept(String name) {
      if (name != null && name.length() != 0) {
         long hash = Fnv.hashCode64(name);
         if (Arrays.binarySearch(this.acceptHashCodes, hash) < 0) {
            long[] hashCodes = new long[this.acceptHashCodes.length + 1];
            hashCodes[hashCodes.length - 1] = hash;
            System.arraycopy(this.acceptHashCodes, 0, hashCodes, 0, this.acceptHashCodes.length);
            Arrays.sort(hashCodes);
            this.acceptHashCodes = hashCodes;
         }
      }
   }

   @Deprecated
   public void addAutoTypeDeny(String name) {
   }

   public Consumer<Class> getAutoTypeHandler() {
      return this.autoTypeHandler;
   }

   public void setAutoTypeHandler(Consumer<Class> autoTypeHandler) {
      this.autoTypeHandler = autoTypeHandler;
   }

   @Override
   public Class getMixIn(Class target) {
      return this.mixInCache.get(target);
   }

   public void cleanupMixIn() {
      this.mixInCache.clear();
   }

   public void mixIn(Class target, Class mixinSource) {
      if (mixinSource == null) {
         this.mixInCache.remove(target);
      } else {
         this.mixInCache.put(target, mixinSource);
      }

      this.cache.remove(target);
      this.cacheFieldBased.remove(target);
   }

   public void registerSeeAlsoSubType(Class subTypeClass) {
      this.registerSeeAlsoSubType(subTypeClass, null);
   }

   public void registerSeeAlsoSubType(Class subTypeClass, String subTypeClassName) {
      Class superClass = subTypeClass.getSuperclass();
      if (superClass == null) {
         throw new JSONException("superclass is null");
      } else {
         ObjectReader objectReader = this.getObjectReader(superClass);
         if (objectReader instanceof ObjectReaderSeeAlso) {
            ObjectReaderSeeAlso readerSeeAlso = (ObjectReaderSeeAlso)objectReader;
            ObjectReaderSeeAlso readerSeeAlsoNew = readerSeeAlso.addSubType(subTypeClass, subTypeClassName);
            if (readerSeeAlsoNew != readerSeeAlso) {
               if (this.cache.containsKey(superClass)) {
                  this.cache.put(superClass, readerSeeAlsoNew);
               } else {
                  this.cacheFieldBased.put(subTypeClass, readerSeeAlsoNew);
               }
            }
         }
      }
   }

   public ObjectReader register(Type type, ObjectReader objectReader, boolean fieldBased) {
      ConcurrentMap<Type, ObjectReader> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return objectReader == null ? cache.remove(type) : cache.put(type, objectReader);
   }

   public ObjectReader register(Type type, ObjectReader objectReader) {
      return this.register(type, objectReader, false);
   }

   public ObjectReader registerIfAbsent(Type type, ObjectReader objectReader) {
      return this.registerIfAbsent(type, objectReader, false);
   }

   public ObjectReader registerIfAbsent(Type type, ObjectReader objectReader, boolean fieldBased) {
      ConcurrentMap<Type, ObjectReader> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.putIfAbsent(type, objectReader);
   }

   public ObjectReader unregisterObjectReader(Type type) {
      return this.unregisterObjectReader(type, false);
   }

   public ObjectReader unregisterObjectReader(Type type, boolean fieldBased) {
      ConcurrentMap<Type, ObjectReader> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.remove(type);
   }

   public boolean unregisterObjectReader(Type type, ObjectReader reader) {
      return this.unregisterObjectReader(type, reader, false);
   }

   public boolean unregisterObjectReader(Type type, ObjectReader reader, boolean fieldBased) {
      ConcurrentMap<Type, ObjectReader> cache = fieldBased ? this.cacheFieldBased : this.cache;
      return cache.remove(type, reader);
   }

   public boolean register(ObjectReaderModule module) {
      for (int i = this.modules.size() - 1; i >= 0; i--) {
         if (this.modules.get(i) == module) {
            return false;
         }
      }

      module.init(this);
      this.modules.add(0, module);
      return true;
   }

   public boolean unregister(ObjectReaderModule module) {
      return this.modules.remove(module);
   }

   public void cleanup(Class objectClass) {
      this.mixInCache.remove(objectClass);
      this.cache.remove(objectClass);
      this.cacheFieldBased.remove(objectClass);

      for (ConcurrentHashMap<Long, ObjectReader> tlc : this.tclHashCaches.values()) {
         Iterator<Entry<Long, ObjectReader>> it = tlc.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Long, ObjectReader> entry = it.next();
            ObjectReader reader = entry.getValue();
            if (reader.getObjectClass() == objectClass) {
               it.remove();
            }
         }
      }

      BeanUtils.cleanupCache(objectClass);
   }

   public void clear() {
      this.mixInCache.clear();
      this.cache.clear();
      this.cacheFieldBased.clear();
   }

   static boolean match(Type objectType, ObjectReader objectReader, ClassLoader classLoader) {
      Class<?> objectClass = TypeUtils.getClass(objectType);
      if (objectClass != null && objectClass.getClassLoader() == classLoader) {
         return true;
      } else {
         if (objectType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)objectType;
            Type rawType = paramType.getRawType();
            if (match(rawType, objectReader, classLoader)) {
               return true;
            }

            for (Type argType : paramType.getActualTypeArguments()) {
               if (match(argType, objectReader, classLoader)) {
                  return true;
               }
            }
         }

         if (objectReader instanceof ObjectReaderImplMapTyped) {
            ObjectReaderImplMapTyped mapTyped = (ObjectReaderImplMapTyped)objectReader;
            Class valueClass = mapTyped.valueClass;
            if (valueClass != null && valueClass.getClassLoader() == classLoader) {
               return true;
            } else {
               Class keyClass = TypeUtils.getClass(mapTyped.keyType);
               return keyClass != null && keyClass.getClassLoader() == classLoader;
            }
         } else if (objectReader instanceof ObjectReaderImplList) {
            ObjectReaderImplList list = (ObjectReaderImplList)objectReader;
            return list.itemClass != null && list.itemClass.getClassLoader() == classLoader;
         } else if (objectReader instanceof ObjectReaderImplOptional) {
            Class itemClass = ((ObjectReaderImplOptional)objectReader).itemClass;
            return itemClass != null && itemClass.getClassLoader() == classLoader;
         } else {
            if (objectReader instanceof ObjectReaderAdapter) {
               FieldReader[] fieldReaders = ((ObjectReaderAdapter)objectReader).fieldReaders;

               for (FieldReader fieldReader : fieldReaders) {
                  if (fieldReader.fieldClass != null && fieldReader.fieldClass.getClassLoader() == classLoader) {
                     return true;
                  }

                  Type fieldType = fieldReader.fieldType;
                  if (fieldType instanceof ParameterizedType && match(fieldType, null, classLoader)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public void cleanup(ClassLoader classLoader) {
      this.mixInCache.entrySet().removeIf(entry -> entry.getKey().getClassLoader() == classLoader);
      this.cache.entrySet().removeIf(entry -> match(entry.getKey(), entry.getValue(), classLoader));
      this.cacheFieldBased.entrySet().removeIf(entry -> match(entry.getKey(), entry.getValue(), classLoader));
      int tclHash = System.identityHashCode(classLoader);
      this.tclHashCaches.remove(Integer.valueOf(tclHash));
      BeanUtils.cleanupCache(classLoader);
   }

   public ObjectReaderCreator getCreator() {
      ObjectReaderCreator contextCreator = JSONFactory.getContextReaderCreator();
      return contextCreator != null ? contextCreator : this.creator;
   }

   public ObjectReaderProvider() {
      this.autoTypeBeforeHandler = DEFAULT_AUTO_TYPE_BEFORE_HANDLER;
      this.autoTypeHandler = DEFAULT_AUTO_TYPE_HANDLER;
      long[] hashCodes;
      if (AUTO_TYPE_ACCEPT_LIST == null) {
         hashCodes = new long[1];
      } else {
         hashCodes = new long[AUTO_TYPE_ACCEPT_LIST.length + 1];

         for (int i = 0; i < AUTO_TYPE_ACCEPT_LIST.length; i++) {
            hashCodes[i] = Fnv.hashCode64(AUTO_TYPE_ACCEPT_LIST[i]);
         }
      }

      hashCodes[hashCodes.length - 1] = -6293031534589903644L;
      Arrays.sort(hashCodes);
      this.acceptHashCodes = hashCodes;
      this.hashCache.put(Long.valueOf(ObjectArrayReader.TYPE_HASH_CODE), ObjectArrayReader.INSTANCE);
      long STRING_CLASS_NAME_HASH = -4834614249632438472L;
      this.hashCache.put(Long.valueOf(-4834614249632438472L), ObjectReaderImplString.INSTANCE);
      this.hashCache.put(Long.valueOf(Fnv.hashCode64(TypeUtils.getTypeName(HashMap.class))), ObjectReaderImplMap.INSTANCE);
      hashCodes = null;
      String var8 = JSONFactory.CREATOR;
      switch (var8) {
         case "reflect":
         case "lambda":
            hashCodes = (long[])ObjectReaderCreator.INSTANCE;
            break;
         case "asm":
         default:
            try {
               if (!JDKUtils.ANDROID && !JDKUtils.GRAAL) {
                  hashCodes = (long[])ObjectReaderCreatorASM.INSTANCE;
               }
            } catch (Throwable var5) {
            }

            if (hashCodes == null) {
               hashCodes = (long[])ObjectReaderCreator.INSTANCE;
            }
      }

      this.creator = hashCodes;
      this.modules.add(new ObjectReaderBaseModule(this));
      this.init();
   }

   public ObjectReaderProvider(ObjectReaderCreator creator) {
      this.autoTypeBeforeHandler = DEFAULT_AUTO_TYPE_BEFORE_HANDLER;
      this.autoTypeHandler = DEFAULT_AUTO_TYPE_HANDLER;
      long[] hashCodes;
      if (AUTO_TYPE_ACCEPT_LIST == null) {
         hashCodes = new long[1];
      } else {
         hashCodes = new long[AUTO_TYPE_ACCEPT_LIST.length + 1];

         for (int i = 0; i < AUTO_TYPE_ACCEPT_LIST.length; i++) {
            hashCodes[i] = Fnv.hashCode64(AUTO_TYPE_ACCEPT_LIST[i]);
         }
      }

      hashCodes[hashCodes.length - 1] = -6293031534589903644L;
      Arrays.sort(hashCodes);
      this.acceptHashCodes = hashCodes;
      this.hashCache.put(Long.valueOf(ObjectArrayReader.TYPE_HASH_CODE), ObjectArrayReader.INSTANCE);
      long STRING_CLASS_NAME_HASH = -4834614249632438472L;
      this.hashCache.put(Long.valueOf(-4834614249632438472L), ObjectReaderImplString.INSTANCE);
      this.hashCache.put(Long.valueOf(Fnv.hashCode64(TypeUtils.getTypeName(HashMap.class))), ObjectReaderImplMap.INSTANCE);
      this.creator = creator;
      this.modules.add(new ObjectReaderBaseModule(this));
      this.init();
   }

   void init() {
      for (ObjectReaderModule module : this.modules) {
         module.init(this);
      }
   }

   public Function getTypeConvert(Type from, Type to) {
      Map<Type, Function> map = this.typeConverts.get(from);
      return map == null ? null : map.get(to);
   }

   public Function registerTypeConvert(Type from, Type to, Function typeConvert) {
      Map<Type, Function> map = this.typeConverts.get(from);
      if (map == null) {
         this.typeConverts.putIfAbsent(from, new ConcurrentHashMap<>());
         map = this.typeConverts.get(from);
      }

      return map.put(to, typeConvert);
   }

   public ObjectReader getObjectReader(long hashCode) {
      ObjectReaderProvider.ObjectReaderCachePair pair = readerCache;
      if (pair != null) {
         if (pair.hashCode == hashCode) {
            return pair.reader;
         }

         if (pair.missCount++ > 16) {
            readerCache = null;
         }
      }

      Long hashCodeObj = hashCode;
      ObjectReader objectReader = null;
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      if (tcl != null && tcl != FASTJSON2_CLASS_LOADER) {
         int tclHash = System.identityHashCode(tcl);
         ConcurrentHashMap<Long, ObjectReader> tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         if (tclHashCache != null) {
            objectReader = tclHashCache.get(hashCodeObj);
         }
      }

      if (objectReader == null) {
         objectReader = this.hashCache.get(hashCodeObj);
      }

      if (objectReader != null && readerCache == null) {
         readerCache = new ObjectReaderProvider.ObjectReaderCachePair(hashCode, objectReader);
      }

      return objectReader;
   }

   public ObjectReader getObjectReader(String typeName, Class<?> expectClass, long features) {
      Class<?> autoTypeClass = this.checkAutoType(typeName, expectClass, features);
      if (autoTypeClass == null) {
         return null;
      } else {
         boolean fieldBased = (features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader objectReader = this.getObjectReader(autoTypeClass, fieldBased);
         if (autoTypeClass != expectClass) {
            this.registerIfAbsent(Fnv.hashCode64(typeName), objectReader);
         }

         return objectReader;
      }
   }

   final void afterAutoType(String typeName, Class type) {
      if (this.autoTypeHandler != null) {
         this.autoTypeHandler.accept(type);
      }

      synchronized (this.autoTypeList) {
         this.autoTypeList.putIfAbsent(typeName, new Date());
      }
   }

   public Class<?> checkAutoType(String typeName, Class<?> expectClass, long features) {
      if (typeName != null && !typeName.isEmpty()) {
         if (this.autoTypeBeforeHandler != null) {
            Class<?> resolvedClass = this.autoTypeBeforeHandler.apply(typeName, expectClass, features);
            if (resolvedClass != null) {
               this.afterAutoType(typeName, resolvedClass);
               return resolvedClass;
            }
         }

         if (SAFE_MODE) {
            return null;
         } else {
            int typeNameLength = typeName.length();
            if (typeNameLength >= 192) {
               throw new JSONException("autoType is not support. " + typeName);
            } else {
               if (typeName.charAt(0) == '[') {
                  String componentTypeName = typeName.substring(1);
                  this.checkAutoType(componentTypeName, null, features);
               }

               if (expectClass != null && expectClass.getName().equals(typeName)) {
                  this.afterAutoType(typeName, expectClass);
                  return expectClass;
               } else {
                  boolean autoTypeSupport = (features & JSONReader.Feature.SupportAutoType.mask) != 0L;
                  if (autoTypeSupport) {
                     long hash = -3750763034362895579L;

                     for (int i = 0; i < typeNameLength; i++) {
                        char ch = typeName.charAt(i);
                        if (ch == '$') {
                           ch = '.';
                        }

                        hash ^= (long)ch;
                        hash *= 1099511628211L;
                        if (Arrays.binarySearch(this.acceptHashCodes, hash) >= 0) {
                           Class<?> clazz = TypeUtils.loadClass(typeName);
                           if (clazz != null) {
                              if (expectClass != null && !expectClass.isAssignableFrom(clazz)) {
                                 throw new JSONException("type not match. " + typeName + " -> " + expectClass.getName());
                              }

                              this.afterAutoType(typeName, clazz);
                              return clazz;
                           }
                        }
                     }
                  }

                  if (!autoTypeSupport) {
                     long hash = -3750763034362895579L;

                     for (int i = 0; i < typeNameLength; i++) {
                        char chx = typeName.charAt(i);
                        if (chx == '$') {
                           chx = '.';
                        }

                        hash ^= (long)chx;
                        hash *= 1099511628211L;
                        if (Arrays.binarySearch(this.acceptHashCodes, hash) >= 0) {
                           Class<?> clazz = TypeUtils.loadClass(typeName);
                           if (clazz != null && expectClass != null && !expectClass.isAssignableFrom(clazz)) {
                              throw new JSONException("type not match. " + typeName + " -> " + expectClass.getName());
                           }

                           this.afterAutoType(typeName, clazz);
                           return clazz;
                        }
                     }
                  }

                  if (!autoTypeSupport) {
                     return null;
                  } else {
                     Class<?> clazz = TypeUtils.getMapping(typeName);
                     if (clazz == null) {
                        clazz = TypeUtils.loadClass(typeName);
                        if (clazz != null) {
                           if (ClassLoader.class.isAssignableFrom(clazz) || JDKUtils.isSQLDataSourceOrRowSet(clazz)) {
                              throw new JSONException("autoType is not support. " + typeName);
                           }

                           if (expectClass != null) {
                              if (expectClass.isAssignableFrom(clazz)) {
                                 this.afterAutoType(typeName, clazz);
                                 return clazz;
                              }

                              if ((features & JSONReader.Feature.IgnoreAutoTypeNotMatch.mask) != 0L) {
                                 return expectClass;
                              }

                              throw new JSONException("type not match. " + typeName + " -> " + expectClass.getName());
                           }
                        }

                        this.afterAutoType(typeName, clazz);
                        return clazz;
                     } else if (expectClass != null && expectClass != Object.class && clazz != HashMap.class && !expectClass.isAssignableFrom(clazz)) {
                        throw new JSONException("type not match. " + typeName + " -> " + expectClass.getName());
                     } else {
                        this.afterAutoType(typeName, clazz);
                        return clazz;
                     }
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   public List<ObjectReaderModule> getModules() {
      return this.modules;
   }

   public void getBeanInfo(BeanInfo beanInfo, Class objectClass) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectReaderModule module = this.modules.get(i);
         module.getBeanInfo(beanInfo, objectClass);
      }
   }

   public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Field field) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectReaderModule module = this.modules.get(i);
         module.getFieldInfo(fieldInfo, objectClass, field);
      }
   }

   public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Constructor constructor, int paramIndex, Parameter parameter) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectReaderAnnotationProcessor annotationProcessor = this.modules.get(i).getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getFieldInfo(fieldInfo, objectClass, constructor, paramIndex, parameter);
         }
      }
   }

   public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Method method, int paramIndex, Parameter parameter) {
      for (int i = 0; i < this.modules.size(); i++) {
         ObjectReaderAnnotationProcessor annotationProcessor = this.modules.get(i).getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getFieldInfo(fieldInfo, objectClass, method, paramIndex, parameter);
         }
      }
   }

   public ObjectReader getObjectReader(Type objectType) {
      return this.getObjectReader(objectType, false);
   }

   public Function<Consumer, ByteArrayValueConsumer> createValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return this.creator.createByteArrayValueConsumerCreator(objectClass, fieldReaderArray);
   }

   public Function<Consumer, CharArrayValueConsumer> createCharArrayValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return this.creator.createCharArrayValueConsumerCreator(objectClass, fieldReaderArray);
   }

   public ObjectReader getObjectReader(Type objectType, boolean fieldBased) {
      if (objectType == null) {
         objectType = Object.class;
      }

      ObjectReader objectReader = fieldBased ? this.cacheFieldBased.get(objectType) : this.cache.get(objectType);
      if (objectReader == null && objectType instanceof WildcardType) {
         Type[] upperBounds = ((WildcardType)objectType).getUpperBounds();
         if (upperBounds.length == 1) {
            Type upperBoundType = upperBounds[0];
            objectReader = fieldBased ? this.cacheFieldBased.get(upperBoundType) : this.cache.get(upperBoundType);
         }
      }

      return objectReader != null ? objectReader : this.getObjectReaderInternal(objectType, fieldBased);
   }

   private ObjectReader getObjectReaderInternal(Type objectType, boolean fieldBased) {
      ObjectReader objectReader = null;

      for (ObjectReaderModule module : this.modules) {
         objectReader = module.getObjectReader(this, objectType);
         if (objectReader != null) {
            ObjectReader previous = fieldBased ? this.cacheFieldBased.putIfAbsent(objectType, objectReader) : this.cache.putIfAbsent(objectType, objectReader);
            if (previous != null) {
               objectReader = previous;
            }

            return objectReader;
         }
      }

      if (objectType instanceof TypeVariable) {
         Type[] bounds = ((TypeVariable)objectType).getBounds();
         if (bounds.length > 0) {
            Type bound = bounds[0];
            if (bound instanceof Class) {
               ObjectReader boundObjectReader = this.getObjectReader(bound, fieldBased);
               if (boundObjectReader != null) {
                  ObjectReader previous = this.getPreviousObjectReader(fieldBased, objectType, boundObjectReader);
                  if (previous != null) {
                     boundObjectReader = previous;
                  }

                  return boundObjectReader;
               }
            }
         }
      }

      if (objectType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)objectType;
         Type rawType = parameterizedType.getRawType();
         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         if (rawType instanceof Class) {
            Class rawClass = (Class)rawType;
            boolean generic = false;

            for (Class clazz = rawClass; clazz != Object.class; clazz = clazz.getSuperclass()) {
               if (clazz.getTypeParameters().length > 0) {
                  generic = true;
                  break;
               }
            }

            if (typeArguments.length == 0 || !generic) {
               ObjectReader rawClassReader = this.getObjectReader(rawClass, fieldBased);
               if (rawClassReader != null) {
                  ObjectReader previous = this.getPreviousObjectReader(fieldBased, objectType, rawClassReader);
                  if (previous != null) {
                     rawClassReader = previous;
                  }

                  return rawClassReader;
               }
            }

            if (typeArguments.length == 1 && ArrayList.class.isAssignableFrom(rawClass)) {
               return ObjectReaderImplList.of(objectType, rawClass, 0L);
            }

            if (typeArguments.length == 2 && Map.class.isAssignableFrom(rawClass)) {
               return ObjectReaderImplMap.of(objectType, (Class)rawType, 0L);
            }
         }
      }

      Class<?> objectClass = TypeUtils.getMapping(objectType);
      String className = objectClass.getName();
      if (!fieldBased && "com.google.common.collect.ArrayListMultimap".equals(className)) {
         objectReader = ObjectReaderImplMap.of(null, objectClass, 0L);
      }

      if (objectReader == null) {
         ObjectReaderCreator creator = this.getCreator();
         objectReader = creator.createObjectReader(objectClass, objectType, fieldBased, this);
      }

      ObjectReader previous = this.getPreviousObjectReader(fieldBased, objectType, objectReader);
      if (previous != null) {
         objectReader = previous;
      }

      return objectReader;
   }

   private ObjectReader getPreviousObjectReader(boolean fieldBased, Type objectType, ObjectReader boundObjectReader) {
      return fieldBased ? this.cacheFieldBased.putIfAbsent(objectType, boundObjectReader) : this.cache.putIfAbsent(objectType, boundObjectReader);
   }

   public JSONReader.AutoTypeBeforeHandler getAutoTypeBeforeHandler() {
      return this.autoTypeBeforeHandler;
   }

   public Map<String, Date> getAutoTypeList() {
      return this.autoTypeList;
   }

   public void setAutoTypeBeforeHandler(JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler) {
      this.autoTypeBeforeHandler = autoTypeBeforeHandler;
   }

   public void getFieldInfo(FieldInfo fieldInfo, Class objectClass, Method method) {
      for (ObjectReaderModule module : this.modules) {
         ObjectReaderAnnotationProcessor annotationProcessor = module.getAnnotationProcessor();
         if (annotationProcessor != null) {
            annotationProcessor.getFieldInfo(fieldInfo, objectClass, method);
         }
      }

      if (fieldInfo.fieldName == null && fieldInfo.alternateNames == null) {
         String methodName = method.getName();
         if (methodName.startsWith("set")) {
            String findName = methodName.substring(3);
            Field field = BeanUtils.getDeclaredField(objectClass, findName);
            if (field != null) {
               fieldInfo.alternateNames = new String[]{findName};
            }
         }
      }
   }

   public <T> Supplier<T> createObjectCreator(Class<T> objectClass, long readerFeatures) {
      boolean fieldBased = (readerFeatures & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = fieldBased ? this.cacheFieldBased.get(objectClass) : this.cache.get(objectClass);
      if (objectReader != null) {
         return () -> (T)objectReader.createInstance(0L);
      } else {
         Constructor constructor = BeanUtils.getDefaultConstructor(objectClass, false);
         if (constructor == null) {
            throw new JSONException("default constructor not found : " + objectClass.getName());
         } else {
            return LambdaMiscCodec.createSupplier(constructor);
         }
      }
   }

   public FieldReader createFieldReader(Class objectClass, String fieldName, long readerFeatures) {
      boolean fieldBased = (readerFeatures & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = fieldBased ? this.cacheFieldBased.get(objectClass) : this.cache.get(objectClass);
      if (objectReader != null) {
         return objectReader.getFieldReader(fieldName);
      } else {
         AtomicReference<Field> fieldRef = new AtomicReference<>();
         long nameHashLCase = Fnv.hashCode64LCase(fieldName);
         BeanUtils.fields(objectClass, fieldx -> {
            if (nameHashLCase == Fnv.hashCode64LCase(fieldx.getName())) {
               fieldRef.set(fieldx);
            }
         });
         Field field = fieldRef.get();
         if (field != null) {
            return this.creator.createFieldReader(fieldName, null, field.getType(), field);
         } else {
            AtomicReference<Method> methodRef = new AtomicReference<>();
            BeanUtils.setters(objectClass, methodx -> {
               String setterName = BeanUtils.setterName(methodx.getName(), PropertyNamingStrategy.CamelCase.name());
               if (nameHashLCase == Fnv.hashCode64LCase(setterName)) {
                  methodRef.set(methodx);
               }
            });
            Method method = methodRef.get();
            if (method != null) {
               Class<?>[] params = method.getParameterTypes();
               Class fieldClass = params[0];
               return this.creator.createFieldReaderMethod(objectClass, fieldName, null, fieldClass, fieldClass, method);
            } else {
               return null;
            }
         }
      }
   }

   public <T> ObjectReader<T> createObjectReader(String[] names, Type[] types, Supplier<T> supplier, FieldConsumer<T> c) {
      return this.createObjectReader(names, types, null, supplier, c);
   }

   public <T> ObjectReader<T> createObjectReader(String[] names, Type[] types, long[] features, Supplier<T> supplier, FieldConsumer<T> c) {
      FieldReader[] fieldReaders = new FieldReader[names.length];

      for (int i = 0; i < names.length; i++) {
         Type fieldType = types[i];
         Class fieldClass = TypeUtils.getClass(fieldType);
         long feature = features != null && i < features.length ? features[i] : 0L;
         fieldReaders[i] = this.creator.createFieldReader(names[i], fieldType, fieldClass, feature, new FieldBiConsumer<>(i, c));
      }

      return this.creator.createObjectReader(null, supplier, fieldReaders);
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

   public boolean isDisableSmartMatch() {
      return this.disableSmartMatch;
   }

   public void setDisableSmartMatch(boolean disableSmartMatch) {
      this.disableSmartMatch = disableSmartMatch;
   }

   public PropertyNamingStrategy getNamingStrategy() {
      return this.namingStrategy;
   }

   public void setNamingStrategy(PropertyNamingStrategy namingStrategy) {
      this.namingStrategy = namingStrategy;
   }

   static {
      String property = System.getProperty("fastjson2.parser.deny");
      if (property == null) {
         property = JSONFactory.getProperty("fastjson2.parser.deny");
      }

      if (property != null && property.length() > 0) {
         DENYS = property.split(",");
      } else {
         DENYS = new String[0];
      }

      property = System.getProperty("fastjson2.autoTypeAccept");
      if (property == null) {
         property = JSONFactory.getProperty("fastjson2.autoTypeAccept");
      }

      if (property != null && property.length() > 0) {
         AUTO_TYPE_ACCEPT_LIST = property.split(",");
      } else {
         AUTO_TYPE_ACCEPT_LIST = new String[0];
      }

      property = System.getProperty("fastjson2.autoTypeBeforeHandler");
      if (property == null || property.isEmpty()) {
         property = JSONFactory.getProperty("fastjson2.autoTypeBeforeHandler");
      }

      if (property != null) {
         property = property.trim();
      }

      if (property != null && !property.isEmpty()) {
         Class handlerClass = TypeUtils.loadClass(property);
         if (handlerClass != null) {
            try {
               DEFAULT_AUTO_TYPE_BEFORE_HANDLER = (JSONReader.AutoTypeBeforeHandler)handlerClass.newInstance();
            } catch (Exception var4) {
               DEFAULT_AUTO_TYPE_HANDLER_INIT_ERROR = true;
            }
         }
      }

      property = System.getProperty("fastjson2.autoTypeHandler");
      if (property == null || property.isEmpty()) {
         property = JSONFactory.getProperty("fastjson2.autoTypeHandler");
      }

      if (property != null) {
         property = property.trim();
      }

      if (property != null && !property.isEmpty()) {
         Class handlerClass = TypeUtils.loadClass(property);
         if (handlerClass != null) {
            try {
               DEFAULT_AUTO_TYPE_HANDLER = (Consumer<Class>)handlerClass.newInstance();
            } catch (Exception var3) {
               DEFAULT_AUTO_TYPE_HANDLER_INIT_ERROR = true;
            }
         }
      }

      property = System.getProperty("fastjson.parser.safeMode");
      if (property == null || property.isEmpty()) {
         property = JSONFactory.getProperty("fastjson.parser.safeMode");
      }

      if (property == null || property.isEmpty()) {
         property = System.getProperty("fastjson2.parser.safeMode");
      }

      if (property == null || property.isEmpty()) {
         property = JSONFactory.getProperty("fastjson2.parser.safeMode");
      }

      if (property != null) {
         property = property.trim();
      }

      SAFE_MODE = "true".equals(property);
   }

   private static final class LRUAutoTypeCache extends LinkedHashMap<String, Date> {
      private final int maxSize;

      public LRUAutoTypeCache(int maxSize) {
         super(16, 0.75F, false);
         this.maxSize = maxSize;
      }

      @Override
      protected boolean removeEldestEntry(Entry<String, Date> eldest) {
         return this.size() > this.maxSize;
      }
   }

   private static final class ObjectReaderCachePair {
      final long hashCode;
      final ObjectReader reader;
      volatile int missCount;

      public ObjectReaderCachePair(long hashCode, ObjectReader reader) {
         this.hashCode = hashCode;
         this.reader = reader;
      }
   }
}
