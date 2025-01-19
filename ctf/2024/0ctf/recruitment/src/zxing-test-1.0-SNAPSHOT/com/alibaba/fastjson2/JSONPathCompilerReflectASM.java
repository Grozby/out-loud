package com.alibaba.fastjson2;

import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.internal.asm.ClassWriter;
import com.alibaba.fastjson2.internal.asm.MethodWriter;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.util.DynamicClassLoader;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLong;

class JSONPathCompilerReflectASM extends JSONPathCompilerReflect {
   static final AtomicLong seed = new AtomicLong();
   static final JSONPathCompilerReflectASM INSTANCE = new JSONPathCompilerReflectASM(DynamicClassLoader.getInstance());
   static final String DESC_OBJECT_READER = ASMUtils.desc(ObjectReader.class);
   static final String DESC_FIELD_READER = ASMUtils.desc(FieldReader.class);
   static final String DESC_OBJECT_WRITER = ASMUtils.desc(ObjectWriter.class);
   static final String DESC_FIELD_WRITER = ASMUtils.desc(FieldWriter.class);
   static final String TYPE_SINGLE_NAME_PATH_TYPED = ASMUtils.type(JSONPathCompilerReflect.SingleNamePathTyped.class);
   static final String METHOD_SINGLE_NAME_PATH_TYPED_INIT = "(Ljava/lang/String;Ljava/lang/Class;"
      + DESC_OBJECT_READER
      + DESC_FIELD_READER
      + DESC_OBJECT_WRITER
      + DESC_FIELD_WRITER
      + ")V";
   static final int THIS = 0;
   protected final DynamicClassLoader classLoader;

   public JSONPathCompilerReflectASM(DynamicClassLoader classLoader) {
      this.classLoader = classLoader;
   }

   private boolean support(Class objectClass) {
      boolean externalClass = this.classLoader.isExternalClass(objectClass);
      int objectClassModifiers = objectClass.getModifiers();
      return Modifier.isAbstract(objectClassModifiers)
         || Modifier.isInterface(objectClassModifiers)
         || !Modifier.isPublic(objectClassModifiers)
         || externalClass;
   }

