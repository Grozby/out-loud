package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

class ObjectReaderImplInt8Array extends ObjectReaderPrimitive {
   static final ObjectReaderImplInt8Array INSTANCE = new ObjectReaderImplInt8Array(null);
   static final long HASH_TYPE = Fnv.hashCode64("[Byte");
   final String format;

   public ObjectReaderImplInt8Array(String format) {
      super(Byte[].class);
      this.format = format;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         Byte[] values = new Byte[16];
         int size = 0;

         while (!jsonReader.nextIfArrayEnd()) {
            if (jsonReader.isEnd()) {
               throw new JSONException(jsonReader.info("input end"));
            }

            int minCapacity = size + 1;
            if (minCapacity - values.length > 0) {
               int oldCapacity = values.length;
               int newCapacity = oldCapacity + (oldCapacity >> 1);
               if (newCapacity - minCapacity < 0) {
                  newCapacity = minCapacity;
               }

               values = Arrays.copyOf(values, newCapacity);
            }

            Integer i = jsonReader.readInt32();
            values[size++] = i == null ? null : i.byteValue();
         }

         jsonReader.nextIfComma();
         return Arrays.copyOf(values, size);
      } else if (jsonReader.current() == 'x') {
         return jsonReader.readBinary();
      } else {
         if (jsonReader.isString()) {
            if ("hex".equals(this.format)) {
               return jsonReader.readHex();
            }

            String strVal = jsonReader.readString();
            if (strVal.isEmpty()) {
               return null;
            }

            if ("base64".equals(this.format)) {
               return Base64.getDecoder().decode(strVal);
            }

            if ("gzip,base64".equals(this.format) || "gzip".equals(this.format)) {
               byte[] bytes = Base64.getDecoder().decode(strVal);

               try {
                  GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(bytes));
                  ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                  while (true) {
                     byte[] buf = new byte[1024];
                     int len = gzipIn.read(buf);
                     if (len == -1) {
                        return byteOut.toByteArray();
                     }

                     if (len > 0) {
                        byteOut.write(buf, 0, len);
                     }
                  }
               } catch (IOException var12) {
                  throw new JSONException(jsonReader.info("unzip bytes error."), var12);
               }
            }
         }

         throw new JSONException(jsonReader.info("TODO"));
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfMatch((byte)-110)) {
         long typeHashCode = jsonReader.readTypeHashCode();
         if (typeHashCode != HASH_TYPE) {
            throw new JSONException("not support autoType : " + jsonReader.getString());
         }
      }

      if (jsonReader.isString() && "hex".equals(this.format)) {
         return jsonReader.readHex();
      } else {
         int entryCnt = jsonReader.startArray();
         if (entryCnt == -1) {
            return null;
         } else {
            Byte[] array = new Byte[entryCnt];

            for (int i = 0; i < entryCnt; i++) {
               Integer integer = jsonReader.readInt32();
               array[i] = integer == null ? null : integer.byteValue();
            }

            return array;
         }
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      Byte[] array = new Byte[collection.size()];
      int i = 0;

      for (Object item : collection) {
         Byte value;
         if (item == null) {
            value = null;
         } else if (item instanceof Number) {
            value = ((Number)item).byteValue();
         } else {
            Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(item.getClass(), Byte.class);
            if (typeConvert == null) {
               throw new JSONException("can not cast to Byte " + item.getClass());
            }

            value = (Byte)typeConvert.apply(item);
         }

         array[i++] = value;
      }

      return array;
   }
}
