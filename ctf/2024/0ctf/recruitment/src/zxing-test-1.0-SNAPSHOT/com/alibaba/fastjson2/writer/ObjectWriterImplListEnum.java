package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.util.List;

final class ObjectWriterImplListEnum extends ObjectWriterPrimitiveImpl {
   final Class defineClass;
   final Class enumType;
   final long features;
   byte[] typeNameJSONB;

   public ObjectWriterImplListEnum(Class defineClass, Class enumType, long features) {
      this.defineClass = defineClass;
      this.enumType = enumType;
      this.features = features;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         Class<?> objectClass = object.getClass();
         if (jsonWriter.isWriteTypeInfo(object) && this.defineClass != objectClass) {
            jsonWriter.writeTypeName(TypeUtils.getTypeName(objectClass));
         }

         List list = (List)object;
         int size = list.size();
         jsonWriter.startArray(size);
         boolean writeEnumUsingToString = jsonWriter.isEnabled(JSONWriter.Feature.WriteEnumUsingToString);

         for (int i = 0; i < size; i++) {
            Enum e = (Enum)list.get(i);
            if (e == null) {
               jsonWriter.writeNull();
            } else {
               Class enumClass = e.getClass();
               if (enumClass != this.enumType) {
                  ObjectWriter enumWriter = jsonWriter.getObjectWriter(enumClass);
                  enumWriter.writeJSONB(jsonWriter, e, null, this.enumType, this.features | features);
               } else {
                  String str;
                  if (writeEnumUsingToString) {
                     str = e.toString();
                  } else {
                     str = e.name();
                  }

                  jsonWriter.writeString(str);
               }
            }
         }

         jsonWriter.endArray();
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         List list = (List)object;
         jsonWriter.startArray();

         for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            String item = (String)list.get(i);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               jsonWriter.writeString(item);
            }
         }

         jsonWriter.endArray();
      }
   }
}
