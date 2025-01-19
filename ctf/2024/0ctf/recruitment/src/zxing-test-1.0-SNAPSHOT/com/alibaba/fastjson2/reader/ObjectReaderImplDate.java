package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ObjectReaderImplDate extends DateTimeCodec implements ObjectReader {
   public static final ObjectReaderImplDate INSTANCE = new ObjectReaderImplDate(null, null);

   public static ObjectReaderImplDate of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectReaderImplDate(format, locale);
   }

   public ObjectReaderImplDate(String format, Locale locale) {
      super(format, locale);
   }

   @Override
   public Class getObjectClass() {
      return Date.class;
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readDate(jsonReader);
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      return this.readDate(jsonReader);
   }

   private Object readDate(JSONReader jsonReader) {
      if (jsonReader.isInt()) {
         long millis = jsonReader.readInt64Value();
         if (this.formatUnixTime) {
            millis *= 1000L;
         }

         return new Date(millis);
      } else if (jsonReader.readIfNull()) {
         return null;
      } else if (jsonReader.nextIfNullOrEmptyString()) {
         return null;
      } else if (jsonReader.current() == 'n') {
         return jsonReader.readNullOrNewDate();
      } else if (!this.useSimpleFormatter && this.locale == null) {
         long millis;
         if ((this.formatUnixTime || this.formatMillis) && jsonReader.isString()) {
            millis = jsonReader.readInt64Value();
            if (this.formatUnixTime) {
               millis *= 1000L;
            }
         } else if (this.format == null) {
            if (jsonReader.isDate()) {
               return jsonReader.readDate();
            }

            if (jsonReader.isTypeRedirect() && jsonReader.nextIfMatchIdent('"', 'v', 'a', 'l', '"')) {
               jsonReader.nextIfMatch(':');
               millis = jsonReader.readInt64Value();
               jsonReader.nextIfObjectEnd();
               jsonReader.setTypeRedirect(false);
            } else {
               millis = jsonReader.readMillisFromString();
            }

            if (millis == 0L && jsonReader.wasNull()) {
               return null;
            }

            if (this.formatUnixTime) {
               millis *= 1000L;
            }
         } else {
            ZonedDateTime zdt;
            if (this.yyyyMMddhhmmss19) {
               if (jsonReader.isSupportSmartMatch()) {
                  millis = jsonReader.readMillisFromString();
               } else {
                  millis = jsonReader.readMillis19();
               }

               if (millis != 0L || !jsonReader.wasNull()) {
                  return new Date(millis);
               }

               zdt = jsonReader.readZonedDateTime();
            } else {
               DateTimeFormatter formatter = this.getDateFormatter(jsonReader.getLocale());
               if (formatter != null) {
                  String str = jsonReader.readString();
                  if (str.isEmpty() || "null".equals(str)) {
                     return null;
                  }

                  LocalDateTime ldt;
                  if (!this.formatHasHour) {
                     if (!this.formatHasDay) {
                        TemporalAccessor parsed = formatter.parse(str);
                        int year = parsed.get(ChronoField.YEAR);
                        int month = parsed.get(ChronoField.MONTH_OF_YEAR);
                        int dayOfYear = 1;
                        ldt = LocalDateTime.of(LocalDate.of(year, month, dayOfYear), LocalTime.MIN);
                     } else if (str.length() == 19 && jsonReader.isEnabled(JSONReader.Feature.SupportSmartMatch)) {
                        ldt = DateUtils.parseLocalDateTime(str, 0, str.length());
                     } else {
                        if (this.format.indexOf(45) != -1 && str.indexOf(45) == -1 && TypeUtils.isInteger(str)) {
                           millis = Long.parseLong(str);
                           return new Date(millis);
                        }

                        LocalDate localDate = LocalDate.parse(str, formatter);
                        ldt = LocalDateTime.of(localDate, LocalTime.MIN);
                     }
                  } else if (str.length() == 19
                     && (this.yyyyMMddhhmm16 || jsonReader.isEnabled(JSONReader.Feature.SupportSmartMatch) || "yyyy-MM-dd hh:mm:ss".equals(this.format))) {
                     int length = this.yyyyMMddhhmm16 ? 16 : 19;
                     ldt = DateUtils.parseLocalDateTime(str, 0, length);
                  } else if (this.formatHasDay) {
                     ldt = LocalDateTime.parse(str, formatter);
                  } else {
                     LocalTime localTime = LocalTime.parse(str, formatter);
                     ldt = LocalDateTime.of(LocalDate.MIN, localTime);
                  }

                  zdt = ldt.atZone(jsonReader.getContext().getZoneId());
               } else {
                  zdt = jsonReader.readZonedDateTime();
               }
            }

            if (zdt == null) {
               return null;
            }

            long seconds = zdt.toEpochSecond();
            int nanos = zdt.toLocalTime().getNano();
            if (seconds < 0L && nanos > 0) {
               millis = (seconds + 1L) * 1000L;
               long adjustment = (long)(nanos / 1000000 - 1000);
               millis += adjustment;
            } else {
               millis = seconds * 1000L;
               millis += (long)(nanos / 1000000);
            }
         }

         return new Date(millis);
      } else {
         String strx = jsonReader.readString();

         try {
            SimpleDateFormat dateFormat;
            if (this.locale != null) {
               dateFormat = new SimpleDateFormat(this.format, this.locale);
            } else {
               dateFormat = new SimpleDateFormat(this.format);
            }

            return dateFormat.parse(strx);
         } catch (ParseException var12) {
            throw new JSONException(jsonReader.info("parse error : " + strx), var12);
         }
      }
   }

   public Date createInstance(Map map, long features) {
      return TypeUtils.toDate(map);
   }
}
