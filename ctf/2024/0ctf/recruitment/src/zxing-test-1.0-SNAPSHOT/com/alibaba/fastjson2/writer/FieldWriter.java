package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.SymbolTable;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.JdbcSupport;
import com.alibaba.fastjson2.util.JodaSupport;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

public abstract class FieldWriter<T> implements Comparable {
   public final String fieldName;
   public final Type fieldType;
   public final Class fieldClass;
   public final long features;
   public final int ordinal;
   public final String format;
   public final Locale locale;
   public final DecimalFormat decimalFormat;
   public final String label;
   public final Field field;
   public final Method method;
   protected final long fieldOffset;
   protected final boolean primitive;
   final long hashCode;
   final byte[] nameWithColonUTF8;
   final char[] nameWithColonUTF16;
   final byte[] nameJSONB;
   long nameSymbolCache;
   final boolean fieldClassSerializable;
   final JSONWriter.Path rootParentPath;
   final boolean symbol;
   final boolean trim;
   final boolean raw;
   final boolean managedReference;
   final boolean backReference;
   transient JSONWriter.Path path;
   volatile ObjectWriter initObjectWriter;
   Object defaultValue;
   static final AtomicReferenceFieldUpdater<FieldWriter, ObjectWriter> initObjectWriterUpdater = AtomicReferenceFieldUpdater.newUpdater(
      FieldWriter.class, ObjectWriter.class, "initObjectWriter"
   );

   FieldWriter(
      String name, int ordinal, long features, String format, Locale locale, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      if ("string".equals(format) && fieldClass != String.class) {
         features |= JSONWriter.Feature.WriteNonStringValueAsString.mask;
      }

      this.fieldName = name;
      this.ordinal = ordinal;
      this.format = format;
      this.locale = locale;
      this.label = label;
      this.hashCode = Fnv.hashCode64(name);
      this.features = features;
      this.fieldType = TypeUtils.intern(fieldType);
      this.fieldClass = fieldClass;
      this.fieldClassSerializable = fieldClass != null && (Serializable.class.isAssignableFrom(fieldClass) || !Modifier.isFinal(fieldClass.getModifiers()));
      this.field = field;
      this.method = method;
      this.primitive = fieldClass.isPrimitive();
      this.nameJSONB = JSONB.toBytes(this.fieldName);
      DecimalFormat decimalFormat = null;
      if (format != null
         && (
            fieldClass == float.class
               || fieldClass == float[].class
               || fieldClass == Float.class
               || fieldClass == Float[].class
               || fieldClass == double.class
               || fieldClass == double[].class
               || fieldClass == Double.class
               || fieldClass == Double[].class
               || fieldClass == BigDecimal.class
               || fieldClass == BigDecimal[].class
         )) {
         decimalFormat = new DecimalFormat(format);
      }

      this.decimalFormat = decimalFormat;
      long fieldOffset = -1L;
      if (field != null) {
         fieldOffset = JDKUtils.UNSAFE.objectFieldOffset(field);
      }

      this.fieldOffset = fieldOffset;
      this.symbol = "symbol".equals(format);
      this.trim = "trim".equals(format);
      this.raw = (features & 1125899906842624L) != 0L;
      this.managedReference = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
      this.backReference = (features & 2305843009213693952L) != 0L;
      this.rootParentPath = new JSONWriter.Path(JSONWriter.Path.ROOT, name);
      int nameLength = name.length();
      int utflen = nameLength + 3;

      for (int i = 0; i < nameLength; i++) {
         char c = name.charAt(i);
         if (c < 1 || c > 127) {
            if (c > 2047) {
               utflen += 2;
            } else {
               utflen++;
            }
         }
      }

      byte[] bytes = new byte[utflen];
      int off = 0;
      bytes[off++] = 34;

      for (int ix = 0; ix < nameLength; ix++) {
         char c = name.charAt(ix);
         if (c >= 1 && c <= 127) {
            bytes[off++] = (byte)c;
         } else if (c > 2047) {
            bytes[off++] = (byte)(224 | c >> '\f' & 15);
            bytes[off++] = (byte)(128 | c >> 6 & 63);
            bytes[off++] = (byte)(128 | c & '?');
         } else {
            bytes[off++] = (byte)(192 | c >> 6 & 31);
            bytes[off++] = (byte)(128 | c & '?');
         }
      }

      bytes[off++] = 34;
      bytes[off] = 58;
      this.nameWithColonUTF8 = bytes;
      char[] chars = new char[nameLength + 3];
      chars[0] = '"';
      name.getChars(0, name.length(), chars, 1);
      chars[chars.length - 2] = '"';
      chars[chars.length - 1] = ':';
      this.nameWithColonUTF16 = chars;
   }

