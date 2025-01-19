package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ByteArrayValueConsumer;
import com.alibaba.fastjson2.reader.CharArrayValueConsumer;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.stream.StreamReader;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public abstract class CSVReader<T> extends StreamReader<T> implements Closeable {
   boolean quote;
   protected Class<T> objectClass;
   private boolean objectSupport = true;

   CSVReader() {
   }

   CSVReader(Class<T> objectClass) {
      this.objectClass = objectClass;
   }

   public CSVReader(Type[] types) {
      super(types);
      this.objectSupport = false;
   }

   public void config(StreamReader.Feature... features) {
      for (StreamReader.Feature feature : features) {
         this.features = this.features | feature.mask;
      }
   }

   public void config(StreamReader.Feature feature, boolean state) {
      if (state) {
         this.features = this.features | feature.mask;
      } else {
         this.features = this.features & ~feature.mask;
      }
   }

   public static <T> CSVReader<T> of(Reader reader, Class<T> objectClass) {
      return new CSVReaderUTF16<>(reader, objectClass);
   }

   public static <T> CSVReader of(String str, Class<T> objectClass) {
      if (JDKUtils.JVM_VERSION > 8 && JDKUtils.STRING_VALUE != null) {
         try {
            int coder = JDKUtils.STRING_CODER.applyAsInt(str);
            if (coder == 0) {
               byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
               return new CSVReaderUTF8<>(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1, objectClass);
            }
         } catch (Exception var4) {
            throw new JSONException("unsafe get String.coder error");
         }
      }

      char[] chars = JDKUtils.getCharArray(str);
      return new CSVReaderUTF16<>(chars, 0, chars.length, objectClass);
   }

   public static <T> CSVReader<T> of(char[] chars, Class<T> objectClass) {
      return new CSVReaderUTF16<>(chars, 0, chars.length, objectClass);
   }

   public static <T> CSVReader<T> of(byte[] utf8Bytes, Class<T> objectClass) {
      return of(utf8Bytes, 0, utf8Bytes.length, StandardCharsets.UTF_8, objectClass);
   }

   public static CSVReader of(File file, Type... types) throws IOException {
      return new CSVReaderUTF8(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8, types);
   }

   public static CSVReader of(File file, ByteArrayValueConsumer consumer) throws IOException {
      return of(file, StandardCharsets.UTF_8, consumer);
   }

   public static CSVReader of(File file, Charset charset, ByteArrayValueConsumer consumer) throws IOException {
      if (charset != StandardCharsets.UTF_16 && charset != StandardCharsets.UTF_16LE && charset != StandardCharsets.UTF_16BE) {
         return new CSVReaderUTF8(Files.newInputStream(file.toPath()), charset, consumer);
      } else {
         throw new JSONException("not support charset : " + charset);
      }
   }

   public static CSVReader of(File file, CharArrayValueConsumer consumer) throws IOException {
      return of(file, StandardCharsets.UTF_8, consumer);
   }

   public static CSVReader of(File file, Charset charset, CharArrayValueConsumer consumer) throws IOException {
      return new CSVReaderUTF16(new InputStreamReader(Files.newInputStream(file.toPath()), charset), consumer);
   }

   public static CSVReader of(File file, Charset charset, Type... types) throws IOException {
      return (CSVReader)(JDKUtils.JVM_VERSION != 8
            && charset != StandardCharsets.UTF_16
            && charset != StandardCharsets.UTF_16LE
            && charset != StandardCharsets.UTF_16BE
         ? new CSVReaderUTF8(Files.newInputStream(file.toPath()), charset, types)
         : new CSVReaderUTF16(new InputStreamReader(Files.newInputStream(file.toPath()), charset), types));
   }

   public static <T> CSVReader<T> of(File file, Class<T> objectClass) throws IOException {
      return of(file, StandardCharsets.UTF_8, objectClass);
   }

   public static <T> CSVReader<T> of(File file, Charset charset, Class<T> objectClass) throws IOException {
      return (CSVReader<T>)(JDKUtils.JVM_VERSION != 8
            && charset != StandardCharsets.UTF_16
            && charset != StandardCharsets.UTF_16LE
            && charset != StandardCharsets.UTF_16BE
         ? new CSVReaderUTF8<>(Files.newInputStream(file.toPath()), charset, objectClass)
         : new CSVReaderUTF16<>(new InputStreamReader(Files.newInputStream(file.toPath()), charset), objectClass));
   }

   public static CSVReader of(InputStream in, Type... types) throws IOException {
      return of(in, StandardCharsets.UTF_8, types);
   }

   public static <T> CSVReader<T> of(InputStream in, Class<T> objectClass) {
      return of(in, StandardCharsets.UTF_8, objectClass);
   }

   public static <T> CSVReader<T> of(InputStream in, Charset charset, Class<T> objectClass) {
      return (CSVReader<T>)(JDKUtils.JVM_VERSION != 8
            && charset != StandardCharsets.UTF_16
            && charset != StandardCharsets.UTF_16LE
            && charset != StandardCharsets.UTF_16BE
         ? new CSVReaderUTF8<>(in, charset, objectClass)
         : new CSVReaderUTF16<>(new InputStreamReader(in, charset), objectClass));
   }

   public static CSVReader of(InputStream in, Charset charset, Type... types) {
      return (CSVReader)(JDKUtils.JVM_VERSION != 8
            && charset != StandardCharsets.UTF_16
            && charset != StandardCharsets.UTF_16LE
            && charset != StandardCharsets.UTF_16BE
         ? new CSVReaderUTF8(in, charset, types)
         : new CSVReaderUTF16(new InputStreamReader(in, charset), types));
   }

   public static CSVReader of(Reader in, Type... types) {
      return new CSVReaderUTF16(in, types);
   }

   public static CSVReader of(String str, Type... types) {
      if (JDKUtils.JVM_VERSION > 8 && JDKUtils.STRING_VALUE != null) {
         try {
            int coder = JDKUtils.STRING_CODER.applyAsInt(str);
            if (coder == 0) {
               byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
               return new CSVReaderUTF8(bytes, 0, bytes.length, types);
            }
         } catch (Exception var4) {
            throw new JSONException("unsafe get String.coder error");
         }
      }

      char[] chars = JDKUtils.getCharArray(str);
      return new CSVReaderUTF16(chars, 0, chars.length, types);
   }

   public static CSVReader of(char[] chars, Type... types) {
      return new CSVReaderUTF16(chars, 0, chars.length, types);
   }

   public static <T> CSVReader<T> of(char[] chars, int off, int len, CharArrayValueConsumer consumer) {
      return new CSVReaderUTF16<>(chars, off, len, consumer);
   }

   public static CSVReader of(byte[] utf8Bytes, Type... types) {
      return new CSVReaderUTF8(utf8Bytes, 0, utf8Bytes.length, types);
   }

   public static CSVReader of(byte[] utf8Bytes, ByteArrayValueConsumer consumer) {
      return of(utf8Bytes, 0, utf8Bytes.length, StandardCharsets.UTF_8, consumer);
   }

   public static <T> CSVReader<T> of(byte[] utf8Bytes, int off, int len, Charset charset, ByteArrayValueConsumer consumer) {
      return new CSVReaderUTF8<>(utf8Bytes, off, len, charset, consumer);
   }

   public static <T> CSVReader<T> of(byte[] utf8Bytes, Charset charset, Class<T> objectClass) {
      return of(utf8Bytes, 0, utf8Bytes.length, charset, objectClass);
   }

   public static <T> CSVReader<T> of(byte[] utf8Bytes, int off, int len, Class<T> objectClass) {
      return new CSVReaderUTF8<>(utf8Bytes, off, len, StandardCharsets.UTF_8, objectClass);
   }

   public static <T> CSVReader<T> of(byte[] utf8Bytes, int off, int len, Charset charset, Class<T> objectClass) {
      if (charset != StandardCharsets.UTF_16 && charset != StandardCharsets.UTF_16LE && charset != StandardCharsets.UTF_16BE) {
         return new CSVReaderUTF8<>(utf8Bytes, off, len, charset, objectClass);
      } else {
         char[] chars = new char[len];
         int size = IOUtils.decodeUTF8(utf8Bytes, off, len, chars);
         return new CSVReaderUTF16<>(chars, 0, size, objectClass);
      }
   }

   public static <T> CSVReader<T> of(char[] utf8Bytes, int off, int len, Class<T> objectClass) {
      return new CSVReaderUTF16<>(utf8Bytes, off, len, objectClass);
   }

   public void skipLines(int lines) throws IOException {
      if (lines < 0) {
         throw new IllegalArgumentException();
      } else {
         for (int i = 0; i < lines; i++) {
            this.seekLine();
         }
      }
   }

   public List<String> readHeader() {
      this.objectSupport = true;
      String[] columns = (String[])this.readLineValues(true);
      if (this.objectClass != null) {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         boolean fieldBased = (this.features & JSONReader.Feature.FieldBased.mask) != 0L;
         Type[] types = new Type[columns.length];
         ObjectReader[] typeReaders = new ObjectReader[columns.length];
         FieldReader[] fieldReaders = new FieldReader[columns.length];

         for (int i = 0; i < columns.length; i++) {
            String column = columns[i].trim();
            FieldReader fieldReader = provider.createFieldReader(this.objectClass, column, this.features);
            if (fieldReader != null) {
               fieldReaders[i] = fieldReader;
               Type fieldType = fieldReader.fieldType;
               if (fieldType instanceof Class) {
                  Class fieldClass = (Class)fieldType;
                  if (fieldClass.isPrimitive()) {
                     fieldType = TypeUtils.nonePrimitive((Class)fieldType);
                  }
               }

               types[i] = fieldType;
               typeReaders[i] = provider.getObjectReader(fieldType, fieldBased);
            } else {
               types[i] = String.class;
            }
         }

         this.types = types;
         this.typeReaders = typeReaders;
         this.fieldReaders = fieldReaders;
         this.objectCreator = provider.createObjectCreator(this.objectClass, this.features);
      }

      this.columns = Arrays.asList(columns);
      this.columnStats = new ArrayList<>();
      IntStream.range(0, columns.length).forEach(ix -> this.columnStats.add(new StreamReader.ColumnStat(columns[ix])));
      if (this.rowCount == 1) {
         this.rowCount = this.lineTerminated ? 0 : -1;
      }

      return this.columns;
   }

   public List<String> getColumns() {
      return this.columns;
   }

   public String getColumn(int columnIndex) {
      return this.columns != null && columnIndex < this.columns.size() ? this.columns.get(columnIndex) : null;
   }

   public Type getColumnType(int columnIndex) {
      return this.types != null && columnIndex < this.types.length ? this.types[columnIndex] : null;
   }

   public List<StreamReader.ColumnStat> getColumnStats() {
      return this.columnStats;
   }

   public void readLineObjectAll(Consumer<T> consumer) {
      this.readLineObjectAll(true, consumer);
   }

   public abstract void readLineObjectAll(boolean var1, Consumer<T> var2);

   @Override
   public T readLineObject() {
      if (!this.objectSupport) {
         throw new UnsupportedOperationException("this method should not be called, try specify objectClass or method readLineValues instead ?");
      } else if (this.inputEnd) {
         return null;
      } else {
         if (this.fieldReaders == null) {
            ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
            if (this.objectClass == null) {
               throw new JSONException("not support operation, objectClass is null");
            }

            boolean fieldBased = (this.features & JSONReader.Feature.FieldBased.mask) != 0L;
            ObjectReader objectReader = provider.getObjectReader(this.objectClass, fieldBased);
            if (!(objectReader instanceof ObjectReaderAdapter)) {
               throw new JSONException("not support operation : " + this.objectClass);
            }

            this.fieldReaders = ((ObjectReaderAdapter)objectReader).getFieldReaders();
            this.types = new Type[this.fieldReaders.length];

            for (int i = 0; i < this.types.length; i++) {
               this.types[i] = this.fieldReaders[i].fieldType;
            }

            this.objectCreator = provider.createObjectCreator(this.objectClass, this.features);
         }

         if (this.objectCreator == null) {
            throw new JSONException("not support operation, objectClass is null");
         } else {
            Object[] values = this.readLineValues(false);
            if (values == null) {
               return null;
            } else if (this.fieldReaders != null) {
               Object object = this.objectCreator.get();

               for (int i = 0; i < this.fieldReaders.length; i++) {
                  FieldReader fieldReader = this.fieldReaders[i];
                  if (fieldReader != null) {
                     fieldReader.accept(object, values[i]);
                  }
               }

               return (T)object;
            } else {
               throw new JSONException("not support operation, objectClass is null");
            }
         }
      }
   }

   public abstract boolean isEnd();

   public final Object[] readLineValues() {
      return this.readLineValues(false);
   }

   protected abstract Object[] readLineValues(boolean var1);

   public final String[] readLine() {
      return (String[])this.readLineValues(true);
   }

   public static int rowCount(String str, StreamReader.Feature... features) {
      CSVReader state = new CSVReaderUTF8(features);
      state.rowCount(str, str.length());
      return state.rowCount();
   }

   public static int rowCount(byte[] bytes, StreamReader.Feature... features) {
      CSVReaderUTF8 state = new CSVReaderUTF8(features);
      state.rowCount(bytes, bytes.length);
      return state.rowCount();
   }

   public static int rowCount(char[] chars, StreamReader.Feature... features) {
      CSVReaderUTF16 state = new CSVReaderUTF16(features);
      state.rowCount(chars, chars.length);
      return state.rowCount();
   }

   public static int rowCount(File file) throws IOException {
      if (!file.exists()) {
         return -1;
      } else {
         FileInputStream in = new FileInputStream(file);

         int var2;
         try {
            var2 = rowCount(in);
         } catch (Throwable var5) {
            try {
               in.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         in.close();
         return var2;
      }
   }

   public static int rowCount(InputStream in) throws IOException {
      byte[] bytes = new byte[524288];
      CSVReaderUTF8 state = new CSVReaderUTF8();

      while (true) {
         int cnt = in.read(bytes);
         if (cnt == -1) {
            return state.rowCount();
         }

         state.rowCount(bytes, cnt);
      }
   }

   public int errorCount() {
      return this.errorCount;
   }

   public int rowCount() {
      return this.lineTerminated ? this.rowCount : this.rowCount + 1;
   }

   void rowCount(String bytes, int length) {
      this.lineTerminated = false;

      for (int i = 0; i < length; i++) {
         char ch = bytes.charAt(i);
         if (ch == '"') {
            this.lineSize++;
            if (!this.quote) {
               this.quote = true;
            } else {
               int n = i + 1;
               if (n >= length) {
                  break;
               }

               char next = bytes.charAt(n);
               if (next == '"') {
                  i++;
               } else {
                  this.quote = false;
               }
            }
         } else if (this.quote) {
            this.lineSize++;
         } else if (ch == '\n') {
            if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
               this.rowCount++;
               this.lineSize = 0;
            }

            this.lineTerminated = i + 1 == length;
         } else if (ch == '\r') {
            this.lineTerminated = true;
            if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
               this.rowCount++;
            }

            this.lineSize = 0;
            int nx = i + 1;
            if (nx >= length) {
               break;
            }

            char next = bytes.charAt(nx);
            if (next == '\n') {
               i++;
            }

            this.lineTerminated = i + 1 == length;
         } else {
            this.lineSize++;
         }
      }
   }

   void rowCount(byte[] bytes, int length) {
      this.lineTerminated = false;

      for (int i = 0; i < length; i++) {
         if (i + 4 < length) {
            byte b0 = bytes[i];
            byte b1 = bytes[i + 1];
            byte b2 = bytes[i + 2];
            byte b3 = bytes[i + 3];
            if (b0 > 34 && b1 > 34 && b2 > 34 && b3 > 34) {
               this.lineSize += 4;
               i += 3;
               continue;
            }
         }

         byte ch = bytes[i];
         if (ch == 34) {
            this.lineSize++;
            if (!this.quote) {
               this.quote = true;
            } else {
               int n = i + 1;
               if (n >= length) {
                  break;
               }

               byte next = bytes[n];
               if (next == 34) {
                  i++;
               } else {
                  this.quote = false;
               }
            }
         } else if (this.quote) {
            this.lineSize++;
         } else if (ch == 10) {
            if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
               this.rowCount++;
            }

            this.lineSize = 0;
            this.lineTerminated = i + 1 == length;
         } else if (ch == 13) {
            if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
               this.rowCount++;
            }

            this.lineTerminated = true;
            this.lineSize = 0;
            int nx = i + 1;
            if (nx >= length) {
               break;
            }

            byte next = bytes[nx];
            if (next == 10) {
               i++;
            }

            this.lineTerminated = i + 1 == length;
         } else {
            this.lineSize++;
         }
      }
   }

   void rowCount(char[] bytes, int length) {
      this.lineTerminated = false;

      for (int i = 0; i < length; i++) {
         if (i + 4 < length) {
            char b0 = bytes[i];
            char b1 = bytes[i + 1];
            char b2 = bytes[i + 2];
            char b3 = bytes[i + 3];
            if (b0 > '"' && b1 > '"' && b2 > '"' && b3 > '"') {
               i += 3;
               this.lineSize += 4;
               continue;
            }
         }

         char ch = bytes[i];
         if (ch == '"') {
            this.lineSize++;
            if (!this.quote) {
               this.quote = true;
            } else {
               int n = i + 1;
               if (n >= length) {
                  break;
               }

               char next = bytes[n];
               if (next == '"') {
                  i++;
               } else {
                  this.quote = false;
               }
            }
         } else if (this.quote) {
            this.lineSize++;
         } else if (ch == '\n') {
            if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
               this.rowCount++;
            }

            this.lineSize = 0;
            this.lineTerminated = i + 1 == length;
         } else if (ch != '\r' && (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) != 0L) {
            this.lineSize++;
         } else {
            if (this.lineSize > 0) {
               this.rowCount++;
            }

            this.lineTerminated = true;
            this.lineSize = 0;
            int nx = i + 1;
            if (nx >= length) {
               break;
            }

            char next = bytes[nx];
            if (next == '\n') {
               i++;
            }

            this.lineTerminated = i + 1 == length;
         }
      }
   }

   protected Object error(int columnIndex, Exception e) {
      this.errorCount++;
      this.getColumnStat(columnIndex).errors++;
      if ((this.features & StreamReader.Feature.ErrorAsNull.mask) != 0L) {
         return null;
      } else {
         String message = "read csv error, line " + this.rowCount + ", column ";
         String column = null;
         if (this.columns != null && columnIndex < this.columns.size()) {
            column = this.columns.get(columnIndex);
         }

         if (column != null && !column.isEmpty()) {
            message = message + column;
         } else {
            message = message + columnIndex;
         }

         throw new JSONException(message, e);
      }
   }

   public StreamReader.ColumnStat getColumnStat(String name) {
      if (this.columnStats != null) {
         for (StreamReader.ColumnStat stat : this.columnStats) {
            if (name.equals(stat.name)) {
               return stat;
            }
         }
      }

      return null;
   }

   public StreamReader.ColumnStat getColumnStat(int i) {
      if (this.columnStats == null) {
         this.columnStats = new ArrayList<>();
      }

      StreamReader.ColumnStat stat = null;
      if (i >= this.columnStats.size()) {
         for (int j = this.columnStats.size(); j <= i; j++) {
            String column = null;
            if (this.columns != null && i < this.columns.size()) {
               column = this.columns.get(i);
            }

            stat = new StreamReader.ColumnStat(column);
            this.columnStats.add(stat);
         }
      } else {
         stat = this.columnStats.get(i);
      }

      return stat;
   }

   public List<String[]> readLineAll() {
      List<String[]> lines = new ArrayList<>();

      while (true) {
         String[] line = this.readLine();
         if (line == null) {
            return lines;
         }

         lines.add(line);
      }
   }

   public List<T> readLineObjectAll() {
      List<T> objects = new ArrayList<>();

      while (true) {
         T object = this.readLineObject();
         if (object == null) {
            return objects;
         }

         objects.add(object);
      }
   }

   public boolean isObjectSupport() {
      return this.objectSupport;
   }

   public abstract void statAll();

   public abstract void statAll(int var1);

   public abstract void readAll();

   public abstract void readAll(int var1);
}
