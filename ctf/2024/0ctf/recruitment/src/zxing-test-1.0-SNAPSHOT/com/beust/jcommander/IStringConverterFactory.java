package com.beust.jcommander;

public interface IStringConverterFactory {
   Class<? extends IStringConverter<?>> getConverter(Class<?> var1);
}
