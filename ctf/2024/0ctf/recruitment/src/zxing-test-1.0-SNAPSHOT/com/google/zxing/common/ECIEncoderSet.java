package com.google.zxing.common;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

public final class ECIEncoderSet {
   private static final List<CharsetEncoder> ENCODERS = new ArrayList<>();
   private final CharsetEncoder[] encoders;
   private final int priorityEncoderIndex;

   public ECIEncoderSet(String stringToEncode, Charset priorityCharset, int fnc1) {
      List<CharsetEncoder> neededEncoders = new ArrayList<>();
      neededEncoders.add(StandardCharsets.ISO_8859_1.newEncoder());
      boolean needUnicodeEncoder = priorityCharset != null && priorityCharset.name().startsWith("UTF");

      for (int i = 0; i < stringToEncode.length(); i++) {
         boolean canEncode = false;

         for (CharsetEncoder encoder : neededEncoders) {
            char c = stringToEncode.charAt(i);
            if (c == fnc1 || encoder.canEncode(c)) {
               canEncode = true;
               break;
            }
         }

         if (!canEncode) {
            for (CharsetEncoder encoderx : ENCODERS) {
               if (encoderx.canEncode(stringToEncode.charAt(i))) {
                  neededEncoders.add(encoderx);
                  canEncode = true;
                  break;
               }
            }
         }

         if (!canEncode) {
            needUnicodeEncoder = true;
         }
      }

      if (neededEncoders.size() == 1 && !needUnicodeEncoder) {
         this.encoders = new CharsetEncoder[]{neededEncoders.get(0)};
      } else {
         this.encoders = new CharsetEncoder[neededEncoders.size() + 2];
         int index = 0;

         for (CharsetEncoder encoderxx : neededEncoders) {
            this.encoders[index++] = encoderxx;
         }

         this.encoders[index] = StandardCharsets.UTF_8.newEncoder();
         this.encoders[index + 1] = StandardCharsets.UTF_16BE.newEncoder();
      }

      int priorityEncoderIndexValue = -1;
      if (priorityCharset != null) {
         for (int i = 0; i < this.encoders.length; i++) {
            if (this.encoders[i] != null && priorityCharset.name().equals(this.encoders[i].charset().name())) {
               priorityEncoderIndexValue = i;
               break;
            }
         }
      }

      this.priorityEncoderIndex = priorityEncoderIndexValue;

      assert this.encoders[0].charset().equals(StandardCharsets.ISO_8859_1);
   }

   public int length() {
      return this.encoders.length;
   }

   public String getCharsetName(int index) {
      assert index < this.length();

      return this.encoders[index].charset().name();
   }

   public Charset getCharset(int index) {
      assert index < this.length();

      return this.encoders[index].charset();
   }

   public int getECIValue(int encoderIndex) {
      return CharacterSetECI.getCharacterSetECI(this.encoders[encoderIndex].charset()).getValue();
   }

   public int getPriorityEncoderIndex() {
      return this.priorityEncoderIndex;
   }

   public boolean canEncode(char c, int encoderIndex) {
      assert encoderIndex < this.length();

      CharsetEncoder encoder = this.encoders[encoderIndex];
      return encoder.canEncode("" + c);
   }

   public byte[] encode(char c, int encoderIndex) {
      assert encoderIndex < this.length();

      CharsetEncoder encoder = this.encoders[encoderIndex];

      assert encoder.canEncode("" + c);

      return ("" + c).getBytes(encoder.charset());
   }

   public byte[] encode(String s, int encoderIndex) {
      assert encoderIndex < this.length();

      CharsetEncoder encoder = this.encoders[encoderIndex];
      return s.getBytes(encoder.charset());
   }

   static {
      String[] names = new String[]{
         "IBM437",
         "ISO-8859-2",
         "ISO-8859-3",
         "ISO-8859-4",
         "ISO-8859-5",
         "ISO-8859-6",
         "ISO-8859-7",
         "ISO-8859-8",
         "ISO-8859-9",
         "ISO-8859-10",
         "ISO-8859-11",
         "ISO-8859-13",
         "ISO-8859-14",
         "ISO-8859-15",
         "ISO-8859-16",
         "windows-1250",
         "windows-1251",
         "windows-1252",
         "windows-1256",
         "Shift_JIS"
      };

      for (String name : names) {
         if (CharacterSetECI.getCharacterSetECIByName(name) != null) {
            try {
               ENCODERS.add(Charset.forName(name).newEncoder());
            } catch (UnsupportedCharsetException var6) {
            }
         }
      }
   }
}
