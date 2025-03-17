package org.jetbrains.bsp.protocol.og

/** The general type of a rule (e.g. test, binary, etc.).  */
enum class RuleType {
  TEST,
  BINARY,
  LIBRARY,
  UNKNOWN,
}
