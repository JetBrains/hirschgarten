package org.jetbrains.bazel.commons

import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SyntheticLabel
import java.nio.file.Path

sealed interface RepoMapping

data class BzlmodRepoMapping(
  val canonicalRepoNameToLocalPath: Map<String, Path>,
  val apparentRepoNameToCanonicalName: BidirectionalMap<String, String>,
  val canonicalRepoNameToPath: Map<String, Path>,
) : RepoMapping

data object RepoMappingDisabled : RepoMapping

fun Label.canonicalize(repoMapping: RepoMapping): Label =
  when (repoMapping) {
    is RepoMappingDisabled -> this
    is BzlmodRepoMapping -> {
      when (this) {
        is ResolvedLabel -> {
          when (this.repo) {
            is Main -> this
            is Canonical -> this
            is Apparent -> {
              val apparentRepoName = this.repoName
              val canonicalRepoName =
                repoMapping.apparentRepoNameToCanonicalName[apparentRepoName] ?: error("No canonical name found for $this")
              this.copy(repo = Canonical.createCanonicalOrMain(canonicalRepoName))
            }
          }
        }

        is RelativeLabel -> error("Relative label $this cannot be canonicalized")
        is SyntheticLabel -> this
      }
    }
  }
