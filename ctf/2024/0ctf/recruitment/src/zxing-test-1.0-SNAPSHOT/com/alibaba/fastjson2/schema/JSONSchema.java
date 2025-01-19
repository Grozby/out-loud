package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONSchemaValidException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.reader.ObjectReaderBean;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@JSONType(
   serializer = JSONSchema.JSONSchemaWriter.class
)
public abstract class JSONSchema {
   static final Map<String, JSONSchema> CACHE = new ConcurrentHashMap<>();
   final String title;
   final String description;
   static final JSONReader.Context CONTEXT = JSONFactory.createReadContext();
   static final ValidateResult SUCCESS = new ValidateResult(true, "success");
   static final ValidateResult FAIL_INPUT_NULL = new ValidateResult(false, "input null");
   static final ValidateResult FAIL_INPUT_NOT_ENCODED = new ValidateResult(false, "input not encoded string");
   static final ValidateResult FAIL_ANY_OF = new ValidateResult(false, "anyOf fail");
   static final ValidateResult FAIL_ONE_OF = new ValidateResult(false, "oneOf fail");
   static final ValidateResult FAIL_NOT = new ValidateResult(false, "not fail");
   static final ValidateResult FAIL_TYPE_NOT_MATCH = new ValidateResult(false, "type not match");
   static final ValidateResult FAIL_PROPERTY_NAME = new ValidateResult(false, "propertyName not match");
   static final ValidateResult CONTAINS_NOT_MATCH = new ValidateResult(false, "contains not match");
   static final ValidateResult UNIQUE_ITEMS_NOT_MATCH = new ValidateResult(false, "uniqueItems not match");
   static final ValidateResult REQUIRED_NOT_MATCH = new ValidateResult(false, "required");

   JSONSchema(JSONObject input) {
      this.title = input.getString("title");
      this.description = input.getString("description");
   }

   JSONSchema(String title, String description) {
      this.title = title;
      this.description = description;
   }

   void addResolveTask(UnresolvedReference.ResolveTask task) {
   }

   public static JSONSchema of(JSONObject input, Class objectClass) {
      if (input == null || input.isEmpty()) {
         return null;
      } else if (objectClass == null || objectClass == Object.class) {
         return of(input);
      } else if (objectClass != byte.class
         && objectClass != short.class
         && objectClass != int.class
         && objectClass != long.class
         && objectClass != Byte.class
         && objectClass != Short.class
         && objectClass != Integer.class
         && objectClass != Long.class
         && objectClass != BigInteger.class
         && objectClass != AtomicInteger.class
         && objectClass != AtomicLong.class) {
         if (objectClass != BigDecimal.class
            && objectClass != float.class
            && objectClass != double.class
            && objectClass != Float.class
            && objectClass != Double.class
            && objectClass != Number.class) {
            if (objectClass == boolean.class || objectClass == Boolean.class) {
               return new BooleanSchema(input);
            } else if (objectClass == String.class) {
               return new StringSchema(input);
            } else if (Collection.class.isAssignableFrom(objectClass)) {
               return new ArraySchema(input, null);
            } else {
               return (JSONSchema)(objectClass.isArray() ? new ArraySchema(input, null) : new ObjectSchema(input, null));
            }
         } else if (input.containsKey("AnyOf") || input.containsKey("anyOf")) {
            return anyOf(input, objectClass);
         } else if (input.containsKey("oneOf")) {
            return oneOf(input, objectClass);
         } else {
            return (JSONSchema)(input.containsKey("not") ? ofNot(input, objectClass) : new NumberSchema(input));
         }
      } else if (input.containsKey("AnyOf") || input.containsKey("anyOf")) {
         return anyOf(input, objectClass);
      } else if (input.containsKey("oneOf")) {
         return oneOf(input, objectClass);
      } else {
         return (JSONSchema)(input.containsKey("not") ? ofNot(input, objectClass) : new IntegerSchema(input));
      }
   }

