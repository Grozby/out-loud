package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.CharArrayValueConsumer;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.stream.StreamReader;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

final class CSVReaderUTF16<T> extends CSVReader<T> {
   static final Map<Long, Function<Consumer, CharArrayValueConsumer>> valueConsumerCreators = new ConcurrentHashMap<>();
   CharArrayValueConsumer valueConsumer;
   char[] buf;
   Reader input;

   CSVReaderUTF16(StreamReader.Feature... features) {
      for (StreamReader.Feature feature : features) {
         this.features = this.features | feature.mask;
      }
   }

   CSVReaderUTF16(Reader input, Class<T> objectClass) {
      super(objectClass);
      this.input = input;
   }

   CSVReaderUTF16(Reader input, CharArrayValueConsumer valueConsumer) {
      this.valueConsumer = valueConsumer;
      this.input = input;
   }

   CSVReaderUTF16(Reader input, Type[] types) {
      super(types);
      this.input = input;
   }

   CSVReaderUTF16(char[] bytes, int off, int len, Class<T> objectClass) {
      super(objectClass);
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
   }

   CSVReaderUTF16(char[] bytes, int off, int len, CharArrayValueConsumer valueConsumer) {
      this.valueConsumer = valueConsumer;
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
   }

   CSVReaderUTF16(char[] bytes, int off, int len, Type[] types) {
      super(types);
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
   }

   @Override
   protected boolean seekLine() throws IOException {
      char[] buf = this.buf;
      int off = this.off;
      if (buf == null && this.input != null) {
         buf = this.buf = new char[524288];
         int cnt = this.input.read(buf);
         if (cnt == -1) {
            this.inputEnd = true;
            return false;
         }

         this.end = cnt;
      }

      int k = 0;

      while (k < 3) {
         this.lineTerminated = false;

         for (int i = off; i < this.end; i++) {
            if (i + 4 < this.end) {
               char b0 = buf[i];
               char b1 = buf[i + 1];
               char b2 = buf[i + 2];
               char b3 = buf[i + 3];
               if (b0 > '"' && b1 > '"' && b2 > '"' && b3 > '"') {
                  this.lineSize += 4;
                  i += 3;
                  continue;
               }
            }

            char ch = buf[i];
            if (ch == '"') {
               this.lineSize++;
               if (!this.quote) {
                  this.quote = true;
               } else {
                  int n = i + 1;
                  if (n >= this.end) {
                     break;
                  }

                  if (buf[n] == '"') {
                     this.lineSize++;
                     i++;
                  } else {
                     this.quote = false;
                  }
               }
            } else if (this.quote) {
               this.lineSize++;
            } else {
               if (ch == '\n') {
                  if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
                     this.rowCount++;
                  }

                  this.lineTerminated = true;
                  this.lineSize = 0;
                  this.lineEnd = i;
                  this.lineStart = this.lineNextStart;
                  this.lineNextStart = off = i + 1;
                  break;
               }

               if (ch == '\r') {
                  if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
                     this.rowCount++;
                  }

                  this.lineTerminated = true;
                  this.lineSize = 0;
                  this.lineEnd = i;
                  int nx = i + 1;
                  if (nx < this.end) {
                     if (buf[nx] == '\n') {
                        i++;
                     }

                     this.lineStart = this.lineNextStart;
                     this.lineNextStart = off = i + 1;
                  }
                  break;
               }

               this.lineSize++;
            }
         }

         if (!this.lineTerminated) {
            if (this.input != null && !this.inputEnd) {
               int len = this.end - off;
               if (off > 0) {
                  if (len > 0) {
                     System.arraycopy(buf, off, buf, 0, len);
                  }

                  this.lineStart = this.lineNextStart = 0;
                  off = 0;
                  this.end = len;
                  this.quote = false;
               }

               int cnt = this.input.read(buf, this.end, buf.length - this.end);
               if (cnt != -1) {
                  this.end += cnt;
                  k++;
                  continue;
               }

               this.inputEnd = true;
               if (off == this.end) {
                  this.off = off;
                  return false;
               }
            }

            this.lineStart = this.lineNextStart;
            this.lineEnd = this.end;
            this.rowCount++;
            this.lineSize = 0;
            off = this.end;
         }

