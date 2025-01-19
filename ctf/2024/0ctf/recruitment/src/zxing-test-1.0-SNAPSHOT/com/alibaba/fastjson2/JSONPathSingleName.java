package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.reader.ValueConsumer;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

final class JSONPathSingleName extends JSONPathSingle {
   final long nameHashCode;
   final String name;

   public JSONPathSingleName(String path, JSONPathSegmentName segment, JSONPath.Feature... features) {
      super(segment, path, features);
      this.name = segment.name;
      this.nameHashCode = segment.nameHashCode;
   }

   @Override
   public Object eval(Object root) {
      Object value;
      if (root instanceof Map) {
         Map map = (Map)root;
         value = map.get(this.name);
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
      } else {
         ObjectWriter objectWriter = this.getWriterContext().getObjectWriter(root.getClass());
         if (objectWriter == null) {
            return null;
         }

         FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
         if (fieldWriter == null) {
            return null;
         }

         value = fieldWriter.getFieldValue(root);
      }

      if ((this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
         value = value == null ? new JSONArray() : JSONArray.of(value);
      }

      return value;
   }

   @Override
   public boolean remove(Object root) {
      if (root == null) {
         return false;
      } else if (root instanceof Map) {
         return ((Map)root).remove(this.name) != null;
      } else {
         ObjectReaderProvider provider = this.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(root.getClass());
         if (objectReader == null) {
            return false;
         } else {
            FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
            if (fieldReader == null) {
               return false;
            } else {
               try {
                  fieldReader.accept(root, null);
                  return true;
               } catch (Exception var6) {
                  return false;
               }
            }
         }
      }
   }

   @Override
   public boolean isRef() {
      return true;
   }

   @Override
   public boolean contains(Object root) {
      if (root instanceof Map) {
         return ((Map)root).containsKey(this.name);
      } else {
         ObjectWriterProvider provider = this.getWriterContext().provider;
         ObjectWriter objectWriter = provider.getObjectWriter(root.getClass());
         if (objectWriter == null) {
            return false;
         } else {
            FieldWriter fieldWriter = objectWriter.getFieldWriter(this.nameHashCode);
            return fieldWriter == null ? false : fieldWriter.getFieldValue(root) != null;
         }
      }
   }

   @Override
   public void set(Object rootObject, Object value) {
      if (rootObject instanceof Map) {
         Map map = (Map)rootObject;
         map.put(this.name, value);
      } else {
         ObjectReaderProvider provider = this.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(rootObject.getClass());
         FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
         if (fieldReader != null) {
            if (value != null) {
               Class<?> valueClass = value.getClass();
               Class fieldClass = fieldReader.fieldClass;
               if (!fieldReader.supportAcceptType(valueClass)) {
                  Function typeConvert = provider.getTypeConvert(valueClass, fieldClass);
                  if (typeConvert != null) {
                     value = typeConvert.apply(value);
                  }
               }
            }

            fieldReader.accept(rootObject, value);
         } else if (objectReader instanceof ObjectReaderBean) {
            objectReader.acceptExtra(rootObject, this.name, value, 0L);
         }
      }
   }

   @Override
   public void set(Object rootObject, Object value, JSONReader.Feature... readerFeatures) {
      if (!(rootObject instanceof Map)) {
         ObjectReaderProvider provider = this.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(rootObject.getClass());
         FieldReader fieldReader = objectReader.getFieldReader(this.nameHashCode);
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

         fieldReader.accept(rootObject, value);
      } else {
         Map map = (Map)rootObject;
         Object origin = map.put(this.name, value);
         if (origin != null) {
            boolean duplicateKeyValueAsArray = false;

            for (JSONReader.Feature feature : readerFeatures) {
               if (feature == JSONReader.Feature.DuplicateKeyValueAsArray) {
                  duplicateKeyValueAsArray = true;
                  break;
               }
            }

            if (duplicateKeyValueAsArray) {
               if (origin instanceof Collection) {
                  ((Collection)origin).add(value);
                  map.put(this.name, value);
               } else {
                  JSONArray array = JSONArray.of(origin, value);
                  map.put(this.name, array);
               }
            }
         }
      }
   }

   @Override
   public void setCallback(Object object, BiFunction callback) {
      if (!(object instanceof Map)) {
         Class<?> objectClass = object.getClass();
         if (this.readerContext == null) {
            this.readerContext = JSONFactory.createReadContext();
         }

         FieldReader fieldReader = this.readerContext.provider.getObjectReader(objectClass).getFieldReader(this.nameHashCode);
         if (this.writerContext == null) {
            this.writerContext = JSONFactory.createWriteContext();
         }

         FieldWriter fieldWriter = this.writerContext.provider.getObjectWriter(objectClass).getFieldWriter(this.nameHashCode);
         if (fieldReader != null && fieldWriter != null) {
            Object fieldValue = fieldWriter.getFieldValue(object);
            Object value = callback.apply(object, fieldValue);
            fieldReader.accept(object, value);
         }
      } else {
         Map map = (Map)object;
         Object originValue = map.get(this.name);
         if (originValue != null || map.containsKey(this.name)) {
            map.put(this.name, callback.apply(map, originValue));
         }
      }
   }

   @Override
   public void setInt(Object obejct, int value) {
      if (obejct instanceof Map) {
         ((Map)obejct).put(this.name, value);
      } else {
         ObjectReaderProvider provider = this.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(obejct.getClass());
         objectReader.setFieldValue(obejct, this.name, this.nameHashCode, value);
      }
   }

   @Override
   public void setLong(Object object, long value) {
      if (object instanceof Map) {
         ((Map)object).put(this.name, value);
      } else {
         ObjectReaderProvider provider = this.getReaderContext().getProvider();
         ObjectReader objectReader = provider.getObjectReader(object.getClass());
         objectReader.setFieldValue(object, this.name, this.nameHashCode, value);
      }
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (jsonReader.jsonb) {
         if (jsonReader.nextIfObjectStart()) {
            while (!jsonReader.nextIfObjectEnd()) {
               long nameHashCode = jsonReader.readFieldNameHashCode();
               if (nameHashCode != 0L) {
                  boolean match = nameHashCode == this.nameHashCode;
                  if (match || jsonReader.isObject() || jsonReader.isArray()) {
                     return jsonReader.readAny();
                  }

                  jsonReader.skipValue();
               }
            }
         }

         return (this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L ? new JSONArray() : null;
      } else {
         if (jsonReader.nextIfObjectStart()) {
            while (!jsonReader.nextIfObjectEnd()) {
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
                        val = jsonReader.readNumber();
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
                        break;
                     default:
                        throw new JSONException("TODO : " + jsonReader.ch);
                  }

                  if ((this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L) {
                     if (val == null) {
                        val = new JSONArray();
                     } else {
                        val = JSONArray.of(val);
                     }
                  }

                  return val;
               }

               jsonReader.skipValue();
            }
         }

         return (this.features & JSONPath.Feature.AlwaysReturnList.mask) != 0L ? new JSONArray() : null;
      }
   }

   @Override
   public String extractScalar(JSONReader jsonReader) {
      if (jsonReader.nextIfObjectStart()) {
         while (jsonReader.ch != '}') {
            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            char ch = jsonReader.ch;
            if (match || ch == '{' || ch == '[') {
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
                     val = jsonReader.readNumber();
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
                     break;
                  default:
                     throw new JSONException("TODO : " + jsonReader.ch);
               }

               return JSON.toJSONString(val);
            }

            jsonReader.skipValue();
         }

         jsonReader.next();
      }

      return null;
   }

