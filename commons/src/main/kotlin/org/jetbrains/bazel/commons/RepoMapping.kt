package org.jetbrains.bazel.commons

import java.nio.file.Path

sealed interface RepoMapping

data class BzlmodRepoMapping(
  val canonicalRepoNameToLocalPath: Map<String, Path>,
  val apparentRepoNameToCanonicalName: Map<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
) : RepoMapping

data object RepoMappingDisabled : RepoMapping
