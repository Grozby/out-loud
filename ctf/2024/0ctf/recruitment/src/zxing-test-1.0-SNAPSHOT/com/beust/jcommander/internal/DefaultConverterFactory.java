package com.beust.jcommander.internal;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;
import com.beust.jcommander.converters.BigDecimalConverter;
import com.beust.jcommander.converters.BooleanConverter;
import com.beust.jcommander.converters.DoubleConverter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.converters.FloatConverter;
import com.beust.jcommander.converters.ISO8601DateConverter;
import com.beust.jcommander.converters.IntegerConverter;
import com.beust.jcommander.converters.LongConverter;
import com.beust.jcommander.converters.PathConverter;
import com.beust.jcommander.converters.StringConverter;
import com.beust.jcommander.converters.URIConverter;
import com.beust.jcommander.converters.URLConverter;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

public class DefaultConverterFactory implements IStringConverterFactory {
   private static Map<Class, Class<? extends IStringConverter<?>>> classConverters = Maps.newHashMap();

   @Override
   public Class<? extends IStringConverter<?>> getConverter(Class forType) {
      return classConverters.get(forType);
   }

   static {
      classConverters.put(String.class, StringConverter.class);
      classConverters.put(Integer.class, IntegerConverter.class);
      classConverters.put(int.class, IntegerConverter.class);
      classConverters.put(Long.class, LongConverter.class);
      classConverters.put(long.class, LongConverter.class);
      classConverters.put(Float.class, FloatConverter.class);
      classConverters.put(float.class, FloatConverter.class);
      classConverters.put(Double.class, DoubleConverter.class);
      classConverters.put(double.class, DoubleConverter.class);
      classConverters.put(Boolean.class, BooleanConverter.class);
      classConverters.put(boolean.class, BooleanConverter.class);
      classConverters.put(File.class, FileConverter.class);
      classConverters.put(BigDecimal.class, BigDecimalConverter.class);
      classConverters.put(Date.class, ISO8601DateConverter.class);
      classConverters.put(URI.class, URIConverter.class);
      classConverters.put(URL.class, URLConverter.class);

      try {
         classConverters.put(Path.class, PathConverter.class);
      } catch (NoClassDefFoundError var1) {
      }
   }
}
