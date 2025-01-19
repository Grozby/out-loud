package com.google.zxing.client.result;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CalendarParsedResult extends ParsedResult {
   private static final Pattern RFC2445_DURATION = Pattern.compile("P(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?");
   private static final long[] RFC2445_DURATION_FIELD_UNITS = new long[]{604800000L, 86400000L, 3600000L, 60000L, 1000L};
   private static final Pattern DATE_TIME = Pattern.compile("[0-9]{8}(T[0-9]{6}Z?)?");
   private final String summary;
   private final long start;
   private final boolean startAllDay;
   private final long end;
   private final boolean endAllDay;
   private final String location;
   private final String organizer;
   private final String[] attendees;
   private final String description;
   private final double latitude;
   private final double longitude;

   public CalendarParsedResult(
      String summary,
      String startString,
      String endString,
      String durationString,
      String location,
      String organizer,
      String[] attendees,
      String description,
      double latitude,
      double longitude
   ) {
      super(ParsedResultType.CALENDAR);
      this.summary = summary;

      try {
         this.start = parseDate(startString);
      } catch (ParseException var16) {
         throw new IllegalArgumentException(var16.toString());
      }

      if (endString == null) {
         long durationMS = parseDurationMS(durationString);
         this.end = durationMS < 0L ? -1L : this.start + durationMS;
      } else {
         try {
            this.end = parseDate(endString);
         } catch (ParseException var15) {
            throw new IllegalArgumentException(var15.toString());
         }
      }

      this.startAllDay = startString.length() == 8;
      this.endAllDay = endString != null && endString.length() == 8;
      this.location = location;
      this.organizer = organizer;
      this.attendees = attendees;
      this.description = description;
      this.latitude = latitude;
      this.longitude = longitude;
   }

   public String getSummary() {
      return this.summary;
   }

   @Deprecated
   public Date getStart() {
      return new Date(this.start);
   }

   public long getStartTimestamp() {
      return this.start;
   }

   public boolean isStartAllDay() {
      return this.startAllDay;
   }

   @Deprecated
   public Date getEnd() {
      return this.end < 0L ? null : new Date(this.end);
   }

   public long getEndTimestamp() {
      return this.end;
   }

   public boolean isEndAllDay() {
      return this.endAllDay;
   }

   public String getLocation() {
      return this.location;
   }

   public String getOrganizer() {
      return this.organizer;
   }

   public String[] getAttendees() {
      return this.attendees;
   }

   public String getDescription() {
      return this.description;
   }

   public double getLatitude() {
      return this.latitude;
   }

   public double getLongitude() {
      return this.longitude;
   }

   @Override
   public String getDisplayResult() {
      StringBuilder result = new StringBuilder(100);
      maybeAppend(this.summary, result);
      maybeAppend(format(this.startAllDay, this.start), result);
      maybeAppend(format(this.endAllDay, this.end), result);
      maybeAppend(this.location, result);
      maybeAppend(this.organizer, result);
      maybeAppend(this.attendees, result);
      maybeAppend(this.description, result);
      return result.toString();
   }

   private static long parseDate(String when) throws ParseException {
      if (!DATE_TIME.matcher(when).matches()) {
         throw new ParseException(when, 0);
      } else if (when.length() == 8) {
         DateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
         format.setTimeZone(TimeZone.getTimeZone("GMT"));
         return format.parse(when).getTime();
      } else if (when.length() == 16 && when.charAt(15) == 'Z') {
         long milliseconds = parseDateTimeString(when.substring(0, 15));
         Calendar calendar = new GregorianCalendar();
         milliseconds += (long)calendar.get(15);
         calendar.setTime(new Date(milliseconds));
         return milliseconds + (long)calendar.get(16);
      } else {
         return parseDateTimeString(when);
      }
   }

   private static String format(boolean allDay, long date) {
      if (date < 0L) {
         return null;
      } else {
         DateFormat format = allDay ? DateFormat.getDateInstance(2) : DateFormat.getDateTimeInstance(2, 2);
         return format.format(date);
      }
   }

   private static long parseDurationMS(CharSequence durationString) {
      if (durationString == null) {
         return -1L;
      } else {
         Matcher m = RFC2445_DURATION.matcher(durationString);
         if (!m.matches()) {
            return -1L;
         } else {
            long durationMS = 0L;

            for (int i = 0; i < RFC2445_DURATION_FIELD_UNITS.length; i++) {
               String fieldValue = m.group(i + 1);
               if (fieldValue != null) {
                  durationMS += RFC2445_DURATION_FIELD_UNITS[i] * (long)Integer.parseInt(fieldValue);
               }
            }

            return durationMS;
         }
      }
   }

   private static long parseDateTimeString(String dateTimeString) throws ParseException {
      DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
      return format.parse(dateTimeString).getTime();
   }
}
