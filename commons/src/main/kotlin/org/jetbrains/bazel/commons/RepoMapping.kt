package org.jetbrains.bazel.commons

import com.intellij.util.containers.BidirectionalMap
import java.nio.file.Path

sealed interface RepoMapping

data class BzlmodRepoMapping(
  val canonicalRepoNameToLocalPath: Map<String, Path>,
  val apparentRepoNameToCanonicalName: BidirectionalMap<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
) : RepoMapping

data object RepoMappingDisabled : RepoMapping
