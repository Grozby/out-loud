package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class BarcodeValue {
   private final Map<Integer, Integer> values = new HashMap<>();

   void setValue(int value) {
      Integer confidence = this.values.get(value);
      if (confidence == null) {
         confidence = 0;
      }

      confidence = confidence + 1;
      this.values.put(value, confidence);
   }

   int[] getValue() {
      int maxConfidence = -1;
      Collection<Integer> result = new ArrayList<>();

      for (Entry<Integer, Integer> entry : this.values.entrySet()) {
         if (entry.getValue() > maxConfidence) {
            maxConfidence = entry.getValue();
            result.clear();
            result.add(entry.getKey());
         } else if (entry.getValue() == maxConfidence) {
            result.add(entry.getKey());
         }
      }

      return PDF417Common.toIntArray(result);
   }

   Integer getConfidence(int value) {
      return this.values.get(value);
   }
}
