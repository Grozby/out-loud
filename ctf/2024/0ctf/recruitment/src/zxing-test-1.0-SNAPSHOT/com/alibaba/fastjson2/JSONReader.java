package com.alibaba.fastjson2;

import com.alibaba.fastjson2.filter.ContextAutoTypeBeforeHandler;
import com.alibaba.fastjson2.filter.ExtraProcessor;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.reader.ObjectReaderImplObject;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.reader.ValueConsumer;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.ReferenceKey;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.util.Wrapper;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class JSONReader implements Closeable {
   static final int MAX_EXP = 2047;
   static final byte JSON_TYPE_INT = 1;
   static final byte JSON_TYPE_DEC = 2;
   static final byte JSON_TYPE_STRING = 3;
   static final byte JSON_TYPE_BOOL = 4;
   static final byte JSON_TYPE_NULL = 5;
   static final byte JSON_TYPE_OBJECT = 6;
   static final byte JSON_TYPE_ARRAY = 7;
   static final byte JSON_TYPE_BIG_DEC = 8;
   static final byte JSON_TYPE_INT8 = 9;
   static final byte JSON_TYPE_INT16 = 10;
   static final byte JSON_TYPE_INT64 = 11;
   static final byte JSON_TYPE_FLOAT = 12;
   static final byte JSON_TYPE_DOUBLE = 13;
   static final char EOI = '\u001a';
   static final long SPACE = 4294981376L;
   protected final JSONReader.Context context;
   public final boolean jsonb;
   public final boolean utf8;
   List<JSONReader.ResolveTask> resolveTasks;
   protected int offset;
   protected char ch;
   protected boolean comma;
   protected boolean nameEscape;
   protected boolean valueEscape;
   protected boolean wasNull;
   protected boolean boolValue;
   protected boolean negative;
   protected byte valueType;
   protected short exponent;
   protected short scale;
   protected int mag0;
   protected int mag1;
   protected int mag2;
   protected int mag3;
   protected int level;
   protected String stringValue;
   protected Object complex;
   protected boolean typeRedirect;
   protected char[] doubleChars;

   public final char current() {
      return this.ch;
   }

   public boolean isEnd() {
      return this.ch == 26;
   }

   public byte getType() {
      return -128;
   }

   public boolean isInt() {
      return this.ch == '-' || this.ch == '+' || this.ch >= '0' && this.ch <= '9';
   }

   public abstract boolean isNull();

   public final boolean hasComma() {
      return this.comma;
   }

   public abstract Date readNullOrNewDate();

   public abstract boolean nextIfNull();

   public JSONReader(JSONReader.Context context, boolean jsonb, boolean utf8) {
      this.context = context;
      this.jsonb = jsonb;
      this.utf8 = utf8;
   }

   public final JSONReader.Context getContext() {
      return this.context;
   }

   public final void errorOnNoneSerializable(Class objectClass) {
      if ((this.context.features & JSONReader.Feature.ErrorOnNoneSerializable.mask) != 0L && !Serializable.class.isAssignableFrom(objectClass)) {
         throw new JSONException("not support none-Serializable, class " + objectClass.getName());
      }
   }

   public final boolean isEnabled(JSONReader.Feature feature) {
      return (this.context.features & feature.mask) != 0L;
   }

   public final Locale getLocale() {
      return this.context.getLocale();
   }

   public final ZoneId getZoneId() {
      return this.context.getZoneId();
   }

   public final long features(long features) {
      return this.context.features | features;
   }

   public abstract int getRawInt();

   public abstract long getRawLong();

   public abstract boolean nextIfName4Match2();

   public boolean nextIfValue4Match2() {
      return false;
   }

   public abstract boolean nextIfName4Match3();

   public boolean nextIfValue4Match3() {
      return false;
   }

   public abstract boolean nextIfName4Match4(byte var1);

   public boolean nextIfValue4Match4(byte c4) {
      return false;
   }

   public abstract boolean nextIfName4Match5(int var1);

   public boolean nextIfValue4Match5(byte c4, byte c5) {
      return false;
   }

   public abstract boolean nextIfName4Match6(int var1);

   public boolean nextIfValue4Match6(int name1) {
      return false;
   }

   public abstract boolean nextIfName4Match7(int var1);

   public boolean nextIfValue4Match7(int name1) {
      return false;
   }

   public abstract boolean nextIfName4Match8(int var1, byte var2);

   public boolean nextIfValue4Match8(int name1, byte c8) {
      return false;
   }

   public abstract boolean nextIfName4Match9(long var1);

   public boolean nextIfValue4Match9(int name1, byte c8, byte c9) {
      return false;
   }

   public abstract boolean nextIfName4Match10(long var1);

   public boolean nextIfValue4Match10(long name1) {
      return false;
   }

   public abstract boolean nextIfName4Match11(long var1);

   public boolean nextIfValue4Match11(long name1) {
      return false;
   }

   public abstract boolean nextIfName4Match12(long var1, byte var3);

   public abstract boolean nextIfName4Match13(long var1, int var3);

   public boolean nextIfName4Match14(long name1, int name2) {
      return false;
   }

   public boolean nextIfName4Match15(long name1, int name2) {
      return false;
   }

   public abstract boolean nextIfName4Match16(long var1, int var3, byte var4);

   public abstract boolean nextIfName4Match17(long var1, long var3);

   public abstract boolean nextIfName4Match18(long var1, long var3);

   public boolean nextIfName4Match19(long name1, long name2) {
      return false;
   }

   public abstract boolean nextIfName4Match20(long var1, long var3, byte var5);

   public boolean nextIfName4Match21(long name1, long name2, int name3) {
      return false;
   }

   public abstract boolean nextIfName4Match22(long var1, long var3, int var5);

   public abstract boolean nextIfName4Match23(long var1, long var3, int var5);

   public abstract boolean nextIfName4Match24(long var1, long var3, int var5, byte var6);

   public abstract boolean nextIfName4Match25(long var1, long var3, long var5);

   public abstract boolean nextIfName4Match26(long var1, long var3, long var5);

   public abstract boolean nextIfName4Match27(long var1, long var3, long var5);

   public abstract boolean nextIfName4Match28(long var1, long var3, long var5, byte var7);

   public abstract boolean nextIfName4Match29(long var1, long var3, long var5, int var7);

   public abstract boolean nextIfName4Match30(long var1, long var3, long var5, int var7);

   public abstract boolean nextIfName4Match31(long var1, long var3, long var5, int var7);

   public abstract boolean nextIfName4Match32(long var1, long var3, long var5, int var7, byte var8);

   public abstract boolean nextIfName4Match33(long var1, long var3, long var5, long var7);

   public abstract boolean nextIfName4Match34(long var1, long var3, long var5, long var7);

   public abstract boolean nextIfName4Match35(long var1, long var3, long var5, long var7);

   public abstract boolean nextIfName4Match36(long var1, long var3, long var5, long var7, byte var9);

   public abstract boolean nextIfName4Match37(long var1, long var3, long var5, long var7, int var9);

   public abstract boolean nextIfName4Match38(long var1, long var3, long var5, long var7, int var9);

   public abstract boolean nextIfName4Match39(long var1, long var3, long var5, long var7, int var9);

   public abstract boolean nextIfName4Match40(long var1, long var3, long var5, long var7, int var9, byte var10);

   public abstract boolean nextIfName4Match41(long var1, long var3, long var5, long var7, long var9);

   public abstract boolean nextIfName4Match42(long var1, long var3, long var5, long var7, long var9);

   public abstract boolean nextIfName4Match43(long var1, long var3, long var5, long var7, long var9);

   public boolean nextIfName8Match0() {
      return false;
   }

   public boolean nextIfName8Match1() {
      return false;
   }

   public boolean nextIfName8Match2() {
      return false;
   }

   public final void handleResolveTasks(Object root) {
      if (this.resolveTasks != null) {
         Object previous = null;

         for (JSONReader.ResolveTask resolveTask : this.resolveTasks) {
            JSONPath path = resolveTask.reference;
            FieldReader fieldReader = resolveTask.fieldReader;
            Object fieldValue;
            if (path.isPrevious()) {
               fieldValue = previous;
            } else {
               if (!path.isRef()) {
                  throw new JSONException("reference path invalid : " + path);
               }

               path.setReaderContext(this.context);
               if ((this.context.features & JSONReader.Feature.FieldBased.mask) != 0L) {
                  JSONWriter.Context writeContext = JSONFactory.createWriteContext();
                  writeContext.features = writeContext.features | JSONWriter.Feature.FieldBased.mask;
                  path.setWriterContext(writeContext);
               }

               fieldValue = path.eval(root);
               previous = fieldValue;
            }

            Object resolvedName = resolveTask.name;
            Object resolvedObject = resolveTask.object;
            if (resolvedName != null) {
               if (resolvedObject instanceof Map) {
                  Map map = (Map)resolvedObject;
                  if (resolvedName instanceof ReferenceKey) {
                     if (map instanceof LinkedHashMap) {
                        int size = map.size();
                        if (size != 0) {
                           Object[] keys = new Object[size];
                           Object[] values = new Object[size];
                           int index = 0;

                           for (Object o : map.entrySet()) {
                              Entry entry = (Entry)o;
                              Object entryKey = entry.getKey();
                              if (resolvedName == entryKey) {
                                 keys[index] = fieldValue;
                              } else {
                                 keys[index] = entryKey;
                              }

                              values[index++] = entry.getValue();
                           }

                           map.clear();

                           for (int j = 0; j < keys.length; j++) {
                              map.put(keys[j], values[j]);
                           }
                        }
                        continue;
                     }

                     map.put(fieldValue, map.remove(resolvedName));
                     continue;
                  }

                  map.put(resolvedName, fieldValue);
                  continue;
               }

               if (resolvedName instanceof Integer) {
                  if (resolvedObject instanceof List) {
                     int index = (Integer)resolvedName;
                     List list = (List)resolvedObject;
                     if (index == list.size()) {
                        list.add(fieldValue);
                        continue;
                     }

                     if (index < list.size() && list.get(index) == null) {
                        list.set(index, fieldValue);
                        continue;
                     }

                     list.add(index, fieldValue);
                     continue;
                  }

                  if (resolvedObject instanceof Object[]) {
                     int indexx = (Integer)resolvedName;
                     Object[] array = (Object[])resolvedObject;
                     array[indexx] = fieldValue;
                     continue;
                  }

                  if (resolvedObject instanceof Collection) {
                     Collection collection = (Collection)resolvedObject;
                     collection.add(fieldValue);
                     continue;
                  }
               }
            }

            fieldReader.accept(resolvedObject, fieldValue);
         }
      }
   }

   public final ObjectReader getObjectReader(Type type) {
      boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      return this.context.provider.getObjectReader(type, fieldBased);
   }

   public final boolean isSupportSmartMatch() {
      return (this.context.features & JSONReader.Feature.SupportSmartMatch.mask) != 0L;
   }

   public final boolean isInitStringFieldAsEmpty() {
      return (this.context.features & JSONReader.Feature.InitStringFieldAsEmpty.mask) != 0L;
   }

   public final boolean isSupportSmartMatch(long features) {
      return ((this.context.features | features) & JSONReader.Feature.SupportSmartMatch.mask) != 0L;
   }

   public final boolean isSupportBeanArray() {
      return (this.context.features & JSONReader.Feature.SupportArrayToBean.mask) != 0L;
   }

   public final boolean isSupportBeanArray(long features) {
      return ((this.context.features | features) & JSONReader.Feature.SupportArrayToBean.mask) != 0L;
   }

   public final boolean isSupportAutoType(long features) {
      return ((this.context.features | features) & JSONReader.Feature.SupportAutoType.mask) != 0L;
   }

   public final boolean isSupportAutoTypeOrHandler(long features) {
      return ((this.context.features | features) & JSONReader.Feature.SupportAutoType.mask) != 0L || this.context.autoTypeBeforeHandler != null;
   }

   public final boolean isJSONB() {
      return this.jsonb;
   }

   public final boolean isIgnoreNoneSerializable() {
      return (this.context.features & JSONReader.Feature.IgnoreNoneSerializable.mask) != 0L;
   }

   public ObjectReader checkAutoType(Class expectClass, long expectClassHash, long features) {
      return null;
   }

   final char char1(int c) {
      switch (c) {
         case 34:
         case 35:
         case 38:
         case 39:
         case 40:
         case 41:
         case 44:
         case 46:
         case 47:
         case 64:
         case 91:
         case 92:
         case 93:
         case 95:
            return (char)c;
         case 36:
         case 37:
         case 42:
         case 43:
         case 45:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
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
         case 94:
         case 96:
         case 97:
         case 99:
         case 100:
         case 101:
         case 103:
         case 104:
         case 105:
         case 106:
         case 107:
         case 108:
         case 109:
         case 111:
         case 112:
         case 113:
         case 115:
         case 117:
         default:
            throw new JSONException(this.info("unclosed.str '\\" + (char)c));
         case 48:
            return '\u0000';
         case 49:
            return '\u0001';
         case 50:
            return '\u0002';
         case 51:
            return '\u0003';
         case 52:
            return '\u0004';
         case 53:
            return '\u0005';
         case 54:
            return '\u0006';
         case 55:
            return '\u0007';
         case 70:
         case 102:
            return '\f';
         case 98:
            return '\b';
         case 110:
            return '\n';
         case 114:
            return '\r';
         case 116:
            return '\t';
         case 118:
            return '\u000b';
      }
   }

   static char char2(int c1, int c2) {
      return (char)(JSONFactory.DIGITS2[c1] * 16 + JSONFactory.DIGITS2[c2]);
   }

   static char char4(int c1, int c2, int c3, int c4) {
      return (char)(JSONFactory.DIGITS2[c1] * 4096 + JSONFactory.DIGITS2[c2] * 256 + JSONFactory.DIGITS2[c3] * 16 + JSONFactory.DIGITS2[c4]);
   }

   public abstract boolean nextIfObjectStart();

   public abstract boolean nextIfNullOrEmptyString();

   public abstract boolean nextIfObjectEnd();

   public int startArray() {
      if (!this.nextIfArrayStart()) {
         throw new JSONException(this.info("illegal input, expect '[', but " + this.ch));
      } else {
         return Integer.MAX_VALUE;
      }
   }

   public abstract boolean isReference();

   public abstract String readReference();

   public boolean readReference(List list, int i) {
      return this.readReference((Collection)list, i);
   }

   public boolean readReference(Collection list, int i) {
      if (!this.isReference()) {
         return false;
      } else {
         String path = this.readReference();
         if ("..".equals(path)) {
            list.add(list);
         } else {
            this.addResolveTask(list, i, JSONPath.of(path));
         }

         return true;
      }
   }

   public final void addResolveTask(FieldReader fieldReader, Object object, JSONPath path) {
      if (this.resolveTasks == null) {
         this.resolveTasks = new ArrayList<>();
      }

      this.resolveTasks.add(new JSONReader.ResolveTask(fieldReader, object, fieldReader.fieldName, path));
   }

   public final void addResolveTask(Map object, Object key, JSONPath reference) {
      if (this.resolveTasks == null) {
         this.resolveTasks = new ArrayList<>();
      }

      if (object instanceof LinkedHashMap) {
         object.put(key, null);
      }

      this.resolveTasks.add(new JSONReader.ResolveTask(null, object, key, reference));
   }

   public final void addResolveTask(Collection object, int i, JSONPath reference) {
      if (this.resolveTasks == null) {
         this.resolveTasks = new ArrayList<>();
      }

      this.resolveTasks.add(new JSONReader.ResolveTask(null, object, i, reference));
   }

   public final void addResolveTask(Object[] object, int i, JSONPath reference) {
      if (this.resolveTasks == null) {
         this.resolveTasks = new ArrayList<>();
      }

      this.resolveTasks.add(new JSONReader.ResolveTask(null, object, i, reference));
   }

   public boolean isArray() {
      return this.ch == '[';
   }

   public boolean isObject() {
      return this.ch == '{';
   }

   public boolean isNumber() {
      switch (this.ch) {
         case '+':
         case '-':
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
            return true;
         case ',':
         case '.':
         case '/':
         default:
            return false;
      }
   }

   public boolean isString() {
      return this.ch == '"' || this.ch == '\'';
   }

   public void endArray() {
      this.next();
   }

   public abstract boolean nextIfMatch(char var1);

   public abstract boolean nextIfComma();

   public abstract boolean nextIfArrayStart();

   public abstract boolean nextIfArrayEnd();

   public abstract boolean nextIfSet();

   public abstract boolean nextIfInfinity();

   public abstract String readPattern();

   public final int getOffset() {
      return this.offset;
   }

   public abstract void next();

   public abstract long readValueHashCode();

   public long readTypeHashCode() {
      return this.readValueHashCode();
   }

   public abstract long readFieldNameHashCode();

   public abstract long getNameHashCodeLCase();

   public abstract String readFieldName();

   public abstract String getFieldName();

   public final void setTypeRedirect(boolean typeRedirect) {
      this.typeRedirect = typeRedirect;
   }

   public final boolean isTypeRedirect() {
      return this.typeRedirect;
   }

   public abstract long readFieldNameHashCodeUnquote();

   public final String readFieldNameUnquote() {
      if (this.ch == '/') {
         this.skipComment();
      }

      this.readFieldNameHashCodeUnquote();
      String name = this.getFieldName();
      if (name != null && !name.equals("")) {
         return name;
      } else {
         throw new JSONException(this.info("illegal input"));
      }
   }

   public abstract boolean skipName();

   public abstract void skipValue();

   public boolean isBinary() {
      return false;
   }

   public abstract byte[] readHex();

   public byte[] readBinary() {
      if (this.ch == 'x') {
         return this.readHex();
      } else if (this.isString()) {
         String str = this.readString();
         if (str.isEmpty()) {
            return null;
         } else if ((this.context.features & JSONReader.Feature.Base64StringAsByteArray.mask) != 0L) {
            return Base64.getDecoder().decode(str);
         } else {
            throw new JSONException(this.info("not support input " + str));
         }
      } else if (this.nextIfArrayStart()) {
         int index = 0;

         byte[] bytes;
         for (bytes = new byte[64]; this.ch != ']'; bytes[index++] = (byte)this.readInt32Value()) {
            if (index == bytes.length) {
               int oldCapacity = bytes.length;
               int newCapacity = oldCapacity + (oldCapacity >> 1);
               bytes = Arrays.copyOf(bytes, newCapacity);
            }
         }

         this.next();
         this.nextIfComma();
         return Arrays.copyOf(bytes, index);
      } else {
         throw new JSONException(this.info("not support read binary"));
      }
   }

   public abstract int readInt32Value();

   public int[] readInt32ValueArray() {
      if (this.nextIfNull()) {
         return null;
      } else if (this.nextIfArrayStart()) {
         int[] values = new int[8];

         int size;
         for (size = 0; !this.nextIfArrayEnd(); values[size++] = this.readInt32Value()) {
            if (this.isEnd()) {
               throw new JSONException(this.info("input end"));
            }

            if (size == values.length) {
               values = Arrays.copyOf(values, values.length << 1);
            }
         }

         this.nextIfComma();
         int[] array;
         if (size == values.length) {
            array = values;
         } else {
            array = Arrays.copyOf(values, size);
         }

         return array;
      } else if (this.isString()) {
         String str = this.readString();
         if (str.isEmpty()) {
            return null;
         } else {
            throw new JSONException(this.info("not support input " + str));
         }
      } else {
         throw new JSONException(this.info("TODO"));
      }
   }

   public boolean nextIfMatch(byte type) {
      throw new JSONException("UnsupportedOperation");
   }

   public boolean nextIfMatchTypedAny() {
      throw new JSONException("UnsupportedOperation");
   }

   public abstract boolean nextIfMatchIdent(char var1, char var2);

   public abstract boolean nextIfMatchIdent(char var1, char var2, char var3);

   public abstract boolean nextIfMatchIdent(char var1, char var2, char var3, char var4);

   public abstract boolean nextIfMatchIdent(char var1, char var2, char var3, char var4, char var5);

   public abstract boolean nextIfMatchIdent(char var1, char var2, char var3, char var4, char var5, char var6);

   public final Byte readInt8() {
      Integer i = this.readInt32();
      return i == null ? null : i.byteValue();
   }

   public byte readInt8Value() {
      int i = this.readInt32Value();
      return (byte)i;
   }

   public final Short readInt16() {
      Integer i = this.readInt32();
      return i == null ? null : i.shortValue();
   }

   public short readInt16Value() {
      int i = this.readInt32Value();
      return (short)i;
   }

   public abstract Integer readInt32();

   public final int getInt32Value() {
      switch (this.valueType) {
         case 1:
         case 9:
         case 10:
            if (this.mag1 == 0 && this.mag2 == 0 && this.mag3 != Integer.MIN_VALUE) {
               return this.negative ? -this.mag3 : this.mag3;
            } else {
               Number number = this.getNumber();
               if (number instanceof Long) {
                  long longValue = number.longValue();
                  if (longValue >= -2147483648L && longValue <= 2147483647L) {
                     return (int)longValue;
                  }

                  throw new JSONException(this.info("integer overflow " + longValue));
               } else {
                  if (number instanceof BigInteger) {
                     BigInteger bigInt = (BigInteger)number;
                     if ((this.context.features & JSONReader.Feature.NonErrorOnNumberOverflow.mask) != 0L) {
                        return bigInt.intValue();
                     }

                     try {
                        return bigInt.intValueExact();
                     } catch (ArithmeticException var6) {
                        throw this.numberError();
                     }
                  }

                  return number.intValue();
               }
            }
         case 2:
            return this.getNumber().intValue();
         case 3:
            return this.toInt32(this.stringValue);
         case 4:
            return this.boolValue ? 1 : 0;
         case 5:
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("int value not support input null"));
            }

            return 0;
         case 6:
            Number num = this.toNumber((Map)this.complex);
            if (num != null) {
               return num.intValue();
            }

            return 0;
         case 7:
            return this.toInt((List)this.complex);
         case 8:
            try {
               return this.getBigDecimal().intValueExact();
            } catch (ArithmeticException var5) {
               throw this.numberError();
            }
         case 11:
         case 12:
         case 13:
            Number num = this.getNumber();
            long int64 = num.longValue();
            if ((int64 < -2147483648L || int64 > 2147483647L) && (this.context.features & JSONReader.Feature.NonErrorOnNumberOverflow.mask) == 0L) {
               throw new JSONException(this.info("integer overflow " + int64));
            } else {
               return (int)int64;
            }
         default:
            throw new JSONException("TODO : " + this.valueType);
      }
   }

   public final long getInt64Value() {
      switch (this.valueType) {
         case 1:
         case 9:
         case 10:
            if (this.mag1 == 0 && this.mag2 == 0 && this.mag3 != Integer.MIN_VALUE) {
               return this.negative ? (long)(-this.mag3) : (long)this.mag3;
            } else {
               Number number = this.getNumber();
               if (number instanceof BigInteger) {
                  BigInteger bigInt = (BigInteger)number;
                  if ((this.context.features & JSONReader.Feature.NonErrorOnNumberOverflow.mask) != 0L) {
                     return bigInt.longValue();
                  }

                  try {
                     return bigInt.longValueExact();
                  } catch (ArithmeticException var5) {
                     throw this.numberError();
                  }
               }

               return number.longValue();
            }
         case 2:
            return this.getNumber().longValue();
         case 3:
            return this.toInt64(this.stringValue);
         case 4:
            return this.boolValue ? 1L : 0L;
         case 5:
            if ((this.context.features & JSONReader.Feature.ErrorOnNullForPrimitives.mask) != 0L) {
               throw new JSONException(this.info("long value not support input null"));
            }

            return 0L;
         case 6:
            return this.toLong((Map)this.complex);
         case 7:
            return (long)this.toInt((List)this.complex);
         case 8:
            try {
               return this.getBigDecimal().longValueExact();
            } catch (ArithmeticException var4) {
               throw this.numberError();
            }
         case 11:
         case 12:
         case 13:
            return this.getNumber().longValue();
         default:
            throw new JSONException("TODO : " + this.valueType);
      }
   }

   public long[] readInt64ValueArray() {
      if (this.nextIfNull()) {
         return null;
      } else if (this.nextIfArrayStart()) {
         long[] values = new long[8];

         int size;
         for (size = 0; !this.nextIfArrayEnd(); values[size++] = this.readInt64Value()) {
            if (this.isEnd()) {
               throw new JSONException(this.info("input end"));
            }

            if (size == values.length) {
               values = Arrays.copyOf(values, values.length << 1);
            }
         }

         this.nextIfComma();
         long[] array;
         if (size == values.length) {
            array = values;
         } else {
            array = Arrays.copyOf(values, size);
         }

         return array;
      } else if (this.isString()) {
         String str = this.readString();
         if (str.isEmpty()) {
            return null;
         } else {
            throw new JSONException(this.info("not support input " + str));
         }
      } else {
         throw new JSONException(this.info("TODO"));
      }
   }

   public abstract long readInt64Value();

   public abstract Long readInt64();

   public abstract float readFloatValue();

   public Float readFloat() {
      if (this.nextIfNull()) {
         return null;
      } else {
         this.wasNull = false;
         float value = this.readFloatValue();
         return this.wasNull ? null : value;
      }
   }

   public abstract double readDoubleValue();

   public final Double readDouble() {
      if (this.nextIfNull()) {
         return null;
      } else {
         this.wasNull = false;
         double value = this.readDoubleValue();
         return this.wasNull ? null : value;
      }
   }

   public Number readNumber() {
      this.readNumber0();
      return this.getNumber();
   }

   public BigInteger readBigInteger() {
      this.readNumber0();
      return this.getBigInteger();
   }

   public abstract BigDecimal readBigDecimal();

   public abstract UUID readUUID();

   public LocalDate readLocalDate() {
      if (this.nextIfNull()) {
         return null;
      } else if (this.isInt()) {
         long millis = this.readInt64Value();
         if (this.context.formatUnixTime) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         ZonedDateTime zdt = instant.atZone(this.context.getZoneId());
         return zdt.toLocalDate();
      } else {
         if (this.context.dateFormat == null
            || this.context.formatyyyyMMddhhmmss19
            || this.context.formatyyyyMMddhhmmssT19
            || this.context.formatyyyyMMdd8
            || this.context.formatISO8601) {
            int len = this.getStringLength();
            LocalDateTime ldt = null;
            switch (len) {
               case 8: {
                  LocalDate localDate = this.readLocalDate8();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 9: {
                  LocalDate localDate = this.readLocalDate9();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 10: {
                  LocalDate localDate = this.readLocalDate10();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 11: {
                  LocalDate localDate = this.readLocalDate11();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               default:
                  if (len > 20) {
                     ldt = this.readLocalDateTimeX(len);
                  }
                  break;
               case 19:
                  ldt = this.readLocalDateTime19();
                  break;
               case 20:
                  ldt = this.readLocalDateTime20();
            }

            if (ldt != null) {
               return ldt.toLocalDate();
            }
         }

         String str = this.readString();
         if (!str.isEmpty() && !"null".equals(str)) {
            DateTimeFormatter formatter = this.context.getDateFormatter();
            if (formatter != null) {
               return this.context.formatHasHour ? LocalDateTime.parse(str, formatter).toLocalDate() : LocalDate.parse(str, formatter);
            } else if (IOUtils.isNumber(str)) {
               long millis = Long.parseLong(str);
               Instant instant = Instant.ofEpochMilli(millis);
               ZonedDateTime zdt = instant.atZone(this.context.getZoneId());
               return zdt.toLocalDate();
            } else {
               throw new JSONException("not support input : " + str);
            }
         } else {
            return null;
         }
      }
   }

   public LocalDateTime readLocalDateTime() {
      if (this.isInt()) {
         long millis = this.readInt64Value();
         Instant instant = Instant.ofEpochMilli(millis);
         ZonedDateTime zdt = instant.atZone(this.context.getZoneId());
         return zdt.toLocalDateTime();
      } else if (this.isTypeRedirect() && this.nextIfMatchIdent('"', 'v', 'a', 'l', '"')) {
         this.nextIfMatch(':');
         LocalDateTime dateTime = this.readLocalDateTime();
         this.nextIfObjectEnd();
         this.setTypeRedirect(false);
         return dateTime;
      } else {
         if (this.context.dateFormat == null
            || this.context.formatyyyyMMddhhmmss19
            || this.context.formatyyyyMMddhhmmssT19
            || this.context.formatyyyyMMdd8
            || this.context.formatISO8601) {
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
               case 12:
               case 13:
               case 14:
               case 15:
               default:
                  break;
               case 16:
                  return this.readLocalDateTime16();
               case 17:
                  LocalDateTime ldtxxxx = this.readLocalDateTime17();
                  if (ldtxxxx != null) {
                     return ldtxxxx;
                  }
                  break;
               case 18:
                  LocalDateTime ldtxxx = this.readLocalDateTime18();
                  if (ldtxxx != null) {
                     return ldtxxx;
                  }
                  break;
               case 19:
                  LocalDateTime ldtxx = this.readLocalDateTime19();
                  if (ldtxx != null) {
                     return ldtxx;
                  }
                  break;
               case 20:
                  LocalDateTime ldtx = this.readLocalDateTime20();
                  if (ldtx != null) {
                     return ldtx;
                  }

                  ZonedDateTime zdt = this.readZonedDateTimeX(len);
                  if (zdt != null) {
                     return zdt.toLocalDateTime();
                  }
                  break;
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
                  }

                  ZonedDateTime zdt = this.readZonedDateTimeX(len);
                  if (zdt != null) {
                     ZoneId contextZoneId = this.context.getZoneId();
                     if (!zdt.getZone().equals(contextZoneId)) {
                        ldt = zdt.toInstant().atZone(contextZoneId).toLocalDateTime();
                     } else {
                        ldt = zdt.toLocalDateTime();
                     }

                     return ldt;
                  }
            }
         }

         String str = this.readString();
         if (!str.isEmpty() && !"null".equals(str)) {
            DateTimeFormatter formatter = this.context.getDateFormatter();
            if (formatter != null) {
               return !this.context.formatHasHour ? LocalDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN) : LocalDateTime.parse(str, formatter);
            } else if (IOUtils.isNumber(str)) {
               long millis = Long.parseLong(str);
               if (this.context.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               return LocalDateTime.ofInstant(instant, this.context.getZoneId());
            } else if (str.startsWith("/Date(") && str.endsWith(")/")) {
               String dotnetDateStr = str.substring(6, str.length() - 2);
               int i = dotnetDateStr.indexOf(43);
               if (i == -1) {
                  i = dotnetDateStr.indexOf(45);
               }

               if (i != -1) {
                  dotnetDateStr = dotnetDateStr.substring(0, i);
               }

               long millis = Long.parseLong(dotnetDateStr);
               Instant instant = Instant.ofEpochMilli(millis);
               return LocalDateTime.ofInstant(instant, this.context.getZoneId());
            } else if ("0000-00-00 00:00:00".equals(str)) {
               this.wasNull = true;
               return null;
            } else {
               throw new JSONException(this.info("read LocalDateTime error " + str));
            }
         } else {
            this.wasNull = true;
            return null;
         }
      }
   }

   public abstract OffsetDateTime readOffsetDateTime();

   public ZonedDateTime readZonedDateTime() {
      if (this.isInt()) {
         long millis = this.readInt64Value();
         if (this.context.formatUnixTime) {
            millis *= 1000L;
         }

         Instant instant = Instant.ofEpochMilli(millis);
         return instant.atZone(this.context.getZoneId());
      } else if (!this.isString()) {
         if (this.nextIfNull()) {
            return null;
         } else {
            throw new JSONException("TODO : " + this.ch);
         }
      } else {
         if (this.context.dateFormat == null
            || this.context.formatyyyyMMddhhmmss19
            || this.context.formatyyyyMMddhhmmssT19
            || this.context.formatyyyyMMdd8
            || this.context.formatISO8601) {
            int len = this.getStringLength();
            LocalDateTime ldt = null;
            switch (len) {
               case 8: {
                  LocalDate localDate = this.readLocalDate8();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 9: {
                  LocalDate localDate = this.readLocalDate9();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 10: {
                  LocalDate localDate = this.readLocalDate10();
                  ldt = localDate == null ? null : LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 11: {
                  LocalDate localDate = this.readLocalDate11();
                  ldt = LocalDateTime.of(localDate, LocalTime.MIN);
                  break;
               }
               case 12:
               case 13:
               case 14:
               case 15:
               default:
                  ZonedDateTime zdt = this.readZonedDateTimeX(len);
                  if (zdt != null) {
                     return zdt;
                  }
                  break;
               case 16:
                  ldt = this.readLocalDateTime16();
                  break;
               case 17:
                  ldt = this.readLocalDateTime17();
                  break;
               case 18:
                  ldt = this.readLocalDateTime18();
                  break;
               case 19:
                  ldt = this.readLocalDateTime19();
                  break;
               case 20:
                  ldt = this.readLocalDateTime20();
            }

            if (ldt != null) {
               return ZonedDateTime.ofLocal(ldt, this.context.getZoneId(), null);
            }
         }

         String str = this.readString();
         if (!str.isEmpty() && !"null".equals(str)) {
            DateTimeFormatter formatter = this.context.getDateFormatter();
            if (formatter != null) {
               if (!this.context.formatHasHour) {
                  LocalDate localDate = LocalDate.parse(str, formatter);
                  return ZonedDateTime.of(localDate, LocalTime.MIN, this.context.getZoneId());
               } else {
                  LocalDateTime localDateTime = LocalDateTime.parse(str, formatter);
                  return ZonedDateTime.of(localDateTime, this.context.getZoneId());
               }
            } else if (IOUtils.isNumber(str)) {
               long millis = Long.parseLong(str);
               if (this.context.formatUnixTime) {
                  millis *= 1000L;
               }

               Instant instant = Instant.ofEpochMilli(millis);
               return instant.atZone(this.context.getZoneId());
            } else {
               return ZonedDateTime.parse(str);
            }
         } else {
            return null;
         }
      }
   }

   public abstract OffsetTime readOffsetTime();

   public Calendar readCalendar() {
      if (this.isString()) {
         long millis = this.readMillisFromString();
         if (millis == 0L && this.wasNull) {
            return null;
         } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            return calendar;
         }
      } else if (this.readIfNull()) {
         return null;
      } else {
         long millis = this.readInt64Value();
         if (this.context.formatUnixTime) {
            millis *= 1000L;
         }

         Calendar calendar = Calendar.getInstance();
         calendar.setTimeInMillis(millis);
         return calendar;
      }
   }

   public Date readDate() {
      if (this.isInt()) {
         long millis = this.readInt64Value();
         return new Date(millis);
      } else if (this.readIfNull()) {
         return null;
      } else if (this.nextIfNullOrEmptyString()) {
         return null;
      } else if (this.current() == 'n') {
         return this.readNullOrNewDate();
      } else {
         long millis;
         if (this.isTypeRedirect() && this.nextIfMatchIdent('"', 'v', 'a', 'l', '"')) {
            this.nextIfMatch(':');
            millis = this.readInt64Value();
            this.nextIfObjectEnd();
            this.setTypeRedirect(false);
         } else {
            if (this.isObject()) {
               JSONObject object = this.readJSONObject();
               Object date = object.get("$date");
               if (date instanceof String) {
                  return DateUtils.parseDate((String)date, this.context.getZoneId());
               }

               return TypeUtils.toDate(object);
            }

            millis = this.readMillisFromString();
         }

         return millis == 0L && this.wasNull ? null : new Date(millis);
      }
   }

   public LocalTime readLocalTime() {
      if (this.nextIfNull()) {
         return null;
      } else if (this.isInt()) {
         long millis = this.readInt64Value();
         Instant instant = Instant.ofEpochMilli(millis);
         ZonedDateTime zdt = instant.atZone(this.context.getZoneId());
         return zdt.toLocalTime();
      } else {
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
               String str = this.readString();
               if (!str.isEmpty() && !"null".equals(str)) {
                  if (IOUtils.isNumber(str)) {
                     long millis = Long.parseLong(str);
                     Instant instant = Instant.ofEpochMilli(millis);
                     ZonedDateTime zdt = instant.atZone(this.context.getZoneId());
                     return zdt.toLocalTime();
                  }

                  throw new JSONException("not support len : " + str);
               }

               return null;
            case 18:
               return this.readLocalTime18();
            case 19:
               return this.readLocalDateTime19().toLocalTime();
            case 20:
               return this.readLocalDateTime20().toLocalTime();
         }
      }
   }

   protected abstract int getStringLength();

   public boolean isDate() {
      return false;
   }

   public Instant readInstant() {
      if (this.nextIfNull()) {
         return null;
      } else if (this.isNumber()) {
         long millis = this.readInt64Value();
         if (this.context.formatUnixTime) {
            millis *= 1000L;
         }

         return Instant.ofEpochMilli(millis);
      } else if (this.isObject()) {
         return (Instant)this.getObjectReader(Instant.class).createInstance(this.readObject(), 0L);
      } else {
         ZonedDateTime zdt = this.readZonedDateTime();
         return zdt == null ? null : Instant.ofEpochSecond(zdt.toEpochSecond(), (long)zdt.toLocalTime().getNano());
      }
   }

   public final long readMillisFromString() {
      this.wasNull = false;
      String format = this.context.dateFormat;
      if (format == null
         || this.context.formatyyyyMMddhhmmss19
         || this.context.formatyyyyMMddhhmmssT19
         || this.context.formatyyyyMMdd8
         || this.context.formatISO8601) {
         int len = this.getStringLength();
         LocalDateTime ldt = null;
         switch (len) {
            case 8:
               LocalDate localDatexx = this.readLocalDate8();
               if (localDatexx == null) {
                  throw new JSONException("TODO : " + this.readString());
               }

               ldt = LocalDateTime.of(localDatexx, LocalTime.MIN);
               break;
            case 9:
               LocalDate localDatex = this.readLocalDate9();
               if (localDatex != null) {
                  ldt = LocalDateTime.of(localDatex, LocalTime.MIN);
               }
               break;
            case 10:
               LocalDate localDatex = this.readLocalDate10();
               if (localDatex == null) {
                  String str = this.readString();
                  if ("0000-00-00".equals(str)) {
                     this.wasNull = true;
                     return 0L;
                  }

                  if (IOUtils.isNumber(str)) {
                     return Long.parseLong(str);
                  }

                  throw new JSONException("TODO : " + str);
               }

               ldt = LocalDateTime.of(localDatex, LocalTime.MIN);
               break;
            case 11:
               LocalDate localDate = this.readLocalDate11();
               if (localDate != null) {
                  ldt = LocalDateTime.of(localDate, LocalTime.MIN);
               }
               break;
            case 12:
               ldt = this.readLocalDateTime12();
            case 13:
            case 15:
            default:
               break;
            case 14:
               ldt = this.readLocalDateTime14();
               break;
            case 16:
               ldt = this.readLocalDateTime16();
               break;
            case 17:
               ldt = this.readLocalDateTime17();
               break;
            case 18:
               ldt = this.readLocalDateTime18();
               break;
            case 19:
               long millis = this.readMillis19();
               if (millis != 0L || !this.wasNull) {
                  return millis;
               }

               ldt = this.readLocalDateTime19();
               break;
            case 20:
               ldt = this.readLocalDateTime20();
         }

         ZonedDateTime zdt = null;
         if (ldt != null) {
            zdt = ZonedDateTime.ofLocal(ldt, this.context.getZoneId(), null);
         } else if (len >= 20) {
            zdt = this.readZonedDateTimeX(len);
            if (zdt == null && len >= 32 && len <= 35) {
               String strx = this.readString();
               zdt = DateUtils.parseZonedDateTime(strx, null);
            }
         }

         if (zdt != null) {
            long seconds = zdt.toEpochSecond();
            int nanos = zdt.toLocalTime().getNano();
            if (seconds < 0L && nanos > 0) {
               long millis = (seconds + 1L) * 1000L;
               long adjustment = (long)(nanos / 1000000 - 1000);
               return millis + adjustment;
            }

            long millis = seconds * 1000L;
            return millis + (long)(nanos / 1000000);
         }
      }

      String strx = this.readString();
      if (strx.isEmpty() || "null".equals(strx)) {
         this.wasNull = true;
         return 0L;
      } else if (this.context.formatMillis || this.context.formatUnixTime) {
         long millis = Long.parseLong(strx);
         if (this.context.formatUnixTime) {
            millis *= 1000L;
         }

         return millis;
      } else if (format != null && !format.isEmpty()) {
         if (!"yyyy-MM-dd HH:mm:ss".equals(format)) {
            if ("yyyy-MM-dd HH:mm:ss.SSS".equals(format)
               && strx.length() == 19
               && strx.charAt(4) == '-'
               && strx.charAt(7) == '-'
               && strx.charAt(10) == ' '
               && strx.charAt(13) == ':'
               && strx.charAt(16) == ':') {
               return DateUtils.parseMillis19(strx, this.context.getZoneId());
            } else {
               SimpleDateFormat utilFormat = new SimpleDateFormat(format);

               try {
                  return utilFormat.parse(strx).getTime();
               } catch (ParseException var13) {
                  throw new JSONException("parse date error, " + strx + ", expect format " + utilFormat.toPattern());
               }
            }
         } else {
            return (strx.length() < 4 || strx.charAt(4) != '-') && IOUtils.isNumber(strx)
               ? Long.parseLong(strx)
               : DateUtils.parseMillis19(strx, this.context.getZoneId());
         }
      } else if ("0000-00-00T00:00:00".equals(strx) || "0001-01-01T00:00:00+08:00".equals(strx)) {
         return 0L;
      } else if (strx.startsWith("/Date(") && strx.endsWith(")/")) {
         String dotnetDateStr = strx.substring(6, strx.length() - 2);
         int i = dotnetDateStr.indexOf(43);
         if (i == -1) {
            i = dotnetDateStr.indexOf(45);
         }

         if (i != -1) {
            dotnetDateStr = dotnetDateStr.substring(0, i);
         }

         return Long.parseLong(dotnetDateStr);
      } else if (IOUtils.isNumber(strx)) {
         return Long.parseLong(strx);
      } else {
         throw new JSONException(this.info("format " + format + " not support, input " + strx));
      }
   }

   protected abstract LocalDateTime readLocalDateTime12();

   protected abstract LocalDateTime readLocalDateTime14();

   protected abstract LocalDateTime readLocalDateTime16();

   protected abstract LocalDateTime readLocalDateTime17();

   protected abstract LocalDateTime readLocalDateTime18();

   protected abstract LocalDateTime readLocalDateTime19();

   protected abstract LocalDateTime readLocalDateTime20();

   public abstract long readMillis19();

   protected abstract LocalDateTime readLocalDateTimeX(int var1);

   protected abstract LocalTime readLocalTime5();

   protected abstract LocalTime readLocalTime6();

   protected abstract LocalTime readLocalTime7();

   protected abstract LocalTime readLocalTime8();

   protected abstract LocalTime readLocalTime9();

   protected abstract LocalTime readLocalTime10();

   protected abstract LocalTime readLocalTime11();

   protected abstract LocalTime readLocalTime12();

   protected abstract LocalTime readLocalTime18();

   protected abstract LocalDate readLocalDate8();

   protected abstract LocalDate readLocalDate9();

   protected abstract LocalDate readLocalDate10();

   protected abstract LocalDate readLocalDate11();

   protected abstract ZonedDateTime readZonedDateTimeX(int var1);

   public void readNumber(ValueConsumer consumer, boolean quoted) {
      this.readNumber0();
      Number number = this.getNumber();
      consumer.accept(number);
   }

   public void readString(ValueConsumer consumer, boolean quoted) {
      String str = this.readString();
      if (quoted) {
         consumer.accept(JSON.toJSONString(str));
      } else {
         consumer.accept(str);
      }
   }

   protected abstract void readNumber0();

   public abstract String readString();

   public String[] readStringArray() {
      if (this.ch == 'n' && this.nextIfNull()) {
         return null;
      } else if (!this.nextIfArrayStart()) {
         if (this.ch != '"' && this.ch != '\'') {
            throw new JSONException(this.info("not support input"));
         } else {
            String str = this.readString();
            if (str.isEmpty()) {
               return null;
            } else {
               throw new JSONException(this.info("not support input " + str));
            }
         }
      } else {
         String[] values = null;

         int size;
         for (size = 0; !this.nextIfArrayEnd(); values[size++] = this.readString()) {
            if (this.isEnd()) {
               throw new JSONException(this.info("input end"));
            }

            if (values == null) {
               values = new String[16];
            } else if (size == values.length) {
               values = Arrays.copyOf(values, values.length << 1);
            }
         }

         if (values == null) {
            values = new String[0];
         }

         this.nextIfComma();
         return values.length == size ? values : Arrays.copyOf(values, size);
      }
   }

   public char readCharValue() {
      String str = this.readString();
      if (str != null && !str.isEmpty()) {
         return str.charAt(0);
      } else {
         this.wasNull = true;
         return '\u0000';
      }
   }

   public Character readCharacter() {
      String str = this.readString();
      if (str != null && !str.isEmpty()) {
         return str.charAt(0);
      } else {
         this.wasNull = true;
         return '\u0000';
      }
   }

   public abstract void readNull();

   public abstract boolean readIfNull();

   public abstract String getString();

   public boolean wasNull() {
      return this.wasNull;
   }

   public <T> T read(Type type) {
      boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = this.context.provider.getObjectReader(type, fieldBased);
      return (T)objectReader.readObject(this, null, null, 0L);
   }

   public final void read(List list) {
      if (!this.nextIfArrayStart()) {
         throw new JSONException("illegal input, offset " + this.offset + ", char " + this.ch);
      } else {
         this.level++;
         if (this.level >= this.context.maxLevel) {
            throw new JSONException("level too large : " + this.level);
         } else {
            while (!this.nextIfArrayEnd()) {
               Object item = ObjectReaderImplObject.INSTANCE.readObject(this, null, null, 0L);
               list.add(item);
               this.nextIfComma();
            }

            this.level--;
            this.nextIfComma();
         }
      }
   }

   public final void read(Collection list) {
      if (!this.nextIfArrayStart()) {
         throw new JSONException("illegal input, offset " + this.offset + ", char " + this.ch);
      } else {
         this.level++;
         if (this.level >= this.context.maxLevel) {
            throw new JSONException("level too large : " + this.level);
         } else {
            while (!this.nextIfArrayEnd()) {
               Object item = this.readAny();
               list.add(item);
               this.nextIfComma();
            }

            this.level--;
            this.nextIfComma();
         }
      }
   }

   public final void readObject(Object object, JSONReader.Feature... features) {
      long featuresLong = 0L;

      for (JSONReader.Feature feature : features) {
         featuresLong |= feature.mask;
      }

      this.readObject(object, featuresLong);
   }

   public final void readObject(Object object, long features) {
      if (object == null) {
         throw new JSONException("object is null");
      } else {
         Class objectClass = object.getClass();
         boolean fieldBased = ((this.context.features | features) & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader objectReader = this.context.provider.getObjectReader(objectClass, fieldBased);
         if (objectReader instanceof ObjectReaderBean) {
            ObjectReaderBean objectReaderBean = (ObjectReaderBean)objectReader;
            objectReaderBean.readObject(this, object, features);
         } else {
            if (!(object instanceof Map)) {
               throw new JSONException("read object not support");
            }

            this.read((Map)object, features);
         }
      }
   }

   public void read(Map object, ObjectReader itemReader, long features) {
      this.nextIfObjectStart();
      Map map;
      if (object instanceof Wrapper) {
         map = ((Wrapper)object).unwrap(Map.class);
      } else {
         map = object;
      }

      long contextFeatures = features | this.context.getFeatures();
      int i = 0;

      while (true) {
         if (this.ch == '/') {
            this.skipComment();
         }

         if (this.nextIfObjectEnd()) {
            this.nextIfComma();
            return;
         }

         if (i != 0 && !this.comma) {
            throw new JSONException(this.info());
         }

         String name = this.readFieldName();
         Object value = itemReader.readObject(this, itemReader.getObjectClass(), name, features);
         if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
            Object origin = map.put(name, value);
            if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
               if (origin instanceof Collection) {
                  ((Collection)origin).add(value);
                  map.put(name, origin);
               } else {
                  JSONArray array = JSONArray.of(origin, value);
                  map.put(name, array);
               }
            }
         }

         i++;
      }
   }

   public void read(Map object, long features) {
      if (this.ch == '\'' && (this.context.features & JSONReader.Feature.DisableSingleQuote.mask) != 0L) {
         throw this.notSupportName();
      } else {
         if ((this.ch == '"' || this.ch == '\'') && !this.typeRedirect) {
            String str = this.readString();
            if (str.isEmpty()) {
               return;
            }

            if (str.charAt(0) == '{') {
               JSONReader jsonReader = of(str, this.context);

               try {
                  jsonReader.readObject(object, features);
               } catch (Throwable var16) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return;
            }
         }

         boolean match = this.nextIfObjectStart();
         boolean typeRedirect = false;
         if (!match) {
            if (!(typeRedirect = this.isTypeRedirect())) {
               if (this.isString()) {
                  String strx = this.readString();
                  if (strx.isEmpty()) {
                     return;
                  }
               }

               throw new JSONException(this.info());
            }

            this.setTypeRedirect(false);
         }

         Map map;
         if (object instanceof Wrapper) {
            map = ((Wrapper)object).unwrap(Map.class);
         } else {
            map = object;
         }

         long contextFeatures = features | this.context.getFeatures();
         int i = 0;

         while (true) {
            if (this.ch == '/') {
               this.skipComment();
            }

            if (this.nextIfObjectEnd()) {
               this.nextIfComma();
               return;
            }

            if (i != 0 && !this.comma) {
               throw new JSONException(this.info());
            }

            Object name;
            if (!match && !typeRedirect) {
               name = this.getFieldName();
               match = true;
            } else if ((this.ch < '0' || this.ch > '9') && this.ch != '-') {
               name = this.readFieldName();
            } else {
               name = null;
            }

            if (name == null) {
               if (this.isNumber()) {
                  name = this.readNumber();
                  if ((this.context.features & JSONReader.Feature.NonStringKeyAsString.mask) != 0L) {
                     name = name.toString();
                  }

                  if (this.comma) {
                     throw new JSONException(this.info("syntax error, illegal key-value"));
                  }
               } else {
                  if ((this.context.features & JSONReader.Feature.AllowUnQuotedFieldNames.mask) == 0L) {
                     throw new JSONException(this.info("not allow unquoted fieldName"));
                  }

                  name = this.readFieldNameUnquote();
               }

               if (this.ch == ':') {
                  this.next();
               }
            }

            label176:
            if (this.isReference()) {
               String reference = this.readReference();
               Object value = null;
               if ("..".equals(reference)) {
                  value = map;
               } else {
                  JSONPath jsonPath;
                  try {
                     jsonPath = JSONPath.of(reference);
                  } catch (Exception var17) {
                     map.put(name, new JSONObject().fluentPut("$ref", reference));
                     break label176;
                  }

                  this.addResolveTask(map, name, jsonPath);
               }

               map.put(name, value);
            } else {
               label210: {
                  this.comma = false;
                  Object value;
                  switch (this.ch) {
                     case '"':
                     case '\'':
                        value = this.readString();
                        break;
                     case '#':
                     case '$':
                     case '%':
                     case '&':
                     case '(':
                     case ')':
                     case '*':
                     case ',':
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
                     case 'J':
                     case 'K':
                     case 'L':
                     case 'M':
                     case 'N':
                     case 'O':
                     case 'P':
                     case 'Q':
                     case 'R':
                     case 'T':
                     case 'U':
                     case 'V':
                     case 'W':
                     case 'X':
                     case 'Y':
                     case 'Z':
                     case '\\':
                     case ']':
                     case '^':
                     case '_':
                     case '`':
                     case 'a':
                     case 'b':
                     case 'c':
                     case 'd':
                     case 'e':
                     case 'g':
                     case 'h':
                     case 'i':
                     case 'j':
                     case 'k':
                     case 'l':
                     case 'm':
                     case 'o':
                     case 'p':
                     case 'q':
                     case 'r':
                     case 's':
                     case 'u':
                     case 'v':
                     case 'w':
                     case 'y':
                     case 'z':
                     default:
                        throw new JSONException("FASTJSON2.0.53error, offset " + this.offset + ", char " + this.ch);
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
                        value = this.readNumber();
                        break;
                     case '/':
                        this.next();
                        if (this.ch != '/') {
                           throw new JSONException("FASTJSON2.0.53input not support " + this.ch + ", offset " + this.offset);
                        }

                        this.skipComment();
                        break label210;
                     case 'I':
                        if (!this.nextIfInfinity()) {
                           throw new JSONException("FASTJSON2.0.53error, offset " + this.offset + ", char " + this.ch);
                        }

                        value = Double.POSITIVE_INFINITY;
                        break;
                     case 'S':
                        if (!this.nextIfSet()) {
                           throw new JSONException("FASTJSON2.0.53error, offset " + this.offset + ", char " + this.ch);
                        }

                        value = this.read(HashSet.class);
                        break;
                     case '[':
                        value = this.readArray();
                        break;
                     case 'f':
                     case 't':
                        value = this.readBoolValue();
                        break;
                     case 'n':
                        value = this.readNullOrNewDate();
                        break;
                     case 'x':
                        value = this.readBinary();
                        break;
                     case '{':
                        if (typeRedirect) {
                           value = ObjectReaderImplObject.INSTANCE.readObject(this, null, name, features);
                        } else {
                           value = this.readObject();
                        }
                  }

                  if ((value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L)
                     && (
                        i != 0
                           || (contextFeatures & JSONReader.Feature.SupportAutoType.mask) == 0L
                           || !name.equals("@type")
                           || !object.getClass().getName().equals(value)
                     )) {
                     Object origin = map.put(name, value);
                     if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                        if (origin instanceof Collection) {
                           ((Collection)origin).add(value);
                           map.put(name, origin);
                        } else {
                           JSONArray array = JSONArray.of(origin, value);
                           map.put(name, array);
                        }
                     }
                  }
               }
            }

            i++;
         }
      }
   }

   public final void read(Map object, Type keyType, Type valueType, long features) {
      boolean match = this.nextIfObjectStart();
      if (!match) {
         throw new JSONException("illegal input， offset " + this.offset + ", char " + this.ch);
      } else {
         ObjectReader keyReader = this.context.getObjectReader(keyType);
         ObjectReader valueReader = this.context.getObjectReader(valueType);
         long contextFeatures = features | this.context.getFeatures();
         int i = 0;

         while (true) {
            if (this.ch == '/') {
               this.skipComment();
            }

            if (this.nextIfObjectEnd()) {
               this.nextIfComma();
               return;
            }

            if (i != 0 && !this.comma) {
               throw new JSONException(this.info());
            }

            Object name;
            if (keyType == String.class) {
               name = this.readFieldName();
            } else {
               name = keyReader.readObject(this, null, null, 0L);
               this.nextIfMatch(':');
            }

            Object value = valueReader.readObject(this, null, null, 0L);
            if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
               Object origin = object.put(name, value);
               if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                  if (origin instanceof Collection) {
                     ((Collection)origin).add(value);
                     object.put(name, origin);
                  } else {
                     JSONArray array = JSONArray.of(origin, value);
                     object.put(name, array);
                  }
               }
            }

            i++;
         }
      }
   }

   public <T> T read(Class<T> type) {
      boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = this.context.provider.getObjectReader(type, fieldBased);
      return (T)objectReader.readObject(this, null, null, 0L);
   }

   public Map<String, Object> readObject() {
      this.nextIfObjectStart();
      this.level++;
      if (this.level >= this.context.maxLevel) {
         throw new JSONException("level too large : " + this.level);
      } else {
         Map object;
         if (this.context.objectSupplier == null) {
            if ((this.context.features & JSONReader.Feature.UseNativeObject.mask) != 0L) {
               object = new HashMap();
            } else {
               object = new JSONObject();
            }
         } else {
            object = this.context.objectSupplier.get();
         }

         for (int i = 0; this.ch != '}'; i++) {
            Object name = this.readFieldName();
            if (name == null) {
               if (this.ch == 26) {
                  throw new JSONException("input end");
               }

               if (this.ch != '-' && (this.ch < '0' || this.ch > '9')) {
                  if (this.ch == '{') {
                     name = this.readObject();
                  } else if (this.ch == '[') {
                     name = this.readArray();
                  } else {
                     name = this.readFieldNameUnquote();
                  }
               } else {
                  this.readNumber0();
                  name = this.getNumber();
               }

               this.nextIfMatch(':');
            }

            if (i == 0 && (this.context.features & JSONReader.Feature.ErrorOnNotSupportAutoType.mask) != 0L && "@type".equals(name)) {
               String typeName = this.readString();
               throw new JSONException("autoType not support : " + typeName);
            }

            Object val;
            switch (this.ch) {
               case '"':
               case '\'':
                  val = this.readString();
                  break;
               case '#':
               case '$':
               case '%':
               case '&':
               case '(':
               case ')':
               case '*':
               case ',':
               case '.':
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
               case 'J':
               case 'K':
               case 'L':
               case 'M':
               case 'N':
               case 'O':
               case 'P':
               case 'Q':
               case 'R':
               case 'T':
               case 'U':
               case 'V':
               case 'W':
               case 'X':
               case 'Y':
               case 'Z':
               case '\\':
               case ']':
               case '^':
               case '_':
               case '`':
               case 'a':
               case 'b':
               case 'c':
               case 'd':
               case 'e':
               case 'g':
               case 'h':
               case 'i':
               case 'j':
               case 'k':
               case 'l':
               case 'm':
               case 'o':
               case 'p':
               case 'q':
               case 'r':
               case 's':
               case 'u':
               case 'v':
               case 'w':
               case 'x':
               case 'y':
               case 'z':
               default:
                  throw new JSONException(this.info("illegal input " + this.ch));
               case '+':
               case '-':
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
                  this.readNumber0();
                  val = this.getNumber();
                  break;
               case '/':
                  this.skipComment();
                  continue;
               case 'I':
                  if (!this.nextIfInfinity()) {
                     throw new JSONException(this.info("illegal input " + this.ch));
                  }

                  val = Double.POSITIVE_INFINITY;
                  break;
               case 'S':
                  if (!this.nextIfSet()) {
                     throw new JSONException(this.info("illegal input " + this.ch));
                  }

                  val = this.read(Set.class);
                  break;
               case '[':
                  val = this.readArray();
                  break;
               case 'f':
               case 't':
                  val = this.readBoolValue();
                  break;
               case 'n':
                  val = this.readNullOrNewDate();
                  break;
               case '{':
                  if (this.isReference()) {
                     this.addResolveTask(object, name, JSONPath.of(this.readReference()));
                     val = null;
                  } else {
                     val = this.readObject();
                  }
            }

            if (val != null || (this.context.features & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
               Object origin = object.put(name, val);
               if (origin != null && (this.context.features & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                  if (origin instanceof Collection) {
                     ((Collection)origin).add(val);
                     object.put(name, origin);
                  } else {
                     JSONArray array = JSONArray.of(origin, val);
                     object.put(name, array);
                  }
               }
            }
         }

         this.next();
         if (this.comma = this.ch == ',') {
            this.next();
         }

         this.level--;
         return object;
      }
   }

   public abstract void skipComment();

   public Boolean readBool() {
      if (this.nextIfNull()) {
         return null;
      } else {
         boolean boolValue = this.readBoolValue();
         return !boolValue && this.wasNull ? null : boolValue;
      }
   }

   public abstract boolean readBoolValue();

   public Object readAny() {
      return this.read(Object.class);
   }

   public List readArray(Type itemType) {
      if (this.nextIfNull()) {
         return null;
      } else {
         List list = new ArrayList();
         if (this.ch == '[') {
            this.next();
            boolean fieldBased = (this.context.features & JSONReader.Feature.FieldBased.mask) != 0L;
            ObjectReader objectReader = this.context.provider.getObjectReader(itemType, fieldBased);

            while (!this.nextIfArrayEnd()) {
               int mark = this.offset;
               Object item = objectReader.readObject(this, null, null, 0L);
               if (mark == this.offset || this.ch == '}' || this.ch == 26) {
                  throw new JSONException("illegal input : " + this.ch + ", offset " + this.getOffset());
               }

               list.add(item);
            }
         } else {
            if (this.ch != '"' && this.ch != '\'' && this.ch != '{') {
               throw new JSONException(this.info("syntax error"));
            }

            String str = this.readString();
            if (str != null && !str.isEmpty()) {
               list.add(str);
            }
         }

         if (this.comma = this.ch == ',') {
            this.next();
         }

         return list;
      }
   }

   public List readList(Type[] types) {
      if (this.nextIfNull()) {
         return null;
      } else if (!this.nextIfArrayStart()) {
         throw new JSONException("syntax error : " + this.ch);
      } else {
         int i = 0;
         int max = types.length;
         List list = new ArrayList(max);

         while (!this.nextIfArrayEnd() && i < max) {
            int mark = this.offset;
            Object item = this.read(types[i++]);
            if (mark == this.offset || this.ch == '}' || this.ch == 26) {
               throw new JSONException("illegal input : " + this.ch + ", offset " + this.getOffset());
            }

            list.add(item);
         }

         if (i != max) {
            throw new JSONException(this.info("element length mismatch"));
         } else {
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return list;
         }
      }
   }

   public final Object[] readArray(Type[] types) {
      if (this.nextIfNull()) {
         return null;
      } else if (!this.nextIfArrayStart()) {
         throw new JSONException(this.info("syntax error"));
      } else {
         int i = 0;
         int max = types.length;
         Object[] list = new Object[max];

         while (!this.nextIfArrayEnd() && i < max) {
            int mark = this.offset;
            Object item = this.read(types[i]);
            if (mark == this.offset || this.ch == '}' || this.ch == 26) {
               throw new JSONException("illegal input : " + this.ch + ", offset " + this.getOffset());
            }

            list[i++] = item;
         }

         if (i != max) {
            throw new JSONException(this.info("element length mismatch"));
         } else {
            if (this.comma = this.ch == ',') {
               this.next();
            }

            return list;
         }
      }
   }

   public final void readArray(List list, Type itemType) {
      this.readArray((Collection)list, itemType);
   }

   public final void readArray(Collection list, Type itemType) {
      if (this.nextIfArrayStart()) {
         while (!this.nextIfArrayEnd()) {
            Object item = this.read(itemType);
            list.add(item);
         }
      } else {
         if (this.isString()) {
            String str = this.readString();
            if (itemType == String.class) {
               list.add(str);
            } else {
               Function typeConvert = this.context.getProvider().getTypeConvert(String.class, itemType);
               if (typeConvert == null) {
                  throw new JSONException(this.info("not support input " + str));
               }

               if (str.indexOf(44) != -1) {
                  String[] items = str.split(",");

                  for (String strItem : items) {
                     Object item = typeConvert.apply(strItem);
                     list.add(item);
                  }
               } else {
                  Object item = typeConvert.apply(str);
                  list.add(item);
               }
            }
         } else {
            Object item = this.read(itemType);
            list.add(item);
         }

         if (this.comma = this.ch == ',') {
            this.next();
         }
      }
   }

   public final JSONArray readJSONArray() {
      JSONArray array = new JSONArray();
      this.read((List)array);
      return array;
   }

   public final JSONObject readJSONObject() {
      JSONObject object = new JSONObject();
      this.read(object, 0L);
      return object;
   }

   public List readArray() {
      this.next();
      this.level++;
      if (this.level >= this.context.maxLevel) {
         throw new JSONException("level too large : " + this.level);
      } else {
         int i = 0;
         List<Object> list = null;
         Object first = null;
         Object second = null;

         while (true) {
            label89: {
               Object val;
               switch (this.ch) {
                  case '"':
                  case '\'':
                     val = this.readString();
                     break;
                  case '#':
                  case '$':
                  case '%':
                  case '&':
                  case '(':
                  case ')':
                  case '*':
                  case ',':
                  case '.':
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
                  case '\\':
                  case '^':
                  case '_':
                  case '`':
                  case 'a':
                  case 'b':
                  case 'c':
                  case 'd':
                  case 'e':
                  case 'g':
                  case 'h':
                  case 'i':
                  case 'j':
                  case 'k':
                  case 'l':
                  case 'm':
                  case 'o':
                  case 'p':
                  case 'q':
                  case 'r':
                  case 's':
                  case 'u':
                  case 'v':
                  case 'w':
                  case 'x':
                  case 'y':
                  case 'z':
                  default:
                     throw new JSONException(this.info());
                  case '+':
                  case '-':
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
                     this.readNumber0();
                     val = this.getNumber();
                     break;
                  case '/':
                     this.skipComment();
                     break label89;
                  case '[':
                     val = this.readArray();
                     break;
                  case ']':
                     this.next();
                     if (list == null) {
                        if (this.context.arraySupplier != null) {
                           list = this.context.arraySupplier.get();
                        } else if (this.context.isEnabled(JSONReader.Feature.UseNativeObject)) {
                           list = i == 2 ? new ArrayList<>(2) : new ArrayList<>(1);
                        } else {
                           list = i == 2 ? new JSONArray(2) : new JSONArray(1);
                        }

                        if (i == 1) {
                           this.add(list, 0, first);
                        } else if (i == 2) {
                           this.add(list, 0, first);
                           this.add(list, 1, second);
                        }
                     }

                     if (this.comma = this.ch == ',') {
                        this.next();
                     }

                     this.level--;
                     return list;
                  case 'f':
                  case 't':
                     val = this.readBoolValue();
                     break;
                  case 'n':
                     this.readNull();
                     val = null;
                     break;
                  case '{':
                     if (this.context.autoTypeBeforeHandler != null || (this.context.features & JSONReader.Feature.SupportAutoType.mask) != 0L) {
                        val = ObjectReaderImplObject.INSTANCE.readObject(this, null, null, 0L);
                     } else if (this.isReference()) {
                        val = JSONPath.of(this.readReference());
                     } else {
                        val = this.readObject();
                     }
               }

               if (i == 0) {
                  first = val;
               } else if (i == 1) {
                  second = val;
               } else if (i == 2) {
                  if (this.context.arraySupplier != null) {
                     list = this.context.arraySupplier.get();
                  } else {
                     list = new JSONArray();
                  }

                  this.add(list, 0, first);
                  this.add(list, 1, second);
                  this.add(list, i, val);
               } else {
                  this.add(list, i, val);
               }
            }

            i++;
         }
      }
   }

   private void add(List<Object> list, int i, Object val) {
      if (val instanceof JSONPath) {
         this.addResolveTask(list, i, (JSONPath)val);
         list.add(null);
      } else {
         list.add(val);
      }
   }

   public final BigInteger getBigInteger() {
      Number number = this.getNumber();
      if (number == null) {
         return null;
      } else {
         return number instanceof BigInteger ? (BigInteger)number : BigInteger.valueOf(number.longValue());
      }
   }

   public final BigDecimal getBigDecimal() {
      if (this.wasNull) {
         return null;
      } else {
         switch (this.valueType) {
            case 1:
               if (this.mag1 == 0 && this.mag2 == 0 && this.mag3 >= 0) {
                  return BigDecimal.valueOf(this.negative ? (long)(-this.mag3) : (long)this.mag3);
               } else {
                  int[] mag;
                  if (this.mag0 == 0) {
                     if (this.mag1 == 0) {
                        long v3 = (long)this.mag3 & 4294967295L;
                        long v2 = (long)this.mag2 & 4294967295L;
                        if (v2 <= 2147483647L) {
                           long v23 = (v2 << 32) + v3;
                           return BigDecimal.valueOf(this.negative ? -v23 : v23);
                        }

                        mag = new int[]{this.mag2, this.mag3};
                     } else {
                        mag = new int[]{this.mag1, this.mag2, this.mag3};
                     }
                  } else {
                     mag = new int[]{this.mag0, this.mag1, this.mag2, this.mag3};
                  }

                  int signum = this.negative ? -1 : 1;
                  BigInteger bigInt = JSONReader.BigIntegerCreator.BIG_INTEGER_CREATOR.apply(signum, mag);
                  return new BigDecimal(bigInt);
               }
            case 2:
               BigDecimal decimalx = null;
               if (this.exponent == 0 && this.mag0 == 0 && this.mag1 == 0) {
                  if (this.mag2 == 0 && this.mag3 >= 0) {
                     int unscaledVal = this.negative ? -this.mag3 : this.mag3;
                     decimalx = BigDecimal.valueOf((long)unscaledVal, this.scale);
                  } else {
                     long v3 = (long)this.mag3 & 4294967295L;
                     long v2 = (long)this.mag2 & 4294967295L;
                     if (v2 <= 2147483647L) {
                        long v23 = (v2 << 32) + v3;
                        long unscaledVal = this.negative ? -v23 : v23;
                        decimalx = BigDecimal.valueOf(unscaledVal, this.scale);
                     }
                  }
               }

               if (decimalx == null) {
                  int[] mag = this.mag0 == 0
                     ? (this.mag1 == 0 ? (this.mag2 == 0 ? new int[]{this.mag3} : new int[]{this.mag2, this.mag3}) : new int[]{this.mag1, this.mag2, this.mag3})
                     : new int[]{this.mag0, this.mag1, this.mag2, this.mag3};
                  int signum = this.negative ? -1 : 1;
                  BigInteger bigInt = JSONReader.BigIntegerCreator.BIG_INTEGER_CREATOR.apply(signum, mag);
                  decimalx = new BigDecimal(bigInt, this.scale);
               }

               if (this.exponent != 0) {
                  String doubleStr = decimalx.toPlainString() + "E" + this.exponent;
                  double doubleValue = Double.parseDouble(doubleStr);
                  return TypeUtils.toBigDecimal(doubleValue);
               } else {
                  return decimalx;
               }
            case 3:
               try {
                  return TypeUtils.toBigDecimal(this.stringValue);
               } catch (NumberFormatException var10) {
                  throw new JSONException(this.info("read decimal error, value " + this.stringValue), var10);
               }
            case 4:
               return this.boolValue ? BigDecimal.ONE : BigDecimal.ZERO;
            case 5:
            case 7:
            default:
               throw new JSONException("TODO : " + this.valueType);
            case 6:
               JSONObject object = (JSONObject)this.complex;
               BigDecimal decimal = object.getBigDecimal("value");
               if (decimal == null) {
                  decimal = object.getBigDecimal("$numberDecimal");
               }

               if (decimal != null) {
                  return decimal;
               }

               throw new JSONException("TODO : " + this.valueType);
            case 8:
               return TypeUtils.toBigDecimal(this.stringValue);
         }
      }
   }

   public final Number getNumber() {
      if (this.wasNull) {
         return null;
      } else {
         switch (this.valueType) {
            case 1:
            case 11:
               if (this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 != Integer.MIN_VALUE) {
                  int intValue;
                  if (this.negative) {
                     if (this.mag3 < 0) {
                        long longValue = -((long)this.mag3 & 4294967295L);
                        if ((this.context.features & JSONReader.Feature.UseBigIntegerForInts.mask) != 0L) {
                           return BigInteger.valueOf(longValue);
                        }

                        return longValue;
                     }

                     intValue = -this.mag3;
                  } else {
                     if (this.mag3 < 0) {
                        long longValue = (long)this.mag3 & 4294967295L;
                        if ((this.context.features & JSONReader.Feature.UseBigIntegerForInts.mask) != 0L) {
                           return BigInteger.valueOf(longValue);
                        }

                        return longValue;
                     }

                     intValue = this.mag3;
                  }

                  if ((this.context.features & JSONReader.Feature.UseBigIntegerForInts.mask) != 0L) {
                     return BigInteger.valueOf((long)intValue);
                  } else if ((this.context.features & JSONReader.Feature.UseLongForInts.mask) != 0L) {
                     return (long)intValue;
                  } else {
                     if (this.valueType == 11) {
                        return (long)intValue;
                     }

                     return intValue;
                  }
               } else {
                  int[] magx;
                  if (this.mag0 == 0) {
                     if (this.mag1 == 0) {
                        long v3 = (long)this.mag3 & 4294967295L;
                        long v2 = (long)this.mag2 & 4294967295L;
                        if (v2 <= 2147483647L) {
                           long v23 = (v2 << 32) + v3;
                           long longValue = this.negative ? -v23 : v23;
                           if ((this.context.features & JSONReader.Feature.UseBigIntegerForInts.mask) != 0L) {
                              return BigInteger.valueOf(longValue);
                           }

                           return longValue;
                        }

                        magx = new int[]{this.mag2, this.mag3};
                     } else {
                        magx = new int[]{this.mag1, this.mag2, this.mag3};
                     }
                  } else {
                     magx = new int[]{this.mag0, this.mag1, this.mag2, this.mag3};
                  }

                  int signumx = this.negative ? -1 : 1;
                  BigInteger integer = JSONReader.BigIntegerCreator.BIG_INTEGER_CREATOR.apply(signumx, magx);
                  if ((this.context.features & JSONReader.Feature.UseLongForInts.mask) != 0L) {
                     return integer.longValue();
                  }

                  return integer;
               }
            case 2:
               BigDecimal decimalx = null;
               if (this.mag0 == 0 && this.mag1 == 0) {
                  if (this.mag2 == 0 && this.mag3 >= 0) {
                     int unscaledVal = this.negative ? -this.mag3 : this.mag3;
                     decimalx = BigDecimal.valueOf((long)unscaledVal, this.scale);
                  } else {
                     long v3 = (long)this.mag3 & 4294967295L;
                     long v2 = (long)this.mag2 & 4294967295L;
                     if (v2 <= 2147483647L) {
                        long v23 = (v2 << 32) + v3;
                        long unscaledVal = this.negative ? -v23 : v23;
                        if (this.exponent == 0) {
                           if ((this.context.features & JSONReader.Feature.UseBigDecimalForFloats.mask) != 0L) {
                              boolean isNegative;
                              long unsignedUnscaledVal;
                              if (unscaledVal < 0L) {
                                 isNegative = true;
                                 unsignedUnscaledVal = -unscaledVal;
                              } else {
                                 isNegative = false;
                                 unsignedUnscaledVal = unscaledVal;
                              }

                              int len = IOUtils.stringSize(unsignedUnscaledVal);
                              if (this.doubleChars == null) {
                                 this.doubleChars = new char[20];
                              }

                              IOUtils.getChars(unsignedUnscaledVal, len, this.doubleChars);
                              return TypeUtils.floatValue(isNegative, len - this.scale, this.doubleChars, len);
                           }

                           if ((this.context.features & JSONReader.Feature.UseBigDecimalForDoubles.mask) != 0L) {
                              boolean isNegativex;
                              long unsignedUnscaledValx;
                              if (unscaledVal < 0L) {
                                 isNegativex = true;
                                 unsignedUnscaledValx = -unscaledVal;
                              } else {
                                 isNegativex = false;
                                 unsignedUnscaledValx = unscaledVal;
                              }

                              if (unsignedUnscaledValx < 4503599627370496L) {
                                 if (this.scale > 0 && this.scale < JSONFactory.DOUBLE_10_POW.length) {
                                    return (double)unscaledVal / JSONFactory.DOUBLE_10_POW[this.scale];
                                 }

                                 if (this.scale < 0 && this.scale > -JSONFactory.DOUBLE_10_POW.length) {
                                    return (double)unscaledVal * JSONFactory.DOUBLE_10_POW[-this.scale];
                                 }
                              }

                              int len = unsignedUnscaledValx < 10000000000000000L
                                 ? 16
                                 : (unsignedUnscaledValx < 100000000000000000L ? 17 : (unsignedUnscaledValx < 1000000000000000000L ? 18 : 19));
                              if (this.doubleChars == null) {
                                 this.doubleChars = new char[20];
                              }

                              IOUtils.getChars(unsignedUnscaledValx, len, this.doubleChars);
                              return TypeUtils.doubleValue(isNegativex, len - this.scale, this.doubleChars, len);
                           }
                        }

                        decimalx = BigDecimal.valueOf(unscaledVal, this.scale);
                     }
                  }
               }

               if (decimalx == null) {
                  int[] magxx = this.mag0 == 0
                     ? (this.mag1 == 0 ? new int[]{this.mag2, this.mag3} : new int[]{this.mag1, this.mag2, this.mag3})
                     : new int[]{this.mag0, this.mag1, this.mag2, this.mag3};
                  int signumx = this.negative ? -1 : 1;
                  BigInteger bigIntx = JSONReader.BigIntegerCreator.BIG_INTEGER_CREATOR.apply(signumx, magxx);
                  int adjustedScale = this.scale - this.exponent;
                  decimalx = new BigDecimal(bigIntx, adjustedScale);
                  if (this.exponent != 0
                     && (this.context.features & (JSONReader.Feature.UseBigDecimalForDoubles.mask | JSONReader.Feature.UseBigDecimalForFloats.mask)) == 0L) {
                     return decimalx.doubleValue();
                  }
               }

               if (this.exponent != 0
                  && (this.context.features & (JSONReader.Feature.UseBigDecimalForDoubles.mask | JSONReader.Feature.UseBigDecimalForFloats.mask)) == 0L) {
                  String decimalStr = decimalx.toPlainString();
                  return Double.parseDouble(decimalStr + "E" + this.exponent);
               } else {
                  return (Number)((this.context.features & JSONReader.Feature.UseDoubleForDecimals.mask) != 0L ? decimalx.doubleValue() : decimalx);
               }
            case 3:
               return this.toInt64(this.stringValue);
            case 4:
               return this.boolValue ? 1 : 0;
            case 5:
               return null;
            case 6:
               return this.toNumber((Map)this.complex);
            case 7:
               return this.toNumber((List)this.complex);
            case 8:
               if (this.scale > 0) {
                  if (this.scale > JSONFactory.defaultDecimalMaxScale) {
                     throw new JSONException("scale overflow : " + this.scale);
                  }

                  return TypeUtils.toBigDecimal(this.stringValue);
               }

               return new BigInteger(this.stringValue);
            case 9:
               if (this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 >= 0) {
                  int intValue = this.negative ? -this.mag3 : this.mag3;
                  return (byte)intValue;
               }

               throw new JSONException(this.info("shortValue overflow"));
            case 10:
               if (this.mag0 == 0 && this.mag1 == 0 && this.mag2 == 0 && this.mag3 >= 0) {
                  int intValue = this.negative ? -this.mag3 : this.mag3;
                  return (short)intValue;
               }

               throw new JSONException(this.info("shortValue overflow"));
            case 12:
            case 13:
               int[] mag = this.mag0 == 0
                  ? (this.mag1 == 0 ? (this.mag2 == 0 ? new int[]{this.mag3} : new int[]{this.mag2, this.mag3}) : new int[]{this.mag1, this.mag2, this.mag3})
                  : new int[]{this.mag0, this.mag1, this.mag2, this.mag3};
               int signum = this.negative ? -1 : 1;
               BigInteger bigInt = JSONReader.BigIntegerCreator.BIG_INTEGER_CREATOR.apply(signum, mag);
               BigDecimal decimal = new BigDecimal(bigInt, this.scale);
               if (this.valueType == 12) {
                  if (this.exponent != 0) {
                     return Float.parseFloat(decimal + "E" + this.exponent);
                  }

                  return decimal.floatValue();
               } else {
                  if (this.exponent != 0) {
                     return Double.parseDouble(decimal + "E" + this.exponent);
                  }

                  return decimal.doubleValue();
               }
            default:
               throw new JSONException("TODO : " + this.valueType);
         }
      }
   }

   @Override
   public abstract void close();

   protected final int toInt32(String val) {
      if (!IOUtils.isNumber(val) && val.lastIndexOf(44) != val.length() - 4) {
         throw new JSONException("parseInt error, value : " + val);
      } else {
         return TypeUtils.toIntValue(val);
      }
   }

   protected final long toInt64(String val) {
      if (IOUtils.isNumber(val) || val.lastIndexOf(44) == val.length() - 4) {
         return TypeUtils.toLongValue(val);
      } else {
         if (val.length() > 10 && val.length() < 40) {
            try {
               return DateUtils.parseMillis(val, this.context.zoneId);
            } catch (JSONException | DateTimeException var3) {
            }
         }

         throw new JSONException("parseLong error, value : " + val);
      }
   }

   protected final long toLong(Map map) {
      Object val = map.get("val");
      if (val instanceof Number) {
         return (long)((Number)val).intValue();
      } else {
         throw new JSONException("parseLong error, value : " + map);
      }
   }

   protected final int toInt(List list) {
      if (list.size() == 1) {
         Object val = list.get(0);
         if (val instanceof Number) {
            return ((Number)val).intValue();
         }

         if (val instanceof String) {
            return Integer.parseInt((String)val);
         }
      }

      throw new JSONException("parseLong error, field : value " + list);
   }

   protected final Number toNumber(Map map) {
      Object val = map.get("val");
      return val instanceof Number ? (Number)val : null;
   }

   protected final BigDecimal decimal(JSONObject object) {
      BigDecimal decimal = object.getBigDecimal("value");
      if (decimal == null) {
         decimal = object.getBigDecimal("$numberDecimal");
      }

      if (decimal != null) {
         return decimal;
      } else {
         throw new JSONException("can not cast to decimal " + object);
      }
   }

   protected final Number toNumber(List list) {
      if (list.size() == 1) {
         Object val = list.get(0);
         if (val instanceof Number) {
            return (Number)val;
         }

         if (val instanceof String) {
            return TypeUtils.toBigDecimal((String)val);
         }
      }

      return null;
   }

   protected final String toString(List array) {
      JSONWriter writer = JSONWriter.of();
      writer.setRootObject(array);
      writer.write(array);
      return writer.toString();
   }

   protected final String toString(Map object) {
      JSONWriter writer = JSONWriter.of();
      writer.setRootObject(object);
      writer.write(object);
      return writer.toString();
   }

   public static JSONReader of(byte[] utf8Bytes) {
      boolean ascii = false;
      if (JDKUtils.PREDICATE_IS_ASCII != null) {
         ascii = JDKUtils.PREDICATE_IS_ASCII.test(utf8Bytes);
      }

      JSONReader.Context context = JSONFactory.createReadContext();
      if (ascii) {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderASCII(context, null, utf8Bytes, 0, utf8Bytes.length));
      } else {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderUTF8(context, null, utf8Bytes, 0, utf8Bytes.length));
      }
   }

   @Deprecated
   public static JSONReader of(JSONReader.Context context, byte[] utf8Bytes) {
      boolean ascii = false;
      if (JDKUtils.PREDICATE_IS_ASCII != null) {
         ascii = JDKUtils.PREDICATE_IS_ASCII.test(utf8Bytes);
      }

      if (ascii) {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderASCII(context, null, utf8Bytes, 0, utf8Bytes.length));
      } else {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderUTF8(context, null, utf8Bytes, 0, utf8Bytes.length));
      }
   }

   public static JSONReader of(byte[] utf8Bytes, JSONReader.Context context) {
      boolean ascii = false;
      if (JDKUtils.PREDICATE_IS_ASCII != null) {
         ascii = JDKUtils.PREDICATE_IS_ASCII.test(utf8Bytes);
      }

      if (ascii) {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderASCII(context, null, utf8Bytes, 0, utf8Bytes.length));
      } else {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, utf8Bytes, 0, utf8Bytes.length)
            : new JSONReaderUTF8(context, null, utf8Bytes, 0, utf8Bytes.length));
      }
   }

   public static JSONReader of(char[] chars) {
      JSONReader.Context context = JSONFactory.createReadContext();
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, null, chars, 0, chars.length)
         : new JSONReaderUTF16(context, null, chars, 0, chars.length));
   }

   @Deprecated
   public static JSONReader of(JSONReader.Context context, char[] chars) {
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, null, chars, 0, chars.length)
         : new JSONReaderUTF16(context, null, chars, 0, chars.length));
   }

   public static JSONReader of(char[] chars, JSONReader.Context context) {
      return new JSONReaderUTF16(context, null, chars, 0, chars.length);
   }

   public static JSONReader ofJSONB(byte[] jsonbBytes) {
      return new JSONReaderJSONB(JSONFactory.createReadContext(), jsonbBytes, 0, jsonbBytes.length);
   }

   @Deprecated
   public static JSONReader ofJSONB(JSONReader.Context context, byte[] jsonbBytes) {
      return new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);
   }

   public static JSONReader ofJSONB(byte[] jsonbBytes, JSONReader.Context context) {
      return new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);
   }

   public static JSONReader ofJSONB(InputStream in, JSONReader.Context context) {
      return new JSONReaderJSONB(context, in);
   }

   public static JSONReader ofJSONB(byte[] jsonbBytes, JSONReader.Feature... features) {
      JSONReader.Context context = JSONFactory.createReadContext();
      context.config(features);
      return new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);
   }

   public static JSONReader ofJSONB(byte[] bytes, int offset, int length) {
      return new JSONReaderJSONB(JSONFactory.createReadContext(), bytes, offset, length);
   }

   public static JSONReader ofJSONB(byte[] bytes, int offset, int length, JSONReader.Context context) {
      return new JSONReaderJSONB(context, bytes, offset, length);
   }

   public static JSONReader ofJSONB(byte[] bytes, int offset, int length, SymbolTable symbolTable) {
      return new JSONReaderJSONB(JSONFactory.createReadContext(symbolTable), bytes, offset, length);
   }

   public static JSONReader of(byte[] bytes, int offset, int length, Charset charset) {
      JSONReader.Context context = JSONFactory.createReadContext();
      if (charset == StandardCharsets.UTF_8) {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, bytes, offset, length)
            : new JSONReaderUTF8(context, null, bytes, offset, length));
      } else if (charset == StandardCharsets.UTF_16) {
         return new JSONReaderUTF16(context, bytes, offset, length);
      } else if (charset != StandardCharsets.US_ASCII && charset != StandardCharsets.ISO_8859_1) {
         throw new JSONException("not support charset " + charset);
      } else {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, bytes, offset, length)
            : new JSONReaderASCII(context, null, bytes, offset, length));
      }
   }

   public static JSONReader of(byte[] bytes, int offset, int length, Charset charset, JSONReader.Context context) {
      if (charset == StandardCharsets.UTF_8) {
         if (offset == 0 && bytes.length == length) {
            return of(bytes, context);
         } else {
            boolean hasNegative = true;
            if (JDKUtils.METHOD_HANDLE_HAS_NEGATIVE != null) {
               try {
                  hasNegative = (Boolean)JDKUtils.METHOD_HANDLE_HAS_NEGATIVE.invoke((byte[])bytes, (int)0, (int)bytes.length);
               } catch (Throwable var7) {
               }
            }

            if (!hasNegative) {
               return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
                  ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, bytes, offset, length)
                  : new JSONReaderASCII(context, null, bytes, offset, length));
            } else {
               return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
                  ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, bytes, offset, length)
                  : new JSONReaderUTF8(context, null, bytes, offset, length));
            }
         }
      } else if (charset == StandardCharsets.UTF_16) {
         return new JSONReaderUTF16(context, bytes, offset, length);
      } else if (charset != StandardCharsets.US_ASCII && charset != StandardCharsets.ISO_8859_1) {
         throw new JSONException("not support charset " + charset);
      } else {
         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, null, bytes, offset, length)
            : new JSONReaderASCII(context, null, bytes, offset, length));
      }
   }

   public static JSONReader of(byte[] bytes, int offset, int length) {
      JSONReader.Context context = JSONFactory.createReadContext();
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, bytes, offset, length)
         : new JSONReaderUTF8(context, null, bytes, offset, length));
   }

   public static JSONReader of(byte[] bytes, int offset, int length, JSONReader.Context context) {
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF8.create(context, null, bytes, offset, length)
         : new JSONReaderUTF8(context, null, bytes, offset, length));
   }

   public static JSONReader of(char[] chars, int offset, int length) {
      JSONReader.Context context = JSONFactory.createReadContext();
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, null, chars, offset, length)
         : new JSONReaderUTF16(context, null, chars, offset, length));
   }

   public static JSONReader of(char[] chars, int offset, int length, JSONReader.Context context) {
      return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
         ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, null, chars, offset, length)
         : new JSONReaderUTF16(context, null, chars, offset, length));
   }

   public static JSONReader of(URL url, JSONReader.Context context) throws IOException {
      InputStream is = url.openStream();

      JSONReader var3;
      try {
         var3 = of(is, StandardCharsets.UTF_8, context);
      } catch (Throwable var6) {
         if (is != null) {
            try {
               is.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (is != null) {
         is.close();
      }

      return var3;
   }

   public static JSONReader of(InputStream is, Charset charset) {
      JSONReader.Context context = JSONFactory.createReadContext();
      return of(is, charset, context);
   }

   public static JSONReader of(InputStream is, Charset charset, JSONReader.Context context) {
      if (is == null) {
         throw new JSONException("inputStream is null");
      } else if (charset == StandardCharsets.UTF_8 || charset == null) {
         return new JSONReaderUTF8(context, is);
      } else if (charset == StandardCharsets.UTF_16) {
         return new JSONReaderUTF16(context, is);
      } else {
         return (JSONReader)(charset == StandardCharsets.US_ASCII ? new JSONReaderASCII(context, is) : of(new InputStreamReader(is, charset), context));
      }
   }

   public static JSONReader of(Reader is) {
      return new JSONReaderUTF16(JSONFactory.createReadContext(), is);
   }

   public static JSONReader of(Reader is, JSONReader.Context context) {
      return new JSONReaderUTF16(context, is);
   }

   public static JSONReader of(ByteBuffer buffer, Charset charset) {
      JSONReader.Context context = JSONFactory.createReadContext();
      if (charset != StandardCharsets.UTF_8 && charset != null) {
         throw new JSONException("not support charset " + charset);
      } else {
         return new JSONReaderUTF8(context, buffer);
      }
   }

   public static JSONReader of(ByteBuffer buffer, Charset charset, JSONReader.Context context) {
      if (charset != StandardCharsets.UTF_8 && charset != null) {
         throw new JSONException("not support charset " + charset);
      } else {
         return new JSONReaderUTF8(context, buffer);
      }
   }

   @Deprecated
   public static JSONReader of(JSONReader.Context context, String str) {
      return of(str, context);
   }

   public static JSONReader of(String str) {
      if (str == null) {
         throw new NullPointerException();
      } else {
         JSONReader.Context context = JSONFactory.createReadContext();
         if (JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER != null && JDKUtils.PREDICATE_IS_ASCII != null) {
            try {
               int LATIN1 = 0;
               int coder = JDKUtils.STRING_CODER.applyAsInt(str);
               if (coder == 0) {
                  byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
                  if (JDKUtils.PREDICATE_IS_ASCII.test(bytes)) {
                     if (JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null) {
                        return JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, str, bytes, 0, bytes.length);
                     }

                     return new JSONReaderASCII(context, str, bytes, 0, bytes.length);
                  }
               }
            } catch (Exception var5) {
               throw new JSONException("unsafe get String.coder error");
            }
         }

         int length = str.length();
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            return new JSONReaderUTF16(context, str, chars, 0, length);
         } else {
            return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
               ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, str, null, 0, length)
               : new JSONReaderUTF16(context, str, 0, length));
         }
      }
   }

   public static JSONReader of(String str, JSONReader.Context context) {
      if (str == null || context == null) {
         throw new NullPointerException();
      } else {
         if (JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER != null) {
            try {
               int LATIN1 = 0;
               int coder = JDKUtils.STRING_CODER.applyAsInt(str);
               if (coder == 0) {
                  byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
                  if (JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null) {
                     return JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, str, bytes, 0, bytes.length);
                  }

                  return new JSONReaderASCII(context, str, bytes, 0, bytes.length);
               }
            } catch (Exception var5) {
               throw new JSONException("unsafe get String.coder error");
            }
         }

         int length = str.length();
         char[] chars;
         if (JDKUtils.JVM_VERSION == 8) {
            chars = JDKUtils.getCharArray(str);
         } else {
            chars = str.toCharArray();
         }

         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, str, chars, 0, length)
            : new JSONReaderUTF16(context, str, chars, 0, length));
      }
   }

   public static JSONReader of(String str, int offset, int length) {
      if (str == null) {
         throw new NullPointerException();
      } else {
         JSONReader.Context context = JSONFactory.createReadContext();
         if (JDKUtils.STRING_VALUE != null) {
            try {
               int LATIN1 = 0;
               int coder = JDKUtils.STRING_CODER.applyAsInt(str);
               if (coder == 0) {
                  byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
                  if (JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null) {
                     return JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, str, bytes, offset, length);
                  }

                  return new JSONReaderASCII(context, str, bytes, offset, length);
               }
            } catch (Exception var7) {
               throw new JSONException("unsafe get String.coder error");
            }
         }

         char[] chars;
         if (JDKUtils.JVM_VERSION == 8) {
            chars = JDKUtils.getCharArray(str);
         } else {
            chars = str.toCharArray();
         }

         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, str, chars, offset, length)
            : new JSONReaderUTF16(context, str, chars, offset, length));
      }
   }

   public static JSONReader of(String str, int offset, int length, JSONReader.Context context) {
      if (str == null || context == null) {
         throw new NullPointerException();
      } else {
         if (JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER != null) {
            try {
               int LATIN1 = 0;
               int coder = JDKUtils.STRING_CODER.applyAsInt(str);
               if (coder == 0) {
                  byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
                  if (JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII != null) {
                     return JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_ASCII.create(context, str, bytes, offset, length);
                  }

                  return new JSONReaderASCII(context, str, bytes, offset, length);
               }
            } catch (Exception var7) {
               throw new JSONException("unsafe get String.coder error");
            }
         }

         char[] chars;
         if (JDKUtils.JVM_VERSION == 8) {
            chars = JDKUtils.getCharArray(str);
         } else {
            chars = str.toCharArray();
         }

         return (JSONReader)(JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16 != null
            ? JSONFactory.INCUBATOR_VECTOR_READER_CREATOR_UTF16.create(context, str, chars, offset, length)
            : new JSONReaderUTF16(context, str, chars, offset, length));
      }
   }

   final void bigInt(char[] chars, int off, int len) {
      int numDigits = len - off;
      if (this.scale > 0) {
         numDigits--;
      }

      if (numDigits > 38) {
         throw new JSONException("number too large : " + new String(chars, off, numDigits));
      } else {
         int firstGroupLen = numDigits % 9;
         if (firstGroupLen == 0) {
            firstGroupLen = 9;
         }

         int cursor;
         int end = cursor = off + firstGroupLen;
         int groupVal = off + 1;
         char c = chars[off];
         if (c == '.') {
            c = chars[groupVal++];
            cursor++;
         }

         int result = c - '0';

         for (int index = groupVal; index < end; index++) {
            c = chars[index];
            if (c == '.') {
               c = chars[++index];
               cursor++;
               if (end < len) {
                  end++;
               }
            }

            int nextVal = c - '0';
            result = 10 * result + nextVal;
         }

         this.mag3 = result;

         while (cursor < len) {
            end = cursor;
            cursor += 9;
            c = (char)cursor;
            char cx = chars[end++];
            if (cx == '.') {
               cx = chars[end++];
               cursor++;
               c++;
            }

            int resultx = cx - '0';

            for (int index = end; index < c; index++) {
               cx = chars[index];
               if (cx == '.') {
                  cx = chars[++index];
                  cursor++;
                  c++;
               }

               int nextVal = cx - '0';
               resultx = 10 * resultx + nextVal;
            }

            long ylong = 1000000000L;
            long carry = 0L;

            for (int i = 3; i >= 0; i--) {
               long product;
               switch (i) {
                  case 0:
                     product = ylong * ((long)this.mag0 & 4294967295L) + carry;
                     this.mag0 = (int)product;
                     break;
                  case 1:
                     product = ylong * ((long)this.mag1 & 4294967295L) + carry;
                     this.mag1 = (int)product;
                     break;
                  case 2:
                     product = ylong * ((long)this.mag2 & 4294967295L) + carry;
                     this.mag2 = (int)product;
                     break;
                  case 3:
                     product = ylong * ((long)this.mag3 & 4294967295L) + carry;
                     this.mag3 = (int)product;
                     break;
                  default:
                     throw new ArithmeticException("BigInteger would overflow supported range");
               }

               carry = product >>> 32;
            }

            long zlong = (long)resultx & 4294967295L;
            long sum = ((long)this.mag3 & 4294967295L) + zlong;
            this.mag3 = (int)sum;
            carry = sum >>> 32;

            for (int i = 2; i >= 0; i--) {
               switch (i) {
                  case 0:
                     sum = ((long)this.mag0 & 4294967295L) + carry;
                     this.mag0 = (int)sum;
                     break;
                  case 1:
                     sum = ((long)this.mag1 & 4294967295L) + carry;
                     this.mag1 = (int)sum;
                     break;
                  case 2:
                     sum = ((long)this.mag2 & 4294967295L) + carry;
                     this.mag2 = (int)sum;
                     break;
                  case 3:
                     sum = ((long)this.mag3 & 4294967295L) + carry;
                     this.mag3 = (int)sum;
                     break;
                  default:
                     throw new ArithmeticException("BigInteger would overflow supported range");
               }

               carry = sum >>> 32;
            }
         }
      }
   }

   final void bigInt(byte[] chars, int off, int len) {
      int numDigits = len - off;
      if (this.scale > 0) {
         numDigits--;
      }

      if (numDigits > 38) {
         throw new JSONException("number too large : " + new String(chars, off, numDigits));
      } else {
         int firstGroupLen = numDigits % 9;
         if (firstGroupLen == 0) {
            firstGroupLen = 9;
         }

         int cursor;
         int end = cursor = off + firstGroupLen;
         int groupVal = off + 1;
         char c = (char)chars[off];
         if (c == '.') {
            c = (char)chars[groupVal++];
            cursor++;
         }

         int result = c - '0';

         for (int index = groupVal; index < end; index++) {
            c = (char)chars[index];
            if (c == '.') {
               c = (char)chars[++index];
               cursor++;
               if (end < len) {
                  end++;
               }
            }

            int nextVal = c - '0';
            result = 10 * result + nextVal;
         }

         this.mag3 = result;

         while (cursor < len) {
            end = cursor;
            cursor += 9;
            c = (char)cursor;
            char cx = (char)chars[end++];
            if (cx == '.') {
               cx = (char)chars[end++];
               cursor++;
               c++;
            }

            int resultx = cx - '0';

            for (int index = end; index < c; index++) {
               cx = (char)chars[index];
               if (cx == '.') {
                  cx = (char)chars[++index];
                  cursor++;
                  c++;
               }

               int nextVal = cx - '0';
               resultx = 10 * resultx + nextVal;
            }

            long ylong = 1000000000L;
            long zlong = (long)resultx & 4294967295L;
            long carry = 0L;

            for (int i = 3; i >= 0; i--) {
               long product;
               switch (i) {
                  case 0:
                     product = ylong * ((long)this.mag0 & 4294967295L) + carry;
                     this.mag0 = (int)product;
                     break;
                  case 1:
                     product = ylong * ((long)this.mag1 & 4294967295L) + carry;
                     this.mag1 = (int)product;
                     break;
                  case 2:
                     product = ylong * ((long)this.mag2 & 4294967295L) + carry;
                     this.mag2 = (int)product;
                     break;
                  case 3:
                     product = ylong * ((long)this.mag3 & 4294967295L) + carry;
                     this.mag3 = (int)product;
                     break;
                  default:
                     throw new ArithmeticException("BigInteger would overflow supported range");
               }

               carry = product >>> 32;
            }

            long sum = ((long)this.mag3 & 4294967295L) + zlong;
            this.mag3 = (int)sum;
            carry = sum >>> 32;

            for (int i = 2; i >= 0; i--) {
               switch (i) {
                  case 0:
                     sum = ((long)this.mag0 & 4294967295L) + carry;
                     this.mag0 = (int)sum;
                     break;
                  case 1:
                     sum = ((long)this.mag1 & 4294967295L) + carry;
                     this.mag1 = (int)sum;
                     break;
                  case 2:
                     sum = ((long)this.mag2 & 4294967295L) + carry;
                     this.mag2 = (int)sum;
                     break;
                  case 3:
                     sum = ((long)this.mag3 & 4294967295L) + carry;
                     this.mag3 = (int)sum;
                     break;
                  default:
                     throw new ArithmeticException("BigInteger would overflow supported range");
               }

               carry = sum >>> 32;
            }
         }
      }
   }

   public static JSONReader.AutoTypeBeforeHandler autoTypeFilter(String... names) {
      return new ContextAutoTypeBeforeHandler(names);
   }

   public static JSONReader.AutoTypeBeforeHandler autoTypeFilter(boolean includeBasic, String... names) {
      return new ContextAutoTypeBeforeHandler(includeBasic, names);
   }

   public static JSONReader.AutoTypeBeforeHandler autoTypeFilter(Class... types) {
      return new ContextAutoTypeBeforeHandler(types);
   }

   public static JSONReader.AutoTypeBeforeHandler autoTypeFilter(boolean includeBasic, Class... types) {
      return new ContextAutoTypeBeforeHandler(includeBasic, types);
   }

   public JSONReader.SavePoint mark() {
      return new JSONReader.SavePoint(this.offset, this.ch);
   }

   public void reset(JSONReader.SavePoint savePoint) {
      this.offset = savePoint.offset;
      this.ch = (char)savePoint.current;
   }

   final JSONException notSupportName() {
      return new JSONException(this.info("not support unquoted name"));
   }

   final JSONException valueError() {
      return new JSONException(this.info("illegal value"));
   }

   final JSONException error(int offset, int ch) {
      throw new JSONValidException("error, offset " + offset + ", char " + (char)ch);
   }

   static JSONException syntaxError(int ch) {
      return new JSONException("syntax error, expect ',', but '" + (char)ch + "'");
   }

   static JSONException syntaxError(int offset, int ch) {
      return new JSONException("syntax error, offset " + offset + ", char " + (char)ch);
   }

   static JSONException numberError(int offset, int ch) {
      return new JSONException("illegal number, offset " + offset + ", char " + (char)ch);
   }

   JSONException numberError() {
      return new JSONException("illegal number, offset " + this.offset + ", char " + this.ch);
   }

   public final String info() {
      return this.info(null);
   }

   public String info(String message) {
      return message != null && !message.isEmpty() ? message + ", offset " + this.offset : "offset " + this.offset;
   }

   static boolean isFirstIdentifier(int ch) {
      return ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122 || ch == 95 || ch == 36 || ch >= 48 && ch <= 57 || ch > 127;
   }

   public ObjectReader getObjectReaderAutoType(long typeHash, Class expectClass, long features) {
      ObjectReader autoTypeObjectReader = this.context.getObjectReaderAutoType(typeHash);
      if (autoTypeObjectReader != null) {
         return autoTypeObjectReader;
      } else {
         String typeName = this.getString();
         if (this.context.autoTypeBeforeHandler != null) {
            Class<?> autoTypeClass = this.context.autoTypeBeforeHandler.apply(typeName, expectClass, features);
            if (autoTypeClass != null) {
               boolean fieldBased = (features & JSONReader.Feature.FieldBased.mask) != 0L;
               return this.context.provider.getObjectReader(autoTypeClass, fieldBased);
            }
         }

         return this.context.provider.getObjectReader(typeName, expectClass, this.context.features | features);
      }
   }

   protected final String readStringNotMatch() {
      switch (this.ch) {
         case '+':
         case '-':
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
            this.readNumber0();
            Number number = this.getNumber();
            return number.toString();
         case '[':
            return this.toString(this.readArray());
         case 'f':
         case 't':
            this.boolValue = this.readBoolValue();
            return this.boolValue ? "true" : "false";
         case 'n':
            this.readNull();
            return null;
         case '{':
            return this.toString(this.readObject());
         default:
            throw new JSONException(this.info("illegal input : " + this.ch));
      }
   }

   public interface AutoTypeBeforeHandler extends Filter {
      default Class<?> apply(long typeNameHash, Class<?> expectClass, long features) {
         return null;
      }

      Class<?> apply(String var1, Class<?> var2, long var3);
   }

   static final class BigIntegerCreator implements BiFunction<Integer, int[], BigInteger> {
      static final BiFunction<Integer, int[], BigInteger> BIG_INTEGER_CREATOR;

      public BigInteger apply(Integer integer, int[] mag) {
         int signum = integer;
         int bitLength;
         if (mag.length == 0) {
            bitLength = 0;
         } else {
            int bitLengthForInt = 32 - Integer.numberOfLeadingZeros(mag[0]);
            int magBitLength = (mag.length - 1 << 5) + bitLengthForInt;
            if (signum < 0) {
               boolean pow2 = Integer.bitCount(mag[0]) == 1;

               for (int i = 1; i < mag.length && pow2; i++) {
                  pow2 = mag[i] == 0;
               }

               bitLength = pow2 ? magBitLength - 1 : magBitLength;
            } else {
               bitLength = magBitLength;
            }
         }

         int byteLen = bitLength / 8 + 1;
         byte[] bytes = new byte[byteLen];
         int i = byteLen - 1;
         int bytesCopied = 4;
         int nextInt = 0;

         for (int intIndex = 0; i >= 0; i--) {
            if (bytesCopied != 4) {
               nextInt >>>= 8;
               bytesCopied++;
            } else {
               int n = intIndex++;
               if (n < 0) {
                  nextInt = 0;
               } else if (n >= mag.length) {
                  nextInt = signum < 0 ? -1 : 0;
               } else {
                  int magInt = mag[mag.length - n - 1];
                  if (signum >= 0) {
                     nextInt = magInt;
                  } else {
                     int mlen = mag.length;
                     int j = mlen - 1;

                     while (j >= 0 && mag[j] == 0) {
                        j--;
                     }

                     int firstNonzeroIntNum = mlen - j - 1;
                     if (n <= firstNonzeroIntNum) {
                        nextInt = -magInt;
                     } else {
                        nextInt = ~magInt;
                     }
                  }
               }

               bytesCopied = 1;
            }

            bytes[i] = (byte)nextInt;
         }

         return new BigInteger(bytes);
      }

      static {
         BiFunction<Integer, int[], BigInteger> bigIntegerCreator = null;
         if (!JDKUtils.ANDROID && !JDKUtils.GRAAL) {
            try {
               Lookup caller = JDKUtils.trustedLookup(BigInteger.class);
               MethodHandle handle = caller.findConstructor(BigInteger.class, MethodType.methodType(void.class, int.class, int[].class));
               CallSite callSite = LambdaMetafactory.metafactory(
                  caller,
                  "apply",
                  MethodType.methodType(BiFunction.class),
                  handle.type().generic(),
                  handle,
                  MethodType.methodType(BigInteger.class, Integer.class, int[].class)
               );
               bigIntegerCreator = (BiFunction)callSite.getTarget().invokeExact();
            } catch (Throwable var4) {
            }
         }

         if (bigIntegerCreator == null) {
            bigIntegerCreator = new JSONReader.BigIntegerCreator();
         }

         BIG_INTEGER_CREATOR = bigIntegerCreator;
      }
   }

   public static final class Context {
      String dateFormat;
      boolean formatyyyyMMddhhmmss19;
      boolean formatyyyyMMddhhmmssT19;
      boolean yyyyMMddhhmm16;
      boolean formatyyyyMMdd8;
      boolean formatMillis;
      boolean formatUnixTime;
      boolean formatISO8601;
      boolean formatHasDay;
      boolean formatHasHour;
      boolean useSimpleFormatter;
      int maxLevel = 2048;
      int bufferSize = 524288;
      DateTimeFormatter dateFormatter;
      ZoneId zoneId;
      long features;
      Locale locale;
      TimeZone timeZone;
      Supplier<Map> objectSupplier;
      Supplier<List> arraySupplier;
      JSONReader.AutoTypeBeforeHandler autoTypeBeforeHandler;
      ExtraProcessor extraProcessor;
      final ObjectReaderProvider provider;
      final SymbolTable symbolTable;

      public Context(ObjectReaderProvider provider) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }
      }

      public Context(ObjectReaderProvider provider, long features) {
         this.features = features;
         this.provider = provider;
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }
      }

      public Context(JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = JSONFactory.getDefaultObjectReaderProvider();
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public Context(String dateFormat, JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = JSONFactory.getDefaultObjectReaderProvider();
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }

         this.setDateFormat(dateFormat);
      }

      public Context(ObjectReaderProvider provider, JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public Context(ObjectReaderProvider provider, Filter filter, JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.objectSupplier = JSONFactory.defaultObjectSupplier;
         this.arraySupplier = JSONFactory.defaultArraySupplier;
         this.symbolTable = null;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         this.config(filter);
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public Context(ObjectReaderProvider provider, SymbolTable symbolTable) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.symbolTable = symbolTable;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }
      }

      public Context(ObjectReaderProvider provider, SymbolTable symbolTable, JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.symbolTable = symbolTable;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public Context(ObjectReaderProvider provider, SymbolTable symbolTable, Filter[] filters, JSONReader.Feature... features) {
         this.features = JSONFactory.defaultReaderFeatures;
         this.provider = provider;
         this.symbolTable = symbolTable;
         this.zoneId = JSONFactory.defaultReaderZoneId;
         this.config(filters);
         String format = JSONFactory.defaultReaderFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public boolean isFormatUnixTime() {
         return this.formatUnixTime;
      }

      public boolean isFormatyyyyMMddhhmmss19() {
         return this.formatyyyyMMddhhmmss19;
      }

      public boolean isFormatyyyyMMddhhmmssT19() {
         return this.formatyyyyMMddhhmmssT19;
      }

      public boolean isFormatyyyyMMdd8() {
         return this.formatyyyyMMdd8;
      }

      public boolean isFormatMillis() {
         return this.formatMillis;
      }

      public boolean isFormatISO8601() {
         return this.formatISO8601;
      }

      public boolean isFormatHasHour() {
         return this.formatHasHour;
      }

      public ObjectReader getObjectReader(Type type) {
         boolean fieldBased = (this.features & JSONReader.Feature.FieldBased.mask) != 0L;
         return this.provider.getObjectReader(type, fieldBased);
      }

      public ObjectReaderProvider getProvider() {
         return this.provider;
      }

      public ObjectReader getObjectReaderAutoType(long hashCode) {
         return this.provider.getObjectReader(hashCode);
      }

      public ObjectReader getObjectReaderAutoType(String typeName, Class expectClass) {
         if (this.autoTypeBeforeHandler != null) {
            Class<?> autoTypeClass = this.autoTypeBeforeHandler.apply(typeName, expectClass, this.features);
            if (autoTypeClass != null) {
               boolean fieldBased = (this.features & JSONReader.Feature.FieldBased.mask) != 0L;
               return this.provider.getObjectReader(autoTypeClass, fieldBased);
            }
         }

         return this.provider.getObjectReader(typeName, expectClass, this.features);
      }

      public JSONReader.AutoTypeBeforeHandler getContextAutoTypeBeforeHandler() {
         return this.autoTypeBeforeHandler;
      }

      public ObjectReader getObjectReaderAutoType(String typeName, Class expectClass, long features) {
         if (this.autoTypeBeforeHandler != null) {
            Class<?> autoTypeClass = this.autoTypeBeforeHandler.apply(typeName, expectClass, features);
            if (autoTypeClass != null) {
               boolean fieldBased = (features & JSONReader.Feature.FieldBased.mask) != 0L;
               return this.provider.getObjectReader(autoTypeClass, fieldBased);
            }
         }

         return this.provider.getObjectReader(typeName, expectClass, this.features | features);
      }

      public ExtraProcessor getExtraProcessor() {
         return this.extraProcessor;
      }

      public void setExtraProcessor(ExtraProcessor extraProcessor) {
         this.extraProcessor = extraProcessor;
      }

      public Supplier<Map> getObjectSupplier() {
         return this.objectSupplier;
      }

      public void setObjectSupplier(Supplier<Map> objectSupplier) {
         this.objectSupplier = objectSupplier;
      }

      public Supplier<List> getArraySupplier() {
         return this.arraySupplier;
      }

      public void setArraySupplier(Supplier<List> arraySupplier) {
         this.arraySupplier = arraySupplier;
      }

      public DateTimeFormatter getDateFormatter() {
         if (this.dateFormatter == null && this.dateFormat != null && !this.formatMillis && !this.formatISO8601 && !this.formatUnixTime) {
            this.dateFormatter = this.locale == null ? DateTimeFormatter.ofPattern(this.dateFormat) : DateTimeFormatter.ofPattern(this.dateFormat, this.locale);
         }

         return this.dateFormatter;
      }

      public void setDateFormatter(DateTimeFormatter dateFormatter) {
         this.dateFormatter = dateFormatter;
      }

      public String getDateFormat() {
         return this.dateFormat;
      }

      public void setDateFormat(String format) {
         if (format != null && format.isEmpty()) {
            format = null;
         }

         boolean formatUnixTime = false;
         boolean formatISO8601 = false;
         boolean formatMillis = false;
         boolean hasDay = false;
         boolean hasHour = false;
         boolean useSimpleFormatter = false;
         if (format != null) {
            switch (format) {
               case "unixtime":
                  formatUnixTime = true;
                  break;
               case "iso8601":
                  formatISO8601 = true;
                  break;
               case "millis":
                  formatMillis = true;
                  break;
               case "yyyyMMddHHmmssSSSZ":
                  useSimpleFormatter = true;
               case "yyyy-MM-dd HH:mm:ss":
               case "yyyy-MM-ddTHH:mm:ss":
                  this.formatyyyyMMddhhmmss19 = true;
                  hasDay = true;
                  hasHour = true;
                  break;
               case "yyyy-MM-dd'T'HH:mm:ss":
                  this.formatyyyyMMddhhmmssT19 = true;
                  hasDay = true;
                  hasHour = true;
                  break;
               case "yyyyMMdd":
               case "yyyy-MM-dd":
                  this.formatyyyyMMdd8 = true;
                  hasDay = true;
                  hasHour = false;
                  break;
               case "yyyy-MM-dd HH:mm":
                  this.yyyyMMddhhmm16 = true;
                  break;
               default:
                  hasDay = format.indexOf(100) != -1;
                  hasHour = format.indexOf(72) != -1 || format.indexOf(104) != -1 || format.indexOf(75) != -1 || format.indexOf(107) != -1;
            }
         }

         if (!Objects.equals(this.dateFormat, format)) {
            this.dateFormatter = null;
         }

         this.dateFormat = format;
         this.formatUnixTime = formatUnixTime;
         this.formatMillis = formatMillis;
         this.formatISO8601 = formatISO8601;
         this.formatHasDay = hasDay;
         this.formatHasHour = hasHour;
         this.useSimpleFormatter = useSimpleFormatter;
      }

      public ZoneId getZoneId() {
         if (this.zoneId == null) {
            this.zoneId = DateUtils.DEFAULT_ZONE_ID;
         }

         return this.zoneId;
      }

      public long getFeatures() {
         return this.features;
      }

      public void setFeatures(long features) {
         this.features = features;
      }

      public void setZoneId(ZoneId zoneId) {
         this.zoneId = zoneId;
      }

      public int getMaxLevel() {
         return this.maxLevel;
      }

      public void setMaxLevel(int maxLevel) {
         this.maxLevel = maxLevel;
      }

      public int getBufferSize() {
         return this.bufferSize;
      }

      public JSONReader.Context setBufferSize(int bufferSize) {
         if (bufferSize < 0) {
            throw new IllegalArgumentException("buffer size can not be less than zero");
         } else {
            this.bufferSize = bufferSize;
            return this;
         }
      }

      public Locale getLocale() {
         return this.locale;
      }

      public void setLocale(Locale locale) {
         this.locale = locale;
      }

      public TimeZone getTimeZone() {
         return this.timeZone;
      }

      public void setTimeZone(TimeZone timeZone) {
         this.timeZone = timeZone;
      }

      public void config(JSONReader.Feature... features) {
         for (int i = 0; i < features.length; i++) {
            this.features = this.features | features[i].mask;
         }
      }

      public void config(Filter filter, JSONReader.Feature... features) {
         if (filter instanceof JSONReader.AutoTypeBeforeHandler) {
            this.autoTypeBeforeHandler = (JSONReader.AutoTypeBeforeHandler)filter;
         }

         if (filter instanceof ExtraProcessor) {
            this.extraProcessor = (ExtraProcessor)filter;
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public void config(Filter filter) {
         if (filter instanceof JSONReader.AutoTypeBeforeHandler) {
            this.autoTypeBeforeHandler = (JSONReader.AutoTypeBeforeHandler)filter;
         }

         if (filter instanceof ExtraProcessor) {
            this.extraProcessor = (ExtraProcessor)filter;
         }
      }

      public void config(Filter[] filters, JSONReader.Feature... features) {
         for (Filter filter : filters) {
            if (filter instanceof JSONReader.AutoTypeBeforeHandler) {
               this.autoTypeBeforeHandler = (JSONReader.AutoTypeBeforeHandler)filter;
            }

            if (filter instanceof ExtraProcessor) {
               this.extraProcessor = (ExtraProcessor)filter;
            }
         }

         for (JSONReader.Feature feature : features) {
            this.features = this.features | feature.mask;
         }
      }

      public void config(Filter[] filters) {
         for (Filter filter : filters) {
            if (filter instanceof JSONReader.AutoTypeBeforeHandler) {
               this.autoTypeBeforeHandler = (JSONReader.AutoTypeBeforeHandler)filter;
            }

            if (filter instanceof ExtraProcessor) {
               this.extraProcessor = (ExtraProcessor)filter;
            }
         }
      }

      public boolean isEnabled(JSONReader.Feature feature) {
         return (this.features & feature.mask) != 0L;
      }

      public void config(JSONReader.Feature feature, boolean state) {
         if (state) {
            this.features = this.features | feature.mask;
         } else {
            this.features = this.features & ~feature.mask;
         }
      }
   }

   public static enum Feature {
      FieldBased(1L),
      IgnoreNoneSerializable(2L),
      ErrorOnNoneSerializable(4L),
      SupportArrayToBean(8L),
      InitStringFieldAsEmpty(16L),
      @Deprecated
      SupportAutoType(32L),
      SupportSmartMatch(64L),
      UseNativeObject(128L),
      SupportClassForName(256L),
      IgnoreSetNullValue(512L),
      UseDefaultConstructorAsPossible(1024L),
      UseBigDecimalForFloats(2048L),
      UseBigDecimalForDoubles(4096L),
      ErrorOnEnumNotMatch(8192L),
      TrimString(16384L),
      ErrorOnNotSupportAutoType(32768L),
      DuplicateKeyValueAsArray(65536L),
      AllowUnQuotedFieldNames(131072L),
      NonStringKeyAsString(262144L),
      Base64StringAsByteArray(524288L),
      IgnoreCheckClose(1048576L),
      ErrorOnNullForPrimitives(2097152L),
      NullOnError(4194304L),
      IgnoreAutoTypeNotMatch(8388608L),
      NonZeroNumberCastToBooleanAsTrue(16777216L),
      IgnoreNullPropertyValue(33554432L),
      ErrorOnUnknownProperties(67108864L),
      EmptyStringAsNull(134217728L),
      NonErrorOnNumberOverflow(268435456L),
      UseBigIntegerForInts(536870912L),
      UseLongForInts(1073741824L),
      DisableSingleQuote(2147483648L),
      UseDoubleForDecimals(4294967296L);

      public final long mask;

      private Feature(long mask) {
         this.mask = mask;
      }

      public static long of(JSONReader.Feature[] features) {
         if (features == null) {
            return 0L;
         } else {
            long value = 0L;

            for (JSONReader.Feature feature : features) {
               value |= feature.mask;
            }

            return value;
         }
      }

      public boolean isEnabled(long features) {
         return (features & this.mask) != 0L;
      }

      public static boolean isEnabled(long features, JSONReader.Feature feature) {
         return (features & feature.mask) != 0L;
      }
   }

   static class ResolveTask {
      final FieldReader fieldReader;
      final Object object;
      final Object name;
      final JSONPath reference;

      ResolveTask(FieldReader fieldReader, Object object, Object name, JSONPath reference) {
         this.fieldReader = fieldReader;
         this.object = object;
         this.name = name;
         this.reference = reference;
      }

      @Override
      public String toString() {
         return this.reference.toString();
      }
   }

   public static class SavePoint {
      protected final int offset;
      protected final int current;

      protected SavePoint(int offset, int current) {
         this.offset = offset;
         this.current = current;
      }
   }
}
