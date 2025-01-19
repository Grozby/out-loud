package com.google.zxing.client.result;

import com.google.zxing.Result;

public final class AddressBookDoCoMoResultParser extends AbstractDoCoMoResultParser {
   public AddressBookParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      if (!rawText.startsWith("MECARD:")) {
         return null;
      } else {
         String[] rawName = matchDoCoMoPrefixedField("N:", rawText);
         if (rawName == null) {
            return null;
         } else {
            String name = parseName(rawName[0]);
            String pronunciation = matchSingleDoCoMoPrefixedField("SOUND:", rawText, true);
            String[] phoneNumbers = matchDoCoMoPrefixedField("TEL:", rawText);
            String[] emails = matchDoCoMoPrefixedField("EMAIL:", rawText);
            String note = matchSingleDoCoMoPrefixedField("NOTE:", rawText, false);
            String[] addresses = matchDoCoMoPrefixedField("ADR:", rawText);
            String birthday = matchSingleDoCoMoPrefixedField("BDAY:", rawText, true);
            if (!isStringOfDigits(birthday, 8)) {
               birthday = null;
            }

            String[] urls = matchDoCoMoPrefixedField("URL:", rawText);
            String org = matchSingleDoCoMoPrefixedField("ORG:", rawText, true);
            return new AddressBookParsedResult(
               maybeWrap(name), null, pronunciation, phoneNumbers, null, emails, null, null, note, addresses, null, org, birthday, null, urls, null
            );
         }
      }
   }

   private static String parseName(String name) {
      int comma = name.indexOf(44);
      return comma >= 0 ? name.substring(comma + 1) + ' ' + name.substring(0, comma) : name;
   }
}
