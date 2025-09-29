package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource

sealed interface BazelEntitySource : EntitySource

data object BazelProjectEntitySource : BazelEntitySource

data class BazelModuleEntitySource(val moduleName: String) : BazelEntitySource

data object BazelDummyEntitySource : BazelEntitySource
