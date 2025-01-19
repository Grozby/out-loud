package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

final class ObjectWriterMisc implements ObjectWriter {
   static final ObjectWriterMisc INSTANCE = new ObjectWriterMisc();

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         Class<?> objectClass = object.getClass();
         String objectClassName = objectClass.getName();
         String str;
         switch (objectClassName) {
            case "net.sf.json.JSONNull":
               jsonWriter.writeNull();
               return;
            case "java.net.Inet4Address":
            case "java.net.Inet6Address":
               str = ((InetAddress)object).getHostName();
               break;
            case "java.text.SimpleDateFormat":
               str = ((SimpleDateFormat)object).toPattern();
               break;
            case "java.util.regex.Pattern":
               str = ((Pattern)object).pattern();
               break;
            case "java.net.InetSocketAddress":
               InetSocketAddress address = (InetSocketAddress)object;
               jsonWriter.startObject();
               jsonWriter.writeName("address");
               jsonWriter.writeColon();
               jsonWriter.writeAny(address.getAddress());
               jsonWriter.writeName("port");
               jsonWriter.writeColon();
               jsonWriter.writeInt32(address.getPort());
               jsonWriter.endObject();
               return;
            case "com.fasterxml.jackson.databind.node.ArrayNode":
               str = object.toString();
               if (jsonWriter.isUTF8()) {
                  jsonWriter.writeRaw(str.getBytes(StandardCharsets.UTF_8));
               } else {
                  jsonWriter.writeRaw(str);
               }

               return;
            default:
               throw new JSONException("not support class : " + objectClassName);
         }

         jsonWriter.writeString(str);
      }
   }
}
