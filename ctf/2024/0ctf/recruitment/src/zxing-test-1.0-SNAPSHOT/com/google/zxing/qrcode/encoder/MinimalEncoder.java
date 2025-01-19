package com.google.zxing.qrcode.encoder;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.ECIEncoderSet;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

final class MinimalEncoder {
   private final String stringToEncode;
   private final boolean isGS1;
   private final ECIEncoderSet encoders;
   private final ErrorCorrectionLevel ecLevel;

   MinimalEncoder(String stringToEncode, Charset priorityCharset, boolean isGS1, ErrorCorrectionLevel ecLevel) {
      this.stringToEncode = stringToEncode;
      this.isGS1 = isGS1;
      this.encoders = new ECIEncoderSet(stringToEncode, priorityCharset, -1);
      this.ecLevel = ecLevel;
   }

   static MinimalEncoder.ResultList encode(String stringToEncode, Version version, Charset priorityCharset, boolean isGS1, ErrorCorrectionLevel ecLevel) throws WriterException {
      return new MinimalEncoder(stringToEncode, priorityCharset, isGS1, ecLevel).encode(version);
   }

   MinimalEncoder.ResultList encode(Version version) throws WriterException {
      if (version == null) {
         Version[] versions = new Version[]{
            getVersion(MinimalEncoder.VersionSize.SMALL), getVersion(MinimalEncoder.VersionSize.MEDIUM), getVersion(MinimalEncoder.VersionSize.LARGE)
         };
         MinimalEncoder.ResultList[] results = new MinimalEncoder.ResultList[]{
            this.encodeSpecificVersion(versions[0]), this.encodeSpecificVersion(versions[1]), this.encodeSpecificVersion(versions[2])
         };
         int smallestSize = Integer.MAX_VALUE;
         int smallestResult = -1;

         for (int i = 0; i < 3; i++) {
            int size = results[i].getSize();
            if (Encoder.willFit(size, versions[i], this.ecLevel) && size < smallestSize) {
               smallestSize = size;
               smallestResult = i;
            }
         }

         if (smallestResult < 0) {
            throw new WriterException("Data too big for any version");
         } else {
            return results[smallestResult];
         }
      } else {
         MinimalEncoder.ResultList result = this.encodeSpecificVersion(version);
         if (!Encoder.willFit(result.getSize(), getVersion(getVersionSize(result.getVersion())), this.ecLevel)) {
            throw new WriterException("Data too big for version" + version);
         } else {
            return result;
         }
      }
   }

   static MinimalEncoder.VersionSize getVersionSize(Version version) {
      return version.getVersionNumber() <= 9
         ? MinimalEncoder.VersionSize.SMALL
         : (version.getVersionNumber() <= 26 ? MinimalEncoder.VersionSize.MEDIUM : MinimalEncoder.VersionSize.LARGE);
   }

   static Version getVersion(MinimalEncoder.VersionSize versionSize) {
      switch (versionSize) {
         case SMALL:
            return Version.getVersionForNumber(9);
         case MEDIUM:
            return Version.getVersionForNumber(26);
         case LARGE:
         default:
            return Version.getVersionForNumber(40);
      }
   }

   static boolean isNumeric(char c) {
      return c >= '0' && c <= '9';
   }

   static boolean isDoubleByteKanji(char c) {
      return Encoder.isOnlyDoubleByteKanji(String.valueOf(c));
   }

   static boolean isAlphanumeric(char c) {
      return Encoder.getAlphanumericCode(c) != -1;
   }

   boolean canEncode(Mode mode, char c) {
      switch (mode) {
         case KANJI:
            return isDoubleByteKanji(c);
         case ALPHANUMERIC:
            return isAlphanumeric(c);
         case NUMERIC:
            return isNumeric(c);
         case BYTE:
            return true;
         default:
            return false;
      }
   }

   static int getCompactedOrdinal(Mode mode) {
      if (mode == null) {
         return 0;
      } else {
         switch (mode) {
            case KANJI:
               return 0;
            case ALPHANUMERIC:
               return 1;
            case NUMERIC:
               return 2;
            case BYTE:
               return 3;
            default:
               throw new IllegalStateException("Illegal mode " + mode);
         }
      }
   }

