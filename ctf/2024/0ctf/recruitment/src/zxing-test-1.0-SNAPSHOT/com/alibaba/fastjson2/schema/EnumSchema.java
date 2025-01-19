package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.util.TypeUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;

public final class EnumSchema extends JSONSchema {
   final Set<Object> items;

   EnumSchema(Object... items) {
      super(null, null);
      this.items = new LinkedHashSet<>(items.length);

      for (Object item : items) {
         if (item instanceof BigDecimal) {
            BigDecimal decimal = ((BigDecimal)item).stripTrailingZeros();
            if (decimal.scale() == 0) {
               BigInteger bigInt = decimal.toBigInteger();
               if (bigInt.compareTo(TypeUtils.BIGINT_INT32_MIN) >= 0 && bigInt.compareTo(TypeUtils.BIGINT_INT32_MAX) <= 0) {
                  item = bigInt.intValue();
               } else if (bigInt.compareTo(TypeUtils.BIGINT_INT64_MIN) >= 0 && bigInt.compareTo(TypeUtils.BIGINT_INT64_MAX) <= 0) {
                  item = bigInt.longValue();
               } else {
                  item = bigInt;
               }
            } else {
               item = decimal;
            }
         }

         this.items.add(item);
      }
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Enum;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value instanceof BigDecimal) {
         BigDecimal decimal = (BigDecimal)value;
         value = decimal.stripTrailingZeros();
         long longValue = decimal.longValue();
         if (decimal.compareTo(BigDecimal.valueOf(longValue)) == 0) {
            value = longValue;
         } else if (decimal.scale() == 0) {
            value = decimal.unscaledValue();
         }
      } else if (value instanceof BigInteger) {
         BigInteger bigInt = (BigInteger)value;
         if (bigInt.compareTo(TypeUtils.BIGINT_INT64_MIN) >= 0 && bigInt.compareTo(TypeUtils.BIGINT_INT64_MAX) <= 0) {
            value = bigInt.longValue();
         }
      }

      if (value instanceof Long) {
         long longValue = (Long)value;
         if (longValue >= -2147483648L && longValue <= 2147483647L) {
            value = (int)longValue;
         }
      }

      if (!this.items.contains(value)) {
         return value == null ? FAIL_INPUT_NULL : new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Enum, value.getClass());
      } else {
         return SUCCESS;
      }
   }
}
