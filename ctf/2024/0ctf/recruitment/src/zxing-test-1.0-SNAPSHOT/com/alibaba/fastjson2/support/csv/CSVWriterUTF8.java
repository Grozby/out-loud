package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.DoubleToDecimal;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

final class CSVWriterUTF8 extends CSVWriter {
   static final byte[] BYTES_TRUE = "true".getBytes();
   static final byte[] BYTES_FALSE = "false".getBytes();
   static final byte[] BYTES_LONG_MIN = "-9223372036854775808".getBytes();
   final OutputStream out;
   final Charset charset;
   final byte[] bytes;

   CSVWriterUTF8(OutputStream out, Charset charset, ZoneId zoneId, CSVWriter.Feature... features) {
      super(zoneId, features);
      this.out = out;
      this.charset = charset;
      this.bytes = new byte[65536];
   }

   void writeDirect(byte[] bytes, int off, int len) {
      try {
         this.out.write(bytes, off, len);
      } catch (IOException var5) {
         throw new JSONException("write csv error", var5);
      }
   }

   @Override
   public void writeComma() {
      if (this.off + 1 == this.bytes.length) {
         this.flush();
      }

      this.bytes[this.off++] = 44;
   }

   @Override
   protected void writeQuote() {
      if (this.off + 1 == this.bytes.length) {
         this.flush();
      }

      this.bytes[this.off++] = 34;
   }

   @Override
   public void writeLine() {
      if (this.off + 1 == this.bytes.length) {
         this.flush();
      }

      this.bytes[this.off++] = 10;
   }

   @Override
   public void writeBoolean(boolean booleanValue) {
      byte[] valueBytes = booleanValue ? BYTES_TRUE : BYTES_FALSE;
      this.writeRaw(valueBytes);
   }

   @Override
   public void writeInt64(long longValue) {
      int minCapacity = this.off + 21;
      if (minCapacity - this.bytes.length > 0) {
         this.flush();
      }

      this.off = IOUtils.writeInt64(this.bytes, this.off, longValue);
   }

   @Override
   public void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
      if (this.off + 11 >= this.bytes.length) {
         this.flush();
      }