   void addEdge(MinimalEncoder.Edge[][][] edges, int position, MinimalEncoder.Edge edge) {
      int vertexIndex = position + edge.characterLength;
      MinimalEncoder.Edge[] modeEdges = edges[vertexIndex][edge.charsetEncoderIndex];
      int modeOrdinal = getCompactedOrdinal(edge.mode);
      if (modeEdges[modeOrdinal] == null || modeEdges[modeOrdinal].cachedTotalSize > edge.cachedTotalSize) {
         modeEdges[modeOrdinal] = edge;
      }
   }

   void addEdges(Version version, MinimalEncoder.Edge[][][] edges, int from, MinimalEncoder.Edge previous) {
      int start = 0;
      int end = this.encoders.length();
      int priorityEncoderIndex = this.encoders.getPriorityEncoderIndex();
      if (priorityEncoderIndex >= 0 && this.encoders.canEncode(this.stringToEncode.charAt(from), priorityEncoderIndex)) {
         start = priorityEncoderIndex;
         end = priorityEncoderIndex + 1;
      }

      for (int i = start; i < end; i++) {
         if (this.encoders.canEncode(this.stringToEncode.charAt(from), i)) {
            this.addEdge(edges, from, new MinimalEncoder.Edge(Mode.BYTE, from, i, 1, previous, version));
         }
      }

      if (this.canEncode(Mode.KANJI, this.stringToEncode.charAt(from))) {
         this.addEdge(edges, from, new MinimalEncoder.Edge(Mode.KANJI, from, 0, 1, previous, version));
      }

      int inputLength = this.stringToEncode.length();
      if (this.canEncode(Mode.ALPHANUMERIC, this.stringToEncode.charAt(from))) {
         this.addEdge(
            edges,
            from,
            new MinimalEncoder.Edge(
               Mode.ALPHANUMERIC,
               from,
               0,
               from + 1 < inputLength && this.canEncode(Mode.ALPHANUMERIC, this.stringToEncode.charAt(from + 1)) ? 2 : 1,
               previous,
               version
            )
         );
      }

      if (this.canEncode(Mode.NUMERIC, this.stringToEncode.charAt(from))) {
         this.addEdge(
            edges,
            from,
            new MinimalEncoder.Edge(
               Mode.NUMERIC,
               from,
               0,
               from + 1 >= inputLength || !this.canEncode(Mode.NUMERIC, this.stringToEncode.charAt(from + 1))
                  ? 1
                  : (from + 2 < inputLength && this.canEncode(Mode.NUMERIC, this.stringToEncode.charAt(from + 2)) ? 3 : 2),
               previous,
               version
            )
         );
      }
   }

   MinimalEncoder.ResultList encodeSpecificVersion(Version version) throws WriterException {
      int inputLength = this.stringToEncode.length();
      MinimalEncoder.Edge[][][] edges = new MinimalEncoder.Edge[inputLength + 1][this.encoders.length()][4];
      this.addEdges(version, edges, 0, null);

      for (int i = 1; i <= inputLength; i++) {
         for (int j = 0; j < this.encoders.length(); j++) {
            for (int k = 0; k < 4; k++) {
               if (edges[i][j][k] != null && i < inputLength) {
                  this.addEdges(version, edges, i, edges[i][j][k]);
               }
            }
         }
      }

      int minimalJ = -1;
      int minimalK = -1;
      int minimalSize = Integer.MAX_VALUE;

      for (int j = 0; j < this.encoders.length(); j++) {
         for (int kx = 0; kx < 4; kx++) {
            if (edges[inputLength][j][kx] != null) {
               MinimalEncoder.Edge edge = edges[inputLength][j][kx];
               if (edge.cachedTotalSize < minimalSize) {
                  minimalSize = edge.cachedTotalSize;
                  minimalJ = j;
                  minimalK = kx;
               }
            }
         }
      }

      if (minimalJ < 0) {
         throw new WriterException("Internal error: failed to encode \"" + this.stringToEncode + "\"");
      } else {
         return new MinimalEncoder.ResultList(version, edges[inputLength][minimalJ][minimalK]);
      }
   }

