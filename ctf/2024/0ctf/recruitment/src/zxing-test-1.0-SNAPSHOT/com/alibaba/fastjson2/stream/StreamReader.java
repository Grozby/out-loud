package com.alibaba.fastjson2.stream;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.reader.FieldReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.support.csv.CSVReader;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.JDKUtils;
import com.alibaba.fastjson2.util.TypeUtils;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class StreamReader<T> {
   protected static final int SIZE_512K = 524288;
   protected long features;
   protected Type[] types;
   protected ObjectReader[] typeReaders;
   protected Supplier objectCreator;
   protected FieldReader[] fieldReaders;
   protected int lineSize;
   protected int rowCount;
   protected int errorCount;
   protected int lineStart;
   protected int lineEnd;
   protected int lineNextStart;
   protected int end;
   protected int off;
   protected boolean inputEnd;
   protected boolean lineTerminated = true;
   protected Map<String, StreamReader.ColumnStat> columnStatsMap;
   protected List<String> columns;
   protected List<StreamReader.ColumnStat> columnStats;
   protected int[] mapping;

   public StreamReader() {
   }

   public StreamReader(Type[] types) {
      this.types = types;
      if (types.length == 0) {
         this.typeReaders = new ObjectReader[0];
      } else {
         ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
         ObjectReader[] readers = new ObjectReader[types.length];

         for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (type != String.class && type != Object.class) {
               readers[i] = provider.getObjectReader(type);
            } else {
               readers[i] = null;
            }
         }

         this.typeReaders = readers;
      }
   }

   protected abstract boolean seekLine() throws IOException;

   public abstract <T> T readLineObject();

   public <T> Stream<T> stream() {
      return StreamSupport.stream(new StreamReader.StreamReaderSpliterator<>(this), false);
   }

   public <T> Stream<T> stream(Class<T> clazz) {
      return StreamSupport.stream(new StreamReader.StreamReaderSpliterator<>(this, clazz), false);
   }

   public static class ColumnStat {
      @JSONField(
         ordinal = -1
      )
      public final String name;
      public int values;
      public int nulls;
      public int integers;
      public int doubles;
      public int numbers;
      public int dates;
      public int booleans;
      public int precision;
      public int scale;
      public int nonAsciiStrings;
      public int errors;
      public int maps;
      public int arrays;

      public ColumnStat(String name) {
         this.name = name;
      }

      public void stat(char[] bytes, int off, int len) {
         this.values++;
         if (len == 0) {
            this.nulls++;
         } else {
            int end = off + len;
            boolean nonAscii = false;

            for (int i = off; i < end; i++) {
               char b = bytes[i];
               if (b > 127) {
                  nonAscii = true;
                  break;
               }
            }

            if (nonAscii) {
               if (this.precision < len) {
                  this.precision = len;
               }

               this.nonAsciiStrings++;
            } else {
               int precision = len;
               if (TypeUtils.isNumber(bytes, off, len)) {
                  char ch = bytes[off];
                  if (ch == '+' || ch == '-') {
                     precision = len - 1;
                  }

                  this.numbers++;
                  if (TypeUtils.isInteger(bytes, off, len)) {
                     this.integers++;
                  } else {
                     boolean e = false;
                     int dotIndex = -1;

                     for (int ix = off; ix < end; ix++) {
                        char b = bytes[ix];
                        if (b == '.') {
                           dotIndex = ix;
                        } else if (b == 'e' || b == 'E') {
                           e = true;
                        }
                     }

                     if (e) {
                        this.doubles++;
                     } else if (dotIndex != -1) {
                        int scale = end - dotIndex - 1;
                        if (this.scale < scale) {
                           this.scale = scale;
                        }

                        precision--;
                     }
                  }
               } else {
                  boolean checkDate = false;
                  int sub = 0;
                  int slash = 0;
                  int colon = 0;
                  int dot = 0;
                  int nums = 0;

                  for (int ixx = off; ixx < end; ixx++) {
                     char chx = bytes[ixx];
                     switch (chx) {
                        case '-':
                           sub++;
                           break;
                        case '.':
                           dot++;
                           break;
                        case '/':
                           slash++;
                           break;
                        case ':':
                           colon++;
                           break;
                        default:
                           if (chx >= '0' && chx <= '9') {
                              nums++;
                           }
                     }
                  }

                  if (sub == 2 || slash == 2 || colon == 2) {
                     checkDate = true;
                  }

                  if (checkDate && (nums < 2 || len > 36)) {
                     checkDate = false;
                  }

                  if (checkDate) {
                     try {
                        LocalDateTime ldt = null;
                        switch (len) {
                           case 8: {
                              LocalDate localDate = DateUtils.parseLocalDate8(bytes, off);
                              ldt = localDate.atStartOfDay();
                              break;
                           }
                           case 9: {
                              LocalDate localDate = DateUtils.parseLocalDate9(bytes, off);
                              ldt = localDate.atStartOfDay();
                              break;
                           }
                           case 10: {
                              LocalDate localDate = DateUtils.parseLocalDate10(bytes, off);
                              ldt = localDate.atStartOfDay();
                           }
                        }

                        if (ldt == null) {
                           String str = new String(bytes, off, len);
                           ZonedDateTime zdt = DateUtils.parseZonedDateTime(str);
                           if (zdt != null) {
                              ldt = zdt.toLocalDateTime();
                           }
                        }

                        if (ldt != null) {
                           precision = 0;
                           this.dates++;
                        }

                        int nanoOfSeconds = ldt.getNano();
                        if (nanoOfSeconds != 0) {
                           if (nanoOfSeconds % 100000000 == 0) {
                              precision = 1;
                           } else if (nanoOfSeconds % 10000000 == 0) {
                              precision = 2;
                           } else if (nanoOfSeconds % 1000000 == 0) {
                              precision = 3;
                           } else if (nanoOfSeconds % 100000 == 0) {
                              precision = 4;
                           } else if (nanoOfSeconds % 10000 == 0) {
                              precision = 5;
                           } else if (nanoOfSeconds % 1000 == 0) {
                              precision = 6;
                           } else if (nanoOfSeconds % 100 == 0) {
                              precision = 7;
                           } else if (nanoOfSeconds % 10 == 0) {
                              precision = 8;
                           } else {
                              precision = 9;
                           }
                        }
                     } catch (Exception var16) {
                        this.errors++;
                     }
                  }
               }

               if (this.precision < precision) {
                  this.precision = precision;
               }
            }
         }
      }

      public void stat(byte[] bytes, int off, int len, Charset charset) {
         this.values++;
         if (len == 0) {
            this.nulls++;
         } else {
            int end = off + len;
            boolean nonAscii = false;

            for (int i = off; i < end; i++) {
               byte b = bytes[i];
               if (b < 0) {
                  nonAscii = true;
                  break;
               }
            }

            if (nonAscii) {
               if (this.precision < len) {
                  this.precision = len;
               }

               this.nonAsciiStrings++;
            } else {
               int precision = len;
               if (TypeUtils.isNumber(bytes, off, len)) {
                  char ch = (char)bytes[off];
                  if (ch == '+' || ch == '-') {
                     precision = len - 1;
                  }

                  this.numbers++;
                  if (TypeUtils.isInteger(bytes, off, len)) {
                     this.integers++;
                  } else {
                     boolean e = false;
                     int dotIndex = -1;

                     for (int ix = off; ix < end; ix++) {
                        byte b = bytes[ix];
                        if (b == 46) {
                           dotIndex = ix;
                        } else if (b == 101 || b == 69) {
                           e = true;
                        }
                     }

                     if (e) {
                        this.doubles++;
                     } else if (dotIndex != -1) {
                        int scale = end - dotIndex - 1;
                        if (this.scale < scale) {
                           this.scale = scale;
                        }

                        precision--;
                     }
                  }
               } else {
                  boolean checkDate = false;
                  int sub = 0;
                  int slash = 0;
                  int colon = 0;
                  int dot = 0;
                  int nums = 0;

                  for (int ixx = off; ixx < end; ixx++) {
                     char chx = (char)bytes[ixx];
                     switch (chx) {
                        case '-':
                           sub++;
                           break;
                        case '.':
                           dot++;
                           break;
                        case '/':
                           slash++;
                           break;
                        case ':':
                           colon++;
                           break;
                        default:
                           if (chx >= '0' && chx <= '9') {
                              nums++;
                           }
                     }
                  }

                  if (sub == 2 || slash == 2 || colon == 2) {
                     checkDate = true;
                  }

                  if (checkDate && (nums < 2 || len > 36)) {
                     checkDate = false;
                  }

                  if (checkDate) {
                     try {
                        LocalDateTime ldt = null;
                        switch (len) {
                           case 8: {
                              LocalDate localDate = DateUtils.parseLocalDate8(bytes, off);
                              ldt = localDate.atStartOfDay();
                              break;
                           }
                           case 9: {
                              LocalDate localDate = DateUtils.parseLocalDate9(bytes, off);
                              ldt = localDate.atStartOfDay();
                              break;
                           }
                           case 10: {
                              LocalDate localDate = DateUtils.parseLocalDate10(bytes, off);
                              ldt = localDate.atStartOfDay();
                           }
                        }

                        if (ldt == null) {
                           String str = new String(bytes, off, len, charset);
                           ZonedDateTime zdt = DateUtils.parseZonedDateTime(str);
                           if (zdt != null) {
                              ldt = zdt.toLocalDateTime();
                           }
                        }

                        if (ldt != null) {
                           precision = 0;
                           this.dates++;
                        }

                        int nanoOfSeconds = ldt.getNano();
                        if (nanoOfSeconds != 0) {
                           if (nanoOfSeconds % 100000000 == 0) {
                              precision = 1;
                           } else if (nanoOfSeconds % 10000000 == 0) {
                              precision = 2;
                           } else if (nanoOfSeconds % 1000000 == 0) {
                              precision = 3;
                           } else if (nanoOfSeconds % 100000 == 0) {
                              precision = 4;
                           } else if (nanoOfSeconds % 10000 == 0) {
                              precision = 5;
                           } else if (nanoOfSeconds % 1000 == 0) {
                              precision = 6;
                           } else if (nanoOfSeconds % 100 == 0) {
                              precision = 7;
                           } else if (nanoOfSeconds % 10 == 0) {
                              precision = 8;
                           } else {
                              precision = 9;
                           }
                        }
                     } catch (Exception var17) {
                        this.errors++;
                     }
                  }
               }

               if (this.precision < precision) {
                  this.precision = precision;
               }
            }
         }
      }

      public void stat(String str) {
         if (JDKUtils.STRING_CODER != null && JDKUtils.STRING_CODER.applyAsInt(str) == JDKUtils.LATIN1 && JDKUtils.STRING_VALUE != null) {
            byte[] bytes = JDKUtils.STRING_VALUE.apply(str);
            this.stat(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
         } else {
            char[] chars = JDKUtils.getCharArray(str);
            this.stat(chars, 0, chars.length);
         }
      }

      public String getInferSQLType() {
         if (this.nonAsciiStrings > 0 || this.nulls == this.values) {
            return "STRING";
         } else if (this.values == this.dates + this.nulls) {
            return this.precision != 0 ? "TIMESTAMP" : "DATETIME";
         } else if (this.values == this.integers + this.nulls) {
            if (this.precision < 10) {
               return "INT";
            } else {
               return this.precision < 20 ? "BIGINT" : "DECIMAL(" + this.precision + ", 0)";
            }
         } else if (this.values != this.numbers + this.nulls) {
            return "STRING";
         } else if (this.doubles <= 0 && this.scale <= 5) {
            int precision = this.precision;
            if (precision < 19) {
               precision = 19;
            }

            return "DECIMAL(" + precision + ", " + this.scale + ")";
         } else {
            return "DOUBLE";
         }
      }

      public Type getInferType() {
         if (this.nonAsciiStrings > 0 || this.nulls == this.values) {
            return String.class;
         } else if (this.values == this.booleans + this.nulls) {
            return Boolean.class;
         } else if (this.values == this.dates + this.nulls) {
            return this.precision != 0 ? Instant.class : Date.class;
         } else if (this.doubles > 0) {
            return Double.class;
         } else if (this.values == this.integers + this.nulls) {
            if (this.precision < 10) {
               return Integer.class;
            } else {
               return this.precision < 20 ? Long.class : BigInteger.class;
            }
         } else if (this.values == this.numbers + this.nulls) {
            return BigDecimal.class;
         } else if (this.arrays > 0) {
            return Collection.class;
         } else {
            return this.maps > 0 ? Map.class : String.class;
         }
      }
   }

   public static enum Feature {
      IgnoreEmptyLine(1L),
      ErrorAsNull(2L);

      public final long mask;

      private Feature(long mask) {
         this.mask = mask;
      }
   }

   protected static class StreamReaderSpliterator<T> implements Spliterator<T> {
      private final StreamReader<T> streamReader;
      private Class<T> clazz;
      private CSVReader csvReader;

      public StreamReaderSpliterator(StreamReader<T> streamReader) {
         this.streamReader = streamReader;
         if (streamReader instanceof CSVReader) {
            CSVReader reader = (CSVReader)streamReader;
            if (!reader.isObjectSupport()) {
               this.csvReader = reader;
            }
         }
      }

      public StreamReaderSpliterator(StreamReader<T> streamReader, Class<T> clazz) {
         this.streamReader = streamReader;
         this.clazz = clazz;
         if (streamReader instanceof CSVReader) {
            CSVReader reader = (CSVReader)streamReader;
            if (!reader.isObjectSupport()) {
               this.csvReader = reader;
            }
         }
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         if (action == null) {
            throw new IllegalArgumentException("action must not be null");
         } else {
            T object = this.next();
            if (!this.streamReader.inputEnd && object != null) {
               action.accept(object);
               return true;
            } else {
               return false;
            }
         }
      }

      private T next() {
         if (this.csvReader != null) {
            Object[] objects = this.csvReader.readLineValues();
            if (this.clazz != null & !this.clazz.isAssignableFrom(objects.getClass())) {
               throw new ClassCastException(String.format("%s can not cast to %s", objects.getClass(), this.clazz));
            } else {
               return (T)objects;
            }
         } else {
            return this.streamReader.readLineObject();
         }
      }

      @Override
      public Spliterator<T> trySplit() {
         throw new UnsupportedOperationException("parallel stream not supported");
      }

      @Override
      public long estimateSize() {
         return this.streamReader.inputEnd ? 0L : Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
         return 1296;
      }
   }
}
