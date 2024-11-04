package org.jetbrains.bsp.protocol

data class BazelTestParamsData(
  val coverage: Boolean?,
  val testFilter: String?,
  val additionalBazelParams: String?,
) {
  companion object {
    const val DATA_KIND = "bazel-test"
  }
}
