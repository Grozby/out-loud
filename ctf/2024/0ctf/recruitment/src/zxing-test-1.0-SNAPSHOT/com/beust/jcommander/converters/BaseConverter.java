package com.beust.jcommander.converters;

import com.beust.jcommander.IStringConverter;

public abstract class BaseConverter<T> implements IStringConverter<T> {
   private String optionName;

   public BaseConverter(String optionName) {
      this.optionName = optionName;
   }

   public String getOptionName() {
      return this.optionName;
   }

   protected String getErrorString(String value, String to) {
      return "\"" + this.getOptionName() + "\": couldn't convert \"" + value + "\" to " + to;
   }
}
