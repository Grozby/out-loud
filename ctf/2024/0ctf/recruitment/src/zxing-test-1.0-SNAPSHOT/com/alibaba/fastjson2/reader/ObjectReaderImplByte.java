package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;

public final class ObjectReaderImplByte extends ObjectReaderPrimitive<Byte> {
   static final ObjectReaderImplByte INSTANCE = new ObjectReaderImplByte();
   public static final long HASH_TYPE = Fnv.hashCode64("B");

   ObjectReaderImplByte() {
      super(Byte.class);
   }

   public Byte readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      Integer i = jsonReader.readInt32();
      return i == null ? null : i.byteValue();
   }

   public Byte readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      Integer i = jsonReader.readInt32();
      return i == null ? null : i.byteValue();
   }
}
