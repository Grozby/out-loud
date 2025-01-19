package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

final class ObjectWriterImplZonedDateTime extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplZonedDateTime INSTANCE = new ObjectWriterImplZonedDateTime(null, null);
   private final Function function;

   public ObjectWriterImplZonedDateTime(String format, Locale locale) {
      this(format, locale, null);
   }

   public ObjectWriterImplZonedDateTime(String format, Locale locale, Function function) {
      super(format, locale);
      this.function = function;
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      ZonedDateTime zdt;
      if (this.function != null) {
         zdt = (ZonedDateTime)this.function.apply(object);
      } else {
         zdt = (ZonedDateTime)object;
      }

      jsonWriter.writeZonedDateTime(zdt);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         ZonedDateTime zdt;
         if (this.function != null) {
            zdt = (ZonedDateTime)this.function.apply(object);
         } else {
            zdt = (ZonedDateTime)object;
         }

         JSONWriter.Context ctx = jsonWriter.context;
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis() || ctx.provider.namingStrategy == PropertyNamingStrategy.CamelCase1x)) {
               int year = zdt.getYear();
               if (year >= 0 && year <= 9999) {
                  if (this.formatISO8601 || ctx.isDateFormatISO8601()) {
                     jsonWriter.writeDateTimeISO8601(
                        year,
                        zdt.getMonthValue(),
                        zdt.getDayOfMonth(),
                        zdt.getHour(),
                        zdt.getMinute(),
                        zdt.getSecond(),
                        zdt.getNano() / 1000000,
                        zdt.getOffset().getTotalSeconds(),
                        true
                     );
                     return;
                  }

                  if (this.yyyyMMddhhmmss19) {
                     jsonWriter.writeDateTime19(year, zdt.getMonthValue(), zdt.getDayOfMonth(), zdt.getHour(), zdt.getMinute(), zdt.getSecond());
                     return;
                  }

                  if (this.yyyyMMddhhmmss14) {
                     jsonWriter.writeDateTime14(year, zdt.getMonthValue(), zdt.getDayOfMonth(), zdt.getHour(), zdt.getMinute(), zdt.getSecond());
                     return;
                  }
               }

               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter == null) {
                  formatter = ctx.getDateFormatter();
               }

               if (formatter == null) {
                  jsonWriter.writeZonedDateTime(zdt);
               } else {
                  String str = formatter.format(zdt);
                  jsonWriter.writeString(str);
               }
            } else {
               jsonWriter.writeInt64(zdt.toInstant().toEpochMilli());
            }
         } else {
            long millis = zdt.toInstant().toEpochMilli();
            jsonWriter.writeInt64(millis / 1000L);
         }
      }
   }
}
