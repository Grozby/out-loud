package com.alibaba.fastjson2.reader;

import java.nio.charset.Charset;

public interface ByteArrayValueConsumer {
   default void start() {
   }

   default void beforeRow(int row) {
   }

   void accept(int var1, int var2, byte[] var3, int var4, int var5, Charset var6);

   default void afterRow(int row) {
   }

   default void end() {
   }
}
