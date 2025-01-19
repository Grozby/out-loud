package com.alibaba.fastjson2.internal.asm;

final class SymbolTable {
   final ClassWriter classWriter;
   String className;
   private int entryCount;
   private Symbol[] entries;
   int constantPoolCount;
   final ByteVector constantPool;
   private int typeCount;
   Symbol[] typeTable;

   SymbolTable(ClassWriter classWriter) {
      this.classWriter = classWriter;
      this.entries = new Symbol[256];
      this.constantPoolCount = 1;
      this.constantPool = new ByteVector(4096);
   }

   int setMajorVersionAndClassName(int majorVersion, String className) {
      this.className = className;
      return this.addConstantUtf8Reference(7, className).index;
   }

   private Symbol put(Symbol entry) {
      if (this.entryCount > this.entries.length * 3 / 4) {
         int currentCapacity = this.entries.length;
         int newCapacity = currentCapacity * 2 + 1;
         Symbol[] newEntries = new Symbol[newCapacity];

         for (int i = currentCapacity - 1; i >= 0; i--) {
            Symbol currentEntry = this.entries[i];

            while (currentEntry != null) {
               int newCurrentEntryIndex = currentEntry.hashCode % newCapacity;
               Symbol nextEntry = currentEntry.next;
               currentEntry.next = newEntries[newCurrentEntryIndex];
               newEntries[newCurrentEntryIndex] = currentEntry;
               currentEntry = nextEntry;
            }
         }

         this.entries = newEntries;
      }

      this.entryCount++;
      int index = entry.hashCode % this.entries.length;
      entry.next = this.entries[index];
      return this.entries[index] = entry;
   }

   Symbol addConstantMemberReference(int tag, String owner, String name, String descriptor) {
      int hashCode = 2147483647 & tag + owner.hashCode() * name.hashCode() * descriptor.hashCode();

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == tag && entry.hashCode == hashCode && entry.owner.equals(owner) && entry.name.equals(name) && entry.value.equals(descriptor)) {
            return entry;
         }
      }

      this.constantPool.put122(tag, this.addConstantUtf8Reference(7, owner).index, this.addConstantNameAndType(name, descriptor));
      return this.put(new Symbol(this.constantPoolCount++, tag, owner, name, descriptor, 0L, hashCode));
   }

   Symbol addConstantIntegerOrFloat(int value) {
      int hashCode = 2147483647 & 3 + value;

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 3 && entry.hashCode == hashCode && entry.data == (long)value) {
            return entry;
         }
      }

      this.constantPool.putByte(3).putInt(value);
      return this.put(new Symbol(this.constantPoolCount++, 3, null, null, null, (long)value, hashCode));
   }

   Symbol addConstantLongOrDouble(long value) {
      int hashCode = 2147483647 & 5 + (int)value + (int)(value >>> 32);

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 5 && entry.hashCode == hashCode && entry.data == value) {
            return entry;
         }
      }

      int index = this.constantPoolCount;
      this.constantPool.putByte(5).putLong(value);
      this.constantPoolCount += 2;
      return this.put(new Symbol(index, 5, null, null, null, value, hashCode));
   }

   int addConstantNameAndType(String name, String descriptor) {
      int tag = 12;
      int hashCode = 2147483647 & 12 + name.hashCode() * descriptor.hashCode();

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 12 && entry.hashCode == hashCode && entry.name.equals(name) && entry.value.equals(descriptor)) {
            return entry.index;
         }
      }

      this.constantPool.put122(12, this.addConstantUtf8(name), this.addConstantUtf8(descriptor));
      return this.put(new Symbol(this.constantPoolCount++, 12, null, name, descriptor, 0L, hashCode)).index;
   }

   int addConstantUtf8(String value) {
      int CONSTANT_UTF8_TAG = 1;
      int hashCode = 2147483647 & 1 + value.hashCode();

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 1 && entry.hashCode == hashCode && entry.value.equals(value)) {
            return entry.index;
         }
      }

      this.constantPool.putByte(1).putUTF8(value);
      return this.put(new Symbol(this.constantPoolCount++, 1, null, null, value, 0L, hashCode)).index;
   }

   Symbol addConstantUtf8Reference(int tag, String value) {
      int hashCode = 2147483647 & tag + value.hashCode();

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == tag && entry.hashCode == hashCode && entry.value.equals(value)) {
            return entry;
         }
      }

      this.constantPool.put12(tag, this.addConstantUtf8(value));
      return this.put(new Symbol(this.constantPoolCount++, tag, null, null, value, 0L, hashCode));
   }

   int addType(String value) {
      int TYPE_TAG = 128;
      int hashCode = 2147483647 & 128 + value.hashCode();

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 128 && entry.hashCode == hashCode && entry.value.equals(value)) {
            return entry.index;
         }
      }

      return this.addTypeInternal(new Symbol(this.typeCount, 128, null, null, value, 0L, hashCode));
   }

   int addUninitializedType(String value, int bytecodeOffset) {
      int UNINITIALIZED_TYPE_TAG = 129;
      int hashCode = 2147483647 & 129 + value.hashCode() + bytecodeOffset;

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 129 && entry.hashCode == hashCode && entry.data == (long)bytecodeOffset && entry.value.equals(value)) {
            return entry.index;
         }
      }

      return this.addTypeInternal(new Symbol(this.typeCount, 129, null, null, value, (long)bytecodeOffset, hashCode));
   }

   int addMergedType(int typeTableIndex1, int typeTableIndex2) {
      int MERGED_TYPE_TAG = 130;
      long data = typeTableIndex1 < typeTableIndex2 ? (long)typeTableIndex1 | (long)typeTableIndex2 << 32 : (long)typeTableIndex2 | (long)typeTableIndex1 << 32;
      int hashCode = 2147483647 & 130 + typeTableIndex1 + typeTableIndex2;

      for (Symbol entry = this.entries[hashCode % this.entries.length]; entry != null; entry = entry.next) {
         if (entry.tag == 130 && entry.hashCode == hashCode && entry.data == data) {
            return entry.info;
         }
      }

      String type1 = this.typeTable[typeTableIndex1].value;
      String type2 = this.typeTable[typeTableIndex2].value;
      int commonSuperTypeIndex = this.addType(this.classWriter.getCommonSuperClass(type1, type2));
      this.put(new Symbol(this.typeCount, 130, null, null, null, data, hashCode)).info = commonSuperTypeIndex;
      return commonSuperTypeIndex;
   }

   private int addTypeInternal(Symbol entry) {
      if (this.typeTable == null) {
         this.typeTable = new Symbol[16];
      }

      if (this.typeCount == this.typeTable.length) {
         Symbol[] newTypeTable = new Symbol[2 * this.typeTable.length];
         System.arraycopy(this.typeTable, 0, newTypeTable, 0, this.typeTable.length);
         this.typeTable = newTypeTable;
      }

      this.typeTable[this.typeCount++] = entry;
      return this.put(entry).index;
   }
}
