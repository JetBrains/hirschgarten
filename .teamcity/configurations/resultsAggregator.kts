
package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Aggregator(vcsRoot: GitVcsRoot) :
  BuildType({

    name = "Results"

    allowExternalStatus = true

    vcs {
      root(vcsRoot)
      showDependenciesChanges = false
    }

    if (vcsRoot.name == "hirschgarten-github") {
      id("GitHub$name".toExtId())
      features {
        pullRequests {
          vcsRootExtId = "${vcsRoot.id}"
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
    } else {
      id("Space$name".toExtId())
      features {
        commitStatusPublisher {
          vcsRootExtId = "${vcsRoot.id}"
          publisher =
            space {
              authType =
                connection {
                  connectionId = "PROJECT_EXT_12"
                }
              displayName = "BazelTeamCityCloud"
            }
        }
      }
    }

    type = Type.COMPOSITE
  })

object GitHub : Aggregator(
  vcsRoot = BaseConfiguration.GitHubVcs,
)

