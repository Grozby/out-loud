package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.ValueConsumer;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

class JSONReaderUTF8 extends JSONReader {
   protected final byte[] bytes;
   protected final int length;
   protected final int start;
   protected final int end;
   protected int nameBegin;
   protected int nameEnd;
   protected int nameLength;
   protected boolean nameAscii;
   protected int referenceBegin;
   protected final InputStream in;
   protected JSONFactory.CacheItem cacheItem;
   protected char[] charBuf;

   JSONReaderUTF8(JSONReader.Context ctx, InputStream is) {
      super(ctx, false, true);
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      this.cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
      int bufferSize = ctx.bufferSize;
      if (bytes == null) {
         bytes = new byte[bufferSize];
      }

      int off = 0;

      try {
         while (true) {
            int n = is.read(bytes, off, bytes.length - off);
            if (n == -1) {
               break;
            }

            off += n;
            if (off == bytes.length) {
               bytes = Arrays.copyOf(bytes, bytes.length + bufferSize);
            }
         }
      } catch (IOException var8) {
         throw new JSONException("read error", var8);
      }

      this.bytes = bytes;
      this.offset = 0;
      this.length = off;
      this.in = is;
      this.start = 0;
      this.end = this.length;
      this.next();
      if (this.ch == '/') {
         this.skipComment();
      }
   }

   JSONReaderUTF8(JSONReader.Context ctx, ByteBuffer buffer) {
      super(ctx, false, true);
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      this.cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
      int remaining = buffer.remaining();
      if (bytes == null || bytes.length < remaining) {
         bytes = new byte[remaining];
      }

      buffer.get(bytes, 0, remaining);
      this.bytes = bytes;
      this.offset = 0;
      this.length = remaining;
      this.in = null;
      this.start = 0;
      this.end = this.length;
      this.next();
      if (this.ch == '/') {
         this.skipComment();
      }
   }

   JSONReaderUTF8(JSONReader.Context ctx, String str, byte[] bytes, int offset, int length) {
      super(ctx, false, true);
      this.bytes = bytes;
      this.offset = offset;
      this.length = length;
      this.in = null;
      this.start = offset;
      this.end = offset + length;
      this.cacheItem = null;
      this.next();
   }

   private void char_utf8(int ch, int offset) {
      byte[] bytes = this.bytes;
      switch ((ch & 0xFF) >> 4) {
         case 12:
         case 13:
            ch = char2_utf8(ch & 0xFF, bytes[offset++], offset);
            break;
         case 14:
            ch = char2_utf8(ch & 0xFF, bytes[offset], bytes[offset + 1], offset);
            offset += 2;
            break;
         default:
            if (ch >> 3 != -2) {
               throw new JSONException("malformed input around byte " + offset);
            }

            int c2 = bytes[offset];
            int c3 = bytes[offset + 1];
            int c4 = bytes[offset + 2];
            ch = ch << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
            offset += 3;
      }

      this.ch = (char)ch;
      this.offset = offset;
   }

   static int char2_utf8(int ch, int char2, int offset) {
      if ((char2 & 192) != 128) {
         throw new JSONException("malformed input around byte " + offset);
      } else {
         return (ch & 31) << 6 | char2 & 63;
      }
   }

   static int char2_utf8(int ch, int char2, int char3, int offset) {
      if ((char2 & 192) == 128 && (char3 & 192) == 128) {
         return (ch & 15) << 12 | (char2 & 63) << 6 | char3 & 63;
      } else {
         throw new JSONException("malformed input around byte " + offset);
      }
   }

   static void char2_utf8(byte[] bytes, int offset, int c, char[] chars, int charPos) {
      if (c >> 3 == -2) {
         int c2 = bytes[offset + 1];
         int c3 = bytes[offset + 2];
         int c4 = bytes[offset + 3];
         int uc = c << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
         if ((c2 & 192) == 128 && (c3 & 192) == 128 && (c4 & 192) == 128 && uc >= 65536 && uc < 1114112) {
            chars[charPos] = (char)((uc >>> 10) + 55232);
            chars[charPos + 1] = (char)((uc & 1023) + 56320);
         } else {
            throw new JSONException("malformed input around byte " + offset);
         }
      } else {
         throw new JSONException("malformed input around byte " + offset);
      }
   }

