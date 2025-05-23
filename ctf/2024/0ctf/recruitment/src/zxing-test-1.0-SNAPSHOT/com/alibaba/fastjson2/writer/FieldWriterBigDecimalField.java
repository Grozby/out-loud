package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;

final class FieldWriterBigDecimalField<T> extends FieldWriter<T> {
   FieldWriterBigDecimalField(String name, int ordinal, long features, String format, String label, Field field) {
      super(name, ordinal, features, format, null, label, BigDecimal.class, BigDecimal.class, field, null);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      BigDecimal value = (BigDecimal)this.getFieldValue(object);
      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & JSONWriter.Feature.WriteNulls.mask) == 0L) {
            return false;
         }
      }

      this.writeFieldName(jsonWriter);
      jsonWriter.writeDecimal(value, this.features, this.decimalFormat);
      return true;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      BigDecimal value = (BigDecimal)this.getFieldValue(object);
      jsonWriter.writeDecimal(value, this.features, this.decimalFormat);
   }
}