   public boolean isFieldClassSerializable() {
      return this.fieldClassSerializable;
   }

   public boolean isDateFormatMillis() {
      return false;
   }

   public boolean isDateFormatISO8601() {
      return false;
   }

   public void writeEnumJSONB(JSONWriter jsonWriter, Enum e) {
      throw new UnsupportedOperationException();
   }

   public ObjectWriter getInitWriter() {
      return null;
   }

   public boolean unwrapped() {
      return false;
   }

   public final void writeFieldNameJSONB(JSONWriter jsonWriter) {
      SymbolTable symbolTable = jsonWriter.symbolTable;
      if (symbolTable == null || !this.writeFieldNameSymbol(jsonWriter, symbolTable)) {
         jsonWriter.writeNameRaw(this.nameJSONB, this.hashCode);
      }
   }

   public final void writeFieldName(JSONWriter jsonWriter) {
      if (jsonWriter.jsonb) {
         SymbolTable symbolTable = jsonWriter.symbolTable;
         if (symbolTable == null || !this.writeFieldNameSymbol(jsonWriter, symbolTable)) {
            jsonWriter.writeNameRaw(this.nameJSONB, this.hashCode);
         }
      } else {
         if (!jsonWriter.useSingleQuote && (jsonWriter.context.getFeatures() & JSONWriter.Feature.UnquoteFieldName.mask) == 0L) {
            if (jsonWriter.utf8) {
               jsonWriter.writeNameRaw(this.nameWithColonUTF8);
               return;
            }

            if (jsonWriter.utf16) {
               jsonWriter.writeNameRaw(this.nameWithColonUTF16);
               return;
            }
         }

         jsonWriter.writeName(this.fieldName);
         jsonWriter.writeColon();
      }
   }

   private boolean writeFieldNameSymbol(JSONWriter jsonWriter, SymbolTable symbolTable) {
      int symbolTableIdentity = System.identityHashCode(symbolTable);
      int symbol;
      if (this.nameSymbolCache == 0L) {
         symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
         this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
      } else if ((int)this.nameSymbolCache == symbolTableIdentity) {
         symbol = (int)(this.nameSymbolCache >> 32);
      } else {
         symbol = symbolTable.getOrdinalByHashCode(this.hashCode);
         this.nameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
      }

      if (symbol != -1) {
         jsonWriter.writeSymbol(-symbol);
         return true;
      } else {
         return false;
      }
   }

   public final JSONWriter.Path getRootParentPath() {
      return this.rootParentPath;
   }

   public final JSONWriter.Path getPath(JSONWriter.Path parent) {
      if (this.path == null) {
         return this.path = new JSONWriter.Path(parent, this.fieldName);
      } else {
         return this.path.parent == parent ? this.path : new JSONWriter.Path(parent, this.fieldName);
      }
   }

   public Type getItemType() {
      return null;
   }

   public Class getItemClass() {
      return null;
   }

   @Override
   public String toString() {
      return this.fieldName;
   }