   static Not ofNot(JSONObject input, Class objectClass) {
      Object not = input.get("not");
      if (not instanceof Boolean) {
         return new Not(null, null, (Boolean)not);
      } else {
         JSONObject object = (JSONObject)not;
         if (object != null && !object.isEmpty()) {
            if (object.size() == 1) {
               Object type = object.get("type");
               if (type instanceof JSONArray) {
                  JSONArray array = (JSONArray)type;
                  JSONSchema.Type[] types = new JSONSchema.Type[array.size()];

                  for (int i = 0; i < array.size(); i++) {
                     types[i] = array.getObject(i, JSONSchema.Type.class);
                  }

                  return new Not(null, types, null);
               }
            }

            JSONSchema schema = of(object, objectClass);
            return new Not(schema, null, null);
         } else {
            return new Not(null, new JSONSchema.Type[]{JSONSchema.Type.Any}, null);
         }
      }
   }

   public static JSONSchema parseSchema(String schema) {
      if ("true".equals(schema)) {
         return Any.INSTANCE;
      } else if ("false".equals(schema)) {
         return Any.NOT_ANY;
      } else {
         JSONReader reader = JSONReader.of(schema);

         JSONSchema var4;
         try {
            ObjectReader<?> objectReader = reader.getObjectReader(Object.class);
            JSONObject object = (JSONObject)objectReader.readObject(reader, null, null, 0L);
            var4 = of(object);
         } catch (Throwable var6) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (reader != null) {
            reader.close();
         }

         return var4;
      }
   }

   @JSONCreator
   public static JSONSchema of(JSONObject input) {
      return of(input, (JSONSchema)null);
   }

   public static JSONSchema of(java.lang.reflect.Type type) {
      return of(type, null);
   }

   public static JSONSchema ofValue(Object value) {
      return ofValue(value, null);
   }

   static JSONSchema ofValue(Object value, JSONSchema root) {
      if (value == null) {
         return null;
      } else {
         if (value instanceof Collection) {
            Collection collection = (Collection)value;
            if (collection.isEmpty()) {
               return new ArraySchema(JSONObject.of("type", "array"), root);
            }

            Object firstItem = null;
            Class firstItemClass = null;
            boolean sameClass = true;

            for (Object item : collection) {
               if (item != null) {
                  if (firstItem == null) {
                     firstItem = item;
                  }

                  if (firstItemClass == null) {
                     firstItemClass = item.getClass();
                  } else if (firstItemClass != item.getClass()) {
                     sameClass = false;
                  }
               }
            }

            if (sameClass) {
               JSONSchema itemSchema;
               if (Map.class.isAssignableFrom(firstItemClass)) {
                  itemSchema = ofValue(firstItem, root);
               } else {
                  itemSchema = of(firstItemClass, root);
               }

               ArraySchema schema = new ArraySchema(JSONObject.of("type", "array"), root);
               schema.itemSchema = itemSchema;
               return schema;
            }
         }

         if (value instanceof Map) {
            JSONObject object = JSONObject.of("type", "object");
            ObjectSchema schema = new ObjectSchema(object, root);
            Map map = (Map)value;

            for (Entry entry : map.entrySet()) {
               Object entryKey = entry.getKey();
               Object entryValue = entry.getValue();
               if (entryKey instanceof String) {
                  JSONSchema valueSchema;
                  if (entryValue == null) {
                     valueSchema = new StringSchema(JSONObject.of());
                  } else {
                     valueSchema = ofValue(entryValue, (JSONSchema)(root == null ? schema : root));
                  }

                  schema.properties.put((String)entryKey, valueSchema);
               }
            }

            return schema;
         } else {
            return of(value.getClass(), root);
         }
      }
   }

