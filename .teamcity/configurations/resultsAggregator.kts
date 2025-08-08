
package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests

object ResultsAggregator : BuildType({

    name = "Results"

    allowExternalStatus = true

    vcs {
      root(BaseConfiguration.GitHubVcs)
      showDependenciesChanges = false
    }

    id(name.toExtId())
    features {
      pullRequests {
        vcsRootExtId = "${BaseConfiguration.GitHubVcs.id}"
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

