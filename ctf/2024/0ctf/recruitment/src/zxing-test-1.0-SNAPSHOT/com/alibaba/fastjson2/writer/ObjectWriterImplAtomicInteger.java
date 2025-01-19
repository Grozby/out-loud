package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

final class ObjectWriterImplAtomicInteger extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplAtomicInteger INSTANCE = new ObjectWriterImplAtomicInteger(null);
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("AtomicInteger");
   final Class defineClass;

   public ObjectWriterImplAtomicInteger(Class defineClass) {
      this.defineClass = defineClass;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         AtomicInteger atomic = (AtomicInteger)object;
         if (jsonWriter.isWriteTypeInfo(atomic, fieldType)) {
            long JSONB_TYPE_HASH = 7576651708426282938L;
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, 7576651708426282938L);
         }

         jsonWriter.writeInt32(atomic.intValue());
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         AtomicInteger atomic = (AtomicInteger)object;
         jsonWriter.writeInt32(atomic.intValue());
      }
   }
}
