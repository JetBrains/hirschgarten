package org.jetbrains.bazel.bazelrunner

import com.google.gson.Gson
import org.eclipse.lsp4j.jsonrpc.CancelChecker

sealed interface ShowRepoResult {
  val name: String

  /**
   * ## @community:
   * # <builtin>
   * local_repository(
   *   name = "community~",
   *   path = "community",
   * )
   * # Rule community~ instantiated at (most recent call last):
   * #   <builtin> in <toplevel>
   */
  data class LocalRepository(override val name: String, val path: String) : ShowRepoResult

  /**
   * Any other output that doesn't match the expected format but contains the name of the module.
   */
  data class Unknown(override val name: String, val output: String) : ShowRepoResult
}

class ModuleOutputParser {
  private fun extractAttribute(lines: List<String>, attributeName: String): String =
    lines
      .first { it.contains("$attributeName = ") }
      .substringAfter("$attributeName = \"")
      .substringBefore("\",")
      .trim()

  // TODO: keep track of https://github.com/bazelbuild/bazel/issues/21617
  fun parseShowRepoResult(bazelProcessResult: BazelProcessResult): ShowRepoResult {
    if (bazelProcessResult.isNotSuccess) {
      // If the exit code is not 0, bazel prints the error message to stderr
      error("Failed to resolve module from bazel info. Bazel Info output:\n'${bazelProcessResult.stderr}'")
    }

    try {
      // No matter how the repository is defined, it will always have a name
      val name = extractAttribute(bazelProcessResult.stdoutLines, "name")

      val isLocalRepository = bazelProcessResult.stdoutLines.any { it.contains("local_repository(") }
      if (isLocalRepository) {
        val path = extractAttribute(bazelProcessResult.stdoutLines, "path")
        return ShowRepoResult.LocalRepository(name, path)
      } else {
        return ShowRepoResult.Unknown(name, bazelProcessResult.stdout)
      }
    } catch (e: Exception) {
      throw IllegalStateException("Failed to parse module from bazel info. Bazel Info output:\n'${bazelProcessResult.stdout}'", e)
    }
  }
}

class ModuleResolver(private val bazelRunner: BazelRunner, private val moduleOutputParser: ModuleOutputParser) {
  /**
   * The name can be @@repo, @repo or repo. It will be resolved in the context of the main workspace.
   */
  fun resolveModule(moduleName: String, cancelChecker: CancelChecker): ShowRepoResult {
    val command =
      bazelRunner.buildBazelCommand {
        showRepo {
          options.add(moduleName)
        }
      }
    val processResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult(cancelChecker, true)
    return moduleOutputParser.parseShowRepoResult(processResult)
  }

  val gson = Gson()

  /**
   * Obtains the mappings from apparent repo names to canonical repo names in the context of `canonicalRepoName`.
   *
   * <canonicalRepoName> is a canonical repo name without any leading @ characters.
   * The canonical repo name of the root module repository is the empty string.
   */
  fun getRepoMapping(canonicalRepoName: String, cancelChecker: CancelChecker): Map<String, String> {
    if (canonicalRepoName.startsWith('@')) {
      error("Canonical repo name cannot contain '@' characters: $canonicalRepoName")
    }

    val command =
      bazelRunner.buildBazelCommand {
        dumpRepoMapping {
          options.add(canonicalRepoName)
        }
      }
    val processResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult(cancelChecker, true)

    if (processResult.isNotSuccess) {
      // dumpRepoMapping was added in Bazel 7.1.0, so it's going to fail with 7.0.0: https://github.com/bazelbuild/bazel/issues/20972
      error("dumpRepoMapping failed with output: ${processResult.stdout}")
    }

    // Output is json, we need to parse it
    val output = processResult.stdout
    @Suppress("UNCHECKED_CAST")
    return gson.fromJson(output, Map::class.java) as? Map<String, String>
      ?: error("Failed to parse repo mapping from bazel. Bazel output:\n$output")
  }
}
