package org.jetbrains.bsp.protocol

data class JvmToolchainInfo(val java_home: String, val toolchain_path: String, val jvm_opts: List<String>)
