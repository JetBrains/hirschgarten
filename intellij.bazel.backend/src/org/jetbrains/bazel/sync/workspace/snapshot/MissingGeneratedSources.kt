package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocationCollection

/** Generated sources that did not exist during the sync. */
@ApiStatus.Internal
data class MissingGeneratedSources(val sources: OutputLocationCollection) : BuildTargetData
