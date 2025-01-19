package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import java.util.function.Function;

public class ToByte implements Function {
   final Byte defaultValue;

   public ToByte(Byte defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return Byte.valueOf((byte)((Boolean)o ? 1 : 0));
      } else if (o instanceof Number) {
         return ((Number)o).byteValue();
      } else {
         throw new JSONException("can not cast to Byte " + o.getClass());
      }
   }
}
