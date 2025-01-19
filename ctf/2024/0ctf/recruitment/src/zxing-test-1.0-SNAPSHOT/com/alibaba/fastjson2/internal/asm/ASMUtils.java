package com.alibaba.fastjson2.internal.asm;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONPathCompilerReflect;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.function.ObjBoolConsumer;
import com.alibaba.fastjson2.function.ObjByteConsumer;
import com.alibaba.fastjson2.function.ObjCharConsumer;
import com.alibaba.fastjson2.function.ObjFloatConsumer;
import com.alibaba.fastjson2.function.ObjShortConsumer;
import com.alibaba.fastjson2.reader.ByteArrayValueConsumer;
import com.alibaba.fastjson2.reader.CharArrayValueConsumer;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReader1;
import com.alibaba.fastjson2.reader.ObjectReader10;
import com.alibaba.fastjson2.reader.ObjectReader11;
import com.alibaba.fastjson2.reader.ObjectReader12;
import com.alibaba.fastjson2.reader.ObjectReader2;
import com.alibaba.fastjson2.reader.ObjectReader3;
import com.alibaba.fastjson2.reader.ObjectReader4;
import com.alibaba.fastjson2.reader.ObjectReader5;
import com.alibaba.fastjson2.reader.ObjectReader6;
import com.alibaba.fastjson2.reader.ObjectReader7;
import com.alibaba.fastjson2.reader.ObjectReader8;
import com.alibaba.fastjson2.reader.ObjectReader9;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.ParameterizedTypeImpl;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriter1;
import com.alibaba.fastjson2.writer.ObjectWriter10;
import com.alibaba.fastjson2.writer.ObjectWriter11;
import com.alibaba.fastjson2.writer.ObjectWriter12;
import com.alibaba.fastjson2.writer.ObjectWriter2;
import com.alibaba.fastjson2.writer.ObjectWriter3;
import com.alibaba.fastjson2.writer.ObjectWriter4;
import com.alibaba.fastjson2.writer.ObjectWriter5;
import com.alibaba.fastjson2.writer.ObjectWriter6;
import com.alibaba.fastjson2.writer.ObjectWriter7;
import com.alibaba.fastjson2.writer.ObjectWriter8;
import com.alibaba.fastjson2.writer.ObjectWriter9;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

public class ASMUtils {
   public static final String TYPE_UNSAFE_UTILS = JDKUtils.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_ADAPTER = ObjectWriterAdapter.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_1 = ObjectWriter1.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_2 = ObjectWriter2.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_3 = ObjectWriter3.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_4 = ObjectWriter4.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_5 = ObjectWriter5.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_6 = ObjectWriter6.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_7 = ObjectWriter7.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_8 = ObjectWriter8.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_9 = ObjectWriter9.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_10 = ObjectWriter10.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_11 = ObjectWriter11.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER_12 = ObjectWriter12.class.getName().replace('.', '/');
   public static final String TYPE_FIELD_READE = FieldReader.class.getName().replace('.', '/');
   public static final String TYPE_JSON_READER = JSONReader.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER = ObjectReader.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_ADAPTER = ObjectReaderAdapter.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_1 = ObjectReader1.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_2 = ObjectReader2.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_3 = ObjectReader3.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_4 = ObjectReader4.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_5 = ObjectReader5.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_6 = ObjectReader6.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_7 = ObjectReader7.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_8 = ObjectReader8.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_9 = ObjectReader9.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_10 = ObjectReader10.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_11 = ObjectReader11.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_READER_12 = ObjectReader12.class.getName().replace('.', '/');
   public static final String TYPE_BYTE_ARRAY_VALUE_CONSUMER = ByteArrayValueConsumer.class.getName().replace('.', '/');
   public static final String TYPE_CHAR_ARRAY_VALUE_CONSUMER = CharArrayValueConsumer.class.getName().replace('.', '/');
   public static final String TYPE_TYPE_UTILS = TypeUtils.class.getName().replace('.', '/');
   public static final String TYPE_DATE_UTILS = DateUtils.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT_WRITER = ObjectWriter.class.getName().replace('.', '/');
   public static final String TYPE_JSON_WRITER = JSONWriter.class.getName().replace('.', '/');
   public static final String TYPE_FIELD_WRITER = com.alibaba.fastjson2.writer.FieldWriter.class.getName().replace('.', '/');
   public static final String TYPE_OBJECT = "java/lang/Object";
   public static final String DESC_FIELD_WRITER = 'L' + com.alibaba.fastjson2.writer.FieldWriter.class.getName().replace('.', '/') + ';';
   public static final String DESC_FIELD_WRITER_ARRAY = "[" + DESC_FIELD_WRITER;
   public static final String DESC_FIELD_READER = 'L' + FieldReader.class.getName().replace('.', '/') + ';';
   public static final String DESC_FIELD_READER_ARRAY = "[" + DESC_FIELD_READER;
   public static final String DESC_JSON_READER = 'L' + TYPE_JSON_READER + ';';
   public static final String DESC_JSON_WRITER = 'L' + TYPE_JSON_WRITER + ';';
   public static final String DESC_OBJECT_READER = 'L' + TYPE_OBJECT_READER + ';';
   public static final String DESC_OBJECT_WRITER = 'L' + TYPE_OBJECT_WRITER + ';';
   public static final String DESC_SUPPLIER = "Ljava/util/function/Supplier;";
   public static final String DESC_JSONSCHEMA = 'L' + JSONSchema.class.getName().replace('.', '/') + ';';
   static final Map<ASMUtils.MethodInfo, String[]> paramMapping = new HashMap<>();
   static final Map<Class, String> descMapping = new HashMap<>();
   static final Map<Class, String> typeMapping = new HashMap<>();
   static final AtomicReference<char[]> descCacheRef;

