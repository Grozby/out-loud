package com.google.zxing.client.result;

public abstract class ParsedResult {
   private final ParsedResultType type;

   protected ParsedResult(ParsedResultType type) {
      this.type = type;
   }

   public final ParsedResultType getType() {
      return this.type;
   }

   public abstract String getDisplayResult();

   @Override
   public final String toString() {
      return this.getDisplayResult();
   }

   public static void maybeAppend(String value, StringBuilder result) {
      if (value != null && !value.isEmpty()) {
         if (result.length() > 0) {
            result.append('\n');
         }

         result.append(value);
      }
   }

   public static void maybeAppend(String[] values, StringBuilder result) {
      if (values != null) {
         for (String value : values) {
            maybeAppend(value, result);
         }
      }
   }
}
