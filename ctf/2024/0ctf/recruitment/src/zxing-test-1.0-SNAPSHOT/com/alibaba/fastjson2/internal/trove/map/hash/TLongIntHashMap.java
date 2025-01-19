package com.alibaba.fastjson2.internal.trove.map.hash;

import java.util.Arrays;
import java.util.function.BiFunction;

public final class TLongIntHashMap {
   static final int largestPrime = TLongIntHashMap.primeCapacities[TLongIntHashMap.primeCapacities.length - 1];
   static final int[] primeCapacities = new int[]{
      5,
      11,
      23,
      47,
      97,
      197,
      397,
      797,
      1597,
      3203,
      6421,
      12853,
      25717,
      51437,
      102877,
      205759,
      411527,
      823117,
      1646237,
      3292489,
      6584983,
      13169977,
      26339969,
      52679969,
      105359939,
      210719881,
      421439783,
      842879579,
      1685759167,
      433,
      877,
      1759,
      3527,
      7057,
      14143,
      28289,
      56591,
      113189,
      226379,
      452759,
      905551,
      1811107,
      3622219,
      7244441,
      14488931,
      28977863,
      57955739,
      115911563,
      231823147,
      463646329,
      927292699,
      1854585413,
      953,
      1907,
      3821,
      7643,
      15287,
      30577,
      61169,
      122347,
      244703,
      489407,
      978821,
      1957651,
      3915341,
      7830701,
      15661423,
      31322867,
      62645741,
      125291483,
      250582987,
      501165979,
      1002331963,
      2004663929,
      1039,
      2081,
      4177,
      8363,
      16729,
      33461,
      66923,
      133853,
      267713,
      535481,
      1070981,
      2141977,
      4283963,
      8567929,
      17135863,
      34271747,
      68543509,
      137087021,
      274174111,
      548348231,
      1096696463,
      31,
      67,
      137,
      277,
      557,
      1117,
      2237,
      4481,
      8963,
      17929,
      35863,
      71741,
      143483,
      286973,
      573953,
      1147921,
      2295859,
      4591721,
      9183457,
      18366923,
      36733847,
      73467739,
      146935499,
      293871013,
      587742049,
      1175484103,
      599,
      1201,
      2411,
      4831,
      9677,
      19373,
      38747,
      77509,
      155027,
      310081,
      620171,
      1240361,
      2480729,
      4961459,
      9922933,
      19845871,
      39691759,
      79383533,
      158767069,
      317534141,
      635068283,
      1270136683,
      311,
      631,
      1277,
      2557,
      5119,
      10243,
      20507,
      41017,
      82037,
      164089,
      328213,
      656429,
      1312867,
      2625761,
      5251529,
      10503061,
      21006137,
      42012281,
      84024581,
      168049163,
      336098327,
      672196673,
      1344393353,
      3,
      7,
      17,
      37,
      79,
      163,
      331,
      673,
      1361,
      2729,
      5471,
      10949,
      21911,
      43853,
      87719,
      175447,
      350899,
      701819,
      1403641,
      2807303,
      5614657,
      11229331,
      22458671,
      44917381,
      89834777,
      179669557,
      359339171,
      718678369,
      1437356741,
      43,
      89,
      179,
      359,
      719,
      1439,
      2879,
      5779,
      11579,
      23159,
      46327,
      92657,
      185323,
      370661,
      741337,
      1482707,
      2965421,
      5930887,
      11861791,
      23723597,
      47447201,
      94894427,
      189788857,
      379577741,
      759155483,
      1518310967,
      379,
      761,
      1523,
      3049,
      6101,
      12203,
      24407,
      48817,
      97649,
      195311,
      390647,
      781301,
      1562611,
      3125257,
      6250537,
      12501169,
      25002389,
      50004791,
      100009607,
      200019221,
      400038451,
      800076929,
      1600153859
   };
   private int[] values;
   private long[] set;
   private boolean consumeFreeSlot;
   private int size;
   private int free;
   private int maxSize;

   static int nextPrime(int desiredCapacity) {
      if (desiredCapacity >= largestPrime) {
         return largestPrime;
      } else {
         int i = Arrays.binarySearch(primeCapacities, desiredCapacity);
         if (i < 0) {
            i = -i - 1;
         }

         return primeCapacities[i];
      }
   }

   public TLongIntHashMap() {
      int capacity = 37;
      this.maxSize = 18;
      this.free = 37;
      this.set = new long[capacity];
      this.values = new int[capacity];
   }

   public TLongIntHashMap(long key, int value) {
      this.maxSize = 18;
      this.set = new long[37];
      this.values = new int[37];
      this.consumeFreeSlot = true;
      int index = ((int)(key ^ key >>> 32) & 2147483647) % this.set.length;
      this.set[index] = key;
      this.values[index] = value;
      this.free = 36;
      this.size = 1;
   }

