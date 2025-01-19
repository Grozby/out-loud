package com.google.zxing.common.detector;

public final class MathUtils {
   private MathUtils() {
   }

   public static int round(float d) {
      return (int)(d + (d < 0.0F ? -0.5F : 0.5F));
   }

   public static float distance(float aX, float aY, float bX, float bY) {
      double xDiff = (double)(aX - bX);
      double yDiff = (double)(aY - bY);
      return (float)Math.sqrt(xDiff * xDiff + yDiff * yDiff);
   }

   public static float distance(int aX, int aY, int bX, int bY) {
      double xDiff = (double)(aX - bX);
      double yDiff = (double)(aY - bY);
      return (float)Math.sqrt(xDiff * xDiff + yDiff * yDiff);
   }

   public static int sum(int[] array) {
      int count = 0;

      for (int a : array) {
         count += a;
      }

      return count;
   }
}
