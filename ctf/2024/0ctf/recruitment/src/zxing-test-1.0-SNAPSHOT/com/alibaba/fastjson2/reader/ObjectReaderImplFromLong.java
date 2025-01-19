package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;
import java.util.function.LongFunction;

public final class ObjectReaderImplFromLong<T> extends ObjectReaderPrimitive<T> {
   final LongFunction<T> creator;

   public ObjectReaderImplFromLong(Class<T> objectClass, LongFunction creator) {
      super(objectClass);
      this.creator = creator;
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.nextIfNull() ? null : this.creator.apply(jsonReader.readInt64Value());
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return jsonReader.nextIfNull() ? null : this.creator.apply(jsonReader.readInt64Value());
   }
}
