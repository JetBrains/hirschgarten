package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.python
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import java.io.File

/**
 * Data class for static analysis test definitions
 */
data class AnalysisDef(
  val name: String,
  val repo: String? = null,
  val cloudTokenKey: String,
  val cloudTokenCredentials: String,
  val linterImage: String = Utils.CommonParams.DockerQodanaImage,
  val unchanged: String? = null,
  val diff: String = "0",
  val allowFailure: Boolean = false,
  val teamcityTokenNeeded: Boolean = false,
  val enabled: Boolean = true  // Controls whether this analysis is included in the pipeline
)

/**
 * Static analysis configuration using Qodana.
 * 
 * @param analysisDef The analysis definition containing test parameters
 */
class StaticAnalysisTest(
  private val analysisDef: AnalysisDef
) : BaseConfiguration.BaseBuildType(
  name = "[analysis] Qodana ${analysisDef.repo ?: "Hirschgarten"}",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
    equals("container.engine.osType", "linux")
  },
  artifactRules = "+:.teamcity/qodana/*.zip",
  dependencies = {
    // Depend on the latest platform build (last in the list)
    val latestPlatformBuild = PluginBuild.Factory.ForAllPlatforms.last()
    snapshot(latestPlatformBuild) {}
    artifacts(latestPlatformBuild) {
      artifactRules = Utils.CommonParams.QodanaArtifactRules
    }
  },
  steps = {
    // Use the latest platform from CrossBuildPlatforms
    val platform = Utils.CommonParams.CrossBuildPlatforms.last()
    val platformDot = "20${platform.take(2)}.${platform.last()}"
    
    script {
      name = "add plugins to qodana"
      id = "add_plugins_to_qodana_$platform"
      scriptContent = """
        #!/bin/bash
        set -euxo
        
        rm -rf %system.agent.persistent.cache%/plugins 
        mkdir %system.agent.persistent.cache%/plugins
        
        unzip %system.teamcity.build.checkoutDir%/tc-artifacts/plugin-bazel-$platform.zip -d %system.agent.persistent.cache%/plugins
      """.trimIndent()
    }
    
    if (analysisDef.repo != null) {
      script {
        name = "clone ${analysisDef.repo}"
        id = "clone_${analysisDef.repo}_$platform"
        scriptContent = """
          #!/bin/bash
          set -euxo

          if [ ! -d "%system.agent.persistent.cache%/${analysisDef.repo}" ]; then
            git clone --depth 1 https://github.com/JetBrainsBazelBot/${analysisDef.repo} %system.agent.persistent.cache%/${analysisDef.repo}
          
            cd %system.agent.persistent.cache%/${analysisDef.repo}
            
            # Set commit hash
            echo "##teamcity[setParameter name='env.GIT_COMMIT' value='${'$'}(git rev-parse HEAD)']"
            echo %env.GIT_COMMIT%
          
            # Set repo URL
            echo "##teamcity[setParameter name='env.GIT_REPO_URL' value='${'$'}(git config --get remote.origin.url)']"
            echo %env.GIT_REPO_URL%
          fi
        """.trimIndent()
      }
    }
    
    qodana {
      name = "run qodana"
      id = "run_qodana_$platform"
      reportAsTests = false
      workingDir = if (analysisDef.repo != null) { "%system.agent.persistent.cache%/${analysisDef.repo}" } else { "" }
      linter = customLinter {
        image = "${analysisDef.linterImage}:$platformDot-nightly"
      }
      additionalDockerArguments = """
        -v %system.agent.persistent.cache%/plugins/plugin-bazel:/opt/idea/custom-plugins/plugin-bazel
        ${if (analysisDef.repo != null) {"-v %system.agent.persistent.cache%/${analysisDef.repo}/qodana.yaml:%system.agent.persistent.cache%/${analysisDef.repo}/qodana.yaml"} else {""}}
        ${if (analysisDef.repo != null) {"-e QODANA_REMOTE_URL=\"%env.GIT_REPO_URL%\""} else {""}}
        ${if (analysisDef.repo != null) {"-e QODANA_REVISION=\"%env.GIT_COMMIT%\""} else {""}}
      """.trimIndent()
      additionalQodanaArguments = """
        --property=bsp.build.project.on.sync=true
        --property=idea.is.internal=true
        --property=idea.kotlin.plugin.use.k2=true
        --report-dir /data/results/report
        --save-report
        ${if (analysisDef.repo != null) {"--property=bsp.android.support=true"} else {""}}
        --baseline ${if (analysisDef.repo != null) {"qodana.sarif.json"} else {"tools/qodana/qodana.sarif.json"}}
        --config ${if (analysisDef.repo != null) {"%system.agent.persistent.cache%/${analysisDef.repo}/qodana.yaml"} else {"tools/qodana/qodana.yaml"}}
      """.trimIndent()
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
    param("env.GIT_REPO_URL", "")
    param("env.GIT_COMMIT", "")
    password(analysisDef.cloudTokenKey, analysisDef.cloudTokenCredentials, 
             label = analysisDef.cloudTokenKey, 
             description = "Qodana token for ${analysisDef.repo ?: "Hirschgarten"} statistics", 
             display = ParameterDisplay.HIDDEN)
    if (analysisDef.teamcityTokenNeeded) {
      password("jetbrains.bazel.teamcity.token", Utils.CredentialsStore.BazelTeamcityToken, 
               label = "jetbrains.bazel.teamcity.token", 
               description = "Bazel TCC token", 
               display = ParameterDisplay.HIDDEN)
    }
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
  // Define the available static analysis tests
  private val analysisTests = listOf(
    AnalysisDef(
      name = "Hirschgarten",
      cloudTokenKey = "qodana.cloud.token.hirschgarten",
      cloudTokenCredentials = "credentialsJSON:d57ead0e-b567-440d-817e-f92e084a1cc0",
      enabled = true  // Enabled in pipeline
    ),
    AnalysisDef(
      name = "Bazel",
      repo = "bazel",
      cloudTokenKey = "qodana.cloud.token.bazel",
      cloudTokenCredentials = "credentialsJSON:34041ec3-8e8c-4934-b3e2-0143ff2aee5e",
      teamcityTokenNeeded = true,
      enabled = true  // Enabled in pipeline
    ),
    AnalysisDef(
      name = "AndroidBazelRules",
      repo = "android-forked-bazel-rules-project",
      cloudTokenKey = "qodana.cloud.token.android-bazel-rules",
      cloudTokenCredentials = "credentialsJSON:92c2f175-fa8f-4215-a527-f76da7f98b25",
      linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
      unchanged = "3",
      diff = "1",
      allowFailure = true,
      teamcityTokenNeeded = true,
      enabled = false  // Not included in pipeline
    ),
    AnalysisDef(
      name = "AndroidTestdpc",
      repo = "android-testdpc",
      cloudTokenKey = "qodana.cloud.token.android-testdpc",
      cloudTokenCredentials = "credentialsJSON:9d593e23-4a3c-40a1-834b-cb6883135cfd",
      linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
      unchanged = "3281",
      diff = "10",
      allowFailure = true,
      teamcityTokenNeeded = true,
      enabled = false  // Not included in pipeline
    ),
    AnalysisDef(
      name = "JetpackCompose",
      repo = "jetpack_compose",
      cloudTokenKey = "qodana.cloud.token.jetpack-compose",
      cloudTokenCredentials = "credentialsJSON:0579ad7c-87ad-4bc0-af74-8bdc1cc0c6a5",
      linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
      unchanged = "1",
      allowFailure = true,
      teamcityTokenNeeded = true,
      enabled = false  // Not included in pipeline
    )
  )
  
  /** All static analysis test build types. */
  val AllAnalysisTests: List<BaseConfiguration.BaseBuildType> by lazy { createAnalysisTests() }
  
  /** Only enabled analysis tests for the pipeline. */
  val EnabledAnalysisTests: List<BaseConfiguration.BaseBuildType> by lazy { 
    createAnalysisTests(onlyEnabled = true) 
  }
  
  private fun createAnalysisTests(onlyEnabled: Boolean = false): List<BaseConfiguration.BaseBuildType> =
    analysisTests
      .filter { if (onlyEnabled) it.enabled else true }
      .map { analysisDef ->
        StaticAnalysisTest(analysisDef)
      }
}
