package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import sun.misc.Unsafe;

final class JSONWriterUTF16JDK9UF extends JSONWriterUTF16 {
   JSONWriterUTF16JDK9UF(JSONWriter.Context ctx) {
      super(ctx);
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeStringNull();
      } else {
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escape = false;
         char quote = this.quote;
         int strlen = str.length();
         int minCapacity = this.off + strlen + 2;
         if (minCapacity >= this.chars.length) {
            this.ensureCapacity(minCapacity);
         }

         int coder = JDKUtils.STRING_CODER.applyAsInt(str);
         byte[] value = JDKUtils.STRING_VALUE.apply(str);
         int off = this.off;
         char[] chars = this.chars;
         chars[off++] = quote;

         for (int i = 0; i < strlen; i++) {
            int c;
            if (coder == 0) {
               c = value[i];
            } else {
               c = JDKUtils.UNSAFE.getChar(str, (long)Unsafe.ARRAY_CHAR_BASE_OFFSET + (long)(i * 2));
            }

            if (c == 92 || c == quote || c < 32 || browserSecure && (c == 60 || c == 62 || c == 40 || c == 41) || escapeNoneAscii && c > 127) {
               escape = true;
               break;
            }

            chars[off++] = (char)c;
         }

         if (!escape) {
            chars[off++] = quote;
            this.off = off;
         } else {
            this.writeStringEscape(str);
         }
      }
   }

   @Override
   public void writeBool(boolean value) {
      int minCapacity = this.off + 5;
      if (minCapacity >= this.chars.length) {
         this.ensureCapacity(minCapacity);
      }

      char[] chars = this.chars;
      int off = this.off;
      if ((this.context.features & JSONWriter.Feature.WriteBooleanAsNumber.mask) != 0L) {
         chars[off++] = (char)(value ? 49 : 48);
      } else {
         if (!value) {
            chars[off++] = 'f';
         }

         JDKUtils.UNSAFE.putLong(chars, JDKUtils.ARRAY_CHAR_BASE_OFFSET + ((long)off << 1), value ? IOUtils.TRUE_64 : IOUtils.ALSE_64);
         off += 4;
      }

      this.off = off;
   }
}
