package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label

data class JvmRunEnvironmentParams(val targets: List<CanonicalLabel>, val originId: String? = null)
