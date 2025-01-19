package com.google.zxing.client.result;

import com.google.zxing.Result;
import java.util.Map;
import java.util.regex.Pattern;

public final class EmailAddressResultParser extends ResultParser {
   private static final Pattern COMMA = Pattern.compile(",");

   public EmailAddressParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      if (rawText.startsWith("mailto:") || rawText.startsWith("MAILTO:")) {
         String hostEmail = rawText.substring(7);
         int queryStart = hostEmail.indexOf(63);
         if (queryStart >= 0) {
            hostEmail = hostEmail.substring(0, queryStart);
         }

         try {
            hostEmail = urlDecode(hostEmail);
         } catch (IllegalArgumentException var13) {
            return null;
         }

         String[] tos = null;
         if (!hostEmail.isEmpty()) {
            tos = COMMA.split(hostEmail);
         }

         Map<String, String> nameValues = parseNameValuePairs(rawText);
         String[] ccs = null;
         String[] bccs = null;
         String subject = null;
         String body = null;
         if (nameValues != null) {
            if (tos == null) {
               String tosString = nameValues.get("to");
               if (tosString != null) {
                  tos = COMMA.split(tosString);
               }
            }

            String ccString = nameValues.get("cc");
            if (ccString != null) {
               ccs = COMMA.split(ccString);
            }

            String bccString = nameValues.get("bcc");
            if (bccString != null) {
               bccs = COMMA.split(bccString);
            }

            subject = nameValues.get("subject");
            body = nameValues.get("body");
         }

         return new EmailAddressParsedResult(tos, ccs, bccs, subject, body);
      } else {
         return !EmailDoCoMoResultParser.isBasicallyValidEmailAddress(rawText) ? null : new EmailAddressParsedResult(rawText);
      }
   }
}
