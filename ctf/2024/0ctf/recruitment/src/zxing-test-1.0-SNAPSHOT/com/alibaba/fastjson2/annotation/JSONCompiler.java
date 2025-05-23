package com.alibaba.fastjson2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
public @interface JSONCompiler {
   JSONCompiler.CompilerOption value() default JSONCompiler.CompilerOption.DEFAULT;

   public static enum CompilerOption {
      DEFAULT,
      LAMBDA;
   }
}
