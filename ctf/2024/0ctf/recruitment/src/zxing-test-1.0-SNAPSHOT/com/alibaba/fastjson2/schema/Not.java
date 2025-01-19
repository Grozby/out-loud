package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.reader.ObjectReaderBean;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class Not extends JSONSchema {
   final JSONSchema schema;
   final JSONSchema.Type[] types;
   final Boolean result;

   public Not(JSONSchema schema, JSONSchema.Type[] types, Boolean result) {
      super(null, null);
      this.schema = schema;
      this.types = types;
      this.result = result;
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.AllOf;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (this.schema != null && this.schema.validate(value).isSuccess()) {
         return FAIL_NOT;
      } else {
         if (this.types != null) {
            for (JSONSchema.Type type : this.types) {
               switch (type) {
                  case String:
                     if (value instanceof String) {
                        return FAIL_NOT;
                     }
                     break;
                  case Integer:
                     if (value instanceof Byte
                        || value instanceof Short
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof BigInteger
                        || value instanceof AtomicInteger
                        || value instanceof AtomicLong) {
                        return FAIL_NOT;
                     }
                     break;
                  case Number:
                     if (value instanceof Number) {
                        return FAIL_NOT;
                     }
                     break;
                  case Null:
                     if (value == null) {
                        return FAIL_NOT;
                     }
                     break;
                  case Array:
                     if (value instanceof Object[] || value instanceof Collection || value != null && value.getClass().isArray()) {
                        return FAIL_NOT;
                     }
                     break;
                  case Object:
                     if (value instanceof Map) {
                        return FAIL_NOT;
                     }

                     if (value != null && JSONSchema.CONTEXT.getObjectReader(value.getClass()) instanceof ObjectReaderBean) {
                        return FAIL_NOT;
                     }
                     break;
                  case Boolean:
                     if (value instanceof Boolean) {
                        return FAIL_NOT;
                     }
                     break;
                  case Any:
                     return FAIL_NOT;
               }
            }
         }

         if (this.result != null) {
            return this.result ? FAIL_NOT : SUCCESS;
         } else {
            return SUCCESS;
         }
      }
   }
}
