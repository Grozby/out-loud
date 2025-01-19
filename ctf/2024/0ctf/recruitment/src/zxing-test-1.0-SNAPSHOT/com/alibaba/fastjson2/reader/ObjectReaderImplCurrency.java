package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.util.Fnv;
import java.lang.reflect.Type;
import java.util.Currency;

final class ObjectReaderImplCurrency extends ObjectReaderPrimitive {
   static final ObjectReaderImplCurrency INSTANCE = new ObjectReaderImplCurrency();
   static final long TYPE_HASH = Fnv.hashCode64("Currency");

   ObjectReaderImplCurrency() {
      super(Currency.class);
   }

   @Override
   public Object readJSONBObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      if (jsonReader.getType() == -110) {
         jsonReader.next();
         long typeHash = jsonReader.readTypeHashCode();
         long TYPE_HASH_FULL = -7860540621745740270L;
         if (typeHash != TYPE_HASH && typeHash != -7860540621745740270L) {
            throw new JSONException(jsonReader.info("currency not support input autoTypeClass " + jsonReader.getString()));
         }
      }

      String strVal = jsonReader.readString();
      return strVal != null && !strVal.isEmpty() ? Currency.getInstance(strVal) : null;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String strVal;
      if (jsonReader.isObject()) {
         JSONObject jsonObject = new JSONObject();
         jsonReader.readObject(jsonObject);
         strVal = jsonObject.getString("currency");
         if (strVal == null) {
            strVal = jsonObject.getString("currencyCode");
         }
      } else {
         strVal = jsonReader.readString();
      }

      return strVal != null && !strVal.isEmpty() ? Currency.getInstance(strVal) : null;
   }
}
