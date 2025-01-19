package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.function.Function;

final class FieldWriterBigIntFunc<T> extends FieldWriter<T> {
   final Function<T, BigInteger> function;

   FieldWriterBigIntFunc(
      String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Function<T, BigInteger> function
   ) {
      super(fieldName, ordinal, features, format, null, label, BigInteger.class, BigInteger.class, null, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      BigInteger value = (BigInteger)this.getFieldValue(object);
      jsonWriter.writeBigInt(value, this.features);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T o) {
      BigInteger value = this.function.apply(o);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
            return false;
         }
      }

      this.writeFieldName(jsonWriter);
      jsonWriter.writeBigInt(value, this.features);
      return true;
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
