package com.alibaba.fastjson2.filter;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ValueFilter extends Filter {
   Object apply(Object var1, String var2, Object var3);

   static ValueFilter compose(ValueFilter before, ValueFilter after) {
      return (object, name, value) -> after.apply(object, name, before.apply(object, name, value));
   }

   static ValueFilter of(String name, Function function) {
      return (object, fieldName, fieldValue) -> name != null && !name.equals(fieldName) ? fieldValue : function.apply(fieldValue);
   }

   static ValueFilter of(String name, Map map) {
      return (object, fieldName, fieldValue) -> {
         if (name == null || name.equals(fieldName)) {
            Object o = map.get(fieldValue);
            if (o != null || map.containsKey(fieldValue)) {
               return o;
            }
         }

         return fieldValue;
      };
   }

   static ValueFilter of(Predicate<String> nameMatcher, Function function) {
      return (object, fieldName, fieldValue) -> nameMatcher != null && !nameMatcher.test(fieldName) ? fieldValue : function.apply(fieldValue);
   }
}
