package com.alibaba.fastjson2;

import com.alibaba.fastjson2.function.impl.ToDouble;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

final class JSONPathFunction extends JSONPathSegment implements JSONPathSegment.EvalSegment {
   static final JSONPathFunction FUNC_TYPE = new JSONPathFunction(JSONPathFunction::type);
   static final JSONPathFunction FUNC_DOUBLE = new JSONPathFunction(new ToDouble(null));
   static final JSONPathFunction FUNC_FLOOR = new JSONPathFunction(JSONPathFunction::floor);
   static final JSONPathFunction FUNC_CEIL = new JSONPathFunction(JSONPathFunction::ceil);
   static final JSONPathFunction FUNC_ABS = new JSONPathFunction(JSONPathFunction::abs);
   static final JSONPathFunction FUNC_NEGATIVE = new JSONPathFunction(JSONPathFunction::negative);
   static final JSONPathFunction FUNC_EXISTS = new JSONPathFunction(JSONPathFunction::exists);
   static final JSONPathFunction FUNC_LOWER = new JSONPathFunction(JSONPathFunction::lower);
   static final JSONPathFunction FUNC_UPPER = new JSONPathFunction(JSONPathFunction::upper);
   static final JSONPathFunction FUNC_TRIM = new JSONPathFunction(JSONPathFunction::trim);
   static final JSONPathFunction FUNC_FIRST = new JSONPathFunction(JSONPathFunction::first);
   static final JSONPathFunction FUNC_LAST = new JSONPathFunction(JSONPathFunction::last);
   final Function function;

   public JSONPathFunction(Function function) {
      this.function = function;
   }

   static Object floor(Object value) {
      if (value instanceof Double) {
         return Math.floor((Double)value);
      } else if (value instanceof Float) {
         return Math.floor((double)((Float)value).floatValue());
      } else if (value instanceof BigDecimal) {
         return ((BigDecimal)value).setScale(0, RoundingMode.FLOOR);
      } else {
         if (value instanceof List) {
            List list = (List)value;
            int i = 0;

            for (int l = list.size(); i < l; i++) {
               Object item = list.get(i);
               if (item instanceof Double) {
                  list.set(i, Math.floor((Double)item));
               } else if (item instanceof Float) {
                  list.set(i, Math.floor((double)((Float)item).floatValue()));
               } else if (item instanceof BigDecimal) {
                  list.set(i, ((BigDecimal)item).setScale(0, RoundingMode.FLOOR));
               }
            }
         }

         return value;
      }
   }

   static Object ceil(Object value) {
      if (value instanceof Double) {
         return Math.ceil((Double)value);
      } else if (value instanceof Float) {
         return Math.ceil((double)((Float)value).floatValue());
      } else if (value instanceof BigDecimal) {
         return ((BigDecimal)value).setScale(0, RoundingMode.CEILING);
      } else {
         if (value instanceof List) {
            List list = (List)value;
            int i = 0;

            for (int l = list.size(); i < l; i++) {
               Object item = list.get(i);
               if (item instanceof Double) {
                  list.set(i, Math.ceil((Double)item));
               } else if (item instanceof Float) {
                  list.set(i, Math.ceil((double)((Float)item).floatValue()));
               } else if (item instanceof BigDecimal) {
                  list.set(i, ((BigDecimal)item).setScale(0, RoundingMode.CEILING));
               }
            }
         }

         return value;
      }
   }

   static Object exists(Object value) {
      return value != null;
   }

   static Object negative(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof Integer) {
         int intValue = (Integer)value;
         return intValue == Integer.MIN_VALUE ? -((long)intValue) : -intValue;
      } else if (value instanceof Long) {
         long longValue = (Long)value;
         return longValue == Long.MIN_VALUE ? BigInteger.valueOf(longValue).negate() : -longValue;
      } else if (value instanceof Byte) {
         byte byteValue = (Byte)value;
         return byteValue == -128 ? -((short)byteValue) : (byte)(-byteValue);
      } else if (value instanceof Short) {
         short shortValue = (Short)value;
         return shortValue == -32768 ? -shortValue : (short)(-shortValue);
      } else if (value instanceof Double) {
         return -(Double)value;
      } else if (value instanceof Float) {
         return -(Float)value;
      } else if (value instanceof BigDecimal) {
         return ((BigDecimal)value).negate();
      } else if (value instanceof BigInteger) {
         return ((BigInteger)value).negate();
      } else if (!(value instanceof List)) {
         return value;
      } else {
         List list = (List)value;
         JSONArray values = new JSONArray(list.size());

         for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object negativeItem = negative(item);
            values.add(negativeItem);
         }

