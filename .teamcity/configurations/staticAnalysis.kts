package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.qodana
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class Analyze(vcsRoot: GitVcsRoot) :
    BaseConfiguration.BaseBuildType(
        name = "[analysis] Qodana",
        requirements = {
          endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
          equals("container.engine.osType", "linux")
        },
        steps = {
            bazel {
                name = "build plugins"
                id = "build_plugins"
                command = "build"
                targets = "//..."
                arguments = Utils.CommonParams.BazelCiSpecificArgs
                logging = BazelStep.Verbosity.Diagnostic
                Utils.DockerParams.get().forEach { (key, value) ->
                  param(key, value)
                }
            }
            script {
                name = "add plugins to qodana"
                id = "add_plugins_to_qodana"
                scriptContent = """
                    #!/bin/bash
                    set -euxo
                    
                    rm -R bazel-bin
                    cp -R %system.agent.persistent.cache%/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/bin ./bazel-bin
                    
                    mkdir %system.agent.persistent.cache%/plugins
                    
                    unzip bazel-bin/plugin-bazel/intellij-bazel.zip -d %system.agent.persistent.cache%/plugins
                    unzip bazel-bin/plugin-bsp/intellij-bsp.zip -d %system.agent.persistent.cache%/plugins
                """.trimIndent()
            }
            qodana {
                name = "run qodana"
                id = "run_qodana"
                reportAsTests = true
                linter = customLinter {
                    image = Utils.CommonParams.DockerQodanaImage
                }
                additionalDockerArguments = """
                    -v %system.agent.persistent.cache%/plugins/intellij-bazel:/opt/idea/custom-plugins/intellij-bazel
                    -v %system.agent.persistent.cache%/plugins/intellij-bsp:/opt/idea/custom-plugins/intellij-bsp
                """.trimIndent()
                additionalQodanaArguments = """
                    --property=bsp.build.project.on.sync=true
                    --property=idea.is.internal=true
                    --report-dir /data/results/report
                    --save-report
                    --baseline tools/qodana/qodana.sarif.json
                    --config tools/qodana/qodana.yaml
                """.trimIndent()
                cloudToken = "%qodana.cloud.token%"
                collectAnonymousStatistics = true
            }
        },
        vcsRoot = vcsRoot,
        params = {
            password("qodana.cloud.token", "credentialsJSON:d57ead0e-b567-440d-817e-f92e084a1cc0", label = "qodana.cloud.token", description = "Qodana token for Hirschgarten statistics", display = ParameterDisplay.HIDDEN)
        },
        dockerSupport = {
            loginToRegistry = on {
            dockerRegistryId = "PROJECT_EXT_3"
            }
        },
        failureConditions = { executionTimeoutMin = 30 }
    )

object GitHub : Analyze(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Analyze(
    vcsRoot = BaseConfiguration.SpaceVcs,
)
