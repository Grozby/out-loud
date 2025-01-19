package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.JDKUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

final class JSONBDump {
   static Charset GB18030;
   final byte[] bytes;
   final boolean raw;
   int offset;
   byte type;
   int strlen;
   byte strtype;
   int strBegin;
   String lastReference;
   final JSONWriter jsonWriter;
   final SymbolTable symbolTable;
   final Map<Integer, String> symbols = new HashMap<>();

   public JSONBDump(byte[] bytes, boolean raw) {
      this.bytes = bytes;
      this.raw = raw;
      this.jsonWriter = JSONWriter.ofPretty();
      this.symbolTable = null;
      this.dumpAny();
   }

   public JSONBDump(byte[] bytes, SymbolTable symbolTable, boolean raw) {
      this.bytes = bytes;
      this.raw = raw;
      this.symbolTable = symbolTable;
      this.jsonWriter = JSONWriter.ofPretty();
      this.dumpAny();
   }

   private void dumpAny() {
      if (this.offset < this.bytes.length) {
         this.type = this.bytes[this.offset++];
         switch (this.type) {
            case -112:
               int intValue = this.readInt32Value();
               this.jsonWriter.writeChar((char)intValue);
               return;
            case -111: {
               int len = this.readInt32Value();
               byte[] bytes = new byte[len];
               System.arraycopy(this.bytes, this.offset, bytes, 0, len);
               this.offset += len;
               this.jsonWriter.writeBinary(bytes);
               return;
            }
            case -110:
               boolean isInt = this.isInt();
               String typeName = null;
               int symbol;
               if (isInt) {
                  symbol = this.readInt32Value();
               } else {
                  typeName = this.readString();
                  symbol = this.readInt32Value();
                  this.symbols.put(symbol, typeName);
               }

               if (!this.raw && this.bytes[this.offset] == -90) {
                  if (typeName == null) {
                     typeName = this.getString(symbol);
                  }

                  this.offset++;
                  this.dumpObject(typeName);
                  return;
               }

               this.jsonWriter.startObject();
               this.jsonWriter.writeName("@type");
               this.jsonWriter.writeColon();
               if (typeName == null) {
                  if (symbol < 0) {
                     if (this.raw) {
                        this.jsonWriter.writeString("#" + symbol);
                     } else {
                        String name = this.symbolTable.getName(-symbol);
                        this.jsonWriter.writeString(name);
                     }
                  } else {
                     this.jsonWriter.writeString("#" + symbol);
                  }
               } else if (this.raw) {
                  this.jsonWriter.writeString(typeName + "#" + symbol);
               } else {
                  this.jsonWriter.writeString(typeName);
               }

               this.jsonWriter.writeName("@value");
               this.jsonWriter.writeColon();
               this.dumpAny();
               this.jsonWriter.endObject();
               return;
            case -109:
               this.dumpReference();
               break;
            case -90:
               this.dumpObject(null);
               break;
            case -88: {
               int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
               int month = this.bytes[this.offset++];
               int dayOfMonth = this.bytes[this.offset++];
               int hour = this.bytes[this.offset++];
               int minute = this.bytes[this.offset++];
               int second = this.bytes[this.offset++];
               int nano = this.readInt32Value();
               LocalDateTime localDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
               this.jsonWriter.writeLocalDateTime(localDateTime);
               break;
            }
            case -87: {
               int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
               int month = this.bytes[this.offset++];
               int dayOfMonth = this.bytes[this.offset++];
               LocalDate localDate = LocalDate.of(year, month, dayOfMonth);
               this.jsonWriter.writeLocalDate(localDate);
               break;
            }
            case -85:
            case -66: {
               long int64Value = ((long)this.bytes[this.offset + 7] & 255L)
                  + (((long)this.bytes[this.offset + 6] & 255L) << 8)
                  + (((long)this.bytes[this.offset + 5] & 255L) << 16)
                  + (((long)this.bytes[this.offset + 4] & 255L) << 24)
                  + (((long)this.bytes[this.offset + 3] & 255L) << 32)
                  + (((long)this.bytes[this.offset + 2] & 255L) << 40)
                  + (((long)this.bytes[this.offset + 1] & 255L) << 48)
                  + ((long)this.bytes[this.offset] << 56);
               this.offset += 8;
               this.jsonWriter.writeInt64(int64Value);
               return;
            }
            case -84:
            case -83:
            case 72: {
               int int32Value = (this.bytes[this.offset + 3] & 255)
                  + ((this.bytes[this.offset + 2] & 255) << 8)
                  + ((this.bytes[this.offset + 1] & 255) << 16)
                  + (this.bytes[this.offset] << 24);
               this.offset += 4;
               this.jsonWriter.writeInt32(int32Value);
               return;
            }
            case -82: {
               long epochSeconds = this.readInt64Value();
               int nano = this.readInt32Value();
               this.jsonWriter.writeInstant(Instant.ofEpochSecond(epochSeconds, (long)nano));
               break;
            }
            case -81:
               this.jsonWriter.writeNull();
               return;
            case -80:
               this.jsonWriter.writeBool(false);
               return;
            case -79:
               this.jsonWriter.writeBool(true);
               return;
            case -78:
               this.jsonWriter.writeDouble(0.0);
               return;
            case -77:
               this.jsonWriter.writeDouble(1.0);
               return;
            case -76:
               this.jsonWriter.writeDouble((double)this.readInt64Value());
               return;
            case -75: {
               long int64Value = ((long)this.bytes[this.offset + 7] & 255L)
                  + (((long)this.bytes[this.offset + 6] & 255L) << 8)
                  + (((long)this.bytes[this.offset + 5] & 255L) << 16)
                  + (((long)this.bytes[this.offset + 4] & 255L) << 24)
                  + (((long)this.bytes[this.offset + 3] & 255L) << 32)
                  + (((long)this.bytes[this.offset + 2] & 255L) << 40)
                  + (((long)this.bytes[this.offset + 1] & 255L) << 48)
                  + ((long)this.bytes[this.offset] << 56);
               this.offset += 8;
               this.jsonWriter.writeDouble(Double.longBitsToDouble(int64Value));
               return;
            }
            case -74:
               this.jsonWriter.writeFloat((float)this.readInt32Value());
               return;
            case -73: {
               int int32Value = (this.bytes[this.offset + 3] & 255)
                  + ((this.bytes[this.offset + 2] & 255) << 8)
                  + ((this.bytes[this.offset + 1] & 255) << 16)
                  + (this.bytes[this.offset] << 24);
               this.offset += 4;
               this.jsonWriter.writeFloat(Float.intBitsToFloat(int32Value));
               return;
            }
            case -72:
               this.jsonWriter.writeDecimal(BigDecimal.valueOf(this.readInt64Value()), 0L, null);
               return;
            case -71:
               int scale = this.readInt32Value();
               int type = this.bytes[this.offset++];
               BigInteger unscaledValue;
               switch (type) {
                  case -70:
                     unscaledValue = BigInteger.valueOf(this.readInt64Value());
                     break;
                  case -66:
                     long unscaledValueLong = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
                     unscaledValue = BigInteger.valueOf(JDKUtils.BIG_ENDIAN ? unscaledValueLong : Long.reverseBytes(unscaledValueLong));
                     this.offset += 8;
                     break;
                  case 72:
                     unscaledValue = BigInteger.valueOf((long)this.readInt32Value());
                     break;
                  default:
                     if (type >= -16 && type <= 47) {
                        unscaledValue = BigInteger.valueOf((long)type);
                     } else if (type >= 48 && type <= 63) {
                        unscaledValue = BigInteger.valueOf((long)((type - 56 << 8) + (this.bytes[this.offset++] & 255)));
                     } else if (type >= 64 && type <= 71) {
                        unscaledValue = BigInteger.valueOf(
                           (long)((type - 68 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255))
                        );
                     } else {
                        int lenx = this.readInt32Value();
                        byte[] bytesx = new byte[lenx];
                        System.arraycopy(this.bytes, this.offset, bytesx, 0, lenx);
                        this.offset += lenx;
                        unscaledValue = new BigInteger(bytesx);
                     }
               }

               BigDecimal decimal;
               if (scale == 0) {
                  decimal = new BigDecimal(unscaledValue);
               } else {
                  decimal = new BigDecimal(unscaledValue, scale);
               }

               this.jsonWriter.writeDecimal(decimal, 0L, null);
               return;
            case -70:
               this.jsonWriter.writeInt64(this.readInt64Value());
               return;
            case -69: {
               int len = this.readInt32Value();
               byte[] bytes = new byte[len];
               System.arraycopy(this.bytes, this.offset, bytes, 0, len);
               this.offset += len;
               this.jsonWriter.writeBigInt(new BigInteger(bytes));
               return;
            }
            case -68:
               this.jsonWriter.writeInt16((short)((this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255)));
               return;
            case -67:
               this.jsonWriter.writeInt8(this.bytes[this.offset++]);
               return;
            case -65: {
               int int32Value = (this.bytes[this.offset + 3] & 255)
                  + ((this.bytes[this.offset + 2] & 255) << 8)
                  + ((this.bytes[this.offset + 1] & 255) << 16)
                  + (this.bytes[this.offset] << 24);
               this.offset += 4;
               this.jsonWriter.writeInt64((long)int32Value);
               return;
            }
            case 122: {
               int strlen = this.readLength();
               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_8);
               this.offset += strlen;
               this.jsonWriter.writeString(str);
               return;
            }
            case 123: {
               int strlen = this.readLength();
               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16);
               this.offset += strlen;
               this.jsonWriter.writeString(str);
               return;
            }
            case 124: {
               int strlen = this.readLength();
               String str;
               if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
                  byte[] chars = new byte[strlen];
                  System.arraycopy(this.bytes, this.offset, chars, 0, strlen);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
               } else {
                  str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
               }

               this.offset += strlen;
               this.jsonWriter.writeString(str);
               return;
            }
            case 125: {
               int strlen = this.readLength();
               String str;
               if (JDKUtils.STRING_CREATOR_JDK11 != null && JDKUtils.BIG_ENDIAN) {
                  byte[] chars = new byte[strlen];
                  System.arraycopy(this.bytes, this.offset, chars, 0, strlen);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
               } else {
                  str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16BE);
               }

