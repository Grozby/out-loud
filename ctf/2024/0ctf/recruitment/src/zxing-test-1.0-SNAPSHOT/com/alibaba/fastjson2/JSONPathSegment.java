package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

abstract class JSONPathSegment {
   public abstract void accept(JSONReader var1, JSONPath.Context var2);

   public abstract void eval(JSONPath.Context var1);

   public boolean contains(JSONPath.Context context) {
      this.eval(context);
      return context.value != null;
   }

   public boolean remove(JSONPath.Context context) {
      throw new JSONException("UnsupportedOperation " + this.getClass());
   }

   public void set(JSONPath.Context context, Object value) {
      throw new JSONException("UnsupportedOperation " + this.getClass());
   }

   public void setCallback(JSONPath.Context context, BiFunction callback) {
      throw new JSONException("UnsupportedOperation " + this.getClass());
   }

   public void setInt(JSONPath.Context context, int value) {
      this.set(context, value);
   }

   public void setLong(JSONPath.Context context, long value) {
      this.set(context, value);
   }

   static final class AllSegment extends JSONPathSegment {
      static final JSONPathSegment.AllSegment INSTANCE = new JSONPathSegment.AllSegment(false);
      static final JSONPathSegment.AllSegment INSTANCE_ARRAY = new JSONPathSegment.AllSegment(true);
      final boolean array;

