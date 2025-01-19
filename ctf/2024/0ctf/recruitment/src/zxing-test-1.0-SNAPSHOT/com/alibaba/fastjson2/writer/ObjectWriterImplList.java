package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

final class ObjectWriterImplList extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplList INSTANCE = new ObjectWriterImplList(null, null, null, null, 0L);
   static final ObjectWriterImplList INSTANCE_JSON_ARRAY = new ObjectWriterImplList(JSONArray.class, null, null, null, 0L);
   static final ObjectWriterImplList INSTANCE_JSON_ARRAY_1x;
   static final Class CLASS_SUBLIST;
   static final String TYPE_NAME_ARRAY_LIST;
   static final byte[] TYPE_NAME_JSONB_ARRAY_LIST;
   static final long TYPE_NAME_HASH_ARRAY_LIST;
   final Class defineClass;
   final Type defineType;
   final Class itemClass;
   final Type itemType;
   final long features;
   final boolean itemClassRefDetect;
   volatile ObjectWriter itemClassWriter;

   public ObjectWriterImplList(Class defineClass, Type defineType, Class itemClass, Type itemType, long features) {
      this.defineClass = defineClass;
      this.defineType = defineType;
      this.itemClass = itemClass;
      this.itemType = itemType;
      this.features = features;
      this.itemClassRefDetect = itemClass != null && !ObjectWriterProvider.isNotReferenceDetect(itemClass);
   }

   @Override
   public void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeArrayNull();
      } else {
         List list = this.getList(object);
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         jsonWriter.startArray(list.size());

         for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               Class<?> itemClass = item.getClass();
               ObjectWriter itemObjectWriter;
               if (itemClass == previousClass) {
                  itemObjectWriter = previousObjectWriter;
               } else {
                  itemObjectWriter = jsonWriter.getObjectWriter(itemClass);
                  previousClass = itemClass;
                  previousObjectWriter = itemObjectWriter;
               }

               itemObjectWriter.writeArrayMappingJSONB(jsonWriter, item, i, this.itemType, this.features | features);
            }
         }
      }
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeArrayNull();
      } else {
         Class fieldItemClass = null;
         Class fieldClass = null;
         if (fieldType instanceof Class) {
            fieldClass = (Class)fieldType;
         } else if (fieldType == this.defineType) {
            fieldClass = this.itemClass;
         } else if (fieldType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)fieldType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
               Type fieldItemType = actualTypeArguments[0];
               if (fieldItemType instanceof Class) {
                  fieldItemClass = (Class)fieldItemType;
               }
            }

            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
               fieldClass = (Class)rawType;
            }
         }

         Class<?> objectClass = object.getClass();
         if (jsonWriter.isWriteTypeInfo(object, fieldClass, features)) {
            if (objectClass != CLASS_SUBLIST && objectClass != ArrayList.class) {
               String typeName = TypeUtils.getTypeName(objectClass);
               jsonWriter.writeTypeName(typeName);
            } else {
               jsonWriter.writeTypeName(TYPE_NAME_JSONB_ARRAY_LIST, TYPE_NAME_HASH_ARRAY_LIST);
            }
         }

         List list = this.getList(object);
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         int size = list.size();
         if (size == 0) {
            jsonWriter.writeRaw((byte)-108);
         } else {
            boolean beanToArray = jsonWriter.isBeanToArray();
            if (beanToArray) {
               jsonWriter.startArray(size);

               for (int i = 0; i < size; i++) {
                  Object item = list.get(i);
                  if (item == null) {
                     jsonWriter.writeNull();
                  } else {
                     Class<?> itemClass = item.getClass();
                     ObjectWriter itemObjectWriter;
                     if (itemClass == previousClass) {
                        itemObjectWriter = previousObjectWriter;
                     } else {
                        itemObjectWriter = jsonWriter.getObjectWriter(itemClass);
                        previousClass = itemClass;
                        previousObjectWriter = itemObjectWriter;
                     }

                     itemObjectWriter.writeArrayMappingJSONB(jsonWriter, item, i, fieldItemClass, features);
                  }
               }

               jsonWriter.endArray();
            } else {
               JSONWriter.Context context = jsonWriter.context;
               jsonWriter.startArray(size);

               for (int ix = 0; ix < size; ix++) {
                  Object item = list.get(ix);
                  if (item == null) {
                     jsonWriter.writeNull();
                  } else {
                     Class<?> itemClass = item.getClass();
                     if (itemClass == String.class) {
                        jsonWriter.writeString((String)item);
                     } else {
                        boolean refDetect = itemClass == this.itemClass ? this.itemClassRefDetect && jsonWriter.isRefDetect() : jsonWriter.isRefDetect(item);
                        ObjectWriter itemObjectWriter;
                        if (itemClass == this.itemClass && this.itemClassWriter != null) {
                           itemObjectWriter = this.itemClassWriter;
                        } else if (itemClass == previousClass) {
                           itemObjectWriter = previousObjectWriter;
                        } else {
                           if (itemClass == JSONObject.class) {
                              itemObjectWriter = ObjectWriterImplMap.INSTANCE;
                           } else if (itemClass == TypeUtils.CLASS_JSON_OBJECT_1x) {
                              itemObjectWriter = ObjectWriterImplMap.INSTANCE_1x;
                           } else if (itemClass == JSONArray.class) {
                              itemObjectWriter = INSTANCE_JSON_ARRAY;
                           } else if (itemClass == TypeUtils.CLASS_JSON_ARRAY_1x) {
                              itemObjectWriter = INSTANCE_JSON_ARRAY_1x;
                           } else {
                              itemObjectWriter = context.getObjectWriter(itemClass);
                           }

                           previousClass = itemClass;
                           previousObjectWriter = itemObjectWriter;
                           if (itemClass == this.itemClass) {
                              this.itemClassWriter = itemObjectWriter;
                           }
                        }

                        if (refDetect) {
                           String refPath = jsonWriter.setPath(ix, item);
                           if (refPath != null) {
                              jsonWriter.writeReference(refPath);
                              jsonWriter.popPath(item);
                              continue;
                           }
                        }

                        itemObjectWriter.writeJSONB(jsonWriter, item, ix, this.itemType, this.features);
                        if (refDetect) {
                           jsonWriter.popPath(item);
                        }
                     }
                  }
               }

               jsonWriter.endArray();
            }
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeArrayNull();
      } else {
         List list = this.getList(object);
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         boolean previousRefDetect = true;
         if (jsonWriter.jsonb) {
            jsonWriter.startArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> itemClass = item.getClass();
                  ObjectWriter itemObjectWriter;
                  if (itemClass == previousClass) {
                     itemObjectWriter = previousObjectWriter;
                  } else {
                     itemObjectWriter = jsonWriter.getObjectWriter(itemClass);
                     previousClass = itemClass;
                     previousObjectWriter = itemObjectWriter;
                  }

                  itemObjectWriter.writeJSONB(jsonWriter, item, i, this.itemType, features);
               }
            }
         } else {
            JSONWriter.Context context = jsonWriter.context;
            ObjectWriterProvider provider = context.provider;
            jsonWriter.startArray();

            for (int ix = 0; ix < list.size(); ix++) {
               if (ix != 0) {
                  jsonWriter.writeComma();
               }

               Object item = list.get(ix);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> itemClass = item.getClass();
                  if (itemClass == String.class) {
                     jsonWriter.writeString((String)item);
                  } else if (itemClass == Integer.class) {
                     if ((provider.userDefineMask & 2L) == 0L) {
                        jsonWriter.writeInt32((Integer)item);
                     } else {
                        ObjectWriter valueWriter = provider.getObjectWriter(itemClass, itemClass, false);
                        valueWriter.write(jsonWriter, item, ix, Integer.class, features);
                     }
                  } else if (itemClass == Long.class) {
                     if ((provider.userDefineMask & 4L) == 0L) {
                        jsonWriter.writeInt64((Long)item);
                     } else {
                        ObjectWriter valueWriter = provider.getObjectWriter(itemClass, itemClass, false);
                        valueWriter.write(jsonWriter, item, ix, Long.class, features);
                     }
                  } else if (itemClass == Boolean.class) {
                     if ((provider.userDefineMask & 2L) == 0L) {
                        jsonWriter.writeBool((Boolean)item);
                     } else {
                        ObjectWriter valueWriter = provider.getObjectWriter(itemClass, itemClass, false);
                        valueWriter.write(jsonWriter, item, ix, Boolean.class, features);
                     }
                  } else if (itemClass == BigDecimal.class) {
                     if ((provider.userDefineMask & 8L) == 0L) {
                        jsonWriter.writeDecimal((BigDecimal)item, features, null);
                     } else {
                        ObjectWriter valueWriter = provider.getObjectWriter(itemClass, itemClass, false);
                        valueWriter.write(jsonWriter, item, ix, BigDecimal.class, features);
                     }
                  } else {
                     ObjectWriter itemObjectWriter;
                     boolean refDetect;
                     if (itemClass == this.itemClass && this.itemClassWriter != null) {
                        itemObjectWriter = this.itemClassWriter;
                        refDetect = this.itemClassRefDetect && jsonWriter.isRefDetect();
                     } else if (itemClass == previousClass) {
                        itemObjectWriter = previousObjectWriter;
                        refDetect = previousRefDetect;
                     } else {
                        if (itemClass == JSONObject.class) {
                           itemObjectWriter = ObjectWriterImplMap.INSTANCE;
                           refDetect = jsonWriter.isRefDetect();
                        } else if (itemClass == TypeUtils.CLASS_JSON_OBJECT_1x) {
                           itemObjectWriter = ObjectWriterImplMap.INSTANCE_1x;
                           refDetect = jsonWriter.isRefDetect();
                        } else if (itemClass == JSONArray.class) {
                           itemObjectWriter = INSTANCE_JSON_ARRAY;
                           refDetect = jsonWriter.isRefDetect();
                        } else if (itemClass == TypeUtils.CLASS_JSON_ARRAY_1x) {
                           itemObjectWriter = INSTANCE_JSON_ARRAY_1x;
                           refDetect = jsonWriter.isRefDetect();
                        } else {
                           itemObjectWriter = context.getObjectWriter(itemClass);
                           refDetect = jsonWriter.isRefDetect(item);
                        }

                        previousClass = itemClass;
                        previousObjectWriter = itemObjectWriter;
                        previousRefDetect = refDetect;
                        if (itemClass == this.itemClass) {
                           this.itemClassWriter = itemObjectWriter;
                        }
                     }

                     if (refDetect) {
                        String refPath = jsonWriter.setPath(ix, item);
                        if (refPath != null) {
                           jsonWriter.writeReference(refPath);
                           jsonWriter.popPath(item);
                           continue;
                        }
                     }

                     itemObjectWriter.write(jsonWriter, item, ix, this.itemType, this.features);
                     if (refDetect) {
                        jsonWriter.popPath(item);
                     }
                  }
               }
            }

            jsonWriter.endArray();
         }
      }
   }

   private List getList(Object object) {
      if (object instanceof List) {
         return (List)object;
      } else if (!(object instanceof Iterable)) {
         throw new JSONException("Can not cast '" + object.getClass() + "' to List");
      } else {
         Iterable items = (Iterable)object;
         List list = items instanceof Collection ? new ArrayList(((Collection)items).size()) : new ArrayList();
         Iterator iterator = items.iterator();

         while (iterator.hasNext()) {
            list.add(iterator.next());
         }

         return list;
      }
   }

   static {
      if (TypeUtils.CLASS_JSON_ARRAY_1x == null) {
         INSTANCE_JSON_ARRAY_1x = null;
      } else {
         INSTANCE_JSON_ARRAY_1x = new ObjectWriterImplList(TypeUtils.CLASS_JSON_ARRAY_1x, null, null, null, 0L);
      }

      CLASS_SUBLIST = new ArrayList().subList(0, 0).getClass();
      TYPE_NAME_ARRAY_LIST = TypeUtils.getTypeName(ArrayList.class);
      TYPE_NAME_JSONB_ARRAY_LIST = JSONB.toBytes(TYPE_NAME_ARRAY_LIST);
      TYPE_NAME_HASH_ARRAY_LIST = Fnv.hashCode64(TYPE_NAME_ARRAY_LIST);
   }
}
