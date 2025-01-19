package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplInt32Array;
import com.alibaba.fastjson2.reader.ObjectReaderImplInt32ValueArray;
import com.alibaba.fastjson2.reader.ObjectReaderImplInt64Array;
import com.alibaba.fastjson2.reader.ObjectReaderImplInt64ValueArray;
import com.alibaba.fastjson2.reader.ObjectReaderImplStringArray;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class JSONReaderJSONB extends JSONReader {
   static final long BASE = (long)JDKUtils.UNSAFE.arrayBaseOffset(byte[].class);
   static final byte[] SHANGHAI_ZONE_ID_NAME_BYTES = JSONB.toBytes("Asia/Shanghai");
   static Charset GB18030;
   static final byte[] FIXED_TYPE_SIZE;
   protected final byte[] bytes;
   protected final int length;
   protected final int end;
   protected byte type;
   protected int strlen;
   protected byte strtype;
   protected int strBegin;
   protected byte[] valueBytes;
   protected char[] charBuf;
   protected final JSONFactory.CacheItem cacheItem;
   protected final SymbolTable symbolTable;
   protected long symbol0Hash;
   protected int symbol0Begin;
   protected int symbol0Length;
   protected byte symbol0StrType;
   protected long[] symbols;

   JSONReaderJSONB(JSONReader.Context ctx, InputStream is) {
      super(ctx, true, false);
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
      this.end = this.length;
      this.symbolTable = ctx.symbolTable;
   }

   JSONReaderJSONB(JSONReader.Context ctx, byte[] bytes, int off, int length) {
      super(ctx, true, false);
      this.bytes = bytes;
      this.offset = off;
      this.length = length;
      this.end = off + length;
      this.symbolTable = ctx.symbolTable;
      this.cacheItem = JSONFactory.CACHE_ITEMS[System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1];
   }

   @Override
   public String getString() {
      byte strtype = this.strtype;
      int strlen = this.strlen;
      if (strtype == -81) {
         return null;
      } else if (strlen < 0) {
         return this.symbolTable.getName(-strlen);
      } else {
         Charset charset;
         if (strtype == 121) {
            charset = StandardCharsets.ISO_8859_1;
         } else if (strtype >= 73 && strtype <= 120) {
            if (JDKUtils.STRING_CREATOR_JDK8 != null) {
               char[] chars = new char[strlen];

               for (int i = 0; i < strlen; i++) {
                  chars[i] = (char)(this.bytes[this.strBegin + i] & 255);
               }

               return JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
            }

            if (JDKUtils.STRING_CREATOR_JDK11 != null) {
               byte[] chars = new byte[strlen];
               System.arraycopy(this.bytes, this.strBegin, chars, 0, strlen);
               return JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.LATIN1);
            }

            charset = StandardCharsets.ISO_8859_1;
         } else if (strtype == 122) {
            charset = StandardCharsets.UTF_8;
         } else if (strtype == 123) {
            charset = StandardCharsets.UTF_16;
         } else if (strtype == 124) {
            charset = StandardCharsets.UTF_16LE;
         } else {
            if (strtype != 125) {
               throw notSupportType(strtype);
            }

            charset = StandardCharsets.UTF_16BE;
         }

         return new String(this.bytes, this.strBegin, strlen, charset);
      }
   }

   public int readLength() {
      byte type = this.bytes[this.offset++];
      if (type >= -16 && type <= 47) {
         return type;
      } else if (type >= 48 && type <= 63) {
         return (type - 56 << 8) + (this.bytes[this.offset++] & 0xFF);
      } else if (type >= 64 && type <= 71) {
         int len = getInt3(this.bytes, this.offset, type);
         this.offset += 2;
         return len;
      } else if (type == 72) {
         int len = getInt(this.bytes, this.offset);
         this.offset += 4;
         if (len > 268435456) {
            throw new JSONException("input length overflow");
         } else {
            return len;
         }
      } else {
         throw notSupportType(type);
      }
   }

   static int getInt3(byte[] bytes, int offset, int type) {
      return (type - 68 << 16) + ((bytes[offset] & 0xFF) << 8) + (bytes[offset + 1] & 0xFF);
   }

   @Override
   public boolean isArray() {
      if (this.offset >= this.bytes.length) {
         return false;
      } else {
         byte type = this.bytes[this.offset];
         return type >= -108 && type <= -92;
      }
   }

   @Override
   public boolean isObject() {
      return this.offset < this.end && this.bytes[this.offset] == -90;
   }

   @Override
   public boolean isNumber() {
      byte type = this.bytes[this.offset];
      return type >= -78 && type <= 72;
   }

   @Override
   public boolean isString() {
      return this.offset < this.bytes.length && (this.type = this.bytes[this.offset]) >= 73;
   }

   @Override
   public boolean nextIfMatch(char ch) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfArrayStart() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfComma() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfArrayEnd() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfObjectStart() {
      if (this.bytes[this.offset] != -90) {
         return false;
      } else {
         this.offset++;
         return true;
      }
   }

   @Override
   public boolean nextIfObjectEnd() {
      if (this.bytes[this.offset] != -91) {
         return false;
      } else {
         this.offset++;
         return true;
      }
   }

   @Override
   public boolean nextIfNullOrEmptyString() {
      if (this.bytes[this.offset] == -81) {
         this.offset++;
         return true;
      } else if (this.bytes[this.offset] != 73) {
         return false;
      } else {
         this.offset++;
         return true;
      }
   }

   @Override
   public <T> T read(Type type) {
      boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = this.context.provider.getObjectReader(type, fieldBased);
      return (T)objectReader.readJSONBObject(this, null, null, 0L);
   }

   @Override
   public <T> T read(Class<T> type) {
      boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = this.context.provider.getObjectReader(type, fieldBased);
      return (T)objectReader.readJSONBObject(this, null, null, 0L);
   }

   @Override
   public Map<String, Object> readObject() {
      byte[] bytes = this.bytes;
      long features = this.context.features;
      this.type = bytes[this.offset++];
      if (this.type == -81) {
         return null;
      } else if (this.type < -90) {
         if (this.type == -110) {
            ObjectReader objectReader = this.checkAutoType(Map.class, 0L, 0L);
            return (Map<String, Object>)objectReader.readObject(this, null, null, 0L);
         } else {
            throw notSupportType(this.type);
         }
      } else {
         Map map;
         if ((features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
            if (JDKUtils.JVM_VERSION == 8 && bytes[this.offset] != -91) {
               map = new HashMap(10);
            } else {
               map = new HashMap();
            }
         } else if (JDKUtils.JVM_VERSION == 8 && bytes[this.offset] != -91) {
            map = new JSONObject(10);
         } else {
            map = new JSONObject();
         }

         int i = 0;

         while (true) {
            this.type = bytes[this.offset];
            if (this.type == -91) {
               this.offset++;
               return map;
            }

            Object name;
            if (this.isString()) {
               name = this.readFieldName();
            } else {
               name = this.readAny();
            }

            if (this.offset < bytes.length && bytes[this.offset] == -109) {
               String reference = this.readReference();
               if ("..".equals(reference)) {
                  map.put(name, map);
               } else {
                  this.addResolveTask(map, name, JSONPath.of(reference));
               }
            } else {
               byte valueType = bytes[this.offset];
               Object value;
               if (valueType >= 73 && valueType <= 126) {
                  value = this.readString();
               } else if (valueType >= -16 && valueType <= 47) {
                  this.offset++;
                  value = Integer.valueOf(valueType);
               } else if (valueType == -79) {
                  this.offset++;
                  value = Boolean.TRUE;
               } else if (valueType == -80) {
                  this.offset++;
                  value = Boolean.FALSE;
               } else if (valueType == -90) {
                  value = this.readObject();
               } else if (valueType == -66) {
                  long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset + 1L);
                  this.offset += 9;
                  value = JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value);
               } else if (valueType >= -108 && valueType <= -92) {
                  this.offset++;
                  int len;
                  if (valueType == -92) {
                     byte itemType = bytes[this.offset];
                     if (itemType >= -16 && itemType <= 47) {
                        this.offset++;
                        len = itemType;
                     } else {
                        len = this.readLength();
                     }
                  } else {
                     len = valueType - -108;
                  }

                  if (len == 0) {
                     if ((features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                        value = new ArrayList();
                     } else if (this.context.arraySupplier != null) {
                        value = this.context.arraySupplier.get();
                     } else {
                        value = new JSONArray();
                     }
                  } else {
                     List list;
                     if ((features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                        list = new ArrayList(len);
                     } else {
                        list = new JSONArray(len);
                     }

                     for (int j = 0; j < len; j++) {
                        if (this.isReference()) {
                           String reference = this.readReference();
                           if ("..".equals(reference)) {
                              list.add(list);
                           } else {
                              list.add(null);
                              this.addResolveTask(list, j, JSONPath.of(reference));
                           }
                        } else {
                           byte itemType = bytes[this.offset];
                           Object item;
                           if (itemType >= 73 && itemType <= 126) {
                              item = this.readString();
                           } else if (itemType == -90) {
                              item = this.readObject();
                           } else {
                              item = this.readAny();
                           }

                           list.add(item);
                        }
                     }

                     value = list;
                  }
               } else if (valueType >= 48 && valueType <= 63) {
                  value = (valueType - 56 << 8) + (bytes[this.offset + 1] & 255);
                  this.offset += 2;
               } else if (valueType >= 64 && valueType <= 71) {
                  int int32Value = getInt3(bytes, this.offset + 1, valueType);
                  this.offset += 3;
                  value = int32Value;
               } else if (valueType == 72) {
                  int int32Value = getInt(bytes, this.offset + 1);
                  this.offset += 5;
                  value = int32Value;
               } else {
                  value = this.readAny();
               }

               if (value != null || (features & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                  map.put(name, value);
               }
            }

            i++;
         }
      }
   }

   @Override
   public void read(Map map, long features) {
      if (this.bytes[this.offset] != -90) {
         throw new JSONException("object not support input " + this.error(this.type));
      } else {
         this.offset++;
         long contextFeatures = features | this.context.getFeatures();

         while (true) {
            byte type = this.bytes[this.offset];
            if (type == -91) {
               this.offset++;
               return;
            }

            Object name;
            if (type >= 73) {
               name = this.readFieldName();
            } else {
               name = this.readAny();
            }

            if (this.isReference()) {
               String reference = this.readReference();
               if ("..".equals(reference)) {
                  map.put(name, map);
               } else {
                  this.addResolveTask(map, name, JSONPath.of(reference));
                  map.put(name, null);
               }
            } else {
               byte valueType = this.bytes[this.offset];
               Object value;
               if (valueType >= 73 && valueType <= 126) {
                  value = this.readString();
               } else if (valueType >= -16 && valueType <= 47) {
                  this.offset++;
                  value = Integer.valueOf(valueType);
               } else if (valueType == -79) {
                  this.offset++;
                  value = Boolean.TRUE;
               } else if (valueType == -80) {
                  this.offset++;
                  value = Boolean.FALSE;
               } else if (valueType == -90) {
                  value = this.readObject();
               } else {
                  value = this.readAny();
               }

               if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                  map.put(name, value);
               }
            }
         }
      }
   }

   @Override
   public Object readAny() {
      if (this.offset >= this.bytes.length) {
         throw new JSONException("readAny overflow : " + this.offset + "/" + this.bytes.length);
      } else {
         this.type = this.bytes[this.offset++];
         switch (this.type) {
            case -112:
               int intValue = this.readInt32Value();
               return (char)intValue;
            case -111: {
               int len = this.readLength();
               byte[] binary = Arrays.copyOfRange(this.bytes, this.offset, this.offset + len);
               this.offset += len;
               return binary;
            }
            case -110:
               long typeHash = this.readTypeHashCode();
               if (this.context.autoTypeBeforeHandler != null) {
                  Class<?> filterClass = this.context.autoTypeBeforeHandler.apply(typeHash, null, this.context.features);
                  if (filterClass == null) {
                     String typeName = this.getString();
                     filterClass = this.context.autoTypeBeforeHandler.apply(typeName, null, this.context.features);
                  }

                  if (filterClass != null) {
                     ObjectReader autoTypeObjectReader = this.context.getObjectReader(filterClass);
                     return autoTypeObjectReader.readJSONBObject(this, null, null, 0L);
                  }
               }

               boolean supportAutoType = (this.context.features & JSONReader.Feature.SupportAutoType.mask) != 0L;
               if (!supportAutoType) {
                  if (this.isObject()) {
                     return this.readObject();
                  } else {
                     if (this.isArray()) {
                        return this.readArray();
                     }

                     throw new JSONException("autoType not support , offset " + this.offset + "/" + this.bytes.length);
                  }
               } else {
                  ObjectReader autoTypeObjectReader = this.context.getObjectReaderAutoType(typeHash);
                  if (autoTypeObjectReader == null) {
                     String typeName = this.getString();
                     autoTypeObjectReader = this.context.getObjectReaderAutoType(typeName, null);
                     if (autoTypeObjectReader == null) {
                        throw new JSONException("autoType not support : " + typeName + ", offset " + this.offset + "/" + this.bytes.length);
                     }
                  }

                  return autoTypeObjectReader.readJSONBObject(this, null, null, 0L);
               }
            case -90:
               Map map = null;
               boolean supportAutoType = (this.context.features & JSONReader.Feature.SupportAutoType.mask) != 0L;
               int i = 0;

               while (true) {
                  byte type = this.bytes[this.offset];
                  if (type == -91) {
                     this.offset++;
                     if (map == null) {
                        if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                           map = new HashMap();
                        } else {
                           map = new JSONObject();
                        }
                     }

                     return map;
                  }

                  Object name;
                  if (supportAutoType && i == 0 && type >= 73) {
                     long hash = this.readFieldNameHashCode();
                     if (hash == ObjectReader.HASH_TYPE) {
                        long typeHashx = this.readValueHashCode();
                        ObjectReader autoTypeObjectReader = this.context.getObjectReaderAutoType(typeHashx);
                        if (autoTypeObjectReader == null) {
                           String typeName = this.getString();
                           autoTypeObjectReader = this.context.getObjectReaderAutoType(typeName, null);
                           if (autoTypeObjectReader == null) {
                              throw new JSONException("autoType not support : " + typeName + ", offset " + this.offset + "/" + this.bytes.length);
                           }
                        }

                        this.typeRedirect = true;
                        return autoTypeObjectReader.readJSONBObject(this, null, null, 0L);
                     }

                     name = this.getFieldName();
                  } else if (type >= 73) {
                     name = this.readFieldName();
                  } else {
                     name = this.readAny();
                  }

                  if (map == null) {
                     if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                        map = new HashMap();
                     } else {
                        map = new JSONObject();
                     }
                  }

                  if (this.isReference()) {
                     String reference = this.readReference();
                     if ("..".equals(reference)) {
                        map.put(name, map);
                     } else {
                        this.addResolveTask(map, name, JSONPath.of(reference));
                        map.put(name, null);
                     }
                  } else {
                     byte valueType = this.bytes[this.offset];
                     Object value;
                     if (valueType >= 73 && valueType <= 126) {
                        value = this.readString();
                     } else if (valueType >= -16 && valueType <= 47) {
                        this.offset++;
                        value = Integer.valueOf(valueType);
                     } else if (valueType == -79) {
                        this.offset++;
                        value = Boolean.TRUE;
                     } else if (valueType == -80) {
                        this.offset++;
                        value = Boolean.FALSE;
                     } else if (valueType == -90) {
                        value = this.readObject();
                     } else {
                        value = this.readAny();
                     }

                     if (value != null || (this.context.features & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                        map.put(name, value);
                     }
                  }

                  i++;
               }
            case -89: {
               byte hour = this.bytes[this.offset++];
               byte minute = this.bytes[this.offset++];
               byte second = this.bytes[this.offset++];
               int nano = this.readInt32Value();
               return LocalTime.of(hour, minute, second, nano);
            }
            case -88: {
               int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
               byte month = this.bytes[this.offset++];
               byte dayOfMonth = this.bytes[this.offset++];
               byte hour = this.bytes[this.offset++];
               byte minute = this.bytes[this.offset++];
               byte second = this.bytes[this.offset++];
               int nano = this.readInt32Value();
               return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
            }
            case -87: {
               int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
               byte month = this.bytes[this.offset++];
               byte dayOfMonth = this.bytes[this.offset++];
               return LocalDate.of(year, month, dayOfMonth);
            }
            case -86:
               return this.readTimestampWithTimeZone();
            case -85:
               long millis = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return new Date(JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
            case -84:
               long seconds = (long)getInt(this.bytes, this.offset);
               this.offset += 4;
               return new Date(seconds * 1000L);
            case -83:
               long minutes = (long)getInt(this.bytes, this.offset);
               this.offset += 4;
               return new Date(minutes * 60L * 1000L);
            case -82: {
               long epochSeconds = this.readInt64Value();
               int nano = this.readInt32Value();
               return Instant.ofEpochSecond(epochSeconds, (long)nano);
            }
            case -81:
               return null;
            case -80:
               return false;
            case -79:
               return true;
            case -78:
               return 0.0;
            case -77:
               return 1.0;
            case -76:
               return (double)this.readInt64Value();
            case -75: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            }
            case -74:
               return (float)this.readInt32Value();
            case -73: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return Float.intBitsToFloat(int32Value);
            }
            case -72:
               return BigDecimal.valueOf(this.readInt64Value());
            case -71:
               int scale = this.readInt32Value();
               BigInteger unscaledValue = this.readBigInteger();
               BigDecimal decimal;
               if (scale == 0) {
                  decimal = new BigDecimal(unscaledValue);
               } else {
                  decimal = new BigDecimal(unscaledValue, scale);
               }

               return decimal;
            case -70:
               return BigInteger.valueOf(this.readInt64Value());
            case -69: {
               int len = this.readInt32Value();
               byte[] bytes = new byte[len];
               System.arraycopy(this.bytes, this.offset, bytes, 0, len);
               this.offset += len;
               return new BigInteger(bytes);
            }
            case -68:
               return (short)((this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255));
            case -67:
               return this.bytes[this.offset++];
            case -66: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value);
            }
            case -65: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return (long)int32Value;
            }
            case 72: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return int32Value;
            }
            case 122: {
               int strlen = this.readLength();
               if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
                  if (this.valueBytes == null) {
                     this.valueBytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
                     if (this.valueBytes == null) {
                        this.valueBytes = new byte[8192];
                     }
                  }

                  int minCapacity = strlen << 1;
                  if (minCapacity > this.valueBytes.length) {
                     this.valueBytes = new byte[minCapacity];
                  }

                  int utf16_len = IOUtils.decodeUTF8(this.bytes, this.offset, strlen, this.valueBytes);
                  if (utf16_len != -1) {
                     byte[] value = new byte[utf16_len];
                     System.arraycopy(this.valueBytes, 0, value, 0, utf16_len);
                     String strx = JDKUtils.STRING_CREATOR_JDK11.apply(value, JDKUtils.UTF16);
                     this.offset += strlen;
                     return strx;
                  }
               }

               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_8);
               this.offset += strlen;
               return str;
            }
            case 123: {
               int strlen = this.readLength();
               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16);
               this.offset += strlen;
               return str;
            }
            case 124: {
               int strlen = this.readLength();
               String str;
               if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
                  byte[] chars = new byte[strlen];
                  System.arraycopy(this.bytes, this.offset, chars, 0, strlen);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, strlen == 0 ? JDKUtils.LATIN1 : JDKUtils.UTF16);
               } else {
                  str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
               }

               this.offset += strlen;
               return str;
            }
            case 125: {
               int strlen = this.readLength();
               String str;
               if (JDKUtils.STRING_CREATOR_JDK11 != null && JDKUtils.BIG_ENDIAN) {
                  byte[] chars = new byte[strlen];
                  System.arraycopy(this.bytes, this.offset, chars, 0, strlen);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, strlen == 0 ? JDKUtils.LATIN1 : JDKUtils.UTF16);
               } else {
                  str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16BE);
               }

               this.offset += strlen;
               return str;
            }
            case 126: {
               if (GB18030 == null) {
                  GB18030 = Charset.forName("GB18030");
               }

               int strlen = this.readLength();
               String str = new String(this.bytes, this.offset, strlen, GB18030);
               this.offset += strlen;
               return str;
            }
            default:
               if (this.type >= -16 && this.type <= 47) {
                  return Integer.valueOf(this.type);
               } else if (this.type >= 48 && this.type <= 63) {
                  return (this.type - 56 << 8) + (this.bytes[this.offset++] & 255);
               } else if (this.type >= 64 && this.type <= 71) {
                  int int3 = getInt3(this.bytes, this.offset, this.type);
                  this.offset += 2;
                  return int3;
               } else if (this.type >= -40 && this.type <= -17) {
                  return -8L + (long)(this.type - -40);
               } else if (this.type >= -56 && this.type <= -41) {
                  return (long)(this.type - -48 << 8) + (long)(this.bytes[this.offset++] & 255);
               } else if (this.type >= -64 && this.type <= -57) {
                  return (long)((this.type - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255));
               } else if (this.type >= -108 && this.type <= -92) {
                  int lenx = this.type == -92 ? this.readLength() : this.type - -108;
                  if (lenx == 0) {
                     if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                        return new ArrayList();
                     } else {
                        return this.context.arraySupplier != null ? this.context.arraySupplier.get() : new JSONArray();
                     }
                  } else {
                     List list;
                     if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                        list = new ArrayList(lenx);
                     } else {
                        list = new JSONArray(lenx);
                     }

                     for (int i = 0; i < lenx; i++) {
                        if (this.isReference()) {
                           String reference = this.readReference();
                           if ("..".equals(reference)) {
                              list.add(list);
                           } else {
                              list.add(null);
                              this.addResolveTask(list, i, JSONPath.of(reference));
                           }
                        } else {
                           Object item = this.readAny();
                           list.add(item);
                        }
                     }

                     return list;
                  }
               } else if (this.type >= 73 && this.type <= 121) {
                  this.strlen = this.type == 121 ? this.readLength() : this.type - 73;
                  if (this.strlen < 0) {
                     return this.symbolTable.getName(-this.strlen);
                  } else if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                     char[] chars = new char[this.strlen];

                     for (int ix = 0; ix < this.strlen; ix++) {
                        chars[ix] = (char)(this.bytes[this.offset + ix] & 255);
                     }

                     this.offset = this.offset + this.strlen;
                     String strx = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                     if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
                        strx = strx.trim();
                     }

                     if (strx.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
                        strx = null;
                     }

                     return strx;
                  } else if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                     byte[] chars = new byte[this.strlen];
                     System.arraycopy(this.bytes, this.offset, chars, 0, this.strlen);
                     this.offset = this.offset + this.strlen;
                     String strxx = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.LATIN1);
                     if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
                        strxx = strxx.trim();
                     }

                     if (strxx.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
                        strxx = null;
                     }

                     return strxx;
                  } else {
                     String strxxx = new String(this.bytes, this.offset, this.strlen, StandardCharsets.ISO_8859_1);
                     this.offset = this.offset + this.strlen;
                     if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
                        strxxx = strxxx.trim();
                     }

                     if (strxxx.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
                        strxxx = null;
                     }

                     return strxxx;
                  }
               } else if (this.type == 127) {
                  this.strlen = this.readLength();
                  if (this.strlen >= 0) {
                     throw new JSONException("not support symbol : " + this.strlen);
                  } else {
                     return this.symbolTable.getName(-this.strlen);
                  }
               } else {
                  throw new JSONException("not support type : " + this.error(this.type));
               }
         }
      }
   }

   private ZonedDateTime readTimestampWithTimeZone() {
      byte[] bytes = this.bytes;
      int year = (bytes[this.offset++] << 8) + (bytes[this.offset++] & 255);
      byte month = bytes[this.offset++];
      byte dayOfMonth = bytes[this.offset++];
      byte hour = bytes[this.offset++];
      byte minute = bytes[this.offset++];
      byte second = bytes[this.offset++];
      int nano = this.readInt32Value();
      byte[] shanghaiZoneIdNameBytes = SHANGHAI_ZONE_ID_NAME_BYTES;
      boolean shanghai;
      if (this.offset + shanghaiZoneIdNameBytes.length < bytes.length) {
         shanghai = true;

         for (int i = 0; i < shanghaiZoneIdNameBytes.length; i++) {
            if (bytes[this.offset + i] != shanghaiZoneIdNameBytes[i]) {
               shanghai = false;
               break;
            }
         }
      } else {
         shanghai = false;
      }

      ZoneId zoneId;
      if (shanghai) {
         this.offset += shanghaiZoneIdNameBytes.length;
         zoneId = DateUtils.SHANGHAI_ZONE_ID;
      } else {
         String zoneIdStr = this.readString();
         zoneId = DateUtils.getZoneId(zoneIdStr, DateUtils.SHANGHAI_ZONE_ID);
      }

      LocalDateTime ldt = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
      return ZonedDateTime.of(ldt, zoneId);
   }

   @Override
   public byte getType() {
      return this.bytes[this.offset];
   }

   @Override
   public List readArray() {
      int entryCnt = this.startArray();
      JSONArray array = new JSONArray(entryCnt);

      for (int i = 0; i < entryCnt; i++) {
         byte valueType = this.bytes[this.offset];
         Object value;
         if (valueType >= 73 && valueType <= 126) {
            value = this.readString();
         } else if (valueType >= -16 && valueType <= 47) {
            this.offset++;
            value = Integer.valueOf(valueType);
         } else if (valueType == -79) {
            this.offset++;
            value = Boolean.TRUE;
         } else if (valueType == -80) {
            this.offset++;
            value = Boolean.FALSE;
         } else if (valueType == -90) {
            value = this.readObject();
         } else if (valueType == -66) {
            this.offset++;
            long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            value = JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value);
         } else if (valueType >= -108 && valueType <= -92) {
            this.offset++;
            int len = valueType == -92 ? this.readLength() : valueType - -108;
            if (len == 0) {
               if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                  value = new ArrayList();
               } else if (this.context.arraySupplier != null) {
                  value = this.context.arraySupplier.get();
               } else {
                  value = new JSONArray();
               }
            } else {
               List list;
               if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
                  list = new ArrayList(len);
               } else {
                  list = new JSONArray(len);
               }

               for (int j = 0; j < len; j++) {
                  if (this.isReference()) {
                     String reference = this.readReference();
                     if ("..".equals(reference)) {
                        list.add(list);
                     } else {
                        list.add(null);
                        this.addResolveTask(list, j, JSONPath.of(reference));
                     }
                  } else {
                     byte itemType = this.bytes[this.offset];
                     Object item;
                     if (itemType >= 73 && itemType <= 126) {
                        item = this.readString();
                     } else if (itemType == -90) {
                        item = this.readObject();
                     } else {
                        item = this.readAny();
                     }

                     list.add(item);
                  }
               }

               value = list;
            }
         } else if (valueType >= 48 && valueType <= 63) {
            value = (valueType - 56 << 8) + (this.bytes[this.offset + 1] & 255);
            this.offset += 2;
         } else if (valueType >= 64 && valueType <= 71) {
            int int3 = getInt3(this.bytes, this.offset + 1, valueType);
            this.offset += 3;
            value = int3;
         } else if (valueType == 72) {
            int int32Value = JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset + 1L);
            this.offset += 5;
            value = JDKUtils.BIG_ENDIAN ? int32Value : Integer.reverseBytes(int32Value);
         } else if (valueType == -109) {
            String reference = this.readReference();
            if (!"..".equals(reference)) {
               this.addResolveTask(array, i, JSONPath.of(reference));
               continue;
            }

            value = array;
         } else {
            value = this.readAny();
         }

         array.add(value);
      }

      return array;
   }

   @Override
   public List readArray(Type itemType) {
      if (this.nextIfNull()) {
         return null;
      } else if (this.bytes[this.offset] == -110) {
         Object obj = this.readAny();
         if (obj instanceof List) {
            return (List)obj;
         } else if (obj instanceof Collection) {
            return new JSONArray((Collection<?>)obj);
         } else {
            throw new JSONException("not support class " + obj.getClass());
         }
      } else {
         int entryCnt = this.startArray();
         JSONArray array = new JSONArray(entryCnt);

         for (int i = 0; i < entryCnt; i++) {
            array.add(this.read(itemType));
         }

         return array;
      }
   }

   @Override
   public List readList(Type[] types) {
      if (this.nextIfNull()) {
         return null;
      } else {
         int entryCnt = this.startArray();
         JSONArray array = new JSONArray(entryCnt);

         for (int i = 0; i < entryCnt; i++) {
            Type itemType = types[i];
            array.add(this.read(itemType));
         }

         return array;
      }
   }

   @Override
   public byte[] readHex() {
      String str = this.readString();
      byte[] bytes = new byte[str.length() / 2];

      for (int i = 0; i < bytes.length; i++) {
         char c0 = str.charAt(i * 2);
         char c1 = str.charAt(i * 2 + 1);
         int b0 = c0 - (c0 <= '9' ? 48 : 55);
         int b1 = c1 - (c1 <= '9' ? 48 : 55);
         bytes[i] = (byte)(b0 << 4 | b1);
      }

      return bytes;
   }

   @Override
   public boolean isReference() {
      return this.offset < this.bytes.length && this.bytes[this.offset] == -109;
   }

   @Override
   public String readReference() {
      if (this.bytes[this.offset] != -109) {
         return null;
      } else {
         this.offset++;
         if (this.isString()) {
            return this.readString();
         } else {
            throw new JSONException("reference not support input " + this.error(this.type));
         }
      }
   }

   @Override
   public boolean readReference(Collection list, int i) {
      if (this.bytes[this.offset] != -109) {
         return false;
      } else {
         this.offset++;
         String path = this.readString();
         if ("..".equals(path)) {
            list.add(list);
         } else {
            this.addResolveTask(list, i, JSONPath.of(path));
         }

         return true;
      }
   }

   Object readAnyObject() {
      if (this.bytes[this.offset] != -110) {
         return this.readAny();
      } else {
         JSONReader.Context context = this.context;
         this.offset++;
         long typeHash = this.readTypeHashCode();
         ObjectReader autoTypeObjectReader = null;
         JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler = context.autoTypeBeforeHandler;
         if (autoTypeBeforeHandler != null) {
            Class<?> objectClass = autoTypeBeforeHandler.apply(typeHash, Object.class, 0L);
            if (objectClass == null) {
               objectClass = autoTypeBeforeHandler.apply(this.getString(), Object.class, 0L);
            }

            if (objectClass != null) {
               autoTypeObjectReader = context.getObjectReader(objectClass);
            }
         }

         long features = context.features;
         if (autoTypeObjectReader == null) {
            if ((features & JSONReader.Feature.SupportAutoType.mask) == 0L) {
               if ((features & JSONReader.Feature.ErrorOnNotSupportAutoType.mask) == 0L) {
                  return null;
               }

               this.autoTypeError();
            }

            autoTypeObjectReader = context.provider.getObjectReader(typeHash);
         }

         if (autoTypeObjectReader != null) {
            Class objectClassx = autoTypeObjectReader.getObjectClass();
            if (objectClassx != null) {
               ClassLoader classLoader = objectClassx.getClassLoader();
               if (classLoader != null) {
                  ClassLoader tcl = Thread.currentThread().getContextClassLoader();
                  if (classLoader != tcl) {
                     autoTypeObjectReader = this.getObjectReaderContext(autoTypeObjectReader, objectClassx, tcl);
                  }
               }
            }
         }

         if (autoTypeObjectReader == null) {
            autoTypeObjectReader = context.provider.getObjectReader(this.getString(), Object.class, features);
            if (autoTypeObjectReader == null) {
               if ((features & JSONReader.Feature.ErrorOnNotSupportAutoType.mask) == 0L) {
                  return null;
               }

               this.autoTypeError();
            }
         }

         this.type = this.bytes[this.offset];
         return autoTypeObjectReader.readJSONBObject(this, Object.class, null, context.features);
      }
   }

   @Override
   public ObjectReader checkAutoType(Class expectClass, long expectClassHash, long features) {
      ObjectReader autoTypeObjectReader = null;
      if (this.bytes[this.offset] == -110) {
         this.offset++;
         JSONReader.Context context = this.context;
         long typeHash = this.readTypeHashCode();
         if (expectClassHash == typeHash) {
            ObjectReader objectReader = context.getObjectReader(expectClass);
            Class objectClass = objectReader.getObjectClass();
            if (objectClass != null && objectClass == expectClass) {
               context.provider.registerIfAbsent(typeHash, objectReader);
               return objectReader;
            }
         }

         JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler = context.autoTypeBeforeHandler;
         if (autoTypeBeforeHandler != null) {
            ObjectReader objectReader = this.checkAutoTypeWithHandler(expectClass, features, autoTypeBeforeHandler, typeHash);
            if (objectReader != null) {
               return objectReader;
            }
         }

         long features2 = context.features | features;
         if ((features2 & JSONReader.Feature.SupportAutoType.mask) == 0L) {
            if ((features2 & JSONReader.Feature.ErrorOnNotSupportAutoType.mask) == 0L) {
               return null;
            }

            this.autoTypeError();
         }

         autoTypeObjectReader = context.provider.getObjectReader(typeHash);
         if (autoTypeObjectReader != null) {
            Class objectClass = autoTypeObjectReader.getObjectClass();
            if (objectClass != null) {
               ClassLoader classLoader = objectClass.getClassLoader();
               if (classLoader != null) {
                  ClassLoader tcl = Thread.currentThread().getContextClassLoader();
                  if (classLoader != tcl) {
                     autoTypeObjectReader = this.getObjectReaderContext(autoTypeObjectReader, objectClass, tcl);
                  }
               }
            }
         }

         if (autoTypeObjectReader == null) {
            autoTypeObjectReader = context.provider.getObjectReader(this.getString(), expectClass, features2);
            if (autoTypeObjectReader == null) {
               if ((features2 & JSONReader.Feature.ErrorOnNotSupportAutoType.mask) == 0L) {
                  return null;
               }

               this.autoTypeError();
            }
         }

         this.type = this.bytes[this.offset];
      }

      return autoTypeObjectReader;
   }

   ObjectReader checkAutoTypeWithHandler(Class expectClass, long features, JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler, long typeHash) {
      Class<?> objectClass = autoTypeBeforeHandler.apply(typeHash, expectClass, features);
      if (objectClass == null) {
         objectClass = autoTypeBeforeHandler.apply(this.getString(), expectClass, features);
      }

      return objectClass != null ? this.context.getObjectReader(objectClass) : null;
   }

   void autoTypeError() {
      throw new JSONException("autoType not support : " + this.getString());
   }

   private ObjectReader getObjectReaderContext(ObjectReader autoTypeObjectReader, Class objectClass, ClassLoader contextClassLoader) {
      String typeName = this.getString();
      Class contextClass = TypeUtils.getMapping(typeName);
      if (contextClass == null) {
         try {
            contextClass = (contextClassLoader != null ? contextClassLoader : JSON.class.getClassLoader()).loadClass(typeName);
         } catch (ClassNotFoundException var7) {
         }
      }

      if (contextClass != null && !objectClass.equals(contextClass)) {
         autoTypeObjectReader = this.getObjectReader(contextClass);
      }

      return autoTypeObjectReader;
   }

   @Override
   public int startArray() {
      byte type = this.type = this.bytes[this.offset++];
      if (type == -81) {
         return -1;
      } else if (type >= -108 && type <= -93) {
         this.ch = (char)(-type);
         return type - -108;
      } else if (type == -111) {
         return this.readInt32Value();
      } else if (type != -92) {
         throw new JSONException("array not support input " + this.error(type));
      } else {
         return this.readInt32Value();
      }
   }

   public String error(byte type) {
      StringBuilder buf = new StringBuilder();
      buf.append(JSONB.typeName(type));
      if (this.isString()) {
         int mark = this.offset--;
         String str = null;

         try {
            str = this.readString();
         } catch (Throwable var6) {
         }

         if (str != null) {
            buf.append(' ');
            buf.append(str);
         }

         this.offset = mark;
      }

      buf.append(", offset ");
      buf.append(this.offset);
      buf.append('/');
      buf.append(this.bytes.length);
      return buf.toString();
   }

   @Override
   public void next() {
      this.offset++;
   }

   @Override
   public long readFieldNameHashCode() {
      byte[] bytes = this.bytes;
      byte strtype = this.strtype = bytes[this.offset++];
      boolean typeSymbol = strtype == 127;
      if (typeSymbol) {
         strtype = this.strtype = bytes[this.offset];
         if (strtype >= -16 && strtype <= 72) {
            int symbol;
            if (strtype <= 47) {
               this.offset++;
               symbol = strtype;
            } else {
               symbol = this.readInt32Value();
            }

            if (symbol < 0) {
               return this.symbolTable.getHashCode(-symbol);
            }

            if (symbol == 0) {
               this.strtype = this.symbol0StrType;
               this.strlen = this.symbol0Length;
               this.strBegin = this.symbol0Begin;
               if (this.symbol0Hash == 0L) {
                  this.symbol0Hash = this.getNameHashCode();
               }

               return this.symbol0Hash;
            }

            int index = symbol * 2;
            long strInfo = this.symbols[index + 1];
            this.strtype = (byte)((int)strInfo);
            this.strlen = (int)strInfo >> 8;
            this.strBegin = (int)(strInfo >> 32);
            long nameHashCode = this.symbols[index];
            if (nameHashCode == 0L) {
               nameHashCode = this.getNameHashCode();
               this.symbols[index] = nameHashCode;
            }

            return nameHashCode;
         }

         this.offset++;
      }

      int strlen;
      if (strtype >= 73 && strtype <= 120) {
         strlen = strtype - 73;
      } else {
         if (strtype != 121 && strtype != 122) {
            throw this.readFieldNameHashCodeError();
         }

         strlen = this.readLength();
      }

      this.strlen = strlen;
      this.strBegin = this.offset;
      long hashCode;
      if (strlen < 0) {
         hashCode = this.symbolTable.getHashCode(-strlen);
      } else {
         long nameValue = 0L;
         if (strlen <= 8 && this.offset + strlen <= bytes.length) {
            long offsetBase = (long)this.offset + BASE;
            switch (strlen) {
               case 1:
                  nameValue = (long)bytes[this.offset];
                  break;
               case 2:
                  nameValue = (long)JDKUtils.UNSAFE.getShort(bytes, offsetBase) & 65535L;
                  break;
               case 3:
                  nameValue = (long)(bytes[this.offset + 2] << 16) + ((long)JDKUtils.UNSAFE.getShort(bytes, offsetBase) & 65535L);
                  break;
               case 4:
                  nameValue = (long)JDKUtils.UNSAFE.getInt(bytes, offsetBase);
                  break;
               case 5:
                  nameValue = ((long)bytes[this.offset + 4] << 32) + ((long)JDKUtils.UNSAFE.getInt(bytes, offsetBase) & 4294967295L);
                  break;
               case 6:
                  nameValue = ((long)JDKUtils.UNSAFE.getShort(bytes, offsetBase + 4L) << 32) + ((long)JDKUtils.UNSAFE.getInt(bytes, offsetBase) & 4294967295L);
                  break;
               case 7:
                  nameValue = ((long)bytes[this.offset + 6] << 48)
                     + (((long)bytes[this.offset + 5] & 255L) << 40)
                     + (((long)bytes[this.offset + 4] & 255L) << 32)
                     + ((long)JDKUtils.UNSAFE.getInt(bytes, offsetBase) & 4294967295L);
                  break;
               default:
                  nameValue = JDKUtils.UNSAFE.getLong(bytes, offsetBase);
            }
         }

         if (nameValue != 0L) {
            this.offset += strlen;
            hashCode = nameValue;
         } else {
            hashCode = -3750763034362895579L;

            for (int i = 0; i < strlen; i++) {
               hashCode ^= (long)bytes[this.offset++];
               hashCode *= 1099511628211L;
            }
         }
      }

      if (typeSymbol) {
         int symbolx;
         if ((symbolx = bytes[this.offset]) >= -16 && symbolx <= 47) {
            this.offset++;
         } else {
            symbolx = this.readInt32Value();
         }

         if (symbolx == 0) {
            this.symbol0Begin = this.strBegin;
            this.symbol0Length = strlen;
            this.symbol0StrType = strtype;
            this.symbol0Hash = hashCode;
         } else {
            int symbolIndex = symbolx << 1;
            int minCapacity = symbolIndex + 2;
            if (this.symbols == null) {
               this.symbols = new long[Math.max(minCapacity, 32)];
            } else if (this.symbols.length < minCapacity) {
               this.symbols = Arrays.copyOf(this.symbols, minCapacity + 16);
            }

            this.symbols[symbolIndex] = hashCode;
            this.symbols[symbolIndex + 1] = ((long)this.strBegin << 32) + ((long)strlen << 8) + (long)strtype;
         }
      }

      return hashCode;
   }

   JSONException readFieldNameHashCodeError() {
      StringBuilder message = new StringBuilder().append("fieldName not support input type ").append(JSONB.typeName(this.strtype));
      if (this.strtype == -109) {
         message.append(" ").append(this.readString());
      }

      message.append(", offset ").append(this.offset);
      return new JSONException(message.toString());
   }

   @Override
   public boolean isInt() {
      int type = this.bytes[this.offset];
      return type >= -70 && type <= 72 || type == -84 || type == -83 || type == -85;
   }

   @Override
   public boolean isNull() {
      return this.bytes[this.offset] == -81;
   }

   @Override
   public Date readNullOrNewDate() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfNull() {
      if (this.bytes[this.offset] == -81) {
         this.offset++;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void readNull() {
      this.type = this.bytes[this.offset++];
      if (this.type != -81) {
         throw new JSONException("null not match, " + this.type);
      }
   }

   @Override
   public boolean readIfNull() {
      if (this.bytes[this.offset] == -81) {
         this.offset++;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public long readTypeHashCode() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte strtype = this.strtype = bytes[offset];
      if (strtype == 121) {
         int type;
         if ((type = bytes[++offset]) > 8 && type <= 63) {
            int typelen;
            if (type <= 47) {
               offset++;
               typelen = type;
            } else {
               typelen = (type - 56 << 8) + (bytes[offset + 1] & 255);
               offset += 2;
            }

            int strBegin = offset;
            long hashCode = -3750763034362895579L;

            for (int i = 0; i < typelen; i++) {
               hashCode ^= (long)bytes[offset++];
               hashCode *= 1099511628211L;
            }

            int symbol;
            if ((symbol = bytes[offset]) >= 0 && symbol <= 47) {
               offset++;
               if (symbol == 0) {
                  this.symbol0Begin = strBegin;
                  this.symbol0Length = typelen;
                  this.symbol0StrType = strtype;
                  this.symbol0Hash = hashCode;
               } else {
                  int minCapacity = symbol * 2 + 2;
                  if (this.symbols == null) {
                     this.symbols = new long[Math.max(minCapacity, 32)];
                  } else if (this.symbols.length < minCapacity) {
                     this.symbols = Arrays.copyOf(this.symbols, minCapacity + 16);
                  }

                  this.symbols[symbol * 2 + 1] = ((long)strBegin << 32) + ((long)typelen << 8) + (long)strtype;
               }

               this.strBegin = strBegin;
               this.strlen = typelen;
               this.offset = offset;
               return hashCode;
            }
         }
      }

      return this.readTypeHashCode0();
   }

   public long readTypeHashCode0() {
      byte[] bytes = this.bytes;
      byte strtype = this.strtype = bytes[this.offset];
      if (strtype == 127) {
         this.offset++;
         strtype = this.strtype = bytes[this.offset];
         if (strtype >= -16 && strtype <= 72) {
            int symbol;
            if (strtype <= 47) {
               this.offset++;
               symbol = strtype;
            } else {
               symbol = this.readInt32Value();
            }

            if (symbol < 0) {
               return this.symbolTable.getHashCode(-symbol);
            }

            if (symbol == 0) {
               this.strtype = this.symbol0StrType;
               this.strlen = this.symbol0Length;
               this.strBegin = this.symbol0Begin;
               if (this.symbol0Hash == 0L) {
                  this.symbol0Hash = this.getNameHashCode();
               }

               return this.symbol0Hash;
            }

            int index = symbol * 2;
            long strInfo = this.symbols[index + 1];
            this.strtype = (byte)((int)strInfo);
            this.strlen = (int)strInfo >> 8;
            this.strBegin = (int)(strInfo >> 32);
            long nameHashCode = this.symbols[index];
            if (nameHashCode == 0L) {
               nameHashCode = this.getNameHashCode();
               this.symbols[index] = nameHashCode;
            }

            return nameHashCode;
         }
      }

      if (strtype >= -16 && strtype <= 72) {
         int typeIndex;
         if (strtype <= 47) {
            this.offset++;
            typeIndex = strtype;
         } else if (strtype <= 63) {
            this.offset++;
            typeIndex = (strtype - 56 << 8) + (bytes[this.offset++] & 255);
         } else {
            typeIndex = this.readInt32Value();
         }

         long refTypeHash;
         if (typeIndex == 0) {
            this.strtype = this.symbol0StrType;
            this.strlen = this.symbol0Length;
            this.strBegin = this.symbol0Begin;
            if (this.symbol0Hash == 0L) {
               this.symbol0Hash = Fnv.hashCode64(this.getString());
            }

            refTypeHash = this.symbol0Hash;
         } else if (typeIndex < 0) {
            this.strlen = strtype;
            refTypeHash = this.symbolTable.getHashCode(-typeIndex);
         } else {
            refTypeHash = this.symbols[typeIndex * 2];
            if (refTypeHash == 0L) {
               long strInfo = this.symbols[typeIndex * 2 + 1];
               this.strtype = (byte)((int)strInfo);
               this.strlen = (int)strInfo >> 8;
               this.strBegin = (int)(strInfo >> 32);
               refTypeHash = Fnv.hashCode64(this.getString());
            }
         }

         if (refTypeHash == -1L) {
            throw typeRefNotFound(typeIndex);
         } else {
            return refTypeHash;
         }
      } else {
         this.offset++;
         this.strBegin = this.offset;
         if (strtype >= 73 && strtype <= 120) {
            this.strlen = strtype - 73;
         } else {
            if (strtype != 121 && strtype != 122 && strtype != 123 && strtype != 124 && strtype != 125) {
               throw this.readStringError();
            }

            byte type = bytes[this.offset];
            if (type >= -16 && type <= 47) {
               this.offset++;
               this.strlen = type;
            } else if (type >= 48 && type <= 63) {
               this.offset++;
               this.strlen = (type - 56 << 8) + (bytes[this.offset++] & 255);
            } else {
               this.strlen = this.readLength();
            }

            this.strBegin = this.offset;
         }

         long hashCode;
         if (this.strlen < 0) {
            hashCode = this.symbolTable.getHashCode(-this.strlen);
         } else if (strtype == 122) {
            hashCode = -3750763034362895579L;
            int end = this.offset + this.strlen;

            while (this.offset < end) {
               int c = bytes[this.offset];
               if (c >= 0) {
                  this.offset++;
               } else {
                  c &= 255;
                  switch (c >> 4) {
                     case 12:
                     case 13:
                        c = JSONReaderUTF8.char2_utf8(c, bytes[this.offset + 1], this.offset);
                        this.offset += 2;
                        break;
                     case 14:
                        c = JSONReaderUTF8.char2_utf8(c, bytes[this.offset + 1], bytes[this.offset + 2], this.offset);
                        this.offset += 3;
                        break;
                     default:
                        throw new JSONException("malformed input around byte " + this.offset);
                  }
               }

               hashCode ^= (long)c;
               hashCode *= 1099511628211L;
            }
         } else if (strtype != 123 && strtype != 125) {
            if (strtype == 124) {
               hashCode = -3750763034362895579L;

               for (int i = 0; i < this.strlen; i += 2) {
                  byte c0 = bytes[this.offset + i];
                  byte c1 = bytes[this.offset + i + 1];
                  char ch = (char)(c0 & 255 | (c1 & 255) << 8);
                  hashCode ^= (long)ch;
                  hashCode *= 1099511628211L;
               }
            } else {
               long nameValue = 0L;
               if (this.strlen <= 8) {
                  int i = 0;

                  for (int start = this.offset; i < this.strlen; i++) {
                     byte c = bytes[this.offset];
                     if (c < 0 || c == 0 && bytes[start] == 0) {
                        nameValue = 0L;
                        this.offset = start;
                        break;
                     }

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

                     this.offset++;
                  }
               }

               if (nameValue != 0L) {
                  hashCode = nameValue;
               } else {
                  hashCode = -3750763034362895579L;

                  for (int i = 0; i < this.strlen; i++) {
                     byte c = bytes[this.offset++];
                     hashCode ^= (long)c;
                     hashCode *= 1099511628211L;
                  }
               }
            }
         } else {
            hashCode = -3750763034362895579L;

            for (int i = 0; i < this.strlen; i += 2) {
               byte c0 = bytes[this.offset + i];
               byte c1 = bytes[this.offset + i + 1];
               char ch = (char)(c1 & 255 | (c0 & 255) << 8);
               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
            }
         }

         int symbolx;
         if ((this.type = bytes[this.offset]) >= -16 && this.type <= 47) {
            symbolx = this.type;
            this.offset++;
         } else {
            symbolx = this.readInt32Value();
         }

         if (symbolx == 0) {
            this.symbol0Begin = this.strBegin;
            this.symbol0Length = this.strlen;
            this.symbol0StrType = strtype;
            this.symbol0Hash = hashCode;
         } else {
            int minCapacity = symbolx * 2 + 2;
            if (this.symbols == null) {
               this.symbols = new long[Math.max(minCapacity, 32)];
            } else if (this.symbols.length < minCapacity) {
               this.symbols = Arrays.copyOf(this.symbols, minCapacity + 16);
            }

            long strInfo = ((long)this.strBegin << 32) + ((long)this.strlen << 8) + (long)strtype;
            this.symbols[symbolx * 2 + 1] = strInfo;
         }

         return hashCode;
      }
   }

   @Override
   public long readValueHashCode() {
      byte[] bytes = this.bytes;
      byte strtype = this.strtype = bytes[this.offset++];
      this.strBegin = this.offset;
      if (strtype >= 73 && strtype <= 120) {
         this.strlen = strtype - 73;
      } else if (strtype != 121 && strtype != 122 && strtype != 123 && strtype != 124 && strtype != 125) {
         if (strtype != 127) {
            throw this.readStringError();
         }

         this.strlen = this.readLength();
         this.strBegin = this.offset;
      } else {
         this.strlen = this.readLength();
         this.strBegin = this.offset;
      }

      long hashCode;
      if (this.strlen < 0) {
         hashCode = this.symbolTable.getHashCode(-this.strlen);
      } else if (strtype == 122) {
         hashCode = -3750763034362895579L;
         int end = this.offset + this.strlen;

         while (this.offset < end) {
            int c = bytes[this.offset];
            if (c >= 0) {
               this.offset++;
            } else {
               c &= 255;
               switch (c >> 4) {
                  case 12:
                  case 13:
                     c = JSONReaderUTF8.char2_utf8(c, bytes[this.offset + 1], this.offset);
                     this.offset += 2;
                     break;
                  case 14:
                     c = JSONReaderUTF8.char2_utf8(c, bytes[this.offset + 1], bytes[this.offset + 2], this.offset);
                     this.offset += 3;
                     break;
                  default:
                     throw new JSONException("malformed input around byte " + this.offset);
               }
            }

            hashCode ^= (long)c;
            hashCode *= 1099511628211L;
         }
      } else if (strtype == 123) {
         int offset = this.offset;
         hashCode = -3750763034362895579L;
         if (bytes[offset] == -2 && bytes[offset + 1] == -1) {
            if (this.strlen <= 16) {
               long nameValue = 0L;

               for (int i = 2; i < this.strlen; i += 2) {
                  byte c0 = bytes[offset + i];
                  byte c1 = bytes[offset + i + 1];
                  char ch = (char)(c1 & 255 | (c0 & 255) << 8);
                  if (ch > 127 || i == 0 && ch == 0) {
                     nameValue = 0L;
                     break;
                  }

                  byte c = (byte)ch;
                  switch (i - 2 >> 1) {
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
               }

               if (nameValue != 0L) {
                  return nameValue;
               }
            }

            for (int i = 2; i < this.strlen; i += 2) {
               byte c0 = bytes[offset + i];
               byte c1 = bytes[offset + i + 1];
               char ch = (char)(c1 & 255 | (c0 & 255) << 8);
               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
            }
         } else if (bytes[offset] == -1 && bytes[offset + 1] == -2) {
            for (int i = 2; i < this.strlen; i += 2) {
               byte c1 = bytes[offset + i];
               byte c0 = bytes[offset + i + 1];
               char ch = (char)(c1 & 255 | (c0 & 255) << 8);
               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
            }
         } else {
            for (int i = 0; i < this.strlen; i += 2) {
               byte c0 = bytes[offset + i];
               byte c1 = bytes[offset + i + 1];
               char ch = (char)(c0 & 255 | (c1 & 255) << 8);
               hashCode ^= (long)ch;
               hashCode *= 1099511628211L;
            }
         }
      } else if (strtype == 125) {
         int offset = this.offset;
         if (this.strlen <= 16) {
            long nameValue = 0L;

            for (int i = 0; i < this.strlen; i += 2) {
               byte c0 = bytes[offset + i];
               byte c1 = bytes[offset + i + 1];
               char ch = (char)(c1 & 255 | (c0 & 255) << 8);
               if (ch > 127 || i == 0 && ch == 0) {
                  nameValue = 0L;
                  break;
               }

               byte c = (byte)ch;
               switch (i >> 1) {
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
            }

            if (nameValue != 0L) {
               return nameValue;
            }
         }

         hashCode = -3750763034362895579L;

         for (int i = 0; i < this.strlen; i += 2) {
            byte c0 = bytes[offset + i];
            byte c1 = bytes[offset + i + 1];
            char ch = (char)(c1 & 255 | (c0 & 255) << 8);
            hashCode ^= (long)ch;
            hashCode *= 1099511628211L;
         }
      } else if (strtype == 124) {
         int offset = this.offset;
         if (this.strlen <= 16) {
            long nameValue = 0L;

            for (int i = 0; i < this.strlen; i += 2) {
               byte c0 = bytes[offset + i];
               byte c1 = bytes[offset + i + 1];
               char ch = (char)(c0 & 255 | (c1 & 255) << 8);
               if (ch > 127 || i == 0 && ch == 0) {
                  nameValue = 0L;
                  break;
               }

               byte c = (byte)ch;
               switch (i >> 1) {
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
            }

            if (nameValue != 0L) {
               return nameValue;
            }
         }

         hashCode = -3750763034362895579L;

         for (int i = 0; i < this.strlen; i += 2) {
            byte c0 = bytes[offset + i];
            byte c1 = bytes[offset + i + 1];
            char ch = (char)(c0 & 255 | (c1 & 255) << 8);
            hashCode ^= (long)ch;
            hashCode *= 1099511628211L;
         }
      } else {
         if (this.strlen <= 8) {
            long nameValue = 0L;
            int i = 0;

            for (int start = this.offset; i < this.strlen; i++) {
               byte c = bytes[this.offset];
               if (c < 0 || c == 0 && bytes[start] == 0) {
                  nameValue = 0L;
                  this.offset = start;
                  break;
               }

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

               this.offset++;
            }

            if (nameValue != 0L) {
               return nameValue;
            }
         }

         hashCode = -3750763034362895579L;

         for (int i = 0; i < this.strlen; i++) {
            byte c = bytes[this.offset++];
            hashCode ^= (long)c;
            hashCode *= 1099511628211L;
         }
      }

      return hashCode;
   }

   protected long getNameHashCode() {
      int offset = this.strBegin;
      long nameValue = 0L;

      for (int i = 0; i < this.strlen; offset++) {
         byte c = this.bytes[offset];
         if (c < 0 || i >= 8 || i == 0 && this.bytes[this.strBegin] == 0) {
            offset = this.strBegin;
            nameValue = 0L;
            break;
         }

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

         i++;
      }

      if (nameValue != 0L) {
         return nameValue;
      } else {
         long hashCode = -3750763034362895579L;

         for (int i = 0; i < this.strlen; i++) {
            byte c = this.bytes[offset++];
            hashCode ^= (long)c;
            hashCode *= 1099511628211L;
         }

         return hashCode;
      }
   }

   @Override
   public long getNameHashCodeLCase() {
      int offset = this.strBegin;
      long nameValue = 0L;

      for (int i = 0; i < this.strlen; offset++) {
         byte c = this.bytes[offset];
         if (c < 0 || i >= 8 || i == 0 && this.bytes[this.strBegin] == 0) {
            offset = this.strBegin;
            nameValue = 0L;
            break;
         }

         if (c == 95 || c == 45 || c == 32) {
            byte c1 = this.bytes[offset + 1];
            if (c1 != c) {
               continue;
            }
         }

         if (c >= 65 && c <= 90) {
            c = (byte)(c + 32);
         }

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

         i++;
      }

      if (nameValue != 0L) {
         return nameValue;
      } else {
         long hashCode = -3750763034362895579L;

         for (int i = 0; i < this.strlen; i++) {
            byte cx = this.bytes[offset++];
            if (cx >= 65 && cx <= 90) {
               cx = (byte)(cx + 32);
            }

            if (cx != 95 && cx != 45 && cx != 32) {
               hashCode ^= (long)cx;
               hashCode *= 1099511628211L;
            }
         }

         return hashCode;
      }
   }

   @Override
   public void skipValue() {
      byte type = this.bytes[this.offset++];
      switch (type) {
         case -111:
            int byteslen = this.readInt32Value();
            this.offset += byteslen;
            return;
         case -110:
            this.readTypeHashCode();
            this.skipValue();
            return;
         case -109:
            if (this.isString()) {
               this.skipName();
               return;
            }

            throw notSupportType(type);
         case -108:
         case -81:
         case -80:
         case -79:
         case -78:
         case -77:
         case 73:
            return;
         case -90:
            while (true) {
               byte b = this.bytes[this.offset];
               if (b == -91) {
                  this.offset++;
                  return;
               }

               int size = FIXED_TYPE_SIZE[b & 255];
               if (size > 0) {
                  this.offset += size;
               } else if (size == -1) {
                  this.offset++;
                  int len = this.readInt32Value();
                  this.offset += len;
               } else {
                  this.skipName();
               }

               b = this.bytes[this.offset];
               int var11 = FIXED_TYPE_SIZE[b & 255];
               if (var11 > 0) {
                  this.offset += var11;
               } else if (var11 == -1) {
                  this.offset++;
                  int len = this.readInt32Value();
                  this.offset += len;
               } else {
                  this.skipValue();
               }
            }
         case -89:
            this.offset += 3;
            this.readInt32Value();
            return;
         case -88:
            this.offset += 7;
            this.readInt32Value();
            return;
         case -87:
         case -84:
         case -83:
         case -73:
         case -65:
         case 72:
            this.offset += 4;
            return;
         case -86:
            this.offset += 7;
            this.readInt32Value();
            this.readString();
            return;
         case -85:
         case -75:
         case -66:
            this.offset += 8;
            return;
         case -76:
         case -72:
            this.readInt64Value();
            return;
         case -74:
            this.readInt32Value();
            return;
         case -71:
            this.readInt32Value();
            this.readBigInteger();
            return;
         case -68:
            this.offset += 2;
            return;
         case -67:
            this.offset++;
            return;
         case 121:
         case 122:
         case 123:
         case 124:
         case 125:
            int strlen = this.readInt32Value();
            this.offset += strlen;
            return;
         default:
            if (type < -16 || type > 47) {
               if (type < -40 || type > -17) {
                  if (type >= 48 && type <= 63) {
                     this.offset++;
                  } else if (type >= 64 && type <= 71) {
                     this.offset += 2;
                  } else if (type >= 73 && type <= 120) {
                     this.offset += type - 73;
                  } else if (type >= -56 && type <= -41) {
                     this.offset++;
                  } else if (type >= -64 && type <= -57) {
                     this.offset += 2;
                  } else if (type >= -108 && type <= -92) {
                     int itemCnt;
                     if (type == -92) {
                        itemCnt = this.readInt32Value();
                     } else {
                        itemCnt = type - -108;
                     }

                     for (int i = 0; i < itemCnt; i++) {
                        int sizex = FIXED_TYPE_SIZE[this.bytes[this.offset] & 255];
                        if (sizex > 0) {
                           this.offset += sizex;
                        } else if (sizex == -1) {
                           this.offset++;
                           int len = this.readInt32Value();
                           this.offset += len;
                        } else {
                           this.skipValue();
                        }
                     }
                  } else {
                     throw notSupportType(type);
                  }
               }
            }
      }
   }

   @Override
   public boolean skipName() {
      byte strtype = this.strtype = this.bytes[this.offset++];
      if (strtype >= 73 && strtype <= 120) {
         this.offset += strtype - 73;
         return true;
      } else if (strtype == 121 || strtype == 122 || strtype == 123 || strtype == 124 || strtype == 125) {
         this.strlen = this.readLength();
         this.offset = this.offset + this.strlen;
         return true;
      } else if (strtype == 127) {
         int type = this.bytes[this.offset];
         if (type >= -16 && type <= 72) {
            this.readInt32Value();
            return true;
         } else {
            this.readString();
            this.readInt32Value();
            return true;
         }
      } else {
         throw notSupportType(strtype);
      }
   }

   private static JSONException notSupportType(byte type) {
      return new JSONException("name not support input : " + JSONB.typeName(type));
   }

   private JSONException notSupportString() {
      throw new JSONException("readString not support type " + JSONB.typeName(this.strtype) + ", offset " + this.offset + "/" + this.bytes.length);
   }

   private JSONException readInt32ValueError(byte type) {
      throw new JSONException("readInt32Value not support " + JSONB.typeName(type) + ", offset " + this.offset + "/" + this.bytes.length);
   }

   private JSONException readInt64ValueError(byte type) {
      throw new JSONException("readInt64Value not support " + JSONB.typeName(type) + ", offset " + this.offset + "/" + this.bytes.length);
   }

   private JSONException readStringError() {
      throw new JSONException("string value not support input " + JSONB.typeName(this.type) + " offset " + this.offset + "/" + this.bytes.length);
   }

   static JSONException typeRefNotFound(int typeIndex) {
      throw new JSONException("type ref not found : " + typeIndex);
   }

   @Override
   public String readFieldName() {
      byte[] bytes = this.bytes;
      byte strtype = this.strtype = bytes[this.offset++];
      if (strtype == -81) {
         return null;
      } else {
         boolean typeSymbol = strtype == 127;
         if (typeSymbol) {
            strtype = this.strtype = bytes[this.offset];
            if (strtype >= -16 && strtype <= 72) {
               int symbol = this.readInt32Value();
               if (symbol < 0) {
                  return this.symbolTable.getName(-symbol);
               }

               if (symbol == 0) {
                  this.strtype = this.symbol0StrType;
                  this.strlen = this.symbol0Length;
                  this.strBegin = this.symbol0Begin;
                  return this.getFieldName();
               }

               int index = symbol * 2 + 1;
               long strInfo = this.symbols[index];
               this.strtype = (byte)((int)strInfo);
               this.strlen = (int)strInfo >> 8;
               this.strBegin = (int)(strInfo >> 32);
               return this.getString();
            }

            this.offset++;
         }

         this.strBegin = this.offset;
         Charset charset = null;
         String str = null;
         if (strtype == 74) {
            str = TypeUtils.toString((char)(bytes[this.offset] & 255));
            this.strlen = 1;
            this.offset++;
         } else if (strtype == 75) {
            str = TypeUtils.toString((char)(bytes[this.offset] & 255), (char)(bytes[this.offset + 1] & 255));
            this.strlen = 2;
            this.offset += 2;
         } else if (strtype >= 73 && strtype <= 121) {
            long nameValue0 = -1L;
            long nameValue1 = -1L;
            if (strtype == 121) {
               this.strlen = this.readLength();
               this.strBegin = this.offset;
            } else {
               int offset = this.offset;
               this.strlen = strtype - 73;
               if (offset + this.strlen > bytes.length) {
                  throw new JSONException("illegal jsonb data");
               }

               switch (this.strlen) {
                  case 3:
                     nameValue0 = (long)(bytes[offset + 2] << 16) + (((long)bytes[offset + 1] & 255L) << 8) + ((long)bytes[offset] & 255L);
                     break;
                  case 4:
                     nameValue0 = (long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset);
                     break;
                  case 5:
                     nameValue0 = ((long)bytes[offset + 4] << 32) + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     break;
                  case 6:
                     nameValue0 = ((long)bytes[offset + 5] << 40)
                        + (((long)bytes[offset + 4] & 255L) << 32)
                        + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     break;
                  case 7:
                     nameValue0 = ((long)bytes[offset + 6] << 48)
                        + (((long)bytes[offset + 5] & 255L) << 40)
                        + (((long)bytes[offset + 4] & 255L) << 32)
                        + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     break;
                  case 8:
                     nameValue0 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset);
                     break;
                  case 9:
                     nameValue0 = (long)bytes[offset];
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 1L);
                     break;
                  case 10:
                     nameValue0 = (long)JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 2L);
                     break;
                  case 11:
                     nameValue0 = (long)(bytes[offset] << 16) + (((long)bytes[offset + 1] & 255L) << 8) + ((long)bytes[offset + 2] & 255L);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 3L);
                     break;
                  case 12:
                     nameValue0 = (long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 4L);
                     break;
                  case 13:
                     nameValue0 = ((long)bytes[offset + 4] << 32) + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 5L);
                     break;
                  case 14:
                     nameValue0 = ((long)bytes[offset + 5] << 40)
                        + (((long)bytes[offset + 4] & 255L) << 32)
                        + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 6L);
                     break;
                  case 15:
                     nameValue0 = ((long)bytes[offset + 6] << 48)
                        + (((long)bytes[offset + 5] & 255L) << 40)
                        + (((long)bytes[offset + 4] & 255L) << 32)
                        + ((long)JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset) & 4294967295L);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 7L);
                     break;
                  case 16:
                     nameValue0 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset);
                     nameValue1 = JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset + 8L);
               }
            }

            int strlen = this.strlen;
            if (bytes[this.offset + strlen - 1] > 0 && nameValue0 != -1L) {
               if (nameValue1 != -1L) {
                  long nameValue01 = nameValue0 ^ nameValue1;
                  int indexMask = (int)(nameValue01 ^ nameValue01 >>> 32) & JSONFactory.NAME_CACHE2.length - 1;
                  JSONFactory.NameCacheEntry2 entry = JSONFactory.NAME_CACHE2[indexMask];
                  if (entry != null) {
                     if (entry.value0 == nameValue0 && entry.value1 == nameValue1) {
                        this.offset += strlen;
                        str = entry.name;
                     }
                  } else {
                     String name;
                     if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                        char[] chars = new char[strlen];

                        for (int i = 0; i < strlen; i++) {
                           chars[i] = (char)(bytes[this.offset + i] & 255);
                        }

                        name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                     } else {
                        name = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
                     }

                     JSONFactory.NAME_CACHE2[indexMask] = new JSONFactory.NameCacheEntry2(name, nameValue0, nameValue1);
                     this.offset += strlen;
                     str = name;
                  }
               } else {
                  int indexMask = (int)(nameValue0 ^ nameValue0 >>> 32) & JSONFactory.NAME_CACHE.length - 1;
                  JSONFactory.NameCacheEntry entry = JSONFactory.NAME_CACHE[indexMask];
                  if (entry != null) {
                     if (entry.value == nameValue0) {
                        this.offset += strlen;
                        str = entry.name;
                     }
                  } else {
                     String name;
                     if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                        char[] chars = new char[strlen];

                        for (int i = 0; i < strlen; i++) {
                           chars[i] = (char)(bytes[this.offset + i] & 255);
                        }

                        name = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                     } else {
                        name = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
                     }

                     JSONFactory.NAME_CACHE[indexMask] = new JSONFactory.NameCacheEntry(name, nameValue0);
                     this.offset += strlen;
                     str = name;
                  }
               }
            }

            if (str == null) {
               if (strlen >= 0) {
                  if (JDKUtils.STRING_CREATOR_JDK8 == null) {
                     if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                        byte[] chars = new byte[strlen];
                        System.arraycopy(bytes, this.offset, chars, 0, strlen);
                        str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.LATIN1);
                        this.offset += strlen;
                     }
                  } else {
                     char[] chars = new char[strlen];

                     for (int i = 0; i < strlen; i++) {
                        chars[i] = (char)(bytes[this.offset + i] & 255);
                     }

                     this.offset += strlen;
                     str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
                  }
               }

               charset = StandardCharsets.ISO_8859_1;
            }
         } else if (strtype == 122) {
            this.strlen = this.readLength();
            this.strBegin = this.offset;
            if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
               if (this.valueBytes == null) {
                  this.valueBytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
                  if (this.valueBytes == null) {
                     this.valueBytes = new byte[8192];
                  }
               }

               int minCapacity = this.strlen << 1;
               if (minCapacity > this.valueBytes.length) {
                  this.valueBytes = new byte[minCapacity];
               }

               int utf16_len = IOUtils.decodeUTF8(bytes, this.offset, this.strlen, this.valueBytes);
               if (utf16_len != -1) {
                  byte[] value = new byte[utf16_len];
                  System.arraycopy(this.valueBytes, 0, value, 0, utf16_len);
                  str = JDKUtils.STRING_CREATOR_JDK11.apply(value, JDKUtils.UTF16);
                  this.offset = this.offset + this.strlen;
               }
            }

            charset = StandardCharsets.UTF_8;
         } else if (strtype == 123) {
            this.strlen = this.readLength();
            this.strBegin = this.offset;
            charset = StandardCharsets.UTF_16;
         } else if (strtype == 124) {
            this.strlen = this.readLength();
            this.strBegin = this.offset;
            if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
               byte[] chars = new byte[this.strlen];
               System.arraycopy(bytes, this.offset, chars, 0, this.strlen);
               str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
               this.offset = this.offset + this.strlen;
            }

            charset = StandardCharsets.UTF_16LE;
         } else if (strtype == 125) {
            this.strlen = this.readLength();
            this.strBegin = this.offset;
            if (JDKUtils.STRING_CREATOR_JDK11 != null && JDKUtils.BIG_ENDIAN) {
               byte[] chars = new byte[this.strlen];
               System.arraycopy(bytes, this.offset, chars, 0, this.strlen);
               str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
               this.offset = this.offset + this.strlen;
            }

            charset = StandardCharsets.UTF_16BE;
         } else if (strtype == 126) {
            this.strlen = this.readLength();
            if (GB18030 == null) {
               GB18030 = Charset.forName("GB18030");
            }

            charset = GB18030;
         }

         if (this.strlen < 0) {
            str = this.symbolTable.getName(-this.strlen);
         }

         if (str == null) {
            str = new String(bytes, this.offset, this.strlen, charset);
            this.offset = this.offset + this.strlen;
         }

         if (typeSymbol) {
            int symbolx = this.readInt32Value();
            if (symbolx == 0) {
               this.symbol0Begin = this.strBegin;
               this.symbol0Length = this.strlen;
               this.symbol0StrType = strtype;
            } else {
               int minCapacityx = symbolx * 2 + 2;
               if (this.symbols == null) {
                  this.symbols = new long[Math.max(minCapacityx, 32)];
               } else if (this.symbols.length < minCapacityx) {
                  this.symbols = Arrays.copyOf(this.symbols, this.symbols.length + 16);
               }

               long strInfo = ((long)this.strBegin << 32) + ((long)this.strlen << 8) + (long)strtype;
               this.symbols[symbolx * 2 + 1] = strInfo;
            }
         }

         return str;
      }
   }

   @Override
   public String getFieldName() {
      return this.getString();
   }

   @Override
   public String readString() {
      byte[] bytes = this.bytes;
      byte strtype = this.strtype = bytes[this.offset++];
      if (strtype == -81) {
         return null;
      } else {
         this.strBegin = this.offset;
         String str = null;
         if (strtype >= 73 && strtype <= 121) {
            int strlen;
            if (strtype != 121) {
               strlen = strtype - 73;
            } else {
               byte strType = bytes[this.offset];
               if (strType >= -16 && strType <= 47) {
                  this.offset++;
                  strlen = strType;
               } else {
                  strlen = this.readLength();
               }

               this.strBegin = this.offset;
            }

            this.strlen = strlen;
            if (strlen >= 0) {
               if (JDKUtils.STRING_CREATOR_JDK8 == null) {
                  if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                     byte[] chars = new byte[strlen];
                     System.arraycopy(bytes, this.offset, chars, 0, strlen);
                     str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.LATIN1);
                     this.offset += strlen;
                  }
               } else {
                  char[] chars = new char[strlen];

                  for (int i = 0; i < strlen; i++) {
                     chars[i] = (char)(bytes[this.offset + i] & 255);
                  }

                  this.offset += strlen;
                  str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
               }
            }

            if (str != null) {
               if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
                  str = str.trim();
               }

               if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
                  str = null;
               }

               return str;
            }
         }

         return this.readStringNonAscii();
      }
   }

   private String readStringNonAscii() {
      String str = null;
      int strtype = this.strtype;
      Charset charset;
      if (strtype >= 73 && strtype <= 121) {
         charset = StandardCharsets.ISO_8859_1;
      } else if (strtype == 122) {
         str = this.readStringUTF8();
         charset = StandardCharsets.UTF_8;
      } else if (strtype == 123) {
         this.strlen = this.readLength();
         this.strBegin = this.offset;
         charset = StandardCharsets.UTF_16;
      } else if (strtype == 124) {
         str = this.readUTF16LE();
         charset = StandardCharsets.UTF_16LE;
      } else if (strtype == 125) {
         str = this.readUTF16BE();
         if (str != null) {
            return str;
         }

         charset = StandardCharsets.UTF_16BE;
      } else {
         if (strtype != 126) {
            return this.readStringTypeNotMatch();
         }

         this.readGB18030();
         charset = GB18030;
      }

      if (str != null) {
         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         return str;
      } else {
         return this.readString(charset);
      }
   }

   private String readString(Charset charset) {
      if (this.strlen < 0) {
         return this.symbolTable.getName(-this.strlen);
      } else {
         char[] chars = null;
         if (JDKUtils.JVM_VERSION == 8 && this.strtype == 122 && this.strlen < 8192) {
            int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
            JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
            chars = JSONFactory.CHARS_UPDATER.getAndSet(cacheItem, null);
            if (chars == null) {
               chars = new char[8192];
            }
         }

         String str;
         if (chars != null) {
            int len = IOUtils.decodeUTF8(this.bytes, this.offset, this.strlen, chars);
            str = new String(chars, 0, len);
            if (chars.length < 4194304) {
               JSONFactory.CHARS_UPDATER.lazySet(this.cacheItem, chars);
            }
         } else {
            str = new String(this.bytes, this.offset, this.strlen, charset);
         }

         this.offset = this.offset + this.strlen;
         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         return str;
      }
   }

   private void readGB18030() {
      this.strlen = this.readLength();
      this.strBegin = this.offset;
      if (GB18030 == null) {
         GB18030 = Charset.forName("GB18030");
      }
   }

   private String readUTF16BE() {
      this.strlen = this.readLength();
      this.strBegin = this.offset;
      if (JDKUtils.STRING_CREATOR_JDK11 != null && JDKUtils.BIG_ENDIAN) {
         byte[] chars = new byte[this.strlen];
         System.arraycopy(this.bytes, this.offset, chars, 0, this.strlen);
         String str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
         this.offset = this.offset + this.strlen;
         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         return str;
      } else {
         return null;
      }
   }

   private String readUTF16LE() {
      byte strType = this.bytes[this.offset];
      if (strType >= -16 && strType <= 47) {
         this.offset++;
         this.strlen = strType;
      } else if (strType >= 48 && strType <= 63) {
         this.offset++;
         this.strlen = (strType - 56 << 8) + (this.bytes[this.offset++] & 255);
      } else {
         this.strlen = this.readLength();
      }

      this.strBegin = this.offset;
      if (this.strlen == 0) {
         return "";
      } else if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
         byte[] chars = new byte[this.strlen];
         System.arraycopy(this.bytes, this.offset, chars, 0, this.strlen);
         String str = JDKUtils.STRING_CREATOR_JDK11.apply(chars, JDKUtils.UTF16);
         this.offset = this.offset + this.strlen;
         if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
            str = str.trim();
         }

         if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
            str = null;
         }

         return str;
      } else {
         return null;
      }
   }

   private String readStringUTF8() {
      byte strType = this.bytes[this.offset];
      if (strType >= -16 && strType <= 47) {
         this.offset++;
         this.strlen = strType;
      } else if (strType >= 48 && strType <= 63) {
         this.offset++;
         this.strlen = (strType - 56 << 8) + (this.bytes[this.offset++] & 255);
      } else {
         this.strlen = this.readLength();
      }

      this.strBegin = this.offset;
      if (JDKUtils.STRING_CREATOR_JDK11 != null && !JDKUtils.BIG_ENDIAN) {
         if (this.valueBytes == null) {
            this.valueBytes = JSONFactory.BYTES_UPDATER.getAndSet(this.cacheItem, null);
            if (this.valueBytes == null) {
               this.valueBytes = new byte[8192];
            }
         }

         int minCapacity = this.strlen << 1;
         if (minCapacity > this.valueBytes.length) {
            this.valueBytes = new byte[minCapacity];
         }

         int utf16_len = IOUtils.decodeUTF8(this.bytes, this.offset, this.strlen, this.valueBytes);
         if (utf16_len != -1) {
            byte[] value = new byte[utf16_len];
            System.arraycopy(this.valueBytes, 0, value, 0, utf16_len);
            String str = JDKUtils.STRING_CREATOR_JDK11.apply(value, JDKUtils.UTF16);
            this.offset = this.offset + this.strlen;
            if ((this.context.features & JSONReader.Feature.TrimString.mask) != 0L) {
               str = str.trim();
            }

            if (str.isEmpty() && (this.context.features & JSONReader.Feature.EmptyStringAsNull.mask) != 0L) {
               str = null;
            }

            return str;
         }
      }

      return null;
   }

   private String readStringTypeNotMatch() {
      if (this.strtype >= -16 && this.strtype <= 47) {
         return Byte.toString(this.strtype);
      } else if (this.strtype >= 48 && this.strtype <= 63) {
         int intValue = (this.strtype - 56 << 8) + (this.bytes[this.offset++] & 255);
         return Integer.toString(intValue);
      } else if (this.strtype >= 64 && this.strtype <= 71) {
         int int3 = getInt3(this.bytes, this.offset, this.strtype);
         this.offset += 2;
         return Integer.toString(int3);
      } else if (this.strtype >= -40 && this.strtype <= -17) {
         int intValue = -8 + (this.strtype - -40);
         return Integer.toString(intValue);
      } else if (this.strtype >= -56 && this.strtype <= -41) {
         int intValue = (this.strtype - -48 << 8) + (this.bytes[this.offset++] & 255);
         return Integer.toString(intValue);
      } else if (this.strtype >= -64 && this.strtype <= -57) {
         int intValue = (this.strtype - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255);
         return Integer.toString(intValue);
      } else {
         switch (this.strtype) {
            case -110:
               this.offset--;
               Object typedAny = this.readAny();
               return typedAny == null ? null : JSON.toJSONString(typedAny, JSONWriter.Feature.WriteThrowableClassName);
            case -85: {
               long millis = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               Date date = new Date(JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
               return DateUtils.toString(date);
            }
            case -84: {
               int seconds = JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               long millis = (long)(JDKUtils.BIG_ENDIAN ? seconds : Integer.reverseBytes(seconds)) * 1000L;
               Date date = new Date(millis);
               return DateUtils.toString(date);
            }
            case -83: {
               int minutes = JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               long millis = (long)(JDKUtils.BIG_ENDIAN ? minutes : Integer.reverseBytes(minutes)) * 60000L;
               Date date = new Date(millis);
               return DateUtils.toString(date);
            }
            case -81:
               return null;
            case -78:
               return "0.0";
            case -77:
               return "1.0";
            case -76: {
               double doubleValue = (double)this.readInt64Value();
               return Double.toString(doubleValue);
            }
            case -75: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               double doubleValue = Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
               return Double.toString(doubleValue);
            }
            case -74:
               return Float.toString((float)this.readInt32Value());
            case -73: {
               int int32Value = JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               float floatValue = Float.intBitsToFloat(JDKUtils.BIG_ENDIAN ? int32Value : Integer.reverseBytes(int32Value));
               return Float.toString(floatValue);
            }
            case -72:
            case -70:
               return Long.toString(this.readInt64Value());
            case -71:
               int scale = this.readInt32Value();
               BigInteger unscaledValue = this.readBigInteger();
               BigDecimal decimal;
               if (scale == 0) {
                  decimal = new BigDecimal(unscaledValue);
               } else {
                  decimal = new BigDecimal(unscaledValue, scale);
               }

               return decimal.toString();
            case -69:
               int len = this.readInt32Value();
               byte[] bytes = new byte[len];
               System.arraycopy(this.bytes, this.offset, bytes, 0, len);
               this.offset += len;
               return new BigInteger(bytes).toString();
            case -66: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return Long.toString(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            }
            case -65:
            case 72: {
               int int32Value = JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               return Long.toString(JDKUtils.BIG_ENDIAN ? (long)int32Value : (long)Integer.reverseBytes(int32Value));
            }
            default:
               throw this.notSupportString();
         }
      }
   }

   @Override
   public String[] readStringArray() {
      if (this.nextIfMatch((byte)-110)) {
         long typeHash = this.readTypeHashCode();
         if (typeHash != ObjectReaderImplStringArray.HASH_TYPE) {
            throw new JSONException(this.info("not support type " + this.getString()));
         }
      }

      int entryCnt = this.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         String[] array = new String[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = this.readString();
         }

         return array;
      }
   }

   @Override
   public char readCharValue() {
      byte type = this.bytes[this.offset];
      if (type == -112) {
         this.offset++;
         return (char)this.readInt32Value();
      } else if (type == 73) {
         this.offset++;
         return '\u0000';
      } else if (type > 73 && type < 120) {
         this.offset++;
         return (char)(this.bytes[this.offset++] & 255);
      } else {
         String str = this.readString();
         return str != null && !str.isEmpty() ? str.charAt(0) : '\u0000';
      }
   }

   @Override
   public int[] readInt32ValueArray() {
      if (this.nextIfMatch((byte)-110)) {
         long typeHash = this.readTypeHashCode();
         if (typeHash != ObjectReaderImplInt64ValueArray.HASH_TYPE
            && typeHash != ObjectReaderImplInt64Array.HASH_TYPE
            && typeHash != ObjectReaderImplInt32Array.HASH_TYPE
            && typeHash != ObjectReaderImplInt32ValueArray.HASH_TYPE) {
            throw new JSONException(this.info("not support " + this.getString()));
         }
      }

      int entryCnt = this.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         int[] array = new int[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = this.readInt32Value();
         }

         return array;
      }
   }

   @Override
   public long[] readInt64ValueArray() {
      if (this.nextIfMatch((byte)-110)) {
         long typeHash = this.readTypeHashCode();
         if (typeHash != ObjectReaderImplInt64ValueArray.HASH_TYPE
            && typeHash != ObjectReaderImplInt64Array.HASH_TYPE
            && typeHash != ObjectReaderImplInt32Array.HASH_TYPE
            && typeHash != ObjectReaderImplInt32ValueArray.HASH_TYPE) {
            throw new JSONException(this.info("not support " + this.getString()));
         }
      }

      int entryCnt = this.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         long[] array = new long[entryCnt];

         for (int i = 0; i < entryCnt; i++) {
            array[i] = this.readInt64Value();
         }

         return array;
      }
   }

   @Override
   public long readInt64Value() {
      this.wasNull = false;
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte type = bytes[offset++];
      long int64Value;
      if (type >= -40 && type <= -17) {
         int64Value = (long)(-8 + (type - -40));
      } else if (type >= -56 && type <= -41) {
         int64Value = (long)((type - -48 << 8) + (bytes[offset] & 255));
         offset++;
      } else if (type >= -64 && type <= -57) {
         int64Value = (long)((type - -60 << 16) + ((bytes[offset] & 255) << 8) + (bytes[offset + 1] & 255));
         offset += 2;
      } else if (type == -65) {
         int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
         int64Value = JDKUtils.BIG_ENDIAN ? (long)int32Value : (long)Integer.reverseBytes(int32Value);
         offset += 4;
      } else {
         if (type != -66) {
            this.offset = offset;
            return this.readInt64Value0(bytes, type);
         }

         int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
         if (!JDKUtils.BIG_ENDIAN) {
            int64Value = Long.reverseBytes(int64Value);
         }

         offset += 8;
      }

      this.offset = offset;
      return int64Value;
   }

   private long readInt64Value0(byte[] bytes, byte type) {
      if (type >= 48 && type <= 63) {
         return (long)((type - 56 << 8) + (bytes[this.offset++] & 255));
      } else if (type >= -16 && type <= 47) {
         return (long)type;
      } else if (type >= 64 && type <= 71) {
         int int3 = getInt3(bytes, this.offset, type);
         this.offset += 2;
         return (long)int3;
      } else {
         switch (type) {
            case -85:
               long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value);
            case -84:
               long seconds = (long)getInt(bytes, this.offset);
               this.offset += 4;
               return seconds * 1000L;
            case -83:
               long minutes = (long)getInt(bytes, this.offset);
               this.offset += 4;
               return minutes * 60L * 1000L;
            case -81:
               if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
                  throw new JSONException(this.info("long value not support input null"));
               }

               this.wasNull = true;
               return 0L;
            case -80:
            case -78:
               return 0L;
            case -79:
            case -77:
               return 1L;
            case -76:
               return (long)((double)this.readInt64Value());
            case -75:
               this.offset--;
               return (long)this.readDoubleValue();
            case -74:
               return (long)((float)this.readInt32Value());
            case -73: {
               int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               float floatValue = Float.intBitsToFloat(JDKUtils.BIG_ENDIAN ? int32Value : Integer.reverseBytes(int32Value));
               return (long)floatValue;
            }
            case -71:
               int scale = this.readInt32Value();
               BigInteger unscaledValue = this.readBigInteger();
               BigDecimal decimal;
               if (scale == 0) {
                  decimal = new BigDecimal(unscaledValue);
               } else {
                  decimal = new BigDecimal(unscaledValue, scale);
               }

               return decimal.longValue();
            case -68:
               int int16Value = (bytes[this.offset + 1] & 255) + (bytes[this.offset] << 8);
               this.offset += 2;
               return (long)int16Value;
            case -67:
               return (long)bytes[this.offset++];
            case 72: {
               int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 4;
               return JDKUtils.BIG_ENDIAN ? (long)int32Value : (long)Integer.reverseBytes(int32Value);
            }
            case 121:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return (long)new BigInteger(str).intValue();
               }

               return (long)TypeUtils.toBigDecimal(str).intValue();
            case 122:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return (long)new BigInteger(str).intValue();
               }

               return (long)TypeUtils.toBigDecimal(str).intValue();
            case 124:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return (long)new BigInteger(str).intValue();
               }

               return (long)TypeUtils.toBigDecimal(str).intValue();
            default:
               if (type >= 73 && type <= 120) {
                  int strlen = type - 73;
                  String str = this.readFixedAsciiString(strlen);
                  this.offset += strlen;
                  return str.indexOf(46) == -1 ? new BigInteger(str).longValue() : TypeUtils.toBigDecimal(str).longValue();
               } else {
                  throw this.readInt64ValueError(type);
               }
         }
      }
   }

   @Override
   public int readInt32Value() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte type = bytes[offset++];
      int int32Value;
      if (type >= -16 && type <= 47) {
         int32Value = type;
      } else if (type >= 48 && type <= 63) {
         int32Value = (type - 56 << 8) + (bytes[offset] & 255);
         offset++;
      } else if (type >= 64 && type <= 71) {
         int32Value = (type - 68 << 16) + ((bytes[offset] & 255) << 8) + (bytes[offset + 1] & 255);
         offset += 2;
      } else {
         if (type != 72) {
            this.offset = offset;
            return this.readInt32Value0(bytes, type);
         }

         int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
         if (!JDKUtils.BIG_ENDIAN) {
            int32Value = Integer.reverseBytes(int32Value);
         }

         offset += 4;
      }

      this.offset = offset;
      return int32Value;
   }

   private int readInt32Value0(byte[] bytes, byte type) {
      if (type >= -40 && type <= -17) {
         return -8 + (type - -40);
      } else if (type >= -56 && type <= -41) {
         return (type - -48 << 8) + (bytes[this.offset++] & 0xFF);
      } else if (type >= -64 && type <= -57) {
         return (type - -60 << 16) + ((bytes[this.offset++] & 0xFF) << 8) + (bytes[this.offset++] & 0xFF);
      } else {
         switch (type) {
            case -84:
            case -83:
            case -65: {
               int int32Value = getInt(bytes, this.offset);
               this.offset += 4;
               return int32Value;
            }
            case -81:
               if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
                  throw new JSONException(this.info("int value not support input null"));
               }

               this.wasNull = true;
               return 0;
            case -80:
            case -78:
               return 0;
            case -79:
            case -77:
               return 1;
            case -76:
               return (int)this.readInt64Value();
            case -75:
               this.offset--;
               return (int)this.readDoubleValue();
            case -74:
               return (int)((float)this.readInt32Value());
            case -73: {
               int int32Value = getInt(bytes, this.offset);
               this.offset += 4;
               float floatValue = Float.intBitsToFloat(int32Value);
               return (int)floatValue;
            }
            case -71:
               int scale = this.readInt32Value();
               BigInteger unscaledValue = this.readBigInteger();
               BigDecimal decimal;
               if (scale == 0) {
                  decimal = new BigDecimal(unscaledValue);
               } else {
                  decimal = new BigDecimal(unscaledValue, scale);
               }

               return decimal.intValue();
            case -68:
               int int16Value = (bytes[this.offset + 1] & 255) + (bytes[this.offset] << 8);
               this.offset += 2;
               return int16Value;
            case -67:
               return bytes[this.offset++];
            case -66:
               long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return (int)(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            case 121:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return new BigInteger(str).intValue();
               }

               return TypeUtils.toBigDecimal(str).intValue();
            case 122:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return new BigInteger(str).intValue();
               }

               return TypeUtils.toBigDecimal(str).intValue();
            case 124:
               int strlen = this.readInt32Value();
               String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
               this.offset += strlen;
               if (str.indexOf(46) == -1) {
                  return new BigInteger(str).intValue();
               }

               return TypeUtils.toBigDecimal(str).intValue();
            default:
               if (type >= 73 && type <= 120) {
                  int strlen = type - 73;
                  String str = this.readFixedAsciiString(strlen);
                  this.offset += strlen;
                  return str.indexOf(46) == -1 ? new BigInteger(str).intValue() : TypeUtils.toBigDecimal(str).intValue();
               } else {
                  throw this.readInt32ValueError(type);
               }
         }
      }
   }

   @Override
   public boolean isBinary() {
      return this.bytes[this.offset] == -111;
   }

   @Override
   public byte[] readBinary() {
      byte type = this.bytes[this.offset++];
      if (type != -111) {
         throw notSupportType(type);
      } else {
         int len = this.readLength();
         byte[] bytes = new byte[len];
         System.arraycopy(this.bytes, this.offset, bytes, 0, len);
         this.offset += len;
         return bytes;
      }
   }

   @Override
   public Integer readInt32() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte type = bytes[offset++];
      if (type == -81) {
         this.offset = offset;
         return null;
      } else {
         int int32Value;
         if (type >= -16 && type <= 47) {
            int32Value = type;
         } else if (type >= 48 && type <= 63) {
            int32Value = (type - 56 << 8) + (bytes[offset] & 255);
            offset++;
         } else if (type >= 64 && type <= 71) {
            int32Value = (type - 68 << 16) + ((bytes[offset] & 255) << 8) + (bytes[offset + 1] & 255);
            offset += 2;
         } else {
            if (type != 72) {
               this.offset = offset;
               return this.readInt32Value0(bytes, type);
            }

            int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
            if (!JDKUtils.BIG_ENDIAN) {
               int32Value = Integer.reverseBytes(int32Value);
            }

            offset += 4;
         }

         this.offset = offset;
         return int32Value;
      }
   }

   @Override
   public Long readInt64() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte type = bytes[offset++];
      if (type == -81) {
         this.offset = offset;
         return null;
      } else {
         long int64Value;
         if (type >= -40 && type <= -17) {
            int64Value = (long)(-8 + (type - -40));
         } else if (type >= -56 && type <= -41) {
            int64Value = (long)((type - -48 << 8) + (bytes[offset] & 255));
            offset++;
         } else if (type >= -64 && type <= -57) {
            int64Value = (long)((type - -60 << 16) + ((bytes[offset] & 255) << 8) + (bytes[offset + 1] & 255));
            offset += 2;
         } else if (type == -65) {
            int int32Val = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
            int64Value = JDKUtils.BIG_ENDIAN ? (long)int32Val : (long)Integer.reverseBytes(int32Val);
            offset += 4;
         } else {
            if (type != -66) {
               this.offset = offset;
               return this.readInt64Value0(bytes, type);
            }

            int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
            if (!JDKUtils.BIG_ENDIAN) {
               int64Value = Long.reverseBytes(int64Value);
            }

            offset += 8;
         }

         this.offset = offset;
         return int64Value;
      }
   }

   protected String readFixedAsciiString(int strlen) {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      String str;
      if (strlen == 1) {
         str = TypeUtils.toString((char)(bytes[offset] & 255));
      } else if (strlen == 2) {
         str = TypeUtils.toString((char)(bytes[offset] & 255), (char)(bytes[offset + 1] & 255));
      } else if (JDKUtils.STRING_CREATOR_JDK8 != null) {
         char[] chars = new char[strlen];

         for (int i = 0; i < strlen; i++) {
            chars[i] = (char)(bytes[offset + i] & 255);
         }

         str = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
      } else {
         str = new String(bytes, offset, strlen, StandardCharsets.ISO_8859_1);
      }

      return str;
   }

   @Override
   public Float readFloat() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      byte type = bytes[offset];
      if (type == -73) {
         int int32Value = (bytes[offset + 4] & 255) + ((bytes[offset + 3] & 255) << 8) + ((bytes[offset + 2] & 255) << 16) + (bytes[offset + 1] << 24);
         this.offset = offset + 5;
         return Float.intBitsToFloat(int32Value);
      } else if (type == -81) {
         this.offset = offset + 1;
         return null;
      } else {
         return this.readFloat0();
      }
   }

   @Override
   public float readFloatValue() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      if (bytes[offset] == -73) {
         int int32Val = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset + 1L);
         this.offset = offset + 5;
         return Float.intBitsToFloat(JDKUtils.BIG_ENDIAN ? int32Val : Integer.reverseBytes(int32Val));
      } else {
         return this.readFloat0();
      }
   }

   private float readFloat0() {
      byte[] bytes = this.bytes;
      byte type = bytes[this.offset++];
      switch (type) {
         case -81:
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            return 0.0F;
         case -80:
         case -78:
            return 0.0F;
         case -79:
         case -77:
            return 1.0F;
         case -76:
            return (float)((double)this.readInt64Value());
         case -75: {
            long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return (float)Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
         }
         case -74:
            return (float)this.readInt32Value();
         case -71:
            int scale = this.readInt32Value();
            BigInteger unscaledValue = this.readBigInteger();
            BigDecimal decimal;
            if (scale == 0) {
               decimal = new BigDecimal(unscaledValue);
            } else {
               decimal = new BigDecimal(unscaledValue, scale);
            }

            return (float)decimal.intValue();
         case -68:
            int int16Value = (bytes[this.offset + 1] & 255) + (bytes[this.offset] << 8);
            this.offset += 2;
            return (float)int16Value;
         case -67:
            return (float)bytes[this.offset++];
         case -66: {
            long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return (float)(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
         }
         case -65:
         case 72:
            int int32Value = getInt(bytes, this.offset);
            this.offset += 4;
            return (float)int32Value;
         case 121:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (float)new BigInteger(str).intValue();
            }

            return (float)TypeUtils.toBigDecimal(str).intValue();
         case 122:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (float)new BigInteger(str).intValue();
            }

            return (float)TypeUtils.toBigDecimal(str).intValue();
         case 124:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (float)new BigInteger(str).intValue();
            }

            return (float)TypeUtils.toBigDecimal(str).intValue();
         default:
            if (type >= -16 && type <= 47) {
               return (float)type;
            } else if (type >= 48 && type <= 63) {
               return (float)((type - 56 << 8) + (bytes[this.offset++] & 255));
            } else if (type >= 64 && type <= 71) {
               int int3 = getInt3(bytes, this.offset, type);
               this.offset += 2;
               return (float)int3;
            } else if (type >= -40 && type <= -17) {
               return (float)(-8 + (type - -40));
            } else if (type >= -56 && type <= -41) {
               return (float)((type - -48 << 8) + (bytes[this.offset++] & 255));
            } else if (type >= -64 && type <= -57) {
               return (float)((type - -60 << 16) + ((bytes[this.offset++] & 255) << 8) + (bytes[this.offset++] & 255));
            } else if (type >= 73 && type <= 120) {
               int strlen = type - 73;
               String str = this.readFixedAsciiString(strlen);
               this.offset += strlen;
               return str.indexOf(46) == -1 ? (float)new BigInteger(str).intValue() : (float)TypeUtils.toBigDecimal(str).intValue();
            } else {
               throw notSupportType(type);
            }
      }
   }

   @Override
   public double readDoubleValue() {
      byte[] bytes = this.bytes;
      int offset = this.offset;
      if (bytes[offset] == -75) {
         long int64Value = ((long)bytes[offset + 8] & 255L)
            + (((long)bytes[offset + 7] & 255L) << 8)
            + (((long)bytes[offset + 6] & 255L) << 16)
            + (((long)bytes[offset + 5] & 255L) << 24)
            + (((long)bytes[offset + 4] & 255L) << 32)
            + (((long)bytes[offset + 3] & 255L) << 40)
            + (((long)bytes[offset + 2] & 255L) << 48)
            + ((long)bytes[offset + 1] << 56);
         this.offset = offset + 9;
         return Double.longBitsToDouble(int64Value);
      } else {
         return this.readDoubleValue0();
      }
   }

   private double readDoubleValue0() {
      byte[] bytes = this.bytes;
      byte type = bytes[this.offset++];
      switch (type) {
         case -81:
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            return 0.0;
         case -80:
         case -78:
            return 0.0;
         case -79:
         case -77:
            return 1.0;
         case -76:
            return (double)this.readInt64Value();
         case -74:
            return (double)((float)this.readInt32Value());
         case -73: {
            int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 4;
            return (double)Float.intBitsToFloat(JDKUtils.BIG_ENDIAN ? int32Value : Integer.reverseBytes(int32Value));
         }
         case -71:
            int scale = this.readInt32Value();
            BigInteger unscaledValue = this.readBigInteger();
            BigDecimal decimal;
            if (scale == 0) {
               decimal = new BigDecimal(unscaledValue);
            } else {
               decimal = new BigDecimal(unscaledValue, scale);
            }

            return (double)decimal.intValue();
         case -68:
            int int16Value = (bytes[this.offset + 1] & 255) + (bytes[this.offset] << 8);
            this.offset += 2;
            return (double)int16Value;
         case -67:
            return (double)bytes[this.offset++];
         case -66:
            long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return (double)(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
         case -65:
         case 72: {
            int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 4;
            return JDKUtils.BIG_ENDIAN ? (double)int32Value : (double)Integer.reverseBytes(int32Value);
         }
         case 121:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (double)new BigInteger(str).intValue();
            }

            return (double)TypeUtils.toBigDecimal(str).intValue();
         case 122:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (double)new BigInteger(str).intValue();
            }

            return (double)TypeUtils.toBigDecimal(str).intValue();
         case 124:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return (double)new BigInteger(str).intValue();
            }

            return (double)TypeUtils.toBigDecimal(str).intValue();
         default:
            if (type >= -16 && type <= 47) {
               return (double)type;
            } else if (type >= 48 && type <= 63) {
               return (double)((type - 56 << 8) + (bytes[this.offset++] & 255));
            } else if (type >= 64 && type <= 71) {
               int int3 = getInt3(bytes, this.offset, type);
               this.offset += 2;
               return (double)int3;
            } else if (type >= -40 && type <= -17) {
               return (double)(-8L + (long)(type - -40));
            } else if (type >= -56 && type <= -41) {
               return (double)((type - -48 << 8) + (bytes[this.offset++] & 255));
            } else if (type >= -64 && type <= -57) {
               return (double)((type - -60 << 16) + ((bytes[this.offset++] & 255) << 8) + (bytes[this.offset++] & 255));
            } else if (type >= 73 && type <= 120) {
               int strlen = type - 73;
               String str = this.readFixedAsciiString(strlen);
               this.offset += strlen;
               return str.indexOf(46) == -1 ? (double)new BigInteger(str).intValue() : (double)TypeUtils.toBigDecimal(str).intValue();
            } else {
               throw notSupportType(type);
            }
      }
   }

   @Override
   protected void readNumber0() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public Number readNumber() {
      byte type = this.bytes[this.offset++];
      if (type >= -16 && type <= 47) {
         return Integer.valueOf(type);
      } else if (type >= 48 && type <= 63) {
         return (type - 56 << 8) + (this.bytes[this.offset++] & 255);
      } else if (type >= 64 && type <= 71) {
         int int3 = getInt3(this.bytes, this.offset, type);
         this.offset += 2;
         return int3;
      } else if (type >= -40 && type <= -17) {
         return -8L + (long)(type - -40);
      } else if (type >= -56 && type <= -41) {
         return (type - -48 << 8) + (this.bytes[this.offset++] & 255);
      } else if (type >= -64 && type <= -57) {
         return (type - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255);
      } else {
         switch (type) {
            case -110:
               String typeName = this.readString();
               throw new JSONException("not support input type : " + typeName);
            case -81:
               return null;
            case -80:
            case -78:
               return 0.0;
            case -79:
            case -77:
               return 1.0;
            case -76:
               return (double)this.readInt64Value();
            case -75: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            }
            case -74:
               return (float)this.readInt32Value();
            case -73: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return Float.intBitsToFloat(int32Value);
            }
            case -72:
               return BigDecimal.valueOf(this.readInt64Value());
            case -71:
               int scale = this.readInt32Value();
               BigInteger unscaledValue = this.readBigInteger();
               if (scale == 0) {
                  return new BigDecimal(unscaledValue);
               }

               return new BigDecimal(unscaledValue, scale);
            case -70:
               return BigInteger.valueOf(this.readInt64Value());
            case -69:
               int len = this.readInt32Value();
               byte[] bytes = new byte[len];
               System.arraycopy(this.bytes, this.offset, bytes, 0, len);
               this.offset += len;
               return new BigInteger(bytes);
            case -68:
               int int16Value = (this.bytes[this.offset + 1] & 255) + (this.bytes[this.offset] << 8);
               this.offset += 2;
               return (short)int16Value;
            case -67:
               return this.bytes[this.offset++];
            case -66: {
               long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
               this.offset += 8;
               return JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value);
            }
            case -65: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return (long)int32Value;
            }
            case 72: {
               int int32Value = getInt(this.bytes, this.offset);
               this.offset += 4;
               return int32Value;
            }
            case 121: {
               int strlen = this.readInt32Value();
               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
               this.offset += strlen;
               return TypeUtils.toBigDecimal(str);
            }
            case 122: {
               int strlen = this.readInt32Value();
               String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_8);
               this.offset += strlen;
               return TypeUtils.toBigDecimal(str);
            }
            default:
               if (type >= 73 && type <= 120) {
                  int strlenx = type - 73;
                  String strx = this.readFixedAsciiString(strlenx);
                  this.offset += strlenx;
                  return TypeUtils.toBigDecimal(strx);
               } else {
                  throw notSupportType(type);
               }
         }
      }
   }

   @Override
   public BigDecimal readBigDecimal() {
      byte[] bytes = this.bytes;
      byte type = bytes[this.offset++];
      BigDecimal decimal;
      if (type == -71) {
         int scale = this.readInt32Value();
         if (bytes[this.offset] == -70) {
            this.offset++;
            long unscaledLongValue = this.readInt64Value();
            decimal = BigDecimal.valueOf(unscaledLongValue, scale);
         } else if (bytes[this.offset] == 72) {
            decimal = BigDecimal.valueOf((long)getInt(bytes, this.offset + 1), scale);
            this.offset += 5;
         } else if (bytes[this.offset] == -66) {
            long unscaledValue = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset + 1L);
            decimal = BigDecimal.valueOf(JDKUtils.BIG_ENDIAN ? unscaledValue : Long.reverseBytes(unscaledValue), scale);
            this.offset += 9;
         } else {
            BigInteger unscaledValue = this.readBigInteger();
            decimal = scale == 0 ? new BigDecimal(unscaledValue) : new BigDecimal(unscaledValue, scale);
         }
      } else if (type == -72) {
         decimal = BigDecimal.valueOf(this.readInt64Value());
      } else {
         decimal = this.readDecimal0(type);
      }

      return decimal;
   }

   private BigDecimal readDecimal0(byte type) {
      switch (type) {
         case -81:
            return null;
         case -80:
         case -78:
            return BigDecimal.ZERO;
         case -79:
         case -77:
            return BigDecimal.ONE;
         case -76: {
            double doubleValue = (double)this.readInt64Value();
            return BigDecimal.valueOf((long)doubleValue);
         }
         case -75: {
            long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            double doubleValue = Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            return BigDecimal.valueOf((long)doubleValue);
         }
         case -74: {
            float floatValue = (float)this.readInt32Value();
            return BigDecimal.valueOf((long)floatValue);
         }
         case -73: {
            int int32Value = getInt(this.bytes, this.offset);
            this.offset += 4;
            float floatValue = Float.intBitsToFloat(int32Value);
            return BigDecimal.valueOf((long)floatValue);
         }
         case -69:
            BigInteger bigInt = this.readBigInteger();
            return new BigDecimal(bigInt);
         case -68:
            int int16Value = (this.bytes[this.offset + 1] & 255) + (this.bytes[this.offset] << 8);
            this.offset += 2;
            return BigDecimal.valueOf((long)int16Value);
         case -67:
            return BigDecimal.valueOf((long)this.bytes[this.offset++]);
         case -66: {
            long int64Value = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return BigDecimal.valueOf(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
         }
         case -65:
         case 72: {
            int int32Value = getInt(this.bytes, this.offset);
            this.offset += 4;
            return BigDecimal.valueOf((long)int32Value);
         }
         case 121: {
            int strlen = this.readInt32Value();
            String str = new String(this.bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
            this.offset += strlen;
            return TypeUtils.toBigDecimal(str);
         }
         case 122: {
            int strlen = this.readInt32Value();
            String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_8);
            this.offset += strlen;
            return TypeUtils.toBigDecimal(str);
         }
         case 124: {
            int strlen = this.readInt32Value();
            String str = new String(this.bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
            this.offset += strlen;
            return TypeUtils.toBigDecimal(str);
         }
         default:
            if (type >= -16 && type <= 47) {
               return BigDecimal.valueOf((long)type);
            } else if (type >= 48 && type <= 63) {
               int intValue = (type - 56 << 8) + (this.bytes[this.offset++] & 255);
               return BigDecimal.valueOf((long)intValue);
            } else if (type >= 64 && type <= 71) {
               int int3 = getInt3(this.bytes, this.offset, type);
               this.offset += 2;
               return BigDecimal.valueOf((long)int3);
            } else if (type >= -40 && type <= -17) {
               int intValue = -8 + (type - -40);
               return BigDecimal.valueOf((long)intValue);
            } else if (type >= -56 && type <= -41) {
               int intValue = (type - -48 << 8) + (this.bytes[this.offset++] & 255);
               return BigDecimal.valueOf((long)intValue);
            } else if (type >= -64 && type <= -57) {
               int intValue = (type - -60 << 16) + ((this.bytes[this.offset++] & 255) << 8) + (this.bytes[this.offset++] & 255);
               return BigDecimal.valueOf((long)intValue);
            } else if (type >= 73 && type <= 120) {
               int strlenx = type - 73;
               String strx = this.readFixedAsciiString(strlenx);
               this.offset += strlenx;
               return TypeUtils.toBigDecimal(strx);
            } else {
               throw notSupportType(type);
            }
      }
   }

   @Override
   public BigInteger readBigInteger() {
      byte type = this.bytes[this.offset++];
      BigInteger bigInt;
      if (type == -70) {
         bigInt = BigInteger.valueOf(this.readInt64Value());
      } else if (type == -69) {
         int len = this.readInt32Value();
         byte[] bytes = new byte[len];
         System.arraycopy(this.bytes, this.offset, bytes, 0, len);
         this.offset += len;
         bigInt = new BigInteger(bytes);
      } else {
         bigInt = this.readBigInteger0(type);
      }

      return bigInt;
   }

   private BigInteger readBigInteger0(byte type) {
      byte[] bytes = this.bytes;
      switch (type) {
         case -111:
            int len = this.readInt32Value();
            byte[] buf = new byte[len];
            System.arraycopy(this.bytes, this.offset, buf, 0, len);
            this.offset += len;
            return new BigInteger(buf);
         case -81:
            return null;
         case -80:
         case -78:
            return BigInteger.ZERO;
         case -79:
         case -77:
            return BigInteger.ONE;
         case -76: {
            double doubleValue = (double)this.readInt64Value();
            return BigInteger.valueOf((long)doubleValue);
         }
         case -75: {
            long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            double doubleValue = Double.longBitsToDouble(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
            return BigInteger.valueOf((long)doubleValue);
         }
         case -74: {
            float floatValue = (float)this.readInt32Value();
            return BigInteger.valueOf((long)floatValue);
         }
         case -73: {
            int int32Value = getInt(bytes, this.offset);
            this.offset += 4;
            float floatValue = Float.intBitsToFloat(int32Value);
            return BigInteger.valueOf((long)floatValue);
         }
         case -71:
            int scale = this.readInt32Value();
            BigInteger unscaledValue = this.readBigInteger();
            BigDecimal decimal;
            if (scale == 0) {
               decimal = new BigDecimal(unscaledValue);
            } else {
               decimal = new BigDecimal(unscaledValue, scale);
            }

            return decimal.toBigInteger();
         case -68:
            int int16Value = (bytes[this.offset + 1] & 255) + (bytes[this.offset] << 8);
            this.offset += 2;
            return BigInteger.valueOf((long)int16Value);
         case -67:
            return BigInteger.valueOf((long)bytes[this.offset++]);
         case -66: {
            long int64Value = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return BigInteger.valueOf(JDKUtils.BIG_ENDIAN ? int64Value : Long.reverseBytes(int64Value));
         }
         case -65:
         case 72: {
            int int32Value = getInt(bytes, this.offset);
            this.offset += 4;
            return BigInteger.valueOf((long)int32Value);
         }
         case 121:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.ISO_8859_1);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return new BigInteger(str);
            }

            return TypeUtils.toBigDecimal(str).toBigInteger();
         case 122:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return new BigInteger(str);
            }

            return TypeUtils.toBigDecimal(str).toBigInteger();
         case 124:
            int strlen = this.readInt32Value();
            String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_16LE);
            this.offset += strlen;
            if (str.indexOf(46) == -1) {
               return new BigInteger(str);
            }

            return TypeUtils.toBigDecimal(str).toBigInteger();
         default:
            if (type >= -16 && type <= 47) {
               return BigInteger.valueOf((long)type);
            } else if (type >= 48 && type <= 63) {
               int intValue = (type - 56 << 8) + (bytes[this.offset++] & 255);
               return BigInteger.valueOf((long)intValue);
            } else if (type >= 64 && type <= 71) {
               int int3 = getInt3(bytes, this.offset, type);
               this.offset += 2;
               return BigInteger.valueOf((long)int3);
            } else if (type >= -40 && type <= -17) {
               int intValue = -8 + (type - -40);
               return BigInteger.valueOf((long)intValue);
            } else if (type >= -56 && type <= -41) {
               int intValue = (type - -48 << 8) + (bytes[this.offset++] & 255);
               return BigInteger.valueOf((long)intValue);
            } else if (type >= -64 && type <= -57) {
               int intValue = (type - -60 << 16) + ((bytes[this.offset++] & 255) << 8) + (bytes[this.offset++] & 255);
               return BigInteger.valueOf((long)intValue);
            } else if (type >= 73 && type <= 120) {
               int strlen = type - 73;
               String str = this.readFixedAsciiString(strlen);
               this.offset += strlen;
               return new BigInteger(str);
            } else {
               throw notSupportType(type);
            }
      }
   }

   @Override
   public LocalDate readLocalDate() {
      int type = this.bytes[this.offset];
      if (type == -87) {
         this.offset++;
         int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
         int month = this.bytes[this.offset++];
         int dayOfMonth = this.bytes[this.offset++];
         return LocalDate.of(year, month, dayOfMonth);
      } else if (type == -81) {
         this.offset++;
         return null;
      } else {
         return this.readLocalDate0(type);
      }
   }

   private LocalDate readLocalDate0(int type) {
      if (type == -88) {
         return this.readLocalDateTime().toLocalDate();
      } else if (type == -86) {
         return this.readZonedDateTime().toLocalDate();
      } else if (type >= 73 && type <= 120) {
         int len = this.getStringLength();
         switch (len) {
            case 8:
               return this.readLocalDate8();
            case 9:
               return this.readLocalDate9();
            case 10:
               return this.readLocalDate10();
            case 11:
               return this.readLocalDate11();
            default:
               if (this.bytes[this.offset + len] == 90) {
                  ZonedDateTime zdt = this.readZonedDateTime();
                  return zdt.toInstant().atZone(this.context.getZoneId()).toLocalDate();
               } else {
                  throw new JSONException("TODO : " + len + ", " + this.readString());
               }
         }
      } else {
         if (type == 122 || type == 121) {
            this.strtype = (byte)type;
            this.offset++;
            this.strlen = this.readLength();
            switch (this.strlen) {
               case 8:
                  return this.readLocalDate8();
               case 9:
                  return this.readLocalDate9();
               case 10:
                  return this.readLocalDate10();
               case 11:
                  return this.readLocalDate11();
            }
         }

         throw notSupportType((byte)type);
      }
   }

   @Override
   public LocalDateTime readLocalDateTime() {
      int type = this.bytes[this.offset];
      if (type == -88) {
         this.offset++;
         int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
         int month = this.bytes[this.offset++];
         int dayOfMonth = this.bytes[this.offset++];
         int hour = this.bytes[this.offset++];
         int minute = this.bytes[this.offset++];
         int second = this.bytes[this.offset++];
         int nano = this.readInt32Value();
         return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
      } else if (type == -81) {
         this.offset++;
         return null;
      } else {
         return this.readLocalDateTime0(type);
      }
   }

   private LocalDateTime readLocalDateTime0(int type) {
      if (type == -87) {
         LocalDate localDate = this.readLocalDate();
         return localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
      } else if (type == -86) {
         return this.readZonedDateTime().toLocalDateTime();
      } else if (type >= 73 && type <= 120) {
         int len = this.getStringLength();
         switch (len) {
            case 8: {
               LocalDate localDate = this.readLocalDate8();
               return localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
            }
            case 9: {
               LocalDate localDate = this.readLocalDate9();
               return localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
            }
            case 10: {
               LocalDate localDate = this.readLocalDate10();
               return localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
            }
            case 11: {
               LocalDate localDate = this.readLocalDate11();
               return localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
            }
            case 16:
               return this.readLocalDateTime16();
            case 17:
               return this.readLocalDateTime17();
            case 18:
               return this.readLocalDateTime18();
            case 19:
               return this.readLocalDateTime19();
            case 20:
               return this.readLocalDateTime20();
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
               LocalDateTime ldt = this.readLocalDateTimeX(len);
               if (ldt != null) {
                  return ldt;
               } else {
                  ZonedDateTime zdt = this.readZonedDateTimeX(len);
                  if (zdt != null) {
                     return zdt.toLocalDateTime();
                  }
               }
            case 12:
            case 13:
            case 14:
            case 15:
            default:
               throw new JSONException("TODO : " + len + ", " + this.readString());
         }
      } else {
         throw notSupportType((byte)type);
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime12() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 85 && (ldt = DateUtils.parseLocalDateTime12(this.bytes, this.offset + 1)) != null) {
         this.offset += 13;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime14() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 87 && (ldt = DateUtils.parseLocalDateTime14(this.bytes, this.offset + 1)) != null) {
         this.offset += 15;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime16() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 89 && (ldt = DateUtils.parseLocalDateTime16(this.bytes, this.offset + 1)) != null) {
         this.offset += 17;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime17() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 90 && (ldt = DateUtils.parseLocalDateTime17(this.bytes, this.offset + 1)) != null) {
         this.offset += 18;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime10() {
      LocalTime time;
      if (this.bytes[this.offset] == 83 && (time = DateUtils.parseLocalTime10(this.bytes, this.offset + 1)) != null) {
         this.offset += 11;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime11() {
      LocalTime time;
      if (this.bytes[this.offset] == 84 && (time = DateUtils.parseLocalTime11(this.bytes, this.offset + 1)) != null) {
         this.offset += 12;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected ZonedDateTime readZonedDateTimeX(int len) {
      this.type = this.bytes[this.offset];
      if (this.type >= 73 && this.type <= 120) {
         ZonedDateTime ldt;
         if (len >= 19 && (ldt = DateUtils.parseZonedDateTime(this.bytes, this.offset + 1, len, this.context.zoneId)) != null) {
            this.offset += len + 1;
            return ldt;
         } else {
            throw new JSONException("illegal LocalDateTime string : " + this.readString());
         }
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   public void skipComment() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public LocalTime readLocalTime() {
      int type = this.bytes[this.offset];
      if (type == -89) {
         this.offset++;
         int hour = this.bytes[this.offset++];
         int minute = this.bytes[this.offset++];
         int second = this.bytes[this.offset++];
         int nano = this.readInt32Value();
         return LocalTime.of(hour, minute, second, nano);
      } else if (type == -81) {
         this.offset++;
         return null;
      } else if (type >= 73 && type <= 120) {
         int len = this.getStringLength();
         switch (len) {
            case 5:
               return this.readLocalTime5();
            case 6:
               return this.readLocalTime6();
            case 7:
               return this.readLocalTime7();
            case 8:
               return this.readLocalTime8();
            case 9:
               return this.readLocalTime9();
            case 10:
               return this.readLocalTime10();
            case 11:
               return this.readLocalTime11();
            case 12:
               return this.readLocalTime12();
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            default:
               throw new JSONException("not support len : " + len);
            case 18:
               return this.readLocalTime18();
         }
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public Instant readInstant() {
      int type = this.bytes[this.offset++];
      switch (type) {
         case -85:
         case -66:
            long millis = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            return Instant.ofEpochMilli(JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
         case -84:
            long seconds = (long)getInt(this.bytes, this.offset);
            this.offset += 4;
            return Instant.ofEpochSecond(seconds, 0L);
         case -83:
            long minutes = (long)getInt(this.bytes, this.offset);
            this.offset += 4;
            return Instant.ofEpochSecond(minutes * 60L, 0L);
         case -82:
            return Instant.ofEpochSecond(this.readInt64Value(), (long)this.readInt32Value());
         default:
            throw new UnsupportedOperationException();
      }
   }

   @Override
   public OffsetTime readOffsetTime() {
      ZonedDateTime zdt = this.readZonedDateTime();
      return zdt == null ? null : zdt.toOffsetDateTime().toOffsetTime();
   }

   @Override
   public OffsetDateTime readOffsetDateTime() {
      ZonedDateTime zdt = this.readZonedDateTime();
      return zdt == null ? null : zdt.toOffsetDateTime();
   }

   @Override
   public ZonedDateTime readZonedDateTime() {
      int type = this.bytes[this.offset++];
      if (type == -86) {
         int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
         int month = this.bytes[this.offset++];
         int dayOfMonth = this.bytes[this.offset++];
         int hour = this.bytes[this.offset++];
         int minute = this.bytes[this.offset++];
         int second = this.bytes[this.offset++];
         int nano = this.readInt32Value();
         LocalDateTime ldt = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
         long zoneIdHash = this.readValueHashCode();
         long SHANGHAI_ZONE_ID_HASH = -4800907791268808639L;
         ZoneId zoneId;
         if (zoneIdHash == -4800907791268808639L) {
            zoneId = DateUtils.SHANGHAI_ZONE_ID;
         } else {
            String zoneIdStr = this.getString();
            ZoneId contextZoneId = this.context.getZoneId();
            if (contextZoneId.getId().equals(zoneIdStr)) {
               zoneId = contextZoneId;
            } else {
               zoneId = DateUtils.getZoneId(zoneIdStr, DateUtils.SHANGHAI_ZONE_ID);
            }
         }

         return ZonedDateTime.ofLocal(ldt, zoneId, null);
      } else {
         return this.readZonedDateTime0(type);
      }
   }

   private ZonedDateTime readZonedDateTime0(int type) {
      switch (type) {
         case -88: {
            int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
            byte month = this.bytes[this.offset++];
            byte dayOfMonth = this.bytes[this.offset++];
            byte hour = this.bytes[this.offset++];
            byte minute = this.bytes[this.offset++];
            byte second = this.bytes[this.offset++];
            int nano = this.readInt32Value();
            LocalDateTime ldt = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nano);
            return ZonedDateTime.of(ldt, DateUtils.DEFAULT_ZONE_ID);
         }
         case -87: {
            int year = (this.bytes[this.offset++] << 8) + (this.bytes[this.offset++] & 255);
            byte month = this.bytes[this.offset++];
            byte dayOfMonth = this.bytes[this.offset++];
            LocalDate localDate = LocalDate.of(year, month, dayOfMonth);
            return ZonedDateTime.of(localDate, LocalTime.MIN, DateUtils.DEFAULT_ZONE_ID);
         }
         case -86:
         case -80:
         case -79:
         case -78:
         case -77:
         case -76:
         case -75:
         case -74:
         case -73:
         case -72:
         case -71:
         case -70:
         case -69:
         case -68:
         case -67:
         default:
            if (type >= 73 && type <= 120) {
               this.offset--;
               return this.readZonedDateTimeX(type - 73);
            }

            throw notSupportType((byte)type);
         case -85:
         case -66: {
            long millis = JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            this.offset += 8;
            Instant instant = Instant.ofEpochMilli(JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
            return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
         }
         case -84: {
            long seconds = (long)getInt(this.bytes, this.offset);
            this.offset += 4;
            Instant instant = Instant.ofEpochSecond(seconds);
            return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
         }
         case -83: {
            long minutes = (long)getInt(this.bytes, this.offset);
            this.offset += 4;
            Instant instant = Instant.ofEpochSecond(minutes * 60L);
            return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
         }
         case -82: {
            long second = this.readInt64Value();
            int nano = this.readInt32Value();
            Instant instant = Instant.ofEpochSecond(second, (long)nano);
            return ZonedDateTime.ofInstant(instant, DateUtils.DEFAULT_ZONE_ID);
         }
         case -81:
            return null;
      }
   }

   @Override
   public UUID readUUID() {
      byte[] bytes = this.bytes;
      byte type = bytes[this.offset++];
      long hi = 0L;
      long lo = 0L;
      switch (type) {
         case -111:
            int len = this.readLength();
            if (len != 16) {
               throw new JSONException("uuid not support " + len);
            }

            long msb = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset);
            long lsb = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset + 8L);
            this.offset += 16;
            hi = JDKUtils.BIG_ENDIAN ? msb : Long.reverseBytes(msb);
            lo = JDKUtils.BIG_ENDIAN ? lsb : Long.reverseBytes(lsb);
            break;
         case -81:
            return null;
         case 105:
            for (int i = 0; i < 16; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            for (int i = 16; i < 32; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            this.offset += 32;
            break;
         case 109:
            byte ch1 = bytes[this.offset + 8];
            byte ch2 = bytes[this.offset + 13];
            byte ch3 = bytes[this.offset + 18];
            byte ch4 = bytes[this.offset + 23];
            if (ch1 != 45 || ch2 != 45 || ch3 != 45 || ch4 != 45) {
               throw new JSONException("Invalid UUID string:  " + new String(bytes, this.offset, 36, StandardCharsets.ISO_8859_1));
            }

            for (int i = 0; i < 8; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            for (int i = 9; i < 13; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            for (int i = 14; i < 18; i++) {
               hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            for (int i = 19; i < 23; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            for (int i = 24; i < 36; i++) {
               lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
            }

            this.offset += 36;
            break;
         case 121:
         case 122:
            int strlen = this.readLength();
            if (strlen == 32) {
               for (int i = 0; i < 16; i++) {
                  hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
               }

               for (int i = 16; i < 32; i++) {
                  lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
               }

               this.offset += 32;
            } else {
               if (strlen != 36) {
                  String str = new String(bytes, this.offset, strlen, StandardCharsets.UTF_8);
                  this.offset += strlen;
                  throw new JSONException("Invalid UUID string:  " + str);
               }

               byte ch1 = bytes[this.offset + 8];
               byte ch2 = bytes[this.offset + 13];
               byte ch3 = bytes[this.offset + 18];
               byte ch4 = bytes[this.offset + 23];
               if (ch1 == 45 && ch2 == 45 && ch3 == 45 && ch4 == 45) {
                  for (int i = 0; i < 8; i++) {
                     hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
                  }

                  for (int i = 9; i < 13; i++) {
                     hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
                  }

                  for (int i = 14; i < 18; i++) {
                     hi = (hi << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
                  }

                  for (int i = 19; i < 23; i++) {
                     lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
                  }

                  for (int i = 24; i < 36; i++) {
                     lo = (lo << 4) + (long)JSONFactory.UUID_VALUES[bytes[this.offset + i] - 48];
                  }

                  this.offset += 36;
               }
            }
            break;
         default:
            throw notSupportType(type);
      }

      return new UUID(hi, lo);
   }

   @Override
   public Boolean readBool() {
      byte type = this.bytes[this.offset++];
      if (type == -81) {
         return null;
      } else if (type == -79) {
         return true;
      } else {
         return type == -80 ? false : this.readBoolValue0(type);
      }
   }

   @Override
   public boolean readBoolValue() {
      this.wasNull = false;
      byte type = this.bytes[this.offset++];
      if (type == -79) {
         return true;
      } else {
         return type == -80 ? false : this.readBoolValue0(type);
      }
   }

   private boolean readBoolValue0(byte type) {
      byte[] bytes = this.bytes;
      switch (type) {
         case -81:
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            this.wasNull = true;
            return false;
         case 0:
            return false;
         case 1:
            return true;
         case 74:
            if (bytes[this.offset] == 49 || bytes[this.offset] == 89) {
               this.offset++;
               return true;
            } else if (bytes[this.offset] == 48 || bytes[this.offset] == 78) {
               this.offset++;
               return false;
            }
         case 77:
            if (bytes[this.offset] == 116 && bytes[this.offset + 1] == 114 && bytes[this.offset + 2] == 117 && bytes[this.offset + 3] == 101) {
               this.offset += 4;
               return true;
            } else if (bytes[this.offset] == 84 && bytes[this.offset + 1] == 82 && bytes[this.offset + 2] == 85 && bytes[this.offset + 3] == 69) {
               this.offset += 4;
               return true;
            }
         case 78:
            if (bytes[this.offset] == 102
               && bytes[this.offset + 1] == 97
               && bytes[this.offset + 2] == 108
               && bytes[this.offset + 3] == 115
               && bytes[this.offset + 4] == 101) {
               this.offset += 5;
               return false;
            } else if (bytes[this.offset] == 70
               && bytes[this.offset + 1] == 65
               && bytes[this.offset + 2] == 76
               && bytes[this.offset + 3] == 83
               && bytes[this.offset + 4] == 69) {
               this.offset += 5;
               return false;
            }
         case 121:
         case 122:
            this.strlen = this.readLength();
            if (this.strlen == 1) {
               if (bytes[this.offset] == 89) {
                  this.offset++;
                  return true;
               }

               if (bytes[this.offset] == 78) {
                  this.offset++;
                  return true;
               }
            } else {
               if (this.strlen == 4
                  && bytes[this.offset] == 116
                  && bytes[this.offset + 1] == 114
                  && bytes[this.offset + 2] == 117
                  && bytes[this.offset + 3] == 101) {
                  this.offset += 4;
                  return true;
               }

               if (this.strlen == 5) {
                  if (bytes[this.offset] == 102
                     && bytes[this.offset + 1] == 97
                     && bytes[this.offset + 2] == 108
                     && bytes[this.offset + 3] == 115
                     && bytes[this.offset + 4] == 101) {
                     this.offset += 5;
                     return false;
                  }

                  if (bytes[this.offset] == 70
                     && bytes[this.offset + 1] == 65
                     && bytes[this.offset + 2] == 76
                     && bytes[this.offset + 3] == 83
                     && bytes[this.offset + 4] == 69) {
                     this.offset += 5;
                     return false;
                  }
               }
            }

            String str = new String(bytes, this.offset, this.strlen, StandardCharsets.ISO_8859_1);
            this.offset = this.offset + this.strlen;
            throw new JSONException("not support input " + str);
         case 123:
         case 124:
         case 125:
            this.strlen = this.readLength();
            byte[] chars = new byte[this.strlen];
            System.arraycopy(bytes, this.offset, chars, 0, this.strlen);
            Charset charset = type == 125 ? StandardCharsets.UTF_16BE : (type == 124 ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16);
            String str = new String(chars, charset);
            this.offset = this.offset + this.strlen;
            switch (str) {
               case "0":
               case "N":
               case "false":
               case "FALSE":
                  return false;
               case "1":
               case "Y":
               case "true":
               case "TRUE":
                  return true;
               default:
                  throw new JSONException("not support input " + str);
            }
         default:
            throw notSupportType(type);
      }
   }

   @Override
   public boolean nextIfMatch(byte type) {
      if (this.bytes[this.offset] == type) {
         this.offset++;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfMatchTypedAny() {
      if (this.bytes[this.offset] == -110) {
         this.offset++;
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected int getStringLength() {
      this.type = this.bytes[this.offset];
      if (this.type >= 73 && this.type < 120) {
         return this.type - 73;
      } else {
         throw new UnsupportedOperationException();
      }
   }

   @Override
   public boolean isDate() {
      byte type = this.bytes[this.offset];
      return type >= -89 && type <= -82;
   }

   @Override
   public Date readDate() {
      ZonedDateTime zdt = null;
      int offset = this.offset;
      byte[] bytes = this.bytes;
      byte type = bytes[offset];
      switch (type) {
         case -89: {
            LocalTime localTime = this.readLocalTime();
            LocalDateTime ldt = LocalDateTime.of(LocalDate.of(1970, 1, 1), localTime);
            zdt = ZonedDateTime.ofLocal(ldt, this.context.getZoneId(), null);
            break;
         }
         case -88: {
            LocalDateTime ldt = this.readLocalDateTime();
            zdt = ZonedDateTime.ofLocal(ldt, this.context.getZoneId(), null);
            break;
         }
         case -87:
            LocalDate localDate = this.readLocalDate();
            zdt = ZonedDateTime.ofLocal(LocalDateTime.of(localDate, LocalTime.MIN), this.context.getZoneId(), null);
            break;
         case -86:
            this.offset = offset + 1;
            zdt = this.readTimestampWithTimeZone();
            break;
         case -85:
            long millis = JDKUtils.UNSAFE.getLong(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset + 1L);
            this.offset += 9;
            return new Date(JDKUtils.BIG_ENDIAN ? millis : Long.reverseBytes(millis));
         case -84:
            long seconds = (long)getInt(bytes, offset + 1);
            this.offset += 5;
            return new Date(seconds * 1000L);
         case -83:
            long minutes = (long)getInt(bytes, offset + 1);
            this.offset += 5;
            return new Date(minutes * 60L * 1000L);
         case -82:
            this.offset = offset + 1;
            long epochSeconds = this.readInt64Value();
            int nano = this.readInt32Value();
            return Date.from(Instant.ofEpochSecond(epochSeconds, (long)nano));
      }

      if (zdt == null) {
         return super.readDate();
      } else {
         long seconds = zdt.toEpochSecond();
         int nanos = zdt.toLocalTime().getNano();
         long millis;
         if (seconds < 0L && nanos > 0) {
            millis = (seconds + 1L) * 1000L;
            long adjustment = (long)(nanos / 1000000 - 1000);
            millis += adjustment;
         } else {
            millis = seconds * 1000L;
            millis += (long)(nanos / 1000000);
         }

         return new Date(millis);
      }
   }

   @Override
   public LocalDate readLocalDate8() {
      LocalDate ldt;
      if (this.bytes[this.offset] == 81 && (ldt = DateUtils.parseLocalDate8(this.bytes, this.offset + 1)) != null) {
         this.offset += 9;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   public LocalDate readLocalDate9() {
      LocalDate ldt;
      if (this.bytes[this.offset] == 82 && (ldt = DateUtils.parseLocalDate9(this.bytes, this.offset + 1)) != null) {
         this.offset += 10;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDate readLocalDate10() {
      LocalDate ldt;
      if ((this.strtype == 121 || this.strtype == 122) && this.strlen == 10) {
         ldt = DateUtils.parseLocalDate10(this.bytes, this.offset);
      } else if (this.bytes[this.offset] != 83 || (ldt = DateUtils.parseLocalDate10(this.bytes, this.offset + 1)) == null) {
         throw new JSONException("date only support string input");
      }

      this.offset += 11;
      return ldt;
   }

   @Override
   protected LocalDate readLocalDate11() {
      LocalDate ldt;
      if ((this.strtype == 121 || this.strtype == 122) && this.strlen == 11) {
         ldt = DateUtils.parseLocalDate11(this.bytes, this.offset);
      } else if (this.bytes[this.offset] != 84 || (ldt = DateUtils.parseLocalDate11(this.bytes, this.offset + 1)) == null) {
         throw new JSONException("date only support string input");
      }

      this.offset += 12;
      return ldt;
   }

   @Override
   protected LocalTime readLocalTime5() {
      LocalTime time;
      if (this.bytes[this.offset] == 78 && (time = DateUtils.parseLocalTime5(this.bytes, this.offset + 1)) != null) {
         this.offset += 6;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime6() {
      LocalTime time;
      if (this.bytes[this.offset] == 79 && (time = DateUtils.parseLocalTime6(this.bytes, this.offset + 1)) != null) {
         this.offset += 7;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime7() {
      LocalTime time;
      if (this.bytes[this.offset] == 80 && (time = DateUtils.parseLocalTime7(this.bytes, this.offset + 1)) != null) {
         this.offset += 8;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime8() {
      LocalTime time;
      if (this.bytes[this.offset] == 81 && (time = DateUtils.parseLocalTime8(this.bytes, this.offset + 1)) != null) {
         this.offset += 9;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime9() {
      LocalTime time;
      if (this.bytes[this.offset] == 82 && (time = DateUtils.parseLocalTime8(this.bytes, this.offset + 1)) != null) {
         this.offset += 10;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime12() {
      LocalTime time;
      if (this.bytes[this.offset] == 85 && (time = DateUtils.parseLocalTime12(this.bytes, this.offset + 1)) != null) {
         this.offset += 13;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalTime readLocalTime18() {
      LocalTime time;
      if (this.bytes[this.offset] == 91 && (time = DateUtils.parseLocalTime18(this.bytes, this.offset + 1)) != null) {
         this.offset += 19;
         return time;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime18() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 91 && (ldt = DateUtils.parseLocalDateTime18(this.bytes, this.offset + 1)) != null) {
         this.offset += 19;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime20() {
      LocalDateTime ldt;
      if (this.bytes[this.offset] == 93 && (ldt = DateUtils.parseLocalDateTime20(this.bytes, this.offset + 1)) != null) {
         this.offset += 21;
         return ldt;
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   public long readMillis19() {
      if (this.bytes[this.offset] != 92) {
         throw new JSONException("date only support string input");
      } else {
         long millis = DateUtils.parseMillis19(this.bytes, this.offset + 1, this.context.zoneId);
         this.offset += 20;
         return millis;
      }
   }

   @Override
   protected LocalDateTime readLocalDateTime19() {
      this.type = this.bytes[this.offset];
      if (this.type != 92) {
         throw new JSONException("date only support string input");
      } else {
         LocalDateTime ldt = DateUtils.parseLocalDateTime19(this.bytes, this.offset + 1);
         if (ldt == null) {
            throw new JSONException("date only support string input");
         } else {
            this.offset += 20;
            return ldt;
         }
      }
   }

   @Override
   protected LocalDateTime readLocalDateTimeX(int len) {
      this.type = this.bytes[this.offset];
      if (this.type >= 73 && this.type <= 120) {
         LocalDateTime ldt;
         if (len >= 21 && len <= 29 && (ldt = DateUtils.parseLocalDateTimeX(this.bytes, this.offset + 1, len)) != null) {
            this.offset += len + 1;
            return ldt;
         } else {
            throw new JSONException("illegal LocalDateTime string : " + this.readString());
         }
      } else {
         throw new JSONException("date only support string input");
      }
   }

   @Override
   public String readPattern() {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfMatchIdent(char c0, char c1) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfMatchIdent(char c0, char c1, char c2) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public long readFieldNameHashCodeUnquote() {
      return this.readFieldNameHashCode();
   }

   @Override
   public boolean nextIfSet() {
      return false;
   }

   @Override
   public boolean nextIfInfinity() {
      return false;
   }

   @Override
   public boolean nextIfMatchIdent(char c0, char c1, char c2, char c3) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfMatchIdent(char c0, char c1, char c2, char c3, char c4) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public boolean nextIfMatchIdent(char c0, char c1, char c2, char c3, char c4, char c5) {
      throw new JSONException("UnsupportedOperation");
   }

   @Override
   public JSONReader.SavePoint mark() {
      return new JSONReader.SavePoint(this.offset, this.type);
   }

   @Override
   public void reset(JSONReader.SavePoint savePoint) {
      this.offset = savePoint.offset;
      this.type = (byte)savePoint.current;
   }

   @Override
   public void close() {
      byte[] valueBytes = this.valueBytes;
      if (valueBytes != null && valueBytes.length < 4194304) {
         JSONFactory.BYTES_UPDATER.lazySet(this.cacheItem, valueBytes);
      }

      char[] nameChars = this.charBuf;
      if (nameChars != null && nameChars.length < 4194304) {
         JSONFactory.CHARS_UPDATER.lazySet(this.cacheItem, nameChars);
      }
   }

   @Override
   public boolean isEnd() {
      return this.offset >= this.end;
   }

   @Override
   public int getRawInt() {
      return this.offset + 3 < this.end ? JDKUtils.UNSAFE.getInt(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset) : 0;
   }

   @Override
   public long getRawLong() {
      return this.offset + 7 < this.end ? JDKUtils.UNSAFE.getLong(this.bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)this.offset) : 0L;
   }

   @Override
   public boolean nextIfName4Match2() {
      return false;
   }

   @Override
   public boolean nextIfName4Match3() {
      int offset = this.offset + 4;
      if (offset > this.end) {
         return false;
      } else {
         this.offset = offset;
         return true;
      }
   }

   @Override
   public boolean nextIfName4Match4(byte name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 5;
      if (offset <= this.end && bytes[offset - 1] == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match5(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 6;
      if (offset <= this.end && JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset - 2L) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match6(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 7;
      if (offset <= this.end && (JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 3L) & 16777215) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match7(int name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 8;
      if (offset <= this.end && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 4L) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match8(int name1, byte name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 9;
      if (offset < this.end && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 5L) == name1 && bytes[offset - 1] == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match9(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 10;
      if (offset + 1 < this.end && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 6L) & 281474976710655L) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match10(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 11;
      if (offset < this.end && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 7L) & 72057594037927935L) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match11(long name1) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 12;
      if (offset < this.end && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 8L) == name1) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match12(long name1, byte name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 13;
      if (offset < this.end && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 9L) == name1 && bytes[offset - 1] == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match13(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 14;
      if (offset + 1 < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 10L) == name1
         && JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset - 2L) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match14(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 15;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 11L) == name1
         && (JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 3L) & 16777215) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match15(long name1, int name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 16;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 12L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 4L) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match16(long name1, int name2, byte name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 17;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 13L) == name1
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 5L) == name2
         && bytes[offset - 1] == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match17(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 18;
      if (offset + 1 < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 14L) == name1
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 6L) & 281474976710655L) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match18(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 19;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 15L) == name1
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 7L) & 72057594037927935L) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match19(long name1, long name2) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 20;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 16L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 8L) == name2) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match20(long name1, long name2, byte name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 21;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 17L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 9L) == name2
         && bytes[offset - 1] == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match21(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 22;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 18L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 10L) == name2
         && JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset - 2L) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match22(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 23;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 19L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 11L) == name2
         && (JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 3L) & 16777215) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match23(long name1, long name2, int name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 24;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 20L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 12L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 4L) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match24(long name1, long name2, int name3, byte name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 25;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 21L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 13L) == name2
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 5L) == name3
         && bytes[offset - 1] == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match25(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 26;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 22L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 14L) == name2
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 6L) & 281474976710655L) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match26(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 27;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 23L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 15L) == name2
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 7L) & 72057594037927935L) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match27(long name1, long name2, long name3) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 28;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 24L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 16L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 8L) == name3) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match28(long name1, long name2, long name3, byte name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 29;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 25L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 17L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 9L) == name3
         && bytes[offset - 1] == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match29(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 30;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 26L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 18L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 10L) == name3
         && JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset - 2L) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match30(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 31;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 27L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 19L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 11L) == name3
         && (JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 3L) & 16777215) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match31(long name1, long name2, long name3, int name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 32;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 28L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 20L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 12L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 4L) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match32(long name1, long name2, long name3, int name4, byte name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 33;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 29L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 21L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 13L) == name3
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 5L) == name4
         && bytes[offset - 1] == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match33(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 34;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 30L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 22L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 14L) == name3
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 6L) & 281474976710655L) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match34(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 35;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 31L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 23L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 15L) == name3
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 7L) & 72057594037927935L) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match35(long name1, long name2, long name3, long name4) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 36;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 32L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 24L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 16L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 8L) == name4) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match36(long name1, long name2, long name3, long name4, byte name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 37;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 33L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 25L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 17L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 9L) == name4
         && bytes[offset - 1] == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match37(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 38;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 34L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 26L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 18L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 10L) == name4
         && JDKUtils.UNSAFE.getShort(bytes, BASE + (long)offset - 2L) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match38(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 39;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 35L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 27L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 19L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 11L) == name4
         && (JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 3L) & 16777215) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match39(long name1, long name2, long name3, long name4, int name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 40;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 36L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 28L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 20L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 12L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 4L) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match40(long name1, long name2, long name3, long name4, int name5, byte name6) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 41;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 37L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 29L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 21L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 13L) == name4
         && JDKUtils.UNSAFE.getInt(bytes, BASE + (long)offset - 5L) == name5
         && bytes[offset - 1] == name6) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match41(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 42;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 38L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 30L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 22L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 14L) == name4
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 6L) & 281474976710655L) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match42(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 43;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 39L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 31L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 23L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 15L) == name4
         && (JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 7L) & 72057594037927935L) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean nextIfName4Match43(long name1, long name2, long name3, long name4, long name5) {
      byte[] bytes = this.bytes;
      int offset = this.offset + 44;
      if (offset < this.end
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 40L) == name1
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 32L) == name2
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 24L) == name3
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 16L) == name4
         && JDKUtils.UNSAFE.getLong(bytes, BASE + (long)offset - 8L) == name5) {
         this.offset = offset;
         return true;
      } else {
         return false;
      }
   }

   static int getInt(byte[] bytes, int offset) {
      int int32Value = JDKUtils.UNSAFE.getInt(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)offset);
      return JDKUtils.BIG_ENDIAN ? int32Value : Integer.reverseBytes(int32Value);
   }

   static {
      byte[] bytes = new byte[256];

      for (int i = -16; i < 47; i++) {
         bytes[i & 0xFF] = 1;
      }

      for (int i = 48; i < 63; i++) {
         bytes[i & 0xFF] = 2;
      }

      for (int i = 64; i < 71; i++) {
         bytes[i & 0xFF] = 3;
      }

      for (int i = -40; i < -17; i++) {
         bytes[i & 0xFF] = 1;
      }

      for (int i = -56; i < -41; i++) {
         bytes[i & 0xFF] = 2;
      }

      for (int i = -64; i < -57; i++) {
         bytes[i & 0xFF] = 3;
      }

      for (int i = 73; i < 120; i++) {
         bytes[i & 0xFF] = (byte)(i - 73 + 1);
      }

      bytes[148] = 1;
      bytes[73] = 1;
      bytes[175] = 1;
      bytes[176] = 1;
      bytes[177] = 1;
      bytes[189] = 2;
      bytes[188] = 3;
      bytes[72] = 5;
      bytes[172] = 5;
      bytes[183] = 5;
      bytes[191] = 5;
      bytes[190] = 9;
      bytes[171] = 9;
      bytes[181] = 9;
      bytes[121] = -1;
      bytes[122] = -1;
      bytes[123] = -1;
      bytes[124] = -1;
      bytes[125] = -1;
      FIXED_TYPE_SIZE = bytes;
   }
}
