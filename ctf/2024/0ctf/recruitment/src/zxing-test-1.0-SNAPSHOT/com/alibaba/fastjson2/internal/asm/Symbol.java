package com.alibaba.fastjson2.internal.asm;

final class Symbol {
   final int index;
   final int tag;
   final String owner;
   final String name;
   final String value;
   final long data;
   int info;
   final int hashCode;
   Symbol next;

   Symbol(int index, int tag, String owner, String name, String value, long data, int hashCode) {
      this.index = index;
      this.tag = tag;
      this.owner = owner;
      this.name = name;
      this.value = value;
      this.data = data;
      this.hashCode = hashCode;
   }

   int getArgumentsAndReturnSizes() {
      if (this.info == 0) {
         this.info = Type.getArgumentsAndReturnSizes(this.value);
      }

      return this.info;
   }
}
