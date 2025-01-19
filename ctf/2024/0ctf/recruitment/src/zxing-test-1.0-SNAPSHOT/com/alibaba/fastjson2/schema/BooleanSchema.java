package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONObject;

public final class BooleanSchema extends JSONSchema {
   BooleanSchema(JSONObject input) {
      super(input);
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Boolean;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value == null) {
         return FAIL_INPUT_NULL;
      } else {
         return value instanceof Boolean ? SUCCESS : new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Boolean, value.getClass());
      }
   }

   @Override
   public JSONObject toJSONObject() {
      return JSONObject.of("type", "boolean");
   }
}
