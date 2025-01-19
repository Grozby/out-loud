package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.function.Function;

public final class ObjectReaderImplFromString<T> extends ObjectReaderPrimitive<T> {
   final Function<String, T> creator;

   public ObjectReaderImplFromString(Class<T> objectClass, Function<String, T> creator) {
      super(objectClass);
      this.creator = creator;
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String str = jsonReader.readString();
      return str != null && !str.isEmpty() ? this.creator.apply(str) : null;
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String str = jsonReader.readString();
      return str == null ? null : this.creator.apply(str);
   }
}
