package com.alibaba.fastjson2.support.money;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderCreator;
import com.alibaba.fastjson2.reader.ObjectReaderImplValue;
import com.alibaba.fastjson2.reader.ObjectReaderNoneDefaultConstructor;
import com.alibaba.fastjson2.support.LambdaMiscCodec;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import com.alibaba.fastjson2.writer.FieldWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterAdapter;
import com.alibaba.fastjson2.writer.ObjectWriterCreator;
import com.alibaba.fastjson2.writer.ObjectWriters;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MoneySupport {
   static Class CLASS_MONETARY;
   static Class CLASS_MONETARY_AMOUNT;
   static Class CLASS_MONETARY_AMOUNT_FACTORY;
   static Class CLASS_DEFAULT_NUMBER_VALUE;
   static Class CLASS_NUMBER_VALUE;
   static Class CLASS_CURRENCY_UNIT;
   static Function<Object, Object> FUNC_CREATE;
   static Supplier<Object> FUNC_GET_DEFAULT_AMOUNT_FACTORY;
   static BiFunction<Object, Object, Object> FUNC_SET_CURRENCY;
   static BiFunction<Object, Object, Number> FUNC_SET_NUMBER;
   static Function<String, Object> FUNC_GET_CURRENCY;
   static Function<Object, BigDecimal> FUNC_NUMBER_VALUE;
   static Method METHOD_NUMBER_VALUE_OF;

   public static ObjectReader createCurrencyUnitReader() {
      if (CLASS_MONETARY == null) {
         CLASS_MONETARY = TypeUtils.loadClass("javax.money.Monetary");
      }

      if (CLASS_CURRENCY_UNIT == null) {
         CLASS_CURRENCY_UNIT = TypeUtils.loadClass("javax.money.CurrencyUnit");
      }

      if (FUNC_GET_CURRENCY == null) {
         Lookup lookup = JDKUtils.trustedLookup(CLASS_MONETARY);

         try {
            MethodHandle methodHandle = lookup.findStatic(
               CLASS_MONETARY, "getCurrency", MethodType.methodType(CLASS_CURRENCY_UNIT, String.class, String[].class)
            );
            CallSite callSite = LambdaMetafactory.metafactory(
               lookup,
               "apply",
               TypeUtils.METHOD_TYPE_BI_FUNCTION,
               TypeUtils.METHOD_TYPE_OBJECT_OBJECT_OBJECT,
               methodHandle,
               MethodType.methodType(CLASS_CURRENCY_UNIT, String.class, String[].class)
            );
            MethodHandle target = callSite.getTarget();
            BiFunction<String, String[], Object> biFunctionGetCurrency = (BiFunction)target.invokeExact();
            FUNC_GET_CURRENCY = s -> biFunctionGetCurrency.apply(s, new String[0]);
         } catch (Throwable var5) {
            throw new JSONException("method not found : javax.money.Monetary.getCurrency", var5);
         }
      }

      return ObjectReaderImplValue.of(CLASS_CURRENCY_UNIT, String.class, FUNC_GET_CURRENCY);
   }

   public static ObjectReader createMonetaryAmountReader() {
      if (CLASS_NUMBER_VALUE == null) {
         CLASS_NUMBER_VALUE = TypeUtils.loadClass("javax.money.NumberValue");
      }

      if (CLASS_CURRENCY_UNIT == null) {
         CLASS_CURRENCY_UNIT = TypeUtils.loadClass("javax.money.CurrencyUnit");
      }

      try {
         Method factoryMethod = MoneySupport.class.getMethod("createMonetaryAmount", Object.class, Object.class);
         String[] paramNames = new String[]{"currency", "number"};
         Function<Map<Long, Object>, Object> factoryFunction = ObjectReaderCreator.INSTANCE.createFactoryFunction(factoryMethod, paramNames);
         FieldReader fieldReader0 = ObjectReaderCreator.INSTANCE
            .createFieldReaderParam(
               MoneySupport.class, MoneySupport.class, "currency", 0, 0L, null, CLASS_CURRENCY_UNIT, CLASS_CURRENCY_UNIT, "currency", null, null, null
            );
         FieldReader fieldReader1 = ObjectReaderCreator.INSTANCE
            .createFieldReaderParam(
               MoneySupport.class,
               MoneySupport.class,
               "number",
               0,
               0L,
               null,
               CLASS_DEFAULT_NUMBER_VALUE,
               CLASS_DEFAULT_NUMBER_VALUE,
               "number",
               null,
               null,
               null
            );
         FieldReader[] fieldReaders = new FieldReader[]{fieldReader0, fieldReader1};
         return new ObjectReaderNoneDefaultConstructor<>(null, null, null, 0L, factoryFunction, null, paramNames, fieldReaders, null, null, null);
      } catch (NoSuchMethodException var6) {
         throw new JSONException("createMonetaryAmountReader error", var6);
      }
   }

   public static ObjectReader createNumberValueReader() {
      if (CLASS_DEFAULT_NUMBER_VALUE == null) {
         CLASS_DEFAULT_NUMBER_VALUE = TypeUtils.loadClass("org.javamoney.moneta.spi.DefaultNumberValue");
      }

      if (METHOD_NUMBER_VALUE_OF == null) {
         try {
            METHOD_NUMBER_VALUE_OF = CLASS_DEFAULT_NUMBER_VALUE.getMethod("of", Number.class);
         } catch (NoSuchMethodException var1) {
            throw new JSONException("method not found : org.javamoney.moneta.spi.DefaultNumberValue.of", var1);
         }
      }

      if (CLASS_NUMBER_VALUE == null) {
         CLASS_NUMBER_VALUE = TypeUtils.loadClass("javax.money.NumberValue");
      }

      return ObjectReaderImplValue.of(CLASS_NUMBER_VALUE, BigDecimal.class, METHOD_NUMBER_VALUE_OF);
   }

   public static ObjectWriter createMonetaryAmountWriter() {
      if (CLASS_MONETARY == null) {
         CLASS_MONETARY = TypeUtils.loadClass("javax.money.Monetary");
      }

      if (CLASS_MONETARY_AMOUNT == null) {
         CLASS_MONETARY_AMOUNT = TypeUtils.loadClass("javax.money.MonetaryAmount");
      }

      if (CLASS_NUMBER_VALUE == null) {
         CLASS_NUMBER_VALUE = TypeUtils.loadClass("javax.money.NumberValue");
      }

      if (CLASS_CURRENCY_UNIT == null) {
         CLASS_CURRENCY_UNIT = TypeUtils.loadClass("javax.money.CurrencyUnit");
      }

      Function<Object, Object> FUNC_GET_CURRENCY;
      try {
         FUNC_GET_CURRENCY = LambdaMiscCodec.createFunction(CLASS_MONETARY_AMOUNT.getMethod("getCurrency"));
      } catch (Throwable var5) {
         throw new JSONException("method not found : javax.money.Monetary.getCurrency", var5);
      }

      Function<Object, Object> FUNC_GET_NUMBER;
      try {
         FUNC_GET_NUMBER = LambdaMiscCodec.createFunction(CLASS_MONETARY_AMOUNT.getMethod("getNumber"));
      } catch (Throwable var4) {
         throw new JSONException("method not found : javax.money.Monetary.getNumber", var4);
      }

      FieldWriter fieldWriter0 = ObjectWriterCreator.INSTANCE.createFieldWriter("currency", CLASS_CURRENCY_UNIT, CLASS_CURRENCY_UNIT, FUNC_GET_CURRENCY);
      FieldWriter fieldWriter1 = ObjectWriterCreator.INSTANCE.createFieldWriter("number", CLASS_NUMBER_VALUE, CLASS_NUMBER_VALUE, FUNC_GET_NUMBER);
      return new ObjectWriterAdapter(CLASS_MONETARY_AMOUNT, null, null, 0L, Arrays.asList(fieldWriter0, fieldWriter1));
   }

   public static ObjectWriter createNumberValueWriter() {
      if (CLASS_NUMBER_VALUE == null) {
         CLASS_NUMBER_VALUE = TypeUtils.loadClass("javax.money.NumberValue");
      }

      if (FUNC_NUMBER_VALUE == null) {
         try {
            BiFunction<Object, Class, Number> biFunctionNumberValue = LambdaMiscCodec.createBiFunction(CLASS_NUMBER_VALUE.getMethod("numberValue", Class.class));
            FUNC_NUMBER_VALUE = o -> (BigDecimal)biFunctionNumberValue.apply(o, BigDecimal.class);
         } catch (Throwable var1) {
            throw new JSONException("method not found : javax.money.NumberValue.numberValue", var1);
         }
      }

      return ObjectWriters.ofToBigDecimal(FUNC_NUMBER_VALUE);
   }

   public static Object createMonetaryAmount(Object currency, Object number) {
      if (CLASS_NUMBER_VALUE == null) {
         CLASS_NUMBER_VALUE = TypeUtils.loadClass("javax.money.NumberValue");
      }

      if (CLASS_CURRENCY_UNIT == null) {
         CLASS_CURRENCY_UNIT = TypeUtils.loadClass("javax.money.CurrencyUnit");
      }

      if (CLASS_MONETARY == null) {
         CLASS_MONETARY = TypeUtils.loadClass("javax.money.Monetary");
      }

      if (CLASS_MONETARY_AMOUNT == null) {
         CLASS_MONETARY_AMOUNT = TypeUtils.loadClass("javax.money.MonetaryAmount");
      }

      if (CLASS_MONETARY_AMOUNT_FACTORY == null) {
         CLASS_MONETARY_AMOUNT_FACTORY = TypeUtils.loadClass("javax.money.MonetaryAmountFactory");
      }

      if (FUNC_GET_DEFAULT_AMOUNT_FACTORY == null) {
         Lookup lookup = JDKUtils.trustedLookup(CLASS_MONETARY);

         try {
            MethodHandle methodHandle = lookup.findStatic(CLASS_MONETARY, "getDefaultAmountFactory", MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY));
            CallSite callSite = LambdaMetafactory.metafactory(
               lookup, "get", TypeUtils.METHOD_TYPE_SUPPLIER, TypeUtils.METHOD_TYPE_OBJECT, methodHandle, MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY)
            );
            MethodHandle target = callSite.getTarget();
            FUNC_GET_DEFAULT_AMOUNT_FACTORY = (Supplier)target.invokeExact();
         } catch (Throwable var9) {
            throw new JSONException("method not found : javax.money.Monetary.getDefaultAmountFactory", var9);
         }
      }

      if (FUNC_SET_CURRENCY == null) {
         Lookup lookup = JDKUtils.trustedLookup(CLASS_MONETARY_AMOUNT_FACTORY);

         try {
            MethodHandle methodHandle = lookup.findVirtual(
               CLASS_MONETARY_AMOUNT_FACTORY, "setCurrency", MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY, CLASS_CURRENCY_UNIT)
            );
            CallSite callSite = LambdaMetafactory.metafactory(
               lookup,
               "apply",
               TypeUtils.METHOD_TYPE_BI_FUNCTION,
               TypeUtils.METHOD_TYPE_OBJECT_OBJECT_OBJECT,
               methodHandle,
               MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY, CLASS_MONETARY_AMOUNT_FACTORY, CLASS_CURRENCY_UNIT)
            );
            MethodHandle target = callSite.getTarget();
            FUNC_SET_CURRENCY = (BiFunction)target.invokeExact();
         } catch (Throwable var8) {
            throw new JSONException("method not found : javax.money.NumberValue.numberValue", var8);
         }
      }

      if (FUNC_SET_NUMBER == null) {
         Lookup lookup = JDKUtils.trustedLookup(CLASS_MONETARY_AMOUNT_FACTORY);

         try {
            MethodHandle methodHandle = lookup.findVirtual(
               CLASS_MONETARY_AMOUNT_FACTORY, "setNumber", MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY, Number.class)
            );
            CallSite callSite = LambdaMetafactory.metafactory(
               lookup,
               "apply",
               TypeUtils.METHOD_TYPE_BI_FUNCTION,
               TypeUtils.METHOD_TYPE_OBJECT_OBJECT_OBJECT,
               methodHandle,
               MethodType.methodType(CLASS_MONETARY_AMOUNT_FACTORY, CLASS_MONETARY_AMOUNT_FACTORY, Number.class)
            );
            MethodHandle target = callSite.getTarget();
            FUNC_SET_NUMBER = (BiFunction)target.invokeExact();
         } catch (Throwable var7) {
            throw new JSONException("method not found : javax.money.NumberValue.numberValue", var7);
         }
      }

      if (FUNC_CREATE == null) {
         Lookup lookup = JDKUtils.trustedLookup(CLASS_MONETARY_AMOUNT_FACTORY);

         try {
            MethodHandle methodHandle = lookup.findVirtual(CLASS_MONETARY_AMOUNT_FACTORY, "create", MethodType.methodType(CLASS_MONETARY_AMOUNT));
            CallSite callSite = LambdaMetafactory.metafactory(
               lookup,
               "apply",
               TypeUtils.METHOD_TYPE_FUNCTION,
               TypeUtils.METHOD_TYPE_OBJECT_OBJECT,
               methodHandle,
               MethodType.methodType(CLASS_MONETARY_AMOUNT, CLASS_MONETARY_AMOUNT_FACTORY)
            );
            MethodHandle target = callSite.getTarget();
            FUNC_CREATE = (Function)target.invokeExact();
         } catch (Throwable var6) {
            throw new JSONException("method not found : javax.money.NumberValue.numberValue", var6);
         }
      }

      Object factoryObject = FUNC_GET_DEFAULT_AMOUNT_FACTORY.get();
      if (currency != null) {
         FUNC_SET_CURRENCY.apply(factoryObject, currency);
      }

      if (number != null) {
         FUNC_SET_NUMBER.apply(factoryObject, number);
      }

      return FUNC_CREATE.apply(factoryObject);
   }
}
