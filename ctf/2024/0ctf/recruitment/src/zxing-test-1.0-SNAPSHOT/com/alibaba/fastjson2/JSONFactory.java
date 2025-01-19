package com.alibaba.fastjson2;

import com.alibaba.fastjson2.filter.ExtraProcessor;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderCreator;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterCreator;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

public final class JSONFactory {
   static volatile Throwable initErrorLast;
   public static final String CREATOR;
   public static final String PROPERTY_DENY_PROPERTY = "fastjson2.parser.deny";
   public static final String PROPERTY_AUTO_TYPE_ACCEPT = "fastjson2.autoTypeAccept";
   public static final String PROPERTY_AUTO_TYPE_HANDLER = "fastjson2.autoTypeHandler";
   public static final String PROPERTY_AUTO_TYPE_BEFORE_HANDLER = "fastjson2.autoTypeBeforeHandler";
   static boolean useJacksonAnnotation;
   static boolean useGsonAnnotation;
   static long defaultReaderFeatures;
   static String defaultReaderFormat;
   static ZoneId defaultReaderZoneId;
   static long defaultWriterFeatures;
   static String defaultWriterFormat;
   static ZoneId defaultWriterZoneId;
   static boolean defaultWriterAlphabetic;
   static final boolean disableReferenceDetect;
   static final boolean disableArrayMapping;
   static final boolean disableJSONB;
   static final boolean disableAutoType;
   static final boolean disableSmartMatch;
   static Supplier<Map> defaultObjectSupplier;
   static Supplier<List> defaultArraySupplier;
   static final JSONFactory.NameCacheEntry[] NAME_CACHE = new JSONFactory.NameCacheEntry[8192];
   static final JSONFactory.NameCacheEntry2[] NAME_CACHE2 = new JSONFactory.NameCacheEntry2[8192];
   static final Function<JSONWriter.Context, JSONWriter> INCUBATOR_VECTOR_WRITER_CREATOR_UTF8;
   static final Function<JSONWriter.Context, JSONWriter> INCUBATOR_VECTOR_WRITER_CREATOR_UTF16;
   static final JSONFactory.JSONReaderUTF8Creator INCUBATOR_VECTOR_READER_CREATOR_ASCII;
   static final JSONFactory.JSONReaderUTF8Creator INCUBATOR_VECTOR_READER_CREATOR_UTF8;
   static final JSONFactory.JSONReaderUTF16Creator INCUBATOR_VECTOR_READER_CREATOR_UTF16;
   static int defaultDecimalMaxScale = 2048;
   static final char[] CA = new char[]{
      'A',
      'B',
      'C',
      'D',
      'E',
      'F',
      'G',
      'H',
      'I',
      'J',
      'K',
      'L',
      'M',
      'N',
      'O',
      'P',
      'Q',
      'R',
      'S',
      'T',
      'U',
      'V',
      'W',
      'X',
      'Y',
      'Z',
      'a',
      'b',
      'c',
      'd',
      'e',
      'f',
      'g',
      'h',
      'i',
      'j',
      'k',
      'l',
      'm',
      'n',
      'o',
      'p',
      'q',
      'r',
      's',
      't',
      'u',
      'v',
      'w',
      'x',
      'y',
      'z',
      '0',
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
      '7',
      '8',
      '9',
      '+',
      '/'
   };
   static final int[] DIGITS2 = new int[]{
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      10,
      11,
      12,
      13,
      14,
      15,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      10,
      11,
      12,
      13,
      14,
      15
   };
   static final float[] FLOAT_10_POW = new float[]{1.0F, 10.0F, 100.0F, 1000.0F, 10000.0F, 100000.0F, 1000000.0F, 1.0E7F, 1.0E8F, 1.0E9F, 1.0E10F};
   static final double[] DOUBLE_10_POW = new double[]{
      1.0,
      10.0,
      100.0,
      1000.0,
      10000.0,
      100000.0,
      1000000.0,
      1.0E7,
      1.0E8,
      1.0E9,
      1.0E10,
      1.0E11,
      1.0E12,
      1.0E13,
      1.0E14,
      1.0E15,
      1.0E16,
      1.0E17,
      1.0E18,
      1.0E19,
      1.0E20,
      1.0E21,
      1.0E22
   };
   static final Double DOUBLE_ZERO = 0.0;
   static final JSONFactory.CacheItem[] CACHE_ITEMS;
   static final int CACHE_THRESHOLD = 4194304;
   static final AtomicReferenceFieldUpdater<JSONFactory.CacheItem, char[]> CHARS_UPDATER;
   static final AtomicReferenceFieldUpdater<JSONFactory.CacheItem, byte[]> BYTES_UPDATER;
   static final Properties DEFAULT_PROPERTIES;
   static final ObjectWriterProvider defaultObjectWriterProvider;
   static final ObjectReaderProvider defaultObjectReaderProvider;
   static final JSONFactory.JSONPathCompiler defaultJSONPathCompiler;
   static final ThreadLocal<ObjectReaderCreator> readerCreatorLocal;
   static final ThreadLocal<ObjectReaderProvider> readerProviderLocal;
   static final ThreadLocal<ObjectWriterCreator> writerCreatorLocal;
   static final ThreadLocal<JSONFactory.JSONPathCompiler> jsonPathCompilerLocal;
   static final ObjectReader<JSONArray> ARRAY_READER;
   static final ObjectReader<JSONObject> OBJECT_READER;
   static final byte[] UUID_VALUES;

