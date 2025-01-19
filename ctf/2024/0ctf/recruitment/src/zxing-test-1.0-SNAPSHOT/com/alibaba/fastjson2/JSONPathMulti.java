package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

final class JSONPathMulti extends JSONPath {
   final List<JSONPathSegment> segments;
   final boolean ref;
   final boolean extractSupport;

   JSONPathMulti(String path, List<JSONPathSegment> segments, JSONPath.Feature... features) {
      super(path, features);
      this.segments = segments;
      boolean extractSupport = true;
      boolean ref = true;
      int size = segments.size();

      for (int i = 0; i < size - 1; i++) {
         JSONPathSegment segment = segments.get(i);
         if (segment instanceof JSONPathSegmentIndex) {
            if (((JSONPathSegmentIndex)segment).index < 0) {
               extractSupport = false;
            }
         } else if (!(segment instanceof JSONPathSegmentName)) {
            ref = false;
            if (i > 0) {
               JSONPathSegment prev = segments.get(i - 1);
               if (prev instanceof JSONPathSegment.CycleNameSegment
                  && ((JSONPathSegment.CycleNameSegment)prev).shouldRecursive()
                  && segment instanceof JSONPathFilter.NameFilter) {
                  ((JSONPathFilter.NameFilter)segment).excludeArray();
               }
            }
            break;
         }
      }

      this.extractSupport = extractSupport;
      this.ref = ref;
   }

   @Override
   public boolean remove(Object root) {
      JSONPath.Context context = null;
      int size = this.segments.size();
      if (size == 0) {
         return false;
      } else {
         for (int i = 0; i < size; i++) {
            JSONPathSegment segment = this.segments.get(i);
            JSONPathSegment nextSegment = null;
            int nextIndex = i + 1;
            if (nextIndex < size) {
               nextSegment = this.segments.get(nextIndex);
            }

            context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
            if (i == 0) {
               context.root = root;
            }

            if (i == size - 1) {
               return segment.remove(context);
            }

            segment.eval(context);
            if (context.value == null) {
               return false;
            }
         }

         return false;
      }
   }

   @Override
   public boolean contains(Object root) {
      JSONPath.Context context = null;
      int size = this.segments.size();
      if (size == 0) {
         return root != null;
      } else {
         for (int i = 0; i < size; i++) {
            JSONPathSegment segment = this.segments.get(i);
            JSONPathSegment nextSegment = null;
            int nextIndex = i + 1;
            if (nextIndex < size) {
               nextSegment = this.segments.get(nextIndex);
            }

            context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
            if (i == 0) {
               context.root = root;
            }

            if (i == size - 1) {
               return segment.contains(context);
            }

            segment.eval(context);
         }

         return false;
      }
   }

   @Override
   public boolean endsWithFilter() {
      int size = this.segments.size();
      JSONPathSegment last = this.segments.get(size - 1);
      return last instanceof JSONPathFilter;
   }

   @Override
   public JSONPath getParent() {
      int size = this.segments.size();
      if (size == 0) {
         return null;
      } else if (size == 1) {
         return JSONPath.RootPath.INSTANCE;
      } else if (size == 2) {
         return JSONPathSingle.of(this.segments.get(0));
      } else {
         StringBuilder buf = new StringBuilder();
         buf.append('$');
         List<JSONPathSegment> parentSegments = new ArrayList<>(size - 1);
         int i = 0;

         for (int end = size - 1; i < end; i++) {
            JSONPathSegment segment = this.segments.get(i);
            parentSegments.add(segment);
            boolean array = segment instanceof JSONPathSegmentIndex
               || segment instanceof JSONPathSegment.MultiIndexSegment
               || segment instanceof JSONPathFilter;
            if (!array) {
               buf.append('.');
            }

            buf.append(segment);
         }

         String parentPath = buf.toString();
         if (size == 3) {
            new JSONPathTwoSegment(parentPath, this.segments.get(0), this.segments.get(1));
         }

         return new JSONPathMulti(parentPath, parentSegments);
      }
   }

   @Override
   public boolean isRef() {
      return this.ref;
   }

