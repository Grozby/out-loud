package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

final class ObjectWriterImplEnum<E extends Enum<E>> extends ObjectWriterPrimitiveImpl {
   final Member valueField;
   final Class defineClass;
   final Class enumType;
   final long features;
   byte[] typeNameJSONB;
   long typeNameHash;
   final Enum[] enumConstants;
   final String[] names;
   final long[] hashCodes;
   byte[][] jsonbNames;
   final String[] annotationNames;

   public ObjectWriterImplEnum(Class defineClass, Class enumType, Member valueField, String[] annotationNames, long features) {
      this.defineClass = defineClass;
      this.enumType = enumType;
      this.features = features;
      this.valueField = valueField;
      if (valueField instanceof AccessibleObject) {
         try {
            ((AccessibleObject)valueField).setAccessible(true);
         } catch (Throwable var9) {
         }
      }

      this.enumConstants = (Enum[])enumType.getEnumConstants();
      this.names = new String[this.enumConstants.length];
      this.hashCodes = new long[this.enumConstants.length];

      for (int i = 0; i < this.enumConstants.length; i++) {
         String name = this.enumConstants[i].name();
         this.names[i] = name;
         this.hashCodes[i] = Fnv.hashCode64(name);
      }

      this.annotationNames = annotationNames;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
         if (this.typeNameJSONB == null) {
            String typeName = TypeUtils.getTypeName(this.enumType);
            this.typeNameJSONB = JSONB.toBytes(typeName);
            this.typeNameHash = Fnv.hashCode64(typeName);
         }

         jsonWriter.writeTypeName(this.typeNameJSONB, this.typeNameHash);
      }

      Enum e = (Enum)object;
      if (jsonWriter.isEnabled(JSONWriter.Feature.WriteEnumUsingToString)) {
         jsonWriter.writeString(e.toString());
      } else {
         if (this.jsonbNames == null) {
            this.jsonbNames = new byte[this.names.length][];
         }

         int ordinal = e.ordinal();
         byte[] jsonbName = this.jsonbNames[ordinal];
         if (jsonbName == null) {
            jsonbName = JSONB.toBytes(this.names[ordinal]);
            this.jsonbNames[ordinal] = jsonbName;
         }

         jsonWriter.writeRaw(jsonbName);
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      Enum e = (Enum)object;
      if (e == null) {
         jsonWriter.writeNull();
      } else {
         if (this.valueField != null) {
            try {
               Object fieldValue;
               if (this.valueField instanceof Field) {
                  fieldValue = ((Field)this.valueField).get(object);
               } else {
                  fieldValue = ((Method)this.valueField).invoke(object);
               }

               if (fieldValue != object) {
                  jsonWriter.writeAny(fieldValue);
                  return;
               }
            } catch (Exception var12) {
               throw new JSONException("getEnumValue error", var12);
            }
         }

         long features2 = jsonWriter.getFeatures(features | this.features);
         if ((features2 & JSONWriter.Feature.WriteEnumUsingToString.mask) != 0L) {
            jsonWriter.writeString(e.toString());
         } else if ((features2 & JSONWriter.Feature.WriteEnumUsingOrdinal.mask) != 0L) {
            jsonWriter.writeInt32(e.ordinal());
         } else {
            String enumName = null;
            if (this.annotationNames != null) {
               int ordinal = e.ordinal();
               if (ordinal < this.annotationNames.length) {
                  enumName = this.annotationNames[ordinal];
               }
            }

            if (enumName == null) {
               enumName = e.name();
            }

            jsonWriter.writeString(enumName);
         }
      }
   }
}
