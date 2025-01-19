package com.google.zxing.common;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MinimalECIInput implements ECIInput {
   private static final int COST_PER_ECI = 3;
   private final int[] bytes;
   private final int fnc1;

   public MinimalECIInput(String stringToEncode, Charset priorityCharset, int fnc1) {
      this.fnc1 = fnc1;
      ECIEncoderSet encoderSet = new ECIEncoderSet(stringToEncode, priorityCharset, fnc1);
      if (encoderSet.length() == 1) {
         this.bytes = new int[stringToEncode.length()];

         for (int i = 0; i < this.bytes.length; i++) {
            char c = stringToEncode.charAt(i);
            this.bytes[i] = c == fnc1 ? 1000 : c;
         }
      } else {
         this.bytes = encodeMinimally(stringToEncode, encoderSet, fnc1);
      }
   }

   public int getFNC1Character() {
      return this.fnc1;
   }

   @Override
   public int length() {
      return this.bytes.length;
   }

   @Override
   public boolean haveNCharacters(int index, int n) {
      if (index + n - 1 >= this.bytes.length) {
         return false;
      } else {
         for (int i = 0; i < n; i++) {
            if (this.isECI(index + i)) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public char charAt(int index) {
      if (index < 0 || index >= this.length()) {
         throw new IndexOutOfBoundsException("" + index);
      } else if (this.isECI(index)) {
         throw new IllegalArgumentException("value at " + index + " is not a character but an ECI");
      } else {
         return this.isFNC1(index) ? (char)this.fnc1 : (char)this.bytes[index];
      }
   }

   @Override
   public CharSequence subSequence(int start, int end) {
      if (start >= 0 && start <= end && end <= this.length()) {
         StringBuilder result = new StringBuilder();

         for (int i = start; i < end; i++) {
            if (this.isECI(i)) {
               throw new IllegalArgumentException("value at " + i + " is not a character but an ECI");
            }

            result.append(this.charAt(i));
         }

         return result;
      } else {
         throw new IndexOutOfBoundsException("" + start);
      }
   }

   @Override
   public boolean isECI(int index) {
      if (index >= 0 && index < this.length()) {
         return this.bytes[index] > 255 && this.bytes[index] <= 999;
      } else {
         throw new IndexOutOfBoundsException("" + index);
      }
   }

   public boolean isFNC1(int index) {
      if (index >= 0 && index < this.length()) {
         return this.bytes[index] == 1000;
      } else {
         throw new IndexOutOfBoundsException("" + index);
      }
   }

   @Override
   public int getECIValue(int index) {
      if (index < 0 || index >= this.length()) {
         throw new IndexOutOfBoundsException("" + index);
      } else if (!this.isECI(index)) {
         throw new IllegalArgumentException("value at " + index + " is not an ECI but a character");
      } else {
         return this.bytes[index] - 256;
      }
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < this.length(); i++) {
         if (i > 0) {
            result.append(", ");
         }

         if (this.isECI(i)) {
            result.append("ECI(");
            result.append(this.getECIValue(i));
            result.append(')');
         } else if (this.charAt(i) < 128) {
            result.append('\'');
            result.append(this.charAt(i));
            result.append('\'');
         } else {
            result.append(this.charAt(i));
         }
      }

      return result.toString();
   }

   static void addEdge(MinimalECIInput.InputEdge[][] edges, int to, MinimalECIInput.InputEdge edge) {
      if (edges[to][edge.encoderIndex] == null || edges[to][edge.encoderIndex].cachedTotalSize > edge.cachedTotalSize) {
         edges[to][edge.encoderIndex] = edge;
      }
   }

   static void addEdges(
      String stringToEncode, ECIEncoderSet encoderSet, MinimalECIInput.InputEdge[][] edges, int from, MinimalECIInput.InputEdge previous, int fnc1
   ) {
      char ch = stringToEncode.charAt(from);
      int start = 0;
      int end = encoderSet.length();
      if (encoderSet.getPriorityEncoderIndex() >= 0 && (ch == fnc1 || encoderSet.canEncode(ch, encoderSet.getPriorityEncoderIndex()))) {
         start = encoderSet.getPriorityEncoderIndex();
         end = start + 1;
      }

      for (int i = start; i < end; i++) {
         if (ch == fnc1 || encoderSet.canEncode(ch, i)) {
            addEdge(edges, from + 1, new MinimalECIInput.InputEdge(ch, encoderSet, i, previous, fnc1));
         }
      }
   }

   static int[] encodeMinimally(String stringToEncode, ECIEncoderSet encoderSet, int fnc1) {
      int inputLength = stringToEncode.length();
      MinimalECIInput.InputEdge[][] edges = new MinimalECIInput.InputEdge[inputLength + 1][encoderSet.length()];
      addEdges(stringToEncode, encoderSet, edges, 0, null, fnc1);

      for (int i = 1; i <= inputLength; i++) {
         for (int j = 0; j < encoderSet.length(); j++) {
            if (edges[i][j] != null && i < inputLength) {
               addEdges(stringToEncode, encoderSet, edges, i, edges[i][j], fnc1);
            }
         }

         for (int jx = 0; jx < encoderSet.length(); jx++) {
            edges[i - 1][jx] = null;
         }
      }

      int minimalJ = -1;
      int minimalSize = Integer.MAX_VALUE;

      for (int jx = 0; jx < encoderSet.length(); jx++) {
         if (edges[inputLength][jx] != null) {
            MinimalECIInput.InputEdge edge = edges[inputLength][jx];
            if (edge.cachedTotalSize < minimalSize) {
               minimalSize = edge.cachedTotalSize;
               minimalJ = jx;
            }
         }
      }

      if (minimalJ < 0) {
         throw new IllegalStateException("Failed to encode \"" + stringToEncode + "\"");
      } else {
         List<Integer> intsAL = new ArrayList<>();

         for (MinimalECIInput.InputEdge current = edges[inputLength][minimalJ]; current != null; current = current.previous) {
            if (current.isFNC1()) {
               intsAL.add(0, 1000);
            } else {
               byte[] bytes = encoderSet.encode(current.c, current.encoderIndex);

               for (int i = bytes.length - 1; i >= 0; i--) {
                  intsAL.add(0, bytes[i] & 255);
               }
            }

            int previousEncoderIndex = current.previous == null ? 0 : current.previous.encoderIndex;
            if (previousEncoderIndex != current.encoderIndex) {
               intsAL.add(0, 256 + encoderSet.getECIValue(current.encoderIndex));
            }
         }

         int[] ints = new int[intsAL.size()];

         for (int i = 0; i < ints.length; i++) {
            ints[i] = intsAL.get(i);
         }

         return ints;
      }
   }

   private static final class InputEdge {
      private final char c;
      private final int encoderIndex;
      private final MinimalECIInput.InputEdge previous;
      private final int cachedTotalSize;

      private InputEdge(char c, ECIEncoderSet encoderSet, int encoderIndex, MinimalECIInput.InputEdge previous, int fnc1) {
         this.c = c == fnc1 ? 1000 : c;
         this.encoderIndex = encoderIndex;
         this.previous = previous;
         int size = this.c == 1000 ? 1 : encoderSet.encode(c, encoderIndex).length;
         int previousEncoderIndex = previous == null ? 0 : previous.encoderIndex;
         if (previousEncoderIndex != encoderIndex) {
            size += 3;
         }

         if (previous != null) {
            size += previous.cachedTotalSize;
         }

         this.cachedTotalSize = size;
      }

      boolean isFNC1() {
         return this.c == 1000;
      }
   }
}