   @Override
   public Object eval(Object root) {
      JSONPath.Context context = null;
      int size = this.segments.size();
      if (size == 0) {
         return root;
      } else {
         for (int i = 0; i < size; i++) {
            JSONPathSegment segment = this.segments.get(i);
            JSONPathSegment nextSegment = null;
            int nextIndex = i + 1;
            if (nextIndex < size) {
               nextSegment = this.segments.get(nextIndex);
            }

            context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
            if (i == 0) {
               context.root = root;
            }

            if (i > 0) {
               JSONPathSegment prev = this.segments.get(i - 1);
               if (prev instanceof JSONPathSegment.CycleNameSegment
                  && ((JSONPathSegment.CycleNameSegment)prev).shouldRecursive()
                  && segment instanceof JSONPathFilter.NameFilter) {
                  ((JSONPathFilter.NameFilter)segment).excludeArray();
               }
            }

            segment.eval(context);
         }

         Object contextValue = context.value;
         if ((context.path.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
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
      JSONPath.Context context = null;
      int size = this.segments.size();

      for (int i = 0; i < size - 1; i++) {
         JSONPathSegment segment = this.segments.get(i);
         JSONPathSegment nextSegment = null;
         int nextIndex = i + 1;
         if (nextIndex < size) {
            nextSegment = this.segments.get(nextIndex);
         }

         context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
         if (i == 0) {
            context.root = root;
         }

         segment.eval(context);
         if (context.value == null && nextSegment != null) {
            Object parentObject;
            if (i == 0) {
               parentObject = root;
            } else {
               parentObject = context.parent.value;
            }

            Object emptyValue;
            if (nextSegment instanceof JSONPathSegmentIndex) {
               emptyValue = new JSONArray();
            } else {
               if (!(nextSegment instanceof JSONPathSegmentName)) {
                  return;
               }

               JSONPath.Context parentContext = context.parent;
               if (parentContext == null) {
                  parentContext = context;
               }

               Object bean;
               if (parentContext.parent == null) {
                  bean = parentContext.root;
               } else {
                  bean = parentContext.parent.value;
               }

               Class itemClass = null;
               if (bean != null && parentContext.current instanceof JSONPathSegmentName) {
                  String name = ((JSONPathSegmentName)parentContext.current).name;
                  JSONReader.Context readerContext = this.getReaderContext();
                  ObjectReader<?> objectReader = readerContext.getObjectReader(bean.getClass());
                  FieldReader fieldReader = objectReader.getFieldReader(name);
                  if (fieldReader != null) {
                     itemClass = fieldReader.getItemClass();
                  }
               }

               if (itemClass != null) {
                  ObjectReader<?> itemReader = this.readerContext.getObjectReader(itemClass);
                  emptyValue = itemReader.createInstance(0L);
               } else {
                  emptyValue = new JSONObject();
               }
            }

            context.value = emptyValue;
            if (parentObject instanceof Map && segment instanceof JSONPathSegmentName) {
               ((Map)parentObject).put(((JSONPathSegmentName)segment).name, emptyValue);
            } else if (parentObject instanceof List && segment instanceof JSONPathSegmentIndex) {
               List list = (List)parentObject;
               int index = ((JSONPathSegmentIndex)segment).index;
               if (index < list.size()) {
                  list.set(index, emptyValue);
               } else {
                  int delta = index - list.size();

                  for (int j = 0; j < delta; j++) {
                     list.add(null);
                  }

                  list.add(emptyValue);
               }
            } else if (parentObject != null) {
               Class<?> parentObjectClass = parentObject.getClass();
               JSONReader.Context readerContext = this.getReaderContext();
               ObjectReader<?> objectReader = readerContext.getObjectReader(parentObjectClass);
               if (segment instanceof JSONPathSegmentName) {
                  FieldReader fieldReader = objectReader.getFieldReader(((JSONPathSegmentName)segment).nameHashCode);
                  if (fieldReader != null) {
                     ObjectReader fieldObjectReader = fieldReader.getObjectReader(readerContext);
                     Object fieldValue = fieldObjectReader.createInstance();
                     fieldReader.accept(parentObject, fieldValue);
                     context.value = fieldValue;
                  }
               }
            }
         }
      }

      context = new JSONPath.Context(this, context, this.segments.get(0), null, 0L);
      context.root = root;
      JSONPathSegment segmentx = this.segments.get(size - 1);
      segmentx.set(context, value);
   }

   @Override
   public void set(Object root, Object value, JSONReader.Feature... readerFeatures) {
      long features = 0L;

      for (JSONReader.Feature feature : readerFeatures) {
         features |= feature.mask;
      }

      JSONPath.Context context = null;
      int size = this.segments.size();

      for (int i = 0; i < size - 1; i++) {
         JSONPathSegment segment = this.segments.get(i);
         JSONPathSegment nextSegment = null;
         int nextIndex = i + 1;
         if (nextIndex < size) {
            nextSegment = this.segments.get(nextIndex);
         }

         context = new JSONPath.Context(this, context, segment, nextSegment, features);
         if (i == 0) {
            context.root = root;
         }

         segment.eval(context);
      }

      context = new JSONPath.Context(this, context, this.segments.get(0), null, features);
      context.root = root;
      JSONPathSegment segmentx = this.segments.get(size - 1);
      segmentx.set(context, value);
   }

   @Override
   public void setCallback(Object root, BiFunction callback) {
      JSONPath.Context context = null;
      int size = this.segments.size();

      for (int i = 0; i < size - 1; i++) {
         JSONPathSegment segment = this.segments.get(i);
         JSONPathSegment nextSegment = null;
         int nextIndex = i + 1;
         if (nextIndex < size) {
            nextSegment = this.segments.get(nextIndex);
         }

         context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
         if (i == 0) {
            context.root = root;
         }

         segment.eval(context);
      }

      context = new JSONPath.Context(this, context, this.segments.get(0), null, 0L);
      context.root = root;
      JSONPathSegment segmentx = this.segments.get(size - 1);
      segmentx.setCallback(context, callback);
   }

   @Override
   public void setInt(Object rootObject, int value) {
      this.set(rootObject, value);
   }

   @Override
   public void setLong(Object rootObject, long value) {
      this.set(rootObject, value);
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader == null) {
         return null;
      } else {
         int size = this.segments.size();
         if (size == 0) {
            return null;
         } else if (!this.extractSupport) {
            Object root = jsonReader.readAny();
            return this.eval(root);
         } else {
            boolean eval = false;
            JSONPath.Context context = null;

            for (int i = 0; i < size; i++) {
               JSONPathSegment segment = this.segments.get(i);
               JSONPathSegment nextSegment = null;
               int nextIndex = i + 1;
               if (nextIndex < size) {
                  nextSegment = this.segments.get(nextIndex);
               }

               context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
               if (eval) {
                  segment.eval(context);
               } else {
                  segment.accept(jsonReader, context);
               }

               if (context.eval) {
                  eval = true;
                  if (context.value == null) {
                     break;
                  }
               }
            }

            Object value = context.value;
            if (value instanceof JSONPath.Sequence) {
               value = ((JSONPath.Sequence)value).values;
            }

            if ((this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
               if (value == null) {
                  value = new JSONArray();
               } else if (!(value instanceof List)) {
                  value = JSONArray.of(value);
               }
            }

            return value;
         }
      }
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      int size = this.segments.size();
      if (size == 0) {
         return null;
      } else {
         boolean eval = false;
         JSONPath.Context context = null;

         for (int i = 0; i < size; i++) {
            JSONPathSegment segment = this.segments.get(i);
            JSONPathSegment nextSegment = null;
            int nextIndex = i + 1;
            if (nextIndex < size) {
               nextSegment = this.segments.get(nextIndex);
            }

            context = new JSONPath.Context(this, context, segment, nextSegment, 0L);
            if (eval) {
               segment.eval(context);
            } else {
               segment.accept(jsonReader, context);
            }

            if (context.eval) {
               eval = true;
               if (context.value == null) {
                  break;
               }
            }
         }

         return JSON.toJSONString(context.value);
      }
   }
}
