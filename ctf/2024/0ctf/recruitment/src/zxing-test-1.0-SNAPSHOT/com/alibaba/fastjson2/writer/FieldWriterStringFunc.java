package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

final class FieldWriterStringFunc<T> extends FieldWriter<T> {
   final Function<T, String> function;
   final boolean symbol;
   final boolean trim;
   final boolean raw;

   FieldWriterStringFunc(String fieldName, int ordinal, long features, String format, String label, Field field, Method method, Function<T, String> function) {
      super(fieldName, ordinal, features, format, null, label, String.class, String.class, field, method);
      this.function = function;
      this.symbol = "symbol".equals(format);
      this.trim = "trim".equals(format);
      this.raw = (features & 1125899906842624L) != 0L;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      String value;
      try {
         value = this.function.apply(object);
      } catch (RuntimeException var6) {
         if ((jsonWriter.getFeatures(this.features) | JSONWriter.Feature.IgnoreNonFieldGetter.mask) != 0L) {
            return false;
         }

         throw var6;
      }

      long features = this.features | jsonWriter.getFeatures();
      if (value == null) {
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask))
            == 0L) {
            return false;
         }
      } else if (this.trim) {
         value = value.trim();
      }

      if (value != null && value.isEmpty() && (features & JSONWriter.Feature.IgnoreEmpty.mask) != 0L) {
         return false;
      } else {
         this.writeFieldName(jsonWriter);
         if (value == null) {
            if ((features & (JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) != 0L) {
               jsonWriter.writeString("");
            } else {
               jsonWriter.writeNull();
            }

            return true;
         } else {
            if (this.symbol && jsonWriter.jsonb) {
               jsonWriter.writeSymbol(value);
            } else if (this.raw) {
               jsonWriter.writeRaw(value);
            } else {
               jsonWriter.writeString(value);
            }

            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      String value = this.function.apply(object);
      if (this.trim && value != null) {
         value = value.trim();
      }

      if (this.symbol && jsonWriter.jsonb) {
         jsonWriter.writeSymbol(value);
      } else if (this.raw) {
         jsonWriter.writeRaw(value);
      } else {
         jsonWriter.writeString(value);
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
