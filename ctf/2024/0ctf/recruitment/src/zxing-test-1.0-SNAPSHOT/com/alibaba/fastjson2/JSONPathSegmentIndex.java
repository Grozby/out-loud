package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.function.BiFunction;

final class JSONPathSegmentIndex extends JSONPathSegment {
   static final JSONPathSegmentIndex ZERO = new JSONPathSegmentIndex(0);
   static final JSONPathSegmentIndex ONE = new JSONPathSegmentIndex(1);
   static final JSONPathSegmentIndex TWO = new JSONPathSegmentIndex(2);
   static final JSONPathSegmentIndex LAST = new JSONPathSegmentIndex(-1);
   final int index;

   public JSONPathSegmentIndex(int index) {
      this.index = index;
   }

   static JSONPathSegmentIndex of(int index) {
      if (index == 0) {
         return ZERO;
      } else if (index == 1) {
         return ONE;
      } else if (index == 2) {
         return TWO;
      } else {
         return index == -1 ? LAST : new JSONPathSegmentIndex(index);
      }
   }

   @Override
   public void eval(JSONPath.Context context) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object == null) {
         context.eval = true;
      } else if (object instanceof List) {
         List list = (List)object;
         if (this.index >= 0) {
            if (this.index < list.size()) {
               context.value = list.get(this.index);
            }
         } else {
            int itemIndex = list.size() + this.index;
            if (itemIndex >= 0 && itemIndex < list.size()) {
               context.value = list.get(itemIndex);
            }
         }

         context.eval = true;
      } else if (object instanceof SortedSet
         || object instanceof LinkedHashSet
         || object instanceof Queue
         || this.index == 0 && object instanceof Collection && ((Collection)object).size() == 1) {
         Collection collection = (Collection)object;
         int i = 0;

         for (Object item : collection) {
            if (i == this.index) {
               context.value = item;
               break;
            }

            i++;
         }

         context.eval = true;
      } else if (object instanceof Object[]) {
         Object[] array = (Object[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               context.value = array[this.index];
            }
         } else {
            int itemIndex = array.length + this.index;
            if (itemIndex >= 0 && itemIndex < array.length) {
               context.value = array[itemIndex];
            }
         }

         context.eval = true;
      } else {
         Class objectClass = object.getClass();
         if (objectClass.isArray()) {
            int length = Array.getLength(object);
            if (this.index >= 0) {
               if (this.index < length) {
                  context.value = Array.get(object, this.index);
               }
            } else {
               int itemIndex = length + this.index;
               if (itemIndex >= 0 && itemIndex < length) {
                  context.value = Array.get(object, itemIndex);
               }
            }

            context.eval = true;
         } else if (!(object instanceof JSONPath.Sequence)) {
            if (Map.class.isAssignableFrom(objectClass)) {
               context.value = this.eval((Map)object);
               context.eval = true;
            } else if (this.index == 0) {
               context.value = object;
               context.eval = true;
            } else {
               throw new JSONException("jsonpath not support operate : " + context.path + ", objectClass" + objectClass.getName());
            }
         } else {
            List sequence = ((JSONPath.Sequence)object).values;
            JSONArray values = new JSONArray(sequence.size());

            for (Object o : sequence) {
               context.value = o;
               JSONPath.Context itemContext = new JSONPath.Context(context.path, context, context.current, context.next, context.readerFeatures);
               this.eval(itemContext);
               values.add(itemContext.value);
            }

            if (context.next != null) {
               context.value = new JSONPath.Sequence(values);
            } else {
               context.value = values;
            }

            context.eval = true;
         }
      }
   }

   private Object eval(Map object) {
      Object value = object.get(this.index);
      if (value == null) {
         value = object.get(Integer.toString(this.index));
      }

      if (value == null) {
         int size = object.size();
         Iterator it = object.entrySet().iterator();
         if (size != 1 && !(object instanceof LinkedHashMap) && !(object instanceof SortedMap)) {
            for (int i = 0; i <= this.index && i < object.size() && it.hasNext(); i++) {
               Entry entry = (Entry)it.next();
               Object entryKey = entry.getKey();
               Object entryValue = entry.getValue();
               if (entryKey instanceof Long && entryKey.equals((long)this.index)) {
                  value = entryValue;
                  break;
               }
            }
         } else {
            for (int ix = 0; ix <= this.index && ix < size && it.hasNext(); ix++) {
               Entry entry = (Entry)it.next();
               Object entryKey = entry.getKey();
               Object entryValue = entry.getValue();
               if (entryKey instanceof Long) {
                  if (entryKey.equals((long)this.index)) {
                     value = entryValue;
                     break;
                  }
               } else if (ix == this.index) {
                  value = entryValue;
               }
            }
         }
      }

      return value;
   }

   @Override
   public void set(JSONPath.Context context, Object value) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (!(object instanceof List)) {
         if (object instanceof Object[]) {
            Object[] array = (Object[])object;
            int length = array.length;
            if (this.index >= 0) {
               if (this.index < length) {
                  array[this.index] = value;
               }
            } else {
               int arrayIndex = length + this.index;
               if (arrayIndex >= 0 && arrayIndex < length) {
                  array[arrayIndex] = value;
               }
            }
         } else if (object != null && object.getClass().isArray()) {
            int length = Array.getLength(object);
            if (this.index >= 0) {
               if (this.index < length) {
                  Array.set(object, this.index, value);
               }
            } else {
               int arrayIndex = length + this.index;
               if (arrayIndex >= 0 && arrayIndex < length) {
                  Array.set(object, arrayIndex, value);
               }
            }
         } else {
            throw new JSONException("UnsupportedOperation");
         }
      } else {
         List list = (List)object;
         if (this.index >= 0) {
            if (this.index > list.size()) {
               for (int i = list.size(); i < this.index; i++) {
                  list.add(null);
               }
            }

            if (this.index < list.size()) {
               list.set(this.index, value);
            } else if (this.index <= list.size()) {
               list.add(value);
            }
         } else {
            int itemIndex = list.size() + this.index;
            if (itemIndex >= 0) {
               list.set(itemIndex, value);
            }
         }
      }
   }

   @Override
   public void setCallback(JSONPath.Context context, BiFunction callback) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof List) {
         List list = (List)object;
         if (this.index >= 0) {
            if (this.index < list.size()) {
               Object value = list.get(this.index);
               value = callback.apply(object, value);
               list.set(this.index, value);
            }
         } else {
            int itemIndex = list.size() + this.index;
            if (itemIndex >= 0) {
               Object value = list.get(itemIndex);
               value = callback.apply(object, value);
               list.set(itemIndex, value);
            }
         }
      } else if (object instanceof Object[]) {
         Object[] array = (Object[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               Object value = array[this.index];
               value = callback.apply(object, value);
               array[this.index] = value;
            }
         } else {
            int itemIndex = array.length + this.index;
            if (itemIndex >= 0) {
               Object value = array[itemIndex];
               value = callback.apply(object, value);
               array[itemIndex] = value;
            }
         }
      } else if (object != null && object.getClass().isArray()) {
         int length = Array.getLength(object);
         if (this.index >= 0) {
            if (this.index < length) {
               Object value = Array.get(object, this.index);
               value = callback.apply(object, value);
               Array.set(object, this.index, value);
            }
         } else {
            int arrayIndex = length + this.index;
            if (arrayIndex >= 0) {
               Object value = Array.get(object, arrayIndex);
               value = callback.apply(object, value);
               Array.set(object, arrayIndex, value);
            }
         }
      } else {
         throw new JSONException("UnsupportedOperation");
      }
   }

   @Override
   public boolean remove(JSONPath.Context context) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof List) {
         List list = (List)object;
         if (this.index >= 0) {
            if (this.index < list.size()) {
               list.remove(this.index);
               return true;
            }
         } else {
            int itemIndex = list.size() + this.index;
            if (itemIndex >= 0) {
               list.remove(itemIndex);
               return true;
            }
         }

         return false;
      } else {
         throw new JSONException("UnsupportedOperation");
      }
   }

   @Override
   public void setInt(JSONPath.Context context, int value) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof int[]) {
         int[] array = (int[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               array[this.index] = value;
            }
         } else {
            int arrayIndex = array.length + this.index;
            if (arrayIndex >= 0) {
               array[arrayIndex] = value;
            }
         }
      } else if (object instanceof long[]) {
         long[] array = (long[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               array[this.index] = (long)value;
            }
         } else {
            int arrayIndex = array.length + this.index;
            if (arrayIndex >= 0) {
               array[arrayIndex] = (long)value;
            }
         }
      } else {
         this.set(context, value);
      }
   }

   @Override
   public void setLong(JSONPath.Context context, long value) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof int[]) {
         int[] array = (int[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               array[this.index] = (int)value;
            }
         } else {
            int arrayIndex = array.length + this.index;
            if (arrayIndex >= 0) {
               array[arrayIndex] = (int)value;
            }
         }
      } else if (object instanceof long[]) {
         long[] array = (long[])object;
         if (this.index >= 0) {
            if (this.index < array.length) {
               array[this.index] = value;
            }
         } else {
            int arrayIndex = array.length + this.index;
            if (arrayIndex >= 0) {
               array[arrayIndex] = value;
            }
         }
      } else {
         this.set(context, value);
      }
   }

   @Override
   public void accept(JSONReader jsonReader, JSONPath.Context context) {
      if (context.parent == null || !context.parent.eval && (!(context.parent.current instanceof JSONPathSegment.CycleNameSegment) || context.next != null)) {
         if (jsonReader.jsonb) {
            int itemCnt = jsonReader.startArray();

            for (int i = 0; i < itemCnt; i++) {
               boolean match = this.index == i;
               if (match) {
                  if (!jsonReader.isArray() && !jsonReader.isObject() || context.next == null) {
                     context.value = jsonReader.readAny();
                     context.eval = true;
                  }
                  break;
               }

               jsonReader.skipValue();
            }
         } else if (jsonReader.ch == '{') {
            Map object = jsonReader.readObject();
            context.value = this.eval(object);
            context.eval = true;
         } else {
            jsonReader.next();
            int i = 0;

            while (true) {
               label127: {
                  if (jsonReader.ch != 26) {
                     if (jsonReader.ch == ']') {
                        jsonReader.next();
                        context.eval = true;
                     } else {
                        boolean match = this.index == -1 || this.index == i;
                        if (!match) {
                           jsonReader.skipValue();
                           if (jsonReader.ch == ',') {
                              jsonReader.next();
                           }
                           break label127;
                        }

                        Object val;
                        switch (jsonReader.ch) {
                           case '"':
                           case '\'':
                              val = jsonReader.readString();
                              break;
                           case '#':
                           case '$':
                           case '%':
                           case '&':
                           case '(':
                           case ')':
                           case '*':
                           case ',':
                           case '/':
                           case ':':
                           case ';':
                           case '<':
                           case '=':
                           case '>':
                           case '?':
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
                              throw new JSONException(jsonReader.info("not support : " + jsonReader.ch));
                           case '+':
                           case '-':
                           case '.':
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
                              jsonReader.readNumber0();
                              val = jsonReader.getNumber();
                              break;
                           case '[':
                              if (context.next != null && !(context.next instanceof JSONPathSegment.EvalSegment)) {
                                 return;
                              }

                              val = jsonReader.readArray();
                              break;
                           case 'f':
                           case 't':
                              val = jsonReader.readBoolValue();
                              break;
                           case 'n':
                              jsonReader.readNull();
                              val = null;
                              break;
                           case '{':
                              if (context.next != null && !(context.next instanceof JSONPathSegment.EvalSegment)) {
                                 return;
                              }

                              val = jsonReader.readObject();
                        }

                        if (this.index == -1) {
                           if (jsonReader.ch == ']') {
                              context.value = val;
                           }
                        } else {
                           context.value = val;
                        }
                        break label127;
                     }
                  }

                  return;
               }

               i++;
            }
         }
      } else {
         this.eval(context);
      }
   }

   @Override
   public String toString() {
      int size = this.index < 0 ? IOUtils.stringSize(-this.index) + 1 : IOUtils.stringSize(this.index);
      byte[] bytes = new byte[size + 2];
      bytes[0] = 91;
      IOUtils.getChars(this.index, bytes.length - 1, bytes);
      bytes[bytes.length - 1] = 93;
      String str;
      if (JDKUtils.STRING_CREATOR_JDK11 != null) {
         str = JDKUtils.STRING_CREATOR_JDK11.apply(bytes, JDKUtils.LATIN1);
      } else {
         str = new String(bytes, StandardCharsets.ISO_8859_1);
      }

      return str;
   }
}