   void setDefaultValue(T object) {
      Object fieldValue = null;
      if (!Iterable.class.isAssignableFrom(this.fieldClass) && !Map.class.isAssignableFrom(this.fieldClass)) {
         if (this.field != null && object != null) {
            try {
               this.field.setAccessible(true);
               fieldValue = this.field.get(object);
            } catch (Throwable var4) {
            }
         }

         if (fieldValue != null) {
            if (this.fieldClass == boolean.class) {
               if (fieldValue == Boolean.FALSE) {
                  return;
               }
            } else if (this.fieldClass != byte.class
               && this.fieldClass != short.class
               && this.fieldClass != int.class
               && this.fieldClass != long.class
               && this.fieldClass != float.class
               && this.fieldClass != double.class) {
               if (this.fieldClass == char.class && (Character)fieldValue == 0) {
                  return;
               }
            } else if (((Number)fieldValue).doubleValue() == 0.0) {
               return;
            }

            this.defaultValue = fieldValue;
         }
      }
   }

   public Object getFieldValue(T object) {
      if (object == null) {
         throw new JSONException("field.get error, " + this.fieldName);
      } else if (this.field == null) {
         throw new UnsupportedOperationException();
      } else {
         try {
            Object value;
            if (this.fieldOffset != -1L && !this.primitive) {
               value = JDKUtils.UNSAFE.getObject(object, this.fieldOffset);
            } else {
               value = this.field.get(object);
            }

            return value;
         } catch (IllegalAccessException | IllegalArgumentException var3) {
            throw new JSONException("field.get error, " + this.fieldName, var3);
         }
      }
   }

   @Override
   public int compareTo(Object o) {
      FieldWriter other = (FieldWriter)o;
      int thisOrdinal = this.ordinal;
      int otherOrdinal = other.ordinal;
      if (thisOrdinal < otherOrdinal) {
         return -1;
      } else if (thisOrdinal > otherOrdinal) {
         return 1;
      } else {
         int nameCompare = this.fieldName.compareTo(other.fieldName);
         if (nameCompare != 0) {
            return nameCompare;
         } else {
            Member thisMember;
            if (this.method != null && (this.field == null || !Modifier.isPublic(this.field.getModifiers()))) {
               thisMember = this.method;
            } else {
               thisMember = this.field;
            }

            Member otherMember;
            if (other.method != null && (other.field == null || !Modifier.isPublic(other.field.getModifiers()))) {
               otherMember = other.method;
            } else {
               otherMember = other.field;
            }

            if (thisMember != null && otherMember != null) {
               Class otherDeclaringClass = otherMember.getDeclaringClass();
               Class thisDeclaringClass = thisMember.getDeclaringClass();
               if (thisDeclaringClass != otherDeclaringClass) {
                  if (thisDeclaringClass.isAssignableFrom(otherDeclaringClass)) {
                     return 1;
                  }

                  if (otherDeclaringClass.isAssignableFrom(thisDeclaringClass)) {
                     return -1;
                  }
               }

               JSONField thisField = null;
               JSONField otherField = null;
               if (thisMember instanceof Field) {
                  thisField = ((Field)thisMember).getAnnotation(JSONField.class);
               } else if (thisMember instanceof Method) {
                  thisField = ((Method)thisMember).getAnnotation(JSONField.class);
               }

               if (otherMember instanceof Field) {
                  otherField = ((Field)otherMember).getAnnotation(JSONField.class);
               } else if (thisMember instanceof Method) {
                  otherField = ((Method)otherMember).getAnnotation(JSONField.class);
               }

               if (thisField != null && otherField == null) {
                  return -1;
               }

               if (thisField == null && otherField != null) {
                  return 1;
               }
            }

            if (thisMember instanceof Field && otherMember instanceof Method && ((Field)thisMember).getType() == ((Method)otherMember).getReturnType()) {
               return -1;
            } else if (thisMember instanceof Method && otherMember instanceof Field && ((Method)thisMember).getReturnType() == ((Field)otherMember).getType()) {
               return 1;
            } else {
               Class otherFieldClass = other.fieldClass;
               Class thisFieldClass = this.fieldClass;
               if (thisFieldClass != otherFieldClass && thisFieldClass != null && otherFieldClass != null) {
                  if (thisFieldClass.isAssignableFrom(otherFieldClass)) {
                     return 1;
                  }

                  if (otherFieldClass.isAssignableFrom(thisFieldClass)) {
                     return -1;
                  }
               }

               if (thisFieldClass == boolean.class && otherFieldClass != boolean.class) {
                  return 1;
               } else {
                  if (thisFieldClass == Boolean.class && otherFieldClass == Boolean.class && thisMember instanceof Method && otherMember instanceof Method) {
                     String thisMethodName = thisMember.getName();
                     String otherMethodName = otherMember.getName();
                     if (thisMethodName.startsWith("is") && otherMethodName.startsWith("get")) {
                        return 1;
                     }

                     if (thisMethodName.startsWith("get") && otherMethodName.startsWith("is")) {
                        return -1;
                     }
                  }

                  if (thisMember instanceof Method && otherMember instanceof Method) {
                     String thisMethodNamex = thisMember.getName();
                     String otherMethodNamex = otherMember.getName();
                     if (!thisMethodNamex.equals(otherMethodNamex)) {
                        String thisSetterName = BeanUtils.getterName(thisMethodNamex, null);
                        String otherSetterName = BeanUtils.getterName(otherMethodNamex, null);
                        if (this.fieldName.equals(thisSetterName) && !other.fieldName.equals(otherSetterName)) {
                           return 1;
                        }

                        if (this.fieldName.equals(otherSetterName) && !other.fieldName.equals(thisSetterName)) {
                           return -1;
                        }
                     }
                  }

                  if (thisFieldClass.isPrimitive() && !otherFieldClass.isPrimitive()) {
                     return -1;
                  } else if (!thisFieldClass.isPrimitive() && otherFieldClass.isPrimitive()) {
                     return 1;
                  } else if (thisFieldClass.getName().startsWith("java.") && !otherFieldClass.getName().startsWith("java.")) {
                     return -1;
                  } else {
                     return !thisFieldClass.getName().startsWith("java.") && otherFieldClass.getName().startsWith("java.") ? 1 : nameCompare;
                  }
               }
            }
         }
      }
   }

