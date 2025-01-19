package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ApacheLang3Support {
   public static class PairReader implements ObjectReader {
      static final long LEFT = Fnv.hashCode64("left");
      static final long RIGHT = Fnv.hashCode64("right");
      final Class objectClass;
      final Type leftType;
      final Type rightType;
      final BiFunction of;

      public PairReader(Class objectClass, Type leftType, Type rightType) {
         this.objectClass = objectClass;
         this.leftType = leftType;
         this.rightType = rightType;

         try {
            this.of = LambdaMiscCodec.createBiFunction(objectClass.getMethod("of", Object.class, Object.class));
         } catch (NoSuchMethodException var5) {
            throw new JSONException("Pair.of method not found", var5);
         }
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.nextIfNull()) {
            return null;
         } else {
            if (jsonReader.nextIfMatch((byte)-110)) {
               long typeHash = jsonReader.readTypeHashCode();
               long PAIR = 4645080105124911238L;
               long MUTABLE_PAIR = 8310287657375596772L;
               long IMMUTABLE_PAIR = -2802985644706367574L;
               if (typeHash != 4645080105124911238L && typeHash != -2802985644706367574L && typeHash != 8310287657375596772L) {
                  throw new JSONException("not support inputType : " + jsonReader.getString());
               }
            }

            Object left = null;
            Object right = null;
            if (jsonReader.nextIfObjectStart()) {
               for (int i = 0; i < 100 && !jsonReader.nextIfObjectEnd(); i++) {
                  if (jsonReader.isString()) {
                     long hashCode = jsonReader.readFieldNameHashCode();
                     if (hashCode == LEFT) {
                        left = jsonReader.read(this.leftType);
                     } else if (hashCode == RIGHT) {
                        right = jsonReader.read(this.rightType);
                     } else if (i == 0) {
                        left = jsonReader.getFieldName();
                        right = jsonReader.read(this.rightType);
                     } else {
                        jsonReader.skipValue();
                     }
                  } else {
                     if (i != 0) {
                        throw new JSONException(jsonReader.info("not support input"));
                     }

                     left = jsonReader.read(this.leftType);
                     right = jsonReader.read(this.rightType);
                  }
               }
            } else {
               if (!jsonReader.isArray()) {
                  throw new JSONException(jsonReader.info("not support input"));
               }

               int len = jsonReader.startArray();
               if (len != 2) {
                  throw new JSONException(jsonReader.info("not support input"));
               }

               left = jsonReader.read(this.leftType);
               right = jsonReader.read(this.rightType);
            }

            return this.of.apply(left, right);
         }
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.nextIfNull()) {
            return null;
         } else {
            Object left = null;
            Object right = null;
            if (jsonReader.nextIfObjectStart()) {
               for (int i = 0; i < 100 && !jsonReader.nextIfObjectEnd(); i++) {
                  if (jsonReader.isString()) {
                     long hashCode = jsonReader.readFieldNameHashCode();
                     if (hashCode == LEFT) {
                        left = jsonReader.read(this.leftType);
                     } else if (hashCode == RIGHT) {
                        right = jsonReader.read(this.rightType);
                     } else if (i == 0) {
                        left = jsonReader.getFieldName();
                        jsonReader.nextIfMatch(':');
                        right = jsonReader.read(this.rightType);
                     } else {
                        jsonReader.skipValue();
                     }
                  } else {
                     if (i != 0) {
                        throw new JSONException(jsonReader.info("not support input"));
                     }

                     left = jsonReader.read(this.leftType);
                     jsonReader.nextIfMatch(':');
                     right = jsonReader.read(this.rightType);
                  }
               }
            } else {
               if (!jsonReader.nextIfArrayStart()) {
                  throw new JSONException(jsonReader.info("not support input"));
               }

               left = jsonReader.read(this.leftType);
               right = jsonReader.read(this.rightType);
               if (!jsonReader.nextIfArrayEnd()) {
                  throw new JSONException(jsonReader.info("not support input"));
               }
            }

            return this.of.apply(left, right);
         }
      }
   }

   public static class PairWriter implements ObjectWriter {
      final Class objectClass;
      final String typeName;
      final long typeNameHash;
      Function left;
      Function right;
      byte[] typeNameJSONB;
      static final byte[] leftName = JSONB.toBytes("left");
      static final byte[] rightName = JSONB.toBytes("right");

      public PairWriter(Class objectClass) {
         this.objectClass = objectClass;
         this.typeName = objectClass.getName();
         this.typeNameHash = Fnv.hashCode64(this.typeName);
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (object == null) {
            jsonWriter.writeNull();
         } else {
            if ((jsonWriter.getFeatures(features) & JSONWriter.Feature.WriteClassName.mask) != 0L) {
               if (this.typeNameJSONB == null) {
                  this.typeNameJSONB = JSONB.toBytes(this.typeName);
               }

               jsonWriter.writeTypeName(this.typeNameJSONB, this.typeNameHash);
            }

            jsonWriter.startObject();
            Object left = this.getLeft(object);
            Object right = this.getRight(object);
            jsonWriter.writeNameRaw(leftName, ApacheLang3Support.PairReader.LEFT);
            jsonWriter.writeAny(left);
            jsonWriter.writeNameRaw(rightName, ApacheLang3Support.PairReader.RIGHT);
            jsonWriter.writeAny(right);
            jsonWriter.endObject();
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (object == null) {
            jsonWriter.writeNull();
         } else {
            Object left = this.getLeft(object);
            Object right = this.getRight(object);
            jsonWriter.startObject();
            if ((jsonWriter.getFeatures(features) & JSONWriter.Feature.WritePairAsJavaBean.mask) != 0L) {
               jsonWriter.writeName("left");
               jsonWriter.writeColon();
               jsonWriter.writeAny(left);
               jsonWriter.writeName("right");
            } else {
               jsonWriter.writeNameAny(left);
            }

            jsonWriter.writeColon();
            jsonWriter.writeAny(right);
            jsonWriter.endObject();
         }
      }

      Object getLeft(Object object) {
         Class<?> objectClass = object.getClass();
         if (this.left == null) {
            try {
               this.left = LambdaMiscCodec.createFunction(objectClass.getMethod("getLeft"));
            } catch (NoSuchMethodException var4) {
               throw new JSONException("getLeft method not found", var4);
            }
         }

         return this.left.apply(object);
      }

      Object getRight(Object object) {
         Class<?> objectClass = object.getClass();
         if (this.right == null) {
            try {
               this.right = LambdaMiscCodec.createFunction(objectClass.getMethod("getRight"));
            } catch (NoSuchMethodException var4) {
               throw new JSONException("getRight method not found", var4);
            }
         }

         return this.right.apply(object);
      }
   }

   public interface TripleMixIn<L, M, R> {
      @JSONCreator
      static <L, M, R> Object of(L left, M middle, R right) {
         return null;
      }
   }
}
