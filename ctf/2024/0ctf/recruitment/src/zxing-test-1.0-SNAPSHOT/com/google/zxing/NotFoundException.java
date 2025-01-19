package com.google.zxing;

public final class NotFoundException extends ReaderException {
   private static final NotFoundException INSTANCE = new NotFoundException();

   private NotFoundException() {
   }

   public static NotFoundException getNotFoundInstance() {
      return isStackTrace ? new NotFoundException() : INSTANCE;
   }

   static {
      INSTANCE.setStackTrace(NO_TRACE);
   }
}
