package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

class FieldReaderObjectField<T> extends FieldReaderObject<T> {
   FieldReaderObjectField(
      String fieldName,
      Type fieldType,
      Class fieldClass,
      int ordinal,
      long features,
      String format,
      Locale locale,
      Object defaultValue,
      JSONSchema schema,
      Field field
   ) {
      super(
         fieldName,
         (Type)(fieldType == null ? field.getType() : fieldType),
         fieldClass,
         ordinal,
         features,
         format,
         locale,
         defaultValue,
         schema,
         null,
         field,
         null
      );
   }

   @Override
   public void accept(T object, boolean value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == boolean.class) {
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
   public void accept(T object, byte value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == byte.class) {
         JDKUtils.UNSAFE.putByte(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setByte(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void accept(T object, short value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == short.class) {
         JDKUtils.UNSAFE.putShort(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setShort(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void accept(T object, int value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == int.class) {
         JDKUtils.UNSAFE.putInt(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setInt(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void accept(T object, long value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == long.class) {
         JDKUtils.UNSAFE.putLong(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setLong(object, value);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }
      }
   }

   @Override
   public void accept(T object, float value) {
      if (this.schema != null) {
         this.schema.assertValidate((double)value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == float.class) {
         JDKUtils.UNSAFE.putFloat(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setFloat(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void accept(T object, double value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == double.class) {
         JDKUtils.UNSAFE.putDouble(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setDouble(object, value);
         } catch (Exception var5) {
            throw new JSONException("set " + this.fieldName + " error", var5);
         }
      }
   }

   @Override
   public void accept(T object, char value) {
      if (this.schema != null) {
         this.schema.assertValidate((long)value);
      }

      if (this.fieldOffset != -1L && this.fieldClass == char.class) {
         JDKUtils.UNSAFE.putChar(object, this.fieldOffset, value);
      } else {
         try {
            this.field.setChar(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   @Override
   public void accept(T object, Object value) {
      if (this.schema != null) {
         this.schema.assertValidate(value);
      }

      if (value == null) {
         if ((this.features & JSONReader.Feature.IgnoreSetNullValue.mask) != 0L) {
            return;
         }
      } else {
         if (this.fieldClass.isPrimitive()) {
            this.acceptPrimitive(object, value);
            return;
         }

         if (this.fieldType != this.fieldClass && Map.class.isAssignableFrom(this.fieldClass) && value instanceof Map && this.fieldClass != Map.class) {
            ObjectReader objectReader = this.getObjectReader(JSONFactory.createReadContext());
            value = objectReader.createInstance((Map)value);
         } else if (!this.fieldClass.isInstance(value)) {
            if (value instanceof String) {
               String str = (String)value;
               if (this.fieldClass == LocalDate.class) {
                  if (this.format != null) {
                     value = LocalDate.parse(str, DateTimeFormatter.ofPattern(this.format));
                  } else {
                     value = DateUtils.parseLocalDate(str);
                  }
               } else if (this.fieldClass == Date.class) {
                  if (this.format != null) {
                     value = DateUtils.parseDate(str, this.format, DateUtils.DEFAULT_ZONE_ID);
                  } else {
                     value = DateUtils.parseDate(str);
                  }
               }
            }

            if (!this.fieldClass.isInstance(value)) {
               value = TypeUtils.cast(value, this.fieldType);
            }
         }
      }

      if (this.fieldOffset != -1L) {
         JDKUtils.UNSAFE.putObject(object, this.fieldOffset, value);
      } else {
         try {
            this.field.set(object, value);
         } catch (Exception var4) {
            throw new JSONException("set " + this.fieldName + " error", var4);
         }
      }
   }

   final void acceptPrimitive(T object, Object value) {
      if (this.fieldClass == int.class) {
         if (value instanceof Number) {
            int intValue = ((Number)value).intValue();
            this.accept(object, intValue);
            return;
         }
      } else if (this.fieldClass == long.class) {
         if (value instanceof Number) {
            long longValue = ((Number)value).longValue();
            this.accept(object, longValue);
            return;
         }
      } else if (this.fieldClass == float.class) {
         if (value instanceof Number) {
            float floatValue = ((Number)value).floatValue();
            this.accept(object, floatValue);
            return;
         }
      } else if (this.fieldClass == double.class) {
         if (value instanceof Number) {
            double doubleValue = ((Number)value).doubleValue();
            this.accept(object, doubleValue);
            return;
         }
      } else if (this.fieldClass == short.class) {
         if (value instanceof Number) {
            short shortValue = ((Number)value).shortValue();
            this.accept(object, shortValue);
            return;
         }
      } else if (this.fieldClass == byte.class) {
         if (value instanceof Number) {
            byte byteValue = ((Number)value).byteValue();
            this.accept(object, byteValue);
            return;
         }
      } else if (this.fieldClass == char.class) {
         if (value instanceof Character) {
            char charValue = (Character)value;
            this.accept(object, charValue);
            return;
         }
      } else if (this.fieldClass == boolean.class && value instanceof Boolean) {
         boolean booleanValue = (Boolean)value;
         this.accept(object, booleanValue);
         return;
      }

      throw new JSONException("set " + this.fieldName + " error, type not support " + value.getClass());
   }
}
