package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;
import java.util.Map;

public final class UPCAReader extends UPCEANReader {
   private final UPCEANReader ean13Reader = new EAN13Reader();

   @Override
   public Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException, ChecksumException {
      return maybeReturnResult(this.ean13Reader.decodeRow(rowNumber, row, startGuardRange, hints));
   }

   @Override
   public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException, ChecksumException {
      return maybeReturnResult(this.ean13Reader.decodeRow(rowNumber, row, hints));
   }

   @Override
   public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
      return maybeReturnResult(this.ean13Reader.decode(image));
   }

   @Override
   public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, FormatException {
      return maybeReturnResult(this.ean13Reader.decode(image, hints));
   }

   @Override
   BarcodeFormat getBarcodeFormat() {
      return BarcodeFormat.UPC_A;
   }

   @Override
   protected int decodeMiddle(BitArray row, int[] startRange, StringBuilder resultString) throws NotFoundException {
      return this.ean13Reader.decodeMiddle(row, startRange, resultString);
   }

   private static Result maybeReturnResult(Result result) throws FormatException {
      String text = result.getText();
      if (text.charAt(0) == '0') {
         Result upcaResult = new Result(text.substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
         if (result.getResultMetadata() != null) {
            upcaResult.putAllMetadata(result.getResultMetadata());
         }

         return upcaResult;
      } else {
         throw FormatException.getFormatInstance();
      }
   }
}
