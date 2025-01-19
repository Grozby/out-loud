package com.alibaba.fastjson2.internal.asm;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class TypeCollector {
   static final Map<String, String> PRIMITIVES;
   final String methodName;
   final Class<?>[] parameterTypes;
   protected MethodCollector collector;

   public TypeCollector(String methodName, Class<?>[] parameterTypes) {
      this.methodName = methodName;
      this.parameterTypes = parameterTypes;
      this.collector = null;
   }

   protected MethodCollector visitMethod(int access, String name, String desc) {
      if (this.collector != null) {
         return null;
      } else if (!name.equals(this.methodName)) {
         return null;
      } else {
         Type[] argTypes = Type.getArgumentTypes(desc);
         int longOrDoubleQuantity = 0;

         for (int i = 0; i < argTypes.length; i++) {
            Type t = argTypes[i];
            String className = t.getClassName();
            if ("long".equals(className) || "double".equals(className)) {
               longOrDoubleQuantity++;
            }
         }

         if (argTypes.length != this.parameterTypes.length) {
            return null;
         } else {
            for (int ix = 0; ix < argTypes.length; ix++) {
               if (!this.correctTypeName(argTypes[ix], this.parameterTypes[ix].getName())) {
                  return null;
               }
            }

            return this.collector = new MethodCollector(Modifier.isStatic(access) ? 0 : 1, argTypes.length + longOrDoubleQuantity);
         }
      }
   }

   private boolean correctTypeName(Type type, String paramTypeName) {
      String s = type.getClassName();

      StringBuilder braces;
      for (braces = new StringBuilder(); s.endsWith("[]"); s = s.substring(0, s.length() - 2)) {
         braces.append('[');
      }

      if (braces.length() != 0) {
         if (PRIMITIVES.containsKey(s)) {
            s = braces.append(PRIMITIVES.get(s)).toString();
         } else {
            s = braces.append('L').append(s).append(';').toString();
         }
      }

      return s.equals(paramTypeName);
   }

   public String[] getParameterNamesForMethod() {
      return this.collector != null && this.collector.debugInfoPresent ? this.collector.getResult().split(",") : new String[0];
   }

   static {
      HashMap map = new HashMap();
      map.put("int", "I");
      map.put("boolean", "Z");
      map.put("byte", "B");
      map.put("char", "C");
      map.put("short", "S");
      map.put("float", "F");
      map.put("long", "J");
      map.put("double", "D");
      PRIMITIVES = map;
   }
}
