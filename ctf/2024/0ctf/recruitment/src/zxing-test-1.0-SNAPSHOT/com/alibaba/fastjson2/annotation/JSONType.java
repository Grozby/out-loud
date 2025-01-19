package com.alibaba.fastjson2.annotation;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.filter.Filter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface JSONType {
   Class<?> builder() default void.class;

   String typeKey() default "";

   String typeName() default "";

   Class<?>[] seeAlso() default {};

   Class<?> seeAlsoDefault() default Void.class;

   boolean alphabetic() default true;

   JSONReader.Feature[] deserializeFeatures() default {};

   JSONWriter.Feature[] serializeFeatures() default {};

   PropertyNamingStrategy naming() default PropertyNamingStrategy.NeverUseThisValueExceptDefaultValue;

   boolean writeEnumAsJavaBean() default false;

   String[] ignores() default {};

   String[] includes() default {};

   String[] orders() default {};

   Class<?> serializer() default Void.class;

   Class<?> deserializer() default Void.class;

   Class<? extends Filter>[] serializeFilters() default {};

   String schema() default "";

   String format() default "";

   String locale() default "";

   Class<? extends JSONReader.AutoTypeBeforeHandler> autoTypeBeforeHandler() default JSONReader.AutoTypeBeforeHandler.class;

   boolean disableReferenceDetect() default false;

   String rootName() default "";
}
