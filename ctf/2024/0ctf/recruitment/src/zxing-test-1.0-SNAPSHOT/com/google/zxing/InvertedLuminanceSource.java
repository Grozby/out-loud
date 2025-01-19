package com.google.zxing;

public final class InvertedLuminanceSource extends LuminanceSource {
   private final LuminanceSource delegate;

   public InvertedLuminanceSource(LuminanceSource delegate) {
      super(delegate.getWidth(), delegate.getHeight());
      this.delegate = delegate;
   }

   @Override
   public byte[] getRow(int y, byte[] row) {
      row = this.delegate.getRow(y, row);
      int width = this.getWidth();

      for (int i = 0; i < width; i++) {
         row[i] = (byte)(255 - (row[i] & 255));
      }

      return row;
   }

   @Override
   public byte[] getMatrix() {
      byte[] matrix = this.delegate.getMatrix();
      int length = this.getWidth() * this.getHeight();
      byte[] invertedMatrix = new byte[length];

      for (int i = 0; i < length; i++) {
         invertedMatrix[i] = (byte)(255 - (matrix[i] & 255));
      }

      return invertedMatrix;
   }

   @Override
   public boolean isCropSupported() {
      return this.delegate.isCropSupported();
   }

   @Override
   public LuminanceSource crop(int left, int top, int width, int height) {
      return new InvertedLuminanceSource(this.delegate.crop(left, top, width, height));
   }

   @Override
   public boolean isRotateSupported() {
      return this.delegate.isRotateSupported();
   }

   @Override
   public LuminanceSource invert() {
      return this.delegate;
   }

   @Override
   public LuminanceSource rotateCounterClockwise() {
      return new InvertedLuminanceSource(this.delegate.rotateCounterClockwise());
   }

   @Override
   public LuminanceSource rotateCounterClockwise45() {
      return new InvertedLuminanceSource(this.delegate.rotateCounterClockwise45());
   }
}
