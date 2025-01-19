package com.alibaba.fastjson2.filter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class BeanContext {
   private final Class beanClass;
   private final Method method;
   private final Field field;
   private final String name;
   private final String label;
   private final Class fieldClass;
   private final Type fieldType;
   private final long features;
   private final String format;

   public BeanContext(Class beanClass, Method method, Field field, String name, String label, Class fieldClass, Type fieldType, long features, String format) {
      this.beanClass = beanClass;
      this.method = method;
      this.field = field;
      this.name = name;
      this.label = label;
      this.fieldClass = fieldClass;
      this.fieldType = fieldType;
      this.features = features;
      this.format = format;
   }

   public Class<?> getBeanClass() {
      return this.beanClass;
   }

   public Method getMethod() {
      return this.method;
   }

   public Field getField() {
      return this.field;
   }

   public String getName() {
      return this.name;
   }

   public String getLabel() {
      return this.label;
   }

   public Class<?> getFieldClass() {
      return this.fieldClass;
   }

   public Type getFieldType() {
      return this.fieldType;
   }

   public long getFeatures() {
      return this.features;
   }

   public boolean isJsonDirect() {
      return (this.features & 1125899906842624L) != 0L;
   }

   public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
      T annotation = null;
      if (this.method != null) {
         annotation = this.method.getAnnotation(annotationClass);
      }

      if (annotation == null && this.field != null) {
         annotation = this.field.getAnnotation(annotationClass);
      }

      return annotation;
   }

   public String getFormat() {
      return this.format;
   }
}