   @Override
   protected JSONPath compileSingleNamePath(Class objectClass, JSONPathSingleName path) {
      if (this.support(objectClass)) {
         return super.compileSingleNamePath(objectClass, path);
      } else {
         String fieldName = path.name;
         String TYPE_OBJECT = ASMUtils.type(objectClass);
         ObjectReader objectReader = path.getReaderContext().getObjectReader(objectClass);
         FieldReader fieldReader = objectReader.getFieldReader(fieldName);
         ObjectWriter objectWriter = path.getWriterContext().getObjectWriter(objectClass);
         FieldWriter fieldWriter = objectWriter.getFieldWriter(fieldName);
         ClassWriter cw = new ClassWriter(null);
         String className = "JSONPath_" + seed.incrementAndGet();
         Package pkg = JSONPathCompilerReflectASM.class.getPackage();
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

         cw.visit(52, 49, classNameType, TYPE_SINGLE_NAME_PATH_TYPED, new String[0]);
         int PATH = 1;
         int CLASS = 2;
         int OBJECT_READER = 3;
         int FIELD_READER = 4;
         int OBJECT_WRITER = 5;
         int FIELD_WRITER = 6;
         MethodWriter mw = cw.visitMethod(1, "<init>", METHOD_SINGLE_NAME_PATH_TYPED_INIT, 64);
         mw.visitVarInsn(25, 0);
         mw.visitVarInsn(25, 1);
         mw.visitVarInsn(25, 2);
         mw.visitVarInsn(25, 3);
         mw.visitVarInsn(25, 4);
         mw.visitVarInsn(25, 5);
         mw.visitVarInsn(25, 6);
         mw.visitMethodInsn(183, TYPE_SINGLE_NAME_PATH_TYPED, "<init>", METHOD_SINGLE_NAME_PATH_TYPED_INIT, false);
         mw.visitInsn(177);
         mw.visitMaxs(3, 3);
         if (fieldReader != null) {
            Class fieldClass = fieldReader.fieldClass;
            CLASS = 1;
            OBJECT_READER = 2;
            if (fieldClass == int.class) {
               MethodWriter mwx = cw.visitMethod(1, "setInt", "(Ljava/lang/Object;I)V", 64);
               mwx.visitVarInsn(25, CLASS);
               mwx.visitTypeInsn(192, TYPE_OBJECT);
               mwx.visitVarInsn(21, OBJECT_READER);
               this.gwSetValue(mwx, TYPE_OBJECT, fieldReader);
               mwx.visitInsn(177);
               mwx.visitMaxs(2, 2);
            }

            if (fieldClass == long.class) {
               MethodWriter mwx = cw.visitMethod(1, "setLong", "(Ljava/lang/Object;J)V", 64);
               mwx.visitVarInsn(25, CLASS);
               mwx.visitTypeInsn(192, TYPE_OBJECT);
               mwx.visitVarInsn(22, OBJECT_READER);
               this.gwSetValue(mwx, TYPE_OBJECT, fieldReader);
               mwx.visitInsn(177);
               mwx.visitMaxs(2, 2);
            }

            MethodWriter mwx = cw.visitMethod(1, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", 64);
            mwx.visitVarInsn(25, CLASS);
            mwx.visitTypeInsn(192, TYPE_OBJECT);
            mwx.visitVarInsn(25, OBJECT_READER);
            if (fieldClass == int.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "intValue", "()I", false);
            } else if (fieldClass == long.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "longValue", "()J", false);
            } else if (fieldClass == float.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "floatValue", "()F", false);
            } else if (fieldClass == double.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "doubleValue", "()D", false);
            } else if (fieldClass == short.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "shortValue", "()S", false);
            } else if (fieldClass == byte.class) {
               mwx.visitTypeInsn(192, "java/lang/Number");
               mwx.visitMethodInsn(182, "java/lang/Number", "byteValue", "()B", false);
            } else if (fieldClass == boolean.class) {
               mwx.visitTypeInsn(192, "java/lang/Boolean");
               mwx.visitMethodInsn(182, "java/lang/Boolean", "booleanValue", "()Z", false);
            } else if (fieldClass == char.class) {
               mwx.visitTypeInsn(192, "java/lang/Character");
               mwx.visitMethodInsn(182, "java/lang/Character", "charValue", "()C", false);
            }

            this.gwSetValue(mwx, TYPE_OBJECT, fieldReader);
            mwx.visitInsn(177);
            mwx.visitMaxs(2, 2);
         }

         if (fieldWriter != null) {
            Class fieldClassx = fieldReader.fieldClass;
            CLASS = 1;
            MethodWriter mwx = cw.visitMethod(1, "eval", "(Ljava/lang/Object;)Ljava/lang/Object;", 64);
            mwx.visitVarInsn(25, CLASS);
            mwx.visitTypeInsn(192, TYPE_OBJECT);
            this.gwGetValue(mwx, TYPE_OBJECT, fieldWriter);
            if (fieldClassx == int.class) {
               mwx.visitMethodInsn(184, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (fieldClassx == long.class) {
               mwx.visitMethodInsn(184, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (fieldClassx == float.class) {
               mwx.visitMethodInsn(184, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (fieldClassx == double.class) {
               mwx.visitMethodInsn(184, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else if (fieldClassx == short.class) {
               mwx.visitMethodInsn(184, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (fieldClassx == byte.class) {
               mwx.visitMethodInsn(184, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (fieldClassx == boolean.class) {
               mwx.visitMethodInsn(184, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (fieldClassx == char.class) {
               mwx.visitMethodInsn(184, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            }

            mwx.visitInsn(176);
            mwx.visitMaxs(2, 2);
         }

         byte[] code = cw.toByteArray();
         Class<?> readerClass = this.classLoader.defineClassPublic(classNameFull, code, 0, code.length);

         try {
            Constructor<?> constructor = readerClass.getConstructors()[0];
            return (JSONPath)constructor.newInstance(path.path, objectClass, objectReader, fieldReader, objectWriter, fieldWriter);
         } catch (Throwable var21) {
            throw new JSONException("compile jsonpath error, path " + path.path + ", objectType " + objectClass.getTypeName(), var21);
         }
      }
   }

   private void gwSetValue(MethodWriter mw, String TYPE_OBJECT, FieldReader fieldReader) {
      Method method = fieldReader.method;
      Field field = fieldReader.field;
      Class fieldClass = fieldReader.fieldClass;
      String fieldClassDesc = ASMUtils.desc(fieldClass);
      if (method != null) {
         Class<?> returnType = method.getReturnType();
         String methodDesc = '(' + fieldClassDesc + ')' + ASMUtils.desc(returnType);
         mw.visitMethodInsn(182, TYPE_OBJECT, method.getName(), methodDesc, false);
         if (returnType != void.class) {
            mw.visitInsn(87);
         }
      } else {
         mw.visitFieldInsn(181, TYPE_OBJECT, field.getName(), fieldClassDesc);
      }
   }

   private void gwGetValue(MethodWriter mw, String TYPE_OBJECT, FieldWriter fieldWriter) {
      Method method = fieldWriter.method;
      Field field = fieldWriter.field;
      Class fieldClass = fieldWriter.fieldClass;
      String fieldClassDesc = ASMUtils.desc(fieldClass);
      if (method != null) {
         String methodDesc = "()" + fieldClassDesc;
         mw.visitMethodInsn(182, TYPE_OBJECT, method.getName(), methodDesc, false);
      } else {
         mw.visitFieldInsn(180, TYPE_OBJECT, field.getName(), fieldClassDesc);
      }
   }
}
