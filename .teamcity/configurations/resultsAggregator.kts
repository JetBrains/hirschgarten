package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Aggregator(vcsRoot: GitVcsRoot, component: String) :
    BuildType({
      name = "Results"

      allowExternalStatus = true

      vcs {
        root(vcsRoot)
        showDependenciesChanges = false
      }

      if (vcsRoot.name == "hirschgarten-github") {
        id("GitHub$component$name".toExtId())
        features {
          pullRequests {
            vcsRootExtId = "${vcsRoot.id}"
            provider = github {
              authType = token { token = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6" }
              filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
            }
          }
        }
      } else {
        id("Space$component$name".toExtId())
      }

      type = Type.COMPOSITE
    })

object ServerGitHub : Aggregator(component = "Server", vcsRoot = BaseConfiguration.GitHubVcs)

object ServerSpace : Aggregator(component = "Server", vcsRoot = BaseConfiguration.SpaceVcs)

object PluginBspGitHub :
    Aggregator(component = "Plugin BSP", vcsRoot = BaseConfiguration.GitHubVcs)

object PluginBspSpace : Aggregator(component = "Plugin BSP", vcsRoot = BaseConfiguration.SpaceVcs)

object PluginBazelGitHub :
    Aggregator(component = "Plugin Bazel", vcsRoot = BaseConfiguration.GitHubVcs)

object PluginBazelSpace :
    Aggregator(component = "Plugin Bazel", vcsRoot = BaseConfiguration.SpaceVcs)
