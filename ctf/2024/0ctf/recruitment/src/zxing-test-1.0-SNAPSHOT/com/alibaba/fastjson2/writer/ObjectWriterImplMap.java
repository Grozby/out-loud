package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.AfterFilter;
import com.alibaba.fastjson2.filter.BeforeFilter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class ObjectWriterImplMap extends ObjectWriterPrimitiveImpl {
   static final byte[] TYPE_NAME_JSONObject1O = JSONB.toBytes("JO10");
   static final long TYPE_HASH_JSONObject1O = Fnv.hashCode64("JO10");
   static final ObjectWriterImplMap INSTANCE = new ObjectWriterImplMap(String.class, Object.class, JSONObject.class, JSONObject.class, 0L);
   static final ObjectWriterImplMap INSTANCE_1x;
   final Type objectType;
   final Class objectClass;
   final Type keyType;
   final Type valueType;
   final String format;
   final boolean valueTypeRefDetect;
   volatile ObjectWriter keyWriter;
   volatile ObjectWriter valueWriter;
   final byte[] jsonbTypeInfo;
   final long typeNameHash;
   final long features;
   final boolean jsonObject1;
   final Field jsonObject1InnerMap;
   final long jsonObject1InnerMapOffset;
   final char[] typeInfoUTF16;
   final byte[] typeInfoUTF8;

   public ObjectWriterImplMap(Class objectClass, long features) {
      this(null, null, objectClass, objectClass, features);
   }

   public ObjectWriterImplMap(Type keyType, Type valueType, Class objectClass, Type objectType, long features) {
      this(keyType, valueType, null, objectClass, objectType, features);
   }

   public ObjectWriterImplMap(Type keyType, Type valueType, String format, Class objectClass, Type objectType, long features) {
      this.keyType = keyType;
      this.valueType = valueType;
      this.format = format;
      this.objectClass = objectClass;
      this.objectType = objectType;
      this.features = features;
      if (valueType == null) {
         this.valueTypeRefDetect = true;
      } else {
         this.valueTypeRefDetect = !ObjectWriterProvider.isNotReferenceDetect(TypeUtils.getClass(valueType));
      }

      String typeName = TypeUtils.getTypeName(objectClass);
      String typeInfoStr = "\"@type\":\"" + objectClass.getName() + "\"";
      this.typeInfoUTF16 = typeInfoStr.toCharArray();
      this.typeInfoUTF8 = typeInfoStr.getBytes(StandardCharsets.UTF_8);
      this.jsonObject1 = "JO1".equals(typeName);
      this.jsonbTypeInfo = JSONB.toBytes(typeName);
      this.typeNameHash = Fnv.hashCode64(typeName);
      long jsonObject1InnerMapOffset = -1L;
      if (this.jsonObject1) {
         this.jsonObject1InnerMap = BeanUtils.getDeclaredField(objectClass, "map");
         if (this.jsonObject1InnerMap != null) {
            this.jsonObject1InnerMap.setAccessible(true);
            jsonObject1InnerMapOffset = JDKUtils.UNSAFE.objectFieldOffset(this.jsonObject1InnerMap);
         }
      } else {
         this.jsonObject1InnerMap = null;
      }

      this.jsonObject1InnerMapOffset = jsonObject1InnerMapOffset;
   }

   public static ObjectWriterImplMap of(Class objectClass) {
      if (objectClass == JSONObject.class) {
         return INSTANCE;
      } else {
         return objectClass == TypeUtils.CLASS_JSON_OBJECT_1x ? INSTANCE_1x : new ObjectWriterImplMap(null, null, objectClass, objectClass, 0L);
      }
   }

   public static ObjectWriterImplMap of(Type type) {
      Class objectClass = TypeUtils.getClass(type);
      return new ObjectWriterImplMap(objectClass, 0L);
   }

   public static ObjectWriterImplMap of(Type type, Class defineClass) {
      return of(type, null, defineClass);
   }

   public static ObjectWriterImplMap of(Type type, String format, Class defineClass) {
      Type keyType = null;
      Type valueType = null;
      if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type rawType = parameterizedType.getRawType();
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 2) {
            keyType = actualTypeArguments[0];
            valueType = actualTypeArguments[1];
         }
      }

      return new ObjectWriterImplMap(keyType, valueType, format, defineClass, type, 0L);
   }

   @Override
   public void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      Map map = (Map)object;
      jsonWriter.startObject();
      boolean writeNulls = jsonWriter.isWriteNulls();

      for (Entry<String, Object> entry : map.entrySet()) {
         String key = entry.getKey();
         Object value = entry.getValue();
         if (value == null) {
            if (writeNulls) {
               jsonWriter.writeString(key);
               jsonWriter.writeNull();
            }
         } else {
            jsonWriter.writeString(key);
            Class<?> valueType = value.getClass();
            if (valueType == String.class) {
               jsonWriter.writeString((String)value);
            } else {
               ObjectWriter valueWriter = jsonWriter.getObjectWriter(valueType);
               valueWriter.writeJSONB(jsonWriter, value, key, this.valueType, this.features);
            }
         }
      }

      jsonWriter.endObject();
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (fieldType == this.objectType && jsonWriter.isWriteMapTypeInfo(object, this.objectClass, features)
         || jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
         boolean ordered = false;
         if (this.jsonObject1InnerMap != null) {
            if (this.jsonObject1InnerMapOffset != -1L) {
               Object innerMap = JDKUtils.UNSAFE.getObject(object, this.jsonObject1InnerMapOffset);
               ordered = innerMap instanceof LinkedHashMap;
            } else {
               try {
                  Object innerMap = this.jsonObject1InnerMap.get(object);
                  ordered = innerMap instanceof LinkedHashMap;
               } catch (IllegalAccessException var27) {
               }
            }
         }

         if (ordered) {
            jsonWriter.writeTypeName(TYPE_NAME_JSONObject1O, TYPE_HASH_JSONObject1O);
         } else {
            jsonWriter.writeTypeName(this.jsonbTypeInfo, this.typeNameHash);
         }
      }

      Map map = (Map)object;
      JSONWriter.Context context = jsonWriter.context;
      jsonWriter.startObject();
      Type fieldValueType = this.valueType;
      if (fieldType == this.objectType) {
         fieldValueType = this.valueType;
      } else if (fieldType instanceof ParameterizedType) {
         Type[] actualTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
         if (actualTypeArguments.length == 2) {
            fieldValueType = actualTypeArguments[1];
         }
      }

      long contextFeatures = context.getFeatures();
      boolean writeNulls = (contextFeatures & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask)) != 0L;
      boolean fieldBased = (contextFeatures & JSONWriter.Feature.FieldBased.mask) != 0L;
      ObjectWriterProvider provider = context.provider;
      Class itemClass = null;
      ObjectWriter itemWriter = null;
      boolean contextRefDetect = (contextFeatures & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
      int i = 0;

      for (Entry entry : map.entrySet()) {
         Object entryKey = entry.getKey();
         Object value = entry.getValue();
         if (value == null) {
            if (writeNulls) {
               if (entryKey instanceof String) {
                  jsonWriter.writeString((String)entryKey);
               } else {
                  Class<?> entryKeyClass = entryKey.getClass();
                  boolean keyRefDetect = contextRefDetect && !ObjectWriterProvider.isNotReferenceDetect(entryKeyClass);
                  String refPath = null;
                  if (keyRefDetect) {
                     jsonWriter.setPath(i, entry);
                     refPath = jsonWriter.setPath("key", entryKey);
                  }

                  if (refPath != null) {
                     jsonWriter.writeReference(refPath);
                  } else {
                     ObjectWriter keyWriter = provider.getObjectWriter(entryKeyClass, entryKeyClass, fieldBased);
                     keyWriter.writeJSONB(jsonWriter, entryKey, null, null, 0L);
                  }

                  if (keyRefDetect) {
                     jsonWriter.popPath(entry);
                     jsonWriter.popPath(entryKey);
                  }
               }

               jsonWriter.writeNull();
            }
         } else {
            label260: {
               if (!(entryKey instanceof String) && (contextFeatures & JSONWriter.Feature.WriteClassName.mask) != 0L) {
                  if (entryKey == null) {
                     jsonWriter.writeNull();
                  } else {
                     if (contextRefDetect) {
                        jsonWriter.config(JSONWriter.Feature.ReferenceDetection, false);
                     }

                     Class<?> entryKeyClassx = entryKey.getClass();
                     ObjectWriter keyWriter = provider.getObjectWriter(entryKeyClassx, entryKeyClassx, fieldBased);
                     keyWriter.writeJSONB(jsonWriter, entryKey, null, null, 0L);
                     if (contextRefDetect) {
                        jsonWriter.config(JSONWriter.Feature.ReferenceDetection, true);
                     }
                  }
               } else {
                  String key;
                  if (entryKey instanceof String) {
                     key = (String)entryKey;
                  } else {
                     key = entryKey.toString();
                  }

                  if (jsonWriter.symbolTable != null) {
                     jsonWriter.writeSymbol(key);
                     if (value instanceof String) {
                        jsonWriter.writeSymbol((String)value);
                        break label260;
                     }
                  } else {
                     jsonWriter.writeString(key);
                  }
               }

               Class<?> valueClass = value.getClass();
               if (valueClass == String.class) {
                  jsonWriter.writeString((String)value);
               } else if (valueClass == Integer.class) {
                  jsonWriter.writeInt32((Integer)value);
               } else if (valueClass == Long.class) {
                  jsonWriter.writeInt64((Long)value);
               } else {
                  label254: {
                     boolean valueRefDetecChanged = false;
                     boolean valueRefDetect;
                     if (valueClass == this.valueType) {
                        valueRefDetect = contextRefDetect && this.valueTypeRefDetect;
                     } else {
                        valueRefDetect = contextRefDetect && !ObjectWriterProvider.isNotReferenceDetect(valueClass);
                     }

                     if (valueRefDetect) {
                        if (value == object) {
                           jsonWriter.writeReference("..");
                           break label254;
                        }

                        String refPathx;
                        if (entryKey instanceof String) {
                           refPathx = jsonWriter.setPath((String)entryKey, value);
                        } else if (ObjectWriterProvider.isPrimitiveOrEnum(entryKey.getClass())) {
                           refPathx = jsonWriter.setPath(entryKey.toString(), value);
                        } else if (map.size() != 1 && !(map instanceof SortedMap) && !(map instanceof LinkedHashMap)) {
                           refPathx = null;
                           jsonWriter.config(JSONWriter.Feature.ReferenceDetection, false);
                           valueRefDetecChanged = true;
                           valueRefDetect = false;
                        } else {
                           refPathx = jsonWriter.setPath(i, value);
                        }

                        if (refPathx != null) {
                           jsonWriter.writeReference(refPathx);
                           jsonWriter.popPath(value);
                           break label254;
                        }
                     }

                     ObjectWriter valueWriter;
                     if (valueClass == this.valueType && this.valueWriter != null) {
                        valueWriter = this.valueWriter;
                     } else if (itemClass == valueClass) {
                        valueWriter = itemWriter;
                     } else {
                        if (valueClass == JSONObject.class) {
                           valueWriter = INSTANCE;
                        } else if (valueClass == TypeUtils.CLASS_JSON_OBJECT_1x) {
                           valueWriter = INSTANCE_1x;
                        } else if (valueClass == JSONArray.class) {
                           valueWriter = ObjectWriterImplList.INSTANCE;
                        } else if (valueClass == TypeUtils.CLASS_JSON_ARRAY_1x) {
                           valueWriter = ObjectWriterImplList.INSTANCE;
                        } else {
                           valueWriter = provider.getObjectWriter(valueClass, valueClass, fieldBased);
                        }

                        if (itemWriter == null) {
                           itemWriter = valueWriter;
                           itemClass = valueClass;
                        }

                        if (valueClass == this.valueType) {
                           this.valueWriter = valueWriter;
                        }
                     }

                     valueWriter.writeJSONB(jsonWriter, value, entryKey, fieldValueType, this.features);
                     if (valueRefDetecChanged) {
                        jsonWriter.config(JSONWriter.Feature.ReferenceDetection, true);
                     } else if (valueRefDetect) {
                        jsonWriter.popPath(value);
                     }
                  }
               }
            }
         }

         i++;
      }

      jsonWriter.endObject();
   }

   @Override
   public boolean writeTypeInfo(JSONWriter jsonWriter) {
      if (jsonWriter.utf8) {
         jsonWriter.writeNameRaw(this.typeInfoUTF8);
      } else {
         jsonWriter.writeNameRaw(this.typeInfoUTF16);
      }

      return true;
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.jsonb) {
         this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
      } else if (this.hasFilter(jsonWriter)) {
         this.writeWithFilter(jsonWriter, object, fieldName, fieldType, features);
      } else {
         boolean refDetect = jsonWriter.isRefDetect();
         jsonWriter.startObject();
         if (fieldType == this.objectType && jsonWriter.isWriteMapTypeInfo(object, this.objectClass, features)
            || jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            this.writeTypeInfo(jsonWriter);
         }

         Map map = (Map)object;
         features |= jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.MapSortField.mask | JSONWriter.Feature.SortMapEntriesByKeys.mask)) != 0L
            && !(map instanceof SortedMap)
            && (map.getClass() != LinkedHashMap.class || (features & JSONWriter.Feature.SortMapEntriesByKeys.mask) != 0L)) {
            map = new TreeMap(map);
         }

         ObjectWriterProvider provider = jsonWriter.context.provider;

         for (Entry entry : map.entrySet()) {
            Object value = entry.getValue();
            Object key = entry.getKey();
            if (value == null) {
               if ((features & JSONWriter.Feature.WriteNulls.mask) != 0L) {
                  if (key == null) {
                     jsonWriter.writeName("null");
                  } else if (key instanceof String) {
                     jsonWriter.writeName((String)key);
                  } else if ((features & (JSONWriter.Feature.WriteNonStringKeyAsString.mask | JSONWriter.Feature.BrowserCompatible.mask)) != 0L) {
                     jsonWriter.writeName(key.toString());
                  } else if (key instanceof Integer) {
                     jsonWriter.writeName((Integer)key);
                  } else if (key instanceof Long) {
                     jsonWriter.writeName((Long)key);
                  } else {
                     jsonWriter.writeNameAny(key);
                  }

                  jsonWriter.writeColon();
                  jsonWriter.writeNull();
               }
            } else if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) == 0L
               || (!(value instanceof Collection) || !((Collection)value).isEmpty()) && (!value.getClass().isArray() || Array.getLength(value) != 0)) {
               String strKey = null;
               if (this.keyWriter != null) {
                  this.keyWriter.write(jsonWriter, key, null, null, 0L);
               } else if (key == null) {
                  jsonWriter.writeName("null");
               } else if (key instanceof String) {
                  jsonWriter.writeName(strKey = (String)key);
               } else {
                  boolean writeAsString = (features & (JSONWriter.Feature.WriteNonStringKeyAsString.mask | JSONWriter.Feature.BrowserCompatible.mask)) != 0L
                     && ObjectWriterProvider.isPrimitiveOrEnum(key.getClass());
                  if (writeAsString && (key instanceof Temporal || key instanceof Date)) {
                     writeAsString = false;
                  }

                  if (writeAsString) {
                     jsonWriter.writeName(strKey = key.toString());
                  } else if (key instanceof Integer) {
                     jsonWriter.writeName((Integer)key);
                  } else if (key instanceof Long) {
                     long longKey = (Long)key;
                     jsonWriter.writeName(longKey);
                  } else {
                     jsonWriter.writeNameAny(key);
                  }
               }

               jsonWriter.writeColon();
               Class<?> valueClass = value.getClass();
               if (valueClass == String.class) {
                  jsonWriter.writeString((String)value);
               } else if (valueClass == Integer.class) {
                  jsonWriter.writeInt32((Integer)value);
               } else if (valueClass == Long.class) {
                  if ((provider.userDefineMask & 4L) == 0L) {
                     jsonWriter.writeInt64((Long)value);
                  } else {
                     ObjectWriter valueWriter = jsonWriter.getObjectWriter(valueClass);
                     valueWriter.write(jsonWriter, value, strKey, Long.class, features);
                  }
               } else if (valueClass == Boolean.class) {
                  jsonWriter.writeBool((Boolean)value);
               } else if (valueClass == BigDecimal.class) {
                  if ((provider.userDefineMask & 8L) == 0L) {
                     jsonWriter.writeDecimal((BigDecimal)value, features, null);
                  } else {
                     ObjectWriter valueWriter = jsonWriter.getObjectWriter(valueClass);
                     valueWriter.write(jsonWriter, value, key, this.valueType, this.features);
                  }
               } else {
                  ObjectWriter valueWriter;
                  boolean isPrimitiveOrEnum;
                  if (valueClass == this.valueType) {
                     if (this.valueWriter != null) {
                        valueWriter = this.valueWriter;
                     } else {
                        valueWriter = this.valueWriter = this.format != null
                           ? jsonWriter.getObjectWriter(valueClass, this.format)
                           : jsonWriter.getObjectWriter(valueClass);
                     }

                     isPrimitiveOrEnum = ObjectWriterProvider.isPrimitiveOrEnum(value.getClass());
                  } else if (valueClass == JSONObject.class) {
                     valueWriter = INSTANCE;
                     isPrimitiveOrEnum = false;
                  } else if (valueClass == TypeUtils.CLASS_JSON_OBJECT_1x) {
                     valueWriter = INSTANCE_1x;
                     isPrimitiveOrEnum = false;
                  } else if (valueClass == JSONArray.class) {
                     valueWriter = ObjectWriterImplList.INSTANCE;
                     isPrimitiveOrEnum = false;
                  } else if (valueClass == TypeUtils.CLASS_JSON_ARRAY_1x) {
                     valueWriter = ObjectWriterImplList.INSTANCE;
                     isPrimitiveOrEnum = false;
                  } else {
                     valueWriter = jsonWriter.getObjectWriter(valueClass);
                     isPrimitiveOrEnum = ObjectWriterProvider.isPrimitiveOrEnum(value.getClass());
                  }

                  boolean valueRefDetect = refDetect && strKey != null && !isPrimitiveOrEnum;
                  if (valueRefDetect) {
                     if (value == object) {
                        jsonWriter.writeReference("..");
                        continue;
                     }

                     String refPath = jsonWriter.setPath(strKey, value);
                     if (refPath != null) {
                        jsonWriter.writeReference(refPath);
                        jsonWriter.popPath(value);
                        continue;
                     }
                  }

                  valueWriter.write(jsonWriter, value, key, this.valueType, this.features);
                  if (valueRefDetect) {
                     jsonWriter.popPath(value);
                  }
               }
            }
         }

         jsonWriter.endObject();
      }
   }

   @Override
   public void writeWithFilter(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         jsonWriter.startObject();
         Map map = (Map)object;
         features |= jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.MapSortField.mask | JSONWriter.Feature.SortMapEntriesByKeys.mask)) != 0L
            && !(map instanceof SortedMap)
            && (map.getClass() != LinkedHashMap.class || (features & JSONWriter.Feature.SortMapEntriesByKeys.mask) != 0L)) {
            map = new TreeMap(map);
         }

         JSONWriter.Context context = jsonWriter.context;
         BeforeFilter beforeFilter = context.getBeforeFilter();
         if (beforeFilter != null) {
            beforeFilter.writeBefore(jsonWriter, object);
         }

         PropertyPreFilter propertyPreFilter = context.getPropertyPreFilter();
         NameFilter nameFilter = context.getNameFilter();
         ValueFilter valueFilter = context.getValueFilter();
         PropertyFilter propertyFilter = context.getPropertyFilter();
         AfterFilter afterFilter = context.getAfterFilter();
         boolean writeNulls = context.isEnabled(JSONWriter.Feature.WriteNulls.mask);
         boolean refDetect = context.isEnabled(JSONWriter.Feature.ReferenceDetection.mask);

         for (Entry entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null || writeNulls) {
               Object entryKey = entry.getKey();
               String key;
               if (entryKey == null) {
                  key = null;
               } else {
                  key = entryKey.toString();
               }

               String refPath = null;
               if (refDetect) {
                  refPath = jsonWriter.setPath(key, value);
                  if (refPath != null) {
                     jsonWriter.writeName(key);
                     jsonWriter.writeColon();
                     jsonWriter.writeReference(refPath);
                     jsonWriter.popPath(value);
                     continue;
                  }
               }

               try {
                  if (propertyPreFilter == null || propertyPreFilter.process(jsonWriter, object, key)) {
                     if (nameFilter != null) {
                        key = nameFilter.process(object, key, value);
                     }

                     if (propertyFilter == null || propertyFilter.apply(object, key, value)) {
                        if (valueFilter != null) {
                           value = valueFilter.apply(object, key, value);
                        }

                        if (value != null || (jsonWriter.getFeatures(features) & JSONWriter.Feature.WriteNulls.mask) != 0L) {
                           jsonWriter.writeName(key);
                           jsonWriter.writeColon();
                           if (value == null) {
                              jsonWriter.writeNull();
                           } else {
                              Class<?> valueType = value.getClass();
                              ObjectWriter valueWriter = jsonWriter.getObjectWriter(valueType);
                              valueWriter.write(jsonWriter, value, fieldName, fieldType, this.features);
                           }
                        }
                     }
                  }
               } finally {
                  if (refDetect) {
                     jsonWriter.popPath(value);
                  }
               }
            }
         }

         if (afterFilter != null) {
            afterFilter.writeAfter(jsonWriter, object);
         }

         jsonWriter.endObject();
      }
   }

   static {
      if (TypeUtils.CLASS_JSON_OBJECT_1x == null) {
         INSTANCE_1x = null;
      } else {
         INSTANCE_1x = new ObjectWriterImplMap(String.class, Object.class, TypeUtils.CLASS_JSON_OBJECT_1x, TypeUtils.CLASS_JSON_OBJECT_1x, 0L);
      }
   }
}
