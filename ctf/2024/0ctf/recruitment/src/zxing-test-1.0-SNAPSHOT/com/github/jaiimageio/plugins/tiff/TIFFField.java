package com.github.jaiimageio.plugins.tiff;

import com.github.jaiimageio.impl.plugins.tiff.TIFFFieldNode;
import java.util.StringTokenizer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class TIFFField implements Comparable {
   private static final String[] typeNames = new String[]{
      null, "Byte", "Ascii", "Short", "Long", "Rational", "SByte", "Undefined", "SShort", "SLong", "SRational", "Float", "Double", "IFDPointer"
   };
   private static final boolean[] isIntegral = new boolean[]{false, true, false, true, true, false, true, true, true, true, false, false, false, false};
   private TIFFTag tag;
   private int tagNumber;
   private int type;
   private int count;
   private Object data;

   private TIFFField() {
   }

   private static String getAttribute(Node node, String attrName) {
      NamedNodeMap attrs = node.getAttributes();
      return attrs.getNamedItem(attrName).getNodeValue();
   }

   private static void initData(Node node, int[] otype, int[] ocount, Object[] odata) {
      Object data = null;
      String typeName = node.getNodeName();
      typeName = typeName.substring(4);
      typeName = typeName.substring(0, typeName.length() - 1);
      int type = getTypeByName(typeName);
      if (type == -1) {
         throw new IllegalArgumentException("typeName = " + typeName);
      } else {
         Node child = node.getFirstChild();

         int count;
         for (count = 0; child != null; child = child.getNextSibling()) {
            String childTypeName = child.getNodeName().substring(4);
            if (!typeName.equals(childTypeName)) {
            }

            count++;
         }

         if (count > 0) {
            data = createArrayForType(type, count);
            child = node.getFirstChild();

            for (int idx = 0; child != null; child = child.getNextSibling()) {
               String value = getAttribute(child, "value");
               switch (type) {
                  case 1:
                  case 6:
                     ((byte[])data)[idx] = (byte)Integer.parseInt(value);
                     break;
                  case 2:
                     ((String[])data)[idx] = value;
                     break;
                  case 3:
                     ((char[])data)[idx] = (char)Integer.parseInt(value);
                     break;
                  case 4:
                  case 13:
                     ((long[])data)[idx] = Long.parseLong(value);
                     break;
                  case 5: {
                     int slashPos = value.indexOf("/");
                     String numerator = value.substring(0, slashPos);
                     String denominator = value.substring(slashPos + 1);
                     ((long[][])data)[idx] = new long[2];
                     ((long[][])data)[idx][0] = Long.parseLong(numerator);
                     ((long[][])data)[idx][1] = Long.parseLong(denominator);
                  }
                  case 7:
                  default:
                     break;
                  case 8:
                     ((short[])data)[idx] = (short)Integer.parseInt(value);
                     break;
                  case 9:
                     ((int[])data)[idx] = Integer.parseInt(value);
                     break;
                  case 10: {
                     int slashPos = value.indexOf("/");
                     String numerator = value.substring(0, slashPos);
                     String denominator = value.substring(slashPos + 1);
                     ((int[][])data)[idx] = new int[2];
                     ((int[][])data)[idx][0] = Integer.parseInt(numerator);
                     ((int[][])data)[idx][1] = Integer.parseInt(denominator);
                     break;
                  }
                  case 11:
                     ((float[])data)[idx] = Float.parseFloat(value);
                     break;
                  case 12:
                     ((double[])data)[idx] = Double.parseDouble(value);
               }

               idx++;
            }
         }

         otype[0] = type;
         ocount[0] = count;
         odata[0] = data;
      }
   }

   public static TIFFField createFromMetadataNode(TIFFTagSet tagSet, Node node) {
      if (node == null) {
         throw new IllegalArgumentException("node == null!");
      } else {
         String name = node.getNodeName();
         if (!name.equals("TIFFField")) {
            throw new IllegalArgumentException("!name.equals(\"TIFFField\")");
         } else {
            int tagNumber = Integer.parseInt(getAttribute(node, "number"));
            TIFFTag tag;
            if (tagSet != null) {
               tag = tagSet.getTag(tagNumber);
            } else {
               tag = new TIFFTag("unknown", tagNumber, 0, null);
            }

            int type = 7;
            int count = 0;
            Object data = null;
            Node child = node.getFirstChild();
            if (child != null) {
               String typeName = child.getNodeName();
               if (typeName.equals("TIFFUndefined")) {
                  String values = getAttribute(child, "value");
                  StringTokenizer st = new StringTokenizer(values, ",");
                  count = st.countTokens();
                  byte[] bdata = new byte[count];

                  for (int i = 0; i < count; i++) {
                     bdata[i] = (byte)Integer.parseInt(st.nextToken());
                  }

                  type = 7;
                  data = bdata;
               } else {
                  int[] otype = new int[1];
                  int[] ocount = new int[1];
                  Object[] odata = new Object[1];
                  initData(node.getFirstChild(), otype, ocount, odata);
                  type = otype[0];
                  count = ocount[0];
                  data = odata[0];
               }
            } else {
               int t = 13;

               while (t >= 1 && !tag.isDataTypeOK(t)) {
                  t--;
               }

               type = t;
            }

            return new TIFFField(tag, type, count, data);
         }
      }
   }

   public TIFFField(TIFFTag tag, int type, int count, Object data) {
      if (tag == null) {
         throw new IllegalArgumentException("tag == null!");
      } else if (type < 1 || type > 13) {
         throw new IllegalArgumentException("Unknown data type " + type);
      } else if (count < 0) {
         throw new IllegalArgumentException("count < 0!");
      } else {
         this.tag = tag;
         this.tagNumber = tag.getNumber();
         this.type = type;
         this.count = count;
         this.data = data;
      }
   }

   public TIFFField(TIFFTag tag, int type, int count) {
      this(tag, type, count, createArrayForType(type, count));
   }

   public TIFFField(TIFFTag tag, int value) {
      if (tag == null) {
         throw new IllegalArgumentException("tag == null!");
      } else if (value < 0) {
         throw new IllegalArgumentException("value < 0!");
      } else {
         this.tag = tag;
         this.tagNumber = tag.getNumber();
         this.count = 1;
         if (value < 65536) {
            this.type = 3;
            char[] cdata = new char[]{(char)value};
            this.data = cdata;
         } else {
            this.type = 4;
            long[] ldata = new long[1];
            ldata[0] = (long)value;
            this.data = ldata;
         }
      }
   }

   public TIFFTag getTag() {
      return this.tag;
   }

   public int getTagNumber() {
      return this.tagNumber;
   }

   public int getType() {
      return this.type;
   }

   public static String getTypeName(int dataType) {
      if (dataType >= 1 && dataType <= 13) {
         return typeNames[dataType];
      } else {
         throw new IllegalArgumentException("Unknown data type " + dataType);
      }
   }

   public static int getTypeByName(String typeName) {
      for (int i = 1; i <= 13; i++) {
         if (typeName.equals(typeNames[i])) {
            return i;
         }
      }

      return -1;
   }

   public static Object createArrayForType(int dataType, int count) {
      if (count < 0) {
         throw new IllegalArgumentException("count < 0!");
      } else {
         switch (dataType) {
            case 1:
            case 6:
            case 7:
               return new byte[count];
            case 2:
               return new String[count];
            case 3:
               return new char[count];
            case 4:
            case 13:
               return new long[count];
            case 5:
               return new long[count][2];
            case 8:
               return new short[count];
            case 9:
               return new int[count];
            case 10:
               return new int[count][2];
            case 11:
               return new float[count];
            case 12:
               return new double[count];
            default:
               throw new IllegalArgumentException("Unknown data type " + dataType);
         }
      }
   }

   public Node getAsNativeNode() {
      return new TIFFFieldNode(this);
   }

   public boolean isIntegral() {
      return isIntegral[this.type];
   }

   public int getCount() {
      return this.count;
   }

   public Object getData() {
      return this.data;
   }

   public byte[] getAsBytes() {
      return (byte[])this.data;
   }

   public char[] getAsChars() {
      return (char[])this.data;
   }

   public short[] getAsShorts() {
      return (short[])this.data;
   }

   public int[] getAsInts() {
      if (this.data instanceof int[]) {
         return (int[])this.data;
      } else if (this.data instanceof char[]) {
         char[] cdata = (char[])this.data;
         int[] idata = new int[cdata.length];

         for (int i = 0; i < cdata.length; i++) {
            idata[i] = cdata[i] & '\uffff';
         }

         return idata;
      } else if (!(this.data instanceof short[])) {
         throw new ClassCastException("Data not char[], short[], or int[]!");
      } else {
         short[] sdata = (short[])this.data;
         int[] idata = new int[sdata.length];

         for (int i = 0; i < sdata.length; i++) {
            idata[i] = sdata[i];
         }

         return idata;
      }
   }

   public long[] getAsLongs() {
      return (long[])this.data;
   }

   public float[] getAsFloats() {
      return (float[])this.data;
   }

   public double[] getAsDoubles() {
      return (double[])this.data;
   }

   public int[][] getAsSRationals() {
      return (int[][])this.data;
   }

   public long[][] getAsRationals() {
      return (long[][])this.data;
   }

   public int getAsInt(int index) {
      switch (this.type) {
         case 1:
         case 7:
            return ((byte[])this.data)[index] & 0xFF;
         case 2:
            String s = ((String[])this.data)[index];
            return (int)Double.parseDouble(s);
         case 3:
            return ((char[])this.data)[index] & 65535;
         case 4:
         case 13:
            return (int)((long[])this.data)[index];
         case 5:
            long[] lvalue = this.getAsRational(index);
            return (int)((double)lvalue[0] / (double)lvalue[1]);
         case 6:
            return ((byte[])this.data)[index];
         case 8:
            return ((short[])this.data)[index];
         case 9:
            return ((int[])this.data)[index];
         case 10:
            int[] ivalue = this.getAsSRational(index);
            return (int)((double)ivalue[0] / (double)ivalue[1]);
         case 11:
            return (int)((float[])this.data)[index];
         case 12:
            return (int)((double[])this.data)[index];
         default:
            throw new ClassCastException();
      }
   }

   public long getAsLong(int index) {
      switch (this.type) {
         case 1:
         case 7:
            return (long)(((byte[])this.data)[index] & 255);
         case 2:
            String s = ((String[])this.data)[index];
            return (long)Double.parseDouble(s);
         case 3:
            return (long)(((char[])this.data)[index] & '\uffff');
         case 4:
         case 13:
            return ((long[])this.data)[index];
         case 5:
            long[] lvalue = this.getAsRational(index);
            return (long)((double)lvalue[0] / (double)lvalue[1]);
         case 6:
            return (long)((byte[])this.data)[index];
         case 8:
            return (long)((short[])this.data)[index];
         case 9:
            return (long)((int[])this.data)[index];
         case 10:
            int[] ivalue = this.getAsSRational(index);
            return (long)((double)ivalue[0] / (double)ivalue[1]);
         case 11:
         case 12:
         default:
            throw new ClassCastException();
      }
   }

   public float getAsFloat(int index) {
      switch (this.type) {
         case 1:
         case 7:
            return (float)(((byte[])this.data)[index] & 255);
         case 2:
            String s = ((String[])this.data)[index];
            return (float)Double.parseDouble(s);
         case 3:
            return (float)(((char[])this.data)[index] & '\uffff');
         case 4:
         case 13:
            return (float)((long[])this.data)[index];
         case 5:
            long[] lvalue = this.getAsRational(index);
            return (float)((double)lvalue[0] / (double)lvalue[1]);
         case 6:
            return (float)((byte[])this.data)[index];
         case 8:
            return (float)((short[])this.data)[index];
         case 9:
            return (float)((int[])this.data)[index];
         case 10:
            int[] ivalue = this.getAsSRational(index);
            return (float)((double)ivalue[0] / (double)ivalue[1]);
         case 11:
            return ((float[])this.data)[index];
         case 12:
            return (float)((double[])this.data)[index];
         default:
            throw new ClassCastException();
      }
   }

   public double getAsDouble(int index) {
      switch (this.type) {
         case 1:
         case 7:
            return (double)(((byte[])this.data)[index] & 255);
         case 2:
            String s = ((String[])this.data)[index];
            return Double.parseDouble(s);
         case 3:
            return (double)(((char[])this.data)[index] & '\uffff');
         case 4:
         case 13:
            return (double)((long[])this.data)[index];
         case 5:
            long[] lvalue = this.getAsRational(index);
            return (double)lvalue[0] / (double)lvalue[1];
         case 6:
            return (double)((byte[])this.data)[index];
         case 8:
            return (double)((short[])this.data)[index];
         case 9:
            return (double)((int[])this.data)[index];
         case 10:
            int[] ivalue = this.getAsSRational(index);
            return (double)ivalue[0] / (double)ivalue[1];
         case 11:
            return (double)((float[])this.data)[index];
         case 12:
            return ((double[])this.data)[index];
         default:
            throw new ClassCastException();
      }
   }

   public String getAsString(int index) {
      return ((String[])this.data)[index];
   }

   public int[] getAsSRational(int index) {
      return ((int[][])this.data)[index];
   }

   public long[] getAsRational(int index) {
      return ((long[][])this.data)[index];
   }

   public String getValueAsString(int index) {
      switch (this.type) {
         case 1:
         case 7:
            return Integer.toString(((byte[])this.data)[index] & 255);
         case 2:
            return ((String[])this.data)[index];
         case 3:
            return Integer.toString(((char[])this.data)[index] & '\uffff');
         case 4:
         case 13:
            return Long.toString(((long[])this.data)[index]);
         case 5:
            long[] lvalue = this.getAsRational(index);
            String rationalString;
            if (lvalue[1] != 0L && lvalue[0] % lvalue[1] == 0L) {
               rationalString = Long.toString(lvalue[0] / lvalue[1]) + "/1";
            } else {
               rationalString = Long.toString(lvalue[0]) + "/" + Long.toString(lvalue[1]);
            }

            return rationalString;
         case 6:
            return Integer.toString(((byte[])this.data)[index]);
         case 8:
            return Integer.toString(((short[])this.data)[index]);
         case 9:
            return Integer.toString(((int[])this.data)[index]);
         case 10:
            int[] ivalue = this.getAsSRational(index);
            String srationalString;
            if (ivalue[1] != 0 && ivalue[0] % ivalue[1] == 0) {
               srationalString = Integer.toString(ivalue[0] / ivalue[1]) + "/1";
            } else {
               srationalString = Integer.toString(ivalue[0]) + "/" + Integer.toString(ivalue[1]);
            }

            return srationalString;
         case 11:
            return Float.toString(((float[])this.data)[index]);
         case 12:
            return Double.toString(((double[])this.data)[index]);
         default:
            throw new ClassCastException();
      }
   }

   @Override
   public int compareTo(Object o) {
      if (o == null) {
         throw new IllegalArgumentException();
      } else {
         int oTagNumber = ((TIFFField)o).getTagNumber();
         if (this.tagNumber < oTagNumber) {
            return -1;
         } else {
            return this.tagNumber > oTagNumber ? 1 : 0;
         }
      }
   }
}
