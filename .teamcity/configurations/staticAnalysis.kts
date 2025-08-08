package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.python
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import java.io.File

// Base class for static analysis builds
open class Analysis(
  cloudToken: String,
  params: ParametrizedWithType.() -> Unit = {},
  repo: String? = null,
  linterImage: String = Utils.CommonParams.DockerQodanaImage,
  unchanged: String? = null,
  diff: String = "0",
  failureConditions: FailureConditions.() -> Unit = {},
) : BaseConfiguration.BaseBuildType(
  name = "[analysis] Qodana ${if (repo != null) {"$repo"} else {"Hirschgarten"}}",
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
    equals("container.engine.osType", "linux")
  },
  artifactRules = "+:.teamcity/qodana/*.zip",
  dependencies = {
    snapshot(ProjectBuild) {}
    artifacts(ProjectBuild) {
      artifactRules = Utils.CommonParams.QodanaArtifactRules
    }
  },
  steps = {
    // TODO: remove this hardcoded value for platform once we move to ultimate-teamcity-config
    val platform = "252"
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
    if (repo != null) {
      script {
        name = "clone $repo"
        id = "clone_${repo}_$platform"
        scriptContent = """
          #!/bin/bash
          set -euxo

          if [ ! -d "%system.agent.persistent.cache%/$repo" ]; then
            git clone --depth 1 https://github.com/JetBrainsBazelBot/$repo %system.agent.persistent.cache%/$repo
          
            cd %system.agent.persistent.cache%/$repo
            
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
      name = "Qodana ${if (repo != null) {"$repo"} else {"Hirschgarten"}}"
      linterImage = linterImage
      reportAsTests = true
      //cloudToken = cloudToken
      displayCloudToken = cloudToken
      resultsPath = "%system.agent.persistent.cache%/.qodana/report/$platform"
      cacheFolder = "%system.agent.persistent.cache%/.qodana/cache/$platform"
      projectPath = if (repo != null) {"%system.agent.persistent.cache%/$repo"} else {"%system.teamcity.build.checkoutDir%"}
      reportId = if (repo != null) {"bazel-${repo.lowercase()}"} else {"hirschgarten-bazel"}
      additionalDockerArguments = if (unchanged != null) {
        """--env QODANA_TOKEN=$cloudToken --privileged -v %system.agent.persistent.cache%/plugins:/opt/idea/plugins"""
      } else {
        """--env QODANA_TOKEN=$cloudToken --privileged -v %system.agent.persistent.cache%/plugins:/opt/idea/plugins"""
      }
      
      arguments = if (unchanged != null) {
        """--profile-name="qodana.recommended" -u:$unchanged"""
      } else {
        """--profile-name="qodana.recommended""""
      }
      gitBranch = "%teamcity.build.branch%"
      gitCommit = if (repo != null) {"%env.GIT_COMMIT%"} else {"%build.vcs.number%"}
      gitRepoUrl = if (repo != null) {"%env.GIT_REPO_URL%"} else {"https://github.com/JetBrains/hirschgarten"}
      envVariables = "QODANA_REPORT_ID" to if (repo != null) {"bazel-${repo.lowercase()}"} else {"hirschgarten-bazel"}
    }
    
    script {
      name = "save Qodana report $platform"
      id = "save_qodana_report_$platform"
      scriptContent = """
        #!/bin/bash
        set -uxo
        
        QODANA_PATH="%system.agent.persistent.cache%/.qodana/report/$platform"
        PROJECT_PATH="%system.teamcity.build.checkoutDir%"
        
        mkdir -p ${'$'}{PROJECT_PATH}/.teamcity/qodana
        cd ${'$'}{QODANA_PATH}
        
        zip -rq9 ${'$'}{PROJECT_PATH}/.teamcity/qodana/qodana-report-${platformDot}.zip report.json .descriptions qodana.sarif.json log -x log/\*
      """.trimIndent()
    }
  },
  failureConditions = failureConditions,
  params = {
    params()
    password("qodana.cloud.token", "credentialsJSON:e9ba85df-94fa-4cf1-adfe-f0cc5baff45b", label = "Qodana Cloud Token", description = "Qodana Cloud Token", display = ParameterDisplay.HIDDEN)
  }
)

// Define all static analysis configurations
object StaticAnalysis {
  object Hirschgarten : Analysis(
    cloudToken = "%qodana.cloud.token%",
    params = {
      text("qodana.ide", value = "IC-${Utils.CommonParams.QodanaVersion}", label = "Qodana IDE", description = "The name of the IDEA-based IDE installation used by Qodana")
      text("qodana.licence", value = "license", label = "Qodana Licence Alias", description = "Alias of the license from the license vault")
    },
    unchanged = File("./ide-baseline.sarif.json").readText(),
    failureConditions = {
      executionTimeoutMin = 240
    }
  )

  object Bazel : Analysis(
    cloudToken = "%qodana.cloud.token%",
    params = {
      text("qodana.ide", value = "IC-${Utils.CommonParams.QodanaVersion}", label = "Qodana IDE", description = "The name of the IDEA-based IDE installation used by Qodana")
      text("qodana.licence", value = "license", label = "Qodana Licence Alias", description = "Alias of the license from the license vault")
    },
    repo = "bazel",
    linterImage = "jetbrains/qodana-jvm-community:${Utils.CommonParams.QodanaVersion}",
    diff = "1000",
    failureConditions = {
      executionTimeoutMin = 240
    }
  )

  object AndroidBazelRules : Analysis(
    cloudToken = "%qodana.cloud.token%",
    params = {
      text("qodana.ide", value = "IC-${Utils.CommonParams.QodanaVersion}", label = "Qodana IDE", description = "The name of the IDEA-based IDE installation used by Qodana")
      text("qodana.licence", value = "license", label = "Qodana Licence Alias", description = "Alias of the license from the license vault")
    },
    repo = "android_bazel_rules",
    linterImage = "jetbrains/qodana-jvm-android:${Utils.CommonParams.QodanaVersion}",
    failureConditions = {
      executionTimeoutMin = 240
    }
  )

  object AndroidTestdpc : Analysis(
    cloudToken = "%qodana.cloud.token%",
    params = {
      text("qodana.ide", value = "IC-${Utils.CommonParams.QodanaVersion}", label = "Qodana IDE", description = "The name of the IDEA-based IDE installation used by Qodana")
      text("qodana.licence", value = "license", label = "Qodana Licence Alias", description = "Alias of the license from the license vault")
    },
    repo = "android_testdpc",
    linterImage = "jetbrains/qodana-jvm-android:${Utils.CommonParams.QodanaVersion}",
    failureConditions = {
      executionTimeoutMin = 240
    }
  )

  object JetpackCompose : Analysis(
    cloudToken = "%qodana.cloud.token%",
    params = {
      text("qodana.ide", value = "IC-${Utils.CommonParams.QodanaVersion}", label = "Qodana IDE", description = "The name of the IDEA-based IDE installation used by Qodana")
      text("qodana.licence", value = "license", label = "Qodana Licence Alias", description = "Alias of the license from the license vault")
    },
    repo = "compose_jetpack",
    linterImage = "jetbrains/qodana-jvm-android:${Utils.CommonParams.QodanaVersion}",
    failureConditions = {
      executionTimeoutMin = 240
    }
  )
}
