package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.text.DecimalFormat;

final class ObjectWriterImplFloat extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplFloat INSTANCE = new ObjectWriterImplFloat(null);
   private final DecimalFormat format;

   public ObjectWriterImplFloat(DecimalFormat format) {
      this.format = format;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         float value = (Float)object;
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeFloat(value);
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else if (this.format != null) {
         String str = this.format.format(object);
         jsonWriter.writeRaw(str);
      } else {
         float value = (Float)object;
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeFloat(value);
            long features2 = jsonWriter.getFeatures(features);
            if ((features2 & JSONWriter.Feature.WriteClassName.mask) != 0L
               && (features2 & JSONWriter.Feature.WriteNonStringKeyAsString.mask) == 0L
               && (features2 & JSONWriter.Feature.NotWriteNumberClassName.mask) == 0L
               && fieldType != Float.class
               && fieldType != float.class) {
               jsonWriter.writeRaw('F');
            }
         }
      }
   }
}
