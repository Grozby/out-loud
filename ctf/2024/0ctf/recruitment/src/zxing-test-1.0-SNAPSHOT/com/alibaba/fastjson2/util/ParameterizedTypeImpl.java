package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

@JSONType(
   deserializeFeatures = {JSONReader.Feature.SupportAutoType},
   typeName = "java.lang.reflect.ParameterizedType"
)
public class ParameterizedTypeImpl implements ParameterizedType {
   private final Type[] actualTypeArguments;
   private final Type ownerType;
   private final Type rawType;

   @JSONCreator
   public ParameterizedTypeImpl(Type[] actualTypeArguments, Type ownerType, Type rawType) {
      this.actualTypeArguments = actualTypeArguments;
      this.ownerType = ownerType;
      this.rawType = rawType;
   }

   public ParameterizedTypeImpl(Type rawType, Type... actualTypeArguments) {
      this.rawType = rawType;
      this.actualTypeArguments = actualTypeArguments;
      this.ownerType = null;
   }

   @Override
   public Type[] getActualTypeArguments() {
      return this.actualTypeArguments;
   }

   @Override
   public Type getOwnerType() {
      return this.ownerType;
   }

   @Override
   public Type getRawType() {
      return this.rawType;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ParameterizedTypeImpl that = (ParameterizedTypeImpl)o;
         if (!Arrays.equals((Object[])this.actualTypeArguments, (Object[])that.actualTypeArguments)) {
            return false;
         } else {
            return !Objects.equals(this.ownerType, that.ownerType) ? false : Objects.equals(this.rawType, that.rawType);
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.actualTypeArguments != null ? Arrays.hashCode((Object[])this.actualTypeArguments) : 0;
      result = 31 * result + (this.ownerType != null ? this.ownerType.hashCode() : 0);
      return 31 * result + (this.rawType != null ? this.rawType.hashCode() : 0);
   }
}
