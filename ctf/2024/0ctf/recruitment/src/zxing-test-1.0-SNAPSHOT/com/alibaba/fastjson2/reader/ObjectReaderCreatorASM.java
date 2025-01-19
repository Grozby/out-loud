package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.function.FieldBiConsumer;
import com.alibaba.fastjson2.function.FieldConsumer;
import com.alibaba.fastjson2.function.ObjBoolConsumer;
import com.alibaba.fastjson2.function.ObjByteConsumer;
import com.alibaba.fastjson2.function.ObjCharConsumer;
import com.alibaba.fastjson2.function.ObjFloatConsumer;
import com.alibaba.fastjson2.function.ObjShortConsumer;
import com.alibaba.fastjson2.internal.CodeGenUtils;
import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.internal.asm.ClassWriter;
import com.alibaba.fastjson2.internal.asm.FieldWriter;
import com.alibaba.fastjson2.internal.asm.Label;
import com.alibaba.fastjson2.internal.asm.MethodWriter;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.DynamicClassLoader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

public class ObjectReaderCreatorASM extends ObjectReaderCreator {
   public static final ObjectReaderCreatorASM INSTANCE = new ObjectReaderCreatorASM(DynamicClassLoader.getInstance());
   protected static final AtomicLong seed = new AtomicLong();
   protected final DynamicClassLoader classLoader;
   static final String METHOD_DESC_GET_ITEM_OBJECT_READER = "(" + ASMUtils.DESC_JSON_READER + ")" + ASMUtils.DESC_OBJECT_READER;
   static final String METHOD_DESC_GET_OBJECT_READER_1 = "(" + ASMUtils.DESC_JSON_READER + ")" + ASMUtils.DESC_OBJECT_READER;
   static final String METHOD_DESC_INIT = "(Ljava/lang/Class;Ljava/util/function/Supplier;" + ASMUtils.DESC_FIELD_READER_ARRAY + ")V";
   static final String METHOD_DESC_ADAPTER_INIT = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;J"
      + ASMUtils.DESC_JSONSCHEMA
      + "Ljava/util/function/Supplier;"
      + "Ljava/util/function/Function;"
      + ASMUtils.DESC_FIELD_READER_ARRAY
      + ")V";
   static final String METHOD_DESC_READ_OBJECT = "(" + ASMUtils.DESC_JSON_READER + "Ljava/lang/reflect/Type;Ljava/lang/Object;J)Ljava/lang/Object;";
   static final String METHOD_DESC_GET_FIELD_READER = "(J)" + ASMUtils.DESC_FIELD_READER;
   static final String METHOD_DESC_READ_FIELD_VALUE = "(" + ASMUtils.DESC_JSON_READER + "Ljava/lang/Object;)V";
   static final String GET_FIELD_READER_UL = "(J" + ASMUtils.DESC_JSON_READER + "J)" + ASMUtils.DESC_FIELD_READER;
   static final String READ_FIELD_READER_UL = "(J" + ASMUtils.DESC_JSON_READER + "JLjava/lang/Object;)V";
   static final String METHOD_DESC_ADD_RESOLVE_TASK = "(" + ASMUtils.DESC_JSON_READER + "Ljava/lang/Object;Ljava/lang/String;)V";
   static final String METHOD_DESC_ADD_RESOLVE_TASK_2 = "(" + ASMUtils.DESC_JSON_READER + "Ljava/util/List;ILjava/lang/String;)V";
   static final String METHOD_DESC_CHECK_ARRAY_AUTO_TYPE = "(" + ASMUtils.DESC_JSON_READER + ")" + ASMUtils.DESC_OBJECT_READER;
   static final String METHOD_DESC_PROCESS_EXTRA = "(" + ASMUtils.DESC_JSON_READER + "Ljava/lang/Object;J)V";
   static final String METHOD_DESC_JSON_READER_CHECK_ARRAY_AUTO_TYPE = "(" + ASMUtils.DESC_JSON_READER + "J)" + ASMUtils.DESC_OBJECT_READER;
   static final String METHOD_DESC_READ_ARRAY_MAPPING_JSONB_OBJECT0 = "(" + ASMUtils.DESC_JSON_READER + "Ljava/lang/Object;I)V";
   static final int THIS = 0;
   static final String packageName;
   static final Map<Class, ObjectReaderCreatorASM.FieldReaderInfo> infos = new HashMap<>();
   static final String[] fieldItemObjectReader = new String[1024];

   static String fieldObjectReader(int i) {
      switch (i) {
         case 0:
            return "objectReader0";
         case 1:
            return "objectReader1";
         case 2:
            return "objectReader2";
         case 3:
            return "objectReader3";
         case 4:
            return "objectReader4";
         case 5:
            return "objectReader5";
         case 6:
            return "objectReader6";
         case 7:
            return "objectReader7";
         case 8:
            return "objectReader8";
         case 9:
            return "objectReader9";
         case 10:
            return "objectReader10";
         case 11:
            return "objectReader11";
         case 12:
            return "objectReader12";
         case 13:
            return "objectReader13";
         case 14:
            return "objectReader14";
         case 15:
            return "objectReader15";
         default:
            String base = "objectReader";
            int size = IOUtils.stringSize(i);
            char[] chars = new char[base.length() + size];
            base.getChars(0, base.length(), chars, 0);
            IOUtils.getChars(i, chars.length, chars);
            return new String(chars);
      }
   }

   static String fieldItemObjectReader(int i) {
      String fieldName = fieldItemObjectReader[i];
      if (fieldName != null) {
         return fieldName;
      } else {
         String base = "itemReader";
         int size = IOUtils.stringSize(i);
         char[] chars = new char[base.length() + size];
         base.getChars(0, base.length(), chars, 0);
         IOUtils.getChars(i, chars.length, chars);
         fieldItemObjectReader[i] = fieldName = new String(chars);
         return fieldName;
      }
   }

   public ObjectReaderCreatorASM(ClassLoader classLoader) {
      this.classLoader = classLoader instanceof DynamicClassLoader ? (DynamicClassLoader)classLoader : new DynamicClassLoader(classLoader);
   }

   @Override
   public <T> ObjectReader<T> createObjectReader(Class<T> objectClass, Type objectType, boolean fieldBased, ObjectReaderProvider provider) {
      boolean externalClass = objectClass != null && this.classLoader.isExternalClass(objectClass);
      int objectClassModifiers = objectClass.getModifiers();
      if (!Modifier.isAbstract(objectClassModifiers) && !Modifier.isInterface(objectClassModifiers)) {
         BeanInfo beanInfo = new BeanInfo(provider);
         provider.getBeanInfo(beanInfo, objectClass);
         if (externalClass || !Modifier.isPublic(objectClassModifiers)) {
            beanInfo.readerFeatures |= 18014398509481984L;
         }

         if (beanInfo.deserializer != null && ObjectReader.class.isAssignableFrom(beanInfo.deserializer)) {
            try {
               Constructor constructor = beanInfo.deserializer.getDeclaredConstructor();
               constructor.setAccessible(true);
               return (ObjectReader<T>)constructor.newInstance();
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException var15) {
               throw new JSONException("create deserializer error", var15);
            }
         } else {
            if (fieldBased && (objectClass.isInterface() || BeanUtils.isRecord(objectClass))) {
               fieldBased = false;
            }

            if (!Enum.class.isAssignableFrom(objectClass) || beanInfo.createMethod != null && beanInfo.createMethod.getParameterCount() != 1) {
               if (beanInfo.creatorConstructor != null || beanInfo.createMethod != null) {
                  return this.createObjectReaderWithCreator(objectClass, objectType, provider, beanInfo);
               } else if (beanInfo.builder != null) {
                  return this.createObjectReaderWithBuilder(objectClass, objectType, provider, beanInfo);
               } else if (Throwable.class.isAssignableFrom(objectClass) || BeanUtils.isExtendedMap(objectClass)) {
                  return super.createObjectReader(objectClass, objectType, fieldBased, provider);
               } else if (objectClass == Class.class) {
                  return ObjectReaderImplClass.INSTANCE;
               } else {
                  FieldReader[] fieldReaderArray = this.createFieldReaders(objectClass, objectType, beanInfo, fieldBased, provider);
                  boolean match = true;
                  if (!fieldBased) {
                     if (JDKUtils.JVM_VERSION >= 9 && objectClass == StackTraceElement.class) {
                        try {
                           Constructor<StackTraceElement> constructor = StackTraceElement.class
                              .getConstructor(String.class, String.class, String.class, String.class, String.class, String.class, int.class);
                           return this.createObjectReaderNoneDefaultConstructor(
                              constructor,
                              new String[]{"", "classLoaderName", "moduleName", "moduleVersion", "declaringClass", "methodName", "fileName", "lineNumber"}
                           );
                        } catch (SecurityException | NoSuchMethodException var17) {
                        }
                     }

                     for (FieldReader fieldReader : fieldReaderArray) {
                        if (fieldReader.isReadOnly() || fieldReader.isUnwrapped()) {
                           match = false;
                           break;
                        }

                        if ((fieldReader.features & 2251799813685248L) != 0L) {
                           match = false;
                           break;
                        }
                     }
                  }

                  if (beanInfo.autoTypeBeforeHandler != null) {
                     match = false;
                  }

                  if (match) {
                     for (FieldReader fieldReader : fieldReaderArray) {
                        if (fieldReader.defaultValue != null || fieldReader.schema != null) {
                           match = false;
                           break;
                        }

                        Class fieldClass = fieldReader.fieldClass;
                        if (!Modifier.isPublic(fieldClass.getModifiers())) {
                           match = false;
                           break;
                        }

                        if (fieldReader instanceof FieldReaderMapField && ((FieldReaderMapField)fieldReader).arrayToMapKey != null) {
                           match = false;
                           break;
                        }

                        if (fieldReader instanceof FieldReaderMapMethod && ((FieldReaderMapMethod)fieldReader).arrayToMapKey != null) {
                           match = false;
                           break;
                        }
                     }
                  }

                  if (match && (beanInfo.rootName != null || beanInfo.schema != null && !beanInfo.schema.isEmpty())) {
                     match = false;
                  }

                  if (!match) {
                     return super.createObjectReader(objectClass, objectType, fieldBased, provider);
                  } else {
                     Constructor defaultConstructor = null;
                     if (!Modifier.isInterface(objectClassModifiers) && !Modifier.isAbstract(objectClassModifiers)) {
                        Constructor constructor = BeanUtils.getDefaultConstructor(objectClass, true);
                        if (constructor != null) {
                           defaultConstructor = constructor;

                           try {
                              constructor.setAccessible(true);
                           } catch (SecurityException var16) {
                           }
                        }
                     }

                     if (beanInfo.seeAlso != null && beanInfo.seeAlso.length != 0) {
                        return this.createObjectReaderSeeAlso(
                           objectClass, beanInfo.typeKey, beanInfo.seeAlso, beanInfo.seeAlsoNames, beanInfo.seeAlsoDefault, fieldReaderArray
                        );
                     } else {
                        return (ObjectReader<T>)(!fieldBased && defaultConstructor == null
                           ? super.createObjectReader(objectClass, objectType, false, provider)
                           : this.jitObjectReader(
                              objectClass, objectType, fieldBased, externalClass, objectClassModifiers, beanInfo, null, fieldReaderArray, defaultConstructor
                           ));
                     }
                  }
               }
            } else {
               return this.createEnumReader(objectClass, beanInfo.createMethod, provider);
            }
         }
      } else {
         return super.createObjectReader(objectClass, objectType, fieldBased, provider);
      }
   }

   @Override
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
      if (objectClass == null && defaultCreator != null && buildFunction == null) {
         boolean allFunction = true;

         for (int i = 0; i < fieldReaders.length; i++) {
            FieldReader fieldReader = fieldReaders[i];
            if (fieldReader.getFunction() == null) {
               allFunction = false;
               break;
            }
         }

         if (allFunction) {
            BeanInfo beanInfo = new BeanInfo(JSONFactory.getDefaultObjectReaderProvider());
            return this.jitObjectReader(objectClass, objectClass, false, false, 0, beanInfo, defaultCreator, fieldReaders, null);
         }
      }

