package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.Locale;

class ObjectReaderImplLocale extends ObjectReaderPrimitive {
   static final ObjectReaderImplLocale INSTANCE = new ObjectReaderImplLocale();

   ObjectReaderImplLocale() {
      super(Locale.class);
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String strVal = jsonReader.readString();
      if (strVal != null && !strVal.isEmpty()) {
         String[] items = strVal.split("_");
         if (items.length == 1) {
            return new Locale(items[0]);
         } else {
            return items.length == 2 ? new Locale(items[0], items[1]) : new Locale(items[0], items[1], items[2]);
         }
      } else {
         return null;
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String strVal = jsonReader.readString();
      if (strVal != null && !strVal.isEmpty()) {
         String[] items = strVal.split("_");
         if (items.length == 1) {
            return new Locale(items[0]);
         } else {
            return items.length == 2 ? new Locale(items[0], items[1]) : new Locale(items[0], items[1], items[2]);
         }
      } else {
         return null;
      }
   }
}