   public static String type(Class<?> clazz) {
      String type = typeMapping.get(clazz);
      if (type != null) {
         return type;
      } else {
         return clazz.isArray() ? "[" + desc(clazz.getComponentType()) : clazz.getName().replace('.', '/');
      }
   }

   public static String desc(Class<?> clazz) {
      String desc = descMapping.get(clazz);
      if (desc != null) {
         return desc;
      } else if (clazz.isArray()) {
         Class<?> componentType = clazz.getComponentType();
         return "[" + desc(componentType);
      } else {
         String className = clazz.getName();
         char[] chars = descCacheRef.getAndSet(null);
         if (chars == null) {
            chars = new char[512];
         }

         chars[0] = 'L';
         className.getChars(0, className.length(), chars, 1);

         for (int i = 1; i < chars.length; i++) {
            if (chars[i] == '.') {
               chars[i] = '/';
            }
         }

         chars[className.length() + 1] = ';';
         String str = new String(chars, 0, className.length() + 2);
         descCacheRef.compareAndSet(null, chars);
         return str;
      }
   }

   public static String[] lookupParameterNames(AccessibleObject methodOrCtor) {
      if (methodOrCtor instanceof Constructor) {
         Constructor constructor = (Constructor)methodOrCtor;
         Class[] parameterTypes = constructor.getParameterTypes();
         Class declaringClass = constructor.getDeclaringClass();
         if (declaringClass == DateTimeParseException.class) {
            if (parameterTypes.length == 3) {
               if (parameterTypes[0] == String.class && parameterTypes[1] == CharSequence.class && parameterTypes[2] == int.class) {
                  return new String[]{"message", "parsedString", "errorIndex"};
               }
            } else if (parameterTypes.length == 4
               && parameterTypes[0] == String.class
               && parameterTypes[1] == CharSequence.class
               && parameterTypes[2] == int.class
               && parameterTypes[3] == Throwable.class) {
               return new String[]{"message", "parsedString", "errorIndex", "cause"};
            }
         }

         if (Throwable.class.isAssignableFrom(declaringClass)) {
            switch (parameterTypes.length) {
               case 1:
                  if (parameterTypes[0] == String.class) {
                     return new String[]{"message"};
                  }

                  if (Throwable.class.isAssignableFrom(parameterTypes[0])) {
                     return new String[]{"cause"};
                  }
                  break;
               case 2:
                  if (parameterTypes[0] == String.class && Throwable.class.isAssignableFrom(parameterTypes[1])) {
                     return new String[]{"message", "cause"};
                  }
            }
         }
      }

      int paramCount;
      Class<?>[] types;
      Class<?> declaringClassx;
      String name;
      if (methodOrCtor instanceof Method) {
         Method method = (Method)methodOrCtor;
         types = method.getParameterTypes();
         name = method.getName();
         declaringClassx = method.getDeclaringClass();
         paramCount = method.getParameterCount();
      } else {
         Constructor<?> constructorx = (Constructor<?>)methodOrCtor;
         types = constructorx.getParameterTypes();
         declaringClassx = constructorx.getDeclaringClass();
         name = "<init>";
         paramCount = constructorx.getParameterCount();
      }

      if (types.length == 0) {
         return new String[paramCount];
      } else {
         String[] paramNames = paramMapping.get(new ASMUtils.MethodInfo(declaringClassx.getName(), name, types));
         if (paramNames != null) {
            return paramNames;
         } else {
            ClassLoader classLoader = declaringClassx.getClassLoader();
            if (classLoader == null) {
               classLoader = ClassLoader.getSystemClassLoader();
            }

            String className = declaringClassx.getName();
            String resourceName = className.replace('.', '/') + ".class";
            InputStream is = classLoader.getResourceAsStream(resourceName);
            label161:
            if (is != null) {
               String[] var27;
               try {
                  ClassReader reader = new ClassReader(is);
                  TypeCollector visitor = new TypeCollector(name, types);
                  reader.accept(visitor);
                  paramNames = visitor.getParameterNamesForMethod();
                  if (paramNames != null && paramNames.length == paramCount - 1) {
                     Class<?> dd = declaringClassx.getDeclaringClass();
                     if (dd != null && dd.equals(types[0])) {
                        String[] strings = new String[paramCount];
                        strings[0] = "this$0";
                        System.arraycopy(paramNames, 0, strings, 1, paramNames.length);
                        paramNames = strings;
                     }
                  }

                  var27 = paramNames;
               } catch (ArrayIndexOutOfBoundsException | IOException var17) {
                  break label161;
               } finally {
                  IOUtils.close(is);
               }

               return var27;
            }

            paramNames = new String[paramCount];
            int i;
            if (types[0] == declaringClassx.getDeclaringClass() && !Modifier.isStatic(declaringClassx.getModifiers())) {
               paramNames[0] = "this.$0";
               i = 1;
            } else {
               i = 0;
            }

            while (i < paramNames.length) {
               paramNames[i] = "arg" + i;
               i++;
            }

            return paramNames;
         }
      }
   }

