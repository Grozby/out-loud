package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

class JSONPathTwoSegment extends JSONPath {
   final JSONPathSegment first;
   final JSONPathSegment second;
   final boolean ref;
   final boolean extractSupport;

   JSONPathTwoSegment(String path, JSONPathSegment first, JSONPathSegment second, JSONPath.Feature... features) {
      super(path, features);
      this.first = first;
      this.second = second;
      this.ref = (first instanceof JSONPathSegmentIndex || first instanceof JSONPathSegmentName)
         && (second instanceof JSONPathSegmentIndex || second instanceof JSONPathSegmentName);
      boolean extractSupport = true;
      if (first instanceof JSONPathSegment.EvalSegment) {
         extractSupport = false;
      } else if (first instanceof JSONPathSegmentIndex && ((JSONPathSegmentIndex)first).index < 0) {
         extractSupport = false;
      } else if (second instanceof JSONPathSegment.EvalSegment) {
         extractSupport = false;
      } else if (second instanceof JSONPathSegmentIndex && ((JSONPathSegmentIndex)second).index < 0) {
         extractSupport = false;
      }

      this.extractSupport = extractSupport;
      if (first instanceof JSONPathSegment.CycleNameSegment
         && ((JSONPathSegment.CycleNameSegment)first).shouldRecursive()
         && second instanceof JSONPathFilter.NameFilter) {
         ((JSONPathFilter.NameFilter)second).excludeArray();
      }
   }

   @Override
   public boolean endsWithFilter() {
      return this.second instanceof JSONPathFilter;
   }

   @Override
   public JSONPath getParent() {
      return JSONPathSingle.of(this.first);
   }

   @Override
   public boolean remove(Object root) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value == null) {
         return false;
      } else {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         return this.second.remove(context1);
      }
   }

   @Override
   public boolean contains(Object root) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value == null) {
         return false;
      } else {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         return this.second.contains(context1);
      }
   }

   @Override
   public boolean isRef() {
      return this.ref;
   }

   @Override
   public Object eval(Object root) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value == null) {
         return null;
      } else {
         if (this.first instanceof JSONPathSegment.CycleNameSegment
            && ((JSONPathSegment.CycleNameSegment)this.first).shouldRecursive()
            && this.second instanceof JSONPathFilter.NameFilter) {
            ((JSONPathFilter.NameFilter)this.second).excludeArray();
         }

         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         this.second.eval(context1);
         Object contextValue = context1.value;
         if ((this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
            if (contextValue == null) {
               contextValue = new JSONArray();
            } else if (!(contextValue instanceof List)) {
               contextValue = JSONArray.of(contextValue);
            }
         }

         return contextValue;
      }
   }

   @Override
   public void set(Object root, Object value) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value == null) {
         Object emptyValue;
         if (this.second instanceof JSONPathSegmentIndex) {
            if (this.readerContext != null && this.readerContext.arraySupplier != null) {
               emptyValue = this.readerContext.arraySupplier.get();
            } else {
               emptyValue = new JSONArray();
            }
         } else {
            if (!(this.second instanceof JSONPathSegmentName)) {
               return;
            }

            if (this.readerContext != null && this.readerContext.objectSupplier != null) {
               emptyValue = this.readerContext.objectSupplier.get();
            } else {
               emptyValue = new JSONObject();
            }
         }

         context0.value = emptyValue;
         if (root instanceof Map && this.first instanceof JSONPathSegmentName) {
            ((Map)root).put(((JSONPathSegmentName)this.first).name, emptyValue);
         } else if (root instanceof List && this.first instanceof JSONPathSegmentIndex) {
            ((List)root).set(((JSONPathSegmentIndex)this.first).index, emptyValue);
         } else if (root != null) {
            Class<?> parentObjectClass = root.getClass();
            JSONReader.Context readerContext = this.getReaderContext();
            ObjectReader<?> objectReader = readerContext.getObjectReader(parentObjectClass);
            if (this.first instanceof JSONPathSegmentName) {
               FieldReader fieldReader = objectReader.getFieldReader(((JSONPathSegmentName)this.first).nameHashCode);
               if (fieldReader != null) {
                  ObjectReader fieldObjectReader = fieldReader.getObjectReader(readerContext);
                  Object fieldValue = fieldObjectReader.createInstance();
                  fieldReader.accept(root, fieldValue);
                  context0.value = fieldValue;
               }
            }
         }
      }

      JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
      this.second.set(context1, value);
   }

   @Override
   public void set(Object root, Object value, JSONReader.Feature... readerFeatures) {
      long features = 0L;

      for (JSONReader.Feature feature : readerFeatures) {
         features |= feature.mask;
      }

      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, features);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value != null) {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, features);
         this.second.set(context1, value);
      }
   }

   @Override
   public void setCallback(Object root, BiFunction callback) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value != null) {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         this.second.setCallback(context1, callback);
      }
   }

   @Override
   public void setInt(Object root, int value) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value != null) {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         this.second.setInt(context1, value);
      }
   }

   @Override
   public void setLong(Object root, long value) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      context0.root = root;
      this.first.eval(context0);
      if (context0.value != null) {
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         this.second.setLong(context1, value);
      }
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader == null) {
         return null;
      } else if (!this.extractSupport) {
         Object root = jsonReader.readAny();
         return this.eval(root);
      } else {
         JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
         this.first.accept(jsonReader, context0);
         JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
         if (context0.eval) {
            this.second.eval(context1);
         } else {
            this.second.accept(jsonReader, context1);
         }

         Object contextValue = context1.value;
         if ((this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
            if (contextValue == null) {
               contextValue = new JSONArray();
            } else if (!(contextValue instanceof List)) {
               contextValue = JSONArray.of(contextValue);
            }
         }

         if (contextValue instanceof JSONPath.Sequence) {
            contextValue = ((JSONPath.Sequence)contextValue).values;
         }

         return contextValue;
      }
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      JSONPath.Context context0 = new JSONPath.Context(this, null, this.first, this.second, 0L);
      this.first.accept(jsonReader, context0);
      JSONPath.Context context1 = new JSONPath.Context(this, context0, this.second, null, 0L);
      this.second.accept(jsonReader, context1);
      return JSON.toJSONString(context1.value);
   }
}
