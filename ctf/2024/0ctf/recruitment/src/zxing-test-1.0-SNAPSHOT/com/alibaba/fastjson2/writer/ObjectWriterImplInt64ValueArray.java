package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.function.Function;

final class ObjectWriterImplInt64ValueArray extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplInt64ValueArray INSTANCE = new ObjectWriterImplInt64ValueArray(null);
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("[J");
   static final long JSONB_TYPE_HASH = Fnv.hashCode64("[J");
   private final Function<Object, long[]> function;

   public ObjectWriterImplInt64ValueArray(Function<Object, long[]> function) {
      this.function = function;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         if (jsonWriter.isWriteTypeInfo(object, fieldType)) {
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, JSONB_TYPE_HASH);
         }

         long[] array;
         if (this.function != null) {
            array = this.function.apply(object);
         } else {
            array = (long[])object;
         }

         boolean writeAsString = (features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L;
         if (writeAsString) {
            jsonWriter.writeString(array);
         } else {
            jsonWriter.writeInt64(array);
         }
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         ObjectWriterProvider provider = jsonWriter.context.provider;
         ObjectWriter objectWriter = null;
         if ((provider.userDefineMask & 4L) != 0L) {
            objectWriter = jsonWriter.context.getObjectWriter(Long.class);
         }

         long[] array;
         if (this.function != null) {
            array = this.function.apply(object);
         } else {
            array = (long[])object;
         }

         if (objectWriter != null && objectWriter != ObjectWriterImplInt32.INSTANCE) {
            jsonWriter.startArray();

            for (int i = 0; i < array.length; i++) {
               if (i != 0) {
                  jsonWriter.writeComma();
               }

               objectWriter.write(jsonWriter, array[i], i, long.class, features);
            }

            jsonWriter.endArray();
         } else {
            jsonWriter.writeInt64(array);
         }
      }
   }
}
