package com.alibaba.fastjson2;

import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.MultiType;
import com.alibaba.fastjson2.util.ParameterizedTypeImpl;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public interface JSONB {
   static void dump(byte[] jsonbBytes) {
      System.out.println(toJSONString(jsonbBytes, true));
   }

   static void dump(byte[] jsonbBytes, SymbolTable symbolTable) {
      JSONBDump dump = new JSONBDump(jsonbBytes, symbolTable, true);
      String str = dump.toString();
      System.out.println(str);
   }

   static byte[] toBytes(boolean v) {
      return new byte[]{(byte)(v ? -79 : -80)};
   }

   static byte[] toBytes(int i) {
      if (i >= -16 && i <= 47) {
         return new byte[]{(byte)i};
      } else {
         JSONWriter jsonWriter = JSONWriter.ofJSONB();

         byte[] var2;
         try {
            jsonWriter.writeInt32(i);
            var2 = jsonWriter.getBytes();
         } catch (Throwable var5) {
            if (jsonWriter != null) {
               try {
                  jsonWriter.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (jsonWriter != null) {
            jsonWriter.close();
         }

         return var2;
      }
   }

   static byte[] toBytes(byte i) {
      JSONWriter jsonWriter = JSONWriter.ofJSONB();

      byte[] var2;
      try {
         jsonWriter.writeInt8(i);
         var2 = jsonWriter.getBytes();
      } catch (Throwable var5) {
         if (jsonWriter != null) {
            try {
               jsonWriter.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (jsonWriter != null) {
         jsonWriter.close();
      }

      return var2;
   }

   static byte[] toBytes(short i) {
      JSONWriter jsonWriter = JSONWriter.ofJSONB();

      byte[] var2;
      try {
         jsonWriter.writeInt16(i);
         var2 = jsonWriter.getBytes();
      } catch (Throwable var5) {
         if (jsonWriter != null) {
            try {
               jsonWriter.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (jsonWriter != null) {
         jsonWriter.close();
      }

      return var2;
   }

   static byte[] toBytes(long i) {
      if (i >= -8L && i <= 15L) {
         return new byte[]{(byte)((int)(-40L + (i - -8L)))};
      } else {
         JSONWriter jsonWriter = JSONWriter.ofJSONB();

         byte[] var3;
         try {
            jsonWriter.writeInt64(i);
            var3 = jsonWriter.getBytes();
         } catch (Throwable var6) {
            if (jsonWriter != null) {
               try {
                  jsonWriter.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (jsonWriter != null) {
            jsonWriter.close();
         }

         return var3;
      }
   }

   static int writeInt(byte[] bytes, int off, int i) {
      if (i >= -16 && i <= 47) {
         bytes[off] = (byte)i;
         return 1;
      } else if (i >= -2048 && i <= 2047) {
         bytes[off] = (byte)(56 + (i >> 8));
         bytes[off + 1] = (byte)i;
         return 2;
      } else if (i >= -262144 && i <= 262143) {
         bytes[off] = (byte)(68 + (i >> 16));
         bytes[off + 1] = (byte)(i >> 8);
         bytes[off + 2] = (byte)i;
         return 3;
      } else {
         bytes[off] = 72;
         bytes[off + 1] = (byte)(i >>> 24);
         bytes[off + 2] = (byte)(i >>> 16);
         bytes[off + 3] = (byte)(i >>> 8);
         bytes[off + 4] = (byte)i;
         return 5;
      }
   }

   static Object parse(byte[] jsonbBytes, JSONReader.Context context) {
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      Object var4;
      try {
         Object object = reader.readAnyObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var4 = object;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static Object parse(byte[] jsonbBytes, JSONReader.Feature... features) {
      JSONReaderJSONB reader = new JSONReaderJSONB(
         new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), features), jsonbBytes, 0, jsonbBytes.length
      );

      Object var4;
      try {
         Object object = reader.readAnyObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var4 = object;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static Object parse(InputStream in, JSONReader.Context context) {
      JSONReaderJSONB reader = new JSONReaderJSONB(context, in);

      Object var4;
      try {
         Object object = reader.readAny();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var4 = object;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static Object parse(byte[] jsonbBytes, SymbolTable symbolTable, JSONReader.Feature... features) {
      JSONReaderJSONB reader = new JSONReaderJSONB(
         new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), symbolTable, features), jsonbBytes, 0, jsonbBytes.length
      );

      Object var5;
      try {
         Object object = reader.readAnyObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var5 = object;
      } catch (Throwable var7) {
         try {
            reader.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      reader.close();
      return var5;
   }

   static JSONObject parseObject(byte[] jsonbBytes) {
      JSONReaderJSONB reader = new JSONReaderJSONB(new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider()), jsonbBytes, 0, jsonbBytes.length);

      JSONObject var3;
      try {
         JSONObject object = (JSONObject)reader.readObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var3 = object;
      } catch (Throwable var5) {
         try {
            reader.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      reader.close();
      return var3;
   }

   static JSONObject parseObject(byte[] jsonbBytes, JSONReader.Feature... features) {
      JSONReaderJSONB reader = new JSONReaderJSONB(
         new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), features), jsonbBytes, 0, jsonbBytes.length
      );

      JSONObject var4;
      try {
         JSONObject object = (JSONObject)reader.readObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var4 = object;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static JSONObject parseObject(InputStream in, JSONReader.Context context) {
      JSONReaderJSONB reader = new JSONReaderJSONB(context, in);

      JSONObject var4;
      try {
         JSONObject object = (JSONObject)reader.readObject();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var4 = object;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static JSONArray parseArray(byte[] jsonbBytes) {
      JSONReaderJSONB reader = new JSONReaderJSONB(new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider()), jsonbBytes, 0, jsonbBytes.length);

      JSONArray var3;
      try {
         JSONArray array = (JSONArray)reader.readArray();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(array);
         }

         var3 = array;
      } catch (Throwable var5) {
         try {
            reader.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      reader.close();
      return var3;
   }

   static JSONArray parseArray(InputStream in, JSONReader.Context context) {
      JSONReaderJSONB reader = new JSONReaderJSONB(context, in);

      JSONArray var4;
      try {
         JSONArray array = (JSONArray)reader.readArray();
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(array);
         }

         var4 = array;
      } catch (Throwable var6) {
         try {
            reader.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      reader.close();
      return var4;
   }

   static <T> List<T> parseArray(byte[] jsonbBytes, Type type) {
      if (jsonbBytes != null && jsonbBytes.length != 0) {
         Type paramType = new ParameterizedTypeImpl(new Type[]{type}, null, List.class);
         JSONReaderJSONB reader = new JSONReaderJSONB(new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider()), jsonbBytes, 0, jsonbBytes.length);

         List var5;
         try {
            List<T> list = reader.read(paramType);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            var5 = list;
         } catch (Throwable var7) {
            try {
               reader.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         reader.close();
         return var5;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] jsonbBytes, Type type, JSONReader.Feature... features) {
      if (jsonbBytes != null && jsonbBytes.length != 0) {
         Type paramType = new ParameterizedTypeImpl(new Type[]{type}, null, List.class);
         JSONReaderJSONB reader = new JSONReaderJSONB(
            new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), features), jsonbBytes, 0, jsonbBytes.length
         );

         List var6;
         try {
            List<T> list = reader.read(paramType);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            var6 = list;
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] jsonbBytes, Type... types) {
      if (jsonbBytes != null && jsonbBytes.length != 0) {
         JSONReaderJSONB reader = new JSONReaderJSONB(new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider()), jsonbBytes, 0, jsonbBytes.length);

         List var4;
         try {
            List<T> list = reader.readList(types);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            var4 = list;
         } catch (Throwable var6) {
            try {
               reader.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         reader.close();
         return var4;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] jsonbBytes, Type[] types, JSONReader.Feature... features) {
      if (jsonbBytes != null && jsonbBytes.length != 0) {
         JSONReaderJSONB reader = new JSONReaderJSONB(
            new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), features), jsonbBytes, 0, jsonbBytes.length
         );

         List var5;
         try {
            List<T> list = reader.readList(types);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            var5 = list;
         } catch (Throwable var7) {
            try {
               reader.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         reader.close();
         return var5;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] jsonbBytes, Class<T> objectClass) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(new JSONReader.Context(provider), jsonbBytes, 0, jsonbBytes.length);

      ObjectReader objectReader;
      try {
         Object object;
         if (objectClass == Object.class) {
            object = jsonReader.readAny();
         } else {
            objectReader = provider.getObjectReader(objectClass, (JSONFactory.defaultReaderFeatures & JSONReader.Feature.FieldBased.mask) != 0L);
            object = objectReader.readJSONBObject(jsonReader, objectClass, null, 0L);
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         objectReader = (ObjectReader)object;
      } catch (Throwable var7) {
         try {
            jsonReader.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      jsonReader.close();
      return (T)objectReader;
   }

   static <T> T parseObject(byte[] jsonbBytes, Type objectType) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      ObjectReader objectReader = provider.getObjectReader(objectType);
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(new JSONReader.Context(provider), jsonbBytes, 0, jsonbBytes.length);

      Object var6;
      try {
         T object = (T)objectReader.readJSONBObject(jsonReader, objectType, null, 0L);
         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         var6 = object;
      } catch (Throwable var8) {
         try {
            jsonReader.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      jsonReader.close();
      return (T)var6;
   }

   static <T> T parseObject(byte[] jsonbBytes, Type... types) {
      return parseObject(jsonbBytes, new MultiType(types));
   }

   static <T> T parseObject(byte[] jsonbBytes, Type objectType, SymbolTable symbolTable) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      ObjectReader objectReader = provider.getObjectReader(objectType);
      JSONReaderJSONB reader = new JSONReaderJSONB(new JSONReader.Context(provider, symbolTable), jsonbBytes, 0, jsonbBytes.length);

      Object var7;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectType, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var7 = object;
      } catch (Throwable var9) {
         try {
            reader.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      reader.close();
      return (T)var7;
   }

   static <T> T parseObject(byte[] jsonbBytes, Type objectType, SymbolTable symbolTable, JSONReader.Feature... features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, symbolTable, features);
      boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = provider.getObjectReader(objectType, fieldBased);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      Object var10;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectType, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var10 = object;
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      return (T)var10;
   }

   static <T> T parseObject(byte[] jsonbBytes, Class<T> objectClass, Filter filter, JSONReader.Feature... features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, filter, features);
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      Object var13;
      try {
         for (int i = 0; i < features.length; i++) {
            context.features = context.features | features[i].mask;
         }

         Object object;
         if (objectClass == Object.class) {
            object = jsonReader.readAnyObject();
         } else {
            boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
            ObjectReader objectReader = provider.getObjectReader(objectClass, fieldBased);
            object = objectReader.readJSONBObject(jsonReader, objectClass, null, 0L);
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         var13 = object;
      } catch (Throwable var11) {
         try {
            jsonReader.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      jsonReader.close();
      return (T)var13;
   }

   static <T> T parseObject(byte[] jsonbBytes, Type objectType, SymbolTable symbolTable, Filter[] filters, JSONReader.Feature... features) {
      if (jsonbBytes != null && jsonbBytes.length != 0) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider, symbolTable, filters, features);
         JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

         Object var14;
         try {
            for (int i = 0; i < features.length; i++) {
               context.features = context.features | features[i].mask;
            }

            Object object;
            if (objectType == Object.class) {
               object = jsonReader.readAnyObject();
            } else {
               boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
               ObjectReader objectReader = provider.getObjectReader(objectType, fieldBased);
               object = objectReader.readJSONBObject(jsonReader, objectType, null, 0L);
            }

            if (jsonReader.resolveTasks != null) {
               jsonReader.handleResolveTasks(object);
            }

            var14 = object;
         } catch (Throwable var12) {
            try {
               jsonReader.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }

            throw var12;
         }

         jsonReader.close();
         return (T)var14;
      } else {
         return null;
      }
   }

   static <T> T copy(T object, JSONWriter.Feature... features) {
      return JSON.copy(object, features);
   }

   static <T> T parseObject(byte[] jsonbBytes, TypeReference typeReference, JSONReader.Feature... features) {
      return parseObject(jsonbBytes, typeReference.getType(), features);
   }

   static <T> T parseObject(InputStream in, Class objectClass, JSONReader.Feature... features) throws IOException {
      return parseObject(in, objectClass, JSONFactory.createReadContext(features));
   }

   static <T> T parseObject(InputStream in, Type objectType, JSONReader.Feature... features) throws IOException {
      return parseObject(in, objectType, JSONFactory.createReadContext(features));
   }

   static <T> T parseObject(InputStream in, Type objectType, JSONReader.Context context) {
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, in);

      ObjectReader objectReader;
      try {
         Object object;
         if (objectType == Object.class) {
            object = jsonReader.readAny();
         } else {
            objectReader = context.getObjectReader(objectType);
            object = objectReader.readJSONBObject(jsonReader, objectType, null, 0L);
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         objectReader = (ObjectReader)object;
      } catch (Throwable var7) {
         try {
            jsonReader.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      jsonReader.close();
      return (T)objectReader;
   }

   static <T> T parseObject(InputStream in, Class objectClass, JSONReader.Context context) {
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, in);

      ObjectReader objectReader;
      try {
         Object object;
         if (objectClass == Object.class) {
            object = jsonReader.readAny();
         } else {
            objectReader = context.getObjectReader(objectClass);
            object = objectReader.readJSONBObject(jsonReader, objectClass, null, 0L);
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         objectReader = (ObjectReader)object;
      } catch (Throwable var7) {
         try {
            jsonReader.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      jsonReader.close();
      return (T)objectReader;
   }

   static <T> T parseObject(InputStream in, int length, Type objectType, JSONReader.Context context) throws IOException {
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(cacheItem, null);
      if (bytes == null) {
         bytes = new byte[8192];
      }

      Object var8;
      try {
         if (bytes.length < length) {
            bytes = new byte[length];
         }

         int read = in.read(bytes, 0, length);
         if (read != length) {
            throw new IllegalArgumentException("deserialize failed. expected read length: " + length + " but actual read: " + read);
         }

         var8 = parseObject(bytes, 0, length, objectType, context);
      } finally {
         JSONFactory.BYTES_UPDATER.lazySet(cacheItem, bytes);
      }

      return (T)var8;
   }

   static <T> T parseObject(InputStream in, int length, Type objectType, JSONReader.Feature... features) throws IOException {
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(cacheItem, null);
      if (bytes == null) {
         bytes = new byte[8192];
      }

      Object var8;
      try {
         if (bytes.length < length) {
            bytes = new byte[length];
         }

         int read = in.read(bytes, 0, length);
         if (read != length) {
            throw new IllegalArgumentException("deserialize failed. expected read length: " + length + " but actual read: " + read);
         }

         var8 = parseObject(bytes, 0, length, objectType, features);
      } finally {
         JSONFactory.BYTES_UPDATER.lazySet(cacheItem, bytes);
      }

      return (T)var8;
   }

   static <T> T parseObject(byte[] jsonbBytes, Class<T> objectClass, JSONReader.Feature... features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, features);
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      Object var11;
      try {
         Object object;
         if (objectClass == Object.class) {
            object = jsonReader.readAnyObject();
         } else {
            boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
            ObjectReader objectReader = provider.getObjectReader(objectClass, fieldBased);
            if ((context.features & JSONReader.Feature.SupportArrayToBean.mask) != 0L && jsonReader.isArray() && objectReader instanceof ObjectReaderBean) {
               object = objectReader.readArrayMappingJSONBObject(jsonReader, objectClass, null, 0L);
            } else {
               object = objectReader.readJSONBObject(jsonReader, objectClass, null, 0L);
            }
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         var11 = object;
      } catch (Throwable var10) {
         try {
            jsonReader.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      jsonReader.close();
      return (T)var11;
   }

   static <T> T parseObject(byte[] jsonbBytes, Class<T> objectClass, JSONReader.Context context) {
      JSONReaderJSONB jsonReader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      ObjectReader objectReader;
      try {
         Object object;
         if (objectClass == Object.class) {
            object = jsonReader.readAnyObject();
         } else {
            objectReader = context.provider.getObjectReader(objectClass, (context.features & JSONReader.Feature.FieldBased.mask) != 0L);
            if ((context.features & JSONReader.Feature.SupportArrayToBean.mask) != 0L && jsonReader.isArray() && objectReader instanceof ObjectReaderBean) {
               object = objectReader.readArrayMappingJSONBObject(jsonReader, objectClass, null, 0L);
            } else {
               object = objectReader.readJSONBObject(jsonReader, objectClass, null, 0L);
            }
         }

         if (jsonReader.resolveTasks != null) {
            jsonReader.handleResolveTasks(object);
         }

         objectReader = (ObjectReader)object;
      } catch (Throwable var7) {
         try {
            jsonReader.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      jsonReader.close();
      return (T)objectReader;
   }

   static <T> T parseObject(byte[] jsonbBytes, Type objectClass, JSONReader.Feature... features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, features);
      ObjectReader objectReader = provider.getObjectReader(objectClass, (context.features & JSONReader.Feature.FieldBased.mask) != 0L);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, 0, jsonbBytes.length);

      Object var8;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var8 = object;
      } catch (Throwable var10) {
         try {
            reader.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      reader.close();
      return (T)var8;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Class<T> objectClass) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider);
      boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = provider.getObjectReader(objectClass, fieldBased);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var10;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var10 = object;
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      return (T)var10;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Type type) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider);
      boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = provider.getObjectReader(type, fieldBased);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var10;
      try {
         T object = (T)objectReader.readJSONBObject(reader, type, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var10 = object;
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      return (T)var10;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Class<T> objectClass, JSONReader.Feature... features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, features);
      boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
      ObjectReader objectReader = provider.getObjectReader(objectClass, fieldBased);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var11;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var11 = object;
      } catch (Throwable var13) {
         try {
            reader.close();
         } catch (Throwable var12) {
            var13.addSuppressed(var12);
         }

         throw var13;
      }

      reader.close();
      return (T)var11;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Type objectType, JSONReader.Context context) {
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var9;
      try {
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader objectReader = context.provider.getObjectReader(objectType, fieldBased);
         T object = (T)objectReader.readJSONBObject(reader, objectType, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var9 = object;
      } catch (Throwable var11) {
         try {
            reader.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      reader.close();
      return (T)var9;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Type objectType, JSONReader.Feature... features) {
      JSONReader.Context context = JSONFactory.createReadContext(features);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var9;
      try {
         ObjectReader objectReader = reader.getObjectReader(objectType);
         T object = (T)objectReader.readJSONBObject(reader, objectType, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var9 = object;
      } catch (Throwable var11) {
         try {
            reader.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      reader.close();
      return (T)var9;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Class<T> objectClass, SymbolTable symbolTable) {
      JSONReader.Context context = JSONFactory.createReadContext(symbolTable);
      ObjectReader objectReader = context.getObjectReader(objectClass);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var9;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var9 = object;
      } catch (Throwable var11) {
         try {
            reader.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      reader.close();
      return (T)var9;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Type objectClass, SymbolTable symbolTable) {
      JSONReader.Context context = JSONFactory.createReadContext(symbolTable);
      ObjectReader objectReader = context.getObjectReader(objectClass);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var9;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var9 = object;
      } catch (Throwable var11) {
         try {
            reader.close();
         } catch (Throwable var10) {
            var11.addSuppressed(var10);
         }

         throw var11;
      }

      reader.close();
      return (T)var9;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Class<T> objectClass, SymbolTable symbolTable, JSONReader.Feature... features) {
      JSONReader.Context context = JSONFactory.createReadContext(symbolTable, features);
      ObjectReader objectReader = context.getObjectReader(objectClass);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var10;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var10 = object;
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      return (T)var10;
   }

   static <T> T parseObject(byte[] jsonbBytes, int off, int len, Type objectClass, SymbolTable symbolTable, JSONReader.Feature... features) {
      JSONReader.Context context = JSONFactory.createReadContext(symbolTable, features);
      ObjectReader objectReader = context.getObjectReader(objectClass);
      JSONReaderJSONB reader = new JSONReaderJSONB(context, jsonbBytes, off, len);

      Object var10;
      try {
         T object = (T)objectReader.readJSONBObject(reader, objectClass, null, 0L);
         if (reader.resolveTasks != null) {
            reader.handleResolveTasks(object);
         }

         var10 = object;
      } catch (Throwable var12) {
         try {
            reader.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      reader.close();
      return (T)var10;
   }

   static byte[] toBytes(String str) {
      if (str == null) {
         return new byte[]{-81};
      } else {
         if (JDKUtils.JVM_VERSION == 8) {
            char[] chars = JDKUtils.getCharArray(str);
            int strlen = chars.length;
            if (strlen <= 47) {
               boolean ascii = true;

               for (int i = 0; i < strlen; i++) {
                  if (chars[i] > 127) {
                     ascii = false;
                     break;
                  }
               }

               if (ascii) {
                  byte[] bytes = new byte[chars.length + 1];
                  bytes[0] = (byte)(strlen + 73);

                  for (int ix = 0; ix < strlen; ix++) {
                     bytes[ix + 1] = (byte)chars[ix];
                  }

                  return bytes;
               }
            }
         } else if (JDKUtils.STRING_VALUE != null) {
            int coder = JDKUtils.STRING_CODER.applyAsInt(str);
            if (coder == 0) {
               byte[] value = JDKUtils.STRING_VALUE.apply(str);
               int strlen = value.length;
               if (strlen <= 47) {
                  byte[] bytes = new byte[value.length + 1];
                  bytes[0] = (byte)(strlen + 73);
                  System.arraycopy(value, 0, bytes, 1, value.length);
                  return bytes;
               }
            }
         }

         JSONWriterJSONB writer = new JSONWriterJSONB(new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider), null);

         byte[] var11;
         try {
            writer.writeString(str);
            var11 = writer.getBytes();
         } catch (Throwable var7) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         writer.close();
         return var11;
      }
   }

   static byte[] toBytes(String str, Charset charset) {
      if (str == null) {
         return new byte[]{-81};
      } else {
         byte type;
         if (charset == StandardCharsets.UTF_16) {
            type = 123;
         } else if (charset == StandardCharsets.UTF_16BE) {
            type = 125;
         } else if (charset == StandardCharsets.UTF_16LE) {
            type = 124;
         } else if (charset == StandardCharsets.UTF_8) {
            type = 122;
         } else if (charset != StandardCharsets.US_ASCII && charset != StandardCharsets.ISO_8859_1) {
            if (charset == null || !"GB18030".equals(charset.name())) {
               return toBytes(str);
            }

            type = 126;
         } else {
            type = 121;
         }

         byte[] utf16 = str.getBytes(charset);
         int byteslen = 2 + utf16.length;
         if (utf16.length <= 47) {
            byteslen += 0;
         } else if (utf16.length <= 2047) {
            byteslen++;
         } else if (utf16.length <= 262143) {
            byteslen += 2;
         } else {
            byteslen += 4;
         }

         byte[] bytes = new byte[byteslen];
         bytes[0] = type;
         int off = 1;
         off += writeInt(bytes, off, utf16.length);
         System.arraycopy(utf16, 0, bytes, off, utf16.length);
         return bytes;
      }
   }

   static byte[] toBytes(Object object) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider);
      JSONWriterJSONB writer = new JSONWriterJSONB(context, null);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            Class<?> valueClass = object.getClass();
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            ObjectWriter objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            objectWriter.writeJSONB(writer, object, null, null, 0L);
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         try {
            writer.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      writer.close();
      return var8;
   }

   static byte[] toBytes(Object object, JSONWriter.Context context) {
      if (context == null) {
         context = JSONFactory.createWriteContext();
      }

      JSONWriterJSONB writer = new JSONWriterJSONB(context, null);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            Class<?> valueClass = object.getClass();
            ObjectWriter objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            if ((context.features & JSONWriter.Feature.BeanToArray.mask) != 0L) {
               objectWriter.writeArrayMappingJSONB(writer, object, null, null, 0L);
            } else {
               objectWriter.writeJSONB(writer, object, null, null, 0L);
            }
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         try {
            writer.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      writer.close();
      return var8;
   }

   static byte[] toBytes(Object object, SymbolTable symbolTable) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider);
      JSONWriterJSONB writer = new JSONWriterJSONB(context, symbolTable);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.setRootObject(object);
            Class<?> valueClass = object.getClass();
            ObjectWriter objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.writeJSONB(writer, object, null, null, 0L);
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         try {
            writer.close();
         } catch (Throwable var6) {
            var7.addSuppressed(var6);
         }

         throw var7;
      }

      writer.close();
      return var8;
   }

   static byte[] toBytes(Object object, SymbolTable symbolTable, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      JSONWriterJSONB writer = new JSONWriterJSONB(context, symbolTable);

      byte[] var10;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.setRootObject(object);
            Class<?> valueClass = object.getClass();
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            ObjectWriter objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            if ((context.features & JSONWriter.Feature.BeanToArray.mask) != 0L) {
               objectWriter.writeArrayMappingJSONB(writer, object, null, null, 0L);
            } else {
               objectWriter.writeJSONB(writer, object, null, null, 0L);
            }
         }

         var10 = writer.getBytes();
      } catch (Throwable var9) {
         try {
            writer.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      writer.close();
      return var10;
   }

   static byte[] toBytes(Object object, SymbolTable symbolTable, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      context.configFilter(filters);
      JSONWriterJSONB writer = new JSONWriterJSONB(context, symbolTable);

      byte[] var11;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.setRootObject(object);
            Class<?> valueClass = object.getClass();
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            ObjectWriter objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            if ((context.features & JSONWriter.Feature.BeanToArray.mask) != 0L) {
               objectWriter.writeArrayMappingJSONB(writer, object, null, null, 0L);
            } else {
               objectWriter.writeJSONB(writer, object, null, null, 0L);
            }
         }

         var11 = writer.getBytes();
      } catch (Throwable var10) {
         try {
            writer.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      writer.close();
      return var11;
   }

   static byte[] toBytes(Object object, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      JSONWriterJSONB writer = new JSONWriterJSONB(context, null);

      byte[] var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            Class<?> valueClass = object.getClass();
            ObjectWriter objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            if ((context.features & JSONWriter.Feature.BeanToArray.mask) != 0L) {
               objectWriter.writeArrayMappingJSONB(writer, object, null, null, 0L);
            } else {
               objectWriter.writeJSONB(writer, object, null, null, 0L);
            }
         }

         var9 = writer.getBytes();
      } catch (Throwable var8) {
         try {
            writer.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      writer.close();
      return var9;
   }

   static SymbolTable symbolTable(String... names) {
      return new SymbolTable(names);
   }

   static String toJSONString(byte[] jsonbBytes) {
      return new JSONBDump(jsonbBytes, false).toString();
   }

   static String toJSONString(byte[] jsonbBytes, boolean raw) {
      return new JSONBDump(jsonbBytes, raw).toString();
   }

   static String toJSONString(byte[] jsonbBytes, SymbolTable symbolTable) {
      return new JSONBDump(jsonbBytes, symbolTable, false).toString();
   }

   static int writeTo(OutputStream out, Object object, JSONWriter.Feature... features) {
      try {
         JSONWriterJSONB writer = new JSONWriterJSONB(new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider), null);

         int var9;
         try {
            writer.config(features);
            if (object == null) {
               writer.writeNull();
            } else {
               writer.setRootObject(object);
               Class<?> valueClass = object.getClass();
               ObjectWriter objectWriter = writer.getObjectWriter(valueClass, valueClass);
               objectWriter.writeJSONB(writer, object, null, null, 0L);
            }

            var9 = writer.flushTo(out);
         } catch (Throwable var7) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         writer.close();
         return var9;
      } catch (IOException var8) {
         throw new JSONException("writeJSONString error", var8);
      }
   }

   static byte[] fromJSONString(String str) {
      return toBytes(JSON.parse(str));
   }

   static byte[] fromJSONBytes(byte[] jsonUtf8Bytes) {
      JSONReader reader = JSONReader.of(jsonUtf8Bytes);
      ObjectReader objectReader = reader.getObjectReader(Object.class);
      Object object = objectReader.readObject(reader, null, null, 0L);
      return toBytes(object);
   }

   static String typeName(byte type) {
      switch (type) {
         case -111:
            return "BINARY " + Integer.toString(type);
         case -110:
            return "TYPED_ANY " + Integer.toString(type);
         case -109:
            return "REFERENCE " + Integer.toString(type);
         case -91:
            return "OBJECT_END " + Integer.toString(type);
         case -90:
            return "OBJECT " + Integer.toString(type);
         case -89:
            return "LOCAL_TIME " + Integer.toString(type);
         case -88:
            return "LOCAL_DATETIME " + Integer.toString(type);
         case -87:
            return "LOCAL_DATE " + Integer.toString(type);
         case -86:
            return "TIMESTAMP_WITH_TIMEZONE " + Integer.toString(type);
         case -85:
            return "TIMESTAMP_MILLIS " + Integer.toString(type);
         case -84:
            return "TIMESTAMP_SECONDS " + Integer.toString(type);
         case -83:
            return "TIMESTAMP_MINUTES " + Integer.toString(type);
         case -82:
            return "TIMESTAMP " + Integer.toString(type);
         case -81:
            return "NULL " + Integer.toString(type);
         case -80:
            return "FALSE " + Integer.toString(type);
         case -79:
            return "TRUE " + Integer.toString(type);
         case -78:
         case -77:
         case -76:
         case -75:
            return "DOUBLE " + Integer.toString(type);
         case -74:
         case -73:
            return "FLOAT " + Integer.toString(type);
         case -72:
         case -71:
            return "DECIMAL " + Integer.toString(type);
         case -70:
         case -69:
            return "BIGINT " + Integer.toString(type);
         case -68:
            return "INT16 " + Integer.toString(type);
         case -67:
            return "INT8 " + Integer.toString(type);
         case -66:
         case -65:
            return "INT64 " + Integer.toString(type);
         case 72:
            return "INT32 " + Integer.toString(type);
         case 122:
            return "STR_UTF8 " + Integer.toString(type);
         case 123:
            return "STR_UTF16 " + Integer.toString(type);
         case 124:
            return "STR_UTF16LE " + Integer.toString(type);
         case 125:
            return "STR_UTF16BE " + Integer.toString(type);
         case 127:
            return "SYMBOL " + Integer.toString(type);
         default:
            if (type >= -108 && type <= -92) {
               return "ARRAY " + Integer.toString(type);
            } else if (type >= 73 && type <= 121) {
               return "STR_ASCII " + Integer.toString(type);
            } else if (type >= -16 && type <= 47) {
               return "INT32 " + Integer.toString(type);
            } else if (type >= 48 && type <= 63) {
               return "INT32 " + Integer.toString(type);
            } else if (type >= 64 && type <= 71) {
               return "INT32 " + Integer.toString(type);
            } else if (type >= -40 && type <= -17) {
               return "INT64 " + Integer.toString(type);
            } else if (type >= -56 && type <= -41) {
               return "INT64 " + Integer.toString(type);
            } else {
               return type >= -64 && type <= -57 ? "INT64 " + Integer.toString(type) : Integer.toString(type);
            }
      }
   }

   public interface Constants {
      byte BC_CHAR = -112;
      byte BC_BINARY = -111;
      byte BC_TYPED_ANY = -110;
      byte BC_REFERENCE = -109;
      int ARRAY_FIX_LEN = 15;
      byte BC_ARRAY_FIX_0 = -108;
      byte BC_ARRAY_FIX_MIN = -108;
      byte BC_ARRAY_FIX_MAX = -93;
      byte BC_ARRAY = -92;
      byte BC_OBJECT_END = -91;
      byte BC_OBJECT = -90;
      byte BC_LOCAL_TIME = -89;
      byte BC_LOCAL_DATETIME = -88;
      byte BC_LOCAL_DATE = -87;
      byte BC_TIMESTAMP_WITH_TIMEZONE = -86;
      byte BC_TIMESTAMP_MILLIS = -85;
      byte BC_TIMESTAMP_SECONDS = -84;
      byte BC_TIMESTAMP_MINUTES = -83;
      byte BC_TIMESTAMP = -82;
      byte BC_NULL = -81;
      byte BC_FALSE = -80;
      byte BC_TRUE = -79;
      byte BC_DOUBLE_NUM_0 = -78;
      byte BC_DOUBLE_NUM_1 = -77;
      byte BC_DOUBLE_LONG = -76;
      byte BC_DOUBLE = -75;
      byte BC_FLOAT_INT = -74;
      byte BC_FLOAT = -73;
      byte BC_DECIMAL_LONG = -72;
      byte BC_DECIMAL = -71;
      byte BC_BIGINT_LONG = -70;
      byte BC_BIGINT = -69;
      byte BC_INT16 = -68;
      byte BC_INT8 = -67;
      byte BC_INT64 = -66;
      byte BC_INT64_INT = -65;
      int INT64_SHORT_MIN = -262144;
      int INT64_SHORT_MAX = 262143;
      int INT64_BYTE_MIN = -2048;
      int INT64_BYTE_MAX = 2047;
      byte BC_INT64_SHORT_MIN = -64;
      byte BC_INT64_SHORT_ZERO = -60;
      byte BC_INT64_SHORT_MAX = -57;
      byte BC_INT64_BYTE_MIN = -56;
      byte BC_INT64_BYTE_ZERO = -48;
      byte BC_INT64_BYTE_MAX = -41;
      byte BC_INT64_NUM_MIN = -40;
      byte BC_INT64_NUM_MAX = -17;
      int INT64_NUM_LOW_VALUE = -8;
      int INT64_NUM_HIGH_VALUE = 15;
      byte BC_INT32_NUM_0 = 0;
      byte BC_INT32_NUM_1 = 1;
      byte BC_INT32_NUM_16 = 16;
      byte BC_INT32_NUM_MIN = -16;
      byte BC_INT32_NUM_MAX = 47;
      byte BC_INT32_BYTE_MIN = 48;
      byte BC_INT32_BYTE_ZERO = 56;
      byte BC_INT32_BYTE_MAX = 63;
      byte BC_INT32_SHORT_MIN = 64;
      byte BC_INT32_SHORT_ZERO = 68;
      byte BC_INT32_SHORT_MAX = 71;
      byte BC_INT32 = 72;
      int INT32_BYTE_MIN = -2048;
      int INT32_BYTE_MAX = 2047;
      int INT32_SHORT_MIN = -262144;
      int INT32_SHORT_MAX = 262143;
      byte BC_STR_ASCII_FIX_0 = 73;
      byte BC_STR_ASCII_FIX_1 = 74;
      byte BC_STR_ASCII_FIX_4 = 77;
      byte BC_STR_ASCII_FIX_5 = 78;
      byte BC_STR_ASCII_FIX_32 = 105;
      byte BC_STR_ASCII_FIX_36 = 109;
      int STR_ASCII_FIX_LEN = 47;
      byte BC_STR_ASCII_FIX_MIN = 73;
      byte BC_STR_ASCII_FIX_MAX = 120;
      byte BC_STR_ASCII = 121;
      byte BC_STR_UTF8 = 122;
      byte BC_STR_UTF16 = 123;
      byte BC_STR_UTF16LE = 124;
      byte BC_STR_UTF16BE = 125;
      byte BC_STR_GB18030 = 126;
      byte BC_SYMBOL = 127;
   }
}
