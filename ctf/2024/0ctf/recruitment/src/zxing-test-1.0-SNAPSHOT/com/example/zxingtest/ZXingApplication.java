package com.example.zxingtest;

import com.alibaba.fastjson2.JSON;
import com.example.zxingtest.models.TestResult;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.decoder.QRCodeDecoderMetaData;
import com.google.zxing.qrcode.detector.Detector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;

public class ZXingApplication {
   private static final Decoder decoder = new Decoder();

   public static void main(String[] args) {
      if (args.length != 1) {
         System.out.println("No image");
         System.exit(1);
      }

      String imagePath = args[0];
      File imageFile = new File(imagePath);
      TestResult result = new TestResult();

      try {
         BufferedImage bufferedImage = ImageIO.read(imageFile);
         BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
         result = decode(binaryBitmap);
      } catch (NotFoundException var25) {
         result.setSuccess(false);
         result.setMessage("No qrcode");
      } catch (FormatException var26) {
         result.setSuccess(false);
         result.setMessage("Error decoding");
      } catch (IOException var27) {
         result.setSuccess(false);
         result.setMessage("Error reading");
      } catch (Exception var28) {
         result.setSuccess(false);
         result.setMessage("Unknown error");
      } finally {
         String text = JSON.toJSONString(result);
         String newFileName = imageFile.getName() + ".json";
         Path outputFilePath = imageFile.toPath().resolveSibling(newFileName);

         try {
            Files.writeString(outputFilePath, text, StandardCharsets.UTF_8);
         } catch (IOException var24) {
         }
      }
   }

   public static final TestResult decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
      DetectorResult detectorResult = new Detector(image.getBlackMatrix()).detect(null);
      BitMatrix matrix = detectorResult.getBits().clone();
      DecoderResult decoderResult = decoder.decode(detectorResult.getBits(), null);
      if (decoderResult.getOther() instanceof QRCodeDecoderMetaData) {
         mirror(matrix);
      }

      TestResult result = new TestResult();
      result.setSuccess(true);
      result.setCodeResult(decoderResult.getText());
      result.setCodeBytes(Base64.getEncoder().encodeToString(decoderResult.getRawBytes()));
      result.setMessage("ok");
      result.setCodeMatrix(matrix.toString("1", "0", "\n"));
      return result;
   }

   public static void mirror(BitMatrix bitMatrix) {
      for (int x = 0; x < bitMatrix.getWidth(); x++) {
         for (int y = x + 1; y < bitMatrix.getHeight(); y++) {
            if (bitMatrix.get(x, y) != bitMatrix.get(y, x)) {
               bitMatrix.flip(y, x);
               bitMatrix.flip(x, y);
            }
         }
      }
   }
}
