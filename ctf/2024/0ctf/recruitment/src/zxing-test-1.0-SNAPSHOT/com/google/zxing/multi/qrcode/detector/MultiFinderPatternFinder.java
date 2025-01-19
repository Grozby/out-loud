package com.google.zxing.multi.qrcode.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MultiFinderPatternFinder extends FinderPatternFinder {
   private static final FinderPatternInfo[] EMPTY_RESULT_ARRAY = new FinderPatternInfo[0];
   private static final FinderPattern[] EMPTY_FP_ARRAY = new FinderPattern[0];
   private static final FinderPattern[][] EMPTY_FP_2D_ARRAY = new FinderPattern[0][];
   private static final float MAX_MODULE_COUNT_PER_EDGE = 180.0F;
   private static final float MIN_MODULE_COUNT_PER_EDGE = 9.0F;
   private static final float DIFF_MODSIZE_CUTOFF_PERCENT = 0.05F;
   private static final float DIFF_MODSIZE_CUTOFF = 0.5F;

   public MultiFinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
      super(image, resultPointCallback);
   }

   private FinderPattern[][] selectMultipleBestPatterns() throws NotFoundException {
      List<FinderPattern> possibleCenters = new ArrayList<>();

      for (FinderPattern fp : this.getPossibleCenters()) {
         if (fp.getCount() >= 2) {
            possibleCenters.add(fp);
         }
      }

      int size = possibleCenters.size();
      if (size < 3) {
         throw NotFoundException.getNotFoundInstance();
      } else if (size == 3) {
         return new FinderPattern[][]{possibleCenters.toArray(EMPTY_FP_ARRAY)};
      } else {
         Collections.sort(possibleCenters, new MultiFinderPatternFinder.ModuleSizeComparator());
         List<FinderPattern[]> results = new ArrayList<>();

         for (int i1 = 0; i1 < size - 2; i1++) {
            FinderPattern p1 = possibleCenters.get(i1);
            if (p1 != null) {
               for (int i2 = i1 + 1; i2 < size - 1; i2++) {
                  FinderPattern p2 = possibleCenters.get(i2);
                  if (p2 != null) {
                     float vModSize12 = (p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize())
                        / Math.min(p1.getEstimatedModuleSize(), p2.getEstimatedModuleSize());
                     float vModSize12A = Math.abs(p1.getEstimatedModuleSize() - p2.getEstimatedModuleSize());
                     if (vModSize12A > 0.5F && vModSize12 >= 0.05F) {
                        break;
                     }

                     for (int i3 = i2 + 1; i3 < size; i3++) {
                        FinderPattern p3 = possibleCenters.get(i3);
                        if (p3 != null) {
                           float vModSize23 = (p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize())
                              / Math.min(p2.getEstimatedModuleSize(), p3.getEstimatedModuleSize());
                           float vModSize23A = Math.abs(p2.getEstimatedModuleSize() - p3.getEstimatedModuleSize());
                           if (vModSize23A > 0.5F && vModSize23 >= 0.05F) {
                              break;
                           }

                           FinderPattern[] test = new FinderPattern[]{p1, p2, p3};
                           ResultPoint.orderBestPatterns(test);
                           FinderPatternInfo info = new FinderPatternInfo(test);
                           float dA = ResultPoint.distance(info.getTopLeft(), info.getBottomLeft());
                           float dC = ResultPoint.distance(info.getTopRight(), info.getBottomLeft());
                           float dB = ResultPoint.distance(info.getTopLeft(), info.getTopRight());
                           float estimatedModuleCount = (dA + dB) / (p1.getEstimatedModuleSize() * 2.0F);
                           if (!(estimatedModuleCount > 180.0F) && !(estimatedModuleCount < 9.0F)) {
                              float vABBC = Math.abs((dA - dB) / Math.min(dA, dB));
                              if (!(vABBC >= 0.1F)) {
                                 float dCpy = (float)Math.sqrt((double)dA * (double)dA + (double)dB * (double)dB);
                                 float vPyC = Math.abs((dC - dCpy) / Math.min(dC, dCpy));
                                 if (!(vPyC >= 0.1F)) {
                                    results.add(test);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (!results.isEmpty()) {
            return results.toArray(EMPTY_FP_2D_ARRAY);
         } else {
            throw NotFoundException.getNotFoundInstance();
         }
      }
   }

   public FinderPatternInfo[] findMulti(Map<DecodeHintType, ?> hints) throws NotFoundException {
      boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
      BitMatrix image = this.getImage();
      int maxI = image.getHeight();
      int maxJ = image.getWidth();
      int iSkip = 3 * maxI / 388;
      if (iSkip < 3 || tryHarder) {
         iSkip = 3;
      }

      int[] stateCount = new int[5];

      for (int i = iSkip - 1; i < maxI; i += iSkip) {
         doClearCounts(stateCount);
         int currentState = 0;

         for (int j = 0; j < maxJ; j++) {
            if (image.get(j, i)) {
               if ((currentState & 1) == 1) {
                  currentState++;
               }

               stateCount[currentState]++;
            } else if ((currentState & 1) == 0) {
               if (currentState == 4) {
                  if (foundPatternCross(stateCount) && this.handlePossibleCenter(stateCount, i, j)) {
                     currentState = 0;
                     doClearCounts(stateCount);
                  } else {
                     doShiftCounts2(stateCount);
                     currentState = 3;
                  }
               } else {
                  currentState++;
                  stateCount[currentState]++;
               }
            } else {
               stateCount[currentState]++;
            }
         }

         if (foundPatternCross(stateCount)) {
            this.handlePossibleCenter(stateCount, i, maxJ);
         }
      }

      FinderPattern[][] patternInfo = this.selectMultipleBestPatterns();
      List<FinderPatternInfo> result = new ArrayList<>();

      for (FinderPattern[] pattern : patternInfo) {
         ResultPoint.orderBestPatterns(pattern);
         result.add(new FinderPatternInfo(pattern));
      }

      return result.isEmpty() ? EMPTY_RESULT_ARRAY : result.toArray(EMPTY_RESULT_ARRAY);
   }

   private static final class ModuleSizeComparator implements Comparator<FinderPattern>, Serializable {
      private ModuleSizeComparator() {
      }

      public int compare(FinderPattern center1, FinderPattern center2) {
         float value = center2.getEstimatedModuleSize() - center1.getEstimatedModuleSize();
         return (double)value < 0.0 ? -1 : ((double)value > 0.0 ? 1 : 0);
      }
   }
}
