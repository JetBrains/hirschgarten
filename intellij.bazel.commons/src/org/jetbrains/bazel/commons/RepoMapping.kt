package org.jetbrains.bazel.commons

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
sealed interface RepoMapping

@ApiStatus.Internal
data class BzlmodRepoMapping(
  val canonicalRepoNameToLocalPath: Map<String, Path>,
  val apparentRepoNameToCanonicalName: Map<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
) : RepoMapping {
  val canonicalRepoNameToApparentName: Map<String, String> =
    apparentRepoNameToCanonicalName.entries.associate { (apparent, canonical) -> canonical to apparent }.toSortedMap()
}

@ApiStatus.Internal
data object RepoMappingDisabled : RepoMapping

@ApiStatus.Internal
data class LocalRepositoryMapping(val localRepositories: Map<String, Path>)

@ApiStatus.Internal
fun RepoMapping.getLocalRepositories() = LocalRepositoryMapping((this as? BzlmodRepoMapping)?.canonicalRepoNameToLocalPath ?: mapOf())
