package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.JDKUtils;

public final class JSONWriterUTF16JDK8UF extends JSONWriterUTF16 {
   JSONWriterUTF16JDK8UF(JSONWriter.Context ctx) {
      super(ctx);
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeStringNull();
      } else {
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         char[] value = (char[])JDKUtils.UNSAFE.getObject(str, JDKUtils.FIELD_STRING_VALUE_OFFSET);
         int strlen = value.length;
         boolean escape = false;

         for (int i = 0; i < value.length; i++) {
            char ch = value[i];
            if (ch == this.quote
               || ch == '\\'
               || ch < ' '
               || browserSecure && (ch == '<' || ch == '>' || ch == '(' || ch == ')')
               || escapeNoneAscii && ch > 127) {
               escape = true;
               break;
            }
         }

         if (!escape) {
            int off = this.off;
            int minCapacity = off + strlen + 2;
            if (minCapacity >= this.chars.length) {
               this.ensureCapacity(minCapacity);
            }

            char[] chars = this.chars;
            chars[off++] = this.quote;
            System.arraycopy(value, 0, chars, off, value.length);
            off += strlen;
            chars[off] = this.quote;
            this.off = off + 1;
         } else {
            this.writeStringEscape(str);
         }
      }
   }
}
