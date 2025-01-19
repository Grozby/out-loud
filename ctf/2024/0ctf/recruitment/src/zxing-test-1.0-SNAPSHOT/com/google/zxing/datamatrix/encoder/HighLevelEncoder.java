package com.google.zxing.datamatrix.encoder;

import com.google.zxing.Dimension;
import java.util.Arrays;

public final class HighLevelEncoder {
   private static final char PAD = '\u0081';
   static final char LATCH_TO_C40 = 'æ';
   static final char LATCH_TO_BASE256 = 'ç';
   static final char UPPER_SHIFT = 'ë';
   private static final char MACRO_05 = 'ì';
   private static final char MACRO_06 = 'í';
   static final char LATCH_TO_ANSIX12 = 'î';
   static final char LATCH_TO_TEXT = 'ï';
   static final char LATCH_TO_EDIFACT = 'ð';
   static final char C40_UNLATCH = 'þ';
   static final char X12_UNLATCH = 'þ';
   static final String MACRO_05_HEADER = "[)>\u001e05\u001d";
   static final String MACRO_06_HEADER = "[)>\u001e06\u001d";
   static final String MACRO_TRAILER = "\u001e\u0004";
   static final int ASCII_ENCODATION = 0;
   static final int C40_ENCODATION = 1;
   static final int TEXT_ENCODATION = 2;
   static final int X12_ENCODATION = 3;
   static final int EDIFACT_ENCODATION = 4;
   static final int BASE256_ENCODATION = 5;

   private HighLevelEncoder() {
   }

   private static char randomize253State(int codewordPosition) {
      int pseudoRandom = 149 * codewordPosition % 253 + 1;
      int tempVariable = 129 + pseudoRandom;
      return (char)(tempVariable <= 254 ? tempVariable : tempVariable - 254);
   }

   public static String encodeHighLevel(String msg) {
      return encodeHighLevel(msg, SymbolShapeHint.FORCE_NONE, null, null, false);
   }

   public static String encodeHighLevel(String msg, SymbolShapeHint shape, Dimension minSize, Dimension maxSize) {
      return encodeHighLevel(msg, shape, minSize, maxSize, false);
   }

   public static String encodeHighLevel(String msg, SymbolShapeHint shape, Dimension minSize, Dimension maxSize, boolean forceC40) {
      C40Encoder c40Encoder = new C40Encoder();
      Encoder[] encoders = new Encoder[]{new ASCIIEncoder(), c40Encoder, new TextEncoder(), new X12Encoder(), new EdifactEncoder(), new Base256Encoder()};
      EncoderContext context = new EncoderContext(msg);
      context.setSymbolShape(shape);
      context.setSizeConstraints(minSize, maxSize);
      if (msg.startsWith("[)>\u001e05\u001d") && msg.endsWith("\u001e\u0004")) {
         context.writeCodeword('ì');
         context.setSkipAtEnd(2);
         context.pos = context.pos + "[)>\u001e05\u001d".length();
      } else if (msg.startsWith("[)>\u001e06\u001d") && msg.endsWith("\u001e\u0004")) {
         context.writeCodeword('í');
         context.setSkipAtEnd(2);
         context.pos = context.pos + "[)>\u001e06\u001d".length();
      }

      int encodingMode = 0;
      if (forceC40) {
         c40Encoder.encodeMaximal(context);
         encodingMode = context.getNewEncoding();
         context.resetEncoderSignal();
      }

      while (context.hasMoreCharacters()) {
         encoders[encodingMode].encode(context);
         if (context.getNewEncoding() >= 0) {
            encodingMode = context.getNewEncoding();
            context.resetEncoderSignal();
         }
      }

      int len = context.getCodewordCount();
      context.updateSymbolInfo();
      int capacity = context.getSymbolInfo().getDataCapacity();
      if (len < capacity && encodingMode != 0 && encodingMode != 5 && encodingMode != 4) {
         context.writeCodeword('þ');
      }

      StringBuilder codewords = context.getCodewords();
      if (codewords.length() < capacity) {
         codewords.append('\u0081');
      }

      while (codewords.length() < capacity) {
         codewords.append(randomize253State(codewords.length() + 1));
      }

      return context.getCodewords().toString();
   }

