package com.beust.jcommander;

import com.beust.jcommander.internal.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

public class DefaultUsageFormatter implements IUsageFormatter {
   private final JCommander commander;

   public DefaultUsageFormatter(JCommander commander) {
      this.commander = commander;
   }

   @Override
   public final void usage(String commandName) {
      StringBuilder sb = new StringBuilder();
      this.usage(commandName, sb);
      this.commander.getConsole().println(sb.toString());
   }

   @Override
   public final void usage(String commandName, StringBuilder out) {
      this.usage(commandName, out, "");
   }

   @Override
   public final void usage(StringBuilder out) {
      this.usage(out, "");
   }

   @Override
   public final void usage(String commandName, StringBuilder out, String indent) {
      String description = this.getCommandDescription(commandName);
      JCommander jc = this.commander.findCommandByAlias(commandName);
      if (description != null) {
         out.append(indent).append(description);
         out.append("\n");
      }

      jc.getUsageFormatter().usage(out, indent);
   }

   @Override
   public void usage(StringBuilder out, String indent) {
      if (this.commander.getDescriptions() == null) {
         this.commander.createDescriptions();
      }

      boolean hasCommands = !this.commander.getCommands().isEmpty();
      boolean hasOptions = !this.commander.getDescriptions().isEmpty();
      int descriptionIndent = 6;
      int indentCount = indent.length() + 6;
      this.appendMainLine(out, hasOptions, hasCommands, indentCount, indent);
      int longestName = 0;
      List<ParameterDescription> sortedParameters = Lists.newArrayList();

      for (ParameterDescription pd : this.commander.getFields().values()) {
         if (!pd.getParameter().hidden()) {
            sortedParameters.add(pd);
            int length = pd.getNames().length() + 2;
            if (length > longestName) {
               longestName = length;
            }
         }
      }

      sortedParameters.sort(this.commander.getParameterDescriptionComparator());
      this.appendAllParametersDetails(out, indentCount, indent, sortedParameters);
      if (hasCommands) {
         this.appendCommands(out, indentCount, 6, indent);
      }
   }

