package com.alibaba.fastjson2;

import java.lang.reflect.Type;
import java.time.ZoneId;

public class JSONPathTypedMultiNamesPrefixIndex1 extends JSONPathTypedMultiNames {
   final int index;

   JSONPathTypedMultiNamesPrefixIndex1(
      JSONPath[] paths, JSONPathSingleIndex prefix, JSONPath[] namePaths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features
   ) {
      super(paths, prefix, namePaths, types, formats, pathFeatures, zoneId, features);
      this.index = prefix.index;
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.nextIfNull()) {
         return new Object[this.paths.length];
      } else if (!jsonReader.nextIfArrayStart()) {
         throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
      } else {
         for (int i = 0; i < this.index; i++) {
            if (jsonReader.nextIfArrayEnd()) {
               return new Object[this.paths.length];
            }

            if (jsonReader.isEnd()) {
               throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
            }

            jsonReader.skipValue();
         }

         if (jsonReader.nextIfNull()) {
            return new Object[this.paths.length];
         } else {
            return jsonReader.nextIfArrayEnd() ? new Object[this.paths.length] : this.objectReader.readObject(jsonReader);
         }
      }
   }
}