   public void writeEnum(JSONWriter jsonWriter, Enum e) {
      this.writeFieldName(jsonWriter);
      jsonWriter.writeEnum(e);
   }

   public void writeBinary(JSONWriter jsonWriter, byte[] value) {
      if (value == null) {
         if (jsonWriter.isWriteNulls()) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
         }
      } else {
         this.writeFieldName(jsonWriter);
         if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else if (!"base64".equals(this.format)
            && (this.format != null || (jsonWriter.getFeatures(this.features) & JSONWriter.Feature.WriteByteArrayAsBase64.mask) == 0L)) {
            if ("hex".equals(this.format)) {
               jsonWriter.writeHex(value);
            } else if (!"gzip,base64".equals(this.format) && !"gzip".equals(this.format)) {
               jsonWriter.writeBinary(value);
            } else {
               GZIPOutputStream gzipOut = null;

               try {
                  ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                  if (value.length < 512) {
                     gzipOut = new GZIPOutputStream(byteOut, value.length);
                  } else {
                     gzipOut = new GZIPOutputStream(byteOut);
                  }

                  gzipOut.write(value);
                  gzipOut.finish();
                  value = byteOut.toByteArray();
               } catch (IOException var8) {
                  throw new JSONException("write gzipBytes error", var8);
               } finally {
                  IOUtils.close(gzipOut);
               }

               jsonWriter.writeBase64(value);
            }
         } else {
            jsonWriter.writeBase64(value);
         }
      }
   }

   public void writeInt16(JSONWriter jsonWriter, short[] value) {
      if (value != null || jsonWriter.isWriteNulls()) {
         this.writeFieldName(jsonWriter);
         if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeInt16(value);
         }
      }
   }

   public void writeInt32(JSONWriter jsonWriter, int value) {
      this.writeFieldName(jsonWriter);
      jsonWriter.writeInt32(value);
   }

   public void writeInt64(JSONWriter jsonWriter, long value) {
      this.writeFieldName(jsonWriter);
      if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
         jsonWriter.writeString(Long.toString(value));
      } else {
         jsonWriter.writeInt64(value);
      }
   }

   public void writeString(JSONWriter jsonWriter, String value) {
      this.writeFieldName(jsonWriter);
      if (value == null && (this.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) != 0L) {
         jsonWriter.writeString("");
      } else {
         if (this.trim && value != null) {
            value = value.trim();
         }

         if (this.symbol && jsonWriter.jsonb) {
            jsonWriter.writeSymbol(value);
         } else if (this.raw) {
            jsonWriter.writeRaw(value);
         } else {
            jsonWriter.writeString(value);
         }
      }
   }

   public void writeString(JSONWriter jsonWriter, char[] value) {
      if (value != null || jsonWriter.isWriteNulls()) {
         this.writeFieldName(jsonWriter);
         if (value == null) {
            jsonWriter.writeStringNull();
         } else {
            jsonWriter.writeString(value, 0, value.length);
         }
      }
   }

   public void writeFloat(JSONWriter jsonWriter, float value) {
      this.writeFieldName(jsonWriter);
      if (this.decimalFormat != null) {
         jsonWriter.writeFloat(value, this.decimalFormat);
      } else if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
         jsonWriter.writeString(Float.toString(value));
      } else {
         jsonWriter.writeFloat(value);
      }
   }

   public void writeDouble(JSONWriter jsonWriter, double value) {
      this.writeFieldName(jsonWriter);
      if (this.decimalFormat != null) {
         jsonWriter.writeDouble(value, this.decimalFormat);
      } else {
         boolean writeNonStringValueAsString = (this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         if (writeNonStringValueAsString) {
            jsonWriter.writeString(Double.toString(value));
         } else {
            jsonWriter.writeDouble(value);
         }
      }
   }

   public void writeBool(JSONWriter jsonWriter, boolean value) {
      throw new UnsupportedOperationException();
   }

   public void writeBool(JSONWriter jsonWriter, boolean[] value) {
      if (value != null || jsonWriter.isWriteNulls()) {
         this.writeFieldName(jsonWriter);
         if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeBool(value);
         }
      }
   }

   public void writeFloat(JSONWriter jsonWriter, float[] value) {
      if (value != null || jsonWriter.isWriteNulls()) {
         this.writeFieldName(jsonWriter);
         if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeFloat(value);
         }
      }
   }

   public void writeDouble(JSONWriter jsonWriter, double[] value) {
      if (value != null || jsonWriter.isWriteNulls()) {
         this.writeFieldName(jsonWriter);
         if ((this.features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeDouble(value);
         }
      }
   }

   public void writeDouble(JSONWriter jsonWriter, Double value) {
      if (value == null) {
         long features = jsonWriter.getFeatures(this.features);
         if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L && (features & JSONWriter.Feature.NotWriteDefaultValue.mask) == 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeNumberNull();
         }
      } else {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeDouble(value);
      }
   }

   public void writeDate(JSONWriter jsonWriter, boolean writeFieldName, Date value) {
      if (value == null) {
         if (writeFieldName) {
            this.writeFieldName(jsonWriter);
         }

         jsonWriter.writeNull();
      } else {
         this.writeDate(jsonWriter, writeFieldName, value.getTime());
      }
   }

   public void writeDate(JSONWriter jsonWriter, long millis) {
      this.writeDate(jsonWriter, true, millis);
   }

   public void writeDate(JSONWriter jsonWriter, boolean writeFieldName, long millis) {
      if (jsonWriter.jsonb) {
         jsonWriter.writeMillis(millis);
      } else {
         int SECONDS_PER_DAY = 86400;
         JSONWriter.Context ctx = jsonWriter.context;
         if (!this.isDateFormatMillis() && !ctx.isDateFormatMillis()) {
            ZoneId zoneId = ctx.getZoneId();
            String dateFormat = ctx.getDateFormat();
            if (dateFormat == null) {
               Instant instant = Instant.ofEpochMilli(millis);
               long epochSecond = instant.getEpochSecond();
               ZoneOffset offset = zoneId.getRules().getOffset(instant);
               long localSecond = epochSecond + (long)offset.getTotalSeconds();
               long localEpochDay = Math.floorDiv(localSecond, 86400L);
               int secsOfDay = (int)Math.floorMod(localSecond, 86400L);
               int DAYS_PER_CYCLE = 146097;
               long DAYS_0000_TO_1970 = 719528L;
               long zeroDay = localEpochDay + 719528L;
               zeroDay -= 60L;
               long adjust = 0L;
               if (zeroDay < 0L) {
                  long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
                  adjust = adjustCycles * 400L;
                  zeroDay += -adjustCycles * 146097L;
               }

               long yearEst = (400L * zeroDay + 591L) / 146097L;
               long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
               if (doyEst < 0L) {
                  yearEst--;
                  doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
               }

               yearEst += adjust;
               int marchDoy0 = (int)doyEst;
               int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
               int month = (marchMonth0 + 2) % 12 + 1;
               int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
               yearEst += (long)(marchMonth0 / 10);
               int year = ChronoField.YEAR.checkValidIntValue(yearEst);
               int MINUTES_PER_HOUR = 60;
               int SECONDS_PER_MINUTE = 60;
               int SECONDS_PER_HOUR = 3600;
               long secondOfDay = (long)secsOfDay;
               ChronoField.SECOND_OF_DAY.checkValidValue(secondOfDay);
               int hours = (int)(secondOfDay / 3600L);
               secondOfDay -= (long)(hours * 3600);
               int minutes = (int)(secondOfDay / 60L);
               secondOfDay -= (long)(minutes * 60);
               int second = (int)secondOfDay;
               if (writeFieldName) {
                  this.writeFieldName(jsonWriter);
               }

               jsonWriter.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
            } else {
               ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
               if (this.isDateFormatISO8601() || ctx.isDateFormatISO8601()) {
                  int year = zdt.getYear();
                  int month = zdt.getMonthValue();
                  int dayOfMonth = zdt.getDayOfMonth();
                  int hour = zdt.getHour();
                  int minute = zdt.getMinute();
                  int second = zdt.getSecond();
                  int milliSeconds = zdt.getNano() / 1000000;
                  int offsetSeconds = zdt.getOffset().getTotalSeconds();
                  jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, milliSeconds, offsetSeconds, true);
                  return;
               }

               String str = ctx.getDateFormatter().format(zdt);
               if (writeFieldName) {
                  this.writeFieldName(jsonWriter);
               }

               jsonWriter.writeString(str);
            }
         } else {
            if (writeFieldName) {
               this.writeFieldName(jsonWriter);
            }

            jsonWriter.writeInt64(millis);
         }
      }
   }

   public ObjectWriter getItemWriter(JSONWriter writer, Type itemType) {
      return writer.getObjectWriter(itemType, null);
   }

   public abstract void writeValue(JSONWriter var1, T var2);

   public abstract boolean write(JSONWriter var1, T var2);

   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      if (valueClass == Float[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Float.class, this.decimalFormat) : ObjectWriterArrayFinal.FLOAT_ARRAY;
      } else if (valueClass == Double[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(Double.class, this.decimalFormat) : ObjectWriterArrayFinal.DOUBLE_ARRAY;
      } else if (valueClass == BigDecimal[].class) {
         return this.decimalFormat != null ? new ObjectWriterArrayFinal(BigDecimal.class, this.decimalFormat) : ObjectWriterArrayFinal.DECIMAL_ARRAY;
      } else {
         return jsonWriter.getObjectWriter(valueClass);
      }
   }

   public void writeListValueJSONB(JSONWriter jsonWriter, List list) {
      throw new UnsupportedOperationException();
   }

   public void writeListValue(JSONWriter jsonWriter, List list) {
      throw new UnsupportedOperationException();
   }

   public void writeListJSONB(JSONWriter jsonWriter, List list) {
      throw new UnsupportedOperationException();
   }

   public void writeList(JSONWriter jsonWriter, List list) {
      throw new UnsupportedOperationException();
   }

   public void writeListStr(JSONWriter jsonWriter, boolean writeFieldName, List<String> list) {
      throw new UnsupportedOperationException();
   }

   static ObjectWriter getObjectWriter(Type fieldType, Class fieldClass, String format, Locale locale, Class valueClass) {
      if (Map.class.isAssignableFrom(valueClass)) {
         return fieldClass.isAssignableFrom(valueClass) ? ObjectWriterImplMap.of(fieldType, format, valueClass) : ObjectWriterImplMap.of(valueClass);
      } else if (Calendar.class.isAssignableFrom(valueClass)) {
         return format != null && !format.isEmpty() ? new ObjectWriterImplCalendar(format, locale) : ObjectWriterImplCalendar.INSTANCE;
      } else if (ZonedDateTime.class.isAssignableFrom(valueClass)) {
         return format != null && !format.isEmpty() ? new ObjectWriterImplZonedDateTime(format, locale) : ObjectWriterImplZonedDateTime.INSTANCE;
      } else if (OffsetDateTime.class.isAssignableFrom(valueClass)) {
         return format != null && !format.isEmpty() ? ObjectWriterImplOffsetDateTime.of(format, locale) : ObjectWriterImplOffsetDateTime.INSTANCE;
      } else if (LocalDateTime.class.isAssignableFrom(valueClass)) {
         ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(LocalDateTime.class);
         if (objectWriter != null && objectWriter != ObjectWriterImplLocalDateTime.INSTANCE) {
            return objectWriter;
         } else {
            return format != null && !format.isEmpty() ? new ObjectWriterImplLocalDateTime(format, locale) : ObjectWriterImplLocalDateTime.INSTANCE;
         }
      } else if (LocalDate.class.isAssignableFrom(valueClass)) {
         ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(LocalDate.class);
         return (ObjectWriter)(objectWriter != null && objectWriter != ObjectWriterImplLocalDate.INSTANCE
            ? objectWriter
            : ObjectWriterImplLocalDate.of(format, locale));
      } else if (LocalTime.class.isAssignableFrom(valueClass)) {
         ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(LocalTime.class);
         if (objectWriter != null && objectWriter != ObjectWriterImplLocalTime.INSTANCE) {
            return objectWriter;
         } else {
            return format != null && !format.isEmpty() ? new ObjectWriterImplLocalTime(format, locale) : ObjectWriterImplLocalTime.INSTANCE;
         }
      } else if (Instant.class == valueClass) {
         return format != null && !format.isEmpty() ? new ObjectWriterImplInstant(format, locale) : ObjectWriterImplInstant.INSTANCE;
      } else if (BigDecimal.class == valueClass) {
         return format != null && !format.isEmpty() ? new ObjectWriterImplBigDecimal(new DecimalFormat(format), null) : ObjectWriterImplBigDecimal.INSTANCE;
      } else if (BigDecimal[].class == valueClass) {
         return format != null && !format.isEmpty()
            ? new ObjectWriterArrayFinal(BigDecimal.class, new DecimalFormat(format))
            : new ObjectWriterArrayFinal(BigDecimal.class, null);
      } else if (Optional.class == valueClass) {
         return ObjectWriterImplOptional.of(format, locale);
      } else {
         String className = valueClass.getName();
         switch (className) {
            case "java.sql.Time":
               return JdbcSupport.createTimeWriter(format);
            case "java.sql.Date":
               return new ObjectWriterImplDate(format, locale);
            case "java.sql.Timestamp":
               return JdbcSupport.createTimestampWriter(valueClass, format);
            case "org.joda.time.LocalDate":
               return JodaSupport.createLocalDateWriter(valueClass, format);
            case "org.joda.time.LocalDateTime":
               return JodaSupport.createLocalDateTimeWriter(valueClass, format);
            default:
               return null;
         }
      }
   }

   public Function getFunction() {
      return null;
   }
}
