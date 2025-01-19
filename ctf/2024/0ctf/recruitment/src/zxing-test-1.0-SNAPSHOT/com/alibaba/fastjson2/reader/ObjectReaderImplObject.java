package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public final class ObjectReaderImplObject extends ObjectReaderPrimitive {
   public static final ObjectReaderImplObject INSTANCE = new ObjectReaderImplObject();

   public ObjectReaderImplObject() {
      super(Object.class);
   }

   @Override
   public Object createInstance(long features) {
      return new JSONObject();
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      return collection;
   }

   @Override
   public Object createInstance(Map map, long features) {
      ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
      Object typeKey = map.get(this.getTypeKey());
      if (typeKey instanceof String) {
         String typeName = (String)typeKey;
         long typeHash = Fnv.hashCode64(typeName);
         ObjectReader reader = null;
         if ((features & JSONReader.Feature.SupportAutoType.mask) != 0L) {
            reader = this.autoType(provider, typeHash);
         }

         if (reader == null) {
            reader = provider.getObjectReader(typeName, this.getObjectClass(), features | this.getFeatures());
            if (reader == null) {
               throw new JSONException("No suitable ObjectReader found for" + typeName);
            }
         }

         if (reader != this) {
            return reader.createInstance(map, features);
         }
      }

      return map;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return jsonReader.readAny();
      } else {
         JSONReader.Context context = jsonReader.getContext();
         long contextFeatures = features | context.getFeatures();
         String typeName = null;
         if (!jsonReader.isObject()) {
            char ch = jsonReader.current();
            if (ch == '/') {
               jsonReader.skipComment();
               ch = jsonReader.current();
            }

            Object value;
            switch (ch) {
               case '"':
               case '\'':
                  value = jsonReader.readString();
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
               default:
                  throw new JSONException(jsonReader.info());
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
                  value = jsonReader.readNumber();
                  break;
               case 'S':
                  if (!jsonReader.nextIfSet()) {
                     throw new JSONException(jsonReader.info());
                  }

                  HashSet<Object> set = new HashSet<>();
                  jsonReader.read(set);
                  value = set;
                  break;
               case '[':
                  value = jsonReader.readArray();
                  break;
               case 'f':
               case 't':
                  value = jsonReader.readBoolValue();
                  break;
               case 'n':
                  value = jsonReader.readNullOrNewDate();
            }

            return value;
         } else {
            jsonReader.nextIfObjectStart();
            long hash = 0L;
            if (jsonReader.isString()) {
               hash = jsonReader.readFieldNameHashCode();
               if (hash == HASH_TYPE) {
                  boolean supportAutoType = context.isEnabled(JSONReader.Feature.SupportAutoType);
                  ObjectReader autoTypeObjectReader;
                  if (supportAutoType) {
                     long typeHash = jsonReader.readTypeHashCode();
                     autoTypeObjectReader = context.getObjectReaderAutoType(typeHash);
                     if (autoTypeObjectReader != null) {
                        Class objectClass = autoTypeObjectReader.getObjectClass();
                        if (objectClass != null) {
                           ClassLoader objectClassLoader = objectClass.getClassLoader();
                           ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                           if (objectClassLoader != classLoader) {
                              Class contextClass = null;
                              typeName = jsonReader.getString();

                              try {
                                 if (classLoader == null) {
                                    classLoader = this.getClass().getClassLoader();
                                 }

                                 contextClass = classLoader.loadClass(typeName);
                              } catch (ClassNotFoundException var21) {
                              }

                              if (!objectClass.equals(contextClass)) {
                                 autoTypeObjectReader = context.getObjectReader(contextClass);
                              }
                           }
                        }
                     }

                     if (autoTypeObjectReader == null) {
                        typeName = jsonReader.getString();
                        autoTypeObjectReader = context.getObjectReaderAutoType(typeName, null);
                     }
                  } else {
                     typeName = jsonReader.readString();
                     autoTypeObjectReader = context.getObjectReaderAutoType(typeName, null);
                     if (autoTypeObjectReader == null && jsonReader.getContext().isEnabled(JSONReader.Feature.ErrorOnNotSupportAutoType)) {
                        throw new JSONException(jsonReader.info("autoType not support : " + typeName));
                     }
                  }

                  if (autoTypeObjectReader != null) {
                     jsonReader.setTypeRedirect(true);
                     return autoTypeObjectReader.readObject(jsonReader, fieldType, fieldName, features);
                  }
               }
            }

            Supplier<Map> objectSupplier = jsonReader.getContext().getObjectSupplier();
            Map object;
            if (objectSupplier != null) {
               object = objectSupplier.get();
            } else if (((features | context.getFeatures()) & JSONReader.Feature.UseNativeObject.mask) != 0L) {
               object = new HashMap();
            } else {
               object = (Map)ObjectReaderImplMap.INSTANCE_OBJECT.createInstance(jsonReader.features(features));
            }

            if (typeName != null) {
               switch (typeName) {
                  default:
                     object.put("@type", typeName);
                  case "java.util.ImmutableCollections$Map1":
                  case "java.util.ImmutableCollections$MapN":
                     hash = 0L;
               }
            }

            for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
               Object name;
               if (i == 0 && typeName == null && hash != 0L) {
                  name = jsonReader.getFieldName();
               } else if (jsonReader.isNumber()) {
                  name = jsonReader.readNumber();
                  jsonReader.nextIfMatch(':');
               } else {
                  name = jsonReader.readFieldName();
               }

               if (name == null) {
                  char current = jsonReader.current();
                  if (current != '{' && current != '[') {
                     name = jsonReader.readFieldNameUnquote();
                     if (jsonReader.current() == ':') {
                        jsonReader.next();
                     }
                  } else {
                     name = jsonReader.readAny();
                     if (!jsonReader.nextIfMatch(':')) {
                        throw new JSONException(jsonReader.info("illegal input"));
                     }
                  }
               }

               Object value;
               switch (jsonReader.current()) {
                  case '"':
                  case '\'':
                     value = jsonReader.readString();
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
                     throw new JSONException(jsonReader.info());
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
                     value = jsonReader.readNumber();
                     break;
                  case 'S':
                     if (!jsonReader.nextIfSet()) {
                        throw new JSONException(jsonReader.info());
                     }

                     value = jsonReader.read(HashSet.class);
                     break;
                  case '[':
                     value = jsonReader.readArray();
                     break;
                  case 'f':
                  case 't':
                     value = jsonReader.readBoolValue();
                     break;
                  case 'n':
                     value = jsonReader.readNullOrNewDate();
                     break;
                  case '{':
                     value = jsonReader.readObject();
               }

               if (value != null || (contextFeatures & JSONReader.Feature.IgnoreNullPropertyValue.mask) == 0L) {
                  Object origin = object.put(name, value);
                  if (origin != null && (contextFeatures & JSONReader.Feature.DuplicateKeyValueAsArray.mask) != 0L) {
                     if (origin instanceof Collection) {
                        ((Collection)origin).add(value);
                        object.put(name, origin);
                     } else {
                        JSONArray array = JSONArray.of(origin, value);
                        object.put(name, array);
                     }
                  }
               }
            }

            jsonReader.nextIfComma();
            return object;
         }
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      byte type = jsonReader.getType();
      if (type >= 73 && type <= 125) {
         return jsonReader.readString();
      } else {
         if (type == -110) {
            ObjectReader autoTypeObjectReader = jsonReader.checkAutoType(Object.class, 0L, features);
            if (autoTypeObjectReader != null) {
               return autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
            }
         }

         if (type == -81) {
            jsonReader.next();
            return null;
         } else {
            return jsonReader.readAny();
         }
      }
   }
}
