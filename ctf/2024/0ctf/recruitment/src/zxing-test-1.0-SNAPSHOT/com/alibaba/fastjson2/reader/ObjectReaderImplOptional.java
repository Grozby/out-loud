package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Optional;

class ObjectReaderImplOptional extends ObjectReaderPrimitive {
   static final ObjectReaderImplOptional INSTANCE = new ObjectReaderImplOptional(null, null, null);
   final String format;
   final Locale locale;
   final Type itemType;
   final Class itemClass;
   ObjectReader itemObjectReader;

   static ObjectReaderImplOptional of(Type type, String format, Locale locale) {
      return type == null ? INSTANCE : new ObjectReaderImplOptional(type, format, locale);
   }

   public ObjectReaderImplOptional(Type type, String format, Locale locale) {
      super(Optional.class);
      Type itemType = null;
      if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         if (actualTypeArguments.length == 1) {
            itemType = actualTypeArguments[0];
         }
      }

      this.itemType = itemType;
      this.itemClass = TypeUtils.getClass(itemType);
      this.format = format;
      this.locale = locale;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      Object value;
      if (this.itemType == null) {
         value = jsonReader.readAny();
      } else {
         if (this.itemObjectReader == null) {
            ObjectReader formattedObjectReader = null;
            if (this.format != null) {
               formattedObjectReader = FieldReader.createFormattedObjectReader(this.itemType, this.itemClass, this.format, this.locale);
            }

            if (formattedObjectReader == null) {
               this.itemObjectReader = jsonReader.getObjectReader(this.itemType);
            } else {
               this.itemObjectReader = formattedObjectReader;
            }
         }

         value = this.itemObjectReader.readJSONBObject(jsonReader, this.itemType, fieldName, 0L);
      }

      return value == null ? Optional.empty() : Optional.of(value);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      Object value;
      if (this.itemType == null) {
         value = jsonReader.readAny();
      } else {
         if (this.itemObjectReader == null) {
            ObjectReader formattedObjectReader = null;
            if (this.format != null) {
               formattedObjectReader = FieldReader.createFormattedObjectReader(this.itemType, this.itemClass, this.format, this.locale);
            }

            if (formattedObjectReader == null) {
               this.itemObjectReader = jsonReader.getObjectReader(this.itemType);
            } else {
               this.itemObjectReader = formattedObjectReader;
            }
         }

         value = this.itemObjectReader.readObject(jsonReader, this.itemType, fieldName, 0L);
      }

      return value == null ? Optional.empty() : Optional.of(value);
   }
}
