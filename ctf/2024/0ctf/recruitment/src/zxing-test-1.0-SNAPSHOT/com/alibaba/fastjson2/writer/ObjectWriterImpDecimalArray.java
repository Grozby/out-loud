package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;

final class ObjectWriterImpDecimalArray extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImpDecimalArray INSTANCE = new ObjectWriterImpDecimalArray();
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("[BigDecimal");

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         if (jsonWriter.isEnabled(JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask)) {
            jsonWriter.startArray();
            jsonWriter.endArray();
         } else {
            jsonWriter.writeNull();
         }
      } else {
         BigDecimal[] array = (BigDecimal[])object;
         jsonWriter.startArray();

         for (int i = 0; i < array.length; i++) {
            if (i != 0) {
               jsonWriter.writeComma();
            }

            jsonWriter.writeDecimal(array[i], 0L, null);
         }

         jsonWriter.endArray();
      }
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         if (jsonWriter.isWriteTypeInfo(object, fieldType)) {
            long JSONB_TYPE_HASH = -2138534155605614069L;
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, -2138534155605614069L);
         }

         BigDecimal[] array = (BigDecimal[])object;
         jsonWriter.startArray(array.length);

         for (BigDecimal bigDecimal : array) {
            jsonWriter.writeDecimal(bigDecimal, 0L, null);
         }
      }
   }
}
