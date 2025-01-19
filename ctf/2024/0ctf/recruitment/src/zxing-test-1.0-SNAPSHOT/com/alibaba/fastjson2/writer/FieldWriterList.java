package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

abstract class FieldWriterList<T> extends FieldWriter<T> {
   final Type itemType;
   final Class itemClass;
   final boolean itemClassNotReferenceDetect;
   ObjectWriter listWriter;
   ObjectWriter itemObjectWriter;
   final boolean writeAsString;

   FieldWriterList(
      String name, Type itemType, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method
   ) {
      super(name, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      this.writeAsString = (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
      this.itemType = (Type)(itemType == null ? Object.class : itemType);
      if (this.itemType instanceof Class) {
         this.itemClass = (Class)itemType;
         if (this.itemClass != null) {
            if (Enum.class.isAssignableFrom(this.itemClass)) {
               this.listWriter = new ObjectWriterImplListEnum(fieldClass, this.itemClass, features);
            } else if (this.itemClass == String.class) {
               this.listWriter = ObjectWriterImplListStr.INSTANCE;
            } else {
               this.listWriter = new ObjectWriterImplList(fieldClass, fieldType, this.itemClass, itemType, features);
            }
         }
      } else {
         this.itemClass = TypeUtils.getMapping(itemType);
      }

      this.itemClassNotReferenceDetect = this.itemClass != null && ObjectWriterProvider.isNotReferenceDetect(this.itemClass);
      if (format != null && this.itemClass == Date.class) {
         this.itemObjectWriter = new ObjectWriterImplDate(format, null);
      }
   }

   @Override
   public Type getItemType() {
      return this.itemType;
   }

   @Override
   public Class getItemClass() {
      return this.itemClass;
   }

   @Override
   public ObjectWriter getItemWriter(JSONWriter jsonWriter, Type itemType) {
      if (itemType != null && itemType != this.itemType) {
         return jsonWriter.getObjectWriter(itemType, TypeUtils.getClass(itemType));
      } else if (this.itemObjectWriter != null) {
         return this.itemObjectWriter;
      } else {
         return this.format != null
            ? jsonWriter.getContext().getProvider().getObjectWriter(itemType, this.format, null)
            : (this.itemObjectWriter = jsonWriter.getObjectWriter(this.itemType, this.itemClass));
      }
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      ObjectWriter listWriter = this.listWriter;
      if (listWriter != null && this.fieldClass.isAssignableFrom(valueClass)) {
         return listWriter;
      } else {
         return listWriter == null && valueClass == this.fieldClass
            ? (this.listWriter = jsonWriter.getObjectWriter(valueClass))
            : jsonWriter.getObjectWriter(valueClass);
      }
   }

   @Override
   public final void writeListValueJSONB(JSONWriter jsonWriter, List list) {
      Class previousClass = null;
      ObjectWriter previousObjectWriter = null;
      long features = jsonWriter.getFeatures(this.features);
      boolean beanToArray = (features & JSONWriter.Feature.BeanToArray.mask) != 0L;
      int size = list.size();
      boolean refDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
      if (jsonWriter.isWriteTypeInfo(list, this.fieldClass)) {
         jsonWriter.writeTypeName(TypeUtils.getTypeName(list.getClass()));
      }

      jsonWriter.startArray(size);

      for (int i = 0; i < size; i++) {
         Object item = list.get(i);
         if (item == null) {
            jsonWriter.writeNull();
         } else {
            Class<?> itemClass = item.getClass();
            if (itemClass != previousClass) {
               refDetect = jsonWriter.isRefDetect();
               if (itemClass == this.itemType && this.itemObjectWriter != null) {
                  previousObjectWriter = this.itemObjectWriter;
               } else {
                  previousObjectWriter = this.getItemWriter(jsonWriter, itemClass);
               }

               previousClass = itemClass;
               if (refDetect) {
                  if (itemClass == this.itemClass) {
                     refDetect = !this.itemClassNotReferenceDetect;
                  } else {
                     refDetect = !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                  }
               }
            }

            if (!refDetect || !jsonWriter.writeReference(i, item)) {
               if (beanToArray) {
                  previousObjectWriter.writeArrayMappingJSONB(jsonWriter, item, i, this.itemType, features);
               } else {
                  previousObjectWriter.writeJSONB(jsonWriter, item, i, this.itemType, features);
               }

               if (refDetect) {
                  jsonWriter.popPath(item);
               }
            }
         }
      }
   }

   @Override
   public void writeListValue(JSONWriter jsonWriter, List list) {
      if (jsonWriter.jsonb) {
         this.writeListJSONB(jsonWriter, list);
      } else {
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         long features = jsonWriter.getFeatures(this.features);
         boolean previousItemRefDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
         jsonWriter.startArray();

         for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            Object item = list.get(i);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               Class<?> itemClass = item.getClass();
               if (itemClass == String.class) {
                  jsonWriter.writeString((String)item);
               } else if (this.writeAsString) {
                  jsonWriter.writeString(item.toString());
               } else {
                  boolean itemRefDetect;
                  ObjectWriter itemObjectWriter;
                  if (itemClass == previousClass) {
                     itemObjectWriter = previousObjectWriter;
                     itemRefDetect = previousItemRefDetect;
                  } else {
                     itemRefDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
                     itemObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                     previousClass = itemClass;
                     previousObjectWriter = itemObjectWriter;
                     if (itemRefDetect) {
                        itemRefDetect = !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                     }

                     previousItemRefDetect = itemRefDetect;
                  }

                  if (!itemRefDetect || !jsonWriter.writeReference(i, item)) {
                     if (this.managedReference) {
                        jsonWriter.addManagerReference(item);
                     }

                     itemObjectWriter.write(jsonWriter, item, null, this.itemType, features);
                     if (itemRefDetect) {
                        jsonWriter.popPath(item);
                     }
                  }
               }
            }
         }

         jsonWriter.endArray();
      }
   }

