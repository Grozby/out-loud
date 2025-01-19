package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.Fnv;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public final class SymbolTable {
   private final String[] names;
   private final long hashCode64;
   private final short[] mapping;
   private final long[] hashCodes;
   private final long[] hashCodesOrigin;

   public SymbolTable(String... input) {
      Set<String> set = new TreeSet<>(Arrays.asList(input));
      this.names = new String[set.size()];
      Iterator<String> it = set.iterator();

      for (int i = 0; i < this.names.length; i++) {
         if (it.hasNext()) {
            this.names[i] = it.next();
         }
      }

      long[] hashCodes = new long[this.names.length];

      for (int ix = 0; ix < this.names.length; ix++) {
         long hashCode = Fnv.hashCode64(this.names[ix]);
         hashCodes[ix] = hashCode;
      }

      this.hashCodesOrigin = hashCodes;
      this.hashCodes = Arrays.copyOf(hashCodes, hashCodes.length);
      Arrays.sort(this.hashCodes);
      this.mapping = new short[this.hashCodes.length];

      for (int ix = 0; ix < hashCodes.length; ix++) {
         long hashCode = hashCodes[ix];
         int index = Arrays.binarySearch(this.hashCodes, hashCode);
         this.mapping[index] = (short)ix;
      }

      long hashCode64 = -3750763034362895579L;

      for (long hashCode : hashCodes) {
         hashCode64 ^= hashCode;
         hashCode64 *= 1099511628211L;
      }

      this.hashCode64 = hashCode64;
   }

   public int size() {
      return this.names.length;
   }

   public long hashCode64() {
      return this.hashCode64;
   }

   public String getNameByHashCode(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodes, hashCode);
      if (m < 0) {
         return null;
      } else {
         int index = this.mapping[m];
         return this.names[index];
      }
   }

   public int getOrdinalByHashCode(long hashCode) {
      int m = Arrays.binarySearch(this.hashCodes, hashCode);
      return m < 0 ? -1 : this.mapping[m] + 1;
   }

   public int getOrdinal(String name) {
      int m = Arrays.binarySearch(this.hashCodes, Fnv.hashCode64(name));
      return m < 0 ? -1 : this.mapping[m] + 1;
   }

   public String getName(int ordinal) {
      return this.names[ordinal - 1];
   }

   public long getHashCode(int ordinal) {
      return this.hashCodesOrigin[ordinal - 1];
   }
}
