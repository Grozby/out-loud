package com.google.zxing;

public enum EncodeHintType {
   ERROR_CORRECTION,
   CHARACTER_SET,
   DATA_MATRIX_SHAPE,
   DATA_MATRIX_COMPACT,
   @Deprecated
   MIN_SIZE,
   @Deprecated
   MAX_SIZE,
   MARGIN,
   PDF417_COMPACT,
   PDF417_COMPACTION,
   PDF417_DIMENSIONS,
   PDF417_AUTO_ECI,
   AZTEC_LAYERS,
   QR_VERSION,
   QR_MASK_PATTERN,
   QR_COMPACT,
   GS1_FORMAT,
   FORCE_CODE_SET,
   FORCE_C40,
   CODE128_COMPACT;
}