      this.off = IOUtils.writeLocalDate(this.bytes, this.off, year, month, dayOfMonth);
   }

   @Override
   public void writeDateTime19(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      if (this.off + 20 >= this.bytes.length) {
         this.flush();
      }

      byte[] bytes = this.bytes;
      int off = this.off;
      off = IOUtils.writeLocalDate(bytes, off, year, month, dayOfMonth);
      bytes[off] = 32;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 1L, IOUtils.PACKED_DIGITS[hour]);
      bytes[off + 3] = 58;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 4L, IOUtils.PACKED_DIGITS[minute]);
      bytes[off + 6] = 58;
      JDKUtils.UNSAFE.putShort(bytes, JDKUtils.ARRAY_BYTE_BASE_OFFSET + (long)off + 7L, IOUtils.PACKED_DIGITS[second]);
      this.off = off + 9;
   }

   @Override
   public void writeString(String value) {
      byte[] bytes;
      if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_VALUE != null && JDKUtils.STRING_CODER.applyAsInt(value) == JDKUtils.LATIN1) {
         bytes = JDKUtils.STRING_VALUE.apply(value);
      } else {
         bytes = value.getBytes(this.charset);
      }

      this.writeString(bytes);
   }

   @Override
   public void writeInt32(int intValue) {
      int minCapacity = this.off + 11;
      if (minCapacity - this.bytes.length > 0) {
         this.flush();
      }

      this.off = IOUtils.writeInt32(this.bytes, this.off, intValue);
   }

   @Override
   public void writeDouble(double value) {
      int minCapacity = this.off + 24;
      if (minCapacity - this.bytes.length > 0) {
         this.flush();
      }

      int size = DoubleToDecimal.toString(value, this.bytes, this.off, true);
      this.off += size;
   }

   @Override
   public void writeFloat(float value) {
      int minCapacity = this.off + 15;
      if (minCapacity - this.bytes.length > 0) {
         this.flush();
      }

      int size = DoubleToDecimal.toString(value, this.bytes, this.off, true);
      this.off += size;
   }

   @Override
   public void flush() {
      try {
         this.out.write(this.bytes, 0, this.off);
         this.off = 0;
         this.out.flush();
      } catch (IOException var2) {
         throw new JSONException("write csv error", var2);
      }
   }

   @Override
   public void writeString(byte[] utf8) {
      if (utf8 != null && utf8.length != 0) {
         int len = utf8.length;
         int escapeCount = 0;
         boolean comma = false;
         if (utf8[0] == 34) {
            for (byte ch : utf8) {
               if (ch == 34) {
                  escapeCount++;
               }
            }
         } else {
            for (byte chx : utf8) {
               if (chx == 44) {
                  comma = true;
               } else if (chx == 34) {
                  escapeCount++;
               }
            }

            if (!comma) {
               escapeCount = 0;
            }
         }

         if (escapeCount == 0) {
            this.writeRaw(utf8);
         } else {
            if (this.off + 2 + utf8.length + escapeCount >= this.bytes.length) {
               this.flush();
            }

            this.bytes[this.off++] = 34;

            for (byte chxx : utf8) {
               if (chxx == 34) {
                  this.bytes[this.off++] = 34;
                  this.bytes[this.off++] = 34;
               } else {
                  this.bytes[this.off++] = chxx;
               }
            }

            this.bytes[this.off++] = 34;
         }
      }
   }

   @Override
   public void writeDecimal(BigDecimal value) {
      if (value != null) {
         String str = value.toString();
         int strlen = str.length();
         int minCapacity = this.off + 24;
         if (minCapacity - this.bytes.length > 0) {
            this.flush();
         }

         str.getBytes(0, strlen, this.bytes, this.off);
         this.off += strlen;
      }
   }

   @Override
   public void writeDecimal(long unscaledVal, int scale) {
      if (scale == 0) {
         this.writeInt64(unscaledVal);
      } else if (unscaledVal != Long.MIN_VALUE && scale < 20 && scale >= 0) {
         int minCapacity = this.off + 24;
         if (minCapacity - this.bytes.length > 0) {
            this.flush();
         }

         this.off = IOUtils.writeDecimal(this.bytes, this.off, unscaledVal, scale);
      } else {
         this.writeDecimal(BigDecimal.valueOf(unscaledVal, scale));
      }
   }

   private void writeRaw(byte[] strBytes) {
      if (strBytes.length + this.off < this.bytes.length) {
         System.arraycopy(strBytes, 0, this.bytes, this.off, strBytes.length);
         this.off += strBytes.length;
      } else {
         this.flush();
         if (strBytes.length >= this.bytes.length) {
            this.writeDirect(strBytes, 0, strBytes.length);
         } else {
            System.arraycopy(strBytes, 0, this.bytes, this.off, strBytes.length);
            this.off += strBytes.length;
         }
      }
   }

   @Override
   protected void writeRaw(String str) {
      if (str != null && !str.isEmpty()) {
         byte[] strBytes = str.getBytes(this.charset);
         if (strBytes.length + this.off < this.bytes.length) {
            System.arraycopy(strBytes, 0, this.bytes, this.off, strBytes.length);
            this.off += strBytes.length;
         } else {
            this.flush();
            if (strBytes.length >= this.bytes.length) {
               this.writeDirect(strBytes, 0, strBytes.length);
            } else {
               System.arraycopy(strBytes, 0, this.bytes, this.off, strBytes.length);
               this.off += strBytes.length;
            }
         }
      }
   }

   @Override
   public void writeLocalDateTime(LocalDateTime ldt) {
      if (ldt != null) {
         this.off = IOUtils.writeLocalDate(this.bytes, this.off, ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth());
         this.bytes[this.off++] = 32;
         this.off = IOUtils.writeLocalTime(this.bytes, this.off, ldt.toLocalTime());
      }
   }

   @Override
   public void close() throws IOException {
      if (this.off > 0) {
         this.flush();
      }

      this.out.close();
   }

   @Override
   public String toString() {
      if (this.out instanceof ByteArrayOutputStream) {
         this.flush();
         byte[] strBytes = ((ByteArrayOutputStream)this.out).toByteArray();
         return new String(strBytes, StandardCharsets.UTF_8);
      } else {
         return super.toString();
      }
   }
}
