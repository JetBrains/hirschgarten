package org.jetbrains.bsp.protocol

// Underscores needed because this class is used with Gson to parse Bazel's output
@Suppress("PropertyName")
data class JvmToolchainInfo(
  val java_home: String,
  val toolchain_path: String,
  val jvm_opts: List<String>,
)