   public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount, String indent) {
      String programName = this.commander.getProgramDisplayName() != null ? this.commander.getProgramDisplayName() : "<main class>";
      StringBuilder mainLine = new StringBuilder();
      mainLine.append(indent).append("Usage: ").append(programName);
      if (hasOptions) {
         mainLine.append(" [options]");
      }

      if (hasCommands) {
         mainLine.append(indent).append(" [command] [command options]");
      }

      if (this.commander.getMainParameter() != null && this.commander.getMainParameter().getDescription() != null) {
         mainLine.append(" ").append(this.commander.getMainParameter().getDescription().getDescription());
      }

      this.wrapDescription(out, indentCount, mainLine.toString());
      out.append("\n");
   }

   public void appendAllParametersDetails(StringBuilder out, int indentCount, String indent, List<ParameterDescription> sortedParameters) {
      if (sortedParameters.size() > 0) {
         out.append(indent).append("  Options:\n");
      }

      for (ParameterDescription pd : sortedParameters) {
         WrappedParameter parameter = pd.getParameter();
         String description = pd.getDescription();
         boolean hasDescription = !description.isEmpty();
         out.append(indent).append("  ").append(parameter.required() ? "* " : "  ").append(pd.getNames()).append("\n");
         if (hasDescription) {
            this.wrapDescription(out, indentCount, s(indentCount) + description);
         }

         Object def = pd.getDefault();
         if (pd.isDynamicParameter()) {
            String syntax = "Syntax: " + parameter.names()[0] + "key" + parameter.getAssignment() + "value";
            if (hasDescription) {
               out.append(newLineAndIndent(indentCount));
            } else {
               out.append(s(indentCount));
            }

            out.append(syntax);
         }

         if (def != null && !pd.isHelp()) {
            String displayedDef = Strings.isStringEmpty(def.toString()) ? "<empty string>" : def.toString();
            String defaultText = "Default: " + (parameter.password() ? "********" : displayedDef);
            if (hasDescription) {
               out.append(newLineAndIndent(indentCount));
            } else {
               out.append(s(indentCount));
            }

            out.append(defaultText);
         }

         Class<?> type = pd.getParameterized().getType();
         if (type.isEnum()) {
            String valueList = EnumSet.allOf(type).toString();
            String possibleValues = "Possible Values: " + valueList;
            if (!description.contains("Options: " + valueList)) {
               if (hasDescription) {
                  out.append(newLineAndIndent(indentCount));
               } else {
                  out.append(s(indentCount));
               }

               out.append(possibleValues);
            }
         }

         out.append("\n");
      }
   }

   public void appendCommands(StringBuilder out, int indentCount, int descriptionIndent, String indent) {
      boolean hasOnlyHiddenCommands = true;

      for (Entry<JCommander.ProgramName, JCommander> commands : this.commander.getRawCommands().entrySet()) {
         Object arg = commands.getValue().getObjects().get(0);
         Parameters p = arg.getClass().getAnnotation(Parameters.class);
         if (p == null || !p.hidden()) {
            hasOnlyHiddenCommands = false;
         }
      }

      if (!hasOnlyHiddenCommands) {
         out.append(indent + "  Commands:\n");

         for (Entry<JCommander.ProgramName, JCommander> commandsx : this.commander.getRawCommands().entrySet()) {
            Object arg = commandsx.getValue().getObjects().get(0);
            Parameters p = arg.getClass().getAnnotation(Parameters.class);
            if (p == null || !p.hidden()) {
               JCommander.ProgramName progName = commandsx.getKey();
               String dispName = progName.getDisplayName();
               String description = indent + s(4) + dispName + s(6) + this.getCommandDescription(progName.getName());
               this.wrapDescription(out, indentCount + descriptionIndent, description);
               out.append("\n");
               JCommander jc = this.commander.findCommandByAlias(progName.getName());
               jc.getUsageFormatter().usage(out, indent + s(6));
               out.append("\n");
            }
         }
      }
   }

   @Override
   public String getCommandDescription(String commandName) {
      JCommander jc = this.commander.findCommandByAlias(commandName);
      if (jc == null) {
         throw new ParameterException("Asking description for unknown command: " + commandName);
      } else {
         Object arg = jc.getObjects().get(0);
         Parameters p = arg.getClass().getAnnotation(Parameters.class);
         String result = null;
         if (p != null) {
            result = p.commandDescription();
            String bundleName = p.resourceBundle();
            java.util.ResourceBundle bundle;
            if (!bundleName.isEmpty()) {
               bundle = java.util.ResourceBundle.getBundle(bundleName, Locale.getDefault());
            } else {
               bundle = this.commander.getBundle();
            }

            if (bundle != null) {
               String descriptionKey = p.commandDescriptionKey();
               if (!descriptionKey.isEmpty()) {
                  result = getI18nString(bundle, descriptionKey, p.commandDescription());
               }
            }
         }

         return result;
      }
   }

   public void wrapDescription(StringBuilder out, int indent, int currentLineIndent, String description) {
      int max = this.commander.getColumnSize();
      String[] words = description.split(" ");
      int current = currentLineIndent;

      for (int i = 0; i < words.length; i++) {
         String word = words[i];
         if (word.length() <= max && current + 1 + word.length() > max) {
            out.append("\n").append(s(indent)).append(word).append(" ");
            current = indent + word.length() + 1;
         } else {
            out.append(word);
            current += word.length();
            if (i != words.length - 1) {
               out.append(" ");
               current++;
            }
         }
      }
   }

   public void wrapDescription(StringBuilder out, int indent, String description) {
      this.wrapDescription(out, indent, 0, description);
   }

   public static String getI18nString(java.util.ResourceBundle bundle, String key, String def) {
      String s = bundle != null ? bundle.getString(key) : null;
      return s != null ? s : def;
   }

   public static String s(int count) {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < count; i++) {
         result.append(" ");
      }

      return result.toString();
   }

   private static String newLineAndIndent(int indent) {
      return "\n" + s(indent);
   }
}
