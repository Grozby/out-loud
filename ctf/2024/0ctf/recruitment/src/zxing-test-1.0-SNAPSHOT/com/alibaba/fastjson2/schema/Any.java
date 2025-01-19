package com.alibaba.fastjson2.schema;

final class Any extends JSONSchema {
   public static final Any INSTANCE = new Any();
   public static final JSONSchema NOT_ANY = new Not(INSTANCE, null, null);

   public Any() {
      super(null, null);
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Any;
   }

   @Override
   public ValidateResult validate(Object value) {
      return SUCCESS;
   }
}
