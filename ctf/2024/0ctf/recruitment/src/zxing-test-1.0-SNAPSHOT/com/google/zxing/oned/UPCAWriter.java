package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import java.util.Map;

public final class UPCAWriter implements Writer {
   private final EAN13Writer subWriter = new EAN13Writer();

   @Override
   public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) {
      return this.encode(contents, format, width, height, null);
   }

   @Override
   public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) {
      if (format != BarcodeFormat.UPC_A) {
         throw new IllegalArgumentException("Can only encode UPC-A, but got " + format);
      } else {
         return this.subWriter.encode('0' + contents, BarcodeFormat.EAN_13, width, height, hints);
      }
   }
}
