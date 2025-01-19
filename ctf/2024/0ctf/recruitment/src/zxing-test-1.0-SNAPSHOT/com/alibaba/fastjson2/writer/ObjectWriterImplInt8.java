package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;

final class ObjectWriterImplInt8 extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplInt8 INSTANCE = new ObjectWriterImplInt8();

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         byte byteValue = (Byte)object;
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(byteValue);
         } else {
            jsonWriter.writeInt8(byteValue);
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         byte byteValue = (Byte)object;
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(byteValue);
         } else {
            jsonWriter.writeInt8(byteValue);
            long features2 = jsonWriter.getFeatures(features);
            if ((features2 & JSONWriter.Feature.WriteClassName.mask) != 0L
               && (features2 & JSONWriter.Feature.WriteNonStringKeyAsString.mask) == 0L
               && (features2 & JSONWriter.Feature.NotWriteNumberClassName.mask) == 0L
               && fieldType != Byte.class
               && fieldType != byte.class) {
               jsonWriter.writeRaw('B');
            }
         }
      }
   }
}
