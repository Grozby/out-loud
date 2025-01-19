package com.alibaba.fastjson2.support.csv;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ByteArrayValueConsumer;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReaderAdapter;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.stream.StreamReader;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.IOUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

final class CSVReaderUTF8<T> extends CSVReader<T> {
   static final Map<Long, Function<Consumer, ByteArrayValueConsumer>> valueConsumerCreators = new ConcurrentHashMap<>();
   byte[] buf;
   InputStream input;
   Charset charset = StandardCharsets.UTF_8;
   ByteArrayValueConsumer valueConsumer;

   CSVReaderUTF8(StreamReader.Feature... features) {
      for (StreamReader.Feature feature : features) {
         this.features = this.features | feature.mask;
      }
   }

   CSVReaderUTF8(byte[] bytes, int off, int len, Charset charset, Class<T> objectClass) {
      super(objectClass);
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
      this.charset = charset;
   }

   CSVReaderUTF8(byte[] bytes, int off, int len, Charset charset, ByteArrayValueConsumer valueConsumer) {
      this.valueConsumer = valueConsumer;
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
      this.charset = charset;
   }

   CSVReaderUTF8(byte[] bytes, int off, int len, Type[] types) {
      super(types);
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
      this.types = types;
   }

   CSVReaderUTF8(byte[] bytes, int off, int len, Class<T> objectClass) {
      super(objectClass);
      this.buf = bytes;
      this.off = off;
      this.end = off + len;
   }

   CSVReaderUTF8(InputStream input, Charset charset, Type[] types) {
      super(types);
      this.charset = charset;
      this.input = input;
   }

   CSVReaderUTF8(InputStream input, Charset charset, Class<T> objectClass) {
      super(objectClass);
      this.charset = charset;
      this.input = input;
   }

   CSVReaderUTF8(InputStream input, Charset charset, ByteArrayValueConsumer valueConsumer) {
      this.charset = charset;
      this.input = input;
      this.valueConsumer = valueConsumer;
   }

