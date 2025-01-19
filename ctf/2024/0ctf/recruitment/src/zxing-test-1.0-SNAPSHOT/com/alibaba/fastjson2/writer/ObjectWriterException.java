package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.List;

public class ObjectWriterException extends ObjectWriterAdapter<Exception> {
   public ObjectWriterException(Class objectType, long features, List<FieldWriter> fieldWriters) {
      super(objectType, null, null, features, fieldWriters);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      this.writeClassInfo(jsonWriter);
      int size = this.fieldWriters.size();
      jsonWriter.startObject();

      for (int i = 0; i < size; i++) {
         FieldWriter fw = this.fieldWriters.get(i);
         fw.write(jsonWriter, object);
      }

      jsonWriter.endObject();
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.jsonb) {
         this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
      } else if (this.hasFilter(jsonWriter)) {
         this.writeWithFilter(jsonWriter, object);
      } else {
         jsonWriter.startObject();
         if ((jsonWriter.getFeatures(features) & (JSONWriter.Feature.WriteClassName.mask | JSONWriter.Feature.WriteThrowableClassName.mask)) != 0L) {
            this.writeTypeInfo(jsonWriter);
         }

         for (FieldWriter fieldWriter : this.fieldWriters) {
            fieldWriter.write(jsonWriter, object);
         }

         jsonWriter.endObject();
      }
   }
}
