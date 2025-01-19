package com.alibaba.fastjson2;

import com.alibaba.fastjson2.util.JDKUtils;

class JSONWriterUTF8JDK9 extends JSONWriterUTF8 {
   JSONWriterUTF8JDK9(JSONWriter.Context ctx) {
      super(ctx);
   }

   @Override
   public void writeString(String str) {
      if (str == null) {
         this.writeStringNull();
      } else {
         int coder = JDKUtils.STRING_CODER.applyAsInt(str);
         byte[] value = JDKUtils.STRING_VALUE.apply(str);
         if (coder == 0) {
            this.writeStringLatin1(value);
         } else {
            this.writeStringUTF16(value);
         }
      }
   }
}
