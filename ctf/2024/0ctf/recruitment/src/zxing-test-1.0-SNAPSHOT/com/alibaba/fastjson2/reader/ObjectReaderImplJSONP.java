package com.alibaba.fastjson2.reader;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPObject;
import com.alibaba.fastjson2.JSONReader;
import java.lang.reflect.Type;

public class ObjectReaderImplJSONP implements ObjectReader {
   private final Class objectClass;

   public ObjectReaderImplJSONP(Class objectClass) {
      this.objectClass = objectClass;
   }

   @Override
   public Object readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
      String funcName = jsonReader.readFieldNameUnquote();
      if (jsonReader.nextIfMatch('.')) {
         String name2 = jsonReader.readFieldNameUnquote();
         funcName = funcName + '.' + name2;
      }

      char ch = jsonReader.current();
      if (ch == '/' && jsonReader.nextIfMatchIdent('/', '*', '*', '/')) {
         ch = jsonReader.current();
      }

      if (ch != '(') {
         throw new JSONException(jsonReader.info("illegal jsonp input"));
      } else {
         jsonReader.next();
         JSONPObject jsonp;
         if (this.objectClass == JSONObject.class) {
            jsonp = new JSONPObject(funcName);
         } else {
            try {
               jsonp = (JSONPObject)this.objectClass.newInstance();
            } catch (IllegalAccessException | InstantiationException var10) {
               throw new JSONException("create jsonp instance error", var10);
            }

            jsonp.setFunction(funcName);
         }

         while (!jsonReader.isEnd()) {
            if (jsonReader.nextIfMatch(')')) {
               jsonReader.nextIfMatch(';');
               jsonReader.nextIfMatchIdent('/', '*', '*', '/');
               return jsonp;
            }

            Object param = jsonReader.readAny();
            jsonp.addParameter(param);
         }

         throw new JSONException(jsonReader.info("illegal jsonp input"));
      }
   }
}
