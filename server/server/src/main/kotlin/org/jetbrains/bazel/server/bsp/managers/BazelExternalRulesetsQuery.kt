package org.jetbrains.bazel.server.bsp.managers

import com.google.gson.JsonObject
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bsp.utils.readXML
import org.jetbrains.bazel.server.bsp.utils.toJson
import org.jetbrains.bazel.server.bzlmod.rootRulesToNeededTransitiveRules
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

interface BazelExternalRulesetsQuery {
  /**
   * the list of returned ruleset names should be in the apparent form as they will be used in aspect files
   */
  suspend fun fetchExternalRulesetNames(): List<String>
}

class BazelEnabledRulesetsQueryImpl(private val enabledRules: List<String>) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> {
    val specifiedRules = enabledRules
    val neededTransitiveRules = specifiedRules.mapNotNull { rootRulesToNeededTransitiveRules[it] }.flatten()

    return (specifiedRules + neededTransitiveRules).distinct()
  }
}

class BazelExternalRulesetsQueryImpl(
  private val originId: String?,
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val isWorkspaceEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
  private val bazelPathsResolver: BazelPathsResolver,
  private val workspaceContext: WorkspaceContext,
  private val repoMapping: RepoMapping,
) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> =
    when {
      workspaceContext.enabledRules.isNotEmpty() -> BazelEnabledRulesetsQueryImpl(workspaceContext.enabledRules).fetchExternalRulesetNames()
      else ->
        BazelBzlModExternalRulesetsQueryImpl(
          originId,
          bazelRunner,
          isBzlModEnabled,
          bspClientLogger,
          workspaceContext,
        ).fetchExternalRulesetNames() +
          BazelWorkspaceExternalRulesetsQueryImpl(
            originId,
            bazelRunner,
            isWorkspaceEnabled,
            bspClientLogger,
            workspaceContext,
          ).fetchExternalRulesetNames()
    }
}

class BazelWorkspaceExternalRulesetsQueryImpl(
  private val originId: String?,
  private val bazelRunner: BazelRunner,
  private val isWorkspaceEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
  private val workspaceContext: WorkspaceContext,
) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> =
    if (!isWorkspaceEnabled) {
      emptyList()
    } else {
      bazelRunner.run {
        val command =
          buildBazelCommand(workspaceContext) {
            query {
              targets.add(Label.parse("//external:*"))
              options.addAll(listOf("--output=xml", "--order_output=no"))
            }
          }

        runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
          .waitAndGetResult(ensureAllOutputRead = true)
          .let { result ->
            if (result.isNotSuccess) {
              val queryFailedMessage = getQueryFailedMessage(result)
              bspClientLogger.warn(queryFailedMessage)
              log.warn(queryFailedMessage)
              null
            } else {
              result.stdout.readXML(log)?.calculateEligibleRules()
            }
          }.orEmpty()
      }
    }

  private fun Document.calculateEligibleRules(): List<String> {
    val xPath = XPathFactory.newInstance().newXPath()
    val expression =
      "/query/rule[contains(@class, 'http_archive') and " +
        "(not(string[@name='generator_function']) or string[@name='generator_function' and contains(@value, 'http_archive')])" +
        "]//string[@name='name']"
    val eligibleItems = xPath.evaluate(expression, this, XPathConstants.NODESET) as NodeList
    val returnList = mutableListOf<String>()
    for (i in 0 until eligibleItems.length) {
      eligibleItems
        .item(i)
        .attributes
        .getNamedItem("value")
        ?.nodeValue
        ?.let { returnList.add(it) }
    }
    return returnList.toList()
  }

  companion object {
    private val log = LoggerFactory.getLogger(BazelExternalRulesetsQueryImpl::class.java)
  }
}

class BazelBzlModExternalRulesetsQueryImpl(
  private val originId: String?,
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
  private val workspaceContext: WorkspaceContext,
) : BazelExternalRulesetsQuery {
  private val gson = bazelGson

  override suspend fun fetchExternalRulesetNames(): List<String> {
    if (!isBzlModEnabled) return emptyList()
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        graph { options.add("--output=json") }
      }
    val bzlmodGraphJson =
      bazelRunner
        .runBazelCommand(
          command,
          originId = originId,
          logProcessOutput = false,
          serverPidFuture = null,
        ).waitAndGetResult(ensureAllOutputRead = true)
        .let { result ->
          if (result.isNotSuccess) {
            val queryFailedMessage = getQueryFailedMessage(result)
            bspClientLogger.warn(queryFailedMessage)
            bazelRunner.workspaceRoot
              ?.takeIf { originId != null }
              ?.let { workspaceRoot ->
                val target = SyntheticLabel(AllRuleTargets)
                val diagnostics =
                  DiagnosticsService(workspaceRoot)
                    .extractDiagnostics(result.stderr, target, originId!!, isCommandLineFormattedOutput = true)
                diagnostics.forEach { bspClientLogger.publishDiagnostics(it) }
              }
            log.warn(queryFailedMessage)
          }
          // best effort to parse the output even when there are errors
          result.stdout.toJson(log)
        } as? JsonObject

    return try {
      val graph = gson.fromJson(bzlmodGraphJson, BzlmodGraph::class.java)
      val directDependencyNames = graph.getAllDirectRulesetDependencyNames()
      val indirectDeps =
        rootRulesToNeededTransitiveRules
          .filterKeys { it in directDependencyNames }
          .flatMap { it.value.flatMap { transitiveDep -> graph.includedByDirectDeps(it.key, transitiveDep) } }

      val directDepsApparentNames = graph.dependencies.map { it.apparentName }
      val indirectDepsApparentNames = indirectDeps.map { it.apparentName }

      return (directDepsApparentNames + indirectDepsApparentNames).distinct()
    } catch (e: Throwable) {
      log.warn("The returned bzlmod json is not parsable:\n$bzlmodGraphJson", e)
      emptyList()
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(BazelBzlModExternalRulesetsQueryImpl::class.java)
  }
}

private fun getQueryFailedMessage(result: BazelProcessResult): String = "Bazel query failed with output:\n${result.stderr}"

data class BzlmodDependency(val key: String, val name: String, val apparentName: String, val dependencies: List<BzlmodDependency>)

data class BzlmodGraph(val dependencies: List<BzlmodDependency>) {
  fun getAllDirectRulesetDependencyNames() = dependencies.map { it.name }

  fun includedByDirectDeps(rootRulesetName: String, transitiveRulesetName: String): List<BzlmodDependency> =
    dependencies.find { it.name == rootRulesetName }?.dependencies?.filter { it.name == transitiveRulesetName } ?: emptyList()
}
