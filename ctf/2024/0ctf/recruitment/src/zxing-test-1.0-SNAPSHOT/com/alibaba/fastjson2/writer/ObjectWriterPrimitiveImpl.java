package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import java.lang.reflect.Type;
import java.util.function.Function;

public abstract class ObjectWriterPrimitiveImpl<T> implements ObjectWriter<T> {
   @Override
   public void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      this.writeJSONB(jsonWriter, object, null, null, 0L);
   }

   @Override
   public void writeArrayMapping(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      this.write(jsonWriter, object, null, null, 0L);
   }

   public Function getFunction() {
      return null;
   }
}
