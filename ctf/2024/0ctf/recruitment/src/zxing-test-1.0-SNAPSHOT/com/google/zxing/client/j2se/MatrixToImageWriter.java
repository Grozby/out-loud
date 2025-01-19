package com.google.zxing.client.j2se;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class MatrixToImageWriter {
   private static final MatrixToImageConfig DEFAULT_CONFIG = new MatrixToImageConfig();

   private MatrixToImageWriter() {
   }

   public static BufferedImage toBufferedImage(BitMatrix matrix) {
      return toBufferedImage(matrix, DEFAULT_CONFIG);
   }

   public static BufferedImage toBufferedImage(BitMatrix matrix, MatrixToImageConfig config) {
      int width = matrix.getWidth();
      int height = matrix.getHeight();
      BufferedImage image = new BufferedImage(width, height, config.getBufferedImageColorModel());
      int onColor = config.getPixelOnColor();
      int offColor = config.getPixelOffColor();
      int[] rowPixels = new int[width];
      BitArray row = new BitArray(width);

      for (int y = 0; y < height; y++) {
         row = matrix.getRow(y, row);

         for (int x = 0; x < width; x++) {
            rowPixels[x] = row.get(x) ? onColor : offColor;
         }

         image.setRGB(0, y, width, 1, rowPixels, 0, width);
      }

      return image;
   }

   @Deprecated
   public static void writeToFile(BitMatrix matrix, String format, File file) throws IOException {
      writeToPath(matrix, format, file.toPath());
   }

   public static void writeToPath(BitMatrix matrix, String format, Path file) throws IOException {
      writeToPath(matrix, format, file, DEFAULT_CONFIG);
   }

   @Deprecated
   public static void writeToFile(BitMatrix matrix, String format, File file, MatrixToImageConfig config) throws IOException {
      writeToPath(matrix, format, file.toPath(), config);
   }

   public static void writeToPath(BitMatrix matrix, String format, Path file, MatrixToImageConfig config) throws IOException {
      BufferedImage image = toBufferedImage(matrix, config);
      if (!ImageIO.write(image, format, file.toFile())) {
         throw new IOException("Could not write an image of format " + format + " to " + file);
      }
   }

   public static void writeToStream(BitMatrix matrix, String format, OutputStream stream) throws IOException {
      writeToStream(matrix, format, stream, DEFAULT_CONFIG);
   }

   public static void writeToStream(BitMatrix matrix, String format, OutputStream stream, MatrixToImageConfig config) throws IOException {
      BufferedImage image = toBufferedImage(matrix, config);
      if (!ImageIO.write(image, format, stream)) {
         throw new IOException("Could not write an image of format " + format);
      }
   }
}
