package com.google.zxing.common;

public interface ECIInput {
   int length();

   char charAt(int var1);

   CharSequence subSequence(int var1, int var2);

   boolean isECI(int var1);

   int getECIValue(int var1);

   boolean haveNCharacters(int var1, int var2);
}
