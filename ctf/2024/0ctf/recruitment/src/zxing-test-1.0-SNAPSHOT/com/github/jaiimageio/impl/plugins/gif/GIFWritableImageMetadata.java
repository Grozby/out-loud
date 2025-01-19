package com.github.jaiimageio.impl.plugins.gif;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

class GIFWritableImageMetadata extends GIFImageMetadata {
   static final String NATIVE_FORMAT_NAME = "javax_imageio_gif_image_1.0";

   GIFWritableImageMetadata() {
      super(true, "javax_imageio_gif_image_1.0", "com.github.jaiimageio.impl.plugins.gif.GIFImageMetadataFormat", null, null);
   }

   @Override
   public boolean isReadOnly() {
      return false;
   }

   @Override
   public void reset() {
      this.imageLeftPosition = 0;
      this.imageTopPosition = 0;
      this.imageWidth = 0;
      this.imageHeight = 0;
      this.interlaceFlag = false;
      this.sortFlag = false;
      this.localColorTable = null;
      this.disposalMethod = 0;
      this.userInputFlag = false;
      this.transparentColorFlag = false;
      this.delayTime = 0;
      this.transparentColorIndex = 0;
      this.hasPlainTextExtension = false;
      this.textGridLeft = 0;
      this.textGridTop = 0;
      this.textGridWidth = 0;
      this.textGridHeight = 0;
      this.characterCellWidth = 0;
      this.characterCellHeight = 0;
      this.textForegroundColor = 0;
      this.textBackgroundColor = 0;
      this.text = null;
      this.applicationIDs = null;
      this.authenticationCodes = null;
      this.applicationData = null;
      this.comments = null;
   }

   private byte[] fromISO8859(String data) {
      try {
         return data.getBytes("ISO-8859-1");
      } catch (UnsupportedEncodingException var3) {
         return new String("").getBytes();
      }
   }

   @Override
   protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
      if (!root.getNodeName().equals("javax_imageio_gif_image_1.0")) {
         fatal(root, "Root must be javax_imageio_gif_image_1.0");
      }

