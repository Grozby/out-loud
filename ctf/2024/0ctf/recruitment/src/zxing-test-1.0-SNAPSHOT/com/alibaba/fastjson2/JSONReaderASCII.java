package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class JSONReaderASCII extends JSONReaderUTF8 {
   final String str;

   JSONReaderASCII(JSONReader.Context ctx, String str, byte[] bytes, int offset, int length) {
      super(ctx, str, bytes, offset, length);
      this.str = str;
      this.nameAscii = true;
   }

   JSONReaderASCII(JSONReader.Context ctx, InputStream is) {
      super(ctx, is);
      this.nameAscii = true;
      this.str = null;
   }

   @Override
   public final void next() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      int ch = offset >= this.end ? 26 : bytes[offset++];

      while (ch == 0 || ch > 0 && ch <= 32 && (1L << ch & 4294981376L) != 0L) {
         ch = offset == this.end ? 26 : bytes[offset++];
      }

      this.offset = offset;
      this.ch = (char)(ch & 0xFF);
      if (ch == 47) {
         this.skipComment();
      }
   }

   @Override
   public final boolean nextIfObjectStart() {
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

         this.ch = (char)(var4 & 255);
         this.offset = offset;
         if (var4 == 47) {
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

      this.offset = offset;
      this.ch = (char)(ch & 0xFF);
      return true;
   }

   @Override
   public final long readFieldNameHashCode() {
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
               errorMsg = "illegal fieldName input " + ch;
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
         long nameValue = 0L;
         if (offset + 9 < this.end) {
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
            for (int i = 0; offset < this.end; i++) {
               ch = bytes[offset] & 255;
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
                  int var16 = bytes[++offset];
                  switch (var16) {
                     case 34:
                     case 92:
                     default:
                        ch = this.char1(var16);
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
               int var17 = bytes[offset];
               if (var17 == 92) {
                  this.nameEscape = true;
                  var17 = bytes[++offset];
                  char var19;
                  switch (var17) {
                     case 34:
                     case 92:
                     default:
                        var19 = this.char1(var17);
                        break;
                     case 117:
                        var19 = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        var19 = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)var19;
                  hashCode *= 1099511628211L;
               } else {
                  if (var17 == quote) {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                     break;
                  }

                  offset++;
                  hashCode ^= (long)var17;
                  hashCode *= 1099511628211L;
               }

               i++;
            }
         }

         int var20 = offset == this.end ? 26 : bytes[offset++];

         while (var20 <= 32 && (1L << var20 & 4294981376L) != 0L) {
            var20 = offset == this.end ? 26 : bytes[offset++];
         }

         if (var20 != 58) {
            throw new JSONException(this.info("expect ':', but " + var20));
         } else {
            var20 = offset == this.end ? 26 : bytes[offset++];

            while (var20 <= 32 && (1L << var20 & 4294981376L) != 0L) {
               var20 = offset == this.end ? 26 : bytes[offset++];
            }

            this.offset = offset;
            this.ch = (char)(var20 & 255);
            return hashCode;
         }
      }
   }

   @Override
   public final long readFieldNameHashCodeUnquote() {
      this.nameEscape = false;
      int offset = this.offset;
      int end = this.end;
      byte[] bytes = this.bytes;
      int ch = this.ch;
      this.nameBegin = offset - 1;
      int first = ch;
      long nameValue = 0L;

      label151:
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
               break label151;
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
            default:
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

               ch = offset == end ? 26 : bytes[offset++] & 255;
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
               ch = offset == end ? 26 : bytes[offset++] & 255;
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
                        ch = offset == end ? 26 : bytes[offset++] & 255;
                     }
                     break label131;
                  default:
                     hashCode ^= (long)ch;
                     hashCode *= 1099511628211L;
                     ch = offset == end ? 26 : bytes[offset++] & 255;
               }
            }

            i++;
         }
      }

      if (ch == 58) {
         ch = offset == end ? 26 : bytes[offset++] & 255;

         while (ch <= 32 && (1L << ch & 4294981376L) != 0L) {
            ch = offset == end ? 26 : bytes[offset++] & 255;
         }
      }

      this.offset = offset;
      this.ch = (char)ch;
      return hashCode;
   }

   public static long getLong(byte[] bytes, int off) {
      return JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off);
   }

   @Override
   public final long readValueHashCode() {
      int ch = this.ch;
      if (ch != 34 && ch != 39) {
         return -1L;
      } else {
         byte[] bytes = this.bytes;
         int quote = ch;
         this.nameAscii = true;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         long nameValue = 0L;

         for (int i = 0; offset < this.end; i++) {
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
               int var11 = bytes[++offset];
               switch (var11) {
                  case 34:
                  case 92:
                  default:
                     ch = this.char1(var11);
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
               int var12 = bytes[offset];
               if (var12 == 92) {
                  this.nameEscape = true;
                  var12 = bytes[++offset];
                  char var14;
                  switch (var12) {
                     case 34:
                     case 92:
                     default:
                        var14 = this.char1(var12);
                        break;
                     case 117:
                        var14 = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 120:
                        var14 = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                  }

                  offset++;
                  hashCode ^= (long)var14;
                  hashCode *= 1099511628211L;
               } else {
                  if (var12 == 34) {
                     this.nameLength = i;
                     this.nameEnd = offset++;
                     break;
                  }

                  offset++;
                  hashCode ^= (long)var12;
                  hashCode *= 1099511628211L;
               }

               i++;
            }
         }

         int var15 = offset == this.end ? 26 : bytes[offset++];

         while (var15 <= 32 && (1L << var15 & 4294981376L) != 0L) {
            var15 = offset == this.end ? 26 : bytes[offset++];
         }

         if (this.comma = var15 == 44) {
            var15 = offset == this.end ? 26 : bytes[offset++];

            while (var15 <= 32 && (1L << var15 & 4294981376L) != 0L) {
               var15 = offset == this.end ? 26 : bytes[offset++];
            }
         }

         this.offset = offset;
         this.ch = (char)(var15 & 255);
         return hashCode;
      }
   }

   @Override
   public final long getNameHashCodeLCase() {
      byte[] bytes = this.bytes;
      int offset = this.nameBegin;
      long nameValue = 0L;

      for (int i = 0; offset < this.end; offset++) {
         int c = bytes[offset];
         if (c == 92) {
            int var11 = bytes[++offset];
            switch (var11) {
               case 34:
               case 92:
               default:
                  c = this.char1(var11);
                  break;
               case 117:
                  c = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                  offset += 4;
                  break;
               case 120:
                  c = char2(bytes[offset + 1], bytes[offset + 2]);
                  offset += 2;
            }
         } else if (c == 34) {
            break;
         }

         if (c > 255 || c < 0 || i >= 8 || i == 0 && c == 0) {
            nameValue = 0L;
            offset = this.nameBegin;
            break;
         }

         if (c == 95 || c == 45 || c == 32) {
            byte c1 = bytes[offset + 1];
            if (c1 != 34 && c1 != 39 && c1 != c) {
               continue;
            }
         }

         if (c >= 65 && c <= 90) {
            c = (char)(c + 32);
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
            int cx = bytes[offset];
            if (cx == 92) {
               int var13 = bytes[++offset];
               switch (var13) {
                  case 34:
                  case 92:
                  default:
                     cx = this.char1(var13);
                     break;
                  case 117:
                     cx = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                     offset += 4;
                     break;
                  case 120:
                     cx = char2(bytes[offset + 1], bytes[offset + 2]);
                     offset += 2;
               }
            } else if (cx == 34) {
               break;
            }

            offset++;
            if (cx == 95 || cx == 45 || cx == 32) {
               byte c1 = bytes[offset];
               if (c1 != 34 && c1 != 39 && c1 != cx) {
                  continue;
               }
            }

            if (cx >= 65 && cx <= 90) {
               cx = (char)(cx + 32);
            }

            hashCode ^= cx < 0 ? (long)(cx & 0xFF) : (long)cx;
            hashCode *= 1099511628211L;
         }

         return hashCode;
      }
   }

   @Override
   public final String getFieldName() {
      byte[] bytes = this.bytes;
      int offset = this.nameBegin;
      int length = this.nameEnd - offset;
      if (!this.nameEscape) {
         if (this.str != null) {
            return this.str.substring(offset, this.nameEnd);
         } else {
            return JDKUtils.ANDROID ? this.getLatin1String(offset, length) : new String(bytes, offset, length, StandardCharsets.ISO_8859_1);
         }
      } else {
         if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            byte[] chars = new byte[this.nameLength];

            label69:
            for (int i = 0; offset < this.nameEnd; i++) {
               byte b = bytes[offset];
               if (b == 92) {
                  b = bytes[++offset];
                  switch (b) {
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
                        char ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        if (ch > 255) {
                           chars = null;
                           break label69;
                        }

                        b = (byte)ch;
                        break;
                     case 120:
                        char c = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                        if (c > 255) {
                           chars = null;
                           break label69;
                        }

                        b = (byte)c;
                        break;
                     default:
                        b = (byte)this.char1(b);
                  }
               } else if (b == 34) {
                  break;
               }

               chars[i] = b;
               offset++;
            }

            if (chars != null) {
               return JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.LATIN1);
            }
         }

         offset = this.nameBegin;
         char[] chars = new char[this.nameLength];

         for (int i = 0; offset < this.nameEnd; i++) {
            char ch = (char)(bytes[offset] & 255);
            if (ch == '\\') {
               ch = (char)bytes[++offset];
               switch (ch) {
                  case '*':
                  case '+':
                  case '-':
                  case '.':
                  case '/':
                  case '<':
                  case '=':
                  case '>':
                  case '@':
                     break;
                  case 'u':
                     ch = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                     offset += 4;
                     break;
                  case 'x':
                     ch = char2(bytes[offset + 1], bytes[offset + 2]);
                     offset += 2;
                     break;
                  default:
                     ch = this.char1(ch);
               }
            } else if (ch == '"') {
               break;
            }

            chars[i] = ch;
            offset++;
         }

         return new String(chars);
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
         byte[] bytes = this.bytes;
         this.nameEscape = false;
         int offset = this.nameBegin = this.offset;
         int nameBegin = this.nameBegin;

         for (int i = 0; offset < this.end; i++) {
            int c = bytes[offset];
            if (c == 92) {
               this.nameEscape = true;
               int var18 = bytes[offset + 1];
               offset += var18 == 117 ? 6 : (var18 == 120 ? 4 : 2);
            } else {
               if (c == quote) {
                  this.nameLength = i;
                  this.nameEnd = offset++;
                  int var19 = bytes[offset];

                  while (var19 <= 32 && (1L << var19 & 4294981376L) != 0L) {
                     var19 = bytes[++offset];
                  }

                  if (var19 != 58) {
                     throw syntaxError(offset, this.ch);
                  }

                  if (++offset >= this.end) {
                     this.ch = 26;
                     throw syntaxError(offset, this.ch);
                  }

                  var19 = bytes[offset];

                  while (var19 <= 32 && (1L << var19 & 4294981376L) != 0L) {
                     var19 = bytes[++offset];
                  }

                  this.offset = offset + 1;
                  this.ch = (char)var19;
                  break;
               }

               offset++;
            }
         }

         if (this.nameEnd < nameBegin) {
            throw new JSONException("syntax error : " + offset);
         } else {
            if (!this.nameEscape) {
               long nameValue0 = -1L;
               long nameValue1 = -1L;
               int length = this.nameEnd - nameBegin;
               switch (length) {
                  case 1:
                     return TypeUtils.toString(bytes[nameBegin]);
                  case 2:
                     return TypeUtils.toString(bytes[nameBegin], bytes[nameBegin + 1]);
                  case 3:
                     nameValue0 = (long)((bytes[nameBegin + 2] << 16) + ((bytes[nameBegin + 1] & 255) << 8) + (bytes[nameBegin] & 255));
                     break;
                  case 4:
                     nameValue0 = (long)(
                        (bytes[nameBegin + 3] << 24) + ((bytes[nameBegin + 2] & 255) << 16) + ((bytes[nameBegin + 1] & 255) << 8) + (bytes[nameBegin] & 255)
                     );
                     break;
                  case 5:
                     nameValue0 = ((long)bytes[nameBegin + 4] << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     break;
                  case 6:
                     nameValue0 = ((long)bytes[nameBegin + 5] << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     break;
                  case 7:
                     nameValue0 = ((long)bytes[nameBegin + 6] << 48)
                        + (((long)bytes[nameBegin + 5] & 255L) << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     break;
                  case 8:
                     nameValue0 = ((long)bytes[nameBegin + 7] << 56)
                        + (((long)bytes[nameBegin + 6] & 255L) << 48)
                        + (((long)bytes[nameBegin + 5] & 255L) << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     break;
                  case 9:
                     nameValue0 = (long)bytes[nameBegin];
                     nameValue1 = ((long)bytes[nameBegin + 8] << 56)
                        + (((long)bytes[nameBegin + 7] & 255L) << 48)
                        + (((long)bytes[nameBegin + 6] & 255L) << 40)
                        + (((long)bytes[nameBegin + 5] & 255L) << 32)
                        + (((long)bytes[nameBegin + 4] & 255L) << 24)
                        + (((long)bytes[nameBegin + 3] & 255L) << 16)
                        + (((long)bytes[nameBegin + 2] & 255L) << 8)
                        + ((long)bytes[nameBegin + 1] & 255L);
                     break;
                  case 10:
                     nameValue0 = (long)((bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                     nameValue1 = ((long)bytes[nameBegin + 9] << 56)
                        + (((long)bytes[nameBegin + 8] & 255L) << 48)
                        + (((long)bytes[nameBegin + 7] & 255L) << 40)
                        + (((long)bytes[nameBegin + 6] & 255L) << 32)
                        + (((long)bytes[nameBegin + 5] & 255L) << 24)
                        + (((long)bytes[nameBegin + 4] & 255L) << 16)
                        + (((long)bytes[nameBegin + 3] & 255L) << 8)
                        + ((long)bytes[nameBegin + 2] & 255L);
                     break;
                  case 11:
                     nameValue0 = (long)((bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                     nameValue1 = ((long)bytes[nameBegin + 10] << 56)
                        + (((long)bytes[nameBegin + 9] & 255L) << 48)
                        + (((long)bytes[nameBegin + 8] & 255L) << 40)
                        + (((long)bytes[nameBegin + 7] & 255L) << 32)
                        + (((long)bytes[nameBegin + 6] & 255L) << 24)
                        + (((long)bytes[nameBegin + 5] & 255L) << 16)
                        + (((long)bytes[nameBegin + 4] & 255L) << 8)
                        + ((long)bytes[nameBegin + 3] & 255L);
                     break;
                  case 12:
                     nameValue0 = (long)((bytes[nameBegin + 3] << 24) + (bytes[nameBegin + 2] << 16) + (bytes[nameBegin + 1] << 8) + bytes[nameBegin]);
                     nameValue1 = ((long)bytes[nameBegin + 11] << 56)
                        + (((long)bytes[nameBegin + 10] & 255L) << 48)
                        + (((long)bytes[nameBegin + 9] & 255L) << 40)
                        + (((long)bytes[nameBegin + 8] & 255L) << 32)
                        + (((long)bytes[nameBegin + 7] & 255L) << 24)
                        + (((long)bytes[nameBegin + 6] & 255L) << 16)
                        + (((long)bytes[nameBegin + 5] & 255L) << 8)
                        + ((long)bytes[nameBegin + 4] & 255L);
                     break;
                  case 13:
                     nameValue0 = ((long)bytes[nameBegin + 4] << 32)
                        + ((long)bytes[nameBegin + 3] << 24)
                        + ((long)bytes[nameBegin + 2] << 16)
                        + ((long)bytes[nameBegin + 1] << 8)
                        + (long)bytes[nameBegin];
                     nameValue1 = ((long)bytes[nameBegin + 12] << 56)
                        + (((long)bytes[nameBegin + 11] & 255L) << 48)
                        + (((long)bytes[nameBegin + 10] & 255L) << 40)
                        + (((long)bytes[nameBegin + 9] & 255L) << 32)
                        + (((long)bytes[nameBegin + 8] & 255L) << 24)
                        + (((long)bytes[nameBegin + 7] & 255L) << 16)
                        + (((long)bytes[nameBegin + 6] & 255L) << 8)
                        + ((long)bytes[nameBegin + 5] & 255L);
                     break;
                  case 14:
                     nameValue0 = ((long)bytes[nameBegin + 5] << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     nameValue1 = ((long)bytes[nameBegin + 13] << 56)
                        + (((long)bytes[nameBegin + 12] & 255L) << 48)
                        + (((long)bytes[nameBegin + 11] & 255L) << 40)
                        + (((long)bytes[nameBegin + 10] & 255L) << 32)
                        + (((long)bytes[nameBegin + 9] & 255L) << 24)
                        + (((long)bytes[nameBegin + 8] & 255L) << 16)
                        + (((long)bytes[nameBegin + 7] & 255L) << 8)
                        + ((long)bytes[nameBegin + 6] & 255L);
                     break;
                  case 15:
                     nameValue0 = ((long)bytes[nameBegin + 6] << 48)
                        + (((long)bytes[nameBegin + 5] & 255L) << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     nameValue1 = ((long)bytes[nameBegin + 14] << 56)
                        + (((long)bytes[nameBegin + 13] & 255L) << 48)
                        + (((long)bytes[nameBegin + 12] & 255L) << 40)
                        + (((long)bytes[nameBegin + 11] & 255L) << 32)
                        + (((long)bytes[nameBegin + 10] & 255L) << 24)
                        + (((long)bytes[nameBegin + 9] & 255L) << 16)
                        + (((long)bytes[nameBegin + 8] & 255L) << 8)
                        + ((long)bytes[nameBegin + 7] & 255L);
                     break;
                  case 16:
                     nameValue0 = ((long)bytes[nameBegin + 7] << 56)
                        + (((long)bytes[nameBegin + 6] & 255L) << 48)
                        + (((long)bytes[nameBegin + 5] & 255L) << 40)
                        + (((long)bytes[nameBegin + 4] & 255L) << 32)
                        + (((long)bytes[nameBegin + 3] & 255L) << 24)
                        + (((long)bytes[nameBegin + 2] & 255L) << 16)
                        + (((long)bytes[nameBegin + 1] & 255L) << 8)
                        + ((long)bytes[nameBegin] & 255L);
                     nameValue1 = ((long)bytes[nameBegin + 15] << 56)
                        + (((long)bytes[nameBegin + 14] & 255L) << 48)
                        + (((long)bytes[nameBegin + 13] & 255L) << 40)
                        + (((long)bytes[nameBegin + 12] & 255L) << 32)
                        + (((long)bytes[nameBegin + 11] & 255L) << 24)
                        + (((long)bytes[nameBegin + 10] & 255L) << 16)
                        + (((long)bytes[nameBegin + 9] & 255L) << 8)
                        + ((long)bytes[nameBegin + 8] & 255L);
               }

               if (nameValue0 != -1L) {
                  if (nameValue1 != -1L) {
                     long nameValue01 = nameValue0 ^ nameValue1;
                     int indexMask = (int)(nameValue01 ^ nameValue01 >>> 32) & JSONFactory.NAME_CACHE2.length - 1;
                     JSONFactory.NameCacheEntry2 entry = JSONFactory.NAME_CACHE2[indexMask];
                     if (entry == null) {
                        char[] chars = new char[length];

                        for (int ix = 0; ix < length; ix++) {
                           chars[ix] = (char)(bytes[nameBegin + ix] & 255);
                        }

                        String name;
                        if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                           name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                        } else {
                           name = new String(chars);
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
                        char[] chars = new char[length];

                        for (int ix = 0; ix < length; ix++) {
                           chars[ix] = (char)(bytes[nameBegin + ix] & 255);
                        }

                        String name;
                        if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                           name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                        } else {
                           name = new String(chars);
                        }

                        JSONFactory.NAME_CACHE[indexMaskx] = new JSONFactory.NameCacheEntry(name, nameValue0);
                        return name;
                     }

                     if (entryx.value == nameValue0) {
                        return entryx.name;
                     }
                  }
               }
            }

            return this.getFieldName();
         }
      }
   }

   @Override
   protected final void readString0() {
      byte[] bytes = this.bytes;
      char quote = this.ch;
      int start = this.offset;
      int offset = this.offset;
      this.valueEscape = false;
      int i = 0;

      while (true) {
         int c = bytes[offset];
         if (c == 92) {
            this.valueEscape = true;
            int var12 = bytes[offset + 1];
            offset += var12 == 117 ? 6 : (var12 == 120 ? 4 : 2);
         } else {
            if (c == quote) {
               String str;
               if (this.valueEscape) {
                  char[] chars = new char[i];
                  offset = start;
                  int ix = 0;

                  while (true) {
                     char cx = (char)(bytes[offset] & 255);
                     if (cx == '\\') {
                        cx = (char)bytes[++offset];
                        switch (cx) {
                           case '"':
                           case '\\':
                              break;
                           case 'u':
                              cx = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                              offset += 4;
                              break;
                           case 'x':
                              cx = char2(bytes[offset + 1], bytes[offset + 2]);
                              offset += 2;
                              break;
                           default:
                              cx = this.char1(cx);
                        }
                     } else if (cx == '"') {
                        str = new String(chars);
                        break;
                     }

                     chars[ix] = cx;
                     offset++;
                     ix++;
                  }
               } else if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                  byte[] buf = Arrays.copyOfRange(bytes, start, offset);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(buf, JDKUtils.LATIN1);
               } else {
                  str = new String(bytes, start, offset - start, StandardCharsets.ISO_8859_1);
               }

               c = bytes[++offset];

               while (c > 0 && c <= 32 && (1L << c & 4294981376L) != 0L) {
                  c = bytes[++offset];
               }

               this.offset = offset + 1;
               if (this.comma = c == 44) {
                  this.next();
               } else {
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
         byte[] bytes = this.bytes;
         byte quote = (byte)this.ch;
         byte slash = 92;
         int offset = this.offset;
         int start = offset;
         int end = this.end;
         boolean valueEscape = false;
         int i = 0;
         byte c0 = (byte)0;
         byte c1 = (byte)0;
         byte c2 = 0;
         boolean quoted = false;

         for (int upperBound = offset + (end - offset & -4); offset < upperBound; i += 4) {
            c0 = bytes[offset];
            c1 = bytes[offset + 1];
            c2 = bytes[offset + 2];
            byte c3 = bytes[offset + 3];
            if (c0 == 92 || c1 == 92 || c2 == 92 || c3 == 92) {
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
               if (offset >= end) {
                  throw new JSONException("invalid escape character EOI");
               }

               byte c = bytes[offset];
               if (c == 92) {
                  valueEscape = true;
                  c = bytes[offset + 1];
                  offset += c == 117 ? 6 : (c == 120 ? 4 : 2);
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
            c1 = (byte)0;

            while (true) {
               char c = (char)(bytes[offset] & 255);
               if (c == '\\') {
                  c = (char)bytes[++offset];
                  switch (c) {
                     case '"':
                     case '\\':
                        break;
                     case 'b':
                        c = '\b';
                        break;
                     case 'f':
                        c = '\f';
                        break;
                     case 'n':
                        c = '\n';
                        break;
                     case 'r':
                        c = '\r';
                        break;
                     case 't':
                        c = '\t';
                        break;
                     case 'u':
                        c = char4(bytes[offset + 1], bytes[offset + 2], bytes[offset + 3], bytes[offset + 4]);
                        offset += 4;
                        break;
                     case 'x':
                        c = char2(bytes[offset + 1], bytes[offset + 2]);
                        offset += 2;
                        break;
                     default:
                        c = this.char1(c);
                  }
               } else if (c == quote) {
                  str = new String(buf);
                  break;
               }

               buf[c1] = c;
               offset++;
               c1++;
            }
         } else if (this.str != null) {
            str = this.str.substring(start, offset);
         } else if (JDKUtils.STRING_CREATOR_JDK11 != null) {
            str = JDKUtils.STRING_CREATOR_JDK11.apply(Arrays.copyOfRange(bytes, start, offset), JDKUtils.LATIN1);
         } else if (JDKUtils.ANDROID) {
            str = this.getLatin1String(start, offset - start);
         } else {
            str = new String(bytes, start, offset - start, StandardCharsets.ISO_8859_1);
         }

         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         offset++;
         c0 = offset == end ? 26 : bytes[offset++];

         while (c0 <= 32 && (1L << c0 & 4294981376L) != 0L) {
            c0 = offset == end ? 26 : bytes[offset++];
         }

         if (this.comma = c0 == 44) {
            c0 = offset == end ? 26 : bytes[offset++];

            while (c0 <= 32 && (1L << c0 & 4294981376L) != 0L) {
               c0 = offset == end ? 26 : bytes[offset++];
            }
         }

         this.ch = (char)(c0 & 255);
         this.offset = offset;
         return str;
      }
   }
}
