package com.alibaba.fastjson2.internal.asm;

import com.alibaba.fastjson2.JSONException;

public final class MethodWriter {
   MethodWriter mv;
   private final SymbolTable symbolTable;
   private final int accessFlags;
   private final int nameIndex;
   private final String name;
   private final int descriptorIndex;
   private final String descriptor;
   private int maxStack;
   private int maxLocals;
   private final ByteVector code;
   int stackMapTableNumberOfEntries;
   private ByteVector stackMapTableEntries;
   private final Label firstBasicBlock;
   private Label lastBasicBlock;
   private Label currentBasicBlock;
   private int[] previousFrame;
   private int[] currentFrame;
   boolean hasAsmInstructions;
   private int lastBytecodeOffset;

   MethodWriter(SymbolTable symbolTable, int access, String name, String descriptor, int codeInitCapacity) {
      this.symbolTable = symbolTable;
      this.accessFlags = "<init>".equals(name) ? access | 262144 : access;
      this.nameIndex = symbolTable.addConstantUtf8(name);
      this.name = name;
      this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
      this.descriptor = descriptor;
      this.code = new ByteVector(codeInitCapacity);
      int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
      if ((access & 8) != 0) {
         argumentsSize--;
      }

      this.maxLocals = argumentsSize;
      this.firstBasicBlock = new Label();
      this.visitLabel(this.firstBasicBlock);
   }

