package com.alibaba.fastjson2.internal.asm;

public class ByteVector {
   byte[] data;
   int length;

   public ByteVector(int initialCapacity) {
      this.data = new byte[initialCapacity];
   }

   public ByteVector putByte(int byteValue) {
      int currentLength = this.length;
      if (currentLength + 1 > this.data.length) {
         this.enlarge(1);
      }

      this.data[currentLength++] = (byte)byteValue;
      this.length = currentLength;
      return this;
   }

   final void put11(int byteValue1, int byteValue2) {
      int currentLength = this.length;
      if (currentLength + 2 > this.data.length) {
         this.enlarge(2);
      }

      byte[] currentData = this.data;
      currentData[currentLength++] = (byte)byteValue1;
      currentData[currentLength++] = (byte)byteValue2;
      this.length = currentLength;
   }

   public ByteVector putShort(int shortValue) {
      int currentLength = this.length;
      if (currentLength + 2 > this.data.length) {
         this.enlarge(2);
      }

      byte[] currentData = this.data;
      currentData[currentLength++] = (byte)(shortValue >>> 8);
      currentData[currentLength++] = (byte)shortValue;
      this.length = currentLength;
      return this;
   }

   final ByteVector put12(int byteValue, int shortValue) {
      int currentLength = this.length;
      if (currentLength + 3 > this.data.length) {
         this.enlarge(3);
      }

      byte[] currentData = this.data;
      currentData[currentLength++] = (byte)byteValue;
      currentData[currentLength++] = (byte)(shortValue >>> 8);
      currentData[currentLength++] = (byte)shortValue;
      this.length = currentLength;
      return this;
   }

   public ByteVector putInt(int intValue) {
      int currentLength = this.length;
      if (currentLength + 4 > this.data.length) {
         this.enlarge(4);
      }

      byte[] currentData = this.data;
      currentData[currentLength++] = (byte)(intValue >>> 24);
      currentData[currentLength++] = (byte)(intValue >>> 16);
      currentData[currentLength++] = (byte)(intValue >>> 8);
      currentData[currentLength++] = (byte)intValue;
      this.length = currentLength;
      return this;
   }

   final void put122(int byteValue, int shortValue1, int shortValue2) {
      int currentLength = this.length;
      if (currentLength + 5 > this.data.length) {
         this.enlarge(5);
      }

      byte[] currentData = this.data;
      currentData[currentLength++] = (byte)byteValue;
      currentData[currentLength++] = (byte)(shortValue1 >>> 8);
      currentData[currentLength++] = (byte)shortValue1;
      currentData[currentLength++] = (byte)(shortValue2 >>> 8);
      currentData[currentLength++] = (byte)shortValue2;
      this.length = currentLength;
   }

   public void putLong(long longValue) {
      int currentLength = this.length;
      if (currentLength + 8 > this.data.length) {
         this.enlarge(8);
      }

      byte[] currentData = this.data;
      int intValue = (int)(longValue >>> 32);
      currentData[currentLength++] = (byte)(intValue >>> 24);
      currentData[currentLength++] = (byte)(intValue >>> 16);
      currentData[currentLength++] = (byte)(intValue >>> 8);
      currentData[currentLength++] = (byte)intValue;
      intValue = (int)longValue;
      currentData[currentLength++] = (byte)(intValue >>> 24);
      currentData[currentLength++] = (byte)(intValue >>> 16);
      currentData[currentLength++] = (byte)(intValue >>> 8);
      currentData[currentLength++] = (byte)intValue;
      this.length = currentLength;
   }

   public void putUTF8(String stringValue) {
      int charLength = stringValue.length();
      if (charLength > 65535) {
         throw new IllegalArgumentException("UTF8 string too large");
      } else {
         int currentLength = this.length;
         if (currentLength + 2 + charLength > this.data.length) {
            this.enlarge(2 + charLength);
         }

         byte[] currentData = this.data;
         currentData[currentLength++] = (byte)(charLength >>> 8);
         currentData[currentLength++] = (byte)charLength;

         for (int i = 0; i < charLength; i++) {
            char charValue = stringValue.charAt(i);
            if (charValue < 1 || charValue > 127) {
               this.length = currentLength;
               this.encodeUtf8(stringValue, i);
               return;
            }

            currentData[currentLength++] = (byte)charValue;
         }

         this.length = currentLength;
      }
   }

   final void encodeUtf8(String stringValue, int offset) {
      int charLength = stringValue.length();
      int byteLength = offset;

      for (int i = offset; i < charLength; i++) {
         char charValue = stringValue.charAt(i);
         if (charValue >= 1 && charValue <= 127) {
            byteLength++;
         } else if (charValue <= 2047) {
            byteLength += 2;
         } else {
            byteLength += 3;
         }
      }

      if (byteLength > 65535) {
         throw new IllegalArgumentException("UTF8 string too large");
      } else {
         int byteLengthOffset = this.length - offset - 2;
         if (byteLengthOffset >= 0) {
            this.data[byteLengthOffset] = (byte)(byteLength >>> 8);
            this.data[byteLengthOffset + 1] = (byte)byteLength;
         }

         if (this.length + byteLength - offset > this.data.length) {
            this.enlarge(byteLength - offset);
         }

         int currentLength = this.length;

         for (int ix = offset; ix < charLength; ix++) {
            char charValue = stringValue.charAt(ix);
            if (charValue >= 1 && charValue <= 127) {
               this.data[currentLength++] = (byte)charValue;
            } else if (charValue <= 2047) {
               this.data[currentLength++] = (byte)(192 | charValue >> 6 & 31);
               this.data[currentLength++] = (byte)(128 | charValue & '?');
            } else {
               this.data[currentLength++] = (byte)(224 | charValue >> '\f' & 15);
               this.data[currentLength++] = (byte)(128 | charValue >> 6 & 63);
               this.data[currentLength++] = (byte)(128 | charValue & '?');
            }
         }

         this.length = currentLength;
      }
   }

   public void putByteArray(byte[] byteArrayValue, int byteOffset, int byteLength) {
      if (this.length + byteLength > this.data.length) {
         this.enlarge(byteLength);
      }

      if (byteArrayValue != null) {
         System.arraycopy(byteArrayValue, byteOffset, this.data, this.length, byteLength);
      }

      this.length += byteLength;
   }

   private void enlarge(int size) {
      int doubleCapacity = 2 * this.data.length;
      int minimalCapacity = this.length + size;
      byte[] newData = new byte[Math.max(doubleCapacity, minimalCapacity)];
      System.arraycopy(this.data, 0, newData, 0, this.length);
      this.data = newData;
   }
}
