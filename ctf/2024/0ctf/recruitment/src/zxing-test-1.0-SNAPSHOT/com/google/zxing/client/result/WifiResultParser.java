package com.google.zxing.client.result;

import com.google.zxing.Result;

public final class WifiResultParser extends ResultParser {
   public WifiParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      if (!rawText.startsWith("WIFI:")) {
         return null;
      } else {
         rawText = rawText.substring("WIFI:".length());
         String ssid = matchSinglePrefixedField("S:", rawText, ';', false);
         if (ssid != null && !ssid.isEmpty()) {
            String pass = matchSinglePrefixedField("P:", rawText, ';', false);
            String type = matchSinglePrefixedField("T:", rawText, ';', false);
            if (type == null) {
               type = "nopass";
            }

            boolean hidden = false;
            String phase2Method = matchSinglePrefixedField("PH2:", rawText, ';', false);
            String hValue = matchSinglePrefixedField("H:", rawText, ';', false);
            if (hValue != null) {
               if (phase2Method == null && !"true".equalsIgnoreCase(hValue) && !"false".equalsIgnoreCase(hValue)) {
                  phase2Method = hValue;
               } else {
                  hidden = Boolean.parseBoolean(hValue);
               }
            }

            String identity = matchSinglePrefixedField("I:", rawText, ';', false);
            String anonymousIdentity = matchSinglePrefixedField("A:", rawText, ';', false);
            String eapMethod = matchSinglePrefixedField("E:", rawText, ';', false);
            return new WifiParsedResult(type, ssid, pass, hidden, identity, anonymousIdentity, eapMethod, phase2Method);
         } else {
            return null;
         }
      }
   }
}
