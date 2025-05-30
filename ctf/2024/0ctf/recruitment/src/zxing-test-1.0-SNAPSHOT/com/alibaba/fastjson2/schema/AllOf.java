package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

final class AllOf extends JSONSchema {
   final JSONSchema[] items;

   public AllOf(JSONSchema[] items) {
      super(null, null);
      this.items = items;
   }

   public AllOf(JSONObject input, JSONSchema parent) {
      super(input);
      JSONArray items = input.getJSONArray("allOf");
      if (items != null && !items.isEmpty()) {
         this.items = new JSONSchema[items.size()];
         JSONSchema.Type type = null;

         for (int i = 0; i < this.items.length; i++) {
            JSONSchema itemSchema = null;
            Object item = items.get(i);
            if (item instanceof Boolean) {
               itemSchema = (JSONSchema)((Boolean)item ? Any.INSTANCE : Any.NOT_ANY);
            } else {
               JSONObject itemObject = (JSONObject)item;
               if (!itemObject.containsKey("type") && type != null) {
                  switch (type) {
                     case String:
                        itemSchema = new StringSchema(itemObject);
                        break;
                     case Integer:
                        itemSchema = new IntegerSchema(itemObject);
                        break;
                     case Number:
                        itemSchema = new NumberSchema(itemObject);
                        break;
                     case Boolean:
                        itemSchema = new BooleanSchema(itemObject);
                        break;
                     case Array:
                        itemSchema = new ArraySchema(itemObject, null);
                        break;
                     case Object:
                        itemSchema = new ObjectSchema(itemObject);
                  }
               }

               if (itemSchema == null) {
                  itemSchema = JSONSchema.of(itemObject, parent);
               }
            }

            type = itemSchema.getType();
            this.items[i] = itemSchema;
         }
      } else {
         throw new JSONException("allOf not found");
      }
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.AllOf;
   }

   @Override
   public ValidateResult validate(Object value) {
      for (JSONSchema item : this.items) {
         ValidateResult result = item.validate(value);
         if (!result.isSuccess()) {
            return result;
         }
      }

      return SUCCESS;
   }
}
