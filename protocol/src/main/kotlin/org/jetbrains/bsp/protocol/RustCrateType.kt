package org.jetbrains.bsp.protocol

enum class RustCrateType(val value: Int) {
  BIN(1),
  LIB(2),
  RLIB(3),
  DYLIB(4),
  CDYLIB(5),
  STATICLIB(6),
  PROC_MACRO(7),
  UNKNOWN(8),
}
