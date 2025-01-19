package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.SymbolTable;
import com.alibaba.fastjson2.codec.BeanInfo;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.internal.asm.ClassWriter;
import com.alibaba.fastjson2.internal.asm.Label;
import com.alibaba.fastjson2.internal.asm.MethodWriter;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.DynamicClassLoader;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.Function;

public class ObjectWriterCreatorASM extends ObjectWriterCreator {
   public static final ObjectWriterCreatorASM INSTANCE = new ObjectWriterCreatorASM(DynamicClassLoader.getInstance());
   protected static final AtomicLong seed = new AtomicLong();
   protected final DynamicClassLoader classLoader;
   static final String[] INTERFACES = new String[]{ASMUtils.TYPE_OBJECT_WRITER};
   static final String METHOD_DESC_WRITE_VALUE = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Object;)V";
   static final String METHOD_DESC_WRITE = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;J)V";
   static final String METHOD_DESC_WRITE_FIELD_NAME = "(" + ASMUtils.DESC_JSON_WRITER + ")V";
   static final String METHOD_DESC_WRITE_OBJECT = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;J)V";
   static final String METHOD_DESC_WRITE_J = "(" + ASMUtils.DESC_JSON_WRITER + "J)V";
   static final String METHOD_DESC_WRITE_D = "(" + ASMUtils.DESC_JSON_WRITER + "D)V";
   static final String METHOD_DESC_WRITE_F = "(" + ASMUtils.DESC_JSON_WRITER + "F)V";
   static final String METHOD_DESC_WRITE_DATE_WITH_FIELD_NAME = "(" + ASMUtils.DESC_JSON_WRITER + "ZLjava/util/Date;)V";
   static final String METHOD_DESC_WRITE_Z = "(" + ASMUtils.DESC_JSON_WRITER + "Z)V";
   static final String METHOD_DESC_WRITE_ZARRAY = "(" + ASMUtils.DESC_JSON_WRITER + "[Z)V";
   static final String METHOD_DESC_WRITE_FARRAY = "(" + ASMUtils.DESC_JSON_WRITER + "[F)V";
   static final String METHOD_DESC_WRITE_DARRAY = "(" + ASMUtils.DESC_JSON_WRITER + "[D)V";
   static final String METHOD_DESC_WRITE_I = "(" + ASMUtils.DESC_JSON_WRITER + "I)V";
   static final String METHOD_DESC_WRITE_SArray = "(" + ASMUtils.DESC_JSON_WRITER + "[S)V";
   static final String METHOD_DESC_WRITE_BArray = "(" + ASMUtils.DESC_JSON_WRITER + "[B)V";
   static final String METHOD_DESC_WRITE_CArray = "(" + ASMUtils.DESC_JSON_WRITER + "[C)V";
   static final String METHOD_DESC_WRITE_ENUM = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Enum;)V";
   static final String METHOD_DESC_WRITE_LIST = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/util/List;)V";
   static final String METHOD_DESC_FIELD_WRITE_OBJECT = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Object;)Z";
   static final String METHOD_DESC_GET_OBJECT_WRITER = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/Class;)" + ASMUtils.DESC_OBJECT_WRITER;
   static final String METHOD_DESC_GET_ITEM_WRITER = "(" + ASMUtils.DESC_JSON_WRITER + "Ljava/lang/reflect/Type;)" + ASMUtils.DESC_OBJECT_WRITER;
   static final String METHOD_DESC_WRITE_TYPE_INFO = "(" + ASMUtils.DESC_JSON_WRITER + ")Z";
   static final String METHOD_DESC_HAS_FILTER = "(" + ASMUtils.DESC_JSON_WRITER + ")Z";
   static final String METHOD_DESC_SET_PATH2 = "(" + ASMUtils.DESC_FIELD_WRITER + "Ljava/lang/Object;)Ljava/lang/String;";
   static final String METHOD_DESC_WRITE_REFERENCE = "(Ljava/lang/String;)V";
   static final String METHOD_DESC_WRITE_CLASS_INFO = "(" + ASMUtils.DESC_JSON_WRITER + ")V";
   static final String DESC_SYMBOL = ASMUtils.desc(SymbolTable.class);
   static final int THIS = 0;
   static final int JSON_WRITER = 1;
   static final String NOT_WRITE_DEFAULT_VALUE = "WRITE_DEFAULT_VALUE";
   static final String WRITE_NULLS = "WRITE_NULLS";
   static final String CONTEXT_FEATURES = "CONTEXT_FEATURES";
   static final String NAME_DIRECT = "NAME_DIRECT";

   static String fieldWriter(int i) {
      switch (i) {
         case 0:
            return "fieldWriter0";
         case 1:
            return "fieldWriter1";
         case 2:
            return "fieldWriter2";
         case 3:
            return "fieldWriter3";
         case 4:
            return "fieldWriter4";
         case 5:
            return "fieldWriter5";
         case 6:
            return "fieldWriter6";
         case 7:
            return "fieldWriter7";
         case 8:
            return "fieldWriter8";
         case 9:
            return "fieldWriter9";
         case 10:
            return "fieldWriter10";
         case 11:
            return "fieldWriter11";
         case 12:
            return "fieldWriter12";
         case 13:
            return "fieldWriter13";
         case 14:
            return "fieldWriter14";
         case 15:
            return "fieldWriter15";
         default:
            String base = "fieldWriter";
            int size = IOUtils.stringSize(i);
            char[] chars = new char[base.length() + size];
            base.getChars(0, base.length(), chars, 0);
            IOUtils.getChars(i, chars.length, chars);
            return new String(chars);
      }
   }

   public ObjectWriterCreatorASM() {
      this.classLoader = new DynamicClassLoader();
   }

   public ObjectWriterCreatorASM(ClassLoader classLoader) {
      this.classLoader = classLoader instanceof DynamicClassLoader ? (DynamicClassLoader)classLoader : new DynamicClassLoader(classLoader);
   }

   @Override
   public ObjectWriter createObjectWriter(List<FieldWriter> fieldWriters) {
      boolean allFunction = true;

      for (int i = 0; i < fieldWriters.size(); i++) {
         if (fieldWriters.get(i).getFunction() == null) {
            allFunction = false;
            break;
         }
      }

      if (!allFunction) {
         return super.createObjectWriter(fieldWriters);
      } else {
         ObjectWriterProvider provider = JSONFactory.getDefaultObjectWriterProvider();
         BeanInfo beanInfo = provider.createBeanInfo();
         return this.jitWriter(null, provider, beanInfo, fieldWriters, 0L);
      }
   }

   @Override
   public ObjectWriter createObjectWriter(Class objectClass, long features, ObjectWriterProvider provider) {
      int modifiers = objectClass.getModifiers();
      boolean externalClass = this.classLoader.isExternalClass(objectClass);
      boolean publicClass = Modifier.isPublic(modifiers);
      BeanInfo beanInfo = provider.createBeanInfo();
      provider.getBeanInfo(beanInfo, objectClass);
      if (beanInfo.serializer != null && ObjectWriter.class.isAssignableFrom(beanInfo.serializer)) {
         try {
            Constructor constructor = beanInfo.serializer.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ObjectWriter)constructor.newInstance();
         } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException var22) {
            throw new JSONException("create serializer error", var22);
         }
      } else {
         long beanFeatures = beanInfo.writerFeatures;
         if (beanInfo.seeAlso != null) {
            beanFeatures &= ~JSONWriter.Feature.WriteClassName.mask;
         }

         long writerFieldFeatures = features | beanFeatures;
         boolean fieldBased = (writerFieldFeatures & JSONWriter.Feature.FieldBased.mask) != 0L && !objectClass.isInterface() || !beanInfo.alphabetic;
         if (!Throwable.class.isAssignableFrom(objectClass) && !BeanUtils.isExtendedMap(objectClass) && beanInfo.rootName == null) {
            boolean record = BeanUtils.isRecord(objectClass);
            Map<String, FieldWriter> fieldWriterMap = new LinkedHashMap<>();
            if (fieldBased && !record) {
               FieldInfo fieldInfo = new FieldInfo();
               BeanUtils.declaredFields(objectClass, field -> {
                  fieldInfo.init();
                  FieldWriter fieldWriterx = this.creteFieldWriter(objectClass, writerFieldFeatures, provider, beanInfo, fieldInfo, field);
                  if (fieldWriterx != null) {
                     fieldWriterMap.put(fieldWriterx.fieldName, fieldWriterx);
                  }
               });
            } else {
               List<FieldWriter> fieldWriterList = new ArrayList<>();
               boolean fieldWritersCreated = false;

               for (ObjectWriterModule module : provider.modules) {
                  if (module.createFieldWriters(this, objectClass, fieldWriterList)) {
                     fieldWritersCreated = true;
                     break;
                  }
               }

               if (fieldWritersCreated) {
                  for (FieldWriter fieldWriter : fieldWriterList) {
                     Method method = fieldWriter.method;
                     if (method == null) {
                        return super.createObjectWriter(objectClass, writerFieldFeatures, provider);
                     }

                     fieldWriterMap.putIfAbsent(fieldWriter.fieldName, fieldWriter);
                  }
               } else {
                  FieldInfo fieldInfo = new FieldInfo();
                  if (!record) {
                     BeanUtils.declaredFields(objectClass, field -> {
                        fieldInfo.init();
                        fieldInfo.ignore = (field.getModifiers() & 1) == 0 || (field.getModifiers() & 128) != 0;
                        FieldWriter fieldWriterx = this.creteFieldWriter(objectClass, writerFieldFeatures, provider, beanInfo, fieldInfo, field);
                        if (fieldWriterx != null) {
                           FieldWriter origin = fieldWriterMap.putIfAbsent(fieldWriterx.fieldName, fieldWriterx);
                           if (origin != null) {
                              int cmp = origin.compareTo(fieldWriterx);
                              if (cmp > 0) {
                                 fieldWriterMap.put(fieldWriterx.fieldName, fieldWriterx);
                              }
                           }
                        }
                     });
                  }

                  Class mixIn = provider.getMixIn(objectClass);
                  BeanUtils.getters(
                     objectClass,
                     mixIn,
                     beanInfo.kotlin,
                     method -> {
                        fieldInfo.init();
                        fieldInfo.features |= writerFieldFeatures;
                        fieldInfo.format = beanInfo.format;
                        provider.getFieldInfo(beanInfo, fieldInfo, objectClass, method);
                        if (!fieldInfo.ignore) {
                           String fieldName = getFieldName(objectClass, provider, beanInfo, record, fieldInfo, method);
                           if (beanInfo.orders != null) {
                              boolean matchxx = false;

                              for (int i = 0; i < beanInfo.orders.length; i++) {
                                 if (fieldName.equals(beanInfo.orders[i])) {
                                    fieldInfo.ordinal = i;
                                    matchxx = true;
                                 }
                              }

                              if (!matchxx && fieldInfo.ordinal == 0) {
                                 fieldInfo.ordinal = beanInfo.orders.length;
                              }
                           }

                           if (beanInfo.includes != null && beanInfo.includes.length > 0) {
                              boolean matchx = false;

                              for (String include : beanInfo.includes) {
                                 if (include.equals(fieldName)) {
                                    matchx = true;
                                    break;
                                 }
                              }

                              if (!matchx) {
                                 return;
                              }
                           }

                           if ((beanInfo.writerFeatures & JSONWriter.Feature.WriteClassName.mask) == 0L || !fieldName.equals(beanInfo.typeKey)) {
                              Class<?> returnType = method.getReturnType();
                              if (!TypeUtils.isFunction(returnType) && returnType != void.class) {
                                 method.setAccessible(true);
                                 ObjectWriter writeUsingWriter = null;
                                 if (fieldInfo.writeUsing != null) {
                                    try {
                                       Constructor<?> constructor = fieldInfo.writeUsing.getDeclaredConstructor();
                                       constructor.setAccessible(true);
                                       writeUsingWriter = (ObjectWriter)constructor.newInstance();
                                    } catch (Exception var17x) {
                                       throw new JSONException(
                                          "create writeUsing Writer error, method " + method.getName() + ", serializer " + fieldInfo.writeUsing.getName(),
                                          var17x
                                       );
                                    }
                                 }

                                 if (writeUsingWriter == null && fieldInfo.fieldClassMixIn) {
                                    writeUsingWriter = ObjectWriterBaseModule.VoidObjectWriter.INSTANCE;
                                 }

                                 FieldWriter fieldWriterx = null;
                                 boolean jit = (fieldInfo.features & 18014398509481984L) != 0L;
                                 if (jit) {
                                    try {
                                       fieldWriterx = this.createFieldWriterLambda(
                                          provider,
                                          objectClass,
                                          fieldName,
                                          fieldInfo.ordinal,
                                          fieldInfo.features,
                                          fieldInfo.format,
                                          fieldInfo.label,
                                          method,
                                          writeUsingWriter
                                       );
                                    } catch (Throwable var16x) {
                                       this.jitErrorCount.incrementAndGet();
                                       this.jitErrorLast = var16x;
                                    }
                                 }

                                 if (fieldWriterx == null) {
                                    fieldWriterx = this.createFieldWriter(
                                       provider,
                                       objectClass,
                                       fieldName,
                                       fieldInfo.ordinal,
                                       fieldInfo.features,
                                       fieldInfo.format,
                                       fieldInfo.locale,
                                       fieldInfo.label,
                                       method,
                                       writeUsingWriter
                                    );
                                 }

                                 FieldWriter origin = fieldWriterMap.putIfAbsent(fieldName, fieldWriterx);
                                 if (origin != null && origin.compareTo(fieldWriterx) > 0) {
                                    fieldWriterMap.put(fieldName, fieldWriterx);
                                 }
                              }
                           }
                        }
                     }
                  );
               }
            }

            List<FieldWriter> fieldWriters = new ArrayList<>(fieldWriterMap.values());
            this.handleIgnores(beanInfo, fieldWriters);
            if (beanInfo.alphabetic) {
               try {
                  Collections.sort(fieldWriters);
               } catch (Exception var23) {
                  StringBuilder msg = new StringBuilder("fieldWriters sort error, objectClass ").append(objectClass.getName()).append(", fields ");
                  JSONArray array = new JSONArray();

                  for (FieldWriter fieldWriter : fieldWriters) {
                     array.add(
                        JSONObject.of(
                           "name",
                           fieldWriter.fieldName,
                           "type",
                           fieldWriter.fieldClass,
                           "ordinal",
                           fieldWriter.ordinal,
                           "field",
                           fieldWriter.field,
                           "method",
                           fieldWriter.method
                        )
                     );
                  }

                  msg.append(array);
                  throw new JSONException(msg.toString(), var23);
               }
            }

            boolean match = fieldWriters.size() < 100 && !Throwable.class.isAssignableFrom(objectClass);
            if (!publicClass || externalClass) {
               for (FieldWriter fieldWriter : fieldWriters) {
                  if (fieldWriter.method != null) {
                     match = false;
                     break;
                  }
               }
            }

            for (FieldWriter fieldWriterx : fieldWriters) {
               if (fieldWriterx.getInitWriter() != null
                  || (fieldWriterx.features & 281474976710656L) != 0L
                  || (fieldWriterx.features & 1125899906842624L) != 0L) {
                  match = false;
                  break;
               }
            }

            if (objectClass.getSuperclass() == Object.class) {
               String simpleName = objectClass.getSimpleName();
               if (simpleName.indexOf(36) != -1 && simpleName.contains("$$")) {
                  match = false;
               }
            }

            long writerFeatures = features | beanInfo.writerFeatures;
            if (!match) {
               return super.createObjectWriter(objectClass, features, provider);
            } else {
               this.setDefaultValue(fieldWriters, objectClass);
               return this.jitWriter(objectClass, provider, beanInfo, fieldWriters, writerFeatures);
            }
         } else {
            return super.createObjectWriter(objectClass, features, provider);
         }
      }
   }

   private ObjectWriterAdapter jitWriter(
      Class objectClass, ObjectWriterProvider provider, BeanInfo beanInfo, List<FieldWriter> fieldWriters, long writerFeatures
   ) {
      ClassWriter cw = new ClassWriter(null);
      String className = "OWG_" + seed.incrementAndGet() + "_" + fieldWriters.size() + (objectClass == null ? "" : "_" + objectClass.getSimpleName());
      Package pkg = ObjectWriterCreatorASM.class.getPackage();
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

      String objectWriterSupper;
      switch (fieldWriters.size()) {
         case 1:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_1;
            break;
         case 2:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_2;
            break;
         case 3:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_3;
            break;
         case 4:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_4;
            break;
         case 5:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_5;
            break;
         case 6:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_6;
            break;
         case 7:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_7;
            break;
         case 8:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_8;
            break;
         case 9:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_9;
            break;
         case 10:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_10;
            break;
         case 11:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_11;
            break;
         case 12:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_12;
            break;
         default:
            objectWriterSupper = ASMUtils.TYPE_OBJECT_WRITER_ADAPTER;
      }

      cw.visit(52, 49, classNameType, objectWriterSupper, INTERFACES);
      this.genFields(fieldWriters, cw, objectWriterSupper);
      this.genMethodInit(fieldWriters, cw, classNameType, objectWriterSupper);
      boolean disableJSONB = (writerFeatures & 1152921504606846976L) != 0L;
      boolean disableArrayMapping = (writerFeatures & 288230376151711744L) != 0L;
      if (!disableJSONB) {
         this.genMethodWriteJSONB(provider, objectClass, fieldWriters, cw, classNameType, writerFeatures);
      }

      if ((writerFeatures & JSONWriter.Feature.BeanToArray.mask) != 0L && !disableJSONB) {
         this.genMethodWriteArrayMapping(provider, "write", objectClass, writerFeatures, fieldWriters, cw, classNameType);
      } else {
         this.genMethodWrite(provider, objectClass, fieldWriters, cw, classNameType, writerFeatures);
      }

      if (!disableJSONB) {
         this.genMethodWriteArrayMappingJSONB(provider, objectClass, writerFeatures, fieldWriters, cw, classNameType, writerFeatures);
      }

      if (!disableArrayMapping) {
         this.genMethodWriteArrayMapping(provider, "writeArrayMapping", objectClass, writerFeatures, fieldWriters, cw, classNameType);
      }

      byte[] code = cw.toByteArray();
      Class<?> deserClass = this.classLoader.defineClassPublic(classNameFull, code, 0, code.length);

      try {
         Constructor<?> constructor = deserClass.getConstructor(Class.class, String.class, String.class, long.class, List.class);
         ObjectWriterAdapter objectWriter = (ObjectWriterAdapter)constructor.newInstance(
            objectClass, beanInfo.typeKey, beanInfo.typeName, writerFeatures, fieldWriters
         );
         if (beanInfo.serializeFilters != null) {
            configSerializeFilters(beanInfo, objectWriter);
         }

         return objectWriter;
      } catch (Throwable var19) {
         throw new JSONException("create objectWriter error, objectType " + objectClass, var19);
      }
   }

   private void genMethodWrite(
      ObjectWriterProvider provider, Class objectType, List<FieldWriter> fieldWriters, ClassWriter cw, String classNameType, long objectFeatures
   ) {
      boolean disableJSONB = (objectFeatures & 1152921504606846976L) != 0L;
      boolean disableArrayMapping = (objectFeatures & 288230376151711744L) != 0L;
      boolean disableAutoType = (objectFeatures & 576460752303423488L) != 0L;
      MethodWriter mw = cw.visitMethod(1, "write", METHOD_DESC_WRITE, fieldWriters.size() < 6 ? 512 : 1024);
      int OBJECT = 2;
      int FIELD_NAME = 3;
      int FIELD_TYPE = 4;
      int FIELD_FEATURES = 5;
      int COMMA = 7;
      Label notSuper_ = new Label();
      ObjectWriterCreatorASM.MethodWriterContext mwc = new ObjectWriterCreatorASM.MethodWriterContext(
         provider, objectType, objectFeatures, classNameType, mw, 8, false
      );
      mwc.genVariantsMethodBefore(false);
      mwc.genIsEnabled(JSONWriter.Feature.IgnoreErrorGetter.mask | JSONWriter.Feature.UnquoteFieldName.mask, notSuper_);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 2);
      mw.visitVarInsn(25, 3);
      mw.visitVarInsn(25, 4);
      mw.visitVarInsn(22, 5);
      mw.visitMethodInsn(183, ASMUtils.TYPE_OBJECT_WRITER_ADAPTER, "write", METHOD_DESC_WRITE_OBJECT, false);
      mw.visitInsn(177);
      mw.visitLabel(notSuper_);
      if (!disableJSONB) {
         Label json_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitFieldInsn(180, ASMUtils.TYPE_JSON_WRITER, "jsonb", "Z");
         mw.visitJumpInsn(153, json_);
         if (!disableArrayMapping) {
            Label jsonb_ = new Label();
            mwc.genIsEnabled(JSONWriter.Feature.BeanToArray.mask, jsonb_);
            mw.visitVarInsn(25, 0);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, 2);
            mw.visitVarInsn(25, 3);
            mw.visitVarInsn(25, 4);
            mw.visitVarInsn(22, 5);
            mw.visitMethodInsn(182, classNameType, "writeArrayMappingJSONB", METHOD_DESC_WRITE_OBJECT, false);
            mw.visitInsn(177);
            mw.visitLabel(jsonb_);
         }

         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 2);
         mw.visitVarInsn(25, 3);
         mw.visitVarInsn(25, 4);
         mw.visitVarInsn(22, 5);
         mw.visitMethodInsn(182, classNameType, "writeJSONB", METHOD_DESC_WRITE_OBJECT, false);
         mw.visitInsn(177);
         mw.visitLabel(json_);
      }

      if (!disableArrayMapping) {
         Label checkFilter_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.BeanToArray.mask, checkFilter_);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 2);
         mw.visitVarInsn(25, 3);
         mw.visitVarInsn(25, 4);
         mw.visitVarInsn(22, 5);
         mw.visitMethodInsn(182, classNameType, "writeArrayMapping", METHOD_DESC_WRITE_OBJECT, false);
         mw.visitInsn(177);
         mw.visitLabel(checkFilter_);
      }

      Label object_ = new Label();
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, "hasFilter", METHOD_DESC_HAS_FILTER, true);
      mw.visitJumpInsn(153, object_);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 2);
      mw.visitVarInsn(25, 3);
      mw.visitVarInsn(25, 4);
      mw.visitVarInsn(22, 5);
      mw.visitMethodInsn(182, classNameType, "writeWithFilter", METHOD_DESC_WRITE_OBJECT, false);
      mw.visitInsn(177);
      mw.visitLabel(object_);
      Label return_ = new Label();
      if (objectType == null || !Serializable.class.isAssignableFrom(objectType)) {
         Label endIgnoreNoneSerializable_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.IgnoreNoneSerializable.mask, endIgnoreNoneSerializable_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
         mw.visitJumpInsn(167, return_);
         mw.visitLabel(endIgnoreNoneSerializable_);
         Label endErrorOnNoneSerializable_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.ErrorOnNoneSerializable.mask, endErrorOnNoneSerializable_);
         mw.visitVarInsn(25, 0);
         mw.visitMethodInsn(182, mwc.classNameType, "errorOnNoneSerializable", "()V", false);
         mw.visitJumpInsn(167, return_);
         mw.visitLabel(endErrorOnNoneSerializable_);
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startObject", "()V", false);
      if (!disableAutoType) {
         mw.visitInsn(4);
         mw.visitVarInsn(54, 7);
         Label writeFields_ = new Label();
         isWriteTypeInfo(objectFeatures, mw, 2, 4, 5, writeFields_);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, "writeTypeInfo", METHOD_DESC_WRITE_TYPE_INFO, true);
         mw.visitInsn(4);
         mw.visitInsn(130);
         mw.visitVarInsn(54, 7);
         mw.visitLabel(writeFields_);
      }

      for (int i = 0; i < fieldWriters.size(); i++) {
         FieldWriter fieldWriter = fieldWriters.get(i);
         this.gwFieldValue(mwc, fieldWriter, 2, i);
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "endObject", "()V", false);
      mw.visitLabel(return_);
      mw.visitInsn(177);
      mw.visitMaxs(mwc.maxVariant + 1, mwc.maxVariant + 1);
   }

   private static void isWriteTypeInfo(long objectFeatures, MethodWriter mw, int OBJECT, int FIELD_TYPE, int FEILD_FEATURE, Label notWriteType) {
      if ((objectFeatures & JSONWriter.Feature.WriteClassName.mask) == 0L || (objectFeatures & JSONWriter.Feature.NotWriteRootClassName.mask) != 0L) {
         mw.visitVarInsn(25, OBJECT);
         mw.visitJumpInsn(198, notWriteType);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
         mw.visitVarInsn(25, FIELD_TYPE);
         mw.visitJumpInsn(165, notWriteType);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, OBJECT);
         mw.visitVarInsn(25, FIELD_TYPE);
         mw.visitVarInsn(22, FEILD_FEATURE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isWriteTypeInfo", "(Ljava/lang/Object;Ljava/lang/reflect/Type;J)Z", false);
         mw.visitJumpInsn(153, notWriteType);
      }
   }

   private void genMethodWriteJSONB(
      ObjectWriterProvider provider, Class objectType, List<FieldWriter> fieldWriters, ClassWriter cw, String classNameType, long objectFeatures
   ) {
      MethodWriter mw = cw.visitMethod(1, "writeJSONB", METHOD_DESC_WRITE, fieldWriters.size() < 6 ? 512 : 1024);
      int OBJECT = 2;
      int FIELD_NAME = 3;
      int FIELD_TYPE = 4;
      int FIELD_FEATURES = 5;
      ObjectWriterCreatorASM.MethodWriterContext mwc = new ObjectWriterCreatorASM.MethodWriterContext(
         provider, objectType, objectFeatures, classNameType, mw, 7, true
      );
      mwc.genVariantsMethodBefore(true);
      Label return_ = new Label();
      if (objectType == null || !Serializable.class.isAssignableFrom(objectType)) {
         Label endIgnoreNoneSerializable_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.IgnoreNoneSerializable.mask, endIgnoreNoneSerializable_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
         mw.visitJumpInsn(167, return_);
         mw.visitLabel(endIgnoreNoneSerializable_);
         Label endErrorOnNoneSerializable_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.ErrorOnNoneSerializable.mask, endErrorOnNoneSerializable_);
         mw.visitVarInsn(25, 0);
         mw.visitMethodInsn(182, mwc.classNameType, "errorOnNoneSerializable", "()V", false);
         mw.visitJumpInsn(167, return_);
         mw.visitLabel(endErrorOnNoneSerializable_);
      }

      if ((objectFeatures & 576460752303423488L) == 0L) {
         Label notWriteType = new Label();
         isWriteTypeInfo(objectFeatures, mw, 2, 4, 5, notWriteType);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, classNameType, "writeClassInfo", METHOD_DESC_WRITE_CLASS_INFO, false);
         mw.visitLabel(notWriteType);
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startObject", "()V", false);

      for (int i = 0; i < fieldWriters.size(); i++) {
         FieldWriter fieldWriter = fieldWriters.get(i);
         this.gwFieldValueJSONB(mwc, fieldWriter, 2, i);
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "endObject", "()V", false);
      mw.visitLabel(return_);
      mw.visitInsn(177);
      mw.visitMaxs(mwc.maxVariant + 1, mwc.maxVariant + 1);
   }

   private void genMethodWriteArrayMappingJSONB(
      ObjectWriterProvider provider, Class objectType, long objectFeatures, List<FieldWriter> fieldWriters, ClassWriter cw, String classNameType, long features
   ) {
      MethodWriter mw = cw.visitMethod(1, "writeArrayMappingJSONB", METHOD_DESC_WRITE, 512);
      int OBJECT = 2;
      int FIELD_NAME = 3;
      int FIELD_TYPE = 4;
      int FIELD_FEATURES = 5;
      if ((features & 576460752303423488L) == 0L) {
         Label notWriteType = new Label();
         isWriteTypeInfo(objectFeatures, mw, 2, 4, 5, notWriteType);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, classNameType, "writeClassInfo", METHOD_DESC_WRITE_CLASS_INFO, false);
         mw.visitLabel(notWriteType);
      }

      int size = fieldWriters.size();
      mw.visitVarInsn(25, 1);
      if (size <= 15) {
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startArray" + size, "()V", false);
      } else {
         if (size >= 128) {
            mw.visitIntInsn(17, size);
         } else {
            mw.visitIntInsn(16, size);
         }

         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startArray", "(I)V", false);
      }

      ObjectWriterCreatorASM.MethodWriterContext mwc = new ObjectWriterCreatorASM.MethodWriterContext(
         provider, objectType, objectFeatures, classNameType, mw, 7, true
      );
      mwc.genVariantsMethodBefore(true);

      for (int i = 0; i < size; i++) {
         FieldWriter fieldWriter = fieldWriters.get(i);
         this.gwValueJSONB(mwc, fieldWriter, 2, i);
      }

      mw.visitInsn(177);
      mw.visitMaxs(mwc.maxVariant + 1, mwc.maxVariant + 1);
   }

   private void gwValueFZF(
      ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter0, FieldWriter fieldWriter1, FieldWriter fieldWriter2, int OBJECT, int i
   ) {
      MethodWriter mw = mwc.mw;
      mw.visitVarInsn(25, 1);
      this.genGetObject(mwc, fieldWriter0, i, OBJECT);
      this.genGetObject(mwc, fieldWriter1, i + 1, OBJECT);
      this.genGetObject(mwc, fieldWriter2, i + 2, OBJECT);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeFZF", "(FZF)V", false);
   }

   private void gwValueJSONB(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      long features = fieldWriter.features | mwc.objectFeatures;
      Class<?> fieldClass = fieldWriter.fieldClass;
      boolean beanToArray = (features & JSONWriter.Feature.BeanToArray.mask) != 0L;
      boolean userDefineWriter = false;
      if ((fieldClass == long.class || fieldClass == Long.class || fieldClass == long[].class) && (mwc.provider.userDefineMask & 4L) != 0L) {
         userDefineWriter = mwc.provider.getObjectWriter(Long.class) != ObjectWriterImplInt64.INSTANCE;
      }

      if (fieldClass == boolean.class
         || fieldClass == boolean[].class
         || fieldClass == char.class
         || fieldClass == char[].class
         || fieldClass == byte.class
         || fieldClass == byte[].class
         || fieldClass == short.class
         || fieldClass == short[].class
         || fieldClass == int.class
         || fieldClass == int[].class
         || fieldClass == long.class
         || fieldClass == long[].class && !userDefineWriter
         || fieldClass == float.class
         || fieldClass == float[].class
         || fieldClass == double.class
         || fieldClass == double[].class
         || fieldClass == String.class
         || fieldClass == Integer.class
         || fieldClass == Long.class
         || fieldClass == BigDecimal.class
         || fieldClass.isEnum()) {
         this.gwValue(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Date.class) {
         this.gwDate(mwc, fieldWriter, OBJECT, i);
      } else if (fieldWriter instanceof FieldWriterList) {
         this.gwListJSONB(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass.isArray()) {
         this.gwObjectA(mwc, fieldWriter, OBJECT, i);
      } else {
         this.gwObjectJSONB(fieldWriter, OBJECT, mwc, i, beanToArray);
      }
   }

   private void gwObjectJSONB(FieldWriter fieldWriter, int OBJECT, ObjectWriterCreatorASM.MethodWriterContext mwc, int i, boolean beanToArray) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      String fieldName = fieldWriter.fieldName;
      String classNameType = mwc.classNameType;
      MethodWriter mw = mwc.mw;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      boolean refDetection = !mwc.disableSupportArrayMapping() && !ObjectWriterProvider.isNotReferenceDetect(fieldClass);
      if (refDetection) {
         int REF_PATH = mwc.var("REF_PATH");
         Label endDetect_ = new Label();
         Label refSetPath_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.ReferenceDetection.mask, endDetect_);
         mw.visitVarInsn(25, OBJECT);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitJumpInsn(166, refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitLdcInsn("..");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitJumpInsn(167, endIfNull_);
         mw.visitLabel(refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "setPath", METHOD_DESC_SET_PATH2, false);
         mw.visitInsn(89);
         mw.visitVarInsn(58, REF_PATH);
         mw.visitJumpInsn(198, endDetect_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, REF_PATH);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         mw.visitJumpInsn(167, endIfNull_);
         mw.visitLabel(endDetect_);
      }

      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "getObjectWriter", METHOD_DESC_GET_OBJECT_WRITER, false);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitLdcInsn(fieldName);
      mwc.loadFieldType(i, fieldWriter.fieldType);
      mw.visitLdcInsn(fieldWriter.features);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, beanToArray ? "writeJSONB" : "writeArrayMappingJSONB", METHOD_DESC_WRITE_OBJECT, true);
      if (refDetection) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
      }

      mw.visitLabel(endIfNull_);
   }

   private void gwListJSONB(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean disableReferenceDetect = mwc.disableReferenceDetect();
      Type fieldType = fieldWriter.fieldType;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String classNameType = mwc.classNameType;
      MethodWriter mw = mwc.mw;
      int LIST = mwc.var(fieldClass);
      int REF_PATH = mwc.var("REF_PATH");
      boolean listSimple = false;
      Type itemType = null;
      Class itemClass = null;
      if (fieldType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)fieldType;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            itemType = actualTypeArguments[0];
            itemClass = TypeUtils.getClass(itemType);
            listSimple = itemType == String.class || itemType == Integer.class || itemType == Long.class;
         }
      }

      Label endIfListNull_ = new Label();
      Label listNotNull_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, LIST);
      mw.visitJumpInsn(199, listNotNull_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitJumpInsn(167, endIfListNull_);
      mw.visitLabel(listNotNull_);
      if (!disableReferenceDetect) {
         Label endDetect_ = new Label();
         Label refSetPath_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.ReferenceDetection.mask, endDetect_);
         mw.visitVarInsn(25, OBJECT);
         mw.visitVarInsn(25, LIST);
         mw.visitJumpInsn(166, refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitLdcInsn("..");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitJumpInsn(167, endIfListNull_);
         mw.visitLabel(refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "setPath", METHOD_DESC_SET_PATH2, false);
         mw.visitInsn(89);
         mw.visitVarInsn(58, REF_PATH);
         mw.visitJumpInsn(198, endDetect_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, REF_PATH);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         mw.visitJumpInsn(167, endIfListNull_);
         mw.visitLabel(endDetect_);
      }

      if (listSimple) {
         gwListSimpleType(mwc, i, mw, fieldClass, itemClass, LIST);
      } else {
         int PREVIOUS_CLASS = mwc.var("ITEM_CLASS");
         int ITEM_OBJECT_WRITER = mwc.var("ITEM_OBJECT_WRITER");
         mw.visitInsn(1);
         mw.visitInsn(89);
         mw.visitVarInsn(58, PREVIOUS_CLASS);
         mw.visitVarInsn(58, ITEM_OBJECT_WRITER);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeListValueJSONB", METHOD_DESC_WRITE_LIST, false);
      }

      if (!disableReferenceDetect) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
      }

      mw.visitLabel(endIfListNull_);
   }

   private void gwDate(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      MethodWriter mw = mwc.mw;
      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitInsn(3);
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeDate", METHOD_DESC_WRITE_DATE_WITH_FIELD_NAME, false);
   }

   private void gwValue(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      MethodWriter mw = mwc.mw;
      Class fieldClass = fieldWriter.fieldClass;
      if (fieldClass == String.class) {
         this.genGetObject(mwc, fieldWriter, i, OBJECT);
         mw.visitTypeInsn(192, "java/lang/String");
         int FIELD_VALUE = mwc.var("FIELD_VALUE_" + fieldWriter.fieldClass.getName());
         mw.visitVarInsn(58, FIELD_VALUE);
         gwString(mwc, false, true, FIELD_VALUE);
      } else {
         mw.visitVarInsn(25, 1);
         this.genGetObject(mwc, fieldWriter, i, OBJECT);
         if (fieldWriter.decimalFormat != null) {
            if (fieldClass == double.class) {
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDouble", "(DLjava/text/DecimalFormat;)V", false);
            } else if (fieldClass == float.class) {
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeFloat", "(FLjava/text/DecimalFormat;)V", false);
            } else {
               if (fieldClass != BigDecimal.class) {
                  throw new UnsupportedOperationException();
               }

               mw.visitLdcInsn(fieldWriter.features);
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDecimal", "(Ljava/math/BigDecimal;JLjava/text/DecimalFormat;)V", false);
            }
         } else {
            boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
            String methodName;
            String methodDesc;
            if (fieldClass == boolean.class) {
               methodName = "writeBool";
               methodDesc = "(Z)V";
            } else if (fieldClass == char.class) {
               methodName = "writeChar";
               methodDesc = "(C)V";
            } else if (fieldClass == byte.class) {
               methodName = writeAsString ? "writeString" : "writeInt8";
               methodDesc = "(B)V";
            } else if (fieldClass == short.class) {
               methodName = writeAsString ? "writeString" : "writeInt16";
               methodDesc = "(S)V";
            } else if (fieldClass == int.class) {
               methodName = writeAsString ? "writeString" : "writeInt32";
               methodDesc = "(I)V";
            } else if (fieldClass == Integer.class) {
               methodName = "writeInt32";
               methodDesc = "(Ljava/lang/Integer;)V";
            } else if (fieldClass == long.class) {
               methodName = writeAsString ? "writeString" : "writeInt64";
               methodDesc = "(J)V";
            } else if (fieldClass == Long.class) {
               methodName = "writeInt64";
               methodDesc = "(Ljava/lang/Long;)V";
            } else if (fieldClass == float.class) {
               methodName = writeAsString ? "writeString" : "writeFloat";
               methodDesc = "(F)V";
            } else if (fieldClass == double.class) {
               methodName = writeAsString ? "writeString" : "writeDouble";
               methodDesc = "(D)V";
            } else if (fieldClass == boolean[].class) {
               methodName = "writeBool";
               methodDesc = "([Z)V";
            } else if (fieldClass == char[].class) {
               methodName = "writeString";
               methodDesc = "([C)V";
            } else if (fieldClass == byte[].class) {
               methodName = "writeBinary";
               methodDesc = "([B)V";
            } else if (fieldClass == short[].class) {
               methodName = "writeInt16";
               methodDesc = "([S)V";
            } else if (fieldClass == int[].class) {
               methodName = "writeInt32";
               methodDesc = "([I)V";
            } else if (fieldClass == long[].class && mwc.provider.getObjectWriter(Long.class) == ObjectWriterImplInt64.INSTANCE) {
               methodName = "writeInt64";
               methodDesc = "([J)V";
            } else if (fieldClass == float[].class) {
               methodName = "writeFloat";
               methodDesc = "([F)V";
            } else if (fieldClass == double[].class) {
               methodName = "writeDouble";
               methodDesc = "([D)V";
            } else if (fieldClass == BigDecimal.class) {
               methodName = "writeDecimal";
               methodDesc = "(Ljava/math/BigDecimal;JLjava/text/DecimalFormat;)V";
               mw.visitLdcInsn(fieldWriter.features);
               mw.visitInsn(1);
            } else {
               if (!Enum.class.isAssignableFrom(fieldClass)) {
                  throw new UnsupportedOperationException();
               }

               methodName = "writeEnum";
               methodDesc = "(Ljava/lang/Enum;)V";
            }

            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, methodName, methodDesc, false);
         }
      }
   }

   private void gwObjectA(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      MethodWriter mw = mwc.mw;
      if (fieldWriter.fieldClass == String[].class) {
         mw.visitVarInsn(25, 1);
         this.genGetObject(mwc, fieldWriter, i, OBJECT);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "([Ljava/lang/String;)V", false);
      } else {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeValue", METHOD_DESC_WRITE_VALUE, false);
      }
   }

   private void genMethodWriteArrayMapping(
      ObjectWriterProvider provider,
      String methodName,
      Class objectType,
      long objectFeatures,
      List<FieldWriter> fieldWriters,
      ClassWriter cw,
      String classNameType
   ) {
      MethodWriter mw = cw.visitMethod(1, methodName, METHOD_DESC_WRITE, 512);
      int OBJECT = 2;
      int FIELD_NAME = 3;
      int FIELD_TYPE = 4;
      int FIELD_FEATURES = 5;
      Label jsonb_ = new Label();
      mw.visitVarInsn(25, 1);
      mw.visitFieldInsn(180, ASMUtils.TYPE_JSON_WRITER, "jsonb", "Z");
      mw.visitJumpInsn(153, jsonb_);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 2);
      mw.visitVarInsn(25, 3);
      mw.visitVarInsn(25, 4);
      mw.visitVarInsn(22, 5);
      mw.visitMethodInsn(182, classNameType, "writeArrayMappingJSONB", METHOD_DESC_WRITE_OBJECT, false);
      mw.visitInsn(177);
      mw.visitLabel(jsonb_);
      Label object_ = new Label();
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, "hasFilter", METHOD_DESC_HAS_FILTER, true);
      mw.visitJumpInsn(153, object_);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 2);
      mw.visitVarInsn(25, 3);
      mw.visitVarInsn(25, 4);
      mw.visitVarInsn(22, 5);
      mw.visitMethodInsn(183, ASMUtils.TYPE_OBJECT_WRITER_ADAPTER, methodName, METHOD_DESC_WRITE, false);
      mw.visitInsn(177);
      mw.visitLabel(object_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startArray", "()V", false);
      ObjectWriterCreatorASM.MethodWriterContext mwc = new ObjectWriterCreatorASM.MethodWriterContext(
         provider, objectType, objectFeatures, classNameType, mw, 7, false
      );

      for (int i = 0; i < fieldWriters.size(); i++) {
         if (i != 0) {
            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeComma", "()V", false);
         }

         this.gwFieldValueArrayMapping(fieldWriters.get(i), mwc, 2, i);
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "endArray", "()V", false);
      mw.visitInsn(177);
      mw.visitMaxs(mwc.maxVariant + 1, mwc.maxVariant + 1);
   }

   private void gwFieldValueArrayMapping(FieldWriter fieldWriter, ObjectWriterCreatorASM.MethodWriterContext mwc, int OBJECT, int i) {
      Class objectType = mwc.objectClass;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String TYPE_OBJECT = objectType == null ? "java/lang/Object" : ASMUtils.type(objectType);
      boolean userDefineWriter = false;
      if ((fieldClass == long.class || fieldClass == Long.class || fieldClass == long[].class) && (mwc.provider.userDefineMask & 4L) != 0L) {
         userDefineWriter = mwc.provider.getObjectWriter(Long.class) != ObjectWriterImplInt64.INSTANCE;
      }

      if (fieldClass == boolean.class
         || fieldClass == boolean[].class
         || fieldClass == char.class
         || fieldClass == char[].class
         || fieldClass == byte.class
         || fieldClass == byte[].class
         || fieldClass == short.class
         || fieldClass == short[].class
         || fieldClass == int.class
         || fieldClass == int[].class
         || fieldClass == long.class
         || fieldClass == long[].class && !userDefineWriter
         || fieldClass == float.class
         || fieldClass == float[].class
         || fieldClass == double.class
         || fieldClass == double[].class
         || fieldClass == String.class
         || fieldClass == Integer.class
         || fieldClass == Long.class
         || fieldClass == BigDecimal.class
         || fieldClass.isEnum()) {
         this.gwValue(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Date.class) {
         this.gwDate(mwc, fieldWriter, OBJECT, i);
      } else if (fieldWriter instanceof FieldWriterList) {
         this.gwList(mwc, OBJECT, i, fieldWriter);
      } else {
         this.gwObject(mwc, OBJECT, i, fieldWriter, TYPE_OBJECT);
      }
   }

   private void gwObject(ObjectWriterCreatorASM.MethodWriterContext mwc, int OBJECT, int i, FieldWriter fieldWriter, String TYPE_OBJECT) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      String fieldName = fieldWriter.fieldName;
      MethodWriter mw = mwc.mw;
      int FIELD_VALUE = mwc.var(fieldClass);
      int REF_PATH = mwc.var("REF_PATH");
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      if (fieldClass != Double.class && fieldClass != Float.class && fieldClass != BigDecimal.class) {
         boolean refDetection = !ObjectWriterProvider.isNotReferenceDetect(fieldClass);
         if (refDetection) {
            Label endDetect_ = new Label();
            Label refSetPath_ = new Label();
            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isRefDetect", "()Z", false);
            mw.visitJumpInsn(153, endDetect_);
            mw.visitVarInsn(25, OBJECT);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitJumpInsn(166, refSetPath_);
            mw.visitVarInsn(25, 1);
            mw.visitLdcInsn("..");
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
            mw.visitJumpInsn(167, endIfNull_);
            mw.visitLabel(refSetPath_);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "setPath", METHOD_DESC_SET_PATH2, false);
            mw.visitInsn(89);
            mw.visitVarInsn(58, REF_PATH);
            mw.visitJumpInsn(198, endDetect_);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, REF_PATH);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
            mw.visitJumpInsn(167, endIfNull_);
            mw.visitLabel(endDetect_);
         }

         if (fieldClass == String[].class) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "([Ljava/lang/String;)V", false);
         } else {
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "getObjectWriter", METHOD_DESC_GET_OBJECT_WRITER, false);
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitLdcInsn(fieldWriter.fieldName);
            mwc.loadFieldType(i, fieldWriter.fieldType);
            mw.visitLdcInsn(fieldWriter.features);
            mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, "write", METHOD_DESC_WRITE_OBJECT, true);
         }

         if (refDetection) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         }
      } else {
         mw.visitVarInsn(25, 1);
         if (fieldWriter.decimalFormat != null) {
            mw.visitVarInsn(25, FIELD_VALUE);
            if (fieldClass == Double.class) {
               mw.visitMethodInsn(182, "java/lang/Double", "doubleValue", "()D", false);
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDouble", "(DLjava/text/DecimalFormat;)V", false);
            } else if (fieldClass == Float.class) {
               mw.visitMethodInsn(182, "java/lang/Float", "floatValue", "()F", false);
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeFloat", "(FLjava/text/DecimalFormat;)V", false);
            } else {
               long features = fieldWriter.features;
               mw.visitLdcInsn(features);
               mw.visitVarInsn(25, 0);
               mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
               mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDecimal", "(Ljava/math/BigDecimal;JLjava/text/DecimalFormat;)V", false);
            }
         } else {
            mw.visitVarInsn(25, FIELD_VALUE);
            if (fieldClass == Double.class) {
               mw.visitMethodInsn(182, "java/lang/Double", "doubleValue", "()D", false);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDouble", "(D)V", false);
            } else if (fieldClass == Float.class) {
               mw.visitMethodInsn(182, "java/lang/Float", "floatValue", "()F", false);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeFloat", "(F)V", false);
            } else {
               long features = fieldWriter.features;
               mw.visitLdcInsn(features);
               mw.visitInsn(1);
               mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDecimal", "(Ljava/math/BigDecimal;JLjava/text/DecimalFormat;)V", false);
            }
         }
      }

      mw.visitLabel(endIfNull_);
   }

   private void gwList(ObjectWriterCreatorASM.MethodWriterContext mwc, int OBJECT, int i, FieldWriter fieldWriter) {
      Type fieldType = fieldWriter.fieldType;
      Class<?> fieldClass = fieldWriter.fieldClass;
      int LIST = mwc.var(fieldClass);
      MethodWriter mw = mwc.mw;
      boolean listSimple = false;
      Class itemClass = null;
      if (fieldType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)fieldType;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            Type itemType = actualTypeArguments[0];
            itemClass = TypeUtils.getMapping(itemType);
            listSimple = itemType == String.class || itemType == Integer.class || itemType == Long.class;
         }
      }

      Label endIfListNull_ = new Label();
      Label listNotNull_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, LIST);
      mw.visitJumpInsn(199, listNotNull_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitJumpInsn(167, endIfListNull_);
      mw.visitLabel(listNotNull_);
      if (listSimple) {
         this.genGetObject(mwc, fieldWriter, i, OBJECT);
         mw.visitVarInsn(58, LIST);
         gwListSimpleType(mwc, i, mw, fieldClass, itemClass, LIST);
      } else {
         int LIST_SIZE = mwc.var("LIST_SIZE");
         int J = mwc.var("J");
         int ITEM_CLASS = mwc.var(Class.class);
         int PREVIOUS_CLASS = mwc.var("PREVIOUS_CLASS");
         int ITEM_OBJECT_WRITER = mwc.var("ITEM_OBJECT_WRITER");
         mw.visitInsn(1);
         mw.visitInsn(89);
         mw.visitVarInsn(58, PREVIOUS_CLASS);
         mw.visitVarInsn(58, ITEM_OBJECT_WRITER);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(185, "java/util/List", "size", "()I", true);
         mw.visitVarInsn(54, LIST_SIZE);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "startArray", "()V", false);
         Label for_start_j_ = new Label();
         Label for_end_j_ = new Label();
         Label for_inc_j_ = new Label();
         Label notFirst_ = new Label();
         mw.visitInsn(3);
         mw.visitVarInsn(54, J);
         mw.visitLabel(for_start_j_);
         mw.visitVarInsn(21, J);
         mw.visitVarInsn(21, LIST_SIZE);
         mw.visitJumpInsn(162, for_end_j_);
         mw.visitVarInsn(21, J);
         mw.visitJumpInsn(153, notFirst_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeComma", "()V", false);
         mw.visitLabel(notFirst_);
         int ITEM = mwc.var(itemClass);
         Label notNull_ = new Label();
         Label classEQ_ = new Label();
         mw.visitVarInsn(25, LIST);
         mw.visitVarInsn(21, J);
         mw.visitMethodInsn(185, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
         mw.visitInsn(89);
         mw.visitVarInsn(58, ITEM);
         mw.visitJumpInsn(199, notNull_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
         mw.visitJumpInsn(167, for_inc_j_);
         mw.visitLabel(notNull_);
         mw.visitVarInsn(25, ITEM);
         mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
         mw.visitInsn(89);
         mw.visitVarInsn(58, ITEM_CLASS);
         mw.visitVarInsn(25, PREVIOUS_CLASS);
         mw.visitJumpInsn(165, classEQ_);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, ITEM_CLASS);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "getItemWriter", METHOD_DESC_GET_ITEM_WRITER, false);
         mw.visitVarInsn(58, ITEM_OBJECT_WRITER);
         mw.visitVarInsn(25, ITEM_CLASS);
         mw.visitVarInsn(58, PREVIOUS_CLASS);
         mw.visitLabel(classEQ_);
         mw.visitVarInsn(25, ITEM_OBJECT_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, ITEM);
         mw.visitVarInsn(21, J);
         mw.visitMethodInsn(184, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
         mwc.loadFieldType(i, fieldType);
         mw.visitLdcInsn(fieldWriter.features);
         mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, "write", METHOD_DESC_WRITE_OBJECT, true);
         mw.visitLabel(for_inc_j_);
         mw.visitIincInsn(J, 1);
         mw.visitJumpInsn(167, for_start_j_);
         mw.visitLabel(for_end_j_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "endArray", "()V", false);
      }

      mw.visitLabel(endIfListNull_);
   }

   private void gwFieldValue(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if (fieldClass == boolean.class) {
         this.gwFieldValueBooleanV(mwc, fieldWriter, OBJECT, i, false);
      } else if (fieldClass == boolean[].class
         || fieldClass == byte[].class
         || fieldClass == char[].class
         || fieldClass == short[].class
         || fieldClass == float[].class
         || fieldClass == double[].class) {
         this.gwFieldValueArray(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == int.class && !writeAsString) {
         this.gwFieldValueInt32V(mwc, fieldWriter, OBJECT, i, false);
      } else if (fieldClass == char.class
         || fieldClass == byte.class
         || fieldClass == int.class
         || fieldClass == short.class
         || fieldClass == float.class
         || fieldClass == double.class) {
         this.gwFieldName(mwc, fieldWriter, i);
         this.gwValue(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == int[].class) {
         this.gwFieldValueIntVA(mwc, fieldWriter, OBJECT, i, false);
      } else if (fieldClass == long.class) {
         this.gwFieldValueInt64V(mwc, fieldWriter, OBJECT, i, false);
      } else if (fieldClass == long[].class && mwc.provider.getObjectWriter(Long.class) == ObjectWriterImplInt64.INSTANCE) {
         this.gwFieldValueInt64VA(mwc, fieldWriter, OBJECT, i, false);
      } else if (fieldClass == Integer.class) {
         this.gwInt32(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Long.class) {
         this.gwInt64(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Float.class) {
         this.gwFloat(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Double.class) {
         this.gwDouble(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == String.class) {
         this.gwFieldValueString(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass.isEnum() && BeanUtils.getEnumValueField(fieldClass, mwc.provider) == null && !(fieldWriter instanceof FieldWriterObject)) {
         this.gwFieldValueEnum(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Date.class) {
         this.gwFieldValueDate(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == List.class) {
         this.gwFieldValueList(mwc, fieldWriter, OBJECT, i);
      } else {
         this.gwFieldValueObject(mwc, fieldWriter, OBJECT, i, false);
      }
   }

   private void gwFieldValueEnum(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      MethodWriter mw = mwc.mw;
      int FIELD_VALUE = mwc.var(fieldClass);
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      Label null_ = new Label();
      Label notNull_ = new Label();
      mw.visitJumpInsn(198, null_);
      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeEnum", METHOD_DESC_WRITE_ENUM, false);
      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(null_);
      mw.visitVarInsn(21, mwc.var("WRITE_NULLS"));
      mw.visitJumpInsn(153, notNull_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitLabel(notNull_);
   }

   private void gwFieldValueObject(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      Type fieldType = fieldWriter.fieldType;
      String fieldName = fieldWriter.fieldName;
      boolean disableReferenceDetect = mwc.disableReferenceDetect();
      boolean refDetection = !disableReferenceDetect && !ObjectWriterProvider.isNotReferenceDetect(fieldClass);
      int FIELD_VALUE = mwc.var(fieldClass);
      Integer REF_PATH = null;
      if (refDetection) {
         REF_PATH = mwc.var("REF_PATH");
      }

      long features = fieldWriter.features | mwc.objectFeatures;
      MethodWriter mw = mwc.mw;
      Label null_ = new Label();
      Label notNull_ = new Label();
      if (fieldWriter.unwrapped() || (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "write", METHOD_DESC_FIELD_WRITE_OBJECT, false);
         mw.visitInsn(87);
         mw.visitJumpInsn(167, notNull_);
      }

      if (fieldWriter.backReference) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "containsReference", "(Ljava/lang/Object;)Z", false);
         mw.visitJumpInsn(154, notNull_);
      }

      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(198, null_);
      if (Map.class.isAssignableFrom(fieldClass)) {
         Label ignoreEmptyEnd_ = null;
         if ((fieldWriter.features & JSONWriter.Feature.IgnoreEmpty.mask) == 0L) {
            ignoreEmptyEnd_ = new Label();
            mwc.genIsEnabled(JSONWriter.Feature.IgnoreEmpty.mask, ignoreEmptyEnd_);
         }

         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(185, "java/util/Map", "isEmpty", "()Z", true);
         mw.visitJumpInsn(154, notNull_);
         if (ignoreEmptyEnd_ != null) {
            mw.visitLabel(ignoreEmptyEnd_);
         }
      }

      if (!Serializable.class.isAssignableFrom(fieldClass) && fieldClass != List.class) {
         mw.visitVarInsn(25, 1);
         if (!fieldWriter.isFieldClassSerializable()) {
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isIgnoreNoneSerializable", "()Z", false);
         } else {
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isIgnoreNoneSerializable", "(Ljava/lang/Object;)Z", false);
         }

         mw.visitJumpInsn(154, notNull_);
      }

      if (refDetection) {
         Label endDetect_ = new Label();
         Label refSetPath_ = new Label();
         int REF_DETECT = mwc.var("REF_DETECT");
         if (fieldClass == Object.class) {
            mw.visitVarInsn(25, 1);
            mw.visitVarInsn(25, FIELD_VALUE);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isRefDetect", "(Ljava/lang/Object;)Z", false);
         } else {
            mwc.genIsEnabled(JSONWriter.Feature.ReferenceDetection.mask, null);
         }

         mw.visitInsn(89);
         mw.visitVarInsn(54, REF_DETECT);
         mw.visitJumpInsn(153, endDetect_);
         mw.visitVarInsn(25, OBJECT);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitJumpInsn(166, refSetPath_);
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitLdcInsn("..");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitJumpInsn(167, notNull_);
         mw.visitLabel(refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "setPath", METHOD_DESC_SET_PATH2, false);
         mw.visitInsn(89);
         mw.visitVarInsn(58, REF_PATH);
         mw.visitJumpInsn(198, endDetect_);
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, REF_PATH);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         mw.visitJumpInsn(167, null_);
         mw.visitLabel(endDetect_);
         if ("this$0".equals(fieldName) || "this$1".equals(fieldName) || "this$2".equals(fieldName)) {
            mw.visitVarInsn(21, REF_DETECT);
            mw.visitJumpInsn(153, null_);
         }
      }

      if (Object[].class.isAssignableFrom(fieldClass)) {
         Label notWriteEmptyArrayEnd_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.NotWriteEmptyArray.mask, notWriteEmptyArrayEnd_);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitTypeInsn(192, "[Ljava/lang/Object;");
         mw.visitInsn(190);
         mw.visitJumpInsn(154, notWriteEmptyArrayEnd_);
         mw.visitJumpInsn(167, notNull_);
         mw.visitLabel(notWriteEmptyArrayEnd_);
      } else if (Collection.class.isAssignableFrom(fieldClass)) {
         Label notWriteEmptyArrayEnd_ = new Label();
         if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) == 0L) {
            mwc.genIsEnabled(JSONWriter.Feature.NotWriteEmptyArray.mask, notWriteEmptyArrayEnd_);
         }

         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitTypeInsn(192, "java/util/Collection");
         mw.visitMethodInsn(185, "java/util/Collection", "isEmpty", "()Z", true);
         mw.visitJumpInsn(153, notWriteEmptyArrayEnd_);
         mw.visitJumpInsn(167, notNull_);
         mw.visitLabel(notWriteEmptyArrayEnd_);
      }

      this.gwFieldName(mwc, fieldWriter, i);
      Class itemClass = fieldWriter.getItemClass();
      if (fieldClass == BigDecimal.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitLdcInsn(features);
         if (fieldWriter.decimalFormat != null) {
            mw.visitVarInsn(25, 0);
            mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
            mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "decimalFormat", "Ljava/text/DecimalFormat;");
         } else {
            mw.visitInsn(1);
         }

         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDecimal", "(Ljava/math/BigDecimal;JLjava/text/DecimalFormat;)V", false);
      } else if (fieldClass == BigInteger.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         if (features == 0L) {
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeBigInt", "(Ljava/math/BigInteger;)V", false);
         } else {
            mw.visitLdcInsn(features);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeBigInt", "(Ljava/math/BigInteger;J)V", false);
         }
      } else if (fieldClass == UUID.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeUUID", "(Ljava/util/UUID;)V", false);
      } else if (fieldClass == LocalDate.class
         && fieldWriter.format == null
         && mwc.provider.getObjectWriter(LocalDate.class) == ObjectWriterImplLocalDate.INSTANCE) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeLocalDate", "(Ljava/time/LocalDate;)V", false);
      } else if (fieldClass == OffsetDateTime.class
         && fieldWriter.format == null
         && mwc.provider.getObjectWriter(OffsetDateTime.class) == ObjectWriterImplOffsetDateTime.INSTANCE) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeOffsetDateTime", "(Ljava/time/OffsetDateTime;)V", false);
      } else if (fieldClass == String[].class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "([Ljava/lang/String;)V", false);
      } else if (fieldClass != List.class || itemClass != String.class && itemClass != Integer.class && itemClass != Long.class) {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "getObjectWriter", METHOD_DESC_GET_OBJECT_WRITER, false);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitLdcInsn(fieldName);
         mwc.loadFieldType(i, fieldType);
         mw.visitLdcInsn(features);
         String writeMethod;
         if (jsonb) {
            writeMethod = (features & JSONWriter.Feature.BeanToArray.mask) != 0L ? "writeArrayMappingJSONB" : "writeJSONB";
         } else {
            writeMethod = (features & JSONWriter.Feature.BeanToArray.mask) != 0L ? "writeArrayMapping" : "write";
         }

         mw.visitMethodInsn(185, ASMUtils.TYPE_OBJECT_WRITER, writeMethod, METHOD_DESC_WRITE_OBJECT, true);
      } else {
         gwListSimpleType(mwc, i, mw, fieldClass, itemClass, FIELD_VALUE);
      }

      if (refDetection) {
         int REF_DETECTx = mwc.var("REF_DETECT");
         Label endDetect_x = new Label();
         mw.visitVarInsn(21, REF_DETECTx);
         mw.visitJumpInsn(153, endDetect_x);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         mw.visitLabel(endDetect_x);
      }

      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(null_);
      if ((features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
         long nullFeatures = JSONWriter.Feature.WriteNulls.mask;
         if (fieldClass == AtomicLongArray.class
            || fieldClass == AtomicIntegerArray.class
            || Collection.class.isAssignableFrom(fieldClass)
            || fieldClass.isArray()) {
            nullFeatures |= JSONWriter.Feature.WriteNullListAsEmpty.mask;
            nullFeatures |= JSONWriter.Feature.NullAsDefaultValue.mask;
         } else if (Number.class.isAssignableFrom(fieldClass)) {
            nullFeatures |= JSONWriter.Feature.WriteNullNumberAsZero.mask;
            nullFeatures |= JSONWriter.Feature.NullAsDefaultValue.mask;
         } else if (fieldClass == Boolean.class) {
            nullFeatures |= JSONWriter.Feature.WriteNullBooleanAsFalse.mask;
            nullFeatures |= JSONWriter.Feature.NullAsDefaultValue.mask;
         } else if (fieldClass == String.class) {
            nullFeatures |= JSONWriter.Feature.WriteNullStringAsEmpty.mask;
            nullFeatures |= JSONWriter.Feature.NullAsDefaultValue.mask;
         }

         mwc.genIsEnabled(nullFeatures, notNull_);
      }

      this.gwFieldName(mwc, fieldWriter, i);
      String WRITE_NULL_METHOD;
      if (fieldClass == AtomicLongArray.class
         || fieldClass == AtomicIntegerArray.class
         || Collection.class.isAssignableFrom(fieldClass)
         || fieldClass.isArray()) {
         WRITE_NULL_METHOD = "writeArrayNull";
      } else if (Number.class.isAssignableFrom(fieldClass)) {
         WRITE_NULL_METHOD = "writeNumberNull";
      } else if (fieldClass == Boolean.class) {
         WRITE_NULL_METHOD = "writeBooleanNull";
      } else if (fieldClass != String.class && fieldClass != Appendable.class && fieldClass != StringBuffer.class && fieldClass != StringBuilder.class) {
         WRITE_NULL_METHOD = "writeNull";
      } else {
         WRITE_NULL_METHOD = "writeStringNull";
      }

      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, WRITE_NULL_METHOD, "()V", false);
      mw.visitLabel(notNull_);
   }

   private void gwFieldValueList(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean disableReferenceDetect = mwc.disableReferenceDetect();
      Type fieldType = fieldWriter.fieldType;
      Class<?> fieldClass = fieldWriter.fieldClass;
      MethodWriter mw = mwc.mw;
      int LIST = mwc.var(fieldClass);
      int REF_PATH = -1;
      if (!disableReferenceDetect) {
         REF_PATH = mwc.var("REF_PATH");
      }

      Class itemClass = null;
      boolean listSimple = false;
      if ((fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) == 0L && fieldType instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)fieldType;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            Type arg0 = actualTypeArguments[0];
            itemClass = TypeUtils.getClass(arg0);
            listSimple = arg0 == String.class || arg0 == Integer.class || arg0 == Long.class;
         }
      }

      int FIELD_VALUE = mwc.var(fieldClass);
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      Label null_ = new Label();
      Label notNull_ = new Label();
      mw.visitJumpInsn(198, null_);
      Label ignoreEmptyEnd_ = null;
      if ((fieldWriter.features & JSONWriter.Feature.IgnoreEmpty.mask) == 0L) {
         ignoreEmptyEnd_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.IgnoreEmpty.mask, ignoreEmptyEnd_);
      }

      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(185, "java/util/Collection", "isEmpty", "()Z", true);
      mw.visitJumpInsn(154, notNull_);
      if (ignoreEmptyEnd_ != null) {
         mw.visitLabel(ignoreEmptyEnd_);
      }

      if (!disableReferenceDetect) {
         Label endDetect_ = new Label();
         Label refSetPath_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.ReferenceDetection.mask, endDetect_);
         mw.visitVarInsn(25, OBJECT);
         mw.visitVarInsn(25, LIST);
         mw.visitJumpInsn(166, refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitLdcInsn("..");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitJumpInsn(167, notNull_);
         mw.visitLabel(refSetPath_);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "setPath", METHOD_DESC_SET_PATH2, false);
         mw.visitInsn(89);
         mw.visitVarInsn(58, REF_PATH);
         mw.visitJumpInsn(198, endDetect_);
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, REF_PATH);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeReference", "(Ljava/lang/String;)V", false);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
         mw.visitJumpInsn(167, notNull_);
         mw.visitLabel(endDetect_);
      }

      Label notWriteEmptyArrayEnd_ = new Label();
      mwc.genIsEnabled(JSONWriter.Feature.NotWriteEmptyArray.mask, notWriteEmptyArrayEnd_);
      mw.visitVarInsn(25, LIST);
      mw.visitMethodInsn(185, "java/util/Collection", "isEmpty", "()Z", true);
      mw.visitJumpInsn(153, notWriteEmptyArrayEnd_);
      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(notWriteEmptyArrayEnd_);
      if (listSimple) {
         this.gwFieldName(mwc, fieldWriter, i);
         gwListSimpleType(mwc, i, mw, fieldClass, itemClass, FIELD_VALUE);
      } else {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, mwc.jsonb ? "writeListValueJSONB" : "writeListValue", METHOD_DESC_WRITE_LIST, false);
      }

      if (!disableReferenceDetect) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, LIST);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "popPath", "(Ljava/lang/Object;)V", false);
      }

      mw.visitJumpInsn(167, notNull_);
      mw.visitLabel(null_);
      mwc.genIsEnabled(JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask, notNull_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeArrayNull", "()V", false);
      mw.visitLabel(notNull_);
   }

   private void gwFieldValueJSONB(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      Class<?> fieldClass = fieldWriter.fieldClass;
      boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if (fieldClass == boolean.class) {
         this.gwFieldValueBooleanV(mwc, fieldWriter, OBJECT, i, true);
      } else if (fieldClass == boolean[].class
         || fieldClass == byte[].class
         || fieldClass == char[].class
         || fieldClass == short[].class
         || fieldClass == float[].class
         || fieldClass == double[].class) {
         this.gwFieldValueArray(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == int.class && !writeAsString) {
         this.gwFieldValueInt32V(mwc, fieldWriter, OBJECT, i, true);
      } else if (fieldClass == char.class
         || fieldClass == byte.class
         || fieldClass == short.class
         || fieldClass == int.class
         || fieldClass == float.class
         || fieldClass == double.class) {
         this.gwFieldName(mwc, fieldWriter, i);
         this.gwValue(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == int[].class) {
         this.gwFieldValueIntVA(mwc, fieldWriter, OBJECT, i, true);
      } else if (fieldClass == long.class) {
         this.gwFieldValueInt64V(mwc, fieldWriter, OBJECT, i, true);
      } else if (fieldClass == long[].class && mwc.provider.getObjectWriter(Long.class) == ObjectWriterImplInt64.INSTANCE) {
         this.gwFieldValueInt64VA(mwc, fieldWriter, OBJECT, i, true);
      } else if (fieldClass == Integer.class) {
         this.gwInt32(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Long.class) {
         this.gwInt64(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == String.class) {
         this.gwFieldValueString(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass.isEnum()) {
         this.gwFieldValueArray(mwc, fieldWriter, OBJECT, i);
      } else if (fieldClass == Date.class) {
         this.gwFieldValueDate(mwc, fieldWriter, OBJECT, i);
      } else {
         this.gwFieldValueObject(mwc, fieldWriter, OBJECT, i, true);
      }
   }

   private void gwInt32(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean jsonb = mwc.jsonb;
      String classNameType = mwc.classNameType;
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      if ((
            fieldWriter.features
               & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask)
         )
         == 0L) {
         mwc.genIsEnabled(
            JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask,
            writeNullValue_,
            endIfNull_
         );
         mw.visitLabel(writeNullValue_);
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNumberNull", "()V", false);
      } else {
         long features = fieldWriter.features;
         if ((features & (JSONWriter.Feature.WriteNullNumberAsZero.mask | JSONWriter.Feature.NullAsDefaultValue.mask)) != 0L) {
            this.gwFieldName(mwc, fieldWriter, i);
            mw.visitVarInsn(25, 1);
            mw.visitLdcInsn(0);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt32", "(I)V", false);
         } else {
            this.gwFieldName(mwc, fieldWriter, i);
            mw.visitVarInsn(25, 1);
            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
         }
      }

      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      if (writeAsString) {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Integer", "intValue", "()I", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeInt32", METHOD_DESC_WRITE_I, false);
      } else {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Integer", "intValue", "()I", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt32", "(I)V", false);
      }

      mw.visitLabel(endIfNull_);
   }

   private void gwInt64(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean jsonb = mwc.jsonb;
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      if ((fieldWriter.features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
         mw.visitInsn(89);
         mw.visitVarInsn(58, FIELD_VALUE);
         mw.visitJumpInsn(199, notNull_);
         mwc.genIsEnabled(
            JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask,
            writeNullValue_,
            endIfNull_
         );
      } else {
         mw.visitVarInsn(58, FIELD_VALUE);
      }

      mw.visitLabel(writeNullValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt64Null", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      if ((
            fieldWriter.features
               & (JSONWriter.Feature.WriteNonStringValueAsString.mask | JSONWriter.Feature.WriteLongAsString.mask | JSONWriter.Feature.BrowserCompatible.mask)
         )
         == 0L) {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Long", "longValue", "()J", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt64", "(J)V", false);
      } else {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Long", "longValue", "()J", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeInt64", METHOD_DESC_WRITE_J, false);
      }

      mw.visitLabel(endIfNull_);
   }

   private void gwDouble(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean jsonb = mwc.jsonb;
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      if ((fieldWriter.features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
         mw.visitInsn(89);
         mw.visitVarInsn(58, FIELD_VALUE);
         mw.visitJumpInsn(199, notNull_);
         mwc.genIsEnabled(
            JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask,
            writeNullValue_,
            endIfNull_
         );
      } else {
         mw.visitVarInsn(58, FIELD_VALUE);
      }

      mw.visitLabel(writeNullValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNumberNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      if (jsonb && (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) == 0L) {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Double", "doubleValue", "()D", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeDouble", "(D)V", false);
      } else {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Double", "doubleValue", "()D", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeDouble", METHOD_DESC_WRITE_D, false);
      }

      mw.visitLabel(endIfNull_);
   }

   private void gwFloat(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean jsonb = mwc.jsonb;
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      mwc.genIsEnabled(
         JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask,
         writeNullValue_,
         endIfNull_
      );
      mw.visitLabel(writeNullValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNumberNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      if (jsonb) {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Float", "floatValue", "()F", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeFloat", "(D)V", false);
      } else {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/Float", "floatValue", "()F", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeFloat", METHOD_DESC_WRITE_F, false);
      }

      mw.visitLabel(endIfNull_);
   }

   private static void gwListSimpleType(
      ObjectWriterCreatorASM.MethodWriterContext mwc, int i, MethodWriter mw, Class<?> fieldClass, Class itemClass, int FIELD_VALUE
   ) {
      if (mwc.jsonb) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mwc.loadFieldClass(i, fieldClass);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "checkAndWriteTypeName", "(Ljava/lang/Object;Ljava/lang/Class;)V", false);
      }

      if (itemClass == Integer.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeListInt32", "(Ljava/util/List;)V", false);
      } else if (itemClass == Long.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeListInt64", "(Ljava/util/List;)V", false);
      } else if (itemClass == String.class) {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "(Ljava/util/List;)V", false);
      } else {
         throw new JSONException("TOOD " + itemClass.getName());
      }
   }

   static void gwString(ObjectWriterCreatorASM.MethodWriterContext mwc, boolean symbol, boolean checkNull, int STR) {
      MethodWriter mw = mwc.mw;
      Label notNull_ = new Label();
      Label endNull_ = new Label();
      if (checkNull) {
         mw.visitVarInsn(25, STR);
         mw.visitJumpInsn(199, notNull_);
         mw.visitVarInsn(25, 1);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeStringNull", "()V", false);
         mw.visitJumpInsn(167, endNull_);
         mw.visitLabel(notNull_);
      }

      if (JDKUtils.JVM_VERSION == 8 && !JDKUtils.OPENJ9 && !JDKUtils.FIELD_STRING_VALUE_ERROR && !symbol) {
         mw.visitVarInsn(25, 1);
         mw.visitFieldInsn(178, ObjectWriterCreatorASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
         mw.visitVarInsn(25, STR);
         mw.visitLdcInsn(JDKUtils.FIELD_STRING_VALUE_OFFSET);
         mw.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false);
         mw.visitTypeInsn(192, "[C");
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "([C)V", false);
      } else if (JDKUtils.JVM_VERSION > 8
         && !JDKUtils.OPENJ9
         && JDKUtils.FIELD_STRING_CODER_OFFSET != -1L
         && JDKUtils.FIELD_STRING_VALUE_OFFSET != -1L
         && !symbol) {
         Label utf16_ = new Label();
         Label end_ = new Label();
         mw.visitVarInsn(25, 1);
         mw.visitFieldInsn(178, ObjectWriterCreatorASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
         mw.visitVarInsn(25, STR);
         mw.visitLdcInsn(JDKUtils.FIELD_STRING_VALUE_OFFSET);
         mw.visitMethodInsn(182, "sun/misc/Unsafe", "getObject", "(Ljava/lang/Object;J)Ljava/lang/Object;", false);
         mw.visitTypeInsn(192, "[B");
         mw.visitFieldInsn(178, ObjectWriterCreatorASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
         mw.visitVarInsn(25, STR);
         mw.visitLdcInsn(JDKUtils.FIELD_STRING_CODER_OFFSET);
         mw.visitMethodInsn(182, "sun/misc/Unsafe", "getByte", "(Ljava/lang/Object;J)B", false);
         mw.visitJumpInsn(154, utf16_);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeStringLatin1", "([B)V", false);
         mw.visitJumpInsn(167, end_);
         mw.visitLabel(utf16_);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeStringUTF16", "([B)V", false);
         mw.visitLabel(end_);
      } else {
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, STR);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, symbol ? "writeSymbol" : "writeString", "(Ljava/lang/String;)V", false);
      }

      if (checkNull) {
         mw.visitLabel(endNull_);
      }
   }

   private void gwFieldValueDate(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      Label null_ = new Label();
      Label writeNull_ = new Label();
      Label endIfNull_ = new Label();
      int FIELD_VALUE = mwc.var(fieldClass);
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(198, null_);
      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, "java/util/Date", "getTime", "()J", false);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeDate", METHOD_DESC_WRITE_J, false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(null_);
      if ((fieldWriter.features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
         mw.visitVarInsn(21, mwc.var("WRITE_NULLS"));
         mw.visitJumpInsn(154, writeNull_);
         mw.visitJumpInsn(167, endIfNull_);
      }

      mw.visitLabel(writeNull_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeNull", "()V", false);
      mw.visitLabel(endIfNull_);
   }

   private void gwFieldValueArray(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      MethodWriter mw = mwc.mw;
      Class fieldClass = fieldWriter.fieldClass;
      String methodName;
      String methodDesc;
      if (fieldClass == char[].class) {
         methodName = "writeString";
         methodDesc = METHOD_DESC_WRITE_CArray;
      } else if (fieldClass == boolean[].class) {
         methodName = "writeBool";
         methodDesc = METHOD_DESC_WRITE_ZARRAY;
      } else if (fieldClass == byte[].class) {
         methodName = "writeBinary";
         methodDesc = METHOD_DESC_WRITE_BArray;
      } else if (fieldClass == short[].class) {
         methodName = "writeInt16";
         methodDesc = METHOD_DESC_WRITE_SArray;
      } else if (fieldClass == float[].class) {
         methodName = "writeFloat";
         methodDesc = METHOD_DESC_WRITE_FARRAY;
      } else if (fieldClass == double[].class) {
         methodName = "writeDouble";
         methodDesc = METHOD_DESC_WRITE_DARRAY;
      } else {
         if (!fieldClass.isEnum()) {
            throw new UnsupportedOperationException();
         }

         methodName = "writeEnumJSONB";
         methodDesc = METHOD_DESC_WRITE_ENUM;
      }

      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, methodName, methodDesc, false);
   }

   private void gwFieldName(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int i) {
      MethodWriter mw = mwc.mw;
      String classNameType = mwc.classNameType;
      Label labelElse = new Label();
      Label labelEnd = new Label();
      boolean writeDirect = false;
      if (!mwc.jsonb) {
         byte[] fieldNameUTF8 = fieldWriter.fieldName.getBytes(StandardCharsets.UTF_8);
         boolean asciiName = true;

         for (int j = 0; j < fieldNameUTF8.length; j++) {
            if (fieldNameUTF8[j] < 0) {
               asciiName = false;
               break;
            }
         }

         int length = fieldNameUTF8.length;
         if (length >= 2 && length <= 16 && asciiName) {
            Number name1 = 0;
            Number name1SQ = 0;
            String methodDesc = "(J)V";
            byte[] bytes = new byte[8];
            String methodName;
            switch (length) {
               case 2:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 2);
                  bytes[3] = 34;
                  bytes[4] = 58;
                  methodName = "writeName2Raw";
                  break;
               case 3:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 3);
                  bytes[4] = 34;
                  bytes[5] = 58;
                  methodName = "writeName3Raw";
                  break;
               case 4:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 4);
                  bytes[5] = 34;
                  bytes[6] = 58;
                  methodName = "writeName4Raw";
                  break;
               case 5:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 5);
                  bytes[6] = 34;
                  bytes[7] = 58;
                  methodName = "writeName5Raw";
                  break;
               case 6:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 6);
                  bytes[7] = 34;
                  methodName = "writeName6Raw";
                  break;
               case 7:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodName = "writeName7Raw";
                  break;
               case 8:
                  bytes = fieldNameUTF8;
                  methodName = "writeName8Raw";
                  break;
               case 9: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JI)V";
                  byte[] name1Bytes = new byte[]{fieldNameUTF8[7], fieldNameUTF8[8], 34, 58};
                  name1 = JDKUtils.UNSAFE.getInt(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[2] = 39;
                  name1SQ = JDKUtils.UNSAFE.getInt(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName9Raw";
                  break;
               }
               case 10: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  byte[] name1Bytes = new byte[8];
                  name1Bytes[0] = fieldNameUTF8[7];
                  name1Bytes[1] = fieldNameUTF8[8];
                  name1Bytes[2] = fieldNameUTF8[9];
                  name1Bytes[3] = 34;
                  name1Bytes[4] = 58;
                  name1 = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[3] = 39;
                  name1SQ = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName10Raw";
                  break;
               }
               case 11: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  byte[] name1Bytes = new byte[]{fieldNameUTF8[7], fieldNameUTF8[8], fieldNameUTF8[9], fieldNameUTF8[10], 34, 58, 0, 0};
                  name1 = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[4] = 39;
                  name1SQ = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName11Raw";
                  break;
               }
               case 12: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  byte[] name1Bytes = new byte[]{fieldNameUTF8[7], fieldNameUTF8[8], fieldNameUTF8[9], fieldNameUTF8[10], fieldNameUTF8[11], 34, 58, 0};
                  name1 = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[5] = 39;
                  name1SQ = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName12Raw";
                  break;
               }
               case 13: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  byte[] name1Bytes = new byte[]{
                     fieldNameUTF8[7], fieldNameUTF8[8], fieldNameUTF8[9], fieldNameUTF8[10], fieldNameUTF8[11], fieldNameUTF8[12], 34, 58
                  };
                  name1 = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[6] = 39;
                  name1SQ = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName13Raw";
                  break;
               }
               case 14: {
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  byte[] name1Bytes = new byte[]{
                     fieldNameUTF8[7], fieldNameUTF8[8], fieldNameUTF8[9], fieldNameUTF8[10], fieldNameUTF8[11], fieldNameUTF8[12], fieldNameUTF8[13], 34
                  };
                  name1 = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  name1Bytes[7] = 39;
                  name1SQ = JDKUtils.UNSAFE.getLong(name1Bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
                  methodName = "writeName14Raw";
                  break;
               }
               case 15:
                  bytes[0] = 34;
                  System.arraycopy(fieldNameUTF8, 0, bytes, 1, 7);
                  methodDesc = "(JJ)V";
                  name1 = JDKUtils.UNSAFE.getLong(fieldNameUTF8, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 7L);
                  name1SQ = name1;
                  methodName = "writeName15Raw";
                  break;
               case 16:
                  System.arraycopy(fieldNameUTF8, 0, bytes, 0, 8);
                  methodDesc = "(JJ)V";
                  name1 = JDKUtils.UNSAFE.getLong(fieldNameUTF8, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L);
                  name1SQ = name1;
                  methodName = "writeName16Raw";
                  break;
               default:
                  throw new IllegalStateException("length : " + length);
            }

            long nameIn64 = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);

            for (int jx = 0; jx < bytes.length; jx++) {
               if (bytes[jx] == 34) {
                  bytes[jx] = 39;
               }
            }

            long nameIn64SQ = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
            mw.visitVarInsn(25, 1);
            mwc.ldcIFEQ("NAME_DIRECT", nameIn64, nameIn64SQ);
            if ("(JI)V".equals(methodDesc) || "(JJ)V".equals(methodDesc)) {
               mwc.ldcIFEQ("NAME_DIRECT", name1, name1SQ);
            }

            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, methodName, methodDesc, false);
            return;
         }
      } else {
         byte[] fieldNameUTF8 = JSONB.toBytes(fieldWriter.fieldName);
         int length = fieldNameUTF8.length;
         String methodName = null;
         String methodDesc = "(J)V";
         byte[] bytes = Arrays.copyOf(fieldNameUTF8, 16);
         switch (length) {
            case 2:
               methodName = "writeName2Raw";
               break;
            case 3:
               methodName = "writeName3Raw";
               break;
            case 4:
               methodName = "writeName4Raw";
               break;
            case 5:
               methodName = "writeName5Raw";
               break;
            case 6:
               methodName = "writeName6Raw";
               break;
            case 7:
               methodName = "writeName7Raw";
               break;
            case 8:
               methodName = "writeName8Raw";
               break;
            case 9:
               methodName = "writeName9Raw";
               methodDesc = "(JI)V";
               break;
            case 10:
               methodName = "writeName10Raw";
               methodDesc = "(JJ)V";
               break;
            case 11:
               methodName = "writeName11Raw";
               methodDesc = "(JJ)V";
               break;
            case 12:
               methodName = "writeName12Raw";
               methodDesc = "(JJ)V";
               break;
            case 13:
               methodName = "writeName13Raw";
               methodDesc = "(JJ)V";
               break;
            case 14:
               methodName = "writeName14Raw";
               methodDesc = "(JJ)V";
               break;
            case 15:
               methodName = "writeName15Raw";
               methodDesc = "(JJ)V";
               break;
            case 16:
               methodName = "writeName16Raw";
               methodDesc = "(JJ)V";
         }

         if (methodName != null) {
            mw.visitVarInsn(21, mwc.var("NAME_DIRECT"));
            mw.visitJumpInsn(153, labelElse);
            long nameIn64 = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET);
            mw.visitVarInsn(25, 1);
            mw.visitLdcInsn(nameIn64);
            if ("(JI)V".equals(methodDesc)) {
               int name1 = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L);
               mw.visitLdcInsn(name1);
            } else if ("(JJ)V".equals(methodDesc)) {
               long name1 = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + 8L);
               mw.visitLdcInsn(name1);
            }

            mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, methodName, methodDesc, false);
            mw.visitJumpInsn(167, labelEnd);
            writeDirect = true;
         }
      }

      if (writeDirect) {
         mw.visitLabel(labelElse);
      }

      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, mwc.jsonb ? "writeFieldNameJSONB" : "writeFieldName", METHOD_DESC_WRITE_FIELD_NAME, false);
      if (writeDirect) {
         mw.visitLabel(labelEnd);
      }
   }

   private void gwFieldValueInt64VA(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(21, mwc.var("WRITE_NULLS"));
      mw.visitJumpInsn(154, writeNullValue_);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(writeNullValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeArrayNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      this.gwFieldName(mwc, fieldWriter, i);
      boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, writeAsString ? "writeString" : "writeInt64", "([J)V", false);
      mw.visitLabel(endIfNull_);
   }

   private void gwFieldValueInt64V(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      MethodWriter mw = mwc.mw;
      String format = fieldWriter.format;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(long.class);
      int WRITE_DEFAULT_VALUE = mwc.var("WRITE_DEFAULT_VALUE");
      Label notDefaultValue_ = new Label();
      Label endWriteValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(92);
      mw.visitVarInsn(55, FIELD_VALUE);
      mw.visitInsn(9);
      mw.visitInsn(148);
      mw.visitJumpInsn(154, notDefaultValue_);
      if (fieldWriter.defaultValue == null) {
         mw.visitVarInsn(21, WRITE_DEFAULT_VALUE);
         mw.visitJumpInsn(153, notDefaultValue_);
         mw.visitJumpInsn(167, endWriteValue_);
      }

      mw.visitLabel(notDefaultValue_);
      boolean iso8601 = "iso8601".equals(format);
      if (!iso8601
         && (
               fieldWriter.features
                  & (
                     JSONWriter.Feature.WriteNonStringValueAsString.mask
                        | JSONWriter.Feature.WriteLongAsString.mask
                        | JSONWriter.Feature.BrowserCompatible.mask
                  )
            )
            == 0L) {
         this.gwFieldName(mwc, fieldWriter, i);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(22, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt64", "(J)V", false);
      } else {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(22, FIELD_VALUE);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, iso8601 ? "writeDate" : "writeInt64", METHOD_DESC_WRITE_J, false);
      }

      mw.visitLabel(endWriteValue_);
   }

   void gwFieldValueIntVA(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label endIfNull_ = new Label();
      Label notNull_ = new Label();
      Label writeNullValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(199, notNull_);
      mw.visitVarInsn(21, mwc.var("WRITE_NULLS"));
      mw.visitJumpInsn(154, writeNullValue_);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(writeNullValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeArrayNull", "()V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(notNull_);
      this.gwFieldName(mwc, fieldWriter, i);
      boolean writeAsString = (fieldWriter.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, writeAsString ? "writeString" : "writeInt32", "([I)V", false);
      mw.visitLabel(endIfNull_);
   }

   private void gwFieldValueInt32V(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      MethodWriter mw = mwc.mw;
      String format = fieldWriter.format;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(int.class);
      int WRITE_DEFAULT_VALUE = mwc.var("WRITE_DEFAULT_VALUE");
      Label notDefaultValue_ = new Label();
      Label endWriteValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(54, FIELD_VALUE);
      mw.visitJumpInsn(154, notDefaultValue_);
      if (fieldWriter.defaultValue == null) {
         mw.visitVarInsn(21, WRITE_DEFAULT_VALUE);
         mw.visitJumpInsn(153, notDefaultValue_);
         mw.visitJumpInsn(167, endWriteValue_);
      }

      mw.visitLabel(notDefaultValue_);
      this.gwFieldName(mwc, fieldWriter, i);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(21, FIELD_VALUE);
      if ("string".equals(format)) {
         mw.visitMethodInsn(184, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "(Ljava/lang/String;)V", false);
      } else if (format != null) {
         mw.visitLdcInsn(format);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt32", "(ILjava/lang/String;)V", false);
      } else {
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeInt32", "(I)V", false);
      }

      mw.visitLabel(endWriteValue_);
   }

   private void gwFieldValueBooleanV(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i, boolean jsonb) {
      MethodWriter mw = mwc.mw;
      String classNameType = mwc.classNameType;
      int FIELD_VALUE = mwc.var(boolean.class);
      int WRITE_DEFAULT_VALUE = mwc.var("WRITE_DEFAULT_VALUE");
      Label notDefaultValue_ = new Label();
      Label endWriteValue_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(54, FIELD_VALUE);
      mw.visitJumpInsn(154, notDefaultValue_);
      if (fieldWriter.defaultValue == null) {
         mw.visitVarInsn(21, WRITE_DEFAULT_VALUE);
         mw.visitJumpInsn(153, notDefaultValue_);
         mw.visitJumpInsn(167, endWriteValue_);
      }

      mw.visitLabel(notDefaultValue_);
      mw.visitVarInsn(25, 0);
      mw.visitFieldInsn(180, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(21, FIELD_VALUE);
      mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "writeBool", METHOD_DESC_WRITE_Z, false);
      mw.visitLabel(endWriteValue_);
   }

   private void gwFieldValueString(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int OBJECT, int i) {
      boolean jsonb = mwc.jsonb;
      long features = fieldWriter.features | mwc.objectFeatures;
      MethodWriter mw = mwc.mw;
      Class<?> fieldClass = fieldWriter.fieldClass;
      String format = fieldWriter.format;
      int FIELD_VALUE = mwc.var(fieldClass);
      Label null_ = new Label();
      Label endIfNull_ = new Label();
      this.genGetObject(mwc, fieldWriter, i, OBJECT);
      mw.visitInsn(89);
      mw.visitVarInsn(58, FIELD_VALUE);
      mw.visitJumpInsn(198, null_);
      if ("trim".equals(format)) {
         mw.visitVarInsn(25, FIELD_VALUE);
         mw.visitMethodInsn(182, "java/lang/String", "trim", "()Ljava/lang/String;", false);
         mw.visitVarInsn(58, FIELD_VALUE);
      }

      Label ignoreEmptyEnd_ = null;
      if ((features & JSONWriter.Feature.IgnoreEmpty.mask) == 0L) {
         ignoreEmptyEnd_ = new Label();
         mwc.genIsEnabled(JSONWriter.Feature.IgnoreEmpty.mask, ignoreEmptyEnd_);
      }

      mw.visitVarInsn(25, FIELD_VALUE);
      mw.visitMethodInsn(182, "java/lang/String", "isEmpty", "()Z", false);
      mw.visitJumpInsn(154, endIfNull_);
      if (ignoreEmptyEnd_ != null) {
         mw.visitLabel(ignoreEmptyEnd_);
      }

      this.gwFieldName(mwc, fieldWriter, i);
      boolean symbol = jsonb && "symbol".equals(format);
      gwString(mwc, symbol, false, FIELD_VALUE);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(null_);
      Label writeNullValue_ = new Label();
      Label writeNull_ = new Label();
      long defaultValueMask = JSONWriter.Feature.NullAsDefaultValue.mask
         | JSONWriter.Feature.WriteNullNumberAsZero.mask
         | JSONWriter.Feature.WriteNullBooleanAsFalse.mask
         | JSONWriter.Feature.WriteNullListAsEmpty.mask
         | JSONWriter.Feature.WriteNullStringAsEmpty.mask;
      if ((features & (JSONWriter.Feature.WriteNulls.mask | defaultValueMask)) == 0L) {
         mwc.genIsEnabled(
            JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask,
            writeNull_,
            endIfNull_
         );
      }

      mw.visitLabel(writeNull_);
      if (fieldWriter.defaultValue == null) {
         mwc.genIsDisabled(JSONWriter.Feature.NotWriteDefaultValue.mask, endIfNull_);
      }

      this.gwFieldName(mwc, fieldWriter, i);
      if ((features & defaultValueMask) == 0L) {
         long mask = JSONWriter.Feature.NullAsDefaultValue.mask;
         if (fieldClass == String.class) {
            mask |= JSONWriter.Feature.WriteNullStringAsEmpty.mask;
         } else if (fieldClass == Boolean.class) {
            mask |= JSONWriter.Feature.WriteNullBooleanAsFalse.mask;
         } else if (Number.class.isAssignableFrom(fieldClass)) {
            mask |= JSONWriter.Feature.WriteNullNumberAsZero.mask;
         } else if (Collection.class.isAssignableFrom(fieldClass)) {
            mask |= JSONWriter.Feature.WriteNullListAsEmpty.mask;
         }

         mw.visitVarInsn(25, 1);
         mw.visitLdcInsn(mask);
         mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "isEnabled", "(J)Z", false);
         mw.visitJumpInsn(153, writeNullValue_);
      }

      mw.visitVarInsn(25, 1);
      mw.visitLdcInsn("");
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeString", "(Ljava/lang/String;)V", false);
      mw.visitJumpInsn(167, endIfNull_);
      mw.visitLabel(writeNullValue_);
      mw.visitVarInsn(25, 1);
      mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "writeStringNull", "()V", false);
      mw.visitLabel(endIfNull_);
   }

   private void genMethodInit(List<FieldWriter> fieldWriters, ClassWriter cw, String classNameType, String objectWriterSupper) {
      MethodWriter mw = cw.visitMethod(1, "<init>", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/util/List;)V", 64);
      mw.visitVarInsn(25, 0);
      mw.visitVarInsn(25, 1);
      mw.visitVarInsn(25, 2);
      mw.visitVarInsn(25, 3);
      mw.visitVarInsn(22, 4);
      mw.visitVarInsn(25, 6);
      mw.visitMethodInsn(183, objectWriterSupper, "<init>", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/util/List;)V", false);
      if (objectWriterSupper == ASMUtils.TYPE_OBJECT_WRITER_ADAPTER) {
         for (int i = 0; i < fieldWriters.size(); i++) {
            mw.visitVarInsn(25, 0);
            mw.visitInsn(89);
            mw.visitFieldInsn(180, ASMUtils.TYPE_OBJECT_WRITER_ADAPTER, "fieldWriterArray", ASMUtils.DESC_FIELD_WRITER_ARRAY);
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
            mw.visitTypeInsn(192, ASMUtils.TYPE_FIELD_WRITER);
            mw.visitFieldInsn(181, classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         }
      }

      mw.visitInsn(177);
      mw.visitMaxs(7, 7);
   }

   private void genFields(List<FieldWriter> fieldWriters, ClassWriter cw, String objectWriterSupper) {
      if (objectWriterSupper == ASMUtils.TYPE_OBJECT_WRITER_ADAPTER) {
         for (int i = 0; i < fieldWriters.size(); i++) {
            cw.visitField(1, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         }
      }
   }

   @Override
   public <T> FieldWriter<T> createFieldWriter(
      ObjectWriterProvider provider,
      String fieldName,
      int ordinal,
      long features,
      String format,
      Locale locale,
      String label,
      Field field,
      ObjectWriter initObjectWriter
   ) {
      Class<?> declaringClass = field.getDeclaringClass();
      if (!Throwable.class.isAssignableFrom(declaringClass) && !declaringClass.getName().startsWith("java.lang")) {
         Class<?> fieldClass = field.getType();
         Type fieldType = field.getGenericType();
         if (initObjectWriter != null) {
            if (fieldClass == byte.class) {
               fieldClass = Byte.class;
               fieldType = Byte.class;
            } else if (fieldClass == short.class) {
               fieldClass = Short.class;
               fieldType = Short.class;
            } else if (fieldClass == float.class) {
               fieldClass = Float.class;
               fieldType = Float.class;
            } else if (fieldClass == double.class) {
               fieldClass = Double.class;
               fieldType = Double.class;
            } else if (fieldClass == boolean.class) {
               fieldClass = Boolean.class;
               fieldType = Boolean.class;
            }

            FieldWriterObject objImp = new FieldWriterObject(fieldName, ordinal, features, format, locale, label, fieldType, fieldClass, field, null);
            objImp.initValueClass = fieldClass;
            if (initObjectWriter != ObjectWriterBaseModule.VoidObjectWriter.INSTANCE) {
               objImp.initObjectWriter = initObjectWriter;
            }

            return objImp;
         } else if (fieldClass == boolean.class) {
            return new FieldWriterBoolValField(fieldName, ordinal, features, format, label, field, fieldClass);
         } else if (fieldClass == byte.class) {
            return new FieldWriterInt8ValField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == short.class) {
            return new FieldWriterInt16ValField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == int.class) {
            return new FieldWriterInt32Val<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == long.class) {
            return (FieldWriter<T>)(format != null && !format.isEmpty()
               ? new FieldWriterMillisField<>(fieldName, ordinal, features, format, label, field)
               : new FieldWriterInt64ValField<>(fieldName, ordinal, features, format, label, field));
         } else if (fieldClass == float.class) {
            return new FieldWriterFloatValField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == Float.class) {
            return new FieldWriterFloatField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == double.class) {
            return new FieldWriterDoubleValField<>(fieldName, ordinal, format, label, field);
         } else if (fieldClass == Double.class) {
            return new FieldWriterDoubleField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == char.class) {
            return new FieldWriterCharValField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == BigInteger.class) {
            return new FieldWriterBigIntField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == BigDecimal.class) {
            return new FieldWriterBigDecimalField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == Date.class) {
            if (format != null) {
               format = format.trim();
               if (format.isEmpty()) {
                  format = null;
               }
            }

            return new FieldWriterDateField<>(fieldName, ordinal, features, format, label, field);
         } else if (fieldClass == String.class) {
            return new FieldWriterStringField<>(fieldName, ordinal, features, format, label, field);
         } else {
            if (fieldClass.isEnum()) {
               BeanInfo beanInfo = provider.createBeanInfo();
               provider.getBeanInfo(beanInfo, fieldClass);
               boolean writeEnumAsJavaBean = beanInfo.writeEnumAsJavaBean;
               if (!writeEnumAsJavaBean) {
                  ObjectWriter objectWriter = provider.cache.get(fieldClass);
                  if (objectWriter != null && !(objectWriter instanceof ObjectWriterImplEnum)) {
                     writeEnumAsJavaBean = true;
                  }
               }

               Member enumValueField = BeanUtils.getEnumValueField(fieldClass, provider);
               if (enumValueField == null && !writeEnumAsJavaBean) {
                  String[] enumAnnotationNames = BeanUtils.getEnumAnnotationNames(fieldClass);
                  if (enumAnnotationNames == null) {
                     return new FieldWriterEnum(fieldName, ordinal, features, format, label, fieldType, (Class<? extends Enum>)fieldClass, field, null);
                  }
               }
            }

            if (fieldClass != List.class && fieldClass != ArrayList.class) {
               if (fieldClass.isArray()) {
                  Class<?> itemClass = fieldClass.getComponentType();
                  if (declaringClass == Throwable.class && "stackTrace".equals(fieldName)) {
                     try {
                        Method method = Throwable.class.getMethod("getStackTrace");
                        return new FieldWriterObjectArrayMethod<>(fieldName, itemClass, ordinal, features, format, label, fieldType, fieldClass, field, method);
                     } catch (NoSuchMethodException var18) {
                     }
                  }
               }

               if (fieldClass == BigDecimal[].class) {
                  return new FieldWriterObjectArrayField<>(
                     fieldName, BigDecimal.class, ordinal, features, format, label, BigDecimal[].class, BigDecimal[].class, field
                  );
               } else if (fieldClass == Float[].class) {
                  return new FieldWriterObjectArrayField<>(fieldName, Float.class, ordinal, features, format, label, Float[].class, Float[].class, field);
               } else if (fieldClass == Double[].class) {
                  return new FieldWriterObjectArrayField<>(fieldName, Float.class, ordinal, features, format, label, Double[].class, Double[].class, field);
               } else {
                  return TypeUtils.isFunction(fieldClass)
                     ? null
                     : new FieldWriterObject<>(fieldName, ordinal, features, format, locale, label, field.getGenericType(), fieldClass, field, null);
               }
            } else {
               Type itemType = null;
               if (fieldType instanceof ParameterizedType) {
                  itemType = ((ParameterizedType)fieldType).getActualTypeArguments()[0];
               }

               return new FieldWriterListField<>(fieldName, itemType, ordinal, features, format, label, fieldType, fieldClass, field);
            }
         }
      } else {
         return super.createFieldWriter(provider, fieldName, ordinal, features, format, locale, label, field, initObjectWriter);
      }
   }

   void genGetObject(ObjectWriterCreatorASM.MethodWriterContext mwc, FieldWriter fieldWriter, int i, int OBJECT) {
      MethodWriter mw = mwc.mw;
      Class objectClass = mwc.objectClass;
      String TYPE_OBJECT = objectClass == null ? "java/lang/Object" : ASMUtils.type(objectClass);
      Class fieldClass = fieldWriter.fieldClass;
      Member member = (Member)(fieldWriter.method != null ? fieldWriter.method : fieldWriter.field);
      Function function = fieldWriter.getFunction();
      if (member == null && function != null) {
         mw.visitVarInsn(25, 0);
         mw.visitFieldInsn(180, mwc.classNameType, fieldWriter(i), ASMUtils.DESC_FIELD_WRITER);
         mw.visitMethodInsn(182, ASMUtils.TYPE_FIELD_WRITER, "getFunction", "()Ljava/util/function/Function;", false);
         mw.visitVarInsn(25, OBJECT);
         mw.visitMethodInsn(185, ASMUtils.type(Function.class), "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
         mw.visitTypeInsn(192, ASMUtils.type(fieldClass));
      } else if (member instanceof Method) {
         mw.visitVarInsn(25, OBJECT);
         mw.visitTypeInsn(192, TYPE_OBJECT);
         mw.visitMethodInsn(182, TYPE_OBJECT, member.getName(), "()" + ASMUtils.desc(fieldClass), false);
      } else if (Modifier.isPublic(objectClass.getModifiers()) && Modifier.isPublic(member.getModifiers()) && !this.classLoader.isExternalClass(objectClass)) {
         mw.visitVarInsn(25, OBJECT);
         mw.visitTypeInsn(192, TYPE_OBJECT);
         mw.visitFieldInsn(180, TYPE_OBJECT, member.getName(), ASMUtils.desc(fieldClass));
      } else {
         Field field = (Field)member;
         String castToType = null;
         String methodName;
         String methodDes;
         if (fieldClass == int.class) {
            methodName = "getInt";
            methodDes = "(Ljava/lang/Object;J)I";
         } else if (fieldClass == long.class) {
            methodName = "getLong";
            methodDes = "(Ljava/lang/Object;J)J";
         } else if (fieldClass == float.class) {
            methodName = "getFloat";
            methodDes = "(Ljava/lang/Object;J)F";
         } else if (fieldClass == double.class) {
            methodName = "getDouble";
            methodDes = "(Ljava/lang/Object;J)D";
         } else if (fieldClass == char.class) {
            methodName = "getChar";
            methodDes = "(Ljava/lang/Object;J)C";
         } else if (fieldClass == byte.class) {
            methodName = "getByte";
            methodDes = "(Ljava/lang/Object;J)B";
         } else if (fieldClass == short.class) {
            methodName = "getShort";
            methodDes = "(Ljava/lang/Object;J)S";
         } else if (fieldClass == boolean.class) {
            methodName = "getBoolean";
            methodDes = "(Ljava/lang/Object;J)Z";
         } else {
            methodName = "getObject";
            methodDes = "(Ljava/lang/Object;J)Ljava/lang/Object;";
            if (fieldClass.isEnum()) {
               castToType = "java/lang/Enum";
            } else if (ObjectWriterProvider.isPrimitiveOrEnum(fieldClass)) {
               castToType = ASMUtils.type(fieldClass);
            } else if (fieldClass.isArray() && ObjectWriterProvider.isPrimitiveOrEnum(fieldClass.getComponentType())) {
               castToType = ASMUtils.type(fieldClass);
            } else if (Map.class.isAssignableFrom(fieldClass)) {
               castToType = "java/util/Map";
            } else if (List.class.isAssignableFrom(fieldClass)) {
               castToType = "java/util/List";
            } else if (Collection.class.isAssignableFrom(fieldClass)) {
               castToType = "java/util/Collection";
            }
         }

         mw.visitFieldInsn(178, ObjectWriterCreatorASMUtils.TYPE_UNSAFE_UTILS, "UNSAFE", "Lsun/misc/Unsafe;");
         mw.visitVarInsn(25, OBJECT);
         mw.visitLdcInsn(JDKUtils.UNSAFE.objectFieldOffset(field));
         mw.visitMethodInsn(182, "sun/misc/Unsafe", methodName, methodDes, false);
         if (castToType != null) {
            mw.visitTypeInsn(192, castToType);
         }
      }
   }

   static class MethodWriterContext {
      final ObjectWriterProvider provider;
      final Class objectClass;
      final long objectFeatures;
      final String classNameType;
      final MethodWriter mw;
      final Map<Object, Integer> variants = new LinkedHashMap<>();
      final boolean jsonb;
      int maxVariant;

      public MethodWriterContext(
         ObjectWriterProvider provider, Class objectClass, long objectFeatures, String classNameType, MethodWriter mw, int maxVariant, boolean jsonb
      ) {
         this.provider = provider;
         this.objectClass = objectClass;
         this.objectFeatures = objectFeatures;
         this.classNameType = classNameType;
         this.mw = mw;
         this.jsonb = jsonb;
         this.maxVariant = maxVariant;
      }

      int var(Object key) {
         Integer var = this.variants.get(key);
         if (var == null) {
            var = this.maxVariant;
            this.variants.put(key, var);
            if (key != long.class && key != double.class) {
               this.maxVariant++;
            } else {
               this.maxVariant += 2;
            }
         }

         return var;
      }

      int var2(Object key) {
         Integer var = this.variants.get(key);
         if (var == null) {
            var = this.maxVariant;
            this.variants.put(key, var);
            this.maxVariant += 2;
         }

         return var;
      }

      void genVariantsMethodBefore(boolean jsonb) {
         Label notDefault_ = new Label();
         Label end_ = new Label();
         this.mw.visitVarInsn(25, 1);
         this.mw.visitMethodInsn(182, ASMUtils.TYPE_JSON_WRITER, "getFeatures", "()J", false);
         this.mw.visitVarInsn(55, this.var2("CONTEXT_FEATURES"));
         if (!jsonb) {
            Label l1 = new Label();
            Label l2 = new Label();
            this.mw.visitVarInsn(25, 1);
            this.mw.visitFieldInsn(180, ASMUtils.TYPE_JSON_WRITER, "useSingleQuote", "Z");
            this.mw.visitJumpInsn(154, l1);
            this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
            this.mw.visitLdcInsn(JSONWriter.Feature.UnquoteFieldName.mask | JSONWriter.Feature.UseSingleQuotes.mask);
            this.mw.visitInsn(127);
            this.mw.visitInsn(9);
            this.mw.visitInsn(148);
            this.mw.visitJumpInsn(154, l1);
            this.mw.visitInsn(4);
            this.mw.visitJumpInsn(167, l2);
            this.mw.visitLabel(l1);
            this.mw.visitInsn(3);
            this.mw.visitLabel(l2);
            this.mw.visitVarInsn(54, this.var2("NAME_DIRECT"));
         } else {
            Label l1 = new Label();
            Label l2 = new Label();
            this.mw.visitVarInsn(25, 1);
            this.mw.visitFieldInsn(180, ASMUtils.TYPE_JSON_WRITER, "symbolTable", ObjectWriterCreatorASM.DESC_SYMBOL);
            this.mw.visitJumpInsn(199, l1);
            this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
            this.mw.visitLdcInsn(JSONWriter.Feature.WriteNameAsSymbol.mask);
            this.mw.visitInsn(127);
            this.mw.visitInsn(9);
            this.mw.visitInsn(148);
            this.mw.visitJumpInsn(154, l1);
            this.mw.visitInsn(4);
            this.mw.visitJumpInsn(167, l2);
            this.mw.visitLabel(l1);
            this.mw.visitInsn(3);
            this.mw.visitLabel(l2);
            this.mw.visitVarInsn(54, this.var2("NAME_DIRECT"));
         }

         this.genIsEnabledAndAssign(JSONWriter.Feature.NotWriteDefaultValue.mask, this.var("WRITE_DEFAULT_VALUE"));
         this.mw.visitVarInsn(21, this.var("WRITE_DEFAULT_VALUE"));
         this.mw.visitJumpInsn(153, notDefault_);
         this.mw.visitInsn(3);
         this.mw.visitVarInsn(54, this.var("WRITE_NULLS"));
         this.mw.visitJumpInsn(167, end_);
         this.mw.visitLabel(notDefault_);
         long features = JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask;
         this.genIsEnabledAndAssign(features, this.var("WRITE_NULLS"));
         this.mw.visitLabel(end_);
      }

      void genIsEnabled(long features, Label elseLabel) {
         this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
         this.mw.visitLdcInsn(features);
         this.mw.visitInsn(127);
         this.mw.visitInsn(9);
         this.mw.visitInsn(148);
         if (elseLabel != null) {
            this.mw.visitJumpInsn(153, elseLabel);
         }
      }

      void genIsDisabled(long features, Label elseLabel) {
         this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
         this.mw.visitLdcInsn(features);
         this.mw.visitInsn(127);
         this.mw.visitInsn(9);
         this.mw.visitInsn(148);
         this.mw.visitJumpInsn(154, elseLabel);
      }

      void genIsEnabled(long features, Label trueLabel, Label falseLabel) {
         this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
         this.mw.visitLdcInsn(features);
         this.mw.visitInsn(127);
         this.mw.visitInsn(9);
         this.mw.visitInsn(148);
         this.mw.visitJumpInsn(153, falseLabel);
         this.mw.visitJumpInsn(167, trueLabel);
      }

      void genIsEnabledAndAssign(long features, int var) {
         this.mw.visitVarInsn(22, this.var2("CONTEXT_FEATURES"));
         this.mw.visitLdcInsn(features);
         this.mw.visitInsn(127);
         this.mw.visitInsn(9);
         this.mw.visitInsn(148);
         this.mw.visitVarInsn(54, var);
      }

      private void loadFieldType(int fieldIndex, Type fieldType) {
         if (fieldType instanceof Class && fieldType.getTypeName().startsWith("java")) {
            this.mw.visitLdcInsn((Class)fieldType);
         } else {
            this.mw.visitVarInsn(25, 0);
            this.mw.visitFieldInsn(180, this.classNameType, ObjectWriterCreatorASM.fieldWriter(fieldIndex), ASMUtils.DESC_FIELD_WRITER);
            this.mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "fieldType", "Ljava/lang/reflect/Type;");
         }
      }

      private void loadFieldClass(int fieldIndex, Class fieldClass) {
         if (fieldClass.getName().startsWith("java")) {
            this.mw.visitLdcInsn(fieldClass);
         } else {
            this.mw.visitVarInsn(25, 0);
            this.mw.visitFieldInsn(180, this.classNameType, ObjectWriterCreatorASM.fieldWriter(fieldIndex), ASMUtils.DESC_FIELD_WRITER);
            this.mw.visitFieldInsn(180, ASMUtils.TYPE_FIELD_WRITER, "fieldClass", "Ljava/lang/Class;");
         }
      }

      private void ldcIFEQ(String varName, Number name1, Number name1SQ) {
         if (name1.longValue() == name1SQ.longValue()) {
            this.mw.visitLdcInsn(name1);
         } else {
            Label L1 = new Label();
            Label L2 = new Label();
            this.mw.visitVarInsn(21, this.var(varName));
            this.mw.visitJumpInsn(153, L1);
            this.mw.visitLdcInsn(name1);
            this.mw.visitJumpInsn(167, L2);
            this.mw.visitLabel(L1);
            this.mw.visitLdcInsn(name1SQ);
            this.mw.visitLabel(L2);
         }
      }

      public boolean disableSupportArrayMapping() {
         return (this.objectFeatures & 288230376151711744L) != 0L;
      }

      public boolean disableReferenceDetect() {
         return (this.objectFeatures & 144115188075855872L) != 0L;
      }

      public boolean disableSmartMatch() {
         return (this.objectFeatures & 288230376151711744L) != 0L;
      }

      public boolean disableAutoType() {
         return (this.objectFeatures & 576460752303423488L) != 0L;
      }

      public boolean disableJSONB() {
         return (this.objectFeatures & 1152921504606846976L) != 0L;
      }
   }
}
