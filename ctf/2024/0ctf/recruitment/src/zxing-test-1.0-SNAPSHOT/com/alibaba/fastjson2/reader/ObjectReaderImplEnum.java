package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public final class ObjectReaderImplEnum implements ObjectReader {
   final Method createMethod;
   final Type createMethodParamType;
   final Member valueField;
   final Type valueFieldType;
   final Class enumClass;
   final long typeNameHash;
   private final Enum[] enums;
   private final Enum[] ordinalEnums;
   private final long[] enumNameHashCodes;
   private String[] stringValues;
   private long[] intValues;

   public ObjectReaderImplEnum(Class enumClass, Method createMethod, Member valueField, Enum[] enums, Enum[] ordinalEnums, long[] enumNameHashCodes) {
      this.enumClass = enumClass;
      this.createMethod = createMethod;
      if (valueField instanceof AccessibleObject) {
         ((AccessibleObject)valueField).setAccessible(true);
      }

      this.valueField = valueField;
      Type valueFieldType = null;
      if (valueField instanceof Field) {
         valueFieldType = ((Field)valueField).getType();
      } else if (valueField instanceof Method) {
         valueFieldType = ((Method)valueField).getReturnType();
      }

      this.valueFieldType = valueFieldType;
      if (valueFieldType != null) {
         if (valueFieldType == String.class) {
            this.stringValues = new String[enums.length];
         } else {
            this.intValues = new long[enums.length];
         }

         for (int i = 0; i < enums.length; i++) {
            Enum e = enums[i];

            try {
               Object fieldValue;
               if (valueField instanceof Field) {
                  fieldValue = ((Field)valueField).get(e);
               } else {
                  fieldValue = ((Method)valueField).invoke(e);
               }

               if (valueFieldType == String.class) {
                  this.stringValues[i] = (String)fieldValue;
               } else if (fieldValue instanceof Number) {
                  this.intValues[i] = ((Number)fieldValue).longValue();
               }
            } catch (Exception var11) {
            }
         }
      }

      Type createMethodParamType = null;
      if (createMethod != null && createMethod.getParameterCount() == 1) {
         createMethodParamType = createMethod.getParameterTypes()[0];
      }

      this.createMethodParamType = createMethodParamType;
      this.typeNameHash = Fnv.hashCode64(TypeUtils.getTypeName(enumClass));
      this.enums = enums;
      this.ordinalEnums = ordinalEnums;
      this.enumNameHashCodes = enumNameHashCodes;
   }

   @Override
   public Class getObjectClass() {
      return this.enumClass;
   }

   public Enum getEnumByHashCode(long hashCode) {
      if (this.enums == null) {
         return null;
      } else {
         int enumIndex = Arrays.binarySearch(this.enumNameHashCodes, hashCode);
         return enumIndex < 0 ? null : this.enums[enumIndex];
      }
   }

   public Enum getEnum(String name) {
      return name == null ? null : this.getEnumByHashCode(Fnv.hashCode64(name));
   }

   public Enum getEnumByOrdinal(int ordinal) {
      if (ordinal >= 0 && ordinal < this.ordinalEnums.length) {
         return this.ordinalEnums[ordinal];
      } else {
         throw new JSONException("No enum ordinal " + this.enumClass.getCanonicalName() + "." + ordinal);
      }
   }

   public Enum of(int intValue) {
      Enum enumValue = null;
      if (this.valueField == null) {
         if (intValue >= 0 && intValue < this.ordinalEnums.length) {
            enumValue = this.ordinalEnums[intValue];
         }

         return enumValue;
      } else {
         try {
            if (this.valueField instanceof Field) {
               for (int i = 0; i < this.enums.length; i++) {
                  Enum e = this.enums[i];
                  if (((Field)this.valueField).getInt(e) == intValue) {
                     enumValue = e;
                     break;
                  }
               }
            } else {
               Method valueMethod = (Method)this.valueField;

               for (int ix = 0; ix < this.enums.length; ix++) {
                  Enum e = this.enums[ix];
                  if (((Number)valueMethod.invoke(e)).intValue() == intValue) {
                     enumValue = e;
                     break;
                  }
               }
            }
         } catch (Exception var6) {
            throw new JSONException("parse enum error, class " + this.enumClass.getName() + ", value " + intValue, var6);
         }

         if (enumValue == null) {
            throw new JSONException("None enum ordinal or value " + intValue);
         } else {
            return enumValue;
         }
      }
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int start = jsonReader.getOffset();
      byte type = jsonReader.getType();
      if (type == -110) {
         ObjectReader autoTypeObjectReader = jsonReader.checkAutoType(this.enumClass, 0L, features);
         if (autoTypeObjectReader != null) {
            if (autoTypeObjectReader != this) {
               return autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, features);
            }
         } else if (jsonReader.isEnabled(JSONReader.Feature.ErrorOnNotSupportAutoType)) {
            throw new JSONException(jsonReader.info("not support enumType : " + jsonReader.getString()));
         }
      }

      boolean isInt = type >= -16 && type <= 72;
      Enum fieldValue;
      if (isInt) {
         int ordinal;
         if (type <= 47) {
            ordinal = type;
            jsonReader.next();
         } else {
            ordinal = jsonReader.readInt32Value();
         }

         if (ordinal < 0 || ordinal >= this.ordinalEnums.length) {
            throw new JSONException("No enum ordinal " + this.enumClass.getCanonicalName() + "." + ordinal);
         }

         fieldValue = this.ordinalEnums[ordinal];
      } else {
         if (jsonReader.nextIfNullOrEmptyString()) {
            return null;
         }

         fieldValue = this.getEnumByHashCode(jsonReader.readValueHashCode());
         if (fieldValue == null) {
            long nameHash = jsonReader.getNameHashCodeLCase();
            fieldValue = this.getEnumByHashCode(nameHash);
         }
      }

      if (fieldValue == null && jsonReader.getOffset() == start) {
         this.oomCheck(fieldType);
      }

      return fieldValue;
   }

   private void oomCheck(Type fieldType) {
      if (fieldType instanceof ParameterizedType) {
         Type rawType = ((ParameterizedType)fieldType).getRawType();
         if (List.class.isAssignableFrom((Class<?>)rawType)) {
            throw new JSONException(
               this.getClass().getSimpleName() + " parses error, JSONReader not forward when field type belongs to collection to avoid OOM"
            );
         }
      }
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      int start = jsonReader.getOffset();
      if (this.createMethodParamType != null) {
         Object paramValue = jsonReader.read(this.createMethodParamType);

         try {
            return this.createMethod.invoke(null, paramValue);
         } catch (InvocationTargetException | IllegalAccessException var12) {
            throw new JSONException(jsonReader.info("create enum error, enumClass " + this.enumClass.getName() + ", paramValue " + paramValue), var12);
         }
      } else {
         Enum<?> fieldValue = null;
         if (jsonReader.isInt()) {
            int intValue = jsonReader.readInt32Value();
            if (this.valueField == null) {
               if (intValue < 0 || intValue >= this.ordinalEnums.length) {
                  throw new JSONException("No enum ordinal " + this.enumClass.getCanonicalName() + "." + intValue);
               }

               fieldValue = this.ordinalEnums[intValue];
            } else {
               if (this.intValues != null) {
                  for (int i = 0; i < this.intValues.length; i++) {
                     if (this.intValues[i] == (long)intValue) {
                        fieldValue = this.enums[i];
                        break;
                     }
                  }
               }

               if (fieldValue == null && jsonReader.isEnabled(JSONReader.Feature.ErrorOnEnumNotMatch)) {
                  throw new JSONException(
                     jsonReader.info("parse enum error, class " + this.enumClass.getName() + ", " + this.valueField.getName() + " " + intValue)
                  );
               }
            }
         } else if (!jsonReader.nextIfNullOrEmptyString()) {
            if (this.stringValues != null && jsonReader.isString()) {
               String str = jsonReader.readString();

               for (int ix = 0; ix < this.stringValues.length; ix++) {
                  if (str.equals(this.stringValues[ix])) {
                     fieldValue = this.enums[ix];
                     break;
                  }
               }

               if (fieldValue == null && this.valueField != null) {
                  try {
                     fieldValue = Enum.valueOf(this.enumClass, str);
                  } catch (IllegalArgumentException var13) {
                  }
               }
            } else if (this.intValues != null && jsonReader.isInt()) {
               int intValue = jsonReader.readInt32Value();

               for (int ixx = 0; ixx < this.intValues.length; ixx++) {
                  if (this.intValues[ixx] == (long)intValue) {
                     fieldValue = this.enums[ixx];
                     break;
                  }
               }
            } else {
               long hashCode = jsonReader.readValueHashCode();
               if (hashCode == -3750763034362895579L) {
                  return null;
               }

               fieldValue = this.getEnumByHashCode(hashCode);
               if (fieldValue == null) {
                  fieldValue = this.getEnumByHashCode(jsonReader.getNameHashCodeLCase());
               }

               if (fieldValue == null) {
                  String str = jsonReader.getString();
                  if (TypeUtils.isInteger(str)) {
                     int ordinal = Integer.parseInt(str);
                     if (ordinal >= 0 && ordinal < this.ordinalEnums.length) {
                        fieldValue = this.ordinalEnums[ordinal];
                     }
                  }
               }
            }

            if (fieldValue == null && jsonReader.isEnabled(JSONReader.Feature.ErrorOnEnumNotMatch)) {
               String strVal = jsonReader.getString();
               throw new JSONException(jsonReader.info("parse enum error, class " + this.enumClass.getName() + ", value " + strVal));
            }
         }

         if (fieldValue == null && jsonReader.getOffset() == start) {
            this.oomCheck(fieldType);
         }

         return fieldValue;
      }
   }
}
