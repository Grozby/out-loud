package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.GuavaSupport;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

public final class ObjectReaderImplList implements ObjectReader {
   static final Class CLASS_EMPTY_SET = Collections.emptySet().getClass();
   static final Class CLASS_EMPTY_LIST = Collections.emptyList().getClass();
   static final Class CLASS_SINGLETON = Collections.singleton(0).getClass();
   static final Class CLASS_SINGLETON_LIST = Collections.singletonList(0).getClass();
   static final Class CLASS_ARRAYS_LIST = Arrays.asList(0).getClass();
   static final Class CLASS_UNMODIFIABLE_COLLECTION = Collections.unmodifiableCollection(Collections.emptyList()).getClass();
   static final Class CLASS_UNMODIFIABLE_LIST = Collections.unmodifiableList(Collections.emptyList()).getClass();
   static final Class CLASS_UNMODIFIABLE_SET = Collections.unmodifiableSet(Collections.emptySet()).getClass();
   static final Class CLASS_UNMODIFIABLE_SORTED_SET = Collections.unmodifiableSortedSet(Collections.emptySortedSet()).getClass();
   static final Class CLASS_UNMODIFIABLE_NAVIGABLE_SET = Collections.unmodifiableNavigableSet(Collections.emptyNavigableSet()).getClass();
   public static ObjectReaderImplList INSTANCE = new ObjectReaderImplList(ArrayList.class, ArrayList.class, ArrayList.class, Object.class, null);
   static List kotlinEmptyList;
   static Set kotlinEmptySet;
   final Type listType;
   final Class listClass;
   final Class instanceType;
   final long instanceTypeHash;
   final Type itemType;
   final Class itemClass;
   final String itemClassName;
   final long itemClassNameHash;
   final Function builder;
   Object listSingleton;
   ObjectReader itemObjectReader;
   volatile boolean instanceError;
   volatile Constructor constructor;

