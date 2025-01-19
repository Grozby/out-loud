package com.beust.jcommander;

public interface IStringConverterInstanceFactory {
   IStringConverter<?> getConverterInstance(Parameter var1, Class<?> var2, String var3);
}
