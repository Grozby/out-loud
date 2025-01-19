package com.alibaba.fastjson2;

final class JSONPathSingleNameString extends JSONPathTyped {
   final long nameHashCode;
   final String name;

   public JSONPathSingleNameString(JSONPathSingleName jsonPath) {
      super(jsonPath, String.class);
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
                     return jsonReader.readString();
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
               return jsonReader.readString();
            }

            jsonReader.skipValue();
         }
      }

      return null;
   }
}
