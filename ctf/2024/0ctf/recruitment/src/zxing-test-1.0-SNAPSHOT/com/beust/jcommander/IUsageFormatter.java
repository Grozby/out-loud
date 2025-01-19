package com.beust.jcommander;

public interface IUsageFormatter {
   void usage(String var1);

   void usage(String var1, StringBuilder var2);

   void usage(StringBuilder var1);

   void usage(String var1, StringBuilder var2, String var3);

   void usage(StringBuilder var1, String var2);

   String getCommandDescription(String var1);
}
