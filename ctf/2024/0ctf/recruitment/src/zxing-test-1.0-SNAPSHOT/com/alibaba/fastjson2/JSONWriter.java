package com.alibaba.fastjson2;

import com.alibaba.fastjson2.filter.AfterFilter;
import com.alibaba.fastjson2.filter.BeforeFilter;
import com.alibaba.fastjson2.filter.ContextNameFilter;
import com.alibaba.fastjson2.filter.ContextValueFilter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.LabelFilter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;

public abstract class JSONWriter implements Closeable {
   static final long WRITE_ARRAY_NULL_MASK = JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask;
   static final char[] DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
   public final JSONWriter.Context context;
   public final boolean utf8;
   public final boolean utf16;
   public final boolean jsonb;
   public final boolean useSingleQuote;
   public final SymbolTable symbolTable;
   protected final Charset charset;
   protected final char quote;
   protected final int maxArraySize;
   protected boolean startObject;
   protected int level;
   protected int off;
   protected Object rootObject;
   protected IdentityHashMap<Object, JSONWriter.Path> refs;
   protected JSONWriter.Path path;
   protected String lastReference;
   protected boolean pretty;
   protected int indent;
   protected Object attachment;

   protected JSONWriter(JSONWriter.Context context, SymbolTable symbolTable, boolean jsonb, Charset charset) {
      this.context = context;
      this.symbolTable = symbolTable;
      this.charset = charset;
      this.jsonb = jsonb;
      this.utf8 = !jsonb && charset == StandardCharsets.UTF_8;
      this.utf16 = !jsonb && charset == StandardCharsets.UTF_16;
      this.useSingleQuote = !jsonb && (context.features & JSONWriter.Feature.UseSingleQuotes.mask) != 0L;
      this.quote = (char)(this.useSingleQuote ? 39 : 34);
      this.maxArraySize = (context.features & JSONWriter.Feature.LargeObject.mask) != 0L ? 1073741824 : 67108864;
      this.pretty = (context.features & JSONWriter.Feature.PrettyFormat.mask) != 0L;
   }

   public final Charset getCharset() {
      return this.charset;
   }

   public final boolean isUTF8() {
      return this.utf8;
   }

   public final boolean isUTF16() {
      return this.utf16;
   }

   public final boolean isIgnoreNoneSerializable() {
      return (this.context.features & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L;
   }

   public final boolean isIgnoreNoneSerializable(Object object) {
      return (this.context.features & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L
         && object != null
         && !Serializable.class.isAssignableFrom(object.getClass());
   }

   public final SymbolTable getSymbolTable() {
      return this.symbolTable;
   }

   public final void config(JSONWriter.Feature... features) {
      this.context.config(features);
   }

   public final void config(JSONWriter.Feature feature, boolean state) {
      this.context.config(feature, state);
   }

   public final JSONWriter.Context getContext() {
      return this.context;
   }

   public final int level() {
      return this.level;
   }

   public final void setRootObject(Object rootObject) {
      this.rootObject = rootObject;
      this.path = JSONWriter.Path.ROOT;
   }

   public final String setPath(String name, Object object) {
      if (!this.isRefDetect(object)) {
         return null;
      } else {
         this.path = new JSONWriter.Path(this.path, name);
         JSONWriter.Path previous;
         if (object == this.rootObject) {
            previous = JSONWriter.Path.ROOT;
         } else if (this.refs == null || (previous = this.refs.get(object)) == null) {
            if (this.refs == null) {
               this.refs = new IdentityHashMap<>(8);
            }

            this.refs.put(object, this.path);
            return null;
         }

         return previous.toString();
      }
   }

   public final String setPath(FieldWriter fieldWriter, Object object) {
      if (!this.isRefDetect(object)) {
         return null;
      } else {
         this.path = this.path == JSONWriter.Path.ROOT ? fieldWriter.getRootParentPath() : fieldWriter.getPath(this.path);
         JSONWriter.Path previous;
         if (object == this.rootObject) {
            previous = JSONWriter.Path.ROOT;
         } else if (this.refs == null || (previous = this.refs.get(object)) == null) {
            if (this.refs == null) {
               this.refs = new IdentityHashMap<>(8);
            }

            this.refs.put(object, this.path);
            return null;
         }

         return previous.toString();
      }
   }

   public final void addManagerReference(Object object) {
      if (this.refs == null) {
         this.refs = new IdentityHashMap<>(8);
      }

      this.refs.putIfAbsent(object, JSONWriter.Path.MANGER_REFERNCE);
   }

   public final boolean writeReference(int index, Object object) {
      String refPath = this.setPath(index, object);
      if (refPath != null) {
         this.writeReference(refPath);
         this.popPath(object);
         return true;
      } else {
         return false;
      }
   }

   public final String setPath(int index, Object object) {
      if (!this.isRefDetect(object)) {
         return null;
      } else {
         this.path = index == 0
            ? (this.path.child0 != null ? this.path.child0 : (this.path.child0 = new JSONWriter.Path(this.path, index)))
            : (
               index == 1
                  ? (this.path.child1 != null ? this.path.child1 : (this.path.child1 = new JSONWriter.Path(this.path, index)))
                  : new JSONWriter.Path(this.path, index)
            );
         JSONWriter.Path previous;
         if (object == this.rootObject) {
            previous = JSONWriter.Path.ROOT;
         } else if (this.refs == null || (previous = this.refs.get(object)) == null) {
            if (this.refs == null) {
               this.refs = new IdentityHashMap<>(8);
            }

            this.refs.put(object, this.path);
            return null;
         }

         return previous.toString();
      }
   }

   public final void popPath(Object object) {
      if (this.isRefDetect(object)) {
         if (this.path != null
            && (this.context.features & JSONWriter.Feature.ReferenceDetection.mask) != 0L
            && object != Collections.EMPTY_LIST
            && object != Collections.EMPTY_SET) {
            this.path = this.path.parent;
         }
      }
   }

   public final boolean hasFilter() {
      return this.context.hasFilter;
   }

   public final boolean hasFilter(long feature) {
      return this.context.hasFilter || (this.context.features & feature) != 0L;
   }

   public final boolean hasFilter(boolean containsNoneFieldGetter) {
      return this.context.hasFilter || containsNoneFieldGetter && (this.context.features & JSONWriter.Feature.IgnoreNonFieldGetter.mask) != 0L;
   }

   public final boolean isWriteNulls() {
      return (this.context.features & JSONWriter.Feature.WriteNulls.mask) != 0L;
   }

   public final boolean isRefDetect() {
      return (this.context.features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
   }

   public final boolean isUseSingleQuotes() {
      return this.useSingleQuote;
   }

   public final boolean isRefDetect(Object object) {
      return (this.context.features & JSONWriter.Feature.ReferenceDetection.mask) != 0L
         && object != null
         && !ObjectWriterProvider.isNotReferenceDetect(object.getClass());
   }

   public final boolean containsReference(Object value) {
      return this.refs != null && this.refs.containsKey(value);
   }

   public final String getPath(Object value) {
      JSONWriter.Path path;
      return this.refs != null && (path = this.refs.get(value)) != null ? path.toString() : "$";
   }

   public String getPath() {
      return this.path == null ? null : this.path.toString();
   }

   public final boolean removeReference(Object value) {
      return this.refs != null && this.refs.remove(value) != null;
   }

   public final boolean isBeanToArray() {
      return (this.context.features & JSONWriter.Feature.BeanToArray.mask) != 0L;
   }

   public final boolean isEnabled(JSONWriter.Feature feature) {
      return (this.context.features & feature.mask) != 0L;
   }

   public final boolean isEnabled(long feature) {
      return (this.context.features & feature) != 0L;
   }

   public final long getFeatures() {
      return this.context.features;
   }

   public final long getFeatures(long features) {
      return this.context.features | features;
   }

   public final boolean isIgnoreErrorGetter() {
      return (this.context.features & JSONWriter.Feature.IgnoreErrorGetter.mask) != 0L;
   }

   public final boolean isWriteTypeInfo(Object object, Class fieldClass) {
      long features = this.context.features;
      if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
         return false;
      } else if (object == null) {
         return false;
      } else {
         Class objectClass = object.getClass();
         if (objectClass == fieldClass) {
            return false;
         } else {
            return (features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) == 0L
                  || objectClass != HashMap.class && objectClass != ArrayList.class
               ? (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject
               : false;
         }
      }
   }

   public final boolean isWriteTypeInfo(Object object, Type fieldType) {
      long features = this.context.features;
      if ((features & JSONWriter.Feature.WriteClassName.mask) != 0L && object != null) {
         Class objectClass = object.getClass();
         Class fieldClass = null;
         if (fieldType instanceof Class) {
            fieldClass = (Class)fieldType;
         } else if (fieldType instanceof GenericArrayType) {
            if (isWriteTypeInfoGenericArray((GenericArrayType)fieldType, objectClass)) {
               return false;
            }
         } else if (fieldType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType)fieldType).getRawType();
            if (rawType instanceof Class) {
               fieldClass = (Class)rawType;
            }
         }

         if (objectClass == fieldClass) {
            return false;
         } else {
            return (features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) == 0L
                  || objectClass != HashMap.class && objectClass != ArrayList.class
               ? (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject
               : false;
         }
      } else {
         return false;
      }
   }

   private static boolean isWriteTypeInfoGenericArray(GenericArrayType fieldType, Class objectClass) {
      Type componentType = fieldType.getGenericComponentType();
      if (componentType instanceof ParameterizedType) {
         componentType = ((ParameterizedType)componentType).getRawType();
      }

      return objectClass.isArray() ? objectClass.getComponentType().equals(componentType) : false;
   }

   public final boolean isWriteTypeInfo(Object object) {
      long features = this.context.features;
      if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
         return false;
      } else {
         if ((features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) != 0L && object != null) {
            Class objectClass = object.getClass();
            if (objectClass == HashMap.class || objectClass == ArrayList.class) {
               return false;
            }
         }

         return (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject;
      }
   }

   public final boolean isWriteTypeInfo(Object object, Type fieldType, long features) {
      features |= this.context.features;
      if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
         return false;
      } else if (object == null) {
         return false;
      } else {
         Class objectClass = object.getClass();
         Class fieldClass = null;
         if (fieldType instanceof Class) {
            fieldClass = (Class)fieldType;
         } else if (fieldType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType)fieldType).getRawType();
            if (rawType instanceof Class) {
               fieldClass = (Class)rawType;
            }
         }

         if (objectClass == fieldClass) {
            return false;
         } else {
            if ((features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) != 0L) {
               if (objectClass == HashMap.class) {
                  if (fieldClass == null || fieldClass == Object.class || fieldClass == Map.class || fieldClass == AbstractMap.class) {
                     return false;
                  }
               } else if (objectClass == ArrayList.class) {
                  return false;
               }
            }

            return (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject;
         }
      }
   }

