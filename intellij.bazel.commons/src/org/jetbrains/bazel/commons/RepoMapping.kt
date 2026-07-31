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

  /**
   * Resolves the canonical repo name (e.g. `"protobuf+"`) of the module named [rulesetModuleName].
   *
   * Prefers a direct apparent-name match, but falls back to locating the module's canonical repo when
   * the root module gave it a non-default apparent name -- e.g. protobuf declared with
   * `repo_name = "com_google_protobuf"`, which removes `"protobuf"` from [apparentRepoNameToCanonicalName]
   * and would otherwise leave aspects loading the non-existent `@protobuf`.
   *
   * A top-level module's canonical repo is `"<module>+"` (Bazel 8+) or `"<module>~<version>"` (older
   * Bazel); extension-generated repos carry extra segments (e.g. `"gazelle++go_deps+..."`), so the
   * exact/prefix match isolates the module repo. Returns null when the module is absent.
   */
  fun canonicalNameForRuleset(rulesetModuleName: String): String? =
    apparentRepoNameToCanonicalName[rulesetModuleName]
      ?: apparentRepoNameToCanonicalName.values.firstOrNull {
        it == "$rulesetModuleName+" || it.startsWith("$rulesetModuleName~")
      }
}

@ApiStatus.Internal
data object RepoMappingDisabled : RepoMapping

@ApiStatus.Internal
data class LocalRepositoryMapping(val localRepositories: Map<String, Path>)

@ApiStatus.Internal
fun RepoMapping.getLocalRepositories() = LocalRepositoryMapping((this as? BzlmodRepoMapping)?.canonicalRepoNameToLocalPath ?: mapOf())
