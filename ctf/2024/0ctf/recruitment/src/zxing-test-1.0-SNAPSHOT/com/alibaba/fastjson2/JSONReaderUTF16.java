package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class JSONReaderUTF16 extends JSONReader {
   static final long CHAR_MASK = JDKUtils.BIG_ENDIAN ? 71777214294589695L : -71777214294589696L;
   protected final String str;
   protected final char[] chars;
   protected final int length;
   protected final int start;
   protected final int end;
   private int nameBegin;
   private int nameEnd;
   private int nameLength;
   private int referenceBegin;
   private Closeable input;
   private int cacheIndex = -1;

   JSONReaderUTF16(JSONReader.Context ctx, byte[] bytes, int offset, int length) {
      super(ctx, false, false);
      this.str = null;
      this.chars = new char[length / 2];
      int j = 0;
      int bytesEnd = offset + length;

      for (int i = offset; i < bytesEnd; j++) {
         byte c0 = bytes[i];
         byte c1 = bytes[i + 1];
         this.chars[j] = (char)(c1 & 255 | (c0 & 255) << 8);
         i += 2;
      }

      this.start = offset;
      this.end = this.length = j;
      if (this.offset >= this.end) {
         this.ch = 26;
      } else {
         for (this.ch = this.chars[this.offset]; this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L; this.ch = this.chars[this.offset]) {
            this.offset++;
            if (this.offset >= length) {
               this.ch = 26;
               return;
            }
         }

         while (this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L) {
            this.offset++;
            if (this.offset >= length) {
               this.ch = 26;
               return;
            }

            this.ch = this.chars[this.offset];
         }

         this.offset++;
         if (this.ch == '\ufffe' || this.ch == '\ufeff') {
            this.next();
         }

         if (this.ch == '/') {
            this.skipComment();
         }
      }
   }

   JSONReaderUTF16(JSONReader.Context ctx, Reader input) {
      super(ctx, false, false);
      this.input = input;
      this.cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[this.cacheIndex];
      char[] chars = JSONFactory.CHARS_UPDATER.getAndSet(cacheItem, null);
      if (chars == null) {
         chars = new char[8192];
      }

      int off = 0;

      try {
         while (true) {
            int n = input.read(chars, off, chars.length - off);
            if (n == -1) {
               break;
            }

            off += n;
            if (off == chars.length) {
               int oldCapacity = chars.length;
               int newCapacity = oldCapacity + (oldCapacity >> 1);
               chars = Arrays.copyOf(chars, newCapacity);
            }
         }
      } catch (IOException var9) {
         throw new JSONException("read error", var9);
      }

      this.str = null;
      this.chars = chars;
      this.offset = 0;
      this.length = off;
      this.start = 0;
      this.end = this.length;
      if (this.offset >= this.end) {
         this.ch = 26;
      } else {
         for (this.ch = chars[this.offset]; this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L; this.ch = chars[this.offset]) {
            this.offset++;
            if (this.offset >= this.length) {
               this.ch = 26;
               return;
            }
         }

         this.offset++;
         if (this.ch == '\ufffe' || this.ch == '\ufeff') {
            this.next();
         }

         if (this.ch == '/') {
            this.skipComment();
         }
      }
   }

   JSONReaderUTF16(JSONReader.Context ctx, String str, int offset, int length) {
      super(ctx, false, false);
      this.cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[this.cacheIndex];
      char[] chars = JSONFactory.CHARS_UPDATER.getAndSet(cacheItem, null);
      if (chars == null || chars.length < length) {
         chars = new char[Math.max(length, 8192)];
      }

      str.getChars(offset, offset + length, chars, 0);
      this.str = offset == 0 ? str : null;
      this.chars = chars;
      this.offset = 0;
      this.length = length;
      this.start = 0;
      this.end = this.length;
      if (this.offset >= this.end) {
         this.ch = 26;
      } else {
         for (this.ch = chars[this.offset]; this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L; this.ch = chars[this.offset]) {
            this.offset++;
            if (this.offset >= this.length) {
               this.ch = 26;
               return;
            }
         }

         this.offset++;
         if (this.ch == '\ufffe' || this.ch == '\ufeff') {
            this.next();
         }

         if (this.ch == '/') {
            this.skipComment();
         }
      }
   }

   JSONReaderUTF16(JSONReader.Context ctx, String str, char[] chars, int offset, int length) {
      super(ctx, false, false);
      this.str = str;
      this.chars = chars;
      this.offset = offset;
      this.length = length;
      this.start = offset;
      this.end = offset + length;
      if (this.offset >= this.end) {
         this.ch = 26;
      } else {
         for (this.ch = chars[this.offset]; this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L; this.ch = chars[this.offset]) {
            this.offset++;
            if (this.offset >= length) {
               this.ch = 26;
               return;
            }
         }

         this.offset++;
         if (this.ch == '\ufffe' || this.ch == '\ufeff') {
            this.next();
         }

         if (this.ch == '/') {
            this.skipComment();
         }
      }
   }

   JSONReaderUTF16(JSONReader.Context ctx, InputStream input) {
      super(ctx, false, false);
      this.input = input;
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(cacheItem, null);
      int bufferSize = ctx.bufferSize;
      if (bytes == null) {
         bytes = new byte[bufferSize];
      }

      char[] chars;
      try {
         int off = 0;

         while (true) {
            int n = input.read(bytes, off, bytes.length - off);
            if (n == -1) {
               if (off % 2 == 1) {
                  throw new JSONException("illegal input utf16 bytes, length " + off);
               }

               chars = new char[off / 2];
               n = 0;

               for (int j = 0; n < off; j++) {
                  byte c0 = bytes[n];
                  byte c1 = bytes[n + 1];
                  chars[j] = (char)(c1 & 255 | (c0 & 255) << 8);
                  n += 2;
               }
               break;
            }

            off += n;
            if (off == bytes.length) {
               bytes = Arrays.copyOf(bytes, bytes.length + bufferSize);
            }
         }
      } catch (IOException var16) {
         throw new JSONException("read error", var16);
      } finally {
         JSONFactory.BYTES_UPDATER.lazySet(cacheItem, bytes);
      }

      int var18 = chars.length;
      this.str = null;
      this.chars = chars;
      this.offset = 0;
      this.length = var18;
      this.start = 0;
      this.end = var18;
      if (this.end == 0) {
         this.ch = 26;
      } else {
         int offset = 0;

         char ch;
         for (ch = chars[offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
            if (++offset >= var18) {
               this.ch = 26;
               return;
            }
         }

         this.ch = ch;
         this.offset++;
         if (ch == '\ufffe' || ch == '\ufeff') {
            this.next();
         }

         if (this.ch == '/') {
            this.skipComment();
         }
      }
   }

   @Override
   public final byte[] readHex() {
      char ch = this.ch;
      int offset = this.offset;
      char[] chars = this.chars;
      if (ch == 'x') {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (ch != '\'' && ch != '"') {
         throw syntaxError(offset, ch);
      } else {
         int start = offset;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F') {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch != ch) {
            throw syntaxError(offset, ch);
         } else {
            ch = offset == this.end ? 26 : chars[offset++];
            int len = offset - start - 2;
            if (ch == 26) {
               len++;
            }

            if (len % 2 != 0) {
               throw syntaxError(offset, ch);
            } else {
               byte[] bytes = new byte[len / 2];

               for (int i = 0; i < bytes.length; i++) {
                  char c0 = chars[start + i * 2];
                  char c1 = chars[start + i * 2 + 1];
                  int b0 = c0 - (c0 <= '9' ? 48 : 55);
                  int b1 = c1 - (c1 <= '9' ? 48 : 55);
                  bytes[i] = (byte)(b0 << 4 | b1);
               }

               while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == this.end ? 26 : chars[offset++];
               }

               if (ch == ',' && offset < this.end) {
                  this.comma = true;
                  ch = offset == this.end ? 26 : chars[offset++];

                  while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                     ch = offset == this.end ? 26 : chars[offset++];
                  }

                  this.offset = offset;
                  this.ch = ch;
                  if (this.ch == '/') {
                     this.skipComment();
                  }

                  return bytes;
               } else {
                  this.offset = offset;
                  this.ch = ch;
                  return bytes;
               }
            }
         }
      }
   }

   @Override
   public final boolean isReference() {
      char[] chars = this.chars;
      char ch = this.ch;
      if (ch != '{') {
         return false;
      } else {
         int offset = this.offset;
         int end = this.end;
         if (offset == end) {
            return false;
         } else {
            for (ch = chars[offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
               if (++offset >= end) {
                  return false;
               }
            }

            if (offset + 6 < end
               && chars[offset + 1] == '$'
               && chars[offset + 2] == 'r'
               && chars[offset + 3] == 'e'
               && chars[offset + 4] == 'f'
               && chars[offset + 5] == ch) {
               offset += 6;

               for (ch = chars[offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
                  if (++offset >= end) {
                     return false;
                  }
               }

               if (ch == ':' && offset + 1 < end) {
                  for (ch = chars[++offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
                     if (++offset >= end) {
                        return false;
                     }
                  }

                  if (ch == ch && (offset + 1 >= end || chars[offset + 1] != '#')) {
                     this.referenceBegin = offset;
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   @Override
   public final String readReference() {
      if (this.referenceBegin == this.end) {
         return null;
      } else {
         char[] chars = this.chars;
         this.offset = this.referenceBegin;
         this.ch = chars[this.offset++];
         String reference = this.readString();
         char ch = this.ch;
         int offset = this.offset;

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch != '}') {
            throw new JSONException("illegal reference : ".concat(reference));
         } else {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (this.comma = ch == ',') {
               ch = offset == this.end ? 26 : chars[offset++];

               while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == this.end ? 26 : chars[offset++];
               }
            }

            this.ch = ch;
            this.offset = offset;
            return reference;
         }
      }
   }

   @Override
   public final boolean nextIfMatch(char m) {
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (ch != m) {
         return false;
      } else {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.offset = offset;
         this.ch = ch;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final boolean nextIfComma() {
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (ch != ',') {
         return false;
      } else {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.offset = offset;
         this.ch = ch;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final boolean nextIfArrayStart() {
      char ch = this.ch;
      if (ch != '[') {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.ch = ch;
         this.offset = offset;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final boolean nextIfArrayEnd() {
      char ch = this.ch;
      if (ch != ']') {
         return false;
      } else {
         int offset = this.offset;
         char[] chars = this.chars;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch == ',') {
            this.comma = true;
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final boolean nextIfNullOrEmptyString() {
      char first = this.ch;
      int end = this.end;
      int offset = this.offset;
      char[] chars = this.chars;
      if (first == 'n' && offset + 2 < end && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
         offset += 3;
      } else {
         if (first != '"' && first != '\'' || offset >= end || chars[offset] != first) {
            return false;
         }

         offset++;
      }

      char ch = offset == end ? 26 : chars[offset++];

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == end ? 26 : chars[offset++];
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : chars[offset++];
      }

      this.offset = offset;
      this.ch = ch;
      return true;
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1) {
      if (this.ch != c0) {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         if (offset + 1 <= this.end && chars[offset] == c1) {
            offset++;
            char ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (offset == this.offset + 2 && ch != 26 && ch != '(' && ch != '[' && ch != ']' && ch != ')' && ch != ':' && ch != ',') {
               return false;
            } else {
               this.offset = offset;
               this.ch = ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1, char c2) {
      if (this.ch != c0) {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         if (offset + 2 <= this.end && chars[offset] == c1 && chars[offset + 1] == c2) {
            offset += 2;
            char ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (offset == this.offset + 3 && ch != 26 && ch != '(' && ch != '[' && ch != ']' && ch != ')' && ch != ':' && ch != ',') {
               return false;
            } else {
               this.offset = offset;
               this.ch = ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1, char c2, char c3) {
      if (this.ch != c0) {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         if (offset + 3 <= this.end && chars[offset] == c1 && chars[offset + 1] == c2 && chars[offset + 2] == c3) {
            offset += 3;
            char ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (offset == this.offset + 4 && ch != 26 && ch != '(' && ch != '[' && ch != ']' && ch != ')' && ch != ':' && ch != ',') {
               return false;
            } else {
               this.offset = offset;
               this.ch = ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1, char c2, char c3, char c4) {
      if (this.ch != c0) {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         if (offset + 4 <= this.end && chars[offset] == c1 && chars[offset + 1] == c2 && chars[offset + 2] == c3 && chars[offset + 3] == c4) {
            offset += 4;
            char ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (offset == this.offset + 5 && ch != 26 && ch != '(' && ch != '[' && ch != ']' && ch != ')' && ch != ':' && ch != ',') {
               return false;
            } else {
               this.offset = offset;
               this.ch = ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1, char c2, char c3, char c4, char c5) {
      if (this.ch != c0) {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         if (offset + 5 <= this.end
            && chars[offset] == c1
            && chars[offset + 1] == c2
            && chars[offset + 2] == c3
            && chars[offset + 3] == c4
            && chars[offset + 4] == c5) {
            offset += 5;
            char ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            if (offset == this.offset + 6 && ch != 26 && ch != '(' && ch != '[' && ch != ']' && ch != ')' && ch != ':' && ch != ',') {
               return false;
            } else {
               this.offset = offset;
               this.ch = ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfSet() {
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;
      if (ch == 'S' && offset + 1 < this.end && chars[offset] == 'e' && chars[offset + 1] == 't') {
         offset += 2;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.offset = offset;
         this.ch = ch;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfInfinity() {
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;
      if (ch == 'I'
         && offset + 6 < this.end
         && chars[offset] == 'n'
         && chars[offset + 1] == 'f'
         && chars[offset + 2] == 'i'
         && chars[offset + 3] == 'n'
         && chars[offset + 4] == 'i'
         && chars[offset + 5] == 't'
         && chars[offset + 6] == 'y') {
         offset += 7;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.offset = offset;
         this.ch = ch;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfObjectStart() {
      char ch = this.ch;
      if (ch != '{') {
         return false;
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.ch = ch;
         this.offset = offset;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final boolean nextIfObjectEnd() {
      char ch = this.ch;
      if (ch != '}') {
         return false;
      } else {
         int offset = this.offset;
         char[] chars = this.chars;
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch == ',') {
            this.comma = true;
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         if (ch == '/') {
            this.skipComment();
         }

         return true;
      }
   }

   @Override
   public final void next() {
      int offset = this.offset;
      char[] chars = this.chars;
      char ch = offset >= this.end ? 26 : chars[offset++];

      while (ch == 0 || ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      this.offset = offset;
      this.ch = ch;
      if (ch == '/') {
         this.skipComment();
      }
   }

   @Override
   public final long readFieldNameHashCodeUnquote() {
      this.nameEscape = false;
      int offset = this.offset;
      int end = this.end;
      char[] chars = this.chars;
      char ch = this.ch;
      this.nameBegin = offset - 1;
      char first = ch;
      long nameValue = 0L;

      label151:
      for (int i = 0; offset <= end; i++) {
         switch (ch) {
            case '\b':
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case '\u001a':
            case ' ':
            case '!':
            case '&':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '-':
            case '.':
            case '/':
            case ':':
            case '<':
            case '=':
            case '>':
            case '[':
            case ']':
            case '{':
            case '|':
            case '}':
               this.nameLength = i;
               this.nameEnd = ch == 26 ? offset : offset - 1;
               if (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == end ? 26 : chars[offset++];
               }
               break label151;
            case '\u000b':
            case '\u000e':
            case '\u000f':
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
            case '\u001b':
            case '\u001c':
            case '\u001d':
            case '\u001e':
            case '\u001f':
            case '"':
            case '#':
            case '$':
            case '%':
            case '\'':
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
            case ';':
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
            case '\\':
            case '^':
            case '_':
            case '`':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            default:
               if (ch == '\\') {
                  this.nameEscape = true;
                  ch = chars[offset++];
                  switch (ch) {
                     case '"':
                     case '*':
                     case '+':
                     case '-':
                     case '.':
                     case '/':
                     case ':':
                     case '<':
                     case '=':
                     case '>':
                     case '@':
                     case '\\':
                        break;
                     case 'u':
                        ch = char4(chars[offset], chars[offset + 1], chars[offset + 2], chars[offset + 3]);
                        offset += 4;
                        break;
                     case 'x':
                        ch = char2(chars[offset], chars[offset + 1]);
                        offset += 2;
                        break;
                     default:
                        ch = this.char1(ch);
                  }
               }

               if (ch > 255 || i >= 8 || i == 0 && ch == 0) {
                  nameValue = 0L;
                  ch = first;
                  offset = this.nameBegin + 1;
                  break label151;
               }

               byte c = (byte)ch;
               switch (i) {
                  case 0:
                     nameValue = (long)c;
                     break;
                  case 1:
                     nameValue = (long)(c << 8) + (nameValue & 255L);
                     break;
                  case 2:
                     nameValue = (long)(c << 16) + (nameValue & 65535L);
                     break;
                  case 3:
                     nameValue = (long)(c << 24) + (nameValue & 16777215L);
                     break;
                  case 4:
                     nameValue = ((long)c << 32) + (nameValue & 4294967295L);
                     break;
                  case 5:
                     nameValue = ((long)c << 40) + (nameValue & 1099511627775L);
                     break;
                  case 6:
                     nameValue = ((long)c << 48) + (nameValue & 281474976710655L);
                     break;
                  case 7:
                     nameValue = ((long)c << 56) + (nameValue & 72057594037927935L);
               }

               ch = offset == end ? 26 : chars[offset++];
         }
      }

      long hashCode;
      if (nameValue != 0L) {
         hashCode = nameValue;
      } else {
         hashCode = -3750763034362895579L;
         int i = 0;

         label131:
         while (true) {
            if (ch == '\\') {
               this.nameEscape = true;
               ch = chars[offset++];
               switch (ch) {
                  case '"':
                  case '*':
                  case '+':
                  case '-':
                  case '.':
                  case '/':
                  case ':':
                  case '<':
                  case '=':
                  case '>':
                  case '@':
                  case '\\':
                     break;
                  case 'u':
                     ch = char4(chars[offset], chars[offset + 1], chars[offset + 2], chars[offset + 3]);
                     offset += 4;
                     break;
                  case 'x':
                     ch = char2(chars[offset], chars[offset + 1]);
                     offset += 2;
                     break;
                  default:
                     ch = this.char1(ch);
               }

               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
               ch = offset == end ? 26 : chars[offset++];
            } else {
               switch (ch) {
                  case '\b':
                  case '\t':
                  case '\n':
                  case '\f':
                  case '\r':
                  case '\u001a':
                  case ' ':
                  case '!':
                  case '(':
                  case ')':
                  case '*':
                  case '+':
                  case ',':
                  case '-':
                  case '.':
                  case '/':
                  case ':':
                  case '<':
                  case '=':
                  case '>':
                  case '[':
                  case ']':
                  case '{':
                  case '}':
                     this.nameLength = i;
                     this.nameEnd = ch == 26 ? offset : offset - 1;

                     while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                        ch = offset == end ? 26 : chars[offset++];
                     }
                     break label131;
                  default:
                     hashCode ^= (long)ch;
                     hashCode *= 1099511628211L;
                     ch = offset == end ? 26 : chars[offset++];
               }
            }

            i++;
         }
      }

      if (ch == ':') {
         ch = offset == end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : chars[offset++];
         }
      }

      this.offset = offset;
      this.ch = ch;
      return hashCode;
   }

   @Override
   public final long readFieldNameHashCode() {
      char[] chars = this.chars;
      if (this.ch == '\'' && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else if (this.ch != '"' && this.ch != '\'') {
         if ((this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) != 0L && isFirstIdentifier(this.ch)) {
            return this.readFieldNameHashCodeUnquote();
         } else if (this.ch != '}' && !this.isNull()) {
            String errorMsg;
            if (this.ch == '[' && this.nameBegin > 0) {
               errorMsg = "illegal fieldName input " + this.ch + ", previous fieldName " + this.getFieldName();
            } else {
               errorMsg = "illegal fieldName input" + this.ch;
            }

            throw new JSONException(this.info(errorMsg));
         } else {
            return -1L;
         }
      } else {
         char quote = this.ch;
         this.stringValue = null;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         long nameValue = 0L;
         if (offset + 9 < end) {
            char c0 = chars[offset];
            char c1 = chars[offset + 1];
            char c2 = chars[offset + 2];
            char c3 = chars[offset + 3];
            char c4 = chars[offset + 4];
            char c5 = chars[offset + 5];
            char c6 = chars[offset + 6];
            char c7 = chars[offset + 7];
            char c8 = chars[offset + 8];
            if (c0 == quote) {
               nameValue = 0L;
            } else if (c1 == quote && c0 != 0 && c0 != '\\' && c0 <= 255) {
               nameValue = (long)((byte)c0);
               this.nameLength = 1;
               this.nameEnd = offset + 1;
               offset += 2;
            } else if (c2 == quote && c0 != 0 && c0 != '\\' && c1 != '\\' && c0 <= 255 && c1 <= 255) {
               nameValue = (long)(((byte)c1 << 8) + c0);
               this.nameLength = 2;
               this.nameEnd = offset + 2;
               offset += 3;
            } else if (c3 == quote && c0 != 0 && c0 != '\\' && c1 != '\\' && c2 != '\\' && c0 <= 255 && c1 <= 255 && c2 <= 255) {
               nameValue = (long)(((byte)c2 << 16) + (c1 << '\b') + c0);
               this.nameLength = 3;
               this.nameEnd = offset + 3;
               offset += 4;
            } else if (c4 == quote && c0 != 0 && c0 != '\\' && c1 != '\\' && c2 != '\\' && c3 != '\\' && c0 <= 255 && c1 <= 255 && c2 <= 255 && c3 <= 255) {
               nameValue = (long)(((byte)c3 << 24) + (c2 << 16) + (c1 << '\b') + c0);
               this.nameLength = 4;
               this.nameEnd = offset + 4;
               offset += 5;
            } else if (c5 == quote
               && c0 != 0
               && c0 != '\\'
               && c1 != '\\'
               && c2 != '\\'
               && c3 != '\\'
               && c4 != '\\'
               && c0 <= 255
               && c1 <= 255
               && c2 <= 255
               && c3 <= 255
               && c4 <= 255) {
               nameValue = ((long)((byte)c4) << 32) + ((long)c3 << 24) + ((long)c2 << 16) + ((long)c1 << 8) + (long)c0;
               this.nameLength = 5;
               this.nameEnd = offset + 5;
               offset += 6;
            } else if (c6 == quote
               && c0 != 0
               && c0 != '\\'
               && c1 != '\\'
               && c2 != '\\'
               && c3 != '\\'
               && c4 != '\\'
               && c5 != '\\'
               && c0 <= 255
               && c1 <= 255
               && c2 <= 255
               && c3 <= 255
               && c4 <= 255
               && c5 <= 255) {
               nameValue = ((long)((byte)c5) << 40) + ((long)c4 << 32) + ((long)c3 << 24) + ((long)c2 << 16) + ((long)c1 << 8) + (long)c0;
               this.nameLength = 6;
               this.nameEnd = offset + 6;
               offset += 7;
            } else if (c7 == quote
               && c0 != 0
               && c0 != '\\'
               && c1 != '\\'
               && c2 != '\\'
               && c3 != '\\'
               && c4 != '\\'
               && c5 != '\\'
               && c6 != '\\'
               && c0 <= 255
               && c1 <= 255
               && c2 <= 255
               && c3 <= 255
               && c4 <= 255
               && c5 <= 255
               && c6 <= 255) {
               nameValue = ((long)((byte)c6) << 48) + ((long)c5 << 40) + ((long)c4 << 32) + ((long)c3 << 24) + ((long)c2 << 16) + ((long)c1 << 8) + (long)c0;
               this.nameLength = 7;
               this.nameEnd = offset + 7;
               offset += 8;
            } else if (c8 == quote
               && c0 != 0
               && c0 != '\\'
               && c1 != '\\'
               && c2 != '\\'
               && c3 != '\\'
               && c4 != '\\'
               && c5 != '\\'
               && c6 != '\\'
               && c7 != '\\'
               && c0 <= 255
               && c1 <= 255
               && c2 <= 255
               && c3 <= 255
               && c4 <= 255
               && c5 <= 255
               && c6 <= 255
               && c7 <= 255) {
               nameValue = ((long)((byte)c7) << 56)
                  + ((long)c6 << 48)
                  + ((long)c5 << 40)
                  + ((long)c4 << 32)
                  + ((long)c3 << 24)
                  + ((long)c2 << 16)
                  + ((long)c1 << 8)
                  + (long)c0;
               this.nameLength = 8;
               this.nameEnd = offset + 8;
               offset += 9;
            }
         }

         if (nameValue == 0L) {
            for (int i = 0; offset < end; i++) {
               char c = chars[offset];
               if (c == quote) {
                  if (i == 0) {
                     offset = this.nameBegin;
                  } else {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                  }
                  break;
               }

               if (c == '\\') {
                  this.nameEscape = true;
                  c = chars[++offset];
                  switch (c) {
                     case '"':
                     case '\\':
                     default:
                        c = this.char1(c);
                        break;
                     case 'u':
                        c = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        c = char2(chars[offset + 1], chars[offset + 2]);
                        offset += 2;
                  }
               }

               if (c > 255 || i >= 8 || i == 0 && c == 0) {
                  nameValue = 0L;
                  offset = this.nameBegin;
                  break;
               }

               switch (i) {
                  case 0:
                     nameValue = (long)((byte)c);
                     break;
                  case 1:
                     nameValue = (long)((byte)c << 8) + (nameValue & 255L);
                     break;
                  case 2:
                     nameValue = (long)((byte)c << 16) + (nameValue & 65535L);
                     break;
                  case 3:
                     nameValue = (long)((byte)c << 24) + (nameValue & 16777215L);
                     break;
                  case 4:
                     nameValue = ((long)((byte)c) << 32) + (nameValue & 4294967295L);
                     break;
                  case 5:
                     nameValue = ((long)((byte)c) << 40) + (nameValue & 1099511627775L);
                     break;
                  case 6:
                     nameValue = ((long)((byte)c) << 48) + (nameValue & 281474976710655L);
                     break;
                  case 7:
                     nameValue = ((long)((byte)c) << 56) + (nameValue & 72057594037927935L);
               }

               offset++;
            }
         }

         long hashCode;
         if (nameValue != 0L) {
            hashCode = nameValue;
         } else {
            hashCode = -3750763034362895579L;
            int i = 0;

            while (true) {
               char cx = chars[offset];
               if (cx == '\\') {
                  this.nameEscape = true;
                  cx = chars[++offset];
                  switch (cx) {
                     case '"':
                     case '\\':
                     default:
                        cx = this.char1(cx);
                        break;
                     case 'u':
                        cx = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        cx = char2(chars[offset + 1], chars[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)cx;
                  hashCode *= 1099511628211L;
               } else {
                  if (cx == quote) {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                     break;
                  }

                  offset++;
                  hashCode ^= (long)cx;
                  hashCode *= 1099511628211L;
               }

               i++;
            }
         }

         char cx = offset == end ? 26 : chars[offset++];

         while (cx <= ' ' && (1L << cx & 4294981376L) != 0L) {
            cx = offset == end ? 26 : chars[offset++];
         }

         if (cx != ':') {
            throw new JSONException(this.info("expect ':', but " + cx));
         } else {
            cx = offset == end ? 26 : chars[offset++];

            while (cx <= ' ' && (1L << cx & 4294981376L) != 0L) {
               cx = offset == end ? 26 : chars[offset++];
            }

            this.offset = offset;
            this.ch = cx;
            return hashCode;
         }
      }
   }

   @Override
   public final long readValueHashCode() {
      char quote = this.ch;
      if (quote != '"' && quote != '\'') {
         return -1L;
      } else {
         char[] chars = this.chars;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         long nameValue = 0L;

         for (int i = 0; offset < end; i++) {
            char ch = chars[offset];
            if (ch == quote) {
               if (i == 0) {
                  nameValue = 0L;
                  offset = this.nameBegin;
               } else {
                  this.nameLength = i;
                  this.nameEnd = offset++;
               }
               break;
            }

            if (ch == '\\') {
               this.nameEscape = true;
               ch = chars[++offset];
               switch (ch) {
                  case '"':
                  case '\\':
                  default:
                     ch = this.char1(ch);
                     break;
                  case 'u':
                     ch = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                     offset += 4;
                     break;
                  case 'x':
                     ch = char2(chars[offset + 1], chars[offset + 2]);
                     offset += 2;
               }
            }

            if (ch > 255 || i >= 8 || i == 0 && ch == 0) {
               nameValue = 0L;
               offset = this.nameBegin;
               break;
            }

            switch (i) {
               case 0:
                  nameValue = (long)((byte)ch);
                  break;
               case 1:
                  nameValue = (long)((byte)ch << 8) + (nameValue & 255L);
                  break;
               case 2:
                  nameValue = (long)((byte)ch << 16) + (nameValue & 65535L);
                  break;
               case 3:
                  nameValue = (long)((byte)ch << 24) + (nameValue & 16777215L);
                  break;
               case 4:
                  nameValue = ((long)((byte)ch) << 32) + (nameValue & 4294967295L);
                  break;
               case 5:
                  nameValue = ((long)((byte)ch) << 40) + (nameValue & 1099511627775L);
                  break;
               case 6:
                  nameValue = ((long)((byte)ch) << 48) + (nameValue & 281474976710655L);
                  break;
               case 7:
                  nameValue = ((long)((byte)ch) << 56) + (nameValue & 72057594037927935L);
            }

            offset++;
         }

         long hashCode;
         if (nameValue != 0L) {
            hashCode = nameValue;
         } else {
            hashCode = -3750763034362895579L;
            int i = 0;

            while (true) {
               char c = chars[offset];
               if (c == '\\') {
                  this.nameEscape = true;
                  c = chars[++offset];
                  switch (c) {
                     case '"':
                     case '\\':
                     default:
                        c = this.char1(c);
                        break;
                     case 'u':
                        c = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        c = char2(chars[offset + 1], chars[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)c;
                  hashCode *= 1099511628211L;
               } else {
                  if (c == '"') {
                     this.nameLength = i;
                     this.nameEnd = offset;
                     this.stringValue = null;
                     offset++;
                     break;
                  }

                  offset++;
                  hashCode ^= (long)c;
                  hashCode *= 1099511628211L;
               }

               i++;
            }
         }

         char c;
         if (offset == end) {
            c = 26;
         } else {
            c = chars[offset];
         }

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[++offset];
         }

         if (this.comma = c == ',') {
            if (++offset == end) {
               c = 26;
            } else {
               c = chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }
         }

         this.offset = offset + 1;
         this.ch = c;
         return hashCode;
      }
   }

   @Override
   public final long getNameHashCodeLCase() {
      int offset = this.nameBegin;
      long nameValue = 0L;
      char[] chars = this.chars;

      for (int i = 0; offset < this.end; offset++) {
         char c = chars[offset];
         if (c == '\\') {
            c = chars[++offset];
            switch (c) {
               case '"':
               case '\\':
               default:
                  c = this.char1(c);
                  break;
               case 'u':
                  c = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                  offset += 4;
                  break;
               case 'x':
                  c = char2(chars[offset + 1], chars[offset + 2]);
                  offset += 2;
            }
         } else if (c == '"') {
            break;
         }

         if (c > 255 || i >= 8 || i == 0 && c == 0) {
            nameValue = 0L;
            offset = this.nameBegin;
            break;
         }

         if (c == '_' || c == '-' || c == ' ') {
            char c1 = chars[offset + 1];
            if (c1 != '"' && c1 != '\'' && c1 != c) {
               continue;
            }
         }

         if (c >= 'A' && c <= 'Z') {
            c = (char)(c + ' ');
         }

         switch (i) {
            case 0:
               nameValue = (long)((byte)c);
               break;
            case 1:
               nameValue = (long)((byte)c << 8) + (nameValue & 255L);
               break;
            case 2:
               nameValue = (long)((byte)c << 16) + (nameValue & 65535L);
               break;
            case 3:
               nameValue = (long)((byte)c << 24) + (nameValue & 16777215L);
               break;
            case 4:
               nameValue = ((long)((byte)c) << 32) + (nameValue & 4294967295L);
               break;
            case 5:
               nameValue = ((long)((byte)c) << 40) + (nameValue & 1099511627775L);
               break;
            case 6:
               nameValue = ((long)((byte)c) << 48) + (nameValue & 281474976710655L);
               break;
            case 7:
               nameValue = ((long)((byte)c) << 56) + (nameValue & 72057594037927935L);
         }

         i++;
      }

      if (nameValue != 0L) {
         return nameValue;
      } else {
         long hashCode = -3750763034362895579L;

         while (offset < this.end) {
            char cx = chars[offset];
            if (cx == '\\') {
               cx = chars[++offset];
               switch (cx) {
                  case '"':
                  case '\\':
                  default:
                     cx = this.char1(cx);
                     break;
                  case 'u':
                     cx = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                     offset += 4;
                     break;
                  case 'x':
                     cx = char2(chars[offset + 1], chars[offset + 2]);
                     offset += 2;
               }
            } else if (cx == '"') {
               break;
            }

            offset++;
            if (cx == '_' || cx == '-' || cx == ' ') {
               char c1 = chars[offset];
               if (c1 != '"' && c1 != '\'' && c1 != cx) {
                  continue;
               }
            }

            if (cx >= 'A' && cx <= 'Z') {
               cx = (char)(cx + ' ');
            }

            hashCode ^= (long)cx;
            hashCode *= 1099511628211L;
         }

         return hashCode;
      }
   }

   @Override
   public final String getFieldName() {
      if (!this.nameEscape) {
         return this.str != null ? this.str.substring(this.nameBegin, this.nameEnd) : new String(this.chars, this.nameBegin, this.nameEnd - this.nameBegin);
      } else {
         char[] buf = new char[this.nameLength];
         char[] chars = this.chars;
         int offset = this.nameBegin;

         for (int i = 0; offset < this.nameEnd; i++) {
            char c = chars[offset];
            if (c == '\\') {
               c = chars[++offset];
               switch (c) {
                  case '"':
                  case '*':
                  case '+':
                  case ',':
                  case '-':
                  case '.':
                  case '/':
                  case ':':
                  case '<':
                  case '=':
                  case '>':
                  case '@':
                  case '\\':
                     break;
                  case 'u':
                     c = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                     offset += 4;
                     break;
                  case 'x':
                     c = char2(chars[offset + 1], chars[offset + 2]);
                     offset += 2;
                     break;
                  default:
                     c = this.char1(c);
               }
            } else if (c == '"') {
               break;
            }

            buf[i] = c;
            offset++;
         }

         return new String(buf);
      }
   }

   @Override
   public final String readFieldName() {
      char quote = this.ch;
      if (quote == '\'' && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else if (quote != '"' && quote != '\'') {
         return (this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) != 0L && isFirstIdentifier(quote)
            ? this.readFieldNameUnquote()
            : null;
      } else {
         char[] chars = this.chars;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         int nameBegin = this.nameBegin;

         for (int i = 0; offset < end; i++) {
            int c = chars[offset];
            if (c == 92) {
               this.nameEscape = true;
               int var33 = chars[offset + 1];
               offset += var33 == 'u' ? 6 : (var33 == 'x' ? 4 : 2);
            } else {
               if (c == quote) {
                  this.nameLength = i;
                  this.nameEnd = offset++;
                  char var34;
                  if (offset < end) {
                     var34 = chars[offset];
                  } else {
                     var34 = '\u001a';
                  }

                  while (var34 <= ' ' && (1L << var34 & 4294981376L) != 0L) {
                     var34 = chars[++offset];
                  }

                  if (var34 != ':') {
                     throw new JSONException("syntax error : " + offset);
                  }

                  offset++;
                  var34 = offset == end ? 26 : chars[offset];

                  while (var34 <= ' ' && (1L << var34 & 4294981376L) != 0L) {
                     var34 = chars[++offset];
                  }

                  this.offset = offset + 1;
                  this.ch = (char)var34;
                  break;
               }

               offset++;
            }
         }

         if (this.nameEnd < nameBegin) {
            throw new JSONException("syntax error : " + offset);
         } else if (!this.nameEscape) {
            long nameValue0 = -1L;
            long nameValue1 = -1L;
            switch (this.nameLength) {
               case 1:
                  return TypeUtils.toString(chars[nameBegin]);
               case 2:
                  return TypeUtils.toString(chars[nameBegin], chars[nameBegin + 1]);
               case 3:
                  int c0xxxxxxxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxxxxxxx = chars[nameBegin + 2];
                  if ((c0xxxxxxxxxxxxx & 0xFF) == c0xxxxxxxxxxxxx && (c1xxxxxxxxxxxxx & 0xFF) == c1xxxxxxxxxxxxx && (c2xxxxxxxxxxxxx & 0xFF) == c2xxxxxxxxxxxxx
                     )
                   {
                     nameValue0 = (long)((c2xxxxxxxxxxxxx << 16) + (c1xxxxxxxxxxxxx << 8) + c0xxxxxxxxxxxxx);
                  }
                  break;
               case 4:
                  int c0xxxxxxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxxxxxxx = chars[nameBegin + 3];
                  if ((c0xxxxxxxxxxxx & 0xFF) == c0xxxxxxxxxxxx
                     && (c1xxxxxxxxxxxx & 0xFF) == c1xxxxxxxxxxxx
                     && (c2xxxxxxxxxxxx & 0xFF) == c2xxxxxxxxxxxx
                     && (c3xxxxxxxxxxxx & 0xFF) == c3xxxxxxxxxxxx) {
                     nameValue0 = (long)((c3xxxxxxxxxxxx << 24) + (c2xxxxxxxxxxxx << 16) + (c1xxxxxxxxxxxx << 8) + c0xxxxxxxxxxxx);
                  }
                  break;
               case 5:
                  int c0xxxxxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxxxxxx = chars[nameBegin + 3];
                  int c4xxxxxxxxxxx = chars[nameBegin + 4];
                  if ((c0xxxxxxxxxxx & 0xFF) == c0xxxxxxxxxxx
                     && (c1xxxxxxxxxxx & 0xFF) == c1xxxxxxxxxxx
                     && (c2xxxxxxxxxxx & 0xFF) == c2xxxxxxxxxxx
                     && (c3xxxxxxxxxxx & 0xFF) == c3xxxxxxxxxxx
                     && (c4xxxxxxxxxxx & 0xFF) == c4xxxxxxxxxxx) {
                     nameValue0 = ((long)c4xxxxxxxxxxx << 32)
                        + ((long)c3xxxxxxxxxxx << 24)
                        + ((long)c2xxxxxxxxxxx << 16)
                        + ((long)c1xxxxxxxxxxx << 8)
                        + (long)c0xxxxxxxxxxx;
                  }
                  break;
               case 6:
                  int c0xxxxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxxxxx = chars[nameBegin + 3];
                  int c4xxxxxxxxxx = chars[nameBegin + 4];
                  int c5xxxxxxxxxx = chars[nameBegin + 5];
                  if ((c0xxxxxxxxxx & 0xFF) == c0xxxxxxxxxx
                     && (c1xxxxxxxxxx & 0xFF) == c1xxxxxxxxxx
                     && (c2xxxxxxxxxx & 0xFF) == c2xxxxxxxxxx
                     && (c3xxxxxxxxxx & 0xFF) == c3xxxxxxxxxx
                     && (c4xxxxxxxxxx & 0xFF) == c4xxxxxxxxxx
                     && (c5xxxxxxxxxx & 0xFF) == c5xxxxxxxxxx) {
                     nameValue0 = ((long)c5xxxxxxxxxx << 40)
                        + ((long)c4xxxxxxxxxx << 32)
                        + ((long)c3xxxxxxxxxx << 24)
                        + ((long)c2xxxxxxxxxx << 16)
                        + ((long)c1xxxxxxxxxx << 8)
                        + (long)c0xxxxxxxxxx;
                  }
                  break;
               case 7:
                  int c0xxxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxxxx = chars[nameBegin + 3];
                  int c4xxxxxxxxx = chars[nameBegin + 4];
                  int c5xxxxxxxxx = chars[nameBegin + 5];
                  int c6xxxxxxxxx = chars[nameBegin + 6];
                  if ((c0xxxxxxxxx & 0xFF) == c0xxxxxxxxx
                     && (c1xxxxxxxxx & 0xFF) == c1xxxxxxxxx
                     && (c2xxxxxxxxx & 0xFF) == c2xxxxxxxxx
                     && (c3xxxxxxxxx & 0xFF) == c3xxxxxxxxx
                     && (c4xxxxxxxxx & 0xFF) == c4xxxxxxxxx
                     && (c5xxxxxxxxx & 0xFF) == c5xxxxxxxxx
                     && (c6xxxxxxxxx & 0xFF) == c6xxxxxxxxx) {
                     nameValue0 = ((long)c6xxxxxxxxx << 48)
                        + ((long)c5xxxxxxxxx << 40)
                        + ((long)c4xxxxxxxxx << 32)
                        + ((long)c3xxxxxxxxx << 24)
                        + ((long)c2xxxxxxxxx << 16)
                        + ((long)c1xxxxxxxxx << 8)
                        + (long)c0xxxxxxxxx;
                  }
                  break;
               case 8:
                  int c0xxxxxxxx = chars[nameBegin];
                  int c1xxxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxxx = chars[nameBegin + 3];
                  int c4xxxxxxxx = chars[nameBegin + 4];
                  int c5xxxxxxxx = chars[nameBegin + 5];
                  int c6xxxxxxxx = chars[nameBegin + 6];
                  int c7xxxxxxxx = chars[nameBegin + 7];
                  if ((c0xxxxxxxx & 0xFF) == c0xxxxxxxx
                     && (c1xxxxxxxx & 0xFF) == c1xxxxxxxx
                     && (c2xxxxxxxx & 0xFF) == c2xxxxxxxx
                     && (c3xxxxxxxx & 0xFF) == c3xxxxxxxx
                     && (c4xxxxxxxx & 0xFF) == c4xxxxxxxx
                     && (c5xxxxxxxx & 0xFF) == c5xxxxxxxx
                     && (c6xxxxxxxx & 0xFF) == c6xxxxxxxx
                     && (c7xxxxxxxx & 0xFF) == c7xxxxxxxx) {
                     nameValue0 = ((long)c7xxxxxxxx << 56)
                        + ((long)c6xxxxxxxx << 48)
                        + ((long)c5xxxxxxxx << 40)
                        + ((long)c4xxxxxxxx << 32)
                        + ((long)c3xxxxxxxx << 24)
                        + ((long)c2xxxxxxxx << 16)
                        + ((long)c1xxxxxxxx << 8)
                        + (long)c0xxxxxxxx;
                  }
                  break;
               case 9:
                  int c0xxxxxxx = chars[nameBegin];
                  int c1xxxxxxx = chars[nameBegin + 1];
                  int c2xxxxxxx = chars[nameBegin + 2];
                  int c3xxxxxxx = chars[nameBegin + 3];
                  int c4xxxxxxx = chars[nameBegin + 4];
                  int c5xxxxxxx = chars[nameBegin + 5];
                  int c6xxxxxxx = chars[nameBegin + 6];
                  int c7xxxxxxx = chars[nameBegin + 7];
                  int c8xxxxxxx = chars[nameBegin + 8];
                  if ((c0xxxxxxx & 0xFF) == c0xxxxxxx
                     && (c1xxxxxxx & 0xFF) == c1xxxxxxx
                     && (c2xxxxxxx & 0xFF) == c2xxxxxxx
                     && (c3xxxxxxx & 0xFF) == c3xxxxxxx
                     && (c4xxxxxxx & 0xFF) == c4xxxxxxx
                     && (c5xxxxxxx & 0xFF) == c5xxxxxxx
                     && (c6xxxxxxx & 0xFF) == c6xxxxxxx
                     && (c7xxxxxxx & 0xFF) == c7xxxxxxx
                     && (c8xxxxxxx & 0xFF) == c8xxxxxxx) {
                     nameValue0 = (long)c0xxxxxxx;
                     nameValue1 = ((long)c8xxxxxxx << 56)
                        + ((long)c7xxxxxxx << 48)
                        + ((long)c6xxxxxxx << 40)
                        + ((long)c5xxxxxxx << 32)
                        + ((long)c4xxxxxxx << 24)
                        + ((long)c3xxxxxxx << 16)
                        + ((long)c2xxxxxxx << 8)
                        + (long)c1xxxxxxx;
                  }
                  break;
               case 10:
                  int c0xxxxxx = chars[nameBegin];
                  int c1xxxxxx = chars[nameBegin + 1];
                  int c2xxxxxx = chars[nameBegin + 2];
                  int c3xxxxxx = chars[nameBegin + 3];
                  int c4xxxxxx = chars[nameBegin + 4];
                  int c5xxxxxx = chars[nameBegin + 5];
                  int c6xxxxxx = chars[nameBegin + 6];
                  int c7xxxxxx = chars[nameBegin + 7];
                  int c8xxxxxx = chars[nameBegin + 8];
                  int c9xxxxxx = chars[nameBegin + 9];
                  if ((c0xxxxxx & 0xFF) == c0xxxxxx
                     && (c1xxxxxx & 0xFF) == c1xxxxxx
                     && (c2xxxxxx & 0xFF) == c2xxxxxx
                     && (c3xxxxxx & 0xFF) == c3xxxxxx
                     && (c4xxxxxx & 0xFF) == c4xxxxxx
                     && (c5xxxxxx & 0xFF) == c5xxxxxx
                     && (c6xxxxxx & 0xFF) == c6xxxxxx
                     && (c7xxxxxx & 0xFF) == c7xxxxxx
                     && (c8xxxxxx & 0xFF) == c8xxxxxx
                     && (c9xxxxxx & 0xFF) == c9xxxxxx) {
                     nameValue0 = (long)((c1xxxxxx << 8) + c0xxxxxx);
                     nameValue1 = ((long)c9xxxxxx << 56)
                        + ((long)c8xxxxxx << 48)
                        + ((long)c7xxxxxx << 40)
                        + ((long)c6xxxxxx << 32)
                        + ((long)c5xxxxxx << 24)
                        + ((long)c4xxxxxx << 16)
                        + ((long)c3xxxxxx << 8)
                        + (long)c2xxxxxx;
                  }
                  break;
               case 11:
                  int c0xxxxx = chars[nameBegin];
                  int c1xxxxx = chars[nameBegin + 1];
                  int c2xxxxx = chars[nameBegin + 2];
                  int c3xxxxx = chars[nameBegin + 3];
                  int c4xxxxx = chars[nameBegin + 4];
                  int c5xxxxx = chars[nameBegin + 5];
                  int c6xxxxx = chars[nameBegin + 6];
                  int c7xxxxx = chars[nameBegin + 7];
                  int c8xxxxx = chars[nameBegin + 8];
                  int c9xxxxx = chars[nameBegin + 9];
                  int c10xxxxx = chars[nameBegin + 10];
                  if ((c0xxxxx & 0xFF) == c0xxxxx
                     && (c1xxxxx & 0xFF) == c1xxxxx
                     && (c2xxxxx & 0xFF) == c2xxxxx
                     && (c3xxxxx & 0xFF) == c3xxxxx
                     && (c4xxxxx & 0xFF) == c4xxxxx
                     && (c5xxxxx & 0xFF) == c5xxxxx
                     && (c6xxxxx & 0xFF) == c6xxxxx
                     && (c7xxxxx & 0xFF) == c7xxxxx
                     && (c8xxxxx & 0xFF) == c8xxxxx
                     && (c9xxxxx & 0xFF) == c9xxxxx
                     && (c10xxxxx & 0xFF) == c10xxxxx) {
                     nameValue0 = (long)((c2xxxxx << 16) + (c1xxxxx << 8) + c0xxxxx);
                     nameValue1 = ((long)c10xxxxx << 56)
                        + ((long)c9xxxxx << 48)
                        + ((long)c8xxxxx << 40)
                        + ((long)c7xxxxx << 32)
                        + ((long)c6xxxxx << 24)
                        + ((long)c5xxxxx << 16)
                        + ((long)c4xxxxx << 8)
                        + (long)c3xxxxx;
                  }
                  break;
               case 12:
                  int c0xxxx = chars[nameBegin];
                  int c1xxxx = chars[nameBegin + 1];
                  int c2xxxx = chars[nameBegin + 2];
                  int c3xxxx = chars[nameBegin + 3];
                  int c4xxxx = chars[nameBegin + 4];
                  int c5xxxx = chars[nameBegin + 5];
                  int c6xxxx = chars[nameBegin + 6];
                  int c7xxxx = chars[nameBegin + 7];
                  int c8xxxx = chars[nameBegin + 8];
                  int c9xxxx = chars[nameBegin + 9];
                  int c10xxxx = chars[nameBegin + 10];
                  int c11xxxx = chars[nameBegin + 11];
                  if ((c0xxxx & 0xFF) == c0xxxx
                     && (c1xxxx & 0xFF) == c1xxxx
                     && (c2xxxx & 0xFF) == c2xxxx
                     && (c3xxxx & 0xFF) == c3xxxx
                     && (c4xxxx & 0xFF) == c4xxxx
                     && (c5xxxx & 0xFF) == c5xxxx
                     && (c6xxxx & 0xFF) == c6xxxx
                     && (c7xxxx & 0xFF) == c7xxxx
                     && (c8xxxx & 0xFF) == c8xxxx
                     && (c9xxxx & 0xFF) == c9xxxx
                     && (c10xxxx & 0xFF) == c10xxxx
                     && (c11xxxx & 0xFF) == c11xxxx) {
                     nameValue0 = (long)((c3xxxx << 24) + (c2xxxx << 16) + (c1xxxx << 8) + c0xxxx);
                     nameValue1 = ((long)c11xxxx << 56)
                        + ((long)c10xxxx << 48)
                        + ((long)c9xxxx << 40)
                        + ((long)c8xxxx << 32)
                        + ((long)c7xxxx << 24)
                        + ((long)c6xxxx << 16)
                        + ((long)c5xxxx << 8)
                        + (long)c4xxxx;
                  }
                  break;
               case 13:
                  int c0xxx = chars[nameBegin];
                  int c1xxx = chars[nameBegin + 1];
                  int c2xxx = chars[nameBegin + 2];
                  int c3xxx = chars[nameBegin + 3];
                  int c4xxx = chars[nameBegin + 4];
                  int c5xxx = chars[nameBegin + 5];
                  int c6xxx = chars[nameBegin + 6];
                  int c7xxx = chars[nameBegin + 7];
                  int c8xxx = chars[nameBegin + 8];
                  int c9xxx = chars[nameBegin + 9];
                  int c10xxx = chars[nameBegin + 10];
                  int c11xxx = chars[nameBegin + 11];
                  int c12xxx = chars[nameBegin + 12];
                  if ((c0xxx & 0xFF) == c0xxx
                     && (c1xxx & 0xFF) == c1xxx
                     && (c2xxx & 0xFF) == c2xxx
                     && (c3xxx & 0xFF) == c3xxx
                     && (c4xxx & 0xFF) == c4xxx
                     && (c5xxx & 0xFF) == c5xxx
                     && (c6xxx & 0xFF) == c6xxx
                     && (c7xxx & 0xFF) == c7xxx
                     && (c8xxx & 0xFF) == c8xxx
                     && (c9xxx & 0xFF) == c9xxx
                     && (c10xxx & 0xFF) == c10xxx
                     && (c11xxx & 0xFF) == c11xxx
                     && (c12xxx & 0xFF) == c12xxx) {
                     nameValue0 = ((long)c4xxx << 32) + ((long)c3xxx << 24) + ((long)c2xxx << 16) + ((long)c1xxx << 8) + (long)c0xxx;
                     nameValue1 = ((long)c12xxx << 56)
                        + ((long)c11xxx << 48)
                        + ((long)c10xxx << 40)
                        + ((long)c9xxx << 32)
                        + ((long)c8xxx << 24)
                        + ((long)c7xxx << 16)
                        + ((long)c6xxx << 8)
                        + (long)c5xxx;
                  }
                  break;
               case 14:
                  int c0xx = chars[nameBegin];
                  int c1xx = chars[nameBegin + 1];
                  int c2xx = chars[nameBegin + 2];
                  int c3xx = chars[nameBegin + 3];
                  int c4xx = chars[nameBegin + 4];
                  int c5xx = chars[nameBegin + 5];
                  int c6xx = chars[nameBegin + 6];
                  int c7xx = chars[nameBegin + 7];
                  int c8xx = chars[nameBegin + 8];
                  int c9xx = chars[nameBegin + 9];
                  int c10xx = chars[nameBegin + 10];
                  int c11xx = chars[nameBegin + 11];
                  int c12xx = chars[nameBegin + 12];
                  int c13xx = chars[nameBegin + 13];
                  if ((c0xx & 0xFF) == c0xx
                     && (c1xx & 0xFF) == c1xx
                     && (c2xx & 0xFF) == c2xx
                     && (c3xx & 0xFF) == c3xx
                     && (c4xx & 0xFF) == c4xx
                     && (c5xx & 0xFF) == c5xx
                     && (c6xx & 0xFF) == c6xx
                     && (c7xx & 0xFF) == c7xx
                     && (c8xx & 0xFF) == c8xx
                     && (c9xx & 0xFF) == c9xx
                     && (c10xx & 0xFF) == c10xx
                     && (c11xx & 0xFF) == c11xx
                     && (c12xx & 0xFF) == c12xx
                     && (c13xx & 0xFF) == c13xx) {
                     nameValue0 = ((long)c5xx << 40) + ((long)c4xx << 32) + ((long)c3xx << 24) + ((long)c2xx << 16) + ((long)c1xx << 8) + (long)c0xx;
                     nameValue1 = ((long)c13xx << 56)
                        + ((long)c12xx << 48)
                        + ((long)c11xx << 40)
                        + ((long)c10xx << 32)
                        + ((long)c9xx << 24)
                        + ((long)c8xx << 16)
                        + ((long)c7xx << 8)
                        + (long)c6xx;
                  }
                  break;
               case 15:
                  int c0x = chars[nameBegin];
                  int c1x = chars[nameBegin + 1];
                  int c2x = chars[nameBegin + 2];
                  int c3x = chars[nameBegin + 3];
                  int c4x = chars[nameBegin + 4];
                  int c5x = chars[nameBegin + 5];
                  int c6x = chars[nameBegin + 6];
                  int c7x = chars[nameBegin + 7];
                  int c8x = chars[nameBegin + 8];
                  int c9x = chars[nameBegin + 9];
                  int c10x = chars[nameBegin + 10];
                  int c11x = chars[nameBegin + 11];
                  int c12x = chars[nameBegin + 12];
                  int c13x = chars[nameBegin + 13];
                  int c14x = chars[nameBegin + 14];
                  if ((c0x & 0xFF) == c0x
                     && (c1x & 0xFF) == c1x
                     && (c2x & 0xFF) == c2x
                     && (c3x & 0xFF) == c3x
                     && (c4x & 0xFF) == c4x
                     && (c5x & 0xFF) == c5x
                     && (c6x & 0xFF) == c6x
                     && (c7x & 0xFF) == c7x
                     && (c8x & 0xFF) == c8x
                     && (c9x & 0xFF) == c9x
                     && (c10x & 0xFF) == c10x
                     && (c11x & 0xFF) == c11x
                     && (c12x & 0xFF) == c12x
                     && (c13x & 0xFF) == c13x
                     && (c14x & 0xFF) == c14x) {
                     nameValue0 = ((long)c6x << 48)
                        + ((long)c5x << 40)
                        + ((long)c4x << 32)
                        + ((long)c3x << 24)
                        + ((long)c2x << 16)
                        + ((long)c1x << 8)
                        + (long)c0x;
                     nameValue1 = ((long)c14x << 56)
                        + ((long)c13x << 48)
                        + ((long)c12x << 40)
                        + ((long)c11x << 32)
                        + ((long)c10x << 24)
                        + ((long)c9x << 16)
                        + ((long)c8x << 8)
                        + (long)c7x;
                  }
                  break;
               case 16:
                  int c0 = chars[nameBegin];
                  int c1 = chars[nameBegin + 1];
                  int c2 = chars[nameBegin + 2];
                  int c3 = chars[nameBegin + 3];
                  int c4 = chars[nameBegin + 4];
                  int c5 = chars[nameBegin + 5];
                  int c6 = chars[nameBegin + 6];
                  int c7 = chars[nameBegin + 7];
                  int c8 = chars[nameBegin + 8];
                  int c9 = chars[nameBegin + 9];
                  int c10 = chars[nameBegin + 10];
                  int c11 = chars[nameBegin + 11];
                  int c12 = chars[nameBegin + 12];
                  int c13 = chars[nameBegin + 13];
                  int c14 = chars[nameBegin + 14];
                  int c15 = chars[nameBegin + 15];
                  if ((c0 & 0xFF) == c0
                     && (c1 & 0xFF) == c1
                     && (c2 & 0xFF) == c2
                     && (c3 & 0xFF) == c3
                     && (c4 & 0xFF) == c4
                     && (c5 & 0xFF) == c5
                     && (c6 & 0xFF) == c6
                     && (c7 & 0xFF) == c7
                     && (c8 & 0xFF) == c8
                     && (c9 & 0xFF) == c9
                     && (c10 & 0xFF) == c10
                     && (c11 & 0xFF) == c11
                     && (c12 & 0xFF) == c12
                     && (c13 & 0xFF) == c13
                     && (c14 & 0xFF) == c14
                     && (c15 & 0xFF) == c15) {
                     nameValue0 = ((long)c7 << 56)
                        + ((long)c6 << 48)
                        + ((long)c5 << 40)
                        + ((long)c4 << 32)
                        + ((long)c3 << 24)
                        + ((long)c2 << 16)
                        + ((long)c1 << 8)
                        + (long)c0;
                     nameValue1 = ((long)c15 << 56)
                        + ((long)c14 << 48)
                        + ((long)c13 << 40)
                        + ((long)c12 << 32)
                        + ((long)c11 << 24)
                        + ((long)c10 << 16)
                        + ((long)c9 << 8)
                        + (long)c8;
                  }
            }

            if (nameValue0 != -1L) {
               if (nameValue1 != -1L) {
                  long nameValue01 = nameValue0 ^ nameValue1;
                  int indexMask = (int)(nameValue01 ^ nameValue01 >>> 32) & JSONFactory.NAME_CACHE2.length - 1;
                  JSONFactory.NameCacheEntry2 entry = JSONFactory.NAME_CACHE2[indexMask];
                  if (entry == null) {
                     String name;
                     if (this.str != null) {
                        name = this.str.substring(nameBegin, this.nameEnd);
                     } else {
                        name = new String(chars, nameBegin, this.nameEnd - nameBegin);
                     }

                     JSONFactory.NAME_CACHE2[indexMask] = new JSONFactory.NameCacheEntry2(name, nameValue0, nameValue1);
                     return name;
                  }

                  if (entry.value0 == nameValue0 && entry.value1 == nameValue1) {
                     return entry.name;
                  }
               } else {
                  int indexMaskx = (int)(nameValue0 ^ nameValue0 >>> 32) & JSONFactory.NAME_CACHE.length - 1;
                  JSONFactory.NameCacheEntry entryx = JSONFactory.NAME_CACHE[indexMaskx];
                  if (entryx == null) {
                     String name;
                     if (this.str != null) {
                        name = this.str.substring(nameBegin, this.nameEnd);
                     } else {
                        name = new String(chars, nameBegin, this.nameEnd - nameBegin);
                     }

                     JSONFactory.NAME_CACHE[indexMaskx] = new JSONFactory.NameCacheEntry(name, nameValue0);
                     return name;
                  }

                  if (entryx.value == nameValue0) {
                     return entryx.name;
                  }
               }
            }

            return this.str != null ? this.str.substring(nameBegin, this.nameEnd) : new String(chars, nameBegin, this.nameEnd - nameBegin);
         } else {
            return this.getFieldName();
         }
      }
   }

   @Override
   public final boolean skipName() {
      char quote = this.ch;
      if (quote == '\'' && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else if (quote != '"' && quote != '\'') {
         if ((this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) != 0L) {
            this.readFieldNameHashCodeUnquote();
            return true;
         } else {
            throw this.notSupportName();
         }
      } else {
         int offset = this.offset;
         char[] chars = this.chars;

         while (true) {
            char ch = chars[offset++];
            if (ch == '\\') {
               ch = chars[offset];
               offset += ch == 'u' ? 5 : (ch == 'x' ? 3 : 1);
            } else if (ch == quote) {
               ch = offset == this.end ? 26 : chars[offset++];

               while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == this.end ? 26 : chars[offset++];
               }

               if (ch != ':') {
                  throw syntaxError(ch);
               }

               ch = offset == this.end ? 26 : chars[offset++];

               while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == this.end ? 26 : chars[offset++];
               }

               this.offset = offset;
               this.ch = ch;
               return true;
            }
         }
      }
   }

   @Override
   public final int readInt32Value() {
      boolean negative = false;
      char ch = this.ch;
      int offset = this.offset;
      int end = this.end;
      char[] chars = this.chars;
      int intValue = 0;
      int quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
      }

      if (ch == '-') {
         negative = true;
         ch = chars[offset++];
      } else if (ch == '+') {
         ch = chars[offset++];
      } else if (ch == ',') {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < '0' || ch > '9'; ch >= '0' && ch <= '9'; ch = offset == end ? 26 : chars[offset++]) {
         int intValue10 = intValue * 10 + (ch - '0');
         if (intValue10 < intValue) {
            overflow = true;
            break;
         }

         intValue = intValue10;
      }

      if (ch == '.' || ch == 'e' || ch == 'E' || ch == 't' || ch == 'f' || ch == 'n' || ch == '{' || ch == '[' || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.getInt32Value();
      } else {
         if (quote != 0) {
            ch = offset == end ? 26 : chars[offset++];
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset == end ? 26 : chars[offset++];
         }

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         return negative ? -intValue : intValue;
      }
   }

   @Override
   public final Integer readInt32() {
      boolean negative = false;
      char ch = this.ch;
      int offset = this.offset;
      char[] chars = this.chars;
      int intValue = 0;
      int quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
      }

      if (ch == '-') {
         negative = true;
         ch = chars[offset++];
      } else if (ch == '+') {
         ch = chars[offset++];
      } else if (ch == ',') {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < '0' || ch > '9'; ch >= '0' && ch <= '9'; ch = offset == this.end ? 26 : chars[offset++]) {
         int intValue10 = intValue * 10 + (ch - '0');
         if (intValue10 < intValue) {
            overflow = true;
            break;
         }

         intValue = intValue10;
      }

      if (ch == '.' || ch == 'e' || ch == 'E' || ch == 't' || ch == 'f' || ch == 'n' || ch == '{' || ch == '[' || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.wasNull ? null : this.getInt32Value();
      } else {
         if (quote != 0) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         return negative ? -intValue : intValue;
      }
   }

   @Override
   public final long readInt64Value() {
      boolean negative = false;
      char ch = this.ch;
      int offset = this.offset;
      char[] chars = this.chars;
      long longValue = 0L;
      char quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
      }

      if (ch == '-') {
         negative = true;
         ch = chars[offset++];
      } else if (ch == '+') {
         ch = chars[offset++];
      } else if (ch == ',') {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < '0' || ch > '9'; ch >= '0' && ch <= '9'; ch = offset == this.end ? 26 : chars[offset++]) {
         long intValue10 = longValue * 10L + (long)(ch - '0');
         if (intValue10 < longValue) {
            overflow = true;
            break;
         }

         longValue = intValue10;
      }

      if (ch == '.' || ch == 'e' || ch == 'E' || ch == 't' || ch == 'f' || ch == 'n' || ch == '{' || ch == '[' || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.getInt64Value();
      } else {
         if (quote != 0) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         return negative ? -longValue : longValue;
      }
   }

   @Override
   public final Long readInt64() {
      boolean negative = false;
      char ch = this.ch;
      int offset = this.offset;
      char[] chars = this.chars;
      long longValue = 0L;
      char quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
      }

      if (ch == '-') {
         negative = true;
         ch = chars[offset++];
      } else if (ch == '+') {
         ch = chars[offset++];
      } else if (ch == ',') {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < '0' || ch > '9'; ch >= '0' && ch <= '9'; ch = offset == this.end ? 26 : chars[offset++]) {
         long intValue10 = longValue * 10L + (long)(ch - '0');
         if (intValue10 < longValue) {
            overflow = true;
            break;
         }

         longValue = intValue10;
      }

      if (ch == '.' || ch == 'e' || ch == 'E' || ch == 't' || ch == 'f' || ch == 'n' || ch == '{' || ch == '[' || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.wasNull ? null : this.getInt64Value();
      } else {
         this.wasNull = false;
         if (quote != 0) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         return negative ? -longValue : longValue;
      }
   }

   @Override
   public final double readDoubleValue() {
      boolean valid = false;
      this.wasNull = false;
      boolean value = false;
      double doubleValue = 0.0;
      char[] chars = this.chars;
      char quote = 0;
      char ch = this.ch;
      int offset = this.offset;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
         if (ch == quote) {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            this.ch = ch;
            this.offset = offset;
            this.nextIfComma();
            this.wasNull = true;
            return 0.0;
         }
      }

      int start = offset;
      if (ch == '-') {
         this.negative = true;
         ch = chars[offset++];
      } else {
         this.negative = false;
         if (ch == '+') {
            ch = chars[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
         valid = true;
         if (!overflow) {
            long intValue10 = longValue * 10L + (long)(ch - '0');
            if (intValue10 < longValue) {
               overflow = true;
            } else {
               longValue = intValue10;
            }
         }

         if (offset == this.end) {
            ch = '\u001a';
            offset++;
            break;
         }
      }

      this.scale = 0;
      if (ch == '.') {
         this.valueType = 2;

         for (ch = chars[offset++]; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long intValue10 = longValue * 10L + (long)(ch - '0');
               if (intValue10 < longValue) {
                  overflow = true;
               } else {
                  longValue = intValue10;
               }
            }

            if (offset == this.end) {
               ch = '\u001a';
               offset++;
               break;
            }
         }
      }

      int expValue = 0;
      if (ch == 'e' || ch == 'E') {
         boolean negativeExp = false;
         ch = chars[offset++];
         if (ch == '-') {
            negativeExp = true;
            ch = chars[offset++];
         } else if (ch == '+') {
            ch = chars[offset++];
         }

         while (ch >= '0' && ch <= '9') {
            valid = true;
            int byteVal = ch - '0';
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = '\u001a';
               offset++;
               break;
            }

            ch = chars[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 'n' && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
            offset += 3;
            valid = true;
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 't' && chars[offset] == 'r' && chars[offset + 1] == 'u' && chars[offset + 2] == 'e') {
            offset += 3;
            valid = true;
            value = true;
            doubleValue = 1.0;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 'f' && chars[offset] == 'a' && chars[offset + 1] == 'l' && chars[offset + 2] == 's' && chars[offset + 3] == 'e') {
            valid = true;
            offset += 4;
            doubleValue = 0.0;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 'N' && chars[offset] == 'a' && chars[offset + 1] == 'N') {
            valid = true;
            offset += 2;
            doubleValue = Double.NaN;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == '{' && quote == 0) {
            valid = true;
            this.ch = ch;
            this.offset = offset;
            Map<String, Object> obj = this.readObject();
            if (!obj.isEmpty()) {
               throw new JSONException(this.info());
            }

            offset = this.offset;
            ch = this.ch;
            value = true;
            this.wasNull = true;
         } else if (ch == '[' && quote == 0) {
            valid = true;
            this.ch = ch;
            this.offset = offset;
            List array = this.readArray();
            if (!array.isEmpty()) {
               throw new JSONException(this.info());
            }

            offset = this.offset;
            ch = this.ch;
            value = true;
            this.wasNull = true;
         }
      }

      int len = offset - start;
      String str = null;
      if (quote != 0) {
         if (ch != quote) {
            this.offset = offset - 1;
            this.ch = quote;
            str = this.readString();
            offset = this.offset;
         }

         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (!value) {
         if (!overflow) {
            if (longValue == 0L) {
               if (this.scale == 1) {
                  doubleValue = 0.0;
                  value = true;
               }
            } else {
               int scale = this.scale - expValue;
               if (scale == 0) {
                  doubleValue = (double)longValue;
                  if (this.negative) {
                     doubleValue = -doubleValue;
                  }

                  value = true;
               } else if ((long)((double)longValue) == longValue) {
                  if (0 < scale && scale < JSONFactory.DOUBLE_10_POW.length) {
                     doubleValue = (double)longValue / JSONFactory.DOUBLE_10_POW[scale];
                     if (this.negative) {
                        doubleValue = -doubleValue;
                     }

                     value = true;
                  } else if (0 > scale && scale > -JSONFactory.DOUBLE_10_POW.length) {
                     doubleValue = (double)longValue * JSONFactory.DOUBLE_10_POW[-scale];
                     if (this.negative) {
                        doubleValue = -doubleValue;
                     }

                     value = true;
                  }
               }

               if (!value && scale > -128 && scale < 128) {
                  doubleValue = TypeUtils.doubleValue(this.negative ? -1 : 1, longValue, scale);
                  value = true;
               }
            }
         }

         if (!value) {
            if (str != null) {
               try {
                  doubleValue = Double.parseDouble(str);
               } catch (NumberFormatException var17) {
                  throw new JSONException(this.info(), var17);
               }
            } else {
               doubleValue = TypeUtils.parseDouble(chars, start - 1, len);
            }
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = ch;
         this.offset = offset;
         return doubleValue;
      }
   }

   @Override
   public final float readFloatValue() {
      boolean valid = false;
      this.wasNull = false;
      boolean value = false;
      float floatValue = 0.0F;
      char[] chars = this.chars;
      char quote = 0;
      char ch = this.ch;
      int offset = this.offset;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
         if (ch == quote) {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            this.ch = ch;
            this.offset = offset;
            this.nextIfComma();
            this.wasNull = true;
            return 0.0F;
         }
      }

      int start = offset;
      if (ch == '-') {
         this.negative = true;
         ch = chars[offset++];
      } else {
         this.negative = false;
         if (ch == '+') {
            ch = chars[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
         valid = true;
         if (!overflow) {
            long intValue10 = longValue * 10L + (long)(ch - '0');
            if (intValue10 < longValue) {
               overflow = true;
            } else {
               longValue = intValue10;
            }
         }

         if (offset == this.end) {
            ch = '\u001a';
            offset++;
            break;
         }
      }

      this.scale = 0;
      if (ch == '.') {
         this.valueType = 2;

         for (ch = chars[offset++]; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long intValue10 = longValue * 10L + (long)(ch - '0');
               if (intValue10 < longValue) {
                  overflow = true;
               } else {
                  longValue = intValue10;
               }
            }

            if (offset == this.end) {
               ch = '\u001a';
               offset++;
               break;
            }
         }
      }

      int expValue = 0;
      if (ch == 'e' || ch == 'E') {
         boolean negativeExp = false;
         ch = chars[offset++];
         if (ch == '-') {
            negativeExp = true;
            ch = chars[offset++];
         } else if (ch == '+') {
            ch = chars[offset++];
         }

         while (ch >= '0' && ch <= '9') {
            valid = true;
            int byteVal = ch - '0';
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = '\u001a';
               offset++;
               break;
            }

            ch = chars[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 'n' && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
            offset += 3;
            valid = true;
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 't' && chars[offset] == 'r' && chars[offset + 1] == 'u' && chars[offset + 2] == 'e') {
            offset += 3;
            valid = true;
            value = true;
            floatValue = 1.0F;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 'f' && chars[offset] == 'a' && chars[offset + 1] == 'l' && chars[offset + 2] == 's' && chars[offset + 3] == 'e') {
            offset += 4;
            valid = true;
            floatValue = 0.0F;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 'N' && chars[offset] == 'a' && chars[offset + 1] == 'N') {
            offset += 2;
            valid = true;
            value = true;
            floatValue = Float.NaN;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == '{' && quote == 0) {
            valid = true;
            this.ch = ch;
            this.offset = offset;
            Map<String, Object> obj = this.readObject();
            if (!obj.isEmpty()) {
               throw new JSONException(this.info());
            }

            ch = this.ch;
            offset = this.offset;
            value = true;
            this.wasNull = true;
         } else if (ch == '[' && quote == 0) {
            this.ch = ch;
            this.offset = offset;
            valid = true;
            List array = this.readArray();
            if (!array.isEmpty()) {
               throw new JSONException(this.info());
            }

            ch = this.ch;
            offset = this.offset;
            value = true;
            this.wasNull = true;
         }
      }

      int len = offset - start;
      String str = null;
      if (quote != 0) {
         if (ch != quote) {
            overflow = true;
            this.offset = offset - 1;
            this.ch = quote;
            str = this.readString();
            offset = this.offset;
         }

         ch = offset >= this.end ? 26 : chars[offset++];
      }

      if (!value) {
         if (!overflow) {
            int scale = this.scale - expValue;
            if (scale == 0) {
               floatValue = (float)longValue;
               if (this.negative) {
                  floatValue = -floatValue;
               }

               value = true;
            } else if ((long)((float)longValue) == longValue) {
               if (0 < scale && scale < JSONFactory.FLOAT_10_POW.length) {
                  floatValue = (float)longValue / JSONFactory.FLOAT_10_POW[scale];
                  if (this.negative) {
                     floatValue = -floatValue;
                  }
               } else if (0 > scale && scale > -JSONFactory.FLOAT_10_POW.length) {
                  floatValue = (float)longValue * JSONFactory.FLOAT_10_POW[-scale];
                  if (this.negative) {
                     floatValue = -floatValue;
                  }
               }
            }

            if (!value && scale > -128 && scale < 128) {
               floatValue = TypeUtils.floatValue(this.negative ? -1 : 1, longValue, scale);
               value = true;
            }
         }

         if (!value) {
            if (str != null) {
               try {
                  floatValue = Float.parseFloat(str);
               } catch (NumberFormatException var16) {
                  throw new JSONException(this.info(), var16);
               }
            } else {
               floatValue = TypeUtils.parseFloat(chars, start - 1, len);
            }
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset >= this.end ? 26 : chars[offset++];
         }
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset >= this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset >= this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset >= this.end ? 26 : chars[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = ch;
         this.offset = offset;
         return floatValue;
      }
   }

   private void skipString() {
      char[] chars = this.chars;
      int offset = this.offset;
      char quote = this.ch;
      char ch = chars[offset++];

      while (true) {
         while (ch != '\\') {
            if (ch == quote) {
               ch = offset == this.end ? chars[offset++] : 26;

               while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
                  ch = chars[offset++];
               }

               if (this.comma = ch == ',') {
                  if (offset >= this.end) {
                     this.offset = offset;
                     this.ch = 26;
                     return;
                  }

                  for (ch = chars[offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
                     if (++offset >= this.end) {
                        this.offset = offset;
                        this.ch = 26;
                        return;
                     }
                  }

                  offset++;
               } else if (ch != 26 && ch != '}' && ch != ']' && ch != 26) {
                  throw this.error(offset, ch);
               }

               this.offset = offset;
               this.ch = ch;
               return;
            }

            ch = offset == this.end ? chars[offset++] : 26;
         }

         if (offset >= this.end) {
            throw new JSONException(this.info("illegal string, end"));
         }

         ch = chars[offset++];
         if (ch == 'u') {
            offset += 4;
         } else if (ch == 'x') {
            offset += 2;
         } else if (ch != '\\' && ch != '"') {
            this.char1(ch);
         }

         ch = chars[offset++];
      }
   }

   @Override
   public final String getString() {
      if (this.stringValue != null) {
         return this.stringValue;
      } else {
         int length = this.nameEnd - this.nameBegin;
         if (!this.nameEscape) {
            return new String(this.chars, this.nameBegin, length);
         } else {
            char[] chars = this.chars;
            char[] buf = new char[this.nameLength];
            int offset = this.nameBegin;
            int i = 0;

            while (true) {
               char c = chars[offset];
               if (c == '\\') {
                  c = chars[++offset];
                  switch (c) {
                     case '"':
                     case '\\':
                        break;
                     case 'u':
                        c = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        c = char2(chars[offset + 1], chars[offset + 2]);
                        offset += 2;
                        break;
                     default:
                        c = this.char1(c);
                  }
               } else if (c == '"') {
                  return this.stringValue = new String(buf);
               }

               buf[i] = c;
               offset++;
               i++;
            }
         }
      }
   }

   protected final void readString0() {
      char[] chars = this.chars;
      char quote = this.ch;
      int offset = this.offset;
      int start = offset;
      this.valueEscape = false;
      int i = 0;

      while (true) {
         char c = chars[offset];
         if (c == '\\') {
            this.valueEscape = true;
            c = chars[offset + 1];
            offset += c == 'u' ? 6 : (c == 'x' ? 4 : 2);
         } else {
            if (c == quote) {
               String str;
               if (this.valueEscape) {
                  char[] buf = new char[i];
                  offset = start;
                  int ix = 0;

                  while (true) {
                     char cx = this.chars[offset];
                     if (cx == '\\') {
                        cx = this.chars[++offset];
                        if (cx == 'u') {
                           cx = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                           offset += 4;
                        } else if (cx == 'x') {
                           cx = char2(chars[offset + 1], chars[offset + 2]);
                           offset += 2;
                        } else if (cx != '\\' && cx != '"') {
                           cx = this.char1(cx);
                        }
                     } else if (cx == '"') {
                        str = new String(buf);
                        break;
                     }

                     buf[ix] = cx;
                     offset++;
                     ix++;
                  }
               } else {
                  str = new String(chars, this.offset, offset - this.offset);
               }

               if (++offset == this.end) {
                  c = (char)26;
               } else {
                  c = chars[offset];
               }

               while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
                  c = chars[++offset];
               }

               if (this.comma = c == ',') {
                  this.offset = offset + 1;
                  this.ch = chars[this.offset++];

                  while (this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L) {
                     if (this.offset >= this.end) {
                        this.ch = 26;
                     } else {
                        this.ch = chars[this.offset++];
                     }
                  }
               } else {
                  this.offset = offset + 1;
                  this.ch = (char)c;
               }

               this.stringValue = str;
               return;
            }

            offset++;
         }

         i++;
      }
   }

   @Override
   public String readString() {
      if (this.ch != '"' && this.ch != '\'') {
         return this.readStringNotMatch();
      } else {
         char[] chars = this.chars;
         char quote = this.ch;
         int offset = this.offset;
         int start = offset;
         boolean valueEscape = false;
         int i = 0;
         char c0 = (char)0;
         char c1 = (char)0;
         char c2 = (char)0;
         boolean quoted = false;

         for (int upperBound = offset + (this.end - offset & -4); offset < upperBound; i += 4) {
            c0 = chars[offset];
            c1 = chars[offset + 1];
            c2 = chars[offset + 2];
            char c3 = chars[offset + 3];
            if (c0 == '\\' || c1 == '\\' || c2 == '\\' || c3 == '\\') {
               break;
            }

            if (c0 == quote || c1 == quote || c2 == quote || c3 == quote) {
               quoted = true;
               break;
            }

            offset += 4;
         }

         int valueLength;
         if (quoted) {
            if (c0 != quote) {
               if (c1 == quote) {
                  offset++;
                  i++;
               } else if (c2 == quote) {
                  offset += 2;
                  i += 2;
               } else {
                  offset += 3;
                  i += 3;
               }
            }

            valueLength = i;
         } else {
            while (true) {
               if (offset >= this.end) {
                  throw new JSONException(this.info("invalid escape character EOI"));
               }

               char c = chars[offset];
               if (c == '\\') {
                  valueEscape = true;
                  c = chars[offset + 1];
                  offset += c == 'u' ? 6 : (c == 'x' ? 4 : 2);
               } else {
                  if (c == quote) {
                     valueLength = i;
                     break;
                  }

                  offset++;
               }

               i++;
            }
         }

         String str;
         if (valueEscape) {
            char[] buf = new char[valueLength];
            offset = start;
            c1 = (char)0;

            while (true) {
               c2 = chars[offset];
               if (c2 == '\\') {
                  c2 = chars[++offset];
                  switch (c2) {
                     case '"':
                     case '\\':
                        break;
                     case 'b':
                        c2 = (char)8;
                        break;
                     case 'f':
                        c2 = (char)12;
                        break;
                     case 'n':
                        c2 = (char)10;
                        break;
                     case 'r':
                        c2 = (char)13;
                        break;
                     case 't':
                        c2 = (char)9;
                        break;
                     case 'u':
                        c2 = char4(chars[offset + 1], chars[offset + 2], chars[offset + 3], chars[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        c2 = char2(chars[offset + 1], chars[offset + 2]);
                        offset += 2;
                        break;
                     default:
                        c2 = this.char1(c2);
                  }
               } else if (c2 == quote) {
                  if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                     str = JDKUtils.STRING_CREATOR_JDK8.apply(buf, Boolean.TRUE);
                  } else {
                     str = new String(buf);
                  }
                  break;
               }

               buf[c1] = c2;
               offset++;
               c1++;
            }
         } else {
            c2 = (char)(offset - start);
            if (c2 == 1 && (c0 = chars[start]) < 128) {
               str = TypeUtils.toString(c0);
            } else {
               char c1x;
               if (c2 == 2 && (c0 = chars[start]) < 128 && (c1x = chars[start + 1]) < 128) {
                  str = TypeUtils.toString(c0, c1x);
               } else if (this.str == null || JDKUtils.JVM_VERSION <= 8 && !JDKUtils.ANDROID) {
                  str = new String(chars, start, offset - start);
               } else {
                  str = this.str.substring(start, offset);
               }
            }
         }

         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         offset++;
         c0 = offset == this.end ? 26 : chars[offset++];

         while (c0 <= ' ' && (1L << c0 & 4294981376L) != 0L) {
            c0 = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = c0 == ',') {
            c0 = offset == this.end ? 26 : chars[offset++];

            while (c0 <= ' ' && (1L << c0 & 4294981376L) != 0L) {
               c0 = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = (char)c0;
         this.offset = offset;
         return str;
      }
   }

   @Override
   public final void skipValue() {
      char[] chars;
      char ch;
      int offset;
      int end;
      chars = this.chars;
      ch = this.ch;
      offset = this.offset;
      end = this.end;
      this.comma = false;
      label373:
      switch (ch) {
         case '"':
         case '\'':
            char quote = ch;
            ch = chars[offset++];

            while (true) {
               while (ch != '\\') {
                  if (ch == quote) {
                     ch = offset == end ? 26 : chars[offset++];
                     break label373;
                  }

                  ch = chars[offset++];
               }

               ch = chars[offset++];
               if (ch == 'u') {
                  offset += 4;
               } else if (ch == 'x') {
                  offset += 2;
               } else if (ch != '\\' && ch != '"') {
                  this.char1(ch);
               }

               ch = chars[offset++];
            }
         case '+':
         case '-':
         case '.':
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
            boolean sign = ch == '-' || ch == '+';
            if (sign) {
               if (offset >= end) {
                  throw numberError(offset, ch);
               }

               ch = chars[offset++];
            }

            boolean dot = ch == '.';
            boolean num = false;
            if (!dot && ch >= '0' && ch <= '9') {
               num = true;

               do {
                  ch = offset == end ? 26 : chars[offset++];
               } while (ch >= '0' && ch <= '9');
            }

            if (num && (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S')) {
               ch = chars[offset++];
            }

            boolean small = false;
            if (ch == '.') {
               small = true;
               ch = offset == end ? 26 : chars[offset++];
               if (ch >= '0' && ch <= '9') {
                  do {
                     ch = offset == end ? 26 : chars[offset++];
                  } while (ch >= '0' && ch <= '9');
               }
            }

            if (!num && !small) {
               throw numberError(offset, ch);
            }

            if (ch == 'e' || ch == 'E') {
               ch = chars[offset++];
               boolean eSign = false;
               if (ch == '+' || ch == '-') {
                  eSign = true;
                  if (offset >= end) {
                     throw numberError(offset, ch);
                  }

                  ch = chars[offset++];
               }

               if (ch >= '0' && ch <= '9') {
                  do {
                     ch = offset == end ? 26 : chars[offset++];
                  } while (ch >= '0' && ch <= '9');
               } else if (eSign) {
                  throw numberError(offset, ch);
               }
            }

            if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
               ch = offset == end ? 26 : chars[offset++];
            }
            break;
         case 'f':
            if (offset + 4 > end) {
               throw this.error(offset, ch);
            }

            if (chars[offset] != 'a' || chars[offset + 1] != 'l' || chars[offset + 2] != 's' || chars[offset + 3] != 'e') {
               throw this.error(offset, ch);
            }

            offset += 4;
            ch = offset == end ? 26 : chars[offset++];
            break;
         case 'n':
            if (offset + 3 > end) {
               throw this.error(offset, ch);
            }

            if (chars[offset] != 'u' || chars[offset + 1] != 'l' || chars[offset + 2] != 'l') {
               throw this.error(offset, ch);
            }

            offset += 3;
            ch = offset == end ? 26 : chars[offset++];
            break;
         case 't':
            if (offset + 3 > end) {
               throw this.error(offset, ch);
            }

            if (chars[offset] != 'r' || chars[offset + 1] != 'u' || chars[offset + 2] != 'e') {
               throw this.error(offset, ch);
            }

            offset += 3;
            ch = offset == end ? 26 : chars[offset++];
            break;
         default:
            if (ch == '[') {
               this.next();

               for (int i = 0; this.ch != ']'; i++) {
                  if (i != 0 && !this.comma) {
                     throw this.valueError();
                  }

                  this.comma = false;
                  this.skipValue();
               }

               this.comma = false;
               offset = this.offset;
               ch = offset == end ? 26 : chars[offset++];
            } else if (ch == '{') {
               this.next();

               while (this.ch != '}') {
                  this.skipName();
                  this.skipValue();
               }

               this.comma = false;
               offset = this.offset;
               ch = offset == end ? 26 : chars[offset++];
            } else {
               if (ch != 'S' || !this.nextIfSet()) {
                  throw this.error(offset, ch);
               }

               this.skipValue();
               ch = this.ch;
               offset = this.offset;
            }
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : chars[offset++];
      }

      if (ch == ',') {
         this.comma = true;
         ch = offset == end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : chars[offset++];
         }
      }

      if (!this.comma && ch != '}' && ch != ']' && ch != 26) {
         throw this.error(offset, ch);
      } else if (!this.comma || ch != '}' && ch != ']' && ch != 26) {
         this.ch = ch;
         this.offset = offset;
      } else {
         throw this.error(offset, ch);
      }
   }

   @Override
   public final void skipComment() {
      int offset = this.offset;
      if (offset + 1 >= this.end) {
         throw new JSONException(this.info());
      } else {
         char[] chars = this.chars;
         char ch = chars[offset++];
         boolean multi;
         if (ch == '*') {
            multi = true;
         } else {
            if (ch != '/') {
               return;
            }

            multi = false;
         }

         ch = chars[offset++];

         while (true) {
            boolean endOfComment = false;
            if (multi) {
               if (ch == '*' && offset <= this.end && chars[offset] == '/') {
                  offset++;
                  endOfComment = true;
               }
            } else {
               endOfComment = ch == '\n';
            }

            if (endOfComment) {
               if (offset >= this.end) {
                  ch = '\u001a';
               } else {
                  for (ch = chars[offset]; ch <= ' ' && (1L << ch & 4294981376L) != 0L; ch = chars[offset]) {
                     if (++offset >= this.end) {
                        ch = '\u001a';
                        break;
                     }
                  }

                  offset++;
               }
               break;
            }

            if (offset >= this.end) {
               ch = '\u001a';
               break;
            }

            ch = chars[offset++];
         }

         this.ch = ch;
         this.offset = offset;
         if (ch == '/') {
            this.skipComment();
         }
      }
   }

   @Override
   public final void readNumber0() {
      boolean valid = false;
      this.wasNull = false;
      this.mag0 = 0;
      this.mag1 = 0;
      this.mag2 = 0;
      this.mag3 = 0;
      this.negative = false;
      this.exponent = 0;
      this.scale = 0;
      int firstOffset = this.offset;
      char[] chars = this.chars;
      char ch = this.ch;
      int offset = this.offset;
      char quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
         if (ch == quote) {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }

            this.ch = ch;
            this.offset = offset;
            this.comma = this.nextIfComma();
            this.wasNull = true;
            this.valueType = 5;
            return;
         }
      }

      int start = offset;
      int multmin;
      if (ch == '-') {
         if (offset == this.end) {
            throw new JSONException(this.info("illegal input"));
         }

         multmin = -214748364;
         this.negative = true;
         ch = chars[offset++];
      } else {
         if (ch == '+') {
            if (offset == this.end) {
               throw new JSONException(this.info("illegal input"));
            }

            ch = chars[offset++];
         }

         multmin = -214748364;
      }

      boolean intOverflow = false;

      for (this.valueType = 1; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
         valid = true;
         if (!intOverflow) {
            int digit = ch - '0';
            this.mag3 *= 10;
            if (this.mag3 < multmin) {
               intOverflow = true;
            } else {
               this.mag3 -= digit;
               if (this.mag3 < multmin) {
                  intOverflow = true;
               }
            }
         }

         if (offset == this.end) {
            ch = '\u001a';
            offset++;
            break;
         }
      }

      if (ch == '.') {
         this.valueType = 2;

         for (ch = chars[offset++]; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
            valid = true;
            if (!intOverflow) {
               int digit = ch - '0';
               this.mag3 *= 10;
               if (this.mag3 < multmin) {
                  intOverflow = true;
               } else {
                  this.mag3 -= digit;
                  if (this.mag3 < multmin) {
                     intOverflow = true;
                  }
               }
            }

            this.scale++;
            if (offset == this.end) {
               ch = '\u001a';
               offset++;
               break;
            }
         }
      }

      if (intOverflow) {
         int numStart = this.negative ? start : start - 1;
         int numDigits = this.scale > 0 ? offset - 2 - numStart : offset - 1 - numStart;
         if (numDigits > 38) {
            this.valueType = 8;
            if (this.negative) {
               numStart--;
            }

            this.stringValue = new String(chars, numStart, offset - 1 - numStart);
         } else {
            this.bigInt(chars, numStart, offset - 1);
         }
      } else {
         this.mag3 = -this.mag3;
      }

      if (ch == 'e' || ch == 'E') {
         boolean negativeExp = false;
         int expValue = 0;
         ch = chars[offset++];
         if (ch == '-') {
            negativeExp = true;
            ch = chars[offset++];
         } else if (ch == '+') {
            ch = chars[offset++];
         }

         while (ch >= '0' && ch <= '9') {
            valid = true;
            int byteVal = ch - '0';
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = '\u001a';
               break;
            }

            ch = chars[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 'n') {
            if (chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
               offset += 3;
               valid = true;
               this.wasNull = true;
               this.valueType = 5;
               ch = offset == this.end ? 26 : chars[offset++];
            }
         } else if (ch == 't' && chars[offset] == 'r' && chars[offset + 1] == 'u' && chars[offset + 2] == 'e') {
            offset += 3;
            valid = true;
            this.boolValue = true;
            this.valueType = 4;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 'f' && chars[offset] == 'a' && chars[offset + 1] == 'l' && chars[offset + 2] == 's' && chars[offset + 3] == 'e') {
            valid = true;
            offset += 4;
            this.boolValue = false;
            this.valueType = 4;
            ch = offset == this.end ? 26 : chars[offset++];
         } else {
            if (ch == '{' && quote == 0) {
               this.offset = offset;
               this.ch = ch;
               this.complex = this.readObject();
               this.valueType = 6;
               return;
            }

            if (ch == '[' && quote == 0) {
               this.offset = offset;
               this.ch = ch;
               this.complex = this.readArray();
               this.valueType = 7;
               return;
            }
         }
      }

      if (quote != 0) {
         if (ch != quote) {
            this.offset = firstOffset;
            this.ch = quote;
            this.readString0();
            this.valueType = 3;
            return;
         }

         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
         switch (ch) {
            case 'B':
               if (!intOverflow && this.valueType != 2) {
                  this.valueType = 9;
               }
               break;
            case 'D':
               this.valueType = 13;
               break;
            case 'F':
               this.valueType = 12;
               break;
            case 'L':
               if (offset - start < 19 && this.valueType != 2) {
                  this.valueType = 11;
               }
               break;
            case 'S':
               if (!intOverflow && this.valueType != 2) {
                  this.valueType = 10;
               }
         }

         ch = offset == this.end ? 26 : chars[offset++];
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.offset = offset;
         this.ch = ch;
      }
   }

   @Override
   public final boolean readIfNull() {
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;
      if (ch == 'n' && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
         if (offset + 3 == this.end) {
            ch = '\u001a';
         } else {
            ch = chars[offset + 3];
         }

         offset += 4;

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final Date readNullOrNewDate() {
      Date date = null;
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;
      if (offset + 2 < this.end && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
         if (offset + 3 == this.end) {
            ch = '\u001a';
         } else {
            ch = chars[offset + 3];
         }

         offset += 4;
      } else {
         if (offset + 1 >= this.end || chars[offset] != 'e' || chars[offset + 1] != 'w') {
            throw new JSONException("json syntax error, not match null or new Date" + offset);
         }

         if (offset + 3 == this.end) {
            ch = '\u001a';
         } else {
            ch = chars[offset + 2];
         }

         offset += 3;

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            if (offset >= this.end) {
               ch = '\u001a';
            } else {
               ch = chars[offset++];
            }
         }

         if (offset + 4 >= this.end || ch != 'D' || chars[offset] != 'a' || chars[offset + 1] != 't' || chars[offset + 2] != 'e') {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         if (offset + 3 == this.end) {
            ch = '\u001a';
         } else {
            ch = chars[offset + 3];
         }

         offset += 4;

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch != '(' || offset >= this.end) {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         ch = chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.ch = ch;
         this.offset = offset;
         long millis = this.readInt64Value();
         ch = this.ch;
         offset = this.offset;
         if (ch != ')') {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         ch = offset == this.end ? 26 : chars[offset++];
         date = new Date(millis);
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      this.ch = ch;
      this.offset = offset;
      return date;
   }

   @Override
   public final boolean isNull() {
      return this.ch == 'n' && this.offset < this.end && this.chars[this.offset] == 'u';
   }

   @Override
   public final boolean nextIfNull() {
      if (this.ch == 'n' && this.offset + 2 < this.end && this.chars[this.offset] == 'u') {
         this.readNull();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void readNull() {
      char[] chars = this.chars;
      int offset = this.offset;
      if (chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
         char ch;
         if (offset + 3 == this.end) {
            ch = 26;
         } else {
            ch = chars[offset + 3];
         }

         offset += 4;

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.ch = ch;
         this.offset = offset;
      } else {
         throw new JSONException("json syntax error, not match null, offset " + offset);
      }
   }

   @Override
   public final BigDecimal readBigDecimal() {
      boolean valid = false;
      char[] chars = this.chars;
      char ch = this.ch;
      int offset = this.offset;
      boolean value = false;
      BigDecimal decimal = null;
      int quote = 0;
      if (ch == '"' || ch == '\'') {
         quote = ch;
         ch = chars[offset++];
         if (ch == quote) {
            this.ch = offset == this.end ? 26 : chars[offset++];
            this.offset = offset;
            this.nextIfComma();
            return null;
         }
      }

      int start = offset;
      if (ch == '-') {
         this.negative = true;
         ch = chars[offset++];
      } else {
         this.negative = false;
         if (ch == '+') {
            ch = chars[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
         valid = true;
         if (!overflow) {
            long r = longValue * 10L;
            if ((longValue | 10L) >>> 31 != 0L && r / 10L != longValue) {
               overflow = true;
            } else {
               longValue = r + (long)(ch - '0');
            }
         }

         if (offset == this.end) {
            ch = 26;
            offset++;
            break;
         }
      }

      if (longValue < 0L) {
         overflow = true;
      }

      this.scale = 0;
      if (ch == '.') {
         this.valueType = 2;

         for (ch = chars[offset++]; ch >= '0' && ch <= '9'; ch = chars[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long r = longValue * 10L;
               if ((longValue | 10L) >>> 31 != 0L && r / 10L != longValue) {
                  overflow = true;
               } else {
                  longValue = r + (long)(ch - '0');
               }
            }

            if (offset == this.end) {
               ch = 26;
               offset++;
               break;
            }
         }
      }

      int expValue = 0;
      if (ch == 'e' || ch == 'E') {
         ch = chars[offset++];
         boolean negativeExp;
         if ((negativeExp = ch == '-') || ch == '+') {
            ch = chars[offset++];
         }

         while (ch >= '0' && ch <= '9') {
            valid = true;
            int byteVal = ch - '0';
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = 26;
               offset++;
               break;
            }

            ch = chars[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 'n' && chars[offset++] == 'u' && chars[offset++] == 'l' && chars[offset++] == 'l') {
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : chars[offset];
            offset++;
            valid = true;
         } else if (ch == 't' && offset + 3 <= this.end && chars[offset] == 'r' && chars[offset + 1] == 'u' && chars[offset + 2] == 'e') {
            valid = true;
            offset += 3;
            value = true;
            decimal = BigDecimal.ONE;
            ch = offset == this.end ? 26 : chars[offset];
            offset++;
         } else if (ch == 'f'
            && offset + 4 <= this.end
            && chars[offset] == 'a'
            && chars[offset + 1] == 'l'
            && chars[offset + 2] == 's'
            && chars[offset + 3] == 'e') {
            valid = true;
            offset += 4;
            decimal = BigDecimal.ZERO;
            value = true;
            ch = offset == this.end ? 26 : chars[offset];
            offset++;
         } else {
            if (ch == '{' && quote == 0) {
               JSONObject jsonObject = new JSONObject();
               this.readObject(jsonObject, 0L);
               this.wasNull = false;
               return this.decimal(jsonObject);
            }

            if (ch == '[' && quote == 0) {
               List array = this.readArray();
               if (!array.isEmpty()) {
                  throw new JSONException(this.info());
               }

               this.wasNull = true;
               return null;
            }
         }
      }

      int len = offset - start;
      if (quote != 0) {
         if (ch != quote) {
            String str = this.readString();

            try {
               return TypeUtils.toBigDecimal(str);
            } catch (NumberFormatException var16) {
               throw new JSONException(this.info(var16.getMessage()), var16);
            }
         }

         ch = offset >= this.end ? 26 : chars[offset++];
      }

      if (!value) {
         if (expValue == 0 && !overflow && longValue != 0L) {
            decimal = BigDecimal.valueOf(this.negative ? -longValue : longValue, this.scale);
            value = true;
         }

         if (!value) {
            decimal = TypeUtils.parseBigDecimal(chars, start - 1, len);
         }

         if (ch == 'L' || ch == 'F' || ch == 'D' || ch == 'B' || ch == 'S') {
            ch = offset >= this.end ? 26 : chars[offset++];
         }
      }

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = ch;
         this.offset = offset;
         return decimal;
      }
   }

   @Override
   public final UUID readUUID() {
      char ch = this.ch;
      if (ch == 'n') {
         this.readNull();
         return null;
      } else if (ch != '"' && ch != '\'') {
         throw new JSONException(this.info("syntax error, can not read uuid"));
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         long hi = 0L;
         long lo = 0L;
         if (offset + 36 < chars.length
            && chars[offset + 36] == ch
            && chars[offset + 8] == '-'
            && chars[offset + 13] == '-'
            && chars[offset + 18] == '-'
            && chars[offset + 23] == '-') {
            for (int i = 0; i < 8; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            for (int i = 9; i < 13; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            for (int i = 14; i < 18; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            for (int i = 19; i < 23; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            for (int i = 24; i < 36; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            offset += 37;
         } else {
            if (offset + 32 >= chars.length || chars[offset + 32] != ch) {
               String str = this.readString();
               return str.isEmpty() ? null : UUID.fromString(str);
            }

            for (int i = 0; i < 16; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            for (int i = 16; i < 32; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[chars[offset + i] - '0'];
            }

            offset += 33;
         }

         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         this.offset = offset;
         if (this.comma = ch == ',') {
            this.next();
         } else {
            this.ch = ch;
         }

         return new UUID(hi, lo);
      }
   }

   @Override
   public final int getStringLength() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("string length only support string input");
      } else {
         char quote = this.ch;
         int len = 0;
         int i = this.offset;
         char[] chars = this.chars;
         int i8 = i + 8;
         if (i8 < this.end
            && i8 < chars.length
            && chars[i] != quote
            && chars[i + 1] != quote
            && chars[i + 2] != quote
            && chars[i + 3] != quote
            && chars[i + 4] != quote
            && chars[i + 5] != quote
            && chars[i + 6] != quote
            && chars[i + 7] != quote) {
            i += 8;
            len += 8;
         }

         while (i < this.end && chars[i] != quote) {
            i++;
            len++;
         }

         return len;
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime14() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime14(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 15;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime12() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime12(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 13;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime16() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime16(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 17;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime17() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime17(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 18;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime18() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime18(this.chars, this.offset);
         this.offset += 19;
         this.next();
         if (this.comma = this.ch == ',') {
            this.next();
         }

         return ldt;
      }
   }

   @Override
   protected final LocalTime readLocalTime5() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime5(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 6;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime6() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime6(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 7;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime7() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime7(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 8;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime8() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime8(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 9;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime9() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime8(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 10;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   public final LocalDate readLocalDate8() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate8(this.chars, this.offset);
         } catch (DateTimeException var3) {
            throw new JSONException(this.info("read date error"), var3);
         }

         this.offset += 9;
         this.next();
         if (this.comma = this.ch == ',') {
            this.next();
         }

         return ldt;
      }
   }

   @Override
   public final LocalDate readLocalDate9() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate9(this.chars, this.offset);
         } catch (DateTimeException var3) {
            throw new JSONException(this.info("read date error"), var3);
         }

         this.offset += 10;
         this.next();
         if (this.comma = this.ch == ',') {
            this.next();
         }

         return ldt;
      }
   }

   @Override
   public final LocalDate readLocalDate10() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate10(this.chars, this.offset);
         } catch (DateTimeException var3) {
            throw new JSONException(this.info("read date error"), var3);
         }

         if (ldt == null) {
            return null;
         } else {
            this.offset += 11;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   public final LocalDate readLocalDate11() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt = DateUtils.parseLocalDate11(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 12;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   public final LocalDate readLocalDate() {
      char[] chars = this.chars;
      int offset = this.offset;
      if (this.ch == '"' || this.ch == '\'') {
         JSONReader.Context context = this.context;
         if (context.dateFormat == null
            || context.formatyyyyMMddhhmmss19
            || context.formatyyyyMMddhhmmssT19
            || context.formatyyyyMMdd8
            || context.formatISO8601) {
            char quote = this.ch;
            int c10 = offset + 10;
            if (c10 < chars.length && c10 < this.end && chars[offset + 4] == '-' && chars[offset + 7] == '-' && chars[offset + 10] == quote) {
               char y0 = chars[offset];
               char y1 = chars[offset + 1];
               char y2 = chars[offset + 2];
               char y3 = chars[offset + 3];
               char m0 = chars[offset + 5];
               char m1 = chars[offset + 6];
               char d0 = chars[offset + 8];
               char d1 = chars[offset + 9];
               if (y0 >= '0' && y0 <= '9' && y1 >= '0' && y1 <= '9' && y2 >= '0' && y2 <= '9' && y3 >= '0' && y3 <= '9') {
                  int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
                  if (m0 >= '0' && m0 <= '9' && m1 >= '0' && m1 <= '9') {
                     int month = (m0 - '0') * 10 + (m1 - '0');
                     if (d0 >= '0' && d0 <= '9' && d1 >= '0' && d1 <= '9') {
                        int dom = (d0 - '0') * 10 + (d1 - '0');

                        LocalDate ldt;
                        try {
                           ldt = year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
                        } catch (DateTimeException var19) {
                           throw new JSONException(this.info("read date error"), var19);
                        }

                        this.offset = offset + 11;
                        this.next();
                        if (this.comma = this.ch == ',') {
                           this.next();
                        }

                        return ldt;
                     }
                  }
               }
            }

            int nextQuoteOffset = -1;
            int i = offset;

            for (int end = Math.min(offset + 17, this.end); i < end; i++) {
               if (chars[i] == quote) {
                  nextQuoteOffset = i;
               }
            }

            if (nextQuoteOffset != -1 && nextQuoteOffset - offset > 10 && chars[nextQuoteOffset - 6] == '-' && chars[nextQuoteOffset - 3] == '-') {
               i = TypeUtils.parseInt(chars, offset, nextQuoteOffset - offset - 6);
               int month = TypeUtils.parseInt(chars, nextQuoteOffset - 5, 2);
               int dayOfMonth = TypeUtils.parseInt(chars, nextQuoteOffset - 2, 2);
               LocalDate localDate = LocalDate.of(i, month, dayOfMonth);
               this.offset = nextQuoteOffset + 1;
               this.next();
               if (this.comma = this.ch == ',') {
                  this.next();
               }

               return localDate;
            }
         }
      }

      return super.readLocalDate();
   }

   @Override
   public final OffsetDateTime readOffsetDateTime() {
      char[] chars = this.chars;
      int offset = this.offset;
      JSONReader.Context context = this.context;
      if ((this.ch == '"' || this.ch == '\'')
         && (
            context.dateFormat == null || context.formatyyyyMMddhhmmss19 || context.formatyyyyMMddhhmmssT19 || context.formatyyyyMMdd8 || context.formatISO8601
         )) {
         char quote = this.ch;
         int off21 = offset + 19;
         char c10;
         if (off21 < chars.length
            && off21 < this.end
            && chars[offset + 4] == '-'
            && chars[offset + 7] == '-'
            && ((c10 = chars[offset + 10]) == ' ' || c10 == 'T')
            && chars[offset + 13] == ':'
            && chars[offset + 16] == ':') {
            char y0 = chars[offset];
            char y1 = chars[offset + 1];
            char y2 = chars[offset + 2];
            char y3 = chars[offset + 3];
            char m0 = chars[offset + 5];
            char m1 = chars[offset + 6];
            char d0 = chars[offset + 8];
            char d1 = chars[offset + 9];
            char h0 = chars[offset + 11];
            char h1 = chars[offset + 12];
            char i0 = chars[offset + 14];
            char i1 = chars[offset + 15];
            char s0 = chars[offset + 17];
            char s1 = chars[offset + 18];
            if (y0 < '0' || y0 > '9' || y1 < '0' || y1 > '9' || y2 < '0' || y2 > '9' || y3 < '0' || y3 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int year = (y0 - '0') * 1000 + (y1 - '0') * 100 + (y2 - '0') * 10 + (y3 - '0');
            if (m0 < '0' || m0 > '9' || m1 < '0' || m1 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int month = (m0 - '0') * 10 + (m1 - '0');
            if (d0 < '0' || d0 > '9' || d1 < '0' || d1 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int dom = (d0 - '0') * 10 + (d1 - '0');
            if (h0 < '0' || h0 > '9' || h1 < '0' || h1 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int hour = (h0 - '0') * 10 + (h1 - '0');
            if (i0 < '0' || i0 > '9' || i1 < '0' || i1 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int minute = (i0 - '0') * 10 + (i1 - '0');
            if (s0 < '0' || s0 > '9' || s1 < '0' || s1 > '9') {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int second = (s0 - '0') * 10 + (s1 - '0');

            LocalDate localDate;
            try {
               localDate = year == 0 && month == 0 && dom == 0 ? null : LocalDate.of(year, month, dom);
            } catch (DateTimeException var34) {
               throw new JSONException(this.info("read date error"), var34);
            }

            int nanoSize = -1;
            int len = 0;
            int start = offset + 19;
            int i = start;
            int end = offset + 31;

            while (true) {
               if (i < end && i < this.end && i < chars.length) {
                  if (chars[i] != quote || chars[i - 1] != 'Z') {
                     i++;
                     continue;
                  }

                  nanoSize = i - start - 2;
                  len = i - offset + 1;
               }

               if (nanoSize != -1 || len == 21) {
                  start = nanoSize <= 0 ? 0 : DateUtils.readNanos(chars, nanoSize, offset + 20);
                  LocalTime localTime = LocalTime.of(hour, minute, second, start);
                  LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
                  OffsetDateTime oft = OffsetDateTime.of(ldt, ZoneOffset.UTC);
                  this.offset += len;
                  this.next();
                  if (this.comma = this.ch == ',') {
                     this.next();
                  }

                  return oft;
               }
               break;
            }
         }
      }

      ZonedDateTime zdt = this.readZonedDateTime();
      return zdt == null ? null : zdt.toOffsetDateTime();
   }

   @Override
   public final OffsetTime readOffsetTime() {
      char[] chars = this.chars;
      int offset = this.offset;
      JSONReader.Context context = this.context;
      if ((this.ch == '"' || this.ch == '\'') && context.dateFormat == null) {
         char quote = this.ch;
         int off10 = offset + 8;
         if (off10 < chars.length && off10 < this.end && chars[offset + 2] == ':' && chars[offset + 5] == ':') {
            char h0 = chars[offset];
            char h1 = chars[offset + 1];
            char i0 = chars[offset + 3];
            char i1 = chars[offset + 4];
            char s0 = chars[offset + 6];
            char s1 = chars[offset + 7];
            if (h0 >= '0' && h0 <= '9' && h1 >= '0' && h1 <= '9') {
               int hour = (h0 - '0') * 10 + (h1 - '0');
               if (i0 >= '0' && i0 <= '9' && i1 >= '0' && i1 <= '9') {
                  int minute = (i0 - '0') * 10 + (i1 - '0');
                  if (s0 >= '0' && s0 <= '9' && s1 >= '0' && s1 <= '9') {
                     int second = (s0 - '0') * 10 + (s1 - '0');
                     int nanoSize = -1;
                     int len = 0;
                     int start = offset + 8;
                     int zoneOffset = start;

                     for (int zoneOffsetSize = offset + 25; zoneOffset < zoneOffsetSize && zoneOffset < this.end && zoneOffset < chars.length; zoneOffset++) {
                        char b = chars[zoneOffset];
                        if (nanoSize == -1 && (b == 'Z' || b == '+' || b == '-')) {
                           nanoSize = zoneOffset - start - 1;
                        }

                        if (b == quote) {
                           len = zoneOffset - offset;
                           break;
                        }
                     }

                     start = nanoSize <= 0 ? 0 : DateUtils.readNanos(chars, nanoSize, offset + 9);
                     int zoneOffsetSize = len - 9 - nanoSize;
                     ZoneOffset zoneOffsetx;
                     if (zoneOffsetSize <= 1) {
                        zoneOffsetx = ZoneOffset.UTC;
                     } else {
                        String zonedId = new String(chars, offset + 9 + nanoSize, zoneOffsetSize);
                        zoneOffsetx = ZoneOffset.of(zonedId);
                     }

                     LocalTime localTime = LocalTime.of(hour, minute, second, start);
                     OffsetTime oft = OffsetTime.of(localTime, zoneOffsetx);
                     this.offset += len + 1;
                     this.next();
                     if (this.comma = this.ch == ',') {
                        this.next();
                     }

                     return oft;
                  }

                  throw new JSONException(this.info("illegal offsetTime"));
               }

               throw new JSONException(this.info("illegal offsetTime"));
            }

            throw new JSONException(this.info("illegal offsetTime"));
         }
      }

      throw new JSONException(this.info("illegal offsetTime"));
   }

   @Override
   protected final ZonedDateTime readZonedDateTimeX(int len) {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else if (len < 19) {
         return null;
      } else {
         char[] chars = this.chars;
         ZonedDateTime zdt;
         if (len == 30 && chars[this.offset + 29] == 'Z') {
            LocalDateTime ldt = DateUtils.parseLocalDateTime29(chars, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 29 && chars[this.offset + 28] == 'Z') {
            LocalDateTime ldt = DateUtils.parseLocalDateTime28(chars, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 28 && chars[this.offset + 27] == 'Z') {
            LocalDateTime ldt = DateUtils.parseLocalDateTime27(chars, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 27 && chars[this.offset + 26] == 'Z') {
            LocalDateTime ldt = DateUtils.parseLocalDateTime26(chars, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else {
            zdt = DateUtils.parseZonedDateTime(chars, this.offset, len, this.context.zoneId);
         }

         if (zdt == null) {
            return null;
         } else {
            this.offset += len + 1;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return zdt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime19() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime19(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 20;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime20() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime20(this.chars, this.offset);
         if (ldt == null) {
            return null;
         } else {
            this.offset += 21;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   public final long readMillis19() {
      char quote = this.ch;
      if (quote != '"' && quote != '\'') {
         throw new JSONException("date only support string input");
      } else if (this.offset + 18 >= this.end) {
         this.wasNull = true;
         return 0L;
      } else {
         long millis = DateUtils.parseMillis19(this.chars, this.offset, this.context.zoneId);
         if (this.chars[this.offset + 19] != quote) {
            throw new JSONException(this.info("illegal date input"));
         } else {
            this.offset += 20;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return millis;
         }
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTimeX(int len) {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt;
         if (this.chars[this.offset + len - 1] == 'Z') {
            ZonedDateTime zdt = DateUtils.parseZonedDateTime(this.chars, this.offset, len);
            ldt = zdt.toInstant().atZone(this.context.getZoneId()).toLocalDateTime();
         } else {
            ldt = DateUtils.parseLocalDateTimeX(this.chars, this.offset, len);
         }

         if (ldt == null) {
            return null;
         } else {
            this.offset += len + 1;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return ldt;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime10() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime10(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 11;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime11() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime11(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 12;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime12() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime12(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 13;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   protected final LocalTime readLocalTime18() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime18(this.chars, this.offset);
         if (time == null) {
            return null;
         } else {
            this.offset += 19;
            this.next();
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return time;
         }
      }
   }

   @Override
   public final String readPattern() {
      if (this.ch != '/') {
         throw new JSONException("illegal pattern");
      } else {
         char[] chars = this.chars;
         int offset = this.offset;
         int start = offset;

         while (offset < this.end && chars[offset] != '/') {
            offset++;
         }

         String str = new String(chars, start, offset - start);
         offset++;
         char ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (this.comma = ch == ',') {
            ch = offset == this.end ? 26 : chars[offset++];

            while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : chars[offset++];
            }
         }

         this.offset = offset;
         this.ch = ch;
         return str;
      }
   }

   @Override
   public final boolean readBoolValue() {
      this.wasNull = false;
      char[] chars = this.chars;
      int offset = this.offset;
      char ch = this.ch;
      boolean val;
      if (ch == 't' && offset + 2 < chars.length && chars[offset] == 'r' && chars[offset + 1] == 'u' && chars[offset + 2] == 'e') {
         offset += 3;
         val = true;
      } else if (ch == 'f'
         && offset + 3 < chars.length
         && chars[offset] == 'a'
         && chars[offset + 1] == 'l'
         && chars[offset + 2] == 's'
         && chars[offset + 3] == 'e') {
         offset += 4;
         val = false;
      } else {
         if (ch == '-' || ch >= '0' && ch <= '9') {
            this.readNumber();
            if (this.valueType != 1) {
               return false;
            }

            if ((this.context.features & JSONReader.Feature.NonZeroNumberCastToBooleanAsTrue.mask) != 0L) {
               return this.mag0 != 0 || this.mag1 != 0 || this.mag2 != 0 || this.mag3 != 0;
            }

            return this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 == 1;
         }

         if (ch == 'n' && offset + 2 < chars.length && chars[offset] == 'u' && chars[offset + 1] == 'l' && chars[offset + 2] == 'l') {
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("boolean value not support input null"));
            }

            this.wasNull = true;
            offset += 3;
            val = false;
         } else {
            if (ch != '"') {
               if (ch == '[') {
                  this.next();
                  val = this.readBoolValue();
                  if (!this.nextIfMatch(']')) {
                     throw new JSONException("not closed square brackets, expect ] but found : " + ch);
                  }

                  return val;
               }

               throw new JSONException("syntax error : " + ch);
            }

            if (offset + 1 >= chars.length || chars[offset + 1] != '"') {
               String str = this.readString();
               if ("true".equalsIgnoreCase(str)) {
                  return true;
               } else if ("false".equalsIgnoreCase(str)) {
                  return false;
               } else if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                  throw new JSONException("can not convert to boolean : " + str);
               } else {
                  this.wasNull = true;
                  return false;
               }
            }

            char c0 = chars[offset];
            offset += 2;
            if (c0 != '0' && c0 != 'N') {
               if (c0 != '1' && c0 != 'Y') {
                  throw new JSONException("can not convert to boolean : " + c0);
               }

               val = true;
            } else {
               val = false;
            }
         }
      }

      ch = offset == this.end ? 26 : chars[offset++];

      while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == ',') {
         ch = offset == this.end ? 26 : chars[offset++];

         while (ch <= ' ' && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }
      }

      this.offset = offset;
      this.ch = ch;
      return val;
   }

   @Override
   public final String info(String message) {
      int line = 1;
      int column = 0;

      for (int i = 0; i < this.offset & i < this.end; column++) {
         if (this.chars[i] == '\n') {
            column = 0;
            line++;
         }

         i++;
      }

      StringBuilder buf = new StringBuilder();
      if (message != null && !message.isEmpty()) {
         buf.append(message).append(", ");
      }

      buf.append("offset ")
         .append(this.offset)
         .append(", character ")
         .append(this.ch)
         .append(", line ")
         .append(line)
         .append(", column ")
         .append(column)
         .append(", fastjson-version ")
         .append("2.0.53")
         .append((char)(line > 1 ? '\n' : ' '));
      int MAX_OUTPUT_LENGTH = 65535;
      buf.append(this.chars, this.start, Math.min(this.length, 65535));
      return buf.toString();
   }

   @Override
   public final void close() {
      if (this.cacheIndex != -1 && this.chars.length < 4194304) {
         JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[this.cacheIndex];
         JSONFactory.CHARS_UPDATER.lazySet(cacheItem, this.chars);
      }

      if (this.input != null) {
         try {
            this.input.close();
         } catch (IOException var2) {
         }
      }
   }

   @Override
   public final int getRawInt() {
      return this.offset + 3 < this.chars.length ? getInt(this.chars, this.offset - 1) : 0;
   }

   static int getInt(char[] chars, int offset) {
      long int64Val = JDKUtils.UNSAFE.getLong(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(offset << 1));
      if ((int64Val & CHAR_MASK) != 0L) {
         return 0;
      } else {
         if (JDKUtils.BIG_ENDIAN) {
            int64Val >>= 8;
         }

         return (int)(int64Val & 255L | (int64Val & 16711680L) >> 8 | (int64Val & 1095216660480L) >> 16 | (int64Val & 71776119061217280L) >> 24);
      }
   }

   @Override
   public final long getRawLong() {
      return this.offset + 7 < this.chars.length ? getLong(this.chars, this.offset - 1) : 0L;
   }

   static long getLong(char[] chars, int offset) {
      long arrayOffset = JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(offset << 1);
      long int64Val0 = JDKUtils.UNSAFE.getLong(chars, arrayOffset);
      long int64Val1 = JDKUtils.UNSAFE.getLong(chars, arrayOffset + 8L);
      if (((int64Val0 | int64Val1) & CHAR_MASK) != 0L) {
         return 0L;
      } else {
         if (JDKUtils.BIG_ENDIAN) {
            int64Val0 >>= 8;
            int64Val1 >>= 8;
         }

         return int64Val0 & 255L
            | (int64Val0 & 16711680L) >> 8
            | (int64Val0 & 1095216660480L) >> 16
            | (int64Val0 & 71776119061217280L) >> 24
            | (int64Val1 & 255L) << 32
            | (int64Val1 & 16711680L) << 24
            | (int64Val1 & 1095216660480L) << 16
            | (int64Val1 & 71776119061217280L) << 8;
      }
   }

   @Override
   public final boolean nextIfName8Match0() {
      char[] chars = this.chars;
      int offset = this.offset;
      offset += 7;
      if (offset == this.end) {
         this.ch = 26;
         return false;
      } else {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName8Match1() {
      char[] chars = this.chars;
      int offset = this.offset;
      offset += 8;
      if (offset < this.end && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName8Match2() {
      char[] chars = this.chars;
      int offset = this.offset;
      offset += 9;
      if (offset < this.end && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match2() {
      char[] chars = this.chars;
      int offset = this.offset + 4;
      if (offset < this.end && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match3() {
      char[] chars = this.chars;
      int offset = this.offset + 5;
      if (offset < this.end && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[++offset];
         }

         this.offset = offset + 1;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match4(byte c4) {
      char[] chars = this.chars;
      int offset = this.offset + 6;
      if (offset < this.end && chars[offset - 3] == c4 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[++offset];
         }

         this.offset = offset + 1;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match5(int name1) {
      char[] chars = this.chars;
      int offset = this.offset + 7;
      if (offset < this.end && getInt(chars, offset - 4) == name1) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match6(int name1) {
      char[] chars = this.chars;
      int offset = this.offset + 8;
      if (offset < this.end && getInt(chars, offset - 5) == name1 && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match7(int name1) {
      char[] chars = this.chars;
      int offset = this.offset + 9;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 6) == name1 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match8(int name1, byte c8) {
      char[] chars = this.chars;
      int offset = this.offset + 10;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 7) == name1 && chars[offset - 3] == c8 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match9(long name1) {
      char[] chars = this.chars;
      int offset = this.offset + 11;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 8) != name1) {
         return false;
      } else {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName4Match10(long name1) {
      char[] chars = this.chars;
      int offset = this.offset + 12;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 9) == name1 && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match11(long name1) {
      char[] chars = this.chars;
      int offset = this.offset + 13;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 10) == name1 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match12(long name1, byte name2) {
      char[] bytes = this.chars;
      int offset = this.offset + 14;
      if (offset >= this.end) {
         return false;
      } else if (getLong(bytes, offset - 11) == name1 && bytes[offset - 3] == name2 && bytes[offset - 2] == '"' && bytes[offset - 1] == ':') {
         char c = bytes[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match13(long name1, int name2) {
      char[] chars = this.chars;
      int offset = this.offset + 15;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 12) == name1 && getInt(chars, offset - 4) == name2) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match14(long name1, int name2) {
      char[] chars = this.chars;
      int offset = this.offset + 16;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 13) == name1 && getInt(chars, offset - 5) == name2 && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match15(long name1, int name2) {
      char[] chars = this.chars;
      int offset = this.offset + 17;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 14) == name1 && getInt(chars, offset - 6) == name2 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match16(long name1, int name2, byte name3) {
      char[] chars = this.chars;
      int offset = this.offset + 18;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 15) == name1
         && getInt(chars, offset - 7) == name2
         && chars[offset - 3] == name3
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match17(long name1, long name2) {
      char[] chars = this.chars;
      int offset = this.offset + 19;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 16) == name1 && getLong(chars, offset - 8) == name2) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match18(long name1, long name2) {
      char[] chars = this.chars;
      int offset = this.offset + 20;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 17) == name1 && getLong(chars, offset - 9) == name2 && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match19(long name1, long name2) {
      char[] chars = this.chars;
      int offset = this.offset + 21;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 18) == name1 && getLong(chars, offset - 10) == name2 && chars[offset - 2] == '"' && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match20(long name1, long name2, byte name3) {
      char[] chars = this.chars;
      int offset = this.offset + 22;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 19) == name1
         && getLong(chars, offset - 11) == name2
         && chars[offset - 3] == name3
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match21(long name1, long name2, int name3) {
      char[] chars = this.chars;
      int offset = this.offset + 23;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 20) == name1 && getLong(chars, offset - 12) == name2 && getInt(chars, offset - 4) == name3) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match22(long name1, long name2, int name3) {
      char[] chars = this.chars;
      int offset = this.offset + 24;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 21) == name1 && getLong(chars, offset - 13) == name2 && getInt(chars, offset - 5) == name3 && chars[offset - 1] == ':'
         )
       {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match23(long name1, long name2, int name3) {
      char[] chars = this.chars;
      int offset = this.offset + 25;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 22) == name1
         && getLong(chars, offset - 14) == name2
         && getInt(chars, offset - 6) == name3
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match24(long name1, long name2, int name3, byte name4) {
      char[] chars = this.chars;
      int offset = this.offset + 26;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 23) == name1
         && getLong(chars, offset - 15) == name2
         && getInt(chars, offset - 7) == name3
         && chars[offset - 3] == name4
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match25(long name1, long name2, long name3) {
      char[] chars = this.chars;
      int offset = this.offset + 27;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 24) == name1 && getLong(chars, offset - 16) == name2 && getLong(chars, offset - 8) == name3) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match26(long name1, long name2, long name3) {
      char[] bytes = this.chars;
      int offset = this.offset + 28;
      if (offset >= this.end) {
         return false;
      } else if (getLong(bytes, offset - 25) == name1
         && getLong(bytes, offset - 17) == name2
         && getLong(bytes, offset - 9) == name3
         && bytes[offset - 1] == ':') {
         char c = this.chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = this.chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match27(long name1, long name2, long name3) {
      char[] bytes = this.chars;
      int offset = this.offset + 29;
      if (offset >= this.end) {
         return false;
      } else if (getLong(bytes, offset - 26) == name1
         && getLong(bytes, offset - 18) == name2
         && getLong(bytes, offset - 10) == name3
         && bytes[offset - 2] == '"'
         && bytes[offset - 1] == ':') {
         char c = this.chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = this.chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match28(long name1, long name2, long name3, byte c29) {
      char[] chars = this.chars;
      int offset = this.offset + 30;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 27) == name1
         && getLong(chars, offset - 19) == name2
         && getLong(chars, offset - 11) == name3
         && chars[offset - 3] == c29
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match29(long name1, long name2, long name3, int name4) {
      char[] chars = this.chars;
      int offset = this.offset + 31;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 28) == name1
         && getLong(chars, offset - 20) == name2
         && getLong(chars, offset - 12) == name3
         && getInt(chars, offset - 4) == name4) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match30(long name1, long name2, long name3, int name4) {
      char[] chars = this.chars;
      int offset = this.offset + 32;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 29) == name1
         && getLong(chars, offset - 21) == name2
         && getLong(chars, offset - 13) == name3
         && getInt(chars, offset - 5) == name4
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match31(long name1, long name2, long name3, int name4) {
      char[] chars = this.chars;
      int offset = this.offset + 33;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 30) == name1
         && getLong(chars, offset - 22) == name2
         && getLong(chars, offset - 14) == name3
         && getInt(chars, offset - 6) == name4
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match32(long name1, long name2, long name3, int name4, byte c32) {
      char[] chars = this.chars;
      int offset = this.offset + 34;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 31) == name1
         && getLong(chars, offset - 23) == name2
         && getLong(chars, offset - 15) == name3
         && getInt(chars, offset - 7) == name4
         && chars[offset - 3] == c32
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match33(long name1, long name2, long name3, long name4) {
      char[] chars = this.chars;
      int offset = this.offset + 35;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 32) == name1
         && getLong(chars, offset - 24) == name2
         && getLong(chars, offset - 16) == name3
         && getLong(chars, offset - 8) == name4) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match34(long name1, long name2, long name3, long name4) {
      char[] chars = this.chars;
      int offset = this.offset + 36;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 33) == name1
         && getLong(chars, offset - 25) == name2
         && getLong(chars, offset - 17) == name3
         && getLong(chars, offset - 9) == name4
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match35(long name1, long name2, long name3, long name4) {
      char[] chars = this.chars;
      int offset = this.offset + 37;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 34) == name1
         && getLong(chars, offset - 26) == name2
         && getLong(chars, offset - 18) == name3
         && getLong(chars, offset - 10) == name4
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match36(long name1, long name2, long name3, long name4, byte c36) {
      char[] chars = this.chars;
      int offset = this.offset + 38;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 35) == name1
         && getLong(chars, offset - 27) == name2
         && getLong(chars, offset - 19) == name3
         && getLong(chars, offset - 11) == name4
         && chars[offset - 3] == c36
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match37(long name1, long name2, long name3, long name4, int name5) {
      char[] chars = this.chars;
      int offset = this.offset + 39;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 36) == name1
         && getLong(chars, offset - 28) == name2
         && getLong(chars, offset - 20) == name3
         && getLong(chars, offset - 12) == name4
         && getInt(chars, offset - 4) == name5) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match38(long name1, long name2, long name3, long name4, int name5) {
      char[] chars = this.chars;
      int offset = this.offset + 40;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 37) == name1
         && getLong(chars, offset - 29) == name2
         && getLong(chars, offset - 21) == name3
         && getLong(chars, offset - 13) == name4
         && getInt(chars, offset - 5) == name5
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match39(long name1, long name2, long name3, long name4, int name5) {
      char[] chars = this.chars;
      int offset = this.offset + 41;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 38) == name1
         && getLong(chars, offset - 30) == name2
         && getLong(chars, offset - 22) == name3
         && getLong(chars, offset - 14) == name4
         && getInt(chars, offset - 6) == name5
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match40(long name1, long name2, long name3, long name4, int name5, byte c40) {
      char[] chars = this.chars;
      int offset = this.offset + 42;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 39) == name1
         && getLong(chars, offset - 31) == name2
         && getLong(chars, offset - 23) == name3
         && getLong(chars, offset - 15) == name4
         && getInt(chars, offset - 7) == name5
         && chars[offset - 3] == c40
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match41(long name1, long name2, long name3, long name4, long name5) {
      char[] chars = this.chars;
      int offset = this.offset + 43;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 40) == name1
         && getLong(chars, offset - 32) == name2
         && getLong(chars, offset - 24) == name3
         && getLong(chars, offset - 16) == name4
         && getLong(chars, offset - 8) == name5) {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match42(long name1, long name2, long name3, long name4, long name5) {
      char[] chars = this.chars;
      int offset = this.offset + 44;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 41) == name1
         && getLong(chars, offset - 33) == name2
         && getLong(chars, offset - 25) == name3
         && getLong(chars, offset - 17) == name4
         && getLong(chars, offset - 9) == name5
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match43(long name1, long name2, long name3, long name4, long name5) {
      char[] chars = this.chars;
      int offset = this.offset + 45;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 42) == name1
         && getLong(chars, offset - 34) == name2
         && getLong(chars, offset - 26) == name3
         && getLong(chars, offset - 18) == name4
         && getLong(chars, offset - 10) == name5
         && chars[offset - 2] == '"'
         && chars[offset - 1] == ':') {
         char c = chars[offset++];

         while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
            c = chars[offset++];
         }

         this.offset = offset;
         this.ch = c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match2() {
      char[] chars = this.chars;
      int offset = this.offset + 3;
      if (offset >= this.end) {
         return false;
      } else {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match3() {
      char[] chars = this.chars;
      int offset = this.offset + 4;
      if (offset >= this.end) {
         return false;
      } else if (chars[offset - 1] != '"') {
         return false;
      } else {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match4(byte c4) {
      char[] chars = this.chars;
      int offset = this.offset + 5;
      if (offset >= this.end) {
         return false;
      } else if (chars[offset - 2] == c4 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match5(byte c4, byte c5) {
      char[] chars = this.chars;
      int offset = this.offset + 6;
      if (offset >= this.end) {
         return false;
      } else if (chars[offset - 3] == c4 && chars[offset - 2] == c5 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match6(int name1) {
      char[] chars = this.chars;
      int offset = this.offset + 7;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 4) != name1) {
         return false;
      } else {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match7(int name1) {
      char[] chars = this.chars;
      int offset = this.offset + 8;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 5) == name1 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match8(int name1, byte c8) {
      char[] chars = this.chars;
      int offset = this.offset + 9;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 6) == name1 && chars[offset - 2] == c8 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match9(int name1, byte c8, byte c9) {
      char[] chars = this.chars;
      int offset = this.offset + 10;
      if (offset >= this.end) {
         return false;
      } else if (getInt(chars, offset - 7) == name1 && chars[offset - 3] == c8 && chars[offset - 2] == c9 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match10(long name1) {
      char[] chars = this.chars;
      int offset = this.offset + 11;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 8) != name1) {
         return false;
      } else {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match11(long name1) {
      char[] chars = this.chars;
      int offset = this.offset + 12;
      if (offset >= this.end) {
         return false;
      } else if (getLong(chars, offset - 9) == name1 && chars[offset - 1] == '"') {
         char c = chars[offset];
         if (c != ',' && c != '}' && c != ']') {
            return false;
         } else {
            if (c == ',') {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : chars[offset];
            }

            while (c <= ' ' && (1L << c & 4294981376L) != 0L) {
               c = chars[++offset];
            }

            this.offset = offset + 1;
            this.ch = c;
            return true;
         }
      } else {
         return false;
      }
   }
}
