package com.beust.jcommander.converters;

import com.beust.jcommander.IStringConverter;

public class StringConverter implements IStringConverter<String> {
   public String convert(String value) {
      return value;
   }
}
