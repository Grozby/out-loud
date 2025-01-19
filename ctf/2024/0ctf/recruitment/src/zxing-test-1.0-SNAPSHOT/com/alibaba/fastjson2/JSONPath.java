package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.ValueConsumer;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class JSONPath {
   static final JSONReader.Context PARSE_CONTEXT = JSONFactory.createReadContext();
   JSONReader.Context readerContext;
   JSONWriter.Context writerContext;
   final String path;
   final long features;

   protected JSONPath(String path, JSONPath.Feature... features) {
      this.path = path;
      long featuresValue = 0L;

      for (JSONPath.Feature feature : features) {
         featuresValue |= feature.mask;
      }

      this.features = featuresValue;
   }

   protected JSONPath(String path, long features) {
      this.path = path;
      this.features = features;
   }

   public abstract JSONPath getParent();

   public boolean endsWithFilter() {
      return false;
   }

   public boolean isPrevious() {
      return false;
   }

   @Override
   public final String toString() {
      return this.path;
   }

   public static Object extract(String json, String path) {
      JSONReader jsonReader = JSONReader.of(json);
      JSONPath jsonPath = of(path);
      return jsonPath.extract(jsonReader);
   }

   public static Object extract(String json, String path, JSONPath.Feature... features) {
      JSONReader jsonReader = JSONReader.of(json);
      JSONPath jsonPath = of(path, features);
      return jsonPath.extract(jsonReader);
   }

   public static Object eval(String str, String path) {
      return extract(str, path);
   }

   public static Object eval(Object rootObject, String path) {
      return of(path).eval(rootObject);
   }

   public static String set(String json, String path, Object value) {
      Object object = JSON.parse(json);
      of(path).set(object, value);
      return JSON.toJSONString(object);
   }

   public static boolean contains(Object rootObject, String path) {
      if (rootObject == null) {
         return false;
      } else {
         JSONPath jsonPath = of(path);
         return jsonPath.contains(rootObject);
      }
   }

   public static Object set(Object rootObject, String path, Object value) {
      of(path).set(rootObject, value);
      return rootObject;
   }

   public static Object setCallback(Object rootObject, String path, Function callback) {
      of(path).setCallback(rootObject, callback);
      return rootObject;
   }

   public static Object setCallback(Object rootObject, String path, BiFunction callback) {
      of(path).setCallback(rootObject, callback);
      return rootObject;
   }

   public static String remove(String json, String path) {
      Object object = JSON.parse(json);
      of(path).remove(object);
      return JSON.toJSONString(object);
   }

   public static void remove(Object rootObject, String path) {
      of(path).remove(rootObject);
   }

   public static Map<String, Object> paths(Object javaObject) {
      Map<Object, String> values = new IdentityHashMap<>();
      Map<String, Object> paths = new LinkedHashMap<>();
      JSONPath.RootPath.INSTANCE.paths(values, paths, "$", javaObject);
      return paths;
   }

   void paths(Map<Object, String> values, Map paths, String parent, Object javaObject) {
      if (javaObject != null) {
         String p = values.put(javaObject, parent);
         if (p != null) {
            Class<?> type = javaObject.getClass();
            boolean basicType = type == String.class
               || type == Boolean.class
               || type == Character.class
               || type == UUID.class
               || javaObject instanceof Enum
               || javaObject instanceof Number
               || javaObject instanceof Date;
            if (!basicType) {
               return;
            }
         }

         paths.put(parent, javaObject);
         if (javaObject instanceof Map) {
            Map map = (Map)javaObject;

            for (Object entryObj : map.entrySet()) {
               Entry entry = (Entry)entryObj;
               Object key = entry.getKey();
               if (key instanceof String) {
                  String strKey = (String)key;
                  boolean escape = strKey.isEmpty();
                  if (!escape) {
                     char c0 = strKey.charAt(0);
                     escape = (c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z') && c0 != '_';
                     if (!escape) {
                        for (int i = 1; i < strKey.length(); i++) {
                           char ch = strKey.charAt(i);
                           escape = (ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9') && ch != '_';
                           if (escape) {
                              break;
                           }
                        }
                     }
                  }

                  String path;
                  if (escape) {
                     path = parent + '[' + JSON.toJSONString(strKey, JSONWriter.Feature.UseSingleQuotes) + ']';
                  } else {
                     path = parent + "." + strKey;
                  }

                  this.paths(values, paths, path, entry.getValue());
               }
            }
         } else if (javaObject instanceof Collection) {
            Collection collection = (Collection)javaObject;
            int ix = 0;

            for (Object item : collection) {
               String path = parent + "[" + ix + "]";
               this.paths(values, paths, path, item);
               ix++;
            }
         } else {
            Class<?> clazz = javaObject.getClass();
            if (clazz.isArray()) {
               int len = Array.getLength(javaObject);

               for (int ix = 0; ix < len; ix++) {
                  Object item = Array.get(javaObject, ix);
                  String path = parent + "[" + ix + "]";
                  this.paths(values, paths, path, item);
               }
            } else if (!ObjectWriterProvider.isPrimitiveOrEnum(clazz)) {
               ObjectWriter serializer = this.getWriterContext().getObjectWriter(clazz);
               if (serializer instanceof ObjectWriterAdapter) {
                  ObjectWriterAdapter javaBeanSerializer = (ObjectWriterAdapter)serializer;

                  try {
                     Map<String, Object> fieldValues = javaBeanSerializer.toMap(javaObject);

                     for (Entry<String, Object> entry : fieldValues.entrySet()) {
                        String key = entry.getKey();
                        if (key != null) {
                           String path = parent + "." + key;
                           this.paths(values, paths, path, entry.getValue());
                        }
                     }
                  } catch (Exception var17) {
                     throw new JSONException("toJSON error", var17);
                  }
               }
            }
         }
      }
   }

   public abstract boolean isRef();

   public void arrayAdd(Object root, Object... values) {
      Object result = this.eval(root);
      if (result == null) {
         this.set(root, JSONArray.of(values));
      } else {
         if (result instanceof Collection) {
            Collection collection = (Collection)result;
            collection.addAll(Arrays.asList(values));
         }
      }
   }

   public abstract boolean contains(Object var1);

   public abstract Object eval(Object var1);

   protected JSONReader.Context createContext() {
      return JSONFactory.createReadContext();
   }

   public Object extract(String jsonStr) {
      if (jsonStr == null) {
         return null;
      } else {
         JSONReader jsonReader = JSONReader.of(jsonStr, this.createContext());

         Object var3;
         try {
            var3 = this.extract(jsonReader);
         } catch (Throwable var6) {
            if (jsonReader != null) {
               try {
                  jsonReader.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (jsonReader != null) {
            jsonReader.close();
         }

         return var3;
      }
   }

   public Object extract(byte[] jsonBytes) {
      if (jsonBytes == null) {
         return null;
      } else {
         JSONReader jsonReader = JSONReader.of(jsonBytes, this.createContext());

         Object var3;
         try {
            var3 = this.extract(jsonReader);
         } catch (Throwable var6) {
            if (jsonReader != null) {
               try {
                  jsonReader.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (jsonReader != null) {
            jsonReader.close();
         }

         return var3;
      }
   }

   public Object extract(byte[] jsonBytes, int off, int len, Charset charset) {
      if (jsonBytes == null) {
         return null;
      } else {
         JSONReader jsonReader = JSONReader.of(jsonBytes, off, len, charset, this.createContext());

         Object var6;
         try {
            var6 = this.extract(jsonReader);
         } catch (Throwable var9) {
            if (jsonReader != null) {
               try {
                  jsonReader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (jsonReader != null) {
            jsonReader.close();
         }

         return var6;
      }
   }

   public abstract Object extract(JSONReader var1);

   public abstract String extractScalar(JSONReader var1);

   public JSONReader.Context getReaderContext() {
      if (this.readerContext == null) {
         this.readerContext = JSONFactory.createReadContext();
      }

      return this.readerContext;
   }

   public JSONPath setReaderContext(JSONReader.Context context) {
      this.readerContext = context;
      return this;
   }

   public JSONWriter.Context getWriterContext() {
      if (this.writerContext == null) {
         this.writerContext = JSONFactory.createWriteContext();
      }

      return this.writerContext;
   }

   public JSONPath setWriterContext(JSONWriter.Context writerContext) {
      this.writerContext = writerContext;
      return this;
   }

   public abstract void set(Object var1, Object var2);

   public abstract void set(Object var1, Object var2, JSONReader.Feature... var3);

   public void setCallback(Object object, Function callback) {
      this.setCallback(object, new JSONPathFunction.BiFunctionAdapter(callback));
   }

   public abstract void setCallback(Object var1, BiFunction var2);

   public abstract void setInt(Object var1, int var2);

   public abstract void setLong(Object var1, long var2);

   public abstract boolean remove(Object var1);

   public void extract(JSONReader jsonReader, ValueConsumer consumer) {
      Object object = this.extract(jsonReader);
      if (object == null) {
         consumer.acceptNull();
      } else if (object instanceof Number) {
         consumer.accept((Number)object);
      } else if (object instanceof String) {
         consumer.accept((String)object);
      } else if (object instanceof Boolean) {
         consumer.accept((Boolean)object);
      } else if (object instanceof Map) {
         consumer.accept((Map)object);
      } else if (object instanceof List) {
         consumer.accept((List)object);
      } else {
         throw new JSONException("TODO : " + object.getClass());
      }
   }

   public void extractScalar(JSONReader jsonReader, ValueConsumer consumer) {
      Object object = this.extractScalar(jsonReader);
      if (object == null) {
         consumer.acceptNull();
      } else {
         String str = object.toString();
         consumer.accept(str);
      }
   }

   public Long extractInt64(JSONReader jsonReader) {
      long value = this.extractInt64Value(jsonReader);
      return jsonReader.wasNull ? null : value;
   }

   public long extractInt64Value(JSONReader jsonReader) {
      Object object = this.extract(jsonReader);
      if (object == null) {
         jsonReader.wasNull = true;
         return 0L;
      } else if (object instanceof Number) {
         return ((Number)object).longValue();
      } else {
         Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(object.getClass(), long.class);
         if (typeConvert == null) {
            throw new JSONException("can not convert to long : " + object);
         } else {
            Object converted = typeConvert.apply(object);
            return (Long)converted;
         }
      }
   }

   public Integer extractInt32(JSONReader jsonReader) {
      int intValue = this.extractInt32Value(jsonReader);
      return jsonReader.wasNull ? null : intValue;
   }

   public int extractInt32Value(JSONReader jsonReader) {
      Object object = this.extract(jsonReader);
      if (object == null) {
         jsonReader.wasNull = true;
         return 0;
      } else if (object instanceof Number) {
         return ((Number)object).intValue();
      } else {
         Function typeConvert = JSONFactory.getDefaultObjectReaderProvider().getTypeConvert(object.getClass(), int.class);
         if (typeConvert == null) {
            throw new JSONException("can not convert to int : " + object);
         } else {
            return (Integer)typeConvert.apply(object);
         }
      }
   }

   @Deprecated
   public static JSONPath compile(String path) {
      return of(path);
   }

   public static JSONPath compile(String strPath, Class objectClass) {
      JSONPath path = of(strPath);
      JSONFactory.JSONPathCompiler compiler = JSONFactory.getDefaultJSONPathCompiler();
      return compiler.compile(objectClass, path);
   }

   static JSONPathSingle of(JSONPathSegment segment) {
      String prefix;
      if (!(segment instanceof JSONPathSegment.MultiIndexSegment) && !(segment instanceof JSONPathSegmentIndex)) {
         prefix = "$.";
      } else {
         prefix = "$";
      }

      String path = prefix + segment.toString();
      return (JSONPathSingle)(segment instanceof JSONPathSegmentName
         ? new JSONPathSingleName(path, (JSONPathSegmentName)segment)
         : new JSONPathSingle(segment, path));
   }

   public static JSONPath of(String path) {
      return (JSONPath)("#-1".equals(path) ? JSONPath.PreviousPath.INSTANCE : new JSONPathParser(path).parse());
   }

   public static JSONPath of(String path, Type type) {
      JSONPath jsonPath = of(path);
      return JSONPathTyped.of(jsonPath, type);
   }

   public static JSONPath of(String path, Type type, JSONPath.Feature... features) {
      JSONPath jsonPath = of(path, features);
      return JSONPathTyped.of(jsonPath, type);
   }

   public static JSONPath of(String[] paths, Type[] types) {
      return of(paths, types, null, null, (ZoneId)null);
   }

   public static JSONPath of(String[] paths, Type[] types, JSONReader.Feature... features) {
      return of(paths, types, null, null, null, features);
   }

   public static JSONPath of(String[] paths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, JSONReader.Feature... features) {
      if (paths.length == 0) {
         throw new JSONException("illegal paths, not support 0 length");
      } else {
         if (types == null) {
            types = new Type[paths.length];
            Arrays.fill(types, Object.class);
         }

         if (types.length != paths.length) {
            throw new JSONException("types.length not equals paths.length");
         } else {
            JSONPath[] jsonPaths = new JSONPath[paths.length];

            for (int i = 0; i < paths.length; i++) {
               jsonPaths[i] = of(paths[i]);
            }

            boolean allSingleName = true;
            boolean allSinglePositiveIndex = true;
            boolean allTwoName = true;
            boolean allTwoIndexPositive = true;
            boolean allThreeName = true;
            boolean sameMultiLength = true;
            JSONPathMulti firstMulti = null;

            for (int i = 0; i < jsonPaths.length; i++) {
               JSONPath path = jsonPaths[i];
               if (i == 0) {
                  if (path instanceof JSONPathMulti) {
                     firstMulti = (JSONPathMulti)path;
                  } else {
                     sameMultiLength = false;
                  }
               } else if (sameMultiLength && path instanceof JSONPathMulti && ((JSONPathMulti)path).segments.size() != firstMulti.segments.size()) {
                  sameMultiLength = false;
               }

               if (allSingleName && !(path instanceof JSONPathSingleName)) {
                  allSingleName = false;
               }

               if (allSinglePositiveIndex && (!(path instanceof JSONPathSingleIndex) || ((JSONPathSingleIndex)path).index < 0)) {
                  allSinglePositiveIndex = false;
               }

               if (allTwoName) {
                  if (path instanceof JSONPathTwoSegment) {
                     JSONPathTwoSegment two = (JSONPathTwoSegment)path;
                     if (!(two.second instanceof JSONPathSegmentName)) {
                        allTwoName = false;
                     }
                  } else {
                     allTwoName = false;
                  }
               }

               if (allTwoIndexPositive) {
                  if (path instanceof JSONPathTwoSegment) {
                     JSONPathTwoSegment two = (JSONPathTwoSegment)path;
                     if (!(two.second instanceof JSONPathSegmentIndex) || ((JSONPathSegmentIndex)two.second).index < 0) {
                        allTwoIndexPositive = false;
                     }
                  } else {
                     allTwoIndexPositive = false;
                  }
               }

               if (allThreeName) {
                  if (path instanceof JSONPathMulti) {
                     JSONPathMulti multi = (JSONPathMulti)path;
                     if (multi.segments.size() == 3) {
                        JSONPathSegment three = multi.segments.get(2);
                        if (multi.segments.get(0) instanceof JSONPathSegment.AllSegment
                           || multi.segments.get(1) instanceof JSONPathSegment.AllSegment
                           || !(three instanceof JSONPathSegmentName)) {
                           allThreeName = false;
                        }
                     } else {
                        allThreeName = false;
                     }
                  } else {
                     allThreeName = false;
                  }
               }
            }

            long featuresValue = JSONReader.Feature.of(features);
            if (allSingleName) {
               return new JSONPathTypedMultiNames(jsonPaths, null, jsonPaths, types, formats, pathFeatures, zoneId, featuresValue);
            } else if (allSinglePositiveIndex) {
               return new JSONPathTypedMultiIndexes(jsonPaths, null, jsonPaths, types, formats, pathFeatures, zoneId, featuresValue);
            } else {
               if (allTwoName || allTwoIndexPositive) {
                  boolean samePrefix = true;
                  JSONPathSegment first0 = ((JSONPathTwoSegment)jsonPaths[0]).first;

                  for (int i = 1; i < jsonPaths.length; i++) {
                     JSONPathTwoSegment two = (JSONPathTwoSegment)jsonPaths[i];
                     if (!first0.equals(two.first)) {
                        samePrefix = false;
                        break;
                     }
                  }

                  if (samePrefix) {
                     JSONPath firstPath = jsonPaths[0];
                     if (allTwoName) {
                        JSONPathSingleName[] names = new JSONPathSingleName[jsonPaths.length];

                        for (int ix = 0; ix < jsonPaths.length; ix++) {
                           JSONPathTwoSegment two = (JSONPathTwoSegment)jsonPaths[ix];
                           JSONPathSegmentName name = (JSONPathSegmentName)two.second;
                           names[ix] = new JSONPathSingleName("$." + name, name);
                        }

                        String prefixPath = firstPath.path.substring(0, firstPath.path.length() - names[0].name.length() - 1);
                        if (first0 instanceof JSONPathSegmentName) {
                           JSONPathSegmentName name = (JSONPathSegmentName)first0;
                           JSONPath prefix = new JSONPathSingleName(prefixPath, name);
                           return new JSONPathTypedMultiNamesPrefixName1(jsonPaths, prefix, names, types, formats, pathFeatures, zoneId, featuresValue);
                        }

                        if (first0 instanceof JSONPathSegmentIndex) {
                           JSONPathSegmentIndex first0Index = (JSONPathSegmentIndex)first0;
                           if (first0Index.index >= 0) {
                              JSONPathSingleIndex prefix = new JSONPathSingleIndex(prefixPath, first0Index);
                              return new JSONPathTypedMultiNamesPrefixIndex1(jsonPaths, prefix, names, types, formats, pathFeatures, zoneId, featuresValue);
                           }
                        }
                     } else {
                        JSONPathSingleIndex[] indexes = new JSONPathSingleIndex[jsonPaths.length];

                        for (int ix = 0; ix < jsonPaths.length; ix++) {
                           JSONPathTwoSegment two = (JSONPathTwoSegment)jsonPaths[ix];
                           JSONPathSegmentIndex name = (JSONPathSegmentIndex)two.second;
                           indexes[ix] = new JSONPathSingleIndex("$" + name, name);
                        }

                        JSONPath prefix = null;
                        if (first0 instanceof JSONPathSegmentName) {
                           JSONPathSegmentName name = (JSONPathSegmentName)first0;
                           prefix = new JSONPathSingleName("$." + name.name, name);
                        } else if (first0 instanceof JSONPathSegmentIndex) {
                           JSONPathSegmentIndex index = (JSONPathSegmentIndex)first0;
                           prefix = new JSONPathSingleIndex("$[" + index.index + "]", index);
                        }

                        if (prefix != null) {
                           return new JSONPathTypedMultiIndexes(jsonPaths, prefix, indexes, types, formats, pathFeatures, zoneId, featuresValue);
                        }
                     }
                  }
               } else if (allThreeName) {
                  boolean samePrefix = true;
                  JSONPathSegment first0 = ((JSONPathMulti)jsonPaths[0]).segments.get(0);
                  JSONPathSegment first1 = ((JSONPathMulti)jsonPaths[0]).segments.get(1);

                  for (int ix = 1; ix < jsonPaths.length; ix++) {
                     JSONPathMulti multi = (JSONPathMulti)jsonPaths[ix];
                     if (!first0.equals(multi.segments.get(0))) {
                        samePrefix = false;
                        break;
                     }

                     if (!first1.equals(multi.segments.get(1))) {
                        samePrefix = false;
                        break;
                     }
                  }

                  if (samePrefix) {
                     JSONPathSingleName[] names = new JSONPathSingleName[jsonPaths.length];

                     for (int ix = 0; ix < jsonPaths.length; ix++) {
                        JSONPathMulti multix = (JSONPathMulti)jsonPaths[ix];
                        JSONPathSegmentName name = (JSONPathSegmentName)multix.segments.get(2);
                        names[ix] = new JSONPathSingleName("$." + name, name);
                     }

                     JSONPath firstPath = jsonPaths[0];
                     String prefixPathx = firstPath.path.substring(0, firstPath.path.length() - names[0].name.length() - 1);
                     JSONPathTwoSegment prefixx = new JSONPathTwoSegment(prefixPathx, first0, first1);
                     if (first0 instanceof JSONPathSegmentName && first1 instanceof JSONPathSegmentName) {
                        return new JSONPathTypedMultiNamesPrefixName2(jsonPaths, prefixx, names, types, formats, pathFeatures, zoneId, featuresValue);
                     }

                     return new JSONPathTypedMultiNames(jsonPaths, prefixx, names, types, formats, pathFeatures, zoneId, featuresValue);
                  }
               }

               if (sameMultiLength && paths.length > 1) {
                  boolean samePrefix = true;
                  boolean sameType = true;
                  int lastIndex = firstMulti.segments.size() - 1;
                  JSONPathSegment lastSegment = firstMulti.segments.get(lastIndex);

                  for (int ix = 0; ix < lastIndex; ix++) {
                     JSONPathSegment segment = firstMulti.segments.get(ix);

                     for (int j = 1; j < paths.length; j++) {
                        JSONPath jsonPath = jsonPaths[j];
                        JSONPathSegment segment1;
                        if (jsonPath instanceof JSONPathMulti) {
                           JSONPathMulti pathx = (JSONPathMulti)jsonPath;
                           segment1 = pathx.segments.get(ix);
                        } else if (jsonPath instanceof JSONPathSingleName) {
                           segment1 = ((JSONPathSingleName)jsonPath).segment;
                        } else if (jsonPath instanceof JSONPathSingleIndex) {
                           segment1 = ((JSONPathSingleIndex)jsonPath).segment;
                        } else {
                           segment1 = null;
                        }

                        if (!segment.equals(segment1)) {
                           samePrefix = false;
                           break;
                        }
                     }

                     if (!samePrefix) {
                        break;
                     }
                  }

                  if (samePrefix) {
                     for (int ix = 1; ix < paths.length; ix++) {
                        JSONPathMulti pathx = (JSONPathMulti)jsonPaths[ix];
                        if (!lastSegment.getClass().equals(pathx.segments.get(lastIndex).getClass())) {
                           sameType = false;
                           break;
                        }
                     }

                     if (sameType) {
                        List<JSONPathSegment> prefixSegments = firstMulti.segments.subList(0, lastIndex - 1);
                        String prefixPathx = null;
                        int dotIndex = firstMulti.path.lastIndexOf(46);
                        if (dotIndex != -1) {
                           prefixPathx = firstMulti.path.substring(0, dotIndex - 1);
                        }

                        if (prefixPathx != null) {
                           JSONPathMulti prefixx = new JSONPathMulti(prefixPathx, prefixSegments);
                           if (lastSegment instanceof JSONPathSegmentIndex) {
                              JSONPath[] indexPaths = new JSONPath[paths.length];

                              for (int ixx = 0; ixx < jsonPaths.length; ixx++) {
                                 JSONPathMulti pathx = (JSONPathMulti)jsonPaths[ixx];
                                 JSONPathSegmentIndex lastSegmentIndex = (JSONPathSegmentIndex)pathx.segments.get(lastIndex);
                                 indexPaths[ixx] = new JSONPathSingleIndex(lastSegmentIndex.toString(), lastSegmentIndex);
                              }

                              return new JSONPathTypedMultiIndexes(jsonPaths, prefixx, indexPaths, types, formats, pathFeatures, zoneId, featuresValue);
                           }
                        }
                     }
                  }
               }

               return new JSONPathTypedMulti(jsonPaths, types, formats, pathFeatures, zoneId, featuresValue);
            }
         }
      }
   }

   public static JSONPath of(String path, JSONPath.Feature... features) {
      return (JSONPath)("#-1".equals(path) ? JSONPath.PreviousPath.INSTANCE : new JSONPathParser(path).parse(features));
   }

   static JSONPathFilter.Operator parseOperator(JSONReader jsonReader) {
      JSONPathFilter.Operator operator;
      switch (jsonReader.ch) {
         case '!':
            jsonReader.next();
            if (jsonReader.ch != '=') {
               throw new JSONException("not support operator : !" + jsonReader.ch);
            }

            jsonReader.next();
            operator = JSONPathFilter.Operator.NE;
            break;
         case '<':
            jsonReader.next();
            if (jsonReader.ch == '=') {
               jsonReader.next();
               operator = JSONPathFilter.Operator.LE;
            } else if (jsonReader.ch == '>') {
               jsonReader.next();
               operator = JSONPathFilter.Operator.NE;
            } else {
               operator = JSONPathFilter.Operator.LT;
            }
            break;
         case '=':
            jsonReader.next();
            if (jsonReader.ch == '~') {
               jsonReader.next();
               operator = JSONPathFilter.Operator.REG_MATCH;
            } else if (jsonReader.ch == '=') {
               jsonReader.next();
               operator = JSONPathFilter.Operator.EQ;
            } else {
               operator = JSONPathFilter.Operator.EQ;
            }
            break;
         case '>':
            jsonReader.next();
            if (jsonReader.ch == '=') {
               jsonReader.next();
               operator = JSONPathFilter.Operator.GE;
            } else {
               operator = JSONPathFilter.Operator.GT;
            }
            break;
         case 'B':
         case 'b':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = jsonReader.getFieldName();
            if (!"between".equalsIgnoreCase(fieldName)) {
               throw new JSONException("not support operator : " + fieldName);
            }

            operator = JSONPathFilter.Operator.BETWEEN;
            break;
         case 'E':
         case 'e':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = jsonReader.getFieldName();
            if ("ends".equalsIgnoreCase(fieldName)) {
               jsonReader.readFieldNameHashCodeUnquote();
               fieldName = jsonReader.getFieldName();
               if (!"with".equalsIgnoreCase(fieldName)) {
                  throw new JSONException("not support operator : " + fieldName);
               }
            } else if (!"endsWith".equalsIgnoreCase(fieldName)) {
               throw new JSONException("not support operator : " + fieldName);
            }

            operator = JSONPathFilter.Operator.ENDS_WITH;
            break;
         case 'I':
         case 'i':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = jsonReader.getFieldName();
            if ("in".equalsIgnoreCase(fieldName)) {
               operator = JSONPathFilter.Operator.IN;
            } else {
               if (!"is".equalsIgnoreCase(fieldName)) {
                  throw new JSONException("not support operator : " + fieldName);
               }

               operator = JSONPathFilter.Operator.EQ;
            }
            break;
         case 'L':
         case 'l':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldNamexx = jsonReader.getFieldName();
            if (!"like".equalsIgnoreCase(fieldNamexx)) {
               throw new JSONException("not support operator : " + fieldNamexx);
            }

            operator = JSONPathFilter.Operator.LIKE;
            break;
         case 'N':
         case 'n':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldNamex = jsonReader.getFieldName();
            if ("nin".equalsIgnoreCase(fieldNamex)) {
               operator = JSONPathFilter.Operator.NOT_IN;
            } else {
               if (!"not".equalsIgnoreCase(fieldNamex)) {
                  throw new JSONException("not support operator : " + fieldNamex);
               }

               jsonReader.readFieldNameHashCodeUnquote();
               fieldNamex = jsonReader.getFieldName();
               if ("like".equalsIgnoreCase(fieldNamex)) {
                  operator = JSONPathFilter.Operator.NOT_LIKE;
               } else if ("rlike".equalsIgnoreCase(fieldNamex)) {
                  operator = JSONPathFilter.Operator.NOT_RLIKE;
               } else if ("in".equalsIgnoreCase(fieldNamex)) {
                  operator = JSONPathFilter.Operator.NOT_IN;
               } else {
                  if (!"between".equalsIgnoreCase(fieldNamex)) {
                     throw new JSONException("not support operator : " + fieldNamex);
                  }

                  operator = JSONPathFilter.Operator.NOT_BETWEEN;
               }
            }
            break;
         case 'R':
         case 'r':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = jsonReader.getFieldName();
            if (!"rlike".equalsIgnoreCase(fieldName)) {
               throw new JSONException("not support operator : " + fieldName);
            }

            operator = JSONPathFilter.Operator.RLIKE;
            break;
         case 'S':
         case 's':
            jsonReader.readFieldNameHashCodeUnquote();
            String fieldName = jsonReader.getFieldName();
            if ("starts".equalsIgnoreCase(fieldName)) {
               jsonReader.readFieldNameHashCodeUnquote();
               fieldName = jsonReader.getFieldName();
               if (!"with".equalsIgnoreCase(fieldName)) {
                  throw new JSONException("not support operator : " + fieldName);
               }
            } else if (!"startsWith".equalsIgnoreCase(fieldName)) {
               throw new JSONException("not support operator : " + fieldName);
            }

            operator = JSONPathFilter.Operator.STARTS_WITH;
            break;
         default:
            jsonReader.readFieldNameHashCodeUnquote();
            throw new JSONException("not support operator : " + jsonReader.getFieldName());
      }

      return operator;
   }

   static final class Context {
      final JSONPath path;
      final JSONPath.Context parent;
      final JSONPathSegment current;
      final JSONPathSegment next;
      final long readerFeatures;
      Object root;
      Object value;
      boolean eval;

      Context(JSONPath path, JSONPath.Context parent, JSONPathSegment current, JSONPathSegment next, long readerFeatures) {
         this.path = path;
         this.current = current;
         this.next = next;
         this.parent = parent;
         this.readerFeatures = readerFeatures;
      }
   }

   public static enum Feature {
      AlwaysReturnList(1L),
      NullOnError(2L),
      KeepNullValue(4L);

      public final long mask;

      private Feature(long mask) {
         this.mask = mask;
      }
   }

   static final class PreviousPath extends JSONPath {
      static final JSONPath.PreviousPath INSTANCE = new JSONPath.PreviousPath("#-1");

      PreviousPath(String path) {
         super(path);
      }

      @Override
      public boolean isRef() {
         throw new JSONException("unsupported operation");
      }

      @Override
      public boolean isPrevious() {
         return true;
      }

      @Override
      public boolean contains(Object rootObject) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public Object eval(Object rootObject) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public Object extract(JSONReader jsonReader) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public String extractScalar(JSONReader jsonReader) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void set(Object rootObject, Object value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void set(Object rootObject, Object value, JSONReader.Feature... readerFeatures) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setCallback(Object rootObject, BiFunction callback) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public JSONPath getParent() {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setInt(Object rootObject, int value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setLong(Object rootObject, long value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public boolean remove(Object rootObject) {
         throw new JSONException("unsupported operation");
      }
   }

   static final class RootPath extends JSONPath {
      static final JSONPath.RootPath INSTANCE = new JSONPath.RootPath();

      private RootPath() {
         super("$");
      }

      @Override
      public boolean isRef() {
         return true;
      }

      @Override
      public boolean contains(Object object) {
         return false;
      }

      @Override
      public Object eval(Object object) {
         return object;
      }

      @Override
      public Object extract(JSONReader jsonReader) {
         return jsonReader == null ? null : jsonReader.readAny();
      }

      @Override
      public String extractScalar(JSONReader jsonReader) {
         Object any = jsonReader.readAny();
         return JSON.toJSONString(any);
      }

      @Override
      public void set(Object object, Object value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void set(Object object, Object value, JSONReader.Feature... readerFeatures) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setCallback(Object object, BiFunction callback) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setInt(Object object, int value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public void setLong(Object object, long value) {
         throw new JSONException("unsupported operation");
      }

      @Override
      public boolean remove(Object object) {
         return false;
      }

      @Override
      public JSONPath getParent() {
         return null;
      }
   }

   static class Sequence {
      final List values;

      public Sequence(List values) {
         this.values = values;
      }
   }
}
