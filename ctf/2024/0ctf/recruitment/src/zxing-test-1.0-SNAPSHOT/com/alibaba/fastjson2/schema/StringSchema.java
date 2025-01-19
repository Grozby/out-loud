package com.alibaba.fastjson2.schema;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringSchema extends JSONSchema {
   static final Pattern EMAIL_PATTERN = Pattern.compile("^\\s*?(.+)@(.+?)\\s*$");
   static final Pattern IP_DOMAIN_PATTERN = Pattern.compile("^\\[(.*)\\]$");
   static final Pattern USER_PATTERN = Pattern.compile(
      "^\\s*(((\\\\.)|[^\\s\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]]|')+|(\"[^\"]*\"))(\\.(((\\\\.)|[^\\s\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]]|')+|(\"[^\"]*\")))*$"
   );
   final int maxLength;
   final int minLength;
   final String format;
   final String patternFormat;
   final Pattern pattern;
   final boolean typed;
   final AnyOf anyOf;
   final OneOf oneOf;
   final String constValue;
   final Set<String> enumValues;
   final Predicate<String> formatValidator;

   StringSchema(JSONObject input) {
      super(input);
      this.typed = "string".equalsIgnoreCase(input.getString("type"));
      this.minLength = input.getIntValue("minLength", -1);
      this.maxLength = input.getIntValue("maxLength", -1);
      this.patternFormat = input.getString("pattern");
      this.pattern = this.patternFormat == null ? null : Pattern.compile(this.patternFormat);
      this.format = input.getString("format");
      Object anyOf = input.get("anyOf");
      if (anyOf instanceof JSONArray) {
         this.anyOf = anyOf((JSONArray)anyOf, String.class);
      } else {
         this.anyOf = null;
      }

      Object oneOf = input.get("oneOf");
      if (oneOf instanceof JSONArray) {
         this.oneOf = oneOf((JSONArray)oneOf, String.class);
      } else {
         this.oneOf = null;
      }

      this.constValue = input.getString("const");
      Set<String> enumValues = null;
      Object property = input.get("enum");
      if (property instanceof Collection) {
         Collection enums = (Collection)property;
         enumValues = new LinkedHashSet<>(enums.size());
         enumValues.addAll(enums);
      } else if (property instanceof Object[]) {
         enumValues = input.getObject("enum", TypeReference.collectionType(LinkedHashSet.class, String.class));
      }

      this.enumValues = enumValues;
      if (this.format == null) {
         this.formatValidator = null;
      } else {
         String var7 = this.format;
         switch (var7) {
            case "email":
               this.formatValidator = StringSchema::isEmail;
               break;
            case "ipv4":
               this.formatValidator = TypeUtils::validateIPv4;
               break;
            case "ipv6":
               this.formatValidator = TypeUtils::validateIPv6;
               break;
            case "uri":
               this.formatValidator = url -> {
                  if (url != null && !url.isEmpty()) {
                     try {
                        new URI(url);
                        return true;
                     } catch (URISyntaxException var2x) {
                        return false;
                     }
                  } else {
                     return false;
                  }
               };
               break;
            case "date-time":
               this.formatValidator = DateUtils::isDate;
               break;
            case "date":
               this.formatValidator = DateUtils::isLocalDate;
               break;
            case "time":
               this.formatValidator = DateUtils::isLocalTime;
               break;
            case "duration":
               this.formatValidator = str -> {
                  if (str != null && !str.isEmpty()) {
                     try {
                        Duration.parse(str);
                        return true;
                     } catch (DateTimeParseException var2x) {
                        return false;
                     }
                  } else {
                     return false;
                  }
               };
               break;
            case "uuid":
               this.formatValidator = TypeUtils::isUUID;
               break;
            default:
               this.formatValidator = null;
         }
      }
   }

   @Override
   public JSONSchema.Type getType() {
      return JSONSchema.Type.String;
   }

   @Override
   public ValidateResult validate(Object value) {
      if (value == null) {
         return this.typed ? REQUIRED_NOT_MATCH : SUCCESS;
      } else if (!(value instanceof String)) {
         return !this.typed ? SUCCESS : new ValidateResult(false, "expect type %s, but %s", JSONSchema.Type.String, value.getClass());
      } else {
         String str = (String)value;
         if (this.minLength >= 0 || this.maxLength >= 0) {
            int count = str.codePointCount(0, str.length());
            if (this.minLength >= 0 && count < this.minLength) {
               return new ValidateResult(false, "minLength not match, expect >= %s, but %s", this.minLength, str.length());
            }

            if (this.maxLength >= 0 && count > this.maxLength) {
               return new ValidateResult(false, "maxLength not match, expect <= %s, but %s", this.maxLength, str.length());
            }
         }

         if (this.pattern != null && !this.pattern.matcher(str).find()) {
            return new ValidateResult(false, "pattern not match, expect %s, but %s", this.patternFormat, str);
         } else if (this.formatValidator != null && !this.formatValidator.test(str)) {
            return new ValidateResult(false, "format not match, expect %s, but %s", this.format, str);
         } else {
            if (this.anyOf != null) {
               ValidateResult result = this.anyOf.validate(str);
               if (!result.isSuccess()) {
                  return result;
               }
            }

            if (this.oneOf != null) {
               ValidateResult result = this.oneOf.validate(str);
               if (!result.isSuccess()) {
                  return result;
               }
            }

            if (this.constValue != null && !this.constValue.equals(str)) {
               return new ValidateResult(false, "must be const %s, but %s", this.constValue, str);
            } else {
               return this.enumValues != null && !this.enumValues.contains(str) ? new ValidateResult(false, "not in enum values, %s", str) : SUCCESS;
            }
         }
      }
   }

   public static boolean isEmail(String email) {
      if (email == null) {
         return false;
      } else if (email.endsWith(".")) {
         return false;
      } else {
         Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
         if (!emailMatcher.matches()) {
            return false;
         } else {
            String user = emailMatcher.group(1);
            if (user.length() > 64) {
               return false;
            } else if (!USER_PATTERN.matcher(user).matches()) {
               return false;
            } else {
               String domain = emailMatcher.group(2);
               Matcher ipDomainMatcher = IP_DOMAIN_PATTERN.matcher(domain);
               boolean validDomain;
               if (ipDomainMatcher.matches()) {
                  String inetAddress = ipDomainMatcher.group(1);
                  validDomain = TypeUtils.validateIPv4(inetAddress) || TypeUtils.validateIPv6(inetAddress);
               } else {
                  validDomain = DomainValidator.isValid(domain) || DomainValidator.isValidTld(domain);
               }

               return validDomain;
            }
         }
      }
   }

   @Override
   public JSONObject toJSONObject() {
      JSONObject object = new JSONObject();
      object.put("type", "string");
      if (this.minLength != -1) {
         object.put("minLength", Integer.valueOf(this.minLength));
      }

      if (this.format != null) {
         object.put("format", this.format);
      }

      if (this.patternFormat != null) {
         object.put("pattern", this.pattern);
      }

      if (this.anyOf != null) {
         object.put("anyOf", this.anyOf);
      }

      if (this.oneOf != null) {
         object.put("oneOf", this.oneOf);
      }

      if (this.constValue != null) {
         object.put("const", this.constValue);
      }

      if (this.enumValues != null && !this.enumValues.isEmpty()) {
         object.put("enum", this.enumValues);
      }

      return object;
   }
}
