package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.internal.asm.ASMUtils;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class ObjectReaderException<T> extends ObjectReaderAdapter<T> {
   static final long HASH_TYPE = Fnv.hashCode64("@type");
   static final long HASH_MESSAGE = Fnv.hashCode64("message");
   static final long HASH_DETAIL_MESSAGE = Fnv.hashCode64("detailMessage");
   static final long HASH_LOCALIZED_MESSAGE = Fnv.hashCode64("localizedMessage");
   static final long HASH_CAUSE = Fnv.hashCode64("cause");
   static final long HASH_STACKTRACE = Fnv.hashCode64("stackTrace");
   static final long HASH_SUPPRESSED_EXCEPTIONS = Fnv.hashCode64("suppressedExceptions");
   private final FieldReader fieldReaderStackTrace;
   final List<Constructor> constructors;
   final Constructor constructorDefault;
   final Constructor constructorMessage;
   final Constructor constructorMessageCause;
   final Constructor constructorCause;
   final List<String[]> constructorParameters;

   ObjectReaderException(Class<T> objectClass) {
      this(
         objectClass,
         Arrays.asList(BeanUtils.getConstructor(objectClass)),
         ObjectReaders.fieldReader("stackTrace", StackTraceElement[].class, Throwable::setStackTrace)
      );
   }

   ObjectReaderException(Class<T> objectClass, List<Constructor> constructors, FieldReader... fieldReaders) {
      super(objectClass, null, objectClass.getName(), 0L, null, null, null, fieldReaders);
      this.constructors = constructors;
      Constructor constructorDefault = null;
      Constructor constructorMessage = null;
      Constructor constructorMessageCause = null;
      Constructor constructorCause = null;

      for (Constructor constructor : constructors) {
         if (constructor != null && constructorMessageCause == null) {
            int paramCount = constructor.getParameterCount();
            if (paramCount == 0) {
               constructorDefault = constructor;
            } else {
               Class[] paramTypes = constructor.getParameterTypes();
               Class paramType0 = paramTypes[0];
               if (paramCount == 1) {
                  if (paramType0 == String.class) {
                     constructorMessage = constructor;
                  } else if (Throwable.class.isAssignableFrom(paramType0)) {
                     constructorCause = constructor;
                  }
               }

               if (paramCount == 2 && paramType0 == String.class && Throwable.class.isAssignableFrom(paramTypes[1])) {
                  constructorMessageCause = constructor;
               }
            }
         }
      }

      this.constructorDefault = constructorDefault;
      this.constructorMessage = constructorMessage;
      this.constructorMessageCause = constructorMessageCause;
      this.constructorCause = constructorCause;
      constructors.sort((left, right) -> {
         int x = left.getParameterCount();
         int y = right.getParameterCount();
         return Integer.compare(y, x);
      });
      this.constructorParameters = new ArrayList<>(constructors.size());

      for (Constructor constructorx : constructors) {
         int paramCount = constructorx.getParameterCount();
         String[] parameterNames = null;
         if (paramCount > 0) {
            parameterNames = ASMUtils.lookupParameterNames(constructorx);
            Parameter[] parameters = constructorx.getParameters();
            FieldInfo fieldInfo = new FieldInfo();

            for (int i = 0; i < parameters.length && i < parameterNames.length; i++) {
               fieldInfo.init();
               Parameter parameter = parameters[i];
               ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
               provider.getFieldInfo(fieldInfo, objectClass, constructorx, i, parameter);
               if (fieldInfo.fieldName != null) {
                  parameterNames[i] = fieldInfo.fieldName;
               }
            }
         }

         this.constructorParameters.add(parameterNames);
      }

      FieldReader fieldReaderStackTrace = null;

      for (FieldReader fieldReader : fieldReaders) {
         if ("stackTrace".equals(fieldReader.fieldName) && fieldReader.fieldClass == StackTraceElement[].class) {
            fieldReaderStackTrace = fieldReader;
         }
      }

      this.fieldReaderStackTrace = fieldReaderStackTrace;
   }

   @Override
   public T readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      boolean objectStart = jsonReader.nextIfObjectStart();
      if (!objectStart && jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else {
         String message = null;
         String localizedMessage = null;
         Throwable cause = null;
         StackTraceElement[] stackTrace = null;
         List<Throwable> suppressedExceptions = null;
         String stackTraceReference = null;
         String suppressedExceptionsReference = null;
         String causeReference = null;
         Map<String, Object> fieldValues = null;
         Map<String, String> references = null;

         for (int i = 0; !jsonReader.nextIfObjectEnd(); i++) {
            long hash = jsonReader.readFieldNameHashCode();
            if (i == 0 && hash == HASH_TYPE && jsonReader.isSupportAutoType(features)) {
               long typeHash = jsonReader.readTypeHashCode();
               JSONReader.Context context = jsonReader.getContext();
               ObjectReader reader = this.autoType(context, typeHash);
               if (reader == null) {
                  String typeName = jsonReader.getString();
                  reader = context.getObjectReaderAutoType(typeName, this.objectClass, features);
                  if (reader == null) {
                     throw new JSONException(jsonReader.info("No suitable ObjectReader found for" + typeName));
                  }
               }

               if (reader != this) {
                  return (T)reader.readObject(jsonReader);
               }
            } else if (hash == HASH_MESSAGE || hash == HASH_DETAIL_MESSAGE) {
               message = jsonReader.readString();
            } else if (hash == HASH_LOCALIZED_MESSAGE) {
               localizedMessage = jsonReader.readString();
            } else if (hash == HASH_CAUSE) {
               if (jsonReader.isReference()) {
                  causeReference = jsonReader.readReference();
               } else {
                  cause = jsonReader.read(Throwable.class);
               }
            } else if (hash == HASH_STACKTRACE) {
               if (jsonReader.isReference()) {
                  stackTraceReference = jsonReader.readReference();
               } else {
                  stackTrace = jsonReader.read(StackTraceElement[].class);
               }
            } else if (hash == HASH_SUPPRESSED_EXCEPTIONS) {
               if (jsonReader.isReference()) {
                  suppressedExceptionsReference = jsonReader.readReference();
               } else if (jsonReader.getType() == -110) {
                  suppressedExceptions = (List<Throwable>)jsonReader.readAny();
               } else {
                  suppressedExceptions = jsonReader.readArray(Throwable.class);
               }
            } else {
               FieldReader fieldReader = this.getFieldReader(hash);
               if (fieldValues == null) {
                  fieldValues = new HashMap<>();
               }

               String name;
               if (fieldReader != null) {
                  name = fieldReader.fieldName;
               } else {
                  name = jsonReader.getFieldName();
               }

               if (jsonReader.isReference()) {
                  String reference = jsonReader.readReference();
                  if (references == null) {
                     references = new HashMap<>();
                  }

                  references.put(name, reference);
               } else {
                  Object fieldValue;
                  if (fieldReader != null) {
                     fieldValue = fieldReader.readFieldValue(jsonReader);
                  } else {
                     fieldValue = jsonReader.readAny();
                  }

                  fieldValues.put(name, fieldValue);
               }
            }
         }

         Throwable object = this.createObject(message, cause);
         if (object == null) {
            for (int ix = 0; ix < this.constructors.size(); ix++) {
               String[] paramNames = this.constructorParameters.get(ix);
               if (paramNames != null && paramNames.length != 0) {
                  boolean matchAll = true;

                  for (String paramName : paramNames) {
                     if (paramName == null) {
                        matchAll = false;
                        break;
                     }

                     switch (paramName) {
                        case "message":
                        case "cause":
                           break;
                        case "errorIndex":
                           if (this.objectClass != DateTimeParseException.class && !fieldValues.containsKey(paramName)) {
                              matchAll = false;
                           }
                           break;
                        default:
                           if (!fieldValues.containsKey(paramName)) {
                              matchAll = false;
                           }
                     }
                  }

                  if (matchAll) {
                     Object[] args = new Object[paramNames.length];

                     for (int j = 0; j < paramNames.length; j++) {
                        String paramName = paramNames[j];
                        Object fieldValue;
                        switch (paramName) {
                           case "message":
                              fieldValue = message;
                              break;
                           case "cause":
                              fieldValue = cause;
                              break;
                           case "errorIndex":
                              fieldValue = fieldValues.get(paramName);
                              if (fieldValue == null && this.objectClass == DateTimeParseException.class) {
                                 fieldValue = 0;
                              }
                              break;
                           default:
                              fieldValue = fieldValues.get(paramName);
                        }

                        args[j] = fieldValue;
                     }

                     Constructor constructor = this.constructors.get(ix);

                     try {
                        object = (Throwable)constructor.newInstance(args);
                        break;
                     } catch (Throwable var27) {
                        throw new JSONException("create error, objectClass " + constructor + ", " + var27.getMessage(), var27);
                     }
                  }
               }
            }
         }

         if (object == null) {
            throw new JSONException(jsonReader.info(jsonReader.info("not support : " + this.objectClass.getName())));
         } else {
            if (stackTrace != null) {
               int nullCount = 0;

               for (StackTraceElement item : stackTrace) {
                  if (item == null) {
                     nullCount++;
                  }
               }

               if (stackTrace.length == 0 || nullCount != stackTrace.length) {
                  object.setStackTrace(stackTrace);
               }
            }

            if (stackTraceReference != null) {
               jsonReader.addResolveTask(this.fieldReaderStackTrace, object, JSONPath.of(stackTraceReference));
            }

            if (fieldValues != null) {
               for (Entry<String, Object> entry : fieldValues.entrySet()) {
                  FieldReader fieldReaderx = this.getFieldReader(entry.getKey());
                  if (fieldReaderx != null) {
                     fieldReaderx.accept(object, entry.getValue());
                  }
               }
            }

            if (references != null) {
               for (Entry<String, String> entryx : references.entrySet()) {
                  FieldReader fieldReaderx = this.getFieldReader(entryx.getKey());
                  if (fieldReaderx != null) {
                     fieldReaderx.addResolveTask(jsonReader, object, entryx.getValue());
                  }
               }
            }

            return (T)object;
         }
      }
   }

   @Override
   public T createInstance(Map map, long features) {
      return map == null ? null : this.readObject(JSONReader.of(JSON.toJSONString(map)), features);
   }

   @Override
   public T readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.getType() == -110) {
         JSONReader.Context context = jsonReader.getContext();
         if (jsonReader.isSupportAutoType(features) || context.getContextAutoTypeBeforeHandler() != null) {
            jsonReader.next();
            long typeHash = jsonReader.readTypeHashCode();
            ObjectReader autoTypeObjectReader = context.getObjectReaderAutoType(typeHash);
            if (autoTypeObjectReader == null) {
               String typeName = jsonReader.getString();
               autoTypeObjectReader = context.getObjectReaderAutoType(typeName, null);
               if (autoTypeObjectReader == null) {
                  throw new JSONException("autoType not support : " + typeName + ", offset " + jsonReader.getOffset());
               }
            }

            return (T)autoTypeObjectReader.readJSONBObject(jsonReader, fieldType, fieldName, 0L);
         }
      }

      return this.readObject(jsonReader, fieldType, fieldName, features);
   }

   private Throwable createObject(String message, Throwable cause) {
      try {
         if (this.constructorMessageCause != null && cause != null && message != null) {
            return (Throwable)this.constructorMessageCause.newInstance(message, cause);
         } else if (this.constructorMessage != null && message != null) {
            return (Throwable)this.constructorMessage.newInstance(message);
         } else if (this.constructorCause != null && cause != null) {
            return (Throwable)this.constructorCause.newInstance(cause);
         } else if (this.constructorMessageCause == null || cause == null && message == null) {
            if (this.constructorDefault != null) {
               return (Throwable)this.constructorDefault.newInstance();
            } else if (this.constructorMessageCause != null) {
               return (Throwable)this.constructorMessageCause.newInstance(message, cause);
            } else if (this.constructorMessage != null) {
               return (Throwable)this.constructorMessage.newInstance(message);
            } else {
               return this.constructorCause != null ? (Throwable)this.constructorCause.newInstance(cause) : null;
            }
         } else {
            return (Throwable)this.constructorMessageCause.newInstance(message, cause);
         }
      } catch (Throwable var4) {
         throw new JSONException("create Exception error, class " + this.objectClass.getName() + ", " + var4.getMessage(), var4);
      }
   }
}
