package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.BeanUtils;

public enum PropertyNamingStrategy {
   CamelCase,
   CamelCase1x,
   PascalCase,
   SnakeCase,
   UpperCase,
   UpperCamelCaseWithSpaces,
   UpperCamelCaseWithUnderScores,
   UpperCamelCaseWithDashes,
   UpperCamelCaseWithDots,
   KebabCase,
   UpperCaseWithUnderScores,
   UpperCaseWithDashes,
   UpperCaseWithDots,
   LowerCase,
   LowerCaseWithUnderScores,
   LowerCaseWithDashes,
   LowerCaseWithDots,
   NeverUseThisValueExceptDefaultValue;

   public String fieldName(String name) {
      return BeanUtils.fieldName(name, this.name());
   }

   public static String snakeToCamel(String name) {
      if (name != null && name.indexOf(95) != -1) {
         int underscoreCount = 0;

         for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '_') {
               underscoreCount++;
            }
         }

         char[] chars = new char[name.length() - underscoreCount];
         int ix = 0;

         for (int j = 0; ix < name.length(); ix++) {
            char ch = name.charAt(ix);
            if (ch != '_') {
               if (ix > 0 && name.charAt(ix - 1) == '_' && ch >= 'a' && ch <= 'z') {
                  ch = (char)(ch - ' ');
               }

               chars[j++] = ch;
            }
         }

         return new String(chars);
      } else {
         return name;
      }
   }

   public static PropertyNamingStrategy of(String strategy) {
      if (strategy != null && !strategy.isEmpty()) {
         switch (strategy) {
            case "Upper":
            case "upper":
               return UpperCase;
            case "Lower":
            case "lower":
               return LowerCase;
            case "Camel":
            case "camel":
               return CamelCase;
            default:
               for (PropertyNamingStrategy value : values()) {
                  if (value.name().equals(strategy)) {
                     return value;
                  }
               }

               return null;
         }
      } else {
         return null;
      }
   }
}