   static int lookAheadTest(CharSequence msg, int startpos, int currentMode) {
      int newMode = lookAheadTestIntern(msg, startpos, currentMode);
      if (currentMode == 3 && newMode == 3) {
         int endpos = Math.min(startpos + 3, msg.length());

         for (int i = startpos; i < endpos; i++) {
            if (!isNativeX12(msg.charAt(i))) {
               return 0;
            }
         }
      } else if (currentMode == 4 && newMode == 4) {
         int endpos = Math.min(startpos + 4, msg.length());

         for (int ix = startpos; ix < endpos; ix++) {
            if (!isNativeEDIFACT(msg.charAt(ix))) {
               return 0;
            }
         }
      }

      return newMode;
   }

   static int lookAheadTestIntern(CharSequence msg, int startpos, int currentMode) {
      if (startpos >= msg.length()) {
         return currentMode;
      } else {
         float[] charCounts;
         if (currentMode == 0) {
            charCounts = new float[]{0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.25F};
         } else {
            charCounts = new float[]{1.0F, 2.0F, 2.0F, 2.0F, 2.0F, 2.25F};
            charCounts[currentMode] = 0.0F;
         }

         int charsProcessed = 0;
         byte[] mins = new byte[6];
         int[] intCharCounts = new int[6];

         while (startpos + charsProcessed != msg.length()) {
            char c = msg.charAt(startpos + charsProcessed);
            charsProcessed++;
            if (isDigit(c)) {
               charCounts[0] += 0.5F;
            } else if (isExtendedASCII(c)) {
               charCounts[0] = (float)Math.ceil((double)charCounts[0]);
               charCounts[0] += 2.0F;
            } else {
               charCounts[0] = (float)Math.ceil((double)charCounts[0]);
               charCounts[0]++;
            }

            if (isNativeC40(c)) {
               charCounts[1] += 0.6666667F;
            } else if (isExtendedASCII(c)) {
               charCounts[1] += 2.6666667F;
            } else {
               charCounts[1]++;
            }

            if (isNativeText(c)) {
               charCounts[2] += 0.6666667F;
            } else if (isExtendedASCII(c)) {
               charCounts[2] += 2.6666667F;
            } else {
               charCounts[2]++;
            }

            if (isNativeX12(c)) {
               charCounts[3] += 0.6666667F;
            } else if (isExtendedASCII(c)) {
               charCounts[3] += 4.3333335F;
            } else {
               charCounts[3] += 3.3333333F;
            }

            if (isNativeEDIFACT(c)) {
               charCounts[4] += 0.75F;
            } else if (isExtendedASCII(c)) {
               charCounts[4] += 4.25F;
            } else {
               charCounts[4] += 3.25F;
            }

            if (isSpecialB256(c)) {
               charCounts[5] += 4.0F;
            } else {
               charCounts[5]++;
            }

            if (charsProcessed >= 4) {
               Arrays.fill(mins, (byte)0);
               Arrays.fill(intCharCounts, 0);
               findMinimums(charCounts, intCharCounts, Integer.MAX_VALUE, mins);
               if (intCharCounts[0] < min(intCharCounts[5], intCharCounts[1], intCharCounts[2], intCharCounts[3], intCharCounts[4])) {
                  return 0;
               }

               if (intCharCounts[5] < intCharCounts[0] || intCharCounts[5] + 1 < min(intCharCounts[1], intCharCounts[2], intCharCounts[3], intCharCounts[4])) {
                  return 5;
               }

               if (intCharCounts[4] + 1 < min(intCharCounts[5], intCharCounts[1], intCharCounts[2], intCharCounts[3], intCharCounts[0])) {
                  return 4;
               }

               if (intCharCounts[2] + 1 < min(intCharCounts[5], intCharCounts[1], intCharCounts[4], intCharCounts[3], intCharCounts[0])) {
                  return 2;
               }

               if (intCharCounts[3] + 1 < min(intCharCounts[5], intCharCounts[1], intCharCounts[4], intCharCounts[2], intCharCounts[0])) {
                  return 3;
               }

               if (intCharCounts[1] + 1 < min(intCharCounts[0], intCharCounts[5], intCharCounts[4], intCharCounts[2])) {
                  if (intCharCounts[1] < intCharCounts[3]) {
                     return 1;
                  }

                  if (intCharCounts[1] == intCharCounts[3]) {
                     for (int p = startpos + charsProcessed + 1; p < msg.length(); p++) {
                        char tc = msg.charAt(p);
                        if (isX12TermSep(tc)) {
                           return 3;
                        }

                        if (!isNativeX12(tc)) {
                           break;
                        }
                     }

                     return 1;
                  }
               }
            }
         }

         Arrays.fill(mins, (byte)0);
         Arrays.fill(intCharCounts, 0);
         int min = findMinimums(charCounts, intCharCounts, Integer.MAX_VALUE, mins);
         int minCount = getMinimumCount(mins);
         if (intCharCounts[0] == min) {
            return 0;
         } else {
            if (minCount == 1) {
               if (mins[5] > 0) {
                  return 5;
               }

               if (mins[4] > 0) {
                  return 4;
               }

               if (mins[2] > 0) {
                  return 2;
               }

               if (mins[3] > 0) {
                  return 3;
               }
            }

            return 1;
         }
      }
   }

