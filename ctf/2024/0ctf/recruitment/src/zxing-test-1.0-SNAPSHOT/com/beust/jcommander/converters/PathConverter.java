package com.beust.jcommander.converters;

import com.beust.jcommander.ParameterException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathConverter extends BaseConverter<Path> {
   public PathConverter(String optionName) {
      super(optionName);
   }

   public Path convert(String value) {
      try {
         return Paths.get(value);
      } catch (InvalidPathException var4) {
         String encoded = escapeUnprintable(value);
         throw new ParameterException(this.getErrorString(encoded, "a path"));
      }
   }

   private static String escapeUnprintable(String value) {
      StringBuilder bldr = new StringBuilder();

      for (char c : value.toCharArray()) {
         if (c < ' ') {
            bldr.append("\\u").append(String.format("%04X", Integer.valueOf(c)));
         } else {
            bldr.append(c);
         }
      }

      return bldr.toString();
   }
}
