package com.beust.jcommander;

import com.beust.jcommander.validators.NoValidator;
import com.beust.jcommander.validators.NoValueValidator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ParameterDescription {
   private Object object;
   private WrappedParameter wrappedParameter;
   private Parameter parameterAnnotation;
   private DynamicParameter dynamicParameterAnnotation;
   private Parameterized parameterized;
   private boolean assigned = false;
   private java.util.ResourceBundle bundle;
   private String description;
   private JCommander jCommander;
   private Object defaultObject;
   private String longestName = "";

   public ParameterDescription(Object object, DynamicParameter annotation, Parameterized parameterized, java.util.ResourceBundle bundle, JCommander jc) {
      if (!Map.class.isAssignableFrom(parameterized.getType())) {
         throw new ParameterException("@DynamicParameter " + parameterized.getName() + " should be of type Map but is " + parameterized.getType().getName());
      } else {
         this.dynamicParameterAnnotation = annotation;
         this.wrappedParameter = new WrappedParameter(this.dynamicParameterAnnotation);
         this.init(object, parameterized, bundle, jc);
      }
   }

   public ParameterDescription(Object object, Parameter annotation, Parameterized parameterized, java.util.ResourceBundle bundle, JCommander jc) {
      this.parameterAnnotation = annotation;
      this.wrappedParameter = new WrappedParameter(this.parameterAnnotation);
      this.init(object, parameterized, bundle, jc);
   }

   private java.util.ResourceBundle findResourceBundle(Object o) {
      java.util.ResourceBundle result = null;
      Parameters p = o.getClass().getAnnotation(Parameters.class);
      if (p != null && !this.isEmpty(p.resourceBundle())) {
         result = java.util.ResourceBundle.getBundle(p.resourceBundle(), Locale.getDefault());
      } else {
         ResourceBundle a = o.getClass().getAnnotation(ResourceBundle.class);
         if (a != null && !this.isEmpty(a.value())) {
            result = java.util.ResourceBundle.getBundle(a.value(), Locale.getDefault());
         }
      }

      return result;
   }

   private boolean isEmpty(String s) {
      return s == null || "".equals(s);
   }

   private void initDescription(String description, String descriptionKey, String[] names) {
      this.description = description;
      if (!"".equals(descriptionKey) && this.bundle != null) {
         this.description = this.bundle.getString(descriptionKey);
      }

      for (String name : names) {
         if (name.length() > this.longestName.length()) {
            this.longestName = name;
         }
      }
   }

   private void init(Object object, Parameterized parameterized, java.util.ResourceBundle bundle, JCommander jCommander) {
      this.object = object;
      this.parameterized = parameterized;
      this.bundle = bundle;
      if (this.bundle == null) {
         this.bundle = this.findResourceBundle(object);
      }

      this.jCommander = jCommander;
      if (this.parameterAnnotation != null) {
         String description;
         if (Enum.class.isAssignableFrom(parameterized.getType()) && this.parameterAnnotation.description().isEmpty()) {
            description = "Options: " + EnumSet.allOf(parameterized.getType());
         } else {
            description = this.parameterAnnotation.description();
         }

         this.initDescription(description, this.parameterAnnotation.descriptionKey(), this.parameterAnnotation.names());
      } else {
         if (this.dynamicParameterAnnotation == null) {
            throw new AssertionError("Shound never happen");
         }

         this.initDescription(
            this.dynamicParameterAnnotation.description(), this.dynamicParameterAnnotation.descriptionKey(), this.dynamicParameterAnnotation.names()
         );
      }

      try {
         this.defaultObject = parameterized.get(object);
      } catch (Exception var6) {
      }

      if (this.defaultObject != null && this.parameterAnnotation != null) {
         this.validateDefaultValues(this.parameterAnnotation.names());
      }
   }

   private void validateDefaultValues(String[] names) {
      String name = names.length > 0 ? names[0] : "";
      this.validateValueParameter(name, this.defaultObject);
   }

   public String getLongestName() {
      return this.longestName;
   }

   public Object getDefault() {
      return this.defaultObject;
   }

   public String getDescription() {
      return this.description;
   }

   public Object getObject() {
      return this.object;
   }

   public String getNames() {
      StringBuilder sb = new StringBuilder();
      String[] names = this.wrappedParameter.names();

      for (int i = 0; i < names.length; i++) {
         if (i > 0) {
            sb.append(", ");
         }

         sb.append(names[i]);
      }

      return sb.toString();
   }

   public WrappedParameter getParameter() {
      return this.wrappedParameter;
   }

   public Parameterized getParameterized() {
      return this.parameterized;
   }

   private boolean isMultiOption() {
      Class<?> fieldType = this.parameterized.getType();
      return fieldType.equals(List.class) || fieldType.equals(Set.class) || this.parameterized.isDynamicParameter();
   }

   public void addValue(String value) {
      this.addValue(value, false);
   }

   public boolean isAssigned() {
      return this.assigned;
   }

   public void setAssigned(boolean b) {
      this.assigned = b;
   }

   public void addValue(String value, boolean isDefault) {
      this.addValue(null, value, isDefault, true, -1);
   }

   Object addValue(String name, String value, boolean isDefault, boolean validate, int currentIndex) {
      this.p("Adding " + (isDefault ? "default " : "") + "value:" + value + " to parameter:" + this.parameterized.getName());
      if (name == null) {
         name = this.wrappedParameter.names()[0];
      }

      if ((currentIndex != 0 || !this.assigned || this.isMultiOption() || this.jCommander.isParameterOverwritingAllowed()) && !this.isNonOverwritableForced()) {
         if (validate) {
            this.validateParameter(name, value);
         }

         Class<?> type = this.parameterized.getType();
         Object convertedValue = this.jCommander.convertValue(this.getParameterized(), this.getParameterized().getType(), name, value);
         if (validate) {
            this.validateValueParameter(name, convertedValue);
         }

         boolean isCollection = Collection.class.isAssignableFrom(type);
         Object finalValue;
         if (isCollection) {
            Collection<Object> l = (Collection<Object>)this.parameterized.get(this.object);
            if (l == null || this.fieldIsSetForTheFirstTime(isDefault)) {
               l = this.newCollection(type);
               this.parameterized.set(this.object, l);
            }

            if (convertedValue instanceof Collection) {
               l.addAll((Collection<? extends Object>)convertedValue);
            } else {
               l.add(convertedValue);
            }

            finalValue = l;
         } else {
            List<ParameterDescription.SubParameterIndex> subParameters = this.findSubParameters(type);
            if (!subParameters.isEmpty()) {
               finalValue = this.handleSubParameters(value, currentIndex, type, subParameters);
            } else {
               this.wrappedParameter.addValue(this.parameterized, this.object, convertedValue);
               finalValue = convertedValue;
            }
         }

         if (!isDefault) {
            this.assigned = true;
         }

         return finalValue;
      } else {
         throw new ParameterException("Can only specify option " + name + " once.");
      }
   }

   private Object handleSubParameters(String value, int currentIndex, Class<?> type, List<ParameterDescription.SubParameterIndex> subParameters) {
      ParameterDescription.SubParameterIndex sai = null;

      for (ParameterDescription.SubParameterIndex si : subParameters) {
         if (si.order == currentIndex) {
            sai = si;
            break;
         }
      }

      if (sai != null) {
         Object objectValue = this.parameterized.get(this.object);

         try {
            if (objectValue == null) {
               objectValue = type.newInstance();
               this.parameterized.set(this.object, objectValue);
            }

            this.wrappedParameter.addValue(this.parameterized, objectValue, value, sai.field);
            return objectValue;
         } catch (IllegalAccessException | InstantiationException var9) {
            throw new ParameterException("Couldn't instantiate " + type, var9);
         }
      } else {
         throw new ParameterException("Couldn't find where to assign parameter " + value + " in " + type);
      }
   }

   public Parameter getParameterAnnotation() {
      return this.parameterAnnotation;
   }

   private List<ParameterDescription.SubParameterIndex> findSubParameters(Class<?> type) {
      List<ParameterDescription.SubParameterIndex> result = new ArrayList<>();

      for (Field field : type.getDeclaredFields()) {
         Annotation subParameter = field.getAnnotation(SubParameter.class);
         if (subParameter != null) {
            SubParameter sa = (SubParameter)subParameter;
            result.add(new ParameterDescription.SubParameterIndex(sa.order(), field));
         }
      }

      return result;
   }

   private void validateParameter(String name, String value) {
      Class<? extends IParameterValidator>[] validators = this.wrappedParameter.validateWith();
      if (validators != null && validators.length > 0) {
         for (Class<? extends IParameterValidator> validator : validators) {
            this.validateParameter(validator, name, value);
         }
      }
   }

   void validateValueParameter(String name, Object value) {
      Class<? extends IValueValidator>[] validators = this.wrappedParameter.validateValueWith();
      if (validators != null && validators.length > 0) {
         for (Class<? extends IValueValidator> validator : validators) {
            this.validateValueParameter(validator, name, value);
         }
      }
   }

   public void validateValueParameter(Class<? extends IValueValidator> validator, String name, Object value) {
      try {
         if (validator != NoValueValidator.class) {
            this.p("Validating value parameter:" + name + " value:" + value + " validator:" + validator);
         }

         ((IValueValidator)validator.newInstance()).validate(name, value);
      } catch (IllegalAccessException | InstantiationException var5) {
         throw new ParameterException("Can't instantiate validator:" + var5);
      }
   }

   public void validateParameter(Class<? extends IParameterValidator> validator, String name, String value) {
      try {
         if (validator != NoValidator.class) {
            this.p("Validating parameter:" + name + " value:" + value + " validator:" + validator);
         }

         validator.newInstance().validate(name, value);
         if (IParameterValidator2.class.isAssignableFrom(validator)) {
            IParameterValidator2 instance = (IParameterValidator2)validator.newInstance();
            instance.validate(name, value, this);
         }
      } catch (IllegalAccessException | InstantiationException var5) {
         throw new ParameterException("Can't instantiate validator:" + var5);
      } catch (ParameterException var6) {
         throw var6;
      } catch (Exception var7) {
         throw new ParameterException(var7);
      }
   }

   private Collection<Object> newCollection(Class<?> type) {
      if (SortedSet.class.isAssignableFrom(type)) {
         return new TreeSet<>();
      } else if (LinkedHashSet.class.isAssignableFrom(type)) {
         return new LinkedHashSet<>();
      } else if (Set.class.isAssignableFrom(type)) {
         return new HashSet<>();
      } else if (List.class.isAssignableFrom(type)) {
         return new ArrayList<>();
      } else {
         throw new ParameterException("Parameters of Collection type '" + type.getSimpleName() + "' are not supported. Please use List or Set instead.");
      }
   }

   private boolean fieldIsSetForTheFirstTime(boolean isDefault) {
      return !isDefault && !this.assigned;
   }

   private void p(String string) {
      if (System.getProperty("jcommander.debug") != null) {
         this.jCommander.getConsole().println("[ParameterDescription] " + string);
      }
   }

   @Override
   public String toString() {
      return "[ParameterDescription " + this.parameterized.getName() + "]";
   }

   public boolean isDynamicParameter() {
      return this.dynamicParameterAnnotation != null;
   }

   public boolean isHelp() {
      return this.wrappedParameter.isHelp();
   }

   public boolean isNonOverwritableForced() {
      return this.wrappedParameter.isNonOverwritableForced();
   }

   class SubParameterIndex {
      int order = -1;
      Field field;

      public SubParameterIndex(int order, Field field) {
         this.order = order;
         this.field = field;
      }
   }
}
