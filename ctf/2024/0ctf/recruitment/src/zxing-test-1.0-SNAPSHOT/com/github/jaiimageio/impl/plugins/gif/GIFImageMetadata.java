package com.github.jaiimageio.impl.plugins.gif;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

public class GIFImageMetadata extends GIFMetadata {
   static final String nativeMetadataFormatName = "javax_imageio_gif_image_1.0";
   static final String[] disposalMethodNames = new String[]{
      "none",
      "doNotDispose",
      "restoreToBackgroundColor",
      "restoreToPrevious",
      "undefinedDisposalMethod4",
      "undefinedDisposalMethod5",
      "undefinedDisposalMethod6",
      "undefinedDisposalMethod7"
   };
   public int imageLeftPosition;
   public int imageTopPosition;
   public int imageWidth;
   public int imageHeight;
   public boolean interlaceFlag = false;
   public boolean sortFlag = false;
   public byte[] localColorTable = null;
   public int disposalMethod = 0;
   public boolean userInputFlag = false;
   public boolean transparentColorFlag = false;
   public int delayTime = 0;
   public int transparentColorIndex = 0;
   public boolean hasPlainTextExtension = false;
   public int textGridLeft;
   public int textGridTop;
   public int textGridWidth;
   public int textGridHeight;
   public int characterCellWidth;
   public int characterCellHeight;
   public int textForegroundColor;
   public int textBackgroundColor;
   public byte[] text;
   public List applicationIDs = null;
   public List authenticationCodes = null;
   public List applicationData = null;
   public List comments = null;

   protected GIFImageMetadata(
      boolean standardMetadataFormatSupported,
      String nativeMetadataFormatName,
      String nativeMetadataFormatClassName,
      String[] extraMetadataFormatNames,
      String[] extraMetadataFormatClassNames
   ) {
      super(standardMetadataFormatSupported, nativeMetadataFormatName, nativeMetadataFormatClassName, extraMetadataFormatNames, extraMetadataFormatClassNames);
   }

   public GIFImageMetadata() {
      this(true, "javax_imageio_gif_image_1.0", "com.github.jaiimageio.impl.plugins.gif.GIFImageMetadataFormat", null, null);
   }

   @Override
   public boolean isReadOnly() {
      return true;
   }

   @Override
   public Node getAsTree(String formatName) {
      if (formatName.equals("javax_imageio_gif_image_1.0")) {
         return this.getNativeTree();
      } else if (formatName.equals("javax_imageio_1.0")) {
         return this.getStandardTree();
      } else {
         throw new IllegalArgumentException("Not a recognized format!");
      }
   }

   private String toISO8859(byte[] data) {
      try {
         return new String(data, "ISO-8859-1");
      } catch (UnsupportedEncodingException var3) {
         return "";
      }
   }

