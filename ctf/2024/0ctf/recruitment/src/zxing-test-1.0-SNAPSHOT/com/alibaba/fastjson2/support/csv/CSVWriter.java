package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

public abstract class CSVWriter implements Closeable, Flushable {
   private long features;
   final ZoneId zoneId;
   int off;

   CSVWriter(ZoneId zoneId, CSVWriter.Feature... features) {
      for (CSVWriter.Feature feature : features) {
         this.features = this.features | feature.mask;
      }

      this.zoneId = zoneId;
   }

   public static CSVWriter of() {
      return of(new ByteArrayOutputStream());
   }

   public static CSVWriter of(File file) throws FileNotFoundException {
      return of(new FileOutputStream(file), StandardCharsets.UTF_8);
   }

   public static CSVWriter of(File file, Charset charset) throws FileNotFoundException {
      return of(new FileOutputStream(file), charset);
   }

   public final void writeLineObject(Object object) {
      if (object == null) {
         this.writeLine();
      } else {
         ObjectWriterProvider provider = JSONFactory.getDefaultObjectWriterProvider();
         Class<?> objectClass = object.getClass();
         ObjectWriter objectWriter = provider.getObjectWriter(objectClass);
         if (objectWriter instanceof ObjectWriterAdapter) {
            ObjectWriterAdapter adapter = (ObjectWriterAdapter)objectWriter;
            List<FieldWriter> fieldWriters = adapter.getFieldWriters();
            if (fieldWriters.size() == 1 && (fieldWriters.get(0).features & 281474976710656L) != 0L) {
               Object fieldValue = fieldWriters.get(0).getFieldValue(object);
               this.writeLineObject(fieldValue);
               return;
            }

            Object[] values = new Object[fieldWriters.size()];

            for (int i = 0; i < fieldWriters.size(); i++) {
               values[i] = fieldWriters.get(i).getFieldValue(object);
            }

            this.writeLine(values);
         } else {
            this.writeLine(object);
         }
      }
   }

   public final void writeDate(Date date) {
      if (date != null) {
         long millis = date.getTime();
         this.writeDate(millis);
      }
   }

   public final void writeInstant(Instant instant) {
      if (instant != null) {
         int nano = instant.getNano();
         if (nano % 1000000 == 0) {
            long millis = instant.toEpochMilli();
            this.writeDate(millis);
         } else {
            if ((this.features & CSVWriter.Feature.AlwaysQuoteStrings.mask) != 0L) {
               this.writeQuote();
            }

            LocalDateTime ldt = instant.atZone(this.zoneId).toLocalDateTime();
            this.writeLocalDateTime(ldt);
         }
      }
   }

   public void writeLocalDate(LocalDate date) {
      if (date != null) {
         String str = DateTimeFormatter.ISO_LOCAL_DATE.format(date);
         this.writeRaw(str);
      }
   }

   public abstract void writeLocalDateTime(LocalDateTime var1);

   public final void writeLine(int columnCount, IntFunction function) {
      for (int i = 0; i < columnCount; i++) {
         Object value = function.apply(i);
         if (i != 0) {
            this.writeComma();
         }

         this.writeValue(value);
      }

      this.writeLine();
   }

   public final void writeLine(List values) {
      for (int i = 0; i < values.size(); i++) {
         if (i != 0) {
            this.writeComma();
         }

         this.writeValue(values.get(i));
      }

      this.writeLine();
   }

   public final void writeLine(Object... values) {
      for (int i = 0; i < values.length; i++) {
         if (i != 0) {
            this.writeComma();
         }

         this.writeValue(values[i]);
      }

      this.writeLine();
   }

   public abstract void writeComma();

   protected abstract void writeQuote();

   public abstract void writeLine();

   public void writeValue(Object value) {
      if (value != null) {
         if (value instanceof Optional) {
            Optional optional = (Optional)value;
            if (!optional.isPresent()) {
               return;
            }

            value = optional.get();
         }

         if (value instanceof Integer) {
            this.writeInt32((Integer)value);
         } else if (value instanceof Long) {
            this.writeInt64((Long)value);
         } else if (value instanceof String) {
            this.writeString((String)value);
         } else if (value instanceof Boolean) {
            boolean booleanValue = (Boolean)value;
            this.writeBoolean(booleanValue);
         } else if (value instanceof Float) {
            float floatValue = (Float)value;
            this.writeFloat(floatValue);
         } else if (value instanceof Double) {
            this.writeDouble((Double)value);
         } else if (value instanceof Short) {
            this.writeInt32(((Short)value).intValue());
         } else if (value instanceof Byte) {
            this.writeInt32(((Byte)value).intValue());
         } else if (value instanceof BigDecimal) {
            this.writeDecimal((BigDecimal)value);
         } else if (value instanceof BigInteger) {
            this.writeBigInteger((BigInteger)value);
         } else if (value instanceof Date) {
            this.writeDate((Date)value);
         } else if (value instanceof Instant) {
            this.writeInstant((Instant)value);
         } else if (value instanceof LocalDate) {
            this.writeLocalDate((LocalDate)value);
         } else if (value instanceof LocalDateTime) {
            this.writeLocalDateTime((LocalDateTime)value);
         } else {
            String str = value.toString();
            this.writeString(str);
         }
      }
   }

