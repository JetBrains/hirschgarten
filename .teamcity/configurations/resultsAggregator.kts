
package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests

open class Results : BuildType({

    name = "Results"

    allowExternalStatus = true

    vcs {
      root(VcsRoots.GitHubVcs)
      showDependenciesChanges = false
    }

    id("GitHub" + name.toExtId())
    features {
      pullRequests {
        vcsRootExtId = "${VcsRoots.GitHubVcs.id}"
        provider =
          github {
            authType =
              token {
                token = Utils.CredentialsStore.GitHubPassword
              }
            filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
          }
      }
    }

    type = Type.COMPOSITE
  })

object Aggregator : Results()
