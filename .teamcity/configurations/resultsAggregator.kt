package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.*

open class Results(
  private val vcsRootToUse: VcsRoot
) : BuildType({
  val isGitHub = (vcsRootToUse.id == VcsRootHirschgarten.id)
  name = "Results"

  allowExternalStatus = true

  vcs {
    root(vcsRootToUse)
    showDependenciesChanges = false
  }

  if (isGitHub) {
    id("GitHub" + name.toExtId())
  } else {
    id("Space" + name.toExtId())
  }

  if (isGitHub) {
    features {
      pullRequests {
        vcsRootExtId = "${VcsRootHirschgarten.id}"
        provider = github {
          authType = token { token = CredentialsStore.GitHubPassword }
          filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
        }
      }
    }
  } else {
    // Publish statuses to Space for Space Results aggregator
    features {
      commitStatusPublisher {
        vcsRootExtId = "${VcsRootHirschgartenSpace.id}"
        publisher = space {
          authType = connection { connectionId = "PROJECT_EXT_12" }
          displayName = "BazelTeamCityCloud"
        }
      }
    }
  }

  type = Type.COMPOSITE
})

object Aggregator : Results(VcsRootHirschgarten)
object AggregatorSpace : Results(VcsRootHirschgartenSpace)
