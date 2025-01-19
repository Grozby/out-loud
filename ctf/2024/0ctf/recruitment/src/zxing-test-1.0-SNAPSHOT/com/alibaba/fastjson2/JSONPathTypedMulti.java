package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.util.function.BiFunction;

class JSONPathTypedMulti extends JSONPath {
   final JSONPath[] paths;
   final Type[] types;
   final String[] formats;
   final long[] pathFeatures;
   final ZoneId zoneId;

   protected JSONPathTypedMulti(JSONPath[] paths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features) {
      super(JSON.toJSONString(paths), features);
      this.types = types;
      this.paths = paths;
      this.formats = formats;
      this.pathFeatures = pathFeatures;
      this.zoneId = zoneId;
   }

   @Override
   public JSONPath getParent() {
      return this.paths[0].getParent();
   }

   @Override
   public boolean isRef() {
      for (JSONPath jsonPath : this.paths) {
         if (!jsonPath.isRef()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean contains(Object object) {
      for (JSONPath jsonPath : this.paths) {
         if (jsonPath.contains(object)) {
            return true;
         }
      }

      return false;
   }

   protected final boolean ignoreError(int index) {
      return this.pathFeatures != null && index < this.pathFeatures.length && (this.pathFeatures[index] & JSONPath.Feature.NullOnError.mask) != 0L;
   }

   @Override
   public Object eval(Object object) {
      Object[] array = new Object[this.paths.length];

      for (int i = 0; i < this.paths.length; i++) {
         JSONPath jsonPath = this.paths[i];
         Object result = jsonPath.eval(object);

         try {
            array[i] = TypeUtils.cast(result, this.types[i]);
         } catch (Exception var7) {
            if (!this.ignoreError(i)) {
               throw new JSONException("jsonpath eval path, path : " + jsonPath + ", msg : " + var7.getMessage(), var7);
            }
         }
      }

      return array;
   }

   @Override
   protected JSONReader.Context createContext() {
      JSONReader.Context context = JSONFactory.createReadContext(this.features);
      if (this.zoneId != null && this.zoneId != DateUtils.DEFAULT_ZONE_ID) {
         context.zoneId = this.zoneId;
      }

      return context;
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      Object object = jsonReader.readAny();
      return this.eval(object);
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      Object object = this.extract(jsonReader);
      return JSON.toJSONString(object);
   }

   @Override
   public void set(Object object, Object value) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void set(Object object, Object value, JSONReader.Feature... readerFeatures) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void setCallback(Object object, BiFunction callback) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void setInt(Object object, int value) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public void setLong(Object object, long value) {
      throw new JSONException("unsupported operation");
   }

   @Override
   public boolean remove(Object object) {
      throw new JSONException("unsupported operation");
   }
}
