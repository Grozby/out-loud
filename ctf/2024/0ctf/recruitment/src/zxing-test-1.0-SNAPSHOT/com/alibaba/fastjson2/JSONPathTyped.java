package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.function.BiFunction;

class JSONPathTyped extends JSONPath {
   final JSONPath jsonPath;
   final Type type;

   protected JSONPathTyped(JSONPath jsonPath, Type type) {
      super(jsonPath.path, jsonPath.features);
      this.type = type;
      this.jsonPath = jsonPath;
   }

   @Override
   public JSONPath getParent() {
      return this.jsonPath.getParent();
   }

   @Override
   public boolean isRef() {
      return this.jsonPath.isRef();
   }

   @Override
   public boolean contains(Object object) {
      return this.jsonPath.contains(object);
   }

   @Override
   public Object eval(Object object) {
      Object result = this.jsonPath.eval(object);
      return TypeUtils.cast(result, this.type);
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      Object result = this.jsonPath.extract(jsonReader);
      return TypeUtils.cast(result, this.type);
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      return this.jsonPath.extractScalar(jsonReader);
   }

   @Override
   public void set(Object object, Object value) {
      this.jsonPath.set(object, value);
   }

   @Override
   public void set(Object object, Object value, JSONReader.Feature... readerFeatures) {
      this.jsonPath.set(object, value, readerFeatures);
   }

   @Override
   public void setCallback(Object object, BiFunction callback) {
      this.jsonPath.setCallback(object, callback);
   }

   @Override
   public void setInt(Object object, int value) {
      this.jsonPath.setInt(object, value);
   }

   @Override
   public void setLong(Object object, long value) {
      this.jsonPath.setLong(object, value);
   }

   @Override
   public boolean remove(Object object) {
      return this.jsonPath.remove(object);
   }

   public Type getType() {
      return this.type;
   }

   public static JSONPath of(JSONPath jsonPath, Type type) {
      if (type != null && type != Object.class) {
         if (jsonPath instanceof JSONPathTyped) {
            JSONPathTyped jsonPathTyped = (JSONPathTyped)jsonPath;
            if (jsonPathTyped.type.equals(type)) {
               return jsonPath;
            }
         }

         if (jsonPath instanceof JSONPathSingleName) {
            if (type == Integer.class) {
               return new JSONPathSingleNameInteger((JSONPathSingleName)jsonPath);
            }

            if (type == Long.class) {
               return new JSONPathSingleNameLong((JSONPathSingleName)jsonPath);
            }

            if (type == String.class) {
               return new JSONPathSingleNameString((JSONPathSingleName)jsonPath);
            }

            if (type == BigDecimal.class) {
               return new JSONPathSingleNameDecimal((JSONPathSingleName)jsonPath);
            }
         }

         return new JSONPathTyped(jsonPath, type);
      } else {
         return jsonPath;
      }
   }
}
