package com.alibaba.fastjson2.function;

@FunctionalInterface
public interface FieldSupplier<T> {
   Object get(T var1, int var2);
}
