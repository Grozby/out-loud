package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

final class ConstructorSupplier implements Supplier {
   final Constructor constructor;
   final Class objectClass;
   final boolean useClassNewInstance;

   public ConstructorSupplier(Constructor constructor) {
      constructor.setAccessible(true);
      this.constructor = constructor;
      this.objectClass = this.constructor.getDeclaringClass();
      this.useClassNewInstance = constructor.getParameterCount() == 0
         && Modifier.isPublic(constructor.getModifiers())
         && Modifier.isPublic(this.objectClass.getModifiers());
   }

   @Override
   public Object get() {
      try {
         if (this.useClassNewInstance) {
            return this.objectClass.newInstance();
         } else {
            return this.constructor.getParameterCount() == 1 ? this.constructor.newInstance() : this.constructor.newInstance();
         }
      } catch (Throwable var2) {
         throw new JSONException("create instance error", var2);
      }
   }
}
