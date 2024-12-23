package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.python
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.io.File


open class Analyze(
vcsRoot: GitVcsRoot,
cloudToken: String,
params: ParametrizedWithType.() -> Unit = {},
repo: String? = null,
linterImage: String = Utils.CommonParams.DockerQodanaImage,
unchanged: String? = null,
diff: String = "0",
) : BaseConfiguration.BaseBuildType(
    name = "[analysis] Qodana ${if (repo != null) {"$repo"} else {"Hirschgarten"}}",
    requirements = {
        endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
        equals("container.engine.osType", "linux")
    },
    artifactRules = "+:.teamcity/qodana/*.zip",
    dependencies = {
        // select build dependency based on vcs used
        if (vcsRoot == BaseConfiguration.SpaceVcs){
            snapshot(ProjectBuild.Space) {}
            artifacts(ProjectBuild.Space) {
                artifactRules = Utils.CommonParams.QodanaArtifactRules
          }
        } else {
            snapshot(ProjectBuild.GitHub) {}
            artifacts(ProjectBuild.GitHub) {
                artifactRules = Utils.CommonParams.QodanaArtifactRules
            }
        }
    },
    steps = {
      Utils.CommonParams.CrossBuildPlatforms.forEach { platform ->
        script {
            name = "add plugins to qodana"
            id = "add_plugins_to_qodana"
            scriptContent = """
                #!/bin/bash
                set -euxo
                
                rm -rf %system.agent.persistent.cache%/plugins 
                mkdir %system.agent.persistent.cache%/plugins
                
                unzip %system.teamcity.build.checkoutDir%/tc-artifacts/intellij-bazel-$platform.zip -d %system.agent.persistent.cache%/plugins
                unzip %system.teamcity.build.checkoutDir%/tc-artifacts/intellij-bsp-$platform.zip -d %system.agent.persistent.cache%/plugins
            """.trimIndent()
        }
        if (repo != null) {
          script {
            name = "clone $repo"
            id = "clone_$repo"
            scriptContent = """
              #!/bin/bash
              set -euxo

              git clone --depth 1 https://github.com/JetBrainsBazelBot/$repo %system.agent.persistent.cache%/$repo

              cd %system.agent.persistent.cache%/$repo
              # Set commit hash
              echo "##teamcity[setParameter name='env.GIT_COMMIT' value='${'$'}(git rev-parse HEAD)']"
              echo %env.GIT_COMMIT%

              # Set repo URL
              echo "##teamcity[setParameter name='env.GIT_REPO_URL' value='${'$'}(git config --get remote.origin.url)']"
              echo %env.GIT_REPO_URL%
            """.trimIndent()
          }
        }
        qodana {
            name = "run qodana"
            id = "run_qodana"
            reportAsTests = false
          workingDir = if (repo != null ){ "%system.agent.persistent.cache%/$repo" } else {""}
            linter = customLinter {
                image = linterImage
            }
            additionalDockerArguments = """
                -v %system.agent.persistent.cache%/plugins/intellij-bazel:/opt/idea/custom-plugins/intellij-bazel
                -v %system.agent.persistent.cache%/plugins/intellij-bsp:/opt/idea/custom-plugins/intellij-bsp
                ${if (repo != null) {"-v %system.agent.persistent.cache%/$repo/qodana.yaml:%system.agent.persistent.cache%/$repo/qodana.yaml"} else {""}}
                ${if (repo != null) {"-e QODANA_REMOTE_URL=\"%env.GIT_REPO_URL%\""} else {""}}
                ${if (repo != null) {"-e QODANA_REVISION=\"%env.GIT_COMMIT%\""} else {""}}
            """.trimIndent()
            additionalQodanaArguments = """
                  --property=bsp.build.project.on.sync=true
                  --property=idea.is.internal=true
                  --property=idea.kotlin.plugin.use.k2=true
                  --report-dir /data/results/report
                  --save-report
                  ${if (repo != null) {"--property=bsp.android.support=true"} else {""}}
                  --baseline ${if (repo != null) {"qodana.sarif.json"} else {"tools/qodana/qodana.sarif.json"}}
                  --config ${if (repo != null) {"%system.agent.persistent.cache%/$repo/qodana.yaml"} else {"tools/qodana/qodana.yaml"}}
              """.trimIndent()
              this.cloudToken = cloudToken
              collectAnonymousStatistics = true
        }
        if (unchanged != null) {
          script {
            this.name = "create requirements.txt"
            id = "create_requirements_txt"
            scriptContent = "echo \"requests\" > %system.teamcity.build.checkoutDir%/requirements.txt"
          }
          python {
            this.name = "check changes for anomalies"
            id = "check_changes_for_anomalies"
            environment = venv { }
            command = script {
              content = File("scripts/evaluate_qodana.py").readText()
              scriptArguments = "--unchanged $unchanged --diff $diff"
            }
          }
        }
      }
    },
    vcsRoot = vcsRoot,
    params = {
      param("env.GIT_REPO_URL", "")
      param("env.GIT_COMMIT", "")
      params(this)
      if (repo != null) {password("jetbrains.bazel.teamcity.token", "credentialsJSON:f47ac10b-58cc-4372-a567-0e02b2c3d479", label = "jetbrains.bazel.teamcity.token", description = "Bazel TCC token", display = ParameterDisplay.HIDDEN) }
    },
    dockerSupport = {
        loginToRegistry = on {
        dockerRegistryId = "PROJECT_EXT_3"
        }
    },
    failureConditions = { executionTimeoutMin = 30 },

)