   public static String getProperty(String key) {
      return DEFAULT_PROPERTIES.getProperty(key);
   }

   private static boolean getPropertyBool(Properties properties, String name, boolean defaultValue) {
      boolean propertyValue = defaultValue;
      String property = System.getProperty(name);
      if (property != null) {
         property = property.trim();
         if (property.isEmpty()) {
            property = properties.getProperty(name);
            if (property != null) {
               property = property.trim();
            }
         }

         if (defaultValue) {
            if ("false".equals(property)) {
               propertyValue = false;
            }
         } else if ("true".equals(property)) {
            propertyValue = true;
         }
      }

      return propertyValue;
   }

   private static String getProperty(Properties properties, String name) {
      String property = System.getProperty(name);
      if (property != null) {
         property = property.trim();
         if (property.isEmpty()) {
            property = properties.getProperty(name);
            if (property != null) {
               property = property.trim();
            }
         }
      }

      return property;
   }

   public static boolean isUseJacksonAnnotation() {
      return useJacksonAnnotation;
   }

   public static boolean isUseGsonAnnotation() {
      return useGsonAnnotation;
   }

   public static void setUseJacksonAnnotation(boolean useJacksonAnnotation) {
      JSONFactory.useJacksonAnnotation = useJacksonAnnotation;
   }

   public static void setDefaultObjectSupplier(Supplier<Map> objectSupplier) {
      defaultObjectSupplier = objectSupplier;
   }

   public static void setDefaultArraySupplier(Supplier<List> arraySupplier) {
      defaultArraySupplier = arraySupplier;
   }

   public static Supplier<Map> getDefaultObjectSupplier() {
      return defaultObjectSupplier;
   }

   public static Supplier<List> getDefaultArraySupplier() {
      return defaultArraySupplier;
   }

   public static JSONWriter.Context createWriteContext() {
      return new JSONWriter.Context(defaultObjectWriterProvider);
   }

   public static JSONWriter.Context createWriteContext(ObjectWriterProvider provider, JSONWriter.Feature... features) {
      JSONWriter.Context context = new JSONWriter.Context(provider);
      context.config(features);
      return context;
   }

   public static JSONWriter.Context createWriteContext(JSONWriter.Feature... features) {
      return new JSONWriter.Context(defaultObjectWriterProvider, features);
   }

   public static JSONReader.Context createReadContext() {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      return new JSONReader.Context(provider);
   }

