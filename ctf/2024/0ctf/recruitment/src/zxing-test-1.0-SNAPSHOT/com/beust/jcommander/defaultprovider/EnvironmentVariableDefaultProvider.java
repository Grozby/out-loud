package com.beust.jcommander.defaultprovider;

import com.beust.jcommander.IDefaultProvider;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnvironmentVariableDefaultProvider implements IDefaultProvider {
   private static final String DEFAULT_VARIABLE_NAME = "JCOMMANDER_OPTS";
   private static final String DEFAULT_PREFIXES_PATTERN = "-/";
   private final String environmentVariableValue;
   private final String optionPrefixesPattern;

   public EnvironmentVariableDefaultProvider() {
      this("JCOMMANDER_OPTS", "-/");
   }

   public EnvironmentVariableDefaultProvider(String environmentVariableName, String optionPrefixes) {
      this(Objects.requireNonNull(environmentVariableName), Objects.requireNonNull(optionPrefixes), System::getenv);
   }

   EnvironmentVariableDefaultProvider(String environmentVariableName, String optionPrefixes, Function<String, String> resolver) {
      this.environmentVariableValue = resolver.apply(environmentVariableName);
      this.optionPrefixesPattern = Objects.requireNonNull(optionPrefixes);
   }

   @Override
   public final String getDefaultValueFor(String optionName) {
      if (this.environmentVariableValue == null) {
         return null;
      } else {
         Matcher matcher = Pattern.compile(
               "(?:(?:.*\\s+)|(?:^))("
                  + Pattern.quote(optionName)
                  + ")\\s*((?:'[^']*(?='))|(?:\"[^\"]*(?=\"))|(?:[^"
                  + this.optionPrefixesPattern
                  + "\\s]+))?.*"
            )
            .matcher(this.environmentVariableValue);
         if (!matcher.matches()) {
            return null;
         } else {
            String value = matcher.group(2);
            if (value == null) {
               return "true";
            } else {
               char firstCharacter = value.charAt(0);
               if (firstCharacter == '\'' || firstCharacter == '"') {
                  value = value.substring(1);
               }

               return value;
            }
         }
      }
   }
}
