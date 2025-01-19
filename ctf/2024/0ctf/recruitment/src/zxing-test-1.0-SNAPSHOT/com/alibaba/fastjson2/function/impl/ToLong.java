package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public class ToLong implements Function {
   final Long defaultValue;

   public ToLong(Long defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? 1L : 0L;
      } else if (o instanceof Number) {
         return ((Number)o).longValue();
      } else {
         throw new JSONException("can not cast to Long " + o.getClass());
      }
   }
}
