package com.alibaba.fastjson2;

import com.alibaba.fastjson2.internal.trove.map.hash.TLongIntHashMap;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

final class JSONWriterJSONB extends JSONWriter {
   static final byte[] SHANGHAI_ZONE_ID_NAME_BYTES = JSONB.toBytes("Asia/Shanghai");
   static final byte[] OFFSET_8_ZONE_ID_NAME_BYTES = JSONB.toBytes("+08:00");
   static final long WRITE_ENUM_USING_STRING_MASK = JSONWriter.Feature.WriteEnumUsingToString.mask | JSONWriter.Feature.WriteEnumsUsingName.mask;
   private final JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1];
   private byte[] bytes;
   private TLongIntHashMap symbols;
   private int symbolIndex;
   private long rootTypeNameHash;
   static final long WRITE_NUM_NULL_MASK = JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask;

   JSONWriterJSONB(JSONWriter.Context ctx, SymbolTable symbolTable) {
      super(ctx, symbolTable, true, StandardCharsets.UTF_8);
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
      if (bytes == null) {
         bytes = new byte[8192];
      }

      this.bytes = bytes;
   }

   @Override
   public void close() {
      byte[] bytes = this.bytes;
      if (bytes.length < 4194304) {
         JSONFactory.BYTES_UPDATER.lazySet(this.cacheItem, bytes);
      }
   }

   @Override
   public void writeAny(Object value) {
      if (value == null) {
         this.writeNull();
      } else {
         boolean fieldBased = (this.context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
         Class<?> valueClass = value.getClass();
         ObjectWriter objectWriter = this.context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
         if (this.isBeanToArray()) {
            objectWriter.writeArrayMappingJSONB(this, value, null, null, 0L);
         } else {
            objectWriter.writeJSONB(this, value, null, null, 0L);
         }
      }
   }

   @Override
   public void startObject() {
      if (this.level >= this.context.maxLevel) {
         throw new JSONException("level too large : " + this.level);
      } else {
         this.level++;
         int off = this.off;
         if (off == this.bytes.length) {
            this.ensureCapacity(off + 1);
         }

         this.bytes[off] = -90;
         this.off = off + 1;
      }
   }

   @Override
   public void endObject() {
      this.level--;
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -91;
      this.off = off + 1;
   }

   @Override
   public void startArray() {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void startArray(Object array, int size) {
      if (this.isWriteTypeInfo(array)) {
         this.writeTypeName(array.getClass().getName());
      }

      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      boolean tinyInt = size <= 15;
      this.bytes[off] = tinyInt ? (byte)(-108 + size) : -92;
      this.off = off + 1;
      if (!tinyInt) {
         this.writeInt32(size);
      }
   }

   @Override
   public void startArray(int size) {
      int off = this.off;
      if (off + 1 >= this.bytes.length) {
         this.ensureCapacity(off + 2);
      }

      boolean tinyInt = size <= 15;
      this.bytes[off] = tinyInt ? (byte)(-108 + size) : -92;
      this.off = off + 1;
      if (!tinyInt) {
         this.writeInt32(size);
      }
   }

   @Override
   public void startArray0() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -108;
      this.off = off + 1;
   }

   @Override
   public void startArray1() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -107;
      this.off = off + 1;
   }

   @Override
   public void startArray2() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -106;
      this.off = off + 1;
   }

   @Override
   public void startArray3() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -105;
      this.off = off + 1;
   }

   @Override
   public void startArray4() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -104;
      this.off = off + 1;
   }

   @Override
   public void startArray5() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -103;
      this.off = off + 1;
   }

   @Override
   public void startArray6() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -102;
      this.off = off + 1;
   }

   @Override
   public void startArray7() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -101;
      this.off = off + 1;
   }

   @Override
   public void startArray8() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -100;
      this.off = off + 1;
   }

   @Override
   public void startArray9() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -99;
      this.off = off + 1;
   }

   @Override
   public void startArray10() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -98;
      this.off = off + 1;
   }

   @Override
   public void startArray11() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -97;
      this.off = off + 1;
   }

   @Override
   public void startArray12() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -96;
      this.off = off + 1;
   }

   @Override
   public void startArray13() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -95;
      this.off = off + 1;
   }

   @Override
   public void startArray14() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -94;
      this.off = off + 1;
   }

   @Override
   public void startArray15() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -93;
      this.off = off + 1;
   }

   @Override
   public void writeRaw(byte b) {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = b;
   }

   @Override
   public void writeChar(char ch) {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = -112;
      this.writeInt32(ch);
   }

   @Override
   public void writeName(String name) {
      this.writeString(name);
   }

   @Override
   public void writeNull() {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = -81;
   }

   @Override
   public void writeStringNull() {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = -81;
   }

   @Override
   public void endArray() {
   }

   @Override
   public void writeComma() {
      throw new JSONException("unsupported operation");
   }

   @Override
   protected void write0(char ch) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void writeString(boolean value) {
      this.writeString(Boolean.toString(value));
   }

   @Override
   public void writeString(byte value) {
      this.writeString(Integer.toString(value));
   }

   @Override
   public void writeString(short value) {
      this.writeString(Integer.toString(value));
   }

   @Override
   public void writeString(int value) {
      this.writeString(Integer.toString(value));
   }

   @Override
   public void writeString(long value) {
      this.writeString(Long.toString(value));
   }

   @Override
   public void writeString(boolean[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(byte[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(short[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(int[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(long[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(float[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(double[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray(value.length);

         for (int i = 0; i < value.length; i++) {
            this.writeString(value[i]);
         }
      }
   }

   @Override
   public void writeString(char[] chars, int off, int len, boolean quote) {
      if (chars == null) {
         this.writeNull();
      } else {
         boolean ascii = true;

         for (int i = 0; i < len; i++) {
            if (chars[i + off] > 255) {
               ascii = false;
               break;
            }
         }

         if (!ascii) {
            this.writeString(new String(chars, off, len));
         } else {
            if (len <= 47) {
               this.bytes[this.off++] = (byte)(len + 73);
            } else {
               this.bytes[this.off++] = 121;
               this.writeInt32(len);
            }

            for (int ix = 0; ix < len; ix++) {
               this.bytes[this.off++] = (byte)chars[off + ix];
            }
         }
      }
   }

   @Override
   public void writeStringLatin1(byte[] value) {
      if (value == null) {
         this.writeStringNull();
      } else {
         int off = this.off;
         int strlen = value.length;
         int minCapacity = value.length + off + 5 + 1;
         if (minCapacity - this.bytes.length > 0) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (strlen <= 47) {
            bytes[off++] = (byte)(strlen + 73);
         } else if (strlen <= 2047) {
            putStringSizeSmall(bytes, off, strlen);
            off += 3;
         } else {
            off += putStringSizeLarge(bytes, off, strlen);
         }

         System.arraycopy(value, 0, bytes, off, value.length);
         this.off = off + strlen;
      }
   }

   private static void putStringSizeSmall(byte[] bytes, int off, int val) {
      bytes[off] = 121;
      bytes[off + 1] = (byte)(56 + (val >> 8));
      bytes[off + 2] = (byte)val;
   }

   private static int putStringSizeLarge(byte[] bytes, int off, int strlen) {
      if (strlen <= 262143) {
         bytes[off] = 121;
         bytes[off + 1] = (byte)(68 + (strlen >> 16));
         bytes[off + 2] = (byte)(strlen >> 8);
         bytes[off + 3] = (byte)strlen;
         return 4;
      } else {
         bytes[off] = 121;
         bytes[off + 1] = 72;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 2L, JDKUtils.BIG_ENDIAN ? strlen : Integer.reverseBytes(strlen));
         return 6;
      }
   }

   @Override
   public void writeString(char[] chars) {
      if (chars == null) {
         this.writeNull();
      } else {
         int off = this.off;
         boolean ascii = true;
         int strlen = chars.length;
         if (chars.length < 47) {
            int minCapacity = off + 1 + strlen;
            if (minCapacity - this.bytes.length > 0) {
               this.ensureCapacity(minCapacity);
            }

            this.bytes[off++] = (byte)(strlen + 73);

            for (int i = 0; i < chars.length; i++) {
               char ch = chars[i];
               if (ch > 255) {
                  ascii = false;
                  break;
               }

               this.bytes[off++] = (byte)ch;
            }

            if (ascii) {
               this.off = off;
               return;
            }

            off = this.off;
         }

         int i = 0;

         for (int upperBound = chars.length & -4; i < upperBound; i += 4) {
            char c0 = chars[i];
            char c1 = chars[i + 1];
            char c2 = chars[i + 2];
            char c3 = chars[i + 3];
            if (c0 > 255 || c1 > 255 || c2 > 255 || c3 > 255) {
               ascii = false;
               break;
            }
         }

         if (ascii) {
            while (i < chars.length) {
               if (chars[i] > 255) {
                  ascii = false;
                  break;
               }

               i++;
            }
         }

         i = (ascii ? strlen : strlen * 3) + off + 5 + 1;
         if (i - this.bytes.length > 0) {
            this.ensureCapacity(i);
         }

         if (ascii) {
            if (strlen <= 47) {
               this.bytes[off++] = (byte)(strlen + 73);
            } else if (strlen <= 2047) {
               putStringSizeSmall(this.bytes, off, strlen);
               off += 3;
            } else {
               off += putStringSizeLarge(this.bytes, off, strlen);
            }

            for (int ix = 0; ix < chars.length; ix++) {
               this.bytes[off++] = (byte)chars[ix];
            }
         } else {
            int maxSize = chars.length * 3;
            int lenByteCnt = sizeOfInt(maxSize);
            this.ensureCapacity(off + maxSize + lenByteCnt + 1);
            int result = IOUtils.encodeUTF8(chars, 0, chars.length, this.bytes, off + lenByteCnt + 1);
            int utf8len = result - off - lenByteCnt - 1;
            int utf8lenByteCnt = sizeOfInt(utf8len);
            if (lenByteCnt != utf8lenByteCnt) {
               System.arraycopy(this.bytes, off + lenByteCnt + 1, this.bytes, off + utf8lenByteCnt + 1, utf8len);
            }

            byte[] bytes = this.bytes;
            bytes[off++] = 122;
            if (utf8len >= -16 && utf8len <= 47) {
               bytes[off++] = (byte)utf8len;
            } else if (utf8len >= -2048 && utf8len <= 2047) {
               bytes[off] = (byte)(56 + (utf8len >> 8));
               bytes[off + 1] = (byte)utf8len;
               off += 2;
            } else {
               off += writeInt32(bytes, off, utf8len);
            }

            off += utf8len;
         }

         this.off = off;
      }
   }

   @Override
   public void writeString(char[] chars, int charsOff, int len) {
      if (chars == null) {
         this.writeNull();
      } else {
         boolean ascii = true;
         if (len < 47) {
            int mark = this.off;
            int minCapacity = this.off + 1 + len;
            if (minCapacity - this.bytes.length > 0) {
               this.ensureCapacity(minCapacity);
            }

            this.bytes[this.off++] = (byte)(len + 73);

            for (int i = charsOff; i < len; i++) {
               char ch = chars[i];
               if (ch > 255) {
                  ascii = false;
                  break;
               }

               this.bytes[this.off++] = (byte)ch;
            }

            if (ascii) {
               return;
            }

            this.off = mark;
         }

         int i = charsOff;

         for (int upperBound = chars.length & -4; i < upperBound; i += 4) {
            char c0 = chars[i];
            char c1 = chars[i + 1];
            char c2 = chars[i + 2];
            char c3 = chars[i + 3];
            if (c0 > 255 || c1 > 255 || c2 > 255 || c3 > 255) {
               ascii = false;
               break;
            }
         }

         if (ascii) {
            while (i < chars.length) {
               if (chars[i] > 255) {
                  ascii = false;
                  break;
               }

               i++;
            }
         }

         i = (ascii ? len : len * 3) + this.off + 5 + 1;
         if (i - this.bytes.length > 0) {
            this.ensureCapacity(i);
         }

         if (ascii) {
            byte[] bytes = this.bytes;
            if (len <= 47) {
               bytes[this.off++] = (byte)(len + 73);
            } else if (len <= 2047) {
               int off = this.off;
               bytes[off] = 121;
               bytes[off + 1] = (byte)(56 + (len >> 8));
               bytes[off + 2] = (byte)len;
               this.off += 3;
            } else {
               bytes[this.off++] = 121;
               this.writeInt32(len);
            }

            for (int ix = 0; ix < chars.length; ix++) {
               bytes[this.off++] = (byte)chars[ix];
            }
         } else {
            int maxSize = chars.length * 3;
            int lenByteCnt = sizeOfInt(maxSize);
            this.ensureCapacity(this.off + maxSize + lenByteCnt + 1);
            int result = IOUtils.encodeUTF8(chars, 0, chars.length, this.bytes, this.off + lenByteCnt + 1);
            int utf8len = result - this.off - lenByteCnt - 1;
            int utf8lenByteCnt = sizeOfInt(utf8len);
            if (lenByteCnt != utf8lenByteCnt) {
               System.arraycopy(this.bytes, this.off + lenByteCnt + 1, this.bytes, this.off + utf8lenByteCnt + 1, utf8len);
            }

            this.bytes[this.off++] = 122;
            if (utf8len >= -16 && utf8len <= 47) {
               this.bytes[this.off++] = (byte)utf8len;
            } else if (utf8len >= -2048 && utf8len <= 2047) {
               this.bytes[this.off] = (byte)(56 + (utf8len >> 8));
               this.bytes[this.off + 1] = (byte)utf8len;
               this.off += 2;
            } else {
               this.writeInt32(utf8len);
            }

            this.off += utf8len;
         }
      }
   }

   @Override
   public void writeString(String[] strings) {
      if (strings == null) {
         this.writeArrayNull();
      } else {
         this.startArray(strings.length);

         for (int i = 0; i < strings.length; i++) {
            String item = strings[i];
            if (item == null) {
               this.writeStringNull();
            } else {
               this.writeString(item);
            }
         }
      }
   }

   @Override
   public void writeSymbol(String str) {
      if (str == null) {
         this.writeNull();
      } else {
         if (this.symbolTable != null) {
            int ordinal = this.symbolTable.getOrdinal(str);
            if (ordinal >= 0) {
               this.writeRaw((byte)127);
               this.writeInt32(-ordinal);
               return;
            }
         }

         this.writeString(str);
      }
   }

   @Override
   public void writeTypeName(String typeName) {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = -110;
      long hash = Fnv.hashCode64(typeName);
      int symbol = -1;
      if (this.symbolTable != null) {
         symbol = this.symbolTable.getOrdinalByHashCode(hash);
         if (symbol == -1 && this.symbols != null) {
            symbol = this.symbols.get(hash);
         }
      } else if (this.symbols != null) {
         symbol = this.symbols.get(hash);
      }

      if (symbol == -1) {
         if (this.symbols == null) {
            this.symbols = new TLongIntHashMap();
         }

         this.symbols.put(hash, symbol = this.symbolIndex++);
         this.writeString(typeName);
         this.writeInt32(symbol);
      } else {
         if (this.off == this.bytes.length) {
            this.ensureCapacity(this.off + 1);
         }

         this.writeInt32(symbol);
      }
   }

   @Override
   public boolean writeTypeName(byte[] typeName, long hash) {
      if (this.symbolTable != null) {
         int symbol = this.symbolTable.getOrdinalByHashCode(hash);
         if (symbol != -1) {
            return this.writeTypeNameSymbol(symbol);
         }
      }

      boolean symbolExists = false;
      int symbol;
      if (this.rootTypeNameHash == hash) {
         symbolExists = true;
         symbol = 0;
      } else if (this.symbols != null) {
         symbol = this.symbols.putIfAbsent(hash, this.symbolIndex);
         if (symbol != this.symbolIndex) {
            symbolExists = true;
         } else {
            this.symbolIndex++;
         }
      } else {
         symbol = this.symbolIndex++;
         if (symbol == 0) {
            this.rootTypeNameHash = hash;
         }

         if (symbol != 0 || (this.context.features & JSONWriter.Feature.WriteNameAsSymbol.mask) != 0L) {
            this.symbols = new TLongIntHashMap(hash, symbol);
         }
      }

      if (symbolExists) {
         this.writeTypeNameSymbol(-symbol);
         return false;
      } else {
         int off = this.off;
         int minCapacity = off + 2 + typeName.length;
         if (minCapacity > this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = -110;
         System.arraycopy(typeName, 0, bytes, off, typeName.length);
         off += typeName.length;
         if (symbol >= -16 && symbol <= 47) {
            bytes[off] = (byte)symbol;
            this.off = off + 1;
         } else {
            this.off = off;
            this.writeInt32(symbol);
         }

         return false;
      }
   }

   private boolean writeTypeNameSymbol(int symbol) {
      int off = this.off;
      if (off + 2 >= this.bytes.length) {
         this.ensureCapacity(off + 2);
      }

      this.bytes[off] = -110;
      this.off = off + 1;
      this.writeInt32(-symbol);
      return false;
   }

   static int sizeOfInt(int i) {
      if (i >= -16 && i <= 47) {
         return 1;
      } else if (i >= -2048 && i <= 2047) {
         return 2;
      } else {
         return i >= -262144 && i <= 262143 ? 3 : 5;
      }
   }

   @Override
   public void writeString(List<String> list) {
      if (list == null) {
         this.writeArrayNull();
      } else {
         int size = list.size();
         this.startArray(size);
         if (JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER != null) {
            int mark = this.off;
            int LATIN = 0;
            boolean latinAll = true;

            for (int i = 0; i < list.size(); i++) {
               String str = list.get(i);
               if (str == null) {
                  this.writeNull();
               } else {
                  int coder = JDKUtils.STRING_CODER.applyAsInt(str);
                  if (coder != 0) {
                     latinAll = false;
                     this.off = mark;
                     break;
                  }

                  int strlen = str.length();
                  if (this.off + strlen + 6 > this.bytes.length) {
                     this.ensureCapacity(this.off + strlen + 6);
                  }

                  if (strlen <= 47) {
                     this.bytes[this.off++] = (byte)(strlen + 73);
                  } else if (strlen <= 2047) {
                     int off = this.off;
                     this.bytes[off] = 121;
                     this.bytes[off + 1] = (byte)(56 + (strlen >> 8));
                     this.bytes[off + 2] = (byte)strlen;
                     this.off += 3;
                  } else {
                     this.bytes[this.off++] = 121;
                     this.writeInt32(strlen);
                  }

                  byte[] value = JDKUtils.STRING_VALUE.apply(str);
                  System.arraycopy(value, 0, this.bytes, this.off, value.length);
                  this.off += strlen;
               }
            }

            if (latinAll) {
               return;
            }
         }

         for (int ix = 0; ix < list.size(); ix++) {
            String str = list.get(ix);
            this.writeString(str);
         }
      }
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeNull();
      } else {
         if (JDKUtils.STRING_VALUE != null) {
            int coder = JDKUtils.STRING_CODER.applyAsInt(str);
            byte[] value = JDKUtils.STRING_VALUE.apply(str);
            if (coder == 0) {
               int off = this.off;
               int strlen = value.length;
               int minCapacity = value.length + off + 6;
               if (minCapacity - this.bytes.length > 0) {
                  this.ensureCapacity(minCapacity);
               }

               byte[] bytes = this.bytes;
               if (strlen <= 47) {
                  bytes[off++] = (byte)(strlen + 73);
               } else if (strlen <= 2047) {
                  putStringSizeSmall(bytes, off, strlen);
                  off += 3;
               } else {
                  off += putStringSizeLarge(bytes, off, strlen);
               }

               System.arraycopy(value, 0, bytes, off, value.length);
               this.off = off + strlen;
               return;
            }

            if (this.tryWriteStringUTF16(value)) {
               return;
            }
         }

         this.writeString(JDKUtils.getCharArray(str));
      }
   }

   @Override
   public void writeStringUTF16(byte[] value) {
      int off = this.off;
      int strlen = value.length;
      int minCapacity = off + strlen + 6;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = (byte)(JDKUtils.BIG_ENDIAN ? 125 : 124);
      off += writeInt32(bytes, off, strlen);
      System.arraycopy(value, 0, bytes, off, strlen);
      this.off = off + strlen;
   }

   protected boolean tryWriteStringUTF16(byte[] value) {
      int check_cnt = 128;
      if (check_cnt > value.length) {
         check_cnt = value.length;
      }

      if ((check_cnt & 1) == 1) {
         check_cnt--;
      }

      int asciiCount = 0;

      for (int i = 0; i + 2 <= check_cnt; i += 2) {
         byte b0 = value[i];
         byte b1 = value[i + 1];
         if (b0 == 0 || b1 == 0) {
            asciiCount++;
         }
      }

      boolean utf16 = value.length != 0 && (asciiCount == 0 || (check_cnt >> 1) / asciiCount >= 3);
      int off = this.off;
      int minCapacity = off + 6 + value.length * 2 + 1;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (!utf16) {
         int maxSize = value.length + (value.length >> 2);
         int lenByteCnt = sizeOfInt(maxSize);
         int result = IOUtils.encodeUTF8(value, 0, value.length, bytes, off + lenByteCnt + 1);
         int utf8len = result - off - lenByteCnt - 1;
         if (utf8len > value.length) {
            utf16 = true;
         } else if (result != -1) {
            this.off = off + writeUTF8(bytes, off, value, utf8len, asciiCount, lenByteCnt);
            return true;
         }
      }

      if (utf16) {
         this.off = off + writeUTF16(bytes, off, value);
         return true;
      } else {
         return false;
      }
   }

   private static int writeUTF8(byte[] bytes, int off, byte[] value, int utf8len, int asciiCount, int lenByteCnt) {
      byte strtype;
      if (utf8len * 2 == value.length) {
         if (asciiCount <= 47) {
            bytes[off] = (byte)(73 + utf8len);
            System.arraycopy(bytes, off + 1 + lenByteCnt, bytes, off + 1, utf8len);
            return utf8len + 1;
         }

         strtype = 121;
      } else {
         strtype = 122;
      }

      int utf8lenByteCnt = sizeOfInt(utf8len);
      if (lenByteCnt != utf8lenByteCnt) {
         System.arraycopy(bytes, off + lenByteCnt + 1, bytes, off + utf8lenByteCnt + 1, utf8len);
      }

      bytes[off] = strtype;
      return writeInt32(bytes, off + 1, utf8len) + utf8len + 1;
   }

   private static int writeUTF16(byte[] bytes, int off, byte[] value) {
      bytes[off] = (byte)(JDKUtils.BIG_ENDIAN ? 125 : 124);
      int size = writeInt32(bytes, off + 1, value.length);
      System.arraycopy(value, 0, bytes, off + size + 1, value.length);
      return value.length + size + 1;
   }

   void ensureCapacity(int minCapacity) {
      if (minCapacity >= this.bytes.length) {
         int oldCapacity = this.bytes.length;
         int newCapacity = oldCapacity + (oldCapacity >> 1);
         if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
         }

         if (newCapacity > this.maxArraySize) {
            throw new OutOfMemoryError("try enabling LargeObject feature instead");
         }

         this.bytes = Arrays.copyOf(this.bytes, newCapacity);
      }
   }

   @Override
   public void writeMillis(long millis) {
      int off = this.off;
      int minCapacity = off + 9;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (millis % 1000L == 0L) {
         long seconds = millis / 1000L;
         if (seconds >= -2147483648L && seconds <= 2147483647L) {
            int secondsInt = (int)seconds;
            bytes[off] = -84;
            JDKUtils.UNSAFE
               .putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? secondsInt : Integer.reverseBytes(secondsInt));
            this.off = off + 5;
            return;
         }

         if (seconds % 60L == 0L) {
            long minutes = seconds / 60L;
            if (minutes >= -2147483648L && minutes <= 2147483647L) {
               int minutesInt = (int)minutes;
               bytes[off] = -83;
               JDKUtils.UNSAFE
                  .putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? minutesInt : Integer.reverseBytes(minutesInt));
               this.off = off + 5;
               return;
            }
         }
      }

      bytes[off] = -85;
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
      this.off = off + 9;
   }

   @Override
   public void writeInt64(Long i) {
      int minCapacity = this.off + 9;
      if (minCapacity > this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      int size;
      if (i == null) {
         bytes[off] = (byte)((this.context.features & WRITE_NUM_NULL_MASK) == 0L ? -81 : -32);
         size = 1;
      } else {
         long val = i;
         if (val >= -8L && val <= 15L) {
            bytes[off] = (byte)((int)(-40L + (val - -8L)));
            size = 1;
         } else if (val >= -2048L && val <= 2047L) {
            bytes[off] = (byte)((int)(-48L + (val >> 8)));
            bytes[off + 1] = (byte)((int)val);
            size = 2;
         } else if (val >= -262144L && val <= 262143L) {
            bytes[off] = (byte)((int)(-60L + (val >> 16)));
            bytes[off + 1] = (byte)((int)(val >> 8));
            bytes[off + 2] = (byte)((int)val);
            size = 3;
         } else if (val >= -2147483648L && val <= 2147483647L) {
            bytes[off] = -65;
            JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? (int)val : Integer.reverseBytes((int)val));
            size = 5;
         } else {
            size = writeInt64Large8(bytes, off, val);
         }
      }

      this.off = off + size;
   }

   @Override
   public void writeInt64(long val) {
      int minCapacity = this.off + 9;
      if (minCapacity > this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      int size;
      if (val >= -8L && val <= 15L) {
         bytes[off] = (byte)((int)(-40L + (val - -8L)));
         size = 1;
      } else if (val >= -2048L && val <= 2047L) {
         bytes[off] = (byte)((int)(-48L + (val >> 8)));
         bytes[off + 1] = (byte)((int)val);
         size = 2;
      } else if (val >= -262144L && val <= 262143L) {
         bytes[off] = (byte)((int)(-60L + (val >> 16)));
         bytes[off + 1] = (byte)((int)(val >> 8));
         bytes[off + 2] = (byte)((int)val);
         size = 3;
      } else if (val >= -2147483648L && val <= 2147483647L) {
         bytes[off] = -65;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? (int)val : Integer.reverseBytes((int)val));
         size = 5;
      } else {
         bytes[off] = -66;
         JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Long.reverseBytes(val));
         size = 9;
      }

      this.off = off + size;
   }

   @Override
   public void writeInt64(long[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         int size = value.length;
         int off = this.off;
         int minCapacity = off + size * 9 + 5;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (size <= 15) {
            bytes[off++] = (byte)(-108 + size);
         } else {
            bytes[off] = -92;
            off += writeInt32(bytes, off + 1, size) + 1;
         }

         for (int i = 0; i < value.length; i++) {
            long val = value[i];
            if (val >= -8L && val <= 15L) {
               bytes[off++] = (byte)((int)(-40L + (val - -8L)));
            } else if (val >= -2048L && val <= 2047L) {
               bytes[off] = (byte)((int)(-48L + (val >> 8)));
               bytes[off + 1] = (byte)((int)val);
               off += 2;
            } else if (val >= -262144L && val <= 262143L) {
               bytes[off] = (byte)((int)(-60L + (val >> 16)));
               bytes[off + 1] = (byte)((int)(val >> 8));
               bytes[off + 2] = (byte)((int)val);
               off += 3;
            } else if (val >= -2147483648L && val <= 2147483647L) {
               bytes[off] = -65;
               JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? (int)val : Integer.reverseBytes((int)val));
               off += 5;
            } else {
               bytes[off] = -66;
               JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Long.reverseBytes(val));
               off += 9;
            }
         }

         this.off = off;
      }
   }

   @Override
   public void writeListInt64(List<Long> values) {
      if (values == null) {
         this.writeArrayNull();
      } else {
         int size = values.size();
         int off = this.off;
         int minCapacity = off + size * 9 + 5;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (size <= 15) {
            bytes[off++] = (byte)(-108 + size);
         } else {
            bytes[off] = -92;
            off += writeInt32(bytes, off + 1, size) + 1;
         }

         for (int i = 0; i < size; i++) {
            Long item = values.get(i);
            if (item == null) {
               bytes[off++] = -81;
            } else {
               long val = item;
               if (val >= -8L && val <= 15L) {
                  bytes[off++] = (byte)((int)(-40L + (val - -8L)));
               } else if (val >= -2048L && val <= 2047L) {
                  bytes[off] = (byte)((int)(-48L + (val >> 8)));
                  bytes[off + 1] = (byte)((int)val);
                  off += 2;
               } else if (val >= -262144L && val <= 262143L) {
                  bytes[off] = (byte)((int)(-60L + (val >> 16)));
                  bytes[off + 1] = (byte)((int)(val >> 8));
                  bytes[off + 2] = (byte)((int)val);
                  off += 3;
               } else {
                  off += writeInt64Large(bytes, off, val);
               }
            }
         }

         this.off = off;
      }
   }

   private static int writeInt64Large(byte[] bytes, int off, long val) {
      if (val >= -2147483648L && val <= 2147483647L) {
         bytes[off] = -65;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? (int)val : Integer.reverseBytes((int)val));
         return 5;
      } else {
         return writeInt64Large8(bytes, off, val);
      }
   }

   private static int writeInt64Large8(byte[] bytes, int off, long val) {
      bytes[off] = -66;
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Long.reverseBytes(val));
      return 9;
   }

   @Override
   public void writeFloat(float value) {
      int off = this.off;
      int minCapacity = off + 5;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int i = (int)value;
      if ((float)i == value && value >= -16.0F && value <= 47.0F) {
         bytes[off] = -74;
         bytes[off + 1] = (byte)i;
         off += 2;
      } else {
         bytes[off] = -73;
         i = Float.floatToIntBits(value);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? i : Integer.reverseBytes(i));
         off += 5;
      }

      this.off = off;
   }

   @Override
   public void writeFloat(float[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         this.startArray(values.length);

         for (int i = 0; i < values.length; i++) {
            this.writeFloat(values[i]);
         }

         this.endArray();
      }
   }

   @Override
   public void writeDouble(double value) {
      if (value == 0.0) {
         this.ensureCapacity(this.off + 1);
         this.bytes[this.off++] = -78;
      } else {
         int off = this.off;
         if (value == 1.0) {
            this.ensureCapacity(off + 1);
            this.bytes[off] = -77;
            this.off = off + 1;
         } else {
            if (value >= -2.1474836E9F && value <= 2.147483647E9) {
               long longValue = (long)value;
               if ((double)longValue == value) {
                  this.ensureCapacity(off + 1);
                  this.bytes[off] = -76;
                  this.off = off + 1;
                  this.writeInt64(longValue);
                  return;
               }
            }

            this.ensureCapacity(off + 9);
            byte[] bytes = this.bytes;
            bytes[off] = -75;
            long i = Double.doubleToLongBits(value);
            JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? i : Long.reverseBytes(i));
            this.off = off + 9;
         }
      }
   }

   @Override
   public void writeDouble(double[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         this.startArray(values.length);

         for (int i = 0; i < values.length; i++) {
            this.writeDouble(values[i]);
         }

         this.endArray();
      }
   }

   @Override
   public void writeInt16(short[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         this.startArray(values.length);

         for (int i = 0; i < values.length; i++) {
            this.writeInt32(values[i]);
         }

         this.endArray();
      }
   }

   @Override
   public void writeInt32(int[] values) {
      if (values == null) {
         this.writeArrayNull();
      } else {
         int size = values.length;
         if (this.off == this.bytes.length) {
            this.ensureCapacity(this.off + 1);
         }

         if (size <= 15) {
            this.bytes[this.off++] = (byte)(-108 + size);
         } else {
            this.bytes[this.off++] = -92;
            this.writeInt32(size);
         }

         int off = this.off;
         int minCapacity = off + values.length * 5;
         if (minCapacity - this.bytes.length > 0) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;

         for (int i = 0; i < values.length; i++) {
            int val = values[i];
            if (val >= -16 && val <= 47) {
               bytes[off++] = (byte)val;
            } else if (val >= -2048 && val <= 2047) {
               bytes[off++] = (byte)(56 + (val >> 8));
               bytes[off++] = (byte)val;
            } else if (val >= -262144 && val <= 262143) {
               bytes[off] = (byte)(68 + (val >> 16));
               bytes[off + 1] = (byte)(val >> 8);
               bytes[off + 2] = (byte)val;
               off += 3;
            } else {
               bytes[off] = 72;
               JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Integer.reverseBytes(val));
               off += 5;
            }
         }

         this.off = off;
      }
   }

   @Override
   public void writeInt8(byte[] values) {
      if (values == null) {
         this.writeArrayNull();
      } else {
         int size = values.length;
         if (this.off == this.bytes.length) {
            this.ensureCapacity(this.off + 1);
         }

         if (size <= 15) {
            this.bytes[this.off++] = (byte)(-108 + size);
         } else {
            this.bytes[this.off++] = -92;
            this.writeInt32(size);
         }

         int off = this.off;
         int minCapacity = off + values.length * 2;
         if (minCapacity - this.bytes.length > 0) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;

         for (int i = 0; i < values.length; i++) {
            int val = values[i];
            if (val >= -16 && val <= 47) {
               bytes[off++] = (byte)val;
            } else {
               bytes[off++] = (byte)(56 + (val >> 8));
               bytes[off++] = (byte)val;
            }
         }

         this.off = off;
      }
   }

   @Override
   public void writeInt8(byte val) {
      int off = this.off;
      int minCapacity = off + 2;
      if (minCapacity - this.bytes.length > 0) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = -67;
      bytes[off + 1] = val;
      this.off = off + 2;
   }

   @Override
   public void writeInt16(short val) {
      int off = this.off;
      int minCapacity = off + 3;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = -68;
      bytes[off + 1] = (byte)(val >>> 8);
      bytes[off + 2] = (byte)val;
      this.off = off + 3;
   }

   @Override
   public void writeEnum(Enum e) {
      if (e == null) {
         this.writeNull();
      } else {
         if ((this.context.features & WRITE_ENUM_USING_STRING_MASK) != 0L) {
            this.writeString((this.context.features & JSONWriter.Feature.WriteEnumUsingToString.mask) != 0L ? e.toString() : e.name());
         } else {
            int val = e.ordinal();
            if (val <= 47) {
               if (this.off == this.bytes.length) {
                  this.ensureCapacity(this.off + 1);
               }

               this.bytes[this.off++] = (byte)val;
               return;
            }

            this.writeInt32(val);
         }
      }
   }

   @Override
   public void writeInt32(Integer i) {
      int minCapacity = this.off + 5;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      int size;
      if (i == null) {
         if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask)) == 0L) {
            bytes[off] = -81;
         } else {
            bytes[off] = 0;
         }

         size = 1;
      } else {
         int val = i;
         if (val >= -16 && val <= 47) {
            bytes[off] = (byte)val;
            size = 1;
         } else if (val >= -2048 && val <= 2047) {
            bytes[off] = (byte)(56 + (val >> 8));
            bytes[off + 1] = (byte)val;
            size = 2;
         } else if (val >= -262144 && val <= 262143) {
            bytes[off] = (byte)(68 + (val >> 16));
            bytes[off + 1] = (byte)(val >> 8);
            bytes[off + 2] = (byte)val;
            size = 3;
         } else {
            bytes[off] = 72;
            JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Integer.reverseBytes(val));
            size = 5;
         }
      }

      this.off += size;
   }

   @Override
   public void writeInt32(int val) {
      int minCapacity = this.off + 5;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      int size;
      if (val >= -16 && val <= 47) {
         bytes[off] = (byte)val;
         size = 1;
      } else if (val >= -2048 && val <= 2047) {
         bytes[off] = (byte)(56 + (val >> 8));
         bytes[off + 1] = (byte)val;
         size = 2;
      } else if (val >= -262144 && val <= 262143) {
         bytes[off] = (byte)(68 + (val >> 16));
         bytes[off + 1] = (byte)(val >> 8);
         bytes[off + 2] = (byte)val;
         size = 3;
      } else {
         bytes[off] = 72;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Integer.reverseBytes(val));
         size = 5;
      }

      this.off += size;
   }

   @Override
   public void writeListInt32(List<Integer> values) {
      if (values == null) {
         this.writeArrayNull();
      } else {
         int size = values.size();
         int off = this.off;
         int minCapacity = off + size * 5 + 5;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (size <= 15) {
            bytes[off++] = (byte)(-108 + size);
         } else {
            bytes[off] = -92;
            off += writeInt32(bytes, off + 1, size) + 1;
         }

         for (int i = 0; i < size; i++) {
            Number item = values.get(i);
            if (item == null) {
               bytes[off++] = -81;
            } else {
               int val = item.intValue();
               if (val >= -16 && val <= 47) {
                  bytes[off++] = (byte)val;
               } else if (val >= -2048 && val <= 2047) {
                  bytes[off] = (byte)(56 + (val >> 8));
                  bytes[off + 1] = (byte)val;
                  off += 2;
               } else if (val >= -262144 && val <= 262143) {
                  bytes[off] = (byte)(68 + (val >> 16));
                  bytes[off + 1] = (byte)(val >> 8);
                  bytes[off + 2] = (byte)val;
                  off += 3;
               } else {
                  bytes[off] = 72;
                  JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Integer.reverseBytes(val));
                  off += 5;
               }
            }
         }

         this.off = off;
      }
   }

   public static int writeInt32(byte[] bytes, int off, int val) {
      if (val >= -16 && val <= 47) {
         bytes[off] = (byte)val;
         return 1;
      } else if (val >= -2048 && val <= 2047) {
         bytes[off] = (byte)(56 + (val >> 8));
         bytes[off + 1] = (byte)val;
         return 2;
      } else if (val >= -262144 && val <= 262143) {
         bytes[off] = (byte)(68 + (val >> 16));
         bytes[off + 1] = (byte)(val >> 8);
         bytes[off + 2] = (byte)val;
         return 3;
      } else {
         bytes[off] = 72;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, JDKUtils.BIG_ENDIAN ? val : Integer.reverseBytes(val));
         return 5;
      }
   }

   @Override
   public void writeArrayNull() {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = (byte)((this.context.features & WRITE_ARRAY_NULL_MASK) != 0L ? -108 : -81);
   }

   @Override
   public void writeRaw(String str) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void writeRaw(byte[] bytes) {
      int minCapacity = this.off + bytes.length;
      if (minCapacity - this.bytes.length > 0) {
         this.ensureCapacity(minCapacity);
      }

      System.arraycopy(bytes, 0, this.bytes, this.off, bytes.length);
      this.off += bytes.length;
   }

   @Override
   public void writeSymbol(int symbol) {
      int minCapacity = this.off + 3;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[this.off++] = 127;
      if (symbol >= -16 && symbol <= 47) {
         bytes[this.off++] = (byte)symbol;
      } else if (symbol >= -2048 && symbol <= 2047) {
         bytes[this.off] = (byte)(56 + (symbol >> 8));
         bytes[this.off + 1] = (byte)symbol;
         this.off += 2;
      } else {
         this.writeInt32(symbol);
      }
   }

   @Override
   public void writeNameRaw(byte[] name, long nameHash) {
      int off = this.off;
      int minCapacity = off + name.length + 2;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int symbol;
      if (this.symbolTable == null || (symbol = this.symbolTable.getOrdinalByHashCode(nameHash)) == -1) {
         if ((this.context.features & JSONWriter.Feature.WriteNameAsSymbol.mask) == 0L) {
            System.arraycopy(name, 0, bytes, off, name.length);
            this.off = off + name.length;
            return;
         }

         boolean symbolExists = false;
         if (this.symbols != null) {
            if ((symbol = this.symbols.putIfAbsent(nameHash, this.symbolIndex)) != this.symbolIndex) {
               symbolExists = true;
            } else {
               this.symbolIndex++;
            }
         } else {
            (this.symbols = new TLongIntHashMap()).put(nameHash, symbol = this.symbolIndex++);
         }

         if (!symbolExists) {
            bytes[off++] = 127;
            System.arraycopy(name, 0, bytes, off, name.length);
            this.off = off + name.length;
            if (symbol >= -16 && symbol <= 47) {
               bytes[this.off++] = (byte)symbol;
            } else {
               this.writeInt32(symbol);
            }

            return;
         }

         symbol = -symbol;
      }

      bytes[off++] = 127;
      int intValue = -symbol;
      if (intValue >= -16 && intValue <= 47) {
         bytes[off] = (byte)intValue;
         this.off = off + 1;
      } else {
         this.off = off;
         this.writeInt32(intValue);
      }
   }

   @Override
   public void writeLocalDate(LocalDate date) {
      if (date == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 5);
         byte[] bytes = this.bytes;
         bytes[off] = -87;
         int year = date.getYear();
         bytes[off + 1] = (byte)(year >>> 8);
         bytes[off + 2] = (byte)year;
         bytes[off + 3] = (byte)date.getMonthValue();
         bytes[off + 4] = (byte)date.getDayOfMonth();
         this.off = off + 5;
      }
   }

   @Override
   public void writeLocalTime(LocalTime time) {
      if (time == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 4);
         byte[] bytes = this.bytes;
         bytes[off] = -89;
         bytes[off + 1] = (byte)time.getHour();
         bytes[off + 2] = (byte)time.getMinute();
         bytes[off + 3] = (byte)time.getSecond();
         this.off = off + 4;
         int nano = time.getNano();
         this.writeInt32(nano);
      }
   }

   @Override
   public void writeLocalDateTime(LocalDateTime dateTime) {
      if (dateTime == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 8);
         byte[] bytes = this.bytes;
         bytes[off] = -88;
         int year = dateTime.getYear();
         bytes[off + 1] = (byte)(year >>> 8);
         bytes[off + 2] = (byte)year;
         bytes[off + 3] = (byte)dateTime.getMonthValue();
         bytes[off + 4] = (byte)dateTime.getDayOfMonth();
         bytes[off + 5] = (byte)dateTime.getHour();
         bytes[off + 6] = (byte)dateTime.getMinute();
         bytes[off + 7] = (byte)dateTime.getSecond();
         this.off = off + 8;
         int nano = dateTime.getNano();
         this.writeInt32(nano);
      }
   }

   @Override
   public void writeZonedDateTime(ZonedDateTime dateTime) {
      if (dateTime == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 8);
         byte[] bytes = this.bytes;
         bytes[off] = -86;
         int year = dateTime.getYear();
         bytes[off + 1] = (byte)(year >>> 8);
         bytes[off + 2] = (byte)year;
         bytes[off + 3] = (byte)dateTime.getMonthValue();
         bytes[off + 4] = (byte)dateTime.getDayOfMonth();
         bytes[off + 5] = (byte)dateTime.getHour();
         bytes[off + 6] = (byte)dateTime.getMinute();
         bytes[off + 7] = (byte)dateTime.getSecond();
         this.off = off + 8;
         int nano = dateTime.getNano();
         this.writeInt32(nano);
         ZoneId zoneId = dateTime.getZone();
         String zoneIdStr = zoneId.getId();
         if (zoneIdStr.equals("Asia/Shanghai")) {
            this.writeRaw(SHANGHAI_ZONE_ID_NAME_BYTES);
         } else {
            this.writeString(zoneIdStr);
         }
      }
   }

   @Override
   public void writeOffsetDateTime(OffsetDateTime dateTime) {
      if (dateTime == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 8);
         byte[] bytes = this.bytes;
         bytes[off] = -86;
         int year = dateTime.getYear();
         bytes[off + 1] = (byte)(year >>> 8);
         bytes[off + 2] = (byte)year;
         bytes[off + 3] = (byte)dateTime.getMonthValue();
         bytes[off + 4] = (byte)dateTime.getDayOfMonth();
         bytes[off + 5] = (byte)dateTime.getHour();
         bytes[off + 6] = (byte)dateTime.getMinute();
         bytes[off + 7] = (byte)dateTime.getSecond();
         this.off = off + 8;
         int nano = dateTime.getNano();
         this.writeInt32(nano);
         ZoneId zoneId = dateTime.getOffset();
         String zoneIdStr = zoneId.getId();
         if (zoneIdStr.equals("+08:00")) {
            this.writeRaw(OFFSET_8_ZONE_ID_NAME_BYTES);
         } else {
            this.writeString(zoneIdStr);
         }
      }
   }

   @Override
   public void writeOffsetTime(OffsetTime offsetTime) {
      if (offsetTime == null) {
         this.writeNull();
      } else {
         this.writeOffsetDateTime(OffsetDateTime.of(DateUtils.LOCAL_DATE_19700101, offsetTime.toLocalTime(), offsetTime.getOffset()));
      }
   }

   @Override
   public void writeInstant(Instant instant) {
      if (instant == null) {
         this.writeNull();
      } else {
         this.ensureCapacity(this.off + 1);
         this.bytes[this.off++] = -82;
         long second = instant.getEpochSecond();
         int nano = instant.getNano();
         this.writeInt64(second);
         this.writeInt32(nano);
      }
   }

   @Override
   public void writeUUID(UUID value) {
      if (value == null) {
         this.writeNull();
      } else {
         int off = this.off;
         this.ensureCapacity(off + 18);
         byte[] bytes = this.bytes;
         bytes[off] = -111;
         bytes[off + 1] = 16;
         long msb = value.getMostSignificantBits();
         JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 2L, JDKUtils.BIG_ENDIAN ? msb : Long.reverseBytes(msb));
         long lsb = value.getLeastSignificantBits();
         JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 10L, JDKUtils.BIG_ENDIAN ? lsb : Long.reverseBytes(lsb));
         this.off = off + 18;
      }
   }

   @Override
   public void writeBigInt(BigInteger value, long features) {
      if (value == null) {
         this.writeNull();
      } else if (TypeUtils.isInt64(value)) {
         if (this.off == this.bytes.length) {
            this.ensureCapacity(this.off + 1);
         }

         this.bytes[this.off++] = -70;
         long int64Value = value.longValue();
         this.writeInt64(int64Value);
      } else {
         byte[] valueBytes = value.toByteArray();
         this.ensureCapacity(this.off + 5 + valueBytes.length);
         this.bytes[this.off++] = -69;
         this.writeInt32(valueBytes.length);
         System.arraycopy(valueBytes, 0, this.bytes, this.off, valueBytes.length);
         this.off += valueBytes.length;
      }
   }

   @Override
   public void writeBinary(byte[] bytes) {
      if (bytes == null) {
         this.writeNull();
      } else {
         this.ensureCapacity(this.off + 6 + bytes.length);
         this.bytes[this.off++] = -111;
         this.writeInt32(bytes.length);
         System.arraycopy(bytes, 0, this.bytes, this.off, bytes.length);
         this.off += bytes.length;
      }
   }

   @Override
   public void writeDecimal(BigDecimal value, long features, DecimalFormat format) {
      if (value == null) {
         this.writeNull();
      } else {
         int precision = value.precision();
         int scale = value.scale();
         if (precision < 19 && JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET != -1L) {
            long intCompact = JDKUtils.UNSAFE.getLong(value, JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET);
            if (scale == 0) {
               this.ensureCapacity(this.off + 1);
               this.bytes[this.off++] = -72;
               this.writeInt64(intCompact);
            } else {
               this.ensureCapacity(this.off + 1);
               this.bytes[this.off++] = -71;
               this.writeInt32(scale);
               if (intCompact >= -2147483648L && intCompact <= 2147483647L) {
                  this.writeInt32((int)intCompact);
               } else {
                  this.writeInt64(intCompact);
               }
            }
         } else {
            BigInteger unscaledValue = value.unscaledValue();
            if (scale == 0 && TypeUtils.isInt64(unscaledValue)) {
               this.ensureCapacity(this.off + 1);
               this.bytes[this.off++] = -72;
               long longValue = unscaledValue.longValue();
               this.writeInt64(longValue);
            } else {
               this.ensureCapacity(this.off + 1);
               this.bytes[this.off++] = -71;
               this.writeInt32(scale);
               if (TypeUtils.isInt32(unscaledValue)) {
                  int intValue = unscaledValue.intValue();
                  this.writeInt32(intValue);
               } else if (TypeUtils.isInt64(unscaledValue)) {
                  long longValue = unscaledValue.longValue();
                  this.writeInt64(longValue);
               } else {
                  this.writeBigInt(unscaledValue, 0L);
               }
            }
         }
      }
   }

   @Override
   public void writeBool(boolean value) {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = (byte)(value ? -79 : -80);
      this.off = off + 1;
   }

   @Override
   public void writeBool(boolean[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         this.startArray(values.length);

         for (int i = 0; i < values.length; i++) {
            this.writeBool(values[i]);
         }

         this.endArray();
      }
   }

   @Override
   public void writeReference(String path) {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = -109;
      this.off = off + 1;
      if (path == this.lastReference) {
         this.writeString("#-1");
      } else {
         this.writeString(path);
      }

      this.lastReference = path;
   }

   @Override
   public void writeDateTime14(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      int off = this.off;
      this.ensureCapacity(off + 8);
      byte[] bytes = this.bytes;
      bytes[off] = -88;
      bytes[off + 1] = (byte)(year >>> 8);
      bytes[off + 2] = (byte)year;
      bytes[off + 3] = (byte)month;
      bytes[off + 4] = (byte)dayOfMonth;
      bytes[off + 5] = (byte)hour;
      bytes[off + 6] = (byte)minute;
      bytes[off + 7] = (byte)second;
      this.off = off + 8;
      int nano = 0;
      this.writeInt32(nano);
   }

   @Override
   public void writeDateTime19(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      int off = this.off;
      this.ensureCapacity(off + 8);
      byte[] bytes = this.bytes;
      bytes[off] = -88;
      bytes[off + 1] = (byte)(year >>> 8);
      bytes[off + 2] = (byte)year;
      bytes[off + 3] = (byte)month;
      bytes[off + 4] = (byte)dayOfMonth;
      bytes[off + 5] = (byte)hour;
      bytes[off + 6] = (byte)minute;
      bytes[off + 7] = (byte)second;
      this.off = off + 8;
      int nano = 0;
      this.writeInt32(nano);
   }

   @Override
   public void writeDateTimeISO8601(int year, int month, int dayOfMonth, int hour, int minute, int second, int millis, int offsetSeconds, boolean timeZone) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void writeDateYYYMMDD8(int year, int month, int dayOfMonth) {
      int off = this.off;
      this.ensureCapacity(off + 5);
      byte[] bytes = this.bytes;
      bytes[off] = -87;
      bytes[off + 1] = (byte)(year >>> 8);
      bytes[off + 2] = (byte)year;
      bytes[off + 3] = (byte)month;
      bytes[off + 4] = (byte)dayOfMonth;
      this.off = off + 5;
   }

   @Override
   public void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
      this.writeDateYYYMMDD8(year, month, dayOfMonth);
   }

   @Override
   public void writeTimeHHMMSS8(int hour, int minute, int second) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void writeBase64(byte[] bytes) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public void writeHex(byte[] bytes) {
      this.writeBinary(bytes);
   }

   @Override
   public void writeRaw(char ch) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public void writeNameRaw(byte[] bytes) {
      this.writeRaw(bytes);
   }

   @Override
   public void writeName2Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 2;
   }

   @Override
   public void writeName3Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 3;
   }

   @Override
   public void writeName4Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 4;
   }

   @Override
   public void writeName5Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 5;
   }

   @Override
   public void writeName6Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 6;
   }

   @Override
   public void writeName7Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 7;
   }

   @Override
   public void writeName8Raw(long name) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 8;
   }

   @Override
   public void writeName9Raw(long name0, int name1) {
      int off = this.off;
      int minCapacity = off + 12;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 9;
   }

   @Override
   public void writeName10Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 10;
   }

   @Override
   public void writeName11Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 11;
   }

   @Override
   public void writeName12Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 12;
   }

   @Override
   public void writeName13Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 13;
   }

   @Override
   public void writeName14Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 14;
   }

   @Override
   public void writeName15Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 15;
   }

   @Override
   public void writeName16Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 16;
   }

   @Override
   public void writeNameRaw(char[] chars) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public void writeNameRaw(char[] bytes, int offset, int len) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public void writeColon() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public void write(List array) {
      if (array == null) {
         this.writeArrayNull();
      } else {
         int size = array.size();
         this.startArray(size);

         for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            this.writeAny(item);
         }
      }
   }

   @Override
   public void write(Map map) {
      if (map == null) {
         this.writeNull();
      } else {
         this.startObject();

         for (Entry entry : map.entrySet()) {
            this.writeAny(entry.getKey());
            this.writeAny(entry.getValue());
         }

         this.endObject();
      }
   }

   @Override
   public void write(JSONObject object) {
      if (object == null) {
         this.writeNull();
      } else {
         this.startObject();

         for (Entry entry : object.entrySet()) {
            this.writeAny(entry.getKey());
            this.writeAny(entry.getValue());
         }

         this.endObject();
      }
   }

   @Override
   public byte[] getBytes() {
      return Arrays.copyOf(this.bytes, this.off);
   }

   @Override
   public int size() {
      return this.off;
   }

   @Override
   public byte[] getBytes(Charset charset) {
      throw new JSONException("not support operator");
   }

   @Override
   public int flushTo(OutputStream to) throws IOException {
      int len = this.off;
      to.write(this.bytes, 0, this.off);
      this.off = 0;
      return len;
   }

   @Override
   public int flushTo(OutputStream out, Charset charset) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public String toString() {
      if (this.bytes.length == 0) {
         return "<empty>";
      } else {
         byte[] jsonbBytes = this.getBytes();
         JSONReader reader = JSONReader.ofJSONB(jsonbBytes);
         JSONWriter writer = JSONWriter.of();

         try {
            Object object = reader.readAny();
            writer.writeAny(object);
            return writer.toString();
         } catch (Exception var5) {
            return JSONB.typeName(this.bytes[0]) + ", bytes length " + this.off;
         }
      }
   }

   @Override
   public void println() {
   }
}
