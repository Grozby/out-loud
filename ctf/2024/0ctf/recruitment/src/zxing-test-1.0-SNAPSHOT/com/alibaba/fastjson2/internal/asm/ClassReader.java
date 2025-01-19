package com.alibaba.fastjson2.internal.asm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassReader {
   public final byte[] b;
   private final int[] items;
   private final String[] strings;
   private final int maxStringLength;
   public final int header;

   public ClassReader(InputStream is) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];

      while (true) {
         int len = is.read(buf);
         if (len == -1) {
            is.close();
            this.b = out.toByteArray();
            this.items = new int[this.readUnsignedShort(8)];
            int n = this.items.length;
            this.strings = new String[n];
            int max = 0;
            len = 10;

            for (int i = 1; i < n; i++) {
               this.items[i] = len + 1;
               int size;
               switch (this.b[len]) {
                  case 1:
                     size = 3 + this.readUnsignedShort(len + 1);
                     if (size > max) {
                        max = size;
                     }
                     break;
                  case 2:
                  case 7:
                  case 8:
                  case 13:
                  case 14:
                  case 16:
                  case 17:
                  default:
                     size = 3;
                     break;
                  case 3:
                  case 4:
                  case 9:
                  case 10:
                  case 11:
                  case 12:
                  case 18:
                     size = 5;
                     break;
                  case 5:
                  case 6:
                     size = 9;
                     i++;
                     break;
                  case 15:
                     size = 4;
               }

               len += size;
            }

            this.maxStringLength = max;
            this.header = len;
            return;
         }

         if (len > 0) {
            out.write(buf, 0, len);
         }
      }
   }

   public void accept(TypeCollector classVisitor) {
      char[] c = new char[this.maxStringLength];
      int u = this.header;
      int len = this.readUnsignedShort(u + 6);
      u += 8;

      for (int i = 0; i < len; i++) {
         u += 2;
      }

      int var8 = this.readUnsignedShort(u);

      int v;
      for (v = u + 2; var8 > 0; var8--) {
         int j = this.readUnsignedShort(v + 6);

         for (v += 8; j > 0; j--) {
            v += 6 + this.readInt(v + 2);
         }
      }

      var8 = this.readUnsignedShort(v);

      for (v += 2; var8 > 0; var8--) {
         int j = this.readUnsignedShort(v + 6);

         for (v += 8; j > 0; j--) {
            v += 6 + this.readInt(v + 2);
         }
      }

      var8 = this.readUnsignedShort(v);

      for (int var19 = v + 2; var8 > 0; var8--) {
         var19 += 6 + this.readInt(var19 + 2);
      }

      var8 = this.readUnsignedShort(u);

      for (u += 2; var8 > 0; var8--) {
         int j = this.readUnsignedShort(u + 6);

         for (u += 8; j > 0; j--) {
            u += 6 + this.readInt(u + 2);
         }
      }

      var8 = this.readUnsignedShort(u);

      for (int var17 = u + 2; var8 > 0; var8--) {
         var17 = this.readMethod(classVisitor, c, var17);
      }
   }

   private int readMethod(TypeCollector classVisitor, char[] c, int u) {
      int access = this.readUnsignedShort(u);
      String name = this.readUTF8(u + 2, c);
      String desc = this.readUTF8(u + 4, c);
      int v = 0;
      int j = this.readUnsignedShort(u + 6);

      for (u += 8; j > 0; j--) {
         String attrName = this.readUTF8(u, c);
         int attrSize = this.readInt(u + 2);
         u += 6;
         if ("Code".equals(attrName)) {
            v = u;
         }

         u += attrSize;
      }

      MethodCollector mv = classVisitor.visitMethod(access, name, desc);
      if (mv != null && v != 0) {
         int codeLength = this.readInt(v + 4);
         v += 8;
         v += codeLength;
         j = this.readUnsignedShort(v);

         for (v += 2; j > 0; j--) {
            v += 8;
         }

         int varTable = 0;
         int varTypeTable = 0;
         j = this.readUnsignedShort(v);

         for (int var22 = v + 2; j > 0; j--) {
            String attrName = this.readUTF8(var22, c);
            if ("LocalVariableTable".equals(attrName)) {
               varTable = var22 + 6;
            } else if ("LocalVariableTypeTable".equals(attrName)) {
               varTypeTable = var22 + 6;
            }

            var22 += 6 + this.readInt(var22 + 2);
         }

         if (varTable != 0) {
            if (varTypeTable != 0) {
               int k = this.readUnsignedShort(varTypeTable) * 3;

               for (int w = varTypeTable + 2; k > 0; w += 10) {
                  k -= 3;
               }
            }

            int k = this.readUnsignedShort(varTable);

            for (int w = varTable + 2; k > 0; k--) {
               int index = this.readUnsignedShort(w + 8);
               mv.visitLocalVariable(this.readUTF8(w + 4, c), index);
               w += 10;
            }
         }
      }

      return u;
   }

   private int readUnsignedShort(int index) {
      byte[] b = this.b;
      return (b[index] & 0xFF) << 8 | b[index + 1] & 0xFF;
   }

   private int readInt(int index) {
      byte[] b = this.b;
      return (b[index] & 0xFF) << 24 | (b[index + 1] & 0xFF) << 16 | (b[index + 2] & 0xFF) << 8 | b[index + 3] & 0xFF;
   }

   private String readUTF8(int index, char[] buf) {
      int item = this.readUnsignedShort(index);
      String s = this.strings[item];
      if (s != null) {
         return s;
      } else {
         index = this.items[item];
         return this.strings[item] = this.readUTF(index + 2, this.readUnsignedShort(index), buf);
      }
   }

   private String readUTF(int index, int utfLen, char[] buf) {
      int endIndex = index + utfLen;
      byte[] b = this.b;
      int strLen = 0;
      int st = 0;
      char cc = 0;

      while (index < endIndex) {
         int c = b[index++];
         switch (st) {
            case 0:
               c &= 255;
               if (c < 128) {
                  buf[strLen++] = (char)c;
               } else {
                  if (c < 224 && c > 191) {
                     cc = (char)(c & 31);
                     st = 1;
                     continue;
                  }

                  cc = (char)(c & 15);
                  st = 2;
               }
               break;
            case 1:
               buf[strLen++] = (char)(cc << 6 | c & 63);
               st = 0;
               break;
            case 2:
               cc = (char)(cc << 6 | c & 63);
               st = 1;
         }
      }

      return new String(buf, 0, strLen);
   }
}
