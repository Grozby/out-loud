package com.beust.jcommander.converters;

import java.util.Arrays;
import java.util.List;

public class CommaParameterSplitter implements IParameterSplitter {
   @Override
   public List<String> split(String value) {
      return Arrays.asList(value.split(","));
   }
}
