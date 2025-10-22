package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object ProjectUnitTests : BaseBuildType(
    name = "[unit tests] project unit tests",
    artifactRules = CommonParams.BazelTestlogsArtifactRules,
    requirements = {
      endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Large")
      equals("container.engine.osType", "linux")
    },
    steps = {
      bazel {
        name = "bazel test //... (without single-job targets)"
        command = "test"
        targets = "//... -//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/... -//server/server/src/test/kotlin/org/jetbrains/bazel/server/sync/..."
        arguments = CommonParams.BazelCiSpecificArgs
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
      bazel {
        name = "bazel test single-job targets"
        command = "test"
        targets = "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/... //server/server/src/test/kotlin/org/jetbrains/bazel/server/sync/..."
        arguments = CommonParams.BazelCiSpecificArgs + " --jobs 1"
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        DockerParams.get().forEach { (key, value) ->
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
  )

