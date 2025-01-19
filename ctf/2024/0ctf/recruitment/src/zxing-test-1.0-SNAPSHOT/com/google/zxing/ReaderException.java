package com.google.zxing;

public abstract class ReaderException extends Exception {
   protected static boolean isStackTrace = System.getProperty("surefire.test.class.path") != null;
   protected static final StackTraceElement[] NO_TRACE = new StackTraceElement[0];

   ReaderException() {
   }

   ReaderException(Throwable cause) {
      super(cause);
   }

   @Override
   public final synchronized Throwable fillInStackTrace() {
      return null;
   }

   public static void setStackTrace(boolean enabled) {
      isStackTrace = enabled;
   }
}
