package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.BitSet;

public final class ObjectReaderImplBitSet extends ObjectReaderPrimitive<BitSet> {
   static final ObjectReaderImplBitSet INSTANCE = new ObjectReaderImplBitSet();
   public static final long HASH_TYPE = Fnv.hashCode64("BitSet");

   public ObjectReaderImplBitSet() {
      super(BitSet.class);
   }

   public BitSet readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         if (jsonReader.nextIfMatch((byte)-110)) {
            long typeHash = jsonReader.readTypeHashCode();
            if (typeHash != HASH_TYPE) {
               String typeName = jsonReader.getString();
               throw new JSONException(jsonReader.info(typeName));
            }
         }

         byte[] bytes = jsonReader.readBinary();
         return BitSet.valueOf(bytes);
      }
   }

   public BitSet readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         byte[] bytes = jsonReader.readBinary();
         return BitSet.valueOf(bytes);
      }
   }
}
