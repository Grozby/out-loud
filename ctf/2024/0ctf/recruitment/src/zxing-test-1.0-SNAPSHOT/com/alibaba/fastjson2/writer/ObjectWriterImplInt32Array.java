package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;

final class ObjectWriterImplInt32Array extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplInt32Array INSTANCE = new ObjectWriterImplInt32Array();
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("[Integer");
   static final long JSONB_TYPE_HASH = Fnv.hashCode64("[Integer");

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         if (jsonWriter.isEnabled(JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask)) {
            jsonWriter.startArray();
            jsonWriter.endArray();
         } else {
            jsonWriter.writeNull();
         }
      } else {
         boolean writeAsString = (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         Integer[] array = (Integer[])object;
         jsonWriter.startArray();

         for (int i = 0; i < array.length; i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            Integer item = array[i];
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               int value = item;
               if (writeAsString) {
                  jsonWriter.writeString(value);
               } else {
                  jsonWriter.writeInt32(value);
               }
            }
         }

         jsonWriter.endArray();
      }
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         if (jsonWriter.isWriteTypeInfo(object, fieldType)) {
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, JSONB_TYPE_HASH);
         }

         boolean writeAsString = (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         Integer[] array = (Integer[])object;
         jsonWriter.startArray(array.length);

         for (int i = 0; i < array.length; i++) {
            Integer item = array[i];
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               int value = item;
               if (writeAsString) {
                  jsonWriter.writeString(value);
               } else {
                  jsonWriter.writeInt32(value);
               }
            }
         }
      }
   }
}
