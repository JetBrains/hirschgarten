package configurations.pluginBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class UnitTests(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[tests] Plugin BSP unit tests",
    artifactRules = "+:/home/teamcity/.cache/bazel/_bazel_teamcity/*/execroot/_main/bazel-out/k8-fastbuild/testlogs/** => testlogs.zip",
    vcsRoot = vcsRoot,
    steps = {
      bazel {
        this.name = "run unit tests"
        id = "run_unit_tests"
        command = "test"
        targets = "//plugin-bsp/..."
        param("toolPath", "/usr/local/bin")
      }
    },
  )

object GitHub : UnitTests(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : UnitTests(
  vcsRoot = BaseConfiguration.SpaceVcs,
)
