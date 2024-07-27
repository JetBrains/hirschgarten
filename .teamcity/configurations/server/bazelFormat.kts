package configurations.server

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Buildifier(vcsRoot: GitVcsRoot, name: String) :
  BaseConfiguration.BaseBuildType(
    name = name,
    steps = {
      script {
        this.name = "checking formatting with buildifier"
        scriptContent =
          """
          bazel run //tools/format:format.check --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140
          """.trimIndent()
      }
    },
    vcsRoot = vcsRoot,
  )

object GitHub : Buildifier(
  name = "[format] GH formatter",
  vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Buildifier(
  name = "[format] Space formatter",
  vcsRoot = BaseConfiguration.SpaceVcs,
)