   public void visitInsn(int opcode) {
      this.lastBytecodeOffset = this.code.length;
      this.code.putByte(opcode);
      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, 0, null, null);
         if (opcode >= 172 && opcode <= 177 || opcode == 191) {
            this.endCurrentBasicBlockWithNoSuccessor();
         }
      }
   }

   public void visitIntInsn(int opcode, int operand) {
      this.lastBytecodeOffset = this.code.length;
      if (opcode == 17) {
         this.code.put12(opcode, operand);
      } else {
         this.code.put11(opcode, operand);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, operand, null, null);
      }
   }

   public void visitVarInsn(int opcode, int var) {
      this.lastBytecodeOffset = this.code.length;
      if (var < 4 && opcode != 169) {
         int optimizedOpcode;
         if (opcode < 54) {
            optimizedOpcode = 26 + (opcode - 21 << 2) + var;
         } else {
            optimizedOpcode = 59 + (opcode - 54 << 2) + var;
         }

         this.code.putByte(optimizedOpcode);
      } else if (var >= 256) {
         this.code.putByte(196).put12(opcode, var);
      } else {
         this.code.put11(opcode, var);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, var, null, null);
      }

      int currentMaxLocals;
      if (opcode != 22 && opcode != 24 && opcode != 55 && opcode != 57) {
         currentMaxLocals = var + 1;
      } else {
         currentMaxLocals = var + 2;
      }

      if (currentMaxLocals > this.maxLocals) {
         this.maxLocals = currentMaxLocals;
      }
   }

   public void visitTypeInsn(int opcode, String type) {
      this.lastBytecodeOffset = this.code.length;
      Symbol typeSymbol = this.symbolTable.addConstantUtf8Reference(7, type);
      this.code.put12(opcode, typeSymbol.index);
      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, this.lastBytecodeOffset, typeSymbol, this.symbolTable);
      }
   }

   public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      this.lastBytecodeOffset = this.code.length;
      Symbol fieldrefSymbol = this.symbolTable.addConstantMemberReference(9, owner, name, descriptor);
      this.code.put12(opcode, fieldrefSymbol.index);
      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, 0, fieldrefSymbol, this.symbolTable);
      }
   }

   public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      this.lastBytecodeOffset = this.code.length;
      Symbol methodrefSymbol = this.symbolTable.addConstantMemberReference(isInterface ? 11 : 10, owner, name, descriptor);
      if (opcode == 185) {
         this.code.put12(185, methodrefSymbol.index).put11(methodrefSymbol.getArgumentsAndReturnSizes() >> 2, 0);
      } else {
         this.code.put12(opcode, methodrefSymbol.index);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(opcode, 0, methodrefSymbol, this.symbolTable);
      }
   }

   public void visitJumpInsn(int opcode, Label label) {
      this.lastBytecodeOffset = this.code.length;
      int baseOpcode = opcode >= 200 ? opcode - 33 : opcode;
      boolean nextInsnIsJumpTarget = false;
      if ((label.flags & 4) != 0 && label.bytecodeOffset - this.code.length < -32768) {
         throw new JSONException("not supported");
      } else {
         if (baseOpcode != opcode) {
            this.code.putByte(opcode);
            label.put(this.code, this.code.length - 1, true);
         } else {
            this.code.putByte(baseOpcode);
            label.put(this.code, this.code.length - 1, false);
         }

         if (this.currentBasicBlock != null) {
            Label nextBasicBlock = null;
            this.currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
            Label var10000 = label.getCanonicalInstance();
            var10000.flags = (short)(var10000.flags | 2);
            this.addSuccessorToCurrentBasicBlock(label);
            if (baseOpcode != 167) {
               nextBasicBlock = new Label();
            }

            if (nextBasicBlock != null) {
               this.visitLabel(nextBasicBlock);
            }

            if (baseOpcode == 167) {
               this.endCurrentBasicBlockWithNoSuccessor();
            }
         }
      }
   }

   public void visitLabel(Label label) {
      this.hasAsmInstructions = this.hasAsmInstructions | label.resolve(this.code.data, this.code.length);
      if ((label.flags & 1) == 0) {
         if (this.currentBasicBlock != null) {
            if (label.bytecodeOffset == this.currentBasicBlock.bytecodeOffset) {
               this.currentBasicBlock.flags = (short)(this.currentBasicBlock.flags | label.flags & 2);
               label.frame = this.currentBasicBlock.frame;
               return;
            }

            this.addSuccessorToCurrentBasicBlock(label);
         }

         if (this.lastBasicBlock != null) {
            if (label.bytecodeOffset == this.lastBasicBlock.bytecodeOffset) {
               this.lastBasicBlock.flags = (short)(this.lastBasicBlock.flags | label.flags & 2);
               label.frame = this.lastBasicBlock.frame;
               this.currentBasicBlock = this.lastBasicBlock;
               return;
            }

            this.lastBasicBlock.nextBasicBlock = label;
         }

         this.lastBasicBlock = label;
         this.currentBasicBlock = label;
         label.frame = new Frame(label);
      }
   }

   public void visitLdcInsn(String value) {
      int CONSTANT_STRING_TAG = 8;
      this.lastBytecodeOffset = this.code.length;
      Symbol constantSymbol = this.symbolTable.addConstantUtf8Reference(8, value);
      int constantIndex = constantSymbol.index;
      if (constantIndex >= 256) {
         this.code.put12(19, constantIndex);
      } else {
         this.code.put11(18, constantIndex);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(18, 0, constantSymbol, this.symbolTable);
      }
   }

   public void visitLdcInsn(Class value) {
      String desc = ASMUtils.desc(value);
      Type type = Type.getTypeInternal(desc, 0, desc.length());
      this.lastBytecodeOffset = this.code.length;
      int typeSort = type.sort == 12 ? 10 : type.sort;
      Symbol constantSymbol;
      if (typeSort == 10) {
         constantSymbol = this.symbolTable.addConstantUtf8Reference(7, type.valueBuffer.substring(type.valueBegin, type.valueEnd));
      } else {
         constantSymbol = this.symbolTable.addConstantUtf8Reference(7, type.getDescriptor());
      }

      int constantIndex = constantSymbol.index;
      if (constantIndex >= 256) {
         this.code.put12(19, constantIndex);
      } else {
         this.code.put11(18, constantIndex);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(18, 0, constantSymbol, this.symbolTable);
      }
   }

   public void visitLdcInsn(Number value) {
      if (value instanceof Integer) {
         this.visitLdcInsn(value.intValue());
      } else {
         if (!(value instanceof Long)) {
            throw new UnsupportedOperationException(value.getClass().getName());
         }

         this.visitLdcInsn(value.longValue());
      }
   }

   public void visitLdcInsn(int value) {
      this.lastBytecodeOffset = this.code.length;
      Symbol constantSymbol = this.symbolTable.addConstantIntegerOrFloat(value);
      int constantIndex = constantSymbol.index;
      if (constantIndex >= 256) {
         this.code.put12(19, constantIndex);
      } else {
         this.code.put11(18, constantIndex);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(18, 0, constantSymbol, this.symbolTable);
      }
   }

   public void visitLdcInsn(long value) {
      this.lastBytecodeOffset = this.code.length;
      Symbol constantSymbol = this.symbolTable.addConstantLongOrDouble(value);
      int constantIndex = constantSymbol.index;
      this.code.put12(20, constantIndex);
      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(18, 0, constantSymbol, this.symbolTable);
      }
   }

   public void visitIincInsn(int var, int increment) {
      this.lastBytecodeOffset = this.code.length;
      if (var <= 255 && increment <= 127 && increment >= -128) {
         this.code.putByte(132).put11(var, increment);
      } else {
         this.code.putByte(196).put12(132, var).putShort(increment);
      }

      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(132, var, null, null);
      }
   }

   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      this.lastBytecodeOffset = this.code.length;
      this.code.putByte(171).putByteArray(null, 0, (4 - this.code.length % 4) % 4);
      dflt.put(this.code, this.lastBytecodeOffset, true);
      this.code.putInt(labels.length);

      for (int i = 0; i < labels.length; i++) {
         this.code.putInt(keys[i]);
         labels[i].put(this.code, this.lastBytecodeOffset, true);
      }

      this.visitSwitchInsn(dflt, labels);
   }

   private void visitSwitchInsn(Label dflt, Label[] labels) {
      if (this.currentBasicBlock != null) {
         this.currentBasicBlock.frame.execute(171, 0, null, null);
         this.addSuccessorToCurrentBasicBlock(dflt);
         Label var10000 = dflt.getCanonicalInstance();
         var10000.flags = (short)(var10000.flags | 2);

         for (int i = 0; i < labels.length; i++) {
            Label label = labels[i];
            this.addSuccessorToCurrentBasicBlock(label);
            var10000 = label.getCanonicalInstance();
            var10000.flags = (short)(var10000.flags | 2);
         }

         this.endCurrentBasicBlockWithNoSuccessor();
      }
   }

   public void visitMaxs(int maxStack, int maxLocals) {
      Frame firstFrame = this.firstBasicBlock.frame;
      firstFrame.setInputFrameFromDescriptor(this.symbolTable, this.accessFlags, this.descriptor, this.maxLocals);
      firstFrame.accept(this);
      Label listOfBlocksToProcess = this.firstBasicBlock;
      listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
      int maxStackSize = 0;

      while (listOfBlocksToProcess != Label.EMPTY_LIST) {
         Label basicBlock = listOfBlocksToProcess;
         listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
         basicBlock.nextListElement = null;
         basicBlock.flags = (short)(basicBlock.flags | 8);
         int maxBlockStackSize = basicBlock.frame.inputStack.length + basicBlock.outputStackMax;
         if (maxBlockStackSize > maxStackSize) {
            maxStackSize = maxBlockStackSize;
         }

         for (Edge outgoingEdge = basicBlock.outgoingEdges; outgoingEdge != null; outgoingEdge = outgoingEdge.nextEdge) {
            Label successorBlock = outgoingEdge.successor.getCanonicalInstance();
            boolean successorBlockChanged = basicBlock.frame.merge(this.symbolTable, successorBlock.frame);
            if (successorBlockChanged && successorBlock.nextListElement == null) {
               successorBlock.nextListElement = listOfBlocksToProcess;
               listOfBlocksToProcess = successorBlock;
            }
         }
      }

      for (Label basicBlock = this.firstBasicBlock; basicBlock != null; basicBlock = basicBlock.nextBasicBlock) {
         if ((basicBlock.flags & 10) == 10) {
            basicBlock.frame.accept(this);
         }

         if ((basicBlock.flags & 8) == 0) {
            Label nextBasicBlock = basicBlock.nextBasicBlock;
            int startOffset = basicBlock.bytecodeOffset;
            int endOffset = (nextBasicBlock == null ? this.code.length : nextBasicBlock.bytecodeOffset) - 1;
            if (endOffset >= startOffset) {
               for (int i = startOffset; i < endOffset; i++) {
                  this.code.data[i] = 0;
               }

               this.code.data[endOffset] = -65;
               int frameIndex = this.visitFrameStart(startOffset, 0, 1);
               this.currentFrame[frameIndex] = 8388608 | this.symbolTable.addType("java/lang/Throwable");
               this.visitFrameEnd();
               maxStackSize = Math.max(maxStackSize, 1);
            }
         }
      }

      this.maxStack = maxStackSize;
   }

   private void addSuccessorToCurrentBasicBlock(Label successor) {
      this.currentBasicBlock.outgoingEdges = new Edge(successor, this.currentBasicBlock.outgoingEdges);
   }

   private void endCurrentBasicBlockWithNoSuccessor() {
      Label nextBasicBlock = new Label();
      nextBasicBlock.frame = new Frame(nextBasicBlock);
      nextBasicBlock.resolve(this.code.data, this.code.length);
      this.lastBasicBlock.nextBasicBlock = nextBasicBlock;
      this.lastBasicBlock = nextBasicBlock;
      this.currentBasicBlock = null;
   }

   int visitFrameStart(int offset, int numLocal, int numStack) {
      int frameLength = 3 + numLocal + numStack;
      if (this.currentFrame == null || this.currentFrame.length < frameLength) {
         this.currentFrame = new int[frameLength];
      }

      this.currentFrame[0] = offset;
      this.currentFrame[1] = numLocal;
      this.currentFrame[2] = numStack;
      return 3;
   }

   void visitAbstractType(int frameIndex, int abstractType) {
      this.currentFrame[frameIndex] = abstractType;
   }

   void visitFrameEnd() {
      if (this.previousFrame != null) {
         if (this.stackMapTableEntries == null) {
            this.stackMapTableEntries = new ByteVector(2048);
         }

         this.putFrame();
         this.stackMapTableNumberOfEntries++;
      }

      this.previousFrame = this.currentFrame;
      this.currentFrame = null;
   }

   private void putFrame() {
      int numLocal = this.currentFrame[1];
      int numStack = this.currentFrame[2];
      int offsetDelta = this.stackMapTableNumberOfEntries == 0 ? this.currentFrame[0] : this.currentFrame[0] - this.previousFrame[0] - 1;
      int previousNumlocal = this.previousFrame[1];
      int numLocalDelta = numLocal - previousNumlocal;
      int type = 255;
      if (numStack == 0) {
         switch (numLocalDelta) {
            case -3:
            case -2:
            case -1:
               type = 248;
               break;
            case 0:
               type = offsetDelta < 64 ? 0 : 251;
               break;
            case 1:
            case 2:
            case 3:
               type = 252;
         }
      } else if (numLocalDelta == 0 && numStack == 1) {
         type = offsetDelta < 63 ? 64 : 247;
      }

      if (type != 255) {
         int frameIndex = 3;

         for (int i = 0; i < previousNumlocal && i < numLocal; i++) {
            if (this.currentFrame[frameIndex] != this.previousFrame[frameIndex]) {
               type = 255;
               break;
            }

            frameIndex++;
         }
      }

      switch (type) {
         case 0:
            this.stackMapTableEntries.putByte(offsetDelta);
            break;
         case 64:
            this.stackMapTableEntries.putByte(64 + offsetDelta);
            this.putAbstractTypes(3 + numLocal, 4 + numLocal);
            break;
         case 247:
            this.stackMapTableEntries.putByte(247).putShort(offsetDelta);
            this.putAbstractTypes(3 + numLocal, 4 + numLocal);
            break;
         case 248:
            this.stackMapTableEntries.putByte(251 + numLocalDelta).putShort(offsetDelta);
            break;
         case 251:
            this.stackMapTableEntries.putByte(251).putShort(offsetDelta);
            break;
         case 252:
            this.stackMapTableEntries.putByte(251 + numLocalDelta).putShort(offsetDelta);
            this.putAbstractTypes(3 + previousNumlocal, 3 + numLocal);
            break;
         case 255:
         default:
            this.stackMapTableEntries.putByte(255).putShort(offsetDelta).putShort(numLocal);
            this.putAbstractTypes(3, 3 + numLocal);
            this.stackMapTableEntries.putShort(numStack);
            this.putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
      }
   }

   private void putAbstractTypes(int start, int end) {
      for (int i = start; i < end; i++) {
         int abstractType = this.currentFrame[i];
         ByteVector output = this.stackMapTableEntries;
         int arrayDimensions = (abstractType & -67108864) >> 26;
         if (arrayDimensions == 0) {
            int typeValue = abstractType & 1048575;
            switch (abstractType & 62914560) {
               case 4194304:
                  output.putByte(typeValue);
                  break;
               case 8388608:
                  output.putByte(7).putShort(this.symbolTable.addConstantUtf8Reference(7, this.symbolTable.typeTable[typeValue].value).index);
                  break;
               case 12582912:
                  output.putByte(8).putShort((int)this.symbolTable.typeTable[typeValue].data);
                  break;
               default:
                  throw new AssertionError();
            }
         } else {
            StringBuilder typeDescriptor = new StringBuilder();

            while (arrayDimensions-- > 0) {
               typeDescriptor.append('[');
            }

            if ((abstractType & 62914560) == 8388608) {
               typeDescriptor.append('L').append(this.symbolTable.typeTable[abstractType & 1048575].value).append(';');
            } else {
               switch (abstractType & 1048575) {
                  case 1:
                     typeDescriptor.append('I');
                     break;
                  case 2:
                     typeDescriptor.append('F');
                     break;
                  case 3:
                     typeDescriptor.append('D');
                     break;
                  case 4:
                     typeDescriptor.append('J');
                     break;
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  default:
                     throw new AssertionError();
                  case 9:
                     typeDescriptor.append('Z');
                     break;
                  case 10:
                     typeDescriptor.append('B');
                     break;
                  case 11:
                     typeDescriptor.append('C');
                     break;
                  case 12:
                     typeDescriptor.append('S');
               }
            }

            output.putByte(7).putShort(this.symbolTable.addConstantUtf8Reference(7, typeDescriptor.toString()).index);
         }
      }
   }

   int computeMethodInfoSize() {
      int size = 8;
      if (this.code.length > 0) {
         if (this.code.length > 65535) {
            throw new JSONException(
               "Method too large: " + this.symbolTable.className + "." + this.name + " " + this.descriptor + ", length " + this.code.length
            );
         }

         this.symbolTable.addConstantUtf8("Code");
         size += 16 + this.code.length + 2;
         if (this.stackMapTableEntries != null) {
            this.symbolTable.addConstantUtf8("StackMapTable");
            size += 8 + this.stackMapTableEntries.length;
         }
      }

      return size;
   }

   void putMethodInfo(ByteVector output) {
      int mask = 0;
      output.putShort(this.accessFlags & ~mask).putShort(this.nameIndex).putShort(this.descriptorIndex);
      int attributeCount = 0;
      if (this.code.length > 0) {
         attributeCount++;
      }

      output.putShort(attributeCount);
      if (this.code.length > 0) {
         int size = 10 + this.code.length + 2;
         int codeAttributeCount = 0;
         if (this.stackMapTableEntries != null) {
            size += 8 + this.stackMapTableEntries.length;
            codeAttributeCount++;
         }

         output.putShort(this.symbolTable.addConstantUtf8("Code"))
            .putInt(size)
            .putShort(this.maxStack)
            .putShort(this.maxLocals)
            .putInt(this.code.length)
            .putByteArray(this.code.data, 0, this.code.length);
         output.putShort(0);
         output.putShort(codeAttributeCount);
         if (this.stackMapTableEntries != null) {
            boolean useStackMapTable = true;
            output.putShort(this.symbolTable.addConstantUtf8("StackMapTable"))
               .putInt(2 + this.stackMapTableEntries.length)
               .putShort(this.stackMapTableNumberOfEntries)
               .putByteArray(this.stackMapTableEntries.data, 0, this.stackMapTableEntries.length);
         }
      }
   }
}
