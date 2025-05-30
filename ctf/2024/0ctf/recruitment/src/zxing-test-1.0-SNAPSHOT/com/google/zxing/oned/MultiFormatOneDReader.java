package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.rss.RSS14Reader;
import com.google.zxing.oned.rss.expanded.RSSExpandedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public final class MultiFormatOneDReader extends OneDReader {
   private static final OneDReader[] EMPTY_ONED_ARRAY = new OneDReader[0];
   private final OneDReader[] readers;

   public MultiFormatOneDReader(Map<DecodeHintType, ?> hints) {
      Collection<BarcodeFormat> possibleFormats = hints == null ? null : (Collection)hints.get(DecodeHintType.POSSIBLE_FORMATS);
      boolean useCode39CheckDigit = hints != null && hints.get(DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT) != null;
      Collection<OneDReader> readers = new ArrayList<>();
      if (possibleFormats != null) {
         if (possibleFormats.contains(BarcodeFormat.EAN_13)
            || possibleFormats.contains(BarcodeFormat.UPC_A)
            || possibleFormats.contains(BarcodeFormat.EAN_8)
            || possibleFormats.contains(BarcodeFormat.UPC_E)) {
            readers.add(new MultiFormatUPCEANReader(hints));
         }

         if (possibleFormats.contains(BarcodeFormat.CODE_39)) {
            readers.add(new Code39Reader(useCode39CheckDigit));
         }

         if (possibleFormats.contains(BarcodeFormat.CODE_93)) {
            readers.add(new Code93Reader());
         }

         if (possibleFormats.contains(BarcodeFormat.CODE_128)) {
            readers.add(new Code128Reader());
         }

         if (possibleFormats.contains(BarcodeFormat.ITF)) {
            readers.add(new ITFReader());
         }

         if (possibleFormats.contains(BarcodeFormat.CODABAR)) {
            readers.add(new CodaBarReader());
         }

         if (possibleFormats.contains(BarcodeFormat.RSS_14)) {
            readers.add(new RSS14Reader());
         }

         if (possibleFormats.contains(BarcodeFormat.RSS_EXPANDED)) {
            readers.add(new RSSExpandedReader());
         }
      }

      if (readers.isEmpty()) {
         readers.add(new MultiFormatUPCEANReader(hints));
         readers.add(new Code39Reader());
         readers.add(new CodaBarReader());
         readers.add(new Code93Reader());
         readers.add(new Code128Reader());
         readers.add(new ITFReader());
         readers.add(new RSS14Reader());
         readers.add(new RSSExpandedReader());
      }

      this.readers = readers.toArray(EMPTY_ONED_ARRAY);
   }

   @Override
   public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException {
      for (OneDReader reader : this.readers) {
         try {
            return reader.decodeRow(rowNumber, row, hints);
         } catch (ReaderException var9) {
         }
      }

      throw NotFoundException.getNotFoundInstance();
   }

   @Override
   public void reset() {
      for (Reader reader : this.readers) {
         reader.reset();
      }
   }
}
