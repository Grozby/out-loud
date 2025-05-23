package com.google.zxing.client.result;

import com.google.zxing.Result;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class SMSMMSResultParser extends ResultParser {
   public SMSParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      if (!rawText.startsWith("sms:") && !rawText.startsWith("SMS:") && !rawText.startsWith("mms:") && !rawText.startsWith("MMS:")) {
         return null;
      } else {
         Map<String, String> nameValuePairs = parseNameValuePairs(rawText);
         String subject = null;
         String body = null;
         boolean querySyntax = false;
         if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
            subject = nameValuePairs.get("subject");
            body = nameValuePairs.get("body");
            querySyntax = true;
         }

         int queryStart = rawText.indexOf(63, 4);
         String smsURIWithoutQuery;
         if (queryStart >= 0 && querySyntax) {
            smsURIWithoutQuery = rawText.substring(4, queryStart);
         } else {
            smsURIWithoutQuery = rawText.substring(4);
         }

         int lastComma = -1;
         List<String> numbers = new ArrayList<>(1);
         List<String> vias = new ArrayList<>(1);

         int comma;
         while ((comma = smsURIWithoutQuery.indexOf(44, lastComma + 1)) > lastComma) {
            String numberPart = smsURIWithoutQuery.substring(lastComma + 1, comma);
            addNumberVia(numbers, vias, numberPart);
            lastComma = comma;
         }

         addNumberVia(numbers, vias, smsURIWithoutQuery.substring(lastComma + 1));
         return new SMSParsedResult(numbers.toArray(EMPTY_STR_ARRAY), vias.toArray(EMPTY_STR_ARRAY), subject, body);
      }
   }

   private static void addNumberVia(Collection<String> numbers, Collection<String> vias, String numberPart) {
      int numberEnd = numberPart.indexOf(59);
      if (numberEnd < 0) {
         numbers.add(numberPart);
         vias.add(null);
      } else {
         numbers.add(numberPart.substring(0, numberEnd));
         String maybeVia = numberPart.substring(numberEnd + 1);
         String via;
         if (maybeVia.startsWith("via=")) {
            via = maybeVia.substring(4);
         } else {
            via = null;
         }

         vias.add(via);
      }
   }
}
