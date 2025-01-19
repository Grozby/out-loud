package com.github.jaiimageio.impl.plugins.gif;

import javax.imageio.metadata.IIOInvalidTreeException;
import org.w3c.dom.Node;

class GIFWritableStreamMetadata extends GIFStreamMetadata {
   static final String NATIVE_FORMAT_NAME = "javax_imageio_gif_stream_1.0";

   public GIFWritableStreamMetadata() {
      super(true, "javax_imageio_gif_stream_1.0", "com.github.jaiimageio.impl.plugins.gif.GIFStreamMetadataFormat", null, null);
      this.reset();
   }

   @Override
   public boolean isReadOnly() {
      return false;
   }

   @Override
   public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
      if (formatName.equals("javax_imageio_gif_stream_1.0")) {
         if (root == null) {
            throw new IllegalArgumentException("root == null!");
         }

         this.mergeNativeTree(root);
      } else {
         if (!formatName.equals("javax_imageio_1.0")) {
            throw new IllegalArgumentException("Not a recognized format!");
         }

         if (root == null) {
            throw new IllegalArgumentException("root == null!");
         }

         this.mergeStandardTree(root);
      }
   }

   @Override
   public void reset() {
      this.version = null;
      this.logicalScreenWidth = -1;
      this.logicalScreenHeight = -1;
      this.colorResolution = -1;
      this.pixelAspectRatio = 0;
      this.backgroundColorIndex = 0;
      this.sortFlag = false;
      this.globalColorTable = null;
   }

   @Override
   protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
      if (!root.getNodeName().equals("javax_imageio_gif_stream_1.0")) {
         fatal(root, "Root must be javax_imageio_gif_stream_1.0");
      }

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
         String name = node.getNodeName();
         if (name.equals("Version")) {
            this.version = getStringAttribute(node, "value", null, true, versionStrings);
         } else if (name.equals("LogicalScreenDescriptor")) {
            this.logicalScreenWidth = getIntAttribute(node, "logicalScreenWidth", -1, true, true, 1, 65535);
            this.logicalScreenHeight = getIntAttribute(node, "logicalScreenHeight", -1, true, true, 1, 65535);
            this.colorResolution = getIntAttribute(node, "colorResolution", -1, true, true, 1, 8);
            this.pixelAspectRatio = getIntAttribute(node, "pixelAspectRatio", 0, true, true, 0, 255);
         } else if (name.equals("GlobalColorTable")) {
            int sizeOfGlobalColorTable = getIntAttribute(node, "sizeOfGlobalColorTable", true, 2, 256);
            if (sizeOfGlobalColorTable != 2
               && sizeOfGlobalColorTable != 4
               && sizeOfGlobalColorTable != 8
               && sizeOfGlobalColorTable != 16
               && sizeOfGlobalColorTable != 32
               && sizeOfGlobalColorTable != 64
               && sizeOfGlobalColorTable != 128
               && sizeOfGlobalColorTable != 256) {
               fatal(node, "Bad value for GlobalColorTable attribute sizeOfGlobalColorTable!");
            }

            this.backgroundColorIndex = getIntAttribute(node, "backgroundColorIndex", 0, true, true, 0, 255);
            this.sortFlag = getBooleanAttribute(node, "sortFlag", false, true);
            this.globalColorTable = this.getColorTable(node, "ColorTableEntry", true, sizeOfGlobalColorTable);
         } else {
            fatal(node, "Unknown child of root node!");
         }
      }
   }

   @Override
   protected void mergeStandardTree(Node root) throws IIOInvalidTreeException {
      if (!root.getNodeName().equals("javax_imageio_1.0")) {
         fatal(root, "Root must be javax_imageio_1.0");
      }

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
         String name = node.getNodeName();
         if (name.equals("Chroma")) {
            for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
               String childName = childNode.getNodeName();
               if (childName.equals("Palette")) {
                  this.globalColorTable = this.getColorTable(childNode, "PaletteEntry", false, -1);
               } else if (childName.equals("BackgroundIndex")) {
                  this.backgroundColorIndex = getIntAttribute(childNode, "value", -1, true, true, 0, 255);
               }
            }
         } else if (name.equals("Data")) {
            for (Node childNodex = node.getFirstChild(); childNodex != null; childNodex = childNodex.getNextSibling()) {
               String childName = childNodex.getNodeName();
               if (childName.equals("BitsPerSample")) {
                  this.colorResolution = getIntAttribute(childNodex, "value", -1, true, true, 1, 8);
                  break;
               }
            }
         } else if (name.equals("Dimension")) {
            for (Node childNodexx = node.getFirstChild(); childNodexx != null; childNodexx = childNodexx.getNextSibling()) {
               String childName = childNodexx.getNodeName();
               if (childName.equals("PixelAspectRatio")) {
                  float aspectRatio = getFloatAttribute(childNodexx, "value");
                  if (aspectRatio == 1.0F) {
                     this.pixelAspectRatio = 0;
                  } else {
                     int ratio = (int)(aspectRatio * 64.0F - 15.0F);
                     this.pixelAspectRatio = Math.max(Math.min(ratio, 255), 0);
                  }
               } else if (childName.equals("HorizontalScreenSize")) {
                  this.logicalScreenWidth = getIntAttribute(childNodexx, "value", -1, true, true, 1, 65535);
               } else if (childName.equals("VerticalScreenSize")) {
                  this.logicalScreenHeight = getIntAttribute(childNodexx, "value", -1, true, true, 1, 65535);
               }
            }
         } else if (name.equals("Document")) {
            for (Node childNodexxx = node.getFirstChild(); childNodexxx != null; childNodexxx = childNodexxx.getNextSibling()) {
               String childName = childNodexxx.getNodeName();
               if (childName.equals("FormatVersion")) {
                  String formatVersion = getStringAttribute(childNodexxx, "value", null, true, null);

                  for (int i = 0; i < versionStrings.length; i++) {
                     if (formatVersion.equals(versionStrings[i])) {
                        this.version = formatVersion;
                        break;
                     }
                  }
                  break;
               }
            }
         }
      }
   }

   @Override
   public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
      this.reset();
      this.mergeTree(formatName, root);
   }
}
