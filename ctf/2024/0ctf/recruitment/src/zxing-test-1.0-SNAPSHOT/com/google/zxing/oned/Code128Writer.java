package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class Code128Writer extends OneDimensionalCodeWriter {
   private static final int CODE_START_A = 103;
   private static final int CODE_START_B = 104;
   private static final int CODE_START_C = 105;
   private static final int CODE_CODE_A = 101;
   private static final int CODE_CODE_B = 100;
   private static final int CODE_CODE_C = 99;
   private static final int CODE_STOP = 106;
   private static final char ESCAPE_FNC_1 = 'ñ';
   private static final char ESCAPE_FNC_2 = 'ò';
   private static final char ESCAPE_FNC_3 = 'ó';
   private static final char ESCAPE_FNC_4 = 'ô';
   private static final int CODE_FNC_1 = 102;
   private static final int CODE_FNC_2 = 97;
   private static final int CODE_FNC_3 = 96;
   private static final int CODE_FNC_4_A = 101;
   private static final int CODE_FNC_4_B = 100;

   @Override
   protected Collection<BarcodeFormat> getSupportedWriteFormats() {
      return Collections.singleton(BarcodeFormat.CODE_128);
   }

   @Override
   public boolean[] encode(String contents) {
      return this.encode(contents, null);
   }

   @Override
   public boolean[] encode(String contents, Map<EncodeHintType, ?> hints) {
      int forcedCodeSet = check(contents, hints);
      boolean hasCompactionHint = hints != null
         && hints.containsKey(EncodeHintType.CODE128_COMPACT)
         && Boolean.parseBoolean(hints.get(EncodeHintType.CODE128_COMPACT).toString());
      return hasCompactionHint ? new Code128Writer.MinimalEncoder().encode(contents) : encodeFast(contents, forcedCodeSet);
   }

   private static int check(String contents, Map<EncodeHintType, ?> hints) {
      int forcedCodeSet = -1;
      if (hints != null && hints.containsKey(EncodeHintType.FORCE_CODE_SET)) {
         String codeSetHint = hints.get(EncodeHintType.FORCE_CODE_SET).toString();
         switch (codeSetHint) {
            case "A":
               forcedCodeSet = 101;
               break;
            case "B":
               forcedCodeSet = 100;
               break;
            case "C":
               forcedCodeSet = 99;
               break;
            default:
               throw new IllegalArgumentException("Unsupported code set hint: " + codeSetHint);
         }
      }

      int length = contents.length();

      for (int i = 0; i < length; i++) {
         char c = contents.charAt(i);
         switch (c) {
            case 'ñ':
            case 'ò':
            case 'ó':
            case 'ô':
               break;
            default:
               if (c > 127) {
                  throw new IllegalArgumentException("Bad character in input: ASCII value=" + c);
               }
         }

         switch (forcedCodeSet) {
            case 99:
               if (c < '0' || c > '9' && c <= 127 || c == 242 || c == 243 || c == 244) {
                  throw new IllegalArgumentException("Bad character in input for forced code set C: ASCII value=" + c);
               }
               break;
            case 100:
               if (c < ' ') {
                  throw new IllegalArgumentException("Bad character in input for forced code set B: ASCII value=" + c);
               }
               break;
            case 101:
               if (c > '_' && c <= 127) {
                  throw new IllegalArgumentException("Bad character in input for forced code set A: ASCII value=" + c);
               }
         }
      }

      return forcedCodeSet;
   }

   private static boolean[] encodeFast(String contents, int forcedCodeSet) {
      int length = contents.length();
      Collection<int[]> patterns = new ArrayList<>();
      int checkSum = 0;
      int checkWeight = 1;
      int codeSet = 0;
      int position = 0;

      while (position < length) {
         int newCodeSet;
         if (forcedCodeSet == -1) {
            newCodeSet = chooseCode(contents, position, codeSet);
         } else {
            newCodeSet = forcedCodeSet;
         }

         int patternIndex;
         if (newCodeSet == codeSet) {
            switch (contents.charAt(position)) {
               case 'ñ':
                  patternIndex = 102;
                  break;
               case 'ò':
                  patternIndex = 97;
                  break;
               case 'ó':
                  patternIndex = 96;
                  break;
               case 'ô':
                  if (codeSet == 101) {
                     patternIndex = 101;
                  } else {
                     patternIndex = 100;
                  }
                  break;
               default:
                  switch (codeSet) {
                     case 100:
                        patternIndex = contents.charAt(position) - ' ';
                        break;
                     case 101:
                        patternIndex = contents.charAt(position) - ' ';
                        if (patternIndex < 0) {
                           patternIndex += 96;
                        }
                        break;
                     default:
                        if (position + 1 == length) {
                           throw new IllegalArgumentException("Bad number of characters for digit only encoding.");
                        }

                        patternIndex = Integer.parseInt(contents.substring(position, position + 2));
                        position++;
                  }
            }

            position++;
         } else {
            if (codeSet == 0) {
               switch (newCodeSet) {
                  case 100:
                     patternIndex = 104;
                     break;
                  case 101:
                     patternIndex = 103;
                     break;
                  default:
                     patternIndex = 105;
               }
            } else {
               patternIndex = newCodeSet;
            }

            codeSet = newCodeSet;
         }

         patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);
         checkSum += patternIndex * checkWeight;
         if (position != 0) {
            checkWeight++;
         }
      }

      return produceResult(patterns, checkSum);
   }

   static boolean[] produceResult(Collection<int[]> patterns, int checkSum) {
      checkSum %= 103;
      if (checkSum < 0) {
         throw new IllegalArgumentException("Unable to compute a valid input checksum");
      } else {
         patterns.add(Code128Reader.CODE_PATTERNS[checkSum]);
         patterns.add(Code128Reader.CODE_PATTERNS[106]);
         int codeWidth = 0;

         for (int[] pattern : patterns) {
            for (int width : pattern) {
               codeWidth += width;
            }
         }

         boolean[] result = new boolean[codeWidth];
         int pos = 0;

         for (int[] pattern : patterns) {
            pos += appendPattern(result, pos, pattern, true);
         }

         return result;
      }
   }

   private static Code128Writer.CType findCType(CharSequence value, int start) {
      int last = value.length();
      if (start >= last) {
         return Code128Writer.CType.UNCODABLE;
      } else {
         char c = value.charAt(start);
         if (c == 241) {
            return Code128Writer.CType.FNC_1;
         } else if (c >= '0' && c <= '9') {
            if (start + 1 >= last) {
               return Code128Writer.CType.ONE_DIGIT;
            } else {
               c = value.charAt(start + 1);
               return c >= '0' && c <= '9' ? Code128Writer.CType.TWO_DIGITS : Code128Writer.CType.ONE_DIGIT;
            }
         } else {
            return Code128Writer.CType.UNCODABLE;
         }
      }
   }

   private static int chooseCode(CharSequence value, int start, int oldCode) {
      Code128Writer.CType lookahead = findCType(value, start);
      if (lookahead == Code128Writer.CType.ONE_DIGIT) {
         return oldCode == 101 ? 101 : 100;
      } else if (lookahead == Code128Writer.CType.UNCODABLE) {
         if (start < value.length()) {
            char c = value.charAt(start);
            if (c < ' ' || oldCode == 101 && (c < '`' || c >= 241 && c <= 244)) {
               return 101;
            }
         }

         return 100;
      } else if (oldCode == 101 && lookahead == Code128Writer.CType.FNC_1) {
         return 101;
      } else if (oldCode == 99) {
         return 99;
      } else if (oldCode == 100) {
         if (lookahead == Code128Writer.CType.FNC_1) {
            return 100;
         } else {
            lookahead = findCType(value, start + 2);
            if (lookahead == Code128Writer.CType.UNCODABLE || lookahead == Code128Writer.CType.ONE_DIGIT) {
               return 100;
            } else if (lookahead == Code128Writer.CType.FNC_1) {
               lookahead = findCType(value, start + 3);
               return lookahead == Code128Writer.CType.TWO_DIGITS ? 99 : 100;
            } else {
               int index = start + 4;

               while ((lookahead = findCType(value, index)) == Code128Writer.CType.TWO_DIGITS) {
                  index += 2;
               }

               return lookahead == Code128Writer.CType.ONE_DIGIT ? 100 : 99;
            }
         }
      } else {
         if (lookahead == Code128Writer.CType.FNC_1) {
            lookahead = findCType(value, start + 1);
         }

         return lookahead == Code128Writer.CType.TWO_DIGITS ? 99 : 100;
      }
   }

   private static enum CType {
      UNCODABLE,
      ONE_DIGIT,
      TWO_DIGITS,
      FNC_1;
   }

   private static final class MinimalEncoder {
      static final String A = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001fÿ";
      static final String B = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007fÿ";
      private static final int CODE_SHIFT = 98;
      private int[][] memoizedCost;
      private Code128Writer.MinimalEncoder.Latch[][] minPath;

      private MinimalEncoder() {
      }

      private boolean[] encode(String contents) {
         this.memoizedCost = new int[4][contents.length()];
         this.minPath = new Code128Writer.MinimalEncoder.Latch[4][contents.length()];
         this.encode(contents, Code128Writer.MinimalEncoder.Charset.NONE, 0);
         Collection<int[]> patterns = new ArrayList<>();
         int[] checkSum = new int[]{0};
         int[] checkWeight = new int[]{1};
         int length = contents.length();
         Code128Writer.MinimalEncoder.Charset charset = Code128Writer.MinimalEncoder.Charset.NONE;

         for (int i = 0; i < length; i++) {
            Code128Writer.MinimalEncoder.Latch latch = this.minPath[charset.ordinal()][i];
            switch (latch) {
               case A:
                  charset = Code128Writer.MinimalEncoder.Charset.A;
                  addPattern(patterns, i == 0 ? 103 : 101, checkSum, checkWeight, i);
                  break;
               case B:
                  charset = Code128Writer.MinimalEncoder.Charset.B;
                  addPattern(patterns, i == 0 ? 104 : 100, checkSum, checkWeight, i);
                  break;
               case C:
                  charset = Code128Writer.MinimalEncoder.Charset.C;
                  addPattern(patterns, i == 0 ? 105 : 99, checkSum, checkWeight, i);
                  break;
               case SHIFT:
                  addPattern(patterns, 98, checkSum, checkWeight, i);
            }

            if (charset == Code128Writer.MinimalEncoder.Charset.C) {
               if (contents.charAt(i) == 241) {
                  addPattern(patterns, 102, checkSum, checkWeight, i);
               } else {
                  addPattern(patterns, Integer.parseInt(contents.substring(i, i + 2)), checkSum, checkWeight, i);

                  assert i + 1 < length;

                  if (i + 1 < length) {
                     i++;
                  }
               }
            } else {
               int patternIndex;
               switch (contents.charAt(i)) {
                  case 'ñ':
                     patternIndex = 102;
                     break;
                  case 'ò':
                     patternIndex = 97;
                     break;
                  case 'ó':
                     patternIndex = 96;
                     break;
                  case 'ô':
                     if ((charset != Code128Writer.MinimalEncoder.Charset.A || latch == Code128Writer.MinimalEncoder.Latch.SHIFT)
                        && (charset != Code128Writer.MinimalEncoder.Charset.B || latch != Code128Writer.MinimalEncoder.Latch.SHIFT)) {
                        patternIndex = 100;
                     } else {
                        patternIndex = 101;
                     }
                     break;
                  default:
                     patternIndex = contents.charAt(i) - ' ';
               }

               if ((
                     charset == Code128Writer.MinimalEncoder.Charset.A && latch != Code128Writer.MinimalEncoder.Latch.SHIFT
                        || charset == Code128Writer.MinimalEncoder.Charset.B && latch == Code128Writer.MinimalEncoder.Latch.SHIFT
                  )
                  && patternIndex < 0) {
                  patternIndex += 96;
               }

               addPattern(patterns, patternIndex, checkSum, checkWeight, i);
            }
         }

         this.memoizedCost = (int[][])null;
         this.minPath = (Code128Writer.MinimalEncoder.Latch[][])null;
         return Code128Writer.produceResult(patterns, checkSum[0]);
      }

      private static void addPattern(Collection<int[]> patterns, int patternIndex, int[] checkSum, int[] checkWeight, int position) {
         patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);
         if (position != 0) {
            checkWeight[0]++;
         }

         checkSum[0] += patternIndex * checkWeight[0];
      }

      private static boolean isDigit(char c) {
         return c >= '0' && c <= '9';
      }

      private boolean canEncode(CharSequence contents, Code128Writer.MinimalEncoder.Charset charset, int position) {
         char c = contents.charAt(position);
         switch (charset) {
            case A:
               return c == 241
                  || c == 242
                  || c == 243
                  || c == 244
                  || " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000b\f\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001fÿ"
                        .indexOf(c)
                     >= 0;
            case B:
               return c == 241
                  || c == 242
                  || c == 243
                  || c == 244
                  || " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007fÿ".indexOf(c) >= 0;
            case C:
               return c == 241 || position + 1 < contents.length() && isDigit(c) && isDigit(contents.charAt(position + 1));
            default:
               return false;
         }
      }

      private int encode(CharSequence contents, Code128Writer.MinimalEncoder.Charset charset, int position) {
         assert position < contents.length();

         int mCost = this.memoizedCost[charset.ordinal()][position];
         if (mCost > 0) {
            return mCost;
         } else {
            int minCost = Integer.MAX_VALUE;
            Code128Writer.MinimalEncoder.Latch minLatch = Code128Writer.MinimalEncoder.Latch.NONE;
            boolean atEnd = position + 1 >= contents.length();
            Code128Writer.MinimalEncoder.Charset[] sets = new Code128Writer.MinimalEncoder.Charset[]{
               Code128Writer.MinimalEncoder.Charset.A, Code128Writer.MinimalEncoder.Charset.B
            };

            for (int i = 0; i <= 1; i++) {
               if (this.canEncode(contents, sets[i], position)) {
                  int cost = 1;
                  Code128Writer.MinimalEncoder.Latch latch = Code128Writer.MinimalEncoder.Latch.NONE;
                  if (charset != sets[i]) {
                     cost++;
                     latch = Code128Writer.MinimalEncoder.Latch.valueOf(sets[i].toString());
                  }

                  if (!atEnd) {
                     cost += this.encode(contents, sets[i], position + 1);
                  }

                  if (cost < minCost) {
                     minCost = cost;
                     minLatch = latch;
                  }

                  cost = 1;
                  if (charset == sets[(i + 1) % 2]) {
                     cost++;
                     latch = Code128Writer.MinimalEncoder.Latch.SHIFT;
                     if (!atEnd) {
                        cost += this.encode(contents, charset, position + 1);
                     }

                     if (cost < minCost) {
                        minCost = cost;
                        minLatch = latch;
                     }
                  }
               }
            }

            if (this.canEncode(contents, Code128Writer.MinimalEncoder.Charset.C, position)) {
               int costx = 1;
               Code128Writer.MinimalEncoder.Latch latchx = Code128Writer.MinimalEncoder.Latch.NONE;
               if (charset != Code128Writer.MinimalEncoder.Charset.C) {
                  costx++;
                  latchx = Code128Writer.MinimalEncoder.Latch.C;
               }

               int advance = contents.charAt(position) == 241 ? 1 : 2;
               if (position + advance < contents.length()) {
                  costx += this.encode(contents, Code128Writer.MinimalEncoder.Charset.C, position + advance);
               }

               if (costx < minCost) {
                  minCost = costx;
                  minLatch = latchx;
               }
            }

            if (minCost == Integer.MAX_VALUE) {
               throw new IllegalArgumentException("Bad character in input: ASCII value=" + contents.charAt(position));
            } else {
               this.memoizedCost[charset.ordinal()][position] = minCost;
               this.minPath[charset.ordinal()][position] = minLatch;
               return minCost;
            }
         }
      }

      private static enum Charset {
         A,
         B,
         C,
         NONE;
      }

      private static enum Latch {
         A,
         B,
         C,
         SHIFT,
         NONE;
      }
   }
}
