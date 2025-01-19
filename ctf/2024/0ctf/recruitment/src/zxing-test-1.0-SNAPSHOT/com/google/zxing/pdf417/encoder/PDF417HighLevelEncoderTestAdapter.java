package com.google.zxing.pdf417.encoder;

import com.google.zxing.WriterException;
import java.nio.charset.Charset;

public final class PDF417HighLevelEncoderTestAdapter {
   private PDF417HighLevelEncoderTestAdapter() {
   }

   public static String encodeHighLevel(String msg, Compaction compaction, Charset encoding, boolean autoECI) throws WriterException {
      return PDF417HighLevelEncoder.encodeHighLevel(msg, compaction, encoding, autoECI);
   }
}
