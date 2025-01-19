package com.beust.jcommander;

import com.beust.jcommander.converters.DefaultListConverter;
import com.beust.jcommander.converters.EnumConverter;
import com.beust.jcommander.converters.IParameterSplitter;
import com.beust.jcommander.converters.NoConverter;
import com.beust.jcommander.converters.StringConverter;
import com.beust.jcommander.internal.Console;
import com.beust.jcommander.internal.DefaultConsole;
import com.beust.jcommander.internal.DefaultConverterFactory;
import com.beust.jcommander.internal.JDK6Console;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Nullable;
import com.beust.jcommander.parser.DefaultParameterizedParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class JCommander {
   public static final String DEBUG_PROPERTY = "jcommander.debug";
   protected IParameterizedParser parameterizedParser = new DefaultParameterizedParser();
   private Map<FuzzyMap.IKey, ParameterDescription> descriptions;
   private List<Object> objects = Lists.newArrayList();
   private IUsageFormatter usageFormatter = new DefaultUsageFormatter(this);
   private JCommander.MainParameter mainParameter = null;
   private Map<Parameterized, ParameterDescription> requiredFields = Maps.newHashMap();
   private Map<Parameterized, ParameterDescription> fields = Maps.newHashMap();
   private Map<JCommander.ProgramName, JCommander> commands = Maps.newLinkedHashMap();
   private Map<FuzzyMap.IKey, JCommander.ProgramName> aliasMap = Maps.newLinkedHashMap();
   private String parsedCommand;
   private String parsedAlias;
   private JCommander.ProgramName programName;
   private boolean helpWasSpecified;
   private List<String> unknownArgs = Lists.newArrayList();
   private Console console;
   private final JCommander.Options options;
   private final IVariableArity DEFAULT_VARIABLE_ARITY = new JCommander.DefaultVariableArity();

   private JCommander(JCommander.Options options) {
      if (options == null) {
         throw new NullPointerException("options");
      } else {
         this.options = options;
         if (options.converterInstanceFactories.isEmpty()) {
            this.addConverterFactory(new DefaultConverterFactory());
         }
      }
   }

   public JCommander() {
      this(new JCommander.Options());
   }

   public JCommander(Object object) {
      this(object, (java.util.ResourceBundle)null);
   }

   public JCommander(Object object, @Nullable java.util.ResourceBundle bundle) {
      this(object, bundle, (String[])null);
   }

   public JCommander(Object object, @Nullable java.util.ResourceBundle bundle, String... args) {
      this();
      this.addObject(object);
      if (bundle != null) {
         this.setDescriptionsBundle(bundle);
      }

      this.createDescriptions();
      if (args != null) {
         this.parse(args);
      }
   }

   @Deprecated
   public JCommander(Object object, String... args) {
      this(object);
      this.parse(args);
   }

   public void setParameterizedParser(IParameterizedParser parameterizedParser) {
      this.parameterizedParser = parameterizedParser;
   }

   public void setExpandAtSign(boolean expandAtSign) {
      this.options.expandAtSign = expandAtSign;
   }

   public void setConsole(Console console) {
      this.console = console;
   }

   public synchronized Console getConsole() {
      if (this.console == null) {
         try {
            Method consoleMethod = System.class.getDeclaredMethod("console");
            Object console = consoleMethod.invoke(null);
            this.console = new JDK6Console(console);
         } catch (Throwable var3) {
            this.console = new DefaultConsole();
         }
      }

      return this.console;
   }

   public final void addObject(Object object) {
      if (object instanceof Iterable) {
         for (Object o : (Iterable)object) {
            this.objects.add(o);
         }
      } else if (object.getClass().isArray()) {
         for (Object o : (Object[])object) {
            this.objects.add(o);
         }
      } else {
         this.objects.add(object);
      }
   }

   public final void setDescriptionsBundle(java.util.ResourceBundle bundle) {
      this.options.bundle = bundle;
   }

   public void parse(String... args) {
      try {
         this.parse(true, args);
      } catch (ParameterException var3) {
         var3.setJCommander(this);
         throw var3;
      }
   }

   public void parseWithoutValidation(String... args) {
      this.parse(false, args);
   }

   private void parse(boolean validate, String... args) {
      StringBuilder sb = new StringBuilder("Parsing \"");
      sb.append(Strings.join(" ", args)).append("\"\n  with:").append(Strings.join(" ", this.objects.toArray()));
      this.p(sb.toString());
      if (this.descriptions == null) {
         this.createDescriptions();
      }

      this.initializeDefaultValues();
      this.parseValues(this.expandArgs(args), validate);
      if (validate) {
         this.validateOptions();
      }
   }

   private void initializeDefaultValues() {
      if (this.options.defaultProvider != null) {
         for (ParameterDescription pd : this.descriptions.values()) {
            this.initializeDefaultValue(pd);
         }

         for (Entry<JCommander.ProgramName, JCommander> entry : this.commands.entrySet()) {
            entry.getValue().initializeDefaultValues();
         }
      }
   }

   private void validateOptions() {
      if (!this.helpWasSpecified) {
         if (this.requiredFields.isEmpty()) {
            if (this.mainParameter != null && this.mainParameter.description != null) {
               ParameterDescription mainParameterDescription = this.mainParameter.description;
               if (mainParameterDescription.getParameter().required() && !mainParameterDescription.isAssigned()) {
                  throw new ParameterException("Main parameters are required (\"" + mainParameterDescription.getDescription() + "\")");
               }

               int arity = mainParameterDescription.getParameter().arity();
               if (arity != -1) {
                  Object value = mainParameterDescription.getParameterized().get(this.mainParameter.object);
                  if (List.class.isAssignableFrom(value.getClass())) {
                     int size = ((List)value).size();
                     if (size != arity) {
                        throw new ParameterException("There should be exactly " + arity + " main parameters but " + size + " were found");
                     }
                  }
               }
            }
         } else {
            List<String> missingFields = new ArrayList<>();

            for (ParameterDescription pd : this.requiredFields.values()) {
               missingFields.add("[" + Strings.join(" | ", pd.getParameter().names()) + "]");
            }

            String message = Strings.join(", ", missingFields);
            throw new ParameterException("The following " + pluralize(this.requiredFields.size(), "option is required: ", "options are required: ") + message);
         }
      }
   }

   private static String pluralize(int quantity, String singular, String plural) {
      return quantity == 1 ? singular : plural;
   }

   private String[] expandArgs(String[] originalArgv) {
      List<String> vResult1 = Lists.newArrayList();

      for (String arg : originalArgv) {
         if (arg.startsWith("@") && this.options.expandAtSign) {
            String fileName = arg.substring(1);
            vResult1.addAll(this.readFile(fileName));
         } else {
            List<String> expanded = this.expandDynamicArg(arg);
            vResult1.addAll(expanded);
         }
      }

      List<String> vResult2 = Lists.newArrayList();

      for (String argx : vResult1) {
         if (this.isOption(argx)) {
            String sep = this.getSeparatorFor(argx);
            if (!" ".equals(sep)) {
               String[] sp = argx.split("[" + sep + "]", 2);

               for (String ssp : sp) {
                  vResult2.add(ssp);
               }
            } else {
               vResult2.add(argx);
            }
         } else {
            vResult2.add(argx);
         }
      }

      return vResult2.toArray(new String[vResult2.size()]);
   }

   private List<String> expandDynamicArg(String arg) {
      for (ParameterDescription pd : this.descriptions.values()) {
         if (pd.isDynamicParameter()) {
            for (String name : pd.getParameter().names()) {
               if (arg.startsWith(name) && !arg.equals(name)) {
                  return Arrays.asList(name, arg.substring(name.length()));
               }
            }
         }
      }

      return Arrays.asList(arg);
   }

   private boolean matchArg(String arg, FuzzyMap.IKey key) {
      String kn = this.options.caseSensitiveOptions ? key.getName() : key.getName().toLowerCase();
      if (this.options.allowAbbreviatedOptions) {
         if (kn.startsWith(arg)) {
            return true;
         }
      } else {
         ParameterDescription pd = this.descriptions.get(key);
         if (pd != null) {
            String separator = this.getSeparatorFor(arg);
            if (!" ".equals(separator)) {
               if (arg.startsWith(kn)) {
                  return true;
               }
            } else if (kn.equals(arg)) {
               return true;
            }
         } else if (kn.equals(arg)) {
            return true;
         }
      }

      return false;
   }

   private boolean isOption(String passedArg) {
      if (this.options.acceptUnknownOptions) {
         return true;
      } else {
         String arg = this.options.caseSensitiveOptions ? passedArg : passedArg.toLowerCase();

         for (FuzzyMap.IKey key : this.descriptions.keySet()) {
            if (this.matchArg(arg, key)) {
               return true;
            }
         }

         for (FuzzyMap.IKey keyx : this.commands.keySet()) {
            if (this.matchArg(arg, keyx)) {
               return true;
            }
         }

         return false;
      }
   }

   private ParameterDescription getPrefixDescriptionFor(String arg) {
      for (Entry<FuzzyMap.IKey, ParameterDescription> es : this.descriptions.entrySet()) {
         if (Strings.startsWith(arg, es.getKey().getName(), this.options.caseSensitiveOptions)) {
            return es.getValue();
         }
      }

      return null;
   }

   private ParameterDescription getDescriptionFor(String arg) {
      return this.getPrefixDescriptionFor(arg);
   }

   private String getSeparatorFor(String arg) {
      ParameterDescription pd = this.getDescriptionFor(arg);
      if (pd != null) {
         Parameters p = pd.getObject().getClass().getAnnotation(Parameters.class);
         if (p != null) {
            return p.separators();
         }
      }

      return " ";
   }

   private List<String> readFile(String fileName) {
      List<String> result = Lists.newArrayList();

      try {
         BufferedReader bufRead = Files.newBufferedReader(Paths.get(fileName), this.options.atFileCharset);

         String line;
         try {
            while ((line = bufRead.readLine()) != null) {
               if (line.length() > 0 && !line.trim().startsWith("#")) {
                  result.add(line);
               }
            }
         } catch (Throwable var7) {
            if (bufRead != null) {
               try {
                  bufRead.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (bufRead != null) {
            bufRead.close();
         }

         return result;
      } catch (IOException var8) {
         throw new ParameterException("Could not read file " + fileName + ": " + var8);
      }
   }

   private static String trim(String string) {
      String result = string.trim();
      if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1) {
         result = result.substring(1, result.length() - 1);
      }

      return result;
   }

   public void createDescriptions() {
      this.descriptions = Maps.newHashMap();

      for (Object object : this.objects) {
         this.addDescription(object);
      }
   }

   private void addDescription(Object object) {
      Class<?> cls = object.getClass();

      for (Parameterized parameterized : this.parameterizedParser.parseArg(object)) {
         WrappedParameter wp = parameterized.getWrappedParameter();
         if (wp != null && wp.getParameter() != null) {
            Parameter annotation = wp.getParameter();
            Parameter p = annotation;
            if (annotation.names().length == 0) {
               this.p("Found main parameter:" + parameterized);
               if (this.mainParameter != null) {
                  throw new ParameterException("Only one @Parameter with no names attribute is allowed, found:" + this.mainParameter + " and " + parameterized);
               }

               this.mainParameter = new JCommander.MainParameter();
               this.mainParameter.parameterized = parameterized;
               this.mainParameter.object = object;
               this.mainParameter.annotation = annotation;
               this.mainParameter.description = new ParameterDescription(object, annotation, parameterized, this.options.bundle, this);
            } else {
               ParameterDescription pd = new ParameterDescription(object, annotation, parameterized, this.options.bundle, this);

               for (String name : annotation.names()) {
                  if (this.descriptions.containsKey(new StringKey(name))) {
                     throw new ParameterException("Found the option " + name + " multiple times");
                  }

                  this.p("Adding description for " + name);
                  this.fields.put(parameterized, pd);
                  this.descriptions.put(new StringKey(name), pd);
                  if (p.required()) {
                     this.requiredFields.put(parameterized, pd);
                  }
               }
            }
         } else if (parameterized.getDelegateAnnotation() != null) {
            Object delegateObject = parameterized.get(object);
            if (delegateObject == null) {
               throw new ParameterException("Delegate field '" + parameterized.getName() + "' cannot be null.");
            }

            this.addDescription(delegateObject);
         } else if (wp != null && wp.getDynamicParameter() != null) {
            DynamicParameter dp = wp.getDynamicParameter();

            for (String name : dp.names()) {
               if (this.descriptions.containsKey(name)) {
                  throw new ParameterException("Found the option " + name + " multiple times");
               }

               this.p("Adding description for " + name);
               ParameterDescription pd = new ParameterDescription(object, dp, parameterized, this.options.bundle, this);
               this.fields.put(parameterized, pd);
               this.descriptions.put(new StringKey(name), pd);
               if (dp.required()) {
                  this.requiredFields.put(parameterized, pd);
               }
            }
         }
      }
   }

   private void initializeDefaultValue(ParameterDescription pd) {
      for (String optionName : pd.getParameter().names()) {
         String def = this.options.defaultProvider.getDefaultValueFor(optionName);
         if (def != null) {
            this.p("Initializing " + optionName + " with default value:" + def);
            pd.addValue(def, true);
            this.requiredFields.remove(pd.getParameterized());
            return;
         }
      }
   }

   private void parseValues(String[] args, boolean validate) {
      boolean commandParsed = false;
      int i = 0;
      boolean isDashDash = false;

      while (i < args.length && !commandParsed) {
         String arg = args[i];
         String a = trim(arg);
         args[i] = a;
         this.p("Parsing arg: " + a);
         JCommander jc = this.findCommandByAlias(arg);
         int increment = 1;
         if (!isDashDash && !"--".equals(a) && this.isOption(a) && jc == null) {
            ParameterDescription pd = this.findParameterDescription(a);
            if (pd != null) {
               if (pd.getParameter().password()) {
                  increment = this.processPassword(args, i, pd, validate);
               } else if (pd.getParameter().variableArity()) {
                  increment = this.processVariableArity(args, i, pd, validate);
               } else {
                  Class<?> fieldType = pd.getParameterized().getType();
                  if (pd.getParameter().arity() == -1 && this.isBooleanType(fieldType)) {
                     this.handleBooleanOption(pd, fieldType);
                  } else {
                     increment = this.processFixedArity(args, i, pd, validate, fieldType);
                  }

                  if (pd.isHelp()) {
                     this.helpWasSpecified = true;
                  }
               }
            } else {
               if (!this.options.acceptUnknownOptions) {
                  throw new ParameterException("Unknown option: " + arg);
               }

               this.unknownArgs.add(arg);
               i++;

               while (i < args.length && !this.isOption(args[i])) {
                  this.unknownArgs.add(args[i++]);
               }

               increment = 0;
            }
         } else if ("--".equals(arg) && !isDashDash) {
            isDashDash = true;
         } else if (this.commands.isEmpty()) {
            this.initMainParameterValue(arg);
            String value = a;
            Object convertedValue = a;
            if (this.mainParameter.annotation.converter() != null && this.mainParameter.annotation.converter() != NoConverter.class) {
               convertedValue = this.convertValue(this.mainParameter.parameterized, this.mainParameter.parameterized.getType(), null, a);
            }

            Type genericType = this.mainParameter.parameterized.getGenericType();
            if (genericType instanceof ParameterizedType) {
               ParameterizedType p = (ParameterizedType)genericType;
               Type cls = p.getActualTypeArguments()[0];
               if (cls instanceof Class) {
                  convertedValue = this.convertValue(this.mainParameter.parameterized, (Class)cls, null, a);
               }
            }

            for (Class<? extends IParameterValidator> validator : this.mainParameter.annotation.validateWith()) {
               this.mainParameter.description.validateParameter(validator, "Default", value);
            }

            this.mainParameter.description.setAssigned(true);
            this.mainParameter.addValue(convertedValue);
         } else {
            if (jc == null && validate) {
               throw new MissingCommandException("Expected a command, got " + arg, arg);
            }

            if (jc != null) {
               this.parsedCommand = jc.programName.name;
               this.parsedAlias = arg;
               jc.parse(validate, this.subArray(args, i + 1));
               commandParsed = true;
            }
         }

         i += increment;
      }

      for (ParameterDescription parameterDescription : this.descriptions.values()) {
         if (parameterDescription.isAssigned()) {
            this.fields.get(parameterDescription.getParameterized()).setAssigned(true);
         }
      }
   }

   private boolean isBooleanType(Class<?> fieldType) {
      return Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType);
   }

   private void handleBooleanOption(ParameterDescription pd, Class<?> fieldType) {
      Boolean value = (Boolean)pd.getParameterized().get(pd.getObject());
      if (value != null) {
         pd.addValue(value ? "false" : "true");
      } else if (!fieldType.isPrimitive()) {
         pd.addValue("true");
      }

      this.requiredFields.remove(pd.getParameterized());
   }

   private final int determineArity(String[] args, int index, ParameterDescription pd, IVariableArity va) {
      List<String> currentArgs = Lists.newArrayList();

      for (int j = index + 1; j < args.length; j++) {
         currentArgs.add(args[j]);
      }

      return va.processVariableArity(pd.getParameter().names()[0], currentArgs.toArray(new String[0]));
   }

   private int processPassword(String[] args, int index, ParameterDescription pd, boolean validate) {
      int passwordArity = this.determineArity(args, index, pd, this.DEFAULT_VARIABLE_ARITY);
      if (passwordArity == 0) {
         char[] password = this.readPassword(pd.getDescription(), pd.getParameter().echoInput());
         pd.addValue(new String(password));
         this.requiredFields.remove(pd.getParameterized());
         return 1;
      } else if (passwordArity == 1) {
         return this.processFixedArity(args, index, pd, validate, List.class, 1);
      } else {
         throw new ParameterException("Password parameter must have at most 1 argument.");
      }
   }

   private int processVariableArity(String[] args, int index, ParameterDescription pd, boolean validate) {
      Object arg = pd.getObject();
      IVariableArity va;
      if (!(arg instanceof IVariableArity)) {
         va = this.DEFAULT_VARIABLE_ARITY;
      } else {
         va = (IVariableArity)arg;
      }

      int arity = this.determineArity(args, index, pd, va);
      return this.processFixedArity(args, index, pd, validate, List.class, arity);
   }

   private int processFixedArity(String[] args, int index, ParameterDescription pd, boolean validate, Class<?> fieldType) {
      int arity = pd.getParameter().arity();
      int n = arity != -1 ? arity : 1;
      return this.processFixedArity(args, index, pd, validate, fieldType, n);
   }

   private int processFixedArity(String[] args, int originalIndex, ParameterDescription pd, boolean validate, Class<?> fieldType, int arity) {
      int index = originalIndex;
      String arg = args[originalIndex];
      if (arity == 0 && this.isBooleanType(fieldType)) {
         this.handleBooleanOption(pd, fieldType);
      } else {
         if (arity == 0) {
            throw new ParameterException("Expected a value after parameter " + arg);
         }

         if (originalIndex >= args.length - 1) {
            throw new ParameterException("Expected a value after parameter " + arg);
         }

         int offset = "--".equals(args[originalIndex + 1]) ? 1 : 0;
         Object finalValue = null;
         if (originalIndex + arity >= args.length) {
            throw new ParameterException("Expected " + arity + " values after " + arg);
         }

         for (int j = 1; j <= arity; j++) {
            String value = args[index + j + offset];
            finalValue = pd.addValue(arg, value, false, validate, j - 1);
            this.requiredFields.remove(pd.getParameterized());
         }

         if (finalValue != null && validate) {
            pd.validateValueParameter(arg, finalValue);
         }

         index += arity + offset;
      }

      return arity + 1;
   }

   private char[] readPassword(String description, boolean echoInput) {
      this.getConsole().print(description + ": ");
      return this.getConsole().readPassword(echoInput);
   }

   private String[] subArray(String[] args, int index) {
      int l = args.length - index;
      String[] result = new String[l];
      System.arraycopy(args, index, result, 0, l);
      return result;
   }

   private void initMainParameterValue(String arg) {
      if (this.mainParameter == null) {
         throw new ParameterException("Was passed main parameter '" + arg + "' but no main parameter was defined in your arg class");
      } else {
         Object object = this.mainParameter.parameterized.get(this.mainParameter.object);
         Class<?> type = this.mainParameter.parameterized.getType();
         if (List.class.isAssignableFrom(type)) {
            List result;
            if (object == null) {
               result = Lists.newArrayList();
            } else {
               result = (List)object;
            }

            if (this.mainParameter.firstTimeMainParameter) {
               result.clear();
               this.mainParameter.firstTimeMainParameter = false;
            }

            this.mainParameter.multipleValue = result;
            this.mainParameter.parameterized.set(this.mainParameter.object, result);
         }
      }
   }

   public String getMainParameterDescription() {
      if (this.descriptions == null) {
         this.createDescriptions();
      }

      return this.mainParameter.annotation != null ? this.mainParameter.annotation.description() : null;
   }

   public void setProgramName(String name) {
      this.setProgramName(name);
   }

   public String getProgramName() {
      return this.programName == null ? null : this.programName.getName();
   }

   public String getProgramDisplayName() {
      return this.programName == null ? null : this.programName.getDisplayName();
   }

   public void setProgramName(String name, String... aliases) {
      this.programName = new JCommander.ProgramName(name, Arrays.asList(aliases));
   }

   public void usage() {
      StringBuilder sb = new StringBuilder();
      this.usageFormatter.usage(sb);
      this.getConsole().println(sb.toString());
   }

   public void setUsageFormatter(IUsageFormatter usageFormatter) {
      if (usageFormatter == null) {
         throw new IllegalArgumentException("Argument UsageFormatter must not be null");
      } else {
         this.usageFormatter = usageFormatter;
      }
   }

   public IUsageFormatter getUsageFormatter() {
      return this.usageFormatter;
   }

   public JCommander.Options getOptions() {
      return this.options;
   }

   public Map<FuzzyMap.IKey, ParameterDescription> getDescriptions() {
      return this.descriptions;
   }

   public JCommander.MainParameter getMainParameter() {
      return this.mainParameter;
   }

   public static JCommander.Builder newBuilder() {
      return new JCommander.Builder();
   }

   public Map<Parameterized, ParameterDescription> getFields() {
      return this.fields;
   }

   public Comparator<? super ParameterDescription> getParameterDescriptionComparator() {
      return this.options.parameterDescriptionComparator;
   }

   public void setParameterDescriptionComparator(Comparator<? super ParameterDescription> c) {
      this.options.parameterDescriptionComparator = c;
   }

   public void setColumnSize(int columnSize) {
      this.options.columnSize = columnSize;
   }

   public int getColumnSize() {
      return this.options.columnSize;
   }

   public java.util.ResourceBundle getBundle() {
      return this.options.bundle;
   }

   public List<ParameterDescription> getParameters() {
      return new ArrayList<>(this.fields.values());
   }

   public ParameterDescription getMainParameterValue() {
      return this.mainParameter.description;
   }

   private void p(String string) {
      if (this.options.verbose > 0 || System.getProperty("jcommander.debug") != null) {
         this.getConsole().println("[JCommander] " + string);
      }
   }

   public void setDefaultProvider(IDefaultProvider defaultProvider) {
      this.options.defaultProvider = defaultProvider;
   }

   public void addConverterFactory(final IStringConverterFactory converterFactory) {
      this.addConverterInstanceFactory(new IStringConverterInstanceFactory() {
         @Override
         public IStringConverter<?> getConverterInstance(Parameter parameter, Class<?> forType, String optionName) {
            Class<? extends IStringConverter<?>> converterClass = converterFactory.getConverter(forType);

            try {
               if (optionName == null) {
                  optionName = parameter.names().length > 0 ? parameter.names()[0] : "[Main class]";
               }

               return converterClass != null ? JCommander.instantiateConverter(optionName, converterClass) : null;
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var6) {
               throw new ParameterException(var6);
            }
         }
      });
   }

   public void addConverterInstanceFactory(IStringConverterInstanceFactory converterInstanceFactory) {
      this.options.converterInstanceFactories.add(0, converterInstanceFactory);
   }

   private IStringConverter<?> findConverterInstance(Parameter parameter, Class<?> forType, String optionName) {
      for (IStringConverterInstanceFactory f : this.options.converterInstanceFactories) {
         IStringConverter<?> result = f.getConverterInstance(parameter, forType, optionName);
         if (result != null) {
            return result;
         }
      }

      return null;
   }

   public Object convertValue(final Parameterized parameterized, Class type, String optionName, String value) {
      Parameter annotation = parameterized.getParameter();
      if (annotation == null) {
         return value;
      } else {
         if (optionName == null) {
            optionName = annotation.names().length > 0 ? annotation.names()[0] : "[Main class]";
         }

         IStringConverter<?> converter = null;
         if (type.isAssignableFrom(List.class)) {
            converter = tryInstantiateConverter(optionName, (Class<IStringConverter<?>>)annotation.listConverter());
         }

         if (type.isAssignableFrom(List.class) && converter == null) {
            IParameterSplitter splitter = tryInstantiateConverter(null, annotation.splitter());
            converter = new DefaultListConverter(splitter, new IStringConverter() {
               @Override
               public Object convert(String value) {
                  Type genericType = parameterized.findFieldGenericType();
                  return JCommander.this.convertValue(parameterized, genericType instanceof Class ? (Class)genericType : String.class, null, value);
               }
            });
         }

         if (converter == null) {
            converter = tryInstantiateConverter(optionName, (Class<IStringConverter<?>>)annotation.converter());
         }

         if (converter == null) {
            converter = this.findConverterInstance(annotation, type, optionName);
         }

         if (converter == null && type.isEnum()) {
            converter = new EnumConverter(optionName, type);
         }

         if (converter == null) {
            converter = new StringConverter();
         }

         return converter.convert(value);
      }
   }

   private static <T> T tryInstantiateConverter(String optionName, Class<T> converterClass) {
      if (converterClass != NoConverter.class && converterClass != null) {
         try {
            return instantiateConverter(optionName, converterClass);
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static <T> T instantiateConverter(String optionName, Class<? extends T> converterClass) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      Constructor<T> ctor = null;
      Constructor<T> stringCtor = null;

      for (Constructor<T> c : converterClass.getDeclaredConstructors()) {
         c.setAccessible(true);
         Class<?>[] types = c.getParameterTypes();
         if (types.length == 1 && types[0].equals(String.class)) {
            stringCtor = c;
         } else if (types.length == 0) {
            ctor = c;
         }
      }

      return stringCtor != null ? stringCtor.newInstance(optionName) : (ctor != null ? ctor.newInstance() : null);
   }

   public void addCommand(String name, Object object) {
      this.addCommand(name, object);
   }

   public void addCommand(Object object) {
      Parameters p = object.getClass().getAnnotation(Parameters.class);
      if (p != null && p.commandNames().length > 0) {
         for (String commandName : p.commandNames()) {
            this.addCommand(commandName, object);
         }
      } else {
         throw new ParameterException("Trying to add command " + object.getClass().getName() + " without specifying its names in @Parameters");
      }
   }

   public void addCommand(String name, Object object, String... aliases) {
      JCommander jc = new JCommander(this.options);
      jc.addObject(object);
      jc.createDescriptions();
      jc.setProgramName(name, aliases);
      JCommander.ProgramName progName = jc.programName;
      this.commands.put(progName, jc);
      this.aliasMap.put(new StringKey(name), progName);

      for (String a : aliases) {
         FuzzyMap.IKey alias = new StringKey(a);
         if (!alias.equals(name)) {
            JCommander.ProgramName mappedName = this.aliasMap.get(alias);
            if (mappedName != null && !mappedName.equals(progName)) {
               throw new ParameterException(
                  "Cannot set alias " + alias + " for " + name + " command because it has already been defined for " + mappedName.name + " command"
               );
            }

            this.aliasMap.put(alias, progName);
         }
      }
   }

   public Map<String, JCommander> getCommands() {
      Map<String, JCommander> res = Maps.newLinkedHashMap();

      for (Entry<JCommander.ProgramName, JCommander> entry : this.commands.entrySet()) {
         res.put(entry.getKey().name, entry.getValue());
      }

      return res;
   }

   public Map<JCommander.ProgramName, JCommander> getRawCommands() {
      Map<JCommander.ProgramName, JCommander> res = Maps.newLinkedHashMap();

      for (Entry<JCommander.ProgramName, JCommander> entry : this.commands.entrySet()) {
         res.put(entry.getKey(), entry.getValue());
      }

      return res;
   }

   public String getParsedCommand() {
      return this.parsedCommand;
   }

   public String getParsedAlias() {
      return this.parsedAlias;
   }

   private String s(int count) {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < count; i++) {
         result.append(" ");
      }

      return result.toString();
   }

   public List<Object> getObjects() {
      return this.objects;
   }

   private ParameterDescription findParameterDescription(String arg) {
      return FuzzyMap.findInMap(this.descriptions, new StringKey(arg), this.options.caseSensitiveOptions, this.options.allowAbbreviatedOptions);
   }

   private JCommander findCommand(JCommander.ProgramName name) {
      return FuzzyMap.findInMap(this.commands, name, this.options.caseSensitiveOptions, this.options.allowAbbreviatedOptions);
   }

   private JCommander.ProgramName findProgramName(String name) {
      return FuzzyMap.findInMap(this.aliasMap, new StringKey(name), this.options.caseSensitiveOptions, this.options.allowAbbreviatedOptions);
   }

   public JCommander findCommandByAlias(String commandOrAlias) {
      JCommander.ProgramName progName = this.findProgramName(commandOrAlias);
      if (progName == null) {
         return null;
      } else {
         JCommander jc = this.findCommand(progName);
         if (jc == null) {
            throw new IllegalStateException("There appears to be inconsistency in the internal command database.  This is likely a bug. Please report.");
         } else {
            return jc;
         }
      }
   }

   public void setVerbose(int verbose) {
      this.options.verbose = verbose;
   }

   public void setCaseSensitiveOptions(boolean b) {
      this.options.caseSensitiveOptions = b;
   }

   public void setAllowAbbreviatedOptions(boolean b) {
      this.options.allowAbbreviatedOptions = b;
   }

   public void setAcceptUnknownOptions(boolean b) {
      this.options.acceptUnknownOptions = b;
   }

   public List<String> getUnknownOptions() {
      return this.unknownArgs;
   }

   public void setAllowParameterOverwriting(boolean b) {
      this.options.allowParameterOverwriting = b;
   }

   public boolean isParameterOverwritingAllowed() {
      return this.options.allowParameterOverwriting;
   }

   public void setAtFileCharset(Charset charset) {
      this.options.atFileCharset = charset;
   }

   public static class Builder {
      private JCommander jCommander = new JCommander();
      private String[] args = null;

      public JCommander.Builder addObject(Object o) {
         this.jCommander.addObject(o);
         return this;
      }

      public JCommander.Builder resourceBundle(java.util.ResourceBundle bundle) {
         this.jCommander.setDescriptionsBundle(bundle);
         return this;
      }

      public JCommander.Builder args(String[] args) {
         this.args = args;
         return this;
      }

      public JCommander.Builder console(Console console) {
         this.jCommander.setConsole(console);
         return this;
      }

      public JCommander.Builder expandAtSign(Boolean expand) {
         this.jCommander.setExpandAtSign(expand);
         return this;
      }

      public JCommander.Builder programName(String name) {
         this.jCommander.setProgramName(name);
         return this;
      }

      public JCommander.Builder columnSize(int columnSize) {
         this.jCommander.setColumnSize(columnSize);
         return this;
      }

      public JCommander.Builder defaultProvider(IDefaultProvider provider) {
         this.jCommander.setDefaultProvider(provider);
         return this;
      }

      public JCommander.Builder addConverterFactory(IStringConverterFactory factory) {
         this.jCommander.addConverterFactory(factory);
         return this;
      }

      public JCommander.Builder verbose(int verbose) {
         this.jCommander.setVerbose(verbose);
         return this;
      }

      public JCommander.Builder allowAbbreviatedOptions(boolean b) {
         this.jCommander.setAllowAbbreviatedOptions(b);
         return this;
      }

      public JCommander.Builder acceptUnknownOptions(boolean b) {
         this.jCommander.setAcceptUnknownOptions(b);
         return this;
      }

      public JCommander.Builder allowParameterOverwriting(boolean b) {
         this.jCommander.setAllowParameterOverwriting(b);
         return this;
      }

      public JCommander.Builder atFileCharset(Charset charset) {
         this.jCommander.setAtFileCharset(charset);
         return this;
      }

      public JCommander.Builder addConverterInstanceFactory(IStringConverterInstanceFactory factory) {
         this.jCommander.addConverterInstanceFactory(factory);
         return this;
      }

      public JCommander.Builder addCommand(Object command) {
         this.jCommander.addCommand(command);
         return this;
      }

      public JCommander.Builder addCommand(String name, Object command, String... aliases) {
         this.jCommander.addCommand(name, command, aliases);
         return this;
      }

      public JCommander.Builder usageFormatter(IUsageFormatter usageFormatter) {
         this.jCommander.setUsageFormatter(usageFormatter);
         return this;
      }

      public JCommander build() {
         if (this.args != null) {
            this.jCommander.parse(this.args);
         }

         return this.jCommander;
      }
   }

   private class DefaultVariableArity implements IVariableArity {
      private DefaultVariableArity() {
      }

      @Override
      public int processVariableArity(String optionName, String[] options) {
         int i = 0;

         while (i < options.length && !JCommander.this.isOption(options[i])) {
            i++;
         }

         return i;
      }
   }

   static class MainParameter {
      Parameterized parameterized;
      Object object;
      private Parameter annotation;
      private ParameterDescription description;
      private List<Object> multipleValue = null;
      private Object singleValue = null;
      private boolean firstTimeMainParameter = true;

      public ParameterDescription getDescription() {
         return this.description;
      }

      public void addValue(Object convertedValue) {
         if (this.multipleValue != null) {
            this.multipleValue.add(convertedValue);
         } else {
            if (this.singleValue != null) {
               throw new ParameterException("Only one main parameter allowed but found several: \"" + this.singleValue + "\" and \"" + convertedValue + "\"");
            }

            this.singleValue = convertedValue;
            this.parameterized.set(this.object, convertedValue);
         }
      }
   }

   private static class Options {
      private java.util.ResourceBundle bundle;
      private IDefaultProvider defaultProvider;
      private Comparator<? super ParameterDescription> parameterDescriptionComparator = new Comparator<ParameterDescription>() {
         public int compare(ParameterDescription p0, ParameterDescription p1) {
            WrappedParameter a0 = p0.getParameter();
            WrappedParameter a1 = p1.getParameter();
            if (a0 != null && a0.order() != -1 && a1 != null && a1.order() != -1) {
               return Integer.compare(a0.order(), a1.order());
            } else if (a0 != null && a0.order() != -1) {
               return -1;
            } else {
               return a1 != null && a1.order() != -1 ? 1 : p0.getLongestName().compareTo(p1.getLongestName());
            }
         }
      };
      private int columnSize = 79;
      private boolean acceptUnknownOptions = false;
      private boolean allowParameterOverwriting = false;
      private boolean expandAtSign = true;
      private int verbose = 0;
      private boolean caseSensitiveOptions = true;
      private boolean allowAbbreviatedOptions = false;
      private final List<IStringConverterInstanceFactory> converterInstanceFactories = new CopyOnWriteArrayList<>();
      private Charset atFileCharset = Charset.defaultCharset();

      private Options() {
      }
   }

   public static final class ProgramName implements FuzzyMap.IKey {
      private final String name;
      private final List<String> aliases;

      ProgramName(String name, List<String> aliases) {
         this.name = name;
         this.aliases = aliases;
      }

      @Override
      public String getName() {
         return this.name;
      }

      public String getDisplayName() {
         StringBuilder sb = new StringBuilder();
         sb.append(this.name);
         if (!this.aliases.isEmpty()) {
            sb.append("(");
            Iterator<String> aliasesIt = this.aliases.iterator();

            while (aliasesIt.hasNext()) {
               sb.append(aliasesIt.next());
               if (aliasesIt.hasNext()) {
                  sb.append(",");
               }
            }

            sb.append(")");
         }

         return sb.toString();
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         return 31 * result + (this.name == null ? 0 : this.name.hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            JCommander.ProgramName other = (JCommander.ProgramName)obj;
            if (this.name == null) {
               if (other.name != null) {
                  return false;
               }
            } else if (!this.name.equals(other.name)) {
               return false;
            }

            return true;
         }
      }

      @Override
      public String toString() {
         return this.getDisplayName();
      }
   }
}
