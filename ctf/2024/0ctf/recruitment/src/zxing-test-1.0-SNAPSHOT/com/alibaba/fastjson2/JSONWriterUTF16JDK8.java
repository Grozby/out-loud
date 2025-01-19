package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.JDKUtils;

final class JSONWriterUTF16JDK8 extends JSONWriterUTF16 {
   JSONWriterUTF16JDK8(JSONWriter.Context ctx) {
      super(ctx);
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         if (this.isEnabled(JSONWriter.Feature.NullAsDefaultValue.mask | JSONWriter.Feature.WriteNullStringAsEmpty.mask)) {
            this.writeString("");
         } else {
            this.writeNull();
         }
      } else {
         boolean browserSecure = (this.context.features & JSONWriter.Feature.BrowserSecure.mask) != 0L;
         boolean escapeNoneAscii = (this.context.features & JSONWriter.Feature.EscapeNoneAscii.mask) != 0L;
         char[] value = JDKUtils.getCharArray(str);
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
            int minCapacity = this.off + strlen + 2;
            if (minCapacity >= this.chars.length) {
               this.ensureCapacity(minCapacity);
            }

            this.chars[this.off++] = this.quote;
            System.arraycopy(value, 0, this.chars, this.off, value.length);
            this.off += strlen;
            this.chars[this.off++] = this.quote;
         } else {
            this.writeStringEscape(str);
         }
      }
   }
}
