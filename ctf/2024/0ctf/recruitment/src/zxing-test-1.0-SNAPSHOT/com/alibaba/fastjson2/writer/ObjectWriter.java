package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.alibaba.fastjson2.filter.NameFilter;
import com.alibaba.fastjson2.filter.PropertyFilter;
import com.alibaba.fastjson2.filter.PropertyPreFilter;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public interface ObjectWriter<T> {
   default long getFeatures() {
      return 0L;
   }

   default List<FieldWriter> getFieldWriters() {
      return Collections.emptyList();
   }

   default FieldWriter getFieldWriter(long hashCode) {
      return null;
   }

   default Object getFieldValue(Object object, String fieldName) {
      FieldWriter fieldWriter = this.getFieldWriter(fieldName);
      return fieldWriter == null ? null : fieldWriter.getFieldValue(object);
   }

   default FieldWriter getFieldWriter(String name) {
      long nameHash = Fnv.hashCode64(name);
      FieldWriter fieldWriter = this.getFieldWriter(nameHash);
      if (fieldWriter == null) {
         long nameHashLCase = Fnv.hashCode64LCase(name);
         if (nameHashLCase != nameHash) {
            fieldWriter = this.getFieldWriter(nameHashLCase);
         }
      }

      return fieldWriter;
   }

   default boolean writeTypeInfo(JSONWriter jsonWriter) {
      return false;
   }

   default void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      this.write(jsonWriter, object, fieldName, fieldType, features);
   }

   default void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object) {
      this.writeArrayMappingJSONB(jsonWriter, object, null, null, 0L);
   }

   default void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      List<FieldWriter> fieldWriters = this.getFieldWriters();
      int size = fieldWriters.size();
      jsonWriter.startArray(size);

      for (int i = 0; i < size; i++) {
         FieldWriter fieldWriter = fieldWriters.get(i);
         fieldWriter.writeValue(jsonWriter, object);
      }
   }

   default void writeArrayMapping(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (jsonWriter.jsonb) {
         this.writeArrayMappingJSONB(jsonWriter, object, fieldName, fieldType, features);
      } else {
         List<FieldWriter> fieldWriters = this.getFieldWriters();
         jsonWriter.startArray();
         boolean hasFilter = this.hasFilter(jsonWriter);
         if (!hasFilter) {
            int i = 0;

            for (int size = fieldWriters.size(); i < size; i++) {
               if (i != 0) {
                  jsonWriter.writeComma();
               }

               FieldWriter fieldWriter = fieldWriters.get(i);
               fieldWriter.writeValue(jsonWriter, object);
            }
         } else {
            JSONWriter.Context ctx = jsonWriter.context;
            PropertyPreFilter propertyPreFilter = ctx.getPropertyPreFilter();
            ValueFilter valueFilter = ctx.getValueFilter();
            PropertyFilter propertyFilter = ctx.getPropertyFilter();
            int i = 0;

            for (int size = fieldWriters.size(); i < size; i++) {
               if (i != 0) {
                  jsonWriter.writeComma();
               }

               FieldWriter fieldWriter = fieldWriters.get(i);
               if (propertyPreFilter != null && !propertyPreFilter.process(jsonWriter, object, fieldWriter.fieldName)) {
                  jsonWriter.writeNull();
               } else {
                  Object fieldValue = fieldWriter.getFieldValue(object);
                  if (propertyFilter != null && !propertyFilter.apply(object, fieldWriter.fieldName, fieldValue)) {
                     jsonWriter.writeNull();
                  } else if (valueFilter != null) {
                     Object processValue = valueFilter.apply(object, fieldWriter.fieldName, fieldValue);
                     if (processValue == null) {
                        jsonWriter.writeNull();
                     } else {
                        ObjectWriter processValueWriter = fieldWriter.getObjectWriter(jsonWriter, processValue.getClass());
                        processValueWriter.write(jsonWriter, fieldValue);
                     }
                  } else if (fieldValue == null) {
                     jsonWriter.writeNull();
                  } else {
                     ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, fieldValue.getClass());
                     fieldValueWriter.write(jsonWriter, fieldValue);
                  }
               }
            }
         }

         jsonWriter.endArray();
      }
   }

   default boolean hasFilter(JSONWriter jsonWriter) {
      return jsonWriter.hasFilter(JSONWriter.Feature.IgnoreNonFieldGetter.mask);
   }

   default void write(JSONWriter jsonWriter, Object object) {
      this.write(jsonWriter, object, null, null, 0L);
   }

   default String toJSONString(T object, JSONWriter.Feature... features) {
      JSONWriter jsonWriter = JSONWriter.of(features);

      String var4;
      try {
         this.write(jsonWriter, object, null, null, 0L);
         var4 = jsonWriter.toString();
      } catch (Throwable var7) {
         if (jsonWriter != null) {
            try {
               jsonWriter.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (jsonWriter != null) {
         jsonWriter.close();
      }

      return var4;
   }

   void write(JSONWriter var1, Object var2, Object var3, Type var4, long var5);

   default void writeWithFilter(JSONWriter jsonWriter, Object object) {
      this.writeWithFilter(jsonWriter, object, null, null, 0L);
   }

   default void writeWithFilter(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      throw new UnsupportedOperationException();
   }

   default void setPropertyFilter(PropertyFilter propertyFilter) {
   }

   default void setValueFilter(ValueFilter valueFilter) {
   }

   default void setNameFilter(NameFilter nameFilter) {
   }

   default void setPropertyPreFilter(PropertyPreFilter propertyPreFilter) {
   }

   default void setFilter(Filter filter) {
      if (filter instanceof PropertyFilter) {
         this.setPropertyFilter((PropertyFilter)filter);
      }

      if (filter instanceof ValueFilter) {
         this.setValueFilter((ValueFilter)filter);
      }

      if (filter instanceof NameFilter) {
         this.setNameFilter((NameFilter)filter);
      }

      if (filter instanceof PropertyPreFilter) {
         this.setPropertyPreFilter((PropertyPreFilter)filter);
      }
   }
}
