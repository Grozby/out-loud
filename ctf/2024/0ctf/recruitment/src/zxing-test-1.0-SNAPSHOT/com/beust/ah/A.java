package com.beust.ah;

import java.util.HashSet;
import java.util.Set;

public class A {
   public static void main(String[] args) {
      int n = 40000;
      Set<Pair<Integer, Integer>> set = new HashSet<>();
      long start = System.currentTimeMillis();

      for (int i = 0; i < n; i++) {
         set.add(new Pair<>(i, i));
      }

      System.out.printf("Total time to add %d entries: %d ms", n, System.currentTimeMillis() - start);
   }
}