   static JSONSchema of(java.lang.reflect.Type type, JSONSchema root) {
      if (type instanceof ParameterizedType) {
         ParameterizedType paramType = (ParameterizedType)type;
         java.lang.reflect.Type rawType = paramType.getRawType();
         java.lang.reflect.Type[] arguments = paramType.getActualTypeArguments();
         if (rawType instanceof Class && Collection.class.isAssignableFrom((Class<?>)rawType)) {
            JSONObject object = JSONObject.of("type", "array");
            ArraySchema arraySchema = new ArraySchema(object, root);
            if (arguments.length == 1) {
               arraySchema.itemSchema = of(arguments[0], (JSONSchema)(root == null ? arraySchema : root));
            }

            return arraySchema;
         }

         if (rawType instanceof Class && Map.class.isAssignableFrom((Class<?>)rawType)) {
            JSONObject object = JSONObject.of("type", "object");
            return new ObjectSchema(object, root);
         }
      }

      if (type instanceof GenericArrayType) {
         GenericArrayType arrayType = (GenericArrayType)type;
         java.lang.reflect.Type componentType = arrayType.getGenericComponentType();
         JSONObject object = JSONObject.of("type", "array");
         ArraySchema arraySchema = new ArraySchema(object, root);
         arraySchema.itemSchema = of(componentType, (JSONSchema)(root == null ? arraySchema : root));
         return arraySchema;
      } else if (type == byte.class
         || type == short.class
         || type == int.class
         || type == long.class
         || type == Byte.class
         || type == Short.class
         || type == Integer.class
         || type == Long.class
         || type == BigInteger.class
         || type == AtomicInteger.class
         || type == AtomicLong.class) {
         return new IntegerSchema(JSONObject.of("type", "integer"));
      } else if (type == float.class || type == double.class || type == Float.class || type == Double.class || type == BigDecimal.class) {
         return new NumberSchema(JSONObject.of("type", "number"));
      } else if (type == boolean.class || type == Boolean.class || type == AtomicBoolean.class) {
         return new BooleanSchema(JSONObject.of("type", "boolean"));
      } else if (type == String.class) {
         return new StringSchema(JSONObject.of("type", "string"));
      } else {
         if (type instanceof Class) {
            Class schemaClass = (Class)type;
            if (Enum.class.isAssignableFrom(schemaClass)) {
               Object[] enums = schemaClass.getEnumConstants();
               String[] names = new String[enums.length];

               for (int i = 0; i < enums.length; i++) {
                  names[i] = ((Enum)enums[i]).name();
               }

               return new StringSchema(JSONObject.of("type", "string", "enum", names));
            }

            if (schemaClass.isArray()) {
               Class componentType = schemaClass.getComponentType();
               JSONObject object = JSONObject.of("type", "array");
               ArraySchema arraySchema = new ArraySchema(object, root);
               arraySchema.itemSchema = of(componentType, (JSONSchema)(root == null ? arraySchema : root));
               return arraySchema;
            }

            if (Map.class.isAssignableFrom(schemaClass)) {
               return new ObjectSchema(JSONObject.of("type", "object"), root);
            }

            if (Collection.class.isAssignableFrom(schemaClass)) {
               return new ArraySchema(JSONObject.of("type", "array"), root);
            }
         }

         ObjectReader reader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(type);
         if (reader instanceof ObjectReaderBean) {
            ObjectReaderAdapter adapter = (ObjectReaderAdapter)reader;
            JSONArray required = new JSONArray();
            adapter.apply(e -> {
               if (e.fieldClass.isPrimitive()) {
                  required.add(e.fieldName);
               }
            });
            JSONObject object = JSONObject.of("type", "object", "required", required);
            ObjectSchema schema = new ObjectSchema(object);
            adapter.apply(e -> schema.properties.put(e.fieldName, of(e.fieldType, (JSONSchema)(root == null ? schema : root))));
            return schema;
         } else {
            throw new JSONException("TODO : " + type);
         }
      }
   }

