package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.TypeUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class IntegerSchema extends JSONSchema {
   final boolean typed;
   final long minimum;
   final boolean exclusiveMinimum;
   final long maximum;
   final boolean exclusiveMaximum;
   final long multipleOf;
   final Long constValue;

   IntegerSchema(JSONObject input) {
      super(input);
      this.typed = "integer".equalsIgnoreCase(input.getString("type")) || input.getBooleanValue("required");
      Object exclusiveMinimum = input.get("exclusiveMinimum");
      long minimum = input.getLongValue("minimum", Long.MIN_VALUE);
      if (exclusiveMinimum == Boolean.TRUE) {
         this.exclusiveMinimum = true;
         this.minimum = minimum;
      } else if (exclusiveMinimum instanceof Number) {
         this.exclusiveMinimum = true;
         this.minimum = input.getLongValue("exclusiveMinimum");
      } else {
         this.minimum = minimum;
         this.exclusiveMinimum = false;
      }

      long maximum = input.getLongValue("maximum", Long.MIN_VALUE);
      Object exclusiveMaximum = input.get("exclusiveMaximum");
      if (exclusiveMaximum == Boolean.TRUE) {
         this.exclusiveMaximum = true;
         this.maximum = maximum;
      } else if (exclusiveMaximum instanceof Number) {
         this.exclusiveMaximum = true;
         this.maximum = input.getLongValue("exclusiveMaximum");
      } else {
         this.exclusiveMaximum = false;
         this.maximum = maximum;
      }

      this.multipleOf = input.getLongValue("multipleOf", 0L);
      this.constValue = input.getLong("const");
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Integer;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value == null) {
         return this.typed ? FAIL_INPUT_NULL : SUCCESS;
      } else {
         Class valueClass = value.getClass();
         if (valueClass != Byte.class
            && valueClass != Short.class
            && valueClass != Integer.class
            && valueClass != Long.class
            && valueClass != BigInteger.class
            && valueClass != AtomicInteger.class
            && valueClass != AtomicLong.class) {
            if (value instanceof BigDecimal) {
               BigDecimal decimal = (BigDecimal)value;
               boolean integer = TypeUtils.isInteger(decimal);
               if (integer) {
                  BigInteger unscaleValue = decimal.toBigInteger();
                  if (this.constValue != null) {
                     boolean equals = false;
                     if (TypeUtils.isInt64(unscaleValue)) {
                        equals = this.constValue == unscaleValue.longValue();
                     }

                     if (!equals) {
                        return new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
                     }
                  }

                  return SUCCESS;
               }

               if (this.constValue != null) {
                  return new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
               }
            }

            if (this.constValue != null) {
               if (value instanceof Float) {
                  float floatValue = (Float)value;
                  if ((float)this.constValue.longValue() != floatValue) {
                     return new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
                  }
               } else if (value instanceof Double) {
                  double doubleValue = (Double)value;
                  if ((double)this.constValue.longValue() != doubleValue) {
                     return new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
                  }
               } else if (value instanceof String) {
                  String str = (String)value;
                  boolean equalsx = false;
                  if (TypeUtils.isInteger(str) && str.length() < 21) {
                     try {
                        long longValue = Long.parseLong(str);
                        equalsx = this.constValue == longValue;
                     } catch (NumberFormatException var7) {
                     }
                  }

                  if (!equalsx) {
                     return new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
                  }
               }
            }

            return this.typed ? new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Integer, valueClass) : SUCCESS;
         } else {
            boolean isInt64 = true;
            if (valueClass == BigInteger.class) {
               isInt64 = TypeUtils.isInt64((BigInteger)value);
            }

            long longValue = ((Number)value).longValue();
            if (this.minimum == Long.MIN_VALUE || (this.exclusiveMinimum ? longValue > this.minimum : longValue >= this.minimum)) {
               if (this.maximum == Long.MIN_VALUE || (this.exclusiveMaximum ? longValue < this.maximum : longValue <= this.maximum)) {
                  if (this.multipleOf != 0L && longValue % this.multipleOf != 0L) {
                     return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, value);
                  } else {
                     return this.constValue == null || this.constValue == longValue && isInt64
                        ? SUCCESS
                        : new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value);
                  }
               } else {
                  return new ValidateResult(
                     false,
                     this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                     this.maximum,
                     value
                  );
               }
            } else {
               return new ValidateResult(
                  false,
                  this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
                  this.minimum,
                  value
               );
            }
         }
      }
   }

   @Override
   public ValidateResult validate(long longValue) {
      if (this.minimum == Long.MIN_VALUE || (this.exclusiveMinimum ? longValue > this.minimum : longValue >= this.minimum)) {
         if (this.maximum == Long.MIN_VALUE || (this.exclusiveMaximum ? longValue < this.maximum : longValue <= this.maximum)) {
            if (this.multipleOf != 0L && longValue % this.multipleOf != 0L) {
               return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, longValue);
            } else {
               return this.constValue != null && this.constValue != longValue
                  ? new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, longValue)
                  : SUCCESS;
            }
         } else {
            return new ValidateResult(
               false,
               this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
               this.maximum,
               longValue
            );
         }
      } else {
         return new ValidateResult(
            false,
            this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
            this.minimum,
            longValue
         );
      }
   }

   @Override
   public ValidateResult validate(Long value) {
      if (value == null) {
         return this.typed ? FAIL_INPUT_NULL : SUCCESS;
      } else {
         long longValue = value;
         if (this.minimum == Long.MIN_VALUE || (this.exclusiveMinimum ? longValue > this.minimum : longValue >= this.minimum)) {
            if (this.maximum == Long.MIN_VALUE || (this.exclusiveMaximum ? longValue < this.maximum : longValue <= this.maximum)) {
               if (this.multipleOf != 0L && longValue % this.multipleOf != 0L) {
                  return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, longValue);
               } else {
                  return this.constValue != null && this.constValue != longValue
                     ? new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value)
                     : SUCCESS;
               }
            } else {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         } else {
            return new ValidateResult(
               false,
               this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
               this.minimum,
               value
            );
         }
      }
   }

   @Override
   public ValidateResult validate(Integer value) {
      if (value == null) {
         return this.typed ? FAIL_INPUT_NULL : SUCCESS;
      } else {
         long longValue = value.longValue();
         if (this.minimum == Long.MIN_VALUE || (this.exclusiveMinimum ? longValue > this.minimum : longValue >= this.minimum)) {
            if (this.maximum == Long.MIN_VALUE || (this.exclusiveMaximum ? longValue < this.maximum : longValue <= this.maximum)) {
               if (this.multipleOf != 0L && longValue % this.multipleOf != 0L) {
                  return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, longValue);
               } else {
                  return this.constValue != null && this.constValue != longValue
                     ? new ValidateResult(false, "const not match, expect %s, but %s", this.constValue, value)
                     : SUCCESS;
               }
            } else {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         } else {
            return new ValidateResult(
               false,
               this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
               this.minimum,
               value
            );
         }
      }
   }

   @Override
   public JSONObject toJSONObject() {
      JSONObject object = new JSONObject();
      object.put("type", "integer");
      if (this.minimum != Long.MIN_VALUE) {
         object.put(this.exclusiveMinimum ? "exclusiveMinimum" : "minimum", Long.valueOf(this.minimum));
      }

      if (this.maximum != Long.MIN_VALUE) {
         object.put(this.exclusiveMaximum ? "exclusiveMaximum" : "maximum", Long.valueOf(this.maximum));
      }

      if (this.multipleOf != 0L) {
         object.put("multipleOf", Long.valueOf(this.multipleOf));
      }

      if (this.constValue != null) {
         object.put("const", this.constValue);
      }

      return object;
   }
}
