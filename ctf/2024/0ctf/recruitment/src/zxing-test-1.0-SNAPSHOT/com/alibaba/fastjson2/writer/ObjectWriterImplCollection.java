package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

final class ObjectWriterImplCollection extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplCollection INSTANCE = new ObjectWriterImplCollection();
   static final byte[] LINKED_HASH_SET_JSONB_TYPE_NAME_BYTES = JSONB.toBytes(TypeUtils.getTypeName(LinkedHashSet.class));
   static final long LINKED_HASH_SET_JSONB_TYPE_HASH = Fnv.hashCode64(TypeUtils.getTypeName(LinkedHashSet.class));
   static final byte[] TREE_SET_JSONB_TYPE_NAME_BYTES = JSONB.toBytes(TypeUtils.getTypeName(TreeSet.class));
   static final long TREE_SET_JSONB_TYPE_HASH = Fnv.hashCode64(TypeUtils.getTypeName(TreeSet.class));
   Type itemType;
   long features;

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         Type fieldItemType = null;
         Class fieldClass = null;
         if (fieldType instanceof Class) {
            fieldClass = (Class)fieldType;
         } else if (fieldType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)fieldType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
               fieldItemType = actualTypeArguments[0];
            }

            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
               fieldClass = (Class)rawType;
            }
         }

         Collection collection = (Collection)object;
         Class<?> objectClass = object.getClass();
         boolean writeTypeInfo = jsonWriter.isWriteTypeInfo(object, fieldClass);
         if (writeTypeInfo) {
            if (fieldClass == Set.class && objectClass == HashSet.class) {
               writeTypeInfo = false;
            } else if (fieldType == Collection.class && objectClass == ArrayList.class) {
               writeTypeInfo = false;
            }
         }

         if (writeTypeInfo) {
            if (objectClass == LinkedHashSet.class) {
               jsonWriter.writeTypeName(LINKED_HASH_SET_JSONB_TYPE_NAME_BYTES, LINKED_HASH_SET_JSONB_TYPE_HASH);
            } else if (objectClass == TreeSet.class) {
               jsonWriter.writeTypeName(TREE_SET_JSONB_TYPE_NAME_BYTES, TREE_SET_JSONB_TYPE_HASH);
            } else {
               jsonWriter.writeTypeName(TypeUtils.getTypeName(objectClass));
            }
         }

         boolean refDetect = jsonWriter.isRefDetect();
         if (collection.size() > 1 && !(collection instanceof SortedSet) && !(collection instanceof LinkedHashSet)) {
            refDetect = false;
         }

         jsonWriter.startArray(collection.size());
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         int i = 0;

         for (Object item : collection) {
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               label96: {
                  Class<?> itemClass = item.getClass();
                  ObjectWriter itemObjectWriter;
                  if (itemClass == previousClass) {
                     itemObjectWriter = previousObjectWriter;
                  } else {
                     itemObjectWriter = jsonWriter.getObjectWriter(itemClass);
                     previousClass = itemClass;
                     previousObjectWriter = itemObjectWriter;
                  }

                  boolean itemRefDetect = refDetect && !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                  if (itemRefDetect) {
                     String refPath = jsonWriter.setPath(i, item);
                     if (refPath != null) {
                        jsonWriter.writeReference(refPath);
                        jsonWriter.popPath(item);
                        break label96;
                     }
                  }

                  itemObjectWriter.writeJSONB(jsonWriter, item, i, fieldItemType, features);
                  if (itemRefDetect) {
                     jsonWriter.popPath(item);
                  }
               }
            }

            i++;
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.jsonb) {
         this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
      } else if (object == null) {
         jsonWriter.writeNull();
      } else {
         long features3;
         if (object instanceof Set
            && jsonWriter.isWriteTypeInfo(object, features3 = jsonWriter.getFeatures(features | this.features))
            && (features3 & JSONWriter.Feature.NotWriteSetClassName.mask) == 0L) {
            jsonWriter.writeRaw("Set");
         }

         Iterable iterable = (Iterable)object;
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         jsonWriter.startArray();
         int i = 0;

         for (Object o : iterable) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            if (o == null) {
               jsonWriter.writeNull();
               i++;
            } else {
               Class<?> itemClass = o.getClass();
               ObjectWriter itemObjectWriter;
               if (itemClass == previousClass) {
                  itemObjectWriter = previousObjectWriter;
               } else {
                  itemObjectWriter = jsonWriter.getObjectWriter(itemClass);
                  previousClass = itemClass;
                  previousObjectWriter = itemObjectWriter;
               }

               itemObjectWriter.write(jsonWriter, o, i, this.itemType, this.features);
               i++;
            }
         }

         jsonWriter.endArray();
      }
   }
}
