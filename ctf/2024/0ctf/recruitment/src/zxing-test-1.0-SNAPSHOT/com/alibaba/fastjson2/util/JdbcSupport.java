package com.alibaba.fastjson2.util;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderImplDate;
import com.alibaba.fastjson2.writer.ObjectWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class JdbcSupport {
   static Class CLASS_STRUCT;
   static volatile boolean CLASS_STRUCT_ERROR;
   static Class CLASS_CLOB;
   static volatile boolean CLASS_CLOB_ERROR;

   public static ObjectReader createTimeReader(Class objectClass, String format, Locale locale) {
      return new JdbcSupport.TimeReader(format, locale);
   }

   public static ObjectReader createTimestampReader(Class objectClass, String format, Locale locale) {
      return new JdbcSupport.TimestampReader(format, locale);
   }

   public static ObjectReader createDateReader(Class objectClass, String format, Locale locale) {
      return new JdbcSupport.DateReader(format, locale);
   }

   public static ObjectWriter createTimeWriter(String format) {
      return format == null ? JdbcSupport.TimeWriter.INSTANCE : new JdbcSupport.TimeWriter(format);
   }

   public static Object createTimestamp(long millis) {
      return new Timestamp(millis);
   }

   public static Object createDate(long millis) {
      return new Date(millis);
   }

   public static Object createTime(long millis) {
      return new Time(millis);
   }

   public static ObjectWriter createClobWriter(Class objectClass) {
      return new JdbcSupport.ClobWriter(objectClass);
   }

   public static ObjectWriter createTimestampWriter(Class objectClass, String format) {
      return new JdbcSupport.TimestampWriter(format);
   }

   public static boolean isClob(Class objectClass) {
      if (CLASS_CLOB == null && !CLASS_CLOB_ERROR) {
         try {
            CLASS_CLOB = Class.forName("java.sql.Clob");
         } catch (Throwable var2) {
            CLASS_CLOB_ERROR = true;
         }
      }

      return CLASS_CLOB != null && CLASS_CLOB.isAssignableFrom(objectClass);
   }

   public static boolean isStruct(Class objectClass) {
      if (CLASS_STRUCT == null && !CLASS_STRUCT_ERROR) {
         try {
            CLASS_STRUCT = Class.forName("java.sql.Struct");
         } catch (Throwable var2) {
            CLASS_STRUCT_ERROR = true;
         }
      }

      return CLASS_STRUCT != null && CLASS_STRUCT.isAssignableFrom(objectClass);
   }

   static class ClobWriter implements ObjectWriter {
      final Class objectClass;

      public ClobWriter(Class objectClass) {
         if (JdbcSupport.CLASS_CLOB == null && !JdbcSupport.CLASS_CLOB_ERROR) {
            try {
               JdbcSupport.CLASS_CLOB = Class.forName("java.sql.Clob");
            } catch (Throwable var3) {
               JdbcSupport.CLASS_CLOB_ERROR = true;
            }
         }

         if (JdbcSupport.CLASS_CLOB == null) {
            throw new JSONException("class java.sql.Clob not found");
         } else {
            this.objectClass = objectClass;
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         Clob clob = (Clob)object;

         Reader reader;
         try {
            reader = clob.getCharacterStream();
         } catch (SQLException var10) {
            throw new JSONException("Clob.getCharacterStream error", var10);
         }

         jsonWriter.writeString(reader);
      }
   }

   static class DateReader extends ObjectReaderImplDate {
      public DateReader(String format, Locale locale) {
         super(format, locale);
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         return this.readObject(jsonReader, fieldType, fieldName, features);
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            return new Date(millis);
         } else if (jsonReader.readIfNull()) {
            return null;
         } else if (this.formatUnixTime && jsonReader.isString()) {
            String str = jsonReader.readString();
            long millis = Long.parseLong(str);
            millis *= 1000L;
            return new Date(millis);
         } else if (this.format != null && !this.formatISO8601 && !this.formatMillis) {
            String str = jsonReader.readString();
            if (str.isEmpty()) {
               return null;
            } else {
               DateTimeFormatter dateFormatter = this.getDateFormatter();
               Instant instant;
               if (!this.formatHasHour) {
                  LocalDate localDate = LocalDate.parse(str, dateFormatter);
                  LocalDateTime ldt = LocalDateTime.of(localDate, LocalTime.MIN);
                  instant = ldt.atZone(jsonReader.getContext().getZoneId()).toInstant();
               } else {
                  LocalDateTime ldt = LocalDateTime.parse(str, dateFormatter);
                  instant = ldt.atZone(jsonReader.getContext().getZoneId()).toInstant();
               }

               return new Date(instant.toEpochMilli());
            }
         } else {
            LocalDateTime localDateTime = jsonReader.readLocalDateTime();
            if (localDateTime != null) {
               return Date.valueOf(localDateTime.toLocalDate());
            } else if (jsonReader.wasNull()) {
               return null;
            } else {
               long millis = jsonReader.readMillisFromString();
               return millis == 0L && jsonReader.wasNull() ? null : new Date(millis);
            }
         }
      }
   }

   static class TimeReader extends ObjectReaderImplDate {
      public TimeReader(String format, Locale locale) {
         super(format, locale);
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         return this.readObject(jsonReader, fieldType, fieldName, features);
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            return new Time(millis);
         } else if (jsonReader.readIfNull()) {
            return null;
         } else if (this.formatISO8601 || this.formatMillis) {
            long millis = jsonReader.readMillisFromString();
            return new Time(millis);
         } else if (this.formatUnixTime) {
            long seconds = jsonReader.readInt64();
            return new Time(seconds * 1000L);
         } else {
            long millis;
            if (this.format != null) {
               DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
               ZonedDateTime zdt;
               if (formatter != null) {
                  String str = jsonReader.readString();
                  if (str.isEmpty()) {
                     return null;
                  }

                  LocalDateTime ldt;
                  if (!this.formatHasHour) {
                     ldt = LocalDateTime.of(LocalDate.parse(str, formatter), LocalTime.MIN);
                  } else if (!this.formatHasDay) {
                     ldt = LocalDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.parse(str, formatter));
                  } else {
                     ldt = LocalDateTime.parse(str, formatter);
                  }

                  zdt = ldt.atZone(jsonReader.getContext().getZoneId());
               } else {
                  zdt = jsonReader.readZonedDateTime();
               }

               millis = zdt.toInstant().toEpochMilli();
            } else {
               String strx = jsonReader.readString();
               if (!"0000-00-00".equals(strx) && !"0000-00-00 00:00:00".equals(strx)) {
                  if (strx.length() != 9 || strx.charAt(8) != 'Z') {
                     if (!strx.isEmpty() && !"null".equals(strx)) {
                        return Time.valueOf(strx);
                     }

                     return null;
                  }

                  LocalTime localTime = DateUtils.parseLocalTime(
                     strx.charAt(0), strx.charAt(1), strx.charAt(2), strx.charAt(3), strx.charAt(4), strx.charAt(5), strx.charAt(6), strx.charAt(7)
                  );
                  millis = LocalDateTime.of(DateUtils.LOCAL_DATE_19700101, localTime).atZone(DateUtils.DEFAULT_ZONE_ID).toInstant().toEpochMilli();
               } else {
                  millis = 0L;
               }
            }

            return new Time(millis);
         }
      }
   }

   static class TimeWriter extends DateTimeCodec implements ObjectWriter {
      public static final JdbcSupport.TimeWriter INSTANCE = new JdbcSupport.TimeWriter(null);

      public TimeWriter(String format) {
         super(format);
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (object == null) {
            jsonWriter.writeNull();
         } else {
            JSONWriter.Context context = jsonWriter.context;
            if (this.formatUnixTime || context.isDateFormatUnixTime()) {
               long millis = ((java.util.Date)object).getTime();
               long seconds = millis / 1000L;
               jsonWriter.writeInt64(seconds);
            } else if (this.formatMillis || context.isDateFormatMillis()) {
               long millis = ((java.util.Date)object).getTime();
               jsonWriter.writeInt64(millis);
            } else if (!this.formatISO8601 && !context.isDateFormatISO8601()) {
               DateTimeFormatter dateFormatter = null;
               if (this.format != null && !this.format.contains("dd")) {
                  dateFormatter = this.getDateFormatter();
               }

               if (dateFormatter == null) {
                  String format = context.getDateFormat();
                  if (format != null && !format.contains("dd")) {
                     dateFormatter = context.getDateFormatter();
                  }
               }

               if (dateFormatter == null) {
                  jsonWriter.writeString(object.toString());
               } else {
                  java.util.Date time = (java.util.Date)object;
                  ZoneId zoneId = context.getZoneId();
                  Instant instant = Instant.ofEpochMilli(time.getTime());
                  ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
                  String str = dateFormatter.format(zdt);
                  jsonWriter.writeString(str);
               }
            } else {
               ZoneId zoneId = context.getZoneId();
               long millis = ((java.util.Date)object).getTime();
               Instant instant = Instant.ofEpochMilli(millis);
               ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
               int offsetSeconds = zdt.getOffset().getTotalSeconds();
               int year = zdt.getYear();
               int month = zdt.getMonthValue();
               int dayOfMonth = zdt.getDayOfMonth();
               int hour = zdt.getHour();
               int minute = zdt.getMinute();
               int second = zdt.getSecond();
               int nano = 0;
               jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, nano, offsetSeconds, true);
            }
         }
      }
   }

   static class TimestampReader extends ObjectReaderImplDate {
      public TimestampReader(String format, Locale locale) {
         super(format, locale);
      }

      @Override
      public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            return this.createTimestamp(millis, 0);
         } else if (jsonReader.readIfNull()) {
            return null;
         } else {
            byte type = jsonReader.getType();
            if (type == -88) {
               LocalDateTime ldt = jsonReader.readLocalDateTime();
               Instant instant = ldt.atZone(jsonReader.getContext().getZoneId()).toInstant();
               return this.createTimestamp(instant.toEpochMilli(), instant.getNano());
            } else {
               return this.readObject(jsonReader, fieldType, fieldName, features);
            }
         }
      }

      Object createTimestamp(long millis, int nanos) {
         Timestamp timestamp = new Timestamp(millis);
         if (nanos != 0) {
            timestamp.setNanos(nanos);
         }

         return timestamp;
      }

      @Override
      public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
         if (jsonReader.isInt()) {
            long millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }

            return this.createTimestamp(millis, 0);
         } else if (jsonReader.nextIfNullOrEmptyString()) {
            return null;
         } else if (this.format != null && !this.formatISO8601 && !this.formatMillis) {
            String str = jsonReader.readString();
            if (str.isEmpty()) {
               return null;
            } else {
               DateTimeFormatter dateFormatter = this.getDateFormatter();
               Instant instant;
               if (!this.formatHasHour) {
                  LocalDate localDate = LocalDate.parse(str, dateFormatter);
                  LocalDateTime ldt = LocalDateTime.of(localDate, LocalTime.MIN);
                  instant = ldt.atZone(jsonReader.getContext().getZoneId()).toInstant();
               } else {
                  LocalDateTime ldt = LocalDateTime.parse(str, dateFormatter);
                  instant = ldt.atZone(jsonReader.getContext().getZoneId()).toInstant();
               }

               long millis = instant.toEpochMilli();
               int nanos = instant.getNano();
               return this.createTimestamp(millis, nanos);
            }
         } else {
            LocalDateTime localDateTime = jsonReader.readLocalDateTime();
            if (localDateTime != null) {
               return Timestamp.valueOf(localDateTime);
            } else if (jsonReader.wasNull()) {
               return null;
            } else {
               long millis = jsonReader.readMillisFromString();
               return millis == 0L && jsonReader.wasNull() ? null : new Timestamp(millis);
            }
         }
      }
   }

   static class TimestampWriter extends DateTimeCodec implements ObjectWriter {
      public TimestampWriter(String format) {
         super(format);
      }

      @Override
      public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (object == null) {
            jsonWriter.writeNull();
         } else {
            Timestamp date = (Timestamp)object;
            if (this.format != null) {
               this.write(jsonWriter, object, fieldName, fieldType, features);
            } else {
               LocalDateTime localDateTime = date.toLocalDateTime();
               jsonWriter.writeLocalDateTime(localDateTime);
            }
         }
      }

      @Override
      public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
         if (object == null) {
            jsonWriter.writeNull();
         } else {
            JSONWriter.Context ctx = jsonWriter.context;
            Timestamp date = (Timestamp)object;
            if (!this.formatUnixTime && !ctx.isDateFormatUnixTime()) {
               ZoneId zoneId = ctx.getZoneId();
               Instant instant = date.toInstant();
               ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, zoneId);
               int offsetSeconds = zdt.getOffset().getTotalSeconds();
               if ((this.formatISO8601 || ctx.isDateFormatISO8601()) && zdt.getNano() % 1000000 == 0) {
                  int year = zdt.getYear();
                  int month = zdt.getMonthValue();
                  int dayOfMonth = zdt.getDayOfMonth();
                  int hour = zdt.getHour();
                  int minute = zdt.getMinute();
                  int second = zdt.getSecond();
                  int nano = zdt.getNano();
                  int millis = nano / 1000000;
                  jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, millis, offsetSeconds, true);
               } else {
                  DateTimeFormatter dateFormatter = this.getDateFormatter();
                  if (dateFormatter == null) {
                     dateFormatter = ctx.getDateFormatter();
                  }

                  if (dateFormatter == null) {
                     if (this.formatMillis || ctx.isDateFormatMillis()) {
                        long millis = date.getTime();
                        jsonWriter.writeInt64(millis);
                        return;
                     }

                     int nanos = date.getNanos();
                     int year = zdt.getYear();
                     int month = zdt.getMonthValue();
                     int dayOfMonth = zdt.getDayOfMonth();
                     int hour = zdt.getHour();
                     int minute = zdt.getMinute();
                     int second = zdt.getSecond();
                     if (nanos % 1000000 == 0) {
                        jsonWriter.writeDateTimeISO8601(year, month, dayOfMonth, hour, minute, second, nanos / 1000000, offsetSeconds, false);
                     } else {
                        jsonWriter.writeLocalDateTime(zdt.toLocalDateTime());
                     }
                  } else {
                     String str = dateFormatter.format(zdt);
                     jsonWriter.writeString(str);
                  }
               }
            } else {
               long millis = date.getTime();
               jsonWriter.writeInt64(millis / 1000L);
            }
         }
      }
   }
}
