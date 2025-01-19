package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import java.util.List;
import java.util.function.Function;

public class ToDouble implements Function {
   final Double defaultValue;

   public ToDouble(Double defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object o) {
      if (o == null) {
         return this.defaultValue;
      } else if (o instanceof Boolean) {
         return (Boolean)o ? 1.0 : 0.0;
      } else if (o instanceof Number) {
         return ((Number)o).doubleValue();
      } else if (o instanceof String) {
         String str = (String)o;
         return str.isEmpty() ? this.defaultValue : Double.parseDouble(str);
      } else if (!(o instanceof List)) {
         throw new JSONException("can not cast to Double " + o.getClass());
      } else {
         List list = (List)o;
         JSONArray array = new JSONArray(list.size());

         for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            array.add(this.apply(item));
         }

         return array;
      }
   }
}
