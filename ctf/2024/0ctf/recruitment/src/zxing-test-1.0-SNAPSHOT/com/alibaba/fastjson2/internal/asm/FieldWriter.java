package com.alibaba.fastjson2.internal.asm;

public final class FieldWriter {
   FieldWriter fv;
   private final int accessFlags;
   private final int nameIndex;
   private final int descriptorIndex;

   FieldWriter(SymbolTable symbolTable, int access, String name, String descriptor) {
      this.accessFlags = access;
      this.nameIndex = symbolTable.addConstantUtf8(name);
      this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
   }

   void putFieldInfo(ByteVector output) {
      int mask = 0;
      output.putShort(this.accessFlags & ~mask).putShort(this.nameIndex).putShort(this.descriptorIndex);
      int attributesCount = 0;
      output.putShort(attributesCount);
   }
}
