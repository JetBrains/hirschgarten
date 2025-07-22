package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class Benchmark(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    artifactRules = "+:%system.teamcity.build.checkoutDir%/metrics.txt",
    name = "[benchmark] server 10 targets",
    vcsRoot = vcsRoot,
    steps = {
      bazel {
        name = "run benchmark 10 targets"
        id = "run_benchmark"
        command = "run"
        targets = "//server/bspcli:bspcli /home/hirschuser/project_10 %system.teamcity.build.checkoutDir%/metrics.txt"
        arguments = Utils.CommonParams.BazelCiSpecificArgs
        toolPath = "/usr/local/bin"
        logging = BazelStep.Verbosity.Diagnostic
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }
      }
    },
  )

object GitHub : Benchmark(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

