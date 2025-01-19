package com.github.jaiimageio.impl.plugins.tiff;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.imageio.metadata.IIOMetadataFormat;

public abstract class TIFFMetadataFormat implements IIOMetadataFormat {
   protected Map elementInfoMap = new HashMap();
   protected Map attrInfoMap = new HashMap();
   protected String resourceBaseName;
   protected String rootName;

   @Override
   public String getRootName() {
      return this.rootName;
   }

   private String getResource(String key, Locale locale) {
      if (locale == null) {
         locale = Locale.getDefault();
      }

      try {
         ResourceBundle bundle = ResourceBundle.getBundle(this.resourceBaseName, locale);
         return bundle.getString(key);
      } catch (MissingResourceException var4) {
         return null;
      }
   }

   private TIFFElementInfo getElementInfo(String elementName) {
      if (elementName == null) {
         throw new IllegalArgumentException("elementName == null!");
      } else {
         TIFFElementInfo info = (TIFFElementInfo)this.elementInfoMap.get(elementName);
         if (info == null) {
            throw new IllegalArgumentException("No such element: " + elementName);
         } else {
            return info;
         }
      }
   }

   private TIFFAttrInfo getAttrInfo(String elementName, String attrName) {
      if (elementName == null) {
         throw new IllegalArgumentException("elementName == null!");
      } else if (attrName == null) {
         throw new IllegalArgumentException("attrName == null!");
      } else {
         String key = elementName + "/" + attrName;
         TIFFAttrInfo info = (TIFFAttrInfo)this.attrInfoMap.get(key);
         if (info == null) {
            throw new IllegalArgumentException("No such attribute: " + key);
         } else {
            return info;
         }
      }
   }

   @Override
   public int getElementMinChildren(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.minChildren;
   }

   @Override
   public int getElementMaxChildren(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.maxChildren;
   }

   @Override
   public String getElementDescription(String elementName, Locale locale) {
      if (!this.elementInfoMap.containsKey(elementName)) {
         throw new IllegalArgumentException("No such element: " + elementName);
      } else {
         return this.getResource(elementName, locale);
      }
   }

   @Override
   public int getChildPolicy(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.childPolicy;
   }

   @Override
   public String[] getChildNames(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.childNames;
   }

   @Override
   public String[] getAttributeNames(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.attributeNames;
   }

   @Override
   public int getAttributeValueType(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.valueType;
   }

   @Override
   public int getAttributeDataType(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.dataType;
   }

   @Override
   public boolean isAttributeRequired(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.isRequired;
   }

   @Override
   public String getAttributeDefaultValue(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.defaultValue;
   }

   @Override
   public String[] getAttributeEnumerations(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.enumerations;
   }

   @Override
   public String getAttributeMinValue(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.minValue;
   }

   @Override
   public String getAttributeMaxValue(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.maxValue;
   }

   @Override
   public int getAttributeListMinLength(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.listMinLength;
   }

   @Override
   public int getAttributeListMaxLength(String elementName, String attrName) {
      TIFFAttrInfo info = this.getAttrInfo(elementName, attrName);
      return info.listMaxLength;
   }

   @Override
   public String getAttributeDescription(String elementName, String attrName, Locale locale) {
      String key = elementName + "/" + attrName;
      if (!this.attrInfoMap.containsKey(key)) {
         throw new IllegalArgumentException("No such attribute: " + key);
      } else {
         return this.getResource(key, locale);
      }
   }

   @Override
   public int getObjectValueType(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      return info.objectValueType;
   }

   @Override
   public Class getObjectClass(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectClass;
      }
   }

   @Override
   public Object getObjectDefaultValue(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectDefaultValue;
      }
   }

   @Override
   public Object[] getObjectEnumerations(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectEnumerations;
      }
   }

   @Override
   public Comparable getObjectMinValue(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectMinValue;
      }
   }

   @Override
   public Comparable getObjectMaxValue(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectMaxValue;
      }
   }

   @Override
   public int getObjectArrayMinLength(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectArrayMinLength;
      }
   }

   @Override
   public int getObjectArrayMaxLength(String elementName) {
      TIFFElementInfo info = this.getElementInfo(elementName);
      if (info.objectValueType == 0) {
         throw new IllegalArgumentException("Element cannot contain an object value: " + elementName);
      } else {
         return info.objectArrayMaxLength;
      }
   }
}
