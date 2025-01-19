package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public class ToInteger implements Function {
   final Integer defaultValue;

   public ToInteger(Integer defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? 1 : 0;
      } else if (o instanceof Number) {
         return ((Number)o).intValue();
      } else {
         throw new JSONException("can not cast to Integer " + o.getClass());
      }
   }
}
