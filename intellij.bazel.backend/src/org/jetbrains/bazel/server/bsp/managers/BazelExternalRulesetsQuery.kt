package org.jetbrains.bazel.server.bsp.managers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.JsonProto.Attribute
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.server.bzlmod.rootRulesToNeededTransitiveRules
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.asLogger

@ApiStatus.Internal
interface BazelExternalRulesetsQuery {
  /**
   * the list of returned ruleset names should be in the apparent form as they will be used in aspect files
   */
  suspend fun fetchExternalRulesetNames(): List<String>
}

internal class BazelExternalRulesetsQueryImpl(
  private val taskId: TaskId,
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val isWorkspaceEnabled: Boolean,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val projectView: ProjectView,
  private val repoMapping: RepoMapping,
) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> =
    BazelBzlModExternalRulesetsQueryImpl(
      taskId,
      bazelRunner,
      isBzlModEnabled,
      taskEventsHandler,
      projectView,
      repoMapping,
    ).fetchExternalRulesetNames() +
    BazelWorkspaceExternalRulesetsQueryImpl(
      taskId,
      bazelRunner,
      isWorkspaceEnabled,
      taskEventsHandler,
      projectView,
    ).fetchExternalRulesetNames()
}

/**
 * Finds the external rulesets that a `WORKSPACE` file declares with `git_repository` or `http_archive`.
 *
 * The query uses `--output=streamed_jsonproto`, so Bazel prints one target per line. The parser reads the lines one by
 * one, and the output size has no limit. See [parseWorkspaceExternalRulesetNames].
 */
@ApiStatus.Internal
class BazelWorkspaceExternalRulesetsQueryImpl(
  private val taskId: TaskId,
  private val bazelRunner: BazelRunner,
  private val isWorkspaceEnabled: Boolean,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val projectView: ProjectView,
) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> =
    if (!isWorkspaceEnabled) {
      emptyList()
    }
    else {
      bazelRunner.run {
        val command =
          buildBazelCommand(projectView) {
            query {
              targets.add(Label.parse("//external:*"))
              options.addAll(QUERY_OPTIONS)
            }
          }

        runBazelCommand(command, taskId, logProcessOutput = false)
          .waitAndGetResult()
          .let { result ->
            if (result.isNotSuccess) {
              val queryFailedMessage = getQueryFailedMessage(result)
              taskEventsHandler.asLogger(taskId).warn(queryFailedMessage)
              log.warn(queryFailedMessage)
              emptyList()
            }
            else {
              parseWorkspaceExternalRulesetNames(result.stdoutLines)
            }
          }
      }
    }

  companion object {
    private val log = logger<BazelWorkspaceExternalRulesetsQueryImpl>()

    /**
     * The query options. Only the `name` and the `generator_function` attributes are needed, so the other attributes
     * are not printed.
     */
    private val QUERY_OPTIONS: List<String> =
      listOf(
        "--output=streamed_jsonproto",
        "--order_output=no",
        "--proto:output_rule_attrs=name,generator_function",
      )

    /**
     * Parses the ndjson output of `bazel query //external:* --output=streamed_jsonproto`.
     *
     * Each line holds one target in the `blaze_query.Target` JSON form. The result holds the `name` attribute of each
     * eligible rule, in the output order. A rule is eligible when its class contains `git_repository`, or when its
     * class contains `http_archive` and the rule is not an output of a macro other than an `http_archive` wrapper.
     * A blank line is skipped. A line that does not parse is logged and skipped, so it does not drop the other rules.
     */
    fun parseWorkspaceExternalRulesetNames(stdoutLines: List<String>): List<String> =
      stdoutLines.mapNotNull { line ->
        parseTarget(line)
          ?.rule
          ?.takeIf { it.isEligible() }
          ?.attributeValue("name")
      }

    private fun parseTarget(line: String): JsonProto.Target? {
      if (line.isBlank()) return null
      return try {
        bazelGson.fromJson(line, JsonProto.Target::class.java)?.takeIf { it.type == "RULE" }
      }
      catch (e: Exception) {
        rethrowControlFlowException(e)
        log.warn("Failed to parse a query output line as json: $line", e)
        null
      }
    }

    private fun JsonProto.Rule.isEligible(): Boolean {
      val ruleClass = ruleClass ?: return false
      if (ruleClass.contains("git_repository")) return true
      if (!ruleClass.contains("http_archive")) return false
      // Bazel prints the default value of `generator_function`, an empty string, for a rule that no macro created.
      val generatorFunction = attributeValue("generator_function")
      return generatorFunction.isNullOrEmpty() || generatorFunction.contains("http_archive")
    }

    private fun JsonProto.Rule.attributeValue(attributeName: String): String? =
      attribute?.firstOrNull { it.name == attributeName }?.stringValue
  }
}