      AllSegment(boolean array) {
         this.array = array;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object == null) {
            context.value = null;
            context.eval = true;
         } else if (object instanceof Map) {
            Map map = (Map)object;
            JSONArray array = new JSONArray(map.size());

            for (Object value : map.values()) {
               if (this.array && value instanceof Collection) {
                  array.addAll((Collection<? extends Object>)value);
               } else {
                  array.add(value);
               }
            }

            if (context.next != null) {
               context.value = new JSONPath.Sequence(array);
            } else {
               context.value = array;
            }

            context.eval = true;
         } else if (object instanceof List) {
            List list = (List)object;
            JSONArray values = new JSONArray(list.size());
            if (context.next == null && !this.array) {
               for (int i = 0; i < list.size(); i++) {
                  Object item = list.get(i);
                  if (item instanceof Map) {
                     values.addAll(((Map)item).values());
                  } else {
                     values.add(item);
                  }
               }

               context.value = values;
               context.eval = true;
            } else {
               if (context.next != null) {
                  context.value = new JSONPath.Sequence(list);
               } else {
                  context.value = object;
               }

               context.eval = true;
            }
         } else if (object instanceof Collection) {
            context.value = object;
            context.eval = true;
         } else if (object instanceof JSONPath.Sequence) {
            List list = ((JSONPath.Sequence)object).values;
            JSONArray values = new JSONArray(list.size());
            if (context.next != null) {
               context.value = new JSONPath.Sequence(list);
               context.eval = true;
            } else {
               for (int ix = 0; ix < list.size(); ix++) {
                  Object item = list.get(ix);
                  if (item instanceof Map && !this.array) {
                     values.addAll(((Map)item).values());
                  } else if (item instanceof Collection) {
                     Collection collection = (Collection)item;
                     values.addAll(collection);
                  } else {
                     values.add(item);
                  }
               }

               context.value = values;
               context.eval = true;
            }
         } else {
            ObjectWriterProvider provider = context.path.getWriterContext().provider;
            ObjectWriter objectWriter = provider.getObjectWriter(object.getClass());
            List<FieldWriter> fieldWriters = objectWriter.getFieldWriters();
            int size = fieldWriters.size();
            JSONArray array = new JSONArray(size);

            for (int ixx = 0; ixx < size; ixx++) {
               Object fieldValue = fieldWriters.get(ixx).getFieldValue(object);
               array.add(fieldValue);
            }

            context.value = array;
            context.eval = true;
         }
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof Map) {
            ((Map)object).clear();
            return true;
         } else if (object instanceof Collection) {
            ((Collection)object).clear();
            return true;
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof Map) {
            Map map = (Map)object;

            for (Object o : map.entrySet()) {
               Entry entry = (Entry)o;
               entry.setValue(value);
            }
         } else if (object instanceof List) {
            Collections.fill((List<? super Object>)object, value);
         } else if (object != null && object.getClass().isArray()) {
            int len = Array.getLength(object);

            for (int i = 0; i < len; i++) {
               Array.set(object, i, value);
            }
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof Map) {
            Map map = (Map)object;

            for (Object o : map.entrySet()) {
               Entry entry = (Entry)o;
               Object value = entry.getValue();
               Object apply = callback.apply(object, value);
               if (apply != value) {
                  entry.setValue(apply);
               }
            }
         } else if (object instanceof List) {
            List list = (List)object;

            for (int i = 0; i < list.size(); i++) {
               Object value = list.get(i);
               Object apply = callback.apply(object, value);
               if (apply != value) {
                  list.set(i, apply);
               }
            }
         } else if (object != null && object.getClass().isArray()) {
            int len = Array.getLength(object);

            for (int ix = 0; ix < len; ix++) {
               Object value = Array.get(object, ix);
               Object apply = callback.apply(object, value);
               if (apply != value) {
                  Array.set(object, ix, apply);
               }
            }
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent != null && context.parent.eval) {
            this.eval(context);
         } else if (jsonReader.isEnd()) {
            context.eval = true;
         } else if (jsonReader.jsonb) {
            List<Object> values = new JSONArray();
            if (!jsonReader.nextIfMatch((byte)-90)) {
               if (!jsonReader.isArray() || context.next == null) {
                  throw new JSONException("TODO");
               }
            } else {
               while (!jsonReader.nextIfMatch((byte)-91)) {
                  if (jsonReader.skipName()) {
                     Object val = jsonReader.readAny();
                     if (this.array && val instanceof Collection) {
                        values.addAll((Collection<? extends Object>)val);
                     } else {
                        values.add(val);
                     }
                  }
               }

               context.value = values;
            }
         } else {
            boolean alwaysReturnList = context.next == null && (context.path.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L;
            List<Object> values = new JSONArray();
            if (!jsonReader.nextIfObjectStart()) {
               if (jsonReader.ch == '[') {
                  jsonReader.next();

                  while (jsonReader.ch != ']') {
                     Object value = jsonReader.readAny();
                     values.add(value);
                     if (jsonReader.ch == ',') {
                        jsonReader.next();
                     }
                  }

                  jsonReader.next();
                  if (context.next != null) {
                     context.value = new JSONPath.Sequence(values);
                  } else {
                     context.value = values;
                  }

                  context.eval = true;
               } else {
                  throw new JSONException("TODO");
               }
            } else {
               label99:
               while (true) {
                  if (jsonReader.ch == '}') {
                     jsonReader.next();
                     break;
                  }

                  jsonReader.skipName();
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
                     case '.':
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
                        throw new JSONException("TODO : " + jsonReader.ch);
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
                        jsonReader.readNumber0();
                        val = jsonReader.getNumber();
                        break;
                     case '[':
                        val = jsonReader.readArray();
                        break;
                     case ']':
                        jsonReader.next();
                        break label99;
                     case 'f':
                     case 't':
                        val = jsonReader.readBoolValue();
                        break;
                     case 'n':
                        jsonReader.readNull();
                        val = null;
                        break;
                     case '{':
                        val = jsonReader.readObject();
                  }

                  if (val instanceof Collection) {
                     if (alwaysReturnList) {
                        values.add(val);
                     } else {
                        values.addAll((Collection<? extends Object>)val);
                     }
                  } else {
                     values.add(val);
                  }

                  if (jsonReader.ch == ',') {
                     jsonReader.next();
                  }
               }

               context.value = values;
               context.eval = true;
            }
         }
      }
   }

   static final class CycleNameSegment extends JSONPathSegment {
      static final long HASH_STAR = Fnv.hashCode64("*");
      static final long HASH_EMPTY = Fnv.hashCode64("");
      final String name;
      final long nameHashCode;

      public CycleNameSegment(String name, long nameHashCode) {
         this.name = name;
         this.nameHashCode = nameHashCode;
      }

      @Override
      public String toString() {
         return ".." + this.name;
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         JSONPathSegment.CycleNameSegment.LoopRemove action = new JSONPathSegment.CycleNameSegment.LoopRemove(context);
         action.accept(object);
         return context.eval = true;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         List values = new JSONArray();
         Consumer action;
         if (this.shouldRecursive()) {
            action = new JSONPathSegment.CycleNameSegment.MapRecursive(context, values, 0);
         } else {
            action = new JSONPathSegment.CycleNameSegment.MapLoop(context, values);
         }

         action.accept(object);
         if (values.size() == 1 && values.get(0) instanceof Collection) {
            context.value = values.get(0);
         } else {
            context.value = values;
         }

         if (context.value instanceof List && context.next instanceof JSONPathFilter) {
            context.value = new JSONPath.Sequence((List)context.value);
         }

         context.eval = true;
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         JSONPathSegment.CycleNameSegment.LoopSet action = new JSONPathSegment.CycleNameSegment.LoopSet(context, value);
         action.accept(object);
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         JSONPathSegment.CycleNameSegment.LoopCallback action = new JSONPathSegment.CycleNameSegment.LoopCallback(context, callback);
         action.accept(object);
      }

      protected boolean shouldRecursive() {
         return this.nameHashCode == HASH_STAR || this.nameHashCode == HASH_EMPTY;
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         List values = new JSONArray();
         this.accept(jsonReader, context, values);
         context.value = values;
         context.eval = true;
      }

      public void accept(JSONReader jsonReader, JSONPath.Context context, List<Object> values) {
         if (jsonReader.jsonb) {
            if (jsonReader.nextIfMatch((byte)-90)) {
               while (!jsonReader.nextIfMatch((byte)-91)) {
                  long nameHashCode = jsonReader.readFieldNameHashCode();
                  if (nameHashCode != 0L) {
                     boolean match = nameHashCode == this.nameHashCode;
                     if (match) {
                        if (jsonReader.isArray()) {
                           values.addAll(jsonReader.readArray());
                        } else {
                           values.add(jsonReader.readAny());
                        }
                     } else if (!jsonReader.isObject() && !jsonReader.isArray()) {
                        jsonReader.skipValue();
                     } else {
                        this.accept(jsonReader, context, values);
                     }
                  }
               }
            } else {
               if (jsonReader.isArray()) {
                  int itemCnt = jsonReader.startArray();

                  for (int i = 0; i < itemCnt; i++) {
                     if (!jsonReader.isObject() && !jsonReader.isArray()) {
                        jsonReader.skipValue();
                     } else {
                        this.accept(jsonReader, context, values);
                     }
                  }
               } else {
                  jsonReader.skipValue();
               }
            }
         } else {
            if (jsonReader.ch == '{') {
               jsonReader.next();

               while (jsonReader.ch != '}') {
                  long nameHashCode = jsonReader.readFieldNameHashCode();
                  boolean match = nameHashCode == this.nameHashCode;
                  char ch = jsonReader.ch;
                  if (!match && ch != '{' && ch != '[') {
                     jsonReader.skipValue();
                  } else {
                     Object val;
                     switch (jsonReader.ch) {
                        case '"':
                        case '\'':
                           val = jsonReader.readString();
                           break;
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
                           jsonReader.readNumber0();
                           val = jsonReader.getNumber();
                           break;
                        case '[':
                        case '{':
                           if (!match) {
                              this.accept(jsonReader, context, values);
                              continue;
                           }

                           val = ch == '[' ? jsonReader.readArray() : jsonReader.readObject();
                           break;
                        case 'f':
                        case 't':
                           val = jsonReader.readBoolValue();
                           break;
                        case 'n':
                           jsonReader.readNull();
                           val = null;
                           break;
                        default:
                           throw new JSONException("TODO : " + jsonReader.ch);
                     }

                     if (val instanceof Collection) {
                        values.addAll((Collection<? extends Object>)val);
                     } else {
                        values.add(val);
                     }

                     if (jsonReader.ch == ',') {
                        jsonReader.next();
                     }
                  }
               }

               jsonReader.next();
               if (jsonReader.ch == ',') {
                  jsonReader.next();
               }
            } else if (jsonReader.ch == '[') {
               jsonReader.next();

               while (true) {
                  if (jsonReader.ch == ']') {
                     jsonReader.next();
                     break;
                  }

                  if (jsonReader.ch != '{' && jsonReader.ch != '[') {
                     jsonReader.skipValue();
                  } else {
                     this.accept(jsonReader, context, values);
                  }

                  if (jsonReader.ch == ',') {
                     jsonReader.next();
                     break;
                  }
               }

               if (jsonReader.ch == ',') {
                  jsonReader.next();
               }
            } else {
               jsonReader.skipValue();
            }
         }
      }

      class LoopCallback {
         final JSONPath.Context context;
         final BiFunction callback;

         public LoopCallback(JSONPath.Context context, BiFunction callback) {
            this.context = context;
            this.callback = callback;
         }

         public void accept(Object object) {
            if (object instanceof Map) {
               for (Entry entry : ((Map)object).entrySet()) {
                  Object entryValue = entry.getValue();
                  if (CycleNameSegment.this.name.equals(entry.getKey())) {
                     Object applyValue = this.callback.apply(object, entryValue);
                     entry.setValue(applyValue);
                     this.context.eval = true;
                  } else if (entryValue != null) {
                     this.accept(entryValue);
                  }
               }
            } else if (object instanceof Collection) {
               for (Object item : (List)object) {
                  if (item != null) {
                     this.accept(item);
                  }
               }
            } else {
               Class<?> entryValueClass = object.getClass();
               ObjectReader objectReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(entryValueClass);
               ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(entryValueClass);
               if (objectReader instanceof ObjectReaderBean) {
                  FieldReader fieldReader = objectReader.getFieldReader(CycleNameSegment.this.nameHashCode);
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(CycleNameSegment.this.nameHashCode);
                  if (fieldWriter != null && fieldReader != null) {
                     Object fieldValue = fieldWriter.getFieldValue(object);
                     fieldValue = this.callback.apply(object, fieldValue);
                     fieldReader.accept(object, fieldValue);
                     this.context.eval = true;
                     return;
                  }
               }

               for (FieldWriter fieldWriter : objectWriter.getFieldWriters()) {
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  this.accept(fieldValue);
               }
            }
         }
      }

      class LoopRemove {
         final JSONPath.Context context;

         public LoopRemove(JSONPath.Context context) {
            this.context = context;
         }

         public void accept(Object object) {
            if (object instanceof Map) {
               Iterator<Entry> it = ((Map)object).entrySet().iterator();

               while (it.hasNext()) {
                  Entry entry = it.next();
                  if (CycleNameSegment.this.name.equals(entry.getKey())) {
                     it.remove();
                     this.context.eval = true;
                  } else {
                     Object entryValue = entry.getValue();
                     if (entryValue != null) {
                        this.accept(entryValue);
                     }
                  }
               }
            } else if (object instanceof Collection) {
               for (Object item : (List)object) {
                  if (item != null) {
                     this.accept(item);
                  }
               }
            } else {
               Class<?> entryValueClass = object.getClass();
               ObjectReader objectReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(entryValueClass);
               if (objectReader instanceof ObjectReaderBean) {
                  FieldReader fieldReader = objectReader.getFieldReader(CycleNameSegment.this.nameHashCode);
                  if (fieldReader != null) {
                     fieldReader.accept(object, null);
                     this.context.eval = true;
                     return;
                  }
               }

               ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(entryValueClass);

               for (FieldWriter fieldWriter : objectWriter.getFieldWriters()) {
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  this.accept(fieldValue);
               }
            }
         }
      }

      class LoopSet {
         final JSONPath.Context context;
         final Object value;

         public LoopSet(JSONPath.Context context, Object value) {
            this.context = context;
            this.value = value;
         }

         public void accept(Object object) {
            if (object instanceof Map) {
               for (Entry entry : ((Map)object).entrySet()) {
                  if (CycleNameSegment.this.name.equals(entry.getKey())) {
                     entry.setValue(this.value);
                     this.context.eval = true;
                  } else {
                     Object entryValue = entry.getValue();
                     if (entryValue != null) {
                        this.accept(entryValue);
                     }
                  }
               }
            } else if (object instanceof Collection) {
               for (Object item : (List)object) {
                  if (item != null) {
                     this.accept(item);
                  }
               }
            } else {
               Class<?> entryValueClass = object.getClass();
               ObjectReader objectReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(entryValueClass);
               if (objectReader instanceof ObjectReaderBean) {
                  FieldReader fieldReader = objectReader.getFieldReader(CycleNameSegment.this.nameHashCode);
                  if (fieldReader != null) {
                     fieldReader.accept(object, this.value);
                     this.context.eval = true;
                     return;
                  }
               }

               ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(entryValueClass);

               for (FieldWriter fieldWriter : objectWriter.getFieldWriters()) {
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  this.accept(fieldValue);
               }
            }
         }
      }

      class MapLoop implements BiConsumer, Consumer {
         final JSONPath.Context context;
         final List values;

         public MapLoop(JSONPath.Context context, List values) {
            this.context = context;
            this.values = values;
         }

         @Override
         public void accept(Object key, Object value) {
            if (CycleNameSegment.this.name.equals(key)) {
               this.values.add(value);
            }

            if (value instanceof Map) {
               ((Map)value).forEach(this);
            } else if (value instanceof List) {
               ((List)value).forEach(this);
            } else if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_STAR) {
               this.values.add(value);
            }
         }

         @Override
         public void accept(Object value) {
            if (value != null) {
               if (value instanceof Map) {
                  ((Map)value).forEach(this);
               } else if (value instanceof List) {
                  ((List)value).forEach(this);
               } else {
                  ObjectWriter<?> objectWriter = this.context.path.getWriterContext().getObjectWriter(value.getClass());
                  if (objectWriter instanceof ObjectWriterAdapter) {
                     FieldWriter fieldWriter = objectWriter.getFieldWriter(CycleNameSegment.this.nameHashCode);
                     if (fieldWriter != null) {
                        Object fieldValue = fieldWriter.getFieldValue(value);
                        if (fieldValue != null) {
                           this.values.add(fieldValue);
                        }

                        return;
                     }

                     for (int i = 0; i < objectWriter.getFieldWriters().size(); i++) {
                        fieldWriter = objectWriter.getFieldWriters().get(i);
                        Object fieldValue = fieldWriter.getFieldValue(value);
                        this.accept(fieldValue);
                     }
                  } else if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_STAR) {
                     this.values.add(value);
                  }
               }
            }
         }
      }

      class MapRecursive implements Consumer {
         static final int maxLevel = 2048;
         final JSONPath.Context context;
         final List values;
         final int level;

         public MapRecursive(JSONPath.Context context, List values, int level) {
            this.context = context;
            this.values = values;
            this.level = level;
         }

         @Override
         public void accept(Object value) {
            this.recursive(value, this.values, this.level);
         }

         private void recursive(Object value, List values, int level) {
            if (level >= 2048) {
               throw new JSONException("level too large");
            } else {
               if (value instanceof Map) {
                  Collection collection = ((Map)value).values();
                  if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_STAR) {
                     values.addAll(collection);
                  } else if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_EMPTY) {
                     values.add(value);
                  }

                  collection.forEach(this);
               } else if (value instanceof Collection) {
                  Collection collection = (Collection)value;
                  if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_STAR) {
                     values.addAll(collection);
                  } else if (CycleNameSegment.this.nameHashCode == JSONPathSegment.CycleNameSegment.HASH_EMPTY) {
                     values.add(value);
                  }

                  collection.forEach(this);
               } else if (value != null) {
                  ObjectWriter<?> objectWriter = this.context.path.getWriterContext().getObjectWriter(value.getClass());
                  if (objectWriter instanceof ObjectWriterAdapter) {
                     ObjectWriterAdapter writerAdapter = (ObjectWriterAdapter)objectWriter;
                     List<FieldWriter> fieldWriters = writerAdapter.getFieldWriters();
                     Object temp = fieldWriters != null && !fieldWriters.isEmpty()
                        ? fieldWriters.stream().filter(Objects::nonNull).map(v -> v.getFieldValue(value)).collect(Collectors.toList())
                        : new ArrayList();
                     this.recursive(temp, values, level + 1);
                  }
               }
            }
         }
      }
   }

   static final class EntrySetSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.EntrySetSegment INSTANCE = new JSONPathSegment.EntrySetSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (!jsonReader.isObject()) {
            throw new JSONException("TODO");
         } else {
            jsonReader.next();
            JSONArray array = new JSONArray();

            while (!jsonReader.nextIfObjectEnd()) {
               String fieldName = jsonReader.readFieldName();
               Object value = jsonReader.readAny();
               array.add(JSONObject.of("key", fieldName, "value", value));
            }

            context.value = array;
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (!(object instanceof Map)) {
            throw new JSONException("TODO");
         } else {
            Map map = (Map)object;
            JSONArray array = new JSONArray(map.size());

            for (Entry entry : ((Map)object).entrySet()) {
               array.add(JSONObject.of("key", entry.getKey(), "value", entry.getValue()));
            }

            context.value = array;
            context.eval = true;
         }
      }
   }

   interface EvalSegment {
   }

   static final class KeysSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.KeysSegment INSTANCE = new JSONPathSegment.KeysSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (!jsonReader.isObject()) {
            throw new JSONException("TODO");
         } else {
            jsonReader.next();
            JSONArray array = new JSONArray();

            while (!jsonReader.nextIfObjectEnd()) {
               String fieldName = jsonReader.readFieldName();
               array.add(fieldName);
               jsonReader.skipValue();
            }

            context.value = array;
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof Map) {
            context.value = new JSONArray(((Map)object).keySet());
            context.eval = true;
         } else {
            throw new JSONException("TODO");
         }
      }
   }

   static final class LengthSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.LengthSegment INSTANCE = new JSONPathSegment.LengthSegment();

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
         if (value != null) {
            int length = 1;
            if (value instanceof Collection) {
               length = ((Collection)value).size();
            } else if (value.getClass().isArray()) {
               length = Array.getLength(value);
            } else if (value instanceof Map) {
               length = ((Map)value).size();
            } else if (value instanceof String) {
               length = ((String)value).length();
            } else if (value instanceof JSONPath.Sequence) {
               length = ((JSONPath.Sequence)value).values.size();
            }

            context.value = length;
         }
      }
   }

   static final class MaxSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.MaxSegment INSTANCE = new JSONPathSegment.MaxSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object value = context.parent == null ? context.root : context.parent.value;
         if (value != null) {
            Object max = null;
            if (value instanceof Collection) {
               for (Object item : (Collection)value) {
                  if (item != null) {
                     if (max == null) {
                        max = item;
                     } else if (TypeUtils.compare(max, item) < 0) {
                        max = item;
                     }
                  }
               }
            } else if (value instanceof Object[]) {
               Object[] array = (Object[])value;

               for (Object itemx : array) {
                  if (itemx != null) {
                     if (max == null) {
                        max = itemx;
                     } else if (TypeUtils.compare(max, itemx) < 0) {
                        max = itemx;
                     }
                  }
               }
            } else {
               if (!(value instanceof JSONPath.Sequence)) {
                  throw new UnsupportedOperationException();
               }

               for (Object itemxx : ((JSONPath.Sequence)value).values) {
                  if (itemxx != null) {
                     if (max == null) {
                        max = itemxx;
                     } else if (TypeUtils.compare(max, itemxx) < 0) {
                        max = itemxx;
                     }
                  }
               }
            }

            context.value = max;
            context.eval = true;
         }
      }
   }

   static final class MinSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.MinSegment INSTANCE = new JSONPathSegment.MinSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object value = context.parent == null ? context.root : context.parent.value;
         if (value != null) {
            Object min = null;
            if (value instanceof Collection) {
               for (Object item : (Collection)value) {
                  if (item != null) {
                     if (min == null) {
                        min = item;
                     } else if (TypeUtils.compare(min, item) > 0) {
                        min = item;
                     }
                  }
               }
            } else if (value instanceof Object[]) {
               Object[] array = (Object[])value;

               for (Object itemx : array) {
                  if (itemx != null) {
                     if (min == null) {
                        min = itemx;
                     } else if (TypeUtils.compare(min, itemx) > 0) {
                        min = itemx;
                     }
                  }
               }
            } else {
               if (!(value instanceof JSONPath.Sequence)) {
                  throw new UnsupportedOperationException();
               }

               for (Object itemxx : ((JSONPath.Sequence)value).values) {
                  if (itemxx != null) {
                     if (min == null) {
                        min = itemxx;
                     } else if (TypeUtils.compare(min, itemxx) > 0) {
                        min = itemxx;
                     }
                  }
               }
            }

            context.value = min;
            context.eval = true;
         }
      }
   }

   static final class MultiIndexSegment extends JSONPathSegment {
      final int[] indexes;

      public MultiIndexSegment(int[] indexes) {
         this.indexes = indexes;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         List<Object> result = new JSONArray();
         if (object instanceof JSONPath.Sequence) {
            List list = ((JSONPath.Sequence)object).values;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               context.value = list.get(i);
               JSONPath.Context itemContext = new JSONPath.Context(context.path, context, context.current, context.next, context.readerFeatures);
               this.eval(itemContext);
               Object value = itemContext.value;
               if (value instanceof Collection) {
                  result.addAll((Collection<? extends Object>)value);
               } else {
                  result.add(value);
               }
            }

            context.value = result;
         } else {
            for (int index : this.indexes) {
               Object value;
               if (object instanceof List) {
                  List list = (List)object;
                  if (index >= 0) {
                     if (index >= list.size()) {
                        continue;
                     }

                     value = list.get(index);
                  } else {
                     int itemIndex = list.size() + index;
                     if (itemIndex < 0) {
                        continue;
                     }

                     value = list.get(itemIndex);
                  }
               } else {
                  if (!(object instanceof Object[])) {
                     continue;
                  }

                  Object[] array = (Object[])object;
                  if (index >= 0) {
                     if (index >= array.length) {
                        continue;
                     }

                     value = array[index];
                  } else {
                     int itemIndex = array.length + index;
                     if (itemIndex < 0) {
                        continue;
                     }

                     value = array[itemIndex];
                  }
               }

               if (value instanceof Collection) {
                  result.addAll((Collection<? extends Object>)value);
               } else {
                  result.add(value);
               }
            }

            context.value = result;
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent != null && context.parent.current instanceof JSONPathSegment.CycleNameSegment && context.next == null) {
            this.eval(context);
         } else if (jsonReader.jsonb) {
            JSONArray array = new JSONArray();
            int itemCnt = jsonReader.startArray();

            for (int i = 0; i < itemCnt; i++) {
               boolean match = Arrays.binarySearch(this.indexes, i) >= 0;
               if (!match) {
                  jsonReader.skipValue();
               } else {
                  array.add(jsonReader.readAny());
               }
            }

            context.value = array;
         } else {
            JSONArray array = new JSONArray();
            jsonReader.next();

            for (int ix = 0; jsonReader.ch != 26; ix++) {
               if (jsonReader.ch == ']') {
                  jsonReader.next();
                  break;
               }

               boolean match = Arrays.binarySearch(this.indexes, ix) >= 0;
               if (!match) {
                  jsonReader.skipValue();
                  if (jsonReader.ch == ',') {
                     jsonReader.next();
                  }
               } else {
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
                        throw new JSONException("TODO : " + jsonReader.ch);
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
                        val = jsonReader.readObject();
                  }

                  array.add(val);
               }
            }

            context.value = array;
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               for (int index : this.indexes) {
                  if (index == i) {
                     Object item = list.get(i);
                     item = callback.apply(object, item);
                     list.set(i, item);
                  }
               }
            }
         } else {
            if (object != null) {
               Class objectClass = object.getClass();
               if (objectClass.isArray()) {
                  int size = Array.getLength(object);

                  for (int i = 0; i < size; i++) {
                     for (int indexx : this.indexes) {
                        if (indexx == i) {
                           Object item = Array.get(object, i);
                           item = callback.apply(object, item);
                           Array.set(object, i, item);
                        }
                     }
                  }

                  return;
               }
            }

            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               for (int index : this.indexes) {
                  if (index == i) {
                     list.set(i, value);
                  }
               }
            }
         } else {
            if (object != null) {
               Class objectClass = object.getClass();
               if (objectClass.isArray()) {
                  int size = Array.getLength(object);

                  for (int i = 0; i < size; i++) {
                     for (int indexx : this.indexes) {
                        if (indexx == i) {
                           Array.set(object, i, value);
                        }
                     }
                  }

                  return;
               }
            }

            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }
   }

   static final class MultiNameSegment extends JSONPathSegment {
      final String[] names;
      final long[] nameHashCodes;
      final Set<String> nameSet;

      public MultiNameSegment(String[] names) {
         this.names = names;
         this.nameHashCodes = new long[names.length];
         this.nameSet = new HashSet<>();

         for (int i = 0; i < names.length; i++) {
            this.nameHashCodes[i] = Fnv.hashCode64(names[i]);
            this.nameSet.add(names[i]);
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (!(object instanceof Map)) {
            if (object instanceof Collection) {
               context.value = object;
            } else {
               ObjectWriterProvider provider = context.path.getWriterContext().provider;
               ObjectWriter objectWriter = provider.getObjectWriter(object.getClass());
               JSONArray array = new JSONArray(this.names.length);

               for (int i = 0; i < this.names.length; i++) {
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCodes[i]);
                  Object fieldValue = null;
                  if (fieldWriter != null) {
                     fieldValue = fieldWriter.getFieldValue(object);
                  }

                  array.add(fieldValue);
               }

               context.value = array;
            }
         } else {
            Map map = (Map)object;
            JSONArray array = new JSONArray(this.names.length);

            for (String name : this.names) {
               Object value = map.get(name);
               array.add(value);
            }

            context.value = array;
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent == null
            || !context.parent.eval
               && !(context.parent.current instanceof JSONPathFilter)
               && !(context.parent.current instanceof JSONPathSegment.MultiIndexSegment)) {
            Object object = jsonReader.readAny();
            if (object instanceof Map) {
               Map map = (Map)object;
               JSONArray array = new JSONArray(this.names.length);

               for (String name : this.names) {
                  Object value = map.get(name);
                  array.add(value);
               }

               context.value = array;
            } else if (!(object instanceof Collection)) {
               throw new JSONException("UnsupportedOperation " + this.getClass());
            } else if (context.next != null) {
               context.value = object;
            } else {
               Collection collection = (Collection)object;
               JSONArray collectionArray = new JSONArray(collection.size());

               for (Object item : collection) {
                  if (item instanceof Map) {
                     Map map = (Map)item;
                     JSONArray array = new JSONArray(this.names.length);

                     for (String name : this.names) {
                        Object value = map.get(name);
                        array.add(value);
                     }

                     collectionArray.add(array);
                  }
               }

               context.value = collectionArray;
            }
         } else {
            this.eval(context);
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (!(object instanceof Map)) {
            ObjectReaderProvider provider = context.path.getReaderContext().provider;
            ObjectReader objectReader = provider.getObjectReader(object.getClass());
            if (objectReader instanceof ObjectReaderBean) {
               for (long nameHash : this.nameHashCodes) {
                  FieldReader fieldReader = objectReader.getFieldReader(nameHash);
                  if (fieldReader != null) {
                     fieldReader.accept(object, value);
                  }
               }
            } else {
               throw new JSONException("UnsupportedOperation " + this.getClass());
            }
         } else {
            Map map = (Map)object;

            for (String name : this.names) {
               map.put(name, value);
            }
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof Map) {
            Map map = (Map)object;

            for (String name : this.names) {
               Object value = map.get(name);
               Object apply = callback.apply(map, value);
               if (apply != value) {
                  map.put(name, apply);
               }
            }
         } else {
            ObjectWriterProvider writerProvider = context.path.getWriterContext().provider;
            ObjectWriter objectWriter = writerProvider.getObjectWriter(object.getClass());
            if (objectWriter instanceof ObjectWriterAdapter) {
               ObjectReaderProvider readerProvider = context.path.getReaderContext().provider;
               ObjectReader objectReader = readerProvider.getObjectReader(object.getClass());
               if (objectReader instanceof ObjectReaderBean) {
                  for (long nameHash : this.nameHashCodes) {
                     FieldWriter fieldWriter = objectWriter.getFieldWriter(nameHash);
                     if (fieldWriter != null) {
                        FieldReader fieldReader = objectReader.getFieldReader(nameHash);
                        if (fieldReader != null) {
                           Object fieldValue = fieldWriter.getFieldValue(object);
                           Object apply = callback.apply(object, fieldValue);
                           if (apply != fieldValue) {
                              fieldReader.accept(object, apply);
                           }
                        }
                     }
                  }

                  return;
               }
            }

            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         int removeCount = 0;
         if (object instanceof Map) {
            Map map = (Map)object;

            for (String name : this.names) {
               if (map.remove(name) != null) {
                  removeCount++;
               }
            }

            return removeCount > 0;
         } else {
            ObjectReaderProvider provider = context.path.getReaderContext().provider;
            ObjectReader objectReader = provider.getObjectReader(object.getClass());
            if (objectReader instanceof ObjectReaderBean) {
               for (long nameHash : this.nameHashCodes) {
                  FieldReader fieldReader = objectReader.getFieldReader(nameHash);
                  if (fieldReader != null) {
                     fieldReader.accept(object, null);
                     removeCount++;
                  }
               }

               return removeCount > 0;
            } else {
               throw new JSONException("UnsupportedOperation " + this.getClass());
            }
         }
      }
   }

   static final class RandomIndexSegment extends JSONPathSegment {
      public static final JSONPathSegment.RandomIndexSegment INSTANCE = new JSONPathSegment.RandomIndexSegment();
      Random random;

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent == null || !context.parent.eval && (!(context.parent.current instanceof JSONPathSegment.CycleNameSegment) || context.next != null)) {
            if (jsonReader.jsonb) {
               JSONArray array = new JSONArray();
               int itemCnt = jsonReader.startArray();

               for (int i = 0; i < itemCnt; i++) {
                  array.add(jsonReader.readAny());
               }

               if (this.random == null) {
                  this.random = new Random();
               }

               itemCnt = Math.abs(this.random.nextInt()) % array.size();
               context.value = array.get(itemCnt);
               context.eval = true;
            } else {
               JSONArray array = new JSONArray();
               jsonReader.next();

               for (int i = 0; jsonReader.ch != 26; i++) {
                  if (jsonReader.ch == ']') {
                     jsonReader.next();
                     break;
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
                        throw new JSONException("TODO : " + jsonReader.ch);
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
                        val = jsonReader.readObject();
                  }

                  array.add(val);
               }

               if (this.random == null) {
                  this.random = new Random();
               }

               int index = Math.abs(this.random.nextInt()) % array.size();
               context.value = array.get(index);
               context.eval = true;
            }
         } else {
            this.eval(context);
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            if (!list.isEmpty()) {
               if (this.random == null) {
                  this.random = new Random();
               }

               int randomIndex = Math.abs(this.random.nextInt()) % list.size();
               context.value = list.get(randomIndex);
               context.eval = true;
            }
         } else if (object instanceof Object[]) {
            Object[] array = (Object[])object;
            if (array.length != 0) {
               if (this.random == null) {
                  this.random = new Random();
               }

               int randomIndex = this.random.nextInt() % array.length;
               context.value = array[randomIndex];
               context.eval = true;
            }
         } else {
            throw new JSONException("TODO");
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            if (this.random == null) {
               this.random = new Random();
            }

            int randomIndex = Math.abs(this.random.nextInt()) % list.size();
            Object item = list.get(randomIndex);
            Object apply = callback.apply(list, item);
            list.set(randomIndex, apply);
         } else {
            throw new JSONException("UnsupportedOperation ");
         }
      }
   }

   static final class RangeIndexSegment extends JSONPathSegment {
      final int begin;
      final int end;

      public RangeIndexSegment(int begin, int end) {
         this.begin = begin;
         this.end = end;
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         List result = new JSONArray();
         if (object instanceof List) {
            List list = (List)object;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               int index = this.begin >= 0 ? i : i - size;
               if (index >= this.begin && index < this.end) {
                  result.add(list.get(i));
               }
            }

            context.value = result;
            context.eval = true;
         } else if (!(object instanceof Object[])) {
            throw new JSONException("TODO");
         } else {
            Object[] array = (Object[])object;

            for (int i = 0; i < array.length; i++) {
               boolean match = i >= this.begin && i <= this.end || i - array.length > this.begin && i - array.length <= this.end;
               if (match) {
                  result.add(array[i]);
               }
            }

            context.value = result;
            context.eval = true;
         }
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent == null || !context.parent.eval && (!(context.parent.current instanceof JSONPathSegment.CycleNameSegment) || context.next != null)) {
            if (jsonReader.jsonb) {
               JSONArray array = new JSONArray();
               int itemCnt = jsonReader.startArray();

               for (int i = 0; i < itemCnt; i++) {
                  boolean match = this.begin < 0 || i >= this.begin && i < this.end;
                  if (!match) {
                     jsonReader.skipValue();
                  } else {
                     array.add(jsonReader.readAny());
                  }
               }

               if (this.begin < 0) {
                  itemCnt = array.size();

                  for (int ix = itemCnt - 1; ix >= 0; ix--) {
                     int ni = ix - itemCnt;
                     if (ni < this.begin || ni >= this.end) {
                        array.remove(ix);
                     }
                  }
               }

               context.value = array;
               context.eval = true;
            } else {
               JSONArray array = new JSONArray();
               jsonReader.next();

               for (int ixx = 0; jsonReader.ch != 26; ixx++) {
                  if (jsonReader.ch == ']') {
                     jsonReader.next();
                     break;
                  }

                  boolean match = this.begin < 0 || ixx >= this.begin && ixx < this.end;
                  if (!match) {
                     jsonReader.skipValue();
                     if (jsonReader.ch == ',') {
                        jsonReader.next();
                     }
                  } else {
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
                           throw new JSONException("TODO : " + jsonReader.ch);
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
                           val = jsonReader.readObject();
                     }

                     array.add(val);
                  }
               }

               if (this.begin < 0) {
                  int size = array.size();

                  for (int ixx = size - 1; ixx >= 0; ixx--) {
                     int ni = ixx - size;
                     if (ni < this.begin || ni >= this.end) {
                        array.remove(ixx);
                     }
                  }
               }

               context.value = array;
               context.eval = true;
            }
         } else {
            this.eval(context);
         }
      }

      @Override
      public void set(JSONPath.Context context, Object value) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               int index = this.begin >= 0 ? i : i - size;
               if (index >= this.begin && index < this.end) {
                  list.set(i, value);
               }
            }
         } else {
            if (object != null) {
               Class objectClass = object.getClass();
               if (objectClass.isArray()) {
                  int sizex = Array.getLength(object);

                  for (int i = 0; i < sizex; i++) {
                     int index = this.begin >= 0 ? i : i - sizex;
                     if (index >= this.begin && index < this.end) {
                        Array.set(object, i, value);
                     }
                  }

                  return;
               }
            }

            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public void setCallback(JSONPath.Context context, BiFunction callback) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            int i = 0;

            for (int size = list.size(); i < size; i++) {
               int index = this.begin >= 0 ? i : i - size;
               if (index >= this.begin && index < this.end) {
                  Object item = list.get(i);
                  item = callback.apply(list, item);
                  list.set(index, item);
               }
            }
         } else {
            if (object != null) {
               Class objectClass = object.getClass();
               if (objectClass.isArray()) {
                  int sizex = Array.getLength(object);

                  for (int i = 0; i < sizex; i++) {
                     int index = this.begin >= 0 ? i : i - sizex;
                     if (index >= this.begin && index < this.end) {
                        Object item = Array.get(object, i);
                        item = callback.apply(object, item);
                        Array.set(object, i, item);
                     }
                  }

                  return;
               }
            }

            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }

      @Override
      public boolean remove(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object instanceof List) {
            List list = (List)object;
            int removeCount = 0;
            int size = list.size();

            for (int i = size - 1; i >= 0; i--) {
               int index = this.begin >= 0 ? i : i - size;
               if (index >= this.begin && index < this.end) {
                  list.remove(i);
                  removeCount++;
               }
            }

            return removeCount > 0;
         } else {
            throw new JSONException("UnsupportedOperation " + this.getClass());
         }
      }
   }

   static final class RootSegment extends JSONPathSegment {
      static final JSONPathSegment.RootSegment INSTANCE = new JSONPathSegment.RootSegment();

      private RootSegment() {
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         if (context.parent != null) {
            throw new JSONException("not support operation");
         } else {
            context.value = jsonReader.readAny();
            context.eval = true;
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         context.value = context.parent == null ? context.root : context.parent.root;
      }
   }

   static final class SelfSegment extends JSONPathSegment {
      static final JSONPathSegment.SelfSegment INSTANCE = new JSONPathSegment.SelfSegment();

      private SelfSegment() {
      }

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         context.value = jsonReader.readAny();
         context.eval = true;
      }

      @Override
      public void eval(JSONPath.Context context) {
         context.value = context.parent == null ? context.root : context.parent.value;
      }
   }

   static final class SumSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.SumSegment INSTANCE = new JSONPathSegment.SumSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      static Number add(Number a, Number b) {
         boolean aIsInt = a instanceof Byte || a instanceof Short || a instanceof Integer || a instanceof Long;
         boolean bIsInt = b instanceof Byte || b instanceof Short || b instanceof Integer || b instanceof Long;
         if (aIsInt && bIsInt) {
            return a.longValue() + b.longValue();
         } else {
            boolean aIsDouble = a instanceof Float || a instanceof Double;
            boolean bIsDouble = b instanceof Float || b instanceof Double;
            if (aIsDouble || bIsDouble) {
               return a.doubleValue() + b.doubleValue();
            } else if (a instanceof BigDecimal || b instanceof BigDecimal) {
               return TypeUtils.toBigDecimal(a).add(TypeUtils.toBigDecimal(b));
            } else if (!(a instanceof BigInteger) && !(b instanceof BigInteger)) {
               throw new JSONException("not support operation");
            } else {
               return TypeUtils.toBigInteger(a).add(TypeUtils.toBigInteger(b));
            }
         }
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object value = context.parent == null ? context.root : context.parent.value;
         if (value != null) {
            Number sum = 0;
            if (value instanceof Collection) {
               for (Object item : (Collection)value) {
                  if (item != null) {
                     sum = add(sum, (Number)item);
                  }
               }
            } else if (value instanceof Object[]) {
               Object[] array = (Object[])value;

               for (Object itemx : array) {
                  if (itemx != null) {
                     sum = add(sum, (Number)itemx);
                  }
               }
            } else {
               if (!(value instanceof JSONPath.Sequence)) {
                  throw new UnsupportedOperationException();
               }

               for (Object itemxx : ((JSONPath.Sequence)value).values) {
                  if (itemxx != null) {
                     sum = add(sum, (Number)itemxx);
                  }
               }
            }

            context.value = sum;
            context.eval = true;
         }
      }
   }

   static final class ValuesSegment extends JSONPathSegment implements JSONPathSegment.EvalSegment {
      static final JSONPathSegment.ValuesSegment INSTANCE = new JSONPathSegment.ValuesSegment();

      @Override
      public void accept(JSONReader jsonReader, JSONPath.Context context) {
         this.eval(context);
      }

      @Override
      public void eval(JSONPath.Context context) {
         Object object = context.parent == null ? context.root : context.parent.value;
         if (object == null) {
            context.value = null;
            context.eval = true;
         } else if (object instanceof Map) {
            context.value = new JSONArray(((Map)object).values());
            context.eval = true;
         } else {
            throw new JSONException("TODO");
         }
      }
   }
}
