package com.alibaba.fastjson2.internal.asm;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.TypeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ClassWriter {
   private final Function<String, Class> typeProvider;
   private int version;
   private final SymbolTable symbolTable = new SymbolTable(this);
   private int accessFlags;
   private int thisClass;
   private int superClass;
   private int interfaceCount;
   private int[] interfaces;
   private FieldWriter firstField;
   private FieldWriter lastField;
   private MethodWriter firstMethod;
   private MethodWriter lastMethod;

   public ClassWriter(Function<String, Class> typeProvider) {
      this.typeProvider = typeProvider;
   }

   public final void visit(int version, int access, String name, String superName, String[] interfaces) {
      this.version = version;
      this.accessFlags = access;
      this.thisClass = this.symbolTable.setMajorVersionAndClassName(version & 65535, name);
      this.superClass = superName == null ? 0 : this.symbolTable.addConstantUtf8Reference(7, superName).index;
      if (interfaces != null && interfaces.length > 0) {
         this.interfaceCount = interfaces.length;
         this.interfaces = new int[this.interfaceCount];

         for (int i = 0; i < this.interfaceCount; i++) {
            this.interfaces[i] = this.symbolTable.addConstantUtf8Reference(7, interfaces[i]).index;
         }
      }
   }

   public final FieldWriter visitField(int access, String name, String descriptor) {
      FieldWriter fieldWriter = new FieldWriter(this.symbolTable, access, name, descriptor);
      if (this.firstField == null) {
         this.firstField = fieldWriter;
      } else {
         this.lastField.fv = fieldWriter;
      }

      return this.lastField = fieldWriter;
   }

   public final MethodWriter visitMethod(int access, String name, String descriptor, int codeInitCapacity) {
      MethodWriter methodWriter = new MethodWriter(this.symbolTable, access, name, descriptor, codeInitCapacity);
      if (this.firstMethod == null) {
         this.firstMethod = methodWriter;
      } else {
         this.lastMethod.mv = methodWriter;
      }

      return this.lastMethod = methodWriter;
   }

   public byte[] toByteArray() {
      int size = 24 + 2 * this.interfaceCount;
      int fieldsCount = 0;

      for (FieldWriter fieldWriter = this.firstField; fieldWriter != null; fieldWriter = fieldWriter.fv) {
         fieldsCount++;
         size += 8;
      }

      int methodsCount = 0;

      for (MethodWriter methodWriter = this.firstMethod; methodWriter != null; methodWriter = methodWriter.mv) {
         methodsCount++;
         size += methodWriter.computeMethodInfoSize();
      }

      int attributesCount = 0;
      size += this.symbolTable.constantPool.length;
      int constantPoolCount = this.symbolTable.constantPoolCount;
      if (constantPoolCount > 65535) {
         throw new JSONException("Class too large: " + this.symbolTable.className + ", constantPoolCount " + constantPoolCount);
      } else {
         ByteVector result = new ByteVector(size);
         result.putInt(-889275714).putInt(this.version);
         result.putShort(constantPoolCount).putByteArray(this.symbolTable.constantPool.data, 0, this.symbolTable.constantPool.length);
         int mask = 0;
         result.putShort(this.accessFlags & ~mask).putShort(this.thisClass).putShort(this.superClass);
         result.putShort(this.interfaceCount);

         for (int i = 0; i < this.interfaceCount; i++) {
            result.putShort(this.interfaces[i]);
         }

         result.putShort(fieldsCount);

         for (FieldWriter var13 = this.firstField; var13 != null; var13 = var13.fv) {
            var13.putFieldInfo(result);
         }

         result.putShort(methodsCount);
         boolean hasFrames = false;
         boolean hasAsmInstructions = false;

         for (MethodWriter var14 = this.firstMethod; var14 != null; var14 = var14.mv) {
            hasFrames |= var14.stackMapTableNumberOfEntries > 0;
            hasAsmInstructions |= var14.hasAsmInstructions;
            var14.putMethodInfo(result);
         }

         result.putShort(attributesCount);
         if (hasAsmInstructions) {
            throw new UnsupportedOperationException();
         } else {
            return result.data;
         }
      }
   }

   protected Class loadClass(String type) {
      switch (type) {
         case "java/util/List":
            return List.class;
         case "java/util/ArrayList":
            return ArrayList.class;
         case "java/lang/Object":
            return Object.class;
         default:
            String className1 = type.replace('/', '.');
            Class clazz = null;
            if (this.typeProvider != null) {
               clazz = this.typeProvider.apply(className1);
            }

            if (clazz == null) {
               clazz = TypeUtils.loadClass(className1);
            }

            return clazz;
      }
   }

   protected String getCommonSuperClass(String type1, String type2) {
      Class<?> class1 = this.loadClass(type1);
      if (class1 == null) {
         throw new JSONException("class not found " + type1);
      } else {
         Class<?> class2 = this.loadClass(type2);
         if (class2 == null) {
            return "java/lang/Object";
         } else if (class1.isAssignableFrom(class2)) {
            return type1;
         } else if (class2.isAssignableFrom(class1)) {
            return type2;
         } else if (!class1.isInterface() && !class2.isInterface()) {
            do {
               class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));

            return class1.getName().replace('.', '/');
         } else {
            return "java/lang/Object";
         }
      }
   }
}
