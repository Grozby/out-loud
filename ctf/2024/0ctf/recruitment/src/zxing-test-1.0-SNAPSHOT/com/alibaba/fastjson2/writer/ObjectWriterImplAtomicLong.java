package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicLong;

final class ObjectWriterImplAtomicLong extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplAtomicLong INSTANCE = new ObjectWriterImplAtomicLong(null);
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("AtomicLong");
   final Class defineClass;

   public ObjectWriterImplAtomicLong(Class defineClass) {
      this.defineClass = defineClass;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         AtomicLong atomic = (AtomicLong)object;
         if (jsonWriter.isWriteTypeInfo(atomic, fieldType)) {
            long JSONB_TYPE_HASH = -1591858996898070466L;
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, -1591858996898070466L);
         }

         long longValue = atomic.longValue();
         jsonWriter.writeInt64(longValue);
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNumberNull();
      } else {
         jsonWriter.writeInt64(((Number)object).longValue());
      }
   }
}
