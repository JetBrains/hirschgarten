package org.jetbrains.bazel.server.bsp.managers

import com.google.gson.JsonObject
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bsp.utils.readXML
import org.jetbrains.bazel.server.bsp.utils.toJson
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bazel.server.bzlmod.rootRulesToNeededTransitiveRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
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

class BazelEnabledRulesetsQueryImpl(private val enabledRulesSpec: EnabledRulesSpec) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> {
    val specifiedRules = enabledRulesSpec.values
    val neededTransitiveRules = specifiedRules.mapNotNull { rootRulesToNeededTransitiveRules[it] }.flatten()

    return (specifiedRules + neededTransitiveRules).distinct()
  }
}

class BazelExternalRulesetsQueryImpl(
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val isWorkspaceEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
  private val workspaceContext: WorkspaceContext,
  private val repoMapping: RepoMapping,
  private val bidirectionalMapFactory: () -> BidirectionalMap<String, String>,
) : BazelExternalRulesetsQuery {
  override suspend fun fetchExternalRulesetNames(): List<String> =
    when {
      workspaceContext.enabledRules.isNotEmpty() -> BazelEnabledRulesetsQueryImpl(workspaceContext.enabledRules).fetchExternalRulesetNames()
      else ->
        BazelBzlModExternalRulesetsQueryImpl(
          bazelRunner,
          isBzlModEnabled,
          bspClientLogger,
          workspaceContext,
          repoMapping,
          bidirectionalMapFactory,
        ).fetchExternalRulesetNames() +
          BazelWorkspaceExternalRulesetsQueryImpl(
            bazelRunner,
            isWorkspaceEnabled,
            bspClientLogger,
            workspaceContext,
          ).fetchExternalRulesetNames()
    }
}

class BazelWorkspaceExternalRulesetsQueryImpl(
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
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
  private val workspaceContext: WorkspaceContext,
  private val repoMapping: RepoMapping,
  private val bidirectionalMapFactory: () -> BidirectionalMap<String, String>,
) : BazelExternalRulesetsQuery {
  private val gson = bazelGson

  override suspend fun fetchExternalRulesetNames(): List<String> {
    if (!isBzlModEnabled) return emptyList()
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        graph { options.add("--output=json") }
      }
    val apparelRepoNameToCanonicalName = (repoMapping as? BzlmodRepoMapping)?.apparentRepoNameToCanonicalName ?: bidirectionalMapFactory()
    val bzlmodGraphJson =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult(ensureAllOutputRead = true)
        .let { result ->
          if (result.isNotSuccess) {
            val queryFailedMessage = getQueryFailedMessage(result)
            bspClientLogger.warn(queryFailedMessage)
            log.warn(queryFailedMessage)
            null
          } else {
            result.stdout.toJson(log)
          }
        } as? JsonObject
    return try {
      val graph = gson.fromJson(bzlmodGraphJson, BzlmodGraph::class.java)
      val directDeps = graph.getAllDirectRulesetDependencies()
      val indirectDeps =
        rootRulesToNeededTransitiveRules
          .filterKeys { it in directDeps }
          .filter { it.value.any { transitiveDep -> graph.isIncludedTransitively(it.key, transitiveDep) } }
          .values
          .flatten()

      fun toApparentName(canonicalRepoName: String) = apparelRepoNameToCanonicalName.getKeysByValue(canonicalRepoName)?.firstOrNull()
      // this is important as bzlmod graph outputs canonical names, without the "+" or "~" to the end.
      // i.e., @@rules_scala+ will be represented as rules_scala
      // [BAZEL-2109](https://youtrack.jetbrains.com/issue/BAZEL-2109)
      return (directDeps + indirectDeps).map { toApparentName("$it~") ?: toApparentName("$it+") ?: it }.distinct()
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

data class BzlmodDependency(val key: String, val dependencies: List<BzlmodDependency>) {
  /**
   * Extract dependency name from bzlmod dependency, where the raw format is `<DEP_NAME>@<DEP_VERSION>`.
   *
   * There were some issues with (empty) bzlmod projects and android, so the automatic mechanism ignores it.
   * Use `enabled_rules` to enable `rules_android` instead.
   */
  fun toDependencyName(): String = key.substringBefore('@')
}

data class BzlmodGraph(val dependencies: List<BzlmodDependency>) {
  fun getAllDirectRulesetDependencies() = dependencies.map { it.toDependencyName() }

  fun isIncludedTransitively(rootRulesetName: String, transitiveRulesetName: String): Boolean =
    dependencies.find { it.toDependencyName() == rootRulesetName }?.dependencies?.any { it.toDependencyName() == transitiveRulesetName } ==
      true
}