   @Override
   public final void writeListJSONB(JSONWriter jsonWriter, List list) {
      Class previousClass = null;
      ObjectWriter previousObjectWriter = null;
      long features = jsonWriter.getFeatures(this.features);
      boolean beanToArray = (features & JSONWriter.Feature.BeanToArray.mask) != 0L;
      int size = list.size();
      if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) == 0L || size != 0) {
         this.writeFieldName(jsonWriter);
         boolean refDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
         if (jsonWriter.isWriteTypeInfo(list, this.fieldClass)) {
            jsonWriter.writeTypeName(TypeUtils.getTypeName(list.getClass()));
         }

         jsonWriter.startArray(size);

         for (int i = 0; i < size; i++) {
            Object item = list.get(i);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               Class<?> itemClass = item.getClass();
               if (itemClass != previousClass) {
                  refDetect = jsonWriter.isRefDetect();
                  if (itemClass == this.itemType && this.itemObjectWriter != null) {
                     previousObjectWriter = this.itemObjectWriter;
                  } else {
                     previousObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                  }

                  previousClass = itemClass;
                  if (refDetect) {
                     if (itemClass == this.itemClass) {
                        refDetect = !this.itemClassNotReferenceDetect;
                     } else {
                        refDetect = !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                     }
                  }
               }

               if (!refDetect || !jsonWriter.writeReference(i, item)) {
                  if (beanToArray) {
                     previousObjectWriter.writeArrayMappingJSONB(jsonWriter, item, i, this.itemType, features);
                  } else {
                     previousObjectWriter.writeJSONB(jsonWriter, item, i, this.itemType, features);
                  }

                  if (refDetect) {
                     jsonWriter.popPath(item);
                  }
               }
            }
         }
      }
   }

   @Override
   public void writeList(JSONWriter jsonWriter, List list) {
      if (jsonWriter.jsonb) {
         this.writeListJSONB(jsonWriter, list);
      } else {
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         long features = jsonWriter.getFeatures(this.features);
         if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) == 0L || !list.isEmpty()) {
            this.writeFieldName(jsonWriter);
            boolean previousItemRefDetect = (features & JSONWriter.Feature.ReferenceDetection.mask) != 0L;
            jsonWriter.startArray();

            for (int i = 0; i < list.size(); i++) {
               if (i != 0) {
                  jsonWriter.writeComma();
               }

               Object item = list.get(i);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> itemClass = item.getClass();
                  if (itemClass == String.class) {
                     jsonWriter.writeString((String)item);
                  } else {
                     boolean itemRefDetect;
                     ObjectWriter itemObjectWriter;
                     if (itemClass == previousClass) {
                        itemObjectWriter = previousObjectWriter;
                        itemRefDetect = previousItemRefDetect;
                     } else {
                        itemRefDetect = jsonWriter.isRefDetect();
                        itemObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                        previousClass = itemClass;
                        previousObjectWriter = itemObjectWriter;
                        if (itemRefDetect) {
                           itemRefDetect = !ObjectWriterProvider.isNotReferenceDetect(itemClass);
                        }

                        previousItemRefDetect = itemRefDetect;
                     }

                     if (itemRefDetect) {
                        if (jsonWriter.writeReference(i, item)) {
                           continue;
                        }
                     } else if (this.managedReference) {
                        jsonWriter.addManagerReference(item);
                     }

                     itemObjectWriter.write(jsonWriter, item, null, this.itemType, features);
                     if (itemRefDetect) {
                        jsonWriter.popPath(item);
                     }
                  }
               }
            }

            jsonWriter.endArray();
         }
      }
   }

   @Override
   public void writeListStr(JSONWriter jsonWriter, boolean writeFieldName, List<String> list) {
      if (writeFieldName) {
         this.writeFieldName(jsonWriter);
      }

      if (jsonWriter.jsonb && jsonWriter.isWriteTypeInfo(list, this.fieldClass)) {
         jsonWriter.writeTypeName(TypeUtils.getTypeName(list.getClass()));
      }

      jsonWriter.writeString(list);
   }
}
