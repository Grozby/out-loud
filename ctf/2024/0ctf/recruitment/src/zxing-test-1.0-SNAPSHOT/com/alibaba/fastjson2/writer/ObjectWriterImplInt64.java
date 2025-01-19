package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;

final class ObjectWriterImplInt64 extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplInt64 INSTANCE = new ObjectWriterImplInt64(null);
   final Class defineClass;

   public ObjectWriterImplInt64(Class defineClass) {
      this.defineClass = defineClass;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         long longValue = (Long)object;
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(longValue);
         } else {
            jsonWriter.writeInt64(longValue);
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         long value = ((Number)object).longValue();
         if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeInt64(value);
            if (value >= -2147483648L && value <= 2147483647L && (features & JSONWriter.Feature.WriteClassName.mask) != 0L) {
               long contextFeatures = jsonWriter.getFeatures();
               if ((contextFeatures & JSONWriter.Feature.WriteClassName.mask) == 0L) {
                  boolean writeAsString = (contextFeatures & (JSONWriter.Feature.WriteNonStringValueAsString.mask | JSONWriter.Feature.WriteLongAsString.mask))
                     != 0L;
                  if (!writeAsString) {
                     jsonWriter.writeRaw('L');
                  }
               }
            }
         }
      }
   }
}
