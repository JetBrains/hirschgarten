@file:Suppress("MatchingDeclarationName")

package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

public data class WorkspaceInvalidTargetsResult(val targets: List<Label>)
