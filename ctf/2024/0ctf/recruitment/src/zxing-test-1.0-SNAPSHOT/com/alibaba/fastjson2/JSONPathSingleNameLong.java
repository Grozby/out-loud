package com.alibaba.fastjson2;

final class JSONPathSingleNameLong extends JSONPathTyped {
   final long nameHashCode;
   final String name;

   public JSONPathSingleNameLong(JSONPathSingleName jsonPath) {
      super(jsonPath, Long.class);
      this.nameHashCode = jsonPath.nameHashCode;
      this.name = jsonPath.name;
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.jsonb) {
         if (jsonReader.isObject()) {
            jsonReader.nextIfObjectStart();

            while (!jsonReader.nextIfObjectEnd()) {
               long nameHashCode = jsonReader.readFieldNameHashCode();
               if (nameHashCode != 0L) {
                  boolean match = nameHashCode == this.nameHashCode;
                  if (match || jsonReader.isObject() || jsonReader.isArray()) {
                     return jsonReader.readInt64();
                  }

                  jsonReader.skipValue();
               }
            }
         }
      } else if (jsonReader.nextIfObjectStart()) {
         while (!jsonReader.nextIfObjectEnd()) {
            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            if (match) {
               return jsonReader.readInt64();
            }

            jsonReader.skipValue();
         }
      }

      return null;
   }
}
