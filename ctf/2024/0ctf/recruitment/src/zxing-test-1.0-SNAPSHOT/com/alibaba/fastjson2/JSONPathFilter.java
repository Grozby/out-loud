package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract class JSONPathFilter extends JSONPathSegment implements JSONPathSegment.EvalSegment {
   private boolean and = true;

   public boolean isAnd() {
      return this.and;
   }

   public JSONPathFilter setAnd(boolean and) {
      this.and = and;
      return this;
   }

   abstract boolean apply(JSONPath.Context var1, Object var2);

   static final class EndsWithSegment extends JSONPathFilter.NameFilter {
      final String prefix;

      public EndsWithSegment(String fieldName, long fieldNameNameHash, String prefix) {
         super(fieldName, fieldNameNameHash);
         this.prefix = prefix;
      }

      @Override
      boolean apply(Object fieldValue) {
         String propertyValue = fieldValue.toString();
         return propertyValue != null && propertyValue.endsWith(this.prefix);
      }
   }

   static final class GroupFilter extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      final List<JSONPathFilter> filters;

      public GroupFilter(List<JSONPathFilter> filters) {
         this.filters = filters;
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent == null) {
            context.root = jsonReader.readAny();
         }

         this.eval(context);
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         List<JSONPathFilter> orderedFilters = new ArrayList<>();
         if (this.filters != null) {
            orderedFilters = this.filters.stream().sorted(Comparator.comparing(JSONPathFilter::isAnd)).collect(Collectors.toList());
         }

         if (object instanceof List) {
            List list = (List)object;
            JSONArray array = new JSONArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               boolean match = false;

               for (JSONPathFilter filter : orderedFilters) {
                  boolean and = filter.isAnd();
                  match = and;
                  boolean result = filter.apply(context, item);
                  if (and) {
                     if (!result) {
                        match = false;
                        break;
                     }
                  } else if (result) {
                     match = true;
                     break;
                  }
               }

               if (match) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else {
            boolean match = false;

            for (JSONPathFilter filterx : orderedFilters) {
               boolean and = filterx.isAnd();
               match = and;
               boolean result = filterx.apply(context, object);
               if (and) {
                  if (!result) {
                     match = false;
                     break;
                  }
               } else if (result) {
                  match = true;
                  break;
               }
            }

            if (match) {
               context.value = object;
            }

            context.eval = true;
         }
      }
   }

   static final class NameArrayOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final JSONArray array;

      public NameArrayOpSegment(
         String fieldName,
         long fieldNameNameHash,
         String[] fieldName2,
         long[] fieldNameNameHash2,
         Function function,
         JSONPathFilter.Operator operator,
         JSONArray array
      ) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, function);
         this.operator = operator;
         this.array = array;
      }

      @Override
      boolean apply(Object fieldValue) {
         if (Objects.requireNonNull(this.operator) == JSONPathFilter.Operator.EQ) {
            return this.array.equals(fieldValue);
         } else {
            throw new JSONException("not support operator : " + this.operator);
         }
      }
   }

   static final class NameDecimalOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final BigDecimal value;

      public NameDecimalOpSegment(String name, long nameHashCode, JSONPathFilter.Operator operator, BigDecimal value) {
         super(name, nameHashCode);
         this.operator = operator;
         this.value = value;
      }

      @Override
      protected boolean applyNull() {
         return this.operator == JSONPathFilter.Operator.NE;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (fieldValue == null) {
            return false;
         } else {
            BigDecimal fieldValueDecimal;
            if (fieldValue instanceof Boolean) {
               fieldValueDecimal = (Boolean)fieldValue ? BigDecimal.ONE : BigDecimal.ZERO;
            } else if (fieldValue instanceof Byte || fieldValue instanceof Short || fieldValue instanceof Integer || fieldValue instanceof Long) {
               fieldValueDecimal = BigDecimal.valueOf(((Number)fieldValue).longValue());
            } else if (fieldValue instanceof BigDecimal) {
               fieldValueDecimal = (BigDecimal)fieldValue;
            } else {
               if (!(fieldValue instanceof BigInteger)) {
                  throw new UnsupportedOperationException();
               }

               fieldValueDecimal = new BigDecimal((BigInteger)fieldValue);
            }

            int cmp = fieldValueDecimal.compareTo(this.value);
            switch (this.operator) {
               case EQ:
                  return cmp == 0;
               case NE:
                  return cmp != 0;
               case GT:
                  return cmp > 0;
               case GE:
                  return cmp >= 0;
               case LT:
                  return cmp < 0;
               case LE:
                  return cmp <= 0;
               default:
                  throw new UnsupportedOperationException();
            }
         }
      }
   }

   static final class NameDoubleOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final double value;

      public NameDoubleOpSegment(String name, long nameHashCode, JSONPathFilter.Operator operator, Double value) {
         super(name, nameHashCode);
         this.operator = operator;
         this.value = value;
      }

      @Override
      protected boolean applyNull() {
         return this.operator == JSONPathFilter.Operator.NE;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (fieldValue == null) {
            return false;
         } else {
            Double fieldValueDouble;
            if (fieldValue instanceof Boolean) {
               fieldValueDouble = (Boolean)fieldValue ? 1.0 : 0.0;
            } else {
               if (!(fieldValue instanceof Number)) {
                  throw new UnsupportedOperationException();
               }

               fieldValueDouble = ((Number)fieldValue).doubleValue();
            }

            int cmp = fieldValueDouble.compareTo(this.value);
            switch (this.operator) {
               case EQ:
                  return cmp == 0;
               case NE:
                  return cmp != 0;
               case GT:
                  return cmp > 0;
               case GE:
                  return cmp >= 0;
               case LT:
                  return cmp < 0;
               case LE:
                  return cmp <= 0;
               default:
                  throw new UnsupportedOperationException();
            }
         }
      }
   }

   static final class NameExistsFilter extends JSONPathFilter {
      final String name;
      final long nameHashCode;

      public NameExistsFilter(String name, long nameHashCode) {
         this.name = name;
         this.nameHashCode = nameHashCode;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         JSONArray array = new JSONArray();
         if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (item instanceof Map && ((Map)item).containsKey(this.name)) {
                  array.add(item);
               }
            }

            context.value = array;
         } else if (object instanceof Map) {
            Map map = (Map)object;
            Object value = map.get(this.name);
            context.value = value != null ? object : null;
         } else if (object instanceof JSONPath.Sequence) {
            List list = ((JSONPath.Sequence)object).values;

            for (int ix = 0; ix < list.size(); ix++) {
               Object item = list.get(ix);
               if (item instanceof Map && ((Map)item).containsKey(this.name)) {
                  array.add(item);
               }
            }

            if (context.next != null) {
               context.value = new JSONPath.Sequence(array);
            } else {
               context.value = array;
            }
         } else {
            throw new UnsupportedOperationException();
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      @Override
      public String toString() {
         return '?' + this.name;
      }

      @Override
      public boolean apply(JSONPath.Context context, Object object) {
         if (object instanceof Map) {
            return ((Map)object).containsKey(this.name);
         } else {
            throw new UnsupportedOperationException();
         }
      }
   }

   abstract static class NameFilter extends JSONPathFilter {
      final String fieldName;
      final long fieldNameNameHash;
      final String[] fieldName2;
      final long[] fieldNameNameHash2;
      final Function function;
      boolean includeArray = true;

      public NameFilter(String fieldName, long fieldNameNameHash) {
         this.fieldName = fieldName;
         this.fieldNameNameHash = fieldNameNameHash;
         this.fieldName2 = null;
         this.fieldNameNameHash2 = null;
         this.function = null;
      }

      public NameFilter(String fieldName, long fieldNameNameHash, String[] fieldName2, long[] fieldNameNameHash2, Function function) {
         this.fieldName = fieldName;
         this.fieldNameNameHash = fieldNameNameHash;
         this.fieldName2 = fieldName2;
         this.fieldNameNameHash2 = fieldNameNameHash2;
         this.function = function;
      }

      abstract boolean apply(Object var1);

      @Override
      public final void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent == null) {
            context.root = jsonReader.readAny();
         }

         this.eval(context);
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;

            for (int i = list.size() - 1; i >= 0; i--) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  list.remove(i);
               }
            }

            return true;
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public final void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            JSONArray array = new JSONArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else if (object instanceof Object[]) {
            Object[] list = (Object[])object;
            JSONArray array = new JSONArray(list.length);

            for (Object item : list) {
               if (this.apply(context, item)) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else if (!(object instanceof JSONPath.Sequence)) {
            if (this.apply(context, object)) {
               context.value = object;
               context.eval = true;
            }
         } else {
            JSONPath.Sequence sequence = (JSONPath.Sequence)object;
            JSONArray array = new JSONArray();

            for (Object value : sequence.values) {
               if (this.includeArray && value instanceof Collection) {
                  for (Object valueItem : (Collection)value) {
                     if (this.apply(context, valueItem)) {
                        array.add(valueItem);
                     }
                  }
               } else if (this.apply(context, value)) {
                  array.add(value);
               }
            }

            context.value = array;
            context.eval = true;
         }
      }

      protected boolean applyNull() {
         return false;
      }

      @Override
      public boolean apply(JSONPath.Context context, Object object) {
         if (object == null) {
            return false;
         } else {
            JSONWriter.Context writerContext = context.path.getWriterContext();
            if (object instanceof Map) {
               Object fieldValue = this.fieldName == null ? object : ((Map)object).get(this.fieldName);
               if (fieldValue == null) {
                  return this.applyNull();
               } else {
                  if (this.fieldName2 != null) {
                     for (int i = 0; i < this.fieldName2.length; i++) {
                        String name = this.fieldName2[i];
                        if (fieldValue instanceof Map) {
                           fieldValue = ((Map)fieldValue).get(name);
                        } else {
                           ObjectWriter objectWriter2 = writerContext.getObjectWriter(fieldValue.getClass());
                           if (!(objectWriter2 instanceof ObjectWriterAdapter)) {
                              return false;
                           }

                           FieldWriter fieldWriter2 = objectWriter2.getFieldWriter(this.fieldNameNameHash2[i]);
                           if (fieldWriter2 == null) {
                              return false;
                           }

                           fieldValue = fieldWriter2.getFieldValue(fieldValue);
                        }

                        if (fieldValue == null) {
                           return this instanceof JSONPathFilter.NameIsNull;
                        }
                     }
                  }

                  if (this.function != null) {
                     fieldValue = this.function.apply(fieldValue);
                  }

                  return this.apply(fieldValue);
               }
            } else {
               ObjectWriter objectWriter = writerContext.getObjectWriter(object.getClass());
               if (objectWriter instanceof ObjectWriterAdapter) {
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(this.fieldNameNameHash);
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  if (fieldValue == null) {
                     return false;
                  } else {
                     if (this.fieldName2 != null) {
                        for (int i = 0; i < this.fieldName2.length; i++) {
                           String namex = this.fieldName2[i];
                           if (fieldValue instanceof Map) {
                              fieldValue = ((Map)fieldValue).get(namex);
                           } else {
                              ObjectWriter objectWriter2x = writerContext.getObjectWriter(fieldValue.getClass());
                              if (!(objectWriter2x instanceof ObjectWriterAdapter)) {
                                 return false;
                              }

                              FieldWriter fieldWriter2 = objectWriter2x.getFieldWriter(this.fieldNameNameHash2[i]);
                              if (fieldWriter2 == null) {
                                 return false;
                              }

                              fieldValue = fieldWriter2.getFieldValue(fieldValue);
                           }

                           if (fieldValue == null) {
                              return false;
                           }
                        }
                     }

                     if (this.function != null) {
                        fieldValue = this.function.apply(fieldValue);
                     }

                     return this.apply(fieldValue);
                  }
               } else if (this.function != null) {
                  Object fieldValue = this.function.apply(object);
                  return this.apply(fieldValue);
               } else {
                  return this.fieldName == null ? this.apply(object) : false;
               }
            }
         }
      }

      protected void excludeArray() {
         this.includeArray = false;
      }
   }

   static final class NameIntBetweenSegment extends JSONPathFilter.NameFilter {
      private final long begin;
      private final long end;
      private final boolean not;

      public NameIntBetweenSegment(String fieldName, long fieldNameNameHash, long begin, long end, boolean not) {
         super(fieldName, fieldNameNameHash);
         this.begin = begin;
         this.end = end;
         this.not = not;
      }

      @Override
      protected boolean applyNull() {
         return this.not;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (fieldValue instanceof Byte || fieldValue instanceof Short || fieldValue instanceof Integer || fieldValue instanceof Long) {
            long fieldValueLong = ((Number)fieldValue).longValue();
            return fieldValueLong >= this.begin && fieldValueLong <= this.end ? !this.not : this.not;
         } else if (fieldValue instanceof Float || fieldValue instanceof Double) {
            double fieldValueDouble = ((Number)fieldValue).doubleValue();
            return fieldValueDouble >= (double)this.begin && fieldValueDouble <= (double)this.end ? !this.not : this.not;
         } else if (fieldValue instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)fieldValue;
            int cmpBegin = decimal.compareTo(BigDecimal.valueOf(this.begin));
            int cmpEnd = decimal.compareTo(BigDecimal.valueOf(this.end));
            return cmpBegin >= 0 && cmpEnd <= 0 ? !this.not : this.not;
         } else if (fieldValue instanceof BigInteger) {
            BigInteger bigInt = (BigInteger)fieldValue;
            int cmpBegin = bigInt.compareTo(BigInteger.valueOf(this.begin));
            int cmpEnd = bigInt.compareTo(BigInteger.valueOf(this.end));
            return cmpBegin >= 0 && cmpEnd <= 0 ? !this.not : this.not;
         } else {
            return this.not;
         }
      }
   }

   static final class NameIntInSegment extends JSONPathFilter.NameFilter {
      private final long[] values;
      private final boolean not;

      public NameIntInSegment(
         String fieldName, long fieldNameNameHash, String[] fieldName2, long[] fieldNameNameHash2, Function expr, long[] values, boolean not
      ) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, expr);
         this.values = values;
         this.not = not;
      }

      @Override
      protected boolean applyNull() {
         return this.not;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (!(fieldValue instanceof Byte) && !(fieldValue instanceof Short) && !(fieldValue instanceof Integer) && !(fieldValue instanceof Long)) {
            if (!(fieldValue instanceof Float) && !(fieldValue instanceof Double)) {
               if (fieldValue instanceof BigDecimal) {
                  BigDecimal decimal = (BigDecimal)fieldValue;
                  long longValue = decimal.longValue();

                  for (long value : this.values) {
                     if (value == longValue && decimal.compareTo(BigDecimal.valueOf(value)) == 0) {
                        return !this.not;
                     }
                  }

                  return this.not;
               } else if (fieldValue instanceof BigInteger) {
                  BigInteger bigiInt = (BigInteger)fieldValue;
                  long longValue = bigiInt.longValue();

                  for (long valuex : this.values) {
                     if (valuex == longValue && bigiInt.equals(BigInteger.valueOf(valuex))) {
                        return !this.not;
                     }
                  }

                  return this.not;
               } else {
                  return this.not;
               }
            } else {
               double fieldValueDouble = ((Number)fieldValue).doubleValue();

               for (long valuexx : this.values) {
                  if ((double)valuexx == fieldValueDouble) {
                     return !this.not;
                  }
               }

               return this.not;
            }
         } else {
            long fieldValueLong = ((Number)fieldValue).longValue();

            for (long valuexxx : this.values) {
               if (valuexxx == fieldValueLong) {
                  return !this.not;
               }
            }

            return this.not;
         }
      }
   }

   static final class NameIntOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final long value;

      public NameIntOpSegment(
         String name, long nameHashCode, String[] fieldName2, long[] fieldNameNameHash2, Function expr, JSONPathFilter.Operator operator, long value
      ) {
         super(name, nameHashCode, fieldName2, fieldNameNameHash2, expr);
         this.operator = operator;
         this.value = value;
      }

      @Override
      protected boolean applyNull() {
         return this.operator == JSONPathFilter.Operator.NE;
      }

      @Override
      public boolean apply(Object fieldValue) {
         boolean objInt = fieldValue instanceof Boolean
            || fieldValue instanceof Byte
            || fieldValue instanceof Short
            || fieldValue instanceof Integer
            || fieldValue instanceof Long;
         if (objInt) {
            long fieldValueInt;
            if (fieldValue instanceof Boolean) {
               fieldValueInt = (Boolean)fieldValue ? 1L : 0L;
            } else {
               fieldValueInt = ((Number)fieldValue).longValue();
            }

            switch (this.operator) {
               case EQ:
                  return fieldValueInt == this.value;
               case NE:
                  return fieldValueInt != this.value;
               case GT:
                  return fieldValueInt > this.value;
               case GE:
                  return fieldValueInt >= this.value;
               case LT:
                  return fieldValueInt < this.value;
               case LE:
                  return fieldValueInt <= this.value;
               default:
                  throw new UnsupportedOperationException();
            }
         } else {
            int cmp;
            if (fieldValue instanceof BigDecimal) {
               cmp = ((BigDecimal)fieldValue).compareTo(BigDecimal.valueOf(this.value));
            } else if (fieldValue instanceof BigInteger) {
               cmp = ((BigInteger)fieldValue).compareTo(BigInteger.valueOf(this.value));
            } else if (fieldValue instanceof Float) {
               cmp = ((Float)fieldValue).compareTo((float)this.value);
            } else if (fieldValue instanceof Double) {
               cmp = ((Double)fieldValue).compareTo((double)this.value);
            } else {
               if (!(fieldValue instanceof String)) {
                  throw new UnsupportedOperationException();
               }

               String fieldValueStr = (String)fieldValue;
               if (IOUtils.isNumber(fieldValueStr)) {
                  try {
                     cmp = Long.compare(Long.parseLong(fieldValueStr), this.value);
                  } catch (Exception var6) {
                     cmp = fieldValueStr.compareTo(Long.toString(this.value));
                  }
               } else {
                  cmp = fieldValueStr.compareTo(Long.toString(this.value));
               }
            }

            switch (this.operator) {
               case EQ:
                  return cmp == 0;
               case NE:
                  return cmp != 0;
               case GT:
                  return cmp > 0;
               case GE:
                  return cmp >= 0;
               case LT:
                  return cmp < 0;
               case LE:
                  return cmp <= 0;
               default:
                  throw new UnsupportedOperationException();
            }
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  list.set(i, value);
               }
            }
         } else {
            throw new JSONException("UnsupportedOperation ");
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  Object value = callback.apply(list, item);
                  if (value != item) {
                     list.set(i, value);
                  }
               }
            }
         } else {
            throw new JSONException("UnsupportedOperation ");
         }
      }

      @Override
      public String toString() {
         return "[?(" + (this.fieldName2 == null ? "@" : this.fieldName2) + '.' + this.fieldName + ' ' + this.operator + ' ' + this.value + ")]";
      }
   }

   static final class NameIsNull extends JSONPathFilter.NameFilter {
      public NameIsNull(String fieldName, long fieldNameNameHash, String[] fieldName2, long[] fieldNameNameHash2, Function function) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, function);
      }

      @Override
      protected boolean applyNull() {
         return true;
      }

      @Override
      boolean apply(Object fieldValue) {
         if (this.function != null) {
            fieldValue = this.function.apply(fieldValue);
         }

         return fieldValue == null;
      }
   }

   static final class NameLongContainsSegment extends JSONPathFilter.NameFilter {
      private final long[] values;
      private final boolean not;

      public NameLongContainsSegment(String fieldName, long fieldNameNameHash, String[] fieldName2, long[] fieldNameNameHash2, long[] values, boolean not) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, null);
         this.values = values;
         this.not = not;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (fieldValue instanceof Collection) {
            Collection collection = (Collection)fieldValue;
            boolean containsAll = true;

            for (long value : this.values) {
               boolean containsItem = false;

               for (Object item : collection) {
                  if (item instanceof Byte || item instanceof Short || item instanceof Integer || item instanceof Long) {
                     long longItem = ((Number)item).longValue();
                     if (longItem == value) {
                        containsItem = true;
                        break;
                     }
                  }

                  if (item instanceof Float && (float)value == (Float)item) {
                     containsItem = true;
                     break;
                  }

                  if (item instanceof Double && (double)value == (Double)item) {
                     containsItem = true;
                     break;
                  }

                  if (item instanceof BigDecimal) {
                     BigDecimal decimal = (BigDecimal)item;
                     long longValue = decimal.longValue();
                     if (value == longValue && decimal.compareTo(BigDecimal.valueOf(value)) == 0) {
                        containsItem = true;
                        break;
                     }
                  }

                  if (item instanceof BigInteger) {
                     BigInteger bigiInt = (BigInteger)item;
                     long longValue = bigiInt.longValue();
                     if (value == longValue && bigiInt.equals(BigInteger.valueOf(value))) {
                        containsItem = true;
                        break;
                     }
                  }
               }

               if (!containsItem) {
                  containsAll = false;
                  break;
               }
            }

            if (containsAll) {
               return !this.not;
            }
         }

         return this.not;
      }
   }

   static final class NameMatchFilter extends JSONPathFilter.NameFilter {
      final String startsWithValue;
      final String endsWithValue;
      final String[] containsValues;
      final int minLength;
      final boolean not;

      public NameMatchFilter(String fieldName, long fieldNameNameHash, String startsWithValue, String endsWithValue, String[] containsValues, boolean not) {
         super(fieldName, fieldNameNameHash);
         this.startsWithValue = startsWithValue;
         this.endsWithValue = endsWithValue;
         this.containsValues = containsValues;
         this.not = not;
         int len = 0;
         if (startsWithValue != null) {
            len += startsWithValue.length();
         }

         if (endsWithValue != null) {
            len += endsWithValue.length();
         }

         if (containsValues != null) {
            for (String item : containsValues) {
               len += item.length();
            }
         }

         this.minLength = len;
      }

      @Override
      boolean apply(Object arg) {
         if (!(arg instanceof String)) {
            return false;
         } else {
            String fieldValue = (String)arg;
            if (fieldValue.length() < this.minLength) {
               return this.not;
            } else {
               int start = 0;
               if (this.startsWithValue != null) {
                  if (!fieldValue.startsWith(this.startsWithValue)) {
                     return this.not;
                  }

                  start += this.startsWithValue.length();
               }

               if (this.containsValues != null) {
                  for (String containsValue : this.containsValues) {
                     int index = fieldValue.indexOf(containsValue, start);
                     if (index == -1) {
                        return this.not;
                     }

                     start = index + containsValue.length();
                  }
               }

               return this.endsWithValue != null && !fieldValue.endsWith(this.endsWithValue) ? this.not : !this.not;
            }
         }
      }
   }

   static final class NameName extends JSONPathFilter.NameFilter {
      final String fieldName1;
      final long fieldNameName1Hash;

      public NameName(String fieldName, long fieldNameNameHash, String fieldName1, long fieldNameName1Hash) {
         super(fieldName, fieldNameNameHash);
         this.fieldName1 = fieldName1;
         this.fieldNameName1Hash = fieldNameName1Hash;
      }

      @Override
      public boolean apply(JSONPath.Context context, Object object) {
         if (object == null) {
            return false;
         } else {
            JSONWriter.Context writerContext = context.path.getWriterContext();
            Object fieldValue;
            Object fieldValue1;
            if (object instanceof Map) {
               Map map = (Map)object;
               fieldValue = map.get(this.fieldName);
               fieldValue1 = map.get(this.fieldName1);
            } else {
               ObjectWriter objectWriter = writerContext.getObjectWriter(object.getClass());
               if (!(objectWriter instanceof ObjectWriterAdapter)) {
                  return false;
               }

               FieldWriter fieldWriter = objectWriter.getFieldWriter(this.fieldNameNameHash);
               if (fieldWriter == null) {
                  return false;
               }

               fieldValue = fieldWriter.getFieldValue(object);
               FieldWriter fieldWriter1 = objectWriter.getFieldWriter(this.fieldNameNameHash);
               if (fieldWriter1 == null) {
                  return false;
               }

               fieldValue1 = fieldWriter1.getFieldValue(object);
            }

            return Objects.equals(fieldValue, fieldValue1);
         }
      }

      @Override
      boolean apply(Object fieldValue) {
         throw new JSONException("TODO");
      }
   }

   static final class NameObjectOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final JSONObject object;

      public NameObjectOpSegment(
         String fieldName,
         long fieldNameNameHash,
         String[] fieldName2,
         long[] fieldNameNameHash2,
         Function function,
         JSONPathFilter.Operator operator,
         JSONObject object
      ) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, function);
         this.operator = operator;
         this.object = object;
      }

      @Override
      boolean apply(Object fieldValue) {
         if (Objects.requireNonNull(this.operator) == JSONPathFilter.Operator.EQ) {
            return this.object.equals(fieldValue);
         } else {
            throw new JSONException("not support operator : " + this.operator);
         }
      }
   }

   static final class NameRLikeSegment extends JSONPathFilter.NameFilter {
      final Pattern pattern;
      final boolean not;

      public NameRLikeSegment(String fieldName, long fieldNameNameHash, Pattern pattern, boolean not) {
         super(fieldName, fieldNameNameHash);
         this.pattern = pattern;
         this.not = not;
      }

      @Override
      boolean apply(Object fieldValue) {
         String strPropertyValue = fieldValue.toString();
         Matcher m = this.pattern.matcher(strPropertyValue);
         boolean match = m.matches();
         if (this.not) {
            match = !match;
         }

         return match;
      }
   }

   static final class NameStringContainsSegment extends JSONPathFilter.NameFilter {
      private final String[] values;
      private final boolean not;

      public NameStringContainsSegment(String fieldName, long fieldNameNameHash, String[] fieldName2, long[] fieldNameNameHash2, String[] values, boolean not) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, null);
         this.values = values;
         this.not = not;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (fieldValue instanceof Collection) {
            Collection collection = (Collection)fieldValue;
            boolean containsAll = true;

            for (String value : this.values) {
               if (!collection.contains(value)) {
                  containsAll = false;
                  break;
               }
            }

            if (containsAll) {
               return !this.not;
            }
         }

         return this.not;
      }
   }

   static final class NameStringInSegment extends JSONPathFilter.NameFilter {
      private final String[] values;
      private final boolean not;

      public NameStringInSegment(String fieldName, long fieldNameNameHash, String[] values, boolean not) {
         super(fieldName, fieldNameNameHash);
         this.values = values;
         this.not = not;
      }

      @Override
      protected boolean applyNull() {
         return this.not;
      }

      @Override
      public boolean apply(Object fieldValue) {
         for (String value : this.values) {
            if (value == fieldValue) {
               return !this.not;
            }

            if (value != null && value.equals(fieldValue)) {
               return !this.not;
            }
         }

         return this.not;
      }
   }

   static final class NameStringOpSegment extends JSONPathFilter.NameFilter {
      final JSONPathFilter.Operator operator;
      final String value;

      public NameStringOpSegment(
         String fieldName,
         long fieldNameNameHash,
         String[] fieldName2,
         long[] fieldNameNameHash2,
         Function expr,
         JSONPathFilter.Operator operator,
         String value
      ) {
         super(fieldName, fieldNameNameHash, fieldName2, fieldNameNameHash2, expr);
         this.operator = operator;
         this.value = value;
      }

      @Override
      protected boolean applyNull() {
         return this.operator == JSONPathFilter.Operator.NE;
      }

      @Override
      public boolean apply(Object fieldValue) {
         if (!(fieldValue instanceof String)) {
            return false;
         } else {
            String fieldValueStr = (String)fieldValue;
            if (this.operator == JSONPathFilter.Operator.STARTS_WITH) {
               return fieldValueStr.startsWith(this.value);
            } else if (this.operator == JSONPathFilter.Operator.ENDS_WITH) {
               return fieldValueStr.endsWith(this.value);
            } else {
               int cmp = fieldValueStr.compareTo(this.value);
               switch (this.operator) {
                  case EQ:
                     return cmp == 0;
                  case NE:
                     return cmp != 0;
                  case GT:
                     return cmp > 0;
                  case GE:
                     return cmp >= 0;
                  case LT:
                     return cmp < 0;
                  case LE:
                     return cmp <= 0;
                  default:
                     throw new UnsupportedOperationException();
               }
            }
         }
      }
   }

   static final class NamesExistsFilter extends JSONPathFilter {
      final String[] names;
      final long[] nameHashCodes;

      public NamesExistsFilter(List<String> names) {
         this.names = names.toArray(new String[0]);
         long[] nameHashCodes = new long[this.names.length];

         for (int i = 0; i < nameHashCodes.length; i++) {
            nameHashCodes[i] = Fnv.hashCode64(this.names[i]);
         }

         this.nameHashCodes = nameHashCodes;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object first = context.parent == null ? context.root : context.parent.value;
         Object object = first;

         for (int i = 0; i < this.names.length; i++) {
            String name = this.names[i];
            if (object instanceof Map) {
               Map map = (Map)object;
               Object value = map.get(name);
               if (i == this.names.length - 1 || value == null) {
                  context.value = value != null ? first : null;
                  return;
               }

               object = value;
            }
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      @Override
      public String toString() {
         StringBuilder buf = new StringBuilder("exists(@");

         for (int i = 0; i < this.names.length; i++) {
            buf.append('.');
            buf.append(this.names[i]);
         }

         buf.append(')');
         return buf.toString();
      }

      @Override
      public boolean apply(JSONPath.Context context, Object object) {
         throw new UnsupportedOperationException();
      }
   }

   static enum Operator {
      EQ,
      NE,
      GT,
      GE,
      LT,
      LE,
      LIKE,
      NOT_LIKE,
      RLIKE,
      NOT_RLIKE,
      IN,
      NOT_IN,
      BETWEEN,
      NOT_BETWEEN,
      AND,
      OR,
      REG_MATCH,
      STARTS_WITH,
      ENDS_WITH,
      CONTAINS,
      NOT_CONTAINS;

      @Override
      public String toString() {
         switch (this) {
            case EQ:
               return "==";
            case NE:
               return "!=";
            case GT:
               return ">";
            case GE:
               return ">=";
            case LT:
               return "<";
            case LE:
               return "<=";
            case LIKE:
               return "like";
            case NOT_LIKE:
               return "not like";
            case RLIKE:
               return "rlike";
            case NOT_RLIKE:
               return "not rlike";
            case IN:
            case NOT_IN:
            case REG_MATCH:
            default:
               return this.name();
            case BETWEEN:
               return "between";
            case NOT_BETWEEN:
               return "not between";
            case AND:
               return "and";
            case OR:
               return "or";
            case STARTS_WITH:
               return "starts with";
            case ENDS_WITH:
               return "ends with";
            case CONTAINS:
               return "contains";
            case NOT_CONTAINS:
               return "not contains";
         }
      }
   }

   static final class RangeIndexSegmentFilter extends JSONPathFilter {
      final JSONPathSegment.RangeIndexSegment expr;
      final JSONPathFilter.Operator operator;
      final Object value;

      public RangeIndexSegmentFilter(JSONPathSegment.RangeIndexSegment expr, JSONPathFilter.Operator operator, Object value) {
         this.expr = expr;
         this.operator = operator;
         this.value = value;
      }

      @Override
      boolean apply(JSONPath.Context context, Object object) {
         if (object == null) {
            return false;
         } else {
            JSONPath.Context context0 = new JSONPath.Context(null, null, this.expr, null, 0L);
            context0.root = object;
            this.expr.eval(context0);
            List result = (List)context0.value;

            for (int i = 0; i < result.size(); i++) {
               Object item = result.get(i);
               int cmp = TypeUtils.compare(item, this.value);
               boolean itemResult;
               switch (this.operator) {
                  case EQ:
                     itemResult = cmp == 0;
                     break;
                  case NE:
                     itemResult = cmp != 0;
                     break;
                  case GT:
                     itemResult = cmp > 0;
                     break;
                  case GE:
                     itemResult = cmp >= 0;
                     break;
                  case LT:
                     itemResult = cmp < 0;
                     break;
                  case LE:
                     itemResult = cmp <= 0;
                     break;
                  default:
                     throw new UnsupportedOperationException();
               }

               if (!itemResult) {
                  return false;
               }
            }

            return true;
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         throw new JSONException("UnsupportedOperation " + this.getClass());
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            JSONArray array = new JSONArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else {
            throw new JSONException("UnsupportedOperation " + object.getClass());
         }
      }
   }

   static final class Segment2Filter extends JSONPathFilter {
      final JSONPathSegment left;
      final JSONPathFilter.Operator operator;
      final JSONPathSegment right;

      public Segment2Filter(JSONPathSegment left, JSONPathFilter.Operator operator, JSONPathSegment right) {
         this.left = left;
         this.operator = operator;
         this.right = right;
      }

      @Override
      boolean apply(JSONPath.Context context, Object object) {
         if (object == null) {
            return false;
         } else {
            JSONPath.Context context0 = new JSONPath.Context(null, null, this.left, null, 0L);
            context0.root = object;
            this.left.eval(context0);
            Object result0 = context0.value;
            JSONPath.Context context1 = new JSONPath.Context(null, null, this.right, null, 0L);
            context1.root = object;
            this.right.eval(context1);
            Object result1 = context1.value;
            int cmp = TypeUtils.compare(result0, result1);
            switch (this.operator) {
               case EQ:
                  return cmp == 0;
               case NE:
                  return cmp != 0;
               case GT:
                  return cmp > 0;
               case GE:
                  return cmp >= 0;
               case LT:
                  return cmp < 0;
               case LE:
                  return cmp <= 0;
               default:
                  throw new UnsupportedOperationException();
            }
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         throw new JSONException("UnsupportedOperation " + this.getClass());
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            JSONArray array = new JSONArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else {
            throw new JSONException("UnsupportedOperation " + object.getClass());
         }
      }
   }

   static final class SegmentFilter extends JSONPathFilter {
      final JSONPathSegment expr;
      final JSONPathFilter.Operator operator;
      final Object value;

      public SegmentFilter(JSONPathSegment expr, JSONPathFilter.Operator operator, Object value) {
         this.expr = expr;
         this.operator = operator;
         this.value = value;
      }

      @Override
      boolean apply(JSONPath.Context context, Object object) {
         if (object == null) {
            return false;
         } else {
            JSONPath.Context context0 = new JSONPath.Context(null, null, this.expr, null, 0L);
            context0.root = object;
            this.expr.eval(context0);
            Object result = context0.value;
            int cmp = TypeUtils.compare(result, this.value);
            switch (this.operator) {
               case EQ:
                  return cmp == 0;
               case NE:
                  return cmp != 0;
               case GT:
                  return cmp > 0;
               case GE:
                  return cmp >= 0;
               case LT:
                  return cmp < 0;
               case LE:
                  return cmp <= 0;
               default:
                  throw new UnsupportedOperationException();
            }
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         throw new JSONException("UnsupportedOperation " + this.getClass());
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            JSONArray array = new JSONArray(list.size());

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  array.add(item);
               }
            }

            context.value = array;
            context.eval = true;
         } else {
            throw new JSONException("UnsupportedOperation " + object.getClass());
         }
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;

            for (int i = list.size() - 1; i >= 0; i--) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  list.remove(i);
               }
            }

            return true;
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < list.size(); i++) {
               Object item = list.get(i);
               if (this.apply(context, item)) {
                  list.set(i, value);
               }
            }
         } else {
            throw new JSONException("UnsupportedOperation ");
         }
      }
   }

   static final class StartsWithSegment extends JSONPathFilter.NameFilter {
      final String prefix;

      public StartsWithSegment(String fieldName, long fieldNameNameHash, String prefix) {
         super(fieldName, fieldNameNameHash);
         this.prefix = prefix;
      }

      @Override
      boolean apply(Object fieldValue) {
         String propertyValue = fieldValue.toString();
         return propertyValue != null && propertyValue.startsWith(this.prefix);
      }
   }
}