   private Node getNativeTree() {
      IIOMetadataNode root = new IIOMetadataNode("javax_imageio_gif_image_1.0");
      IIOMetadataNode node = new IIOMetadataNode("ImageDescriptor");
      node.setAttribute("imageLeftPosition", Integer.toString(this.imageLeftPosition));
      node.setAttribute("imageTopPosition", Integer.toString(this.imageTopPosition));
      node.setAttribute("imageWidth", Integer.toString(this.imageWidth));
      node.setAttribute("imageHeight", Integer.toString(this.imageHeight));
      node.setAttribute("interlaceFlag", this.interlaceFlag ? "true" : "false");
      root.appendChild(node);
      if (this.localColorTable != null) {
         node = new IIOMetadataNode("LocalColorTable");
         int numEntries = this.localColorTable.length / 3;
         node.setAttribute("sizeOfLocalColorTable", Integer.toString(numEntries));
         node.setAttribute("sortFlag", this.sortFlag ? "TRUE" : "FALSE");

         for (int i = 0; i < numEntries; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("ColorTableEntry");
            entry.setAttribute("index", Integer.toString(i));
            int r = this.localColorTable[3 * i] & 255;
            int g = this.localColorTable[3 * i + 1] & 255;
            int b = this.localColorTable[3 * i + 2] & 255;
            entry.setAttribute("red", Integer.toString(r));
            entry.setAttribute("green", Integer.toString(g));
            entry.setAttribute("blue", Integer.toString(b));
            node.appendChild(entry);
         }

         root.appendChild(node);
      }

      node = new IIOMetadataNode("GraphicControlExtension");
      node.setAttribute("disposalMethod", disposalMethodNames[this.disposalMethod]);
      node.setAttribute("userInputFlag", this.userInputFlag ? "true" : "false");
      node.setAttribute("transparentColorFlag", this.transparentColorFlag ? "true" : "false");
      node.setAttribute("delayTime", Integer.toString(this.delayTime));
      node.setAttribute("transparentColorIndex", Integer.toString(this.transparentColorIndex));
      root.appendChild(node);
      if (this.hasPlainTextExtension) {
         node = new IIOMetadataNode("PlainTextExtension");
         node.setAttribute("textGridLeft", Integer.toString(this.textGridLeft));
         node.setAttribute("textGridTop", Integer.toString(this.textGridTop));
         node.setAttribute("textGridWidth", Integer.toString(this.textGridWidth));
         node.setAttribute("textGridHeight", Integer.toString(this.textGridHeight));
         node.setAttribute("characterCellWidth", Integer.toString(this.characterCellWidth));
         node.setAttribute("characterCellHeight", Integer.toString(this.characterCellHeight));
         node.setAttribute("textForegroundColor", Integer.toString(this.textForegroundColor));
         node.setAttribute("textBackgroundColor", Integer.toString(this.textBackgroundColor));
         node.setAttribute("text", this.toISO8859(this.text));
         root.appendChild(node);
      }

      int numAppExtensions = this.applicationIDs == null ? 0 : this.applicationIDs.size();
      if (numAppExtensions > 0) {
         node = new IIOMetadataNode("ApplicationExtensions");

         for (int i = 0; i < numAppExtensions; i++) {
            IIOMetadataNode appExtNode = new IIOMetadataNode("ApplicationExtension");
            byte[] applicationID = (byte[])this.applicationIDs.get(i);
            appExtNode.setAttribute("applicationID", this.toISO8859(applicationID));
            byte[] authenticationCode = (byte[])this.authenticationCodes.get(i);
            appExtNode.setAttribute("authenticationCode", this.toISO8859(authenticationCode));
            byte[] appData = (byte[])this.applicationData.get(i);
            appExtNode.setUserObject((byte[])appData.clone());
            node.appendChild(appExtNode);
         }

         root.appendChild(node);
      }

      int numComments = this.comments == null ? 0 : this.comments.size();
      if (numComments > 0) {
         node = new IIOMetadataNode("CommentExtensions");

         for (int i = 0; i < numComments; i++) {
            IIOMetadataNode commentNode = new IIOMetadataNode("CommentExtension");
            byte[] comment = (byte[])this.comments.get(i);
            commentNode.setAttribute("value", this.toISO8859(comment));
            node.appendChild(commentNode);
         }

         root.appendChild(node);
      }

      return root;
   }

   @Override
   public IIOMetadataNode getStandardChromaNode() {
      IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("ColorSpaceType");
      node.setAttribute("name", "RGB");
      chroma_node.appendChild(node);
      node = new IIOMetadataNode("NumChannels");
      node.setAttribute("value", this.transparentColorFlag ? "4" : "3");
      chroma_node.appendChild(node);
      node = new IIOMetadataNode("BlackIsZero");
      node.setAttribute("value", "TRUE");
      chroma_node.appendChild(node);
      if (this.localColorTable != null) {
         node = new IIOMetadataNode("Palette");
         int numEntries = this.localColorTable.length / 3;

         for (int i = 0; i < numEntries; i++) {
            IIOMetadataNode entry = new IIOMetadataNode("PaletteEntry");
            entry.setAttribute("index", Integer.toString(i));
            entry.setAttribute("red", Integer.toString(this.localColorTable[3 * i] & 255));
            entry.setAttribute("green", Integer.toString(this.localColorTable[3 * i + 1] & 255));
            entry.setAttribute("blue", Integer.toString(this.localColorTable[3 * i + 2] & 255));
            node.appendChild(entry);
         }

         chroma_node.appendChild(node);
      }

      return chroma_node;
   }

