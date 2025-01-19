package com.alibaba.fastjson2.internal.asm;

public class Label {
   static final int FLAG_DEBUG_ONLY = 1;
   static final int FLAG_JUMP_TARGET = 2;
   static final int FLAG_RESOLVED = 4;
   static final int FLAG_REACHABLE = 8;
   static final int FORWARD_REFERENCES_CAPACITY_INCREMENT = 6;
   static final int FORWARD_REFERENCE_TYPE_MASK = -268435456;
   static final int FORWARD_REFERENCE_TYPE_SHORT = 268435456;
   static final int FORWARD_REFERENCE_TYPE_WIDE = 536870912;
   static final int FORWARD_REFERENCE_HANDLE_MASK = 268435455;
   static final Label EMPTY_LIST = new Label();
   public Object info;
   short flags;
   int bytecodeOffset;
   private int[] forwardReferences;
   short outputStackMax;
   Frame frame;
   Label nextBasicBlock;
   Edge outgoingEdges;
   Label nextListElement;

   final Label getCanonicalInstance() {
      return this.frame == null ? this : this.frame.owner;
   }

   final void put(ByteVector code, int sourceInsnBytecodeOffset, boolean wideReference) {
      if ((this.flags & 4) == 0) {
         if (wideReference) {
            this.addForwardReference(sourceInsnBytecodeOffset, 536870912, code.length);
            code.putInt(-1);
         } else {
            this.addForwardReference(sourceInsnBytecodeOffset, 268435456, code.length);
            code.putShort(-1);
         }
      } else if (wideReference) {
         code.putInt(this.bytecodeOffset - sourceInsnBytecodeOffset);
      } else {
         code.putShort(this.bytecodeOffset - sourceInsnBytecodeOffset);
      }
   }

   private void addForwardReference(int sourceInsnBytecodeOffset, int referenceType, int referenceHandle) {
      if (this.forwardReferences == null) {
         this.forwardReferences = new int[6];
      }

      int lastElementIndex = this.forwardReferences[0];
      if (lastElementIndex + 2 >= this.forwardReferences.length) {
         int[] newValues = new int[this.forwardReferences.length + 6];
         System.arraycopy(this.forwardReferences, 0, newValues, 0, this.forwardReferences.length);
         this.forwardReferences = newValues;
      }

      this.forwardReferences[lastElementIndex + 1] = sourceInsnBytecodeOffset;
      this.forwardReferences[lastElementIndex + 2] = referenceType | referenceHandle;
      this.forwardReferences[0] = lastElementIndex + 2;
   }

   final boolean resolve(byte[] code, int bytecodeOffset) {
      this.flags = (short)(this.flags | 4);
      this.bytecodeOffset = bytecodeOffset;
      if (this.forwardReferences == null) {
         return false;
      } else {
         boolean hasAsmInstructions = false;

         for (int i = this.forwardReferences[0]; i > 0; i -= 2) {
            int sourceInsnBytecodeOffset = this.forwardReferences[i - 1];
            int reference = this.forwardReferences[i];
            int relativeOffset = bytecodeOffset - sourceInsnBytecodeOffset;
            int handle = reference & 268435455;
            if ((reference & -268435456) == 268435456) {
               if (relativeOffset < -32768 || relativeOffset > 32767) {
                  int opcode = code[sourceInsnBytecodeOffset] & 255;
                  if (opcode < 198) {
                     code[sourceInsnBytecodeOffset] = (byte)(opcode + 49);
                  } else {
                     code[sourceInsnBytecodeOffset] = (byte)(opcode + 20);
                  }

                  hasAsmInstructions = true;
               }
            } else {
               code[handle++] = (byte)(relativeOffset >>> 24);
               code[handle++] = (byte)(relativeOffset >>> 16);
            }

            code[handle++] = (byte)(relativeOffset >>> 8);
            code[handle] = (byte)relativeOffset;
         }

         return hasAsmInstructions;
      }
   }
}
