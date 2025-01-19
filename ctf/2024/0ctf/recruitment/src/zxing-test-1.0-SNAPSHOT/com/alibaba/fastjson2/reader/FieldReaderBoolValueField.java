package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;

final class FieldReaderBoolValueField<T> extends FieldReaderObjectField<T> {
   FieldReaderBoolValueField(String fieldName, int ordinal, long features, String format, Boolean defaultValue, JSONSchema schema, Field field) {
      super(fieldName, boolean.class, boolean.class, ordinal, features, format, null, defaultValue, schema, field);
   }

   @Override
   public void readFieldValue(JSONReader jsonReader, T object) {
      boolean fieldValue = jsonReader.readBoolValue();
      if (this.schema != null) {
         this.schema.assertValidate(fieldValue);
      }

      try {
         this.field.setBoolean(object, fieldValue);
      } catch (Exception var5) {
         throw new JSONException(jsonReader.info("set " + this.fieldName + " error"), var5);
      }
   }

   @Override
   public void accept(T object, int value) {
      this.accept(object, TypeUtils.toBooleanValue(value));
   }

   @Override
   public void accept(T object, Object value) {
      if (value == null) {
         if ((this.features & JSONReader.Feature.IgnoreSetNullValue.mask) == 0L) {
            this.accept(object, false);
         }
      } else if (value instanceof Boolean) {
         this.accept(object, ((Boolean)value).booleanValue());
      } else {
         throw new JSONException("set " + this.fieldName + " error, type not support " + value.getClass());
      }
   }

   @Override
   public void accept(T object, boolean value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (this.fieldOffset != -1L) {
         JDKUtils.UNSAFE.putBoolean(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setBoolean(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public Object readFieldValue(JSONReader jsonReader) {
      return jsonReader.readBool();
   }
}