   @JSONCreator
   public static JSONSchema of(JSONObject input, JSONSchema parent) {
      if (input.size() == 1 && input.isArray("type")) {
         JSONArray types = input.getJSONArray("type");
         JSONSchema[] items = new JSONSchema[types.size()];

         for (int i = 0; i < types.size(); i++) {
            items[i] = of(JSONObject.of("type", types.get(i)));
         }

         return new AnyOf(items);
      } else {
         JSONSchema.Type type = JSONSchema.Type.of(input.getString("type"));
         if (type != null) {
            switch (type) {
               case Null:
                  return new NullSchema(input);
               case Boolean:
                  return new BooleanSchema(input);
               case Object:
                  return new ObjectSchema(input, parent);
               case Array:
                  return new ArraySchema(input, parent);
               case Number:
                  return new NumberSchema(input);
               case String:
                  return new StringSchema(input);
               case Integer:
                  return new IntegerSchema(input);
               default:
                  throw new JSONSchemaValidException("not support type : " + type);
            }
         } else {
            Object[] enums = input.getObject("enum", Object[].class);
            if (enums != null) {
               boolean nonString = false;

               for (Object anEnum : enums) {
                  if (!(anEnum instanceof String)) {
                     nonString = true;
                     break;
                  }
               }

               return (JSONSchema)(!nonString ? new StringSchema(input) : new EnumSchema(enums));
            } else {
               Object constValue = input.get("const");
               if (constValue instanceof String) {
                  return new StringSchema(input);
               } else if (!(constValue instanceof Integer) && !(constValue instanceof Long)) {
                  if (input.size() == 1) {
                     String ref = input.getString("$ref");
                     if (ref != null && !ref.isEmpty()) {
                        if ("http://json-schema.org/draft-04/schema#".equals(ref)) {
                           JSONSchema schema = CACHE.get(ref);
                           if (schema == null) {
                              URL draf4Resource = JSONSchema.class.getClassLoader().getResource("schema/draft-04.json");
                              schema = of(JSON.parseObject(draf4Resource), (JSONSchema)null);
                              JSONSchema origin = CACHE.putIfAbsent(ref, schema);
                              if (origin != null) {
                                 schema = origin;
                              }
                           }

                           return schema;
                        }

                        if ("#".equals(ref)) {
                           return parent;
                        }

                        Map<String, JSONSchema> definitions = null;
                        Map<String, JSONSchema> defs = null;
                        Map<String, JSONSchema> properties = null;
                        if (parent instanceof ObjectSchema) {
                           ObjectSchema objectSchema = (ObjectSchema)parent;
                           definitions = objectSchema.definitions;
                           defs = objectSchema.defs;
                           properties = objectSchema.properties;
                        } else if (parent instanceof ArraySchema) {
                           definitions = ((ArraySchema)parent).definitions;
                           defs = ((ArraySchema)parent).defs;
                        }

                        if (definitions != null && ref.startsWith("#/definitions/")) {
                           int PREFIX_LEN = 14;
                           String refName = ref.substring(14);
                           return definitions.get(refName);
                        }

                        if (defs != null && ref.startsWith("#/$defs/")) {
                           int PREFIX_LEN = 8;
                           String refName = ref.substring(8);
                           refName = URLDecoder.decode(refName);
                           JSONSchema refSchema = defs.get(refName);
                           if (refSchema == null) {
                              refSchema = new UnresolvedReference(refName);
                           }

                           return refSchema;
                        }

                        if (properties != null && ref.startsWith("#/properties/")) {
                           int PREFIX_LEN = 13;
                           String refName = ref.substring(13);
                           return properties.get(refName);
                        }

                        if (ref.startsWith("#/prefixItems/") && parent instanceof ArraySchema) {
                           int PREFIX_LEN = 14;
                           int index = Integer.parseInt(ref.substring(14));
                           return ((ArraySchema)parent).prefixItems[index];
                        }
                     }

                     Object exclusiveMaximum = input.get("exclusiveMaximum");
                     Object exclusiveMinimum = input.get("exclusiveMinimum");
                     if (exclusiveMaximum instanceof Integer
                        || exclusiveMinimum instanceof Integer
                        || exclusiveMaximum instanceof Long
                        || exclusiveMinimum instanceof Long) {
                        return new IntegerSchema(input);
                     }

                     if (exclusiveMaximum instanceof Number || exclusiveMinimum instanceof Number) {
                        return new NumberSchema(input);
                     }
                  }

                  if (input.containsKey("properties")
                     || input.containsKey("dependentSchemas")
                     || input.containsKey("if")
                     || input.containsKey("required")
                     || input.containsKey("patternProperties")
                     || input.containsKey("additionalProperties")
                     || input.containsKey("minProperties")
                     || input.containsKey("maxProperties")
                     || input.containsKey("propertyNames")
                     || input.containsKey("$ref")) {
                     return new ObjectSchema(input, parent);
                  } else if (input.containsKey("maxItems")
                     || input.containsKey("minItems")
                     || input.containsKey("additionalItems")
                     || input.containsKey("items")
                     || input.containsKey("prefixItems")
                     || input.containsKey("uniqueItems")
                     || input.containsKey("maxContains")
                     || input.containsKey("minContains")) {
                     return new ArraySchema(input, parent);
                  } else if (!input.containsKey("pattern")
                     && !input.containsKey("format")
                     && !input.containsKey("minLength")
                     && !input.containsKey("maxLength")) {
                     boolean allOf = input.containsKey("allOf");
                     boolean anyOf = input.containsKey("anyOf");
                     boolean oneOf = input.containsKey("oneOf");
                     if (allOf || anyOf || oneOf) {
                        int count = (allOf ? 1 : 0) + (anyOf ? 1 : 0) + (oneOf ? 1 : 0);
                        if (count != 1) {
                           JSONSchema[] items = new JSONSchema[count];
                           int index = 0;
                           if (allOf) {
                              items[index++] = new AllOf(input, parent);
                           }

                           if (anyOf) {
                              items[index++] = new AnyOf(input, parent);
                           }

                           if (oneOf) {
                              items[index++] = new OneOf(input, parent);
                           }

                           return new AllOf(items);
                        } else if (allOf) {
                           return new AllOf(input, parent);
                        } else {
                           return (JSONSchema)(anyOf ? new AnyOf(input, parent) : new OneOf(input, parent));
                        }
                     } else if (input.containsKey("not")) {
                        return ofNot(input, null);
                     } else if (input.get("maximum") instanceof Number || input.get("minimum") instanceof Number || input.containsKey("multipleOf")) {
                        return new NumberSchema(input);
                     } else if (input.isEmpty()) {
                        return Any.INSTANCE;
                     } else {
                        if (input.size() == 1) {
                           Object propertyType = input.get("type");
                           if (propertyType instanceof JSONArray) {
                              JSONArray array = (JSONArray)propertyType;
                              JSONSchema[] typeSchemas = new JSONSchema[array.size()];

                              for (int i = 0; i < array.size(); i++) {
                                 JSONSchema.Type itemType = JSONSchema.Type.of(array.getString(i));
                                 switch (itemType) {
                                    case Null:
                                       typeSchemas[i] = new NullSchema(JSONObject.of("type", "null"));
                                       break;
                                    case Boolean:
                                       typeSchemas[i] = new BooleanSchema(JSONObject.of("type", "boolean"));
                                       break;
                                    case Object:
                                       typeSchemas[i] = new ObjectSchema(JSONObject.of("type", "object"));
                                       break;
                                    case Array:
                                       typeSchemas[i] = new ArraySchema(JSONObject.of("type", "array"), null);
                                       break;
                                    case Number:
                                       typeSchemas[i] = new NumberSchema(JSONObject.of("type", "number"));
                                       break;
                                    case String:
                                       typeSchemas[i] = new StringSchema(JSONObject.of("type", "string"));
                                       break;
                                    case Integer:
                                       typeSchemas[i] = new IntegerSchema(JSONObject.of("type", "integer"));
                                       break;
                                    default:
                                       throw new JSONSchemaValidException("not support type : " + itemType);
                                 }
                              }

                              return new AnyOf(typeSchemas);
                           }
                        }

                        if (input.getString("type") == null) {
                           throw new JSONSchemaValidException("type required");
                        } else {
                           throw new JSONSchemaValidException("not support type : " + input.getString("type"));
                        }
                     }
                  } else {
                     return new StringSchema(input);
                  }
               } else {
                  return new IntegerSchema(input);
               }
            }
         }
      }
   }

