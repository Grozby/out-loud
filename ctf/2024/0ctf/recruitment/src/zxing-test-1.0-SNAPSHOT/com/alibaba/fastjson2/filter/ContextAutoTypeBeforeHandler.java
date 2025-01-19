package com.alibaba.fastjson2.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

public class ContextAutoTypeBeforeHandler implements JSONReader.AutoTypeBeforeHandler {
   final long[] acceptHashCodes;
   final ConcurrentMap<Integer, ConcurrentHashMap<Long, Class>> tclHashCaches = new ConcurrentHashMap<>();
   final Map<Long, Class> classCache = new ConcurrentHashMap<>(16, 0.75F, 1);

   public ContextAutoTypeBeforeHandler(Class... types) {
      this(false, types);
   }

   public ContextAutoTypeBeforeHandler(boolean includeBasic, Class... types) {
      this(includeBasic, names(Arrays.asList(types)));
   }

   public ContextAutoTypeBeforeHandler(String... acceptNames) {
      this(false, acceptNames);
   }

   public ContextAutoTypeBeforeHandler(boolean includeBasic) {
      this(includeBasic);
   }

   static String[] names(Collection<Class> types) {
      Set<String> nameSet = new HashSet<>();

      for (Class type : types) {
         if (type != null) {
            String name = TypeUtils.getTypeName(type);
            nameSet.add(name);
         }
      }

      return nameSet.toArray(new String[nameSet.size()]);
   }

   public ContextAutoTypeBeforeHandler(boolean includeBasic, String... acceptNames) {
      Set<String> nameSet = new HashSet<>();
      if (includeBasic) {
         Class[] basicTypes = new Class[]{
            Object.class,
            byte.class,
            Byte.class,
            short.class,
            Short.class,
            int.class,
            Integer.class,
            long.class,
            Long.class,
            float.class,
            Float.class,
            double.class,
            Double.class,
            Number.class,
            BigInteger.class,
            BigDecimal.class,
            AtomicInteger.class,
            AtomicLong.class,
            AtomicBoolean.class,
            AtomicIntegerArray.class,
            AtomicLongArray.class,
            AtomicReference.class,
            boolean.class,
            Boolean.class,
            char.class,
            Character.class,
            String.class,
            UUID.class,
            Currency.class,
            BitSet.class,
            EnumSet.class,
            EnumSet.noneOf(TimeUnit.class).getClass(),
            Date.class,
            Calendar.class,
            LocalTime.class,
            LocalDate.class,
            LocalDateTime.class,
            Instant.class,
            SimpleDateFormat.class,
            DateTimeFormatter.class,
            TimeUnit.class,
            Set.class,
            HashSet.class,
            LinkedHashSet.class,
            TreeSet.class,
            List.class,
            ArrayList.class,
            LinkedList.class,
            ConcurrentLinkedQueue.class,
            ConcurrentSkipListSet.class,
            CopyOnWriteArrayList.class,
            Collections.emptyList().getClass(),
            Collections.emptyMap().getClass(),
            TypeUtils.CLASS_SINGLE_SET,
            TypeUtils.CLASS_SINGLE_LIST,
            TypeUtils.CLASS_UNMODIFIABLE_COLLECTION,
            TypeUtils.CLASS_UNMODIFIABLE_LIST,
            TypeUtils.CLASS_UNMODIFIABLE_SET,
            TypeUtils.CLASS_UNMODIFIABLE_SORTED_SET,
            TypeUtils.CLASS_UNMODIFIABLE_NAVIGABLE_SET,
            Collections.unmodifiableMap(new HashMap()).getClass(),
            Collections.unmodifiableNavigableMap(new TreeMap()).getClass(),
            Collections.unmodifiableSortedMap(new TreeMap()).getClass(),
            Arrays.asList().getClass(),
            Map.class,
            HashMap.class,
            Hashtable.class,
            TreeMap.class,
            LinkedHashMap.class,
            WeakHashMap.class,
            IdentityHashMap.class,
            ConcurrentMap.class,
            ConcurrentHashMap.class,
            ConcurrentSkipListMap.class,
            Exception.class,
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
            RuntimeException.class,
            SecurityException.class,
            StackOverflowError.class,
            StringIndexOutOfBoundsException.class,
            TypeNotPresentException.class,
            VerifyError.class,
            StackTraceElement.class
         };

         for (int i = 0; i < basicTypes.length; i++) {
            String name = TypeUtils.getTypeName(basicTypes[i]);
            nameSet.add(name);
         }

         String[] basicTypeNames = new String[]{"javax.validation.ValidationException", "javax.validation.NoProviderFoundException"};
         nameSet.addAll(Arrays.asList(basicTypeNames));
      }

      for (int i = 0; i < acceptNames.length; i++) {
         String name = acceptNames[i];
         if (name != null && !name.isEmpty()) {
            Class mapping = TypeUtils.getMapping(name);
            if (mapping != null) {
               name = TypeUtils.getTypeName(mapping);
            }

            nameSet.add(name);
         }
      }

      long[] array = new long[nameSet.size()];
      int index = 0;

      for (String name : nameSet) {
         long hashCode = -3750763034362895579L;

         for (int j = 0; j < name.length(); j++) {
            char ch = name.charAt(j);
            if (ch == '$') {
               ch = '.';
            }

            hashCode ^= (long)ch;
            hashCode *= 1099511628211L;
         }

         array[index++] = hashCode;
      }

      if (index != array.length) {
         array = Arrays.copyOf(array, index);
      }

      Arrays.sort(array);
      this.acceptHashCodes = array;
   }

