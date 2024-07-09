package configurations.pluginBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

open class Benchmark (
    vcsRoot: GitVcsRoot,
) : BaseConfiguration.BaseBuildType(

    name = "[benchmark] Plugin BSP 10 targets",
    vcsRoot = vcsRoot,
    artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-testlogs/** => testlogs.zip",
    steps = {
        val sysArgs = "--jvmopt=\"-Dbsp.benchmark.project.path=%system.teamcity.build.tempDir%/project_10\" --jvmopt=\"-Dbsp.benchmark.teamcity.url=https://bazel.teamcity.com\""
        script {
            this.name = "install xvfb and generate project for benchmark"
            id = "install_xvfb_and_generate_project_for_benchmark"
            scriptContent = """
                        #!/bin/bash
                        set -euxo pipefail
                        
                        sudo apt-get update
                        sudo apt-get install -y xvfb
                        
                        bazel run //server/bspcli:generator -- %system.teamcity.build.tempDir%/project_10 10 --targetssize 1
                    """.trimIndent()
        }
        bazel {
            name = "run benchmark"
            id = "run_benchmark"
            command = "test"
            targets = "//plugin-bsp/performance-testing"
            arguments = "--jvmopt=\"-Xmx12g\" $sysArgs --sandbox_writable_path=/"
            param("toolPath", "/usr/local/bin")
        }
    }
)


object GitHub : Benchmark(
    vcsRoot = BaseConfiguration.GitHubVcs
)

object Space : Benchmark(
    vcsRoot = BaseConfiguration.SpaceVcs
)