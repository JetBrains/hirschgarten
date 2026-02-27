package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.TaskId

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

  data class HttpArchiveRepository(override val name: String, val urls: List<String>) : ShowRepoResult

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

  private fun extractAttributeList(lines: List<String>, attributeName: String): List<String> =
    lines.first { it.contains("$attributeName = ") }
      .substringAfter("$attributeName = [")
      .substringBefore("]")
      .split(",")
      .map { it.trim().removePrefix("\"").removeSuffix("\"") }

  private fun splitInfoGroups(lines: List<String>): Map<String, List<String>> {
    var groups = mutableMapOf<String, List<String>>()
    var currentGroup = mutableListOf<String>()
    var currentGroupName: String? = null
    for (line in lines) {
      if (line.startsWith("## ")) {
        if (currentGroupName != null) {
          groups[currentGroupName] = currentGroup
        }
        currentGroup = mutableListOf<String>()
        currentGroupName = line.substringAfter("## ").trim()
        if (currentGroupName.endsWith(":")) {
          currentGroupName = currentGroupName.substring(0, currentGroupName.length - 1)
        }
      }
      currentGroup.add(line)
    }
    if (currentGroupName != null) {
      groups[currentGroupName] = currentGroup
    }
    return groups
  }

  fun parseShowRepoStanza(stanza: List<String>): ShowRepoResult? {
    try {
      // No matter how the repository is defined, it will always have a name
      val name = extractAttribute(stanza, "name")

      val isLocalRepository = stanza.any { it.contains("local_repository(") }
      if (isLocalRepository) {
        val path = extractAttribute(stanza, "path")
        return ShowRepoResult.LocalRepository(name, path)
      }
      else if (stanza.any { it.contains("http_archive") }) {
        val urls = extractAttributeList(stanza, "urls")
        return ShowRepoResult.HttpArchiveRepository(name, urls)
      }
      else {
        return ShowRepoResult.Unknown(name, stanza.joinToString("\n") + "\n")
      }
    }
    catch (e: Exception) {
      return null
    }
  }

  fun parseJsonRepoDescription(jsonText: String): Map<String, ShowRepoResult?> {
    if (jsonText.isEmpty()) return emptyMap() // ignore empty lines that might be included in the output

    try {
      val description = bazelGson.fromJson<JsonProto.Repository>(jsonText, JsonProto.Repository::class.java)
      if (description.canonicalName == null) return emptyMap() // Nothing we can do with that repository
      val key = description.moduleKey ?: description.canonicalName

      if (description.repoRuleName == "local_repository") {
        val pathValues = description.attribute.filter { it.name == "path" }.map { it.stringValue }.filterNotNull()
        if (pathValues.isEmpty()) return mapOf(key to ShowRepoResult.Unknown(description.canonicalName, jsonText))
        return mapOf(key to ShowRepoResult.LocalRepository(description.canonicalName, pathValues.last()))
      }
      else if (description.repoRuleName == "http_archive") {
        return mapOf(
          key to ShowRepoResult.HttpArchiveRepository(
            description.canonicalName,
            description.attribute.filter { it.name == "urls" }.flatMap { it.stringListValue ?: emptyList() },
          ),
        )
      }
      return mapOf(key to ShowRepoResult.Unknown(description.canonicalName, jsonText))
    }
    catch (ex: Throwable) {
      throw Error("Failed to parse repository json description: $jsonText", ex)
    }
  }

  fun parseShowRepoResults(bazelProcessResult: BazelProcessResult, isJson: Boolean): Map<String, ShowRepoResult?> {
    if (bazelProcessResult.isNotSuccess) {
      // If the exit code is not 0, bazel prints the error message to stderr
      error("Failed to resolve module from bazel info. Bazel Info output:\n'${bazelProcessResult.stderrLines.joinToString("\n")}'")
    }
    if (!isJson) {
      return splitInfoGroups(bazelProcessResult.stdoutLines).mapValues { (_, stanza) -> parseShowRepoStanza(stanza) }
    }
    // The output is new-line-delimited JSON, i.e., each line is a JSON description of one repository.
    return bazelProcessResult.stdoutLines.map { parseJsonRepoDescription(it) }.reduce { acc, result -> acc + result }
  }
}