   @Override
   public Class<?> apply(long typeNameHash, Class<?> expectClass, long features) {
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      if (tcl != null && tcl != JSON.class.getClassLoader()) {
         int tclHash = System.identityHashCode(tcl);
         ConcurrentHashMap<Long, Class> tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         if (tclHashCache != null) {
            return tclHashCache.get(typeNameHash);
         }
      }

      return this.classCache.get(typeNameHash);
   }

   @Override
   public Class<?> apply(String typeName, Class<?> expectClass, long features) {
      if ("O".equals(typeName)) {
         typeName = "Object";
      }

      long hash = -3750763034362895579L;
      int i = 0;

      for (int typeNameLength = typeName.length(); i < typeNameLength; i++) {
         char ch = typeName.charAt(i);
         if (ch == '$') {
            ch = '.';
         }

         hash ^= (long)ch;
         hash *= 1099511628211L;
         if (Arrays.binarySearch(this.acceptHashCodes, hash) >= 0) {
            long typeNameHash = Fnv.hashCode64(typeName);
            Class clazz = this.apply(typeNameHash, expectClass, features);
            if (clazz == null) {
               clazz = TypeUtils.loadClass(typeName);
               if (clazz != null) {
                  Class origin = this.putCacheIfAbsent(typeNameHash, clazz);
                  if (origin != null) {
                     clazz = origin;
                  }
               }
            }

            if (clazz != null) {
               return clazz;
            }
         }
      }

      long typeNameHashx = Fnv.hashCode64(typeName);
      if (typeName.length() > 0 && typeName.charAt(0) == '[') {
         Class clazzx = this.apply(typeNameHashx, expectClass, features);
         if (clazzx != null) {
            return clazzx;
         }

         String itemTypeName = typeName.substring(1);
         Class itemExpectClass = null;
         if (expectClass != null) {
            itemExpectClass = expectClass.getComponentType();
         }

         Class itemType = this.apply(itemTypeName, itemExpectClass, features);
         if (itemType != null) {
            Class arrayType;
            if (itemType == itemExpectClass) {
               arrayType = expectClass;
            } else {
               arrayType = TypeUtils.getArrayClass(itemType);
            }

            Class origin = this.putCacheIfAbsent(typeNameHashx, arrayType);
            if (origin != null) {
               arrayType = origin;
            }

            return arrayType;
         }
      }

      Class mapping = TypeUtils.getMapping(typeName);
      if (mapping != null) {
         String mappingTypeName = TypeUtils.getTypeName(mapping);
         if (!typeName.equals(mappingTypeName)) {
            Class<?> mappingClass = this.apply(mappingTypeName, expectClass, features);
            if (mappingClass != null) {
               this.putCacheIfAbsent(typeNameHashx, mappingClass);
            }

            return mappingClass;
         }
      }

      return null;
   }

   private Class putCacheIfAbsent(long typeNameHash, Class type) {
      ClassLoader tcl = Thread.currentThread().getContextClassLoader();
      if (tcl != null && tcl != JSON.class.getClassLoader()) {
         int tclHash = System.identityHashCode(tcl);
         ConcurrentHashMap<Long, Class> tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         if (tclHashCache == null) {
            this.tclHashCaches.putIfAbsent(tclHash, new ConcurrentHashMap<>());
            tclHashCache = this.tclHashCaches.get(Integer.valueOf(tclHash));
         }

         return tclHashCache.putIfAbsent(typeNameHash, type);
      } else {
         return this.classCache.putIfAbsent(typeNameHash, type);
      }
   }
}
