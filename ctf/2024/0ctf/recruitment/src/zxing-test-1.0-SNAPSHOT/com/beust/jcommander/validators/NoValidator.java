package com.beust.jcommander.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class NoValidator implements IParameterValidator {
   @Override
   public void validate(String parameterName, String parameterValue) throws ParameterException {
   }
}
