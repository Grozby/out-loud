package com.alibaba.fastjson2.function;

@FunctionalInterface
public interface FieldConsumer<T> {
   void accept(T var1, int var2, Object var3);
}
