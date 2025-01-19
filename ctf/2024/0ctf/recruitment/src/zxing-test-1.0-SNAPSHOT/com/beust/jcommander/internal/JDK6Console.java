package com.beust.jcommander.internal;

import com.beust.jcommander.ParameterException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class JDK6Console implements Console {
   private Object console;
   private PrintWriter writer;

   public JDK6Console(Object console) throws Exception {
      this.console = console;
      Method writerMethod = console.getClass().getDeclaredMethod("writer");
      this.writer = (PrintWriter)writerMethod.invoke(console);
   }

   @Override
   public void print(String msg) {
      this.writer.print(msg);
   }

   @Override
   public void println(String msg) {
      this.writer.println(msg);
   }

   @Override
   public char[] readPassword(boolean echoInput) {
      try {
         this.writer.flush();
         if (echoInput) {
            Method method = this.console.getClass().getDeclaredMethod("readLine");
            return ((String)method.invoke(this.console)).toCharArray();
         } else {
            Method method = this.console.getClass().getDeclaredMethod("readPassword");
            return (char[])method.invoke(this.console);
         }
      } catch (Exception var3) {
         throw new ParameterException(var3);
      }
   }
}