internal class BazelBzlModExternalRulesetsQueryImpl(
  private val taskId: TaskId,
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val taskEventsHandler: BazelTaskEventsHandler,
  private val projectView: ProjectView,
  private val repoMapping: RepoMapping,
) : BazelExternalRulesetsQuery {
  private val gson = bazelGson

  override suspend fun fetchExternalRulesetNames(): List<String> {
    if (!isBzlModEnabled) return emptyList()
    val command =
      bazelRunner.buildBazelCommand(projectView) {
        graph { options.add("--output=json") }
      }
    val bzlmodGraphJson =
      bazelRunner
        .runBazelCommand(
          command,
          taskId = taskId,
          logProcessOutput = false
        ).waitAndGetResult()
        .let { result ->
          if (result.isNotSuccess) {
            val queryFailedMessage = getQueryFailedMessage(result)
            taskEventsHandler.asLogger(taskId).warn(queryFailedMessage)
            log.warn(queryFailedMessage)

            val target = SyntheticLabel(AllRuleTargets)
            val diagnostics =
              DiagnosticsService(bazelRunner.workspaceRoot)
                .extractDiagnostics(result.stderrLines, target, taskId, isCommandLineFormattedOutput = true)
            diagnostics.forEach { taskEventsHandler.onBuildPublishDiagnostics(it) }
          }
          // best effort to parse the output even when there are errors
          try {
            JsonParser.parseReader(result.stdout.inputStream().reader())
          } catch (e: Exception) {
            log.error("Failed to parse string to json", e)
            null
          }
        } as? JsonObject

    return try {
      val graph = gson.fromJson(bzlmodGraphJson, BzlmodGraph::class.java)
      val directDependencyNames = graph.getAllDirectRulesetDependencyNames()
      val indirectDeps =
        rootRulesToNeededTransitiveRules
          .filterKeys { it in directDependencyNames }
          .flatMap { it.value.flatMap { transitiveDep -> graph.includedByDirectDeps(it.key, transitiveDep) } }
      val directDepsApparentNames = graph.dependencies.mapNotNull { it.toApparentName(repoMapping) }
      val indirectDepsApparentNames = indirectDeps.mapNotNull { it.toApparentName(repoMapping) }
      val moduleName = graph.name?.ifEmpty { null }
      // We include the current module name to handle the edge case for the rules_kotlin repository.
      // In this scenario, the Bazel module does not declare a dependency on rules_kotlin because the module itself *is* rules_kotlin.
      // Without this, Kotlin support would not be enabled in this case, causing redcodes in the IDE.
      return (directDepsApparentNames + indirectDepsApparentNames + listOfNotNull(moduleName)).distinct()
    } catch (e: Throwable) {
      log.warn("The returned bzlmod json is not parsable:\n$bzlmodGraphJson", e)
      emptyList()
    }
  }

  companion object {
    private val log = logger<BazelBzlModExternalRulesetsQueryImpl>()
  }
}

private fun getQueryFailedMessage(result: BazelProcessResult): String = "Bazel query failed with output:\n${result.stderrLines.joinToString("\n")}"

@ApiStatus.Internal
data class BzlmodDependency(val key: String, val name: String?, val apparentName: String?, val dependencies: List<BzlmodDependency>) {
  fun toApparentName(repoMapping: RepoMapping): String? {
    if (apparentName != null) return apparentName
    // Bazel 7.4.1 and lower don't provide apparentName in the graph JSON output
    val repoMapping = (repoMapping as? BzlmodRepoMapping) ?: return null
    return repoMapping.canonicalRepoNameToApparentName[key.substringBefore("@") + "~"]
           ?: repoMapping.canonicalRepoNameToApparentName[key.substringBefore("@") + "+"]
  }
}

@ApiStatus.Internal
data class BzlmodGraph(
  val name: String?,
  val dependencies: List<BzlmodDependency>,
) {
  fun getAllDirectRulesetDependencyNames(): List<String> = dependencies.mapNotNull { it.name }

  fun includedByDirectDeps(rootRulesetName: String, transitiveRulesetName: String): List<BzlmodDependency> =
    dependencies.find { it.name == rootRulesetName }?.dependencies?.filter { it.name == transitiveRulesetName } ?: emptyList()
}

/**
 * Representations of the messages from
 * https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/build.proto
 * in the JSON form that `--output=streamed_jsonproto` prints.
 */
private class JsonProto {
  data class Target(
    val type: String?,
    val rule: Rule?,
  )

  data class Rule(
    val name: String?,
    val ruleClass: String?,
    val attribute: List<Attribute>?,
  )
}
