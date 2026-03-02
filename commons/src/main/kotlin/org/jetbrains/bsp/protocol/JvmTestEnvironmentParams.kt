package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

internal data class JvmTestEnvironmentParams(val targets: List<Label>, val originId: String? = null)
