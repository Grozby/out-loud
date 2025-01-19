package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONObject;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class NumberSchema extends JSONSchema {
   final BigDecimal minimum;
   final long minimumLongValue;
   final boolean exclusiveMinimum;
   final BigDecimal maximum;
   final long maximumLongValue;
   final boolean exclusiveMaximum;
   final BigDecimal multipleOf;
   final long multipleOfLongValue;
   final boolean typed;

   NumberSchema(JSONObject input) {
      super(input);
      this.typed = "number".equals(input.get("type"));
      Object exclusiveMinimum = input.get("exclusiveMinimum");
      BigDecimal minimum = input.getBigDecimal("minimum");
      if (exclusiveMinimum == Boolean.TRUE) {
         this.minimum = minimum;
         this.exclusiveMinimum = true;
      } else if (exclusiveMinimum instanceof Number) {
         this.minimum = input.getBigDecimal("exclusiveMinimum");
         this.exclusiveMinimum = true;
      } else {
         this.minimum = minimum;
         this.exclusiveMinimum = false;
      }

      if (this.minimum != null && this.minimum.compareTo(BigDecimal.valueOf(this.minimum.longValue())) == 0) {
         this.minimumLongValue = this.minimum.longValue();
      } else {
         this.minimumLongValue = Long.MIN_VALUE;
      }

      BigDecimal maximum = input.getBigDecimal("maximum");
      Object exclusiveMaximum = input.get("exclusiveMaximum");
      if (exclusiveMaximum == Boolean.TRUE) {
         this.maximum = maximum;
         this.exclusiveMaximum = true;
      } else if (exclusiveMaximum instanceof Number) {
         this.maximum = input.getBigDecimal("exclusiveMaximum");
         this.exclusiveMaximum = true;
      } else {
         this.maximum = maximum;
         this.exclusiveMaximum = false;
      }

      if (this.maximum != null && this.maximum.compareTo(BigDecimal.valueOf(this.maximum.longValue())) == 0) {
         this.maximumLongValue = this.maximum.longValue();
      } else {
         this.maximumLongValue = Long.MIN_VALUE;
      }

      this.multipleOf = input.getBigDecimal("multipleOf");
      if (this.multipleOf == null) {
         this.multipleOfLongValue = Long.MIN_VALUE;
      } else {
         long longValue = this.multipleOf.longValue();
         if (this.multipleOf.compareTo(BigDecimal.valueOf(longValue)) == 0) {
            this.multipleOfLongValue = longValue;
         } else {
            this.multipleOfLongValue = Long.MIN_VALUE;
         }
      }
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Number;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value == null) {
         return this.typed ? FAIL_INPUT_NULL : SUCCESS;
      } else if (!(value instanceof Number)) {
         return this.typed ? FAIL_TYPE_NOT_MATCH : SUCCESS;
      } else {
         Number number = (Number)value;
         if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return this.validate(number.longValue());
         } else if (!(number instanceof Float) && !(number instanceof Double)) {
            BigDecimal decimalValue;
            if (number instanceof BigInteger) {
               decimalValue = new BigDecimal((BigInteger)number);
            } else {
               if (!(number instanceof BigDecimal)) {
                  return new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Number, value.getClass());
               }

               decimalValue = (BigDecimal)number;
            }

            if (this.minimum == null || (this.exclusiveMinimum ? this.minimum.compareTo(decimalValue) < 0 : this.minimum.compareTo(decimalValue) <= 0)) {
               if (this.maximum == null || (this.exclusiveMaximum ? this.maximum.compareTo(decimalValue) > 0 : this.maximum.compareTo(decimalValue) >= 0)) {
                  return this.multipleOf != null && decimalValue.divideAndRemainder(this.multipleOf)[1].abs().compareTo(BigDecimal.ZERO) > 0
                     ? new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, decimalValue)
                     : SUCCESS;
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
         } else {
            return this.validate(number.doubleValue());
         }
      }
   }

   @Override
   public ValidateResult validate(Integer value) {
      return value == null ? SUCCESS : this.validate(value.longValue());
   }

   @Override
   public ValidateResult validate(Float value) {
      return value == null ? SUCCESS : this.validate(value.doubleValue());
   }

   @Override
   public ValidateResult validate(Double value) {
      return value == null ? SUCCESS : this.validate(value.doubleValue());
   }

   @Override
   public ValidateResult validate(Long value) {
      return value == null ? SUCCESS : this.validate(value.longValue());
   }

   @Override
   public ValidateResult validate(long value) {
      BigDecimal decimalValue = null;
      if (this.minimum != null) {
         if (this.minimumLongValue != Long.MIN_VALUE) {
            if (this.exclusiveMinimum ? value <= this.minimumLongValue : value < this.minimumLongValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
                  this.minimum,
                  value
               );
            }
         } else {
            decimalValue = BigDecimal.valueOf(value);
            if (this.exclusiveMinimum ? this.minimum.compareTo(decimalValue) >= 0 : this.minimum.compareTo(decimalValue) > 0) {
               return new ValidateResult(
                  false,
                  this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
                  this.minimum,
                  value
               );
            }
         }
      }

      if (this.maximum != null) {
         if (this.maximumLongValue != Long.MIN_VALUE) {
            if (this.exclusiveMaximum ? value >= this.maximumLongValue : value > this.maximumLongValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         } else {
            if (decimalValue == null) {
               decimalValue = BigDecimal.valueOf(value);
            }

            if (this.exclusiveMaximum ? this.maximum.compareTo(decimalValue) <= 0 : this.maximum.compareTo(decimalValue) < 0) {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         }
      }

      if (this.multipleOf != null) {
         if (this.multipleOfLongValue != Long.MIN_VALUE && value % this.multipleOfLongValue != 0L) {
            return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, decimalValue);
         }

         if (decimalValue == null) {
            decimalValue = BigDecimal.valueOf(value);
         }

         if (decimalValue.divideAndRemainder(this.multipleOf)[1].abs().compareTo(BigDecimal.ZERO) > 0) {
            return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, value);
         }
      }

      return SUCCESS;
   }

   @Override
   public ValidateResult validate(double value) {
      if (this.minimum != null) {
         if (this.minimumLongValue != Long.MIN_VALUE) {
            if (this.exclusiveMinimum ? value <= (double)this.minimumLongValue : value < (double)this.minimumLongValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
                  this.minimum,
                  value
               );
            }
         } else {
            double minimumDoubleValue = this.minimum.doubleValue();
            if (this.exclusiveMinimum ? value <= minimumDoubleValue : value < minimumDoubleValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMinimum ? "exclusiveMinimum not match, expect > %s, but %s" : "minimum not match, expect >= %s, but %s",
                  this.minimum,
                  value
               );
            }
         }
      }

      if (this.maximum != null) {
         if (this.maximumLongValue != Long.MIN_VALUE) {
            if (this.exclusiveMaximum ? value >= (double)this.maximumLongValue : value > (double)this.maximumLongValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         } else {
            double maximumDoubleValue = this.maximum.doubleValue();
            if (this.exclusiveMaximum ? value >= maximumDoubleValue : value > maximumDoubleValue) {
               return new ValidateResult(
                  false,
                  this.exclusiveMaximum ? "exclusiveMaximum not match, expect < %s, but %s" : "maximum not match, expect <= %s, but %s",
                  this.maximum,
                  value
               );
            }
         }
      }

      if (this.multipleOf != null) {
         if (this.multipleOfLongValue != Long.MIN_VALUE && value % (double)this.multipleOfLongValue != 0.0) {
            return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, value);
         }

         BigDecimal decimalValue = BigDecimal.valueOf(value);
         if (decimalValue.divideAndRemainder(this.multipleOf)[1].abs().compareTo(BigDecimal.ZERO) > 0) {
            return new ValidateResult(false, "multipleOf not match, expect multipleOf %s, but %s", this.multipleOf, decimalValue);
         }
      }

      return SUCCESS;
   }

   @Override
   public JSONObject toJSONObject() {
      JSONObject object = JSONObject.of("type", "number");
      if (this.minimumLongValue != Long.MIN_VALUE) {
         object.put(this.exclusiveMinimum ? "exclusiveMinimum" : "minimum", Long.valueOf(this.minimumLongValue));
      } else if (this.minimum != null) {
         object.put(this.exclusiveMinimum ? "exclusiveMinimum" : "minimum", this.minimum);
      }

      if (this.maximumLongValue != Long.MIN_VALUE) {
         object.put(this.exclusiveMaximum ? "exclusiveMaximum" : "maximum", Long.valueOf(this.minimumLongValue));
      } else if (this.maximum != null) {
         object.put(this.exclusiveMaximum ? "exclusiveMaximum" : "maximum", this.maximum);
      }

      if (this.multipleOfLongValue != Long.MIN_VALUE) {
         object.put("multipleOf", Long.valueOf(this.multipleOfLongValue));
      } else if (this.multipleOf != null) {
         object.put("multipleOf", this.multipleOf);
      }

      return object;
   }
}
