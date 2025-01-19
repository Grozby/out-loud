package com.alibaba.fastjson2.internal.asm;

public final class Type {
   static final int VOID = 0;
   static final int BOOLEAN = 1;
   static final int CHAR = 2;
   static final int BYTE = 3;
   static final int SHORT = 4;
   static final int INT = 5;
   static final int FLOAT = 6;
   static final int LONG = 7;
   static final int DOUBLE = 8;
   static final int ARRAY = 9;
   static final int OBJECT = 10;
   static final int METHOD = 11;
   static final int INTERNAL = 12;
   static final Type VOID_TYPE = new Type(0, "VZCBSIFJD", 0, 1);
   static final Type BOOLEAN_TYPE = new Type(1, "VZCBSIFJD", 1, 2);
   static final Type CHAR_TYPE = new Type(2, "VZCBSIFJD", 2, 3);
   static final Type BYTE_TYPE = new Type(3, "VZCBSIFJD", 3, 4);
   static final Type SHORT_TYPE = new Type(4, "VZCBSIFJD", 4, 5);
   static final Type INT_TYPE = new Type(5, "VZCBSIFJD", 5, 6);
   static final Type FLOAT_TYPE = new Type(6, "VZCBSIFJD", 6, 7);
   static final Type LONG_TYPE = new Type(7, "VZCBSIFJD", 7, 8);
   static final Type DOUBLE_TYPE = new Type(8, "VZCBSIFJD", 8, 9);
   static final Type TYPE_CLASS = new Type(10, "Ljava/lang/Class;", 1, 16);
   static final Type TYPE_TYPE = new Type(10, "Ljava/lang/reflect/Type;", 1, 23);
   static final Type TYPE_OBJECT = new Type(10, "Ljava/lang/Object;", 1, 17);
   static final Type TYPE_STRING = new Type(10, "Ljava/lang/String;", 1, 17);
   static final Type TYPE_LIST = new Type(10, "Ljava/util/List;", 1, 15);
   static final Type TYPE_JSON_READER = new Type(10, "Lcom/alibaba/fastjson2/JSONReader;", 1, 33);
   static final Type TYPE_JSON_WRITER = new Type(10, "Lcom/alibaba/fastjson2/JSONWriter;", 1, 33);
   static final Type TYPE_SUPPLIER = new Type(10, "Ljava/util/function/Supplier;", 1, 28);
   static final Type[] TYPES_0 = new Type[]{TYPE_CLASS, TYPE_STRING, TYPE_STRING, LONG_TYPE, TYPE_LIST};
   static final Type[] TYPES_1 = new Type[]{TYPE_JSON_WRITER, TYPE_OBJECT, TYPE_OBJECT, TYPE_TYPE, LONG_TYPE};
   static final Type[] TYPES_2 = new Type[]{TYPE_CLASS, TYPE_SUPPLIER, TYPE_JSON_READER};
   static final Type[] TYPES_3 = new Type[]{LONG_TYPE};
   static final Type[] TYPES_4 = new Type[]{TYPE_JSON_READER, TYPE_TYPE, TYPE_OBJECT, LONG_TYPE};
   final int sort;
   final String valueBuffer;
   final int valueBegin;
   final int valueEnd;

   private Type(int sort, String valueBuffer, int valueBegin, int valueEnd) {
      this.sort = sort;
      this.valueBuffer = valueBuffer;
      this.valueBegin = valueBegin;
      this.valueEnd = valueEnd;
   }

   static Type[] getArgumentTypes(String methodDescriptor) {
      switch (methodDescriptor) {
         case "()V":
            return new Type[0];
         case "(J)Lcom/alibaba/fastjson2/reader/FieldReader;":
         case "(J)Ljava/lang/Object;":
            return TYPES_3;
         case "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/util/List;)V":
            return TYPES_0;
         case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;J)V":
            return TYPES_1;
         case "(Ljava/lang/Class;Ljava/util/function/Supplier;[Lcom/alibaba/fastjson2/reader/FieldReader;)V":
            return TYPES_2;
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/reflect/Type;Ljava/lang/Object;J)Ljava/lang/Object;":
            return TYPES_4;
         default:
            int numArgumentTypes = 0;
            int currentOffset = 1;