   @Override
   protected boolean seekLine() throws IOException {
      byte[] buf = this.buf;
      int off = this.off;
      if (buf == null && this.input != null) {
         buf = this.buf = new byte[524288];
         int cnt = this.input.read(buf);
         if (cnt == -1) {
            this.inputEnd = true;
            return false;
         }

         this.end = cnt;
         if (this.end > 3 && buf[0] == -17 && buf[1] == -69 && buf[2] == -65) {
            off = 3;
            this.lineNextStart = off;
         }
      }

      int k = 0;

      while (k < 3) {
         this.lineTerminated = false;

         for (int i = off; i < this.end; i++) {
            byte ch = buf[i];
            if (ch == 34) {
               this.lineSize++;
               if (!this.quote) {
                  this.quote = true;
               } else {
                  int n = i + 1;
                  if (n >= this.end) {
                     break;
                  }

                  if (buf[n] == 34) {
                     this.lineSize++;
                     i++;
                  } else {
                     this.quote = false;
                  }
               }
            } else if (this.quote) {
               this.lineSize++;
            } else {
               if (ch == 10) {
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

               if (ch == 13) {
                  if (this.lineSize > 0 || (this.features & StreamReader.Feature.IgnoreEmptyLine.mask) == 0L) {
                     this.rowCount++;
                  }

                  this.lineTerminated = true;
                  this.lineSize = 0;
                  this.lineEnd = i;
                  int nx = i + 1;
                  if (nx < this.end) {
                     if (buf[nx] == 10) {
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

               int cntx = this.input.read(buf, this.end, buf.length - this.end);
               if (cntx != -1) {
                  this.end += cntx;
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

   Object readValue(byte[] bytes, int off, int len, Type type) {
      if (len == 0) {
         return null;
      } else if (type == Integer.class) {
         return TypeUtils.parseInt(bytes, off, len);
      } else if (type == Long.class) {
         return TypeUtils.parseLong(bytes, off, len);
      } else if (type == BigDecimal.class) {
         return TypeUtils.parseBigDecimal(bytes, off, len);
      } else if (type == Float.class) {
         return TypeUtils.parseFloat(bytes, off, len);
      } else if (type == Double.class) {
         return TypeUtils.parseDouble(bytes, off, len);
      } else if (type == Date.class) {
         long millis = DateUtils.parseMillis(bytes, off, len, this.charset, DateUtils.DEFAULT_ZONE_ID);
         return new Date(millis);
      } else if (type == Boolean.class) {
         return TypeUtils.parseBoolean(bytes, off, len);
      } else {
         String str = new String(bytes, off, len, this.charset);
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
      } catch (IOException var24) {
         throw new JSONException("seekLine error", var24);
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
         byte ch = this.buf[i];
         if (quote) {
            if (ch != 34) {
               valueSize++;
               continue;
            }

            int n = i + 1;
            if (n < this.lineEnd) {
               byte c1 = this.buf[n];
               if (c1 == 34) {
                  valueSize += 2;
                  escapeCount++;
                  i++;
                  continue;
               }

               if (c1 == 44) {
                  i++;
                  ch = c1;
               }
            } else if (n == this.lineEnd) {
               break;
            }
         } else if (ch == 34) {
            quote = true;
            continue;
         }

         if (ch == 44) {
            Type type = this.types != null && columnIndex < this.types.length ? this.types[columnIndex] : null;
            Object value;
            if (!quote) {
               if (type != null && type != String.class && type != Object.class && !strings) {
                  try {
                     value = this.readValue(this.buf, valueStart, valueSize, type);
                  } catch (Exception var21) {
                     value = this.error(columnIndex, var21);
                  }
               } else {
                  byte c0;
                  if (valueSize == 1 && (c0 = this.buf[valueStart]) >= 0) {
                     value = TypeUtils.toString((char)c0);
                  } else {
                     byte c1x;
                     if (valueSize == 2 && (c0 = this.buf[valueStart]) >= 0 && (c1x = this.buf[valueStart + 1]) >= 0) {
                        value = TypeUtils.toString((char)c0, (char)c1x);
                     } else {
                        value = new String(this.buf, valueStart, valueSize, this.charset);
                     }
                  }
               }
            } else if (escapeCount == 0) {
               if (type != null && type != String.class && type != Object.class && !strings) {
                  try {
                     value = this.readValue(this.buf, valueStart + 1, valueSize, type);
                  } catch (Exception var23) {
                     value = this.error(columnIndex, var23);
                  }
               } else {
                  value = new String(this.buf, valueStart + 1, valueSize, this.charset);
               }
            } else {
               byte[] bytes = new byte[valueSize - escapeCount];
               int valueEnd = valueStart + valueSize;
               int j = valueStart + 1;

               for (int k = 0; j < valueEnd; j++) {
                  byte c = this.buf[j];
                  bytes[k++] = c;
                  if (c == 34 && this.buf[j + 1] == 34) {
                     j++;
                  }
               }

               if (type != null && type != String.class && type != Object.class) {
                  try {
                     value = this.readValue(bytes, 0, bytes.length, type);
                  } catch (Exception var22) {
                     value = this.error(columnIndex, var22);
                  }
               } else {
                  value = new String(bytes, 0, bytes.length, this.charset);
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
            } else {
               byte c0;
               if (valueSize == 1 && (c0 = this.buf[valueStart]) >= 0) {
                  valuex = TypeUtils.toString((char)c0);
               } else {
                  byte c1x;
                  if (valueSize == 2 && (c0 = this.buf[valueStart]) >= 0 && (c1x = this.buf[valueStart + 1]) >= 0) {
                     valuex = TypeUtils.toString((char)c0, (char)c1x);
                  } else {
                     valuex = new String(this.buf, valueStart, valueSize, this.charset);
                  }
               }
            }
         } else if (escapeCount == 0) {
            if (typex != null && typex != String.class && typex != Object.class && !strings) {
               try {
                  valuex = this.readValue(this.buf, valueStart + 1, valueSize, typex);
               } catch (Exception var20) {
                  valuex = this.error(columnIndex, var20);
               }
            } else {
               valuex = new String(this.buf, valueStart + 1, valueSize, this.charset);
            }
         } else {
            byte[] bytes = new byte[valueSize - escapeCount];
            int valueEnd = this.lineEnd;
            int j = valueStart + 1;

            for (int kx = 0; j < valueEnd; j++) {
               byte c = this.buf[j];
               bytes[kx++] = c;
               if (c == 34 && this.buf[j + 1] == 34) {
                  j++;
               }
            }

            if (typex != null && typex != String.class && typex != Object.class && !strings) {
               try {
                  valuex = this.readValue(bytes, 0, bytes.length, typex);
               } catch (Exception var19) {
                  valuex = this.error(columnIndex, var19);
               }
            } else {
               valuex = new String(bytes, 0, bytes.length, this.charset);
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
      ByteArrayValueConsumer consumer = (row, column, bytes, off, len, charset) -> {
         StreamReader.ColumnStat stat = this.getColumnStat(column);
         stat.stat(bytes, off, len, charset);
      };
      this.readAll(consumer, Integer.MAX_VALUE);
   }

   @Override
   public void statAll(int maxRows) {
      ByteArrayValueConsumer consumer = (row, column, bytes, off, len, charset) -> {
         StreamReader.ColumnStat stat = this.getColumnStat(column);
         stat.stat(bytes, off, len, charset);
      };
      this.readAll(consumer, maxRows);
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
         Function<Consumer, ByteArrayValueConsumer> valueConsumerCreator = valueConsumerCreators.get(fullNameHash);
         if (valueConsumerCreator == null) {
            valueConsumerCreator = provider.createValueConsumerCreator(this.objectClass, this.fieldReaders);
            if (valueConsumerCreator != null) {
               valueConsumerCreators.putIfAbsent(fullNameHash, valueConsumerCreator);
            }
         }

         ByteArrayValueConsumer bytesConsumer = null;
         if (valueConsumerCreator != null) {
            bytesConsumer = valueConsumerCreator.apply(consumer);
         }

         if (bytesConsumer == null) {
            bytesConsumer = new CSVReaderUTF8.ByteArrayConsumerImpl(consumer);
         }

         this.readAll(bytesConsumer, Integer.MAX_VALUE);
      }
   }

   private void readAll(ByteArrayValueConsumer consumer, int maxRows) {
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
         } catch (IOException var19) {
            throw new JSONException("seekLine error", var19);
         }

         consumer.beforeRow(this.rowCount);
         boolean quote = false;
         int valueStart = this.lineStart;
         int valueSize = 0;
         int escapeCount = 0;
         int columnIndex = 0;

         for (int i = this.lineStart; i < this.lineEnd; i++) {
            byte ch = this.buf[i];
            if (quote) {
               if (ch != 34) {
                  valueSize++;
                  continue;
               }

               int n = i + 1;
               if (n < this.lineEnd) {
                  byte c1 = this.buf[n];
                  if (c1 == 34) {
                     valueSize += 2;
                     escapeCount++;
                     i++;
                     continue;
                  }

                  if (c1 == 44) {
                     i++;
                     ch = c1;
                  }
               } else if (n == this.lineEnd) {
                  break;
               }
            } else if (ch == 34) {
               quote = true;
               continue;
            }

            if (ch == 44) {
               byte[] columnBuf = this.buf;
               int columnStart = 0;
               int columnSize = valueSize;
               if (!quote) {
                  columnStart = valueStart;
               } else if (escapeCount == 0) {
                  columnStart = valueStart + 1;
               } else {
                  byte[] bytes = new byte[valueSize - escapeCount];
                  int valueEnd = valueStart + valueSize;
                  int j = valueStart + 1;

                  for (int k = 0; j < valueEnd; j++) {
                     byte c = this.buf[j];
                     bytes[k++] = c;
                     if (c == 34 && this.buf[j + 1] == 34) {
                        j++;
                     }
                  }

                  columnBuf = bytes;
                  columnSize = bytes.length;
               }

               consumer.accept(this.rowCount, columnIndex, columnBuf, columnStart, columnSize, this.charset);
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
            byte[] columnBuf = this.buf;
            int columnStart = 0;
            int columnSize = valueSize;
            if (!quote) {
               columnStart = valueStart;
            } else if (escapeCount == 0) {
               columnStart = valueStart + 1;
            } else {
               byte[] bytes = new byte[valueSize - escapeCount];
               int valueEnd = this.lineEnd;
               int j = valueStart + 1;

               for (int kx = 0; j < valueEnd; j++) {
                  byte c = this.buf[j];
                  bytes[kx++] = c;
                  if (c == 34 && this.buf[j + 1] == 34) {
                     j++;
                  }
               }

               columnBuf = bytes;
               columnSize = bytes.length;
            }

            consumer.accept(this.rowCount, columnIndex, columnBuf, columnStart, columnSize, this.charset);
         }

         consumer.afterRow(this.rowCount);
      }

      consumer.end();
   }

   class ByteArrayConsumerImpl implements ByteArrayValueConsumer {
      protected Object object;
      final Consumer consumer;

      public ByteArrayConsumerImpl(Consumer consumer) {
         this.consumer = consumer;
      }

      @Override
      public final void beforeRow(int row) {
         if (CSVReaderUTF8.this.objectCreator != null) {
            this.object = CSVReaderUTF8.this.objectCreator.get();
         }
      }

      @Override
      public void accept(int row, int column, byte[] bytes, int off, int len, Charset charset) {
         if (column < CSVReaderUTF8.this.fieldReaders.length && len != 0) {
            FieldReader fieldReader = CSVReaderUTF8.this.fieldReaders[column];
            Object fieldValue = CSVReaderUTF8.this.readValue(bytes, off, len, fieldReader.fieldType);
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
