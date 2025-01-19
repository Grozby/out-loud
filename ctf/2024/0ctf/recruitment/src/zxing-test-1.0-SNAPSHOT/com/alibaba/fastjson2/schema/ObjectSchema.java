package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ObjectSchema extends JSONSchema {
   final boolean typed;
   final Map<String, JSONSchema> definitions;
   final Map<String, JSONSchema> defs;
   final Map<String, JSONSchema> properties;
   final Set<String> required;
   final boolean additionalProperties;
   final JSONSchema additionalPropertySchema;
   final long[] requiredHashCode;
   final ObjectSchema.PatternProperty[] patternProperties;
   final JSONSchema propertyNames;
   final int minProperties;
   final int maxProperties;
   final Map<String, String[]> dependentRequired;
   final Map<Long, long[]> dependentRequiredHashCodes;
   final Map<String, JSONSchema> dependentSchemas;
   final Map<Long, JSONSchema> dependentSchemasHashMapping;
   final JSONSchema ifSchema;
   final JSONSchema thenSchema;
   final JSONSchema elseSchema;
   final AllOf allOf;
   final AnyOf anyOf;
   final OneOf oneOf;
   final boolean encoded;
   transient List<UnresolvedReference.ResolveTask> resolveTasks;

   public ObjectSchema(JSONObject input) {
      this(input, null);
   }

   public ObjectSchema(JSONObject input, JSONSchema root) {
      super(input);
      this.typed = "object".equalsIgnoreCase(input.getString("type"));
      this.properties = new LinkedHashMap<>();
      this.definitions = new LinkedHashMap<>();
      this.defs = new LinkedHashMap<>();
      this.encoded = input.getBooleanValue("encoded", false);
      JSONObject definitions = input.getJSONObject("definitions");
      if (definitions != null) {
         for (Entry<String, Object> entry : definitions.entrySet()) {
            String entryKey = entry.getKey();
            JSONObject entryValue = (JSONObject)entry.getValue();
            JSONSchema schema = JSONSchema.of(entryValue, (JSONSchema)(root == null ? this : root));
            this.definitions.put(entryKey, schema);
         }
      }

      JSONObject defs = input.getJSONObject("$defs");
      if (defs != null) {
         for (Entry<String, Object> entry : defs.entrySet()) {
            String entryKey = entry.getKey();
            JSONObject entryValue = (JSONObject)entry.getValue();
            JSONSchema schema = JSONSchema.of(entryValue, (JSONSchema)(root == null ? this : root));
            this.defs.put(entryKey, schema);
         }

         if (this.resolveTasks != null) {
            for (UnresolvedReference.ResolveTask resolveTask : this.resolveTasks) {
               resolveTask.resolve(this);
            }
         }
      }

      JSONObject properties = input.getJSONObject("properties");
      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String entryKey = entry.getKey();
            Object entryValue = entry.getValue();
            JSONSchema schema;
            if (entryValue instanceof Boolean) {
               schema = (JSONSchema)((Boolean)entryValue ? Any.INSTANCE : Any.NOT_ANY);
            } else if (entryValue instanceof JSONSchema) {
               schema = (JSONSchema)entryValue;
            } else {
               schema = JSONSchema.of((JSONObject)entryValue, (JSONSchema)(root == null ? this : root));
            }

            this.properties.put(entryKey, schema);
            if (schema instanceof UnresolvedReference) {
               String refName = ((UnresolvedReference)schema).refName;
               UnresolvedReference.PropertyResolveTask task = new UnresolvedReference.PropertyResolveTask(this.properties, entryKey, refName);
               JSONSchema resolveRoot = (JSONSchema)(root == null ? this : root);
               resolveRoot.addResolveTask(task);
            }
         }
      }

      JSONObject patternProperties = input.getJSONObject("patternProperties");
      if (patternProperties != null) {
         this.patternProperties = new ObjectSchema.PatternProperty[patternProperties.size()];
         int index = 0;

         for (Entry<String, Object> entry : patternProperties.entrySet()) {
            String entryKeyx = entry.getKey();
            Object entryValuex = entry.getValue();
            JSONSchema schemax;
            if (entryValuex instanceof Boolean) {
               schemax = (JSONSchema)((Boolean)entryValuex ? Any.INSTANCE : Any.NOT_ANY);
            } else {
               schemax = JSONSchema.of((JSONObject)entryValuex, (JSONSchema)(root == null ? this : root));
            }

            this.patternProperties[index++] = new ObjectSchema.PatternProperty(Pattern.compile(entryKeyx), schemax);
         }
      } else {
         this.patternProperties = new ObjectSchema.PatternProperty[0];
      }

      JSONArray required = input.getJSONArray("required");
      if (required != null && !required.isEmpty()) {
         this.required = new LinkedHashSet<>(required.size());

         for (int i = 0; i < required.size(); i++) {
            this.required.add(required.getString(i));
         }

         this.requiredHashCode = new long[this.required.size()];
         int i = 0;

         for (String item : this.required) {
            this.requiredHashCode[i++] = Fnv.hashCode64(item);
         }
      } else {
         this.required = Collections.emptySet();
         this.requiredHashCode = new long[0];
      }

      Object additionalProperties = input.get("additionalProperties");
      if (additionalProperties instanceof Boolean) {
         this.additionalPropertySchema = null;
         this.additionalProperties = (Boolean)additionalProperties;
      } else if (additionalProperties instanceof JSONObject) {
         this.additionalPropertySchema = JSONSchema.of((JSONObject)additionalProperties, root);
         this.additionalProperties = false;
      } else {
         this.additionalPropertySchema = null;
         this.additionalProperties = true;
      }

      Object propertyNames = input.get("propertyNames");
      if (propertyNames == null) {
         this.propertyNames = null;
      } else if (propertyNames instanceof Boolean) {
         this.propertyNames = (JSONSchema)((Boolean)propertyNames ? Any.INSTANCE : Any.NOT_ANY);
      } else {
         this.propertyNames = new StringSchema((JSONObject)propertyNames);
      }

      this.minProperties = input.getIntValue("minProperties", -1);
      this.maxProperties = input.getIntValue("maxProperties", -1);
      JSONObject dependentRequired = input.getJSONObject("dependentRequired");
      if (dependentRequired != null && !dependentRequired.isEmpty()) {
         this.dependentRequired = new LinkedHashMap<>(dependentRequired.size(), 1.0F);
         this.dependentRequiredHashCodes = new LinkedHashMap<>(dependentRequired.size(), 1.0F);

         for (String key : dependentRequired.keySet()) {
            String[] dependentRequiredProperties = dependentRequired.getObject(key, String[].class);
            long[] dependentRequiredPropertiesHash = new long[dependentRequiredProperties.length];

            for (int i = 0; i < dependentRequiredProperties.length; i++) {
               dependentRequiredPropertiesHash[i] = Fnv.hashCode64(dependentRequiredProperties[i]);
            }

            this.dependentRequired.put(key, dependentRequiredProperties);
            this.dependentRequiredHashCodes.put(Fnv.hashCode64(key), dependentRequiredPropertiesHash);
         }
      } else {
         this.dependentRequired = null;
         this.dependentRequiredHashCodes = null;
      }

      JSONObject dependentSchemas = input.getJSONObject("dependentSchemas");
      if (dependentSchemas != null && !dependentSchemas.isEmpty()) {
         this.dependentSchemas = new LinkedHashMap<>(dependentSchemas.size(), 1.0F);
         this.dependentSchemasHashMapping = new LinkedHashMap<>(dependentSchemas.size(), 1.0F);

         for (String key : dependentSchemas.keySet()) {
            JSONSchema dependentSchema = dependentSchemas.getObject(key, JSONSchema::of);
            this.dependentSchemas.put(key, dependentSchema);
            this.dependentSchemasHashMapping.put(Fnv.hashCode64(key), dependentSchema);
         }
      } else {
         this.dependentSchemas = null;
         this.dependentSchemasHashMapping = null;
      }

      this.ifSchema = input.getObject("if", JSONSchema::of);
      this.elseSchema = input.getObject("else", JSONSchema::of);
      this.thenSchema = input.getObject("then", JSONSchema::of);
      this.allOf = allOf(input, null);
      this.anyOf = anyOf(input, null);
      this.oneOf = oneOf(input, null);
   }

   @Override
   void addResolveTask(UnresolvedReference.ResolveTask task) {
      if (this.resolveTasks == null) {
         this.resolveTasks = new ArrayList<>();
      }

      this.resolveTasks.add(task);
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Object;
   }

   public ValidateResult validate(Map map) {
      for (String item : this.required) {
         if (!map.containsKey(item)) {
            return new ValidateResult(false, "required %s", item);
         }
      }

      for (Entry<String, JSONSchema> entry : this.properties.entrySet()) {
         String key = entry.getKey();
         JSONSchema schema = entry.getValue();
         Object propertyValue = map.get(key);
         if (propertyValue != null || map.containsKey(key)) {
            ValidateResult result = schema.validate(propertyValue);
            if (!result.isSuccess()) {
               return new ValidateResult(result, "property %s invalid", key);
            }
         }
      }

      for (ObjectSchema.PatternProperty patternProperty : this.patternProperties) {
         for (Entry entryx : map.entrySet()) {
            Object entryKey = entryx.getKey();
            if (entryKey instanceof String) {
               String strKey = (String)entryKey;
               if (patternProperty.pattern.matcher(strKey).find()) {
                  ValidateResult result = patternProperty.schema.validate(entryx.getValue());
                  if (!result.isSuccess()) {
                     return result;
                  }
               }
            }
         }
      }

      if (!this.additionalProperties) {
         label173:
         for (Entry entryxx : map.entrySet()) {
            Object key = entryxx.getKey();
            if (!this.properties.containsKey(key)) {
               for (ObjectSchema.PatternProperty patternProperty : this.patternProperties) {
                  if (key instanceof String) {
                     String strKey = (String)key;
                     if (patternProperty.pattern.matcher(strKey).find()) {
                        continue label173;
                     }
                  }
               }

               if (this.additionalPropertySchema == null) {
                  return new ValidateResult(false, "add additionalProperties %s", key);
               }

               ValidateResult result = this.additionalPropertySchema.validate(entryxx.getValue());
               if (!result.isSuccess()) {
                  return result;
               }
            }
         }
      }

      if (this.propertyNames != null) {
         for (Object key : map.keySet()) {
            ValidateResult result = this.propertyNames.validate(key);
            if (!result.isSuccess()) {
               return FAIL_PROPERTY_NAME;
            }
         }
      }

      if (this.minProperties >= 0 && map.size() < this.minProperties) {
         return new ValidateResult(false, "minProperties not match, expect %s, but %s", this.minProperties, map.size());
      } else if (this.maxProperties >= 0 && map.size() > this.maxProperties) {
         return new ValidateResult(false, "maxProperties not match, expect %s, but %s", this.maxProperties, map.size());
      } else {
         if (this.dependentRequired != null) {
            for (Entry<String, String[]> entryxxx : this.dependentRequired.entrySet()) {
               String keyx = entryxxx.getKey();
               Object value = map.get(keyx);
               if (value != null) {
                  String[] dependentRequiredProperties = entryxxx.getValue();

                  for (String dependentRequiredProperty : dependentRequiredProperties) {
                     if (!map.containsKey(dependentRequiredProperty)) {
                        return new ValidateResult(false, "property %s, dependentRequired property %s", keyx, dependentRequiredProperty);
                     }
                  }
               }
            }
         }

         if (this.dependentSchemas != null) {
            for (Entry<String, JSONSchema> entryxxxx : this.dependentSchemas.entrySet()) {
               String keyx = entryxxxx.getKey();
               Object fieldValue = map.get(keyx);
               if (fieldValue != null) {
                  JSONSchema schema = entryxxxx.getValue();
                  ValidateResult result = schema.validate(map);
                  if (!result.isSuccess()) {
                     return result;
                  }
               }
            }
         }

         if (this.ifSchema != null) {
            ValidateResult ifResult = this.ifSchema.validate(map);
            if (ifResult == SUCCESS) {
               if (this.thenSchema != null) {
                  ValidateResult thenResult = this.thenSchema.validate(map);
                  if (!thenResult.isSuccess()) {
                     return thenResult;
                  }
               }
            } else if (this.elseSchema != null) {
               ValidateResult elseResult = this.elseSchema.validate(map);
               if (!elseResult.isSuccess()) {
                  return elseResult;
               }
            }
         }

         if (this.allOf != null) {
            ValidateResult result = this.allOf.validate(map);
            if (!result.isSuccess()) {
               return result;
            }
         }

         if (this.anyOf != null) {
            ValidateResult result = this.anyOf.validate(map);
            if (!result.isSuccess()) {
               return result;
            }
         }

         if (this.oneOf != null) {
            ValidateResult result = this.oneOf.validate(map);
            if (!result.isSuccess()) {
               return result;
            }
         }

         return SUCCESS;
      }
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value == null) {
         return this.typed ? FAIL_INPUT_NULL : SUCCESS;
      } else {
         if (this.encoded) {
            if (!(value instanceof String)) {
               return FAIL_INPUT_NOT_ENCODED;
            }

            try {
               value = JSON.parseObject((String)value);
            } catch (JSONException var20) {
               return FAIL_INPUT_NOT_ENCODED;
            }
         }

         if (value instanceof Map) {
            return this.validate((Map)value);
         } else {
            Class valueClass = value.getClass();
            ObjectWriter objectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(valueClass);
            if (!(objectWriter instanceof ObjectWriterAdapter)) {
               return this.typed ? new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.Object, valueClass) : SUCCESS;
            } else {
               for (int i = 0; i < this.requiredHashCode.length; i++) {
                  long nameHash = this.requiredHashCode[i];
                  FieldWriter fieldWriter = objectWriter.getFieldWriter(nameHash);
                  Object fieldValue = null;
                  if (fieldWriter != null) {
                     fieldValue = fieldWriter.getFieldValue(value);
                  }

                  if (fieldValue == null) {
                     String fieldName = null;
                     int j = 0;

                     for (String itemName : this.required) {
                        if (j == i) {
                           fieldName = itemName;
                        }

                        j++;
                     }

                     return new ValidateResult(false, "required property %s", fieldName);
                  }
               }

               for (Entry<String, JSONSchema> entry : this.properties.entrySet()) {
                  String key = entry.getKey();
                  long keyHash = Fnv.hashCode64(key);
                  JSONSchema schema = entry.getValue();
                  FieldWriter fieldWriterx = objectWriter.getFieldWriter(keyHash);
                  if (fieldWriterx != null) {
                     Object propertyValue = fieldWriterx.getFieldValue(value);
                     if (propertyValue != null) {
                        ValidateResult result = schema.validate(propertyValue);
                        if (!result.isSuccess()) {
                           return result;
                        }
                     }
                  }
               }

               if (this.minProperties >= 0 || this.maxProperties >= 0) {
                  int fieldValueCount = 0;

                  for (FieldWriter fieldWriterx : objectWriter.getFieldWriters()) {
                     Object fieldValuex = fieldWriterx.getFieldValue(value);
                     if (fieldValuex != null) {
                        fieldValueCount++;
                     }
                  }

                  if (this.minProperties >= 0 && fieldValueCount < this.minProperties) {
                     return new ValidateResult(false, "minProperties not match, expect %s, but %s", this.minProperties, fieldValueCount);
                  }

                  if (this.maxProperties >= 0 && fieldValueCount > this.maxProperties) {
                     return new ValidateResult(false, "maxProperties not match, expect %s, but %s", this.maxProperties, fieldValueCount);
                  }
               }

               if (this.dependentRequiredHashCodes != null) {
                  int propertyIndex = 0;

                  for (Entry<Long, long[]> entryx : this.dependentRequiredHashCodes.entrySet()) {
                     Long keyHash = entryx.getKey();
                     long[] dependentRequiredProperties = entryx.getValue();
                     FieldWriter fieldWriterxx = objectWriter.getFieldWriter(keyHash);
                     Object fieldValuex = fieldWriterxx.getFieldValue(value);
                     if (fieldValuex == null) {
                        propertyIndex++;
                     } else {
                        for (int requiredIndex = 0; requiredIndex < dependentRequiredProperties.length; requiredIndex++) {
                           long dependentRequiredHash = dependentRequiredProperties[requiredIndex];
                           FieldWriter dependentFieldWriter = objectWriter.getFieldWriter(dependentRequiredHash);
                           if (dependentFieldWriter == null || dependentFieldWriter.getFieldValue(value) == null) {
                              int i = 0;
                              String property = null;
                              String dependentRequiredProperty = null;

                              for (Entry<String, String[]> dependentRequiredEntry : this.dependentRequired.entrySet()) {
                                 if (propertyIndex == i) {
                                    property = dependentRequiredEntry.getKey();
                                    dependentRequiredProperty = dependentRequiredEntry.getValue()[requiredIndex];
                                 }

                                 i++;
                              }

                              return new ValidateResult(false, "property %s, dependentRequired property %s", property, dependentRequiredProperty);
                           }
                        }

                        propertyIndex++;
                     }
                  }
               }

               if (this.dependentSchemasHashMapping != null) {
                  for (Entry<Long, JSONSchema> entryxx : this.dependentSchemasHashMapping.entrySet()) {
                     Long keyHash = entryxx.getKey();
                     FieldWriter fieldWriterxx = objectWriter.getFieldWriter(keyHash);
                     if (fieldWriterxx != null && fieldWriterxx.getFieldValue(value) != null) {
                        JSONSchema schema = entryxx.getValue();
                        ValidateResult result = schema.validate(value);
                        if (!result.isSuccess()) {
                           return result;
                        }
                     }
                  }
               }

               if (this.ifSchema != null) {
                  ValidateResult ifResult = this.ifSchema.validate(value);
                  if (ifResult.isSuccess()) {
                     if (this.thenSchema != null) {
                        ValidateResult thenResult = this.thenSchema.validate(value);
                        if (!thenResult.isSuccess()) {
                           return thenResult;
                        }
                     }
                  } else if (this.elseSchema != null) {
                     ValidateResult elseResult = this.elseSchema.validate(value);
                     if (!elseResult.isSuccess()) {
                        return elseResult;
                     }
                  }
               }

               if (this.allOf != null) {
                  ValidateResult result = this.allOf.validate(value);
                  if (!result.isSuccess()) {
                     return result;
                  }
               }

               if (this.anyOf != null) {
                  ValidateResult result = this.anyOf.validate(value);
                  if (!result.isSuccess()) {
                     return result;
                  }
               }

               if (this.oneOf != null) {
                  ValidateResult result = this.oneOf.validate(value);
                  if (!result.isSuccess()) {
                     return result;
                  }
               }

               return SUCCESS;
            }
         }
      }
   }

   public Map<String, JSONSchema> getProperties() {
      return this.properties;
   }

   public JSONSchema getProperty(String key) {
      return this.properties.get(key);
   }

   public Set<String> getRequired() {
      return this.required;
   }

   @JSONField(true)
   @Override
   public JSONObject toJSONObject() {
      JSONObject object = new JSONObject();
      object.put("type", "object");
      if (this.title != null) {
         object.put("title", this.title);
      }

      if (this.description != null) {
         object.put("description", this.description);
      }

      if (!this.definitions.isEmpty()) {
         object.put("definitions", this.definitions);
      }

      if (!this.defs.isEmpty()) {
         object.put("defs", this.defs);
      }

      if (!this.properties.isEmpty()) {
         object.put("properties", this.properties);
      }

      if (!this.required.isEmpty()) {
         object.put("required", this.required);
      }

      if (!this.additionalProperties) {
         if (this.additionalPropertySchema != null) {
            object.put("additionalProperties", this.additionalPropertySchema);
         } else {
            object.put("additionalProperties", Boolean.valueOf(this.additionalProperties));
         }
      }

      if (this.patternProperties != null && this.patternProperties.length != 0) {
         object.put("patternProperties", this.patternProperties);
      }

      if (this.propertyNames != null) {
         object.put("propertyNames", this.propertyNames);
      }

      if (this.minProperties != -1) {
         object.put("minProperties", Integer.valueOf(this.minProperties));
      }

      if (this.maxProperties != -1) {
         object.put("maxProperties", Integer.valueOf(this.maxProperties));
      }

      if (this.dependentRequired != null && !this.dependentRequired.isEmpty()) {
         object.put("dependentRequired", this.dependentRequired);
      }

      if (this.dependentSchemas != null && !this.dependentSchemas.isEmpty()) {
         object.put("dependentSchemas", this.dependentSchemas);
      }

      if (this.ifSchema != null) {
         object.put("if", this.ifSchema);
      }

      if (this.thenSchema != null) {
         object.put("then", this.thenSchema);
      }

      if (this.elseSchema != null) {
         object.put("else", this.elseSchema);
      }

      if (this.allOf != null) {
         object.put("allOf", this.allOf);
      }

      if (this.anyOf != null) {
         object.put("anyOf", this.anyOf);
      }

      if (this.oneOf != null) {
         object.put("oneOf", this.oneOf);
      }

      return object;
   }

   @Override
   public void accept(Predicate<JSONSchema> v) {
      if (v.test(this)) {
         this.properties.values().forEach(v::test);
      }
   }

   public JSONSchema getDefs(String def) {
      return this.defs.get(def);
   }

   static final class PatternProperty {
      final Pattern pattern;
      final JSONSchema schema;

      public PatternProperty(Pattern pattern, JSONSchema schema) {
         this.pattern = pattern;
         this.schema = schema;
      }
   }
}
