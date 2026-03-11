package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping

@ApiStatus.Internal
data class WorkspaceBazelRepoMappingResult(val repoMapping: RepoMapping)
