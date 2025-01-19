package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

public final class ObjectArrayReader extends ObjectReaderPrimitive {
   public static final ObjectArrayReader INSTANCE = new ObjectArrayReader();
   public static final long TYPE_HASH_CODE = Fnv.hashCode64("[O");

   public ObjectArrayReader() {
      super(Object[].class);
   }

   public Object[] createInstance(Collection collection, long features) {
      Object[] array = new Object[collection.size()];
      int i = 0;

      for (Object item : collection) {
         array[i++] = item;
      }

      return array;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else if (jsonReader.nextIfArrayStart()) {
         Object[] values = new Object[16];
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

            char ch = jsonReader.current();
            Object value;
            switch (ch) {
               case '"':
                  value = jsonReader.readString();
                  break;
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
               case '[':
                  value = jsonReader.readArray();
                  break;
               case 'f':
               case 't':
                  value = jsonReader.readBoolValue();
                  break;
               case 'n':
                  jsonReader.readNull();
                  value = null;
                  break;
               case '{':
                  value = jsonReader.read(Object.class);
                  break;
               default:
                  throw new JSONException(jsonReader.info());
            }

            values[size++] = value;
         }

         jsonReader.nextIfComma();
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
         ObjectReader autoTypeObjectReader = jsonReader.checkAutoType(Object[].class, TYPE_HASH_CODE, features);
         if (autoTypeObjectReader != this) {
            return autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
         }
      }

      int itemCnt = jsonReader.startArray();
      if (itemCnt == -1) {
         return null;
      } else {
         Object[] array = new Object[itemCnt];

         for (int i = 0; i < itemCnt; i++) {
            byte type = jsonReader.getType();
            Object value;
            if (type >= 73 && type <= 125) {
               value = jsonReader.readString();
            } else if (type == -110) {
               ObjectReader autoTypeValueReader = jsonReader.checkAutoType(Object.class, 0L, features);
               if (autoTypeValueReader != null) {
                  value = autoTypeValueReader.readJSONBObject(jsonReader, null, null, features);
               } else {
                  value = jsonReader.readAny();
               }
            } else if (type == -81) {
               jsonReader.next();
               value = null;
            } else if (type == -79) {
               jsonReader.next();
               value = Boolean.TRUE;
            } else if (type == -80) {
               jsonReader.next();
               value = Boolean.FALSE;
            } else if (type == -66) {
               value = jsonReader.readInt64Value();
            } else {
               value = jsonReader.readAny();
            }

            array[i] = value;
         }

         return array;
      }
   }
}
