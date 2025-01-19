package com.google.zxing.qrcode.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FinderPatternFinder {
   private static final int CENTER_QUORUM = 2;
   private static final FinderPatternFinder.EstimatedModuleComparator moduleComparator = new FinderPatternFinder.EstimatedModuleComparator();
   protected static final int MIN_SKIP = 3;
   protected static final int MAX_MODULES = 97;
   private final BitMatrix image;
   private final List<FinderPattern> possibleCenters;
   private boolean hasSkipped;
   private final int[] crossCheckStateCount;
   private final ResultPointCallback resultPointCallback;

   public FinderPatternFinder(BitMatrix image) {
      this(image, null);
   }

   public FinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
      this.image = image;
      this.possibleCenters = new ArrayList<>();
      this.crossCheckStateCount = new int[5];
      this.resultPointCallback = resultPointCallback;
   }

   protected final BitMatrix getImage() {
      return this.image;
   }

   protected final List<FinderPattern> getPossibleCenters() {
      return this.possibleCenters;
   }

   final FinderPatternInfo find(Map<DecodeHintType, ?> hints) throws NotFoundException {
      boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
      int maxI = this.image.getHeight();
      int maxJ = this.image.getWidth();
      int iSkip = 3 * maxI / 388;
      if (iSkip < 3 || tryHarder) {
         iSkip = 3;
      }

      boolean done = false;
      int[] stateCount = new int[5];

      for (int i = iSkip - 1; i < maxI && !done; i += iSkip) {
         doClearCounts(stateCount);
         int currentState = 0;

         for (int j = 0; j < maxJ; j++) {
            if (this.image.get(j, i)) {
               if ((currentState & 1) == 1) {
                  currentState++;
               }

               stateCount[currentState]++;
            } else if ((currentState & 1) == 0) {
               if (currentState == 4) {
                  if (foundPatternCross(stateCount)) {
                     boolean confirmed = this.handlePossibleCenter(stateCount, i, j);
                     if (confirmed) {
                        iSkip = 2;
                        if (this.hasSkipped) {
                           done = this.haveMultiplyConfirmedCenters();
                        } else {
                           int rowSkip = this.findRowSkip();
                           if (rowSkip > stateCount[2]) {
                              i += rowSkip - stateCount[2] - iSkip;
                              j = maxJ - 1;
                           }
                        }

                        currentState = 0;
                        doClearCounts(stateCount);
                     } else {
                        doShiftCounts2(stateCount);
                        currentState = 3;
                     }
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
            boolean confirmed = this.handlePossibleCenter(stateCount, i, maxJ);
            if (confirmed) {
               iSkip = stateCount[0];
               if (this.hasSkipped) {
                  done = this.haveMultiplyConfirmedCenters();
               }
            }
         }
      }

      FinderPattern[] patternInfo = this.selectBestPatterns();
      ResultPoint.orderBestPatterns(patternInfo);
      return new FinderPatternInfo(patternInfo);
   }

   private static float centerFromEnd(int[] stateCount, int end) {
      return (float)(end - stateCount[4] - stateCount[3]) - (float)stateCount[2] / 2.0F;
   }

   protected static boolean foundPatternCross(int[] stateCount) {
      int totalModuleSize = 0;

      for (int i = 0; i < 5; i++) {
         int count = stateCount[i];
         if (count == 0) {
            return false;
         }

         totalModuleSize += count;
      }

      if (totalModuleSize < 7) {
         return false;
      } else {
         float moduleSize = (float)totalModuleSize / 7.0F;
         float maxVariance = moduleSize / 2.0F;
         return Math.abs(moduleSize - (float)stateCount[0]) < maxVariance
            && Math.abs(moduleSize - (float)stateCount[1]) < maxVariance
            && Math.abs(3.0F * moduleSize - (float)stateCount[2]) < 3.0F * maxVariance
            && Math.abs(moduleSize - (float)stateCount[3]) < maxVariance
            && Math.abs(moduleSize - (float)stateCount[4]) < maxVariance;
      }
   }

   protected static boolean foundPatternDiagonal(int[] stateCount) {
      int totalModuleSize = 0;

      for (int i = 0; i < 5; i++) {
         int count = stateCount[i];
         if (count == 0) {
            return false;
         }

         totalModuleSize += count;
      }

      if (totalModuleSize < 7) {
         return false;
      } else {
         float moduleSize = (float)totalModuleSize / 7.0F;
         float maxVariance = moduleSize / 1.333F;
         return Math.abs(moduleSize - (float)stateCount[0]) < maxVariance
            && Math.abs(moduleSize - (float)stateCount[1]) < maxVariance
            && Math.abs(3.0F * moduleSize - (float)stateCount[2]) < 3.0F * maxVariance
            && Math.abs(moduleSize - (float)stateCount[3]) < maxVariance
            && Math.abs(moduleSize - (float)stateCount[4]) < maxVariance;
      }
   }

   private int[] getCrossCheckStateCount() {
      doClearCounts(this.crossCheckStateCount);
      return this.crossCheckStateCount;
   }

   @Deprecated
   protected final void clearCounts(int[] counts) {
      doClearCounts(counts);
   }

   @Deprecated
   protected final void shiftCounts2(int[] stateCount) {
      doShiftCounts2(stateCount);
   }

   protected static void doClearCounts(int[] counts) {
      Arrays.fill(counts, 0);
   }

   protected static void doShiftCounts2(int[] stateCount) {
      stateCount[0] = stateCount[2];
      stateCount[1] = stateCount[3];
      stateCount[2] = stateCount[4];
      stateCount[3] = 1;
      stateCount[4] = 0;
   }

   private boolean crossCheckDiagonal(int centerI, int centerJ) {
      int[] stateCount = this.getCrossCheckStateCount();

      int i;
      for (i = 0; centerI >= i && centerJ >= i && this.image.get(centerJ - i, centerI - i); i++) {
         stateCount[2]++;
      }

      if (stateCount[2] == 0) {
         return false;
      } else {
         while (centerI >= i && centerJ >= i && !this.image.get(centerJ - i, centerI - i)) {
            stateCount[1]++;
            i++;
         }

         if (stateCount[1] == 0) {
            return false;
         } else {
            while (centerI >= i && centerJ >= i && this.image.get(centerJ - i, centerI - i)) {
               stateCount[0]++;
               i++;
            }

            if (stateCount[0] == 0) {
               return false;
            } else {
               int maxI = this.image.getHeight();
               int maxJ = this.image.getWidth();

               for (i = 1; centerI + i < maxI && centerJ + i < maxJ && this.image.get(centerJ + i, centerI + i); i++) {
                  stateCount[2]++;
               }

               while (centerI + i < maxI && centerJ + i < maxJ && !this.image.get(centerJ + i, centerI + i)) {
                  stateCount[3]++;
                  i++;
               }

               if (stateCount[3] == 0) {
                  return false;
               } else {
                  while (centerI + i < maxI && centerJ + i < maxJ && this.image.get(centerJ + i, centerI + i)) {
                     stateCount[4]++;
                     i++;
                  }

                  return stateCount[4] == 0 ? false : foundPatternDiagonal(stateCount);
               }
            }
         }
      }
   }

   private float crossCheckVertical(int startI, int centerJ, int maxCount, int originalStateCountTotal) {
      BitMatrix image = this.image;
      int maxI = image.getHeight();
      int[] stateCount = this.getCrossCheckStateCount();

      int i;
      for (i = startI; i >= 0 && image.get(centerJ, i); i--) {
         stateCount[2]++;
      }

      if (i < 0) {
         return Float.NaN;
      } else {
         while (i >= 0 && !image.get(centerJ, i) && stateCount[1] <= maxCount) {
            stateCount[1]++;
            i--;
         }

         if (i >= 0 && stateCount[1] <= maxCount) {
            while (i >= 0 && image.get(centerJ, i) && stateCount[0] <= maxCount) {
               stateCount[0]++;
               i--;
            }

            if (stateCount[0] > maxCount) {
               return Float.NaN;
            } else {
               for (i = startI + 1; i < maxI && image.get(centerJ, i); i++) {
                  stateCount[2]++;
               }

               if (i == maxI) {
                  return Float.NaN;
               } else {
                  while (i < maxI && !image.get(centerJ, i) && stateCount[3] < maxCount) {
                     stateCount[3]++;
                     i++;
                  }

                  if (i != maxI && stateCount[3] < maxCount) {
                     while (i < maxI && image.get(centerJ, i) && stateCount[4] < maxCount) {
                        stateCount[4]++;
                        i++;
                     }

                     if (stateCount[4] >= maxCount) {
                        return Float.NaN;
                     } else {
                        int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
                        if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= 2 * originalStateCountTotal) {
                           return Float.NaN;
                        } else {
                           return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
                        }
                     }
                  } else {
                     return Float.NaN;
                  }
               }
            }
         } else {
            return Float.NaN;
         }
      }
   }

   private float crossCheckHorizontal(int startJ, int centerI, int maxCount, int originalStateCountTotal) {
      BitMatrix image = this.image;
      int maxJ = image.getWidth();
      int[] stateCount = this.getCrossCheckStateCount();

      int j;
      for (j = startJ; j >= 0 && image.get(j, centerI); j--) {
         stateCount[2]++;
      }

      if (j < 0) {
         return Float.NaN;
      } else {
         while (j >= 0 && !image.get(j, centerI) && stateCount[1] <= maxCount) {
            stateCount[1]++;
            j--;
         }

         if (j >= 0 && stateCount[1] <= maxCount) {
            while (j >= 0 && image.get(j, centerI) && stateCount[0] <= maxCount) {
               stateCount[0]++;
               j--;
            }

            if (stateCount[0] > maxCount) {
               return Float.NaN;
            } else {
               for (j = startJ + 1; j < maxJ && image.get(j, centerI); j++) {
                  stateCount[2]++;
               }

               if (j == maxJ) {
                  return Float.NaN;
               } else {
                  while (j < maxJ && !image.get(j, centerI) && stateCount[3] < maxCount) {
                     stateCount[3]++;
                     j++;
                  }

                  if (j != maxJ && stateCount[3] < maxCount) {
                     while (j < maxJ && image.get(j, centerI) && stateCount[4] < maxCount) {
                        stateCount[4]++;
                        j++;
                     }

                     if (stateCount[4] >= maxCount) {
                        return Float.NaN;
                     } else {
                        int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
                        if (5 * Math.abs(stateCountTotal - originalStateCountTotal) >= originalStateCountTotal) {
                           return Float.NaN;
                        } else {
                           return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Float.NaN;
                        }
                     }
                  } else {
                     return Float.NaN;
                  }
               }
            }
         } else {
            return Float.NaN;
         }
      }
   }

   @Deprecated
   protected final boolean handlePossibleCenter(int[] stateCount, int i, int j, boolean pureBarcode) {
      return this.handlePossibleCenter(stateCount, i, j);
   }

   protected final boolean handlePossibleCenter(int[] stateCount, int i, int j) {
      int stateCountTotal = stateCount[0] + stateCount[1] + stateCount[2] + stateCount[3] + stateCount[4];
      float centerJ = centerFromEnd(stateCount, j);
      float centerI = this.crossCheckVertical(i, (int)centerJ, stateCount[2], stateCountTotal);
      if (!Float.isNaN(centerI)) {
         centerJ = this.crossCheckHorizontal((int)centerJ, (int)centerI, stateCount[2], stateCountTotal);
         if (!Float.isNaN(centerJ) && this.crossCheckDiagonal((int)centerI, (int)centerJ)) {
            float estimatedModuleSize = (float)stateCountTotal / 7.0F;
            boolean found = false;

            for (int index = 0; index < this.possibleCenters.size(); index++) {
               FinderPattern center = this.possibleCenters.get(index);
               if (center.aboutEquals(estimatedModuleSize, centerI, centerJ)) {
                  this.possibleCenters.set(index, center.combineEstimate(centerI, centerJ, estimatedModuleSize));
                  found = true;
                  break;
               }
            }

            if (!found) {
               FinderPattern point = new FinderPattern(centerJ, centerI, estimatedModuleSize);
               this.possibleCenters.add(point);
               if (this.resultPointCallback != null) {
                  this.resultPointCallback.foundPossibleResultPoint(point);
               }
            }

            return true;
         }
      }

      return false;
   }

   private int findRowSkip() {
      int max = this.possibleCenters.size();
      if (max <= 1) {
         return 0;
      } else {
         ResultPoint firstConfirmedCenter = null;

         for (FinderPattern center : this.possibleCenters) {
            if (center.getCount() >= 2) {
               if (firstConfirmedCenter != null) {
                  this.hasSkipped = true;
                  return (int)(Math.abs(firstConfirmedCenter.getX() - center.getX()) - Math.abs(firstConfirmedCenter.getY() - center.getY())) / 2;
               }

               firstConfirmedCenter = center;
            }
         }

         return 0;
      }
   }

   private boolean haveMultiplyConfirmedCenters() {
      int confirmedCount = 0;
      float totalModuleSize = 0.0F;
      int max = this.possibleCenters.size();

      for (FinderPattern pattern : this.possibleCenters) {
         if (pattern.getCount() >= 2) {
            confirmedCount++;
            totalModuleSize += pattern.getEstimatedModuleSize();
         }
      }

      if (confirmedCount < 3) {
         return false;
      } else {
         float average = totalModuleSize / (float)max;
         float totalDeviation = 0.0F;

         for (FinderPattern patternx : this.possibleCenters) {
            totalDeviation += Math.abs(patternx.getEstimatedModuleSize() - average);
         }

         return totalDeviation <= 0.05F * totalModuleSize;
      }
   }

   private static double squaredDistance(FinderPattern a, FinderPattern b) {
      double x = (double)(a.getX() - b.getX());
      double y = (double)(a.getY() - b.getY());
      return x * x + y * y;
   }

   private FinderPattern[] selectBestPatterns() throws NotFoundException {
      int startSize = this.possibleCenters.size();
      if (startSize < 3) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         Iterator<FinderPattern> it = this.possibleCenters.iterator();

         while (it.hasNext()) {
            if (it.next().getCount() < 2) {
               it.remove();
            }
         }

         Collections.sort(this.possibleCenters, moduleComparator);
         double distortion = Double.MAX_VALUE;
         FinderPattern[] bestPatterns = new FinderPattern[3];

         for (int i = 0; i < this.possibleCenters.size() - 2; i++) {
            FinderPattern fpi = this.possibleCenters.get(i);
            float minModuleSize = fpi.getEstimatedModuleSize();

            for (int j = i + 1; j < this.possibleCenters.size() - 1; j++) {
               FinderPattern fpj = this.possibleCenters.get(j);
               double squares0 = squaredDistance(fpi, fpj);

               for (int k = j + 1; k < this.possibleCenters.size(); k++) {
                  FinderPattern fpk = this.possibleCenters.get(k);
                  float maxModuleSize = fpk.getEstimatedModuleSize();
                  if (!(maxModuleSize > minModuleSize * 1.4F)) {
                     double a = squares0;
                     double b = squaredDistance(fpj, fpk);
                     double c = squaredDistance(fpi, fpk);
                     if (squares0 < b) {
                        if (b > c) {
                           if (squares0 < c) {
                              double temp = b;
                              b = c;
                              c = temp;
                           } else {
                              a = c;
                              c = b;
                              b = squares0;
                           }
                        }
                     } else if (b < c) {
                        if (squares0 < c) {
                           a = b;
                           b = squares0;
                        } else {
                           a = b;
                           b = c;
                           c = squares0;
                        }
                     } else {
                        a = c;
                        c = squares0;
                     }

                     double d = Math.abs(c - 2.0 * b) + Math.abs(c - 2.0 * a);
                     if (d < distortion) {
                        distortion = d;
                        bestPatterns[0] = fpi;
                        bestPatterns[1] = fpj;
                        bestPatterns[2] = fpk;
                     }
                  }
               }
            }
         }

         if (distortion == Double.MAX_VALUE) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            return bestPatterns;
         }
      }
   }

   private static final class EstimatedModuleComparator implements Comparator<FinderPattern>, Serializable {
      private EstimatedModuleComparator() {
      }

      public int compare(FinderPattern center1, FinderPattern center2) {
         return Float.compare(center1.getEstimatedModuleSize(), center2.getEstimatedModuleSize());
      }
   }
}
