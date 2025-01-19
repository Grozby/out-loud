package com.alibaba.fastjson2;

import java.lang.reflect.Type;
import java.time.ZoneId;

public class JSONPathTypedMultiNamesPrefixName2 extends JSONPathTypedMultiNames {
   final String prefixName0;
   final long prefixNameHash0;
   final String prefixName1;
   final long prefixNameHash1;

   JSONPathTypedMultiNamesPrefixName2(
      JSONPath[] paths, JSONPath prefix, JSONPath[] namePaths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features
   ) {
      super(paths, prefix, namePaths, types, formats, pathFeatures, zoneId, features);
      JSONPathTwoSegment prefixTwo = (JSONPathTwoSegment)prefix;
      this.prefixName0 = ((JSONPathSegmentName)prefixTwo.first).name;
      this.prefixNameHash0 = ((JSONPathSegmentName)prefixTwo.first).nameHashCode;
      this.prefixName1 = ((JSONPathSegmentName)prefixTwo.second).name;
      this.prefixNameHash1 = ((JSONPathSegmentName)prefixTwo.second).nameHashCode;
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.nextIfNull()) {
         return new Object[this.paths.length];
      } else if (!jsonReader.nextIfObjectStart()) {
         throw error(jsonReader);
      } else {
         while (!jsonReader.nextIfObjectEnd()) {
            if (jsonReader.isEnd()) {
               throw error(jsonReader);
            }

            boolean match = jsonReader.readFieldNameHashCode() == this.prefixNameHash0;
            if (match) {
               if (jsonReader.nextIfNull()) {
                  return new Object[this.paths.length];
               }

               if (!jsonReader.nextIfObjectStart()) {
                  throw error(jsonReader);
               }

               while (!jsonReader.nextIfObjectEnd()) {
                  if (jsonReader.isEnd()) {
                     throw error(jsonReader);
                  }

                  match = jsonReader.readFieldNameHashCode() == this.prefixNameHash1;
                  if (match) {
                     if (jsonReader.nextIfNull()) {
                        return new Object[this.paths.length];
                     }

                     return this.objectReader.readObject(jsonReader);
                  }

                  jsonReader.skipValue();
               }

               return new Object[this.paths.length];
            }

            jsonReader.skipValue();
         }

         return new Object[this.paths.length];
      }
   }

   private static JSONException error(JSONReader jsonReader) {
      return new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
   }
}
