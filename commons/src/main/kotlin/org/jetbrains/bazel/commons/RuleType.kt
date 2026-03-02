package org.jetbrains.bazel.commons

import org.jetbrains.annotations.ApiStatus

/** The general type of a rule (e.g. test, binary, etc.).  */
@ApiStatus.Internal
enum class RuleType {
  TEST,
  BINARY,
  LIBRARY,
  UNKNOWN,
}
