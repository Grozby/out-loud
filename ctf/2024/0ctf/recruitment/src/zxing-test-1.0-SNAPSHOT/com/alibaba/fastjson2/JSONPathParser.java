package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

class JSONPathParser {
   final String path;
   final JSONReader jsonReader;
   boolean dollar;
   boolean lax;
   boolean strict;
   int segmentIndex;
   JSONPathSegment first;
   JSONPathSegment second;
   List<JSONPathSegment> segments;
   int filterNests;
   boolean negative;

   public JSONPathParser(String str) {
      this.jsonReader = JSONReader.of(this.path = str, JSONPath.PARSE_CONTEXT);
      if (this.jsonReader.ch == 'l' && this.jsonReader.nextIfMatchIdent('l', 'a', 'x')) {
         this.lax = true;
      } else if (this.jsonReader.ch == 's' && this.jsonReader.nextIfMatchIdent('s', 't', 'r', 'i', 'c', 't')) {
         this.strict = true;
      }

      if (this.jsonReader.ch == '-') {
         this.jsonReader.next();
         this.negative = true;
      }

      if (this.jsonReader.ch == '$') {
         this.jsonReader.next();
         this.dollar = true;
      }
   }

   JSONPath parse(JSONPath.Feature... features) {
      if (this.dollar && this.jsonReader.ch == 26) {
         return (JSONPath)(this.negative ? new JSONPathSingle(JSONPathFunction.FUNC_NEGATIVE, this.path) : JSONPath.RootPath.INSTANCE);
      } else if (this.jsonReader.ch == 'e' && this.jsonReader.nextIfMatchIdent('e', 'x', 'i', 's', 't', 's')) {
         if (!this.jsonReader.nextIfMatch('(')) {
            throw new JSONException("syntax error " + this.path);
         } else {
            if (this.jsonReader.ch == '@') {
               this.jsonReader.next();
               if (!this.jsonReader.nextIfMatch('.')) {
                  throw new JSONException("syntax error " + this.path);
               }
            }

            char ch = this.jsonReader.ch;
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && ch != '_' && ch != '@' && !Character.isIdeographic(ch)) {
               throw new JSONException("syntax error " + this.path);
            } else {
               JSONPathSegment segment = this.parseProperty();
               if (!this.jsonReader.nextIfMatch(')')) {
                  throw new JSONException("syntax error " + this.path);
               } else {
                  return new JSONPathTwoSegment(this.path, segment, JSONPathFunction.FUNC_EXISTS);
               }
            }
         }
      } else {
         for (; this.jsonReader.ch != 26; this.segmentIndex++) {
            char ch = this.jsonReader.ch;
            JSONPathSegment segment;
            if (ch == '.') {
               this.jsonReader.next();
               segment = this.parseProperty();
            } else if (this.jsonReader.ch == '[') {
               segment = this.parseArrayAccess();
            } else if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && ch != '_' && !Character.isIdeographic(ch)) {
               if (ch == '?') {
                  if (this.dollar && this.segmentIndex == 0) {
                     this.first = JSONPathSegment.RootSegment.INSTANCE;
                     this.segmentIndex++;
                  }

                  this.jsonReader.next();
                  segment = this.parseFilter();
               } else {
                  if (ch != '@') {
                     throw new JSONException("not support " + ch);
                  }

                  this.jsonReader.next();
                  segment = JSONPathSegment.SelfSegment.INSTANCE;
               }
            } else {
               segment = this.parseProperty();
            }

            if (this.segmentIndex == 0) {
               this.first = segment;
            } else if (this.segmentIndex == 1) {
               this.second = segment;
            } else if (this.segmentIndex == 2) {
               this.segments = new ArrayList<>();
               this.segments.add(this.first);
               this.segments.add(this.second);
               this.segments.add(segment);
            } else {
               this.segments.add(segment);
            }
         }

         if (this.negative) {
            if (this.segmentIndex == 1) {
               this.second = JSONPathFunction.FUNC_NEGATIVE;
            } else if (this.segmentIndex == 2) {
               this.segments = new ArrayList<>();
               this.segments.add(this.first);
               this.segments.add(this.second);
               this.segments.add(JSONPathFunction.FUNC_NEGATIVE);
            } else {
               this.segments.add(JSONPathFunction.FUNC_NEGATIVE);
            }

            this.segmentIndex++;
         }

