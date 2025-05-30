package com.google.zxing.client.result;

public final class VINParsedResult extends ParsedResult {
   private final String vin;
   private final String worldManufacturerID;
   private final String vehicleDescriptorSection;
   private final String vehicleIdentifierSection;
   private final String countryCode;
   private final String vehicleAttributes;
   private final int modelYear;
   private final char plantCode;
   private final String sequentialNumber;

   public VINParsedResult(
      String vin,
      String worldManufacturerID,
      String vehicleDescriptorSection,
      String vehicleIdentifierSection,
      String countryCode,
      String vehicleAttributes,
      int modelYear,
      char plantCode,
      String sequentialNumber
   ) {
      super(ParsedResultType.VIN);
      this.vin = vin;
      this.worldManufacturerID = worldManufacturerID;
      this.vehicleDescriptorSection = vehicleDescriptorSection;
      this.vehicleIdentifierSection = vehicleIdentifierSection;
      this.countryCode = countryCode;
      this.vehicleAttributes = vehicleAttributes;
      this.modelYear = modelYear;
      this.plantCode = plantCode;
      this.sequentialNumber = sequentialNumber;
   }

   public String getVIN() {
      return this.vin;
   }

   public String getWorldManufacturerID() {
      return this.worldManufacturerID;
   }

   public String getVehicleDescriptorSection() {
      return this.vehicleDescriptorSection;
   }

   public String getVehicleIdentifierSection() {
      return this.vehicleIdentifierSection;
   }

   public String getCountryCode() {
      return this.countryCode;
   }

   public String getVehicleAttributes() {
      return this.vehicleAttributes;
   }

   public int getModelYear() {
      return this.modelYear;
   }

   public char getPlantCode() {
      return this.plantCode;
   }

   public String getSequentialNumber() {
      return this.sequentialNumber;
   }

   @Override
   public String getDisplayResult() {
      StringBuilder result = new StringBuilder(50);
      result.append(this.worldManufacturerID).append(' ');
      result.append(this.vehicleDescriptorSection).append(' ');
      result.append(this.vehicleIdentifierSection).append('\n');
      if (this.countryCode != null) {
         result.append(this.countryCode).append(' ');
      }

      result.append(this.modelYear).append(' ');
      result.append(this.plantCode).append(' ');
      result.append(this.sequentialNumber).append('\n');
      return result.toString();
   }
}
