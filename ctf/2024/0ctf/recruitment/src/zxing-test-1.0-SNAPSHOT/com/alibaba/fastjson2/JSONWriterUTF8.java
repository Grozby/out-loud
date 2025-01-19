package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.DoubleToDecimal;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

class JSONWriterUTF8 extends JSONWriter {
   static final byte[] REF_PREF = "{\"$ref\":".getBytes(StandardCharsets.ISO_8859_1);
   static final short[] HEX256;
   final JSONFactory.CacheItem cacheItem;
   protected byte[] bytes;

   JSONWriterUTF8(JSONWriter.Context ctx) {
      super(ctx, null, false, StandardCharsets.UTF_8);
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      this.cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
      if (bytes == null) {
         bytes = new byte[8192];
      }

      this.bytes = bytes;
   }

   @Override
   public final void writeNull() {
      int minCapacity = this.off + 4;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.off, IOUtils.NULL_32);
      this.off += 4;
   }

   @Override
   public final void writeReference(String path) {
      this.lastReference = path;
      this.writeRaw(REF_PREF);
      this.writeString(path);
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = 125;
      this.off = off + 1;
   }

   @Override
   public final void writeBase64(byte[] value) {
      int charsLen = (value.length - 1) / 3 + 1 << 2;
      int off = this.off;
      this.ensureCapacity(off + charsLen + 2);
      byte[] bytes = this.bytes;
      bytes[off++] = (byte)this.quote;
      int eLen = value.length / 3 * 3;

      for (int s = 0; s < eLen; off += 4) {
         int i = (value[s++] & 255) << 16 | (value[s++] & 255) << 8 | value[s++] & 255;
         bytes[off] = (byte)JSONFactory.CA[i >>> 18 & 63];
         bytes[off + 1] = (byte)JSONFactory.CA[i >>> 12 & 63];
         bytes[off + 2] = (byte)JSONFactory.CA[i >>> 6 & 63];
         bytes[off + 3] = (byte)JSONFactory.CA[i & 63];
      }

      int left = value.length - eLen;
      if (left > 0) {
         int i = (value[eLen] & 255) << 10 | (left == 2 ? (value[value.length - 1] & 255) << 2 : 0);
         bytes[off] = (byte)JSONFactory.CA[i >> 12];
         bytes[off + 1] = (byte)JSONFactory.CA[i >>> 6 & 63];
         bytes[off + 2] = left == 2 ? (byte)JSONFactory.CA[i & 63] : 61;
         bytes[off + 3] = 61;
         off += 4;
      }

      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeHex(byte[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         int charsLen = values.length * 2 + 3;
         int off = this.off;
         this.ensureCapacity(off + charsLen + 2);
         byte[] bytes = this.bytes;
         bytes[off] = 120;
         bytes[off + 1] = 39;
         off += 2;

         for (int i = 0; i < values.length; i++) {
            byte b = values[i];
            int a = b & 255;
            int b0 = a >> 4;
            int b1 = a & 15;
            bytes[off] = (byte)(b0 + (b0 < 10 ? 48 : 55));
            bytes[off + 1] = (byte)(b1 + (b1 < 10 ? 48 : 55));
            off += 2;
         }

         bytes[off] = 39;
         this.off = off + 1;
      }
   }

   @Override
   public final void close() {
      byte[] bytes = this.bytes;
      if (bytes.length <= 4194304) {
         JSONFactory.BYTES_UPDATER.lazySet(this.cacheItem, bytes);
      }
   }

   @Override
   public final int size() {
      return this.off;
   }

   @Override
   public final byte[] getBytes() {
      return Arrays.copyOf(this.bytes, this.off);
   }

   @Override
   public final byte[] getBytes(Charset charset) {
      if (charset == StandardCharsets.UTF_8) {
         return Arrays.copyOf(this.bytes, this.off);
      } else {
         String str = this.toString();
         return str.getBytes(charset);
      }
   }

   @Override
   public final int flushTo(OutputStream to) throws IOException {
      int off = this.off;
      if (off > 0) {
         to.write(this.bytes, 0, off);
         this.off = 0;
      }

      return off;
   }

   @Override
   protected final void write0(char c) {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = (byte)c;
      this.off = off + 1;
   }

   @Override
   public final void writeColon() {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = 58;
      this.off = off + 1;
   }

   @Override
   public final void startObject() {
      if (this.level >= this.context.maxLevel) {
         throw new JSONException("level too large : " + this.level);
      } else {
         this.level++;
         this.startObject = true;
         int off = this.off;
         int minCapacity = off + (this.pretty ? 3 + this.indent : 1);
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 123;
         if (this.pretty) {
            this.indent++;
            bytes[off++] = 10;

            for (int i = 0; i < this.indent; i++) {
               bytes[off++] = 9;
            }
         }

         this.off = off;
      }
   }

   @Override
   public final void endObject() {
      this.level--;
      int off = this.off;
      int minCapacity = off + (this.pretty ? 2 + this.indent : 1);
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.pretty) {
         this.indent--;
         bytes[off++] = 10;

         for (int i = 0; i < this.indent; i++) {
            bytes[off++] = 9;
         }
      }

      bytes[off] = 125;
      this.off = off + 1;
      this.startObject = false;
   }

   @Override
   public final void writeComma() {
      this.startObject = false;
      int off = this.off;
      int minCapacity = off + (this.pretty ? 2 + this.indent : 1);
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = 44;
      if (this.pretty) {
         bytes[off++] = 10;

         for (int i = 0; i < this.indent; i++) {
            bytes[off++] = 9;
         }
      }

      this.off = off;
   }

   @Override
   public final void startArray() {
      if (this.level >= this.context.maxLevel) {
         throw new JSONException("level too large : " + this.level);
      } else {
         this.level++;
         int off = this.off;
         int minCapacity = off + (this.pretty ? 3 + this.indent : 1);
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;
         if (this.pretty) {
            this.indent++;
            bytes[off++] = 10;

            for (int i = 0; i < this.indent; i++) {
               bytes[off++] = 9;
            }
         }

         this.off = off;
      }
   }

   @Override
   public final void endArray() {
      this.level--;
      int off = this.off;
      int minCapacity = off + (this.pretty ? 2 + this.indent : 1);
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.pretty) {
         this.indent--;
         bytes[off++] = 10;

         for (int i = 0; i < this.indent; i++) {
            bytes[off++] = 9;
         }
      }

      bytes[off] = 93;
      this.off = off + 1;
      this.startObject = false;
   }

   @Override
   public final void writeString(List<String> list) {
      int off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = 91;
      this.off = off + 1;
      int i = 0;

      for (int size = list.size(); i < size; i++) {
         if (i != 0) {
            off = this.off;
            if (off == this.bytes.length) {
               this.ensureCapacity(off + 1);
            }

            this.bytes[off] = 44;
            this.off = off + 1;
         }

         String str = list.get(i);
         this.writeString(str);
      }

      off = this.off;
      if (off == this.bytes.length) {
         this.ensureCapacity(off + 1);
      }

      this.bytes[off] = 93;
      this.off = off + 1;
   }

   @Override
   public final void writeString(boolean value) {
      byte quote = (byte)this.quote;
      this.bytes[this.off++] = quote;
      this.writeBool(value);
      this.bytes[this.off++] = quote;
   }

   @Override
   public final void writeString(byte value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) == 0L;
      if (writeAsString) {
         this.writeQuote();
      }

      this.writeInt8(value);
      if (writeAsString) {
         this.writeQuote();
      }
   }

   @Override
   public final void writeString(short value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) == 0L;
      if (writeAsString) {
         this.writeQuote();
      }

      this.writeInt16(value);
      if (writeAsString) {
         this.writeQuote();
      }
   }

   @Override
   public final void writeString(int value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) == 0L;
      if (writeAsString) {
         this.writeQuote();
      }

      this.writeInt32(value);
      if (writeAsString) {
         this.writeQuote();
      }
   }

   @Override
   public final void writeString(long value) {
      boolean writeAsString = (this.context.features & (JSONWriter.Feature.WriteNonStringValueAsString.mask | JSONWriter.Feature.WriteLongAsString.mask)) == 0L;
      if (writeAsString) {
         this.writeQuote();
      }

      this.writeInt64(value);
      if (writeAsString) {
         this.writeQuote();
      }
   }

   private void writeQuote() {
      if (this.off == this.bytes.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.bytes[this.off++] = (byte)this.quote;
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeStringNull();
      } else {
         char[] chars = JDKUtils.getCharArray(str);
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         int off = this.off;
         int minCapacity = off + chars.length * 3 + 2;
         if (escapeNoneAscii || browserSecure) {
            minCapacity += chars.length * 3;
         }

         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = (byte)this.quote;

         int i;
         for (i = 0; i < chars.length; i++) {
            char c0 = chars[i];
            if (c0 == this.quote || c0 == '\\' || c0 < ' ' || c0 > 127 || browserSecure && (c0 == '<' || c0 == '>' || c0 == '(' || c0 == ')')) {
               break;
            }

            bytes[off++] = (byte)c0;
         }

         if (i == chars.length) {
            bytes[off] = (byte)this.quote;
            this.off = off + 1;
         } else {
            this.off = off;
            if (i < chars.length) {
               this.writeStringEscapedRest(chars, chars.length, browserSecure, escapeNoneAscii, i);
            }

            this.bytes[this.off++] = (byte)this.quote;
         }
      }
   }

   @Override
   public void writeStringLatin1(byte[] values) {
      boolean escape = false;
      if ((this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L) {
         this.writeStringLatin1BrowserSecure(values);
      } else {
         byte quote = (byte)this.quote;

         for (int i = 0; i < values.length; i++) {
            byte c = values[i];
            if (c == quote || c == 92 || c < 32) {
               escape = true;
               break;
            }
         }

         int off = this.off;
         if (!escape) {
            int minCapacity = off + values.length + 2;
            if (minCapacity >= this.bytes.length) {
               this.ensureCapacity(minCapacity);
            }

            byte[] bytes = this.bytes;
            bytes[off] = quote;
            System.arraycopy(values, 0, bytes, off + 1, values.length);
            off += values.length + 1;
            bytes[off] = quote;
            this.off = off + 1;
         } else {
            this.writeStringEscaped(values);
         }
      }
   }

   protected final void writeStringLatin1BrowserSecure(byte[] values) {
      boolean escape = false;
      byte quote = (byte)this.quote;

      for (int i = 0; i < values.length; i++) {
         byte c = values[i];
         if (c == quote || c == 92 || c < 32 || c == 60 || c == 62 || c == 40 || c == 41) {
            escape = true;
            break;
         }
      }

      int off = this.off;
      if (!escape) {
         int minCapacity = off + values.length + 2;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off] = quote;
         System.arraycopy(values, 0, bytes, off + 1, values.length);
         off += values.length + 1;
         bytes[off] = quote;
         this.off = off + 1;
      } else {
         this.writeStringEscaped(values);
      }
   }

   @Override
   public final void writeStringUTF16(byte[] value) {
      if (value == null) {
         this.writeStringNull();
      } else {
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         int off = this.off;
         int minCapacity = off + value.length * 4 + 2;
         if (escapeNoneAscii) {
            minCapacity += value.length * 2;
         }

         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = (byte)this.quote;
         int valueOffset = 0;

         while (valueOffset < value.length) {
            byte b0 = value[valueOffset];
            byte b1 = value[valueOffset + 1];
            valueOffset += 2;
            if (b1 == 0 && b0 >= 0) {
               switch (b0) {
                  case 0:
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                  case 5:
                  case 6:
                  case 7:
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 48;
                     bytes[off + 5] = (byte)(48 + b0);
                     off += 6;
                     break;
                  case 8:
                     bytes[off] = 92;
                     bytes[off + 1] = 98;
                     off += 2;
                     break;
                  case 9:
                     bytes[off] = 92;
                     bytes[off + 1] = 116;
                     off += 2;
                     break;
                  case 10:
                     bytes[off] = 92;
                     bytes[off + 1] = 110;
                     off += 2;
                     break;
                  case 11:
                  case 14:
                  case 15:
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 48;
                     bytes[off + 5] = (byte)(97 + (b0 - 10));
                     off += 6;
                     break;
                  case 12:
                     bytes[off] = 92;
                     bytes[off + 1] = 102;
                     off += 2;
                     break;
                  case 13:
                     bytes[off] = 92;
                     bytes[off + 1] = 114;
                     off += 2;
                     break;
                  case 16:
                  case 17:
                  case 18:
                  case 19:
                  case 20:
                  case 21:
                  case 22:
                  case 23:
                  case 24:
                  case 25:
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 49;
                     bytes[off + 5] = (byte)(48 + (b0 - 16));
                     off += 6;
                     break;
                  case 26:
                  case 27:
                  case 28:
                  case 29:
                  case 30:
                  case 31:
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 49;
                     bytes[off + 5] = (byte)(97 + (b0 - 26));
                     off += 6;
                     break;
                  case 32:
                  case 33:
                  case 34:
                  case 35:
                  case 36:
                  case 37:
                  case 38:
                  case 39:
                  case 42:
                  case 43:
                  case 44:
                  case 45:
                  case 46:
                  case 47:
                  case 48:
                  case 49:
                  case 50:
                  case 51:
                  case 52:
                  case 53:
                  case 54:
                  case 55:
                  case 56:
                  case 57:
                  case 58:
                  case 59:
                  case 61:
                  case 63:
                  case 64:
                  case 65:
                  case 66:
                  case 67:
                  case 68:
                  case 69:
                  case 70:
                  case 71:
                  case 72:
                  case 73:
                  case 74:
                  case 75:
                  case 76:
                  case 77:
                  case 78:
                  case 79:
                  case 80:
                  case 81:
                  case 82:
                  case 83:
                  case 84:
                  case 85:
                  case 86:
                  case 87:
                  case 88:
                  case 89:
                  case 90:
                  case 91:
                  default:
                     if (b0 == this.quote) {
                        bytes[off] = 92;
                        bytes[off + 1] = (byte)this.quote;
                        off += 2;
                     } else {
                        bytes[off++] = b0;
                     }
                     break;
                  case 40:
                  case 41:
                  case 60:
                  case 62:
                     if (browserSecure) {
                        bytes[off] = 92;
                        bytes[off + 1] = 117;
                        bytes[off + 2] = 48;
                        bytes[off + 3] = 48;
                        bytes[off + 4] = (byte)DIGITS[b0 >>> 4 & 15];
                        bytes[off + 5] = (byte)DIGITS[b0 & 15];
                        off += 6;
                     } else {
                        bytes[off++] = b0;
                     }
                     break;
                  case 92:
                     bytes[off] = 92;
                     bytes[off + 1] = 92;
                     off += 2;
               }
            } else {
               char c = (char)(b0 & 255 | (b1 & 255) << 8);
               if (c < 2048) {
                  bytes[off] = (byte)(192 | c >> 6);
                  bytes[off + 1] = (byte)(128 | c & '?');
                  off += 2;
               } else if (escapeNoneAscii) {
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = (byte)DIGITS[c >>> '\f' & 15];
                  bytes[off + 3] = (byte)DIGITS[c >>> '\b' & 15];
                  bytes[off + 4] = (byte)DIGITS[c >>> 4 & 15];
                  bytes[off + 5] = (byte)DIGITS[c & 15];
                  off += 6;
               } else if (c >= '\ud800' && c < '\ue000') {
                  int ip = valueOffset - 1;
                  if (c >= '\udc00') {
                     bytes[off++] = 63;
                  } else {
                     int uc;
                     if (value.length - ip < 2) {
                        uc = -1;
                     } else {
                        b0 = value[ip + 1];
                        b1 = value[ip + 2];
                        char d = (char)(b0 & 255 | (b1 & 255) << 8);
                        if (d < '\udc00' || d >= '\ue000') {
                           bytes[off++] = 63;
                           continue;
                        }

                        valueOffset += 2;
                        uc = (c << '\n') + d + -56613888;
                     }

                     if (uc < 0) {
                        bytes[off++] = 63;
                     } else {
                        bytes[off] = (byte)(240 | uc >> 18);
                        bytes[off + 1] = (byte)(128 | uc >> 12 & 63);
                        bytes[off + 2] = (byte)(128 | uc >> 6 & 63);
                        bytes[off + 3] = (byte)(128 | uc & 63);
                        off += 4;
                     }
                  }
               } else {
                  bytes[off] = (byte)(224 | c >> '\f');
                  bytes[off + 1] = (byte)(128 | c >> 6 & 63);
                  bytes[off + 2] = (byte)(128 | c & '?');
                  off += 3;
               }
            }
         }

         bytes[off] = (byte)this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeString(char[] chars) {
      if (chars == null) {
         this.writeStringNull();
      } else {
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         int off = this.off;
         int minCapacity = off + chars.length * 3 + 2;
         if (escapeNoneAscii || browserSecure) {
            minCapacity += chars.length * 3;
         }

         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = (byte)this.quote;

         int i;
         for (i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == this.quote || c == '\\' || c < ' ' || c > 127 || browserSecure && (c == '<' || c == '>' || c == '(' || c == ')')) {
               break;
            }

            bytes[off++] = (byte)c;
         }

         this.off = off;
         int rest = chars.length - i;
         minCapacity = off + rest * 6 + 2;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         if (i < chars.length) {
            this.writeStringEscapedRest(chars, chars.length, browserSecure, escapeNoneAscii, i);
         }

         this.bytes[this.off++] = (byte)this.quote;
      }
   }

   @Override
   public final void writeString(char[] chars, int stroff, int strlen) {
      if (chars == null) {
         if (this.isEnabled(JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) {
            this.writeString("");
         } else {
            this.writeNull();
         }
      } else {
         int end = stroff + strlen;
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         int off = this.off;
         int minCapacity = off + strlen * 3 + 2;
         if (escapeNoneAscii || browserSecure) {
            minCapacity += strlen * 3;
         }

         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = (byte)this.quote;

         int i;
         for (i = stroff; i < end; i++) {
            char c0 = chars[i];
            if (c0 == this.quote || c0 == '\\' || c0 < ' ' || c0 > 127 || browserSecure && (c0 == '<' || c0 == '>' || c0 == '(' || c0 == ')')) {
               break;
            }

            bytes[off++] = (byte)c0;
         }

         this.off = off;
         int rest = end - i;
         minCapacity = off + rest * 6 + 2;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         if (i < end) {
            this.writeStringEscapedRest(chars, end, browserSecure, escapeNoneAscii, i);
         }

         this.bytes[this.off++] = (byte)this.quote;
      }
   }

   protected final void writeStringEscaped(byte[] values) {
      int minCapacity = this.off + values.length * 4 + 2;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
      byte[] bytes = this.bytes;
      int off = this.off;
      bytes[off++] = (byte)this.quote;

      for (int i = 0; i < values.length; i++) {
         byte ch = values[i];
         switch (ch) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 48;
               bytes[off + 5] = (byte)(48 + ch);
               off += 6;
               break;
            case 8:
               bytes[off] = 92;
               bytes[off + 1] = 98;
               off += 2;
               break;
            case 9:
               bytes[off] = 92;
               bytes[off + 1] = 116;
               off += 2;
               break;
            case 10:
               bytes[off] = 92;
               bytes[off + 1] = 110;
               off += 2;
               break;
            case 11:
            case 14:
            case 15:
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 48;
               bytes[off + 5] = (byte)(97 + (ch - 10));
               off += 6;
               break;
            case 12:
               bytes[off] = 92;
               bytes[off + 1] = 102;
               off += 2;
               break;
            case 13:
               bytes[off] = 92;
               bytes[off + 1] = 114;
               off += 2;
               break;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 49;
               bytes[off + 5] = (byte)(48 + (ch - 16));
               off += 6;
               break;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 49;
               bytes[off + 5] = (byte)(97 + (ch - 26));
               off += 6;
               break;
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 61:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            default:
               if (ch == this.quote) {
                  bytes[off] = 92;
                  bytes[off + 1] = (byte)this.quote;
                  off += 2;
               } else if (ch < 0) {
                  int c = ch & 255;
                  bytes[off] = (byte)(192 | c >> 6);
                  bytes[off + 1] = (byte)(128 | c & 63);
                  off += 2;
               } else {
                  bytes[off++] = ch;
               }
               break;
            case 40:
            case 41:
            case 60:
            case 62:
               if (browserSecure) {
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = 48;
                  bytes[off + 3] = 48;
                  bytes[off + 4] = (byte)DIGITS[ch >>> 4 & 15];
                  bytes[off + 5] = (byte)DIGITS[ch & 15];
                  off += 6;
               } else {
                  bytes[off++] = ch;
               }
               break;
            case 92:
               bytes[off] = 92;
               bytes[off + 1] = 92;
               off += 2;
         }
      }

      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   protected final void writeStringEscapedRest(char[] chars, int end, boolean browserSecure, boolean escapeNoneAscii, int i) {
      int rest = chars.length - i;
      int minCapacity = this.off + rest * 6 + 2;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;

      int off;
      for (off = this.off; i < end; i++) {
         char ch = chars[i];
         if (ch <= 127) {
            switch (ch) {
               case '\u0000':
               case '\u0001':
               case '\u0002':
               case '\u0003':
               case '\u0004':
               case '\u0005':
               case '\u0006':
               case '\u0007':
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = 48;
                  bytes[off + 3] = 48;
                  bytes[off + 4] = 48;
                  bytes[off + 5] = (byte)('0' + ch);
                  off += 6;
                  break;
               case '\b':
                  bytes[off] = 92;
                  bytes[off + 1] = 98;
                  off += 2;
                  break;
               case '\t':
                  bytes[off] = 92;
                  bytes[off + 1] = 116;
                  off += 2;
                  break;
               case '\n':
                  bytes[off] = 92;
                  bytes[off + 1] = 110;
                  off += 2;
                  break;
               case '\u000b':
               case '\u000e':
               case '\u000f':
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = 48;
                  bytes[off + 3] = 48;
                  bytes[off + 4] = 48;
                  bytes[off + 5] = (byte)(97 + (ch - '\n'));
                  off += 6;
                  break;
               case '\f':
                  bytes[off] = 92;
                  bytes[off + 1] = 102;
                  off += 2;
                  break;
               case '\r':
                  bytes[off] = 92;
                  bytes[off + 1] = 114;
                  off += 2;
                  break;
               case '\u0010':
               case '\u0011':
               case '\u0012':
               case '\u0013':
               case '\u0014':
               case '\u0015':
               case '\u0016':
               case '\u0017':
               case '\u0018':
               case '\u0019':
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = 48;
                  bytes[off + 3] = 48;
                  bytes[off + 4] = 49;
                  bytes[off + 5] = (byte)(48 + (ch - 16));
                  off += 6;
                  break;
               case '\u001a':
               case '\u001b':
               case '\u001c':
               case '\u001d':
               case '\u001e':
               case '\u001f':
                  bytes[off] = 92;
                  bytes[off + 1] = 117;
                  bytes[off + 2] = 48;
                  bytes[off + 3] = 48;
                  bytes[off + 4] = 49;
                  bytes[off + 5] = (byte)(97 + (ch - 26));
                  off += 6;
                  break;
               case ' ':
               case '!':
               case '"':
               case '#':
               case '$':
               case '%':
               case '&':
               case '\'':
               case '*':
               case '+':
               case ',':
               case '-':
               case '.':
               case '/':
               case '0':
               case '1':
               case '2':
               case '3':
               case '4':
               case '5':
               case '6':
               case '7':
               case '8':
               case '9':
               case ':':
               case ';':
               case '=':
               case '?':
               case '@':
               case 'A':
               case 'B':
               case 'C':
               case 'D':
               case 'E':
               case 'F':
               case 'G':
               case 'H':
               case 'I':
               case 'J':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'S':
               case 'T':
               case 'U':
               case 'V':
               case 'W':
               case 'X':
               case 'Y':
               case 'Z':
               case '[':
               default:
                  if (ch == this.quote) {
                     bytes[off] = 92;
                     bytes[off + 1] = (byte)this.quote;
                     off += 2;
                  } else {
                     bytes[off++] = (byte)ch;
                  }
                  break;
               case '(':
               case ')':
               case '<':
               case '>':
                  if (browserSecure) {
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = (byte)DIGITS[ch >>> 4 & 15];
                     bytes[off + 5] = (byte)DIGITS[ch & 15];
                     off += 6;
                  } else {
                     bytes[off++] = (byte)ch;
                  }
                  break;
               case '\\':
                  bytes[off] = 92;
                  bytes[off + 1] = 92;
                  off += 2;
            }
         } else if (escapeNoneAscii) {
            bytes[off] = 92;
            bytes[off + 1] = 117;
            bytes[off + 2] = (byte)DIGITS[ch >>> '\f' & 15];
            bytes[off + 3] = (byte)DIGITS[ch >>> '\b' & 15];
            bytes[off + 4] = (byte)DIGITS[ch >>> 4 & 15];
            bytes[off + 5] = (byte)DIGITS[ch & 15];
            off += 6;
         } else if (ch >= '\ud800' && ch < '\ue000') {
            if (ch < '\udc00') {
               int uc;
               if (chars.length - i < 2) {
                  uc = -1;
               } else {
                  char d = chars[i + 1];
                  if (d < '\udc00' || d >= '\ue000') {
                     bytes[off++] = 63;
                     continue;
                  }

                  uc = (ch << '\n') + d + -56613888;
               }

               if (uc < 0) {
                  bytes[off++] = 63;
               } else {
                  bytes[off] = (byte)(240 | uc >> 18);
                  bytes[off + 1] = (byte)(128 | uc >> 12 & 63);
                  bytes[off + 2] = (byte)(128 | uc >> 6 & 63);
                  bytes[off + 3] = (byte)(128 | uc & 63);
                  off += 4;
                  i++;
               }
            } else {
               bytes[off++] = 63;
            }
         } else if (ch > 2047) {
            bytes[off] = (byte)(224 | ch >> '\f' & 15);
            bytes[off + 1] = (byte)(128 | ch >> 6 & 63);
            bytes[off + 2] = (byte)(128 | ch & '?');
            off += 3;
         } else {
            bytes[off] = (byte)(192 | ch >> 6 & 31);
            bytes[off + 1] = (byte)(128 | ch & '?');
            off += 2;
         }
      }

      this.off = off;
   }

   @Override
   public final void writeString(char[] chars, int offset, int len, boolean quoted) {
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      int minCapacity = this.off + chars.length * 3 + 2;
      if (escapeNoneAscii) {
         minCapacity += len * 3;
      }

      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      if (quoted) {
         bytes[off++] = (byte)this.quote;
      }

      int end = offset + len;

      int i;
      for (i = offset; i < end; i++) {
         char c0 = chars[i];
         if (c0 == this.quote || c0 == '\\' || c0 < ' ' || c0 > 127) {
            break;
         }

         bytes[off++] = (byte)c0;
      }

      if (i == end) {
         if (quoted) {
            bytes[off++] = (byte)this.quote;
         }

         this.off = off;
      } else {
         for (; i < len; i++) {
            char ch = chars[i];
            if (ch <= 127) {
               switch (ch) {
                  case '\u0000':
                  case '\u0001':
                  case '\u0002':
                  case '\u0003':
                  case '\u0004':
                  case '\u0005':
                  case '\u0006':
                  case '\u0007':
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 48;
                     bytes[off + 5] = (byte)('0' + ch);
                     off += 6;
                     break;
                  case '\b':
                     bytes[off] = 92;
                     bytes[off + 1] = 98;
                     off += 2;
                     break;
                  case '\t':
                     bytes[off] = 92;
                     bytes[off + 1] = 116;
                     off += 2;
                     break;
                  case '\n':
                     bytes[off] = 92;
                     bytes[off + 1] = 110;
                     off += 2;
                     break;
                  case '\u000b':
                  case '\u000e':
                  case '\u000f':
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 48;
                     bytes[off + 5] = (byte)(97 + (ch - '\n'));
                     off += 6;
                     break;
                  case '\f':
                     bytes[off] = 92;
                     bytes[off + 1] = 102;
                     off += 2;
                     break;
                  case '\r':
                     bytes[off] = 92;
                     bytes[off + 1] = 114;
                     off += 2;
                     break;
                  case '\u0010':
                  case '\u0011':
                  case '\u0012':
                  case '\u0013':
                  case '\u0014':
                  case '\u0015':
                  case '\u0016':
                  case '\u0017':
                  case '\u0018':
                  case '\u0019':
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 49;
                     bytes[off + 5] = (byte)(48 + (ch - 16));
                     off += 6;
                     break;
                  case '\u001a':
                  case '\u001b':
                  case '\u001c':
                  case '\u001d':
                  case '\u001e':
                  case '\u001f':
                     bytes[off] = 92;
                     bytes[off + 1] = 117;
                     bytes[off + 2] = 48;
                     bytes[off + 3] = 48;
                     bytes[off + 4] = 49;
                     bytes[off + 5] = (byte)(97 + (ch - 26));
                     off += 6;
                     break;
                  case ' ':
                  case '!':
                  case '"':
                  case '#':
                  case '$':
                  case '%':
                  case '&':
                  case '\'':
                  case '(':
                  case ')':
                  case '*':
                  case '+':
                  case ',':
                  case '-':
                  case '.':
                  case '/':
                  case '0':
                  case '1':
                  case '2':
                  case '3':
                  case '4':
                  case '5':
                  case '6':
                  case '7':
                  case '8':
                  case '9':
                  case ':':
                  case ';':
                  case '<':
                  case '=':
                  case '>':
                  case '?':
                  case '@':
                  case 'A':
                  case 'B':
                  case 'C':
                  case 'D':
                  case 'E':
                  case 'F':
                  case 'G':
                  case 'H':
                  case 'I':
                  case 'J':
                  case 'K':
                  case 'L':
                  case 'M':
                  case 'N':
                  case 'O':
                  case 'P':
                  case 'Q':
                  case 'R':
                  case 'S':
                  case 'T':
                  case 'U':
                  case 'V':
                  case 'W':
                  case 'X':
                  case 'Y':
                  case 'Z':
                  case '[':
                  default:
                     if (ch == this.quote) {
                        bytes[off] = 92;
                        bytes[off + 1] = (byte)this.quote;
                        off += 2;
                     } else {
                        bytes[off++] = (byte)ch;
                     }
                     break;
                  case '\\':
                     bytes[off] = 92;
                     bytes[off + 1] = 92;
                     off += 2;
               }
            } else if (escapeNoneAscii) {
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = (byte)DIGITS[ch >>> '\f' & 15];
               bytes[off + 3] = (byte)DIGITS[ch >>> '\b' & 15];
               bytes[off + 4] = (byte)DIGITS[ch >>> 4 & 15];
               bytes[off + 5] = (byte)DIGITS[ch & 15];
               off += 6;
            } else if (ch >= '\ud800' && ch < '\ue000') {
               if (ch < '\udc00') {
                  int uc;
                  if (chars.length - i < 2) {
                     uc = -1;
                  } else {
                     char d = chars[i + 1];
                     if (d < '\udc00' || d >= '\ue000') {
                        bytes[off++] = 63;
                        continue;
                     }

                     uc = (ch << '\n') + d + -56613888;
                  }

                  if (uc < 0) {
                     bytes[off++] = 63;
                  } else {
                     bytes[off] = (byte)(240 | uc >> 18);
                     bytes[off + 1] = (byte)(128 | uc >> 12 & 63);
                     bytes[off + 2] = (byte)(128 | uc >> 6 & 63);
                     bytes[off + 3] = (byte)(128 | uc & 63);
                     off += 4;
                     i++;
                  }
               } else {
                  bytes[off++] = 63;
               }
            } else if (ch > 2047) {
               bytes[off] = (byte)(224 | ch >> '\f' & 15);
               bytes[off + 1] = (byte)(128 | ch >> 6 & 63);
               bytes[off + 2] = (byte)(128 | ch & '?');
               off += 3;
            } else {
               bytes[off] = (byte)(192 | ch >> 6 & 31);
               bytes[off + 1] = (byte)(128 | ch & '?');
               off += 2;
            }
         }

         if (quoted) {
            bytes[off++] = (byte)this.quote;
         }

         this.off = off;
      }
   }

   @Override
   public final void writeString(String[] strings) {
      if (strings == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < strings.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            String item = strings[i];
            if (item == null) {
               if (this.isEnabled(JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) {
                  this.writeString("");
               } else {
                  this.writeNull();
               }
            } else {
               this.writeString(item);
            }
         }

         this.endArray();
      }
   }

   @Override
   public final void writeChar(char ch) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = (byte)this.quote;
      if (ch <= 127) {
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 48;
               bytes[off + 5] = (byte)('0' + ch);
               off += 6;
               break;
            case '\b':
               bytes[off] = 92;
               bytes[off + 1] = 98;
               off += 2;
               break;
            case '\t':
               bytes[off] = 92;
               bytes[off + 1] = 116;
               off += 2;
               break;
            case '\n':
               bytes[off] = 92;
               bytes[off + 1] = 110;
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 48;
               bytes[off + 5] = (byte)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               bytes[off] = 92;
               bytes[off + 1] = 102;
               off += 2;
               break;
            case '\r':
               bytes[off] = 92;
               bytes[off + 1] = 114;
               off += 2;
               break;
            case '\u0010':
            case '\u0011':
            case '\u0012':
            case '\u0013':
            case '\u0014':
            case '\u0015':
            case '\u0016':
            case '\u0017':
            case '\u0018':
            case '\u0019':
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 49;
               bytes[off + 5] = (byte)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               bytes[off] = 92;
               bytes[off + 1] = 117;
               bytes[off + 2] = 48;
               bytes[off + 3] = 48;
               bytes[off + 4] = 49;
               bytes[off + 5] = (byte)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '"':
            case '#':
            case '$':
            case '%':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '-':
            case '.':
            case '/':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '[':
            default:
               if (ch == this.quote) {
                  bytes[off] = 92;
                  bytes[off + 1] = (byte)this.quote;
                  off += 2;
               } else {
                  bytes[off++] = (byte)ch;
               }
               break;
            case '\\':
               bytes[off] = 92;
               bytes[off + 1] = 92;
               off += 2;
         }
      } else {
         if (ch >= '\ud800' && ch < '\ue000') {
            throw new JSONException("illegal char " + ch);
         }

         if (ch > 2047) {
            bytes[off] = (byte)(224 | ch >> '\f' & 15);
            bytes[off + 1] = (byte)(128 | ch >> 6 & 63);
            bytes[off + 2] = (byte)(128 | ch & '?');
            off += 3;
         } else {
            bytes[off] = (byte)(192 | ch >> 6 & 31);
            bytes[off + 1] = (byte)(128 | ch & '?');
            off += 2;
         }
      }

      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   static int packDigits(int b0, int b1) {
      int v = HEX256[b0 & 0xFF] | HEX256[b1 & 0xFF] << 16;
      return JDKUtils.BIG_ENDIAN ? Integer.reverseBytes(v) : v;
   }

   static long packDigits(int b0, int b1, int b2, int b3) {
      short[] digits = HEX256;
      long v = (long)digits[b0 & 0xFF] | (long)digits[b1 & 0xFF] << 16 | (long)digits[b2 & 0xFF] << 32 | (long)digits[b3 & 0xFF] << 48;
      return JDKUtils.BIG_ENDIAN ? Long.reverseBytes(v) : v;
   }

   @Override
   public final void writeUUID(UUID value) {
      if (value == null) {
         this.writeNull();
      } else {
         long msb = value.getMostSignificantBits();
         long lsb = value.getLeastSignificantBits();
         int minCapacity = this.off + 38;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] buf = this.bytes;
         int off = this.off;
         buf[off] = 34;
         long base = JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off;
         JDKUtils.UNSAFE.putLong(buf, base + 1L, packDigits((int)(msb >> 56), (int)(msb >> 48), (int)(msb >> 40), (int)(msb >> 32)));
         buf[off + 9] = 45;
         JDKUtils.UNSAFE.putLong(buf, base + 10L, (long)packDigits((int)msb >> 24, (int)msb >> 16));
         buf[off + 14] = 45;
         JDKUtils.UNSAFE.putLong(buf, base + 15L, (long)packDigits((int)msb >> 8, (int)msb));
         buf[off + 19] = 45;
         JDKUtils.UNSAFE.putLong(buf, base + 20L, (long)packDigits((int)(lsb >> 56), (int)(lsb >> 48)));
         buf[off + 24] = 45;
         JDKUtils.UNSAFE.putLong(buf, base + 25L, packDigits((int)(lsb >> 40), (int)(lsb >> 32), (int)lsb >> 24, (int)lsb >> 16));
         JDKUtils.UNSAFE.putLong(buf, base + 33L, (long)packDigits((int)lsb >> 8, (int)lsb));
         buf[off + 37] = 34;
         this.off += 38;
      }
   }

   @Override
   public final void writeRaw(String str) {
      char[] chars = JDKUtils.getCharArray(str);
      int off = this.off;
      int minCapacity = off + chars.length * 3;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (c >= 1 && c <= 127) {
            bytes[off++] = (byte)c;
         } else if (c > 2047) {
            bytes[off] = (byte)(224 | c >> '\f' & 15);
            bytes[off + 1] = (byte)(128 | c >> 6 & 63);
            bytes[off + 2] = (byte)(128 | c & '?');
            off += 3;
         } else {
            bytes[off] = (byte)(192 | c >> 6 & 31);
            bytes[off + 1] = (byte)(128 | c & '?');
            off += 2;
         }
      }

      this.off = off;
   }

   @Override
   public final void writeRaw(byte[] bytes) {
      int minCapacity = this.off + bytes.length;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      System.arraycopy(bytes, 0, this.bytes, this.off, bytes.length);
      this.off += bytes.length;
   }

   @Override
   public final void writeNameRaw(byte[] name) {
      int off = this.off;
      int minCapacity = off + name.length + 2 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      if (this.startObject) {
         this.startObject = false;
      } else {
         byte[] bytes = this.bytes;
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      System.arraycopy(name, 0, this.bytes, off, name.length);
      this.off = off + name.length;
   }

   @Override
   public final void writeName2Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 5;
   }

   private static int indent(byte[] bytes, int off, int indent) {
      bytes[off++] = 10;
      int end = off + indent;

      while (off < end) {
         bytes[off++] = 9;
      }

      return off;
   }

   @Override
   public final void writeName3Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 6;
   }

   @Override
   public final void writeName4Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 7;
   }

   @Override
   public final void writeName5Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      this.off = off + 8;
   }

   @Override
   public final void writeName6Raw(long name) {
      int off = this.off;
      int minCapacity = off + 11 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      bytes[off + 8] = 58;
      this.off = off + 9;
   }

   @Override
   public final void writeName7Raw(long name) {
      int off = this.off;
      int minCapacity = off + 12 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name);
      bytes[off + 8] = (byte)this.quote;
      bytes[off + 9] = 58;
      this.off = off + 10;
   }

   @Override
   public final void writeName8Raw(long name) {
      int off = this.off;
      int minCapacity = off + 13 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      bytes[off] = (byte)this.quote;
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, name);
      bytes[off + 9] = (byte)this.quote;
      bytes[off + 10] = 58;
      this.off = off + 11;
   }

   @Override
   public final void writeName9Raw(long name0, int name1) {
      int off = this.off;
      int minCapacity = off + 14 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 12;
   }

   @Override
   public final void writeName10Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 13;
   }

   @Override
   public final void writeName11Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 14;
   }

   @Override
   public final void writeName12Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 15;
   }

   @Override
   public final void writeName13Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      this.off = off + 16;
   }

   @Override
   public final void writeName14Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 19 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      bytes[off + 16] = 58;
      this.off = off + 17;
   }

   @Override
   public final void writeName15Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 20 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      bytes[off + 16] = (byte)this.quote;
      bytes[off + 17] = 58;
      this.off = off + 18;
   }

   @Override
   public final void writeName16Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 21 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (this.startObject) {
         this.startObject = false;
      } else {
         bytes[off++] = 44;
         if (this.pretty) {
            off = indent(bytes, off, this.indent);
         }
      }

      bytes[off++] = (byte)this.quote;
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, name0);
      JDKUtils.UNSAFE.putLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 8L, name1);
      bytes[off + 16] = (byte)this.quote;
      bytes[off + 17] = 58;
      this.off = off + 18;
   }

   @Override
   public final void writeRaw(char ch) {
      if (ch > 128) {
         throw new JSONException("not support " + ch);
      } else {
         if (this.off == this.bytes.length) {
            this.ensureCapacity(this.off + 1);
         }

         this.bytes[this.off++] = (byte)ch;
      }
   }

   @Override
   public final void writeRaw(char c0, char c1) {
      if (c0 > 128) {
         throw new JSONException("not support " + c0);
      } else if (c1 > 128) {
         throw new JSONException("not support " + c1);
      } else {
         int off = this.off;
         if (off + 1 >= this.bytes.length) {
            this.ensureCapacity(off + 2);
         }

         this.bytes[off] = (byte)c0;
         this.bytes[off + 1] = (byte)c1;
         this.off = off + 2;
      }
   }

   @Override
   public final void writeNameRaw(byte[] bytes, int off, int len) {
      int minCapacity = this.off + len + 2 + this.indent;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      if (this.startObject) {
         this.startObject = false;
      } else {
         this.writeComma();
      }

      System.arraycopy(bytes, off, this.bytes, this.off, len);
      this.off += len;
   }

   final void ensureCapacity(int minCapacity) {
      if (minCapacity >= this.bytes.length) {
         int oldCapacity = this.bytes.length;
         int newCapacity = oldCapacity + (oldCapacity >> 1);
         if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
         }

         if (newCapacity - this.maxArraySize > 0) {
            throw new OutOfMemoryError("try enabling LargeObject feature instead");
         }

         this.bytes = Arrays.copyOf(this.bytes, newCapacity);
      }
   }

   @Override
   public final void writeInt32(int[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + values.length * 13 + 2;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }

            off = IOUtils.writeInt32(bytes, off, values[i]);
            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt8(byte i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 5;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      off = IOUtils.writeInt8(bytes, off, i);
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      this.off = off;
   }

   @Override
   public final void writeInt8(byte[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + values.length * 5 + 2;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }

            off = IOUtils.writeInt8(bytes, off, values[i]);
            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt16(short i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 7;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      off = IOUtils.writeInt16(bytes, off, i);
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      this.off = off;
   }

   @Override
   public final void writeInt32(Integer i) {
      if (i == null) {
         this.writeNumberNull();
      } else {
         this.writeInt32(i.intValue());
      }
   }

   @Override
   public final void writeInt32(int i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 13;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      off = IOUtils.writeInt32(bytes, off, i);
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      this.off = off;
   }

   @Override
   public final void writeListInt32(List<Integer> values) {
      if (values == null) {
         this.writeNull();
      } else {
         int size = values.size();
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + 2 + size * 23;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < size; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            Number item = values.get(i);
            if (item == null) {
               JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, IOUtils.NULL_32);
               off += 4;
            } else {
               int v = item.intValue();
               if (writeAsString) {
                  bytes[off++] = (byte)this.quote;
               }

               off = IOUtils.writeInt32(bytes, off, v);
               if (writeAsString) {
                  bytes[off++] = (byte)this.quote;
               }
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt64(long[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         int off = this.off;
         int minCapacity = off + 2 + values.length * 23;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            long v = values[i];
            boolean writeAsString = isWriteAsString(v, this.context.features);
            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }

            off = IOUtils.writeInt64(bytes, off, v);
            if (writeAsString) {
               bytes[off++] = (byte)this.quote;
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeListInt64(List<Long> values) {
      if (values == null) {
         this.writeNull();
      } else {
         int size = values.size();
         int off = this.off;
         int minCapacity = off + 2 + size * 23;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < size; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            Long item = values.get(i);
            if (item == null) {
               JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, IOUtils.NULL_32);
               off += 4;
            } else {
               long v = item;
               boolean writeAsString = isWriteAsString(v, this.context.features);
               if (writeAsString) {
                  bytes[off++] = (byte)this.quote;
               }

               off = IOUtils.writeInt64(bytes, off, v);
               if (writeAsString) {
                  bytes[off++] = (byte)this.quote;
               }
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt64(long i) {
      long features = this.context.features;
      boolean writeAsString = isWriteAsString(i, features);
      int off = this.off;
      int minCapacity = off + 23;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      }

      off = IOUtils.writeInt64(bytes, off, i);
      if (writeAsString) {
         bytes[off++] = (byte)this.quote;
      } else if ((features & JSONWriter.Feature.WriteClassName.mask) != 0L
         && (features & JSONWriter.Feature.NotWriteNumberClassName.mask) == 0L
         && i >= -2147483648L
         && i <= 2147483647L) {
         bytes[off++] = 76;
      }

      this.off = off;
   }

   @Override
   public final void writeInt64(Long i) {
      if (i == null) {
         this.writeNumberNull();
      } else {
         this.writeInt64(i.longValue());
      }
   }

   @Override
   public final void writeFloat(float value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 17;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      if (writeAsString) {
         this.bytes[off++] = 34;
      }

      int len = DoubleToDecimal.toString(value, this.bytes, off, true);
      off += len;
      if (writeAsString) {
         this.bytes[off++] = 34;
      }

      this.off = off;
   }

   @Override
   public final void writeDouble(double value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 26;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      if (writeAsString) {
         bytes[off++] = 34;
      }

      off += DoubleToDecimal.toString(value, bytes, off, true);
      if (writeAsString) {
         bytes[off++] = 34;
      }

      this.off = off;
   }

   @Override
   public final void writeFloat(float[] values) {
      if (values == null) {
         this.writeArrayNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + values.length * (writeAsString ? 16 : 18) + 1;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            if (writeAsString) {
               bytes[off++] = 34;
            }

            float value = values[i];
            int len = DoubleToDecimal.toString(value, bytes, off, true);
            off += len;
            if (writeAsString) {
               bytes[off++] = 34;
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeDouble(double[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + values.length * 27 + 1;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = 91;

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               bytes[off++] = 44;
            }

            if (writeAsString) {
               bytes[off++] = 34;
            }

            double value = values[i];
            int len = DoubleToDecimal.toString(value, bytes, off, true);
            off += len;
            if (writeAsString) {
               bytes[off++] = 34;
            }
         }

         bytes[off] = 93;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeDateTime14(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = (byte)this.quote;
      if (year >= 0 && year <= 9999) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         long base = JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off;
         JDKUtils.UNSAFE.putShort(bytes, base + 1L, IOUtils.PACKED_DIGITS[y01]);
         JDKUtils.UNSAFE.putShort(bytes, base + 3L, IOUtils.PACKED_DIGITS[y23]);
         JDKUtils.UNSAFE.putShort(bytes, base + 5L, IOUtils.PACKED_DIGITS[month]);
         JDKUtils.UNSAFE.putShort(bytes, base + 7L, IOUtils.PACKED_DIGITS[dayOfMonth]);
         JDKUtils.UNSAFE.putShort(bytes, base + 9L, IOUtils.PACKED_DIGITS[hour]);
         JDKUtils.UNSAFE.putShort(bytes, base + 11L, IOUtils.PACKED_DIGITS[minute]);
         JDKUtils.UNSAFE.putShort(bytes, base + 13L, IOUtils.PACKED_DIGITS[second]);
         bytes[off + 15] = (byte)this.quote;
         this.off = off + 16;
      } else {
         throw illegalYear(year);
      }
   }

   @Override
   public final void writeDateTime19(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      int off = this.off;
      int minCapacity = off + 21;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = (byte)this.quote;
      off = IOUtils.writeLocalDate(bytes, off + 1, year, month, dayOfMonth);
      bytes[off] = 32;
      IOUtils.writeLocalTime(bytes, off + 1, hour, minute, second);
      bytes[off + 9] = (byte)this.quote;
      this.off = off + 10;
   }

   @Override
   public final void writeLocalDate(LocalDate date) {
      if (date == null) {
         this.writeNull();
      } else if (this.context.dateFormat == null || !this.writeLocalDateWithFormat(date)) {
         int off = this.off;
         int minCapacity = off + 18;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off++] = (byte)this.quote;
         off = IOUtils.writeLocalDate(bytes, off, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
         bytes[off] = (byte)this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeLocalDateTime(LocalDateTime dateTime) {
      int off = this.off;
      int minCapacity = off + 38;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = (byte)this.quote;
      LocalDate localDate = dateTime.toLocalDate();
      off = IOUtils.writeLocalDate(bytes, off, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
      bytes[off++] = 32;
      off = IOUtils.writeLocalTime(bytes, off, dateTime.toLocalTime());
      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeDateYYYMMDD8(int year, int month, int dayOfMonth) {
      int off = this.off;
      int minCapacity = off + 10;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = (byte)this.quote;
      if (year >= 0 && year <= 9999) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, IOUtils.PACKED_DIGITS[y01]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 3L, IOUtils.PACKED_DIGITS[y23]);
         JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 5L, IOUtils.PACKED_DIGITS[month]);
         JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 7L, IOUtils.PACKED_DIGITS[dayOfMonth]);
         bytes[off + 9] = (byte)this.quote;
         this.off = off + 10;
      } else {
         throw illegalYear(year);
      }
   }

   @Override
   public final void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
      int off = this.off;
      int minCapacity = off + 13;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = (byte)this.quote;
      off = IOUtils.writeLocalDate(bytes, off, year, month, dayOfMonth);
      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeTimeHHMMSS8(int hour, int minute, int second) {
      int off = this.off;
      int minCapacity = off + 10;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off] = (byte)this.quote;
      IOUtils.writeLocalTime(bytes, off + 1, hour, minute, second);
      bytes[off + 9] = (byte)this.quote;
      this.off = off + 10;
   }

   @Override
   public final void writeLocalTime(LocalTime time) {
      int off = this.off;
      int minCapacity = off + 20;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      bytes[off++] = (byte)this.quote;
      off = IOUtils.writeLocalTime(bytes, off, time);
      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeZonedDateTime(ZonedDateTime dateTime) {
      if (dateTime == null) {
         this.writeNull();
      } else {
         ZoneId zone = dateTime.getZone();
         String zoneId = zone.getId();
         int zoneIdLength = zoneId.length();
         char firstZoneChar = 0;
         int zoneSize;
         if (ZoneOffset.UTC != zone && (zoneIdLength > 3 || !"UTC".equals(zoneId) && !"Z".equals(zoneId))) {
            if (zoneIdLength == 0 || (firstZoneChar = zoneId.charAt(0)) != '+' && firstZoneChar != '-') {
               zoneSize = 2 + zoneIdLength;
            } else {
               zoneSize = zoneIdLength;
            }
         } else {
            zoneId = "Z";
            zoneSize = 1;
         }

         int off = this.off;
         int minCapacity = off + zoneSize + 38;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off] = (byte)this.quote;
         LocalDate localDate = dateTime.toLocalDate();
         off = IOUtils.writeLocalDate(bytes, off + 1, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
         bytes[off] = 84;
         off = IOUtils.writeLocalTime(bytes, off + 1, dateTime.toLocalTime());
         if (zoneSize == 1) {
            bytes[off++] = 90;
         } else if (firstZoneChar != '+' && firstZoneChar != '-') {
            bytes[off++] = 91;
            zoneId.getBytes(0, zoneIdLength, bytes, off);
            off += zoneIdLength;
            bytes[off++] = 93;
         } else {
            zoneId.getBytes(0, zoneIdLength, bytes, off);
            off += zoneIdLength;
         }

         bytes[off] = (byte)this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeOffsetDateTime(OffsetDateTime dateTime) {
      if (dateTime == null) {
         this.writeNull();
      } else {
         int off = this.off;
         int minCapacity = off + 45;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off] = (byte)this.quote;
         LocalDateTime ldt = dateTime.toLocalDateTime();
         LocalDate date = ldt.toLocalDate();
         off = IOUtils.writeLocalDate(bytes, off + 1, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
         bytes[off] = 84;
         off = IOUtils.writeLocalTime(bytes, off + 1, ldt.toLocalTime());
         ZoneOffset offset = dateTime.getOffset();
         if (offset.getTotalSeconds() == 0) {
            bytes[off++] = 90;
         } else {
            String zoneId = offset.getId();
            zoneId.getBytes(0, zoneId.length(), bytes, off);
            off += zoneId.length();
         }

         bytes[off] = (byte)this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeOffsetTime(OffsetTime time) {
      if (time == null) {
         this.writeNull();
      } else {
         int off = this.off;
         int minCapacity = off + 45;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         bytes[off] = (byte)this.quote;
         off = IOUtils.writeLocalTime(bytes, off + 1, time.toLocalTime());
         ZoneOffset offset = time.getOffset();
         if (offset.getTotalSeconds() == 0) {
            bytes[off++] = 90;
         } else {
            String zoneId = offset.getId();
            zoneId.getBytes(0, zoneId.length(), bytes, off);
            off += zoneId.length();
         }

         bytes[off] = (byte)this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeBigInt(BigInteger value, long features) {
      if (value == null) {
         this.writeNumberNull();
      } else if (TypeUtils.isInt64(value) && features == 0L) {
         this.writeInt64(value.longValue());
      } else {
         String str = value.toString(10);
         boolean writeAsString = isWriteAsString(value, this.context.features | features);
         int off = this.off;
         int strlen = str.length();
         int minCapacity = off + strlen + (writeAsString ? 2 : 0);
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (writeAsString) {
            bytes[off++] = 34;
         }

         str.getBytes(0, strlen, bytes, off);
         off += strlen;
         if (writeAsString) {
            bytes[off++] = 34;
         }

         this.off = off;
      }
   }

   @Override
   public final void writeDateTimeISO8601(
      int year, int month, int dayOfMonth, int hour, int minute, int second, int millis, int offsetSeconds, boolean timeZone
   ) {
      int zonelen;
      if (timeZone) {
         zonelen = offsetSeconds == 0 ? 1 : 6;
      } else {
         zonelen = 0;
      }

      int minCapacity = this.off + 25 + zonelen;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      bytes[off] = (byte)this.quote;
      off = IOUtils.writeLocalDate(bytes, off + 1, year, month, dayOfMonth);
      bytes[off] = (byte)(timeZone ? 84 : 32);
      IOUtils.writeLocalTime(bytes, off + 1, hour, minute, second);
      off += 9;
      if (millis > 0) {
         int div = millis / 10;
         int div2 = div / 10;
         int rem1 = millis - div * 10;
         if (rem1 != 0) {
            IOUtils.putInt(bytes, off, IOUtils.DIGITS_K_32[millis] & -256 | 46);
            off += 4;
         } else {
            bytes[off++] = 46;
            int rem2 = div - div2 * 10;
            if (rem2 != 0) {
               JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, IOUtils.PACKED_DIGITS[div]);
               off += 2;
            } else {
               bytes[off++] = (byte)(div2 + 48);
            }
         }
      }

      if (timeZone) {
         int offset = offsetSeconds / 3600;
         if (offsetSeconds == 0) {
            bytes[off++] = 90;
         } else {
            int offsetAbs = Math.abs(offset);
            bytes[off] = (byte)(offset >= 0 ? 43 : 45);
            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, IOUtils.PACKED_DIGITS[offsetAbs]);
            bytes[off + 3] = 58;
            int offsetMinutes = (offsetSeconds - offset * 3600) / 60;
            if (offsetMinutes < 0) {
               offsetMinutes = -offsetMinutes;
            }

            JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 4L, IOUtils.PACKED_DIGITS[offsetMinutes]);
            off += 6;
         }
      }

      bytes[off] = (byte)this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeDecimal(BigDecimal value, long features, DecimalFormat format) {
      if (value == null) {
         this.writeNumberNull();
      } else if (format != null) {
         String str = format.format(value);
         this.writeRaw(str);
      } else {
         features |= this.context.features;
         int precision = value.precision();
         boolean writeAsString = isWriteAsString(value, features);
         int off = this.off;
         int minCapacity = off + precision + value.scale() + 7;
         if (minCapacity >= this.bytes.length) {
            this.ensureCapacity(minCapacity);
         }

         byte[] bytes = this.bytes;
         if (writeAsString) {
            bytes[off++] = 34;
         }

         boolean asPlain = (features & JSONWriter.Feature.WriteBigDecimalAsPlain.mask) != 0L;
         long unscaleValue;
         int scale;
         if (precision < 19
            && (scale = value.scale()) >= 0
            && JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET != -1L
            && (unscaleValue = JDKUtils.UNSAFE.getLong(value, JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET)) != Long.MIN_VALUE
            && !asPlain) {
            off = IOUtils.writeDecimal(bytes, off, unscaleValue, scale);
         } else {
            String str = asPlain ? value.toPlainString() : value.toString();
            str.getBytes(0, str.length(), bytes, off);
            off += str.length();
         }

         if (writeAsString) {
            bytes[off++] = 34;
         }

         this.off = off;
      }
   }

   @Override
   public final void writeNameRaw(char[] chars) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public final void writeNameRaw(char[] bytes, int offset, int len) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public final void write(JSONObject map) {
      if (map == null) {
         this.writeNull();
      } else {
         long NONE_DIRECT_FEATURES = JSONWriter.Feature.ReferenceDetection.mask
            | JSONWriter.Feature.PrettyFormat.mask
            | JSONWriter.Feature.NotWriteEmptyArray.mask
            | JSONWriter.Feature.NotWriteDefaultValue.mask;
         if ((this.context.features & NONE_DIRECT_FEATURES) != 0L) {
            ObjectWriter objectWriter = this.context.getObjectWriter(map.getClass());
            objectWriter.write(this, map, null, null, 0L);
         } else {
            if (this.off == this.bytes.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.bytes[this.off++] = 123;
            boolean first = true;

            for (Entry entry : map.entrySet()) {
               Object value = entry.getValue();
               if (value != null || (this.context.features & JSONWriter.Feature.WriteMapNullValue.mask) != 0L) {
                  if (!first) {
                     if (this.off == this.bytes.length) {
                        this.ensureCapacity(this.off + 1);
                     }

                     this.bytes[this.off++] = 44;
                  }

                  first = false;
                  Object key = entry.getKey();
                  if (key instanceof String) {
                     this.writeString((String)key);
                  } else {
                     this.writeAny(key);
                  }

                  if (this.off == this.bytes.length) {
                     this.ensureCapacity(this.off + 1);
                  }

                  this.bytes[this.off++] = 58;
                  if (value == null) {
                     this.writeNull();
                  } else {
                     Class<?> valueClass = value.getClass();
                     if (valueClass == String.class) {
                        this.writeString((String)value);
                     } else if (valueClass == Integer.class) {
                        this.writeInt32((Integer)value);
                     } else if (valueClass == Long.class) {
                        this.writeInt64((Long)value);
                     } else if (valueClass == Boolean.class) {
                        this.writeBool((Boolean)value);
                     } else if (valueClass == BigDecimal.class) {
                        this.writeDecimal((BigDecimal)value, 0L, null);
                     } else if (valueClass == JSONArray.class) {
                        this.write((JSONArray)value);
                     } else if (valueClass == JSONObject.class) {
                        this.write((JSONObject)value);
                     } else {
                        ObjectWriter objectWriter = this.context.getObjectWriter(valueClass, valueClass);
                        objectWriter.write(this, value, null, null, 0L);
                     }
                  }
               }
            }

            if (this.off == this.bytes.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.bytes[this.off++] = 125;
         }
      }
   }

   @Override
   public final void write(List array) {
      if (array == null) {
         this.writeArrayNull();
      } else {
         long NONE_DIRECT_FEATURES = JSONWriter.Feature.ReferenceDetection.mask
            | JSONWriter.Feature.PrettyFormat.mask
            | JSONWriter.Feature.NotWriteEmptyArray.mask
            | JSONWriter.Feature.NotWriteDefaultValue.mask;
         if ((this.context.features & NONE_DIRECT_FEATURES) != 0L) {
            ObjectWriter objectWriter = this.context.getObjectWriter(array.getClass());
            objectWriter.write(this, array, null, null, 0L);
         } else {
            if (this.off == this.bytes.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.bytes[this.off++] = 91;
            boolean first = true;

            for (int i = 0; i < array.size(); i++) {
               Object o = array.get(i);
               if (!first) {
                  if (this.off == this.bytes.length) {
                     this.ensureCapacity(this.off + 1);
                  }

                  this.bytes[this.off++] = 44;
               }

               first = false;
               if (o == null) {
                  this.writeNull();
               } else {
                  Class<?> valueClass = o.getClass();
                  if (valueClass == String.class) {
                     this.writeString((String)o);
                  } else if (valueClass == Integer.class) {
                     this.writeInt32((Integer)o);
                  } else if (valueClass == Long.class) {
                     this.writeInt64((Long)o);
                  } else if (valueClass == Boolean.class) {
                     this.writeBool((Boolean)o);
                  } else if (valueClass == BigDecimal.class) {
                     this.writeDecimal((BigDecimal)o, 0L, null);
                  } else if (valueClass == JSONArray.class) {
                     this.write((JSONArray)o);
                  } else if (valueClass == JSONObject.class) {
                     this.write((JSONObject)o);
                  } else {
                     ObjectWriter objectWriter = this.context.getObjectWriter(valueClass, valueClass);
                     objectWriter.write(this, o, null, null, 0L);
                  }
               }
            }

            if (this.off == this.bytes.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.bytes[this.off++] = 93;
         }
      }
   }

   @Override
   public void writeBool(boolean value) {
      int minCapacity = this.off + 5;
      if (minCapacity >= this.bytes.length) {
         this.ensureCapacity(minCapacity);
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      if ((this.context.features & JSONWriter.Feature.WriteBooleanAsNumber.mask) != 0L) {
         bytes[off++] = (byte)(value ? 49 : 48);
      } else {
         if (!value) {
            bytes[off++] = 102;
         }

         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off, value ? IOUtils.TRUE : IOUtils.ALSE);
         off += 4;
      }

      this.off = off;
   }

   @Override
   public final String toString() {
      return new String(this.bytes, 0, this.off, StandardCharsets.UTF_8);
   }

   @Override
   public final int flushTo(OutputStream out, Charset charset) throws IOException {
      if (this.off == 0) {
         return 0;
      } else if (charset != null && charset != StandardCharsets.UTF_8 && charset != StandardCharsets.US_ASCII) {
         if (charset == StandardCharsets.ISO_8859_1) {
            boolean hasNegative = false;
            if (JDKUtils.METHOD_HANDLE_HAS_NEGATIVE != null) {
               try {
                  hasNegative = (Boolean)JDKUtils.METHOD_HANDLE_HAS_NEGATIVE.invoke((byte[])this.bytes, (int)0, (int)this.bytes.length);
               } catch (Throwable var5) {
               }
            }

            if (!hasNegative) {
               int len = this.off;
               out.write(this.bytes, 0, this.off);
               this.off = 0;
               return len;
            }
         }

         String str = new String(this.bytes, 0, this.off);
         byte[] encodedBytes = str.getBytes(charset);
         out.write(encodedBytes);
         return encodedBytes.length;
      } else {
         int len = this.off;
         out.write(this.bytes, 0, this.off);
         this.off = 0;
         return len;
      }
   }

   static {
      short[] digits = new short[256];

      for (int i = 0; i < 16; i++) {
         short hi = (short)(i < 10 ? i + 48 : i - 10 + 97);

         for (int j = 0; j < 16; j++) {
            short lo = (short)(j < 10 ? j + 48 : j - 10 + 97);
            digits[(i << 4) + j] = JDKUtils.BIG_ENDIAN ? (short)(hi << 8 | lo) : (short)(hi | lo << 8);
         }
      }

      HEX256 = digits;
   }
}