   static {
      paramMapping.put(
         new ASMUtils.MethodInfo(
            ParameterizedTypeImpl.class.getName(), "<init>", new String[]{"[Ljava.lang.reflect.Type;", "java.lang.reflect.Type", "java.lang.reflect.Type"}
         ),
         new String[]{"actualTypeArguments", "ownerType", "rawType"}
      );
      paramMapping.put(
         new ASMUtils.MethodInfo("org.apache.commons.lang3.tuple.Triple", "of", new String[]{"java.lang.Object", "java.lang.Object", "java.lang.Object"}),
         new String[]{"left", "middle", "right"}
      );
      paramMapping.put(
         new ASMUtils.MethodInfo(
            "org.apache.commons.lang3.tuple.MutableTriple", "<init>", new String[]{"java.lang.Object", "java.lang.Object", "java.lang.Object"}
         ),
         new String[]{"left", "middle", "right"}
      );
      paramMapping.put(
         new ASMUtils.MethodInfo(
            "org.javamoney.moneta.Money", "<init>", new String[]{"java.math.BigDecimal", "javax.money.CurrencyUnit", "javax.money.MonetaryContext"}
         ),
         new String[]{"number", "currency", "monetaryContext"}
      );
      paramMapping.put(
         new ASMUtils.MethodInfo("org.javamoney.moneta.Money", "<init>", new String[]{"java.math.BigDecimal", "javax.money.CurrencyUnit"}),
         new String[]{"number", "currency"}
      );
      descMapping.put(int.class, "I");
      descMapping.put(void.class, "V");
      descMapping.put(boolean.class, "Z");
      descMapping.put(char.class, "C");
      descMapping.put(byte.class, "B");
      descMapping.put(short.class, "S");
      descMapping.put(float.class, "F");
      descMapping.put(long.class, "J");
      descMapping.put(double.class, "D");
      typeMapping.put(int.class, "I");
      typeMapping.put(void.class, "V");
      typeMapping.put(boolean.class, "Z");
      typeMapping.put(char.class, "C");
      typeMapping.put(byte.class, "B");
      typeMapping.put(short.class, "S");
      typeMapping.put(float.class, "F");
      typeMapping.put(long.class, "J");
      typeMapping.put(double.class, "D");
      Class[] classes = new Class[]{
         String.class,
         List.class,
         Collection.class,
         ObjectReader.class,
         ObjectReader1.class,
         ObjectReader2.class,
         ObjectReader3.class,
         ObjectReader4.class,
         ObjectReader5.class,
         ObjectReader6.class,
         ObjectReader7.class,
         ObjectReader8.class,
         ObjectReader9.class,
         ObjectReader10.class,
         ObjectReader11.class,
         ObjectReader12.class,
         ObjectReaderAdapter.class,
         FieldReader.class,
         JSONReader.class,
         ObjBoolConsumer.class,
         ObjCharConsumer.class,
         ObjByteConsumer.class,
         ObjShortConsumer.class,
         ObjIntConsumer.class,
         ObjLongConsumer.class,
         ObjFloatConsumer.class,
         ObjDoubleConsumer.class,
         BiConsumer.class,
         JDKUtils.class,
         ObjectWriterAdapter.class,
         ObjectWriter1.class,
         ObjectWriter2.class,
         ObjectWriter3.class,
         ObjectWriter4.class,
         ObjectWriter5.class,
         ObjectWriter6.class,
         ObjectWriter7.class,
         ObjectWriter8.class,
         ObjectWriter9.class,
         ObjectWriter10.class,
         ObjectWriter11.class,
         ObjectWriter12.class,
         com.alibaba.fastjson2.writer.FieldWriter.class,
         JSONPathCompilerReflect.SingleNamePathTyped.class,
         JSONWriter.Context.class,
         JSONB.class,
         JSONSchema.class,
         JSONType.class,
         Date.class,
         Supplier.class
      };

      for (Class objectType : classes) {
         String type = objectType.getName().replace('.', '/');
         typeMapping.put(objectType, type);
         String desc = 'L' + type + ';';
         descMapping.put(objectType, desc);
      }

      typeMapping.put(JSONWriter.class, TYPE_JSON_WRITER);
      descMapping.put(JSONWriter.class, DESC_JSON_WRITER);
      typeMapping.put(ObjectWriter.class, TYPE_OBJECT_WRITER);
      descMapping.put(ObjectWriter.class, DESC_OBJECT_WRITER);
      descMapping.put(com.alibaba.fastjson2.writer.FieldWriter[].class, DESC_FIELD_WRITER_ARRAY);
      descMapping.put(FieldReader[].class, DESC_FIELD_READER_ARRAY);
      descCacheRef = new AtomicReference<>();
   }

   static final class MethodInfo {
      final String className;
      final String methodName;
      final String[] paramTypeNames;
      int hash;

      public MethodInfo(String className, String methodName, String[] paramTypeNames) {
         this.className = className;
         this.methodName = methodName;
         this.paramTypeNames = paramTypeNames;
      }

      public MethodInfo(String className, String methodName, Class[] paramTypes) {
         this.className = className;
         this.methodName = methodName;
         this.paramTypeNames = new String[paramTypes.length];

         for (int i = 0; i < paramTypes.length; i++) {
            this.paramTypeNames[i] = paramTypes[i].getName();
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            ASMUtils.MethodInfo that = (ASMUtils.MethodInfo)o;
            return Objects.equals(this.className, that.className)
               && Objects.equals(this.methodName, that.methodName)
               && Arrays.equals((Object[])this.paramTypeNames, (Object[])that.paramTypeNames);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         if (this.hash == 0) {
            int result = Objects.hash(this.className, this.methodName);
            result = 31 * result + Arrays.hashCode((Object[])this.paramTypeNames);
            this.hash = result;
         }

         return this.hash;
      }
   }
}
