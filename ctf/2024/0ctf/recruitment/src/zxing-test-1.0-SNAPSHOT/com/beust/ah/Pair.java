package com.beust.ah;

import java.util.AbstractMap.SimpleEntry;

class Pair<T, V> {
   SimpleEntry<T, V> entry;

   public Pair(T t, V v) {
      this.entry = new SimpleEntry<>(t, v);
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Pair)) {
         return false;
      } else {
         Pair<?, ?> otherPair = (Pair<?, ?>)o;
         return this.entry.equals(otherPair.entry);
      }
   }

   @Override
   public int hashCode() {
      return this.entry.hashCode();
   }
}