   public final boolean isWriteTypeInfo(Object object, Class fieldClass, long features) {
      if (object == null) {
         return false;
      } else {
         Class objectClass = object.getClass();
         if (objectClass == fieldClass) {
            return false;
         } else {
            features |= this.context.features;
            if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
               return false;
            } else {
               if ((features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) != 0L) {
                  if (objectClass == HashMap.class) {
                     if (fieldClass == null || fieldClass == Object.class || fieldClass == Map.class || fieldClass == AbstractMap.class) {
                        return false;
                     }
                  } else if (objectClass == ArrayList.class) {
                     return false;
                  }
               }

               return (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject;
            }
         }
      }
   }

   public final boolean isWriteMapTypeInfo(Object object, Class fieldClass, long features) {
      if (object == null) {
         return false;
      } else {
         Class objectClass = object.getClass();
         if (objectClass == fieldClass) {
            return false;
         } else {
            features |= this.context.features;
            if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
               return false;
            } else {
               return (features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) != 0L && objectClass == HashMap.class
                  ? false
                  : (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject;
            }
         }
      }
   }

   public final boolean isWriteTypeInfo(Object object, long features) {
      features |= this.context.features;
      if ((features & JSONWriter.Feature.WriteClassName.mask) == 0L) {
         return false;
      } else {
         if ((features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) != 0L && object != null) {
            Class objectClass = object.getClass();
            if (objectClass == HashMap.class || objectClass == ArrayList.class) {
               return false;
            }
         }

         return (features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject;
      }
   }

   public final ObjectWriter getObjectWriter(Class objectClass) {
      boolean fieldBased = (this.context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
      return this.context.provider.getObjectWriter(objectClass, objectClass, fieldBased);
   }

   public final ObjectWriter getObjectWriter(Class objectClass, String format) {
      boolean fieldBased = (this.context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
      return this.context.provider.getObjectWriter(objectClass, objectClass, format, fieldBased);
   }

   public final ObjectWriter getObjectWriter(Type objectType, Class objectClass) {
      boolean fieldBased = (this.context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
      return this.context.provider.getObjectWriter(objectType, objectClass, fieldBased);
   }

   public static JSONWriter of() {
      JSONWriter.Context writeContext = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider);
      JSONWriter jsonWriter;
      if (JDKUtils.JVM_VERSION == 8) {
         if (JDKUtils.FIELD_STRING_VALUE != null && !JDKUtils.ANDROID && !JDKUtils.OPENJ9) {
            jsonWriter = new JSONWriterUTF16JDK8UF(writeContext);
         } else {
            jsonWriter = new JSONWriterUTF16JDK8(writeContext);
         }
      } else if ((JSONFactory.defaultWriterFeatures & JSONWriter.Feature.OptimizedForAscii.mask) != 0L) {
         if (JDKUtils.STRING_VALUE != null) {
            if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
               jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(writeContext);
            } else {
               jsonWriter = new JSONWriterUTF8JDK9(writeContext);
            }
         } else {
            jsonWriter = new JSONWriterUTF8(writeContext);
         }
      } else if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16 != null) {
         jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16.apply(writeContext);
      } else if (JDKUtils.FIELD_STRING_VALUE != null && JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null) {
         jsonWriter = new JSONWriterUTF16JDK9UF(writeContext);
      } else {
         jsonWriter = new JSONWriterUTF16(writeContext);
      }

      return jsonWriter;
   }

   public static JSONWriter of(ObjectWriterProvider provider, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(provider);
      context.config(features);
      return of(context);
   }

   public static JSONWriter of(JSONWriter.Context context) {
      if (context == null) {
         context = JSONFactory.createWriteContext();
      }

      JSONWriter jsonWriter;
      if (JDKUtils.JVM_VERSION == 8) {
         if (JDKUtils.FIELD_STRING_VALUE != null && !JDKUtils.ANDROID && !JDKUtils.OPENJ9) {
            jsonWriter = new JSONWriterUTF16JDK8UF(context);
         } else {
            jsonWriter = new JSONWriterUTF16JDK8(context);
         }
      } else if ((context.features & JSONWriter.Feature.OptimizedForAscii.mask) != 0L) {
         if (JDKUtils.STRING_VALUE != null) {
            if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
               jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(context);
            } else {
               jsonWriter = new JSONWriterUTF8JDK9(context);
            }
         } else {
            jsonWriter = new JSONWriterUTF8(context);
         }
      } else if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16 != null) {
         jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16.apply(context);
      } else {
         jsonWriter = new JSONWriterUTF16(context);
      }

