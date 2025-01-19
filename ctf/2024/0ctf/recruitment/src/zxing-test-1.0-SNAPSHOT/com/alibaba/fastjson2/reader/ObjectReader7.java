package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.schema.JSONSchema;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectReader7<T> extends ObjectReaderAdapter<T> {
   protected final FieldReader fieldReader0;
   protected final FieldReader fieldReader1;
   protected final FieldReader fieldReader2;
   protected final FieldReader fieldReader3;
   protected final FieldReader fieldReader4;
   protected final FieldReader fieldReader5;
   protected final FieldReader fieldReader6;
   final long hashCode0;
   final long hashCode1;
   final long hashCode2;
   final long hashCode3;
   final long hashCode4;
   final long hashCode5;
   final long hashCode6;
   final long hashCode0LCase;
   final long hashCode1LCase;
   final long hashCode2LCase;
   final long hashCode3LCase;
   final long hashCode4LCase;
   final long hashCode5LCase;
   final long hashCode6LCase;
   protected ObjectReader objectReader0;
   protected ObjectReader objectReader1;
   protected ObjectReader objectReader2;
   protected ObjectReader objectReader3;
   protected ObjectReader objectReader4;
   protected ObjectReader objectReader5;
   protected ObjectReader objectReader6;

   public ObjectReader7(
      Class objectClass, String typeKey, String typeName, long features, Supplier<T> creator, Function buildFunction, FieldReader... fieldReaders
   ) {
      this(objectClass, typeKey, typeName, features, null, creator, buildFunction, fieldReaders);
   }

   public ObjectReader7(
      Class objectClass,
      String typeKey,
      String typeName,
      long features,
      JSONSchema schema,
      Supplier<T> creator,
      Function buildFunction,
      FieldReader... fieldReaders
   ) {
      super(objectClass, typeKey, typeName, features, schema, creator, buildFunction, fieldReaders);
      this.fieldReader0 = fieldReaders[0];
      this.fieldReader1 = fieldReaders[1];
      this.fieldReader2 = fieldReaders[2];
      this.fieldReader3 = fieldReaders[3];
      this.fieldReader4 = fieldReaders[4];
      this.fieldReader5 = fieldReaders[5];
      this.fieldReader6 = fieldReaders[6];
      this.hashCode0 = this.fieldReader0.fieldNameHash;
      this.hashCode1 = this.fieldReader1.fieldNameHash;
      this.hashCode2 = this.fieldReader2.fieldNameHash;
      this.hashCode3 = this.fieldReader3.fieldNameHash;
      this.hashCode4 = this.fieldReader4.fieldNameHash;
      this.hashCode5 = this.fieldReader5.fieldNameHash;
      this.hashCode6 = this.fieldReader6.fieldNameHash;
      this.hashCode0LCase = this.fieldReader0.fieldNameHashLCase;
      this.hashCode1LCase = this.fieldReader1.fieldNameHashLCase;
      this.hashCode2LCase = this.fieldReader2.fieldNameHashLCase;
      this.hashCode3LCase = this.fieldReader3.fieldNameHashLCase;
      this.hashCode4LCase = this.fieldReader4.fieldNameHashLCase;
      this.hashCode5LCase = this.fieldReader5.fieldNameHashLCase;
      this.hashCode6LCase = this.fieldReader6.fieldNameHashLCase;
   }

   @Override
   public FieldReader getFieldReader(long hashCode) {
      if (hashCode == this.hashCode0) {
         return this.fieldReader0;
      } else if (hashCode == this.hashCode1) {
         return this.fieldReader1;
      } else if (hashCode == this.hashCode2) {
         return this.fieldReader2;
      } else if (hashCode == this.hashCode3) {
         return this.fieldReader3;
      } else if (hashCode == this.hashCode4) {
         return this.fieldReader4;
      } else if (hashCode == this.hashCode5) {
         return this.fieldReader5;
      } else {
         return hashCode == this.hashCode6 ? this.fieldReader6 : null;
      }
   }

   @Override
   public FieldReader getFieldReaderLCase(long hashCode) {
      if (hashCode == this.hashCode0LCase) {
         return this.fieldReader0;
      } else if (hashCode == this.hashCode1LCase) {
         return this.fieldReader1;
      } else if (hashCode == this.hashCode2LCase) {
         return this.fieldReader2;
      } else if (hashCode == this.hashCode3LCase) {
         return this.fieldReader3;
      } else if (hashCode == this.hashCode4LCase) {
         return this.fieldReader4;
      } else if (hashCode == this.hashCode5LCase) {
         return this.fieldReader5;
      } else {
         return hashCode == this.hashCode6LCase ? this.fieldReader6 : null;
      }
   }
}