   private final class Edge {
      private final Mode mode;
      private final int fromPosition;
      private final int charsetEncoderIndex;
      private final int characterLength;
      private final MinimalEncoder.Edge previous;
      private final int cachedTotalSize;

      private Edge(Mode mode, int fromPosition, int charsetEncoderIndex, int characterLength, MinimalEncoder.Edge previous, Version version) {
         this.mode = mode;
         this.fromPosition = fromPosition;
         this.charsetEncoderIndex = mode != Mode.BYTE && previous != null ? previous.charsetEncoderIndex : charsetEncoderIndex;
         this.characterLength = characterLength;
         this.previous = previous;
         int size = previous != null ? previous.cachedTotalSize : 0;
         boolean needECI = mode == Mode.BYTE && previous == null && this.charsetEncoderIndex != 0
            || previous != null && this.charsetEncoderIndex != previous.charsetEncoderIndex;
         if (previous == null || mode != previous.mode || needECI) {
            size += 4 + mode.getCharacterCountBits(version);
         }

         switch (mode) {
            case KANJI:
               size += 13;
               break;
            case ALPHANUMERIC:
               size += characterLength == 1 ? 6 : 11;
               break;
            case NUMERIC:
               size += characterLength == 1 ? 4 : (characterLength == 2 ? 7 : 10);
               break;
            case BYTE:
               size += 8
                  * MinimalEncoder.this.encoders
                     .encode(MinimalEncoder.this.stringToEncode.substring(fromPosition, fromPosition + characterLength), charsetEncoderIndex).length;
               if (needECI) {
                  size += 12;
               }
         }

         this.cachedTotalSize = size;
      }
   }

   final class ResultList {
      private final List<MinimalEncoder.ResultList.ResultNode> list = new ArrayList<>();
      private final Version version;

      ResultList(Version version, MinimalEncoder.Edge solution) {
         int length = 0;
         MinimalEncoder.Edge current = solution;
         boolean containsECI = false;

         while (current != null) {
            length += current.characterLength;
            MinimalEncoder.Edge previous = current.previous;
            boolean needECI = current.mode == Mode.BYTE && previous == null && current.charsetEncoderIndex != 0
               || previous != null && current.charsetEncoderIndex != previous.charsetEncoderIndex;
            if (needECI) {
               containsECI = true;
            }

            if (previous == null || previous.mode != current.mode || needECI) {
               this.list.add(0, new MinimalEncoder.ResultList.ResultNode(current.mode, current.fromPosition, current.charsetEncoderIndex, length));
               length = 0;
            }

            if (needECI) {
               this.list.add(0, new MinimalEncoder.ResultList.ResultNode(Mode.ECI, current.fromPosition, current.charsetEncoderIndex, 0));
            }

            current = previous;
         }

         if (MinimalEncoder.this.isGS1) {
            MinimalEncoder.ResultList.ResultNode first = this.list.get(0);
            if (first != null && first.mode != Mode.ECI && containsECI) {
               this.list.add(0, new MinimalEncoder.ResultList.ResultNode(Mode.ECI, 0, 0, 0));
            }

            first = this.list.get(0);
            this.list.add(first.mode != Mode.ECI ? 0 : 1, new MinimalEncoder.ResultList.ResultNode(Mode.FNC1_FIRST_POSITION, 0, 0, 0));
         }

         int versionNumber = version.getVersionNumber();
         int upperLimit;
         int lowerLimit;
         switch (MinimalEncoder.getVersionSize(version)) {
            case SMALL:
               lowerLimit = 1;
               upperLimit = 9;
               break;
            case MEDIUM:
               lowerLimit = 10;
               upperLimit = 26;
               break;
            case LARGE:
            default:
               lowerLimit = 27;
               upperLimit = 40;
         }

         int size = this.getSize(version);

         while (versionNumber < upperLimit && !Encoder.willFit(size, Version.getVersionForNumber(versionNumber), MinimalEncoder.this.ecLevel)) {
            versionNumber++;
         }

         while (versionNumber > lowerLimit && Encoder.willFit(size, Version.getVersionForNumber(versionNumber - 1), MinimalEncoder.this.ecLevel)) {
            versionNumber--;
         }

         this.version = Version.getVersionForNumber(versionNumber);
      }

