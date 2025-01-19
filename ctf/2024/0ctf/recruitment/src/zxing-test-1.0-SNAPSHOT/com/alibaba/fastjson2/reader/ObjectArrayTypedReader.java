package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

final class ObjectArrayTypedReader extends ObjectReaderPrimitive {
   final Class componentType;
   final Class componentClass;
   final long componentClassHash;
   final String typeName;
   final long typeNameHashCode;

   ObjectArrayTypedReader(Class objectClass) {
      super(objectClass);
      this.componentType = objectClass.getComponentType();
      String componentTypeName = TypeUtils.getTypeName(this.componentType);
      this.componentClassHash = Fnv.hashCode64(componentTypeName);
      this.typeName = '[' + componentTypeName;
      this.typeNameHashCode = Fnv.hashCode64(this.typeName);
      this.componentClass = TypeUtils.getClass(this.componentType);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         Object[] values = (Object[])Array.newInstance(this.componentType, 16);
         int size = 0;

         while (!jsonReader.nextIfArrayEnd()) {
            int minCapacity = size + 1;
            if (minCapacity - values.length > 0) {
               int oldCapacity = values.length;
               int newCapacity = oldCapacity + (oldCapacity >> 1);
               if (newCapacity - minCapacity < 0) {
                  newCapacity = minCapacity;
               }

               values = Arrays.copyOf(values, newCapacity);
            }

            Object value = jsonReader.read(this.componentType);
            values[size++] = value;
            jsonReader.nextIfComma();
         }

         jsonReader.nextIfMatch(',');
         return Arrays.copyOf(values, size);
      } else {
         if (jsonReader.current() == '{') {
            jsonReader.next();
            long filedHash = jsonReader.readFieldNameHashCode();
            if (filedHash == HASH_TYPE) {
               jsonReader.readString();
            }
         }

         if (jsonReader.isString()) {
            String str = jsonReader.readString();
            if (str == null || str.isEmpty()) {
               return null;
            }

            if ("@value".equals(str)) {
               jsonReader.next();
               Object result = this.readObject(jsonReader, fieldType, fieldName, features);
               jsonReader.nextIfObjectEnd();
               return result;
            }
         }

         throw new JSONException(jsonReader.info("TODO"));
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.getType() == -110) {
         jsonReader.next();
         long typeHash = jsonReader.readTypeHashCode();
         if (typeHash != ObjectArrayReader.TYPE_HASH_CODE && typeHash != this.typeNameHashCode) {
            if (jsonReader.isSupportAutoType(features)) {
               ObjectReader autoTypeObjectReader = jsonReader.getObjectReaderAutoType(typeHash, this.objectClass, features);
               if (autoTypeObjectReader == null) {
                  throw new JSONException(jsonReader.info("autoType not support : " + jsonReader.getString()));
               }

               return autoTypeObjectReader.readObject(jsonReader, fieldType, fieldName, features);
            }

            throw new JSONException(jsonReader.info("not support autotype : " + jsonReader.getString()));
         }
      }

      int entryCnt = jsonReader.startArray();
      if (entryCnt == -1) {
         return null;
      } else {
         Object[] values = (Object[])Array.newInstance(this.componentClass, entryCnt);

         for (int i = 0; i < entryCnt; i++) {
            Object value;
            if (jsonReader.isReference()) {
               String reference = jsonReader.readReference();
               if ("..".equals(reference)) {
                  value = values;
               } else {
                  value = null;
                  jsonReader.addResolveTask(values, i, JSONPath.of(reference));
               }
            } else {
               ObjectReader autoTypeReader = jsonReader.checkAutoType(this.componentClass, this.componentClassHash, features);
               if (autoTypeReader != null) {
                  value = autoTypeReader.readJSONBObject(jsonReader, null, null, features);
               } else {
                  value = jsonReader.read(this.componentType);
               }
            }

            values[i] = value;
         }

         return values;
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      Object[] values = (Object[])Array.newInstance(this.componentClass, collection.size());
      int index = 0;

      for (Object item : collection) {
         if (item != null) {
            Class<?> valueClass = item.getClass();
            if (valueClass != this.componentType) {
               ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
               Function typeConvert = provider.getTypeConvert(valueClass, this.componentType);
               if (typeConvert != null) {
                  item = typeConvert.apply(item);
               }
            }
         }

         if (!this.componentType.isInstance(item)) {
            ObjectReader objectReader = JSONFactory.getDefaultObjectReaderProvider().getObjectReader(this.componentType);
            if (item instanceof Map) {
               item = objectReader.createInstance((Map)item);
            } else if (item instanceof Collection) {
               item = objectReader.createInstance((Collection)item, features);
            } else if (item instanceof Object[]) {
               item = objectReader.createInstance(JSONArray.of((Object[])item), features);
            } else if (item != null) {
               Class<?> itemClass = item.getClass();
               if (!itemClass.isArray()) {
                  throw new JSONException("component type not match, expect " + this.componentType.getName() + ", but " + itemClass);
               }

               int length = Array.getLength(item);
               JSONArray array = new JSONArray(length);

               for (int i = 0; i < length; i++) {
                  array.add(Array.get(item, i));
               }

               item = objectReader.createInstance(array, features);
            }
         }

         values[index++] = item;
      }

      return values;
   }
}
