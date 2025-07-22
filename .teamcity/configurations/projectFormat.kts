package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class FormatChecker(vcsRoot: GitVcsRoot) :
  BaseConfiguration.BaseBuildType(
    name = "[format] check formatting",
    steps = {
      script {
        this.name = "checking formatting with buildifier"
        scriptContent =
          """
          bazel run //tools/format:format.check ${Utils.CommonParams.BazelCiSpecificArgs}
          """.trimIndent()
        Utils.DockerParams.get().forEach { (key, value) ->
          param(key, value)
        }      }
    },
    vcsRoot = vcsRoot,
  )

object GitHub : FormatChecker(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

