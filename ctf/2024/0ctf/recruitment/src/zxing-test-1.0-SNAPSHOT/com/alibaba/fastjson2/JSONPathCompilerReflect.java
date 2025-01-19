package com.alibaba.fastjson2;

import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.util.function.BiFunction;

public class JSONPathCompilerReflect implements JSONFactory.JSONPathCompiler {
   static final JSONPathCompilerReflect INSTANCE = new JSONPathCompilerReflect();

   @Override
   public JSONPath compile(Class objectClass, JSONPath path) {
      if (path instanceof JSONPathSingleName) {
         return this.compileSingleNamePath(objectClass, (JSONPathSingleName)path);
      } else {
         if (path instanceof JSONPathTwoSegment) {
            JSONPathTwoSegment twoSegmentPath = (JSONPathTwoSegment)path;
            JSONPathSegment first = this.compile(objectClass, path, twoSegmentPath.first, null);
            JSONPathSegment segment = this.compile(objectClass, path, twoSegmentPath.second, first);
            if (first != twoSegmentPath.first || segment != twoSegmentPath.second) {
               if (first instanceof JSONPathCompilerReflect.NameSegmentTyped && segment instanceof JSONPathCompilerReflect.NameSegmentTyped) {
                  return new JSONPathCompilerReflect.TwoNameSegmentTypedPath(
                     twoSegmentPath.path, (JSONPathCompilerReflect.NameSegmentTyped)first, (JSONPathCompilerReflect.NameSegmentTyped)segment
                  );
               }

               return new JSONPathTwoSegment(twoSegmentPath.path, first, segment);
            }
         }

         return path;
      }
   }

   protected JSONPath compileSingleNamePath(Class objectClass, JSONPathSingleName path) {
      String fieldName = path.name;
      ObjectReader objectReader = path.getReaderContext().getObjectReader(objectClass);
      FieldReader fieldReader = objectReader.getFieldReader(fieldName);
      ObjectWriter objectWriter = path.getWriterContext().getObjectWriter(objectClass);
      FieldWriter fieldWriter = objectWriter.getFieldWriter(fieldName);
      return new JSONPathCompilerReflect.SingleNamePathTyped(path.path, objectClass, objectReader, fieldReader, objectWriter, fieldWriter);
   }

   protected JSONPathSegment compile(Class objectClass, JSONPath path, JSONPathSegment segment, JSONPathSegment parent) {
      if (segment instanceof JSONPathSegmentName) {
         JSONPathSegmentName nameSegment = (JSONPathSegmentName)segment;
         String fieldName = nameSegment.name;
         JSONReader.Context readerContext = path.getReaderContext();
         JSONWriter.Context writerContext = path.getWriterContext();
         ObjectReader objectReader = null;
         FieldReader fieldReader = null;
         if (parent == null) {
            objectReader = readerContext.getObjectReader(objectClass);
         } else if (parent instanceof JSONPathCompilerReflect.NameSegmentTyped) {
            JSONPathCompilerReflect.NameSegmentTyped nameSegmentTyped = (JSONPathCompilerReflect.NameSegmentTyped)parent;
            if (nameSegmentTyped.fieldReader != null) {
               objectReader = readerContext.getObjectReader(nameSegmentTyped.fieldReader.fieldType);
            }
         }

         if (objectReader != null) {
            fieldReader = objectReader.getFieldReader(fieldName);
         }

         ObjectWriter objectWriter = null;
         FieldWriter fieldWriter = null;
         if (parent == null) {
            objectWriter = writerContext.getObjectWriter(objectClass);
         } else if (parent instanceof JSONPathCompilerReflect.NameSegmentTyped) {
            JSONPathCompilerReflect.NameSegmentTyped nameSegmentTyped = (JSONPathCompilerReflect.NameSegmentTyped)parent;
            if (nameSegmentTyped.fieldWriter != null) {
               objectWriter = writerContext.getObjectWriter(nameSegmentTyped.fieldWriter.fieldClass);
            }
         }

         if (objectWriter != null) {
            fieldWriter = objectWriter.getFieldWriter(fieldName);
         }

         return new JSONPathCompilerReflect.NameSegmentTyped(
            objectClass, objectReader, fieldReader, objectWriter, fieldWriter, fieldName, nameSegment.nameHashCode
         );
      } else {
         return segment;
      }
   }

   public static class NameSegmentTyped extends JSONPathSegmentName {
      final Class objectClass;
      final FieldReader fieldReader;
      final FieldWriter fieldWriter;

      public NameSegmentTyped(
         Class objectClass,
         ObjectReader objectReader,
         FieldReader fieldReader,
         ObjectWriter objectWriter,
         FieldWriter fieldWriter,
         String name,
         long nameHashCode
      ) {
         super(name, nameHashCode);
         this.objectClass = objectClass;
         this.fieldReader = fieldReader;
         this.fieldWriter = fieldWriter;
      }

