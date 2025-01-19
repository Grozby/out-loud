package com.google.zxing.client.result;

import java.util.Map;
import java.util.Objects;

public final class ExpandedProductParsedResult extends ParsedResult {
   public static final String KILOGRAM = "KG";
   public static final String POUND = "LB";
   private final String rawText;
   private final String productID;
   private final String sscc;
   private final String lotNumber;
   private final String productionDate;
   private final String packagingDate;
   private final String bestBeforeDate;
   private final String expirationDate;
   private final String weight;
   private final String weightType;
   private final String weightIncrement;
   private final String price;
   private final String priceIncrement;
   private final String priceCurrency;
   private final Map<String, String> uncommonAIs;

   public ExpandedProductParsedResult(
      String rawText,
      String productID,
      String sscc,
      String lotNumber,
      String productionDate,
      String packagingDate,
      String bestBeforeDate,
      String expirationDate,
      String weight,
      String weightType,
      String weightIncrement,
      String price,
      String priceIncrement,
      String priceCurrency,
      Map<String, String> uncommonAIs
   ) {
      super(ParsedResultType.PRODUCT);
      this.rawText = rawText;
      this.productID = productID;
      this.sscc = sscc;
      this.lotNumber = lotNumber;
      this.productionDate = productionDate;
      this.packagingDate = packagingDate;
      this.bestBeforeDate = bestBeforeDate;
      this.expirationDate = expirationDate;
      this.weight = weight;
      this.weightType = weightType;
      this.weightIncrement = weightIncrement;
      this.price = price;
      this.priceIncrement = priceIncrement;
      this.priceCurrency = priceCurrency;
      this.uncommonAIs = uncommonAIs;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof ExpandedProductParsedResult)) {
         return false;
      } else {
         ExpandedProductParsedResult other = (ExpandedProductParsedResult)o;
         return Objects.equals(this.productID, other.productID)
            && Objects.equals(this.sscc, other.sscc)
            && Objects.equals(this.lotNumber, other.lotNumber)
            && Objects.equals(this.productionDate, other.productionDate)
            && Objects.equals(this.bestBeforeDate, other.bestBeforeDate)
            && Objects.equals(this.expirationDate, other.expirationDate)
            && Objects.equals(this.weight, other.weight)
            && Objects.equals(this.weightType, other.weightType)
            && Objects.equals(this.weightIncrement, other.weightIncrement)
            && Objects.equals(this.price, other.price)
            && Objects.equals(this.priceIncrement, other.priceIncrement)
            && Objects.equals(this.priceCurrency, other.priceCurrency)
            && Objects.equals(this.uncommonAIs, other.uncommonAIs);
      }
   }

   @Override
   public int hashCode() {
      int hash = Objects.hashCode(this.productID);
      hash ^= Objects.hashCode(this.sscc);
      hash ^= Objects.hashCode(this.lotNumber);
      hash ^= Objects.hashCode(this.productionDate);
      hash ^= Objects.hashCode(this.bestBeforeDate);
      hash ^= Objects.hashCode(this.expirationDate);
      hash ^= Objects.hashCode(this.weight);
      hash ^= Objects.hashCode(this.weightType);
      hash ^= Objects.hashCode(this.weightIncrement);
      hash ^= Objects.hashCode(this.price);
      hash ^= Objects.hashCode(this.priceIncrement);
      hash ^= Objects.hashCode(this.priceCurrency);
      return hash ^ Objects.hashCode(this.uncommonAIs);
   }

   public String getRawText() {
      return this.rawText;
   }

   public String getProductID() {
      return this.productID;
   }

   public String getSscc() {
      return this.sscc;
   }

   public String getLotNumber() {
      return this.lotNumber;
   }

   public String getProductionDate() {
      return this.productionDate;
   }

   public String getPackagingDate() {
      return this.packagingDate;
   }

   public String getBestBeforeDate() {
      return this.bestBeforeDate;
   }

   public String getExpirationDate() {
      return this.expirationDate;
   }

   public String getWeight() {
      return this.weight;
   }

   public String getWeightType() {
      return this.weightType;
   }

   public String getWeightIncrement() {
      return this.weightIncrement;
   }

   public String getPrice() {
      return this.price;
   }

   public String getPriceIncrement() {
      return this.priceIncrement;
   }

   public String getPriceCurrency() {
      return this.priceCurrency;
   }

   public Map<String, String> getUncommonAIs() {
      return this.uncommonAIs;
   }

   @Override
   public String getDisplayResult() {
      return String.valueOf(this.rawText);
   }
}
