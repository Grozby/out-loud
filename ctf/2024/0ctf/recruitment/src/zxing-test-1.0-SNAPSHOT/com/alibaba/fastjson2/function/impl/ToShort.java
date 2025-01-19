package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public class ToShort implements Function {
   final Short defaultValue;

   public ToShort(Short defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return Short.valueOf((short)((Boolean)o ? 1 : 0));
      } else if (o instanceof Number) {
         return ((Number)o).shortValue();
      } else {
         throw new JSONException("can not cast to Short " + o.getClass());
      }
   }
}
