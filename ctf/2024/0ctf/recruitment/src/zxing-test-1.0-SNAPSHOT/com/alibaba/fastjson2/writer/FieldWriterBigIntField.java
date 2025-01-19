package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.math.BigInteger;

final class FieldWriterBigIntField<T> extends FieldWriter<T> {
   FieldWriterBigIntField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, null, label, BigInteger.class, BigInteger.class, field, null);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      BigInteger value = (BigInteger)this.getFieldValue(object);
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
   public void writeValue(JSONWriter jsonWriter, T object) {
      BigInteger value = (BigInteger)this.getFieldValue(object);
      jsonWriter.writeBigInt(value, this.features);
   }
}
