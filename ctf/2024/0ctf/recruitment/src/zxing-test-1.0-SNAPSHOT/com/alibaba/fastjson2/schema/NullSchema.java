package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONObject;

final class NullSchema extends JSONSchema {
   NullSchema(JSONObject input) {
      super(input);
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Null;
   }

   @Override
   public ValidateResult validate(Object value) {
      return value == null ? SUCCESS : new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Null, value.getClass());
   }
}
