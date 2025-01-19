package com.beust.jcommander;

import java.util.EnumSet;
import java.util.List;

public class UnixStyleUsageFormatter extends DefaultUsageFormatter {
   public UnixStyleUsageFormatter(JCommander commander) {
      super(commander);
   }

   @Override
   public void appendAllParametersDetails(StringBuilder out, int indentCount, String indent, List<ParameterDescription> sortedParameters) {
      if (sortedParameters.size() > 0) {
         out.append(indent).append("  Options:\n");
      }

      int prefixIndent = 0;

      for (ParameterDescription pd : sortedParameters) {
         WrappedParameter parameter = pd.getParameter();
         String prefix = (parameter.required() ? "* " : "  ") + pd.getNames();
         if (prefix.length() > prefixIndent) {
            prefixIndent = prefix.length();
         }
      }

      for (ParameterDescription pdx : sortedParameters) {
         WrappedParameter parameter = pdx.getParameter();
         String prefix = (parameter.required() ? "* " : "  ") + pdx.getNames();
         out.append(indent).append("  ").append(prefix).append(s(prefixIndent - prefix.length())).append(" ");
         int initialLinePrefixLength = indent.length() + prefixIndent + 3;
         String description = pdx.getDescription();
         Object def = pdx.getDefault();
         if (pdx.isDynamicParameter()) {
            String syntax = "(syntax: " + parameter.names()[0] + "key" + parameter.getAssignment() + "value)";
            description = description + (description.length() == 0 ? "" : " ") + syntax;
         }

         if (def != null && !pdx.isHelp()) {
            String displayedDef = Strings.isStringEmpty(def.toString()) ? "<empty string>" : def.toString();
            String defaultText = "(default: " + (parameter.password() ? "********" : displayedDef) + ")";
            description = description + (description.length() == 0 ? "" : " ") + defaultText;
         }

         Class<?> type = pdx.getParameterized().getType();
         if (type.isEnum()) {
            String valueList = EnumSet.allOf(type).toString();
            if (!description.contains("Options: " + valueList)) {
               String possibleValues = "(values: " + valueList + ")";
               description = description + (description.length() == 0 ? "" : " ") + possibleValues;
            }
         }

         this.wrapDescription(out, indentCount + prefixIndent - 3, initialLinePrefixLength, description);
         out.append("\n");
      }
   }
}
