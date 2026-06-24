package org.jetbrains.bazel.languages.starlark.bazel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import com.intellij.util.text.SemVer
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.orFromBazelVersionFile
import org.jetbrains.bazel.commons.toSemVer
import org.jetbrains.bazel.sync.environment.projectCtx
import kotlin.io.path.Path

/**
 * Provides Bazel builtin function signatures for Starlark code completion and documentation.
 *
 * Reads `builtins@{version}.json` (generated offline by
 * `//plugins/bazel/tools/bazel_signatures/builtins:generator`) and
 * deserializes the JSON into [BazelGlobalFunction] objects via Gson.
 *
 * Multiple versions are shipped. The provider selects the closest version that does not
 * exceed the project's Bazel version, obtained from [org.jetbrains.bazel.sync.environment.BazelProjectContextService].
 * Before the first sync, falls back to `.bazelversion`; if absent, uses the latest available signatures.
 *
 * See the KDoc on the generator's `main` function for the full generation pipeline and
 * usage instructions.
 *
 * ### Updating
 * Run given command with wanted version
 * ```bash
 * # Last generated version: 9.1.1
 * bazel run //plugins/bazel/tools/bazel_signatures/builtins:generator -- --version $version
 * ```
 * If the version introduces any changes in the doc, comparing with the last generated version, the new JSON file will be generated.
 * Update [BUILTIN_AVAILABLE_VERSIONS] list, when new JSON is added.
 */
internal class BazelBuiltinFunctionProvider : StarlarkGlobalFunctionProvider {

  @Volatile
  private var cached: Pair<SemVer, List<BazelGlobalFunction>>? = null

  override fun functions(project: Project): List<BazelGlobalFunction> {
    // fallbacks here are only needed before sync
    val release = project.projectCtx.bazelRelease.orFromBazelVersionFileIn(project)
    val currentVersion = release?.toSemVer()
    val selectedVersion = when {
      currentVersion == null -> BUILTIN_AVAILABLE_VERSIONS.lastOrNull() // version not detected, use latest
      else -> BUILTIN_AVAILABLE_VERSIONS
        .lastOrNull { it <= currentVersion }
        ?: BUILTIN_AVAILABLE_VERSIONS.firstOrNull() // version too old, use earliest
    }
    return selectedVersion?.let { loadBuiltinFunctions(it) } ?: emptyList()
  }

  private fun loadBuiltinFunctions(version: SemVer): List<BazelGlobalFunction> = synchronized(this) {
    cached?.let { (v, fns) -> if (v == version) return@synchronized fns }
    val path = "/bazelSignatures/builtins/builtins@$version.json"
    val stream = javaClass.getResourceAsStream(path) ?: return@synchronized emptyList()
    val functions = stream.use { Gson().fromJson(it.bufferedReader(), bazelGlobalFunctionListTypeToken) }
    cached = version to functions
    return@synchronized functions
  }

  private val bazelGlobalFunctionListTypeToken = object : TypeToken<List<BazelGlobalFunction>>() {}
}

private fun BazelRelease?.orFromBazelVersionFileIn(
  project: Project,
): BazelRelease? {
  val basePath = project.basePath ?: return null
  val workspacePath = Path(basePath)
  return this.orFromBazelVersionFile(workspacePath)
}

private val BUILTIN_AVAILABLE_VERSIONS: List<SemVer> = listOf(
  semVer(7, 5),
  semVer(7, 6),
  semVer(7, 7),
  semVer(8),
  semVer(8, 1),
  semVer(8, 2),
  semVer(8, 3),
  semVer(8, 4),
  semVer(8, 4, 1),
  semVer(8, 5),
  semVer(8, 6),
  semVer(9),
  semVer(9, 0, 1),
  semVer(9, 1),
)

private fun semVer(
  major: Int,
  minor: Int = 0,
  patch: Int = 0,
) = SemVer("$major.$minor.$patch", major, minor, patch)
