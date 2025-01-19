package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.TypeUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class ToBigDecimal implements Function {
   @Override
   public Object apply(Object o) {
      if (o == null || o instanceof BigDecimal) {
         return o;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? BigDecimal.ONE : BigDecimal.ZERO;
      } else if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long || o instanceof AtomicInteger || o instanceof AtomicLong) {
         return BigDecimal.valueOf(((Number)o).longValue());
      } else if (o instanceof Float || o instanceof Double) {
         double doubleValue = ((Number)o).doubleValue();
         return TypeUtils.toBigDecimal(doubleValue);
      } else if (o instanceof BigInteger) {
         return new BigDecimal((BigInteger)o);
      } else if (o instanceof String) {
         return new BigDecimal((String)o);
      } else {
         throw new JSONException("can not cast to BigDecimal " + o.getClass());
      }
   }
}