   static AnyOf anyOf(JSONObject input, Class type) {
      JSONArray array = input.getJSONArray("anyOf");
      if (array != null && !array.isEmpty()) {
         JSONSchema[] items = new JSONSchema[array.size()];

         for (int i = 0; i < items.length; i++) {
            items[i] = of(array.getJSONObject(i), type);
         }

         return new AnyOf(items);
      } else {
         return null;
      }
   }

   static AnyOf anyOf(JSONArray array, Class type) {
      if (array != null && !array.isEmpty()) {
         JSONSchema[] items = new JSONSchema[array.size()];

         for (int i = 0; i < items.length; i++) {
            items[i] = of(array.getJSONObject(i), type);
         }

         return new AnyOf(items);
      } else {
         return null;
      }
   }

   static AllOf allOf(JSONObject input, Class type) {
      JSONArray array = input.getJSONArray("allOf");
      if (array != null && !array.isEmpty()) {
         JSONSchema[] items = new JSONSchema[array.size()];

         for (int i = 0; i < items.length; i++) {
            items[i] = of(array.getJSONObject(i), type);
         }

         return new AllOf(items);
      } else {
         return null;
      }
   }

   static OneOf oneOf(JSONObject input, Class type) {
      JSONArray array = input.getJSONArray("oneOf");
      if (array != null && !array.isEmpty()) {
         JSONSchema[] items = new JSONSchema[array.size()];

         for (int i = 0; i < items.length; i++) {
            items[i] = of(array.getJSONObject(i), type);
         }

         return new OneOf(items);
      } else {
         return null;
      }
   }