   @Override
   public long extractInt64Value(JSONReader jsonReader) {
      if (jsonReader.nextIfObjectStart()) {
         label35:
         while (true) {
            if (jsonReader.ch == '}') {
               jsonReader.wasNull = true;
               return 0L;
            }

            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            if (match) {
               switch (jsonReader.ch) {
                  case '"':
                  case '\'':
                     String str = jsonReader.readString();
                     return Long.parseLong(str);
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
                     return jsonReader.readInt64Value();
                  case '[':
                  case '{':
                     Map object = jsonReader.readObject();
                     return jsonReader.toLong(object);
                  case ']':
                     jsonReader.next();
                     break label35;
                  case 'f':
                  case 't':
                     boolean booleanValue = jsonReader.readBoolValue();
                     return booleanValue ? 1L : 0L;
                  case 'n':
                     jsonReader.readNull();
                     jsonReader.wasNull = true;
                     return 0L;
               }
            }

            jsonReader.skipValue();
         }
      }

      jsonReader.wasNull = true;
      return 0L;
   }

   @Override
   public int extractInt32Value(JSONReader jsonReader) {
      if (jsonReader.nextIfObjectStart()) {
         label34:
         while (true) {
            if (jsonReader.ch == '}') {
               jsonReader.wasNull = true;
               return 0;
            }

            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            if (match) {
               switch (jsonReader.ch) {
                  case '"':
                  case '\'':
                     String str = jsonReader.readString();
                     return Integer.parseInt(str);
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
                     return jsonReader.readInt32Value();
                  case ']':
                     jsonReader.next();
                     break label34;
                  case 'f':
                  case 't':
                     boolean booleanValue = jsonReader.readBoolValue();
                     return booleanValue ? 1 : 0;
                  case 'n':
                     jsonReader.readNull();
                     jsonReader.wasNull = true;
                     return 0;
                  default:
                     throw new JSONException("TODO : " + jsonReader.ch);
               }
            }

            jsonReader.skipValue();
         }
      }

      jsonReader.wasNull = true;
      return 0;
   }

