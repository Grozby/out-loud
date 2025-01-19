package com.google.zxing.common;

import com.google.zxing.FormatException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class ECIStringBuilder {
   private StringBuilder currentBytes;
   private StringBuilder result;
   private Charset currentCharset = StandardCharsets.ISO_8859_1;

   public ECIStringBuilder() {
      this.currentBytes = new StringBuilder();
   }

   public ECIStringBuilder(int initialCapacity) {
      this.currentBytes = new StringBuilder(initialCapacity);
   }

   public void append(char value) {
      this.currentBytes.append((char)(value & 255));
   }

   public void append(byte value) {
      this.currentBytes.append((char)(value & 255));
   }

   public void append(String value) {
      this.currentBytes.append(value);
   }

   public void append(int value) {
      this.append(String.valueOf(value));
   }

   public void appendECI(int value) throws FormatException {
      this.encodeCurrentBytesIfAny();
      CharacterSetECI characterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
      if (characterSetECI == null) {
         throw FormatException.getFormatInstance();
      } else {
         this.currentCharset = characterSetECI.getCharset();
      }
   }

   private void encodeCurrentBytesIfAny() {
      if (this.currentCharset.equals(StandardCharsets.ISO_8859_1)) {
         if (this.currentBytes.length() > 0) {
            if (this.result == null) {
               this.result = this.currentBytes;
               this.currentBytes = new StringBuilder();
            } else {
               this.result.append((CharSequence)this.currentBytes);
               this.currentBytes = new StringBuilder();
            }
         }
      } else if (this.currentBytes.length() > 0) {
         byte[] bytes = this.currentBytes.toString().getBytes(StandardCharsets.ISO_8859_1);
         this.currentBytes = new StringBuilder();
         if (this.result == null) {
            this.result = new StringBuilder(new String(bytes, this.currentCharset));
         } else {
            this.result.append(new String(bytes, this.currentCharset));
         }
      }
   }

   public void appendCharacters(StringBuilder value) {
      this.encodeCurrentBytesIfAny();
      this.result.append((CharSequence)value);
   }

   public int length() {
      return this.toString().length();
   }

   public boolean isEmpty() {
      return this.currentBytes.length() == 0 && (this.result == null || this.result.length() == 0);
   }

   @Override
   public String toString() {
      this.encodeCurrentBytesIfAny();
      return this.result == null ? "" : this.result.toString();
   }
}
