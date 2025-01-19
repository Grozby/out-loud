package com.alibaba.fastjson2.function.impl;

import java.util.function.Function;

public class ToString implements Function {
   @Override
   public Object apply(Object o) {
      return o == null ? null : o.toString();
   }
}
