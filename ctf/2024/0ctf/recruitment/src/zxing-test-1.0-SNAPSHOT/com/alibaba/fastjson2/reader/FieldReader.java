package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.JdbcSupport;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class FieldReader<T> implements Comparable<FieldReader> {
   public final int ordinal;
   public final String fieldName;
   public final Class fieldClass;
   public final Type fieldType;
   public final long features;
   public final String format;
   public final Method method;
   public final Field field;
   protected final long fieldOffset;
   public final Object defaultValue;
   public final Locale locale;
   public final JSONSchema schema;
   final boolean fieldClassSerializable;
   final long fieldNameHash;
   final long fieldNameHashLCase;
   volatile ObjectReader reader;
   volatile JSONPath referenceCache;
   final boolean noneStaticMemberClass;
   final boolean readOnly;
   Type itemType;
   Class itemClass;
   volatile ObjectReader itemReader;

   public FieldReader(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      Field field
   ) {
      this.fieldName = fieldName;
      this.fieldType = fieldType;
      this.fieldClass = fieldClass;
      this.fieldClassSerializable = fieldClass != null && (Serializable.class.isAssignableFrom(fieldClass) || Modifier.isInterface(fieldClass.getModifiers()));
      this.features = features;
      this.fieldNameHash = Fnv.hashCode64(fieldName);
      this.fieldNameHashLCase = Fnv.hashCode64LCase(fieldName);
      this.ordinal = ordinal;
      this.format = format;
      this.locale = locale;
      this.defaultValue = defaultValue;
      this.schema = schema;
      this.method = method;
      this.field = field;
      boolean readOnly = false;
      if (method != null && method.getParameterCount() == 0) {
         readOnly = true;
      } else if (field != null && Modifier.isFinal(field.getModifiers())) {
         readOnly = true;
      }

      this.readOnly = readOnly;
      long fieldOffset = -1L;
      if (field != null && (features & 36028797018963968L) == 0L) {
         fieldOffset = JDKUtils.UNSAFE.objectFieldOffset(field);
      }

      this.fieldOffset = fieldOffset;
      if (fieldOffset == -1L && field != null && method == null) {
         try {
            field.setAccessible(true);
         } catch (Throwable var17) {
            JDKUtils.setReflectErrorLast(var17);
         }
      }

      Class declaringClass = null;
      if (method != null) {
         declaringClass = method.getDeclaringClass();
      } else if (field != null) {
         declaringClass = field.getDeclaringClass();
      }

      this.noneStaticMemberClass = BeanUtils.isNoneStaticMemberClass(declaringClass, fieldClass);
   }

   public void acceptDefaultValue(T object) {
      if (this.defaultValue != null) {
         this.accept(object, this.defaultValue);
      }
   }

   public ObjectReader getObjectReader(JSONReader jsonReader) {
      return this.reader != null ? this.reader : (this.reader = jsonReader.getObjectReader(this.fieldType));
   }

   public ObjectReader getObjectReader(JSONReader.Context context) {
      return this.reader != null ? this.reader : (this.reader = context.getObjectReader(this.fieldType));
   }

   public ObjectReader getObjectReader(ObjectReaderProvider provider) {
      if (this.reader != null) {
         return this.reader;
      } else {
         boolean fieldBased = (this.features & JSONReader.Feature.FieldBased.mask) != 0L;
         return this.reader = provider.getObjectReader(this.fieldType, fieldBased);
      }
   }

   public Type getItemType() {
      return this.itemType;
   }

   public Class getItemClass() {
      if (this.itemType == null) {
         return null;
      } else {
         if (this.itemClass == null) {
            this.itemClass = TypeUtils.getClass(this.itemType);
         }

         return this.itemClass;
      }
   }

   public long getItemClassHash() {
      Class itemClass = this.getItemClass();
      return itemClass == null ? 0L : Fnv.hashCode64(itemClass.getName());
   }

   @Override
   public String toString() {
      Member member = (Member)(this.method != null ? this.method : this.field);
      return member != null ? member.getName() : this.fieldName;
   }

   public void addResolveTask(JSONReader jsonReader, Object object, String reference) {
      JSONPath path;
      if (this.referenceCache != null && this.referenceCache.toString().equals(reference)) {
         path = this.referenceCache;
      } else {
         path = this.referenceCache = JSONPath.of(reference);
      }

      jsonReader.addResolveTask(this, object, path);
   }

   public int compareTo(FieldReader o) {
      int nameCompare = this.fieldName.compareTo(o.fieldName);
      if (nameCompare != 0) {
         if (this.ordinal < o.ordinal) {
            return -1;
         } else {
            return this.ordinal > o.ordinal ? 1 : nameCompare;
         }
      } else {
         int cmp = this.isReadOnly() == o.isReadOnly() ? 0 : (this.isReadOnly() ? 1 : -1);
         if (cmp != 0) {
            return cmp;
         } else {
            Member thisMember = (Member)(this.field != null ? this.field : this.method);
            Member otherMember = (Member)(o.field != null ? o.field : o.method);
            if (thisMember != null && otherMember != null && thisMember.getClass() != otherMember.getClass()) {
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
            }

            if (this.field != null && o.field != null) {
               Class<?> thisDeclaringClass = this.field.getDeclaringClass();
               Class<?> otherDeclaringClass = o.field.getDeclaringClass();

               for (Class s = thisDeclaringClass.getSuperclass(); s != null && s != Object.class; s = s.getSuperclass()) {
                  if (s == otherDeclaringClass) {
                     return 1;
                  }
               }

               for (Class sx = otherDeclaringClass.getSuperclass(); sx != null && sx != Object.class; sx = sx.getSuperclass()) {
                  if (sx == thisDeclaringClass) {
                     return -1;
                  }
               }
            }

            if (this.method != null && o.method != null) {
               Class<?> thisDeclaringClass = this.method.getDeclaringClass();
               Class<?> otherDeclaringClass = o.method.getDeclaringClass();
               if (thisDeclaringClass != otherDeclaringClass) {
                  for (Class sxx = thisDeclaringClass.getSuperclass(); sxx != null && sxx != Object.class; sxx = sxx.getSuperclass()) {
                     if (sxx == otherDeclaringClass) {
                        return -1;
                     }
                  }

                  for (Class sxxx = otherDeclaringClass.getSuperclass(); sxxx != null && sxxx != Object.class; sxxx = sxxx.getSuperclass()) {
                     if (sxxx == thisDeclaringClass) {
                        return 1;
                     }
                  }
               }

               if (this.method.getParameterCount() == 1 && o.method.getParameterCount() == 1) {
                  Class<?> thisParamType = this.method.getParameterTypes()[0];
                  Class<?> otherParamType = o.method.getParameterTypes()[0];
                  if (thisParamType != otherParamType) {
                     if (thisParamType.isAssignableFrom(otherParamType)) {
                        return 1;
                     }

                     if (otherParamType.isAssignableFrom(thisParamType)) {
                        return -1;
                     }

                     if (this.needCompareToActualFieldClass(thisParamType) || this.needCompareToActualFieldClass(otherParamType)) {
                        Class actualFieldClass = null;

                        try {
                           actualFieldClass = thisDeclaringClass.getDeclaredField(this.fieldName).getType();
                           if (actualFieldClass == null) {
                              actualFieldClass = otherDeclaringClass.getDeclaredField(this.fieldName).getType();
                           }
                        } catch (NoSuchFieldException var14) {
                        }

                        if (actualFieldClass != null) {
                           for (Class sxxxx = thisParamType; sxxxx != null && sxxxx != Object.class; sxxxx = sxxxx.getSuperclass()) {
                              if (sxxxx == actualFieldClass) {
                                 return -1;
                              }
                           }

                           for (Class sxxxxx = otherParamType; sxxxxx != null && sxxxxx != Object.class; sxxxxx = sxxxxx.getSuperclass()) {
                              if (sxxxxx == actualFieldClass) {
                                 return 1;
                              }
                           }
                        }
                     }

                     JSONField thisAnnotation = BeanUtils.findAnnotation(this.method, JSONField.class);
                     JSONField otherAnnotation = BeanUtils.findAnnotation(o.method, JSONField.class);
                     boolean thisAnnotatedWithJsonFiled = thisAnnotation != null;
                     if (thisAnnotatedWithJsonFiled == (otherAnnotation == null)) {
                        return thisAnnotatedWithJsonFiled ? -1 : 1;
                     }
                  }
               }

               String thisMethodName = this.method.getName();
               String otherMethodName = o.method.getName();
               if (!thisMethodName.equals(otherMethodName)) {
                  boolean thisMethodNameSetStart = thisMethodName.startsWith("set");
                  if (thisMethodNameSetStart != otherMethodName.startsWith("set")) {
                     return thisMethodNameSetStart ? -1 : 1;
                  }

                  String thisName = BeanUtils.setterName(thisMethodName, null);
                  String otherName = BeanUtils.setterName(otherMethodName, null);
                  boolean thisFieldNameEquals = this.fieldName.equals(thisName);
                  if (thisFieldNameEquals != o.fieldName.equals(otherName)) {
                     return thisFieldNameEquals ? 1 : -1;
                  }
               }
            }

            ObjectReader thisInitReader = this.getInitReader();
            ObjectReader otherInitReader = o.getInitReader();
            if (thisInitReader != null && otherInitReader == null) {
               return -1;
            } else if (thisInitReader == null && otherInitReader != null) {
               return 1;
            } else {
               Class thisFieldClass = this.fieldClass;
               Class otherClass = o.fieldClass;
               boolean thisClassPrimitive = thisFieldClass.isPrimitive();
               boolean otherClassPrimitive = otherClass.isPrimitive();
               if (thisClassPrimitive && !otherClassPrimitive) {
                  return -1;
               } else if (!thisClassPrimitive && otherClassPrimitive) {
                  return 1;
               } else {
                  boolean thisClassStartsWithJava = thisFieldClass.getName().startsWith("java.");
                  boolean otherClassStartsWithJava = otherClass.getName().startsWith("java.");
                  if (thisClassStartsWithJava && !otherClassStartsWithJava) {
                     return -1;
                  } else {
                     return !thisClassStartsWithJava && otherClassStartsWithJava ? 1 : cmp;
                  }
               }
            }
         }
      }
   }

   public boolean isUnwrapped() {
      return (this.features & 562949953421312L) != 0L;
   }

   public void addResolveTask(JSONReader jsonReader, List object, int i, String reference) {
      jsonReader.addResolveTask(object, i, JSONPath.of(reference));
   }

   public void readFieldValueJSONB(JSONReader jsonReader, T object) {
      this.readFieldValue(jsonReader, object);
   }

   public abstract Object readFieldValue(JSONReader var1);

   public void accept(T object, boolean value) {
      this.accept(object, Boolean.valueOf(value));
   }

   public boolean supportAcceptType(Class valueClass) {
      return this.fieldClass == valueClass;
   }

   public void accept(T object, byte value) {
      this.accept(object, Byte.valueOf(value));
   }

   public void accept(T object, short value) {
      this.accept(object, Short.valueOf(value));
   }

   public void accept(T object, int value) {
      this.accept(object, Integer.valueOf(value));
   }

   public void accept(T object, long value) {
      this.accept(object, Long.valueOf(value));
   }

   public void accept(T object, char value) {
      this.accept(object, Character.valueOf(value));
   }

   public void accept(T object, float value) {
      this.accept(object, Float.valueOf(value));
   }

   public void accept(T object, double value) {
      this.accept(object, Double.valueOf(value));
   }

   public abstract void accept(T var1, Object var2);

   protected void acceptAny(T object, Object fieldValue, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      boolean autoCast = true;
      if (fieldValue != null) {
         Class<?> valueClass = fieldValue.getClass();
         if (!this.supportAcceptType(valueClass)) {
            if (valueClass == String.class) {
               if (this.fieldClass == Date.class) {
                  autoCast = false;
               }
            } else if (valueClass == Integer.class
               && (this.fieldClass == boolean.class || this.fieldClass == Boolean.class)
               && (features & JSONReader.Feature.NonZeroNumberCastToBooleanAsTrue.mask) != 0L) {
               int intValue = (Integer)fieldValue;
               fieldValue = intValue != 0;
            }

            if (valueClass != this.fieldClass && autoCast) {
               Function typeConvert = provider.getTypeConvert(valueClass, this.fieldClass);
               if (typeConvert != null) {
                  fieldValue = typeConvert.apply(fieldValue);
               }
            }
         }
      }

      Object typedFieldValue;
      if (fieldValue == null || this.fieldType == fieldValue.getClass() || this.fieldType == Object.class) {
         typedFieldValue = fieldValue;
      } else if (fieldValue instanceof JSONObject) {
         JSONReader.Feature[] toFeatures = (features & JSONReader.Feature.SupportSmartMatch.mask) != 0L
            ? new JSONReader.Feature[]{JSONReader.Feature.SupportSmartMatch}
            : new JSONReader.Feature[0];
         typedFieldValue = ((JSONObject)fieldValue).to(this.fieldType, toFeatures);
      } else if (fieldValue instanceof JSONArray) {
         typedFieldValue = ((JSONArray)fieldValue).to(this.fieldType, features);
      } else if ((features == 0L || features == JSONReader.Feature.SupportSmartMatch.mask) && !this.fieldClass.isInstance(fieldValue) && this.format == null) {
         ObjectReader initReader = this.getInitReader();
         if (initReader != null) {
            String fieldValueJson = JSON.toJSONString(fieldValue);
            typedFieldValue = initReader.readObject(JSONReader.of(fieldValueJson), this.fieldType, this.fieldName, features);
         } else {
            typedFieldValue = TypeUtils.cast(fieldValue, this.fieldType, provider);
         }
      } else if (autoCast) {
         String fieldValueJSONString = JSON.toJSONString(fieldValue);
         JSONReader.Context readContext = JSONFactory.createReadContext(features);
         JSONReader jsonReader = JSONReader.of(fieldValueJSONString, readContext);

         try {
            ObjectReader fieldObjectReader = this.getObjectReader(jsonReader);
            typedFieldValue = fieldObjectReader.readObject(jsonReader, null, this.fieldName, features);
         } catch (Throwable var14) {
            if (jsonReader != null) {
               try {
                  jsonReader.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (jsonReader != null) {
            jsonReader.close();
         }
      } else {
         typedFieldValue = fieldValue;
      }

      this.accept(object, typedFieldValue);
   }

   public abstract void readFieldValue(JSONReader var1, T var2);

   public ObjectReader checkObjectAutoType(JSONReader jsonReader) {
      return null;
   }

   public boolean isReadOnly() {
      return this.readOnly;
   }

   public ObjectReader getInitReader() {
      return null;
   }

   public void processExtra(JSONReader jsonReader, Object object) {
      jsonReader.skipValue();
   }

   public void acceptExtra(Object object, String name, Object value) {
   }

   public ObjectReader getItemObjectReader(JSONReader.Context ctx) {
      return this.itemReader != null ? this.itemReader : (this.itemReader = ctx.getObjectReader(this.itemType));
   }

   public ObjectReader getItemObjectReader(JSONReader jsonReader) {
      return this.getItemObjectReader(jsonReader.getContext());
   }

   static ObjectReader createFormattedObjectReader(Type fieldType, Class fieldClass, String format, Locale locale) {
      if (format != null && !format.isEmpty()) {
         String typeName = fieldType.getTypeName();
         switch (typeName) {
            case "java.sql.Time":
               return JdbcSupport.createTimeReader((Class)fieldType, format, locale);
            case "java.sql.Timestamp":
               return JdbcSupport.createTimestampReader((Class)fieldType, format, locale);
            case "java.sql.Date":
               return JdbcSupport.createDateReader((Class)fieldType, format, locale);
            case "byte[]":
            case "[B":
               return new ObjectReaderImplInt8Array(format);
            default:
               if (Calendar.class.isAssignableFrom(fieldClass)) {
                  return ObjectReaderImplCalendar.of(format, locale);
               }

               if (fieldClass == ZonedDateTime.class) {
                  return ObjectReaderImplZonedDateTime.of(format, locale);
               }

               if (fieldClass == LocalDateTime.class) {
                  return new ObjectReaderImplLocalDateTime(format, locale);
               }

               if (fieldClass == LocalDate.class) {
                  return ObjectReaderImplLocalDate.of(format, locale);
               }

               if (fieldClass == LocalTime.class) {
                  return new ObjectReaderImplLocalTime(format, locale);
               }

               if (fieldClass == Instant.class) {
                  return ObjectReaderImplInstant.of(format, locale);
               }

               if (fieldClass == OffsetTime.class) {
                  return ObjectReaderImplOffsetTime.of(format, locale);
               }

               if (fieldClass == OffsetDateTime.class) {
                  return ObjectReaderImplOffsetDateTime.of(format, locale);
               }

               if (fieldClass == Optional.class) {
                  return ObjectReaderImplOptional.of(fieldType, format, locale);
               }

               if (fieldClass == Date.class) {
                  return ObjectReaderImplDate.of(format, locale);
               }
         }
      }

      return null;
   }

   public BiConsumer getFunction() {
      return null;
   }

   public boolean sameTo(FieldReader other) {
      if (this.field != null) {
         String thisName = this.field.getName();
         if (other.field != null) {
            String otherName = other.field.getName();
            if (thisName.equals(otherName)) {
               return true;
            }
         }

         if (other.method != null) {
            String otherName = this.getActualFieldName(other);
            if (thisName.equals(otherName)) {
               return true;
            }
         }
      }

      if (this.method != null) {
         String thisNamex = this.getActualFieldName(this);
         if (other.method != null) {
            String otherName = this.getActualFieldName(other);
            if (thisNamex != null && thisNamex.equals(otherName)) {
               return true;
            }
         }

         if (other.field != null) {
            return thisNamex != null && thisNamex.equals(other.field.getName());
         }
      }

      return false;
   }

   public boolean belongTo(Class clazz) {
      return this.field != null && this.field.getDeclaringClass() == clazz || this.method != null && this.method.getDeclaringClass().isAssignableFrom(clazz);
   }

   private String getActualFieldName(FieldReader fieldReader) {
      String name = fieldReader.method.getName();
      return fieldReader.isReadOnly()
         ? BeanUtils.getterName(name, PropertyNamingStrategy.CamelCase.name())
         : BeanUtils.setterName(name, PropertyNamingStrategy.CamelCase.name());
   }

   private boolean needCompareToActualFieldClass(Class clazz) {
      return clazz.isEnum() || clazz.isInterface();
   }
}