      int getSize() {
         return this.getSize(this.version);
      }

      private int getSize(Version version) {
         int result = 0;

         for (MinimalEncoder.ResultList.ResultNode resultNode : this.list) {
            result += resultNode.getSize(version);
         }

         return result;
      }

      void getBits(BitArray bits) throws WriterException {
         for (MinimalEncoder.ResultList.ResultNode resultNode : this.list) {
            resultNode.getBits(bits);
         }
      }

      Version getVersion() {
         return this.version;
      }

      @Override
      public String toString() {
         StringBuilder result = new StringBuilder();
         MinimalEncoder.ResultList.ResultNode previous = null;

         for (MinimalEncoder.ResultList.ResultNode current : this.list) {
            if (previous != null) {
               result.append(",");
            }

            result.append(current.toString());
            previous = current;
         }

         return result.toString();
      }

      final class ResultNode {
         private final Mode mode;
         private final int fromPosition;
         private final int charsetEncoderIndex;
         private final int characterLength;

         ResultNode(Mode mode, int fromPosition, int charsetEncoderIndex, int characterLength) {
            this.mode = mode;
            this.fromPosition = fromPosition;
            this.charsetEncoderIndex = charsetEncoderIndex;
            this.characterLength = characterLength;
         }

         private int getSize(Version version) {
            int size = 4 + this.mode.getCharacterCountBits(version);
            switch (this.mode) {
               case KANJI:
                  size += 13 * this.characterLength;
                  break;
               case ALPHANUMERIC:
                  size += this.characterLength / 2 * 11;
                  size += this.characterLength % 2 == 1 ? 6 : 0;
                  break;
               case NUMERIC:
                  size += this.characterLength / 3 * 10;
                  int rest = this.characterLength % 3;
                  size += rest == 1 ? 4 : (rest == 2 ? 7 : 0);
                  break;
               case BYTE:
                  size += 8 * this.getCharacterCountIndicator();
                  break;
               case ECI:
                  size += 8;
            }

            return size;
         }

         private int getCharacterCountIndicator() {
            return this.mode == Mode.BYTE
               ? MinimalEncoder.this.encoders
                  .encode(MinimalEncoder.this.stringToEncode.substring(this.fromPosition, this.fromPosition + this.characterLength), this.charsetEncoderIndex).length
               : this.characterLength;
         }

         private void getBits(BitArray bits) throws WriterException {
            bits.appendBits(this.mode.getBits(), 4);
            if (this.characterLength > 0) {
               int length = this.getCharacterCountIndicator();
               bits.appendBits(length, this.mode.getCharacterCountBits(ResultList.this.version));
            }

            if (this.mode == Mode.ECI) {
               bits.appendBits(MinimalEncoder.this.encoders.getECIValue(this.charsetEncoderIndex), 8);
            } else if (this.characterLength > 0) {
               Encoder.appendBytes(
                  MinimalEncoder.this.stringToEncode.substring(this.fromPosition, this.fromPosition + this.characterLength),
                  this.mode,
                  bits,
                  MinimalEncoder.this.encoders.getCharset(this.charsetEncoderIndex)
               );
            }
         }

         @Override
         public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(this.mode).append('(');
            if (this.mode == Mode.ECI) {
               result.append(MinimalEncoder.this.encoders.getCharset(this.charsetEncoderIndex).displayName());
            } else {
               result.append(this.makePrintable(MinimalEncoder.this.stringToEncode.substring(this.fromPosition, this.fromPosition + this.characterLength)));
            }

            result.append(')');
            return result.toString();
         }

         private String makePrintable(String s) {
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < s.length(); i++) {
               if (s.charAt(i) >= ' ' && s.charAt(i) <= '~') {
                  result.append(s.charAt(i));
               } else {
                  result.append('.');
               }
            }

            return result.toString();
         }
      }
   }

   private static enum VersionSize {
      SMALL("version 1-9"),
      MEDIUM("version 10-26"),
      LARGE("version 27-40");

      private final String description;

      private VersionSize(String description) {
         this.description = description;
      }

      @Override
      public String toString() {
         return this.description;
      }
   }
}
