package com.alibaba.fastjson2.reader;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public interface ValueConsumer {
   default void accept(byte[] bytes, int off, int len) {
      this.accept(new String(bytes, off, len, StandardCharsets.UTF_8));
   }

   default void acceptNull() {
   }

   default void accept(boolean val) {
   }

   default void accept(int val) {
      this.accept(Integer.valueOf(val));
   }

   default void accept(long val) {
      this.accept(Long.valueOf(val));
   }

   default void accept(Number val) {
   }

   default void accept(String val) {
   }

   default void accept(Map object) {
   }

   default void accept(List array) {
   }
}
