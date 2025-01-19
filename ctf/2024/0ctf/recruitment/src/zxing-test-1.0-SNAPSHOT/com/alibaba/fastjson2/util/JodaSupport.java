package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToIntFunction;

public class JodaSupport {
   static final long HASH_YEAR = Fnv.hashCode64("year");
   static final long HASH_MONTH = Fnv.hashCode64("month");
   static final long HASH_DAY = Fnv.hashCode64("day");
   static final long HASH_HOUR = Fnv.hashCode64("hour");
   static final long HASH_MINUTE = Fnv.hashCode64("minute");
   static final long HASH_SECOND = Fnv.hashCode64("second");
   static final long HASH_MILLIS = Fnv.hashCode64("millis");
   static final long HASH_CHRONOLOGY = Fnv.hashCode64("chronology");

   public static ObjectWriter createLocalDateTimeWriter(Class objectClass, String format) {
      return new JodaSupport.LocalDateTimeWriter(objectClass, format);
   }

   public static ObjectWriter createLocalDateWriter(Class objectClass, String format) {
      return new JodaSupport.LocalDateWriter(objectClass, format);
   }

   public static ObjectReader createChronologyReader(Class objectClass) {
      return new JodaSupport.ChronologyReader(objectClass);
   }

   public static ObjectReader createLocalDateReader(Class objectClass) {
      return new JodaSupport.LocalDateReader(objectClass);
   }

   public static ObjectReader createLocalDateTimeReader(Class objectClass) {
      return new JodaSupport.LocalDateTimeReader(objectClass);
   }

   public static ObjectReader createInstantReader(Class objectClass) {
      return new JodaSupport.InstantReader(objectClass);
   }

   public static ObjectWriter createGregorianChronologyWriter(Class objectClass) {
      return new JodaSupport.GregorianChronologyWriter(objectClass);
   }

   public static ObjectWriter createISOChronologyWriter(Class objectClass) {
      return new JodaSupport.ISOChronologyWriter(objectClass);
   }

   static class ChronologyReader implements ObjectReader {
      static final long HASH_ZONE_ID = Fnv.hashCode64("zoneId");
      final Class objectClass;
      final Class gregorianChronology;
      final Class dateTimeZone;
      final Function forID;
      final Function getInstance;
      final Object utc;

      ChronologyReader(Class objectClass) {
         this.objectClass = objectClass;
         ClassLoader classLoader = objectClass.getClassLoader();

         try {
            this.gregorianChronology = classLoader.loadClass("org.joda.time.chrono.GregorianChronology");
            this.dateTimeZone = classLoader.loadClass("org.joda.time.DateTimeZone");
            this.utc = this.gregorianChronology.getMethod("getInstanceUTC").invoke(null);
            this.forID = LambdaMiscCodec.createFunction(this.dateTimeZone.getMethod("forID", String.class));
            this.getInstance = LambdaMiscCodec.createFunction(this.gregorianChronology.getMethod("getInstance", this.dateTimeZone));
         } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException var4) {
            throw new JSONException("create ChronologyReader error", var4);
         }
      }

