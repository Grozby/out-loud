package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.GuavaSupport;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

public final class ObjectReaderImplListStr implements ObjectReader {
   final Class listType;
   final Class instanceType;
   Object listSingleton;

   public ObjectReaderImplListStr(Class listType, Class instanceType) {
      this.listType = listType;
      this.instanceType = instanceType;
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType == ArrayList.class) {
         return new ArrayList();
      } else if (this.instanceType == LinkedList.class) {
         return new LinkedList();
      } else {
         try {
            return this.instanceType.newInstance();
         } catch (IllegalAccessException | InstantiationException var4) {
            throw new JSONException("create list error, type " + this.instanceType);
         }
      }
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      if (this.listType.isInstance(collection)) {
         boolean typeMatch = true;

         for (Object item : collection) {
            if (!(item instanceof String)) {
               typeMatch = false;
               break;
            }
         }

         if (typeMatch) {
            return collection;
         }
      }

      Collection typedList = (Collection)this.createInstance(0L);

      for (Object itemx : collection) {
         if (itemx != null && !(itemx instanceof String)) {
            typedList.add(JSON.toJSONString(itemx));
         } else {
            typedList.add(itemx);
         }
      }

      return typedList;
   }

   @Override
   public Class getObjectClass() {
      return this.listType;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      Class instanceType = this.instanceType;
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         ObjectReader objectReader = jsonReader.checkAutoType(this.listType, 0L, features);
         if (objectReader != null) {
            instanceType = objectReader.getObjectClass();
         }

         if (instanceType == ObjectReaderImplList.CLASS_ARRAYS_LIST) {
            int entryCnt = jsonReader.startArray();
            String[] array = new String[entryCnt];

            for (int i = 0; i < entryCnt; i++) {
               array[i] = jsonReader.readString();
            }

            return Arrays.asList(array);
         } else {
            int entryCnt = jsonReader.startArray();
            Function builder = null;
            Collection list;
            if (instanceType == ArrayList.class) {
               list = entryCnt > 0 ? new ArrayList(entryCnt) : new ArrayList();
            } else if (instanceType == JSONArray.class) {
               list = entryCnt > 0 ? new JSONArray(entryCnt) : new JSONArray();
            } else if (instanceType == ObjectReaderImplList.CLASS_UNMODIFIABLE_COLLECTION) {
               list = new ArrayList();
               builder = Collections::unmodifiableCollection;
            } else if (instanceType == ObjectReaderImplList.CLASS_UNMODIFIABLE_LIST) {
               list = new ArrayList();
               builder = Collections::unmodifiableList;
            } else if (instanceType == ObjectReaderImplList.CLASS_UNMODIFIABLE_SET) {
               list = new LinkedHashSet();
               builder = Collections::unmodifiableSet;
            } else if (instanceType == ObjectReaderImplList.CLASS_UNMODIFIABLE_SORTED_SET) {
               list = new TreeSet();
               builder = Collections::unmodifiableSortedSet;
            } else if (instanceType == ObjectReaderImplList.CLASS_UNMODIFIABLE_NAVIGABLE_SET) {
               list = new TreeSet();
               builder = Collections::unmodifiableNavigableSet;
            } else if (instanceType == ObjectReaderImplList.CLASS_SINGLETON) {
               list = new ArrayList();
               builder = collection -> Collections.singleton(collection.iterator().next());
            } else if (instanceType == ObjectReaderImplList.CLASS_SINGLETON_LIST) {
               list = new ArrayList();
               builder = collection -> Collections.singletonList(collection.iterator().next());
            } else if (instanceType != null && instanceType != this.listType) {
               String typeName = instanceType.getTypeName();
               switch (typeName) {
                  case "com.google.common.collect.ImmutableList":
                     list = new ArrayList();
                     builder = GuavaSupport.immutableListConverter();
                     break;
                  case "com.google.common.collect.ImmutableSet":
                     list = new ArrayList();
                     builder = GuavaSupport.immutableSetConverter();
                     break;
                  case "com.google.common.collect.Lists$TransformingRandomAccessList":
                     list = new ArrayList();
                     break;
                  case "com.google.common.collect.Lists.TransformingSequentialList":
                     list = new LinkedList();
                     break;
                  case "kotlin.collections.EmptyList":
                     list = ObjectReaderImplList.getKotlinEmptyList(instanceType);
                     break;
                  case "kotlin.collections.EmptySet":
                     list = ObjectReaderImplList.getKotlinEmptySet(instanceType);
                     break;
                  default:
                     try {
                        list = (Collection)instanceType.newInstance();
                     } catch (IllegalAccessException | InstantiationException var15) {
                        throw new JSONException(jsonReader.info("create instance error " + instanceType), var15);
                     }
               }
            } else {
               list = (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features);
            }

            for (int i = 0; i < entryCnt; i++) {
               list.add(jsonReader.readString());
            }

            if (builder != null) {
               list = (Collection)builder.apply(list);
            }

            return list;
         }
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else {
         boolean set = jsonReader.nextIfSet();
         Collection list = (Collection)(set ? new HashSet() : (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features));
         char ch = jsonReader.current();
         if (ch == '[') {
            jsonReader.next();

            while (!jsonReader.nextIfArrayEnd()) {
               String item = jsonReader.readString();
               if (item != null || !(list instanceof SortedSet)) {
                  list.add(item);
               }
            }
         } else {
            if (ch != '"' && ch != '\'' && ch != '{') {
               throw new JSONException(jsonReader.info());
            }

            String str = jsonReader.readString();
            if (str != null && !str.isEmpty()) {
               list.add(str);
            }
         }

         jsonReader.nextIfComma();
         return list;
      }
   }
}