   private static int min(int f1, int f2, int f3, int f4, int f5) {
      return Math.min(min(f1, f2, f3, f4), f5);
   }

   private static int min(int f1, int f2, int f3, int f4) {
      return Math.min(f1, Math.min(f2, Math.min(f3, f4)));
   }

   private static int findMinimums(float[] charCounts, int[] intCharCounts, int min, byte[] mins) {
      for (int i = 0; i < 6; i++) {
         int current = intCharCounts[i] = (int)Math.ceil((double)charCounts[i]);
         if (min > current) {
            min = current;
            Arrays.fill(mins, (byte)0);
         }

         if (min == current) {
            mins[i]++;
         }
      }

      return min;
   }

   private static int getMinimumCount(byte[] mins) {
      int minCount = 0;

      for (int i = 0; i < 6; i++) {
         minCount += mins[i];
      }

      return minCount;
   }

   static boolean isDigit(char ch) {
      return ch >= '0' && ch <= '9';
   }

   static boolean isExtendedASCII(char ch) {
      return ch >= 128 && ch <= 255;
   }

   static boolean isNativeC40(char ch) {
      return ch == ' ' || ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z';
   }

   static boolean isNativeText(char ch) {
      return ch == ' ' || ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z';
   }

   static boolean isNativeX12(char ch) {
      return isX12TermSep(ch) || ch == ' ' || ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z';
   }

   private static boolean isX12TermSep(char ch) {
      return ch == '\r' || ch == '*' || ch == '>';
   }

   static boolean isNativeEDIFACT(char ch) {
      return ch >= ' ' && ch <= '^';
   }

   private static boolean isSpecialB256(char ch) {
      return false;
   }

   public static int determineConsecutiveDigitCount(CharSequence msg, int startpos) {
      int len = msg.length();
      int idx = startpos;

      while (idx < len && isDigit(msg.charAt(idx))) {
         idx++;
      }

      return idx - startpos;
   }

   static void illegalCharacter(char c) {
      String hex = Integer.toHexString(c);
      hex = "0000".substring(0, 4 - hex.length()) + hex;
      throw new IllegalArgumentException("Illegal character: " + c + " (0x" + hex + ')');
   }
}