class ModuleResolver(
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
  private val taskId: TaskId,
) {
  private val moduleOutputParser = ModuleOutputParser()

  /**
   * The name can be @@repo, @repo or repo. It will be resolved in the context of the main workspace.
   */
  suspend fun resolveModules(unsortedModuleNames: List<String>, bazelInfo: BazelInfo): Map<String, ShowRepoResult?> {
    if (unsortedModuleNames.isEmpty()) return emptyMap() // avoid bazel call if no information is needed
    val moduleNames = unsortedModuleNames.sorted()
    val json_output = bazelInfo.release.major >= 9
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        showRepo {
          if (json_output) {
            options.add("--output=streamed_jsonproto")
          }
          options.addAll(moduleNames)
        }
      }
    val processResult =
      bazelRunner
        .runBazelCommand(command, taskId, logOnlyErrors = true)
        .waitAndGetResult()
    if (bazelInfo.isWorkspaceEnabled && processResult.isNotSuccess) {
      // work around https://github.com/bazelbuild/bazel/issues/28601
      // As we cannot reliably determine which repositories `bazel mod show_repo` is willing to resolve,
      // we have to accept failure. However, to get the maximal amount of information possible, we should
      // ask for each repository individually.
      if (moduleNames.size == 1) {
        // Failure of a request asking for a single repository, so we just answer that we don't know.
        return mapOf(moduleNames[0] to null)
      }
      return moduleNames.map { resolveModules(listOf(it), bazelInfo) }.reduceOrNull { acc, result -> acc + result } ?: emptyMap()
    }

    return moduleOutputParser.parseShowRepoResults(processResult, json_output)
  }

  val gson = bazelGson

  /**
   * Obtains the mappings from apparent repo names to canonical repo names in the context of `canonicalRepoNames`.
   *
   * <canonicalRepoNames> is a list of canonical repo names without any leading @ characters.
   * The canonical repo name of the root module repository is the empty string.
   */
  suspend fun getRepoMappings(unsortedCanonicalRepoNames: List<String>): Map<String, Map<String, String>> {
    if (unsortedCanonicalRepoNames.isEmpty()) return emptyMap()
    val canonicalRepoNames = unsortedCanonicalRepoNames.sorted()
    canonicalRepoNames.forEach {
      if (it.startsWith('@')) error("Canonical repo name cannot contain '@' characters: $it")
    }

    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        dumpRepoMapping {
          options.addAll(canonicalRepoNames)
        }
      }
    val processResult =
      bazelRunner
        .runBazelCommand(command, taskId, logOnlyErrors = true)
        .waitAndGetResult()

    if (processResult.isNotSuccess) {
      // dumpRepoMapping was added in Bazel 7.1.0, so it's going to fail with 7.0.0: https://github.com/bazelbuild/bazel/issues/20972
      error("dumpRepoMapping failed with output: ${processResult.stdoutLines.joinToString("\n")}")
    }
    try {
      return processResult.stdoutLines.map { line ->
        gson.fromJson<Map<String, String>>(line, Map::class.java)
      }.zip(canonicalRepoNames).associate { (map, canonicalRepoName) -> canonicalRepoName to map }
    }
    catch (ex: Throwable) {
      throw Error("Failed to parse repo mapping from bazel. Bazel output:\n${processResult.stdoutLines.joinToString("\n")}", ex)
    }
  }
}

class JsonProto {
  // Class representations of the relevant parts of some messages from https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/build.proto
  data class Attribute(val name: String, val stringValue: String?, val stringListValue: List<String>?)
  data class Repository(val moduleKey: String?, val canonicalName: String?, val repoRuleName: String?, val attribute: List<Attribute>)
}
