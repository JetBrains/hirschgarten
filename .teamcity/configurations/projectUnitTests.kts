package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class UnitTests(vcsRoot: GitVcsRoot, requirements: (Requirements.() -> Unit)? = null) :
  BaseConfiguration.BaseBuildType(
    name = "[unit tests] project unit tests",
    artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
    requirements = requirements,
    steps = {
      bazel {
        name = "bazel test //... (without single-job targets)"
        command = "test"
        targets = "//... -//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/... -//server/server/src/test/kotlin/org/jetbrains/bazel/server/sync/..."
        arguments = Utils.CommonParams.BazelCiSpecificArgs
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
      bazel {
        name = "bazel test single-job targets"
        command = "test"
        targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/... //server/server/src/test/kotlin/org/jetbrains/bazel/server/sync/..."
        arguments = Utils.CommonParams.BazelCiSpecificArgs + " --jobs 1"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
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
    vcsRoot = vcsRoot,
  )

object GitHub : UnitTests(
  vcsRoot = BaseConfiguration.GitHubVcs,
  requirements = {
    endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
    equals("container.engine.osType", "linux")
  },
)

