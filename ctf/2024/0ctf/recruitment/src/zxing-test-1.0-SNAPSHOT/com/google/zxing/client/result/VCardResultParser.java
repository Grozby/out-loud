package com.google.zxing.client.result;

import com.google.zxing.Result;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VCardResultParser extends ResultParser {
   private static final Pattern BEGIN_VCARD = Pattern.compile("BEGIN:VCARD", 2);
   private static final Pattern VCARD_LIKE_DATE = Pattern.compile("\\d{4}-?\\d{2}-?\\d{2}");
   private static final Pattern CR_LF_SPACE_TAB = Pattern.compile("\r\n[ \t]");
   private static final Pattern NEWLINE_ESCAPE = Pattern.compile("\\\\[nN]");
   private static final Pattern VCARD_ESCAPES = Pattern.compile("\\\\([,;\\\\])");
   private static final Pattern EQUALS = Pattern.compile("=");
   private static final Pattern SEMICOLON = Pattern.compile(";");
   private static final Pattern UNESCAPED_SEMICOLONS = Pattern.compile("(?<!\\\\);+");
   private static final Pattern COMMA = Pattern.compile(",");
   private static final Pattern SEMICOLON_OR_COMMA = Pattern.compile("[;,]");

   public AddressBookParsedResult parse(Result result) {
      String rawText = getMassagedText(result);
      Matcher m = BEGIN_VCARD.matcher(rawText);
      if (m.find() && m.start() == 0) {
         List<List<String>> names = matchVCardPrefixedField("FN", rawText, true, false);
         if (names == null) {
            names = matchVCardPrefixedField("N", rawText, true, false);
            formatNames(names);
         }

         List<String> nicknameString = matchSingleVCardPrefixedField("NICKNAME", rawText, true, false);
         String[] nicknames = nicknameString == null ? null : COMMA.split(nicknameString.get(0));
         List<List<String>> phoneNumbers = matchVCardPrefixedField("TEL", rawText, true, false);
         List<List<String>> emails = matchVCardPrefixedField("EMAIL", rawText, true, false);
         List<String> note = matchSingleVCardPrefixedField("NOTE", rawText, false, false);
         List<List<String>> addresses = matchVCardPrefixedField("ADR", rawText, true, true);
         List<String> org = matchSingleVCardPrefixedField("ORG", rawText, true, true);
         List<String> birthday = matchSingleVCardPrefixedField("BDAY", rawText, true, false);
         if (birthday != null && !isLikeVCardDate(birthday.get(0))) {
            birthday = null;
         }

         List<String> title = matchSingleVCardPrefixedField("TITLE", rawText, true, false);
         List<List<String>> urls = matchVCardPrefixedField("URL", rawText, true, false);
         List<String> instantMessenger = matchSingleVCardPrefixedField("IMPP", rawText, true, false);
         List<String> geoString = matchSingleVCardPrefixedField("GEO", rawText, true, false);
         String[] geo = geoString == null ? null : SEMICOLON_OR_COMMA.split(geoString.get(0));
         if (geo != null && geo.length != 2) {
            geo = null;
         }

         return new AddressBookParsedResult(
            toPrimaryValues(names),
            nicknames,
            null,
            toPrimaryValues(phoneNumbers),
            toTypes(phoneNumbers),
            toPrimaryValues(emails),
            toTypes(emails),
            toPrimaryValue(instantMessenger),
            toPrimaryValue(note),
            toPrimaryValues(addresses),
            toTypes(addresses),
            toPrimaryValue(org),
            toPrimaryValue(birthday),
            toPrimaryValue(title),
            toPrimaryValues(urls),
            geo
         );
      } else {
         return null;
      }
   }

   static List<List<String>> matchVCardPrefixedField(CharSequence prefix, String rawText, boolean trim, boolean parseFieldDivider) {
      List<List<String>> matches = null;
      int i = 0;
      int max = rawText.length();

      while (i < max) {
         Matcher matcher = Pattern.compile("(?:^|\n)" + prefix + "(?:;([^:]*))?:", 2).matcher(rawText);
         if (i > 0) {
            i--;
         }

         if (!matcher.find(i)) {
            break;
         }

         i = matcher.end(0);
         String metadataString = matcher.group(1);
         List<String> metadata = null;
         boolean quotedPrintable = false;
         String quotedPrintableCharset = null;
         String valueType = null;
         if (metadataString != null) {
            for (String metadatum : SEMICOLON.split(metadataString)) {
               if (metadata == null) {
                  metadata = new ArrayList<>(1);
               }

               metadata.add(metadatum);
               String[] metadatumTokens = EQUALS.split(metadatum, 2);
               if (metadatumTokens.length > 1) {
                  String key = metadatumTokens[0];
                  String value = metadatumTokens[1];
                  if ("ENCODING".equalsIgnoreCase(key) && "QUOTED-PRINTABLE".equalsIgnoreCase(value)) {
                     quotedPrintable = true;
                  } else if ("CHARSET".equalsIgnoreCase(key)) {
                     quotedPrintableCharset = value;
                  } else if ("VALUE".equalsIgnoreCase(key)) {
                     valueType = value;
                  }
               }
            }
         }

         int matchStart = i;

         while (true) {
            if ((i = rawText.indexOf(10, i)) >= 0) {
               if (i < rawText.length() - 1 && (rawText.charAt(i + 1) == ' ' || rawText.charAt(i + 1) == '\t')) {
                  i += 2;
                  continue;
               }

               if (quotedPrintable && (i >= 1 && rawText.charAt(i - 1) == '=' || i >= 2 && rawText.charAt(i - 2) == '=')) {
                  i++;
                  continue;
               }
            }

            if (i < 0) {
               i = max;
            } else if (i > matchStart) {
               if (matches == null) {
                  matches = new ArrayList<>(1);
               }

               if (i >= 1 && rawText.charAt(i - 1) == '\r') {
                  i--;
               }

               String element = rawText.substring(matchStart, i);
               if (trim) {
                  element = element.trim();
               }

               if (quotedPrintable) {
                  element = decodeQuotedPrintable(element, quotedPrintableCharset);
                  if (parseFieldDivider) {
                     element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
                  }
               } else {
                  if (parseFieldDivider) {
                     element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").trim();
                  }

                  element = CR_LF_SPACE_TAB.matcher(element).replaceAll("");
                  element = NEWLINE_ESCAPE.matcher(element).replaceAll("\n");
                  element = VCARD_ESCAPES.matcher(element).replaceAll("$1");
               }

               if ("uri".equals(valueType)) {
                  try {
                     element = URI.create(element).getSchemeSpecificPart();
                  } catch (IllegalArgumentException var20) {
                  }
               }

               if (metadata == null) {
                  List<String> match = new ArrayList<>(1);
                  match.add(element);
                  matches.add(match);
               } else {
                  metadata.add(0, element);
                  matches.add(metadata);
               }

               i++;
            } else {
               i++;
            }
            break;
         }
      }

      return matches;
   }

   private static String decodeQuotedPrintable(CharSequence value, String charset) {
      int length = value.length();
      StringBuilder result = new StringBuilder(length);
      ByteArrayOutputStream fragmentBuffer = new ByteArrayOutputStream();

      for (int i = 0; i < length; i++) {
         char c = value.charAt(i);
         switch (c) {
            case '\n':
            case '\r':
               break;
            case '=':
               if (i < length - 2) {
                  char nextChar = value.charAt(i + 1);
                  if (nextChar != '\r' && nextChar != '\n') {
                     char nextNextChar = value.charAt(i + 2);
                     int firstDigit = parseHexDigit(nextChar);
                     int secondDigit = parseHexDigit(nextNextChar);
                     if (firstDigit >= 0 && secondDigit >= 0) {
                        fragmentBuffer.write((firstDigit << 4) + secondDigit);
                     }

                     i += 2;
                  }
               }
               break;
            default:
               maybeAppendFragment(fragmentBuffer, charset, result);
               result.append(c);
         }
      }

      maybeAppendFragment(fragmentBuffer, charset, result);
      return result.toString();
   }

   private static void maybeAppendFragment(ByteArrayOutputStream fragmentBuffer, String charset, StringBuilder result) {
      if (fragmentBuffer.size() > 0) {
         byte[] fragmentBytes = fragmentBuffer.toByteArray();
         String fragment;
         if (charset == null) {
            fragment = new String(fragmentBytes, StandardCharsets.UTF_8);
         } else {
            try {
               fragment = new String(fragmentBytes, charset);
            } catch (UnsupportedEncodingException var6) {
               fragment = new String(fragmentBytes, StandardCharsets.UTF_8);
            }
         }

         fragmentBuffer.reset();
         result.append(fragment);
      }
   }

   static List<String> matchSingleVCardPrefixedField(CharSequence prefix, String rawText, boolean trim, boolean parseFieldDivider) {
      List<List<String>> values = matchVCardPrefixedField(prefix, rawText, trim, parseFieldDivider);
      return values != null && !values.isEmpty() ? values.get(0) : null;
   }

   private static String toPrimaryValue(List<String> list) {
      return list != null && !list.isEmpty() ? list.get(0) : null;
   }

   private static String[] toPrimaryValues(Collection<List<String>> lists) {
      if (lists != null && !lists.isEmpty()) {
         List<String> result = new ArrayList<>(lists.size());

         for (List<String> list : lists) {
            String value = list.get(0);
            if (value != null && !value.isEmpty()) {
               result.add(value);
            }
         }

         return result.toArray(EMPTY_STR_ARRAY);
      } else {
         return null;
      }
   }

   private static String[] toTypes(Collection<List<String>> lists) {
      if (lists != null && !lists.isEmpty()) {
         List<String> result = new ArrayList<>(lists.size());

         for (List<String> list : lists) {
            String value = list.get(0);
            if (value != null && !value.isEmpty()) {
               String type = null;

               for (int i = 1; i < list.size(); i++) {
                  String metadatum = list.get(i);
                  int equals = metadatum.indexOf(61);
                  if (equals < 0) {
                     type = metadatum;
                     break;
                  }

                  if ("TYPE".equalsIgnoreCase(metadatum.substring(0, equals))) {
                     type = metadatum.substring(equals + 1);
                     break;
                  }
               }

               result.add(type);
            }
         }

         return result.toArray(EMPTY_STR_ARRAY);
      } else {
         return null;
      }
   }

   private static boolean isLikeVCardDate(CharSequence value) {
      return value == null || VCARD_LIKE_DATE.matcher(value).matches();
   }

   private static void formatNames(Iterable<List<String>> names) {
      if (names != null) {
         for (List<String> list : names) {
            String name = list.get(0);
            String[] components = new String[5];
            int start = 0;
            int componentIndex = 0;

            int end;
            while (componentIndex < components.length - 1 && (end = name.indexOf(59, start)) >= 0) {
               components[componentIndex] = name.substring(start, end);
               componentIndex++;
               start = end + 1;
            }

            components[componentIndex] = name.substring(start);
            StringBuilder newName = new StringBuilder(100);
            maybeAppendComponent(components, 3, newName);
            maybeAppendComponent(components, 1, newName);
            maybeAppendComponent(components, 2, newName);
            maybeAppendComponent(components, 0, newName);
            maybeAppendComponent(components, 4, newName);
            list.set(0, newName.toString().trim());
         }
      }
   }

   private static void maybeAppendComponent(String[] components, int i, StringBuilder newName) {
      if (components[i] != null && !components[i].isEmpty()) {
         if (newName.length() > 0) {
            newName.append(' ');
         }

         newName.append(components[i]);
      }
   }
}
