package com.alibaba.fastjson2.filter;

import java.util.Arrays;

public class Labels {
   public static LabelFilter includes(String... views) {
      return new Labels.DefaultLabelFilter(views, null);
   }

   public static LabelFilter excludes(String... views) {
      return new Labels.DefaultLabelFilter(null, views);
   }

   public static class DefaultLabelFilter implements LabelFilter {
      final String[] includes;
      final String[] excludes;

      public DefaultLabelFilter(String[] includes, String[] excludes) {
         if (includes != null) {
            this.includes = new String[includes.length];
            System.arraycopy(includes, 0, this.includes, 0, includes.length);
            Arrays.sort((Object[])this.includes);
         } else {
            this.includes = null;
         }

         if (excludes != null) {
            this.excludes = new String[excludes.length];
            System.arraycopy(excludes, 0, this.excludes, 0, excludes.length);
            Arrays.sort((Object[])this.excludes);
         } else {
            this.excludes = null;
         }
      }

      @Override
      public boolean apply(String label) {
         return this.excludes != null ? Arrays.binarySearch(this.excludes, label) < 0 : this.includes != null && Arrays.binarySearch(this.includes, label) >= 0;
      }
   }
}