         return values;
      }
   }

   static Object first(Object value) {
      if (value == null) {
         return null;
      } else {
         if (value instanceof JSONPath.Sequence) {
            value = ((JSONPath.Sequence)value).values;
         }

         if (value instanceof List) {
            return ((List)value).isEmpty() ? null : ((List)value).get(0);
         } else if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>)value;
            return collection.isEmpty() ? null : collection.iterator().next();
         } else if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            return len == 0 ? null : Array.get(value, 0);
         } else {
            return value;
         }
      }
   }

   static Object last(Object value) {
      if (value == null) {
         return null;
      } else {
         if (value instanceof JSONPath.Sequence) {
            value = ((JSONPath.Sequence)value).values;
         }

         if (value instanceof List) {
            List list = (List)value;
            int size = list.size();
            return size == 0 ? null : list.get(size - 1);
         } else if (!(value instanceof Collection)) {
            if (value.getClass().isArray()) {
               int len = Array.getLength(value);
               return len == 0 ? null : Array.get(value, len - 1);
            } else {
               return value;
            }
         } else {
            Collection<?> collection = (Collection<?>)value;
            if (collection.isEmpty()) {
               return null;
            } else {
               Object last = null;

               for (Object o : collection) {
                  last = o;
               }

               return last;
            }
         }
      }
   }

   static Object abs(Object value) {
      if (value == null) {
         return null;
      } else if (value instanceof Integer) {
         int intValue = (Integer)value;
         return intValue < 0 ? -intValue : value;
      } else if (value instanceof Long) {
         long longValue = (Long)value;
         return longValue < 0L ? -longValue : value;
      } else if (value instanceof Byte) {
         byte byteValue = (Byte)value;
         return byteValue < 0 ? (byte)(-byteValue) : value;
      } else if (value instanceof Short) {
         short shortValue = (Short)value;
         return shortValue < 0 ? (short)(-shortValue) : value;
      } else if (value instanceof Double) {
         double doubleValue = (Double)value;
         return doubleValue < 0.0 ? -doubleValue : value;
      } else if (value instanceof Float) {
         float floatValue = (Float)value;
         return floatValue < 0.0F ? -floatValue : value;
      } else if (value instanceof BigDecimal) {
         return ((BigDecimal)value).abs();
      } else if (value instanceof BigInteger) {
         return ((BigInteger)value).abs();
      } else if (!(value instanceof List)) {
         throw new JSONException("abs not support " + value);
      } else {
         List list = (List)value;
         JSONArray values = new JSONArray(list.size());

         for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            values.add(abs(item));
         }

         return values;
      }
   }

   static String type(Object value) {
      if (value == null) {
         return "null";
      } else if (value instanceof Collection) {
         return "array";
      } else if (value instanceof Number) {
         return "number";
      } else if (value instanceof Boolean) {
         return "boolean";
      } else {
         return !(value instanceof String) && !(value instanceof UUID) && !(value instanceof Enum) ? "object" : "string";
      }
   }

   static Object lower(Object value) {
      if (value == null) {
         return null;
      } else {
         String str;
         if (value instanceof String) {
            str = (String)value;
         } else {
            str = value.toString();
         }

         return str.toLowerCase();
      }
   }

   static Object upper(Object value) {
      if (value == null) {
         return null;
      } else {
         String str;
         if (value instanceof String) {
            str = (String)value;
         } else {
            str = value.toString();
         }

         return str.toUpperCase();
      }
   }

   static Object trim(Object value) {
      if (value == null) {
         return null;
      } else {
         String str;
         if (value instanceof String) {
            str = (String)value;
         } else {
            str = value.toString();
         }

         return str.trim();
      }
   }

   @Override
   public void accept(JSONReader jsonReader, JSONPath.Context context) {
      if (context.parent == null) {
         context.root = jsonReader.readAny();
         context.eval = true;
      }

      this.eval(context);
   }

   @Override
   public void eval(JSONPath.Context context) {
      Object value = context.parent == null ? context.root : context.parent.value;
      context.value = this.function.apply(value);
   }

   static final class BiFunctionAdapter implements BiFunction {
      private final Function function;

      BiFunctionAdapter(Function function) {
         this.function = function;
      }

      @Override
      public Object apply(Object o1, Object o2) {
         return this.function.apply(o2);
      }
   }

   static final class FilterFunction implements Function {
      final JSONPathFilter filter;

      FilterFunction(JSONPathFilter filter) {
         this.filter = filter;
      }

      @Override
      public Object apply(Object o) {
         return null;
      }
   }

   abstract static class Index implements Function {
      protected abstract boolean eq(Object var1);

      @Override
      public final Object apply(Object o) {
         if (o == null) {
            return null;
         } else if (o instanceof List) {
            List list = (List)o;

            for (int i = 0; i < list.size(); i++) {
               if (this.eq(list.get(i))) {
                  return i;
               }
            }

            return -1;
         } else if (o.getClass().isArray()) {
            int len = Array.getLength(o);

            for (int ix = 0; ix < len; ix++) {
               Object item = Array.get(o, ix);
               if (this.eq(item)) {
                  return ix;
               }
            }

            return -1;
         } else {
            return this.eq(o) ? 0 : null;
         }
      }
   }

   static final class IndexDecimal extends JSONPathFunction.Index {
      final BigDecimal value;

      public IndexDecimal(BigDecimal value) {
         this.value = value;
      }

      @Override
      protected boolean eq(Object item) {
         if (item == null) {
            return false;
         } else if (item instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)item;
            decimal = decimal.stripTrailingZeros();
            return this.value.equals(decimal);
         } else if (!(item instanceof Float) && !(item instanceof Double)) {
            if (item instanceof String) {
               String str = (String)item;
               if (TypeUtils.isNumber(str)) {
                  BigDecimal decimal = new BigDecimal(str).stripTrailingZeros();
                  return this.value.equals(decimal);
               }
            }

            return false;
         } else {
            double doubleValue = ((Number)item).doubleValue();
            BigDecimal decimal = new BigDecimal(doubleValue).stripTrailingZeros();
            return this.value.equals(decimal);
         }
      }
   }

   static final class IndexInt extends JSONPathFunction.Index {
      final long value;
      transient BigDecimal decimalValue;

      public IndexInt(long value) {
         this.value = value;
      }

      @Override
      protected boolean eq(Object item) {
         if (item instanceof Integer || item instanceof Long || item instanceof Byte || item instanceof Short) {
            return ((Number)item).longValue() == this.value;
         } else if (item instanceof Float || item instanceof Double) {
            return ((Number)item).doubleValue() == (double)this.value;
         } else if (item instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)item;
            decimal = decimal.stripTrailingZeros();
            if (this.decimalValue == null) {
               this.decimalValue = BigDecimal.valueOf(this.value);
            }

            return this.decimalValue.equals(decimal);
         } else {
            return false;
         }
      }
   }

   static final class IndexString extends JSONPathFunction.Index {
      final String value;

      public IndexString(String value) {
         this.value = value;
      }

      @Override
      protected boolean eq(Object item) {
         return item == null ? false : this.value.equals(item.toString());
      }
   }

   static final class IndexValue implements Function {
      final int index;

      public IndexValue(int index) {
         this.index = index;
      }

      @Override
      public Object apply(Object o) {
         if (o == null) {
            return null;
         } else if (o instanceof List) {
            return ((List)o).get(this.index);
         } else {
            if (o.getClass().isArray()) {
               int len = Array.getLength(o);
               int i = 0;
               if (i < len) {
                  return Array.get(o, i);
               }
            }

            return null;
         }
      }
   }

   static final class SizeFunction implements Function {
      static final JSONPathFunction.SizeFunction INSTANCE = new JSONPathFunction.SizeFunction();

      @Override
      public Object apply(Object value) {
         if (value == null) {
            return -1;
         } else if (value instanceof Collection) {
            return ((Collection)value).size();
         } else if (value.getClass().isArray()) {
            return Array.getLength(value);
         } else if (value instanceof Map) {
            return ((Map)value).size();
         } else {
            return value instanceof JSONPath.Sequence ? ((JSONPath.Sequence)value).values.size() : 1;
         }
      }
   }

   static final class TypeFunction implements Function {
      static final JSONPathFunction.TypeFunction INSTANCE = new JSONPathFunction.TypeFunction();

      @Override
      public Object apply(Object object) {
         return JSONPathFunction.type(object);
      }
   }
}