   static OneOf oneOf(JSONArray array, Class type) {
      if (array != null && !array.isEmpty()) {
         JSONSchema[] items = new JSONSchema[array.size()];

         for (int i = 0; i < items.length; i++) {
            items[i] = of(array.getJSONObject(i), type);
         }

         return new OneOf(items);
      } else {
         return null;
      }
   }

   public String getTitle() {
      return this.title;
   }

   public String getDescription() {
      return this.description;
   }

   public abstract JSONSchema.Type getType();

   public abstract ValidateResult validate(Object var1);

   public boolean isValid(Object value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(long value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(double value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(Double value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(float value) {
      return this.validate((double)value).isSuccess();
   }

   public boolean isValid(Float value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(Integer value) {
      return this.validate(value).isSuccess();
   }

   public boolean isValid(Long value) {
      return this.validate(value).isSuccess();
   }

   public ValidateResult validate(long value) {
      return this.validate(Long.valueOf(value));
   }

   public ValidateResult validate(double value) {
      return this.validate(Double.valueOf(value));
   }

   public ValidateResult validate(Float value) {
      return this.validate((Object)value);
   }

   public ValidateResult validate(Double value) {
      return this.validate((Object)value);
   }

   public ValidateResult validate(Integer value) {
      return this.validate((Object)value);
   }

   public ValidateResult validate(Long value) {
      return this.validate((Object)value);
   }

   public void assertValidate(Object value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(Integer value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(Long value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(Double value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(Float value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(long value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   public void assertValidate(double value) {
      ValidateResult result = this.validate(value);
      if (!result.isSuccess()) {
         throw new JSONSchemaValidException(result.getMessage());
      }
   }

   @Override
   public String toString() {
      return this.toJSONObject().toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         JSONSchema that = (JSONSchema)o;
         JSONObject thisObj = this.toJSONObject();
         JSONObject thatObj = that.toJSONObject();
         return thisObj.equals(thatObj);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.toJSONObject().hashCode();
   }

   public JSONObject toJSONObject() {
      return new JSONObject();
   }

   public void accept(Predicate<JSONSchema> v) {
      v.test(this);
   }

   static class JSONSchemaWriter implements ObjectWriter {
      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, java.lang.reflect.Type fieldType, long features) {
         JSONObject jsonObject = ((JSONSchema)object).toJSONObject();
         jsonWriter.write(jsonObject);
      }
   }

   public static enum Type {
      Null,
      Boolean,
      Object,
      Array,
      Number,
      String,
      Integer,
      Enum,
      Const,
      OneOf,
      AllOf,
      AnyOf,
      Any,
      UnresolvedReference;

      public static JSONSchema.Type of(String typeStr) {
         if (typeStr == null) {
            return null;
         } else {
            switch (typeStr) {
               case "Null":
               case "null":
                  return Null;
               case "String":
               case "string":
                  return String;
               case "Integer":
               case "integer":
                  return Integer;
               case "Number":
               case "number":
                  return Number;
               case "Boolean":
               case "boolean":
                  return Boolean;
               case "Object":
               case "object":
                  return Object;
               case "Array":
               case "array":
                  return Array;
               default:
                  return null;
            }
         }
      }
   }
}