   public void writeBigInteger(BigInteger value) {
      if (value != null) {
         String str = value.toString();
         this.writeRaw(str);
      }
   }

   public abstract void writeBoolean(boolean var1);

   public abstract void writeInt64(long var1);

   public final void writeDate(long millis) {
      ZoneId zoneId = this.zoneId;
      int SECONDS_PER_DAY = 86400;
      long epochSecond = Math.floorDiv(millis, 1000L);
      int offsetTotalSeconds;
      if (zoneId != DateUtils.SHANGHAI_ZONE_ID && zoneId.getRules() != DateUtils.SHANGHAI_ZONE_RULES) {
         Instant instant = Instant.ofEpochMilli(millis);
         offsetTotalSeconds = zoneId.getRules().getOffset(instant).getTotalSeconds();
      } else {
         offsetTotalSeconds = DateUtils.getShanghaiZoneOffsetTotalSeconds(epochSecond);
      }

      long localSecond = epochSecond + (long)offsetTotalSeconds;
      long localEpochDay = Math.floorDiv(localSecond, 86400L);
      int secsOfDay = (int)Math.floorMod(localSecond, 86400L);
      int DAYS_PER_CYCLE = 146097;
      long DAYS_0000_TO_1970 = 719528L;
      long zeroDay = localEpochDay + 719528L;
      zeroDay -= 60L;
      long adjust = 0L;
      if (zeroDay < 0L) {
         long adjustCycles = (zeroDay + 1L) / 146097L - 1L;
         adjust = adjustCycles * 400L;
         zeroDay += -adjustCycles * 146097L;
      }

      long yearEst = (400L * zeroDay + 591L) / 146097L;
      long doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      if (doyEst < 0L) {
         yearEst--;
         doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L);
      }

      yearEst += adjust;
      int marchDoy0 = (int)doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dayOfMonth = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += (long)(marchMonth0 / 10);
      if (yearEst >= -999999999L && yearEst <= 999999999L) {
         int year = (int)yearEst;
         int MINUTES_PER_HOUR = 60;
         int SECONDS_PER_MINUTE = 60;
         int SECONDS_PER_HOUR = 3600;
         long secondOfDay = (long)secsOfDay;
         if (secondOfDay >= 0L && secondOfDay <= 86399L) {
            int hours = (int)(secondOfDay / 3600L);
            secondOfDay -= (long)(hours * 3600);
            int minutes = (int)(secondOfDay / 60L);
            secondOfDay -= (long)(minutes * 60);
            int second = (int)secondOfDay;
            if (year >= 0 && year <= 9999) {
               int mos = (int)Math.floorMod(millis, 1000L);
               if (mos == 0) {
                  if (hours == 0 && minutes == 0 && second == 0) {
                     this.writeDateYYYMMDD10(year, month, dayOfMonth);
                  } else {
                     this.writeDateTime19(year, month, dayOfMonth, hours, minutes, second);
                  }

                  return;
               }
            }

            String str = DateUtils.toString(millis, false, zoneId);
            this.writeRaw(str);
         } else {
            throw new DateTimeException("Invalid secondOfDay " + secondOfDay);
         }
      } else {
         throw new DateTimeException("Invalid year " + yearEst);
      }
   }

   public abstract void writeDateYYYMMDD10(int var1, int var2, int var3);

   public abstract void writeDateTime19(int var1, int var2, int var3, int var4, int var5, int var6);

   public abstract void writeString(String var1);

   public abstract void writeInt32(int var1);

   public abstract void writeDouble(double var1);

   public abstract void writeFloat(float var1);

   @Override
   public abstract void flush();

   public abstract void writeString(byte[] var1);

   public abstract void writeDecimal(BigDecimal var1);

   public abstract void writeDecimal(long var1, int var3);

   protected abstract void writeRaw(String var1);

   @Override
   public abstract void close() throws IOException;

   public static CSVWriter of(OutputStream out, CSVWriter.Feature... features) {
      return new CSVWriterUTF8(out, StandardCharsets.UTF_8, DateUtils.DEFAULT_ZONE_ID, features);
   }

   public static CSVWriter of(OutputStream out, Charset charset) {
      return of(out, charset, DateUtils.DEFAULT_ZONE_ID);
   }

   public static CSVWriter of(OutputStream out, Charset charset, ZoneId zoneId) {
      if (charset != StandardCharsets.UTF_16 && charset != StandardCharsets.UTF_16LE && charset != StandardCharsets.UTF_16BE) {
         if (charset == null) {
            charset = StandardCharsets.UTF_8;
         }

         return new CSVWriterUTF8(out, charset, zoneId);
      } else {
         return of(new OutputStreamWriter(out, charset), zoneId);
      }
   }

   public static CSVWriter of(Writer out) {
      return new CSVWriterUTF16(out, DateUtils.DEFAULT_ZONE_ID);
   }

   public static CSVWriter of(Writer out, ZoneId zoneId) {
      return new CSVWriterUTF16(out, zoneId);
   }

   public static enum Feature {
      AlwaysQuoteStrings(1L);

      public final long mask;

      private Feature(long mask) {
         this.mask = mask;
      }
   }
}
