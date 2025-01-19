package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.util.DoubleToDecimal;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

final class CSVWriterUTF16 extends CSVWriter {
   static final char[] BYTES_TRUE = "true".toCharArray();
   static final char[] BYTES_FALSE = "false".toCharArray();
   final Writer out;
   final char[] chars;

   CSVWriterUTF16(Writer out, ZoneId zoneId, CSVWriter.Feature... features) {
      super(zoneId, features);
      this.out = out;
      this.chars = new char[65536];
   }

   void writeDirect(char[] bytes, int off, int len) {
      try {
         this.out.write(bytes, off, len);
      } catch (IOException var5) {
         throw new JSONException("write csv error", var5);
      }
   }

   @Override
   public void writeComma() {
      if (this.off + 1 == this.chars.length) {
         this.flush();
      }

      this.chars[this.off++] = ',';
   }

   @Override
   protected void writeQuote() {
      if (this.off + 1 == this.chars.length) {
         this.flush();
      }

      this.chars[this.off++] = '"';
   }

   @Override
   public void writeLine() {
      if (this.off + 1 == this.chars.length) {
         this.flush();
      }

      this.chars[this.off++] = '\n';
   }

   @Override
   public void writeBoolean(boolean booleanValue) {
      char[] valueBytes = booleanValue ? BYTES_TRUE : BYTES_FALSE;
      this.writeRaw(valueBytes);
   }

   @Override
   public void writeInt64(long longValue) {
      int minCapacity = this.off + 21;
      if (minCapacity - this.chars.length > 0) {
         this.flush();
      }

      this.off = IOUtils.writeInt64(this.chars, this.off, longValue);
   }

   @Override
   public void writeDateYYYMMDD10(int year, int month, int dayOfMonth) {
      if (this.off + 11 >= this.chars.length) {
         this.flush();
      }

      this.off = IOUtils.writeLocalDate(this.chars, this.off, year, month, dayOfMonth);
   }

   @Override
   public void writeDateTime19(int year, int month, int dayOfMonth, int hour, int minute, int second) {
      if (this.off + 20 >= this.chars.length) {
         this.flush();
      }

      char[] chars = this.chars;
      int off = this.off;
      off = IOUtils.writeLocalDate(chars, off, year, month, dayOfMonth);
      chars[off] = ' ';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 1 << 1), IOUtils.PACKED_DIGITS_UTF16[hour]);
      chars[off + 3] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 4 << 1), IOUtils.PACKED_DIGITS_UTF16[minute]);
      chars[off + 6] = ':';
      JDKUtils.UNSAFE.putInt(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + (long)(off + 7 << 1), IOUtils.PACKED_DIGITS_UTF16[second]);
      this.off = off + 9;
   }

   @Override
   public void writeString(String str) {
      if (str != null && !str.isEmpty()) {
         int len = str.length();
         int escapeCount = 0;
         boolean comma = false;
         if (str.charAt(0) == '"') {
            for (int i = 0; i < len; i++) {
               char ch = str.charAt(i);
               if (ch == '"') {
                  escapeCount++;
               }
            }
         } else {
            for (int ix = 0; ix < len; ix++) {
               char ch = str.charAt(ix);
               if (ch == ',') {
                  comma = true;
               } else if (ch == '"') {
                  escapeCount++;
               }
            }

            if (!comma) {
               escapeCount = 0;
            }
         }

         if (escapeCount == 0) {
            str.getChars(0, str.length(), this.chars, this.off);
            this.off = this.off + str.length();
         } else {
            if (this.off + 2 + str.length() + escapeCount >= this.chars.length) {
               this.flush();
            }

            this.chars[this.off++] = '"';

            for (int ixx = 0; ixx < str.length(); ixx++) {
               char ch = str.charAt(ixx);
               if (ch == '"') {
                  this.chars[this.off++] = '"';
                  this.chars[this.off++] = '"';
               } else {
                  this.chars[this.off++] = ch;
               }
            }

            this.chars[this.off++] = '"';
         }
      }
   }

   @Override
   public void writeInt32(int intValue) {
      int minCapacity = this.off + 11;
      if (minCapacity - this.chars.length > 0) {
         this.flush();
      }

      this.off = IOUtils.writeInt32(this.chars, this.off, intValue);
   }

   @Override
   public void writeDouble(double value) {
      int minCapacity = this.off + 24;
      if (minCapacity - this.chars.length > 0) {
         this.flush();
      }

      int size = DoubleToDecimal.toString(value, this.chars, this.off, true);
      this.off += size;
   }

   @Override
   public void writeFloat(float value) {
      int minCapacity = this.off + 15;
      if (minCapacity - this.chars.length > 0) {
         this.flush();
      }

      int size = DoubleToDecimal.toString(value, this.chars, this.off, true);
      this.off += size;
   }

   @Override
   public void flush() {
      try {
         this.out.write(this.chars, 0, this.off);
         this.off = 0;
         this.out.flush();
      } catch (IOException var2) {
         throw new JSONException("write csv error", var2);
      }
   }

   @Override
   public void writeString(byte[] utf8) {
      if (utf8 != null && utf8.length != 0) {
         String str = new String(utf8, 0, utf8.length, StandardCharsets.UTF_8);
         this.writeString(str);
      }
   }

   @Override
   public void writeDecimal(BigDecimal value) {
      if (value != null) {
         String str = value.toString();
         int strlen = str.length();
         int minCapacity = this.off + 24;
         if (minCapacity - this.chars.length > 0) {
            this.flush();
         }

         str.getChars(0, strlen, this.chars, this.off);
         this.off += strlen;
      }
   }

   @Override
   public void writeDecimal(long unscaledVal, int scale) {
      if (scale == 0) {
         this.writeInt64(unscaledVal);
      } else if (unscaledVal != Long.MIN_VALUE && scale < 20 && scale >= 0) {
         int minCapacity = this.off + 24;
         if (minCapacity - this.chars.length > 0) {
            this.flush();
         }

         this.off = IOUtils.writeDecimal(this.chars, this.off, unscaledVal, scale);
      } else {
         this.writeDecimal(BigDecimal.valueOf(unscaledVal, scale));
      }
   }

   void writeRaw(char[] chars) {
      if (chars.length + this.off < this.chars.length) {
         System.arraycopy(chars, 0, this.chars, this.off, chars.length);
         this.off += chars.length;
      } else {
         this.flush();
         if (chars.length >= this.chars.length) {
            this.writeDirect(chars, 0, chars.length);
         } else {
            System.arraycopy(chars, 0, this.chars, this.off, chars.length);
            this.off += chars.length;
         }
      }
   }

   @Override
   public void writeLocalDateTime(LocalDateTime ldt) {
      if (ldt != null) {
         this.off = IOUtils.writeLocalDate(this.chars, this.off, ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth());
         this.chars[this.off++] = ' ';
         this.off = IOUtils.writeLocalTime(this.chars, this.off, ldt.toLocalTime());
      }
   }

   @Override
   protected void writeRaw(String str) {
      if (str != null && !str.isEmpty()) {
         if (str.length() + this.off >= this.chars.length) {
            this.flush();
         }

         str.getChars(0, str.length(), this.chars, this.off);
         this.off = this.off + str.length();
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
      if (this.out instanceof StringWriter) {
         this.flush();
         return this.out.toString();
      } else {
         return super.toString();
      }
   }
}