      return super.createObjectReader(objectClass, typeKey, rootName, features, schema, defaultCreator, buildFunction, fieldReaders);
   }

   private <T> ObjectReaderBean jitObjectReader(
      Class<T> objectClass,
      Type objectType,
      boolean fieldBased,
      boolean externalClass,
      int objectClassModifiers,
      BeanInfo beanInfo,
      Supplier<T> defaultCreator,
      FieldReader[] fieldReaderArray,
      Constructor defaultConstructor
   ) {
      ClassWriter cw = new ClassWriter(ex -> objectClass.getName().equals(ex) ? objectClass : null);
      ObjectReaderCreatorASM.ObjectWriteContext context = new ObjectReaderCreatorASM.ObjectWriteContext(
         beanInfo, objectClass, cw, externalClass, fieldReaderArray
      );
      String className = "ORG_" + seed.incrementAndGet() + "_" + fieldReaderArray.length + (objectClass == null ? "" : "_" + objectClass.getSimpleName());
      Package pkg = ObjectReaderCreatorASM.class.getPackage();
      String classNameType;
      String classNameFull;
      if (pkg != null) {
         String packageName = pkg.getName();
         int packageNameLength = packageName.length();
         int charsLength = packageNameLength + 1 + className.length();
         char[] chars = new char[charsLength];
         packageName.getChars(0, packageName.length(), chars, 0);
         chars[packageNameLength] = '.';
         className.getChars(0, className.length(), chars, packageNameLength + 1);
         classNameFull = new String(chars);
         chars[packageNameLength] = '/';

         for (int i = 0; i < packageNameLength; i++) {
            if (chars[i] == '.') {
               chars[i] = '/';
            }
         }

         classNameType = new String(chars);
      } else {
         classNameType = className;
         classNameFull = className;
      }

      boolean generatedFields = fieldReaderArray.length < 128;
      String objectReaderSuper;
      switch (fieldReaderArray.length) {
         case 1:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_1;
            break;
         case 2:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_2;
            break;
         case 3:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_3;
            break;
         case 4:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_4;
            break;
         case 5:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_5;
            break;
         case 6:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_6;
            break;
         case 7:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_7;
            break;
         case 8:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_8;
            break;
         case 9:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_9;
            break;
         case 10:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_10;
            break;
         case 11:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_11;
            break;
         case 12:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_12;
            break;
         default:
            objectReaderSuper = ASMUtils.TYPE_OBJECT_READER_ADAPTER;
      }

      if (generatedFields) {
         this.genFields(fieldReaderArray, cw, objectReaderSuper);
      }

      cw.visit(52, 49, classNameType, objectReaderSuper, new String[0]);
      int CLASS = 1;
      int SUPPLIER = 2;
      int FIELD_READER_ARRAY = 3;
      MethodWriter mw = cw.visitMethod(1, "<init>", METHOD_DESC_INIT, fieldReaderArray.length <= 12 ? 32 : 128);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      if (beanInfo.typeKey != null) {
         mw.visitLdcInsn(beanInfo.typeKey);
      } else {
         mw.visitInsn(1);
      }

      mw.visitInsn(1);
      mw.visitLdcInsn(beanInfo.readerFeatures);
      mw.visitInsn(1);
      mw.visitVarInsn(25, 2);
      mw.visitInsn(1);
      mw.visitVarInsn(25, 3);
      mw.visitMethodInsn(183, objectReaderSuper, "<init>", METHOD_DESC_ADAPTER_INIT, false);
      this.genInitFields(fieldReaderArray, classNameType, generatedFields, 0, 3, mw, objectReaderSuper);
      mw.visitInsn(177);
      mw.visitMaxs(3, 3);
      String TYPE_OBJECT = objectClass == null ? "java/lang/Object" : ASMUtils.type(objectClass);
      String methodName = fieldBased && defaultConstructor == null ? "createInstance0" : "createInstance";
      if (!fieldBased || defaultConstructor != null && Modifier.isPublic(defaultConstructor.getModifiers()) && Modifier.isPublic(objectClass.getModifiers())) {
         if (defaultConstructor != null && Modifier.isPublic(defaultConstructor.getModifiers()) && Modifier.isPublic(objectClass.getModifiers())) {
            MethodWriter mwx = cw.visitMethod(1, methodName, "(J)Ljava/lang/Object;", 32);
            newObject(mwx, TYPE_OBJECT, defaultConstructor);
            mwx.visitInsn(176);
            mwx.visitMaxs(3, 3);
         }
      } else {
         MethodWriter mwx = cw.visitMethod(1, methodName, "(J)Ljava/lang/Object;", 32);
         mwx.visitFieldInsn(178, ASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
         mwx.visitVarInsn(25, 0);
         mwx.visitFieldInsn(180, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "objectClass", "Ljava/lang/Class;");
         mwx.visitMethodInsn(182, "sun/misc/Unsafe", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
         mwx.visitInsn(176);
         mwx.visitMaxs(3, 3);
      }

      Supplier<T> supplier;
      if (defaultConstructor != null) {
         boolean publicObject = Modifier.isPublic(objectClassModifiers) && !this.classLoader.isExternalClass(objectClass);
         boolean jit = !publicObject || !Modifier.isPublic(defaultConstructor.getModifiers());
         supplier = this.createSupplier(defaultConstructor, jit);
      } else {
         supplier = defaultCreator;
      }

      if (generatedFields) {
         long readerFeatures = beanInfo.readerFeatures;
         if (fieldBased) {
            readerFeatures |= JSONReader.Feature.FieldBased.mask;
         }

         boolean disableArrayMapping = context.disableSupportArrayMapping();
         boolean disableJSONB = context.disableJSONB();
         ObjectReaderAdapter objectReaderAdapter = new ObjectReaderAdapter<>(
            objectClass, beanInfo.typeKey, beanInfo.typeName, readerFeatures, null, supplier, null, fieldReaderArray
         );
         if (!disableJSONB) {
            this.genMethodReadJSONBObject(context, defaultConstructor, readerFeatures, TYPE_OBJECT, fieldReaderArray, cw, classNameType, objectReaderAdapter);
            if (!disableArrayMapping) {
               this.genMethodReadJSONBObjectArrayMapping(
                  context, defaultConstructor, readerFeatures, TYPE_OBJECT, fieldReaderArray, cw, classNameType, objectReaderAdapter
               );
            }
         }

         this.genMethodReadObject(context, defaultConstructor, readerFeatures, TYPE_OBJECT, fieldReaderArray, cw, classNameType, objectReaderAdapter);
         if (objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_ADAPTER
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_1
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_2
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_3
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_4
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_5
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_6
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_7
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_8
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_9
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_10
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_11
            || objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_12) {
            this.genMethodGetFieldReader(fieldReaderArray, cw, classNameType, objectReaderAdapter);
            this.genMethodGetFieldReaderLCase(fieldReaderArray, cw, classNameType, objectReaderAdapter);
         }
      }

      byte[] code = cw.toByteArray();

      try {
         Class<?> readerClass = this.classLoader.defineClassPublic(classNameFull, code, 0, code.length);
         Constructor<?> constructor = readerClass.getConstructors()[0];
         return (ObjectReaderBean)constructor.newInstance(objectClass, supplier, fieldReaderArray);
      } catch (Throwable var25) {
         throw new JSONException("create objectReader error" + (objectType == null ? "" : ", objectType " + objectType.getTypeName()), var25);
      }
   }

   private static void newObject(MethodWriter mw, String TYPE_OBJECT, Constructor defaultConstructor) {
      mw.visitTypeInsn(187, TYPE_OBJECT);
      mw.visitInsn(89);
      if (defaultConstructor.getParameterCount() == 0) {
         mw.visitMethodInsn(183, TYPE_OBJECT, "<init>", "()V", false);
      } else {
         Class paramType = defaultConstructor.getParameterTypes()[0];
         mw.visitInsn(1);
         mw.visitMethodInsn(183, TYPE_OBJECT, "<init>", "(" + ASMUtils.desc(paramType) + ")V", false);
      }
   }

   private void genMethodGetFieldReader(FieldReader[] fieldReaderArray, ClassWriter cw, String classNameType, ObjectReaderAdapter objectReaderAdapter) {
      MethodWriter mw = cw.visitMethod(1, "getFieldReader", "(J)" + ASMUtils.DESC_FIELD_READER, 512);
      int HASH_CODE_64 = 1;
      int HASH_CODE_32 = 3;
      Label rtnlt = new Label();
      if (fieldReaderArray.length > 6) {
         Map<Integer, List<Long>> map = new TreeMap<>();

         for (int i = 0; i < objectReaderAdapter.hashCodes.length; i++) {
            long hashCode64 = objectReaderAdapter.hashCodes[i];
            int hashCode32 = (int)(hashCode64 ^ hashCode64 >>> 32);
            List<Long> hashCode64List = map.computeIfAbsent(hashCode32, k -> new ArrayList<>());
            hashCode64List.add(hashCode64);
         }

         int[] hashCode32Keys = new int[map.size()];
         int off = 0;

         for (Integer key : map.keySet()) {
            hashCode32Keys[off++] = key;
         }

         Arrays.sort(hashCode32Keys);
         mw.visitVarInsn(22, 1);
         mw.visitVarInsn(22, 1);
         mw.visitVarInsn(16, 32);
         mw.visitInsn(125);
         mw.visitInsn(131);
         mw.visitInsn(136);
         mw.visitVarInsn(54, 3);
         Label dflt = new Label();
         Label[] labels = new Label[hashCode32Keys.length];

         for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
         }

         mw.visitVarInsn(21, 3);
         mw.visitLookupSwitchInsn(dflt, hashCode32Keys, labels);

         for (int i = 0; i < labels.length; i++) {
            mw.visitLabel(labels[i]);
            int hashCode32 = hashCode32Keys[i];
            List<Long> hashCode64Array = map.get(hashCode32);
            int j = 0;

            for (int size = hashCode64Array.size(); j < size; j++) {
               long hashCode64 = hashCode64Array.get(j);
               Label next = size > 1 ? new Label() : dflt;
               mw.visitVarInsn(22, 1);
               mw.visitLdcInsn(hashCode64);
               mw.visitInsn(148);
               mw.visitJumpInsn(154, next);
               int m = Arrays.binarySearch(objectReaderAdapter.hashCodes, hashCode64);
               int index = objectReaderAdapter.mapping[m];
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(index), ASMUtils.DESC_FIELD_READER);
               mw.visitJumpInsn(167, rtnlt);
               if (next != dflt) {
                  mw.visitLabel(next);
               }
            }

            mw.visitJumpInsn(167, dflt);
         }

         mw.visitLabel(dflt);
      } else {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            Label next_ = new Label();
            Label get_ = new Label();
            String fieldName = fieldReaderArray[i].fieldName;
            long hashCode64 = fieldReaderArray[i].fieldNameHash;
            mw.visitVarInsn(22, 1);
            mw.visitLdcInsn(hashCode64);
            mw.visitInsn(148);
            mw.visitJumpInsn(154, next_);
            mw.visitLabel(get_);
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
            mw.visitJumpInsn(167, rtnlt);
            mw.visitLabel(next_);
         }
      }

      mw.visitInsn(1);
      mw.visitInsn(176);
      mw.visitLabel(rtnlt);
      mw.visitInsn(176);
      mw.visitMaxs(5, 5);
   }

   private void genMethodGetFieldReaderLCase(FieldReader[] fieldReaderArray, ClassWriter cw, String classNameType, ObjectReaderAdapter objectReaderAdapter) {
      MethodWriter mw = cw.visitMethod(1, "getFieldReaderLCase", "(J)" + ASMUtils.DESC_FIELD_READER, 512);
      int HASH_CODE_64 = 1;
      int HASH_CODE_32 = 3;
      Label rtnlt = new Label();
      if (fieldReaderArray.length > 6) {
         Map<Integer, List<Long>> map = new TreeMap<>();

         for (int i = 0; i < objectReaderAdapter.hashCodesLCase.length; i++) {
            long hashCode64 = objectReaderAdapter.hashCodesLCase[i];
            int hashCode32 = (int)(hashCode64 ^ hashCode64 >>> 32);
            List<Long> hashCode64List = map.computeIfAbsent(hashCode32, k -> new ArrayList<>());
            hashCode64List.add(hashCode64);
         }

         int[] hashCode32Keys = new int[map.size()];
         int off = 0;

         for (Integer key : map.keySet()) {
            hashCode32Keys[off++] = key;
         }

         Arrays.sort(hashCode32Keys);
         mw.visitVarInsn(22, 1);
         mw.visitVarInsn(22, 1);
         mw.visitVarInsn(16, 32);
         mw.visitInsn(125);
         mw.visitInsn(131);
         mw.visitInsn(136);
         mw.visitVarInsn(54, 3);
         Label dflt = new Label();
         Label[] labels = new Label[hashCode32Keys.length];

         for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
         }

         mw.visitVarInsn(21, 3);
         mw.visitLookupSwitchInsn(dflt, hashCode32Keys, labels);

         for (int i = 0; i < labels.length; i++) {
            mw.visitLabel(labels[i]);
            int hashCode32 = hashCode32Keys[i];

            for (long hashCode64 : map.get(hashCode32)) {
               mw.visitVarInsn(22, 1);
               mw.visitLdcInsn(hashCode64);
               mw.visitInsn(148);
               mw.visitJumpInsn(154, dflt);
               int m = Arrays.binarySearch(objectReaderAdapter.hashCodesLCase, hashCode64);
               int index = objectReaderAdapter.mappingLCase[m];
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(index), ASMUtils.DESC_FIELD_READER);
               mw.visitJumpInsn(167, rtnlt);
            }

            mw.visitJumpInsn(167, dflt);
         }

         mw.visitLabel(dflt);
      } else {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            Label next_ = new Label();
            Label get_ = new Label();
            String fieldName = fieldReaderArray[i].fieldName;
            long hashCode64 = fieldReaderArray[i].fieldNameHashLCase;
            mw.visitVarInsn(22, 1);
            mw.visitLdcInsn(hashCode64);
            mw.visitInsn(148);
            mw.visitJumpInsn(154, next_);
            mw.visitLabel(get_);
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
            mw.visitJumpInsn(167, rtnlt);
            mw.visitLabel(next_);
         }
      }

      mw.visitInsn(1);
      mw.visitInsn(176);
      mw.visitLabel(rtnlt);
      mw.visitInsn(176);
      mw.visitMaxs(5, 5);
   }

   private void genInitFields(
      FieldReader[] fieldReaderArray,
      String classNameType,
      boolean generatedFields,
      int THIS,
      int FIELD_READER_ARRAY,
      MethodWriter mw,
      String objectReaderSuper
   ) {
      if (objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_ADAPTER && generatedFields) {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            mw.visitVarInsn(25, THIS);
            mw.visitVarInsn(25, FIELD_READER_ARRAY);
            switch (i) {
               case 0:
                  mw.visitInsn(3);
                  break;
               case 1:
                  mw.visitInsn(4);
                  break;
               case 2:
                  mw.visitInsn(5);
                  break;
               case 3:
                  mw.visitInsn(6);
                  break;
               case 4:
                  mw.visitInsn(7);
                  break;
               case 5:
                  mw.visitInsn(8);
                  break;
               default:
                  if (i >= 128) {
                     mw.visitIntInsn(17, i);
                  } else {
                     mw.visitIntInsn(16, i);
                  }
            }

            mw.visitInsn(50);
            mw.visitFieldInsn(181, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
         }
      }
   }

   private void genFields(FieldReader[] fieldReaderArray, ClassWriter cw, String objectReaderSuper) {
      if (objectReaderSuper == ASMUtils.TYPE_OBJECT_READER_ADAPTER) {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            FieldWriter fieldClass = cw.visitField(1, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
         }

         for (int i = 0; i < fieldReaderArray.length; i++) {
            FieldWriter var9 = cw.visitField(1, fieldObjectReader(i), ASMUtils.DESC_OBJECT_READER);
         }
      }

      for (int i = 0; i < fieldReaderArray.length; i++) {
         Class fieldClass = fieldReaderArray[i].fieldClass;
         if (List.class.isAssignableFrom(fieldClass)) {
            FieldWriter var6 = cw.visitField(1, fieldItemObjectReader(i), ASMUtils.DESC_OBJECT_READER);
         }
      }
   }

   private <T> void genMethodReadJSONBObject(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      Constructor defaultConstructor,
      long readerFeatures,
      String TYPE_OBJECT,
      FieldReader[] fieldReaderArray,
      ClassWriter cw,
      String classNameType,
      ObjectReaderAdapter objectReaderAdapter
   ) {
      Class objectClass = context.objectClass;
      boolean fieldBased = (readerFeatures & JSONReader.Feature.FieldBased.mask) != 0L;
      MethodWriter mw = cw.visitMethod(1, "readJSONBObject", METHOD_DESC_READ_OBJECT, 2048);
      boolean disableArrayMapping = context.disableSupportArrayMapping();
      boolean disableAutoType = context.disableAutoType();
      int JSON_READER = 1;
      int FIELD_TYPE = 2;
      int FIELD_NAME = 3;
      int FEATURES = 4;
      int OBJECT = 6;
      int ENTRY_CNT = 7;
      int I = 8;
      int HASH_CODE64 = 9;
      int HASH_CODE_32 = 11;
      int ITEM_CNT = 12;
      int J = 13;
      int FIELD_READER = 14;
      int AUTO_TYPE_OBJECT_READER = 15;
      if (!disableAutoType) {
         this.genCheckAutoType(classNameType, mw, 1, 2, 3, 4, 15);
      }

      int varIndex = 16;
      Map<Object, Integer> variants = new HashMap<>();
      Label notNull_ = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNull", "()Z", false);
      mw.visitJumpInsn(153, notNull_);
      mw.visitInsn(1);
      mw.visitInsn(176);
      mw.visitLabel(notNull_);
      if (objectClass != null && !Serializable.class.isAssignableFrom(objectClass)) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, "objectClass", "Ljava/lang/Class;");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "errorOnNoneSerializable", "(Ljava/lang/Class;)V", false);
      }

      if (!disableArrayMapping) {
         notNull_ = new Label();
         new Label();
         Label endArray_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isArray", "()Z", false);
         mw.visitJumpInsn(153, notNull_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportBeanArray", "()Z", false);
         mw.visitJumpInsn(153, endArray_);
         this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
         mw.visitVarInsn(58, 6);
         Label fieldEnd_ = new Label();
         Label entryCountMatch_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "startArray", "()I", false);
         mw.visitInsn(89);
         mw.visitVarInsn(54, 7);
         mw.visitLdcInsn(fieldReaderArray.length);
         mw.visitJumpInsn(160, entryCountMatch_);

         for (int i = 0; i < fieldReaderArray.length; i++) {
            FieldReader fieldReader = fieldReaderArray[i];
            varIndex = this.genReadFieldValue(
               context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 12, 13, i, true, true, TYPE_OBJECT
            );
         }

         mw.visitJumpInsn(167, fieldEnd_);
         mw.visitLabel(entryCountMatch_);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 6);
         mw.visitVarInsn(21, 7);
         mw.visitMethodInsn(182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "readArrayMappingJSONBObject0", METHOD_DESC_READ_ARRAY_MAPPING_JSONB_OBJECT0, false);
         mw.visitLabel(fieldEnd_);
         mw.visitVarInsn(25, 6);
         mw.visitInsn(176);
         mw.visitLabel(endArray_);
         mw.visitLabel(notNull_);
      }

      this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
      mw.visitVarInsn(58, 6);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfObjectStart", "()Z", false);
      mw.visitInsn(87);
      this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
      mw.visitVarInsn(58, 6);
      notNull_ = new Label();
      Label for_end_i_ = new Label();
      Label for_inc_i_ = new Label();
      if (!disableAutoType) {
         mw.visitInsn(3);
         mw.visitVarInsn(54, 8);
      }

      mw.visitLabel(notNull_);
      Label hashCode64Start = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfObjectEnd", "()Z", false);
      mw.visitJumpInsn(154, for_end_i_);
      if (context.fieldNameLengthMin >= 2 && context.fieldNameLengthMax <= 43) {
         varIndex = this.genRead243(
            context, TYPE_OBJECT, fieldReaderArray, classNameType, fieldBased, mw, 1, 4, 6, 12, 13, varIndex, variants, for_inc_i_, hashCode64Start, true
         );
      }

      mw.visitLabel(hashCode64Start);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readFieldNameHashCode", "()J", false);
      mw.visitInsn(92);
      mw.visitVarInsn(55, 9);
      mw.visitInsn(9);
      mw.visitInsn(148);
      mw.visitJumpInsn(153, for_inc_i_);
      if (!disableAutoType) {
         Label endAutoType_ = new Label();
         mw.visitVarInsn(22, 9);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, "typeKeyHashCode", "J");
         mw.visitInsn(148);
         mw.visitJumpInsn(154, endAutoType_);
         mw.visitVarInsn(22, 9);
         mw.visitInsn(9);
         mw.visitInsn(148);
         mw.visitJumpInsn(153, endAutoType_);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, classNameType, "autoType", "(" + ASMUtils.DESC_JSON_READER + ")Ljava/lang/Object;", false);
         mw.visitVarInsn(58, 6);
         mw.visitJumpInsn(167, for_end_i_);
         mw.visitLabel(endAutoType_);
      }

      if (fieldReaderArray.length > 6) {
         Map<Integer, List<Long>> map = new TreeMap<>();

         for (int i = 0; i < objectReaderAdapter.hashCodes.length; i++) {
            long hashCode64 = objectReaderAdapter.hashCodes[i];
            int hashCode32 = (int)(hashCode64 ^ hashCode64 >>> 32);
            List<Long> hashCode64List = map.computeIfAbsent(hashCode32, k -> new ArrayList<>());
            hashCode64List.add(hashCode64);
         }

         int[] hashCode32Keys = new int[map.size()];
         int off = 0;

         for (Integer key : map.keySet()) {
            hashCode32Keys[off++] = key;
         }

         Arrays.sort(hashCode32Keys);
         mw.visitVarInsn(22, 9);
         mw.visitVarInsn(22, 9);
         mw.visitVarInsn(16, 32);
         mw.visitInsn(125);
         mw.visitInsn(131);
         mw.visitInsn(136);
         mw.visitVarInsn(54, 11);
         Label dflt = new Label();
         Label[] labels = new Label[hashCode32Keys.length];

         for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
         }

         mw.visitVarInsn(21, 11);
         mw.visitLookupSwitchInsn(dflt, hashCode32Keys, labels);

         for (int i = 0; i < labels.length; i++) {
            mw.visitLabel(labels[i]);
            int hashCode32 = hashCode32Keys[i];
            List<Long> hashCode64Array = map.get(hashCode32);
            int j = 0;

            for (int size = hashCode64Array.size(); j < size; j++) {
               long hashCode64 = hashCode64Array.get(j);
               Label next = size > 1 ? new Label() : dflt;
               mw.visitVarInsn(22, 9);
               mw.visitLdcInsn(hashCode64);
               mw.visitInsn(148);
               mw.visitJumpInsn(154, next);
               int m = Arrays.binarySearch(objectReaderAdapter.hashCodes, hashCode64);
               int index = objectReaderAdapter.mapping[m];
               FieldReader fieldReader = fieldReaderArray[index];
               varIndex = this.genReadFieldValue(
                  context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 12, 13, index, true, false, TYPE_OBJECT
               );
               mw.visitJumpInsn(167, for_inc_i_);
               if (next != dflt) {
                  mw.visitLabel(next);
               }
            }

            mw.visitJumpInsn(167, for_inc_i_);
         }

         mw.visitLabel(dflt);
         Label fieldReaderNull_ = new Label();
         if ((readerFeatures & JSONReader.Feature.SupportSmartMatch.mask) == 0L) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(22, 4);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportSmartMatch", "(J)Z", false);
            mw.visitJumpInsn(153, fieldReaderNull_);
         }

         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getNameHashCodeLCase", "()J", false);
         mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, "getFieldReaderLCase", METHOD_DESC_GET_FIELD_READER, true);
         mw.visitInsn(89);
         mw.visitVarInsn(58, 14);
         mw.visitJumpInsn(198, fieldReaderNull_);
         mw.visitVarInsn(25, 14);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 6);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "readFieldValueJSONB", METHOD_DESC_READ_FIELD_VALUE, false);
         mw.visitJumpInsn(167, for_inc_i_);
         mw.visitLabel(fieldReaderNull_);
      } else {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            Label next_ = new Label();
            FieldReader fieldReader = fieldReaderArray[i];
            long hashCode64 = Fnv.hashCode64(fieldReader.fieldName);
            mw.visitVarInsn(22, 9);
            mw.visitLdcInsn(hashCode64);
            mw.visitInsn(148);
            mw.visitJumpInsn(154, next_);
            varIndex = this.genReadFieldValue(
               context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 12, 13, i, true, false, TYPE_OBJECT
            );
            mw.visitJumpInsn(167, for_inc_i_);
            mw.visitLabel(next_);
         }

         Label processExtra_ = new Label();
         if ((readerFeatures & JSONReader.Feature.SupportSmartMatch.mask) == 0L) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(22, 4);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportSmartMatch", "(J)Z", false);
            mw.visitJumpInsn(153, processExtra_);
         }

         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getNameHashCodeLCase", "()J", false);
         mw.visitVarInsn(55, 9);

         for (int i = 0; i < fieldReaderArray.length; i++) {
            Label next_ = new Label();
            FieldReader fieldReader = fieldReaderArray[i];
            long hashCode64 = Fnv.hashCode64(fieldReader.fieldName);
            mw.visitVarInsn(22, 9);
            mw.visitLdcInsn(hashCode64);
            mw.visitInsn(148);
            mw.visitJumpInsn(154, next_);
            varIndex = this.genReadFieldValue(
               context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 12, 13, i, true, false, TYPE_OBJECT
            );
            mw.visitJumpInsn(167, for_inc_i_);
            mw.visitLabel(next_);
         }

         mw.visitLabel(processExtra_);
      }

      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 6);
      mw.visitVarInsn(22, 4);
      mw.visitMethodInsn(182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "processExtra", METHOD_DESC_PROCESS_EXTRA, false);
      mw.visitJumpInsn(167, for_inc_i_);
      mw.visitLabel(for_inc_i_);
      if (!disableAutoType) {
         mw.visitIincInsn(8, 1);
      }

      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(for_end_i_);
      mw.visitVarInsn(25, 6);
      mw.visitInsn(176);
      mw.visitMaxs(5, 10);
   }

   private <T> void genMethodReadJSONBObjectArrayMapping(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      Constructor defaultConstructor,
      long readerFeatures,
      String TYPE_OBJECT,
      FieldReader[] fieldReaderArray,
      ClassWriter cw,
      String classNameType,
      ObjectReaderAdapter objectReaderAdapter
   ) {
      boolean fieldBased = (readerFeatures & JSONReader.Feature.FieldBased.mask) != 0L;
      MethodWriter mw = cw.visitMethod(1, "readArrayMappingJSONBObject", METHOD_DESC_READ_OBJECT, 512);
      int JSON_READER = 1;
      int FIELD_TYPE = 2;
      int FIELD_NAME = 3;
      int FEATURES = 4;
      int OBJECT = 6;
      int ENTRY_CNT = 7;
      int ITEM_CNT = 8;
      int J = 9;
      int AUTO_TYPE_OBJECT_READER = 10;
      if (!context.disableAutoType()) {
         this.genCheckAutoType(classNameType, mw, 1, 2, 3, 4, 10);
      }

      int varIndex = 11;
      Map<Object, Integer> variants = new HashMap<>();
      Label notNull_ = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNull", "()Z", false);
      mw.visitJumpInsn(153, notNull_);
      mw.visitInsn(1);
      mw.visitInsn(176);
      mw.visitLabel(notNull_);
      this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
      mw.visitVarInsn(58, 6);
      notNull_ = new Label();
      Label entryCountMatch_ = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "startArray", "()I", false);
      mw.visitInsn(89);
      mw.visitVarInsn(54, 7);
      mw.visitLdcInsn(fieldReaderArray.length);
      mw.visitJumpInsn(160, entryCountMatch_);

      for (int i = 0; i < fieldReaderArray.length; i++) {
         FieldReader fieldReader = fieldReaderArray[i];
         varIndex = this.genReadFieldValue(
            context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 8, 9, i, true, true, TYPE_OBJECT
         );
      }

      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(entryCountMatch_);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 6);
      mw.visitVarInsn(21, 7);
      mw.visitMethodInsn(182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "readArrayMappingJSONBObject0", METHOD_DESC_READ_ARRAY_MAPPING_JSONB_OBJECT0, false);
      mw.visitLabel(notNull_);
      mw.visitVarInsn(25, 6);
      mw.visitInsn(176);
      mw.visitMaxs(5, 10);
   }

   private void genCheckAutoType(
      String classNameType, MethodWriter mw, int JSON_READER, int FIELD_TYPE, int FIELD_NAME, int FEATURES, int AUTO_TYPE_OBJECT_READER
   ) {
      Label checkArrayAutoTypeNull_ = new Label();
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, JSON_READER);
      mw.visitVarInsn(22, FEATURES);
      mw.visitMethodInsn(182, classNameType, "checkAutoType", METHOD_DESC_JSON_READER_CHECK_ARRAY_AUTO_TYPE, false);
      mw.visitInsn(89);
      mw.visitVarInsn(58, AUTO_TYPE_OBJECT_READER);
      mw.visitJumpInsn(198, checkArrayAutoTypeNull_);
      mw.visitVarInsn(25, AUTO_TYPE_OBJECT_READER);
      mw.visitVarInsn(25, JSON_READER);
      mw.visitVarInsn(25, FIELD_TYPE);
      mw.visitVarInsn(25, FIELD_NAME);
      mw.visitVarInsn(22, FEATURES);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, "readJSONBObject", METHOD_DESC_READ_OBJECT, true);
      mw.visitInsn(176);
      mw.visitLabel(checkArrayAutoTypeNull_);
   }

   private <T> void genMethodReadObject(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      Constructor defaultConstructor,
      long readerFeatures,
      String TYPE_OBJECT,
      FieldReader[] fieldReaderArray,
      ClassWriter cw,
      String classNameType,
      ObjectReaderAdapter objectReaderAdapter
   ) {
      boolean fieldBased = (readerFeatures & JSONReader.Feature.FieldBased.mask) != 0L;
      MethodWriter mw = cw.visitMethod(1, "readObject", METHOD_DESC_READ_OBJECT, 2048);
      int JSON_READER = 1;
      int FIELD_TYPE = 2;
      int FIELD_NAME = 3;
      int FEATURES = 4;
      int OBJECT = 6;
      int I = 7;
      int HASH_CODE64 = 8;
      int HASH_CODE_32 = 10;
      int ITEM_CNT = 11;
      int J = 12;
      int FIELD_READER = 13;
      int varIndex = 14;
      Map<Object, Integer> variants = new HashMap<>();
      boolean disableArrayMapping = context.disableSupportArrayMapping();
      boolean disableAutoType = context.disableAutoType();
      boolean disableJSONB = context.disableJSONB();
      boolean disableSmartMatch = context.disableSmartMatch();
      if (!disableJSONB) {
         Label json_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 1);
         mw.visitFieldInsn(180, ASMUtils.TYPE_JSON_READER, "jsonb", "Z");
         mw.visitJumpInsn(153, json_);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 2);
         mw.visitVarInsn(25, 3);
         mw.visitVarInsn(22, 4);
         mw.visitMethodInsn(182, classNameType, "readJSONBObject", METHOD_DESC_READ_OBJECT, false);
         mw.visitInsn(176);
         mw.visitLabel(json_);
      }

      if (!disableSmartMatch || !disableArrayMapping) {
         Label object_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isArray", "()Z", false);
         mw.visitJumpInsn(153, object_);
         if (!disableArrayMapping) {
            Label singleItemArray_ = new Label();
            if ((readerFeatures & JSONReader.Feature.SupportArrayToBean.mask) == 0L) {
               mw.visitVarInsn(25, 1);
               mw.visitVarInsn(22, 4);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportBeanArray", "(J)Z", false);
               mw.visitJumpInsn(153, singleItemArray_);
            }

            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfArrayStart", "()Z", false);
            this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
            mw.visitVarInsn(58, 6);

            for (int i = 0; i < fieldReaderArray.length; i++) {
               FieldReader fieldReader = fieldReaderArray[i];
               varIndex = this.genReadFieldValue(
                  context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 11, 12, i, false, true, TYPE_OBJECT
               );
            }

            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfArrayEnd", "()Z", false);
            mw.visitInsn(87);
            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfComma", "()Z", false);
            mw.visitInsn(87);
            mw.visitVarInsn(25, 6);
            mw.visitInsn(176);
            mw.visitLabel(singleItemArray_);
         }

         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 2);
         mw.visitVarInsn(25, 3);
         mw.visitVarInsn(22, 4);
         mw.visitMethodInsn(182, classNameType, "processObjectInputSingleItemArray", METHOD_DESC_READ_OBJECT, false);
         mw.visitInsn(176);
         mw.visitLabel(object_);
      }

      Label notNull_ = new Label();
      Label end_ = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfObjectStart", "()Z", false);
      mw.visitJumpInsn(154, notNull_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNullOrEmptyString", "()Z", false);
      mw.visitJumpInsn(153, notNull_);
      mw.visitInsn(1);
      mw.visitVarInsn(58, 6);
      mw.visitJumpInsn(167, end_);
      mw.visitLabel(notNull_);
      this.genCreateObject(mw, context, classNameType, TYPE_OBJECT, 4, fieldBased, defaultConstructor, objectReaderAdapter.creator);
      mw.visitVarInsn(58, 6);
      Label for_start_i_ = new Label();
      Label for_end_i_ = new Label();
      Label for_inc_i_ = new Label();
      if (!disableAutoType) {
         mw.visitInsn(3);
         mw.visitVarInsn(54, 7);
      }

      mw.visitLabel(for_start_i_);
      Label hashCode64Start = new Label();
      Label hashCode64End = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfObjectEnd", "()Z", false);
      mw.visitJumpInsn(154, for_end_i_);
      boolean switchGen = false;
      if (context.fieldNameLengthMin >= 5 && context.fieldNameLengthMax <= 7) {
         varIndex = this.genRead57(
            context, TYPE_OBJECT, fieldReaderArray, classNameType, fieldBased, mw, 1, 4, 6, 11, 12, varIndex, variants, for_inc_i_, hashCode64Start
         );
         switchGen = true;
      } else if (context.fieldNameLengthMin >= 2 && context.fieldNameLengthMax <= 43) {
         varIndex = this.genRead243(
            context, TYPE_OBJECT, fieldReaderArray, classNameType, fieldBased, mw, 1, 4, 6, 11, 12, varIndex, variants, for_inc_i_, hashCode64Start, false
         );
         switchGen = true;
      }

      mw.visitLabel(hashCode64Start);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readFieldNameHashCode", "()J", false);
      mw.visitInsn(92);
      mw.visitVarInsn(55, 8);
      mw.visitLdcInsn(-1L);
      mw.visitInsn(148);
      mw.visitJumpInsn(153, for_end_i_);
      mw.visitLabel(hashCode64End);
      if (!disableAutoType) {
         Label noneAutoType_ = new Label();
         mw.visitVarInsn(21, 7);
         mw.visitJumpInsn(154, noneAutoType_);
         mw.visitVarInsn(22, 8);
         mw.visitLdcInsn(ObjectReader.HASH_TYPE);
         mw.visitInsn(148);
         mw.visitJumpInsn(154, noneAutoType_);
         if ((readerFeatures & JSONReader.Feature.SupportAutoType.mask) == 0L) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(22, 4);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportAutoTypeOrHandler", "(J)Z", false);
            mw.visitJumpInsn(153, noneAutoType_);
         }

         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, "objectClass", "Ljava/lang/Class;");
         mw.visitVarInsn(22, 4);
         mw.visitMethodInsn(
            182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "autoType", "(" + ASMUtils.desc(JSONReader.class) + "Ljava/lang/Class;J)Ljava/lang/Object;", false
         );
         mw.visitInsn(176);
         mw.visitLabel(noneAutoType_);
      }

      if (switchGen) {
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(22, 8);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(22, 4);
         mw.visitVarInsn(25, 6);
         mw.visitMethodInsn(182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "readFieldValue", READ_FIELD_READER_UL, false);
         mw.visitJumpInsn(167, for_inc_i_);
      } else if (fieldReaderArray.length > 6) {
         Map<Integer, List<Long>> map = new TreeMap<>();

         for (int i = 0; i < objectReaderAdapter.hashCodes.length; i++) {
            long hashCode64 = objectReaderAdapter.hashCodes[i];
            int hashCode32 = (int)(hashCode64 ^ hashCode64 >>> 32);
            List<Long> hashCode64List = map.computeIfAbsent(hashCode32, k -> new ArrayList<>());
            hashCode64List.add(hashCode64);
         }

         int[] hashCode32Keys = new int[map.size()];
         int off = 0;

         for (Integer key : map.keySet()) {
            hashCode32Keys[off++] = key;
         }

         Arrays.sort(hashCode32Keys);
         mw.visitVarInsn(22, 8);
         mw.visitVarInsn(22, 8);
         mw.visitVarInsn(16, 32);
         mw.visitInsn(125);
         mw.visitInsn(131);
         mw.visitInsn(136);
         mw.visitVarInsn(54, 10);
         Label dflt = new Label();
         Label[] labels = new Label[hashCode32Keys.length];

         for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
         }

         mw.visitVarInsn(21, 10);
         mw.visitLookupSwitchInsn(dflt, hashCode32Keys, labels);

         for (int i = 0; i < labels.length; i++) {
            mw.visitLabel(labels[i]);
            int hashCode32 = hashCode32Keys[i];
            List<Long> hashCode64Array = map.get(hashCode32);
            int j = 0;

            for (int size = hashCode64Array.size(); j < size; j++) {
               long hashCode64 = hashCode64Array.get(j);
               Label next = size > 1 ? new Label() : dflt;
               mw.visitVarInsn(22, 8);
               mw.visitLdcInsn(hashCode64);
               mw.visitInsn(148);
               mw.visitJumpInsn(154, next);
               int m = Arrays.binarySearch(objectReaderAdapter.hashCodes, hashCode64);
               int index = objectReaderAdapter.mapping[m];
               FieldReader fieldReader = fieldReaderArray[index];
               varIndex = this.genReadFieldValue(
                  context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 11, 12, index, false, false, TYPE_OBJECT
               );
               mw.visitJumpInsn(167, for_inc_i_);
               if (next != dflt) {
                  mw.visitLabel(next);
               }
            }

            mw.visitJumpInsn(167, for_inc_i_);
         }

         mw.visitLabel(dflt);
         if (!disableSmartMatch) {
            Label fieldReaderNull_ = new Label();
            if ((readerFeatures & JSONReader.Feature.SupportSmartMatch.mask) == 0L) {
               mw.visitVarInsn(25, 1);
               mw.visitVarInsn(22, 4);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportSmartMatch", "(J)Z", false);
               mw.visitJumpInsn(153, fieldReaderNull_);
            }

            mw.visitVarInsn(25, 0);
            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getNameHashCodeLCase", "()J", false);
            mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, "getFieldReaderLCase", METHOD_DESC_GET_FIELD_READER, true);
            mw.visitInsn(89);
            mw.visitVarInsn(58, 13);
            mw.visitJumpInsn(198, fieldReaderNull_);
            mw.visitVarInsn(25, 13);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, 6);
            mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "readFieldValue", METHOD_DESC_READ_FIELD_VALUE, false);
            mw.visitJumpInsn(167, for_inc_i_);
            mw.visitLabel(fieldReaderNull_);
         }
      } else {
         for (int i = 0; i < fieldReaderArray.length; i++) {
            Label next_ = new Label();
            Label get_ = new Label();
            FieldReader fieldReader = fieldReaderArray[i];
            String fieldName = fieldReader.fieldName;
            long hashCode64 = fieldReader.fieldNameHash;
            mw.visitVarInsn(22, 8);
            mw.visitLdcInsn(hashCode64);
            mw.visitInsn(148);
            mw.visitJumpInsn(154, next_);
            mw.visitLabel(get_);
            varIndex = this.genReadFieldValue(
               context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 11, 12, i, false, false, TYPE_OBJECT
            );
            mw.visitJumpInsn(167, for_inc_i_);
            mw.visitLabel(next_);
         }

         Label processExtra_ = new Label();
         if (!disableSmartMatch) {
            if ((readerFeatures & JSONReader.Feature.SupportSmartMatch.mask) == 0L) {
               mw.visitVarInsn(25, 1);
               mw.visitVarInsn(22, 4);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isSupportSmartMatch", "(J)Z", false);
               mw.visitJumpInsn(153, processExtra_);
            }

            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getNameHashCodeLCase", "()J", false);
            mw.visitVarInsn(55, 8);

            for (int i = 0; i < fieldReaderArray.length; i++) {
               Label next_ = new Label();
               Label get_ = new Label();
               FieldReader fieldReader = fieldReaderArray[i];
               String fieldName = fieldReader.fieldName;
               long hashCode64 = fieldReader.fieldNameHash;
               long hashCode64LCase = fieldReader.fieldNameHashLCase;
               mw.visitVarInsn(22, 8);
               mw.visitLdcInsn(hashCode64);
               mw.visitInsn(148);
               mw.visitJumpInsn(153, get_);
               if (hashCode64LCase != hashCode64) {
                  mw.visitVarInsn(22, 8);
                  mw.visitLdcInsn(hashCode64LCase);
                  mw.visitInsn(148);
                  mw.visitJumpInsn(154, next_);
               } else {
                  mw.visitJumpInsn(167, next_);
               }

               mw.visitLabel(get_);
               varIndex = this.genReadFieldValue(
                  context, fieldReader, fieldBased, classNameType, mw, 0, 1, 6, 4, varIndex, variants, 11, 12, i, false, false, TYPE_OBJECT
               );
               mw.visitJumpInsn(167, for_inc_i_);
               mw.visitLabel(next_);
            }
         }

         mw.visitLabel(processExtra_);
      }

      if (!switchGen) {
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 6);
         mw.visitVarInsn(22, 4);
         mw.visitMethodInsn(182, ASMUtils.TYPE_OBJECT_READER_ADAPTER, "processExtra", METHOD_DESC_PROCESS_EXTRA, false);
         mw.visitJumpInsn(167, for_inc_i_);
      }

      mw.visitLabel(for_inc_i_);
      if (!disableAutoType) {
         mw.visitIincInsn(7, 1);
      }

      mw.visitJumpInsn(167, for_start_i_);
      mw.visitLabel(for_end_i_);
      mw.visitLabel(end_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfComma", "()Z", false);
      mw.visitInsn(87);
      mw.visitVarInsn(25, 6);
      mw.visitInsn(176);
      mw.visitMaxs(5, 10);
   }

   private int genRead243(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      String TYPE_OBJECT,
      FieldReader[] fieldReaderArray,
      String classNameType,
      boolean fieldBased,
      MethodWriter mw,
      int JSON_READER,
      int FEATURES,
      int OBJECT,
      int ITEM_CNT,
      int J,
      int varIndex,
      Map<Object, Integer> variants,
      Label for_inc_i_,
      Label hashCode64Start,
      boolean jsonb
   ) {
      IdentityHashMap<FieldReader, Integer> readerIndexMap = new IdentityHashMap<>();
      Map<Integer, List<FieldReader>> name0Map = new TreeMap<>();

      for (int i = 0; i < fieldReaderArray.length; i++) {
         FieldReader fieldReader = fieldReaderArray[i];
         readerIndexMap.put(fieldReader, i);
         byte[] name0Bytes = new byte[4];
         if (jsonb) {
            byte[] fieldNameJSONB = JSONB.toBytes(fieldReader.fieldName);
            System.arraycopy(fieldNameJSONB, 0, name0Bytes, 0, Math.min(4, fieldNameJSONB.length));
         } else {
            byte[] fieldName = fieldReader.fieldName.getBytes(StandardCharsets.UTF_8);
            name0Bytes[0] = 34;
            if (fieldName.length == 2) {
               System.arraycopy(fieldName, 0, name0Bytes, 1, 2);
               name0Bytes[3] = 34;
            } else {
               System.arraycopy(fieldName, 0, name0Bytes, 1, 3);
            }
         }

         int name0 = JDKUtils.UNSAFE.getInt(name0Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
         List<FieldReader> fieldReaders = name0Map.get(name0);
         if (fieldReaders == null) {
            fieldReaders = new ArrayList<>();
            name0Map.put(name0, fieldReaders);
         }

         fieldReaders.add(fieldReader);
      }

      Label dflt = new Label();
      int[] switchKeys = new int[name0Map.size()];
      Label[] labels = new Label[name0Map.size()];
      Iterator it = name0Map.keySet().iterator();

      for (int i = 0; i < labels.length; i++) {
         labels[i] = new Label();
         switchKeys[i] = (Integer)it.next();
      }

      mw.visitVarInsn(25, JSON_READER);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getRawInt", "()I", false);
      mw.visitLookupSwitchInsn(dflt, switchKeys, labels);

      for (int i = 0; i < labels.length; i++) {
         mw.visitLabel(labels[i]);
         int name0 = switchKeys[i];
         List<FieldReader> fieldReaders = name0Map.get(name0);

         for (int j = 0; j < fieldReaders.size(); j++) {
            Label nextJ = null;
            if (j + 1 != fieldReaders.size()) {
               nextJ = new Label();
            }

            FieldReader fieldReaderx = fieldReaders.get(j);
            int fieldReaderIndex = readerIndexMap.get(fieldReaderx);
            byte[] fieldName = fieldReaderx.fieldName.getBytes(StandardCharsets.UTF_8);
            int fieldNameLength = fieldName.length;
            switch (fieldNameLength) {
               case 2:
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match2", "()Z", false);
                  break;
               case 3:
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match3", "()Z", false);
                  break;
               case 4:
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(fieldName[3]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match4", "(B)Z", false);
                  break;
               case 5: {
                  byte[] bytes4 = new byte[]{fieldName[3], fieldName[4], 34, 58};
                  int name1 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name1 &= 65535;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match5", "(I)Z", false);
                  break;
               }
               case 6: {
                  byte[] bytes4 = new byte[]{fieldName[3], fieldName[4], fieldName[5], 34};
                  int name1 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name1 &= 16777215;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match6", "(I)Z", false);
                  break;
               }
               case 7: {
                  int name1 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match7", "(I)Z", false);
                  break;
               }
               case 8: {
                  int name1 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(fieldName[7]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match8", "(IB)Z", false);
                  break;
               }
               case 9: {
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 3, bytes8, 0, 6);
                  bytes8[6] = 34;
                  bytes8[7] = 58;
                  long name1 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name1 &= 281474976710655L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match9", "(J)Z", false);
                  break;
               }
               case 10: {
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 3, bytes8, 0, 7);
                  bytes8[7] = 34;
                  long name1 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name1 &= 72057594037927935L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match10", "(J)Z", false);
                  break;
               }
               case 11: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match11", "(J)Z", false);
                  break;
               }
               case 12: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(fieldName[11]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match12", "(JB)Z", false);
                  break;
               }
               case 13: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  byte[] bytes4 = new byte[]{fieldName[11], fieldName[12], 34, 58};
                  int name2 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name2 &= 65535;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match13", "(JI)Z", false);
                  break;
               }
               case 14: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  byte[] bytes4 = new byte[]{fieldName[11], fieldName[12], fieldName[13], 34};
                  int name2 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name2 &= 16777215;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match14", "(JI)Z", false);
                  break;
               }
               case 15: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  int name2 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match15", "(JI)Z", false);
                  break;
               }
               case 16: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  int name2 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(fieldName[15]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match16", "(JIB)Z", false);
                  break;
               }
               case 17: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 11, bytes8, 0, 6);
                  bytes8[6] = 34;
                  bytes8[7] = 58;
                  long name2 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name2 &= 281474976710655L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match17", "(JJ)Z", false);
                  break;
               }
               case 18: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 11, bytes8, 0, 7);
                  bytes8[7] = 34;
                  long name2 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name2 &= 72057594037927935L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match18", "(JJ)Z", false);
                  break;
               }
               case 19: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match19", "(JJ)Z", false);
                  break;
               }
               case 20: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(fieldName[19]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match20", "(JJB)Z", false);
                  break;
               }
               case 21: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  byte[] bytes4 = new byte[]{fieldName[19], fieldName[20], 34, 58};
                  int name3 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name3 &= 65535;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match21", "(JJI)Z", false);
                  break;
               }
               case 22: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  byte[] bytes4 = new byte[]{fieldName[19], fieldName[20], fieldName[21], 34};
                  int name3 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name3 &= 16777215;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match22", "(JJI)Z", false);
                  break;
               }
               case 23: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  int name3 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match23", "(JJI)Z", false);
                  break;
               }
               case 24: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  int name3 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(fieldName[23]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match24", "(JJIB)Z", false);
                  break;
               }
               case 25: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 19, bytes8, 0, 6);
                  bytes8[6] = 34;
                  bytes8[7] = 58;
                  long name3 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name3 &= 281474976710655L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match25", "(JJJ)Z", false);
                  break;
               }
               case 26: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 19, bytes8, 0, 7);
                  bytes8[7] = 34;
                  long name3 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name3 &= 72057594037927935L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match26", "(JJJ)Z", false);
                  break;
               }
               case 27: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match27", "(JJJ)Z", false);
                  break;
               }
               case 28: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(fieldName[27]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match28", "(JJJB)Z", false);
                  break;
               }
               case 29: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  byte[] bytes4 = new byte[]{fieldName[27], fieldName[28], 34, 58};
                  int name4 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name4 &= 65535;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match29", "(JJJI)Z", false);
                  break;
               }
               case 30: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  byte[] bytes4 = new byte[]{fieldName[27], fieldName[28], fieldName[29], 34};
                  int name4 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name4 &= 16777215;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match30", "(JJJI)Z", false);
                  break;
               }
               case 31: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  int name4 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match31", "(JJJI)Z", false);
                  break;
               }
               case 32: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  int name4 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(fieldName[31]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match32", "(JJJIB)Z", false);
                  break;
               }
               case 33: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 27, bytes8, 0, 6);
                  bytes8[6] = 34;
                  bytes8[7] = 58;
                  long name4 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name4 &= 281474976710655L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match33", "(JJJJ)Z", false);
                  break;
               }
               case 34: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 27, bytes8, 0, 7);
                  bytes8[7] = 34;
                  long name4 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name4 &= 72057594037927935L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match34", "(JJJJ)Z", false);
                  break;
               }
               case 35: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match35", "(JJJJ)Z", false);
                  break;
               }
               case 36: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(fieldName[35]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match36", "(JJJJB)Z", false);
                  break;
               }
               case 37: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  byte[] bytes4 = new byte[]{fieldName[35], fieldName[36], 34, 58};
                  int name5 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name5 &= 65535;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match37", "(JJJJI)Z", false);
                  break;
               }
               case 38: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  byte[] bytes4 = new byte[]{fieldName[35], fieldName[36], fieldName[37], 34};
                  int name5 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name5 &= 16777215;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match38", "(JJJJI)Z", false);
                  break;
               }
               case 39: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  int name5 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 35L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match39", "(JJJJI)Z", false);
                  break;
               }
               case 40: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  int name5 = JDKUtils.UNSAFE.getInt(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 35L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitLdcInsn(fieldName[39]);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match40", "(JJJJIB)Z", false);
                  break;
               }
               case 41: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 35, bytes8, 0, 6);
                  bytes8[6] = 34;
                  bytes8[7] = 58;
                  long name5 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name5 &= 281474976710655L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match41", "(JJJJJ)Z", false);
                  break;
               }
               case 42: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  byte[] bytes8 = new byte[8];
                  System.arraycopy(fieldName, 35, bytes8, 0, 7);
                  bytes8[7] = 34;
                  long name5 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  if (jsonb) {
                     name5 &= 72057594037927935L;
                  }

                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match42", "(JJJJJ)Z", false);
                  break;
               }
               case 43: {
                  long name1 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                  long name2 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 11L);
                  long name3 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 19L);
                  long name4 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 27L);
                  long name5 = JDKUtils.UNSAFE.getLong(fieldName, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 35L);
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitLdcInsn(name1);
                  mw.visitLdcInsn(name2);
                  mw.visitLdcInsn(name3);
                  mw.visitLdcInsn(name4);
                  mw.visitLdcInsn(name5);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfName4Match43", "(JJJJJ)Z", false);
                  break;
               }
               default:
                  throw new IllegalStateException("fieldNameLength " + fieldNameLength);
            }

            mw.visitJumpInsn(153, nextJ != null ? nextJ : hashCode64Start);
            varIndex = this.genReadFieldValue(
               context,
               fieldReaderx,
               fieldBased,
               classNameType,
               mw,
               0,
               JSON_READER,
               OBJECT,
               FEATURES,
               varIndex,
               variants,
               ITEM_CNT,
               J,
               fieldReaderIndex,
               jsonb,
               false,
               TYPE_OBJECT
            );
            mw.visitJumpInsn(167, for_inc_i_);
            if (nextJ != null) {
               mw.visitLabel(nextJ);
            }
         }

         mw.visitJumpInsn(167, dflt);
      }

      mw.visitLabel(dflt);
      return varIndex;
   }

   private int genRead57(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      String TYPE_OBJECT,
      FieldReader[] fieldReaderArray,
      String classNameType,
      boolean fieldBased,
      MethodWriter mw,
      int JSON_READER,
      int FEATURES,
      int OBJECT,
      int ITEM_CNT,
      int J,
      int varIndex,
      Map<Object, Integer> variants,
      Label for_inc_i_,
      Label hashCode64Start
   ) {
      Integer RAW_LONG = variants.get("RAW_LONG");
      if (RAW_LONG == null) {
         variants.put("RAW_LONG", RAW_LONG = varIndex);
         varIndex += 2;
      }

      mw.visitVarInsn(25, JSON_READER);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getRawLong", "()J", false);
      mw.visitInsn(92);
      mw.visitVarInsn(55, RAW_LONG);
      mw.visitInsn(9);
      mw.visitInsn(148);
      mw.visitJumpInsn(153, hashCode64Start);

      for (int i = 0; i < fieldReaderArray.length; i++) {
         Label next_ = new Label();
         FieldReader fieldReader = fieldReaderArray[i];
         byte[] fieldName = fieldReader.fieldName.getBytes(StandardCharsets.UTF_8);
         int fieldNameLength = fieldName.length;
         byte[] bytes8 = new byte[8];
         String nextMethodName;
         switch (fieldNameLength) {
            case 5:
               bytes8[0] = 34;
               System.arraycopy(fieldName, 0, bytes8, 1, 5);
               bytes8[6] = 34;
               bytes8[7] = 58;
               nextMethodName = "nextIfName8Match0";
               break;
            case 6:
               bytes8[0] = 34;
               System.arraycopy(fieldName, 0, bytes8, 1, 6);
               bytes8[7] = 34;
               nextMethodName = "nextIfName8Match1";
               break;
            case 7:
               bytes8[0] = 34;
               System.arraycopy(fieldName, 0, bytes8, 1, 7);
               nextMethodName = "nextIfName8Match2";
               break;
            default:
               throw new IllegalStateException("length " + fieldNameLength);
         }

         long rawLong = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
         mw.visitVarInsn(22, RAW_LONG);
         mw.visitLdcInsn(rawLong);
         mw.visitInsn(148);
         mw.visitJumpInsn(154, next_);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, nextMethodName, "()Z", false);
         mw.visitJumpInsn(153, hashCode64Start);
         varIndex = this.genReadFieldValue(
            context,
            fieldReader,
            fieldBased,
            classNameType,
            mw,
            0,
            JSON_READER,
            OBJECT,
            FEATURES,
            varIndex,
            variants,
            ITEM_CNT,
            J,
            i,
            false,
            false,
            TYPE_OBJECT
         );
         mw.visitJumpInsn(167, for_inc_i_);
         mw.visitLabel(next_);
      }

      return varIndex;
   }

   private <T> void genCreateObject(
      MethodWriter mw,
      ObjectReaderCreatorASM.ObjectWriteContext context,
      String classNameType,
      String TYPE_OBJECT,
      int FEATURES,
      boolean fieldBased,
      Constructor defaultConstructor,
      Supplier creator
   ) {
      Class objectClass = context.objectClass;
      int JSON_READER = 1;
      int objectModifiers = objectClass == null ? 1 : objectClass.getModifiers();
      boolean publicObject = Modifier.isPublic(objectModifiers) && (objectClass == null || !this.classLoader.isExternalClass(objectClass));
      if (defaultConstructor != null && publicObject && Modifier.isPublic(defaultConstructor.getModifiers())) {
         newObject(mw, TYPE_OBJECT, defaultConstructor);
      } else {
         if (creator != null) {
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, classNameType, "creator", "Ljava/util/function/Supplier;");
            mw.visitMethodInsn(185, "java/util/function/Supplier", "get", "()Ljava/lang/Object;", true);
         } else {
            mw.visitVarInsn(25, 0);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(22, FEATURES);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "features", "(J)J", false);
            mw.visitMethodInsn(182, classNameType, "createInstance", "(J)Ljava/lang/Object;", false);
         }

         if (publicObject) {
            mw.visitTypeInsn(192, TYPE_OBJECT);
         }
      }

      if (context.hasStringField) {
         Label endInitStringAsEmpty_ = new Label();
         new Label();
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isInitStringFieldAsEmpty", "()Z", false);
         mw.visitJumpInsn(153, endInitStringAsEmpty_);
         mw.visitInsn(89);
         mw.visitVarInsn(25, 0);
         mw.visitInsn(95);
         mw.visitMethodInsn(182, classNameType, "initStringFieldAsEmpty", "(Ljava/lang/Object;)V", false);
         mw.visitLabel(endInitStringAsEmpty_);
      }
   }

   private <T> int genReadFieldValue(
      ObjectReaderCreatorASM.ObjectWriteContext context,
      FieldReader fieldReader,
      boolean fieldBased,
      String classNameType,
      MethodWriter mw,
      int THIS,
      int JSON_READER,
      int OBJECT,
      int FEATURES,
      int varIndex,
      Map<Object, Integer> variants,
      int ITEM_CNT,
      int J,
      int i,
      boolean jsonb,
      boolean arrayMapping,
      String TYPE_OBJECT
   ) {
      Class objectClass = context.objectClass;
      Class fieldClass = fieldReader.fieldClass;
      Type fieldType = fieldReader.fieldType;
      long fieldFeatures = fieldReader.features;
      String format = fieldReader.format;
      Type itemType = fieldReader.itemType;
      if ((fieldFeatures & JSONReader.Feature.NullOnError.mask) != 0L) {
         mw.visitVarInsn(25, THIS);
         mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "readFieldValue", METHOD_DESC_READ_FIELD_VALUE, false);
         return varIndex;
      } else {
         Field field = fieldReader.field;
         Method method = fieldReader.method;
         Label endSet_ = new Label();
         String TYPE_FIELD_CLASS = ASMUtils.type(fieldClass);
         String DESC_FIELD_CLASS = ASMUtils.desc(fieldClass);
         mw.visitVarInsn(25, OBJECT);
         int fieldModifier = 0;
         if ((fieldBased || method == null) && field != null) {
            fieldModifier = field.getModifiers();
         }

         if (fieldBased
            && Modifier.isPublic(objectClass.getModifiers())
            && Modifier.isPublic(fieldModifier)
            && !Modifier.isFinal(fieldModifier)
            && !this.classLoader.isExternalClass(objectClass)) {
            mw.visitTypeInsn(192, TYPE_OBJECT);
         }

         if (fieldClass == boolean.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readBoolValue", "()Z", false);
         } else if (fieldClass == byte.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32Value", "()I", false);
         } else if (fieldClass == short.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32Value", "()I", false);
         } else if (fieldClass == int.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32Value", "()I", false);
         } else if (fieldClass == long.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt64Value", "()J", false);
         } else if (fieldClass == float.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readFloatValue", "()F", false);
         } else if (fieldClass == double.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readDoubleValue", "()D", false);
         } else if (fieldClass == char.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readCharValue", "()C", false);
         } else if (fieldClass == String.class) {
            mw.visitVarInsn(25, JSON_READER);
            Label null_ = new Label();
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readString", "()Ljava/lang/String;", false);
            mw.visitInsn(89);
            mw.visitJumpInsn(198, null_);
            if ("trim".equals(format)) {
               mw.visitMethodInsn(182, "java/lang/String", "trim", "()Ljava/lang/String;", false);
            } else if ("upper".equals(format)) {
               mw.visitMethodInsn(182, "java/lang/String", "toUpperCase", "()Ljava/lang/String;", false);
            }

            mw.visitLabel(null_);
         } else if (fieldClass == Byte.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt8", "()Ljava/lang/Byte;", false);
         } else if (fieldClass == Short.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt16", "()Ljava/lang/Short;", false);
         } else if (fieldClass == Integer.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32", "()Ljava/lang/Integer;", false);
         } else if (fieldClass == Long.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt64", "()Ljava/lang/Long;", false);
         } else if (fieldClass == Float.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readFloat", "()Ljava/lang/Float;", false);
         } else if (fieldClass == Double.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readDouble", "()Ljava/lang/Double;", false);
         } else if (fieldClass == BigDecimal.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readBigDecimal", "()Ljava/math/BigDecimal;", false);
         } else if (fieldClass == BigInteger.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readBigInteger", "()Ljava/math/BigInteger;", false);
         } else if (fieldClass == Number.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readNumber", "()Ljava/lang/Number;", false);
         } else if (fieldClass == UUID.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readUUID", "()Ljava/util/UUID;", false);
         } else if (fieldClass == LocalDate.class && fieldReader.format == null) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readLocalDate", "()Ljava/time/LocalDate;", false);
         } else if (fieldClass == OffsetDateTime.class && fieldReader.format == null) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readOffsetDateTime", "()Ljava/time/OffsetDateTime;", false);
         } else if (fieldClass == Date.class && fieldReader.format == null) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readDate", "()Ljava/util/Date;", false);
         } else if (fieldClass == Calendar.class && fieldReader.format == null) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readCalendar", "()Ljava/util/Calendar;", false);
         } else {
            Label endObject_ = new Label();
            boolean disableReferenceDetect = context.disableReferenceDetect();
            Integer REFERENCE = variants.get("REFERENCE");
            if (REFERENCE == null && !disableReferenceDetect) {
               variants.put("REFERENCE", REFERENCE = varIndex);
               varIndex++;
            }

            if (!disableReferenceDetect && !ObjectWriterProvider.isPrimitiveOrEnum(fieldClass)) {
               Label endReference_ = new Label();
               Label addResolveTask_ = new Label();
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isReference", "()Z", false);
               mw.visitJumpInsn(153, endReference_);
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readReference", "()Ljava/lang/String;", false);
               mw.visitInsn(89);
               mw.visitVarInsn(58, REFERENCE);
               mw.visitLdcInsn("..");
               mw.visitMethodInsn(182, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
               mw.visitJumpInsn(153, addResolveTask_);
               if (objectClass != null && fieldClass.isAssignableFrom(objectClass)) {
                  mw.visitVarInsn(25, OBJECT);
                  mw.visitJumpInsn(167, endObject_);
               }

               mw.visitLabel(addResolveTask_);
               mw.visitVarInsn(25, THIS);
               mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
               mw.visitVarInsn(25, JSON_READER);
               mw.visitVarInsn(25, OBJECT);
               mw.visitVarInsn(25, REFERENCE);
               mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "addResolveTask", METHOD_DESC_ADD_RESOLVE_TASK, false);
               mw.visitInsn(87);
               mw.visitJumpInsn(167, endSet_);
               mw.visitLabel(endReference_);
            }

            if (!fieldReader.fieldClassSerializable) {
               Label endIgnoreCheck_ = new Label();
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isIgnoreNoneSerializable", "()Z", false);
               mw.visitJumpInsn(153, endIgnoreCheck_);
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "skipValue", "()V", false);
               mw.visitInsn(87);
               mw.visitJumpInsn(167, endSet_);
               mw.visitLabel(endIgnoreCheck_);
            }

            boolean list = List.class.isAssignableFrom(fieldClass)
               && fieldReader.getInitReader() == null
               && !fieldClass.getName().startsWith("com.google.common.collect.Immutable");
            if (list) {
               Class itemClass = TypeUtils.getMapping(itemType);
               if (itemClass != null && Collection.class.isAssignableFrom(itemClass)) {
                  list = false;
               }
            }

            if (list && !fieldClass.isInterface() && !BeanUtils.hasPublicDefaultConstructor(fieldClass)) {
               list = false;
            }

            if (list) {
               varIndex = this.genReadFieldValueList(
                  fieldReader,
                  classNameType,
                  mw,
                  THIS,
                  JSON_READER,
                  OBJECT,
                  FEATURES,
                  varIndex,
                  variants,
                  ITEM_CNT,
                  J,
                  i,
                  jsonb,
                  arrayMapping,
                  objectClass,
                  fieldClass,
                  fieldType,
                  fieldFeatures,
                  itemType,
                  TYPE_FIELD_CLASS,
                  context
               );
            } else {
               String FIELD_OBJECT_READER = fieldObjectReader(i);
               Label valueNotNull_ = new Label();
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNull", "()Z", false);
               mw.visitJumpInsn(153, valueNotNull_);
               if (fieldClass == Optional.class) {
                  mw.visitMethodInsn(184, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
               } else if (fieldClass == OptionalInt.class) {
                  mw.visitMethodInsn(184, "java/util/OptionalInt", "empty", "()Ljava/util/OptionalInt;", false);
               } else if (fieldClass == OptionalLong.class) {
                  mw.visitMethodInsn(184, "java/util/OptionalLong", "empty", "()Ljava/util/OptionalLong;", false);
               } else if (fieldClass == OptionalDouble.class) {
                  mw.visitMethodInsn(184, "java/util/OptionalDouble", "empty", "()Ljava/util/OptionalDouble;", false);
               } else {
                  mw.visitInsn(1);
               }

               mw.visitJumpInsn(167, endObject_);
               mw.visitLabel(valueNotNull_);
               if (fieldClass == String[].class) {
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readStringArray", "()[Ljava/lang/String;", false);
               } else if (fieldClass == int[].class) {
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32ValueArray", "()[I", false);
               } else if (fieldClass == long[].class) {
                  mw.visitVarInsn(25, JSON_READER);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt64ValueArray", "()[J", false);
               } else {
                  if (Enum.class.isAssignableFrom(fieldClass) & !jsonb) {
                     this.genReadEnumValueRaw(
                        fieldReader, classNameType, mw, THIS, JSON_READER, i, jsonb, fieldType, fieldClass, fieldFeatures, FIELD_OBJECT_READER
                     );
                  } else {
                     this.genReadObject(fieldReader, classNameType, mw, THIS, JSON_READER, i, jsonb, fieldType, fieldFeatures, FIELD_OBJECT_READER);
                  }

                  if (method != null
                     || (objectClass == null || Modifier.isPublic(objectClass.getModifiers()))
                        && Modifier.isPublic(fieldModifier)
                        && !Modifier.isFinal(fieldModifier)
                        && !this.classLoader.isExternalClass(objectClass)) {
                     mw.visitTypeInsn(192, TYPE_FIELD_CLASS);
                  }

                  if (fieldReader.noneStaticMemberClass) {
                     try {
                        Field this0 = fieldClass.getDeclaredField("this$0");
                        long fieldOffset = JDKUtils.UNSAFE.objectFieldOffset(this0);
                        Label notNull_ = new Label();
                        mw.visitInsn(89);
                        mw.visitJumpInsn(198, notNull_);
                        mw.visitInsn(89);
                        mw.visitFieldInsn(178, ASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
                        mw.visitInsn(95);
                        mw.visitLdcInsn(fieldOffset);
                        mw.visitVarInsn(25, OBJECT);
                        mw.visitMethodInsn(182, "sun/misc/Unsafe", "putObject", "(Ljava/lang/Object;JLjava/lang/Object;)V", false);
                        mw.visitLabel(notNull_);
                     } catch (NoSuchFieldException var41) {
                     }
                  }
               }
            }

            mw.visitLabel(endObject_);
            if (!jsonb) {
               mw.visitVarInsn(25, JSON_READER);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfComma", "()Z", false);
               mw.visitInsn(87);
            }
         }

         if (field != null) {
            String fieldClassName = fieldClass.getName();
            boolean setDirect = (objectClass.getModifiers() & 1) != 0
               && (fieldModifier & 1) != 0
               && (fieldModifier & 16) == 0
               && (
                  ObjectWriterProvider.isPrimitiveOrEnum(fieldClass)
                     || fieldClassName.startsWith("java.")
                     || fieldClass.getClassLoader() == ObjectReaderProvider.FASTJSON2_CLASS_LOADER
               )
               && !this.classLoader.isExternalClass(objectClass)
               && field.getDeclaringClass() == objectClass;
            if (setDirect) {
               mw.visitFieldInsn(181, TYPE_OBJECT, field.getName(), DESC_FIELD_CLASS);
            } else {
               Integer FIELD_VALUE = variants.get(fieldClass);
               if (FIELD_VALUE == null) {
                  variants.put(fieldClass, FIELD_VALUE = varIndex);
                  if (fieldClass != long.class && fieldClass != double.class) {
                     varIndex++;
                  } else {
                     varIndex += 2;
                  }
               }

               String methodName;
               String methodDes;
               int LOAD;
               if (fieldClass == int.class) {
                  methodName = "putInt";
                  methodDes = "(Ljava/lang/Object;JI)V";
                  mw.visitVarInsn(54, FIELD_VALUE);
                  LOAD = 21;
               } else if (fieldClass == long.class) {
                  methodName = "putLong";
                  methodDes = "(Ljava/lang/Object;JJ)V";
                  mw.visitVarInsn(55, FIELD_VALUE);
                  LOAD = 22;
               } else if (fieldClass == float.class) {
                  methodName = "putFloat";
                  methodDes = "(Ljava/lang/Object;JF)V";
                  mw.visitVarInsn(56, FIELD_VALUE);
                  LOAD = 23;
               } else if (fieldClass == double.class) {
                  methodName = "putDouble";
                  methodDes = "(Ljava/lang/Object;JD)V";
                  mw.visitVarInsn(57, FIELD_VALUE);
                  LOAD = 24;
               } else if (fieldClass == char.class) {
                  methodName = "putChar";
                  methodDes = "(Ljava/lang/Object;JC)V";
                  mw.visitVarInsn(54, FIELD_VALUE);
                  LOAD = 21;
               } else if (fieldClass == byte.class) {
                  methodName = "putByte";
                  methodDes = "(Ljava/lang/Object;JB)V";
                  mw.visitVarInsn(54, FIELD_VALUE);
                  LOAD = 21;
               } else if (fieldClass == short.class) {
                  methodName = "putShort";
                  methodDes = "(Ljava/lang/Object;JS)V";
                  mw.visitVarInsn(54, FIELD_VALUE);
                  LOAD = 21;
               } else if (fieldClass == boolean.class) {
                  methodName = "putBoolean";
                  methodDes = "(Ljava/lang/Object;JZ)V";
                  mw.visitVarInsn(54, FIELD_VALUE);
                  LOAD = 21;
               } else {
                  methodName = "putObject";
                  methodDes = "(Ljava/lang/Object;JLjava/lang/Object;)V";
                  mw.visitVarInsn(58, FIELD_VALUE);
                  LOAD = 25;
               }

               mw.visitFieldInsn(178, ASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
               mw.visitInsn(95);
               mw.visitLdcInsn(JDKUtils.UNSAFE.objectFieldOffset(field));
               mw.visitVarInsn(LOAD, FIELD_VALUE);
               mw.visitMethodInsn(182, "sun/misc/Unsafe", methodName, methodDes, false);
            }
         } else {
            boolean invokeFieldReaderAccept = context.externalClass || method == null || !context.publicClass;
            if (invokeFieldReaderAccept) {
               Integer FIELD_VALUEx = variants.get(fieldClass);
               if (FIELD_VALUEx == null) {
                  variants.put(fieldClass, FIELD_VALUEx = varIndex);
                  if (fieldClass != long.class && fieldClass != double.class) {
                     varIndex++;
                  } else {
                     varIndex += 2;
                  }
               }

               String acceptMethodDesc;
               int LOAD;
               if (fieldClass == boolean.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;Z)V";
                  mw.visitVarInsn(54, FIELD_VALUEx);
                  LOAD = 21;
               } else if (fieldClass == byte.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;B)V";
                  mw.visitVarInsn(54, FIELD_VALUEx);
                  LOAD = 21;
               } else if (fieldClass == short.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;S)V";
                  mw.visitVarInsn(54, FIELD_VALUEx);
                  LOAD = 21;
               } else if (fieldClass == int.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;I)V";
                  mw.visitVarInsn(54, FIELD_VALUEx);
                  LOAD = 21;
               } else if (fieldClass == long.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;J)V";
                  mw.visitVarInsn(55, FIELD_VALUEx);
                  LOAD = 22;
               } else if (fieldClass == char.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;C)V";
                  mw.visitVarInsn(54, FIELD_VALUEx);
                  LOAD = 21;
               } else if (fieldClass == float.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;F)V";
                  mw.visitVarInsn(56, FIELD_VALUEx);
                  LOAD = 23;
               } else if (fieldClass == double.class) {
                  acceptMethodDesc = "(Ljava/lang/Object;D)V";
                  mw.visitVarInsn(57, FIELD_VALUEx);
                  LOAD = 24;
               } else {
                  acceptMethodDesc = "(Ljava/lang/Object;Ljava/lang/Object;)V";
                  mw.visitVarInsn(58, FIELD_VALUEx);
                  LOAD = 25;
               }

               mw.visitVarInsn(25, THIS);
               mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
               BiConsumer function = fieldReader.getFunction();
               if (function instanceof FieldBiConsumer) {
                  FieldBiConsumer fieldBiConsumer = (FieldBiConsumer)function;
                  mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "getFunction", "()Ljava/util/function/BiConsumer;", false);
                  mw.visitTypeInsn(192, ASMUtils.type(FieldBiConsumer.class));
                  mw.visitFieldInsn(180, ASMUtils.type(FieldBiConsumer.class), "consumer", ASMUtils.desc(FieldConsumer.class));
                  mw.visitInsn(95);
                  mw.visitLdcInsn(fieldBiConsumer.fieldIndex);
                  mw.visitVarInsn(LOAD, FIELD_VALUEx);
                  mw.visitMethodInsn(185, ASMUtils.type(FieldConsumer.class), "accept", "(Ljava/lang/Object;ILjava/lang/Object;)V", true);
               } else {
                  mw.visitInsn(95);
                  mw.visitVarInsn(LOAD, FIELD_VALUEx);
                  mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "accept", acceptMethodDesc, false);
               }
            } else {
               Class<?> returnType = method.getReturnType();
               String methodName = method.getName();
               String methodDesc = null;
               if (returnType == void.class) {
                  if (fieldClass == boolean.class) {
                     methodDesc = "(Z)V";
                  } else if (fieldClass == byte.class) {
                     methodDesc = "(B)V";
                  } else if (fieldClass == short.class) {
                     methodDesc = "(S)V";
                  } else if (fieldClass == int.class) {
                     methodDesc = "(I)V";
                  } else if (fieldClass == long.class) {
                     methodDesc = "(J)V";
                  } else if (fieldClass == char.class) {
                     methodDesc = "(C)V";
                  } else if (fieldClass == float.class) {
                     methodDesc = "(F)V";
                  } else if (fieldClass == double.class) {
                     methodDesc = "(D)V";
                  } else if (fieldClass == Boolean.class) {
                     methodDesc = "(Ljava/lang/Boolean;)V";
                  } else if (fieldClass == Integer.class) {
                     methodDesc = "(Ljava/lang/Integer;)V";
                  } else if (fieldClass == Long.class) {
                     methodDesc = "(Ljava/lang/Long;)V";
                  } else if (fieldClass == Float.class) {
                     methodDesc = "(Ljava/lang/Float;)V";
                  } else if (fieldClass == Double.class) {
                     methodDesc = "(Ljava/lang/Double;)V";
                  } else if (fieldClass == BigDecimal.class) {
                     methodDesc = "(Ljava/math/BigDecimal;)V";
                  } else if (fieldClass == String.class) {
                     methodDesc = "(Ljava/lang/String;)V";
                  } else if (fieldClass == UUID.class) {
                     methodDesc = "(Ljava/util/UUID;)V";
                  } else if (fieldClass == List.class) {
                     methodDesc = "(Ljava/util/List;)V";
                  } else if (fieldClass == Map.class) {
                     methodDesc = "(Ljava/util/Map;)V";
                  }
               }

               if (methodDesc == null) {
                  methodDesc = "(" + DESC_FIELD_CLASS + ")" + ASMUtils.desc(returnType);
               }

               mw.visitMethodInsn(182, TYPE_OBJECT, methodName, methodDesc, false);
               if (returnType != void.class) {
                  mw.visitInsn(87);
               }
            }
         }

         mw.visitLabel(endSet_);
         return varIndex;
      }
   }

   private void genReadObject(
      FieldReader fieldReader,
      String classNameType,
      MethodWriter mw,
      int THIS,
      int JSON_READER,
      int i,
      boolean jsonb,
      Type fieldType,
      long fieldFeatures,
      String FIELD_OBJECT_READER
   ) {
      Label notNull_ = new Label();
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(25, THIS);
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
      mw.visitVarInsn(25, JSON_READER);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "getObjectReader", METHOD_DESC_GET_OBJECT_READER_1, false);
      mw.visitFieldInsn(181, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitLabel(notNull_);
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitVarInsn(25, JSON_READER);
      this.gwGetFieldType(classNameType, mw, THIS, i, fieldType);
      mw.visitLdcInsn(fieldReader.fieldName);
      mw.visitLdcInsn(fieldFeatures);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, jsonb ? "readJSONBObject" : "readObject", METHOD_DESC_READ_OBJECT, true);
   }

   private void genReadEnumValueRaw(
      FieldReader fieldReader,
      String classNameType,
      MethodWriter mw,
      int THIS,
      int JSON_READER,
      int fieldIndex,
      boolean jsonb,
      Type fieldType,
      Class fieldClass,
      long fieldFeatures,
      String FIELD_OBJECT_READER
   ) {
      Object[] enums = fieldClass.getEnumConstants();
      Map<Integer, List<Enum>> name0Map = new TreeMap<>();
      int nameLengthMin = 0;
      int nameLengthMax = 0;
      if (enums != null) {
         for (int i = 0; i < enums.length; i++) {
            Enum e = (Enum)enums[i];
            byte[] enumName = e.name().getBytes(StandardCharsets.UTF_8);
            int nameLength = enumName.length;
            if (i == 0) {
               nameLengthMin = nameLength;
               nameLengthMax = nameLength;
            } else {
               nameLengthMin = Math.min(nameLength, nameLengthMin);
               nameLengthMax = Math.max(nameLength, nameLengthMax);
            }

            byte[] name0Bytes = new byte[4];
            name0Bytes[0] = 34;
            if (enumName.length == 2) {
               System.arraycopy(enumName, 0, name0Bytes, 1, 2);
               name0Bytes[3] = 34;
            } else if (enumName.length >= 3) {
               System.arraycopy(enumName, 0, name0Bytes, 1, 3);
            }

            int name0 = JDKUtils.UNSAFE.getInt(name0Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
            List<Enum> enumList = name0Map.get(name0);
            if (enumList == null) {
               enumList = new ArrayList<>();
               name0Map.put(name0, enumList);
            }

            enumList.add(e);
         }
      }

      Label dflt = new Label();
      Label enumEnd = new Label();
      Label notNull_ = new Label();
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(25, THIS);
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(fieldIndex), ASMUtils.DESC_FIELD_READER);
      mw.visitVarInsn(25, JSON_READER);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "getObjectReader", METHOD_DESC_GET_OBJECT_READER_1, false);
      mw.visitFieldInsn(181, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitLabel(notNull_);
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitTypeInsn(193, ASMUtils.type(ObjectReaderImplEnum.class));
      mw.visitJumpInsn(153, dflt);
      if (nameLengthMin >= 2 && nameLengthMax <= 11) {
         int[] switchKeys = new int[name0Map.size()];
         Label[] labels = new Label[name0Map.size()];
         Iterator it = name0Map.keySet().iterator();

         for (int j = 0; j < labels.length; j++) {
            labels[j] = new Label();
            switchKeys[j] = (Integer)it.next();
         }

         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "getRawInt", "()I", false);
         mw.visitLookupSwitchInsn(dflt, switchKeys, labels);

         for (int i = 0; i < labels.length; i++) {
            mw.visitLabel(labels[i]);
            int name0 = switchKeys[i];
            List<Enum> enumList = name0Map.get(name0);

            for (int j = 0; j < enumList.size(); j++) {
               Label nextJ = null;
               if (j > 0) {
                  nextJ = new Label();
               }

               Enum ex = enumList.get(j);
               byte[] enumNamex = ex.name().getBytes(StandardCharsets.UTF_8);
               int fieldNameLength = enumNamex.length;
               switch (fieldNameLength) {
                  case 2:
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match2", "()Z", false);
                     break;
                  case 3:
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match3", "()Z", false);
                     break;
                  case 4:
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(enumNamex[3]);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match4", "(B)Z", false);
                     break;
                  case 5:
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(enumNamex[3]);
                     mw.visitLdcInsn(enumNamex[4]);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match5", "(BB)Z", false);
                     break;
                  case 6: {
                     byte[] bytes4 = new byte[]{enumNamex[3], enumNamex[4], enumNamex[5], 34};
                     int name1 = JDKUtils.UNSAFE.getInt(bytes4, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match6", "(I)Z", false);
                     break;
                  }
                  case 7: {
                     int name1 = JDKUtils.UNSAFE.getInt(enumNamex, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match7", "(I)Z", false);
                     break;
                  }
                  case 8: {
                     int name1 = JDKUtils.UNSAFE.getInt(enumNamex, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitLdcInsn(enumNamex[7]);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match8", "(IB)Z", false);
                     break;
                  }
                  case 9: {
                     int name1 = JDKUtils.UNSAFE.getInt(enumNamex, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 3L);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitLdcInsn(enumNamex[7]);
                     mw.visitLdcInsn(enumNamex[8]);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match9", "(IBB)Z", false);
                     break;
                  }
                  case 10: {
                     byte[] bytes8 = new byte[8];
                     System.arraycopy(enumNamex, 3, bytes8, 0, 7);
                     bytes8[7] = 34;
                     long name1 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match10", "(J)Z", false);
                     break;
                  }
                  case 11: {
                     byte[] bytes8 = new byte[8];
                     System.arraycopy(enumNamex, 3, bytes8, 0, 8);
                     long name1 = JDKUtils.UNSAFE.getLong(bytes8, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                     mw.visitVarInsn(25, JSON_READER);
                     mw.visitLdcInsn(name1);
                     mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfValue4Match11", "(J)Z", false);
                     break;
                  }
                  default:
                     throw new IllegalStateException("fieldNameLength " + fieldNameLength);
               }

               mw.visitJumpInsn(153, nextJ != null ? nextJ : dflt);
               mw.visitVarInsn(25, THIS);
               mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
               mw.visitTypeInsn(192, ASMUtils.type(ObjectReaderImplEnum.class));
               mw.visitLdcInsn(ex.ordinal());
               mw.visitMethodInsn(182, ASMUtils.type(ObjectReaderImplEnum.class), "getEnumByOrdinal", "(I)Ljava/lang/Enum;", false);
               mw.visitJumpInsn(167, enumEnd);
               if (nextJ != null) {
                  mw.visitLabel(nextJ);
               }
            }

            mw.visitJumpInsn(167, dflt);
         }
      }

      mw.visitLabel(dflt);
      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, FIELD_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
      mw.visitVarInsn(25, JSON_READER);
      this.gwGetFieldType(classNameType, mw, THIS, fieldIndex, fieldType);
      mw.visitLdcInsn(fieldReader.fieldName);
      mw.visitLdcInsn(fieldFeatures);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, jsonb ? "readJSONBObject" : "readObject", METHOD_DESC_READ_OBJECT, true);
      mw.visitLabel(enumEnd);
   }

   private int genReadFieldValueList(
      FieldReader fieldReader,
      String classNameType,
      MethodWriter mw,
      int THIS,
      int JSON_READER,
      int OBJECT,
      int FEATURES,
      int varIndex,
      Map<Object, Integer> variants,
      int ITEM_CNT,
      int J,
      int i,
      boolean jsonb,
      boolean arrayMapping,
      Class objectClass,
      Class fieldClass,
      Type fieldType,
      long fieldFeatures,
      Type itemType,
      String TYPE_FIELD_CLASS,
      ObjectReaderCreatorASM.ObjectWriteContext context
   ) {
      if (itemType == null) {
         itemType = Object.class;
      }

      Class itemClass = TypeUtils.getMapping(itemType);
      String ITEM_OBJECT_READER = fieldItemObjectReader(i);
      Integer LIST = variants.get(fieldClass);
      if (LIST == null) {
         variants.put(fieldClass, LIST = varIndex);
         varIndex++;
      }

      Integer AUTO_TYPE_OBJECT_READER = variants.get(ObjectReader.class);
      if (AUTO_TYPE_OBJECT_READER == null) {
         variants.put(fieldClass, AUTO_TYPE_OBJECT_READER = varIndex);
         varIndex++;
      }

      String LIST_TYPE = fieldClass.isInterface() ? "java/util/ArrayList" : TYPE_FIELD_CLASS;
      Label loadList_ = new Label();
      Label listNotNull_ = new Label();
      Label listInitEnd_ = new Label();
      boolean initCapacity = JDKUtils.JVM_VERSION == 8 && "java/util/ArrayList".equals(LIST_TYPE);
      if (jsonb) {
         if (!context.disableAutoType()) {
            Label checkAutoTypeNull_ = new Label();
            mw.visitVarInsn(25, THIS);
            mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "checkObjectAutoType", METHOD_DESC_CHECK_ARRAY_AUTO_TYPE, false);
            mw.visitInsn(89);
            mw.visitVarInsn(58, AUTO_TYPE_OBJECT_READER);
            mw.visitJumpInsn(198, checkAutoTypeNull_);
            mw.visitVarInsn(25, AUTO_TYPE_OBJECT_READER);
            mw.visitVarInsn(25, JSON_READER);
            this.gwGetFieldType(classNameType, mw, THIS, i, fieldType);
            mw.visitLdcInsn(fieldReader.fieldName);
            mw.visitLdcInsn(fieldFeatures);
            mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, "readJSONBObject", METHOD_DESC_READ_OBJECT, true);
            mw.visitTypeInsn(192, TYPE_FIELD_CLASS);
            mw.visitVarInsn(58, LIST);
            mw.visitJumpInsn(167, loadList_);
            mw.visitLabel(checkAutoTypeNull_);
         }

         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "startArray", "()I", false);
         mw.visitInsn(89);
         mw.visitVarInsn(54, ITEM_CNT);
         mw.visitLdcInsn(-1);
         mw.visitJumpInsn(160, listNotNull_);
         mw.visitInsn(1);
         mw.visitVarInsn(58, LIST);
         mw.visitJumpInsn(167, loadList_);
         mw.visitLabel(listNotNull_);
         if (fieldReader.method == null && fieldReader.field != null) {
            long fieldOffset = JDKUtils.UNSAFE.objectFieldOffset(fieldReader.field);
            mw.visitFieldInsn(178, ASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
            mw.visitVarInsn(25, OBJECT);
            mw.visitLdcInsn(fieldOffset);
            mw.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false);
            mw.visitInsn(89);
            mw.visitTypeInsn(192, TYPE_FIELD_CLASS);
            mw.visitVarInsn(58, LIST);
            Label listNull_ = new Label();
            mw.visitJumpInsn(198, listNull_);
            mw.visitVarInsn(25, LIST);
            mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mw.visitFieldInsn(178, "java/util/Collections", "EMPTY_LIST", "Ljava/util/List;");
            mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mw.visitJumpInsn(166, listInitEnd_);
            mw.visitLabel(listNull_);
         }

         mw.visitTypeInsn(187, LIST_TYPE);
         mw.visitInsn(89);
         if (initCapacity) {
            mw.visitVarInsn(21, ITEM_CNT);
            mw.visitMethodInsn(183, LIST_TYPE, "<init>", "(I)V", false);
         } else {
            mw.visitMethodInsn(183, LIST_TYPE, "<init>", "()V", false);
         }

         mw.visitVarInsn(58, LIST);
         mw.visitLabel(listInitEnd_);
      } else {
         Label match_ = new Label();
         Label skipValue_ = new Label();
         Label loadNull_ = new Label();
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNull", "()Z", false);
         mw.visitJumpInsn(154, loadNull_);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfArrayStart", "()Z", false);
         mw.visitJumpInsn(154, match_);
         if (itemClass == String.class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isString", "()Z", false);
            mw.visitJumpInsn(153, skipValue_);
            mw.visitTypeInsn(187, LIST_TYPE);
            mw.visitInsn(89);
            if (initCapacity) {
               mw.visitLdcInsn(10);
               mw.visitMethodInsn(183, LIST_TYPE, "<init>", "(I)V", false);
            } else {
               mw.visitMethodInsn(183, LIST_TYPE, "<init>", "()V", false);
            }

            mw.visitVarInsn(58, LIST);
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNullOrEmptyString", "()Z", false);
            mw.visitJumpInsn(154, loadList_);
            mw.visitVarInsn(25, LIST);
            mw.visitVarInsn(25, JSON_READER);
            if (itemClass == String.class) {
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readString", "()Ljava/lang/String;", false);
            }

            mw.visitMethodInsn(185, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mw.visitInsn(87);
            mw.visitJumpInsn(167, loadList_);
         } else if (itemType instanceof Class) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfNullOrEmptyString", "()Z", false);
            mw.visitJumpInsn(154, loadNull_);
            mw.visitTypeInsn(187, LIST_TYPE);
            mw.visitInsn(89);
            if (initCapacity) {
               mw.visitLdcInsn(10);
               mw.visitMethodInsn(183, LIST_TYPE, "<init>", "(I)V", false);
            } else {
               mw.visitMethodInsn(183, LIST_TYPE, "<init>", "()V", false);
            }

            mw.visitVarInsn(58, LIST);
            mw.visitVarInsn(25, JSON_READER);
            mw.visitVarInsn(25, LIST);
            mw.visitLdcInsn((Class)itemType);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readArray", "(Ljava/util/List;Ljava/lang/reflect/Type;)V", false);
            mw.visitJumpInsn(167, loadList_);
         }

         mw.visitLabel(skipValue_);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "skipValue", "()V", false);
         mw.visitLabel(loadNull_);
         mw.visitInsn(1);
         mw.visitVarInsn(58, LIST);
         mw.visitJumpInsn(167, loadList_);
         mw.visitLabel(match_);
         mw.visitTypeInsn(187, LIST_TYPE);
         mw.visitInsn(89);
         if (initCapacity) {
            mw.visitLdcInsn(10);
            mw.visitMethodInsn(183, LIST_TYPE, "<init>", "(I)V", false);
         } else {
            mw.visitMethodInsn(183, LIST_TYPE, "<init>", "()V", false);
         }

         mw.visitVarInsn(58, LIST);
      }

      Label for_start_j_ = new Label();
      Label for_end_j_ = new Label();
      Label for_inc_j_ = new Label();
      mw.visitInsn(3);
      mw.visitVarInsn(54, J);
      mw.visitLabel(for_start_j_);
      if (jsonb) {
         mw.visitVarInsn(21, J);
         mw.visitVarInsn(21, ITEM_CNT);
         mw.visitJumpInsn(162, for_end_j_);
      } else {
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfArrayEnd", "()Z", false);
         mw.visitJumpInsn(154, for_end_j_);
      }

      if (itemType == String.class) {
         mw.visitVarInsn(25, LIST);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readString", "()Ljava/lang/String;", false);
      } else if (itemType == Integer.class) {
         mw.visitVarInsn(25, LIST);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt32", "()Ljava/lang/Integer;", false);
      } else if (itemType == Long.class) {
         mw.visitVarInsn(25, LIST);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readInt64", "()Ljava/lang/Long;", false);
      } else {
         Label notNull_ = new Label();
         mw.visitVarInsn(25, THIS);
         mw.visitFieldInsn(180, classNameType, ITEM_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
         mw.visitJumpInsn(199, notNull_);
         mw.visitVarInsn(25, THIS);
         mw.visitVarInsn(25, THIS);
         mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_READE, "getItemObjectReader", METHOD_DESC_GET_ITEM_OBJECT_READER, false);
         mw.visitFieldInsn(181, classNameType, ITEM_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
         mw.visitLabel(notNull_);
         if (!context.disableReferenceDetect()) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitVarInsn(25, LIST);
            mw.visitVarInsn(21, J);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "readReference", "(Ljava/util/List;I)Z", false);
            mw.visitJumpInsn(154, for_inc_j_);
         }

         mw.visitVarInsn(25, LIST);
         Label readObject_ = new Label();
         Label readObjectEnd_ = new Label();
         if (arrayMapping) {
            mw.visitVarInsn(25, JSON_READER);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "isArray", "()Z", false);
            mw.visitJumpInsn(153, readObject_);
            mw.visitVarInsn(25, THIS);
            mw.visitFieldInsn(180, classNameType, ITEM_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
            mw.visitVarInsn(25, JSON_READER);
            this.gwGetFieldType(classNameType, mw, THIS, i, fieldType);
            mw.visitLdcInsn(fieldReader.fieldName);
            mw.visitVarInsn(22, FEATURES);
            mw.visitMethodInsn(
               185, ASMUtils.TYPE_OBJECT_READER, jsonb ? "readArrayMappingJSONBObject" : "readArrayMappingObject", METHOD_DESC_READ_OBJECT, true
            );
            mw.visitJumpInsn(167, readObjectEnd_);
            mw.visitLabel(readObject_);
         }

         mw.visitVarInsn(25, THIS);
         mw.visitFieldInsn(180, classNameType, ITEM_OBJECT_READER, ASMUtils.DESC_OBJECT_READER);
         mw.visitVarInsn(25, JSON_READER);
         this.gwGetFieldType(classNameType, mw, THIS, i, fieldType);
         mw.visitLdcInsn(fieldReader.fieldName);
         mw.visitVarInsn(22, FEATURES);
         mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_READER, jsonb ? "readJSONBObject" : "readObject", METHOD_DESC_READ_OBJECT, true);
         if (arrayMapping) {
            mw.visitLabel(readObjectEnd_);
         }
      }

      mw.visitMethodInsn(185, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
      mw.visitInsn(87);
      if (!jsonb) {
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfComma", "()Z", false);
         mw.visitInsn(87);
      }

      mw.visitLabel(for_inc_j_);
      mw.visitIincInsn(J, 1);
      mw.visitJumpInsn(167, for_start_j_);
      mw.visitLabel(for_end_j_);
      if (!jsonb) {
         mw.visitVarInsn(25, JSON_READER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_READER, "nextIfComma", "()Z", false);
         mw.visitInsn(87);
      }

      mw.visitLabel(loadList_);
      mw.visitVarInsn(25, LIST);
      return varIndex;
   }

   private void gwGetFieldType(String classNameType, MethodWriter mw, int THIS, int i, Type fieldType) {
      if (fieldType instanceof Class) {
         Class fieldClass = (Class)fieldType;
         String fieldClassName = fieldClass.getName();
         boolean publicClass = Modifier.isPublic(fieldClass.getModifiers());
         boolean internalClass = fieldClassName.startsWith("java.") || fieldClass == JSONArray.class || fieldClass == JSONObject.class;
         if (publicClass && internalClass) {
            mw.visitLdcInsn((Class)fieldType);
            return;
         }
      }

      mw.visitVarInsn(25, THIS);
      mw.visitFieldInsn(180, classNameType, CodeGenUtils.fieldReader(i), ASMUtils.DESC_FIELD_READER);
      mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_READE, "fieldType", "Ljava/lang/reflect/Type;");
   }

   @Override
   public Function<Consumer, ByteArrayValueConsumer> createByteArrayValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return this.createValueConsumer0(objectClass, fieldReaderArray, true);
   }

   @Override
   public Function<Consumer, CharArrayValueConsumer> createCharArrayValueConsumerCreator(Class objectClass, FieldReader[] fieldReaderArray) {
      return this.createValueConsumer0(objectClass, fieldReaderArray, false);
   }

   private Function createValueConsumer0(Class objectClass, FieldReader[] fieldReaderArray, boolean bytes) {
      Constructor defaultConstructor = BeanUtils.getDefaultConstructor(objectClass, false);
      if (defaultConstructor != null && Modifier.isPublic(objectClass.getModifiers())) {
         ClassWriter cw = new ClassWriter(ex -> objectClass.getName().equals(ex) ? objectClass : null);
         String className = (bytes ? "VBACG_" : "VCACG_") + seed.incrementAndGet() + "_" + fieldReaderArray.length + "_" + objectClass.getSimpleName();
         Package pkg = ObjectReaderCreatorASM.class.getPackage();
         String classNameType;
         String classNameFull;
         if (pkg != null) {
            String packageName = pkg.getName();
            int packageNameLength = packageName.length();
            int charsLength = packageNameLength + 1 + className.length();
            char[] chars = new char[charsLength];
            packageName.getChars(0, packageName.length(), chars, 0);
            chars[packageNameLength] = '.';
            className.getChars(0, className.length(), chars, packageNameLength + 1);
            classNameFull = new String(chars);
            chars[packageNameLength] = '/';

            for (int i = 0; i < packageNameLength; i++) {
               if (chars[i] == '.') {
                  chars[i] = '/';
               }
            }

            classNameType = new String(chars);
         } else {
            classNameType = className;
            classNameFull = className;
         }

         String TYPE_OBJECT = ASMUtils.type(objectClass);
         String DESC_OBJECT = ASMUtils.desc(objectClass);
         cw.visitField(17, "consumer", "Ljava/util/function/Consumer;");
         cw.visitField(1, "object", DESC_OBJECT);
         cw.visit(
            52, 49, classNameType, "java/lang/Object", new String[]{bytes ? ASMUtils.TYPE_BYTE_ARRAY_VALUE_CONSUMER : ASMUtils.TYPE_CHAR_ARRAY_VALUE_CONSUMER}
         );
         int CONSUMER = 1;
         MethodWriter mw = cw.visitMethod(1, "<init>", "(Ljava/util/function/Consumer;)V", 32);
         mw.visitVarInsn(25, 0);
         mw.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitFieldInsn(181, classNameType, "consumer", "Ljava/util/function/Consumer;");
         mw.visitInsn(177);
         mw.visitMaxs(3, 3);
         MethodWriter mwx = cw.visitMethod(1, "beforeRow", "(I)V", 32);
         mwx.visitVarInsn(25, 0);
         newObject(mwx, TYPE_OBJECT, defaultConstructor);
         mwx.visitFieldInsn(181, classNameType, "object", DESC_OBJECT);
         mwx.visitInsn(177);
         mwx.visitMaxs(3, 3);
         MethodWriter mwxx = cw.visitMethod(1, "afterRow", "(I)V", 32);
         mwxx.visitVarInsn(25, 0);
         mwxx.visitFieldInsn(180, classNameType, "consumer", "Ljava/util/function/Consumer;");
         mwxx.visitVarInsn(25, 0);
         mwxx.visitFieldInsn(180, classNameType, "object", DESC_OBJECT);
         mwxx.visitMethodInsn(185, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true);
         mwxx.visitVarInsn(25, 0);
         mwxx.visitInsn(1);
         mwxx.visitFieldInsn(181, classNameType, "object", DESC_OBJECT);
         mwxx.visitInsn(177);
         mwxx.visitMaxs(3, 3);
         CONSUMER = 1;
         int COLUMN = 2;
         int BYTES = 3;
         int OFF = 4;
         int LEN = 5;
         int CHARSET = 6;
         String methodDesc;
         if (bytes) {
            methodDesc = "(II[BIILjava/nio/charset/Charset;)V";
         } else {
            methodDesc = "(II[CII)V";
         }

         MethodWriter mwxxx = cw.visitMethod(1, "accept", methodDesc, 32);
         Label switch_ = new Label();
         Label L0_ = new Label();
         Label L1_ = new Label();
         mwxxx.visitVarInsn(21, 5);
         mwxxx.visitJumpInsn(154, L0_);
         mwxxx.visitInsn(177);
         mwxxx.visitLabel(L0_);
         mwxxx.visitVarInsn(21, 2);
         mwxxx.visitJumpInsn(156, L1_);
         mwxxx.visitInsn(177);
         mwxxx.visitLabel(L1_);
         mwxxx.visitVarInsn(21, 2);
         mwxxx.visitLdcInsn(fieldReaderArray.length);
         mwxxx.visitJumpInsn(164, switch_);
         mwxxx.visitInsn(177);
         mwxxx.visitLabel(switch_);
         Label dflt = new Label();
         Label[] labels = new Label[fieldReaderArray.length];
         int[] columns = new int[fieldReaderArray.length];

         for (int ix = 0; ix < columns.length; ix++) {
            columns[ix] = ix;
            labels[ix] = new Label();
         }

         mwxxx.visitVarInsn(21, 2);
         mwxxx.visitLookupSwitchInsn(dflt, columns, labels);

         for (int ix = 0; ix < labels.length; ix++) {
            mwxxx.visitLabel(labels[ix]);
            FieldReader fieldReader = fieldReaderArray[ix];
            Field field = fieldReader.field;
            Class fieldClass = fieldReader.fieldClass;
            Type fieldType = fieldReader.fieldType;
            mwxxx.visitVarInsn(25, 0);
            mwxxx.visitFieldInsn(180, classNameType, "object", DESC_OBJECT);
            String DESC_FIELD_CLASS;
            String DESC_METHOD;
            if (fieldType == Integer.class
               || fieldType == int.class
               || fieldType == Short.class
               || fieldType == short.class
               || fieldType == Byte.class
               || fieldType == byte.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "parseInt", bytes ? "([BII)I" : "([CII)I", false);
               if (fieldType == short.class) {
                  DESC_FIELD_CLASS = "S";
                  DESC_METHOD = "(S)V";
               } else if (fieldType == Short.class) {
                  mwxxx.visitMethodInsn(184, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Short;";
                  DESC_METHOD = "(Ljava/lang/Short;)V";
               } else if (fieldType == byte.class) {
                  DESC_FIELD_CLASS = "B";
                  DESC_METHOD = "(B)V";
               } else if (fieldType == Byte.class) {
                  mwxxx.visitMethodInsn(184, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Byte;";
                  DESC_METHOD = "(Ljava/lang/Byte;)V";
               } else if (fieldType == int.class) {
                  DESC_FIELD_CLASS = "I";
                  DESC_METHOD = "(I)V";
               } else {
                  mwxxx.visitMethodInsn(184, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Integer;";
                  DESC_METHOD = "(Ljava/lang/Integer;)V";
               }
            } else if (fieldType == Long.class || fieldType == long.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "parseLong", bytes ? "([BII)J" : "([CII)J", false);
               if (fieldType == long.class) {
                  DESC_FIELD_CLASS = "J";
                  DESC_METHOD = "(J)V";
               } else {
                  mwxxx.visitMethodInsn(184, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Long;";
                  DESC_METHOD = "(Ljava/lang/Long;)V";
               }
            } else if (fieldType == Float.class || fieldType == float.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "parseFloat", bytes ? "([BII)F" : "([CII)F", false);
               if (fieldType == float.class) {
                  DESC_FIELD_CLASS = "F";
                  DESC_METHOD = "(F)V";
               } else {
                  mwxxx.visitMethodInsn(184, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Float;";
                  DESC_METHOD = "(Ljava/lang/Float;)V";
               }
            } else if (fieldType == Double.class || fieldType == double.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "parseDouble", bytes ? "([BII)D" : "([CII)D", false);
               if (fieldType == double.class) {
                  DESC_FIELD_CLASS = "D";
                  DESC_METHOD = "(D)V";
               } else {
                  mwxxx.visitMethodInsn(184, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                  DESC_FIELD_CLASS = "Ljava/lang/Double;";
                  DESC_METHOD = "(Ljava/lang/Double;)V";
               }
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "parseBoolean", bytes ? "([BII)Ljava/lang/Boolean;" : "([CII)Ljava/lang/Boolean;", false);
               if (fieldType == boolean.class) {
                  mwxxx.visitMethodInsn(182, "java/lang/Boolean", "booleanValue", "()Z", false);
                  DESC_FIELD_CLASS = "Z";
                  DESC_METHOD = "(Z)V";
               } else {
                  DESC_FIELD_CLASS = "Ljava/lang/Boolean;";
                  DESC_METHOD = "(Ljava/lang/Boolean;)V";
               }
            } else if (fieldType == Date.class) {
               mwxxx.visitTypeInsn(187, "java/util/Date");
               mwxxx.visitInsn(89);
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               if (bytes) {
                  mwxxx.visitVarInsn(25, 6);
                  mwxxx.visitMethodInsn(184, ASMUtils.TYPE_DATE_UTILS, "parseMillis", "([BIILjava/nio/charset/Charset;)J", false);
               } else {
                  mwxxx.visitMethodInsn(184, ASMUtils.TYPE_DATE_UTILS, "parseMillis", "([CII)J", false);
               }

               mwxxx.visitMethodInsn(183, "java/util/Date", "<init>", "(J)V", false);
               DESC_FIELD_CLASS = "Ljava/util/Date;";
               DESC_METHOD = "(Ljava/util/Date;)V";
            } else if (fieldType == BigDecimal.class) {
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               mwxxx.visitMethodInsn(
                  184, ASMUtils.TYPE_TYPE_UTILS, "parseBigDecimal", bytes ? "([BII)Ljava/math/BigDecimal;" : "([CII)Ljava/math/BigDecimal;", false
               );
               DESC_FIELD_CLASS = "Ljava/math/BigDecimal;";
               DESC_METHOD = "(Ljava/math/BigDecimal;)V";
            } else {
               mwxxx.visitTypeInsn(187, "java/lang/String");
               mwxxx.visitInsn(89);
               mwxxx.visitVarInsn(25, 3);
               mwxxx.visitVarInsn(21, 4);
               mwxxx.visitVarInsn(21, 5);
               if (bytes) {
                  mwxxx.visitVarInsn(25, 6);
                  mwxxx.visitMethodInsn(183, "java/lang/String", "<init>", "([BIILjava/nio/charset/Charset;)V", false);
               } else {
                  mwxxx.visitMethodInsn(183, "java/lang/String", "<init>", "([CII)V", false);
               }

               if (fieldType == String.class) {
                  DESC_FIELD_CLASS = "Ljava/lang/String;";
                  DESC_METHOD = "(Ljava/lang/String;)V";
               } else {
                  DESC_FIELD_CLASS = ASMUtils.desc(fieldClass);
                  if (fieldClass == char.class) {
                     DESC_METHOD = "(C)V";
                  } else {
                     DESC_METHOD = "(" + DESC_FIELD_CLASS + ")V";
                  }

                  mwxxx.visitLdcInsn(fieldClass);
                  mwxxx.visitMethodInsn(184, ASMUtils.TYPE_TYPE_UTILS, "cast", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false);
                  mwxxx.visitTypeInsn(192, ASMUtils.type(fieldClass));
               }
            }

            if (fieldReader.method != null) {
               if (fieldReader.method.getReturnType() != void.class) {
                  return null;
               }

               mwxxx.visitMethodInsn(182, TYPE_OBJECT, fieldReader.method.getName(), DESC_METHOD, false);
            } else {
               if (field == null) {
                  return null;
               }

               mwxxx.visitFieldInsn(181, TYPE_OBJECT, field.getName(), DESC_FIELD_CLASS);
            }

            mwxxx.visitJumpInsn(167, dflt);
         }

         mwxxx.visitLabel(dflt);
         mwxxx.visitInsn(177);
         mwxxx.visitMaxs(3, 3);
         byte[] code = cw.toByteArray();

         try {
            Class<?> consumerClass = this.classLoader.defineClassPublic(classNameFull, code, 0, code.length);
            Constructor<?> constructor = consumerClass.getConstructor(Consumer.class);
            return c -> {
               try {
                  return constructor.newInstance(c);
               } catch (IllegalAccessException | InvocationTargetException | InstantiationException var3x) {
                  throw new JSONException("create ByteArrayValueConsumer error", var3x);
               }
            };
         } catch (Throwable var33) {
            var33.printStackTrace();
            return null;
         }
      } else {
         return null;
      }
   }

   static {
      Package pkg = ObjectReaderCreatorASM.class.getPackage();
      packageName = pkg != null ? pkg.getName() : "";
      infos.put(
         boolean.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjBoolConsumer.class), "(Ljava/lang/Object;Z)V", "(Z)V", 21, "readFieldBoolValue", "()Z", 54)
      );
      infos.put(
         char.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjCharConsumer.class), "(Ljava/lang/Object;C)V", "(C)V", 21, "readInt32Value", "()C", 54)
      );
      infos.put(
         byte.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjByteConsumer.class), "(Ljava/lang/Object;B)V", "(B)V", 21, "readInt32Value", "()B", 54)
      );
      infos.put(
         short.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjShortConsumer.class), "(Ljava/lang/Object;S)V", "(S)V", 21, "readInt32Value", "()S", 54)
      );
      infos.put(
         int.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjIntConsumer.class), "(Ljava/lang/Object;I)V", "(I)V", 21, "readInt32Value", "()I", 54)
      );
      infos.put(
         long.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(ASMUtils.type(ObjLongConsumer.class), "(Ljava/lang/Object;J)V", "(J)V", 22, "readInt64Value", "()V", 55)
      );
      infos.put(
         float.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(
            ASMUtils.type(ObjFloatConsumer.class), "(Ljava/lang/Object;F)V", "(F)V", 23, "readFieldFloatValue", "()F", 56
         )
      );
      infos.put(
         double.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(
            ASMUtils.type(ObjDoubleConsumer.class), "(Ljava/lang/Object;D)V", "(D)V", 24, "readFloatDoubleValue", "()D", 57
         )
      );
      infos.put(
         String.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(
            ASMUtils.type(BiConsumer.class), "(Ljava/lang/Object;Ljava/lang/Object;)V", "(Ljava/lang/String;)V", 25, "readString", "()Ljava/lang/String;", 58
         )
      );
      infos.put(
         Integer.class,
         new ObjectReaderCreatorASM.FieldReaderInfo(
            ASMUtils.type(BiConsumer.class), "(Ljava/lang/Object;Ljava/lang/Integer;)V", "(Ljava/lang/Integer;)V", 25, "readInt32", "()Ljava/lang/Integer;", 58
         )
      );
   }

   private static class FieldReaderInfo {
      final String interfaceDesc;
      final String acceptDesc;
      final String setterDesc;
      final int loadCode;
      final String readMethodName;
      final String readMethodDesc;
      final int storeCode;

      FieldReaderInfo(String interfaceDesc, String acceptDesc, String setterDesc, int loadCode, String readMethodName, String readMethodDesc, int storeCode) {
         this.interfaceDesc = interfaceDesc;
         this.acceptDesc = acceptDesc;
         this.setterDesc = setterDesc;
         this.loadCode = loadCode;
         this.readMethodName = readMethodName;
         this.readMethodDesc = readMethodDesc;
         this.storeCode = storeCode;
      }
   }

   static class ObjectWriteContext {
      final BeanInfo beanInfo;
      final Class objectClass;
      final ClassWriter cw;
      final boolean publicClass;
      final boolean externalClass;
      final FieldReader[] fieldReaders;
      final boolean hasStringField;
      final int fieldNameLengthMin;
      final int fieldNameLengthMax;

      public ObjectWriteContext(BeanInfo beanInfo, Class objectClass, ClassWriter cw, boolean externalClass, FieldReader[] fieldReaders) {
         this.beanInfo = beanInfo;
         this.objectClass = objectClass;
         this.cw = cw;
         this.publicClass = objectClass == null || Modifier.isPublic(objectClass.getModifiers());
         this.externalClass = externalClass;
         this.fieldReaders = fieldReaders;
         int fieldNameLengthMin = 0;
         int fieldNameLengthMax = 0;
         boolean hasStringField = false;

         for (int i = 0; i < fieldReaders.length; i++) {
            FieldReader fieldReader = fieldReaders[i];
            if (fieldReader.fieldClass == String.class) {
               hasStringField = true;
            }

            byte[] nameUTF8 = fieldReader.fieldName.getBytes(StandardCharsets.UTF_8);
            int fieldNameLength = nameUTF8.length;

            for (byte ch : nameUTF8) {
               if (ch <= 0) {
                  fieldNameLength = -1;
                  break;
               }
            }

            if (i == 0) {
               fieldNameLengthMin = fieldNameLength;
               fieldNameLengthMax = fieldNameLength;
            } else {
               fieldNameLengthMin = Math.min(fieldNameLength, fieldNameLengthMin);
               fieldNameLengthMax = Math.max(fieldNameLength, fieldNameLengthMax);
            }
         }

         this.hasStringField = hasStringField;
         this.fieldNameLengthMin = fieldNameLengthMin;
         this.fieldNameLengthMax = fieldNameLengthMax;
      }

      public boolean disableSupportArrayMapping() {
         return (this.beanInfo.readerFeatures & 288230376151711744L) != 0L;
      }

      public boolean disableReferenceDetect() {
         return (this.beanInfo.readerFeatures & 144115188075855872L) != 0L;
      }

      public boolean disableAutoType() {
         return (this.beanInfo.readerFeatures & 576460752303423488L) != 0L;
      }

      public boolean disableJSONB() {
         return (this.beanInfo.readerFeatures & 1152921504606846976L) != 0L;
      }

      public boolean disableSmartMatch() {
         return (this.beanInfo.readerFeatures & 9007199254740992L) != 0L;
      }
   }
}
