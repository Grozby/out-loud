package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

final class JSONPathTypedMultiIndexes extends JSONPathTypedMulti {
   final JSONPath prefix;
   final JSONPath[] indexPaths;
   final int[] indexes;
   final int maxIndex;
   final boolean duplicate;

   JSONPathTypedMultiIndexes(
      JSONPath[] paths, JSONPath prefix, JSONPath[] indexPaths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features
   ) {
      super(paths, types, formats, pathFeatures, zoneId, features);
      this.prefix = prefix;
      this.indexPaths = indexPaths;
      int[] indexes = new int[paths.length];

      for (int i = 0; i < indexPaths.length; i++) {
         JSONPathSingleIndex indexPath = (JSONPathSingleIndex)indexPaths[i];
         indexes[i] = indexPath.index;
      }

      this.indexes = indexes;
      boolean duplicate = false;
      int maxIndex = -1;

      for (int i = 0; i < indexes.length; i++) {
         int index = indexes[i];
         if (i == 0) {
            maxIndex = index;
         } else {
            maxIndex = Math.max(maxIndex, index);
         }

         for (int j = 0; j < indexes.length && !duplicate; j++) {
            if (j != i && index == indexes[j]) {
               duplicate = true;
               break;
            }
         }
      }

      this.duplicate = duplicate;
      this.maxIndex = maxIndex;
   }

   @Override
   public Object eval(Object root) {
      Object[] array = new Object[this.paths.length];
      Object object = root;
      if (this.prefix != null) {
         object = this.prefix.eval(root);
      }

      if (object == null) {
         return array;
      } else {
         if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < this.indexes.length; i++) {
               int index = this.indexes[i];
               Object result = index < list.size() ? list.get(index) : null;
               Type type = this.types[i];

               try {
                  if (result != null && result.getClass() != type) {
                     if (type == Long.class) {
                        result = TypeUtils.toLong(result);
                     } else if (type == BigDecimal.class) {
                        result = TypeUtils.toBigDecimal(result);
                     } else if (type == String[].class) {
                        result = TypeUtils.toStringArray(result);
                     } else {
                        result = TypeUtils.cast(result, type);
                     }
                  }

                  array[i] = result;
               } catch (Exception var10) {
                  if (!this.ignoreError(i)) {
                     throw new JSONException("jsonpath eval path, path : " + this.paths[i] + ", msg : " + var10.getMessage(), var10);
                  }
               }
            }
         } else {
            for (int i = 0; i < this.paths.length; i++) {
               JSONPath jsonPath = this.indexPaths[i];
               Type type = this.types[i];

               try {
                  Object result = jsonPath.eval(object);
                  if (result != null && result.getClass() != type) {
                     if (type == Long.class) {
                        result = TypeUtils.toLong(result);
                     } else if (type == BigDecimal.class) {
                        result = TypeUtils.toBigDecimal(result);
                     } else if (type == String[].class) {
                        result = TypeUtils.toStringArray(result);
                     } else {
                        result = TypeUtils.cast(result, type);
                     }
                  }

                  array[i] = result;
               } catch (Exception var11) {
                  if (!this.ignoreError(i)) {
                     throw new JSONException("jsonpath eval path, path : " + this.paths[i] + ", msg : " + var11.getMessage(), var11);
                  }
               }
            }
         }

         return array;
      }
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.nextIfNull()) {
         return new Object[this.indexes.length];
      } else {
         if (this.prefix instanceof JSONPathSingleName) {
            JSONPathSingleName prefixName = (JSONPathSingleName)this.prefix;
            long prefixNameHash = prefixName.nameHashCode;
            if (!jsonReader.nextIfObjectStart()) {
               throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
            }

            while (!jsonReader.nextIfObjectEnd()) {
               long nameHashCode = jsonReader.readFieldNameHashCode();
               boolean match = nameHashCode == prefixNameHash;
               if (match) {
                  break;
               }

               jsonReader.skipValue();
            }

            if (jsonReader.nextIfNull()) {
               return new Object[this.indexes.length];
            }
         } else if (this.prefix instanceof JSONPathSingleIndex) {
            int index = ((JSONPathSingleIndex)this.prefix).index;
            int max = jsonReader.startArray();

            for (int i = 0; i < index && i < max; i++) {
               jsonReader.skipValue();
            }

            if (jsonReader.nextIfNull()) {
               return null;
            }
         } else if (this.prefix != null) {
            Object object = jsonReader.readAny();
            return this.eval(object);
         }

         int max = jsonReader.startArray();
         Object[] array = new Object[this.indexes.length];

         for (int i = 0; i <= this.maxIndex && i < max && (jsonReader.jsonb || !jsonReader.nextIfArrayEnd()); i++) {
            Integer index = null;

            for (int j = 0; j < this.indexes.length; j++) {
               if (this.indexes[j] == i) {
                  index = j;
                  break;
               }
            }

            if (index == null) {
               jsonReader.skipValue();
            } else {
               Type type = this.types[index];

               Object value;
               try {
                  value = jsonReader.read(type);
               } catch (Exception var11) {
                  if (!this.ignoreError(index)) {
                     throw var11;
                  }

                  value = null;
               }

               array[index] = value;
               if (this.duplicate) {
                  for (int jx = index + 1; jx < this.indexes.length; jx++) {
                     if (this.indexes[jx] == i) {
                        Type typeJ = this.types[jx];
                        Object valueJ;
                        if (typeJ == type) {
                           valueJ = value;
                        } else {
                           valueJ = TypeUtils.cast(value, typeJ);
                        }

                        array[jx] = valueJ;
                     }
                  }
               }
            }
         }

         return array;
      }
   }
}
