package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

final class FieldWriterListStrFunc<T> extends FieldWriter<T> {
   final Function<T, List> function;

   FieldWriterListStrFunc(
      String fieldName,
      int ordinal,
      long features,
      String format,
      String label,
      Field field,
      Method method,
      Function<T, List> function,
      Type fieldType,
      Class fieldClass
   ) {
      super(fieldName, ordinal, features, format, null, label, fieldType, fieldClass, field, method);
      this.function = function;
   }

   @Override
   public Object getFieldValue(T object) {
      return this.function.apply(object);
   }

   @Override
   public boolean write(JSONWriter jsonWriter, T object) {
      List list;
      try {
         list = this.function.apply(object);
      } catch (RuntimeException var9) {
         if (jsonWriter.isIgnoreErrorGetter()) {
            return false;
         }

         throw var9;
      }

      long features = this.features | jsonWriter.getFeatures();
      if (list == null) {
         if ((features & (JSONWriter.Feature.WriteNulls.mask | JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullListAsEmpty.mask))
            != 0L) {
            this.writeFieldName(jsonWriter);
            jsonWriter.writeArrayNull();
            return true;
         } else {
            return false;
         }
      } else if ((features & JSONWriter.Feature.NotWriteEmptyArray.mask) != 0L && list.isEmpty()) {
         return false;
      } else {
         this.writeFieldName(jsonWriter);
         if (jsonWriter.jsonb) {
            int size = list.size();
            jsonWriter.startArray(size);

            for (int i = 0; i < size; i++) {
               String item = (String)list.get(i);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  jsonWriter.writeString(item);
               }
            }

            return true;
         } else {
            jsonWriter.startArray();

            for (int ix = 0; ix < list.size(); ix++) {
               if (ix != 0) {
                  jsonWriter.writeComma();
               }

               String item = (String)list.get(ix);
               if (item == null) {
                  jsonWriter.writeNull();
               } else {
                  jsonWriter.writeString(item);
               }
            }

            jsonWriter.endArray();
            return true;
         }
      }
   }

   @Override
   public void writeValue(JSONWriter jsonWriter, T object) {
      List list = this.function.apply(object);
      if (list == null) {
         jsonWriter.writeNull();
      } else if (jsonWriter.jsonb) {
         int size = list.size();
         jsonWriter.startArray(size);

         for (int i = 0; i < size; i++) {
            String item = (String)list.get(i);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               jsonWriter.writeString(item);
            }
         }
      } else {
         jsonWriter.startArray();

         for (int ix = 0; ix < list.size(); ix++) {
            if (ix != 0) {
               jsonWriter.writeComma();
            }

            String item = (String)list.get(ix);
            if (item == null) {
               jsonWriter.writeNull();
            } else {
               jsonWriter.writeString(item);
            }
         }

         jsonWriter.endArray();
      }
   }
}
