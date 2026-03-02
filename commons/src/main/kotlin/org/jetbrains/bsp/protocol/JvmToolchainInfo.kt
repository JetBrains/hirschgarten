package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

// Underscores needed because this class is used with Gson to parse Bazel's output
@Suppress("PropertyName")
@ApiStatus.Internal
data class JvmToolchainInfo(
  val java_home: String,
  val toolchain_path: String,
  val jvm_opts: List<String>,
)
