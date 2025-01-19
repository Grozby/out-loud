package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.SymbolTable;
import com.alibaba.fastjson2.filter.AfterFilter;
import com.alibaba.fastjson2.filter.BeanContext;
import com.alibaba.fastjson2.filter.BeforeFilter;
import com.alibaba.fastjson2.filter.ContextNameFilter;
import com.alibaba.fastjson2.filter.ContextValueFilter;
import com.alibaba.fastjson2.filter.LabelFilter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObjectWriterAdapter<T> implements ObjectWriter<T> {
   boolean hasFilter;
   PropertyPreFilter propertyPreFilter;
   PropertyFilter propertyFilter;
   NameFilter nameFilter;
   ValueFilter valueFilter;
   static final String TYPE = "@type";
   final Class objectClass;
   final List<FieldWriter> fieldWriters;
   protected final FieldWriter[] fieldWriterArray;
   final String typeKey;
   byte[] typeKeyJSONB;
   protected final String typeName;
   protected final long typeNameHash;
   protected long typeNameSymbolCache;
   protected final byte[] typeNameJSONB;
   byte[] nameWithColonUTF8;
   char[] nameWithColonUTF16;
   final long features;
   final long[] hashCodes;
   final short[] mapping;
   final boolean hasValueField;
   final boolean serializable;
   final boolean containsNoneFieldGetter;
   final boolean googleCollection;
   byte[] jsonbClassInfo;

   public ObjectWriterAdapter(Class<T> objectClass, List<FieldWriter> fieldWriters) {
      this(objectClass, null, null, 0L, fieldWriters);
   }

   public ObjectWriterAdapter(Class<T> objectClass, String typeKey, String typeName, long features, List<FieldWriter> fieldWriters) {
      if (typeName == null && objectClass != null) {
         if (Enum.class.isAssignableFrom(objectClass) && !objectClass.isEnum()) {
            typeName = objectClass.getSuperclass().getName();
         } else {
            typeName = TypeUtils.getTypeName(objectClass);
         }
      }

      this.objectClass = objectClass;
      this.typeKey = typeKey != null && !typeKey.isEmpty() ? typeKey : "@type";
      this.typeName = typeName;
      this.typeNameHash = typeName != null ? Fnv.hashCode64(typeName) : 0L;
      this.typeNameJSONB = JSONB.toBytes(typeName);
      this.features = features;
      this.fieldWriters = fieldWriters;
      this.serializable = objectClass == null || Serializable.class.isAssignableFrom(objectClass);
      this.googleCollection = "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList".equals(typeName)
         || "com.google.common.collect.AbstractMapBasedMultimap$WrappedSet".equals(typeName);
      this.fieldWriterArray = new FieldWriter[fieldWriters.size()];
      fieldWriters.toArray(this.fieldWriterArray);
      this.hasValueField = this.fieldWriterArray.length == 1 && (this.fieldWriterArray[0].features & 281474976710656L) != 0L;
      boolean containsNoneFieldGetter = false;
      long[] hashCodes = new long[this.fieldWriterArray.length];

      for (int i = 0; i < this.fieldWriterArray.length; i++) {
         FieldWriter fieldWriter = this.fieldWriterArray[i];
         long hashCode = Fnv.hashCode64(fieldWriter.fieldName);
         hashCodes[i] = hashCode;
         if (fieldWriter.method != null && (fieldWriter.features & 4503599627370496L) == 0L) {
            containsNoneFieldGetter = true;
         }
      }

      this.containsNoneFieldGetter = containsNoneFieldGetter;
      this.hashCodes = Arrays.copyOf(hashCodes, hashCodes.length);
      Arrays.sort(this.hashCodes);
      this.mapping = new short[this.hashCodes.length];

      for (int ix = 0; ix < hashCodes.length; ix++) {
         long hashCode = hashCodes[ix];
         int index = Arrays.binarySearch(this.hashCodes, hashCode);
         this.mapping[index] = (short)ix;
      }
   }

   @Override
   public long getFeatures() {
      return this.features;
   }

   @Override
   public FieldWriter getFieldWriter(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodes, hashCode);
      if (m < 0) {
         return null;
      } else {
         int index = this.mapping[m];
         return this.fieldWriterArray[index];
      }
   }

   @Override
   public final boolean hasFilter(JSONWriter jsonWriter) {
      return this.hasFilter || jsonWriter.hasFilter(this.containsNoneFieldGetter);
   }

   @Override
   public void setPropertyFilter(PropertyFilter propertyFilter) {
      this.propertyFilter = propertyFilter;
      if (propertyFilter != null) {
         this.hasFilter = true;
      }
   }

   @Override
   public void setValueFilter(ValueFilter valueFilter) {
      this.valueFilter = valueFilter;
      if (valueFilter != null) {
         this.hasFilter = true;
      }
   }

   @Override
   public void setNameFilter(NameFilter nameFilter) {
      this.nameFilter = nameFilter;
      if (nameFilter != null) {
         this.hasFilter = true;
      }
   }

   @Override
   public void setPropertyPreFilter(PropertyPreFilter propertyPreFilter) {
      this.propertyPreFilter = propertyPreFilter;
      if (propertyPreFilter != null) {
         this.hasFilter = true;
      }
   }

   @Override
   public void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
         this.writeClassInfo(jsonWriter);
      }

      int size = this.fieldWriters.size();
      jsonWriter.startArray(size);

      for (int i = 0; i < size; i++) {
         FieldWriter fieldWriter = this.fieldWriters.get(i);
         fieldWriter.writeValue(jsonWriter, object);
      }
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      long featuresAll = features | this.features | jsonWriter.getFeatures();
      if (!this.serializable) {
         if ((featuresAll & JSONWriter.Feature.ErrorOnNoneSerializable.mask) != 0L) {
            this.errorOnNoneSerializable();
            return;
         }

         if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L) {
            jsonWriter.writeNull();
            return;
         }
      }

      if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L) {
         this.writeWithFilter(jsonWriter, object, fieldName, fieldType, features);
      } else {
         int size = this.fieldWriterArray.length;
         if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            this.writeClassInfo(jsonWriter);
         }

         jsonWriter.startObject();

         for (int i = 0; i < size; i++) {
            this.fieldWriters.get(i).write(jsonWriter, object);
         }

         jsonWriter.endObject();
      }
   }

   protected final void writeClassInfo(JSONWriter jsonWriter) {
      SymbolTable symbolTable = jsonWriter.symbolTable;
      if (symbolTable == null || !this.writeClassInfoSymbol(jsonWriter, symbolTable)) {
         jsonWriter.writeTypeName(this.typeNameJSONB, this.typeNameHash);
      }
   }

   private boolean writeClassInfoSymbol(JSONWriter jsonWriter, SymbolTable symbolTable) {
      int symbolTableIdentity = System.identityHashCode(symbolTable);
      int symbol;
      if (this.typeNameSymbolCache == 0L) {
         symbol = symbolTable.getOrdinalByHashCode(this.typeNameHash);
         if (symbol != -1) {
            this.typeNameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
         }
      } else {
         int identity = (int)this.typeNameSymbolCache;
         if (identity == symbolTableIdentity) {
            symbol = (int)(this.typeNameSymbolCache >> 32);
         } else {
            symbol = symbolTable.getOrdinalByHashCode(this.typeNameHash);
            if (symbol != -1) {
               this.typeNameSymbolCache = (long)symbol << 32 | (long)symbolTableIdentity;
            }
         }
      }

      if (symbol != -1) {
         jsonWriter.writeRaw((byte)-110);
         jsonWriter.writeInt32(-symbol);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (this.hasValueField) {
         FieldWriter fieldWriter = this.fieldWriterArray[0];
         fieldWriter.writeValue(jsonWriter, object);
      } else {
         long featuresAll = features | this.features | jsonWriter.getFeatures();
         boolean beanToArray = (featuresAll & JSONWriter.Feature.BeanToArray.mask) != 0L;
         if (jsonWriter.jsonb) {
            if (beanToArray) {
               this.writeArrayMappingJSONB(jsonWriter, object, fieldName, fieldType, features);
            } else {
               this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
            }
         } else if (this.googleCollection) {
            Collection collection = (Collection)object;
            ObjectWriterImplCollection.INSTANCE.write(jsonWriter, collection, fieldName, fieldType, features);
         } else if (beanToArray) {
            this.writeArrayMapping(jsonWriter, object, fieldName, fieldType, features);
         } else {
            if (!this.serializable) {
               if ((featuresAll & JSONWriter.Feature.ErrorOnNoneSerializable.mask) != 0L) {
                  this.errorOnNoneSerializable();
                  return;
               }

               if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L) {
                  jsonWriter.writeNull();
                  return;
               }
            }

            if (this.hasFilter(jsonWriter)) {
               this.writeWithFilter(jsonWriter, object, fieldName, fieldType, features);
            } else {
               jsonWriter.startObject();
               if (((features | this.features) & JSONWriter.Feature.WriteClassName.mask) != 0L || jsonWriter.isWriteTypeInfo(object, features)) {
                  this.writeTypeInfo(jsonWriter);
               }

               int size = this.fieldWriters.size();

               for (int i = 0; i < size; i++) {
                  FieldWriter fieldWriter = this.fieldWriters.get(i);
                  fieldWriter.write(jsonWriter, object);
               }

               jsonWriter.endObject();
            }
         }
      }
   }

   public Map<String, Object> toMap(Object object) {
      int size = this.fieldWriters.size();
      JSONObject map = new JSONObject(size, 1.0F);

      for (int i = 0; i < size; i++) {
         FieldWriter fieldWriter = this.fieldWriters.get(i);
         map.put(fieldWriter.fieldName, fieldWriter.getFieldValue(object));
      }

      return map;
   }

   @Override
   public List<FieldWriter> getFieldWriters() {
      return this.fieldWriters;
   }

   @Override
   public boolean writeTypeInfo(JSONWriter jsonWriter) {
      if (jsonWriter.utf8) {
         if (this.nameWithColonUTF8 == null) {
            int typeKeyLength = this.typeKey.length();
            int typeNameLength = this.typeName.length();
            byte[] chars = new byte[typeKeyLength + typeNameLength + 5];
            chars[0] = 34;
            this.typeKey.getBytes(0, typeKeyLength, chars, 1);
            chars[typeKeyLength + 1] = 34;
            chars[typeKeyLength + 2] = 58;
            chars[typeKeyLength + 3] = 34;
            this.typeName.getBytes(0, typeNameLength, chars, typeKeyLength + 4);
            chars[typeKeyLength + typeNameLength + 4] = 34;
            this.nameWithColonUTF8 = chars;
         }

         jsonWriter.writeNameRaw(this.nameWithColonUTF8);
         return true;
      } else if (jsonWriter.utf16) {
         if (this.nameWithColonUTF16 == null) {
            int typeKeyLength = this.typeKey.length();
            int typeNameLength = this.typeName.length();
            char[] chars = new char[typeKeyLength + typeNameLength + 5];
            chars[0] = '"';
            this.typeKey.getChars(0, typeKeyLength, chars, 1);
            chars[typeKeyLength + 1] = '"';
            chars[typeKeyLength + 2] = ':';
            chars[typeKeyLength + 3] = '"';
            this.typeName.getChars(0, typeNameLength, chars, typeKeyLength + 4);
            chars[typeKeyLength + typeNameLength + 4] = '"';
            this.nameWithColonUTF16 = chars;
         }

         jsonWriter.writeNameRaw(this.nameWithColonUTF16);
         return true;
      } else if (jsonWriter.jsonb) {
         if (this.typeKeyJSONB == null) {
            this.typeKeyJSONB = JSONB.toBytes(this.typeKey);
         }

         jsonWriter.writeRaw(this.typeKeyJSONB);
         jsonWriter.writeRaw(this.typeNameJSONB);
         return true;
      } else {
         jsonWriter.writeString(this.typeKey);
         jsonWriter.writeColon();
         jsonWriter.writeString(this.typeName);
         return true;
      }
   }

   @Override
   public void writeWithFilter(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            if (jsonWriter.jsonb) {
               this.writeClassInfo(jsonWriter);
               jsonWriter.startObject();
            } else {
               jsonWriter.startObject();
               this.writeTypeInfo(jsonWriter);
            }
         } else {
            jsonWriter.startObject();
         }

         JSONWriter.Context context = jsonWriter.context;
         long features2 = context.getFeatures() | features;
         boolean refDetect = (features2 & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
         boolean ignoreNonFieldGetter = (features2 & JSONWriter.Feature.IgnoreNonFieldGetter.mask) != 0L;
         BeforeFilter beforeFilter = context.getBeforeFilter();
         if (beforeFilter != null) {
            beforeFilter.writeBefore(jsonWriter, object);
         }

         PropertyPreFilter propertyPreFilter = context.getPropertyPreFilter();
         if (propertyPreFilter == null) {
            propertyPreFilter = this.propertyPreFilter;
         }

         NameFilter nameFilter = context.getNameFilter();
         if (nameFilter == null) {
            nameFilter = this.nameFilter;
         } else if (this.nameFilter != null) {
            nameFilter = NameFilter.compose(this.nameFilter, nameFilter);
         }

         ContextNameFilter contextNameFilter = context.getContextNameFilter();
         ValueFilter valueFilter = context.getValueFilter();
         if (valueFilter == null) {
            valueFilter = this.valueFilter;
         } else if (this.valueFilter != null) {
            valueFilter = ValueFilter.compose(this.valueFilter, valueFilter);
         }

         ContextValueFilter contextValueFilter = context.getContextValueFilter();
         PropertyFilter propertyFilter = context.getPropertyFilter();
         if (propertyFilter == null) {
            propertyFilter = this.propertyFilter;
         }

         LabelFilter labelFilter = context.getLabelFilter();

         for (int i = 0; i < this.fieldWriters.size(); i++) {
            FieldWriter fieldWriter = this.fieldWriters.get(i);
            Field field = fieldWriter.field;
            if (!ignoreNonFieldGetter || fieldWriter.method == null || (fieldWriter.features & 4503599627370496L) != 0L) {
               String fieldWriterFieldName = fieldWriter.fieldName;
               if (propertyPreFilter == null || propertyPreFilter.process(jsonWriter, object, fieldWriterFieldName)) {
                  if (labelFilter != null) {
                     String label = fieldWriter.label;
                     if (label != null && !label.isEmpty() && !labelFilter.apply(label)) {
                        continue;
                     }
                  }

                  if (nameFilter == null && propertyFilter == null && contextValueFilter == null && contextNameFilter == null && valueFilter == null) {
                     fieldWriter.write(jsonWriter, object);
                  } else {
                     Object fieldValue;
                     try {
                        fieldValue = fieldWriter.getFieldValue(object);
                     } catch (Throwable var30) {
                        if ((context.getFeatures() & JSONWriter.Feature.IgnoreErrorGetter.mask) != 0L) {
                           continue;
                        }

                        throw var30;
                     }

                     if ((fieldValue != null || jsonWriter.isWriteNulls())
                        && (
                           refDetect
                              || !"this$0".equals(fieldWriterFieldName) && !"this$1".equals(fieldWriterFieldName) && !"this$2".equals(fieldWriterFieldName)
                        )) {
                        BeanContext beanContext = null;
                        String filteredName = fieldWriterFieldName;
                        if (nameFilter != null) {
                           filteredName = nameFilter.process(object, fieldWriterFieldName, fieldValue);
                        }

                        if (contextNameFilter != null && beanContext == null) {
                           if (field == null && fieldWriter.method != null) {
                              field = BeanUtils.getDeclaredField(this.objectClass, fieldWriter.fieldName);
                           }

                           beanContext = new BeanContext(
                              this.objectClass,
                              fieldWriter.method,
                              field,
                              fieldWriter.fieldName,
                              fieldWriter.label,
                              fieldWriter.fieldClass,
                              fieldWriter.fieldType,
                              fieldWriter.features,
                              fieldWriter.format
                           );
                           filteredName = contextNameFilter.process(beanContext, object, filteredName, fieldValue);
                        }

                        if (propertyFilter == null || propertyFilter.apply(object, fieldWriterFieldName, fieldValue)) {
                           boolean nameChanged = filteredName != null && filteredName != fieldWriterFieldName;
                           Object filteredValue = fieldValue;
                           if (valueFilter != null) {
                              filteredValue = valueFilter.apply(object, fieldWriterFieldName, fieldValue);
                           }

                           if (contextValueFilter != null) {
                              if (beanContext == null) {
                                 if (field == null && fieldWriter.method != null) {
                                    field = BeanUtils.getDeclaredField(this.objectClass, fieldWriter.fieldName);
                                 }

                                 beanContext = new BeanContext(
                                    this.objectClass,
                                    fieldWriter.method,
                                    field,
                                    fieldWriter.fieldName,
                                    fieldWriter.label,
                                    fieldWriter.fieldClass,
                                    fieldWriter.fieldType,
                                    fieldWriter.features,
                                    fieldWriter.format
                                 );
                              }

                              filteredValue = contextValueFilter.process(beanContext, object, filteredName, filteredValue);
                           }

                           if (filteredValue != fieldValue) {
                              if (nameChanged) {
                                 jsonWriter.writeName(filteredName);
                                 jsonWriter.writeColon();
                              } else {
                                 fieldWriter.writeFieldName(jsonWriter);
                              }

                              if (filteredValue == null) {
                                 jsonWriter.writeNull();
                              } else {
                                 ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, filteredValue.getClass());
                                 fieldValueWriter.write(jsonWriter, filteredValue, fieldName, fieldType, features);
                              }
                           } else if (!nameChanged) {
                              fieldWriter.write(jsonWriter, object);
                           } else {
                              jsonWriter.writeName(filteredName);
                              jsonWriter.writeColon();
                              if (fieldValue == null) {
                                 ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, fieldWriter.fieldClass);
                                 fieldValueWriter.write(jsonWriter, null, fieldName, fieldType, features);
                              } else {
                                 ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, fieldValue.getClass());
                                 fieldValueWriter.write(jsonWriter, fieldValue, fieldName, fieldType, features);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         AfterFilter afterFilter = context.getAfterFilter();
         if (afterFilter != null) {
            afterFilter.writeAfter(jsonWriter, object);
         }

         jsonWriter.endObject();
      }
   }

   public JSONObject toJSONObject(T object) {
      return this.toJSONObject(object, 0L);
   }

   public JSONObject toJSONObject(T object, long features) {
      JSONObject jsonObject = new JSONObject();
      int i = 0;

      for (int size = this.fieldWriters.size(); i < size; i++) {
         FieldWriter fieldWriter = this.fieldWriters.get(i);
         Object fieldValue = fieldWriter.getFieldValue(object);
         String format = fieldWriter.format;
         Class fieldClass = fieldWriter.fieldClass;
         if (format != null) {
            if (fieldClass == Date.class) {
               if ("millis".equals(format)) {
                  fieldValue = ((Date)fieldValue).getTime();
               } else {
                  fieldValue = DateUtils.format((Date)fieldValue, format);
               }
            } else if (fieldClass == LocalDate.class) {
               fieldValue = DateUtils.format((LocalDate)fieldValue, format);
            } else if (fieldClass == LocalDateTime.class) {
               fieldValue = DateUtils.format((LocalDateTime)fieldValue, format);
            }
         }

         long fieldFeatures = fieldWriter.features;
         if ((fieldFeatures & 562949953421312L) != 0L) {
            if (fieldValue instanceof Map) {
               jsonObject.putAll((Map<? extends String, ? extends Object>)fieldValue);
            } else {
               ObjectWriter fieldObjectWriter = fieldWriter.getInitWriter();
               if (fieldObjectWriter == null) {
                  fieldObjectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(fieldClass);
               }

               List<FieldWriter> unwrappedFieldWriters = fieldObjectWriter.getFieldWriters();
               int j = 0;

               for (int unwrappedSize = unwrappedFieldWriters.size(); j < unwrappedSize; j++) {
                  FieldWriter unwrappedFieldWriter = unwrappedFieldWriters.get(j);
                  Object unwrappedFieldValue = unwrappedFieldWriter.getFieldValue(fieldValue);
                  jsonObject.put(unwrappedFieldWriter.fieldName, unwrappedFieldValue);
               }
            }
         } else {
            if (fieldValue != null) {
               String fieldValueClassName = fieldValue.getClass().getName();
               if (Collection.class.isAssignableFrom(fieldClass)
                  && fieldValue.getClass() != JSONObject.class
                  && !fieldValueClassName.equals("com.alibaba.fastjson.JSONObject")) {
                  Collection collection = (Collection)fieldValue;
                  JSONArray array = new JSONArray(collection.size());

                  for (Object item : collection) {
                     Object itemJSON = item == object ? jsonObject : JSON.toJSON(item);
                     array.add(itemJSON);
                  }

                  fieldValue = array;
               }
            }

            if (fieldValue != null || ((this.features | features) & JSONWriter.Feature.WriteNulls.mask) != 0L) {
               if (fieldValue == object) {
                  fieldValue = jsonObject;
               }

               jsonObject.put(fieldWriter.fieldName, fieldValue);
            }
         }
      }

      return jsonObject;
   }

   @Override
   public String toString() {
      return this.objectClass.getName();
   }

   protected void errorOnNoneSerializable() {
      throw new JSONException("not support none serializable class " + this.objectClass.getName());
   }
}