         this.lineTerminated = off == this.end;
         break;
      }

      this.off = off;
      return true;
   }

   Object readValue(char[] chars, int off, int len, Type type) {
      if (len == 0) {
         return null;
      } else if (type == Integer.class) {
         return TypeUtils.parseInt(chars, off, len);
      } else if (type == Long.class) {
         return TypeUtils.parseLong(chars, off, len);
      } else if (type == BigDecimal.class) {
         return TypeUtils.parseBigDecimal(chars, off, len);
      } else if (type == Float.class) {
         return TypeUtils.parseFloat(chars, off, len);
      } else if (type == Double.class) {
         return TypeUtils.parseDouble(chars, off, len);
      } else if (type == Date.class) {
         long millis = DateUtils.parseMillis(chars, off, len, DateUtils.DEFAULT_ZONE_ID);
         return new Date(millis);
      } else if (type == Boolean.class) {
         return TypeUtils.parseBoolean(chars, off, len);
      } else {
         String str = new String(chars, off, len);
         return TypeUtils.cast(str, type);
      }
   }

   @Override
   public boolean isEnd() {
      return this.inputEnd;
   }

   @Override
   public Object[] readLineValues(boolean strings) {
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
      } catch (IOException var23) {
         throw new JSONException("seekLine error", var23);
      }

      Object[] values = null;
      List<Object> valueList = null;
      if (this.columns != null) {
         if (strings) {
            values = new String[this.columns.size()];
         } else {
            values = new Object[this.columns.size()];
         }
      }

      boolean quote = false;
      int valueStart = this.lineStart;
      int valueSize = 0;
      int escapeCount = 0;
      int columnIndex = 0;

      for (int i = this.lineStart; i < this.lineEnd; i++) {
         char ch = this.buf[i];
         if (quote) {
            if (ch != '"') {
               valueSize++;
               continue;
            }

            int n = i + 1;
            if (n < this.lineEnd) {
               char c1 = this.buf[n];
               if (c1 == '"') {
                  valueSize += 2;
                  escapeCount++;
                  i++;
                  continue;
               }

               if (c1 == ',') {
                  i++;
                  ch = c1;
               }
            } else if (n == this.lineEnd) {
               break;
            }
         } else if (ch == '"') {
            quote = true;
            continue;
         }

         if (ch == ',') {
            Type type = this.types != null && columnIndex < this.types.length ? this.types[columnIndex] : null;
            Object value;
            if (!quote) {
               if (type != null && type != String.class && type != Object.class && !strings) {
                  try {
                     value = this.readValue(this.buf, valueStart, valueSize, type);
                  } catch (Exception var20) {
                     value = this.error(columnIndex, var20);
                  }
               } else if (valueSize == 1) {
                  value = TypeUtils.toString(this.buf[valueStart]);
               } else if (valueSize == 2) {
                  value = TypeUtils.toString(this.buf[valueStart], this.buf[valueStart + 1]);
               } else {
                  value = new String(this.buf, valueStart, valueSize);
               }
            } else if (escapeCount == 0) {
               if (type != null && type != String.class && type != Object.class && !strings) {
                  try {
                     value = this.readValue(this.buf, valueStart + 1, valueSize, type);
                  } catch (Exception var22) {
                     value = this.error(columnIndex, var22);
                  }
               } else {
                  value = new String(this.buf, valueStart + 1, valueSize);
               }
            } else {
               char[] chars = new char[valueSize - escapeCount];
               int valueEnd = valueStart + valueSize;
               int j = valueStart + 1;

               for (int k = 0; j < valueEnd; j++) {
                  char c = this.buf[j];
                  chars[k++] = c;
                  if (c == '"' && this.buf[j + 1] == '"') {
                     j++;
                  }
               }

               if (type != null && type != String.class && type != Object.class && !strings) {
                  try {
                     value = this.readValue(chars, 0, chars.length, type);
                  } catch (Exception var21) {
                     value = this.error(columnIndex, var21);
                  }
               } else {
                  value = new String(chars);
               }
            }

            if (values != null) {
               if (columnIndex < values.length) {
                  values[columnIndex] = value;
               }
            } else {
               if (valueList == null) {
                  valueList = new ArrayList<>();
               }

               valueList.add(value);
            }

            quote = false;
            valueStart = i + 1;
            valueSize = 0;
            escapeCount = 0;
            columnIndex++;
         } else {
            valueSize++;
         }
      }

      if (valueSize > 0 || quote) {
         Type typex = this.types != null && columnIndex < this.types.length ? this.types[columnIndex] : null;
         Object valuex;
         if (!quote) {
            if (typex != null && typex != String.class && typex != Object.class && !strings) {
               try {
                  valuex = this.readValue(this.buf, valueStart, valueSize, typex);
               } catch (Exception var18) {
                  valuex = this.error(columnIndex, var18);
               }
            } else if (valueSize == 1) {
               valuex = TypeUtils.toString(this.buf[valueStart]);
            } else if (valueSize == 2) {
               valuex = TypeUtils.toString(this.buf[valueStart], this.buf[valueStart + 1]);
            } else {
               valuex = new String(this.buf, valueStart, valueSize);
            }
         } else if (escapeCount == 0) {
            if (typex != null && typex != String.class && typex != Object.class && !strings) {
               valuex = this.readValue(this.buf, valueStart + 1, valueSize, typex);
            } else {
               valuex = new String(this.buf, valueStart + 1, valueSize);
            }
         } else {
            char[] chars = new char[valueSize - escapeCount];
            int valueEnd = this.lineEnd;
            int j = valueStart + 1;

            for (int kx = 0; j < valueEnd; j++) {
               char c = this.buf[j];
               chars[kx++] = c;
               if (c == '"' && this.buf[j + 1] == '"') {
                  j++;
               }
            }

            if (typex != null && typex != String.class && typex != Object.class && !strings) {
               try {
                  valuex = this.readValue(chars, 0, chars.length, typex);
               } catch (Exception var19) {
                  valuex = this.error(columnIndex, var19);
               }
            } else {
               valuex = new String(chars);
            }
         }

         if (values != null) {
            if (columnIndex < values.length) {
               values[columnIndex] = valuex;
            }
         } else {
            if (valueList == null) {
               valueList = new ArrayList<>();
            }

            valueList.add(valuex);
         }
      }

      if (values == null && valueList != null) {
         if (strings) {
            values = new String[valueList.size()];
         } else {
            values = new Object[valueList.size()];
         }

         valueList.toArray(values);
      }

      if (this.input == null && this.off == this.end) {
         this.inputEnd = true;
      }

      return values;
   }

   @Override
   public void close() {
      if (this.input != null) {
         IOUtils.close(this.input);
      }
   }

   @Override
   public void statAll() {
      CharArrayValueConsumer consumer = (row, column, bytes, off, len) -> {
         StreamReader.ColumnStat stat = this.getColumnStat(column);
         stat.stat(bytes, off, len);
      };
      this.readAll(consumer, Integer.MAX_VALUE);
   }

   @Override
   public void statAll(int maxRows) {
      CharArrayValueConsumer consumer = (row, column, bytes, off, len) -> {
         StreamReader.ColumnStat stat = this.getColumnStat(column);
         stat.stat(bytes, off, len);
      };
      this.readAll(consumer, maxRows);
   }

   @Override
   public void readLineObjectAll(boolean readHeader, Consumer<T> consumer) {
      if (readHeader) {
         this.readHeader();
      }

      if (this.fieldReaders == null) {
         while (true) {
            Object[] line = this.readLineValues(false);
            if (line == null) {
               return;
            }

            consumer.accept((T)line);
         }
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         if (this.fieldReaders == null && this.objectClass != null) {
            ObjectReaderAdapter objectReader = (ObjectReaderAdapter)provider.getObjectReader(this.objectClass);
            this.fieldReaders = objectReader.getFieldReaders();
            this.objectCreator = provider.createObjectCreator(this.objectClass, this.features);
         }

         String[] strings = new String[this.fieldReaders.length + 1];
         strings[0] = this.objectClass.getName();

         for (int i = 0; i < this.fieldReaders.length; i++) {
            strings[i + 1] = this.fieldReaders[i].fieldName;
         }

         long fullNameHash = Fnv.hashCode64(strings);
         Function<Consumer, CharArrayValueConsumer> valueConsumerCreator = valueConsumerCreators.get(fullNameHash);
         if (valueConsumerCreator == null) {
            valueConsumerCreator = provider.createCharArrayValueConsumerCreator(this.objectClass, this.fieldReaders);
            if (valueConsumerCreator != null) {
               valueConsumerCreators.putIfAbsent(fullNameHash, valueConsumerCreator);
            }
         }

         CharArrayValueConsumer bytesConsumer = null;
         if (valueConsumerCreator != null) {
            bytesConsumer = valueConsumerCreator.apply(consumer);
         }

         if (bytesConsumer == null) {
            bytesConsumer = new CSVReaderUTF16.CharArrayConsumerImpl<>(consumer);
         }

         this.readAll(bytesConsumer, Integer.MAX_VALUE);
      }
   }

   @Override
   public void readAll() {
      if (this.valueConsumer == null) {
         throw new JSONException("unsupported operation, consumer is null");
      } else {
         this.readAll(this.valueConsumer, Integer.MAX_VALUE);
      }
   }

   @Override
   public void readAll(int maxRows) {
      if (this.valueConsumer == null) {
         throw new JSONException("unsupported operation, consumer is null");
      } else {
         this.readAll(this.valueConsumer, maxRows);
      }
   }

   public void readAll(CharArrayValueConsumer<T> consumer, int maxRows) {
      consumer.start();

      for (int r = 0; r < maxRows || maxRows < 0; r++) {
         try {
            if (this.inputEnd || this.input == null && this.off >= this.end) {
               break;
            }

            boolean result = this.seekLine();
            if (!result) {
               break;
            }
         } catch (IOException var16) {
            throw new JSONException("seekLine error", var16);
         }

         consumer.beforeRow(this.rowCount);
         boolean quote = false;
         int valueStart = this.lineStart;
         int valueSize = 0;
         int escapeCount = 0;
         int columnIndex = 0;

         for (int i = this.lineStart; i < this.lineEnd; i++) {
            char ch = this.buf[i];
            if (quote) {
               if (ch != '"') {
                  valueSize++;
                  continue;
               }

               int n = i + 1;
               if (n < this.lineEnd) {
                  char c1 = this.buf[n];
                  if (c1 == '"') {
                     valueSize += 2;
                     escapeCount++;
                     i++;
                     continue;
                  }

                  if (c1 == ',') {
                     i++;
                     ch = c1;
                  }
               } else if (n == this.lineEnd) {
                  break;
               }
            } else if (ch == '"') {
               quote = true;
               continue;
            }

            if (ch == ',') {
               if (!quote) {
                  consumer.accept(this.rowCount, columnIndex, this.buf, valueStart, valueSize);
               } else if (escapeCount == 0) {
                  consumer.accept(this.rowCount, columnIndex, this.buf, valueStart + 1, valueSize);
               } else {
                  char[] bytes = new char[valueSize - escapeCount];
                  int valueEnd = valueStart + valueSize;
                  int j = valueStart + 1;

                  for (int k = 0; j < valueEnd; j++) {
                     char c = this.buf[j];
                     bytes[k++] = c;
                     if (c == '"' && this.buf[j + 1] == '"') {
                        j++;
                     }
                  }

                  consumer.accept(this.rowCount, columnIndex, bytes, 0, bytes.length);
               }

               quote = false;
               valueStart = i + 1;
               valueSize = 0;
               escapeCount = 0;
               columnIndex++;
            } else {
               valueSize++;
            }
         }

         if (valueSize > 0) {
            if (quote) {
               if (escapeCount == 0) {
                  consumer.accept(this.rowCount, columnIndex, this.buf, valueStart + 1, valueSize);
               } else {
                  char[] bytes = new char[valueSize - escapeCount];
                  int valueEnd = this.lineEnd;
                  int j = valueStart + 1;

                  for (int kx = 0; j < valueEnd; j++) {
                     char c = this.buf[j];
                     bytes[kx++] = c;
                     if (c == '"' && this.buf[j + 1] == '"') {
                        j++;
                     }
                  }

                  consumer.accept(this.rowCount, columnIndex, bytes, 0, bytes.length);
               }
            } else {
               consumer.accept(this.rowCount, columnIndex, this.buf, valueStart, valueSize);
            }
         }

         consumer.afterRow(this.rowCount);
      }

      consumer.end();
   }

   class CharArrayConsumerImpl<T> implements CharArrayValueConsumer {
      protected T object;
      final Consumer<T> consumer;

      public CharArrayConsumerImpl(Consumer<T> consumer) {
         this.consumer = consumer;
      }

      @Override
      public final void beforeRow(int row) {
         if (CSVReaderUTF16.this.objectCreator != null) {
            this.object = (T)CSVReaderUTF16.this.objectCreator.get();
         }
      }

      @Override
      public void accept(int row, int column, char[] bytes, int off, int len) {
         if (column < CSVReaderUTF16.this.fieldReaders.length && len != 0) {
            FieldReader fieldReader = CSVReaderUTF16.this.fieldReaders[column];
            Object fieldValue = CSVReaderUTF16.this.readValue(bytes, off, len, fieldReader.fieldType);
            fieldReader.accept(this.object, fieldValue);
         }
      }

      @Override
      public final void afterRow(int row) {
         this.consumer.accept(this.object);
         this.object = null;
      }
   }
}
