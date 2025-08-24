package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

object CheckFormating : BaseBuildType(
  name = "[format] `bazel run :format`",
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

