package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ObjectReaderImplMapString extends ObjectReaderImplMapTyped {
   public ObjectReaderImplMapString(Class mapType, Class instanceType, long features) {
      super(mapType, instanceType, null, String.class, features, null);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, features);
      } else {
         boolean match = jsonReader.nextIfObjectStart();
         if (!match) {
            if (jsonReader.current() == '[') {
               jsonReader.next();
               if (jsonReader.current() == '{') {
                  Object arrayItem = this.readObject(jsonReader, String.class, fieldName, features);
                  if (jsonReader.nextIfArrayEnd()) {
                     jsonReader.nextIfComma();
                     return arrayItem;
                  }
               }

               throw new JSONException(jsonReader.info("expect '{', but '['"));
            }

            if (jsonReader.nextIfNullOrEmptyString() || jsonReader.nextIfMatchIdent('"', 'n', 'u', 'l', 'l', '"')) {
               return null;
            }
         }

         JSONReader.Context context = jsonReader.getContext();
         Map<String, Object> object = (Map<String, Object>)(this.instanceType == HashMap.class
            ? new HashMap<>()
            : (Map)this.createInstance(context.getFeatures() | features));
         long contextFeatures = features | context.getFeatures();

         for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
            String name = jsonReader.readFieldName();
            if (this.multiValue && jsonReader.nextIfArrayStart()) {
               List list = new JSONArray();

               while (!jsonReader.nextIfArrayEnd()) {
                  String value = jsonReader.readString();
                  list.add(value);
               }

               object.put(name, list);
            } else {
               String value = jsonReader.readString();
               if ((i != 0 || (contextFeatures & JSONReader.Feature.SupportAutoType.mask) == 0L || !name.equals(this.getTypeKey()))
                  && (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L)) {
                  Object origin = object.put(name, value);
                  if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                     if (origin instanceof Collection) {
                        ((Collection)origin).add(value);
                        object.put(name, origin);
                     } else {
                        JSONArray array = JSONArray.of(origin, value);
                        object.put(name, array);
                     }
                  }
               }
            }
         }

         jsonReader.nextIfMatch(',');
         return object;
      }
   }
}
