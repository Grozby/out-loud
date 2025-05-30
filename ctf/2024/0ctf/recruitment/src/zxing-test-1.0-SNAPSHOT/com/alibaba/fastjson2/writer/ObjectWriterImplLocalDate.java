package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectWriterImplLocalDate extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplLocalDate INSTANCE = new ObjectWriterImplLocalDate(null, null);

   private ObjectWriterImplLocalDate(String format, Locale locale) {
      super(format, locale);
   }

   public static ObjectWriterImplLocalDate of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectWriterImplLocalDate(format, locale);
   }

   @Override
   public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (this.format != null) {
         this.write(jsonWriter, object, fieldName, fieldType, features);
      } else {
         jsonWriter.writeLocalDate((LocalDate)object);
      }
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         LocalDate date = (LocalDate)object;
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
               if (this.yyyyMMdd8) {
                  jsonWriter.writeDateYYYMMDD8(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
               } else if (this.yyyyMMdd10) {
                  jsonWriter.writeDateYYYMMDD10(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
               } else if (this.yyyyMMddhhmmss19) {
                  jsonWriter.writeDateTime19(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), 0, 0, 0);
               } else {
                  DateTimeFormatter formatter = this.getDateFormatter();
                  if (formatter == null) {
                     formatter = ctx.getDateFormatter();
                  }

                  if (formatter == null) {
                     jsonWriter.writeDateYYYMMDD10(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
                  } else {
                     String str;
                     if (!this.formatHasHour && !ctx.isDateFormatHasHour()) {
                        str = formatter.format(date);
                     } else {
                        str = formatter.format(LocalDateTime.of(date, LocalTime.MIN));
                     }

                     jsonWriter.writeString(str);
                  }
               }
            } else {
               LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.MIN);
               long millis = dateTime.atZone(ctx.getZoneId()).toInstant().toEpochMilli();
               jsonWriter.writeInt64(millis);
            }
         } else {
            LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.MIN);
            long millis = dateTime.atZone(ctx.getZoneId()).toInstant().toEpochMilli();
            jsonWriter.writeInt64(millis / 1000L);
         }
      }
   }
}
