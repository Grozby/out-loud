package com.alibaba.fastjson2;

import java.util.function.BiFunction;

class JSONPathSingle extends JSONPath {
   final JSONPathSegment segment;
   final boolean ref;
   final boolean extractSupport;

   JSONPathSingle(JSONPathSegment segment, String path, JSONPath.Feature... features) {
      super(path, features);
      this.segment = segment;
      this.ref = segment instanceof JSONPathSegmentIndex || segment instanceof JSONPathSegmentName || segment instanceof JSONPathSegment.SelfSegment;
      boolean extractSupport = true;
      if (segment instanceof JSONPathSegment.EvalSegment) {
         extractSupport = false;
      } else if (segment instanceof JSONPathSegmentIndex && ((JSONPathSegmentIndex)segment).index < 0) {
         extractSupport = false;
      } else if (segment instanceof JSONPathSegment.CycleNameSegment && ((JSONPathSegment.CycleNameSegment)segment).shouldRecursive()) {
         extractSupport = false;
      }

      this.extractSupport = extractSupport;
   }

   @Override
   public boolean remove(Object root) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      return this.segment.remove(context);
   }

   @Override
   public boolean contains(Object root) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      return this.segment.contains(context);
   }

   @Override
   public boolean isRef() {
      return this.ref;
   }

   @Override
   public Object eval(Object root) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.eval(context);
      return context.value;
   }

   @Override
   public void set(Object root, Object value) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.set(context, value);
   }

   @Override
   public void set(Object root, Object value, JSONReader.Feature... readerFeatures) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.set(context, value);
   }

   @Override
   public void setCallback(Object root, BiFunction callback) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.setCallback(context, callback);
   }

   @Override
   public void setInt(Object root, int value) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.setInt(context, value);
   }

   @Override
   public void setLong(Object root, long value) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      context.root = root;
      this.segment.setLong(context, value);
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      if (!this.extractSupport) {
         context.root = jsonReader.readAny();
         this.segment.eval(context);
      } else {
         this.segment.accept(jsonReader, context);
      }

      return context.value;
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      JSONPath.Context context = new JSONPath.Context(this, null, this.segment, null, 0L);
      this.segment.accept(jsonReader, context);
      return JSON.toJSONString(context.value);
   }

   @Override
   public final JSONPath getParent() {
      return JSONPath.RootPath.INSTANCE;
   }
}
