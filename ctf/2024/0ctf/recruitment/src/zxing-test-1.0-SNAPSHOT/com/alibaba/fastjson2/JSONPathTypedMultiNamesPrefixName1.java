package com.alibaba.fastjson2;

import java.lang.reflect.Type;
import java.time.ZoneId;

public class JSONPathTypedMultiNamesPrefixName1 extends JSONPathTypedMultiNames {
   final JSONPathSingleName prefixName;
   final long prefixNameHash;

   JSONPathTypedMultiNamesPrefixName1(
      JSONPath[] paths, JSONPath prefix, JSONPath[] namePaths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features
   ) {
      super(paths, prefix, namePaths, types, formats, pathFeatures, zoneId, features);
      this.prefixName = (JSONPathSingleName)prefix;
      this.prefixNameHash = this.prefixName.nameHashCode;
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.nextIfNull()) {
         return new Object[this.paths.length];
      } else if (!jsonReader.nextIfObjectStart()) {
         throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
      } else {
         while (!jsonReader.nextIfObjectEnd()) {
            if (jsonReader.isEnd()) {
               throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
            }

            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.prefixNameHash;
            if (match) {
               if (jsonReader.nextIfNull()) {
                  return new Object[this.paths.length];
               }

               if (!jsonReader.nextIfObjectStart()) {
                  throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
               }

               return this.objectReader.readObject(jsonReader);
            }

            jsonReader.skipValue();
         }

         return new Object[this.paths.length];
      }
   }
}
