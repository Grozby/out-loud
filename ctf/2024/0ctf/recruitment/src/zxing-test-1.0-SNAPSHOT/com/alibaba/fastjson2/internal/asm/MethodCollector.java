package com.alibaba.fastjson2.internal.asm;

public class MethodCollector {
   private final int paramCount;
   private final int ignoreCount;
   private int currentParameter;
   private final StringBuilder result;
   protected boolean debugInfoPresent;

   protected MethodCollector(int ignoreCount, int paramCount) {
      this.ignoreCount = ignoreCount;
      this.paramCount = paramCount;
      this.result = new StringBuilder();
      this.currentParameter = 0;
      this.debugInfoPresent = paramCount == 0;
   }

   protected void visitLocalVariable(String name, int index) {
      if (index >= this.ignoreCount && index < this.ignoreCount + this.paramCount) {
         if (!("arg" + this.currentParameter).equals(name)) {
            this.debugInfoPresent = true;
         }

         this.result.append(',');
         this.result.append(name);
         this.currentParameter++;
      }
   }

   protected String getResult() {
      return this.result.length() != 0 ? this.result.substring(1) : "";
   }
}