      @Override
      public void eval(JSONPath.Context context) {
         if (this.fieldWriter == null) {
            throw new UnsupportedOperationException();
         } else {
            Object object = context.parent == null ? context.root : context.parent.value;
            if (object != null) {
               context.value = this.fieldWriter.getFieldValue(object);
            }
         }
      }
   }

   public static class SingleNamePathTyped extends JSONPath {
      final Class objectClass;
      final ObjectReader objectReader;
      final FieldReader fieldReader;
      final ObjectWriter objectWriter;
      final FieldWriter fieldWriter;

      public SingleNamePathTyped(
         String path, Class objectClass, ObjectReader objectReader, FieldReader fieldReader, ObjectWriter objectWriter, FieldWriter fieldWriter
      ) {
         super(path);
         this.objectClass = objectClass;
         this.objectReader = objectReader;
         this.fieldReader = fieldReader;
         this.objectWriter = objectWriter;
         this.fieldWriter = fieldWriter;
      }

      @Override
      public JSONPath getParent() {
         return null;
      }

      @Override
      public boolean isRef() {
         return true;
      }

      @Override
      public boolean contains(Object rootObject) {
         return this.fieldWriter != null && this.fieldWriter.getFieldValue(rootObject) != null;
      }

      @Override
      public Object eval(Object object) {
         if (this.fieldWriter == null) {
            throw new UnsupportedOperationException();
         } else {
            return this.fieldWriter.getFieldValue(object);
         }
      }

      @Override
      public Object extract(JSONReader jsonReader) {
         throw new UnsupportedOperationException();
      }

      @Override
      public String extractScalar(JSONReader jsonReader) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void set(Object rootObject, Object value) {
         if (this.fieldReader == null) {
            throw new UnsupportedOperationException();
         } else {
            this.fieldReader.accept(rootObject, value);
         }
      }

      @Override
      public void set(Object rootObject, Object value, JSONReader.Feature... readerFeatures) {
         if (this.fieldReader == null) {
            throw new UnsupportedOperationException();
         } else {
            this.fieldReader.accept(rootObject, value);
         }
      }

      @Override
      public void setCallback(Object rootObject, BiFunction callback) {
         if (this.fieldWriter == null) {
            throw new UnsupportedOperationException();
         } else {
            Object fieldValue = this.fieldWriter.getFieldValue(rootObject);
            Object fieldValueApply = callback.apply(rootObject, fieldValue);
            if (fieldValueApply != fieldValue) {
               if (this.fieldReader == null) {
                  throw new UnsupportedOperationException();
               } else {
                  this.fieldReader.accept(rootObject, fieldValueApply);
               }
            }
         }
      }

      @Override
      public void setInt(Object rootObject, int value) {
         if (this.fieldReader == null) {
            throw new UnsupportedOperationException();
         } else {
            this.fieldReader.accept(rootObject, value);
         }
      }

      @Override
      public void setLong(Object rootObject, long value) {
         if (this.fieldReader == null) {
            throw new UnsupportedOperationException();
         } else {
            this.fieldReader.accept(rootObject, value);
         }
      }

      @Override
      public boolean remove(Object rootObject) {
         throw new UnsupportedOperationException();
      }
   }

   public static class TwoNameSegmentTypedPath extends JSONPathTwoSegment {
      final JSONPathCompilerReflect.NameSegmentTyped first;
      final JSONPathCompilerReflect.NameSegmentTyped second;

      public TwoNameSegmentTypedPath(String path, JSONPathCompilerReflect.NameSegmentTyped first, JSONPathCompilerReflect.NameSegmentTyped second) {
         super(path, first, second);
         this.first = first;
         this.second = second;
      }

      @Override
      public Object eval(Object root) {
         Object firstValue = this.first.fieldWriter.getFieldValue(root);
         return firstValue == null ? null : this.second.fieldWriter.getFieldValue(firstValue);
      }

      @Override
      public void set(Object root, Object value) {
         Object firstValue = this.first.fieldWriter.getFieldValue(root);
         if (firstValue != null) {
            this.second.fieldReader.accept(firstValue, value);
         }
      }

      @Override
      public void setInt(Object root, int value) {
         Object firstValue = this.first.fieldWriter.getFieldValue(root);
         if (firstValue != null) {
            this.second.fieldReader.accept(firstValue, value);
         }
      }

      @Override
      public void setLong(Object root, long value) {
         Object firstValue = this.first.fieldWriter.getFieldValue(root);
         if (firstValue != null) {
            this.second.fieldReader.accept(firstValue, value);
         }
      }

      @Override
      public void setCallback(Object root, BiFunction callback) {
         Object firstValue = this.first.fieldWriter.getFieldValue(root);
         if (firstValue != null) {
            Object secondValue = this.second.fieldWriter.getFieldValue(firstValue);
            Object secondValueApply = callback.apply(firstValue, secondValue);
            if (secondValueApply != secondValue) {
               if (this.second.fieldReader == null) {
                  throw new UnsupportedOperationException();
               } else {
                  this.second.fieldReader.accept(firstValue, secondValueApply);
               }
            }
         }
      }
   }
}
