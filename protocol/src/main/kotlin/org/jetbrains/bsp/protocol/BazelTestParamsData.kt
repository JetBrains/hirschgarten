package org.jetbrains.bsp.protocol

public data class BazelTestParamsData(val coverage: Boolean?, val testFilter: String?) {
  companion object {
    const val DATA_KIND = "bazel-test"
  }
}
