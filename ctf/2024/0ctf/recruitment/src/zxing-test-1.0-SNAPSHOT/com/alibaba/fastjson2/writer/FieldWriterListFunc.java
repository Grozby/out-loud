package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

final class FieldWriterListFunc<T> extends FieldWriterList<T> {
   final Function<T, List> function;

   FieldWriterListFunc(
      String fieldName,
      int ordinal,
      long features,
      String format,
      String label,
      Type itemType,
      Field field,
      Method method,
      Function<T, List> function,
      Type fieldType,
      Class fieldClass
   ) {
      super(fieldName, itemType, ordinal, features, format, label, fieldType, fieldClass, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      List value;
      try {
         value = this.function.apply(object);
      } catch (RuntimeException var6) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var6;
      }

      if (value == null) {
         long features = this.features | jsonWriter.getFeatures();
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask))
            == 0L) {
            return false;
         } else {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
            return true;
         }
      } else if ((this.features & JSONWriter.Feature.NotWriteEmptyArray.mask) != 0L && value.isEmpty()) {
         return false;
      } else {
         String refPath = jsonWriter.setPath(this, value);
         if (refPath != null) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeReference(refPath);
            jsonWriter.popPath(value);
            return true;
         } else {
            if (this.itemType == String.class) {
               this.writeListStr(jsonWriter, true, value);
            } else {
               this.writeList(jsonWriter, value);
            }

            jsonWriter.popPath(value);
            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      List list = this.function.apply(object);
      if (list == null) {
         jsonWriter.writeNull();
      } else {
         Class previousClass = null;
         ObjectWriter previousObjectWriter = null;
         if (jsonWriter.jsonb) {
            int size = list.size();
            jsonWriter.startArray(size);

            for (int i = 0; i < size; i++) {
               Object item = list.get(i);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> itemClass = item.getClass();
                  ObjectWriter itemObjectWriter;
                  if (itemClass == previousClass) {
                     itemObjectWriter = previousObjectWriter;
                  } else {
                     itemObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                     previousClass = itemClass;
                     previousObjectWriter = itemObjectWriter;
                  }

                  itemObjectWriter.write(jsonWriter, item);
               }
            }
         } else {
            jsonWriter.startArray();

            for (int ix = 0; ix < list.size(); ix++) {
               if (ix != 0) {
                  jsonWriter.writeComma();
               }

               Object item = list.get(ix);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  Class<?> itemClass = item.getClass();
                  ObjectWriter itemObjectWriter;
                  if (itemClass == previousClass) {
                     itemObjectWriter = previousObjectWriter;
                  } else {
                     itemObjectWriter = this.getItemWriter(jsonWriter, itemClass);
                     previousClass = itemClass;
                     previousObjectWriter = itemObjectWriter;
                  }

                  itemObjectWriter.write(jsonWriter, item);
               }
            }

            jsonWriter.endArray();
         }
      }
   }

   @Override
   public Function getFunction() {
      return this.function;
   }
}
