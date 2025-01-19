package com.alibaba.fastjson2.stream;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class JSONStreamReaderUTF16<T> extends JSONStreamReader<T> {
   char[] buf;
   final Reader input;
   final JSONReader.Context context;

   JSONStreamReaderUTF16(Reader input, ObjectReaderAdapter objectReader) {
      super(objectReader);
      this.input = input;
      this.context = JSONFactory.createReadContext();
   }

   JSONStreamReaderUTF16(Reader input, Type[] types) {
      super(types);
      this.input = input;
      this.context = JSONFactory.createReadContext();
   }

   @Override
   protected boolean seekLine() throws IOException {
      if (this.buf == null && this.input != null) {
         this.buf = new char[524288];
         int cnt = this.input.read(this.buf);
         if (cnt == -1) {
            this.inputEnd = true;
            return false;
         }

         this.end = cnt;
      }

      int k = 0;

      while (k < 3) {
         this.lineTerminated = false;

         for (int i = this.off; i < this.end; i++) {
            if (i + 4 < this.end) {
               char b0 = this.buf[i];
               char b1 = this.buf[i + 1];
               char b2 = this.buf[i + 2];
               char b3 = this.buf[i + 3];
               if (b0 > '"' && b1 > '"' && b2 > '"' && b3 > '"') {
                  this.lineSize += 4;
                  i += 3;
                  continue;
               }
            }

            char ch = this.buf[i];
            if (ch == '\n') {
               if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
                  this.rowCount++;
               }

               this.lineTerminated = true;
               this.lineSize = 0;
               this.lineEnd = i;
               this.lineStart = this.lineNextStart;
               this.lineNextStart = this.off = i + 1;
               break;
            }

            if (ch == '\r') {
               if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
                  this.rowCount++;
               }

               this.lineTerminated = true;
               this.lineSize = 0;
               this.lineEnd = i;
               int n = i + 1;
               if (n < this.end) {
                  if (this.buf[n] == '\n') {
                     i++;
                  }

                  this.lineStart = this.lineNextStart;
                  this.lineNextStart = this.off = i + 1;
               }
               break;
            }

            this.lineSize++;
         }

         if (!this.lineTerminated) {
            if (this.input != null && !this.inputEnd) {
               int len = this.end - this.off;
               if (this.off > 0) {
                  if (len > 0) {
                     System.arraycopy(this.buf, this.off, this.buf, 0, len);
                  }

                  this.lineStart = this.lineNextStart = 0;
                  this.off = 0;
                  this.end = len;
               }

               int cnt = this.input.read(this.buf, this.end, this.buf.length - this.end);
               if (cnt != -1) {
                  this.end += cnt;
                  k++;
                  continue;
               }

               this.inputEnd = true;
               if (this.off == this.end) {
                  return false;
               }
            }

            this.lineStart = this.lineNextStart;
            this.lineEnd = this.end;
            this.rowCount++;
            this.lineSize = 0;
            this.off = this.end;
         }

         this.lineTerminated = this.off == this.end;
         break;
      }

      return true;
   }

   @Override
   public <T> T readLineObject() {
      try {
         if (this.inputEnd) {
            return null;
         }

         if (this.input == null && this.off >= this.end) {
            return null;
         }

         boolean result = this.seekLine();
         if (!result) {
            return null;
         }
      } catch (IOException var3) {
         throw new JSONException("seekLine error", var3);
      }

      JSONReader reader = JSONReader.of(this.buf, this.lineStart, this.lineEnd - this.lineStart, this.context);
      Object object;
      if (this.objectReader != null) {
         object = this.objectReader.readObject(reader, null, null, this.features);
      } else if (reader.isArray() && this.types != null && this.types.length != 0) {
         object = reader.readList(this.types);
      } else {
         object = reader.readAny();
      }

      return (T)object;
   }
}
