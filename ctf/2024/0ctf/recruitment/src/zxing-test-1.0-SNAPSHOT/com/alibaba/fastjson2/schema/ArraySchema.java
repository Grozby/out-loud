package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class ArraySchema extends JSONSchema {
   final Map<String, JSONSchema> definitions;
   final Map<String, JSONSchema> defs;
   final boolean typed;
   final int maxLength;
   final int minLength;
   JSONSchema itemSchema;
   final JSONSchema[] prefixItems;
   final boolean additionalItems;
   final JSONSchema additionalItem;
   final JSONSchema contains;
   final int minContains;
   final int maxContains;
   final boolean uniqueItems;
   final AllOf allOf;
   final AnyOf anyOf;
   final OneOf oneOf;
   final boolean encoded;

   public ArraySchema(JSONObject input, JSONSchema root) {
      super(input);
      this.typed = "array".equals(input.get("type"));
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
      }

      this.minLength = input.getIntValue("minItems", -1);
      this.maxLength = input.getIntValue("maxItems", -1);
      Object items = input.get("items");
      Object additionalItems = input.get("additionalItems");
      JSONArray prefixItems = input.getJSONArray("prefixItems");
      boolean additionalItemsSupport;
      if (items == null) {
         additionalItemsSupport = true;
         this.itemSchema = null;
      } else if (items instanceof Boolean) {
         additionalItemsSupport = (Boolean)items;
         this.itemSchema = null;
      } else if (items instanceof JSONArray) {
         if (prefixItems != null) {
            throw new JSONException("schema error, items : " + items);
         }

         prefixItems = (JSONArray)items;
         this.itemSchema = null;
         additionalItemsSupport = true;
      } else {
         additionalItemsSupport = true;
         this.itemSchema = JSONSchema.of((JSONObject)items, (JSONSchema)(root != null ? root : this));
      }

      if (additionalItems instanceof JSONObject) {
         this.additionalItem = JSONSchema.of((JSONObject)additionalItems, (JSONSchema)(root == null ? this : root));
         additionalItemsSupport = true;
      } else if (additionalItems instanceof Boolean) {
         additionalItemsSupport = (Boolean)additionalItems;
         this.additionalItem = null;
      } else {
         this.additionalItem = null;
      }

      if (this.itemSchema != null && !(this.itemSchema instanceof Any)) {
         additionalItemsSupport = true;
      } else if (prefixItems == null && !(items instanceof Boolean)) {
         additionalItemsSupport = true;
      }

      this.additionalItems = additionalItemsSupport;
      if (prefixItems == null) {
         this.prefixItems = new JSONSchema[0];
      } else {
         this.prefixItems = new JSONSchema[prefixItems.size()];

         for (int i = 0; i < prefixItems.size(); i++) {
            Object prefixItem = prefixItems.get(i);
            JSONSchema schema;
            if (prefixItem instanceof Boolean) {
               schema = (JSONSchema)((Boolean)prefixItem ? Any.INSTANCE : Any.NOT_ANY);
            } else {
               JSONObject jsonObject = (JSONObject)prefixItem;
               schema = JSONSchema.of(jsonObject, (JSONSchema)(root == null ? this : root));
            }

            this.prefixItems[i] = schema;
         }
      }

      this.contains = input.getObject("contains", JSONSchema::of);
      this.minContains = input.getIntValue("minContains", -1);
      this.maxContains = input.getIntValue("maxContains", -1);
      this.uniqueItems = input.getBooleanValue("uniqueItems");
      this.allOf = allOf(input, null);
      this.anyOf = anyOf(input, null);
      this.oneOf = oneOf(input, null);
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.Array;
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
               value = JSON.parseArray((String)value);
            } catch (JSONException var4) {
               return FAIL_INPUT_NOT_ENCODED;
            }
         }

         if (value instanceof Object[]) {
            Object[] items = (Object[])value;
            return this.validateItems(value, items.length, i -> items[i]);
         } else if (value.getClass().isArray()) {
            int size = Array.getLength(value);
            Object finalValue = value;
            return this.validateItems(finalValue, size, i -> Array.get(finalValue, i));
         } else if (value instanceof Collection) {
            Collection<?> items = (Collection<?>)value;
            Iterator<?> iterator = items.iterator();
            return this.validateItems(value, items.size(), i -> iterator.next());
         } else {
            return this.typed ? FAIL_TYPE_NOT_MATCH : SUCCESS;
         }
      }
   }

   private ValidateResult validateItems(Object value, int size, IntFunction<Object> itemGetter) {
      if (this.minLength >= 0 && size < this.minLength) {
         return new ValidateResult(false, "minLength not match, expect >= %s, but %s", this.minLength, size);
      } else if (this.maxLength >= 0 && size > this.maxLength) {
         return new ValidateResult(false, "maxLength not match, expect <= %s, but %s", this.maxLength, size);
      } else if (!this.additionalItems && size > this.prefixItems.length) {
         return new ValidateResult(false, "additional items not match, max size %s, but %s", this.prefixItems.length, size);
      } else {
         boolean isCollection = value instanceof Collection;
         Set<Object> uniqueItemsSet = null;
         int containsCount = 0;

         for (int index = 0; index < size; index++) {
            Object item = itemGetter.apply(index);
            boolean prefixMatch = false;
            if (index < this.prefixItems.length) {
               ValidateResult result = this.prefixItems[index].validate(item);
               if (!result.isSuccess()) {
                  return result;
               }

               prefixMatch = true;
            } else if (isCollection && this.itemSchema == null && this.additionalItem != null) {
               ValidateResult result = this.additionalItem.validate(item);
               if (!result.isSuccess()) {
                  return result;
               }
            }

            if (!prefixMatch && this.itemSchema != null) {
               ValidateResult result = this.itemSchema.validate(item);
               if (!result.isSuccess()) {
                  return result;
               }
            }

            if (this.contains != null && (this.minContains > 0 || this.maxContains > 0 || containsCount == 0)) {
               ValidateResult result = this.contains.validate(item);
               if (result == SUCCESS) {
                  containsCount++;
               }
            }

            if (this.uniqueItems) {
               if (uniqueItemsSet == null) {
                  uniqueItemsSet = new HashSet<>(size, 1.0F);
               }

               if (item instanceof BigDecimal) {
                  item = ((BigDecimal)item).stripTrailingZeros();
               }

               if (!uniqueItemsSet.add(item)) {
                  return UNIQUE_ITEMS_NOT_MATCH;
               }
            }
         }

         if (!isCollection || this.contains != null) {
            if (this.minContains >= 0 && containsCount < this.minContains) {
               return new ValidateResult(false, "minContains not match, expect %s, but %s", this.minContains, containsCount);
            }

            if (isCollection) {
               if (containsCount == 0 && this.minContains != 0) {
                  return CONTAINS_NOT_MATCH;
               }
            } else if (this.contains != null && containsCount == 0) {
               return CONTAINS_NOT_MATCH;
            }

            if (this.maxContains >= 0 && containsCount > this.maxContains) {
               return new ValidateResult(false, "maxContains not match, expect %s, but %s", this.maxContains, containsCount);
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

   @Override
   public JSONObject toJSONObject() {
      JSONObject object = new JSONObject();
      object.put("type", "array");
      if (this.maxLength != -1) {
         object.put("maxLength", Integer.valueOf(this.maxLength));
      }

      if (this.minLength != -1) {
         object.put("minLength", Integer.valueOf(this.minLength));
      }

      if (this.itemSchema != null) {
         object.put("items", this.itemSchema);
      }

      if (this.prefixItems != null && this.prefixItems.length != 0) {
         object.put("prefixItems", this.prefixItems);
      }

      if (!this.additionalItems) {
         object.put("additionalItems", Boolean.valueOf(this.additionalItems));
      }

      if (this.additionalItem != null) {
         object.put("additionalItem", this.additionalItem);
      }

      if (this.contains != null) {
         object.put("contains", this.contains);
      }

      if (this.minContains != -1) {
         object.put("minContains", Integer.valueOf(this.minContains));
      }

      if (this.maxContains != -1) {
         object.put("maxContains", Integer.valueOf(this.maxContains));
      }

      if (this.uniqueItems) {
         object.put("uniqueItems", Boolean.valueOf(this.uniqueItems));
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
      if (v.test(this) && this.itemSchema != null) {
         this.itemSchema.accept(v);
      }
   }

   public JSONSchema getItemSchema() {
      return this.itemSchema;
   }
}
