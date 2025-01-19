package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

abstract class FieldWriterBoolean extends FieldWriter {
   final byte[] utf8ValueTrue;
   final byte[] utf8ValueFalse;
   final byte[] utf8Value1;
   final byte[] utf8Value0;
   final char[] utf16ValueTrue;
   final char[] utf16ValueFalse;
   final char[] utf16Value1;
   final char[] utf16Value0;

   FieldWriterBoolean(String name, int ordinal, long features, String format, String label, Type fieldType, Class fieldClass, Field field, Method method) {
      super(name, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      byte[] bytes = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + 4);
      bytes[this.nameWithColonUTF8.length] = 116;
      bytes[this.nameWithColonUTF8.length + 1] = 114;
      bytes[this.nameWithColonUTF8.length + 2] = 117;
      bytes[this.nameWithColonUTF8.length + 3] = 101;
      this.utf8ValueTrue = bytes;
      bytes = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + 5);
      bytes[this.nameWithColonUTF8.length] = 102;
      bytes[this.nameWithColonUTF8.length + 1] = 97;
      bytes[this.nameWithColonUTF8.length + 2] = 108;
      bytes[this.nameWithColonUTF8.length + 3] = 115;
      bytes[this.nameWithColonUTF8.length + 4] = 101;
      this.utf8ValueFalse = bytes;
      bytes = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + 1);
      bytes[this.nameWithColonUTF8.length] = 49;
      this.utf8Value1 = bytes;
      bytes = Arrays.copyOf(this.nameWithColonUTF8, this.nameWithColonUTF8.length + 1);
      bytes[this.nameWithColonUTF8.length] = 48;
      this.utf8Value0 = bytes;
      char[] chars = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + 4);
      chars[this.nameWithColonUTF16.length] = 't';
      chars[this.nameWithColonUTF16.length + 1] = 'r';
      chars[this.nameWithColonUTF16.length + 2] = 'u';
      chars[this.nameWithColonUTF16.length + 3] = 'e';
      this.utf16ValueTrue = chars;
      char[] charsx = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + 5);
      charsx[this.nameWithColonUTF16.length] = 'f';
      charsx[this.nameWithColonUTF16.length + 1] = 'a';
      charsx[this.nameWithColonUTF16.length + 2] = 'l';
      charsx[this.nameWithColonUTF16.length + 3] = 's';
      charsx[this.nameWithColonUTF16.length + 4] = 'e';
      this.utf16ValueFalse = charsx;
      char[] charsxx = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + 1);
      charsxx[this.nameWithColonUTF16.length] = '1';
      this.utf16Value1 = charsxx;
      char[] charsxxx = Arrays.copyOf(this.nameWithColonUTF16, this.nameWithColonUTF16.length + 1);
      charsxxx[this.nameWithColonUTF16.length] = '0';
      this.utf16Value0 = charsxxx;
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, Object object) {
      Boolean value = (Boolean)this.getFieldValue(object);
      if (value == null) {
         jsonWriter.writeNull();
      } else {
         jsonWriter.writeBool(value);
      }
   }

   @Override
   public final void writeBool(JSONWriter jsonWriter, boolean value) {
      long features = jsonWriter.getFeatures(this.features);
      if ((features & JSONWriter.Feature.WriteNonStringValueAsString.mask) != 0L) {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeString(value ? "true" : "false");
      } else if (jsonWriter.utf8) {
         jsonWriter.writeNameRaw(
            (features & JSONWriter.Feature.WriteBooleanAsNumber.mask) != 0L
               ? (value ? this.utf8Value1 : this.utf8Value0)
               : (value ? this.utf8ValueTrue : this.utf8ValueFalse)
         );
      } else if (jsonWriter.utf16) {
         jsonWriter.writeNameRaw(
            (features & JSONWriter.Feature.WriteBooleanAsNumber.mask) != 0L
               ? (value ? this.utf16Value1 : this.utf16Value0)
               : (value ? this.utf16ValueTrue : this.utf16ValueFalse)
         );
      } else {
         this.writeFieldName(jsonWriter);
         jsonWriter.writeBool(value);
      }
   }

   @Override
   public boolean write(JSONWriter jsonWriter, Object object) {
      Boolean value;
      try {
         value = (Boolean)this.getFieldValue(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullBooleanAsFalse.mask))
            == 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeBooleanNull();
            return true;
         }
      } else if (this.fieldClass == boolean.class && !value && (jsonWriter.getFeatures(this.features) & JSONWriter.Feature.NotWriteDefaultValue.mask) != 0L) {
         return false;
      } else {
         this.writeBool(jsonWriter, value);
         return true;
      }
   }

   @Override
   public ObjectWriter getObjectWriter(JSONWriter jsonWriter, Class valueClass) {
      return ObjectWriterImplBoolean.INSTANCE;
   }
}