   public static ObjectReader of(Type type, Class listClass, long features) {
      if (listClass == type && "".equals(listClass.getSimpleName())) {
         type = listClass.getGenericSuperclass();
         listClass = listClass.getSuperclass();
      }

      Type itemType = Object.class;
      Type rawType;
      if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         rawType = parameterizedType.getRawType();
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            itemType = actualTypeArguments[0];
         }
      } else {
         rawType = type;
         if (listClass != null) {
            Type superType = listClass.getGenericSuperclass();
            if (superType instanceof ParameterizedType) {
               ParameterizedType parameterizedType = (ParameterizedType)superType;
               rawType = parameterizedType.getRawType();
               Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  itemType = actualTypeArguments[0];
               }
            }
         }
      }

      if (listClass == null) {
         listClass = TypeUtils.getClass(rawType);
      }

      Function builder = null;
      Class instanceClass;
      if (listClass == Iterable.class
         || listClass == Collection.class
         || listClass == List.class
         || listClass == AbstractCollection.class
         || listClass == AbstractList.class) {
         instanceClass = ArrayList.class;
      } else if (listClass == Queue.class || listClass == Deque.class || listClass == AbstractSequentialList.class) {
         instanceClass = LinkedList.class;
      } else if (listClass == Set.class || listClass == AbstractSet.class) {
         instanceClass = HashSet.class;
      } else if (listClass == EnumSet.class) {
         instanceClass = HashSet.class;
         builder = o -> {
            Collection collection = (Collection)o;
            return collection.isEmpty() && itemType instanceof Class ? EnumSet.noneOf((Class)itemType) : EnumSet.copyOf(collection);
         };
      } else if (listClass == NavigableSet.class || listClass == SortedSet.class) {
         instanceClass = TreeSet.class;
      } else if (listClass == CLASS_SINGLETON) {
         instanceClass = ArrayList.class;
         builder = obj -> Collections.singleton(((List)obj).get(0));
      } else if (listClass == CLASS_SINGLETON_LIST) {
         instanceClass = ArrayList.class;
         builder = obj -> Collections.singletonList(((List)obj).get(0));
      } else if (listClass == CLASS_ARRAYS_LIST) {
         instanceClass = CLASS_ARRAYS_LIST;
         builder = obj -> Arrays.asList(((List)obj).toArray());
      } else if (listClass == CLASS_UNMODIFIABLE_COLLECTION) {
         instanceClass = ArrayList.class;
         builder = obj -> Collections.unmodifiableCollection((Collection)obj);
      } else if (listClass == CLASS_UNMODIFIABLE_LIST) {
         instanceClass = ArrayList.class;
         builder = obj -> Collections.unmodifiableList((List)obj);
      } else if (listClass == CLASS_UNMODIFIABLE_SET) {
         instanceClass = LinkedHashSet.class;
         builder = obj -> Collections.unmodifiableSet((Set)obj);
      } else if (listClass == CLASS_UNMODIFIABLE_SORTED_SET) {
         instanceClass = TreeSet.class;
         builder = obj -> Collections.unmodifiableSortedSet((SortedSet)obj);
      } else if (listClass == CLASS_UNMODIFIABLE_NAVIGABLE_SET) {
         instanceClass = TreeSet.class;
         builder = obj -> Collections.unmodifiableNavigableSet((NavigableSet)obj);
      } else {
         String typeName = listClass.getTypeName();
         switch (typeName) {
            case "com.google.common.collect.ImmutableList":
            case "com.google.common.collect.SingletonImmutableList":
            case "com.google.common.collect.RegularImmutableList":
            case "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList":
               instanceClass = ArrayList.class;
               builder = GuavaSupport.immutableListConverter();
               break;
            case "com.google.common.collect.ImmutableSet":
            case "com.google.common.collect.SingletonImmutableSet":
            case "com.google.common.collect.RegularImmutableSet":
               instanceClass = ArrayList.class;
               builder = GuavaSupport.immutableSetConverter();
               break;
            case "com.google.common.collect.Lists$TransformingRandomAccessList":
               instanceClass = ArrayList.class;
               break;
            case "com.google.common.collect.Lists.TransformingSequentialList":
               instanceClass = LinkedList.class;
               break;
            case "java.util.Collections$SynchronizedRandomAccessList":
               instanceClass = ArrayList.class;
               builder = Collections::synchronizedList;
               break;
            case "java.util.Collections$SynchronizedCollection":
               instanceClass = ArrayList.class;
               builder = Collections::synchronizedCollection;
               break;
            case "java.util.Collections$SynchronizedSet":
               instanceClass = HashSet.class;
               builder = Collections::synchronizedSet;
               break;
            case "java.util.Collections$SynchronizedSortedSet":
               instanceClass = TreeSet.class;
               builder = Collections::synchronizedSortedSet;
               break;
            case "java.util.Collections$SynchronizedNavigableSet":
               instanceClass = TreeSet.class;
               builder = Collections::synchronizedNavigableSet;
               break;
            case "java.util.RandomAccessSubList":
            case "java.util.AbstractList$RandomAccessSubList":
               instanceClass = ArrayList.class;
               break;
            default:
               instanceClass = listClass;
         }
      }

      String var16 = type.getTypeName();
      switch (var16) {
         case "kotlin.collections.EmptySet": {
            Class<?> clazz = (Class<?>)type;
            return new ObjectReaderImplList(clazz, getKotlinEmptySet(clazz));
         }
         case "kotlin.collections.EmptyList": {
            Class<?> clazz = (Class<?>)type;
            return new ObjectReaderImplList(clazz, getKotlinEmptyList(clazz));
         }
         case "java.util.Collections$EmptySet":
            return new ObjectReaderImplList((Class)type, Collections.emptySet());
         case "java.util.Collections$EmptyList":
            return new ObjectReaderImplList((Class)type, Collections.emptyList());
         default:
            if (itemType == String.class && builder == null) {
               return new ObjectReaderImplListStr(listClass, instanceClass);
            } else {
               return (ObjectReader)(itemType == Long.class && builder == null
                  ? new ObjectReaderImplListInt64(listClass, instanceClass)
                  : new ObjectReaderImplList(type, listClass, instanceClass, itemType, builder));
            }
      }
   }

   ObjectReaderImplList(Class listClass, Object listSingleton) {
      this(listClass, listClass, listClass, Object.class, null);
      this.listSingleton = listSingleton;
   }

   public ObjectReaderImplList(Type listType, Class listClass, Class instanceType, Type itemType, Function builder) {
      this.listType = listType;
      this.listClass = listClass;
      this.instanceType = instanceType;
      this.instanceTypeHash = Fnv.hashCode64(TypeUtils.getTypeName(instanceType));
      this.itemType = itemType;
      this.itemClass = TypeUtils.getClass(itemType);
      this.builder = builder;
      this.itemClassName = this.itemClass != null ? TypeUtils.getTypeName(this.itemClass) : null;
      this.itemClassNameHash = this.itemClassName != null ? Fnv.hashCode64(this.itemClassName) : 0L;
   }

   static Set getKotlinEmptySet(Class clazz) {
      Set empty = kotlinEmptySet;
      if (empty == null) {
         try {
            Field field = clazz.getField("INSTANCE");
            if (!field.isAccessible()) {
               field.setAccessible(true);
            }

            kotlinEmptySet = empty = (Set)field.get(null);
         } catch (IllegalAccessException | NoSuchFieldException var3) {
            throw new IllegalStateException("Failed to get singleton of " + clazz, var3);
         }
      }

      return empty;
   }

   static List getKotlinEmptyList(Class clazz) {
      List empty = kotlinEmptyList;
      if (empty == null) {
         try {
            Field field = clazz.getField("INSTANCE");
            if (!field.isAccessible()) {
               field.setAccessible(true);
            }

            kotlinEmptyList = empty = (List)field.get(null);
         } catch (IllegalAccessException | NoSuchFieldException var3) {
            throw new IllegalStateException("Failed to get singleton of " + clazz, var3);
         }
      }

      return empty;
   }

   @Override
   public Class getObjectClass() {
      return this.listClass;
   }

   @Override
   public Function getBuildFunction() {
      return this.builder;
   }

   @Override
   public Object createInstance(Collection collection, long features) {
      int size = collection.size();
      if (size == 0 && this.listClass == List.class) {
         Collection list = new ArrayList();
         return this.builder != null ? this.builder.apply(list) : list;
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         Collection list;
         if (this.instanceType == ArrayList.class) {
            list = new ArrayList(collection.size());
         } else {
            list = (Collection)this.createInstance(0L);
         }

         for (Object item : collection) {
            if (item == null) {
               list.add(null);
            } else {
               Object value = item;
               Class<?> valueClass = item.getClass();
               if ((valueClass == JSONObject.class || valueClass == TypeUtils.CLASS_JSON_OBJECT_1x) && this.itemClass != valueClass) {
                  if (this.itemObjectReader == null) {
                     this.itemObjectReader = provider.getObjectReader(this.itemType);
                  }

                  value = this.itemObjectReader.createInstance((Map)item, features);
               } else if (valueClass != this.itemType) {
                  Function typeConvert = provider.getTypeConvert(valueClass, this.itemType);
                  if (typeConvert != null) {
                     value = typeConvert.apply(item);
                  } else if (item instanceof Map) {
                     Map map = (Map)item;
                     if (this.itemObjectReader == null) {
                        this.itemObjectReader = provider.getObjectReader(this.itemType);
                     }

                     value = this.itemObjectReader.createInstance(map, 0L);
                  } else if (item instanceof Collection) {
                     if (this.itemObjectReader == null) {
                        this.itemObjectReader = provider.getObjectReader(this.itemType);
                     }

                     value = this.itemObjectReader.createInstance((Collection)item, features);
                  } else if (!this.itemClass.isInstance(item)) {
                     if (!Enum.class.isAssignableFrom(this.itemClass)) {
                        throw new JSONException("can not convert from " + valueClass + " to " + this.itemType);
                     }

                     if (this.itemObjectReader == null) {
                        this.itemObjectReader = provider.getObjectReader(this.itemType);
                     }

                     if (!(this.itemObjectReader instanceof ObjectReaderImplEnum)) {
                        throw new JSONException("can not convert from " + valueClass + " to " + this.itemType);
                     }

                     value = ((ObjectReaderImplEnum)this.itemObjectReader).getEnum((String)item);
                  }
               }

               list.add(value);
            }
         }

         return this.builder != null ? this.builder.apply(list) : list;
      }
   }

   @Override
   public Object createInstance(long features) {
      if (this.instanceType == ArrayList.class) {
         return JDKUtils.JVM_VERSION == 8 ? new ArrayList(10) : new ArrayList();
      } else if (this.instanceType == LinkedList.class) {
         return new LinkedList();
      } else if (this.instanceType == HashSet.class) {
         return new HashSet();
      } else if (this.instanceType == LinkedHashSet.class) {
         return new LinkedHashSet();
      } else if (this.instanceType == TreeSet.class) {
         return new TreeSet();
      } else if (this.listSingleton != null) {
         return this.listSingleton;
      } else {
         if (this.instanceType != null) {
            JSONException error = null;
            if (this.constructor == null && !BeanUtils.hasPublicDefaultConstructor(this.instanceType)) {
               this.constructor = BeanUtils.getDefaultConstructor(this.instanceType, false);
               this.constructor.setAccessible(true);
            }

            if (!this.instanceError) {
               try {
                  if (this.constructor != null) {
                     return this.constructor.newInstance();
                  }

                  return this.instanceType.newInstance();
               } catch (IllegalAccessException | InvocationTargetException | RuntimeException | InstantiationException var6) {
                  this.instanceError = true;
                  error = new JSONException("create list error, type " + this.instanceType);
               }
            }

            if (this.instanceError && List.class.isAssignableFrom(this.instanceType.getSuperclass())) {
               try {
                  return this.instanceType.getSuperclass().newInstance();
               } catch (IllegalAccessException | InstantiationException var5) {
                  this.instanceError = true;
                  error = new JSONException("create list error, type " + this.instanceType);
               }
            }

            if (error != null) {
               throw error;
            }
         }

         return new ArrayList();
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNull()) {
         return null;
      } else {
         ObjectReader objectReader = jsonReader.checkAutoType(this.listClass, 0L, features);
         Function builder = this.builder;
         Class listType = this.instanceType;
         if (objectReader != null) {
            if (objectReader instanceof ObjectReaderImplList) {
               listType = ((ObjectReaderImplList)objectReader).instanceType;
               builder = ((ObjectReaderImplList)objectReader).builder;
            } else {
               listType = objectReader.getObjectClass();
            }

            if (listType == CLASS_UNMODIFIABLE_COLLECTION) {
               listType = ArrayList.class;
               builder = Collections::unmodifiableCollection;
            } else if (listType == CLASS_UNMODIFIABLE_LIST) {
               listType = ArrayList.class;
               builder = Collections::unmodifiableList;
            } else if (listType == CLASS_UNMODIFIABLE_SET) {
               listType = LinkedHashSet.class;
               builder = Collections::unmodifiableSet;
            } else if (listType == CLASS_UNMODIFIABLE_SORTED_SET) {
               listType = TreeSet.class;
               builder = Collections::unmodifiableSortedSet;
            } else if (listType == CLASS_UNMODIFIABLE_NAVIGABLE_SET) {
               listType = TreeSet.class;
               builder = Collections::unmodifiableNavigableSet;
            } else if (listType == CLASS_SINGLETON) {
               listType = ArrayList.class;
               builder = list -> Collections.singleton(list.iterator().next());
            } else if (listType == CLASS_SINGLETON_LIST) {
               listType = ArrayList.class;
               builder = list -> Collections.singletonList(list.get(0));
            } else {
               String entryCnt = listType.getTypeName();
               switch (entryCnt) {
                  case "kotlin.collections.EmptySet":
                  case "kotlin.collections.EmptyList":
                     return objectReader.readObject(jsonReader, fieldType, fieldName, features);
               }
            }
         }

         int entryCnt = jsonReader.startArray();
         if (entryCnt > 0 && this.itemObjectReader == null) {
            this.itemObjectReader = jsonReader.getContext().getObjectReader(this.itemType);
         }

         if (listType == CLASS_ARRAYS_LIST) {
            Object[] array = new Object[entryCnt];
            List list = Arrays.asList(array);

            for (int i = 0; i < entryCnt; i++) {
               Object item;
               if (jsonReader.isReference()) {
                  String reference = jsonReader.readReference();
                  if ("..".equals(reference)) {
                     item = list;
                  } else {
                     item = null;
                     jsonReader.addResolveTask(list, i, JSONPath.of(reference));
                  }
               } else {
                  item = this.itemObjectReader.readJSONBObject(jsonReader, this.itemType, i, features);
               }

               array[i] = item;
            }

            return list;
         } else {
            Collection list;
            if (listType == ArrayList.class) {
               list = entryCnt > 0 ? new ArrayList(entryCnt) : new ArrayList();
            } else if (listType == JSONArray.class) {
               list = entryCnt > 0 ? new JSONArray(entryCnt) : new JSONArray();
            } else if (listType == HashSet.class) {
               list = new HashSet();
            } else if (listType == LinkedHashSet.class) {
               list = new LinkedHashSet();
            } else if (listType == TreeSet.class) {
               list = new TreeSet();
            } else if (listType == CLASS_EMPTY_SET) {
               list = Collections.emptySet();
            } else if (listType == CLASS_EMPTY_LIST) {
               list = Collections.emptyList();
            } else if (listType == CLASS_SINGLETON_LIST) {
               list = new ArrayList();
               builder = items -> Collections.singletonList(items.iterator().next());
            } else if (listType == CLASS_UNMODIFIABLE_LIST) {
               list = new ArrayList();
               builder = Collections::unmodifiableList;
            } else if (listType != null && EnumSet.class.isAssignableFrom(listType)) {
               list = new HashSet();
               builder = o -> {
                  Collection collection = (Collection)o;
                  return collection.isEmpty() && this.itemType instanceof Class ? EnumSet.noneOf((Class)this.itemType) : EnumSet.copyOf(collection);
               };
            } else if (listType != null && listType != this.listType) {
               String itemObjectReader = listType.getName();
               switch (itemObjectReader) {
                  case "kotlin.collections.EmptySet":
                     list = getKotlinEmptySet(listType);
                     break;
                  case "kotlin.collections.EmptyList":
                     list = getKotlinEmptyList(listType);
                     break;
                  default:
                     try {
                        list = (Collection)listType.newInstance();
                     } catch (IllegalAccessException | InstantiationException var16) {
                        throw new JSONException(jsonReader.info("create instance error " + listType), var16);
                     }
               }
            } else {
               list = (Collection)this.createInstance(jsonReader.getContext().getFeatures() | features);
            }

            ObjectReader itemObjectReader = this.itemObjectReader;
            Type itemType = this.itemType;
            if (fieldType instanceof ParameterizedType) {
               Type[] actualTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  itemType = actualTypeArguments[0];
                  if (itemType != this.itemType) {
                     itemObjectReader = jsonReader.getObjectReader(itemType);
                  }
               }
            }

            for (int i = 0; i < entryCnt; i++) {
               Object item;
               if (jsonReader.isReference()) {
                  String reference = jsonReader.readReference();
                  if ("..".equals(reference)) {
                     item = list;
                  } else {
                     jsonReader.addResolveTask(list, i, JSONPath.of(reference));
                     if (!(list instanceof List)) {
                        continue;
                     }

                     item = null;
                  }
               } else {
                  ObjectReader autoTypeReader = jsonReader.checkAutoType(this.itemClass, this.itemClassNameHash, features);
                  if (autoTypeReader != null) {
                     item = autoTypeReader.readJSONBObject(jsonReader, itemType, i, features);
                  } else {
                     item = itemObjectReader.readJSONBObject(jsonReader, itemType, i, features);
                  }
               }

               list.add(item);
            }

            return builder != null ? builder.apply(list) : list;
         }
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      JSONReader.Context context = jsonReader.getContext();
      if (this.itemObjectReader == null) {
         this.itemObjectReader = context.getObjectReader(this.itemType);
      }

      if (jsonReader.jsonb) {
         return this.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else {
         Collection list;
         if (jsonReader.nextIfSet()) {
            list = new HashSet();
         } else {
            list = (Collection)this.createInstance(context.getFeatures() | features);
         }

         char ch = jsonReader.current();
         if (ch == '"') {
            String str = jsonReader.readString();
            if (this.itemClass == String.class) {
               jsonReader.nextIfComma();
               list.add(str);
               return list;
            } else if (str.isEmpty()) {
               jsonReader.nextIfComma();
               return null;
            } else {
               ObjectReaderProvider provider = context.getProvider();
               if (this.itemClass.isEnum()) {
                  ObjectReader enumReader = provider.getObjectReader(this.itemClass);
                  if (enumReader instanceof ObjectReaderImplEnum) {
                     Enum e = ((ObjectReaderImplEnum)enumReader).getEnum(str);
                     if (e == null) {
                        if (JSONReader.Feature.ErrorOnEnumNotMatch.isEnabled(jsonReader.features(features))) {
                           throw new JSONException(jsonReader.info("enum not match : " + str));
                        }

                        return null;
                     }

                     list.add(e);
                     return list;
                  }
               }

               Function typeConvert = provider.getTypeConvert(String.class, this.itemType);
               if (typeConvert != null) {
                  Object converted = typeConvert.apply(str);
                  jsonReader.nextIfComma();
                  list.add(converted);
                  return list;
               } else {
                  throw new JSONException(jsonReader.info());
               }
            }
         } else if (ch == '[') {
            jsonReader.next();
            ObjectReader var14 = this.itemObjectReader;
            Type itemType = this.itemType;
            if (fieldType != this.listType && fieldType instanceof ParameterizedType) {
               Type[] actualTypeArguments = ((ParameterizedType)fieldType).getActualTypeArguments();
               if (actualTypeArguments.length == 1) {
                  itemType = actualTypeArguments[0];
                  if (itemType != this.itemType) {
                     var14 = jsonReader.getObjectReader(itemType);
                  }
               }
            }

            for (int i = 0; !jsonReader.nextIfArrayEnd(); i++) {
               Object item;
               if (itemType == String.class) {
                  item = jsonReader.readString();
               } else {
                  if (var14 == null) {
                     throw new JSONException(jsonReader.info("TODO : " + itemType));
                  }

                  if (jsonReader.isReference()) {
                     String reference = jsonReader.readReference();
                     if (!"..".equals(reference)) {
                        jsonReader.addResolveTask(list, i, JSONPath.of(reference));
                        continue;
                     }

                     item = this;
                  } else {
                     item = var14.readObject(jsonReader, itemType, i, 0L);
                  }
               }

               list.add(item);
            }

            jsonReader.nextIfComma();
            return this.builder != null ? this.builder.apply(list) : list;
         } else if (this.itemClass != Object.class && this.itemObjectReader != null || this.itemClass == Object.class && jsonReader.isObject()) {
            Object item = this.itemObjectReader.readObject(jsonReader, this.itemType, 0, 0L);
            list.add(item);
            if (this.builder != null) {
               list = (Collection)this.builder.apply(list);
            }

            return list;
         } else {
            throw new JSONException(jsonReader.info());
         }
      }
   }
}