               this.offset += strlen;
               this.jsonWriter.writeString(str);
               return;
            }
            case 126: {
               if (GB18030 == null) {
                  GB18030 = Charset.forName("GB18030");
               }

               int strlen = this.readLength();
               String str = new String(this.bytes, this.offset, strlen, GB18030);
               this.offset += strlen;
               this.jsonWriter.writeString(str);
               return;
            }
            case 127:
               if (this.isInt()) {
                  int symbol = this.readInt32Value();
                  if (this.raw) {
                     this.jsonWriter.writeString("#" + symbol);
                  } else {
                     String name = this.getString(symbol);
                     this.jsonWriter.writeString(name);
                  }
               } else {
                  String name = this.readString();
                  int symbol = this.readInt32Value();
                  this.symbols.put(symbol, name);
                  if (this.raw) {
                     this.jsonWriter.writeString(name + "#" + symbol);
                  } else {
                     this.jsonWriter.writeString(name);
                  }
               }

               return;
            default:
               if (this.type >= -16 && this.type <= 47) {
                  this.jsonWriter.writeInt32(this.type);
                  return;
               }

               if (this.type >= -40 && this.type <= -17) {
                  long value = (long)(-8 + (this.type - -40));
                  this.jsonWriter.writeInt64(value);
                  return;
               }

               if (this.type >= 48 && this.type <= 63) {
                  int value = (this.type - 56 << 8) + (this.bytes[this.offset++] & 255);
                  this.jsonWriter.writeInt32(value);
                  return;
               }

               if (this.type >= 64 && this.type <= 71) {
                  int value = (this.type - 68 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255);
                  this.jsonWriter.writeInt32(value);
                  return;
               }

               if (this.type >= -56 && this.type <= -41) {
                  int value = (this.type - -48 << 8) + (this.bytes[this.offset++] & 255);
                  this.jsonWriter.writeInt32(value);
                  return;
               }

               if (this.type >= -64 && this.type <= -57) {
                  int value = (this.type - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255);
                  this.jsonWriter.writeInt64((long)value);
                  return;
               }

               if (this.type >= -108 && this.type <= -92) {
                  this.dumpArray();
                  return;
               }

               if (this.type >= 73) {
                  this.strlen = this.type == 121 ? this.readLength() : this.type - 73;
                  if (this.strlen < 0) {
                     this.jsonWriter.writeRaw("{\"$symbol\":");
                     this.jsonWriter.writeInt32(this.strlen);
                     this.jsonWriter.writeRaw("}");
                     return;
                  }

                  String strx = new String(this.bytes, this.offset, this.strlen, StandardCharsets.ISO_8859_1);
                  this.offset = this.offset + this.strlen;
                  this.jsonWriter.writeString(strx);
                  return;
               }

               throw new JSONException("not support type : " + JSONB.typeName(this.type) + ", offset " + this.offset);
         }
      }
   }

   private void dumpArray() {
      int len = this.type == -92 ? this.readLength() : this.type - -108;
      if (len == 0) {
         this.jsonWriter.writeRaw("[]");
      } else {
         if (len == 1) {
            this.type = this.bytes[this.offset];
            if (this.isInt() || this.type == -81 || this.type >= 73 && this.type <= 120) {
               this.jsonWriter.writeRaw("[");
               this.dumpAny();
               this.jsonWriter.writeRaw("]");
               return;
            }
         }

         this.jsonWriter.startArray();

         for (int i = 0; i < len; i++) {
            if (i != 0) {
               this.jsonWriter.writeComma();
            }

            if (this.isReference()) {
               this.dumpReference();
            } else {
               this.dumpAny();
            }
         }

         this.jsonWriter.endArray();
      }
   }

   private void dumpObject(String typeName) {
      if (typeName != null) {
         this.jsonWriter.startObject();
         this.jsonWriter.writeName("@type");
         this.jsonWriter.writeColon();
         this.jsonWriter.writeString(typeName);
      } else {
         if (this.bytes[this.offset] == -91) {
            this.jsonWriter.writeRaw("{}");
            this.offset++;
            return;
         }

         this.jsonWriter.startObject();
      }

      int valueCount = 0;

      while (true) {
         byte type = this.bytes[this.offset];
         switch (type) {
            case -109:
               this.dumpReference();
               break;
            case -91:
               this.offset++;
               this.jsonWriter.endObject();
               return;
            case 127:
               this.offset++;
               if (this.isInt()) {
                  int symbol = this.readInt32Value();
                  if (this.raw) {
                     this.jsonWriter.writeName("#" + symbol);
                  } else {
                     this.jsonWriter.writeName(this.getString(symbol));
                  }
               } else {
                  String name = this.readString();
                  int symbol = this.readInt32Value();
                  this.symbols.put(symbol, name);
                  if (this.raw) {
                     this.jsonWriter.writeName(name + "#" + symbol);
                  } else {
                     this.jsonWriter.writeName(name);
                  }
               }
               break;
            default:
               if (this.isString()) {
                  this.jsonWriter.writeName(this.readString());
               } else if (type >= -16 && type <= 72) {
                  this.jsonWriter.writeName(this.readInt32Value());
               } else if ((type < -40 || type > -17) && type != -66) {
                  if (valueCount != 0) {
                     this.jsonWriter.writeComma();
                  }

                  this.dumpAny();
               } else {
                  this.jsonWriter.writeName(this.readInt64Value());
               }
         }

         valueCount++;
         this.jsonWriter.writeColon();
         if (this.isReference()) {
            this.dumpReference();
         } else {
            this.dumpAny();
         }
      }
   }

   private void dumpReference() {
      this.jsonWriter.writeRaw("{\"$ref\":");
      String reference = this.readReference();
      this.jsonWriter.writeString(reference);
      if (!"#-1".equals(reference)) {
         this.lastReference = reference;
      }

      this.jsonWriter.writeRaw("}");
   }

   int readInt32Value() {
      byte type = this.bytes[this.offset++];
      if (type >= -16 && type <= 47) {
         return type;
      } else if (type >= 48 && type <= 63) {
         return (type - 56 << 8) + (this.bytes[this.offset++] & 0xFF);
      } else if (type >= 64 && type <= 71) {
         return (type - 68 << 16) + ((this.bytes[this.offset++] & 0xFF) << 8) + (this.bytes[this.offset++] & 0xFF);
      } else {
         switch (type) {
            case -84:
            case -83:
            case 72:
               int int32Value = (this.bytes[this.offset + 3] & 255)
                  + ((this.bytes[this.offset + 2] & 255) << 8)
                  + ((this.bytes[this.offset + 1] & 255) << 16)
                  + (this.bytes[this.offset] << 24);
               this.offset += 4;
               return int32Value;
            default:
               throw new JSONException("readInt32Value not support " + JSONB.typeName(type) + ", offset " + this.offset + "/" + this.bytes.length);
         }
      }
   }

   long readInt64Value() {
      byte type = this.bytes[this.offset++];
      if (type >= -16 && type <= 47) {
         return (long)type;
      } else if (type >= 48 && type <= 63) {
         return (long)((type - 56 << 8) + (this.bytes[this.offset++] & 255));
      } else if (type >= 64 && type <= 71) {
         return (long)((type - 68 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255));
      } else if (type >= -40 && type <= -17) {
         return -8L + (long)(type - -40);
      } else if (type >= -56 && type <= -41) {
         return (long)((type - -48 << 8) + (this.bytes[this.offset++] & 255));
      } else if (type >= -64 && type <= -57) {
         return (long)((type - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255));
      } else {
         switch (type) {
            case -85:
            case -66:
               long int64Value = ((long)this.bytes[this.offset + 7] & 255L)
                  + (((long)this.bytes[this.offset + 6] & 255L) << 8)
                  + (((long)this.bytes[this.offset + 5] & 255L) << 16)
                  + (((long)this.bytes[this.offset + 4] & 255L) << 24)
                  + (((long)this.bytes[this.offset + 3] & 255L) << 32)
                  + (((long)this.bytes[this.offset + 2] & 255L) << 40)
                  + (((long)this.bytes[this.offset + 1] & 255L) << 48)
                  + ((long)this.bytes[this.offset] << 56);
               this.offset += 8;
               return int64Value;
            case -68:
               int int16Value = (this.bytes[this.offset + 1] & 255) + (this.bytes[this.offset] << 8);
               this.offset += 2;
               return (long)int16Value;
            case -67:
               return (long)this.bytes[this.offset++];
            case -65:
            case 72:
               int int32Value = (this.bytes[this.offset + 3] & 255)
                  + ((this.bytes[this.offset + 2] & 255) << 8)
                  + ((this.bytes[this.offset + 1] & 255) << 16)
                  + (this.bytes[this.offset] << 24);
               this.offset += 4;
               return (long)int32Value;
            default:
               throw new JSONException("readInt64Value not support " + JSONB.typeName(type) + ", offset " + this.offset + "/" + this.bytes.length);
         }
      }
   }

   int readLength() {
      byte type = this.bytes[this.offset++];
      if (type >= -16 && type <= 47) {
         return type;
      } else if (type >= 64 && type <= 71) {
         return (type - 68 << 16) + ((this.bytes[this.offset++] & 0xFF) << 8) + (this.bytes[this.offset++] & 0xFF);
      } else if (type >= 48 && type <= 63) {
         return (type - 56 << 8) + (this.bytes[this.offset++] & 0xFF);
      } else if (type == 72) {
         return (this.bytes[this.offset++] << 24)
            + ((this.bytes[this.offset++] & 0xFF) << 16)
            + ((this.bytes[this.offset++] & 0xFF) << 8)
            + (this.bytes[this.offset++] & 0xFF);
      } else {
         throw new JSONException("not support length type : " + type);
      }
   }

   boolean isReference() {
      return this.offset < this.bytes.length && this.bytes[this.offset] == -109;
   }

   boolean isString() {
      byte type = this.bytes[this.offset];
      return type >= 73 && type <= 125;
   }

   String readString() {
      this.strtype = this.bytes[this.offset++];
      this.strBegin = this.offset;
      Charset charset;
      if (this.strtype >= 73 && this.strtype <= 121) {
         if (this.strtype == 121) {
            this.strlen = this.readLength();
            this.strBegin = this.offset;
         } else {
            this.strlen = this.strtype - 73;
         }

         charset = StandardCharsets.ISO_8859_1;
      } else if (this.strtype == 122) {
         this.strlen = this.readLength();
         this.strBegin = this.offset;
         charset = StandardCharsets.UTF_8;
      } else if (this.strtype == 123) {
         this.strlen = this.readLength();
         this.strBegin = this.offset;
         charset = StandardCharsets.UTF_16;
      } else if (this.strtype == 124) {
         this.strlen = this.readLength();
         this.strBegin = this.offset;
         if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
            byte[] chars = new byte[this.strlen];
            System.arraycopy(this.bytes, this.offset, chars, 0, this.strlen);
            String str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
            this.offset = this.offset + this.strlen;
            return str;
         }

         charset = StandardCharsets.UTF_16LE;
      } else {
         if (this.strtype != 125) {
            throw new JSONException("readString not support type " + JSONB.typeName(this.strtype) + ", offset " + this.offset + "/" + this.bytes.length);
         }

         this.strlen = this.readLength();
         this.strBegin = this.offset;
         if (JDKUtils.STRING_CREATOR_JDK11 != null && JDKUtils.BIG_ENDIAN) {
            byte[] chars = new byte[this.strlen];
            System.arraycopy(this.bytes, this.offset, chars, 0, this.strlen);
            String str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
            this.offset = this.offset + this.strlen;
            return str;
         }

         charset = StandardCharsets.UTF_16BE;
      }

      if (this.strlen < 0) {
         return this.symbolTable.getName(-this.strlen);
      } else {
         String str = new String(this.bytes, this.offset, this.strlen, charset);
         this.offset = this.offset + this.strlen;
         return str;
      }
   }

   String readReference() {
      if (this.bytes[this.offset] != -109) {
         return null;
      } else {
         this.offset++;
         if (this.isString()) {
            return this.readString();
         } else {
            throw new JSONException("reference not support input " + JSONB.typeName(this.type));
         }
      }
   }

   @Override
   public String toString() {
      return this.jsonWriter.toString();
   }

   boolean isInt() {
      int type = this.bytes[this.offset];
      return type >= -70 && type <= 72 || type == -83 || type == -84 || type == -85;
   }

   public String getString(int symbol) {
      String name;
      if (symbol < 0) {
         name = this.symbolTable.getName(-symbol);
      } else {
         name = this.symbols.get(symbol);
      }

      if (name == null) {
         throw new JSONException("symbol not found : " + symbol);
      } else {
         return name;
      }
   }
}
