package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class E2ETest(
  vcsRoot: GitVcsRoot,
  targets: String,
  failureConditions: FailureConditions.() -> Unit = {},
  requirements: (Requirements.() -> Unit)? = null,
) : BaseConfiguration.BaseBuildType(
    name = "[e2e tests] $targets",
    vcsRoot = vcsRoot,
    failureConditions = failureConditions,
    artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
    steps = {
      bazel {
        this.name = "test $targets"
        this.command = "test"
        this.targets = targets
        // This fixes FileUtils.getCacheDirectory in integration tests
        this.arguments =
          "--sandbox_writable_path=/home/hirschuser/.cache ${Utils.CommonParams.BazelCiSpecificArgs}"
        logging = BazelStep.Verbosity.Diagnostic
        toolPath = "/usr/local/bin"
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
      script {
        id = "simpleRunner"
        scriptContent =
          """
          #!/bin/bash
          set -euxo
          
          cp -R /home/teamcity/agent/system/.persistent_cache/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/testlogs .
          """.trimIndent()
      }
    },
    requirements = requirements,
  )

open class SampleRepo(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:sample_repo_test",
  )

object SampleRepoGitHub : SampleRepo(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object SampleRepoSpace : SampleRepo(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class LocalJdk(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:local_jdk_test",
  )

object LocalJdkGitHub : LocalJdk(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object LocalJdkSpace : LocalJdk(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class RemoteJdk(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:remote_jdk_test",
  )

object RemoteJdkGitHub : RemoteJdk(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object RemoteJdkSpace : RemoteJdk(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class ServerDownloadsBazelisk(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:server_downloads_bazelisk_test",
    failureConditions = {
      testFailure = false
      nonZeroExitCode = false
      javaCrash = false
    },
  )

object ServerDownloadsBazeliskGitHub : ServerDownloadsBazelisk(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object ServerDownloadsBazeliskSpace : ServerDownloadsBazelisk(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class KotlinProject(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:kotlin_project_test",
  )

object KotlinProjectGitHub : KotlinProject(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object KotlinProjectSpace : KotlinProject(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class AndroidProject(vcsRoot: GitVcsRoot, requirements: (Requirements.() -> Unit)? = null) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:android_project_test",
    requirements = requirements,
    failureConditions = {
      testFailure = false
      nonZeroExitCode = false
    },
  )

object AndroidProjectGitHub : AndroidProject(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "-Large")
    equals("container.engine.osType", "linux")
  },
)

object AndroidProjectSpace : AndroidProject(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class AndroidKotlinProject(vcsRoot: GitVcsRoot, requirements: (Requirements.() -> Unit)? = null) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:android_kotlin_project_test",
    requirements = requirements,
    failureConditions = {
      testFailure = false
      nonZeroExitCode = false
    },
  )

object AndroidKotlinProjectGitHub : AndroidKotlinProject(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "-Large")
    equals("container.engine.osType", "linux")
  },
)

object AndroidKotlinProjectSpace : AndroidKotlinProject(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class ScalaProject(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:enabled_rules_test",
  )

object ScalaProjectGitHub : ScalaProject(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object ScalaProjectSpace : ScalaProject(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class PythonProject(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:python_project_test",
  )

object PythonProjectGitHub : PythonProject(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object PythonProjectSpace : PythonProject(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class JavaDiagnostics(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:java_diagnostics_test",
  )

object JavaDiagnosticsGitHub : JavaDiagnostics(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object JavaDiagnosticsSpace : JavaDiagnostics(
  vcsRoot = BaseConfiguration.SpaceVcs,
)

open class ManualTargets(vcsRoot: GitVcsRoot) :
  E2ETest(
    vcsRoot = vcsRoot,
    targets = "//server/e2e:allow_manual_targets_sync_test",
  )

object ManualTargetsGitHub : ManualTargets(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object ManualTargetsSpace : ManualTargets(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
