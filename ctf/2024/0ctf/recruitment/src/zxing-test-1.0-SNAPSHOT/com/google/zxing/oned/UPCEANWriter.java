package com.google.zxing.oned;

public abstract class UPCEANWriter extends OneDimensionalCodeWriter {
   @Override
   public int getDefaultMargin() {
      return 9;
   }
}