   @Override
   public IIOMetadataNode getStandardCompressionNode() {
      IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("CompressionTypeName");
      node.setAttribute("value", "lzw");
      compression_node.appendChild(node);
      node = new IIOMetadataNode("Lossless");
      node.setAttribute("value", "TRUE");
      compression_node.appendChild(node);
      node = new IIOMetadataNode("NumProgressiveScans");
      node.setAttribute("value", this.interlaceFlag ? "4" : "1");
      compression_node.appendChild(node);
      return compression_node;
   }

   @Override
   public IIOMetadataNode getStandardDataNode() {
      IIOMetadataNode data_node = new IIOMetadataNode("Data");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("SampleFormat");
      node.setAttribute("value", "Index");
      data_node.appendChild(node);
      return data_node;
   }

   @Override
   public IIOMetadataNode getStandardDimensionNode() {
      IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
      IIOMetadataNode node = null;
      node = new IIOMetadataNode("ImageOrientation");
      node.setAttribute("value", "Normal");
      dimension_node.appendChild(node);
      node = new IIOMetadataNode("HorizontalPixelOffset");
      node.setAttribute("value", Integer.toString(this.imageLeftPosition));
      dimension_node.appendChild(node);
      node = new IIOMetadataNode("VerticalPixelOffset");
      node.setAttribute("value", Integer.toString(this.imageTopPosition));
      dimension_node.appendChild(node);
      return dimension_node;
   }

   @Override
   public IIOMetadataNode getStandardTextNode() {
      if (this.comments == null) {
         return null;
      } else {
         Iterator commentIter = this.comments.iterator();
         if (!commentIter.hasNext()) {
            return null;
         } else {
            IIOMetadataNode text_node = new IIOMetadataNode("Text");
            IIOMetadataNode node = null;

            while (commentIter.hasNext()) {
               byte[] comment = (byte[])commentIter.next();
               String s = null;

               try {
                  s = new String(comment, "ISO-8859-1");
               } catch (UnsupportedEncodingException var7) {
                  throw new RuntimeException("Encoding ISO-8859-1 unknown!");
               }

               node = new IIOMetadataNode("TextEntry");
               node.setAttribute("value", s);
               node.setAttribute("encoding", "ISO-8859-1");
               node.setAttribute("compression", "none");
               text_node.appendChild(node);
            }

            return text_node;
         }
      }
   }

   @Override
   public IIOMetadataNode getStandardTransparencyNode() {
      if (!this.transparentColorFlag) {
         return null;
      } else {
         IIOMetadataNode transparency_node = new IIOMetadataNode("Transparency");
         IIOMetadataNode node = null;
         node = new IIOMetadataNode("TransparentIndex");
         node.setAttribute("value", Integer.toString(this.transparentColorIndex));
         transparency_node.appendChild(node);
         return transparency_node;
      }
   }

   @Override
   public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
      throw new IllegalStateException("Metadata is read-only!");
   }

   @Override
   protected void mergeNativeTree(Node root) throws IIOInvalidTreeException {
      throw new IllegalStateException("Metadata is read-only!");
   }

   @Override
   protected void mergeStandardTree(Node root) throws IIOInvalidTreeException {
      throw new IllegalStateException("Metadata is read-only!");
   }

   @Override
   public void reset() {
      throw new IllegalStateException("Metadata is read-only!");
   }
}
