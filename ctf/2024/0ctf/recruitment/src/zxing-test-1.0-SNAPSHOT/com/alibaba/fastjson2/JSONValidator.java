package com.alibaba.fastjson2;

public class JSONValidator {
   private final JSONReader jsonReader;
   private Boolean validateResult;
   private JSONValidator.Type type;

   protected JSONValidator(JSONReader jsonReader) {
      this.jsonReader = jsonReader;
   }

   public static JSONValidator fromUtf8(byte[] jsonBytes) {
      return new JSONValidator(JSONReader.of(jsonBytes));
   }

   public static JSONValidator from(String jsonStr) {
      return new JSONValidator(JSONReader.of(jsonStr));
   }

   public static JSONValidator from(JSONReader jsonReader) {
      return new JSONValidator(jsonReader);
   }

   public boolean validate() {
      if (this.validateResult != null) {
         return this.validateResult;
      } else {
         char firstChar;
         label44: {
            boolean var3;
            try {
               firstChar = this.jsonReader.current();
               this.jsonReader.skipValue();
               break label44;
            } catch (ArrayIndexOutOfBoundsException | JSONException var7) {
               var3 = this.validateResult = false;
            } finally {
               this.jsonReader.close();
            }

            return var3;
         }

         if (firstChar == '{') {
            this.type = JSONValidator.Type.Object;
         } else if (firstChar == '[') {
            this.type = JSONValidator.Type.Array;
         } else {
            this.type = JSONValidator.Type.Value;
         }

         return this.validateResult = this.jsonReader.isEnd();
      }
   }

   public JSONValidator.Type getType() {
      if (this.type == null) {
         this.validate();
      }

      return this.type;
   }

   public static enum Type {
      Object,
      Array,
      Value;
   }
}
