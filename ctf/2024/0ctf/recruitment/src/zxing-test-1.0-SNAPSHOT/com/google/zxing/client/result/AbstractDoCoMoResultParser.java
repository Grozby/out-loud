package com.google.zxing.client.result;

abstract class AbstractDoCoMoResultParser extends ResultParser {
   static String[] matchDoCoMoPrefixedField(String prefix, String rawText) {
      return matchPrefixedField(prefix, rawText, ';', true);
   }

   static String matchSingleDoCoMoPrefixedField(String prefix, String rawText, boolean trim) {
      return matchSinglePrefixedField(prefix, rawText, ';', trim);
   }
}
