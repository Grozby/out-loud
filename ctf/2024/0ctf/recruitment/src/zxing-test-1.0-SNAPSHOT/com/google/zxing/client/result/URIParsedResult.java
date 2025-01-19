package com.google.zxing.client.result;

public final class URIParsedResult extends ParsedResult {
   private final String uri;
   private final String title;

   public URIParsedResult(String uri, String title) {
      super(ParsedResultType.URI);
      this.uri = massageURI(uri);
      this.title = title;
   }

   public String getURI() {
      return this.uri;
   }

   public String getTitle() {
      return this.title;
   }

   @Deprecated
   public boolean isPossiblyMaliciousURI() {
      return URIResultParser.isPossiblyMaliciousURI(this.uri);
   }

   @Override
   public String getDisplayResult() {
      StringBuilder result = new StringBuilder(30);
      maybeAppend(this.title, result);
      maybeAppend(this.uri, result);
      return result.toString();
   }

   private static String massageURI(String uri) {
      uri = uri.trim();
      int protocolEnd = uri.indexOf(58);
      if (protocolEnd < 0 || isColonFollowedByPortNumber(uri, protocolEnd)) {
         uri = "http://" + uri;
      }

      return uri;
   }

   private static boolean isColonFollowedByPortNumber(String uri, int protocolEnd) {
      int start = protocolEnd + 1;
      int nextSlash = uri.indexOf(47, start);
      if (nextSlash < 0) {
         nextSlash = uri.length();
      }

      return ResultParser.isSubstringOfDigits(uri, start, nextSlash - start);
   }
}
