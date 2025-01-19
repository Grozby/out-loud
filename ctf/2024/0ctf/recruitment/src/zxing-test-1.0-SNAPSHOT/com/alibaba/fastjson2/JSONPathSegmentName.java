package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

class JSONPathSegmentName extends JSONPathSegment {
   static final long HASH_NAME = Fnv.hashCode64("name");
   static final long HASH_ORDINAL = Fnv.hashCode64("ordinal");
   final String name;
   final long nameHashCode;

   public JSONPathSegmentName(String name, long nameHashCode) {
      this.name = name;
      this.nameHashCode = nameHashCode;
   }

   @Override
   public boolean remove(JSONPath.Context context) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof Map) {
         Map map = (Map)object;
         map.remove(this.name);
         return context.eval = true;
      } else if (object instanceof Collection) {
         for (Object item : (Collection)object) {
            if (item != null) {
               if (item instanceof Map) {
                  Map map = (Map)item;
                  map.remove(this.name);
               } else {
                  ObjectReaderProvider provider = context.path.getReaderContext().getProvider();
                  ObjectReader objectReader = provider.getObjectReader(item.getClass());
                  FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
                  if (fieldReader != null) {
                     fieldReader.accept(item, null);
                  }
               }
            }
         }

         return context.eval = true;
      } else {
         ObjectReaderProvider provider = context.path.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(object.getClass());
         FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
         if (fieldReader != null) {
            fieldReader.accept(object, null);
         }

         return context.eval = true;
      }
   }

   @Override
   public boolean contains(JSONPath.Context context) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object == null) {
         return false;
      } else if (object instanceof Map) {
         return ((Map)object).containsKey(this.name);
      } else if (object instanceof Collection) {
         for (Object item : (Collection)object) {
            if (item != null) {
               if (item instanceof Map && ((Map)item).get(this.name) != null) {
                  return true;
               }

               ObjectWriter<?> objectWriter = context.path.getWriterContext().getObjectWriter(item.getClass());
               if (objectWriter instanceof ObjectWriterAdapter) {
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
                  if (fieldWriter != null && fieldWriter.getFieldValue(item) != null) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else if (object instanceof JSONPath.Sequence) {
         JSONPath.Sequence sequence = (JSONPath.Sequence)object;

         for (Object itemx : sequence.values) {
            if (itemx != null) {
               if (itemx instanceof Map && ((Map)itemx).get(this.name) != null) {
                  return true;
               }

               ObjectWriter<?> objectWriter = context.path.getWriterContext().getObjectWriter(itemx.getClass());
               if (objectWriter instanceof ObjectWriterAdapter) {
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
                  if (fieldWriter != null && fieldWriter.getFieldValue(itemx) != null) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         if (object instanceof Object[]) {
            Object[] array = (Object[])object;

            for (Object itemxx : array) {
               if (itemxx != null) {
                  if (itemxx instanceof Map && ((Map)itemxx).get(this.name) != null) {
                     return true;
                  }

                  ObjectWriter<?> objectWriter = context.path.getWriterContext().getObjectWriter(itemxx.getClass());
                  if (objectWriter instanceof ObjectWriterAdapter) {
                     FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
                     if (fieldWriter != null && fieldWriter.getFieldValue(itemxx) != null) {
                        return true;
                     }
                  }
               }
            }
         }

         ObjectWriter<?> objectWriter = context.path.getWriterContext().getObjectWriter(object.getClass());
         if (objectWriter instanceof ObjectWriterAdapter) {
            FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
            if (fieldWriter != null) {
               return fieldWriter.getFieldValue(object) != null;
            }
         }

         return false;
      }
   }

   @Override
   public void eval(JSONPath.Context context) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object != null) {
         if (object instanceof Map) {
            Map map = (Map)object;
            Object value = map.get(this.name);
            if (value == null) {
               boolean isNum = IOUtils.isNumber(this.name);
               Long longValue = null;

               for (Object o : map.entrySet()) {
                  Entry entry = (Entry)o;
                  Object entryKey = entry.getKey();
                  if (entryKey instanceof Enum && ((Enum)entryKey).name().equals(this.name)) {
                     value = entry.getValue();
                     break;
                  }

                  if (entryKey instanceof Long) {
                     if (longValue == null && isNum) {
                        longValue = Long.parseLong(this.name);
                     }

                     if (entryKey.equals(longValue)) {
                        value = entry.getValue();
                        break;
                     }
                  }
               }
            }

            context.value = value;
         } else if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>)object;
            int size = collection.size();
            Collection values = null;

            for (Object item : collection) {
               if (item instanceof Map) {
                  Object val = ((Map)item).get(this.name);
                  if (val != null) {
                     if (val instanceof Collection) {
                        if (size == 1) {
                           values = (Collection)val;
                        } else {
                           if (values == null) {
                              values = new JSONArray(size);
                           }

                           values.addAll((Collection)val);
                        }
                     } else {
                        if (values == null) {
                           values = new JSONArray(size);
                        }

                        values.add(val);
                     }
                  }
               }
            }

            context.value = values;
         } else if (!(object instanceof JSONPath.Sequence)) {
            JSONWriter.Context writerContext = context.path.getWriterContext();
            ObjectWriter<?> objectWriter = writerContext.getObjectWriter(object.getClass());
            if (objectWriter instanceof ObjectWriterAdapter) {
               FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
               if (fieldWriter != null) {
                  context.value = fieldWriter.getFieldValue(object);
               }
            } else if (this.nameHashCode == HASH_NAME && object instanceof Enum) {
               context.value = ((Enum)object).name();
            } else if (this.nameHashCode == HASH_ORDINAL && object instanceof Enum) {
               context.value = ((Enum)object).ordinal();
            } else if (object instanceof String) {
               String str = (String)object;
               if (!str.isEmpty() && str.charAt(0) == '{') {
                  context.value = JSONPath.of("$." + this.name).extract(JSONReader.of(str));
               } else {
                  context.value = null;
               }
            } else if (!(object instanceof Number) && !(object instanceof Boolean)) {
               throw new JSONException("not support : " + object.getClass());
            } else {
               context.value = null;
            }
         } else {
            List sequence = ((JSONPath.Sequence)object).values;
            JSONArray values = new JSONArray(sequence.size());

            for (Object o : sequence) {
               context.value = o;
               JSONPath.Context itemContext = new JSONPath.Context(context.path, context, context.current, context.next, context.readerFeatures);
               this.eval(itemContext);
               Object val = itemContext.value;
               if (val != null || (context.path.features & JSONPath.Feature.KeepNullValue.mask) != 0L) {
                  if (val instanceof Collection) {
                     values.addAll((Collection<? extends Object>)val);
                  } else {
                     values.add(val);
                  }
               }
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

   @Override
   public void set(JSONPath.Context context, Object value) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof Map) {
         Map map = (Map)object;
         Object origin = map.put(this.name, value);
         if (origin != null && (context.readerFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
            if (origin instanceof Collection) {
               ((Collection)origin).add(value);
               map.put(this.name, value);
            } else {
               JSONArray array = JSONArray.of(origin, value);
               map.put(this.name, array);
            }
         }
      } else if (object instanceof Collection) {
         for (Object item : (Collection)object) {
            if (item != null) {
               if (item instanceof Map) {
                  Map map = (Map)item;
                  Object origin = map.put(this.name, value);
                  if (origin != null && (context.readerFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                     if (origin instanceof Collection) {
                        ((Collection)origin).add(value);
                        map.put(this.name, value);
                     } else {
                        JSONArray array = JSONArray.of(origin, value);
                        map.put(this.name, array);
                     }
                  }
               } else {
                  ObjectReaderProvider provider = context.path.getReaderContext().getProvider();
                  ObjectReader objectReader = provider.getObjectReader(item.getClass());
                  FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
                  if (fieldReader != null) {
                     fieldReader.accept(item, null);
                  }
               }
            }
         }
      } else {
         ObjectReaderProvider provider = context.path.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(object.getClass());
         FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
         if (fieldReader != null) {
            if (value != null) {
               Class<?> valueClass = value.getClass();
               Class fieldClass = fieldReader.fieldClass;
               if (valueClass != fieldClass) {
                  Function typeConvert = provider.getTypeConvert(valueClass, fieldClass);
                  if (typeConvert != null) {
                     value = typeConvert.apply(value);
                  }
               }
            }

            fieldReader.accept(object, value);
         }
      }
   }

   @Override
   public void setCallback(JSONPath.Context context, BiFunction callback) {
      Object object = context.parent == null ? context.root : context.parent.value;
      if (object instanceof Map) {
         Map map = (Map)object;
         Object origin = map.get(this.name);
         if (origin != null) {
            Object applyValue = callback.apply(map, origin);
            map.put(this.name, applyValue);
         }
      } else {
         ObjectReaderProvider provider = context.path.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(object.getClass());
         ObjectWriter objectWriter = context.path.getWriterContext().provider.getObjectWriter(object.getClass());
         FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
         FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
         if (fieldReader != null && fieldWriter != null) {
            Object fieldValue = fieldWriter.getFieldValue(object);
            Object applyValue = callback.apply(object, fieldValue);
            fieldReader.accept(object, applyValue);
         }
      }
   }

   @Override
   public void accept(JSONReader jsonReader, JSONPath.Context context) {
      if (context.parent == null
         || !context.parent.eval
            && !(context.parent.current instanceof JSONPathFilter)
            && !(context.parent.current instanceof JSONPathSegment.MultiIndexSegment)) {
         if (!jsonReader.jsonb) {
            if (jsonReader.nextIfObjectStart()) {
               if (jsonReader.ch == '}') {
                  jsonReader.next();
                  if (jsonReader.isEnd()) {
                     return;
                  }

                  jsonReader.nextIfComma();
               }

               while (true) {
                  if (jsonReader.nextIfObjectEnd()) {
                     jsonReader.next();
                     break;
                  }

                  long nameHashCode = jsonReader.readFieldNameHashCode();
                  boolean match = nameHashCode == this.nameHashCode;
                  if (match) {
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
                           if (context.next != null
                              && !(context.next instanceof JSONPathSegment.EvalSegment)
                              && !(context.next instanceof JSONPathSegmentName)
                              && !(context.next instanceof JSONPathSegment.AllSegment)) {
                              return;
                           }

                           val = jsonReader.readArray();
                           context.eval = true;
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
                           if (context.next != null
                              && !(context.next instanceof JSONPathSegment.EvalSegment)
                              && !(context.next instanceof JSONPathSegment.AllSegment)) {
                              return;
                           }

                           val = jsonReader.readObject();
                           context.eval = true;
                           break;
                        default:
                           throw new JSONException("TODO : " + jsonReader.ch);
                     }

                     context.value = val;
                     break;
                  }

                  jsonReader.skipValue();
                  if (jsonReader.ch == ',') {
                     jsonReader.next();
                  }
               }
            } else if (jsonReader.ch == '[' && context.parent != null && context.parent.current instanceof JSONPathSegment.AllSegment) {
               jsonReader.next();
               List values = new JSONArray();

               while (jsonReader.ch != 26) {
                  if (jsonReader.ch == ']') {
                     jsonReader.next();
                     break;
                  }

                  label228:
                  if (jsonReader.ch == '{') {
                     jsonReader.next();

                     while (jsonReader.ch != '}') {
                        long nameHashCodex = jsonReader.readFieldNameHashCode();
                        boolean matchx = nameHashCodex == this.nameHashCode;
                        if (!matchx) {
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
                                 if (context.next != null) {
                                    break label228;
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
                                 if (context.next != null) {
                                    break label228;
                                 }

                                 val = jsonReader.readObject();
                           }

                           values.add(val);
                        }
                     }

                     jsonReader.next();
                  } else {
                     jsonReader.skipValue();
                  }

                  if (jsonReader.ch == ',') {
                     jsonReader.next();
                  }
               }

               context.value = values;
            }
         } else if (jsonReader.nextIfObjectStart()) {
            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               long nameHashCodex = jsonReader.readFieldNameHashCode();
               if (nameHashCodex != 0L) {
                  boolean matchx = nameHashCodex == this.nameHashCode;
                  if (matchx) {
                     if (!jsonReader.isArray() && !jsonReader.isObject() || context.next == null) {
                        context.value = jsonReader.readAny();
                        context.eval = true;
                     }
                     break;
                  }

                  jsonReader.skipValue();
               }
            }
         } else if (jsonReader.isArray() && context.parent != null && context.parent.current instanceof JSONPathSegment.AllSegment) {
            List values = new JSONArray();
            int itemCnt = jsonReader.startArray();

            for (int ix = 0; ix < itemCnt; ix++) {
               if (!jsonReader.nextIfMatch((byte)-90)) {
                  jsonReader.skipValue();
               } else {
                  for (int j = 0; !jsonReader.nextIfMatch((byte)-91); j++) {
                     long nameHashCodex = jsonReader.readFieldNameHashCode();
                     boolean matchx = nameHashCodex == this.nameHashCode;
                     if (!matchx) {
                        jsonReader.skipValue();
                     } else {
                        if ((jsonReader.isArray() || jsonReader.isObject()) && context.next != null) {
                           break;
                        }

                        values.add(jsonReader.readAny());
                     }
                  }
               }
            }

            context.value = values;
            context.eval = true;
         } else {
            throw new JSONException("TODO");
         }
      } else {
         this.eval(context);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         JSONPathSegmentName that = (JSONPathSegmentName)o;
         return this.nameHashCode == that.nameHashCode && Objects.equals(this.name, that.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name, this.nameHashCode);
   }

   @Override
   public String toString() {
      return this.name;
   }
}