      return jsonWriter;
   }

   public static JSONWriter of(JSONWriter.Feature... features) {
      JSONWriter.Context writeContext = JSONFactory.createWriteContext(features);
      JSONWriter jsonWriter;
      if (JDKUtils.JVM_VERSION == 8) {
         if (JDKUtils.FIELD_STRING_VALUE != null && !JDKUtils.ANDROID && !JDKUtils.OPENJ9) {
            jsonWriter = new JSONWriterUTF16JDK8UF(writeContext);
         } else {
            jsonWriter = new JSONWriterUTF16JDK8(writeContext);
         }
      } else if ((writeContext.features & JSONWriter.Feature.OptimizedForAscii.mask) != 0L) {
         if (JDKUtils.STRING_VALUE != null) {
            if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
               jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(writeContext);
            } else {
               jsonWriter = new JSONWriterUTF8JDK9(writeContext);
            }
         } else {
            jsonWriter = new JSONWriterUTF8(writeContext);
         }
      } else if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16 != null) {
         jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16.apply(writeContext);
      } else {
         jsonWriter = new JSONWriterUTF16(writeContext);
      }

      return jsonWriter;
   }

   public static JSONWriter ofUTF16(JSONWriter.Feature... features) {
      JSONWriter.Context writeContext = JSONFactory.createWriteContext(features);
      JSONWriter jsonWriter;
      if (JDKUtils.JVM_VERSION == 8) {
         if (JDKUtils.FIELD_STRING_VALUE != null && !JDKUtils.ANDROID && !JDKUtils.OPENJ9) {
            jsonWriter = new JSONWriterUTF16JDK8UF(writeContext);
         } else {
            jsonWriter = new JSONWriterUTF16JDK8(writeContext);
         }
      } else if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16 != null) {
         jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF16.apply(writeContext);
      } else {
         jsonWriter = new JSONWriterUTF16(writeContext);
      }

      return jsonWriter;
   }

   public static JSONWriter ofJSONB() {
      return new JSONWriterJSONB(new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider), null);
   }

   public static JSONWriter ofJSONB(JSONWriter.Context context) {
      return new JSONWriterJSONB(context, null);
   }

   public static JSONWriter ofJSONB(JSONWriter.Context context, SymbolTable symbolTable) {
      return new JSONWriterJSONB(context, symbolTable);
   }

   public static JSONWriter ofJSONB(JSONWriter.Feature... features) {
      return new JSONWriterJSONB(new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features), null);
   }

   public static JSONWriter ofJSONB(SymbolTable symbolTable) {
      return new JSONWriterJSONB(new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider), symbolTable);
   }

   public static JSONWriter ofPretty() {
      return of(JSONWriter.Feature.PrettyFormat);
   }

   public static JSONWriter ofPretty(JSONWriter writer) {
      if (!writer.pretty) {
         writer.pretty = true;
         writer.context.features = writer.context.features | JSONWriter.Feature.PrettyFormat.mask;
      }

      return writer;
   }

   public static JSONWriter ofUTF8() {
      JSONWriter.Context context = JSONFactory.createWriteContext();
      JSONWriter jsonWriter;
      if (JDKUtils.STRING_VALUE != null) {
         if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
            jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(context);
         } else {
            jsonWriter = new JSONWriterUTF8JDK9(context);
         }
      } else {
         jsonWriter = new JSONWriterUTF8(context);
      }

      return jsonWriter;
   }

   public static JSONWriter ofUTF8(JSONWriter.Context context) {
      JSONWriter jsonWriter;
      if (JDKUtils.STRING_VALUE != null) {
         if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
            jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(context);
         } else {
            jsonWriter = new JSONWriterUTF8JDK9(context);
         }
      } else {
         jsonWriter = new JSONWriterUTF8(context);
      }

      return jsonWriter;
   }

   public static JSONWriter ofUTF8(JSONWriter.Feature... features) {
      JSONWriter.Context context = JSONFactory.createWriteContext(features);
      JSONWriter jsonWriter;
      if (JDKUtils.STRING_VALUE != null) {
         if (JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 != null) {
            jsonWriter = JSONFactory.INCUBATOR_VECTOR_WRITER_CREATOR_UTF8.apply(context);
         } else {
            jsonWriter = new JSONWriterUTF8JDK9(context);
         }
      } else {
         jsonWriter = new JSONWriterUTF8(context);
      }

      return jsonWriter;
   }

   public void writeBinary(byte[] bytes) {
      if (bytes == null) {
         this.writeArrayNull();
      } else if ((this.context.features & JSONWriter.Feature.WriteByteArrayAsBase64.mask) != 0L) {
         this.writeBase64(bytes);
      } else {
         this.startArray();

         for (int i = 0; i < bytes.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeInt32(bytes[i]);
         }

         this.endArray();
      }
   }

   public abstract void writeBase64(byte[] var1);

   public abstract void writeHex(byte[] var1);

   protected abstract void write0(char var1);

   public abstract void writeRaw(String var1);

   public abstract void writeRaw(byte[] var1);

   public void writeRaw(byte b) {
      throw new JSONException("UnsupportedOperation");
   }

   public void writeNameRaw(byte[] bytes, int offset, int len) {
      throw new JSONException("UnsupportedOperation");
   }

   public final void writeRaw(char[] chars) {
      this.writeRaw(chars, 0, chars.length);
   }

   public void writeRaw(char[] chars, int off, int charslen) {
      throw new JSONException("UnsupportedOperation");
   }

   public abstract void writeChar(char var1);

   public abstract void writeRaw(char var1);

   public void writeRaw(char c0, char c1) {
      throw new JSONException("UnsupportedOperation");
   }

   public abstract void writeNameRaw(byte[] var1);

   public abstract void writeName2Raw(long var1);

   public abstract void writeName3Raw(long var1);

   public abstract void writeName4Raw(long var1);

   public abstract void writeName5Raw(long var1);

   public abstract void writeName6Raw(long var1);

   public abstract void writeName7Raw(long var1);

   public abstract void writeName8Raw(long var1);

   public abstract void writeName9Raw(long var1, int var3);

   public abstract void writeName10Raw(long var1, long var3);

   public abstract void writeName11Raw(long var1, long var3);

   public abstract void writeName12Raw(long var1, long var3);

   public abstract void writeName13Raw(long var1, long var3);

   public abstract void writeName14Raw(long var1, long var3);

   public abstract void writeName15Raw(long var1, long var3);

   public abstract void writeName16Raw(long var1, long var3);

   public void writeSymbol(int symbol) {
      throw new JSONException("UnsupportedOperation");
   }

   public void writeNameRaw(byte[] name, long nameHash) {
      throw new JSONException("UnsupportedOperation");
   }

   protected static boolean isWriteAsString(long value, long features) {
      return (features & (JSONWriter.Feature.WriteNonStringValueAsString.mask | JSONWriter.Feature.WriteLongAsString.mask)) != 0L
         ? true
         : (features & JSONWriter.Feature.BrowserCompatible.mask) != 0L && !TypeUtils.isJavaScriptSupport(value);
   }

   protected static boolean isWriteAsString(BigInteger value, long features) {
      return (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L
         ? true
         : (features & JSONWriter.Feature.BrowserCompatible.mask) != 0L && !TypeUtils.isJavaScriptSupport(value);
   }

   protected static boolean isWriteAsString(BigDecimal value, long features) {
      return (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L
         ? true
         : (features & JSONWriter.Feature.BrowserCompatible.mask) != 0L && value.precision() >= 16 && !TypeUtils.isJavaScriptSupport(value.unscaledValue());
   }

   public abstract void writeNameRaw(char[] var1);

   public abstract void writeNameRaw(char[] var1, int var2, int var3);

   public void writeName(String name) {
      if (this.startObject) {
         this.startObject = false;
      } else {
         this.writeComma();
      }

      boolean unquote = (this.context.features & JSONWriter.Feature.UnquoteFieldName.mask) != 0L;
      if (unquote && (name.indexOf(this.quote) >= 0 || name.indexOf(92) >= 0)) {
         unquote = false;
      }

      if (unquote) {
         this.writeRaw(name);
      } else {
         this.writeString(name);
      }
   }

   public final void writeNameValue(String name, Object value) {
      this.writeName(name);
      this.writeAny(value);
   }

   public final void writeName(long name) {
      if (this.startObject) {
         this.startObject = false;
      } else {
         this.writeComma();
      }

      this.writeInt64(name);
   }

   public final void writeName(int name) {
      if (this.startObject) {
         this.startObject = false;
      } else {
         this.writeComma();
      }

      this.writeInt32(name);
   }

   public void writeNameAny(Object name) {
      if (this.startObject) {
         this.startObject = false;
      } else {
         this.writeComma();
      }

      this.writeAny(name);
   }

   public abstract void startObject();

   public abstract void endObject();

   public abstract void startArray();

   public void startArray(int size) {
      throw new JSONException("UnsupportedOperation");
   }

   public void startArray0() {
      this.startArray(0);
   }

   public void startArray1() {
      this.startArray(1);
   }

   public void startArray2() {
      this.startArray(2);
   }

   public void startArray3() {
      this.startArray(3);
   }

   public void startArray4() {
      this.startArray(4);
   }

   public void startArray5() {
      this.startArray(5);
   }

   public void startArray6() {
      this.startArray(6);
   }

   public void startArray7() {
      this.startArray(7);
   }

   public void startArray8() {
      this.startArray(8);
   }

   public void startArray9() {
      this.startArray(9);
   }

   public void startArray10() {
      this.startArray(10);
   }

   public void startArray11() {
      this.startArray(11);
   }

   public void startArray12() {
      this.startArray(12);
   }

   public void startArray13() {
      this.startArray(13);
   }

   public void startArray14() {
      this.startArray(14);
   }

   public void startArray15() {
      this.startArray(15);
   }

   public void startArray(Object array, int size) {
      throw new JSONException("UnsupportedOperation");
   }

   public abstract void endArray();

   public abstract void writeComma();

   public abstract void writeColon();

   public void writeInt16(short[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeInt16(value[i]);
         }

         this.endArray();
      }
   }

   public abstract void writeInt8(byte var1);

   public abstract void writeInt8(byte[] var1);

   public abstract void writeInt16(short var1);

   public abstract void writeInt32(int[] var1);

   public abstract void writeInt32(int var1);

   public abstract void writeInt32(Integer var1);

   public final void writeInt32(int value, DecimalFormat format) {
      if (format != null && !this.jsonb) {
         this.writeString(format.format((long)value));
      } else {
         this.writeInt32(value);
      }
   }

   public final void writeInt32(int value, String format) {
      if (format != null && !this.jsonb) {
         this.writeString(String.format(format, value));
      } else {
         this.writeInt32(value);
      }
   }

   public abstract void writeInt64(long var1);

   public abstract void writeInt64(Long var1);

   public void writeMillis(long i) {
      this.writeInt64(i);
   }

   public abstract void writeInt64(long[] var1);

   public abstract void writeListInt64(List<Long> var1);

   public abstract void writeListInt32(List<Integer> var1);

   public abstract void writeFloat(float var1);

   public final void writeFloat(float value, DecimalFormat format) {
      if (format == null || this.jsonb) {
         this.writeFloat(value);
      } else if (!Float.isNaN(value) && !Float.isInfinite(value)) {
         String str = format.format((double)value);
         this.writeRaw(str);
      } else {
         this.writeNull();
      }
   }

   public abstract void writeFloat(float[] var1);

   public final void writeFloat(float[] value, DecimalFormat format) {
      if (format != null && !this.jsonb) {
         if (value == null) {
            this.writeNull();
         } else {
            this.startArray();

            for (int i = 0; i < value.length; i++) {
               if (i != 0) {
                  this.writeComma();
               }

               String str = format.format((double)value[i]);
               this.writeRaw(str);
            }

            this.endArray();
         }
      } else {
         this.writeFloat(value);
      }
   }

   public final void writeFloat(Float value) {
      if (value == null) {
         this.writeNumberNull();
      } else {
         this.writeDouble((double)value.floatValue());
      }
   }

   public abstract void writeDouble(double var1);

   public final void writeDouble(double value, DecimalFormat format) {
      if (format == null || this.jsonb) {
         this.writeDouble(value);
      } else if (!Double.isNaN(value) && !Double.isInfinite(value)) {
         String str = format.format(value);
         this.writeRaw(str);
      } else {
         this.writeNull();
      }
   }

   public void writeDoubleArray(double value0, double value1) {
      this.startArray();
      this.writeDouble(value0);
      this.writeComma();
      this.writeDouble(value1);
      this.endArray();
   }

   public final void writeDouble(double[] value, DecimalFormat format) {
      if (format != null && !this.jsonb) {
         if (value == null) {
            this.writeNull();
         } else {
            this.startArray();

            for (int i = 0; i < value.length; i++) {
               if (i != 0) {
                  this.writeComma();
               }

               String str = format.format(value[i]);
               this.writeRaw(str);
            }

            this.endArray();
         }
      } else {
         this.writeDouble(value);
      }
   }

   public abstract void writeDouble(double[] var1);

   public abstract void writeBool(boolean var1);

   public void writeBool(boolean[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeBool(value[i]);
         }

         this.endArray();
      }
   }

   public abstract void writeNull();

   public void writeStringNull() {
      String raw;
      if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) != 0L) {
         raw = (this.context.features & JSONWriter.Feature.UseSingleQuotes.mask) != 0L ? "''" : "\"\"";
      } else {
         raw = "null";
      }

      this.writeRaw(raw);
   }

   public void writeArrayNull() {
      String raw;
      if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask)) != 0L) {
         raw = "[]";
      } else {
         raw = "null";
      }

      this.writeRaw(raw);
   }

   public final void writeNumberNull() {
      if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask)) != 0L) {
         this.writeInt32(0);
      } else {
         this.writeNull();
      }
   }

   public final void writeInt64Null() {
      if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullNumberAsZero.mask)) != 0L) {
         this.writeInt64(0L);
      } else {
         this.writeNull();
      }
   }

   public final void writeBooleanNull() {
      if ((this.context.features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullBooleanAsFalse.mask)) != 0L) {
         this.writeBool(false);
      } else {
         this.writeNull();
      }
   }

   public final void writeDecimal(BigDecimal value) {
      this.writeDecimal(value, 0L, null);
   }

   public final void writeDecimal(BigDecimal value, long features) {
      this.writeDecimal(value, features, null);
   }

   public abstract void writeDecimal(BigDecimal var1, long var2, DecimalFormat var4);

   public void writeEnum(Enum e) {
      if (e == null) {
         this.writeNull();
      } else {
         if ((this.context.features & JSONWriter.Feature.WriteEnumUsingToString.mask) != 0L) {
            this.writeString(e.toString());
         } else if ((this.context.features & JSONWriter.Feature.WriteEnumsUsingName.mask) != 0L) {
            this.writeString(e.name());
         } else {
            this.writeInt32(e.ordinal());
         }
      }
   }

   public final void writeBigInt(BigInteger value) {
      this.writeBigInt(value, 0L);
   }

   public abstract void writeBigInt(BigInteger var1, long var2);

   public abstract void writeUUID(UUID var1);

   public final void checkAndWriteTypeName(Object object, Class fieldClass) {
      long features = this.context.features;
      Class objectClass;
      if ((features & JSONWriter.Feature.WriteClassName.mask) != 0L
         && object != null
         && (objectClass = object.getClass()) != fieldClass
         && ((features & JSONWriter.Feature.NotWriteHashMapArrayListClassName.mask) == 0L || objectClass != HashMap.class && objectClass != ArrayList.class)
         && ((features & JSONWriter.Feature.NotWriteRootClassName.mask) == 0L || object != this.rootObject)) {
         this.writeTypeName(TypeUtils.getTypeName(objectClass));
      }
   }

   public void writeTypeName(String typeName) {
      throw new JSONException("UnsupportedOperation");
   }

   public boolean writeTypeName(byte[] typeName, long typeNameHash) {
      throw new JSONException("UnsupportedOperation");
   }

   public final void writeString(Reader reader) {
      this.writeRaw(this.quote);

      try {
         char[] chars = new char[2048];

         while (true) {
            int len = reader.read(chars, 0, chars.length);
            if (len < 0) {
               break;
            }

            if (len > 0) {
               this.writeString(chars, 0, len, false);
            }
         }
      } catch (Exception var4) {
         throw new JSONException("read string from reader error", var4);
      }

      this.writeRaw(this.quote);
   }

   public abstract void writeString(String var1);

   public abstract void writeString(boolean var1);

   public abstract void writeString(byte var1);

   public abstract void writeString(short var1);

   public void writeString(boolean[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(byte[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(short[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(int[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(long[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(float[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public void writeString(double[] value) {
      if (value == null) {
         this.writeArrayNull();
      } else {
         this.startArray();

         for (int i = 0; i < value.length; i++) {
            if (i != 0) {
               this.writeComma();
            }

            this.writeString(value[i]);
         }

         this.endArray();
      }
   }

   public abstract void writeString(int var1);

   public void writeString(float value) {
      this.writeString(Float.toString(value));
   }

   public void writeString(double value) {
      this.writeString(Double.toString(value));
   }

   public abstract void writeString(long var1);

   public abstract void writeStringLatin1(byte[] var1);

   public abstract void writeStringUTF16(byte[] var1);

   public void writeString(List<String> list) {
      this.startArray();
      int i = 0;

      for (int size = list.size(); i < size; i++) {
         if (i != 0) {
            this.writeComma();
         }

         String str = list.get(i);
         this.writeString(str);
      }

      this.endArray();
   }

   public abstract void writeString(String[] var1);

   public void writeSymbol(String string) {
      this.writeString(string);
   }

   public abstract void writeString(char[] var1);

   public abstract void writeString(char[] var1, int var2, int var3);

   public abstract void writeString(char[] var1, int var2, int var3, boolean var4);

   public abstract void writeLocalDate(LocalDate var1);

   protected final boolean writeLocalDateWithFormat(LocalDate date) {
      JSONWriter.Context context = this.context;
      if (!context.dateFormatUnixTime && !context.dateFormatMillis) {
         DateTimeFormatter formatter = context.getDateFormatter();
         if (formatter != null) {
            String str;
            if (context.isDateFormatHasHour()) {
               str = formatter.format(LocalDateTime.of(date, LocalTime.MIN));
            } else {
               str = formatter.format(date);
            }

            this.writeString(str);
            return true;
         } else {
            return false;
         }
      } else {
         LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.MIN);
         long millis = dateTime.atZone(context.getZoneId()).toInstant().toEpochMilli();
         this.writeInt64(context.dateFormatMillis ? millis : millis / 1000L);
         return true;
      }
   }

   public abstract void writeLocalDateTime(LocalDateTime var1);

   public abstract void writeLocalTime(LocalTime var1);

   public abstract void writeZonedDateTime(ZonedDateTime var1);

   public abstract void writeOffsetDateTime(OffsetDateTime var1);

   public abstract void writeOffsetTime(OffsetTime var1);

   public void writeInstant(Instant instant) {
      if (instant == null) {
         this.writeNull();
      } else {
         String str = DateTimeFormatter.ISO_INSTANT.format(instant);
         this.writeString(str);
      }
   }

   public abstract void writeDateTime14(int var1, int var2, int var3, int var4, int var5, int var6);

   public abstract void writeDateTime19(int var1, int var2, int var3, int var4, int var5, int var6);

   public abstract void writeDateTimeISO8601(int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9);

   public abstract void writeDateYYYMMDD8(int var1, int var2, int var3);

   public abstract void writeDateYYYMMDD10(int var1, int var2, int var3);

   public abstract void writeTimeHHMMSS8(int var1, int var2, int var3);

   public abstract void write(List var1);

   public void write(Map map) {
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
            this.write0('{');
            boolean first = true;

            for (Entry o : map.entrySet()) {
               if (!first) {
                  this.write0(',');
               }

               this.writeAny(o.getKey());
               this.write0(':');
               this.writeAny(o.getValue());
               first = false;
            }

            this.write0('}');
         }
      }
   }

   public abstract void write(JSONObject var1);

   public void writeAny(Object value) {
      if (value == null) {
         this.writeNull();
      } else {
         Class<?> valueClass = value.getClass();
         ObjectWriter objectWriter = this.context.getObjectWriter(valueClass, valueClass);
         objectWriter.write(this, value, null, null, 0L);
      }
   }

   public final void writeAs(Object value, Class type) {
      if (value == null) {
         this.writeNull();
      } else {
         ObjectWriter objectWriter = this.context.getObjectWriter(type);
         objectWriter.write(this, value, null, null, 0L);
      }
   }

   public abstract void writeReference(String var1);

   @Override
   public abstract void close();

   public abstract int size();

   public abstract byte[] getBytes();

   public abstract byte[] getBytes(Charset var1);

   public void flushTo(Writer to) {
      try {
         String json = this.toString();
         to.write(json);
         this.off = 0;
      } catch (IOException var3) {
         throw new JSONException("flushTo error", var3);
      }
   }

   public abstract int flushTo(OutputStream var1) throws IOException;

   public abstract int flushTo(OutputStream var1, Charset var2) throws IOException;

   protected static IllegalArgumentException illegalYear(int year) {
      return new IllegalArgumentException("Only 4 digits numbers are supported. Provided: " + year);
   }

   /** @deprecated */
   public final void incrementIndent() {
      this.indent++;
   }

   /** @deprecated */
   public final void decrementIdent() {
      this.indent--;
   }

   /** @deprecated */
   public void println() {
      this.writeChar('\n');

      for (int i = 0; i < this.indent; i++) {
         this.writeChar('\t');
      }
   }

   /** @deprecated */
   public final void writeReference(Object object) {
      if (this.refs != null) {
         JSONWriter.Path path = this.refs.get(object);
         if (path != null) {
            this.writeReference(path.toString());
         }
      }
   }

   public Object getAttachment() {
      return this.attachment;
   }

   public void setAttachment(Object attachment) {
      this.attachment = attachment;
   }

   public static final class Context {
      static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
      public final ObjectWriterProvider provider;
      DateTimeFormatter dateFormatter;
      String dateFormat;
      Locale locale;
      boolean dateFormatMillis;
      boolean dateFormatISO8601;
      boolean dateFormatUnixTime;
      boolean formatyyyyMMddhhmmss19;
      boolean formatHasDay;
      boolean formatHasHour;
      long features;
      ZoneId zoneId;
      int maxLevel = 2048;
      boolean hasFilter;
      PropertyPreFilter propertyPreFilter;
      PropertyFilter propertyFilter;
      NameFilter nameFilter;
      ValueFilter valueFilter;
      BeforeFilter beforeFilter;
      AfterFilter afterFilter;
      LabelFilter labelFilter;
      ContextValueFilter contextValueFilter;
      ContextNameFilter contextNameFilter;

      public Context(ObjectWriterProvider provider) {
         if (provider == null) {
            throw new IllegalArgumentException("objectWriterProvider must not null");
         } else {
            this.features = JSONFactory.defaultWriterFeatures;
            this.provider = provider;
            this.zoneId = JSONFactory.defaultWriterZoneId;
            String format = JSONFactory.defaultWriterFormat;
            if (format != null) {
               this.setDateFormat(format);
            }
         }
      }

      public Context(JSONWriter.Feature... features) {
         this.features = JSONFactory.defaultWriterFeatures;
         this.provider = JSONFactory.getDefaultObjectWriterProvider();
         this.zoneId = JSONFactory.defaultWriterZoneId;
         String format = JSONFactory.defaultWriterFormat;
         if (format != null) {
            this.setDateFormat(format);
         }

         for (int i = 0; i < features.length; i++) {
            this.features = this.features | features[i].mask;
         }
      }

      public Context(String format, JSONWriter.Feature... features) {
         this.features = JSONFactory.defaultWriterFeatures;
         this.provider = JSONFactory.getDefaultObjectWriterProvider();
         this.zoneId = JSONFactory.defaultWriterZoneId;

         for (int i = 0; i < features.length; i++) {
            this.features = this.features | features[i].mask;
         }

         if (format == null) {
            format = JSONFactory.defaultWriterFormat;
         }

         if (format != null) {
            this.setDateFormat(format);
         }
      }

      public Context(ObjectWriterProvider provider, JSONWriter.Feature... features) {
         if (provider == null) {
            throw new IllegalArgumentException("objectWriterProvider must not null");
         } else {
            this.features = JSONFactory.defaultWriterFeatures;
            this.provider = provider;
            this.zoneId = JSONFactory.defaultWriterZoneId;

            for (int i = 0; i < features.length; i++) {
               this.features = this.features | features[i].mask;
            }

            String format = JSONFactory.defaultWriterFormat;
            if (format != null) {
               this.setDateFormat(format);
            }
         }
      }

      public long getFeatures() {
         return this.features;
      }

      public void setFeatures(long features) {
         this.features = features;
      }

      public boolean isEnabled(JSONWriter.Feature feature) {
         return (this.features & feature.mask) != 0L;
      }

      public boolean isEnabled(long feature) {
         return (this.features & feature) != 0L;
      }

      public void config(JSONWriter.Feature... features) {
         for (int i = 0; i < features.length; i++) {
            this.features = this.features | features[i].mask;
         }
      }

      public void config(JSONWriter.Feature feature, boolean state) {
         if (state) {
            this.features = this.features | feature.mask;
         } else {
            this.features = this.features & ~feature.mask;
         }
      }

      public void configFilter(Filter... filters) {
         for (int i = 0; i < filters.length; i++) {
            Filter filter = filters[i];
            if (filter instanceof NameFilter) {
               if (this.nameFilter == null) {
                  this.nameFilter = (NameFilter)filter;
               } else {
                  this.nameFilter = NameFilter.compose(this.nameFilter, (NameFilter)filter);
               }
            }

            if (filter instanceof ValueFilter) {
               if (this.valueFilter == null) {
                  this.valueFilter = (ValueFilter)filter;
               } else {
                  this.valueFilter = ValueFilter.compose(this.valueFilter, (ValueFilter)filter);
               }
            }

            if (filter instanceof PropertyFilter) {
               this.propertyFilter = (PropertyFilter)filter;
            }

            if (filter instanceof PropertyPreFilter) {
               this.propertyPreFilter = (PropertyPreFilter)filter;
            }

            if (filter instanceof BeforeFilter) {
               this.beforeFilter = (BeforeFilter)filter;
            }

            if (filter instanceof AfterFilter) {
               this.afterFilter = (AfterFilter)filter;
            }

            if (filter instanceof LabelFilter) {
               this.labelFilter = (LabelFilter)filter;
            }

            if (filter instanceof ContextValueFilter) {
               this.contextValueFilter = (ContextValueFilter)filter;
            }

            if (filter instanceof ContextNameFilter) {
               this.contextNameFilter = (ContextNameFilter)filter;
            }
         }

         this.hasFilter = this.propertyPreFilter != null
            || this.propertyFilter != null
            || this.nameFilter != null
            || this.valueFilter != null
            || this.beforeFilter != null
            || this.afterFilter != null
            || this.labelFilter != null
            || this.contextValueFilter != null
            || this.contextNameFilter != null;
      }

      public <T> ObjectWriter<T> getObjectWriter(Class<T> objectType) {
         boolean fieldBased = (this.features & JSONWriter.Feature.FieldBased.mask) != 0L;
         return this.provider.getObjectWriter(objectType, objectType, fieldBased);
      }

      public <T> ObjectWriter<T> getObjectWriter(Type objectType, Class<T> objectClass) {
         boolean fieldBased = (this.features & JSONWriter.Feature.FieldBased.mask) != 0L;
         return this.provider.getObjectWriter(objectType, objectClass, fieldBased);
      }

      public ObjectWriterProvider getProvider() {
         return this.provider;
      }

      public ZoneId getZoneId() {
         if (this.zoneId == null) {
            this.zoneId = DEFAULT_ZONE_ID;
         }

         return this.zoneId;
      }

      public void setZoneId(ZoneId zoneId) {
         this.zoneId = zoneId;
      }

      public String getDateFormat() {
         return this.dateFormat;
      }

      public boolean isDateFormatMillis() {
         return this.dateFormatMillis;
      }

      public boolean isDateFormatUnixTime() {
         return this.dateFormatUnixTime;
      }

      public boolean isDateFormatISO8601() {
         return this.dateFormatISO8601;
      }

      public boolean isDateFormatHasDay() {
         return this.formatHasDay;
      }

      public boolean isDateFormatHasHour() {
         return this.formatHasHour;
      }

      public boolean isFormatyyyyMMddhhmmss19() {
         return this.formatyyyyMMddhhmmss19;
      }

      public DateTimeFormatter getDateFormatter() {
         if (this.dateFormatter == null && this.dateFormat != null && !this.dateFormatMillis && !this.dateFormatISO8601 && !this.dateFormatUnixTime) {
            this.dateFormatter = this.locale == null ? DateTimeFormatter.ofPattern(this.dateFormat) : DateTimeFormatter.ofPattern(this.dateFormat, this.locale);
         }

         return this.dateFormatter;
      }

      public void setDateFormat(String dateFormat) {
         if (dateFormat == null || !dateFormat.equals(this.dateFormat)) {
            this.dateFormatter = null;
         }

         if (dateFormat != null && !dateFormat.isEmpty()) {
            boolean dateFormatMillis = false;
            boolean dateFormatISO8601 = false;
            boolean dateFormatUnixTime = false;
            boolean formatHasDay = false;
            boolean formatHasHour = false;
            boolean formatyyyyMMddhhmmss19 = false;
            switch (dateFormat) {
               case "millis":
                  dateFormatMillis = true;
                  break;
               case "iso8601":
                  dateFormatMillis = false;
                  dateFormatISO8601 = true;
                  break;
               case "unixtime":
                  dateFormatMillis = false;
                  dateFormatUnixTime = true;
                  break;
               case "yyyy-MM-ddTHH:mm:ss":
                  dateFormat = "yyyy-MM-dd'T'HH:mm:ss";
                  formatHasDay = true;
                  formatHasHour = true;
                  break;
               case "yyyy-MM-dd HH:mm:ss":
                  formatyyyyMMddhhmmss19 = true;
                  formatHasDay = true;
                  formatHasHour = true;
                  break;
               default:
                  dateFormatMillis = false;
                  formatHasDay = dateFormat.contains("d");
                  formatHasHour = dateFormat.contains("H");
            }

            this.dateFormatMillis = dateFormatMillis;
            this.dateFormatISO8601 = dateFormatISO8601;
            this.dateFormatUnixTime = dateFormatUnixTime;
            this.formatHasDay = formatHasDay;
            this.formatHasHour = formatHasHour;
            this.formatyyyyMMddhhmmss19 = formatyyyyMMddhhmmss19;
         }

         this.dateFormat = dateFormat;
      }

      public PropertyPreFilter getPropertyPreFilter() {
         return this.propertyPreFilter;
      }

      public void setPropertyPreFilter(PropertyPreFilter propertyPreFilter) {
         this.propertyPreFilter = propertyPreFilter;
         if (propertyPreFilter != null) {
            this.hasFilter = true;
         }
      }

      public NameFilter getNameFilter() {
         return this.nameFilter;
      }

      public void setNameFilter(NameFilter nameFilter) {
         this.nameFilter = nameFilter;
         if (nameFilter != null) {
            this.hasFilter = true;
         }
      }

      public ValueFilter getValueFilter() {
         return this.valueFilter;
      }

      public void setValueFilter(ValueFilter valueFilter) {
         this.valueFilter = valueFilter;
         if (valueFilter != null) {
            this.hasFilter = true;
         }
      }

      public ContextValueFilter getContextValueFilter() {
         return this.contextValueFilter;
      }

      public void setContextValueFilter(ContextValueFilter contextValueFilter) {
         this.contextValueFilter = contextValueFilter;
         if (contextValueFilter != null) {
            this.hasFilter = true;
         }
      }

      public ContextNameFilter getContextNameFilter() {
         return this.contextNameFilter;
      }

      public void setContextNameFilter(ContextNameFilter contextNameFilter) {
         this.contextNameFilter = contextNameFilter;
         if (contextNameFilter != null) {
            this.hasFilter = true;
         }
      }

      public PropertyFilter getPropertyFilter() {
         return this.propertyFilter;
      }

      public void setPropertyFilter(PropertyFilter propertyFilter) {
         this.propertyFilter = propertyFilter;
         if (propertyFilter != null) {
            this.hasFilter = true;
         }
      }

      public AfterFilter getAfterFilter() {
         return this.afterFilter;
      }

      public void setAfterFilter(AfterFilter afterFilter) {
         this.afterFilter = afterFilter;
         if (afterFilter != null) {
            this.hasFilter = true;
         }
      }

      public BeforeFilter getBeforeFilter() {
         return this.beforeFilter;
      }

      public void setBeforeFilter(BeforeFilter beforeFilter) {
         this.beforeFilter = beforeFilter;
         if (beforeFilter != null) {
            this.hasFilter = true;
         }
      }

      public LabelFilter getLabelFilter() {
         return this.labelFilter;
      }

      public void setLabelFilter(LabelFilter labelFilter) {
         this.labelFilter = labelFilter;
         if (labelFilter != null) {
            this.hasFilter = true;
         }
      }
   }

   public static enum Feature {
      FieldBased(1L),
      IgnoreNoneSerializable(2L),
      ErrorOnNoneSerializable(4L),
      BeanToArray(8L),
      WriteNulls(16L),
      WriteMapNullValue(16L),
      BrowserCompatible(32L),
      NullAsDefaultValue(64L),
      WriteBooleanAsNumber(128L),
      WriteNonStringValueAsString(256L),
      WriteClassName(512L),
      NotWriteRootClassName(1024L),
      NotWriteHashMapArrayListClassName(2048L),
      NotWriteDefaultValue(4096L),
      WriteEnumsUsingName(8192L),
      WriteEnumUsingToString(16384L),
      IgnoreErrorGetter(32768L),
      PrettyFormat(65536L),
      ReferenceDetection(131072L),
      WriteNameAsSymbol(262144L),
      WriteBigDecimalAsPlain(524288L),
      UseSingleQuotes(1048576L),
      /** @deprecated */
      MapSortField(2097152L),
      WriteNullListAsEmpty(4194304L),
      WriteNullStringAsEmpty(8388608L),
      WriteNullNumberAsZero(16777216L),
      WriteNullBooleanAsFalse(33554432L),
      /** @deprecated */
      NotWriteEmptyArray(67108864L),
      IgnoreEmpty(67108864L),
      WriteNonStringKeyAsString(134217728L),
      WritePairAsJavaBean(268435456L),
      OptimizedForAscii(536870912L),
      EscapeNoneAscii(1073741824L),
      WriteByteArrayAsBase64(2147483648L),
      IgnoreNonFieldGetter(4294967296L),
      LargeObject(8589934592L),
      WriteLongAsString(17179869184L),
      BrowserSecure(34359738368L),
      WriteEnumUsingOrdinal(68719476736L),
      WriteThrowableClassName(137438953472L),
      UnquoteFieldName(274877906944L),
      NotWriteSetClassName(549755813888L),
      NotWriteNumberClassName(1099511627776L),
      SortMapEntriesByKeys(2199023255552L);

      public final long mask;

      private Feature(long mask) {
         this.mask = mask;
      }

      public boolean isEnabled(long features) {
         return (features & this.mask) != 0L;
      }
   }

   public static final class Path {
      public static final JSONWriter.Path ROOT = new JSONWriter.Path(null, "$");
      public static final JSONWriter.Path MANGER_REFERNCE = new JSONWriter.Path(null, "#");
      public final JSONWriter.Path parent;
      final String name;
      final int index;
      String fullPath;
      JSONWriter.Path child0;
      JSONWriter.Path child1;

      public Path(JSONWriter.Path parent, String name) {
         this.parent = parent;
         this.name = name;
         this.index = -1;
      }

      public Path(JSONWriter.Path parent, int index) {
         this.parent = parent;
         this.name = null;
         this.index = index;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            JSONWriter.Path path = (JSONWriter.Path)o;
            return this.index == path.index && Objects.equals(this.parent, path.parent) && Objects.equals(this.name, path.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.parent, this.name, this.index);
      }

      @Override
      public String toString() {
         if (this.fullPath != null) {
            return this.fullPath;
         } else {
            byte[] buf = new byte[16];
            int off = 0;
            int level = 0;
            JSONWriter.Path[] items = new JSONWriter.Path[4];

            for (JSONWriter.Path p = this; p != null; p = p.parent) {
               if (items.length == level) {
                  items = Arrays.copyOf(items, items.length + 4);
               }

               items[level] = p;
               level++;
            }

            boolean ascii = true;

            for (int i = level - 1; i >= 0; i--) {
               JSONWriter.Path item = items[i];
               String name = item.name;
               if (name == null) {
                  int intValue = item.index;
                  int intValueSize = IOUtils.stringSize(intValue);

                  while (off + intValueSize + 2 >= buf.length) {
                     int newCapacity = buf.length + (buf.length >> 1);
                     buf = Arrays.copyOf(buf, newCapacity);
                  }

                  buf[off++] = 91;
                  IOUtils.getChars(intValue, off + intValueSize, buf);
                  off += intValueSize;
                  buf[off++] = 93;
               } else {
                  if (off + 1 >= buf.length) {
                     int newCapacity = buf.length + (buf.length >> 1);
                     buf = Arrays.copyOf(buf, newCapacity);
                  }

                  if (i != level - 1) {
                     buf[off++] = 46;
                  }

                  if (JDKUtils.JVM_VERSION == 8) {
                     char[] chars = JDKUtils.getCharArray(name);

                     for (int j = 0; j < chars.length; j++) {
                        char ch = chars[j];
                        switch (ch) {
                           case '!':
                           case '"':
                           case '#':
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
                           case ':':
                           case ';':
                           case '<':
                           case '=':
                           case '>':
                           case '?':
                           case '@':
                           case '[':
                           case '\\':
                           case ']':
                           case '^':
                           case '`':
                           case '~':
                              if (off + 1 >= buf.length) {
                                 int newCapacity = buf.length + (buf.length >> 1);
                                 buf = Arrays.copyOf(buf, newCapacity);
                              }

                              buf[off] = 92;
                              buf[off + 1] = (byte)ch;
                              off += 2;
                              break;
                           case '$':
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
                           case '_':
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
                           case '{':
                           case '|':
                           case '}':
                           default:
                              if (ch >= 1 && ch <= 127) {
                                 if (off == buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 buf[off++] = (byte)ch;
                              } else if (ch >= '\ud800' && ch < '\ue000') {
                                 ascii = false;
                                 if (ch < '\udc00') {
                                    int uc;
                                    if (name.length() - i < 2) {
                                       uc = -1;
                                    } else {
                                       char d = name.charAt(i + 1);
                                       if (d < '\udc00' || d >= '\ue000') {
                                          buf[off++] = 63;
                                          continue;
                                       }

                                       uc = (ch << '\n') + d + -56613888;
                                    }

                                    if (uc < 0) {
                                       if (off == buf.length) {
                                          int newCapacity = buf.length + (buf.length >> 1);
                                          buf = Arrays.copyOf(buf, newCapacity);
                                       }

                                       buf[off++] = 63;
                                    } else {
                                       if (off + 3 >= buf.length) {
                                          int newCapacity = buf.length + (buf.length >> 1);
                                          buf = Arrays.copyOf(buf, newCapacity);
                                       }

                                       buf[off] = (byte)(240 | uc >> 18);
                                       buf[off + 1] = (byte)(128 | uc >> 12 & 63);
                                       buf[off + 2] = (byte)(128 | uc >> 6 & 63);
                                       buf[off + 3] = (byte)(128 | uc & 63);
                                       off += 4;
                                       j++;
                                    }
                                 } else {
                                    buf[off++] = 63;
                                 }
                              } else if (ch > 2047) {
                                 if (off + 2 >= buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 ascii = false;
                                 buf[off] = (byte)(224 | ch >> '\f' & 15);
                                 buf[off + 1] = (byte)(128 | ch >> 6 & 63);
                                 buf[off + 2] = (byte)(128 | ch & '?');
                                 off += 3;
                              } else {
                                 if (off + 1 >= buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 ascii = false;
                                 buf[off] = (byte)(192 | ch >> 6 & 31);
                                 buf[off + 1] = (byte)(128 | ch & '?');
                                 off += 2;
                              }
                        }
                     }
                  } else {
                     for (int j = 0; j < name.length(); j++) {
                        char ch = name.charAt(j);
                        switch (ch) {
                           case '!':
                           case '"':
                           case '#':
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
                           case ':':
                           case ';':
                           case '<':
                           case '=':
                           case '>':
                           case '?':
                           case '@':
                           case '[':
                           case '\\':
                           case ']':
                           case '^':
                           case '`':
                           case '~':
                              if (off + 1 >= buf.length) {
                                 int newCapacity = buf.length + (buf.length >> 1);
                                 buf = Arrays.copyOf(buf, newCapacity);
                              }

                              buf[off] = 92;
                              buf[off + 1] = (byte)ch;
                              off += 2;
                              break;
                           case '$':
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
                           case '_':
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
                           case '{':
                           case '|':
                           case '}':
                           default:
                              if (ch >= 1 && ch <= 127) {
                                 if (off == buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 buf[off++] = (byte)ch;
                              } else if (ch >= '\ud800' && ch < '\ue000') {
                                 ascii = false;
                                 if (ch < '\udc00') {
                                    int ucx;
                                    if (name.length() - i < 2) {
                                       ucx = -1;
                                    } else {
                                       char d = name.charAt(i + 1);
                                       if (d < '\udc00' || d >= '\ue000') {
                                          buf[off++] = 63;
                                          continue;
                                       }

                                       ucx = (ch << '\n') + d + -56613888;
                                    }

                                    if (ucx < 0) {
                                       if (off == buf.length) {
                                          int newCapacity = buf.length + (buf.length >> 1);
                                          buf = Arrays.copyOf(buf, newCapacity);
                                       }

                                       buf[off++] = 63;
                                    } else {
                                       if (off + 4 >= buf.length) {
                                          int newCapacity = buf.length + (buf.length >> 1);
                                          buf = Arrays.copyOf(buf, newCapacity);
                                       }

                                       buf[off] = (byte)(240 | ucx >> 18);
                                       buf[off + 1] = (byte)(128 | ucx >> 12 & 63);
                                       buf[off + 2] = (byte)(128 | ucx >> 6 & 63);
                                       buf[off + 3] = (byte)(128 | ucx & 63);
                                       off += 4;
                                       j++;
                                    }
                                 } else {
                                    buf[off++] = 63;
                                 }
                              } else if (ch > 2047) {
                                 if (off + 2 >= buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 ascii = false;
                                 buf[off] = (byte)(224 | ch >> '\f' & 15);
                                 buf[off + 1] = (byte)(128 | ch >> 6 & 63);
                                 buf[off + 2] = (byte)(128 | ch & '?');
                                 off += 3;
                              } else {
                                 if (off + 1 >= buf.length) {
                                    int newCapacity = buf.length + (buf.length >> 1);
                                    buf = Arrays.copyOf(buf, newCapacity);
                                 }

                                 ascii = false;
                                 buf[off] = (byte)(192 | ch >> 6 & 31);
                                 buf[off + 1] = (byte)(128 | ch & '?');
                                 off += 2;
                              }
                        }
                     }
                  }
               }
            }

            if (ascii) {
               if (JDKUtils.STRING_CREATOR_JDK11 != null) {
                  byte[] bytes;
                  if (off == buf.length) {
                     bytes = buf;
                  } else {
                     bytes = new byte[off];
                     System.arraycopy(buf, 0, bytes, 0, off);
                  }

                  return this.fullPath = JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
               }

               if (JDKUtils.STRING_CREATOR_JDK8 != null) {
                  char[] chars = new char[off];

                  for (int ix = 0; ix < off; ix++) {
                     chars[ix] = (char)buf[ix];
                  }

                  return this.fullPath = JDKUtils.STRING_CREATOR_JDK8.apply(chars, Boolean.TRUE);
               }
            }

            return this.fullPath = new String(buf, 0, off, ascii ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
         }
      }
   }
}