            for (; methodDescriptor.charAt(currentOffset) != ')'; numArgumentTypes++) {
               while (methodDescriptor.charAt(currentOffset) == '[') {
                  currentOffset++;
               }

               if (methodDescriptor.charAt(currentOffset++) == 'L') {
                  int semiColumnOffset = methodDescriptor.indexOf(59, currentOffset);
                  currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
               }
            }

            Type[] argumentTypes = new Type[numArgumentTypes];
            currentOffset = 1;
            int currentArgumentTypeIndex = 0;

            while (methodDescriptor.charAt(currentOffset) != ')') {
               int currentArgumentTypeOffset = currentOffset;

               while (methodDescriptor.charAt(currentOffset) == '[') {
                  currentOffset++;
               }

               if (methodDescriptor.charAt(currentOffset++) == 'L') {
                  int semiColumnOffset = methodDescriptor.indexOf(59, currentOffset);
                  currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
               }

               argumentTypes[currentArgumentTypeIndex++] = getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset);
            }

            return argumentTypes;
      }
   }

   static Type getTypeInternal(String descriptorBuffer, int descriptorBegin, int descriptorEnd) {
      switch (descriptorBuffer.charAt(descriptorBegin)) {
         case '(':
            return new Type(11, descriptorBuffer, descriptorBegin, descriptorEnd);
         case 'B':
            return BYTE_TYPE;
         case 'C':
            return CHAR_TYPE;
         case 'D':
            return DOUBLE_TYPE;
         case 'F':
            return FLOAT_TYPE;
         case 'I':
            return INT_TYPE;
         case 'J':
            return LONG_TYPE;
         case 'L':
            int len = descriptorEnd - descriptorBegin;
            switch (len) {
               case 16:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_LIST.valueBuffer, 0, len)) {
                     return TYPE_LIST;
                  }
                  break;
               case 17:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_CLASS.valueBuffer, 0, len)) {
                     return TYPE_CLASS;
                  }
                  break;
               case 18:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_STRING.valueBuffer, 0, len)) {
                     return TYPE_STRING;
                  }

                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_OBJECT.valueBuffer, 0, len)) {
                     return TYPE_OBJECT;
                  }
               case 19:
               case 20:
               case 21:
               case 22:
               case 23:
               case 25:
               case 26:
               case 27:
               case 28:
               case 30:
               case 31:
               case 32:
               case 33:
               default:
                  break;
               case 24:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_TYPE.valueBuffer, 0, len)) {
                     return TYPE_TYPE;
                  }
                  break;
               case 29:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_SUPPLIER.valueBuffer, 0, len)) {
                     return TYPE_SUPPLIER;
                  }
                  break;
               case 34:
                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_JSON_WRITER.valueBuffer, 0, len)) {
                     return TYPE_JSON_WRITER;
                  }

                  if (descriptorBuffer.regionMatches(descriptorBegin, TYPE_JSON_READER.valueBuffer, 0, len)) {
                     return TYPE_JSON_READER;
                  }
            }

            return new Type(10, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
         case 'S':
            return SHORT_TYPE;
         case 'V':
            return VOID_TYPE;
         case 'Z':
            return BOOLEAN_TYPE;
         case '[':
            return new Type(9, descriptorBuffer, descriptorBegin, descriptorEnd);
         default:
            throw new IllegalArgumentException();
      }
   }

   public String getDescriptor() {
      if (this.sort == 10) {
         String var3 = this.valueBuffer;
         switch (var3) {
            case "(Ljava/lang/Class;Ljava/util/function/Supplier;[Lcom/alibaba/fastjson2/reader/FieldReader;)V":
               if (this.valueBegin == 2 && this.valueEnd == 17) {
                  return "Ljava/lang/Class;";
               }

               if (this.valueBegin == 19 && this.valueEnd == 46) {
                  return "Ljava/util/function/Supplier;";
               }
               break;
            case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/reflect/Type;Ljava/lang/Object;J)Ljava/lang/Object;":
               if (this.valueBegin == 2 && this.valueEnd == 34) {
                  return "Lcom/alibaba/fastjson2/JSONReader;";
               }

               if (this.valueBegin == 36 && this.valueEnd == 58) {
                  return "Ljava/lang/reflect/Type;";
               }

               if (this.valueBegin == 60 && this.valueEnd == 76) {
                  return "Ljava/lang/Object;";
               }
               break;
            case "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/util/List;)V":
               if (this.valueBegin == 2 && this.valueEnd == 17) {
                  return "Ljava/lang/Class;";
               }

               if (this.valueBegin == 19 && this.valueEnd == 35) {
                  return "Ljava/lang/String;";
               }

               if (this.valueBegin == 37 && this.valueEnd == 53) {
                  return "Ljava/lang/String;";
               }

               if (this.valueBegin == 56 && this.valueEnd == 70) {
                  return "Ljava/util/List;";
               }
               break;
            case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;J)V":
               if (this.valueBegin == 2 && this.valueEnd == 34) {
                  return "Lcom/alibaba/fastjson2/JSONWriter;";
               }

               if (this.valueBegin == 36 && this.valueEnd == 52) {
                  return "Ljava/lang/Object;";
               }

               if (this.valueBegin == 54 && this.valueEnd == 70) {
                  return "Ljava/lang/Object;";
               }

               if (this.valueBegin == 72 && this.valueEnd == 94) {
                  return "Ljava/lang/reflect/Type;";
               }
         }

         return this.valueBegin == 1 && this.valueEnd + 1 == this.valueBuffer.length()
            ? this.valueBuffer
            : this.valueBuffer.substring(this.valueBegin - 1, this.valueEnd + 1);
      } else if (this.sort == 12) {
         return 'L' + this.valueBuffer.substring(this.valueBegin, this.valueEnd) + ';';
      } else {
         String var1 = this.valueBuffer;
         switch (var1) {
            case "VZCBSIFJD":
               if (this.valueBegin == 7 && this.valueEnd == 8) {
                  return "J";
               }
               break;
            case "(Ljava/lang/Class;Ljava/util/function/Supplier;[Lcom/alibaba/fastjson2/reader/FieldReader;)V":
               if (this.valueBegin == 47 && this.valueEnd == 90) {
                  return "[Lcom/alibaba/fastjson2/reader/FieldReader;";
               }
         }

         return this.valueBuffer.substring(this.valueBegin, this.valueEnd);
      }
   }

   public static int getArgumentsAndReturnSizes(String methodDescriptor) {
      switch (methodDescriptor) {
         case "()V":
            return 4;
         case "()I":
         case "()Z":
         case "()Ljava/lang/String;":
         case "()Ljava/lang/Class;":
            return 5;
         case "()J":
            return 6;
         case "(I)V":
         case "(Ljava/lang/String;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;)V":
         case "(Ljava/lang/Object;)V":
         case "(Ljava/lang/Enum;)V":
            return 8;
         case "(C)Z":
         case "(Lcom/alibaba/fastjson2/JSONReader;)Lcom/alibaba/fastjson2/reader/ObjectReader;":
         case "(Ljava/lang/Object;)Z":
         case "(I)Ljava/lang/Object;":
         case "(Lcom/alibaba/fastjson2/JSONReader;)Ljava/lang/Object;":
         case "(Lcom/alibaba/fastjson2/JSONWriter;)Z":
         case "(I)Ljava/lang/Integer;":
            return 9;
         case "(J)V":
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/Object;)V":
         case "(Ljava/util/List;Ljava/lang/reflect/Type;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/Enum;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;I)V":
            return 12;
         case "(J)Z":
         case "(J)Ljava/lang/Object;":
         case "(J)Lcom/alibaba/fastjson2/reader/FieldReader;":
         case "(Ljava/lang/Object;Ljava/lang/reflect/Type;)Z":
         case "(Lcom/alibaba/fastjson2/writer/FieldWriter;Ljava/lang/Object;)Ljava/lang/String;":
         case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/Class;)Lcom/alibaba/fastjson2/writer/ObjectWriter;":
         case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/reflect/Type;)Lcom/alibaba/fastjson2/writer/ObjectWriter;":
            return 13;
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/Object;Ljava/lang/String;)V":
         case "(Ljava/lang/Class;Ljava/util/function/Supplier;[Lcom/alibaba/fastjson2/reader/FieldReader;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;ZLjava/util/List;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;J)V":
            return 16;
         case "(Ljava/lang/Object;JLjava/lang/Object;)V":
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/util/List;ILjava/lang/String;)V":
            return 20;
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/Class;J)Ljava/lang/Object;":
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/Class;J)Lcom/alibaba/fastjson2/reader/ObjectReader;":
            return 21;
         case "(Lcom/alibaba/fastjson2/JSONReader;Ljava/lang/reflect/Type;Ljava/lang/Object;J)Ljava/lang/Object;":
            return 25;
         case "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLjava/util/List;)V":
         case "(Lcom/alibaba/fastjson2/JSONWriter;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;J)V":
            return 28;
         case "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;JLcom/alibaba/fastjson2/schema/JSONSchema;Ljava/util/function/Supplier;Ljava/util/function/Function;[Lcom/alibaba/fastjson2/reader/FieldReader;)V":
            return 40;
         default:
            int argumentsSize = 1;
            int currentOffset = 1;
            int currentChar = methodDescriptor.charAt(currentOffset);

            for (; currentChar != 41; currentChar = methodDescriptor.charAt(currentOffset)) {
               if (currentChar != 74 && currentChar != 68) {
                  while (methodDescriptor.charAt(currentOffset) == '[') {
                     currentOffset++;
                  }

                  if (methodDescriptor.charAt(currentOffset++) == 'L') {
                     int semiColumnOffset = methodDescriptor.indexOf(59, currentOffset);
                     currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
                  }

                  argumentsSize++;
               } else {
                  currentOffset++;
                  argumentsSize += 2;
               }
            }

            int var6 = methodDescriptor.charAt(currentOffset + 1);
            if (var6 == 'V') {
               return argumentsSize << 2;
            } else {
               int returnSize = var6 != 'J' && var6 != 'D' ? 1 : 2;
               return argumentsSize << 2 | returnSize;
            }
      }
   }

   public String getClassName() {
      switch (this.sort) {
         case 0:
            return "void";
         case 1:
            return "boolean";
         case 2:
            return "char";
         case 3:
            return "byte";
         case 4:
            return "short";
         case 5:
            return "int";
         case 6:
            return "float";
         case 7:
            return "long";
         case 8:
            return "double";
         case 9:
            Type elementType = getTypeInternal(this.valueBuffer, this.valueBegin + this.getDimensions(), this.valueEnd);
            StringBuilder stringBuilder = new StringBuilder(elementType.getClassName());

            for (int i = this.getDimensions(); i > 0; i--) {
               stringBuilder.append("[]");
            }

            return stringBuilder.toString();
         case 10:
         case 12:
            return this.valueBuffer.substring(this.valueBegin, this.valueEnd).replace('/', '.');
         case 11:
         default:
            throw new AssertionError();
      }
   }

   public int getDimensions() {
      int numDimensions = 1;

      while (this.valueBuffer.charAt(this.valueBegin + numDimensions) == '[') {
         numDimensions++;
      }

      return numDimensions;
   }
}
