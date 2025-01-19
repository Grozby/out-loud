package com.alibaba.fastjson2;

import com.alibaba.fastjson2.filter.AfterFilter;
import com.alibaba.fastjson2.filter.BeforeFilter;
import com.alibaba.fastjson2.filter.ContextNameFilter;
import com.alibaba.fastjson2.filter.ContextValueFilter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.LabelFilter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.modules.ObjectReaderModule;
import com.alibaba.fastjson2.modules.ObjectWriterModule;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.reader.ObjectReaderNoneDefaultConstructor;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.MapMultiValueType;
import com.alibaba.fastjson2.util.MultiType;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface JSON {
   String VERSION = "2.0.53";

   static Object parse(String text) {
      if (text != null && !text.isEmpty()) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider);
         JSONReader reader = JSONReader.of(text, context);

         JSONObject jsonObject;
         try {
            char ch = reader.current();
            Object object;
            if (context.objectSupplier == null && (context.features & JSONReader.Feature.UseNativeObject.mask) == 0L && (ch == '{' || ch == '[')) {
               if (ch == '{') {
                  jsonObject = new JSONObject();
                  reader.read(jsonObject, 0L);
                  object = jsonObject;
               } else {
                  jsonObject = new JSONArray();
                  reader.read(jsonObject);
                  object = jsonObject;
               }

               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }
            } else {
               jsonObject = provider.getObjectReader(Object.class, false);
               object = jsonObject.readObject(reader, null, null, 0L);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            jsonObject = (JSONObject)object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return jsonObject;
      } else {
         return null;
      }
   }

   static Object parse(String text, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider, features);
         ObjectReader<?> objectReader = provider.getObjectReader(Object.class, false);
         JSONReader reader = JSONReader.of(text, context);

         Object var7;
         try {
            context.config(features);
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return var7;
      } else {
         return null;
      }
   }

   static Object parse(String text, int offset, int length, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty() && length != 0) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider, features);
         ObjectReader<?> objectReader = provider.getObjectReader(Object.class, false);
         JSONReader reader = JSONReader.of(text, offset, length, context);

         Object var9;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return var9;
      } else {
         return null;
      }
   }

   static Object parse(String text, JSONReader.Context context) {
      if (text != null && !text.isEmpty()) {
         ObjectReader<?> objectReader = context.provider.getObjectReader(Object.class, false);
         JSONReader reader = JSONReader.of(text, context);

         Object var5;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = object;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static Object parse(byte[] bytes, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider, features);
         ObjectReader<?> objectReader = provider.getObjectReader(Object.class, false);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var7;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return var7;
      } else {
         return null;
      }
   }

   static Object parse(byte[] bytes, JSONReader.Context context) {
      if (bytes != null && bytes.length != 0) {
         ObjectReader<?> objectReader = context.getObjectReader(Object.class);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var5;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = object;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static Object parse(byte[] bytes, int offset, int length, Charset charset, JSONReader.Context context) {
      if (bytes != null && bytes.length != 0) {
         ObjectReader<?> objectReader = context.getObjectReader(Object.class);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         Object var8;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return var8;
      } else {
         return null;
      }
   }

   static Object parse(char[] chars, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider, features);
         ObjectReader<?> objectReader = provider.getObjectReader(Object.class, false);
         JSONReader reader = JSONReader.of(chars, context);

         Object var7;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return var7;
      } else {
         return null;
      }
   }

   static Object parse(char[] chars, JSONReader.Context context) {
      if (chars != null && chars.length != 0) {
         ObjectReader<?> objectReader = context.getObjectReader(Object.class);
         JSONReader reader = JSONReader.of(chars, context);

         Object var5;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = object;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static Object parse(InputStream in, JSONReader.Context context) {
      if (in == null) {
         return null;
      } else {
         ObjectReader<?> objectReader = context.getObjectReader(Object.class);
         JSONReaderUTF8 reader = new JSONReaderUTF8(context, in);

         Object var5;
         try {
            Object object = objectReader.readObject(reader, null, null, 0L);
            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
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
   }

   static JSONObject parseObject(String text) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(text, context);

         JSONObject object;
         label60: {
            JSONObject var4;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = object;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(String text, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, context);

         JSONObject object;
         label60: {
            JSONObject var5;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(String text, int offset, int length, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty() && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, offset, length, context);

         JSONObject object;
         label62: {
            JSONObject var7;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label62;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = object;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return var7;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(String text, int offset, int length, JSONReader.Context context) {
      if (text != null && !text.isEmpty() && length != 0) {
         JSONReader reader = JSONReader.of(text, offset, length, context);

         JSONObject object;
         label62: {
            JSONObject var6;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label62;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var6 = object;
            } catch (Throwable var8) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (reader != null) {
               reader.close();
            }

            return var6;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(String text, JSONReader.Context context) {
      if (text != null && !text.isEmpty()) {
         JSONReader reader = JSONReader.of(text, context);

         JSONObject object;
         label60: {
            JSONObject var4;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = object;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(Reader input, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(input, context);

         JSONObject object;
         label58: {
            JSONObject var5;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label58;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static JSONObject parseObject(InputStream input, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(input, StandardCharsets.UTF_8, context);

         JSONObject object;
         label58: {
            JSONObject var5;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label58;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static JSONObject parseObject(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(bytes, context);

         JSONObject object;
         label60: {
            JSONObject var4;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = object;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(char[] chars) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(chars, context);

         JSONObject object;
         label60: {
            JSONObject var4;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = object;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(InputStream in, Charset charset) {
      if (in == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(in, charset, context);

         JSONObject object;
         label58: {
            JSONObject var5;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label58;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static JSONObject parseObject(InputStream input, Charset charset, JSONReader.Context context) {
      if (input == null) {
         return null;
      } else {
         JSONReader reader = JSONReader.of(input, charset, context);

         JSONObject object;
         label58: {
            JSONObject var5;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label58;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static JSONObject parseObject(URL url) {
      if (url == null) {
         return null;
      } else {
         try {
            InputStream is = url.openStream();

            JSONObject var2;
            try {
               var2 = parseObject(is, StandardCharsets.UTF_8);
            } catch (Throwable var5) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (is != null) {
               is.close();
            }

            return var2;
         } catch (IOException var6) {
            throw new JSONException("JSON#parseObject cannot parse '" + url + "'", var6);
         }
      }
   }

   static JSONObject parseObject(byte[] bytes, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, context);

         JSONObject object;
         label60: {
            JSONObject var5;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label60;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = object;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(byte[] bytes, int offset, int length, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, offset, length, context);

         JSONObject object;
         label62: {
            JSONObject var7;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label62;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = object;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return var7;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(char[] chars, int offset, int length, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(chars, offset, length, context);

         JSONObject object;
         label62: {
            JSONObject var7;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label62;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = object;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return var7;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static JSONObject parseObject(byte[] bytes, int offset, int length, Charset charset, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         JSONObject object;
         label62: {
            JSONObject var8;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label62;
               }

               object = new JSONObject();
               reader.read(object, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var8 = object;
            } catch (Throwable var10) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (reader != null) {
               reader.close();
            }

            return var8;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Class<T> clazz) {
      if (text != null && !text.isEmpty()) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider);
         ObjectReader<T> objectReader = provider.getObjectReader(clazz, (JSONFactory.defaultReaderFeatures & JSONReader.Feature.FieldBased.mask) != 0L);
         JSONReader reader = JSONReader.of(text, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Class<T> clazz, Filter filter, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(filter, features);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         T object;
         label65: {
            Object var9;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label65;
               }

               object = objectReader.readObject(reader, clazz, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var9 = object;
            } catch (Throwable var11) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var9;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type, String format, Filter[] filters, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), null, filters, features);
         context.setDateFormat(format);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         T object;
         label65: {
            Object var10;
            try {
               if (reader.nextIfNull()) {
                  object = null;
                  break label65;
               }

               object = objectReader.readObject(reader, type, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var10 = object;
            } catch (Throwable var12) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var10;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type) {
      if (text != null && !text.isEmpty()) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider);
         ObjectReader<T> objectReader = provider.getObjectReader(type, (JSONFactory.defaultReaderFeatures & JSONReader.Feature.FieldBased.mask) != 0L);
         JSONReader reader = JSONReader.of(text, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type, JSONReader.Context context) {
      if (text != null && !text.isEmpty()) {
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(text, context);

         Object var6;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var6;
      } else {
         return null;
      }
   }

   static <T extends Map<String, Object>> T parseObject(String text, MapMultiValueType<T> type) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(text, context);

         Map var6;
         try {
            T object = (T)objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var6;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type... types) {
      return parseObject(text, new MultiType(types));
   }

   static <T> T parseObject(String text, TypeReference<T> typeReference, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         Type type = typeReference.getType();
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, TypeReference<T> typeReference, Filter filter, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(filter, features);
         Type type = typeReference.getType();
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         Object var10;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var10 = object;
         } catch (Throwable var12) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var10;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Class<T> clazz, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, int offset, int length, Class<T> clazz, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty() && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(text, offset, length, context);

         Object var10;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var10 = object;
         } catch (Throwable var12) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var10;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Class<T> clazz, JSONReader.Context context) {
      if (text != null && !text.isEmpty()) {
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Class<T> clazz, String format, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         if (format != null && !format.isEmpty()) {
            context.setDateFormat(format);
         }

         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(text, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(text, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type, Filter filter, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(filter, features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(text, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      } else {
         return null;
      }
   }

   static <T> T parseObject(String text, Type type, String format, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         if (format != null && !format.isEmpty()) {
            context.setDateFormat(format);
         }

         JSONReader reader = JSONReader.of(text, context);

         Object var8;
         try {
            ObjectReader<T> objectReader = context.getObjectReader(type);
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      } else {
         return null;
      }
   }

   static <T> T parseObject(char[] chars, int offset, int length, Type type, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(chars, offset, length, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(char[] chars, Class<T> clazz) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(clazz);
         JSONReader reader = JSONReader.of(chars, context);

         Object var6;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var6;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, int offset, int length, Type type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, offset, length, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Type type) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var6;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var6;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Class<T> clazz) {
      if (bytes != null && bytes.length != 0) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         JSONReader.Context context = new JSONReader.Context(provider);
         ObjectReader<T> objectReader = provider.getObjectReader(clazz, (JSONFactory.defaultReaderFeatures & JSONReader.Feature.FieldBased.mask) != 0L);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Class<T> clazz, Filter filter, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(filter, features);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Class<T> clazz, JSONReader.Context context) {
      if (bytes != null && bytes.length != 0) {
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(clazz, fieldBased);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Type type, String format, Filter[] filters, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = new JSONReader.Context(JSONFactory.getDefaultObjectReaderProvider(), null, filters, features);
         context.setDateFormat(format);
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var10;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var10 = object;
         } catch (Throwable var12) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var10;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Class<T> clazz, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(clazz);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, clazz, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Type type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(char[] chars, Class<T> objectClass, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(objectClass);
         JSONReader reader = JSONReader.of(chars, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, objectClass, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(char[] chars, Type type, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(chars, context);

         Object var7;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var7 = object;
         } catch (Throwable var9) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var7;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Type type, Filter filter, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(filter, features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, Type type, String format, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         if (format != null && !format.isEmpty()) {
            context.setDateFormat(format);
         }

         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      } else {
         return null;
      }
   }

   static <T> T parseObject(ByteBuffer buffer, Class<T> objectClass) {
      if (buffer == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(objectClass);
         JSONReader reader = JSONReader.of(buffer, null, context);

         Object var6;
         try {
            T object = objectReader.readObject(reader, objectClass, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = object;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var6;
      }
   }

   static <T> T parseObject(Reader input, Type type, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(input, context);

         T object;
         label58: {
            Object var7;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label58;
               }

               object = objectReader.readObject(reader, type, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = object;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var7;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static <T> T parseObject(InputStream input, Type type, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext();
         context.config(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(input, StandardCharsets.UTF_8, context);

         T object;
         label58: {
            Object var7;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label58;
               }

               object = objectReader.readObject(reader, type, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = object;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var7;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static <T> T parseObject(InputStream input, Charset charset, Type type, JSONReader.Context context) {
      if (input == null) {
         return null;
      } else {
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(input, charset, context);

         T object;
         label63: {
            Object var8;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label63;
               }

               object = objectReader.readObject(reader, type, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var8 = object;
            } catch (Throwable var10) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var8;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static <T> T parseObject(InputStream input, Charset charset, Class<T> type, JSONReader.Context context) {
      if (input == null) {
         return null;
      } else {
         boolean fieldBased = (context.features & JSONReader.Feature.FieldBased.mask) != 0L;
         ObjectReader<T> objectReader = context.provider.getObjectReader(type, fieldBased);
         JSONReader reader = JSONReader.of(input, charset, context);

         T object;
         label63: {
            Object var8;
            try {
               if (reader.isEnd()) {
                  object = null;
                  break label63;
               }

               object = objectReader.readObject(reader, type, null, 0L);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(object);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var8 = object;
            } catch (Throwable var10) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (reader != null) {
               reader.close();
            }

            return (T)var8;
         }

         if (reader != null) {
            reader.close();
         }

         return object;
      }
   }

   static <T> T parseObject(URL url, Type type, JSONReader.Feature... features) {
      if (url == null) {
         return null;
      } else {
         try {
            InputStream is = url.openStream();

            Object var4;
            try {
               var4 = parseObject(is, type, features);
            } catch (Throwable var7) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (is != null) {
               is.close();
            }

            return (T)var4;
         } catch (IOException var8) {
            throw new JSONException("parseObject error", var8);
         }
      }
   }

   static <T> T parseObject(URL url, Class<T> objectClass, JSONReader.Feature... features) {
      if (url == null) {
         return null;
      } else {
         try {
            InputStream is = url.openStream();

            Object var4;
            try {
               var4 = parseObject(is, objectClass, features);
            } catch (Throwable var7) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (is != null) {
               is.close();
            }

            return (T)var4;
         } catch (IOException var8) {
            throw new JSONException("JSON#parseObject cannot parse '" + url + "' to '" + objectClass + "'", var8);
         }
      }
   }

   static <T> T parseObject(URL url, Function<JSONObject, T> function, JSONReader.Feature... features) {
      if (url == null) {
         return null;
      } else {
         try {
            InputStream is = url.openStream();

            Object var9;
            label52: {
               try {
                  JSONObject object = parseObject(is, features);
                  if (object == null) {
                     var9 = null;
                     break label52;
                  }

                  var9 = function.apply(object);
               } catch (Throwable var7) {
                  if (is != null) {
                     try {
                        is.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (is != null) {
                  is.close();
               }

               return (T)var9;
            }

            if (is != null) {
               is.close();
            }

            return (T)var9;
         } catch (IOException var8) {
            throw new JSONException("JSON#parseObject cannot parse '" + url + "'", var8);
         }
      }
   }

   static <T> T parseObject(InputStream input, Type type, String format, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         if (format != null && !format.isEmpty()) {
            context.setDateFormat(format);
         }

         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(input, StandardCharsets.UTF_8, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      }
   }

   static <T> T parseObject(InputStream input, Charset charset, Type type, JSONReader.Feature... features) {
      if (input == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(input, charset, context);

         Object var8;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var8 = object;
         } catch (Throwable var10) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var8;
      }
   }

   static <T> T parseObject(byte[] bytes, int offset, int length, Charset charset, Type type) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, int offset, int length, Charset charset, Class<T> type) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         Object var9;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = object;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var9;
      } else {
         return null;
      }
   }

   static <T> T parseObject(byte[] bytes, int offset, int length, Charset charset, Class<T> type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         ObjectReader<T> objectReader = context.getObjectReader(type);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         Object var10;
         try {
            T object = objectReader.readObject(reader, type, null, 0L);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(object);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var10 = object;
         } catch (Throwable var12) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (reader != null) {
            reader.close();
         }

         return (T)var10;
      } else {
         return null;
      }
   }

   static <T> void parseObject(InputStream input, Type type, Consumer<T> consumer, JSONReader.Feature... features) {
      parseObject(input, StandardCharsets.UTF_8, '\n', type, consumer, features);
   }

   static <T> void parseObject(InputStream input, Charset charset, char delimiter, Type type, Consumer<T> consumer, JSONReader.Feature... features) {
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      byte[] bytes = JSONFactory.BYTES_UPDATER.getAndSet(cacheItem, null);
      int bufferSize = 524288;
      if (bytes == null) {
         bytes = new byte[bufferSize];
      }

      int offset = 0;
      int start = 0;
      ObjectReader<? extends T> objectReader = null;
      JSONReader.Context context = JSONFactory.createReadContext(features);

      try {
         while (true) {
            int n = input.read(bytes, offset, bytes.length - offset);
            if (n == -1) {
               return;
            }

            int k = offset;
            offset += n;

            boolean dispose;
            for (dispose = false; k < offset; k++) {
               if (bytes[k] == delimiter) {
                  JSONReader jsonReader = JSONReader.of(bytes, start, k - start, charset, context);
                  if (objectReader == null) {
                     objectReader = context.getObjectReader(type);
                  }

                  T object = (T)objectReader.readObject(jsonReader, type, null, 0L);
                  if (jsonReader.resolveTasks != null) {
                     jsonReader.handleResolveTasks(object);
                  }

                  if (jsonReader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                     throw new JSONException(jsonReader.info("input not end"));
                  }

                  consumer.accept(object);
                  start = k + 1;
                  dispose = true;
               }
            }

            if (offset == bytes.length) {
               if (dispose) {
                  int len = bytes.length - start;
                  System.arraycopy(bytes, start, bytes, 0, len);
                  start = 0;
                  offset = len;
               } else {
                  bytes = Arrays.copyOf(bytes, bytes.length + bufferSize);
               }
            }
         }
      } catch (IOException var23) {
         throw new JSONException("JSON#parseObject cannot parse the 'InputStream' to '" + type + "'", var23);
      } finally {
         JSONFactory.BYTES_UPDATER.lazySet(cacheItem, bytes);
      }
   }

   static <T> void parseObject(Reader input, char delimiter, Type type, Consumer<T> consumer) {
      int cacheIndex = System.identityHashCode(Thread.currentThread()) & JSONFactory.CACHE_ITEMS.length - 1;
      JSONFactory.CacheItem cacheItem = JSONFactory.CACHE_ITEMS[cacheIndex];
      char[] chars = JSONFactory.CHARS_UPDATER.getAndSet(cacheItem, null);
      if (chars == null) {
         chars = new char[8192];
      }

      int offset = 0;
      int start = 0;
      ObjectReader<? extends T> objectReader = null;
      JSONReader.Context context = JSONFactory.createReadContext();

      try {
         while (true) {
            int n = input.read(chars, offset, chars.length - offset);
            if (n == -1) {
               return;
            }

            int k = offset;
            offset += n;

            boolean dispose;
            for (dispose = false; k < offset; k++) {
               if (chars[k] == delimiter) {
                  JSONReader jsonReader = JSONReader.of(chars, start, k - start, context);
                  if (objectReader == null) {
                     objectReader = context.getObjectReader(type);
                  }

                  consumer.accept((T)objectReader.readObject(jsonReader, type, null, 0L));
                  start = k + 1;
                  dispose = true;
               }
            }

            if (offset == chars.length) {
               if (dispose) {
                  int len = chars.length - start;
                  System.arraycopy(chars, start, chars, 0, len);
                  start = 0;
                  offset = len;
               } else {
                  chars = Arrays.copyOf(chars, chars.length + 8192);
               }
            }
         }
      } catch (IOException var19) {
         throw new JSONException("JSON#parseObject cannot parse the 'Reader' to '" + type + "'", var19);
      } finally {
         JSONFactory.CHARS_UPDATER.lazySet(cacheItem, chars);
      }
   }

   static JSONArray parseArray(String text) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(text, context);

         JSONArray array;
         label60: {
            JSONArray var4;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label60;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = array;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static JSONArray parseArray(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(bytes, context);

         JSONArray array;
         label60: {
            JSONArray var4;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label60;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = array;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static JSONArray parseArray(byte[] bytes, int offset, int length, Charset charset) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         JSONArray array;
         label62: {
            JSONArray var7;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label62;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var7 = array;
            } catch (Throwable var9) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (reader != null) {
               reader.close();
            }

            return var7;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static JSONArray parseArray(char[] chars) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(chars, context);

         JSONArray array;
         label60: {
            JSONArray var4;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label60;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var4 = array;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static JSONArray parseArray(String text, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, context);

         JSONArray array;
         label60: {
            JSONArray var5;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label60;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = array;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static JSONArray parseArray(URL url, JSONReader.Feature... features) {
      if (url == null) {
         return null;
      } else {
         try {
            InputStream is = url.openStream();

            JSONArray var3;
            try {
               var3 = parseArray(is, features);
            } catch (Throwable var6) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (is != null) {
               is.close();
            }

            return var3;
         } catch (IOException var7) {
            throw new JSONException("JSON#parseArray cannot parse '" + url + "' to '" + JSONArray.class + "'", var7);
         }
      }
   }

   static JSONArray parseArray(InputStream in, JSONReader.Feature... features) {
      if (in == null) {
         return null;
      } else {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(in, StandardCharsets.UTF_8, context);

         JSONArray array;
         label58: {
            JSONArray var5;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label58;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = array;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      }
   }

   static JSONArray parseArray(InputStream in, Charset charset, JSONReader.Context context) {
      if (in == null) {
         return null;
      } else {
         JSONReader reader = JSONReader.of(in, charset, context);

         JSONArray array;
         label58: {
            JSONArray var5;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label58;
               }

               array = new JSONArray();
               reader.read((List)array);
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var5 = array;
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      }
   }

   static <T> List<T> parseArray(String text, Type type, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, context);

         List var6;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = list;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(String text, Type type) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(text, context);

         List var5;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = list;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(String text, Class<T> type) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(text, context);

         List var5;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = list;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(String text, Type... types) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext();
         JSONReader reader = JSONReader.of(text, context);

         List var5;
         try {
            List<T> list = reader.readList(types);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var5 = list;
         } catch (Throwable var7) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (reader != null) {
            reader.close();
         }

         return var5;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(String text, Class<T> type, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, context);

         List var6;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = list;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(char[] chars, Class<T> type, JSONReader.Feature... features) {
      if (chars != null && chars.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(chars, context);

         List var6;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = list;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(String text, Type[] types, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(text, context);

         List<T> array;
         label68: {
            Object var10;
            try {
               if (reader.nextIfNull()) {
                  array = null;
                  break label68;
               }

               reader.startArray();
               array = new ArrayList<>(types.length);

               for (int i = 0; i < types.length; i++) {
                  array.add(reader.read(types[i]));
               }

               reader.endArray();
               if (reader.resolveTasks != null) {
                  reader.handleResolveTasks(array);
               }

               if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
                  throw new JSONException(reader.info("input not end"));
               }

               var10 = array;
            } catch (Throwable var8) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (reader != null) {
               reader.close();
            }

            return (List<T>)var10;
         }

         if (reader != null) {
            reader.close();
         }

         return array;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] bytes, Type type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, context);

         List var6;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = list;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] bytes, Class<T> type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, context);

         List var6;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var6 = list;
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }

         return var6;
      } else {
         return null;
      }
   }

   static <T> List<T> parseArray(byte[] bytes, int offset, int length, Charset charset, Class<T> type, JSONReader.Feature... features) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         JSONReader.Context context = JSONFactory.createReadContext(features);
         JSONReader reader = JSONReader.of(bytes, offset, length, charset, context);

         List var9;
         try {
            List<T> list = reader.readArray(type);
            if (reader.resolveTasks != null) {
               reader.handleResolveTasks(list);
            }

            if (reader.ch != 26 && (context.features & JSONReader.Feature.IgnoreCheckClose.mask) == 0L) {
               throw new JSONException(reader.info("input not end"));
            }

            var9 = list;
         } catch (Throwable var11) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (reader != null) {
            reader.close();
         }

         return var9;
      } else {
         return null;
      }
   }

   static String toJSONString(Object object) {
      ObjectWriterProvider provider = JSONFactory.defaultObjectWriterProvider;
      JSONWriter.Context context = new JSONWriter.Context(provider);

      try {
         JSONWriter writer = JSONWriter.of(context);

         String var9;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               if (valueClass == JSONObject.class && context.features == 0L) {
                  writer.write((JSONObject)object);
               } else {
                  ObjectWriter<?> objectWriter = provider.getObjectWriter(
                     valueClass, valueClass, (JSONFactory.defaultWriterFeatures & JSONWriter.Feature.FieldBased.mask) != 0L
                  );
                  objectWriter.write(writer, object, null, null, 0L);
               }
            }

            var9 = writer.toString();
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }

         return var9;
      } catch (NumberFormatException | NullPointerException var8) {
         throw new JSONException("JSON#toJSONString cannot serialize '" + object + "'", var8);
      }
   }

   static String toJSONString(Object object, JSONWriter.Context context) {
      if (context == null) {
         context = JSONFactory.createWriteContext();
      }

      try {
         JSONWriter writer = JSONWriter.of(context);

         String var8;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var8 = writer.toString();
         } catch (Throwable var6) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (writer != null) {
            writer.close();
         }

         return var8;
      } catch (NumberFormatException | NullPointerException var7) {
         throw new JSONException("JSON#toJSONString cannot serialize '" + object + "'", var7);
      }
   }

   static String toJSONString(Object object, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      JSONWriter writer = JSONWriter.of(context);

      String var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            boolean fieldBased = (context.features & JSONWriter.Feature.FieldBased.mask) != 0L;
            ObjectWriter<?> objectWriter = context.provider.getObjectWriter(valueClass, valueClass, fieldBased);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.toString();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static String toJSONString(Object object, Filter filter, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      JSONWriter writer = JSONWriter.of(context);

      String var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            if (filter != null) {
               writer.context.configFilter(filter);
            }

            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.toString();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static String toJSONString(Object object, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      JSONWriter writer = JSONWriter.of(context);

      String var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.toString();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static String toJSONString(Object object, String format, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (format != null && !format.isEmpty()) {
         context.setDateFormat(format);
      }

      JSONWriter writer = JSONWriter.of(context);

      String var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.toString();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static String toJSONString(Object object, String format, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (format != null && !format.isEmpty()) {
         context.setDateFormat(format);
      }

      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      JSONWriter writer = JSONWriter.of(context);

      String var10;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var10 = writer.toString();
      } catch (Throwable var9) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (writer != null) {
         writer.close();
      }

      return var10;
   }

   static byte[] toJSONBytes(Object object) {
      ObjectWriterProvider provider = JSONFactory.defaultObjectWriterProvider;
      JSONWriter.Context context = new JSONWriter.Context(provider);
      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            if (valueClass == JSONObject.class && writer.context.features == 0L) {
               writer.write((JSONObject)object);
            } else {
               ObjectWriter<?> objectWriter = provider.getObjectWriter(
                  valueClass, valueClass, (JSONFactory.defaultWriterFeatures & JSONWriter.Feature.FieldBased.mask) != 0L
               );
               objectWriter.write(writer, object, null, null, 0L);
            }
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (writer != null) {
         writer.close();
      }

      return var8;
   }

   static byte[] toJSONBytes(Object object, Charset charset, JSONWriter.Feature... features) {
      ObjectWriterProvider provider = JSONFactory.defaultObjectWriterProvider;
      JSONWriter.Context context = new JSONWriter.Context(provider, features);
      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var10;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            if (valueClass == JSONObject.class && writer.context.features == 0L) {
               writer.write((JSONObject)object);
            } else {
               ObjectWriter<?> objectWriter = provider.getObjectWriter(
                  valueClass, valueClass, (JSONFactory.defaultWriterFeatures & JSONWriter.Feature.FieldBased.mask) != 0L
               );
               objectWriter.write(writer, object, null, null, 0L);
            }
         }

         var10 = writer.getBytes(charset);
      } catch (Throwable var9) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (writer != null) {
         writer.close();
      }

      return var10;
   }

   static byte[] toJSONBytes(Object object, Charset charset, JSONWriter.Context context) {
      ObjectWriterProvider provider = context.provider;
      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            if (valueClass == JSONObject.class && writer.context.features == 0L) {
               writer.write((JSONObject)object);
            } else {
               ObjectWriter<?> objectWriter = provider.getObjectWriter(
                  valueClass, valueClass, (JSONFactory.defaultWriterFeatures & JSONWriter.Feature.FieldBased.mask) != 0L
               );
               objectWriter.write(writer, object, null, null, 0L);
            }
         }

         var9 = writer.getBytes(charset);
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static byte[] toJSONBytes(Object object, String format, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (format != null && !format.isEmpty()) {
         context.setDateFormat(format);
      }

      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.getBytes();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static byte[] toJSONBytes(Object object, Filter... filters) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider);
      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (writer != null) {
         writer.close();
      }

      return var8;
   }

   static byte[] toJSONBytes(Object object, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var8;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var8 = writer.getBytes();
      } catch (Throwable var7) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (writer != null) {
         writer.close();
      }

      return var8;
   }

   static byte[] toJSONBytes(Object object, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var9;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var9 = writer.getBytes();
      } catch (Throwable var8) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (writer != null) {
         writer.close();
      }

      return var9;
   }

   static byte[] toJSONBytes(Object object, String format, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (format != null && !format.isEmpty()) {
         context.setDateFormat(format);
      }

      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      JSONWriter writer = JSONWriter.ofUTF8(context);

      byte[] var10;
      try {
         if (object == null) {
            writer.writeNull();
         } else {
            writer.rootObject = object;
            writer.path = JSONWriter.Path.ROOT;
            Class<?> valueClass = object.getClass();
            ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
            objectWriter.write(writer, object, null, null, 0L);
         }

         var10 = writer.getBytes();
      } catch (Throwable var9) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (writer != null) {
         writer.close();
      }

      return var10;
   }

   static int writeTo(OutputStream out, Object object) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider);

      try {
         JSONWriter writer = JSONWriter.ofUTF8(context);

         int var9;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var9 = writer.flushTo(out);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }

         return var9;
      } catch (Exception var8) {
         throw new JSONException(var8.getMessage(), var8);
      }
   }

   static int writeTo(OutputStream out, Object object, JSONWriter.Context context) {
      try {
         JSONWriter writer = JSONWriter.ofUTF8(context);

         int var9;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var9 = writer.flushTo(out);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }

         return var9;
      } catch (Exception var8) {
         throw new JSONException(var8.getMessage(), var8);
      }
   }

   static int writeTo(OutputStream out, Object object, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);

      try {
         JSONWriter writer = JSONWriter.ofUTF8(context);

         int var10;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var10 = writer.flushTo(out);
         } catch (Throwable var8) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (writer != null) {
            writer.close();
         }

         return var10;
      } catch (Exception var9) {
         throw new JSONException(var9.getMessage(), var9);
      }
   }

   static int writeTo(OutputStream out, Object object, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      try {
         JSONWriter writer = JSONWriter.ofUTF8(context);

         int var11;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var11 = writer.flushTo(out);
         } catch (Throwable var9) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (writer != null) {
            writer.close();
         }

         return var11;
      } catch (Exception var10) {
         throw new JSONException("JSON#writeTo cannot serialize '" + object + "' to 'OutputStream'", var10);
      }
   }

   static int writeTo(OutputStream out, Object object, String format, Filter[] filters, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(JSONFactory.defaultObjectWriterProvider, features);
      if (format != null && !format.isEmpty()) {
         context.setDateFormat(format);
      }

      if (filters != null && filters.length != 0) {
         context.configFilter(filters);
      }

      try {
         JSONWriter writer = JSONWriter.ofUTF8(context);

         int var12;
         try {
            if (object == null) {
               writer.writeNull();
            } else {
               writer.rootObject = object;
               writer.path = JSONWriter.Path.ROOT;
               Class<?> valueClass = object.getClass();
               ObjectWriter<?> objectWriter = context.getObjectWriter(valueClass, valueClass);
               objectWriter.write(writer, object, null, null, 0L);
            }

            var12 = writer.flushTo(out);
         } catch (Throwable var10) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (writer != null) {
            writer.close();
         }

         return var12;
      } catch (Exception var11) {
         throw new JSONException("JSON#writeTo cannot serialize '" + object + "' to 'OutputStream'", var11);
      }
   }

   static boolean isValid(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            JSONReader jsonReader = JSONReader.of(text);

            boolean var2;
            try {
               jsonReader.skipValue();
               var2 = jsonReader.isEnd() && !jsonReader.comma;
            } catch (Throwable var5) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var2;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValid(String text, JSONReader.Feature... features) {
      if (text != null && !text.isEmpty()) {
         try {
            JSONReader jsonReader = JSONReader.of(text, JSONFactory.createReadContext(features));

            boolean var3;
            try {
               jsonReader.skipValue();
               var3 = jsonReader.isEnd() && !jsonReader.comma;
            } catch (Throwable var6) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var3;
         } catch (ArrayIndexOutOfBoundsException | JSONException var7) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValid(char[] chars) {
      if (chars != null && chars.length != 0) {
         try {
            JSONReader jsonReader = JSONReader.of(chars);

            boolean var2;
            try {
               jsonReader.skipValue();
               var2 = jsonReader.isEnd() && !jsonReader.comma;
            } catch (Throwable var5) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var2;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValidObject(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            JSONReader jsonReader = JSONReader.of(text);

            boolean var7;
            label61: {
               try {
                  if (!jsonReader.isObject()) {
                     var7 = false;
                     break label61;
                  }

                  jsonReader.skipValue();
                  var7 = jsonReader.isEnd() && !jsonReader.comma;
               } catch (Throwable var5) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return var7;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var7;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValidObject(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         try {
            JSONReader jsonReader = JSONReader.of(bytes);

            boolean var7;
            label61: {
               try {
                  if (!jsonReader.isObject()) {
                     var7 = false;
                     break label61;
                  }

                  jsonReader.skipValue();
                  var7 = jsonReader.isEnd() && !jsonReader.comma;
               } catch (Throwable var5) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return var7;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var7;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValidArray(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            JSONReader jsonReader = JSONReader.of(text);

            boolean var7;
            label61: {
               try {
                  if (!jsonReader.isArray()) {
                     var7 = false;
                     break label61;
                  }

                  jsonReader.skipValue();
                  var7 = jsonReader.isEnd() && !jsonReader.comma;
               } catch (Throwable var5) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return var7;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var7;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValid(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         try {
            JSONReader jsonReader = JSONReader.of(bytes);

            boolean var2;
            try {
               jsonReader.skipValue();
               var2 = jsonReader.isEnd() && !jsonReader.comma;
            } catch (Throwable var5) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var2;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValid(byte[] bytes, Charset charset) {
      return bytes != null && bytes.length != 0 ? isValid(bytes, 0, bytes.length, charset) : false;
   }

   static boolean isValidArray(byte[] bytes) {
      if (bytes != null && bytes.length != 0) {
         try {
            JSONReader jsonReader = JSONReader.of(bytes);

            boolean var7;
            label61: {
               try {
                  if (!jsonReader.isArray()) {
                     var7 = false;
                     break label61;
                  }

                  jsonReader.skipValue();
                  var7 = jsonReader.isEnd() && !jsonReader.comma;
               } catch (Throwable var5) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                     }
                  }

                  throw var5;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return var7;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var7;
         } catch (ArrayIndexOutOfBoundsException | JSONException var6) {
            return false;
         }
      } else {
         return false;
      }
   }

   static boolean isValid(byte[] bytes, int offset, int length, Charset charset) {
      if (bytes != null && bytes.length != 0 && length != 0) {
         try {
            JSONReader jsonReader = JSONReader.of(bytes, offset, length, charset);

            boolean var5;
            try {
               jsonReader.skipValue();
               var5 = jsonReader.isEnd() && !jsonReader.comma;
            } catch (Throwable var8) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return var5;
         } catch (ArrayIndexOutOfBoundsException | JSONException var9) {
            return false;
         }
      } else {
         return false;
      }
   }

   static Object toJSON(Object object) {
      return toJSON(object, (JSONWriter.Feature[])null);
   }

   static Object toJSON(Object object, JSONWriter.Feature... features) {
      if (object == null) {
         return null;
      } else if (!(object instanceof JSONObject) && !(object instanceof JSONArray)) {
         JSONWriter.Context writeContext = features == null ? JSONFactory.createWriteContext() : JSONFactory.createWriteContext(features);
         Class<?> valueClass = object.getClass();
         ObjectWriter<?> objectWriter = writeContext.getObjectWriter(valueClass, valueClass);
         if (objectWriter instanceof ObjectWriterAdapter && !writeContext.isEnabled(JSONWriter.Feature.ReferenceDetection)) {
            ObjectWriterAdapter objectWriterAdapter = (ObjectWriterAdapter)objectWriter;
            return objectWriterAdapter.toJSONObject(object, writeContext.features);
         } else {
            String str;
            try {
               JSONWriter writer = JSONWriter.of(writeContext);

               try {
                  objectWriter.write(writer, object, null, null, writeContext.features);
                  str = writer.toString();
               } catch (Throwable var10) {
                  if (writer != null) {
                     try {
                        writer.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (writer != null) {
                  writer.close();
               }
            } catch (NumberFormatException | NullPointerException var11) {
               throw new JSONException("toJSONString error", var11);
            }

            return parse(str);
         }
      } else {
         return object;
      }
   }

   static <T> T to(Class<T> clazz, Object object) {
      if (object == null) {
         return null;
      } else {
         return object instanceof JSONObject ? ((JSONObject)object).to(clazz) : TypeUtils.cast(object, clazz, JSONFactory.getDefaultObjectReaderProvider());
      }
   }

   /** @deprecated */
   static <T> T toJavaObject(Object object, Class<T> clazz) {
      return to(clazz, object);
   }

   static void mixIn(Class<?> target, Class<?> mixinSource) {
      JSONFactory.defaultObjectWriterProvider.mixIn(target, mixinSource);
      JSONFactory.getDefaultObjectReaderProvider().mixIn(target, mixinSource);
   }

   static ObjectReader<?> register(Type type, ObjectReader<?> objectReader) {
      return JSONFactory.getDefaultObjectReaderProvider().register(type, objectReader);
   }

   static ObjectReader<?> register(Type type, ObjectReader<?> objectReader, boolean fieldBased) {
      return JSONFactory.getDefaultObjectReaderProvider().register(type, objectReader, fieldBased);
   }

   static ObjectReader<?> registerIfAbsent(Type type, ObjectReader<?> objectReader) {
      return JSONFactory.getDefaultObjectReaderProvider().registerIfAbsent(type, objectReader);
   }

   static ObjectReader<?> registerIfAbsent(Type type, ObjectReader<?> objectReader, boolean fieldBased) {
      return JSONFactory.getDefaultObjectReaderProvider().registerIfAbsent(type, objectReader, fieldBased);
   }

   static boolean register(ObjectReaderModule objectReaderModule) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      return provider.register(objectReaderModule);
   }

   static void registerSeeAlsoSubType(Class subTypeClass) {
      registerSeeAlsoSubType(subTypeClass, null);
   }

   static void registerSeeAlsoSubType(Class subTypeClass, String subTypeClassName) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      provider.registerSeeAlsoSubType(subTypeClass, subTypeClassName);
   }

   static boolean register(ObjectWriterModule objectWriterModule) {
      return JSONFactory.getDefaultObjectWriterProvider().register(objectWriterModule);
   }

   static ObjectWriter<?> register(Type type, ObjectWriter<?> objectWriter) {
      return JSONFactory.getDefaultObjectWriterProvider().register(type, objectWriter);
   }

   static ObjectWriter<?> register(Type type, ObjectWriter<?> objectWriter, boolean fieldBased) {
      return JSONFactory.getDefaultObjectWriterProvider().register(type, objectWriter, fieldBased);
   }

   static ObjectWriter<?> registerIfAbsent(Type type, ObjectWriter<?> objectWriter) {
      return JSONFactory.getDefaultObjectWriterProvider().registerIfAbsent(type, objectWriter);
   }

   static ObjectWriter<?> registerIfAbsent(Type type, ObjectWriter<?> objectWriter, boolean fieldBased) {
      return JSONFactory.getDefaultObjectWriterProvider().registerIfAbsent(type, objectWriter, fieldBased);
   }

   static void register(Class type, Filter filter) {
      boolean writerFilter = filter instanceof AfterFilter
         || filter instanceof BeforeFilter
         || filter instanceof ContextNameFilter
         || filter instanceof ContextValueFilter
         || filter instanceof LabelFilter
         || filter instanceof NameFilter
         || filter instanceof PropertyFilter
         || filter instanceof PropertyPreFilter
         || filter instanceof ValueFilter;
      if (writerFilter) {
         ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(type);
         objectWriter.setFilter(filter);
      }
   }

   static void config(JSONReader.Feature... features) {
      for (int i = 0; i < features.length; i++) {
         JSONReader.Feature feature = features[i];
         if (feature == JSONReader.Feature.SupportAutoType) {
            throw new JSONException("not support config global autotype support");
         }

         JSONFactory.defaultReaderFeatures = JSONFactory.defaultReaderFeatures | feature.mask;
      }
   }

   static void config(JSONReader.Feature feature, boolean state) {
      if (feature == JSONReader.Feature.SupportAutoType && state) {
         throw new JSONException("not support config global autotype support");
      } else {
         if (state) {
            JSONFactory.defaultReaderFeatures = JSONFactory.defaultReaderFeatures | feature.mask;
         } else {
            JSONFactory.defaultReaderFeatures = JSONFactory.defaultReaderFeatures & ~feature.mask;
         }
      }
   }

   static boolean isEnabled(JSONReader.Feature feature) {
      return (JSONFactory.defaultReaderFeatures & feature.mask) != 0L;
   }

   static void configReaderDateFormat(String dateFormat) {
      JSONFactory.defaultReaderFormat = dateFormat;
   }

   static void configWriterDateFormat(String dateFormat) {
      JSONFactory.defaultWriterFormat = dateFormat;
   }

   static void configReaderZoneId(ZoneId zoneId) {
      JSONFactory.defaultReaderZoneId = zoneId;
   }

   static void configWriterZoneId(ZoneId zoneId) {
      JSONFactory.defaultWriterZoneId = zoneId;
   }

   static void config(JSONWriter.Feature... features) {
      for (int i = 0; i < features.length; i++) {
         JSONFactory.defaultWriterFeatures = JSONFactory.defaultWriterFeatures | features[i].mask;
      }
   }

   static void config(JSONWriter.Feature feature, boolean state) {
      if (state) {
         JSONFactory.defaultWriterFeatures = JSONFactory.defaultWriterFeatures | feature.mask;
      } else {
         JSONFactory.defaultWriterFeatures = JSONFactory.defaultWriterFeatures & ~feature.mask;
      }
   }

   static boolean isEnabled(JSONWriter.Feature feature) {
      return (JSONFactory.defaultWriterFeatures & feature.mask) != 0L;
   }

   static <T> T copy(T object, JSONWriter.Feature... features) {
      if (object == null) {
         return null;
      } else {
         Class<?> objectClass = object.getClass();
         if (ObjectWriterProvider.isPrimitiveOrEnum(objectClass)) {
            return object;
         } else {
            boolean fieldBased = false;
            boolean beanToArray = false;
            long featuresValue = JSONFactory.defaultReaderFeatures;

            for (int i = 0; i < features.length; i++) {
               JSONWriter.Feature feature = features[i];
               featuresValue |= feature.mask;
               if (feature == JSONWriter.Feature.FieldBased) {
                  fieldBased = true;
               } else if (feature == JSONWriter.Feature.BeanToArray) {
                  beanToArray = true;
               }
            }

            ObjectWriter objectWriter = JSONFactory.defaultObjectWriterProvider.getObjectWriter(objectClass, objectClass, fieldBased);
            ObjectReader objectReader = JSONFactory.defaultObjectReaderProvider.getObjectReader(objectClass, fieldBased);
            if (objectWriter instanceof ObjectWriterAdapter && objectReader instanceof ObjectReaderBean) {
               List<FieldWriter> fieldWriters = objectWriter.getFieldWriters();
               int size = fieldWriters.size();
               if (!(objectReader instanceof ObjectReaderNoneDefaultConstructor)) {
                  T instance = (T)objectReader.createInstance(featuresValue);

                  for (int ix = 0; ix < size; ix++) {
                     FieldWriter fieldWriter = fieldWriters.get(ix);
                     FieldReader fieldReader = objectReader.getFieldReader(fieldWriter.fieldName);
                     if (fieldReader != null) {
                        Object fieldValue = fieldWriter.getFieldValue(object);
                        Object fieldValueCopied = copy(fieldValue);
                        fieldReader.accept(instance, fieldValueCopied);
                     }
                  }

                  return instance;
               } else {
                  Map<String, Object> map = new HashMap<>(size, 1.0F);

                  for (int ixx = 0; ixx < size; ixx++) {
                     FieldWriter fieldWriter = fieldWriters.get(ixx);
                     Object fieldValue = fieldWriter.getFieldValue(object);
                     map.put(fieldWriter.fieldName, fieldValue);
                  }

                  return (T)objectReader.createInstance(map, featuresValue);
               }
            } else {
               JSONWriter writer = JSONWriter.ofJSONB(features);

               byte[] jsonbBytes;
               try {
                  writer.config(JSONWriter.Feature.WriteClassName);
                  objectWriter.writeJSONB(writer, object, null, null, 0L);
                  jsonbBytes = writer.getBytes();
               } catch (Throwable var19) {
                  if (writer != null) {
                     try {
                        writer.close();
                     } catch (Throwable var18) {
                        var19.addSuppressed(var18);
                     }
                  }

                  throw var19;
               }

               if (writer != null) {
                  writer.close();
               }

               JSONReader jsonReader = JSONReader.ofJSONB(jsonbBytes, JSONReader.Feature.SupportAutoType, JSONReader.Feature.SupportClassForName);

               Object instance;
               try {
                  if (beanToArray) {
                     jsonReader.context.config(JSONReader.Feature.SupportArrayToBean);
                  }

                  instance = objectReader.readJSONBObject(jsonReader, null, null, featuresValue);
               } catch (Throwable var20) {
                  if (jsonReader != null) {
                     try {
                        jsonReader.close();
                     } catch (Throwable var17) {
                        var20.addSuppressed(var17);
                     }
                  }

                  throw var20;
               }

               if (jsonReader != null) {
                  jsonReader.close();
               }

               return (T)instance;
            }
         }
      }
   }

   static <T> T copyTo(Object object, Class<T> targetClass, JSONWriter.Feature... features) {
      if (object == null) {
         return null;
      } else {
         Class<?> objectClass = object.getClass();
         boolean fieldBased = false;
         boolean beanToArray = false;
         long featuresValue = JSONFactory.defaultReaderFeatures;

         for (int i = 0; i < features.length; i++) {
            JSONWriter.Feature feature = features[i];
            featuresValue |= feature.mask;
            if (feature == JSONWriter.Feature.FieldBased) {
               fieldBased = true;
            } else if (feature == JSONWriter.Feature.BeanToArray) {
               beanToArray = true;
            }
         }

         ObjectWriter objectWriter = JSONFactory.defaultObjectWriterProvider.getObjectWriter(objectClass, objectClass, fieldBased);
         ObjectReader objectReader = JSONFactory.defaultObjectReaderProvider.getObjectReader(targetClass, fieldBased);
         if (objectWriter instanceof ObjectWriterAdapter && objectReader instanceof ObjectReaderBean) {
            List<FieldWriter> fieldWriters = objectWriter.getFieldWriters();
            if (objectReader instanceof ObjectReaderNoneDefaultConstructor) {
               Map<String, Object> map = new HashMap<>(fieldWriters.size(), 1.0F);

               for (int ix = 0; ix < fieldWriters.size(); ix++) {
                  FieldWriter fieldWriter = fieldWriters.get(ix);
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  map.put(fieldWriter.fieldName, fieldValue);
               }

               return (T)objectReader.createInstance(map, featuresValue);
            } else {
               T instance = (T)objectReader.createInstance(featuresValue);

               for (int ix = 0; ix < fieldWriters.size(); ix++) {
                  FieldWriter fieldWriter = fieldWriters.get(ix);
                  FieldReader fieldReader = objectReader.getFieldReader(fieldWriter.fieldName);
                  if (fieldReader != null) {
                     Object fieldValue = fieldWriter.getFieldValue(object);
                     Object fieldValueCopied;
                     if (fieldWriter.fieldClass == Date.class && fieldReader.fieldClass == String.class) {
                        fieldValueCopied = DateUtils.format((Date)fieldValue, fieldWriter.format);
                     } else if (fieldWriter.fieldClass == LocalDate.class && fieldReader.fieldClass == String.class) {
                        fieldValueCopied = DateUtils.format((LocalDate)fieldValue, fieldWriter.format);
                     } else if (fieldValue != null && !fieldReader.supportAcceptType(fieldValue.getClass())) {
                        fieldValueCopied = copy(fieldValue);
                     } else {
                        fieldValueCopied = fieldValue;
                     }

                     fieldReader.accept(instance, fieldValueCopied);
                  }
               }

               return instance;
            }
         } else {
            JSONWriter writer = JSONWriter.ofJSONB(features);

            byte[] jsonbBytes;
            try {
               writer.config(JSONWriter.Feature.WriteClassName);
               objectWriter.writeJSONB(writer, object, null, null, 0L);
               jsonbBytes = writer.getBytes();
            } catch (Throwable var19) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var18) {
                     var19.addSuppressed(var18);
                  }
               }

               throw var19;
            }

            if (writer != null) {
               writer.close();
            }

            JSONReader jsonReader = JSONReader.ofJSONB(jsonbBytes, JSONReader.Feature.SupportAutoType, JSONReader.Feature.SupportClassForName);

            Object ixx;
            try {
               if (beanToArray) {
                  jsonReader.context.config(JSONReader.Feature.SupportArrayToBean);
               }

               ixx = objectReader.readJSONBObject(jsonReader, null, null, 0L);
            } catch (Throwable var20) {
               if (jsonReader != null) {
                  try {
                     jsonReader.close();
                  } catch (Throwable var17) {
                     var20.addSuppressed(var17);
                  }
               }

               throw var20;
            }

            if (jsonReader != null) {
               jsonReader.close();
            }

            return (T)ixx;
         }
      }
   }
}