      for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
         String name = node.getNodeName();
         if (name.equals("ImageDescriptor")) {
            this.imageLeftPosition = getIntAttribute(node, "imageLeftPosition", -1, true, true, 0, 65535);
            this.imageTopPosition = getIntAttribute(node, "imageTopPosition", -1, true, true, 0, 65535);
            this.imageWidth = getIntAttribute(node, "imageWidth", -1, true, true, 1, 65535);
            this.imageHeight = getIntAttribute(node, "imageHeight", -1, true, true, 1, 65535);
            this.interlaceFlag = getBooleanAttribute(node, "interlaceFlag", false, true);
         } else if (name.equals("LocalColorTable")) {
            int sizeOfLocalColorTable = getIntAttribute(node, "sizeOfLocalColorTable", true, 2, 256);
            if (sizeOfLocalColorTable != 2
               && sizeOfLocalColorTable != 4
               && sizeOfLocalColorTable != 8
               && sizeOfLocalColorTable != 16
               && sizeOfLocalColorTable != 32
               && sizeOfLocalColorTable != 64
               && sizeOfLocalColorTable != 128
               && sizeOfLocalColorTable != 256) {
               fatal(node, "Bad value for LocalColorTable attribute sizeOfLocalColorTable!");
            }

            this.sortFlag = getBooleanAttribute(node, "sortFlag", false, true);
            this.localColorTable = this.getColorTable(node, "ColorTableEntry", true, sizeOfLocalColorTable);
         } else if (name.equals("GraphicControlExtension")) {
            String disposalMethodName = getStringAttribute(node, "disposalMethod", null, true, disposalMethodNames);
            this.disposalMethod = 0;

            while (!disposalMethodName.equals(disposalMethodNames[this.disposalMethod])) {
               this.disposalMethod++;
            }

            this.userInputFlag = getBooleanAttribute(node, "userInputFlag", false, true);
            this.transparentColorFlag = getBooleanAttribute(node, "transparentColorFlag", false, true);
            this.delayTime = getIntAttribute(node, "delayTime", -1, true, true, 0, 65535);
            this.transparentColorIndex = getIntAttribute(node, "transparentColorIndex", -1, true, true, 0, 65535);
         } else if (name.equals("PlainTextExtension")) {
            this.hasPlainTextExtension = true;
            this.textGridLeft = getIntAttribute(node, "textGridLeft", -1, true, true, 0, 65535);
            this.textGridTop = getIntAttribute(node, "textGridTop", -1, true, true, 0, 65535);
            this.textGridWidth = getIntAttribute(node, "textGridWidth", -1, true, true, 1, 65535);
            this.textGridHeight = getIntAttribute(node, "textGridHeight", -1, true, true, 1, 65535);
            this.characterCellWidth = getIntAttribute(node, "characterCellWidth", -1, true, true, 1, 65535);
            this.characterCellHeight = getIntAttribute(node, "characterCellHeight", -1, true, true, 1, 65535);
            this.textForegroundColor = getIntAttribute(node, "textForegroundColor", -1, true, true, 0, 255);
            this.textBackgroundColor = getIntAttribute(node, "textBackgroundColor", -1, true, true, 0, 255);
            String textString = getStringAttribute(node, "text", "", false, null);
            this.text = this.fromISO8859(textString);
         } else if (name.equals("ApplicationExtensions")) {
            IIOMetadataNode applicationExtension = (IIOMetadataNode)node.getFirstChild();
            if (!applicationExtension.getNodeName().equals("ApplicationExtension")) {
               fatal(node, "Only a ApplicationExtension may be a child of a ApplicationExtensions!");
            }

            String applicationIDString = getStringAttribute(applicationExtension, "applicationID", null, true, null);
            String authenticationCodeString = getStringAttribute(applicationExtension, "authenticationCode", null, true, null);
            Object applicationExtensionData = applicationExtension.getUserObject();
            if (applicationExtensionData == null || !(applicationExtensionData instanceof byte[])) {
               fatal(applicationExtension, "Bad user object in ApplicationExtension!");
            }

            if (this.applicationIDs == null) {
               this.applicationIDs = new ArrayList();
               this.authenticationCodes = new ArrayList();
               this.applicationData = new ArrayList();
            }

            this.applicationIDs.add(this.fromISO8859(applicationIDString));
            this.authenticationCodes.add(this.fromISO8859(authenticationCodeString));
            this.applicationData.add(applicationExtensionData);
         } else if (name.equals("CommentExtensions")) {
            Node commentExtension = node.getFirstChild();
            if (commentExtension != null) {
               while (commentExtension != null) {
                  if (!commentExtension.getNodeName().equals("CommentExtension")) {
                     fatal(node, "Only a CommentExtension may be a child of a CommentExtensions!");
                  }

                  if (this.comments == null) {
                     this.comments = new ArrayList();
                  }

                  String comment = getStringAttribute(commentExtension, "value", null, true, null);
                  this.comments.add(this.fromISO8859(comment));
                  commentExtension = commentExtension.getNextSibling();
               }
            }
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
                  this.localColorTable = this.getColorTable(childNode, "PaletteEntry", false, -1);
                  break;
               }
            }
         } else if (name.equals("Compression")) {
            for (Node childNodex = node.getFirstChild(); childNodex != null; childNodex = childNodex.getNextSibling()) {
               String childName = childNodex.getNodeName();
               if (childName.equals("NumProgressiveScans")) {
                  int numProgressiveScans = getIntAttribute(childNodex, "value", 4, false, true, 1, Integer.MAX_VALUE);
                  if (numProgressiveScans > 1) {
                     this.interlaceFlag = true;
                  }
                  break;
               }
            }
         } else if (name.equals("Dimension")) {
            for (Node childNodexx = node.getFirstChild(); childNodexx != null; childNodexx = childNodexx.getNextSibling()) {
               String childName = childNodexx.getNodeName();
               if (childName.equals("HorizontalPixelOffset")) {
                  this.imageLeftPosition = getIntAttribute(childNodexx, "value", -1, true, true, 0, 65535);
               } else if (childName.equals("VerticalPixelOffset")) {
                  this.imageTopPosition = getIntAttribute(childNodexx, "value", -1, true, true, 0, 65535);
               }
            }
         } else if (name.equals("Text")) {
            for (Node childNodexxx = node.getFirstChild(); childNodexxx != null; childNodexxx = childNodexxx.getNextSibling()) {
               String childName = childNodexxx.getNodeName();
               if (childName.equals("TextEntry")
                  && getAttribute(childNodexxx, "compression", "none", false).equals("none")
                  && Charset.isSupported(getAttribute(childNodexxx, "encoding", "ISO-8859-1", false))) {
                  String value = getAttribute(childNodexxx, "value");
                  byte[] comment = this.fromISO8859(value);
                  if (this.comments == null) {
                     this.comments = new ArrayList();
                  }

                  this.comments.add(comment);
               }
            }
         } else if (name.equals("Transparency")) {
            for (Node childNodexxxx = node.getFirstChild(); childNodexxxx != null; childNodexxxx = childNodexxxx.getNextSibling()) {
               String childName = childNodexxxx.getNodeName();
               if (childName.equals("TransparentIndex")) {
                  this.transparentColorIndex = getIntAttribute(childNodexxxx, "value", -1, true, true, 0, 255);
                  this.transparentColorFlag = true;
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
