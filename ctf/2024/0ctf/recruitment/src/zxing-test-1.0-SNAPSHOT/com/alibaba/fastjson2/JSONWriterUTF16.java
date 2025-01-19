package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.DoubleToDecimal;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
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
import sun.misc.Unsafe;

class JSONWriterUTF16 extends JSONWriter {
   static final char[] REF_PREF = "{\"$ref\":".toCharArray();
   static final int[] HEX256;
   protected char[] chars;
   final JSONFactory.CacheItem cacheItem;

   JSONWriterUTF16(JSONWriter.Context ctx) {
      super(ctx, null, false, StandardCharsets.UTF_16);
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      this.cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      char[] chars = JSONFactory.CHARS_UPDATER.getAndSet(this.cacheItem, null);
      if (chars == null) {
         chars = new char[8192];
      }

      this.chars = chars;
   }

   @Override
   public final void writeNull() {
      int minCapacity = this.off + 4;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      JDKUtils.UNSAFE.putLong(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)this.off << 1), IOUtils.NULL_64);
      this.off += 4;
   }

   @Override
   public final void flushTo(Writer to) {
      try {
         int off = this.off;
         if (off > 0) {
            to.write(this.chars, 0, off);
            this.off = 0;
         }
      } catch (IOException var3) {
         throw new JSONException("flushTo error", var3);
      }
   }

   @Override
   public final void close() {
      char[] chars = this.chars;
      if (chars.length <= 4194304) {
         JSONFactory.CHARS_UPDATER.lazySet(this.cacheItem, chars);
      }
   }

   @Override
   protected final void write0(char c) {
      int off = this.off;
      if (off == this.chars.length) {
         this.ensureCapacity(off + 1);
      }

      this.chars[off] = c;
      this.off = off + 1;
   }

   @Override
   public final void writeColon() {
      int off = this.off;
      if (off == this.chars.length) {
         this.ensureCapacity(off + 1);
      }

      this.chars[off] = ':';
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '{';
         if (this.pretty) {
            this.indent++;
            chars[off++] = '\n';

            for (int i = 0; i < this.indent; i++) {
               chars[off++] = '\t';
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
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.pretty) {
         this.indent--;
         chars[off++] = '\n';

         for (int i = 0; i < this.indent; i++) {
            chars[off++] = '\t';
         }
      }

      chars[off] = '}';
      this.off = off + 1;
      this.startObject = false;
   }

   @Override
   public final void writeComma() {
      this.startObject = false;
      int off = this.off;
      int minCapacity = off + (this.pretty ? 2 + this.indent : 1);
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off++] = ',';
      if (this.pretty) {
         chars[off++] = '\n';

         for (int i = 0; i < this.indent; i++) {
            chars[off++] = '\t';
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';
         if (this.pretty) {
            this.indent++;
            chars[off++] = '\n';

            for (int i = 0; i < this.indent; i++) {
               chars[off++] = '\t';
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
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.pretty) {
         this.indent--;
         chars[off++] = '\n';

         for (int i = 0; i < this.indent; i++) {
            chars[off++] = '\t';
         }
      }

      chars[off] = ']';
      this.off = off + 1;
      this.startObject = false;
   }

   @Override
   public final void writeString(List<String> list) {
      if (this.off == this.chars.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.chars[this.off++] = '[';
      int i = 0;

      for (int size = list.size(); i < size; i++) {
         if (i != 0) {
            if (this.off == this.chars.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.chars[this.off++] = ',';
         }

         String str = list.get(i);
         this.writeString(str);
      }

      if (this.off == this.chars.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.chars[this.off++] = ']';
   }

   @Override
   public void writeStringLatin1(byte[] value) {
      if ((this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L) {
         this.writeStringLatin1BrowserSecure(value);
      } else {
         boolean escape = false;
         int off = this.off;
         int minCapacity = off + value.length + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         int start = off;
         char[] chars = this.chars;
         chars[off++] = this.quote;

         for (byte c : value) {
            if (c == 92 || c == this.quote || c < 32) {
               escape = true;
               break;
            }

            chars[off++] = (char)c;
         }

         if (!escape) {
            chars[off] = this.quote;
            this.off = off + 1;
         } else {
            this.off = start;
            this.writeStringEscape(value);
         }
      }
   }

   protected final void writeStringLatin1BrowserSecure(byte[] value) {
      boolean escape = false;
      int off = this.off;
      int minCapacity = off + value.length + 2;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      int start = off;
      char[] chars = this.chars;
      chars[off++] = this.quote;

      for (byte c : value) {
         if (c == 92 || c == this.quote || c < 32 || c == 60 || c == 62 || c == 40 || c == 41) {
            escape = true;
            break;
         }

         chars[off++] = (char)c;
      }

      if (!escape) {
         chars[off] = this.quote;
         this.off = off + 1;
      } else {
         this.off = start;
         this.writeStringEscape(value);
      }
   }

   @Override
   public void writeStringUTF16(byte[] value) {
      if (value == null) {
         this.writeStringNull();
      } else {
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         boolean escape = false;
         int off = this.off;
         int minCapacity = off + value.length + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = this.quote;

         for (int i = 0; i < value.length; i += 2) {
            char c = JDKUtils.UNSAFE.getChar(value, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + (long)i);
            if (c == '\\' || c == this.quote || c < ' ' || browserSecure && (c == '<' || c == '>' || c == '(' || c == ')') || escapeNoneAscii && c > 127) {
               escape = true;
               break;
            }

            chars[off++] = c;
         }

         if (!escape) {
            chars[off] = this.quote;
            this.off = off + 1;
         } else {
            this.writeStringEscapeUTF16(value);
         }
      }
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeStringNull();
      } else {
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escape = false;
         char quote = this.quote;
         int strlen = str.length();
         int minCapacity = this.off + strlen + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         for (int i = 0; i < strlen; i++) {
            char c = str.charAt(i);
            if (c == '\\' || c == quote || c < ' ' || browserSecure && (c == '<' || c == '>' || c == '(' || c == ')') || escapeNoneAscii && c > 127) {
               escape = true;
               break;
            }
         }

         if (!escape) {
            int off = this.off;
            char[] chars = this.chars;
            chars[off++] = quote;
            str.getChars(0, strlen, chars, off);
            off += strlen;
            chars[off] = quote;
            this.off = off + 1;
         } else {
            this.writeStringEscape(str);
         }
      }
   }

   protected final void writeStringEscape(String str) {
      int strlen = str.length();
      char quote = this.quote;
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
      int off = this.off;
      this.ensureCapacity(off + strlen * 6 + 2);
      char[] chars = this.chars;
      chars[off++] = quote;

      for (int i = 0; i < strlen; i++) {
         char ch = str.charAt(i);
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)('0' + ch);
               off += 6;
               break;
            case '\b':
               chars[off] = '\\';
               chars[off + 1] = 'b';
               off += 2;
               break;
            case '\t':
               chars[off] = '\\';
               chars[off + 1] = 't';
               off += 2;
               break;
            case '\n':
               chars[off] = '\\';
               chars[off + 1] = 'n';
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               chars[off] = '\\';
               chars[off + 1] = 'f';
               off += 2;
               break;
            case '\r':
               chars[off] = '\\';
               chars[off + 1] = 'r';
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
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
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
               if (escapeNoneAscii && ch > 127) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = DIGITS[ch >>> '\f' & 15];
                  chars[off + 3] = DIGITS[ch >>> '\b' & 15];
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '"':
            case '\'':
               if (ch == quote) {
                  chars[off++] = '\\';
               }

               chars[off++] = ch;
               break;
            case '(':
            case ')':
            case '<':
            case '>':
               if (browserSecure) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = '0';
                  chars[off + 3] = '0';
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '\\':
               chars[off] = '\\';
               chars[off + 1] = ch;
               off += 2;
         }
      }

      chars[off] = quote;
      this.off = off + 1;
   }

   protected final void writeStringEscapeUTF16(byte[] str) {
      int strlen = str.length;
      char quote = this.quote;
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
      int off = this.off;
      this.ensureCapacity(off + strlen * 6 + 2);
      char[] chars = this.chars;
      chars[off++] = quote;

      for (int i = 0; i < strlen; i += 2) {
         char ch = JDKUtils.UNSAFE.getChar(str, (long)Unsafe.ARRAY_BYTE_BASE_OFFSET + (long)i);
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)('0' + ch);
               off += 6;
               break;
            case '\b':
               chars[off] = '\\';
               chars[off + 1] = 'b';
               off += 2;
               break;
            case '\t':
               chars[off] = '\\';
               chars[off + 1] = 't';
               off += 2;
               break;
            case '\n':
               chars[off] = '\\';
               chars[off + 1] = 'n';
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               chars[off] = '\\';
               chars[off + 1] = 'f';
               off += 2;
               break;
            case '\r':
               chars[off] = '\\';
               chars[off + 1] = 'r';
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
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
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
               if (escapeNoneAscii && ch > 127) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = DIGITS[ch >>> '\f' & 15];
                  chars[off + 3] = DIGITS[ch >>> '\b' & 15];
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '"':
            case '\'':
               if (ch == quote) {
                  chars[off++] = '\\';
               }

               chars[off++] = ch;
               break;
            case '(':
            case ')':
            case '<':
            case '>':
               if (browserSecure) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = '0';
                  chars[off + 3] = '0';
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '\\':
               chars[off] = '\\';
               chars[off + 1] = ch;
               off += 2;
         }
      }

      chars[off] = quote;
      this.off = off + 1;
   }

   protected final void writeStringEscape(char[] str) {
      int strlen = str.length;
      char quote = this.quote;
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
      int off = this.off;
      this.ensureCapacity(off + strlen * 6 + 2);
      char[] chars = this.chars;
      chars[off++] = quote;

      for (int i = 0; i < str.length; i++) {
         char ch = str[i];
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)('0' + ch);
               off += 6;
               break;
            case '\b':
               chars[off] = '\\';
               chars[off + 1] = 'b';
               off += 2;
               break;
            case '\t':
               chars[off] = '\\';
               chars[off + 1] = 't';
               off += 2;
               break;
            case '\n':
               chars[off] = '\\';
               chars[off + 1] = 'n';
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               chars[off] = '\\';
               chars[off + 1] = 'f';
               off += 2;
               break;
            case '\r':
               chars[off] = '\\';
               chars[off + 1] = 'r';
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
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
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
               if (escapeNoneAscii && ch > 127) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = DIGITS[ch >>> '\f' & 15];
                  chars[off + 3] = DIGITS[ch >>> '\b' & 15];
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '"':
            case '\'':
               if (ch == quote) {
                  chars[off++] = '\\';
               }

               chars[off++] = ch;
               break;
            case '(':
            case ')':
            case '<':
            case '>':
               if (browserSecure) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = '0';
                  chars[off + 3] = '0';
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '\\':
               chars[off] = '\\';
               chars[off + 1] = ch;
               off += 2;
         }
      }

      chars[off] = quote;
      this.off = off + 1;
   }

   protected final void writeStringEscape(byte[] str) {
      int strlen = str.length;
      char quote = this.quote;
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
      int off = this.off;
      this.ensureCapacity(off + strlen * 6 + 2);
      char[] chars = this.chars;
      chars[off++] = quote;

      for (int i = 0; i < str.length; i++) {
         byte b = str[i];
         char ch = (char)(b & 255);
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)('0' + ch);
               off += 6;
               break;
            case '\b':
               chars[off] = '\\';
               chars[off + 1] = 'b';
               off += 2;
               break;
            case '\t':
               chars[off] = '\\';
               chars[off + 1] = 't';
               off += 2;
               break;
            case '\n':
               chars[off] = '\\';
               chars[off + 1] = 'n';
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               chars[off] = '\\';
               chars[off + 1] = 'f';
               off += 2;
               break;
            case '\r':
               chars[off] = '\\';
               chars[off + 1] = 'r';
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
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
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
               if (escapeNoneAscii && ch > 127) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = '0';
                  chars[off + 3] = '0';
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '"':
            case '\'':
               if (ch == quote) {
                  chars[off++] = '\\';
               }

               chars[off++] = ch;
               break;
            case '(':
            case ')':
            case '<':
            case '>':
               if (browserSecure) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = '0';
                  chars[off + 3] = '0';
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '\\':
               chars[off] = '\\';
               chars[off + 1] = ch;
               off += 2;
         }
      }

      chars[off] = quote;
      this.off = off + 1;
   }

   @Override
   public final void writeString(char[] str, int offset, int len, boolean quoted) {
      boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
      char quote = this.quote;
      int off = this.off;
      int minCapacity = quoted ? off + 2 : off;
      if (escapeNoneAscii) {
         minCapacity += len * 6;
      } else {
         minCapacity += len * 2;
      }

      if (minCapacity - this.chars.length > 0) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (quoted) {
         chars[off++] = quote;
      }

      for (int i = offset; i < len; i++) {
         char ch = str[i];
         switch (ch) {
            case '\u0000':
            case '\u0001':
            case '\u0002':
            case '\u0003':
            case '\u0004':
            case '\u0005':
            case '\u0006':
            case '\u0007':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)('0' + ch);
               off += 6;
               break;
            case '\b':
               chars[off] = '\\';
               chars[off + 1] = 'b';
               off += 2;
               break;
            case '\t':
               chars[off] = '\\';
               chars[off + 1] = 't';
               off += 2;
               break;
            case '\n':
               chars[off] = '\\';
               chars[off + 1] = 'n';
               off += 2;
               break;
            case '\u000b':
            case '\u000e':
            case '\u000f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '0';
               chars[off + 5] = (char)(97 + (ch - '\n'));
               off += 6;
               break;
            case '\f':
               chars[off] = '\\';
               chars[off + 1] = 'f';
               off += 2;
               break;
            case '\r':
               chars[off] = '\\';
               chars[off + 1] = 'r';
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
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(48 + (ch - 16));
               off += 6;
               break;
            case '\u001a':
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
               chars[off] = '\\';
               chars[off + 1] = 'u';
               chars[off + 2] = '0';
               chars[off + 3] = '0';
               chars[off + 4] = '1';
               chars[off + 5] = (char)(97 + (ch - 26));
               off += 6;
               break;
            case ' ':
            case '!':
            case '#':
            case '$':
            case '%':
            case '&':
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
               if (escapeNoneAscii && ch > 127) {
                  chars[off] = '\\';
                  chars[off + 1] = 'u';
                  chars[off + 2] = DIGITS[ch >>> '\f' & 15];
                  chars[off + 3] = DIGITS[ch >>> '\b' & 15];
                  chars[off + 4] = DIGITS[ch >>> 4 & 15];
                  chars[off + 5] = DIGITS[ch & 15];
                  off += 6;
               } else {
                  chars[off++] = ch;
               }
               break;
            case '"':
            case '\'':
               if (ch == quote) {
                  chars[off++] = '\\';
               }

               chars[off++] = ch;
               break;
            case '\\':
               chars[off] = '\\';
               chars[off + 1] = ch;
               off += 2;
         }
      }

      if (quoted) {
         chars[off++] = quote;
      }

      this.off = off;
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
   public final void writeReference(String path) {
      this.lastReference = path;
      this.writeRaw(REF_PREF, 0, REF_PREF.length);
      this.writeString(path);
      int off = this.off;
      if (off == this.chars.length) {
         this.ensureCapacity(off + 1);
      }

      this.chars[off] = '}';
      this.off = off + 1;
   }

   @Override
   public final void writeBase64(byte[] bytes) {
      if (bytes == null) {
         this.writeArrayNull();
      } else {
         int charsLen = (bytes.length - 1) / 3 + 1 << 2;
         int off = this.off;
         this.ensureCapacity(off + charsLen + 2);
         char[] chars = this.chars;
         chars[off++] = this.quote;
         int eLen = bytes.length / 3 * 3;

         for (int s = 0; s < eLen; off += 4) {
            int i = (bytes[s++] & 255) << 16 | (bytes[s++] & 255) << 8 | bytes[s++] & 255;
            chars[off] = JSONFactory.CA[i >>> 18 & 63];
            chars[off + 1] = JSONFactory.CA[i >>> 12 & 63];
            chars[off + 2] = JSONFactory.CA[i >>> 6 & 63];
            chars[off + 3] = JSONFactory.CA[i & 63];
         }

         int left = bytes.length - eLen;
         if (left > 0) {
            int i = (bytes[eLen] & 255) << 10 | (left == 2 ? (bytes[bytes.length - 1] & 255) << 2 : 0);
            chars[off] = JSONFactory.CA[i >> 12];
            chars[off + 1] = JSONFactory.CA[i >>> 6 & 63];
            chars[off + 2] = left == 2 ? JSONFactory.CA[i & 63] : 61;
            chars[off + 3] = '=';
            off += 4;
         }

         chars[off] = this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeHex(byte[] bytes) {
      if (bytes == null) {
         this.writeNull();
      } else {
         int charsLen = bytes.length * 2 + 3;
         int off = this.off;
         this.ensureCapacity(off + charsLen + 2);
         char[] chars = this.chars;
         chars[off] = 'x';
         chars[off + 1] = '\'';
         off += 2;

         for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            int a = b & 255;
            int b0 = a >> 4;
            int b1 = a & 15;
            chars[off] = (char)(b0 + (b0 < 10 ? 48 : 55));
            chars[off + 1] = (char)(b1 + (b1 < 10 ? 48 : 55));
            off += 2;
         }

         chars[off] = '\'';
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] bytes = this.chars;
         if (writeAsString) {
            bytes[off++] = '"';
         }

         str.getChars(0, strlen, bytes, off);
         off += strlen;
         if (writeAsString) {
            bytes[off++] = '"';
         }

         this.off = off;
      }
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         if (writeAsString) {
            chars[off++] = '"';
         }

         boolean asPlain = (features & JSONWriter.Feature.WriteBigDecimalAsPlain.mask) != 0L;
         long unscaleValue;
         int scale;
         if (precision < 19
            && (scale = value.scale()) >= 0
            && JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET != -1L
            && (unscaleValue = JDKUtils.UNSAFE.getLong(value, JDKUtils.FIELD_DECIMAL_INT_COMPACT_OFFSET)) != Long.MIN_VALUE
            && !asPlain) {
            off = IOUtils.writeDecimal(chars, off, unscaleValue, scale);
         } else {
            String str = asPlain ? value.toPlainString() : value.toString();
            str.getChars(0, str.length(), chars, off);
            off += str.length();
         }

         if (writeAsString) {
            chars[off++] = '"';
         }

         this.off = off;
      }
   }

   static void putLong(char[] buf, int off, int b0, int b1) {
      long v = (long)HEX256[b0 & 0xFF] | (long)HEX256[b1 & 0xFF] << 32;
      JDKUtils.UNSAFE.putLong(buf, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)off << 1), JDKUtils.BIG_ENDIAN ? Long.reverseBytes(v << 8) : v);
   }

   @Override
   public final void writeUUID(UUID value) {
      if (value == null) {
         this.writeNull();
      } else {
         long msb = value.getMostSignificantBits();
         long lsb = value.getLeastSignificantBits();
         int minCapacity = this.off + 38;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] buf = this.chars;
         int off = this.off;
         buf[off] = '"';
         putLong(buf, off + 1, (int)(msb >> 56), (int)(msb >> 48));
         putLong(buf, off + 5, (int)(msb >> 40), (int)(msb >> 32));
         buf[off + 9] = '-';
         putLong(buf, off + 10, (int)msb >> 24, (int)msb >> 16);
         buf[off + 14] = '-';
         putLong(buf, off + 15, (int)msb >> 8, (int)msb);
         buf[off + 19] = '-';
         putLong(buf, off + 20, (int)(lsb >> 56), (int)(lsb >> 48));
         buf[off + 24] = '-';
         putLong(buf, off + 25, (int)(lsb >> 40), (int)(lsb >> 32));
         putLong(buf, off + 29, (int)lsb >> 24, (int)lsb >> 16);
         putLong(buf, off + 33, (int)lsb >> 8, (int)lsb);
         buf[off + 37] = '"';
         this.off += 38;
      }
   }

   @Override
   public final void writeRaw(String str) {
      this.ensureCapacity(this.off + str.length());
      str.getChars(0, str.length(), this.chars, this.off);
      this.off = this.off + str.length();
   }

   @Override
   public final void writeRaw(char[] chars, int off, int charslen) {
      int minCapacity = this.off + charslen;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      System.arraycopy(chars, off, this.chars, this.off, charslen);
      this.off += charslen;
   }

   @Override
   public final void writeChar(char ch) {
      int off = this.off;
      int minCapacity = off + 8;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off++] = this.quote;
      switch (ch) {
         case '\u0000':
         case '\u0001':
         case '\u0002':
         case '\u0003':
         case '\u0004':
         case '\u0005':
         case '\u0006':
         case '\u0007':
            chars[off] = '\\';
            chars[off + 1] = 'u';
            chars[off + 2] = '0';
            chars[off + 3] = '0';
            chars[off + 4] = '0';
            chars[off + 5] = (char)('0' + ch);
            off += 6;
            break;
         case '\b':
            chars[off] = '\\';
            chars[off + 1] = 'b';
            off += 2;
            break;
         case '\t':
            chars[off] = '\\';
            chars[off + 1] = 't';
            off += 2;
            break;
         case '\n':
            chars[off] = '\\';
            chars[off + 1] = 'n';
            off += 2;
            break;
         case '\u000b':
         case '\u000e':
         case '\u000f':
            chars[off] = '\\';
            chars[off + 1] = 'u';
            chars[off + 2] = '0';
            chars[off + 3] = '0';
            chars[off + 4] = '0';
            chars[off + 5] = (char)(97 + (ch - '\n'));
            off += 6;
            break;
         case '\f':
            chars[off] = '\\';
            chars[off + 1] = 'f';
            off += 2;
            break;
         case '\r':
            chars[off] = '\\';
            chars[off + 1] = 'r';
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
            chars[off] = '\\';
            chars[off + 1] = 'u';
            chars[off + 2] = '0';
            chars[off + 3] = '0';
            chars[off + 4] = '1';
            chars[off + 5] = (char)(48 + (ch - 16));
            off += 6;
            break;
         case '\u001a':
         case '\u001b':
         case '\u001c':
         case '\u001d':
         case '\u001e':
         case '\u001f':
            chars[off] = '\\';
            chars[off + 1] = 'u';
            chars[off + 2] = '0';
            chars[off + 3] = '0';
            chars[off + 4] = '1';
            chars[off + 5] = (char)(97 + (ch - 26));
            off += 6;
            break;
         case ' ':
         case '!':
         case '#':
         case '$':
         case '%':
         case '&':
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
            chars[off++] = ch;
            break;
         case '"':
         case '\'':
            if (ch == this.quote) {
               chars[off++] = '\\';
            }

            chars[off++] = ch;
            break;
         case '\\':
            chars[off] = '\\';
            chars[off + 1] = ch;
            off += 2;
      }

      chars[off] = this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeRaw(char ch) {
      if (this.off == this.chars.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.chars[this.off++] = ch;
   }

   @Override
   public final void writeRaw(char c0, char c1) {
      int off = this.off;
      if (off + 1 >= this.chars.length) {
         this.ensureCapacity(off + 2);
      }

      this.chars[off] = c0;
      this.chars[off + 1] = c1;
      this.off = off + 2;
   }

   @Override
   public final void writeNameRaw(char[] name) {
      int off = this.off;
      int minCapacity = off + name.length + 2 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      if (this.startObject) {
         this.startObject = false;
      } else {
         char[] chars = this.chars;
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      System.arraycopy(name, 0, this.chars, off, name.length);
      this.off = off + name.length;
   }

   @Override
   public final void writeName2Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      this.off = off + 5;
   }

   @Override
   public final void writeName3Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      this.off = off + 6;
   }

   @Override
   public final void writeName4Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      this.off = off + 7;
   }

   @Override
   public final void writeName5Raw(long name) {
      int off = this.off;
      int minCapacity = off + 10 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      this.off = off + 8;
   }

   @Override
   public final void writeName6Raw(long name) {
      int off = this.off;
      int minCapacity = off + 11 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      chars[off + 8] = ':';
      this.off = off + 9;
   }

   @Override
   public final void writeName7Raw(long name) {
      int off = this.off;
      int minCapacity = off + 12 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name);
      chars[off + 8] = this.quote;
      chars[off + 9] = ':';
      this.off = off + 10;
   }

   @Override
   public final void writeName8Raw(long name) {
      int off = this.off;
      int minCapacity = off + 13 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      chars[off++] = this.quote;
      putLong(chars, off, name);
      chars[off + 8] = this.quote;
      chars[off + 9] = ':';
      this.off = off + 10;
   }

   @Override
   public final void writeName9Raw(long name0, int name1) {
      int off = this.off;
      int minCapacity = off + 14 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      this.off = off + 12;
   }

   @Override
   public final void writeName10Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      this.off = off + 13;
   }

   @Override
   public final void writeName11Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      this.off = off + 14;
   }

   @Override
   public final void writeName12Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      this.off = off + 15;
   }

   @Override
   public final void writeName13Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 18 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      this.off = off + 16;
   }

   @Override
   public final void writeName14Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 19 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      chars[off + 16] = ':';
      this.off = off + 17;
   }

   @Override
   public final void writeName15Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 20 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      putLong(chars, off, name0, name1);
      chars[off + 16] = this.quote;
      chars[off + 17] = ':';
      this.off = off + 18;
   }

   @Override
   public final void writeName16Raw(long name0, long name1) {
      int off = this.off;
      int minCapacity = off + 21 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (this.startObject) {
         this.startObject = false;
      } else {
         chars[off++] = ',';
         if (this.pretty) {
            off = indent(chars, off, this.indent);
         }
      }

      chars[off++] = this.quote;
      putLong(chars, off, name0, name1);
      chars[off + 16] = this.quote;
      chars[off + 17] = ':';
      this.off = off + 18;
   }

   private static void putLong(char[] chars, int off, long name) {
      long base = JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off << 1);
      JDKUtils.UNSAFE.putLong(chars, base, name & 255L | (name & 65280L) << 8 | (name & 16711680L) << 16 | (name & 4278190080L) << 24);
      JDKUtils.UNSAFE
         .putLong(
            chars,
            base + 8L,
            (name & 1095216660480L) >> 32 | (name & 280375465082880L) >> 24 | (name & 71776119061217280L) >> 16 | (name & -72057594037927936L) >> 8
         );
   }

   private static void putLong(char[] chars, int off, long name, int name1) {
      long base = JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off << 1);
      JDKUtils.UNSAFE.putLong(chars, base, name & 255L | (name & 65280L) << 8 | (name & 16711680L) << 16 | (name & 4278190080L) << 24);
      JDKUtils.UNSAFE
         .putLong(
            chars,
            base + 8L,
            (name & 1095216660480L) >> 32 | (name & 280375465082880L) >> 24 | (name & 71776119061217280L) >> 16 | (name & -72057594037927936L) >> 8
         );
      JDKUtils.UNSAFE
         .putLong(chars, base + 16L, (long)name1 & 255L | ((long)name1 & 65280L) << 8 | ((long)name1 & 16711680L) << 16 | ((long)name1 & 4278190080L) << 24);
   }

   private static void putLong(char[] chars, int off, long name, long name1) {
      long base = JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off << 1);
      JDKUtils.UNSAFE.putLong(chars, base, name & 255L | (name & 65280L) << 8 | (name & 16711680L) << 16 | (name & 4278190080L) << 24);
      JDKUtils.UNSAFE
         .putLong(
            chars,
            base + 8L,
            (name & 1095216660480L) >> 32 | (name & 280375465082880L) >> 24 | (name & 71776119061217280L) >> 16 | (name & -72057594037927936L) >> 8
         );
      JDKUtils.UNSAFE.putLong(chars, base + 16L, name1 & 255L | (name1 & 65280L) << 8 | (name1 & 16711680L) << 16 | (name1 & 4278190080L) << 24);
      JDKUtils.UNSAFE
         .putLong(
            chars,
            base + 24L,
            (name1 & 1095216660480L) >> 32 | (name1 & 280375465082880L) >> 24 | (name1 & 71776119061217280L) >> 16 | (name1 & -72057594037927936L) >> 8
         );
   }

   private static int indent(char[] chars, int off, int indent) {
      chars[off++] = '\n';
      int end = off + indent;

      while (off < end) {
         chars[off++] = '\t';
      }

      return off;
   }

   @Override
   public final void writeNameRaw(char[] chars, int off, int len) {
      int minCapacity = this.off + len + 2 + this.indent;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      if (this.startObject) {
         this.startObject = false;
      } else {
         this.chars[this.off++] = ',';
      }

      System.arraycopy(chars, off, this.chars, this.off, len);
      this.off += len;
   }

   final void ensureCapacity(int minCapacity) {
      if (minCapacity - this.chars.length > 0) {
         int oldCapacity = this.chars.length;
         int newCapacity = oldCapacity + (oldCapacity >> 1);
         if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
         }

         if (newCapacity - this.maxArraySize > 0) {
            throw new OutOfMemoryError("try enabling LargeObject feature instead");
         }

         this.chars = Arrays.copyOf(this.chars, newCapacity);
      }
   }

   @Override
   public final void writeInt32(int[] value) {
      if (value == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + value.length * 13 + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            if (writeAsString) {
               chars[off++] = this.quote;
            }

            off = IOUtils.writeInt32(chars, off, value[i]);
            if (writeAsString) {
               chars[off++] = this.quote;
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt8(byte i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 7;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      off = IOUtils.writeInt8(chars, off, i);
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      this.off = off;
   }

   @Override
   public final void writeInt8(byte[] value) {
      if (value == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + value.length * 5 + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            if (writeAsString) {
               chars[off++] = this.quote;
            }

            off = IOUtils.writeInt8(chars, off, value[i]);
            if (writeAsString) {
               chars[off++] = this.quote;
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt16(short i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 7;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      off = IOUtils.writeInt16(chars, off, i);
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      this.off = off;
   }

   @Override
   public final void writeInt32(int i) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 13;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      off = IOUtils.writeInt32(chars, off, i);
      if (writeAsString) {
         chars[off++] = this.quote;
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
   public final void writeInt64(long[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         int off = this.off;
         int minCapacity = off + 2 + values.length * 23;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            long v = values[i];
            boolean writeAsString = isWriteAsString(v, this.context.features);
            if (writeAsString) {
               chars[off++] = this.quote;
            }

            off = IOUtils.writeInt64(chars, off, v);
            if (writeAsString) {
               chars[off++] = this.quote;
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < size; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            Number item = values.get(i);
            if (item == null) {
               chars[off] = 'n';
               chars[off + 1] = 'u';
               chars[off + 2] = 'l';
               chars[off + 3] = 'l';
               off += 4;
            } else {
               int v = item.intValue();
               if (writeAsString) {
                  chars[off++] = this.quote;
               }

               off = IOUtils.writeInt32(chars, off, v);
               if (writeAsString) {
                  chars[off++] = this.quote;
               }
            }
         }

         chars[off] = ']';
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < size; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            Long item = values.get(i);
            if (item == null) {
               chars[off] = 'n';
               chars[off + 1] = 'u';
               chars[off + 2] = 'l';
               chars[off + 3] = 'l';
               off += 4;
            } else {
               long v = item;
               boolean writeAsString = isWriteAsString(v, this.context.features);
               if (writeAsString) {
                  chars[off++] = this.quote;
               }

               off = IOUtils.writeInt64(chars, off, v);
               if (writeAsString) {
                  chars[off++] = this.quote;
               }
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
   }

   @Override
   public final void writeInt64(long i) {
      long features = this.context.features;
      boolean writeAsString = isWriteAsString(i, features);
      int off = this.off;
      int minCapacity = off + 23;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = this.quote;
      }

      off = IOUtils.writeInt64(chars, off, i);
      if (writeAsString) {
         chars[off++] = this.quote;
      } else if ((features & JSONWriter.Feature.WriteClassName.mask) != 0L
         && (features & JSONWriter.Feature.NotWriteNumberClassName.mask) == 0L
         && i >= -2147483648L
         && i <= 2147483647L) {
         chars[off++] = 'L';
      }

      this.off = off;
   }

   @Override
   public final void writeInt64(Long i) {
      if (i == null) {
         this.writeInt64Null();
      } else {
         this.writeInt64(i.longValue());
      }
   }

   @Override
   public final void writeFloat(float value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 15;
      if (writeAsString) {
         minCapacity += 2;
      }

      this.ensureCapacity(minCapacity);
      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = '"';
      }

      int len = DoubleToDecimal.toString(value, chars, off, true);
      off += len;
      if (writeAsString) {
         chars[off++] = '"';
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            if (writeAsString) {
               chars[off++] = '"';
            }

            float value = values[i];
            int len = DoubleToDecimal.toString(value, chars, off, true);
            off += len;
            if (writeAsString) {
               chars[off++] = '"';
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
   }

   @Override
   public final void writeDouble(double value) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 24;
      if (writeAsString) {
         minCapacity += 2;
      }

      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      if (writeAsString) {
         chars[off++] = '"';
      }

      off += DoubleToDecimal.toString(value, chars, off, true);
      if (writeAsString) {
         chars[off++] = '"';
      }

      this.off = off;
   }

   @Override
   public final void writeDoubleArray(double value0, double value1) {
      boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      int off = this.off;
      int minCapacity = off + 48 + 3;
      if (writeAsString) {
         minCapacity += 2;
      }

      this.ensureCapacity(minCapacity);
      char[] chars = this.chars;
      chars[off++] = '[';
      if (writeAsString) {
         chars[off++] = '"';
      }

      int len0 = DoubleToDecimal.toString(value0, chars, off, true);
      off += len0;
      if (writeAsString) {
         chars[off++] = '"';
      }

      chars[off++] = ',';
      if (writeAsString) {
         chars[off++] = '"';
      }

      int len1 = DoubleToDecimal.toString(value1, chars, off, true);
      off += len1;
      if (writeAsString) {
         chars[off++] = '"';
      }

      chars[off] = ']';
      this.off = off + 1;
   }

   @Override
   public final void writeDouble(double[] values) {
      if (values == null) {
         this.writeNull();
      } else {
         boolean writeAsString = (this.context.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         int off = this.off;
         int minCapacity = off + values.length * 27 + 1;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = '[';

         for (int i = 0; i < values.length; i++) {
            if (i != 0) {
               chars[off++] = ',';
            }

            if (writeAsString) {
               chars[off++] = '"';
            }

            double value = values[i];
            int len = DoubleToDecimal.toString(value, chars, off, true);
            off += len;
            if (writeAsString) {
               chars[off++] = '"';
            }
         }

         chars[off] = ']';
         this.off = off + 1;
      }
   }

   @Override
   public final void writeDateTime14(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      int off = this.off;
      int minCapacity = off + 16;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] bytes = this.chars;
      bytes[off] = this.quote;
      if (year >= 0 && year <= 9999) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         JDKUtils.UNSAFE.putInt(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 1) << 1), IOUtils.PACKED_DIGITS_UTF16[y01]);
         JDKUtils.UNSAFE.putInt(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 3) << 1), IOUtils.PACKED_DIGITS_UTF16[y23]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 5) << 1), IOUtils.PACKED_DIGITS_UTF16[month]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 7) << 1), IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 9) << 1), IOUtils.PACKED_DIGITS_UTF16[hour]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 11) << 1), IOUtils.PACKED_DIGITS_UTF16[minute]);
         JDKUtils.UNSAFE.putInt(bytes, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 13) << 1), IOUtils.PACKED_DIGITS_UTF16[second]);
         bytes[off + 15] = this.quote;
         this.off = off + 16;
      } else {
         throw illegalYear(year);
      }
   }

   @Override
   public final void writeDateTime19(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      this.ensureCapacity(this.off + 21);
      char[] chars = this.chars;
      int off = this.off;
      chars[off] = this.quote;
      if (year >= 0 && year <= 9999) {
         off = IOUtils.writeLocalDate(chars, off + 1, year, month, dayOfMonth);
         chars[off] = ' ';
         IOUtils.writeLocalTime(chars, off + 1, hour, minute, second);
         chars[off + 9] = this.quote;
         this.off = off + 10;
      } else {
         throw illegalYear(year);
      }
   }

   @Override
   public final void writeLocalDate(LocalDate date) {
      if (date == null) {
         this.writeNull();
      } else if (this.context.dateFormat == null || !this.writeLocalDateWithFormat(date)) {
         int off = this.off;
         int minCapacity = off + 18;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off++] = this.quote;
         off = IOUtils.writeLocalDate(chars, off, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
         chars[off] = this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeLocalDateTime(LocalDateTime dateTime) {
      int off = this.off;
      int minCapacity = off + 38;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off++] = this.quote;
      LocalDate localDate = dateTime.toLocalDate();
      off = IOUtils.writeLocalDate(chars, off, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
      chars[off++] = ' ';
      off = IOUtils.writeLocalTime(chars, off, dateTime.toLocalTime());
      chars[off] = this.quote;
      this.off = off + 1;
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

      int off = this.off;
      int minCapacity = off + 25 + zonelen;
      if (off + minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off] = this.quote;
      off = IOUtils.writeLocalDate(chars, off + 1, year, month, dayOfMonth);
      chars[off] = (char)(timeZone ? 84 : 32);
      IOUtils.writeLocalTime(chars, off + 1, hour, minute, second);
      off += 9;
      if (millis > 0) {
         int div = millis / 10;
         int div2 = div / 10;
         int rem1 = millis - div * 10;
         if (rem1 != 0) {
            IOUtils.putLong(chars, off, IOUtils.DIGITS_K_64[millis] & -65536L | IOUtils.DOT_X0);
            off += 4;
         } else {
            chars[off++] = '.';
            int rem2 = div - div2 * 10;
            if (rem2 != 0) {
               JDKUtils.UNSAFE.putInt(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)off << 1), IOUtils.PACKED_DIGITS_UTF16[div]);
               off += 2;
            } else {
               chars[off++] = (char)((byte)(div2 + 48));
            }
         }
      }

      if (timeZone) {
         int offset = offsetSeconds / 3600;
         if (offsetSeconds == 0) {
            chars[off++] = 'Z';
         } else {
            int offsetAbs = Math.abs(offset);
            chars[off] = (char)(offset >= 0 ? 43 : 45);
            JDKUtils.UNSAFE.putInt(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 1) << 1), IOUtils.PACKED_DIGITS_UTF16[offsetAbs]);
            chars[off + 3] = ':';
            int offsetMinutes = (offsetSeconds - offset * 3600) / 60;
            if (offsetMinutes < 0) {
               offsetMinutes = -offsetMinutes;
            }

            JDKUtils.UNSAFE.putInt(this.chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 4) << 1), IOUtils.PACKED_DIGITS_UTF16[offsetMinutes]);
            off += 6;
         }
      }

      chars[off] = this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeDateYYYMMDD8(int year, int month, int dayOfMonth) {
      int off = this.off;
      int minCapacity = off + 10;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off] = this.quote;
      if (year >= 0 && year <= 9999) {
         int y01 = year / 100;
         int y23 = year - y01 * 100;
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 1) << 1), IOUtils.PACKED_DIGITS_UTF16[y01]);
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 3) << 1), IOUtils.PACKED_DIGITS_UTF16[y23]);
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 5) << 1), IOUtils.PACKED_DIGITS_UTF16[month]);
         JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 7) << 1), IOUtils.PACKED_DIGITS_UTF16[dayOfMonth]);
         chars[off + 9] = this.quote;
         this.off = off + 10;
      } else {
         throw illegalYear(year);
      }
   }

   @Override
   public final void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
      int off = this.off;
      int minCapacity = off + 13;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off++] = this.quote;
      off = IOUtils.writeLocalDate(chars, off, year, month, dayOfMonth);
      chars[off] = this.quote;
      this.off = off + 1;
   }

   @Override
   public final void writeTimeHHMMSS8(int hour, int minute, int second) {
      int off = this.off;
      int minCapacity = off + 10;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off] = (char)((byte)this.quote);
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 1) << 1), IOUtils.PACKED_DIGITS_UTF16[hour]);
      chars[off + 3] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 4) << 1), IOUtils.PACKED_DIGITS_UTF16[minute]);
      chars[off + 6] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)(off + 7) << 1), IOUtils.PACKED_DIGITS_UTF16[second]);
      chars[off + 9] = (char)((byte)this.quote);
      this.off = off + 10;
   }

   @Override
   public final void writeLocalTime(LocalTime time) {
      int off = this.off;
      int minCapacity = off + 20;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      chars[off++] = this.quote;
      off = IOUtils.writeLocalTime(chars, off, time);
      chars[off] = this.quote;
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off] = this.quote;
         LocalDate localDate = dateTime.toLocalDate();
         off = IOUtils.writeLocalDate(chars, off + 1, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
         chars[off] = 'T';
         off = IOUtils.writeLocalTime(chars, off + 1, dateTime.toLocalTime());
         if (zoneSize == 1) {
            chars[off++] = 'Z';
         } else if (firstZoneChar != '+' && firstZoneChar != '-') {
            chars[off++] = '[';
            zoneId.getChars(0, zoneIdLength, chars, off);
            off += zoneIdLength;
            chars[off++] = ']';
         } else {
            zoneId.getChars(0, zoneIdLength, chars, off);
            off += zoneIdLength;
         }

         chars[off] = this.quote;
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
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off] = this.quote;
         LocalDateTime ldt = dateTime.toLocalDateTime();
         LocalDate date = ldt.toLocalDate();
         off = IOUtils.writeLocalDate(chars, off + 1, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
         chars[off] = 'T';
         off = IOUtils.writeLocalTime(chars, off + 1, ldt.toLocalTime());
         ZoneOffset offset = dateTime.getOffset();
         if (offset.getTotalSeconds() == 0) {
            chars[off++] = 'Z';
         } else {
            String zoneId = offset.getId();
            zoneId.getChars(0, zoneId.length(), chars, off);
            off += zoneId.length();
         }

         chars[off] = this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeOffsetTime(OffsetTime time) {
      if (time == null) {
         this.writeNull();
      } else {
         ZoneOffset offset = time.getOffset();
         int off = this.off;
         int minCapacity = off + 25;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         char[] chars = this.chars;
         chars[off] = this.quote;
         off = IOUtils.writeLocalTime(chars, off + 1, time.toLocalTime());
         if (offset.getTotalSeconds() == 0) {
            chars[off++] = 'Z';
         } else {
            String zoneId = offset.getId();
            zoneId.getChars(0, zoneId.length(), chars, off);
            off += zoneId.length();
         }

         chars[off] = this.quote;
         this.off = off + 1;
      }
   }

   @Override
   public final void writeNameRaw(byte[] bytes) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public final int flushTo(OutputStream out) throws IOException {
      if (out == null) {
         throw new JSONException("out is nulll");
      } else {
         boolean ascii = true;

         for (int i = 0; i < this.off; i++) {
            if (this.chars[i] >= 128) {
               ascii = false;
               break;
            }
         }

         if (!ascii) {
            byte[] utf8 = new byte[this.off * 3];
            int utf8Length = IOUtils.encodeUTF8(this.chars, 0, this.off, utf8, 0);
            out.write(utf8, 0, utf8Length);
            this.off = 0;
            return utf8Length;
         } else {
            byte[] bytes = new byte[this.off];

            for (int ix = 0; ix < this.off; ix++) {
               bytes[ix] = (byte)this.chars[ix];
            }

            out.write(bytes);
            this.off = 0;
            return bytes.length;
         }
      }
   }

   @Override
   public final int flushTo(OutputStream out, Charset charset) throws IOException {
      if (this.off == 0) {
         return 0;
      } else if (out == null) {
         throw new JSONException("out is null");
      } else {
         byte[] bytes = this.getBytes(charset);
         out.write(bytes);
         this.off = 0;
         return bytes.length;
      }
   }

   @Override
   public final String toString() {
      return new String(this.chars, 0, this.off);
   }

   @Override
   public final byte[] getBytes() {
      boolean ascii = true;

      for (int i = 0; i < this.off; i++) {
         if (this.chars[i] >= 128) {
            ascii = false;
            break;
         }
      }

      if (!ascii) {
         byte[] utf8 = new byte[this.off * 3];
         int utf8Length = IOUtils.encodeUTF8(this.chars, 0, this.off, utf8, 0);
         return Arrays.copyOf(utf8, utf8Length);
      } else {
         byte[] bytes = new byte[this.off];

         for (int ix = 0; ix < this.off; ix++) {
            bytes[ix] = (byte)this.chars[ix];
         }

         return bytes;
      }
   }

   @Override
   public final int size() {
      return this.off;
   }

   @Override
   public final byte[] getBytes(Charset charset) {
      boolean ascii = true;

      for (int i = 0; i < this.off; i++) {
         if (this.chars[i] >= 128) {
            ascii = false;
            break;
         }
      }

      if (ascii && (charset == StandardCharsets.UTF_8 || charset == StandardCharsets.ISO_8859_1 || charset == StandardCharsets.US_ASCII)) {
         byte[] bytes = new byte[this.off];

         for (int ix = 0; ix < this.off; ix++) {
            bytes[ix] = (byte)this.chars[ix];
         }

         return bytes;
      } else {
         String str = new String(this.chars, 0, this.off);
         if (charset == null) {
            charset = StandardCharsets.UTF_8;
         }

         return str.getBytes(charset);
      }
   }

   @Override
   public final void writeRaw(byte[] bytes) {
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
            if (this.off == this.chars.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.chars[this.off++] = '{';
            boolean first = true;

            for (Entry entry : map.entrySet()) {
               Object value = entry.getValue();
               if (value != null || (this.context.features & JSONWriter.Feature.WriteMapNullValue.mask) != 0L) {
                  if (!first) {
                     if (this.off == this.chars.length) {
                        this.ensureCapacity(this.off + 1);
                     }

                     this.chars[this.off++] = ',';
                  }

                  first = false;
                  Object key = entry.getKey();
                  if (key instanceof String) {
                     this.writeString((String)key);
                  } else {
                     this.writeAny(key);
                  }

                  if (this.off == this.chars.length) {
                     this.ensureCapacity(this.off + 1);
                  }

                  this.chars[this.off++] = ':';
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

            if (this.off == this.chars.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.chars[this.off++] = '}';
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
            if (this.off == this.chars.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.chars[this.off++] = '[';
            boolean first = true;

            for (int i = 0; i < array.size(); i++) {
               Object o = array.get(i);
               if (!first) {
                  if (this.off == this.chars.length) {
                     this.ensureCapacity(this.off + 1);
                  }

                  this.chars[this.off++] = ',';
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

            if (this.off == this.chars.length) {
               this.ensureCapacity(this.off + 1);
            }

            this.chars[this.off++] = ']';
         }
      }
   }

   @Override
   public final void writeString(boolean value) {
      this.chars[this.off++] = this.quote;
      this.writeBool(value);
      this.chars[this.off++] = this.quote;
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
      if (this.off == this.chars.length) {
         this.ensureCapacity(this.off + 1);
      }

      this.chars[this.off++] = this.quote;
   }

   @Override
   public final void writeString(char[] chars) {
      if (chars == null) {
         this.writeStringNull();
      } else {
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean special = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         int i = 0;

         while (i < chars.length) {
            char c = chars[i];
            if (c != '\\' && c != this.quote && c >= ' ') {
               if (!browserSecure || c != '<' && c != '>' && c != '(' && c != ')') {
                  i++;
                  continue;
               }

               special = true;
               break;
            }

            special = true;
            break;
         }

         if (!special) {
            i = this.off + chars.length + 2;
            if (i > this.chars.length) {
               this.ensureCapacity(i);
            }

            this.chars[this.off++] = this.quote;
            System.arraycopy(chars, 0, this.chars, this.off, chars.length);
            this.off += chars.length;
            this.chars[this.off++] = this.quote;
         } else {
            this.writeStringEscape(chars);
         }
      }
   }

   @Override
   public final void writeString(char[] chars, int off, int len) {
      if (chars == null) {
         this.writeStringNull();
      } else {
         boolean special = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;

         for (int i = off; i < len; i++) {
            char ch = chars[i];
            if (ch == '\\' || ch == this.quote || ch < ' ') {
               special = true;
               break;
            }
         }

         if (!special) {
            int minCapacity = this.off + len + 2;
            if (minCapacity >= this.chars.length) {
               this.ensureCapacity(minCapacity);
            }

            this.chars[this.off++] = this.quote;
            System.arraycopy(chars, off, this.chars, this.off, len);
            this.off += len;
            this.chars[this.off++] = this.quote;
         } else {
            this.writeStringEscape(new String(chars, off, len));
         }
      }
   }

   @Override
   public void writeBool(boolean value) {
      int minCapacity = this.off + 5;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      int off = this.off;
      if ((this.context.features & JSONWriter.Feature.WriteBooleanAsNumber.mask) != 0L) {
         chars[off++] = (char)(value ? 49 : 48);
      } else if (!value) {
         chars[off] = 'f';
         chars[off + 1] = 'a';
         chars[off + 2] = 'l';
         chars[off + 3] = 's';
         chars[off + 4] = 'e';
         off += 5;
      } else {
         chars[off] = 't';
         chars[off + 1] = 'r';
         chars[off + 2] = 'u';
         chars[off + 3] = 'e';
         off += 4;
      }

      this.off = off;
   }

   static {
      int[] digits = new int[256];

      for (int i = 0; i < 16; i++) {
         int hi = (short)(i < 10 ? i + 48 : i - 10 + 97);

         for (int j = 0; j < 16; j++) {
            int lo = (short)(j < 10 ? j + 48 : j - 10 + 97);
            digits[(i << 4) + j] = hi | lo << 16;
         }
      }

      if (JDKUtils.BIG_ENDIAN) {
         for (int i = 0; i < digits.length; i++) {
            digits[i] = Integer.reverseBytes(digits[i] << 8);
         }
      }

      HEX256 = digits;
   }
}
