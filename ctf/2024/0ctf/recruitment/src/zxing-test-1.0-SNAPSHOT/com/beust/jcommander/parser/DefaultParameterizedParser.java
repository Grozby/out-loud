package com.beust.jcommander.parser;

import com.beust.jcommander.IParameterizedParser;
import com.beust.jcommander.Parameterized;
import java.util.List;

public class DefaultParameterizedParser implements IParameterizedParser {
   @Override
   public List<Parameterized> parseArg(Object annotatedObj) {
      return Parameterized.parseArg(annotatedObj);
   }
}
