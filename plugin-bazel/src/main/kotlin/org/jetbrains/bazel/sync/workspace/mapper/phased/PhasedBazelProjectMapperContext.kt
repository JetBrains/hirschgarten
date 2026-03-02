package org.jetbrains.bazel.sync.workspace.mapper.phased

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping

@ApiStatus.Internal
data class PhasedBazelProjectMapperContext(val repoMapping: RepoMapping)
