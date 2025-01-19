package com.beust.jcommander.internal;

import com.beust.jcommander.ParameterException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class DefaultConsole implements Console {
   private final PrintStream target;

   public DefaultConsole(PrintStream target) {
      this.target = target;
   }

   public DefaultConsole() {
      this.target = System.out;
   }

   @Override
   public void print(String msg) {
      this.target.print(msg);
   }

   @Override
   public void println(String msg) {
      this.target.println(msg);
   }

   @Override
   public char[] readPassword(boolean echoInput) {
      try {
         InputStreamReader isr = new InputStreamReader(System.in);
         BufferedReader in = new BufferedReader(isr);
         String result = in.readLine();
         return result.toCharArray();
      } catch (IOException var5) {
         throw new ParameterException(var5);
      }
   }
}
