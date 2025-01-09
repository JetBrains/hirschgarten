package org.jetbrains.bsp.bazel.server.bsp.managers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.logging.log4j.LogManager
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.workspacecontext.EnabledRulesSpec
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

interface BazelExternalRulesQuery {
  fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String>
}

class BazelEnabledRulesQueryImpl(private val enabledRulesSpec: EnabledRulesSpec) : BazelExternalRulesQuery {
  override fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String> = enabledRulesSpec.values
}

class BazelExternalRulesQueryImpl(
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val isWorkspaceEnabled: Boolean,
  private val enabledRules: EnabledRulesSpec,
  private val bspClientLogger: BspClientLogger,
) : BazelExternalRulesQuery {
  override fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String> =
    when {
      enabledRules.isNotEmpty() -> BazelEnabledRulesQueryImpl(enabledRules).fetchExternalRuleNames(cancelChecker)
      else ->
        BazelBzlModExternalRulesQueryImpl(bazelRunner, isBzlModEnabled, bspClientLogger).fetchExternalRuleNames(cancelChecker) +
          BazelWorkspaceExternalRulesQueryImpl(
            bazelRunner,
            isWorkspaceEnabled,
            bspClientLogger,
          ).fetchExternalRuleNames(cancelChecker)
    }
}

class BazelWorkspaceExternalRulesQueryImpl(
  private val bazelRunner: BazelRunner,
  private val isWorkspaceEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
) : BazelExternalRulesQuery {
  override fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String> =
    if (!isWorkspaceEnabled) {
      emptyList()
    } else {
      bazelRunner.run {
        val command =
          buildBazelCommand {
            query {
              targets.add(Label.parse("//external:*"))
              options.addAll(listOf("--output=xml", "--order_output=no"))
            }
          }

        runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
          .waitAndGetResult(cancelChecker, ensureAllOutputRead = true)
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
    private val log = LogManager.getLogger(BazelExternalRulesQueryImpl::class.java)
  }
}

class BazelBzlModExternalRulesQueryImpl(
  private val bazelRunner: BazelRunner,
  private val isBzlModEnabled: Boolean,
  private val bspClientLogger: BspClientLogger,
) : BazelExternalRulesQuery {
  private val gson = Gson()

  override fun fetchExternalRuleNames(cancelChecker: CancelChecker): List<String> {
    if (!isBzlModEnabled) return emptyList()
    val command =
      bazelRunner.buildBazelCommand {
        graph { options.add("--output=json") }
      }
    val bzlmodGraphJson =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult(cancelChecker, ensureAllOutputRead = true)
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
      gson.fromJson(bzlmodGraphJson, BzlmodGraph::class.java).getAllDirectRuleDependencies()
    } catch (e: Throwable) {
      log.warn("The returned bzlmod json is not parsable:\n$bzlmodGraphJson", e)
      emptyList()
    }
  }

  companion object {
    private val log = LogManager.getLogger(BazelBzlModExternalRulesQueryImpl::class.java)
  }
}

private fun getQueryFailedMessage(result: BazelProcessResult): String = "Bazel query failed with output:\n${result.stderr}"

data class BzlmodDependency(val key: String) {
  /**
   * Extract dependency name from bzlmod dependency, where the raw format is `<DEP_NAME>@<DEP_VERSION>`.
   *
   * There were some issues with (empty) bzlmod projects and android, so the automatic mechanism ignores it.
   * Use `enabled_rules` to enable `rules_android` instead.
   */
  fun toDependencyName(): String = key.substringBefore('@')
}

data class BzlmodGraph(val dependencies: List<BzlmodDependency>) {
  fun getAllDirectRuleDependencies() = dependencies.map { it.toDependencyName() }
}
