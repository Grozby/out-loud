package com.alibaba.fastjson2.annotation;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface JSONField {
   int ordinal() default 0;

   String name() default "";

   String format() default "";

   String label() default "";

   boolean serialize() default true;

   boolean deserialize() default true;

   boolean unwrapped() default false;

   String[] alternateNames() default {};

   @Deprecated
   Class writeUsing() default Void.class;

   Class serializeUsing() default Void.class;

   Class deserializeUsing() default Void.class;

   JSONReader.Feature[] deserializeFeatures() default {};

   JSONWriter.Feature[] serializeFeatures() default {};

   boolean value() default false;

   String defaultValue() default "";

   String locale() default "";

   String schema() default "";

   boolean jsonDirect() default false;

   boolean required() default false;

   String arrayToMapKey() default "";

   Class<?> arrayToMapDuplicateHandler() default Void.class;
}
