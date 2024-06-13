@file:Suppress("detekt:all")
package org.jetbrains.bsp.performance.testing

import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.di.di
import com.intellij.tools.ide.util.common.logOutput
import org.kodein.di.direct
import org.kodein.di.instance
import java.net.URI
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader

// Copied from monorepo to fix an authentication issue
fun CIServer.asTeamCity(): TeamCityCIServer = this as TeamCityCIServer

open class TeamCityCIServer(
  private val systemPropertiesFilePath: Path? = try {
    Path(System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE"))
  }
  catch (_: Exception) {
    null
  }
) : CIServer {
  override fun publishArtifact(source: Path, artifactPath: String, artifactName: String) {
    TeamCityClient.publishTeamCityArtifacts(source = source, artifactPath = artifactPath, artifactName = artifactName)
  }

  fun reportTest(testName: String, message: String, details: String, isFailure: Boolean) {
    val flowId = UUID.randomUUID().toString()

    val generifiedTestName = testName.processStringForTC()

    logOutput(String.format("##teamcity[testStarted name='%s' flowId='%s' nodeId='%s' parentNodeId='0']", generifiedTestName, flowId, generifiedTestName))
    if (isFailure) {
      logOutput(String.format(
        "##teamcity[testFailed name='%s' message='%s' details='%s' flowId='%s' nodeId='%s' parentNodeId='0']",
        generifiedTestName, message.processStringForTC(), details.processStringForTC(), flowId, generifiedTestName
      ))
    }
    logOutput(String.format("##teamcity[testFinished name='%s' flowId='%s' nodeId='%s' parentNodeId='0']", generifiedTestName, flowId, generifiedTestName))
  }

  override fun reportTestFailure(testName: String, message: String, details: String) {
    reportTest(testName, message, details, isFailure = true)
  }

  fun reportPassedTest(testName: String, message: String, details: String) {
    reportTest(testName, message, details, isFailure = false)
  }

  override fun ignoreTestFailure(testName: String, message: String, details: String) {
    val flowId = UUID.randomUUID().toString()
    val generifiedTestName = testName.processStringForTC()
    logOutput(String.format(
      "##teamcity[testIgnored name='%s' message='%s' details='%s' flowId='%s']",
      generifiedTestName, message.processStringForTC(), details.processStringForTC(), flowId
    ))
  }

  override fun isTestFailureShouldBeIgnored(message: String): Boolean {
    listOfPatternsWhichShouldBeIgnored.forEach { pattern ->
      if (pattern.containsMatchIn(message)) {
        return true
      }
    }
    return false
  }

  val buildStartTime: String by lazy {
    if (buildId == LOCAL_RUN_ID) {
      ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)
    }
    else {
      val fullUrl = TeamCityClient.restUri.resolve("builds/id:${buildId}?fields=startDate")
      TeamCityClient.get(fullUrl) { it.withAuth() }.fields().asSequence().firstOrNull { it.key == "startDate" }?.value?.asText()?.let {
        runCatching {
          ZonedDateTime.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")).format(DateTimeFormatter.RFC_1123_DATE_TIME)
        }.getOrNull()
      }
        ?: ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)
    }
  }

  private val listOfPatternsWhichShouldBeIgnored by lazy {
    val ignoredPattern = System.getenv("IGNORED_TEST_FAILURE_PATTERN")
    logOutput("DEBUG: ignored patterns form ENV $ignoredPattern")
    val patterns = mutableListOf(
      "No files have been downloaded for .+:.+".toRegex(),
      "Library '.+' resolution failed".toRegex(),
      "Too many IDE internal errors. Monitoring stopped.".toRegex(),
      "Invalid folding descriptor detected".toRegex(),
      "Non-idempotent computation: it returns different results when invoked multiple times".toRegex()
    )
    if (ignoredPattern != null && ignoredPattern.isNotBlank()) {
      val ignoredPatterns = ignoredPattern.split("\n")
      ignoredPatterns.forEach {
        logOutput("Add $it ignored pattern from env")
        patterns.add(it.toRegex())
      }
    }
    patterns
  }

  private fun loadProperties(propertiesPath: Path): Map<String, String> =
    try {
      propertiesPath.bufferedReader().use {
        val map = mutableMapOf<String, String>()
        val ps = Properties()
        ps.load(it)

        ps.forEach { k, v ->
          if (k != null && v != null) {
            map[k.toString()] = v.toString()
          }
        }
        map
      }
    }
    catch (t: Throwable) {
      emptyMap()
    }

  private val systemProperties by lazy {
    val props = mutableMapOf<String, String>()
    systemPropertiesFilePath?.let { props.putAll(loadProperties(it)) }

    props.putAll(System.getProperties().map { it.key.toString() to it.value.toString() })
    props
  }

  /**
   * @return String or Null if parameters isn't found
   */
  private fun getBuildParam(name: String, impreciseNameMatch: Boolean = false): String? {
    val totalParams = systemProperties.plus(buildParams)

    val paramValue = if (impreciseNameMatch) {
      val paramCandidates = totalParams.filter { it.key.contains(name) }
      if (paramCandidates.size > 1) System.err.println("Found many parameters matching $name. Candidates: $paramCandidates")
      paramCandidates[paramCandidates.toSortedMap().firstKey()]
    }
    else totalParams[name]

    return paramValue
  }

  override val isBuildRunningOnCI = System.getenv("TEAMCITY_VERSION") != null
  override val buildNumber by lazy { System.getenv("BUILD_NUMBER") ?: "" }
  override val branchName by lazy { buildParams["teamcity.build.branch"] ?: "" }

  val configurationName by lazy { getBuildParam("teamcity.buildConfName") }
  val buildVcsNumber by lazy { getBuildParam("build.vcs.number") ?: "Unknown" }
  val buildConfigName: String? by lazy { getBuildParam("teamcity.buildConfName") }
  override val buildParams by lazy {
    val configurationPropertiesFile = systemProperties["teamcity.configuration.properties.file"]

    if (configurationPropertiesFile.isNullOrBlank()) return@lazy emptyMap()
    loadProperties(Path(configurationPropertiesFile))
  }

  /** Root URI of the server */
  val serverUri: URI by lazy {
    return@lazy di.direct.instance<URI>(tag = "teamcity.uri")
  }

  val userName: String by lazy { getBuildParam("teamcity.auth.userId")!! }
  val password: String by lazy { getBuildParam("teamcity.auth.password")!! }

  private val isDefaultBranch by lazy {
    //see https://www.jetbrains.com/help/teamcity/predefined-build-parameters.html#PredefinedBuildParameters-Branch-RelatedParameters
    hasBooleanProperty("teamcity.build.branch.is_default", default = false)
  }

  val isPersonalBuild by lazy {
    getBuildParam("build.is.personal").equals("true", ignoreCase = true)
  }

  val buildId: String by lazy {
    getBuildParam("teamcity.build.id") ?: LOCAL_RUN_ID
  }
  val teamcityAgentName by lazy { getBuildParam("teamcity.agent.name") }
  val teamcityCloudProfile by lazy { getBuildParam("system.cloud.profile_id") }

  val buildTypeId: String? by lazy { getBuildParam("teamcity.buildType.id") }

  fun buildUrl(): String = "$serverUri/buildConfiguration/$buildTypeId/$buildId?buildTab=tests"

  val isSpecialBuild: Boolean
    get() {
      if (!isBuildRunningOnCI) {
        logOutput("[Metrics Publishing] Not running build on TeamCity => DISABLED")
        return true
      }

      if (isPersonalBuild) {
        logOutput("[Metrics Publishing] Personal builds are ignored => DISABLED")
        return true
      }

      if (!isDefaultBranch) {
        logOutput("[Metrics Publishing] Non default branches builds are ignored => DISABLED")
        return true
      }

      return false
    }

  private fun hasBooleanProperty(key: String, default: Boolean) = getBuildParam(key)?.equals("true", ignoreCase = true) ?: default

  fun isSafePush(): Boolean {
    val isSafePush = System.getenv("SAFE_PUSH")
    return (isSafePush != null && isSafePush == "true")
  }

  companion object {
    const val LOCAL_RUN_ID = "LOCAL_RUN_SNAPSHOT"

    fun String.processStringForTC(): String {
      //todo replace to intellij.platform.testFramework.util.teamcity.escapeStringForTeamCity when module is published
      return this.substring(0, kotlin.math.min(7000, this.length))
        .replace("\\|", "||")
        .replace("\\[", "|[")
        .replace("]", "|]")
        .replace("\n", "|n")
        .replace("'", "|'")
        .replace("\r", "|r")
    }

    fun setStatusTextPrefix(text: String) {
      logOutput(" ##teamcity[buildStatus text='$text {build.status.text}'] ")
    }

    fun reportTeamCityStatistics(key: String, value: Int) {
      logOutput(" ##teamcity[buildStatisticValue key='${key}' value='${value}']")
    }

    fun reportTeamCityStatistics(key: String, value: Long) {
      logOutput(" ##teamcity[buildStatisticValue key='${key}' value='${value}']")
    }

    fun reportTeamCityMessage(text: String) {
      logOutput(" ##teamcity[message text='$text']")
    }

    fun testSuiteStarted(suiteName: String, flowId: String) {
      println("##teamcity[testSuiteStarted name='${suiteName.processStringForTC()}' flowId='$flowId']")
    }

    fun testSuiteFinished(suiteName: String, flowId: String) {
      println("##teamcity[testSuiteFinished name='${suiteName.processStringForTC()}' flowId='$flowId']")
    }

    fun testStarted(testName: String, flowId: String) {
      println("##teamcity[testStarted name='${testName.processStringForTC()}' flowId='$flowId']")
    }

    fun testFinished(testName: String, flowId: String) {
      println("##teamcity[testFinished name='${testName.processStringForTC()}' flowId='$flowId']")
    }

    fun testIgnored(testName: String, message: String, flowId: String) {
      println("##teamcity[testIgnored name='${testName.processStringForTC()}' message='${message.processStringForTC()}' flowId='$flowId']")
    }

    fun testFailed(testName: String, message: String, flowId: String) {
      println("##teamcity[testFailed name='${testName.processStringForTC()}' message='${message.processStringForTC()}' flowId='$flowId']")
    }

    fun addTextMetadata(testName: String, value: String, flowId: String) {
      addTestMetadata(testName, TeamCityMetadataType.TEXT, flowId, null, value)
    }

    fun addTestMetadata(testName: String, type: TeamCityMetadataType, flowId: String, name: String?, value: String) {
      val nameAttr = if (name != null) {
        "name='${name.processStringForTC()}'"
      }
      else ""
      println("##teamcity[testMetadata testName='${testName.processStringForTC()}' type='${type.name.lowercase()}' ${nameAttr} value='${value.processStringForTC()}' flowId='$flowId']")
    }

    fun progressStart(activityName: String) {
      println("##teamcity[progressStart '${activityName.processStringForTC()}']")
    }

    fun progressFinish(activityName: String) {
      println("##teamcity[progressFinish '${activityName.processStringForTC()}']")
    }
  }

  enum class TeamCityMetadataType {
    NUMBER,
    TEXT,
    LINK,
    ARTIFACT,
    IMAGE
  }
}
