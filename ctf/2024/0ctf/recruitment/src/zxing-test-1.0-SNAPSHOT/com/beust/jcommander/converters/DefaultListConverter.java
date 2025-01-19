package com.beust.jcommander.converters;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.internal.Lists;
import java.util.List;

public class DefaultListConverter<T> implements IStringConverter<List<T>> {
   private final IParameterSplitter splitter;
   private final IStringConverter<T> converter;

   public DefaultListConverter(IParameterSplitter splitter, IStringConverter<T> converter) {
      this.splitter = splitter;
      this.converter = converter;
   }

   public List<T> convert(String value) {
      List<T> result = Lists.newArrayList();

      for (String param : this.splitter.split(value)) {
         result.add(this.converter.convert(param));
      }

      return result;
   }
}
