package com.example.zxingtest.models;

public class TestResult {
   private boolean success;
   private String message;
   private String codeBytes;
   private String codeResult;
   private String codeMatrix;

   public boolean isSuccess() {
      return this.success;
   }

   public void setSuccess(boolean success) {
      this.success = success;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getCodeBytes() {
      return this.codeBytes;
   }

   public void setCodeBytes(String codeBytes) {
      this.codeBytes = codeBytes;
   }

   public String getCodeResult() {
      return this.codeResult;
   }

   public void setCodeResult(String codeResult) {
      this.codeResult = codeResult;
   }

   public String getCodeMatrix() {
      return this.codeMatrix;
   }

   public void setCodeMatrix(String codeMatrix) {
      this.codeMatrix = codeMatrix;
   }
}
