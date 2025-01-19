package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

final class AnyOf extends JSONSchema {
   final JSONSchema[] items;

   public AnyOf(JSONSchema[] items) {
      super(null, null);
      this.items = items;
   }

   public AnyOf(JSONObject input, JSONSchema parent) {
      super(input);
      JSONArray items = input.getJSONArray("anyOf");
      if (items != null && !items.isEmpty()) {
         this.items = new JSONSchema[items.size()];

         for (int i = 0; i < this.items.length; i++) {
            Object item = items.get(i);
            if (item instanceof Boolean) {
               this.items[i] = (JSONSchema)((Boolean)item ? Any.INSTANCE : Any.NOT_ANY);
            } else {
               this.items[i] = JSONSchema.of((JSONObject)item, parent);
            }
         }
      } else {
         throw new JSONException("anyOf not found");
      }
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.AnyOf;
   }

   @Override
   public ValidateResult validate(Object value) {
      for (JSONSchema item : this.items) {
         ValidateResult result = item.validate(value);
         if (result == SUCCESS) {
            return SUCCESS;
         }
      }

      return FAIL_ANY_OF;
   }

   @Override
   public JSONObject toJSONObject() {
      return JSONObject.of("anyOf", this.items);
   }
}
