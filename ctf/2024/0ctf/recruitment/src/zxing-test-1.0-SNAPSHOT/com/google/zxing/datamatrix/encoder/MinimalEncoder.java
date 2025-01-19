package com.google.zxing.datamatrix.encoder;

import com.google.zxing.common.MinimalECIInput;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MinimalEncoder {
   static final char[] C40_SHIFT2_CHARS = new char[]{
      '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'
   };

   private MinimalEncoder() {
   }

   static boolean isExtendedASCII(char ch, int fnc1) {
      return ch != fnc1 && ch >= 128 && ch <= 255;
   }

   private static boolean isInC40Shift1Set(char ch) {
      return ch <= 31;
   }

   private static boolean isInC40Shift2Set(char ch, int fnc1) {
      for (char c40Shift2Char : C40_SHIFT2_CHARS) {
         if (c40Shift2Char == ch) {
            return true;
         }
      }

      return ch == fnc1;
   }

   private static boolean isInTextShift1Set(char ch) {
      return isInC40Shift1Set(ch);
   }

   private static boolean isInTextShift2Set(char ch, int fnc1) {
      return isInC40Shift2Set(ch, fnc1);
   }

   public static String encodeHighLevel(String msg) {
      return encodeHighLevel(msg, null, -1, SymbolShapeHint.FORCE_NONE);
   }

   public static String encodeHighLevel(String msg, Charset priorityCharset, int fnc1, SymbolShapeHint shape) {
      int macroId = 0;
      if (msg.startsWith("[)>\u001e05\u001d") && msg.endsWith("\u001e\u0004")) {
         macroId = 5;
         msg = msg.substring("[)>\u001e05\u001d".length(), msg.length() - 2);
      } else if (msg.startsWith("[)>\u001e06\u001d") && msg.endsWith("\u001e\u0004")) {
         macroId = 6;
         msg = msg.substring("[)>\u001e06\u001d".length(), msg.length() - 2);
      }

      return new String(encode(msg, priorityCharset, fnc1, shape, macroId), StandardCharsets.ISO_8859_1);
   }

   static byte[] encode(String input, Charset priorityCharset, int fnc1, SymbolShapeHint shape, int macroId) {
      return encodeMinimally(new MinimalEncoder.Input(input, priorityCharset, fnc1, shape, macroId)).getBytes();
   }

   static void addEdge(MinimalEncoder.Edge[][] edges, MinimalEncoder.Edge edge) {
      int vertexIndex = edge.fromPosition + edge.characterLength;
      if (edges[vertexIndex][edge.getEndMode().ordinal()] == null || edges[vertexIndex][edge.getEndMode().ordinal()].cachedTotalSize > edge.cachedTotalSize) {
         edges[vertexIndex][edge.getEndMode().ordinal()] = edge;
      }
   }

   static int getNumberOfC40Words(MinimalEncoder.Input input, int from, boolean c40, int[] characterLength) {
      int thirdsCount = 0;

      for (int i = from; i < input.length(); i++) {
         if (input.isECI(i)) {
            characterLength[0] = 0;
            return 0;
         }

         char ci = input.charAt(i);
         if ((!c40 || !HighLevelEncoder.isNativeC40(ci)) && (c40 || !HighLevelEncoder.isNativeText(ci))) {
            if (!isExtendedASCII(ci, input.getFNC1Character())) {
               thirdsCount += 2;
            } else {
               int asciiValue = ci & 255;
               if (asciiValue < 128
                  || (!c40 || !HighLevelEncoder.isNativeC40((char)(asciiValue - 128))) && (c40 || !HighLevelEncoder.isNativeText((char)(asciiValue - 128)))) {
                  thirdsCount += 4;
               } else {
                  thirdsCount += 3;
               }
            }
         } else {
            thirdsCount++;
         }

         if (thirdsCount % 3 == 0 || (thirdsCount - 2) % 3 == 0 && i + 1 == input.length()) {
            characterLength[0] = i - from + 1;
            return (int)Math.ceil((double)thirdsCount / 3.0);
         }
      }

      characterLength[0] = 0;
      return 0;
   }

   static void addEdges(MinimalEncoder.Input input, MinimalEncoder.Edge[][] edges, int from, MinimalEncoder.Edge previous) {
      if (input.isECI(from)) {
         addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.ASCII, from, 1, previous));
      } else {
         char ch = input.charAt(from);
         if (previous == null || previous.getEndMode() != MinimalEncoder.Mode.EDF) {
            if (HighLevelEncoder.isDigit(ch) && input.haveNCharacters(from, 2) && HighLevelEncoder.isDigit(input.charAt(from + 1))) {
               addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.ASCII, from, 2, previous));
            } else {
               addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.ASCII, from, 1, previous));
            }

            MinimalEncoder.Mode[] modes = new MinimalEncoder.Mode[]{MinimalEncoder.Mode.C40, MinimalEncoder.Mode.TEXT};

            for (MinimalEncoder.Mode mode : modes) {
               int[] characterLength = new int[1];
               if (getNumberOfC40Words(input, from, mode == MinimalEncoder.Mode.C40, characterLength) > 0) {
                  addEdge(edges, new MinimalEncoder.Edge(input, mode, from, characterLength[0], previous));
               }
            }

            if (input.haveNCharacters(from, 3)
               && HighLevelEncoder.isNativeX12(input.charAt(from))
               && HighLevelEncoder.isNativeX12(input.charAt(from + 1))
               && HighLevelEncoder.isNativeX12(input.charAt(from + 2))) {
               addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.X12, from, 3, previous));
            }

            addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.B256, from, 1, previous));
         }

         int i;
         for (i = 0; i < 3; i++) {
            int pos = from + i;
            if (!input.haveNCharacters(pos, 1) || !HighLevelEncoder.isNativeEDIFACT(input.charAt(pos))) {
               break;
            }

            addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.EDF, from, i + 1, previous));
         }

         if (i == 3 && input.haveNCharacters(from, 4) && HighLevelEncoder.isNativeEDIFACT(input.charAt(from + 3))) {
            addEdge(edges, new MinimalEncoder.Edge(input, MinimalEncoder.Mode.EDF, from, 4, previous));
         }
      }
   }

   static MinimalEncoder.Result encodeMinimally(MinimalEncoder.Input input) {
      int inputLength = input.length();
      MinimalEncoder.Edge[][] edges = new MinimalEncoder.Edge[inputLength + 1][6];
      addEdges(input, edges, 0, null);

      for (int i = 1; i <= inputLength; i++) {
         for (int j = 0; j < 6; j++) {
            if (edges[i][j] != null && i < inputLength) {
               addEdges(input, edges, i, edges[i][j]);
            }
         }

         for (int jx = 0; jx < 6; jx++) {
            edges[i - 1][jx] = null;
         }
      }

      int minimalJ = -1;
      int minimalSize = Integer.MAX_VALUE;

      for (int jx = 0; jx < 6; jx++) {
         if (edges[inputLength][jx] != null) {
            MinimalEncoder.Edge edge = edges[inputLength][jx];
            int size = jx >= 1 && jx <= 3 ? edge.cachedTotalSize + 1 : edge.cachedTotalSize;
            if (size < minimalSize) {
               minimalSize = size;
               minimalJ = jx;
            }
         }
      }

      if (minimalJ < 0) {
         throw new IllegalStateException("Failed to encode \"" + input + "\"");
      } else {
         return new MinimalEncoder.Result(edges[inputLength][minimalJ]);
      }
   }

   private static final class Edge {
      private static final int[] allCodewordCapacities = new int[]{
         3, 5, 8, 10, 12, 16, 18, 22, 30, 32, 36, 44, 49, 62, 86, 114, 144, 174, 204, 280, 368, 456, 576, 696, 816, 1050, 1304, 1558
      };
      private static final int[] squareCodewordCapacities = new int[]{
         3, 5, 8, 12, 18, 22, 30, 36, 44, 62, 86, 114, 144, 174, 204, 280, 368, 456, 576, 696, 816, 1050, 1304, 1558
      };
      private static final int[] rectangularCodewordCapacities = new int[]{5, 10, 16, 33, 32, 49};
      private final MinimalEncoder.Input input;
      private final MinimalEncoder.Mode mode;
      private final int fromPosition;
      private final int characterLength;
      private final MinimalEncoder.Edge previous;
      private final int cachedTotalSize;

      private Edge(MinimalEncoder.Input input, MinimalEncoder.Mode mode, int fromPosition, int characterLength, MinimalEncoder.Edge previous) {
         this.input = input;
         this.mode = mode;
         this.fromPosition = fromPosition;
         this.characterLength = characterLength;
         this.previous = previous;

         assert fromPosition + characterLength <= input.length();

         int size = previous != null ? previous.cachedTotalSize : 0;
         MinimalEncoder.Mode previousMode = this.getPreviousMode();
         switch (mode) {
            case ASCII:
               size++;
               if (input.isECI(fromPosition) || MinimalEncoder.isExtendedASCII(input.charAt(fromPosition), input.getFNC1Character())) {
                  size++;
               }

               if (previousMode == MinimalEncoder.Mode.C40 || previousMode == MinimalEncoder.Mode.TEXT || previousMode == MinimalEncoder.Mode.X12) {
                  size++;
               }
               break;
            case B256:
               size++;
               if (previousMode != MinimalEncoder.Mode.B256) {
                  size++;
               } else if (this.getB256Size() == 250) {
                  size++;
               }

               if (previousMode == MinimalEncoder.Mode.ASCII) {
                  size++;
               } else if (previousMode == MinimalEncoder.Mode.C40 || previousMode == MinimalEncoder.Mode.TEXT || previousMode == MinimalEncoder.Mode.X12) {
                  size += 2;
               }
               break;
            case C40:
            case TEXT:
            case X12:
               if (mode == MinimalEncoder.Mode.X12) {
                  size += 2;
               } else {
                  int[] charLen = new int[1];
                  size += MinimalEncoder.getNumberOfC40Words(input, fromPosition, mode == MinimalEncoder.Mode.C40, charLen) * 2;
               }

               if (previousMode == MinimalEncoder.Mode.ASCII || previousMode == MinimalEncoder.Mode.B256) {
                  size++;
               } else if (previousMode != mode
                  && (previousMode == MinimalEncoder.Mode.C40 || previousMode == MinimalEncoder.Mode.TEXT || previousMode == MinimalEncoder.Mode.X12)) {
                  size += 2;
               }
               break;
            case EDF:
               size += 3;
               if (previousMode == MinimalEncoder.Mode.ASCII || previousMode == MinimalEncoder.Mode.B256) {
                  size++;
               } else if (previousMode == MinimalEncoder.Mode.C40 || previousMode == MinimalEncoder.Mode.TEXT || previousMode == MinimalEncoder.Mode.X12) {
                  size += 2;
               }
         }

         this.cachedTotalSize = size;
      }

      int getB256Size() {
         int cnt = 0;

         for (MinimalEncoder.Edge current = this; current != null && current.mode == MinimalEncoder.Mode.B256 && cnt <= 250; current = current.previous) {
            cnt++;
         }

         return cnt;
      }

      MinimalEncoder.Mode getPreviousStartMode() {
         return this.previous == null ? MinimalEncoder.Mode.ASCII : this.previous.mode;
      }

      MinimalEncoder.Mode getPreviousMode() {
         return this.previous == null ? MinimalEncoder.Mode.ASCII : this.previous.getEndMode();
      }

      MinimalEncoder.Mode getEndMode() {
         if (this.mode == MinimalEncoder.Mode.EDF) {
            if (this.characterLength < 4) {
               return MinimalEncoder.Mode.ASCII;
            }

            int lastASCII = this.getLastASCII();
            if (lastASCII > 0 && this.getCodewordsRemaining(this.cachedTotalSize + lastASCII) <= 2 - lastASCII) {
               return MinimalEncoder.Mode.ASCII;
            }
         }

         if (this.mode == MinimalEncoder.Mode.C40 || this.mode == MinimalEncoder.Mode.TEXT || this.mode == MinimalEncoder.Mode.X12) {
            if (this.fromPosition + this.characterLength >= this.input.length() && this.getCodewordsRemaining(this.cachedTotalSize) == 0) {
               return MinimalEncoder.Mode.ASCII;
            }

            int lastASCII = this.getLastASCII();
            if (lastASCII == 1 && this.getCodewordsRemaining(this.cachedTotalSize + 1) == 0) {
               return MinimalEncoder.Mode.ASCII;
            }
         }

         return this.mode;
      }

      MinimalEncoder.Mode getMode() {
         return this.mode;
      }

      int getLastASCII() {
         int length = this.input.length();
         int from = this.fromPosition + this.characterLength;
         if (length - from <= 4 && from < length) {
            if (length - from == 1) {
               return MinimalEncoder.isExtendedASCII(this.input.charAt(from), this.input.getFNC1Character()) ? 0 : 1;
            } else if (length - from == 2) {
               if (MinimalEncoder.isExtendedASCII(this.input.charAt(from), this.input.getFNC1Character())
                  || MinimalEncoder.isExtendedASCII(this.input.charAt(from + 1), this.input.getFNC1Character())) {
                  return 0;
               } else {
                  return HighLevelEncoder.isDigit(this.input.charAt(from)) && HighLevelEncoder.isDigit(this.input.charAt(from + 1)) ? 1 : 2;
               }
            } else if (length - from == 3) {
               if (HighLevelEncoder.isDigit(this.input.charAt(from))
                  && HighLevelEncoder.isDigit(this.input.charAt(from + 1))
                  && !MinimalEncoder.isExtendedASCII(this.input.charAt(from + 2), this.input.getFNC1Character())) {
                  return 2;
               } else {
                  return HighLevelEncoder.isDigit(this.input.charAt(from + 1))
                        && HighLevelEncoder.isDigit(this.input.charAt(from + 2))
                        && !MinimalEncoder.isExtendedASCII(this.input.charAt(from), this.input.getFNC1Character())
                     ? 2
                     : 0;
               }
            } else {
               return HighLevelEncoder.isDigit(this.input.charAt(from))
                     && HighLevelEncoder.isDigit(this.input.charAt(from + 1))
                     && HighLevelEncoder.isDigit(this.input.charAt(from + 2))
                     && HighLevelEncoder.isDigit(this.input.charAt(from + 3))
                  ? 2
                  : 0;
            }
         } else {
            return 0;
         }
      }

      int getMinSymbolSize(int minimum) {
         switch (this.input.getShapeHint()) {
            case FORCE_SQUARE:
               for (int capacityx : squareCodewordCapacities) {
                  if (capacityx >= minimum) {
                     return capacityx;
                  }
               }
               break;
            case FORCE_RECTANGLE:
               for (int capacity : rectangularCodewordCapacities) {
                  if (capacity >= minimum) {
                     return capacity;
                  }
               }
         }

         for (int capacityxx : allCodewordCapacities) {
            if (capacityxx >= minimum) {
               return capacityxx;
            }
         }

         return allCodewordCapacities[allCodewordCapacities.length - 1];
      }

      int getCodewordsRemaining(int minimum) {
         return this.getMinSymbolSize(minimum) - minimum;
      }

      static byte[] getBytes(int c) {
         return new byte[]{(byte)c};
      }

      static byte[] getBytes(int c1, int c2) {
         return new byte[]{(byte)c1, (byte)c2};
      }

      static void setC40Word(byte[] bytes, int offset, int c1, int c2, int c3) {
         int val16 = 1600 * (c1 & 0xFF) + 40 * (c2 & 0xFF) + (c3 & 0xFF) + 1;
         bytes[offset] = (byte)(val16 / 256);
         bytes[offset + 1] = (byte)(val16 % 256);
      }

      private static int getX12Value(char c) {
         return c == 13 ? 0 : (c == 42 ? 1 : (c == 62 ? 2 : (c == 32 ? 3 : (c >= 48 && c <= 57 ? c - 44 : (c >= 65 && c <= 90 ? c - 51 : c)))));
      }

      byte[] getX12Words() {
         assert this.characterLength % 3 == 0;

         byte[] result = new byte[this.characterLength / 3 * 2];

         for (int i = 0; i < result.length; i += 2) {
            setC40Word(
               result,
               i,
               getX12Value(this.input.charAt(this.fromPosition + i / 2 * 3)),
               getX12Value(this.input.charAt(this.fromPosition + i / 2 * 3 + 1)),
               getX12Value(this.input.charAt(this.fromPosition + i / 2 * 3 + 2))
            );
         }

         return result;
      }

      static int getShiftValue(char c, boolean c40, int fnc1) {
         return (!c40 || !MinimalEncoder.isInC40Shift1Set(c)) && (c40 || !MinimalEncoder.isInTextShift1Set(c))
            ? ((!c40 || !MinimalEncoder.isInC40Shift2Set(c, fnc1)) && (c40 || !MinimalEncoder.isInTextShift2Set(c, fnc1)) ? 2 : 1)
            : 0;
      }

      private static int getC40Value(boolean c40, int setIndex, char c, int fnc1) {
         if (c == fnc1) {
            assert setIndex == 2;

            return 27;
         } else if (c40) {
            return c <= 31
               ? c
               : (
                  c == 32
                     ? 3
                     : (c <= 47 ? c - 33 : (c <= 57 ? c - 44 : (c <= 64 ? c - 43 : (c <= 90 ? c - 51 : (c <= 95 ? c - 69 : (c <= 127 ? c - 96 : c))))))
               );
         } else {
            return c == 0
               ? 0
               : (
                  setIndex == 0 && c <= 3
                     ? c - 1
                     : (
                        setIndex == 1 && c <= 31
                           ? c
                           : (
                              c == 32
                                 ? 3
                                 : (
                                    c >= 33 && c <= 47
                                       ? c - 33
                                       : (
                                          c >= 48 && c <= 57
                                             ? c - 44
                                             : (
                                                c >= 58 && c <= 64
                                                   ? c - 43
                                                   : (
                                                      c >= 65 && c <= 90
                                                         ? c - 64
                                                         : (
                                                            c >= 91 && c <= 95
                                                               ? c - 69
                                                               : (c == 96 ? 0 : (c >= 97 && c <= 122 ? c - 83 : (c >= 123 && c <= 127 ? c - 96 : c)))
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               );
         }
      }

      byte[] getC40Words(boolean c40, int fnc1) {
         List<Byte> c40Values = new ArrayList<>();

         for (int i = 0; i < this.characterLength; i++) {
            char ci = this.input.charAt(this.fromPosition + i);
            if ((!c40 || !HighLevelEncoder.isNativeC40(ci)) && (c40 || !HighLevelEncoder.isNativeText(ci))) {
               if (!MinimalEncoder.isExtendedASCII(ci, fnc1)) {
                  int shiftValue = getShiftValue(ci, c40, fnc1);
                  c40Values.add((byte)shiftValue);
                  c40Values.add((byte)getC40Value(c40, shiftValue, ci, fnc1));
               } else {
                  char asciiValue = (char)((ci & 255) - 128);
                  if ((!c40 || !HighLevelEncoder.isNativeC40(asciiValue)) && (c40 || !HighLevelEncoder.isNativeText(asciiValue))) {
                     c40Values.add((byte)1);
                     c40Values.add((byte)30);
                     int shiftValue = getShiftValue(asciiValue, c40, fnc1);
                     c40Values.add((byte)shiftValue);
                     c40Values.add((byte)getC40Value(c40, shiftValue, asciiValue, fnc1));
                  } else {
                     c40Values.add((byte)1);
                     c40Values.add((byte)30);
                     c40Values.add((byte)getC40Value(c40, 0, asciiValue, fnc1));
                  }
               }
            } else {
               c40Values.add((byte)getC40Value(c40, 0, ci, fnc1));
            }
         }

         if (c40Values.size() % 3 != 0) {
            assert (c40Values.size() - 2) % 3 == 0 && this.fromPosition + this.characterLength == this.input.length();

            c40Values.add((byte)0);
         }

         byte[] result = new byte[c40Values.size() / 3 * 2];
         int byteIndex = 0;

         for (int ix = 0; ix < c40Values.size(); ix += 3) {
            setC40Word(result, byteIndex, c40Values.get(ix) & 255, c40Values.get(ix + 1) & 255, c40Values.get(ix + 2) & 255);
            byteIndex += 2;
         }

         return result;
      }

      byte[] getEDFBytes() {
         int numberOfThirds = (int)Math.ceil((double)this.characterLength / 4.0);
         byte[] result = new byte[numberOfThirds * 3];
         int pos = this.fromPosition;
         int endPos = Math.min(this.fromPosition + this.characterLength - 1, this.input.length() - 1);

         for (int i = 0; i < numberOfThirds; i += 3) {
            int[] edfValues = new int[4];

            for (int j = 0; j < 4; j++) {
               if (pos <= endPos) {
                  edfValues[j] = this.input.charAt(pos++) & '?';
               } else {
                  edfValues[j] = pos == endPos + 1 ? 31 : 0;
               }
            }

            int val24 = edfValues[0] << 18;
            val24 |= edfValues[1] << 12;
            val24 |= edfValues[2] << 6;
            val24 |= edfValues[3];
            result[i] = (byte)(val24 >> 16 & 0xFF);
            result[i + 1] = (byte)(val24 >> 8 & 0xFF);
            result[i + 2] = (byte)(val24 & 0xFF);
         }

         return result;
      }

      byte[] getLatchBytes() {
         switch (this.getPreviousMode()) {
            case ASCII:
            case B256:
               switch (this.mode) {
                  case B256:
                     return getBytes(231);
                  case C40:
                     return getBytes(230);
                  case TEXT:
                     return getBytes(239);
                  case X12:
                     return getBytes(238);
                  case EDF:
                     return getBytes(240);
                  default:
                     return new byte[0];
               }
            case C40:
            case TEXT:
            case X12:
               if (this.mode != this.getPreviousMode()) {
                  switch (this.mode) {
                     case ASCII:
                        return getBytes(254);
                     case B256:
                        return getBytes(254, 231);
                     case C40:
                        return getBytes(254, 230);
                     case TEXT:
                        return getBytes(254, 239);
                     case X12:
                        return getBytes(254, 238);
                     case EDF:
                        return getBytes(254, 240);
                  }
               }
               break;
            case EDF:
               assert this.mode == MinimalEncoder.Mode.EDF;
         }

         return new byte[0];
      }

      byte[] getDataBytes() {
         switch (this.mode) {
            case ASCII:
               if (this.input.isECI(this.fromPosition)) {
                  return getBytes(241, this.input.getECIValue(this.fromPosition) + 1);
               } else if (MinimalEncoder.isExtendedASCII(this.input.charAt(this.fromPosition), this.input.getFNC1Character())) {
                  return getBytes(235, this.input.charAt(this.fromPosition) - 127);
               } else if (this.characterLength == 2) {
                  return getBytes((this.input.charAt(this.fromPosition) - '0') * 10 + this.input.charAt(this.fromPosition + 1) - 48 + 130);
               } else {
                  if (this.input.isFNC1(this.fromPosition)) {
                     return getBytes(232);
                  }

                  return getBytes(this.input.charAt(this.fromPosition) + 1);
               }
            case B256:
               return getBytes(this.input.charAt(this.fromPosition));
            case C40:
               return this.getC40Words(true, this.input.getFNC1Character());
            case TEXT:
               return this.getC40Words(false, this.input.getFNC1Character());
            case X12:
               return this.getX12Words();
            case EDF:
               return this.getEDFBytes();
            default:
               assert false;

               return new byte[0];
         }
      }
   }

   private static final class Input extends MinimalECIInput {
      private final SymbolShapeHint shape;
      private final int macroId;

      private Input(String stringToEncode, Charset priorityCharset, int fnc1, SymbolShapeHint shape, int macroId) {
         super(stringToEncode, priorityCharset, fnc1);
         this.shape = shape;
         this.macroId = macroId;
      }

      private int getMacroId() {
         return this.macroId;
      }

      private SymbolShapeHint getShapeHint() {
         return this.shape;
      }
   }

   static enum Mode {
      ASCII,
      C40,
      TEXT,
      X12,
      EDF,
      B256;
   }

   private static final class Result {
      private final byte[] bytes;

      Result(MinimalEncoder.Edge solution) {
         MinimalEncoder.Input input = solution.input;
         int size = 0;
         List<Byte> bytesAL = new ArrayList<>();
         List<Integer> randomizePostfixLength = new ArrayList<>();
         List<Integer> randomizeLengths = new ArrayList<>();
         if ((solution.mode == MinimalEncoder.Mode.C40 || solution.mode == MinimalEncoder.Mode.TEXT || solution.mode == MinimalEncoder.Mode.X12)
            && solution.getEndMode() != MinimalEncoder.Mode.ASCII) {
            size += prepend(MinimalEncoder.Edge.getBytes(254), bytesAL);
         }

         for (MinimalEncoder.Edge current = solution; current != null; current = current.previous) {
            size += prepend(current.getDataBytes(), bytesAL);
            if (current.previous == null || current.getPreviousStartMode() != current.getMode()) {
               if (current.getMode() == MinimalEncoder.Mode.B256) {
                  if (size <= 249) {
                     bytesAL.add(0, (byte)size);
                     size++;
                  } else {
                     bytesAL.add(0, (byte)(size % 250));
                     bytesAL.add(0, (byte)(size / 250 + 249));
                     size += 2;
                  }

                  randomizePostfixLength.add(bytesAL.size());
                  randomizeLengths.add(size);
               }

               prepend(current.getLatchBytes(), bytesAL);
               size = 0;
            }
         }

         if (input.getMacroId() == 5) {
            size += prepend(MinimalEncoder.Edge.getBytes(236), bytesAL);
         } else if (input.getMacroId() == 6) {
            size += prepend(MinimalEncoder.Edge.getBytes(237), bytesAL);
         }

         if (input.getFNC1Character() > 0) {
            size += prepend(MinimalEncoder.Edge.getBytes(232), bytesAL);
         }

         for (int i = 0; i < randomizePostfixLength.size(); i++) {
            applyRandomPattern(bytesAL, bytesAL.size() - randomizePostfixLength.get(i), randomizeLengths.get(i));
         }

         int capacity = solution.getMinSymbolSize(bytesAL.size());
         if (bytesAL.size() < capacity) {
            bytesAL.add((byte)-127);
         }

         while (bytesAL.size() < capacity) {
            bytesAL.add((byte)randomize253State(bytesAL.size() + 1));
         }

         this.bytes = new byte[bytesAL.size()];

         for (int i = 0; i < this.bytes.length; i++) {
            this.bytes[i] = bytesAL.get(i);
         }
      }

      static int prepend(byte[] bytes, List<Byte> into) {
         for (int i = bytes.length - 1; i >= 0; i--) {
            into.add(0, bytes[i]);
         }

         return bytes.length;
      }

      private static int randomize253State(int codewordPosition) {
         int pseudoRandom = 149 * codewordPosition % 253 + 1;
         int tempVariable = 129 + pseudoRandom;
         return tempVariable <= 254 ? tempVariable : tempVariable - 254;
      }

      static void applyRandomPattern(List<Byte> bytesAL, int startPosition, int length) {
         for (int i = 0; i < length; i++) {
            int Pad_codeword_position = startPosition + i;
            int Pad_codeword_value = bytesAL.get(Pad_codeword_position) & 255;
            int pseudo_random_number = 149 * (Pad_codeword_position + 1) % 255 + 1;
            int temp_variable = Pad_codeword_value + pseudo_random_number;
            bytesAL.set(Pad_codeword_position, (byte)(temp_variable <= 255 ? temp_variable : temp_variable - 256));
         }
      }

      public byte[] getBytes() {
         return this.bytes;
      }
   }
}
