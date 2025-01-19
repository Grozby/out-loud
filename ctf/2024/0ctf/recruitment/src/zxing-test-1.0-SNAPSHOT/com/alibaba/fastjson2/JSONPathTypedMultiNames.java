package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;

class JSONPathTypedMultiNames extends JSONPathTypedMulti {
   final JSONPath prefix;
   final JSONPath[] namePaths;
   final String[] names;
   final FieldReader[] fieldReaders;
   final ObjectReaderAdapter<Object[]> objectReader;

   JSONPathTypedMultiNames(
      JSONPath[] paths, JSONPath prefix, JSONPath[] namePaths, Type[] types, String[] formats, long[] pathFeatures, ZoneId zoneId, long features
   ) {
      super(paths, types, formats, pathFeatures, zoneId, features);
      this.prefix = prefix;
      this.namePaths = namePaths;
      this.names = new String[paths.length];

      for (int i = 0; i < paths.length; i++) {
         JSONPathSingleName jsonPathSingleName = (JSONPathSingleName)namePaths[i];
         String fieldName = jsonPathSingleName.name;
         this.names[i] = fieldName;
      }

      long[] fieldReaderFeatures = new long[this.names.length];
      if (pathFeatures != null) {
         for (int i = 0; i < pathFeatures.length; i++) {
            if ((pathFeatures[i] & JSONPath.Feature.NullOnError.mask) != 0L) {
               fieldReaderFeatures[i] |= JSONReader.Feature.NullOnError.mask;
            }
         }
      }

      Type[] fieldTypes = (Type[])types.clone();

      for (int ix = 0; ix < fieldTypes.length; ix++) {
         Type fieldType = fieldTypes[ix];
         if (fieldType == boolean.class) {
            fieldTypes[ix] = Boolean.class;
         } else if (fieldType == char.class) {
            fieldTypes[ix] = Character.class;
         } else if (fieldType == byte.class) {
            fieldTypes[ix] = Byte.class;
         } else if (fieldType == short.class) {
            fieldTypes[ix] = Short.class;
         } else if (fieldType == int.class) {
            fieldTypes[ix] = Integer.class;
         } else if (fieldType == long.class) {
            fieldTypes[ix] = Long.class;
         } else if (fieldType == float.class) {
            fieldTypes[ix] = Float.class;
         } else if (fieldType == double.class) {
            fieldTypes[ix] = Double.class;
         }
      }

      int length = this.names.length;
      this.objectReader = (ObjectReaderAdapter<Object[]>)JSONFactory.getDefaultObjectReaderProvider()
         .<Object[]>createObjectReader(this.names, fieldTypes, fieldReaderFeatures, () -> new Object[length], (o, ixx, v) -> o[ixx] = v);
      this.fieldReaders = this.objectReader.getFieldReaders();
   }

   @Override
   public boolean isRef() {
      return true;
   }

   @Override
   public Object eval(Object root) {
      Object[] array = new Object[this.paths.length];
      Object object = root;
      if (this.prefix != null) {
         object = this.prefix.eval(root);
      }

      if (object == null) {
         return new Object[this.paths.length];
      } else if (object instanceof Map) {
         return this.objectReader.createInstance((Map)object, 0L);
      } else {
         ObjectWriter objectReader = JSONFactory.defaultObjectWriterProvider.getObjectWriter(object.getClass());

         for (int i = 0; i < this.names.length; i++) {
            FieldWriter fieldWriter = objectReader.getFieldWriter(this.names[i]);
            if (fieldWriter != null) {
               Object result = fieldWriter.getFieldValue(object);
               Type type = this.types[i];
               if (result != null && result.getClass() != type) {
                  if (type == Long.class) {
                     result = TypeUtils.toLong(result);
                  } else if (type == BigDecimal.class) {
                     result = TypeUtils.toBigDecimal(result);
                  } else if (type == String[].class) {
                     result = TypeUtils.toStringArray(result);
                  } else {
                     result = TypeUtils.cast(result, type);
                  }
               }

               array[i] = result;
            }
         }

         return array;
      }
   }

   @Override
   public Object extract(JSONReader jsonReader) {
      if (this.prefix != null) {
         Object object = jsonReader.readAny();
         return this.eval(object);
      } else if (jsonReader.nextIfNull()) {
         return new Object[this.paths.length];
      } else if (!jsonReader.nextIfObjectStart()) {
         throw new JSONException(jsonReader.info("illegal input, expect '[', but " + jsonReader.current()));
      } else {
         return this.objectReader.readObject(jsonReader, null, null, 0L);
      }
   }
}