   @Override
   public final boolean nextIfMatch(char e) {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (ch != e) {
         return false;
      } else {
         int var5 = offset == this.end ? 26 : bytes[offset++];

         while (var5 == 0 || var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
            var5 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var5 < 0) {
            this.char_utf8(var5, offset);
            return true;
         } else {
            this.offset = offset;
            this.ch = (char)var5;
            if (var5 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public final boolean nextIfComma() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (ch != 44) {
         return false;
      } else {
         int var4 = offset == this.end ? 26 : bytes[offset++];

         while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
            var4 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var4 < 0) {
            this.char_utf8(var4, offset);
            return true;
         } else {
            this.offset = offset;
            this.ch = (char)var4;
            if (var4 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public final boolean nextIfArrayStart() {
      int ch = this.ch;
      if (ch != 91) {
         return false;
      } else {
         byte[] bytes = this.bytes;
         int offset = this.offset;
         int var4 = offset == this.end ? 26 : bytes[offset++];

         while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
            var4 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var4 < 0) {
            this.char_utf8(var4, offset);
            return true;
         } else {
            this.ch = (char)var4;
            this.offset = offset;
            if (var4 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public final boolean nextIfArrayEnd() {
      int ch = this.ch;
      byte[] bytes = this.bytes;
      int offset = this.offset;
      if (ch != 93) {
         return false;
      } else {
         int var4 = offset == this.end ? 26 : bytes[offset++];

         while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
            var4 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var4 == 44) {
            this.comma = true;
            var4 = offset == this.end ? 26 : bytes[offset++];

            while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
               var4 = offset == this.end ? 26 : bytes[offset++];
            }
         }

         if (var4 < 0) {
            this.char_utf8(var4, offset);
            return true;
         } else {
            this.ch = (char)var4;
            this.offset = offset;
            if (var4 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public final boolean nextIfSet() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;
      if (ch == 83 && offset + 1 < this.end && bytes[offset] == 101 && bytes[offset + 1] == 116) {
         offset += 2;
         int var5 = offset == this.end ? 26 : bytes[offset++];

         while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
            var5 = offset == this.end ? 26 : bytes[offset++];
         }

         this.offset = offset;
         this.ch = (char)var5;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfInfinity() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;
      if (ch == 73
         && offset + 6 < this.end
         && bytes[offset] == 110
         && bytes[offset + 1] == 102
         && bytes[offset + 2] == 105
         && bytes[offset + 3] == 110
         && bytes[offset + 4] == 105
         && bytes[offset + 5] == 116
         && bytes[offset + 6] == 121) {
         offset += 7;
         int var5 = offset == this.end ? 26 : bytes[offset++];

         while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
            var5 = offset == this.end ? 26 : bytes[offset++];
         }

         this.offset = offset;
         this.ch = (char)var5;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfObjectStart() {
      int ch = this.ch;
      if (ch != 123) {
         return false;
      } else {
         byte[] bytes = this.bytes;
         int offset = this.offset;
         int var4 = offset == this.end ? 26 : bytes[offset++];

         while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
            var4 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var4 < 0) {
            this.char_utf8(var4, offset);
            return true;
         } else {
            this.ch = (char)var4;
            this.offset = offset;
            if (var4 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public final boolean nextIfObjectEnd() {
      int ch = this.ch;
      byte[] bytes = this.bytes;
      int offset = this.offset;
      if (ch != 125) {
         return false;
      } else {
         int var4 = offset == this.end ? 26 : bytes[offset++];

         while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
            var4 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var4 == 44) {
            this.comma = true;
            var4 = offset == this.end ? 26 : bytes[offset++];

            while (var4 == 0 || var4 <= 32 && (1L << var4 & 4294981376L) != 0L) {
               var4 = offset == this.end ? 26 : bytes[offset++];
            }
         }

         if (var4 < 0) {
            this.char_utf8(var4, offset);
            return true;
         } else {
            this.ch = (char)var4;
            this.offset = offset;
            if (var4 == 47) {
               this.skipComment();
            }

            return true;
         }
      }
   }

   @Override
   public void next() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = offset >= this.end ? 26 : bytes[offset++];

      while (ch == 0 || ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (ch < 0) {
         this.char_utf8(ch, offset);
      } else {
         this.offset = offset;
         this.ch = (char)ch;
         if (ch == 47) {
            this.skipComment();
         }
      }
   }

   @Override
   public long readFieldNameHashCodeUnquote() {
      this.nameEscape = false;
      int offset = this.offset;
      int end = this.end;
      byte[] bytes = this.bytes;
      int ch = this.ch;
      this.nameBegin = offset - 1;
      int first = ch;
      long nameValue = 0L;

      label182:
      for (int i = 0; offset <= end; i++) {
         switch (ch) {
            case 8:
            case 9:
            case 10:
            case 12:
            case 13:
            case 26:
            case 32:
            case 33:
            case 38:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 58:
            case 60:
            case 61:
            case 62:
            case 91:
            case 93:
            case 123:
            case 124:
            case 125:
               this.nameLength = i;
               this.nameEnd = ch == 26 ? offset : offset - 1;
               if (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
                  ch = offset == end ? 26 : (char)bytes[offset++];
               }
               break label182;
            case 11:
            case 14:
            case 15:
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
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 34:
            case 35:
            case 36:
            case 37:
            case 39:
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
            case 59:
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
            case 92:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
         }

         if (ch == 92) {
            this.nameEscape = true;
            ch = (char)bytes[offset++];
            switch (ch) {
               case 34:
               case 42:
               case 43:
               case 45:
               case 46:
               case 47:
               case 58:
               case 60:
               case 61:
               case 62:
               case 64:
               case 92:
                  break;
               case 117:
                  ch = char4(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
                  offset += 4;
                  break;
               case 120:
                  ch = char2(bytes[offset], bytes[offset + 1]);
                  offset += 2;
                  break;
               default:
                  ch = this.char1(ch);
            }
         }

         if (ch < 0) {
            switch ((ch & 0xFF) >> 4) {
               case 12:
               case 13:
                  ch = char2_utf8(ch & 0xFF, bytes[offset++], offset);
                  break;
               case 14:
                  ch = char2_utf8(ch & 0xFF, bytes[offset], bytes[offset + 1], offset);
                  offset += 2;
                  break;
               default:
                  if (ch >> 3 != -2) {
                     throw new JSONException("malformed input around byte " + offset);
                  }

                  int c2 = bytes[offset];
                  int c3 = bytes[offset + 1];
                  int c4 = bytes[offset + 2];
                  ch = ch << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
            }
         }

         if (ch > 255 || i >= 8 || i == 0 && ch == 0) {
            nameValue = 0L;
            ch = first;
            offset = this.nameBegin + 1;
            break;
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

         ch = offset == end ? 26 : bytes[offset++];
      }

      long hashCode;
      if (nameValue != 0L) {
         hashCode = nameValue;
      } else {
         hashCode = -3750763034362895579L;
         int i = 0;

         label158:
         while (true) {
            if (ch == 92) {
               this.nameEscape = true;
               ch = bytes[offset++];
               switch (ch) {
                  case 34:
                  case 42:
                  case 43:
                  case 45:
                  case 46:
                  case 47:
                  case 58:
                  case 60:
                  case 61:
                  case 62:
                  case 64:
                  case 92:
                     break;
                  case 117:
                     ch = char4(bytes[offset], bytes[offset + 1], bytes[offset + 2], bytes[offset + 3]);
                     offset += 4;
                     break;
                  case 120:
                     ch = char2(bytes[offset], bytes[offset + 1]);
                     offset += 2;
                     break;
                  default:
                     ch = this.char1(ch);
               }

               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
               ch = offset == end ? 26 : bytes[offset++];
            } else {
               switch (ch) {
                  case 8:
                  case 9:
                  case 10:
                  case 12:
                  case 13:
                  case 26:
                  case 32:
                  case 33:
                  case 40:
                  case 41:
                  case 42:
                  case 43:
                  case 44:
                  case 45:
                  case 46:
                  case 47:
                  case 58:
                  case 60:
                  case 61:
                  case 62:
                  case 91:
                  case 93:
                  case 123:
                  case 125:
                     this.nameLength = i;
                     this.nameEnd = ch == 26 ? offset : offset - 1;

                     while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
                        ch = offset == end ? 26 : bytes[offset++];
                     }
                     break label158;
                  default:
                     if (ch < 0) {
                        switch ((ch & 0xFF) >> 4) {
                           case 12:
                           case 13:
                              ch = char2_utf8(ch & 0xFF, bytes[offset++], offset);
                              this.nameAscii = false;
                              break;
                           case 14:
                              ch = char2_utf8(ch & 0xFF, bytes[offset], bytes[offset + 1], offset);
                              offset += 2;
                              this.nameAscii = false;
                              break;
                           default:
                              if (ch >> 3 != -2) {
                                 throw new JSONException("malformed input around byte " + offset);
                              }

                              int c2 = bytes[offset];
                              int c3 = bytes[offset + 1];
                              int c4 = bytes[offset + 2];
                              ch = ch << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
                              offset += 3;
                              this.nameAscii = false;
                        }
                     }

                     if (ch > 65535) {
                        int c0 = (ch >>> 10) + 55232;
                        hashCode ^= (long)c0;
                        hashCode *= 1099511628211L;
                        int c1 = (ch & 1023) + 56320;
                        hashCode ^= (long)c1;
                        hashCode *= 1099511628211L;
                     } else {
                        hashCode ^= (long)ch;
                        hashCode *= 1099511628211L;
                     }

                     ch = offset == end ? 26 : bytes[offset++];
               }
            }

            i++;
         }
      }

      if (ch == 58) {
         ch = offset == end ? 26 : (char)bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : bytes[offset++];
         }
      }

      this.offset = offset;
      this.ch = (char)ch;
      return hashCode;
   }

   @Override
   public final int getRawInt() {
      return this.offset + 3 < this.bytes.length ? JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset - 1L) : 0;
   }

   @Override
   public final long getRawLong() {
      return this.offset + 8 < this.bytes.length ? JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset - 1L) : 0L;
   }

   @Override
   public final boolean nextIfName8Match2() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      offset += 9;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 2] == 34 && bytes[offset - 1] == 58) {
         int c = bytes[offset] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[++offset] & 255;
         }

         this.offset = offset + 1;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName8Match1() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      offset += 8;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 1] != 58) {
         return false;
      } else {
         int c = bytes[offset] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[++offset] & 255;
         }

         this.offset = offset + 1;
         this.ch = (char)c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName8Match0() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      offset += 7;
      if (offset == this.end) {
         this.ch = 26;
         return false;
      } else {
         int c = bytes[offset] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[++offset] & 255;
         }

         this.offset = offset + 1;
         this.ch = (char)c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName4Match2() {
      byte[] bytes = this.bytes;
      int offset = this.offset + 4;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 1] != 58) {
         return false;
      } else {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName4Match3() {
      byte[] bytes = this.bytes;
      int offset = this.offset + 5;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 2] == 34 && bytes[offset - 1] == 58) {
         int c = bytes[offset] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[++offset] & 255;
         }

         this.offset = offset + 1;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match4(byte c4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 6;
      if (offset < this.end && bytes[offset - 3] == c4 && bytes[offset - 2] == 34 && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match5(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 7;
      if (offset < this.end && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) == name1) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match6(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 8;
      if (offset < this.end && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name1 && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match7(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 9;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name1
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match8(int name1, byte c8) {
      int offset = this.offset + 10;
      if (offset >= this.end) {
         return false;
      } else {
         byte[] bytes = this.bytes;
         if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name1
            && bytes[offset - 3] == c8
            && bytes[offset - 2] == 34
            && bytes[offset - 1] == 58) {
            int c = bytes[offset++] & 255;

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[offset++] & 255;
            }

            this.offset = offset;
            this.ch = (char)c;
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public final boolean nextIfName4Match9(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 11;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) != name1) {
         return false;
      } else {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      }
   }

   @Override
   public final boolean nextIfName4Match10(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 12;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name1 && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match11(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 13;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 10L) == name1
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match12(long name1, byte name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 14;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 11L) == name1
         && bytes[offset - 3] == name2
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match13(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 15;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 12L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) == name2) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match14(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 16;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 13L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name2
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match15(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 17;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 14L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name2
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match16(long name1, int name2, byte name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 18;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 15L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name2
         && bytes[offset - 3] == name3
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match17(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 19;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 16L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) == name2) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match18(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 20;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 17L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name2
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match19(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 21;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 18L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 10L) == name2
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match20(long name1, long name2, byte name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 22;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 19L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 11L) == name2
         && bytes[offset - 3] == name3
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match21(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 23;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 20L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 12L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) == name3) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match22(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 24;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 21L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 13L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name3
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match23(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 25;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 22L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 14L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name3
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match24(long name1, long name2, int name3, byte name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 26;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 23L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 15L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name3
         && bytes[offset - 3] == name4
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match25(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 27;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 24L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 16L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) == name3) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match26(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 28;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 25L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 17L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name3
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match27(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 29;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 26L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 18L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 10L) == name3
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match28(long name1, long name2, long name3, byte c29) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 30;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 27L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 19L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 11L) == name3
         && bytes[offset - 3] == c29
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match29(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 31;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 28L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 20L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 12L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) == name4) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match30(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 32;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 29L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 21L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 13L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name4
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match31(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 33;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 30L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 22L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 14L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name4
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match32(long name1, long name2, long name3, int name4, byte c32) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 34;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 31L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 23L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 15L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name4
         && bytes[offset - 3] == c32
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match33(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 35;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 32L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 24L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 16L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) == name4) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match34(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 36;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 33L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 25L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 17L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name4
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match35(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 37;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 34L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 26L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 18L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 10L) == name4
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match36(long name1, long name2, long name3, long name4, byte c36) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 38;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 35L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 27L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 19L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 11L) == name4
         && bytes[offset - 3] == c36
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match37(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 39;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 36L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 28L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 20L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 12L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) == name5) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match38(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 40;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 37L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 29L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 21L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 13L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name5
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match39(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 41;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 38L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 30L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 22L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 14L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name5
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match40(long name1, long name2, long name3, long name4, int name5, byte c40) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 42;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 39L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 31L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 23L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 15L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name5
         && bytes[offset - 3] == c40
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match41(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 43;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 40L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 32L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 24L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 16L) == name4
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) == name5) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match42(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 44;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 41L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 33L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 25L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 17L) == name4
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name5
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfName4Match43(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 45;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 42L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 34L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 26L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 18L) == name4
         && JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 10L) == name5
         && bytes[offset - 2] == 34
         && bytes[offset - 1] == 58) {
         int c = bytes[offset++] & 255;

         while (c <= 32 && (1L << c & 4294981376L) != 0L) {
            c = bytes[offset++] & 255;
         }

         this.offset = offset;
         this.ch = (char)c;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match2() {
      byte[] bytes = this.bytes;
      int offset = this.offset + 3;
      if (offset >= this.end) {
         return false;
      } else {
         int c = bytes[offset++] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               c = offset == this.end ? 26 : bytes[offset++] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[offset++] & 255;
            }

            this.offset = offset;
            this.ch = (char)c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match3() {
      byte[] bytes = this.bytes;
      int offset = this.offset + 4;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 1] != 34) {
         return false;
      } else {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match4(byte c4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 5;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 2] == c4 && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match5(byte c4, byte c5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 6;
      if (offset >= this.end) {
         return false;
      } else if (bytes[offset - 3] == c4 && bytes[offset - 2] == c5 && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match6(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 7;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 4L) != name1) {
         return false;
      } else {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match7(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 8;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 5L) == name1 && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match8(int name1, byte c8) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 9;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 6L) == name1
         && bytes[offset - 2] == c8
         && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match9(int name1, byte c8, byte c9) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 10;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 7L) == name1
         && bytes[offset - 3] == c8
         && bytes[offset - 2] == c9
         && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final boolean nextIfValue4Match10(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 11;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 8L) != name1) {
         return false;
      } else {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      }
   }

   @Override
   public final boolean nextIfValue4Match11(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 12;
      if (offset >= this.end) {
         return false;
      } else if (JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset - 9L) == name1 && bytes[offset - 1] == 34) {
         int c = bytes[offset] & 255;
         if (c != 44 && c != 125 && c != 93) {
            return false;
         } else {
            if (c == 44) {
               this.comma = true;
               offset++;
               c = offset == this.end ? 26 : bytes[offset] & 255;
            }

            while (c <= 32 && (1L << c & 4294981376L) != 0L) {
               c = bytes[++offset] & 255;
            }

            this.offset = offset + 1;
            this.ch = (char)c;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public long readFieldNameHashCode() {
      byte[] bytes = this.bytes;
      int ch = this.ch;
      if (ch == 39 && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else if (ch != 34 && ch != 39) {
         if ((this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) != 0L && isFirstIdentifier(ch)) {
            return this.readFieldNameHashCodeUnquote();
         } else if (ch != 125 && !this.isNull()) {
            String errorMsg;
            String preFieldName;
            if (ch == 91 && this.nameBegin > 0 && (preFieldName = this.getFieldName()) != null) {
               errorMsg = "illegal fieldName input " + ch + ", previous fieldName " + preFieldName;
            } else {
               errorMsg = "illegal fieldName input" + ch;
            }

            throw new JSONException(this.info(errorMsg));
         } else {
            return -1L;
         }
      } else {
         int quote = ch;
         this.nameAscii = true;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         long nameValue = 0L;
         if (offset + 9 < end) {
            byte c0;
            if ((c0 = bytes[offset]) == ch) {
               nameValue = 0L;
            } else {
               byte c1;
               if ((c1 = bytes[offset + 1]) == ch && c0 != 92 && c0 > 0) {
                  nameValue = (long)c0;
                  this.nameLength = 1;
                  this.nameEnd = offset + 1;
                  offset += 2;
               } else {
                  byte c2;
                  if ((c2 = bytes[offset + 2]) == ch && c0 != 92 && c1 != 92 && c0 >= 0 && c1 > 0) {
                     nameValue = (long)((c1 << 8) + c0);
                     this.nameLength = 2;
                     this.nameEnd = offset + 2;
                     offset += 3;
                  } else {
                     byte c3;
                     if ((c3 = bytes[offset + 3]) == ch && c0 != 92 && c1 != 92 && c2 != 92 && c0 >= 0 && c1 >= 0 && c2 > 0) {
                        nameValue = (long)((c2 << 16) + (c1 << 8) + c0);
                        this.nameLength = 3;
                        this.nameEnd = offset + 3;
                        offset += 4;
                     } else {
                        byte c4;
                        if ((c4 = bytes[offset + 4]) == ch && c0 != 92 && c1 != 92 && c2 != 92 && c3 != 92 && c0 >= 0 && c1 >= 0 && c2 >= 0 && c3 > 0) {
                           nameValue = (long)((c3 << 24) + (c2 << 16) + (c1 << 8) + c0);
                           this.nameLength = 4;
                           this.nameEnd = offset + 4;
                           offset += 5;
                        } else {
                           byte c5;
                           if ((c5 = bytes[offset + 5]) == ch
                              && c0 != 92
                              && c1 != 92
                              && c2 != 92
                              && c3 != 92
                              && c4 != 92
                              && c0 >= 0
                              && c1 >= 0
                              && c2 >= 0
                              && c3 >= 0
                              && c4 > 0) {
                              nameValue = ((long)c4 << 32) + (long)(c3 << 24) + (long)(c2 << 16) + (long)(c1 << 8) + (long)c0;
                              this.nameLength = 5;
                              this.nameEnd = offset + 5;
                              offset += 6;
                           } else {
                              byte c6;
                              if ((c6 = bytes[offset + 6]) == ch
                                 && c0 != 92
                                 && c1 != 92
                                 && c2 != 92
                                 && c3 != 92
                                 && c4 != 92
                                 && c5 != 92
                                 && c0 >= 0
                                 && c1 >= 0
                                 && c2 >= 0
                                 && c3 >= 0
                                 && c4 >= 0
                                 && c5 > 0) {
                                 nameValue = ((long)c5 << 40) + ((long)c4 << 32) + (long)(c3 << 24) + (long)(c2 << 16) + (long)(c1 << 8) + (long)c0;
                                 this.nameLength = 6;
                                 this.nameEnd = offset + 6;
                                 offset += 7;
                              } else {
                                 byte c7;
                                 if ((c7 = bytes[offset + 7]) == ch
                                    && c0 != 92
                                    && c1 != 92
                                    && c2 != 92
                                    && c3 != 92
                                    && c4 != 92
                                    && c5 != 92
                                    && c6 != 92
                                    && c0 >= 0
                                    && c1 >= 0
                                    && c2 >= 0
                                    && c3 >= 0
                                    && c4 >= 0
                                    && c5 >= 0
                                    && c6 > 0) {
                                    nameValue = ((long)c6 << 48)
                                       + ((long)c5 << 40)
                                       + ((long)c4 << 32)
                                       + (long)(c3 << 24)
                                       + (long)(c2 << 16)
                                       + (long)(c1 << 8)
                                       + (long)c0;
                                    this.nameLength = 7;
                                    this.nameEnd = offset + 7;
                                    offset += 8;
                                 } else if (bytes[offset + 8] == ch
                                    && c0 != 92
                                    && c1 != 92
                                    && c2 != 92
                                    && c3 != 92
                                    && c4 != 92
                                    && c5 != 92
                                    && c6 != 92
                                    && c7 != 92
                                    && c0 >= 0
                                    && c1 >= 0
                                    && c2 >= 0
                                    && c3 >= 0
                                    && c4 >= 0
                                    && c5 >= 0
                                    && c6 >= 0
                                    && c7 > 0) {
                                    nameValue = ((long)c7 << 56)
                                       + ((long)c6 << 48)
                                       + ((long)c5 << 40)
                                       + ((long)c4 << 32)
                                       + (long)(c3 << 24)
                                       + (long)(c2 << 16)
                                       + (long)(c1 << 8)
                                       + (long)c0;
                                    this.nameLength = 8;
                                    this.nameEnd = offset + 8;
                                    offset += 9;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (nameValue == 0L) {
            for (int i = 0; offset < end; i++) {
               ch = bytes[offset];
               if (ch == quote) {
                  if (i == 0) {
                     offset = this.nameBegin;
                  } else {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                  }
                  break;
               }

               if (ch == 92) {
                  this.nameEscape = true;
                  int var17 = bytes[++offset];
                  switch (var17) {
                     case 34:
                     case 92:
                     default:
                        ch = this.char1(var17);
                        break;
                     case 117:
                        ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        ch = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                  }

                  if (ch > 255) {
                     this.nameAscii = false;
                  }
               } else if (ch == -61 || ch == -62) {
                  byte c1 = bytes[++offset];
                  ch = (char)((ch & 31) << 6 | c1 & 63);
                  this.nameAscii = false;
               }

               if (ch > 255 || ch < 0 || i >= 8 || i == 0 && ch == 0) {
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
         }

         long hashCode;
         if (nameValue != 0L) {
            hashCode = nameValue;
         } else {
            hashCode = -3750763034362895579L;
            int i = 0;

            while (true) {
               ch = bytes[offset];
               if (ch == 92) {
                  this.nameEscape = true;
                  int var19 = bytes[++offset];
                  char var20;
                  switch (var19) {
                     case 34:
                     case 92:
                     default:
                        var20 = this.char1(var19);
                        break;
                     case 117:
                        var20 = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        var20 = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)var20;
                  hashCode *= 1099511628211L;
               } else {
                  if (ch == quote) {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                     break;
                  }

                  if (ch >= 0) {
                     offset++;
                  } else {
                     ch &= 255;
                     switch (ch >> 4) {
                        case 12:
                        case 13:
                           ch = char2_utf8(ch, bytes[offset + 1], offset);
                           offset += 2;
                           this.nameAscii = false;
                           break;
                        case 14:
                           ch = char2_utf8(ch, bytes[offset + 1], bytes[offset + 2], offset);
                           offset += 3;
                           this.nameAscii = false;
                           break;
                        default:
                           throw new JSONException("malformed input around byte " + offset);
                     }
                  }

                  hashCode ^= (long)ch;
                  hashCode *= 1099511628211L;
               }

               i++;
            }
         }

         int var22 = offset == end ? 26 : bytes[offset++];

         while (var22 <= 32 && (1L << var22 & 4294981376L) != 0L) {
            var22 = offset == end ? 26 : bytes[offset++];
         }

         if (var22 != 58) {
            throw new JSONException(this.info("expect ':', but " + var22));
         } else {
            var22 = offset == end ? 26 : bytes[offset++];

            while (var22 <= 32 && (1L << var22 & 4294981376L) != 0L) {
               var22 = offset == end ? 26 : bytes[offset++];
            }

            this.offset = offset;
            this.ch = (char)var22;
            return hashCode;
         }
      }
   }

   @Override
   public long readValueHashCode() {
      int ch = this.ch;
      if (ch != 34 && ch != 39) {
         return -1L;
      } else {
         byte[] bytes = this.bytes;
         int quote = ch;
         this.nameAscii = true;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         long nameValue = 0L;

         for (int i = 0; offset < end; i++) {
            ch = bytes[offset];
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

            if (ch == 92) {
               this.nameEscape = true;
               int var18 = bytes[++offset];
               switch (var18) {
                  case 34:
                  case 92:
                  default:
                     ch = this.char1(var18);
                     break;
                  case 117:
                     ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                     offset += 4;
                     break;
                  case 120:
                     ch = char2(bytes[offset + 1], bytes[offset + 2]);
                     offset += 2;
               }
            } else if (ch == -61 || ch == -62) {
               ch = (char)((ch & 31) << 6 | bytes[++offset] & 63);
            }

            if (ch > 255 || ch < 0 || i >= 8 || i == 0 && ch == 0) {
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
               ch = bytes[offset];
               if (ch == 92) {
                  this.nameEscape = true;
                  int var20 = bytes[++offset];
                  char var21;
                  switch (var20) {
                     case 34:
                     case 92:
                     default:
                        var21 = this.char1(var20);
                        break;
                     case 117:
                        var21 = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        var21 = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)var21;
                  hashCode *= 1099511628211L;
               } else {
                  label196: {
                     if (ch == 34) {
                        this.nameLength = i;
                        this.nameEnd = offset++;
                        break;
                     }

                     if (ch >= 0) {
                        offset++;
                     } else {
                        switch ((ch & 0xFF) >> 4) {
                           case 12:
                           case 13:
                              ch = char2_utf8(ch, bytes[offset + 1], offset);
                              offset += 2;
                              this.nameAscii = false;
                              break;
                           case 14:
                              ch = char2_utf8(ch, bytes[offset + 1], bytes[offset + 2], offset);
                              offset += 3;
                              this.nameAscii = false;
                              break;
                           default:
                              if (ch >> 3 != -2) {
                                 throw new JSONException("malformed input around byte " + offset);
                              }

                              int var10001 = ++offset;
                              offset++;
                              int c2 = bytes[var10001];
                              int c3 = bytes[offset++];
                              int c4 = bytes[offset++];
                              int uc = ch << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
                              if ((c2 & 192) != 128 || (c3 & 192) != 128 || (c4 & 192) != 128 || uc < 65536 || uc >= 1114112) {
                                 throw new JSONException("malformed input around byte " + offset);
                              }

                              char x1 = (char)((uc >>> 10) + 55232);
                              char x2 = (char)((uc & 1023) + 56320);
                              hashCode ^= (long)x1;
                              hashCode *= 1099511628211L;
                              hashCode ^= (long)x2;
                              hashCode *= 1099511628211L;
                              i++;
                              break label196;
                        }
                     }

                     hashCode ^= (long)ch;
                     hashCode *= 1099511628211L;
                  }
               }

               i++;
            }
         }

         int var22 = offset == end ? 26 : bytes[offset++];

         while (var22 <= 32 && (1L << var22 & 4294981376L) != 0L) {
            var22 = offset == end ? 26 : bytes[offset++];
         }

         if (this.comma = var22 == 44) {
            var22 = offset == end ? 26 : bytes[offset++];

            while (var22 <= 32 && (1L << var22 & 4294981376L) != 0L) {
               var22 = offset == end ? 26 : bytes[offset++];
            }
         }

         this.offset = offset;
         this.ch = (char)var22;
         return hashCode;
      }
   }

   @Override
   public long getNameHashCodeLCase() {
      long hashCode = -3750763034362895579L;
      int offset = this.nameBegin;
      byte[] bytes = this.bytes;
      long nameValue = 0L;

      for (int i = 0; offset < this.end; offset++) {
         int ch = bytes[offset];
         if (ch == 92) {
            int var17 = bytes[++offset];
            switch (var17) {
               case 34:
               case 92:
               default:
                  ch = this.char1(var17);
                  break;
               case 117:
                  ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                  offset += 4;
                  break;
               case 120:
                  ch = char2(bytes[offset + 1], bytes[offset + 2]);
                  offset += 2;
            }
         } else if (ch != -61 && ch != -62) {
            if (ch == 34) {
               break;
            }
         } else {
            ch = (ch & 31) << 6 | bytes[++offset] & 63;
         }

         if (i >= 8 || ch > 255 || ch < 0 || i == 0 && ch == 0) {
            nameValue = 0L;
            offset = this.nameBegin;
            break;
         }

         if (ch == 95 || ch == 45 || ch == 32) {
            int c1 = bytes[offset + 1];
            if (c1 != 34 && c1 != 39 && c1 != ch) {
               continue;
            }
         }

         if (ch >= 65 && ch <= 90) {
            ch = (char)(ch + 32);
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

         i++;
      }

      if (nameValue != 0L) {
         return nameValue;
      } else if (this.nameAscii && !this.nameEscape) {
         for (int i = this.nameBegin; i < this.nameEnd; i++) {
            int chx = bytes[i];
            if (chx >= 65 && chx <= 90) {
               chx += 32;
            }

            if (chx == 95 || chx == 45 || chx == 32) {
               byte c1 = bytes[i + 1];
               if (c1 != 34 && c1 != 39 && c1 != chx) {
                  continue;
               }
            }

            hashCode ^= (long)chx;
            hashCode *= 1099511628211L;
         }

         return hashCode;
      } else {
         while (true) {
            int chxx = bytes[offset];
            if (chxx == 92) {
               int var14 = bytes[++offset];
               switch (var14) {
                  case 34:
                  case 92:
                  default:
                     chxx = this.char1(var14);
                     break;
                  case 117:
                     chxx = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                     offset += 4;
                     break;
                  case 120:
                     chxx = char2(bytes[offset + 1], bytes[offset + 2]);
                     offset += 2;
               }

               offset++;
            } else {
               if (chxx == 34) {
                  return hashCode;
               }

               if (chxx >= 0) {
                  if (chxx >= 65 && chxx <= 90) {
                     chxx += 32;
                  }

                  offset++;
               } else {
                  chxx &= 255;
                  switch (chxx >> 4) {
                     case 12:
                     case 13: {
                        int c2 = bytes[offset + 1];
                        chxx = (chxx & 31) << 6 | c2 & 63;
                        offset += 2;
                        break;
                     }
                     case 14: {
                        int c2 = bytes[offset + 1];
                        int c3 = bytes[offset + 2];
                        chxx = (chxx & 15) << 12 | (c2 & 63) << 6 | c3 & 63;
                        offset += 3;
                        break;
                     }
                     default:
                        throw new JSONException("malformed input around byte " + offset);
                  }
               }
            }

            if (chxx != 95 && chxx != 45 && chxx != 32) {
               hashCode ^= (long)chxx;
               hashCode *= 1099511628211L;
            }
         }
      }
   }

   final String getLatin1String(int offset, int length) {
      if (JDKUtils.ANDROID_SDK_INT >= 34) {
         return new String(this.bytes, offset, length, StandardCharsets.ISO_8859_1);
      } else {
         char[] charBuf = this.charBuf;
         if (charBuf == null) {
            if (this.cacheItem == null) {
               int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
               this.cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
            }

            this.charBuf = charBuf = JSONFactory.CHARS_UPDATER.getAndSet(this.cacheItem, null);
         }

         if (charBuf == null || charBuf.length < length) {
            this.charBuf = charBuf = new char[length];
         }

         for (int i = 0; i < length; i++) {
            charBuf[i] = (char)(this.bytes[offset + i] & 255);
         }

         return new String(charBuf, 0, length);
      }
   }

   @Override
   public String getFieldName() {
      byte[] bytes = this.bytes;
      int offset = this.nameBegin;
      int length = this.nameEnd - offset;
      if (!this.nameEscape) {
         if (this.nameAscii) {
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               char[] chars = new char[length];

               for (int i = 0; i < length; i++) {
                  chars[i] = (char)bytes[offset + i];
               }

               return JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            }

            if (JDKUtils.STRING_CREATOR_JDK11 != null) {
               return JDKUtils.STRING_CREATOR_JDK11.apply(Arrays.copyOfRange(bytes, offset, this.nameEnd), JDKUtils.LATIN1);
            }

            if (JDKUtils.ANDROID) {
               return this.getLatin1String(offset, length);
            }
         }

         return new String(bytes, offset, length, this.nameAscii ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
      } else {
         char[] chars = new char[this.nameLength];

         for (int i = 0; offset < this.nameEnd; i++) {
            int ch = bytes[offset];
            if (ch < 0) {
               ch &= 255;
               switch (ch >> 4) {
                  case 12:
                  case 13:
                     ch = char2_utf8(ch, bytes[offset + 1], offset);
                     offset += 2;
                     break;
                  case 14:
                     ch = char2_utf8(ch, bytes[offset + 1], bytes[offset + 2], offset);
                     offset += 3;
                     break;
                  default:
                     throw new JSONException("malformed input around byte " + offset);
               }

               chars[i] = (char)ch;
            } else {
               if (ch == 92) {
                  ch = (char)bytes[++offset];
                  switch (ch) {
                     case 34:
                     case 42:
                     case 43:
                     case 45:
                     case 46:
                     case 47:
                     case 58:
                     case 60:
                     case 61:
                     case 62:
                     case 64:
                     case 92:
                        break;
                     case 117:
                        ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        ch = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                        break;
                     default:
                        ch = this.char1(ch);
                  }
               } else if (ch == 34) {
                  break;
               }

               chars[i] = (char)ch;
               offset++;
            }
         }

         return new String(chars);
      }
   }

   @Override
   public String readFieldName() {
      char quote = this.ch;
      if (quote == '\'' && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else if (quote != '"' && quote != '\'') {
         return (this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) != 0L && isFirstIdentifier(quote)
            ? this.readFieldNameUnquote()
            : null;
      } else {
         byte[] bytes = this.bytes;
         this.nameAscii = true;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int end = this.end;
         int nameBegin = this.nameBegin;

         for (int i = 0; offset < end; i++) {
            int ch = bytes[offset];
            if (ch == 92) {
               this.nameEscape = true;
               int var20 = bytes[offset + 1];
               offset += var20 == 117 ? 6 : (var20 == 120 ? 4 : 2);
            } else {
               if (ch == quote) {
                  this.nameLength = i;
                  this.nameEnd = offset++;
                  ch = bytes[offset] & 255;

                  while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
                     ch = bytes[++offset] & 255;
                  }

                  if (ch != 58) {
                     throw syntaxError(offset, ch);
                  }

                  offset++;
                  int var22 = offset == end ? 26 : bytes[offset++];

                  while (var22 <= 32 && (1L << var22 & 4294981376L) != 0L) {
                     var22 = offset == end ? 26 : bytes[offset++];
                  }

                  this.offset = offset;
                  this.ch = (char)var22;
                  break;
               }

               if (ch >= 0) {
                  offset++;
               } else {
                  if (this.nameAscii) {
                     this.nameAscii = false;
                  }

                  switch ((ch & 0xFF) >> 4) {
                     case 12:
                     case 13:
                        offset += 2;
                        break;
                     case 14:
                        offset += 3;
                        break;
                     default:
                        if (ch >> 3 != -2) {
                           throw syntaxError(offset, ch);
                        }

                        offset += 4;
                  }
               }
            }
         }

         if (this.nameEnd < nameBegin) {
            throw syntaxError(offset, this.ch);
         } else {
            int length = this.nameEnd - nameBegin;
            if (this.nameEscape) {
               return this.getFieldName();
            } else {
               if (this.nameAscii) {
                  long nameValue0 = -1L;
                  long nameValue1 = -1L;
                  switch (length) {
                     case 1:
                        return TypeUtils.toString((char)(bytes[nameBegin] & 255));
                     case 2:
                        return TypeUtils.toString((char)(bytes[nameBegin] & 255), (char)(bytes[nameBegin + 1] & 255));
                     case 3:
                        nameValue0 = (long)((bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                        break;
                     case 4:
                        nameValue0 = (long)((bytes[nameBegin + 3] << 24) + (bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                        break;
                     case 5:
                        nameValue0 = ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        break;
                     case 6:
                        nameValue0 = ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        break;
                     case 7:
                        nameValue0 = ((long)bytes[nameBegin + 6] << 48)
                           + ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        break;
                     case 8:
                        nameValue0 = ((long)bytes[nameBegin + 7] << 56)
                           + ((long)bytes[nameBegin + 6] << 48)
                           + ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        break;
                     case 9:
                        nameValue0 = (long)bytes[nameBegin];
                        nameValue1 = ((long)bytes[nameBegin + 8] << 56)
                           + ((long)bytes[nameBegin + 7] << 48)
                           + ((long)bytes[nameBegin + 6] << 40)
                           + ((long)bytes[nameBegin + 5] << 32)
                           + ((long)bytes[nameBegin + 4] << 24)
                           + ((long)bytes[nameBegin + 3] << 16)
                           + ((long)bytes[nameBegin + 2] << 8)
                           + (long)bytes[nameBegin + 1];
                        break;
                     case 10:
                        nameValue0 = (long)((bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                        nameValue1 = ((long)bytes[nameBegin + 9] << 56)
                           + ((long)bytes[nameBegin + 8] << 48)
                           + ((long)bytes[nameBegin + 7] << 40)
                           + ((long)bytes[nameBegin + 6] << 32)
                           + ((long)bytes[nameBegin + 5] << 24)
                           + ((long)bytes[nameBegin + 4] << 16)
                           + ((long)bytes[nameBegin + 3] << 8)
                           + (long)bytes[nameBegin + 2];
                        break;
                     case 11:
                        nameValue0 = (long)((bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                        nameValue1 = ((long)bytes[nameBegin + 10] << 56)
                           + ((long)bytes[nameBegin + 9] << 48)
                           + ((long)bytes[nameBegin + 8] << 40)
                           + ((long)bytes[nameBegin + 7] << 32)
                           + ((long)bytes[nameBegin + 6] << 24)
                           + ((long)bytes[nameBegin + 5] << 16)
                           + ((long)bytes[nameBegin + 4] << 8)
                           + (long)bytes[nameBegin + 3];
                        break;
                     case 12:
                        nameValue0 = (long)((bytes[nameBegin + 3] << 24) + (bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                        nameValue1 = ((long)bytes[nameBegin + 11] << 56)
                           + ((long)bytes[nameBegin + 10] << 48)
                           + ((long)bytes[nameBegin + 9] << 40)
                           + ((long)bytes[nameBegin + 8] << 32)
                           + ((long)bytes[nameBegin + 7] << 24)
                           + ((long)bytes[nameBegin + 6] << 16)
                           + ((long)bytes[nameBegin + 5] << 8)
                           + (long)bytes[nameBegin + 4];
                        break;
                     case 13:
                        nameValue0 = ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        nameValue1 = ((long)bytes[nameBegin + 12] << 56)
                           + ((long)bytes[nameBegin + 11] << 48)
                           + ((long)bytes[nameBegin + 10] << 40)
                           + ((long)bytes[nameBegin + 9] << 32)
                           + ((long)bytes[nameBegin + 8] << 24)
                           + ((long)bytes[nameBegin + 7] << 16)
                           + ((long)bytes[nameBegin + 6] << 8)
                           + (long)bytes[nameBegin + 5];
                        break;
                     case 14:
                        nameValue0 = ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        nameValue1 = ((long)bytes[nameBegin + 13] << 56)
                           + ((long)bytes[nameBegin + 12] << 48)
                           + ((long)bytes[nameBegin + 11] << 40)
                           + ((long)bytes[nameBegin + 10] << 32)
                           + ((long)bytes[nameBegin + 9] << 24)
                           + ((long)bytes[nameBegin + 8] << 16)
                           + ((long)bytes[nameBegin + 7] << 8)
                           + (long)bytes[nameBegin + 6];
                        break;
                     case 15:
                        nameValue0 = ((long)bytes[nameBegin + 6] << 48)
                           + ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        nameValue1 = ((long)bytes[nameBegin + 14] << 56)
                           + ((long)bytes[nameBegin + 13] << 48)
                           + ((long)bytes[nameBegin + 12] << 40)
                           + ((long)bytes[nameBegin + 11] << 32)
                           + ((long)bytes[nameBegin + 10] << 24)
                           + ((long)bytes[nameBegin + 9] << 16)
                           + ((long)bytes[nameBegin + 8] << 8)
                           + (long)bytes[nameBegin + 7];
                        break;
                     case 16:
                        nameValue0 = ((long)bytes[nameBegin + 7] << 56)
                           + ((long)bytes[nameBegin + 6] << 48)
                           + ((long)bytes[nameBegin + 5] << 40)
                           + ((long)bytes[nameBegin + 4] << 32)
                           + ((long)bytes[nameBegin + 3] << 24)
                           + ((long)bytes[nameBegin + 2] << 16)
                           + ((long)bytes[nameBegin + 1] << 8)
                           + (long)bytes[nameBegin];
                        nameValue1 = ((long)bytes[nameBegin + 15] << 56)
                           + ((long)bytes[nameBegin + 14] << 48)
                           + ((long)bytes[nameBegin + 13] << 40)
                           + ((long)bytes[nameBegin + 12] << 32)
                           + ((long)bytes[nameBegin + 11] << 24)
                           + ((long)bytes[nameBegin + 10] << 16)
                           + ((long)bytes[nameBegin + 9] << 8)
                           + (long)bytes[nameBegin + 8];
                  }

                  if (nameValue0 != -1L) {
                     if (nameValue1 != -1L) {
                        long nameValue01 = nameValue0 ^ nameValue1;
                        int indexMask = (int)(nameValue01 ^ nameValue01 >>> 32) & JSONFactory.NAME_CACHE2.length - 1;
                        JSONFactory.NameCacheEntry2 entry = JSONFactory.NAME_CACHE2[indexMask];
                        if (entry == null) {
                           String name;
                           if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                              char[] chars = new char[length];

                              for (int ix = 0; ix < length; ix++) {
                                 chars[ix] = (char)bytes[nameBegin + ix];
                              }

                              name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                           } else if (JDKUtils.ANDROID) {
                              name = this.getLatin1String(nameBegin, length);
                           } else {
                              name = new String(bytes, nameBegin, length, StandardCharsets.ISO_8859_1);
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
                           if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                              char[] chars = new char[length];

                              for (int ix = 0; ix < length; ix++) {
                                 chars[ix] = (char)bytes[nameBegin + ix];
                              }

                              name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                           } else if (JDKUtils.ANDROID) {
                              name = this.getLatin1String(nameBegin, length);
                           } else {
                              name = new String(bytes, nameBegin, length, StandardCharsets.ISO_8859_1);
                           }

                           JSONFactory.NAME_CACHE[indexMaskx] = new JSONFactory.NameCacheEntry(name, nameValue0);
                           return name;
                        }

                        if (entryx.value == nameValue0) {
                           return entryx.name;
                        }
                     }
                  }

                  if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                     char[] chars = new char[length];

                     for (int ix = 0; ix < length; ix++) {
                        chars[ix] = (char)bytes[nameBegin + ix];
                     }

                     return JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                  }

                  if (JDKUtils.ANDROID) {
                     return this.getLatin1String(nameBegin, this.nameEnd - nameBegin);
                  }

                  if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                     return JDKUtils.STRING_CREATOR_JDK11.apply(Arrays.copyOfRange(bytes, nameBegin, this.nameEnd), JDKUtils.LATIN1);
                  }
               }

               return this.nameAscii && JDKUtils.ANDROID
                  ? this.getLatin1String(nameBegin, length)
                  : new String(bytes, nameBegin, length, this.nameAscii ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
            }
         }
      }
   }

   @Override
   public final int readInt32Value() {
      boolean negative = false;
      int ch = this.ch;
      int offset = this.offset;
      int end = this.end;
      byte[] bytes = this.bytes;
      int intValue = 0;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
      }

      if (ch == 45) {
         negative = true;
         ch = bytes[offset++];
      } else if (ch == 43) {
         ch = bytes[offset++];
      } else if (ch == 44) {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < 48 || ch > 57; ch >= 48 && ch <= 57; ch = offset == end ? 26 : bytes[offset++]) {
         int intValue10 = intValue * 10 + (ch - 48);
         if (intValue10 < intValue) {
            overflow = true;
            break;
         }

         intValue = intValue10;
      }

      if (ch == 46 || ch == 101 || ch == 69 || ch == 116 || ch == 102 || ch == 110 || ch == 123 || ch == 91 || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.getInt32Value();
      } else {
         if (quote != 0) {
            ch = offset == end ? 26 : bytes[offset++];
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset == end ? 26 : bytes[offset++];
         }

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)ch;
         this.offset = offset;
         return negative ? -intValue : intValue;
      }
   }

   @Override
   public final Integer readInt32() {
      boolean negative = false;
      int ch = this.ch;
      int offset = this.offset;
      byte[] bytes = this.bytes;
      int intValue = 0;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
      }

      if (ch == 45) {
         negative = true;
         ch = bytes[offset++];
      } else if (ch == 43) {
         ch = bytes[offset++];
      } else if (ch == 44) {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < 48 || ch > 57; ch >= 48 && ch <= 57; ch = offset == this.end ? 26 : bytes[offset++]) {
         int intValue10 = intValue * 10 + (ch - 48);
         if (intValue10 < intValue) {
            overflow = true;
            break;
         }

         intValue = intValue10;
      }

      if (ch == 46 || ch == 101 || ch == 69 || ch == 116 || ch == 102 || ch == 110 || ch == 123 || ch == 91 || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.wasNull ? null : this.getInt32Value();
      } else {
         if (quote != 0) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)ch;
         this.offset = offset;
         return negative ? -intValue : intValue;
      }
   }

   @Override
   public final long readInt64Value() {
      boolean negative = false;
      int ch = this.ch;
      int offset = this.offset;
      byte[] bytes = this.bytes;
      long longValue = 0L;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
      }

      if (ch == 45) {
         negative = true;
         ch = bytes[offset++];
      } else if (ch == 43) {
         ch = bytes[offset++];
      } else if (ch == 44) {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < 48 || ch > 57; ch >= 48 && ch <= 57; ch = offset == this.end ? 26 : bytes[offset++]) {
         long intValue10 = longValue * 10L + (long)(ch - 48);
         if (intValue10 < longValue) {
            overflow = true;
            break;
         }

         longValue = intValue10;
      }

      if (ch == 46 || ch == 101 || ch == 69 || ch == 116 || ch == 102 || ch == 110 || ch == 123 || ch == 91 || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.getInt64Value();
      } else {
         if (quote != 0) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == this.end ? 26 : (char)bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)ch;
         this.offset = offset;
         return negative ? -longValue : longValue;
      }
   }

   @Override
   public final Long readInt64() {
      boolean negative = false;
      int ch = this.ch;
      int offset = this.offset;
      byte[] bytes = this.bytes;
      long longValue = 0L;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
      }

      if (ch == 45) {
         negative = true;
         ch = bytes[offset++];
      } else if (ch == 43) {
         ch = bytes[offset++];
      } else if (ch == 44) {
         throw this.numberError();
      }

      boolean overflow;
      for (overflow = ch < 48 || ch > 57; ch >= 48 && ch <= 57; ch = offset == this.end ? 26 : bytes[offset++]) {
         long intValue10 = longValue * 10L + (long)(ch - 48);
         if (intValue10 < longValue) {
            overflow = true;
            break;
         }

         longValue = intValue10;
      }

      if (ch == 46 || ch == 101 || ch == 69 || ch == 116 || ch == 102 || ch == 110 || ch == 123 || ch == 91 || quote != 0 && ch != quote) {
         overflow = true;
      }

      if (overflow) {
         this.readNumber0();
         return this.wasNull ? null : this.getInt64Value();
      } else {
         this.wasNull = false;
         if (quote != 0) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == this.end ? 26 : (char)bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)ch;
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
      byte[] bytes = this.bytes;
      int quote = 0;
      int ch = this.ch;
      int offset = this.offset;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
         if (ch == quote) {
            int var18 = offset == this.end ? 26 : bytes[offset++];

            while (var18 <= 32 && (1L << var18 & 4294981376L) != 0L) {
               var18 = offset == this.end ? 26 : bytes[offset++];
            }

            this.ch = (char)var18;
            this.offset = offset;
            this.nextIfComma();
            this.wasNull = true;
            return 0.0;
         }
      }

      int start = offset;
      if (ch == 45) {
         this.negative = true;
         ch = bytes[offset++];
      } else {
         this.negative = false;
         if (ch == 43) {
            ch = bytes[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
         valid = true;
         if (!overflow) {
            long intValue10 = longValue * 10L + (long)(ch - 48);
            if (intValue10 < longValue) {
               overflow = true;
            } else {
               longValue = intValue10;
            }
         }

         if (offset == this.end) {
            ch = 26;
            offset++;
            break;
         }
      }

      this.scale = 0;
      if (ch == 46) {
         this.valueType = 2;

         for (ch = bytes[offset++]; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long intValue10 = longValue * 10L + (long)(ch - 48);
               if (intValue10 < longValue) {
                  overflow = true;
               } else {
                  longValue = intValue10;
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
      if (ch == 101 || ch == 69) {
         boolean negativeExp = false;
         ch = bytes[offset++];
         if (ch == 45) {
            negativeExp = true;
            ch = bytes[offset++];
         } else if (ch == 43) {
            ch = bytes[offset++];
         }

         while (ch >= 48 && ch <= 57) {
            valid = true;
            int byteVal = ch - 48;
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = 26;
               offset++;
               break;
            }

            ch = bytes[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 110 && bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
            offset += 3;
            valid = true;
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : bytes[offset++];
         } else if (ch == 116 && bytes[offset] == 114 && bytes[offset + 1] == 117 && bytes[offset + 2] == 101) {
            offset += 3;
            valid = true;
            value = true;
            doubleValue = 1.0;
            ch = offset == this.end ? 26 : bytes[offset++];
         } else if (ch == 102 && bytes[offset] == 97 && bytes[offset + 1] == 108 && bytes[offset + 2] == 115 && bytes[offset + 3] == 101) {
            valid = true;
            offset += 4;
            doubleValue = 0.0;
            value = true;
            ch = offset == this.end ? 26 : bytes[offset++];
         } else if (ch == 78 && bytes[offset] == 97 && bytes[offset + 1] == 78) {
            valid = true;
            offset += 2;
            doubleValue = Double.NaN;
            value = true;
            ch = offset == this.end ? 26 : bytes[offset++];
         } else if (ch == 123 && quote == 0) {
            valid = true;
            this.ch = (char)ch;
            this.offset = offset;
            Map<String, Object> obj = this.readObject();
            if (!obj.isEmpty()) {
               throw new JSONException(this.info());
            }

            offset = this.offset;
            ch = this.ch;
            value = true;
            this.wasNull = true;
         } else if (ch == 91 && quote == 0) {
            valid = true;
            this.ch = (char)ch;
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
            this.ch = (char)quote;
            str = this.readString();
            offset = this.offset;
         }

         ch = offset == this.end ? 26 : bytes[offset++];
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
               doubleValue = TypeUtils.parseDouble(bytes, start - 1, len);
            }
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }
      }

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset == this.end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = (char)ch;
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
      byte[] chars = this.bytes;
      int quote = 0;
      int ch = this.ch;
      int offset = this.offset;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = chars[offset++];
         if (ch == quote) {
            int var17 = offset == this.end ? 26 : chars[offset++];

            while (var17 <= 32 && (1L << var17 & 4294981376L) != 0L) {
               var17 = offset == this.end ? 26 : chars[offset++];
            }

            this.ch = (char)var17;
            this.offset = offset;
            this.nextIfComma();
            this.wasNull = true;
            return 0.0F;
         }
      }

      int start = offset;
      if (ch == 45) {
         this.negative = true;
         ch = chars[offset++];
      } else {
         this.negative = false;
         if (ch == 43) {
            ch = chars[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= 48 && ch <= 57; ch = chars[offset++]) {
         valid = true;
         if (!overflow) {
            long intValue10 = longValue * 10L + (long)(ch - 48);
            if (intValue10 < longValue) {
               overflow = true;
            } else {
               longValue = intValue10;
            }
         }

         if (offset == this.end) {
            ch = 26;
            offset++;
            break;
         }
      }

      this.scale = 0;
      if (ch == 46) {
         this.valueType = 2;

         for (ch = chars[offset++]; ch >= 48 && ch <= 57; ch = chars[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long intValue10 = longValue * 10L + (long)(ch - 48);
               if (intValue10 < longValue) {
                  overflow = true;
               } else {
                  longValue = intValue10;
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
      if (ch == 101 || ch == 69) {
         boolean negativeExp = false;
         ch = chars[offset++];
         if (ch == 45) {
            negativeExp = true;
            ch = chars[offset++];
         } else if (ch == 43) {
            ch = chars[offset++];
         }

         while (ch >= 48 && ch <= 57) {
            valid = true;
            int byteVal = ch - 48;
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
         if (ch == 110 && chars[offset] == 117 && chars[offset + 1] == 108 && chars[offset + 2] == 108) {
            offset += 3;
            valid = true;
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 116 && chars[offset] == 114 && chars[offset + 1] == 117 && chars[offset + 2] == 101) {
            offset += 3;
            valid = true;
            value = true;
            floatValue = 1.0F;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 102 && chars[offset] == 97 && chars[offset + 1] == 108 && chars[offset + 2] == 115 && chars[offset + 3] == 101) {
            offset += 4;
            valid = true;
            floatValue = 0.0F;
            value = true;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 78 && chars[offset] == 97 && chars[offset + 1] == 78) {
            offset += 2;
            valid = true;
            value = true;
            floatValue = Float.NaN;
            ch = offset == this.end ? 26 : chars[offset++];
         } else if (ch == 123 && quote == 0) {
            valid = true;
            this.ch = (char)ch;
            this.offset = offset;
            Map<String, Object> obj = this.readObject();
            if (!obj.isEmpty()) {
               throw new JSONException(this.info());
            }

            ch = this.ch;
            offset = this.offset;
            value = true;
            this.wasNull = true;
         } else if (ch == 91 && quote == 0) {
            this.ch = (char)ch;
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
            this.ch = (char)quote;
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

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset >= this.end ? 26 : chars[offset++];
         }
      }

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset >= this.end ? 26 : chars[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset >= this.end ? 26 : chars[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset >= this.end ? 26 : chars[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = (char)ch;
         this.offset = offset;
         return floatValue;
      }
   }

   @Override
   public final void readString(ValueConsumer consumer, boolean quoted) {
      char quote = this.ch;
      int offset = this.offset;
      int start = offset;
      this.valueEscape = false;
      byte[] bytes = this.bytes;
      int i = 0;

      while (true) {
         int c = bytes[offset];
         if (c == 92) {
            this.valueEscape = true;
            int var21 = bytes[++offset];
            switch (var21) {
               case 117:
                  offset += 4;
                  break;
               case 120:
                  offset += 2;
            }

            offset++;
         } else if (c >= 0) {
            if (c == quote) {
               if (this.valueEscape) {
                  int bytesMaxiumLength = offset - this.offset;
                  char[] chars = new char[i];
                  offset = start;
                  int ix = 0;

                  while (true) {
                     int cx = bytes[offset];
                     if (cx == 92) {
                        cx = bytes[++offset];
                        switch (cx) {
                           case 34:
                           case 92:
                              break;
                           case 117:
                              cx = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                              offset += 4;
                              break;
                           case 120:
                              cx = char2(bytes[offset + 1], bytes[offset + 2]);
                              offset += 2;
                              break;
                           default:
                              cx = this.char1(cx);
                        }
                     } else if (cx == 34) {
                        if (quoted) {
                           JSONWriter jsonWriter = JSONWriterUTF8.of();
                           jsonWriter.writeString(chars, 0, chars.length);
                           byte[] buf = jsonWriter.getBytes();
                           consumer.accept(buf, 0, buf.length);
                        } else {
                           byte[] buf = new byte[bytesMaxiumLength];
                           cx = IOUtils.encodeUTF8(chars, 0, chars.length, buf, 0);
                           consumer.accept(buf, 0, cx);
                        }
                        break;
                     }

                     if (cx >= 0) {
                        chars[ix] = (char)cx;
                        offset++;
                     } else {
                        switch ((cx & 0xFF) >> 4) {
                           case 12:
                           case 13:
                              chars[ix] = (char)((cx & 31) << 6 | bytes[offset + 1] & 63);
                              offset += 2;
                              break;
                           case 14:
                              chars[ix] = (char)((cx & 15) << 12 | (bytes[offset + 1] & 63) << 6 | bytes[offset + 2] & 63);
                              offset += 3;
                              break;
                           default:
                              if (cx >> 3 != -2) {
                                 throw new JSONException("malformed input around byte " + offset);
                              }

                              int c2 = bytes[offset + 1];
                              int c3 = bytes[offset + 2];
                              int c4 = bytes[offset + 3];
                              offset += 4;
                              int uc = cx << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
                              if ((c2 & 192) != 128 || (c3 & 192) != 128 || (c4 & 192) != 128 || uc < 65536 || uc >= 1114112) {
                                 throw new JSONException("malformed input around byte " + offset);
                              }

                              chars[ix++] = (char)((uc >>> 10) + 55232);
                              chars[ix] = (char)((uc & 1023) + 56320);
                        }
                     }

                     ix++;
                  }
               } else {
                  i = quoted ? this.offset - 1 : this.offset;
                  c = quoted ? offset - this.offset + 2 : offset - this.offset;
                  if (quoted && quote == '\'') {
                     byte[] quotedBytes = new byte[c];
                     System.arraycopy(bytes, this.offset - 1, quotedBytes, 0, c);
                     quotedBytes[0] = 34;
                     quotedBytes[quotedBytes.length - 1] = 34;
                     consumer.accept(quotedBytes, 0, quotedBytes.length);
                  } else {
                     consumer.accept(bytes, i, c);
                  }
               }

               i = bytes[++offset];

               while (i <= 32 && (1L << i & 4294981376L) != 0L) {
                  i = bytes[++offset];
               }

               if (this.comma = i == 44) {
                  this.offset = offset + 1;
                  this.next();
               } else {
                  this.offset = offset + 1;
                  this.ch = (char)i;
               }

               return;
            }

            offset++;
         } else {
            switch ((c & 0xFF) >> 4) {
               case 12:
               case 13:
                  offset += 2;
                  break;
               case 14:
                  offset += 3;
                  break;
               default:
                  if (c >> 3 != -2) {
                     throw new JSONException("malformed input around byte " + offset);
                  }

                  offset += 4;
                  i++;
            }
         }

         i++;
      }
   }

   protected void readString0() {
      char quote = this.ch;
      int offset = this.offset;
      int start = offset;
      boolean ascii = true;
      this.valueEscape = false;
      byte[] bytes = this.bytes;
      int i = 0;

      while (true) {
         int c = bytes[offset];
         if (c == 92) {
            this.valueEscape = true;
            int var24 = bytes[offset + 1];
            offset += var24 == 117 ? 6 : (var24 == 120 ? 4 : 2);
         } else if (c >= 0) {
            if (c == quote) {
               String str;
               if (this.valueEscape) {
                  char[] chars = new char[i];
                  offset = start;
                  int ix = 0;

                  while (true) {
                     int cx = bytes[offset];
                     if (cx == 92) {
                        cx = bytes[++offset];
                        switch (cx) {
                           case 34:
                           case 92:
                              break;
                           case 117:
                              cx = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                              offset += 4;
                              break;
                           case 120:
                              cx = char2(bytes[offset + 1], bytes[offset + 2]);
                              offset += 2;
                              break;
                           default:
                              cx = this.char1(cx);
                        }

                        chars[ix] = (char)cx;
                        offset++;
                     } else {
                        if (cx == 34) {
                           str = new String(chars);
                           break;
                        }

                        if (cx >= 0) {
                           chars[ix] = (char)cx;
                           offset++;
                        } else {
                           switch ((cx & 0xFF) >> 4) {
                              case 12:
                              case 13: {
                                 int var37 = ++offset;
                                 offset++;
                                 int c2 = bytes[var37];
                                 chars[ix] = (char)((cx & 31) << 6 | c2 & 63);
                                 break;
                              }
                              case 14: {
                                 int var35 = ++offset;
                                 offset++;
                                 int c2 = bytes[var35];
                                 int c3 = bytes[offset++];
                                 chars[ix] = (char)((cx & 15) << 12 | (c2 & 63) << 6 | c3 & 63);
                                 break;
                              }
                              default: {
                                 if (cx >> 3 != -2) {
                                    throw new JSONException("malformed input around byte " + offset);
                                 }

                                 int var10001 = ++offset;
                                 offset++;
                                 int c2 = bytes[var10001];
                                 int c3 = bytes[offset++];
                                 int c4 = bytes[offset++];
                                 int uc = cx << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
                                 if ((c2 & 192) != 128 || (c3 & 192) != 128 || (c4 & 192) != 128 || uc < 65536 || uc >= 1114112) {
                                    throw new JSONException("malformed input around byte " + offset);
                                 }

                                 chars[ix++] = (char)((uc >>> 10) + 55232);
                                 chars[ix] = (char)((uc & 1023) + 56320);
                              }
                           }
                        }
                     }

                     ix++;
                  }
               } else if (ascii) {
                  c = offset - this.offset;
                  if (JDKUtils.ANDROID) {
                     str = this.getLatin1String(this.offset, c);
                  } else {
                     str = new String(bytes, this.offset, c, StandardCharsets.ISO_8859_1);
                  }
               } else {
                  str = new String(bytes, this.offset, offset - this.offset, StandardCharsets.UTF_8);
               }

               c = bytes[++offset];

               while (c <= 32 && (1L << c & 4294981376L) != 0L) {
                  c = bytes[++offset];
               }

               this.comma = c == 44;
               this.offset = offset + 1;
               if (c == 44) {
                  this.next();
               } else {
                  this.ch = (char)c;
               }

               this.stringValue = str;
               return;
            }

            offset++;
         } else {
            switch ((c & 0xFF) >> 4) {
               case 12:
               case 13:
                  offset += 2;
                  ascii = false;
                  break;
               case 14:
                  offset += 3;
                  ascii = false;
                  break;
               default:
                  if (c >> 3 != -2) {
                     throw new JSONException("malformed input around byte " + offset);
                  }

                  offset += 4;
                  i++;
                  ascii = false;
            }
         }

         i++;
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
         byte[] bytes = this.bytes;

         while (true) {
            int ch = bytes[offset++];
            if (ch == 92) {
               int var7 = bytes[offset];
               offset += var7 == 117 ? 5 : (var7 == 120 ? 3 : 1);
            } else if (ch == quote) {
               int var5 = offset == this.end ? 26 : bytes[offset++];

               while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
                  var5 = offset == this.end ? 26 : bytes[offset++];
               }

               if (var5 != 58) {
                  throw syntaxError(var5);
               }

               var5 = offset == this.end ? 26 : bytes[offset++];

               while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
                  var5 = offset == this.end ? 26 : bytes[offset++];
               }

               this.offset = offset;
               this.ch = (char)var5;
               return true;
            }
         }
      }
   }

   @Override
   public final void skipValue() {
      byte[] bytes;
      int ch;
      int offset;
      int end;
      bytes = this.bytes;
      ch = this.ch;
      offset = this.offset;
      end = this.end;
      this.comma = false;
      label373:
      switch (ch) {
         case 34:
         case 39:
            int quote = ch;
            int var10 = bytes[offset++];

            while (true) {
               while (var10 != 92) {
                  if (var10 == quote) {
                     ch = offset == end ? 26 : bytes[offset++];
                     break label373;
                  }

                  var10 = bytes[offset++];
               }

               var10 = bytes[offset++];
               if (var10 == 117) {
                  offset += 4;
               } else if (var10 == 120) {
                  offset += 2;
               } else if (var10 != 92 && var10 != 34) {
                  this.char1(var10);
               }

               var10 = bytes[offset++];
            }
         case 43:
         case 45:
         case 46:
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
            boolean sign = ch == 45 || ch == 43;
            if (sign) {
               if (offset >= end) {
                  throw numberError(offset, ch);
               }

               ch = bytes[offset++];
            }

            boolean dot = ch == 46;
            boolean num = false;
            if (!dot && ch >= 48 && ch <= 57) {
               num = true;

               do {
                  ch = offset == end ? 26 : bytes[offset++];
               } while (ch >= 48 && ch <= 57);
            }

            if (num && (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83)) {
               ch = bytes[offset++];
            }

            boolean small = false;
            if (ch == 46) {
               small = true;
               ch = offset == end ? 26 : bytes[offset++];
               if (ch >= 48 && ch <= 57) {
                  do {
                     ch = offset == end ? 26 : bytes[offset++];
                  } while (ch >= 48 && ch <= 57);
               }
            }

            if (!num && !small) {
               throw numberError(offset, ch);
            }

            if (ch == 101 || ch == 69) {
               ch = bytes[offset++];
               boolean eSign = false;
               if (ch == 43 || ch == 45) {
                  eSign = true;
                  if (offset >= end) {
                     throw numberError(offset, ch);
                  }

                  ch = bytes[offset++];
               }

               if (ch >= 48 && ch <= 57) {
                  do {
                     ch = offset == end ? 26 : bytes[offset++];
                  } while (ch >= 48 && ch <= 57);
               } else if (eSign) {
                  throw numberError(offset, ch);
               }
            }

            if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
               ch = offset == end ? 26 : bytes[offset++];
            }
            break;
         case 102:
            if (offset + 4 > end) {
               throw this.error(offset, ch);
            }

            if (bytes[offset] != 97 || bytes[offset + 1] != 108 || bytes[offset + 2] != 115 || bytes[offset + 3] != 101) {
               throw this.error(offset, ch);
            }

            offset += 4;
            ch = offset == end ? 26 : bytes[offset++];
            break;
         case 110:
            if (offset + 3 > end) {
               throw this.error(offset, ch);
            }

            if (bytes[offset] != 117 || bytes[offset + 1] != 108 || bytes[offset + 2] != 108) {
               throw this.error(offset, ch);
            }

            offset += 3;
            ch = offset == end ? 26 : bytes[offset++];
            break;
         case 116:
            if (offset + 3 > end) {
               throw this.error(offset, ch);
            }

            if (bytes[offset] != 114 || bytes[offset + 1] != 117 || bytes[offset + 2] != 101) {
               throw this.error(offset, ch);
            }

            offset += 3;
            ch = offset == end ? 26 : bytes[offset++];
            break;
         default:
            if (ch == 91) {
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
               ch = offset == end ? 26 : bytes[offset++];
            } else if (ch == 123) {
               this.next();

               while (this.ch != '}') {
                  this.skipName();
                  this.skipValue();
               }

               this.comma = false;
               offset = this.offset;
               ch = offset == end ? 26 : bytes[offset++];
            } else {
               if (ch != 83 || !this.nextIfSet()) {
                  throw this.error(offset, ch);
               }

               this.skipValue();
               ch = this.ch;
               offset = this.offset;
            }
      }

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : bytes[offset++];
      }

      if (ch == 44) {
         this.comma = true;
         ch = offset == end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : bytes[offset++];
         }
      }

      if (!this.comma && ch != 125 && ch != 93 && ch != 26) {
         throw this.error(offset, ch);
      } else if (!this.comma || ch != 125 && ch != 93 && ch != 26) {
         this.ch = (char)ch;
         this.offset = offset;
      } else {
         throw this.error(offset, ch);
      }
   }

   @Override
   public final String getString() {
      if (this.stringValue != null) {
         return this.stringValue;
      } else {
         byte[] bytes = this.bytes;
         int offset = this.nameBegin;
         int length = this.nameEnd - offset;
         if (!this.nameEscape) {
            return JDKUtils.ANDROID
               ? this.getLatin1String(offset, length)
               : new String(bytes, offset, length, this.nameAscii ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
         } else {
            char[] chars = new char[this.nameLength];
            int i = 0;

            while (true) {
               int c = bytes[offset];
               label81:
               if (c < 0) {
                  switch ((c & 0xFF) >> 4) {
                     case 12:
                     case 13:
                        int char2 = bytes[offset + 1];
                        if ((char2 & 192) != 128) {
                           throw new JSONException("malformed input around byte " + offset);
                        }

                        c = (c & 31) << 6 | char2 & 63;
                        offset += 2;
                        break;
                     case 14:
                        int char2 = bytes[offset + 1];
                        int char3 = bytes[offset + 2];
                        if ((char2 & 192) != 128 || (char3 & 192) != 128) {
                           throw new JSONException("malformed input around byte " + (offset + 2));
                        }

                        c = (c & 15) << 12 | (char2 & 63) << 6 | char3 & 63;
                        offset += 3;
                        break;
                     default:
                        if (c >> 3 == -2) {
                           int c2 = bytes[offset + 1];
                           int c3 = bytes[offset + 2];
                           int c4 = bytes[offset + 3];
                           offset += 4;
                           int uc = c << 18 ^ c2 << 12 ^ c3 << 6 ^ c4 ^ 3678080;
                           if ((c2 & 192) != 128 || (c3 & 192) != 128 || (c4 & 192) != 128 || uc < 65536 || uc >= 1114112) {
                              throw new JSONException("malformed input around byte " + offset);
                           }

                           chars[i++] = (char)((uc >>> 10) + 55232);
                           chars[i] = (char)((uc & 1023) + 56320);
                           break label81;
                        }

                        c &= 255;
                        offset++;
                  }

                  chars[i] = (char)c;
               } else {
                  if (c == 92) {
                     c = (char)bytes[++offset];
                     switch (c) {
                        case 34:
                        case 92:
                           break;
                        case 117: {
                           int c1 = bytes[offset + 1];
                           int c2 = bytes[offset + 2];
                           int c3 = bytes[offset + 3];
                           int c4 = bytes[offset + 4];
                           c = char4(c1, c2, c3, c4);
                           offset += 4;
                           break;
                        }
                        case 120: {
                           int c1 = bytes[offset + 1];
                           int c2 = bytes[offset + 2];
                           c = char2(c1, c2);
                           offset += 2;
                           break;
                        }
                        default:
                           c = this.char1(c);
                     }
                  } else if (c == 34) {
                     return this.stringValue = new String(chars);
                  }

                  chars[i] = (char)c;
                  offset++;
               }

               i++;
            }
         }
      }
   }

   @Override
   public final void skipComment() {
      int offset = this.offset;
      if (offset + 1 >= this.end) {
         throw new JSONException(this.info());
      } else {
         byte[] bytes = this.bytes;
         byte ch = bytes[offset++];
         boolean multi;
         if (ch == 42) {
            multi = true;
         } else {
            if (ch != 47) {
               return;
            }

            multi = false;
         }

         ch = bytes[offset++];

         while (true) {
            boolean endOfComment = false;
            if (multi) {
               if (ch == 42 && offset <= this.end && bytes[offset] == 47) {
                  offset++;
                  endOfComment = true;
               }
            } else {
               endOfComment = ch == 10;
            }

            if (endOfComment) {
               if (offset >= this.end) {
                  ch = 26;
               } else {
                  for (ch = bytes[offset]; ch <= 32 && (1L << ch & 4294981376L) != 0L; ch = bytes[offset]) {
                     if (++offset >= this.end) {
                        ch = 26;
                        break;
                     }
                  }

                  offset++;
               }
               break;
            }

            if (offset >= this.end) {
               ch = 26;
               break;
            }

            ch = bytes[offset++];
         }

         this.ch = (char)ch;
         this.offset = offset;
         if (ch == 47) {
            this.skipComment();
         }
      }
   }

   @Override
   public String readString() {
      byte[] bytes = this.bytes;
      if (this.ch != '"' && this.ch != '\'') {
         return this.readStringNotMatch();
      } else {
         char quote = this.ch;
         int offset = this.offset;
         int start = offset;
         int end = this.end;
         boolean ascii = true;
         this.valueEscape = false;

         for (int i = 0; offset < end; i++) {
            int ch = bytes[offset];
            if (ch == 92) {
               this.valueEscape = true;
               int var16 = bytes[offset + 1];
               offset += var16 == 117 ? 6 : (var16 == 120 ? 4 : 2);
            } else if (ch >= 0) {
               if (ch == quote) {
                  String str;
                  if (this.valueEscape) {
                     char[] chars = new char[i];
                     offset = start;
                     int ix = 0;

                     while (true) {
                        int chx = bytes[offset];
                        if (chx == 92) {
                           chx = bytes[++offset];
                           switch (chx) {
                              case 34:
                              case 92:
                                 break;
                              case 98:
                                 chx = 8;
                                 break;
                              case 102:
                                 chx = 12;
                                 break;
                              case 110:
                                 chx = 10;
                                 break;
                              case 114:
                                 chx = 13;
                                 break;
                              case 116:
                                 chx = 9;
                                 break;
                              case 117:
                                 chx = JSONFactory.DIGITS2[bytes[offset + 1]] * 4096
                                    + JSONFactory.DIGITS2[bytes[offset + 2]] * 256
                                    + JSONFactory.DIGITS2[bytes[offset + 3]] * 16
                                    + JSONFactory.DIGITS2[bytes[offset + 4]];
                                 offset += 4;
                                 break;
                              case 120:
                                 chx = JSONFactory.DIGITS2[bytes[offset + 1]] * 16 + JSONFactory.DIGITS2[bytes[offset + 2]];
                                 offset += 2;
                                 break;
                              default:
                                 chx = this.char1(chx);
                           }

                           chars[ix] = (char)chx;
                           offset++;
                        } else {
                           if (chx == quote) {
                              str = new String(chars);
                              break;
                           }

                           if (chx >= 0) {
                              chars[ix] = (char)chx;
                              offset++;
                           } else {
                              switch ((chx & 0xFF) >> 4) {
                                 case 12:
                                 case 13:
                                    int c2 = bytes[offset + 1];
                                    chars[ix] = (char)((chx & 31) << 6 | c2 & 63);
                                    offset += 2;
                                    break;
                                 case 14:
                                    chars[ix] = (char)((chx & 15) << 12 | (bytes[offset + 1] & 63) << 6 | bytes[offset + 2] & 63);
                                    offset += 3;
                                    break;
                                 default:
                                    char2_utf8(bytes, offset, chx, chars, ix);
                                    offset += 4;
                                    ix++;
                              }
                           }
                        }

                        ix++;
                     }
                  } else if (ascii) {
                     ch = offset - start;
                     if (ch == 1) {
                        str = TypeUtils.toString((char)(bytes[start] & 255));
                     } else if (ch == 2) {
                        str = TypeUtils.toString((char)(bytes[start] & 255), (char)(bytes[start + 1] & 255));
                     } else if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                        char[] chars = new char[ch];

                        for (int ix = 0; ix < ch; ix++) {
                           chars[ix] = (char)bytes[start + ix];
                        }

                        str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                     } else if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                        byte[] buf = Arrays.copyOfRange(bytes, start, offset);
                        str = JDKUtils.STRING_CREATOR_JDK11.apply(buf, JDKUtils.LATIN1);
                     } else if (JDKUtils.ANDROID) {
                        str = this.getLatin1String(start, offset - start);
                     } else {
                        str = new String(bytes, start, offset - start, StandardCharsets.ISO_8859_1);
                     }
                  } else {
                     str = new String(bytes, start, offset - start, StandardCharsets.UTF_8);
                  }

                  if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
                     str = str.trim();
                  }

                  if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
                     str = null;
                  }

                  offset++;
                  ch = offset == end ? 26 : bytes[offset++];

                  while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
                     ch = offset == end ? 26 : bytes[offset++];
                  }

                  if (this.comma = ch == 44) {
                     ch = offset == end ? 26 : bytes[offset++];

                     while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
                        ch = offset == end ? 26 : bytes[offset++];
                     }
                  }

                  this.ch = (char)ch;
                  this.offset = offset;
                  return str;
               }

               offset++;
            } else {
               ascii = false;
               switch ((ch & 0xFF) >> 4) {
                  case 12:
                  case 13:
                     offset += 2;
                     break;
                  case 14:
                     offset += 3;
                     break;
                  default:
                     if (ch >> 3 != -2) {
                        throw new JSONException("malformed input around byte " + offset);
                     }

                     offset += 4;
                     i++;
               }
            }
         }

         throw new JSONException("invalid escape character EOI");
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
      byte[] bytes = this.bytes;
      int ch = this.ch;
      int offset = this.offset;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = (char)bytes[offset++];
         if (ch == quote) {
            int var13 = offset == this.end ? 26 : bytes[offset++];

            while (var13 <= 32 && (1L << var13 & 4294981376L) != 0L) {
               var13 = offset == this.end ? 26 : bytes[offset++];
            }

            this.ch = (char)var13;
            this.offset = offset;
            this.comma = this.nextIfComma();
            this.wasNull = true;
            this.valueType = 5;
            return;
         }
      }

      int start = offset;
      int multmin;
      if (ch == 45) {
         if (offset == this.end) {
            throw new JSONException(this.info("illegal input"));
         }

         multmin = -214748364;
         this.negative = true;
         ch = bytes[offset++];
      } else {
         if (ch == 43) {
            if (offset == this.end) {
               throw new JSONException(this.info("illegal input"));
            }

            ch = bytes[offset++];
         }

         multmin = -214748364;
      }

      boolean intOverflow = false;

      for (this.valueType = 1; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
         valid = true;
         if (!intOverflow) {
            int digit = ch - 48;
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
            ch = 26;
            offset++;
            break;
         }
      }

      if (ch == 46) {
         this.valueType = 2;

         for (ch = bytes[offset++]; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
            valid = true;
            if (!intOverflow) {
               int digit = ch - 48;
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
               ch = 26;
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

            this.stringValue = new String(bytes, numStart, offset - 1 - numStart);
         } else {
            this.bigInt(bytes, numStart, offset - 1);
         }
      } else {
         this.mag3 = -this.mag3;
      }

      if (ch == 101 || ch == 69) {
         boolean negativeExp = false;
         int expValue = 0;
         ch = bytes[offset++];
         if (ch == 45) {
            negativeExp = true;
            ch = bytes[offset++];
         } else if (ch == 43) {
            ch = (char)bytes[offset++];
         }

         while (ch >= 48 && ch <= 57) {
            valid = true;
            int byteVal = ch - 48;
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = 26;
               break;
            }

            ch = bytes[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 110) {
            if (bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
               offset += 3;
               valid = true;
               this.wasNull = true;
               this.valueType = 5;
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         } else if (ch == 116 && bytes[offset] == 114 && bytes[offset + 1] == 117 && bytes[offset + 2] == 101) {
            offset += 3;
            valid = true;
            this.boolValue = true;
            this.valueType = 4;
            ch = offset == this.end ? 26 : bytes[offset++];
         } else if (ch == 102) {
            if (offset + 4 <= this.end && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset) == IOUtils.ALSE) {
               valid = true;
               offset += 4;
               this.boolValue = false;
               this.valueType = 4;
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         } else {
            if (ch == 123 && quote == 0) {
               this.offset = offset;
               this.ch = (char)ch;
               this.complex = this.readObject();
               this.valueType = 6;
               return;
            }

            if (ch == 91 && quote == 0) {
               this.offset = offset;
               this.ch = (char)ch;
               this.complex = this.readArray();
               this.valueType = 7;
               return;
            }
         }
      }

      if (quote != 0) {
         if (ch != quote) {
            this.offset = firstOffset;
            this.ch = (char)quote;
            this.readString0();
            this.valueType = 3;
            return;
         }

         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
         switch (ch) {
            case 66:
               if (!intOverflow && this.valueType != 2) {
                  this.valueType = 9;
               }
               break;
            case 68:
               this.valueType = 13;
               break;
            case 70:
               this.valueType = 12;
               break;
            case 76:
               if (offset - start < 19 && this.valueType != 2) {
                  this.valueType = 11;
               }
               break;
            case 83:
               if (!intOverflow && this.valueType != 2) {
                  this.valueType = 10;
               }
         }

         ch = offset == this.end ? 26 : bytes[offset++];
      }

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset == this.end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.offset = offset;
         this.ch = (char)ch;
      }
   }

   @Override
   public final void readNumber(ValueConsumer consumer, boolean quoted) {
      boolean valid = false;
      this.wasNull = false;
      this.boolValue = false;
      this.mag0 = 0;
      this.mag1 = 0;
      this.mag2 = 0;
      this.mag3 = 0;
      this.negative = false;
      this.exponent = 0;
      this.scale = 0;
      byte[] bytes = this.bytes;
      char quote = 0;
      if (this.ch == '"' || this.ch == '\'') {
         quote = this.ch;
         this.ch = (char)bytes[this.offset++];
      }

      int start = this.offset;
      if (this.ch == '-') {
         this.negative = true;
         this.ch = (char)bytes[this.offset++];
      }

      boolean intOverflow = false;

      for (this.valueType = 1; this.ch >= '0' && this.ch <= '9'; this.ch = (char)bytes[this.offset++]) {
         valid = true;
         if (!intOverflow) {
            int mag3_10 = this.mag3 * 10 + (this.ch - '0');
            if (mag3_10 < this.mag3) {
               intOverflow = true;
            } else {
               this.mag3 = mag3_10;
            }
         }
      }

      if (this.ch == '.') {
         this.valueType = 2;

         for (this.ch = (char)bytes[this.offset++]; this.ch >= '0' && this.ch <= '9'; this.ch = (char)bytes[this.offset++]) {
            valid = true;
            if (!intOverflow) {
               int mag3_10 = this.mag3 * 10 + (this.ch - '0');
               if (mag3_10 < this.mag3) {
                  intOverflow = true;
               } else {
                  this.mag3 = mag3_10;
               }
            }

            this.scale++;
         }
      }

      if (intOverflow) {
         int numStart = this.negative ? start : start - 1;
         this.bigInt(bytes, numStart, this.offset - 1);
      }

      if (this.ch == 'e' || this.ch == 'E') {
         boolean negativeExp = false;
         int expValue = 0;
         this.ch = (char)bytes[this.offset++];
         if (this.ch == '-') {
            negativeExp = true;
            this.ch = (char)bytes[this.offset++];
         } else if (this.ch == '+') {
            this.ch = (char)bytes[this.offset++];
         }

         while (this.ch >= '0' && this.ch <= '9') {
            valid = true;
            int byteVal = this.ch - '0';
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            this.ch = (char)bytes[this.offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      int len = this.offset - start;
      if (this.offset == start) {
         if (this.ch == 'n') {
            if (bytes[this.offset++] == 117 && bytes[this.offset++] == 108 && bytes[this.offset++] == 108) {
               valid = true;
               this.wasNull = true;
               this.valueType = 5;
               this.ch = (char)bytes[this.offset++];
            }
         } else if (this.ch == 't') {
            if (bytes[this.offset++] == 114 && bytes[this.offset++] == 117 && bytes[this.offset++] == 101) {
               valid = true;
               this.boolValue = true;
               this.valueType = 4;
               this.ch = (char)bytes[this.offset++];
            }
         } else if (this.ch == 'f') {
            if (this.offset + 4 <= this.end && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset) == IOUtils.ALSE) {
               valid = true;
               this.offset += 4;
               this.boolValue = false;
               this.valueType = 4;
               this.ch = (char)bytes[this.offset++];
            }
         } else {
            if (this.ch == '{' && quote == 0) {
               this.complex = this.readObject();
               this.valueType = 6;
               return;
            }

            if (this.ch == '[' && quote == 0) {
               this.complex = this.readArray();
               this.valueType = 7;
               return;
            }
         }
      }

      if (quote != 0) {
         if (this.ch != quote) {
            this.offset--;
            this.ch = quote;
            this.readString0();
            this.valueType = 3;
            return;
         }

         this.ch = (char)bytes[this.offset++];
      }

      while (this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L) {
         if (this.offset >= this.end) {
            this.ch = 26;
         } else {
            this.ch = (char)bytes[this.offset++];
         }
      }

      if (this.comma = this.ch == ',') {
         this.ch = (char)bytes[this.offset++];
         if (this.offset >= this.end) {
            this.ch = 26;
         } else {
            while (this.ch <= ' ' && (1L << this.ch & 4294981376L) != 0L) {
               if (this.offset >= this.end) {
                  this.ch = 26;
               } else {
                  this.ch = (char)bytes[this.offset++];
               }
            }
         }
      }

      if (quoted || this.valueType != 1 && this.valueType != 2) {
         if (this.valueType == 1) {
            if (this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 != Integer.MIN_VALUE) {
               int intValue = this.negative ? -this.mag3 : this.mag3;
               consumer.accept(intValue);
               return;
            }

            if (this.mag0 == 0 && this.mag1 == 0) {
               long v3 = (long)this.mag3 & 4294967295L;
               long v2 = (long)this.mag2 & 4294967295L;
               if (v2 <= 2147483647L) {
                  long v23 = (v2 << 32) + v3;
                  long longValue = this.negative ? -v23 : v23;
                  consumer.accept(longValue);
                  return;
               }
            }
         }

         Number number = this.getNumber();
         consumer.accept(number);
         if (!valid) {
            throw new JSONException(this.info("illegal input error"));
         }
      } else {
         consumer.accept(bytes, start - 1, len);
      }
   }

   @Override
   public final boolean readIfNull() {
      byte[] bytes = this.bytes;
      int ch = this.ch;
      int offset = this.offset;
      if (ch == 110 && bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
         if (offset + 3 == this.end) {
            ch = 26;
         } else {
            ch = (char)bytes[offset + 3];
         }

         offset += 4;

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == this.end ? 26 : (char)bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.offset = offset;
         this.ch = (char)ch;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean isNull() {
      return this.ch == 'n' && this.offset < this.end && this.bytes[this.offset] == 117;
   }

   @Override
   public final Date readNullOrNewDate() {
      byte[] bytes = this.bytes;
      int ch = this.ch;
      int offset = this.offset;
      Date date = null;
      byte var11;
      if (offset + 2 < this.end && bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
         if (offset + 3 == this.end) {
            var11 = 26;
         } else {
            var11 = bytes[offset + 3];
         }

         offset += 4;
      } else {
         if (offset + 1 >= this.end || bytes[offset] != 101 || bytes[offset + 1] != 119) {
            throw new JSONException("json syntax error, not match null or new Date" + offset);
         }

         if (offset + 3 == this.end) {
            var11 = 26;
         } else {
            var11 = bytes[offset + 2];
         }

         offset += 3;

         while (var11 <= 32 && (1L << var11 & 4294981376L) != 0L) {
            var11 = offset == this.end ? 26 : bytes[offset++];
         }

         if (offset + 4 >= this.end || var11 != 68 || bytes[offset] != 97 || bytes[offset + 1] != 116 || bytes[offset + 2] != 101) {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         if (offset + 3 == this.end) {
            var11 = 26;
         } else {
            var11 = bytes[offset + 3];
         }

         offset += 4;

         while (var11 <= 32 && (1L << var11 & 4294981376L) != 0L) {
            var11 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var11 != 40 || offset >= this.end) {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         var11 = bytes[offset++];

         while (var11 <= 32 && (1L << var11 & 4294981376L) != 0L) {
            var11 = offset == this.end ? 26 : bytes[offset++];
         }

         this.ch = (char)var11;
         this.offset = offset;
         long millis = this.readInt64Value();
         int var10 = this.ch;
         offset = this.offset;
         if (var10 != ')') {
            throw new JSONException("json syntax error, not match new Date" + offset);
         }

         var11 = offset >= this.end ? 26 : bytes[offset++];
         date = new Date(millis);
      }

      while (var11 <= 32 && (1L << var11 & 4294981376L) != 0L) {
         var11 = offset == this.end ? 26 : bytes[offset++];
      }

      if (this.comma = var11 == 44) {
         var11 = offset == this.end ? 26 : bytes[offset++];

         while (var11 <= 32 && (1L << var11 & 4294981376L) != 0L) {
            var11 = offset == this.end ? 26 : bytes[offset++];
         }
      }

      this.offset = offset;
      this.ch = (char)var11;
      return date;
   }

   @Override
   public final boolean nextIfNull() {
      int offset = this.offset;
      if (this.ch == 'n' && offset + 2 < this.end && this.bytes[offset] == 117) {
         this.readNull();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void readNull() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;
      if (bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
         offset += 3;
         int var5 = offset == this.end ? 26 : bytes[offset++];

         while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
            var5 = offset >= this.end ? 26 : bytes[offset++];
         }

         if (this.comma = var5 == 44) {
            var5 = offset >= this.end ? 26 : bytes[offset++];

            while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
               var5 = offset >= this.end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)var5;
         this.offset = offset;
      } else {
         throw new JSONException("json syntax error, not match null" + offset);
      }
   }

   @Override
   public final int getStringLength() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("string length only support string input " + this.ch);
      } else {
         char quote = this.ch;
         int len = 0;
         int i = this.offset;
         byte[] bytes = this.bytes;
         int i8 = i + 8;
         if (i8 < this.end
            && i8 < bytes.length
            && bytes[i] != quote
            && bytes[i + 1] != quote
            && bytes[i + 2] != quote
            && bytes[i + 3] != quote
            && bytes[i + 4] != quote
            && bytes[i + 5] != quote
            && bytes[i + 6] != quote
            && bytes[i + 7] != quote) {
            i += 8;
            len += 8;
         }

         while (i < this.end && bytes[i] != quote) {
            i++;
            len++;
         }

         return len;
      }
   }

   @Override
   public final LocalDate readLocalDate() {
      byte[] bytes = this.bytes;
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
            if (c10 < bytes.length && c10 < this.end && bytes[offset + 4] == 45 && bytes[offset + 7] == 45 && bytes[offset + 10] == quote) {
               byte y0 = bytes[offset];
               byte y1 = bytes[offset + 1];
               byte y2 = bytes[offset + 2];
               byte y3 = bytes[offset + 3];
               byte m0 = bytes[offset + 5];
               byte m1 = bytes[offset + 6];
               byte d0 = bytes[offset + 8];
               byte d1 = bytes[offset + 9];
               if (y0 >= 48 && y0 <= 57 && y1 >= 48 && y1 <= 57 && y2 >= 48 && y2 <= 57 && y3 >= 48 && y3 <= 57) {
                  int year = (y0 - 48) * 1000 + (y1 - 48) * 100 + (y2 - 48) * 10 + (y3 - 48);
                  if (m0 >= 48 && m0 <= 57 && m1 >= 48 && m1 <= 57) {
                     int month = (m0 - 48) * 10 + (m1 - 48);
                     if (d0 >= 48 && d0 <= 57 && d1 >= 48 && d1 <= 57) {
                        int dom = (d0 - 48) * 10 + (d1 - 48);

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
               if (bytes[i] == quote) {
                  nextQuoteOffset = i;
               }
            }

            if (nextQuoteOffset != -1 && nextQuoteOffset - offset > 10 && bytes[nextQuoteOffset - 6] == 45 && bytes[nextQuoteOffset - 3] == 45) {
               i = TypeUtils.parseInt(bytes, offset, nextQuoteOffset - offset - 6);
               int month = TypeUtils.parseInt(bytes, nextQuoteOffset - 5, 2);
               int dayOfMonth = TypeUtils.parseInt(bytes, nextQuoteOffset - 2, 2);
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
      byte[] bytes = this.bytes;
      int offset = this.offset;
      JSONReader.Context context = this.context;
      if ((this.ch == '"' || this.ch == '\'')
         && (
            context.dateFormat == null || context.formatyyyyMMddhhmmss19 || context.formatyyyyMMddhhmmssT19 || context.formatyyyyMMdd8 || context.formatISO8601
         )) {
         char quote = this.ch;
         int off21 = offset + 19;
         byte c10;
         if (off21 < bytes.length
            && off21 < this.end
            && bytes[offset + 4] == 45
            && bytes[offset + 7] == 45
            && ((c10 = bytes[offset + 10]) == 32 || c10 == 84)
            && bytes[offset + 13] == 58
            && bytes[offset + 16] == 58) {
            byte y0 = bytes[offset];
            byte y1 = bytes[offset + 1];
            byte y2 = bytes[offset + 2];
            byte y3 = bytes[offset + 3];
            byte m0 = bytes[offset + 5];
            byte m1 = bytes[offset + 6];
            byte d0 = bytes[offset + 8];
            byte d1 = bytes[offset + 9];
            byte h0 = bytes[offset + 11];
            byte h1 = bytes[offset + 12];
            byte i0 = bytes[offset + 14];
            byte i1 = bytes[offset + 15];
            byte s0 = bytes[offset + 17];
            byte s1 = bytes[offset + 18];
            if (y0 < 48 || y0 > 57 || y1 < 48 || y1 > 57 || y2 < 48 || y2 > 57 || y3 < 48 || y3 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int year = (y0 - 48) * 1000 + (y1 - 48) * 100 + (y2 - 48) * 10 + (y3 - 48);
            if (m0 < 48 || m0 > 57 || m1 < 48 || m1 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int month = (m0 - 48) * 10 + (m1 - 48);
            if (d0 < 48 || d0 > 57 || d1 < 48 || d1 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int dom = (d0 - 48) * 10 + (d1 - 48);
            if (h0 < 48 || h0 > 57 || h1 < 48 || h1 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int hour = (h0 - 48) * 10 + (h1 - 48);
            if (i0 < 48 || i0 > 57 || i1 < 48 || i1 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int minute = (i0 - 48) * 10 + (i1 - 48);
            if (s0 < 48 || s0 > 57 || s1 < 48 || s1 > 57) {
               ZonedDateTime zdt = this.readZonedDateTime();
               return zdt == null ? null : zdt.toOffsetDateTime();
            }

            int second = (s0 - 48) * 10 + (s1 - 48);

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
               if (i < end && i < this.end && i < bytes.length) {
                  if (bytes[i] != quote || bytes[i - 1] != 90) {
                     i++;
                     continue;
                  }

                  nanoSize = i - start - 2;
                  len = i - offset + 1;
               }

               if (nanoSize != -1 || len == 21) {
                  start = nanoSize <= 0 ? 0 : DateUtils.readNanos(bytes, nanoSize, offset + 20);
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
      byte[] bytes = this.bytes;
      int offset = this.offset;
      JSONReader.Context context = this.context;
      if ((this.ch == '"' || this.ch == '\'') && context.dateFormat == null) {
         char quote = this.ch;
         int off10 = offset + 8;
         if (off10 < bytes.length && off10 < this.end && bytes[offset + 2] == 58 && bytes[offset + 5] == 58) {
            byte h0 = bytes[offset];
            byte h1 = bytes[offset + 1];
            byte i0 = bytes[offset + 3];
            byte i1 = bytes[offset + 4];
            byte s0 = bytes[offset + 6];
            byte s1 = bytes[offset + 7];
            if (h0 >= 48 && h0 <= 57 && h1 >= 48 && h1 <= 57) {
               int hour = (h0 - 48) * 10 + (h1 - 48);
               if (i0 >= 48 && i0 <= 57 && i1 >= 48 && i1 <= 57) {
                  int minute = (i0 - 48) * 10 + (i1 - 48);
                  if (s0 >= 48 && s0 <= 57 && s1 >= 48 && s1 <= 57) {
                     int second = (s0 - 48) * 10 + (s1 - 48);
                     int nanoSize = -1;
                     int len = 0;
                     int start = offset + 8;
                     int zoneOffset = start;

                     for (int zoneOffsetSize = offset + 25; zoneOffset < zoneOffsetSize && zoneOffset < this.end && zoneOffset < bytes.length; zoneOffset++) {
                        byte b = bytes[zoneOffset];
                        if (nanoSize == -1 && (b == 90 || b == 43 || b == 45)) {
                           nanoSize = zoneOffset - start - 1;
                        }

                        if (b == quote) {
                           len = zoneOffset - offset;
                           break;
                        }
                     }

                     start = nanoSize <= 0 ? 0 : DateUtils.readNanos(bytes, nanoSize, offset + 9);
                     int zoneOffsetSize = len - 9 - nanoSize;
                     ZoneOffset zoneOffsetx;
                     if (zoneOffsetSize <= 1) {
                        zoneOffsetx = ZoneOffset.UTC;
                     } else {
                        String zonedId = new String(bytes, offset + 9 + nanoSize, zoneOffsetSize);
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
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else if (len < 19) {
         return null;
      } else {
         ZonedDateTime zdt;
         if (len == 30 && this.bytes[this.offset + 29] == 90) {
            LocalDateTime ldt = DateUtils.parseLocalDateTime29(this.bytes, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 29 && this.bytes[this.offset + 28] == 90) {
            LocalDateTime ldt = DateUtils.parseLocalDateTime28(this.bytes, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 28 && this.bytes[this.offset + 27] == 90) {
            LocalDateTime ldt = DateUtils.parseLocalDateTime27(this.bytes, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else if (len == 27 && this.bytes[this.offset + 26] == 90) {
            LocalDateTime ldt = DateUtils.parseLocalDateTime26(this.bytes, this.offset);
            zdt = ZonedDateTime.of(ldt, ZoneOffset.UTC);
         } else {
            zdt = DateUtils.parseZonedDateTime(this.bytes, this.offset, len, this.context.zoneId);
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
   public final LocalDate readLocalDate8() {
      if (!this.isString()) {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate8(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate9(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt;
         try {
            ldt = DateUtils.parseLocalDate10(this.bytes, this.offset);
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
   protected final LocalDate readLocalDate11() {
      if (!this.isString()) {
         throw new JSONException("localDate only support string input");
      } else {
         LocalDate ldt = DateUtils.parseLocalDate11(this.bytes, this.offset);
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
   protected final LocalDateTime readLocalDateTime17() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime17(this.bytes, this.offset);
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
   protected final LocalTime readLocalTime5() {
      if (this.ch != '"' && this.ch != '\'') {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime5(this.bytes, this.offset);
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
         LocalTime time = DateUtils.parseLocalTime6(this.bytes, this.offset);
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
         LocalTime time = DateUtils.parseLocalTime7(this.bytes, this.offset);
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
         LocalTime time = DateUtils.parseLocalTime8(this.bytes, this.offset);
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
         LocalTime time = DateUtils.parseLocalTime8(this.bytes, this.offset);
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
   protected final LocalTime readLocalTime10() {
      if (!this.isString()) {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime10(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime11(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime12(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("localTime only support string input");
      } else {
         LocalTime time = DateUtils.parseLocalTime18(this.bytes, this.offset);
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
   protected final LocalDateTime readLocalDateTime12() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime12(this.bytes, this.offset);
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
   protected final LocalDateTime readLocalDateTime14() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime14(this.bytes, this.offset);
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
   protected final LocalDateTime readLocalDateTime16() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime16(this.bytes, this.offset);
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
   protected final LocalDateTime readLocalDateTime18() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime18(this.bytes, this.offset);
         this.offset += 19;
         this.next();
         if (this.comma = this.ch == ',') {
            this.next();
         }

         return ldt;
      }
   }

   @Override
   protected final LocalDateTime readLocalDateTime19() {
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime19(this.bytes, this.offset);
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
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime20(this.bytes, this.offset);
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
         long millis = DateUtils.parseMillis19(this.bytes, this.offset, this.context.zoneId);
         if (this.bytes[this.offset + 19] != quote) {
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
      if (!this.isString()) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt;
         if (this.bytes[this.offset + len - 1] == 90) {
            ZonedDateTime zdt = DateUtils.parseZonedDateTime(this.bytes, this.offset, len);
            ldt = zdt.toInstant().atZone(this.context.getZoneId()).toLocalDateTime();
         } else {
            ldt = DateUtils.parseLocalDateTimeX(this.bytes, this.offset, len);
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
   public final BigDecimal readBigDecimal() {
      boolean valid = false;
      byte[] bytes = this.bytes;
      int ch = this.ch;
      int offset = this.offset;
      boolean value = false;
      BigDecimal decimal = null;
      int quote = 0;
      if (ch == 34 || ch == 39) {
         quote = ch;
         ch = bytes[offset++];
         if (ch == quote) {
            this.ch = offset == this.end ? 26 : (char)bytes[offset++];
            this.offset = offset;
            this.nextIfComma();
            return null;
         }
      }

      int start = offset;
      if (ch == 45) {
         this.negative = true;
         ch = bytes[offset++];
      } else {
         this.negative = false;
         if (ch == 43) {
            ch = bytes[offset++];
         }
      }

      this.valueType = 1;
      boolean overflow = false;

      long longValue;
      for (longValue = 0L; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
         valid = true;
         if (!overflow) {
            long r = longValue * 10L;
            if ((longValue | 10L) >>> 31 != 0L && r / 10L != longValue) {
               overflow = true;
            } else {
               longValue = r + (long)(ch - 48);
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
      if (ch == 46) {
         this.valueType = 2;

         for (ch = bytes[offset++]; ch >= 48 && ch <= 57; ch = bytes[offset++]) {
            valid = true;
            this.scale++;
            if (!overflow) {
               long r = longValue * 10L;
               if ((longValue | 10L) >>> 31 != 0L && r / 10L != longValue) {
                  overflow = true;
               } else {
                  longValue = r + (long)(ch - 48);
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
      if (ch == 101 || ch == 69) {
         ch = bytes[offset++];
         boolean negativeExp;
         if ((negativeExp = ch == 45) || ch == 43) {
            ch = bytes[offset++];
         }

         while (ch >= 48 && ch <= 57) {
            valid = true;
            int byteVal = ch - 48;
            expValue = expValue * 10 + byteVal;
            if (expValue > 2047) {
               throw new JSONException("too large exp value : " + expValue);
            }

            if (offset == this.end) {
               ch = 26;
               offset++;
               break;
            }

            ch = bytes[offset++];
         }

         if (negativeExp) {
            expValue = -expValue;
         }

         this.exponent = (short)expValue;
         this.valueType = 2;
      }

      if (offset == start) {
         if (ch == 110 && bytes[offset++] == 117 && bytes[offset++] == 108 && bytes[offset++] == 108) {
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            value = true;
            ch = offset == this.end ? 26 : bytes[offset];
            offset++;
            valid = true;
         } else if (ch == 116 && offset + 3 <= this.end && bytes[offset] == 114 && bytes[offset + 1] == 117 && bytes[offset + 2] == 101) {
            valid = true;
            offset += 3;
            value = true;
            decimal = BigDecimal.ONE;
            ch = offset == this.end ? 26 : bytes[offset];
            offset++;
         } else if (ch == 102
            && offset + 4 <= this.end
            && bytes[offset] == 97
            && bytes[offset + 1] == 108
            && bytes[offset + 2] == 115
            && bytes[offset + 3] == 101) {
            valid = true;
            offset += 4;
            decimal = BigDecimal.ZERO;
            value = true;
            ch = offset == this.end ? 26 : bytes[offset];
            offset++;
         } else {
            if (ch == 123 && quote == 0) {
               JSONObject jsonObject = new JSONObject();
               this.readObject(jsonObject, 0L);
               this.wasNull = false;
               return this.decimal(jsonObject);
            }

            if (ch == 91 && quote == 0) {
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

         ch = offset >= this.end ? 26 : bytes[offset++];
      }

      if (!value) {
         if (expValue == 0 && !overflow && longValue != 0L) {
            decimal = BigDecimal.valueOf(this.negative ? -longValue : longValue, this.scale);
            value = true;
         }

         if (!value) {
            decimal = TypeUtils.parseBigDecimal(bytes, start - 1, len);
         }

         if (ch == 76 || ch == 70 || ch == 68 || ch == 66 || ch == 83) {
            ch = offset >= this.end ? 26 : bytes[offset++];
         }
      }

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset == this.end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }
      }

      if (!valid) {
         throw new JSONException(this.info("illegal input error"));
      } else {
         this.ch = (char)ch;
         this.offset = offset;
         return decimal;
      }
   }

   @Override
   public final UUID readUUID() {
      int ch = this.ch;
      if (ch == 110) {
         this.readNull();
         return null;
      } else if (ch != 34 && ch != 39) {
         throw new JSONException(this.info("syntax error, can not read uuid"));
      } else {
         byte[] bytes = this.bytes;
         int offset = this.offset;
         long hi = 0L;
         long lo = 0L;
         if (offset + 36 < bytes.length
            && bytes[offset + 36] == ch
            && bytes[offset + 8] == 45
            && bytes[offset + 13] == 45
            && bytes[offset + 18] == 45
            && bytes[offset + 23] == 45) {
            for (int i = 0; i < 8; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            for (int i = 9; i < 13; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            for (int i = 14; i < 18; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            for (int i = 19; i < 23; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            for (int i = 24; i < 36; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            offset += 37;
         } else {
            if (offset + 32 >= bytes.length || bytes[offset + 32] != ch) {
               String str = this.readString();
               return str.isEmpty() ? null : UUID.fromString(str);
            }

            for (int i = 0; i < 16; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            for (int i = 16; i < 32; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[offset + i] - 48];
            }

            offset += 33;
         }

         int var10 = offset == this.end ? 26 : bytes[offset++];

         while (var10 <= 32 && (1L << var10 & 4294981376L) != 0L) {
            var10 = offset == this.end ? 26 : bytes[offset++];
         }

         this.offset = offset;
         if (this.comma = var10 == 44) {
            this.next();
         } else {
            this.ch = (char)var10;
         }

         return new UUID(hi, lo);
      }
   }

   @Override
   public final String readPattern() {
      if (this.ch != '/') {
         throw new JSONException("illegal pattern");
      } else {
         byte[] bytes = this.bytes;
         int offset = this.offset;
         int start = offset;

         while (offset < this.end && bytes[offset] != 47) {
            offset++;
         }

         String str = new String(bytes, start, offset - start, StandardCharsets.UTF_8);
         offset++;
         int ch = offset == this.end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = ch == 44) {
            ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.offset = offset;
         this.ch = (char)ch;
         return str;
      }
   }

   @Override
   public boolean nextIfNullOrEmptyString() {
      char first = this.ch;
      int end = this.end;
      int offset = this.offset;
      byte[] bytes = this.bytes;
      if (first == 'n' && offset + 2 < end && bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
         offset += 3;
      } else {
         if (first != '"' && first != '\'' || offset >= end || bytes[offset] != first) {
            return false;
         }

         offset++;
      }

      int ch = offset == end ? 26 : bytes[offset++];

      while (ch >= 0 && ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : bytes[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset == end ? 26 : bytes[offset++];
      }

      while (ch >= 0 && ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == end ? 26 : bytes[offset++];
      }

      if (ch < 0) {
         this.char_utf8(ch, offset);
         return true;
      } else {
         this.offset = offset;
         this.ch = (char)ch;
         return true;
      }
   }

   @Override
   public final boolean nextIfMatchIdent(char c0, char c1) {
      if (this.ch != c0) {
         return false;
      } else {
         byte[] bytes = this.bytes;
         int offset = this.offset;
         if (offset + 1 <= this.end && bytes[offset] == c1) {
            offset++;
            int ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }

            if (offset == this.offset + 2 && ch != 26 && ch != 40 && ch != 91 && ch != 93 && ch != 41 && ch != 58 && ch != 44) {
               return false;
            } else {
               this.offset = offset;
               this.ch = (char)ch;
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
         byte[] bytes = this.bytes;
         int offset = this.offset;
         if (offset + 2 <= this.end && bytes[offset] == c1 && bytes[offset + 1] == c2) {
            offset += 2;
            int ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }

            if (offset == this.offset + 3 && ch != 26 && ch != 40 && ch != 91 && ch != 93 && ch != 41 && ch != 58 && ch != 44) {
               return false;
            } else {
               this.offset = offset;
               this.ch = (char)ch;
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
         byte[] bytes = this.bytes;
         int offset = this.offset;
         if (offset + 3 <= this.end && bytes[offset] == c1 && bytes[offset + 1] == c2 && bytes[offset + 2] == c3) {
            offset += 3;
            int ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }

            if (offset == this.offset + 4 && ch != 26 && ch != 40 && ch != 91 && ch != 93 && ch != 41 && ch != 58 && ch != 44) {
               return false;
            } else {
               this.offset = offset;
               this.ch = (char)ch;
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
         byte[] bytes = this.bytes;
         int offset = this.offset;
         if (offset + 4 <= this.end && bytes[offset] == c1 && bytes[offset + 1] == c2 && bytes[offset + 2] == c3 && bytes[offset + 3] == c4) {
            offset += 4;
            int ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }

            if (offset == this.offset + 5 && ch != 26 && ch != 40 && ch != 91 && ch != 93 && ch != 41 && ch != 58 && ch != 44) {
               return false;
            } else {
               this.offset = offset;
               this.ch = (char)ch;
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
         byte[] bytes = this.bytes;
         int offset = this.offset;
         if (offset + 5 <= this.end
            && bytes[offset] == c1
            && bytes[offset + 1] == c2
            && bytes[offset + 2] == c3
            && bytes[offset + 3] == c4
            && bytes[offset + 4] == c5) {
            offset += 5;
            int ch = offset == this.end ? 26 : bytes[offset++];

            while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
               ch = offset == this.end ? 26 : bytes[offset++];
            }

            if (offset == this.offset + 6 && ch != 26 && ch != 40 && ch != 91 && ch != 93 && ch != 41 && ch != 58 && ch != 44) {
               return false;
            } else {
               this.offset = offset;
               this.ch = (char)ch;
               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public final byte[] readHex() {
      int offset = this.offset;
      byte[] bytes = this.bytes;
      int ch = this.ch;
      if (ch == 120) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      if (ch != 39 && ch != 34) {
         throw syntaxError(offset, ch);
      } else {
         int start = offset;
         int var13 = offset == this.end ? 26 : bytes[offset++];

         while (var13 >= 48 && var13 <= 57 || var13 >= 65 && var13 <= 70) {
            var13 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var13 != ch) {
            throw syntaxError(offset, var13);
         } else {
            var13 = offset == this.end ? 26 : bytes[offset++];
            int len = offset - start - 2;
            if (var13 == 26) {
               len++;
            }

            if (len % 2 != 0) {
               throw syntaxError(offset, var13);
            } else {
               byte[] hex = new byte[len / 2];

               for (int i = 0; i < hex.length; i++) {
                  byte c0 = bytes[start + i * 2];
                  byte c1 = bytes[start + i * 2 + 1];
                  int b0 = c0 - (c0 <= 57 ? 48 : 55);
                  int b1 = c1 - (c1 <= 57 ? 48 : 55);
                  hex[i] = (byte)(b0 << 4 | b1);
               }

               while (var13 <= 32 && (1L << var13 & 4294981376L) != 0L) {
                  var13 = offset == this.end ? 26 : bytes[offset++];
               }

               if (var13 == 44 && offset < this.end) {
                  this.comma = true;
                  var13 = offset == this.end ? 26 : bytes[offset++];

                  while (var13 == 0 || var13 <= 32 && (1L << var13 & 4294981376L) != 0L) {
                     var13 = offset == this.end ? 26 : bytes[offset++];
                  }

                  this.offset = offset;
                  this.ch = (char)var13;
                  if (this.ch == '/') {
                     this.skipComment();
                  }

                  return hex;
               } else {
                  this.offset = offset;
                  this.ch = (char)var13;
                  return hex;
               }
            }
         }
      }
   }

   @Override
   public boolean isReference() {
      byte[] bytes = this.bytes;
      int ch = this.ch;
      if (ch != 123) {
         return false;
      } else {
         int offset = this.offset;
         int end = this.end;
         if (offset == end) {
            return false;
         } else {
            for (var6 = bytes[offset]; var6 <= 32 && (1L << var6 & 4294981376L) != 0L; var6 = bytes[offset]) {
               if (++offset >= end) {
                  return false;
               }
            }

            if (offset + 6 < end
               && bytes[offset + 1] == 36
               && bytes[offset + 2] == 114
               && bytes[offset + 3] == 101
               && bytes[offset + 4] == 102
               && bytes[offset + 5] == var6) {
               offset += 6;

               for (var7 = bytes[offset]; var7 >= 0 && var7 <= 32 && (1L << var7 & 4294981376L) != 0L; var7 = bytes[offset]) {
                  if (++offset >= end) {
                     return false;
                  }
               }

               if (var7 == 58 && offset + 1 < end) {
                  for (var8 = bytes[++offset]; var8 >= 0 && var8 <= 32 && (1L << var8 & 4294981376L) != 0L; var8 = bytes[offset]) {
                     if (++offset >= end) {
                        return false;
                     }
                  }

                  if (var8 == var6 && (offset + 1 >= end || bytes[offset + 1] != 35)) {
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
         byte[] chars = this.bytes;
         this.offset = this.referenceBegin;
         this.ch = (char)chars[this.offset++];
         String reference = this.readString();
         int ch = this.ch;
         int offset = this.offset;

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == this.end ? 26 : chars[offset++];
         }

         if (ch != 125) {
            throw new JSONException("illegal reference : ".concat(reference));
         } else {
            int var5 = offset == this.end ? 26 : chars[offset++];

            while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
               var5 = offset == this.end ? 26 : chars[offset++];
            }

            if (this.comma = var5 == 44) {
               var5 = offset == this.end ? 26 : chars[offset++];

               while (var5 <= 32 && (1L << var5 & 4294981376L) != 0L) {
                  var5 = offset == this.end ? 26 : chars[offset++];
               }
            }

            this.ch = (char)var5;
            this.offset = offset;
            return reference;
         }
      }
   }

   @Override
   public final boolean readBoolValue() {
      this.wasNull = false;
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = this.ch;
      boolean val;
      if (ch == 116 && offset + 2 < bytes.length && bytes[offset] == 114 && bytes[offset + 1] == 117 && bytes[offset + 2] == 101) {
         offset += 3;
         val = true;
      } else if (ch == 102 && offset + 4 <= bytes.length && JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset) == IOUtils.ALSE) {
         offset += 4;
         val = false;
      } else {
         if (ch == 45 || ch >= 48 && ch <= 57) {
            this.readNumber();
            if (this.valueType != 1) {
               return false;
            }

            if ((this.context.features & JSONReader.Feature.NonZeroNumberCastToBooleanAsTrue.mask) != 0L) {
               return this.mag0 != 0 || this.mag1 != 0 || this.mag2 != 0 || this.mag3 != 0;
            }

            return this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 == 1;
         }

         if (ch == 110 && offset + 2 < bytes.length && bytes[offset] == 117 && bytes[offset + 1] == 108 && bytes[offset + 2] == 108) {
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("boolean value not support input null"));
            }

            this.wasNull = true;
            offset += 3;
            val = false;
         } else {
            if (ch != 34) {
               if (ch == 91) {
                  this.next();
                  val = this.readBoolValue();
                  if (!this.nextIfMatch(']')) {
                     throw new JSONException("not closed square brackets, expect ] but found : " + (char)ch);
                  }

                  return val;
               }

               throw new JSONException("syntax error : " + ch);
            }

            if (offset + 1 >= bytes.length || bytes[offset + 1] != 34) {
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

            byte c0 = bytes[offset];
            offset += 2;
            if (c0 != 48 && c0 != 78) {
               if (c0 != 49 && c0 != 89) {
                  throw new JSONException("can not convert to boolean : " + c0);
               }

               val = true;
            } else {
               val = false;
            }
         }
      }

      ch = offset == this.end ? 26 : (char)bytes[offset++];

      while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset >= this.end ? 26 : bytes[offset++];
      }

      if (this.comma = ch == 44) {
         ch = offset >= this.end ? 26 : bytes[offset++];

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset >= this.end ? 26 : bytes[offset++];
         }
      }

      this.offset = offset;
      this.ch = (char)ch;
      return val;
   }

   @Override
   public final String info(String message) {
      int line = 1;
      int column = 0;

      for (int i = 0; i < this.offset && i < this.end; column++) {
         if (this.bytes[i] == 10) {
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
      String str = new String(this.bytes, this.start, Math.min(this.length, 65535));
      buf.append(str);
      return buf.toString();
   }

   @Override
   public final void close() {
      if (this.cacheItem != null) {
         if (this.bytes.length < 4194304) {
            JSONFactory.BYTES_UPDATER.lazySet(this.cacheItem, this.bytes);
         }

         if (this.charBuf != null && this.charBuf.length < 4194304) {
            JSONFactory.CHARS_UPDATER.lazySet(this.cacheItem, this.charBuf);
         }
      }

      if (this.in != null) {
         try {
            this.in.close();
         } catch (IOException var2) {
         }
      }
   }
}