   public static JSONReader.Context createReadContext(long features) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      return new JSONReader.Context(provider, features);
   }

   public static JSONReader.Context createReadContext(JSONReader.Feature... features) {
      JSONReader.Context context = new JSONReader.Context(getDefaultObjectReaderProvider());

      for (int i = 0; i < features.length; i++) {
         context.features = context.features | features[i].mask;
      }

      return context;
   }

   public static JSONReader.Context createReadContext(Filter filter, JSONReader.Feature... features) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider);
      if (filter instanceof JSONReader.AutoTypeBeforeHandler) {
         context.autoTypeBeforeHandler = (JSONReader.AutoTypeBeforeHandler)filter;
      }

      if (filter instanceof ExtraProcessor) {
         context.extraProcessor = (ExtraProcessor)filter;
      }

      for (int i = 0; i < features.length; i++) {
         context.features = context.features | features[i].mask;
      }

      return context;
   }

   public static JSONReader.Context createReadContext(ObjectReaderProvider provider, JSONReader.Feature... features) {
      if (provider == null) {
         provider = getDefaultObjectReaderProvider();
      }

      JSONReader.Context context = new JSONReader.Context(provider);
      context.config(features);
      return context;
   }

   public static JSONReader.Context createReadContext(SymbolTable symbolTable) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      return new JSONReader.Context(provider, symbolTable);
   }

   public static JSONReader.Context createReadContext(SymbolTable symbolTable, JSONReader.Feature... features) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider, symbolTable);
      context.config(features);
      return context;
   }

   public static JSONReader.Context createReadContext(Supplier<Map> objectSupplier, JSONReader.Feature... features) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider);
      context.setObjectSupplier(objectSupplier);
      context.config(features);
      return context;
   }

   public static JSONReader.Context createReadContext(Supplier<Map> objectSupplier, Supplier<List> arraySupplier, JSONReader.Feature... features) {
      ObjectReaderProvider provider = getDefaultObjectReaderProvider();
      JSONReader.Context context = new JSONReader.Context(provider);
      context.setObjectSupplier(objectSupplier);
      context.setArraySupplier(arraySupplier);
      context.config(features);
      return context;
   }

   public static ObjectReader getObjectReader(Type type, long features) {
      return getDefaultObjectReaderProvider().getObjectReader(type, JSONReader.Feature.FieldBased.isEnabled(features));
   }

   public static ObjectWriter getObjectWriter(Type type, long features) {
      return getDefaultObjectWriterProvider().getObjectWriter(type, TypeUtils.getClass(type), JSONWriter.Feature.FieldBased.isEnabled(features));
   }

   public static ObjectWriterProvider getDefaultObjectWriterProvider() {
      return defaultObjectWriterProvider;
   }

   public static ObjectReaderProvider getDefaultObjectReaderProvider() {
      ObjectReaderProvider providerLocal = readerProviderLocal.get();
      return providerLocal != null ? providerLocal : defaultObjectReaderProvider;
   }

   public static JSONFactory.JSONPathCompiler getDefaultJSONPathCompiler() {
      JSONFactory.JSONPathCompiler compilerLocal = jsonPathCompilerLocal.get();
      return compilerLocal != null ? compilerLocal : defaultJSONPathCompiler;
   }

   public static void setContextReaderCreator(ObjectReaderCreator creator) {
      readerCreatorLocal.set(creator);
   }

   public static void setContextObjectReaderProvider(ObjectReaderProvider creator) {
      readerProviderLocal.set(creator);
   }

   public static ObjectReaderCreator getContextReaderCreator() {
      return readerCreatorLocal.get();
   }

   public static void setContextJSONPathCompiler(JSONFactory.JSONPathCompiler compiler) {
      jsonPathCompilerLocal.set(compiler);
   }

   public static void setContextWriterCreator(ObjectWriterCreator creator) {
      writerCreatorLocal.set(creator);
   }

   public static ObjectWriterCreator getContextWriterCreator() {
      return writerCreatorLocal.get();
   }

   public static long getDefaultReaderFeatures() {
      return defaultReaderFeatures;
   }

   public static ZoneId getDefaultReaderZoneId() {
      return defaultReaderZoneId;
   }

   public static String getDefaultReaderFormat() {
      return defaultReaderFormat;
   }

   public static long getDefaultWriterFeatures() {
      return defaultWriterFeatures;
   }

   public static ZoneId getDefaultWriterZoneId() {
      return defaultWriterZoneId;
   }

   public static String getDefaultWriterFormat() {
      return defaultWriterFormat;
   }

   public static boolean isDefaultWriterAlphabetic() {
      return defaultWriterAlphabetic;
   }

   public static void setDefaultWriterAlphabetic(boolean defaultWriterAlphabetic) {
      JSONFactory.defaultWriterAlphabetic = defaultWriterAlphabetic;
   }

   public static boolean isDisableReferenceDetect() {
      return disableReferenceDetect;
   }

   public static boolean isDisableAutoType() {
      return disableAutoType;
   }

   public static boolean isDisableJSONB() {
      return disableJSONB;
   }

   public static boolean isDisableArrayMapping() {
      return disableArrayMapping;
   }

   public static void setDisableReferenceDetect(boolean disableReferenceDetect) {
      defaultObjectWriterProvider.setDisableReferenceDetect(disableReferenceDetect);
      defaultObjectReaderProvider.setDisableReferenceDetect(disableReferenceDetect);
   }

   public static void setDisableArrayMapping(boolean disableArrayMapping) {
      defaultObjectWriterProvider.setDisableArrayMapping(disableArrayMapping);
      defaultObjectReaderProvider.setDisableArrayMapping(disableArrayMapping);
   }

   public static void setDisableJSONB(boolean disableJSONB) {
      defaultObjectWriterProvider.setDisableJSONB(disableJSONB);
      defaultObjectReaderProvider.setDisableJSONB(disableJSONB);
   }

   public static void setDisableAutoType(boolean disableAutoType) {
      defaultObjectWriterProvider.setDisableAutoType(disableAutoType);
      defaultObjectReaderProvider.setDisableAutoType(disableAutoType);
   }

   public static boolean isDisableSmartMatch() {
      return disableSmartMatch;
   }

   public static void setDisableSmartMatch(boolean disableSmartMatch) {
      defaultObjectReaderProvider.setDisableSmartMatch(disableSmartMatch);
   }

   static {
      Properties properties = new Properties();
      InputStream inputStream = AccessController.doPrivileged((PrivilegedAction<InputStream>)(() -> {
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         String resourceFile = "fastjson2.properties";
         return cl != null ? cl.getResourceAsStream("fastjson2.properties") : ClassLoader.getSystemResourceAsStream("fastjson2.properties");
      }));
      if (inputStream != null) {
         try {
            properties.load(inputStream);
         } catch (IOException var28) {
         } finally {
            IOUtils.close(inputStream);
         }
      }

      DEFAULT_PROPERTIES = properties;
      String property = System.getProperty("fastjson2.creator");
      if (property != null) {
         property = property.trim();
      }

      if (property == null || property.isEmpty()) {
         property = properties.getProperty("fastjson2.creator");
         if (property != null) {
            property = property.trim();
         }
      }

      CREATOR = property == null ? "asm" : property;
      boolean disableReferenceDetect0 = false;
      boolean disableArrayMapping0 = false;
      boolean disableJSONB0 = false;
      boolean disableAutoType0 = false;
      boolean disableSmartMatch0 = false;
      String features = System.getProperty("fastjson2.features");
      if (features == null) {
         features = getProperty("fastjson2.features");
      }

      if (features != null) {
         for (String feature : features.split(",")) {
            switch (feature) {
               case "disableReferenceDetect":
                  disableReferenceDetect0 = true;
                  break;
               case "disableArrayMapping":
                  disableArrayMapping0 = true;
                  break;
               case "disableJSONB":
                  disableJSONB0 = true;
                  break;
               case "disableAutoType":
                  disableAutoType0 = true;
                  break;
               case "disableSmartMatch":
                  disableSmartMatch0 = true;
            }
         }
      }

      disableReferenceDetect = disableReferenceDetect0;
      disableArrayMapping = disableArrayMapping0;
      disableJSONB = disableJSONB0;
      disableAutoType = disableAutoType0;
      disableSmartMatch = disableSmartMatch0;
      useJacksonAnnotation = getPropertyBool(properties, "fastjson2.useJacksonAnnotation", true);
      useGsonAnnotation = getPropertyBool(properties, "fastjson2.useGsonAnnotation", true);
      defaultWriterAlphabetic = getPropertyBool(properties, "fastjson2.writer.alphabetic", true);
      boolean readerVector = getPropertyBool(properties, "fastjson2.readerVector", false);
      Function<JSONWriter.Context, JSONWriter> incubatorVectorCreatorUTF8 = null;
      Function<JSONWriter.Context, JSONWriter> incubatorVectorCreatorUTF16 = null;
      JSONFactory.JSONReaderUTF8Creator readerCreatorASCII = null;
      JSONFactory.JSONReaderUTF8Creator readerCreatorUTF8 = null;
      JSONFactory.JSONReaderUTF16Creator readerCreatorUTF16 = null;
      if (JDKUtils.VECTOR_SUPPORT) {
         if (JDKUtils.VECTOR_BIT_LENGTH >= 64) {
            try {
               Class<?> factoryClass = Class.forName("com.alibaba.fastjson2.JSONWriterUTF8Vector$Factory");
               incubatorVectorCreatorUTF8 = (Function<JSONWriter.Context, JSONWriter>)factoryClass.newInstance();
            } catch (Throwable var27) {
               initErrorLast = var27;
            }

            try {
               Class<?> factoryClass = Class.forName("com.alibaba.fastjson2.JSONWriterUTF16Vector$Factory");
               incubatorVectorCreatorUTF16 = (Function<JSONWriter.Context, JSONWriter>)factoryClass.newInstance();
            } catch (Throwable var26) {
               initErrorLast = var26;
            }

            if (readerVector) {
               try {
                  Class<?> factoryClass = Class.forName("com.alibaba.fastjson2.JSONReaderASCIIVector$Factory");
                  readerCreatorASCII = (JSONFactory.JSONReaderUTF8Creator)factoryClass.newInstance();
               } catch (Throwable var25) {
                  initErrorLast = var25;
               }

               try {
                  Class<?> factoryClass = Class.forName("com.alibaba.fastjson2.JSONReaderUTF8Vector$Factory");
                  readerCreatorUTF8 = (JSONFactory.JSONReaderUTF8Creator)factoryClass.newInstance();
               } catch (Throwable var24) {
                  initErrorLast = var24;
               }
            }
         }

         if (JDKUtils.VECTOR_BIT_LENGTH >= 128 && readerVector) {
            try {
               Class<?> factoryClass = Class.forName("com.alibaba.fastjson2.JSONReaderUTF16Vector$Factory");
               readerCreatorUTF16 = (JSONFactory.JSONReaderUTF16Creator)factoryClass.newInstance();
            } catch (Throwable var23) {
               initErrorLast = var23;
            }
         }
      }

      INCUBATOR_VECTOR_WRITER_CREATOR_UTF8 = incubatorVectorCreatorUTF8;
      INCUBATOR_VECTOR_WRITER_CREATOR_UTF16 = incubatorVectorCreatorUTF16;
      INCUBATOR_VECTOR_READER_CREATOR_ASCII = readerCreatorASCII;
      INCUBATOR_VECTOR_READER_CREATOR_UTF8 = readerCreatorUTF8;
      INCUBATOR_VECTOR_READER_CREATOR_UTF16 = readerCreatorUTF16;
      JSONFactory.CacheItem[] items = new JSONFactory.CacheItem[16];

      for (int i = 0; i < items.length; i++) {
         items[i] = new JSONFactory.CacheItem();
      }

      CACHE_ITEMS = items;
      CHARS_UPDATER = AtomicReferenceFieldUpdater.newUpdater(JSONFactory.CacheItem.class, char[].class, "chars");
      BYTES_UPDATER = AtomicReferenceFieldUpdater.newUpdater(JSONFactory.CacheItem.class, byte[].class, "bytes");
      defaultObjectWriterProvider = new ObjectWriterProvider();
      defaultObjectReaderProvider = new ObjectReaderProvider();
      JSONPathCompilerReflect compiler = null;
      String var36 = CREATOR;
      switch (var36) {
         case "reflect":
         case "lambda":
            compiler = JSONPathCompilerReflect.INSTANCE;
            break;
         default:
            try {
               if (!JDKUtils.ANDROID && !JDKUtils.GRAAL) {
                  compiler = JSONPathCompilerReflectASM.INSTANCE;
               }
            } catch (Throwable var22) {
            }

            if (compiler == null) {
               compiler = JSONPathCompilerReflect.INSTANCE;
            }
      }

      defaultJSONPathCompiler = compiler;
      readerCreatorLocal = new ThreadLocal<>();
      readerProviderLocal = new ThreadLocal<>();
      writerCreatorLocal = new ThreadLocal<>();
      jsonPathCompilerLocal = new ThreadLocal<>();
      ARRAY_READER = getDefaultObjectReaderProvider().getObjectReader(JSONArray.class);
      OBJECT_READER = getDefaultObjectReaderProvider().getObjectReader(JSONObject.class);
      UUID_VALUES = new byte[55];

      for (char c = '0'; c <= '9'; c++) {
         UUID_VALUES[c - '0'] = (byte)(c - '0');
      }

      for (char c = 'a'; c <= 'f'; c++) {
         UUID_VALUES[c - '0'] = (byte)(c - 'a' + 10);
      }

      for (char c = 'A'; c <= 'F'; c++) {
         UUID_VALUES[c - '0'] = (byte)(c - 'A' + 10);
      }
   }

   static final class CacheItem {
      volatile char[] chars;
      volatile byte[] bytes;
   }

   public interface JSONPathCompiler {
      JSONPath compile(Class var1, JSONPath var2);
   }

   interface JSONReaderUTF16Creator {
      JSONReader create(JSONReader.Context var1, String var2, char[] var3, int var4, int var5);
   }

   interface JSONReaderUTF8Creator {
      JSONReader create(JSONReader.Context var1, String var2, byte[] var3, int var4, int var5);
   }

   static final class NameCacheEntry {
      final String name;
      final long value;

      public NameCacheEntry(String name, long value) {
         this.name = name;
         this.value = value;
      }
   }

   static final class NameCacheEntry2 {
      final String name;
      final long value0;
      final long value1;

      public NameCacheEntry2(String name, long value0, long value1) {
         this.name = name;
         this.value0 = value0;
         this.value1 = value1;
      }
   }
}
