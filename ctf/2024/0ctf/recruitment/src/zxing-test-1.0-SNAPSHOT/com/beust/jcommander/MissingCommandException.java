package com.beust.jcommander;

public class MissingCommandException extends ParameterException {
   private final String unknownCommand;

   public MissingCommandException(String message) {
      this(message, null);
   }

   public MissingCommandException(String message, String command) {
      super(message);
      this.unknownCommand = command;
   }

   public String getUnknownCommand() {
      return this.unknownCommand;
   }
}