         if (this.segmentIndex == 1) {
            if (this.first instanceof JSONPathSegmentName) {
               return new JSONPathSingleName(this.path, (JSONPathSegmentName)this.first, features);
            } else {
               if (this.first instanceof JSONPathSegmentIndex) {
                  JSONPathSegmentIndex firstIndex = (JSONPathSegmentIndex)this.first;
                  if (firstIndex.index >= 0) {
                     return new JSONPathSingleIndex(this.path, firstIndex, features);
                  }
               }

               return new JSONPathSingle(this.first, this.path, features);
            }
         } else {
            return (JSONPath)(this.segmentIndex == 2
               ? new JSONPathTwoSegment(this.path, this.first, this.second, features)
               : new JSONPathMulti(this.path, this.segments, features));
         }
      }
   }

   private JSONPathSegment parseArrayAccess() {
      JSONPathSegment segment;
      this.jsonReader.next();
      label154:
      switch (this.jsonReader.ch) {
         case '"':
         case '\'':
            String name = this.jsonReader.readString();
            if (this.jsonReader.current() == ']') {
               segment = new JSONPathSegmentName(name, Fnv.hashCode64(name));
            } else {
               if (!this.jsonReader.isString()) {
                  throw new JSONException("TODO : " + this.jsonReader.current());
               }

               List<String> names = new ArrayList<>();
               names.add(name);

               do {
                  names.add(this.jsonReader.readString());
               } while (this.jsonReader.isString());

               String[] nameArray = new String[names.size()];
               names.toArray(nameArray);
               segment = new JSONPathSegment.MultiNameSegment(nameArray);
            }
            break;
         case '#':
         case '$':
         case '%':
         case '&':
         case ')':
         case '+':
         case ',':
         case '.':
         case '/':
         case ';':
         case '<':
         case '=':
         case '>':
         case '@':
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         case 'Z':
         case '[':
         case '\\':
         case ']':
         case '^':
         case '_':
         case '`':
         case 'a':
         case 'b':
         case 'c':
         case 'd':
         case 'e':
         case 'f':
         case 'g':
         case 'h':
         case 'i':
         case 'j':
         case 'k':
         case 'm':
         case 'n':
         case 'o':
         case 'p':
         case 'q':
         default:
            throw new JSONException("TODO : " + this.jsonReader.current());
         case '(':
            this.jsonReader.next();
            if (!this.jsonReader.nextIfMatch('@') || !this.jsonReader.nextIfMatch('.')) {
               throw new JSONException("not support : " + this.path);
            }

            String fieldName = this.jsonReader.readFieldNameUnquote();
            switch (fieldName) {
               case "length":
               case "size":
                  int indexx = this.jsonReader.readInt32Value();
                  if (!this.jsonReader.nextIfMatch(')')) {
                     throw new JSONException("not support : " + fieldName);
                  }

                  if (indexx > 0) {
                     throw new JSONException("not support : " + fieldName);
                  }

                  segment = JSONPathSegmentIndex.of(indexx);
                  break label154;
               default:
                  throw new JSONException("not support : " + this.path);
            }
         case '*':
            this.jsonReader.next();
            segment = JSONPathSegment.AllSegment.INSTANCE_ARRAY;
            break;
         case '-':
         case '0':
         case '1':
         case '2':
         case '3':
         case '4':
         case '5':
         case '6':
         case '7':
         case '8':
         case '9':
            int index = this.jsonReader.readInt32Value();
            boolean last = false;
            if (this.jsonReader.ch == ':') {
               this.jsonReader.next();
               if (this.jsonReader.ch == ']') {
                  segment = new JSONPathSegment.RangeIndexSegment(index, index >= 0 ? Integer.MAX_VALUE : 0);
               } else {
                  int endx = this.jsonReader.readInt32Value();
                  segment = new JSONPathSegment.RangeIndexSegment(index, endx);
               }
            } else if (!this.jsonReader.isNumber() && !(last = this.jsonReader.nextIfMatchIdent('l', 'a', 's', 't'))) {
               segment = JSONPathSegmentIndex.of(index);
            } else {
               List<Integer> list = new ArrayList<>();
               list.add(index);
               if (last) {
                  list.add(-1);
                  this.jsonReader.nextIfComma();
               }

               while (true) {
                  while (!this.jsonReader.isNumber()) {
                     if (!this.jsonReader.nextIfMatchIdent('l', 'a', 's', 't')) {
                        int[] indics = new int[list.size()];

                        for (int i = 0; i < list.size(); i++) {
                           indics[i] = list.get(i);
                        }

                        segment = new JSONPathSegment.MultiIndexSegment(indics);
                        break label154;
                     }

                     list.add(-1);
                     this.jsonReader.nextIfComma();
                  }

                  index = this.jsonReader.readInt32Value();
                  list.add(index);
               }
            }
            break;
         case ':':
            this.jsonReader.next();
            int end = this.jsonReader.ch == ']' ? 0 : this.jsonReader.readInt32Value();
            if (end > 0) {
               segment = new JSONPathSegment.RangeIndexSegment(0, end);
            } else {
               segment = new JSONPathSegment.RangeIndexSegment(Integer.MIN_VALUE, end);
            }
            break;
         case '?':
            this.jsonReader.next();
            segment = this.parseFilter();
            break;
         case 'l':
            String fieldName = this.jsonReader.readFieldNameUnquote();
            if (!"last".equals(fieldName)) {
               throw new JSONException("not support : " + fieldName);
            }

            segment = JSONPathSegmentIndex.of(-1);
            break;
         case 'r':
            String fieldName = this.jsonReader.readFieldNameUnquote();
            if (!"randomIndex".equals(fieldName) || !this.jsonReader.nextIfMatch('(') || !this.jsonReader.nextIfMatch(')') || this.jsonReader.ch != ']') {
               throw new JSONException("not support : " + fieldName);
            }

            segment = JSONPathSegment.RandomIndexSegment.INSTANCE;
      }

      while (this.jsonReader.ch == '&' || this.jsonReader.ch == '|' || this.jsonReader.ch == 'a' || this.jsonReader.ch == 'o') {
         this.filterNests--;
         segment = this.parseFilterRest(segment);
      }

      while (this.filterNests > 0) {
         this.jsonReader.next();
         this.filterNests--;
      }

      if (!this.jsonReader.nextIfArrayEnd()) {
         throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
      } else {
         return segment;
      }
   }

   private JSONPathSegment parseProperty() {
      JSONPathSegment segment;
      if (this.jsonReader.ch == '*') {
         this.jsonReader.next();
         segment = JSONPathSegment.AllSegment.INSTANCE;
      } else if (this.jsonReader.ch == '.') {
         this.jsonReader.next();
         if (this.jsonReader.ch == '*') {
            this.jsonReader.next();
            segment = new JSONPathSegment.CycleNameSegment("*", Fnv.hashCode64("*"));
         } else {
            long hashCode = this.jsonReader.readFieldNameHashCodeUnquote();
            String name = this.jsonReader.getFieldName();
            segment = new JSONPathSegment.CycleNameSegment(name, hashCode);
         }
      } else {
         boolean isNum = this.jsonReader.isNumber();
         long hashCode = this.jsonReader.readFieldNameHashCodeUnquote();
         String name = this.jsonReader.getFieldName();
         if (isNum) {
            int length = name.length();
            if (length <= 9) {
               for (int i = 0; i < length; i++) {
                  char ch = name.charAt(i);
                  if (ch < '0' || ch > '9') {
                     break;
                  }
               }
            }
         }

         if (this.jsonReader.ch == '(') {
            this.jsonReader.next();
            switch (name) {
               case "length":
               case "size":
                  segment = JSONPathSegment.LengthSegment.INSTANCE;
                  break;
               case "keys":
                  segment = JSONPathSegment.KeysSegment.INSTANCE;
                  break;
               case "values":
                  segment = JSONPathSegment.ValuesSegment.INSTANCE;
                  break;
               case "entrySet":
                  segment = JSONPathSegment.EntrySetSegment.INSTANCE;
                  break;
               case "min":
                  segment = JSONPathSegment.MinSegment.INSTANCE;
                  break;
               case "max":
                  segment = JSONPathSegment.MaxSegment.INSTANCE;
                  break;
               case "sum":
                  segment = JSONPathSegment.SumSegment.INSTANCE;
                  break;
               case "type":
                  segment = JSONPathFunction.FUNC_TYPE;
                  break;
               case "floor":
                  segment = JSONPathFunction.FUNC_FLOOR;
                  break;
               case "ceil":
               case "ceiling":
                  segment = JSONPathFunction.FUNC_CEIL;
                  break;
               case "double":
                  segment = JSONPathFunction.FUNC_DOUBLE;
                  break;
               case "abs":
                  segment = JSONPathFunction.FUNC_ABS;
                  break;
               case "lower":
                  segment = JSONPathFunction.FUNC_LOWER;
                  break;
               case "upper":
                  segment = JSONPathFunction.FUNC_UPPER;
                  break;
               case "trim":
                  segment = JSONPathFunction.FUNC_TRIM;
                  break;
               case "negative":
                  segment = JSONPathFunction.FUNC_NEGATIVE;
                  break;
               case "first":
                  segment = JSONPathFunction.FUNC_FIRST;
                  break;
               case "last":
                  segment = JSONPathFunction.FUNC_LAST;
                  break;
               case "index":
                  if (this.jsonReader.isNumber()) {
                     Number number = this.jsonReader.readNumber();
                     if (number instanceof BigDecimal) {
                        BigDecimal decimal = (BigDecimal)number;
                        decimal = decimal.stripTrailingZeros();
                        if (decimal.scale() != 0) {
                           segment = new JSONPathFunction(new JSONPathFunction.IndexDecimal(decimal));
                           break;
                        }

                        BigInteger unscaledValue = decimal.unscaledValue();
                        if (unscaledValue.compareTo(TypeUtils.BIGINT_INT64_MIN) >= 0 && unscaledValue.compareTo(TypeUtils.BIGINT_INT64_MAX) <= 0) {
                           number = unscaledValue.longValue();
                        } else {
                           number = unscaledValue;
                        }
                     }

                     if (!(number instanceof Integer) && !(number instanceof Long)) {
                        throw new JSONException("not support syntax, path : " + this.path);
                     }

                     long longValue = number.longValue();
                     segment = new JSONPathFunction(new JSONPathFunction.IndexInt(longValue));
                  } else {
                     if (!this.jsonReader.isString()) {
                        throw new JSONException("not support syntax, path : " + this.path);
                     }

                     String indexValue = this.jsonReader.readString();
                     segment = new JSONPathFunction(new JSONPathFunction.IndexString(indexValue));
                  }
                  break;
               default:
                  throw new JSONException("not support syntax, path : " + this.path);
            }

            if (!this.jsonReader.nextIfMatch(')')) {
               throw new JSONException("not support syntax, path : " + this.path);
            }
         } else {
            segment = new JSONPathSegmentName(name, hashCode);
         }
      }

      return segment;
   }

   JSONPathSegment parseFilterRest(JSONPathSegment segment) {
      boolean and;
      switch (this.jsonReader.ch) {
         case '&':
            this.jsonReader.next();
            if (!this.jsonReader.nextIfMatch('&')) {
               throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
            }

            and = true;
            break;
         case 'A':
         case 'a':
            String fieldName = this.jsonReader.readFieldNameUnquote();
            if (!"and".equalsIgnoreCase(fieldName)) {
               throw new JSONException("syntax error : " + fieldName);
            }

            and = true;
            break;
         case 'O':
         case 'o':
            String fieldName = this.jsonReader.readFieldNameUnquote();
            if (!"or".equalsIgnoreCase(fieldName)) {
               throw new JSONException("syntax error : " + fieldName);
            }

            and = false;
            break;
         case '|':
            this.jsonReader.next();
            if (!this.jsonReader.nextIfMatch('|')) {
               throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
            }

            and = false;
            break;
         default:
            throw new JSONException("TODO : " + this.jsonReader.ch);
      }

      JSONPathSegment right = this.parseFilter();
      if (segment instanceof JSONPathFilter.GroupFilter) {
         JSONPathFilter.GroupFilter group = (JSONPathFilter.GroupFilter)segment;
         group.filters.add(((JSONPathFilter)right).setAnd(and));
         return group;
      } else {
         List<JSONPathFilter> filters = new ArrayList<>();
         filters.add((JSONPathFilter)segment);
         if (right instanceof JSONPathFilter.GroupFilter) {
            JSONPathFilter.GroupFilter group = (JSONPathFilter.GroupFilter)right;
            List<JSONPathFilter> groupFilters = group.filters;
            if (groupFilters != null && groupFilters.size() > 0) {
               for (int i = 0; i < groupFilters.size(); i++) {
                  JSONPathFilter filter = groupFilters.get(i);
                  if (i == 0) {
                     filter.setAnd(and);
                  }

                  filters.add(filter);
               }
            }
         } else {
            filters.add(((JSONPathFilter)right).setAnd(and));
         }

         return new JSONPathFilter.GroupFilter(filters);
      }
   }

   JSONPathSegment parseFilter() {
      boolean parentheses = this.jsonReader.nextIfMatch('(');
      if (parentheses && this.filterNests > 0) {
         this.filterNests++;
      }

      boolean at = this.jsonReader.ch == '@';
      if (at) {
         this.jsonReader.next();
      } else if (this.jsonReader.nextIfMatchIdent('e', 'x', 'i', 's', 't', 's')) {
         if (!this.jsonReader.nextIfMatch('(')) {
            throw new JSONException(this.jsonReader.info("exists"));
         }

         if (this.jsonReader.nextIfMatch('@') && this.jsonReader.nextIfMatch('.')) {
            long hashCode = this.jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = this.jsonReader.getFieldName();
            if (this.jsonReader.nextIfMatch(')')) {
               if (parentheses && !this.jsonReader.nextIfMatch(')')) {
                  throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
               }

               return new JSONPathFilter.NameExistsFilter(fieldName, hashCode);
            }

            if (this.jsonReader.ch == '.') {
               List<String> names = new ArrayList<>();
               names.add(fieldName);

               do {
                  this.jsonReader.next();
                  fieldName = this.jsonReader.readFieldNameUnquote();
                  names.add(fieldName);
               } while (this.jsonReader.ch == '.');

               if (this.jsonReader.nextIfMatch(')') && parentheses && !this.jsonReader.nextIfMatch(')')) {
                  throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
               }

               return new JSONPathFilter.NamesExistsFilter(names);
            }
         }

         throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
      }

      boolean starts = this.jsonReader.nextIfMatchIdent('s', 't', 'a', 'r', 't', 's');
      boolean ends = !starts && this.jsonReader.nextIfMatchIdent('e', 'n', 'd', 's');
      if ((!at || !starts && !ends)
         && (this.jsonReader.ch == '.' || this.jsonReader.ch == '[' || isOperator(this.jsonReader.ch) || JSONReader.isFirstIdentifier(this.jsonReader.ch))) {
         String fieldNamex = null;
         long hashCodex = 0L;
         if (at) {
            if (this.jsonReader.ch == '[') {
               JSONPathSegment segment = this.parseArrayAccess();
               if (!(segment instanceof JSONPathSegmentName)) {
                  JSONPathFilter.Operator operator = JSONPath.parseOperator(this.jsonReader);
                  if (this.jsonReader.ch == '@') {
                     JSONPathSegment segment2 = this.parseSegment();
                     if (parentheses) {
                        this.jsonReader.nextIfMatch(')');
                     }

                     return new JSONPathFilter.Segment2Filter(segment, operator, segment2);
                  }

                  Object value = this.jsonReader.readAny();
                  if (parentheses) {
                     this.jsonReader.nextIfMatch(')');
                  }

                  if (segment instanceof JSONPathSegment.RangeIndexSegment) {
                     return new JSONPathFilter.RangeIndexSegmentFilter((JSONPathSegment.RangeIndexSegment)segment, operator, value);
                  }

                  return new JSONPathFilter.SegmentFilter(segment, operator, value);
               }

               fieldNamex = ((JSONPathSegmentName)segment).name;
               hashCodex = ((JSONPathSegmentName)segment).nameHashCode;
            } else {
               if (isOperator(this.jsonReader.ch)) {
                  JSONPathSegment.SelfSegment self = JSONPathSegment.SelfSegment.INSTANCE;
                  JSONPathFilter.Operator operatorx = JSONPath.parseOperator(this.jsonReader);
                  if (this.jsonReader.ch == '@') {
                     JSONPathSegment segment2 = this.parseSegment();
                     if (parentheses) {
                        this.jsonReader.nextIfMatch(')');
                     }

                     return new JSONPathFilter.Segment2Filter(self, operatorx, segment2);
                  }

                  Object valuex = this.jsonReader.readAny();
                  JSONPathSegment segment = new JSONPathFilter.SegmentFilter(self, operatorx, valuex);
                  if (parentheses) {
                     while (this.jsonReader.ch == '&' || this.jsonReader.ch == '|' || this.jsonReader.ch == 'a' || this.jsonReader.ch == 'o') {
                        this.filterNests--;
                        segment = this.parseFilterRest(segment);
                     }

                     this.jsonReader.nextIfMatch(')');
                  }

                  return segment;
               }

               this.jsonReader.next();
            }
         }

         if (fieldNamex == null) {
            hashCodex = this.jsonReader.readFieldNameHashCodeUnquote();
            fieldNamex = this.jsonReader.getFieldName();
         }

         if (parentheses && this.jsonReader.nextIfMatch(')')) {
            if (this.filterNests > 0) {
               this.filterNests--;
            }

            return new JSONPathFilter.NameExistsFilter(fieldNamex, hashCodex);
         } else {
            String functionName = null;
            long[] hashCode2 = null;
            String[] fieldName2 = null;

            while (this.jsonReader.ch == '.') {
               this.jsonReader.next();
               long hash = this.jsonReader.readFieldNameHashCodeUnquote();
               String str = this.jsonReader.getFieldName();
               if (this.jsonReader.ch == '(') {
                  functionName = str;
                  break;
               }

               if (hashCode2 == null) {
                  hashCode2 = new long[]{hash};
                  fieldName2 = new String[]{str};
               } else {
                  hashCode2 = Arrays.copyOf(hashCode2, hashCode2.length + 1);
                  hashCode2[hashCode2.length - 1] = hash;
                  fieldName2 = Arrays.copyOf(fieldName2, fieldName2.length + 1);
                  fieldName2[fieldName2.length - 1] = str;
               }
            }

            if (fieldName2 != null || parentheses || this.jsonReader.ch != ']' && this.jsonReader.ch != '|' && this.jsonReader.ch != '&') {
               JSONPathFilter.Operator operatorxx = null;
               Function function = null;
               if (this.jsonReader.ch == '(') {
                  if (functionName == null) {
                     functionName = fieldNamex;
                     fieldNamex = null;
                  }

                  switch (functionName) {
                     case "type":
                        hashCodex = 0L;
                        function = JSONPathFunction.TypeFunction.INSTANCE;
                        break;
                     case "size":
                        hashCodex = 0L;
                        function = JSONPathFunction.SizeFunction.INSTANCE;
                        break;
                     case "contains":
                        hashCodex = 0L;
                        operatorxx = JSONPathFilter.Operator.CONTAINS;
                        break;
                     default:
                        throw new JSONException("syntax error, function not support " + fieldNamex);
                  }

                  if (function != null) {
                     this.jsonReader.next();
                     if (!this.jsonReader.nextIfMatch(')')) {
                        throw new JSONException("syntax error, function " + functionName);
                     }
                  }
               }

               if (function == null && this.jsonReader.ch == '[') {
                  this.jsonReader.next();
                  if (this.jsonReader.ch == '?') {
                     this.jsonReader.next();
                     JSONPathFilter subFilter = (JSONPathFilter)this.parseFilter();
                     function = new JSONPathFunction.FilterFunction(subFilter);
                  } else {
                     int index = this.jsonReader.readInt32Value();
                     function = new JSONPathFunction.IndexValue(index);
                  }

                  if (!this.jsonReader.nextIfMatch(']')) {
                     throw new JSONException("syntax error");
                  }
               }

               if (operatorxx == null) {
                  if (parentheses && this.jsonReader.nextIfMatch(')')) {
                     return new JSONPathFilter.NameExistsFilter(fieldNamex, hashCodex);
                  }

                  operatorxx = JSONPath.parseOperator(this.jsonReader);
               }

               switch (operatorxx) {
                  case REG_MATCH:
                  case RLIKE:
                  case NOT_RLIKE:
                     String regex;
                     boolean ignoreCase;
                     if (this.jsonReader.isString()) {
                        regex = this.jsonReader.readString();
                        ignoreCase = false;
                     } else {
                        regex = this.jsonReader.readPattern();
                        ignoreCase = this.jsonReader.nextIfMatch('i');
                     }

                     Pattern pattern = ignoreCase ? Pattern.compile(regex, 2) : Pattern.compile(regex);
                     JSONPathSegment segmentxx = new JSONPathFilter.NameRLikeSegment(
                        fieldNamex, hashCodex, pattern, operatorxx == JSONPathFilter.Operator.NOT_RLIKE
                     );
                     if (this.jsonReader.ch == '&' || this.jsonReader.ch == '|' || this.jsonReader.ch == 'a' || this.jsonReader.ch == 'o') {
                        this.filterNests--;
                        segmentxx = this.parseFilterRest(segmentxx);
                     }

                     if (!this.jsonReader.nextIfMatch(')')) {
                        throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                     }

                     return segmentxx;
                  case IN:
                  case NOT_IN:
                     if (this.jsonReader.ch != '(') {
                        throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                     } else {
                        this.jsonReader.next();
                        JSONPathSegment segmentx;
                        if (this.jsonReader.isString()) {
                           List<String> list = new ArrayList<>();

                           while (this.jsonReader.isString()) {
                              list.add(this.jsonReader.readString());
                           }

                           String[] strArray = new String[list.size()];
                           list.toArray(strArray);
                           segmentx = new JSONPathFilter.NameStringInSegment(fieldNamex, hashCodex, strArray, operatorxx == JSONPathFilter.Operator.NOT_IN);
                        } else {
                           if (!this.jsonReader.isNumber()) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           List<Number> list = new ArrayList<>();

                           while (this.jsonReader.isNumber()) {
                              list.add(this.jsonReader.readNumber());
                           }

                           long[] values = new long[list.size()];

                           for (int i = 0; i < list.size(); i++) {
                              values[i] = list.get(i).longValue();
                           }

                           segmentx = new JSONPathFilter.NameIntInSegment(
                              fieldNamex, hashCodex, fieldName2, hashCode2, function, values, operatorxx == JSONPathFilter.Operator.NOT_IN
                           );
                        }

                        if (!this.jsonReader.nextIfMatch(')')) {
                           throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                        } else {
                           if (this.jsonReader.ch == '&' || this.jsonReader.ch == '|' || this.jsonReader.ch == 'a' || this.jsonReader.ch == 'o') {
                              this.filterNests--;
                              segmentx = this.parseFilterRest(segmentx);
                           }

                           if (!this.jsonReader.nextIfMatch(')')) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           return segmentx;
                        }
                     }
                  case CONTAINS:
                     if (this.jsonReader.ch != '(') {
                        throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                     } else {
                        this.jsonReader.next();
                        JSONPathSegment segment;
                        if (this.jsonReader.isString()) {
                           List<String> list = new ArrayList<>();

                           while (this.jsonReader.isString()) {
                              list.add(this.jsonReader.readString());
                           }

                           String[] strArray = new String[list.size()];
                           list.toArray(strArray);
                           segment = new JSONPathFilter.NameStringContainsSegment(fieldNamex, hashCodex, fieldName2, hashCode2, strArray, false);
                        } else {
                           if (!this.jsonReader.isNumber()) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           List<Number> list = new ArrayList<>();

                           while (this.jsonReader.isNumber()) {
                              list.add(this.jsonReader.readNumber());
                           }

                           long[] values = new long[list.size()];

                           for (int i = 0; i < list.size(); i++) {
                              values[i] = list.get(i).longValue();
                           }

                           segment = new JSONPathFilter.NameLongContainsSegment(fieldNamex, hashCodex, fieldName2, hashCode2, values, false);
                        }

                        if (!this.jsonReader.nextIfMatch(')')) {
                           throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                        } else {
                           if (!this.jsonReader.nextIfMatch(')')) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           return segment;
                        }
                     }
                  case BETWEEN:
                  case NOT_BETWEEN:
                     if (this.jsonReader.isNumber()) {
                        Number begin = this.jsonReader.readNumber();
                        String and = this.jsonReader.readFieldNameUnquote();
                        if (!"and".equalsIgnoreCase(and)) {
                           throw new JSONException("syntax error, " + and);
                        }

                        Number end = this.jsonReader.readNumber();
                        JSONPathSegment segment = new JSONPathFilter.NameIntBetweenSegment(
                           fieldNamex, hashCodex, begin.longValue(), end.longValue(), operatorxx == JSONPathFilter.Operator.NOT_BETWEEN
                        );
                        if (parentheses && !this.jsonReader.nextIfMatch(')')) {
                           throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                        }

                        return segment;
                     }

                     throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                  default:
                     JSONPathSegment segmentxxx = null;
                     switch (this.jsonReader.ch) {
                        case '"':
                        case '\'':
                           String strVal = this.jsonReader.readString();
                           int p0 = strVal.indexOf(37);
                           if (p0 == -1) {
                              if (operatorxx == JSONPathFilter.Operator.LIKE) {
                                 operatorxx = JSONPathFilter.Operator.EQ;
                              } else if (operatorxx == JSONPathFilter.Operator.NOT_LIKE) {
                                 operatorxx = JSONPathFilter.Operator.NE;
                              }
                           }

                           if (operatorxx != JSONPathFilter.Operator.LIKE && operatorxx != JSONPathFilter.Operator.NOT_LIKE) {
                              segmentxxx = new JSONPathFilter.NameStringOpSegment(fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, strVal);
                           } else {
                              String[] items = strVal.split("%");
                              String startsWithValue = null;
                              String endsWithValue = null;
                              String[] containsValues = null;
                              if (p0 == 0) {
                                 if (strVal.charAt(strVal.length() - 1) == '%') {
                                    containsValues = new String[items.length - 1];
                                    System.arraycopy(items, 1, containsValues, 0, containsValues.length);
                                 } else {
                                    endsWithValue = items[items.length - 1];
                                    if (items.length > 2) {
                                       containsValues = new String[items.length - 2];
                                       System.arraycopy(items, 1, containsValues, 0, containsValues.length);
                                    }
                                 }
                              } else if (strVal.charAt(strVal.length() - 1) == '%') {
                                 if (items.length == 1) {
                                    startsWithValue = items[0];
                                 } else {
                                    containsValues = items;
                                 }
                              } else if (items.length == 1) {
                                 startsWithValue = items[0];
                              } else if (items.length == 2) {
                                 startsWithValue = items[0];
                                 endsWithValue = items[1];
                              } else {
                                 startsWithValue = items[0];
                                 endsWithValue = items[items.length - 1];
                                 containsValues = new String[items.length - 2];
                                 System.arraycopy(items, 1, containsValues, 0, containsValues.length);
                              }

                              segmentxxx = new JSONPathFilter.NameMatchFilter(
                                 fieldNamex, hashCodex, startsWithValue, endsWithValue, containsValues, operatorxx == JSONPathFilter.Operator.NOT_LIKE
                              );
                           }
                           break;
                        case '#':
                        case '$':
                        case '%':
                        case '&':
                        case '(':
                        case ')':
                        case '*':
                        case ',':
                        case '.':
                        case '/':
                        case ':':
                        case ';':
                        case '<':
                        case '=':
                        case '>':
                        case '?':
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                        case 'G':
                        case 'H':
                        case 'I':
                        case 'J':
                        case 'K':
                        case 'L':
                        case 'M':
                        case 'N':
                        case 'O':
                        case 'P':
                        case 'Q':
                        case 'R':
                        case 'S':
                        case 'T':
                        case 'U':
                        case 'V':
                        case 'W':
                        case 'X':
                        case 'Y':
                        case 'Z':
                        case '\\':
                        case ']':
                        case '^':
                        case '_':
                        case '`':
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'g':
                        case 'h':
                        case 'i':
                        case 'j':
                        case 'k':
                        case 'l':
                        case 'm':
                        case 'o':
                        case 'p':
                        case 'q':
                        case 'r':
                        case 's':
                        case 'u':
                        case 'v':
                        case 'w':
                        case 'x':
                        case 'y':
                        case 'z':
                        default:
                           throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                        case '+':
                        case '-':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                           Number number = this.jsonReader.readNumber();
                           if (number instanceof Integer || number instanceof Long) {
                              segmentxxx = new JSONPathFilter.NameIntOpSegment(
                                 fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, number.longValue()
                              );
                           } else if (number instanceof BigDecimal) {
                              segmentxxx = new JSONPathFilter.NameDecimalOpSegment(fieldNamex, hashCodex, operatorxx, (BigDecimal)number);
                           } else {
                              if (!(number instanceof Float) && !(number instanceof Double)) {
                                 throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                              }

                              segmentxxx = new JSONPathFilter.NameDoubleOpSegment(fieldNamex, hashCodex, operatorxx, number.doubleValue());
                           }
                           break;
                        case '@':
                           this.jsonReader.next();
                           if (!this.jsonReader.nextIfMatch('.')) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           String fieldName1 = this.jsonReader.readFieldNameUnquote();
                           long fieldName1Hash = Fnv.hashCode64(fieldName1);
                           segmentxxx = new JSONPathFilter.NameName(fieldNamex, hashCodex, fieldName1, fieldName1Hash);
                           break;
                        case '[':
                           JSONArray array = this.jsonReader.read(JSONArray.class);
                           segmentxxx = new JSONPathFilter.NameArrayOpSegment(fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, array);
                           break;
                        case 'f':
                           String ident = this.jsonReader.readFieldNameUnquote();
                           if ("false".equalsIgnoreCase(ident)) {
                              segmentxxx = new JSONPathFilter.NameIntOpSegment(fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, 0L);
                           }
                           break;
                        case 'n':
                           boolean nextNull = this.jsonReader.nextIfNull();
                           if (!nextNull) {
                              throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                           }

                           segmentxxx = new JSONPathFilter.NameIsNull(fieldNamex, hashCodex, fieldName2, hashCode2, function);
                           break;
                        case 't':
                           String identx = this.jsonReader.readFieldNameUnquote();
                           if ("true".equalsIgnoreCase(identx)) {
                              segmentxxx = new JSONPathFilter.NameIntOpSegment(fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, 1L);
                           }
                           break;
                        case '{':
                           JSONObject object = this.jsonReader.read(JSONObject.class);
                           segmentxxx = new JSONPathFilter.NameObjectOpSegment(fieldNamex, hashCodex, fieldName2, hashCode2, function, operatorxx, object);
                     }

                     if (this.jsonReader.ch == '&' || this.jsonReader.ch == '|' || this.jsonReader.ch == 'a' || this.jsonReader.ch == 'o') {
                        this.filterNests--;
                        segmentxxx = this.parseFilterRest(segmentxxx);
                     }

                     if (parentheses && !this.jsonReader.nextIfMatch(')')) {
                        throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
                     } else {
                        return segmentxxx;
                     }
               }
            } else {
               return new JSONPathFilter.NameExistsFilter(fieldNamex, hashCodex);
            }
         }
      } else if (this.jsonReader.nextIfMatch('(')) {
         this.filterNests++;
         this.filterNests++;
         return this.parseFilter();
      } else if (!at) {
         throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
      } else {
         JSONPathFilter.Operator operatorxxx;
         if (!starts && !ends) {
            operatorxxx = JSONPath.parseOperator(this.jsonReader);
         } else {
            this.jsonReader.readFieldNameHashCodeUnquote();
            String fieldNamexx = this.jsonReader.getFieldName();
            if (!"with".equalsIgnoreCase(fieldNamexx)) {
               throw new JSONException("not support operator : " + fieldNamexx);
            }

            operatorxxx = starts ? JSONPathFilter.Operator.STARTS_WITH : JSONPathFilter.Operator.ENDS_WITH;
         }

         JSONPathSegment segmentxxx = null;
         if (this.jsonReader.isNumber()) {
            Number number = this.jsonReader.readNumber();
            if (number instanceof Integer || number instanceof Long) {
               segmentxxx = new JSONPathFilter.NameIntOpSegment(null, 0L, null, null, null, operatorxxx, number.longValue());
            }
         } else if (this.jsonReader.isString()) {
            String string = this.jsonReader.readString();
            switch (operatorxxx) {
               case STARTS_WITH:
                  segmentxxx = new JSONPathFilter.StartsWithSegment(null, 0L, string);
                  break;
               case ENDS_WITH:
                  segmentxxx = new JSONPathFilter.EndsWithSegment(null, 0L, string);
                  break;
               default:
                  throw new JSONException("syntax error, " + string);
            }
         }

         while (this.jsonReader.ch == '&' || this.jsonReader.ch == '|') {
            this.filterNests--;
            segmentxxx = this.parseFilterRest(segmentxxx);
         }

         if (segmentxxx == null) {
            throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
         } else if (parentheses && !this.jsonReader.nextIfMatch(')')) {
            throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
         } else {
            return segmentxxx;
         }
      }
   }

   public JSONPathSegment parseSegment() {
      boolean at = this.jsonReader.nextIfMatch('@');
      if (at) {
         Object field = null;
         if (this.jsonReader.nextIfMatch('.')) {
            if (this.jsonReader.isNumber()) {
               field = this.jsonReader.readNumber();
            } else {
               field = this.jsonReader.readFieldNameUnquote();
            }
         } else if (this.jsonReader.nextIfArrayStart()) {
            if (this.jsonReader.isNumber()) {
               field = this.jsonReader.readNumber();
            } else {
               if (!this.jsonReader.isString()) {
                  throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
               }

               field = this.jsonReader.readString();
            }

            if (!this.jsonReader.nextIfArrayEnd()) {
               throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
            }
         }

         if (field instanceof String) {
            String name = (String)field;
            return new JSONPathSegmentName(name, Fnv.hashCode64(name));
         }

         if (field instanceof Integer) {
            return new JSONPathSegmentIndex((Integer)field);
         }
      }

      throw new JSONException(this.jsonReader.info("jsonpath syntax error"));
   }

   static boolean isOperator(char ch) {
      return ch == '=' || ch == '<' || ch == '>' || ch == '!';
   }
}
