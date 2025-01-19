package com.beust.jcommander;

public class ParameterException extends RuntimeException {
   private JCommander jc;

   public ParameterException(Throwable t) {
      super(t);
   }

   public ParameterException(String string) {
      super(string);
   }

   public ParameterException(String string, Throwable t) {
      super(string, t);
   }

   public void setJCommander(JCommander jc) {
      this.jc = jc;
   }

   public JCommander getJCommander() {
      return this.jc;
   }

   public void usage() {
      if (this.jc != null) {
         this.jc.usage();
      }
   }
}
