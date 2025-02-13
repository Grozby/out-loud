package com.google.zxing.client.result;

import com.google.zxing.Result;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeoResultParser extends ResultParser {
   private static final Pattern GEO_URL_PATTERN = Pattern.compile("geo:([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?(.*))?", 2);

   public GeoParsedResult parse(Result result) {
      CharSequence rawText = getMassagedText(result);
      Matcher matcher = GEO_URL_PATTERN.matcher(rawText);
      if (!matcher.matches()) {
         return null;
      } else {
         String query = matcher.group(4);

         double latitude;
         double longitude;
         double altitude;
         try {
            latitude = Double.parseDouble(matcher.group(1));
            if (latitude > 90.0 || latitude < -90.0) {
               return null;
            }

            longitude = Double.parseDouble(matcher.group(2));
            if (longitude > 180.0 || longitude < -180.0) {
               return null;
            }

            if (matcher.group(3) == null) {
               altitude = 0.0;
            } else {
               altitude = Double.parseDouble(matcher.group(3));
               if (altitude < 0.0) {
                  return null;
               }
            }
         } catch (NumberFormatException var12) {
            return null;
         }

         return new GeoParsedResult(latitude, longitude, altitude, query);
      }
   }
}
