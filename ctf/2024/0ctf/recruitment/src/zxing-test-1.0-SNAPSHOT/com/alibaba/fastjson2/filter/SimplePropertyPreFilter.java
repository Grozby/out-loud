package com.alibaba.fastjson2.filter;

import com.alibaba.fastjson2.JSONWriter;
import java.util.HashSet;
import java.util.Set;

public class SimplePropertyPreFilter implements PropertyPreFilter {
   private final Class<?> clazz;
   private final Set<String> includes = new HashSet<>();
   private final Set<String> excludes = new HashSet<>();
   private int maxLevel;

   public SimplePropertyPreFilter(String... properties) {
      this(null, properties);
   }

   public SimplePropertyPreFilter(Class<?> clazz, String... properties) {
      this.clazz = clazz;

      for (String item : properties) {
         if (item != null) {
            this.includes.add(item);
         }
      }
   }

   public int getMaxLevel() {
      return this.maxLevel;
   }

   public void setMaxLevel(int maxLevel) {
      this.maxLevel = maxLevel;
   }

   public Class<?> getClazz() {
      return this.clazz;
   }

   public Set<String> getIncludes() {
      return this.includes;
   }

   public Set<String> getExcludes() {
      return this.excludes;
   }

   @Override
   public boolean process(JSONWriter writer, Object source, String name) {
      if (source == null) {
         return true;
      } else if (this.clazz != null && !this.clazz.isInstance(source)) {
         return true;
      } else if (this.excludes.contains(name)) {
         return false;
      } else {
         return this.maxLevel > 0 && writer.level() > this.maxLevel ? false : this.includes.size() == 0 || this.includes.contains(name);
      }
   }
}
