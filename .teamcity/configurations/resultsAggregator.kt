
package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests

open class Results : BuildType({

    name = "Results"

    allowExternalStatus = true

    vcs {
      root(VcsRootHirschgarten)
      showDependenciesChanges = false
    }

    id("GitHub" + name.toExtId())
    features {
      pullRequests {
        vcsRootExtId = "${VcsRootHirschgarten.id}"
        provider =
          github {
            authType =
              token {
                token = CredentialsStore.GitHubPassword
              }
            filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
          }
      }
    }

    type = Type.COMPOSITE
  })

object Aggregator : Results()
