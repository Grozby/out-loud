package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public class ToFloat implements Function {
   final Float defaultValue;

   public ToFloat(Float defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? 1.0F : 0.0F;
      } else if (o instanceof Number) {
         return ((Number)o).floatValue();
      } else {
         throw new JSONException("can not cast to Float " + o.getClass());
      }
   }
}
