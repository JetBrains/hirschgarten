package org.jetbrains.bsp.protocol

enum class RustTargetKind(val value: Int) {
  LIB(1),
  BIN(2),
  TEST(3),
  EXAPMLE(4),
  BENCH(5),
  CUSTOM_BUILD(6),
  UNKNOWN(7),
}
