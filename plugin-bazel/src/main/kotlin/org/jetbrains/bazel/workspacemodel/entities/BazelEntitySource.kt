package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import org.jetbrains.annotations.ApiStatus

internal sealed interface BazelEntitySource : EntitySource

@ApiStatus.Internal
data object BazelProjectEntitySource : BazelEntitySource

@ApiStatus.Internal
data class BazelModuleEntitySource(val moduleName: String) : BazelEntitySource

@ApiStatus.Internal
data object BazelDummyEntitySource : BazelEntitySource
