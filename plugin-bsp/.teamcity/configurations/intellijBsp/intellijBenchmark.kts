package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

open class Benchmark (
    vcsRoot: GitVcsRoot,
) : BaseConfiguration.BaseBuildType(

    name = "[benchmark] 10 targets",
    vcsRoot = vcsRoot,
    steps = {
        var sysArgs = "-DDO_NOT_REPORT_ERRORS=true  -Dbsp.benchmark.project.path=/tmp/project_10"
        script {
            this.name = "generate 10 targets project for benchmark"
            id = "generate_10_targets_project_for_benchmark"
            scriptContent = """
                        #!/bin/bash
                        set -euxo pipefail
                        
                        git clone https://github.com/JetBrains/bazel-bsp.git /tmp/bazel-bsp
                        cd /tmp/bazel-bsp
                        bazel run //bspcli:generator -- /tmp/project_10 10 --targetssize 1
                    """.trimIndent()
        }
        gradle {
            name = "run benchmark"
            id = "run_benchmark"
            tasks = ":performance-testing:test --tests org.jetbrains.bsp.performance.testing.BazelTest"
            gradleParams = "-Dorg.gradle.jvmargs=-Xmx12g $sysArgs"
        }
    }
)


object GitHub : Benchmark(
    vcsRoot = BaseConfiguration.intellijBspGitHubVcs
)

object Space : Benchmark(
    vcsRoot = BaseConfiguration.intellijBspSpaceVcs
)