open class Hirschgarten (
  vcsRoot: GitVcsRoot
): Analyze(
  vcsRoot = vcsRoot,
  cloudToken = "%qodana.cloud.token.hirschgarten%",
  params = {
    password("qodana.cloud.token.hirschgarten", "credentialsJSON:d57ead0e-b567-440d-817e-f92e084a1cc0", label = "qodana.cloud.token.hirschgarten", description = "Qodana token for Hirschgarten statistics", display = ParameterDisplay.HIDDEN)
  }
)

object HirschgartenGitHub : Hirschgarten(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object HirschgartenSpace : Hirschgarten(
    vcsRoot = BaseConfiguration.SpaceVcs,
)

open class Bazel(
  vcsRoot: GitVcsRoot
): Analyze(
  vcsRoot = vcsRoot,
  cloudToken = "%qodana.cloud.token.bazel%",
  params = {
    password("qodana.cloud.token.bazel", "credentialsJSON:34041ec3-8e8c-4934-b3e2-0143ff2aee5e", label = "qodana.cloud.token.bazel", description = "Qodana token for Bazel statistics", display = ParameterDisplay.HIDDEN)
  },
  repo = "bazel",
)

object BazelGitHub : Bazel(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object BazelSpace : Bazel(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class AndroidBazelRules(
  vcsRoot: GitVcsRoot
): Analyze(
  vcsRoot = vcsRoot,
  cloudToken = "%qodana.cloud.token.android-bazel-rules%",
  params = {
    password("qodana.cloud.token.android-bazel-rules", "credentialsJSON:92c2f175-fa8f-4215-a527-f76da7f98b25", label = "qodana.cloud.token.android-bazel-rules", description = "Qodana token for Android Bazel Rules statistics", display = ParameterDisplay.HIDDEN)
  },
  repo = "android-forked-bazel-rules-project",
  linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
  unchanged = "3",
  diff = "1",
)

object AndroidBazelRulesGitHub : AndroidBazelRules (
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object AndroidBazelRulesSpace : AndroidBazelRules (
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class AndroidTestdpc(
  vcsRoot: GitVcsRoot
): Analyze(
  vcsRoot = vcsRoot,
  cloudToken = "%qodana.cloud.token.android-testdpc%",
  params = {
    password("qodana.cloud.token.android-testdpc", "credentialsJSON:9d593e23-4a3c-40a1-834b-cb6883135cfd", label = "qodana.cloud.token.android-testdpc", description = "Qodana token for Android TestDPC statistics", display = ParameterDisplay.HIDDEN)
  },
  repo = "android-testdpc",
  linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
  unchanged = "3281",
  diff = "10",
)

object AndroidTestdpcGitHub : AndroidTestdpc (
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object AndroidTestdpcSpace : AndroidTestdpc (
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class JetpackCompose(
  vcsRoot: GitVcsRoot
): Analyze(
  vcsRoot = vcsRoot,
  cloudToken = "%qodana.cloud.token.jetpack-compose%",
  params = {
    password("qodana.cloud.token.jetpack-compose", "credentialsJSON:0579ad7c-87ad-4bc0-af74-8bdc1cc0c6a5", label = "qodana.cloud.token.jetpack-compose", description = "Qodana token for Jetpack Compose statistics", display = ParameterDisplay.HIDDEN)
  },
  repo = "jetpack_compose",
  linterImage = Utils.CommonParams.DockerQodanaAndroidImage,
  unchanged = "1"
)

object JetpackComposeGitHub : JetpackCompose (
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object JetpackComposeSpace : JetpackCompose (
  vcsRoot = BaseConfiguration.SpaceVcs,
)
