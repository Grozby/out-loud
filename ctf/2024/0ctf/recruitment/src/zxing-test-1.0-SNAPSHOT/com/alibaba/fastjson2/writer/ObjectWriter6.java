package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.List;

public class ObjectWriter6<T> extends ObjectWriterAdapter<T> {
   public final FieldWriter fieldWriter0;
   public final FieldWriter fieldWriter1;
   public final FieldWriter fieldWriter2;
   public final FieldWriter fieldWriter3;
   public final FieldWriter fieldWriter4;
   public final FieldWriter fieldWriter5;

   public ObjectWriter6(Class<T> objectClass, String typeKey, String typeName, long features, List<FieldWriter> fieldWriters) {
      super(objectClass, typeKey, typeName, features, fieldWriters);
      this.fieldWriter0 = fieldWriters.get(0);
      this.fieldWriter1 = fieldWriters.get(1);
      this.fieldWriter2 = fieldWriters.get(2);
      this.fieldWriter3 = fieldWriters.get(3);
      this.fieldWriter4 = fieldWriters.get(4);
      this.fieldWriter5 = fieldWriters.get(5);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      long featuresAll = features | this.features | jsonWriter.getFeatures();
      boolean beanToArray = (featuresAll & JSONWriter.Feature.BeanToArray.mask) != 0L;
      if (jsonWriter.jsonb) {
         if (beanToArray) {
            this.writeArrayMappingJSONB(jsonWriter, object, fieldName, fieldType, features);
         } else {
            this.writeJSONB(jsonWriter, object, fieldName, fieldType, features);
         }
      } else if (beanToArray) {
         this.writeArrayMapping(jsonWriter, object, fieldName, fieldType, features | this.features);
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
            this.fieldWriter2.write(jsonWriter, object);
            this.fieldWriter3.write(jsonWriter, object);
            this.fieldWriter4.write(jsonWriter, object);
            this.fieldWriter5.write(jsonWriter, object);
            jsonWriter.endObject();
         }
      }
   }

   @Override
   public final FieldWriter getFieldWriter(long hashCode) {
      if (hashCode == this.fieldWriter0.hashCode) {
         return this.fieldWriter0;
      } else if (hashCode == this.fieldWriter1.hashCode) {
         return this.fieldWriter1;
      } else if (hashCode == this.fieldWriter2.hashCode) {
         return this.fieldWriter2;
      } else if (hashCode == this.fieldWriter3.hashCode) {
         return this.fieldWriter3;
      } else if (hashCode == this.fieldWriter4.hashCode) {
         return this.fieldWriter4;
      } else {
         return hashCode == this.fieldWriter5.hashCode ? this.fieldWriter5 : null;
      }
   }
}
