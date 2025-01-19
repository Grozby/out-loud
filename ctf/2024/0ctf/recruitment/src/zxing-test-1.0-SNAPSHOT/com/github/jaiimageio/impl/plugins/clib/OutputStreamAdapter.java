package com.github.jaiimageio.impl.plugins.clib;

import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.stream.ImageOutputStream;

public final class OutputStreamAdapter extends OutputStream {
   ImageOutputStream stream;

   public OutputStreamAdapter(ImageOutputStream stream) {
      this.stream = stream;
   }

   @Override
   public void close() throws IOException {
      this.stream.close();
   }

   @Override
   public void write(byte[] b) throws IOException {
      this.stream.write(b);
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      this.stream.write(b, off, len);
   }

   @Override
   public void write(int b) throws IOException {
      this.stream.write(b);
   }
}
