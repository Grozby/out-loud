package com.alibaba.fastjson2.reader;

public interface CharArrayValueConsumer<T> {
   default void start() {
   }

   default void beforeRow(int row) {
   }

   void accept(int var1, int var2, char[] var3, int var4, int var5);

   default void afterRow(int row) {
   }

   default void end() {
   }
}
