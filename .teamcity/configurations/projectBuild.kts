package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build(vcsRoot: GitVcsRoot) :
    BaseConfiguration.BaseBuildType(
        name = "[build] build project",
        vcsRoot = vcsRoot,
        artifactRules = "+:%system.teamcity.build.checkoutDir%/plugin-*.zip",
        steps = {
            Utils.CommonParams.CrossBuildPlatforms.forEach { platform ->
                bazel {
                val platformDot = "${platform.take(2)}.${platform.last()}"

                // enabled = (platform != "242")
                executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE

                name = "build //... $platform"
                command = "build"
                targets = "//..."
                arguments = "--define=ij_product=intellij-20$platformDot ${Utils.CommonParams.BazelCiBuildSpecificArgs}"
                logging = BazelStep.Verbosity.Diagnostic
                Utils.DockerParams.get().forEach { (key, value) ->
                  param(key, value)
                }
            }
            script {
              name = "save plugin $platform"
              id = "save_plugin_$platform"
              scriptContent = """
                  #!/bin/bash
                  set -uxo
                  
                  BAZEL_BIN_PATH="%system.agent.persistent.cache%/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/bin"
                  PROJECT_PATH="%system.teamcity.build.checkoutDir%"  
                  cp ${"$"}{BAZEL_BIN_PATH}/plugin-bazel/plugin-bazel.zip ${"$"}{PROJECT_PATH}/plugin-bazel-$platform.zip
              """.trimIndent()
            }
        }
    },
  )

object GitHub : Build(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

