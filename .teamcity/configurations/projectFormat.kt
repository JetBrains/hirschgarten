package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.VcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

class CheckFormattingBuild(
  customVcsRoot: VcsRoot
) : BaseBuildType(
  name = "[format] `bazel run :format`",
  customVcsRoot = customVcsRoot,
  steps = {
    script {
      this.name = "checking formatting with buildifier"
      scriptContent =
        """
          bazel run //tools/format:format.check ${CommonParams.BazelCiSpecificArgs}
          """.trimIndent()
      DockerParams.get().forEach { (key, value) ->
        param(key, value)
      }
    }
  }
)

object FormatBuildFactory {
  val GitHub: BaseBuildType by lazy { CheckFormattingBuild(VcsRootHirschgarten) }
  val Space: BaseBuildType by lazy { CheckFormattingBuild(VcsRootHirschgartenSpace) }
}
