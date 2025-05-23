package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

final class ObjectReaderImplString extends ObjectReaderPrimitive {
   static final ObjectReaderImplString INSTANCE = new ObjectReaderImplString();

   public ObjectReaderImplString() {
      super(String.class);
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readString();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.readString();
   }

   @Override
   public Object createInstance(Map map, long features) {
      return map == null ? null : JSON.toJSONString(map);
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      return collection == null ? null : JSON.toJSONString(collection);
   }
}
