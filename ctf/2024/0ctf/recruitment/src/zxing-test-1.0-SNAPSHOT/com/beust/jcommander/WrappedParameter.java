package com.beust.jcommander;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WrappedParameter {
   private Parameter parameter;
   private DynamicParameter dynamicParameter;

   public WrappedParameter(Parameter p) {
      this.parameter = p;
   }

   public WrappedParameter(DynamicParameter p) {
      this.dynamicParameter = p;
   }

   public Parameter getParameter() {
      return this.parameter;
   }

   public DynamicParameter getDynamicParameter() {
      return this.dynamicParameter;
   }

   public int arity() {
      return this.parameter != null ? this.parameter.arity() : 1;
   }

   public boolean hidden() {
      return this.parameter != null ? this.parameter.hidden() : this.dynamicParameter.hidden();
   }

   public boolean required() {
      return this.parameter != null ? this.parameter.required() : this.dynamicParameter.required();
   }

   public boolean password() {
      return this.parameter != null ? this.parameter.password() : false;
   }

   public String[] names() {
      return this.parameter != null ? this.parameter.names() : this.dynamicParameter.names();
   }

   public boolean variableArity() {
      return this.parameter != null ? this.parameter.variableArity() : false;
   }

   public int order() {
      return this.parameter != null ? this.parameter.order() : this.dynamicParameter.order();
   }

   public Class<? extends IParameterValidator>[] validateWith() {
      return this.parameter != null ? this.parameter.validateWith() : this.dynamicParameter.validateWith();
   }

   public Class<? extends IValueValidator>[] validateValueWith() {
      return this.parameter != null ? this.parameter.validateValueWith() : this.dynamicParameter.validateValueWith();
   }

   public boolean echoInput() {
      return this.parameter != null ? this.parameter.echoInput() : false;
   }

   public void addValue(Parameterized parameterized, Object object, Object value) {
      try {
         this.addValue(parameterized, object, value, null);
      } catch (IllegalAccessException var5) {
         throw new ParameterException("Couldn't set " + object + " to " + value, var5);
      }
   }

   public void addValue(Parameterized parameterized, Object object, Object value, Field field) throws IllegalAccessException {
      if (this.parameter != null) {
         if (field != null) {
            field.set(object, value);
         } else {
            parameterized.set(object, value);
         }
      } else {
         String a = this.dynamicParameter.assignment();
         String sv = value.toString();
         int aInd = sv.indexOf(a);
         if (aInd == -1) {
            throw new ParameterException("Dynamic parameter expected a value of the form a" + a + "b but got:" + sv);
         }

         this.callPut(object, parameterized, sv.substring(0, aInd), sv.substring(aInd + 1));
      }
   }

   private void callPut(Object object, Parameterized parameterized, String key, String value) {
      try {
         Method m = this.findPut(parameterized.getType());
         m.invoke(parameterized.get(object), key, value);
      } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | SecurityException var6) {
         var6.printStackTrace();
      }
   }

   private Method findPut(Class<?> cls) throws SecurityException, NoSuchMethodException {
      return cls.getMethod("put", Object.class, Object.class);
   }

   public String getAssignment() {
      return this.dynamicParameter != null ? this.dynamicParameter.assignment() : "";
   }

   public boolean isHelp() {
      return this.parameter != null && this.parameter.help();
   }

   public boolean isNonOverwritableForced() {
      return this.parameter != null && this.parameter.forceNonOverwritable();
   }
}
