package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;

abstract class ObjectReaderPrimitive<T> implements ObjectReader<T> {
   protected final Class objectClass;

   public ObjectReaderPrimitive(Class objectClass) {
      this.objectClass = objectClass;
   }

   @Override
   public Class getObjectClass() {
      return this.objectClass;
   }

   @Override
   public T createInstance(long features) {
      throw new JSONException("createInstance not supported " + this.objectClass.getName());
   }

   @Override
   public abstract T readJSONBObject(JSONReader var1, Type var2, Object var3, long var4);
}