   public void put(long key, int value) {
      int hash = (int)(key ^ key >>> 32) & 2147483647;
      int index = hash % this.set.length;
      long setKey = this.set[index];
      this.consumeFreeSlot = false;
      if (setKey == 0L) {
         this.consumeFreeSlot = true;
         this.set[index] = key;
      } else if (setKey == key) {
         index = -index - 1;
      } else {
         int length = this.set.length;
         int probe = 1 + hash % (length - 2);
         int loopIndex = index;

         do {
            index -= probe;
            if (index < 0) {
               index += length;
            }

            setKey = this.set[index];
            if (setKey == 0L) {
               this.consumeFreeSlot = true;
               this.set[index] = key;
               break;
            }

            if (setKey == key) {
               index = -index - 1;
               break;
            }
         } while (index != loopIndex);
      }

      boolean isNewMapping = true;
      if (index < 0) {
         index = -index - 1;
         isNewMapping = false;
      }

      this.values[index] = value;
      if (isNewMapping) {
         if (this.consumeFreeSlot) {
            this.free--;
         }

         if (++this.size > this.maxSize || this.free == 0) {
            int capacity = this.set.length;
            int newCapacity = this.size > this.maxSize ? nextPrime(capacity << 1) : capacity;
            int oldCapacity = this.set.length;
            long[] oldKeys = this.set;
            int[] oldVals = this.values;
            this.set = new long[newCapacity];
            this.values = new int[newCapacity];
            int i = oldCapacity;

            while (i-- > 0) {
               if (oldKeys[i] != 0L) {
                  long o = oldKeys[i];
                  index = this.insertKey(o);
                  this.values[index] = oldVals[i];
               }
            }

            int var18 = this.set.length;
            this.maxSize = Math.min(var18 - 1, (int)((float)var18 * 0.5F));
            this.free = var18 - this.size;
         }
      }
   }

   public int putIfAbsent(long key, int value) {
      int hash = (int)(key ^ key >>> 32) & 2147483647;
      int index = hash % this.set.length;
      long setKey = this.set[index];
      this.consumeFreeSlot = false;
      if (setKey == 0L) {
         this.consumeFreeSlot = true;
         this.set[index] = key;
      } else if (setKey == key) {
         index = -index - 1;
      } else {
         int loopIndex = index;

         do {
            index -= 1 + hash % (this.set.length - 2);
            if (index < 0) {
               index += this.set.length;
            }

            setKey = this.set[index];
            if (setKey == 0L) {
               this.consumeFreeSlot = true;
               this.set[index] = key;
               break;
            }

            if (setKey == key) {
               index = -index - 1;
               break;
            }
         } while (index != loopIndex);
      }

      if (index < 0) {
         return this.values[-index - 1];
      } else {
         this.values[index] = value;
         if (this.consumeFreeSlot) {
            this.free--;
         }

         if (++this.size > this.maxSize || this.free == 0) {
            hash = this.set.length;
            this.rehash(hash);
            hash = this.set.length;
            this.maxSize = Math.min(hash - 1, (int)((float)hash * 0.5F));
            this.free = hash - this.size;
         }

         return value;
      }
   }

   private void rehash(int capacity) {
      int newCapacity = this.size > this.maxSize ? nextPrime(capacity << 1) : capacity;
      int oldCapacity = this.set.length;
      long[] oldKeys = this.set;
      int[] oldVals = this.values;
      this.set = new long[newCapacity];
      this.values = new int[newCapacity];
      int i = oldCapacity;

      while (i-- > 0) {
         long key = oldKeys[i];
         if (key != 0L) {
            this.values[this.insertKey(key)] = oldVals[i];
         }
      }
   }

   public int get(long key) {
      int DEFAULT_ENTRY_VALUE = -1;
      int length = this.set.length;
      int hash = (int)(key ^ key >>> 32) & 2147483647;
      int index = hash % length;
      long setKey = this.set[index];
      if (setKey == 0L) {
         return -1;
      } else if (setKey == key) {
         return this.values[index];
      } else {
         int setLength = this.set.length;
         int probe = 1 + hash % (setLength - 2);
         int loopIndex = index;

         do {
            index -= probe;
            if (index < 0) {
               index += setLength;
            }

            setKey = this.set[index];
            if (setKey == 0L) {
               return -1;
            }

            if (key == this.set[index]) {
               return this.values[index];
            }
         } while (index != loopIndex);

         return -1;
      }
   }

   public boolean forEachEntry(BiFunction<Long, Integer, Boolean> procedure) {
      long[] keys = this.set;
      int[] values = this.values;
      int i = keys.length;

      while (i-- > 0) {
         if (this.set[i] != 0L && !procedure.apply(keys[i], values[i])) {
            return false;
         }
      }

      return true;
   }

   @Override
   public String toString() {
      final StringBuilder buf = new StringBuilder("{");
      this.forEachEntry(new BiFunction<Long, Integer, Boolean>() {
         private boolean first = true;

         public Boolean apply(Long key, Integer value) {
            if (this.first) {
               this.first = false;
            } else {
               buf.append(", ");
            }

            buf.append(key);
            buf.append("=");
            buf.append(value);
            return true;
         }
      });
      buf.append("}");
      return buf.toString();
   }

   public int size() {
      return this.size;
   }

   private int insertKey(long key) {
      int hash = (int)(key ^ key >>> 32) & 2147483647;
      int index = hash % this.set.length;
      boolean state = this.set[index] != 0L;
      this.consumeFreeSlot = false;
      if (!state) {
         this.consumeFreeSlot = true;
         this.set[index] = key;
         return index;
      } else if (this.set[index] == key) {
         return -index - 1;
      } else {
         int length = this.set.length;
         int probe = 1 + hash % (length - 2);
         int loopIndex = index;

         do {
            index -= probe;
            if (index < 0) {
               index += length;
            }

            state = this.set[index] != 0L;
            if (!state) {
               this.consumeFreeSlot = true;
               this.set[index] = key;
               return index;
            }

            if (this.set[index] == key) {
               return -index - 1;
            }
         } while (index != loopIndex);

         throw new IllegalStateException("No free or removed slots available. Key set full?!!");
      }
   }

   static {
      Arrays.sort(primeCapacities);
   }
}
