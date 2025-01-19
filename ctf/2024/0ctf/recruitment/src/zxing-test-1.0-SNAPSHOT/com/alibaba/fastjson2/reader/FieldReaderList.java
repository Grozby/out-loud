package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FieldReaderList<T, V> extends FieldReaderObject<T> {
   final long fieldClassHash;
   final long itemClassHash;

   public FieldReaderList(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      Type itemType,
      Class itemClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Method method,
      Field field,
      BiConsumer function
   ) {
      super(fieldName, fieldType, fieldClass, ordinal, features, format, locale, defaultValue, schema, method, field, function);
      this.itemType = itemType;
      this.itemClass = itemClass;
      this.itemClassHash = this.itemClass == null ? 0L : Fnv.hashCode64(itemClass.getName());
      this.fieldClassHash = fieldClass == null ? 0L : Fnv.hashCode64(TypeUtils.getTypeName(fieldClass));
      if (format != null && itemType == Date.class) {
         this.itemReader = new ObjectReaderImplDate(format, locale);
      }
   }

   @Override
   public long getItemClassHash() {
      return this.itemClassHash;
   }

   public Collection<V> createList(JSONReader.Context context) {
      return (Collection<V>)(this.fieldClass != List.class && this.fieldClass != Collection.class && this.fieldClass != ArrayList.class
         ? (Collection)this.getObjectReader(context).createInstance(this.features)
         : new ArrayList<>());
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      if (jsonReader.jsonb) {
         this.readFieldValueJSONB(jsonReader, object);
      } else if (jsonReader.nextIfNull()) {
         this.accept(object, null);
      } else if (jsonReader.isReference()) {
         String reference = jsonReader.readReference();
         if ("..".equals(reference)) {
            this.accept(object, object);
         } else {
            this.addResolveTask(jsonReader, object, reference);
         }
      } else {
         JSONReader.Context context = jsonReader.getContext();
         ObjectReader objectReader = this.getObjectReader(context);
         Function builder = null;
         if (this.initReader != null) {
            builder = this.initReader.getBuildFunction();
         } else if (objectReader instanceof ObjectReaderImplList) {
            builder = objectReader.getBuildFunction();
         }

         char current = jsonReader.current();
         if (current == '[') {
            ObjectReader itemObjectReader = this.getItemObjectReader(context);
            Collection list = this.createList(context);
            jsonReader.next();

            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               if (!jsonReader.readReference(list, i)) {
                  list.add(itemObjectReader.readObject(jsonReader, null, null, 0L));
                  jsonReader.nextIfComma();
               }
            }

            if (builder != null) {
               list = (Collection)builder.apply(list);
            }

            this.accept(object, list);
            jsonReader.nextIfComma();
         } else if (current == '{' && this.getItemObjectReader(context) instanceof ObjectReaderBean) {
            Object itemValue = jsonReader.jsonb
               ? this.itemReader.readJSONBObject(jsonReader, null, null, this.features)
               : this.itemReader.readObject(jsonReader, null, null, this.features);
            Collection list = (Collection)objectReader.createInstance(this.features);
            list.add(itemValue);
            if (builder != null) {
               list = (Collection)builder.apply(list);
            }

            this.accept(object, list);
            jsonReader.nextIfComma();
         } else {
            Object value = jsonReader.jsonb
               ? objectReader.readJSONBObject(jsonReader, null, null, this.features)
               : objectReader.readObject(jsonReader, null, null, this.features);
            this.accept(object, value);
         }
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      if (jsonReader.jsonb) {
         int entryCnt = jsonReader.startArray();
         if (entryCnt == -1) {
            return null;
         } else {
            Object[] array = new Object[entryCnt];
            ObjectReader itemObjectReader = this.getItemObjectReader(jsonReader.getContext());

            for (int i = 0; i < entryCnt; i++) {
               array[i] = itemObjectReader.readObject(jsonReader, null, null, 0L);
            }

            return Arrays.asList(array);
         }
      } else if (jsonReader.current() == '[') {
         JSONReader.Context ctx = jsonReader.getContext();
         ObjectReader itemObjectReader = this.getItemObjectReader(ctx);
         Collection list = this.createList(ctx);
         jsonReader.next();

         while (!jsonReader.nextIfArrayEnd()) {
            list.add(itemObjectReader.readObject(jsonReader, null, null, 0L));
            jsonReader.nextIfComma();
         }

         jsonReader.nextIfComma();
         return list;
      } else {
         if (jsonReader.isString()) {
            String str = jsonReader.readString();
            if (this.itemType instanceof Class && Number.class.isAssignableFrom((Class<?>)this.itemType)) {
               Function typeConvert = jsonReader.getContext().getProvider().getTypeConvert(String.class, this.itemType);
               if (typeConvert != null) {
                  Collection list = this.createList(jsonReader.getContext());
                  if (str.indexOf(44) != -1) {
                     String[] items = str.split(",");

                     for (String item : items) {
                        Object converted = typeConvert.apply(item);
                        list.add(converted);
                     }
                  }

                  return list;
               }
            }
         }

         throw new JSONException(jsonReader.info("TODO : " + this.getClass()));
      }
   }

   @Override
   public ObjectReader checkObjectAutoType(JSONReader jsonReader) {
      if (jsonReader.nextIfMatch((byte)-110)) {
         long typeHash = jsonReader.readTypeHashCode();
         long features = jsonReader.features(this.features);
         JSONReader.Context context = jsonReader.getContext();
         JSONReader.AutoTypeBeforeHandler autoTypeFilter = context.getContextAutoTypeBeforeHandler();
         if (autoTypeFilter != null) {
            Class<?> filterClass = autoTypeFilter.apply(typeHash, this.fieldClass, features);
            if (filterClass == null) {
               String typeName = jsonReader.getString();
               filterClass = autoTypeFilter.apply(typeName, this.fieldClass, features);
            }

            if (filterClass != null) {
               return context.getObjectReader(this.fieldClass);
            }
         }

         boolean isSupportAutoType = jsonReader.isSupportAutoType(features);
         if (!isSupportAutoType) {
            if (jsonReader.isArray() && !jsonReader.isEnabled(JSONReader.Feature.ErrorOnNotSupportAutoType)) {
               return this.getObjectReader(jsonReader);
            } else {
               throw new JSONException(jsonReader.info("autoType not support input " + jsonReader.getString()));
            }
         } else {
            ObjectReader autoTypeObjectReader = jsonReader.getObjectReaderAutoType(typeHash, this.fieldClass, features);
            if (autoTypeObjectReader instanceof ObjectReaderImplList) {
               ObjectReaderImplList listReader = (ObjectReaderImplList)autoTypeObjectReader;
               autoTypeObjectReader = new ObjectReaderImplList(this.fieldType, this.fieldClass, listReader.instanceType, this.itemType, listReader.builder);
            }

            if (autoTypeObjectReader == null) {
               throw new JSONException(jsonReader.info("autoType not support : " + jsonReader.getString()));
            } else {
               return autoTypeObjectReader;
            }
         }
      } else {
         return null;
      }
   }
}
