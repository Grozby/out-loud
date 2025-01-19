package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;

class ObjectReaderImplCharacter extends ObjectReaderPrimitive {
   static final ObjectReaderImplCharacter INSTANCE = new ObjectReaderImplCharacter();

   ObjectReaderImplCharacter() {
      super(Character.class);
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.nextIfNull() ? null : jsonReader.readCharValue();
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String str = jsonReader.readString();
      return str == null ? null : str.charAt(0);
   }
}
