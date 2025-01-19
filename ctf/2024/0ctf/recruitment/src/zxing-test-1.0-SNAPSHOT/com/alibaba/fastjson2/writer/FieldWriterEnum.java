package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.SymbolTable;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class FieldWriterEnum extends FieldWriter {
   final byte[][] valueNameCacheUTF8;
   final char[][] valueNameCacheUTF16;
   final byte[][] utf8ValueCache;
   final char[][] utf16ValueCache;
   final Class enumType;
   final Enum[] enumConstants;
   final long[] hashCodes;
   final long[] hashCodesSymbolCache;

   protected FieldWriterEnum(
      String name, int ordinal, long features, String format, String label, Type fieldType, Class<? extends Enum> enumClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, null, label, fieldType, enumClass, field, method);
      this.enumType = enumClass;
      this.enumConstants = enumClass.getEnumConstants();
      this.hashCodes = new long[this.enumConstants.length];
      this.hashCodesSymbolCache = new long[this.enumConstants.length];

      for (int i = 0; i < this.enumConstants.length; i++) {
         this.hashCodes[i] = Fnv.hashCode64(this.enumConstants[i].name());
      }

      this.valueNameCacheUTF8 = new byte[this.enumConstants.length][];
      this.valueNameCacheUTF16 = new char[this.enumConstants.length][];
      this.utf8ValueCache = new byte[this.enumConstants.length][];
      this.utf16ValueCache = new char[this.enumConstants.length][];
   }

   @Override
   public final void writeEnumJSONB(JSONWriter jsonWriter, Enum e) {
      if (e != null) {
         long features = jsonWriter.getFeatures(this.features);
         boolean usingOrdinal = (features & (JSONWriter.Feature.WriteEnumUsingToString.mask | JSONWriter.Feature.WriteEnumsUsingName.mask)) == 0L;
         boolean usingToString = (features & JSONWriter.Feature.WriteEnumUsingToString.mask) != 0L;
         int ordinal = e.ordinal();
         SymbolTable symbolTable = jsonWriter.symbolTable;
         if (symbolTable == null || !usingOrdinal || usingToString || !this.writeSymbolNameOrdinal(jsonWriter, ordinal, symbolTable)) {
            if (usingToString) {
               this.writeJSONBToString(jsonWriter, e, symbolTable);
            } else if (usingOrdinal) {
               int symbol;
               if (symbolTable != null) {
                  int symbolTableIdentity = System.identityHashCode(symbolTable);
                  if (this.nameSymbolCache == 0L) {
                     symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
                     this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
                  } else {
                     int identity = (int)this.nameSymbolCache;
                     if (identity == symbolTableIdentity) {
                        symbol = (int)(this.nameSymbolCache >> 32);
                     } else {
                        symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
                        this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
                     }
                  }
               } else {
                  symbol = -1;
               }

               if (symbol != -1) {
                  jsonWriter.writeSymbol(-symbol);
               } else {
                  jsonWriter.writeNameRaw(this.nameJSONB, this.hashCode);
               }

               jsonWriter.writeInt32(ordinal);
            } else {
               this.writeFieldName(jsonWriter);
               jsonWriter.writeString(e.name());
            }
         }
      }
   }

   private boolean writeSymbolNameOrdinal(JSONWriter jsonWriter, int ordinal, SymbolTable symbolTable) {
      int symbolTableIdentity = System.identityHashCode(symbolTable);
      long enumNameCache = this.hashCodesSymbolCache[ordinal];
      int enumSymbol;
      if (enumNameCache == 0L) {
         enumSymbol = symbolTable.getOrdinalByHashCode(this.hashCodes[ordinal]);
         this.hashCodesSymbolCache[ordinal] = (long)enumSymbol << 32 | (long)symbolTableIdentity;
      } else {
         int identity = (int)enumNameCache;
         if (identity == symbolTableIdentity) {
            enumSymbol = (int)(enumNameCache >> 32);
         } else {
            enumSymbol = symbolTable.getOrdinalByHashCode(this.hashCodes[ordinal]);
            this.hashCodesSymbolCache[ordinal] = (long)enumSymbol << 32 | (long)symbolTableIdentity;
         }
      }

      if (enumSymbol >= 0) {
         int symbol;
         if (this.nameSymbolCache == 0L) {
            symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
            if (symbol != -1) {
               this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
            }
         } else {
            int identityx = (int)this.nameSymbolCache;
            if (identityx == symbolTableIdentity) {
               symbol = (int)(this.nameSymbolCache >> 32);
            } else {
               symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
               this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
            }
         }

         if (symbol != -1) {
            jsonWriter.writeSymbol(-symbol);
         } else {
            jsonWriter.writeNameRaw(this.nameJSONB, this.hashCode);
         }

         jsonWriter.writeRaw((byte)121);
         jsonWriter.writeInt32(-enumSymbol);
         return true;
      } else {
         return false;
      }
   }

   private void writeJSONBToString(JSONWriter jsonWriter, Enum e, SymbolTable symbolTable) {
      int symbol;
      if (symbolTable != null) {
         int symbolTableIdentity = System.identityHashCode(symbolTable);
         if (this.nameSymbolCache == 0L) {
            symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
            this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
         } else {
            int identity = (int)this.nameSymbolCache;
            if (identity == symbolTableIdentity) {
               symbol = (int)(this.nameSymbolCache >> 32);
            } else {
               symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
               this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
            }
         }
      } else {
         symbol = -1;
      }

      if (symbol != -1) {
         jsonWriter.writeSymbol(-symbol);
      } else {
         jsonWriter.writeNameRaw(this.nameJSONB, this.hashCode);
      }

      jsonWriter.writeString(e.toString());
   }

   @Override
   public final void writeEnum(JSONWriter jsonWriter, Enum e) {
      long features = jsonWriter.getFeatures(this.features);
      if ((features & JSONWriter.Feature.WriteEnumUsingToString.mask) == 0L) {
         if (jsonWriter.jsonb) {
            this.writeEnumJSONB(jsonWriter, e);
            return;
         }

         boolean unquoteName = (features & JSONWriter.Feature.UnquoteFieldName.mask) != 0L;
         boolean utf8 = jsonWriter.utf8;
         boolean utf16 = !jsonWriter.utf8 && jsonWriter.utf16;
         int ordinal = e.ordinal();
         if ((features & JSONWriter.Feature.WriteEnumUsingOrdinal.mask) != 0L) {
            if (!unquoteName) {
               if (utf8) {
                  byte[] bytes = this.utf8ValueCache[ordinal];
                  if (bytes == null) {
                     this.utf8ValueCache[ordinal] = bytes = this.getBytes(ordinal);
                  }

                  jsonWriter.writeNameRaw(bytes);
                  return;
               }

               if (utf16) {
                  char[] chars = this.utf16ValueCache[ordinal];
                  if (chars == null) {
                     this.utf16ValueCache[ordinal] = chars = this.getChars(ordinal);
                  }

                  jsonWriter.writeNameRaw(chars);
                  return;
               }
            }

            this.writeFieldName(jsonWriter);
            jsonWriter.writeInt32(ordinal);
            return;
         }

         if (!unquoteName) {
            if (utf8) {
               byte[] bytes = this.valueNameCacheUTF8[ordinal];
               if (bytes == null) {
                  this.valueNameCacheUTF8[ordinal] = bytes = this.getNameBytes(ordinal);
               }

               jsonWriter.writeNameRaw(bytes);
               return;
            }

            if (utf16) {
               char[] chars = this.valueNameCacheUTF16[ordinal];
               if (chars == null) {
                  this.valueNameCacheUTF16[ordinal] = chars = this.getNameChars(ordinal);
               }

               jsonWriter.writeNameRaw(chars);
               return;
            }
         }
      }

      this.writeFieldName(jsonWriter);
      jsonWriter.writeString(e.toString());
   }

   private char[] getNameChars(int ordinal) {
      String name = this.enumConstants[ordinal].name();
      char[] chars = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + name.length() + 2);
      chars[this.nameWithColonUTF16.length] = '"';
      name.getChars(0, name.length(), chars, this.nameWithColonUTF16.length + 1);
      chars[chars.length - 1] = '"';
      return chars;
   }

   private byte[] getNameBytes(int ordinal) {
      byte[] nameUft8Bytes = this.enumConstants[ordinal].name().getBytes(StandardCharsets.UTF_8);
      byte[] bytes = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + nameUft8Bytes.length + 2);
      bytes[this.nameWithColonUTF8.length] = 34;
      int index = this.nameWithColonUTF8.length + 1;

      for (byte b : nameUft8Bytes) {
         bytes[index++] = b;
      }

      bytes[bytes.length - 1] = 34;
      return bytes;
   }

   private char[] getChars(int ordinal) {
      int size = IOUtils.stringSize(ordinal);
      char[] original = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + size);
      char[] chars = Arrays.copyOf(original, original.length);
      IOUtils.getChars(ordinal, chars.length, chars);
      return chars;
   }

   private byte[] getBytes(int ordinal) {
      int size = IOUtils.stringSize(ordinal);
      byte[] original = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + size);
      byte[] bytes = Arrays.copyOf(original, original.length);
      IOUtils.getChars(ordinal, bytes.length, bytes);
      return bytes;
   }

   @Override
   public final void writeValue(JSONWriter jsonWriter, Object object) {
      Enum value = (Enum)this.getFieldValue(object);
      jsonWriter.writeEnum(value);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      Enum value = (Enum)this.getFieldValue(object);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNull();
            return true;
         } else {
            return false;
         }
      } else {
         if (jsonWriter.jsonb) {
            this.writeEnumJSONB(jsonWriter, value);
         } else {
            this.writeEnum(jsonWriter, value);
         }

         return true;
      }
   }
}
