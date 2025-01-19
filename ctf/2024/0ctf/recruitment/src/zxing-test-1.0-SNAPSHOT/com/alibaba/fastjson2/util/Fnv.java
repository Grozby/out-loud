package com.alibaba.fastjson2.util;

public final class Fnv {
   public static final long MAGIC_HASH_CODE = -3750763034362895579L;
   public static final long MAGIC_PRIME = 1099511628211L;

   public static long hashCode64LCase(String name) {
      boolean ascii = true;
      long nameValue = 0L;
      int scoreCount = 0;

      for (int i = 0; i < name.length(); i++) {
         char ch = name.charAt(i);
         if (ch > 255 || i == 0 && ch == 0) {
            ascii = false;
            break;
         }

         if (ch == '-' || ch == '_' || ch == ' ') {
            scoreCount++;
         }
      }

      if (ascii && name.length() - scoreCount <= 8) {
         int i = name.length() - 1;

         for (int j = 0; i >= 0; i--) {
            char chx = name.charAt(i);
            if (chx != '-' && chx != '_' && chx != ' ') {
               if (chx >= 'A' && chx <= 'Z') {
                  chx = (char)(chx + ' ');
               }

               if (j == 0) {
                  nameValue = (long)((byte)chx);
               } else {
                  nameValue <<= 8;
                  nameValue += (long)chx;
               }

               j++;
            }
         }

         if (nameValue != 0L) {
            return nameValue;
         }
      }

      long hashCode = -3750763034362895579L;

      for (int i = 0; i < name.length(); i++) {
         char chx = name.charAt(i);
         if (chx != '-' && chx != '_' && chx != ' ') {
            if (chx >= 'A' && chx <= 'Z') {
               chx = (char)(chx + ' ');
            }

            hashCode ^= (long)chx;
            hashCode *= 1099511628211L;
         }
      }

      return hashCode;
   }

   public static long hashCode64(String... names) {
      if (names.length == 1) {
         return hashCode64(names[0]);
      } else {
         long hashCode = -3750763034362895579L;

         for (String name : names) {
            long h = hashCode64(name);
            hashCode ^= h;
            hashCode *= 1099511628211L;
         }

         return hashCode;
      }
   }

   public static long hashCode64(String name) {
      if (name.length() <= 8) {
         boolean ascii = true;
         long nameValue = 0L;

         for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch > 255 || i == 0 && ch == 0) {
               ascii = false;
               break;
            }
         }

         if (ascii) {
            for (int ix = name.length() - 1; ix >= 0; ix--) {
               char ch = name.charAt(ix);
               if (ix == name.length() - 1) {
                  nameValue = (long)((byte)ch);
               } else {
                  nameValue <<= 8;
                  nameValue += (long)ch;
               }
            }

            if (nameValue != 0L) {
               return nameValue;
            }
         }
      }

      long hashCode = -3750763034362895579L;

      for (int ixx = 0; ixx < name.length(); ixx++) {
         char ch = name.charAt(ixx);
         hashCode ^= (long)ch;
         hashCode *= 1099511628211L;
      }

      return hashCode;
   }
}
