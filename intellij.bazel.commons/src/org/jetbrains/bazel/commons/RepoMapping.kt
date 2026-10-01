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
  val nonLocalCanonicalRepoNames: Set<String>,
) : RepoMapping {
  val canonicalRepoNameToApparentName: Map<String, String> =
    apparentRepoNameToCanonicalName.entries.associate { (apparent, canonical) -> canonical to apparent }.toSortedMap()
}

@ApiStatus.Internal
data object RepoMappingDisabled : RepoMapping

@ApiStatus.Internal
data class LocalRepositoryMapping(val localRepositories: Map<String, Path>)

/**
 * A mapping without local repositories.
 * Use it for a generated artifact or an existence check, which the local override does not change.
 */
@ApiStatus.Internal
val NoLocalRepositories: LocalRepositoryMapping = LocalRepositoryMapping(emptyMap())

@ApiStatus.Internal
fun RepoMapping.getLocalRepositories() = LocalRepositoryMapping((this as? BzlmodRepoMapping)?.canonicalRepoNameToLocalPath ?: emptyMap())
