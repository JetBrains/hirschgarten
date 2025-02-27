@file:Suppress("MatchingDeclarationName")

package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.BuildTargetIdentifier

public data class WorkspaceInvalidTargetsResult(val targets: List<BuildTargetIdentifier>)
