package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.python
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.io.File

/**
 * Data class for static analysis test definitions
 */
data class AnalysisDef(
  val name: String,
  val vcsRoot: GitVcsRoot,
  val cloudTokenKey: String,
  val cloudTokenCredentials: String,
  val linterImage: String = CommonParams.DockerQodanaImage,
  val unchanged: String? = null,
  val diff: String = "0",
  val allowFailure: Boolean = false,
  val enabled: Boolean = true,  // Controls whether this analysis is included in the pipeline
  val qodanaConfig: String = "qodana.yaml",
  val qodanaBaseline: String = "qodana.sarif.json"
)

/**
 * Static analysis configuration using Qodana.
 * 
 * @param analysisDef The analysis definition containing test parameters
 */
class StaticAnalysisTest(
  private val analysisDef: AnalysisDef,
  idNamespace: String? = null,
) : BaseBuildType(
  name = "[analysis] Qodana ${analysisDef.name}",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
    equals("container.engine.osType", "linux")
  },
  customVcsRoot = analysisDef.vcsRoot,
  idNamespace = idNamespace,
  artifactRules = "+:.teamcity/qodana/*.zip",
  dependencies = {
    // Depend on the latest platform build (last in the list), matching the subproject namespace
    val latestPlatformBuild = if (idNamespace == "Space") {
      PluginBuildFactory.ForAllPlatformsSpace.last()
    } else {
      PluginBuildFactory.ForAllPlatforms.last()
    }
    snapshot(latestPlatformBuild) {}
    artifacts(latestPlatformBuild) {
      artifactRules = CommonParams.QodanaArtifactRules
      buildRule = sameChainOrLastFinished()
    }
  },
  steps = {
    // Use the latest platform from CrossBuildPlatforms
    val platform = CommonParams.CrossBuildPlatforms.last()
    val platformDot = "20${platform.take(2)}.${platform.last()}"
    
    script {
      name = "add plugins to qodana"
      id = "add_plugins_to_qodana_$platform"
      scriptContent = """
        #!/bin/bash
        set -euxo
        
        mkdir %system.agent.persistent.cache%/plugins

        cd %system.teamcity.build.checkoutDir%/tc-artifacts
        
        shopt -s globstar nullglob
        cp -- **/plugin-bazel.zip .
        
        unzip plugin-bazel.zip -d %system.agent.persistent.cache%/plugins
      """.trimIndent()
    }
    
    qodana {
      name = "run qodana"
      id = "run_qodana_$platform"
      reportAsTests = false
      linter = customLinter {
        image = "${analysisDef.linterImage}:$platformDot-nightly"
      }
      additionalDockerArguments = "-v %system.agent.persistent.cache%/plugins/plugin-bazel:/opt/idea/custom-plugins/plugin-bazel"
      additionalQodanaArguments = listOf(
        "--property=bsp.build.project.on.sync=true",
        "--property=idea.is.internal=true",
        "--report-dir /data/results/report",
        "--save-report",
        "--baseline ${analysisDef.qodanaBaseline}",
        "--config ${analysisDef.qodanaConfig}"
      ).joinToString("\n")
      this.cloudToken = "%${analysisDef.cloudTokenKey}%"
      collectAnonymousStatistics = true
    }
    
    if (analysisDef.unchanged != null) {
      script {
        this.name = "create requirements.txt"
        id = "create_requirements_txt_$platform"
        scriptContent = "echo \"requests\" > %system.teamcity.build.checkoutDir%/requirements.txt"
      }
      python {
        this.name = "check changes for anomalies"
        id = "check_changes_for_anomalies_$platform"
        environment = venv { }
        command = script {
          content = File("scripts/evaluate_qodana.py").readText()
          scriptArguments = "--unchanged ${analysisDef.unchanged} --diff ${analysisDef.diff}"
        }
      }
    }
  },
  params = {
    password(analysisDef.cloudTokenKey, analysisDef.cloudTokenCredentials, 
             label = analysisDef.cloudTokenKey, 
             description = "Qodana token for ${analysisDef.name} statistics", 
             display = ParameterDisplay.HIDDEN)
  },
  dockerSupport = {
    loginToRegistry = on {
      dockerRegistryId = "PROJECT_EXT_3"
    }
  },
  failureConditions = if (analysisDef.allowFailure) {
    {
      testFailure = false
      nonZeroExitCode = false
      javaCrash = false
    }
  } else {
    {}
  }
)

