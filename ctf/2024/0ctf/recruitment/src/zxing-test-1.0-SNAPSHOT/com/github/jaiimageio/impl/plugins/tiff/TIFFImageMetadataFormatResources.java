package com.github.jaiimageio.impl.plugins.tiff;

import java.util.ListResourceBundle;

public class TIFFImageMetadataFormatResources extends ListResourceBundle {
   private static final Object[][] contents = new Object[][]{
      {"TIFFIFD", "An IFD (directory) containing fields"},
      {"TIFFIFD/parentTagNumber", "The tag number of the field pointing to this IFD"},
      {"TIFFIFD/parentTagName", "A mnemonic name for the field pointing to this IFD, if known"},
      {"TIFFField", "A field containing data"},
      {"TIFFField/number", "The tag number asociated with the field"},
      {"TIFFField/name", "A mnemonic name associated with the field, if known"},
      {"TIFFUndefined", "Uninterpreted byte data"},
      {"TIFFUndefined/value", "A list of comma-separated byte values"},
      {"TIFFBytes", "A sequence of TIFFByte nodes"},
      {"TIFFByte", "An integral value between 0 and 255"},
      {"TIFFByte/value", "The value"},
      {"TIFFByte/description", "A description, if available"},
      {"TIFFAsciis", "A sequence of TIFFAscii nodes"},
      {"TIFFAscii", "A String value"},
      {"TIFFAscii/value", "The value"},
      {"TIFFShorts", "A sequence of TIFFShort nodes"},
      {"TIFFShort", "An integral value between 0 and 65535"},
      {"TIFFShort/value", "The value"},
      {"TIFFShort/description", "A description, if available"},
      {"TIFFSShorts", "A sequence of TIFFSShort nodes"},
      {"TIFFSShort", "An integral value between -32768 and 32767"},
      {"TIFFSShort/value", "The value"},
      {"TIFFSShort/description", "A description, if available"},
      {"TIFFLongs", "A sequence of TIFFLong nodes"},
      {"TIFFLong", "An integral value between 0 and 4294967295"},
      {"TIFFLong/value", "The value"},
      {"TIFFLong/description", "A description, if available"},
      {"TIFFSLongs", "A sequence of TIFFSLong nodes"},
      {"TIFFSLong", "An integral value between -2147483648 and 2147483647"},
      {"TIFFSLong/value", "The value"},
      {"TIFFSLong/description", "A description, if available"},
      {"TIFFRationals", "A sequence of TIFFRational nodes"},
      {"TIFFRational", "A rational value consisting of an unsigned numerator and denominator"},
      {"TIFFRational/value", "The numerator and denominator, separated by a slash"},
      {"TIFFSRationals", "A sequence of TIFFSRational nodes"},
      {"TIFFSRational", "A rational value consisting of a signed numerator and denominator"},
      {"TIFFSRational/value", "The numerator and denominator, separated by a slash"},
      {"TIFFFloats", "A sequence of TIFFFloat nodes"},
      {"TIFFFloat", "A single-precision floating-point value"},
      {"TIFFFloat/value", "The value"},
      {"TIFFDoubles", "A sequence of TIFFDouble nodes"},
      {"TIFFDouble", "A double-precision floating-point value"},
      {"TIFFDouble/value", "The value"}
   };

   @Override
   public Object[][] getContents() {
      return contents;
   }
}
