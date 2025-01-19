package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.MultiType;
import java.lang.reflect.Type;
import java.util.Collection;

final class ObjectArrayReaderMultiType implements ObjectReader {
   final Type[] types;
   final ObjectReader[] readers;

   ObjectArrayReaderMultiType(MultiType multiType) {
      Type[] types = new Type[multiType.size()];

      for (int i = 0; i < multiType.size(); i++) {
         types[i] = multiType.getType(i);
      }

      this.types = types;
      this.readers = new ObjectReader[types.length];
   }

   ObjectReader getObjectReader(JSONReader jsonReader, int index) {
      ObjectReader objectReader = this.readers[index];
      if (objectReader == null) {
         Type type = this.types[index];
         objectReader = jsonReader.getObjectReader(type);
         this.readers[index] = objectReader;
      }

      return objectReader;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         Object[] values = new Object[this.types.length];
         if (jsonReader.nextIfArrayStart()) {
            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               Object value;
               if (jsonReader.isReference()) {
                  String reference = jsonReader.readReference();
                  if ("..".equals(reference)) {
                     value = values;
                  } else {
                     value = null;
                     jsonReader.addResolveTask(values, i, JSONPath.of(reference));
                  }
               } else {
                  ObjectReader objectReader = this.getObjectReader(jsonReader, i);
                  value = objectReader.readObject(jsonReader, this.types[i], i, features);
               }

               values[i] = value;
               jsonReader.nextIfComma();
            }

            jsonReader.nextIfComma();
            return values;
         } else {
            throw new JSONException(jsonReader.info("TODO"));
         }
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int entryCnt = jsonReader.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         Object[] values = new Object[this.types.length];

         for (int i = 0; i < entryCnt; i++) {
            Object value;
            if (jsonReader.isReference()) {
               String reference = jsonReader.readReference();
               if ("..".equals(reference)) {
                  value = values;
               } else {
                  value = null;
                  jsonReader.addResolveTask(values, i, JSONPath.of(reference));
               }
            } else {
               ObjectReader objectReader = this.getObjectReader(jsonReader, i);
               value = objectReader.readObject(jsonReader, this.types[i], i, features);
            }

            values[i] = value;
         }

         return values;
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      return new Object[this.types.length];
   }
}
