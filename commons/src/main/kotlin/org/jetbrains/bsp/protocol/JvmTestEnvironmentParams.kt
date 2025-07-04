package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel

data class JvmTestEnvironmentParams(val targets: List<CanonicalLabel>, val originId: String? = null)
