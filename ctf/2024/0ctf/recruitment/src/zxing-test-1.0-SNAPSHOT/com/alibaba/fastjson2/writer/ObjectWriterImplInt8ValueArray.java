package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

final class ObjectWriterImplInt8ValueArray extends ObjectWriterPrimitiveImpl {
   static final ObjectWriterImplInt8ValueArray INSTANCE = new ObjectWriterImplInt8ValueArray(null);
   static final byte[] JSONB_TYPE_NAME_BYTES = JSONB.toBytes("[B");
   static final long JSONB_TYPE_HASH = Fnv.hashCode64("[B");
   private final Function<Object, byte[]> function;

   public ObjectWriterImplInt8ValueArray(Function<Object, byte[]> function) {
      this.function = function;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.isWriteTypeInfo(object, fieldType)) {
         if (object == byte[].class) {
            jsonWriter.writeTypeName(JSONB_TYPE_NAME_BYTES, JSONB_TYPE_HASH);
         } else {
            jsonWriter.writeTypeName(object.getClass().getName());
         }
      }

      byte[] bytes;
      if (this.function != null && object != null) {
         bytes = this.function.apply(object);
      } else {
         bytes = (byte[])object;
      }

      jsonWriter.writeBinary(bytes);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeArrayNull();
      } else {
         byte[] bytes;
         if (this.function != null) {
            bytes = this.function.apply(object);
         } else {
            bytes = (byte[])object;
         }

         String format = jsonWriter.context.getDateFormat();
         if ("millis".equals(format)) {
            format = null;
         }

         if ("gzip".equals(format) || "gzip,base64".equals(format)) {
            GZIPOutputStream gzipOut = null;

            try {
               ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
               if (bytes.length < 512) {
                  gzipOut = new GZIPOutputStream(byteOut, bytes.length);
               } else {
                  gzipOut = new GZIPOutputStream(byteOut);
               }

               gzipOut.write(bytes);
               gzipOut.finish();
               bytes = byteOut.toByteArray();
            } catch (IOException var14) {
               throw new JSONException("write gzipBytes error", var14);
            } finally {
               IOUtils.close(gzipOut);
            }
         }

         if (!"base64".equals(format)
            && !"gzip,base64".equals(format)
            && (jsonWriter.getFeatures(features) & JSONWriter.Feature.WriteByteArrayAsBase64.mask) == 0L) {
            jsonWriter.writeInt8(bytes);
         } else {
            jsonWriter.writeBase64(bytes);
         }
      }
   }
}
