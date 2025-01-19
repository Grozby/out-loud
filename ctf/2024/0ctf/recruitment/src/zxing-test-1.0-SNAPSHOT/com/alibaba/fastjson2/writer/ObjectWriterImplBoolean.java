package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;

final class ObjectWriterImplBoolean extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplBoolean INSTANCE = new ObjectWriterImplBoolean();

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      this.write(jsonWriter, object, fieldName, fieldType, features);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeBooleanNull();
      } else {
         boolean value = (Boolean)object;
         if ((jsonWriter.getFeatures(features) & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
            jsonWriter.writeString(value);
         } else {
            jsonWriter.writeBool(value);
         }
      }
   }
}
