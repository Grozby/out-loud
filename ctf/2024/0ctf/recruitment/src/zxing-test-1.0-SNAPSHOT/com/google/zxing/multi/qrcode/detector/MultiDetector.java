package com.google.zxing.multi.qrcode.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.qrcode.detector.Detector;
import com.google.zxing.qrcode.detector.FinderPatternInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MultiDetector extends Detector {
   private static final DetectorResult[] EMPTY_DETECTOR_RESULTS = new DetectorResult[0];

   public MultiDetector(BitMatrix image) {
      super(image);
   }

   public DetectorResult[] detectMulti(Map<DecodeHintType, ?> hints) throws NotFoundException {
      BitMatrix image = this.getImage();
      ResultPointCallback resultPointCallback = hints == null ? null : (ResultPointCallback)hints.get(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
      MultiFinderPatternFinder finder = new MultiFinderPatternFinder(image, resultPointCallback);
      FinderPatternInfo[] infos = finder.findMulti(hints);
      if (infos.length == 0) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         List<DetectorResult> result = new ArrayList<>();

         for (FinderPatternInfo info : infos) {
            try {
               result.add(this.processFinderPatternInfo(info));
            } catch (ReaderException var12) {
            }
         }

         return result.isEmpty() ? EMPTY_DETECTOR_RESULTS : result.toArray(EMPTY_DETECTOR_RESULTS);
      }
   }
}
