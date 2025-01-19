package com.alibaba.fastjson2.function.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.IOUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class StringToAny implements Function {
   final Object defaultValue;
   final Class targetClass;

   public StringToAny(Class targetClass, Object defaultValue) {
      this.targetClass = targetClass;
      this.defaultValue = defaultValue;
   }

   @Override
   public Object apply(Object from) {
      String str = (String)from;
      if (str == null || "null".equals(str) || str.isEmpty()) {
         return this.defaultValue;
      } else if (this.targetClass == byte.class || this.targetClass == Byte.class) {
         return Byte.parseByte(str);
      } else if (this.targetClass == short.class || this.targetClass == Short.class) {
         return Short.parseShort(str);
      } else if (this.targetClass == int.class || this.targetClass == Integer.class) {
         return Integer.parseInt(str);
      } else if (this.targetClass != long.class && this.targetClass != Long.class) {
         if (this.targetClass == float.class || this.targetClass == Float.class) {
            return Float.parseFloat(str);
         } else if (this.targetClass == double.class || this.targetClass == Double.class) {
            return Double.parseDouble(str);
         } else if (this.targetClass == char.class || this.targetClass == Character.class) {
            return str.charAt(0);
         } else if (this.targetClass == boolean.class || this.targetClass == Boolean.class) {
            return "true".equals(str);
         } else if (this.targetClass == BigDecimal.class) {
            return new BigDecimal(str);
         } else if (this.targetClass == BigInteger.class) {
            return new BigInteger(str);
         } else {
            if (this.targetClass == Collections.class || this.targetClass == List.class || this.targetClass == JSONArray.class) {
               char firstChar = str.charAt(0);
               if (firstChar == '[') {
                  return JSON.parseObject(str, this.targetClass);
               }

               int commaIndex = str.indexOf(44);
               if (commaIndex != -1) {
                  String[] items = str.split(",");
                  return Arrays.asList(items);
               }
            }

            throw new JSONException("can not convert to " + this.targetClass + ", value : " + str);
         }
      } else {
         return !IOUtils.isNumber(str) && str.length() == 19 ? DateUtils.parseMillis(str, DateUtils.DEFAULT_ZONE_ID) : Long.parseLong(str);
      }
   }
}
