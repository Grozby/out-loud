package com.github.jaiimageio.impl.plugins.tiff;

import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.github.jaiimageio.plugins.tiff.TIFFTagSet;
import java.util.Arrays;
import java.util.List;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

public class TIFFFieldNode extends IIOMetadataNode {
   private boolean isIFD;
   private Boolean isInitialized = Boolean.FALSE;
   private TIFFField field;

   private static String getNodeName(TIFFField f) {
      return f.getData() instanceof TIFFDirectory ? "TIFFIFD" : "TIFFField";
   }

   public TIFFFieldNode(TIFFField field) {
      super(getNodeName(field));
      this.isIFD = field.getData() instanceof TIFFDirectory;
      this.field = field;
      TIFFTag tag = field.getTag();
      int tagNumber = tag.getNumber();
      String tagName = tag.getName();
      if (this.isIFD) {
         if (tagNumber != 0) {
            this.setAttribute("parentTagNumber", Integer.toString(tagNumber));
         }

         if (tagName != null) {
            this.setAttribute("parentTagName", tagName);
         }

         TIFFDirectory dir = (TIFFDirectory)field.getData();
         TIFFTagSet[] tagSets = dir.getTagSets();
         if (tagSets != null) {
            String tagSetNames = "";

            for (int i = 0; i < tagSets.length; i++) {
               tagSetNames = tagSetNames + tagSets[i].getClass().getName();
               if (i != tagSets.length - 1) {
                  tagSetNames = tagSetNames + ",";
               }
            }

            this.setAttribute("tagSets", tagSetNames);
         }
      } else {
         this.setAttribute("number", Integer.toString(tagNumber));
         this.setAttribute("name", tagName);
      }
   }

   private synchronized void initialize() {
      if (this.isInitialized != Boolean.TRUE) {
         if (this.isIFD) {
            TIFFDirectory dir = (TIFFDirectory)this.field.getData();
            TIFFField[] fields = dir.getTIFFFields();
            if (fields != null) {
               TIFFTagSet[] tagSets = dir.getTagSets();
               List tagSetList = Arrays.asList(tagSets);

               for (TIFFField f : fields) {
                  int tagNumber = f.getTagNumber();
                  TIFFTag tag = TIFFIFD.getTag(tagNumber, tagSetList);
                  Node node = f.getAsNativeNode();
                  if (node != null) {
                     this.appendChild(node);
                  }
               }
            }
         } else {
            int count = this.field.getCount();
            IIOMetadataNode child;
            if (this.field.getType() != 7) {
               child = new IIOMetadataNode("TIFF" + TIFFField.getTypeName(this.field.getType()) + "s");
               TIFFTag tag = this.field.getTag();

               for (int i = 0; i < count; i++) {
                  IIOMetadataNode cchild = new IIOMetadataNode("TIFF" + TIFFField.getTypeName(this.field.getType()));
                  cchild.setAttribute("value", this.field.getValueAsString(i));
                  if (tag.hasValueNames() && this.field.isIntegral()) {
                     int value = this.field.getAsInt(i);
                     String name = tag.getValueName(value);
                     if (name != null) {
                        cchild.setAttribute("description", name);
                     }
                  }

                  child.appendChild(cchild);
               }
            } else {
               child = new IIOMetadataNode("TIFFUndefined");
               byte[] data = this.field.getAsBytes();
               StringBuffer sb = new StringBuffer();

               for (int i = 0; i < count; i++) {
                  sb.append(Integer.toString(data[i] & 255));
                  if (i < count - 1) {
                     sb.append(",");
                  }
               }

               child.setAttribute("value", sb.toString());
            }

            this.appendChild(child);
         }

         this.isInitialized = Boolean.TRUE;
      }
   }

   @Override
   public Node appendChild(Node newChild) {
      if (newChild == null) {
         throw new IllegalArgumentException("newChild == null!");
      } else {
         return super.insertBefore(newChild, null);
      }
   }

   @Override
   public boolean hasChildNodes() {
      this.initialize();
      return super.hasChildNodes();
   }

   @Override
   public int getLength() {
      this.initialize();
      return super.getLength();
   }

   @Override
   public Node getFirstChild() {
      this.initialize();
      return super.getFirstChild();
   }

   @Override
   public Node getLastChild() {
      this.initialize();
      return super.getLastChild();
   }

   @Override
   public Node getPreviousSibling() {
      this.initialize();
      return super.getPreviousSibling();
   }

   @Override
   public Node getNextSibling() {
      this.initialize();
      return super.getNextSibling();
   }

   @Override
   public Node insertBefore(Node newChild, Node refChild) {
      this.initialize();
      return super.insertBefore(newChild, refChild);
   }

   @Override
   public Node replaceChild(Node newChild, Node oldChild) {
      this.initialize();
      return super.replaceChild(newChild, oldChild);
   }

   @Override
   public Node removeChild(Node oldChild) {
      this.initialize();
      return super.removeChild(oldChild);
   }

   @Override
   public Node cloneNode(boolean deep) {
      this.initialize();
      return super.cloneNode(deep);
   }
}
