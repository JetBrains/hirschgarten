package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled

// this class handles cases when user defines custom bzlmod repo_name
// e.g. bazel_dep(name = "protobuf", version = "32.0", repo_name = "my_protobuf")
class ProtobufRepoMappings(private val repoMapping: RepoMapping) {
  fun getMappedProtobufRepoName(canonicalRuleName: String, apparentRuleName: String): String? {
    return when (repoMapping) {
      is BzlmodRepoMapping -> {
        if (canonicalRuleName.startsWith("@")) {
          val canonicalRuleName = canonicalRuleName.removePrefix("@") + "+"
          repoMapping.apparentRepoNameToCanonicalName.entries
            .firstOrNull { it.value == canonicalRuleName }?.key ?: apparentRuleName
        } else {
          apparentRuleName
        }
      }
      RepoMappingDisabled -> {
        apparentRuleName
      }
    }
  }
}
