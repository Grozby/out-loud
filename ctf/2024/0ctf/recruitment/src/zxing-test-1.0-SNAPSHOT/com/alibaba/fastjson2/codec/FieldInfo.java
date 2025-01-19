package com.alibaba.fastjson2.codec;

import com.alibaba.fastjson2.reader.ObjectReader;
import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.function.BiConsumer;

public class FieldInfo {
   public static final long VALUE_MASK = 281474976710656L;
   public static final long UNWRAPPED_MASK = 562949953421312L;
   public static final long RAW_VALUE_MASK = 1125899906842624L;
   public static final long READ_USING_MASK = 2251799813685248L;
   public static final long FIELD_MASK = 4503599627370496L;
   public static final long DISABLE_SMART_MATCH = 9007199254740992L;
   public static final long JIT = 18014398509481984L;
   public static final long DISABLE_UNSAFE = 36028797018963968L;
   public static final long READ_ONLY = 72057594037927936L;
   public static final long DISABLE_REFERENCE_DETECT = 144115188075855872L;
   public static final long DISABLE_ARRAY_MAPPING = 288230376151711744L;
   public static final long DISABLE_AUTO_TYPE = 576460752303423488L;
   public static final long DISABLE_JSONB = 1152921504606846976L;
   public static final long BACKR_EFERENCE = 2305843009213693952L;
   public String fieldName;
   public String format;
   public String label;
   public int ordinal;
   public long features;
   public boolean ignore;
   public String[] alternateNames;
   public Class<?> writeUsing;
   public Class<?> keyUsing;
   public Class<?> valueUsing;
   public Class<?> readUsing;
   public boolean fieldClassMixIn;
   public boolean isTransient;
   public String defaultValue;
   public Locale locale;
   public String schema;
   public boolean required;
   public String arrayToMapKey;
   public Class<?> arrayToMapDuplicateHandler;

   public ObjectReader getInitReader() {
      Class<?> calzz = this.readUsing;
      if (calzz != null && ObjectReader.class.isAssignableFrom(calzz)) {
         try {
            Constructor<?> constructor = calzz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ObjectReader)constructor.newInstance();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   public BiConsumer getInitArrayToMapDuplicateHandler() {
      Class<?> clazz = this.arrayToMapDuplicateHandler;
      if (clazz != null && BiConsumer.class.isAssignableFrom(clazz)) {
         try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (BiConsumer)constructor.newInstance();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   public void init() {
      this.fieldName = null;
      this.format = null;
      this.label = null;
      this.ordinal = 0;
      this.features = 0L;
      this.ignore = false;
      this.alternateNames = null;
      this.writeUsing = null;
      this.keyUsing = null;
      this.valueUsing = null;
      this.readUsing = null;
      this.fieldClassMixIn = false;
      this.isTransient = false;
      this.defaultValue = null;
      this.locale = null;
      this.schema = null;
      this.required = false;
      this.arrayToMapKey = null;
      this.arrayToMapDuplicateHandler = null;
   }
}
