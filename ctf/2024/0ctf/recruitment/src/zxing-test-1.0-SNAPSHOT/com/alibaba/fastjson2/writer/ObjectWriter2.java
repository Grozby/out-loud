package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.List;

public class ObjectWriter2<T> extends ObjectWriterAdapter<T> {
   public final FieldWriter fieldWriter0;
   public final FieldWriter fieldWriter1;

   public ObjectWriter2(Class<T> objectClass, String typeKey, String typeName, long features, List<FieldWriter> fieldWriters) {
      super(objectClass, typeKey, typeName, features, fieldWriters);
      this.fieldWriter0 = fieldWriters.get(0);
      this.fieldWriter1 = fieldWriters.get(1);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      long featuresAll = features | this.features | jsonWriter.getFeatures();
      if (jsonWriter.jsonb) {
         if ((featuresAll & JSONWriter.Feature.BeanToArray.mask) != 0L) {
            this.writeArrayMappingJSONB(jsonWriter, object, fieldName, fieldType, features);
         } else {
            this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
         }
      } else if ((featuresAll & JSONWriter.Feature.BeanToArray.mask) != 0L) {
         this.writeArrayMapping(jsonWriter, object, fieldName, fieldType, features);
      } else {
         if (!this.serializable) {
            if ((featuresAll & JSONWriter.Feature.ErrorOnNoneSerializable.mask) != 0L) {
               this.errorOnNoneSerializable();
               return;
            }

            if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0L) {
               jsonWriter.writeNull();
               return;
            }
         }

         if (this.hasFilter(jsonWriter)) {
            this.writeWithFilter(jsonWriter, object, fieldName, fieldType, 0L);
         } else {
            jsonWriter.startObject();
            if (((features | this.features) & JSONWriter.Feature.WriteClassName.mask) != 0L || jsonWriter.isWriteTypeInfo(object, features)) {
               this.writeTypeInfo(jsonWriter);
            }

            this.fieldWriter0.write(jsonWriter, object);
            this.fieldWriter1.write(jsonWriter, object);
            jsonWriter.endObject();
         }
      }
   }

   @Override
   public final FieldWriter getFieldWriter(long hashCode) {
      if (hashCode == this.fieldWriter0.hashCode) {
         return this.fieldWriter0;
      } else {
         return hashCode == this.fieldWriter1.hashCode ? this.fieldWriter1 : null;
      }
   }
}