      @Override
      public Class getObjectClass() {
         return this.objectClass;
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         throw new JSONException(jsonReader.info("not support"));
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         Integer minimumDaysInFirstWeek = null;
         String zoneId = null;
         jsonReader.nextIfObjectStart();

         while (!jsonReader.nextIfObjectEnd()) {
            long HASH_MINIMUM_DAYS_IN_FIRST_WEEK = 8244232525129275563L;
            long fieldNameHashCode = jsonReader.readFieldNameHashCode();
            if (fieldNameHashCode == 8244232525129275563L) {
               minimumDaysInFirstWeek = jsonReader.readInt32Value();
            } else {
               if (fieldNameHashCode != HASH_ZONE_ID) {
                  throw new JSONException(jsonReader.info("not support fieldName " + jsonReader.getFieldName()));
               }

               zoneId = jsonReader.readString();
            }
         }

         if (minimumDaysInFirstWeek != null) {
            throw new JSONException(jsonReader.info("not support"));
         } else if ("UTC".equals(zoneId)) {
            return this.utc;
         } else {
            Object datetimeZone = this.forID.apply(zoneId);
            return this.getInstance.apply(datetimeZone);
         }
      }
   }

   public static final class DateTime2ZDT implements Function {
      static Class CLASS;
      static ToIntFunction YEAR;
      static ToIntFunction MONTH;
      static ToIntFunction DAY_OF_MONTH;
      static ToIntFunction HOUR;
      static ToIntFunction MINUTE;
      static ToIntFunction SECOND;
      static ToIntFunction MILLIS;
      static Function GET_ZONE;
      static Function GET_ID;

      @Override
      public Object apply(Object o) {
         try {
            if (CLASS == null) {
               CLASS = Class.forName("org.joda.time.DateTime");
            }

            if (YEAR == null) {
               YEAR = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getYear"));
            }

            if (MONTH == null) {
               MONTH = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getMonthOfYear"));
            }

            if (DAY_OF_MONTH == null) {
               DAY_OF_MONTH = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getDayOfMonth"));
            }

            if (HOUR == null) {
               HOUR = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getHourOfDay"));
            }

            if (MINUTE == null) {
               MINUTE = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getMinuteOfHour"));
            }

            if (SECOND == null) {
               SECOND = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getSecondOfMinute"));
            }

            if (MILLIS == null) {
               MILLIS = LambdaMiscCodec.createToIntFunction(CLASS.getMethod("getMillisOfSecond"));
            }

            if (GET_ZONE == null) {
               GET_ZONE = LambdaMiscCodec.createFunction(CLASS.getMethod("getZone"));
            }

            if (GET_ID == null) {
               GET_ID = LambdaMiscCodec.createFunction(Class.forName("org.joda.time.DateTimeZone").getMethod("getID"));
            }

            Object zone = GET_ZONE.apply(o);
            String zonIdStr = (String)GET_ID.apply(zone);
            ZoneId zoneId = ZoneId.of(zonIdStr);
            return ZonedDateTime.of(
               YEAR.applyAsInt(o),
               MONTH.applyAsInt(o),
               DAY_OF_MONTH.applyAsInt(o),
               HOUR.applyAsInt(o),
               MINUTE.applyAsInt(o),
               SECOND.applyAsInt(o),
               MILLIS.applyAsInt(o) * 1000000,
               zoneId
            );
         } catch (Exception var5) {
            throw new JSONException("convert joda org.joda.time.DateTime to java.time.ZonedDateTime error", var5);
         }
      }
   }

   public static final class DateTimeFromZDT implements Function {
      static Constructor CONS;
      static Method FOR_ID;

      @Override
      public Object apply(Object o) {
         ZonedDateTime zdt = (ZonedDateTime)o;

         try {
            if (FOR_ID == null) {
               Class<?> zoneClass = Class.forName("org.joda.time.DateTimeZone");
               FOR_ID = zoneClass.getMethod("forID", String.class);
            }

            if (CONS == null) {
               CONS = Class.forName("org.joda.time.DateTime")
                  .getConstructor(int.class, int.class, int.class, int.class, int.class, int.class, int.class, FOR_ID.getDeclaringClass());
            }

            String zondId = zdt.getZone().getId();
            if ("Z".equals(zondId)) {
               zondId = "UTC";
            }

            return CONS.newInstance(
               zdt.getYear(),
               zdt.getMonthValue(),
               zdt.getDayOfMonth(),
               zdt.getHour(),
               zdt.getMinute(),
               zdt.getSecond(),
               zdt.getNano() / 1000000,
               FOR_ID.invoke(null, zondId)
            );
         } catch (Exception var4) {
            throw new JSONException("build DateTime error", var4);
         }
      }
   }

   static class GregorianChronologyWriter implements ObjectWriter {
      final Class objectClass;
      final ToIntFunction getMinimumDaysInFirstWeek;
      final Function getZone;
      final Function getID;

      GregorianChronologyWriter(Class objectClass) {
         this.objectClass = objectClass;

         try {
            this.getMinimumDaysInFirstWeek = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getMinimumDaysInFirstWeek"));
            Method method = objectClass.getMethod("getZone");
            this.getZone = LambdaMiscCodec.createFunction(method);
            this.getID = LambdaMiscCodec.createFunction(method.getReturnType().getMethod("getID"));
         } catch (NoSuchMethodException var3) {
            throw new JSONException("getMethod error", var3);
         }
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Object zone = this.getZone.apply(object);
         String zoneId = (String)this.getID.apply(zone);
         int minDaysInFirstWeek = this.getMinimumDaysInFirstWeek.applyAsInt(object);
         jsonWriter.startObject();
         if (minDaysInFirstWeek != 4) {
            jsonWriter.writeName("minimumDaysInFirstWeek");
            jsonWriter.writeInt32(minDaysInFirstWeek);
         }

         jsonWriter.writeName("zoneId");
         jsonWriter.writeString(zoneId);
         jsonWriter.endObject();
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Object zone = this.getZone.apply(object);
         String zoneId = (String)this.getID.apply(zone);
         int minDaysInFirstWeek = this.getMinimumDaysInFirstWeek.applyAsInt(object);
         jsonWriter.startObject();
         jsonWriter.writeName("minimumDaysInFirstWeek");
         jsonWriter.writeInt32(minDaysInFirstWeek);
         jsonWriter.writeName("zoneId");
         jsonWriter.writeString(zoneId);
         jsonWriter.endObject();
      }
   }

   static class ISOChronologyWriter implements ObjectWriter {
      final Class objectClass;
      final Function getZone;
      final Function getID;

      ISOChronologyWriter(Class objectClass) {
         this.objectClass = objectClass;

         try {
            Method method = objectClass.getMethod("getZone");
            this.getZone = LambdaMiscCodec.createFunction(method);
            this.getID = LambdaMiscCodec.createFunction(method.getReturnType().getMethod("getID"));
         } catch (NoSuchMethodException var3) {
            throw new JSONException("getMethod error", var3);
         }
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Object zone = this.getZone.apply(object);
         String zoneId = (String)this.getID.apply(zone);
         jsonWriter.startObject();
         jsonWriter.writeName("zoneId");
         jsonWriter.writeString(zoneId);
         jsonWriter.endObject();
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Object zone = this.getZone.apply(object);
         String zoneId = (String)this.getID.apply(zone);
         jsonWriter.startObject();
         jsonWriter.writeName("zoneId");
         jsonWriter.writeString(zoneId);
         jsonWriter.endObject();
      }
   }

   static class InstantReader implements ObjectReader {
      final Class objectClass;
      final LongFunction constructor;

      InstantReader(Class objectClass) {
         this.objectClass = objectClass;

         try {
            this.constructor = LambdaMiscCodec.createLongFunction(objectClass.getConstructor(long.class));
         } catch (NoSuchMethodException var3) {
            throw new JSONException("create joda instant reader error", var3);
         }
      }

      @Override
      public Class getObjectClass() {
         return this.objectClass;
      }

      @Override
      public Object createInstance(Map map, long features) {
         Number millis = (Long)map.get("millis");
         if (millis != null) {
            return this.createInstanceFromMillis(millis.longValue());
         } else {
            Number epochSecond = (Number)map.get("epochSecond");
            if (epochSecond != null) {
               long epochMillis = epochSecond.longValue() * 1000L;
               return this.createInstanceFromMillis(epochMillis);
            } else {
               throw new JSONException("create joda instant error");
            }
         }
      }

      public Object createInstanceFromMillis(long millis) {
         return this.constructor.apply(millis);
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.nextIfNull()) {
            return null;
         } else if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            return this.createInstanceFromMillis(millis);
         } else if (jsonReader.isString()) {
            Instant jdkInstant = jsonReader.readInstant();
            if (jdkInstant == null) {
               return null;
            } else {
               long millis = jdkInstant.toEpochMilli();
               return this.createInstanceFromMillis(millis);
            }
         } else if (jsonReader.isObject()) {
            Map object = jsonReader.readObject();
            return this.createInstance(object, features);
         } else {
            throw new JSONException(jsonReader.info("not support"));
         }
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         return this.readObject(jsonReader, fieldType, fieldName, features);
      }
   }

   static class LocalDateReader implements ObjectReader {
      final Class objectClass;
      final Constructor constructor3;
      final Constructor constructor4;
      final Class classISOChronology;
      final Class classChronology;
      final Object utc;

      LocalDateReader(Class objectClass) {
         this.objectClass = objectClass;

         try {
            ClassLoader classLoader = objectClass.getClassLoader();
            this.classChronology = classLoader.loadClass("org.joda.time.Chronology");
            this.constructor3 = objectClass.getConstructor(int.class, int.class, int.class);
            this.constructor4 = objectClass.getConstructor(int.class, int.class, int.class, this.classChronology);
            this.classISOChronology = classLoader.loadClass("org.joda.time.chrono.ISOChronology");
            this.utc = this.classISOChronology.getMethod("getInstance").invoke(null);
         } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException var3) {
            throw new JSONException("create LocalDateWriter error", var3);
         }
      }

      @Override
      public Class getObjectClass() {
         return this.objectClass;
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.nextIfNull()) {
            return null;
         } else {
            LocalDate localDate = jsonReader.readLocalDate();
            if (localDate == null) {
               return null;
            } else {
               try {
                  return this.constructor4.newInstance(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), null);
               } catch (IllegalAccessException | InvocationTargetException | InstantiationException var8) {
                  throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var8);
               }
            }
         }
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         byte type = jsonReader.getType();
         if (type == -87) {
            LocalDate localDate = jsonReader.readLocalDate();

            try {
               return this.constructor3.newInstance(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var13) {
               throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var13);
            }
         } else if (jsonReader.isObject()) {
            Integer year = null;
            Integer month = null;
            Integer day = null;
            Object chronology = null;
            jsonReader.nextIfObjectStart();

            while (!jsonReader.nextIfObjectEnd()) {
               long fieldNameHashCode = jsonReader.readFieldNameHashCode();
               if (fieldNameHashCode == JodaSupport.HASH_YEAR) {
                  year = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_MONTH) {
                  month = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_DAY) {
                  day = jsonReader.readInt32Value();
               } else {
                  if (fieldNameHashCode != JodaSupport.HASH_CHRONOLOGY) {
                     throw new JSONException(jsonReader.info("not support fieldName " + jsonReader.getFieldName()));
                  }

                  chronology = jsonReader.read(this.classChronology);
               }
            }

            try {
               return this.constructor4.newInstance(year, month, day, chronology);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var14) {
               throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var14);
            }
         } else {
            throw new JSONException(jsonReader.info("not support " + JSONB.typeName(type)));
         }
      }
   }

   static class LocalDateTimeReader implements ObjectReader {
      final Class objectClass;
      final Constructor constructor7;
      final Constructor constructor8;
      final Class classISOChronology;
      final Class classChronology;
      final Object utc;

      LocalDateTimeReader(Class objectClass) {
         this.objectClass = objectClass;

         try {
            ClassLoader classLoader = objectClass.getClassLoader();
            this.classChronology = classLoader.loadClass("org.joda.time.Chronology");
            this.constructor7 = objectClass.getConstructor(int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            this.constructor8 = objectClass.getConstructor(int.class, int.class, int.class, int.class, int.class, int.class, int.class, this.classChronology);
            this.classISOChronology = classLoader.loadClass("org.joda.time.chrono.ISOChronology");
            this.utc = this.classISOChronology.getMethod("getInstance").invoke(null);
         } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException var3) {
            throw new JSONException("create LocalDateWriter error", var3);
         }
      }

      @Override
      public Class getObjectClass() {
         return this.objectClass;
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (!jsonReader.isString() && !jsonReader.isInt()) {
            throw new JSONException(jsonReader.info("not support"));
         } else {
            LocalDateTime ldt = jsonReader.readLocalDateTime();
            if (ldt == null) {
               return null;
            } else {
               try {
                  return this.constructor7
                     .newInstance(
                        ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1000000
                     );
               } catch (IllegalAccessException | InvocationTargetException | InstantiationException var8) {
                  throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var8);
               }
            }
         }
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         byte type = jsonReader.getType();
         if (type == -87) {
            LocalDate localDate = jsonReader.readLocalDate();

            try {
               return this.constructor7.newInstance(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), 0, 0, 0, 0);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var17) {
               throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var17);
            }
         } else if (type == -88) {
            LocalDateTime ldt = jsonReader.readLocalDateTime();

            try {
               return this.constructor7
                  .newInstance(
                     ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getHour(), ldt.getMinute(), ldt.getSecond(), ldt.getNano() / 1000000
                  );
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var18) {
               throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var18);
            }
         } else if (jsonReader.isObject()) {
            Integer year = null;
            Integer month = null;
            Integer day = null;
            Integer hour = null;
            Integer minute = null;
            Integer second = null;
            Integer millis = null;
            Object chronology = null;
            jsonReader.nextIfObjectStart();

            while (!jsonReader.nextIfObjectEnd()) {
               long fieldNameHashCode = jsonReader.readFieldNameHashCode();
               if (fieldNameHashCode == JodaSupport.HASH_YEAR) {
                  year = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_MONTH) {
                  month = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_DAY) {
                  day = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_HOUR) {
                  hour = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_MINUTE) {
                  minute = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_SECOND) {
                  second = jsonReader.readInt32Value();
               } else if (fieldNameHashCode == JodaSupport.HASH_MILLIS) {
                  millis = jsonReader.readInt32Value();
               } else {
                  if (fieldNameHashCode != JodaSupport.HASH_CHRONOLOGY) {
                     throw new JSONException(jsonReader.info("not support fieldName " + jsonReader.getFieldName()));
                  }

                  chronology = jsonReader.read(this.classChronology);
               }
            }

            try {
               return this.constructor8.newInstance(year, month, day, hour, minute, second, millis, chronology);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var19) {
               throw new JSONException(jsonReader.info("read org.joda.time.LocalDate error"), var19);
            }
         } else {
            throw new JSONException(jsonReader.info("not support " + JSONB.typeName(type)));
         }
      }
   }

   static class LocalDateTimeWriter extends DateTimeCodec implements ObjectWriter {
      final Class objectClass;
      final Method getYear;
      final Method getMonthOfYear;
      final Method getDayOfMonth;
      final ToIntFunction getHourOfDay;
      final ToIntFunction getMinuteOfHour;
      final ToIntFunction getSecondOfMinute;
      final ToIntFunction getMillisOfSecond;
      final Function getChronology;
      final Class isoChronology;
      final Object utc;

      LocalDateTimeWriter(Class objectClass, String format) {
         super(format);
         this.objectClass = objectClass;

         try {
            ClassLoader classLoader = objectClass.getClassLoader();
            this.isoChronology = classLoader.loadClass("org.joda.time.chrono.ISOChronology");
            Object instance = this.isoChronology.getMethod("getInstance").invoke(null);
            this.utc = this.isoChronology.getMethod("withUTC").invoke(instance);
            this.getYear = objectClass.getMethod("getYear");
            this.getMonthOfYear = objectClass.getMethod("getMonthOfYear");
            this.getDayOfMonth = objectClass.getMethod("getDayOfMonth");
            this.getHourOfDay = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getHourOfDay"));
            this.getMinuteOfHour = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getMinuteOfHour"));
            this.getSecondOfMinute = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getSecondOfMinute"));
            this.getMillisOfSecond = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getMillisOfSecond"));
            this.getChronology = LambdaMiscCodec.createFunction(objectClass.getMethod("getChronology"));
         } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException var5) {
            throw new JSONException("create LocalDateWriter error", var5);
         }
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         try {
            int year = (Integer)this.getYear.invoke(object);
            int monthOfYear = (Integer)this.getMonthOfYear.invoke(object);
            int dayOfMonth = (Integer)this.getDayOfMonth.invoke(object);
            int hour = this.getHourOfDay.applyAsInt(object);
            int minute = this.getMinuteOfHour.applyAsInt(object);
            int second = this.getSecondOfMinute.applyAsInt(object);
            int millis = this.getMillisOfSecond.applyAsInt(object);
            Object chronology = this.getChronology.apply(object);
            if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
               jsonWriter.writeTypeName(TypeUtils.getTypeName(object.getClass()));
            }

            if (chronology != this.utc && chronology != null) {
               jsonWriter.startObject();
               jsonWriter.writeName("year");
               jsonWriter.writeInt32(year);
               jsonWriter.writeName("month");
               jsonWriter.writeInt32(monthOfYear);
               jsonWriter.writeName("day");
               jsonWriter.writeInt32(dayOfMonth);
               jsonWriter.writeName("hour");
               jsonWriter.writeInt32(hour);
               jsonWriter.writeName("minute");
               jsonWriter.writeInt32(minute);
               jsonWriter.writeName("second");
               jsonWriter.writeInt32(second);
               jsonWriter.writeName("millis");
               jsonWriter.writeInt32(millis);
               jsonWriter.writeName("chronology");
               jsonWriter.writeAny(chronology);
               jsonWriter.endObject();
            } else {
               jsonWriter.writeLocalDateTime(LocalDateTime.of(year, monthOfYear, dayOfMonth, hour, minute, second, millis * 1000000));
            }
         } catch (InvocationTargetException | IllegalAccessException var15) {
            throw new JSONException("write LocalDateWriter error", var15);
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         try {
            int year = (Integer)this.getYear.invoke(object);
            int monthOfYear = (Integer)this.getMonthOfYear.invoke(object);
            int dayOfMonth = (Integer)this.getDayOfMonth.invoke(object);
            int hour = this.getHourOfDay.applyAsInt(object);
            int minute = this.getMinuteOfHour.applyAsInt(object);
            int second = this.getSecondOfMinute.applyAsInt(object);
            int millis = this.getMillisOfSecond.applyAsInt(object);
            Object chronology = this.getChronology.apply(object);
            if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
               jsonWriter.writeTypeName(TypeUtils.getTypeName(object.getClass()));
            }

            if (chronology != this.utc && chronology != null) {
               jsonWriter.startObject();
               jsonWriter.writeName("year");
               jsonWriter.writeInt32(year);
               jsonWriter.writeName("month");
               jsonWriter.writeInt32(monthOfYear);
               jsonWriter.writeName("day");
               jsonWriter.writeInt32(dayOfMonth);
               jsonWriter.writeName("hour");
               jsonWriter.writeInt32(hour);
               jsonWriter.writeName("minute");
               jsonWriter.writeInt32(minute);
               jsonWriter.writeName("second");
               jsonWriter.writeInt32(second);
               jsonWriter.writeName("millis");
               jsonWriter.writeInt32(millis);
               jsonWriter.writeName("chronology");
               jsonWriter.writeAny(chronology);
               jsonWriter.endObject();
            } else {
               int nanoOfSecond = millis * 1000000;
               LocalDateTime ldt = LocalDateTime.of(year, monthOfYear, dayOfMonth, hour, minute, second, nanoOfSecond);
               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter == null) {
                  formatter = jsonWriter.context.getDateFormatter();
               }

               if (formatter == null) {
                  jsonWriter.writeLocalDateTime(ldt);
               } else {
                  String str = formatter.format(ldt);
                  jsonWriter.writeString(str);
               }
            }
         } catch (InvocationTargetException | IllegalAccessException var19) {
            throw new JSONException("write LocalDateWriter error", var19);
         }
      }
   }

   static class LocalDateWriter extends DateTimeCodec implements ObjectWriter {
      final Class objectClass;
      final ToIntFunction getYear;
      final ToIntFunction getMonthOfYear;
      final ToIntFunction getDayOfMonth;
      final Function getChronology;
      final Class isoChronology;
      final Object utc;

      LocalDateWriter(Class objectClass, String format) {
         super(format);
         this.objectClass = objectClass;

         try {
            ClassLoader classLoader = objectClass.getClassLoader();
            this.isoChronology = classLoader.loadClass("org.joda.time.chrono.ISOChronology");
            Object instance = this.isoChronology.getMethod("getInstance").invoke(null);
            this.utc = this.isoChronology.getMethod("withUTC").invoke(instance);
            this.getYear = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getYear"));
            this.getMonthOfYear = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getMonthOfYear"));
            this.getDayOfMonth = LambdaMiscCodec.createToIntFunction(objectClass.getMethod("getDayOfMonth"));
            this.getChronology = LambdaMiscCodec.createFunction(objectClass.getMethod("getChronology"));
         } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassNotFoundException var5) {
            throw new JSONException("create LocalDateWriter error", var5);
         }
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         int year = this.getYear.applyAsInt(object);
         int monthOfYear = this.getMonthOfYear.applyAsInt(object);
         int dayOfMonth = this.getDayOfMonth.applyAsInt(object);
         Object chronology = this.getChronology.apply(object);
         if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            jsonWriter.writeTypeName(TypeUtils.getTypeName(object.getClass()));
         }

         if (chronology != this.utc && chronology != null) {
            jsonWriter.startObject();
            jsonWriter.writeName("year");
            jsonWriter.writeInt32(year);
            jsonWriter.writeName("month");
            jsonWriter.writeInt32(monthOfYear);
            jsonWriter.writeName("day");
            jsonWriter.writeInt32(dayOfMonth);
            jsonWriter.writeName("chronology");
            jsonWriter.writeAny(chronology);
            jsonWriter.endObject();
         } else {
            jsonWriter.writeLocalDate(LocalDate.of(year, monthOfYear, dayOfMonth));
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         int year = this.getYear.applyAsInt(object);
         int monthOfYear = this.getMonthOfYear.applyAsInt(object);
         int dayOfMonth = this.getDayOfMonth.applyAsInt(object);
         Object chronology = this.getChronology.apply(object);
         if (chronology != this.utc && chronology != null) {
            jsonWriter.startObject();
            jsonWriter.writeName("year");
            jsonWriter.writeInt32(year);
            jsonWriter.writeName("month");
            jsonWriter.writeInt32(monthOfYear);
            jsonWriter.writeName("day");
            jsonWriter.writeInt32(dayOfMonth);
            jsonWriter.writeName("chronology");
            jsonWriter.writeAny(chronology);
            jsonWriter.endObject();
         } else {
            LocalDate localDate = LocalDate.of(year, monthOfYear, dayOfMonth);
            DateTimeFormatter formatter = this.getDateFormatter();
            if (formatter == null) {
               formatter = jsonWriter.context.getDateFormatter();
            }

            if (formatter == null) {
               jsonWriter.writeLocalDate(localDate);
            } else {
               String str = formatter.format(localDate);
               jsonWriter.writeString(str);
            }
         }
      }
   }
}
