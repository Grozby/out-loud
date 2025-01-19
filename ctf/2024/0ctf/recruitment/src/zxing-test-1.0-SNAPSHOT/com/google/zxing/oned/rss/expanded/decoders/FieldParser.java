package com.google.zxing.oned.rss.expanded.decoders;

import com.google.zxing.NotFoundException;
import java.util.HashMap;
import java.util.Map;

final class FieldParser {
   private static final Map<String, FieldParser.DataLength> TWO_DIGIT_DATA_LENGTH = new HashMap<>();
   private static final Map<String, FieldParser.DataLength> THREE_DIGIT_DATA_LENGTH;
   private static final Map<String, FieldParser.DataLength> THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH;
   private static final Map<String, FieldParser.DataLength> FOUR_DIGIT_DATA_LENGTH;

   private FieldParser() {
   }

   static String parseFieldsInGeneralPurpose(String rawInformation) throws NotFoundException {
      if (rawInformation.isEmpty()) {
         return null;
      } else if (rawInformation.length() < 2) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         FieldParser.DataLength twoDigitDataLength = TWO_DIGIT_DATA_LENGTH.get(rawInformation.substring(0, 2));
         if (twoDigitDataLength != null) {
            return twoDigitDataLength.variable
               ? processVariableAI(2, twoDigitDataLength.length, rawInformation)
               : processFixedAI(2, twoDigitDataLength.length, rawInformation);
         } else if (rawInformation.length() < 3) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            String firstThreeDigits = rawInformation.substring(0, 3);
            FieldParser.DataLength threeDigitDataLength = THREE_DIGIT_DATA_LENGTH.get(firstThreeDigits);
            if (threeDigitDataLength != null) {
               return threeDigitDataLength.variable
                  ? processVariableAI(3, threeDigitDataLength.length, rawInformation)
                  : processFixedAI(3, threeDigitDataLength.length, rawInformation);
            } else if (rawInformation.length() < 4) {
               throw NotFoundException.getNotFoundInstance();
            } else {
               FieldParser.DataLength threeDigitPlusDigitDataLength = THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.get(firstThreeDigits);
               if (threeDigitPlusDigitDataLength != null) {
                  return threeDigitPlusDigitDataLength.variable
                     ? processVariableAI(4, threeDigitPlusDigitDataLength.length, rawInformation)
                     : processFixedAI(4, threeDigitPlusDigitDataLength.length, rawInformation);
               } else {
                  FieldParser.DataLength firstFourDigitLength = FOUR_DIGIT_DATA_LENGTH.get(rawInformation.substring(0, 4));
                  if (firstFourDigitLength != null) {
                     return firstFourDigitLength.variable
                        ? processVariableAI(4, firstFourDigitLength.length, rawInformation)
                        : processFixedAI(4, firstFourDigitLength.length, rawInformation);
                  } else {
                     throw NotFoundException.getNotFoundInstance();
                  }
               }
            }
         }
      }
   }

   private static String processFixedAI(int aiSize, int fieldSize, String rawInformation) throws NotFoundException {
      if (rawInformation.length() < aiSize) {
         throw NotFoundException.getNotFoundInstance();
      } else {
         String ai = rawInformation.substring(0, aiSize);
         if (rawInformation.length() < aiSize + fieldSize) {
            throw NotFoundException.getNotFoundInstance();
         } else {
            String field = rawInformation.substring(aiSize, aiSize + fieldSize);
            String remaining = rawInformation.substring(aiSize + fieldSize);
            String result = '(' + ai + ')' + field;
            String parsedAI = parseFieldsInGeneralPurpose(remaining);
            return parsedAI == null ? result : result + parsedAI;
         }
      }
   }

   private static String processVariableAI(int aiSize, int variableFieldSize, String rawInformation) throws NotFoundException {
      String ai = rawInformation.substring(0, aiSize);
      int maxSize = Math.min(rawInformation.length(), aiSize + variableFieldSize);
      String field = rawInformation.substring(aiSize, maxSize);
      String remaining = rawInformation.substring(maxSize);
      String result = '(' + ai + ')' + field;
      String parsedAI = parseFieldsInGeneralPurpose(remaining);
      return parsedAI == null ? result : result + parsedAI;
   }

   static {
      TWO_DIGIT_DATA_LENGTH.put("00", FieldParser.DataLength.fixed(18));
      TWO_DIGIT_DATA_LENGTH.put("01", FieldParser.DataLength.fixed(14));
      TWO_DIGIT_DATA_LENGTH.put("02", FieldParser.DataLength.fixed(14));
      TWO_DIGIT_DATA_LENGTH.put("10", FieldParser.DataLength.variable(20));
      TWO_DIGIT_DATA_LENGTH.put("11", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("12", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("13", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("15", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("16", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("17", FieldParser.DataLength.fixed(6));
      TWO_DIGIT_DATA_LENGTH.put("20", FieldParser.DataLength.fixed(2));
      TWO_DIGIT_DATA_LENGTH.put("21", FieldParser.DataLength.variable(20));
      TWO_DIGIT_DATA_LENGTH.put("22", FieldParser.DataLength.variable(29));
      TWO_DIGIT_DATA_LENGTH.put("30", FieldParser.DataLength.variable(8));
      TWO_DIGIT_DATA_LENGTH.put("37", FieldParser.DataLength.variable(8));

      for (int i = 90; i <= 99; i++) {
         TWO_DIGIT_DATA_LENGTH.put(String.valueOf(i), FieldParser.DataLength.variable(30));
      }

      THREE_DIGIT_DATA_LENGTH = new HashMap<>();
      THREE_DIGIT_DATA_LENGTH.put("235", FieldParser.DataLength.variable(28));
      THREE_DIGIT_DATA_LENGTH.put("240", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("241", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("242", FieldParser.DataLength.variable(6));
      THREE_DIGIT_DATA_LENGTH.put("243", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("250", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("251", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("253", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("254", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("255", FieldParser.DataLength.variable(25));
      THREE_DIGIT_DATA_LENGTH.put("400", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("401", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("402", FieldParser.DataLength.fixed(17));
      THREE_DIGIT_DATA_LENGTH.put("403", FieldParser.DataLength.variable(30));
      THREE_DIGIT_DATA_LENGTH.put("410", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("411", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("412", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("413", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("414", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("415", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("416", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("417", FieldParser.DataLength.fixed(13));
      THREE_DIGIT_DATA_LENGTH.put("420", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("421", FieldParser.DataLength.variable(15));
      THREE_DIGIT_DATA_LENGTH.put("422", FieldParser.DataLength.fixed(3));
      THREE_DIGIT_DATA_LENGTH.put("423", FieldParser.DataLength.variable(15));
      THREE_DIGIT_DATA_LENGTH.put("424", FieldParser.DataLength.fixed(3));
      THREE_DIGIT_DATA_LENGTH.put("425", FieldParser.DataLength.variable(15));
      THREE_DIGIT_DATA_LENGTH.put("426", FieldParser.DataLength.fixed(3));
      THREE_DIGIT_DATA_LENGTH.put("427", FieldParser.DataLength.variable(3));
      THREE_DIGIT_DATA_LENGTH.put("710", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("711", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("712", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("713", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("714", FieldParser.DataLength.variable(20));
      THREE_DIGIT_DATA_LENGTH.put("715", FieldParser.DataLength.variable(20));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH = new HashMap<>();

      for (int i = 310; i <= 316; i++) {
         THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), FieldParser.DataLength.fixed(6));
      }

      for (int i = 320; i <= 337; i++) {
         THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), FieldParser.DataLength.fixed(6));
      }

      for (int i = 340; i <= 357; i++) {
         THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), FieldParser.DataLength.fixed(6));
      }

      for (int i = 360; i <= 369; i++) {
         THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), FieldParser.DataLength.fixed(6));
      }

      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("390", FieldParser.DataLength.variable(15));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("391", FieldParser.DataLength.variable(18));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("392", FieldParser.DataLength.variable(15));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("393", FieldParser.DataLength.variable(18));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("394", FieldParser.DataLength.fixed(4));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("395", FieldParser.DataLength.fixed(6));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("703", FieldParser.DataLength.variable(30));
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("723", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH = new HashMap<>();
      FOUR_DIGIT_DATA_LENGTH.put("4300", FieldParser.DataLength.variable(35));
      FOUR_DIGIT_DATA_LENGTH.put("4301", FieldParser.DataLength.variable(35));
      FOUR_DIGIT_DATA_LENGTH.put("4302", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4303", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4304", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4305", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4306", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4307", FieldParser.DataLength.fixed(2));
      FOUR_DIGIT_DATA_LENGTH.put("4308", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("4309", FieldParser.DataLength.fixed(20));
      FOUR_DIGIT_DATA_LENGTH.put("4310", FieldParser.DataLength.variable(35));
      FOUR_DIGIT_DATA_LENGTH.put("4311", FieldParser.DataLength.variable(35));
      FOUR_DIGIT_DATA_LENGTH.put("4312", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4313", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4314", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4315", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4316", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("4317", FieldParser.DataLength.fixed(2));
      FOUR_DIGIT_DATA_LENGTH.put("4318", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("4319", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("4320", FieldParser.DataLength.variable(35));
      FOUR_DIGIT_DATA_LENGTH.put("4321", FieldParser.DataLength.fixed(1));
      FOUR_DIGIT_DATA_LENGTH.put("4322", FieldParser.DataLength.fixed(1));
      FOUR_DIGIT_DATA_LENGTH.put("4323", FieldParser.DataLength.fixed(1));
      FOUR_DIGIT_DATA_LENGTH.put("4324", FieldParser.DataLength.fixed(10));
      FOUR_DIGIT_DATA_LENGTH.put("4325", FieldParser.DataLength.fixed(10));
      FOUR_DIGIT_DATA_LENGTH.put("4326", FieldParser.DataLength.fixed(6));
      FOUR_DIGIT_DATA_LENGTH.put("7001", FieldParser.DataLength.fixed(13));
      FOUR_DIGIT_DATA_LENGTH.put("7002", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("7003", FieldParser.DataLength.fixed(10));
      FOUR_DIGIT_DATA_LENGTH.put("7004", FieldParser.DataLength.variable(4));
      FOUR_DIGIT_DATA_LENGTH.put("7005", FieldParser.DataLength.variable(12));
      FOUR_DIGIT_DATA_LENGTH.put("7006", FieldParser.DataLength.fixed(6));
      FOUR_DIGIT_DATA_LENGTH.put("7007", FieldParser.DataLength.variable(12));
      FOUR_DIGIT_DATA_LENGTH.put("7008", FieldParser.DataLength.variable(3));
      FOUR_DIGIT_DATA_LENGTH.put("7009", FieldParser.DataLength.variable(10));
      FOUR_DIGIT_DATA_LENGTH.put("7010", FieldParser.DataLength.variable(2));
      FOUR_DIGIT_DATA_LENGTH.put("7011", FieldParser.DataLength.variable(10));
      FOUR_DIGIT_DATA_LENGTH.put("7020", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("7021", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("7022", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("7023", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("7040", FieldParser.DataLength.fixed(4));
      FOUR_DIGIT_DATA_LENGTH.put("7240", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("8001", FieldParser.DataLength.fixed(14));
      FOUR_DIGIT_DATA_LENGTH.put("8002", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("8003", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("8004", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("8005", FieldParser.DataLength.fixed(6));
      FOUR_DIGIT_DATA_LENGTH.put("8006", FieldParser.DataLength.fixed(18));
      FOUR_DIGIT_DATA_LENGTH.put("8007", FieldParser.DataLength.variable(34));
      FOUR_DIGIT_DATA_LENGTH.put("8008", FieldParser.DataLength.variable(12));
      FOUR_DIGIT_DATA_LENGTH.put("8009", FieldParser.DataLength.variable(50));
      FOUR_DIGIT_DATA_LENGTH.put("8010", FieldParser.DataLength.variable(30));
      FOUR_DIGIT_DATA_LENGTH.put("8011", FieldParser.DataLength.variable(12));
      FOUR_DIGIT_DATA_LENGTH.put("8012", FieldParser.DataLength.variable(20));
      FOUR_DIGIT_DATA_LENGTH.put("8013", FieldParser.DataLength.variable(25));
      FOUR_DIGIT_DATA_LENGTH.put("8017", FieldParser.DataLength.fixed(18));
      FOUR_DIGIT_DATA_LENGTH.put("8018", FieldParser.DataLength.fixed(18));
      FOUR_DIGIT_DATA_LENGTH.put("8019", FieldParser.DataLength.variable(10));
      FOUR_DIGIT_DATA_LENGTH.put("8020", FieldParser.DataLength.variable(25));
      FOUR_DIGIT_DATA_LENGTH.put("8026", FieldParser.DataLength.fixed(18));
      FOUR_DIGIT_DATA_LENGTH.put("8100", FieldParser.DataLength.fixed(6));
      FOUR_DIGIT_DATA_LENGTH.put("8101", FieldParser.DataLength.fixed(10));
      FOUR_DIGIT_DATA_LENGTH.put("8102", FieldParser.DataLength.fixed(2));
      FOUR_DIGIT_DATA_LENGTH.put("8110", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("8111", FieldParser.DataLength.fixed(4));
      FOUR_DIGIT_DATA_LENGTH.put("8112", FieldParser.DataLength.variable(70));
      FOUR_DIGIT_DATA_LENGTH.put("8200", FieldParser.DataLength.variable(70));
   }

   private static final class DataLength {
      final boolean variable;
      final int length;

      private DataLength(boolean variable, int length) {
         this.variable = variable;
         this.length = length;
      }

      static FieldParser.DataLength fixed(int length) {
         return new FieldParser.DataLength(false, length);
      }

      static FieldParser.DataLength variable(int length) {
         return new FieldParser.DataLength(true, length);
      }
   }
}
