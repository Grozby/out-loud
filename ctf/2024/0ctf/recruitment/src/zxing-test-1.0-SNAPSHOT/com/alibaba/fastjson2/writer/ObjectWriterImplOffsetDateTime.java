package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.codec.DateTimeCodec;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ObjectWriterImplOffsetDateTime extends DateTimeCodec implements ObjectWriter {
   static final ObjectWriterImplOffsetDateTime INSTANCE = new ObjectWriterImplOffsetDateTime(null, null);

   private ObjectWriterImplOffsetDateTime(String format, Locale locale) {
      super(format, locale);
   }

   public static ObjectWriterImplOffsetDateTime of(String format, Locale locale) {
      return format == null ? INSTANCE : new ObjectWriterImplOffsetDateTime(format, locale);
   }

   @Override
   public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
      if (object == null) {
         jsonWriter.writeNull();
      } else {
         JSONWriter.Context ctx = jsonWriter.context;
         OffsetDateTime odt = (OffsetDateTime)object;
         if (!this.formatUnixTime && (this.format != null || !ctx.isDateFormatUnixTime())) {
            if (!this.formatMillis && (this.format != null || !ctx.isDateFormatMillis())) {
               int year = odt.getYear();
               if (year >= 0 && year <= 9999) {
                  if (this.formatISO8601 || ctx.isDateFormatISO8601()) {
                     jsonWriter.writeDateTimeISO8601(
                        year,
                        odt.getMonthValue(),
                        odt.getDayOfMonth(),
                        odt.getHour(),
                        odt.getMinute(),
                        odt.getSecond(),
                        odt.getNano() / 1000000,
                        odt.getOffset().getTotalSeconds(),
                        true
                     );
                     return;
                  }

                  if (this.yyyyMMddhhmmss19) {
                     jsonWriter.writeDateTime19(year, odt.getMonthValue(), odt.getDayOfMonth(), odt.getHour(), odt.getMinute(), odt.getSecond());
                     return;
                  }

                  if (this.yyyyMMddhhmmss14) {
                     jsonWriter.writeDateTime14(year, odt.getMonthValue(), odt.getDayOfMonth(), odt.getHour(), odt.getMinute(), odt.getSecond());
                     return;
                  }
               }

               DateTimeFormatter formatter = this.getDateFormatter();
               if (formatter == null) {
                  formatter = ctx.getDateFormatter();
               }

               if (formatter == null) {
                  jsonWriter.writeOffsetDateTime(odt);
               } else {
                  String str = formatter.format(odt);
                  jsonWriter.writeString(str);
               }
            } else {
               long millis = odt.toInstant().toEpochMilli();
               jsonWriter.writeInt64(millis);
            }
         } else {
            long millis = odt.toInstant().toEpochMilli();
            jsonWriter.writeInt64(millis / 1000L);
         }
      }
   }
}
