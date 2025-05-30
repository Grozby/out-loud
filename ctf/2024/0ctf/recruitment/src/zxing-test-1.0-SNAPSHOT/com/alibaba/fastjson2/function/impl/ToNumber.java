package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public final class ToNumber implements Function {
   final Number defaultValue;

   public ToNumber(Number defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? 1 : 0;
      } else if (o instanceof Number) {
         return o;
      } else {
         throw new JSONException("can not cast to Number " + o.getClass());
      }
   }
}
