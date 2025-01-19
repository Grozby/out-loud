package com.alibaba.fastjson2;

import java.util.List;

final class JSONPathSingleIndex extends JSONPathSingle {
   final JSONPathSegmentIndex segment;
   final int index;

   public JSONPathSingleIndex(String path, JSONPathSegmentIndex segment, JSONPath.Feature... features) {
      super(segment, path, features);
      this.segment = segment;
      this.index = segment.index;
   }

   @Override
   public Object eval(Object object) {
      if (object == null) {
         return null;
      } else if (object instanceof List) {
         Object value = null;
         List list = (List)object;
         if (this.index < list.size()) {
            value = list.get(this.index);
         }

         return value;
      } else {
         JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
         context.root = object;
         this.segment.eval(context);
         return context.value;
      }
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         int max = jsonReader.startArray();
         if (jsonReader.jsonb && this.index >= max) {
            return null;
         } else if (!jsonReader.jsonb && jsonReader.nextIfArrayEnd()) {
            return null;
         } else {
            for (int i = 0; i < this.index && i < max; i++) {
               jsonReader.skipValue();
               if (!jsonReader.jsonb && jsonReader.nextIfArrayEnd()) {
                  return null;
               }
            }

            return jsonReader.readAny();
         }
      }
   }
}