/**
 * Factory for creating static analysis test build types.
 */
object StaticAnalysisFactory {
  // Define the available static analysis tests (Android builds removed)
  private val analysisTests = listOf(
    AnalysisDef(
      name = "Hirschgarten",
      vcsRoot = VcsRootHirschgarten,  // Uses default VCS root
      cloudTokenKey = "qodana.cloud.token.hirschgarten",
      cloudTokenCredentials = "credentialsJSON:d57ead0e-b567-440d-817e-f92e084a1cc0",
      enabled = true,
      qodanaConfig = "tools/qodana/qodana.yaml",
      qodanaBaseline = "tools/qodana/qodana.sarif.json"
    ),
    AnalysisDef(
      name = "Hirschgarten",
      vcsRoot = VcsRootHirschgartenSpace,
      cloudTokenKey = "qodana.cloud.token.hirschgarten",
      cloudTokenCredentials = "credentialsJSON:d57ead0e-b567-440d-817e-f92e084a1cc0",
      enabled = true,
      qodanaConfig = "tools/qodana/qodana.yaml",
      qodanaBaseline = "tools/qodana/qodana.sarif.json"
    ),
    AnalysisDef(
      name = "Bazel",
      vcsRoot = VcsRootBazelQodana,
      cloudTokenKey = "qodana.cloud.token.bazel",
      cloudTokenCredentials = "credentialsJSON:34041ec3-8e8c-4934-b3e2-0143ff2aee5e",
      enabled = true,
      allowFailure = true
    ),
    AnalysisDef(
      name = "BuildBuddy",
      vcsRoot = VcsRootBuildBuddyQodana,
      cloudTokenKey = "qodana.cloud.token.buildbuddy",
      cloudTokenCredentials = "credentialsJSON:8f62c38e-0dd7-4f3f-8432-cfb1c04cc021",
      enabled = true,
      linterImage = CommonParams.DockerQodanaGoImage
    )
  )
  
  /** All static analysis test build types. */
  val AllAnalysisTests: List<BaseBuildType> by lazy { createAnalysisTests() }
  
  /** Only enabled analysis tests for the pipeline. */
  val EnabledAnalysisTests: List<BaseBuildType> by lazy { createAnalysisTests(onlyEnabled = true) }
  val EnabledAnalysisTestsGitHub: List<BaseBuildType> by lazy {
    // Include all enabled analyses that are not explicitly Space-only.
    // This collects Hirschgarten (GitHub), Bazel and BuildBuddy analyses.
    analysisTests
      .filter { it.enabled && it.vcsRoot.id != VcsRootHirschgartenSpace.id }
      .map { analysisDef -> StaticAnalysisTest(analysisDef, idNamespace = "GitHub") }
  }
  val EnabledAnalysisTestsSpace: List<BaseBuildType> by lazy {
    analysisTests
      .filter {
        it.enabled && (
          it.vcsRoot.id == VcsRootHirschgartenSpace.id ||
          it.vcsRoot.id == VcsRootBazelQodana.id ||
          it.vcsRoot.id == VcsRootBuildBuddyQodana.id
        )
      }
      .map { analysisDef -> StaticAnalysisTest(analysisDef, idNamespace = "Space") }
  }
  
  private fun createAnalysisTests(onlyEnabled: Boolean = false): List<BaseBuildType> =
    analysisTests
      .filter { if (onlyEnabled) it.enabled else true }
      .map { analysisDef ->
        StaticAnalysisTest(analysisDef)
      }
}