   @Override
   public void extractScalar(JSONReader jsonReader, ValueConsumer consumer) {
      if (jsonReader.nextIfObjectStart()) {
         label31:
         while (true) {
            if (jsonReader.ch == '}') {
               consumer.acceptNull();
               return;
            }

            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            if (match) {
               switch (jsonReader.ch) {
                  case '"':
                  case '\'':
                     jsonReader.readString(consumer, false);
                     return;
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
                     jsonReader.readNumber(consumer, false);
                     return;
                  case '[':
                     List array = jsonReader.readArray();
                     consumer.accept(array);
                     return;
                  case ']':
                     jsonReader.next();
                     break label31;
                  case 'f':
                  case 't':
                     consumer.accept(jsonReader.readBoolValue());
                     return;
                  case 'n':
                     jsonReader.readNull();
                     consumer.acceptNull();
                     return;
                  case '{':
                     Map object = jsonReader.readObject();
                     consumer.accept(object);
                     return;
               }
            }

            jsonReader.skipValue();
         }
      }

      consumer.acceptNull();
   }

   @Override
   public void extract(JSONReader jsonReader, ValueConsumer consumer) {
      if (jsonReader.nextIfObjectStart()) {
         while (jsonReader.ch != '}') {
            long nameHashCode = jsonReader.readFieldNameHashCode();
            boolean match = nameHashCode == this.nameHashCode;
            if (match) {
               switch (jsonReader.ch) {
                  case '"':
                  case '\'':
                     jsonReader.readString(consumer, true);
                     return;
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
                     jsonReader.readNumber(consumer, true);
                     return;
                  case '[':
                     List array = jsonReader.readArray();
                     consumer.accept(array);
                     return;
                  case 'f':
                  case 't':
                     consumer.accept(jsonReader.readBoolValue());
                     return;
                  case 'n':
                     jsonReader.readNull();
                     consumer.acceptNull();
                     return;
                  case '{':
                     Map object = jsonReader.readObject();
                     consumer.accept(object);
                     return;
                  default:
                     throw new JSONException("TODO : " + jsonReader.ch);
               }
            }

            jsonReader.skipValue();
         }

         consumer.acceptNull();
      } else {
         consumer.acceptNull();
      }
   }
}
