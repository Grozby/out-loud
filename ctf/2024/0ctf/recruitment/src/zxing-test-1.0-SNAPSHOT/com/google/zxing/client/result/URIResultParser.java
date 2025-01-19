package com.google.zxing.client.result;

import com.google.zxing.Result;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URIResultParser extends ResultParser {
   private static final Pattern ALLOWED_URI_CHARS_PATTERN = Pattern.compile("[-._~:/?#\\[\\]@!$&'()*+,;=%A-Za-z0-9]+");
   private static final Pattern USER_IN_HOST = Pattern.compile(":/*([^/@]+)@[^/]+");
   private static final Pattern URL_WITH_PROTOCOL_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+-.]+:");
   private static final Pattern URL_WITHOUT_PROTOCOL_PATTERN = Pattern.compile("([a-zA-Z0-9\\-]+\\.){1,6}[a-zA-Z]{2,}(:\\d{1,5})?(/|\\?|$)");

   public URIParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      if (!rawText.startsWith("URL:") && !rawText.startsWith("URI:")) {
         rawText = rawText.trim();
         return isBasicallyValidURI(rawText) && !isPossiblyMaliciousURI(rawText) ? new URIParsedResult(rawText, null) : null;
      } else {
         return new URIParsedResult(rawText.substring(4).trim(), null);
      }
   }

   static boolean isPossiblyMaliciousURI(String uri) {
      return !ALLOWED_URI_CHARS_PATTERN.matcher(uri).matches() || USER_IN_HOST.matcher(uri).find();
   }

   static boolean isBasicallyValidURI(String uri) {
      if (uri.contains(" ")) {
         return false;
      } else {
         Matcher m = URL_WITH_PROTOCOL_PATTERN.matcher(uri);
         if (m.find() && m.start() == 0) {
            return true;
         } else {
            m = URL_WITHOUT_PROTOCOL_PATTERN.matcher(uri);
            return m.find() && m.start() == 0;
         }
      }
   }
}
