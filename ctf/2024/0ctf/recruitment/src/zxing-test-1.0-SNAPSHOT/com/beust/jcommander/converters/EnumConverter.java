package com.beust.jcommander.converters;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.util.EnumSet;

public class EnumConverter<T extends Enum<T>> implements IStringConverter<T> {
   private final String optionName;
   private final Class<T> clazz;

   public EnumConverter(String optionName, Class<T> clazz) {
      this.optionName = optionName;
      this.clazz = clazz;
   }

   public T convert(String value) {
      try {
         try {
            return Enum.valueOf(this.clazz, value);
         } catch (IllegalArgumentException var3) {
            return Enum.valueOf(this.clazz, value.toUpperCase());
         }
      } catch (Exception var4) {
         throw new ParameterException("Invalid value for " + this.optionName + " parameter. Allowed values:" + EnumSet.<T>allOf(this.clazz));
      }
   }
}
